package com.iamplus.earin.communication.cap.transports;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.cap.BluetoothGattStatus;
import com.iamplus.earin.communication.cap.CapUuids;
import com.iamplus.earin.communication.utils.ByteBuffer;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public abstract class BleCentralTransport extends AbstractTransport implements Handler.Callback
{
    private static final String TAG = BleCentralTransport.class.getSimpleName();

    //-- Descriptor UUIDs
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID       = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    //////////////
    //Other constants

    //Nbr of seconds that a session is queued, waiting for a lock to match/be found, until we automatically kill the session
    private static final int MAX_PROTOCOL_CHARACTERISTIC_WRITE_CHUNK_SIZE = 20;

    private ScheduledExecutorService threadScheduler;
    private ScheduledFuture connectPeripheralTimeoutFuture;

    private BluetoothDevice currentDevice;
    private BluetoothGatt currentGatt;
    private BluetoothGattCharacteristic requestCharacteristic;
    private BluetoothGattCharacteristic eventCharacteristic;
    private BluetoothGattCharacteristic upgradeCharacteristic;
    private boolean isNotifyingOnRequestCharacteristic;
    private boolean isNotifyingOnEventCharacteristic;
    private boolean isNotifyingOnUpgradeCharacteristic;

    private Semaphore writeCharacteristicAccessSemaphore;
    private Semaphore writeCharacteristicCompletionSemaphore;

    private BroadcastReceiver bondStateChangeReceiver;
    private Handler bleHandler;

    protected BleCentralTransport(String identifier)
    {
        super(identifier);

        //Construct...
        this.currentDevice = null;
        this.currentGatt = null;
        this.requestCharacteristic = null;
        this.eventCharacteristic = null;
        this.upgradeCharacteristic = null;
        this.isNotifyingOnRequestCharacteristic = false;
        this.isNotifyingOnEventCharacteristic = false;
        this.isNotifyingOnUpgradeCharacteristic = false;

        //Setup scheduler so that we can detect timeouts...
        this.threadScheduler = Executors.newScheduledThreadPool(5);

        this.connectPeripheralTimeoutFuture = null;

        //Only one can write to a characteristic at a time... (e.g., when doing an upgrade AND requests at the same time...)
        this.writeCharacteristicAccessSemaphore = new Semaphore(1);

        //Initialized with zero permits, imply that when we acquire, we'll block until someone else will release...
        this.writeCharacteristicCompletionSemaphore = new Semaphore(0);

        //Setup the shared background thread that we'll use for *all* communication to/from the Bluetooth API
        // -- crete thread...
        HandlerThread bleHandlerThread = new HandlerThread("CapBleHandlerTread");
        bleHandlerThread.start();

        // -- link to out BLE-handler that we'll use to get the work done...
        this.bleHandler = new Handler(bleHandlerThread.getLooper(), this);

        //Register as listener to BOND changes...
        this.bondStateChangeReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();

                Log.d(TAG, "Received action; " + action);

                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
                {
                    //State has changed!
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    switch(state){
                        case BluetoothDevice.BOND_BONDING:
                            Log.d(TAG, "Bonding...");
                            break;

                        case BluetoothDevice.BOND_BONDED:
                            Log.d(TAG, "Bonded!...");

                            BleCentralTransport.this.bondingComplete(device);

                            break;

                        case BluetoothDevice.BOND_NONE:
                            Log.d(TAG, "Not bonded!...");
                            break;
                    }
                }
            };
        };

        Log.d(TAG, "Registering BOND state-change receiver...");
        EarinApplication.getContext().registerReceiver(
                this.bondStateChangeReceiver,
                new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    public static BleCentralTransport getInstance(String identifier)
    {
        //Well -- based on API, let's do this differently...
        int apiVersion = Build.VERSION.SDK_INT;
        if (apiVersion >= 23)
        {
            return new Api23BleCentralTransport(identifier);
        }
        else if (apiVersion >= 18)
        {
            return new Api18BleCentralTransport(identifier);
        }

        //Not supported on pre-18 releases of Android...
        Log.w(TAG, "BLE not supported on pre-18 API releases (detected API = " + apiVersion + ")");
        return null;
    }

    private void sendMessageToHandler(BleHandlerMessage message)
    {
        this.sendMessageToHandler(message, null);
    }

    private void sendMessageToHandler(BleHandlerMessage message, AbstractBleHandlerMessageData object)
    {
        Log.d(TAG, "Sending message " + message + " to BLE handler...");

        //Obtain & setup the handler-message...
        Message handlerMessage = this.bleHandler.obtainMessage(message.ordinal(), object);

        //Deliver the message!
        handlerMessage.sendToTarget();
    }

    @Override
    public void connect(BluetoothDevice device, long timeoutSeconds) throws Exception
    {
        Log.d(TAG, "Connecting transport");

        if (this.currentDevice == null)
        {
            //Reset characteristics...
            this.requestCharacteristic = null;
            this.eventCharacteristic = null;
            this.upgradeCharacteristic = null;
            this.isNotifyingOnRequestCharacteristic = false;
            this.isNotifyingOnEventCharacteristic = false;
            this.isNotifyingOnUpgradeCharacteristic = false;

            //Kick scheduled timeout detector since connection to the GATT server sometimes fail...
            this.connectPeripheralTimeoutFuture = threadScheduler.schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    //Kick timeout guard
                    BleCentralTransport.this.peripheralConnectionTimeout();
                }
            }, timeoutSeconds, TimeUnit.SECONDS);

            //Start by trying to bond with the device... if we needed
            this.currentDevice = device;
            Log.d(TAG, "Device's type: " + this.currentDevice.getType());

            //Should we bond?
            if (true)
            {
                Log.d(TAG, "Bondable links requested, so let's ensure that we're bonded OR let's bond!");

                int bondState = this.currentDevice.getBondState();
                Log.d(TAG, "Device's current bond-state: " + bondState);

                switch (bondState)
                {
                    case BluetoothDevice.BOND_NONE:
                    {
                        Log.d(TAG, "Not bonded -- attempt to bond!");

                        //Initiate the bond-process...
                        this.sendMessageToHandler(BleHandlerMessage.BondWithPeripheral);
                        break;
                    }

                    case BluetoothDevice.BOND_BONDED:
                    {
                        Log.d(TAG, "Already bonded -- proceed with connect!");

                        //Initiate the connect-process...
                        this.sendMessageToHandler(BleHandlerMessage.ConnectToPeripheral);
                        break;
                    }

                    case BluetoothDevice.BOND_BONDING:
                    {
                        Log.d(TAG, "Bonding in progress -- await result an trust the outcome...");
                        break;
                    }

                    default:
                    {
                        Log.w(TAG, "Unknown bond-state... That's odd?");
                        break;
                    }
                }
            }
            else
            {
                Log.d(TAG, "Bonding NOT necessary, so let's just connect ASAP instead...");

                //Initiate the connect-process...
                this.sendMessageToHandler(BleHandlerMessage.ConnectToPeripheral);
            }
        }
        else
        {
            Log.d(TAG, "Ignore conn request -- we're already busy");
        }
    }

    public abstract void createBond(final BluetoothDevice device) throws Exception;
    public abstract BluetoothGatt connectGattOverBle(final BluetoothDevice device, final BluetoothGattCallback callback) throws Exception;

    private void bondingComplete(BluetoothDevice bondedDevice)
    {
        Log.d(TAG, "Bonding completed!");

        //Check that the bondedDevice is the same device as we're working with...
        if (this.currentDevice == bondedDevice)
        {
            try
            {
                //Initiate the connect-process...
                this.sendMessageToHandler(BleHandlerMessage.ConnectToPeripheral);
            }
            catch (Exception x)
            {
                Log.w(TAG, "Failed post-bond connection...");
            }
        }
        else
        {
            Log.d(TAG, "Ignored... not 'our' current device...");
        }
    }

    @Override
    public void cleanup() throws Exception
    {
        Log.d(TAG, "Cleaning up transport");

        //Initiate the cleanup-process in Handler...
        this.sendMessageToHandler(BleHandlerMessage.CleanupPeripheral);

        //Unregister bond-state change
        Log.d(TAG, "Unregistered BOND state-change receiver...");
        EarinApplication.getContext().unregisterReceiver(this.bondStateChangeReceiver);
    }

    @Override
    public void writeRequestData(byte[] data) throws Exception
    {
        //Get access...
        Log.i(TAG, "Await write access semaphore (for request)...");
        if (!this.writeCharacteristicAccessSemaphore.tryAcquire(5, TimeUnit.SECONDS))
        {
            //Failed...
            Log.w(TAG, "Aborting; no write access for request... something else was blocking us");
            throw new Exception("Write access (request) timeout");
        }
        else
        {
            Log.d(TAG, "Write access for request command granted -- proceed");
        }

        //Do we have the protocol...?
        if (this.currentGatt != null && this.requestCharacteristic != null)
        {
            ByteBuffer buffer = new ByteBuffer();
            buffer.appendBytes(data);

            //Chunk-by-chunk, write the buffer out on the protocol...
            byte[] chunk;
            while ((chunk = buffer.consumeBytes(MAX_PROTOCOL_CHARACTERISTIC_WRITE_CHUNK_SIZE)) != null)
            {
                //Yes, got chunk -- delegate to handler...
                this.sendMessageToHandler(BleHandlerMessage.WriteDataToPeripheral, new WriteDataCharacteristicBleHandlerMessageData(
                        this.currentGatt,
                        this.requestCharacteristic,
                        chunk
                ));

                //Await writeConfirm from callback...
                writeCharacteristicCompletionSemaphore.acquireUninterruptibly();
            }
        }

        //Release...
        Log.d(TAG, "Releasing write access for request");
        this.writeCharacteristicAccessSemaphore.release();
    }

    @Override
    public void writeUpgradeData(byte[] data) throws Exception
    {
        //Get access...
        Log.i(TAG, "Await write access semaphore (for upgrade)...");
        if (!this.writeCharacteristicAccessSemaphore.tryAcquire(5, TimeUnit.SECONDS))
        {
            //Failed...
            Log.w(TAG, "Aborting; no write access for upgrade... something else was blocking us");
            throw new Exception("Write access (upgrade) timeout");
        }
        else
        {
            Log.d(TAG, "Write access for upgrade command granted -- proceed");
        }

        //Do we have the protocol...?
        if (this.currentGatt != null && this.upgradeCharacteristic != null)
        {
            ByteBuffer buffer = new ByteBuffer();
            buffer.appendBytes(data);

            //Chunk-by-chunk, write the buffer out on the protocol...
            byte[] chunk;
            while ((chunk = buffer.consumeBytes(MAX_PROTOCOL_CHARACTERISTIC_WRITE_CHUNK_SIZE)) != null)
            {
                //Yes, got chunk -- delegate to handler...
                this.sendMessageToHandler(BleHandlerMessage.WriteDataToPeripheral, new WriteDataCharacteristicBleHandlerMessageData(
                        this.currentGatt,
                        this.upgradeCharacteristic,
                        chunk
                ));

                //Await writeConfirm from callback...
                writeCharacteristicCompletionSemaphore.acquireUninterruptibly();
            }
        }

        //Release...
        Log.d(TAG, "Releasing write access for upgrade");
        this.writeCharacteristicAccessSemaphore.release();
    }

    /////////////////
    //

    private void peripheralConnected(BluetoothGatt gatt, BluetoothGattStatus status)
    {
        Log.d(TAG, "Peripheral connected with status " + status);

        if (status != BluetoothGattStatus.Success)
        {
            //Failed!
            Log.w(TAG, "Failed connecting to GATT!");

            //Clean-up...
            cleanupCurrentPeripheral();

            //Tell delegate that we've failed...
            didFailWithConnection(new Exception("Failed connect GATT; status = " + status));

            return;
        }

        //The same GATT as we intended to connection to?
        if (this.currentGatt == gatt)
        {
            Log.d(TAG, "Discovering services on current GATT");

            //Abort timeout
            if (connectPeripheralTimeoutFuture != null)
            {
                connectPeripheralTimeoutFuture.cancel(true);
                connectPeripheralTimeoutFuture = null;
            }

            //Kick discovery of services, using the already defined callback used when connecting to the GATT
            currentGatt.discoverServices();
        }
    }

    private void peripheralConnectionTimeout()
    {
        Log.d(TAG, "Peripheral connection timeout!");

        //Cleanup future...
        this.connectPeripheralTimeoutFuture = null;

        //Cleanup!
        this.cleanupCurrentPeripheral();

        didFailWithConnection(new Exception("Failed connect GATT; timedout"));
    }

    private void peripheralDisconnected(BluetoothGatt gatt, BluetoothGattStatus status)
    {
        Log.d(TAG, "GATT disconnected with status " + status);

        //Consistency check...
        if (this.currentGatt == gatt)
        {
            currentGatt.close();
            currentGatt = null;
            Log.d(TAG, "GATT client closed");

            //Clean-up
            this.cleanupCurrentPeripheral();

            //Tell delegate...
            didDisconnect(status);
        }
    }

    private void peripheralServiceDiscoveryCompleted(BluetoothGatt gatt, BluetoothGattStatus status)
    {
        Log.d(TAG, "Peripheral service discovery completed with status " + status);

        if (status != BluetoothGattStatus.Success)
        {
            //Failed!
            Log.w(TAG, "Failed discovering services/characteristics!");

            cleanupCurrentPeripheral();

            didFailWithConnection(new Exception("Failed connect GATT; failed discovering services with status: " + status));

            return;
        }

        //Consistency check...
        if (this.currentGatt == gatt)
        {
            //Get the CAP-service!
            BluetoothGattService service = this.currentGatt.getService(CapUuids.CAP_SERVICE_UUID);
            if (service != null)
            {
                Log.d(TAG, "CapControl service found, inspect it's characteristics");

                //Good -- all characteristics and all services are hereby identified; let's parse and see if we can find what we need!
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics())
                {
                    //Identify...
                    if (characteristic.getUuid().equals(CapUuids.CAP_REQUESTS_CHARACTERISTIC_UUID))
                        this.requestCharacteristic = characteristic;
                    else if (characteristic.getUuid().equals(CapUuids.CAP_EVENTS_CHARACTERISTIC_UUID))
                        this.eventCharacteristic = characteristic;
                    else if (characteristic.getUuid().equals(CapUuids.CAP_UPGRADE_CHARACTERISTIC_UUID))
                        this.upgradeCharacteristic = characteristic;
                }

                //Well, for this to work, we need to have at least a valid request characteristic
                if (this.requestCharacteristic != null)
                {
                    Log.d(TAG, "Found request characteristic!");

                    //Subscribe to notifications on the request charateristic so that we trigger when the lock is feedback response data.
                    this.currentGatt.setCharacteristicNotification(this.requestCharacteristic, true);

                    BluetoothGattDescriptor descriptor = this.requestCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    this.currentGatt.writeDescriptor(descriptor);

                    //Await outcome...

                    return;
                }
                else
                {
                    Log.w(TAG, "Missing the vital request characteristic!");
                }
            }
            else
            {
                Log.w(TAG, "Missing support for CAP service -- ignore");
            }

            didFailWithConnection(new Exception("Failed discovering CapControl services (status = " + status + ")"));

            //If we get here -- we've failed and should cleanup...
            this.cleanupCurrentPeripheral();
        }
        else
        {
            Log.e(TAG, "Callback-Gatt is NOT the currentGatt! Strange!!!");
            gatt.disconnect();
        }
    }

    private void peripheralDescriptorWriteCompleted(BluetoothGatt gatt, UUID descriptorUuid, UUID characteristicUuid, BluetoothGattStatus status)
    {
        Log.d(TAG, "Peripheral descriptor write completed with status " + status);

        if (status != BluetoothGattStatus.Success)
        {
            //Failed!
            Log.w(TAG, "Failed writing descriptor " + descriptorUuid);

            cleanupCurrentPeripheral();
            return;
        }

        //Consistency check...
        if (this.currentGatt == gatt)
        {
            //Tick-off all the characteristics we need to notify on...
            if (this.requestCharacteristic != null && characteristicUuid.equals(this.requestCharacteristic.getUuid()))
            {
                Log.d(TAG, "Request characteristic descriptor written...");
                this.isNotifyingOnRequestCharacteristic = true;
            }

            if (this.eventCharacteristic != null && characteristicUuid.equals(this.eventCharacteristic.getUuid()))
            {
                Log.d(TAG, "Event characteristic descriptor written...");
                this.isNotifyingOnEventCharacteristic = true;
            }

            if (this.upgradeCharacteristic != null && characteristicUuid.equals(this.upgradeCharacteristic.getUuid()))
            {
                Log.d(TAG, "Upgrade characteristic descriptor written...");
                this.isNotifyingOnUpgradeCharacteristic = true;
            }

            //Launch new notification requests OR we're done?
            BluetoothGattCharacteristic nextCharacteristicForNotification = null;
            if (this.eventCharacteristic != null && !this.isNotifyingOnEventCharacteristic)
                nextCharacteristicForNotification = this.eventCharacteristic;
            else if (this.upgradeCharacteristic != null && !this.isNotifyingOnUpgradeCharacteristic)
                nextCharacteristicForNotification = this.upgradeCharacteristic;

            //Found anything that we need to be notifying?
            if (nextCharacteristicForNotification != null)
            {
                //Subscribe to notifications on the request charateristic so that we trigger when the lock is feedback response data.
                this.currentGatt.setCharacteristicNotification(nextCharacteristicForNotification, true);

                BluetoothGattDescriptor nextCharacteristicForNotificationDescriptor = nextCharacteristicForNotification.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                nextCharacteristicForNotificationDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                this.currentGatt.writeDescriptor(nextCharacteristicForNotificationDescriptor);

                //Await outcome...
            }
            else
            {
                //OK -- Great stuff, we're now "notifying" on the protocol characteristic so that we can
                // detect response-data when the lock sends it!
                Log.d(TAG, "All supported characteristic descriptors written... we're done!");

                //Tell delegate that we've done it!
                this.didSucceedWithConnection();
            }
        }
        else
        {
            Log.e(TAG, "Callback-Gatt is NOT the currentGatt! Strange!!!");
            gatt.disconnect();
        }
    }

    private void peripheralCharacteristicWriteCompleted(BluetoothGatt gatt, UUID characteristicUuid, BluetoothGattStatus status)
    {
        Log.d(TAG, "Peripheral characteristic " + characteristicUuid + " write completed with status " + status);

        if (status != BluetoothGattStatus.Success)
        {
            //Failed!
            Log.w(TAG, "Failed writing characteristics " + characteristicUuid);
//
//            cleanupCurrentPeripheral();
//            return;
        }

        //Consistency check...
        if (this.currentGatt == gatt)
        {
            //What happened?
            if (this.requestCharacteristic != null && this.requestCharacteristic.getUuid().equals(characteristicUuid))
            {
                Log.d(TAG, "Request characteristic written -- we can send more now, if there's more to send...");
                this.writeCharacteristicCompletionSemaphore.release();
            }

            else if (this.upgradeCharacteristic != null && this.upgradeCharacteristic.getUuid().equals(characteristicUuid))
            {
                Log.d(TAG, "Upgrade characteristic written -- we can send more now, if there's more to send...");
                this.writeCharacteristicCompletionSemaphore.release();
            }
        }
        else
        {
            Log.e(TAG, "Callback-Gatt is NOT the currentGatt! Strange!!!");
            gatt.disconnect();
        }
    }

    private void peripheralCharacteristicChanged(BluetoothGatt gatt, UUID characteristicUuid, byte [] characteristicValue)
    {
        //Consistency check...
        if (this.currentGatt == gatt)
        {
            if (this.requestCharacteristic != null && this.requestCharacteristic.getUuid().equals(characteristicUuid))
            {
                //We just got data sent into our protocol characteristic!!!
                Log.d(TAG, "Request characteristic changed");

                this.didReceiveResponseData(characteristicValue);
            }

            else if (this.eventCharacteristic != null && this.eventCharacteristic.getUuid().equals(characteristicUuid))
            {
                //We just got data sent into our protocol characteristic!!!
                Log.d(TAG, "Event characteristic changed");

                this.didReceiveEventData(characteristicValue);
            }

            else if (this.upgradeCharacteristic != null && this.upgradeCharacteristic.getUuid().equals(characteristicUuid))
            {
                //We just got data sent into our protocol characteristic!!!
                Log.d(TAG, "Upgrade characteristic changed");

                this.didReceiveUpgradeData(characteristicValue);
            }
        }
        else
        {
            Log.e(TAG, "Callback-Gatt is NOT the currentGatt! Strange!!!");
            gatt.disconnect();
        }
    }

    private void cleanupCurrentPeripheral()
    {
        Log.d(TAG, "Cleaning up current peripheral " + this.currentGatt + ", isMainThread : " + (Looper.myLooper() == Looper.getMainLooper()));

        //Cancel all future timeouts...
        if (this.connectPeripheralTimeoutFuture != null)
        {
            Log.d(TAG, "Aborting scheduled connection timeout");
            this.connectPeripheralTimeoutFuture.cancel(true);
            this.connectPeripheralTimeoutFuture = null;
        }

        //If NOT connected, clean up logical state-holders etc...
        if (this.currentGatt != null)
        {
            //Disconnect...
            this.currentGatt.disconnect();

            //Once we detect a disconnect, we'll call this clean-up process again and "tidy-up"
            //IMPORTANT: do not close/cleanup the "currentGatt" since we need to keep it alive until
            // the disconnection has been properly detected...
            return;
        }

        this.currentDevice = null;
        this.requestCharacteristic = null;
        this.eventCharacteristic = null;
        this.upgradeCharacteristic = null;
        this.isNotifyingOnRequestCharacteristic = false;
        this.isNotifyingOnEventCharacteristic = false;
        this.isNotifyingOnUpgradeCharacteristic = false;
    }

    public boolean handleMessage(Message msg)
    {
        //Essentially -- based on what we're asked to do (the *what*), this handler just
        // delegate to the worker-functions to provide some level of readability... ;)
        Log.d(TAG, "BLE Handler received message; " + msg.what);

        //Convert into BLE message enum...
        BleHandlerMessage bleMessage = BleHandlerMessage.getEnumValue(msg.what);
        Log.d(TAG, "BLE Handler message enum " + bleMessage);

        try
        {
            //... so what happened?
            switch (bleMessage)
            {
                //////////////////////
                //REQUEST messages...

                case ConnectToPeripheral:
                {
                    //Let the actual impl class perform the connect since this is done in different ways for different API versions...
                    this.currentGatt = this.connectGattOverBle(this.currentDevice, new PeripheralGattCallback());
                    break;
                }

                case BondWithPeripheral:
                {
                    //Let the actual impl class perform the bonding since this is done in different ways for different API versions...
                    this.createBond(this.currentDevice);
                    break;
                }

                case CleanupPeripheral:
                {
                    this.cleanupCurrentPeripheral();
                    break;
                }

                case WriteDataToPeripheral:
                {
                    //Get wrapper object-data...
                    WriteDataCharacteristicBleHandlerMessageData data = (WriteDataCharacteristicBleHandlerMessageData)msg.obj;
                    if (data != null)
                    {
                        BluetoothGatt gatt = data.getGatt();
                        BluetoothGattCharacteristic characteristic = data.getCharacteristic();

                        //All there? Then GO!
                        if (gatt != null && characteristic != null)
                        {
                            characteristic.setValue(data.getData());
                            gatt.writeCharacteristic(characteristic);
                        }
                    }

                    break;
                }

                //////////////////////
                //CALLBACK messages...

                case PeripheralConnectionStateChanged:
                {
                    //Get wrapper object-data...
                    ConnectionStateChangedBleHandlerMessageData data = (ConnectionStateChangedBleHandlerMessageData)msg.obj;
                    if (data != null)
                    {
                        //What happened?
                        switch (data.getState())
                        {
                            case BluetoothGatt.STATE_CONNECTED:
                            {
                                this.peripheralConnected(
                                        data.getGatt(),
                                        data.getStatus());
                                break;
                            }

                            case BluetoothGatt.STATE_DISCONNECTED:
                            {
                                this.peripheralDisconnected(
                                        data.getGatt(),
                                        data.getStatus());
                                break;
                            }

                            //Ignore...
                            default:break;
                        }
                    }

                    break;
                }

                case PeripheralServicesDiscovered:
                {
                    //Get wrapper object-data...
                    ServicesDiscoveredBleHandlerMessageData data = (ServicesDiscoveredBleHandlerMessageData)msg.obj;
                    if (data != null)
                    {
                        //And.. delegate to proper function...
                        this.peripheralServiceDiscoveryCompleted(
                                data.getGatt(),
                                data.getStatus());
                    }

                    break;
                }

                case PeripheralCharacteristicWritten:
                {
                    //Get wrapper object-data...
                    CharacteristicWrittenBleHandlerMessageData data = (CharacteristicWrittenBleHandlerMessageData)msg.obj;
                    if (data != null)
                    {
                        //And.. delegate to proper function...
                        this.peripheralCharacteristicWriteCompleted(
                                data.getGatt(),
                                data.getUuid(),
                                data.getStatus());
                    }

                    break;
                }

                case PeripheralCharacteristicChanged:
                {
                    //Get wrapper object-data...
                    CharacteristicChangedBleHandlerMessageData data = (CharacteristicChangedBleHandlerMessageData)msg.obj;
                    if (data != null)
                    {
                        //And.. delegate to proper function...
                        this.peripheralCharacteristicChanged(
                                data.getGatt(),
                                data.getUuid(),
                                data.getData());
                    }

                    break;
                }

                case PeripheralDescriptorWritten:
                {
                    //Get wrapper object-data...
                    DescriptorWrittenBleHandlerMessageData data = (DescriptorWrittenBleHandlerMessageData)msg.obj;
                    if (data != null)
                    {
                        //And.. delegate to proper function...
                        this.peripheralDescriptorWriteCompleted(
                                data.getGatt(),
                                data.getDescriptorUuid(),
                                data.getCharacteristicUuid(),
                                data.getStatus());
                    }

                    break;
                }

                default:
                {
                    break;
                }
            }
        }
        catch (Exception x)
        {
            Log.w(TAG, "Something died trying to process the BLE message...", x);
        }

        //We're done...
        return true;
    }

    private class PeripheralGattCallback extends BluetoothGattCallback
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            super.onConnectionStateChange(gatt, status, newState);

            //Delegate to the BLE handler...
            BleCentralTransport.this.sendMessageToHandler(
                    BleHandlerMessage.PeripheralConnectionStateChanged,
                    new ConnectionStateChangedBleHandlerMessageData(
                            gatt,
                            status,
                            newState));
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status)
        {
            super.onServicesDiscovered(gatt, status);

            //Delegate to the BLE handler...
            BleCentralTransport.this.sendMessageToHandler(
                    BleHandlerMessage.PeripheralServicesDiscovered,
                    new ServicesDiscoveredBleHandlerMessageData(
                            gatt,
                            status));
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status)
        {
            super.onCharacteristicWrite(gatt, characteristic, status);

            //Delegate to the BLE handler...
            BleCentralTransport.this.sendMessageToHandler(
                    BleHandlerMessage.PeripheralCharacteristicWritten,
                    new CharacteristicWrittenBleHandlerMessageData(
                            gatt,
                            characteristic.getUuid(),
                            status));
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicChanged(gatt, characteristic);

            //Delegate to the BLE handler...
            BleCentralTransport.this.sendMessageToHandler(
                    BleHandlerMessage.PeripheralCharacteristicChanged,
                    new CharacteristicChangedBleHandlerMessageData(
                            gatt,
                            characteristic.getUuid(),
                            characteristic.getValue()));
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status)
        {
            super.onDescriptorWrite(gatt, descriptor, status);

            ////Delegate to the BLE handler...
            BleCentralTransport.this.sendMessageToHandler(
                    BleHandlerMessage.PeripheralDescriptorWritten,
                    new DescriptorWrittenBleHandlerMessageData(
                            gatt,
                            descriptor.getUuid(),
                            descriptor.getCharacteristic().getUuid(),
                            status));
        }
    }

    private enum BleHandlerMessage
    {
        //Request enums...
        ConnectToPeripheral,
        BondWithPeripheral,
        CleanupPeripheral,
        WriteDataToPeripheral,

        //Callback enums...
        PeripheralConnectionStateChanged,
        PeripheralServicesDiscovered,
        PeripheralCharacteristicWritten,
        PeripheralCharacteristicChanged,
        PeripheralDescriptorWritten,

        Unknown;

        public static BleHandlerMessage getEnumValue(int ordinal)
        {
            for (BleHandlerMessage enumValue : values())
                if (enumValue.ordinal() == ordinal)
                    return enumValue;

            return Unknown;
        }
    }

    private abstract class AbstractBleHandlerMessageData
    {
        private BluetoothGatt gatt;

        public AbstractBleHandlerMessageData(BluetoothGatt gatt)
        {
            this.gatt = gatt;
        }

        public BluetoothGatt getGatt(){return this.gatt;}
    }

    private class ConnectionStateChangedBleHandlerMessageData extends AbstractBleHandlerMessageData
    {
        private BluetoothGattStatus status;
        private int state;

        public ConnectionStateChangedBleHandlerMessageData(BluetoothGatt gatt, int status, int state)
        {
            super(gatt);

            this.status = BluetoothGattStatus.getStatus(status);
            this.state = state;
        }

        public BluetoothGattStatus getStatus(){return this.status;}
        public int getState(){return this.state;}
    }

    private class WriteDataCharacteristicBleHandlerMessageData extends AbstractBleHandlerMessageData
    {
        private BluetoothGattCharacteristic characteristic;
        private byte [] data;

        public WriteDataCharacteristicBleHandlerMessageData(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte [] data)
        {
            super(gatt);

            this.characteristic = characteristic;
            this.data = data;
        }

        public BluetoothGattCharacteristic getCharacteristic(){return this.characteristic;}
        public byte [] getData(){return this.data;}
    }

    private class ServicesDiscoveredBleHandlerMessageData extends AbstractBleHandlerMessageData
    {
        private BluetoothGattStatus status;

        public ServicesDiscoveredBleHandlerMessageData(BluetoothGatt gatt, int status)
        {
            super(gatt);

            this.status = BluetoothGattStatus.getStatus(status);
        }

        public BluetoothGattStatus getStatus(){return this.status;}
    }

    private class CharacteristicWrittenBleHandlerMessageData extends AbstractBleHandlerMessageData
    {
        private UUID uuid;
        private BluetoothGattStatus status;

        public CharacteristicWrittenBleHandlerMessageData(BluetoothGatt gatt, UUID uuid, int status)
        {
            super(gatt);

            this.uuid = uuid;
            this.status = BluetoothGattStatus.getStatus(status);
        }

        public UUID getUuid(){return this.uuid;}
        public BluetoothGattStatus getStatus(){return this.status;}
    }

    private class CharacteristicChangedBleHandlerMessageData extends AbstractBleHandlerMessageData
    {
        private UUID uuid;
        private byte [] data;

        public CharacteristicChangedBleHandlerMessageData(BluetoothGatt gatt, UUID uuid, byte [] data)
        {
            super(gatt);

            this.uuid = uuid;
            this.data = data;
        }

        public UUID getUuid(){return this.uuid;}
        public byte [] getData(){return this.data;}
    }

    private class DescriptorWrittenBleHandlerMessageData extends AbstractBleHandlerMessageData
    {
        private UUID descriptorUuid;
        private UUID characteristicUuid;
        private BluetoothGattStatus status;

        public DescriptorWrittenBleHandlerMessageData(BluetoothGatt gatt, UUID descriptorUuid, UUID characteristicUuid, int status)
        {
            super(gatt);

            this.descriptorUuid = descriptorUuid;
            this.characteristicUuid = characteristicUuid;
            this.status = BluetoothGattStatus.getStatus(status);
        }

        public UUID getDescriptorUuid(){return this.descriptorUuid;}
        public UUID getCharacteristicUuid(){return this.characteristicUuid;}
        public BluetoothGattStatus getStatus(){return this.status;}
    }
}


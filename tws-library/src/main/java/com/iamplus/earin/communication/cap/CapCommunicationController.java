package com.iamplus.earin.communication.cap;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;


import com.iamplus.earin.BuildConfig;
import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.cap.protocols.CapProtocol;
import com.iamplus.earin.communication.cap.transports.AbstractTransport;
import com.iamplus.earin.communication.cap.transports.BleCentralTransport;
import com.iamplus.earin.communication.cap.transports.CapTransportPreference;
import com.iamplus.earin.communication.cap.transports.SppTransport;
import com.iamplus.earin.communication.cap.transports.TransportDelegate;
import com.iamplus.earin.communication.cap.transports.TransportOpenException;
import com.iamplus.earin.util.SerialExecutor;

import java.util.HashMap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public abstract class CapCommunicationController implements TransportDelegate {
    private static final String TAG = CapCommunicationController.class.getSimpleName();

    //Unique base used to define all intents
    private static final String INTENT_BASE = "se.millsys.apps.capcontrol.CapCommunicationController";

    //All intent actions
    private static final String INTENT_ACTION_BLE = INTENT_BASE + ".Ble";
    public static final String INTENT_ACTION_BLE_CONNECTED = INTENT_ACTION_BLE + ".ConnectionSuccess";
    public static final String INTENT_ACTION_BLE_DISCONNECTED = INTENT_ACTION_BLE + ".Disconnected";

    private static final String INTENT_EXTRAS_BASE = INTENT_BASE + ".Extras";
    public static final String INTENT_EXTRAS_IDENTIFER = INTENT_EXTRAS_BASE + ".Identifier";


    private static final long CONNECT_DEVICE_DEFAULT_TIMEOUT_SECS = 10;
    private static final long CONNECT_DEVICE_UPGRADE_REBOOT_TIMEOUT_SECS = 50;

    ////////////////
    //Local variables/objects

    private BluetoothAdapter bluetoothAdapter;
    private boolean connectingToDevice;
    private CapTransportPreference transportPreference;

    private BluetoothDevice identifiedPeripheral;

    private AbstractTransport currentTransport;
    private CapCommunicator currentCommunicator;

    private HashMap<String, CapUpgradeAssistant> upgradeAssistants;

    private LocalBroadcastManager broadcastManager;
    private CapCommunicationControllerDelegate delegate;

    protected CapCommunicationController() {
        Log.d(TAG, "Created");

        this.broadcastManager = LocalBroadcastManager.getInstance(EarinApplication.getContext());

        this.delegate = null;
        this.connectingToDevice = false;

        this.currentTransport = null;
        this.currentCommunicator = null;
        this.transportPreference = CapTransportPreference.Automatic;

        this.upgradeAssistants = new HashMap<>();

        //and then... wait for state-change!
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                Log.d(TAG, "Received action; " + action);

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    //State has changed!
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    Log.d(TAG, "New state; " + state);
                    switch (state) {
                        case BluetoothAdapter.STATE_ON: {
                            Log.d(TAG, "Our device just enabled Bluetooth! Finally!");

                            //Let the actual impl., controller setup accordingly...
                            setupBluetoothAdapter(bluetoothAdapter);

                            //Well, if we should ne discovering -- let's kick it!
                            if (connectingToDevice) {
                                Log.d(TAG, "We should be attempting to connect to a device... do it!");
                                connectToDevice();
                            }

                            break;
                        }

                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_OFF: {
                            Log.d(TAG, "Someone killed the BT-interface!");

                            cleanupCurrentConnection(false, BluetoothGattStatus.Unknown);

                            break;
                        }

                        default: {
                            //Something else...
                            break;
                        }
                    }
                }
            }
        };

        EarinApplication.getContext().registerReceiver(receiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (this.bluetoothAdapter == null) {
            //Abort - the device does not support Bluetooth...
            Log.e(TAG, "Devices does NOT support Bluetooth -- why are you here?");
        }

        //Check if we have Bluetooth enabled...
        else if (this.bluetoothAdapter.isEnabled()) {
            //Let the actual impl., controller setup accordingly...
            this.setupBluetoothAdapter(this.bluetoothAdapter);
        } else {
            //Bluetooth is NOT enabled -- let's enable ourselves...
            //this.bluetoothAdapter.enable();
        }
    }

    ///////////////////////////////
    //Delegate ctrl setters and getters...

    public void setDelegate(CapCommunicationControllerDelegate delegate) {
        this.delegate = delegate;
    }

//    public CapCommunicationControllerDelegate getDelegate() {
//        return this.delegate;
//    }

    public void setTransportPreference(CapTransportPreference preference) {
        this.transportPreference = preference;
    }

//    public CapTransportPreference getTransportPreference() {
//        return this.transportPreference;
//    }

    private static CapCommunicationController singletonController = null;

    public static CapCommunicationController getInstance() {
        if (singletonController == null) {
            //Well -- based on API, let's do this differently...
            int apiVersion = Build.VERSION.SDK_INT;
            if (apiVersion >= 21)
                singletonController = new Api21CapCommunicationController();
            else if (apiVersion >= 18)
                singletonController = new Api18CapCommunicationController();
            else {
                //Not supported on pre-18 releases of Android...
                Log.e(TAG, "BLE not supported on pre-18 API releases (detected API = " + apiVersion + ")");
            }
        }

        return singletonController;
    }

    public CapCommunicator getConnectedCommunicator() {
        return this.currentCommunicator;
    }

    private synchronized void setIdentifiedPeripheral(BluetoothDevice periheral) {
        Log.d(TAG, "Setting peripheral");
        this.identifiedPeripheral = periheral;
    }

    public synchronized BluetoothDevice getIdentifiedPeripheral() {
        Log.d(TAG, "Retrieving peripheral");


        return this.identifiedPeripheral;
    }

    protected abstract void setupBluetoothAdapter(BluetoothAdapter adapter);

    protected abstract void startScanning(BluetoothAdapter adapter);

    protected abstract void stopScanning(BluetoothAdapter adapter);

    public Boolean isConnected() {
        return this.currentCommunicator != null;
    }


    public void connectToDevice(BluetoothDevice device) {
        if (!this.connectingToDevice) {
            Log.d(TAG, "connectToDevice: ");
            setIdentifiedPeripheral(device);
            connectToDevice();
        }
    }

    private void connectToDevice() {
        Log.d(TAG, "Time to connect to a device (hopefully)");

        if (!this.connectingToDevice) {
            Log.d(TAG, "Not already connecting -- so setup start params");

            //Mark state...
            this.connectingToDevice = true;

        } else {
            Log.d(TAG, "Already connecting!");
            return;
        }

        //Check state...
        if (!this.bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth not enabled... ");

            //Ask delegate for permission to turn Bluetooth on...
            if (this.delegate != null) {
                Log.d(TAG, "Checking with delegate if we can auto-enable Bluetooth");
                if (this.delegate.permittedToEnableBluetooth(this)) {
                    Log.d(TAG, "Enabling Bluetooth interface as the delagete approved...");
                    this.bluetoothAdapter.enable();
                }
            }

            return;
        }

        //Dequeue an identified peripheral...
        BluetoothDevice peripheral = this.getIdentifiedPeripheral();

        //So -- did we find a peripheral?
        if (peripheral != null) {
            //Go and try to connect it!
            this.connectToIdentifiedPeripheral(peripheral);
        } else {
            Log.e(TAG, "Failed to find device to connect to -> restart !?");
            if (BuildConfig.DEBUG) {
                Toast.makeText(EarinApplication.getContext(), "Failed to find device to connect to", Toast.LENGTH_LONG).show();
            }
        }
    }

    public boolean isConnectingToDevice() {
        return connectingToDevice;
    }

    private void connectToIdentifiedPeripheral(BluetoothDevice device) {
        Log.d(TAG, "Try to connect to identified peripheral " + device + ", transport:  " + this.transportPreference);

        //Extract identifier...
        String identifier = device.getAddress();
        Log.d(TAG, "Extracted device identifier: " + identifier);

        //Create suitable transport, and try to establish connection!
        if (this.currentTransport == null) {
            //So -- based on our preference of transport... let's go!
            Log.d(TAG, "Create transport based on preference; " + this.transportPreference);

            switch (this.transportPreference) {
                case Spp: {
                    this.currentTransport = new SppTransport(identifier);
                    break;
                }
/*
                case Automatic:
                {
                                   //There's no transport right now -- so we don't have ongoing connection/attempts -> proceed!

                    // -- if bit 2 (0x04 mask) is set (BR/EDR Not Supported), this means that we may proceed with BLE-based connection,
                    //   ... but if it does NOT define 0x04 as set, we cannot (Android-bug!) connect via GATT (dual-mode device),
                    //   ... so let's fallback on classic SPP instead... ;(
                    if ((advertisementFlag & 0x0C) != 0)
                    {
                        //Creating BLE-transport, since the BR/EDR No supported flag is set...
                        this.currentTransport = new BleCentralTransport();
                    }
                    else
                    {
                        //Creating SPP transport
                        this.currentTransport = new SppTransport();
                    }
                    break;
                }
*/
                default:
                case Ble: {
                    this.currentTransport = BleCentralTransport.getInstance(identifier);
                    break;
                }
            }

            //Found/created a transport?
            if (this.currentTransport != null) {
                //Set delegate and GO!
                this.currentTransport.setDelegate(this);

                //Set a "suitable" timeout guard, based on what we know of the device...
                long connectionTimeout = CONNECT_DEVICE_DEFAULT_TIMEOUT_SECS;

                //If we have an assistant mapped for this device, which is in rebooting-state -- then we should wait longer...
                CapUpgradeAssistant assistant = this.upgradeAssistants.get(device.getAddress());
                if (assistant != null && assistant.isRebooting()) {
                    connectionTimeout = CONNECT_DEVICE_UPGRADE_REBOOT_TIMEOUT_SECS;
                }

                //Kick connection of transport!
                try {
                    Log.d(TAG, "Attempting to connect transport with " + connectionTimeout + " seconds timeout");
                    this.currentTransport.connect(device, connectionTimeout);
                } catch (Exception x) {
                    Log.w(TAG, "Failed connecting transport; " + x.getLocalizedMessage());

                    try {
                        //Cleanup...
                        this.currentTransport.cleanup();
                    } catch (Exception x2) {
                        Log.w(TAG, "Failed cleaning up transport; " + x2.getLocalizedMessage());
                    } finally {
                        this.currentTransport = null;
                    }

                    //Auto-resume connection...
                    this.connectToDevice();
                }
            } else {
                Log.w(TAG, "Failed creating transport. Preference =  " + this.transportPreference);

                //Auto-resume connection...
                this.connectToDevice();
            }
        } else {
            //Ignored -- already setting up transport for a discovered device...
            Log.d(TAG, "Ignored connection -- already setting up transport!");
        }
    }

    private boolean createCommunicator(AbstractTransport transport, String identifier) {
        Log.d(TAG, "Creating communicator for identifier " + identifier + ", using transport: " + transport + "  (main = " + Thread.currentThread() + ")");

        //Create the communicator...
        CapProtocol cap = new CapProtocol(transport);
        this.currentCommunicator = new CapCommunicator(identifier, cap);

        //Is it the "one" that we want to comm with?
        boolean keepDevice = true;

        if (this.delegate != null) {
            //Ask the delegate if it's ok...
            Log.d(TAG, "Asking delegate if the connected peripheral is a 'keeper' or not...");
            try {
                keepDevice = this.delegate.keepConnectedDevice(this, this.currentCommunicator);
            } catch (Exception x) {
                //Opps -- failed.. assume no then...
                Log.w(TAG, "Failed checking with delegate if conneted device was a keeper... auto-reject", x);
                keepDevice = false;
            }
        }

        //So -- match?
        if (keepDevice) {
            Log.d(TAG, "Device accepted! Proceed!");

            //Restore/create upgrade assistant...
            CapUpgradeAssistant assistant = this.upgradeAssistants.get(identifier);
            if (assistant == null) {
                //No assistant found -- create one!
                Log.d(TAG, "Creating upgrade assistant for comm");

                //Create new one...
                assistant = new CapUpgradeAssistant();

                //Ensure that the connected device is not waiting for us to finish up the upgrade...
                // that might be the case if we loose link during an OTA upgrade and never finish up...
                int upgradeIdentifier = this.currentCommunicator.getUpgradeIdentifier();
                Log.d(TAG, "Extracted device's upgrade identifier: " + upgradeIdentifier);

                if (upgradeIdentifier != 0) {
                    Log.d(TAG, "Hmmm, it seems as if this headset is not finished!");

                    //--> Restore/manually setup the assistant with params so that we will auto-resume the upgrade
                    assistant.setIdentifier(upgradeIdentifier);
                    assistant.setState(CapUpgradeAssistantState.Rebooting);
                }

                //And save it to prevent creating a new one the next time we connect (e.g., after a reboot).
                this.upgradeAssistants.put(identifier, assistant);
            }

            //Link assistant to communicator...
            assistant.setCommunicator(this.currentCommunicator);
            this.currentCommunicator.setUpgradeAssistant(assistant);

            //IFF the communicator's assistant is logically performing an update, we've most likely just
            // reconnected from an upgrade-reboot, and we should consequently resume with the upgrade process!
            if (assistant.isUpgrading()) {
                try {
                    //Auto-resume!
                    assistant.resumeUpgrade();
                } catch (Exception x) {
                    //Opps -- failed trying to resume the upgrade!?
                    Log.w(TAG, "Error trying to autio-resume upgrade process", x);
                }
            }

            //We're connected!
            Intent intent = new Intent(INTENT_ACTION_BLE_CONNECTED);
            intent.putExtra(INTENT_EXTRAS_IDENTIFER, identifier);
            broadcastManager.sendBroadcast(intent);

            //Tell the delegate...
            if (this.delegate != null) {
                this.delegate.deviceConnected(this, identifier);
                connectingToDevice = false;
            }

            return true;
        }

        //Feedback the created comm, if it needs to be used for what-ever-reason.
        return false;
    }

    public void cleanupCurrentConnection(boolean tryToReconnect, BluetoothGattStatus status) {
        Log.d(TAG, "Cleaning up current connection - if any, try to reconnect:" + tryToReconnect);
        connectingToDevice = false;

        // tell the delegate that we're disconnected
        if (this.delegate != null) {
            this.delegate.deviceDisconnected(this, "", status);
        }

        //transport, protocol, communicator...
        if (this.currentTransport != null) {
            try {
                this.currentTransport.cleanup();
            } catch (Exception x) {
                Log.w(TAG, "Error cleaning up transport; " + x.getLocalizedMessage());
            }

            //Release...
            this.currentTransport = null;
        }

        if (this.currentCommunicator != null) {
            //Tell out upgrade assistant -- if there's anyone there, that we disconnected... Hopefully with the intention to come back again... of course ;)
            CapUpgradeAssistant assistant = this.currentCommunicator.getUpgradeAssistant();
            if (assistant != null && assistant.isUpgrading()) {
                this.currentCommunicator.setUpgradeAssistant(null);
                assistant.setCommunicator(null);
            }

            //Reset other state variables...
            this.currentCommunicator = null;
            Log.d(TAG, "Transport disconnected -> currentCommunicator = null");

        }

        if (tryToReconnect) {
            this.connectToDevice();
        }

    }

    protected void discoveredPeripheral(BluetoothDevice device, int advertisementFlag, int rssi) {}

    //Methods to allow for the transport to "callback" to its delegate (controller)
    @Override
    public void transportConnected(final AbstractTransport transport, final String identifier) {
        Log.d(TAG, "Transport connected (id = " + identifier + ", Thread = " + Thread.currentThread());

        SerialExecutor.getInstance().execute(() -> {
            //Go! Try to create a communicator!
            if (createCommunicator(transport, identifier)) {
                //We got it!
                return;
            } else {
                Log.w(TAG, "Failed setting up communicator -- disconnect");

                cleanupCurrentConnection(false, BluetoothGattStatus.Unknown);
            }
        });
    }

    @Override
    public void transportFailedToConnected(AbstractTransport transport, final String identifier, Exception exception) {
        Log.d(TAG, "Transport failed to connected; " + exception);

        //Reset...
        this.cleanupCurrentConnection(exception instanceof TransportOpenException, BluetoothGattStatus.Unknown);
    }

    @Override
    public void transportDisconnected(AbstractTransport transport, final String identifier, BluetoothGattStatus status) {
        Log.d(TAG, "Transport disconnected");

//        boolean shouldAutoConnectToDevice = this.connectingToDevice;

        connectingToDevice = false;
        if (this.currentCommunicator != null) {
            //We had a communicator -- and now we've been disconnected
            Intent intent = new Intent(INTENT_ACTION_BLE_DISCONNECTED);
            intent.putExtra(INTENT_EXTRAS_IDENTIFER, identifier);
            broadcastManager.sendBroadcast(intent);

//            if (this.delegate != null) {
//                Log.d(TAG, "Transport disconnected -> currentCommunicator active -> delegate active");
//                this.delegate.deviceDisconnected(this, identifier);
//
//                //Check if there's an assistant linked to the communicator -- and if so, it the assistant is in reboot-state.
//                // If the assistant is indeed in reboot-state, then the transport disconnect we just experienced should
//                // automatically trigger a reconnection without involvement of any Ux
//                CapUpgradeAssistant assistant = this.currentCommunicator.getUpgradeAssistant();
//                if (assistant != null && assistant.isRebooting()) {
//                    //Ensure that we should try to connect again!
//                    Log.d(TAG, "Ongoing reboot found in assistant -- ensure we start to reconnect again!");
//                    shouldAutoConnectToDevice = true;
//                }
//            }

            //Cleanup..
        }
        this.cleanupCurrentConnection(false, status);
    }

    @Override
    public void transportReceivedResponseData(AbstractTransport transport, byte[] data) {
        //A transport has just received data --> Let's feed the current communicator's protocol with it...
        try {
            //Found it!
            if (this.currentCommunicator != null)
                this.currentCommunicator.getProtocol().receivedResponseData(data);
        } catch (Exception x) {
            //opps -- something failed while processing the response -- then we're dead and need to cleanup!
            Log.w(TAG, "Something died when processing the response -- cleanup!", x);

            //Cleanup..
            this.cleanupCurrentConnection(true, BluetoothGattStatus.Unknown);
        }
    }

    @Override
    public void transportReceivedEventData(AbstractTransport transport, byte[] data) {
        //A transport has just received data --> Let's feed the current communicator's protocol with it...
        try {
            //Found it!
            if (this.currentCommunicator != null)
                this.currentCommunicator.getProtocol().receivedEventData(data);
        } catch (Exception x) {
            //opps -- something failed while processing the response -- then we're dead and need to cleanup!
            Log.w(TAG, "Something died when processing event data -- cleanup!", x);

            //Cleanup..
            this.cleanupCurrentConnection(true, BluetoothGattStatus.Unknown);
        }
    }

    @Override
    public void transportReceivedUpgradeData(AbstractTransport transport, byte[] data) {
        //A transport has just received upgrade --> Let's feed the current communicator's protocol with it...
        try {
            //Found it!
            if (this.currentCommunicator != null)
                this.currentCommunicator.getProtocol().receivedUpgradeData(data);
        } catch (Exception x) {
            //opps -- something failed while processing the response -- then we're dead and need to cleanup!
            Log.w(TAG, "Something died when processing upgrade data -- cleanup!", x);

            //Cleanup..
            this.cleanupCurrentConnection(true, BluetoothGattStatus.Unknown);
        }
    }
}

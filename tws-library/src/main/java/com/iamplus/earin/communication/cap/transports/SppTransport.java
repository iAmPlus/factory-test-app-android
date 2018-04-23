package com.iamplus.earin.communication.cap.transports;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.BluetoothGattStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public class SppTransport extends AbstractTransport {
    private static final String TAG = SppTransport.class.getSimpleName();

    private final static String SERVICE_RECORD_UUID_SSP = "00001101-0000-1000-8000-00805F9B34FB";

    private BluetoothSocket sppSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BluetoothSocketListener socketListener;
    private BluetoothDevice sppDevice;

    public SppTransport(String identifier)
    {
        super(identifier);

        this.sppSocket = null;
        this.outputStream = null;
        this.inputStream = null;
        this.socketListener = null;
        this.sppDevice = null;

        BroadcastReceiver receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.d(TAG, "Received action; " + action + " for BT-device; " + device);

                //For *this* instance of the connection, or something old rubbish..?
                if (device.equals(sppDevice))
                {
                    if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
                    {
                        if (sppDevice != null && sppDevice.getAddress().equalsIgnoreCase(device.getAddress()))
                        {
                            Log.d(TAG, "Our device just disconnected! -- abort socketListener and let it cleanup...");
                            socketListener.cancel();
                        }
                    }
                }
            }
        };

        EarinApplication.getContext().registerReceiver(receiver,
                new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
    }

    @Override
    public void connect(BluetoothDevice device, long timeoutSeconds) throws Exception
    {
        Log.d(TAG, "Connecting transport");

        if (this.sppSocket == null) {

            this.sppDevice = device;

            sppSocket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(SERVICE_RECORD_UUID_SSP));
            sppSocket.connect();

            //Acquire streams...
            this.outputStream = sppSocket.getOutputStream();
            this.inputStream = sppSocket.getInputStream();

            //Kick input socket listener...
            this.socketListener = new BluetoothSocketListener();
            new Thread(this.socketListener).start();

            this.didSucceedWithConnection();
        }
        else
        {
            Log.d(TAG, "Ignore conn request -- we're already busy");
        }
    }

    @Override
    public void cleanup() throws Exception {
        Log.d(TAG, "Cleaning up transport");

        if (this.socketListener != null)
        {
            this.socketListener.cancel();
            this.socketListener = null;
        }

        if (this.sppSocket != null) {
            //Kill it!
            this.sppSocket.close();
            this.sppSocket = null;
        }

        this.sppDevice = null;
    }

    @Override
    public void writeRequestData(byte[] data) throws Exception
    {
        outputStream.write(data);
        outputStream.flush();
    }

    @Override
    public void writeUpgradeData(byte[] data) throws Exception
    {
        //But -- let's tunnel this via a CAP requests instead...
        Manager manager = Manager.getSharedManager();
        manager.getCapCommunicationController().getConnectedCommunicator().doUpgradeData(data);
    }

    private class BluetoothSocketListener implements Runnable
    {
        private boolean running;

        public BluetoothSocketListener()
        {
            //Always assume we're running when created!
            this.running = true;
        }

        public void cancel()
        {
            this.running = false;
        }

        public void run()
        {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            try
            {
                while (this.running && sppSocket != null && sppSocket.isConnected())
                {
                    //Anything available?
                    if (inputStream.available() > 0)
                    {
                        //There's something in there...
                        int bytesRead = inputStream.read(buffer);
                        if (bytesRead != -1)
                        {
                            //Feedback what we've found...
                            didReceiveResponseData(Arrays.copyOf(buffer, bytesRead));
                        }
                    }

                    Thread.yield();
                }
            }
            catch (IOException e)
            {
                //KOppp...
                Log.w(TAG, "Failed while reading data on socket; " + e.getLocalizedMessage());
            }

            //when we get here -- let's face it -- we're dead
            //--> tell delegate...
            didDisconnect(BluetoothGattStatus.Unknown);
        }
    }
}

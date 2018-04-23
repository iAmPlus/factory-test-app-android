package com.iamplus.earin.communication.cap;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Api18CapCommunicationController extends CapCommunicationController implements BluetoothAdapter.LeScanCallback
{
    private static final String TAG = Api18CapCommunicationController.class.getSimpleName();

    @Override
    protected void setupBluetoothAdapter(BluetoothAdapter adapter) {

        Log.d(TAG, "Setting up CapControl comm controller");
        //Nothin...
    }

    @Override
    protected void startScanning(final BluetoothAdapter adapter)
    {
        Log.d(TAG, "Starting scanning");

        //Start discovering, using a handler to post to the main looper...
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable()
        {
            public void run()
            {

                //Without UUID filtering since it does not work on the legacy API18 BLE impl. ;(
                adapter.startLeScan(Api18CapCommunicationController.this);
            }
        });
    }

    @Override
    protected void stopScanning(final BluetoothAdapter adapter) {

        Log.d(TAG, "Stop scanning");

        //Stop discovering, using a handler to post to the main looper...
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            public void run() {
                //Stop!
                adapter.stopLeScan(Api18CapCommunicationController.this);
            }
        });

    }

    //////////////////////////
    // LE Callback impl.

    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
    {
        //Found someting!
        this.discoveredPeripheral(device, scanRecord[2], rssi);
    }
}

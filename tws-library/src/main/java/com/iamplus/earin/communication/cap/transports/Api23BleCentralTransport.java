package com.iamplus.earin.communication.cap.transports;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;
import android.util.Log;

import com.iamplus.earin.application.EarinApplication;


/**
 * Created by Markus Millfjord on 2017-02-08.
 */
@TargetApi(Build.VERSION_CODES.M)
public class Api23BleCentralTransport extends BleCentralTransport
{
    private static final String TAG = Api23BleCentralTransport.class.getSimpleName();

    public Api23BleCentralTransport(String identifier)
    {
        super(identifier);
    }

    @Override
    public void createBond(final BluetoothDevice device) throws Exception
    {
        Log.d(TAG, "Create bond Marshmallow style");
        device.createBond();
    }

    @Override
    public BluetoothGatt connectGattOverBle(final BluetoothDevice device, final BluetoothGattCallback callback) throws Exception
    {
        Log.d(TAG, "Connecting GATT over BLE -- Marshmellow style...");
        BluetoothGatt currentGatt = device.connectGatt(EarinApplication.getContext(), false, callback, BluetoothDevice.TRANSPORT_LE);

        //Return connected GATT object...
        return currentGatt;
    }
}

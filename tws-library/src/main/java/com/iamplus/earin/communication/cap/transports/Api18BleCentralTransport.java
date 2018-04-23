package com.iamplus.earin.communication.cap.transports;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;
import android.util.Log;

import com.iamplus.earin.application.EarinApplication;

import java.lang.reflect.Method;


/**
 * Created by Markus Millfjord on 2017-02-08.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Api18BleCentralTransport extends BleCentralTransport
{
    private static final String TAG = Api18BleCentralTransport.class.getSimpleName();

    public Api18BleCentralTransport(String identifier)
    {
        super(identifier);
    }

    @Override
    public void createBond(final BluetoothDevice device) throws Exception
    {
        Log.d(TAG, "Create bond Lollipop style (reflection)...");

        //Always try to go for reflection...
        try
        {
            Log.d(TAG, "Reflection approach");

            Method createBondMethod = device.getClass().getMethod("createBond", int.class);
            int transport = device.getClass().getDeclaredField("TRANSPORT_LE").getInt(null);

            Log.d(TAG, "--> Transport = " + transport);

            createBondMethod.invoke(device, transport);
        }
        catch (NoSuchMethodException x)
        {
            Log.d(TAG, "Failed -- no such method; fallback on normal createBond and hope that works...");
            device.createBond();
        }
    }

    @Override
    public BluetoothGatt connectGattOverBle(final BluetoothDevice device, final BluetoothGattCallback callback) throws Exception
    {
        Log.d(TAG, "Connecting GATT over BLE -- Lollipop style (reflection)...");

        BluetoothGatt currentGatt = null;
/*
        //Always try to go for reflection...
        try
        {
            Log.d(TAG, "Reflection approach");

            Method connectGattMethod = device.getClass().getMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
            int transport = device.getClass().getDeclaredField("TRANSPORT_LE").getInt(null);

            Log.d(TAG, "--> Transport = " + transport);

            currentGatt = (BluetoothGatt) connectGattMethod.invoke(device, CapControlApplication.getInstance().getApplicationContext(), false, callback, transport);
        }
        catch (NoSuchMethodException x)
        {
            Log.d(TAG, "Failed -- no such method; fallback on normal connect and hope that works...");
            */
            currentGatt = device.connectGatt(EarinApplication.getContext(), false, callback);
/*        }
*/
        //Return connected GATT object...
        return currentGatt;
    }
}

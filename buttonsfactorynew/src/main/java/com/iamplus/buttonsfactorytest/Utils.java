package com.iamplus.buttonsfactorytest;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.util.Log;

import java.util.Iterator;
import java.util.Set;

public class Utils {

    public static BluetoothDevice getConnectedDeviceMAC() {

        if (!isBluetoothHeadsetConnected()) {
            Log.v("Utils","headset not connected");
            return null;
        }

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return null;
        }

        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        Iterator<BluetoothDevice> iterator = bondedDevices.iterator();
        BluetoothDevice connectedDevice = null;

        while (iterator.hasNext()) {
            BluetoothDevice item = iterator.next();
            if (item.getName().equalsIgnoreCase("Buttons with Omega") ||
                    (item.getName().contains("Cleer HALO")) ||
                    (item.getName().contains("Cleer FLOW")) ||
                    item.getName().equalsIgnoreCase("Omega Buttons")) {
                connectedDevice = item;
                break;
            }
        }

        return connectedDevice;
    }

    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }

    public static void cancelNotifications(Context context) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }
}

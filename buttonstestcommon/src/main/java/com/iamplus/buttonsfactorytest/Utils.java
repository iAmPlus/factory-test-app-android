package com.iamplus.buttonsfactorytest;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.util.Log;

import com.google.android.exoplayer2.upstream.RawResourceDataSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

    private final static int RES_IDS[] = { R.raw.buddy, R.raw.tenderness, R.raw.energy};
    private final static String RES_NAMES[] = { "buddy", "tenderness", "energy"};

    public static List<MusicController.TrackInfo> getTracks() {
        List<MusicController.TrackInfo> tracks = new ArrayList<>();
        for(int count = 0; count < RES_IDS.length; count++ ) {
            MusicController.TrackInfo track = new MusicController.TrackInfo();
            tracks.add(getTrack(count, track));
        }
        return tracks;
    }

    private static MusicController.TrackInfo getTrack(int position, MusicController.TrackInfo track){
        track.id = String.valueOf(position);
        track.title = (RES_NAMES[position]);
        track.url = makeUrl(position);
        return track;
    }

    private static String makeUrl(int index) {
        return RawResourceDataSource.buildRawResourceUri(RES_IDS[index]).toString();
    }

}

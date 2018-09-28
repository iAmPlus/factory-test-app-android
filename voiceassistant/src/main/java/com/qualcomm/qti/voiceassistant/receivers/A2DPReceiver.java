/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.receivers;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.qualcomm.qti.voiceassistant.Consts;

/**
 * <p>This class receives some information from the system. This class listen for the A2DP events.</p>
 * <p>Register this receiver with the following filters:
 * {@link BluetoothA2dp#ACTION_CONNECTION_STATE_CHANGED ACTION_CONNECTION_STATE_CHANGED}.</p>
 * <p>An example of how to register this receiver within a {@link Context Context}:</p>
 * <blockquote><pre>A2DPReceiver receiver = new A2DPReceiver(this);
 IntentFilter filter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
 context.registerReceiver(receiver, filter);</pre></blockquote>
 <p>Unregister the receiver once it is of no use.</p>
 * <br/>
 * <p>To know when the A2DP is running or stopped, a receiver for the filter
 * {@link BluetoothA2dp#ACTION_PLAYING_STATE_CHANGED ACTION_PLAYING_STATE_CHANGED} has to be implemented.</p>
 */

public class A2DPReceiver extends BroadcastReceiver {

    /**
     * <p>To show the debugging logs of this class.</p>
     */
    private static final boolean DEBUG_LOGS = Consts.Debug.A2DP_RECEIVER;
    /**
     * <p>The tag to display for logs.</p>
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String TAG = "A2DPReceiver";
    /**
     * The listener which has initiated this receiver and wishes to be updated about the A2DP Profile.
     */
    private final A2DPListener mListener;

    /**
     * The constructor of this class.
     *
     * @param listener
     *            the object which wants to receive updates from this receiver.
     */
    public A2DPReceiver(A2DPListener listener) {
        this.mListener = listener;
    }

    @Override // BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action != null && action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothAdapter.ERROR);
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (state == BluetoothProfile.STATE_CONNECTED) {
                if (DEBUG_LOGS) {
                    Log.d(TAG, "A2DP connected with device " + (device != null ? device.getName() : "null"));
                }
                mListener.onA2DPDeviceConnected(device);
            }
            else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                if (DEBUG_LOGS) {
                    Log.d(TAG, "A2DP disconnected from device " + (device != null ? device.getName() : "null"));
                }
                mListener.onA2DPDeviceDisconnected(device);
            }
        }
    }

    /**
     * The interface used to communicate with this object.
     */
    public interface A2DPListener {
        /**
         * <p>To inform the listener that the A2DP profile is now connected with the given device.</p>
         * <p>The A2DP profile is connected when the "media" option is enabled for the device and
         * that the device is currently connected with the Android system for media sessions.</p>
         *
         * @param device
         *          The device for which the A2DP capacity is enabled.
         */
        void onA2DPDeviceConnected(BluetoothDevice device);
        /**
         * <p>To inform that the A2DP profile of the given device had been disconnected. The device is either
         * disconnected form the Android system or does not have anymore the "media" option had been disabled.</p>
         *
         * @param device
         *          The device which does not have the A2DP capacity enabled anymore.
         */
        void onA2DPDeviceDisconnected(BluetoothDevice device);
    }

}

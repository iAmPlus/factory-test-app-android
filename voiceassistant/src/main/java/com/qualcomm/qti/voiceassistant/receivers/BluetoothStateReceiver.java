/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.receivers;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.qualcomm.qti.voiceassistant.Consts;

/**
 * <p>This class receives some information from the system. This class listens for the Bluetooth state of the Android
 * device.</p>
 * <p>Register this receiver with the following filters:
 * {@link BluetoothAdapter#ACTION_STATE_CHANGED ACTION_STATE_CHANGED}.</p>
 * <p>An example of how to register this receiver within a {@link Context Context}:</p>
 * <blockquote><pre>BluetoothStateReceiver receiver = new BluetoothStateReceiver(this);
 IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
 context.registerReceiver(receiver, filter);</pre></blockquote>
 <p>Unregister the receiver once it is of no use.</p>
 */

public class BluetoothStateReceiver extends BroadcastReceiver {

    /**
     * <p>To show the debugging logs of this class.</p>
     */
    private static final boolean DEBUG_LOGS = Consts.Debug.BLUETOOTH_STATE_RECEIVER;
    /**
     * <p>The tag to display for logs.</p>
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String TAG = "BluetoothStateReceiver";
    /**
     * The listener which has initiated this receiver and wishes to be updated about the Bluetooth state.
     */
    private final BluetoothStateListener mListener;

    /**
     * The constructor of this class.
     *
     * @param listener
     *            the object which wants to receive updates from this receiver.
     */
    public BluetoothStateReceiver(BluetoothStateListener listener) {
        this.mListener = listener;
    }

    @Override // BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_OFF) {
                if (DEBUG_LOGS) {
                    Log.d(TAG, "Bluetooth is disabled");
                }
                mListener.onBluetoothDisabled();
            }
            else if (state == BluetoothAdapter.STATE_ON) {
                if (DEBUG_LOGS) {
                    Log.d(TAG, "Bluetooth is enabled");
                }
                mListener.onBluetoothEnabled();
            }
        }
    }

    /**
     * The interface used to communicate with this object.
     */
    public interface BluetoothStateListener {
        /**
         * <p>To inform the listener that the Bluetooth feature of the Android device had been disabled.</p>
         */
        void onBluetoothDisabled();
        /**
         * <p>To inform the listener that the Bluetooth feature of the Android device had been enabled.</p>
         */
        void onBluetoothEnabled();
    }

}

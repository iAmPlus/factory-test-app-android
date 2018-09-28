/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.qualcomm.qti.voiceassistant.Consts;

/**
 * This class allows to receive information from the system about Phone state information.
 * <p>This class receives some information from the system. This class listen for the phone call events.</p>
 * <p>Register this receiver with the following filters:
 * {@link TelephonyManager#ACTION_PHONE_STATE_CHANGED ACTION_CONNECTION_STATE_CHANGED}.</p>
 * <p>An example of how to register this receiver within a {@link Context Context}:</p>
 * <blockquote><pre>CallReceiver receiver = new CallReceiver(this);
 IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
 context.registerReceiver(receiver, filter);</pre></blockquote>
 <p>When this receiver is not of use anymore, call {@link #release()} to release resources and after unregister the
 receiver.</p>
 */
public class CallReceiver extends BroadcastReceiver {

    /**
     * <p>To show the debugging logs of this class.</p>
     */
    private static final boolean DEBUG_LOGS = Consts.Debug.CALL_RECEIVER;
    /**
     * <p>The tag to display for logs.</p>
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String TAG = "CallReceiver";
    /**
     * <p>The TelephonyManager to interact with to get information about any phone call.</p>
     */
    private TelephonyManager telephony;
    /**
     * <p>The listener which receives information about phone calls.</p>
     */
    private final PhoneStateListener mPhoneStateListener;
    /**
     * <p>The listener which has initiated this receiver and wishes to be updated about phone call states.</p>
     */
    private final CallListener mListener;

    /**
     * <p>The constructor of this class.</p>
     *
     * @param listener
     *              Any object which implements the {@link CallListener IncomingCallListener} interface which
     *              wishes to receive updates from this receiver.
     */
    public CallReceiver(CallListener listener) {
        mListener = listener;
        mPhoneStateListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (DEBUG_LOGS) {
                            Log.d(TAG, "call state: IDLE");
                        }
                        mListener.onACall(false);
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (DEBUG_LOGS) {
                            Log.d(TAG, "call state: OFF HOOK");
                        }
                        mListener.onACall(true);
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (DEBUG_LOGS) {
                            Log.d(TAG, "call state: RINGING");
                        }
                        mListener.onACall(true);
                        break;
                }
            }
        };
    }

    @Override // BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * <p>When this object is unregistered this method must be called to release resources.</p>
     */
    public void release() {
        if (telephony != null) telephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    /**
     * The interface used to communicate with this object.
     */
    public interface CallListener {
        /**
         * <p>To get information about an incoming call.</p>
         *
         * @param calling
         *              True if the Android device is in a call (running, incoming, outgoing), false when the Android
         *              device is not anymore in a call.
         */
        void onACall(boolean calling);
    }
}

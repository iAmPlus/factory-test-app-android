/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant;

import android.app.Activity;
import android.content.Intent;

import com.qualcomm.qti.voiceassistant.assistant.LoopbackAssistant;

/**
 * <p>This final class encapsulates all constants used in the application.</p>
 */
public final class Consts {
    /**
     * To display or hide the debug logs of the corresponding classes.
     */
    public static final class Debug {
        /**
         * <p>To display or hide the logs for the
         * {@link com.qualcomm.qti.voiceassistant.service.VoiceAssistantService VoiceAssistantService} class.</p>
         */
        public static final boolean VOICE_ASSISTANT_SERVICE = false;
        /**
         * <p>To display or hide the logs for the
         * {@link LoopbackAssistant LoopbackAssistant} class.</p>
         */
        public static final boolean LOOPBACK_ASSISTANT = true;
        /**
         * <p>To display or hide the logs for the
         * {@link com.qualcomm.qti.voiceassistant.receivers.A2DPReceiver A2DPReceiver} class.</p>
         */
        public static final boolean A2DP_RECEIVER = true;
        /**
         * <p>To display or hide the logs for the
         * {@link com.qualcomm.qti.voiceassistant.receivers.BluetoothStateReceiver BluetoothStateReceiver} class.</p>
         */
        public static final boolean BLUETOOTH_STATE_RECEIVER = true;
        /**
         * <p>To display or hide the logs for the
         * {@link com.qualcomm.qti.voiceassistant.receivers.CallReceiver CallReceiver} class.</p>
         */
        public static final boolean CALL_RECEIVER = true;
    }

    /**
     * The code used by the methods {@link android.app.Activity#onActivityResult(int, int, Intent) onActivityResult()}
     * and {@link Activity#startActivityForResult(Intent, int) startActivityForResult()} when requesting to enable
     * the device Bluetooth.
     */
    public static final int ACTION_REQUEST_ENABLE_BLUETOOTH = 101;

    /**
     * The code used by the methods {@link android.app.Activity#onRequestPermissionsResult(int, String[], int[])}
     * onRequestPermissionsResult()} and
     * {@link android.support.v4.app.ActivityCompat#requestPermissions(Activity, String[], int) requestPermissions()}
     * when requesting to the user to enable required permissions.
     */
    public static final int ACTION_REQUEST_PERMISSIONS = 102;
}

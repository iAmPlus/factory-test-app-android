/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant;

import android.annotation.SuppressLint;
import android.support.annotation.IntDef;

import com.qualcomm.qti.libraries.assistant.Assistant;
import com.qualcomm.qti.libraries.assistant.AssistantEnums;
import com.qualcomm.qti.voiceassistant.assistant.LoopbackAssistant;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class contains all the different enumerations used between classes of this app module to communicate
 * some information.
 */
public class Enums {

    // ====== ENUMS ========================================================================

    /**
     * <p>Enumerates all the different assistants this application supports.</p>
     */
    @IntDef(flag = true, value = { AssistantType.LOOPBACK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AssistantType {
        /**
         * <p>This assistant is an example assistant which plays what it has received.</p>
         * <p>This type is implemented by the
         * {@link LoopbackAssistant LoopbackAssistant} class.</p>
         */
        int LOOPBACK = 1;
    }

    /**
     * <p>Enumerates all exceptions and errors which can occur within a
     * {@link LoopbackAssistant LoopbackAssistant}.</p>
     */
    @IntDef({AssistantError.NOT_INITIALISED, AssistantError.NO_RESPONSE, AssistantError.PLAYING_RESPONSE_FAILED })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AssistantError {
        /**
         * <p>Occurs when trying to use the assistant prior to call {@link Assistant#init() init()}.<br/>
         * This can also happen when the account had been logged off or the assistant reset after an error.</p>
         */
        int NOT_INITIALISED = AssistantEnums.AssistantError.NOT_INITIALISED; // 0
        /**
         * @see AssistantEnums.AssistantError#NO_RESPONSE
         */
        int NO_RESPONSE = AssistantEnums.AssistantError.NO_RESPONSE; // 1
        /**
         * <p>Occurs when playing the audio answer of the assistant fails.</p>
         */
        int PLAYING_RESPONSE_FAILED = 5;
    }

    /**
     * <p>All types of messages which are sent from the
     * {@link com.qualcomm.qti.voiceassistant.service.VoiceAssistantService VoiceAssistantService} to any handler
     * attached to it.</p>
     * <p>These messages are sent within a {@link android.os.Message Message} and contained within
     * <code>{@link android.os.Message#what what}</code>.</p>
     */
    @IntDef(flag = true, value = {ServiceMessage.DEVICE, ServiceMessage.ASSISTANT, ServiceMessage.ACTION,
            ServiceMessage.SESSION, ServiceMessage.ERROR })
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface ServiceMessage {
        /**
         * <p>To inform about some device information such as the connection state, the ivor state, etc.</p>
         * <p>This type of {@link android.os.Message Message} also contains the type of device message as one of
         * {@link DeviceMessage}. This information is contained in
         * <code>{@link android.os.Message#arg1 arg1}</code>.</p>
         */
        int DEVICE = 0;
        /**
         * <p>To inform about some assistant information such as the current state,  etc.</p>
         * <p>This type of {@link android.os.Message Message} also contains the type of assistant message as one of
         * {@link AssistantMessage}. This information is contained in
         * <code>{@link android.os.Message#arg1 arg1}</code>.</p>
         */
        int ASSISTANT = 1;
        /**
         * <p>To inform about some actions the user should do in order to get the assistant feature working.</p>
         * <p>This type of {@link android.os.Message Message} also contains the type of action as one of
         * {@link ActionMessage}. This information is contained in
         * <code>{@link android.os.Message#arg1 arg1}</code>.</p>
         */
        int ACTION = 2;
        /**
         * <p>To inform about the state of the session. A session corresponds to a running assistant request.</p>
         * <p>This type of {@link android.os.Message Message} also contains the state of the session as one of
         * {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.SessionState SessionState}. This information is
         * contained in <code>{@link android.os.Message#obj obj}</code>.</p>
         */
        int SESSION = 3;
        /**
         * <p>To inform that an error occurs with either the assistant, the Bluetooth device or from any required
         * Android component such as the network for instance.</p>
         * <p>This type of {@link android.os.Message Message} also contains the state of error as one of
         * {@link ErrorType}. This information is contained in <code>{@link android.os.Message#arg1 arg1}</code>.</p>
         */
        int ERROR = 4;
    }

    /**
     * <p>All types of {@link ServiceMessage#DEVICE DEVICE} messages which are sent from the
     * {@link com.qualcomm.qti.voiceassistant.service.VoiceAssistantService VoiceAssistantService} to any handler
     * attached to it.</p>
     * <p>These messages are sent within a {@link android.os.Message Message} which its
     * <code>{@link android.os.Message#what what}</code> value is {@link ServiceMessage#DEVICE DEVICE}. The
     * DeviceMessage is contained within <code>{@link android.os.Message#arg1 arg1}</code>.</p>
     */
    @IntDef(flag = true, value = { DeviceMessage.CONNECTION_STATE, DeviceMessage.IVOR_STATE,
            DeviceMessage.DEVICE_INFORMATION})
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface DeviceMessage {
        /**
         * <p>To inform that the connection state with the selected device has changed.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#DEVICE DEVICE}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = CONNECTION_STATE.</li>
         *     <li><code>{@link android.os.Message#obj obj}</code> = the connection state of the device as:
         *     {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.ConnectionState#CONNECTED CONNECTED},
         *     {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.ConnectionState#CONNECTING CONNECTING},
         *     {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.ConnectionState#DISCONNECTING DISCONNECTING}
         *     or
         *     {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.ConnectionState#DISCONNECTED DISCONNECTED}
         *     .</li>
         * </ul>
         */
        int CONNECTION_STATE = 0;

        /**
         * <p>To inform that the IVOR state of the connected device has changed - the IVOR state represents the
         * assistant state of the device as known by this application.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#DEVICE DEVICE}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = IVOR_STATE.</li>
         *     <li><code>{@link android.os.Message#obj obj}</code> = the IVOR state of the device as one of
         *     {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorState IvorState}.</li>
         * </ul>
         */
        int IVOR_STATE = 1;

        /**
         * <p>Contains the BluetoothDevice this application is connected with/had been connected with.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#DEVICE DEVICE}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = DEVICE_INFORMATION.</li>
         *     <li><code>{@link android.os.Message#obj obj}</code> = a
         *     {@link android.bluetooth.BluetoothDevice BluetoothDevice}.</li>
         * </ul>
         */
        int DEVICE_INFORMATION = 2;
    }

    /**
     * <p>All types of {@link ServiceMessage#ASSISTANT ASSISTANT} messages which are sent from the
     * {@link com.qualcomm.qti.voiceassistant.service.VoiceAssistantService VoiceAssistantService} to any handler
     * attached to it.</p>
     * <p>These messages are sent within a {@link android.os.Message Message} which its
     * <code>{@link android.os.Message#what what}</code> value is {@link ServiceMessage#ASSISTANT ASSISTANT}. The
     * AssistantMessage is contained within <code>{@link android.os.Message#arg1 arg1}</code>.</p>
     */
    @IntDef(flag = true, value = { AssistantMessage.STATE, AssistantMessage.TYPE })
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface AssistantMessage {
        /**
         * <p>To inform that the assistant state of the {@link Assistant Assistant} has changed.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#ASSISTANT ASSISTANT}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = STATE.</li>
         *     <li><code>{@link android.os.Message#obj obj}</code> = the
         *     {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.AssistantState AssistantState}.</li>
         * </ul>
         */
        int STATE = 0;
        /**
         * <p>To inform about the type of assistant which is configured.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#ASSISTANT ASSISTANT}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = TYPE.</li>
         *     <li><code>{@link android.os.Message#obj obj}</code> = the {@link AssistantType AssistantType}.</li>
         * </ul>
         */
        int TYPE = 1;
    }

    /**
     * <p>All types of {@link ServiceMessage#ACTION ACTION} messages which are sent from the
     * {@link com.qualcomm.qti.voiceassistant.service.VoiceAssistantService VoiceAssistantService} to any handler
     * attached to it.</p>
     * <p>These messages are sent within a {@link android.os.Message Message} which its
     * <code>{@link android.os.Message#what what}</code> value is {@link ServiceMessage#ACTION ACTION}. The
     * ActionMessage is contained within <code>{@link android.os.Message#arg1 arg1}</code>.</p>
     */
    @IntDef(flag = true, value = { ActionMessage.ENABLE_BLUETOOTH, ActionMessage.CONNECT_A_DEVICE,
            ActionMessage.SELECT_A_DEVICE })
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface ActionMessage {
        /**
         * <p>To request the user to enable the Bluetooth feature within the Android device.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#ACTION ACTION}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = ENABLE_BLUETOOTH.</li>
         * </ul>
         */
        int ENABLE_BLUETOOTH = 0;
        /**
         * <p>To request the user to connect a Bluetooth device with the Android device.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#ACTION ACTION}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = CONNECT_A_DEVICE.</li>
         * </ul>
         */
        int CONNECT_A_DEVICE = 1;
        /**
         * <p>To request the user to select the device to connect with. This also provides the list of devices to
         * connect with.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#ACTION ACTION}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = SELECT_A_DEVICE.</li>
         *     <li><code>{@link android.os.Message#obj obj}</code> =
         *     {@link java.util.List List}<{@link android.bluetooth.BluetoothDevice BluetoothDevice}></li>
         * </ul>
         */
        int SELECT_A_DEVICE = 2;
    }

    /**
     * <p>All types of {@link ServiceMessage#ERROR ERROR} messages which are sent from the
     * {@link com.qualcomm.qti.voiceassistant.service.VoiceAssistantService VoiceAssistantService} to any handler
     * attached to it.</p>
     * <p>These messages are sent within a {@link android.os.Message Message} which its
     * <code>{@link android.os.Message#what what}</code> value is {@link ServiceMessage#ERROR ERROR}. The
     * ErrorType is contained within <code>{@link android.os.Message#arg1 arg1}</code>.</p>
     */
    @IntDef(flag = true, value = { ErrorType.ASSISTANT, ErrorType.DEVICE, ErrorType.IVOR, ErrorType.CALL })
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface ErrorType {
        /**
         * <p>To inform about an {@link AssistantError AssistantError} which occurred with the assistant.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#ERROR ERROR}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = ASSISTANT.</li>
         *     <li><code>{@link android.os.Message#arg2 arg2}</code> = {@link AssistantError AssistantError}.</li>
         * </ul>
         */
        int ASSISTANT = 0;
        /**
         * <p>To inform about an {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.DeviceError DeviceError}
         * which occurred with the Bluetooth device.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#ERROR ERROR}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = DEVICE.</li>
         *     <li><code>{@link android.os.Message#arg2 arg2}</code> =
         *     {@link AssistantEnums.DeviceError DeviceError}.</li>
         *     <li><code>{@link android.os.Message#obj obj}</code> = contains the number of packets which had been
         *     received when <li><code>{@link android.os.Message#arg2 arg2}</code> is
         *     {@link AssistantEnums.DeviceError#DATA_STREAM_STOPPED DATA_STREAM_STOPPED}.</li>
         * </ul>
         */
        int DEVICE = 1;
        /**
         * <p>To inform about an {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorError IvorError}
         * which occurred while using the assistant protocol to communicate with the Bluetooth device.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#ERROR ERROR}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = IVOR.</li>
         *     <li><code>{@link android.os.Message#obj obj}</code> =
         *     {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorError IvorError}.</li>
         * </ul>
         */
        int IVOR = 2;
        /**
         * <p>To inform that a current running assistant session has stopped due to an incoming or outgoing call.</p>
         * <p>This type of {@link android.os.Message Message} is as follows:</p>
         * <ul>
         *     <li><code>{@link android.os.Message#what what}</code> = {@link ServiceMessage#ERROR ERROR}.</li>
         *     <li><code>{@link android.os.Message#arg1 arg1}</code> = CALL.</li>
         * </ul>
         */
        int CALL = 3;
    }


    // ====== STATIC METHODS ========================================================================

    /**
     * <p>To get a human readable label for the given {@link ServiceMessage}.</p>
     * <p>This method gets a {@link ServiceMessage} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param message
     *          The {@link ServiceMessage} to get a label for.
     *
     * @return A human readable label for the <code>message</code>.
     */
    @SuppressWarnings("unused")
    public static String getServiceLabel(@ServiceMessage int message) {
        switch (message) {
            case ServiceMessage.ACTION:
                return "ACTION";
            case ServiceMessage.ASSISTANT:
                return "ASSISTANT";
            case ServiceMessage.DEVICE:
                return "DEVICE_INFORMATION";
            case ServiceMessage.SESSION:
                return "SESSION";
            case ServiceMessage.ERROR:
                return "ERROR";
            default:
                return "UNKNOWN (" + message + ")";
        }
    }

    /**
     * <p>To get a human readable label for the given {@link ActionMessage}.</p>
     * <p>This method gets a {@link ActionMessage} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param action
     *          The {@link ActionMessage} to get a label for.
     *
     * @return A human readable label for the <code>action</code>.
     */
    @SuppressWarnings("unused")
    public static String getActionLabel(@ActionMessage int action) {
        switch (action) {
            case ActionMessage.CONNECT_A_DEVICE:
                return "CONNECT_A_DEVICE";
            case ActionMessage.ENABLE_BLUETOOTH:
                return "ENABLE_BLUETOOTH";
            case ActionMessage.SELECT_A_DEVICE:
                return "SELECT_A_DEVICE";
            default:
                return "UNKNOWN (" + action + ")";
        }
    }

    /**
     * <p>To get a human readable label for the given {@link AssistantMessage}.</p>
     * <p>This method gets a {@link AssistantMessage} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param assistant
     *          The {@link AssistantMessage} to get a label for.
     *
     * @return A human readable label for the <code>assistant</code>.
     */
    @SuppressWarnings("unused")
    public static String getAssistantLabel(@AssistantMessage int assistant) {
        switch (assistant) {
            case AssistantMessage.STATE:
                return "STATE";
            case AssistantMessage.TYPE:
                return "TYPE";
            default:
                return "UNKNOWN (" + assistant + ")";
        }
    }

    /**
     * <p>To get a human readable label for the given {@link DeviceMessage}.</p>
     * <p>This method gets a {@link DeviceMessage} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param message
     *          The {@link DeviceMessage} to get a label for.
     *
     * @return A human readable label for the <code>message</code>.
     */
    @SuppressWarnings("unused")
    public static String getDeviceMessageLabel(@DeviceMessage int message) {
        switch (message) {
            case DeviceMessage.CONNECTION_STATE:
                return "CONNECTION_STATE";
            case DeviceMessage.DEVICE_INFORMATION:
                return "DEVICE_INFORMATION";
            case DeviceMessage.IVOR_STATE:
                return "IVOR_STATE";
            default:
                return "UNKNOWN (" + message + ")";
        }
    }

    /**
     * <p>To get a human readable label for the given {@link AssistantError}.</p>
     * <p>This method gets a {@link AssistantError} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param error
     *          The {@link AssistantError} to get a label for.
     *
     * @return A human readable label for the <code>error</code>.
     */
    public static String getAssistantErrorLabel(@AssistantError int error) {
        switch (error) {
            case AssistantError.NOT_INITIALISED:
                return "NOT_INITIALISED";
            case AssistantError.PLAYING_RESPONSE_FAILED:
                return "PLAYING_RESPONSE_FAILED";
            case AssistantError.NO_RESPONSE:
                return "NO_RESPONSE";
            default:
                return "UNKNOWN (" + error + ")";
        }
    }

    /**
     * <p>To get a human readable label for the given {@link ErrorType}.</p>
     * <p>This method gets a {@link ErrorType} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param type
     *          The {@link ErrorType} to get a label for.
     *
     * @return A human readable label for the <code>type</code>.
     */
    @SuppressWarnings("unused")
    public static String getErrorMessageLabel(@ErrorType int type) {
        switch (type) {
            case ErrorType.ASSISTANT:
                return "ASSISTANT";
            case ErrorType.DEVICE:
                return "DEVICE";
            case ErrorType.IVOR:
                return "IVOR";
            case ErrorType.CALL:
                return "CALL";
            default:
                return "UNKNOWN (" + type + ")";
        }
    }

    /**
     * <p>To get a human readable label for the given {@link AssistantType}.</p>
     * <p>This method gets a {@link AssistantType} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param type
     *          The {@link AssistantType} to get a label for.
     *
     * @return A human readable label for the <code>type</code>.
     */
    public static String getAssistantTypeLabel(@AssistantType int type) {
        switch (type) {
            case AssistantType.LOOPBACK:
                return "LOOPBACK";
            default:
                return "UNKNOWN (" + type + ")";
        }
    }
}

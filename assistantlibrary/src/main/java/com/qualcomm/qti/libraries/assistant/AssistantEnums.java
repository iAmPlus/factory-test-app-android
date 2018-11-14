/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.libraries.assistant;

import android.annotation.SuppressLint;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class contains all the different enumerations used between classes of this module to communicate
 * some information.
 */
public class AssistantEnums {

    // ====== STATES ========================================================================

    /**
     * <p>Enumerates all the possible values for the Bluetooth connection state of a Bluetooth device.</p>
     */
    @IntDef({ ConnectionState.CONNECTED, ConnectionState.DISCONNECTED, ConnectionState.CONNECTING,
            ConnectionState.DISCONNECTING })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConnectionState {
        int DISCONNECTED = 0;
        int CONNECTING = 1;
        int CONNECTED = 2;
        int DISCONNECTING = 3;
    }

    /**
     * <p>Enumerates all the states an {@link Assistant} can have.</p>
     */
    @IntDef({ AssistantState.UNAVAILABLE, AssistantState.INITIALISING, AssistantState.STREAMING, AssistantState.PENDING,
            AssistantState.SPEAKING, AssistantState.IDLE, AssistantState.STARTING, AssistantState.CANCELLING, 
            AssistantState.CLOSING, AssistantState.ENDING_STREAMING })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AssistantState {
        /**
         * <p>The assistant is in this state when it cannot be used - for instance it has not be initialised by
         * calling {@link Assistant#init() init()}.</p>
         */
        int UNAVAILABLE = 0;
        /**
         * <p>When in this state an assistant cannot be used as it is initialising its resources.</p>
         * <p>The assistant is in this state after {@link Assistant#init() init()} had been called.</p>
         */
        int INITIALISING = 1;
        /**
         * <p>The state of the assistant while it is sending the data to its service in charge of analysing the data
         * .</p>
         */
        int STREAMING = 3;
        /**
         * <p>The assistant uses this state when it has stopped the streaming - or the streaming had been stopped -
         * and is waiting for a response from its service in charge of analysing the data.</p>
         */
        int PENDING = 4;
        /**
         * <p>The assistant uses this state when it is playing the response which corresponds to the voice data it
         * has received.</p>
         */
        int SPEAKING = 5;
        /**
         * <p>The assistant is in this state when it is ready to start a data voice session.</p>
         */
        int IDLE = 6;
        /**
         * <p>The assistant is in this state when it has receiving a request to start a voice data stream and is
         * preparing its resources to do so.</p>
         */
        int STARTING = 7;
        /**
         * <p>The state to use when cancelling a request, a session.</p>
         */
        int CANCELLING = 8;
        /**
         * <p>The state to use when {@link Assistant#close() close()} had been called and the assistant cannot be
         * used anymore.</p>
         */
        int CLOSING = 9;
        /**
         * <p>Used when {@link Assistant#endDataStream() endDataStream} had been called and the assistant is
         * stopping the streaming of the voice data.</p>
         */
        int ENDING_STREAMING = 10;
    }

    /**
     * <p>Enumerates all states of a voice session.</p>
     */
    @IntDef({ SessionState.RUNNING, SessionState.READY,
            SessionState.UNAVAILABLE, SessionState.CANCELLING
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SessionState {
        int READY = 10;
        int RUNNING = 11;
        int CANCELLING = 12;
        int UNAVAILABLE = 14;
    }

    /**
     * <p>Enumerates all states of the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager}.</p>
     */
    @IntDef({ IvorState.UNAVAILABLE, IvorState.IDLE, IvorState.VOICE_REQUESTED, IvorState.VOICE_REQUESTING, IvorState.VOICE_ENDED,
            IvorState.VOICE_ENDING, IvorState.ANSWER_ENDING, IvorState.ANSWER_PLAYING, IvorState.ANSWER_STARTING, IvorState.CANCELLING,
            IvorState.SESSION_STARTED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface IvorState {
        /**
         * <p>Used when the device is disconnected or the version of the assistant feature supported by the device is
         * not supported by this version of the
         * {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager}.</p>
         */
        int UNAVAILABLE = 0;
        /**
         * <p>Used when the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} is waiting for
         * either the device or the {@link Assistant Assistant} to start a session.</p>
         */
        int IDLE = 1;
        /**
         * <p>Used by the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} when it has
         * requested the voice to be sent to the device and is waiting for the device to validate the request.</p>
         */
        int VOICE_REQUESTING = 2;
        /**
         * <p>Used by the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} when the device
         * has validated the request to send some voice data.</p>
         */
        int VOICE_REQUESTED = 3;
        /**
         * <p>Used by the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} when it has
         * requested the device to stop to send the voice and is waiting for the device to acknowledge this request.</p>
         */
        int VOICE_ENDING = 4;
        /**
         * <p>Used by the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} when the device
         * has acknowledged to stop to send the voice data.</p>
         */
        int VOICE_ENDED = 5;
        /**
         * <p>Used by the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} when it has
         * informed the device the audio response from the assistant has started to be played and is waiting for the
         * device to acknowledge this information.</p>
         */
        int ANSWER_STARTING = 6;
        /**
         * <p>Used by the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} while the
         * assistant response is being played and once the device has acknowledged that the response has started to
         * be played.</p>
         */
        int ANSWER_PLAYING = 7;
        /**
         * <p>Used by the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} when it has
         * informed the device the audio response from the assistant has finished to be played and is waiting for the
         * device to acknowledge this information.</p>
         */
        int ANSWER_ENDING = 8;
        /**
         * <p>Used by the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} when it is
         * cancelling the session by sending a cancel request to the device and waiting for the device to acknowledge
         * the request.</p>
         */
        int CANCELLING = 9;
        /**
         * <p>Used by the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} when the device
         * has requested a voice session to be started.</p>
         */
        int SESSION_STARTED = 10;
    }


    // ====== ERRORS ========================================================================

    /**
     * <p>Enumerates all errors which can occur within a
     * {@link Assistant Assistant}.</p>
     */
    @IntDef({AssistantError.NOT_INITIALISED, AssistantError.NO_RESPONSE })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AssistantError {
        /**
         * <p>Occurs when trying to use the assistant prior to call
         * {@link Assistant#init() init()}.</p>
         */
        int NOT_INITIALISED = 0;
        /**
         * <p>Occurs when the assistant has not given a response within the set up time out.</p>
         */
        int NO_RESPONSE = 1;
    }

    /**
     * <p>Enumerates all errors related to the device which are not IVOR related.</p>
     */
    @IntDef({DeviceError.CONNECTION_FAILED, DeviceError.CONNECTION_LOST, DeviceError.DATA_STREAM_STOPPED })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DeviceError {
        /**
         * <p>This error is thrown when the asynchronous connection process fails.</p>
         */
        int CONNECTION_FAILED = 0x80;
        /**
         * <p>This error is thrown when the connection is lost while listening through an ongoing connection.</p>
         */
        int CONNECTION_LOST = 0x81;
        /**
         * <p>This error occurs when the device has not send any data within 1 second while streaming the data voice
         * .</p>
         * <p>When this error occurs the current voice session is cancelled.</p>
         */
        int DATA_STREAM_STOPPED = 0x82;
    }

    /**
     * <p>Enumerates all IVOR errors/reasons which are communicated between the application and the connected
     * Bluetooth device.</p>
     */
    @IntDef({ IvorError.CANCELLED_BY_USER, IvorError.UNAVAILABLE, IvorError.CALL,
            IvorError.INCORRECT_STATE, IvorError.NOT_INITIALISED, IvorError.PLAYING_RESPONSE_FAILED,
            IvorError.REQUEST_FAILED, IvorError.UNEXPECTED_ERROR })
    @Retention(RetentionPolicy.SOURCE)
    public @interface IvorError {
        /**
         * <p>Occurs when the user themselves cancels the assistant/voice session, this could be by pressing a button
         * within the application or on the Bluetooth device for instance.</p>
         */
        int CANCELLED_BY_USER           = 0x00;
        /**
         * <p>Occurs when none of the defined other errors matches with the current error.</p>
         */
        int UNEXPECTED_ERROR            = 0x01;
        /**
         * <p>Occurs when the application or the device attempts to set up a voice session but one of them is not
         * initialised yet.</p>
         */
        int NOT_INITIALISED             = 0x02;
        /**
         * <p>Occurs when the {@link Assistant Assistant} fails to answer the user request.</p>
         */
        int REQUEST_FAILED              = 0x03;
        /**
         * <p>Occurs when the device or the application encounter an incorrect state during a session.</p>
         * <p>When this occurs a reset should be forced into the IVOR manager or the Assistant.</p>
         */
        int INCORRECT_STATE             = 0x04;
        /**
         * <p>Occurs when playing the assistant response from the user's request failed.</p>
         */
        int PLAYING_RESPONSE_FAILED     = 0x05;
        /**
         * <p>When the assistant is unavailable, for instance an Internet connection is required and it is
         * disconnected.</p>
         */
        int UNAVAILABLE                 = 0x06;
        /**
         * <p>Occurs when an outgoing or incoming call happens during a voice session.</p>
         */
        int CALL                        = 0x07;
    }


    // ====== MESSAGES ========================================================================

    /**
     * <p>All types of messages the
     * {@link com.qualcomm.qti.libraries.assistant.bluetooth.GaiaBREDRProvider GaiaBREDRProvider} can throw to a
     * registered listener.</p>
     */
    @IntDef(flag = true, value = { ProviderMessage.CONNECTION_STATE_HAS_CHANGED, ProviderMessage.ERROR,
            ProviderMessage.GAIA_READY, ProviderMessage.IVOR_MESSAGE })
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface ProviderMessage {

        /**
         * <p>To inform that the connection state with the given device has changed.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         *     <li>The connection state of the device as: {@link ConnectionState#CONNECTED CONNECTED},
         *     {@link ConnectionState#CONNECTING CONNECTING},
         *     {@link ConnectionState#DISCONNECTING DISCONNECTING} or
         *     {@link ConnectionState#DISCONNECTED DISCONNECTED}. This information is contained in
         *     <code>{@link android.os.Message#obj obj}</code>.</li>
         *     </ul>
         */
        int CONNECTION_STATE_HAS_CHANGED = 0;

        /**
         * <p>To inform about any unexpected error which occurs during an ongoing connection or the connection
         * process itself.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         *     <li>The type of error as one of the {@link DeviceError Errors} values. This information is contained in
         *     <code>{@link android.os.Message#arg1 msg.arg1}</code>.</li>
         * </ul>
         */
        int ERROR = 2;

        /**
         * <p>This message is used to let the application know it can now communicate with the device using the
         * GAIA/IVOR protocol.</p>
         * <p>This message does not contain any other information.</p>
         */
        int GAIA_READY = 3;

        /**
         * <p>To request an action from the application as requested by the device.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         *     <li>The type of IVOR message as one of the {@link IvorMessage IvorMessage} values. This information is
         *     contained in
         *     <code>{@link android.os.Message#arg1 msg.arg1}</code>.</li>
         *     <li>Any complementary information to the IVOR message. This information is
         *     contained in <code>{@link android.os.Message#obj msg.obj}</code>.</li>
         * </ul>
         */
        int IVOR_MESSAGE = 4;
    }

    /**
     * <p>Enumerates all IVOR requests which can happen during a voice session.</p>
     */
    @IntDef(flag = true, value = { IvorMessage.START_SESSION, IvorMessage.VOICE_DATA,
            IvorMessage.CANCEL_SESSION, IvorMessage.VOICE_END, IvorMessage.STATE })
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface IvorMessage {
        /**
         * <p>To inform that the device wants to start a voice session.</p>
         * <p>This type of message does not contain complementary information.</p>
         */
        int START_SESSION = 0;
        /**
         * <p>To transfer the voice data bytes the device has transmitted through the Bluetooth connection.</p>
         * <p>This message also contains the voice data as an object of type <code>byte[]</code>.</p>
         */
        int VOICE_DATA = 1;
        /**
         * <p>To inform that the session is cancelled, either by the device or the
         * {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager}.</p>
         * <p>This message also contains an object of type <code>int</code> as one of the values from
         * {@link IvorError IvorError}.</p>
         */
        int CANCEL_SESSION = 2;
        /**
         * <p>To inform that the device is stopping to send the data voice.</p>
         */
        int VOICE_END = 3;
        /**
         * <p>To inform that the {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} state has
         * changed.</p>
         * <p>This message also contains an object of type <code>int</code> as one of the values from
         * {@link IvorState IvorState}.</p>
         */
        int STATE = 4;

        int USER_VOICE_END = 5;

    }


    // ====== STATIC METHODS ========================================================================

    /**
     * <p>To get a human readable label for the given {@link AssistantState}.</p>
     * <p>This method gets a {@link AssistantState} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param state
     *          The {@link AssistantState} to get a label for.
     *
     * @return A human readable label for the <code>error</code>.
     */
    public static String getAssistantStateLabel(@AssistantState int state) {
        switch (state) {
            case AssistantState.INITIALISING:
                return "INITIALISING";
            case AssistantState.CANCELLING:
                return "CANCELLING";
            case AssistantState.CLOSING:
                return "CLOSING";
            case AssistantState.ENDING_STREAMING:
                return "ENDING_STREAMING";
            case AssistantState.IDLE:
                return "IDLE";
            case AssistantState.PENDING:
                return "PENDING";
            case AssistantState.SPEAKING:
                return "SPEAKING";
            case AssistantState.STARTING:
                return "STARTING";
            case AssistantState.STREAMING:
                return "STREAMING";
            case AssistantState.UNAVAILABLE:
                return "UNAVAILABLE";
            default:
                return "UNKNOWN (" + state + ")";
        }
    }

    /**
     * <p>To get a human readable label for the given {@link ConnectionState}.</p>
     * <p>This method gets a {@link ConnectionState} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param state
     *          The {@link ConnectionState} to get a label for.
     *
     * @return A human readable label for the <code>error</code>.
     */
    public static String getConnectionStateLabel(@ConnectionState int state) {
        switch (state) {
            case ConnectionState.CONNECTED:
                return "CONNECTED";
            case ConnectionState.CONNECTING:
                return "CONNECTING";
            case ConnectionState.DISCONNECTED:
                return "DISCONNECTED";
            case ConnectionState.DISCONNECTING:
                return "DISCONNECTING";
            default:
                return "UNKNOWN (" + state + ")";
        }
    }

    /**
     * <p>To get a human readable label for the given {@link SessionState}.</p>
     * <p>This method gets a {@link SessionState} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param state
     *          The {@link SessionState} to get a label for.
     *
     * @return A human readable label for the <code>error</code>.
     */
    public static String getSessionStateLabel(@SessionState int state) {
        switch (state) {
            case SessionState.CANCELLING:
                return "CANCELLING";
            case SessionState.READY:
                return "READY";
            case SessionState.RUNNING:
                return "RUNNING";
            case SessionState.UNAVAILABLE:
                return "UNAVAILABLE";
            default:
                return "UNKNOWN (" + state + ")";
        }
    }

    /**
     * <p>To get a human readable label for the given {@link DeviceError}.</p>
     * <p>This method gets a {@link DeviceError} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param error
     *          The {@link DeviceError} to get a label for.
     *
     * @return A human readable label for the <code>error</code>.
     */
    public static String getDeviceErrorLabel(@DeviceError int error) {
        switch (error) {
            case DeviceError.CONNECTION_FAILED:
                return "CONNECTION_FAILED";
            case DeviceError.CONNECTION_LOST:
                return "CONNECTION_LOST";
            case DeviceError.DATA_STREAM_STOPPED:
                return "DATA_STREAM_STOPPED";
            default:
                return "UNKNOWN (" + error + ")";
        }
    }

    /**
     * <p>To get a human readable label for the given {@link IvorState}.</p>
     * <p>This method gets a {@link IvorState} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param state
     *          The {@link IvorState} to get a label for.
     *
     * @return A human readable label for the <code>error</code>.
     */
    public static String getIvorStateLabel(@IvorState int state) {
        switch (state) {
            case IvorState.ANSWER_ENDING:
                return "ANSWER_ENDING";
            case IvorState.ANSWER_PLAYING:
                return "ANSWER_PLAYING";
            case IvorState.ANSWER_STARTING:
                return "ANSWER_STARTING";
            case IvorState.IDLE:
                return "IDLE";
            case IvorState.VOICE_REQUESTING:
                return "VOICE_REQUESTING";
            case IvorState.VOICE_REQUESTED:
                return "VOICE_REQUESTED";
            case IvorState.UNAVAILABLE:
                return "UNAVAILABLE";
            case IvorState.VOICE_ENDED:
                return "VOICE_ENDED";
            case IvorState.VOICE_ENDING:
                return "VOICE_ENDING";
            case IvorState.CANCELLING:
                return "CANCELLING";
            case IvorState.SESSION_STARTED:
                return "SESSION_STARTED";
        }

        return "UNKNOWN (" + state + ")";
    }

    /**
     * <p>To get a human readable label for the given {@link IvorError}.</p>
     * <p>This method gets a {@link IvorError} and returns a corresponding <code>String</code> which can be
     * displayed within a user interface or some logs.</p>
     *
     * @param error
     *          The {@link IvorError} to get a label for.
     *
     * @return A human readable label for the <code>error</code>.
     */
    public static String getIvorErrorLabel(int error) {
        switch (error) {
            case IvorError.CANCELLED_BY_USER:
                return "CANCELLED_BY_USER";
            case IvorError.CALL:
                return "CALL";
            case IvorError.INCORRECT_STATE:
                return "INCORRECT_STATE";
            case IvorError.NOT_INITIALISED:
                return "NOT_INITIALISED";
            case IvorError.PLAYING_RESPONSE_FAILED:
                return "PLAYING_RESPONSE_FAILED";
            case IvorError.REQUEST_FAILED:
                return "REQUEST_FAILED";
            case IvorError.UNAVAILABLE:
                return "UNAVAILABLE";
            case IvorError.UNEXPECTED_ERROR:
                return "UNEXPECTED_ERROR";
            default:
                return "UNKNOWN (" + error + ")";
        }
    }
}

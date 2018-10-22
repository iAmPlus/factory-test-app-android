/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.libraries.assistant.ivor;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaAcknowledgementRequest;
import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaPacket;
import com.csr.gaia.library.GaiaRequest;
import com.csr.gaia.library.exceptions.GaiaFrameException;
import com.csr.gaiacontrol.Callback;
import com.csr.gaiacontrol.Controller;
import com.csr.gaiacontrol.SimpleCallback;
import com.csr.gaiacontrol.internal.ButtonsManager;
import com.qualcomm.qti.libraries.assistant.AssistantConsts;
import com.qualcomm.qti.libraries.assistant.AssistantEnums;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>This class follows the Gaia protocol. It manages all IVOR messages which are sent and received over the protocol
 * for the Voice Assistant feature.</p>
 */
public class IvorManager {

    // ====== PRIVATE FIELDS =======================================================================

    /**
     * <p>The tag to display for logs.</p>
     */
    /**
     * <p>An array map which groups all Runnable for requests which have sent a GAIA packet and are waiting for the
     * corresponding acknowledgement packet.</p>
     */
    private final ArrayMap<Integer, LinkedList<TimeOutRequestRunnable>> mTimeOutRequestRunnableMap = new ArrayMap<>();

    private final String TAG = "IvorManager";
    /**
     * To show the debugging logs of this class.
     */
    private static final boolean DEBUG_LOGS = AssistantConsts.Debug.IVOR_MANAGER;
    /**
     * <p>The IVOR version supported by this manager.</p>
     * <p>This array contains: {major, minor} to complete with the structure of the IVOR command
     * {@link Gaia#COMMAND_IVOR_CHECK_VERSION COMMAND_IVOR_CHECK_VERSION}.</p>
     */
    private static final byte[] VERSION = { 0x01, 0x00 };

    private final Handler mListener;
    private final Controller mController;
    /**
     * <p>The current IVOR state of this manager.</p>
     */
    private @AssistantEnums.IvorState int mState = AssistantEnums.IvorState.UNAVAILABLE;

    private @AssistantEnums.ConnectionState int mConnectionState = AssistantEnums.ConnectionState.DISCONNECTED;

    private final Handler mHandler = new Handler();


    // ====== CONSTRUCTOR =======================================================================

    /**
     * <p>Main constructor of this class to initiate the manager.</p>
     *
     * @param listener
     */
    public IvorManager(@NonNull Handler listener) {
        super();
        this.mListener = listener;
        logState("new instance");
        mController = Controller.getInstance();
        mController.registerListener(mCallback);
    }


    // ====== PUBLIC METHODS =======================================================================

    /**
     * <p>To get the current IVOR state of this manager.</p>
     *
     * @return the current IVOR state.
     */
    public @AssistantEnums.IvorState int getState() {
        return mState;
    }

    /**
     * <p>Called by the application to cancel a voice assistant session if there is an ongoing one.</p>
     */
    public void cancelSession(@AssistantEnums.IvorError int error) {
        logState("Session cancelled, error is " + AssistantEnums.getIvorErrorLabel(error));

        switch (mState) {
            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.VOICE_REQUESTING:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.SESSION_STARTED:
                setState(AssistantEnums.IvorState.CANCELLING);
                sendIvorCancel(error);
                setState(AssistantEnums.IvorState.IDLE); // no need to wait for the ACK
                break;

            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.IDLE:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.ANSWER_ENDING:
                Log.w(TAG, "onIvorError: Ignored, state is " + AssistantEnums.getIvorStateLabel(mState));
                break;
        }
    }

    /**
     * <p>Called by the application to cancel a voice assistant session if there is an ongoing one.</p>
     *
     * @param isConnected
     *          True if there is a connected device to send a cancel command to.
     */
    public void forceReset(boolean isConnected) {
        logState("Force reset called, isConnected=" + isConnected);
        if (isConnected) {
            setState(AssistantEnums.IvorState.CANCELLING);
            sendIvorCancel(AssistantEnums.IvorError.INCORRECT_STATE);
            setState(AssistantEnums.IvorState.IDLE); // no need to wait for the ACK
        }
        else {
            setState(AssistantEnums.IvorState.UNAVAILABLE);
        }
    }


    /**
     * <p>To force a reset of the communication with the device. This method resets the IVOR manager and reconnects to
     * the device if it was disconnected.</p>
     */
    public void forceReset() {
        forceReset(getConnectionState() == AssistantEnums.ConnectionState.CONNECTED);
        if (getConnectionState() == AssistantEnums.ConnectionState.DISCONNECTED && mController.getBluetoothDevice() != null) {
            mController.establishGAIAConnection();
        }
    }

    public synchronized @AssistantEnums.ConnectionState int getConnectionState() {
        return mConnectionState;
    }

    public boolean disconnect() {
        if (mConnectionState == AssistantEnums.ConnectionState.DISCONNECTED) {
            Log.w(TAG, "disconnection failed: no device connected.");
            return false;
        }
        mController.disconnect();
        setConnectionState(AssistantEnums.ConnectionState.DISCONNECTED);

        Log.i(TAG, "Provider disconnected from BluetoothDevice " + (mController.getBluetoothDevice() != null ? mController.getBluetoothDevice().getAddress() : "null"));

        return true;
    }

    private synchronized void setConnectionState(@AssistantEnums.ConnectionState int state) {
        mConnectionState = state;
        mListener.obtainMessage(AssistantEnums.ProviderMessage.CONNECTION_STATE_HAS_CHANGED, state).sendToTarget();
        if (state != AssistantEnums.ConnectionState.CONNECTED) {
            reset();
        }
    }

    /**
     * <p>To stop the device to stream the voice data. This method stops the streaming when the current state
     * is {@link AssistantEnums.IvorState#VOICE_REQUESTED VOICE_REQUESTED} or
     * {@link AssistantEnums.IvorState#VOICE_REQUESTING VOICE_REQUESTING} and sets up the state to
     * {@link AssistantEnums.IvorState#VOICE_ENDING VOICE_ENDING}.</p>
     */
    public void stopVoiceStreaming() {
        logState("Application requests STOP voice streaming");

        switch (mState) {
            case AssistantEnums.IvorState.VOICE_REQUESTING:
                // if called before the device has acknowledged the VOICE_DATA_REQUEST command
            case AssistantEnums.IvorState.VOICE_REQUESTED:
                setState(AssistantEnums.IvorState.VOICE_ENDING);
                sendIvorVoiceEnd();
                break;

            case AssistantEnums.IvorState.VOICE_ENDING:
                Log.i(TAG, "stopVoiceStreaming: already in VOICE_ENDING state.");
                break;

            case AssistantEnums.IvorState.SESSION_STARTED:
            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.IDLE:
                Log.w(TAG, "stopVoiceStreaming: Ignored, state is " + AssistantEnums.getIvorStateLabel(mState));
                break;
        }
    }

    /**
     * <p>This method requests the device to start to stream some voice data. This is only possible when the device
     * has requested to start a voice session or if it is called while waiting for or playing an audio answer.</p>
     */
    public boolean startVoiceStreaming() {
        logState("Application requests START voice streaming");

        switch (mState) {
            case AssistantEnums.IvorState.SESSION_STARTED:
            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.IDLE:
                setState(AssistantEnums.IvorState.VOICE_REQUESTING);
                sendIvorDataRequest();
                return true;

            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
            case AssistantEnums.IvorState.VOICE_REQUESTING:
            default:
                Log.w(TAG, "startVoiceStreaming: Ignored, state is " + AssistantEnums.getIvorStateLabel(mState));
                return false;
        }
    }

    /**
     * <p>To inform the device that the assistant has started to play its audio answer.</p>
     */
    public void onStartPlayingAnswer() {
        logState("Application starts to play the answer");

        switch (mState) {
            case AssistantEnums.IvorState.VOICE_ENDING:
                // if called before the device has acknowledged the VOICE_END command
            case AssistantEnums.IvorState.VOICE_ENDED:
                setState(AssistantEnums.IvorState.ANSWER_STARTING);
                sendIvorAnswerStart();
                break;

            case AssistantEnums.IvorState.ANSWER_STARTING:
                Log.i(TAG, "onStartPlayingAnswer: already in ANSWER_STARTING state.");
                break;

            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.IDLE:
            case AssistantEnums.IvorState.SESSION_STARTED:
            case AssistantEnums.IvorState.VOICE_REQUESTING:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.CANCELLING:
                Log.w(TAG, "onStartPlayingAnswer: Ignored, state is " + AssistantEnums.getIvorStateLabel(mState));
                break;
        }
    }

    /**
     * <p>To inform the device that the assistant has finished to play its audio answer.</p>
     */
    public void onFinishPlayingAnswer() {
        logState("Application has finished to play the answer");

        switch (mState) {
            case AssistantEnums.IvorState.ANSWER_STARTING:
                // if called before the device has acknowledged the ANSWER_START command
            case AssistantEnums.IvorState.ANSWER_PLAYING:
                setState(AssistantEnums.IvorState.ANSWER_ENDING);
                sendIvorAnswerEnd();
                break;

            case AssistantEnums.IvorState.ANSWER_ENDING:
                Log.i(TAG, "onFinishPlayingAnswer: already in ANSWER_ENDING state.");
                break;

            case AssistantEnums.IvorState.IDLE:
            case AssistantEnums.IvorState.VOICE_REQUESTING:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.SESSION_STARTED:
            case AssistantEnums.IvorState.VOICE_ENDED:
                Log.w(TAG, "onFinishPlayingAnswer: Ignored, state is " + AssistantEnums.getIvorStateLabel(mState));
                break;
        }
    }


    // ====== PROTECTED METHODS ====================================================================

    protected void receiveSuccessfulAcknowledgement(GaiaPacket packet) {
        switch (packet.getCommand()) {
            case Gaia.COMMAND_IVOR_VOICE_DATA_REQUEST:
                receiveSuccessfulIvorVoiceDataRequestACK();
                break;

            case Gaia.COMMAND_IVOR_VOICE_END:
                receiveSuccessfulIvorVoiceEndACK();
                break;

            case Gaia.COMMAND_IVOR_ANSWER_START:
                receiveSuccessfulIvorAnswerStartACK();
                break;

            case Gaia.COMMAND_IVOR_ANSWER_END:
                receiveSuccessfulIvorAnswerEndACK();
                break;
        }
    }

    protected void receiveUnsuccessfulAcknowledgement(GaiaPacket packet) {
        switch (packet.getCommand()) {
            case Gaia.COMMAND_IVOR_VOICE_DATA_REQUEST:
                if (mState == AssistantEnums.IvorState.VOICE_REQUESTING) {
                    Log.w(TAG, "Cancelling session: unsuccessful acknowledgement for COMMAND_IVOR_VOICE_DATA_REQUEST");
                    cancelSession(AssistantEnums.IvorError.UNEXPECTED_ERROR);
                    mListener.obtainMessage(AssistantEnums.ProviderMessage.IVOR_MESSAGE,
                            AssistantEnums.IvorMessage.CANCEL_SESSION, 0, AssistantEnums.IvorError.UNEXPECTED_ERROR).sendToTarget();
                }
            case Gaia.COMMAND_IVOR_VOICE_END:
                if (mState == AssistantEnums.IvorState.VOICE_ENDING) {
                    setState(AssistantEnums.IvorState.IDLE);
                }
                break;
        }
    }

    protected void hasNotReceivedAcknowledgementPacket(int command) {
        switch (command) {
            case Gaia.COMMAND_IVOR_VOICE_DATA_REQUEST:
                if (mState == AssistantEnums.IvorState.VOICE_REQUESTING) {
                    Log.w(TAG, "Cancelling session: no acknowledgement for COMMAND_IVOR_VOICE_DATA_REQUEST");
                    cancelSession(AssistantEnums.IvorError.UNEXPECTED_ERROR);
                    mListener.obtainMessage(AssistantEnums.ProviderMessage.IVOR_MESSAGE,
                            AssistantEnums.IvorMessage.CANCEL_SESSION, 0, AssistantEnums.IvorError.UNEXPECTED_ERROR).sendToTarget();
                }
                break;
        }
    }

    protected boolean manageReceivedPacket(GaiaPacket packet) {
        switch (packet.getCommand()) {
            case Gaia.COMMAND_IVOR_START:
                return receiveIvorStart(packet);

            case Gaia.COMMAND_IVOR_VOICE_DATA:
                return receiveIvorVoiceData(packet);

            case Gaia.COMMAND_IVOR_CHECK_VERSION:
                return receiveIvorCheckVersion(packet);

            case Gaia.COMMAND_IVOR_CANCEL:
                return receiveIvorCancel(packet);
            
            case Gaia.COMMAND_IVOR_VOICE_END:
                return receiveVoiceEnd(packet);
        }

        return false;
    }

    protected void onSendingFailed(GaiaPacket packet) {
        // nothing to do: the failure is connection related, the GaiaBREDRProvider resets everything
        switch (mState) {
            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.SESSION_STARTED:
            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
            case AssistantEnums.IvorState.VOICE_REQUESTING:
            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.IDLE:
                Log.w(TAG, "Sending failed while in state " + AssistantEnums.getIvorStateLabel(mState));
                break;
        }
    }

    public void reset() {
        logState("reset manager");
        resetTimeOutRequestRunnableMap();
        setState(AssistantEnums.IvorState.UNAVAILABLE);
    }


    // ====== PRIVATE METHODS - RECEIVING =============================================================

    /**
     * <p>This method is called when this manager receives a successful acknowledgement to its voice data request.</p>
     * <p>This method updates the IVOR state from {@link AssistantEnums.IvorState#VOICE_REQUESTING VOICE_REQUESTING} to
     * {@link AssistantEnums.IvorState#VOICE_REQUESTED VOICE_REQUESTED}.</p>
     */
    private void receiveSuccessfulIvorVoiceDataRequestACK() {
        logState("receive ACK for VOICE DATA REQUEST");

        switch (mState) {
            case AssistantEnums.IvorState.VOICE_REQUESTING:
                setState(AssistantEnums.IvorState.VOICE_REQUESTED);
                break;

            case AssistantEnums.IvorState.VOICE_REQUESTED:
                Log.i(TAG, "receiveSuccessfulIvorVoiceDataRequestACK: already in VOICE_REQUESTED state.");
                break;

            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.IDLE:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.SESSION_STARTED:
                Log.w(TAG, "receiveSuccessfulIvorVoiceDataRequestACK: Incorrect state, state is "
                        + AssistantEnums.getIvorStateLabel(mState));
                break;
        }
    }

    /**
     * <p>This method is called when this manager receives a successful acknowledgement to its voice end request.</p>
     * <p>This method updates the IVOR state from {@link AssistantEnums.IvorState#VOICE_ENDING VOICE_ENDING} to
     * {@link AssistantEnums.IvorState#VOICE_ENDED VOICE_ENDED}.</p>
     */
    private void receiveSuccessfulIvorVoiceEndACK() {
        logState("receive ACK for VOICE END");

        switch (mState) {
            case AssistantEnums.IvorState.VOICE_ENDING:
                setState(AssistantEnums.IvorState.VOICE_ENDED);
                break;

            case AssistantEnums.IvorState.VOICE_ENDED:
                Log.i(TAG, "receiveSuccessfulIvorVoiceEndACK: already in VOICE_ENDED state.");
                break;

            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.IDLE:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
            case AssistantEnums.IvorState.VOICE_REQUESTING:
            case AssistantEnums.IvorState.SESSION_STARTED:
                Log.w(TAG, "receiveSuccessfulIvorVoiceEndACK: Incorrect state, state is " + AssistantEnums.getIvorStateLabel(mState));
                break;
        }
    }

    /**
     * <p>This method is called when this manager receives a successful acknowledgement to its answer start request.</p>
     * <p>This method updates the IVOR state from {@link AssistantEnums.IvorState#ANSWER_STARTING ANSWER_STARTING} to
     * {@link AssistantEnums.IvorState#ANSWER_PLAYING ANSWER_PLAYING}.</p>
     */
    private void receiveSuccessfulIvorAnswerStartACK() {
        logState("receive ACK for ANSWER START");

        switch (mState) {
            case AssistantEnums.IvorState.ANSWER_STARTING:
                setState(AssistantEnums.IvorState.ANSWER_PLAYING);
                break;

            case AssistantEnums.IvorState.ANSWER_PLAYING:
                Log.i(TAG, "receiveSuccessfulIvorAnswerStartACK: already in ANSWER_PLAYING state.");
                break;

            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.IDLE:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
            case AssistantEnums.IvorState.VOICE_REQUESTING:
            case AssistantEnums.IvorState.SESSION_STARTED:
                Log.w(TAG, "receiveSuccessfulIvorAnswerStartACK: Incorrect state, state is " + AssistantEnums.getIvorStateLabel(mState));
                break;
        }
    }

    /**
     * <p>This method is called when this manager receives a successful acknowledgement to its answer end request.</p>
     * <p>This method updates the IVOR state from {@link AssistantEnums.IvorState#ANSWER_ENDING ANSWER_ENDING} to
     * {@link AssistantEnums.IvorState#IDLE IDLE}.</p>
     */
    private void receiveSuccessfulIvorAnswerEndACK() {
        logState("receive ACK for ANSWER END");

        switch (mState) {
            case AssistantEnums.IvorState.ANSWER_ENDING:
                setState(AssistantEnums.IvorState.IDLE);
                break;

            case AssistantEnums.IvorState.IDLE:
                Log.i(TAG, "receiveSuccessfulIvorAnswerEndACK: already in IDLE state.");
                break;


            case AssistantEnums.IvorState.VOICE_REQUESTED:
            case AssistantEnums.IvorState.VOICE_REQUESTING:
                // can happen if isSpeechExpected was set
                // nothing to do
                break;

            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.SESSION_STARTED:
                Log.w(TAG, "receiveSuccessfulIvorAnswerEndACK: Incorrect state, state is "
                        + AssistantEnums.getIvorStateLabel(mState));
                break;
        }
    }

    /**
     *
     * @param packet The {@link Gaia#COMMAND_IVOR_START COMMAND_IVOR_START} packet which had been received.
     *
     * @return True if the packet had been acknowledged by the method.
     */
    private boolean receiveIvorStart(GaiaPacket packet) {
        logState("receive START");

        switch (mState) {
            case AssistantEnums.IvorState.IDLE:
                setState(AssistantEnums.IvorState.SESSION_STARTED);
                createAcknowledgmentRequest(packet, Gaia.Status.SUCCESS, null);
                mListener.obtainMessage(AssistantEnums.ProviderMessage.IVOR_MESSAGE,
                        AssistantEnums.IvorMessage.START_SESSION,0, null).sendToTarget();
                return true;

            case AssistantEnums.IvorState.UNAVAILABLE:
                createAcknowledgmentRequest(packet, Gaia.Status.NOT_SUPPORTED, null);
                Log.w(TAG, "receiveIvorStart: Voice assistant is UNAVAILABLE.");
                return true;

            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
            case AssistantEnums.IvorState.VOICE_REQUESTING:
            case AssistantEnums.IvorState.SESSION_STARTED:
                // start session requested while there is an ongoing session.
                createAcknowledgmentRequest(packet, Gaia.Status.INCORRECT_STATE, null);
                Log.w(TAG, "receiveIvorStart: Incorrect state, state is " + AssistantEnums.getIvorStateLabel(mState));
                return true;
        }

        return false;
    }

    /**
     * <p>This method is called when this manager receives a
     * {@link Gaia#COMMAND_IVOR_VOICE_DATA COMMAND_IVOR_VOICE_DATA} packet.</p>
     * <p>If this manager is expecting some voice data, this method transmits the received payload to its listener by
     * for some voice data when it is in one of the following states:
     * {@link AssistantEnums.IvorState#VOICE_REQUESTING VOICE_REQUESTING} or
     * {@link AssistantEnums.IvorState#VOICE_REQUESTED VOICE_REQUESTED}.</p>
     * <p>Otherwise it ignores the packet.</p>
     *
     * @param packet The {@link Gaia#COMMAND_IVOR_VOICE_DATA COMMAND_IVOR_VOICE_DATA} packet which has been received.
     *
     * @return True if the packet was acknowledged by the method.
     */
    private boolean receiveIvorVoiceData(GaiaPacket packet) {
        switch (mState) {
            case AssistantEnums.IvorState.VOICE_REQUESTING:
                setState(AssistantEnums.IvorState.VOICE_REQUESTED);
            case AssistantEnums.IvorState.VOICE_REQUESTED:
                // No acknowledgement for voice data during a session
                mListener.obtainMessage(AssistantEnums.ProviderMessage.IVOR_MESSAGE,
                        AssistantEnums.IvorMessage.VOICE_DATA, 0, packet.getPayload());
                return true;

            case AssistantEnums.IvorState.UNAVAILABLE:
                createAcknowledgmentRequest(packet, Gaia.Status.NOT_SUPPORTED, null);
                Log.w(TAG, "receiveIvorVoiceData: Voice assistant is UNAVAILABLE.");
                return true;

            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.IDLE:
            case AssistantEnums.IvorState.SESSION_STARTED:
                // start session requested while there is an ongoing session.
            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.CANCELLING:
                // device still processing some data, ignoring the packet
                Log.i(TAG, "receiveIvorVoiceData: packet ignored, state is "
                        + AssistantEnums.getIvorStateLabel(mState));
                return true;
        }

        return false;
    }

    /**
     * <p>This method is called when this manager receives a
     * {@link Gaia#COMMAND_IVOR_CHECK_VERSION COMMAND_IVOR_CHECK_VERSION} packet.</p>
     * <p>This method checks if the sent major and minor versions correspond to the one supported by this manager
     * and answers accordingly to the device.</p>
     * <p>If the version is not supported, an unsuccessful acknowledgement packet is sent with the status
     * {@link Gaia.Status#INVALID_PARAMETER INVALID_PARAMETER}.</p>
     *
     * @param packet The {@link Gaia#COMMAND_IVOR_CHECK_VERSION COMMAND_IVOR_CHECK_VERSION} packet which had been
     * received.
     *
     * @return True if the packet was acknowledged by the method.
     */
    @SuppressWarnings("SameReturnValue")
    private boolean receiveIvorCheckVersion(GaiaPacket packet) {
        logState("receive CHECK VERSION");

        // get the device version
        byte[] payload = packet.getPayload();
        boolean supported = Arrays.equals(payload, VERSION);

        // act depending on the support
        if (supported && mState == AssistantEnums.IvorState.UNAVAILABLE) {
            setState(AssistantEnums.IvorState.IDLE);

            // other states: no change to the behavior. If there is an ongoing session it continues
        }
        else if (!supported) {
            if (mState != AssistantEnums.IvorState.IDLE && mState != AssistantEnums.IvorState.UNAVAILABLE) {
                // there is an ongoing session to cancel
                cancelSession(AssistantEnums.IvorError.UNAVAILABLE);
                mListener.obtainMessage(AssistantEnums.ProviderMessage.IVOR_MESSAGE,
                        AssistantEnums.IvorMessage.CANCEL_SESSION, 0, AssistantEnums.IvorError.UNAVAILABLE).sendToTarget();
            }
            setState(AssistantEnums.IvorState.UNAVAILABLE);
        }

        // acknowledge the support
        Gaia.Status status = supported ? Gaia.Status.SUCCESS : Gaia.Status.INVALID_PARAMETER;
        createAcknowledgmentRequest(packet, status, null);
        return true;
    }

    /**
     * <p>This method is called when this manager receives a
     * {@link Gaia#COMMAND_IVOR_VOICE_END COMMAND_IVOR_VOICE_END} packet.</p>
     * <p>If this manager is expecting some voice data, this method changes its state to
     * {@link AssistantEnums.IvorState#VOICE_ENDED VOICE_ENDED} and informs the listener that the device has ended
     * the voice streaming.</p>
     * <p>Otherwise it ignores the packet.</p>
     *
     * @param packet The {@link Gaia#COMMAND_IVOR_VOICE_END COMMAND_IVOR_VOICE_END} packet which had been
     * received.
     *
     * @return True if the packet was acknowledged by the method.
     */
    private boolean receiveVoiceEnd(GaiaPacket packet) {
        logState("Receive VOICE END");

        switch (mState) {
            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.CANCELLING:
            case AssistantEnums.IvorState.IDLE:
            case AssistantEnums.IvorState.UNAVAILABLE:
            case AssistantEnums.IvorState.VOICE_REQUESTING:
            case AssistantEnums.IvorState.SESSION_STARTED:
                createAcknowledgmentRequest(packet, Gaia.Status.INCORRECT_STATE, null);
                return true;

            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
                createAcknowledgmentRequest(packet, Gaia.Status.SUCCESS, null);
                setState(AssistantEnums.IvorState.VOICE_ENDED);
                mListener.obtainMessage(AssistantEnums.ProviderMessage.IVOR_MESSAGE, AssistantEnums.IvorMessage.VOICE_END, 0, null);
                return true;
        }
        
        return false;
    }

    /**
     * <p>This method is called when this manager receives a
     * {@link Gaia#COMMAND_IVOR_CANCEL COMMAND_IVOR_CANCEL} packet.</p>
     * <p>If this manager is currently in an assistant session, this method cancels the session and informs the
     * <p>Otherwise it sends an {@link Gaia.Status#INCORRECT_STATE INCORRECT_STATE}
     * or {@link Gaia.Status#NOT_SUPPORTED NOT_SUPPORTED} packet.</p>
     *
     * @param packet The {@link Gaia#COMMAND_IVOR_CANCEL COMMAND_IVOR_CANCEL} packet which had been
     * received.
     *
     * @return True if the packet had been acknowledged acknowledged by the method.
     */
    private boolean receiveIvorCancel(GaiaPacket packet) {
        logState("Receive CANCEL");

        int PAYLOAD_LENGTH = 1;
        int ERROR_OFFSET= 0;
        byte[] payload = packet.getPayload();
        int error = payload.length >= PAYLOAD_LENGTH ? payload[ERROR_OFFSET] : 0x01;

        switch (mState) {
            case AssistantEnums.IvorState.VOICE_REQUESTING:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.ANSWER_STARTING:
            case AssistantEnums.IvorState.ANSWER_PLAYING:
            case AssistantEnums.IvorState.SESSION_STARTED:
                setState(AssistantEnums.IvorState.CANCELLING);
                createAcknowledgmentRequest(packet, Gaia.Status.SUCCESS, null);
                Log.w(TAG, "Device sends IVOR_CANCEL, cancelling the session, reason="
                        + AssistantEnums.getIvorErrorLabel(error));
                mListener.obtainMessage(AssistantEnums.ProviderMessage.IVOR_MESSAGE,
                        AssistantEnums.IvorMessage.CANCEL_SESSION, 0, error).sendToTarget();
                setState(AssistantEnums.IvorState.IDLE);
                return true;

            case AssistantEnums.IvorState.UNAVAILABLE:
                createAcknowledgmentRequest(packet, Gaia.Status.NOT_SUPPORTED, null);
                Log.w(TAG, "receiveIvorCancel(" + AssistantEnums.getIvorErrorLabel(error)
                        + "): Voice assistant is UNAVAILABLE.");
                return true;


            case AssistantEnums.IvorState.CANCELLING:
                // already within the cancel process.
                createAcknowledgmentRequest(packet, Gaia.Status.SUCCESS, null);
                Log.w(TAG, "receiveIvorCancel(" + AssistantEnums.getIvorErrorLabel(error)
                        + "): Voice assistant is already in CANCELLING state.");
                return true;

            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.IDLE:
                // cancel session requested while there is no more an ongoing session.
                createAcknowledgmentRequest(packet, Gaia.Status.INCORRECT_STATE, null);
                Log.w(TAG, "receiveIvorCancel: Incorrect state, state is " + AssistantEnums.getIvorStateLabel(mState));
                return true;
        }

        return false;
    }

    /**
     * <p>To create an acknowledgement GAIA request to send a packet over the listener.</p>
     *
     * @param packet
     *            The packet to acknowledge over the listener.
     */
    protected void createAcknowledgmentRequest(GaiaPacket packet, Gaia.Status status, @Nullable byte[] data) {
        Log.d(TAG, "Received request to send an acknowledgement packet for command: "
                    + (packet.getCommand()) + " with status: "
                    + (status));

        mController.getGaiaLink().sendAcknowledgement(packet, status);
    }



    // ====== PRIVATE METHODS - SENDING =============================================================

    /**
     * <p>This method builds an {@link Gaia#COMMAND_IVOR_CANCEL COMMAND_IVOR_CANCEL} packet and sends the packet to
     * the device.</p>
     *
     * @param error the reason of the cancel.
     */
    private void sendIvorCancel(@AssistantEnums.IvorError int error) {
        logState("Sending IVOR cancel " + AssistantEnums.getIvorErrorLabel(error));
        final int PAYLOAD_LENGTH = 1;
        final int ERROR_OFFSET = 0;
        byte[] payload = new byte[PAYLOAD_LENGTH];
        payload[ERROR_OFFSET] = (byte) error;
        mController.getGaiaLink().sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_IVOR_CANCEL, payload);
        startTimeOutRequestRunnable(Gaia.COMMAND_IVOR_CANCEL);
    }

    /**
     * <p>This method builds an {@link Gaia#COMMAND_IVOR_VOICE_DATA_REQUEST COMMAND_IVOR_VOICE_DATA_REQUEST} packet and
     * sends the packet to the device.</p>
     */
    private void sendIvorDataRequest() {
        startTimeOutRequestRunnable(Gaia.COMMAND_IVOR_VOICE_DATA_REQUEST);
        mController.getGaiaLink().sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_IVOR_VOICE_DATA_REQUEST);
    }

    /**
     * <p>This method builds an {@link Gaia#COMMAND_IVOR_VOICE_END COMMAND_IVOR_VOICE_END} packet and
     * sends the packet to the device.</p>
     */
    private void sendIvorVoiceEnd() {
        mController.getGaiaLink().sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_IVOR_VOICE_END);
        startTimeOutRequestRunnable(Gaia.COMMAND_IVOR_VOICE_END);

    }

    /**
     * <p>This method builds an {@link Gaia#COMMAND_IVOR_ANSWER_START COMMAND_IVOR_ANSWER_START} packet and
     * sends the packet to the device.</p>
     */
    private void sendIvorAnswerStart() {
        mController.getGaiaLink().sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_IVOR_ANSWER_START);
        startTimeOutRequestRunnable(Gaia.COMMAND_IVOR_ANSWER_START);
    }

    /**
     * <p>This method builds an {@link Gaia#COMMAND_IVOR_ANSWER_END COMMAND_IVOR_ANSWER_END} packet and
     * sends the packet to the device.</p>
     */
    private void sendIvorAnswerEnd() {
        mController.getGaiaLink().sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_IVOR_ANSWER_END);
        startTimeOutRequestRunnable(Gaia.COMMAND_IVOR_ANSWER_END);
    }


    // ====== PRIVATE METHODS =============================================================

    /**
     * <p>This method logs the current manager state and displays the following information:
     * <ul>
     *     <li>a: IVOR state</li>
     * </ul></p>
     * <p>The logged message should look as follows where <code>label</code> is the given label:</p>
     * <blockquote><pre>label
     state  = a</pre></blockquote>
     * <p>This method is usually called after an event which changes the states of the displayed information.</p>
     *
     * @param label
     *          The label to identify the logging.
     */
    private void logState(String label) {
        if (DEBUG_LOGS) {
            String message = label
                    + "\n\tstate  = " + AssistantEnums.getIvorStateLabel(mState);
            Log.d(TAG, message);
        }
    }

    /**
     * This method sets the state of this assistant to the given state.
     *
     * @param state The new state for this assistant.
     *
     * @return The previous state.
     */
    @SuppressWarnings("UnusedReturnValue")
    private @AssistantEnums.IvorState int setState(@AssistantEnums.IvorState int state) {
        logState("setState: " + AssistantEnums.getIvorStateLabel(state));
        @AssistantEnums.IvorState int previous = mState;
        mState = state;
        mListener.obtainMessage(AssistantEnums.ProviderMessage.IVOR_MESSAGE,
                AssistantEnums.IvorMessage.STATE, 0, state).sendToTarget();
        return previous;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mController.getBluetoothDevice();
    }

    public void connect(BluetoothDevice device) {
        mController.establishGAIAConnection();
    }

    private class TimeOutRequestRunnable implements Runnable {
        /**
         * <p>The request which is monitored for a time out.</p>
         */
        private final int command;

        TimeOutRequestRunnable(int command) {
            this.command = command;
        }

        @Override
        public void run() {
            synchronized (mTimeOutRequestRunnableMap) {
                Log.d(TAG, "A request is timed out for command: "+command);

                if (!mTimeOutRequestRunnableMap.containsKey(command)) {
                    // time out are only for ACK commands
                    Log.w(TAG, "Unexpected runnable is running for command: "+command);
                    return;
                }

                // runnable was expected to run
                LinkedList<TimeOutRequestRunnable> list = mTimeOutRequestRunnableMap.get(command);
                // remove the runnable from the list
                list.remove(this);
                // if there is no other runnable for that key we removed the entry from the Map
                if (list.isEmpty()) {
                    mTimeOutRequestRunnableMap.remove(command);
                }
            }

            Log.w(TAG, "No ACK packet for command: ");
            hasNotReceivedAcknowledgementPacket(command);
        }
    }

    /**
     * <p>To start a Runnable which will be thrown after the known time out request delay set up with
     * {@link #(int) setRequestTimeOut}. This Runnable deals with GAIA requests that didn't
     * receive any acknowledgement for their sent packet.</p>
     *
     */
    private void startTimeOutRequestRunnable(int command) {
        Log.d(TAG, "Set up TimeOutRequestRunnable for type request: for command "+ (command));

        TimeOutRequestRunnable runnable = new TimeOutRequestRunnable(command);
        int key = command;
        if (mTimeOutRequestRunnableMap.containsKey(key)) {
            mTimeOutRequestRunnableMap.get(key).add(runnable);
        }
        else {
            LinkedList<TimeOutRequestRunnable> list = new LinkedList<>();
            list.add(runnable);
            mTimeOutRequestRunnableMap.put(command, list);
        }
        mHandler.postDelayed(runnable, 30000);
    }



    /**
     * <p>To cancel the TimeOutRequestRunnable if there is one running.</p>
     * <p>This method will check if the given key corresponds to any running Runnable, if it does it will stop the
     * Runnable and then removed it from the Map.</p>
     * <p>The key corresponds to the GAIA command of the request which corresponds to the Runnable.</p>
     * @param key
     *          The key of the TimeOutRequestRunnable in the Map.
     */
    private boolean cancelTimeOutRequestRunnable(int key) {
        synchronized (mTimeOutRequestRunnableMap) {
                Log.d(TAG, "Request to cancel a TimeOutRequestRunnable for command: " +(key));
            }

        if (!mTimeOutRequestRunnableMap.containsKey(key)) {
            // time out request runnable not found
            Log.w(TAG, "No pending TimeOutRequestRunnable matches command: " + (key));
            return false;
        }

        // expected command
        List<TimeOutRequestRunnable> list = mTimeOutRequestRunnableMap.get(key);
        // get the first runnable corresponding to the given key - which should be the oldest one
        TimeOutRequestRunnable runnable = list.remove(0);
        // stop the runnable
        mHandler.removeCallbacks(runnable);
        // if there is no other runnable for that key we removed the entry from the Map
        if (list.isEmpty()) {
            mTimeOutRequestRunnableMap.remove(key);
        }
        return true;

    }

    /**
     * <p>To reset the list of time out request runnable to an empty state.</p>
     */
    private synchronized void resetTimeOutRequestRunnableMap() {
        Log.d(TAG, "Received request to reset the TimeOutRequestRunnable Map");
        for (int i = 0; i< mTimeOutRequestRunnableMap.size(); i++) {
            for (TimeOutRequestRunnable runnable : mTimeOutRequestRunnableMap.valueAt(i)) {
                mHandler.removeCallbacks(runnable);
            }
        }
        mTimeOutRequestRunnableMap.clear();
    }
    private Callback mCallback = new SimpleCallback() {
        @Override
        public void handleNotification(GaiaPacket packet) {
            super.handleNotification(packet);
        }

        @Override
        public void onConnected() {
            setConnectionState(AssistantEnums.ConnectionState.CONNECTED);

        }

        @Override
        public void onDisconnected() {
            setConnectionState(AssistantEnums.ConnectionState.DISCONNECTED);
        }

        @Override
        public void onGetAppVersion(String version) {
            super.onGetAppVersion(version);
        }

        @Override
        public void onGetUUID(String uuid) {
            super.onGetUUID(uuid);
        }

        @Override
        public void onGetSerialNumber(String sn) {
            super.onGetSerialNumber(sn);
        }

        @Override
        public void onError(GaiaError error) {
            super.onError(error);
        }

        @Override
        public void onGetBatteryLevel(int level) {
            super.onGetBatteryLevel(level);
        }

        @Override
        public void onGetRSSILevel(int level) {
            super.onGetRSSILevel(level);
        }

        @Override
        public void onSetVoiceAssistantConfig(boolean config) {
            super.onSetVoiceAssistantConfig(config);
        }

        @Override
        public void onSetSensoryConfig(boolean config) {
            super.onSetSensoryConfig(config);
        }

        @Override
        public void onPacketCommandNotSupport(GaiaPacket packet) {
            super.onPacketCommandNotSupport(packet);
        }

        @Override
        public void handlePacket(GaiaPacket packet) {
            if (packet.isAcknowledgement()) {
                if (!cancelTimeOutRequestRunnable(packet.getCommand())) {
                    Log.w(TAG, "Received unexpected acknowledgement packet for command " +(packet.getCommand()));
                    return;
                }

                // acknowledgement was expected: it is dispatched to the child
                Gaia.Status status = packet.getStatus();
                Log.d(TAG, "Received GAIA ACK packet for command "
                        + (packet.getCommand()) + " with status: " + (status));

                if (status == Gaia.Status.SUCCESS) {
                    receiveSuccessfulAcknowledgement(packet);
                } else {
                    receiveUnsuccessfulAcknowledgement(packet);
                }
            } else {
                manageReceivedPacket(packet);
            }
        }
    };

}

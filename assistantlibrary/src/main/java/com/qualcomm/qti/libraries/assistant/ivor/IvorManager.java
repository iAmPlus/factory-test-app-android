/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.libraries.assistant.ivor;

import android.support.annotation.NonNull;
import android.util.Log;

import com.csr.gaiacontrol.Events;
import com.qualcomm.qti.libraries.assistant.AssistantConsts;
import com.qualcomm.qti.libraries.assistant.AssistantEnums;
import com.qualcomm.qti.libraries.gaia.GAIA;
import com.qualcomm.qti.libraries.gaia.GaiaManager;
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacket;
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBREDR;

import java.util.Arrays;

import static com.qualcomm.qti.libraries.gaia.GAIA.NotificationEvents.USER_ACTION;

/**
 * <p>This class follows the GAIA protocol. It manages all IVOR messages which are sent and received over the protocol
 * for the Voice Assistant feature.</p>
 */
public class IvorManager extends GaiaManager {

    // ====== PRIVATE FIELDS =======================================================================

    /**
     * <p>The tag to display for logs.</p>
     */
    private final String TAG = "IvorManager";
    /**
     * To show the debugging logs of this class.
     */
    private static final boolean DEBUG_LOGS = AssistantConsts.Debug.IVOR_MANAGER;
    /**
     * <p>The IVOR version supported by this manager.</p>
     * <p>This array contains: {major, minor} to complete with the structure of the IVOR command
     * {@link GAIA#COMMAND_IVOR_CHECK_VERSION COMMAND_IVOR_CHECK_VERSION}.</p>
     */
    private static final byte[] VERSION = { 0x01, 0x00 };
    /**
     * <p>The listener which implements the {@link IvorManagerListener IvorManagerListener} interface to communicate
     * with a device which supports the IVOR/GAIA protocol.</p>
     */
    private final IvorManagerListener mListener;
    /**
     * <p>The current IVOR state of this manager.</p>
     */
    private @AssistantEnums.IvorState int mState = AssistantEnums.IvorState.UNAVAILABLE;


    // ====== CONSTRUCTOR =======================================================================

    /**
     * <p>Main constructor of this class to initiate the manager.</p>
     *
     * @param listener
     *          The object which implements the {@link IvorManagerListener}
     */
    public IvorManager(@NonNull IvorManagerListener listener) {
        super(GAIA.Transport.BR_EDR);
        this.mListener = listener;
        showDebugLogs(AssistantConsts.Debug.GAIA_MANAGER);
        logState("new instance");
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

    @Override // extends GaiaManager
    protected void receiveSuccessfulAcknowledgement(GaiaPacket packet) {
        switch (packet.getCommand()) {
            case GAIA.COMMAND_IVOR_VOICE_DATA_REQUEST:
                receiveSuccessfulIvorVoiceDataRequestACK();
                break;

            case GAIA.COMMAND_IVOR_VOICE_END:
                receiveSuccessfulIvorVoiceEndACK();
                break;

            case GAIA.COMMAND_IVOR_ANSWER_START:
                receiveSuccessfulIvorAnswerStartACK();
                break;

            case GAIA.COMMAND_IVOR_ANSWER_END:
                receiveSuccessfulIvorAnswerEndACK();
                break;
        }
    }

    @Override // extends GaiaManager
    protected void receiveUnsuccessfulAcknowledgement(GaiaPacket packet) {
        switch (packet.getCommand()) {
            case GAIA.COMMAND_IVOR_VOICE_DATA_REQUEST:
                if (mState == AssistantEnums.IvorState.VOICE_REQUESTING) {
                    Log.w(TAG, "Cancelling session: unsuccessful acknowledgement for COMMAND_IVOR_VOICE_DATA_REQUEST");
                    cancelSession(AssistantEnums.IvorError.UNEXPECTED_ERROR);
                    mListener.onIvorError(AssistantEnums.IvorError.UNEXPECTED_ERROR);
                }
            case GAIA.COMMAND_IVOR_VOICE_END:
                if (mState == AssistantEnums.IvorState.VOICE_ENDING) {
                    setState(AssistantEnums.IvorState.IDLE);
                }
                break;
        }
    }

    @Override // extends GaiaManager
    protected void hasNotReceivedAcknowledgementPacket(GaiaPacket packet) {
        switch (packet.getCommand()) {
            case GAIA.COMMAND_IVOR_VOICE_DATA_REQUEST:
                if (mState == AssistantEnums.IvorState.VOICE_REQUESTING) {
                    Log.w(TAG, "Cancelling session: no acknowledgement for COMMAND_IVOR_VOICE_DATA_REQUEST");
                    cancelSession(AssistantEnums.IvorError.UNEXPECTED_ERROR);
                    mListener.onIvorError(AssistantEnums.IvorError.UNEXPECTED_ERROR);
                }
                break;
        }
    }

    @Override // extends GaiaManager
    protected boolean manageReceivedPacket(GaiaPacket packet) {
        switch (packet.getCommand()) {
            case GAIA.COMMAND_IVOR_START:
                return receiveIvorStart(packet);

            case GAIA.COMMAND_IVOR_VOICE_DATA:
                return receiveIvorVoiceData(packet);

            case GAIA.COMMAND_IVOR_CHECK_VERSION:
                return receiveIvorCheckVersion(packet);

            case GAIA.COMMAND_IVOR_CANCEL:
                return receiveIvorCancel(packet);

            case GAIA.COMMAND_IVOR_VOICE_END:
                return receiveVoiceEnd(packet);

            case GAIA.COMMAND_EVENT_NOTIFICATION:
                return receiveEventNotification(packet);
        }

        return false;
    }

    private boolean receiveEventNotification(GaiaPacket packet) {
        int event = packet.getEvent();
        Log.d(TAG, "handleNotification: " + Integer.toHexString(packet.getPayload()[2] & 0xff).toUpperCase());
        switch (event) {
            case USER_ACTION:
                if(Integer.toHexString(packet.getPayload()[2] & 0xff).toUpperCase().equals(Events.GAIA_USER2)) {
                    mListener.onUserVoiceEnd();
                    return true;
                }
                break;
            default:
                return false;
        }
        return false;
    }

    @Override // extends GaiaManager
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

    @Override // extends GaiaManager
    protected boolean sendGAIAPacket(byte[] packet) {
        return mListener.sendGAIAPacket(packet);
    }

    @Override // overrides GaiaManager
    public void reset() {
        super.reset();
        logState("reset manager");
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
     * @param packet The {@link GAIA#COMMAND_IVOR_START COMMAND_IVOR_START} packet which had been received.
     *
     * @return True if the packet had been acknowledged by the method.
     */
    private boolean receiveIvorStart(GaiaPacket packet) {
        logState("receive START");

        switch (mState) {
            case AssistantEnums.IvorState.IDLE:
                setState(AssistantEnums.IvorState.SESSION_STARTED);
                createAcknowledgmentRequest(packet, GAIA.Status.SUCCESS, null);
                mListener.startSession();
                return true;

            case AssistantEnums.IvorState.UNAVAILABLE:
                createAcknowledgmentRequest(packet, GAIA.Status.NOT_SUPPORTED, null);
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
                createAcknowledgmentRequest(packet, GAIA.Status.INCORRECT_STATE, null);
                Log.w(TAG, "receiveIvorStart: Incorrect state, state is " + AssistantEnums.getIvorStateLabel(mState));
                return true;
        }

        return false;
    }

    /**
     * <p>This method is called when this manager receives a
     * {@link GAIA#COMMAND_IVOR_VOICE_DATA COMMAND_IVOR_VOICE_DATA} packet.</p>
     * <p>If this manager is expecting some voice data, this method transmits the received payload to its listener by
     * calling {@link IvorManagerListener#onReceiveGAIAPacket(byte[]) onReceiveGAIAPacket()}. This manager expects
     * for some voice data when it is in one of the following states:
     * {@link AssistantEnums.IvorState#VOICE_REQUESTING VOICE_REQUESTING} or
     * {@link AssistantEnums.IvorState#VOICE_REQUESTED VOICE_REQUESTED}.</p>
     * <p>Otherwise it ignores the packet.</p>
     *
     * @param packet The {@link GAIA#COMMAND_IVOR_VOICE_DATA COMMAND_IVOR_VOICE_DATA} packet which has been received.
     *
     * @return True if the packet was acknowledged by the method.
     */
    private boolean receiveIvorVoiceData(GaiaPacket packet) {
        switch (mState) {
            case AssistantEnums.IvorState.VOICE_REQUESTING:
                setState(AssistantEnums.IvorState.VOICE_REQUESTED);
            case AssistantEnums.IvorState.VOICE_REQUESTED:
                // No acknowledgement for voice data during a session
                mListener.receiveVoiceData(packet.getPayload());
                return true;

            case AssistantEnums.IvorState.UNAVAILABLE:
                createAcknowledgmentRequest(packet, GAIA.Status.NOT_SUPPORTED, null);
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
     * {@link GAIA#COMMAND_IVOR_CHECK_VERSION COMMAND_IVOR_CHECK_VERSION} packet.</p>
     * <p>This method checks if the sent major and minor versions correspond to the one supported by this manager
     * and answers accordingly to the device.</p>
     * <p>If the version is not supported, an unsuccessful acknowledgement packet is sent with the status
     * {@link GAIA.Status#INVALID_PARAMETER INVALID_PARAMETER}.</p>
     *
     * @param packet The {@link GAIA#COMMAND_IVOR_CHECK_VERSION COMMAND_IVOR_CHECK_VERSION} packet which had been
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
                mListener.onIvorError(AssistantEnums.IvorError.UNAVAILABLE);
            }
            setState(AssistantEnums.IvorState.UNAVAILABLE);
        }

        // acknowledge the support
        @GAIA.Status int status = supported ? GAIA.Status.SUCCESS : GAIA.Status.INVALID_PARAMETER;
        createAcknowledgmentRequest(packet, status, null);
        return true;
    }

    /**
     * <p>This method is called when this manager receives a
     * {@link GAIA#COMMAND_IVOR_VOICE_END COMMAND_IVOR_VOICE_END} packet.</p>
     * <p>If this manager is expecting some voice data, this method changes its state to
     * {@link AssistantEnums.IvorState#VOICE_ENDED VOICE_ENDED} and informs the listener that the device has ended
     * the voice streaming.</p>
     * <p>Otherwise it ignores the packet.</p>
     *
     * @param packet The {@link GAIA#COMMAND_IVOR_VOICE_END COMMAND_IVOR_VOICE_END} packet which had been
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
                createAcknowledgmentRequest(packet, GAIA.Status.INCORRECT_STATE, null);
                return true;

            case AssistantEnums.IvorState.VOICE_ENDED:
            case AssistantEnums.IvorState.VOICE_ENDING:
            case AssistantEnums.IvorState.VOICE_REQUESTED:
                createAcknowledgmentRequest(packet, GAIA.Status.SUCCESS, null);
                setState(AssistantEnums.IvorState.VOICE_ENDED);
                mListener.onVoiceEnded();
                return true;
        }

        return false;
    }

    /**
     * <p>This method is called when this manager receives a
     * {@link GAIA#COMMAND_IVOR_CANCEL COMMAND_IVOR_CANCEL} packet.</p>
     * <p>If this manager is currently in an assistant session, this method cancels the session and informs the
     * listener about the error through {@link IvorManagerListener#onIvorError(int) onIvorError()}.</p>
     * <p>Otherwise it sends an {@link GAIA.Status#INCORRECT_STATE INCORRECT_STATE}
     * or {@link GAIA.Status#NOT_SUPPORTED NOT_SUPPORTED} packet.</p>
     *
     * @param packet The {@link GAIA#COMMAND_IVOR_CANCEL COMMAND_IVOR_CANCEL} packet which had been
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
                createAcknowledgmentRequest(packet, GAIA.Status.SUCCESS, null);
                Log.w(TAG, "Device sends IVOR_CANCEL, cancelling the session, reason="
                        + AssistantEnums.getIvorErrorLabel(error));
                mListener.onIvorError(error);
                setState(AssistantEnums.IvorState.IDLE);
                return true;

            case AssistantEnums.IvorState.UNAVAILABLE:
                createAcknowledgmentRequest(packet, GAIA.Status.NOT_SUPPORTED, null);
                Log.w(TAG, "receiveIvorCancel(" + AssistantEnums.getIvorErrorLabel(error)
                        + "): Voice assistant is UNAVAILABLE.");
                return true;


            case AssistantEnums.IvorState.CANCELLING:
                // already within the cancel process.
                createAcknowledgmentRequest(packet, GAIA.Status.SUCCESS, null);
                Log.w(TAG, "receiveIvorCancel(" + AssistantEnums.getIvorErrorLabel(error)
                        + "): Voice assistant is already in CANCELLING state.");
                return true;

            case AssistantEnums.IvorState.ANSWER_ENDING:
            case AssistantEnums.IvorState.IDLE:
                // cancel session requested while there is no more an ongoing session.
                createAcknowledgmentRequest(packet, GAIA.Status.INCORRECT_STATE, null);
                Log.w(TAG, "receiveIvorCancel: Incorrect state, state is " + AssistantEnums.getIvorStateLabel(mState));
                return true;
        }

        return false;
    }



    // ====== PRIVATE METHODS - SENDING =============================================================

    /**
     * <p>This method builds an {@link GAIA#COMMAND_IVOR_CANCEL COMMAND_IVOR_CANCEL} packet and sends the packet to
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
        GaiaPacket packet = new GaiaPacketBREDR(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_IVOR_CANCEL, payload);
        createRequest(packet);
    }

    /**
     * <p>This method builds an {@link GAIA#COMMAND_IVOR_VOICE_DATA_REQUEST COMMAND_IVOR_VOICE_DATA_REQUEST} packet and
     * sends the packet to the device.</p>
     */
    private void sendIvorDataRequest() {
        GaiaPacket packet = new GaiaPacketBREDR(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_IVOR_VOICE_DATA_REQUEST);
        createRequest(packet);
    }

    /**
     * <p>This method builds an {@link GAIA#COMMAND_IVOR_VOICE_END COMMAND_IVOR_VOICE_END} packet and
     * sends the packet to the device.</p>
     */
    private void sendIvorVoiceEnd() {
        GaiaPacket packet = new GaiaPacketBREDR(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_IVOR_VOICE_END);
        createRequest(packet);
    }

    /**
     * <p>This method builds an {@link GAIA#COMMAND_IVOR_ANSWER_START COMMAND_IVOR_ANSWER_START} packet and
     * sends the packet to the device.</p>
     */
    private void sendIvorAnswerStart() {
        GaiaPacket packet = new GaiaPacketBREDR(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_IVOR_ANSWER_START);
        createRequest(packet);
    }

    /**
     * <p>This method builds an {@link GAIA#COMMAND_IVOR_ANSWER_END COMMAND_IVOR_ANSWER_END} packet and
     * sends the packet to the device.</p>
     */
    private void sendIvorAnswerEnd() {
        GaiaPacket packet = new GaiaPacketBREDR(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_IVOR_ANSWER_END);
        createRequest(packet);
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
        mListener.onIvorStateUpdated(state);
        return previous;
    }


    // ====== INTERFACES ===========================================================================

    /**
     * <p>This interface allows the IVOR manager to dispatch messages or events to a listener.</p>
     */
    public interface IvorManagerListener {

        /**
         * <p>To send over a communication channel the bytes of a GAIA packet using the GAIA protocol.</p>
         *
         * @param packet
         *         The byte array to send to a device.
         *
         * @return true if the sending could be done.
         */
        boolean sendGAIAPacket(byte[] packet);

        /**
         * <p>To start an assistant session after receiving a request from the device.</p>
         * <p>Once the listener is ready to receive the incoming voice data it must call
         * {@link #startVoiceStreaming() startVoiceStreaming()}.</p>
         */
        void startSession();

        /**
         * <p>Once this manager gets some incoming voice data during an assistant it transmits it to its listener
         * through this method for analysis of the data.</p>
         * <p>When the listener does not want to receive more data it must call
         * {@link #stopVoiceStreaming() stopVoiceStreaming()} to stop the device to send the data.</p>
         *
         * @param data the incoming voice data to be analysed by the voice assistant.
         */
        void receiveVoiceData(byte[] data);

        /**
         * <p>This method is used by this manager when an error occurs or when the device cancels the assistant
         * session.</p>
         *
         * @param code
         *          The reason of the cancel.
         */
        void onIvorError(int code);

        /**
         * <p>This method is used by this IVOR manager when the device stops the streaming of the voice data.</p>
         */
        void onVoiceEnded();

        /**
         * <p>To inform the listener that the IVOR state of this manager has changed.</p>
         *
         * @param state
         *          The new state of this manager.
         */
        void onIvorStateUpdated(@AssistantEnums.IvorState int state);

        void onUserVoiceEnd();
    }
}

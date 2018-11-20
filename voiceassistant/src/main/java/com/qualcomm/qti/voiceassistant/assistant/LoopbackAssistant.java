/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.assistant;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.iamplus.buttonsfactorytest.MusicController;
import com.qualcomm.qti.libraries.assistant.Assistant;
import com.qualcomm.qti.libraries.assistant.AssistantEnums;
import com.qualcomm.qti.voiceassistant.Consts;
import com.qualcomm.qti.voiceassistant.Enums;

import java.util.LinkedList;

/**
 * <p>This class implements a loopback assistant which loops back the voice data it has received from a device.</p>
 * <p>This class demonstrates how to implement the {@link com.qualcomm.qti.libraries.assistant.Assistant Assistant}
 * interface.</p>
 * <p>Prior to play back the data it has received, this assistant stores for 5 seconds the incoming audio and then
 * uses the {@link LoopbackPlayer LoopbackPlayer} to play the captured voice.</p>
 */
public class LoopbackAssistant implements Assistant, LoopbackPlayer.LoopbackPlayerListener {

    /**
     * The tag to display when creating logs.
     */
    private final static String TAG = "LoopbackAssistant";
    /**
     * <p>To show the debugging logs of this class.</p>
     */
    private static final boolean DEBUG_LOGS = Consts.Debug.LOOPBACK_ASSISTANT;
    /**
     * The time out to stop the data streaming.
     */
    private static final int STOP_STREAMING_DELAY_MS = 8000;
    /**
     * The current state of this assistant.
     */
    private @AssistantEnums.AssistantState int mState = AssistantEnums.AssistantState.UNAVAILABLE;
    /**
     * A queue to keep the voice data prior to play it.
     */
    private final LinkedList<byte[]> mDataQueue = new LinkedList<>();
    /**
     * The number of bytes contains in the data queue.
     */
    private int mSize = 0;
    /**
     * The listener used to communicate with the Bluetooth device.
     */
    private final AssistantListener mListener;
    /**
     * Keep a reference of the context for playing the data.
     */
    private final Context mContext;
    /**
     * A handler to process some tasks.
     */
    private final Handler mHandler = new Handler();
    /**
     * <p>To know if the stop streaming runnable had been posted and is currently waiting to be processed.</p>
     */
    private boolean mIsRunnablePosted = false;
    /**
     * The implementation of the Media Player uses for this loopback assistant.
     */
    private final LoopbackPlayer mPlayer;
    /**
     * The runnable uses to stop the voice data streaming.
     */
    private final Runnable mStopStreamingRunnable = new Runnable() {
        @Override
        public void run() {
            logState("stop streaming triggered");
            mIsRunnablePosted = false;
            if (mState == AssistantEnums.AssistantState.STREAMING) {
                mListener.stopStreaming();
                playData();
            }
        }
    };


    // ====== CONSTRUCTORS =================================================================================

    /**
     * <p>To build an ew instance of LoopbackAssistant.</p>
     * <p>This constructor initialises the fields uses within this class the LoopbackPlayer included.</p>
     *
     * @param context
     *          The application context to build a WAVE file.
     * @param listener
     *          The listener to be informed of the LoopbackAssistant actions.
     */
    public LoopbackAssistant(Context context, AssistantListener listener) {
        mContext = context;
        mListener = listener;
        mPlayer = new LoopbackPlayer(this);
        logState("new instance");
    }

    /**
     * <p>Give the type of this assistant as one of
     * {@link com.qualcomm.qti.voiceassistant.Enums.AssistantType AssistantType}.</p>
     *
     * @return the type value.
     */
    @SuppressWarnings("SameReturnValue")
    public @Enums.AssistantType int getType() {
        return Enums.AssistantType.LOOPBACK;
    }


    // ====== INTERFACE ASSISTANT METHODS ==================================================================

    @Override // Assistant
    public void init() {
        // reset everything
        setState(AssistantEnums.AssistantState.INITIALISING);
        reset();
    }

    @Override // Assistant
    public boolean startSession() {
        logState("start session");
        switch (mState) {
            case AssistantEnums.AssistantState.SPEAKING:
            case AssistantEnums.AssistantState.STARTING:
            case AssistantEnums.AssistantState.STREAMING:
            case AssistantEnums.AssistantState.UNAVAILABLE:
            case AssistantEnums.AssistantState.CANCELLING:
            case AssistantEnums.AssistantState.CLOSING:
            case AssistantEnums.AssistantState.ENDING_STREAMING:
            case AssistantEnums.AssistantState.INITIALISING:
            case AssistantEnums.AssistantState.PENDING:
                Log.w(TAG, "Start session: not started, unexpected state, state=" + AssistantEnums.getAssistantStateLabel(mState));
                return false;

            case AssistantEnums.AssistantState.IDLE:
                setState(AssistantEnums.AssistantState.STARTING);
                //postStopRunnable();
                setState(AssistantEnums.AssistantState.STREAMING);
                mListener.startStreaming();
                break;
        }

        return true;
    }

    @Override // Assistant
    public void sendData(byte[] data) {
        switch (mState) {
            case AssistantEnums.AssistantState.STARTING:
            case AssistantEnums.AssistantState.STREAMING:
                synchronized (mDataQueue) {
                    mDataQueue.add(data);
                    mSize += data.length;
                }
                return;

            case AssistantEnums.AssistantState.UNAVAILABLE:
            case AssistantEnums.AssistantState.IDLE:
            case AssistantEnums.AssistantState.SPEAKING:
            case AssistantEnums.AssistantState.CANCELLING:
            case AssistantEnums.AssistantState.CLOSING:
            case AssistantEnums.AssistantState.ENDING_STREAMING:
            case AssistantEnums.AssistantState.INITIALISING:
            case AssistantEnums.AssistantState.PENDING:
            default:
                Log.w(TAG, "Send data failed: unexpected state, state=" + AssistantEnums.getAssistantStateLabel(mState));
//                return; // unnecessary here but kept for readability
        }
    }

    @Override // Assistant
    public void cancelSession() {
        logState("cancel session");
        switch (mState) {
            case AssistantEnums.AssistantState.SPEAKING:
            case AssistantEnums.AssistantState.STREAMING:
            case AssistantEnums.AssistantState.STARTING:
            case AssistantEnums.AssistantState.PENDING:
            case AssistantEnums.AssistantState.ENDING_STREAMING:
                setState(AssistantEnums.AssistantState.CANCELLING);
                stopAudio();
                reset();
                break;

            case AssistantEnums.AssistantState.CANCELLING:
            case AssistantEnums.AssistantState.UNAVAILABLE:
            case AssistantEnums.AssistantState.IDLE:
            case AssistantEnums.AssistantState.CLOSING:
            case AssistantEnums.AssistantState.INITIALISING:
                // nothing happens
                Log.i(TAG, "Cancel session: unexpected state, state=" + AssistantEnums.getAssistantStateLabel(mState));
                break;
        }
    }

    @Override // Assistant
    public void close() {
        logState("close");
        mPlayer.release();

        switch (mState) {
            case AssistantEnums.AssistantState.SPEAKING:
            case AssistantEnums.AssistantState.STARTING:
            case AssistantEnums.AssistantState.STREAMING:
            case AssistantEnums.AssistantState.ENDING_STREAMING:
            case AssistantEnums.AssistantState.PENDING:
                setState(AssistantEnums.AssistantState.CLOSING);
//                stopAudio(); // done by the release
                cancelStopRunnable();
                mDataQueue.clear();
                mSize = 0;
                setState(AssistantEnums.AssistantState.UNAVAILABLE);
                return;

            case AssistantEnums.AssistantState.IDLE:
            case AssistantEnums.AssistantState.CANCELLING:
            case AssistantEnums.AssistantState.INITIALISING:
                setState(AssistantEnums.AssistantState.UNAVAILABLE);
                return;

            case AssistantEnums.AssistantState.UNAVAILABLE: // not reachable
            case AssistantEnums.AssistantState.CLOSING: // not reachable
                Log.i(TAG, "Closing assistant: unexpected state, state="
                        + AssistantEnums.getAssistantStateLabel(mState));
                /*return; // statement is not necessary here */
        }
    }

    @Override // Assistant
    public void endDataStream() {
        logState("end data stream");
        cancelStopRunnable();
        if (mState == AssistantEnums.AssistantState.STREAMING) {
            playData();
        }
    }

    @Override // Assistant
    public @AssistantEnums.AssistantState int getState() {
        return mState;
    }

    @Override
    public void setMusicController(MusicController musicController) {
        mPlayer.setmusicController(musicController);
    }


    @Override// Assistant
    public void forceReset() {
        logState("force reset");
        setState(AssistantEnums.AssistantState.CANCELLING);
        stopAudio();
        reset();
    }


    // ====== INTERFACE AUDIO LISTENER METHODS ==================================================================

    @Override // AudioListener
    public void onFinishedPlaying() {
        logState("on finish playing");
        reset();
        mListener.onFinishPlayingResponse();
    }

    @Override // AudioListener
    public void onError() {
        logState("on playing error");
        reset();
        mListener.onError(Enums.AssistantError.PLAYING_RESPONSE_FAILED,
                AssistantEnums.IvorError.PLAYING_RESPONSE_FAILED);
    }


    // ====== PRIVATE METHODS =================================================================================

    /**
     * This method sets the state of this assistant to the given state.
     *
     * @param state The new state for this assistant.
     *
     * @return The previous state.
     */
    private @AssistantEnums.AssistantState int setState(@AssistantEnums.AssistantState int state) {
        logState("set state: " + AssistantEnums.getAssistantStateLabel(state));
        @AssistantEnums.AssistantState int previous = mState;
        mState = state;
        mListener.onStateUpdated(state, previous);
        return previous;
    }

    /**
     * <p>To play the voice data which is contained in the data queue of this LoopbackAssistant. This method plays
     * the data by using the {@link LoopbackPlayer} and informs the
     * {@link com.qualcomm.qti.libraries.assistant.Assistant.AssistantListener AssistantListener} that the response
     * starts to be played.</p>
     */
    private void playData() {
        logState("play data" + mSize);
        if(mSize > 0) {
            setState(AssistantEnums.AssistantState.SPEAKING);
            mListener.onStartPlayingResponse();
            mPlayer.play(mDataQueue, mSize, mContext);
        }// start media playing
    }

    /**
     * <p>To stop the {@link LoopbackPlayer} to play the audio.</p>
     */
    private void stopAudio() {
        logState("stop the playing");
        mPlayer.stop();
    }

    /**
     * <p>To post the stop streaming runnable within a {@link Handler}. The stop streaming runnable is posted to be
     * triggered in the time defined in {@link #STOP_STREAMING_DELAY_MS}.</p>
     */
    private void postStopRunnable() {
        if (!mIsRunnablePosted) {
            logState("start the stop streaming time out");
            mIsRunnablePosted = true;
            mHandler.postDelayed(mStopStreamingRunnable, STOP_STREAMING_DELAY_MS);
        }
        else {
            Log.w(TAG, "Attempt to post StopRunnable failed: runnable already posted.");
        }
    }

    /**
     * <p>To cancel the stop streaming runnable if it had been posted within an {@link Handler}.</p>
     */
    private void cancelStopRunnable() {
        if (mIsRunnablePosted) {
            logState("cancel the stop streaming time out");
            mIsRunnablePosted = false;
            mHandler.removeCallbacks(mStopStreamingRunnable);
        }
    }

    /**
     * <p>This method sets this assistant to {@link AssistantEnums.AssistantState#IDLE IDLE} state, resets its data
     * queue, cancels any runnable which had been posted and stops the {@link LoopbackPlayer}.</p>
     */
    private void reset() {
        logState("reset");
        @AssistantEnums.AssistantState int previous = setState(AssistantEnums.AssistantState.IDLE);
        mDataQueue.clear();
        mSize = 0;
        cancelStopRunnable();
        if (previous == AssistantEnums.AssistantState.SPEAKING) {
            mPlayer.stop();
        }
    }

    /**
     * <p>This method logs the current manager state and displays the following information:
     * <ul>
     *     <li>a: Assistant state,</li>
     *     <li>b: if the runnable to stop is streaming is pending.</li>
     *     <li>c: How many bytes had been received from the device</li>
     *     <li>d: How many data chunks waiting to be processed</li>
     * </ul></p>
     * <p>The logged message should look as follows where <code>label</code> is the given label:</p>
     * <blockquote><pre>label
     state  = a 					stop streaming pending = b
     size in bytes = c 				size in chunks = d</pre></blockquote>
     * <p>This method is usually called after an event which changes the states of the displayed information.</p>
     *
     * @param label
     *          The label to identify the logging.
     */
    private void logState(String label) {
        if (DEBUG_LOGS) {
            String message = label
                    + "\n\tstate  = " + AssistantEnums.getAssistantStateLabel(mState)
                    + " \t\t\t\t\tstop streaming pending = " + mIsRunnablePosted
                    + "\n\tsize in bytes = " + mSize + " \t\tsize in chunks = " + mDataQueue.size();
            Log.d(TAG, message);
        }
    }

}

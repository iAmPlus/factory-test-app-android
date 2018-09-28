/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.assistant;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * <p>This class manages a {@link MediaPlayer MediaPlayer} to play the captured voice of the {@link LoopbackAssistant}
 * .</p>
 * <p>When an instance of this class is not in use anymore, prior to release it, call {@link #release()} in order to
 * release all attached resources.</p>
 */
/*package*/ class LoopbackPlayer {

    // ====== PRIVATE FIELDS =================================================================================
    /**
     * <p>The tag to display for logs.</p>
     */
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final String TAG = "LoopbackPlayer";
    /**
     * The MediaPLayer uses to play some audio.
     */
    private final MediaPlayer mPlayer;
    /**
     * The WAVE file to play.
     */
    private File mFile;
    /**
     * The listener to be informed of the MediaPlayer events.
     */
    private final LoopbackPlayerListener mListener;
    /**
     * A handler to process some tasks.
     */
    private final Handler mHandler = new Handler();

    /**
     * <p>The listener to be informed when the {@link MediaPlayer} is prepared after calling
     * {@link MediaPlayer#prepareAsync()}.</p>
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            mPlayer.start();
        }
    };

    /**
     * <p>The listener to be informed when an error occurs with the {@link MediaPlayer}.</p>
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            mListener.onError();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPlayer.reset();
                }
            });
            return false;
        }
    };

    /**
     * <p>The listener to be informed when the {@link MediaPlayer} has finished to play the given file.</p>
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mPlayer.stop();
            if (mFile != null) {
                //noinspection ResultOfMethodCallIgnored
                mFile.delete();
                mFile = null;
            }
            mListener.onFinishedPlaying();
            mPlayer.reset();
        }
    };


    // ====== CONSTRUCTOR =================================================================================

    /**
     * <p>Constructor of this class.</p>
     * <p>This constructor sets up the parameters of the MediaPlayer as follows:
     * <ul>
     *     <li>Content type: {@link AudioAttributes#CONTENT_TYPE_SPEECH}</li>
     *     <li>Legacy stream type: {@link AudioManager#STREAM_MUSIC}</li>
     *     <li>Usage: {@link AudioAttributes#USAGE_MEDIA}</li>
     * </ul></p>
     */
    LoopbackPlayer(LoopbackPlayerListener listener) {
        mPlayer = new MediaPlayer();
        // create media player
        mPlayer.setOnPreparedListener(mPreparedListener);
        mPlayer.setOnErrorListener(mErrorListener);
        mPlayer.setOnCompletionListener(mCompletionListener);
        AudioAttributes.Builder attributes = new AudioAttributes.Builder();
        attributes.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH);
        attributes.setLegacyStreamType(AudioManager.STREAM_MUSIC);
        attributes.setUsage(AudioAttributes.USAGE_MEDIA);
        mPlayer.setAudioAttributes(attributes.build());
        this.mListener = listener;
    }


    // ====== PROTECTED METHODS =================================================================================

    /**
     * <p>To release the resources used within this class.</p>
     */
    /*package*/ void release() {
        mPlayer.release();
        if (mFile != null) {
            //noinspection ResultOfMethodCallIgnored
            mFile.delete();
        }
    }

    /**
     * <p>To stop the played audio.</p>
     */
    /*package*/ void stop() {
        if (mPlayer.isPlaying()) {
            mPlayer.stop();
        }
    }

    /**
     * <p>To play the audio data given as a parameter.</p>
     * <p>This method builds a WAVE file from the given audio data. This means that the audio data must be bytes of
     * raw PCM 16bits.</p>
     *
     * @param data
     *          The audio data to play.
     * @param size
     *          The number of bytes contained in the data parameter.
     * @param context
     *          The context of the application in order to create the WAVE file.
     */
    /*package*/ void play(List<byte[]> data, int size, Context context) {
        // using the media player
        try {
            mFile = createWaveFile(data, size, context);
            mPlayer.reset();
            FileInputStream fis = new FileInputStream(mFile);
            mPlayer.setDataSource(fis.getFD());
            mPlayer.prepareAsync(); // to not block the main thread
        }
        catch (IOException e) {
            e.printStackTrace();
            mListener.onError();
        }
    }


    // ====== PRIVATE METHODS =================================================================================

    /**
     * <p>This method create the WAVE file by creating a temporary file in the cache directory of the application
     * and to build a WAVE file within this temporary file.</p>
     *
     * @param data
     *          The raw PCM to copy into the WAVE file.
     * @param size
     *          The number of bytes contained in the data parameter.
     * @param context
     *          The context of the application which is used to manage the files.
     *
     * @return The WAVE file which had been created.
     *
     * @throws IOException can occur when reading and writing into the file.
     */
    private File createWaveFile(List<byte[]> data, int size, Context context) throws IOException {
        // create a temporary file
        File file = File.createTempFile("audio", "wav", context.getCacheDir());
        WaveFormat.buildWaveFile(data, size, file);
        return file;
    }


    // ====== INNER INTERFACE =================================================================================

    /**
     * <p>Interface to get the process events while a file is played.</p>
     */
    /*package*/ interface LoopbackPlayerListener {

        /**
         * <p>This method is called when the {@link LoopbackPlayer} has reached the end of the data it was playing.</p>
         */
        void onFinishedPlaying();

        /**
         * <p>Called when an error occurs within the {@link LoopbackPlayer}.</p>
         */
        void onError();
    }
}

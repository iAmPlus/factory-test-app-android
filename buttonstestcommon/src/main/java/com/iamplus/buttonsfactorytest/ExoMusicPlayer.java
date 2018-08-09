package com.iamplus.buttonsfactorytest;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ExoMusicPlayer implements MusicController, Player.EventListener,
        ExtractorMediaSource.EventListener {
    private static final String TAG = "ExoMusicPlayer";
    private final Context mContext;

    private SimpleExoPlayer mPlayer;
    private List<TrackInfo> mPlayList = new ArrayList<>();
    private List<MusicListener> mListeners = new ArrayList<>();
    private boolean mAutoPause = false;
    private boolean mStopped = true;
    private Handler mHandler = new Handler();
    private DefaultExtractorsFactory defaultExtractorsFactory = new DefaultExtractorsFactory();
    private AudioManager mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    Log.d(TAG, "onAudioFocusChange: ");
                    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // We have gained focus:
                        mPlayer.setVolume(1.0f);

                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        Log.d(TAG, "onAudioFocusChange:123 ");
                        // We have lost focus. If we can duck (low playback volume), we can keep playing.
                        // Otherwise, we need to pause the playback.
                        mPlayer.setVolume(0.3f);

                        // If we are playing, we need to reset media player by calling configMediaPlayerState
                        // with mAudioFocus properly set.
                    }
                }
            };

    public ExoMusicPlayer(Context context) {
        mContext = context;
        mPlayer = ExoPlayerFactory.newSimpleInstance(mContext, new DefaultTrackSelector());
        mPlayer.addListener(this);
        mPlayer.setVolume(DEFAULT_UNIT_VOLUME);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void addListener(MusicListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void removeListener(MusicListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void playTracks(List<TrackInfo> urls) {
        log("play tracks");
        if (urls == null || urls.isEmpty()) {
            log("nothing to play");
            notifyError();
            return;
        }
        mPlayList.clear();
        mPlayList.addAll(urls);
        mStopped = false;
        createMediaSources();
    }

    @Override
    public TrackInfo getPlayingTrack() {
        if (mStopped) {
            log("nothing is playing.");
            return null;
        }
        return mPlayList.get(mPlayer.getCurrentWindowIndex());
    }

    @Override
    public void play() {
        log("play");
        if (!canPlay()) {
            log("nothing to play");
            notifyError();
            return;
        }
        mPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        log("pause");
        if (mStopped) {
            log("nothing is playing.");
            return;
        }
        mPlayer.setPlayWhenReady(false);
        resetAutoPause();
    }

    @Override
    public void resume() {
        log("resume");
        if (mStopped) {
            log("nothing is playing.");
            return;
        }
        mPlayer.setPlayWhenReady(true);
        resetAutoPause();
    }

    @Override
    public void stop() {
        log("stop");
        if (mStopped) {
            log("already stopped.");
            return;
        }
        mStopped = true;
        mPlayer.stop();
        resetAutoPause();
    }

    @Override
    public void next() {
        log("play next track.");
        mAutoPause = true;
        Timeline currentTimeline = mPlayer.getCurrentTimeline();
        if (currentTimeline.isEmpty()) {
            return;
        }
        int currentWindowIndex = mPlayer.getCurrentWindowIndex();
        if (currentWindowIndex < currentTimeline.getWindowCount() - 1) {
            mPlayer.seekTo(currentWindowIndex + 1, C.TIME_UNSET);
        } else {
            mPlayer.seekTo(0, C.TIME_UNSET);
        }
    }

    @Override
    public void previous() {
        log("play previous track.");
        mAutoPause = true;
        Timeline currentTimeline = mPlayer.getCurrentTimeline();
        if (currentTimeline.isEmpty()) {
            return;
        }
        int currentWindowIndex = mPlayer.getCurrentWindowIndex();
        if (currentWindowIndex > 0) {
            mPlayer.seekTo(currentWindowIndex - 1, C.TIME_UNSET);
        } else {
            mPlayer.seekTo(currentTimeline.getWindowCount() - 1, C.TIME_UNSET);
        }
    }

    @Override
    public void autoPause() {
        log("auto pause");
        if (mStopped) {
            log("nothing is playing.");
            return;
        }
        if (!mPlayer.getPlayWhenReady()) {
            log("Already paused.");
            return;
        }
        mPlayer.setPlayWhenReady(false);
        mAutoPause = true;
    }

    @Override
    public void autoResume() {
        log("auto resume.");
        if (mStopped) {
            log("nothing is playing.");
            return;
        }
        if (!mAutoPause) {
            log("auto pause not set. ignoring.");
            return;
        }
        mPlayer.setPlayWhenReady(true);
        resetAutoPause();
    }

    @Override
    public void paused() {
        log("notify paused.");
        for (MusicListener listener : mListeners) {
            listener.onPaused();
        }
    }

    @Override
    public float getVolume() {
        return mPlayer.getVolume();
    }

    @Override
    public void setVolume(float volume) {
        log(String.format(Locale.US, "setVolume::newVolume:  %.02f", volume));
        if (volume > 1.0) {
            log("Volume greater than 1");
            return;
        } else if (volume < 0.1) {
            log("Volume too low " + volume);
        }
        mPlayer.setVolume(volume);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        log("notify onTracksChanged.");
        for (MusicListener listener : mListeners) {
            listener.onTrackChanged();
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        log(playWhenReady + " onPlayerStateChanged::" + playbackState);
        if (playbackState == Player.STATE_READY) {
            notifyPlayerStarted(playWhenReady);
            mAudioManager.requestAudioFocus(mFocusChangeListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        } else if (playbackState == Player.STATE_ENDED) {
            mAudioManager.abandonAudioFocus(mFocusChangeListener);
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        notifyError();
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    @Override
    public void close() {
        log("close media player");
        mStopped = true;
        if (mPlayer != null) {
            mPlayer.release();
        }
    }

    @Override
    public SimpleExoPlayer getMusicPlayer() {
        return mPlayer;
    }

    private boolean canPlay() {
        Timeline currentTimeline = mPlayer.getCurrentTimeline();
        return !currentTimeline.isEmpty();

    }

    private void createMediaSources() {
        log("createMediaSources");
        MediaSource[] mediaSources = new MediaSource[mPlayList.size()];
        for (int i = 0; i < mPlayList.size(); i++) {
            final Uri uri = Uri.parse(mPlayList.get(i).url);
            mediaSources[i] = new ExtractorMediaSource(uri,
                    new DataSource.Factory() {
                        @Override
                        public DataSource createDataSource() {
                            RawResourceDataSource dataSource = new RawResourceDataSource(mContext);
                            try {
                                dataSource.open(new DataSpec(uri));
                            } catch (RawResourceDataSource.RawResourceDataSourceException e) {
                                Log.e(TAG, "createDataSource: ", e);
                            }
                            return dataSource;
                        }
                    },
                    defaultExtractorsFactory,
                    mHandler, this);
        }
        MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
                : new ConcatenatingMediaSource(mediaSources);
        mPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        mPlayer.prepare(mediaSource, true, true);
        mPlayer.setPlayWhenReady(false);
        mAutoPause = true;
    }

    private void notifyPlayerStarted(boolean playWhenReady) {
        log("notify play started.");
        for (MusicListener listener : mListeners) {
            if (playWhenReady) {
                listener.onPlaybackStarted();
            } else {
                listener.onPaused();
            }
        }
    }

    private void notifyError() {
        log("notify error");
        for (MusicListener listener : mListeners) {
            listener.onError();
        }
    }

    private void resetAutoPause() {
        mAutoPause = false;
    }

    protected void log(String s) {
        Log.d(TAG, s);
    }

    private void log_exception(String s, Exception e) {
        Log.e(TAG, s, e);
    }

    @Override
    public void onLoadError(IOException error) {
        log_exception("Exception while loading", error);
    }
}

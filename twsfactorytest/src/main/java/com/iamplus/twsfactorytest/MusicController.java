package com.iamplus.twsfactorytest;

import com.google.android.exoplayer2.SimpleExoPlayer;

import java.util.List;

public interface MusicController {

    int DEFAULT_VOLUME = 60;
    float DEFAULT_UNIT_VOLUME = (float) Math.log10(DEFAULT_VOLUME / 10);
    int DELTA_VOLUME = 20;
    int MAX_VOLUME = 100;
    int MIN_VOLUME = 11;
    float MIN_UNIT_VOLUME = (float) Math.log10(MIN_VOLUME / 10);
    float MAX_UNIT_VOLUME = 1.0f;

    void addListener(MusicListener listener);

    void removeListener(MusicListener listener);

    void playTracks(List<TrackInfo> urls);

    TrackInfo getPlayingTrack();

    void play();

    void pause();

    void resume();

    void stop();

    void next();

    void previous();

    void paused();

    // For auto pause music during speaking of hot word.
    void autoPause();

    void autoResume(); // Resume only if auto paused.

    float getVolume();

    void setVolume(float volume);

    void close();

    SimpleExoPlayer getMusicPlayer();

    interface MusicListener {

        void onPlaybackStarted();

        void onPaused();

        void onStop();

        void onTrackChanged();

        void onError();
    }

    class TrackInfo {

        public String title;
        public String id;
        public String url;
    }
}


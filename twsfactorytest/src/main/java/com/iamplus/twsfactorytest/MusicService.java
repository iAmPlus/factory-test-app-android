package com.iamplus.twsfactorytest;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;

public class MusicService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private MusicController mMusicPlayer;
    private MediaSessionCompat mMediaSession;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public MusicController getMusicController() {
        return mMusicPlayer;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMusicPlayer = new ExoMusicPlayer(this);

        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mMediaSession = new MediaSessionCompat(getApplicationContext(), "IamMusicService", mediaButtonReceiver, null);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setClass(getApplicationContext(), MediaButtonReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
            mMediaSession.setMediaButtonReceiver(pendingIntent);
        }

        //NOTE:: This code should be valid only until exoplayer is used as a music player.
        if (mMusicPlayer.getMusicPlayer() != null && mMusicPlayer.getMusicPlayer() instanceof SimpleExoPlayer) {
            log("onCreate: ");
            MediaSessionConnector mediaSessionConnector = new MediaSessionConnector(mMediaSession);
            mediaSessionConnector.setPlayer(mMusicPlayer.getMusicPlayer(),
                    null, null);
            mediaSessionConnector.setQueueNavigator(new MediaSessionConnector.QueueNavigator() {
                @Override
                public long getSupportedQueueNavigatorActions(@Nullable Player player) {
                    return MediaSessionConnector.QueueNavigator.ACTIONS;
                }

                @Override
                public void onTimelineChanged(Player player) {

                }

                @Override
                public void onCurrentWindowIndexChanged(Player player) {

                }

                @Override
                public long getActiveQueueItemId(@Nullable Player player) {
                    return 0;
                }

                @Override
                public void onSkipToPrevious(Player player) {
                    log("onSkipToPrevious");
                    mMusicPlayer.previous();
                }

                @Override
                public void onSkipToQueueItem(Player player, long id) {

                }

                @Override
                public void onSkipToNext(Player player) {
                    log("onSkipToNext");
                    mMusicPlayer.next();
                }

                @Override
                public String[] getCommands() {
                    return new String[0];
                }

                @Override
                public void onCommand(Player player, String command, Bundle extras, ResultReceiver cb) {
                    log("onCommand::" + command);
                }
            });
        }

        mMediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMusicPlayer.close();
    }

    void log(String text) {
        Log.v("MusicService", text);
    }

    public class LocalBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
}

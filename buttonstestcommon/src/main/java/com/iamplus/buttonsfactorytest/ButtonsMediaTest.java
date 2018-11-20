package com.iamplus.buttonsfactorytest;

/**
 * Created by abhay on 06-02-2018.
 */

import android.app.Fragment;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import android.preference.TwoStatePreference;
import android.support.annotation.Nullable;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.widget.ToggleButton;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;


public class ButtonsMediaTest extends Fragment implements View.OnClickListener, MusicController.MusicListener {

    private ImageButton mButtonPlayPause;
    private TextView mSongDisplayView;
    private String TAG = "ButtonsMediaTest";
    private boolean mMediaTestResult;
    private MusicController mMusicController;
    private SimpleExoPlayer mExoPlayer;
    private Switch mToggleButton;

    private final static int RES_IDS[] = { R.raw.buddy, R.raw.tenderness, R.raw.energy};

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_mediaplayer_test, container, false);

        mButtonPlayPause = view.findViewById(R.id.playpause);
        mSongDisplayView = view.findViewById(R.id.songname);
        mButtonPlayPause.setOnClickListener(this);

        mToggleButton = view.findViewById(R.id.toggleButton);
        mToggleButton.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mMediaTestResult = b;
            }
        });
        return view;
    }

    @Override
    public void onClick(View view) {
        if(mButtonPlayPause.getId() == view.getId() && mMusicController != null) {
            if(mButtonPlayPause.getTag().equals(getString(R.string.play))) {
                mMusicController.play();
            } else {
                mButtonPlayPause.setBackground(getResources().getDrawable(R.mipmap.play));
                mButtonPlayPause.setTag(getString(R.string.play));
                mMusicController.pause();
            }
        }
    }

    public boolean getMediaTestResult() {
        return mMediaTestResult;
    }

    public void setMusicController(MusicController musicController) {
        mMusicController = musicController;
        mMusicController.addListener(this);
        mExoPlayer = mMusicController.getMusicPlayer();
        mMusicController.playTracks(Utils.getTracks());
    }

    @Override
    public void onPlaybackStarted() {
        Log.d(TAG, "onPlaybackStarted: ");
        upDateSongInfo(mExoPlayer.getCurrentWindowIndex());
    }

    @Override
    public void onPaused() {
        mButtonPlayPause.setBackground(getResources().getDrawable(R.mipmap.play));
        mButtonPlayPause.setTag(getString(R.string.play));
        Log.d(TAG, "onPaused: ");


    }

    @Override
    public void onTrackChanged() {
        Log.d(TAG, "onTrackChanged: "+mExoPlayer.getCurrentWindowIndex());
        upDateSongInfo(mExoPlayer.getCurrentWindowIndex());

    }

    @Override
    public void onError() {
        Log.d(TAG, "onError: ");
    }

    private void upDateSongInfo(int currentWindowIndex) {
        mButtonPlayPause.setBackground(getResources().getDrawable(R.mipmap.pause));
        mButtonPlayPause.setTag(getString(R.string.pause));
        mSongDisplayView.setText(getResources().getResourceEntryName(RES_IDS[currentWindowIndex]) + ".mp3");
    }

    private String makeUrl(int index) {
        return RawResourceDataSource.buildRawResourceUri(RES_IDS[index]).toString();
    }

    public void resetToggle() {
        mToggleButton.setChecked(false);
    }

}

package com.iamplus.twsfactorytest;

/**
 * Created by abhay on 06-02-2018.
 */

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;

import java.util.ArrayList;
import java.util.List;


public class ButtonsMediaTest extends Fragment implements View.OnClickListener, MusicController.MusicListener {

    private final static int RES_IDS[] = {R.raw.buddy, R.raw.tenderness, R.raw.energy};
    private ImageButton mButtonPlayPause;
    private TextView mSongDisplayView;
    private String TAG = "ButtonsMediaTest";
    private boolean mMediaTestResult;
    private MusicController mMusicController;
    private SimpleExoPlayer mExoPlayer;
    private Switch mToggleButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_mediaplayer_test, container, false);

        mButtonPlayPause = view.findViewById(R.id.playpause);
        mSongDisplayView = view.findViewById(R.id.songname);
        mButtonPlayPause.setOnClickListener(this);

        mToggleButton = (Switch) view.findViewById(R.id.toggleButton);
        mToggleButton.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mMediaTestResult = b;
            }
        });
        //prepareExoPlayerFromRawResourceUri(RawResourceDataSource.buildRawResourceUri(R.raw.buddy));;
        return view;
    }

    @Override
    public void onClick(View view) {
        if (mButtonPlayPause.getId() == view.getId() && mMusicController != null) {
            if (mButtonPlayPause.getTag().equals(getString(R.string.play))) {
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

    public void resetToggle() {
        mToggleButton.setChecked(false);
    }

    public void setMusicController(MusicController musicController) {
        mMusicController = musicController;
        mMusicController.addListener(this);
        mExoPlayer = mMusicController.getMusicPlayer();
        mMusicController.playTracks(getTracks());
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
        Log.d(TAG, "onTrackChanged: " + mExoPlayer.getCurrentWindowIndex());
        upDateSongInfo(mExoPlayer.getCurrentWindowIndex());

    }

    @Override
    public void onError() {
        Log.d(TAG, "onError: ");
    }

    private List<MusicController.TrackInfo> getTracks() {
        List<MusicController.TrackInfo> tracks = new ArrayList<>();
        for (int count = 0; count < RES_IDS.length; count++) {
            MusicController.TrackInfo track = new MusicController.TrackInfo();
            tracks.add(getTrack(count, track));
        }
        return tracks;
    }

    private MusicController.TrackInfo getTrack(int position, MusicController.TrackInfo track) {
        track.id = String.valueOf(position);
        track.title = getResources().getResourceEntryName(RES_IDS[position]);
        track.url = makeUrl(position);
        return track;
    }

    private void upDateSongInfo(int currentWindowIndex) {
        mButtonPlayPause.setBackground(getResources().getDrawable(R.mipmap.pause));
        mButtonPlayPause.setTag(getString(R.string.pause));
        mSongDisplayView.setText(getResources().getResourceEntryName(RES_IDS[currentWindowIndex]) + ".mp3");
    }

    private String makeUrl(int index) {
        return RawResourceDataSource.buildRawResourceUri(RES_IDS[index]).toString();
    }

}

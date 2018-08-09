package com.iamplus.buttonsfactorytest;

/**
 * Created by abhay on 22-01-2018.
 */

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;

public class ButtonsMicrophoneTest extends Fragment {

    public static final int RequestPermissionCode = 1;
    private static final String TAG = "ButtonsMicrophoneTest";
    ImageButton buttonStartStopMic, buttonPlayStopRecording;
    String mFileName = null;
    MediaRecorder mRecorder;
    String RandomAudioFileName = "audiofrombuttons";
    MediaPlayer mediaPlayer;
    private AudioManager mAudioManager;
    private Handler mHandler;
    private Switch mToggleButton;
    private boolean mMicrophoneTestResult;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_microphone_test, container, false);

        buttonStartStopMic = (ImageButton) view.findViewById(R.id.startstop);
        buttonStartStopMic.setBackground(getResources().getDrawable(R.mipmap.mic_off));
        buttonStartStopMic.setTag(getString(R.string.recoord));

        buttonPlayStopRecording = (ImageButton) view.findViewById(R.id.playpause);
        buttonPlayStopRecording.setBackground(getResources().getDrawable(R.mipmap.play));
        buttonPlayStopRecording.setTag(getString(R.string.play));

        mHandler = new Handler();
        mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                RandomAudioFileName + ".3gp";

        buttonStartStopMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonStartStopMic.getTag().equals(getString(R.string.recoord))) {
                    stopMediaPlayer();
                    if (!mAudioManager.isBluetoothScoAvailableOffCall()) {
                        Log.d(TAG, "The system does not support Bluetooth audio");
                        Toast.makeText(getActivity(), "The system does not support Bluetooth audio",
                                Toast.LENGTH_LONG).show();
                    } else {
                        getActivity().registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                                mHandler.removeCallbacksAndMessages(null);
                                Log.d(TAG, "onReceive: EXTRA_SCO_AUDIO_STATE:" + state);
                                if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                                    MediaRecorderReady();
                                    try {
                                        mRecorder.prepare();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    //mAudioManager.setBluetoothScoOn(true);
                                    mRecorder.start();
                                    buttonPlayStopRecording.setVisibility(View.GONE);
                                    buttonStartStopMic.setTag(getString(R.string.stop));
                                    buttonStartStopMic.setBackground(getResources().getDrawable(R.mipmap.mic_on));
                                    Toast.makeText(getActivity(), "Recording started",
                                            Toast.LENGTH_LONG).show();
                                    getActivity().unregisterReceiver(this);
                                } else {//Wait for a second and then try to start SCO
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mAudioManager.startBluetoothSco();
                                            Log.d(TAG, "onClick: thread startBluetoothSco");
                                        }
                                    }, 500);
                                }
                            }
                        }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
                        Log.d(TAG, "onClick: startBluetoothSco");
                        mAudioManager.startBluetoothSco();
                    }
                } else if (buttonStartStopMic.getTag().equals(getString(R.string.stop))) {
                    mRecorder.stop();
                    buttonPlayStopRecording.setVisibility(View.GONE);

                    getActivity().registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                            Log.d(TAG, "onReceive: SCO_AUDIO_STATE_DISCONNECTED:" + state);
                            mHandler.removeCallbacksAndMessages(null);

                            if (AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state) {
                                buttonPlayStopRecording.setVisibility(View.VISIBLE);
                                buttonStartStopMic.setBackground(getResources().getDrawable(R.mipmap.mic_off));
                                buttonStartStopMic.setTag(getString(R.string.recoord));
                                Toast.makeText(getActivity(), "Recording Completed",
                                        Toast.LENGTH_LONG).show();
                                getActivity().unregisterReceiver(this);
                            } else {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mAudioManager.stopBluetoothSco();
                                        Log.d(TAG, "onClick: thread stopBluetoothSco");
                                    }
                                }, 500);
                            }
                        }
                    }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
                    Log.d(TAG, "onClick: stopBluetoothSco");
                    mAudioManager.stopBluetoothSco();
                }

            }
        });

        buttonPlayStopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) throws IllegalArgumentException,
                    SecurityException, IllegalStateException {
                if (buttonPlayStopRecording.getTag().equals(getString(R.string.play))) {
                    //buttonStartStopMic.setVisibility(View.GONE);

                    mAudioManager.stopBluetoothSco();
                    mediaPlayer = new MediaPlayer();

                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            Log.d(TAG, "onCompletion: ");
                            //buttonStartStopMic.setVisibility(View.VISIBLE);
                            stopMediaPlayer();
                        }
                    });
                    try {
                        mediaPlayer.setDataSource(mFileName);
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        buttonPlayStopRecording.setBackground(getResources().getDrawable(R.mipmap.pause));
                        buttonPlayStopRecording.setTag(getString(R.string.pause));
                        Toast.makeText(getActivity(), "Recording Playing", Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Log.e(TAG, "onClick: ", e);
                    }

                } else if (buttonPlayStopRecording.getTag().equals(getString(R.string.pause))) {
                    //buttonStartStopMic.setVisibility(View.VISIBLE);
                    stopMediaPlayer();
                }
            }
        });

        mToggleButton = view.findViewById(R.id.toggleButton);
        mToggleButton.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                stopMediaPlayer();

                mMicrophoneTestResult = b;
            }
        });

        return view;
    }

    private void stopMediaPlayer() {
        if (buttonPlayStopRecording.getTag().equals(getString(R.string.pause))) {
            buttonPlayStopRecording.setBackground(getResources().getDrawable(R.mipmap.play));
            buttonPlayStopRecording.setTag(getString(R.string.play));
            mediaPlayer.stop();
            mediaPlayer.release();
            MediaRecorderReady();
        }
    }


    public void MediaRecorderReady() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    }

    public boolean getMicrophoneTestResult() {
        return mMicrophoneTestResult;
    }

    public void resetToggle() {
        mToggleButton.setChecked(false);
    }


}

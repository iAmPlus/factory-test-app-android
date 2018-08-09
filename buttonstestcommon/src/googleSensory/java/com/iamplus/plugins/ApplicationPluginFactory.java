package com.iamplus.plugins;

import android.content.Context;

import com.iamplus.common.HotwordDetector;
import com.iamplus.common.PluginFactory;
import com.iamplus.common.VoiceRecognizer;
import com.iamplus.google.GoogleSpeech;
import com.iamplus.sensory.SensorySDKManager;

public class ApplicationPluginFactory extends PluginFactory {

    @Override
    public HotwordDetector createHotwordDetector(Context context,
            HotwordDetector.HotWordDetectorListener listener, int bufferSize) {
        return new SensorySDKManager(context, listener);
    }

    public VoiceRecognizer createVoiceRecognizer(Context context,
            VoiceRecognizer.VoiceRecognizerListener listener) {
        return new GoogleSpeech(context, listener);
    }
}

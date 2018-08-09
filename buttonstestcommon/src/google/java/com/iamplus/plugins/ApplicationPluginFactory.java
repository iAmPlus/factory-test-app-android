package com.iamplus.plugins;

import android.content.Context;

import com.iamplus.common.PluginFactory;
import com.iamplus.common.VoiceRecognizer;
import com.iamplus.google.GoogleSpeech;

public class ApplicationPluginFactory extends PluginFactory {

    public VoiceRecognizer createVoiceRecognizer(Context context,
            VoiceRecognizer.VoiceRecognizerListener listener) {
        return new GoogleSpeech(context, listener);
    }
}

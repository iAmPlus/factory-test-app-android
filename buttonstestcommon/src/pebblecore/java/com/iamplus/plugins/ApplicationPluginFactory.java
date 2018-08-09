package com.iamplus.plugins;

import android.content.Context;

import com.iamplus.common.PluginFactory;
import com.iamplus.common.Vocalizer;
import com.iamplus.common.VoiceRecognizer;
import com.iamplus.google.GoogleSpeech;
import com.iamplus.onlinetts.IamplusOnlinetts;

public class ApplicationPluginFactory extends PluginFactory {

    public VoiceRecognizer createVoiceRecognizer(Context context,
            VoiceRecognizer.VoiceRecognizerListener listener) {
        return new GoogleSpeech(context, listener);
    }

    @Override
    public Vocalizer createVocalizer(Context context) {
        return new IamplusOnlinetts(context);
    }
}

package com.iamplus.plugins;

import android.content.Context;

import com.iamplus.common.PluginFactory;
import com.iamplus.common.Vocalizer;
import com.iamplus.common.VoiceRecognizer;
import com.iamplus.nuance.NuanceOnlineVocalizer;
import com.iamplus.nuance.NuanceVoiceRecognizer;

public class ApplicationPluginFactory extends PluginFactory {

    public VoiceRecognizer createVoiceRecognizer(Context context,
            VoiceRecognizer.VoiceRecognizerListener listener) {
        return new NuanceVoiceRecognizer(context, listener);
    }

    @Override
    public Vocalizer createVocalizer(Context context) {
        return new NuanceOnlineVocalizer(context);
    }
}

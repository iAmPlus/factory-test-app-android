package com.iamplus.earin.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsUtil {

    private  static final String PREFERENCE_FILE = "EarinAppSharedprefs";

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
    }

}
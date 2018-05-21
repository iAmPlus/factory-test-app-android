package com.iamplus.twsfactorytest;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Util {

    private  static final String OMEGA_ONLY_TOUCH_PROFILE = "TWSFactoryTestprefs";
    private  static final String ADVANCE_MOD_TOUCH_PROFILE = "50886,0,198,50688,0,17668,1095,18501,51914,18248,17668,1095,18501,51914,18248,2048,9,8,0,2304,0,10,0,51914,2560,0,10,0,0,2560 ";


    private  static final String PREFERENCE_FILE = "TWSFactoryTestprefs";
    private  static final String MAC_ADDRESSES = "MACAddresses";
    private static final String ADVANCE_MOD = "advancemod";

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
    }

    public static Set<String> getMACAddress(Context context) {
        return getPrefs(context).getStringSet(MAC_ADDRESSES, new HashSet<>());
    }

    public static void setMACAddress(Context context, Set<String> mac) {
        getPrefs(context).edit()
                .putStringSet(MAC_ADDRESSES, mac)
                .apply();
    }


    public static boolean getAdvanceMod(Context context) {
        return getPrefs(context).getBoolean(ADVANCE_MOD, false);
    }

    public static void setAdvanceMod(Context context, boolean mod) {
        getPrefs(context).edit()
                .putBoolean(ADVANCE_MOD, mod)
                .apply();
    }

}
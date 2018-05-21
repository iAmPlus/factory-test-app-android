package com.iamplus.systemupdater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    public final static String TAG = "FOTA BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "System updater : received boot start");
        Intent updateService = new Intent("aneeda.CHECKS_FOR_UPDATES");
        context.sendBroadcast(updateService);
    }
}

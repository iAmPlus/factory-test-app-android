package com.iamplus.systemupdater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ChannelReceiver extends BroadcastReceiver {
    public final static String TAG = "FOTA ChannelReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "received Service Discovery message!");

        Log.i(TAG, "Service = " + intent.getStringExtra("service"));
        Log.i(TAG, "Current Channel = " + intent.getStringExtra("channel"));
        Log.i(TAG, "Current Url = " + intent.getStringExtra("url"));

        UpdateManager updateManager = UpdateManager.getInstance(context);
        updateManager.setChannel(intent.getStringExtra("channel"), intent.getStringExtra("url"));
    }
}

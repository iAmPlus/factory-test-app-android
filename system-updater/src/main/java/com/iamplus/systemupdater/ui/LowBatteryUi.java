package com.iamplus.systemupdater.ui;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iamplus.systemupdater.R;
import com.iamplus.systemupdater.Utils;

public class LowBatteryUi extends BaseUi {

    private TextView mBatteryStatus;
    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            updateBatteryStatus(level);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.low_battery, container, false);

        mBatteryStatus = (TextView) v.findViewById(R.id.battery_message);
        updateBatteryStatus(Utils.getBatteryLevel(getActivity()));
        return v;
    }

    private void updateBatteryStatus(int level) {
        String text = getString(R.string.current_battery_level) + " " + level + "%. ";
        text += getString(R.string.battery_minimum);
        mBatteryStatus.setText(text);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mBatteryReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        getActivity().registerReceiver(mBatteryReceiver, filter);
    }
}
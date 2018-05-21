package com.iamplus.systemupdater.ui;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.iamplus.systemupdater.R;
import com.iamplus.systemupdater.Utils;

import java.text.DateFormat;
import java.util.Date;

public class IdleUi extends BaseUi {

    Button mUpdatesButton;
    TextView mNoNetwork;
    TextView mLastChecked;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.idle, container, false);

        mNoNetwork = (TextView) v.findViewById(R.id.no_network);

        mUpdatesButton = (Button) v.findViewById(R.id.check_for_updates);
        mUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiCallbacks cb = getCallbacks();
                if (cb != null) {
                    cb.onCheckForUpdates();
                }
            }
        });

        mLastChecked = (TextView) v.findViewById(R.id.last_checked);
        showItems();
        return v;
    }

    private void setLastChecked() {
        Date lastCheckedDate = getUpdateManager().getLastUpdateCheckDate();
        String lastDate = "";
        if (lastCheckedDate.equals(new Date(0))) {
            lastDate = getString(R.string.never);
        } else {
            lastDate = DateFormat.getDateTimeInstance().format(lastCheckedDate);
        }
        String text = getString(R.string.last_checked) + " " + lastDate;
        mLastChecked.setText(text);
    }

    private void showItems() {
        if (Utils.isOnline(getActivity())) {
            mUpdatesButton.setEnabled(true);
            mNoNetwork.setVisibility(View.GONE);
        } else {
            mUpdatesButton.setEnabled(false);
            mNoNetwork.setVisibility(View.VISIBLE);
        }
        setLastChecked();
    }

    @Override
    public void onNetworkChange(boolean connected) {
        showItems();
    }
}
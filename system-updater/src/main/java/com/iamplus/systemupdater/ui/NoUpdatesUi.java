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

public class NoUpdatesUi extends BaseUi {

    Button mUpdatesButton;
    TextView mNoNetwork;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.no_updates, container, false);

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

        showItems();
        return v;
    }

    private void showItems() {
        if (Utils.isOnline(getActivity())) {
            mUpdatesButton.setEnabled(true);
            mNoNetwork.setVisibility(View.GONE);
        } else {
            mUpdatesButton.setEnabled(false);
            mNoNetwork.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNetworkChange(boolean connected) {
        showItems();
    }
}
package com.iamplus.systemupdater.ui;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.iamplus.systemupdater.Config;
import com.iamplus.systemupdater.R;
import com.iamplus.systemupdater.UpdateInfo;
import com.iamplus.systemupdater.Utils;

public class DownloadPausedUi extends BaseUi {
    Button mRetryButton;
    TextView waiting;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.download_paused, container, false);

        waiting = (TextView) v.findViewById(R.id.waiting_for_network);
        UpdateInfo ui = getUpdateManager().getUpdateInfo();
        if (ui != null) {
            int id = ui.isWifiOnly ? R.string.waiting_for_wifi : R.string.waiting_for_network;
            waiting.setText(id);
        }

        TextView resume = (TextView) v.findViewById(R.id.auto_resume);
        resume.setText(
                getString(R.string.auto_resume, Config.DOWNLOAD_RETRY_DURATION / Config.MIN));

        mRetryButton = (Button) v.findViewById(R.id.retry);
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiCallbacks cb = getCallbacks();
                if (cb != null) {
                    cb.onRetryDownload();
                }
            }
        });
        showItems();
        return v;
    }

    private void showItems() {
        UpdateInfo ui = getUpdateManager().getUpdateInfo();
        boolean isOnline = false;
        if (ui != null && ui.isWifiOnly) {
            isOnline = Utils.isOnlineWiFi(getActivity());
        } else {
            isOnline = Utils.isOnline(getActivity());
        }

        if (isOnline) {
            mRetryButton.setEnabled(true);
            waiting.setVisibility(View.GONE);
        } else {
            mRetryButton.setEnabled(false);
            waiting.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNetworkChange(boolean connected) {
        showItems();
    }
}

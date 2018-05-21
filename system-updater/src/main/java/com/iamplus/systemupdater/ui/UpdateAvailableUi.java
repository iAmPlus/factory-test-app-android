package com.iamplus.systemupdater.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.iamplus.systemupdater.R;
import com.iamplus.systemupdater.UpdateInfo;
import com.iamplus.systemupdater.Utils;

public class UpdateAvailableUi extends BaseUi {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.update_available, container, false);
        UpdateInfo ui = getUpdateManager().getUpdateInfo();

        if (ui != null) {

            TextView updateAvailable = (TextView) v.findViewById(R.id.update_available);
            if (ui.isCritical) {
                updateAvailable.setText(R.string.critical_update_available);
                updateAvailable.setTextAppearance(getActivity(), R.style.CriticalTextAppearance);
            } else {
                updateAvailable.setText(R.string.update_available);
                updateAvailable.setTextAppearance(getActivity(), R.style.DefaultTextAppearance);
            }

            TextView wifiOnly = (TextView) v.findViewById(R.id.wifi_only);
            if (!ui.isWifiOnly) {
                wifiOnly.setVisibility(View.GONE);
            }

            TextView sizeView = (TextView) v.findViewById(R.id.size);
            sizeView.setText(getString(R.string.size) + " : " + Utils.readableFileSize(ui.size));

            TextView versionView = (TextView) v.findViewById(R.id.version);
            versionView.setText(getString(R.string.version) + " : " + ui.to_version);

            TextView releaseNotes = (TextView) v.findViewById(R.id.release_notes);
            releaseNotes.setText(getString(R.string.release_notes) + " :\n" + ui.releaseNotes);
        }

        Button downloadAndInstallButton = (Button) v.findViewById(R.id.download_and_install_update);
        downloadAndInstallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiCallbacks cb = getCallbacks();
                if (cb != null) {
                    cb.onDownloadAndInstallUpdate();
                }
            }
        });


        v.findViewById(R.id.cancel_download_update).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiCallbacks cb = getCallbacks();
                if (cb != null) {
                    cb.onQuit();
                }
            }
        });

        return v;
    }
}
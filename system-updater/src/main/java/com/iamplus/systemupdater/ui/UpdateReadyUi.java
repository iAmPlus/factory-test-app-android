package com.iamplus.systemupdater.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.iamplus.systemupdater.R;
import com.iamplus.systemupdater.UpdateInfo;

public class UpdateReadyUi extends BaseUi {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.update_ready, container, false);

        UpdateInfo ui = getUpdateManager().getUpdateInfo();

        if (ui != null) {

            TextView updateReady = (TextView) v.findViewById(R.id.update_ready);
            if (ui.isCritical) {
                updateReady.setText(R.string.critical_update_ready);
                updateReady.setTextAppearance(getActivity(), R.style.CriticalTextAppearance);
            } else {
                updateReady.setText(R.string.update_ready);
                updateReady.setTextAppearance(getActivity(), R.style.DefaultTextAppearance);
            }

            TextView updateReadyDetails = (TextView) v.findViewById(R.id.update_ready_details);
//            updateReadyDetails.setText(R.string.update_ready_details);

            TextView versionView = (TextView) v.findViewById(R.id.version);
            versionView.setText(getString(R.string.version) + " : " + ui.to_version);

            TextView releaseNotes = (TextView) v.findViewById(R.id.release_notes);
            releaseNotes.setText(getString(R.string.release_notes) + " :\n" + ui.releaseNotes);
        }

        Button installButton = (Button) v.findViewById(R.id.install_update);
        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiCallbacks cb = getCallbacks();
                if (cb != null) {
                    log("Intalling update manually");
                    cb.onInstallUpdate();
                }
            }
        });

        Button postponeButton = (Button) v.findViewById(R.id.postpone_update);
        if (ui != null && ui.isCritical) {
            log("Critical update: Auto install in 1 min");
            postponeButton.setVisibility(View.GONE);
        } else {
            postponeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UiCallbacks cb = getCallbacks();
                    if (cb != null) {
                        cb.onPostponeUpdate();
                    }
                }
            });
        }

        v.findViewById(R.id.cancel_update).setOnClickListener(new View.OnClickListener() {
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
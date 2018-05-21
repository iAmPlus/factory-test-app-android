package com.iamplus.systemupdater.ui;

import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.iamplus.systemupdater.Config;
import com.iamplus.systemupdater.R;
import com.iamplus.systemupdater.UpdateInfo;

public class UpdateDoneUi extends BaseUi {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.update_done, container, false);

        UpdateInfo ui = getUpdateManager().getUpdateInfo();

        if (ui != null) {
            TextView updateDone = (TextView) v.findViewById(R.id.update_done);
            if (ui.isCritical) {
                updateDone.setText(R.string.critical_update_done);
                updateDone.setTextAppearance(getActivity(), R.style.CriticalTextAppearance);
            } else {
                updateDone.setText(R.string.update_done);
                updateDone.setTextAppearance(getActivity(), R.style.DefaultTextAppearance);
            }

            TextView versionView = (TextView) v.findViewById(R.id.version);
            versionView.setText(getString(R.string.version) + " : " + ui.to_version);
        }

        Button okButton = (Button) v.findViewById(R.id.ok);
        okButton.setOnClickListener(new View.OnClickListener() {
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

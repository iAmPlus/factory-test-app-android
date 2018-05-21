package com.iamplus.systemupdater.ui;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.iamplus.systemupdater.R;

public class ErrorUi extends BaseUi {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.error, container, false);

        TextView textView = (TextView) v.findViewById(R.id.error);
        textView.setText(getUpdateManager().getErrorText());

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

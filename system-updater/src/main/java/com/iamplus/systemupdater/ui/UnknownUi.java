package com.iamplus.systemupdater.ui;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.TextView;

import com.iamplus.systemupdater.R;

public class UnknownUi extends BaseUi {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.unknown, container, false);

        TextView textView = (TextView) v.findViewById(R.id.unknown_state);
        textView.setText("Unknown State : " + getState());

        return v;
    }
}
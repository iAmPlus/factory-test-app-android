package com.iamplus.systemupdater.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iamplus.systemupdater.R;

public class WaitingUi extends BaseUi {

    int mId;

    public WaitingUi() {
        mId = R.string.waiting;
    }

    public static WaitingUi newInstance(int id) {
        WaitingUi ui = new WaitingUi();
        Bundle bundle = new Bundle();
        bundle.putInt("message", id);
        ui.setArguments(bundle);
        return ui;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.waiting, container, false);

        TextView tv = (TextView) v.findViewById(R.id.waiting);
        Bundle args = getArguments();
        if (args != null) {
            mId = args.getInt("message");
        }
        tv.setText(mId);

        return v;
    }
}
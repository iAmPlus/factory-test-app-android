package com.iamplus.earin.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iamplus.earin.R;
import com.iamplus.earin.ui.activities.dash.DashM2Activity;

public class PowerOffFragment extends BaseFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_power_off, container, false);

        TextView okTextView = rootView.findViewById(R.id.okTextView);
        TextView cancelTextView = rootView.findViewById(R.id.cancelTextView);

        cancelTextView.setOnClickListener(view -> mActivity.removeLastFragment());
        okTextView.setOnClickListener(view -> ((DashM2Activity) mActivity).powerOffEarbuds());


        return rootView;
    }
}

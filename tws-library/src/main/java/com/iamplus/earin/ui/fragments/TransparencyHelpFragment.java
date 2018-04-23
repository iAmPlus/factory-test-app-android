package com.iamplus.earin.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iamplus.earin.R;

public class TransparencyHelpFragment extends BaseFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_transparency_help, container, false);

        TextView closeTextView = rootView.findViewById(R.id.closeTextView);
        closeTextView.setOnClickListener(view -> mActivity.removeLastFragment());

        return rootView;
    }


}

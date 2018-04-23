package com.iamplus.earin.ui.fragments;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iamplus.earin.R;
import com.iamplus.earin.ui.activities.BaseToolbarActivity;

public class NoConnectionFragment extends BaseFragment {

    private static final String ARG_TOOLBAR_TITLE = "toolbarTitle";
    private static final String ARG_DESCRIPTION = "description";

    public static NoConnectionFragment newInstance(String toolbarTitle, String description) {
        NoConnectionFragment noConnectionFragment = new NoConnectionFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TOOLBAR_TITLE, toolbarTitle);
        args.putString(ARG_DESCRIPTION, description);
        noConnectionFragment.setArguments(args);

        return noConnectionFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_no_connection, container, false);


        initToolbar(rootView, getArguments().getString(ARG_TOOLBAR_TITLE, "").toUpperCase());
        initToolbarLeftButton(ContextCompat.getDrawable(getActivity(), R.drawable.ic_back), view -> ((BaseToolbarActivity) getActivity()).removeLastFragment());

        TextView enableConnectionDescriptionTextView = rootView.findViewById(R.id.enableConnectionDescription);
        enableConnectionDescriptionTextView.setText(getArguments().getString(ARG_DESCRIPTION, ""));

        return rootView;
    }
}

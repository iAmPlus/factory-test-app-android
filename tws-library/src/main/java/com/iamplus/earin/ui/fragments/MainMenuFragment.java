package com.iamplus.earin.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.iamplus.earin.R;
import com.iamplus.earin.ui.activities.BaseToolbarActivity;
import com.iamplus.earin.ui.chat.ChatActivity;
import com.iamplus.earin.ui.fragments.supportpages.SectionsFragment;
import com.iamplus.earin.util.NetworkUtil;
import com.iamplus.earin.util.bluetooth.BleBroadcastUtil;

public class MainMenuFragment extends BaseFragment implements View.OnClickListener {

    private TextView mRenameUnitTextView;
    private TextView mSupportPagesTextView;
    private RelativeLayout mSupportChatItemLayout;
    private TextView mOnlineStoreTextView;
    private TextView mAboutTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_menu, container, false);

        mRenameUnitTextView = rootView.findViewById(R.id.renameUnitTextView);
        mSupportPagesTextView = rootView.findViewById(R.id.supportPagesTextView);
        mSupportChatItemLayout = rootView.findViewById(R.id.supportChatItemLayout);
        mOnlineStoreTextView = rootView.findViewById(R.id.onlineStoreTextView);
        mAboutTextView = rootView.findViewById(R.id.aboutTextView);

        ImageButton clearImageButton = rootView.findViewById(R.id.clearImageButton);
        clearImageButton.setOnClickListener(view -> ((BaseToolbarActivity) getActivity()).removeLastFragment());

        // for disabling activity buttons
        rootView.findViewById(R.id.dialogContainer).setOnClickListener(null);

        mRenameUnitTextView.setOnClickListener(this);
        mSupportPagesTextView.setOnClickListener(this);
        mSupportChatItemLayout.setOnClickListener(this);
        mOnlineStoreTextView.setOnClickListener(this);
        mAboutTextView.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        BleBroadcastUtil bleBroadcastUtil = BleBroadcastUtil.getInstance(getActivity());
        if (bleBroadcastUtil.isConnected() && bleBroadcastUtil.getLastNfmiStatus() != null && bleBroadcastUtil.getLastNfmiStatus()) {
            mRenameUnitTextView.setVisibility(View.VISIBLE);
        } else {
            mRenameUnitTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mRenameUnitTextView)) {
            ((BaseToolbarActivity) getActivity()).openFullscreenFragment(new RenameUnitFragment());
        } else if (view.equals(mSupportPagesTextView)) {
            if (NetworkUtil.isInternetAvailable(getActivity())) {
                ((BaseToolbarActivity) getActivity()).openFullscreenFragment(new SectionsFragment());
            } else {
                ((BaseToolbarActivity) getActivity()).openFullscreenFragment(NoConnectionFragment.newInstance(getString(R.string.support_pages), getString(R.string.no_connection_support)));
            }
        } else if (view.equals(mSupportChatItemLayout)) {
            if (NetworkUtil.isInternetAvailable(getActivity())) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                getActivity().startActivity(intent);
            } else {
                ((BaseToolbarActivity) getActivity()).openFullscreenFragment(NoConnectionFragment.newInstance(getString(R.string.support_chat), getString(R.string.no_connection_chat)));
            }
        } else if (view.equals(mOnlineStoreTextView)) {
            if (NetworkUtil.isInternetAvailable(getActivity())) {
                ((BaseToolbarActivity) getActivity()).openFullscreenFragment(new OnlineStoreFragment());
            } else {
                ((BaseToolbarActivity) getActivity()).openFullscreenFragment(NoConnectionFragment.newInstance(getString(R.string.online_store), getString(R.string.no_connection_store)));
            }
        } else if (view.equals(mAboutTextView)) {
            ((BaseToolbarActivity) getActivity()).openFullscreenFragment(new AboutFragment());
        }
    }
}

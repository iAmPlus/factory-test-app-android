package com.iamplus.earin.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iamplus.earin.BuildConfig;
import com.iamplus.earin.R;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.CapCommunicator;
import com.iamplus.earin.util.FirmwareVersion;
import com.iamplus.earin.util.bluetooth.BleBroadcastUtil;

public class AboutFragment extends BaseFragment {

    private TextView mFirmwareVersionTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        initToolbar(rootView, getString(R.string.about).toUpperCase());
        initToolbarLeftButton(ContextCompat.getDrawable(getActivity(), R.drawable.ic_back),
                view -> mActivity.removeLastFragment());

        TextView appVersionTextView = rootView.findViewById(R.id.appVersionTextView);
        String appVersion = getString(R.string.about_app_version) + " " + BuildConfig.VERSION_NAME;
        appVersionTextView.setText(appVersion);

        initVersionText(rootView);

        TextView betaProgramTextView = rootView.findViewById(R.id.betaProgramTextView);
        betaProgramTextView.setOnClickListener(view -> mActivity.openFullscreenFragment(new BetaProgramFragment()));
        return rootView;
    }

    private void initVersionText(View rootView) {
        mFirmwareVersionTextView = rootView.findViewById(R.id.firmwareVersionTextView);

        if (BleBroadcastUtil.getInstance(getActivity()).isConnected()) {
            Manager manager = Manager.getSharedManager();
            CapCommunicator communicator = manager.getCapCommunicationController().getConnectedCommunicator();
            manager.enqueRequest("getVersionAbout", () -> {
                try {
                    final FirmwareVersion firmwareVersion = FirmwareVersion.fromString(communicator.getVersion());
                    if (getActivity() != null) {
                        new Handler(Looper.getMainLooper()).post(() -> mFirmwareVersionTextView.setText(getString(
                                R.string.about_firmware_version,
                                firmwareVersion.getCsr().toString(),
                                firmwareVersion.getNxp().toString())));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }


}

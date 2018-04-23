package com.iamplus.earin.ui.fragments;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.iamplus.earin.R;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.CapCommunicator;

public class GainFragment extends BaseFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gain, container, false);

        initToolbar(rootView, getString(R.string.gain).toUpperCase());
        initToolbarLeftButton(ContextCompat.getDrawable(getActivity(), R.drawable.ic_back), view -> mActivity.removeLastFragment());

        Manager manager = Manager.getSharedManager();
        CapCommunicator communicator = manager.getCapCommunicationController().getConnectedCommunicator();

        ImageButton minusImageButton = rootView.findViewById(R.id.minusImageButton);
        ImageButton plusImageButton = rootView.findViewById(R.id.plusImageButton);

        minusImageButton.setOnClickListener(view -> manager.enqueRequest("gainMinus", () -> {
            try {
                communicator.getVolumeDecrease();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        plusImageButton.setOnClickListener(view -> manager.enqueRequest("gainPlus", () -> {
            try {
                communicator.getVolumeIncrease();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        ImageButton clearImageButton = rootView.findViewById(R.id.clearImageButton);
        clearImageButton.setOnClickListener(view -> mActivity.removeLastFragment());
        return rootView;
    }
}
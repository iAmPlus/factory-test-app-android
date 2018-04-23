package com.iamplus.earin.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.iamplus.earin.R;
import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.CapCommunicator;
import com.iamplus.earin.util.bluetooth.BleBroadcastUtil;

public class RenameUnitFragment extends BaseFragment {

    private String mCurrentName;
    private EditText mUnitNameEditText;
    private Button mRenameUnitButton;
    private ProgressBar mLoadingProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_rename_unit, container, false);

        initToolbar(rootView, getString(R.string.rename_unit).toUpperCase());
        initToolbarLeftButton(ContextCompat.getDrawable(getActivity(), R.drawable.ic_back), view -> mActivity.removeLastFragment());

        mUnitNameEditText = rootView.findViewById(R.id.unitNameEditText);
        mRenameUnitButton = rootView.findViewById(R.id.renameButton);

        mUnitNameEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                renameUnit();
                return true;
            }
            return false;
        });


        mRenameUnitButton.setOnClickListener(view -> renameUnit());
        mLoadingProgressBar = rootView.findViewById(R.id.loadingProgressBar);
        setUpInitialValues();
        return rootView;
    }

    private void setUpInitialValues() {
//        SerialExecutor.getInstance().execute(() -> {
        Manager manager = Manager.getSharedManager();
        CapCommunicator communicator = manager.getCapCommunicationController().getConnectedCommunicator();
        manager.enqueRequest("getCustomName", () -> {
            try {
                String name = communicator.getCustomName();
                new Handler(Looper.getMainLooper()).post(() -> {
                    mCurrentName = name;
                    mUnitNameEditText.setText(name);
                    mUnitNameEditText.setVisibility(View.VISIBLE);
                    mRenameUnitButton.setVisibility(View.VISIBLE);
                    mLoadingProgressBar.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void renameUnit() {
        BleBroadcastUtil bleBroadcastUtil = BleBroadcastUtil.getInstance(getActivity());
        if (bleBroadcastUtil.isConnected() && bleBroadcastUtil.getLastNfmiStatus() != null && bleBroadcastUtil.getLastNfmiStatus()) {
            Manager manager = Manager.getSharedManager();
            CapCommunicator communicator = manager.getCapCommunicationController().getConnectedCommunicator();
            manager.enqueRequest("setCustomName", () -> {
                try {
                    if (!mUnitNameEditText.getText().toString().isEmpty()) {
                        communicator.setCustomName(mUnitNameEditText.getText().toString());
                        new Handler(Looper.getMainLooper()).post(() -> {
                            mCurrentName = mUnitNameEditText.getText().toString();
                            Toast.makeText(EarinApplication.getContext(), getString(R.string.rename_success), Toast.LENGTH_SHORT).show();
                            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(mUnitNameEditText.getWindowToken(), 0);
                        });
                    }
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        mUnitNameEditText.setText(mCurrentName);
                        Toast.makeText(EarinApplication.getContext(), getString(R.string.rename_failed), Toast.LENGTH_SHORT).show();
                    });
                    e.printStackTrace();
                }

            });
        } else {
            mUnitNameEditText.setText(mCurrentName);
            Toast.makeText(EarinApplication.getContext(), getString(R.string.rename_failed), Toast.LENGTH_SHORT).show();
        }
    }
}

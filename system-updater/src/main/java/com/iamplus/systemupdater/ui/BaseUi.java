package com.iamplus.systemupdater.ui;

import android.os.Bundle;
import android.app.Activity;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.util.Log;

import com.iamplus.systemupdater.UpdateManager;

import com.iamplus.systemupdater.R;

public class BaseUi extends Fragment {
    public static final String TAG = "UpdateActivity.";

    protected UpdateManager mUpdateManager;

    UiCallbacks getCallbacks() {
        Activity activity = getActivity();
        if (activity == null) return null;
        UiCallbacks callbacks = (UiCallbacks) activity;
        if (callbacks == null) {
            log("Activity not attached ");
        }
        return callbacks;
    }

    public UpdateManager getUpdateManager() {
        return mUpdateManager;
    }

    public void setUpdateManager(UpdateManager updateManager) {
        mUpdateManager = updateManager;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mUpdateManager = getCallbacks().getUpdateManager();
    }

    public UpdateManager.State getState() {
        return mUpdateManager.getState();
    }

    public UpdateManager.ErrorCodes getErrorStatus() {
        return mUpdateManager.getErrorStatus();
    }

    public void onNetworkChange(boolean connected) {
    }

    public void log(String msg) {
        Log.d(TAG + getClass().getSimpleName(), msg);
    }
}
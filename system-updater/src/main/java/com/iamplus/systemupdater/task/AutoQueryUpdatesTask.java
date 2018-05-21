package com.iamplus.systemupdater.task;

public class AutoQueryUpdatesTask extends BaseTask {

    public boolean doInBackground() {
        return true;
    }

    @Override
    protected boolean onPostUi(boolean result) {
        log("AutoQueryUpdatesTask " + getState());
        mUpdateManager.checkForOTAUpdates();
        return true;
    }
}
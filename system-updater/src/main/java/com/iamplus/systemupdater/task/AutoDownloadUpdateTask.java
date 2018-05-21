package com.iamplus.systemupdater.task;

import com.iamplus.systemupdater.JobUtils;
import com.iamplus.systemupdater.UpdateManager;

public class AutoDownloadUpdateTask extends BaseTask {

    @Override
    public void onPreExecute() {
        switch (getState()) {
            case UPDATE_AVAILABLE:
                break;
            default:
                log("Job Cancelled..Not in proper state: " + getState());
                cancel(true);
                return;
        }
    }

    public boolean doInBackground() {
        return true;
    }

    @Override
    protected boolean onPostUi(boolean result) {
        log("AutoDownloadUpdateTask " + getState());
        mUpdateManager.downloadUpdate();
        return true;
    }
}
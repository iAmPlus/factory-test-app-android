package com.iamplus.systemupdater.task;

import com.iamplus.systemupdater.JobUtils;
import com.iamplus.systemupdater.UpdateManager;

public class RemindUserJob extends BaseTask {

    public boolean doInBackground() {
        return true;
    }

    @Override
    protected boolean onPostUi(boolean result) {
        log("RemindUserJob " + getState());
        mUpdateManager.remindUser();
        return true;
    }
}
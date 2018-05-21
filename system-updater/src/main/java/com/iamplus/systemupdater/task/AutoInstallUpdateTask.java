package com.iamplus.systemupdater.task;

public class AutoInstallUpdateTask extends BaseTask {

    @Override
    public void onPreExecute() {
        switch (getState()) {
            case UPDATE_READY:
            case INSTALLING_UPDATE:
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
        log("AutoInstallUpdateTask " + getState());
//        mUpdateManager.installUpdate();
        mUpdateManager.sendUpdateReadyNotification();
        return true;
    }
}
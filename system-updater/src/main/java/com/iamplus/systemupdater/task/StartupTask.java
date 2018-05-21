package com.iamplus.systemupdater.task;

import com.iamplus.systemupdater.Config;
import com.iamplus.systemupdater.JobUtils;

public class StartupTask extends BaseTask {

    public boolean doInBackground() {
        return true;
    }

    @Override
    protected boolean onPostUi(boolean result) {
        log("onStartup " + getState());
        log("Current Version " + mUpdateManager.getVersion() + " channel " +
                Config.getChannel(getContext()));
        JobUtils.startRegisterCdnJob(getContext(), null, null);
        mUpdateManager.loadUrls();
        switch (getState()) {
            case IDLE:
                log("Idle");
                mUpdateManager.restoreValidState();
                break;
            case QUERYING_UPDATE:
                log("QUERYING_UPDATE");
                JobUtils.startQueryUpdateJob(getContext(), null);
                break;
            case NO_UPDATES:
                log("NO_UPDATES");
                mUpdateManager.restoreValidState();
                break;
            case UPDATE_AVAILABLE:
                log("UPDATE_AVAILABLE");
                mUpdateManager.onExistingUpdateAvailable();
                break;
            case DOWNLOADING_UPDATE:
            case DOWNLOAD_PAUSED:
                log("DOWNLOADING_UPDATE");
                mUpdateManager.downloadUpdate();
                break;
            case VALIDATE_UDPATE:
                log("VALIDATE_UDPATE");
                JobUtils.startVerifyUpdateJob(getContext());
                break;
            case UPDATE_READY:
                log("UPDATE_READY");
                mUpdateManager.onUpdateReady();
                break;
            case WAITING_LOW_BATTERY:
                log("WAITING_LOW_BATTERY");
                mUpdateManager.installUpdate();
                break;
            case WAITING_TO_INSTALL:
                log("WAITING_TO_INSTALL");
                mUpdateManager.applyUpdate();
                break;
            case INSTALLING_UPDATE:
                log("INSTALLING_UPDATE");
                JobUtils.startPostUpdateJob(getContext());
                break;
            case UPDATE_DONE:
                log("UPDATE_DONE");
                mUpdateManager.restoreValidState();
                break;
            case ERROR:
                log("ERROR");
                mUpdateManager.restoreValidState();
                break;
        }

        return true;
    }
}
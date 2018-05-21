package com.iamplus.systemupdater.ui;

import com.iamplus.systemupdater.UpdateManager;

public interface UiCallbacks {
    UpdateManager getUpdateManager();

    void onCheckForUpdates();

    void onQuit();

    void onDownloadAndInstallUpdate();

    void onInstallUpdate();

    void onPostponeUpdate();

    void onRetryDownload();
}
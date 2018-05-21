package com.iamplus.systemupdater;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private static final int NOTIFY_UI = 200;
    private static final int NOTIFY_DOWNLOAD_PROGRESS = 201;
    private static final int NOTIFY_NETWORK_STATE = 202;
    private static final int NOTIFY_FINISH = 203;
    private static UpdateManager mUpdateManager;
    private Context mContext;
    private State mState = State.IDLE;
    private ErrorCodes mErrorStatus = ErrorCodes.NO_ERROR;
    private IntiatedBy mInitiatedBy = IntiatedBy.NONE;
    private int mPostponeCount = 0;
    private int mDownloadPercentage = -1;
    private int mDownloadSize = 0;
    private boolean mCancellingJobs = false;
    private boolean mRecheckUpdate = false;
    private int mDownloadRetryCount = 0;
    private UpdateInfo mUpdateInfo;
    private UpdateLog mUpdateLog;
    private String mRegistrationId;
    private Date mLastCheckDate;
    private List<UpdateListener> mListeners = new ArrayList<UpdateListener>();
    private Handler mHandler;
    private String mControllerVersion;

    private Handler.Callback mHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case NOTIFY_UI:
                    notifyUi();
                    break;
                case NOTIFY_DOWNLOAD_PROGRESS:
                    notifyDownloadProgress(msg.arg1, msg.arg2);
                    break;
                case NOTIFY_NETWORK_STATE:
                    notifyNetworkState();
                    break;
                case NOTIFY_FINISH:
                    notifyFinish();
                    break;
            }
            return true;
        }
    };
    private boolean mIsInitialzied;

    private void notifyFinish() {
        for (UpdateListener listener : mListeners) {
            listener.finish();
        }
    }

    private BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            log("network change event");
            sendEmptyMessage(NOTIFY_NETWORK_STATE);
        }
    };

    public void checkForOTAUpdates() {
        Intent intent = new Intent();
        intent.setAction("aneeda.CHECKS_FOR_UPDATES");
        intent.setPackage("com.iamplus.jewel");
        mContext.startService(intent);
    }

    private UpdateManager(Context context) {
        mContext = context;
        loadState();
        mUpdateLog = new UpdateLog(context, Config.UPDATE_LOG);
        mHandler = new Handler(mContext.getMainLooper(), mHandlerCallback);
    }

    public String getVersion() {
        return mControllerVersion;
    }

    public static UpdateManager getInstance(Context context) {
        if (mUpdateManager == null) {
            mUpdateManager = new UpdateManager(context.getApplicationContext());
        }
        return mUpdateManager;
    }

    public static void doStartup(Context context) {
        log("doStartup");
        JobUtils.startBootupJob(context);
    }

    private static void log(String message) {
        Log.d(TAG, message);
    }

    public void save() {
        saveState();
    }

    public State getState() {
        return mState;
    }

    public void setState(State state) {
        mState = state;
        clearError();
        sendEmptyMessage(NOTIFY_UI);
        log("State: " + mState);
    }

    public boolean hasError() {
        return mErrorStatus != ErrorCodes.NO_ERROR;
    }

    public ErrorCodes getErrorStatus() {
        return mErrorStatus;
    }

    public void setErrorStatus(ErrorCodes error) {
        mErrorStatus = error;
    }

    public int getErrorText() {
        switch (mErrorStatus) {
            case NO_ERROR:
                return R.string.no_error;
            case NO_NETWORK:
                return R.string.no_network;
            case NO_WIFI:
                return R.string.no_wifi;
            case WAITING_NETWORK:
                return R.string.waiting_network;
            case INCORRECT_VERSION:
                return R.string.incorrect_version;
            case UPDATE_CORRUPT:
                return R.string.update_corrupt;
            case UPDATE_FILE_MISSING:
                return R.string.update_file_missing;
            case DOWNLOAD_FAILED:
                return R.string.download_failed;
            case VERIFY_FAILED:
                return R.string.verify_failed;
            case SIGNATURE_MISMATCH:
                return R.string.signature_mismatch;
            case BATTERY_LOW:
                return R.string.battery_low;
            case UPDATE_LOCK_HELD:
                return R.string.update_lock_held;
            case INSTALL_FAILED:
                return R.string.install_failed;
            case UNKNOWN_ERROR:
            default:
                return R.string.unknown_error;
        }
    }

    public void clearError() {
        mErrorStatus = ErrorCodes.NO_ERROR;
    }

    public UpdateInfo getUpdateInfo() {
        return mUpdateInfo;
    }

    public void setUpdateInfo(UpdateInfo updateInfo) {
        mUpdateInfo = updateInfo;
    }

    public int getDownloadPercentage() {
        return mDownloadPercentage;
    }

    public int getDownloadSize() {
        return mDownloadSize;
    }

    public void setState(State state, ErrorCodes errorStatus) {
        mState = state;
        mErrorStatus = errorStatus;
        sendEmptyMessage(NOTIFY_UI);
        log("State: " + mState);
        log("ErrorStatus: " + mErrorStatus);
    }

    public Date getLastUpdateCheckDate() {
        return mLastCheckDate;
    }

    public void updateLastCheckDate() {
        mLastCheckDate = new Date();
    }

    public void setDownloadPercentage(int percentage, int downloadSize) {
        mDownloadPercentage = percentage;
        mDownloadSize = downloadSize;
        Message msg = mHandler.obtainMessage();
        msg.what = NOTIFY_DOWNLOAD_PROGRESS;
        msg.arg1 = percentage;
        msg.arg2 = downloadSize;
        mHandler.sendMessage(msg);
    }

    public void appendLog(String message) {
        mUpdateLog.append(message);
    }

    public void appendFileToLog(File file) {
        mUpdateLog.appendFileToLog(file);
    }

    public void clearLog() {
        mUpdateLog.clear();
    }

    public String getLog() {
        return mUpdateLog.toString();
    }

    public void register(UpdateListener listener) {

        if (!hasListeners()) {
            listenForConnectivityChange();
        }

        mListeners.add(listener);
    }

    public void unregister(UpdateListener listener) {
        mListeners.remove(listener);

        if (!hasListeners()) {
            tryPostponeUpdate();
            restoreValidState();
            save();
            stopListeningForConnectivity();
        }
    }

    public void doAutoCheckForUpdates() {
        if (isOldEnough() && Config.getAutoUpdateConfig(mContext)) {
            log("doAutoCheckForUpdates");
            JobUtils.startPeriodicQueryUpdateJob(mContext);
        }
    }

    public void checkForUpdates() {
        checkForUpdates(IntiatedBy.USER);
    }

    public void checkForUpdates(IntiatedBy initiatedBy) {
        log("checkForUpdates mState:" + mState + " fromVersion:" + mControllerVersion);

        resetCancel();
        switch (mState) {
            case IDLE:
            case NO_UPDATES:
            case UPDATE_DONE:
                if (mControllerVersion != null) {
                    log("Quering for updates, initiated by : " + initiatedBy);
                    mInitiatedBy = initiatedBy;
                    setState(State.QUERYING_UPDATE);
                    JobUtils.startQueryUpdateJob(mContext, mControllerVersion);
                }
                break;
            case UPDATE_AVAILABLE:
                UpdateInfo ui = getUpdateInfo();
                if (ui == null) {
                    log("Invalid state");
                    cancelUpdate();
                    checkForUpdates();
                }
            case UPDATE_READY:
                sendEmptyMessage(NOTIFY_UI);
                break;
            default:
                log("In state " + mState + " . Not querying for updates");
        }
    }

    public void autoDownloadUpdate() {
        log("autoDownloadUpdate task scheduled after 1 min");
        switch (mState) {
            case UPDATE_AVAILABLE:
                JobUtils.startAutoDownloadUpdateJob(mContext);
                break;
            default:
                log("Invalid state: " + mState);
                break;
        }
    }

    public void downloadAndInstallUpdate() {
        log("downloadAndInstallUpdate");
        JobUtils.cancelJob(mContext, UpdateService.JOB_AUTO_DOWNLOAD_UPDATE);
        downloadUpdate();
    }

    public void downloadUpdate() {
        log("downloadUpdate");
        resetCancel();
        UpdateInfo ui = getUpdateInfo();
        if (ui == null) {
            log("No UpdateInfo available");
            return;
        }

        switch (mState) {
            case UPDATE_AVAILABLE:
            case DOWNLOADING_UPDATE:
                log("Initial download");
                mDownloadRetryCount = 0;
                mDownloadSize = 0;
                mDownloadPercentage = -1;
                break;
            case DOWNLOAD_PAUSED:
                log("Download retry");
                break;
            default:
                log("Invalid state: " + mState);
                return;
        }

        setState(State.DOWNLOAD_PAUSED, ErrorCodes.WAITING_NETWORK);
        JobUtils.startDownloadService(mContext);
    }

    public void autoInstallUpdate() {
        log("autoInstallUpdate task scheduled after 1 min");
        switch (mState) {
            case INSTALLING_UPDATE:
                JobUtils.startAutoInstallUpdateJob(mContext);
                setState(State.IDLE);
                break;
            default:
                log("Invalid state: " + mState);
                break;
        }
    }

    private void fileList(File file) {
        File[] fileList = file.listFiles();
        if (fileList != null) {
            String name;
            for (File f : fileList) {
                name = f.getName();
                appendLog(f.getAbsolutePath() + name);
                if (name.contentEquals("recovery")) {
                    fileList(f);
                }
            }
        }
    }

    public void dumpCacheContents() {
        File file = new File(Config.CACHE_LOCATION);
        appendLog("Usable space in cache: " + file.getUsableSpace());
        appendLog("Dumping cache contents...");
        fileList(file);
    }

    public void doUpdate() {
        /* TODO kaustubh
        setState(UpdateManager.State.INSTALLING_UPDATE);
        save();

        UpdateInfo ui = mUpdateManager.getUpdateInfo();

        log("rebooting into bootloader ..");
        try {
            RecoverySystem.installPackage(mContext, ui.getFile(mContext));
        } catch (IOException e) {
            log_exception("Exception while applying delta file", e);
            mUpdateManager.onApplyUpdateFailed(UpdateManager.ErrorCodes.UNKNOWN_ERROR);
            File file = new File(Config.INSTALL_SUCCESS_FILE);
            appendLog("Deleting file: " + Config.INSTALL_SUCCESS_FILE);
            if (file != null) {
                file.delete();
            }
            JobUtils.startPostUpdateJob(mContext);
        }
        */
        log("end of doUpdate");
    }

    public void applyUpdate() {
        log("applyUpdate");

        switch (getState()) {
            case WAITING_TO_INSTALL:
                break;
            default:
                log("applyUpdate Cancelled..Not in proper state: " + getState());
                return;
        }

        UpdateInfo ui = getUpdateInfo();
        log("UpdateInfo " + ui);

        if (!ui.isFileExists(mContext)) {
            log("Update file not available..");
            onApplyUpdateFailed(UpdateManager.ErrorCodes.UPDATE_FILE_MISSING);
            return;
        }

        long updateTime = System.currentTimeMillis();
        SharedPreferences pref = Config.getSharedPreference(mContext);
        pref.edit().putString(Config.PREF_OLD_VERSION, getVersion()).commit();
        pref.edit().putLong(Config.PREF_UPDATE_TIME, updateTime).commit();
        log("time = " + updateTime);

        JobUtils.startUpdateMonitor(mContext, "phoneState");
        appendLog("UpdateInfo " + ui);
        return;
    }

    public void installUpdate() {
        log("installUpdate");
        switch (mState) {
            case WAITING_TO_INSTALL:
            case INSTALLING_UPDATE:
                log("Already installing update");
                return;
            default:
                break;
        }

        JobUtils.cancelJob(mContext, UpdateService.JOB_AUTO_INSTALL_UPDATE);

        UpdateInfo ui = getUpdateInfo();
        if (ui == null) {
            log("No UpdateInfo available");
            return;
        }

        if (!ui.isFileExists(mContext)) {
            log("Update file not available");
            setState(State.ERROR, ErrorCodes.UPDATE_FILE_MISSING);
            return;
        }

        if (!isBatteryEnough()) {
            log("low battery");
            JobUtils.startUpdateMonitor(mContext, "battery");
            setState(State.WAITING_LOW_BATTERY);
            return;
        }

        setState(State.WAITING_TO_INSTALL);
        mPostponeCount = 0;
        applyUpdate();
    }

    public void doNetworkUpdate(UpdateInfo ui) {
        log("doNetworkUpdate");
        resetCancel();
        if (!isValidVersion(ui)) {
            log("invalid version. Ignoring update.");
            return;
        }

        switch (mState) {
            case UPDATE_READY:
            case WAITING_TO_INSTALL:
            case WAITING_LOW_BATTERY:
                log("Cancel pending update");
                forceCancelUpdate();
                break;
            default:
                break;
        }

        mInitiatedBy = IntiatedBy.NETWORK;
        setUpdateInfo(ui);
        setState(State.DOWNLOADING_UPDATE);
        downloadUpdate();
    }

    private void tryPostponeUpdate() {
        switch (mState) {
            case UPDATE_READY:
                log("postpone update");
                mPostponeCount++;
                JobUtils.startRemindUpdateToUserJob(mContext);
                break;
            default:
                break;
        }
    }

    public void postponeUpdate() {
    }

    public boolean canCancelUpdate() {
        switch (mState) {
            case IDLE:
            case QUERYING_UPDATE:
            case NO_UPDATES:
            case INSTALLING_UPDATE:
            case UPDATE_DONE:
            case ERROR:
                return false;
            default:
                return true;
        }
    }

    private void forceCancelUpdate() {
        log("forceCancelUpdate");
        log("Cancelling all jobs");
        mCancellingJobs = true;
        JobUtils.cancelAllJobs(mContext);
        JobUtils.stopUpdateMonitor(mContext);
        JobUtils.stopDownloadService(mContext);
        removeUpdateFile();
        resetToIdle();
        save();
    }

    public void cancelUpdate() {
        log("cancelUpdate");

        UpdateInfo ui = getUpdateInfo();

        forceCancelUpdate();
        if (ui == null) {
            return;
        }
        if (ui.isCritical) {
            log("Critical update cannot be cancelled");
            return;
        } else if (!canCancelUpdate()) {
            log("Update cannot be cancelled " + mState);
            return;
        }
        setState(State.IDLE);
    }

    public void restoreValidState() {
        log("restoreValidState mState:" + mState);
        resetCancel();
        switch (mState) {
            case IDLE:
            case NO_UPDATES:
            case UPDATE_DONE:
            case ERROR:
                setState(State.IDLE);
                doAutoCheckForUpdates();
                Utils.cancelNotifications(mContext);
                mRecheckUpdate = false;
                break;
            default:
                break;
        }
    }

    public void restoreValidStateIfNoUi() {
        log("restoreValidStateIfNoUi");
        if (!hasListeners()) {
            restoreValidState();
        }
    }

    public void resetToIdle() {
        log("resetToIdle");
        setState(State.IDLE);
        clearImages();
        mRecheckUpdate = false;
    }

    private void clearImages() {
        String name = null;
        boolean deleted = false;
        File[] list = new File(Config.getInstallLocation(mContext)).listFiles();

        if (list == null) {
            return;
        }
        for (File file : list) {
            name = file.getName();
            deleted = file.delete();
            log("File (" + name + ") deletion Status (" + deleted + ")");
        }
    }

    public void remindUser() {
        switch (mState) {
            case UPDATE_READY:
                log("remindUser UPDATE_READY");
                if (!hasListeners()) {
                    sendUpdateReadyNotification();
                }
                break;
            default:
                log("ignoring remindUser for " + mState);
                break;
        }
    }

    public void removeUpdateFile() {
        log("removeUpdateFile");
        UpdateInfo ui = getUpdateInfo();
        if (ui == null) {
            log("no update info found");
            return;
        }
        File f = ui.getFile(mContext);
        if (f.exists()) {
            log("Deleting file " + f.getAbsolutePath());
            boolean result = f.delete();
            if (!result) {
                log("Unable to delete file " + f.getAbsolutePath());
            } else {
                log("Successfully deleted file " + f.getAbsolutePath());
            }
        } else {
            log("Update file does not exist " + f.getAbsolutePath());
        }
    }

    public boolean isValidVersion(UpdateInfo ui) {

        if (getVersion() == null) {
            log("Current Version is NULL");
            return false;
        }
        boolean valid = getVersion().equals(ui.from_version);
        if (!valid) {
            log("Invalid version " + ui.from_version + " current version " + getVersion());
        }
        return valid;
    }

    public boolean isBatteryEnough() {
        return Utils.getBatteryLevel(mContext) >= Config.MINIMUM_BATTERY_PERCENTAGE;
    }

    public boolean onQueryFailed() {
        log("Query failed");
        // Will retry.
        return false;
    }

    public void onQueryCancelled() {
        log("Query cancelled");
        if (isCancelling()) {
            log("cancelling jobs..");
            return;
        }
        resetToIdle();
    }

    private void setAndRestoreStates() {
        if (!mRecheckUpdate) {
            setState(State.NO_UPDATES);
            restoreValidStateIfNoUi();
        } else {
            log("Re-check updates done");
            mRecheckUpdate = false;
            setState(State.UPDATE_DONE);
        }
    }

    public void onNoUpdatesAvailable() {
        log("No updates available");
        setAndRestoreStates();
    }

    public void onUpdateAvailable(UpdateInfo ui) {
        log("onUpdateAvailable");
        if (!isValidVersion(ui)) {
            setAndRestoreStates();
            return;
        }

        setUpdateInfo(ui);
        setState(State.UPDATE_AVAILABLE);

        if (ui.isCritical) {
            log("Critical update: Auto download in 1 min");
            autoDownloadUpdate();
        }

        if (!hasListeners()) {
            if (ui.isCritical) {
                sendCriticalUpdateAvailableNotification();
            } else {
                sendUpdateAvailableNotification();
            }
        }
    }

    public void onExistingUpdateAvailable() {
        log("onExistingUpdateAvailable.");
        if (!hasListeners()) {
            UpdateInfo ui = getUpdateInfo();
            if (ui != null && ui.isCritical) {
                sendCriticalUpdateAvailableNotification();
            } else {
                sendUpdateAvailableNotification();
            }
        }
    }

    public void onDownloadStarted() {
        log("Download started..");
        setState(State.DOWNLOADING_UPDATE);
    }

    public void onDownloadComplete() {
        log("Download successful.");
        mDownloadRetryCount = 0;
        JobUtils.startVerifyUpdateJob(mContext);
    }

    public boolean onDownloadFailed() {
        log("Download failed");
        mDownloadRetryCount++;

        if (mDownloadRetryCount < Config.MAX_DOWNLOAD_RETRY_COUNT) {
            log("will retry, count=" + mDownloadRetryCount);
            setState(State.DOWNLOAD_PAUSED, ErrorCodes.WAITING_NETWORK);
            return true;
        } else {
            log("Max retry count reached");
            setState(State.UPDATE_AVAILABLE);
            return false;
        }
    }

    public void onDownloadCancelled() {
        log("Download cancelled");
        removeUpdateFile();
        mDownloadRetryCount = 0;
        if (isCancelling()) {
            log("cancelling jobs");
            return;
        }
        switch (mInitiatedBy) {
            case NONE:
            case DEVICE:
            case NETWORK:
                resetToIdle();
                break;
            case USER:
                setState(State.ERROR, ErrorCodes.DOWNLOAD_FAILED);
                restoreValidStateIfNoUi();
        }
    }

    public void onVerifyComplete(boolean result, ErrorCodes errorCode) {
        log("onVerifyComplete");
        if (result) {
            setState(State.UPDATE_READY);
            UpdateInfo ui = getUpdateInfo();
            boolean isCritical = false;
            if (ui != null && ui.isCritical) {
                log("Critical update: Auto install in 1 min");
                //autoInstallUpdate();
                isCritical = true;
            }

            if (!hasListeners()) {
                if (isCritical) {
                    sendCriticalUpdateReadyNotification();
                } else {
                    sendUpdateReadyNotification();
                }
            }
        } else {
            setState(State.IDLE);
            Utils.cancelNotifications(mContext);
            //setErrorStatus(errorCode);
            removeUpdateFile();
            restoreValidStateIfNoUi();
        }
    }

    public void onUpdateReady() {
        log("onUpdateReady");
        if (!hasListeners()) {
            UpdateInfo ui = getUpdateInfo();
            if (ui != null && ui.isCritical) {
                sendCriticalUpdateReadyNotification();
            } else {
                sendUpdateReadyNotification();
            }
        }
    }

    public void onBatteryLevelOk() {
        log("onBatteryLevelOk");
        switch (mState) {
            case WAITING_LOW_BATTERY:
                break;
            default:
                log("invalid state" + mState);
                return;
        }
        installUpdate();
    }

    public void onApplyUpdateFailed(ErrorCodes errorCode) {
        log("onApplyUpdateFailed " + errorCode);
        setState(State.ERROR);
        setErrorStatus(errorCode);
    }

    public void onUpdateComplete(boolean result) {
        log("onUpdateComplete");
        clearLog();
        save();
        if (result) {
            updateVersion();
            setState(State.UPDATE_DONE);
        } else {
            setState(State.ERROR, ErrorCodes.INSTALL_FAILED);
        }
        if (!hasListeners()) {
            sendUpdateDoneNotification(result);
        }

        /*if (result) {
            log("Re-check updates if any");
            mRecheckUpdate = true;
            checkForUpdates(IntiatedBy.DEVICE);
        }*/
    }

    private void updateVersion() {
        UpdateInfo ui = getUpdateInfo();
        if (ui == null) {
            Log.v(TAG,"UpdateInfo Null");
            return;
        }
        mControllerVersion = ui.to_version;
    }

    public String getChannel() {
        return Config.getChannel(mContext);
    }

    public String getBaseUrl() {
        return Config.getBaseUrl(mContext);
    }

    public void setChannel(String channel, String url) {
        if (channel == null) {
            log("Channel null during setChannel");
            return;
        }
        if (url == null) {
            log("url is null..");
            return;
        }
        log("Set channel " + channel + " url " + url);
        String currentChannel = getChannel();
        boolean hasUrl = true;
        String currentUrl = getBaseUrl();
        if (currentUrl == null) {
            log("Setting base url " + url);
            hasUrl = false;
            Config.setBaseUrl(mContext, url);
        }
        if (currentChannel.equals(channel)) {
            log("Same channel");
            return;
        }
        Config.setChannel(mContext, channel);
        Config.setBaseUrl(mContext, url);
        if (hasUrl) {
            log("Force cancelling the update..");
            forceCancelUpdate();
        } else {
            log("Normal cancel update");
            cancelUpdate();
            log("calling startup job again ..");
            JobUtils.startBootupJob(mContext);
        }
        save();
        log("re-register again..");
        JobUtils.startRegisterCdnJob(mContext, currentChannel, currentUrl);
    }

    public String getRegistrationId() {
        return mRegistrationId;
    }

    public void setRegistrationId(String id) {
        mRegistrationId = id;
    }

    public void loadUrls() {
        log("loadUrls");
        // TODO kaustubh
//        new ServiceDiscoveryFactory(mContext).getServiceDiscovery(Config.SERVICE_NAME, new ServiceDiscoveryFactory.Callback() {
//            @Override
//            public void onServiceDiscoveryAvailable(ServiceDiscovery service) {
//                log("onServiceDiscoveryAvailable");
//                String url = service.getActiveUrl();
//                String channel = service.getActiveChannel().name;
//                setChannel(channel, url);
//                service.close();
//            }
//        });
    }

    private void sendEmptyMessage(int id) {
        mHandler.sendEmptyMessage(id);
    }

    private void resetCancel() {
        mCancellingJobs = false;
    }

    private boolean isCancelling() {
        return mCancellingJobs;
    }

    private void notifyUi() {
        for (UpdateListener listener : mListeners) {
            listener.onStateChanged();
        }
    }

    private void notifyDownloadProgress(int percentage, int downloadSize) {
        for (UpdateListener listener : mListeners) {
            listener.onDownloadProgressChanged(percentage, downloadSize);
        }
    }

    private void notifyNetworkState() {
        for (UpdateListener listener : mListeners) {
            listener.onNetworkChange(Utils.isOnline(mContext));
        }
    }

    private boolean hasListeners() {
        Log.v(TAG,"hasListeners::" + mListeners.size());
        return mListeners.size() != 0;
    }

    private PendingIntent getPendingIntent() {
        Intent i = new Intent("aneeda.UPDATE_BUTTONS");
        return PendingIntent.getActivity(mContext, 0, i, 0);
    }

    private void sendNotification(int id) {
        String text = mContext.getResources().getString(id);
        sendNotification(text);
    }

    public void sendNotification(String text) {
        Utils.sendNotification(mContext, text, getPendingIntent());
    }

    public void sendEmptyNotification(String title, String text) {
        Utils.sendNotification(mContext, title, text, PendingIntent.getActivity(mContext, 0, new Intent(), 0));
    }

    private void sendUpdateDoneNotification(boolean result) {
        int id;
        UpdateInfo ui = getUpdateInfo();

        if (ui != null && ui.isCritical) {
            id = result ? R.string.critical_update_success : R.string.critical_update_failed;
        } else {
            id = result ? R.string.update_success : R.string.update_failed;
        }
        sendNotification(id);
    }

    private void sendUpdateAvailableNotification() {
        sendNotification(R.string.update_available);
    }

    private void sendCriticalUpdateAvailableNotification() {
        sendNotification(R.string.critical_update_available);
    }

    public void sendUpdateReadyNotification() {
        sendNotification(R.string.update_ready);
    }

    private void sendCriticalUpdateReadyNotification() {
        sendNotification(R.string.critical_update_ready);
    }

    private void listenForConnectivityChange() {
        IntentFilter filters = new IntentFilter();
        filters.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        mContext.registerReceiver(mNetworkReceiver, filters);
    }

    private void stopListeningForConnectivity() {
        mContext.unregisterReceiver(mNetworkReceiver);
    }

    private boolean isOldEnough() {
        if (mLastCheckDate.equals(new Date(0))) {
            return true;
        } else {
            long diff = Math.abs(new Date().getTime() - mLastCheckDate.getTime());
            if (diff > Config.AUTO_CHECK_DURATION || !mIsInitialzied) {
                mIsInitialzied = true;
                return true;
            }
        }
        return false;
    }

    private void loadState() {
        try {
            JSONObject j = Utils.readJsonFromFile(mContext, Config.SAVE_FILE);
            String state = j.optString("state", "IDLE");
            try {
                mState = State.valueOf(state);
            } catch (IllegalArgumentException e) {
                log("Unknown state " + state);
                mState = State.IDLE;
            }

            String errorStatus = j.optString("error_status", "NO_ERROR");
            try {
                mErrorStatus = ErrorCodes.valueOf(errorStatus);
            } catch (IllegalArgumentException e) {
                log("Unknown errorStatus " + errorStatus);
                mErrorStatus = ErrorCodes.NO_ERROR;
            }


            String initated_by = j.optString("initated_by", "NONE");
            try {
                mInitiatedBy = IntiatedBy.valueOf(initated_by);
            } catch (IllegalArgumentException e) {
                log("Unknown initated_by " + initated_by);
                mInitiatedBy = IntiatedBy.NONE;
            }

            if (j.has("update_info")) {
                mUpdateInfo = new UpdateInfo();
                mUpdateInfo.loadFromJson(j.getJSONObject("update_info"));
            }
            mDownloadRetryCount = j.optInt("download_retry", 0);
            mPostponeCount = j.optInt("postpone_count", 0);
            String default_date = DateFormat.getDateTimeInstance().format(new Date(0));
            String date = j.optString("last_check_date", default_date);
            try {
                mLastCheckDate = DateFormat.getDateTimeInstance().parse(date);
            } catch (ParseException e) {
                log_exception("unable to parse date " + date, e);
                mLastCheckDate = new Date(0);
            }

        } catch (JSONException e) {
            log_exception("JSONException while loading state", e);
        }

    }

    private void saveState() {

        try {
            JSONObject j = new JSONObject();
            j.put("state", mState.toString());
            j.put("error_status", mErrorStatus.toString());
            j.put("initated_by", mInitiatedBy.toString());
            j.put("postpone_count", mPostponeCount);
            j.put("last_check_date", DateFormat.getDateTimeInstance().format(mLastCheckDate));
            j.put("download_retry", mDownloadRetryCount);

            if (mUpdateInfo != null) {
                j.put("update_info", mUpdateInfo.saveToJson());
            }

            Utils.writeJSONtoFile(mContext, Config.SAVE_FILE, j);
        } catch (JSONException e) {
            log_exception("JSONException while saveState", e);
        }
    }

    private void log_exception(String message, Exception e) {
        Log.d(TAG, message, e);
    }

    public void setControllerVersion(String controllerVersion) {
        mControllerVersion = controllerVersion;
        log("Controller version: " + mControllerVersion);
    }

    public enum State {
        IDLE,
        QUERYING_UPDATE,
        NO_UPDATES,
        UPDATE_AVAILABLE,
        DOWNLOADING_UPDATE,
        DOWNLOAD_PAUSED,
        VALIDATE_UDPATE,
        UPDATE_READY,
        WAITING_TO_INSTALL,
        WAITING_LOW_BATTERY,
        INSTALLING_UPDATE,
        UPDATE_DONE,
        ERROR;
    }

    public enum ErrorCodes {
        NO_ERROR,
        NO_NETWORK,
        NO_WIFI,
        WAITING_NETWORK,
        INCORRECT_VERSION,
        UPDATE_CORRUPT,
        UPDATE_FILE_MISSING,
        DOWNLOAD_FAILED,
        VERIFY_FAILED,
        SIGNATURE_MISMATCH,
        BATTERY_LOW,
        UPDATE_LOCK_HELD,
        INSTALL_FAILED,
        UNKNOWN_ERROR;
    }

    public enum IntiatedBy {
        NONE,
        USER,
        NETWORK,
        DEVICE;
    }

    public interface UpdateListener {
        void onStateChanged();

        void onDownloadProgressChanged(int percentage, int downloadSize);

        void onNetworkChange(boolean connected);

        void finish();
    }
}


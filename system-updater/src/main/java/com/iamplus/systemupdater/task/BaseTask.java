package com.iamplus.systemupdater.task;

import android.app.job.JobParameters;
import android.os.AsyncTask;
import android.content.Context;
import android.util.Log;

import com.iamplus.systemupdater.UpdateService;
import com.iamplus.systemupdater.UpdateManager;

public abstract class BaseTask extends AsyncTask<Void, Integer, Boolean> {
    public static final String TAG = "FOTA UpdateService.";

    protected UpdateManager mUpdateManager;
    protected UpdateService mUpdateService;
    protected JobParameters mParams;

    public void setInfo(UpdateService service, UpdateManager updateManager, JobParameters params) {
        mUpdateService = service;
        mUpdateManager = updateManager;
        mParams = params;
    }

    public JobParameters getParams() {
        return mParams;
    }

    public int getJobId() {
        return mParams.getJobId();
    }

    public Context getContext() {
        return mUpdateService;
    }

    public Boolean doInBackground(Void... args) {
        return doInBackground();
    }

    public UpdateManager.State getState() {
        return mUpdateManager.getState();
    }

    protected abstract boolean doInBackground();

    protected boolean onPostUi(boolean result) {
        return result;
    }

    public void onPostExecute(Boolean result) {
        finish(onPostUi(result));
    }

    public void onCancel() {
        finish(false);
    }

    public void finish(boolean result) {
        mUpdateService.finishTask(this, result);
    }

    public void log(String message) {
        String tag = TAG + getClass().getSimpleName();
        Log.d(tag, message);
    }

    public void log_exception(String message, Exception e) {
        String tag = TAG + getClass().getSimpleName();
        Log.e(tag, message, e);
    }
}
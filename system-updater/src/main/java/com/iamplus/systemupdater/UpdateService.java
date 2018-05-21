package com.iamplus.systemupdater;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import com.iamplus.systemupdater.task.AutoDownloadUpdateTask;
import com.iamplus.systemupdater.task.AutoInstallUpdateTask;
import com.iamplus.systemupdater.task.AutoQueryUpdatesTask;
import com.iamplus.systemupdater.task.BaseTask;
import com.iamplus.systemupdater.task.DownloadUpdateTask;
import com.iamplus.systemupdater.task.PostDeviceDetailsTask;
import com.iamplus.systemupdater.task.PostUpdateTask;
import com.iamplus.systemupdater.task.PushNotificationTask;
import com.iamplus.systemupdater.task.QueryUpdatesTask;
import com.iamplus.systemupdater.task.RegisterTask;
import com.iamplus.systemupdater.task.RemindUserJob;
import com.iamplus.systemupdater.task.StartupTask;
import com.iamplus.systemupdater.task.VerifyUpdateTask;

import java.util.ArrayList;
import java.util.List;

public class UpdateService extends JobService {
    public static final int JOB_DUMMY = 99;
    public static final int JOB_STARTUP = 100;
    public static final int JOB_PUSH_NOTIFICATION = 101;
    public static final int JOB_REGISTE_CDM = 102;
    public static final int JOB_POST_DEVICE_DETAILS = 103;
    public static final int JOB_QUERY_UPDATES = 104;
    public static final int JOB_POST_UPDATE = 105;
    public static final int JOB_DOWNLOAD_UPDATE = 106;
    public static final int JOB_VERIFY_UPDATE = 107;
    public static final int JOB_REMIND_USER = 108;
    public static final int JOB_AUTO_QUERY_UPDATES = 109;
    public static final int JOB_AUTO_INSTALL_UPDATE = 110;
    public static final int JOB_AUTO_DOWNLOAD_UPDATE = 111;
    private static final String TAG = "FOTA UpdateService";
    private UpdateManager mUpdateManager;
    private List<BaseTask> mTasks = new ArrayList<BaseTask>();

    @Override
    public boolean onStartJob(JobParameters params) {
        int id = params.getJobId();
        if (id <= JOB_DUMMY) {
            log("dummy task ");
            return false;
        }

        log("onStartJob " + id + " name " + getTaskName(id));


        BaseTask task = getTask(id);
        if (task == null) {
            log("Unknown task " + id);
            return false;
        }

        task.setInfo(this, mUpdateManager, params);
        mTasks.add(task);
        task.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        int id = params.getJobId();
        log("onStopJob " + id + " name " + getTaskName(id));
        BaseTask task = findTask(params);
        if (task == null) {
            log("Task not found.");
            return false;
        }
        task.cancel(false);
        mTasks.remove(task);
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mUpdateManager = UpdateManager.getInstance(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUpdateManager.save();
        mUpdateManager = null;
    }

    private void log(String message) {
        Log.d(TAG, message);
    }

    public BaseTask findTask(JobParameters params) {
        for (BaseTask task : mTasks) {
            if (task.getJobId() == params.getJobId()) return task;
        }
        return null;
    }

    public void finishTask(BaseTask task, boolean result) {
        int jobId = task.getJobId();
        log("finishing task " + jobId + " result " + result + " name " + getTaskName(jobId));
        jobFinished(task.getParams(), !result);
        mTasks.remove(task);
    }

    private String getTaskName(int job) {
        switch (job) {
            case JOB_STARTUP:
                return "JOB_STARTUP";
            case JOB_PUSH_NOTIFICATION:
                return "JOB_PUSH_NOTIFICATION";
            case JOB_REGISTE_CDM:
                return "JOB_REGISTE_CDM";
            case JOB_POST_DEVICE_DETAILS:
                return "JOB_POST_DEVICE_DETAILS";
            case JOB_QUERY_UPDATES:
                return "JOB_QUERY_UPDATES";
            case JOB_POST_UPDATE:
                return "JOB_POST_UPDATE";
            case JOB_DOWNLOAD_UPDATE:
                return "JOB_DOWNLOAD_UPDATE";
            case JOB_VERIFY_UPDATE:
                return "JOB_VERIFY_UPDATE";
            case JOB_REMIND_USER:
                return "JOB_REMIND_USER";
            case JOB_AUTO_QUERY_UPDATES:
                return "JOB_AUTO_QUERY_UPDATES";
            case JOB_AUTO_INSTALL_UPDATE:
                return "JOB_AUTO_INSTALL_UPDATE";
            case JOB_AUTO_DOWNLOAD_UPDATE:
                return "JOB_AUTO_DOWNLOAD_UPDATE";
            default:
                return "Unknown job " + job;
        }
    }

    private BaseTask getTask(int job) {
        switch (job) {
            case JOB_STARTUP:
                return new StartupTask();
            case JOB_PUSH_NOTIFICATION:
                return new PushNotificationTask();
            case JOB_REGISTE_CDM:
                return new RegisterTask();
            case JOB_POST_DEVICE_DETAILS:
                return new PostDeviceDetailsTask();
            case JOB_QUERY_UPDATES:
                return new QueryUpdatesTask();
            case JOB_POST_UPDATE:
                return new PostUpdateTask();
            case JOB_DOWNLOAD_UPDATE:
                return new DownloadUpdateTask();
            case JOB_VERIFY_UPDATE:
                return new VerifyUpdateTask();
            case JOB_REMIND_USER:
                return new RemindUserJob();
            case JOB_AUTO_QUERY_UPDATES:
                return new AutoQueryUpdatesTask();
            case JOB_AUTO_INSTALL_UPDATE:
                return new AutoInstallUpdateTask();
            case JOB_AUTO_DOWNLOAD_UPDATE:
                return new AutoDownloadUpdateTask();
            default:
                log("Unknown job " + job);
                return null;
        }
    }
}

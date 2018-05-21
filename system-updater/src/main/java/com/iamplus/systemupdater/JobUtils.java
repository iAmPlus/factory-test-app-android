package com.iamplus.systemupdater;


import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;

public class JobUtils {

    public static JobInfo.Builder createJob(Context context, int jobId) {
        ComponentName serviceName = new ComponentName(context, UpdateService.class);
        return new JobInfo.Builder(jobId, serviceName);
    }

    public static JobInfo.Builder createNetworkJob(Context context, int jobId) {
        return createJob(context, jobId).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setBackoffCriteria(Config.MIN, JobInfo.BACKOFF_POLICY_LINEAR);
    }

    public static JobInfo.Builder createWifiNetworkJob(Context context, int jobId) {
        return createJob(context, jobId).setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setBackoffCriteria(Config.BACKOFF_DURATION, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
    }

    public static int startJob(Context context, JobInfo jobInfo) {
        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        return scheduler.schedule(jobInfo);
    }

    public static void startDummyJob(Context context) {
        JobInfo dummy = createNetworkJob(context, UpdateService.JOB_DUMMY).build();
        startJob(context, dummy);
    }


    public static void startBootupJob(Context context) {
        JobInfo ji = createJob(context, UpdateService.JOB_STARTUP)
                .setOverrideDeadline(1000)  // Start 1 sec after boot.
                .build();
        startJob(context, ji);
        JobInfo dummy = createJob(context, UpdateService.JOB_DUMMY - 1)
                .setOverrideDeadline(1000)  // Start 1 sec after boot.
                .build();
        startJob(context, dummy);
    }

    public static void startRegisterCdnJob(Context context, String oldChannel, String oldUrl) {
        PersistableBundle bundle = new PersistableBundle();
        if (oldChannel != null) {
            bundle.putString(Config.OLD_CHANNEL, oldChannel);
            bundle.putString(Config.OLD_BASE_URL, oldUrl);
        }

        JobInfo ji =
                createNetworkJob(context, UpdateService.JOB_REGISTE_CDM).setExtras(bundle).build();
        startJob(context, ji);
        startDummyJob(context);
    }

    public static void startPushNotificationJob(Context context, PersistableBundle bundle) {
        JobInfo ji =
                createNetworkJob(context, UpdateService.JOB_PUSH_NOTIFICATION).setExtras(bundle)
                        .build();

        startJob(context, ji);
        startDummyJob(context);
    }

    public static void startPostDeviceDetailJob(Context context) {
        JobInfo ji = createNetworkJob(context, UpdateService.JOB_POST_DEVICE_DETAILS).build();
        startJob(context, ji);
        startDummyJob(context);
    }

    public static void startQueryUpdateJob(Context context, String from_version) {

        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(Config.UPDATER_VERSION, from_version);
        JobInfo ji = createNetworkJob(context, UpdateService.JOB_QUERY_UPDATES).setExtras(bundle).build();

        startJob(context, ji);
        startDummyJob(context);
    }

    public static void startDownloadUpdateJob(Context context, boolean isWifiOnly) {
        JobInfo ji;
        if (!isWifiOnly) {
            ji = createNetworkJob(context, UpdateService.JOB_DOWNLOAD_UPDATE).build();
        } else {
            ji = createWifiNetworkJob(context, UpdateService.JOB_DOWNLOAD_UPDATE).build();
        }
        startJob(context, ji);
        startDummyJob(context);
    }

    public static void startVerifyUpdateJob(Context context) {
        JobInfo ji = createNetworkJob(context, UpdateService.JOB_VERIFY_UPDATE).build();
        startJob(context, ji);
        startDummyJob(context);
    }

    public static void startPostUpdateJob(Context context) {
        JobInfo ji = createNetworkJob(context, UpdateService.JOB_POST_UPDATE).build();
        startJob(context, ji);
        startDummyJob(context);
    }

    public static void startPeriodicQueryUpdateJob(Context context) {
        JobInfo ji = createNetworkJob(context, UpdateService.JOB_AUTO_QUERY_UPDATES)
                .setPeriodic(Config.AUTO_CHECK_DURATION).build();
        startJob(context, ji);
    }

    public static void startRemindUpdateToUserJob(Context context) {
        JobInfo ji = createNetworkJob(context, UpdateService.JOB_REMIND_USER)
                .setMinimumLatency(Config.REMIND_USER_DURATION).setPersisted(true).build();
        startJob(context, ji);
    }

    public static void startAutoDownloadUpdateJob(Context context) {
        JobInfo ji = createNetworkJob(context, UpdateService.JOB_AUTO_DOWNLOAD_UPDATE)
                .setMinimumLatency(Config.AUTO_DOWNLOAD_DURATION).setPersisted(true).build();
        startJob(context, ji);
    }

    public static void startAutoInstallUpdateJob(Context context) {
        JobInfo ji = createNetworkJob(context, UpdateService.JOB_AUTO_INSTALL_UPDATE)
                .setMinimumLatency(Config.AUTO_INSTALL_DURATION).setPersisted(true).build();
        startJob(context, ji);
    }

    public static void cancelAllJobs(Context context) {
        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancelAll();
    }

    public static void cancelJob(Context context, int jobId) {
        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(jobId);
    }

    public static void startUpdateMonitor(Context context, String reason) {
        Intent i = new Intent(context, UpdateMonitorService.class);
        i.putExtra("action", reason);
        context.startService(i);
    }

    public static void stopUpdateMonitor(Context context) {
        Intent i = new Intent(context, UpdateMonitorService.class);
        context.stopService(i);
    }

    public static void startDownloadService(Context context) {
        Intent i = new Intent(context, UpdateDownloader.class);
        context.startService(i);
    }

    public static void stopDownloadService(Context context) {
        Intent i = new Intent(context, UpdateDownloader.class);
        context.stopService(i);
    }

}

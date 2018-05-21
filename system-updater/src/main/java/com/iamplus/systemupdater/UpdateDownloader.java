package com.iamplus.systemupdater;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;

public class UpdateDownloader extends Service {
    private static String TAG = "FOTA UpdateDownloader";
    private int mStartId;
    private UpdateManager mUpdateManager;
    private UpdateInfo mUpdateInfo;
    private boolean mIsRegistered;
    private DownloaderTask mDownloaderTask;
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private PendingIntent mPendingIntent;
    private PowerManager.WakeLock mWakeLock;
    private BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startDownload();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mUpdateManager = UpdateManager.getInstance(this);

        Intent i = new Intent(this, UpdateDownloader.class);
        mPendingIntent = PendingIntent.getService(this, 0, i, 0);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mStartId != 0) {
            log("already started");
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        mStartId = startId;

        mUpdateInfo = mUpdateManager.getUpdateInfo();
        if (mUpdateInfo == null) {
            log("No update info");
            finish();
        } else {
            startDownload();
        }

        return START_NOT_STICKY;
    }

    private void finish() {
        stopSelf(mStartId);
        mStartId = 0;
    }

    private boolean isNetworkAvailable() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }
        if (!networkInfo.isConnectedOrConnecting()) return false;

        if (mUpdateInfo.isWifiOnly) {
            return networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return true;
    }

    private void startDownload() {
        if (!isNetworkAvailable()) {
            registerConnectivity();
            return;
        }

        unregisterConnectivity();
        cancelAlarm();

        mDownloaderTask = new DownloaderTask(mUpdateManager);
        mDownloaderTask.execute();
    }

    private void cancelTask() {
        if (mDownloaderTask == null) return;
        if (mDownloaderTask.getStatus() == AsyncTask.Status.RUNNING) {
            log("Async task is running ..");
            mDownloaderTask.cancel(true);
            mDownloaderTask = null;
        }
    }

    private void registerConnectivity() {
        if (mIsRegistered) return;
        IntentFilter filters = new IntentFilter();
        filters.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filters);
        mIsRegistered = true;
    }

    private void unregisterConnectivity() {
        if (mIsRegistered) {
            unregisterReceiver(mNetworkReceiver);
            mIsRegistered = false;
        }
    }

    private void setAlarm() {
        log("Setting alarm");
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + Config.DOWNLOAD_RETRY_DURATION, mPendingIntent);
    }

    private void cancelAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(mPendingIntent);
    }

    private void acquireWakelock() {
        log("Acquiring wake lock..");
        mWakeLock.acquire();
    }

    private void releaseWakelock() {
        if (mWakeLock.isHeld()) {
            log("releasing wake lock..");
            mWakeLock.release();
        }
    }

    public void log(String msg) {
        Log.d(TAG, msg);
    }

    public void log_exception(String message, Exception e) {
        Log.e(TAG, message, e);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        unregisterConnectivity();
        releaseWakelock();
        cancelTask();

        mUpdateManager.save();
        mUpdateManager = null;
    }

    private class DownloaderTask extends AsyncTask<Void, Integer, Boolean> {

        private int mDownloadSize;
        private int mDownloadPercentage;
        private UpdateManager mUpdateManager;

        public DownloaderTask(UpdateManager updateManager) {
            mUpdateManager = updateManager;
        }

        public void onPreExecute() {
            log("DownloadUpdateTask onPreExecute");
            mUpdateManager.onDownloadStarted();
        }

        public Boolean doInBackground(Void... args) {
            log("DownloaderTask");

            acquireWakelock();

            try {
                if (Mock.MOCK) {
                    Mock.saveUpdateFile(mContext, mUpdateInfo.getFile(mContext));
                } else {
                    log("Saving URL...");
                    saveUrl(mUpdateInfo.getFile(mContext), mUpdateInfo.url, mUpdateInfo.size);
                }
            } catch (MalformedURLException e) {
                log_exception("Wrong url format", e);
                return true;
            } catch (IOException e) {
                log_exception("IOException while downloading", e);
                return false;
            }
            log("File downloaded successfully");
            return true;
        }

        private void calculateSizes() {
            mDownloadSize = (int) mUpdateInfo.getFile(mContext).length();
            mDownloadPercentage = (int) ((mDownloadSize / (float) mUpdateInfo.size) * 100);
        }

        public void saveUrl(File filename, String urlString, int totalSize) throws IOException,
                MalformedURLException {
            RandomAccessFile raf = null;
            BufferedInputStream in = null;
            InputStream is = null;

            calculateSizes();

            if (!urlString.equals(mUpdateInfo.url)  || !mUpdateInfo.getFilePath(mContext).contains(mUpdateInfo.to_version)) {
                //Url has changed Delete any files already downloaded, start fresh
                log("Download Url changed, delete any existing files and start fresh download");
                mUpdateManager.removeUpdateFile();
                calculateSizes();
            }

            if (mDownloadSize == mUpdateInfo.size) {
                log("Already file downloaded, no need to download again");
                return;
            }

            if (mDownloadSize > mUpdateInfo.size) {
                log("Download size > File size, so Delete file and download again");
                mUpdateManager.removeUpdateFile();
                calculateSizes();
            }

            try {
                HttpURLConnection connection;
                URL url = new URL(urlString);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Range", "bytes=" + mDownloadSize + "-");

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "responseCode = " + responseCode + " for url " + url);

                if (responseCode == 200 || responseCode == 201 || responseCode == 202 ||
                        responseCode == 206) {
                    is = connection.getInputStream();
                } else {
                    throw new IOException("Invalid ResponseCode");
                }

                String newETag = connection.getHeaderField("ETag");
                if (newETag != null && !newETag.equals(mUpdateInfo.mETag)) {

                    String currentETag = mUpdateInfo.mETag;
                    mUpdateInfo.mETag = newETag;
                    mUpdateManager.save();

                    if (!TextUtils.isEmpty(currentETag)) {
                        mUpdateManager.removeUpdateFile();
                        throw new IOException("ETag Modified, Re-try");
                    }
                }
                in = new BufferedInputStream(is);
                raf = new RandomAccessFile(filename, "rw");
                raf.seek(mDownloadSize);

                final byte data[] = new byte[4096];
                int count;
                while ((count = in.read(data, 0, 4096)) != -1) {
                    raf.write(data, 0, count);
                    mDownloadSize += count;
                    int currentPercentage = (int) ((mDownloadSize / (float) totalSize) * 100);
                    if (currentPercentage != mDownloadPercentage) {
                        mDownloadPercentage = currentPercentage;
                        if (mDownloadPercentage % 10 == 0) {
                            log("Download percentage " + mDownloadPercentage);
                        }
                        publishProgress(mDownloadPercentage);
                    }

                    if (isCancelled()) throw new IOException("Job cancelled during download");
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (raf != null) {
                    raf.close();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mUpdateManager.setDownloadPercentage(progress[0], mDownloadSize);
        }

        private void processResult(boolean result) {
            if (result) {
                mUpdateManager.onDownloadComplete();
            } else {
                log("DownloaderTask failed");
                boolean retry = mUpdateManager.onDownloadFailed();
                if (retry) {
                    setAlarm();
                }
            }
        }

        @Override
        public void onPostExecute(Boolean result) {
            processResult(result);
            releaseWakelock();
            finish();
        }

        @Override
        public void onCancelled(Boolean result) {
            log("DownloaderTask cancelled.");
            mUpdateManager.onDownloadCancelled();
        }
    }
}

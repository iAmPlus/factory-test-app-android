package com.iamplus.systemupdater.task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import com.iamplus.systemupdater.Config;
import com.iamplus.systemupdater.Mock;
import com.iamplus.systemupdater.UpdateInfo;
import com.iamplus.systemupdater.Utils;

public class DownloadUpdateTask extends BaseTask {

    private int mDownloadSize = 0;

    public void onPreExecute() {
        log("DownloadUpdateTask onPreExecute");
        mUpdateManager.onDownloadStarted();
    }

    public boolean doInBackground() {
        log("DownloadUpdateTask");
        UpdateInfo ui = mUpdateManager.getUpdateInfo();
        log("UpdateInfo " + ui + " file " + ui.getFile(getContext()));
        String url = ui.url;

        try {
            if (Mock.MOCK) {
                Mock.saveUpdateFile(getContext(), ui.getFile(getContext()));
            } else {
                saveUrl(ui.getFile(getContext()), url, ui.size);
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

    public void saveUrl(File filename, String urlString, int totalSize) throws IOException,
            MalformedURLException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            in = new BufferedInputStream(new URL(urlString).openStream());
            fout = new FileOutputStream(filename);
            mDownloadSize = 0;

            final byte data[] = new byte[4096];
            int count;
            while ((count = in.read(data, 0, 4096)) != -1) {
                mDownloadSize += count;
                publishProgress((int) ((mDownloadSize / (float) totalSize) * 100));
                fout.write(data, 0, count);

                if (isCancelled()) throw new IOException("Job cancelled during download");
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (fout != null) {
                fout.close();
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        mUpdateManager.setDownloadPercentage(progress[0], mDownloadSize);
    }

    @Override
    public boolean onPostUi(boolean result) {
        if (result) {
            mUpdateManager.onDownloadComplete();
        } else {
            log("DownloadUpdateTask failed");
            return !mUpdateManager.onDownloadFailed();
        }
        return result;
    }

    @Override
    public void onCancelled(Boolean result) {
        log("DownloadUpdateTask cancelled.");
        mUpdateManager.onDownloadCancelled();
    }

}
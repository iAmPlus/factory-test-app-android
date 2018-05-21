package com.iamplus.systemupdater.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.DropBoxManager;

import com.iamplus.systemupdater.Config;
import com.iamplus.systemupdater.UpdateInfo;
import com.iamplus.systemupdater.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class PostUpdateTask extends BaseTask {

    private static final String DROPBOX_LOG = "SYSTEM_RECOVERY_LOG";
    private static final int MAX_LOG_SIZE = 30 * 1024;

    public boolean doInBackground() {
        log("PostUpdateTask");
        if (Config.getBaseUrl(getContext()) == null) {
            log("Update result url is null. Will retry ..");
            return false;
        }
        boolean result = getUpdateStatus(Config.INSTALL_SUCCESS_FILE);
        collectLog();
        sendUpdate(result);
        mUpdateManager.removeUpdateFile();
        return result;
    }

    private void collectLog() {
        DropBoxManager dbm =
                (DropBoxManager) getContext().getSystemService(Context.DROPBOX_SERVICE);
        if (dbm == null) {
            mUpdateManager.appendLog("Unable to get DropBoxManager");
            return;
        }

        SharedPreferences pref = Config.getSharedPreference(getContext());
        long updateTime = pref.getLong(Config.PREF_UPDATE_TIME, 0);
        log("time = " + updateTime + " current time " + System.currentTimeMillis());

        DropBoxManager.Entry entry = dbm.getNextEntry(DROPBOX_LOG, updateTime);

        if (entry == null) {
            mUpdateManager.appendLog("No such entry " + DROPBOX_LOG);
            return;
        }

        String text = entry.getText(MAX_LOG_SIZE);
        if (text == null) {
            mUpdateManager.appendLog("Unable to get text" + entry.getTag());
            return;
        }
        mUpdateManager.appendLog(text);
        entry.close();
    }

    private boolean getUpdateStatus(String install_status_file) {
        BufferedReader br = null;
        File file = new File(install_status_file);
        log("Reading " + install_status_file);
        try {
            br = new BufferedReader(new FileReader(file));
            String install_file = br.readLine();
            log("install file = " + install_file);

            do {
                String success = br.readLine();
                log("success = " + success);

                if (success != null && success.equals("1")) {
                    log("getUpdateStatus: Success!");
                    return true;
                }
            } while (br == null);

            log("getUpdateStatus: returning failure ..");
            return false;
        } catch (IOException e) {
            log_exception("IOException while reading update status ", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        log("returning failure..");
        return false;
    }

    public void sendUpdate(boolean result) {
        log("Sending update " + result);
        if (!result) {
            mUpdateManager.dumpCacheContents();
        }
        try {
            UpdateInfo ui = mUpdateManager.getUpdateInfo();
            SharedPreferences pref = Config.getSharedPreference(getContext());
            String oldVersion = pref.getString(Config.PREF_OLD_VERSION, "Unknown");
            String toVersion = result ? mUpdateManager.getVersion() : ui.to_version;
            JSONObject j = new JSONObject();
            j.put("registration_id", mUpdateManager.getRegistrationId());
            j.put("from_version", oldVersion);
            j.put("to_version", toVersion);
            j.put("package_name", ui.url);
            j.put("success", result);
            j.put("error_code", 0);
            j.put("log", mUpdateManager.getLog());
            String url = Config.getUpdateResultUrl(getContext());
            Utils.makeJsonHttpPostRequest(url, j, null);
        } catch (JSONException e) {
            log_exception("JSONException whle reportDeviceInfo", e);
        } catch (IOException e) {
            log_exception("IOException while reportDeviceInfo", e);
        }
        log("finished sending update");
    }

    @Override
    protected boolean onPostUi(boolean result) {
        if (Config.getBaseUrl(getContext()) != null) {
            mUpdateManager.onUpdateComplete(result);
        } else {
            log("No base url available.. No calling onUpdateComplete");
        }
        return true;
    }
}

package com.iamplus.systemupdater;

import android.content.Context;
import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class UpdateInfo {
    public static final String VM_UPDATE_FOLDER = "/VMUPGRADE";
    public String url;
    public String from_version;
    public String to_version;
    public String releaseNotes;
    public int size;
    public boolean isCritical;
    public String md5;
    public boolean isWifiOnly;
    public String mETag;

    public void loadFromJson(JSONObject j) throws JSONException {
        url = j.optString("download_url", "");
        from_version = j.optString("from_version", "");
        to_version = j.optString("to_version", "");
        releaseNotes = j.optString("release_notes", "");
        size = j.optInt("file_size", -1);
        isCritical = j.optBoolean("critical", false);
        isWifiOnly = j.optBoolean("wifi_only", false);
        md5 = j.optString("md5", "");
        mETag = j.optString("ETag", "");
    }

    public JSONObject saveToJson() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("download_url", url);
        j.put("from_version", from_version);
        j.put("to_version", to_version);
        j.put("release_notes", releaseNotes);
        j.put("file_size", size);
        j.put("critical", isCritical);
        j.put("md5", md5);
        j.put("wifi_only", isWifiOnly);
        j.put("ETag", mETag);
        return j;
    }

    public String getFilePath(Context context) {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + VM_UPDATE_FOLDER + "/" + to_version;
    }

    public File getFile(Context context) {
        File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + VM_UPDATE_FOLDER);
        if (!path.exists()) {
            path.mkdir();
        }
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + VM_UPDATE_FOLDER + "/" + to_version);
    }

    public boolean isFileExists(Context context) {
        return getFile(context).exists();
    }

    public String toString() {
        return "Url = " + url + " From Version " + from_version + " To Version " + to_version +
                " Release Notes " + releaseNotes + " File Size " + size + " isCritical " +
                isCritical + " isWifiOnly " + isWifiOnly + " md5 " + md5 + " mETag " + mETag;
    }
}


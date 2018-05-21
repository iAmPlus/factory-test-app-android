package com.iamplus.systemupdater;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class UpdateLog {
    private static final String TAG = "FOTA UpdateLog";
    private StringBuilder mLog = new StringBuilder();
    private Context mContext;
    private String mFile;

    public UpdateLog(Context context, String file) {
        mContext = context;
        mFile = file;
        restoreLog();
    }

    private static void log(String message) {
        Log.d(TAG, message);
    }

    public void append(String message) {
        log(message);
        appendLog(message);
        save();
    }

    public void appendFileToLog(File file) {
        append(Utils.readFile(file));
    }

    public void clear() {
        clearLog();
        save();
    }

    private void restoreLog() {
        clearLog();
        appendLog(Utils.readFile(mContext, mFile));
    }

    private void save() {
        Utils.writeToFile(mContext, mFile, toString());
    }

    private void clearLog() {
        mLog.setLength(0);
    }

    private void appendLog(String message) {
        mLog.append(message);
        mLog.append(System.getProperty("line.separator"));
    }

    public String toString() {
        return mLog.toString();
    }
}


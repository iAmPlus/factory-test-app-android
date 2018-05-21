package com.iamplus.systemupdater.task;

import com.iamplus.systemupdater.UpdateInfo;
import com.iamplus.systemupdater.UpdateManager.ErrorCodes;
import com.iamplus.systemupdater.Utils;

import java.io.File;
import java.io.IOException;

public class VerifyUpdateTask extends BaseTask {
    private ErrorCodes mError = ErrorCodes.NO_ERROR;

    public boolean doInBackground() {
        log("VerifyUpdateTask");
        UpdateInfo ui = mUpdateManager.getUpdateInfo();
        File f = ui.getFile(getContext());
        if (!f.exists()) {
            log("Update file doesnot exist");
            mError = ErrorCodes.UPDATE_FILE_MISSING;
            return false;
        }
        long size = f.length();
        long expected_size = ui.size;
        log("file size " + size + " expected size " + expected_size);
        if (size != expected_size) {
            log("size doesnot match");
            mError = ErrorCodes.UPDATE_CORRUPT;
            return false;
        }

        try {
            String md5sum = Utils.computeMD5sum(f);
            log("Md5sum " + md5sum + " expected md5sum " + ui.md5);
            if (!md5sum.equals(ui.md5)) {
                log("md5sum didn't match !!");
                mError = ErrorCodes.UPDATE_CORRUPT;
                return false;
            }
        } catch (IOException e) {
            log_exception("IOException while computing md5sum", e);
            return false;
        }

        /*ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(f);
        } catch (IOException e) {
            mError = ErrorCodes.UPDATE_CORRUPT;
            return false;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                }
            } catch (IOException e) {
            }
        }*/

        return true;
    }

    @Override
    protected boolean onPostUi(boolean result) {
        mUpdateManager.onVerifyComplete(result, mError);
        return true;
    }
}
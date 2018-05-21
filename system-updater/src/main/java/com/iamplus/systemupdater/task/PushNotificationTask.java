package com.iamplus.systemupdater.task;

import android.os.PersistableBundle;

import org.json.JSONException;
import org.json.JSONObject;

import com.iamplus.systemupdater.UpdateInfo;
import com.iamplus.systemupdater.JobUtils;

public class PushNotificationTask extends BaseTask {

    public boolean doInBackground() {
        log("PushNotificationTask");

        PersistableBundle b = getParams().getExtras();
        String action = b.getString("action");

        if ("report_device_info".equals(action)) {
            reportDeviceInfo();
        } else if ("update".equals(action)) {
            doNetworkUpdate();
        } else {
            log("Unknown action " + action);
        }
        return true;
    }

    private void reportDeviceInfo() {
        JobUtils.startPostDeviceDetailJob(getContext());
    }

    private void doNetworkUpdate() {
        try {
            PersistableBundle b = getParams().getExtras();
            UpdateInfo ui = new UpdateInfo();
            JSONObject j = new JSONObject(b.getString("iapn_raw_data"));
            ui.loadFromJson(j.getJSONObject("firmware_update"));
            log(" Update info  " + ui);
            mUpdateManager.doNetworkUpdate(ui);
        } catch (JSONException e) {
            log_exception("JSONException whle doNetworkUpdate", e);
        }
    }
}

package com.iamplus.systemupdater.task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import com.iamplus.systemupdater.Config;
import com.iamplus.systemupdater.UpdateInfo;
import com.iamplus.systemupdater.Utils;

public class PostDeviceDetailsTask extends BaseTask {

    public boolean doInBackground() {
        log("PostDeviceDetailsTask");
        if (Config.getBaseUrl(getContext()) == null) {
            log("Device info url is null .. Will retry..");
            return false;
        }
        try {
            String device_info = "Device version : " + mUpdateManager.getVersion();
            device_info += " IMEI : " + Utils.getImei(getContext());

            JSONObject j = new JSONObject();
            j.put("registration_id", mUpdateManager.getRegistrationId());
            j.put("device_info", device_info);
            String url = Config.getDeviceInfoUrl(getContext());
            Utils.makeJsonHttpPostRequest(url, j, null);
        } catch (JSONException e) {
            log_exception(" JSONException whle reportDeviceInfo", e);
        } catch (IOException e) {
            log_exception("IOException while reportDeviceInfo", e);
        }
        return true;
    }
}
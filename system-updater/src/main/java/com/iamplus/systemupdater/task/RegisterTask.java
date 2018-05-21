package com.iamplus.systemupdater.task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.iamplus.systemupdater.Config;
import com.iamplus.systemupdater.Utils;

public class RegisterTask extends BaseTask {

    private String mOldChannel;
    private String mOldUrl;
    private boolean mDoUnregister;

    @Override
    public void onPreExecute() {
        mOldChannel = getParams().getExtras().getString(Config.OLD_CHANNEL);
        mOldUrl = getParams().getExtras().getString(Config.OLD_BASE_URL);
        if (mOldChannel != null) {
            mDoUnregister = true;
        }
    }


    public boolean doInBackground() {
        log("registerCDM " + Config.getChannel(getContext()));
        if (mDoUnregister) {
            log("Unregister old channel " + mOldChannel);
            unregister(mOldChannel, mOldUrl);
        }

        return register();
    }

    private boolean register() {
        // TODO kaustubh
        return false;
//        IamplusCloudMessaging cdm = IamplusCloudMessaging.getInstance(getContext().getApplicationContext());
//        String imei = Utils.getImei(getContext());
//        if (imei.isEmpty()) {
//            log ("IMEI is Empty, retry after sometime");
//            return false;
//        }
//
//        if(Config.getBaseUrl(getContext()) == null) {
//            log("Register Url is empty. Will retry..");
//            return false;
//        }
//
//        try {
//            String senderId = Config.getSenderId(getContext());
//            String regId = cdm.register(senderId);
//            mUpdateManager.setRegistrationId(regId);
//            log("Registered with CDM " + regId + " senderId " + senderId);
//            JSONObject j = new JSONObject();
//            j.put("registration_id", regId);
//            j.put("version", Utils.getVersion());
//            j.put("imei", imei);
//            j.put("model", Build.getAneedaModel());
//            j.put("variant", Build.getAneedaVariant());
//            Utils.makeJsonHttpPostRequest(Config.getRegisterUrl(getContext()), j, null);
//        } catch(IOException e) {
//            log_exception("IOException while registering..", e);
//            return false;
//        } catch(JSONException e) {
//            log_exception("JSONException while registering ..", e);
//            return false;
//        }
//        return true;
    }

    private void unregister(String channel, String url) {
//        if(url == null) {
//            log("No url ..");
//            return;
//        }
//        IamplusCloudMessaging cdm = IamplusCloudMessaging.getInstance(getContext().getApplicationContext());
//        try {
//            String regId = mUpdateManager.getRegistrationId();
//            String senderId = Config.getSenderId(channel);
//            log("unregistering from CDM senderId " + senderId);
//            cdm.unregister();
//            if(regId == null) {
//                log("No registration id to unregister");
//                return;
//            }
//            log("Unregistered with CDM " + regId);
//            Map<String,Object> params = new HashMap<String,Object>();
//            params.put("registration_id", regId);
//            Utils.makeHttpDeleteWithParams(Config.getRegisterUrl(url), params);
//        } catch(IOException e) {
//            log_exception("IOException while unregistering..", e);
//        }
    }
}
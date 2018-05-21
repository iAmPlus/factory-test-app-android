package com.iamplus.systemupdater;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;


public class Config {
    public static final long MIN = 60 * 1000;
    public static final long HRS = 60 * MIN;
    public static final long DAY = 24 * HRS;
    public static final String PRODUCTION = "production";
    public static final String QA = "qa";
    public static final String DEV = "dev";
    public static final String DEMO = "demo";
    public static final String STAGING = "staging";
    public static final String PRODUCTION_SENDER_ID = "103";
    public static final String QA_SENDER_ID = "104";
    public static final String DEV_SENDER_ID = "105";
    public static final String SERVICE_NAME = "fota";
    public static final String UPDATER_VERSION = "1";
    public static final String USER_AGENT = "System Updater ver 1.0";
    public static final String SAVE_FILE = "saved_file";
    public static final String UPDATE_LOG = "update_log";
    public static final String PREF_FILE = "preferences";
    public static final String PREF_OLD_VERSION = "old_version";
    public static final String PREF_UPDATE_TIME = "update_time";
    public static final String PREF_IMEI = "imei";
    public static final String CHANNEL = "channel";
    public static final String LAST_REQUEST_TIME = "last_request_time";
    public static final String LAST_UPDATE_TIME = "last_update_time";
    public static final String OLD_CHANNEL = "old-channel";
    public static final String BASE_URL = "base-url";
    public static final String OLD_BASE_URL = "old-base-url";
    public static final String ACTION_CHANNEL_CHANGED = "channel_changed";
    public static final String URL_PATH_BASE = "/device_client";
    public static final String URL_PATH_REGISTER = URL_PATH_BASE + "/registrations";
    public static final String URL_PATH_QUERY = URL_PATH_BASE + "/firmware_updates";
    public static final String URL_PATH_UPDATE_RESULT = URL_PATH_BASE + "/update_results";
    public static final String URL_PATH_DEVICE_INFO = URL_PATH_BASE + "/device_infos";
    public static final String CACHE_LOCATION = "/cache/";
    public static final String INSTALL_SUCCESS_FILE = "/cache/recovery/last_install";
    public static final String INSTALL_LOG_FILE = "/cache/recovery/last_log";
    public static final String INSTALL_LOCATION = "/fota/";
    public static final long AUTO_CHECK_DURATION = 30 * MIN;
    public static final long REMIND_USER_DURATION = 1 * DAY;
    public static final long BACKOFF_DURATION = 10 * MIN;
    public static final long DOWNLOAD_RETRY_DURATION = 30 * MIN;
    public static final long AUTO_INSTALL_DURATION = 30 * MIN;
    public static final long AUTO_DOWNLOAD_DURATION = 1 * MIN;
    public static final int MINIMUM_BATTERY_PERCENTAGE = 20;
    public static final int MAX_DOWNLOAD_RETRY_COUNT = 5;
    private static final String TAG = "FOTA Config";
    private static String AUTO_UPDATE_CHECK_CONFIG = "autoupdatecheck";
    private static Boolean DEFAULT_AUTO_UPDATE_CHECK = false;

    public static String getChannel(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        return preferences.getString(Config.CHANNEL, BuildConfig.FOTA_SERVER_CHANNEL);
    }

    public static void setChannel(Context context, String channel) {
        Log.d(TAG, "setChannel: "+channel);
        SharedPreferences preferences = getSharedPreference(context);
        preferences.edit().putString(Config.CHANNEL, channel).commit();
    }

    public static void setBaseUrl(Context context, String baseUrl) {
        baseUrl = baseUrl.replaceAll("/$", ""); // Remove trailing slash
        SharedPreferences preferences = getSharedPreference(context);
        preferences.edit().putString(Config.BASE_URL, baseUrl).commit();
    }

    public static SharedPreferences getSharedPreference(Context context) {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public static String getBaseUrl(Context context) {
        SharedPreferences preferences = getSharedPreference(context);
        return preferences.getString(Config.BASE_URL, getDefaultBaseUrl(context));
    }
    public static void setAutoUpdateConfig(Context context, Boolean autoUpdateConfig) {
        SharedPreferences preferences = getSharedPreference(context);
        preferences.edit().putBoolean(AUTO_UPDATE_CHECK_CONFIG, DEFAULT_AUTO_UPDATE_CHECK).commit();
    }

    public static Boolean getAutoUpdateConfig(Context context) {
        SharedPreferences preferences = getSharedPreference(context);
        return preferences.getBoolean(AUTO_UPDATE_CHECK_CONFIG, DEFAULT_AUTO_UPDATE_CHECK);
    }

    private static String getDefaultBaseUrl(Context context) {
        String channel = getChannel(context);
        if (TextUtils.isEmpty(channel)) return null;
        else if (QA.equals(channel)) return "https://fota-qa.iamplus.com";
        else if (DEV.equals(channel)) return "https://fota-dev.iamplus.com";
        return "https://fota.iamplus.com";
    }

    public static String getSenderId(Context context) {
        String channel = getChannel(context);
        return getSenderId(channel);
    }

    public static String getSenderId(String channel) {
        if (PRODUCTION.equalsIgnoreCase(channel)) {
            return PRODUCTION_SENDER_ID;
        } else if (QA.equalsIgnoreCase(channel)) {
            return QA_SENDER_ID;
        } else if (DEV.equalsIgnoreCase(channel)) {
            return DEV_SENDER_ID;
        } else {
            Log.e(TAG, "Unknown channel " + channel);
            return PRODUCTION_SENDER_ID;
        }
    }

    public static String getRegisterUrl(Context context) {
        String baseUrl = getBaseUrl(context);
        if (baseUrl == null) return null;
        return baseUrl + URL_PATH_REGISTER;
    }

    public static String getRegisterUrl(String baseUrl) {
        return baseUrl + URL_PATH_REGISTER;
    }

    public static String getQueryUrl(Context context) {
        String baseUrl = getBaseUrl(context);
        if (baseUrl == null) return null;
        return baseUrl + URL_PATH_QUERY;
    }

    public static String getUpdateResultUrl(Context context) {
        String baseUrl = getBaseUrl(context);
        if (baseUrl == null) return null;
        return baseUrl + URL_PATH_UPDATE_RESULT;
    }

    public static String getDeviceInfoUrl(Context context) {
        String baseUrl = getBaseUrl(context);
        if (baseUrl == null) return null;
        return baseUrl + URL_PATH_DEVICE_INFO;
    }

    public static String getInstallLocation(Context context) {
        if (Utils.isEmulator()) {
            return context.getFilesDir().getAbsolutePath();
        } else {
            return INSTALL_LOCATION;
        }
    }
}


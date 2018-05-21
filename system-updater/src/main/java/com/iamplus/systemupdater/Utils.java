package com.iamplus.systemupdater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Map;

public class Utils {
    private final static boolean DEBUG = true;
    private final static boolean VERBOSE = false;
    private final static String TAG = "FOTA Utils";

    private static final String PROPERTY_FIRMWARE_VERSION = "ro.aneeda.version";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String DELETE = "DELETE";


    private static String getProperty(String property) {
        // TODO kaustubh
        return null;
//        return SystemProperties.get(property, "Unknown");
    }


    public static String getBuildType() {
        String[] split = Build.PRODUCT.split("_");
        if (split.length > 1) {
            return split[1];
        }
        return null;
    }

    public static boolean isEmulator() {
        return "goldfish".equals(Build.HARDWARE);
    }

    public static String getImei(Context context) {
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();

        SharedPreferences pref = Config.getSharedPreference(context);
        if (imei != null) {
            Log.d(TAG, "IMEI: " + imei);
            pref.edit().putString(Config.PREF_IMEI, imei).commit();
        } else {
            imei = pref.getString(Config.PREF_IMEI, "");
            Log.d(TAG, "IMEI from pref: " + imei);
        }
        return imei;
    }

    public static String makeJsonHttpPostRequest(String url, JSONObject jsonObj, String auth) throws
            IOException {
        return makeHttpRequest(url, POST, jsonObj.toString(), auth);
    }

    public static String makeHttpPostWithParams(String url, Map<String, Object> params,
            String auth) throws IOException {
        return makeHttpWithParams(url, POST, params, auth);
    }

    public static String makeHttpGet(String url) throws IOException {
        return makeHttpGetWithParams(url, null);
    }

    public static String makeHttpGetWithParams(String url, Map<String, Object> params) throws
            IOException {
        return makeHttpWithParams(url, GET, params, null);
    }

    public static String makeHttpDeleteWithParams(String url, Map<String, Object> params) throws
            IOException {
        return makeHttpWithParams(url, DELETE, params, null);
    }

    public static String updateUrlWithParams(String url, Map<String, Object> params) {
        if (VERBOSE) Log.v(TAG, "Number of params = " + Integer.toString(params.size()));
        StringBuilder data = new StringBuilder();
        try {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (data.length() != 0) data.append('&');
                data.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                data.append('=');
                data.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException while encoding params", e);
        }
        if (!url.endsWith("?")) {
            url += "?";
        }
        url += data.toString();
        return url;
    }

    public static String makeHttpWithParams(String url, String method, Map<String, Object> params,
            String auth) throws IOException {
        if (VERBOSE) Log.v(TAG, url);
        if (params != null && params.size() > 0) {
            url = updateUrlWithParams(url, params);
        }
        return makeHttpRequest(url, method, null, auth);
    }

    public static String makeHttpRequest(String url, String method, String postData,
            String auth) throws IOException {
        URL u = new URL(url);
        HttpURLConnection connection;

        if (DEBUG) Log.d(TAG, "Making http request to " + url + " method " + method);

        connection = (HttpURLConnection) u.openConnection();

        if (auth != null) {
            if (VERBOSE) Log.d(TAG, " authToken " + auth);
            connection.setRequestProperty("Authorization", auth);
        }

        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-type", "application/json");

        if (postData != null) {
            if (VERBOSE) Log.d(TAG, " post Data " + postData);
            connection.setRequestProperty("Content-Length",
                    Integer.toString(postData.getBytes().length));
            connection.setDoOutput(true);
            connection.setDoInput(true);

            OutputStream out = connection.getOutputStream();
            out.write(postData.getBytes());
            out.flush();
            out.close();
        }

        InputStream is;
        int responseCode = connection.getResponseCode();
        if (DEBUG) Log.d(TAG, "responseCode = " + responseCode);
        if (responseCode == 200 || responseCode == 201 || responseCode == 202) {
            is = connection.getInputStream();
        } else {
            is = connection.getErrorStream();
        }

        if (is == null) {
            if (VERBOSE) Log.d(TAG, "input stream is null");
            return "";
        }

        // Get Response
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }

        if (VERBOSE) Log.d(TAG, "input stream = " + total.toString());

        return total.toString().trim();
    }
    public static void sendNotification(Context context, String message, PendingIntent i) {
        sendNotification(context, context.getResources().getString(R.string.app_name), message, i);
    }

    public static void sendNotification(Context context, String title, String message, PendingIntent i) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = new Notification.Builder(context)
                .setContentTitle((title == null) ? context.getResources().getString(R.string.app_name) : title)
                .setContentText(message).setSmallIcon(R.mipmap.ic_launcher)
                .setDefaults(Notification.DEFAULT_SOUND).setContentIntent(i).build();
        nm.notify(0, n);
    }

    public static void sendProgressNotification(Context context, String title, double percentage) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = new NotificationCompat.Builder(context)
                .setContentTitle((title == null) ? context.getResources().getString(R.string.app_name) : title)
                .setContentText(String.valueOf((int)percentage) + "%").setSmallIcon(R.mipmap.ic_launcher)
                .setProgress(100, (int)percentage, false)
                .setOngoing(percentage < 99.99)
                .build();
        nm.notify(0, n);
    }

    public static void cancelNotifications(Context context) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    public static String computeMD5sum(File file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] dataBytes = new byte[8192];
            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            ;
            byte[] mdbytes = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            fis.close();
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "MD5 Algorith not found", e);
            return "";
        }
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static boolean isOnlineWiFi(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI);
    }

    public static String readFromAssets(Context context, String file) {
        try {
            return readFile(context.getAssets().open(file));
        } catch (IOException e) {
            Log.e(TAG, "IOException while reading assets " + file);
            return "";
        }
    }

    public static String readFile(File file) {
        try {
            return readFile(new FileInputStream(file));
        } catch (IOException e) {
            Log.e(TAG, "IO Exception while reading " + file);
            return "";
        }
    }

    public static String readFile(Context context, String file) {
        try {
            File f = new File(context.getFilesDir(), file);
            return readFile(new FileInputStream(f));
        } catch (IOException e) {
            Log.e(TAG, "IO Exception while reading " + file);
            return "";
        }
    }

    public static String readFile(InputStream is) {
        Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (IOException e) {
            return "No file.";
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
            }
        }
    }

    public static JSONObject readJsonFromFile(Context context, String file) {
        String data = readFile(context, file);
        JSONObject j = new JSONObject();
        if (data == null || data.isEmpty()) {
            return j;
        }
        try {
            return new JSONObject(data);
        } catch (JSONException e) {
            Log.e(TAG, "Unable to turn to json " + data);
            return j;
        }
    }

    public static void writeJSONtoFile(Context context, String file, JSONObject j) {
        String data = j.toString();
        writeToFile(context, file, data);
    }

    public static void writeToFile(Context context, String file, String data) {
        File f = new File(context.getFilesDir(), file);
        try {
            FileOutputStream fos = new FileOutputStream(f);
            writeToFile(fos, data);
        } catch (IOException e) {
            Log.e(TAG, "Unable to save to file " + file);
        }
    }

    public static void writeToFile(FileOutputStream fos, String data) {
        PrintStream ps = new PrintStream(fos);
        ps.print(data);
        ps.close();
    }

    public static int getBatteryLevel(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);
        return batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
    }

    public static void updateTextView(TextView textView, int id) {
        textView.setVisibility(View.VISIBLE);
        textView.setText(id);
    }

    public static void updateTextView(TextView textView, String text) {
        textView.setVisibility(View.VISIBLE);
        textView.setText(text);
    }

    public static void clearAndHideTextView(TextView textView) {
        textView.setText("");
        textView.setVisibility(View.GONE);
    }

    public static boolean isWifiAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            return true;
        }
        return false;
    }

    public static boolean isMobileNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobileNetwork =
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()) {
            return true;
        }
        return false;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static void commitBoolean(Context context, String key, boolean b) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Config.PREF_FILE, Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, b);
        editor.commit();
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Config.PREF_FILE, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(key, defValue);
    }

    public static boolean getBoolean(Context context, String key) {
        return getBoolean(context, key, false);
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " +
                units[digitGroups];
    }

    public boolean isRoaming(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobileNetwork =
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isRoaming()) {
            return true;
        }
        return false;
    }
}

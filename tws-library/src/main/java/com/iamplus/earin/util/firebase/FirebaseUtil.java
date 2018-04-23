package com.iamplus.earin.util.firebase;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.iamplus.earin.BuildConfig;
import com.iamplus.earin.communication.models.DeviceAddress;
import com.iamplus.earin.util.FirmwareVersion;
import com.iamplus.earin.util.bluetooth.BleBroadcastUtil;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FirebaseUtil {

    private static final String TAG = FirebaseUtil.class.getSimpleName();
    private static FirebaseUtil instance;

    private DatabaseReference mDatabaseReference;

    public static FirebaseUtil getInstance() {
        if (instance == null) {
            instance = new FirebaseUtil();
        }
        return instance;
    }

    private FirebaseUtil() {
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
    }

    public void sendLog(final Context context, String data, String version, final FirebaseLogListener logListener) {
        if (BleBroadcastUtil.getInstance(context).isConnected() && DeviceAddress.getInstance().getMasterAddress() != null) {

            String address = DeviceAddress.getInstance().getMasterAddress();

            Calendar currentDate = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            DatabaseReference root = mDatabaseReference.child(address);

            DatabaseReference newEntry = root.child(dateFormat.format(currentDate.getTime()));

            newEntry.child("appVersion").setValue(BuildConfig.VERSION_NAME);
            newEntry.child("model").setValue(Build.MANUFACTURER + " " + Build.MODEL);
            newEntry.child("osName").setValue("Android");
            newEntry.child("osVersion").setValue(Build.VERSION.RELEASE + " " + Build.VERSION_CODES.class.getFields()[Build.VERSION.SDK_INT].getName());

            for (String part : data.split(",")) {
                String[] subParts = part.split(":");
                newEntry.child(mapDataString(subParts[0])).setValue(subParts[1]);
            }

            FirmwareVersion firmwareVersion = FirmwareVersion.fromString(version);
            newEntry.child("nxpVersion").setValue(firmwareVersion.getNxp().toString());
            newEntry.child("csrVersion").setValue(firmwareVersion.getCsr().toString());

            Log.w(TAG, "Log sent");
            if (logListener != null) {
                logListener.onSuccess(address);
            }

        } else {
            if (logListener != null) {
                logListener.onError("Address is null!");
            }
            Log.e(TAG, "Address is null!");
        }
    }

    private String mapDataString(String input) {
        switch (input) {
            case "R":
                return "role";
            case "POC":
                return "unitPower";
            case "POV":
                return "powerOn";
            case "LV":
                return "lastVoltage";
            case "PT":
                return "playTime";
            case "SC":
                return "stereo";
            case "EBCB":
                return "emptyBufferBt";
            case "EBCN":
                return "emptyBufferNfmi";
            case "MT":
                return "maxTemperature";
            case "LQ":
                return "btLinkQuality";
            case "RS":
                return "btRssiLevel";
        }
        return "Invalid";
    }

    public interface FirebaseLogListener {
        void onSuccess(String macAddress);

        void onError(String message);
    }
}

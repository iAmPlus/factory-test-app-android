package com.iamplus.systemupdater;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class UpdateMonitorService extends Service {
    private static final String UPDATE_LOCK_CHANGED = "android.os.UpdateLock.UPDATE_LOCK_CHANGED";
    private static final String NOW_IS_CONVENIENT = "nowisconvenient";
    private static String TAG = "FOTA UpdateMonitorService";
    private List<Integer> mBatteryStartIds = new ArrayList<Integer>();
    private List<Integer> mPhoneStateStartIds = new ArrayList<Integer>();
    private List<Integer> mUpdateLockStartIds = new ArrayList<Integer>();
    private boolean mRegisteredBattery = false;
    private boolean mRegisteredPhoneState = false;
    private boolean mRegisteredUpdateLock = false;
    private boolean mIsUpdateLockHeld = false;
    private UpdateManager mUpdateManager;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            log("Phone stat change received : " + state);
            if (state != TelephonyManager.CALL_STATE_IDLE) {
                log("PhoneState not IDLE, wait for Call release");
            } else {
                log("PhoneState IDLE, proceed with update");
                for (Integer i : mPhoneStateStartIds) {
                    stopSelf(i);
                }
                mPhoneStateStartIds.clear();
                mRegisteredPhoneState = false;

                mUpdateManager.doUpdate();
            }
        }
    };
    private TelephonyManager mTelephonyManager;
    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            log("battery level " + level);
            if (level >= Config.MINIMUM_BATTERY_PERCENTAGE) {
                log("battery condition satisfied.");
                for (Integer i : mBatteryStartIds) {
                    stopSelf(i);
                }
                mBatteryStartIds.clear();
                unregisterReceiver(mBatteryReceiver);
                mRegisteredBattery = false;

                mUpdateManager.onBatteryLevelOk();
            }
        }
    };
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isUpdateLockHeld(intent)) {
                log("UpdateLock released");
                for (Integer i : mUpdateLockStartIds) {
                    stopSelf(i);
                }
                mUpdateLockStartIds.clear();
                mRegisteredUpdateLock = false;
                mUpdateManager.doUpdate();
            } else {
                log("UpdateLock held");
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mUpdateManager = UpdateManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra("action");

        log("UpdateMonitorService onStartCommand" + intent);

        if ("battery".equals(action)) {
            log("monitor battery");
            mBatteryStartIds.add(startId);
            monitorBattery();
        } else if ("phoneState".equals(action)) {
            log("monitor phoneState");
            mPhoneStateStartIds.add(startId);
            monitorPhoneState();
        } else if ("updateLock".equals(action)) {
            log("monitor updateLock");
            mUpdateLockStartIds.add(startId);
            monitorUpdateLock();
        } else {
            log("unknow action " + action);
            log("stopping self..");
            stopSelf(startId);
        }

        return START_NOT_STICKY;
    }

    public void monitorBattery() {
        log("monitorBattery");
        if (mRegisteredBattery) {
            log("monitorBattery already registered");
            return;
        }
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryReceiver, filter);
        mRegisteredBattery = true;
    }

    public void monitorPhoneState() {
        log("monitorPhoneState");
        if (mRegisteredPhoneState) {
            log("monitorPhoneState already registered");
            return;
        }
        mTelephonyManager =
                (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private boolean isUpdateLockHeld(Intent i) {
        return !i.getBooleanExtra(NOW_IS_CONVENIENT, true);
    }

    public void monitorUpdateLock() {
        log("monitorUpdateLock");
        if (mRegisteredUpdateLock) {
            log("monitorUpdateLock already registered");
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_LOCK_CHANGED);
        Intent i = getBaseContext().registerReceiver(mUpdateReceiver, filter);
    }

    public void log(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mUpdateManager.save();
        mUpdateManager = null;
    }
}
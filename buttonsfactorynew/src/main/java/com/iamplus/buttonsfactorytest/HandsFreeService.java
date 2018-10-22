package com.iamplus.buttonsfactorytest;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaError;
import com.csr.gaiacontrol.Callback;
import com.csr.gaiacontrol.Controller;
import com.csr.gaiacontrol.SimpleCallback;
import com.iamplus.systemupdater.UpdateManager;

public class HandsFreeService extends Service {

    private static final String TAG ="HandsFreeService";
    public static final String ACTION_CHECK_FOR_UPDATES = "aneeda.CHECKS_FOR_UPDATES";

    private static final boolean SPEAK_INTRO = false;

    private final IBinder mBinder = new LocalBinder();
    private Controller mController;
    private boolean mRegisterUserAction;
    private boolean mVoiceBuffering;
    private Handler mHandler;
    private FirmwareUpdater mFirmwareUpdater;
    private BluetoothAdapter mBluetoothAdapter;
    public BluetoothProfile mBluetoothA2dp;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        HandsFreeService getService() {
            return HandsFreeService.this;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();

        mRegisterUserAction = true;

        mController = Controller.getInstance();
        mFirmwareUpdater = new FirmwareUpdater(getApplicationContext(), mController);
        mController.registerListener(mCallback);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mController.isConnected()) {
            mController.establishGAIAConnection();
        }

        //register for mic controller receiver

        IntentFilter connfilter = new IntentFilter();
        connfilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        connfilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        connfilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mConnectionReceiver,connfilter);

    }

    private BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            BluetoothDevice device = bundle.getParcelable(BluetoothDevice.EXTRA_DEVICE);
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
            log("device state::"+state);
            if (device != null && device.getName() != null && (device.getName().equalsIgnoreCase("Buttons with Omega")) ||
                    (device.getName().equalsIgnoreCase("Buttons")) ||
                    (device.getName().equalsIgnoreCase("Omega buttons"))) {
                if (state == BluetoothAdapter.STATE_CONNECTED) {
                    log("device connected");
                    mController.setConnectedDevice(device);
                    mController.purge();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mController.establishGAIAConnection();
                        }
                    }, 1000);
                }
                else if (state == BluetoothAdapter.STATE_DISCONNECTED){
                    log("device disconnected");
                    mController.disconnect();
                    mController.setConnectedDevice(null);
                    unRegisterEventNotifications();
                    mRegisterUserAction = true;
                    mControllerVersion = null;
                }
            }
        }
    };



    private String getModelNumberFromSKU(String sku) {
        return sku != null ? sku.substring(0, 6) : null;
    }

    private String mControllerVersion;
    private Callback mCallback = new SimpleCallback() {

        public String mModelNumber;

        @Override
        public void onConnected() {
            log("onConnected");
            if (mRegisterUserAction) {
                registerEventNotifications();
            } else {
                unRegisterEventNotifications();
            }
            mController.getSerialNumber();
        }

        @Override
        public void onDisconnected() {
            log("on Gaia disconnected");
            if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                log("updating UpdateManager.State state");
                mFirmwareUpdater.setState(UpdateManager.State.IDLE);
            }
        }

        @Override
        public void onGetSerialNumber(String sn) {
            mModelNumber = getModelNumberFromSKU(sn);
            mController.getAppVersion();
        }

        @Override
        public void onGetAppVersion(String version) {
            mControllerVersion = version + "-" + mModelNumber;
            mFirmwareUpdater.setVersion(mControllerVersion);
        }

        @Override
        public void onError(GaiaError error) {
            log("onError connecting to Gaia");
            mRegisterUserAction = true;
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(mConnectionReceiver);
        stopForeground(true);
        unRegisterEventNotifications();
        mController.unRegisterListener(mCallback);
        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        if(intent == null || intent.getAction() == null) {
            log("No intent to serve.");
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Handling intent action: " + action);
        if (ACTION_CHECK_FOR_UPDATES.equalsIgnoreCase(action)) {
            if (mController.isConnected() && mFirmwareUpdater != null) {
                if (mControllerVersion != null) {
                    mFirmwareUpdater.checkForUpdates();
                } else {
                    mController.getSerialNumber();
                }
            } else {
               mController.establishGAIAConnection();
            }
        }

    }

    private void unRegisterEventNotifications() {
        mRegisterUserAction = true;
        if (!mController.isConnected()) {
            return;
        }
        mController.cancelNotification(Gaia.EventId.USER_ACTION);
    }

    private void registerEventNotifications() {
        log("register for omega button events::"+mRegisterUserAction);
        if (!mController.isConnected()) {
            log("is device connected to controller::"+mController.isConnected());
            mRegisterUserAction = true;
            return;
        }
        mRegisterUserAction = false;
        mController.registerNotification(Gaia.EventId.USER_ACTION);
    }

    private static void log(String s) {
        Log.d(TAG, s);
    }
}

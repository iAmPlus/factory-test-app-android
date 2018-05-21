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
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaPacket;
import com.csr.gaiacontrol.Callback;
import com.csr.gaiacontrol.Controller;
import com.csr.gaiacontrol.Events;
import com.csr.gaiacontrol.SimpleCallback;
import com.iamplus.systemupdater.Config;
import com.iamplus.systemupdater.UpdateManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

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


    BroadcastReceiver mDeviecDiscveryReceiver = new BroadcastReceiver() {
        BluetoothDevice mBluetoothDevice = null;
        @Override
        public void onReceive(Context context, Intent intent) {


            if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                log("Discovery finished");
                if(null != mBluetoothDevice) {
                    log("Creating bond with device " + mBluetoothDevice.getName());
                    mBluetoothDevice.createBond();
                }
                return;
            }

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) {
                log("onReceive: bluetooth device is null");
                return;
            }

            if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                log("device is bonding " + device.getBondState());
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    mBluetoothAdapter.getProfileProxy(getApplicationContext(), mProfileListener, BluetoothProfile.A2DP);
                    return;
                }
            }


            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND) && device != null) {
                final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0);
                log("found device "+device.getName());
                log("previous state::"+previousState);
                log("bond state::"+device.getBondState());
                log("bluetooth class:: "+device.getBluetoothClass().getMajorDeviceClass());
                if (device.getName() != null && (device.getName().equalsIgnoreCase("A.I. Buttons") ||
                        device.getName().equalsIgnoreCase("Buttons with Omega"))) {
                    if(device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.UNCATEGORIZED){
                        log("ignore ble device");
                        return;
                    }
                    if (device.getBondState() == BluetoothDevice.BOND_NONE && previousState != BluetoothDevice.BOND_BONDED) {
                        mBluetoothDevice = device;
                        if(mBluetoothAdapter.isDiscovering())
                            mBluetoothAdapter.cancelDiscovery();
                    }
                }
            }
        }
    };

    private BluetoothDevice getFirstBondedDevice() {

        final Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        if (bondedDevices == null) {
            return null;
        }

        final Iterator<BluetoothDevice> itr = bondedDevices.iterator();
        return itr.hasNext() ? itr.next() : null;
    }

    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile
            .ServiceListener() {

        @Override
        public void onServiceConnected(int profile, BluetoothProfile bluetoothProfile) {
            final BluetoothDevice bluetoothDevice = getFirstBondedDevice();

            if (bluetoothDevice == null) {
                Log.e(TAG, "Failed to get bonded device");
                return;
            }

            toggledevice(bluetoothProfile,bluetoothDevice,"connect", profile);

        }

        @Override
        public void onServiceDisconnected(int i) {
            mBluetoothA2dp = null;
        }
    };

    private void toggledevice(BluetoothProfile bluetoothProfile, BluetoothDevice device, String
            action, int profile){

        try {
            log("toggledevice: "+action+" to speaker...");
            if(profile == BluetoothProfile.A2DP) {
                Method connect = BluetoothA2dp.class.getDeclaredMethod(action, BluetoothDevice
                        .class);
                connect.setAccessible(true);
                connect.invoke(bluetoothProfile, device);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void startButtonsDiscovery(){

        if(mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.startDiscovery();
    }

    private BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            BluetoothDevice device = bundle.getParcelable(BluetoothDevice.EXTRA_DEVICE);
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
            log("device state::"+state);
            if (device != null && device.getName() != null && device.getName().equalsIgnoreCase("A.I. Buttons") ||
                    device.getName().equalsIgnoreCase("Buttons with Omega")) {
                if (state == BluetoothAdapter.STATE_CONNECTED) {
                    log("device connected");
                    mController.setConnectedDevice(device);
                    mController.purge();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mController.establishGAIAConnection();
                        }
                    }, 2000);
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

        unregisterReceiver(mDeviecDiscveryReceiver);
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

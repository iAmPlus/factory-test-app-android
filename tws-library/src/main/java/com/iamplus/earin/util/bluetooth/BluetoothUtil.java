package com.iamplus.earin.util.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.Manager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class BluetoothUtil {

    private static final String TAG = BluetoothUtil.class.getSimpleName();

    private static BluetoothUtil instance;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothA2dp mBluetoothA2dp;

//    private String mExpectedDevice;
    private BluetoothDevice mCurrentDevice;

    private Semaphore mSwitchSemaphore;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice receivedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (action != null && receivedDevice != null && action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                BleBroadcastUtil bleBroadcastUtil = BleBroadcastUtil.getInstance(EarinApplication.getContext());
                String receivedDeviceAddress = receivedDevice.getAddress();
                Log.v(TAG, "New Device Connected: " + receivedDeviceAddress
                        + "\n bleBroadcastUtil is connected: " + bleBroadcastUtil.isConnected()
                        + "\n type: " + receivedDevice.getType()
                        + "\n received address: " + receivedDeviceAddress);
                
                if (!bleBroadcastUtil.isConnected()) {
                    Log.v(TAG, "No BLE devices connected -> try to connect to this one!");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        mCurrentDevice = receivedDevice;
                        if (mBluetoothHeadset == null || mBluetoothA2dp == null) {
                            getBluetoothHeadset(context);
                        }
                        Manager manager = Manager.getSharedManager();
                        manager.getCapCommunicationController().connectToDevice(receivedDevice);
                    }, 2000);
                } else {
                    new Thread(() -> {
                        Log.v(TAG, "HAS BLE devices connected -> try to acquire permit!");
                        try {
                            if (mSwitchSemaphore.tryAcquire(20, TimeUnit.SECONDS)) {
                                Log.v(TAG, "HAS BLE devices connected -> try to acquire permit -> acquired success");
                                mCurrentDevice = receivedDevice;
                                if (mBluetoothHeadset == null || mBluetoothA2dp == null) {
                                    getBluetoothHeadset(context);
                                }
                                Thread.sleep(1000);
                                Manager manager = Manager.getSharedManager();
                                manager.getCapCommunicationController().connectToDevice(receivedDevice);
                            } else {
                                throw new InterruptedException("Timed out!");
                            }
                        } catch (InterruptedException e) {
                            Log.e(TAG, "HAS BLE devices connected -> try to acquire permit -> Failed: " + e.getMessage());
                        }
                    }).start();
                }
            }
        }
    };

    private BluetoothProfile.ServiceListener mBluetoothHeadsetServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            if (i == BluetoothProfile.HEADSET) {
                Log.v(TAG, "Acquired proxy object is HEADSET");
                mBluetoothHeadset = (BluetoothHeadset) bluetoothProfile;
                // This assumes only one device is connected to bluetooth
                // which according to documentation is always true
                List<BluetoothDevice> connectedBluetoothDevices = mBluetoothHeadset.getConnectedDevices();
                if (connectedBluetoothDevices.size() > 0) {
                    Log.v(TAG, "Has connectedBluetoothDevices");
                    if (mCurrentDevice == null) {
                        Log.v(TAG, "We don't have a device to connect to -> set this one !");
                        mCurrentDevice = mBluetoothHeadset.getConnectedDevices().get(0);
                        Manager manager = Manager.getSharedManager();
                        manager.getCapCommunicationController().connectToDevice(mCurrentDevice);
                    }
                }
            } else if (i == BluetoothProfile.A2DP) {
                Log.v(TAG, "Acquired proxy object is A2DP");
                mBluetoothA2dp = (BluetoothA2dp) bluetoothProfile;

                if (mBluetoothA2dp.getConnectedDevices().size() > 0) {
                    Log.v(TAG, "Has connectedBluetoothDevices");
                    if (mCurrentDevice == null) {
                        Log.v(TAG, "We don't have a device to connect to -> set this one !");
                        mCurrentDevice = mBluetoothA2dp.getConnectedDevices().get(0);
                        Manager manager = Manager.getSharedManager();
                        manager.getCapCommunicationController().connectToDevice(mCurrentDevice);
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(int i) {
            if (i == BluetoothProfile.HEADSET)
                mBluetoothHeadset = null;

            if (i == BluetoothProfile.A2DP)
                mBluetoothA2dp = null;
        }
    };

    private BluetoothUtil(Context context) {
        mSwitchSemaphore = new Semaphore(0);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        context.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    public static BluetoothUtil getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothUtil(context);
        }
        return instance;
    }

    Semaphore getSwitchSemaphore() {
        return mSwitchSemaphore;
    }

    @SuppressLint("PrivateApi")
    private static ArrayList<Method> getConnectMethod() {
        Method connectHead, disconnectHead, connectA2dp, disconnectA2dp;
        ArrayList<Method> methods = new ArrayList<>();
        try {
            disconnectA2dp = BluetoothA2dp.class.getDeclaredMethod("disconnect", BluetoothDevice.class);
            disconnectHead = BluetoothHeadset.class.getDeclaredMethod("disconnect", BluetoothDevice.class);
            Log.v(TAG, "Found the method");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.e(TAG, "Unable to find disconnect(BluetoothDevice) method in proxy." + e.getMessage());
            return null;
        }

        try {
            connectA2dp = BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
            connectHead = BluetoothHeadset.class.getDeclaredMethod("connect", BluetoothDevice.class);
        } catch (NoSuchMethodException ex) {
            Log.e(TAG, "Unable to find connect(BluetoothDevice) method in BluetoothA2dp proxy.");
            return null;
        }

        if (connectHead != null && disconnectHead != null && connectA2dp != null && disconnectA2dp != null) {
            methods.add(connectHead);
            methods.add(disconnectHead);
            methods.add(connectA2dp);
            methods.add(disconnectA2dp);
            return methods;
        }
        return null;
    }

    public void getBluetoothHeadset(Context context) {
        mBluetoothAdapter.getProfileProxy(
                context, mBluetoothHeadsetServiceListener, BluetoothProfile.HEADSET);
        mBluetoothAdapter.getProfileProxy(
                context, mBluetoothHeadsetServiceListener, BluetoothProfile.A2DP);
    }

    private void connectToDevice(BluetoothDevice device) {
        ArrayList<Method> methods = getConnectMethod();
        if (methods == null) {
            Log.e(TAG, "Unable to get connect methods!");
            return;
        }
        Method connectHead = methods.get(0);
        Method disconnectHead = methods.get(1);
        Method connectA2dp = methods.get(2);
        Method disconnectA2dp = methods.get(3);
        if (mBluetoothHeadset != null && mBluetoothHeadset.getConnectedDevices().size() > 0) {
            mCurrentDevice = mBluetoothHeadset.getConnectedDevices().get(0);
            Log.v(TAG, "Connected device Mac Address: " + mCurrentDevice.getAddress() + " We want to connect to: " + device.getAddress());

            if (methods.contains(null) || mCurrentDevice == null) {
                Log.v(TAG, "Exiting: methods:" + methods.contains(null) + " mCurrentDevice: " + mCurrentDevice);
                return;
            }
            try {
                disconnectHead.setAccessible(true);
                disconnectA2dp.setAccessible(true);
                disconnectHead.invoke(mBluetoothHeadset, mCurrentDevice);
                disconnectA2dp.invoke(mBluetoothA2dp, mCurrentDevice);
            } catch (InvocationTargetException ex) {
                Log.e(TAG, "Unable to invoke disconnect(BluetoothDevice) method on proxy. " + ex.toString());
            } catch (IllegalAccessException ex) {
                Log.e(TAG, "Illegal Access! " + ex.toString());
            }
        } else {
            Log.e(TAG, " No connected BluetoothHeadsets");
        }

        mSwitchSemaphore = new Semaphore(0);
        try {
            connectHead.setAccessible(true);
            connectA2dp.setAccessible(true);
            connectHead.invoke(mBluetoothHeadset, device);
            connectA2dp.invoke(mBluetoothA2dp, device);
        } catch (InvocationTargetException ex) {
            Log.e(TAG, "Unable to invoke connect(BluetoothDevice) method on proxy. " + ex.toString());
        } catch (IllegalAccessException ex) {
            Log.e(TAG, "Illegal Access! " + ex.toString());
        }
    }

    public void connectToDeviceByMacAddress(String macAddress) {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
        Log.v(TAG, "Device macAddress to connect to:" + device.getAddress());
        connectToDevice(device);
    }

    public BluetoothDevice getCurrentDevice() {
        return mCurrentDevice;
    }

//    public String getExpectedDevice() {
//        return mExpectedDevice;
//    }
//    public void setExpectedDevice(String expectedDevice) {
//        this.mExpectedDevice = expectedDevice;
//    }

//    void removeExpectedDevice() {
//        mExpectedDevice = null;
//    }
}

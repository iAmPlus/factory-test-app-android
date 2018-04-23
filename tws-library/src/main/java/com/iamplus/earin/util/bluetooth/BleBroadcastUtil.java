package com.iamplus.earin.util.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.BluetoothGattStatus;
import com.iamplus.earin.communication.cap.CapCommunicationController;
import com.iamplus.earin.communication.cap.CapCommunicationControllerDelegate;
import com.iamplus.earin.communication.cap.CapCommunicator;
import com.iamplus.earin.communication.cap.CapCommunicatorEvent;
import com.iamplus.earin.communication.models.BatteryReading;
import com.iamplus.earin.communication.models.DeviceAddress;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import static com.iamplus.earin.communication.cap.CapCommunicator.INTENT_COMM_EVENT;
import static com.iamplus.earin.communication.cap.CapCommunicator.INTENT_EXTRAS_EVENT_NAME;
import static com.iamplus.earin.communication.cap.CapCommunicator.INTENT_EXTRAS_EVENT_PAYLOAD;

public class BleBroadcastUtil implements CapCommunicationControllerDelegate {

    private static final String TAG = BleBroadcastUtil.class.getSimpleName();
    private static BleBroadcastUtil instance;
    private ArrayList<BleEventListener> mBleEventListeners;
    private BatteryUpdateListener mBatteryUpdateListener;
    private NfmiUpdateListener mNfmiUpdateListener;
    private OmegaCallEventListener mOmegaCallEventListener;
    private boolean mIsConnected;
    private Boolean mLastNfmiStatus;
    private BatteryReading mLastBatteryReading;

    private BleBroadcastUtil(Context context) {
        mLastNfmiStatus = null;
        mBleEventListeners = new ArrayList<>();
        CapCommunicationController.getInstance().setDelegate(this);

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent incomingIntent) {
                Bundle intentExtras = incomingIntent.getExtras();
                if (intentExtras == null) {
                    return;
                }
                String eventName = intentExtras.getString(INTENT_EXTRAS_EVENT_NAME);

                if (CapCommunicatorEvent.BatteryReading.identifier().equals(eventName)) {
                    mLastBatteryReading = (BatteryReading) intentExtras.get(INTENT_EXTRAS_EVENT_PAYLOAD);
                    if (mBatteryUpdateListener != null) {
                        mBatteryUpdateListener.onBatteryEvent(mLastBatteryReading);
                    }
                } else if (CapCommunicatorEvent.NfmiConnected.identifier().equals(eventName)) {
                    mLastNfmiStatus = true;
                    if (mNfmiUpdateListener != null) {
                        mNfmiUpdateListener.onNfmiStateChange(true);
                    }
                } else if (CapCommunicatorEvent.NfmiDisconnected.identifier().equals(eventName)) {
                    mLastNfmiStatus = false;
                    if (mNfmiUpdateListener != null) {
                        mNfmiUpdateListener.onNfmiStateChange(false);
                    }
                } else if (CapCommunicatorEvent.MacAddress.identifier().equals(eventName)) {
                    String address = intentExtras.getString(INTENT_EXTRAS_EVENT_PAYLOAD);
                    Log.w(TAG, "received address: " + address);
                    DeviceAddress.getInstance().addAddress(address);

                } else if (CapCommunicatorEvent.OmegaCall.identifier().equals(eventName)) {
                    mOmegaCallEventListener.omegaCallEvent();
                    Log.w(TAG, "received address: ");
                }
            }
        };

        IntentFilter filter = new IntentFilter(INTENT_COMM_EVENT);
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, filter);
    }

    public static BleBroadcastUtil getInstance(Context context) {
        if (instance == null) {
            instance = new BleBroadcastUtil(context);
        }
        return instance;
    }

    public void addBleEventListener(BleEventListener bleEventListener) {
        mBleEventListeners.add(bleEventListener);
    }

    public void removeBleEventListener(BleEventListener bleEventListener) {
        this.mBleEventListeners.remove(bleEventListener);
    }

    public void setBatteryUpdateListener(BatteryUpdateListener batteryUpdateListener) {
        this.mBatteryUpdateListener = batteryUpdateListener;
    }

    public void removeBatteryUpdateListener() {
        this.mBatteryUpdateListener = null;
    }

    public void setNfmiUpdateListener(NfmiUpdateListener nfmiUpdateListener) {
        this.mNfmiUpdateListener = nfmiUpdateListener;
    }

    public void setOmegaCallEventListener(OmegaCallEventListener omegaCallEventListener) {
        this.mOmegaCallEventListener = omegaCallEventListener;
    }

    public void removeNfmiUpdateListener() {
        this.mNfmiUpdateListener = null;
    }

    public BatteryReading getLastBatteryReading() {
        return mLastBatteryReading;
    }

    public Boolean getLastNfmiStatus() {
        return mLastNfmiStatus;
    }

    public void setLastNfmiStatus(Boolean lastNfmiStatus) {
        if (this.mLastNfmiStatus == null) this.mLastNfmiStatus = lastNfmiStatus;
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    @Override
    public void deviceConnected(CapCommunicationController controller, String identifier) {
        Log.d(TAG, "Delegate: device connected; " + identifier);
        mIsConnected = true;
        // reset device to connect to!
//        BluetoothUtil.getInstance(EarinApplication.getContext()).removeExpectedDevice();
        BleEventListener bleEventListener = mBleEventListeners.size() > 0 ?
                mBleEventListeners.get(mBleEventListeners.size() - 1) : null;
        if (bleEventListener != null) {
            bleEventListener.onConnect();
        }
    }

    @Override
    public void deviceDisconnected(CapCommunicationController controller, String identifier, BluetoothGattStatus status) {
        Log.d(TAG, "Delegate: device disconnected; " + identifier);
        Manager.getSharedManager().removePendingRequests();
        Semaphore switchSemaphore = BluetoothUtil.getInstance(EarinApplication.getContext()).getSwitchSemaphore();
        if (switchSemaphore.availablePermits() == 0) {
            switchSemaphore.release();
        }
        mIsConnected = false;
        BleEventListener bleEventListener = mBleEventListeners.size() > 0 ?
                mBleEventListeners.get(mBleEventListeners.size() - 1) : null;
        if (bleEventListener != null) {
            bleEventListener.onDisconnect(false, status);
        }
    }

    @Override
    public boolean permittedToEnableBluetooth(CapCommunicationController controller) {
        Log.d(TAG, "Delegate: permitted to enable Bluetooth? -> No, we anable it only on the splash screen!");
        return false;
    }


    @Override
    public boolean keepConnectedDevice(CapCommunicationController controller, CapCommunicator communicator) throws Exception {
        Log.d(TAG, "Delegate: keep conencted device?");
        return true;
    }

    public interface BleEventListener {
        void onConnect();

        void onDisconnect(boolean isCompletingUpgrade, BluetoothGattStatus status);
    }

    public interface BatteryUpdateListener {
        void onBatteryEvent(BatteryReading batteryReading);
    }

    public interface NfmiUpdateListener {
        void onNfmiStateChange(boolean connected);
    }

    public interface OmegaCallEventListener {
        void omegaCallEvent();
    }

    public interface CapResponseListener {
        void onResponse(Object response);

        void onError(Exception exception);
    }

}

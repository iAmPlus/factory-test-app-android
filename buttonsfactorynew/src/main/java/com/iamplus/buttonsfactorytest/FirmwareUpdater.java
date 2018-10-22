package com.iamplus.buttonsfactorytest;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaPacket;
import com.csr.gaiacontrol.Controller;
import com.csr.gaiacontrol.VMUUpdateManager;
import com.csr.gaiacontrol.VMUpdateCallback;
import com.csr.gaiacontrol.implementations.ConnectionRunnable;
import com.iamplus.systemupdater.UpdateManager;

import static com.iamplus.systemupdater.Utils.sendProgressNotification;

public class FirmwareUpdater implements UpdateManager.UpdateListener, VMUpdateCallback,
        VMUUpdateManager.IUpdateVMListener, ConnectionRunnable.IConnectionListener{
    private static String TAG = "FirmwareUpdater";
    private static final String INTENT_UPDATE_COMPLETE = "aneeda.UPDATE_COMPLETE";
    private static final String INTENT_UPDATE_ABORT = "aneeda.UPDATE_ABORT";
    private static final String INTENT_UPDATE_DECLINED = "aneeda.UPDATE_CONFIRMATION_DECLINED";
    private Context mContext;
    private UpdateManager mUpdateManager;
    /**
     * The fragment to display the update for ADK4.0 to the user.
     */
    private VMUUpdateManager mVMUUpdateManager;
    /**
     * The handler to run some tasks.
     */
    private final Handler mHandler = new Handler();

    /**
     * The device in which we are connected.
     */
    private BluetoothDevice mDevice;

    private Controller mController;

    private String mModelNumber;
    private String mControllerVersion;
    private boolean mIsVMUpdaterRegistered;

    public FirmwareUpdater(Context context, Controller controller) {
        mContext = context;
        mController = controller;
        init();
    }

    private void init() {
        mUpdateManager = UpdateManager.getInstance(mContext);
        mUpdateManager.register(this);
        mVMUUpdateManager = VMUUpdateManager.getInstance();
        mVMUUpdateManager.setContext(mContext);
        mUpdateManager.setState(UpdateManager.State.IDLE);
        mUpdateManager.doAutoCheckForUpdates();
    }


    public boolean isUpdateInProgress(){
        return mVMUUpdateManager.isUpdating();
    }

    private void registerReceivers() {
        if (mIsVMUpdaterRegistered) {
            return;
        }
        mIsVMUpdaterRegistered = true;
        mVMUUpdateManager.setUpdateListener(this);
        Utils.cancelNotifications(mContext);

        IntentFilter filters = new IntentFilter();
        filters.addAction(INTENT_UPDATE_ABORT);
        filters.addAction(INTENT_UPDATE_COMPLETE);
        filters.addAction(INTENT_UPDATE_DECLINED);
        mContext.registerReceiver(mUpdateReceiver, filters);
        mController.registerUpdateCallbacks(this);
    }


    private void unregisterReceivers() {
        if (!mIsVMUpdaterRegistered) {
            return;
        }
        mIsVMUpdaterRegistered = false;
        mContext.unregisterReceiver(mUpdateReceiver);
        mController.registerUpdateCallbacks(null);
        mVMUUpdateManager.setUpdateListener(null);
        mVMUUpdateManager.setUpdateFileUrl(null);
    }

    public void checkForUpdates() {
        if(!isUpdateInProgress()) {
            log("starting updates");
            mUpdateManager.checkForUpdates();
        }else{
            log("Update already in progress");
        }
    }

    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("onReceive " + action);
            if (action.equalsIgnoreCase(INTENT_UPDATE_ABORT)) {
                mUpdateManager.cancelUpdate();
            } else if (action.equalsIgnoreCase(INTENT_UPDATE_COMPLETE)) {
                mUpdateManager.onUpdateComplete(true);
            } else if (action.equalsIgnoreCase(INTENT_UPDATE_DECLINED)) {
                mUpdateManager.autoInstallUpdate();
            }
        }
    };

    public void log(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    public void onStateChanged() {
        log("onStateChanged: state " + mUpdateManager.getState());

        if (!mController.isConnected()) {
            return;
        }
        switch (mUpdateManager.getState()) {
            case IDLE:
                unregisterReceivers();
                break;
            case QUERYING_UPDATE:
                if (!mController.isConnected()) {
                    mUpdateManager.setControllerVersion(null);
                }
                break;
            case UPDATE_AVAILABLE:
                if (!mController.isConnected()) {
                    break;
                }
                registerReceivers();
                mUpdateManager.sendEmptyNotification("Update Available ", "OTA download in progress");
                mUpdateManager.downloadAndInstallUpdate();
                break;
            case UPDATE_READY:
                if (!mController.isConnected()) {
                    break;
                }
                mUpdateManager.setState(UpdateManager.State.INSTALLING_UPDATE);
                registerReceivers();
                mVMUUpdateManager.setUpdateFileUrl(mUpdateManager.getUpdateInfo().getFilePath(mContext));
                mVMUUpdateManager.startUpdate();
                break;
            case UPDATE_DONE:
                mUpdateManager.save();
                unregisterReceivers();
                break;
        }
    }

    @Override
    public void onDownloadProgressChanged(int percentage, int downloadSize) {
        //Do nothing. This is when file is downloaded from fota server to phone
    }

    @Override
    public void onNetworkChange(boolean connected) {

    }

    @Override
    public void finish() {

    }


    @Override
    public void onUpdateActivated() {
        mVMUUpdateManager.onUpdateActivated();
    }

    @Override
    public void onUpdateActivatedFailed() {
        mVMUUpdateManager.onUpdateActivatedFailed();
    }

    @Override
    public void onVMDisconnected() {
        Utils.cancelNotifications(mContext);
        boolean hasReceivedFatalError = mVMUUpdateManager.hasReceivedFatalError();
        mVMUUpdateManager.onVMDisconnected();
        if (hasReceivedFatalError) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mController.establishGAIAConnection();
                }
            }, 2 * 1000);
        }
        finish();
    }

    @Override
    public void onVMControlSucceed() {
        mVMUUpdateManager.onVMControlSucceed();
    }

    @Override
    public void onVMControlFailed() {
        mUpdateManager.setState(UpdateManager.State.ERROR);
        mVMUUpdateManager.onVMControlFailed();
    }

    @Override
    public void handlerVMEvent(GaiaPacket packet) {
        mVMUUpdateManager.handlerVMEvent(packet);
    }

    private void onDeviceConnected() {
        log("onDeviceConnected  ");
        if (mVMUUpdateManager.isUpdating()) {
            mVMUUpdateManager.onDeviceConnected();
        }
    }

    /**
     * This method is called when we detect that the device is disconnected.
     */
    private void onDeviceDisconnected() {
        log("onDeviceDisconnected  ");
        mDevice = mController.getBluetoothDevice();
        if (mVMUUpdateManager.isUpdating()) {
            mVMUUpdateManager.onDeviceDisconnected();
        }
    }
    @Override
    public void onConnected() {
        onDeviceConnected();
    }

    @Override
    public void onDisconnected() {
        onDeviceDisconnected();
    }

    @Override
    public void onError(GaiaError error) {
        handleError(error);
    }

    /**
     * To manage errors information catching inside the handler and coming from the library.
     *
     * @param error
     *            The error coming from the library formatting as a <code>GaiaError<</code>.
     */
    private void handleError(GaiaError error) {
        switch (error.getType()) {
            case CONNECTION_FAILED:
                Log.w(TAG, "Received error: " + error.getStringException());
                break;
            case ILLEGAL_ARGUMENT:
            case SENDING_FAILED:
            case RECEIVING_FAILED:
            default:
                Log.w(TAG, "Received error: 1234" + error.getStringException());
        }
    }

    @Override
    public void sendPacket(int commandId, int... param) {
        mController.sendCommand(commandId, param);
    }

    @Override
    public void sendPacket(int commandId, byte[] payload) {
        mController.sendCommand(commandId, payload);
    }

    @Override
    public void disconnectDevice() {
        mController.disconnect();
    }

    @Override
    public void registerForNotifications(Gaia.EventId event) {
        mController.registerNotification(event);
    }

    @Override
    public void unregisterForNotifications(Gaia.EventId event) {
        if (mController.isConnected()) {
            mController.cancelNotification(event);
        }
    }

    @Override
    public boolean showUI() {
        return false;
    }

    @Override
    public void onTransferComplete() {
        Utils.cancelNotifications(mContext);
        mUpdateManager.sendUpdateReadyNotification();
    }

    @Override
    public void onUpdateComplete() {
        //Do Nothing. Will get Broadcast
    }

    @Override
    public void displayLowBatteryAlert() {
        mUpdateManager.sendEmptyNotification(mContext.getString(R.string.update_alert_battery_low_title), mContext.getString(R.string.update_alert_battery_low));
    }

    @Override
    public void notifyUpdateProgress(double percentage) {
        sendProgressNotification(mContext, "Version: "+ mUpdateManager.getUpdateInfo().to_version +" OTA transfer progress", percentage);
    }

    @Override
    public void onUpdateAborted() {
        //Do Nothing
    }

    @Override
    public void onUserConfirmationDeclined() {
        //Do Nothing. Will get Broadcast
    }

    @Override
    public void connect() {
        if (!mController.isConnected()) {
            mController.connect(mDevice, mController.getTransport());
        }
    }

    @Override
    public void connectFailed() {

    }

    public void setVersion(String version) {
        Log.v(TAG,"setVerion: " + version);
        mUpdateManager.setControllerVersion(version);
    }


    public void setState(UpdateManager.State state) {
        mUpdateManager.setState(state);
    }
}

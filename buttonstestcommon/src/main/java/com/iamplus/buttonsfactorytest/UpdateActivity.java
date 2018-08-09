/**************************************************************************************************
 * Copyright 2015 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/
package com.iamplus.buttonsfactorytest;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaPacket;
import com.csr.gaiacontrol.Controller;
import com.csr.gaiacontrol.VMUUpdateManager;
import com.csr.gaiacontrol.VMUpdateCallback;
import com.csr.gaiacontrol.implementations.ConnectionRunnable;
import com.csr.vmupgradelibrary.codes.ResumePoints;

/**
 * <p>This activity is the activity to control the equalizer for a device connected to the application.</p>
 */

public class UpdateActivity extends AppCompatActivity implements VMUUpdateManager.IUpdateVMListener,
        ConnectionRunnable.IConnectionListener, VMUpdateCallback {

    /**
     * For the debug mode, the tag to display for logs.
     */
    private static final String TAG = "UpdateActivity";

    /**
     * The fragment to display the update for ADK4.0 to the user.
     */
    private VMUUpdateManager mVMUUpdateManager;
    /**
     * The handler to run some tasks.
     */
    private static final Handler mHandler = new Handler();
    /**
     * The runnable used for a timeout when we try to connect to the device.
     */
    private static ConnectionRunnable mConnectionRunnable;
    /**
     * A dialog to display information to the user when we attempt to connect to the device.
     */
    private AlertDialog mAttemptConnectionDialog;
    /**
     * A dialog to display a question to the user when we can't connect to the device.
     */
    private AlertDialog mFailedConnectionDialog;
    /**
     * To know if the disconnection is coming from this application or is coming from the board.
     */
    private boolean isDisconnectionFromApp = false;
    /**
     * The device in which we are connected.
     */
    private BluetoothDevice mDevice;

    private Controller mController;


    @Override
    public void sendPacket(int commandId, int... param) {
        mController.sendCommand(commandId, param);
        if (commandId == Gaia.COMMAND_VM_UPGRADE_DISCONNECT) {
            mAttemptConnectionDialog.cancel();
        }
    }

    @Override
    public void sendPacket(int commandId, byte[] payload) {
        mController.sendCommand(commandId, payload);
    }

    @Override
    public void disconnectDevice() {
        isDisconnectionFromApp = true;
        mController.disconnect();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        Log.v(TAG,"*** onSaveInstanceState ****");
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
    public void connect() {
        if (!mController.isConnected()) {
            mController.connect(mDevice, mController.getTransport());
        }
    }

    @Override
    public void connectFailed() {
        mAttemptConnectionDialog.cancel();
        if(!isFinishing()) {
            mFailedConnectionDialog.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.csr.gaiacontrol.R.layout.activity_update);
        this.init();
    }


    /**
     * To initialise objects used in this activity.
     */
    private void init() {
        this.setSupportActionBar((Toolbar) findViewById(com.csr.gaiacontrol.R.id.tb_menu));
        this.buildDialogs();

        mVMUUpdateManager = VMUUpdateManager.getInstance();
        mVMUUpdateManager.setUpdateListener(this);
        mVMUUpdateManager.setContext(this);
        mConnectionRunnable = new ConnectionRunnable(this);

        mController = Controller.getInstance();
        mController.registerUpdateCallbacks(this);

        cancelNotifications(this);
        if (mVMUUpdateManager.getResumePoint() == ResumePoints.TRANSFER_COMPLETE && mVMUUpdateManager.isVMConnected()) {
            mVMUUpdateManager.askForTransferCompleteRes(this);
        } else {
            mVMUUpdateManager.startUpdate();
        }
    }

    /**
     * To initialise alert dialogs to display with this activity.
     */
    private void buildDialogs () {
        // build the dialog to show a progress bar when we try to reconnect.
        AlertDialog.Builder attemptDialogBuilder = new AlertDialog.Builder(UpdateActivity.this);
        attemptDialogBuilder.setTitle(getString(com.csr.gaiacontrol.R.string.alert_attempt_connection_title));

        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(com.csr.gaiacontrol.R.layout.dialog_progress_bar, null);
        attemptDialogBuilder.setView(dialogLayout);

        attemptDialogBuilder.setCancelable(false);
        mAttemptConnectionDialog = attemptDialogBuilder.create();

        // build a dialog to ask to the user to try a reconnection.
        AlertDialog.Builder noConnectionDialogBuilder = new AlertDialog.Builder(UpdateActivity.this);
        noConnectionDialogBuilder.setTitle(getString(com.csr.gaiacontrol.R.string.alert_connection_failed_title));
        noConnectionDialogBuilder.setMessage(getString(com.csr.gaiacontrol.R.string.alert_connection_failed_text));
        // set positive button: "try again" message
        noConnectionDialogBuilder.setPositiveButton(getString(com.csr.gaiacontrol.R.string.alert_connection_failed_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        attemptReconnection();
                    }
                });
        // set cancel button
        noConnectionDialogBuilder.setNegativeButton(getString(com.csr.gaiacontrol.R.string.alert_connection_failed_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        noConnectionDialogBuilder.setCancelable(false);
        mFailedConnectionDialog = noConnectionDialogBuilder.create();
    }

    /**
     * The method to attempt to reconnect to the device.
     */
    private void attemptReconnection() {
        try {
            mHandler.postDelayed(mConnectionRunnable, ConnectionRunnable.CONNECTION_TIMEOUT);
            mAttemptConnectionDialog.show();
        } catch (Exception e) {
            Log.v(TAG,"Exception while attempting to reconnect");
        }
    }

    /**
     * This method is called when we detect that the device is disconnected.
     */
    private void onDeviceDisconnected() {
        if (isDisconnectionFromApp) {
            isDisconnectionFromApp = false;
            finish();
        }
        else {
            mDevice = mController.getBluetoothDevice();
            mVMUUpdateManager.onDeviceDisconnected();
            attemptReconnection();
        }
    }

    /**
     * This method is called when we are connected to a device. This allows to act depending on the application actual state.
     */
    private void onDeviceConnected() {
        mHandler.removeCallbacks(mConnectionRunnable);
        mConnectionRunnable.restart();

        mAttemptConnectionDialog.cancel();

        mVMUUpdateManager.onDeviceConnected();
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
        mVMUUpdateManager.onVMDisconnected();
        finish();
    }

    @Override
    public void onVMControlSucceed() {
        mVMUUpdateManager.onVMControlSucceed();
    }

    @Override
    public void onVMControlFailed() {
        mVMUUpdateManager.onVMControlFailed();
    }

    @Override
    public void handlerVMEvent(GaiaPacket packet) {
        mVMUUpdateManager.handlerVMEvent(packet);
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
    public boolean showUI() {
        return true;
    }

    @Override
    public void onTransferComplete() {
        //Do Nothing
    }

    @Override
    public void onUpdateAborted() {
        notifySystemUpdaterForAbort();
    }

    @Override
    public void onUpdateComplete() {
        notifyUpdateComplete();
    }

    @Override
    public void displayLowBatteryAlert() {
        //Do Nothing
    }

    @Override
    public void notifyUpdateProgress(double percentage) {

    }

    @Override
    public void onUserConfirmationDeclined() {
        notifyUserConfirmationDeclined();
    }

    private void notifySystemUpdaterForAbort() {
        Intent intent = new Intent();
        intent.setAction("aneeda.UPDATE_ABORT");
        sendBroadcast(intent);
    }

    private void notifyUserConfirmationDeclined() {
        Intent intent = new Intent();
        intent.setAction("aneeda.UPDATE_CONFIRMATION_DECLINED");
        sendBroadcast(intent);
    }

    private void notifyUpdateComplete() {
        Intent intent = new Intent();
        intent.setAction("aneeda.UPDATE_COMPLETE");
        sendBroadcast(intent);
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
                mHandler.postDelayed(mConnectionRunnable, ConnectionRunnable.CONNECTION_TIMEOUT);
                break;
            case ILLEGAL_ARGUMENT:
            case SENDING_FAILED:
            case RECEIVING_FAILED:
            default:
                Log.w(TAG, "Received error: 1234" + error.getStringException());
        }
    }

    public static void cancelNotifications(Context context) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }
}

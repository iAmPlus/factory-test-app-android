/**************************************************************************************************
 * Copyright 2015 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/
package com.csr.gaiacontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaPacket;
import com.csr.gaiacontrol.utils.Consts;
import com.csr.gaiacontrol.utils.Utils;
import com.csr.gaiacontrol.views.VMUpdateDialog;
import com.csr.vmupgradelibrary.VMUPacket;
import com.csr.vmupgradelibrary.codes.OpCodes;
import com.csr.vmupgradelibrary.codes.ResumePoints;
import com.csr.vmupgradelibrary.codes.ReturnCodes;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;


public class VMUUpdateManager implements VMUpdateDialog.IDialogUpdateListener {

    /**
     * For the debug mode, the tag to display for logs.
     */
    private static final String TAG = "VMUUpdateManager";
    /**
     * To know if we are using the application in the debug mode.
     */
    private static final boolean DEBUG = Consts.DEBUG;
    private static VMUUpdateManager mVMUUpdateManager;
    private Context mContext;

    /**
     * The listener to interact with the activity which implements this fragment.
     */
    private IUpdateVMListener mActivityListener;

    private VMUpdateDialog mUpdateDialog;
    /**
     * The handler to run some tasks.
     */
    private final Handler mHandler = new Handler();

    /**
     * To know if the update process is currently running.
     */
    private boolean isUpdating = false;
    /**
     * To know how many times we try to start the update.
     */
    private int mStartAttempts = 0;
    /**
     * The offset to use to upload data on the device.
     */
    private int mStartOffset = 0;
    /**
     * The file to upload on the device.
     */
    private byte[] mBytesFile;
    /**
     * To know if an error occurs we just want to abort without disconnecting from the VM update process.
     */
    private boolean isOnlyAborting = false;
    /**
     * The maximum value for the data length of a VM update packet.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final int MAX_DATA = Gaia.MAX_PAYLOAD - VMUPacket.LENGTH_REQUIRED_INFORMATION;
    /**
     * To know the maximum of times we will attempt to start the update process.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final int START_ATTEMPTS_MAX = 5;
    /**
     * To know how much time we should between two attempts to start the update process.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final int START_ATTEMPTS_TIME = 2000;
    /**
     * To know when we received a fatal error and we have to disconnect from the device.
     */
    private boolean hasReceivedFatalError = false;
    /**
     * To know the error code we received when we received an error.
     */
    private byte[] mReceivedErrorCode;
    /**
     * To know if the packet with the operation code "UPDATE_DATA" which was sent was the last packet to send.
     */
    private boolean wasLastPacket = false;
    /**
     * The value of the actual resume point to display to the user.
     */
    private ResumePoints mResumePoint = null;
    /**
     * To know if the user asked for a disconnection during the update process.
     */
    private boolean hasToDisconnect = false;
    /**
     * The file to upload on the board.
     */
    private File mFile;

    /**
     * To know when the transfer starts
     */
    private long mTimeStartTransfer = 0;
    private String mUpdateFileUrl;
    private boolean mUpdateAborted = false;
    private boolean mIsVMDisconnected;
    private boolean mIsDeviceOnLowBattery;

    private VMUUpdateManager() {
    }

    public static VMUUpdateManager getInstance() {
        if (mVMUUpdateManager == null) {
            mVMUUpdateManager = new VMUUpdateManager();
        }
        return mVMUUpdateManager;
    }

    public void setUpdateListener(IUpdateVMListener listener) {
        mActivityListener = listener;
    }

    public void setContext(Context context) {
        mContext = context;
        mUpdateDialog = new VMUpdateDialog(mContext, this);
    }


    /**
     * When the device is ready for the update, this method has to be called.
     */
    public void onUpdateActivated() {
        mIsVMDisconnected = false;
        mActivityListener.registerForNotifications(Gaia.EventId.VMU_PACKET);
        sendSyncReq();
    }

    /**
     * When it's not possible to connect - activate - for the vm update to the device.
     */
    public void onUpdateActivatedFailed() {
        mIsVMDisconnected = true;
        displayErrorOrAbort2(mContext.getResources().getString(R.string.update_error_vm_connection_failed));
    }

    @Override
    public void abortUpdate() {
        if (isUpdating) {
            isUpdating = false;
            if (hasReceivedFatalError) {
                sendErrorConfirmation(mReceivedErrorCode);
                disconnectUpdate();
            }
            else {
                sendAbortReq();
            }
            mUpdateAborted = true;
        }
    }

    public boolean isUpdateAborted() {
        return mUpdateAborted;
    }

    @Override
    public ResumePoints getResumePoint() {
        return mResumePoint;
    }


    /**
     * This method allows to manage a VM message from the device using the Gaia protocol.
     *
     * @param packet
     *            The packet received from the device.
     */
    public void handlerVMEvent(GaiaPacket packet) {
        byte[] payload = packet.getPayload();
        byte[] data = Arrays.copyOfRange(payload, 1, payload.length);
        VMUPacket vmuPacket = VMUPacket.buildPacketFromBytes(data);
        handleVMUPacket(vmuPacket);
    }

    /**
     * This method is called when the VM update is deactivated on the board.
     */
    public void onVMDisconnected() {
        mIsVMDisconnected = true;
        if (hasReceivedFatalError) {
            hasReceivedFatalError = false;
            mActivityListener.disconnectDevice();
        }

    }

    public boolean hasReceivedFatalError() {
        return hasReceivedFatalError;
    }

    /**
     * This method is called when we received a succeed acknowledgment from the board about a VM_CONTROL GAIA command we sent.
     */
    public void onVMControlSucceed() {
        if (wasLastPacket) {
            wasLastPacket = false;
            setResumePoint(ResumePoints.VALIDATION);
            sendValidationDoneReq();
        }
        else if (hasToDisconnect) {
            hasToDisconnect = false;
            disconnectUpdate();
        }
    }

    /**
     * This method is called when we received a failed acknowledgment from the board about a VM_CONTROL GAIA command we sent.
     */
    public void onVMControlFailed() {
        wasLastPacket = false;
        hasToDisconnect = false;
        displayErrorOrAbort2(mContext.getResources().getString(R.string.update_vm_command_failed));
    }

    /**
     * To know if the update process is running.
     *
     * @return
     *          True if the process is running, false otherwise.
     */
    public boolean isUpdating() {
        return isUpdating;
    }

    /**
     * When the device is disconnected - in general because it turned off - this method is called.
     */
    public void onDeviceDisconnected() {
        showUpdateDialog(false);
        restartOffset();
    }

    /**
     * To display the update dialog when the update starts.
     */
    private void showUpdateDialog(boolean show) {
        Log.v(TAG,"showUpdateDialog isShowing: " + mUpdateDialog.isShowing());
        if (!mActivityListener.showUI()) {
            return;
        }
        if (show && !mUpdateDialog.isShowing()){
            if(!((Activity)mContext).isFinishing()) {
                mUpdateDialog.show();
            }
        }
        else //noinspection StatementWithEmptyBody
            if (!show && mUpdateDialog.isShowing()) {
                mUpdateDialog.dismiss();
            }
    }

    /**
     * This method is called by the activity when the board is connected to the application.
     */
    public void onDeviceConnected() {
        showUpdateDialog(true);
        startUpdate();
    }

    public void startUpdate() {
        Log.v(TAG,"startUpdate mUpdateFileUrl:" + mUpdateFileUrl);
        isUpdating = true;
        restartOffset();
        mFile = new File(mUpdateFileUrl);
        mBytesFile = Utils.getBytesFromFile(mFile);
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONNECT);
    }

    /**
     * To disconnect from the device about a VM upgrade.
     */
    private void disconnectUpdate() {
        showUpdateDialog(false);
        mActivityListener.unregisterForNotifications(Gaia.EventId.VMU_PACKET);
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_DISCONNECT);
    }


    /**
     * To display an alert when the board battery is low. Then we restart the process by sending the UPDATE_SYNC_REQ.
     */
    private void displayBatteryLowAlert() {
        if (!mActivityListener.showUI()) {
            mIsDeviceOnLowBattery = true;
            Log.v(TAG, mContext.getString(R.string.update_alert_battery_low));
            mActivityListener.displayLowBatteryAlert();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.update_alert_battery_low)
                .setTitle(R.string.update_alert_battery_low_title)
                .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // we try to restart the update process
                        sendSyncReq();
                    }
                });
        builder.show();
    }

    /**
     * To define the actual resume point and to display the dialog which provide the information to the user.
     *
     * @param point
     *              The resume point ot define as the actual one.
     */
    private void setResumePoint(ResumePoints point) {
        mResumePoint = point;
        Log.v(TAG,"setResumePoint:: " + mResumePoint);
        if (mUpdateDialog.isShowing()) {
            mUpdateDialog.updateStep();
        }
    }

    private Handler mTimerHandler = new Handler();

    private void startTimer() {
        Log.v(TAG,"StartTimer");
        mTimerHandler.removeCallbacks(mCompleteUpdate);
        mTimerHandler.postDelayed(mCompleteUpdate, 3 * 60 * 1000);//3 Minutes
    }

    private void stopTimer() {
        Log.v(TAG,"stopTimer");
        mTimerHandler.removeCallbacks(mCompleteUpdate);
    }

    private Runnable mCompleteUpdate = new Runnable() {
        @Override
        public void run() {
            displayAlertComplete();
        }
    };

    /**
     * To manage the reception of a message about the VM update.
     *
     * @param packet
     *            the received packet.
     */
    private void handleVMUPacket(VMUPacket packet) {
        switch (packet.getOpCode()) {
            case OpCodes.UPDATE_SYNC_CFM:
                receiveSyncCFM(packet);
                break;

            case OpCodes.UPDATE_START_CFM:
                receiveStartCFM(packet);
                break;

            case OpCodes.UPDATE_DATA_BYTES_REQ:
                receiveDataBytesReq(packet);
                break;

            case OpCodes.UPDATE_ABORT_CFM:
                receiveAbortCFM();
                break;

            case OpCodes.UPDATE_ERROR_WARN_IND:
                handleVMUpdateErrors(packet);
                break;

            case OpCodes.UPDATE_IS_VALIDATION_DONE_CFM:
                receiveValidationDoneCFM();
                break;

            case OpCodes.UPDATE_TRANSFER_COMPLETE_IND:
                receiveTransferCompleteInd();
                break;

            case OpCodes.UPDATE_COMMIT_REQ:
                receiveCommitReq();
                break;

            case OpCodes.UPDATE_ERASE_SQIF_REQ:
                receiveEraseSQIFReq();
                break;

            case OpCodes.UPDATE_COMPLETE_IND:
                receiveCompleteInd();
                break;

            default:
                if (DEBUG) Log.d(TAG, "Received VM packet: " + Utils.getStringFromBytes(packet.getBytes()));
        }
    }

    /**
     * To manage errors received during VM Update.
     *
     * @param packet
     *            The received packet.
     */
    private void handleVMUpdateErrors(VMUPacket packet) {
        byte[] data = packet.getData();
        int code = Utils.extractIntField(data, 0, 2, false);

        if (DEBUG)
            Log.d(TAG, "Receive VM UPDATE ERRORS with code: " + Utils.getStringFromBytes(data));

        switch (code) {
            case ReturnCodes.WARN_SYNC_ID_IS_DIFFERENT:
                isOnlyAborting = true;
                displayErrorOrAbort2(mContext.getResources().getString(R.string.update_error_sync_is_different));
                break;

            case ReturnCodes.ERROR_BATTERY_LOW:
                displayBatteryLowAlert();
                break;

            default:
                // All return codes where we have to disconnect from the update.
                hasReceivedFatalError = true;
                mReceivedErrorCode = data;
                displayErrorOrAbort(mContext.getString(R.string.update_error_dot) + " Error code: " + Utils.getStringFromBytes(data),
                        mContext.getString(R.string.update_error_dot) + " Error message: " + ReturnCodes.getReturnCodesMessage(code));
        }
    }

    /**
     * This method is called when we received an UPDATE_SYNC_CFM message. This method starts the next step which is
     * sending an UPDATE_START_REQ message.
     */
    private void receiveSyncCFM(VMUPacket packet) {
        if (DEBUG) Log.d(TAG, "received UPDATE_SYNC_CFM: " + Utils.getStringFromBytes(packet.getData()));
        setResumePoint(ResumePoints.valueOf(packet.getFirstData()));
        if (mResumePoint != null && mResumePoint == ResumePoints.IN_PROGRESS) {
            Log.v(TAG,"Step 3 is successfull");
            stopTimer();
        }
        sendStartReq();
    }

    /**
     * This method is called when we received an UPDATE_START_CFM message. This method read the message and start the
     * next step which is sending an UPDATE_START_DATA_REQ message or abort the update depending on the received
     * message.
     *
     * @param packet
     *            The received packet.
     */
    private void receiveStartCFM(VMUPacket packet) {
        byte[] data = packet.getData();

        if (DEBUG) Log.d(TAG, "received UPDATE_START_CFM: " + Utils.getStringFromBytes(data));

        // the packet has to have a content.
        if (data != null && data.length > 0) {
            if (data[0] == OpCodes.UPDATE_START_CFM_SUCCESS && mResumePoint != null) {
                mStartAttempts = 0;
                // the device is ready for the update, we can go to the resume point or to the update beginning.
                switch (mResumePoint) {
                    case COMMIT:
                        askForCommitCFM();
                        break;
                    case TRANSFER_COMPLETE:
                        if (mActivityListener.showUI()) {
                            askForTransferCompleteRes(mContext);
                        } else {
                            mActivityListener.onTransferComplete();
                        }
                        break;
                    case IN_PROGRESS:
                        sendInProgressRes();
                        break;
                    case VALIDATION:
                        sendValidationDoneReq();
                    case DATA_TRANSFER:
                    default:
                        sendStartDataReq();
                }
            }
            else if (data[0] == OpCodes.UPDATE_START_ERROR_APP_NOT_READY
                    && mStartAttempts < START_ATTEMPTS_MAX) {
                // device not ready we will ask it again.
                mStartAttempts++;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendStartReq();
                    }
                }, START_ATTEMPTS_TIME);
            }
            else {
                // data[0] with a wrong code or too many attempts.
                mStartAttempts = 0;
                displayErrorOrAbort2("The board is not ready to start an update.");
            }
        }
        else {
            // no content on the packet.
            mStartAttempts = 0;
            displayErrorOrAbort2("The board sent a wrong information to start the update, please abort and try again.");
        }
    }

    /**
     * This method is called when we received an UPDATE_DATA_BYTES_REQ message. We manage this packet and use it for the
     * next step which is to upload the file on the device using UPDATE_DATA messages.
     *
     * @param packet
     *            The received packet.
     */
    private void receiveDataBytesReq(VMUPacket packet) {
        byte[] data = packet.getData();

        if (DEBUG) Log.d(TAG, "received UPDATE_DATA_BYTES_REQ: " + Utils.getStringFromBytes(data));

        // manage the UI

        // Checking the data has the good length
        if (data != null && data.length == OpCodes.UPDATE_DATA_BYTES_REQ_LENGTH) {
            double percentage = mStartOffset * 100.0 / mBytesFile.length;
            if (mStartOffset > 0) {
                if (mTimeStartTransfer == 0) {
                    mTimeStartTransfer = Calendar.getInstance().getTimeInMillis();
                }
                long remainingTime = (Calendar.getInstance().getTimeInMillis() - mTimeStartTransfer)
                        * (mBytesFile.length - mStartOffset)
                        / mStartOffset;
                mActivityListener.notifyUpdateProgress(percentage);
                mUpdateDialog.displayTransferProgress(percentage, Utils.getStringFromTime(remainingTime));
            }
            // retrieving information from the received packet
            int bytesLength = Utils.extractIntField(data, 0, Utils.BYTES_IN_INT, false);
            int fileOffset = Utils.extractIntField(data, Utils.BYTES_IN_INT, Utils.BYTES_IN_INT, false);

            // if the asked length doesn't fit with possibilities we use the maximum length we can use.
            if (bytesLength < 0 || bytesLength > MAX_DATA-1) {
                bytesLength = MAX_DATA - 1;
            }

            // we check the value for the offset
            if (fileOffset > 0 && fileOffset+ mStartOffset < mBytesFile.length) {
                mStartOffset += fileOffset;
            }

            // to know if we are sending the last data packet.
            boolean lastPacket = mBytesFile.length- mStartOffset <= bytesLength;

            // we send the data
            byte[] dataToSend = new byte[bytesLength];
            Utils.putArrayField(mBytesFile, mStartOffset, dataToSend, 0, dataToSend.length, false);
            sendData(lastPacket, dataToSend);

            // to reinitialize variables or increment variables
            if (lastPacket) {
                wasLastPacket = true;
            }
            else {
                mStartOffset += bytesLength;
            }
        }
        else {
            displayErrorOrAbort2("The board sent a wrong information during the data transfer, please abort and try again.");
        }
    }

    /**
     * This method is called when we received an UPDATE_IS_VALIDATION_DONE_CFM message. We manage this packet and use it for the
     * next step which is to send an UPDATE_IS_VALIDATION_DONE_REQ.
     */
    private void receiveValidationDoneCFM() {
        if (DEBUG) Log.d(TAG, "received UPDATE_IS_VALIDATION_DONE_CFM");
        sendValidationDoneReq();
    }

    /**
     * This method is called when we received an UPDATE_TRANSFER_COMPLETE_IND message. We manage this packet and use it for the
     * next step which is to send a validation to continue the process or to abort it temporally - it will be done later.
     */
    private void receiveTransferCompleteInd() {
        if (DEBUG) Log.d(TAG, "received UPDATE_TRANSFER_COMPLETE_IND showUI:" + mActivityListener.showUI());
        setResumePoint(ResumePoints.TRANSFER_COMPLETE);
        if (mActivityListener.showUI()) {
            askForTransferCompleteRes(mContext);
        } else {
            mActivityListener.onTransferComplete();
        }
    }

    /**
     * This method is called when we received an UPDATE_COMMIT_RES message. We manage this packet and use it for the
     * next step which is to send a validation to continue the process or to abort it temporally - it will be done later.
     */
    private void receiveCommitReq() {
        if (DEBUG) Log.d(TAG, "received UPDATE_COMMIT_RES");
        setResumePoint(ResumePoints.COMMIT);
        askForCommitCFM();
    }

    /**
     * This method is called when we received an UPDATE_ERASE_SQIF_REQ message. We manage this packet and use it for the
     * next step which is to display a question to the user to continue the process or to abort it temporally - it will be done later.
     */
    private void receiveEraseSQIFReq() {
        if (DEBUG) Log.d(TAG, "received UPDATE_COMMIT_RES");
        setResumePoint(ResumePoints.COMMIT);
        askForEraseSQIFCFM();
    }

    /**
     * This method is called when we received an UPDATE_COMPLETE_IND message. We manage this packet and use it for the
     * next step which is to send a validation to continue the process or to abort it temporally - it will be done later.
     */
    private void receiveCompleteInd() {
        if (DEBUG) Log.d(TAG, "received UPDATE_COMPLETE_IND");
        stopTimer();
        displayAlertComplete();
    }

    /**
     * This method is called when we received an UPDATE_ABORT_CFM message after we asked for an abort to the update process.
     */
    private void receiveAbortCFM() {
        if (DEBUG) Log.d(TAG, "received UPDATE_ABORT_CFM");
        if (isOnlyAborting) {
            onUpdateActivated();
            isOnlyAborting = false;
        }
        else {
            disconnectUpdate();
        }
        mActivityListener.onUpdateAborted();
    }

    /**
     * This method allows the application to ask a confirmation to the user before to continue the process.
     * This method has to be called before to send an UPDATE_TRANSFER_COMPLETE_RES packet.
     */
    public void askForTransferCompleteRes(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.update_alert_transfer_complete)
                .setTitle(R.string.update_alert_transfer_complete_title)
                .setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user wants to process, we start the next step.
                        if (!mUpdateDialog.isShowing()) {
                            mUpdateDialog.show();
                        }
                        sendTransferCompleteReq(true);
                    }
                })
                .setNegativeButton(R.string.alert_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendTransferCompleteReq(false);
                        hasToDisconnect = true;
                        isUpdating = false;
                        mActivityListener.onUserConfirmationDeclined();
                    }
                });
        if(!mUpdateDialog.isShowing()) {
            builder.show();
        }
    }

    /**
     * This method allows the application to ask a confirmation to the user before to continue the process.
     * This method has to be called before to send an UPDATE_IN_PROGRESS_RES packet
     */
    private void askForCommitCFM() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.update_alert_commit)
                .setTitle(R.string.update_alert_commit_title)
                .setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user wants to process, we start the next step.
                        sendCommitCFM(true);
                    }
                })
                .setNegativeButton(R.string.alert_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendCommitCFM(false);
                        hasToDisconnect = true;
                    }
                });
        builder.show();
    }

    /**
     * This method allows the application to ask a confirmation to the user before to continue the process.
     * This method has to be called before to send an UPDATE_IN_PROGRESS_RES packet
     */
    private void askForEraseSQIFCFM() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.update_alert_erase_sqif)
                .setTitle(R.string.update_alert_erase_sqif_title)
                .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user wants to process, we start the next step.
                        sendErasSQIFCFM();
                    }
                });
        builder.show();
    }

    /**
     * To send an UPDATE_SYNC_REQ message.
     */
    private void sendSyncReq () {
        // send the MD5 information here
        byte[] md5Checksum = null;
        md5Checksum = Utils.getMD5FromFile(new File(mUpdateFileUrl));
        Log.v(TAG,"mUpdateFileUrl " + mUpdateFileUrl);
        Log.v(TAG,"md5Checksum " + md5Checksum);
        int lengthData = OpCodes.UPDATE_SYNC_REQ_LENGTH;
        byte[] data = new byte[lengthData];
        // the request only needs to send the last bytes of the md5 checksum
        Utils.putArrayField(md5Checksum, md5Checksum.length-lengthData, data, 0, data.length, false);

        VMUPacket packet = new VMUPacket(OpCodes.UPDATE_SYNC_REQ, lengthData, data);
        byte[] packetBytes = packet.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_SYNC_REQ: " + Utils.getStringFromBytes(packetBytes));
    }

    /**
     * To send an UPDATE_START_REQ message.
     */
    private void sendStartReq () {
        VMUPacket packet = new VMUPacket(OpCodes.UPDATE_START_REQ, 0, null);
        byte[] packetBytes = packet.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_START_REQ: " + Utils.getStringFromBytes(packetBytes));
    }

    /**
     * To send an UPDATE_START_DATA_REQ message.
     */
    private void sendStartDataReq () {
        setResumePoint(ResumePoints.DATA_TRANSFER);
        VMUPacket packet = new VMUPacket(OpCodes.UPDATE_START_DATA_REQ, 0, null);
        byte[] packetBytes = packet.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_START_DATA_REQ: " + Utils.getStringFromBytes(packetBytes));
    }

    /**
     * To send a message to abort the update.
     */
    private void sendAbortReq() {
        VMUPacket packet = new VMUPacket(OpCodes.UPDATE_ABORT_REQ, 0, null);
        byte[] packetBytes = packet.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_ABORT_REQ: " + Utils.getStringFromBytes(packetBytes));
    }

    /**
     * To send an UPDATE_DATA packet.
     *
     * @param lastPacket
     *                  to know if we are sending the last packet for the data.
     * @param data
     *                  the data to send inside this packet.
     */
    private void sendData (boolean lastPacket, byte[] data) {
        byte[] dataToSend = new byte[data.length+1];
        dataToSend[0] = lastPacket ? OpCodes.UPDATE_DATA_LAST_PACKET : OpCodes.UPDATE_DATA_NOT_LAST_PACKET;
        Utils.putArrayField(data, 0, dataToSend, 1, data.length, false);
        VMUPacket packetToSend = new VMUPacket(OpCodes.UPDATE_DATA, dataToSend.length, dataToSend);
        byte[] packetBytes = packetToSend.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_DATA, last packet: " + dataToSend[0] + " - data length: " + data.length);
    }

    /**
     * To send an UPDATE_IS_VALIDATION_DONE_REQ message.
     */
    private void sendValidationDoneReq () {
        VMUPacket packet = new VMUPacket(OpCodes.UPDATE_IS_VALIDATION_DONE_REQ, 0, null);
        byte[] packetBytes = packet.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_IS_VALIDATION_DONE_REQ: " + Utils.getStringFromBytes(packetBytes));
    }

    /**
     * To send an UPDATE_TRANSFER_COMPLETE_RES packet.
     *
     * @param process
     *              To confirm the process should continue, false to abort the process.
     */
    private void sendTransferCompleteReq(boolean process) {
        byte[] data = new byte[1];
        if (process) {
            startTimer();
            data[0] = OpCodes.UPDATE_TRANSFER_COMPLETE_CONTINUE;
        }
        else {
            data[0] = OpCodes.UPDATE_TRANSFER_COMPLETE_ABORT;
        }
        VMUPacket packet = new VMUPacket(OpCodes.UPDATE_TRANSFER_COMPLETE_RES, 1, data);
        byte[] packetBytes = packet.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_TRANSFER_COMPLETE_RES: " + Utils.getStringFromBytes(packetBytes));
    }

    /**
     * To send an UPDATE_IN_PROGRESS_RES packet.
     */
    private void sendInProgressRes() {
        startTimer();
        byte[] data = new byte[1];
        data[0] = OpCodes.UPDATE_IN_PROGRESS_CONTINUE;
        VMUPacket packet = new VMUPacket(OpCodes.UPDATE_IN_PROGRESS_RES, 1, data);
        byte[] packetBytes = packet.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_IN_PROGRESS_RES: " + Utils.getStringFromBytes(packetBytes));
    }

    /**
     * To send an UPDATE_COMMIT_CFM packet.
     *
     * @param process
     *              To confirm the process should continue, false to abort the process.
     */
    private void sendCommitCFM(boolean process) {
        byte[] data = new byte[1];
        if (process) {
            data[0] = OpCodes.UPDATE_COMMIT_CONTINUE;
        }
        else {
            data[0] = OpCodes.UPDATE_COMMIT_ABORT;
        }
        VMUPacket packet = new VMUPacket(OpCodes.UPDATE_COMMIT_CFM, 1, data);
        byte[] packetBytes = packet.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_COMMIT_CFM: " + Utils.getStringFromBytes(packetBytes));
    }

    /**
     * To send an UPDATE_ERASE_SQIF_CFM packet.
     */
    private void sendErasSQIFCFM() {
        VMUPacket packet = new VMUPacket(OpCodes.UPDATE_ERASE_SQIF_CFM, 1, new byte[0]);
        byte[] packetBytes = packet.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_ERASE_SQIF_CFM: " + Utils.getStringFromBytes(packetBytes));
    }

    /**
     * To send an UPDATE_ERROR_WARN_RES packet.
     *
     * @param data
     *              the error code we received and we want to return to the device as an acknowledgment.
     */
    private void sendErrorConfirmation(byte[] data) {
        VMUPacket packet = new VMUPacket(OpCodes.UPDATE_ERROR_WARN_RES, OpCodes.UPDATE_ERROR_WARN_RES_LENGTH, data);
        byte[] packetBytes = packet.getBytes();
        mActivityListener.sendPacket(Gaia.COMMAND_VM_UPGRADE_CONTROL, packetBytes);
        if (DEBUG) Log.d(TAG, "send UPDATE_ABORT_REQ: " + Utils.getStringFromBytes(packetBytes));
    }

    /**
     * When an error occurs this method allows to display the error using the update dialog if it's available or to disconnect directly.
     */
    private void displayErrorOrAbort(String code, String message) {
        if (!mActivityListener.showUI()) {
            Log.v(TAG, code + "::" + message);
            abortUpdate();
            return;
        }

        if (mUpdateDialog.isShowing()) {
            mUpdateDialog.displayError(code, message);
        }
        else {
            abortUpdate();
        }
    }

    /**
     * When an error occurs this method allows to display the error using the update dialog if it's available or to disconnect directly.
     */
    private void displayErrorOrAbort2(String message) {
        if (!mActivityListener.showUI()) {
            Log.v(TAG, message);
            abortUpdate();
            return;
        }
        if (!mUpdateDialog.isShowing()) {
            mUpdateDialog.show();
        }
        mUpdateDialog.displayError(message);
    }

    /**
     * To display an alert when the update completed and succeed.
     */
    private void displayAlertComplete() {
        isUpdating = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.update_complete).setTitle(R.string.update_complete_title)
                .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        disconnectUpdate();
                        mActivityListener.onUpdateComplete();
                    }
                });
        builder.show();
    }

    /**
     * To restart the offset for the upload.
     */
    private void restartOffset() {
        mStartOffset = 0;
        mTimeStartTransfer = 0;
    }

    public void setUpdateFileUrl(String filePath) {
        mUpdateFileUrl = filePath;
    }

    public boolean isVMConnected() {
        return !mIsVMDisconnected;
    }

    /**
     * This interface allows the fragment to communicate information or call the activity on which it is attached.
     */
    public interface IUpdateVMListener {

        /**
         *
         * To send a packet containing a byte array.
         *
         * @param commandId
         *            The Gaia command to send the packet to the device.
         * @param param
         *            Any params to add to the packet.
         */
        void sendPacket(int commandId, int... param);

        /**
         * To send a packet containing a byte array.
         *
         * @param commandId
         *            The Gaia command to send the packet to the device.
         * @param payload
         *            The information to send with the command.
         */
        void sendPacket(@SuppressWarnings("SameParameterValue") int commandId, byte[] payload);

        /**
         * To disconnect the connected board from the application.
         */
        void disconnectDevice();

        /**
         * To register notifications from the connected board.
         *
         * @param event
         *              The event which we want to receive notifications
         */
        void registerForNotifications(@SuppressWarnings("SameParameterValue") Gaia.EventId event);

        /**
         * To unregister from notifications received from the board.
         *
         * @param event
         *              The event which we want to stop to receive notifications.
         */
        void unregisterForNotifications(@SuppressWarnings("SameParameterValue") Gaia.EventId event);


        boolean showUI();

        void onTransferComplete();

        void onUpdateAborted();

        void onUpdateComplete();

        void onUserConfirmationDeclined();

        void displayLowBatteryAlert();

        void notifyUpdateProgress(double percentage);
    }
}

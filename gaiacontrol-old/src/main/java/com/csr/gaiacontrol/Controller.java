package com.csr.gaiacontrol;


import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaLink;
import com.csr.gaiacontrol.internal.ButtonsManager;
import com.csr.gaiacontrol.utils.Utils;

public class Controller {

    private static final String TAG = "Controller";
    private static Controller mController;

    private ButtonsManager mButtonsManager;
    private BluetoothDevice mConnectedDevice;

    private Controller() {
        mButtonsManager = ButtonsManager.getInstance();
    }

    public static Controller getInstance() {
        if (mController == null) {
            mController = new Controller();
        }
        return mController;
    }

    public GaiaLink getGaiaLink() {
        return mButtonsManager.getGaiaLink();
    }

    public void registerListener(Callback callback) {
        mButtonsManager.registerListener(callback);
    }

    public void unRegisterListener(Callback callback) {
        mButtonsManager.unRegisterListener(callback);
    }

    public BluetoothDevice getBluetoothDevice() {
        return mButtonsManager.getBluetoothDevice();
    }

    public void registerNotification(Gaia.EventId eventId) {
        mButtonsManager.registerNotification(eventId);
    }

    public void cancelNotification(Gaia.EventId eventId) {
        mButtonsManager.cancelNotification(eventId);
    }

    public void connect(BluetoothDevice device) {
        mButtonsManager.connect(device);
    }

    public void connect(BluetoothDevice device, GaiaLink.Transport transport) {
        mButtonsManager.connect(device, transport);
    }

    public void purge() {
        if (isGAIAConnected()) {
            mButtonsManager.disconnectDevice();
        }
    }

    private boolean isGAIAConnected() {
        return mButtonsManager.getGaiaLink().isConnected();
    }

    /**
     * To request the Application version from the device.
     */
    public void getAppVersion() {
        if (isGAIAConnected()) {
            mButtonsManager.askForAppVersion();
        }
    }

    /**
     * To request the UUID from the device.
     */
    public void getUUID() {
        if (isGAIAConnected()) {
            mButtonsManager.askForUUID();
        }
    }


    public boolean isConnected() {
        return isGAIAConnected();
    }

    /**
     * To request the SKU from the device.
     */
    public void getSerialNumber() {
        if (isGAIAConnected()) {
            mButtonsManager.askForSerialNumber();
        }
    }


    /**
     * To request the VoiceAssistanntStat from the device.
     */
    public void getVoiceAssistanntState() {
        if (isGAIAConnected()) {
            mButtonsManager.askForVoiceAssistanntState();
        }
    }

    /**
     * To request the getSensoryState from the device.
     */
    public void getSensoryState() {
        if (isGAIAConnected()) {
            mButtonsManager.askForSensoryState();
        }
    }

    /**
     * To request the battery level from the device.
     */
    public void getBatteryLevel() {
        if (isGAIAConnected()) {
            mButtonsManager.askForBatteryLevel();
        }
    }

    /**
     * To request the API version from the device.
     */
    public void getAPIVersion() {
        if (isGAIAConnected()) {
            mButtonsManager.askForAPIVersion();
        }
    }

    /**
     * To request the RSSI level from the device.
     */
    public void getRSSILevel() {
        if (isGAIAConnected()) {
            mButtonsManager.askForRSSILevel();
        }
    }

    public void sendCommand(int commandId, int... param) {
        mButtonsManager.sendGaiaPacket(commandId, param);
    }

    public void sendCommand(int commandId, byte[] payload) {
        mButtonsManager.sendGaiaPacket(commandId, payload);
    }

    public void sendIamplusCommand(int commandId, int payload) {
        mButtonsManager.sendGaiaIamplusPacket(commandId, payload);
    }

    public void disconnect() {
        mButtonsManager.disconnectDevice();
    }

    public void registerUpdateCallbacks(VMUpdateCallback callback) {
        mButtonsManager.registerUpdateCallback(callback);
    }

    public GaiaLink.Transport getTransport() {
        return mButtonsManager.getTransport();
    }

    public BluetoothDevice getConnectedDevice() {
        return mConnectedDevice;
    }

    public void setConnectedDevice(BluetoothDevice connectedDevice) {
        mConnectedDevice = connectedDevice;
    }

    public void establishGAIAConnection() {
        if (isConnected()){
            Log.v(TAG,"Already connceted to giacontrol");
            return;
        }

        BluetoothDevice device = getConnectedDevice() == null ? Utils.getConnectedDeviceMAC(): getConnectedDevice();
        if (device != null) {
            Log.v(TAG,"connecting to giacontrol "+ device.getName());
            connect(device);
        } else{
            disconnect();
            Log.v(TAG, "No headset connected");
        }
    }
}

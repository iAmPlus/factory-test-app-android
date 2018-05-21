package com.csr.gaiacontrol.internal;


import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaLink;
import com.csr.gaia.library.GaiaPacket;
import com.csr.gaiacontrol.Callback;
import com.csr.gaiacontrol.VMUpdateCallback;
import com.csr.gaiacontrol.utils.Consts;
import com.csr.gaiacontrol.utils.Utils;

import java.util.ArrayList;

public class ButtonsManager {

    private static ButtonsManager mButtonsManager;

    private ArrayList<Callback> mListener = new ArrayList<Callback>();
    /**
     * To know if we are using the application in the debug mode.
     */
    static final boolean DEBUG = Consts.DEBUG;
    /**
     * Instance of the object used to communicate with the GAIA device.
     */
    GaiaLink mGaiaLink;

    /**
     * The device to attempt to connect using GaiaLink.
     */
    private BluetoothDevice mDevice;

    /**
     * The profile to use to connect to a device.
     */
    private GaiaLink.Transport mTransport;
    /**
     * To know if the user is waiting for a connection.
     */
    private boolean mWaitingForConnection = false;

    /**
     * To manage exception when the app is already connected to a device: to stop when we tried more than a certain
     * number of attempts.
     */
    private int nbAttemptConnection = 0;
    /**
     * To know if the attempted connection is using SPP as the transport.
     */
    private boolean isAttemptingSPP = false;
    /**
     * To know if the attempted connection is using GAIA UUID as the transport.
     */
    private boolean iSAttemptingGAIAUUID = false;
    /**
     * The maximum number of attempts when a device is already connected.
     */
    private static final int NB_ATTEMPTS_CONNECTION_MAX = 2;

    private String TAG = "ButtonsManager";
    private VMUpdateCallback mVMUpdateCallback;
    private Handler mGaiaHandler = new GaiaHandler();

    private ButtonsManager() {
        init();
    }

    public static ButtonsManager getInstance() {
        if (mButtonsManager == null) {
            mButtonsManager = new ButtonsManager();
        }
        return mButtonsManager;
    }

    public void registerListener(Callback listener) {
        addListener(listener);
    }

    public void registerUpdateCallback(VMUpdateCallback callback) {
        mVMUpdateCallback = callback;
    }

    private void addListener(Callback listener) {
        boolean already_added = false;

        for (Callback callback : mListener) {
            if (callback == listener) {
                already_added = true;
                break;
            }
        }
        if (!already_added) {
            mListener.add(listener);
            if (mGaiaLink.isConnected()) {
                listener.onConnected();
            }
        }
    }


    private void init() {
        mGaiaLink = GaiaLink.getInstance();
        mGaiaLink.setReceiveHandler(getGaiaHandler());
    }

    public GaiaLink getGaiaLink() {
        return mGaiaLink;
    }

    /**
     * To request the battery level from the device.
     */
    public void askForBatteryLevel() {
        sendGaiaPacket(Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL);
    }

    /**
     * To request the API version from the device.
     */
    public void askForAPIVersion() {
        sendGaiaPacket(Gaia.COMMAND_GET_API_VERSION);
    }

    /**
     * To request the RSSI level from the device.
     */
    public void askForRSSILevel() {
        sendGaiaPacket(Gaia.COMMAND_GET_CURRENT_RSSI);
    }

    /**
     * To request the Application version from the device.
     */
    public void askForAppVersion() { sendGaiaPacket(Gaia.COMMAND_GET_APPLICATION_VERSION); }

    /**
     * To request the UUID from the device.
     */
    public void askForUUID() { sendGaiaIamplusPacket(Gaia.COMMAND_GET_UUID); }

    /**
     * To request the SerialNumber from the device.
     */
    public void askForSerialNumber() { sendGaiaIamplusPacket(Gaia.COMMAND_GET_SERIAL_NUMBER); }

    /**
     * To request the Voice Assistant state from the device.
     */
    public void askForVoiceAssistanntState() {
        sendGaiaIamplusPacket(Gaia.COMMAND_SET_VOICE_ASSISTANT_CONFIG);
    }

    /**
     * To request the Sensory state from the device.
     */
    public void askForSensoryState() {
        sendGaiaIamplusPacket(Gaia.COMMAND_SET_SENSORY_CONFIG);
    }

    /**
     * To send a packet to the device.
     *
     * @param command
     *            the command to send to the device.
     * @param payload
     *            the additional information for the command.
     */
    @SuppressWarnings("SameParameterValue")
    void sendGaiaIamplusPacket(int command, byte... payload) {
        mGaiaLink.sendCommand(Gaia.VENDOR_IAMPLUS, command, payload);
    }

    /**
     * To send a packet to the device.
     *
     * @param command
     *            the command to send to the device.
     * @param payload
     *            the additional information for the command.
     */
    public void sendGaiaPacket(int command, int... payload) {
        mGaiaLink.sendCommand(Gaia.VENDOR_CSR, command, payload);
    }

    /**
     * To send a packet to the device.
     *
     * @param command
     *            the command to send to the device.
     * @param payload
     *            the additional information for the command.
     */
    @SuppressWarnings("SameParameterValue")
    public void sendGaiaPacket(int command, byte... payload) {
        mGaiaLink.sendCommand(Gaia.VENDOR_CSR, command, payload);
    }

    /**
     * To send a packet to the device.
     *
     * @param command
     *            the command to send to the device.
     * @param payload
     *            the additional information for the command.
     */
    public void sendGaiaIamplusPacket(int command, int... payload) {
        mGaiaLink.sendCommand(Gaia.VENDOR_IAMPLUS, command, payload);
    }

    /**
     * To send a packet to the device.
     *
     * @param command
     *            the command to send to the device.
     * @param payload
     *            the additional information for the command.
     */
    public void sendGaiaPacket(int command, boolean payload) {
        mGaiaLink.sendCommand(Gaia.VENDOR_CSR, command, payload);
    }

    /**
     * To register for a notification on a GAIA Bluetooth device.
     *
     * @param eventID
     *          The event for which we want to be notified.
     */
    public void registerNotification(Gaia.EventId eventID) {
        mGaiaLink.registerNotification(Gaia.VENDOR_CSR, eventID);
    }

    /**
     * To cancel a notification on a GAIA Bluetooth device.
     *
     * @param eventID
     *          The event for which we want to cancel the notification.
     */
    public void cancelNotification(Gaia.EventId eventID) {
        mGaiaLink.cancelNotification(Gaia.VENDOR_CSR, eventID);
    }

    /**
     * To handle the message providing by the device using GAIA communication.
     */
    private Handler getGaiaHandler () {
        return mGaiaHandler;
    }

    /**
     * To manage packets from Gaia device which are "PACKET" directly by the library.
     *
     * @param msg
     *            The message coming from the handler which calls this method.
     */
    private void handlePacket(Message msg) {
        GaiaPacket packet = (GaiaPacket) msg.obj;
        Gaia.Status status = packet.getStatus();
        boolean validate;

        if (packet.getVendorId() == Gaia.VENDOR_CSR) {
            switch (packet.getCommand()) {
                case Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL:
                    validate = checkStatus(packet);
                    if (DEBUG)
                        Log.i(TAG, "Received \"COMMAND_GET_CURRENT_BATTERY_LEVEL\" packet with a " + validate + " status.");
                    receiveGetCurrentBatteryLevel(packet);
                    break;

                case Gaia.COMMAND_GET_CURRENT_RSSI:
                    validate = checkStatus(packet);
                    if (DEBUG)
                        Log.i(TAG, "Received \"COMMAND_GET_CURRENT_RSSI\" packet with a " + validate + " status.");
                    receiveGetCurrentRSSI(packet);
                    break;

                case Gaia.COMMAND_GET_API_VERSION:
                    validate = checkStatus(packet);
                    if (DEBUG)
                        Log.i(TAG, "Received \"COMMAND_GET_API_VERSION\" packet with a " + validate + " status.");
                    //receiveGetAPIVersion(packet);
                    break;
                case Gaia.COMMAND_EVENT_NOTIFICATION:
                    if (DEBUG)
                        Log.i(TAG, "Received \"Notification\" packet.");
                    handleNotification(packet);
                    break;
                case Gaia.COMMAND_GET_APPLICATION_VERSION:
                    validate = checkStatus(packet);
                    if (DEBUG)
                        Log.i(TAG, "Received \"COMMAND_GET_APPLICATION_VERSION\" packet with a " + validate + " status.");
                    receiveGetAPPVersion(packet);
                    break;
                case Gaia.COMMAND_VM_UPGRADE_CONNECT:
                    validate = checkStatus(packet);
                    if (DEBUG) Log.i(TAG, "Received \"VM connection\" packet with a " + validate + " status.");
                    if (validate) {
                        notifyUpdateActivated();
                    } else {
                        notifyUpdateActivatedFailed();
                    }
                    break;
                case Gaia.COMMAND_VM_UPGRADE_DISCONNECT:
                    validate = checkStatus(packet);
                    if (DEBUG) Log.i(TAG, "Received \"VM disconnection\" packet with a " + validate + " status.");
                    notifyVMDisconnected();
                    break;
                case Gaia.COMMAND_VM_UPGRADE_CONTROL:
                    validate = checkStatus(packet);
                    if (DEBUG) Log.i(TAG, "Received \"VM Control\" packet with a " + validate + " status.");
                    if (validate) {
                        notifyVMControlSucceed();
                    } else {
                        notifyVMControlFailed();
                    }
                    break;
                default:
                    notifyHandlePacket(packet);
                    break;
            }
        } else if (packet.getVendorId() == Gaia.VENDOR_IAMPLUS) {
            switch (packet.getCommandId() & 0x0FFF) {
                case Gaia.COMMAND_GET_UUID:
                    validate = checkStatus(packet);
                    if (DEBUG)
                        Log.i(TAG, "Received IAMPLUS vendor \"COMMAND_GET_UUID\" packet with a " + validate + " status.");
                    receiveGetUUID(packet);
                    break;

                case Gaia.COMMAND_GET_SERIAL_NUMBER:
                    validate = checkStatus(packet);
                    if (DEBUG)
                        Log.i(TAG, "Received IAMPLUS vendor \"COMMAND_GET_SERIAL_NUMBER\" packet with a " + validate + " status.");
                    receiveGetSerialNumber(packet);
                    break;
                case Gaia.COMMAND_SET_VOICE_ASSISTANT_CONFIG:
                    if (DEBUG)
                        Log.i(TAG, "Received \"COMMAND_SET_VOICE_ASSISTANT_CONFIG\" packet with a " + status + " status.");
                    if (checkStatus(packet))
                        receiveSetVoiceAssistantConfig(packet);
                    break;
                case Gaia.COMMAND_SET_SENSORY_CONFIG:
                    if (DEBUG)
                        Log.i(TAG, "Received \"COMMAND_SET_SENSORY_CONFIG\" packet with a " + status + " status.");
                    if (checkStatus(packet))
                        receiveSetSensoryConfig(packet);
                    break;
                default:
                    if (DEBUG)
                        Log.d(TAG, "Received packet -Vendor IAMPLUS command: " + Utils.getIntToHexadecimal(packet.getCommandId())
                                + " - payload: " + Utils.getStringFromBytes(packet.getPayload()));
                    notifyHandlePacket(packet);
                    break;
            }
        }
    }

    private void notifyHandlePacket(GaiaPacket packet) {
        for (Callback callback : mListener) {
            callback.handlePacket(packet);
        }
    }

    private void notifyVMControlFailed() {
        if (mVMUpdateCallback != null) {
            mVMUpdateCallback.onVMControlFailed();
        }
    }

    private void notifyVMControlSucceed() {
        if (mVMUpdateCallback != null) {
            mVMUpdateCallback.onVMControlSucceed();
        }
    }

    private void notifyVMDisconnected() {
        if (mVMUpdateCallback != null) {
            mVMUpdateCallback.onVMDisconnected();
        }
    }

    private void notifyUpdateActivatedFailed() {
        if (mVMUpdateCallback != null) {
            mVMUpdateCallback.onUpdateActivatedFailed();
        }
    }

    private void notifyUpdateActivated() {
        if (mVMUpdateCallback != null) {
            mVMUpdateCallback.onUpdateActivated();
        }
    }

    /**
     * Called when we receive a packet about the command COMMAND_GET_CURRENT_BATTERY_LEVEL to manage the application
     * depending on information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_GET_CURRENT_BATTERY_LEVEL.
     */
    private void receiveGetCurrentBatteryLevel(GaiaPacket packet) {
        if (checkStatus(packet)) {
            int level = Utils.extractIntField(packet.getPayload(), 1, 2, false);
            notifyBatteryLevel(level);
        }
    }

    private void notifyBatteryLevel(int level) {
        for (Callback callback : mListener) {
            callback.onGetBatteryLevel(level);
        }
    }

    /**
     * Called when we receive a packet about the command COMMAND_GET_CURRENT_RSSI to manage the application depending on
     * information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_GET_CURRENT_RSSI.
     */
    private void receiveGetCurrentRSSI(GaiaPacket packet) {
        if (checkStatus(packet)) {
            int level = packet.getByte(1);
            notifyGetRSSILevel(level);
        }
    }

    private void notifyGetRSSILevel(int level) {
        for (Callback callback : mListener) {
            callback.onGetRSSILevel(level);
        }
    }

    /**
     * Called when we receive a packet about the command COMMAND_GET_CURRENT_RSSI to manage the application depending on
     * information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_GET_CURRENT_RSSI.
     */
    /*private void receiveGetAPIVersion(GaiaPacket packet) {
        String apiVersion = null;
        if (checkStatus(packet)) {
            apiVersion = packet.getByte(1) + "." + packet.getByte(2) + "."
                    + packet.getByte(3);
			notifyGetApiVersion(apiVersion);
        }
        
    }

    private void notifyGetApiVersion(String apiVersion) {
        for (Callback callback : mListener) {
            callback.onGetAPIVersion(apiVersion);
        }
    }*/

    /**
     * Called when we receive a packet about the command COMMAND_GET_APPLICATION_VERSION to manage the application depending on
     * information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_GET_APPLICATION_VERSION.
     */
    private void receiveGetAPPVersion(GaiaPacket packet) {
        if (checkStatus(packet)) {
            StringBuilder sb = new StringBuilder();
            StringBuilder majorVer = new StringBuilder();
            StringBuilder minorVer = new StringBuilder();
            char c1;
            char c2;
            for (int i = 1; i < packet.getPayload().length; i++) {
                c1 = Character.forDigit((packet.getByte(i) >> 4) & 0xF, 16);
                c2 = Character.forDigit((packet.getByte(i) & 0xF), 16);
                sb.append(c1);
                sb.append(c2);

                if (i>=9 && i<=12) {
                    // Major version
                    majorVer.append(c1);
                    majorVer.append(c2);
                } else if (i>=13 && i<=16) {
                    // Minor version
                    minorVer.append(c1);
                    minorVer.append(c2);
                }
            }
            int major = Integer.parseInt(majorVer.toString(), 16);
            int minor = Integer.parseInt(minorVer.toString(), 16);
            notifyAppVersionReceived(Integer.toString(major) + "." + Integer.toString(minor));
            Log.i(TAG, "App version Full key: " + sb.toString().toUpperCase());
        }
    }

    private void notifyAppVersionReceived(String version) {
        for (Callback callback : mListener) {
            callback.onGetAppVersion(version);
        }
    }

    /**
     * Called when we receive a packet about the command COMMAND_GET_UUID to manage the application depending on
     * information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_GET_UUID.
     */
    private void receiveGetUUID(GaiaPacket packet) {
        Log.v(TAG,"receiveGetUUID " + packet.getPayload().length);
        if (checkStatus(packet)) {
            // Convert bytes to Hex digits
            //d568025e-673b-45c3-9f70-a8a695913580
            StringBuilder sb = new StringBuilder();
            for (int i=1; i< packet.getPayload().length; i++) {
                sb.append(Character.forDigit((packet.getByte(i) >> 4) & 0xF, 16));
                sb.append(Character.forDigit((packet.getByte(i) & 0xF), 16));

                if (i == 4 || i == 6 || i == 8) {
                    sb.append("-");
                }
            }
            notifyGetUUID(sb.toString().toUpperCase());
            Log.i(TAG,"UUID: " + sb.toString().toUpperCase());
        }
    }

    private void notifyGetUUID(String uuid) {
        for (Callback callback : mListener) {
            callback.onGetUUID(uuid);
        }
    }

    /**
     * Called when we receive a packet about the command COMMAND_GET_SERIAL_NUMBER to manage the application depending on
     * information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_GET_SERIAL_NUMBER.
     */

    private void receiveGetSerialNumber(GaiaPacket packet) {
        if (checkStatus(packet)) {
            // Convert bytes to string, two bytes = one char
            int decimal;
            StringBuilder sb = new StringBuilder();
            StringBuilder sn = new StringBuilder();
            for (int i=1; i<packet.getPayload().length; i++) {

                /*if (packet.getByte(i) == 0) {
                    continue;
                }*/
                sb.append(Character.forDigit((packet.getByte(i) >> 4) & 0xF, 16));
                sb.append(Character.forDigit((packet.getByte(i) & 0xF), 16));

                decimal = Integer.parseInt(sb.toString(), 16);
                sn.append((char)decimal);
                sb.delete(0, 4);
            }
            notifyGetSerialNumber(sn.toString().toUpperCase());
            Log.i(TAG, "Serial Number: " + sn.toString().toUpperCase());
        }
    }

    private void notifyGetSerialNumber(String sn) {
        for (Callback callback : mListener) {
            callback.onGetSerialNumber(sn);
        }
    }

    /**
     * Called when we receive a packet about the command COMMAND_SET_VOICE_ASSISTANT_CONFIG to manage the application depending on
     * information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_SET_VOICE_ASSISTANT_CONFIG.
     */
    private void receiveSetVoiceAssistantConfig(GaiaPacket packet) {
        notifySetVoiceAssistantConfig(packet.getByte(2) == 1);
    }

    private void notifySetVoiceAssistantConfig(boolean config) {
        Log.i(TAG, "VoiceAssistantConfig: " + config);
        for (Callback callback : mListener) {
            callback.onSetVoiceAssistantConfig(config);
        }
    }


    /**
     * Called when we receive a packet about the command COMMAND_SET_SENSORY_CONFIG to manage the application depending on
     * information from the packet.
     *
     * @param packet
     *            The packet about the received command COMMAND_SET_SENSORY_CONFIG.
     */
    private void receiveSetSensoryConfig(GaiaPacket packet) {
        notifySetSensoryConfig(packet.getByte(2) == 1);
    }
    private void notifySetSensoryConfig(boolean config) {
        Log.i(TAG, "notifySetSensoryConfig: " + config);
        for (Callback callback : mListener) {
            callback.onSetSensoryConfig(config);
        }
    }

    /**
     * To check the status of an acknowledgement packet.
     *
     * @param packet
     *            the packet to check.
     *
     * @return true if the status is SUCCESS and the packet is an acknowledgment, false otherwise.
     */
    private boolean checkStatus(GaiaPacket packet) {
        if (!packet.isAcknowledgement()) {
            return false;
        }
        switch (packet.getStatus()) {
            case SUCCESS:
                return true;
            case NOT_SUPPORTED:
                receivePacketCommandNotSupported(packet);
                break;
            case AUTHENTICATING:
            case INCORRECT_STATE:
            case INSUFFICIENT_RESOURCES:
            case INVALID_PARAMETER:
            case NOT_AUTHENTICATED:
            default:
                if (DEBUG)
                    Log.w(TAG, "Status " + packet.getStatus().toString() + " with the command " + packet.getCommand());
        }
        return false;
    }

    /**
     * When we received a packet about a command which is not supported by the device.
     *
     * @param packet
     *            the concerned packet.
     */
    private void receivePacketCommandNotSupported(GaiaPacket packet) {


        if (packet.getVendorId() == Gaia.VENDOR_IAMPLUS) {
            switch (packet.getCommand()) {

                case Gaia.COMMAND_GET_APPLICATION_VERSION:
                    if (DEBUG)
                        Log.w(TAG, "Received \"COMMAND_GET_APPLICATION_VERSION\" not supported.");
                    break;

                case Gaia.COMMAND_GET_UUID:
                    if (DEBUG)
                        Log.w(TAG, "Received \"COMMAND_GET_UUID\" not supported.");
                    break;

                case Gaia.COMMAND_GET_SERIAL_NUMBER:
                    if (DEBUG)
                        Log.w(TAG, "Received \"COMMAND_GET_SERIAL_NUMBER\" not supported.");
                    break;
            }
        } else {
            switch (packet.getCommand()) {
                case Gaia.COMMAND_EVENT_NOTIFICATION:
                    if (DEBUG)
                        Log.w(TAG, "Received \"COMMAND_EVENT_NOTIFICATION\" not supported.");
                    break;
                case Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL:
                    if (DEBUG)
                        Log.w(TAG, "Received \"COMMAND_GET_CURRENT_BATTERY_LEVEL\" not supported.");
                    break;

                case Gaia.COMMAND_GET_CURRENT_RSSI:
                    if (DEBUG)
                        Log.w(TAG, "Received \"COMMAND_GET_CURRENT_RSSI\" not supported.");
                    break;

                case Gaia.COMMAND_GET_API_VERSION:
                    if (DEBUG)
                        Log.w(TAG, "Received \"COMMAND_GET_API_VERSION\" not supported.");
                    break;
            }
            }
    }

    /**
     * To handle notifications coming from the Gaia device.
     */
    private void handleNotification(GaiaPacket packet) {
        Gaia.EventId event = packet.getEvent();
        switch (event) {
            case VMU_PACKET:
                notifyHandlerVMEvent(packet);
                return;

            default:
                if (DEBUG) Log.i(TAG, "Received event: " + event);
        }
        notifyHandleNotification(packet);
    }

    private void notifyHandlerVMEvent(GaiaPacket packet) {
        if (mVMUpdateCallback != null) {
            mVMUpdateCallback.handlerVMEvent(packet);
        }
    }

    private void notifyHandleNotification(GaiaPacket packet) {
        for (Callback callback : mListener) {
            callback.handleNotification(packet);
        }
    }

    public void unRegisterListener(Callback callback) {
        removeListener(callback);
    }

    private void removeListener(Callback listener) {
        for (Callback callback : mListener) {
            if (callback == listener) {
                mListener.remove(listener);
                break;
            }
        }
    }

    public BluetoothDevice getBluetoothDevice() {
        return mGaiaLink.getBluetoothDevice();
    }

    public GaiaLink.Transport getTransport() {
        return mGaiaLink.getTransport();
    }


    private class GaiaHandler extends Handler {

        public GaiaHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            String handleMessage = "Handle a message from Gaia: ";
            GaiaLink.Message message = GaiaLink.Message.valueOf(msg.what);
            if (message == null) {
                return;
            }
            switch (message) {
                case PACKET:
                    handlePacket(msg);
                    break;
                case CONNECTED:
                    if (DEBUG)
                        Log.v(TAG, handleMessage + " CONNECTED");
                    isAttemptingSPP = false;
                    iSAttemptingGAIAUUID = false;
                    notifyOnConnected();
                    break;
                case DISCONNECTED:
                    if (DEBUG)
                        Log.d(TAG, handleMessage + "DISCONNECTED");
                    if (mWaitingForConnection) {
                        isAttemptingSPP = false;
                        iSAttemptingGAIAUUID = false;
                        connectDevice();
                        mWaitingForConnection = false;
                    }
                    notifyOnDisconnected();
                    break;

                case ERROR:
                    if (DEBUG)
                        Log.d(TAG, handleMessage + "ERROR");
                    GaiaError error = (GaiaError) msg.obj;
                    handleError(error);
                    break;

                case STREAM:
                    if (DEBUG)
                        Log.d(TAG, handleMessage + "STREAM");
                    break;

                default:
                    if (DEBUG)
                        Log.d(TAG, handleMessage + "UNKNOWN MESSAGE: " + msg);
                    break;
            }
        }
    }

    private void notifyOnDisconnected() {
        for (Callback callback : mListener) {
            callback.onDisconnected();
        }

        if (mVMUpdateCallback != null) {
            mVMUpdateCallback.onDisconnected();
        }
    }

    private void notifyOnConnected() {
        for (Callback callback : mListener) {
            callback.onConnected();
        }

        if (mVMUpdateCallback != null) {
            mVMUpdateCallback.onConnected();
        }
    }

    /**
     * To manage errors information catching inside the handler and coming from the library.
     *
     * @param error
     *            The error coming from the library, formatting as a <code>GaiaError<</code>.
     */
    private void handleError(GaiaError error) {
        if (isAttemptingSPP) {
            connect(mDevice);
        } else {
            isAttemptingSPP = false;
            iSAttemptingGAIAUUID = false;
        }
        notifyOnError(error);

    }

    private void notifyOnError(GaiaError error) {
        for (Callback callback : mListener) {
            callback.onError(error);
        }

        if (mVMUpdateCallback != null) {
            mVMUpdateCallback.onError(error);
        }
    }

    public void connect(BluetoothDevice device, GaiaLink.Transport transport) {
        mTransport = transport;
        mDevice = device;
        if (!mGaiaLink.isConnected()) {
            connectDevice();
        }
    }

    public void connect(BluetoothDevice device) {
        if (!isAttemptingSPP && !iSAttemptingGAIAUUID) {
            isAttemptingSPP = true;
            mTransport = GaiaLink.Transport.BT_SPP;
        }
        else if (isAttemptingSPP) {
            isAttemptingSPP = false;
            iSAttemptingGAIAUUID = true;
            mTransport = GaiaLink.Transport.BT_GAIA;
        }
        mDevice = device;

        if (mGaiaLink.isConnected()) {
            mWaitingForConnection = true;
            disconnectDevice();
        }
        else {
            mWaitingForConnection = false;
            connectDevice();
        }
    }

    /**
     * To start the connection process.
     */
    public void connectDevice() {
        mGaiaLink.connect(mDevice, mTransport);
    }

    /**
     * To start the disconnection process.
     */
    public void disconnectDevice() {
        mGaiaLink.disconnect();
    }
}

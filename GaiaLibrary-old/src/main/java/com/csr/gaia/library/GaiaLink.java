/******************************************************************************
 *  Copyright (C) Cambridge Silicon Radio Limited 2015
 *
 *  This software is provided to the customer for evaluation
 *  purposes only and, as such early feedback on performance and operation
 *  is anticipated. The software source code is subject to change and
 *  not intended for production. Use of developmental release software is
 *  at the user's own risk. This software is provided "as is," and CSR
 *  cautions users to determine for themselves the suitability of using the
 *  beta release version of this software. CSR makes no warranty or
 *  representation whatsoever of merchantability or fitness of the product
 *  for any particular purpose or use. In no event shall CSR be liable for
 *  any consequential, incidental or special damages whatsoever arising out
 *  of the use of or inability to use this software, even if the user has
 *  advised CSR of the possibility of such damages.
 *
 ******************************************************************************/
package com.csr.gaia.library;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.util.Log;

import com.csr.gaia.library.Gaia.Status;
import com.csr.gaia.library.exceptions.GaiaFrameException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * This class is the main manager to communicate with Gaia devices.
 */

@SuppressWarnings("unused")
public class GaiaLink {

    /**
     * All types of interaction the Android device has with the GAIA device.
     */
    public enum Message {
        /**
         * When we received a GAIA packet from the GAIA device to broadcast to the application.
         */
        PACKET,
        /**
         * To inform the application the GAIA device is now connected to the Android device.
         */
        CONNECTED,
        /**
         * To inform the application an error occurs on the communication with the GAIA device.
         */
        ERROR,
        /**
         * To inform the application about a message for debug.
         */
        DEBUG,
        /**
         * To inform the application the GAIA device is now disconnected to the Android device.
         */
        DISCONNECTED,
        /**
         * To inform the application the library is streaming messages to the GAIA device.
         */
        STREAM;

        private static final Message[] values = Message.values();

        public static Message valueOf(int what) {

            if (what < 0 || what >= values.length)
                return null;

            return values[what];
        }
    }

    /**
     * All types of transports which could be used to communicate with the device.
     */
    public enum Transport {
        BT_SPP, BT_GAIA
    }

    // End of public fields

    private static final String TAG = "GaiaLink";
    private static boolean mDebug = true;

    @SuppressWarnings("FieldCanBeLocal")
    private final int MAX_BUFFER = 1024;
    private boolean mVerbose = true;
    private BluetoothAdapter mBTAdapter;

    private Handler mReceiveHandler = null;

    public Transport mTransport = Transport.BT_GAIA;
    private boolean mIsConnected = false;

    /**
     * Instance of this object.
     */
    private static GaiaLink mInstance;

    /**
     * To retrieve the instance for this class.
     *
     * @return The instance for the GaiaLink class.
     */
    public static synchronized GaiaLink getInstance() {
        if (mInstance == null) {
            mInstance = new GaiaLink();
        }
        return mInstance;
    }

    /**
     * Class constructor.<br/> Use the getInstance method.
     */
    private GaiaLink() {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Returns the used transport.
     * 
     * @return The transport used to connect to a device.
     */
    public Transport getTransport() {
        return mTransport;
    }

    /**
     * Establishes an outbound connection to the specified device.
     * 
     * @param device
     *            Bluetooth or IP address of the remote device depending on the transport.
     *
     */
    public void connect(BluetoothDevice device, Transport transport) {
        connect(device);
    }

    /**
     * Sends unframed data to the remote device
     *
     * @param buffer
     *            The data to send
     */
    public void sendRaw(byte[] buffer) throws IOException {
        sendRaw(buffer, buffer.length);
    }

    /**
     * Sends unframed data to the remote device
     *
     * @param buffer
     *            The data to send
     * @param count
     *            The number of bytes to send
     */
    public void sendRaw(byte[] buffer, int count) throws IOException {
        sendData(buffer);
    }

    /**
     * Sends a Gaia command to the remote device.
     * 
     * @param vendorId
     *            The vendor identifier qualifying the command.
     * @param commandId
     *            The command identifier.
     * @param payload
     *            Array of command-specific bytes.
     * @param payloadLength
     *            The number of payload bytes to send.
     */
    @SuppressWarnings("WeakerAccess")
    public void sendCommand(int vendorId, int commandId, byte[] payload, int payloadLength) {
        byte[] data;
        try {
            data = Gaia.frame(vendorId, commandId, payload, payloadLength);
            //noinspection ConstantConditions
            String text = "\u2192 " + Gaia.hexw(vendorId) + " " + Gaia.hexw(commandId);
            for (byte aPayload : payload) {
                text += " " + Gaia.hexb(aPayload);
            }
            if (mDebug)
                Log.d(TAG, text);
            sendCommandData(data, commandId);
        }
        catch (GaiaFrameException e) {
            handleException("sendCommand", GaiaError.TypeException.SENDING_FAILED, e, commandId);
        }
    }

    /**
     * Sends a Gaia command to the remote device.
     * 
     * @param vendorId
     *            The vendor identifier qualifying the command.
     * @param commandId
     *            The command identifier.
     * @param payload
     *            Array of command-specific bytes.
     */
    public void sendCommand(int vendorId, int commandId, byte[] payload) {
        if (payload == null)
            sendCommand(vendorId, commandId);

        else
            sendCommand(vendorId, commandId, payload, payload.length);
    }

    /**
     * Sends a Gaia command to the remote device.
     * 
     * @param vendorId
     *            The vendor identifier qualifying the command.
     * @param commandId
     *            The command identifier.
     * @param param
     *            Command-specific integers.
     */
    public void sendCommand(int vendorId, int commandId, int... param) {
        if (param == null || param.length == 0) {
            byte[] data;
            try {
                data = Gaia.frame(vendorId, commandId);
                sendCommandData(data, commandId);
            }
            catch (GaiaFrameException e) {
                handleException("sendCommand", GaiaError.TypeException.SENDING_FAILED, e, commandId);
            }
        }

        else {
            // Convenient but involves copying the payload twice. It's usually short.
            byte[] payload;
            payload = new byte[param.length];

            for (int idx = 0; idx < param.length; ++idx)
                payload[idx] = (byte) param[idx];

            sendCommand(vendorId, commandId, payload);
        }
    }

    /**
     * Sends a Gaia enable-style command to the remote device.
     * 
     * @param vendorId
     *            The vendor identifier qualifying the command.
     * @param commandId
     *            The command identifier.
     * @param enable
     *            Enable (true) or disable (false).
     */
    @SuppressWarnings("SameParameterValue")
    public void sendCommand(int vendorId, int commandId, boolean enable) {
        sendCommand(vendorId, commandId, enable ? Gaia.FEATURE_ENABLED : Gaia.FEATURE_DISABLED);
    }

    /**
     * Sends a Gaia acknowledgement to the remote device.
     * 
     * @param vendorId
     *            The vendor identifier qualifying the command.
     * @param commandId
     *            The command identifier.
     * @param status
     *            The status of the command.
     * @param param
     *            Acknowledgement-specific integers.
     */
    @SuppressWarnings("WeakerAccess")
    public void sendAcknowledgement(int vendorId, int commandId, Gaia.Status status, int... param) {
        // Convenient but involves copying the payload twice. It's usually short.
        byte[] payload;

        if (param == null)
            payload = new byte[1];

        else {
            payload = new byte[param.length + 1];

            for (int idx = 0; idx < param.length; ++idx)
                payload[idx + 1] = (byte) param[idx];
        }

        payload[0] = (byte) status.ordinal();
        sendCommand(vendorId, commandId | Gaia.ACK_MASK, payload);
    }

    /**
     * Sends a Gaia acknowledgement to the remote device.
     *
     * @param packet
     *            The packet which contains the vendorId and the command id for this acknowledgment.
     * @param status
     *            The status of the command.
     */
    public void sendAcknowledgement(GaiaPacket packet, Status status) {
        sendAcknowledgement(packet.getVendorId(), packet.getCommandId(), status);
    }

    /**
     * Sends a Gaia acknowledgement to the remote device.
     *
     * @param packet
     *            The packet which contains the vendorId and the command id for this acknowledgment.
     * @param status
     *            The status of the command.
     *
     * @param payload
     *            Any complementary information for this acknowledgment packet.
     */
    public void sendAcknowledgement(GaiaPacket packet, Status status, int... payload) {
        sendAcknowledgement(packet.getVendorId(), packet.getCommandId(), status, payload);
    }

    /**
     * Requests notification of the given event
     * 
     * @param event
     *            The Event for which notifications are to be raised
     */
    @SuppressWarnings("SameParameterValue")
    public void registerNotification(int vendorID, Gaia.EventId event) throws IllegalArgumentException {
        byte[] args;

        switch (event) {
        case START:
        case DEVICE_STATE_CHANGED:
        case DEBUG_MESSAGE:
        case BATTERY_CHARGED:
        case CHARGER_CONNECTION:
        case CAPSENSE_UPDATE:
        case USER_ACTION:
        case SPEECH_RECOGNITION:
        case AV_COMMAND:
        case REMOTE_BATTERY_LEVEL:
        case VMU_PACKET:
            args = new byte[1];
            break;

        default:
            handleException("registerNotification", GaiaError.TypeException.ILLEGAL_ARGUMENT, null,
                    Gaia.COMMAND_REGISTER_NOTIFICATION);
            return;
        }

        args[0] = (byte) event.ordinal();
        sendCommand(vendorID, Gaia.COMMAND_REGISTER_NOTIFICATION, args);
    }

    /**
     * Requests notification of the given event
     * 
     * @param event
     *            The Event for which notifications are to be raised
     * @param level
     *            The level at which events are to be raised
     */
    public void registerNotification(int vendorID, Gaia.EventId event, int level) {
        byte[] args;

        switch (event) {
        case RSSI_LOW_THRESHOLD:
        case RSSI_HIGH_THRESHOLD:
            args = new byte[2];
            args[1] = (byte) level;
            vendorID = Gaia.VENDOR_CSR;
            break;

        case BATTERY_LOW_THRESHOLD:
        case BATTERY_HIGH_THRESHOLD:
            args = new byte[3];
            args[1] = (byte) (level >>> 8);
            args[2] = (byte) level;
            vendorID = Gaia.VENDOR_CSR;
            break;

        case PIO_CHANGED:
            args = new byte[5];
            args[1] = (byte) (level >>> 24);
            args[2] = (byte) (level >>> 16);
            args[3] = (byte) (level >>> 8);
            args[4] = (byte) level;
            break;

        default:
            handleException("registerNotification", GaiaError.TypeException.ILLEGAL_ARGUMENT, null,
                    Gaia.COMMAND_REGISTER_NOTIFICATION);
            return;
        }

        args[0] = (byte) event.ordinal();
        sendCommand(vendorID, Gaia.COMMAND_REGISTER_NOTIFICATION, args);
    }

    /**
     * Requests notification of the given event
     * 
     * @param event
     *            The Event for which notifications are to be raised
     * @param level1
     *            The first level at which events are to be raised
     * @param level2
     *            The second level at which events are to be raised
     */
    public void registerNotification(Gaia.EventId event, int level1, int level2) {
        byte[] args;

        switch (event) {
        case RSSI_LOW_THRESHOLD:
        case RSSI_HIGH_THRESHOLD:
            args = new byte[3];
            args[1] = (byte) level1;
            args[2] = (byte) level2;
            break;

        case BATTERY_LOW_THRESHOLD:
        case BATTERY_HIGH_THRESHOLD:
            args = new byte[5];
            args[1] = (byte) (level1 >>> 8);
            args[2] = (byte) level1;
            args[3] = (byte) (level2 >>> 8);
            args[4] = (byte) level2;
            break;

        default:
            handleException("registerNotification", GaiaError.TypeException.ILLEGAL_ARGUMENT, null,
                    Gaia.COMMAND_REGISTER_NOTIFICATION);
            return;
        }

        args[0] = (byte) event.ordinal();
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_REGISTER_NOTIFICATION, args);
    }

    /**
     * Requests the status of notifications for the given event
     * 
     * @param event
     *            The Event for which the status is requested
     */
    public void getNotification(int vendorID, Gaia.EventId event) {
        byte[] args = new byte[1];
        args[0] = (byte) event.ordinal();
        sendCommand(vendorID, Gaia.COMMAND_GET_NOTIFICATION, args);
    }

    /**
     * Cancels notification of the given event
     * 
     * @param event
     *            The Event for which notifications are no longer to be raised.
     */
    @SuppressWarnings("SameParameterValue")
    public void cancelNotification(int vendorID, Gaia.EventId event) {
        byte[] args = new byte[1];
        args[0] = (byte) event.ordinal();
        sendCommand(vendorID, Gaia.COMMAND_CANCEL_NOTIFICATION, args);
    }

    /**
     * Sets the target for Gaia messages received from the remote device.
     * 
     * @param handler
     *            The Handler for Gaia messages received from the remote device.
     */
    public void setReceiveHandler(Handler handler) {
        mReceiveHandler = handler;
    }

    public Handler getReceiveHandler() {
        return mReceiveHandler;
    }

    /**
     * Returns the friendly name of the remote device or null if there is none.
     * 
     * @return Friendly name as a string.
     */
    public String getName() {
        return mDevice.getName();
    }


    /**
     * Obtain the BluetoothDevice object.
     * 
     * @return BluetoothDevice object.
     */
    public BluetoothDevice getBluetoothDevice() {
        return mDevice;
    }

    /**
     * To know if we are connected to a BluetoothDevice, using this library.
     * 
     * @return true if we are connected, else otherwise.
     */
    public boolean isConnected() {
        return mIsConnected;
    }

    /**
     * Write data to the Bluetooth or datagram socket.
     *
     * @param data
     *            Array of bytes to send.
     */
    private void sendCommandData(byte[] data, int commandId) {
        if (sendData(data)) {
            Log.i(TAG, "send command 0x" + Gaia.hexw(commandId));
        } else {
            handleError("sendCommandData: not connected.", GaiaError.TypeException.NOT_CONNECTED);
        }
    }
    /**
     * Returns the availability of Bluetooth
     *
     * @return true if the local Bluetooth adapter is available
     */
    private boolean getBluetoothAvailable() {
        /* Bluetooth is available only if the adapter exists */
        return mBTAdapter != null;
    }

    /**
     * To handle exceptions when it needs to inform the related application.
     *
     * @param name
     *            The method name from where this method is called.
     * @param type
     *            The type of errors to inform the application.
     * @param exception
     *            If there is an exception, add it here, otherwise "null".
     */
    @SuppressWarnings("SameParameterValue")
    private void handleException(String name, GaiaError.TypeException type, Exception exception) {
        if (mDebug) {
            Log.e(TAG, name + ": " + exception.toString());
        }
        if (mReceiveHandler != null) {
            GaiaError error = new GaiaError(type, exception);
            mReceiveHandler.obtainMessage(Message.ERROR.ordinal(), error).sendToTarget();
        }
    }

    /**
     * To handle exceptions when it needs to inform the related application.
     *
     * @param name
     *            The method name from where this method is called.
     * @param type
     *            The type of errors to inform the application.
     * @param exception
     *            If there is an exception, add it here, otherwise "null".
     * @param command
     *            The command where the error occurs.
     */
    private void handleException(String name, GaiaError.TypeException type, Exception exception, int command) {
        if (mDebug) {
            Log.e(TAG, name + ": " + exception.toString());
        }
        if (mReceiveHandler != null) {
            GaiaError error = new GaiaError(type, exception, command);
            mReceiveHandler.obtainMessage(Message.ERROR.ordinal(), error).sendToTarget();
        }
    }

    /**
     * To handle errors when it needs to inform the related application.
     *
     * @param message
     *            The message to display for a log.
     * @param type
     *            The type of errors to inform the application.
     */
    private void handleError(String message, GaiaError.TypeException type) {
        if (mDebug) {
            Log.e(TAG, message);
        }
        if (mReceiveHandler != null) {
            GaiaError error = new GaiaError(type);
            mReceiveHandler.obtainMessage(Message.ERROR.ordinal(), error).sendToTarget();
        }
    }

    private AssistantCallback mAssistantCallback = null;
    private @AssistantEnums.ConnectionState int mState = AssistantEnums.ConnectionState.DISCONNECTED;


    public void setAssistantCallback(AssistantCallback assistantCallback) {
        mAssistantCallback = assistantCallback;
    }

    public interface AssistantCallback {
        void onConnectionStateChanged(@AssistantEnums.ConnectionState int state);
        void onConnectionError(@AssistantEnums.DeviceError int error);
        void onCommunicationRunning();
        void onDataFound(byte[] data);
    }

    private static class UUIDs {
            /**
             * The SPP UUID as defined by Bluetooth specifications.
             */
            private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            /**
             * The specific GAIA UUID.
             */
            private static final UUID GAIA = UUID.fromString("00001107-D102-11E1-9B23-00025B00A5A5");
        }


        // ====== PRIVATE FIELDS ================================================================================

        /**
         * The Bluetooth Device for which this class/object provides a BR EDR connection.
         */
        private BluetoothDevice mDevice = null;
        /**
         * <p>The Thread which processes a connection to a device.</p>
         * <p>This field is null when there is no ongoing connection or once the connection has been established.</p>
         */
        private ConnectionThread mConnectionThread = null;
        /**
         * <p>The Thread which allows communication with a connected device.</p>
         * <p>This field is null if there is no active connection.</p>
         */
        private CommunicationThread mCommunicationThread = null;
        /**
         * <p>The UUID to use to connect over RFCOMM: SPP or GAIA.</p>
         */
        private UUID mUUIDTransport;
        /**
         * The connection state of this provider.
         */


        // ====== PROTECTED METHODS ============================================================================

        /**
         * <p>To get the device which is connected or had been connected through this provider.</p>
         * <p>If no successful connection had been made yet, this method returns null or a BluetoothDevice which can be
         * irrelevant.</p>
         *
         * @return The known BluetoothDevice with which a connection exists, has been made or attempted. The return value
         * can be null.
         */
        public BluetoothDevice getDevice() {
            return mDevice;
        }

        public boolean connect(BluetoothDevice device) {

            if (mState == AssistantEnums.ConnectionState.CONNECTED) {
                // already connected
                Log.w(TAG, "connection failed: a device is already connected.");
                return false;
            }
            if (device == null) {
                Log.w(TAG, "connection failed: device is null.");
                return false;
            }
            if (device.getType() != BluetoothDevice.DEVICE_TYPE_CLASSIC
                    && device.getType() != BluetoothDevice.DEVICE_TYPE_DUAL) {
                Log.w(TAG, "connection failed: the device is not BR/EDR compatible.");
                // the device is not BR EDR compatible
                return false;
            }
            if (!isBluetoothAvailable()) {
                // Bluetooth not available on Android device
                Log.w(TAG, "connection failed: Bluetooth is not available.");
                return false;
            }
            if (!BluetoothAdapter.checkBluetoothAddress(device.getAddress())) {
                // device cannot be found
                Log.w(TAG, "connection failed: device address not found in list of devices known by the system.");
                return false;
            }

            UUID transport = getUUIDTransport(device.getUuids());

            // connection can be processed only if a compatible transport has been found.
            // if the device has not yet been bonded, the UUIDs has not been fetched yet by the system.
            if (transport == null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "connection: device not bonded, no UUID available, attempt to connect using SPP.");
                transport = UUIDs.SPP;
            }
            else if (transport == null) {
                Log.w(TAG, "connection failed: device bonded and no compatible UUID available.");
                return false;
            }

            return connect(device, transport);
        }

        /**
         * <p>This method will initiate a connection with the last known BluetoothDevice with which a connection had
         * been established or attempted.</p>
         *
         * @return <code>true</code> if the reconnection process could be initiated.
         */
        /*package*/
        @SuppressWarnings("UnusedReturnValue")
        public boolean reconnectToDevice() {
            return mDevice != null && connect(mDevice);
        }

        /**
         * <p>To disconnect from an ongoing connection or to stop a connection process.</p>
         * <p>This method will cancel any ongoing Thread related to the connection of a BluetoothDevice.</p>
         *
         * @return This method returns <code>false</code> only if this provider was already disconnected from any
         * device.
         */
        /*package*/
        @SuppressWarnings({"UnusedReturnValue", "SameReturnValue"})
        public boolean disconnect() {

            if (mState == AssistantEnums.ConnectionState.DISCONNECTED) {
                Log.w(TAG, "disconnection failed: no device connected.");
                return false;
            }

            setState(AssistantEnums.ConnectionState.DISCONNECTING);

            // cancel any running thread
            cancelConnectionThread();
            cancelCommunicationThread();

            setState(AssistantEnums.ConnectionState.DISCONNECTED);
            mIsConnected = false;
            mReceiveHandler.obtainMessage(Message.DISCONNECTED.ordinal()).sendToTarget();

            Log.i(TAG, "Provider disconnected from BluetoothDevice " + (mDevice != null ? mDevice.getAddress() : "null"));

            return true;
        }

        /**
         * <p>To send some data to a connected BluetoothDevice.</p>
         *
         * @param data
         *          The bytes to send to the BluetoothDevice.
         * @return
         *          true if the sending could be initiated.
         */
        public boolean sendData(byte[] data) {
            if (mDebug) {
                Log.d(TAG, "Request received for sending data to a device.");
            }

            // Create temporary object
            CommunicationThread thread;
            // Synchronize a copy of the ConnectedThread
            synchronized (this) {
                if (mState != AssistantEnums.ConnectionState.CONNECTED) {
                    Log.w(TAG, "Attempt to send data failed: provider not currently connected to a device.");
                    return false;
                }
                if (mCommunicationThread == null) {
                    Log.w(TAG, "Attempt to send data failed: CommunicationThread is null.");
                    return false;
                }

                thread = mCommunicationThread;
            }

            // Perform a non synchronized write
            return thread.sendStream(data);
        }

        /**
         * <p>To know if the provider is ready to communicate with the remote device.</p>
         *
         * @return True if the provider is ready to let the application communicate with the device.
         */
        /*package*/
        @SuppressWarnings("unused")
        boolean isConnectionReady() {
            // to be ready the mBluetoothProvider needs to have a running connection
            return mState == AssistantEnums.ConnectionState.CONNECTED
                    && mCommunicationThread != null && mCommunicationThread.mmIsRunning;
        }

        /**
         * <p>Gets the current connection state between this service and a Bluetooth device.</p>
         *
         * @return the current connection state.
         */
        public synchronized @AssistantEnums.ConnectionState int getState() {
            return mState;
        }


        // ====== PRIVATE METHODS ==============================================================================

        /**
         * Set the current state of the chat connection
         *
         * @param state
         *         An integer defining the current connection state
         */
        private synchronized void setState(@AssistantEnums.ConnectionState int state) {
            if (mDebug) {

                Log.d(TAG, "Connection state changes from " + AssistantEnums.getConnectionStateLabel(mState) + " to "
                        + AssistantEnums.getConnectionStateLabel(state));
            }
            mState = state;
            if(mAssistantCallback != null) mAssistantCallback.onConnectionStateChanged(state);
        }

        /**
         * <p>This method aims to find the UUID transport this provider should use.</p>
         * <p>It goes through an array of {@link ParcelUuid ParcelUuid} in order to find the {@link UUIDs#SPP SPP} or
         * {@link UUIDs#GAIA GAIA} UUID.</p>
         *
         * @param uuids
         *          The list of UUIDs in which to look for the {@link UUIDs#SPP SPP} or  {@link UUIDs#GAIA GAIA} UUID.
         *
         * @return the first UUID found which matches {@link UUIDs#SPP SPP} or {@link UUIDs#GAIA GAIA}. If none of these
         * UUID could be found, this method returns null.
         */
        private UUID getUUIDTransport(ParcelUuid[] uuids) {
            if (uuids == null) {
                return null;
            }

            for (ParcelUuid parcel : uuids) {
                UUID uuid = parcel.getUuid();
                if (uuid.equals(UUIDs.SPP) || uuid.equals(UUIDs.GAIA)) {
                    return uuid;
                }
            }

            return null;
        }

        /**
         * <p>This method will cancel the current ConnectionThread.</p>
         */
        private void cancelConnectionThread() {
            if (mConnectionThread != null) {
                mConnectionThread.cancel();
                mConnectionThread = null;
            }
        }

        /**
         * <p>This method will cancel the current CommunicationThread.</p>
         */
        private void cancelCommunicationThread() {
            if (mCommunicationThread != null) {
                mCommunicationThread.cancel();
                mCommunicationThread = null;
            }
        }

        /**
         * <p><p>To initiate the BR/EDR connection with the given Bluetooth device using the given transport.</p>
         * <p>This method returns true if it has been able to successfully initiate the BR/EDR connection with the device
         * .</p>
         * <p>The reasons for the connection initiation to not be successful could be:
         * <ul>
         *     <li>There is already a connected device.</li>
         *     <li>The creation of the BluetoothSocket failed.</li>
         * </ul></p>
         *
         * @param device
         *          The BluetoothDevice to connect with.
         * @param transport
         *          The UUID transport to use to connect over RFCOMM.
         *
         * @return <code>true</code> if the connection had been successfully initiated.
         */
        private boolean connect(@NonNull BluetoothDevice device, @NonNull UUID transport) {
            if (mDebug) {
                Log.d(TAG, "Request received to connect to a BluetoothDevice with UUID " + transport.toString());
            }

            // Check there is no running connection
            if (mState == AssistantEnums.ConnectionState.CONNECTED && mCommunicationThread != null) {
                Log.w(TAG, "connection failed: Provider is already connected to a device with an active communication.");
                return false;
            }

            // Cancel any thread attempting to make a connection
            cancelConnectionThread();
            // Cancel any thread currently running a connection
            cancelCommunicationThread();

            // attempt connection
            setState(AssistantEnums.ConnectionState.CONNECTING);

            // create the Bluetooth socket
            BluetoothSocket socket = createSocket(device, transport);
            if (socket == null) {
                Log.w(TAG, "connection failed: creation of a Bluetooth socket failed.");
                return false; // socket creation failed
            }

            // all check passed successfully, the request can be initiated
            if (mDebug) {
                Log.d(TAG, "Request connect to BluetoothDevice "
                        + socket.getRemoteDevice().getAddress() + " over RFCOMM starts.");
            }

            // keep device and UUID information
            mUUIDTransport = transport;
            mDevice = device;

            // Start the thread to connect with the given device
            mConnectionThread = new ConnectionThread(socket);
            mConnectionThread.start();

            return true;
        }

        /**
         * Returns the availability of Bluetooth feature for the Android device.
         *
         * @return true if the local Bluetooth adapter is available.
         */
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean isBluetoothAvailable() {
            // Bluetooth is available only if the adapter exists
            return mBTAdapter != null;
        }

        /**
         * Check for RFCOMM security.
         *
         * @return True if RFCOMM security is implemented.
         */
        @SuppressLint("ObsoleteSdkInt")
        private boolean btIsSecure() {
            // Establish if RFCOMM security is implemented, in which case we'll
            // use the insecure variants of the RFCOMM functions
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1;
        }

        /**
         * Create the RFCOMM bluetooth socket.
         *
         * @return BluetoothSocket object.
         */
        private BluetoothSocket createSocket(BluetoothDevice device, UUID transport) {
            if (mDebug) {
                Log.d(TAG, "Creating Bluetooth socket for device " + device.getAddress() + " using UUID " + transport);
            }

            try {
                if (btIsSecure()) {
                    return device.createInsecureRfcommSocketToServiceRecord(transport);
                } else {
                    return device.createRfcommSocketToServiceRecord(transport);
                }
            } catch (IOException e) {
                Log.w(TAG, "Exception occurs while creating Bluetooth socket: " + e.toString());
                Log.i(TAG, "Attempting to invoke method to create Bluetooth Socket.");

                try {
                    // This is a workaround that reportedly helps on some older devices like HTC Desire, where using
                    // the standard createRfcommSocketToServiceRecord() method always causes connect() to fail.
                    //noinspection RedundantArrayCreation
                    Method method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    // noinspection UnnecessaryBoxing
                    return (BluetoothSocket) method.invoke(device, Integer.valueOf(1));
                } catch (Exception e1) {
                    // NoSuchMethodException from method getMethod: impossible to retrieve the method.
                    // IllegalArgumentException from method invoke: problem with arguments which don't match with
                    // expectations.
                    // IllegalAccessException from method invoke: if invoked object is not accessible.
                    // InvocationTargetException from method invoke: Exception thrown by the invoked method.
                    Log.w(TAG, "Exception occurs while creating Bluetooth socket by invoking method: " + e.toString());
                }
            }

            return null;
        }

        /**
         * Indicates that the connection attempt failed and notifies the listener.
         */
        private void onConnectionFailed() {
            // update connection state
            setState(AssistantEnums.ConnectionState.DISCONNECTED);
            mIsConnected = false;
            mReceiveHandler.obtainMessage(Message.DISCONNECTED.ordinal()).sendToTarget();
            // Dispatch the corresponding failure message
            if(mAssistantCallback != null) mAssistantCallback.onConnectionError(AssistantEnums.DeviceError.CONNECTION_FAILED);
        }

        /**
         * Indicates that the connection was lost and notifies the listener.
         */
        private void onConnectionLost() {
            // update connection state
            mIsConnected = false;
            setState(AssistantEnums.ConnectionState.DISCONNECTED);
            mReceiveHandler.obtainMessage(Message.DISCONNECTED.ordinal()).sendToTarget();
            // Dispatch the corresponding failure message
            if(mAssistantCallback != null) mAssistantCallback.onConnectionError(AssistantEnums.DeviceError.CONNECTION_LOST);
        }

        /**
         * <p>This method is called when the BluetoothSocket had successfully connected to a BluetoothDevice.</p>
         * <p>This method cancel the Connection and Communication Threads if they exist and creates a
         * new instance of CommunicationThread in order to listen for incoming messages from the connected device.</p>
         *
         * @param socket
         *          The socket used to run the successful connection.
         */
        private void onSocketConnected(BluetoothSocket socket) {
            Log.i(TAG, "Successful connection to device: " + getDevice().getAddress());

            if (mDebug) {
                Log.d(TAG, "Initialisation of ongoing communication by creating and running a CommunicationThread.");
            }
            // Cancel the thread that completed the connection
            cancelConnectionThread();
            // Cancel any thread currently running a connection
            cancelCommunicationThread();

            // the Bluetooth connection is now established
            mIsConnected = true;
            setState(AssistantEnums.ConnectionState.CONNECTED);
            mReceiveHandler.obtainMessage(Message.CONNECTED.ordinal(), mDevice.getAddress()).sendToTarget();
            mCommunicationThread = new CommunicationThread(socket);
            mCommunicationThread.start();
        }


        // ====== INNER CLASS ==================================================================================

        /**
         * <p>Thread to use in order to connect a BluetoothDevice using a BluetoothSocket.</p>
         * <p>The connection to a BluetoothDevice using a BluetoothSocket is synchronous but can take time. To avoid to
         * block the current Thread of the application - in general the UI Thread - the connection runs in its own
         * Thread.</p>
         */
        private class ConnectionThread extends Thread {

            /**
             * The Bluetooth socket to use to connect to the remote device.
             */
            private final BluetoothSocket mmConnectorSocket;
            /**
             * <p>The tag to display for logs of this Thread.</p>
             */
            private final String THREAD_TAG = "ConnectionThread";

            /**
             * <p>To create a new instance of this class.</p>
             *
             * @param socket
             *          The necessary Bluetooth socket for this Thread to connect with a device.
             */
            private ConnectionThread(@NonNull BluetoothSocket socket) {
                setName(THREAD_TAG + getId());
                mmConnectorSocket = socket;
            }

            @Override
            public void run() {
                try {
                    if (mDebug) {
                        Log.d(THREAD_TAG, "Attempt to connect device over BR/EDR: " + mDevice.getAddress()
                                + " using " + (mUUIDTransport.equals(UUIDs.SPP) ? "SPP" : "GAIA"));
                    }
                    // Cancel discovery otherwise it slows down the connection.
                    mBTAdapter.cancelDiscovery();
                    // Connect to the remote device through the socket.
                    // This call blocks until it succeeds or throws an exception.
                    mmConnectorSocket.connect();
                } catch (IOException connectException) {
                    // Unable to connect; close the socket and return.
                    Log.w(THREAD_TAG, "Exception while connecting: " + connectException.toString());
                    try {
                        mmConnectorSocket.close();
                    } catch (IOException closeException) {
                        Log.w(THREAD_TAG, "Could not close the client socket", closeException);
                    }
                    onConnectionFailed();
                    mConnectionThread = null;
                    return;
                }

                // connection succeeds
                onSocketConnected(mmConnectorSocket);
            }

            /**
             * To cancel this thread if it was running.
             */
            private void cancel() {
                // stop the thread if still running
                // because the BluetoothSocket.connect() method is synchronous the only way to stop this Thread is to use
                // the interrupt() method even if it is not recommended.
                interrupt();
            }
        }

        /**
         * <p>Thread to use in order to listen for incoming message from a connected BluetoothDevice.</p>
         * <p>To get messages from a remote device connected using a BluetoothSocket, an application has to constantly
         * read bytes over the InputStream of the BluetoothSocket. In order to avoid to block the current Thread of an
         * application - usually the UI Thread - it is recommended to do it in its own thread.</p>
         */
        private class CommunicationThread extends Thread {
            /**
             * The InputStream object to read bytes from in order to get messages from a connected remote device.
             */
            private final InputStream mmInputStream;
            /**
             * The OutputStream object to write bytes on in  order to send messages to a connected remote device.
             */
            private final OutputStream mmOutputStream;
            /**
             * The BluetoothSocket which has successfully been connected to a BluetoothDevice.
             */
            private final BluetoothSocket mmSocket;
            /**
             * To constantly read messages coming from the remote device.
             */
            private boolean mmIsRunning = false;
            /**
             * The tag to display for logs of this Thread.
             */
            private final String THREAD_TAG = "CommunicationThread";


            final byte[] packet = new byte[Gaia.MAX_PACKET];
            int flags;
            int packet_length = 0;
            int expected = Gaia.MAX_PAYLOAD;

            /**
             * <p>To create a new instance of this class.</p>
             * <p>This constructor will initialized all its private field depending on the given BluetoothSocket.</p>
             *
             * @param socket
             *          A BluetoothSocket which has successfully been connected to a BluetoothDevice.
             */
            CommunicationThread(@NonNull BluetoothSocket socket) {
                setName("CommunicationThread" + getId());
                mmSocket = socket;

                // temporary object to get the Bluetooth socket input and output streams as they are final
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                try {
                    tmpIn = mmSocket.getInputStream();
                    tmpOut = mmSocket.getOutputStream();
                } catch (IOException e) {
                    Log.e(THREAD_TAG, "Error occurred when getting input and output streams", e);
                }

                mmInputStream = tmpIn;
                mmOutputStream = tmpOut;
            }


            @Override // Thread
            public void run() {
                if (mmInputStream == null) {
                    Log.w(THREAD_TAG, "Run thread failed: InputStream is null.");
                    disconnect();
                    return;
                }

                if (mmOutputStream == null) {
                    Log.w(THREAD_TAG, "Run thread failed: OutputStream is null.");
                    disconnect();
                    return;
                }

                if (mmSocket == null) {
                    Log.w(THREAD_TAG, "Run thread failed: BluetoothSocket is null.");
                    disconnect();
                    return;
                }

                if (!mmSocket.isConnected()) {
                    Log.w(THREAD_TAG, "Run thread failed: BluetoothSocket is not connected.");
                    disconnect();
                    return;
                }

                // all check passed successfully, the listening can start
                listenStream();
            }

            /**
             * <p>This method runs the constant read of the InputStream in order to get messages from the connected
             * remote device.</p>
             */
            private void listenStream() {
                final int MAX_BUFFER = 1024;
                byte[] buffer = new byte[MAX_BUFFER];

                if (mDebug) {
                    Log.d(THREAD_TAG, "Start to listen for incoming streams.");
                }

                mmIsRunning = true;
                // to inform subclass it is possible to communicate with the device
                if(mAssistantCallback != null) mAssistantCallback.onCommunicationRunning();

                while (mState == AssistantEnums.ConnectionState.CONNECTED && mmIsRunning) {
                    int length;
                    try {
                        length = mmInputStream.read(buffer);
                    } catch (IOException e) {
                        Log.e(THREAD_TAG, "Reception of data failed: exception occurred while reading: " + e.toString());
                        mmIsRunning = false;
                        if (mState == AssistantEnums.ConnectionState.CONNECTED) {
                            onConnectionLost();
                        }
                        mCommunicationThread = null;
                        break;
                    }

                    scanStream(buffer, length);

                    // if buffer contains some bytes, they are sent to the listener
                    if (length > 0) {
                        byte[] data = new byte[length];
                        System.arraycopy(buffer, 0, data, 0, length);
                        if (mDebug) {
                            Log.d(THREAD_TAG, "Reception of data: " + AssistantUtils.getStringFromBytes(data));
                        }
                        if(mAssistantCallback != null) mAssistantCallback.onDataFound(data);

                    }
                }

                if (mDebug) {
                    Log.d(THREAD_TAG, "Stop to listen for incoming streams.");
                }
            }

            private void scanStream(byte[] buffer, int length) {
                for (int i = 0; i < length; ++i) {
                    if ((packet_length > 0) && (packet_length < Gaia.MAX_PACKET)) {
                        packet[packet_length] = buffer[i];

                        if (packet_length == Gaia.OFFS_FLAGS)
                            flags = buffer[i];

                        else if (packet_length == Gaia.OFFS_PAYLOAD_LENGTH) {
                            expected = buffer[i] + Gaia.OFFS_PAYLOAD + (((flags & Gaia.FLAG_CHECK) != 0) ? 1 : 0);
                            if (mVerbose)
                                Log.d(TAG, "expect " + expected);
                        }

                        ++packet_length;

                        if (packet_length == expected) {
                            if (mVerbose)
                                Log.d(TAG, "got " + expected);

                            if (mReceiveHandler == null) {
                                if (mDebug)
                                    Log.e(TAG, "No receiver");
                            }

                            else {
                                GaiaPacket command = new GaiaPacket(packet, packet_length);

                                if (command.getEvent() == Gaia.EventId.START && !mIsConnected) {
                                    if (mDebug)
                                        Log.i(TAG, "connection starts");
                                    mReceiveHandler.obtainMessage(Message.CONNECTED.ordinal(), mDevice.getAddress()).sendToTarget();
                                    mIsConnected = true;
                                }

                                else {
                                    if (mDebug)
                                        Log.i(TAG, "received command 0x" + Gaia.hexw(command.getCommand()));
                                    mReceiveHandler.obtainMessage(Message.PACKET.ordinal(), command).sendToTarget();
                                }
                            }

                            packet_length = 0;
                            expected = Gaia.MAX_PAYLOAD;
                        }
                    }

                    else if (buffer[i] == Gaia.SOF)
                        packet_length = 1;
                }
            }


            /**
             * <p>To write some data on the OutputStream in order to send it to a connected remote device.</p>
             *
             * @param data
             *              the data to send.
             *
             * @return true, if the data had successfully been writing on the OutputStream.
             */
            /*package*/ boolean sendStream(byte[] data) {
                if (mDebug) {
                    Log.d(TAG, "Process sending of data to the device starts");
                }

                if (mmSocket == null) {
                    Log.w(THREAD_TAG, "Sending of data failed: BluetoothSocket is null.");
                    return false;
                }

                if (!mmSocket.isConnected()) {
                    Log.w(THREAD_TAG, "Sending of data failed: BluetoothSocket is not connected.");
                    return false;
                }

                if (mState != AssistantEnums.ConnectionState.CONNECTED) {
                    Log.w(THREAD_TAG, "Sending of data failed: Provider is not connected.");
                    return false;
                }

                if (mmOutputStream == null) {
                    Log.w(THREAD_TAG, "Sending of data failed: OutputStream is null.");
                    return false;
                }

                try {
                    mmOutputStream.write(data);
                } catch (IOException e) {
                    Log.w(THREAD_TAG, "Sending of data failed: Exception occurred while writing data: " + e.toString());
                    return false;
                }

                if (mDebug) {
                    Log.d(TAG, "Success sending of data.");
                }

                return true;
            }

            /**
             * To cancel this thread if it was running.
             */
            /*package*/ void cancel() {
                if (mDebug) {
                    Log.d(TAG, "Thread is cancelled.");
                }

                mmIsRunning = false;

                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.w(THREAD_TAG, "Cancellation of the Thread: Close of BluetoothSocket failed: " + e.toString());
                }
            }

        }
}
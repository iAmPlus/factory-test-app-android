/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.libraries.assistant.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.qualcomm.qti.libraries.assistant.AssistantConsts;
import com.qualcomm.qti.libraries.assistant.AssistantEnums;
import com.qualcomm.qti.libraries.assistant.AssistantUtils;
import com.qualcomm.qti.libraries.assistant.ivor.IvorManager;
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBREDR;

/**
 * <p>This class provides the tools to connect, communicate and disconnect with a BR/EDR device over RFCOMM using the
 * GAIA protocol. This class manages a BR EDR connection by extending {@link BREDRProvider BREDRProvider}.</p>
 * <p>This class analyzes the incoming data from a device in order to detect GAIA packets which are then provided to
 * a registered listener using {link Messages#GAIA_PACKET}.</p>
 */
public class GaiaBREDRProvider extends BREDRProvider implements IvorManager.IvorManagerListener {
    /**
     * <p>The tag to display for logs.</p>
     */
    private final String TAG = "GaiaBREDRProvider";
    /**
     * <p>To show the debug logs.</p>
     */
    private static final boolean DEBUG_LOGS = AssistantConsts.Debug.GAIA_BR_EDR_PROVIDER;
    /**
     * <p>The listener which is interested in events going on on this provider: connection state, errors, messages
     * received, etc.</p>
     */
    private final Handler mListener;
    /**
     * The analyser of data used to build GAIA packets from bytes received from the Provider.
     */
    private final DataAnalyser mAnalyser = new DataAnalyser();
    /**
     *
     */
    private final IvorManager mIvorManager = new IvorManager(this);


    // ====== ENUMS =====================================================================================


    // ====== CONSTRUCTOR ===============================================================================

    /**
     * <p>Constructor of this class to get a provider of BR/EDR connection.</p>
     *
     * @param manager
     *          The BluetoothManager this provider should use to get a BluetoothAdapter. If this is null the provider
     *          will use {@link BluetoothAdapter#getDefaultAdapter() getDefaultAdapter}, known as less efficient than
     *          {@link BluetoothManager#getAdapter() getAdapter}.
     * @param listener
     *         The handler which would like to get events, messages and information form this provider about a connection to
     *         a device.
     */
     public GaiaBREDRProvider(@NonNull Handler listener, BluetoothManager manager) {
        super(manager);
        mListener = listener;
    }


    // ====== PUBLIC METHODS ===============================================================================

    /**
     * <p>To inform the device that the assistant audio answer had been started to be played.</p>
     */
    public void onStartPlayingAnswer() {
        mIvorManager.onStartPlayingAnswer();
    }

    /**
     * <p>To inform a device that an assistant audio answer had been played.</p>
     */
    public void onFinishPlayingAnswer() {
        mIvorManager.onFinishPlayingAnswer();
    }

    /**
     * <p>To request the device to start to send some audio voice data.</p>
     */
    public boolean startVoiceStreaming() {
        return mIvorManager.startVoiceStreaming();
    }

    /**
     * <p>To ask the device to stop to send the audio voice data.</p>
     */
    public void stopVoiceStreaming() {
        mIvorManager.stopVoiceStreaming();
    }

    /**
     * <p>To cancel any current assistant session with the device.</p>
     *
     * @param error
     *          The error type to provide to the device.
     */
    public void cancelIvorSession(@AssistantEnums.IvorError int error) {
        mIvorManager.cancelSession(error);
    }

    /**
     * <p>To force a reset of the communication with the device. This method resets the IVOR manager and reconnects to
     * the device if it was disconnected.</p>
     */
    public void forceReset() {
        mIvorManager.forceReset(getState() == AssistantEnums.ConnectionState.CONNECTED);
        if (getState() == AssistantEnums.ConnectionState.DISCONNECTED && getDevice() != null) {
            reconnectToDevice();
        }
    }

    /**
     * <p>To initiate the BR/EDR connection with the given Bluetooth device.</p> <p>This method returns true if it has
     * been able to successfully initiate a BR/EDR connection with the device.</p> <p>The reasons for the connection
     * initiation to not be successful could be: <ul> <li>There is already a connected device.</li> <li>The device is
     * not BR/EDR compatible.</li> <li>Bluetooth is not available - could have been turned off, Android device doesn't
     * provide the feature, etc.</li> <li>The Bluetooth device address is unknown.</li> <li>A Bluetooth socket cannot be
     * established with the device.</li> <li>The UUIDs provided by the device does not contain {@link UUIDs#SPP SPP} or
     * {@link UUIDs#GAIA GAIA}.</li> </ul></p>
     *
     * @param device
     *         The device to connect with over a BR/EDR connection.
     *
     * @return True if the connection had been initialised, false if it can't be started.
     */
    public boolean connect(@NonNull BluetoothDevice device) {
        return super.connect(device);
    }

    /**
     * <p>To disconnect from an ongoing connection or to stop a connection process.</p>
     * <p>This method will cancel any ongoing Thread related to the connection of a BluetoothDevice.</p>
     *
     * @return This method returns <code>false</code> only if this provider was already disconnected from any
     * device.
     */
    public boolean disconnect() {
        return super.disconnect();
    }

    /**
     * <p>Gets the current connection state between this provider and a Bluetooth device.</p>
     *
     * @return the current connection state.
     */
    public @AssistantEnums.ConnectionState int getState() {
        return super.getState();
    }

    /**
     * <p>Gets the current IVOR state between this provider and a Bluetooth device.</p>
     *
     * @return the current IVOR state.
     */
    public @AssistantEnums.IvorState int getIvorState() {
        return mIvorManager.getState();
    }

    /**
     * <p>To get the device which is connected or had been connected through this provider.</p>
     * <p>If no successful connection had been made yet, this method returns null or a BluetoothDevice which can be
     * irrelevant.</p>
     *
     * @return The known BluetoothDevice with which a connection exists, has been made or attempted. The return value
     * can be null.
     */
    public BluetoothDevice getDevice() {
        return super.getDevice();
    }


    // ====== OVERRIDE SUPERCLASS METHODS - BREDRProvider ====================================================

    @Override // BREDRProvider
    void onConnectionStateChanged(@AssistantEnums.ConnectionState int state) {
        sendMessageToListener(AssistantEnums.ProviderMessage.CONNECTION_STATE_HAS_CHANGED, state);

        if (state != AssistantEnums.ConnectionState.CONNECTED) {
            mIvorManager.reset();
            mAnalyser.reset();
        }
    }

    @Override // BREDRProvider
    void onConnectionError(@AssistantEnums.DeviceError int error) {
        sendMessageToListener(AssistantEnums.ProviderMessage.ERROR, error);
    }

    @Override // BREDRProvider
    void onCommunicationRunning() {
        // this method is called in a running thread which needs to continue as soon as possible

        // sendMessageToListener throws a message so no process over the connection is done
        sendMessageToListener(AssistantEnums.ProviderMessage.GAIA_READY);
    }

    @Override // BREDRProvider
    void onDataFound(byte[] data) {
        mAnalyser.analyse(data);
    }


    // ====== OVERRIDE SUPERCLASS METHODS - IvorManager.IvorManagerListener ==========================

    @Override // IvorManager.IvorManagerListener
    public boolean sendGAIAPacket(byte[] packet) {
        return sendData(packet);
    }

    @Override // IvorManager.IvorManagerListener
    public void startSession() {
        sendMessageToListener(AssistantEnums.ProviderMessage.IVOR_MESSAGE, AssistantEnums.IvorMessage.START_SESSION, null);
    }

    @Override // IvorManager.IvorManagerListener
    public void receiveVoiceData(byte[] data) {
        sendMessageToListener(AssistantEnums.ProviderMessage.IVOR_MESSAGE, AssistantEnums.IvorMessage.VOICE_DATA, data);
    }

    @Override // IvorManager.IvorManagerListener
    public void onIvorError(int error) {
        sendMessageToListener(AssistantEnums.ProviderMessage.IVOR_MESSAGE, AssistantEnums.IvorMessage.CANCEL_SESSION, error);
    }

    @Override // IvorManager.IvorManagerListener
    public void onVoiceEnded() {
        sendMessageToListener(AssistantEnums.ProviderMessage.IVOR_MESSAGE, AssistantEnums.IvorMessage.VOICE_END, null);
    }

    @Override // IvorManager.IvorManagerListener
    public void onIvorStateUpdated(@AssistantEnums.IvorState int state) {
        sendMessageToListener(AssistantEnums.ProviderMessage.IVOR_MESSAGE, AssistantEnums.IvorMessage.STATE, state);
    }


    // ====== PRIVATE METHODS =========================================================================

    /**
     * <p>This method is called when the {@link DataAnalyser DataAnalyser} has built a potential GAIA packet from
     * incoming data from the connected device.</p>
     * <p>This method will dispatch the packet to its listener if there is no active upgrade.</p>
     *
     * @param data
     *          The potential packet.
     */
    private void onGaiaPacketFound(byte[] data) {
        if (DEBUG_LOGS) {
            Log.d(TAG, "Receive potential GAIA packet: " + AssistantUtils.getStringFromBytes(data));
        }
        mIvorManager.onReceiveGAIAPacket(data);
    }

    /**
     * <p>To inform the listener by sending it a message.</p>
     *
     * @param message
     *         The message type to send.
     */
    @SuppressWarnings("SameParameterValue")
    private void sendMessageToListener(@AssistantEnums.ProviderMessage int message) {
        mListener.obtainMessage(message).sendToTarget();
    }

    /**
     * <p>To inform the listener by sending a message to it.</p>
     *
     * @param message
     *         The message type to send.
     * @param object
     *         Any complementary object to the message.
     */
    private void sendMessageToListener(@AssistantEnums.ProviderMessage int message, Object object) {
        mListener.obtainMessage(message, object).sendToTarget();
    }

    /**
     * <p>To inform the listener by sending a message to it.</p>
     *
     * @param message
     *         The message type to send.
     * @param subMessage
     *         Any complementary message for the message
     * @param object
     *         Any complementary object to the message.
     */
    @SuppressWarnings("SameParameterValue")
    private void sendMessageToListener(@AssistantEnums.ProviderMessage int message, int subMessage, Object object) {
        mListener.obtainMessage(message, subMessage, 0, object).sendToTarget();
    }


    // ====== PRIVATE INNER CLASS ========================================================================

    /**
     * <p>This class analyses incoming data in order to build a packet corresponding to the GAIA protocol.</p>
     */
    private class DataAnalyser {
        /**
         * <p>This array contains the data received from the device and which might correspond to an GAIA packet.</p>
         */
        final byte[] mmData = new byte[GaiaPacketBREDR.MAX_PACKET];
        /**
         * <p>While building the data of an GAIA packet, this contain the flags information of the packet.</p>
         */
        int mmFlags;
        /**
         * <p>To get how many bytes had been received so far.</p>
         */
        int mmReceivedLength = 0;
        /**
         * <p>The number of bytes which are expected to build a current GAIA packet.</p>
         */
        int mmExpectedLength = GaiaPacketBREDR.MAX_PACKET;

        /**
         * <p>To reset the data of the analyser: no current packet at the moment.</p>
         */
        private void reset() {
            mmReceivedLength = 0;
            mmExpectedLength = GaiaPacketBREDR.MAX_PACKET;
        }

        /**
         * <p>This method will build an GAIA packet as defined in {@link GaiaPacketBREDR GaiaPacketBREDR}.</p>
         * <p>This method uses the data provided at each call to build an GAIA packet following this process:
         * <ol>
         *     <li>Looks for the start of the packet known as "start of frame": <code>{@link GaiaPacketBREDR#SOF SOF} =
         *     0xFF</code>.</li>
         *     <li>Gets the expected length of the GAIA packet using the bytes which follow SOF: flags and length.</li>
         *     <li>For each byte of a packet, copies the byte in the data array until it reaches the
         *     expectedLength.</li>
         *     <li>Calls {@link #onDataFound(byte[]) onDataFound} when the number of accumulated data reaches the
         *     expected length.</li>
         * </ol></p>
         *
         * @param data
         *          The data to analyse in order to build GAIA packet(s).
         */
        private void analyse(byte[] data) {
            int length = data.length;

            // go through the received data
            //noinspection ForLoopReplaceableByForEach // it is more efficient to not use foreach
            for (int i = 0; i < length; ++i) {
                // has started to get data of a GAIA packet
                if ((this.mmReceivedLength > 0) && (this.mmReceivedLength <= GaiaPacketBREDR.MAX_PACKET)) {
                    // gets the data
                    mmData[this.mmReceivedLength] = data[i];

                    // gets the flags to know if there is a checksum which has impact on the GAIA packet length
                    if (this.mmReceivedLength == GaiaPacketBREDR.OFFSET_FLAGS)  { // = 2
                        mmFlags = data[i];
                    }
                    // gets the expected length
                    else if (this.mmReceivedLength == GaiaPacketBREDR.OFFSET_LENGTH) { // = 3
                        mmExpectedLength = (data[i] & 0xFF) // payload length
                                + GaiaPacketBREDR.OFFSET_PAYLOAD // number of bytes before
                                + (((mmFlags & GaiaPacketBREDR.FLAG_CHECK_MASK) != 0) ? 1 : 0);
                    }

                    // number of received bytes can be incremented
                    ++this.mmReceivedLength;

                    // if GAIA packet is complete, it is dispatched
                    if (this.mmReceivedLength == mmExpectedLength) {
                        byte[] packet = new byte[mmReceivedLength];
                        System.arraycopy(mmData, 0, packet, 0, mmReceivedLength);
                        reset();
                        onGaiaPacketFound(packet);
                    }
                }
                // look for the start of frame
                else if (data[i] == GaiaPacketBREDR.SOF) {
                    this.mmReceivedLength = 1;
                    mmData[GaiaPacketBREDR.OFFSET_SOF] = data[i];
                }
                // number of received bytes is too big for a GAIA packet
                else if (mmReceivedLength > GaiaPacketBREDR.MAX_PACKET) {
                    Log.w(TAG, "Packet is too long: received length is bigger than the maximum length of a GAIA " +
                            "packet. Resetting analyser.");
                    reset();
                }
            }
        }

    }
}

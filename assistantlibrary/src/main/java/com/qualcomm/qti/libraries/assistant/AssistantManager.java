/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.libraries.assistant;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.qualcomm.qti.libraries.assistant.ivor.IvorManager;

import java.lang.ref.WeakReference;

/**
 * <p>To manage the core of the assistant feature. This manager is in charge of linking the
 * {@link Assistant Assistant} and the Bluetooth device the assistant communicates with.</p>
 * <p>To initialise this manager:
 * <ol><li>Uses the constructor
 * {@link AssistantManager#AssistantManager(AssistantManagerListener) AssistantManager(AssistantManagerListener)}
 * and gives an object which implements the {@link AssistantManagerListener} to be updated by this manager on states
 * or required actions.</li>
 * <li>Call {@link #init(Context)} to initialise the manager. The {@link Context} is not kept within the assistant
 * and is only used to get the {@link BluetoothManager} to manage Bluetooth connection with the
 * {@link com.qualcomm.qti.libraries.assistant.bluetooth.BREDRProvider BREDRProvider}.</li>
 * <li>Set up an assistant to use by calling {@link #setAssistant(Assistant)} and {@link #getAssistantListener()}
 * in order for this manager to answer to the requests from the {@link Assistant}.</li>
 * </ol></p>
 * <p>When the assistant feature is not required anymore, call {@link #close()} in order to release all resources
 * used by this manager.</p>
 */
public class AssistantManager  {

    // ====== CONSTANTS FIELDS ===============================================================================

    /**
     * To show the debugging logs of this class.
     */
    private static final boolean DEBUG_LOGS = AssistantConsts.Debug.ASSISTANT_MANAGER;
    /**
     * The time in milliseconds to delay the trigger which checks if the device has sent more data within this time.
     */
    private static final int NO_DATA_DELAY_MS = 1000;
    /**
     * The time in milliseconds to delay for the first time the trigger which checks if the device has sent more data
     * within this time.
     */
    private static final int FIRST_NO_DATA_DELAY_MS = 4 * NO_DATA_DELAY_MS;
    /**
     * The time in milliseconds within which the assistant shall answer to the streamed voice request.
     */
    private static final int NO_RESPONSE_DELAY_MS = 40000;


    // ====== PRIVATE FIELDS ===============================================================================

    /**
     * <p>The tag to display for logs.</p>
     */
    private final String TAG = "AssistantManager";
    /**
     * <p>To handle some tasks which requires to be delayed.</p>
     */
    private final Handler mHandler = new Handler();
    /**
     * The Provider of a BR/EDR connection with a BluetoothDevice able to detect GAIA/IVOR packets and to manage
     * these packets.
     */
    private IvorManager mIvorManager;
    /**
     * <p>The assistant which provides the core of the assistant feature by being able to analyse and answer to a
     * user voice request.</p>
     */
    private Assistant mAssistant;
    /**
     * <p>The listener to request some tasks or propagate some information.</p>
     */
    private final AssistantManagerListener mListener;
    /**
     * <p>The Bluetooth device to connect with.</p>
     */
    private BluetoothDevice mDeviceToConnect;
    /**
     * <p>The number of packets this manager has received during a voice session.</p>
     */
    private int mReceivedPackets = 0;
    /**
     * <p>The number of packets which had been received last time the trigger to check if the device is still
     * sending some data while this manager expects some had been thrown.</p>
     */
    private int mReceivedPacketsChecked = 0;
    /**
     * <p>The time stamp of the start of the last/current voice session.</p>
     */
    private long mRequestTime = 0;


    // ====== IMPLEMENTED FIELDS ===============================================================================

    /**
     * <p>The runnable triggered to check if the device is still sending some voice data while it is expected to
     * receive some.</p>
     */
    private final Runnable mNoDataRunnable = new Runnable() {
        @Override
        public void run() {
            if (getIvorState() == AssistantEnums.IvorState.VOICE_REQUESTED
                    || getIvorState() == AssistantEnums.IvorState.VOICE_REQUESTING) {
                if (mReceivedPacketsChecked == mReceivedPackets) {
                    // no more data is received but is expected
                    Log.w(TAG, "Session cancelled: no more data received from device, received="
                            + mReceivedPackets + " time=" + (System.currentTimeMillis() - mRequestTime));
                    if (mAssistant != null) mAssistant.forceReset();
                    mIvorManager.cancelSession(AssistantEnums.IvorError.UNEXPECTED_ERROR);
                    mListener.onDeviceError(AssistantEnums.DeviceError.DATA_STREAM_STOPPED, mReceivedPackets);
                }
                else {
                    mReceivedPacketsChecked = mReceivedPackets;
                    mHandler.postDelayed(mNoDataRunnable, NO_DATA_DELAY_MS);
                }
            }
        }
    };

    /**
     * <p>The runnable triggered to check if the {@link Assistant} has responded to the user within the expected
     * time.</p>
     * <p>The expected time is defined by {@link #NO_RESPONSE_DELAY_MS}.</p>
     */
    private final Runnable mNoResponseRunnable = new Runnable() {
        @Override
        public void run() {
            @AssistantEnums.IvorState int state = getIvorState();

            switch (state) {
                case AssistantEnums.IvorState.VOICE_ENDED:
                case AssistantEnums.IvorState.VOICE_ENDING:
                case AssistantEnums.IvorState.SESSION_STARTED:
                case AssistantEnums.IvorState.VOICE_REQUESTED:
                case AssistantEnums.IvorState.VOICE_REQUESTING:
                    Log.w(TAG, "Session reset: assistant did not respond within " + (NO_RESPONSE_DELAY_MS/1000) + "s.");
                    mListener.onAssistantError(AssistantEnums.AssistantError.NO_RESPONSE);
                    forceReset();
                    break;

                case AssistantEnums.IvorState.ANSWER_ENDING:
                case AssistantEnums.IvorState.ANSWER_PLAYING:
                case AssistantEnums.IvorState.ANSWER_STARTING:
                case AssistantEnums.IvorState.CANCELLING:
                case AssistantEnums.IvorState.IDLE:
                case AssistantEnums.IvorState.UNAVAILABLE:
                    break;
            }
        }
    };

    /**
     * <p>Implements the {@link com.qualcomm.qti.libraries.assistant.Assistant.AssistantListener AssistantListener}
     * interface to propagate the requests from the {@link Assistant} to the connected Bluetooth device.</p>
     */
    private final Assistant.AssistantListener mAssistantListener = new Assistant.AssistantListener() {
        @Override // Assistant.AssistantListener
        public void startStreaming() {
            if (mIvorManager.startVoiceStreaming()) {
                mReceivedPackets = 0;
                mReceivedPacketsChecked = 0;
                mRequestTime = System.currentTimeMillis();
                mHandler.removeCallbacks(mNoDataRunnable);
                mHandler.postDelayed(mNoDataRunnable, FIRST_NO_DATA_DELAY_MS);
                mHandler.removeCallbacks(mNoResponseRunnable);
                mHandler.postDelayed(mNoResponseRunnable, NO_RESPONSE_DELAY_MS);
            }
            else {
                Log.w(TAG, "startStreaming: not started.");
            }
        }

        @Override // Assistant.AssistantListener
        public void stopStreaming() {
            Log.i(TAG, "Assistant stopped the voice streaming, received=" + mReceivedPackets + " time="
                    + (System.currentTimeMillis() - mRequestTime));
            mIvorManager.stopVoiceStreaming();
        }

        @Override // Assistant.AssistantListener
        public void onStartPlayingResponse() {
            mIvorManager.onStartPlayingAnswer();
        }

        @Override // Assistant.AssistantListener
        public void onFinishPlayingResponse() {
            mIvorManager.onFinishPlayingAnswer();
        }

        @Override // Assistant.AssistantListener
        public void onError(int error, @AssistantEnums.IvorError int ivorError) {
            mListener.onAssistantError(error);
            mIvorManager.cancelSession(ivorError);
        }

        @Override // Assistant.AssistantListener
        public void onStateUpdated(@AssistantEnums.AssistantState int state,
                                   @AssistantEnums.AssistantState int previous) {
            mListener.onAssistantStateUpdated(state);
            mListener.onSessionStateUpdated(getSessionState());
        }
    };


    // ====== CONSTRUCTOR ===============================================================================

    /**
     * <p>The main constructor of this class.</p>
     *
     * @param listener
     *          To get updates and requests from this manager. These information are independent of the assistant
     *          process itself.
     */
    public AssistantManager(AssistantManagerListener listener) {
        mListener = listener;

        // native lib loading
        this.mSBCHandle = open();
    }


    // ====== PUBLIC METHODS ===============================================================================

    /**
     * <p>To initialise the resources used by this manager.</p>
     *
     * @param context
     *          The context of the application to initialise the objects for the Bluetooth connection.
     */
    public void init(Context context) {
        if (mIvorManager != null) {
            Log.w(TAG, "init: AssistantManager already initialised");
            return;
        }

        ProviderHandler handler = new ProviderHandler(this);
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mIvorManager = new IvorManager(handler);
    }

    /**
     * <p>To release all resources this manager uses prior to destroy this object.</p>
     */
    public void close() {
        if (getDevice() != null) {
            disconnect();
        }

        if (mAssistant != null) {
            mAssistant.close();
        }

        // native lib release
        if (this.mSBCHandle != 0) {
            close(this.mSBCHandle);
        }

        mHandler.removeCallbacks(mNoDataRunnable);
        mHandler.removeCallbacks(mNoResponseRunnable);
    }

    /**
     * <p>To force the manager of the Bluetooth connection and the {@link Assistant} to force a reset of their
     * current assistant session.</p>
     */
    public void forceReset() {
        if (mAssistant != null) mAssistant.forceReset();
        mIvorManager.forceReset();
    }

    /**
     * <p>To set up a new Assistant for the assistant feature.</p>
     * <p>If a previous {@link Assistant} was set up, this method calls {@link Assistant#close()} prior to remove
     * any reference to the previous assistant object.</p>
     *
     * @param assistant
     *          The new {@link Assistant} this manager should use.
     */
    public void setAssistant(Assistant assistant) {
        if (mAssistant != null) {
            mAssistant.close();
            mAssistant = null;
        }

        mAssistant = assistant;
        mAssistant.init();
    }

    /**
     * <p>Gets the BluetoothDevice with which this service is connected or has been connected.</p>
     *
     * @return the BluetoothDevice.
     */
    public BluetoothDevice getDevice() {
        return mIvorManager.getBluetoothDevice();
    }

    /**
     * <p>Gets the current assistant which had been set up with this manager.</p>
     *
     * @return the {@link Assistant} attached to this manager.
     */
    public Assistant getAssistant() {
        return mAssistant;
    }

    /**
     * <p>To disconnect the Bluetooth device from this application.</p>
     *
     * @return True if the disconnection could be initiated, false otherwise.
     */
    public boolean disconnect() {
        if (getDevice() == null) {
            Log.w(TAG, "disconnectDevice: no device to disconnect.");
            return false;
        }

        if (mIvorManager.getState() != AssistantEnums.IvorState.UNAVAILABLE
                || mIvorManager.getState() != AssistantEnums.IvorState.IDLE) {
            mIvorManager.cancelSession(AssistantEnums.IvorError.UNAVAILABLE);
        }

        return mIvorManager.disconnect();
    }

    /**
     * <p>To initiate the Bluetooth connection with the given Bluetooth device. This connection is set up using
     * BR/EDR and RFCOMM.</p>
     * <p>This method returns true if it has been able to successfully initiate a Bluetooth connection with the
     * device.</p>
     * <p>The reasons for the connection initiation to not be successful could be:
     * <ul>
     *     <li>There is already a connected device.</li>
     *     <li>The device is not BR/EDR compatible.</li>
     *     <li>Bluetooth is not available - could have been turned off, etc.</li>
     *     <li>The Bluetooth device address is unknown.</li>
     *     <li>A Bluetooth socket cannot be established with the device.</li>
     *     <li>The given device is null.</li>
     * </ul></p>
     *
     * @param device
     *         The device to connect with over a BR/EDR connection.
     *
     * @return True if the connection had been initialised, false if it can't be started.
     */
    public boolean connect(BluetoothDevice device) {
        if (device == null) {
            Log.w(TAG, "Attempt to connect to a null device.");
            return false;
        }

        if (mIvorManager.getConnectionState() == AssistantEnums.ConnectionState.CONNECTED
                && mIvorManager.getBluetoothDevice().equals(device)) {
            Log.w(TAG, "Connection failed: the device is already connected.");
            return false;
        }
        Log.d(TAG, "connect: ");
        mIvorManager.connect(device);
        return true;
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
     * <p>Gets the current connection state between this provider and a Bluetooth device.</p>
     *
     * @return the current connection state.
     */
    public @AssistantEnums.ConnectionState int getConnectionState() {
        return mIvorManager.getState();
    }

    /**
     * <p>Cancels any current assistant session with the device and the assistant.</p>
     *
     * @param error
     *          The error type to provide to the device.
     */
    public void cancelSession(@AssistantEnums.IvorError int error) {
        if (mAssistant != null) mAssistant.cancelSession();
        mIvorManager.cancelSession(error);
    }


    /**
     * <p>Returns the current session state of the assistant feature.</p>
     *
     * @return the state of the session of one of
     * {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.SessionState SessionState}.
     */
    public @AssistantEnums.SessionState int getSessionState() {
        if (isFeatureDisabled()) {
            return AssistantEnums.SessionState.UNAVAILABLE;
        }

        @AssistantEnums.AssistantState int assistantState = mAssistant != null ? mAssistant.getState()
                : AssistantEnums.AssistantState.UNAVAILABLE;

        switch (assistantState) {
            case AssistantEnums.AssistantState.CANCELLING:
                return AssistantEnums.SessionState.CANCELLING;

            case AssistantEnums.AssistantState.ENDING_STREAMING:
            case AssistantEnums.AssistantState.PENDING:
            case AssistantEnums.AssistantState.STREAMING:
            case AssistantEnums.AssistantState.STARTING:
            case AssistantEnums.AssistantState.SPEAKING:
                return AssistantEnums.SessionState.RUNNING;

            case AssistantEnums.AssistantState.IDLE:
                return AssistantEnums.SessionState.READY;

            case AssistantEnums.AssistantState.UNAVAILABLE:
            case AssistantEnums.AssistantState.CLOSING:
            case AssistantEnums.AssistantState.INITIALISING:
            default:
                return AssistantEnums.SessionState.UNAVAILABLE;
        }
    }

    /**
     * <p>Gets the {@link com.qualcomm.qti.libraries.assistant.Assistant.AssistantListener AssistantListener} to
     * attach to the {@link Assistant} set up with this manager.</p>
     *
     * @return the assistant listener.
     */
    public Assistant.AssistantListener getAssistantListener() {
        return mAssistantListener;
    }


    // ====== PRIVATE METHODS ===============================================================================

    /**
     * <p>This method is called when the {@link ProviderHandler ProviderHandler} receives a message from the
     * <p>This method will act dependently of the received message.</p>
     *
     * @param msg
     *          The message received from the service.
     */
    private void handleMessageFromProvider(Message msg) {
        String handleMessage = "Handle a message from BR/EDR Provider: ";

        switch (msg.what) {
            case AssistantEnums.ProviderMessage.CONNECTION_STATE_HAS_CHANGED:
                @AssistantEnums.ConnectionState int receivedState = (int) msg.obj;
                if (DEBUG_LOGS) {
                    Log.i(TAG, handleMessage + "CONNECTION_STATE_HAS_CHANGED: "
                            + AssistantEnums.getConnectionStateLabel(receivedState));
                }
                onConnectionStateHasChanged(receivedState);
                break;

            case AssistantEnums.ProviderMessage.GAIA_READY:
                if (DEBUG_LOGS) Log.i(TAG, handleMessage + "GAIA_READY");
                break;

            case AssistantEnums.ProviderMessage.ERROR:
                @AssistantEnums.DeviceError int error = (int) msg.obj;
                Log.w(TAG, handleMessage + "ERROR: " + AssistantEnums.getDeviceErrorLabel(error));
                mListener.onDeviceError(error, 0);
                break;

            case AssistantEnums.ProviderMessage.IVOR_MESSAGE:
                handleIvorMessage(msg.arg1, msg.obj);
                break;

            default:
                if (DEBUG_LOGS)
                    Log.d(TAG, handleMessage + "UNKNOWN MESSAGE: " + msg.what + " obj: " + msg.obj);
                break;
        }
    }

    /**
     * <p>This method is called when the Provider dispatches a
     * {@link AssistantEnums.ProviderMessage#CONNECTION_STATE_HAS_CHANGED CONNECTION_STATE_HAS_CHANGED}
     * message.</p>
     * <p>This method dispatches the information to its listener(s) and acts upon the value for the assistant feature
     * .</p>
     * <p>If the device is disconnected and there is a device waiting to be connected with, this method attempts to
     * connection with this device.</p>
     *
     * @param receivedState
     *              The new connection state
     */
    private void onConnectionStateHasChanged(int receivedState) {
        @AssistantEnums.ConnectionState int state =
                receivedState == AssistantEnums.ConnectionState.CONNECTED ? AssistantEnums.ConnectionState.CONNECTED :
                        receivedState == AssistantEnums.ConnectionState.CONNECTING ? AssistantEnums.ConnectionState.CONNECTING :
                                receivedState == AssistantEnums.ConnectionState.DISCONNECTING ? AssistantEnums.ConnectionState.DISCONNECTING :
                                        AssistantEnums.ConnectionState.DISCONNECTED;

        mListener.onDeviceStateUpdated(state);

        // device disconnected in order to connect with another one
        if (receivedState == AssistantEnums.ConnectionState.DISCONNECTED && mDeviceToConnect != null) {
            mIvorManager.connect(mDeviceToConnect);
        }

        if (mAssistant != null && receivedState == AssistantEnums.ConnectionState.DISCONNECTED) {
            mAssistant.cancelSession();
        }
    }

    /**
     * <p>This method is called when this manager handles a
     * {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.ProviderMessage#IVOR_MESSAGE IVOR_MESSAGE} from the
     * <p>This method will act dependently of the received message.</p>
     *
     * @param message
     *          The message which contains the
     *          {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.ProviderMessage#IVOR_MESSAGE IVOR_MESSAGE}.
     * @param object
     *          Any complementary content for the
     *          {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.ProviderMessage#IVOR_MESSAGE IVOR_MESSAGE}.
     */
    private void handleIvorMessage(int message, Object object) {
        @SuppressWarnings("UnusedAssignment") String handleMessage = "Handle an IVOR message: ";

        switch (message) {
            case AssistantEnums.IvorMessage.START_SESSION:
                if (DEBUG_LOGS) Log.i(TAG, handleMessage + "START_SESSION");
                if (!onReceivedStartVoiceSession()) {
                    mIvorManager.cancelSession(AssistantEnums.IvorError.INCORRECT_STATE);
                }
                break;

            case AssistantEnums.IvorMessage.VOICE_DATA:
                byte[] data = (byte[]) object;
                if (DEBUG_LOGS) Log.i(TAG, handleMessage + "VOICE_DATA: length=" + data.length);
                onReceivedVoiceData(data);
                break;

            case AssistantEnums.IvorMessage.CANCEL_SESSION:
                int error = (int) object;
                if (DEBUG_LOGS) Log.i(TAG, handleMessage + "CANCEL_SESSION: error=" + error);
                onReceivedCancelVoiceSession(error);
                break;

            case AssistantEnums.IvorMessage.VOICE_END:
                if (DEBUG_LOGS) Log.i(TAG, handleMessage + "VOICE_END");
                onReceivedEndData();
                break;

            case AssistantEnums.IvorMessage.STATE:
                @AssistantEnums.IvorState int ivorState = (int) object;
                if (DEBUG_LOGS) Log.i(TAG, handleMessage + "STATE : state=" + ivorState);
                mListener.onIvorStateUpdated(ivorState);
                break;

            default:
                if (DEBUG_LOGS)
                    Log.d(TAG, handleMessage + "UNKNOWN MESSAGE: " + message + " obj: " + object);
                break;
        }
    }

    /**
     * <p>Called when this manager has handled a
     * {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorMessage#START_SESSION START_SESSION} message
     * from its Bluetooth provider.</p>
     * <p>This method checks if the assistant feature is available and asks the {@link Assistant} to start a session
     * by calling {@link Assistant#startSession() startSession()}.</p>
     *
     * @return True if the assistant feature is enabled, false otherwise.
     */
    private boolean onReceivedStartVoiceSession() {
        if (isFeatureDisabled()) {
            Log.i(TAG, "onReceivedStartVoiceSession: feature not available.");
            mListener.onSessionStateUpdated(getSessionState()); // General error state
            return false;
        }

        return mAssistant.startSession();
    }

    /**
     * <p>Called when this manager has handled a
     * {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorMessage#VOICE_DATA VOICE_DATA} message
     * from its Bluetooth provider.</p>
     * <p>This method expects to receive SBC packets of 64 bytes contained into the <code>data</code> parameter. It
     * decodes each SBC packet which are then transmitted to the {@link Assistant Assistant} for treatment of the
     * voice request.</p>
     *
     * @param data
     *          one or more SBC packets as sent by the Bluetooth device.
     */
    private void onReceivedVoiceData(byte[] data) {
        int offset = 0;

        mReceivedPackets++;

        while (offset < data.length) {
            byte[] chunk = new byte[AssistantConsts.SBC_FRAME_SIZE];
            int length = data.length - offset;
            length = (length > AssistantConsts.SBC_FRAME_SIZE) ? AssistantConsts.SBC_FRAME_SIZE : length;
            System.arraycopy(data, offset, chunk, 0, length);
            mAssistant.sendData(decode(chunk));
            offset += length;
        }
    }

    /**
     * <p>Called when this manager has handled a
     * {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorMessage#CANCEL_SESSION CANCEL_SESSION} message
     * from its Bluetooth provider.</p>
     * <p>This method cancels any assistant session and propagates the information to its
     * {@link AssistantManagerListener}.</p>
     *
     * @param error
     *          The {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorError IvorError} the Bluetooth
     *          device sent.
     */
    private void onReceivedCancelVoiceSession(int error) {
        mListener.onIvorError(error);
        mAssistant.cancelSession();
    }

    /**
     * <p>Called when this manager has handled a
     * {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorMessage#VOICE_END VOICE_END} message
     * from its Bluetooth provider.</p>
     * <p>This manager informs the {@link Assistant Assistant} that the device has ended the voice streaming by
     * calling {@link Assistant#endDataStream()}.</p>
     */
    private void onReceivedEndData() {
        mAssistant.endDataStream();
    }

    /**
     * <p>To check if the assistant feature is disabled.</p>
     * <p>This method checks if a device is connected, that the IvorManager is available as well as an assistant had
     * been set up. It also asks to the {@link AssistantManagerListener} by calling
     * {@link AssistantManagerListener#isAssistantFeatureAvailable()} if any other constraints could apply.</p>
     *
     * @return True if the feature is NOT available, false if it is available.
     */
    private boolean isFeatureDisabled() {
        return !mListener.isAssistantFeatureAvailable() || mAssistant == null
                || mIvorManager.getConnectionState() != AssistantEnums.ConnectionState.CONNECTED
                || mIvorManager.getState() == AssistantEnums.IvorState.UNAVAILABLE;
    }

    /**
     * <p>This method is called to decode chunks of 64 bytes which are encode with SBC.</p>
     *
     * @param data
     *          64 bytes of SBC data with its 3 bytes header.
     *
     * @return decoded raw PCM data
     */
    private byte[] decode(byte[] data) {
        return decode(mSBCHandle, data);
    }


    // ====== INNER CLASSES ===============================================================================

    /**
     */
    private static class ProviderHandler extends Handler {

        /**
         * The reference to this service.
         */
        final WeakReference<AssistantManager> mmReference;

        /**
         * The constructor for this handler.
         *
         * @param manager
         *            this service.
         */
        ProviderHandler(AssistantManager manager) {
            super();
            mmReference = new WeakReference<>(manager);
        }

        @Override // Handler
        public void handleMessage(Message msg) {
            AssistantManager manager = mmReference.get();
            manager.handleMessageFromProvider(msg);
        }
    }


    // ====== INNER INTERFACES ===============================================================================

    /**
     * <p>An interface to propagate information from this manager to any interested listener.</p>
     * <p>A listener can only be registered when creating an instance of this {@link AssistantManager}./p>
     */
    public interface AssistantManagerListener {
        /**
         * <p>Called when an error occurs within the {@link Assistant}.</p>
         *
         * @param error
         *          The int value provided by the assistant as the first parameter of
         *          {@link Assistant.AssistantListener#onError(int, int)}.
         */
        void onAssistantError(int error);

        /**
         * <p>Called when an error occurs with the device</p>
         *
         * @param error
         *          The {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.DeviceError DeviceError} which
         *          occurred.
         * @param value
         *          Any complementary information, see
         *          {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.DeviceError DeviceError} for more
         *          information.
         */
        void onDeviceError(@AssistantEnums.DeviceError int error, int value);

        /**
         * <p>Called when the device has cancelled the assistant session by sending an error code.</p>
         *
         * @param error
         *          The {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorError IvorError} sent by the
         *          device.
         */
        void onIvorError(int error);

        /**
         * <p>Called when the device connection state had been updated.</p>
         *
         * @param state
         *          The new device state as one of
         *          {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.ConnectionState ConnectionState}.
         */
        void onDeviceStateUpdated(@AssistantEnums.ConnectionState int state);

        /**
         * <p>Called when the session state of this manager had been updated.</p>
         *
         * @param state
         *          The new device state as one of
         *          {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.SessionState SessionState}.
         */
        void onSessionStateUpdated(@AssistantEnums.SessionState int state);

        /**
         * <p>Called when the IVOR process state had been updated.</p>
         *
         * @param state
         *          The new device state as one of
         *          {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorState IvorState}.
         */
        void onIvorStateUpdated(@AssistantEnums.IvorState int state);

        /**
         * <p>Called when the {@link Assistant} state had been updated.</p>
         *
         * @param state
         *          The new device state as one of
         *          {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.AssistantState AssistantState}.
         */
        void onAssistantStateUpdated(@AssistantEnums.AssistantState int state);

        /**
         * <p>To get the availability of the assistant feature depending on the application constraints - if they exist
         * .</p>
         *
         * @return True if all requirements are met, false otherwise.
         */
        boolean isAssistantFeatureAvailable();
    }


    // ====== NATIVE ==========================================================================

    private long mSBCHandle;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("SbcLibWrapper");
    }

    public native long open();
    public native void close(long handle);
    public native byte[] decode(long handle, byte[] frame);
}

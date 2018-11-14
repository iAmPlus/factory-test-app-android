/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.service;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.qualcomm.qti.libraries.assistant.Assistant;
import com.qualcomm.qti.libraries.assistant.AssistantEnums;
import com.qualcomm.qti.libraries.assistant.AssistantManager;
import com.qualcomm.qti.voiceassistant.Consts;
import com.qualcomm.qti.voiceassistant.Enums;
import com.qualcomm.qti.voiceassistant.assistant.LoopbackAssistant;
import com.qualcomm.qti.voiceassistant.receivers.A2DPReceiver;
import com.qualcomm.qti.voiceassistant.receivers.BluetoothStateReceiver;
import com.qualcomm.qti.voiceassistant.receivers.CallReceiver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Defines the main controller of this application which links the user interface in order for the UI to only have
 * to interact with this interface and not any specific methods from the background assistant process.</p>
 * <p>This service instantiates and initialises the {@link AssistantManager AssistantManager} from the
 * <code>assistantlibrary</code> which manages the assistant process: the communication between the assistant and
 * the Bluetooth device.</p>
 * <p>This service also links the {@link AssistantManager AssistantManager} to the implemented {@link Assistant} it
 * shall use.</p>
 * <p>This controller manages the availability of the assistant depending on the A2DP profile connection state, the
 * network connection, the Bluetooth state and the presence of outgoing or incoming call through the use of
 * {@link BroadcastReceiver BroadcastReceiver}.</p>
 * <p>Once this service is connected to a user interface, its {@link #init()} method must be called.</p>
 */
public class VoiceAssistantService extends Service implements CallReceiver.CallListener,
        BluetoothStateReceiver.BluetoothStateListener, A2DPReceiver.A2DPListener,
        AssistantManager.AssistantManagerListener {

    // ====== CONSTS FIELDS ========================================================================

    /**
     * <p>To show the debugging logs of this class.</p>
     */
    private static final boolean DEBUG_LOGS = Consts.Debug.VOICE_ASSISTANT_SERVICE;
    /**
     * <p>The tag to display for logs.</p>
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String TAG = "VoiceAssistantService";


    // ====== PRIVATE FIELDS =======================================================================

    /**
     * <p>The number of required states for the assistant to be available. This is the number of constraints available
     * within the {@link ConstraintsType} enumeration.</p>
     */
    private static final int CONSTRAINTS = 3;
    /**
     * A boolean array to keep the current availability of the constraints.
     */
    private final boolean[] mConstraints = new boolean[CONSTRAINTS];
    /**
     * <p>The listeners which wants to be updated of events from this service.</p>
     */
    private final List<Handler> mAppListeners = new ArrayList<>();
    /**
     * <p>The binder to return to the instance which will bind this service.</p>
     */
    private final IBinder mBinder = new LocalBinder();
    /**
     * The {@link BroadcastReceiver} to register to get events about the A2DP Profile.
     */
    private A2DPReceiver mA2DPReceiver;
    /**
     * The {@link BroadcastReceiver} to register to get events about incoming calls.
     */
    private CallReceiver mCallReceiver;
    /**
     * The {@link BroadcastReceiver} to register to get events about the Bluetooth state on the device.
     */
    private BluetoothStateReceiver mBluetoothStateReceiver;
    /**
     * The A2DP BluetoothProfile proxy to get current information about the A2DP Profile.
     */
    private BluetoothA2dp mBluetoothA2dp;
    /**
     * <p>The reference to the object which manages the assistant process.</p>
     */
    private final AssistantManager mAssistantManager = new AssistantManager(this);

    /**
     * The service listener to get connection state with this service about BluetoothProfile. In this service only
     * A2DP is used.
     */
    private final BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                mBluetoothA2dp = (BluetoothA2dp) proxy;
                onA2DPProfileConnected();
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) {
                mBluetoothA2dp = null;
            }
        }
    };

    public void voiceEnd() {
        mAssistantManager.onReceivedEndUser();
    }


    // ====== ENUM =================================================================================

    /**
     * <p>This enumeration defines all the constraints to define the assistant feature as available.</p>
     */
    @IntDef({ ConstraintsType.ASSISTANT, ConstraintsType.DEVICE, ConstraintsType.NO_CALL })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ConstraintsType {
        /**
         * <p>To be available, the feature requires an {@link Assistant} to be selected/instantiated.</p>
         */
        int ASSISTANT = 0;
        /**
         * <p>The assistant feature requires a Bluetooth device to be connected with.</p>
         */
        int DEVICE = 1;
        /**
         * <p>To be available the assistant feature requires that the Android device is not in a call.</p>
         */
        int NO_CALL = 2;
    }


    // ====== PUBLIC METHODS ========================================================================

    /**
     * <p>To set up the assistant to use for the assistant feature.</p>
     * <p>This method creates an instance of the assistant and updates the {@link AssistantManager}.</p>
     */
    public void setAssistant() {
        Assistant assistant = new LoopbackAssistant(this, mAssistantManager.getAssistantListener());
        mAssistantManager.setAssistant(assistant);
    }

    /**
     * <p>For the current states of all displayed information to be sent to the user interface.</p>
     */
    public void getStates() {
        BluetoothDevice device = getDevice();
        if (device != null) {
            notifyListener(device, Enums.ServiceMessage.DEVICE, Enums.DeviceMessage.DEVICE_INFORMATION);
            notifyListener(getConnectionState(), Enums.ServiceMessage.DEVICE, Enums.DeviceMessage.CONNECTION_STATE);
            notifyListener(getIvorState(), Enums.ServiceMessage.DEVICE, Enums.DeviceMessage.IVOR_STATE);
        }

        LoopbackAssistant assistant = (LoopbackAssistant) mAssistantManager.getAssistant();
        if (assistant != null) {
            notifyListener(assistant.getState(), Enums.ServiceMessage.ASSISTANT, Enums.AssistantMessage.STATE);
            notifyListener(assistant.getType(), Enums.ServiceMessage.ASSISTANT, Enums.AssistantMessage.TYPE);
        }

        notifyListener(getSessionState(), Enums.ServiceMessage.SESSION);
    }

    /**
     * <p>Initialises this service process.</p>
     */
    public void init() {
        checkBluetoothAvailability();
    }

    /**
     * <p>Gets the BluetoothDevice with which this service is connected or has been connected.</p>
     *
     * @return the BluetoothDevice.
     */
    public BluetoothDevice getDevice() {
        return mAssistantManager.getDevice();
    }

    /**
     * <p>Gets the current connection state between this service and a Bluetooth device.</p>
     *
     * @return the connection state.
     */
    public @AssistantEnums.ConnectionState
    int getConnectionState() {
        return mAssistantManager.getConnectionState();
    }

    /**
     * <p>To force the assistant feature to be reset.</p>
     */
    public void forceReset() {
        mAssistantManager.forceReset();
    }

    /**
     * <p>Adds the given handler to the targets list for messages from this service.</p>
     * <p>This method must be synchronized to avoid access to the list of listeners while modifying it.</p>
     *
     * @param handler
     *         The Handler to add.
     */
    public synchronized void addHandler(Handler handler) {
        if (!mAppListeners.contains(handler)) {
            this.mAppListeners.add(handler);
        }
    }

    /**
     * <p>Removes the given handler from the targets list for messages from this service.</p>
     * <p>This method must be synchronized to avoid access to the list of listeners while modifying it.</p>
     *
     * @param handler
     *         The Handler to remove.
     */
    public synchronized void removeHandler(Handler handler) {
        if (mAppListeners.contains(handler)) {
            this.mAppListeners.remove(handler);
        }
    }


    // ====== SERVICE METHODS ========================================================================

    @Override // Service
    public IBinder onBind(Intent intent) {
        registerReceivers(true);
        return mBinder;
    }

    @Override // Service
    public boolean onUnbind(Intent intent) {
        if (mAppListeners.isEmpty() && getDevice() != null) {
            disconnectDevice();
        }
        return super.onUnbind(intent);
    }

    @Override // Service
    public void onCreate() {
        super.onCreate();

        mAssistantManager.init(this);

        updateConstraints(ConstraintsType.NO_CALL, isInACall());
    }

    @Override // Service
    public void onDestroy() {
        mAssistantManager.close();
        registerReceivers(false);
        closeA2DP();

        if (DEBUG_LOGS) {
            Log.i(TAG, "Service destroyed");
        }

        super.onDestroy();
    }


    // ====== CONNECTION METHODS ========================================================================

    /**
     * <p>To connect to the given {@link BluetoothDevice}.</p>
     * <p>The connection result is reported asynchronously through the sending of a
     * {@link Enums.ServiceMessage#DEVICE DEVICE} message with the sub message
     * {@link Enums.DeviceMessage#CONNECTION_STATE CONNECTION_STATE} to any handler which has registered
     * through the {@link #addHandler(Handler) addHandler} method.</p>
     *
     * @return Return <code>true</code> if the operation has successfully been initiated.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean connectToDevice(BluetoothDevice device) {
        return mAssistantManager.connect(device);
        // then wait for AssistantManagerListener.onDeviceStateUpdate to get a message
    }

    /**
     * <p>To disconnect a connected device.</p>
     * <p>The disconnection result is reported asynchronously through the sending of a
     * {@link Enums.ServiceMessage#DEVICE DEVICE} message with the sub message
     * {@link Enums.DeviceMessage#CONNECTION_STATE CONNECTION_STATE} to any handler which has registered
     * through the {@link #addHandler(Handler) addHandler} method.</p>
     *
     * @return Return <code>true</code> if the operation has successfully been initiated.
     */
    @SuppressWarnings("UnusedReturnValue")
    private boolean disconnectDevice() {
        return mAssistantManager.disconnect();
        // then wait for AssistantManagerListener.onDeviceStateUpdate to get a message
    }


    // ====== ASSISTANT METHODS ======================================================================

    @Override // AssistantManager.AssistantManagerListener
    public void onAssistantError(int error) {
        notifyListener(null, Enums.ServiceMessage.ERROR, Enums.ErrorType.ASSISTANT, error);
    }

    @Override // AssistantManager.AssistantManagerListener
    public void onDeviceError(@AssistantEnums.DeviceError int error, int value) {
        notifyListener(value, Enums.ServiceMessage.ERROR, Enums.ErrorType.DEVICE, error);
    }

    @Override // AssistantManager.AssistantManagerListener
    public void onIvorError(int error) {
        notifyListener(error, Enums.ServiceMessage.ERROR, Enums.ErrorType.IVOR);
    }

    @Override // AssistantManager.AssistantManagerListener
    public void onDeviceStateUpdated(@AssistantEnums.ConnectionState int state) {
        notifyListener(state, Enums.ServiceMessage.DEVICE, Enums.DeviceMessage.CONNECTION_STATE);
    }

    @Override // AssistantManager.AssistantManagerListener
    public void onSessionStateUpdated(@AssistantEnums.SessionState int state) {
        notifyListener(state, Enums.ServiceMessage.SESSION);
    }

    @Override // AssistantManager.AssistantManagerListener
    public void onIvorStateUpdated(@AssistantEnums.IvorState int state) {
        notifyListener(state, Enums.ServiceMessage.DEVICE, Enums.DeviceMessage.IVOR_STATE);
        updateConstraints(ConstraintsType.DEVICE, state != AssistantEnums.IvorState.UNAVAILABLE);
    }

    @Override // AssistantManager.AssistantManagerListener
    public void onAssistantStateUpdated(@AssistantEnums.AssistantState int state) {
        notifyListener(state, Enums.ServiceMessage.ASSISTANT, Enums.AssistantMessage.STATE);
        updateConstraints(ConstraintsType.ASSISTANT, state != AssistantEnums.AssistantState.UNAVAILABLE);
    }

    @Override // AssistantManager.AssistantManagerListener
    public boolean isAssistantFeatureAvailable() {
        return mConstraints[ConstraintsType.DEVICE]
                && mConstraints[ConstraintsType.NO_CALL]
                && mConstraints[ConstraintsType.ASSISTANT];
    }


    // ====== RECEIVERS METHODS ======================================================================

    @Override // IncomingCallReceiver.IncomingCallListener
    public void onACall(boolean calling) {
        if (calling) {
            @AssistantEnums.SessionState int state = getSessionState();
            mAssistantManager.cancelSession(AssistantEnums.IvorError.CALL);
            if (state == AssistantEnums.SessionState.RUNNING) {
                notifyListener(null, Enums.ServiceMessage.ERROR, Enums.ErrorType.CALL);
            }
        }

        updateConstraints(ConstraintsType.NO_CALL, !calling);
    }

    @Override // BluetoothStateReceiver.BluetoothStateListener
    public void onBluetoothDisabled() {
        notifyListener(getSessionState(), Enums.ServiceMessage.SESSION);
        notifyListener(null, Enums.ServiceMessage.ACTION, Enums.ActionMessage.ENABLE_BLUETOOTH);
        mAssistantManager.cancelSession(AssistantEnums.IvorError.UNAVAILABLE);
    }

    @Override // BluetoothStateReceiver.BluetoothStateListener
    public void onBluetoothEnabled() {
        checkBluetoothAvailability();
    }

    @Override // A2DPReceiver.A2DPListener
    public void onA2DPDeviceConnected(BluetoothDevice device) {
        if (device != null) {
            notifyListener(device, Enums.ServiceMessage.DEVICE, Enums.DeviceMessage.DEVICE_INFORMATION);
            connectToDevice(device);
        }
    }

    @Override // A2DPReceiver.A2DPListener
    public void onA2DPDeviceDisconnected(BluetoothDevice device) {
        if (device.equals(getDevice())) {
            disconnectDevice();
        }
    }


    // ====== PRIVATE METHODS FOR BLUETOOTH ================================================================

    /**
     * <p>To connection the A2DP profile with this service.</p>
     * <p>This is used when the Bluetooth feature is available to check if an A2DP device is connected with the
     * Android system.</p>
     *
     * @param btAdapter
     *          The Bluetooth adapter to use to connect a Bluetooth profile.
     */
    private void initA2DP(BluetoothAdapter btAdapter) {
        // Establish connection to the proxy.
        btAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.A2DP);
        // then wait for mProfileListener.onServiceConnected to be called
    }

    /**
     * <p>To disconnect the A2DP profile to this service.</p>
     * <p>When the Bluetooth A2DP profile is not needed anymore, this must be called to release the resources.</p>
     */
    private void closeA2DP() {
        if (mBluetoothA2dp != null) {
            // Get the default adapter
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            // Close proxy connection after use.
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothA2dp);
            // then wait for mProfileListener.onServiceDisconnected to be called
        }
    }

    /**
     * <p>When the A2DP profile is connected this method gets the list of connected A2DP Bluetooth device.</p>
     * <p>If there is no device, this method notifies the user interface that a user action is required.</p>
     * <p>If there is one device connected, it provides the device to the assistant process to be the device to use.</p>
     * <p>If there is more than one device connected - this is not supported by Android yet - this method notifies
     * the user interface that a user action is required.</p>
     */
    private void onA2DPProfileConnected() {
        List<BluetoothDevice> devices = mBluetoothA2dp.getConnectedDevices();

        if (devices.isEmpty()) {
            notifyListener(null, Enums.ServiceMessage.ACTION, Enums.ActionMessage.CONNECT_A_DEVICE);
        }
        else if (devices.size() == 1) {
            BluetoothDevice device = devices.get(0);
            notifyListener(device, Enums.ServiceMessage.DEVICE, Enums.DeviceMessage.DEVICE_INFORMATION);
            connectToDevice(device);
        }
        else {
            notifyListener(devices, Enums.ServiceMessage.ACTION, Enums.ActionMessage.SELECT_A_DEVICE);
        }

        closeA2DP();
    }

    /**
     * <p>This method checks if the Bluetooth is enabled within the Android device.</p>
     */
    private void checkBluetoothAvailability() {
        // request BT activation
        BluetoothAdapter btAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            notifyListener(null, Enums.ServiceMessage.ACTION, Enums.ActionMessage.ENABLE_BLUETOOTH);
        }
        else {
            initA2DP(btAdapter);
        }
    }


    // ====== PRIVATE METHODS ==========================================================================

    /**
     * <p>To send messages to the registered listeners.</p>
     *
     */
    private void notifyListener(Object content, int... arguments) {
        int NO_ARGUMENT = -1;
        int length = arguments.length;
        int what = length > 0 ? arguments[0] : NO_ARGUMENT;
        int argument1 = length > 1 ? arguments[1] : NO_ARGUMENT;
        int argument2 = length > 2 ? arguments[2] : NO_ARGUMENT;

        if (!mAppListeners.isEmpty()) {
            for (int i=0; i<mAppListeners.size(); i++) {
                mAppListeners.get(i).obtainMessage(what, argument1, argument2, content).sendToTarget();
            }
        }
    }

    /**
     * <p>To register or unregister all broadcast receivers used with this service.</p>
     *
     * @param register
     *          True to register the receivers, false to unregister them.
     */
    private void registerReceivers(boolean register) {
        registerCallReceiver(register);
        registerBluetoothReceiver(register);
        registerA2DPReceiver(register);
    }

    /**
     * <p>To register or unregister an {@link A2DPReceiver}.</p>
     * <p>If there is already an {@link A2DPReceiver} registered, the action will be ignored.</p>
     *
     * @param register
     *              true to register an {@link A2DPReceiver}, false to unregister.
     */
    private void registerA2DPReceiver(boolean register) {
        if (register && mA2DPReceiver == null) {
            mA2DPReceiver = new A2DPReceiver(this);
            IntentFilter filter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            this.registerReceiver(mA2DPReceiver, filter);
        }
        else if (!register && mA2DPReceiver != null) {
            unregisterReceiver(mA2DPReceiver);
            mA2DPReceiver = null;
        }
    }

    /**
     * <p>To register or unregister an {@link CallReceiver}.</p>
     * <p>If there is already an {@link CallReceiver} registered, the action will be ignored.</p>
     *
     * @param register
     *              true to register an {@link CallReceiver}, false to unregister.
     */
    private void registerCallReceiver(boolean register) {
        if (register && mCallReceiver == null) {
            mCallReceiver = new CallReceiver(this);
            IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            this.registerReceiver(mCallReceiver, filter);
        }
        else if (!register && mCallReceiver != null) {
            unregisterReceiver(mCallReceiver);
            mCallReceiver.release();
            mCallReceiver = null;
        }
    }

    /**
     * <p>To register or unregister an {@link BluetoothStateReceiver}.</p>
     * <p>If there is already an {@link BluetoothStateReceiver} registered, the action will be ignored.</p>
     *
     * @param register
     *              true to register an {@link BluetoothStateReceiver}, false to unregister.
     */
    private void registerBluetoothReceiver(boolean register) {
        if (register && mBluetoothStateReceiver == null) {
            mBluetoothStateReceiver = new BluetoothStateReceiver(this);
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            this.registerReceiver(mBluetoothStateReceiver, filter);
        }
        else if (!register && mBluetoothStateReceiver != null) {
            unregisterReceiver(mBluetoothStateReceiver);
            mBluetoothStateReceiver = null;
        }
    }

    /**
     * <p>This method updates the given constraint state.</p>
     * <p>It also updates the {@link AssistantManager} and the user interface with the new state.</p>
     *
     * @param constraint
     *          The constraint to update.
     * @param validated
     *          True if the requirements are met for the constraint, false otherwise.
     */
    private void updateConstraints(@ConstraintsType int constraint, boolean validated) {
        if (constraint < 0 && constraint >= mConstraints.length) {
            return;
        }

        boolean previousValue = mConstraints[constraint];
        if (previousValue == validated) {
            // no change
            return;
        }

        boolean previousAvailability = isAssistantFeatureAvailable();

        mConstraints[constraint] = validated;
        boolean available = isAssistantFeatureAvailable();

        if (previousAvailability != available && !available) {
            mAssistantManager.cancelSession(AssistantEnums.IvorError.UNAVAILABLE);
        }

        notifyListener(getSessionState(), Enums.ServiceMessage.SESSION);
    }

    /**
     * <p>Returns the current session state of the assistant.</p>
     */
    private @AssistantEnums.SessionState int getSessionState() {
        if (!isAssistantFeatureAvailable()) {
            return AssistantEnums.SessionState.UNAVAILABLE;
        }

        return mAssistantManager.getSessionState();
    }

    /**
     * <p>gives the current IVOR state of the Bluetooth device. If there is no Bluetooth device, it returns
     * {@link com.qualcomm.qti.libraries.assistant.AssistantEnums.IvorState#UNAVAILABLE UNAVAILABLE}.</p>
     */
    private @AssistantEnums.IvorState int getIvorState() {
        return mAssistantManager.getIvorState();
    }

    /**
     * <p>This method checks if the Android device is in a call.</p>
     */
    private boolean isInACall() {
        final TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.getCallState() == TelephonyManager.CALL_STATE_IDLE;
    }


    // ====== INNER CLASS ==========================================================================

    /**
     * <p>The class which allows an entity to communicate with this service when it is bound.</p>
     */
    public class LocalBinder extends Binder {
        /**
         * <p>To retrieve the binder service.</p>
         *
         * @return the service.
         */
        public VoiceAssistantService getService() {
            return VoiceAssistantService.this;
        }
    }

}

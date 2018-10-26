/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.qti.libraries.assistant.AssistantEnums;
import com.qualcomm.qti.voiceassistant.BuildConfig;
import com.qualcomm.qti.voiceassistant.Consts;
import com.qualcomm.qti.voiceassistant.Enums;
import com.qualcomm.qti.voiceassistant.R;
import com.qualcomm.qti.voiceassistant.service.VoiceAssistantService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * <p>This fragment is used to display the current status of the assistant feature.</p>
 * <p>This fragment inflates the layout {@link R.layout#fragment_status fragment_status}.</p>
 * <p>This view contains a {@link SessionView} to display the general state of the feature, a {@link Card Card} to
 * give the state of the assistant, a {@link Card Card} for the device state and some buttons for the user to force a
 * reset of the feature and to send some prerecorded data.</p>
 *
 */
public class StatusFragment extends Fragment implements Card.CardListener, View.OnClickListener {
    private static StatusFragment mStatusFragment;

    // ====== PRIVATE FIELDS ========================================================================

    /**
     * The {@link Card} which displays the device information and states.
     */
    private Card mDeviceCard;
    /**
     * The {@link Card} which displays the assistant information and states.
     */
    private Card mAssistantCard;
    /**
     * The {@link SessionView} which displays the state of the assistant feature.
     */
    private SessionView mSessionView;

    /**
     * The service used to communicate with a Bluetooth device for the assistant feature.
     */
    private VoiceAssistantService mService;
    /**
     * The service connection object to manage the service bind and unbind.
     */
    private final ServiceConnection mServiceConnection = new ActivityServiceConnection();
    /**
     * The handler used by the service to communicate with this activity.
     */
    private ActivityHandler mHandler;
    /**
     * To know if this activity is in the pause state.
     */
    private boolean mIsPaused;
    private Context mContext;


    // ====== STATIC METHODS ========================================================================

    /**
     * Returns a new instance of this fragment.
     */
    public void init(Context context) {
        mContext = context;
        Log.d("abhay", "init: ");
        mHandler = new ActivityHandler(this);

        if(mService != null) {
            onLoopbackAssistantSelected();
        }
        if (mService == null) {
            bindService();
        }
        initValues();
    }



    // ====== FRAGMENT METHODS ========================================================================

    @Override // Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override // Fragment
    public void onResume() {
        Log.d("abhay", "onResume: ");
        super.onResume();
        mIsPaused = false;
        if(mService != null) {
            onLoopbackAssistantSelected();
        }
        if (mService == null) {
            bindService();
        }
        initValues();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("abhay", "onPause: ");
        mIsPaused = true;
    }

    @Override // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate the corresponding layout
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        // get views
        mDeviceCard = view.findViewById(R.id.card_device);
        mDeviceCard.setListener(this);
        mAssistantCard = view.findViewById(R.id.card_assistant);
        mAssistantCard.setListener(this);
        mSessionView = view.findViewById(R.id.session_view);

        View buttonReset = view.findViewById(R.id.bt_reset);
        buttonReset.setOnClickListener(this);
        Log.d("abhay", "onCreateView: ");
        return view;
    }


    // ====== CARD LISTENER METHODS ========================================================================

    @Override // Card.CardListener
    public void onSelectAssistant() {
    }

    @Override // Card.CardListener
    public void onSelectDevice() {
    }


    // ====== PUBLIC METHODS ========================================================================

    /**
     * <p>To refresh the display of the device information with new values.</p>
     *
     * @param name
     *          The device name to display.
     * @param address
     *          The device address to display.
     */
    public void refreshDevice(String name, String address) {
        mDeviceCard.displayInformation(true);
        mDeviceCard.refreshCard(name, address);
    }

    /**
     * <p>To refresh the display of the device connection with the given parameter.</p>
     *
     * @param state
     *          The new state to display.
     */
    public void refreshDeviceConnectionState(@AssistantEnums.ConnectionState int state) {
        int text;
        @StatusView.StatusType int type;
        switch (state) {
            case AssistantEnums.ConnectionState.CONNECTED:
                text = R.string.device_state_connected;
                type = StatusView.StatusType.SUCCESS;
                break;
            case AssistantEnums.ConnectionState.DISCONNECTED:
                text = R.string.device_state_disconnected;
                type = StatusView.StatusType.WARNING;
                break;
            case AssistantEnums.ConnectionState.CONNECTING:
                text = R.string.device_state_connecting;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.ConnectionState.DISCONNECTING:
                text = R.string.device_state_disconnecting;
                type = StatusView.StatusType.PROGRESS;
                break;
            default:
                text = R.string.device_state_unknown;
                type = StatusView.StatusType.ERROR;
                break;
        }
        mDeviceCard.refreshStatus(Card.Status.DEVICE_CONNECTION_STATE, type, text);
    }

    /**
     * <p>To refresh the IVOR state of the device on the display.</p>
     *
     * @param state
     *          The new IVOR state to display.
     */
    public void refreshDeviceIvorState(@AssistantEnums.IvorState int state) {

        int text;
        @StatusView.StatusType int type;

        switch (state) {
            case AssistantEnums.IvorState.ANSWER_ENDING:
                text = R.string.ivor_state_answer_ending;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.IvorState.ANSWER_PLAYING:
                text = R.string.ivor_state_answer_playing;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.IvorState.ANSWER_STARTING:
                text = R.string.ivor_state_answer_starting;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.IvorState.CANCELLING:
                text = R.string.ivor_state_cancelling;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.IvorState.IDLE:
                text = R.string.ivor_state_idle;
                type = StatusView.StatusType.SUCCESS;
                break;
            case AssistantEnums.IvorState.SESSION_STARTED:
                text = R.string.ivor_state_session_started;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.IvorState.UNAVAILABLE:
                text = R.string.ivor_state_unavailable;
                type = StatusView.StatusType.ERROR;
                break;
            case AssistantEnums.IvorState.VOICE_ENDED:
                text = R.string.ivor_state_voice_ended;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.IvorState.VOICE_ENDING:
                text = R.string.ivor_state_voice_ending;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.IvorState.VOICE_REQUESTED:
                text = R.string.ivor_state_voice_requested;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.IvorState.VOICE_REQUESTING:
                text = R.string.ivor_state_voice_requesting;
                type = StatusView.StatusType.PROGRESS;
                break;
            default:
                text = R.string.ivor_state_unknown;
                type = StatusView.StatusType.ERROR;
                break;
        }

        mDeviceCard.refreshStatus(Card.Status.DEVICE_IVOR_STATUS, type, text);
    }

    /**
     * <p>To refresh the assistant information on the display.</p>
     *
     * @param type
     *          The type of assistant to display.
     */
    public void refreshAssistant(@Enums.AssistantType int type) {
        int name;
        int information;
        int drawable;
        boolean tint;

        switch (type) {
            case Enums.AssistantType.LOOPBACK:
            default:
                name = R.string.assistant_type_loopback;
                information = R.string.assistant_type_loopback_information;
                drawable = R.drawable.ic_loop_96dp;
                tint = true;
                break;
        }

        mAssistantCard.displayInformation(true);
        //noinspection ConstantConditions
        mAssistantCard.refreshCard(getString(name), getString(information), drawable, tint);
    }

    /**
     * <p>To refresh the assistant state on the display.</p>
     *
     * @param state
     *          The new state of the assistant to display.
     */
    public void refreshAssistantState(@AssistantEnums.AssistantState int state) {
        mAssistantCard.displayInformation(true);
        int text = R.string.empty_string;
        @StatusView.StatusType int type = StatusView.StatusType.EMPTY;

        switch (state) {
            case AssistantEnums.AssistantState.INITIALISING:
                text = R.string.assistant_status_initialising;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.AssistantState.CANCELLING:
                text = R.string.assistant_status_cancelling;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.AssistantState.CLOSING:
            case AssistantEnums.AssistantState.UNAVAILABLE:
                text = R.string.assistant_status_unavailable;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.AssistantState.ENDING_STREAMING:
            case AssistantEnums.AssistantState.STREAMING:
                text = R.string.assistant_status_streaming;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.AssistantState.IDLE:
                text = R.string.assistant_status_ready;
                type = StatusView.StatusType.SUCCESS;
                break;
            case AssistantEnums.AssistantState.PENDING:
                text = R.string.assistant_status_waiting_for_response;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.AssistantState.SPEAKING:
                text = R.string.assistant_status_playing;
                type = StatusView.StatusType.PROGRESS;
                break;
            case AssistantEnums.AssistantState.STARTING:
                text = R.string.assistant_status_starting;
                type = StatusView.StatusType.PROGRESS;
                break;
        }
        mAssistantCard.refreshStatus(Card.Status.ASSISTANT_STATE, type, text);
    }

    /**
     * <p>To refresh the view of the assistant feature availability and/or its session state.</p>
     *
     * @param sessionState
     *          The new session state of the assistant feature.
     */
    public void refreshSessionInformation(@AssistantEnums.SessionState int sessionState) {
        int text = R.string.session_state_error_unknown;
        int colour = R.color.blue_light_tone_50;
        boolean progress = false;

        switch (sessionState) {
            case AssistantEnums.SessionState.CANCELLING:
                text = R.string.session_state_cancelling;
                colour = R.color.orange_light_tone_60;
                progress = true;
                break;

            case AssistantEnums.SessionState.READY:
                text = R.string.session_state_ready;
                colour = R.color.blue_light_tone_50;
                progress = false;
                break;

            case AssistantEnums.SessionState.RUNNING:
                text = R.string.session_state_running;
                colour = R.color.blue_light_tone_50;
                progress = true;
                break;

            case AssistantEnums.SessionState.UNAVAILABLE:
                text = R.string.session_state_unavailable;
                colour = R.color.red_light_tone_60;
                progress = false;
                break;
        }

        mSessionView.refreshValue(colour, progress, text);
    }

    /**
     * <p>To reset all the information displayed in the UI.</p>
     */
    public void reset() {
        mDeviceCard.displayInformation(false);
        mAssistantCard.displayInformation(false);
        refreshSessionInformation(AssistantEnums.SessionState.UNAVAILABLE);
    }

    /**
     * <p>To hide all the device information in the Device card.</p>
     * <p>This method does not hide the device card.</p>
     */
    public void hideDeviceInformation() {
        mDeviceCard.displayInformation(false);
    }

    @Override
    public void onClick(View view) {
        Log.d("abhay", "onClick: ");
        forceReset();
    }


    // ====== INNER CLASS ==========================================================================

    /**
     * <p>This class is used to be informed of the connection state of the service.</p>
     */
    private class ActivityServiceConnection implements ServiceConnection {

        /**
         * The constructor for this activity service connection.
         *
         */
        ActivityServiceConnection() {
            super();
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            if (componentName.getClassName().equals(VoiceAssistantService.class.getName())) {
                mService = ((VoiceAssistantService.LocalBinder) service).getService();
                mService.addHandler(mHandler);
                mService.init();
                Log.d("abhay", "onServiceConnected: ");
                onVAServiceConnected(); // to inform subclass
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (componentName.getClassName().equals(VoiceAssistantService.class.getName())) {
                mService = null;
                onVAServiceDisconnected(); // to inform subclass
            }
        }
    }

    private void bindService() {
        Log.d("abhay", "bindService: ");
        displayEventOnUI("abhay");
        // bind the service
        Intent gattServiceIntent = new Intent(mContext, VoiceAssistantService.class);
        mContext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * <p>This class is for receiving and managing messages from a
     * {@link VoiceAssistantService VoiceAssistantService}.</p>
     */
    private static class ActivityHandler extends Handler {

        /**
         * The reference to this activity.
         */
        final WeakReference<StatusFragment> mReference;

        ActivityHandler(StatusFragment statusFragment) {
            super();
            mReference = new WeakReference<>(statusFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            StatusFragment statusFragment = mReference.get();
            if (!statusFragment.mIsPaused) {
                statusFragment.handleMessageFromService(msg);
            }
        }
    }
    /**
     * The fragment which displays the background events dispatched to this activity in order to keep an history.
     */
    private EventsFragment mEventsFragment;
    /**
     * The scrollview which contains the fragment.
     */
    private ScrollView mScrollView;
    /**
     * The time of the last received event displayed within the Events fragment.
     */
    private long lastMessage = Calendar.getInstance().getTimeInMillis();





    @Override
    public void onDestroy() {
        super.onDestroy();
        super.onDestroy();

        if (mService != null) {
            mService.removeHandler(mHandler);
            mService = null;
            mContext.unbindService(mServiceConnection);
        }

    }

    // ====== SERVICE ACTIVITY METHODS ========================================================================

    protected void onVAServiceConnected() {
        mService.getStates();
        onLoopbackAssistantSelected();
        displayEventOnUI("Service is connected.");
    }

    protected void onVAServiceDisconnected() {
        displayEventOnUI("Service is disconnected.");
        reset();
    }

    protected void handleMessageFromService(Message msg) {
        Object content = msg.obj;
        switch (msg.what) {
            case Enums.ServiceMessage.DEVICE:
                @Enums.DeviceMessage int deviceMessage = msg.arg1;
                onDeviceMessageFromService(deviceMessage, content);
                break;

            case Enums.ServiceMessage.ASSISTANT:
                @Enums.AssistantMessage int assistantMessage = msg.arg1;
                onAssistantMessageFromService(assistantMessage, content);
                break;

            case Enums.ServiceMessage.ACTION:
                @Enums.ActionMessage int actionMessage = msg.arg1;
                onActionMessageFromService(actionMessage, content);
                break;

            case Enums.ServiceMessage.SESSION:
                @AssistantEnums.SessionState int sessionState = (int) content;
                onSessionStateUpdated(sessionState);
                break;

            case Enums.ServiceMessage.ERROR:
                @Enums.ErrorType int errorType = msg.arg1;
                int error = msg.arg2;
                onErrorMessageFromService(errorType, error, content);
                break;
        }
    }

    /**
     * <p>This method displays the event received within the Events fragment by calling
     * {@link #displayEventOnUI(String) displayEventOnUI()} and depending on the error type call on of the following
     * methods to provide the error information to the user.</p>
     * <p>Depending on the error type the error message can have complementary information such as an error value.</p>
     *
     * @param errorType
     *          The type of the error, one of:
     *          {@link com.qualcomm.qti.voiceassistant.Enums.ErrorType#ASSISTANT ASSISTANT},
     *          {@link com.qualcomm.qti.voiceassistant.Enums.ErrorType#CALL CALL},
     *          {@link com.qualcomm.qti.voiceassistant.Enums.ErrorType#DEVICE DEVICE} or
     *          {@link com.qualcomm.qti.voiceassistant.Enums.ErrorType#IVOR IVOR}.
     * @param argument
     *          Contains some complementary information. See {@link Enums.ErrorType ErrorType} for more
     *          information.
     * @param content
     *          Contains some complementary information. See {@link Enums.ErrorType ErrorType} for more
     *          information.
     */
    private void onErrorMessageFromService(@Enums.ErrorType int errorType, int argument, Object content) {
        switch (errorType) {
            case Enums.ErrorType.ASSISTANT:
                @Enums.AssistantError int errorAssistant = argument;
                onAssistantErrorReceived(errorAssistant);
                displayEventOnUI("Error on assistant: " + Enums.getAssistantErrorLabel(errorAssistant));
                break;
            case Enums.ErrorType.DEVICE:
                @AssistantEnums.DeviceError int errorDevice = argument;
                onDeviceErrorReceived(errorDevice, content);
                displayEventOnUI("Error on device: " + AssistantEnums.getDeviceErrorLabel(errorDevice));
                break;
            case Enums.ErrorType.IVOR:
                @AssistantEnums.IvorError int errorIvor = (int) content;
                onIvorErrorReceived(errorIvor);
                displayEventOnUI("Error on device: " + AssistantEnums.getIvorErrorLabel(errorIvor));
                break;
            case Enums.ErrorType.CALL:
                displayEventOnUI(mContext.getString(R.string.call_error_title) + getString(R.string.call_error_message));
                displayEventOnUI("Session cancelled for CALL.");
                break;
        }
    }

    /**
     * <p>This method is called when this activity catches an error message of type
     * {@link Enums.ErrorType#ASSISTANT ASSISTANT}. This error type message also contains an
     * {@link Enums.AssistantError AssistantError}.</p>
     * <p>This method defines the message to display to the user to let them know about the error. It calls
     * {@link #(int, String)} to display the message.</p>
     *
     * @param errorAssistant
     *          The assistant error which occurs, one of {@link Enums.AssistantError AssistantError}.
     */
    private void onAssistantErrorReceived(@Enums.AssistantError int errorAssistant) {
        int alertMessage = R.string.assistant_error_default_message;
        switch (errorAssistant) {
            case Enums.AssistantError.NOT_INITIALISED:
                alertMessage = R.string.assistant_error_not_initialised_message;
                break;
            case Enums.AssistantError.PLAYING_RESPONSE_FAILED:
                alertMessage = R.string.assistant_error_playing_failed_message;
                break;
            case Enums.AssistantError.NO_RESPONSE:
                alertMessage = R.string.assistant_error_no_response;
                break;
        }
        String message = getString(alertMessage) + "\n\tError: ASSISTANT\n\tCode: "
                + Enums.getAssistantErrorLabel(errorAssistant) + " (" + errorAssistant + ")";
        displayEventOnUI(mContext.getString(R.string.assistant_error_title) + message);

    }

    /**
     * <p>This method is called when this activity catches an error message of type
     * {@link Enums.ErrorType#IVOR IVOR}. This error type message also contains an
     * {@link AssistantEnums.IvorError IvorError}.</p>
     * <p>This method defines the message to display to the user to let them know about the error. It calls
     * {(int, String)} to display the message.</p>
     *
     * @param errorIvor
     *          The IVOR error which occurs, one of {@link AssistantEnums.IvorError IvorError}.
     */
    private void onIvorErrorReceived(@AssistantEnums.IvorError int errorIvor) {
        String errorLabel = "UNKNOWN";

        switch (errorIvor) {
            case AssistantEnums.IvorError.CANCELLED_BY_USER:
                Toast.makeText(mContext, R.string.ivor_error_cancelled_by_user, Toast.LENGTH_LONG).show();
                return;

            case AssistantEnums.IvorError.CALL:
            case AssistantEnums.IvorError.INCORRECT_STATE:
            case AssistantEnums.IvorError.NOT_INITIALISED:
            case AssistantEnums.IvorError.PLAYING_RESPONSE_FAILED:
            case AssistantEnums.IvorError.REQUEST_FAILED:
            case AssistantEnums.IvorError.UNAVAILABLE:
            case AssistantEnums.IvorError.UNEXPECTED_ERROR:
                errorLabel = AssistantEnums.getIvorErrorLabel(errorIvor);
        }

        int alertMessage = R.string.ivor_error_default_message;
        String message = getString(alertMessage) + "\n\tReason: " + errorLabel + "\n\tCode: "
                + errorIvor;
        displayEventOnUI(mContext.getString(R.string.ivor_error_title) + message);
    }

    /**
     * <p>This method is called when this activity catches an error message of type
     * {@link Enums.ErrorType#DEVICE DEVICE}. This error type message also contains an
     * {@link AssistantEnums.DeviceError DeviceError}.</p>
     * <p>This method defines the message to display to the user to let them know about the error. It calls
     * {@link #(int, String)} to display the message.</p>
     * <p>When the error message is {@link AssistantEnums.DeviceError#DATA_STREAM_STOPPED DATA_STREAM_STOPPED} the
     * error message also contains an integer within the Object parameter.</p>
     *
     * @param errorDevice
     *          The device error which occurs, one of {@link AssistantEnums.DeviceError DeviceError}.
     * @param content
     *          <p>When the error message is {@link AssistantEnums.DeviceError#DATA_STREAM_STOPPED DATA_STREAM_STOPPED}
     *          this parameter contains an integer which represents the number of packets which had been received.</p>
     */
    private void onDeviceErrorReceived(@AssistantEnums.DeviceError int errorDevice, Object content) {
        switch (errorDevice) {
            case AssistantEnums.DeviceError.CONNECTION_FAILED:
                Toast.makeText(mContext, R.string.device_connection_error_failed, Toast.LENGTH_LONG).show();
                break;
            case AssistantEnums.DeviceError.CONNECTION_LOST:
                Toast.makeText(mContext, R.string.device_connection_error_lost, Toast.LENGTH_LONG).show();
                break;
            case AssistantEnums.DeviceError.DATA_STREAM_STOPPED:
                int receivedPackets = (int) content;
                String message = getString(R.string.device_no_data_error_message)
                        + "\nTotal of received packets: " + receivedPackets;
                displayEventOnUI("Total of received packets: " + receivedPackets);
                return;
            default:
                Toast.makeText(mContext, R.string.device_error_default, Toast.LENGTH_LONG).show();
        }

    }

    /**
     * <p>This method is called when this activity receives a {@link Enums.ServiceMessage#SESSION SESSION} message.</p>
     * <p>This method updates the session state which is displayed on the user interface and adds a message in the
     * events history by calling {@link #displayEventOnUI(String)}.</p>
     *
     * @param sessionState
     *          The new assistant session state of the background state, one of
     *          {@link AssistantEnums.SessionState SessionState}.
     */
    private void onSessionStateUpdated(@AssistantEnums.SessionState int sessionState) {
        refreshSessionInformation(sessionState);
        displayEventOnUI("Session state updated: " + AssistantEnums.getSessionStateLabel(sessionState));
    }

    /**
     * <p>This method is called when this activity receives a {@link Enums.ServiceMessage#DEVICE DEVICE} message.</p>
     * <p>This method checks the type of device message and acts dependently. It refreshes the user interface which
     * corresponds and adds an event to the events history by calling {@link #displayEventOnUI(String)}.</p>
     *
     * @param deviceMessage
     *          The type of device message, one of {@link Enums.DeviceMessage DeviceMessage}.
     * @param content
     *          Any complementary information for the message.
     */
    private void onDeviceMessageFromService(@Enums.DeviceMessage int deviceMessage, Object content) {
        switch (deviceMessage) {
            case Enums.DeviceMessage.CONNECTION_STATE:
                @AssistantEnums.ConnectionState int connectionState = (int) content;
                refreshDeviceConnectionState(connectionState);
                break;

            case Enums.DeviceMessage.DEVICE_INFORMATION:
                break;

            case Enums.DeviceMessage.IVOR_STATE:
                @AssistantEnums.IvorState int ivorState = (int) content;
                refreshDeviceIvorState(ivorState);
                displayEventOnUI("Device IVOR state updated: state is " + AssistantEnums.getIvorStateLabel(ivorState));
                break;
        }
    }

    /**
     * <p>This method is called when this activity receives a {@link Enums.ServiceMessage#ASSISTANT ASSISTANT}
     * message.</p>
     * <p>This method checks the type of assistant message and acts dependently. It refreshes the user interface which
     * corresponds and adds an event to the events history by calling {@link #displayEventOnUI(String)}.</p>
     *
     * @param assistantMessage
     *          The type of assistant message, one of {@link Enums.AssistantMessage AssistantMessage}.
     * @param content
     *          Any complementary information for the message.
     */
    private void onAssistantMessageFromService(@Enums.AssistantMessage int assistantMessage, Object content) {
        switch (assistantMessage) {
            case Enums.AssistantMessage.STATE:
                @AssistantEnums.AssistantState int state = (int) content;
                refreshAssistantState(state);
                displayEventOnUI("Assistant state updated: " + AssistantEnums.getAssistantStateLabel(state));
                break;

            case Enums.AssistantMessage.TYPE:
                @Enums.AssistantType int type = (int) content;
                refreshAssistant(type);
                String label = Enums.getAssistantTypeLabel(type);
                displayEventOnUI("Assistant type is: " + label);
                break;
        }
    }

    /**
     * <p>This method is called when this activity receives a {@link Enums.ServiceMessage#ACTION ACTION} message.</p>
     * <p>This method checks the type of action and acts dependently. It requests the user to do an action depending
     * on the action type.</p>
     *
     * @param actionMessage
     *          The type of assistant message, one of {@link Enums.ActionMessage ActionMessage}.
     * @param content
     *          Any complementary information for the message.
     */
    private void onActionMessageFromService(@Enums.ActionMessage int actionMessage, Object content) {
        switch (actionMessage) {
            case Enums.ActionMessage.ENABLE_BLUETOOTH:
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, Consts.ACTION_REQUEST_ENABLE_BLUETOOTH);
                break;

            case Enums.ActionMessage.CONNECT_A_DEVICE:
                hideDeviceInformation();
                break;

            case Enums.ActionMessage.SELECT_A_DEVICE:

                break;

            default:
                break;
        }
    }

    public void initValues() {
        if (mService != null) {
            mService.getStates();
        }
    }

    public void forceReset() {
        displayEventOnUI("Request to force Reset");
        mService.forceReset();
        onLoopbackAssistantSelected();
    }


    /**
     * <p>This method is called when the user selects an assistant via the assistant dialog.</p>
     * <p>This method informs the application of the user choice, refreshes the corresponding part of the Ui and
     * adds an event into the events history.</p>
     */
    private void onLoopbackAssistantSelected() {
        refreshAssistant(Enums.AssistantType.LOOPBACK);
        mService.setAssistant();
        displayEventOnUI("Loopback assistant initiated");
    }

    private void displayEventOnUI(String event) {
        Toast.makeText(mContext, event, Toast.LENGTH_SHORT).show();
    }

    public interface StatusFragmentListener {
        /**
         * <p>This is triggered when the user wants to select an assistant.</p>
         */
        void showAssistantSelection();

        /**
         * <p>This is triggered when the user wants to select a device.</p>
         */
        void showDeviceSelection();

        /**
         * This is triggered when this fragment has initiated its view and is ready to display the current
         * information and states of the application.</p>
         */
        void initValues();

        /**
         * This is triggered when the user presses the "FORCE RESET" button.
         */
        void forceReset();
    }

}

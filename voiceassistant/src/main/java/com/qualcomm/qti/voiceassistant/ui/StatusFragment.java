/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.qualcomm.qti.libraries.assistant.AssistantEnums;
import com.qualcomm.qti.voiceassistant.Enums;
import com.qualcomm.qti.voiceassistant.R;

/**
 * <p>This fragment is used to display the current status of the assistant feature.</p>
 * <p>This fragment inflates the layout {@link R.layout#fragment_status fragment_status}.</p>
 * <p>This view contains a {@link SessionView} to display the general state of the feature, a {@link Card Card} to
 * give the state of the assistant, a {@link Card Card} for the device state and some buttons for the user to force a
 * reset of the feature and to send some prerecorded data.</p>
 *
 */
public class StatusFragment extends Fragment implements Card.CardListener {

    // ====== PRIVATE FIELDS ========================================================================

    /**
     * To trigger requests from this fragment.
     */
    private StatusFragmentListener mListener;
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


    // ====== STATIC METHODS ========================================================================

    /**
     * Returns a new instance of this fragment.
     */
    public static StatusFragment newInstance() {
        return new StatusFragment();
    }


    // ====== CONSTRUCTOR ========================================================================

    // default empty constructor, required for Fragment.
    public StatusFragment() {
    }


    // ====== FRAGMENT METHODS ========================================================================

    @Override // Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof StatusFragmentListener) {
            this.mListener = (StatusFragmentListener) context;
        }
    }

    @Override // Fragment
    public void onResume() {
        super.onResume();
        mListener.initValues();
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
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.forceReset();
            }
        });

        return view;
    }


    // ====== CARD LISTENER METHODS ========================================================================

    @Override // Card.CardListener
    public void onSelectAssistant() {
        mListener.showAssistantSelection();
    }

    @Override // Card.CardListener
    public void onSelectDevice() {
        mListener.showDeviceSelection();
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


    // ====== INTERFACE ========================================================================

    /**
     * The interface which listens for request from the fragment.
     */
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

/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.activities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.qualcomm.qti.voiceassistant.ui.DevicesListAdapter;
import com.qualcomm.qti.voiceassistant.ui.EventsFragment;
import com.qualcomm.qti.voiceassistant.ui.StatusFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Main activity of the application which manages the display of all information.
 */
public class MainActivity extends ServiceActivity implements StatusFragment.StatusFragmentListener,
        EventsFragment.EventsFragmentListener {

    // ====== PRIVATE FIELDS ========================================================================

    /**
     * The fragment which displays the current status of the assistant and the Bluetooth device.
     */
    private StatusFragment mStatusFragment;
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
    /**
     * The dialog used to request the selection of an assistant.
     */
    private AlertDialog mAssistantDialog;


    // ====== ACTIVITY METHODS ========================================================================

    @Override // Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // to keep screen on during update
        init();
    }

    @Override // Activity
    public void onBackPressed() {
        // do nothing
        // this is to prevent users to press the back button by inadvertence
    }


    // ====== SERVICE ACTIVITY METHODS ========================================================================

    @Override // ServiceActivity
    protected void onServiceConnected() {
        mService.getStates();
        displayEventOnUI("Service is connected.");
    }

    @Override // ServiceActivity
    protected void onServiceDisconnected() {
        displayEventOnUI("Service is disconnected.");
        if (mStatusFragment != null) {
            mStatusFragment.reset();
        }
    }

    @Override // ServiceActivity
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
                showAlert(R.string.call_error_title, getString(R.string.call_error_message));
                displayEventOnUI("Session cancelled for CALL.");
                break;
        }
    }

    /**
     * <p>This method is called when this activity catches an error message of type
     * {@link Enums.ErrorType#ASSISTANT ASSISTANT}. This error type message also contains an
     * {@link Enums.AssistantError AssistantError}.</p>
     * <p>This method defines the message to display to the user to let them know about the error. It calls
     * {@link #showAlert(int, String)} to display the message.</p>
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
        showAlert(R.string.assistant_error_title, message);
    }

    /**
     * <p>This method is called when this activity catches an error message of type
     * {@link Enums.ErrorType#IVOR IVOR}. This error type message also contains an
     * {@link AssistantEnums.IvorError IvorError}.</p>
     * <p>This method defines the message to display to the user to let them know about the error. It calls
     * {@link #showAlert(int, String)} to display the message.</p>
     *
     * @param errorIvor
     *          The IVOR error which occurs, one of {@link AssistantEnums.IvorError IvorError}.
     */
    private void onIvorErrorReceived(@AssistantEnums.IvorError int errorIvor) {
        String errorLabel = "UNKNOWN";

        switch (errorIvor) {
            case AssistantEnums.IvorError.CANCELLED_BY_USER:
                Toast.makeText(this, R.string.ivor_error_cancelled_by_user, Toast.LENGTH_LONG).show();
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
        showAlert(R.string.ivor_error_title, message);
    }

    /**
     * <p>This method is called when this activity catches an error message of type
     * {@link Enums.ErrorType#DEVICE DEVICE}. This error type message also contains an
     * {@link AssistantEnums.DeviceError DeviceError}.</p>
     * <p>This method defines the message to display to the user to let them know about the error. It calls
     * {@link #showAlert(int, String)} to display the message.</p>
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
                Toast.makeText(this, R.string.device_connection_error_failed, Toast.LENGTH_LONG).show();
                break;
            case AssistantEnums.DeviceError.CONNECTION_LOST:
                Toast.makeText(this, R.string.device_connection_error_lost, Toast.LENGTH_LONG).show();
                break;
            case AssistantEnums.DeviceError.DATA_STREAM_STOPPED:
                int receivedPackets = (int) content;
                String message = getString(R.string.device_no_data_error_message)
                        + "\nTotal of received packets: " + receivedPackets;
                showAlert(R.string.device_no_data_error_title, message);
                displayEventOnUI("Total of received packets: " + receivedPackets);
                return;
            default:
                Toast.makeText(this, R.string.device_error_default, Toast.LENGTH_LONG).show();
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
        mStatusFragment.refreshSessionInformation(sessionState);
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
                mStatusFragment.refreshDeviceConnectionState(connectionState);
                displayEventOnUI("Connection state updated: " + AssistantEnums.getConnectionStateLabel(connectionState));
                break;

            case Enums.DeviceMessage.DEVICE_INFORMATION:
                BluetoothDevice device = (BluetoothDevice) content;
                //mStatusFragment.refreshDevice(device.getName(), device.getAddress());
                //displayEventOnUI("Selected device is " + device.getName());
                break;

            case Enums.DeviceMessage.IVOR_STATE:
                @AssistantEnums.IvorState int ivorState = (int) content;
                mStatusFragment.refreshDeviceIvorState(ivorState);
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
                mStatusFragment.refreshAssistantState(state);
                displayEventOnUI("Assistant state updated: " + AssistantEnums.getAssistantStateLabel(state));
                break;

            case Enums.AssistantMessage.TYPE:
                @Enums.AssistantType int type = (int) content;
                mStatusFragment.refreshAssistant(type);
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
                mStatusFragment.hideDeviceInformation();
                break;

            case Enums.ActionMessage.SELECT_A_DEVICE:
                @SuppressWarnings("unchecked") List<BluetoothDevice> devices = (List<BluetoothDevice>) content;
                showDeviceSelectionDialog(devices);
                break;

            default:
                break;
        }
    }


    // ====== STATUS FRAGMENT LISTENER METHODS ========================================================================

    @Override // StatusFragment.StatusFragmentListener
    public void showAssistantSelection() {
        showAssistantSelectionDialog();
    }

    @Override // StatusFragment.StatusFragmentListener
    public void showDeviceSelection() {
        showDeviceSelectionDialog(getBondedDevices());
    }

    @Override // StatusFragment.StatusFragmentListener
    public void initValues() {
        if (mService != null) {
            mService.getStates();
        }
    }

    @Override // StatusFragment.StatusFragmentListener
    public void forceReset() {
        displayEventOnUI("Request to force Reset");
        mService.forceReset();
    }


    // ====== EVENTS FRAGMENT LISTENER METHODS ========================================================================

    @Override // EventsFragment.EventsFragmentListener
    public void scrollParentDown(final int height) {
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.smoothScrollTo(0, height);
            }
        });
    }


    // ====== PRIVATE METHODS ========================================================================

    /**
     * <p>To initialise the view and components link to this activity.</p>
     */
    private void init() {
        BottomNavigationView.OnNavigationItemSelectedListener navigationListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int i = item.getItemId();
                if (i == R.id.navigation_status) {
                    //replaceFragment(mStatusFragment);
                    return true;
                } else if (i == R.id.navigation_events) {
                    replaceFragment(mEventsFragment);
                    return true;
                }
                return false;
            }
        };

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(navigationListener);

        mStatusFragment = StatusFragment.newInstance();
        mEventsFragment = EventsFragment.newInstance();
        mScrollView = findViewById(R.id.scroll_view);

        //getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mStatusFragment).commit();

        TextView textVersion = findViewById(R.id.tv_app_version);
        textVersion.setText(BuildConfig.VERSION_NAME);

        initAssistantDialog();
    }

    /**
     * <p>Displays a dialog which contains a list of the given devices for the user to select one.</p>
     *
     * @param devices
     *       The list of devices for the user selection.
     */
    private void showDeviceSelectionDialog(List<BluetoothDevice> devices) {
        if (devices.isEmpty()) {
            Toast.makeText(this, "No paired devices", Toast.LENGTH_SHORT);
            return;
        }

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(this).inflate(R.layout.list_devices, null);

        // use a linear layout manager for the recycler view
        RecyclerView recyclerView = view.findViewById(R.id.rv_devices_list);
        LinearLayoutManager devicesListLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(devicesListLayoutManager);
        recyclerView.setHasFixedSize(true);

        // specify an adapter for the recycler view
        int selected = (mService != null && mService.getDevice() != null) ? devices.indexOf(mService.getDevice()) : -1;
        final DevicesListAdapter adapter = new DevicesListAdapter(devices, selected);
        recyclerView.setAdapter(adapter);

        // build the dialog content
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a BT device")
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        BluetoothDevice device = adapter.getSelectedDevice();
                        //mStatusFragment.refreshDevice(device.getName(), device.getAddress());
                        //displayEventOnUI("Selected device is: " + device.getName());
                        if (mService != null
                                && ((mService.getDevice() != null && !mService.getDevice().equals(device))
                                || mService.getConnectionState() != AssistantEnums.ConnectionState.CONNECTED)) {
                            mService.connectToDevice(adapter.getSelectedDevice());
                        }
                    }
                })
                .setNegativeButton(R.string.button_cancel, null);
        builder.show();
    }

    /**
     * <p>This methods displays the Assistant dialog - uses by the user to select an assistant - within the user
     * interface.</p>
     */
    private void showAssistantSelectionDialog() {
        if (mAssistantDialog != null) {
            mAssistantDialog.show();
        }
    }

    /**
     * <p>This methods hides the Assistant dialog - uses by the user to select an assistant - by dismissing it.</p>
     */
    private void dismissAssistantDialog() {
        if (mAssistantDialog != null) {
            mAssistantDialog.dismiss();
        }
    }

    /**
     * <p>This method initialises the content of the assistant dialog which is displayed for the user to select the
     * assistant they want to use with the application.</p>
     */
    private void initAssistantDialog() {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(this).inflate(R.layout.list_assistants, null);

        Button btLoopback = view.findViewById(R.id.button_loopback_assistant);
        btLoopback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissAssistantDialog();
                onLoopbackAssistantSelected();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select an assistant")
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(R.string.button_cancel, null);
        mAssistantDialog = builder.create();
    }

    /**
     * <p>This method is called when the user selects an assistant via the assistant dialog.</p>
     * <p>This method informs the application of the user choice, refreshes the corresponding part of the Ui and
     * adds an event into the events history.</p>
     */
    private void onLoopbackAssistantSelected() {
        mStatusFragment.refreshAssistant(Enums.AssistantType.LOOPBACK);
        mService.setAssistant();
        displayEventOnUI("Loopback assistant initiated");
    }

    /**
     * <p>This method requests the list of devices which are bonded with the Android device. It filters the list of
     * devices by only keeping the devices which have BR/EDR capability.</p>
     *
     * @return the lost of bonded devices with BR/EDR capability.
     */
    private ArrayList<BluetoothDevice> getBondedDevices() {
        Set<BluetoothDevice> listDevices;

        BluetoothAdapter adapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        if (adapter != null && adapter.isEnabled()) {
            listDevices = adapter.getBondedDevices();
        } else {
            listDevices = Collections.emptySet();
        }

        ArrayList<BluetoothDevice> listBLEDevices = new ArrayList<>();

        for (BluetoothDevice device : listDevices) {
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC
                    || device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
                listBLEDevices.add(device);
            }
        }

        return listBLEDevices;
    }

    /**
     * <p>This method adds an event to the events history. It adds to the event the elapsed time since the last
     * event.</p>
     *
     * @param event
     *          The event message to add to the events history.
     */
    private void displayEventOnUI(String event) {
        long newTime = Calendar.getInstance().getTimeInMillis();
        String time = (newTime-lastMessage) + "ms: ";
        lastMessage = newTime;

        event = time + event;
        if (mEventsFragment != null) {
            mEventsFragment.addEvent(event);
        }
    }

    /**
     * <p>To display the given fragment on the UI.</p>
     *
     * @param fragment The fragment to display.
     */
    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * <p>This method displays an alert dialog with the given title and message.</p>
     *
     * @param title
     *          The title to display on the dialog.
     * @param message
     *          The message to display on the dialog.
     */
    private void showAlert(int title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title)
                .setMessage(message);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.create().show();
    }

}

/* ************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.libraries.gaia;

import android.annotation.SuppressLint;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>This class contains all characteristics constants for the GAIA wire protocol.
 * GAIA is the Generic Application Interface Architecture defined by Qualcomm for devices to communicate over a
 * Bluetooth connection.</p>
 */
public final class GAIA {

    /**
     * <p>The mask which represents a command.</p>
     * <p>Mask used to retrieve the command from the packet.</p>
     *
     * @see #ACKNOWLEDGMENT_MASK <code>ACKNOWLEDGMENT_MASK</code> to know if a command is an acknowledgement
     */
    public static final int COMMAND_MASK = 0x7FFF;
    /**
     * <p>The mask which represents an acknowledgement.</p>
     * <ul>
     *     <li><code>COMMAND & ACKNOWLEDGMENT_MASK > 0</code> to know if the command is an acknowledgement.</li>
     *     <li><code>COMMAND | ACKNOWLEDGMENT_MASK</code> to build the acknowledgement command of a command.</li>
     * </ul>
     *
     * @see #COMMAND_MASK <code>COMMAND_MASK</code> to know how to retrieve the command number
     */
    public static final int ACKNOWLEDGMENT_MASK = 0x8000;

    /**
     * <p>The vendor default value defined by the protocol for Qualcomm vendor.</p>
     */
    public static final int VENDOR_QUALCOMM = 0x000A;


    // ------------------------------------------------------------------
    // |                      IVOR COMMANDS 0x10nn                      |
    // ------------------------------------------------------------------
    /**
     * <p>The mask to know if the command is an ivor status command.</p>
     */
    @SuppressWarnings("unused")
    public static final int COMMANDS_IVOR_MASK = 0x1000;
    /**
     * <p>Used by a device to request the start of a voice assistant session.</p>
     */
    public static final int COMMAND_IVOR_START = 0x1000;
    /**
     * <p>Used by the host to request the device to stream the voice request of a voice assistant
     * session.</p>
     */
    public static final int COMMAND_IVOR_VOICE_DATA_REQUEST = 0x1001;
    /**
     * <p>Used by the device to stream the voice.</p>
     * <p>Warning: no acknowledgement is sent for this command.</p>
     */
    public static final int COMMAND_IVOR_VOICE_DATA = 0x1002;
    /**
     * <p>Ued by the host to indicate the device to stop to stream the voice.</p>
     */
    public static final int COMMAND_IVOR_VOICE_END = 0x1003;
    /**
     * <p>Used by the host or the device to cancel a voice assistant session.</p>
     */
    public static final int COMMAND_IVOR_CANCEL = 0x1004;
    /**
     * <p>Used by the device to check if the host supports its IVOR version.</p>
     */
    public static final int COMMAND_IVOR_CHECK_VERSION = 0x1005;
    /**
     * <p>Used by the host to indicate to the device that the voice assistant response is going to be played.</p>
     */
    public static final int COMMAND_IVOR_ANSWER_START = 0x1006;
    /**
     * <p>Used by the host to indicate to the device that the voice assistant response has finished to be played.</p>
     */
    public static final int COMMAND_IVOR_ANSWER_END = 0x1007;
    /**
     * <p></p>
     */
    public static final int COMMAND_IVOR_PING = 0x10F0;


    // ------------------------------------------------------------------
    // |                  NOTIFICATION COMMANDS 0x01nn                  |
    // ------------------------------------------------------------------
    /**
     * <p>The mask to know if the command is a notification command.</p>
     */
    public static final int COMMANDS_NOTIFICATION_MASK = 0x4000;
    /**
     * <p>Hosts register for notifications using the <code>REGISTER_NOTIFICATION</code> command, specifying an Event
     * Type from table below as the first byte of payload, with optional parameters as defined per event in
     * successive payload bytes.</p>
     */
    public static final int COMMAND_REGISTER_NOTIFICATION = 0x4001;
    /**
     * <p>Requests the current status of an event type. For threshold type events where multiple levels may be
     * registered, the response indicates how many notifications are registered. Where an event may be simply
     * registered or not the number will be <code>1</code> or <code>0</code>.</p>
     */
    public static final int COMMAND_GET_NOTIFICATION = 0x4081;
    /**
     * <p>A host can cancel event notification by sending a <code>CANCEL_NOTIFICATION</code> command, the first byte of
     * payload will be the Event Type being cancelled.</p>
     */
    public static final int COMMAND_CANCEL_NOTIFICATION = 0x4002;
    /**
     * <p>Assuming successful registration, the host will asynchronously receive one or more
     * <code>EVENT_NOTIFICATION</code> command(s) (Command ID <code>0x4003</code>). The first byte of the Event
     * Notification command payload will be the Event Type code, indicating the notification type. For example,
     * <code>0x03</code> indicating a battery level low threshold event notification. Further data in the Event
     * Notification payload is dependent on the notification type and defined on a per-notification basis below.</p>
     */
    public static final int COMMAND_EVENT_NOTIFICATION = 0x4003;


    // ------------------------------------------------------------------
    // |                      COMMAND STATUSES                          |
    // ------------------------------------------------------------------
    /**
     * <p>The different status for an acknowledgment packet.</p> <p>By convention, the first octet in an acknowledgement
     * (ACK) packet is a status code indicating the success or the reason for the failure of a request.</p>
     */
    @IntDef(flag = true, value = { Status.NOT_STATUS, Status.SUCCESS, Status.NOT_SUPPORTED, Status.NOT_AUTHENTICATED,
            Status.INSUFFICIENT_RESOURCES, Status.AUTHENTICATING, Status.INVALID_PARAMETER, Status.INCORRECT_STATE,
            Status.IN_PROGRESS })
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface Status {

        int NOT_STATUS = -1;
        /**
         * <p>The request completed successfully.</p>
         */
        int SUCCESS = 0;
        /**
         * <p>An invalid COMMAND ID has been sent or is not supported by the device.</p>
         */
        int NOT_SUPPORTED = 1;
        /**
         * <p>The host is not authenticated to use a Command ID or to control a feature type.</p>
         */
        int NOT_AUTHENTICATED = 2;
        /**
         * <p>The COMMAND ID used is valid but the GAIA device could not complete it successfully.</p>
         */
        int INSUFFICIENT_RESOURCES = 3;
        /**
         * <p>The GAIA device is in the process of authenticating the host.</p>
         */
        int AUTHENTICATING = 4;
        /**
         * <p>The parameters sent were invalid: missing parameters, too much parameters, range, etc.</p>
         */
        int INVALID_PARAMETER = 5;
        /**
         * <p>The GAIA device is not in the correct state to process the command: needs to stream music, use a certain
         * source, etc.</p>
         */
        int INCORRECT_STATE = 6;
        /**
         * <p>The command is in progress.</p> <p>Acknowledgements with <code>IN_PROGRESS</code> status may be sent once
         * or periodically during the processing of a time-consuming operation to indicate that the operation has not
         * stalled.</p>
         */
        int IN_PROGRESS = 7;
    }

    /**
     * <p>To identify a {@link GAIA.Status} by its value.</p>
     *
     * @param status
     *              The status to identify.
     *
     * @return the {@link GAIA.Status} corresponding to the value or {@link com.qualcomm.qti.libraries
     * .gaia.GAIA.Status#NOT_STATUS} if neither {@link GAIA.Status} fits the value.
     */
    public static @GAIA.Status int getStatus(byte status) {
        switch (status) {

            case Status.SUCCESS:
                return GAIA.Status.SUCCESS;
            case Status.NOT_SUPPORTED:
                return GAIA.Status.NOT_SUPPORTED;
            case Status.NOT_AUTHENTICATED:
                return GAIA.Status.NOT_AUTHENTICATED;
            case Status.INSUFFICIENT_RESOURCES:
                return GAIA.Status.INSUFFICIENT_RESOURCES;
            case Status.AUTHENTICATING:
                return GAIA.Status.AUTHENTICATING;
            case Status.INVALID_PARAMETER:
                return GAIA.Status.INVALID_PARAMETER;
            case Status.INCORRECT_STATE:
                return GAIA.Status.INCORRECT_STATE;
            case Status.IN_PROGRESS:
                return GAIA.Status.IN_PROGRESS;

            default:
                return GAIA.Status.NOT_STATUS;
        }
    }

    /**
     * <p>To obtain a readable version of a {@link GAIA.Status}.</p>
     *
     * @param status
     *              the status value.
     *
     * @return A string corresponding to the {@link GAIA.Status} value.
     */
    public static String getStatusToString(int status) {
        switch (status) {

            case Status.SUCCESS:
                return "SUCCESS";
            case Status.NOT_SUPPORTED:
                return "NOT SUPPORTED";
            case Status.NOT_AUTHENTICATED:
                return "NOT AUTHENTICATED";
            case Status.INSUFFICIENT_RESOURCES:
                return "INSUFFICIENT RESOURCES";
            case Status.AUTHENTICATING:
                return "AUTHENTICATING";
            case Status.INVALID_PARAMETER:
                return "INVALID PARAMETER";
            case Status.INCORRECT_STATE:
                return "INCORRECT STATE";
            case Status.IN_PROGRESS:
                return "IN PROGRESS";
            case Status.NOT_STATUS:
                return "NOT STATUS";

            default:
                return "UNKNOWN STATUS";
        }
    }


    // ------------------------------------------------------------------
    // |                    NOTIFICATION EVENTS                         |
    // ------------------------------------------------------------------

    /**
     * <p>All notification event types which can be sent by the device.</p>
     */
    @IntDef(flag = true, value = {NotificationEvents.NOT_NOTIFICATION, NotificationEvents.RSSI_HIGH_THRESHOLD,
            NotificationEvents.RSSI_LOW_THRESHOLD, NotificationEvents.BATTERY_HIGH_THRESHOLD,
            NotificationEvents.BATTERY_LOW_THRESHOLD, NotificationEvents.DEVICE_STATE_CHANGED,
            NotificationEvents.PIO_CHANGED, NotificationEvents.DEBUG_MESSAGE,NotificationEvents.BATTERY_CHARGED,
            NotificationEvents.CHARGER_CONNECTION, NotificationEvents.CAPSENSE_UPDATE, NotificationEvents.USER_ACTION,
            NotificationEvents.SPEECH_RECOGNITION, NotificationEvents.AV_COMMAND,
            NotificationEvents.REMOTE_BATTERY_LEVEL, NotificationEvents.KEY, NotificationEvents.DFU_STATE,
            NotificationEvents.UART_RECEIVED_DATA, NotificationEvents.VMU_PACKET, NotificationEvents.HOST_NOTIFICATION})
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface NotificationEvents {
        /**
         * <p>This is not a notification - <code>0x00</code></p>
         */
        int NOT_NOTIFICATION = 0;
        /**
         * <p>This event provides a way for hosts to receive notification of changes in the RSSI of a device's Bluetooth
         * link with the host - <code>0x01</code></p>
         */
        int RSSI_LOW_THRESHOLD = 0x01;
        /**
         * <p>This command provides a way for hosts to receive notification of changes in the RSSI of a device's Bluetooth
         * link with the host - <code>0x02</code></p>
         */
        int RSSI_HIGH_THRESHOLD = 0x02;
        /**
         * <p>This command provides a way for hosts to receive notification of changes in the battery level of a device - <code>0x03</code></p>
         */
        int BATTERY_LOW_THRESHOLD = 0x03;
        /**
         * <p>This command provides a way for hosts to receive notification of changes in the battery level of a device - <code>0x04</code></p>
         */
        int BATTERY_HIGH_THRESHOLD = 0x04;
        /**
         * <p>A host can register to receive notifications of the device changes in state - <code>0x05</code></p>
         */
        int DEVICE_STATE_CHANGED = 0x05;
        /**
         * <p>A host can register to receive notification of a change in PIO state. The host provides a uint32 bitmap of
         * PIO pins about which it wishes to receive state change notifications - <code>0x06</code></p>
         */
        int PIO_CHANGED = 0x06;
        /**
         * <p>A host can register to receive debug messages from a device - <code>0x07</code></p>
         */
        int DEBUG_MESSAGE = 0x07;
        /**
         * <p>A host can register to receive a notification when the device battery has been fully charged - <code>0x08</code></p>
         */
        int BATTERY_CHARGED = 0x08;
        /**
         * <p>A host can register to receive a notification when the battery charger is connected or disconnected - <code>0x09</code></p>
         */
        int CHARGER_CONNECTION = 0x09;
        /**
         * <p>A host can register to receive a notification when the capacitive touch sensors' state changes. Removed from
         * V1.0 of the API but sounds useful - <code>0x0A</code></p>
         */
        int CAPSENSE_UPDATE = 0x0A;
        /**
         * <p>A host can register to receive a notification when an application-specific user action takes place, for
         * instance a long button press. Not the same as PIO Changed. Removed from V1.0 of the API but sounds useful - <code>0x0B</code></p>
         */
        int USER_ACTION = 0x0B;
        /**
         * <p>A host can register to receive a notification when the Speech Recognition system thinks it heard something - <code>0x0C</code></p>
         */
        int SPEECH_RECOGNITION = 0x0C;
        /**
         * <p>? - <code>0x0D</code></p>
         */
        int AV_COMMAND = 0x0D;
        /**
         * <p>? - <code>0x0E</code></p>
         */
        int REMOTE_BATTERY_LEVEL = 0x0E;
        /**
         * <p>? - <code>0x0F</code></p>
         */
        int KEY = 0x0F;
        /**
         * <p>This notification event indicates the progress of a Device Firmware Upgrade operation -
         * <code>0x10</code>.</p>
         */
        int DFU_STATE = 0x10;
        /**
         * <p>This notification event indicates that data has been received by a UART - <code>0x11</code></p>
         */
        int UART_RECEIVED_DATA = 0x11;
        /**
         * <p>This notification event encapsulates a VM Upgrade Protocol packet - <code>0x12</code></p>
         */
        int VMU_PACKET = 0x12;
        /**
         * <p>This notification event encapsulates all system notification from the Host, such has an incoming call.</p>
         */
        int HOST_NOTIFICATION =0x13;
    }

    /**
     * <p>To identify a {@link GAIA.NotificationEvents} by its value.</p>
     *
     * @param event
     *              The event to identify.
     *
     * @return the {@link GAIA.NotificationEvents} corresponding to the value or
     * {@link com.qualcomm.qti.libraries.gaia.GAIA.NotificationEvents#NOT_NOTIFICATION} if neither
     * {@link GAIA.NotificationEvents} fits the value.
     */
    public static @GAIA.NotificationEvents int getNotificationEvent(byte event) {
        switch (event) {
            case NotificationEvents.RSSI_LOW_THRESHOLD:
                return NotificationEvents.RSSI_LOW_THRESHOLD;

            case NotificationEvents.RSSI_HIGH_THRESHOLD:
                return NotificationEvents.RSSI_HIGH_THRESHOLD;

            case NotificationEvents.BATTERY_LOW_THRESHOLD:
                return NotificationEvents.BATTERY_LOW_THRESHOLD;

            case NotificationEvents.BATTERY_HIGH_THRESHOLD:
                return NotificationEvents.BATTERY_HIGH_THRESHOLD;

            case NotificationEvents.DEVICE_STATE_CHANGED:
                return NotificationEvents.DEVICE_STATE_CHANGED;

            case NotificationEvents.PIO_CHANGED:
                return NotificationEvents.PIO_CHANGED;

            case NotificationEvents.DEBUG_MESSAGE:
                return NotificationEvents.DEBUG_MESSAGE;

            case NotificationEvents.BATTERY_CHARGED:
                return NotificationEvents.BATTERY_CHARGED;

            case NotificationEvents.CHARGER_CONNECTION:
                return NotificationEvents.CHARGER_CONNECTION;

            case NotificationEvents.CAPSENSE_UPDATE:
                return NotificationEvents.CAPSENSE_UPDATE;

            case NotificationEvents.USER_ACTION:
                return NotificationEvents.USER_ACTION;

            case NotificationEvents.SPEECH_RECOGNITION:
                return NotificationEvents.SPEECH_RECOGNITION;

            case NotificationEvents.AV_COMMAND:
                return NotificationEvents.AV_COMMAND;

            case NotificationEvents.REMOTE_BATTERY_LEVEL:
                return NotificationEvents.REMOTE_BATTERY_LEVEL;

            case NotificationEvents.KEY:
                return NotificationEvents.KEY;

            case NotificationEvents.DFU_STATE:
                return NotificationEvents.DFU_STATE;

            case NotificationEvents.UART_RECEIVED_DATA:
                return NotificationEvents.UART_RECEIVED_DATA;

            case NotificationEvents.VMU_PACKET:
                return NotificationEvents.VMU_PACKET;

            case NotificationEvents.HOST_NOTIFICATION:
                return NotificationEvents.HOST_NOTIFICATION;

            default:
                return NotificationEvents.NOT_NOTIFICATION;
        }
    }


    // ------------------------------------------------------------------
    // |                          TRANSPORT                             |
    // ------------------------------------------------------------------
    /**
     * <p>All known transports which can be used to send and transfer GAIA packets as their format changes depending
     * on their transport.</p>
     */
    @IntDef(flag = true, value = { Transport.BLE, Transport.BR_EDR })
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface Transport {
        /**
         * Bluetooth Low Energy.
         */
        int BLE = 0;
        /**
         * Bluetooth Classic.
         */
        int BR_EDR = 1;
    }

}

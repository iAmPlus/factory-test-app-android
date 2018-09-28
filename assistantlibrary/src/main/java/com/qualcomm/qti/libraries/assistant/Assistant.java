/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.libraries.assistant;

/**
 * <p>This interface defines the methods an assistant shall implement in order to communicate with a
 * {@link android.bluetooth.BluetoothDevice BluetoothDevice} which implements the Qualcomm assistant protocol and
 * feature.</p>
 * <p>This assistant is used by the {@link AssistantManager AssistantManager} to provide the requests made by the
 * Bluetooth device to the application.</p>
 * <p>The {@link AssistantManager AssistantManager} implements the {@link AssistantListener} for the Assistant to
 * be able to communicate with the device via the {@link AssistantManager AssistantManager}. To get the
 * {@link AssistantListener}, call {@link AssistantManager#getAssistantListener() getAssistantListener}.</p>
 */
public interface Assistant {

    /**
     * <p>Always called when setting up a new Assistant by calling
     * {@link AssistantManager#setAssistant(Assistant) setAssistant}. This is to ensure that any instruction which
     * couldn't be done within the constructor of the Assistant object would be done in order to use the Assistant.</p>
     */
    void init();

    /**
     * <p>Call when the Bluetooth device wants to start an assistant session.</p>
     * <p>Once the Assistant is ready it must call {@link AssistantListener#startStreaming() startStreaming} to
     * inform the device that it can start to send the voice data.</p>
     *
     * @return True if the session could be started, false otherwise.
     */
    boolean startSession();

    /**
     * <p>Call by the {@link AssistantManager AssistantManager} to give the RAW audio bytes which had been provided
     * by the Bluetooth device as the incoming voice.</p>
     * <p>This is used to provide these incoming bytes to the assistant.</p>
     * <p>Once the assistant has received enough voice data to manage the user request, it must call
     * {@link AssistantListener#stopStreaming()} for the data flow to stop.</p>
     *
     * @param data
     *          Audio bytes with the following format:
     *          <ul>
     *              <li>16 bit linear PCM</li>
     *              <li>16kHz sample rate</li>
     *              <li>Single channel</li>
     *              <li>Little endian byte order</li>
     *          </ul>
     */
    void sendData(byte[] data);

    /**
     * <p>To cancel any ongoing session and reset the assistant to its
     * {@link AssistantEnums.AssistantState#IDLE IDLE} state.</p>
     */
    void cancelSession();

    /**
     * <p>When the application does not need anymore an instance of the Assistant it MUST call this method in order to
     * release any resources the assistant uses.</p>
     * <p>When {@link AssistantManager#setAssistant(Assistant) AssistantManager.setAssistant()} is called it also
     * calls this method if a previous Assistant was set up.</p>
     */
    void close();

    /**
     * <p>This is called when the Bluetooth device initiates the end of the voice streaming.</p>
     * <p>When this is called the Assistant should not expect {@link #sendData(byte[]) sendData} to be called
     * anymore unless a new session started.</p>
     * <p>After this to be called, {@link AssistantListener#onStartPlayingResponse()} is expected to be called.</p>
     */
    void endDataStream();

    /**
     * <p>This is called to force the assistant to be reset at its initial state. This can be used at anytime when
     * the {@link AssistantManager} detects an error.</p>
     */
    void forceReset();

    /**
     * <p>To get the current state of the Assistant.</p>
     *
     * @return the current state of the Assistant as one of {@link AssistantEnums.AssistantState AssistantState}.
     */
    @AssistantEnums.AssistantState int getState();

    /**
     * <p>This is used by the Assistant to communicate instructions to the device.</p>
     * <p>The {@link AssistantManager} implements this interface to convey the requests to the device.</p>
     */
    interface AssistantListener {
        /**
         * <p>To request the device to start to send some data voice.</p>
         * <p>Once this is called and the device is ready, it transmits the data voice by calling
         * {@link Assistant#sendData(byte[]) sendData}.</p>
         */
        void startStreaming();

        /**
         * <p>To request the device to stop to send the data voice.</p>
         * <p>{@link #onStartPlayingResponse() onStartPlayingResponse()} is expected to be called after this.</p>
         */
        void stopStreaming();

        /**
         * <p>To inform the device that the assistant is starting to play the assistant audio response to the user
         * request.</p>
         */
        void onStartPlayingResponse();

        /**
         * <p>To inform the device that the assistant has finished to play the assistant audio response to the user
         * request.</p>
         */
        void onFinishPlayingResponse();

        /**
         * <p>When an error occurs within the assistant during a voice session this method must be called to
         * propagate the error to the device.</p>
         *
         * @param error
         *          Any complementary error which will be transmitted by the {@link AssistantManager} to its
         *          {@link AssistantManager.AssistantManagerListener AssistantManagerListener}.
         * @param ivorError
         *          The corresponding IVOR error as one of {@link AssistantEnums.IvorError IvorError} to
         *          give to the device.
         */
        void onError(int error, @AssistantEnums.IvorError int ivorError);

        /**
         * <p>This must be called when the state of this assistant is updated to propagate the event.</p>
         *
         * @param state
         *          The new state of the assistant as one of {@link AssistantEnums.AssistantState AssistantState}.
         * @param previous
         *          The previous state of the assistant as one of {@link AssistantEnums.AssistantState AssistantState}.
         */
        void onStateUpdated(@AssistantEnums.AssistantState int state, @AssistantEnums.AssistantState int previous);
    }

}

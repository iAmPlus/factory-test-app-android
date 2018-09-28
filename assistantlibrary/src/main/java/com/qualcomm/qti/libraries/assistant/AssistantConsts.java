/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.libraries.assistant;

/**
 * <p>This final class encapsulates all constants used in the application.</p>
 */
public final class AssistantConsts {

    /**
     * To display or hide the debugging logs of the corresponding classes.
     */
    public static final class Debug {
        /**
         * <p>To display or hide the logs for the {@link AssistantManager AssistantManager} class.</p>
         */
        public static final boolean ASSISTANT_MANAGER = false;
        /**
         * <p>To display or hide the logs for the
         * {@link com.qualcomm.qti.libraries.assistant.ivor.IvorManager IvorManager} class.</p>
         */
        public static final boolean IVOR_MANAGER = true;
        /**
         * <p>To display or hide the logs for the {@link com.qualcomm.qti.libraries.gaia.GaiaManager GaiaManager}
         * class.</p>
         */
        public static final boolean GAIA_MANAGER = false;
        /**
         * <p>To display or hide the logs for the
         * {@link com.qualcomm.qti.libraries.assistant.bluetooth.GaiaBREDRProvider GaiaBREDRProvider} class.</p>
         */
        public static final boolean GAIA_BR_EDR_PROVIDER = false;
        /**
         * <p>To display or hide the logs for the
         * {@link com.qualcomm.qti.libraries.assistant.bluetooth.BREDRProvider BREDRProvider} class.</p>
         */
        public static final boolean BR_EDR_PROVIDER = false;
    }

    /**
     * The frame size of a SBC packet received within a
     * {@link com.qualcomm.qti.libraries.gaia.GAIA#COMMAND_IVOR_VOICE_DATA COMMAND_IVOR_VOICE_DATA} packet.
     */
    public static final int SBC_FRAME_SIZE = 64;
}

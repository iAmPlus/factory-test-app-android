/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant;

/**
 * <p>This class contains all useful methods for this module.</p>
 */
public class Utils {

    /**
     * Convert a byte array to a human readable String.
     *
     * @param value
     *            The byte array.
     *
     * @return String object containing values in byte array formatted as hex.
     */
    @SuppressWarnings("unused")
    public static String getStringFromBytes(byte[] value) {
        if (value == null)
            return "null";
        final StringBuilder stringBuilder = new StringBuilder(value.length*2);
        //noinspection ForLoopReplaceableByForEach // the for loop is more efficient than the foreach one
        for (int i = 0; i < value.length; i++) {
            stringBuilder.append(String.format("0x%02x ", value[i]));
        }
        return stringBuilder.toString();
    }

}
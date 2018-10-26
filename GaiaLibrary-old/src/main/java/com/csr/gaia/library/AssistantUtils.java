/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.csr.gaia.library;

/**
 * <p>This class contains all useful methods for this module.</p>
 */
public class AssistantUtils {

    /**
     * Convert a byte array to a human readable String.
     *
     * @param value
     *            The byte array.
     * @return String object containing values in byte array formatted as hex.
     */
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
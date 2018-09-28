/* ************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.libraries.gaia;

/**
 * <p>This class contains all generic methods which can be re-used.</p>
 */
@SuppressWarnings("SameParameterValue")
public final class GaiaUtils {

    /**
     * <p>The number of bytes contained in a int.</p>
     */
    private static final int BYTES_IN_INT = 4;
    /**
     * <p>The number of bits contained in a byte.</p>
     */
    private static final int BITS_IN_BYTE = 8;

    /**
     * <p>This method allows retrieval of a human readable representation of an hexadecimal value contained in a
     * <code>int</code>.</p>
     *
     * @param i
     *         The <code>int</code> value.
     *
     * @return The hexadecimal value as a <code>String</code>.
     */
    public static String getHexadecimalStringFromInt(int i) {
        return String.format("%04X", i & 0xFFFF);
    }

    /**
     * Convert a byte array to a human readable String.
     *
     * @param value
     *         The byte array.
     *
     * @return String object containing values in byte array formatted as hex.
     */
    public static String getHexadecimalStringFromBytes(byte[] value) {
        if (value == null)
            return "null";
        final StringBuilder stringBuilder = new StringBuilder(value.length * 2);
        //noinspection ForLoopReplaceableByForEach // the for loop used less ressources than the foreach one.
        for (int i = 0; i < value.length; i++) {
            stringBuilder.append(String.format("0x%02x ", value[i]));
        }
        return stringBuilder.toString();
    }

    /**
     * <p>Extract an <code>int</code> value from a <code>bytes</code> array.</p>
     *
     * @param source
     *         The array to extract from.
     * @param offset
     *         Offset within source array.
     * @param length
     *         Number of bytes to use (maximum 4).
     * @param reverse
     *         True if bytes should be interpreted in reverse (little endian) order.
     *
     * @return The extracted <code>int</code>.
     */
    public static int extractIntFromByteArray(byte[] source, int offset, int length, boolean reverse) {
        if (length < 0 | length > BYTES_IN_INT)
            throw new IndexOutOfBoundsException("Length must be between 0 and " + BYTES_IN_INT);
        int result = 0;
        int shift = (length - 1) * BITS_IN_BYTE;

        if (reverse) {
            for (int i = offset + length - 1; i >= offset; i--) {
                result |= ((source[i] & 0xFF) << shift);
                shift -= BITS_IN_BYTE;
            }
        } else {
            for (int i = offset; i < offset + length; i++) {
                result |= ((source[i] & 0xFF) << shift);
                shift -= BITS_IN_BYTE;
            }
        }
        return result;
    }

    /**
     * <p>This method allows copy of an int value into a byte array from the specified <code>offset</code> location to
     * the <code>offset + length</code> location.</p>
     *
     * @param sourceValue
     *         The <code>int</code> value to copy in the array.
     * @param target
     *         The <code>byte</code> array to copy in the <code>int</code> value.
     * @param targetOffset
     *         The targeted offset in the array to copy the first byte of the <code>int</code> value.
     * @param length
     *         The number of bytes in the array to copy the <code>int</code> value.
     * @param reverse
     *         True if bytes should be interpreted in reverse (little endian) order.
     */
    public static void copyIntIntoByteArray(int sourceValue, byte[] target, int targetOffset, int length, boolean reverse) {
        if (length < 0 | length > BYTES_IN_INT) {
            throw new IndexOutOfBoundsException("Length must be between 0 and " + BYTES_IN_INT);
        } else if (target.length < targetOffset + length) {
            throw new IndexOutOfBoundsException("The targeted location must be contained in the target array.");
        }

        if (reverse) {
            int shift = 0;
            int j = 0;
            for (int i = length - 1; i >= 0; i--) {
                int mask = 0xFF << shift;
                target[j + targetOffset] = (byte) ((sourceValue & mask) >> shift);
                shift += BITS_IN_BYTE;
                j++;
            }
        } else {
            int shift = (length - 1) * BITS_IN_BYTE;
            for (int i = 0; i < length; i++) {
                int mask = 0xFF << shift;
                target[i + targetOffset] = (byte) ((sourceValue & mask) >> shift);
                shift -= BITS_IN_BYTE;
            }
        }
    }

    /**
     * <p>To get a String label which corresponds to the given GAIA command.</p>
     * <p>The label is built as follows:
     * <ol>
     *     <Li>The value of the GAIA command as an hexadecimal given by {@link #getHexadecimalStringFromInt(int)
     *     getHexadecimalStringFromInt}.</Li>
     *     <li>The name of the GAIA command as defined in the protocol or <code>UNKNOWN</code> if the value cannot be
     *     matched with the known ones.</li>
     *     <li><i>Optional</i>: "(deprecated)" is the command had been deprecated.</li>
     * </ol></p>
     * <p>For instance, for the given value <code>384</code> the method will return <code>"0x0180
     * COMMAND_GET_CONFIGURATION_VERSION"</code>.</p>
     *
     * @param command
     *          The command to obtain a label for.
     *
     * @return the label corresponding to the given command.
     */
    @SuppressWarnings("deprecation")
    public static String getGAIACommandToString(int command) {
        String name = "UNKNOWN";
        switch (command) {
            case GAIA.COMMAND_IVOR_ANSWER_END:
                name = "COMMAND_IVOR_ANSWER_END";
                break;
            case GAIA.COMMAND_IVOR_ANSWER_START:
                name = "COMMAND_IVOR_ANSWER_START";
                break;
            case GAIA.COMMAND_IVOR_CANCEL:
                name = "COMMAND_IVOR_CANCEL";
                break;
            case GAIA.COMMAND_IVOR_CHECK_VERSION:
                name = "COMMAND_IVOR_CHECK_VERSION";
                break;
            case GAIA.COMMAND_IVOR_PING:
                name = "COMMAND_IVOR_PING";
                break;
            case GAIA.COMMAND_IVOR_START:
                name = "COMMAND_IVOR_START";
                break;
            case GAIA.COMMAND_IVOR_VOICE_DATA:
                name = "COMMAND_IVOR_VOICE_DATA";
                break;
            case GAIA.COMMAND_IVOR_VOICE_DATA_REQUEST:
                name = "COMMAND_IVOR_VOICE_DATA_REQUEST";
                break;
            case GAIA.COMMAND_IVOR_VOICE_END:
                name = "COMMAND_IVOR_VOICE_END";
                break;
            case GAIA.COMMAND_REGISTER_NOTIFICATION:
                name = "COMMAND_REGISTER_NOTIFICATION";
                break;
            case GAIA.COMMAND_GET_NOTIFICATION:
                name = "COMMAND_GET_NOTIFICATION";
                break;
            case GAIA.COMMAND_CANCEL_NOTIFICATION:
                name = "COMMAND_CANCEL_NOTIFICATION";
                break;
            case GAIA.COMMAND_EVENT_NOTIFICATION:
                name = "COMMAND_EVENT_NOTIFICATION";
                break;
        }

        return getHexadecimalStringFromInt(command) + " " + name;
    }
}

/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.assistant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 *
 * <p>This class contains the information to build a WAVE file from PCM data bytes using the method
 * {@link #buildWaveFile(List, int, File) buildWaveFile}.</p>
 * <p>This class builds a WAVE file with the following characteristics:
 * <ul>
 *     <li>PCM signed 16 bits</li>
 *     <li>Rate is 16000Hz</li>
 *     <li>Channel is mono</li>
 * </ul></p>
 *
 * <p>A WAVE file is a RIFF file composed of the RIFF chunk header and two sub chunks. The first sub chunk describes
 * the format of the audio bytes, the second sub chunk contains the PCM audio data:</p>
 * <blockquote><pre>
 * 0 bytes          12             36           36+8+n
 * +----------------+--------------+--------------+
 * |  CHUNK HEADER  |  SUBCHUNK 1  |  SUBCHUNK 2  |
 * +----------------+--------------+--------------+
 * </pre></blockquote>
 * <table style="width:100%">
 * <tr><th>Field</th><th>Description</th></tr>
 * <tr><td><code>CHUNK HEADER</code></td><td>Contains the information to identify the type of file and format. See
 * {@link Chunk Chunk} for the composition of this element.</td></tr>
 * <tr><td><code>SUBCHUNK 1</code></td><td>Describes the format of the audio data contained in <code>SUBCHUNK
 * 2</code>. See {@link FormatSubChunk FormatSubChunk} for the composition of this element.</td></tr>
 * <tr><td><code>SUBCHUNK 2</code></td><td>Contains the audio data. See {@link DataSubChunk DataSubChunk} for the
 * composition of this element.</td></tr>
 * </table><br/>
 *
 * See http://soundfile.sapp.org/doc/WaveFormat/ for more information
 */
/*package*/ final class WaveFormat {

    /**
     * <p>The number of bytes contained in a int.</p>
     */
    private static final int BYTES_IN_INT = 4;
    /**
     * <p>The number of bits contained in a byte.</p>
     */
    private static final int BITS_IN_BYTE = 8;
    /**
     * <p>This contains the default header of a WAVE file for audio data with the following characteristics:
     * <ul>
     *     <li>PCM signed 16 bits</li>
     *     <li>Rate is 16000Hz</li>
     *     <li>Channel is mono</li>
     * </ul></p>
     * <p>This default header is composed of the default chunk header, the default format sub chunk and the
     * default header of the data sub chunk for a data length of 0:</p>
     * <blockquote><pre>
     * 0 bytes          12             36             44
     * +----------------+--------------+--------------+
     * |  CHUNK HEADER  |  SUBCHUNK 1  |  SUBCHUNK 2  |
     * +----------------+--------------+--------------+
     * </pre></blockquote>
     * <p>The default header also represents the minimum information which needs to be in a WAVE file.</p>
     */
    private static final byte[] DEFAULT_HEADER = buildHeader();
    /**
     * This is the length of the default header.
     */
    private static final int DEFAULT_HEADER_LENGTH = Chunk.ChunkID.LENGTH + Chunk.ChunkSize.LENGTH + Chunk.Format.LENGTH
            + FormatSubChunk.LENGTH + DataSubChunk.LENGTH_MIN; // 44


    // ------ PACKAGE METHODS ----------------------------------------------------------------------------------

    /**
     * <p>This method builds the content of a WAVE file. It uses a default header in which it passes the size
     * information. Then it copies it into the file as well as the data to comply to the definition of a WAVE file.</p>
     *
     * @param data
     *          A list of byte array which represent the audio data to add to the file.
     * @param size
     *          The number of bytes of the audio data.
     * @param file
     *          An empty file with the "wav" extension. After the successful execution of this method the file
     *          contains the content of a WAV file for the given data.
     * @throws IOException f
     */
    /*package*/ static void buildWaveFile(List<byte[]> data, int size, File file) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            // create and add the header
            output.write(WaveFormat.getHeader(size));
            // add the data
            for (byte[] chunk : data) {
                output.write(chunk);
            }
        }
    }


    // ------ PRIVATE METHODS ----------------------------------------------------------------------------------

    /**
     * <p>This method builds the header a a WAVE file by adding the size of the file within the
     * {@link #DEFAULT_HEADER} used for the WAVE format defined in this class.</p>
     *
     * @param size
     *          The number of data bytes of the WAVE file
     *
     * @return The header of the file which contains the given data size.
     */
    private static byte[] getHeader(int size) {
        // copy the header to get the default values
        byte[] result = new byte[DEFAULT_HEADER.length];
        System.arraycopy(DEFAULT_HEADER, 0, result, 0, DEFAULT_HEADER.length);
        // update the chunk size with the given size
        copyIntIntoArray(Chunk.ChunkSize.DEFAULT_VALUE + size, result, Chunk.ChunkSize.OFFSET, Chunk.ChunkSize.LENGTH);
        // update the data sub chunk size with the given size
        copyIntIntoArray(size, result, DataSubChunk.OFFSET + DataSubChunk.Size.OFFSET, DataSubChunk.Size.LENGTH);
        // return the requested header
        return result;
    }

    /**
     * <p>This method builds a default header of a WAVE file for audio data with the following characteristics:
     * <ul>
     *     <li>PCM signed 16 bits</li>
     *     <li>Rate is 16000Hz</li>
     *     <li>Channel is mono</li>
     * </ul></p>
     * <p>This default header is composed of the default chunk header, the default format sub chunk and the
     * default header of the data sub chunk for a data length of 0:</p>
     * <blockquote><pre>
     * 0 bytes          12             36             44
     * +----------------+--------------+--------------+
     * |  CHUNK HEADER  |  SUBCHUNK 1  |  SUBCHUNK 2  |
     * +----------------+--------------+--------------+
     * </pre></blockquote>
     * <p>The default header also represents the minimum information which needs to be in a WAVE file.</p>
     *
     * @return the default header
     */
    private static byte[] buildHeader() {
        byte[] result = new byte[DEFAULT_HEADER_LENGTH];

        // chunk information
        copyStringIntoArray(Chunk.ChunkID.VALUE, result, Chunk.ChunkID.OFFSET, Chunk.ChunkID.LENGTH);
        copyIntIntoArray(0, result, Chunk.ChunkSize.OFFSET, Chunk.ChunkSize.LENGTH);
        copyStringIntoArray(Chunk.Format.VALUE, result, Chunk.Format.OFFSET, Chunk.Format.LENGTH);

        // format subchunk information
        copyStringIntoArray(FormatSubChunk.ID.VALUE, result,
                FormatSubChunk.OFFSET + FormatSubChunk.ID.OFFSET, FormatSubChunk.ID.LENGTH);
        copyIntIntoArray(FormatSubChunk.Size.VALUE, result,
                FormatSubChunk.OFFSET + FormatSubChunk.Size.OFFSET, FormatSubChunk.Size.LENGTH);
        copyIntIntoArray(FormatSubChunk.AudioFormat.VALUE, result,
                FormatSubChunk.OFFSET + FormatSubChunk.AudioFormat.OFFSET, FormatSubChunk.AudioFormat.LENGTH);
        copyIntIntoArray(FormatSubChunk.Channels.VALUE, result,
                FormatSubChunk.OFFSET + FormatSubChunk.Channels.OFFSET, FormatSubChunk.Channels.LENGTH);
        copyIntIntoArray(FormatSubChunk.SampleRate.VALUE, result,
                FormatSubChunk.OFFSET + FormatSubChunk.SampleRate.OFFSET, FormatSubChunk.SampleRate.LENGTH);
        copyIntIntoArray(FormatSubChunk.ByteRate.VALUE, result,
                FormatSubChunk.OFFSET + FormatSubChunk.ByteRate.OFFSET, FormatSubChunk.ByteRate.LENGTH);
        copyIntIntoArray(FormatSubChunk.BlockAlign.VALUE, result,
                FormatSubChunk.OFFSET + FormatSubChunk.BlockAlign.OFFSET, FormatSubChunk.BlockAlign.LENGTH);
        copyIntIntoArray(FormatSubChunk.BitsPerSample.VALUE, result,
                FormatSubChunk.OFFSET + FormatSubChunk.BitsPerSample.OFFSET, FormatSubChunk.BitsPerSample.LENGTH);

        // data subchunk information
        copyStringIntoArray(DataSubChunk.ID.VALUE, result,
                DataSubChunk.OFFSET + DataSubChunk.ID.OFFSET, DataSubChunk.ID.LENGTH);
        copyIntIntoArray(0, result,
                DataSubChunk.OFFSET + DataSubChunk.Size.OFFSET, DataSubChunk.Size.LENGTH); // default size: 0

        return result;
    }

    /**
     * <p>This method copies a int value into a byte array from the specified <code>offset</code> location to
     * the <code>offset + length</code> location.</p>
     *
     * @param value
     *         The <code>int</code> value to copy in the array.
     * @param target
     *         The <code>byte</code> array to copy in the <code>int</code> value.
     * @param targetOffset
     *         The targeted offset in the array to copy the first byte of the <code>int</code> value.
     * @param length
     *         The number of bytes in the array to copy the <code>int</code> value.
     */
    private static void copyIntIntoArray(int value, byte [] target, int targetOffset, int length) {
        if (length < 0 | length > BYTES_IN_INT) {
            throw new IndexOutOfBoundsException("Length must be between 0 and " + BYTES_IN_INT);
        }

        for (int j=0; j < length; j++) {
            target[j+targetOffset] = (byte) (value >> (BITS_IN_BYTE * j));
        }
    }

    /**
     * <p>This method allows to copy a int value into a byte array from the specified <code>offset</code> location to
     * the <code>offset + length</code> location.</p>
     *
     * @param value
     *         The <code>int</code> value to copy in the array.
     * @param target
     *         The <code>byte</code> array to copy in the <code>int</code> value.
     * @param targetOffset
     *         The targeted offset in the array to copy the first byte of the <code>int</code> value.
     * @param length
     *         The number of bytes in the array to copy the <code>String</code> value.
     */
    private static void copyStringIntoArray(String value, byte [] target, int targetOffset, int length) {
        if (length < 0 | length > value.length()) {
            throw new IndexOutOfBoundsException("Length(" + length + ") must be between 0 and the value length (" +
                    value.length() + ").");
        }

        for (int i = 0; i < length; i++) {
            target[i+targetOffset] = (byte) value.charAt(i);
        }
    }


    // ------ INNER CLASSES ----------------------------------------------------------------------------------

    /**
     * <p>This class contains the information to build a WAVE file using
     * {@link #buildWaveFile(List, int, File) buildWaveFile}.</p>
     *
     * <p>A WAVE file is a RIFF file composed of PCM audio data and some headers to identify the audio
     * characteristics:</p>
     * <blockquote><pre>
     * 0 bytes      4            8            12      ...      36            36+8+n
     * +------------+------------+------------+----------------+----------------+
     * |            |            |            | +------------+ | +------------+ |
     * |  CHUNK ID  | CHUNK SIZE |   FORMAT   | | SUBCHUNK 1 | | | SUBCHUNK 2 | |
     * |   "RIFF"   |    36+n    |   "WAVE"   | +------------+ | +------------+ |
     * +------------+------------+------------+----------------+----------------+
     * </pre></blockquote>
     * <table style="width:100%">
     * <tr><th>Field</th><th>Description</th></tr>
     * <tr><td><code>CHUNK ID</code></td><td>Contains the letters "RIFF" in ASCII form.</td></tr>
     * <tr><td><code>CHUNK SIZE</code></td><td>Contains the number of bytes after that field.<br/></td></tr>
     * <tr><td><code>FORMAT</code></td><td>Contains the format of the file: WAVE.</td></tr>
     * <tr><td><code>SUBCHUNK 1</code></td><td>Describes the format of the audio data contained in <code>SUBCHUNK
     * 2</code>. See {@link FormatSubChunk FormatSubChunk} for the composition of this element.</td></tr>
     * <tr><td><code>SUBCHUNK 2</code></td><td>Contains the audio data. See {@link DataSubChunk DataSubChunk} for the
     * composition of this element.</td></tr>
     * </table><br/>
     */
    private static final class Chunk {

        private static final class ChunkID {
            private static final String VALUE = "RIFF";
            private static final int OFFSET = 0;
            private static final int LENGTH = 4;
        }

        private static final class ChunkSize {
            private static final int DEFAULT_VALUE = Format.LENGTH + FormatSubChunk.LENGTH
                    + DataSubChunk.LENGTH_MIN; // 36
            private static final int OFFSET = ChunkID.OFFSET + ChunkID.LENGTH; // 4
            private static final int LENGTH = 4;
        }

        private static final class Format {
            private static final String VALUE = "WAVE";
            private static final int OFFSET = ChunkSize.OFFSET + ChunkSize.LENGTH; // 8
            private static final int LENGTH = 4;
        }
    }

    /**
     * <h4>SUBCHUNK 1: the format subchunk</h4>
     * <blockquote><pre>
     * 0 bytes  4        8              10         12            16          20            22                24
     * +--------+--------+--------------+----------+-------------+-----------+-------------+-----------------+
     * |   ID   |  SIZE  | AUDIO FORMAT | CHANNELS | SAMPLE RATE | BYTE RATE | BLOCK ALIGN | BITS PER SAMPLE |
     * | "fmt " |   16   |      1       |    1     |    16000    |   32000   |      2      |       16        |
     * +--------+--------+--------------+----------+-------------+-----------+-------------+-----------------+
     * </pre></blockquote>
     * <table style="width:100%">
     * <tr><th>Field</th><th>Description</th></tr>
     * <tr><td><code>ID</code></td><td>Contains "fmt " to describe the type of the subchunk: format.</td></tr>
     * <tr><td><code>SIZE</code></td><td>Contains the number of bytes contained in the subchunk after this
     * field.</td></tr>
     * <tr><td><code>AUDIO FORMAT</code></td><td>Contains the ID of the audio format: PCM=1. Other numbers indicate
     * some form of compression.</td></tr>
     * <tr><td><code>CHANNELS</code></td><td>an ID representing the number of channels: MONO=1, STEREO=2, etc.</td></tr>
     * <tr><td><code>SAMPLE RATE</code></td><td>The rate of the audio in Hz: 8000, 16000, 44100, etc.</td></tr>
     * <tr><td><code>BYTE RATE</code></td><td>This field is calculated as follows:<br/>
     * <code>SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8</code></td></tr>
     * <tr><td><code>BLOCK ALIGN</code></td><td>This field is calculated as follows:<br/>
     * <code>CHANNELS * BITS_PER_SAMPLE / 8</code></td></tr>
     * <tr><td><code>BITS PER SAMPLE</code></td><td>The number of bits per sample: 8bits->8, 16bits->16, etc.</td></tr>
     * </table>
     * <br/>
     *
     *
     */
    private static final class FormatSubChunk {
        private static final int OFFSET = Chunk.Format.OFFSET + Chunk.Format.LENGTH; // 12
        private static final int LENGTH = ID.LENGTH + Size.LENGTH + AudioFormat.LENGTH + Channels.LENGTH
                + SampleRate.LENGTH + ByteRate.LENGTH + BlockAlign.LENGTH + BitsPerSample.LENGTH; // 24

        private static final class ID {
            private static final String VALUE = "fmt ";
            private static final int OFFSET = 0;
            private static final int LENGTH = 4;
        }

        private static final class Size {
            private static final int VALUE = 16;
            private static final int OFFSET = ID.OFFSET + ID.LENGTH; // 4
            private static final int LENGTH = 4;
        }

        private static final class AudioFormat {
            private static final int VALUE = 1;
            private static final int OFFSET = Size.OFFSET + Size.LENGTH; // 8
            private static final int LENGTH = 2;
        }

        private static final class Channels {
            private static final int VALUE = 1;
            private static final int OFFSET = AudioFormat.OFFSET + AudioFormat.LENGTH; // 10
            private static final int LENGTH = 2;
        }

        private static final class SampleRate {
            private static final int VALUE = 16000;
            private static final int OFFSET = Channels.OFFSET + Channels.LENGTH; // 12
            private static final int LENGTH = 4;
        }

        private static final class ByteRate {
            private static final int VALUE = SampleRate.VALUE * Channels.VALUE * BitsPerSample.VALUE / 8; // 32000
            private static final int OFFSET = SampleRate.OFFSET + SampleRate.LENGTH; // 16
            private static final int LENGTH = 4;
        }

        private static final class BlockAlign {
            private static final int VALUE = Channels.VALUE * BitsPerSample.VALUE / 8; // 2
            private static final int OFFSET = ByteRate.OFFSET + ByteRate.LENGTH; // 20
            private static final int LENGTH = 2;
        }

        private static final class BitsPerSample {
            private static final int VALUE = 16;
            private static final int OFFSET = BlockAlign.OFFSET + BlockAlign.LENGTH; // 22
            private static final int LENGTH = 2;
        }
    }

    /**
     * <p>This class contains the constant </p>
     *
     * <h4>SUBCHUNK 2: the data subchunk</h4>
     * <blockquote><pre>
     * 0 bytes  4        8           8+n
     * +--------+--------+------------+
     * |   ID   |  SIZE  | DATA  ...  |
     * | "data" |   n    |            |
     * +--------+--------+------------+
     * </pre></blockquote>
     * <table style="width:100%">
     * <tr><th>Field</th><th>Description</th></tr>
     * <tr><td><code>ID</code></td><td>Contains "data" to describe the type of the subchunk: data.</td></tr>
     * <tr><td><code>SIZE</code></td><td>Contains the number of bytes contained in the subchunk after this field. It
     * corresponds to the data size.</td></tr>
     * <tr><td><code>DATA</code></td><td>The PCM audio data.</td></tr>
     * </table>
     */
    private static final class DataSubChunk {
        /**
         * The offset of the WaveFormat file where the data sub chunk can be found.
         */
        private static final int OFFSET = FormatSubChunk.OFFSET + FormatSubChunk.LENGTH; // 36
        /**
         * The minimum length of the data sub chunk: this is the header of the data sub chunk.
         */
        private static final int LENGTH_MIN = ID.LENGTH + Size.LENGTH; // 8

        private static final class ID {
            private static final String VALUE = "data";
            private static final int OFFSET = 0;
            private static final int LENGTH = 4;
        }

        private static final class Size {
            private static final int OFFSET = ID.OFFSET + ID.LENGTH; // 4
            private static final int LENGTH = 4;
        }

        @SuppressWarnings("unused")
        private static final class Data {
            private static final int OFFSET = Size.OFFSET + Size.LENGTH; // 8
        }
    }
}

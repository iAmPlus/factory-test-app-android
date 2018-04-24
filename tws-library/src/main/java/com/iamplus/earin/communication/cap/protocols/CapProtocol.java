package com.iamplus.earin.communication.cap.protocols;

import android.os.Looper;
import android.util.Log;

import com.iamplus.earin.communication.cap.CapUpgradeHostStatus;
import com.iamplus.earin.communication.cap.CapUpgradeResponse;
import com.iamplus.earin.communication.cap.CapUpgradeStatus;
import com.iamplus.earin.communication.cap.transports.*;
import com.iamplus.earin.communication.utils.ByteBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public class CapProtocol
{
    private static final String TAG = CapProtocol.class.getSimpleName();

    public static final String EVENT_MATCH_STRING = "EVENT ";
    public static final String COMMAND_CONFIRMED_STRING = "OK";
    public static final String COMMAND_FAILED_STRING = "FAIL";

    public static final byte [] INQUIRE_COMMAND_TERMINATOR = new byte[]{0x0d}; //CR
    public static final byte [] RESPONSE_COMMAND_TERMINATOR = new byte[]{0x0d}; //CR

    private static final byte ENCODED_BYTE = 0x7d;
    private static final byte ENCODED_BYTE_OFFSET = 0x20;

    public static final byte DATA_BLOCK_START_CHAR = '[';
    public static final byte DATA_BLOCK_END_CHAR = ']';
    public static final byte DATA_BLOCK_VALUE_SEPARATOR = ',';
    public static final byte COMMENT_CHAR = ',';

    private final int TIMEOUT_MS = 5000; //5 sec...

    private static int[] CRC16_TABLE = {
            0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7,
            0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
            0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6,
            0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
            0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485,
            0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
            0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4,
            0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc,
            0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823,
            0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969, 0xa90a, 0xb92b,
            0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12,
            0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
            0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41,
            0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49,
            0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70,
            0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78,
            0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f,
            0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
            0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e,
            0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256,
            0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d,
            0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
            0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c,
            0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
            0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab,
            0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3,
            0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a,
            0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0, 0x2ab3, 0x3a92,
            0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8, 0x8dc9,
            0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
            0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8,
            0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0
    };

    private CapProtocolRequestState requestState;
    private String lastSendRequestCommand;
    private ByteBuffer responseBuffer;
    private ByteBuffer responsePayload;
    private ByteBuffer eventBuffer;
    private Exception requestException;
    private Semaphore requestSemphore;
    private Semaphore requestAccessSemaphore;

    private CapProtocolUpgradeState upgradeState;
    private CapProtocolUpgradeHostCommand lastSentUpgradeCommand;
    private CapProtocolUpgradeHostCommand expectedUpgradeResponse;
    private ByteBuffer upgradeDataBuffer;
    private ByteBuffer upgradePayload;
    private Exception upgradeException;
    private Semaphore upgradeConfirmSemphore;
    private Semaphore upgradeResponseSemphore;
    private Semaphore upgradeAccessSemaphore;

    private CapProtocolEventDelegate eventDelegate;
    private CapProtocolUpgradeDelegate upgradeDelegate;
    private AbstractTransport transport;

    //datablock parsers...
    private CapProtocolDataBlockParser defaultDataBlockParser;
    private ArrayList<CommandAsciiProtocolDataBlockParserInfo> specialDataBlockParsers;

    public CapProtocol(AbstractTransport transport)
    {
        //Use default parser...
        this(transport, new CapProtocolNestedDataBlockParser());
    }

    public CapProtocol(AbstractTransport transport, CapProtocolDataBlockParser defaultDataBlockParser)
    {
        this.defaultDataBlockParser = defaultDataBlockParser;

        //TODO: Sepcial data-block parserse should be Patterns using Regular expressions instead...
        this.specialDataBlockParsers = new ArrayList<CommandAsciiProtocolDataBlockParserInfo>();

        this.eventDelegate = null;
        this.upgradeDelegate = null;

        this.transport = transport;

        this.requestState = CapProtocolRequestState.IDLE;
        this.lastSendRequestCommand = null;
        this.responseBuffer = new ByteBuffer();
        this.responsePayload = new ByteBuffer();
        this.eventBuffer = new ByteBuffer();
        this.requestException = null;
        this.requestSemphore = new Semaphore(0);
        this.requestAccessSemaphore = new Semaphore(1);

        this.upgradeState = CapProtocolUpgradeState.IDLE;
        this.lastSentUpgradeCommand = CapProtocolUpgradeHostCommand.None;
        this.expectedUpgradeResponse = CapProtocolUpgradeHostCommand.None;
        this.upgradeDataBuffer = new ByteBuffer();
        this.upgradePayload = new ByteBuffer();
        this.upgradeException = null;
        this.upgradeConfirmSemphore = null;
        this.upgradeResponseSemphore = null;
        this.upgradeAccessSemaphore = new Semaphore(1);
    }

    public void setEventDelegate(CapProtocolEventDelegate delegate)
    {
        this.eventDelegate = delegate;
    }
    public void setUpgradeDelegate(CapProtocolUpgradeDelegate delegate)
    {
        this.upgradeDelegate = delegate;
    }

    private int calcCrcOnByteBuffer(ByteBuffer buffer)
    {
        return calcCrcOnByteBuffer(buffer, buffer.size());
    }

    private int calcCrcOnByteBuffer(ByteBuffer buffer, int length)
    {
//        Log.d(TAG, "Calculating checksum for buffer with length " + length);

        int crc = 0;
        for (int i = 0; i < length; i++)
        {
            int b = (buffer.getByteAt(i) + 256) % 256;
            crc = ((crc << 8) & 0xff00) ^ CRC16_TABLE[(( crc >> 8) ^ b & 0xff)];
        }

        return crc;
    }

    public void cleanup()
    {
        Log.d(TAG, "Cleaned up -- no longer connected");

        //Kill delegates
        this.eventDelegate = null;
        this.upgradeDelegate = null;

        //Then, abort all ongoing upgrade/requests
        this.abortUpgradeRequestWithReceivedHostStatus(CapUpgradeHostStatus.ErrorUpdateFailed);

        this.requestAccessSemaphore.release();
        this.upgradeAccessSemaphore.release();
    }

    /////////////////

    public byte [] request(String command) throws Exception
    {
        //Use standard timeout...
        return request(command, null, TIMEOUT_MS);
    }

    public byte [] request(String command, long timeoutMillis) throws Exception
    {
        //Use timeout...
        return request(command, null, timeoutMillis);
    }

    public byte [] request(String command, String data) throws Exception
    {
        //Use standard timeout...
        return request(command, data.getBytes(), TIMEOUT_MS);
    }

    public byte [] request(String command, byte [] data) throws Exception
    {
        //Generate data in buffer...
        ByteBuffer buffer = new ByteBuffer();
        buffer.appendByte((byte)data.length);

        //Add bytes to buffer and pad the "forbidden ones"...
        for (byte d : data)
        {
            switch (d)
            {
                case ENCODED_BYTE:
                case '\r':
                {
                    //Opps -- replace!
                    buffer.appendByte(ENCODED_BYTE);
                    buffer.appendByte((byte)(d + ENCODED_BYTE_OFFSET));
                    break;
                }

                default:
                {
                    //Just add the packet byte "as is"
                    buffer.appendByte(d);
                }
            }
        }

        //Use standard timeout...
        return request(command, buffer.getAllBytes(), TIMEOUT_MS);
    }

    public byte [] request(String command, byte [] data, long timeoutMillis) throws Exception
    {
        //Check is we're the main-thread, cause IFF we are, then this is not a good approach since we will block everthing...
        if (Looper.myLooper() == Looper.getMainLooper())
        {
            //Shit -- we're the main-thread! Abort this ASASP!
            Log.w(TAG, "Aborting; no go on main-thread requests...");
            throw new Exception("Main thread NOT supported as request-thread");
        }

        //Get access...
        Log.i(TAG, "Await request access semaphore...");
        if (!this.requestAccessSemaphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS))
        {
            //Failed...
            Log.w(TAG, "Aborting; no access... something else was blocking us");
            throw new Exception("Protocol is already pending for the response of a previously sent request");
        }
        else
        {
            Log.d(TAG, "Request access granted -- proceed");
        }

        //Generate data in buffer...
        ByteBuffer buffer = new ByteBuffer();

        //the command
        buffer.appendString(command);

        //any data?
        if (data != null && data.length > 0)
        {
            buffer.appendString(" ");
            buffer.appendString(new String(new byte[]{DATA_BLOCK_START_CHAR}));
            buffer.appendBytes(data);
            buffer.appendString(new String(new byte[]{DATA_BLOCK_END_CHAR}));
        }

        //Terminate command...
        buffer.appendBytes(INQUIRE_COMMAND_TERMINATOR);

        //Prepare properties needed to catch the response!
        this.lastSendRequestCommand = command;
        this.responseBuffer.reset();
        this.responsePayload.reset();
        this.requestException = null;
        this.requestState = CapProtocolRequestState.PENDING;

        //Kick request-transfer process
        this.transport.writeRequestData(buffer.getAllBytes());

        //Await response...
        Log.i(TAG, "Await request semaphore...");
        if (!this.requestSemphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS))
        {
            //Failed...
            this.requestState = CapProtocolRequestState.TIMED_OUT;
            this.requestException = new Exception("Timeout requesting command: " + command);
        }
        else
        {
            Log.i(TAG, "Semphore released WIHTOUT timeout. State == " + this.requestState + " -- proceed!");
        }

        //We're here -- so we're no longer "pending" -- great stuff!
        if (this.requestState == CapProtocolRequestState.CONFIRMED)
        {
            //Reset state for next requests...
            this.requestState = CapProtocolRequestState.IDLE;
            this.lastSendRequestCommand = null;

            byte [] payloadToReturn = this.responsePayload.getAllBytes();

            //Return access semaphore...
            this.requestAccessSemaphore.release();

            //Successful response -- return response payload (if any)
            return payloadToReturn;
        }

        else
        {
            //Not success... Then we failed for some reason... Let's indicate that back to our invoker...!
            if (this.requestException == null)
            {
                //Hm.. we've failed, but without a reson/exception. Strange, but let's create a reason so that we can throw the exception already!!!
                this.requestException = new Exception("Comm failed for unknown reason");
            }

            //Reset state for next requests...
            this.requestState = CapProtocolRequestState.IDLE;
            this.lastSendRequestCommand = null;

            Exception exceptionAboutToBeThrown = this.requestException;

            //Return access semaphore...
            this.requestAccessSemaphore.release();

            throw exceptionAboutToBeThrown;
        }
    }

    ////////////////////////////////////
    // Upgrade commands

    public byte [] upgrade(CapProtocolUpgradeHostCommand request, CapProtocolUpgradeHostCommand response) throws Exception
    {
        //Default -- await confirm...
        return this.upgrade(request, response, true);
    }

    public byte [] upgrade(CapProtocolUpgradeHostCommand request, CapProtocolUpgradeHostCommand response, boolean awaitConfirm) throws Exception
    {
        //Default timeout, if we should wait...
        return this.upgrade(request, response, awaitConfirm ? TIMEOUT_MS : 0L);
    }

    public byte [] upgrade(CapProtocolUpgradeHostCommand request, CapProtocolUpgradeHostCommand response, long timeoutMillis) throws Exception
    {
        //No data...
        return this.upgrade(request, response, null, timeoutMillis);
    }

    public byte [] upgrade(CapProtocolUpgradeHostCommand request, CapProtocolUpgradeHostCommand response, byte [] data) throws Exception
    {
        //Default timeout...
        return this.upgrade(request, response, data, TIMEOUT_MS);
    }

    public byte [] upgrade(CapProtocolUpgradeHostCommand request, CapProtocolUpgradeHostCommand response, byte [] data, boolean awaitConfirm) throws Exception
    {
        //Default timeout, if we should wait...
        return this.upgrade(request, response, data, awaitConfirm ? TIMEOUT_MS : 0L);
    }

    public byte [] upgrade(CapProtocolUpgradeHostCommand request, CapProtocolUpgradeHostCommand response, byte [] data, long timeoutMillis) throws Exception
    {
        Log.d(TAG, "Attempting to send upgrade cmd " + request + ", expected resp " + response + ", with payload of " + (data != null ? data.length : 0)+ " bytes");

        //Check is we're the main-thread, cause IFF we are, then this is not a good approach since we will block everthing...
        if (Looper.myLooper() == Looper.getMainLooper())
        {
            //Shit -- we're the main-thread! Abort this ASASP!
            Log.w(TAG, "Aborting; no go on main-thread requests...");
            throw new Exception("Main thread NOT supported as upgrade-thread");
        }

        //Get access...
        Log.i(TAG, "Await upgrade access semaphore...");
        if (!this.upgradeAccessSemaphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS))
        {
            //Failed...
            Log.w(TAG, "Aborting; no upgrade access... something else was blocking us");
            throw new Exception("Protocol is already pending for the response of a previously sent upgrade request (state = " + this.upgradeState + ", lastSent " + this.lastSentUpgradeCommand + ", expectedResp " + this.expectedUpgradeResponse + ")");
        }
        else
        {
            Log.d(TAG, "Upgrade access granted -- proceed");
        }

        //Create packet buffer
        ByteBuffer upgradePacket = new ByteBuffer();

        ////////////////////////////////////////////////////////
        //First of all, we need to generate the header...
        //
        //  <-      header       ->
        //  <--        Upgrade lib packet     -->
        // [  0      |  1  ..  2   |  .........  | n-2  | n-1 ]
        //   request     length     <- payload -> <-   CRC   ->
        ////////////////////////////////////////////////////////

        //Request..
        upgradePacket.appendByte(request.code());

        //Packet length (payload only)
        int packetLength = 0;
        if (data != null)
            packetLength = data.length;

        Log.d(TAG, "Packet/payload length; " + packetLength);

        //Create header... and keep in mind that the Upgrade lib expect Big endian....
        upgradePacket.appendShort(packetLength, true);

        //then, the payload -- if any...
        if (data != null)
            upgradePacket.appendBytes(data);

        //Finally, calc checksum on the entire data packet
        int crc = this.calcCrcOnByteBuffer(upgradePacket);
        upgradePacket.appendShort(crc, true);

        //  Log.d(TAG, "Calc CRC for requerst; " + crc);

        //Prepare properties needed to catch the response!
        this.lastSentUpgradeCommand = request;
        this.expectedUpgradeResponse = response;
        this.upgradeDataBuffer.reset();
        this.upgradePayload.reset();
        this.upgradeException = null;

        //Reset/setup semaphores...
        this.upgradeConfirmSemphore = new Semaphore(0);
        this.upgradeResponseSemphore = new Semaphore(0);

        Log.d(TAG, "Sending upgrade request bytes; " + upgradePacket);

        //Kick data transfer process
        this.upgradeState = CapProtocolUpgradeState.PENDING_CONFIRMATION;
        this.transport.writeUpgradeData(upgradePacket.getAllBytes());

        //Skip await on confirm / respons and just return...?
        if (timeoutMillis == 0)
        {
            //Reset state for next requests...
            Log.d(TAG, "Skipped await of conf/respons");
            this.upgradeState = CapProtocolUpgradeState.IDLE;

            //Return access semaphore...
            this.upgradeAccessSemaphore.release();

            return null;
        }

        //Await response...
        Log.i(TAG, "Await confirm semaphore...");
        if (!this.upgradeConfirmSemphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS))
        {
            //Failed...
            this.upgradeState = CapProtocolUpgradeState.TIMEOUT;
            this.upgradeException = new Exception("Timeout confirm on upgrade command: " + request);
        }
        else
        {
            Log.i(TAG, "Upgrade request WIHTOUT timeout. State == " + this.upgradeState + " -- proceed!");
        }

        //Cleanup conf-semaphore...
        this.upgradeConfirmSemphore = null;

        //Well, we might get response data and payload before we get here, but IFF we need to await the response -- do it here!
        if (this.upgradeState == CapProtocolUpgradeState.SUCCESSFUL_CONFIRMATION)
        {
            //Should we await a response?
            if (this.expectedUpgradeResponse == CapProtocolUpgradeHostCommand.None)
            {
                //Manually signal the response semaphore as we do not expect to have any other responise...
                Log.i(TAG, "No resp expected -- signal resp semaphore manually...");
                this.upgradeResponseSemphore.release();
            }

            Log.i(TAG, "Time to await expected upgrade response!");

            //Await response...
            this.upgradeState = CapProtocolUpgradeState.PENDING_RESPONSE;

            //Await response...
            Log.i(TAG, "Await response semaphore...");
            if (!this.upgradeResponseSemphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS))
            {
                //Failed...
                this.upgradeState = CapProtocolUpgradeState.TIMEOUT;
                this.upgradeException = new Exception("No response in time");
            }
            else
            {
                Log.i(TAG, "Upgrade response WIHTOUT timeout. State == " + this.upgradeState + " -- proceed!");
                this.upgradeState = CapProtocolUpgradeState.SUCCESSFUL_RESPONSE;
            }

            //Cleanup resp-semaphore...
            this.upgradeResponseSemphore = null;
        }

        //So -- great success?
        if (this.upgradeState == CapProtocolUpgradeState.SUCCESSFUL_RESPONSE)
        {
            //Reset state tracker...
            this.upgradeState = CapProtocolUpgradeState.IDLE;

            byte [] payloadToReturn = this.upgradePayload.getAllBytes();

            //Return access semaphore...
            this.upgradeAccessSemaphore.release();

            //Successful response -- return response payload (if any)
            return payloadToReturn;
        }

        else
        {
            Log.d(TAG, "Status is NOT successful-resp!: " + this.upgradeState);

            //Not success... Then we failed for some reason... Let's indicate that back to our invoker...!
            if (this.upgradeException == null)
            {
                //Hm.. we've failed, but without a reson/exception. Strange, but let's create a reason so that we can throw the exception already!!!
                this.upgradeException = new Exception("Upgrade comm failed for unknown reason");
            }

            //Reset state for next requests...
            this.upgradeState = CapProtocolUpgradeState.IDLE;

            Exception exceptionAboutToBeThrown = this.upgradeException;

            //Return access semaphore...
            this.upgradeAccessSemaphore.release();

            //Throw!!
            throw exceptionAboutToBeThrown;
        }
    }

    public boolean abortUpgradeRequestWithReceivedHostStatus(CapUpgradeHostStatus hostStatus)
    {
        Log.d(TAG, "Aborting upgrade request due to received host status error code: "+ hostStatus);

        switch (this.upgradeState)
        {
            case PENDING_CONFIRMATION:
            case PENDING_RESPONSE:
            {
                CapProtocolUpgradeHostStatusException exception = new CapProtocolUpgradeHostStatusException(
                        hostStatus,
                        "Manual abort pending state due to received host status: " + hostStatus);

                this.upgradeException = exception;

                //Signal semaphores depending on state...
                if (this.upgradeConfirmSemphore != null)
                    this.upgradeConfirmSemphore.release();
                if (this.upgradeResponseSemphore != null)
                    this.upgradeResponseSemphore.release();

                break;
            }

            default:
            {
                //Nothing -- we're not waiting for anything...
                break;
            }
        }

        return false;
    }

    ////////////////////////////////////
    // Idle utilty methods...

    public CapProtocolRequestState getRequestState()
    {
        return this.requestState;
    }

    public CapProtocolUpgradeState getUpgradeState()
    {
        return this.upgradeState;
    }

    ////////////////////////////////////
    // Receive request response/event data process methods...

    public int receivedResponseData(byte [] data) throws Exception
    {
        //Add to buffer...
        this.responseBuffer.appendBytes(data);

        int totalNbrOfConsumedBytes = 0;
        int nbrOfConsumedBytes = 0;

        do
        {
            nbrOfConsumedBytes = this.processResponseEventDataBuffer(this.responseBuffer);
            this.responseBuffer.skip(nbrOfConsumedBytes);
            totalNbrOfConsumedBytes += nbrOfConsumedBytes;
        }
        while (nbrOfConsumedBytes > 0);

        //Try to process the buffer
        return totalNbrOfConsumedBytes;
    }

    public int receivedEventData(byte [] data) throws Exception
    {
        //Add to buffer...
        this.eventBuffer.appendBytes(data);

        int totalNbrOfConsumedBytes = 0;
        int nbrOfConsumedBytes = 0;

        do
        {
            nbrOfConsumedBytes = this.processResponseEventDataBuffer(this.eventBuffer);
            this.eventBuffer.skip(nbrOfConsumedBytes);
            totalNbrOfConsumedBytes += nbrOfConsumedBytes;
        }
        while (nbrOfConsumedBytes > 0);

        //Try to process the buffer
        return totalNbrOfConsumedBytes;
    }

    public void receivedUnknownCommandEvent(byte [] data)
    {
        Log.d(TAG, "ReceivedUnknownCommandEvent...");

        if (data != null && data.length > 0)
        {
            //Let's see what we got here...
            String receivedPayload = new String(data);
            Log.d(TAG, "Could " + receivedPayload + " match what we last sent as request?");

            //Well -- essentially; if what we're waiting for is in the received data --
            // it's a match and we can shut things down...

            if (this.lastSendRequestCommand != null && receivedPayload.startsWith(this.lastSendRequestCommand))
            {
                Log.d(TAG, "MATCH!");

                this.requestException = new Exception("Unknown command reported by device");

                //We're done...
                this.requestSemphore.release();
            }
        }
    }


    protected int processResponseEventDataBuffer(ByteBuffer buffer) throws Exception
    {
        //Counter of how many bytes that we've used up...
        int nbrOfConsumedBytes = 0;

        if (buffer != null)
        {
            Log.d(TAG, "Processing buffer: " + buffer.size() + " bytes");

            //First, extract full buffer;
            //IDENTIFYER [data bytes], Comment
            //-- Identifyer is manadatory
            //-- data and comment are optional...
            //-- Entire buffer always ends with termination sequence [CR,LF]

            String identifier = null;
            byte [] responseData = null;
            String comment = null;

            //Identifyer is a string until data-block, OR termination sequence is found...
            //-- find termination sequence... if any
            int terminationSequenceIndex = buffer.indexOf(RESPONSE_COMMAND_TERMINATOR);
            if (terminationSequenceIndex != -1)
            {
                //Found terminator!
                Log.d(TAG, "Found terminator...");

                // -- any data-block in there?
                int dataBlockStartIndex = buffer.indexOf(new byte[]{DATA_BLOCK_START_CHAR});
                if (dataBlockStartIndex != -1 && dataBlockStartIndex < terminationSequenceIndex)
                {
                    //OK -- we found the beginning of a data block BEFORE our found termination index
                    //-- Then identifier is all from 0 --> this index!
                    identifier = new String(buffer.getBytes(0, dataBlockStartIndex)).trim();
                    Log.d(TAG, "There's a datalbock in here...: " + identifier);

                    //Find datablock-parser for this identifier...
                    CapProtocolDataBlockParser parser = findDataBlockParser(identifier);

                    //Packet data for parser to work with...
                    byte presumptiveDataBlock [] = buffer.getBytes(dataBlockStartIndex + 1);
//					logger.debug("Presumptive dataBlock: " + Arrays.toString(presumptiveDataBlock));

                    int relativeDataBlockEndIndex = parser.findDataBlock(presumptiveDataBlock);
//					logger.debug(">> relativeDataBlockEndIndex: " + relativeDataBlockEndIndex);

                    //Found end?
                    if (relativeDataBlockEndIndex != -1)
                    {
                        //Calc dataBlock end...
                        int dataBlockEndIndex = dataBlockStartIndex + 1 + relativeDataBlockEndIndex;

//						logger.debug(">> buffer: " + Arrays.toString(buffer.getAllBytes()));
//						logger.debug(">> dataBlockStartIndex: " + buffer.getByteAt(dataBlockStartIndex) + ", " + dataBlockStartIndex);
//						logger.debug(">> dataBlockEndIndex: " + buffer.getByteAt(dataBlockEndIndex) + ", " + dataBlockEndIndex);

                        //Great!
                        responseData = parser.getDataBlock();
                        Log.d(TAG, "Extracted data block: " + Arrays.toString(responseData) + " (" + new String(responseData) + ")");

                        //Find new termination of command, after found end of data...
                        terminationSequenceIndex = buffer.indexOf(dataBlockEndIndex + 1, RESPONSE_COMMAND_TERMINATOR);
                        if (terminationSequenceIndex != -1)
                        {
                            //Found new terminator!
                            //Then move on and find comment -- if any...
                            int commentIndex = buffer.indexOf(dataBlockEndIndex + 1, new byte[]{COMMENT_CHAR});
                            if (commentIndex != -1 && commentIndex < terminationSequenceIndex)
                            {
                                //Got a comment!
                                int commentLength = terminationSequenceIndex - commentIndex;
                                comment = new String(buffer.getBytes(commentIndex + 1, commentLength)).trim();
                                Log.d(TAG, "Extracted data-block comment :" + comment);
                            }
                        }
                        else
                        {
                            //nope -- did NOT find any termiantion after data-block... not enough data yet...
                            Log.w(TAG, "No termination after data-block...");

                            //TODO: If we end up here multiple times, then this might actually be corrupt...?
                        }
                    }
                    else
                    {
                        //No -- no end-index found, most likely more data is needed, OR data is currupt!
                        Log.w(TAG, "No end of data-block found, data possible corrupt?");

                        //Reset termination index to -1...
                        terminationSequenceIndex = -1;
                    }
                }
                else
                {
                    //No -- no data in here, just a pure terminator...

                    //Then move on and find comment -- if any...
                    int commentIndex = buffer.indexOf(new byte[]{COMMENT_CHAR});
                    if (commentIndex != -1 && commentIndex < terminationSequenceIndex)
                    {
                        //Got a comment!
                        //Identifyer is everything from 0 -> comment
                        identifier = new String(buffer.getBytes(0, commentIndex)).trim();

                        int commentLength = terminationSequenceIndex - commentIndex - 1;
                        comment = new String(buffer.getBytes(commentIndex + 1, commentLength)).trim();
                        Log.d(TAG, "Extracted plain comment: " + comment);
                    }
                    else
                    {
                        //No, no comment...

                        //Identifyer is everything from 0 -> termiantion
                        identifier = new String(buffer.getBytes(0, terminationSequenceIndex)).trim();
                    }
                }

                //So -- we're here... did we find anything?
                if (terminationSequenceIndex != -1)
                {
                    //Successfuly extracted ASCII command with all bits and pieces...!
                    Log.d(TAG, "Successful extract of ASCII command: " + identifier + ", " + Arrays.toString(responseData) + ", " + comment);

                    //Count and consume bytes...
                    nbrOfConsumedBytes = terminationSequenceIndex + RESPONSE_COMMAND_TERMINATOR.length;
                    Log.d(TAG, "nbrOfConsumedBytes = " + nbrOfConsumedBytes);

                    //Now, take action depending on what kind of buffer that we've found
                    //-- Check if this recevied string is an event...
                    if (identifier.toLowerCase().startsWith(EVENT_MATCH_STRING.toLowerCase()))
                    {
                        //This is an event!
                        Log.d(TAG, "An EVENT: " + identifier);

                        //OK -- strip event-prefix, and figure out which kind of event this is!
                        String event = identifier.substring(EVENT_MATCH_STRING.length()).trim();

                        //Distribute!
                        if (this.eventDelegate != null)
                        {
                            Log.d(TAG, "Distributing event " + identifier);
                            this.eventDelegate.receivedCapProtocolEvent(event, responseData, comment);
                        }
                    }
                    else
                    {
                        Log.d(TAG, "Possible DATA: " + identifier);

                        //So, this is NOT an event, so there should be a pending inquirer there...
                        if (this.lastSendRequestCommand != null && identifier.startsWith(this.lastSendRequestCommand))
                        {
                            //Found inquirer...
                            Log.d(TAG, "Identified matches last sent request...");

                            //So, command issues OK, or Fail?

                            //All responses from Handset *should* be in the format;
                            // -- [the original command] [OK|FAIL] followed by optional requested results or comments...

                            //Then focus on the rest...
                            String status = identifier.substring(this.lastSendRequestCommand.length()).trim();

                            //OK -- so now we should either have
                            // -- a "FAIL" followed by an explanation..., or
                            // -- an "OK", followed by optional args, and perhaps an explanation of what's going on now...

                            //So, failed?
                            if (status.toLowerCase().startsWith((COMMAND_FAILED_STRING.toLowerCase())))
                            {
                                //OK -- confirmed failure...
                                this.requestState = CapProtocolRequestState.FAILED;

                                //Any reason?
                                String reason = null;
                                if (comment != null && comment.length() > 0)
                                    reason = comment;
                                else
                                    reason = "Failed";

                                this.requestException = new Exception(reason);
                            }

                            //So, success?
                            else if (status.toLowerCase().startsWith((COMMAND_CONFIRMED_STRING.toLowerCase())))
                            {
                                //OK -- great!
                                this.responsePayload.appendBytes(responseData);
                                this.requestState = CapProtocolRequestState.CONFIRMED;
                            }

                            //Or strangeness...
                            else
                            {
                                //OK -- So, this was not a confirm, fail, or data-to-a-get-request... then this is just rubbish...
                                throw new Exception("Unable to parse buffer: " + status);
                            }

                            Log.i(TAG, "Done -- release semaphore");

                            //We're done...
                            this.requestSemphore.release();
                        }
                        else
                        {
                            //Nope -- missing/wrong match for last sent request...
                            Log.e(TAG, "No pending inquirer found for identifier " + identifier);
                        }
                    }
                }
                else
                {
                    //No - no good data found... ;(
                    //- wait for better days...
                }
            }
            else
            {
                //No terminator found -- then just ignore and wait until more data arrive...
            }
        }

        return nbrOfConsumedBytes;
    }

    public int receivedUpgradeData(byte [] data) throws Exception
    {
        //Add to buffer...
        this.upgradeDataBuffer.appendBytes(data);

        int nbrOfConsumedBytes = this.processUpgradeDataBuffer(this.upgradeDataBuffer);
        this.upgradeDataBuffer.skip(nbrOfConsumedBytes);

        //Try to process the buffer
        return nbrOfConsumedBytes;
    }

    protected int processUpgradeDataBuffer(ByteBuffer buffer) throws Exception
    {
        Log.d(TAG, "Processing upgrade data buffer: " + buffer);

        if (buffer != null && buffer.size() > 0)
        {
            //All response data on the upgrade "channel" is defined as;
            //
            // [   0   |   1    |   n     |   2   ]
            //   type    length   payload    CRC

            //Try to extract header and find a full packet.
            if (buffer.size() < 4)
            {
                //Not a full response -- ignore... and wait for more data...
                Log.d(TAG, "Not a full packet");
                return 0;
            }

            CapUpgradeResponse responseType = CapUpgradeResponse.getEnumValue(buffer.getByteAt(0));
            byte responseLength = buffer.getByteAt(1);

            Log.d(TAG, "Response type " + responseType + ", length: " + responseLength + " bytes, including header length");

            //Based on response-length knowledge (which represent the ENTIRE packet/response, not just the payload, if any... do we have the full packet?
            //Try to extract header and find a full packet.
            if (buffer.size() < responseLength)
            {
                //Not a full response -- ignore... and wait for more data...
                Log.d(TAG, "Still not a full packet");
                return 0;
            }

            //Check that we have a valid packet by verifying the checksum
            int crc = buffer.getShortAt(responseLength - 2, true);
            int refCrc = this.calcCrcOnByteBuffer(buffer, responseLength - 2);

            //Log.d(TAG, "Reference CRC: " +  refCrc + ", received CRC: " + crc);

            if (refCrc != crc)
            {
                Log.w(TAG, "Invalid checksum - dumping/resetting buffer");
                return buffer.size();
            }

            //We're here -- and that's great! Extract payload params for easier/more readable code further down...
            ByteBuffer payload = new ByteBuffer();
            payload.appendBytes(buffer.getBytes(2, responseLength - 4));

            switch (responseType)
            {
                case Confirmation:
                {
                    this.processUpgradeConfirmation(payload);
                    break;
                }

                case Data:
                {
                    this.processUpgradeData(payload);
                    break;
                }

                default:
                {
                    Log.w(TAG, "Unexpected/unsupported upgrade response type");
                    break;
                }
            }

            //We're done -- good job!
            return 4 + payload.size();
        }

        return 0;
    }

    private boolean processUpgradeConfirmation(ByteBuffer payload)
    {
        Log.d(TAG, "Processing upgrade confirmation: " + payload);

        //We need at least one byte...
        if (payload.size() < 1)
        {
            Log.w(TAG, "Unsufficient data in confirmation");
            return false;
        }

        //Check state -- are we even expecting/waiting for a confirmation?
        if (this.upgradeState != CapProtocolUpgradeState.PENDING_CONFIRMATION)
        {
            Log.w(TAG, "Unexpected upgrade state: " +  this.upgradeState);
            return false;
        }

        CapUpgradeStatus status = CapUpgradeStatus.getEnumValue(payload.getByteAt(0));
//        Log.d(TAG, "Confirmation status: " + status);

        //Check that we are successful...
        if (status == CapUpgradeStatus.Success)
        {
            //Change protoclol state
            //           Log.d(TAG, "Success!");
            this.upgradeState = CapProtocolUpgradeState.SUCCESSFUL_CONFIRMATION;
        }
        else
        {
            //Failed!
            Log.w(TAG, "FAILED!");

            //Any reason?
            String reason = null;
            switch (status)
            {
                case UnexpectedError:          reason = "Unexpected error"; break;
                case AlreadyConnectedWarning:  reason = "Already connected"; break;
                case InProgress:               reason = "Upgrade in progress"; break;
                case Busy:                     reason = "Busy"; break;
                case InvalidPowerState:        reason = "Invalid power state"; break;
                case InvalidCRC:               reason = "Invalid CRC"; break;

                default: break;
            }

            if (reason == null)
                reason = "Unknown failure";

            this.upgradeState = CapProtocolUpgradeState.FAILED;
            this.upgradeException = new Exception(reason);
        }

        //... Release semaphore so that we can proceed in our blocked inquery method!
        this.upgradeConfirmSemphore.release();

        return true;
    }

    private boolean processUpgradeData(ByteBuffer payload)
    {
        Log.d(TAG, "Processing upgrade data: " + payload);

        //We need at least one byte...
        if (payload.size() < 3)
        {
            Log.w(TAG, "Unsufficient data in confirmation");
            return false;
        }

        //Extract upgrade command and length...
        CapProtocolUpgradeHostCommand command = CapProtocolUpgradeHostCommand.getEnumValue(payload.getByteAt(0));
        Log.d(TAG, "Extracted command: " + command);

        int commandLength = payload.getShortAt(1, true);
        Log.d(TAG, "Extracted length: " + commandLength);

        ByteBuffer commandData = new ByteBuffer();
        commandData.appendBytes(payload.getBytes(3, commandLength));
        Log.d(TAG, "Extracted command data: " + commandData);

        Log.d(TAG, "We are expecting upgrade response cmd: " + this.expectedUpgradeResponse);

        //So -- are we expecting this response command?
        boolean consumedAsExpectedResponse = false;
        if (this.expectedUpgradeResponse != CapProtocolUpgradeHostCommand.None)
        {
            //We're awaiting a response/command based on something we sent...
            if (this.expectedUpgradeResponse == command)
            {
                //      Log.d(TAG, "Expected upgrade response match received command! Release semaphore and proceed");

                //Matches our expeced response! Great stuff!
                this.upgradePayload = commandData;

                consumedAsExpectedResponse = true;

                //Upate state and release (if any) blocker...
                this.upgradeState = CapProtocolUpgradeState.SUCCESSFUL_RESPONSE;
                this.upgradeResponseSemphore.release();
            }
            else
            {
                Log.w(TAG, "Data does NOT match the expected command/response...");
            }
        }

        if (!consumedAsExpectedResponse)
        {
            Log.d(TAG, "Received data but no-one was interested to consume it as a response");

            //Feed to delegate as a notification that someone might take action on...
            if (this.upgradeDelegate != null)
                this.upgradeDelegate.receivedCapUpgradeCommand(command, commandData);
        }

        return true;
    }

    ////////////////////////////////////
    // Utility methods

    public byte [] generateDataBytes(String [] args)
    {
        //Generate data in buffer...
        ByteBuffer buffer = new ByteBuffer();

        //all args, as a comma-separated list
        for (int i = 0; i < args.length; i++)
        {
            buffer.appendString(args[i]);

            //More?
            if (i < (args.length - 1))
                buffer.appendString(new String(new byte[]{DATA_BLOCK_VALUE_SEPARATOR}));
        }

        return buffer.getAllBytes();
    }

    public String [] extractDataStrings(byte [] data)
    {
        String stringData = new String(data);
        return stringData.split(",");
    }

    /////////////////

    public void setDefaultDataBlockParser(CapProtocolDataBlockParser parser)
    {
        this.defaultDataBlockParser = parser;
    }

    public CapProtocolDataBlockParser getDefaultDataBlockParser()
    {
        return this.defaultDataBlockParser;
    }

    public void addSpecialEventDataBlockParser(String event, CapProtocolDataBlockParser parser)
    {
        this.addSpecialDataBlockParser(this.EVENT_MATCH_STRING + event, parser);
    }

    public void addSpecialDataBlockParser(String identifyer, CapProtocolDataBlockParser parser)
    {
        //Create and add parser!
        this.specialDataBlockParsers.add(new CommandAsciiProtocolDataBlockParserInfo(identifyer, parser));
    }

    public void clearAllSpecialDataBlockParsers()
    {
        //Clear all
        this.specialDataBlockParsers.clear();
    }

    public CapProtocolDataBlockParser findDataBlockParser(String identifyer) throws Exception
    {
//    	Log.d("Finding data-block parser for " + identifyer);

        for (CommandAsciiProtocolDataBlockParserInfo info : this.specialDataBlockParsers)
            if (info.getIdentifyer().equalsIgnoreCase(identifyer))
            {
//    	    	Log.d("Found and useing special data-block parser!");
                return info.getParser();
            }

        //  	Log.d("Going for default data-parser!");

        //no -- then use default...
        if (this.defaultDataBlockParser != null)
            return this.defaultDataBlockParser;
        else
            throw new Exception("No default data-block parser found!");
    }

    private class CommandAsciiProtocolDataBlockParserInfo
    {
        private String identifyer;
        private CapProtocolDataBlockParser parser;

        public CommandAsciiProtocolDataBlockParserInfo(String identifyer, CapProtocolDataBlockParser parser)
        {
            this.identifyer = identifyer;
            this.parser = parser;
        }

        public CapProtocolDataBlockParser getParser()
        {
            return this.parser;
        }

        public String getIdentifyer()
        {
            return this.identifyer;
        }

        @Override
        public boolean equals(Object object)
        {
            if (object instanceof CommandAsciiProtocolDataBlockParserInfo)
            {
                CommandAsciiProtocolDataBlockParserInfo info = (CommandAsciiProtocolDataBlockParserInfo)object;

                //same identifyer?
                return this.identifyer.equalsIgnoreCase(info.getIdentifyer());
            }

            return false;
        }
    }
}
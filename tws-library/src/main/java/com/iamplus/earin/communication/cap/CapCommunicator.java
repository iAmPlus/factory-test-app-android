package com.iamplus.earin.communication.cap;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.cap.protocols.CapProtocol;
import com.iamplus.earin.communication.cap.protocols.CapProtocolEventDelegate;
import com.iamplus.earin.communication.cap.protocols.CapProtocolUpgradeHostCommand;
import com.iamplus.earin.communication.cap.protocols.CapProtocolUpgradeState;
import com.iamplus.earin.communication.models.*;
import com.iamplus.earin.communication.utils.ByteBuffer;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public class CapCommunicator implements CapProtocolEventDelegate
{
    private static final String TAG = CapCommunicator.class.getSimpleName();

    private static final String INTENT_BASE = "se.millsys.apps.capcontrol.CapCommunicator";

    public static final String INTENT_COMM_EVENT = INTENT_BASE + ".Event";

    private static final String INTENT_EXTRAS_BASE = INTENT_BASE + ".Extras";
    public static final String INTENT_EXTRAS_EVENT_NAME = INTENT_EXTRAS_BASE + ".EventName";
    public static final String INTENT_EXTRAS_EVENT_PAYLOAD = INTENT_EXTRAS_BASE + ".EventPayload";

    private String identifier;
    private CapProtocol protocol;
    private CapUpgradeAssistant upgradeAssistant;

    private ArrayList<CapCommunicatorEvent> awaitedEventHistory;
    private CapCommunicatorEvent [] awaitingEvents;
    private CapCommunicatorEvent awaitedEvent;
    private Semaphore awaitingEventsBlockingSemaphore;

    private LocalBroadcastManager broadcastManager;

    public CapCommunicator(String identifer, CapProtocol protocol)
    {
        this.identifier = identifer;
        this.protocol = protocol;

        //Hook-up as event delegate so that we can capture protcol events and convert them into our CAP event enum...
        this.protocol.setEventDelegate(this);
        this.upgradeAssistant = null;

        this.broadcastManager = LocalBroadcastManager.getInstance(EarinApplication.getContext());

        this.awaitedEventHistory = null;
        this.awaitingEvents = null;
        this.awaitingEventsBlockingSemaphore = new Semaphore(0);
    }

    public String getIdentifier(){return this.identifier;}
    public CapProtocol getProtocol(){return this.protocol;}

    public void setUpgradeAssistant(CapUpgradeAssistant communicator){this.upgradeAssistant = communicator;}
    public CapUpgradeAssistant getUpgradeAssistant(){return this.upgradeAssistant;}

    public void cleanup()
    {
        Log.d(TAG, "Cleaned up -- no longer connected");

        //Cleanup protocol...
        if (this.protocol != null)
        {
            this.protocol.setEventDelegate(null);
            this.protocol.cleanup();
            this.protocol = null;
        }

        //Detach from assistant
        if (this.upgradeAssistant != null)
        {
            this.upgradeAssistant.setCommunicator(null);
            this.upgradeAssistant = null;
        }

        //Are we awaiting any events?
        if (this.awaitingEvents != null)
        {
            Log.d(TAG, "Releaseing await-event semaphore -- we'll never get any response...");
            this.awaitingEventsBlockingSemaphore.release();
        }
    }

    //Public basic CAP ctrl functions...

    public void getAddress() throws Exception
    {
        this.protocol.request("GET ADDR");
    }

    public int getSoundMode() throws Exception
    {
        byte [] response = this.protocol.request("GET SOUND MODE");
        if (response != null && response.length > 0)
             return Integer.parseInt(new String(response));
        return 1;
    }

    public String getNameForRemoteAddress(String address) throws Exception
    {
        byte [] response = this.protocol.request("GET DEVICE NAME", address);
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public void setCustomName(String name) throws Exception
    {
        this.protocol.request("SET NAME", name);
    }

    public String getCustomName() throws Exception
    {
        byte [] response = this.protocol.request("GET NAME");
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public void doResetCustomName() throws Exception
    {
        this.protocol.request("DO NAME RESET");
    }

    public String getVersion() throws Exception
    {
        byte [] response = this.protocol.request("GET VERSION");
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public String getSKU(String payload) throws Exception
    {
        byte [] response = this.protocol.request("GET SKU", payload);
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public void setSKU(int version) throws Exception
    {
        this.protocol.request("SET SKU", String.valueOf(version));
    }

    public void setTouchProfile(String payload) throws Exception
    {
        Log.d(TAG, "setTouchProfile: ");
        this.protocol.request("SET TOUCH PROFILE", payload);
    }

    public String getSlaveVersion() throws Exception
    {
        byte [] response = this.protocol.request("GET SLAVE VERSION");
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public String getSinkState() throws Exception
    {
        byte [] response = this.protocol.request("GET SINK STATE");
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public String getChargerState() throws Exception
    {
        byte [] response = this.protocol.request("GET CHARGER STATE");
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public int getRssi() throws Exception
    {
        byte [] response = this.protocol.request("GET RSSI");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return Integer.MAX_VALUE;
    }

    public int getLinkQuality() throws Exception
    {
        byte [] response = this.protocol.request("GET LINKQUALITY");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return Integer.MAX_VALUE;
    }

    //Not supported on M-2
    public void doPowerOn() throws Exception
    {
        this.protocol.request("DO POWER ON");
    }

    public void doPowerOff() throws Exception
    {
        this.protocol.request("DO POWER OFF");
    }

    public int getAutoPowerOffTimeout() throws Exception
    {
        byte [] response = this.protocol.request("GET AUTO POWER OFF TIME");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return Integer.MAX_VALUE;
    }

    public void setAutoPowerOffTimeout(int timeout) throws Exception
    {
        String data = "" + timeout;
        this.protocol.request("SET AUTO POWER OFF TIME", data);
    }

    public int getAutoPowerOffAccelerometerTimeout() throws Exception
    {
        byte [] response = this.protocol.request("GET AUTO POWER OFF ACC TIME");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return Integer.MAX_VALUE;
    }

    public void setAutoPowerOffAccelerometerTimeout(int timeout) throws Exception
    {
        String data = "" + timeout;
        this.protocol.request("SET AUTO POWER OFF ACC TIME", data);
    }

    public void doReconnectAll() throws Exception
    {
        this.protocol.request("DO RECONNECT ALL");
    }

    public void doMobilePairOn() throws Exception
    {
        this.protocol.request("DO MOBILE PAIR ON");
    }

    public void doUnpairAll() throws Exception
    {
        this.protocol.request("DO UNPAIR ALL");
    }

    public Bounds getVolTrimBounds() throws Exception
    {
        byte [] response = this.protocol.request("GET VOL TRIM BOUNDS");
        if (response != null && response.length > 0)
        {
            //Setup conf object with nice contents...
            String [] parts = new String(response).split(",");

            return new Bounds(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()));
        }

        return null;
    }

    public VolTrim getVolTrim() throws Exception
    {
        byte [] response = this.protocol.request("GET VOL TRIM");
        if (response != null && response.length > 0)
        {
            //Setup conf object with nice contents...
            String [] parts = new String(response).split(",");

            return new VolTrim(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()));
        }

        return null;
    }

    public void setVolTrim(VolTrim trim) throws Exception
    {
        String data = "" + trim.getMaster() + "," + trim.getSlave();
        this.protocol.request("SET VOL TRIM", data);
    }

    public String getSerial() throws Exception
    {
        byte [] response = this.protocol.request("GET SERIAL");
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public void getVolumeIncrease() throws Exception
    {
        this.protocol.request("DO VOL +");
    }

    public void getVolumeDecrease() throws Exception
    {
        this.protocol.request("DO VOL -");
    }

    public void doSkipForward() throws Exception
    {
        this.protocol.request("DO SKIP FORWARD");
    }

    public void doSkipBackwards() throws Exception
    {
        this.protocol.request("DO SKIP BACKWARDS");
    }

    public void doPlay() throws Exception
    {
        this.protocol.request("DO PLAY");
    }

    public void doPause() throws Exception
    {
        this.protocol.request("DO PAUSE");
    }

    public void doStop() throws Exception
    {
        this.protocol.request("DO STOP");
    }


    public Bounds getBassBoostBounds() throws Exception
    {
        byte [] response = this.protocol.request("GET AUDIO ENHANCEMENT BOUNDS");
        if (response != null && response.length > 0)
        {
            //Setup conf object with nice contents...
            String [] parts = new String(response).split(",");

            return new Bounds(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()));
        }
        return null;
    }


    public void setBassBoost(int value) throws Exception
    {
        String data = "" + value;
        this.protocol.request("SET AUDIO ENHANCEMENT", data.getBytes());
    }

    public int getBassBoost() throws Exception
    {
        byte [] response = this.protocol.request("GET AUDIO ENHANCEMENT");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return Integer.MAX_VALUE;
    }

    //Not supported on M-2
    public BatteryReading getBatteryReading() throws Exception
    {
        byte [] response = this.protocol.request("GET VBAT");
        if (response != null && response.length > 0)
        {
            return new BatteryReading(new String(response));
        }

        return null;
    }

    public void doRequestBatteryReadings() throws Exception
    {
        this.protocol.request("DO VBAT EVENT");
    }

    public int getChargerMilliVolts() throws Exception
    {
        byte [] response = this.protocol.request("GET VCHG");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return Integer.MAX_VALUE;
    }

    public void doEnterDutMode() throws Exception
    {
        this.protocol.request("DO ENTER DUT MODE");
    }

    public void doEnterDfuMode() throws Exception
    {
        this.protocol.request("DO ENTER DFU MODE");
    }

    // EARIN specifics

    public boolean getNfmiCompiled()
    {
        try
        {
            this.protocol.request("GET NFMI COMPILED");
            return true;
        }
        catch (Exception x)
        {
            //Ignore -- it's just a "no"...
        }

        return false;
    }

    public boolean getAccCompiled()
    {
        try
        {
            this.protocol.request("GET ACC COMPILED");
            return true;
        }
        catch (Exception x)
        {
            //Ignore -- it's just a "no"...
        }

        return false;
    }

    public void setNfmiEnabled(boolean enabled) throws Exception
    {
        if (enabled)
        {
            this.protocol.request("DO NFMI ENABLE");
        }
        else
        {
            this.protocol.request("DO NFMI DISABLE");
        }
    }

    public void setNfmiForceSqifImage() throws Exception
    {
        this.protocol.request("SET NFMI FORCESQIFIMAGE");
    }

    public void setNfmiReset(int init) throws Exception
    {
        String data = "" + init;
        this.protocol.request("SET NFMI RESET", data);
    }

    public void doExitFactory() throws Exception
    {
        this.protocol.request("EXIT FACTORY");
    }

    public String getNxpVersion() throws Exception
    {
        byte [] response = this.protocol.request("GET NXP VERSION");
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public String getNfmiFirmwareVersion() throws Exception
    {
        byte [] response = this.protocol.request("GET NFMI FWVER");
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public String getHardwareId() throws Exception
    {
        byte [] response = this.protocol.request("GET HW ID");
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public int getNfmiVoltage() throws Exception
    {
        byte [] response = this.protocol.request("GET NFMI VOLT");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return Integer.MAX_VALUE;
    }

    public boolean setNfmiVoltage(int voltage) throws Exception
    {
        //Bound check...
        if (voltage > 109 && voltage < 132)
        {
            String data = "" + voltage;
            this.protocol.request("SET NFMI VOLT", data);

            return true;
        }

        return false;
    }

    public String getNfmiRadioInfo() throws Exception
    {
        byte [] response = this.protocol.request("GET NFMI RADIOINFO");
        if (response != null && response.length > 0)
            return new String(response);
        return null;
    }

    public void setPreventPowerOffEnabled(boolean enabled) throws Exception
    {
        String data = "" + (enabled ? "1" : "0");
        this.protocol.request("DO PREVENT POWER OFF", data);
    }

    public void doCsrHardwareReset() throws Exception
    {
        this.protocol.request("DO CSR HW RESET");
    }

    public boolean isNfmiConnected() throws Exception
    {
        byte [] response = this.protocol.request("GET PEER CONNECTED");
        return new String(response).equals("1");
    }

    public String getLastSessionData() throws Exception
    {
        byte [] response = this.protocol.request("GET LAST SESSION DATA");
        return new String(response);
    }

    public Bounds getPassthroughBounds() throws Exception
    {
        byte [] response = this.protocol.request("GET PASSTHROUGH BOUNDS");
        if (response != null && response.length > 0)
        {
            //Setup conf object with nice contents...
            String [] parts = new String(response).split(",");

            return new Bounds(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()));
        }

        return null;
    }

//    public int getPassthrough() throws Exception
//    {
//        byte [] response = this.protocol.request("GET PASSTHROUGH");
//        if (response != null && response.length > 0)
//        {
//            return Integer.parseInt(new String(response));
//        }
//
//        return Integer.MAX_VALUE;
//    }

//    public void setPassthrough(int value) throws Exception
//    {
//        String data = "" + value;
//        this.protocol.request("SET PASSTHROUGH", data);
//    }


    public int getPassthroughPlay() throws Exception
    {
        byte [] response = this.protocol.request("GET PASSTHROUGH LEVEL PLAY");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return 0;
    }

    public int getPassthroughStop() throws Exception
    {
        byte [] response = this.protocol.request("GET PASSTHROUGH LEVEL STOP");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return 0;
    }

    public int getPassthroughCall() throws Exception
    {
        byte [] response = this.protocol.request("GET PASSTHROUGH LEVEL CALL");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return 0;
    }

    public ArrayList<Integer> getPassthroughModes() throws Exception
    {
        byte [] response = this.protocol.request("GET PASSTHROUGH MODES");
        ArrayList<Integer> result = new ArrayList<>();
        if (response != null && response.length > 0)
        {
            Log.v(TAG, "getPassthroughModes: " + new String(response));
            for(String part : new String(response).split(",")) {
                result.add(Integer.valueOf(part));
            }
        }

        return result;
    }


    public void setPassthroughModes(int mode) throws Exception
    {
        String data = "" + mode + "," + mode + "," + mode;
        this.protocol.request("SET PASSTHROUGH MODES", data);
    }

    public void setPassthroughModes(int stop, int play, int call) throws Exception
    {
        String data = "" + stop + "," + play + "," + call;
        this.protocol.request("SET PASSTHROUGH MODES", data);
    }

    public void removePskey(int value) throws Exception
    {
        String data = "" + value;
        this.protocol.request("DO REMOVE PSKEY", data);

    }

    public void unlockBuds() throws Exception
    {
        this.protocol.request("DO UNLOCK BUDS");
    }

    public void setPassthroughPlay(int value) throws Exception
    {
        String data = "" + value;
        this.protocol.request("SET PASSTHROUGH LEVEL PLAY", data);
    }

    public void setPassthroughStop(int value) throws Exception
    {
        String data = "" + value;
        this.protocol.request("SET PASSTHROUGH LEVEL STOP", data);
    }

    public void setPassthroughCall(int value) throws Exception
    {
        String data = "" + value;
        this.protocol.request("SET PASSTHROUGH LEVEL CALL", data);
    }


    public Bounds getNoiseLevelBounds() throws Exception
    {
        byte [] response = this.protocol.request("GET NOISE LEVEL BOUNDS");
        if (response != null && response.length > 0)
        {
            //Setup conf object with nice contents...
            String [] parts = new String(response).split(",");

            return new Bounds(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()));
        }

        return null;
    }

    public int getNoiseLevel() throws Exception
    {
        byte [] response = this.protocol.request("GET NOISE LEVEL");
        if (response != null && response.length > 0)
        {
            return Integer.parseInt(new String(response));
        }

        return Integer.MAX_VALUE;
    }

    public void setNoiseLevel(int value) throws Exception
    {
        String data = "" + value;
        this.protocol.request("SET NOISE LEVEL", data);
    }

    public void setFilesystemDefaults() throws Exception
    {
        this.protocol.request("SET FILESYSTEM DEFAULTS");
    }

    public void setPsKey(int key, int [] words) throws Exception
    {
        //Built payload...
        String payload = key + ",";
        for (int i = 0; i < words.length; i++)
        {
            payload += words[i];
            if (i + 1 < words.length)
                payload += ",";
        }

        this.protocol.request("SET PSKEY", payload);
    }

    //Upgrade commands...

    public void doUpgradeConnect(int partition) throws Exception
    {
        String data = "" + partition;
        this.protocol.request("DO UPGRADE CONNECT", data);
    }

    public boolean getUpgradeConnected() throws Exception
    {
        byte [] response = this.protocol.request("GET UPGRADE CONNECTED");
        if (response != null && response.length > 0)
            return Integer.parseInt(new String(response)) != 0;

        return false;
    }

    public void doUpgradeReset() throws Exception
    {
        this.protocol.request("DO UPGRADE RESET");
    }

    public void doUpgradeDisconnect() throws Exception
    {
        this.protocol.request("DO UPGRADE DISCONNECT");
    }

    public void doUpgradeApply(int deferMillis) throws Exception
    {
        try
        {
            String data = "" + deferMillis;
            this.protocol.request("DO UPGRADE APPLY", data);
        }
        catch (Exception x)
        {
            //Ignore...
        }
    }

    public void doUpgradeCompleted() throws Exception
    {
        try
        {
            this.protocol.request("DO UPGRADE COMPLETED");
        }
        catch (Exception x)
        {
            //Ignore...
        }
    }

    public void setUpgradeIdentifier(int identifier)
    {
        try
        {
            this.protocol.request("SET UPGRADE IDENTIFIER", "" + Integer.toHexString(identifier));
        }
        catch (Exception x)
        {
            Log.w(TAG, "Opps -- failed setting upgrade identifier. ignore and proceed...", x);

            try
            {
                //Extract as 2x words instead...
                int firstWord = (identifier & 0xffff0000) >> 16;
                int secondWord = (identifier & 0x0000ffff);

                //Go!
                this.setPsKey(48, new int[]{firstWord, secondWord});
            }
            catch (Exception x2)
            {
                Log.w(TAG, "Opps -- failed attempting raw PS access too.. ", x2);
            }
        }
    }

    public int getUpgradeIdentifier()
    {
        int identifier = 0;
        Log.w(TAG, "getUpgradeIdentifier!");

        try
        {
            byte [] response = this.protocol.request("GET UPGRADE IDENTIFIER");

            //Extract string...
            if (response != null && response.length > 0)
            {
                String stringResponse = new String(response);

                Log.w(TAG, "getUpgradeIdentifier string: " + stringResponse);
                //Convert hex-string into int...identifier
                identifier = Integer.parseInt(stringResponse, 16);
            }
        }
        catch (Exception x)
        {
            //Ignore...
            Log.w(TAG, "Opps -- failed getting upgrade identifier. ignore and proceed...", x);
        }

        return identifier;
    }

    public void doUpgradeData(byte [] data) throws Exception
    {
        this.protocol.request("DO UPGRADE DATA", data);
    }

    public void resetAwaitedEventHistory()
    {
        if (this.awaitedEventHistory != null)
        {
            //Clear what's already in there...
            this.awaitedEventHistory.clear();
        }
        else
        {
            //Create new one...
            this.awaitedEventHistory = new ArrayList<CapCommunicatorEvent>();
        }
    }

    public CapCommunicatorEvent awaitAnyEvent(CapCommunicatorEvent [] possibleEvents, long timeoutMillis) throws Exception
    {
        //Save reference to event-array that we're awaiting...
        this.awaitingEvents = possibleEvents;

        //First -- check if we've already captured the event we're looking for...
        if (this.awaitedEventHistory != null && this.awaitingEvents != null)
        {
            for (CapCommunicatorEvent awaitedHistoryEvent : this.awaitedEventHistory)
            {
                for (CapCommunicatorEvent testEvent : this.awaitingEvents)
                {
                    if (testEvent == awaitedHistoryEvent)
                    {
                        this.awaitedEventHistory = null;
                        this.awaitingEvents = null;
                        return testEvent;
                    }
                }
            }
        }

        //Await semaphore and let's go... hope that we'll get invoked due to receiving an event
        Log.d(TAG, "Grab semaphore to block the current thread execution UNTIL we either timeout, OR get one of the specified CAP events...");
        if (this.awaitingEventsBlockingSemaphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS))
        {
            //Great -- we got triggered, so let's return our findings...
            this.awaitedEventHistory = null;
            this.awaitingEvents = null;

            return this.awaitedEvent;
        }
        else
        {
            //Opps -- timed out!
            Log.d(TAG, "Await of event timeout");
        }

        this.awaitedEventHistory = null;
        this.awaitingEvents = null;

        Log.w(TAG, "Aborting; we timed out waiting for a matching event...");
        throw new Exception("No such event received in given timespan");
    }

    //Upgrade commands...

    public CapUpgradeResumePoint upgradeSynchronize(int identifier) throws Exception
    {
        //Convert identifier into bytes...
        ByteBuffer request = new ByteBuffer();

        //Add as big-endian...
        request.appendInt(identifier, true);

        byte [] response = this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.SyncRequest,
                CapProtocolUpgradeHostCommand.SyncConfirm,
                request.getAllBytes());

        //Valid response?
        if (response == null || response.length < 1)
        {
           throw new Exception("Too few bytes in response");
        }

        //Parse response and extract resume point...
        return CapUpgradeResumePoint.getEnumValue(response[0]);
    }

    public CapUpgradeStart upgradeStart() throws Exception
    {
        byte [] response = this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.StartRequest,
                CapProtocolUpgradeHostCommand.StartConfirm);

        //Valid response?
        if (response == null || response.length < 1)
        {
            throw new Exception("Too few bytes in response");
        }

        //Parse response and extract resume point...
        return CapUpgradeStart.getEnumValue(response[0]);
    }

    public void upgradeStartData() throws Exception
    {
        //Start the data transfer. We don't want to match this with any response command expected, as we'll instead deal with
        //all subsequent "DATA_BYTES_REQ" commands as they are asked by the device
        this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.StartDataRequest,
                CapProtocolUpgradeHostCommand.None);
    }

    public void upgradeSendData(byte [] chunk, boolean isLastChunk) throws Exception
    {
        Log.d(TAG, "Sending data chunk of " + chunk.length + " bytes, last = " + isLastChunk);

        ByteBuffer request = new ByteBuffer();

        //First -- a byte to indicate last chunk...
        byte lastChunk = isLastChunk ? (byte)0x01 : (byte)0x00;
        request.appendByte(lastChunk);

        //Then -- the data...
        request.appendBytes(chunk);

        //Send data. We don't want to match this with any response command expected, as we'll instead deal with
        //all subsequent "DATA_BYTES_REQ" commands as they are asked by the device
        this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.Data,
                CapProtocolUpgradeHostCommand.None,
                request.getAllBytes());
    }

    public void upgradeAbort() throws Exception
    {
        this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.AbortRequest,
                CapProtocolUpgradeHostCommand.AbortConfirm);
    }

    public void upgradeCommit(CapUpgradeCommit commit) throws Exception
    {
        ByteBuffer request = new ByteBuffer();

        //Create payload with decision...
        request.appendByte(commit.code());

        this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.CommitConfirm,
                CapProtocolUpgradeHostCommand.None,
                request.getAllBytes());
    }

    public void upgradeTransferComplete(CapUpgradeTransferComplete transferComplete) throws Exception
    {
        ByteBuffer request = new ByteBuffer();

        //Create payload with decision...
        request.appendByte(transferComplete.code());

        //Send command, but only await response if the transfer payload is NOT continue, since a
        // continue argument will trigger an immediate reboot into DFU-SQIF verification which might take a few seconds...
        this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.TransferCompleteResult,
                CapProtocolUpgradeHostCommand.None,
                request.getAllBytes(),
                (transferComplete != CapUpgradeTransferComplete.Continue));
    }

    public void upgradeEraseSqif() throws Exception
    {
        ByteBuffer request = new ByteBuffer();

        byte actionByte = 0x00;
        request.appendByte(actionByte);

        this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.EraseSqifConfirm,
                CapProtocolUpgradeHostCommand.None,
                request.getAllBytes());
    }

    public void upgradeSendValidationRequest() throws Exception
    {
        this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.IsCsrValidationDoneRequest,
                CapProtocolUpgradeHostCommand.None);
    }

    public void upgradeSendInProgressResult() throws Exception
    {
        ByteBuffer request = new ByteBuffer();

        byte actionByte = 0x00;
        request.appendByte(actionByte);

        this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.InProgressResult,
                CapProtocolUpgradeHostCommand.None,
                request.getAllBytes());
    }

    public void upgradeConfirmError(CapUpgradeHostStatus errorCode) throws Exception
    {
        ByteBuffer request = new ByteBuffer();

        int errorCodeShort = 0;
        errorCodeShort += errorCode.code();

        request.appendShort(errorCodeShort, true);

        this.protocol.upgrade(
                CapProtocolUpgradeHostCommand.ErrorWarnResult,
                CapProtocolUpgradeHostCommand.None,
                request.getAllBytes());
    }

    public void awaitUpgradeProtocolIdle(long timeoutMillis, long fallbackMillis) throws Exception
    {
        long startTimeReference = System.currentTimeMillis();

        while (this.protocol.getUpgradeState() != CapProtocolUpgradeState.IDLE)
        {
            //Not idle -- check time and compare with time reference for start...
            long timeReference = System.currentTimeMillis();
            if ((timeReference - startTimeReference) < timeoutMillis)
            {
                //Still more time -- wait a bit...
                try
                {
                    Thread.sleep(fallbackMillis);
                }
                catch (InterruptedException x)
                {
                    //IGnore...
                }
            }
            else
            {
                //Opps -- timeout!
                throw new Exception("Timeout waiting for upgarde state to get idle");
            }
        }

        //Done!
    }

    //Impl interfaces...

    @Override
    public void receivedCapProtocolEvent(String identifier, byte[] data, String comment)
    {
        Log.d(TAG, "Did receive event from CapControl protocol!");

        CapCommunicatorEvent event = CapCommunicatorEvent.getEvent(identifier);

        if (event != CapCommunicatorEvent.Unknown)
        {
            Log.d(TAG, "Event identified as " + event);

            //So -- was it an indicator for unknown command?
            if (event == CapCommunicatorEvent.UnknownCommand)
                this.protocol.receivedUnknownCommandEvent(data);

            //Save to event-history...
            if (this.awaitedEventHistory != null)
                this.awaitedEventHistory.add(event);

            //First check; are we awaiting any events? If so -- see if we have a match...
            if (this.awaitingEvents != null && this.awaitingEvents.length > 0)
            {
                for (int j = 0; j < this.awaitingEvents.length; j++)
                {
                    CapCommunicatorEvent testEvent = this.awaitingEvents[j];
                    if (testEvent == event)
                    {
                        Log.d(TAG, "Match!");
                        this.awaitedEvent = event;

                        //... Release semaphore so that we can proceed in our blocked inquery method!
                        Log.d(TAG, "Releaseing await-semaphore");
                        this.awaitingEventsBlockingSemaphore.release();

                        break;
                    }
                }
            }

            //Anything that we can delegate into the CAP Upgrade process?
            if (event == CapCommunicatorEvent.UpgradeResponse)
            {
                Log.d(TAG, "Received UPGRADE RESPONSE! Delegate data into protocol ASAP!");

                try
                {
                    this.protocol.receivedUpgradeData(data);
                }
                catch (Exception x)
                {
                    Log.w(TAG, "Crashed when injecting CAP event data into upgrade response", x);
                }
            }

            if (this.upgradeAssistant != null)
                this.upgradeAssistant.receivedCommunicatorEvent(event, data);

            //TODO: how should we forward the event, and payload, to the Ux?
            // --> as a Parcelable...

            Intent intent = new Intent(INTENT_COMM_EVENT);
            intent.putExtra(INTENT_EXTRAS_EVENT_NAME, event.identifier());

            if (event == CapCommunicatorEvent.BatteryReading)
            {
                BatteryReading batteryReading = new BatteryReading(new String(data));
                intent.putExtra(INTENT_EXTRAS_EVENT_PAYLOAD, batteryReading);
            }
            else if (event == CapCommunicatorEvent.MacAddress) {
                intent.putExtra(INTENT_EXTRAS_EVENT_PAYLOAD, new String(data));
            }

            broadcastManager.sendBroadcast(intent);
        }
        else
        {
            Log.d(TAG, "Event identifier " + identifier + " was NOT identified as a known event");
        }
    }

    //Internal enums and untility classes...

    private enum AudioEnhancement
    {
        SUB_WOOFER   (0x0800),
        SPKR_EQ      (0x0400),
        EQFLAT       (0x0200),
        USER_EQ      (0x0100),
        BASS_BOOST   (0x0080),
        SPATIAL      (0x0040),
        COMPANDER    (0x0020),
        DITHER       (0x0010);

        private int value;
        AudioEnhancement(int value)
        {
            this.value = value;
        }

        public int value(){return this.value;}
        public String string(){return this.name();}
    }
}

package com.iamplus.earin.communication.cap;

import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.protocols.CapProtocolUpgradeDelegate;
import com.iamplus.earin.communication.cap.protocols.CapProtocolUpgradeHostCommand;
import com.iamplus.earin.communication.cap.protocols.CapProtocolUpgradeHostStatusException;
import com.iamplus.earin.communication.utils.ByteBuffer;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * Created by markus on 2017-05-22.
 */

public class CapUpgradeAssistant implements CapProtocolUpgradeDelegate {

    private static final String TAG = CapUpgradeAssistant.class.getSimpleName();

    private static final String INTENT_BASE = "se.millsys.apps.capcontrol.CapUpgradeAssistant";

    public static final String INTENT_UPGRADE_EVENT = INTENT_BASE + ".Event";
    public static final String PREFS_KEY_DOWNLOAD_URL = "downloadUrl";
    public static final String PREFS_KEY_FOTA_DATA = "fotaData";

    private CapUpgradeAssistantState state;
    private CapUpgradeResumePoint resumePoint;
    private ByteBuffer data;
    private int dataOffset;
    private int identifier;

    private Date dateTimeEstimate;
    private long dateTimeEstimateLastReference;
    private int dateTimeEstimateNbrOfTransferredBytesSinceLastReference;

    private CapCommunicator communicator;
    private CapUpgradeAssistantDelegate delegate;

    ///////////////////////////////
    //Constructor(s)

    public CapUpgradeAssistant() {
        this.communicator = null;
        this.delegate = Manager.getSharedManager();

        this.state = CapUpgradeAssistantState.Idle;
        this.dateTimeEstimateLastReference = 0;
    }

    ///////////////////////////////
    //Delegate ctrl setters and getters...

    public void setDelegate(CapUpgradeAssistantDelegate delegate) {
        this.delegate = delegate;
    }

    public CapUpgradeAssistantDelegate getDelegate() {
        return this.delegate;
    }

    ///////////////////////////////
    //Private utility methods...

    private void changeState(CapUpgradeAssistantState state, int progress, Date estimate) {
        this.state = state;

        //Any delegate?
        if (this.delegate != null)
            this.delegate.upgradeAssistantChangedState(this, state, progress, estimate);
    }

    private void failed(CapUpgradeAssistantError error, String reason) {
        //Change state...
        this.changeState(CapUpgradeAssistantState.Failed, 0, null);

        //Any delegate?
        if (this.delegate != null)
            this.delegate.upgradeAssistantFailed(this, error, reason);
    }

    private void startUpgradeProcess() throws Exception {
        Log.d(TAG, "Starting upgrade process!");

        //First -- connect!
        this.connectUpgrade();

        //Reset data offset...
        this.dataOffset = 0;

        //Reset time estimate trackers...
        this.dateTimeEstimate = null;
        this.dateTimeEstimateLastReference = 0;
        this.dateTimeEstimateNbrOfTransferredBytesSinceLastReference = 0;

        this.changeState(CapUpgradeAssistantState.Starting, 0, null);

        try {
            //First of all -- check where/how we should begin to upgrade...
            this.resumePoint = this.communicator.upgradeSynchronize(identifier);
            Log.d(TAG, "Extracted resume point: " + this.resumePoint);
        } catch (CapProtocolUpgradeHostStatusException x) {
            Log.w(TAG, "Ignored host status exception; " + x.getHostCommand());
            return;
        } catch (Exception x) {
            Log.w(TAG, "Error trying to syncronize");

            this.failed(CapUpgradeAssistantError.SynchronizationFailed, x.getMessage());
            return;
        }

        int nbrOfStartAttemptsRemaining = 5;
        CapUpgradeStart startResult = CapUpgradeStart.Unknown;

        do {
            Log.d(TAG, "Try to start upgrade process (nbrOfAttemptsLeft = " + nbrOfStartAttemptsRemaining + ")");

            try {
                //Await comm-access
                this.communicator.awaitUpgradeProtocolIdle(10000, 500);

                //Try to start things up!
                startResult = this.communicator.upgradeStart();
            } catch (Exception x) {
                Log.w(TAG, "Failed starting upgrade -- reason: " + x.getMessage());
                startResult = CapUpgradeStart.Unknown;
            }

            //If we failed, try again!
            if (startResult != CapUpgradeStart.Success && nbrOfStartAttemptsRemaining > 0) {
                //Fallback...
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException x) {
                    //Ignore...
                }

                //Consume attempt...
                nbrOfStartAttemptsRemaining--;
            }
        }
        while (startResult != CapUpgradeStart.Success && nbrOfStartAttemptsRemaining > 0);

        //Are we ready?
        if (startResult != CapUpgradeStart.Success) {
            Log.w(TAG, "Failed starting upgrade!");

            //Fail!
            this.failed(CapUpgradeAssistantError.StartFailed, "Failed starting upgrade");
            return;
        }

        Log.d(TAG, "We've started! -- check resume point for where to proceed...");

        //Ok -- we're here -- based on restart point, take approriate action to proceed with upgrade ASAP!
        try {
            switch (this.resumePoint) {
                case PreValidate: {
                    //Resume from the start of the validation phase, i.e. download is complete.
                    this.communicator.upgradeSendValidationRequest();
                    break;
                }

                case PreReboot: {
                    //Resume after the validation phase, but before the device has rebooted to action the upgrade.
                    this.decideOnTransferComplete();
                    break;
                }

                case PostReboot: {
                    //In progress! We've just back from the reboort -- let's proceed!
                    this.changeState(CapUpgradeAssistantState.Finishing, 0, null);

                    this.communicator.upgradeSendInProgressResult();
                    break;
                }

                case Commit: {
                    //Resume at final stage of an upgrade, ready for host to commit!
                    this.decideOnCommit();
                    break;
                }

                case Start:
                default: {
                    //Just start data request ASAP!
                    this.communicator.upgradeStartData();
                    break;
                }
            }
        } catch (CapProtocolUpgradeHostStatusException x) {
            Log.w(TAG, "Ignored host status exception; " + x);
            return;
        } catch (Exception x) {
            Log.w(TAG, "Failed resuming upgrade from resume point " + this.resumePoint + ", reason: " + x.getMessage());

            //Fail!
            this.failed(CapUpgradeAssistantError.FatalError, x.getMessage());
        }
    }

    private void connectUpgrade() throws Exception {
        Log.d(TAG, "Connecting upgrade!");

        //Connect, if not already connected...
        if (!this.communicator.getUpgradeConnected()) {
            Log.d(TAG, "NOT connected -- connect upgrade to lib!");

            this.changeState(CapUpgradeAssistantState.Connecting, 0, null);

            //Try to connect upgrade...
            try {
                this.communicator.resetAwaitedEventHistory();
                this.communicator.doUpgradeConnect(0);

                //Await CAP-event that we're connected...
                CapCommunicatorEvent event = this.communicator.awaitAnyEvent(new CapCommunicatorEvent[]{
                        CapCommunicatorEvent.UpgradeConnected,
                        CapCommunicatorEvent.UpgradeConnectionFailed
                }, 150000);

                Log.d(TAG, "Got awaited event; " + event);

                if (event != CapCommunicatorEvent.UpgradeConnected) {
                    Log.w(TAG, "Failed!");
                    throw new Exception("Did not receive expected connect success event");
                }
            } catch (Exception x) {
                Log.w(TAG, "Connection failed -- reported reason:" + x.getMessage());

                //Fail!
                this.failed(CapUpgradeAssistantError.ConnectFailed, x.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Already connected to upgrade lib -- proceed!");
        }
    }

    private void disconnectUpgrade(boolean successfulAndFinished) throws Exception {
        Log.d(TAG, "Disconnecting upgrade!");

        //Disconnect, if not already disconnected...
        if (this.communicator.getUpgradeConnected()) {
            Log.d(TAG, "Connected -- disconnect from upgrade lib!");

            this.changeState(CapUpgradeAssistantState.Disconnecting, 0, null);

            //Try to disconnect upgrade...
            try {
                this.communicator.resetAwaitedEventHistory();
                this.communicator.doUpgradeDisconnect();

                //Await CAP-event that we're connected...
                CapCommunicatorEvent event = this.communicator.awaitAnyEvent(new CapCommunicatorEvent[]{
                        CapCommunicatorEvent.UpgradeDisconnected
                }, 10000);

                Log.d(TAG, "Got awaited event; " + event);

                if (event != CapCommunicatorEvent.UpgradeDisconnected) {
                    Log.w(TAG, "Failed!");
                    throw new Exception("Did not receive expected disconnect success event");
                }

                //Fire "completed" CAP notif to the headset
                if (successfulAndFinished)
                    this.communicator.doUpgradeCompleted();

            } catch (Exception x) {
                Log.w(TAG, "Disconnection failed -- reported reason:" + x.getMessage());

                //Fail!
                this.failed(CapUpgradeAssistantError.DisconnectFailed, x.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Already disconnected from upgrade lib!");
        }
    }

    private void processRequestForBytes(int nbrOfBytes, int offset) throws Exception {
        Log.d(TAG, "Processing request for more bytes (nbrOfbytes: " + nbrOfBytes + " , offset: " + offset + ", totalOffset: " + this.dataOffset);

        //Well -- we're asked to transfer data... this CAN be the case when we have failed to be part of the finishing process after a previous OTA,
        // so let's check if we have any downloaded data to work with -- if we DON't, let's abort the upgrade...
        if (this.data == null || this.data.size() == 0) {
            Log.d(TAG, "No data downloaded -- most likely a previous disconencted OTA that we want to bring back to life -- abort!");

            //Reset upgrade identifier...
            this.communicator.setUpgradeIdentifier(0);

            //Abort the upgrade...
            this.abortUpgrade(false);

            return;
        }

        //Any offset requested from device?
        if (offset > 0 || (this.dataOffset + offset) < this.data.size()) {
            //Append offset to our data-offset
            this.dataOffset += offset;
        }

        //How much data is left?
        boolean lastChunk = false;
        if ((this.dataOffset + nbrOfBytes) >= this.data.size()) {
            //Mark as "last-chunk".
            lastChunk = true;
        }

        //Start measuring time?

        //Calc process value...
        int transferProgress = (int) ((this.dataOffset + nbrOfBytes) * 100.0 / this.data.size());

        //If there's no reference timestamp to work with...
        if (this.dateTimeEstimateLastReference == 0) {
            //Let's assign a good first time reference...
            this.dateTimeEstimateLastReference = System.currentTimeMillis();
        }

        //Increment data-tracker...
        this.dateTimeEstimateNbrOfTransferredBytesSinceLastReference += nbrOfBytes;

        //Enough data to calc an estimate on?
        if (this.dateTimeEstimateNbrOfTransferredBytesSinceLastReference > 2000) {
            //How long time has passed?
            long elapsedTimeMillis = System.currentTimeMillis() - this.dateTimeEstimateLastReference;

            //So -- we now know how long time we needed to transfer X bytes
            int remainingNbrOfBytes = (int) this.data.size() - (this.dataOffset + nbrOfBytes);

            long remainingTimeMillis = (elapsedTimeMillis / this.dateTimeEstimateNbrOfTransferredBytesSinceLastReference) * remainingNbrOfBytes;

            //Update our estimate...
            this.dateTimeEstimate = new Date(System.currentTimeMillis() + remainingTimeMillis);

            //Reset counters and prepare for next measurement...
            this.dateTimeEstimateNbrOfTransferredBytesSinceLastReference = 0;
            this.dateTimeEstimateLastReference = System.currentTimeMillis();
        }

        this.changeState(CapUpgradeAssistantState.Transferring, transferProgress, this.dateTimeEstimate);

        Log.d(TAG, ">> Progress: " + transferProgress + "% (estimate finish time: " + this.dateTimeEstimate);

        //Await comm-access
        this.communicator.awaitUpgradeProtocolIdle(10000, 500);

        //Extract chunk from upgrade data...
        byte[] chunk = this.data.getBytes(this.dataOffset, nbrOfBytes);

        //If we get here -- we're done...
        this.dataOffset += nbrOfBytes;

        //Send chunk!
        this.communicator.upgradeSendData(chunk, lastChunk);

        //Finally -- IFF we just sent the last chunk...
        if (lastChunk) {
            //Last chunk was successfully sent! Hence, resume point should be updated!
            this.resumePoint = CapUpgradeResumePoint.PreValidate;

            this.validateUpgrade();
        }
    }

    private void processErrorWarnIndication(CapUpgradeHostStatus hostStatus) throws Exception {
        Log.d(TAG, "Processing error warning indication with host status/error code:" + hostStatus);

        //Brute-force abort any current upgrade requests...
        this.communicator.getProtocol().abortUpgradeRequestWithReceivedHostStatus(hostStatus);

        //Await comm-access since we perhaps sent a request and are still waiting for some kind of response...
        this.communicator.awaitUpgradeProtocolIdle(10000, 500);

        switch (hostStatus) {
            case WarnSyncIdIsDifferent: {
                //The sync id is different!
                Log.d(TAG, "Invalid sync id -- let's abort and restart upgrade");

                //... so we need to abort and restart the upgrade...
                this.abortUpgrade(true);

                break;
            }

            case ErrorBatteryLow: {
                //Low battery warning from device -- don't have any effect onth eactual upgrade process -- just a warning...
                Log.d(TAG, "Low battery warning from device");
                break;
            }

            case ErrorPartitionOpenFailed: {
                //Opps -- we're in partition limbo where the device for whatever reason has become inconsistent and we need to reset the
                // upgrade process entirely and start from scratch!
                Log.d(TAG, "Partition open issue detected in device. Let's try to recover!");

                this.recoverFromPartitionInconsistency();

                break;
            }

            case ErrorPartitionCloseFailedPsSpace: {
                Log.d(TAG, "Partition close failure - most likley reboot to resume needed");

                //Respond to error indication...
                this.communicator.upgradeConfirmError(hostStatus);

                //Then -- do nothing, and trust that the device will ask for a reboot to resume...
                Log.d(TAG, "Await request to reboot...");

                break;
            }

            default: {
                //Default action; Fatal error, die!
                Log.d(TAG, "Something else happened (" + hostStatus + "), and it's fatal -- so let's confirm the error code and disconnect!");

                //Respond to error indication...
                this.communicator.upgradeConfirmError(hostStatus);

                //Disconnect upgrade - we're done!
                this.disconnectUpgrade(false);

                //Fail!
                failed(CapUpgradeAssistantError.FatalError, "Internal upgrade error-code: " + hostStatus);

                break;
            }
        }
    }

    private void recoverFromPartitionInconsistency() throws Exception {
        Log.d(TAG, "Try to recover from partition inconsistency...!");

        //Reset upgrade knowledge
        this.communicator.doUpgradeReset();

        //... and then reset filesystem info to the defalt values...
        this.communicator.setFilesystemDefaults();

        //... and then fianlly, disconnect and restart the upgrade...
        this.disconnectUpgrade(false);

        //Fail!
        this.failed(CapUpgradeAssistantError.PartitionInconsistency, "Partition inconsistency detected!\nReboot device and try again!");
    }

    private void abortUpgrade(boolean restart) throws Exception {
        Log.d(TAG, "Aborting upgrade -- restart: " + restart);

        this.changeState(CapUpgradeAssistantState.Aborting, 0, null);

        try {
            this.communicator.upgradeAbort();
        } catch (Exception x) {
            Log.w(TAG, "Failed (" + x.getMessage() + ") trying to abort upgrade");

            //Fail!
            failed(CapUpgradeAssistantError.AbortFailed, x.getMessage());
        }

        try {
            //Wait for block-done event while cleaning up...
            //Await CAP-event that we're connected...
            CapCommunicatorEvent event = this.communicator.awaitAnyEvent(new CapCommunicatorEvent[]{
                    CapCommunicatorEvent.UpgradeBlockingDone
            }, 5000);
        } catch (Exception x) {
            Log.w(TAG, "No block done event -- perhaps the headset didn't support it...", x);
        }

        //Then -- should we restart it...?
        if (restart) {
            //Try to restart...
            this.startUpgradeProcess();
        } else {
            this.changeState(CapUpgradeAssistantState.Aborted, 0, null);
        }
    }

    private void validateUpgrade() throws Exception {
        Log.d(TAG, "Validating upgrade");

        this.communicator.upgradeSendValidationRequest();
    }

    private void decideOnCommit() throws Exception {
        Log.d(TAG, "Time to decide on commit of upgrade");

        //If there's a delegate that has implemented our decision function -- ask it...
        if (this.delegate != null) {
            //Ask delegate to make a decision...
            this.delegate.shouldCommitUpgrade(this);
        } else {
            //No delegate -- auto proceed and tell the device what we do!
            this.proceedAtCommit(true);
        }
    }

    private void decideOnTransferComplete() throws Exception {
        Log.d(TAG, "Time to decide on transfer complete");

        //If there's a delegate that has implemented our decision function -- ask it...
        if (this.delegate != null) {
            //Ask delegate to make a decision...
            this.delegate.shouldProceedAtTransferComplete(this);
        } else {
            //No delegate -- auto proceed and tell the device what we do!
            this.proceedAtTransferComplete(true);
        }
    }

    private void decideOnEraseSqif() throws Exception {
        Log.d(TAG, "Time to decide on erase SQIF");

        //Auto-proceed...
        this.communicator.upgradeEraseSqif();
    }

    private void processReceivedCommunicatorEvent(CapCommunicatorEvent event, byte[] data) throws Exception {
        Log.d(TAG, "Time to process received comm event: " + event);

        //Ok -- interesting -- anything of interest to us?
        switch (event) {
            case UpgradeApplyIndication: {
                Log.d(TAG, "Upgrade apply indicator detected (upgrade state: " + this.state + " )");

                //Well -- this is expected if we're "rebooting" -- else we have encountered some kind of strangeness and
                //the upgrade want us to reboot to proceed...
                if (isRebooting()) {
                    Log.d(TAG, "Auto-apply since we've already decided to reboot and proceed with the upgrade... ");
                    this.communicator.doUpgradeApply(0);
                } else {
                    Log.d(TAG, "NOT in reboot-state -- ask delegate for permission to reboot to resume...");

                    //Any delegate?
                    if (this.delegate != null) {
                        this.delegate.shouldRebootAndResume(this);
                    } else {
                        //No delegate -- auto proceed without delay
                        this.proceedRebootAndResume(0);
                    }
                }

                break;
            }

            case UpgradeBlocking: {
                Log.d(TAG, "Upgrade block indicator -- let's wait...");

                this.changeState(CapUpgradeAssistantState.Blocking, 0, null);

                break;
            }

            case UpgradeBlockingDone: {
                Log.d(TAG, "Upgrade block DONE indicator!");
                break;
            }

            default: {
                //Ignore...
                break;
            }
        }
    }

    ///////////////////////////////
    //Public upgrade methods...

    public void setCommunicator(CapCommunicator communicator) {
        if (communicator != null) {
            Log.d(TAG, "Linking communicator to assistant");

            //Set the comm as new reference!
            this.communicator = communicator;

            //Ensure that we're the upgrade delegate for the communicator's protocol...
            this.communicator.getProtocol().setUpgradeDelegate(this);
        } else {
            Log.d(TAG, "Communicator removed from assistant -- most likley due to disconnection");

            //Abort any ongoing upgrade command...
            if (this.communicator.getProtocol() != null)
                this.communicator.getProtocol().abortUpgradeRequestWithReceivedHostStatus(CapUpgradeHostStatus.Unknown);

            //Remove any previous comm-reference
            this.communicator = null;

            //Reset any offset we might have...
            this.dataOffset = 0;
        }
    }

    public CapCommunicator getCommunicator() {
        return this.communicator;
    }

    public int getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public boolean upgradeUsingUrlContents(String urlString) throws Exception {
        Log.d(TAG, "Time to upgrade using URL: " + urlString);

//        SharedPreferences prefs = SharedPrefsUtil.getPrefs(EarinApplication.getContext());
//        String storedDownloadUrl = prefs.getString(PREFS_KEY_DOWNLOAD_URL, null);
//        String storedFotaData = prefs.getString(PREFS_KEY_FOTA_DATA, null);
//        if (urlString.equals(storedDownloadUrl) && storedFotaData != null) {
//            return upgradeUsingData(Base64.decode(storedFotaData, Base64.DEFAULT), storedDownloadUrl.hashCode());
//        }

        //Valid URL
        URL url = new URL(urlString);

        //Open connection...
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        // set authentication
        urlConnection.setRequestProperty("Authorization", "Basic " +
                Base64.encodeToString("download:OAd2gz2vDIqZ1P9OBaSboDdhYxDc4c80".getBytes(), Base64.NO_WRAP));

        // set up some things on the connection
        urlConnection.setRequestMethod("GET");
//        urlConnection.setDoOutput(true);

        // and connect!
        urlConnection.connect();
        Log.d(TAG, "urlConnection response " + urlConnection.getResponseCode());

        InputStream inputStream = urlConnection.getInputStream();

        // this is the total size of the file
        int totalSize = urlConnection.getContentLength();
        // variable to store total downloaded bytes
        int downloadedSize = 0;

        //Create buffer object to hold the data...
        ByteBuffer downloadedDataBuffer = new ByteBuffer(totalSize);

        // create a buffer...
        byte[] buffer = new byte[1024];
        int bufferLength = 0;

        //Download everything...
        while ((bufferLength = inputStream.read(buffer)) > 0) {
            downloadedDataBuffer.appendBytes(buffer, bufferLength);

            // add up the size so we know how much is downloaded
            downloadedSize += bufferLength;

            //Calc progress and feedback to delegate...
            int progress = (int) (downloadedSize * 100.0 / totalSize);
            this.changeState(CapUpgradeAssistantState.Downloading, progress, null);
        }

        //Extract identifier...
        int identifier = url.hashCode();

//        String serializedByteBuffer = Base64.encodeToString(downloadedDataBuffer.getAllBytes(), Base64.DEFAULT);
//        SharedPreferences.Editor prefsEditor = prefs.edit();
//        prefsEditor.putString(PREFS_KEY_DOWNLOAD_URL, urlString);
//        prefsEditor.putString(PREFS_KEY_FOTA_DATA, serializedByteBuffer);
//        prefsEditor.apply();

        //Proceed with upgrade!
        return upgradeUsingData(downloadedDataBuffer, identifier);
    }

    public boolean upgradeUsingData(byte[] data, int identifier) throws Exception {
        //Transfer data into buffer...
        ByteBuffer buffer = new ByteBuffer(data.length);
        buffer.appendBytes(data);

        return upgradeUsingData(buffer, identifier);
    }

    public boolean upgradeUsingData(ByteBuffer data, int identifier) throws Exception {
        Log.d(TAG, "Time to upgrade for identifier " + identifier + ", with " + (data != null ? data.size() : 0) + " bytes of data");

        //Prevent main-thread usage...
        if (Looper.myLooper() == Looper.getMainLooper()) {
            //Shit -- we're the main-thread! Abort this ASASP!
            Log.w(TAG, "Aborting; no go on main-thread upgrade...");
            throw new Exception("Main thread NOT supported as upgrade-thread");
        }

        Log.d(TAG, "Are we already upgrafing?");

        //Already upgrading?
        if (this.isUpgrading()) {
            //Opps -- can't run multple upgrades at the same time....
            return false;
        }

        this.data = data;
        this.identifier = identifier;

        this.startUpgradeProcess();

        //We're good!
        return true;
    }

    public boolean resumeUpgrade() throws Exception {
        Log.d(TAG, "Resume upgrade process -- if ongoing...");

        if (this.isUpgrading()) {
            this.startUpgradeProcess();
            return true;
        }

        return false;
    }

    public void proceedRebootAndResume(int deferMillis) throws Exception {
        Log.d(TAG, "Proceed with reboot to be able to resume with " + deferMillis + "  millis defer");

        //Do it!
        this.communicator.doUpgradeApply(deferMillis);
    }

    public void proceedAtTransferComplete(boolean proceed) throws Exception {
        Log.d(TAG, "Proceed at transfer complete: " + proceed);

        //Assume we will continue...
        CapUpgradeTransferComplete action = CapUpgradeTransferComplete.Continue;
        if (!proceed)
            action = CapUpgradeTransferComplete.Abort;

        if (action == CapUpgradeTransferComplete.Continue) {
            //We're continuing -- consequently we'll enter a "await reconnection"-state where it's expected that the connection is lost while the device reboot and load the new FW.
            this.changeState(CapUpgradeAssistantState.Rebooting, 0, null);

            //Set the upgrade identifier to the headset, so that we can ask for it and "resume" an upgrade IFF we loose connection adn app crash or whatever...
            Log.d(TAG, "Store upgrade identifier (" + this.identifier + ") in device/headset IFF we loose the device during reboot...");
            this.communicator.setUpgradeIdentifier(this.identifier);
        }

        //Do it!
        this.communicator.upgradeTransferComplete(action);

        //Note; if we abort -- we also need to disconnect
        if (action == CapUpgradeTransferComplete.Abort) {
            //Disconnect upgrade - we're done!
            this.disconnectUpgrade(false);
        }
    }

    public void proceedAtCommit(boolean proceed) throws Exception {
        Log.d(TAG, "Proceed and commit: " + proceed);

        //Assume we will continue...
        CapUpgradeCommit action = CapUpgradeCommit.Continue;
        if (!proceed)
            action = CapUpgradeCommit.Abort;

        //Do it!
        this.communicator.upgradeCommit(action);

        //Note; if we abort, we're done and will reboot...
        if (action == CapUpgradeCommit.Abort) {
            this.changeState(CapUpgradeAssistantState.Aborted, 0, null);
        }
    }

    public void setState(CapUpgradeAssistantState state) {
        this.state = state;
    }

    public CapUpgradeAssistantState getState() {
        return this.state;
    }

    public boolean isUpgrading() {
        //filter out "end states" where we are NOT actually upgrading...
        switch (this.state) {
            case Idle:
            case Downloading:
            case Complete:
            case Failed:
            case Aborted: {
                //NO upgrading process active..
                return false;
            }

            default: {
                //Else, we're upgrading...
                return true;
            }
        }
    }

    public boolean isRebooting() {
        return this.state == CapUpgradeAssistantState.Rebooting;
    }

    public void receivedCommunicatorEvent(final CapCommunicatorEvent event, final byte[] data) {
        Log.d(TAG, "Assistant received comm event: " + event);

        //Dispatch...
        new Thread(new Runnable() {

            @Override
            public void run() {

                //Ok -- what happened?
                try {
                    processReceivedCommunicatorEvent(event, data);
                } catch (Exception x) {
                    Log.d(TAG, "Failed processing received command!", x);

                    //Fail!
                    failed(CapUpgradeAssistantError.FatalError, "Failed processing received CAP event: " + event);
                }
            }
        }).start();
    }

    //////////////////
    // Delegate impl.

    @Override
    public void receivedCapUpgradeCommand(final CapProtocolUpgradeHostCommand command, final ByteBuffer data) {
        Log.d(TAG, "Received CAP upgrade command " + command + " with " + (data != null ? data.size() : 0) + " bytes data");

        //We're most likely running in main thread, so let's switch to a new thread to ensure that we don't block anything...
        new Thread(new Runnable() {

            @Override
            public void run() {

                //Ok -- what happened?

                try {
                    switch (command) {
                        case TransferCompleteInd: {
                            Log.d(TAG, "Transfer complete detected!");

                            //Transfer of data is complete and we're validated and all -- proceed?
                            resumePoint = CapUpgradeResumePoint.PreReboot;

                            //Deal with commit request and decide how to proceed...
                            decideOnTransferComplete();

                            break;
                        }

                        case CommitRequest: {
                            Log.d(TAG, "Commit request detected!");

                            //Data is downloaded on the device!
                            resumePoint = CapUpgradeResumePoint.Commit;

                            //Deal with commit request and decide how to proceed...
                            decideOnCommit();

                            break;
                        }

                        case EraseSqifRequest: {
                            //Should we erase the SQIF?
                            resumePoint = CapUpgradeResumePoint.Erase;

                            decideOnEraseSqif();

                            break;
                        }

                        case DataBytesRequest: {
                            //Valid payload?
                            if (data.size() < 8) {
                                //Missing data in request...
                                Log.w(TAG, "Payload too small");
                                return;
                            }

                            int nbrOfRequestedBytes = data.getIntAt(0, true);
                            int offset = data.getIntAt(4, true);

                            //Process reqeuest!
                            processRequestForBytes(nbrOfRequestedBytes, offset);

                            break;
                        }

                        case AbortConfirm: {
                            //Aborted!
                            Log.d(TAG, "Upgrade aborted and confirmed!");

                            //Disconnect upgrade - we're done!
                            disconnectUpgrade(false);

                            break;
                        }

                        case CompleteInd: {
                            //We're done!
                            Log.d(TAG, "Upgrade complete!");

                            //Disconnect upgrade - we're done!
                            disconnectUpgrade(true);

                            //Happy days! We made it!
                            changeState(CapUpgradeAssistantState.Complete, 0, null);

                            break;
                        }

                        case ErrorWarnInd: {
                            //Error indicated from chip!
                            Log.w(TAG, "Error reported from chip!");

                            CapUpgradeHostStatus errorCode = CapUpgradeHostStatus.getEnumValue((byte) data.getShortAt(0, true));

                            Log.d(TAG, "Error code: " + errorCode);

                            //Extract error code... some might only lead to a warning, whereas some must force a disconnect the upgrade process...
                            // -- low battery is a warning
                            // -- "sync-id is different"...?

                            processErrorWarnIndication(errorCode);

                            break;
                        }

                        default: {
                            //Ignored...
                            break;
                        }
                    }
                } catch (CapProtocolUpgradeHostStatusException x) {
                    Log.w(TAG, "Ignored host status exception; " + x.getHostCommand());
                    return;
                } catch (Exception x) {
                    Log.d(TAG, "Failed processing received command!", x);

                    //Fail!
                    failed(CapUpgradeAssistantError.FatalError, "Failed processing received command " + command + " with error; " + x);
                }
            }

        }).start();
    }
}

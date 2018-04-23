package com.iamplus.earin.communication;

import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.cap.CapCommunicationController;
import com.iamplus.earin.communication.cap.CapUpgradeAssistant;
import com.iamplus.earin.communication.cap.CapUpgradeAssistantDelegate;
import com.iamplus.earin.communication.cap.CapUpgradeAssistantError;
import com.iamplus.earin.communication.cap.CapUpgradeAssistantState;
import com.iamplus.earin.communication.cap.transports.CapTransportPreference;
import com.iamplus.earin.util.SerialExecutor;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Linus on 2015-09-24.
 */
public class Manager implements CapUpgradeAssistantDelegate {
    private static final String TAG = Manager.class.getSimpleName();
    private static final String INTENT_BASE = "se.millsys.apps.capcontrol.Manager";

    private static Manager singleton;

    private CapCommunicationController capCommunicationController;
    private LocalBroadcastManager broadcastManager;

    private ArrayList<InternalRequest> pendingRequests;
    private Thread processRequestThread;

    private ArrayList<CapUpgradeAssistantDelegate> mCapUpgradeAssistantDelegates;


    public Manager() {
        Log.i(TAG, "Created");

        this.capCommunicationController = CapCommunicationController.getInstance();
        this.broadcastManager = LocalBroadcastManager.getInstance(EarinApplication.getContext());


        int apiVersion = Build.VERSION.SDK_INT;
        if (apiVersion > 23) {
            this.capCommunicationController.setTransportPreference(CapTransportPreference.Ble);
        } else {
            // We wan't to use SPP for devices running Android versions below 23
            this.capCommunicationController.setTransportPreference(CapTransportPreference.Spp);
        }
        this.pendingRequests = new ArrayList<>();
        this.processRequestThread = null;
        this.mCapUpgradeAssistantDelegates = new ArrayList<>();
    }

    public static Manager getSharedManager() {
        if (Manager.singleton == null) {
            Log.i(TAG, "Creating singleton instance");
            Manager.singleton = new Manager();
        }

        return Manager.singleton;
    }

    public CapCommunicationController getCapCommunicationController() {
        return this.capCommunicationController;
    }

    public void enqueRequest(String identifier, Runnable request) {
        //Synchronized access...
        Log.d(TAG, "Trying to enqueue a req.");
        synchronized (this.pendingRequests) {
            Log.d(TAG, "Enqueueing request for identifier " + identifier + " (there are currently " + this.pendingRequests.size() + " pending requests)");

            //Well, simply add the request to the end of the lst of pendign requests, and then clean-up the list by removing ALL occurances of THAT same identifier, given that it's NOT already executing, of course...
            this.pendingRequests.add(new InternalRequest(identifier, request));
        }

        //Make sure process thread is executing
        if (this.processRequestThread == null) {
            Log.d(TAG, "No process thread running -- create one!");

            //Create thread!
            this.processRequestThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    processEnqueuedRequests();
                    processRequestThread = null;
                }
            });

            this.processRequestThread.setPriority(Thread.MIN_PRIORITY);
            this.processRequestThread.start();
        } else {
            Log.d(TAG, "Process thread is already running -- rely on it!");
        }
    }

    private void processEnqueuedRequests() {
        Log.d(TAG, String.format("Processing all enqueued requests (currently " + this.pendingRequests.size() + " pending requests)"));

        while (this.processRequestThread != null && this.pendingRequests.size() > 0) {
            //Since we always ADD new requests to the end of the list, we should always dequeue from the beginning of the list to ensure that we take each request in order...

            InternalRequest request = null;

            //Synchronized access...
            Log.d(TAG, "Getting access to pending req.");
            synchronized (this.pendingRequests) {
                int requestIndex = 0;
                request = this.pendingRequests.get(0);
                Log.d(TAG, "Go for the first one, with index " + requestIndex + ", (id = " + request + ")");

                //Find the latest version of this request...
                for (int searchIndex = this.pendingRequests.size() - 1; searchIndex > requestIndex; searchIndex--) {
                    InternalRequest possiblyUpdatedRequest = this.pendingRequests.get(searchIndex);
                    if (possiblyUpdatedRequest.getIdentifier().equalsIgnoreCase(request.getIdentifier())) {
                        //Update indexes & request object...
                        requestIndex = searchIndex;
                        request = possiblyUpdatedRequest;

                        Log.d(TAG, "Found updated request at index " + requestIndex + " (request = " + request + ")");

                        //Done...
                        break;
                    }
                }

                //So -- let's remove *ALL* occurances of this request identifier...
                Log.d(TAG, "Sanitizing all requests " + request + "; there is a total of " + this.pendingRequests.size() + " pending requests");

                for (int searchIndex = 0; searchIndex < this.pendingRequests.size(); searchIndex++) {
                    InternalRequest testRequest = this.pendingRequests.get(searchIndex);
                    if (testRequest.getIdentifier().equalsIgnoreCase(request.getIdentifier())) {
                        //Remove it!
                        this.pendingRequests.remove(searchIndex);

                        //Adjust index...
                        searchIndex--;
                    }
                }

                Log.d(TAG, "Sanitizing done; there is now a total of " + this.pendingRequests.size() + " pending requests");
            }

            if (request != null) {
                //Get the request
                Log.d(TAG, "Executing request; " + request);

                try {
                    //Execute the request
                    request.execute();
                } catch (Exception x) {
                    Log.w(TAG, "Opps, we failed executing the request!", x);
                }
            }
        }
    }


    public void removePendingRequests() {
        synchronized (this.pendingRequests) {
            Log.d(TAG, "Clear all pending requests to prevent old stacked-up things... (current size of queue: " + this.pendingRequests.size() + ")");

            //Kill 'em all...
            this.pendingRequests.clear();
            this.processRequestThread = null;
        }
    }

    public void addCapUpgradeAssistants(CapUpgradeAssistantDelegate upgradeDelegate) {
        mCapUpgradeAssistantDelegates.add(upgradeDelegate);
    }

    public void removeCapUpgradeAssistants(CapUpgradeAssistantDelegate upgradeDelegate) {
        mCapUpgradeAssistantDelegates.remove(upgradeDelegate);
    }

    @Override
    public void upgradeAssistantChangedState(CapUpgradeAssistant assistant, CapUpgradeAssistantState state, int progress, Date estimate) {
        for (CapUpgradeAssistantDelegate upgradeDelegate : mCapUpgradeAssistantDelegates) {
            upgradeDelegate.upgradeAssistantChangedState(assistant, state, progress, estimate);
        }
    }

    @Override
    public void upgradeAssistantFailed(CapUpgradeAssistant assistant, CapUpgradeAssistantError error, String reason) {
        Log.d(TAG, "Changed upgrade assistant FAILED: " + error + ", reason: " + reason);
        for (CapUpgradeAssistantDelegate upgradeDelegate : mCapUpgradeAssistantDelegates) {
            upgradeDelegate.upgradeAssistantFailed(assistant, error, reason);
        }
//        new Handler(Looper.getMainLooper()).post(() -> {
//            mActivity.removeLastFragment();
//            Toast.makeText(mActivity, "Upgrade failed: " + error.toString(), Toast.LENGTH_LONG).show();
//
//        });
    }

    @Override
    public void shouldRebootAndResume(CapUpgradeAssistant assistant) {
        Log.d(TAG, "Should reboot and resume!");

        SerialExecutor.getInstance().execute(() -> {
            try {
                assistant.proceedRebootAndResume(0);
            } catch (Exception e) {
                Log.e(TAG, "Reboot and resume exception!");
                e.printStackTrace();
            }
        });
    }

    @Override
    public void shouldCommitUpgrade(CapUpgradeAssistant assistant) {
        Log.d(TAG, "Should commit upgrade!");
        SerialExecutor.getInstance().execute(() -> {
            try {
                assistant.proceedAtCommit(true);
            } catch (Exception e) {
                Log.e(TAG, "Error committing upgrade! " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void shouldProceedAtTransferComplete(CapUpgradeAssistant assistant) {
        Log.d(TAG, "Should proceed at transfer complete!");
        // We don't have a sub-delegate -> Proceed!
        SerialExecutor.getInstance().execute(() -> {
            if (mCapUpgradeAssistantDelegates.isEmpty()) {
                try {
                    assistant.proceedAtTransferComplete(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // We have a sub-delegate -> ask te sub delegate!
                for (CapUpgradeAssistantDelegate upgradeDelegate : mCapUpgradeAssistantDelegates) {
                    upgradeDelegate.shouldProceedAtTransferComplete(assistant);
                }
            }
        });
    }

    private class InternalRequest {
        private String identifier;
        private Runnable request;

        public InternalRequest(String identifier, Runnable request) {
            this.identifier = identifier;
            this.request = request;
        }

        public String getIdentifier() {
            return this.identifier;
        }

        public String toString() {
            return this.identifier;
        }

        public void execute() throws Exception {
            if (this.request != null)
                this.request.run();
        }
    }

}

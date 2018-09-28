/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.MenuItem;

import com.qualcomm.qti.voiceassistant.service.VoiceAssistantService;

import java.lang.ref.WeakReference;

/**
 * <p>This class is the abstract activity to extend for each activity of this application which needs to communicate
 * with the service in charge of communicating with a Bluetooth device for the assistant feature. This class binds to
 * an Android service which implements {@link VoiceAssistantService VoiceAssistantService}.</p>
 */

public abstract class ServiceActivity extends PermissionsActivity {

    /**
     * The service used to communicate with a Bluetooth device for the assistant feature.
     */
    VoiceAssistantService mService;
    /**
     * The service connection object to manage the service bind and unbind.
     */
    private final ServiceConnection mServiceConnection = new ActivityServiceConnection(this);
    /**
     * The handler used by the service to communicate with this activity.
     */
    private ActivityHandler mHandler;
    /**
     * To know if this activity is in the pause state.
     */
    private boolean mIsPaused;


    // ====== ACTIVITY METHODS =====================================================================

    @Override // Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.init();
    }

    @Override // Activity
    protected void onResume() {
        super.onResume();

        mIsPaused = false;

        if (mService == null) {
            bindService();
        }
    }

    @Override // Activity
    protected void onPause() {
        super.onPause();
        mIsPaused = true;
    }

    @Override // Activity
    protected void onDestroy() {
        super.onDestroy();

        if (mService != null) {
            mService.removeHandler(mHandler);
            mService = null;
            unbindService(mServiceConnection);
        }

    }

    // to implement the back navigation for all classes which have a parent
    @Override  // Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // ====== PRIVATE METHODS ======================================================================

    /**
     * <p>This method allows to init the bound service by defining this activity as a handler listening its messages.</p>
     */
    private void initService() {
        mService.addHandler(mHandler);
        mService.init();
    }

    /**
     * To initialise objects used in this activity.
     */
    private void init() {
        // the Handler to receive messages from the VoiceAssistantService once attached
        mHandler = new ActivityHandler(this);
    }

    /**
     * <p>To start the Android Service which will allow this application to communicate with a Bluetooth device.</p>
     * <p>Wait for {@link #onServiceConnected() onServiceConnected()} to be called.</p>
     */
    private void bindService() {
        // bind the service
        Intent gattServiceIntent = new Intent(this, VoiceAssistantService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }


    // ====== ABSTRACT METHODS =====================================================================

    /**
     * <p>This method is called when the connected service is sending a message to the activity.</p>
     *
     * @param msg
     *          The message received from the service.
     */
    protected abstract void handleMessageFromService(Message msg);

    /**
     * <p>This method is called when the service has been bound to this activity.</p>
     */
    protected abstract void onServiceConnected();

    /**
     * <p>This method is called when the service has been unbound from this activity.</p>
     */
    @SuppressWarnings("EmptyMethod")
    protected abstract void onServiceDisconnected();


    // ====== INNER CLASS ==========================================================================

    /**
     * <p>This class is used to be informed of the connection state of the service.</p>
     */
    private static class ActivityServiceConnection implements ServiceConnection {

        /**
         * The reference to this activity.
         */
        final WeakReference<ServiceActivity> mActivity;

        /**
         * The constructor for this activity service connection.
         *
         * @param activity
         *            this activity.
         */
        ActivityServiceConnection(ServiceActivity activity) {
            super();
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            ServiceActivity parentActivity = mActivity.get();

            if (componentName.getClassName().equals(VoiceAssistantService.class.getName())) {
                parentActivity.mService = ((VoiceAssistantService.LocalBinder) service).getService();
                parentActivity.initService();
                parentActivity.onServiceConnected(); // to inform subclass
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (componentName.getClassName().equals(VoiceAssistantService.class.getName())) {
                ServiceActivity parentActivity = mActivity.get();
                parentActivity.mService = null;
                parentActivity.onServiceDisconnected(); // to inform subclass
            }
        }
    }

    /**
     * <p>This class is for receiving and managing messages from a
     * {@link VoiceAssistantService VoiceAssistantService}.</p>
     */
    private static class ActivityHandler extends Handler {

        /**
         * The reference to this activity.
         */
        final WeakReference<ServiceActivity> mReference;

        /**
         * The constructor for this activity handler.
         *
         * @param activity
         *            this activity.
         */
        ActivityHandler(ServiceActivity activity) {
            super();
            mReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ServiceActivity activity = mReference.get();
            if (!activity.mIsPaused) {
                activity.handleMessageFromService(msg);
            }
        }
    }
}

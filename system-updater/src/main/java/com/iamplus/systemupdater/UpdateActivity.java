package com.iamplus.systemupdater;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;

import com.iamplus.systemupdater.ui.BaseUi;
import com.iamplus.systemupdater.ui.DownloadPausedUi;
import com.iamplus.systemupdater.ui.DownloadUi;
import com.iamplus.systemupdater.ui.ErrorUi;
import com.iamplus.systemupdater.ui.IdleUi;
import com.iamplus.systemupdater.ui.LowBatteryUi;
import com.iamplus.systemupdater.ui.NoUpdatesUi;
import com.iamplus.systemupdater.ui.UiCallbacks;
import com.iamplus.systemupdater.ui.UnknownUi;
import com.iamplus.systemupdater.ui.UpdateAvailableUi;
import com.iamplus.systemupdater.ui.UpdateDoneUi;
import com.iamplus.systemupdater.ui.UpdateReadyUi;
import com.iamplus.systemupdater.ui.WaitingUi;

public class UpdateActivity extends Activity implements UiCallbacks, UpdateManager.UpdateListener {
    private static String TAG = "FOTA UpdateActivity";

    final Context context = this;
    private Button button;
    private UpdateManager mUpdateManager;
    private BaseUi mUi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUpdateManager = UpdateManager.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleOnResume();
        mUpdateManager.register(this);
        loadUi();
        Utils.cancelNotifications(this);
    }

    private void handleOnResume() {
        if (getIntent() == null) {
            return;
        }
        String action = getIntent().getAction();

        if (action.equals("aneeda.UPDATE_COMPLETE")) {
            mUpdateManager.onUpdateComplete(true);
            finish();
        } else if (action.equalsIgnoreCase("aneeda.UPDATE_ABORT")) {
            onQuit();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUpdateManager.unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUpdateManager.getState() != UpdateManager.State.DOWNLOADING_UPDATE) {
            onQuit();
        }
    }

    private void createActionItems() {
        // TODO kaustubh
//        mActionPanelLayoutWrapper.setActionItems(null);
//        if(mUpdateManager.canCancelUpdate())
//            mActionPanelLayoutWrapper.addActionItem(R.id.cancel_update, getString(R.string.cancel_update));
    }

    private BaseUi getUi() {
        switch (mUpdateManager.getState()) {
            case IDLE:
                return new IdleUi();
            case QUERYING_UPDATE:
                return WaitingUi.newInstance(R.string.querying_update);
            case NO_UPDATES:
                return new NoUpdatesUi();
            case UPDATE_AVAILABLE:
                return new UpdateAvailableUi();
            case DOWNLOADING_UPDATE:
                return new DownloadUi();
            case UPDATE_READY:
                return new UpdateReadyUi();
            case INSTALLING_UPDATE:
                return WaitingUi.newInstance(R.string.installing_update);
            case UPDATE_DONE:
                return new UpdateDoneUi();
            case VALIDATE_UDPATE:
                return WaitingUi.newInstance(R.string.validating_update);
            case WAITING_LOW_BATTERY:
                return new LowBatteryUi();
            case WAITING_TO_INSTALL:
                return WaitingUi.newInstance(R.string.about_to_install);
            case DOWNLOAD_PAUSED:
                return new DownloadPausedUi();
            case ERROR:
                return new ErrorUi();
            default:
                return new UnknownUi();
        }
    }

    private void loadUi() {
        mUi = getUi();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, mUi);
        transaction.disallowAddToBackStack();
        transaction.commit();
        createActionItems();
    }

    public void log(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    public void onStateChanged() {

        loadUi();
    }

    @Override
    public void onDownloadProgressChanged(int percentage, int downloadSize) {
        if (mUi != null && mUi instanceof DownloadUi) {
            DownloadUi downloadUi = (DownloadUi) mUi;
            downloadUi.setProgress(percentage, downloadSize);
        }
    }

    @Override
    public void onNetworkChange(boolean connected) {
        if (mUi != null) {
            mUi.onNetworkChange(connected);
        }
    }

    @Override
    public void onCheckForUpdates() {
        mUpdateManager.checkForUpdates();
    }

    @Override
    public void onQuit() {
        mUpdateManager.cancelUpdate();
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onDownloadAndInstallUpdate() {
        mUpdateManager.downloadAndInstallUpdate();
    }

    @Override
    public void onInstallUpdate() {
        Intent intent = new Intent();
        intent.setAction("aneeda.UPDATE_BUTTONS");
        intent.putExtra("UPDATE_FILE_URL", mUpdateManager.getUpdateInfo().getFilePath(this));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.v(TAG,"ActivityNotFoundException while trying to install updates on controller");
        }
        //mUpdateManager.installUpdate();
    }

    @Override
    public void onPostponeUpdate() {
        mUpdateManager.postponeUpdate();
        finish();
    }

    @Override
    public void onRetryDownload() {
        mUpdateManager.downloadUpdate();
    }

    public void onActionClick(int id, String item) {
        if (id == R.id.cancel_update) {
            mUpdateManager.cancelUpdate();
        }
    }

    @Override
    public UpdateManager getUpdateManager() {
        if (mUpdateManager == null) {
            mUpdateManager = UpdateManager.getInstance(this);
        }
        return mUpdateManager;
    }
}

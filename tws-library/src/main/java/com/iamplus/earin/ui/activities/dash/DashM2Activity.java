package com.iamplus.earin.ui.activities.dash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dinuscxj.progressbar.CircleProgressBar;
import com.iamplus.earin.BuildConfig;
import com.iamplus.earin.R;
import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.BluetoothGattStatus;
import com.iamplus.earin.communication.cap.CapCommunicator;
import com.iamplus.earin.communication.cap.CapUpgradeAssistant;
import com.iamplus.earin.communication.cap.CapUpgradeAssistantDelegate;
import com.iamplus.earin.communication.cap.CapUpgradeAssistantError;
import com.iamplus.earin.communication.cap.CapUpgradeAssistantState;
import com.iamplus.earin.communication.models.BatteryReading;
import com.iamplus.earin.communication.models.DeviceAddress;
import com.iamplus.earin.communication.models.VolTrim;
import com.iamplus.earin.serviceoperations.FotaCheckOperation;
import com.iamplus.earin.ui.activities.BaseToolbarActivity;
import com.iamplus.earin.ui.activities.HelpActivity;
import com.iamplus.earin.ui.fragments.GainFragment;
import com.iamplus.earin.ui.fragments.SetBalanceFragment;
import com.iamplus.earin.ui.fragments.SetTransparencyFragment;
import com.iamplus.earin.ui.fragments.UpdateFragment;
import com.iamplus.earin.util.FirmwareVersion;
import com.iamplus.earin.util.NetworkUtil;
import com.iamplus.earin.util.SeekBarInputWrapper;
import com.iamplus.earin.util.SharedPrefsUtil;
import com.iamplus.earin.util.bluetooth.BleBroadcastUtil;
import com.iamplus.earin.util.bluetooth.BluetoothUtil;
import com.iamplus.earin.util.firebase.FirebaseUtil;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class DashM2Activity extends BaseToolbarActivity implements
        BleBroadcastUtil.BleEventListener, BleBroadcastUtil.BatteryUpdateListener,
        CapUpgradeAssistantDelegate, BleBroadcastUtil.NfmiUpdateListener {

    private static final String TAG = DashM2Activity.class.getSimpleName();

    private static final int DISCONNECT_WAIT_TIME = 10000;
    private static final int COMPLETING_UPDATE_WAIT_TIME = 40000;
    private static final int DEFAULT_BALANCE_VALUE = -7;

    private static final int START_UPGRADE_MIN_BATTERY = 30;
    public static final int REBOOT_UPGRADE_MIN_BATTERY = 20;

    private final int SETUP_MAX_NUMBER_OF_TRIES = 5;

    private int mConsecutivePercentageIncreases;

    private boolean mInitialized;

    private String mFotaDownloadLink;
    private String mFotaReleaseNote;
    private String mFotaVersion;

    private boolean mFirstEarbudUpdated;

    private ViewGroup mLoadProgressLayout;
    private ViewGroup mCompletingUpdateLayout;
    private ProgressBar mLoadProgressBar;


    private CircleProgressBar mBatteryProgressBar;
    private TextView mMonoModeTextView;
    private TextView mBalanceTextView;
    private TextView mGainTextView;
    private TextView mTransparencyTextView;


    private Snackbar mUpdateSnackBar;

    private boolean mShouldShowSnackBar;
    private CapCommunicator mCommunicator;
    private CapUpgradeAssistant mUpgradeAssistant;


    //    private Bounds mBalanceBounds;
    //    private Bounds mTransparencyBounds;
    //    private Bounds mGainBounds;
    private VolTrim mVolTrim;
    private int mBalanceValue;
    private int mTransparencyMode;
    private int mTransparencyValue;
    private int mNoiseLevelValue;
    private boolean mNfmiConnected;
    private int mCurrentBatteryLevel;

    private int mSoundMode;
    private boolean mShouldCheckSlaveOnNfmiConnect;


    private View.OnClickListener mPropertyOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!mInitialized) {
                return;
            }
            if (view.equals(mBalanceTextView)) {
                if (!mNfmiConnected) {
                    return;
                }
                SeekBarInputWrapper.createFirst(mBalanceValue, -14, 0, 7);
                SeekBarInputWrapper.setOnFinishEditingListener((firstValue, secondValue) -> {
                    Log.v(TAG, "Balance set command Try: " + firstValue);
                    VolTrim tempBalance;
                    if (mSoundMode == 2) {
                        tempBalance = new VolTrim(firstValue <= -7 ? 0 : -7 - firstValue, firstValue >= -7 ? 0 : firstValue + 7);
                    } else {
                        tempBalance = new VolTrim(firstValue >= -7 ? 0 : firstValue + 7, firstValue <= -7 ? 0 : -7 - firstValue);
                    }
                    if (mSoundMode == 2 || mSoundMode == 3) {
                        Manager.getSharedManager().enqueRequest("Balance", () -> {
                            try {
                                mCommunicator.setVolTrim(tempBalance);
                                mBalanceValue = firstValue;
                                Log.v(TAG, "Balance set command success: " + firstValue);
                                showToast("Balance set command success: " + firstValue);
                            } catch (Exception e) {
                                Log.e(TAG, "Balance set command failed: " + e.getMessage());
                                showToast("Balance set command failed: " + firstValue);
                            }
                        });
                    }
                });
                SeekBarInputWrapper.setOnExitListener(() -> runOnUiThread(() -> updateBalanceView()));
                openFullscreenFragment(SetBalanceFragment.getInstance(getString(R.string.balance), SetBalanceFragment.ORIENTATION_HORIZONTAL));
            } else if (view.equals(mGainTextView)) {
                openFullscreenFragment(new GainFragment());
//                switchMasterSlave(true);
            } else if (view.equals(mTransparencyTextView)) {
                if (!mNfmiConnected) {
                    return;
                }
                SeekBarInputWrapper.createFirst(mTransparencyValue, -14, 0, 7);
                SeekBarInputWrapper.createSecond(mNoiseLevelValue, -14, 0, 7);
                SeekBarInputWrapper.setOnFinishEditingListener((firstValue, secondValue) -> {
                    if (mTransparencyValue != firstValue) {
                        Manager.getSharedManager().enqueRequest("PassthroughStop", () -> {
                            try {
                                mCommunicator.setPassthroughStop(firstValue);
                                mTransparencyValue = firstValue;

                                Log.v(TAG, "Passthrough stop set command success: " + firstValue);
                                showToast("Passthrough stop set command success: " + firstValue);
                            } catch (Exception e) {
                                Log.v(TAG, "Passthrough stop set command failed for value: " + firstValue);
                                showToast("Passthrough stop set command failed for value: " + firstValue);
                            }
                        });
                        Manager.getSharedManager().enqueRequest("PassthroughPlay", () -> {
                            try {
                                mCommunicator.setPassthroughPlay(firstValue);
                                Log.v(TAG, "Passthrough play set command success: " + firstValue);
                                showToast("Passthrough play set command success: " + firstValue);
                            } catch (Exception e) {
                                Log.v(TAG, "Passthrough play set command failed for value: " + firstValue);
                                showToast("Passthrough play set command failed for value: " + firstValue);
                            }
                        });
                        Manager.getSharedManager().enqueRequest("PassthroughCall", () -> {
                            try {
                                mCommunicator.setPassthroughCall(firstValue);
                                Log.v(TAG, "Passthrough call set command success: " + firstValue);
                                showToast("Passthrough call set command success: " + firstValue);
                            } catch (Exception e) {
                                Log.v(TAG, "Passthrough call set command failed for value: " + firstValue);
                                showToast("Passthrough call set command failed for value: " + firstValue);
                            }
                        });
                    }
                    if (mNoiseLevelValue != secondValue) {
                        Log.v(TAG, "Nose level set request: " + secondValue);
                        Manager.getSharedManager().enqueRequest("Noise", () -> {
                            try {
                                mCommunicator.setNoiseLevel(secondValue);
                                mNoiseLevelValue = secondValue;
                                Log.v(TAG, "Noise level set command success: " + secondValue);
                                showToast("Noise level set command success: " + firstValue);
                            } catch (Exception e) {
                                Log.v(TAG, "Noise level set command failed for value: " + secondValue);
                                showToast("Noise level set command failed for value " + firstValue);
                            }
                        });
                    }

                });
                SeekBarInputWrapper.setOnExitListener(() -> runOnUiThread(() -> updateTransparencyView()));
                SeekBarInputWrapper.setOnModeChangeListener(mode ->
                        Manager.getSharedManager().enqueRequest("PassthroughMode", () -> {
                            try {
                                mCommunicator.setPassthroughModes(mode);
                                mTransparencyMode = mode;
                                Log.v(TAG, "Passthrough mode set command success: " + mode);
                            } catch (Exception e) {
                                Log.v(TAG, "Passthrough mode set command failed for value: " + mode);
                            }
                        }));

                openFullscreenFragment(SetTransparencyFragment.getInstance(getString(R.string.audio_transparency), mTransparencyMode));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_dash_m2);
        super.onCreate(savedInstanceState);

        initLayout();
        initSnackBar();

        showToolbarLeftIcon(true);
        showToolbarRightIcon(true);

        onConnect();
    }

    private void initLayout() {
        mLoadProgressLayout = findViewById(R.id.loadProgressLayout);
        mLoadProgressBar = findViewById(R.id.loadProgressBar);
        mCompletingUpdateLayout = findViewById(R.id.completingUpdateLayout);

        mBatteryProgressBar = findViewById(R.id.batteryProgressBar);
        mCurrentBatteryLevel = 100;

        mMonoModeTextView = findViewById(R.id.monoModeTextView);
        mGainTextView = findViewById(R.id.gainTextView);
        mBalanceTextView = findViewById(R.id.balanceTextView);
        mTransparencyTextView = findViewById(R.id.transparencyTextView);


        mGainTextView.setOnClickListener(mPropertyOnClickListener);
        mBalanceTextView.setOnClickListener(mPropertyOnClickListener);
        mTransparencyTextView.setOnClickListener(mPropertyOnClickListener);
    }

    private void initSnackBar() {
        mUpdateSnackBar = Snackbar.make(mContainer, getString(R.string.update_available), BaseTransientBottomBar.LENGTH_INDEFINITE);
        mUpdateSnackBar.setAction(R.string.click_here, view -> {
            openFullscreenFragment(UpdateFragment.newInstance(mFotaDownloadLink, mFotaReleaseNote, mFotaVersion, true, mFirstEarbudUpdated));
            hideSnackBar();
        });

        View snackBarView = mUpdateSnackBar.getView();
        snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.green));
        TextView snackBarTextView = snackBarView.findViewById(android.support.design.R.id.snackbar_text);
        snackBarTextView.setTextColor(ContextCompat.getColor(this, R.color.black87));
    }

    private void switchMasterSlave(final boolean shouldGetAddress) {

        String deviceAddress = DeviceAddress.getInstance().getFormattedSlaveAddress();
        if (deviceAddress != null) {
            onDisconnect(false, BluetoothGattStatus.Unknown);
            BluetoothUtil.getInstance(DashM2Activity.this).connectToDeviceByMacAddress(deviceAddress);
        } else if (mNfmiConnected && shouldGetAddress) {
            // there is a slave, but we don't know the MAC address -> request the address ->
            // -> wait -> try again
            new Thread(() -> {
                try {
                    Log.v(TAG, "Trying to switch while NFMI is connected, but no slave address -> try to get slave address");
                    Manager manager = Manager.getSharedManager();
                    manager.enqueRequest("getAddress", () -> {
                        try {
                            mCommunicator.getAddress();
                        } catch (Exception e) {
                            Log.e(TAG, "Error getting Connected address:" + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                    Thread.sleep(500);
                    // if it fails again, don't bother repeating this step
                    switchMasterSlave(false);

                } catch (InterruptedException e) {
                    Log.e(TAG, "InterruptedException:" + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        } else if (!mNfmiConnected) {
            Log.v(TAG, "Trying to switch while NFMI is not connected");
        } else {
            Log.v(TAG, "Failed to get slave address evet though slave is connected -> abort!");
        }
    }

    @Override
    public void onConnect() {
        runOnUiThread(() -> {
            showToast("Regained BLE Connection");
            Manager manager = Manager.getSharedManager();
            mCommunicator = manager.getCapCommunicationController().getConnectedCommunicator();
            mUpgradeAssistant = mCommunicator.getUpgradeAssistant();
            Log.v(TAG, "Regained BLE Connection mCommunicator: " + mCommunicator);
            checkCanGetInitialValues();
        });

    }

    @Override
    public void onDisconnect(boolean isCompletingUpgrade, BluetoothGattStatus status) {
        Log.v(TAG, "onDisconnect");
        runOnUiThread(() -> {
            if (mInitialized) {
                showToast("Lost BLE Connection");

                mLoadProgressLayout.setVisibility(View.VISIBLE);
                if (isCompletingUpgrade) {
                    mLoadProgressBar.setVisibility(View.GONE);
                    mCompletingUpdateLayout.setVisibility(View.VISIBLE);
                } else {
                    mLoadProgressBar.setVisibility(View.VISIBLE);
                    mCompletingUpdateLayout.setVisibility(View.GONE);
                }

                mInitialized = false;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!BleBroadcastUtil.getInstance(DashM2Activity.this).isConnected()) {
                        Log.v(TAG, "onDisconnect -> go to HelpActivity");
                        Intent intent = new Intent(DashM2Activity.this, HelpActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }, isCompletingUpgrade ? COMPLETING_UPDATE_WAIT_TIME : DISCONNECT_WAIT_TIME);
            } else if (isCompletingUpgrade) {
                mLoadProgressLayout.setVisibility(View.VISIBLE);
                mLoadProgressBar.setVisibility(View.GONE);
                mCompletingUpdateLayout.setVisibility(View.VISIBLE);
            }

            mShouldShowSnackBar = false;
            hideSnackBar();
            if (mFragmentsArrayList.size() > 0) {
                Fragment lastFragment = mFragmentsArrayList.get(mFragmentsArrayList.size() - 1);
                if (lastFragment instanceof UpdateFragment) {
                    removeLastFragment();
                }
            }
        });
    }

    private void checkCanGetInitialValues() {

        if (!mUpgradeAssistant.isUpgrading()) {
            getInitialValues(0, new BleBroadcastUtil.CapResponseListener() {
                @Override
                public void onResponse(Object response) {
                    Log.v(TAG, "onResponse from communicator after Regained BLE");

                    BleBroadcastUtil.getInstance(DashM2Activity.this).setLastNfmiStatus(mNfmiConnected);
                    mInitialized = true;
                    setUpViews();
                }

                @Override
                public void onError(Exception exception) {

                    Log.e(TAG, "onError from communicator after Regained BLE");
                    FirebaseCrash.report(new Exception("Error setting up initial values: " + exception.getMessage()));
                    Manager manager = Manager.getSharedManager();
                    manager.getCapCommunicationController().cleanupCurrentConnection(true, BluetoothGattStatus.Unknown);
                    Intent intent = new Intent(DashM2Activity.this, HelpActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        } else {
            //Don't do anything -> wait until the upgrade finishes!
            showToast("Still Upgrading with state: " + mUpgradeAssistant.getState() + " -> Wait!");
            if (mUpgradeAssistant.getState() == CapUpgradeAssistantState.Starting) {
                Log.v(TAG, "Upgrade starting -> try again in 2 seconds!");
                new Handler(getMainLooper()).postDelayed(this::checkCanGetInitialValues, 2000);
            } else if (mUpgradeAssistant.getState() == CapUpgradeAssistantState.Transferring) {
                new Handler(getMainLooper()).postDelayed(() ->
                        openFullscreenFragment(UpdateFragment.newInstance(mFirstEarbudUpdated)), 1000);
            } else {
                this.onDisconnect(true, BluetoothGattStatus.Unknown);
            }

        }
    }

    private void getInitialValues(final int tryNumber, final BleBroadcastUtil.CapResponseListener capResponseListener) {
        Manager manager = Manager.getSharedManager();
        manager.enqueRequest("getInitialValues", () -> {
            try {
                String version;
                try {
                    version = mCommunicator.getVersion();
                } catch (Exception e) {

                    //WE DIDN'T CALL "DO UPGRADE COMPLETED" for some reason -> do that now!
                    mCommunicator.doUpgradeCompleted();
                    return;
                }
//                    mTransparencyBounds = mCommunicator.getPassthroughBounds();
//                    mBalanceBounds = mCommunicator.getVolTrimBounds();
//                    mGainBounds = mCommunicator.getNoiseLevelBounds();

                mVolTrim = mCommunicator.getVolTrim();
                mNoiseLevelValue = mCommunicator.getNoiseLevel();
                mTransparencyValue = mCommunicator.getPassthroughStop();

                mCommunicator.doRequestBatteryReadings();
                mNfmiConnected = mCommunicator.isNfmiConnected();
                mCommunicator.getAddress();

                ArrayList<Integer> modes = mCommunicator.getPassthroughModes();
                if (modes.size() > 0) {
                    mTransparencyMode = modes.get(0);
                }

                int soundMode = mCommunicator.getSoundMode();
                calculateBalanceValue(soundMode);

                String lastSessionData = mCommunicator.getLastSessionData();
                FirebaseUtil.getInstance().sendLog(DashM2Activity.this, lastSessionData, version, null);

                Log.v(TAG, "Init Transparency all modes: " + modes.get(0) + " " + modes.get(1) + " " + modes.get(2));
                Log.v(TAG, "Init Transparency mode: " + mTransparencyMode);
                Log.v(TAG, "Init Transparency  value: " + mTransparencyValue);
                Log.v(TAG, "Init Noise level value: " + mNoiseLevelValue);
                Log.v(TAG, "Init Balance value: " + mBalanceValue);
                Log.v(TAG, "Init Sound mode: " + mSoundMode);

                // Initial values successfully retrieved
                runOnUiThread(() -> capResponseListener.onResponse(null));

            } catch (Exception e) {
                Log.e(TAG, "Init Cap command failed: " + e.getMessage());
                if (tryNumber < SETUP_MAX_NUMBER_OF_TRIES) {
                    getInitialValues(tryNumber + 1, capResponseListener);
                } else {
                    runOnUiThread(() -> capResponseListener.onError(new Exception("Cap command failed " + SETUP_MAX_NUMBER_OF_TRIES + "times. Exiting app!")));
                }
            }
        });
    }

    private void calculateBalanceValue(int mode) {
        if (mSoundMode != 2 && mSoundMode != 3) {
            mBalanceValue = DEFAULT_BALANCE_VALUE;
            if (mode == 3) {
                mBalanceValue += mVolTrim.getMaster();
                mBalanceValue += mVolTrim.getMaster();
            } else if (mode == 2) {
                mBalanceValue += mVolTrim.getSlave();
                mBalanceValue -= mVolTrim.getMaster();
            }
        }
        mSoundMode = mode;
    }

    private void setUpViews() {
        if (NetworkUtil.isInternetAvailable(this)) {
            checkForUpdate();
        }
        updateBalanceView();
        updateTransparencyView();
        updateMonoModeView();

        mLoadProgressLayout.setVisibility(View.GONE);
    }

    private void checkForUpdate() {
        if (!BuildConfig.DEBUG) {
            // We don't want FOTA in release variant!
            return;
        }
        final Manager manager = Manager.getSharedManager();

        CapCommunicator communicator = manager.getCapCommunicationController().getConnectedCommunicator();
        final CapUpgradeAssistant assistant = communicator.getUpgradeAssistant();
        Log.v(TAG, "Checking for FOTA Update");

        if (assistant == null) {
            Log.e(TAG, "CapUpgradeAssistant is null!!!");
            FirebaseCrash.report(new Exception("CapUpgradeAssistant is null when checking for update"));
            return;
        }

        if (!assistant.isUpgrading()) {
            getCurrentFirmwareVersion(new FirmwareVersion.FirmwareVersionListener() {
                @Override
                public void onResponse(HashMap<String, FirmwareVersion> versions) {
                    Log.v(TAG, "getCurrentFirmwareVersion -> onResponse");
                    final FirmwareVersion masterFirmwareVersion = versions.get(FirmwareVersion.MASTER);
                    final FirmwareVersion slaveFirmwareVersion = versions.containsKey(FirmwareVersion.SLAVE) ?
                            versions.get(FirmwareVersion.SLAVE) :
                            null;

                    // update Master
                    if (slaveFirmwareVersion == null || slaveFirmwareVersion.isGreaterOrEqualThan(masterFirmwareVersion)) {
//                        FotaCheckOperation.checkForFotaUpdate(DashM2Activity.this, "0", FirmwareVersion.fromString("Earin M-2, CSR FW: 2.0.86, NXP FW:01.05"), new FotaCheckOperation.FotaCheckHandler() {
                        FotaCheckOperation.checkForFotaUpdate(DashM2Activity.this, "0", masterFirmwareVersion, new FotaCheckOperation.FotaCheckHandler() {
                            @Override
                            public void onUpdateRequired(final String url, final String releaseNote, String csr, String nxp) {
                                Log.v(TAG, "FOTA onUpdateRequired: " + url);
                                SharedPreferences sharedPreferences = SharedPrefsUtil.getPrefs(EarinApplication.getContext());
                                String savedUrl = sharedPreferences.getString(UpdateFragment.PREF_KEY_DOWNLOAD_URL, "");
                                mFotaDownloadLink = url;
                                mFotaVersion = "CSR: " + csr + " , NXP: " + nxp;
                                mFotaReleaseNote = releaseNote;
                                mFirstEarbudUpdated = slaveFirmwareVersion == null || masterFirmwareVersion.isGreaterOrEqualThan(slaveFirmwareVersion);

                                if (canStartUpgrade()) {
                                    if (savedUrl.equals(url)) {
//                                    openFullscreenFragment(UpdateFragment.newInstance("https://firmware.earin.com/0/csr2.0.68nxp01.05/download/", releaseNote, false, mFirstEarbudUpdated));
                                        openFullscreenFragment(UpdateFragment.newInstance(mFotaDownloadLink, mFotaReleaseNote, mFotaVersion, false, mFirstEarbudUpdated));
                                    } else {
                                        showSnackBar();
                                    }
                                } else {
                                    showToast("Battery too low to start upgrade!");
                                }

                            }

                            @Override
                            public void onUpdateNotRequired() {
                                Log.v(TAG, "FOTA onUpdateNotRequired");
                                showToast("FOTA upgrade not required");
                                if (slaveFirmwareVersion == null) {
                                    Log.v(TAG, "FOTA onUpdateNotRequired -> in mono mode -> Check slave when NFMI is enabled!");
                                    mShouldCheckSlaveOnNfmiConnect = true;
                                }
                            }

                            @Override
                            public void onError(int statusCode) {
                                //TODO: recheck ?
                                FirebaseCrash.report(new Exception("Check for Fota update error, status code: " + statusCode));
                                Log.v(TAG, "FOTA onError");
                                showToast("FOTA check error, status code: " + statusCode);
                            }
                        });
                    } else if (masterFirmwareVersion.isGreaterThan(slaveFirmwareVersion)) { // Switch
                        Log.v(TAG, "Switching master/slave");
                        showToast("FOTA upgrade: Need to upgrade slave -> Switch");
                        switchMasterSlave(true);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, "Error retrieving device firmware version: " + exception.getMessage());
                    FirebaseCrash.report(new Exception("Error retrieving device firmware version: " + exception.getMessage()));
                }
            });
        } else {
            showToast("Device is already in upgrade mode");
            Log.v(TAG, "Device is already in upgrade mode");
        }
    }

    private void getCurrentFirmwareVersion(final FirmwareVersion.FirmwareVersionListener listener) {
        Log.v(TAG, "getCurrentFirmwareVersion");
        Manager manager = Manager.getSharedManager();
        manager.enqueRequest("getVersions", () -> {
            HashMap<String, FirmwareVersion> versions = new HashMap<>();
            try {
                versions.put(FirmwareVersion.MASTER, FirmwareVersion.fromString(mCommunicator.getVersion()));
                Log.v(TAG, "getCurrentFirmwareVersion -> get master version");
                if (mNfmiConnected) {
                    versions.put(FirmwareVersion.SLAVE, FirmwareVersion.fromString(mCommunicator.getSlaveVersion()));
                    Log.v(TAG, "getCurrentFirmwareVersion -> get slave version");
                }
                runOnUiThread(() -> listener.onResponse(versions));
            } catch (Exception e) {
                Log.e(TAG, "Could not get version -> use default!");
//                versions.put(FirmwareVersion.MASTER, FirmwareVersion.fromString("Earin M-2, CSR FW: 2.0.83, NXP FW:01.05"));
                runOnUiThread(() -> listener.onError(e));
            }
        });
    }

    public void powerOffEarbuds() {
        Manager manager = Manager.getSharedManager();
        manager.enqueRequest("powerOff", () -> {
            try {
                mCommunicator.doPowerOff();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                runOnUiThread(this::finish);
            }
        });
    }

    private void updateBalanceView() {
        runOnUiThread(() -> {
            if (!mNfmiConnected) {
                mBalanceTextView.setAlpha(0.3f);
            } else {
                mBalanceTextView.setAlpha(1f);
            }
        });
    }

    private void updateMonoModeView() {
        runOnUiThread(() -> {
            if (!mNfmiConnected) {
                mMonoModeTextView.setVisibility(View.VISIBLE);
            } else {
                mMonoModeTextView.setVisibility(View.GONE);
            }
        });
    }

    private void updateTransparencyView() {
        runOnUiThread(() -> {
            Log.v(TAG, "updateTransparencyView");
            if (!mNfmiConnected) {
                mTransparencyTextView.setAlpha(0.3f);
                mTransparencyTextView.setText(getText(R.string.transparency));
            } else {
                StringBuilder textBuilder = new StringBuilder(getText(R.string.transparency));
                textBuilder.append(" ");
                switch (mTransparencyMode) {
                    case SetTransparencyFragment.MODE_OFF:
                        textBuilder.append(getText(R.string.off));
                        break;
                    case SetTransparencyFragment.MODE_ON:
                        textBuilder.append(getText(R.string.on));
                        break;
                    case SetTransparencyFragment.MODE_AUTO:
                        textBuilder.append(getText(R.string.auto));
                        break;
                }
                mTransparencyTextView.setText(textBuilder.toString());
                mTransparencyTextView.setAlpha(1f);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        BleBroadcastUtil bleBroadcastUtil = BleBroadcastUtil.getInstance(DashM2Activity.this);
        if (!bleBroadcastUtil.isConnected()) {
            this.onDisconnect(false, BluetoothGattStatus.Unknown);
        } else {
            if (bleBroadcastUtil.getLastBatteryReading() != null) {
                this.onBatteryEvent(bleBroadcastUtil.getLastBatteryReading());
            }
            if (bleBroadcastUtil.getLastNfmiStatus() != null) {
                this.onNfmiStateChange(bleBroadcastUtil.getLastNfmiStatus());
            }
        }

        bleBroadcastUtil.addBleEventListener(this);
        bleBroadcastUtil.setBatteryUpdateListener(this);
        bleBroadcastUtil.setNfmiUpdateListener(this);
        Manager.getSharedManager().addCapUpgradeAssistants(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        BleBroadcastUtil bleBroadcastUtil = BleBroadcastUtil.getInstance(DashM2Activity.this);
        bleBroadcastUtil.removeBleEventListener(this);
        bleBroadcastUtil.removeBatteryUpdateListener();
        bleBroadcastUtil.removeNfmiUpdateListener();
        Manager.getSharedManager().removeCapUpgradeAssistants(this);
    }

    @Override
    public void onBackPressed() {
        if (mFragmentsArrayList.size() > 0) {
            Fragment lastFragment = mFragmentsArrayList.get(mFragmentsArrayList.size() - 1);
            if (!(lastFragment instanceof UpdateFragment)) {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void applyBlur() {
        super.applyBlur();
        hideSnackBar();
    }

    @Override
    protected void removeBlur() {
        super.removeBlur();
        if (mShouldShowSnackBar) {
            showSnackBar();
        }
    }

    private void showSnackBar() {
        if (canStartUpgrade()) {
            mUpdateSnackBar.show();
            mContainer.setPadding(0, 0, 0, 120);
            mShouldShowSnackBar = true;
        }
    }

    private void hideSnackBar() {
        mUpdateSnackBar.dismiss();
        mContainer.setPadding(0, 0, 0, 0);
    }

    public boolean canReboot() {
        Log.v(TAG, "Can reboot with battery level: " + mCurrentBatteryLevel);
        return mCurrentBatteryLevel > REBOOT_UPGRADE_MIN_BATTERY;
    }

    public boolean canStartUpgrade() {
        Log.v(TAG, "Can start upgrade with battery level: " + mCurrentBatteryLevel);
        return mCurrentBatteryLevel > START_UPGRADE_MIN_BATTERY;
    }

    @Override
    public void onBatteryEvent(BatteryReading batteryReading) {

        Log.v(TAG, "Battery reading. Is local: " + batteryReading.isLocal() + ", percentage: " + batteryReading.getPercentage());
        int lowBatteryValue = 15;
        int numConsecutiveIncreasesBeforeCommit = 5;

        if (batteryReading.getPercentage() > mCurrentBatteryLevel) {
            mConsecutivePercentageIncreases++;
        }

        if (batteryReading.getPercentage() < mCurrentBatteryLevel || mConsecutivePercentageIncreases >= numConsecutiveIncreasesBeforeCommit) {
            mConsecutivePercentageIncreases = 0;
            if (mCurrentBatteryLevel > lowBatteryValue && batteryReading.getPercentage() <= lowBatteryValue) {
                mBatteryProgressBar.setProgressEndColor(ContextCompat.getColor(DashM2Activity.this, R.color.red));
                mBatteryProgressBar.setProgressStartColor(ContextCompat.getColor(DashM2Activity.this, R.color.red));

            } else if (mCurrentBatteryLevel <= lowBatteryValue && batteryReading.getPercentage() > lowBatteryValue) {
                mBatteryProgressBar.setProgressEndColor(ContextCompat.getColor(DashM2Activity.this, R.color.white));
                mBatteryProgressBar.setProgressStartColor(ContextCompat.getColor(DashM2Activity.this, R.color.white));
            }
            mCurrentBatteryLevel = batteryReading.getPercentage();
            mBatteryProgressBar.setProgress(mCurrentBatteryLevel);
        }
    }

    public void showToast(String message) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(() -> {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onNfmiStateChange(boolean connected) {
        Log.v(TAG, "NFMI state change: " + connected);

        if (mInitialized && !mUpgradeAssistant.isUpgrading()) {
            if (connected != mNfmiConnected) {
                mNfmiConnected = connected;
                if (connected) {

                    Manager.getSharedManager().enqueRequest("getAddress", () -> {
                        try {
                            // refresh master/slave addresses on NFMI state change
                            mCommunicator.getAddress();
                            Log.v(TAG, "NFMI state change -> refresh slave address");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                    Manager.getSharedManager().enqueRequest("getSoundMode", () -> {
                        try {
                            int soundMode = mCommunicator.getSoundMode();
                            calculateBalanceValue(soundMode);
                            Log.v(TAG, "Sound mode: " + mSoundMode);
                        } catch (Exception e) {
                            Log.e(TAG, "Get sound mode command failed: " + e.getMessage());
                            calculateBalanceValue(2);
                        }
                    });
                    if (mShouldCheckSlaveOnNfmiConnect && NetworkUtil.isInternetAvailable(this)) {
                        mShouldCheckSlaveOnNfmiConnect = false;
                        checkForUpdate();
                    }

                } else {
                    DeviceAddress.getInstance().removeAddresses(false, true);
                }

                Log.v(TAG, "NFMI state change -> updateBalanceView");
                updateBalanceView();
                updateMonoModeView();
                updateTransparencyView();
            }
        } else {
            //We're not initialized -> don't do anything!
            mNfmiConnected = connected;
            Log.v(TAG, "NFMI state change -> Not initialized -> don't do anything!");
        }
    }

    @Override
    public void upgradeAssistantChangedState(CapUpgradeAssistant assistant, CapUpgradeAssistantState state, int progress, Date estimate) {
        Log.v(TAG, "CapUpgradeAssistant state changed to: " + state);
        if (state == CapUpgradeAssistantState.Complete && !mInitialized) {
            checkCanGetInitialValues();
        }
    }

    @Override
    public void upgradeAssistantFailed(CapUpgradeAssistant assistant, CapUpgradeAssistantError error, String reason) {
    }

    @Override
    public void shouldRebootAndResume(CapUpgradeAssistant assistant) {
    }

    @Override
    public void shouldCommitUpgrade(CapUpgradeAssistant assistant) {
    }

    @Override
    public void shouldProceedAtTransferComplete(CapUpgradeAssistant assistant) {
    }
}
package com.iamplus.earin.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dinuscxj.progressbar.CircleProgressBar;
import com.iamplus.earin.R;
import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.BluetoothGattStatus;
import com.iamplus.earin.communication.cap.CapCommunicator;
import com.iamplus.earin.communication.cap.CapUpgradeAssistant;
import com.iamplus.earin.communication.cap.CapUpgradeAssistantDelegate;
import com.iamplus.earin.communication.cap.CapUpgradeAssistantError;
import com.iamplus.earin.communication.cap.CapUpgradeAssistantState;
import com.iamplus.earin.ui.activities.dash.DashM2Activity;
import com.iamplus.earin.util.SerialExecutor;
import com.iamplus.earin.util.SharedPrefsUtil;

import java.util.Calendar;
import java.util.Date;

public class UpdateFragment extends BaseFragment implements CapUpgradeAssistantDelegate {

    private static final String TAG = UpdateFragment.class.getSimpleName();

    public static final String PREF_KEY_DOWNLOAD_URL = "downloadUrlKey";

    private static final String ARG_DOWNLOAD_URL = "downloadUrl";
    private static final String ARG_RELEASE_NOTE = "releaseNote";
    private static final String ARG_VERSION = "version";
    private static final String ARG_ASK_USER = "askUser";
    private static final String ARG_FIRST_EARBUD_UPDATED = "firstEarbudUpdated";

    private Manager mManager;
    private CapUpgradeAssistant mAssistant;
    private String mDownloadUrl;
    private String mReleaseNote;
    private String mVersion;
    private boolean mFirstEarbudUpdated;

    private LinearLayout mReadyToInstallLayout;
    private LinearLayout mUpdatingLayout;
    private LinearLayout mCompleteUpdateLayout;
    private LinearLayout mBatteryLowLayout;

    private CircleProgressBar mUpdatingProgressBar;
    private TextView mUpdatingDescriptionTextView;
    private TextView mUpdatingTitleTextView;

    private TextView mTimeRemainingTextView;

    //For new update
    public static UpdateFragment newInstance(String downloadUrl, String releaseNote, String version, boolean askUser, boolean firstEarbudUpdated) {
        UpdateFragment updateFragment = new UpdateFragment();

        Bundle args = new Bundle();
        args.putString(ARG_DOWNLOAD_URL, downloadUrl);
        args.putString(ARG_VERSION, version);
        args.putString(ARG_RELEASE_NOTE, releaseNote);
        args.putBoolean(ARG_ASK_USER, askUser);
        args.putBoolean(ARG_FIRST_EARBUD_UPDATED, firstEarbudUpdated);
        updateFragment.setArguments(args);

        return updateFragment;
    }

    //For continuing an update
    public static UpdateFragment newInstance(boolean firstEarbudUpdated) {
        UpdateFragment updateFragment = new UpdateFragment();


        Bundle args = new Bundle();
        args.putString(ARG_DOWNLOAD_URL, "");
        args.putString(ARG_VERSION, "");
        args.putString(ARG_RELEASE_NOTE, "");
        args.putBoolean(ARG_ASK_USER, false);
        args.putBoolean(ARG_FIRST_EARBUD_UPDATED, firstEarbudUpdated);
        updateFragment.setArguments(args);

        return updateFragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_update, container, false);

        mDownloadUrl = getArguments().getString(ARG_DOWNLOAD_URL);
        mVersion = getArguments().getString(ARG_VERSION);
        mReleaseNote = getArguments().getString(ARG_RELEASE_NOTE);
        mFirstEarbudUpdated = getArguments().getBoolean(ARG_FIRST_EARBUD_UPDATED);

        initReadyToUpdateLayout(rootView);
        initUpdatingLayout(rootView);
        initCompleteUpdateLayout(rootView);
        initBatteryLowLayout(rootView);

        mManager = Manager.getSharedManager();

        CapCommunicator communicator = mManager.getCapCommunicationController().getConnectedCommunicator();
        mAssistant = communicator.getUpgradeAssistant();
//        mAssistant.setDelegate(this);
        mManager.addCapUpgradeAssistants(this);

        boolean askUser = getArguments().getBoolean(ARG_ASK_USER);
        if (!askUser || mAssistant.isUpgrading()) {
            startUpgrade();
        }
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mManager.removeCapUpgradeAssistants(this);
    }

    private void initReadyToUpdateLayout(View rootView) {
        mReadyToInstallLayout = rootView.findViewById(R.id.readyToInstallLayout);
        TextView acceptUpdateTextView = rootView.findViewById(R.id.acceptUpdateTextView);
        TextView askMeLaterTextView = rootView.findViewById(R.id.askMeLaterTextView);
        TextView releaseNoteTextView = rootView.findViewById(R.id.releaseNoteTextView);
        TextView newVersionTextView = rootView.findViewById(R.id.newVersionTextView);
        releaseNoteTextView.setText(mReleaseNote);
        newVersionTextView.setText(mVersion);
        acceptUpdateTextView.setOnClickListener(view -> startUpgrade());
        askMeLaterTextView.setOnClickListener(view -> mActivity.removeLastFragment());
    }

    private void initUpdatingLayout(View rootView) {
        mUpdatingLayout = rootView.findViewById(R.id.updatingLayout);

        mUpdatingProgressBar = rootView.findViewById(R.id.updatingProgressBar);
        mUpdatingTitleTextView = rootView.findViewById(R.id.updatingTitleTextView);
        mUpdatingDescriptionTextView = rootView.findViewById(R.id.updatingDescriptionTextView);
        mTimeRemainingTextView = rootView.findViewById(R.id.timeRemainingTextView);
    }

    private void initCompleteUpdateLayout(View rootView) {
        mCompleteUpdateLayout = rootView.findViewById(R.id.completeUpdateLayout);
        TextView completeUpdateTextView = rootView.findViewById(R.id.completeUpdateTextView);
        completeUpdateTextView.setOnClickListener(view -> {
            ((DashM2Activity) mActivity).onDisconnect(true, BluetoothGattStatus.Unknown);
            SerialExecutor.getInstance().execute(() -> {
                try {
                    mAssistant.proceedAtTransferComplete(true);
                } catch (Exception e) {
                    Log.e(TAG, "Failed while proceeding with transfer!", e);
                }
            });
        });
    }

    private void initBatteryLowLayout(View rootView) {
        mBatteryLowLayout = rootView.findViewById(R.id.batteryLowLayout);
        TextView batteryLowOkTextView = rootView.findViewById(R.id.batteryLowOkTextView);
        TextView batteryLowDescriptionTextView = rootView.findViewById(R.id.batteryLowDescriptionTextView);
        batteryLowDescriptionTextView.setText(getString(R.string.battery_too_low_description, DashM2Activity.REBOOT_UPGRADE_MIN_BATTERY));
        batteryLowOkTextView.setOnClickListener(view ->
            new Handler(Looper.getMainLooper()).post(() -> mActivity.removeLastFragment()));
    }

    private void startUpgrade() {
        mReadyToInstallLayout.setVisibility(View.GONE);
        mUpdatingLayout.setVisibility(View.VISIBLE);
        if (!mAssistant.isUpgrading()) {
            mManager.enqueRequest("Upgrade", () -> {
                try {
                    mAssistant.upgradeUsingUrlContents(mDownloadUrl);
                    SharedPreferences.Editor editor = SharedPrefsUtil.getPrefs(EarinApplication.getContext()).edit();
                    editor.putString(PREF_KEY_DOWNLOAD_URL, mDownloadUrl).apply();
                } catch (Exception x) {
                    Log.w(TAG, "Opps -- failed starting upgrade: " + x);
                    new Handler(Looper.getMainLooper()).post(() -> mActivity.removeLastFragment());
                }
            });
        }
    }

    @Override
    public void upgradeAssistantChangedState(CapUpgradeAssistant assistant, final CapUpgradeAssistantState state, final int progress, final Date estimate) {
        Log.d(TAG, "Changed upgrade assistant state: " + state + ", progress: " + progress + ", time estimate: " + estimate);

        new Handler(Looper.getMainLooper()).post(() -> {
            mUpdatingProgressBar.setProgress(progress);
            switch (state) {
                case Downloading:
                    mUpdatingTitleTextView.setText(mActivity.getString(R.string.downloading_update));
                    mUpdatingDescriptionTextView.setText(mActivity.getString(R.string.downloading_update_description));
                    break;
                default:
                    String earbudIndexString = mFirstEarbudUpdated ? mActivity.getString(R.string.first) : mActivity.getString(R.string.second);
                    mUpdatingTitleTextView.setText(mActivity.getString(R.string.transferring_update, earbudIndexString));
                    mUpdatingDescriptionTextView.setText(mActivity.getString(R.string.transferring_update_description));
            }

            if (estimate != null) {
                mTimeRemainingTextView.setVisibility(View.VISIBLE);
                Date currentDate = Calendar.getInstance().getTime();
                int elapsedTimeMinutes = (int) ((estimate.getTime() - currentDate.getTime()) / 60000);
                if (elapsedTimeMinutes > 0) {
                    mTimeRemainingTextView.setText(mActivity.getString(R.string.minutes_remaining, elapsedTimeMinutes));
                } else {
                    mTimeRemainingTextView.setVisibility(View.GONE);
                }
            } else {
                mTimeRemainingTextView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void upgradeAssistantFailed(CapUpgradeAssistant assistant, CapUpgradeAssistantError error, String reason) {
        Log.d(TAG, "Changed upgrade assistant FAILED: " + error + ", reason: " + reason);
        new Handler(Looper.getMainLooper()).post(() -> {
            mActivity.removeLastFragment();
            Toast.makeText(mActivity, "Upgrade failed: " + error.toString(), Toast.LENGTH_LONG).show();

        });
    }

    @Override
    public void shouldRebootAndResume(CapUpgradeAssistant assistant) {
        Log.d(TAG, "Should reboot and resume!");
        try {
            assistant.proceedRebootAndResume(0);
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mActivity, "Reboot and resume!", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Log.e(TAG, "Reboot and resume exception!");
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mActivity, "Reboot and resume FAILED!", Toast.LENGTH_SHORT).show());
            e.printStackTrace();
        }
    }

    @Override
    public void shouldCommitUpgrade(CapUpgradeAssistant assistant) {
        Log.d(TAG, "Should commit upgrade!");
        try {
            assistant.proceedAtCommit(true);
        } catch (Exception e) {
            Log.e(TAG, "Error committing upgrade! " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void shouldProceedAtTransferComplete(CapUpgradeAssistant assistant) {
        Log.d(TAG, "Should proceed at transfer complete!");
        if (((DashM2Activity) mActivity).canReboot()) {
            new Handler(Looper.getMainLooper()).post(() -> {
                mUpdatingLayout.setVisibility(View.GONE);
                mCompleteUpdateLayout.setVisibility(View.VISIBLE);
            });
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                mUpdatingLayout.setVisibility(View.GONE);
                mBatteryLowLayout.setVisibility(View.VISIBLE);
            });
        }
    }
}

package com.iamplus.earin.ui.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.iamplus.earin.R;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.BluetoothGattStatus;
import com.iamplus.earin.ui.RippleBackground;
import com.iamplus.earin.ui.activities.dash.DashM2Activity;
import com.iamplus.earin.util.bluetooth.BleBroadcastUtil;
import com.iamplus.earin.util.bluetooth.BluetoothUtil;

public class ConnectActivity extends BaseToolbarActivity implements BleBroadcastUtil.BleEventListener {

    private static final String TAG = ConnectActivity.class.getSimpleName();

    private RippleBackground mRippleBackground;
    private Button mHelpButton;
    private TextView mCenterTextView;
    private boolean mHasNavigatedAway;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_connect);
        super.onCreate(savedInstanceState);

        setToolbarTitle(getString(R.string.connect).toUpperCase());
        showToolbarLeftIcon(true);

        mRippleBackground = findViewById(R.id.rippleBackground);
        mCenterTextView = findViewById(R.id.centerTextView);
        mHelpButton = findViewById(R.id.helpButton);

        mHelpButton.setOnClickListener(view -> finish());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!mHasNavigatedAway) {
                mCenterTextView.setText(R.string.still_looking);
                mHelpButton.setVisibility(View.VISIBLE);
            }
        }, 3000);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mRippleBackground.startRippleAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRippleBackground.stopRippleAnimation();
    }

    @Override
    protected void onStart() {
        super.onStart();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (BleBroadcastUtil.getInstance(ConnectActivity.this).isConnected()) {
            onConnect();
        } else {
            Manager.getSharedManager();
            if (bluetoothAdapter.isEnabled() &&
                    !Manager.getSharedManager().getCapCommunicationController().isConnectingToDevice()) {
                Log.v(TAG, "Try to connect!");
                BluetoothUtil bluetoothUtil = BluetoothUtil.getInstance(this);

                if (bluetoothUtil.getCurrentDevice() != null) {
                    Manager manager = Manager.getSharedManager();
                    manager.getCapCommunicationController().connectToDevice(bluetoothUtil.getCurrentDevice());
                }
            } else {
                Log.v(TAG, "Trying to connect or bluetooth off");
            }
            BleBroadcastUtil.getInstance(this).addBleEventListener(this);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        BleBroadcastUtil.getInstance(this).removeBleEventListener(this);
    }

    @Override
    public void onConnect() {
        mHasNavigatedAway = true;
        Intent intent = new Intent(ConnectActivity.this, DashM2Activity.class);
        startActivity(intent);
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onDisconnect(boolean isCompletingUpgrade, BluetoothGattStatus status) {
        if (status == BluetoothGattStatus.Error) {
            //It's the dreaded 133 error! -> show a message to the User!
            mHasNavigatedAway = true;
            Intent intent = new Intent(ConnectActivity.this, ConnectionErrorActivity.class);
            startActivity(intent);
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

}

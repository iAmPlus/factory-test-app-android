package com.iamplus.earin.ui.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.iamplus.earin.R;
import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.BluetoothGattStatus;
import com.iamplus.earin.ui.activities.dash.DashM2Activity;
import com.iamplus.earin.util.bluetooth.BleBroadcastUtil;
import com.iamplus.earin.util.bluetooth.BluetoothUtil;

import java.util.ArrayList;
import java.util.List;

public class SplashScreenActivity extends AppCompatActivity implements BleBroadcastUtil.BleEventListener {

    private static final int REQUEST_CODE_ASK_LOCATION_PERMISSIONS = 1;
    private boolean mHasNavigatedAway;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash_screen);

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestMarshMallowPermissions();
        } else {
            onPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_LOCATION_PERMISSIONS: {

                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(SplashScreenActivity.this, getString(R.string.allow_permission), Toast.LENGTH_SHORT)
                            .show();
                    requestMarshMallowPermissions();
                } else {
                    onPermissionGranted();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestMarshMallowPermissions() {
        final List<String> permissionsList = new ArrayList<>();
        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                REQUEST_CODE_ASK_LOCATION_PERMISSIONS);
    }

    private void onPermissionGranted() {

        BleBroadcastUtil.getInstance(this).addBleEventListener(this);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            BluetoothUtil.getInstance(EarinApplication.getContext());
            //bluetoothAdapter.enable();
        } else {
            Manager.getSharedManager();
            BluetoothUtil.getInstance(EarinApplication.getContext())
                    .getBluetoothHeadset(EarinApplication.getContext());

        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!mHasNavigatedAway) {
                Intent intent = new Intent(SplashScreenActivity.this, HelpActivity.class);
                startActivity(intent);
                finish();
            } else {
                BleBroadcastUtil.getInstance(SplashScreenActivity.this).removeBleEventListener(SplashScreenActivity.this);
            }
        }, 3000);
    }

    @Override
    public void onConnect() {
        mHasNavigatedAway = true;
        Intent intent = new Intent(SplashScreenActivity.this, DashM2Activity.class);
        startActivity(intent);
        BleBroadcastUtil.getInstance(SplashScreenActivity.this).removeBleEventListener(SplashScreenActivity.this);
        finish();
    }

    @Override
    public void onDisconnect(boolean isCompletingUpgrade, BluetoothGattStatus status) {
        if (status == BluetoothGattStatus.Error) {
            //It's the dreaded 133 error! -> show a message to the User!
            mHasNavigatedAway = true;
            Intent intent = new Intent(SplashScreenActivity.this, ConnectionErrorActivity.class);
            startActivity(intent);
            finish();
        }
    }
}

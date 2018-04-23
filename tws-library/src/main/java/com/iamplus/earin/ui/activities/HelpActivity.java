package com.iamplus.earin.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.iamplus.earin.R;
import com.iamplus.earin.communication.cap.BluetoothGattStatus;
import com.iamplus.earin.ui.activities.dash.DashM2Activity;
import com.iamplus.earin.util.bluetooth.BleBroadcastUtil;

public class HelpActivity extends BaseToolbarActivity implements BleBroadcastUtil.BleEventListener {

    private static final int CONNECT_REQUEST_CODE = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_help);
        super.onCreate(savedInstanceState);
        Button continueButton = findViewById(R.id.continueButton);
        continueButton.setOnClickListener(view -> {
            Intent intent = new Intent(HelpActivity.this, ConnectActivity.class);
            startActivityForResult(intent, CONNECT_REQUEST_CODE);
        });

        setToolbarTitle(getString(R.string.help).toUpperCase());
        showToolbarLeftIcon(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONNECT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (BleBroadcastUtil.getInstance(HelpActivity.this).isConnected()) {
            onConnect();
        } else {
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
        Intent intent = new Intent(HelpActivity.this, DashM2Activity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDisconnect(boolean isCompletingUpgrade, BluetoothGattStatus status) {
        if (status == BluetoothGattStatus.Error) {
            //It's the dreaded 133 error! -> show a message to the User!
            Intent intent = new Intent(HelpActivity.this, ConnectionErrorActivity.class);
            startActivity(intent);
            finish();
        }
    }
}

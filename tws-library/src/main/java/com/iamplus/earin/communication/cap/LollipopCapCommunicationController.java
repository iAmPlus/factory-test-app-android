package com.iamplus.earin.communication.cap;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopCapCommunicationController extends CapCommunicationController
{
    private static final String TAG = LollipopCapCommunicationController.class.getSimpleName();

    private BluetoothLeScanner scanner;
    private ScanSettings settings;
    private CapControlScanCallback callback;

    @Override
    protected void setupBluetoothAdapter(BluetoothAdapter adapter) {

        Log.d(TAG, "Setup CapControl comm controller");

        this.scanner = adapter.getBluetoothLeScanner();

        this.settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        this.callback = new CapControlScanCallback();
    }

    @Override
    protected void startScanning(BluetoothAdapter adapter) {

        Log.d(TAG, "Start scanning");

        //Define filter(s)
        List<ScanFilter> filters = new LinkedList<ScanFilter> ();

        //Add UUID filter
        filters.add(new ScanFilter.Builder()
            .setServiceUuid(new ParcelUuid(CapUuids.CAP_SERVICE_UUID))
            .build());

        //Use the scanner to GO GO GO!
        this.scanner.startScan(filters, this.settings, this.callback);
    }

    @Override
    protected void stopScanning(BluetoothAdapter adapter) {

        Log.d(TAG, "Stop scanning");

        this.scanner.stopScan(this.callback);
    }

    //Internal class for the callback class impl.

    private class CapControlScanCallback extends ScanCallback
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            Log.d(TAG, "Scan result!");

            //Got it -- tell the super-class!
            LollipopCapCommunicationController.this.discoveredPeripheral(
                    result.getDevice(),
                    result.getScanRecord().getAdvertiseFlags(),
                    result.getRssi());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {

            //nothing...

        }

        @Override
        public void onScanFailed(int errorCode) {

            //nothing...
        }
    }
}

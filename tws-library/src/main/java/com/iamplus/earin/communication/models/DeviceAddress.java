package com.iamplus.earin.communication.models;

import android.util.Log;

public class DeviceAddress {

    private static final String TAG = DeviceAddress.class.getSimpleName();

    private String masterAddress;
    private String slaveAddress;

    private static DeviceAddress mInstance;

    public static DeviceAddress getInstance() {
        if (mInstance == null) {
            mInstance = new DeviceAddress();
        }
        return mInstance;
    }

    private DeviceAddress() {
    }

    public String getMasterAddress() {
        return masterAddress;
    }

    public void removeAddresses(boolean removeMaster, boolean removeSlave) {
        if (removeMaster) {
            masterAddress = null;
        }
        if (removeSlave) {
            slaveAddress = null;
        }
    }

    public String getSlaveAddress() {
        return slaveAddress;
    }

    public String getFormattedMasterAddress() {
        return formatAddress(masterAddress);
    }

    public String getFormattedSlaveAddress() {
        return slaveAddress != null ? formatAddress(slaveAddress) : null;
    }

    private String formatAddress(String address) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < address.length() / 2; i++) {
            stringBuilder.append((address.substring(i * 2, i * 2 + 2)).toUpperCase());
            if (i < address.length() / 2 - 1) {
                stringBuilder.append(":");
            }
        }
        return stringBuilder.toString();
    }

    public void addAddress(String addressString) {
        if (addressString.charAt(0) == '1') {
            masterAddress = addressString.substring(3, 15);
            Log.v(TAG, "Added master address: " + addressString.substring(3, 15));
        } else if (addressString.charAt(0) == '0') {
            slaveAddress = addressString.substring(3, 15);
            Log.v(TAG, "Added slave address: " + addressString.substring(3, 15));
        }
    }

}

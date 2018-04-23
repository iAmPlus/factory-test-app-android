package com.iamplus.earin.util;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;


public class FirmwareVersion {

    private static final String TAG = FirmwareVersion.class.getSimpleName();
    public static final String SLAVE = "slave";
    public static final String MASTER = "master";

    private Version csr;
    private Version nxp;

    private FirmwareVersion(Version csr, Version nxp) {
        Log.w(TAG, "Created: " + csr.toString() + " : " + nxp.toString());
        this.csr = csr;
        this.nxp = nxp;
    }

    public Version getCsr() {
        return csr;
    }

    public Version getNxp() {
        return nxp;
    }

    public boolean isGreaterThan(@NonNull FirmwareVersion version) {
        return (csr.isGreaterThan(version.getCsr()) && nxp.isGreaterOrEqualThan(version.getNxp())) ||
                (csr.isGreaterOrEqualThan(version.getCsr()) && nxp.isGreaterThan(version.getNxp()));
    }
    public boolean isGreaterOrEqualThan(@NonNull FirmwareVersion version) {
        return csr.isGreaterOrEqualThan(version.getCsr()) && nxp.isGreaterOrEqualThan(version.getNxp());
    }

    public static FirmwareVersion fromString(String complexString) throws NumberFormatException {
        Log.w(TAG, "fromString: " + complexString);
        String tempVersion = complexString.replaceAll("\\s+","");
        ArrayList<Version> versionList = new ArrayList<>();
        while (true) {
            int startIndex = tempVersion.indexOf(':');
            int endIndex = tempVersion.indexOf(',', startIndex);

            if (endIndex > 0) {
                versionList.add(
                        Version.fromString(tempVersion.substring(startIndex + 1, endIndex)));

                tempVersion = tempVersion.substring(endIndex);
            } else {
                versionList.add(
                        Version.fromString(tempVersion.substring(startIndex + 1)));
                break;
            }
        }
        if(versionList.size() != 2) {
            throw new NumberFormatException();
        }

        return new FirmwareVersion(versionList.get(0), versionList.get(1));
    }

    public interface FirmwareVersionListener {
        void onResponse(HashMap<String, FirmwareVersion> response);
        void onError(Exception exception);
    }

}

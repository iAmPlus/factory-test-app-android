package com.iamplus.earin.util;

import android.support.annotation.NonNull;

public class Version implements Comparable<Version> {
    private int major, minor, patch;
    private String versionString;

    private Version(int major, int minor, int patch, String versionString) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.versionString = versionString;
    }

    public static Version fromString(@NonNull String version) throws NumberFormatException {
        final String parts[] = version.split("\\.");
        return new Version(
                Integer.parseInt(parts[0]),
                parts.length > 1 ? Integer.parseInt(parts[1]) : 0,
                parts.length > 2 ? Integer.parseInt(parts[2]) : 0,
                version);
    }

    @Override
    public int compareTo(@NonNull Version version) {
        int result = this.major - version.major;
        if (result == 0) {
            result = this.minor - version.minor;
            if (result == 0) return this.patch - version.patch;
        }
        return result;
    }

    public boolean isGreaterOrEqualThan(@NonNull Version version) {
        return compareTo(version) >= 0;
    }

    public boolean isGreaterThan(@NonNull Version version) {
        return compareTo(version) > 0;
    }

    @Override
    public String toString() {
        return versionString;
    }
}

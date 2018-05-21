package com.iamplus.systemupdater;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Mock {

    public static boolean MOCK = false;
    public static String VERSION = "2.0.0";

    public static String UPDATE_RESULT = "updates.json";
    public static String UPDATE_FILE = "dummy-delta.zip";

    //public static String UPDATE_RESULT = "updates-corrupt.json";
    //public static String UPDATE_FILE = "dummy-delta-corrupt.zip";

    //public static String UPDATE_RESULT = "updates-signature-mismatch.json";
    //public static String UPDATE_FILE = "dummy-signature-mismatch.zip";

    public static String getVersion() {
        return VERSION;
    }

    public static String getUpdates(Context context) {
        return Utils.readFromAssets(context, UPDATE_RESULT);
    }

    public static void saveUpdateFile(Context context, File filename) throws IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            in = new BufferedInputStream(context.getAssets().open(UPDATE_FILE));
            fout = new FileOutputStream(filename);
            int downloaded_bytes = 0;

            final byte data[] = new byte[4096];
            int count;
            while ((count = in.read(data, 0, 4096)) != -1) {
                downloaded_bytes += count;
                fout.write(data, 0, count);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (fout != null) {
                fout.close();
            }
        }
    }
}
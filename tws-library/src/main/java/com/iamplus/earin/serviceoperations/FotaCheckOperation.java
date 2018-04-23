package com.iamplus.earin.serviceoperations;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.iamplus.earin.BuildConfig;
import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.util.FirmwareVersion;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

import static com.android.volley.toolbox.Volley.newRequestQueue;

public class FotaCheckOperation extends BaseServiceOperation {

    private static final String TAG = FotaCheckOperation.class.getSimpleName();

    public static void checkForFotaUpdate(final Context context, String hardwareVersion, FirmwareVersion firmwareVersion, final FotaCheckHandler fotaCheckHandler) {
        String firmwareVersionString = "csr" + firmwareVersion.getCsr().toString() + "nxp" + firmwareVersion.getNxp().toString();
        if (BuildConfig.DEBUG) {
            Toast.makeText(context, "Device firmware version: " + firmwareVersionString, Toast.LENGTH_SHORT).show();
        }

        String url = FIRMWARE_BASE_URL + "/" + hardwareVersion + "/" + firmwareVersionString;
        Log.w(TAG, "url: " + url);

        RequestQueue queue = newRequestQueue(EarinApplication.getContext());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(url, null,
                response -> {
                    if (response.has("downloadLink")) {
                        try {
                            fotaCheckHandler.onUpdateRequired(
                                    response.getString("downloadLink"),
                                    response.optString("releaseNote", ""),
                                    response.optString("csr", ""),
                                    response.optString("nxp", ""));
                        } catch (JSONException e) {
                            fotaCheckHandler.onError(0);
                        }
                    } else {
                        fotaCheckHandler.onUpdateNotRequired();
                    }
                },
                error -> {
                    error.printStackTrace();
                    if (error == null || error.networkResponse == null) {
                        fotaCheckHandler.onError(404);
                    } else {
                        fotaCheckHandler.onError(error.networkResponse.statusCode);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String credentials = FIRMWARE_AUTH_USER + ":" + FIRMWARE_AUTH_PASS;
                String auth = "Basic "
                        + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", auth);
                headers.put("appToken", FirebaseInstanceId.getInstance().getToken());
                return headers;
            }
        };
        queue.add(jsonObjectRequest);
    }


    public interface FotaCheckHandler extends BaseOperationHandler {
        void onUpdateRequired(String url, String releaseNote, String csr, String nxp);

        void onUpdateNotRequired();
    }
}

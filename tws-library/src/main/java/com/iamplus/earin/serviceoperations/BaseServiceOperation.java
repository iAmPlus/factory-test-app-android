package com.iamplus.earin.serviceoperations;

abstract class BaseServiceOperation {


    static final String FIRMWARE_BASE_URL = "https://firmware.earin.com";
    static final String FIRMWARE_AUTH_USER = "download";
    static final String FIRMWARE_AUTH_PASS = "OAd2gz2vDIqZ1P9OBaSboDdhYxDc4c80";

    interface BaseOperationHandler {
        void onError(int statusCode);
    }
}

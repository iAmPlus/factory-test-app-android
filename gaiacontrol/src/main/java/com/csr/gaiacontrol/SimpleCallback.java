package com.csr.gaiacontrol;

import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaPacket;

public class SimpleCallback implements Callback {

    @Override
    public void handleNotification(GaiaPacket packet) {

    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onGetAppVersion(String version) {

    }

    @Override
    public void onGetUUID(String uuid) {

    }

    @Override
    public void onGetSerialNumber(String sn) {

    }

    @Override
    public void onError(GaiaError error) {

    }

    @Override
    public void onGetBatteryLevel(int level) {

    }

    @Override
    public void onGetRSSILevel(int level) {

    }

    @Override
    public void onSetVoiceAssistantConfig(boolean config) {

    }

    @Override
    public void onSetSensoryConfig(boolean config) {

    }

    @Override
    public void onPacketCommandNotSupport(GaiaPacket packet) {

    }

    @Override
    public void handlePacket(GaiaPacket packet) {

    }
}

package com.qualcomm.qti.gaiacontrol;

import com.qualcomm.qti.libraries.gaia.GaiaException;
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBREDR;

public class SimpleCallback implements Callback {

    @Override
    public void handleNotification(GaiaPacketBREDR packet) {

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
    public void onError(GaiaException error) {

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
    public void onPacketCommandNotSupport(GaiaPacketBREDR packet) {

    }

    @Override
    public void handlePacket(GaiaPacketBREDR packet) {

    }
}

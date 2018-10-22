package com.csr.gaiacontrol;

import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaPacket;

public interface Callback {
    void handleNotification(GaiaPacket packet);
    void onConnected();
    void onDisconnected();
    void onGetAppVersion(String version);
    void onGetUUID(String uuid);
    void onGetSerialNumber(String sn);
    void onError(GaiaError error);
    void onGetBatteryLevel(int level);
    void onGetRSSILevel(int level);
    void onSetVoiceAssistantConfig(boolean config);
    void onSetSensoryConfig(boolean config);
    void onPacketCommandNotSupport(GaiaPacket packet);

    void handlePacket(GaiaPacket packet);
}
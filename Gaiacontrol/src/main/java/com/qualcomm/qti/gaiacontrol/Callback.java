package com.qualcomm.qti.gaiacontrol;

import com.qualcomm.qti.libraries.gaia.GaiaException;
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBREDR;

public interface Callback {
    void handleNotification(GaiaPacketBREDR packet);
    void onConnected();
    void onDisconnected();
    void onGetAppVersion(String version);
    void onGetUUID(String uuid);
    void onGetSerialNumber(String sn);
    void onError(GaiaException error);
    void onGetBatteryLevel(int level);
    void onGetRSSILevel(int level);
    void onSetVoiceAssistantConfig(boolean config);
    void onSetSensoryConfig(boolean config);
    void onPacketCommandNotSupport(GaiaPacketBREDR packet);

    void handlePacket(GaiaPacketBREDR packet);
}
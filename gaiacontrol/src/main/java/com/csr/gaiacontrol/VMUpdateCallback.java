package com.csr.gaiacontrol;

import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaPacket;

public interface VMUpdateCallback {
    void onUpdateActivated();
    void onUpdateActivatedFailed();
    void onVMDisconnected();
    void onVMControlSucceed();
    void onVMControlFailed();
    void handlerVMEvent(GaiaPacket packet);
    void onConnected();
    void onDisconnected();
    void onError(GaiaError error);
}

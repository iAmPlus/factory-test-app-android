package com.iamplus.earin.communication.cap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public interface CapCommunicationControllerDelegate {
    void deviceConnected(CapCommunicationController controller, String identifier);

    void deviceDisconnected(CapCommunicationController controller, String identifier, BluetoothGattStatus status);

    boolean permittedToEnableBluetooth(CapCommunicationController controller);

    boolean keepConnectedDevice(CapCommunicationController controller, CapCommunicator communicator) throws Exception;
}


package com.iamplus.earin.communication.cap.transports;

import com.iamplus.earin.communication.cap.BluetoothGattStatus;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public interface TransportDelegate
{
	//Methods to allow for the transport to "callback" to its delegate (controller)
	public void transportConnected(AbstractTransport transport, String identifier);
	public void transportFailedToConnected(AbstractTransport transport, String identifier, Exception exception);
	public void transportDisconnected(AbstractTransport transport, String identifier, BluetoothGattStatus status);

	public void transportReceivedResponseData(AbstractTransport transport, byte[] data);
	public void transportReceivedEventData(AbstractTransport transport, byte[] data);
	public void transportReceivedUpgradeData(AbstractTransport transport, byte[] data);
}

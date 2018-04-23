package com.iamplus.earin.communication.cap.protocols;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public interface CapProtocolEventDelegate
{
	public void receivedCapProtocolEvent(String identifyer, byte[] data, String comment);
}

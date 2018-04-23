package com.iamplus.earin.communication.cap.protocols;

import com.iamplus.earin.communication.utils.ByteBuffer;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public interface CapProtocolUpgradeDelegate
{
	public void receivedCapUpgradeCommand(CapProtocolUpgradeHostCommand command, ByteBuffer data);
}

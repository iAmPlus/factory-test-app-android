package com.iamplus.earin.communication.cap.protocols;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public interface CapProtocolDataBlockParser
{
	public int findDataBlock(byte[] data);
	public byte [] getDataBlock();
}

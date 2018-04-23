package com.iamplus.earin.communication.cap.protocols;

import com.iamplus.earin.communication.utils.ByteBuffer;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public class CapProtocolNestedDataBlockParser implements CapProtocolDataBlockParser
{
    //Privat state of block...
    private byte [] dataBlock;
	
	public int findDataBlock(byte [] data) 
	{
		//Reset...
		this.dataBlock = null;
		
//		logger.info("Finding data-block from index " + dataBlockStartIndex);
		
		//Find end of that data-block as well!
		// -- find matching end...
		int dataBlockEndIndex = -1;
		int nestCounter = 0;
		for (int seachIndex = 0; seachIndex < data.length; seachIndex++) 
		{
			//Search...
			if (data[seachIndex] == CapProtocol.DATA_BLOCK_START_CHAR)
			{
				//Opps... found a nested block...
				nestCounter ++;
			}
			else if (data[seachIndex] == CapProtocol.DATA_BLOCK_END_CHAR)
			{
				//Found "end" block char, but is this the matching one?
				if (nestCounter > 0)
				{
					//No -- this matches a nested block, let's move on...
					nestCounter --;
				}
				else
				{
					//Yes -- this is the one!
					dataBlockEndIndex = seachIndex;
					break;
				}
			}
		}
		
		//OK -- so, how did that go?
		if (dataBlockEndIndex != -1)
		{
			//Found data -- save it locally...!
			ByteBuffer buffer = new ByteBuffer();
			buffer.appendBytes(data);
			this.dataBlock = buffer.getBytes(0, dataBlockEndIndex);
		}
		
		return dataBlockEndIndex;
	}
	
	public byte [] getDataBlock()
	{
		return this.dataBlock;
	}
}

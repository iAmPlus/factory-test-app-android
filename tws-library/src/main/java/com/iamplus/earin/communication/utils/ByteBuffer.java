package com.iamplus.earin.communication.utils;

public class ByteBuffer
{
	private int capacity;
	private byte [] inputBuffer;
	private int inputBufferSize;

	public ByteBuffer()
	{
		this(2000);
	}

	public ByteBuffer(int capacity)
	{
		this.capacity = capacity;
		this.inputBuffer = new byte[this.capacity];
		this.inputBufferSize = 0;
	}

	public synchronized void appendByte(byte data) throws ArrayIndexOutOfBoundsException
	{
		appendBytes(new byte[]{data});
	}

	public synchronized void appendShort(int data, boolean bigEndian) throws ArrayIndexOutOfBoundsException
	{
		if (bigEndian)
		{
			appendBytes(new byte[]{
					(byte) ((data & 0xFF00) >> 8),
					(byte) ((data & 0x00FF))
			});
		}
		else
		{
			appendBytes(new byte[]{
					(byte) (data & 0x00FF),
					(byte) ((data & 0xFF00) >> 8)
			});
		}
	}

	public synchronized void appendInt(int data, boolean bigEndian) throws ArrayIndexOutOfBoundsException
	{
		if (bigEndian)
		{
			appendBytes(new byte[]{
					(byte) ((data & 0xFF000000) >> 24),
					(byte) ((data & 0x00FF0000) >> 16),
					(byte) ((data & 0x0000FF00) >> 8),
					(byte) ((data & 0x000000FF))
			});
		}
		else
		{
			appendBytes(new byte[]{
					(byte) ((data & 0x000000FF)),
					(byte) ((data & 0x0000FF00) >> 8),
					(byte) ((data & 0x00FF0000) >> 16),
					(byte) ((data & 0xFF000000) >> 24)
			});
		}
	}

	public synchronized void appendString(String data) throws ArrayIndexOutOfBoundsException
	{
		if (data != null)
			appendBytes(data.getBytes());
	}

	public synchronized void appendBytes(byte [] data) throws ArrayIndexOutOfBoundsException
	{
		if (data != null)
			appendBytes(data, data.length);
	}

	public synchronized void appendBytes(byte [] data, int length) throws ArrayIndexOutOfBoundsException
	{
		//Just add this to the end of the buffer...
		if (data != null)
			for (int i = 0; i < length; i ++)
				this.inputBuffer[inputBufferSize++] = data[i];
	}

	public synchronized int size()
	{
		return this.inputBufferSize;
	}

	public synchronized int indexOf(byte [] pattern)
	{
		//start search from 0
		return indexOf(0, pattern);
	}

	public synchronized int indexOf(int offset, byte [] pattern)
	{
		int sequenceIndex = 0;

		for (int i = offset; i < this.inputBufferSize; i++)
		{
			if (this.inputBuffer[i] == pattern[sequenceIndex])
			{
				//found match!
				sequenceIndex ++;

				//Done?
				if (sequenceIndex == pattern.length)

					//return start-index of sequence!
					return i - sequenceIndex + 1;
			}
			else
			{
				//NOT match!
				sequenceIndex = 0;
			}
		}

		return -1;
	}

	public synchronized void reset()
	{
		this.inputBufferSize = 0;
	}

	public synchronized int truncate(int nbrOfBytes)
	{
		//How many bytes are there?
		if (nbrOfBytes > this.inputBufferSize)
			nbrOfBytes = this.inputBufferSize;

		//Simply decrease buffer-size by as many bytes...
		this.inputBufferSize -= nbrOfBytes;

		return nbrOfBytes;
	}

	public synchronized int skip(int nbrOfBytes)
	{
		//Skip and shift
		//How many bytes are there?
		if (nbrOfBytes > this.inputBufferSize)
			nbrOfBytes = this.inputBufferSize;

		//1 or more bytes to skip?
		if (nbrOfBytes >= 1)
		{
			//Left-shift buffer as many steps...
			for (int i = nbrOfBytes; i < this.inputBufferSize; i++)
				this.inputBuffer[i - nbrOfBytes] = this.inputBuffer[i];

			this.inputBufferSize -= nbrOfBytes;
		}

		//return as many bytes taht we just skipped...
		return nbrOfBytes;
	}

	public synchronized int getAvailableBufferSize()
	{
		return this.capacity - this.inputBufferSize;
	}

	public int getCapacity()
	{
		return this.capacity;
	}

	public synchronized byte [] getAllBytes() throws IndexOutOfBoundsException
	{
		//Get all bytes...
		return getBytes(0, this.inputBufferSize);
	}

	public synchronized byte getByteAt(int index) throws IndexOutOfBoundsException
	{
		return this.inputBuffer[index];
	}

	public synchronized int getShortAt(int index, boolean bigEndian) throws IndexOutOfBoundsException
	{
		int value = 0;

		if (bigEndian)
		{
			value += ((this.inputBuffer[index + 0] & 0xff) << 8);
			value += ((this.inputBuffer[index + 1] & 0xff));
		}
		else
		{
			value += ((this.inputBuffer[index + 0] & 0xff));
			value += ((this.inputBuffer[index + 1] & 0xff) << 8);
		}

		return value;
	}

	public synchronized int getIntAt(int index, boolean bigEndian) throws IndexOutOfBoundsException
	{
		int value = 0;

		if (bigEndian)
		{
			value += ((this.inputBuffer[index + 0] & 0xff) << 24);
			value += ((this.inputBuffer[index + 1] & 0xff)<< 16);
			value += ((this.inputBuffer[index + 2] & 0xff)<< 8);
			value += ((this.inputBuffer[index + 3] & 0xff));
		}
		else
		{
			value += ((this.inputBuffer[index + 0] & 0xff));
			value += ((this.inputBuffer[index + 1] & 0xff) << 8);
			value += ((this.inputBuffer[index + 2] & 0xff) << 16);
			value += ((this.inputBuffer[index + 3] & 0xff) << 24);
		}

		return value;
	}

	public synchronized byte [] getBytes(int offset) throws IndexOutOfBoundsException
	{
		//Get all remaning bytes, starting from offset...
		return this.getBytes(offset, this.inputBufferSize - offset);
	}

	public synchronized byte [] getBytes(int offset, int nbrOfBytes) throws IndexOutOfBoundsException
	{
		//How many bytes are there?
		if (nbrOfBytes > (this.inputBufferSize - offset))
			nbrOfBytes = (this.inputBufferSize - offset);

		if (nbrOfBytes > 0)
		{
			//Allocate & transfer into clone...
			byte [] buffer = new byte[nbrOfBytes];
			for (int i = 0; i < buffer.length; i++)
				buffer[i] = this.inputBuffer[i + offset];

			return buffer;
		}
		else
			return null;
	}

	public synchronized byte [] consumeBytes(int nbrOfBytes) throws IndexOutOfBoundsException
	{
		//Can desiredNbrOfBytes be allocated?
		if (nbrOfBytes > this.inputBufferSize)
			nbrOfBytes = this.inputBufferSize;

		if (nbrOfBytes > 0)
		{
			//Allocate & transfer
			byte [] buffer = new byte[nbrOfBytes];
			for (int i = 0; i < buffer.length; i++)
				buffer[i] = this.inputBuffer[i];

			//clean up as this IS a consume op...
			skip(nbrOfBytes);

			return buffer;
		}
		else
			return null;
	}

	public String toString()
	{
		String str = "ByteBuffer of size " + this.inputBufferSize + " with contents; [";
		for (int i = 0; i < this.inputBufferSize; i++)
		{
			str += (this.inputBuffer[i] + 256) % 256;
			if (i + 1 < this.inputBufferSize)
				str += ", ";
		}

		return str;
	}
}

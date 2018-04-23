package com.iamplus.earin.communication.utils;

public class Parser
{
	private static final String TAG = Parser.class.getSimpleName();
/*
	// Charset and decoder for US-ASCII
	// private static Charset charset = Charset.forName("US-ASCII");
	private static Charset charset = Charset.forName("ISO-8859-1");
	private static CharsetDecoder decoder = charset.newDecoder();
	
	public static short [] sliceShortArray(short[] original, int offset, int length)
	{
		//Create new array...
		short [] slice = new short[length];
		
		//Copy...
		for (int index = 0; index < length; index++)
			slice[index] = original[offset + index];
		
		//Return...
		return slice;
	}
	
	public static byte [] sliceByteArray(byte[] original, int offset, int length)
	{
		//Create new array...
		byte [] slice = new byte[length];
		
		//Copy...
		for (int index = 0; index < length; index++)
			slice[index] = original[offset + index];
		
		//Return...
		return slice;
	}

	public static String generateValueString(int value, int desiredLength, char fillChar)
	{
		String string = "" + value;
		
		while (string.length() < desiredLength)
			string = fillChar + string;
			
		return string;
	}
	
	public static String generateTimeString(long millis)
	{
		String timeString = "";

		//Start with the largest quantity...
		
		//-- hours...
		int hours = (int)Math.floor(millis / (60 * 60 * 1000));  
		if (hours > 0)
		{
			timeString += Parser.generateValueString(hours, 2, '0') + ":";
			millis -= hours * (60 * 60 * 1000);
		}
		
		//-- minutes...
		int minutes = (int)Math.floor(millis / (60 * 1000));  
		timeString += Parser.generateValueString(minutes, 2, '0') + ":";
		millis -= minutes * (60 * 1000);
		
		//-- seconds...
		int seconds = (int)Math.floor(millis / (1000));  
		timeString += Parser.generateValueString(seconds, 2, '0');
		millis -= seconds * (1000);
		
		return timeString;
	}
	
	public static String generateByteString(long bytes, int nbrOfDecimals)
	{
		String [] suffixes = new String[]{"B", "kB", "MB", "GB"};
		
		//Ok... How big is this, really?
		int suffixIndex = suffixes.length - 1;
		
		while (suffixIndex >= 0)
		{
			//Check suffix...
			long weight = (long)Math.pow(1000, suffixIndex);
			double result = (double)bytes / weight;
			if (result >= 1.0)
			{
				//Ok -- found a "dividable" suffix!
				//-- check decimals...
				int value = (int)Math.floor(result);
				int decimals = (int)Math.floor((result - value) * Math.pow(10, nbrOfDecimals));
				
				//The main-piece of the pie.
				String output = "" + value;
				
				//Any decimals left to append?
				if (decimals > 0)
					output += "." + decimals;
					
				//and the suffix...
				output += suffixes[suffixIndex];
					
				return output;  				
			} 
			
			suffixIndex--;
		}
		
		return "N/A";
	}

	public static byte[] convertShortArrayToByteArray(short[] indata) throws IOException
	{
		return convertShortArrayToByteArray(indata, indata.length);
	}

	public static byte[] convertShortArrayToByteArray(short[] indata, int length) throws IOException
	{
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		DataOutputStream outputStream = new DataOutputStream(byteOutputStream);

		for (int i = 0; i < length; i++)
			outputStream.writeShort(indata[i]);

		return byteOutputStream.toByteArray();
	}
	
	public static short[] convertByteArrayToShortArray(byte[] indata) throws Exception
	{
		return convertByteArrayToShortArray(indata, indata.length);
	}

	public static short[] convertByteArrayToShortArray(byte[] indata, int length) throws Exception
	{
		//OK -- byte array must be an even number size...
		if ((length % 2) == 1)
			throw new Exception("Cannot form short-arraywith odd number of bytes as in data");
			
		short [] outdata = new short[length / 2];
		
		//Transfer bytes, even bytes beciomes high.bytes, and odd bytes low-bytes...
		for (int i = 0; i < outdata.length; i++)
		{
			//Reset short... 
			outdata[i] = 0x0000;
			
			//High byte...
			outdata[i] += indata[(i * 2)] << 8;

			//Low byte...
			outdata[i] += indata[(i * 2) + 1];
		}

		return outdata;
	}

	public static byte [] translateStringIntoByteArray(String data, String pattern, byte fillerByte)
	{
		//Follow pattern and create byte contents into buffer where string has data, else go for filler for complete pattern...
		ByteBuffer buffer = new ByteBuffer();
		
//		logger.info("Parsing pattern: " + pattern);
		
		//Cycle through pattern...
		for (int k = 0; k < pattern.length(); k++)
		{
//			logger.info("Parsing part: " + pattern.charAt(k));
			
			//Before we proceed, do we have more data to parse?
			if (data != null && data.length() > 0)
			{
				//Yes -- more data to parse...
				
				// How should this pattern char be enterpreted?
				switch (pattern.charAt(k))
				{
					// Enterpret as byte hex
					case 'x':
					{
						//Get two chars from data string and turn into a byte
						String hexString = data.substring(0, 2);
						data = data.substring(2);
//						logger.info("Extracted hexString: " + hexString);
						buffer.append(new byte[]{convertHexStringToByte(hexString)});
						break;
					}
		
					// Enterpret as character
					case 'c':
					{
						// Write to output as an ASCII character
						String charString = data.substring(0, 1);
						data = data.substring(1);
//						logger.info("Extracted charString: " + charString);
						buffer.append(data.getBytes());
						break;
					}
		
					// Or else, by default treat things as "fillers" and
					// just add the pattern byte to the output as a character
					default:
					{
						//OK -- this is just a pattern "filler" char in there, 
						//so let's compare with what filler we actually have...?
						if (data.charAt(0) == pattern.charAt(k))
						{
							//OK -- nice, this is as expected...
//							logger.debug("Pattern filler char match data -- nice...");
							
							//-- consume char in data...
							data = data.substring(1);
						}
						else
						{
							//WTF! Different char found, compared to teh expected filler...
							Log.e(TAG, "Pattern filler char '" + pattern.charAt(k) + "' does NOT match data char '" + data.charAt(0) + "'");
						}
						break;
					}
				}
			}
			else
			{
				//Nope -- no more data there...
				//let's use filler instead...
//				logger.debug("Out of data -- using filler... ");
				buffer.append(new byte[]{fillerByte});
			}
		}
		
//		logger.info("Complete buffer: " + Arrays.toString(buffer.getAllBytes()));

		return buffer.getAllBytes();
	}

	public static String translateByteArrayIntoString(byte[] data, String pattern, String visible)
	{
		StringBuffer buffer = new StringBuffer();

		// Index for the original data array...
		int i = 0;

		// Index for the output buffer data array...
		int j = 0;

		// Transform data according to pattern --> buffer
		int [] dataStringLengths = new int[data.length];
		for (int k = 0; k < pattern.length(); k++)
		{
			// How should this pattern-byte be enterpreted?
			String dataString = "";
			boolean increaseOrignal = false; 
			
			switch (pattern.charAt(k))
			{
				// Enterpret as byte hex
				case 'x':
				{
					// Write to output as 2 bytes hex representation
					dataString = convertByteToHexString(data[i]);
					increaseOrignal = true;
					break;
				}
				
				// Enterpret as byte digit
				case 'd':
				{
					// Write to output as digit representation
					dataString = convertByteToDecString(data[i]);
					increaseOrignal = true;
					break;
				}
	
					// Enterpret as character
				case 'c':
				{
					// Write to output as an ASCII character
					dataString = "" + (char)data[i];
					increaseOrignal = true;
					break;
				}
	
					// Or else, by default treat things as "fillers" and
					// just add the pattern byte to the output as a character
				default:
				{
					// Write pattern-byte to output as an ASCII character
					dataString = "" + pattern.charAt(k);
					break;
				}
			}
			
			buffer.append(dataString);

			// increase output indexes correspondingly
			int dataStringLength = dataString.length();
			j += dataStringLength;
			
			//Was this something that should make progress in orginal pattern?
			if (increaseOrignal)
			{
				//Ok, this was related to the core data -- how long did the string get? 
				dataStringLengths[i] = dataStringLength;
				
				//Increase data index...
				i += 1;
			}
		}

		// Go right-to-left and hide "d"-bytes that are not required to be
		// visible and that doesn't contain anything anyhow (=0x00)
		
		// Setup indexes...
		i = data.length - 1; // the last "real" data index		
		boolean found = false;
		
		for (int k = pattern.length() - 1; k >= 0 && !found; k--)
		{
			// Subject to visibility check?
			if (visible.charAt(k) == '-')
			{
				// What kind of pattern is it -- how should we entepret it's
				// visibilty?
				switch (pattern.charAt(k))
				{
					// Is this "real" data, or just fillers and rubbish...
					case 'x':
					case 'd':
					{
						//How big was the contribution of this "real" data?
						int dataStringLength = dataStringLengths[i];
						
						// In that space, is there anything BUT 0's?
						for (int l = 0; l < dataStringLength; l++)
							if (buffer.charAt(j - l - 1) != '0')
								found = true;
						
						//foudn anything BUT '0'?`
						if (!found)
						{
							//Nope -- nothin found...
							//-- hide it!
							j -= dataStringLength;
						}
						
						//Decrease origianl data-index...
						i --;
	
						break;
					}
					
					// A character?...
					case 'c':
					{
						// Always remove ASCII character...
						j -= 1;
						
						//Decrease origianl data-index...
						i --;			
						
						break;
					}
						
					// Everything else... (fillers...)
					default:
					{
						// Always remove...
						j -= 1;
						break;
					}
				}
			}
			else
			{
				// From now on - everything is visible...
				break;
			}
		}

		// Truncate buffer at position j
		buffer.setLength(j);

		return buffer.toString();
	}

	public static String convertByteToHexString(byte data)
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		PrintStream hexStream = new PrintStream(bytes);
		hexStream.printf("%02x", data);

		String hexString = "";
		try
		{
			ByteBuffer buffer = ByteBuffer.wrap(bytes.toByteArray());
			hexString = decoder.decode(buffer).toString();
		}
		catch (Exception x)
		{
			// Opps...
			hexString = "00";
		}

		return hexString;
	}
	
	public static String convertByteToDecString(byte data)
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		PrintStream hexStream = new PrintStream(bytes);
		hexStream.printf("%d", data);

		String decString = "";
		try
		{
			ByteBuffer buffer = ByteBuffer.wrap(bytes.toByteArray());
			decString = decoder.decode(buffer).toString();
		}
		catch (Exception x)
		{
			// Opps...
			decString = "0";
		}

		return decString;
	}
	
	public static String convertShortToHexString(short data)
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		PrintStream hexStream = new PrintStream(bytes);
		hexStream.printf("%04x", data);

		String hexString = "";
		try
		{
			ByteBuffer buffer = ByteBuffer.wrap(bytes.toByteArray());
			hexString = decoder.decode(buffer).toString();
		}
		catch (Exception x)
		{
			// Opps...
			hexString = "0000";
		}

		return hexString;
	}
	
	public static String convertIntToHexString(int data)
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		PrintStream hexStream = new PrintStream(bytes);
		hexStream.printf("%08x", data);

		String hexString = "";
		try
		{
			ByteBuffer buffer = ByteBuffer.wrap(bytes.toByteArray());
			hexString = decoder.decode(buffer).toString();
		}
		catch (Exception x)
		{
			// Opps...
			hexString = "00000000";
		}

		return hexString;
	}

	public static byte convertHexStringToByte(String buffer)
	{
		byte value = (byte)(convertHexStringToInt(buffer) & 0xff);
//		System.out.println("byte-convertion: " + buffer + " --> " + value);
		return value;
	}

	public static short convertHexStringToShort(String buffer)
	{
		short value = (short)(convertHexStringToInt(buffer) & 0xffff);
//		System.out.println("short-convertion: " + buffer + " --> " + value);
		return value;
	}
	
	public static int convertHexStringToInt(String buffer)
	{
//		System.out.println("Converting string " + buffer + " into integer...");
		
		//Strip any "0x" at the beginning...
		if (buffer.startsWith("0x"))
			buffer = buffer.substring(2);
		
//		System.out.println("removed prefix: " + buffer);
		
		int value = 0;
		int shift = 0;
		
		//Cycle through it all... right --> left...
		for (int index = buffer.length() - 1; index >= 0; index--)
		{
			byte partValue = Byte.parseByte("" + buffer.charAt(index),16); 
			value |= partValue << shift;
			shift += 4;
		}
		
//		System.out.println("int-convertion: " + buffer + " --> " + value);
		
		return value;
	}
	
	public static byte [] convertStringToByteArray(String str)
	{
		byte [] bytes = new byte[str.length()];
		int index = 0;
		
		for (char c : str.toCharArray())
			bytes[index++] = (byte)c;
		
		return bytes;
	}
	
	public static String extractSoftwareVersionString(short[] data) throws Exception
	{		
		// Convert into bytes...
		byte[] bytes = Parser.convertShortArrayToByteArray(data);
		return extractSoftwareVersionString(bytes);
	}

	public static String extractSoftwareVersionString(byte[] data) throws Exception
	{
		try
		{
			// Generate "readable" software version string
			// d.d.d.d
			// +++----
			// 0101 0000 --> 1.01
			String output = Parser.translateByteArrayIntoString(data, "x.x.x.x", "+++----");
			
			//remove any initial 0's...
			while (output.length() > 1 && output.startsWith("0"))
				output = output.substring(1);
			
			//Any whole digits left?
			if (output.startsWith("."))
				output = "0" + output; //Add a 0 to make it look good...
			
			return output;
		}
		catch (Exception x)
		{
			// Opppss... The short array must be bad...
			throw new Exception("Error parsing extracted data");
		}
	}
	
	public static byte [] extractSoftwareVersionBytes(String string) throws Exception
	{
		// Generate byte-array from "readable" swVersion string
		byte [] bytes = Parser.translateStringIntoByteArray(string, "x.x.x.x", (byte)0x00);
		
		//TODO: Trim 0x00 at the start of this one, IFF not the second byte is 0x00...? 
		return bytes;
	}

	public static String extractHardwareVersionString(short[] data) throws Exception
	{
		// Convert to bytes...
		byte [] bytes = Parser.convertShortArrayToByteArray(data);
		return extractHardwareVersionString(bytes);
	}

	public static String extractHardwareVersionString(byte[] data) throws Exception
	{
		//First of all, check if we only have 0x00 in the data... If that's the case, we have no HW-info, and should return null!
		int contentsDetectorIndex;
		for (contentsDetectorIndex = 0; contentsDetectorIndex < data.length; contentsDetectorIndex++)
			if (data[contentsDetectorIndex] != 0x00)
				break;
		
		//So -- if we get here and have counted til the end... we have no contents...
		if (contentsDetectorIndex >= data.length)
		{
			//No contents -- return null! 
			return null;
		}
		else
		{
			try
			{
				// Generate "readable" hwVersion string via the "byte translator"
				// dd.d.dcd
				// ++++++--
				// 1000 1204 2d01 --> 1000.12.04-01
				return Parser.translateByteArrayIntoString(data, "xx.x.xcx", "++++++--");
			}
			catch (Exception x)
			{
				// Opppss... The short array must be bad...
				throw new Exception("Error parsing extracted data");
			}
		}
	}
	
	public static byte [] extractHardwareVersionBytes(String string) throws Exception
	{
		// Generate byte-array from "readable" hwVersion string 
		// dd.d.dcd
		// ++++++--
		// 1000 1204 2d01 --> 1000.12.04-01
		return Parser.translateStringIntoByteArray(string, "xx.x.xcx", (byte)0x00);
	}

	public static String extractArticleString(short[] data) throws Exception
	{
		try
		{
			// Convert bytes...
			byte[] bytes = Parser.convertShortArrayToByteArray(data);
			
			// Create string-representation
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			return decoder.decode(buffer).toString();
		}
		catch (IOException x)
		{
			// Opppss... The short array must be bad...
			throw new Exception("Error parsing extracted data");
		}
	}

	public static String extractBluetoothAddress(byte[] data)
	{
		StringBuffer output = new StringBuffer();

		for (int i = 0; i < data.length; i++)
		{
			output.append(convertByteToHexString(data[i]));

			if (i < (data.length - 1))
				output.append(":");
		}

		return output.toString();
	}
	
	public static byte[] extractBluetoothAddress(String address)
	{
		byte [] data = null;
		
		if (address != null && address.length() > 0)
		{	
			//Split address into ':'
			StringTokenizer tokenizer = new StringTokenizer(address, ":");
			
			if (tokenizer.countTokens() == 6)
			{
				data = new byte[6];
				data[0] = Parser.convertHexStringToByte(tokenizer.nextToken());
				data[1] = Parser.convertHexStringToByte(tokenizer.nextToken());
				data[2] = Parser.convertHexStringToByte(tokenizer.nextToken());
				data[3] = Parser.convertHexStringToByte(tokenizer.nextToken());
				data[4] = Parser.convertHexStringToByte(tokenizer.nextToken());
				data[5] = Parser.convertHexStringToByte(tokenizer.nextToken());
			}
			else
			{
				//Not valid address...
			}
		}
		
		return data;
	}
	
	public static String convertString(short[] data) throws Exception
	{
		return convertString(data, ByteOrder.BIG_ENDIAN);
	}

	public static String convertString(short[] data, ByteOrder order) throws Exception
	{
		try
		{
			// Create string-representation
			ByteBuffer buffer = ByteBuffer.wrap(new byte[data.length * 2]);

			//Set byte order...
			buffer.order(order);

			//Add data...
			buffer.asShortBuffer().put(data);
						
			//Decode...
			return decoder.decode(buffer).toString();
		}
		catch (IOException x)
		{
			// Opppss... The short array must be bad...
			throw new Exception("Error parsing extracted data");
		}
	}

	public static short [] convertString(String data) throws Exception
	{
		return convertString(data, ByteOrder.BIG_ENDIAN);
	}

	public static short [] convertString(String data, ByteOrder order)
	{
		// Create byte-representation and 
		//MAKE SURE that is is an even number!!!
		int dataLength = data.length();
		
		if (dataLength % 2 != 0)
			dataLength += 1;

		ByteBuffer buffer = ByteBuffer.allocate(dataLength);

		//Set byte order...
		buffer.order(order);

		//Set data to buffer
		buffer.put(data.getBytes());
		buffer.rewind();
		
		short [] shorts = new short[buffer.capacity() / 2];
		
		int i = 0;
		while (buffer.hasRemaining())
			shorts[i++] = buffer.getShort();

		return shorts;
	}
	
	public static boolean containsPattern(byte [] needle, byte [] haystack)
	{
		boolean match = false;

		//Check received data with reference packet to find ANY occurance of that pattern in the received data...
		int haystackIndex = 0;
		while (haystackIndex < haystack.length)
		{
			//Always assume that we DO have a match...
			match = true;
			
			//Start check with reference BCSP packet...
			for (int needleIndex = 0; needleIndex < needle.length && haystackIndex < haystack.length; needleIndex ++) 
			{
				//Compare ...
				if (needle[needleIndex] == haystack[haystackIndex])
				{
					//Increase received index to continue comparison...
					haystackIndex ++;
				}
				else
				{
					//No match - abort testloop with reference packet and start all over again...
					match = false;
					break;
				}
			}
			
			//-- check loop ended... still considered as a match?
			if (match)
				break; //-- break the while-loop - we've found our match...
			else
				haystackIndex ++;
		}
		
		return match;
	}
	*/
}

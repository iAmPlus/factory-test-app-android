package com.iamplus.earin.communication.cap.protocols;

public enum CapProtocolRequestState
{
	IDLE		(0, "Idle..."),
	PENDING		(1, "Pending..."),
	TIMED_OUT	(2, "Timed out!"),
	FAILED		(3, "Command failed"),
	CONFIRMED	(4, "Command confirmed!");
	
	private int index;
	private String description;
	
	CapProtocolRequestState(int index, String description)
	{
		this.index = index;
		this.description = description;
	}
	
	public int index(){return index;}
	public String toString(){return description;}
	
	public static CapProtocolRequestState getType(int index)
	{
    	for (CapProtocolRequestState type : values())
    		if (type.index() == index)
    			return type;
    	
    	//Default -- if nothing else was found...
    	return null;
	}
}

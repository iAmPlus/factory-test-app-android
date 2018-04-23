package com.iamplus.earin.communication.cap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public enum CapUpgradeResponse
{
    Confirmation    ((byte)0x00),
    Data            ((byte)0x01),

    Unknown            ((byte)0xFF);

    private byte code;
    CapUpgradeResponse(byte code)
    {
        this.code = code;
    }

    public byte code(){return this.code;}

    public static CapUpgradeResponse getEnumValue(byte code)
    {
        for (CapUpgradeResponse enumValue : values())
            if (enumValue.code() == code)
                return enumValue;

        return Unknown;
    }
}



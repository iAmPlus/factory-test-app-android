package com.iamplus.earin.communication.cap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public enum CapUpgradeStart
{
    Success          ((byte)0x00),
    ErrorAppNotReady ((byte)0x09),

    Unknown ((byte)0xFF);

    private byte code;
    CapUpgradeStart(byte code)
    {
        this.code = code;
    }

    public byte code(){return this.code;}

    public static CapUpgradeStart getEnumValue(byte code)
    {
        for (CapUpgradeStart enumValue : values())
            if (enumValue.code() == code)
                return enumValue;

        return Unknown;
    }
}

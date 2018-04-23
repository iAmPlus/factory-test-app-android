package com.iamplus.earin.communication.cap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public enum CapUpgradeStatus
{
    Success                    ((byte)0x00),
    UnexpectedError            ((byte)0x01),
    AlreadyConnectedWarning    ((byte)0x02),
    InProgress                 ((byte)0x03),
    Busy                       ((byte)0x04),
    InvalidPowerState          ((byte)0x05),
    InvalidCRC                 ((byte)0x06),

    Unknown                    ((byte)0xFF);

    private byte code;
    CapUpgradeStatus(byte code)
    {
        this.code = code;
    }

    public byte code(){return this.code;}

    public static CapUpgradeStatus getEnumValue(byte code)
    {
        for (CapUpgradeStatus enumValue : values())
            if (enumValue.code() == code)
                return enumValue;

        return Unknown;
    }
}

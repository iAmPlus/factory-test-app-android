package com.iamplus.earin.communication.cap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public enum CapUpgradeResumePoint
{
    Start               ((byte)0x00),
    PreValidate         ((byte)0x01),
    PreReboot           ((byte)0x02),

    // Resume after the reboot
    PostReboot          ((byte)0x03),
    Commit              ((byte)0x04),

    // Final stage of an upgrade, partition erase still required
    Erase               ((byte)0x05),

    // Resume in error handling, before reset unhandled error message have been sent
    Error               ((byte)0x06),

    Unknown             ((byte)0xFF);

    private byte code;
    CapUpgradeResumePoint(byte code)
    {
        this.code = code;
    }

    public byte code(){return this.code;}

    public static CapUpgradeResumePoint getEnumValue(byte code)
    {
        for (CapUpgradeResumePoint enumValue : values())
            if (enumValue.code() == code)
                return enumValue;

        return Unknown;
    }
}

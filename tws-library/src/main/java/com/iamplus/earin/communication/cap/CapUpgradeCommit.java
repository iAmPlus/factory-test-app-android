package com.iamplus.earin.communication.cap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public enum CapUpgradeCommit
{
    Continue   ((byte)0x00),
    Abort      ((byte)0x01);

    private byte code;
    CapUpgradeCommit(byte code)
    {
        this.code = code;
    }

    public byte code(){return this.code;}


}



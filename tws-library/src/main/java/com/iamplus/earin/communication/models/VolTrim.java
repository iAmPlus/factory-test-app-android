package com.iamplus.earin.communication.models;

public class VolTrim
{
    private int master;
    private int slave;

    public VolTrim(int master, int slave)
    {
        this.master = master;
        this.slave = slave;
    }

    public int getMaster() { return this.master; }
    public int getSlave() { return this.slave; }
}

package com.iamplus.earin.communication.models;

public class Bounds
{
    private int min;
    private int max;

    public Bounds(int min, int max)
    {
        this.min = min;
        this.max = max;
    }

    public int getMinValue() { return this.min; }
    public int getMax() { return this.max; }
}

package com.iamplus.earin.communication.models;

import android.os.Parcel;
import android.os.Parcelable;

public class BatteryReading implements Parcelable
{
    private boolean isLocal;
    private int milliVolts;
    private int percentage;

    public BatteryReading(String capDataString)
    {
        //Setup conf object with nice contents...
        String [] parts = new String(capDataString).split(",");

        this.isLocal = Integer.parseInt(parts[0]) == 1;
        this.milliVolts = Integer.parseInt(parts[1]);
        this.percentage = Integer.parseInt(parts[2]);
    }

    public BatteryReading(boolean isLocal, int milliVolts, int percentage)
    {
        this.isLocal = isLocal;
        this.milliVolts = milliVolts;
        this.percentage = percentage;
    }

    public BatteryReading(Parcel in)
    {
        readFromParcel(in);
    }

    public boolean isLocal() { return this.isLocal; }
    public int getMilliVolts() { return this.milliVolts; }
    public int getPercentage() { return this.percentage; }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(this.isLocal ? 1 : 0);
        dest.writeInt(this.milliVolts);
        dest.writeInt(this.percentage);
    }

    private void readFromParcel(Parcel in)
    {
        this.isLocal = in.readInt() == 1;
        this.milliVolts = in.readInt();
        this.percentage = in.readInt();
    }

    public static final Creator<BatteryReading> CREATOR = new Creator<BatteryReading>()
    {
        @Override
        public BatteryReading createFromParcel(Parcel in)
        {
            return new BatteryReading(in);
        }

        @Override
        public BatteryReading[] newArray(int size)
        {
            return new BatteryReading[size];
        }
    };
}

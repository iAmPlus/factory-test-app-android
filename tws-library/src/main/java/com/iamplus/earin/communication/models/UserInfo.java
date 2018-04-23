package com.iamplus.earin.communication.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Linus on 2015-09-29.
 */
public class UserInfo implements Parcelable
{
/*    private int balanceLeft;
    private int balanceRight;
    private int balanceBoundMax;
    private int balanceBoundMin;
    private boolean bassBoost;
    private boolean peerConnected;
    private String peerAddress;
    private boolean isLeft;
    private String mobileLinkRole;
    private String peerLinkRole;
*/
    public UserInfo()
    {

    }

    public UserInfo(Parcel in)
    {
        readFromParcel(in);
    }
/*
    public void setBalanceLeft(int value) {this.balanceLeft = value;}
    public int getBalanceLeft() {return this.balanceLeft;}
    public void setBalanceRight(int value) {this.balanceRight = value;}
    public int getBalanceRight() {return this.balanceRight;}
    public void setBalanceBoundMax(int value) {this.balanceBoundMax = value;}
    public int getBalanceBoundMax() {return this.balanceBoundMax;}
    public void setBalanceBoundMin(int value) {this.balanceBoundMin = value;}
    public int getBalanceBoundMin() {return this.balanceBoundMin;}
    public void setIsBassBoost(boolean value) {this.bassBoost = value;}
    public boolean getIsBassBoost() {return this.bassBoost;}
    public void setIsPeerConnected(boolean value) {this.peerConnected = value;}
    public boolean getIsPeerConnected() {return this.peerConnected;}
    public void setPeerAddress(String value) {this.peerAddress = value;}
    public String getPeerAddress() {return this.peerAddress;}
    public void setIsLeft(boolean value) {this.isLeft = value;}
    public boolean getIsLeft() {return this.isLeft;}
    public void setMobileLinkRole(String value) {this.mobileLinkRole = value;}
    public String getMobileLinkRole() {return this.mobileLinkRole;}
    public void setPeerLinkRole(String value) {this.peerLinkRole = value;}
    public String getPeerLinkRole() {return this.peerLinkRole;}
*/
    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
/*        dest.writeInt(this.balanceLeft);
        dest.writeInt(this.balanceRight);
        dest.writeInt(this.balanceBoundMax);
        dest.writeInt(this.balanceBoundMin);
        dest.writeInt(this.bassBoost ? 1 : 0);
        dest.writeInt(this.peerConnected ? 1 : 0);
        dest.writeString(this.peerAddress);
        dest.writeInt(this.isLeft ? 1 : 0);
        dest.writeString(this.mobileLinkRole);
        dest.writeString(this.peerLinkRole);*/
    }

    private void readFromParcel(Parcel in)
    {
/*        this.balanceLeft = in.readInt();
        this.balanceRight = in.readInt();
        this.balanceBoundMax = in.readInt();
        this.balanceBoundMin = in.readInt();
        this.bassBoost = in.readInt() == 1;
        this.peerConnected = in.readInt() == 1;
        this.peerAddress = in.readString();
        this.isLeft = in.readInt() == 1;
        this.mobileLinkRole = in.readString();
        this.peerLinkRole = in.readString();
        */
    }

    public static final Creator<UserInfo> CREATOR = new Creator<UserInfo>()
    {
        @Override
        public UserInfo createFromParcel(Parcel in)
        {
            return new UserInfo(in);
        }

        @Override
        public UserInfo[] newArray(int size)
        {
            return new UserInfo[size];
        }
    };
}

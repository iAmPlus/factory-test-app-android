package com.iamplus.earin.communication.cap.transports;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.iamplus.earin.communication.cap.BluetoothGattStatus;

import java.util.Arrays;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public abstract class AbstractTransport
{
    private static final String TAG = AbstractTransport.class.getSimpleName();

    private TransportDelegate delegate;
    private String identifier;

    public AbstractTransport(String identifier)
    {
        this.delegate = null;
        this.identifier = identifier;
    }

    public String getIdentifier(){return this.identifier;}

    public void setDelegate(TransportDelegate delegate) {this.delegate = delegate;}

    protected void didReceiveResponseData(byte [] data)
    {
        Log.d(TAG, "Transport did receive response data; " + Arrays.toString(data));

        //Forward to delegate -- if any
        if (this.delegate != null)
            this.delegate.transportReceivedResponseData(this, data);
    }

    protected void didReceiveEventData(byte [] data)
    {
        Log.d(TAG, "Transport did receive event data; " + Arrays.toString(data));

        //Forward to delegate -- if any
        if (this.delegate != null)
            this.delegate.transportReceivedEventData(this, data);
    }

    protected void didReceiveUpgradeData(byte [] data)
    {
        Log.d(TAG, "Transport did receive upgrade data; " + Arrays.toString(data));

        //Forward to delegate -- if any
        if (this.delegate != null)
            this.delegate.transportReceivedUpgradeData(this, data);
    }

    protected void didSucceedWithConnection()
    {
        //Forward to delegate -- if any
        if (this.delegate != null)
            this.delegate.transportConnected(this, this.identifier);
    }

    protected void didFailWithConnection(Exception x)
    {
        //Forward to delegate -- if any
        if (this.delegate != null)
            this.delegate.transportFailedToConnected(this, this.identifier, x);
    }

    protected void didDisconnect(BluetoothGattStatus status)
    {
        //Forward to delegate -- if any
        if (this.delegate != null)
            this.delegate.transportDisconnected(this, this.identifier, status);
    }

    public abstract void connect(BluetoothDevice device, long timeoutSeconds) throws Exception;
    public abstract void writeRequestData(byte [] data) throws Exception;
    public abstract void writeUpgradeData(byte [] data) throws Exception;
    public abstract void cleanup() throws Exception;
}

package com.iamplus.earin.communication.cap.protocols;

import com.iamplus.earin.communication.cap.CapUpgradeHostStatus;

/**
 * Created by markus on 2017-05-22.
 */

public class CapProtocolUpgradeHostStatusException extends Exception
{
    private CapUpgradeHostStatus hostStatus;

    public CapProtocolUpgradeHostStatusException(CapUpgradeHostStatus hostStatus, String details)
    {
        super(details);

        this.hostStatus = hostStatus;
    }

    public CapUpgradeHostStatus getHostCommand(){return hostStatus;}
}

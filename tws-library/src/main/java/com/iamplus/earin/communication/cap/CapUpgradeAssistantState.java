package com.iamplus.earin.communication.cap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public enum CapUpgradeAssistantState
{
    Idle, //
    Downloading,
    Connecting,
    Disconnecting,
    Starting,
    Transferring,
    Aborting,
    Rebooting,
    Finishing,
    Blocking,
    Failed,
    Complete,
    Aborted
}



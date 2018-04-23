package com.iamplus.earin.communication.cap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public enum CapCommunicatorEvent
{
    Ready                       ("READY"),
    UnknownCommand              ("UNKNOWN COMMAND"),

    //State tracker events
    SinkStateChanged            ("SINK STATE CHANGED"),
    ChargerStateChanged         ("CHARGER STATE CHANGED"),
    BatteryReading              ("BATTERY READING"),

    //Audio events
    AudioEnhacementChanged      ("AUDIO ENHANCEMENT CHANGED"),

    //A2DP events
    StreamCodecConfigured       ("STREAM CODEC CONFIGURED"),

    //Upgrade events
    UpgradeConnected            ("UPGRADE CONNECTED"),
    UpgradeConnectionFailed     ("UPGRADE CONNECT FAILED"),
    UpgradeResponse             ("UPGRADE RESPONSE"),
    UpgradeDisconnected         ("UPGRADE DISCONNECTED"),
    UpgradeApplyIndication      ("UPGRADE APPLY IND"),
    UpgradeBlocking             ("UPGRADE BLOCKING"),
    UpgradeBlockingDone         ("UPGRADE BLOCKING DONE"),

    //
    BleStateChanged             ("BLE STATE CHANGED"),
    BleEvent                    ("BLE EVENT"),

    NfmiDisconnected            ("NFMI DISCONNECTED"),
    NfmiConnected               ("NFMI CONNECTED"),
    
    MacAddress                  ("MAC ADDR"),

    Debug                       ("DEBUG"),

    //Iamplus events
    OmegaCall                       ("OMEGA ONE"),

    Unknown                     ("");

    private String identifyer;
    CapCommunicatorEvent(String identifyer)
    {
        this.identifyer = identifyer;
    }

    public String identifier(){return this.identifyer;}
    public String string(){return this.name();}

    public static CapCommunicatorEvent getEvent(String identifier)
    {
        for (CapCommunicatorEvent event : values())
            if (event.identifier().equalsIgnoreCase(identifier))
                return event;

        return Unknown;
    }
}

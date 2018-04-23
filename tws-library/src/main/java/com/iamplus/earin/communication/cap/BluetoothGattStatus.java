package com.iamplus.earin.communication.cap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public enum BluetoothGattStatus
{
    Success                     (0x0000),
    InvalidHandle               (0x0001),
    ReadNotPermitted            (0x0002),
    WriteNotPermitted           (0x0003),
    InvalidPdu                  (0x0004),
    InsufficientAuthentication  (0x0005),
    RequestNotSupported         (0x0006),
    InvalidOffset               (0x0007),
    InsufficientAuthorization   (0x0008),
    PrepareQueueFull            (0x0009),
    NotFound                    (0x000a),
    NotLong                     (0x000b),
    InsufficientKeySize         (0x000c),
    InvalidAttributeLength      (0x000d),
    ErrorUnlikely               (0x000e),
    InsufficientEncryption      (0x000f),
    UnsupportedGroupType        (0x0010),
    InsufficientResource        (0x0011),
    IllegalParameter            (0x0087),
    NoResources                 (0x0080),
    InternalError               (0x0081),
    WrongState                  (0x0082),
    DbFull                      (0x0083),
    Busy                        (0x0084),
    Error                       (0x0085),
    CommandStarted              (0x0086),
    Pending                     (0x0088),
    AuthenticationFailed        (0x0089),
    More                        (0x008a),
    InvalidConfiguration        (0x008b),
    ServiceStarted              (0x008c),
    EncryptedNoMitm             (0x008d),
    NotEncrypted                (0x008e),

    Unknown(0xFFFF);

    private int value;
    private BluetoothGattStatus(int value)
    {
        this.value = value;
    }

    public int value(){return this.value;}
    public static BluetoothGattStatus getStatus(int value)
    {
        for (BluetoothGattStatus status : values())
            if (status.value() == value)
                return status;

        return Unknown;
    }
}

package com.iamplus.earin.communication.cap.protocols;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public enum CapProtocolUpgradeHostCommand
{
    None                          ((byte)0x00),

    StartRequest                  ((byte)0x01),
    StartConfirm                  ((byte)0x02),
    DataBytesRequest              ((byte)0x03),
    Data                          ((byte)0x04),
    SuspendInd                    ((byte)0x05),
    ResumeInd                     ((byte)0x06),
    AbortRequest                  ((byte)0x07),
    AbortConfirm                  ((byte)0x08),
    ProgressRequest               ((byte)0x09),
    ProgressConfirm               ((byte)0x0a),
    TransferCompleteInd           ((byte)0x0b),
    TransferCompleteResult        ((byte)0x0c),
    InProgressInd                 ((byte)0x0d),
    InProgressResult              ((byte)0x0e),
    CommitRequest                 ((byte)0x0f),
    CommitConfirm                 ((byte)0x10),
    ErrorWarnInd                  ((byte)0x11),
    CompleteInd                   ((byte)0x12),
    SyncRequest                   ((byte)0x13),
    SyncConfirm                   ((byte)0x14),
    StartDataRequest              ((byte)0x15),
    IsCsrValidationDoneRequest    ((byte)0x16),
    IsCsrValidationDoneConfirm    ((byte)0x17),
    SyncAfterRobootRequest        ((byte)0x18),
    VersionRequest                ((byte)0x19),
    VersionConfirm                ((byte)0x1a),
    VariantRequest                ((byte)0x1b),
    VariantConfirm                ((byte)0x1c),
    EraseSqifRequest              ((byte)0x1d),
    EraseSqifConfirm              ((byte)0x1e),
    ErrorWarnResult               ((byte)0x1f),

    Unknown                       ((byte)0xFF);

    private byte code;
    CapProtocolUpgradeHostCommand(byte code)
    {
        this.code = code;
    }

    public byte code(){return this.code;}

    public static CapProtocolUpgradeHostCommand getEnumValue(byte code)
    {
        for (CapProtocolUpgradeHostCommand enumValue : values())
            if (enumValue.code() == code)
                return enumValue;

        return Unknown;
    }
}

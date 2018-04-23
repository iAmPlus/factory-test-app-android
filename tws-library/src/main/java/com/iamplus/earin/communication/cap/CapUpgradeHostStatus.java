package com.iamplus.earin.communication.cap;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public enum CapUpgradeHostStatus
{
    Success                              ((byte)0x00),
    OemValidationSuccess                 ((byte)0x01),

    ErrorInternalErrorDeprecated         ((byte)0x10),
    ErrorUnknownId                       ((byte)0x11),
    ErrorBadLengthDecrecated             ((byte)0x12),
    ErrorWrongVariant                    ((byte)0x13),
    ErrorWrongPartitionNumber            ((byte)0x14),

    ErrorPartitionSizeMismatch           ((byte)0x15),
    ErrorPartitionTypeNotFoundDecrecated ((byte)0x16),
    ErrorPartitionOpenFailed             ((byte)0x17),
    ErrorPartitionWriteFailedDecrecated  ((byte)0x18),
    ErrorPartitionCloseFailedDecrecated  ((byte)0x19),

    ErrorSfsValidationFailed             ((byte)0x1a),
    ErrorOemValidationFailedDecrecated   ((byte)0x1b),
    ErrorUpdateFailed                    ((byte)0x1c),
    ErrorAppNotReady                     ((byte)0x1d),

    ErrorLoaderError                     ((byte)0x1e),
    ErrorUnexpectedLoaderMsg             ((byte)0x1f),
    ErrorMissingLoaderMsg                ((byte)0x20),

    ErrorBatteryLow                      ((byte)0x21),
    ErrorInvalidSyncId                   ((byte)0x22),
    ErrorInErrorState                    ((byte)0x23),
    ErrorNoMemory                        ((byte)0x24),

    // The remaining errors are grouped, each section starting at a fixed offset

    ErrorBadLengthPartitionParse         ((byte)0x30),
    ErrorBadLengthTooShort               ((byte)0x31),
    ErrorBadLengthUpgradeHeader          ((byte)0x32),
    ErrorBadLengthPartitionHeader        ((byte)0x33),
    ErrorBadLengthSignature              ((byte)0x34),
    ErrorBadLengthDatahdrResume          ((byte)0x35),

    ErrorOemValidationFailedHeaders          ((byte)0x38),
    ErrorOemValidationFailedUpgradeHeader    ((byte)0x39),
    ErrorOemValidationFailedPartitionHeader1 ((byte)0x3a),
    ErrorOemValidationFailedPartitionHeader2 ((byte)0x3b),
    ErrorOemValidationFailedPartitionData    ((byte)0x3c),
    ErrorOemValidationFailedFooter           ((byte)0x3d),
    ErrorOemValidationFailedMemory           ((byte)0x3e),

    ErrorPartitionCloseFailed                ((byte)0x40),
    ErrorPartitionCloseFailedHeader          ((byte)0x41),

/*! When sent, the error indicates that an upgrade could not be completed
 * due to concerns about space in Persistent Store.  No other upgrade
 * activities will be possible until the device restarts.
 * This error requires a UPGRADE_HOST_ERRORWARN_RES response (following
 * which the library will cause a restart, if the VM application permits)
 */
    ErrorPartitionCloseFailedPsSpace         ((byte)0x42),

    ErrorPartitionTypeNotMatching            ((byte)0x48),
    ErrorPartitionTypeTwoDfu                 ((byte)0x49),

    ErrorPartitionWriteFailedHeader          ((byte)0x50),
    ErrorPartitionWriteFailedData            ((byte)0x51),

    ErrorFileTooSmall                        ((byte)0x58),
    ErrorFileTooBig                          ((byte)0x59),

    ErrorInternalError1                      ((byte)0x5a),
    ErrorInternalError2                      ((byte)0x5b),
    ErrorInternalError3                      ((byte)0x5c),
    ErrorInternalError4                      ((byte)0x5d),
    ErrorInternalError5                      ((byte)0x5e),
    ErrorInternalError6                      ((byte)0x5f),
    ErrorInternalError7                      ((byte)0x60),

    WarnAppConfigVersionIncompatible         ((byte)0x80),
    WarnSyncIdIsDifferent                    ((byte)0x81),

    Unknown                                  ((byte)0xFF);

    private byte code;
    CapUpgradeHostStatus(byte code)
    {
        this.code = code;
    }

    public byte code(){return this.code;}

    public static CapUpgradeHostStatus getEnumValue(byte code)
    {
        for (CapUpgradeHostStatus enumValue : values())
            if (enumValue.code() == code)
                return enumValue;

        return Unknown;
    }
}

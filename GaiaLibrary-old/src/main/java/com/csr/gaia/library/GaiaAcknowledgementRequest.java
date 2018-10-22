/* ************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.csr.gaia.library;


/**
 * The data structure to define an acknowledgement request.
 */
public class GaiaAcknowledgementRequest extends GaiaRequest {

    /**
     * The status for the acknowledgement.
     */
    public final Gaia.Status  status;
    /**
     * Any data to add to the ACK.
     */
    public final byte[] data;

    /**
     * To build a new request of type acknowledgement.
     */
    public GaiaAcknowledgementRequest(Gaia.Status status, byte[] data) {
        super(GaiaRequest.Type.ACKNOWLEDGEMENT);
        this.status = status;
        this.data = data;
    }
}

package com.dtu.marksv.dexcomble2.BLEConstants;

/**
 * Created by marksv on 11/12/14.
 */
public enum MSGCode {
    PROGRESS("101"),
    DISMISS("102"),
    CLEAR("201"),
    CONNECTION_STATUS("301"),
    AUTH_STATUS("302"),
    EGV_Update("400"),
    EXTRA_DATA("EXTRA_DATA");

    private final String msg;

    MSGCode(String msg) {
        this.msg = msg;
    }

    public String getValue() {
        return msg;
    }
}

package com.dtu.marksv.dexcomble2.BLEConstants;

/**
 * Created by marksv on 11/12/14.
 */
public enum AuthStatus {
    /* Receiver.STATUS_CHAR response codes */
    VALID("1"),
    INVALID("0"),
    NOT_ENTERED("X");

    private final String status;

    AuthStatus(String status) {
        this.status = status;
    }

    public String getValue() {
        return status;
    }
}

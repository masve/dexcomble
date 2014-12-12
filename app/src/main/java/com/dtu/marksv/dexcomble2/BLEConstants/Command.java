package com.dtu.marksv.dexcomble2.BLEConstants;

/**
 * Created by marksv on 11/12/14.
 */
public enum Command {

    /*

    A Ping command
    private final static byte[] ping = {0x01, 0x01, 0x01, 0x06, 0x00, 0x0A};

    A Get Page Range Message: EGV
    private final static byte[] egvPageRange = {0x01, 0x01, 0x01, 0x07, 0x00, 0x10, 0x04};

    Read Page Message
    private final static byte[] egvGetPage = {0x01, 0x01, 0x01, 0x0C, 0x00, 0x11, 0x04};

     */
    PING(new byte[] {0x01, 0x01, 0x01, 0x06, 0x00, 0x0A}),
    GET_EGV_PAGE_RANGE(new byte[] {0x01, 0x01, 0x01, 0x07, 0x00, 0x10, 0x04}),
    GET_EGV_PAGE(new byte[] {0x01, 0x01, 0x01, 0x0C, 0x00, 0x11, 0x04});

    private final byte[] cmd;

    Command(byte[] cmd) {
        this.cmd = cmd;
    }

    public byte[] getValue() {
        return cmd;
    }
}

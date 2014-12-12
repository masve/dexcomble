package com.dtu.marksv.dexcomble2.BLEConstants;

import java.util.UUID;

/**
 * Created by marksv on 11/12/14.
 */
public enum TXPower {
    /* SERVICE: TX Power  */

    TX_POWER_SERVICE(java.util.UUID.fromString("00001804-0000-1000-8000-00805F9B34FB")),

    /* Characteristic: TX Power  */

    POWER_LEVEL_CHAR(java.util.UUID.fromString("00002A07-0000-1000-8000-00805F9B34FB"));

    private final UUID uuid;

    TXPower(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getValue() {
        return uuid;
    }
}

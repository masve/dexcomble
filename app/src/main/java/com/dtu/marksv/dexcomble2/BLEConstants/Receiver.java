package com.dtu.marksv.dexcomble2.BLEConstants;

import java.util.UUID;

/**
 * Created by marksv on 11/12/14.
 */
public enum Receiver {
    /* SERVICE: Gen4RcvService  */

    SERVICE(java.util.UUID.fromString("F0ACA0B1-EBFA-F96F-28DA-076C35A521DB")),

    /* Characteristics: Gen4RcvService  */

    AUTH_CHAR(java.util.UUID.fromString("F0ACACAC-EBFA-F96F-28DA-076C35A521DB")),
    STATUS_CHAR(java.util.UUID.fromString("F0ACB0CD-EBFA-F96F-28DA-076C35A521DB")),
    HEARTBEAT_CHAR(java.util.UUID.fromString("F0AC2B18-EBFA-F96F-28DA-076C35A521DB")),
    ARRAY_SVR_CHAR(java.util.UUID.fromString("F0ACB20A-EBFA-F96F-28DA-076C35A521DB")),
    ARRAY_CLIENT_CHAR(java.util.UUID.fromString("F0ACB20B-EBFA-F96F-28DA-076C35A521DB")),
    SMARTPHONE_CMD_CHAR(java.util.UUID.fromString("F0ACB0CC-EBFA-F96F-28DA-076C35A521DB")),

    /* Descriptors */

    STATUS_CONFIG(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")),
    ARRAY_CLIENT_CONFIG(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

    private final UUID uuid;

    Receiver(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getValue() {
        return uuid;
    }
}

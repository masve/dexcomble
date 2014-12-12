package com.dtu.marksv.dexcomble2.BLEConstants;

import java.util.UUID;

/**
 * Created by marksv on 11/12/14.
 */
public enum Device {

    /* SERVICE: Device Information  */

    INFO_SERVICE(java.util.UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")),

    /* Characteristics: Device Information  */

    MODEL_NUMBER_CHAR(java.util.UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB")),
    HARDWARE_REVISION_CHAR(java.util.UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB")),
    FIRMWARE_REVISION_CHAR(java.util.UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB")),
    MANUFACTURER_NAME_CHAR(java.util.UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB"));

    private final UUID uuid;

    Device(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getValue() {
        return uuid;
    }
}

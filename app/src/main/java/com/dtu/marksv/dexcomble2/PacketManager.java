package com.dtu.marksv.dexcomble2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

/**
 * Created by marksv on 30/10/14.
 */
public class PacketManager {
    private final static byte[] ping =  {0x01, 0x01, 0x01, 0x06, 0x00, 0x0A, 0x5E, 0x65};
    private final static byte[] ping2 = {0x01, 0x01, 0x01, 0x06, 0x00, 0x0A};


    /* A Get Page Range Message: */
    public final static byte[] pageRange = {0x01, 0x01, 0x01, 0x07, 0x00, 0x10, 0x00, 0x0F, (byte)0xF8};

    /* A Get Page Range Message: EGV */
    private final static byte[] egvPageRangeNoCRC = {0x01, 0x01, 0x01, 0x07, 0x00, 0x10, 0x04};
    private final static byte[] egvPageRangeCRC = {0x01, 0x01, 0x01, 0x07, 0x00, 0x10, 0x04, 0x21, (byte)0xA6};

    public final static int PING = 0;
    public final static int PAGE_RANGE = 100;
    public final static int EGV_PAGE_RANGE = 101;


    /* Get last EGV record */
//    public static byte[] lastEVG = {0x01, 0x01, 0x01, 0x0C, 0x00, 0x11, 0x04, 0xll, 0xll, 0xll, 0xll, 0x01, 0xcrcl, 0xcrch};


    public static byte[] getPacket(int cmd) {
        switch (cmd) {
            case PING:
                return ping;
                //return generatePacket(ping, false);
            case EGV_PAGE_RANGE:
                return generatePacket(egvPageRangeNoCRC, true);
            case PAGE_RANGE:
                return generatePacket(pageRange, false);
        }
        return null;
    }

    private static byte[] generatePacket(byte[] cmd, boolean setCRC) {
        if (setCRC)
            return toLittleEndian(CRC16.get2(cmd));
//            return CRC16.get2(toLittleEndian(cmd));

        return toLittleEndian(cmd);
    }

    private static byte[] toLittleEndian(byte[] cmd) {
        byte[] littlePing = cmd;
        for (int i = 0, j = cmd.length - 1; i < j; i++, j--)
        {
            byte b = littlePing[i];
            littlePing[i] = littlePing[j];
            littlePing[j] = b;
        }
//        ByteBuffer bb = ByteBuffer.wrap(cmd);
//        bb.order(ByteOrder.BIG_ENDIAN);
        return littlePing;
    }
}

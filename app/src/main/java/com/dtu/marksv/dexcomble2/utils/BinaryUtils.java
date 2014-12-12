package com.dtu.marksv.dexcomble2.utils;

/**
 * Created by marksv on 11/12/14.
 */
public class BinaryUtils {
    /**
     * word32le('index')
     .word32le('numrec')
     .word8('type')
     .word8('revision')
     .word32le('pages')
     .word32le('r1')
     .word32le('r2')
     .word32le('r3')
     .word16le('crc')
     */

    public static int word8(int[] data) {
        return data[0];
    }

    public static int word16(int[] data) {
        return data[1] << 8 | data[0];
    }

    public static int word32(int[] data) {
        return data[3] << 24 | data[2] << 16 | data[1] << 8 | data[0];
    }

//    public static int word32(byte[] data) {
//
//    }

}

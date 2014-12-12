package com.dtu.marksv.dexcomble2.utils;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Created by marksv on 11/12/14.
 */
public class CommonUtils {
    public static int[] cutValue(int[] array, int start, int length) {
        return Arrays.copyOfRange(array, start, start+length);
    }

    public static int[] cutRemainder(int[] array, int start, int length) {
        int[] remainderHigh = Arrays.copyOfRange(array, start+length, array.length);

        if (start == 0) {
            return remainderHigh;
        }

        int[] remainderLow = Arrays.copyOfRange(array, 0, start);

        return ArrayUtils.addAll(remainderLow, remainderHigh);
    }

    public static void printArray(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            System.out.println(String.format("%02X ", bytes[i]));
        }
    }

    public static String beautifulBytes(byte[] bytes) {
        StringBuilder builder = new StringBuilder();

        builder.append("byte[] array = { ");

        for (int i = 0; i < bytes.length; i++) {

            String hex = Integer.toHexString(bytes[i] & 0xFF);

            if (hex.length() == 1)
                builder.append("0x0");
            else
                builder.append("0x");

            builder.append(hex);

            if (i < bytes.length - 1)
                builder.append(", ");
        }

        builder.append(" };");

        return builder.toString();
    }

    public static int[] bytesToIntegers(byte[] bytes) {
        int[] integers = new int[bytes.length];
        for(int i = 0; i < bytes.length; i++) {
            integers[i] = bytes[i] & 0xFF;
        }
        return integers;
    }
}

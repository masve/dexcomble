package com.dtu.marksv.dexcomble2.utils;

/**
 * Created by marksv on 07/12/14.
 */
public class Formatter {
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
}

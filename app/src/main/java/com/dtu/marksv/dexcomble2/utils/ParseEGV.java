package com.dtu.marksv.dexcomble2.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marksv on 11/12/14.
 */
public class ParseEGV {

    private static final int length = 534;
    private static final int crcLength = 2;

    private static EGVRecord[] parse(int[] data) {

        // check that data is expected length
        if (data.length < length) {
            return null;
        }

        // cut data we dont need
        if (data.length > length) {
            data = CommonUtils.cutRemainder(data, 0, length);
        }

        // check crc

        int[] expected = CommonUtils.cutValue(data, data.length - crcLength, crcLength);
        int[] actual = CRC16_Integers.get(data);

       if (expected[0] == actual[0] && expected[1] == actual[1]) {

            // get payload
            data = CommonUtils.cutValue(data, 32, data.length - crcLength);

            int expectedRecords = data.length / 13;
           List<EGVRecord> records = new ArrayList<EGVRecord>();


            for (int i = 0; i < expectedRecords; i++) {
                // parse egv record
                int[] recordData = CommonUtils.cutValue(data, i * 13, 13);

                int systemTime = BinaryUtils.word32(CommonUtils.cutValue(recordData, 0, 4));
                int displayTime = BinaryUtils.word32(CommonUtils.cutValue(recordData, 4, 4));
                int fullGlucose = BinaryUtils.word16(CommonUtils.cutValue(recordData, 8, 2));
                int fullTrend = BinaryUtils.word8(CommonUtils.cutValue(recordData, 10, 1));
                int crc = BinaryUtils.word16(CommonUtils.cutValue(recordData, 11, 2));

                // discard records with illegal crc values
                if (crc != 0 && crc != 65535) {

                    // additional crc checks can be made per record before added to records list

                    records.add(new EGVRecord(systemTime, displayTime, fullGlucose, fullTrend, crc));
                }
            }



           return records.toArray(new EGVRecord[records.size()]);

        } else {
            System.out.println("CRC check failed");
            return null;
        }
    }

    public static int getLatestEGV(byte[] data) {
        int[] i = CommonUtils.bytesToIntegers(data);
        EGVRecord[] records = ParseEGV.parse(i);

        if(records == null) {
            System.out.println("Something went wrong");
            return -1;
        }

//        for(EGVRecord e : records) {
//            System.out.println(e.toString());
//        }

        return records[records.length - 1].getFullGlucose();
    }


}

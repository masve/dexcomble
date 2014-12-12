/*

To get EGR values, you need to request data base records.

First thing you can try is ping.  The ping will return the same data and verify that your are communicating with the receiver app.


You need to find the page range of the EVR data using this command:
This gets first page id and last page id of data base record of specific type.  The 04 is EGV data type.
Cmd=ReadDataBasePageRange = 16 =0x10
0x           01   07  00   10  04  CRCH  CRCL

returns
0x         01  0D    00    ss  ss   ss  ss  ll  ll ll  ll  CRCH CRCL  :   ss = 4 bytes start page #, ll = 4 bytes identifying last page number of egv data


Then you can read the last page of the EGV data to get latest value:
This reads specific DB pages you request
Cmd=ReadDatabasePages=17
0x           01  0C  00    11  04 pp pp pp pp #p crcl crch

Then you have to package them in the segmented BLE protocol (seg num, num_seg, msg ….)
A ping message looks like this:

Ping Message:
01-01-01-06-00-0A-5E-65

A Get Page Range Message:
01-01-01-07-00-10-00-0F-F8

01-01-01-07-00-10-04-crcl-crch                                   (for EGV, record type = 4)


A Read Page Message:
01-01-01-0C-00-11-00-00-00-00-00-01-6E-45  (example type = 0, start from record 0, get 1 record)

01-01-01-0C-00-11-04-ll-ll-ll-ll-01-crcl-crch              (for EGV record type = 4, start from last record, get 1 record)

 */


package com.dtu.marksv.dexcomble2.utils;

import com.dtu.marksv.dexcomble2.BLEConstants.Command;

import java.util.Arrays;

/**
 * Created by marksv on 30/10/14.
 */
public class PacketManager {




//    public final static int PING = 0;
//    public final static int PAGE_RANGE = 100;
//    public final static int EGV_PAGE_RANGE = 101;
//    public final static int READ_EGV = 200;



    public static byte[] getPacket(Command cmd, byte[] data) {
        switch (cmd) {
            case PING:
                return generatePacket(Command.PING.getValue(), true);
            case GET_EGV_PAGE_RANGE:
                return generatePacket(Command.GET_EGV_PAGE_RANGE.getValue(), true);
            case GET_EGV_PAGE:
                return generatePacket(createEGVRequest(data), true);
        }
        return null;
    }

    private static byte[] generatePacket(byte[] cmd, boolean setCRC) {
        if (setCRC) {
            return CRC16_Bytes.get(cmd, 2);
        }
        return cmd;
    }

    private static byte[] createEGVRequest(byte[] egvPageRangeResult) {
        byte records = 0x01;
        byte[] lastPage = Arrays.copyOfRange(egvPageRangeResult, 8, 11);

        // Generate request
        byte[] request = new byte[Command.GET_EGV_PAGE.getValue().length + 5];
        for (int i = 0; i < Command.GET_EGV_PAGE.getValue().length; i++) {
            request[i] = Command.GET_EGV_PAGE.getValue()[i];
        }
        // Append last pages to request
        for (int i = 0; i < lastPage.length; i++) {
            request[Command.GET_EGV_PAGE.getValue().length + i] = lastPage[i];
        }
        // Append number of records
        request[request.length - 1] = records;

        return request;
    }
}

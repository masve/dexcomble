package com.dtu.marksv.dexcomble2;

import com.dtu.marksv.dexcomble2.BLEConstants.Command;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by marksv on 11/12/14.
 */
public class ResponseManager {
    private static final String TAG = "BLEService";

    private List<Byte> accumulatedResponse = new ArrayList<Byte>();
    private int responseLength;
    private Command nextCommand;

    private static ResponseManager ourInstance = new ResponseManager();

    public static ResponseManager getInstance() {
        return ourInstance;
    }

    private ResponseManager() {
    }

    public void processResponse(byte[] data) {
        if (data == null) return;

        if (accumulatedResponse.isEmpty()) {
            responseLength = (data[2] << 8) | data[1];
            Collections.addAll(accumulatedResponse, ArrayUtils.toObject(data));
        } else {
            if (accumulatedResponse.size() + data.length > responseLength) {

            } else {
                Collections.addAll(accumulatedResponse, ArrayUtils.toObject(data));
            }
        }
    }

    public Command processResult() {
        if (accumulatedResponse.size() == responseLength && accumulatedResponse.size() != 0) {

            switch (nextCommand) {
                case GET_EGV_PAGE:
                    return Command.GET_EGV_PAGE;

                case GET_EGV_PAGE_RANGE:
                    return Command.GET_EGV_PAGE_RANGE;

                default:
                    return null;
            }

        } else {
            // not done yet
            return null;
        }
    }

    public void setNextCommand(Command nextCommand) {
        resetResponseBuffer();
        this.nextCommand = nextCommand;
    }

    public byte[] getAccumulatedResponse() {
        Byte[] bigBytes = accumulatedResponse.toArray(new Byte[accumulatedResponse.size()]);
        return ArrayUtils.toPrimitive(bigBytes);
    }

    public void resetResponseBuffer() {
        accumulatedResponse = new ArrayList<Byte>();
    }
}

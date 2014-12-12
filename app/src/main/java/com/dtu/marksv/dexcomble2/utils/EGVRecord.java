package com.dtu.marksv.dexcomble2.utils;

/**
 * Created by marksv on 11/12/14.
 */
public class EGVRecord {
    private int systemTime;
    private int displayTime;
    private int fullGlucose;
    private int fullTrend;
    private int crc;

    public EGVRecord(int systemTime, int displayTime, int fullGlucose, int fullTrend, int crc) {
        this.systemTime = systemTime;
        this.displayTime = displayTime;
        this.fullGlucose = fullGlucose;
        this.fullTrend = fullTrend;
        this.crc = crc;
    }

    public int getSystemTime() {
        return systemTime;
    }

    public int getDisplayTime() {
        return displayTime;
    }

    public int getFullGlucose() {
        return fullGlucose;
    }

    public int getFullTrend() {
        return fullTrend;
    }

    public int getCrc() {
        return crc;
    }

    @Override
    public String toString() {
        return "EGVRecord { " +
                "systemTime = " + systemTime +
                ", displayTime = " + displayTime +
                ", fullGlucose = " + fullGlucose +
                ", fullTrend = " + fullTrend +
                ", crc = " + crc +
                " }";
    }
}

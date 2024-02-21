package org.tahomarobotics.scouting.scoutingserver.util;

import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;

public class DataPoint {
    private String name;
    private String value;
    private double howOff;



    private ErrorLevel errorLevel;

    public enum ErrorLevel {
        ZERO,
        MEDIUM,
        HIGH,
        UNKNOWN
    }

    public DataPoint(String theName, String theValue , int howOffItIs) {
        this.name = theName;
        this.value = theValue;
        this.howOff = howOffItIs;
        setErrorLevel(howOffItIs);

    }
    public DataPoint(String theName, String theValue , ErrorLevel howOffItIs) {
        this.name = theName;
        this.value = theValue;
        this.howOff = Double.NaN;
        this.errorLevel = howOffItIs;

    }

    public DataPoint(String theName, String theValue) {
        this.name = theName;
        this.value = theValue;
        this.howOff = 0;
        setErrorLevel(0);

    }

    @Override
    public String toString() {
        return name + ": " + value + " Error Level: " + errorLevel.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public double getHowOff() {
        return howOff;
    }

    public void setHowOff(double howOff) {
        this.howOff = howOff;
    }
    public ErrorLevel getErrorLevel() {
        return errorLevel;
    }

    public void setErrorLevel(double error) {
        this.howOff = error;
        error  = Math.abs(error);
        if (error == 0) {
            this.errorLevel = ErrorLevel.ZERO;
        }else if (error > Constants.LOW_ERROR_THRESHOLD) {
            this.errorLevel = ErrorLevel.HIGH;
        }else {
            this.errorLevel = ErrorLevel.MEDIUM;
        }
    }

    public static ErrorLevel translateErrorNum(double error) {
        error = Math.abs(error);
        if (error == 0) {
            return ErrorLevel.ZERO;
        }else if (error == Double.NaN) {
            return ErrorLevel.UNKNOWN;
        }else if (error > Constants.LOW_ERROR_THRESHOLD) {
             return  ErrorLevel.HIGH;
        }else {
            return ErrorLevel.MEDIUM;
        }
    }

}

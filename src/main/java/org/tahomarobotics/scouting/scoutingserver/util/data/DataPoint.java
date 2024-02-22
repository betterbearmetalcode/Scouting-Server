package org.tahomarobotics.scouting.scoutingserver.util.data;

import javafx.scene.paint.Color;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;


import java.util.HashMap;
import java.util.Map;

public class DataPoint {
    private String name;
    private String value;
    private double howOff;

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    private boolean validated = false;

    private ErrorLevel errorLevel;

    public enum ErrorLevel {
        ZERO,
        UNKNOWN,
        MEDIUM,
        HIGH,

    }

    public static HashMap<ErrorLevel, Color> color = new HashMap<>(Map.ofEntries(
            Map.entry(ErrorLevel.HIGH, Color.RED),
            Map.entry(ErrorLevel.MEDIUM, Color.YELLOW),
            Map.entry(ErrorLevel.ZERO, Color.GREEN),
            Map.entry(ErrorLevel.UNKNOWN, Color.BLUE)
    ));

    public DataPoint(String theName, String theValue ,double errorLevel) {
        this.name = theName;
        this.value = theValue;
        this.howOff = errorLevel;
        this.validated = true;
        this.errorLevel = translateErrorNum(errorLevel);

    }

    public DataPoint(String theName, String theValue) {
        this.name = theName;
        this.value = theValue;
        this.howOff = Double.NaN;
        this.validated = false;
        this.errorLevel = ErrorLevel.UNKNOWN;

    }

    @Override
    public String toString() {
        String error = !validated?", Unchecked":", Error Level: " + errorLevel.toString() + ((errorLevel.ordinal() != 3)?" (" + howOff + ")":"");
        return name.toLowerCase().replaceAll("_", " ") + ": " + value + error;
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
        }else if (Double.isNaN(error)) {
            return ErrorLevel.UNKNOWN;
        }else if (error > Constants.LOW_ERROR_THRESHOLD) {
             return  ErrorLevel.HIGH;
        }else {
            return ErrorLevel.MEDIUM;
        }
    }

}

package org.tahomarobotics.scouting.scoutingserver.util.data;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
        UNKNOWN,
        ZERO,

        MEDIUM,
        HIGH,

    }

    public static HashMap<ErrorLevel, Color> color = new HashMap<>(Map.ofEntries(
            Map.entry(ErrorLevel.HIGH, Color.RED),
            Map.entry(ErrorLevel.MEDIUM, Color.ORANGE),
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

    public DataPoint(String displayString) {
        String[] tokens = displayString.split(":");
        this.name = tokens[0];
        this.value = tokens[1];
        String errorVal = tokens[2].split("=")[1];
        if (!Objects.equals(errorVal, ErrorLevel.UNKNOWN.toString())) {
            this.howOff = Integer.parseInt(errorVal);
        }else {
            howOff = Double.NaN;
        }
        this.validated = false;
        this.errorLevel = translateErrorNum(howOff);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getName().replaceAll("_", " ").toLowerCase()).append(":");//name without underscores and a colon
        stringBuilder.append(getValue()).append(":");//the value and a period
        stringBuilder.append("Error=");
        if (errorLevel == ErrorLevel.UNKNOWN) {
            stringBuilder.append(ErrorLevel.UNKNOWN);
        }else {
            stringBuilder.append(howOff);
        }
        return stringBuilder.toString();
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

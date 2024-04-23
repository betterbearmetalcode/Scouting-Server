package org.tahomarobotics.scouting.scoutingserver.util.data;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration.Datatype;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataPoint<T> {
    private final String name;
    private T value;
    private final Datatype datatype;

    public DataPoint(T theValue, String theName, Datatype theDatatype) {
        this.name = theName;
        this.value = theValue;
        this.datatype = theDatatype;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Datatype getDatatype() {
        return datatype;
    }

    private double howOff;

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    private boolean validated = false;

    private ErrorLevel errorLevel;

    public boolean expanded = false;//for use in constructing trees

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

    public DataPoint(String theName, T theValue, double errorLevel) {
        this.name = theName;
        this.value = theValue;
        this.howOff = errorLevel;
        this.validated = true;
        this.errorLevel = translateErrorNum(errorLevel);
        this.datatype = Datatype.STRING;

    }

    public DataPoint(String theName, T theValue) {
        this.name = theName;
        this.value = theValue;
        this.howOff = Double.NaN;
        this.validated = false;
        this.errorLevel = ErrorLevel.UNKNOWN;
        this.datatype = Datatype.STRING;

    }

    public DataPoint(String displayString) {
        String[] tokens = displayString.split(":");
        this.name = tokens[0].replaceAll(" ", "_").toUpperCase();
        this.value = (T) tokens[1];
        String errorVal = tokens[2].split("=")[1];
        if (!Objects.equals(errorVal, ErrorLevel.UNKNOWN.toString())) {
            this.howOff = Double.parseDouble(errorVal);
        }else {
            howOff = Double.NaN;
        }
        this.validated = false;
        this.errorLevel = translateErrorNum(howOff);
        this.datatype = Datatype.STRING;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getName().replaceAll("_", " ").toLowerCase()).append(":");//name without underscores and noteA colon
        stringBuilder.append(getValue()).append(":");//the value and noteA period
        stringBuilder.append(" Error=");
        if (errorLevel == ErrorLevel.UNKNOWN) {
            stringBuilder.append(ErrorLevel.UNKNOWN);
        }else {
            stringBuilder.append(howOff);
        }
        return stringBuilder.toString();
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

package org.tahomarobotics.scouting.scoutingserver.util.configuration;

public class DataMetric {

    private final boolean validateable;
    private final String name;
    private final Configuration.Datatype datatype;

    private final Object defaultValue;

    public DataMetric (Configuration.Datatype  theDataype, String theName, boolean isValidateable, Object theDefaultValue) {
        validateable = isValidateable;
        name = theName;
        datatype = theDataype;
        defaultValue = theDefaultValue;
    }

    public String getName() {
        return name;
    }

    public Configuration.Datatype getDatatype() {
        return datatype;
    }

    public String getDatatypeAsString() {
        return String.valueOf(datatype.ordinal());
    }

    @Override
    public String toString() {
        return "Data Metric: " + name + ", is an " + datatype.name().toLowerCase() + " and it is " + validateable + " that is can be validated";
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}

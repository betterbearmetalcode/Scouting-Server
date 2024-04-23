package org.tahomarobotics.scouting.scoutingserver.util.configuration;

public class DataMetric {

    private final boolean validateable;
    private final String name;
    private final Configuration.Datatype datatype;

    public DataMetric (Configuration.Datatype  theDataype, String theName, boolean isValidateable) {
        validateable = isValidateable;
        name = theName;
        datatype = theDataype;
    }

    @Override
    public String toString() {
        return "Data Metric: " + name + ", is an " + datatype.name().toLowerCase() + " and it is " + validateable + " that is can be validated";
    }

}

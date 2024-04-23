package org.tahomarobotics.scouting.scoutingserver.util.exceptions;

public class ConfigFileFormatException extends Exception{
    public ConfigFileFormatException() {
        super();
    }
    public ConfigFileFormatException (String message) {
        super(message);
    }
}

package org.tahomarobotics.scouting.scoutingserver.util.configuration;

import java.io.IOException;

public class ConfigFileFormatException extends Exception{
    public ConfigFileFormatException() {
        super();
    }
    public ConfigFileFormatException (String message) {
        super(message);
    }
}

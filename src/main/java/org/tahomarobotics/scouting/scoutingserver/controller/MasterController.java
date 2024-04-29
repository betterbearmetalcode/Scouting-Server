package org.tahomarobotics.scouting.scoutingserver.controller;

import org.tahomarobotics.scouting.scoutingserver.util.Logging;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class MasterController {
    
    //this class in intended to recieve method calls from all over the app and distribute those method calls to 
    //more speacalized controllers
    
    public static void createDatabase() {
        Logging.logInfo("Creating Database");
        
    }

    public static void openJSONFileInDatabase() {
        Logging.logInfo("Opening JSON File Database");
    }

    public static void saveCurrentDatabase() {
        Logging.logInfo("Saving current database");
    }

    public static void saveCurrentDatabaseAs() {
        Logging.logInfo("Saving current database to new file");
    }

    public static void saveAllOpenDatabases() {
        Logging.logInfo("saving all open databases");
    }

    //returns new name of item
    public static String toggleDataTransferServer() {
        Logging.logInfo("starting data transfer server");
        return String.valueOf(System.currentTimeMillis());
    }

    public static void addJSONFile() {
        Logging.logInfo("Adding JSON File to database");
    }

    public static void addCSVFile() {
        Logging.logInfo("Adding CSV File to database");
    }

    public static void validateDatabase() {
        Logging.logInfo("Validating database");
    }

    public static void mergeDatabases() {
        Logging.logInfo("Merging databases");
    }

    public static void findTraps() {
        Logging.logInfo("Finding traps");
    }

    public static void genenerateStratScoutingSchedule() {
        Logging.logInfo("Generating Strat scouting schedule");
    }

    public static void openDocumentation() {
        Logging.logInfo("Opening Documentation");
    }

    //( ͡° ͜ʖ ͡°)
    public static void rickrollUser() {
        Logging.logInfo("Rickrolling user lol");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
            } catch (IOException | URISyntaxException e) {
                Logging.logError(e);
            }
        }
    }
}

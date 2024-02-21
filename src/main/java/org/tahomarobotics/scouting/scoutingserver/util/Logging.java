package org.tahomarobotics.scouting.scoutingserver.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Logging {
    private final static Logger LOGGER =
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void logError(Exception e) {
        LOGGER.log(Level.SEVERE, "exception: ", e);
        Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            alert.hide();
        }


//        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
//            LOGGER.log(Level.SEVERE, stackTraceElement.toString());
//        }
    }
}

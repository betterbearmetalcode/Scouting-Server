package org.tahomarobotics.scouting.scoutingserver.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;
import java.util.logging.Level;

public class Logging {
    private final static java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);

    public static void logError(Exception e) {
        LOGGER.log(Level.SEVERE, "exception: ", e);
        Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            alert.hide();
        }


    }

    public static void logError(Exception e, String customMessage) {
        LOGGER.log(Level.SEVERE, customMessage, e);
        Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
        alert.showAndWait();


    }
}

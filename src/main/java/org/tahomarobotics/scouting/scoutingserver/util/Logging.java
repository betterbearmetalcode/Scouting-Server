package org.tahomarobotics.scouting.scoutingserver.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;
import java.util.logging.Level;

public class Logging {
    private final static java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);

    public static void logError(Exception e) {
        logError(e, "");


    }

    public static void logError(Exception e, String customMessage) {
        LOGGER.log(Level.SEVERE, customMessage, e);
        Platform.runLater(() -> {

            Alert alert = new Alert(Alert.AlertType.ERROR,customMessage + ((e.getMessage() == null)?"":e.getMessage()));
            alert.showAndWait();
        });



    }
}

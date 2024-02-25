package org.tahomarobotics.scouting.scoutingserver.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.tahomarobotics.scouting.scoutingserver.Constants;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.logging.*;

public class Logging {
    private final static java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);

    static {
        File dir = new File(Constants.BASE_APP_DATA_FILEPATH + "/resources/logs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Handler fh = null;
        try {
            fh = new FileHandler(dir.getAbsolutePath() + "\\" + new Date(System.currentTimeMillis()).toString().replaceAll(":", "") + ".log");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOGGER.addHandler(fh);
        LOGGER.addHandler(new ConsoleHandler());
        LOGGER.setLevel(Level.FINEST);
        Logger.getLogger("").addHandler(fh);
        Logger.getLogger("com.wombat").setLevel(Level.FINEST);
    }

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

    public static void logInfo(String info) {
        LOGGER.log(Level.INFO, info);

    }

    public void logInfo(String info, boolean showAlert) {
        logInfo(info);
        if (showAlert) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, info);
                alert.showAndWait();
            });
        }
    }
}

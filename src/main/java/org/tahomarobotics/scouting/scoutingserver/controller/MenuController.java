package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.ServerUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MenuController extends VBox {

    @FXML
    Button qrScannerButton = new Button();

    @FXML
    public void enterDataCollectionScene(ActionEvent event) {
        if (ScoutingServer.currentScene != ScoutingServer.SCENES.QR_SCANNER) {
            ScoutingServer.setCurrentScene(ScoutingServer.dataCollectionScene);
            ScoutingServer.currentScene = ScoutingServer.SCENES.QR_SCANNER;
        }
    }

    @FXML
    public void backToMainMenu(ActionEvent event) {
        //no main menu, just a help button
        if (ScoutingServer.currentScene != ScoutingServer.SCENES.MAIN_MENU) {


            ScoutingServer.setCurrentScene(ScoutingServer.mainScene);
            ScoutingServer.currentScene = ScoutingServer.SCENES.MAIN_MENU;
        }
        //open tutorial
/*        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/Tutorial/TutorialPage.html").toURI());
            } catch (IOException e) {
                Logging.logError(e);
            }
        }*/
    }

    @FXML
    public void openDataScene(ActionEvent event) {
        if (ScoutingServer.currentScene != ScoutingServer.SCENES.DATA_SCENE) {


            ScoutingServer.setCurrentScene(ScoutingServer.dataScene);
            ScoutingServer.currentScene = ScoutingServer.SCENES.DATA_SCENE;
        }
    }

    @FXML
    public void enterDataCorrectionScene(ActionEvent event) {
        if (ScoutingServer.currentScene != ScoutingServer.SCENES.DATA_CORRECTION) {


            ScoutingServer.setCurrentScene(ScoutingServer.dataCorrectionScene);
            ScoutingServer.currentScene = ScoutingServer.SCENES.DATA_CORRECTION;
        }
    }


}

package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;

public class MenuController extends VBox {

@FXML
    Button qrScannerButton = new Button();
    @FXML
    public void enterQRScanner(ActionEvent event) {
        if (ScoutingServer.currentScene != ScoutingServer.SCENES.QR_SCANNER) {
            ScoutingServer.setCurrentScene(ScoutingServer.qrScannerScene);
            ScoutingServer.currentScene = ScoutingServer.SCENES.QR_SCANNER;
        }
    }

    @FXML
    public void backToMainMenu(ActionEvent event) {
        if (ScoutingServer.currentScene != ScoutingServer.SCENES.MAIN_MENU) {


            ScoutingServer.setCurrentScene(ScoutingServer.mainScene);
            ScoutingServer.currentScene = ScoutingServer.SCENES.MAIN_MENU;
        }
    }

    public void debugggy(ActionEvent event) {

    }


}

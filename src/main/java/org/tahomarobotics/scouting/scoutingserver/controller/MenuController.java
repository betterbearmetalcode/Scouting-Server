package org.tahomarobotics.scouting.scoutingserver;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.tahomarobotics.scouting.scoutingserver.util.DataHandler;
import org.tahomarobotics.scouting.scoutingserver.util.WebcamCapture;

import java.io.IOException;
import java.util.ArrayList;

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

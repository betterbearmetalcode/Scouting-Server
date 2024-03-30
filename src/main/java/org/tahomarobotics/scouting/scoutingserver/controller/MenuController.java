package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MenuController extends VBox {

    @FXML
    Button qrScannerButton = new Button();

    @FXML
    public void enterDataCollectionScene(ActionEvent event) {
        if (ScoutingServer.currentScene != ScoutingServer.SCENES.QR_SCANNER) {
            Logging.logInfo("Switching to data collection scene");//tells anyone reading the logs what is happening so we can analyze the circumstances leading up to errors
            ScoutingServer.setCurrentScene(ScoutingServer.dataCollectionScene);
            ScoutingServer.currentScene = ScoutingServer.SCENES.QR_SCANNER;
        }
    }

    @FXML
    public void backToMainMenu(ActionEvent event) {
        Logging.logInfo("Opening Help menu");
        //open tutorial
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/Tutorial/TutorialPage.html").toURI());
            } catch (IOException e) {
                Logging.logError(e);
            }
        }
    }

    @FXML
    public void openDataScene(ActionEvent event) {
        if (ScoutingServer.currentScene != ScoutingServer.SCENES.DATA_SCENE) {
            Logging.logInfo("Opening data scene");

            ScoutingServer.setCurrentScene(ScoutingServer.dataScene);
            ScoutingServer.currentScene = ScoutingServer.SCENES.DATA_SCENE;
        }
    }

    @FXML
    public void enterDataCorrectionScene(ActionEvent event) {
        if (ScoutingServer.currentScene != ScoutingServer.SCENES.DATA_CORRECTION) {

            Logging.logInfo("Opening data correction scene");
            ScoutingServer.setCurrentScene(ScoutingServer.dataCorrectionScene);
            ScoutingServer.currentScene = ScoutingServer.SCENES.DATA_CORRECTION;
        }
    }

    //unused now, maybe in the future
    /*@FXML
    public void developerToolsButton(ActionEvent event) {
        AtomicReference<Boolean> selectedEvent = new AtomicReference<>(false);
        if (!Constants.devUnlocked) {
            PasswordField textField = new PasswordField();
            Dialog<Boolean> dialog = new Dialog<>();
            FlowPane pane = new FlowPane(new Label("Enter Password: "), textField);
            dialog.getDialogPane().setContent(pane);
            dialog.setTitle("Enter Developer Password");
            dialog.setHeaderText("");
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButton) {
                    return textField.getText().equals(Constants.DEVELOPER_PASSWORD);
                }else {
                    return false;
                }
            });
            Optional<Boolean> result = dialog.showAndWait();

            result.ifPresent(selectedEvent::set);
        }

        if (selectedEvent.get() || Constants.devUnlocked) {
            //then the password was right or has already been unlocked
            Constants.devUnlocked = true;
            if (ScoutingServer.currentScene != ScoutingServer.SCENES.MAIN_MENU) {
                ScoutingServer.setCurrentScene(ScoutingServer.mainScene);
                ScoutingServer.currentScene = ScoutingServer.SCENES.MAIN_MENU;
            }
        }
    }*/
    @FXML
    public void openCharts(ActionEvent event) {
        if (ScoutingServer.currentScene != ScoutingServer.SCENES.CHARTS) {
            Logging.logInfo("Opening Charts");

            ScoutingServer.setCurrentScene(ScoutingServer.chartsScene);
            ScoutingServer.currentScene = ScoutingServer.SCENES.CHARTS;
        }
    }



}

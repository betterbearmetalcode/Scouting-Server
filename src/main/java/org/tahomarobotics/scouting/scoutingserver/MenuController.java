package org.tahomarobotics.scouting.scoutingserver;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MenuController extends VBox {

@FXML
    Button qrScannerButton = new Button();
    @FXML
    public void enterQRScanner(ActionEvent event) {
        Stage mainStage = (Stage) qrScannerButton.getScene().getWindow();

        VBox parent = (VBox) ScoutingServer.qrScannerScene.getRoot();
        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane anchorPane = (AnchorPane) splitPane.getItems().get(0);
        anchorPane.getChildren().add(ScoutingServer.qrHamburgerMenu);


        mainStage.setScene(ScoutingServer.qrScannerScene);

    }
}

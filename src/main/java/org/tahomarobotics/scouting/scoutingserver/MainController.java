package org.tahomarobotics.scouting.scoutingserver;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

;


public class MainController extends VBox {


    public MenuItem enterFormEditButton;

    public Label testLabel;


    @FXML
    Text text = new Text("testing 123");


   @FXML
    protected void enterFormEditMode(ActionEvent event) {
       Stage mainStage = (Stage) testLabel.getScene().getWindow();

        mainStage.setScene(ScoutingServer.formEditScene);
   }
    @FXML
   protected void getTBAData(ActionEvent event) {
       System.out.println("Attempting to fetch TBA Data");
       try {
           System.out.println(APInteraction.get("/teams/0"));

       }catch (Exception e) {
           e.printStackTrace();
       }

   }


}
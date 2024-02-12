package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DataController {
    public Button newTabButton;
    @FXML
    private TabPane tabPane;

    List<TabController> controllers = new LinkedList<>();
    @FXML
    public void makeNewTab(ActionEvent event) {



        try {

            FXMLLoader tabLoader = new FXMLLoader(ScoutingServer.class.getResource("FXML/data-tab-anchor-pane.fxml"));
            //ask the user to select a database location
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Database");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            fileChooser.setInitialDirectory(new File(Constants.DATABASE_FILEPATH));
            File selectedFile = fileChooser.showOpenDialog(ScoutingServer.mainStage);
            for (TabController c : controllers) {
                if (c.database.equals(selectedFile)) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setHeaderText("Wrong Database");
                    alert.setHeaderText("Database is already open");
                    alert.showAndWait();
                    tabPane.getSelectionModel().selectPrevious();
                    return;
                }
            }
            if ( selectedFile != null && selectedFile.isFile() && new File(Constants.DATABASE_FILEPATH, selectedFile.getName()).exists()) {



                    //get the data from that database
                LinkedList<DataHandler.MatchRecord> databaseData = DataHandler.readDatabase(selectedFile.getName());
                TabController controller = new TabController(databaseData, selectedFile);
                tabLoader.setController(controller);


                //construct a new tab
                AnchorPane pane = new AnchorPane((AnchorPane) tabLoader.load());
                Tab tab = new Tab();
                tab.setText(selectedFile.getName().substring(0,selectedFile.getName().length() - 4));
                tab.setId(selectedFile.getName());
                tab.setContent(pane);
                tab.setClosable(true);
                tab.setOnClosed(new EventHandler<Event>() {
                    @Override
                    public void handle(Event event) {
                        for (TabController c : controllers) {
                            if (c.database.getName().equals(tab.getId())) {
                                controllers.remove(c);
                                break;
                            }
                        }
                    }
                });
                //pass the tree view to the controller class
                AnchorPane anotherPane = (AnchorPane) pane.getChildren().get(0);
                VBox box = (VBox) anotherPane.getChildren().get(0);
                controller.initialize((TreeView<String>) box.getChildren().get(0));

               //remove controllers from the list when the tab is closed
                tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
                tabPane.getSelectionModel().select(tab);
                controllers.add(controller);


            }else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Wrong Folder");
                alert.setHeaderText("Please select or create a .txt file in the database folder");
                alert.showAndWait();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}

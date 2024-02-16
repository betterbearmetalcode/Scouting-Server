package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;

import java.io.File;
import java.io.IOException;
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

            FXMLLoader tabLoader = new FXMLLoader(new File(System.getProperty("user.dir") + "/resources/FXML/data-tab-anchor-pane.fxml").toURI().toURL());
            //get selected database name
            ///String selectedTable = getSelectedTableFromUser();

            //ask the user to select a database location
            File selectedFile = new File(Constants.DATABASE_FILEPATH + Constants.SQL_DATABASE_NAME);

            //check if this tab is already open
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

            //if the file exists
            if (selectedFile.isFile() && new File(Constants.DATABASE_FILEPATH, selectedFile.getName()).exists()) {
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

/*    private String getSelectedTableFromUser() {
        // Create the custom dialog.
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Database");
        dialog.setHeaderText("Select or create database");


// Set the button types.
        ButtonType loginButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);


        //create the list view
        ListView<String> listView = new ListView<>();

// Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);


        dialog.getDialogPane().setContent(listView);

// Request focus on the username field by default.
        Platform.runLater(() -> username.requestFocus());

// Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();


    }*/


}

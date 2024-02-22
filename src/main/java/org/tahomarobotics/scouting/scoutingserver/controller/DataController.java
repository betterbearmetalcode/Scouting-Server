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
import org.tahomarobotics.scouting.scoutingserver.util.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.TableChooserDialog;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DataController {
    public Button newTabButton;
    @FXML
    private TabPane tabPane;

    List<TabController> controllers = new LinkedList<>();
    @FXML
    public void makeNewTab(ActionEvent event) {
        try {

            FXMLLoader tabLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/data-tab-anchor-pane.fxml").toURI().toURL());
            //get selected table name or competition from user
            String selectedTable = getSelectedTableFromUser();
            if (Objects.equals(selectedTable, "")) {
                return;
            }
            File selectedFile = new File(Constants.DATABASE_FILEPATH + Constants.SQL_DATABASE_NAME);
            //check if this tab is already open


            //get the data from that database
            LinkedList<DataHandler.MatchRecord> databaseData = DataHandler.readDatabase(selectedTable);
            TabController controller = new TabController(databaseData, selectedTable);
            tabLoader.setController(controller);


            //construct a new tab
            AnchorPane pane = new AnchorPane((AnchorPane) tabLoader.load());
            Tab tab = new Tab();
            tab.setText(selectedTable);
            tab.setId(selectedTable);
            tab.setContent(pane);
            tab.setClosable(true);
            tab.setOnClosed(new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    for (TabController c : controllers) {
                        if (Objects.equals(tab.getId(), c.tableName)) {
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



        } catch (IOException e) {
            Logging.logError(e);
        }

    }

    private String getSelectedTableFromUser() {
        String output = Constants.DEFAULT_SQL_TABLE_NAME;
        try {
            TableChooserDialog dialog = new TableChooserDialog(DatabaseManager.getTableNames());
            Optional<String> result = dialog.showAndWait();
            AtomicReference<String> selectedTable = new AtomicReference<>("");
            result.ifPresent(selectedTable::set);
            output = selectedTable.get();
            System.out.println("Dialog Result: " + selectedTable.get());
        } catch (SQLException e) {
            Logging.logError(e);
        }


        return output;
    }




}

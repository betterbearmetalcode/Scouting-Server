package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.TableChooserDialog;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.tahomarobotics.scouting.scoutingserver.Constants.UIValues.*;

public class DataController {
    public Button newTabButton;
    @FXML
    private  TabPane tabPane;


    public static List<TabController> controllers = new LinkedList<>();

    @FXML
    public void makeNewTab(ActionEvent event) {
        try {

            FXMLLoader tabLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/data-tab-anchor-pane.fxml").toURI().toURL());
            //get selected table name or competition from user
            String selectedTable = getSelectedTableFromUser();
            if (Objects.equals(selectedTable, "")) {
                return;
            }
            //check if this tab is already open

            TabController controller = new TabController(DatabaseManager.getUnCorrectedDataFromDatabase(selectedTable), selectedTable, tabPane);
            tabLoader.setController(controller);


            //construct a new tab
            VBox pane = new VBox((VBox) tabLoader.load());
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

            double appWidth = getAppWidth();
            double appHeight = getAppHeight();
            tabPane.setTabMaxHeight(Toolkit.getDefaultToolkit().getScreenSize().height);
            pane.prefHeightProperty().bind(appHeightProperty());
            pane.prefWidthProperty().bind(tabPane.tabMaxWidthProperty());
            pane.setMaxHeight(appHeight);
            VBox box = (VBox) pane.getChildren().get(0);
            box.prefWidthProperty().bind(tabPane.tabMaxWidthProperty());


            box.prefHeightProperty().bind(appHeightProperty());
            TreeView<Label> treeView = (TreeView<Label>) box.getChildren().get(0);
            treeView.prefHeightProperty().bind(Constants.UIValues.databaseHeightProperty());
            treeView.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
            //pass the tree view to the controller class

            controller.initialize(treeView);

            //remove controllers from the list when the tab is closed
            tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
            tabPane.getSelectionModel().select(tab);
            controllers.add(controller);


        } catch (IOException e) {
            Logging.logError(e);
        }

    }

    private String getSelectedTableFromUser() {
        try {
            TableChooserDialog dialog = new TableChooserDialog(SQLUtil.getTableNames());
            Optional<String> result = dialog.showAndWait();
            AtomicReference<String> selectedTable = new AtomicReference<>("");
            result.ifPresent(selectedTable::set);
            if (result.isPresent()) {
                return selectedTable.get();
            }else {
                    return "";
            }
        } catch (SQLException e) {
            Logging.logError(e);
            return "";
        }
    }



}

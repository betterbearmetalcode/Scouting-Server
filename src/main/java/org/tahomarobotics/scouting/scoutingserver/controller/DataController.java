package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class DataController {
    public TreeView treeView;
    public Button newTabButton;
    @FXML
    private TabPane tabPane;

    List<Tab> tabs = new LinkedList<>();
    @FXML
    public void makeNewTab(ActionEvent event) {
        FXMLLoader tabLoader = new FXMLLoader(ScoutingServer.class.getResource("FXML/data-tab-anchor-pane.fxml"));


        try {
            AnchorPane pane = new AnchorPane((AnchorPane) tabLoader.load());
            Tab tab = new Tab();
            tab.setText("New Database");
            tab.setContent(pane);
            tab.setClosable(true);
            tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
            tabPane.getSelectionModel().select(tab);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    
    public void selectItem(ContextMenuEvent event) {
        
    }
}

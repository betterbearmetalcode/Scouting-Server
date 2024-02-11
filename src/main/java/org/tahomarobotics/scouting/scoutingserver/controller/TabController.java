package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class TabController implements Initializable {

    public File database;

    public TabController(ArrayList<DataHandler.MatchRecord> databaseData, File d) {
        database = d;
        System.out.println("In contoller constuctor" + databaseData);
    }

    public void selectItem(Event event) {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}

package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import org.tahomarobotics.scouting.scoutingserver.util.NetworkingDebug;

import java.net.URL;
import java.util.ResourceBundle;


public class DebugController implements Initializable {


    public ScrollPane scrollPane;

    public static NetworkingDebug debug;

    public void debugggy(ActionEvent event) {

    }

    @FXML
    public void debugButton2(ActionEvent event) {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void terminate(ActionEvent event) {
        System.exit(0);
    }
}
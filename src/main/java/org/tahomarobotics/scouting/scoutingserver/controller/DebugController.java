package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.util.Pair;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.*;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DuplicateDataResolvedDialog;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
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
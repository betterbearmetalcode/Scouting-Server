package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;
import org.tahomarobotics.scouting.scoutingserver.util.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.QRCodeUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;

import java.io.IOException;


public class MainController extends VBox {

    public void debugggy(ActionEvent event) {
       System.out.println("Debug button pressed");


    }
    @FXML
    public void debugButton2(ActionEvent event) {
    }
}
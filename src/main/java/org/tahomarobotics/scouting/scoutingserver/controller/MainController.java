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
        try {
            QRCodeUtil.createQRCode("8/2051/5/2/4/7/0/1/3/0/test3/teleNotes", "C:\\Users\\Caleb\\IdeaProjects\\ScoutingServer\\resources\\testQrCode.png", null, 500,500);
        } catch (WriterException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            System.out.println(DataHandler.readDatabase(Constants.DEFAULT_SQL_TABLE_NAME));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @FXML
    public void debugButton2(ActionEvent event) {
    }
}
package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;
import org.tahomarobotics.scouting.scoutingserver.util.QRCodeUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;

import java.io.IOException;


public class MainController extends VBox {

    public void debugggy(ActionEvent event) {
       System.out.println("Debug button pressed");

        /*try {
            SpreadsheetUtil.initializeExcelDatabase(Constants.EXCEL_DATABASE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/

        try {
            System.out.println("Locally Cached QR Data: " + DataHandler.getCachedQRData());
            QRCodeUtil.createQRCode("5/2046/1/2/1/7/0/1/3/0/150they lost comms and tipped over. Really fast drive, can't do trap. Red card for battle bots. ljf;aldsjf;alksdjf;alksjdf;lafa;lfjkljadkljdsjfjfjfjf/ ", "C:\\Users\\Caleb\\IdeaProjects\\ScoutingServer\\src\\main\\resources\\org\\tahomarobotics\\scouting\\scoutingserver\\generatedQRCode.jpg", null,500,500);
            DataHandler.storeRawQRData(System.currentTimeMillis(), QRCodeUtil.readQRCode("C:\\Users\\Caleb\\IdeaProjects\\ScoutingServer\\src\\main\\resources\\org\\tahomarobotics\\scouting\\scoutingserver\\generatedQRCode.jpg"));
            System.out.println("Successfullly created qrcode for funsies");
        } catch (WriterException | IOException e) {
            throw new RuntimeException(e);
        } catch (NotFoundException e) {
            System.err.println("Could not read qr data");
        }
        System.out.println("QR data Cached in RAM" + DataHandler.getMatchData());
    }
    @FXML
    public void debugButton2(ActionEvent event) {
        try {
            SpreadsheetUtil.addDataRow(null, Constants.EXCEL_DATABASE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
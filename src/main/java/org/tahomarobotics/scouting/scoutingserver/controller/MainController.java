package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

;
import javafx.util.Pair;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;
import org.tahomarobotics.scouting.scoutingserver.util.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.QRCodeUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;
import org.tahomarobotics.scouting.scoutingserver.util.WebcamUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;


public class MainController extends VBox {

    public void debugggy(ActionEvent event) {
       System.out.println("Debug button pressed");
        try {
            ArrayList<Pair<String, String>> testData = new ArrayList<>();
            testData.add(new Pair<>(Constants.TEST_QR_STRING_1, System.getProperty("user.dir") + "/resources/test1.png" ));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_2, System.getProperty("user.dir") + "/resources/test2.png" ));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_3, System.getProperty("user.dir") + "/resources/test3.png" ));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_4, System.getProperty("user.dir") + "/resources/test4.png" ));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_5, System.getProperty("user.dir") + "/resources/test5.png" ));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_6, System.getProperty("user.dir") + "/resources/test6.png" ));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_7, System.getProperty("user.dir") + "/resources/test7.png" ));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_8, System.getProperty("user.dir") + "/resources/test8.png" ));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_9, System.getProperty("user.dir") + "/resources/test9.png" ));
            DatabaseManager.addTable(Constants.TEST_SQL_TABLE_NAME, DatabaseManager.createTableSchem(Constants.RAW_TABLE_SCHEMA));
            for (Pair<String, String> p : testData) {
                QRCodeUtil.createQRCode(p.getKey(), p.getValue(), null, 500, 500);
                QRScannerController.readStoredImage(p.getValue(), Constants.TEST_SQL_TABLE_NAME);
            }

        } catch (WriterException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }
    @FXML
    public void debugButton2(ActionEvent event) {
    }
}
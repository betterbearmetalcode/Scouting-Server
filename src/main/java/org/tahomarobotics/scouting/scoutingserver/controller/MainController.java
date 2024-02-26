package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.QRCodeUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ResourceBundle;


public class MainController implements Initializable {


    public ScrollPane scrollPane;

    public void debugggy(ActionEvent event) {
        System.out.println("Debug button pressed");
        try {
            ArrayList<Pair<String, String>> testData = new ArrayList<>();
            testData.add(new Pair<>(Constants.TEST_QR_STRING_1, Constants.BASE_APP_DATA_FILEPATH + "/resources/testImages/test1.png"));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_2, Constants.BASE_APP_DATA_FILEPATH + "/resources/testImages/test2.png"));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_3, Constants.BASE_APP_DATA_FILEPATH + "/resources/testImages/test3.png"));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_4, Constants.BASE_APP_DATA_FILEPATH + "/resources/testImages/test4.png"));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_5, Constants.BASE_APP_DATA_FILEPATH + "/resources/testImages/test5.png"));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_6, Constants.BASE_APP_DATA_FILEPATH + "/resources/testImages/test6.png"));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_7, Constants.BASE_APP_DATA_FILEPATH + "/resources/testImages/test7.png"));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_8, Constants.BASE_APP_DATA_FILEPATH + "/resources/testImages/test8.png"));
            testData.add(new Pair<>(Constants.TEST_QR_STRING_9, Constants.BASE_APP_DATA_FILEPATH + "/resources/testImages/test9.png"));
            SQLUtil.addTable(Constants.TEST_SQL_TABLE_NAME, SQLUtil.createTableSchem(Constants.RAW_TABLE_SCHEMA));
            for (Pair<String, String> p : testData) {
                QRCodeUtil.createQRCode(p.getKey(), p.getValue(), null, 500, 500);
                QRScannerController.readStoredImage(p.getValue(), Constants.TEST_SQL_TABLE_NAME);
            }

        } catch (WriterException | NotFoundException | IOException | SQLException e) {
            Logging.logError(e);
        }


    }

    @FXML
    public void debugButton2(ActionEvent event) {
        try {
            System.out.println("Using debug table to generate simulated JSON inputs from the Scouting App");
            JSONArray arr = new JSONArray();
            arr.put(Constants.TEST_QR_STRING_1);
            arr.put(Constants.TEST_QR_STRING_2);
            arr.put(Constants.TEST_QR_STRING_3);
            arr.put(Constants.TEST_QR_STRING_4);
            arr.put(Constants.TEST_QR_STRING_5);
            arr.put(Constants.TEST_QR_STRING_6);
            arr.put(Constants.TEST_QR_STRING_7);
            arr.put(Constants.TEST_QR_STRING_8);
            arr.put(Constants.TEST_QR_STRING_9);

            File outFile = new File(Constants.BASE_APP_DATA_FILEPATH + "/testJSON.json");
            FileOutputStream out = new FileOutputStream(outFile);
            out.write(arr.toString().getBytes());
            out.flush();
            out.close();
        } catch (IOException e) {
            Logging.logError(e, "IO exception in debug button 2");
        }
        //open tutorial
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/Tutorial/TutorialPage.html").toURI());
            } catch (IOException e) {
                Logging.logError(e);
            }
        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
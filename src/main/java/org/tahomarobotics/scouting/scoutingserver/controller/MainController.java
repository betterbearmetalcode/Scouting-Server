package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.QRCodeUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;


public class MainController extends VBox {

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
       /* try {
            System.out.println("Using debug table to generate simulated JSON inputs from the Scouting App");
            LinkedList<DatabaseManager.MatchRecord> data = DatabaseManager.readDatabase(Constants.TEST_SQL_TABLE_NAME);
            int j = 0;
            for (DatabaseManager.MatchRecord datum : data) {
                JSONArray arr = new JSONArray();
                LinkedList<Pair<String, String>> matchData = datum.getDataAsList();
                for (int i = 1; i < matchData.size(); i++) {
                    arr.put(matchData.get(i).getValue().replace("\"", ""));


                }
                File outFile = new File(Constants.BASE_APP_DATA_FILEPATH + "/testJSON" + j + ".json");
                FileOutputStream out = new FileOutputStream(outFile);
                out.write(arr.toString().getBytes());
                out.flush();
                out.close();
                j++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
        //TabController.validateData(null);
    }
}
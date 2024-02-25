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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
        //TabController.validateData(null);

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
     TextArea textArea = (TextArea) scrollPane.getContent();
     textArea.setEditable(false);
     textArea.setWrapText(true);
     textArea.setText("<!DOCTYPE html>\n" +
             "<html lang=\"\">\n" +
             "<head>\n" +
             "    <title>Scouting Server Tutorial</title>\n" +
             "</head>\n" +
             "<body>\n" +
             "\n" +
             "<h1>Scouting Server Tutorial</h1>\n" +
             "<p>The Scouting Server is a standalone windows application which recieves, processes, and exports FRC Scouting data. The Scouting client application is a cross platform app used to automate match data collection. The Scouting App outputs data in several formats which can be read by this, the Scouting Server and processed for analysis. </p>\n" +
             "<h2>Data Collection</h2>\n" +
             "<p>Clicking the data collection button brings you to the data collection screen. There you can import a json file or load previously scanned qr codes. </p>\n" +
             "<h3>JSON Imports</h3>\n" +
             "<p>A JSON file is a common file format used to transfer data between applications. For our purposes, it contains a list of data about each match which has been scouted during a competition. All data importation tools can be found in the data collection screen. Before importing any data you must first use the \"Select Target Database\" button to open the database selection wizard. There you can select a database, but also add, remove, duplicate, and rename databases. After selecting a target database, you can use the \"Import JSON File\" button to select a file and have it added to the database. Imformation regarding the success, failure, and contents of all imports is displayed in the data collection console. (No, you can't type command like command prompt :( maybe next year) </p>\n" +
             "<h3>QR Imports</h3>\n" +
             "<p>Importing QR codes is a little more advanced, but not too hard. Do import QR codes you simply have to click the button. If in settings there is a default directory selected, then the button will automatically scan all the images in that folder, but if not, then it will prompt you to select a folder. ALL the images in this folder will be scanned and added to the target database. There is more to it however, scanning qr codes with your webcam and built in camera app, if you even have a webcam, it tedious to say the least. But you can use external apps to convert a phone into a webcam and scan codes in much higher quality and with ease. Using Elgato camera hub, linked below, you can scan qr code and configure the app to send snapshots to a folder, which you could set as the import directory in the Scouting Server. I recommend this app as it is free, does not have ads, but only has a small watermark. There are other options for andriod, in fact better ones without paywalls, but I won't bother to link them. </p>\n" +
             "<a href=\"https://apps.apple.com/us/app/epoccam-webcam-for-mac-and-pc/id449133483\">IPhone App</a>\n" +
             "<a href=\"https://www.elgato.com/us/en/s/downloads\">PC App</a>\n" +
             "<h2>Database Management</h2>\n" +
             "<p>This is where the rubber meets the road. In Database management, after opening a database with the plus button you are presented with a plethora of buttons.  The data in the database is displayed in a tree with sections for each match, robot, and the data for those robots.</p>\n" +
             "<h3>Refresh and Validate Data</h3>\n" +
             "<p>The \"Refresh and Validate Data\" button is used to, quite simply, refresh and validate the data. Refreshing updated the graphical representation with the data in storage, and is necessary for you to see newly imported data. Validation is the process by which raw data is checked against blue alliance to see if it is correct. When you click the button you are prompted to select a competition to validate data against, only qualification matches are checked and it is assumed that no data has been imported for non qualification matches.   Because of the nature of the data FIRST and Blue Alliance provide it is impossible to accurately discern exactly what is wrong. If there was a way, nobody would be scouting.  What validation does do is color code groups of datapoints according to how off they are. For example, TBA provides the total number of notes an alliance scored in auto in the speaker, so the code adds up all the notes that an alliance scored in the speaker in amp compares that to the TBA total. If something is off, the whole alliance is flagged for that datapoint. Red means really off, yellow means kind of off, and green is exactly correct. The threshold between red and yellow can be configured in settings. Things like notes missed can not be checked and are marked as unknown. The highest error value for all the data-points is used as the error value for a robot entry, and the same for matches.</p>\n" +
             "<h3>Export</h3>\n" +
             "<p>This button is used to convert the data into a spreadsheet that is readable by humans. Clicking the export button will you prompt you to select a competition so it can validate the data, this is nessacary in order to export up to date trap, auto leave, and endgame data. If you attempt to export without internet you will be hit with like 3 pop ups, but you can do it, the data will just assume that nobody climbed, parked, left in auto, or scored anything in the trap. You are prompted to select a folder to export to and a xls file is saved. This app does not support exporting to xlsx, so just rename the file to the modern extension and use it. </p>\n" +
             "</body>\n" +
             "</html>\n");
    }
}
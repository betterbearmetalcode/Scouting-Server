package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.*;

import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;


public class QRScannerController {
    public static String activeTable = Constants.DEFAULT_SQL_TABLE_NAME;

    @FXML
    public Label selectedDatabaseLabel;

    public VBox imageViewBox;


    @FXML
    private void initialize() {
        selectedDatabaseLabel.setText(Constants.DEFAULT_SQL_TABLE_NAME);
        ScoutingServer.qrScannerController  = this;
    }



    public void selectTargetTable(ActionEvent event) {
        try {
            TableChooserDialog dialog = new TableChooserDialog(SQLUtil.getTableNames());
            Optional<String> result = dialog.showAndWait();
            AtomicReference<String> selectedTable = new AtomicReference<>("");
            result.ifPresent(selectedDatabaseLabel::setText);
            result.ifPresent(this::setActiveTable);
            result.ifPresent(System.out::println);
        } catch (SQLException e) {
            Logging.logError(e);
        }
    }


    @FXML
    public void importJSON(ActionEvent event) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select JSON File");
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            List<File> selectedFile = chooser.showOpenMultipleDialog(ScoutingServer.mainStage.getOwner());
            if (selectedFile != null) {
                for (File file : selectedFile) {
                    if (file.exists()) {
                        FileInputStream inputStream = new FileInputStream(file);
                        JSONArray arr = new JSONArray(new String(inputStream.readAllBytes()));
                        for (Object o : arr.toList()) {
                            DatabaseManager.storeRawQRData(System.currentTimeMillis(), (String) o, "\"" + activeTable + "\"");
                        }

                        inputStream.close();
                    }

                }
            }

        } catch (IOException e) {
            Logging.logError(e);
        }
    }


    //consider this https://www.tutorialspoint.com/java_mysql/java_mysql_quick_guide.html
    @FXML
    public void loadScannedQRCodes(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Import Directory");

        File defaultDirectory = new File(System.getProperty("user.home"));
        chooser.setInitialDirectory(defaultDirectory);

        File dir = chooser.showDialog(ScoutingServer.mainStage);
        File[] arr = dir.listFiles(pathname -> (pathname.getName().toLowerCase().endsWith(".png") || pathname.getName().toLowerCase().endsWith(".jpg")));
        if (arr != null) {
            for (File file : arr) {
                try {
                    String data = readStoredImage(Constants.QR_IAMGE_QUERY_LOCATION + file.getName(), "\"" + activeTable + "\"");
                    logScan(true, data);
                } catch (IOException e) {
                    Logging.logError(e);
                } catch (NotFoundException e) {
                    Logging.logError(e, "Failed to scan QR Code: " + file.getName());
                    logScan(false, "");
                }catch (JSONException e) {
                    Logging.logError(e, "Failed to read JSON: ");
                }
            }
        }

    }

    public static String readStoredImage(String fp, String tableName) throws IOException, NotFoundException {
        //if we have got this far in the code, than the iamge has succesfully be written to the disk

        String qrData = QRCodeUtil.readQRCode(fp);
        System.out.println("Scanner QR Code: " + qrData);
        DatabaseManager.storeRawQRData(System.currentTimeMillis(), qrData, tableName);
        return qrData;
    }


    public void setActiveTable(String s) {
        activeTable = s;
    }


    public void logScan(boolean successful, String qrData) {
        String str = successful?"Successfully scanned Qr code: " + qrData:"Failed to scan Qr code";

        System.out.println(str);

        writeToDataCollectionConsole(str, successful?Color.GREEN:Color.RED);


    }

    public void writeToDataCollectionConsole(String str, Color color) {
        Label l = new Label(str);
        l.setTextFill(color);
        imageViewBox.getChildren().add(l);
    }
    public void writeToDataCollectionConsole(String str) {
        writeToDataCollectionConsole(str, Color.BLACK);
    }

}

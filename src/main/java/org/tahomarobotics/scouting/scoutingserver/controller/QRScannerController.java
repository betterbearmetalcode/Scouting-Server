package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.*;
import org.tahomarobotics.scouting.scoutingserver.util.UI.TableChooserDialog;

import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


public class QRScannerController {
    public static String activeTable = "";

    @FXML
    public Label selectedDatabaseLabel;

    public VBox imageViewBox;

    public Button jsonImprt;

    public Button takePictureButton;

    public Button serverButton;


    @FXML
    private void initialize() {
        selectedDatabaseLabel.setText("No Database Selected");
        jsonImprt.setDisable(true);
        takePictureButton.setDisable(true);
        ScoutingServer.qrScannerController  = this;
        ServerUtil.setServerStatus(false);
    }



    public void selectTargetTable(ActionEvent event) {
        try {
            TableChooserDialog dialog = new TableChooserDialog(SQLUtil.getTableNames());
            Optional<String> result = dialog.showAndWait();
            AtomicReference<String> selectedTable = new AtomicReference<>("");
            result.ifPresent(selectedDatabaseLabel::setText);
            result.ifPresent(this::setActiveTable);
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
                        JSONObject object = new JSONObject(new String(inputStream.readAllBytes()));
                        DatabaseManager.importJSONObject(object, activeTable);
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
    public void loadCSV(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        File selectedFile = chooser.showOpenDialog(ScoutingServer.mainStage.getOwner());
        try {
            FileInputStream inputStream = new FileInputStream(selectedFile);
            String csv = new String(inputStream.readAllBytes());
            csv = csv.replaceAll("\r", "");
            JSONArray result = CDL.toJSONArray(csv);

            for (Object o : result) {
                JSONObject data = (JSONObject) o;
                try {
                    //if any of these fail, then skip the data
                    int x = data.getInt("Match #");
                    int y = data.getInt("Team #");
                    DatabaseManager.RobotPosition.valueOf(data.getString("Position"));
                }catch (Exception e) {
                    continue;
                }
                DatabaseManager.QRRecord m = new DatabaseManager.QRRecord(data.getInt("Match #"),//match num
                        data.getInt("Team #"),//team num
                        DatabaseManager.RobotPosition.valueOf(data.getString("Position")),//allinace pos
                        data.optInt("Auto Speaker", 0),//auto speaker
                        data.optInt("Auto Amp", 0),//auto amp
                        data.optInt("Auto Speaker Missed", 0),//auto speaker missed
                        data.optInt("Auto Amp Missed", 0),//auto amp missed
                        data.optInt("a", 0),//F1
                        data.optInt("b", 0),//F2
                        data.optInt("c", 0),//F3
                        data.optInt("1", 0),//M1
                        data.optInt("2", 0),//M2
                        data.optInt("3", 0),//M3
                        data.optInt("4", 0),//M4
                        data.optInt("5", 0),//M5
                        data.optInt("Tele Speaker", 0),//tele speaker
                        data.optInt("Tele Amp", 0),//tele amp
                        data.optInt("Tele Trap", 0),//tele trap
                        data.optInt("Tele Speaker Missed", 0),//tele speakermissed
                        data.optInt("Tele Amp missed", 0),//tele amp missed
                        data.optInt("Lost Comms", 0),//auto notes
                        data.optString("Tele Comments", "No Comments"));//tele notes
                DatabaseManager.storeQrRecord(m, activeTable);
            }

            inputStream.close();
        } catch (IOException e) {
            Logging.logError(e);
        }


    }

    public static String readStoredImage(String fp, String tableName) throws IOException, NotFoundException {
        //if we have got this far in the code, than the iamge has succesfully be written to the disk

        String qrData = QRCodeUtil.readQRCode(fp);
        System.out.println("Scanner QR Code: " + qrData);
        DatabaseManager.storeRawQRData( qrData, tableName);
        return qrData;
    }

    @FXML
    public void toggleServerStatus(ActionEvent event) {
        ServerUtil.setServerStatus(!ServerUtil.isServerThreadRunning());
        if (ServerUtil.isServerThreadRunning()) {
            serverButton.setText("Stop Server");
        }else {
            serverButton.setText("Start Data Transfer Server");
        }
    }

    public void setActiveTable(String s) {
        activeTable = s;
        jsonImprt.setDisable(false);
        takePictureButton.setDisable(false);
        serverButton.setDisable(false);

    }


    public void logScan(boolean successful, String qrData) {
        String str = successful?"Successfully scanned Qr code: " + qrData:"Failed to scan Qr code";
        Logging.logInfo("tried to scan qr code: succesful=" + successful);

        writeToDataCollectionConsole(str, successful?Color.GREEN:Color.RED);


    }

    public void writeToDataCollectionConsole(String str, Color color) {
        Platform.runLater(() -> {
            Label l = new Label(str);
            l.setTextFill(color);
            imageViewBox.getChildren().add(l);
        });

    }



}

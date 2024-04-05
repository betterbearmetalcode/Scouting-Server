package org.tahomarobotics.scouting.scoutingserver.controller;

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
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.*;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DuplicateDataResolvedDialog;
import org.tahomarobotics.scouting.scoutingserver.util.UI.TableChooserDialog;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
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
            importJSONFiles(chooser.showOpenMultipleDialog(ScoutingServer.mainStage.getOwner()));

        } catch (IOException e) {
            Logging.logError(e);
        }

    }

    private void importJSONFiles(List<File> files) throws IOException {
        if (files != null) {

            for (File file : files) {
                if (file.exists()) {
                    FileInputStream inputStream = new FileInputStream(file);
                    JSONObject object = new JSONObject(new String(inputStream.readAllBytes()));
                    ArrayList<DuplicateDataException> duplicates = DatabaseManager.importJSONObject(object, activeTable);
                    handleDuplicates(duplicates);
                    inputStream.close();
                }

            }
        }
    }




    //consider this https://www.tutorialspoint.com/java_mysql/java_mysql_quick_guide.html
    @FXML
    public void loadCSV(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        File selectedFile = chooser.showOpenDialog(ScoutingServer.mainStage.getOwner());
        try {
            if (selectedFile == null) {
                return;
            }
            FileInputStream inputStream = new FileInputStream(selectedFile);
            String csv = new String(inputStream.readAllBytes());
            csv = csv.replaceAll("\r", "");
            JSONArray result = CDL.toJSONArray(csv);
            ArrayList<DuplicateDataException> duplicates = new ArrayList<>();
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
                        data.optInt("noteA", 0),//F1
                        data.optInt("noteB", 0),//F2
                        data.optInt("noteC", 0),//F3
                        data.optInt("1", 0),//M1
                        data.optInt("2", 0),//M2
                        data.optInt("3", 0),//M3
                        data.optInt("4", 0),//M4
                        data.optInt("5", 0),//M5
                        data.optInt("A-Stop", 0),
                        data.optInt("Shuttled", 0),//shuttled notes
                        data.optInt("Tele Speaker", 0),//tele speaker
                        data.optInt("Tele Amp", 0),//tele amp
                        data.optInt("Tele Trap", 0),//tele trap
                        data.optInt("Tele Speaker Missed", 0),//tele speakermissed
                        data.optInt("Tele Amp missed", 0),//tele amp missed
                        data.optInt("Lost Comms", 0),//auto notes
                        data.optString("Tele Comments", "No Comments"));//tele notes
                try {
                    DatabaseManager.storeQrRecord(m, activeTable);
                } catch (DuplicateDataException e) {
                    duplicates.add(e);
                }
            }
            handleDuplicates(duplicates);

            inputStream.close();
        } catch (IOException e) {
            Logging.logError(e);
        }
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

    @FXML
    public void importDuplicateDataBackup(ActionEvent event) {
        Logging.logInfo("Importing duplicate data backup");
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(Constants.BASE_APP_DATA_FILEPATH + "/resources/duplicateDataBackups"));
        chooser.setTitle("Select Backup to import");
        try {
            importJSONFiles(chooser.showOpenMultipleDialog(ScoutingServer.mainStage.getOwner()));
        } catch (IOException e) {
            Logging.logError(e);
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

    @FXML
    public void clearConsole(ActionEvent event) {
        Logging.logInfo("Clearing data collection console");
        imageViewBox.getChildren().clear();

    }
    public void writeToDataCollectionConsole(String str, Color color) {
        Platform.runLater(() -> {
            Label l = new Label(str);
            l.setTextFill(color);
            imageViewBox.getChildren().add(l);
        });

    }

    public static void handleDuplicates(ArrayList<DuplicateDataException> duplicates) {
        if (duplicates.isEmpty()) {
            return;
        }
        Platform.runLater(() -> {
            //this method has to use noteA duplicate data handler to go through all the duplicates and generate noteA list of records which should be added
            //then for each of these records, all the ones in the database that have the same match and team number are deleted and re added
            DuplicateDataResolvedDialog dialog = new DuplicateDataResolvedDialog(duplicates);
            Optional<ArrayList<DatabaseManager.QRRecord>> recordToAdd = dialog.showAndWait();
            recordToAdd.ifPresent(qrRecords -> {
                for (DatabaseManager.QRRecord qrRecord : qrRecords) {
                    //first delete the old record from the database that caused the duplicate then add the one we want to add
                    try {
                        SQLUtil.execNoReturn("DELETE FROM \"" + activeTable + "\" WHERE " + Constants.SQLColumnName.TEAM_NUM + "=? AND " + Constants.SQLColumnName.MATCH_NUM + "=?", new Object[]{String.valueOf(qrRecord.teamNumber()), String.valueOf(qrRecord.matchNumber())}, true);
                    } catch (SQLException | DuplicateDataException e) {
                        Logging.logError(e);
                    }
                    try {
                        DatabaseManager.storeQrRecord(qrRecord, activeTable);
                    } catch (DuplicateDataException e) {
                        Logging.logInfo("Gosh dang it, got duplicate data again after trying to resolve duplicate data, just giving up now", true);
                    }

                }
            });
        });


    }



}

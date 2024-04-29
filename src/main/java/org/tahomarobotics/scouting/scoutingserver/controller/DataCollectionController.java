package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.ServerUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DuplicateDataResolvedDialog;
import org.tahomarobotics.scouting.scoutingserver.util.UI.TableChooserDialog;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;


public class DataCollectionController {
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
        ScoutingServer.dataCollectionController = this;
        ServerUtil.setServerStatus(false);
    }



    @FXML
    public void selectTargetTable(ActionEvent event) {
        try {
            TableChooserDialog dialog = new TableChooserDialog(SQLUtil.getTableNames());
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(selectedDatabaseLabel::setText);
            result.ifPresent(this::setActiveTable);
        } catch (SQLException e) {
            Logging.logError(e);
        }
    }


    //this is an event handler
    @FXML
    public void importJSON(ActionEvent event) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select JSON File");
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            importJSONFile(chooser.showOpenDialog(ScoutingServer.mainStage.getOwner()));


        } catch (IOException e) {
            Logging.logError(e);
        }

    }

    private void importJSONFile(File selectedFile) throws IOException {
        if (selectedFile == null) {
            return;
        }
        if (!selectedFile.exists()) {
            return;
        }
        FileInputStream inputStream = new FileInputStream(selectedFile);
        JSONArray object = new JSONArray(new String(inputStream.readAllBytes()));
        ArrayList<DuplicateDataException> duplicates = DatabaseManager.importJSONArrayOfDataObjects(object, activeTable);
        handleDuplicates(duplicates);
        inputStream.close();
    }



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
            Configuration.updateConfiguration();
            ArrayList<DuplicateDataException> duplicates = new ArrayList<>();
            for (Object o : result) {
                JSONObject rawData = (JSONObject) o;
                //check if the data is valid
                try {
                    //if any of these fail, then skip the data
                    int x = rawData.getInt(Constants.SQLColumnName.MATCH_NUM.toString());
                    int y = rawData.getInt(Constants.SQLColumnName.TEAM_NUM.toString());
                    DatabaseManager.RobotPosition.valueOf(rawData.getString(Constants.SQLColumnName.ALLIANCE_POS.toString()));
                }catch (Exception e) {
                    continue;
                }
                //convert to standard JSON format
                JSONObject dataToImport = new JSONObject();
                for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
                    //if it can't parse anything the set a default that no one will ever enter so that we can detect if we need to use the default for string datatypes
                    //and seriously, if the following was really what the scout said, then we might as well use the default...
                    String weirdDefault = "deeeeeeeeeeeeeeeeedddffeeeeeeeeefault!!";
                    String csvDatum = rawData.optString(rawDataMetric.getName(), weirdDefault);//if the csv data has something for this, metric, then good, otherwise nothing, sort out default in a sec
                    //in the csv templates the alliance positions are put in as letters to be human readable
                    if (Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.ALLIANCE_POS.name())) {
                        csvDatum = String.valueOf(DatabaseManager.RobotPosition.valueOf(csvDatum).ordinal());
                    }
                    JSONObject finalDataCarryingaObject = new JSONObject();
                    //then we have something
                    switch (rawDataMetric.getDatatype()) {

                        case INTEGER -> {
                            try {
                                finalDataCarryingaObject.put("0", Integer.parseInt(csvDatum));
                            }catch (NumberFormatException e) {
                                finalDataCarryingaObject.put("0", rawDataMetric.getDefaultValue());
                            }

                        }
                        case STRING -> {
                            if (!Objects.equals(csvDatum, weirdDefault)) {
                                finalDataCarryingaObject.put("1", csvDatum);
                            }else {
                                finalDataCarryingaObject.put("1", rawDataMetric.getDefaultValue());
                            }

                        }
                        case BOOLEAN -> {
                            try {
                                int i = Integer.parseInt(csvDatum);
                                if ((i != 0) && (i != 1)) {
                                    //then we have a non boolean value so go the catch block
                                    throw new NumberFormatException();
                                }
                                finalDataCarryingaObject.put("2", i);
                            }catch (NumberFormatException e) {
                                //use default
                                finalDataCarryingaObject.put("2", rawDataMetric.getDefaultValue());
                            }

                        }
                    }
                    dataToImport.put(rawDataMetric.getName(), finalDataCarryingaObject);
                }
                try {
                    SQLUtil.execNoReturn(DatabaseManager.getSQLStatementFromJSONJata(dataToImport, activeTable));
                } catch (DuplicateDataException e) {
                    duplicates.add(e);
                } catch (SQLException e) {
                    Logging.logError(e, "SQL Exception: ");
                }
            }
            handleDuplicates(duplicates);

            inputStream.close();
        } catch (IOException | ConfigFileFormatException e) {
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
            importJSONFile(chooser.showOpenDialog(ScoutingServer.mainStage.getOwner()));
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
                    } catch (SQLException e) {
                        Logging.logError(e);
                    }

                }
            });
        });


    }



}

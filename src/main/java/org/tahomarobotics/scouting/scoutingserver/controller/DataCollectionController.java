package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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



public class DataCollectionController {
    public static String activeTable = "";

    @FXML
    public Label selectedDatabaseLabel;

    public VBox imageViewBox;

    public Button jsonImprt;

    public Button takePictureButton;

    public Button serverButton;


    private void initialize() {
        selectedDatabaseLabel.setText("No Database Selected");
        jsonImprt.setDisable(true);
        takePictureButton.setDisable(true);
        ScoutingServer.dataCollectionController = this;
        ServerUtil.setServerStatus(false);
    }


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
                    SQLUtil.execNoReturn(DatabaseManager.getValuesStatementFromJSONJata(dataToImport));
                } catch (DuplicateDataException e) {
                    duplicates.add(e);
                } catch (SQLException e) {
                    Logging.logError(e, "SQL Exception: ");
                }
            }

            inputStream.close();
        } catch (IOException | ConfigFileFormatException e) {
            Logging.logError(e);
        }
    }



    public void toggleServerStatus(ActionEvent event) {
        ServerUtil.setServerStatus(!ServerUtil.isServerThreadRunning());
        if (ServerUtil.isServerThreadRunning()) {
            serverButton.setText("Stop Server");
        }else {
            serverButton.setText("Start Data Transfer Server");
        }
    }

/*    public void importDuplicateDataBackup(ActionEvent event) {
        Logging.logInfo("Importing duplicate data backup");
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(Constants.BASE_APP_DATA_FILEPATH + "/resources/duplicateDataBackups"));
        chooser.setTitle("Select Backup to import");
        try {
            importJSONFile(chooser.showOpenDialog(ScoutingServer.mainStage.getOwner()));
        } catch (IOException e) {
            Logging.logError(e);
        }

    }*/

    public void setActiveTable(String s) {
        activeTable = s;
        jsonImprt.setDisable(false);
        takePictureButton.setDisable(false);
        serverButton.setDisable(false);

    }



    public void clearConsole(ActionEvent event) {
        Logging.logInfo("Clearing data collection console");
        imageViewBox.getChildren().clear();

    }





}

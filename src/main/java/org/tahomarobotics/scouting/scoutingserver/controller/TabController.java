package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import javafx.util.converter.DefaultStringConverter;
import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.Exporter;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DataValidationCompetitionChooser;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.OperationAbortedByUserException;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TabController extends TreeView<String> {



    public String tableName;


    private TreeItem<String> rootItem;
    private JSONArray eventList;

    private String currentEventCode = "";

    private Optional<JSONArray> tbaDataOptional = Optional.empty();


    public TabController(String table) {
        tableName = table;

        Logging.logInfo("Initializing Tab Controller: " + tableName);
        //init data stuff
        rootItem = new TreeItem<>("root-item");

        this.setEditable(true);
        this.setShowRoot(false);
        this.setRoot(rootItem);
        this.setCellFactory(param -> new EditableTreeCell());
        //init list of events
        try {
            FileInputStream stream = new FileInputStream(Constants.BASE_READ_ONLY_FILEPATH + "/resources/TBAData/eventList.json");
            eventList = new JSONArray(new String(stream.readAllBytes()));
            stream.close();
        } catch (FileNotFoundException e) {
            Logging.logError(e, "Could Not Find list of Competitions");
        } catch (IOException e) {
            Logging.logError(e, "Whooop de doo, another IO exception, I guess your just screwed now,-C.H");
        }

        updateDisplay(false);
    }


    @FXML
    public void export(Event event) {
        Logging.logInfo("Exporting");
        //first gather the nessacary data from tba to export and check to make sure
        //everything is good and ready before showing the user a save dialog
        //save dialog should be the last dialog becuase in the user's mind the file is already created when that dialog comes up



        //gather TBA data
        //first figure out which competition we are at
        if ("".equals(currentEventCode)) {
            //if no event code has been selected
            if (selectCompetition()) {
                //if the selection fails or is canceled etc
                Logging.logInfo("Export Aborted");
                return;
            }
        }
        //gather TBA data and prepare a list of teams who are at the comp
        Exporter exporter;
        try {
            exporter = new Exporter(currentEventCode, tableName);
        } catch (IOException  | InterruptedException e) {
            Logging.logError(e, "Failed to construct exporter like while executing API requests, aborting export");
            return;
        }catch (OperationAbortedByUserException e) {
            Logging.logInfo("export cancelled by user because there is no internet");
            return;
        } catch (SQLException e) {
            Logging.logError(e, "Failed to select teams from database, aborting export");
            return;
        }
        //code here  will only run if exporter was successfully initialized
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        try {
            data = exporter.export();
        } catch (SQLException e) {
            Logging.logError(e, "failed to generate exported data");
        }

        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(Constants.DATABASE_FILEPATH));
        chooser.setTitle("Select Export Location");
        chooser.setInitialFileName("export " + new Date(System.currentTimeMillis()).toString().replaceAll(":", "-"));
        chooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter("Excel Files", ".xlsx"));


        File file = chooser.showSaveDialog(ScoutingServer.mainStage.getOwner());
        if (file != null) {
            try {

                SpreadsheetUtil.writeArrayToSpreadsheet(data, file);
            } catch (IOException ex) {
                Logging.logError(ex, "IO error while writing to spreadsheet");
            }
        }else {
            Logging.logInfo("Export Aborted");
        }
    }

    @FXML
    public void expandAll(Event e) {
        Logging.logInfo("Expanding Tree");
        setExpansionAll(rootItem, true);

    }

    @FXML
    public void collapseAll() {
        Logging.logInfo("Collapsing Tree");
        setExpansionAll(rootItem, false);
    }


    @FXML
    public void validateDataButtonHandler(ActionEvent event) {
        if (tbaDataOptional.isEmpty()) {
            Logging.logInfo("No TBA Data, so cannont validate. Click update to update.", true);
            return;
        }
        updateDisplay(true);
    }

    @FXML
    public void clearDatabase() {
        if (!Constants.askQuestion("Are you sure you want to clear database " + tableName + "?")) {
            return;
        }
        Logging.logInfo("Clearing databse: " + tableName);
        try {
            SQLUtil.execNoReturn("DELETE FROM \"" + tableName + "\"");
        } catch (SQLException | DuplicateDataException e) {
            Logging.logError(e);
        }
        rootItem.getChildren().clear();
        updateDisplay(false);
    }

    @FXML
    public void updateTBAData() {
        Logging.logInfo("UpdatingTBAData");
        if (selectCompetition()) {
            tbaDataOptional = Optional.empty();
            return;
        }
        tbaDataOptional = APIUtil.getEventMatches(currentEventCode);

    }
    @FXML
    public void saveJSONBackup(ActionEvent event) {
        Logging.logInfo("Making JSON Backup of " + tableName);

        try {
            JSONArray dataArray = DatabaseManager.readDatabaseNew(tableName);
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Backup");
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
            chooser.setInitialFileName("Backup " + new Date().toString().replaceAll(":", " ") + ".json");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", ".json"));
            File selectedFile = chooser.showSaveDialog(ScoutingServer.mainStage.getOwner());
            if (selectedFile == null) {
                return;
            }
            if (!selectedFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                selectedFile.createNewFile();
            }
            FileOutputStream os = new FileOutputStream(selectedFile);
            // os.write(output.toString(1).getBytes());
            os.write(dataArray.toString(1).getBytes());
            os.flush();
            os.close();


        } catch (IOException | SQLException | ConfigFileFormatException  e) {
            Logging.logError(e, "Failed to save backup");
        }

    }



    public void updateDisplay(boolean validateData) {
        Logging.logInfo("Updating database: " + tableName);
        try {
            Configuration.updateConfiguration();
        } catch (ConfigFileFormatException e) {
            Logging.logError(e);
        }
        if (validateData && tbaDataOptional.isEmpty()) {
            Logging.logInfo("Cannont validate with no TBA Data. ", true);
            validateData = false;
        }
        try {
            JSONArray data = DatabaseManager.readDatabaseNew(tableName, true);
            //construct a array containing the expansion structure of the database, if there are changes (exceptions), then we can defualt the expansionto false
            ArrayList<Pair<Boolean, ArrayList<Boolean>>> expainsionStructure = new ArrayList<>();
            for (TreeItem<String> matchItem : rootItem.getChildren()) {
                ArrayList<Boolean> matchExpansion = new ArrayList<>();
                for (TreeItem<String> robotItem : matchItem.getChildren()) {
                    matchExpansion.add(robotItem.isExpanded());
                }
                expainsionStructure.add(new Pair<>(matchItem.isExpanded(), matchExpansion));
            }
            rootItem.getChildren().clear();
            if (data.isEmpty()) {
                rootItem.getChildren().add(new TreeItem<>("No Data..."));
                return;
            }
            //for validation
            boolean checkTeamNumbers = true;
            //for each json object representing a entry in the database, sorted
            int arrayIndex = 0;
            int lastMatch = getIntFromEntryObject(Constants.SQLColumnName.MATCH_NUM, (HashMap<String, Object>)data.get(data.length() - 1));
            for (int matchNum = 1; matchNum <=  lastMatch; matchNum++) {
                //iterates through all the matches that  we have. There could be some matches that have no data if there is a random entry with a really high match number.
                //any entry with a match number below 1 will be ingnored, becuase it must be a mistake at that point.
                ArrayList<HashMap<String, Object>> dataForThisMatch = getDataForMatch(matchNum, data);

                if (dataForThisMatch.isEmpty()) {
                    continue;
                }
                //check that the team numbers in this match correspond with the match schdeule on TBA
                Optional<HashMap<String, Object>> redAllianceBreakdown = Optional.empty();
                Optional<HashMap<String, Object>> blueAllianceBreakdown = Optional.empty();
                if (validateData) {

                    int tempMatchNum = matchNum;
                    Optional<Object> matchTBAData =  new ArrayList<>(tbaDataOptional.get().toList()).stream().filter(o -> {
                        HashMap<String, Object> dataum = (HashMap<String, Object>) o;
                        String exptectedMatchKey = currentEventCode + "_qm"  + tempMatchNum;
                        return dataum.get("key").equals(exptectedMatchKey);
                    }).findFirst();


                    if (matchTBAData.isEmpty()) {
                        validateData = false;
                    }else  {
                        //now and only now are we able to validate this match
                        HashMap<String, Object> matchDatum =(HashMap<String, Object>) matchTBAData.get();
                        //get TBA match score breakdowns for each alliance
                        HashMap<String, Object> matchScoreBreakdown = (HashMap<String, Object>) matchDatum.get("score_breakdown");
                        redAllianceBreakdown  = Optional.of((HashMap<String, Object>) matchScoreBreakdown.get("red"));
                        blueAllianceBreakdown = Optional.of((HashMap<String, Object>) matchScoreBreakdown.get("blue"));
                        if (checkTeamNumbers) {
                            checkTeamNumbers = checkTeamNumbersForMatch(matchNum, matchDatum, dataForThisMatch);
                        }
                    }

                }

                //declare the match item
                TreeItem<String> matchItem = new TreeItem<>();
                try {
                    matchItem.setExpanded(expainsionStructure.get(matchNum - 1).getKey());
                }catch (IndexOutOfBoundsException e) {
                    matchItem.setExpanded(false);
                }



                ArrayList<HashMap<String, Object>> redAllianceData = dataForThisMatch.stream().filter(map -> getIntFromEntryObject(Constants.SQLColumnName.ALLIANCE_POS, map) < 3).collect(Collectors.toCollection(ArrayList::new));
                ArrayList<HashMap<String, Object>> blueAllianceData = dataForThisMatch.stream().filter(map -> getIntFromEntryObject(Constants.SQLColumnName.ALLIANCE_POS, map) > 2).collect(Collectors.toCollection(ArrayList::new));
                Optional<Integer> redError = addAlliance(redAllianceData, matchItem, redAllianceBreakdown);
                Optional<Integer> blueError = addAlliance(blueAllianceData, matchItem, blueAllianceBreakdown);
                Optional<Integer> entryError = Optional.empty();
                if (redError.isPresent() && blueError.isPresent()) {
                    entryError = Optional.of(Math.max(redError.get(), blueError.get()));
                }else if (redError.isPresent()) {
                    entryError = redError;
                }else if (blueError.isPresent()) {
                    entryError = blueError;
                }

                String err = "?";
                if (entryError.isPresent()) {
                    err = String.valueOf(entryError.get());
                }
                matchItem.setValue("Match: " + matchNum + " Error=" + err);
                rootItem.getChildren().add(matchItem);
            }//match num loop


        } catch (SQLException | ConfigFileFormatException e) {
            Logging.logError(e);
        }
    }

    //checks team numbers and notifys user of problems
    private boolean checkTeamNumbersForMatch(int matchNum, HashMap<String, Object> matchDatum, ArrayList<HashMap<String, Object>> scoutingDataForThisMatch) {
        //check if the teams we have for this match line up with what tba says to look for scouting mistakes
        //get a list of team numbers correlated with the robot positions.
        //doing these with seperate try catch blocks because some parts of TBA data can be updated at different times



        try {
            //get team objects
            HashMap<String, Object> allianceMap =  (HashMap<String, Object>) matchDatum.getOrDefault("alliances", null);
            HashMap<String, Object> blueAllianceTeams = (HashMap<String, Object>) allianceMap.getOrDefault("blue", null);
            HashMap<String, Object> redAllianceTeams = (HashMap<String, Object>) allianceMap.getOrDefault("red", null);

            //get array of teams
            ArrayList<String> blueTeams = (ArrayList<String>) blueAllianceTeams.getOrDefault("team_keys", null);
            ArrayList<String> teamKeys = ((ArrayList<String>) redAllianceTeams.getOrDefault("team_keys", null));
            teamKeys.addAll(blueTeams);
            ArrayList<String> teamNums =new ArrayList<>();
            teamKeys.forEach(s -> teamNums.add(s.split("frc")[1]));
            //get map of correct team numbers and robot positions
            HashMap<Integer, DatabaseManager.RobotPosition> correctMatchConfiguration = new HashMap<>();
            if (teamNums.size() != 6) {
                Logging.logInfo("Aborting team validation for this match because TBA data is corrupt", true);
                return true;
            }
            for (DatabaseManager.RobotPosition robotPosition : DatabaseManager.RobotPosition.values()) {
                correctMatchConfiguration.put(Integer.parseInt(teamNums.get(robotPosition.ordinal())), robotPosition);
            }

            //check the scouting data against this data
            for (int teamNum : correctMatchConfiguration.keySet()) {
                DatabaseManager.RobotPosition correctRobotPosition = correctMatchConfiguration.get(teamNum);
                List<HashMap<String, Object>> scoutingDataForThisPosition = scoutingDataForThisMatch.stream().filter(map -> getIntFromEntryObject(Constants.SQLColumnName.ALLIANCE_POS, map) == correctRobotPosition.ordinal()).toList();
                if (scoutingDataForThisPosition.size() == 1) {
                    //then there is only one robot entered for this position
                    if (getIntFromEntryObject(Constants.SQLColumnName.TEAM_NUM, scoutingDataForThisPosition.get(0)) != teamNum) {
                        //then the scouting data has the incorrect team number
                        //notify user and if they want to stop checking team numbers
                        if (!Constants.askQuestion("Match: " + matchNum + " Position: " + correctRobotPosition.name() + " has the incorrect team entered, continue checking team numbers?")) {
                            return false;
                        }
                    }
                }else if (scoutingDataForThisPosition.size() > 1) {
                    //then there are multiple teams entered for the position
                    Logging.logInfo("Multiple teams are entered for Match: " + matchNum + " Position: " + correctRobotPosition, true);
                }else {
                    //there is no scouting data for this team on TBA
                    if (!Constants.askQuestion("Match: " + matchNum + " Position: " + correctRobotPosition.name() + " has no data, continue checking team numbers?")) {
                        return  false;
                    }
                }
            }//end for each team in this match

        }catch (NullPointerException | ClassCastException e) {
            //there is no breakdown or something went wrong so skip validation
            Logging.logError(e, " skipping team validation for this match becuase TBA is not updated");
        }
        return true;
    }





    //adds data for an alliance to a match item and validates it if need be
    //this method will need to be re-written each year
    //returns max error found
    private Optional<Integer> addAlliance(ArrayList<HashMap<String, Object>> scoutingData, TreeItem<String> matchItem, Optional<HashMap<String, Object>> breakdownOptinal) {
        Optional<Integer> maxErrorOfThisAlliance = Optional.empty();
        String autoSpeakerError = "?";
        String autoAmpError = "?";
        String teleSpeakerError = "?";
        String teleAmpError = "?";
        if (breakdownOptinal.isPresent() && (scoutingData.size() == 3)) {
            maxErrorOfThisAlliance = Optional.of(0);
            //if we don't have tba data for this match or a full alliance, then we can't validate
            HashMap<String, Object> breakdown = breakdownOptinal.get();

            int autoSpeakerTrue = (int) breakdown.get("autoSpeakerNoteCount");

            int autoAmpTrue = (int) breakdown.get("autoAmpNoteCount");
            int teleSpeakerTrue = ((int) breakdown.get("teleopSpeakerNoteAmplifiedCount")) + ((int) breakdown.get("teleopSpeakerNoteCount"));
            int teleAmpTrue = (int) breakdown.get("teleopAmpNoteCount");
            int autoSpeakerMeasured =
                    getIntFromEntryObject(Constants.SQLColumnName.AUTO_SPEAKER, scoutingData.get(0)) +
                    getIntFromEntryObject(Constants.SQLColumnName.AUTO_SPEAKER, scoutingData.get(1)) +
                    getIntFromEntryObject(Constants.SQLColumnName.AUTO_SPEAKER, scoutingData.get(2));

            int autoAmpMeasured =
                    getIntFromEntryObject(Constants.SQLColumnName.AUTO_AMP, scoutingData.get(0)) +
                    getIntFromEntryObject(Constants.SQLColumnName.AUTO_AMP, scoutingData.get(1)) +
                    getIntFromEntryObject(Constants.SQLColumnName.AUTO_AMP, scoutingData.get(2));


            int teleSpeakerMeasured =
                    getIntFromEntryObject(Constants.SQLColumnName.TELE_SPEAKER, scoutingData.get(0)) +
                    getIntFromEntryObject(Constants.SQLColumnName.TELE_SPEAKER, scoutingData.get(1)) +
                    getIntFromEntryObject(Constants.SQLColumnName.TELE_SPEAKER, scoutingData.get(2)) +
                    getIntFromEntryObject(Constants.SQLColumnName.SPEAKER_RECEIVED, scoutingData.get(0)) +
                    getIntFromEntryObject(Constants.SQLColumnName.SPEAKER_RECEIVED, scoutingData.get(1)) +
                    getIntFromEntryObject(Constants.SQLColumnName.SPEAKER_RECEIVED, scoutingData.get(2));

            int teleAmpMeasured =
                    getIntFromEntryObject(Constants.SQLColumnName.TELE_AMP, scoutingData.get(0)) +
                    getIntFromEntryObject(Constants.SQLColumnName.TELE_AMP, scoutingData.get(1)) +
                    getIntFromEntryObject(Constants.SQLColumnName.TELE_AMP, scoutingData.get(2)) +
                    getIntFromEntryObject(Constants.SQLColumnName.AMP_RECEIVED, scoutingData.get(0)) +
                    getIntFromEntryObject(Constants.SQLColumnName.AMP_RECEIVED, scoutingData.get(1)) +
                    getIntFromEntryObject(Constants.SQLColumnName.AMP_RECEIVED, scoutingData.get(2));


            autoSpeakerError = String.valueOf(autoSpeakerMeasured - autoSpeakerTrue);
            autoAmpError = String.valueOf(autoAmpMeasured - autoAmpTrue);
            teleSpeakerError = String.valueOf(teleSpeakerMeasured - teleSpeakerTrue);
            teleAmpError = String.valueOf(teleAmpMeasured - teleAmpTrue);
            maxErrorOfThisAlliance = Optional.of(findABSMax((autoSpeakerMeasured - autoSpeakerTrue),
                    (autoAmpMeasured - autoAmpTrue),
                    (teleSpeakerMeasured - teleSpeakerTrue),
                    (teleAmpMeasured - teleAmpTrue)));
        }

        //here use the error values calulated above and set them below
        for (HashMap<String, Object> scoutingDatum : scoutingData) {
            DatabaseManager.RobotPosition robotPosition = DatabaseManager.RobotPosition.values()[getIntFromEntryObject(Constants.SQLColumnName.ALLIANCE_POS, scoutingDatum)];
            int teamNum = getIntFromEntryObject(Constants.SQLColumnName.TEAM_NUM, scoutingDatum);
            //declare robot position item
            String err = "?";
            if (maxErrorOfThisAlliance.isPresent()) {
                err = String.valueOf(maxErrorOfThisAlliance.get());
            }
            TreeItem<String> robotPositionItem = new TreeItem<>(robotPosition.name()  + ": " + teamNum + " Error=" + err);
            robotPositionItem.setExpanded(false);
            //add all the data to the robot position item, only consider raw data metrics that we care about
            for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
                String error = "?";

                //some things are inherently correct
                if (Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.MATCH_NUM.name()) || Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.TEAM_NUM.name()) || Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.ALLIANCE_POS.name()) || Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.TELE_COMMENTS.name())) {
                    error = "0";
                } else if (Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.AUTO_SPEAKER.name())) {
                    error = autoSpeakerError;
                } else if (Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.AUTO_AMP.name())) {
                    error = autoAmpError;
                } else if (Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.TELE_SPEAKER.name())) {
                    error = teleSpeakerError;
                } else if (Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.TELE_AMP.name())) {
                    error = teleAmpError;
                } else if (Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.SPEAKER_RECEIVED.name())) {
                    error = teleSpeakerError;
                } else if (Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.AMP_RECEIVED.name())) {
                    error = autoAmpError;
                }
                TreeItem<String> dataPointItem = new TreeItem<>(rawDataMetric.getName()
                        + ":" +
                        ((HashMap<String, Object>) scoutingDatum.get(rawDataMetric.getName())).get(String.valueOf(rawDataMetric.getDatatype().ordinal())).toString()
                        + ":Error=" + error);
                robotPositionItem.getChildren().add(dataPointItem);;
            }
            matchItem.getChildren().add(robotPositionItem);
        }
        return maxErrorOfThisAlliance;

    }

    public boolean selectCompetition() {
        Logging.logInfo("asking user to select which competition they are at");
        DataValidationCompetitionChooser chooser = new DataValidationCompetitionChooser();
        Optional<String> result = chooser.showAndWait();
        AtomicReference<String> selectedEvent = new AtomicReference<>("");
        result.ifPresent(selectedEvent::set);
        currentEventCode = selectedEvent.get();
        return Objects.equals(currentEventCode, "");

    }

    private void setExpansionAll(TreeItem<String> treeItem, boolean val) {
        if (treeItem.getValue().equals("root-item")) {
            //then we are dealing with the root item
            treeItem.setExpanded(true);
        } else {
            treeItem.setExpanded(val);

        }
        if (!treeItem.getChildren().isEmpty()) {
            for (TreeItem<String> t : treeItem.getChildren()) {
                setExpansionAll(t, val);
            }
        }


    }



    private class EditableTreeCell extends TextFieldTreeCell<String> {

        private String oldStringBeingEdited = "";
        private final ContextMenu menu = new ContextMenu();
        public EditableTreeCell() {
            super(new DefaultStringConverter());
            MenuItem editItem = new MenuItem("Edit");
            MenuItem deleteItem = new MenuItem("Delete");
            menu.getItems().addAll(editItem, deleteItem);

            editItem.setOnAction(arg0 -> startEdit());
            deleteItem.setOnAction(event -> {
                if (!Constants.askQuestion("Are you sure you want to delete this?")) {
                    return;
                }
                if (getTreeItem().getParent() == rootItem) {
                    //then this is a match item
                    for (TreeItem<String> positionItem : getTreeItem().getChildren()) {
                        deletePositionItem(positionItem);
                    }
                }else if (getTreeItem().isLeaf()){
                    deletePositionItem(getTreeItem().getParent());
                }else {
                    deletePositionItem(getTreeItem());
                }
            });
        }

        private void deletePositionItem(TreeItem<String> positionItem) {
            String teamNum = positionItem.getValue().split(" ")[1];

            String match = positionItem.getParent().getValue().split(" ")[1];
            try {
                SQLUtil.execNoReturn("DELETE FROM \"" + tableName + "\" WHERE " + Constants.SQLColumnName.MATCH_NUM.name() + "=? AND " + Constants.SQLColumnName.TEAM_NUM.name() + "=?", new Object[]{match, teamNum}, true);

            } catch (SQLException | DuplicateDataException e) {
                Logging.logError(e, "Failed to delete datapoint");
            }
        }


        @Override
        public void startEdit() {
            if (!this.getTreeItem().isLeaf()) {
                super.cancelEdit();
            }else {
                Optional<DataMetric> dataMetricOptinal = Configuration.getMetric(getText().split(":")[0]);
                if (dataMetricOptinal.isEmpty()) {
                    cancelEdit();
                }else if ((Objects.equals(dataMetricOptinal.get().getName(), Constants.SQLColumnName.MATCH_NUM.name())) || (Objects.equals(dataMetricOptinal.get().getName(), Constants.SQLColumnName.TEAM_NUM.name())) || (Objects.equals(dataMetricOptinal.get().getName(), Constants.SQLColumnName.ALLIANCE_POS.name()))) {
                    //then this is one of the data metrics we don't want to be able to edit because they are essential to the nature and identification of the entry
                    cancelEdit();
                }else {
                    oldStringBeingEdited = getText();
                    super.startEdit();
                    ((TextField) getGraphic()).setText(oldStringBeingEdited.split(":")[1]);
                }
            }

        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setTextFill(Color.PINK);//should never see this
            if (!isEditing()) {
                setContextMenu(menu);
            }

            if (empty) {
                setText("");
            } else {
                setText(item);
                //make it readable even if the item is selected
                if (selectedProperty().get()) {
                    setTextFill(Color.WHITE);
                }else if (!item.contains("=")) {
                    setTextFill(Color.BLUE);
                }else {
                    String str = item.split("=")[1];
                    if (Objects.equals(str, "?")) {
                        setTextFill(Color.BLUE);
                    }else {
                        int error = Integer.parseInt(str);
                        if (Math.abs(error) > Constants.LOW_ERROR_THRESHOLD) {
                            setTextFill(Color.RED);
                        }else if (error == 0) {
                            setTextFill(Color.GREEN);
                        }else {
                            setTextFill(Color.ORANGE);
                        }
                    }
                }
            }
        }//end update item


        @Override
        public void cancelEdit() {
            super.cancelEdit();
            oldStringBeingEdited = "";
        }

        @Override
        public void commitEdit(String newString) {
            if (this.getTreeItem().isLeaf()) {
                TreeItem<String> dataItem = this.getTreeItem();
                TreeItem<String> robotItem = dataItem.getParent();
                TreeItem<String> matchItem = robotItem.getParent();

                //figure out which data metric we are dealing with
                Optional<DataMetric> dataMetric = Configuration.getMetric(oldStringBeingEdited.split(":")[0]);
                if (dataMetric.isEmpty()) {
                    //idk how this could happen
                    Logging.logInfo("Cancelling edit because data metric was not found", true);
                    cancelEdit();
                }
                String[] tokens = oldStringBeingEdited.split(":");
                tokens[1] = newString;
                try {
                    //validate input
                    switch (dataMetric.get().getDatatype()) {

                        case INTEGER -> {
                            Integer.parseInt(tokens[1]);
                        }
                        case BOOLEAN -> {
                            if ((Integer.parseInt(tokens[1]) != 1) && (Integer.parseInt(tokens[1]) != 0)) {
                                throw new NumberFormatException();
                            }
                        }
                    }
                }catch (NumberFormatException e) {
                    Logging.logInfo("Please enter valid input", true);
                    return;
                }

                StringBuilder builder = new StringBuilder();
                for (String token : tokens) {
                    builder.append(token).append(":");
                }
                String newStringTodisplay = builder.substring(0, builder.toString().length() - 1);
                int matchNum = Integer.parseInt(matchItem.getValue().split(" ")[1]);
                int teamNum = Integer.parseInt(robotItem.getValue().split(" ")[1]);
                //then this is the datapoint we are looking for
                StringBuilder statementBuilder = new StringBuilder();
                statementBuilder.append("UPDATE \"").append(tableName).append("\" ");
                statementBuilder.append("SET ").append(dataMetric.get().getName()).append("=?");
                statementBuilder.append(" WHERE ").append(Constants.SQLColumnName.TEAM_NUM).append("=?");
                statementBuilder.append(" AND ").append(Constants.SQLColumnName.MATCH_NUM).append("=?");
                try {
                    SQLUtil.execNoReturn(statementBuilder.toString(), new String[] {newString, String.valueOf(teamNum), String.valueOf(matchNum)}, true);
                    super.commitEdit(newStringTodisplay);
                } catch (SQLException | DuplicateDataException e) {
                    Logging.logError(e, "Error updatingSQLDatabase");
                    cancelEdit();
                }

            }else {
                cancelEdit();
            }
        }


    }


    public static int getIntFromEntryObject(Constants.SQLColumnName metric, HashMap<String, Object> entryObject) {
        return Integer.parseInt(((HashMap<String, Object>) entryObject.get(metric.toString()))
                .get(String.valueOf(Configuration.Datatype.INTEGER.ordinal())).toString());
    }

    private ArrayList<HashMap<String, Object>> getDataForMatch(int matchNum, JSONArray data) {
        ArrayList<HashMap<String, Object>> dataForSpecificMatch = new ArrayList<>();
        data.toList().forEach(o -> {
            HashMap<String, Object> entryMap = (HashMap<String, Object>) o;
            int num = getIntFromEntryObject(Constants.SQLColumnName.MATCH_NUM, entryMap);
            if (matchNum == num) {
                dataForSpecificMatch.add(entryMap);
            }

        });
        return dataForSpecificMatch;
    }
    private int findABSMax(int... vals) {
        int max = 0;

        for (int d : vals) {
            if (Math.abs(d) > max) max = d;
        }

        return Math.abs(max);
    }


}

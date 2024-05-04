package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import javafx.util.converter.DefaultStringConverter;
import org.controlsfx.control.textfield.TextFields;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.Exporter;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.controller.MasterController;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.OperationAbortedByUserException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseViewerTabContent extends GenericTabContent{

    private final VBox content = new VBox();

    private final HBox buttonBar = new HBox();

    private final Button validateButton = new Button();
    private final TextField autoCompletionField = new TextField();
    private final  Spinner<Integer> dataValidationThresholdSpinner = new Spinner<>();

    //this must be updated each year, see getButtonBarMethod
    private final ArrayList<Pair<String, String>> events;

    @Override
    public Node getContent() {
        return content;
    }

    @Override
    public void updateDisplay() {
            updateDisplay(false);
    }

    @Override
    public Constants.TabType getTabType() {
        return Constants.TabType.DATABASE_VIEWER;
    }

    @Override
    public void save() {
        Logging.logInfo("Saving " + tabName.get());

        Path backupFile = Paths.get(Constants.BASE_APP_DATA_FILEPATH + "/resources/tempBackup" + System.currentTimeMillis() + ".tmp");
        try {
            JSONArray dataArray = DatabaseManager.readDatabase(tableName);
            File selectedFile;
            boolean needToPickFile = true;
            if (contentFileLocation.isPresent()) {
                File potentialFile = contentFileLocation.get();
                if (potentialFile.exists() && potentialFile.isFile()) {
                    if (potentialFile.getName().endsWith(".json")) {
                        needToPickFile = false;
                    }
                } else  {
                    try {
                        if (contentFileLocation.get().createNewFile()) {
                            needToPickFile = false;
                        }
                    }catch (Exception e) {
                        needToPickFile = true;
                    }

                }
            }

        if (needToPickFile) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Backup");
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
            chooser.setInitialFileName("Backup " + new Date().toString().replaceAll(":", " ") + ".json");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", ".json"));
            selectedFile  = chooser.showSaveDialog(ScoutingServer.mainStage.getOwner());
        }else {
            selectedFile = contentFileLocation.get();
        }

        //maybe, but better safe than sorry
        if (selectedFile == null) {
            Logging.logError(new Exception(), "Could not get a file to save the database at");
            return;
        }
        //save backup of file

        Path originalPath = selectedFile.toPath();
        try {
            Files.copy(originalPath, backupFile);
        } catch (IOException e) {
            Logging.logInfo("Failed to save backup before saving database, will just risk corrupting the users data");
        }

        try {
            FileOutputStream os = new FileOutputStream(selectedFile);
            os.write(dataArray.toString(1).getBytes());
            os.flush();
            os.close();
            setNeedsSavingProperty(false);
            contentFileLocation = Optional.of(selectedFile);
        } catch (IOException e) {
            Logging.logError(e, "Failed to save database, will try and restore backup");
            Path target = selectedFile.toPath();
            try {
                Files.copy(backupFile, target);
            } catch (IOException l) {
                Logging.logInfo("Failed to restore file");
            }
        }
        }catch (SQLException | ConfigFileFormatException  e) {
            Logging.logError(e);
        }finally {
            backupFile.toFile().delete();
        }
    }

    @Override
    public void saveAs() {
        Logging.logInfo("Saving " + tabName.get() +  "As new File");

        try {
            JSONArray dataArray = DatabaseManager.readDatabase(tableName);
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Database");
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
            chooser.setInitialFileName(tabName.get());
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
            os.write(dataArray.toString(1).getBytes());
            os.flush();
            os.close();


        } catch (IOException | SQLException | ConfigFileFormatException e) {
            Logging.logError(e, "Failed to save as new file");
        }
    }


    //this is NOT the same as the tab name shown. The tab name represents the file this would be saved to
        //in the user's eyes the tab is a file that they opened, or created and havent saved yet. The user can save this tab to a file.
        //the SQL table is used internall for data manipulation and this is the table name
        //to ensure tables are unique, the timestamp is used as the table name.
        public String tableName;

        private final TreeItem<String> rootItem;

        private String currentEventCode = "";



        private Optional<JSONArray> tbaDataOptional = Optional.empty();

        public DatabaseViewerTabContent(File file) {
            this(file.getName(), Optional.of(file));
        }

        public DatabaseViewerTabContent(String tabName) {
           this(tabName, Optional.empty());
        }

        public DatabaseViewerTabContent(String tabName, Optional<File> data) {
            super(tabName, data);
            Logging.logInfo("Initializing Database view Tab Content for tab: " + tabName);
            tableName = String.valueOf(System.currentTimeMillis());//see above long comment

            //create sql table
            try {
                SQLUtil.execNoReturn("DROP TABLE IF EXISTS '" + tableName + "'");
                SQLUtil.addTableIfNotExists(tableName);
                if (data.isPresent()) {
                    DatabaseManager.importJSONFile(data.get(), tableName);
                }
            } catch (ConfigFileFormatException | SQLException | IOException | DuplicateDataException e) {
                Logging.logError(e);
                //duplicate data is not handled here because the import json file methond does not throw duplicate data exception, only
                //the drop table and create new table statements are causing it, in which case, we don't care, it won't happen.
            }

            //gui
            setUpButtonBar();

            //init data stuff
            rootItem = new TreeItem<>("root-item");

            TreeView<String> treeView = new TreeView<>();
            treeView.setEditable(true);
            treeView.setShowRoot(false);
            treeView.setRoot(rootItem);
            treeView.setCellFactory(param -> new EditableTreeCell());
            JSONArray eventList = new JSONArray();
            //init list of events
            try {
                FileInputStream stream = new FileInputStream(Constants.BASE_READ_ONLY_FILEPATH + "/resources/TBAData/eventList.json");
                eventList = new JSONArray(new String(stream.readAllBytes()));
                stream.close();
            }catch (IOException e) {
                Logging.logError(e, "Whooop de doo, another IO exception, I guess your just screwed now,-C.H, try closing things and dont delete the resources folder and maybe restart the app and/or your computer if competitons aren't showing up this is probably why, this will probably never show up, but I kind of hope it does because it would be funny and I sat here typing noteA unessacarily long error message for no good reason");
            }


            ArrayList<String> options = new ArrayList<>();
            events = new ArrayList<>();
            for (Object o : eventList.toList()) {
                HashMap<String, String> comp = (HashMap<String, String>) o;
                events.add(new Pair<>(comp.get("key"), comp.get("name")));
                options.add(comp.get("name"));
            }

            TextFields.bindAutoCompletion(autoCompletionField, options);

            updateDisplay(false);
            content.getChildren().add(treeView);
            content.setSpacing(10);
            content.prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
            content.prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
            treeView.prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
            treeView.prefWidthProperty().bind(Constants.UIValues.appWidtProperty());


        }



        public void export() {
            Logging.logInfo("Exporting");
            //first gather the nessacary data from tba to export and check to make sure
            //everything is good and ready before showing the user a save dialog
            //save dialog should be the last dialog becuase in the user's mind the file is already created when that dialog comes up



            //gather TBA data
            //first figure out which competition we are at
/*            if ("".equals(currentEventCode)) {
                //if no event code has been selected
                if (selectCompetition()) {
                    //if the selection fails or is canceled etc
                    Logging.logInfo("Export Aborted");
                    return;
                }
            }*/
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



        public void clearDatabase() {
            if (!Constants.askQuestion("Are you sure you want to clear database " + tabName.get() + "?")) {
                return;
            }
            Logging.logInfo("Clearing databse: " + tabName.get());
            try {
                SQLUtil.execNoReturn("DELETE FROM \"" + tabName.get() + "\"");
            } catch (SQLException | DuplicateDataException e) {
                Logging.logError(e);
            }
            rootItem.getChildren().clear();
            updateDisplay(false);
        }



        public void updateDisplay(boolean validateData) {
            Logging.logInfo("Updating database: " + tabName.get());
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
                JSONArray data = DatabaseManager.readDatabase(tableName, true);
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
                int lastMatch = getIntFromEntryMap(Constants.SQLColumnName.MATCH_NUM, (HashMap<String, Object>)data.get(data.length() - 1));
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



                    ArrayList<HashMap<String, Object>> redAllianceData = dataForThisMatch.stream().filter(map -> getIntFromEntryMap(Constants.SQLColumnName.ALLIANCE_POS, map) < 3).collect(Collectors.toCollection(ArrayList::new));
                    ArrayList<HashMap<String, Object>> blueAllianceData = dataForThisMatch.stream().filter(map -> getIntFromEntryMap(Constants.SQLColumnName.ALLIANCE_POS, map) > 2).collect(Collectors.toCollection(ArrayList::new));
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
                    List<HashMap<String, Object>> scoutingDataForThisPosition = scoutingDataForThisMatch.stream().filter(map -> getIntFromEntryMap(Constants.SQLColumnName.ALLIANCE_POS, map) == correctRobotPosition.ordinal()).toList();
                    if (scoutingDataForThisPosition.size() == 1) {
                        //then there is only one robot entered for this position
                        if (getIntFromEntryMap(Constants.SQLColumnName.TEAM_NUM, scoutingDataForThisPosition.get(0)) != teamNum) {
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
                        getIntFromEntryMap(Constants.SQLColumnName.AUTO_SPEAKER, scoutingData.get(0)) +
                                getIntFromEntryMap(Constants.SQLColumnName.AUTO_SPEAKER, scoutingData.get(1)) +
                                getIntFromEntryMap(Constants.SQLColumnName.AUTO_SPEAKER, scoutingData.get(2));

                int autoAmpMeasured =
                        getIntFromEntryMap(Constants.SQLColumnName.AUTO_AMP, scoutingData.get(0)) +
                                getIntFromEntryMap(Constants.SQLColumnName.AUTO_AMP, scoutingData.get(1)) +
                                getIntFromEntryMap(Constants.SQLColumnName.AUTO_AMP, scoutingData.get(2));


                int teleSpeakerMeasured =
                        getIntFromEntryMap(Constants.SQLColumnName.TELE_SPEAKER, scoutingData.get(0)) +
                                getIntFromEntryMap(Constants.SQLColumnName.TELE_SPEAKER, scoutingData.get(1)) +
                                getIntFromEntryMap(Constants.SQLColumnName.TELE_SPEAKER, scoutingData.get(2)) +
                                getIntFromEntryMap(Constants.SQLColumnName.SPEAKER_RECEIVED, scoutingData.get(0)) +
                                getIntFromEntryMap(Constants.SQLColumnName.SPEAKER_RECEIVED, scoutingData.get(1)) +
                                getIntFromEntryMap(Constants.SQLColumnName.SPEAKER_RECEIVED, scoutingData.get(2));

                int teleAmpMeasured =
                        getIntFromEntryMap(Constants.SQLColumnName.TELE_AMP, scoutingData.get(0)) +
                                getIntFromEntryMap(Constants.SQLColumnName.TELE_AMP, scoutingData.get(1)) +
                                getIntFromEntryMap(Constants.SQLColumnName.TELE_AMP, scoutingData.get(2)) +
                                getIntFromEntryMap(Constants.SQLColumnName.AMP_RECEIVED, scoutingData.get(0)) +
                                getIntFromEntryMap(Constants.SQLColumnName.AMP_RECEIVED, scoutingData.get(1)) +
                                getIntFromEntryMap(Constants.SQLColumnName.AMP_RECEIVED, scoutingData.get(2));


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
                DatabaseManager.RobotPosition robotPosition = DatabaseManager.RobotPosition.values()[getIntFromEntryMap(Constants.SQLColumnName.ALLIANCE_POS, scoutingDatum)];
                int teamNum = getIntFromEntryMap(Constants.SQLColumnName.TEAM_NUM, scoutingDatum);
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
                            ((HashMap<String, Object>) scoutingDatum.get(rawDataMetric.getName())).get(rawDataMetric.getDatatypeAsString()).toString()
                            + ":Error=" + error);
                    robotPositionItem.getChildren().add(dataPointItem);;
                }
                matchItem.getChildren().add(robotPositionItem);
            }
            return maxErrorOfThisAlliance;

        }

/*        public boolean selectCompetition() {
            Logging.logInfo("asking user to select which competition they are at");
            DataValidationCompetitionChooser chooser = new DataValidationCompetitionChooser();
            Optional<String> result = chooser.showAndWait();
            AtomicReference<String> selectedEvent = new AtomicReference<>("");
            result.ifPresent(selectedEvent::set);
            currentEventCode = selectedEvent.get();
            return Objects.equals(currentEventCode, "");

        }*/

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


        private void setUpButtonBar() {
                //data validation competition selector
                autoCompletionField.setPromptText("Select Competition");
                autoCompletionField.setPrefWidth(150);
                buttonBar.getChildren().add(autoCompletionField);

                dataValidationThresholdSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,10,3));
                dataValidationThresholdSpinner.setPrefWidth(65);
                buttonBar.getChildren().addAll(new Label("Error Threshold: "), dataValidationThresholdSpinner);


                setValidateButtonGraphic(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/icons/validation-icon.png"));

                validateButton.setOnAction(e -> validateData());
                buttonBar.getChildren().add(validateButton);


                Button exportButton = new Button();
                exportButton.setTooltip(new Tooltip("Export File"));
                File exportImageFile = new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/icons/export-icon.png");
                Image exportImage = new Image(exportImageFile.toURI().toString());
                ImageView exportImageView = new ImageView();
                exportImageView.setImage(exportImage);
                exportButton.setGraphic(exportImageView);
                exportButton.setOnAction(event -> MasterController.export());
                buttonBar.getChildren().add(exportButton);


            content.getChildren().add(buttonBar);
                buttonBar.setSpacing(10);
                buttonBar.setAlignment(Pos.CENTER_LEFT);



        }

        public void validateData() {
            Constants.LOW_ERROR_THRESHOLD = dataValidationThresholdSpinner.getValue();
            String result = autoCompletionField.getText();
            boolean success = true;
            if (!Objects.equals(result, "")) {
                //if they actuall selected something
                Optional<Pair<String, String>> event = events.stream().filter(s -> s.getValue().equals(result)).findFirst();
                if (event.isPresent()) {
                    currentEventCode = event.get().getKey();
                    tbaDataOptional = APIUtil.getEventMatches(currentEventCode);
                    updateDisplay(true);
                }else {
                    success = false;
                }

            }else {
                success = false;
            }

            if (!success) {
                Logging.logInfo("Failed to validate data, please ensure you have internet and have selected a competiton", true);
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
                    setNeedsSavingProperty(true);
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
                        setNeedsSavingProperty(true);
                    } catch (SQLException | DuplicateDataException e) {
                        Logging.logError(e, "Error updatingSQLDatabase");
                        cancelEdit();
                    }

                }else {
                    cancelEdit();
                }
            }


        }


        public static int getIntFromEntryMap(Constants.SQLColumnName metric, HashMap<String, Object> entryObject) {
            return Integer.parseInt(((HashMap<String, Object>) entryObject.get(metric.toString()))
                    .get(String.valueOf(Configuration.Datatype.INTEGER.ordinal())).toString());
        }

    public static int getIntFromEntryJSONObject(Constants.SQLColumnName metric, JSONObject entryObject) {
        return entryObject.getJSONObject(metric.toString()).getInt(String.valueOf(Configuration.Datatype.INTEGER.ordinal()));
    }

    public static int getIntFromEntryJSONObject(DataMetric metric, JSONObject entryObject) {
        return entryObject.getJSONObject(metric.getName()).getInt(String.valueOf(Configuration.Datatype.INTEGER.ordinal()));
    }

        private ArrayList<HashMap<String, Object>> getDataForMatch(int matchNum, JSONArray data) {
            ArrayList<HashMap<String, Object>> dataForSpecificMatch = new ArrayList<>();
            data.toList().forEach(o -> {
                HashMap<String, Object> entryMap = (HashMap<String, Object>) o;
                int num = getIntFromEntryMap(Constants.SQLColumnName.MATCH_NUM, entryMap);
                if (matchNum == num) {
                    dataForSpecificMatch.add(entryMap);
                }

            });
            return dataForSpecificMatch;
        }
        private int findABSMax(int... vals) {
            int max = 0;

            for (int d : vals) {
                if (Math.abs(d) > max) max = Math.abs(d);
            }

            return Math.abs(max);
        }


        private void setValidateButtonGraphic(File file) {
            Image image = new Image(file.toURI().toString());
            ImageView iamgeView = new ImageView();
            iamgeView.setImage(image);
            validateButton.setGraphic(iamgeView);
        }

}

package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.*;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DataValidationCompetitionChooser;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.RobotPositon;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.OperationAbortedByUserException;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TabController {


    @FXML
    private TreeView<String> treeView;

    @FXML
    public TabPane pane;
    @FXML
    public ToggleButton editToggle;
    @FXML
    public Button validateDataButton;
    @FXML
    public Button exportButton;
    @FXML
    public CheckBox exportNotesCheckbox;
    private ArrayList<Match> databaseData;

    public String tableName;

    private TreeItem<Label> rootItem;

    private TreeItem<String> stringRootItem;
    private JSONArray eventList;

    private String currentEventCode = "";

    private Optional<JSONArray> tbaDataOptional = Optional.empty();


    public TabController(ArrayList<Match> databaseData, String table, TabPane thePane) {
        this.databaseData = databaseData;
        tableName = table;
        pane = thePane;
    }



    @FXML
    public void initialize(TreeView<String> view) {
        Logging.logInfo("Initializing Tab Controller: " + tableName);
        //init data stuff
        treeView = view;
        rootItem = new TreeItem<>(new Label("root-item"));
        stringRootItem = new TreeItem<>("root-item");

        treeView.setEditable(true);
        treeView.setShowRoot(false);
        treeView.setRoot(stringRootItem);
        treeView.setCellFactory(tv -> new TextFieldTreeCell<>(new DefaultStringConverter()) {

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setTextFill(Color.RED);


                if (empty) {
                    setText("");
                } else {
                    setText(item);
                    //make it readable even if the item is selected
                    if (selectedProperty().get()) {
                        setTextFill(Color.WHITE);
                    }else {
                        setTextFill(Color.RED);
                    }

/*                    String styleClass = "-fx-text-fill: #ff0000"; // choose style class for item
                    getStyleClass().add(styleClass);*/
                }
            }
        });

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
            data = exporter.export(exportNotesCheckbox.isSelected());
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
        stringRootItem.getChildren().clear();
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
            for (TreeItem<Label> matchItem : rootItem.getChildren()) {
                ArrayList<Boolean> matchExpansion = new ArrayList<>();
                for (TreeItem<Label> robotItem : matchItem.getChildren()) {
                    matchExpansion.add(robotItem.isExpanded());
                }
                expainsionStructure.add(new Pair<>(matchItem.isExpanded(), matchExpansion));
            }
            stringRootItem.getChildren().clear();
            if (data.isEmpty()) {
                stringRootItem.getChildren().add(new TreeItem<>("No Data..."));
                return;
            }
            //for validation
            boolean checkTeamNumbers = true;
            //for each json object representing a entry in the database, sorted
            int arrayIndex = 0;
            int lastMatch = getIntFromEntryObject(Constants.SQLColumnName.MATCH_NUM, (HashMap<String, Object>)data.get(data.length() - 1));
            for (int matchNum = 1; matchNum <=  lastMatch; matchNum++) {
                if (validateData) {

                    int finalMatchNum = matchNum;
                    Optional<Object> matchTBAData =  new ArrayList<>(tbaDataOptional.get().toList()).stream().filter(o -> {
                        HashMap<String, Object> dataum = (HashMap<String, Object>) o;
                        String exptectedMatchKey = currentEventCode + "_qm"  + finalMatchNum;
                        return dataum.get("key").equals(exptectedMatchKey);
                    }).findFirst();


                    if (matchTBAData.isPresent()) {
                        //now and only now are we able to validate this match
                        HashMap<String, Object> matchDatum = (HashMap<String, Object>) matchTBAData.get();
                        if (checkTeamNumbers) {
                            checkTeamNumbers = checkTeamNumbersForMatch(matchNum, matchDatum, getDataForMatch(matchNum, data));
                        }
                    }

                }

                HashMap<String, Object> entryObject = (HashMap<String, Object>) data.get(arrayIndex);

                int observedMatchNum = getIntFromEntryObject(Constants.SQLColumnName.MATCH_NUM, entryObject);

                if (observedMatchNum != matchNum) {
                    //then there are no robots for this match number, or we have tried all of them, but we need to move on
                    //break out and imcrement the match number we are looking at and see if the first data object matches up then. The data should be sorted.
                    break;
                }
                //now we have asertained that the data entry we are analyzing is actually the match number we think it is.
                TreeItem<String> matchItem = new TreeItem<>("Match: " + matchNum);
                try {
                    matchItem.setExpanded(expainsionStructure.get(matchNum - 1).getKey());
                }catch (IndexOutOfBoundsException e) {
                    matchItem.setExpanded(false);
                }

                for (int robotPosition = 0; robotPosition < DatabaseManager.RobotPosition.values().length; robotPosition++) {
                    int teamNum = getIntFromEntryObject(Constants.SQLColumnName.TEAM_NUM, entryObject);
                    DatabaseManager.RobotPosition observedPosition = DatabaseManager.getRobotPositionFromNum(getIntFromEntryObject(Constants.SQLColumnName.ALLIANCE_POS, entryObject));
                    //the json entries will be in order of robot position, so we can assume that if we check for the first one first, it won't show up later and stuff wont be skipped
                    if (robotPosition == observedPosition.ordinal()) {
                        TreeItem<String> robotPositionItem = new TreeItem<>(observedPosition.name()  + ": " + teamNum);
                        try {
                            robotPositionItem.setExpanded(expainsionStructure.get(matchNum - 1).getValue().get(observedPosition.ordinal()));
                        }catch (IndexOutOfBoundsException e) {
                            robotPositionItem.setExpanded(false);
                        }
                        //here I know the match num, team num, and robot position. i need to validate data.

                        //add all the data to the robot position item
                        for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
                            TreeItem<String> dataPointItem = new TreeItem<>(rawDataMetric.getName()
                                    + ":" +
                                    ((HashMap<String, Object>) entryObject.get(rawDataMetric.getName())).get(String.valueOf(rawDataMetric.getDatatype().ordinal())).toString()
                                    + ":Error=?");
                            robotPositionItem.getChildren().add(dataPointItem);;
                        }

                        matchItem.getChildren().add(robotPositionItem);
                        //we are done taking data from this array entry, move on to the next one
                        //increment if array index and check if the next one is not the same match

                        if (++arrayIndex == data.length()) {
                            //we have reached the end of the data, doing the next check will cause an exception
                            break;
                        }
                        if (getIntFromEntryObject(Constants.SQLColumnName.MATCH_NUM, (HashMap<String, Object>) data.get(arrayIndex)) != matchNum) {
                            //if the next match number is differnet, the we need to increment the match number and start all over again
                            //so break out of this loop and do the next loop of the match num loop

                            break;
                        }
                        //update the object we are analying with the new array index
                        entryObject = (HashMap<String, Object>) data.get(arrayIndex);

                    }


                }//robot position loop.
                stringRootItem.getChildren().add(matchItem);
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


    public boolean selectCompetition() {
        Logging.logInfo("asking user to select which competition they are at");
        DataValidationCompetitionChooser chooser = new DataValidationCompetitionChooser();
        Optional<String> result = chooser.showAndWait();
        AtomicReference<String> selectedEvent = new AtomicReference<>("");
        result.ifPresent(selectedEvent::set);
        currentEventCode = selectedEvent.get();
        return Objects.equals(currentEventCode, "");

    }

    private void setExpansionAll(TreeItem<Label> treeItem, boolean val) {
        if (treeItem.getValue().getText().equals("root-item")) {
            //then we are dealing with the root item
            treeItem.setExpanded(true);
        } else {
            treeItem.setExpanded(val);

        }
        if (!treeItem.getChildren().isEmpty()) {
            for (TreeItem<Label> t : treeItem.getChildren()) {
                setExpansionAll(t, val);
            }
        }


    }



    private class EditableTreeCell extends TextFieldTreeCell<Label> {
        private final ContextMenu menu = new ContextMenu();
        private final TabController controller;
        public EditableTreeCell(TabController c) {
            super(new CustomStringConverter());
            controller = c;
            MenuItem renameItem = new MenuItem("Edit");
            MenuItem deleteItem = new MenuItem("Delete");
            menu.getItems().addAll(renameItem, deleteItem);

            renameItem.setOnAction(arg0 -> startEdit());
            deleteItem.setOnAction(event -> {
                if (getTreeItem().getParent() == rootItem) {
                    //then this is a match item
                    System.out.println("match item");
                    for (TreeItem<Label> positionItem : getTreeItem().getChildren()) {
                        deletePositionItem(positionItem);
                    }
                }else if (getTreeItem().isLeaf()){
                    deletePositionItem(getTreeItem().getParent());
                }else {
                    deletePositionItem(getTreeItem());
                }



            });
        }

        private void deletePositionItem(TreeItem<Label> positionItem) {
            String teamNum = positionItem.getValue().getText().split(" ")[1];

            String match = positionItem.getParent().getValue().getText().split(" ")[1];
            try {
                SQLUtil.execNoReturn("DELETE FROM \"" + tableName + "\" WHERE " + Constants.SQLColumnName.MATCH_NUM.name() + "=? AND " + Constants.SQLColumnName.TEAM_NUM.name() + "=?", new Object[]{match, teamNum}, true);

            } catch (SQLException | DuplicateDataException e) {
                Logging.logError(e, "Failed to delete datapoint");
            }
        }

        @Override
        public void updateItem(Label item, boolean empty) {
            super.updateItem(item, empty);
            if (!isEditing()) {
                setContextMenu(menu);
            }
        }
        @Override
        public void startEdit() {
            if (this.getTreeItem().isLeaf()) {
                String name = this.getTreeItem().getValue().getText().split(":")[0];
                if ((Objects.equals(name, Constants.SQLColumnName.ALLIANCE_POS.toString().replaceAll("_", " ").toLowerCase())) || (Objects.equals(name, Constants.SQLColumnName.TEAM_NUM.toString().replaceAll("_", " ").toLowerCase())) ||(Objects.equals(name, Constants.SQLColumnName.MATCH_NUM.toString().replaceAll("_", " ").toLowerCase()))  || (Objects.equals(name, Constants.SQLColumnName.TELE_COMMENTS.toString().replaceAll("_", " ").toLowerCase())) ) {
                    //then this is a comment
                    cancelEdit();
                    Logging.logInfo("This data cannot be edited", true);


                }else {
                    super.startEdit();
                }

            }else {
                super.cancelEdit();
            }

        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            //setEditMode(false);
        }

        @Override
        public void commitEdit(Label newLabel) {
            if (this.getTreeItem().isLeaf()) {
                //given the code in start edit, the newLabel is guarentted to be a numeric leaf item.
                try {
                    //see if they actually entered a number
                    Integer.valueOf(newLabel.getText());
                }catch (NumberFormatException e) {
                    cancelEdit();
                    Logging.logInfo("This needs to be a number, cancelling edit", true);

                }
                TreeItem<Label> dataItem = this.getTreeItem();
                TreeItem<Label> robotItem = dataItem.getParent();
                TreeItem<Label> matchItem = robotItem.getParent();

                //figure out what the new string should be
                String[] tokens = dataItem.getValue().getText().split(":");
                tokens[1] = newLabel.getText();
                StringBuilder builder = new StringBuilder();
                for (String token : tokens) {
                    builder.append(token).append(":");
                }
                String newString = builder.substring(0, builder.toString().length() - 1);
                super.commitEdit(new Label(newString));
                int matchNum = Integer.parseInt(matchItem.getValue().getText().split(" ")[1]);
                int teamNum = Integer.parseInt(robotItem.getValue().getText().split(" ")[1]);
                Match match = databaseData.stream().filter(match1 -> match1.matchNumber() == matchNum).findFirst().get();
                RobotPositon robotPositon = match.robotPositons().stream().filter(robot1 -> robot1.teamNumber() == teamNum).findFirst().get();
                robotPositon.data().replaceAll(dataPoint -> {
                    if (Objects.equals(dataPoint.getName().replaceAll("_", " ").toLowerCase(), tokens[0])) {
                        //then this is the datapoint we are looking for
                        DataPoint newData = new DataPoint(newString);
                        StringBuilder statementBuilder = new StringBuilder();
                        statementBuilder.append("UPDATE \"").append(tableName).append("\" ");
                        statementBuilder.append("SET ").append(newData.getName()).append("=?");
                        statementBuilder.append(" WHERE ").append(Constants.SQLColumnName.TEAM_NUM).append("=?");
                        statementBuilder.append(" AND ").append(Constants.SQLColumnName.MATCH_NUM).append("=?");
                        try {
                            SQLUtil.execNoReturn(statementBuilder.toString(), new String[] {newLabel.getText(), String.valueOf(teamNum), String.valueOf(matchNum)}, true);
                            Timer timer = new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    //setEditMode(false);
                                }
                            }, 10);
                        } catch (SQLException | DuplicateDataException e) {
                            Logging.logError(e, "Error updatingSQLDatabase");
                            cancelEdit();
                            return dataPoint;
                        }
                        return newData;
                    }else {
                        return dataPoint;
                    }
                });
            }else {
                System.out.println("Cancelling branch edit");
                cancelEdit();
            }


        }


    }


    private static class CustomStringConverter extends StringConverter<Label> {
        Paint paint = Color.PINK;//default, if I see this, something is wrong
        @Override
        public String toString(javafx.scene.control.Label object) {
            return object.getText();
        }

        @Override
        public javafx.scene.control.Label fromString(String string) {
            return new Label(string);
        }
    }

    private int getIntFromEntryObject(Constants.SQLColumnName metric, HashMap<String, Object> entryObject) {
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



}

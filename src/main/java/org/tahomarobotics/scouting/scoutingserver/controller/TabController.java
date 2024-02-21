package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.MatchRecordComparator;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TabController {


    Tab myTab;
    private LinkedList<DatabaseManager.MatchRecord> databaseData;
    public String tableName;

    private TreeView<String> treeView;

    private TreeItem<String> rootItem;


    public TabController(LinkedList<DatabaseManager.MatchRecord> databaseData, String table) {
        this.databaseData = databaseData;
        tableName = table;
    }


    public void export(Event e) {
        System.out.println("exporting");
        refresh(null);
        try {
            FileChooser chooser = new FileChooser();
            chooser.setInitialDirectory(new File(Constants.DATABASE_FILEPATH));
            chooser.setTitle("Select Export Location");
            chooser.setInitialFileName("export");
            chooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter("Excel Files", ".xls"));

            File file = chooser.showSaveDialog(ScoutingServer.mainStage.getOwner());
            SpreadsheetUtil.writeToSpreadSheet(databaseData, file, true);//should add button later if buranik insists to export raw wihtout formulas
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export data: IO Exception");
            alert.showAndWait();
        }
    }

    @FXML
    public void expandAll(Event e) {
        System.out.println("Expand All Button Pressed");
        setExpansionAll(rootItem, true);

    }

    @FXML
    public void collapseAll() {
        System.out.println("Collapse All Button Pressed");
        setExpansionAll(rootItem, false);
    }

    private void setExpansionAll(TreeItem treeItem, boolean val) {
        if (treeItem.getValue().equals("root-item")) {
            //then we are dealing with the root item
            treeItem.setExpanded(true);
        } else {
            treeItem.setExpanded(val);

        }
        if (!treeItem.getChildren().isEmpty()) {
            for (TreeItem t : (ObservableList<TreeItem>) treeItem.getChildren()) {
                setExpansionAll(t, val);
            }
        }


    }


    public void initialize(TreeView<String> view) {
        treeView = view;
        rootItem = new TreeItem<>("root-item");
        if (!databaseData.isEmpty()) {
            constructTree();
        }
        treeView.setShowRoot(false);
        treeView.setRoot(rootItem);


    }
    @FXML
    public void validateData(ActionEvent event) {
            String eventCode = "2024week0";
        try {
            JSONArray eventMatches = APIUtil.get("/event/" + eventCode + "/matches");//returns array of json objects each representing a match
            List<Object> rawList =  eventMatches.toList();
            rawList.sort((o1, o2) -> {
                HashMap<String, Object> thing1 = (HashMap<String, Object>) o1;
                HashMap<String, Object> thing2 = (HashMap<String, Object>) o2;
                return Integer.compare((Integer) thing1.get("match_number"), (Integer) thing2.get("match_number"));
            });//sort by match number
            ArrayList<ArrayList<DataPoint>> correctedData = new ArrayList<>();
            for (Object obj : rawList) {
                //for each match
                HashMap<String, Object> match = (HashMap<String, Object>) obj;
                HashMap<String, Object> matchScoreBreakdown = (HashMap<String, Object>) match.get("score_breakdown");
                int matchNum = (Integer) match.get("match_number");
               // System.out.println("Breakdown for match " + matchNum + ": " + matchScoreBreakdown);
                List<DatabaseManager.MatchRecord> robots = databaseData.stream().filter(matchRecord -> matchRecord.matchNumber() == matchNum).toList();
                boolean matchComplete = robots.size() == 6;//is the whole match invalid
                List<DatabaseManager.MatchRecord> redRobots = robots.stream().filter(matchRecord -> matchRecord.position().ordinal() < 3).toList();
                List<DatabaseManager.MatchRecord> blueRobots = robots.stream().filter(matchRecord -> matchRecord.position().ordinal() > 2).toList();
                HashMap<String, Object> redAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("red");
                HashMap<String, Object> blueAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("blue");
                ArrayList<ArrayList<DataPoint>> correctedMatch = new ArrayList<>();
                correctedMatch.addAll(correctAlliance(matchComplete, redRobots, redAllianceScoreBreakdown));
                correctedMatch.addAll(correctAlliance(matchComplete, blueRobots, blueAllianceScoreBreakdown));
                System.out.println("Corrected Match: " + correctedMatch);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ArrayList<ArrayList<DataPoint>> correctAlliance(boolean matchComplete, List<DatabaseManager.MatchRecord> robots, HashMap<String, Object> breakdown) {
        ArrayList<ArrayList<DataPoint>> correctedAlliance = new ArrayList<>();
        int autoSpeakerTrue = (int) breakdown.get("autoSpeakerNoteCount");
        int autoAmpTrue = (int) breakdown.get("autoAmpNoteCount");
        int teleSpeakerTrue = ((int) breakdown.get("teleopSpeakerNoteAmplifiedCount")) + ((int) breakdown.get("teleopSpeakerNoteCount"));
        int teleAmpTrue = (int) breakdown.get("teleopAmpNoteCount");
        for (DatabaseManager.MatchRecord robot : robots) {
            ArrayList<DataPoint> recordTemp = new ArrayList<>();
            //for each robot
            for (DataPoint dataPoint : robot.getDataAsList()) {
                if (!matchComplete) {
                    //then there is missing data for at least on robot or there is excess data and the whole match will be marked as incorrect
                    recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.ErrorLevel.MEDIUM));
                }
                try {
                    switch (Constants.ColumnName.valueOf(dataPoint.getName())) {

                        case TIMESTAMP, MATCH_NUM, TEAM_NUM, ALLIANCE_POS, AUTO_LEAVE, AUTO_COMMENTS, TELE_COMMENTS, ENDGAME_POS -> {
                            recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.ErrorLevel.ZERO));
                            break;
                        }
                        case AUTO_SPEAKER -> {
                            recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.translateErrorNum(robot.autoSpeaker() - autoSpeakerTrue)));
                            break;
                        }
                        case AUTO_AMP -> {
                            recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.translateErrorNum(robot.autoAmp() - autoAmpTrue)));
                        }
                        case AUTO_COLLECTED, AUTO_SPEAKER_MISSED, AUTO_AMP_MISSED -> {
                            //collected - missed = scored + error
                            //collected - missed - scored = error
                            int totalMissed = robot.autoAmpMissed() + robot.autoSpeakerMissed();
                            int totalScored = autoSpeakerTrue + autoAmpTrue;
                            recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.translateErrorNum(robot.autoCollected() - totalScored - totalMissed)));
                            break;
                        }
                        case TELE_SPEAKER -> {
                            recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.translateErrorNum(robot.teleSpeaker() - teleSpeakerTrue)));
                            break;
                        }
                        case TELE_AMP -> {
                            recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.translateErrorNum(robot.teleAmp() - teleAmpTrue)));
                            break;
                        }
                        case TELE_TRAP -> {
                            int robotPositonNum = (robot.position().ordinal() % 3)+1;
                            String climb = (String) breakdown.get("endGameRobot" + robotPositonNum);
                            boolean trap = false;
                            if (!Objects.equals(climb, "None") && !Objects.equals(climb, "Parked")) {
                                trap = (boolean) breakdown.get("trap" + climb);
                            }
                            recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), (trap == (robot.teleTrap() > 0))?(DataPoint.ErrorLevel.ZERO):(DataPoint.ErrorLevel.HIGH)));
                            break;
                        }
                        case TELE_SPEAKER_MISSED, TELE_AMP_MISSED -> {
                            recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.ErrorLevel.UNKNOWN));
                            break;
                        }
                    }//end switch
                }catch (IllegalArgumentException messingwithAsher) {
                    //do nothing this just happens when you try to access the calculated values
                }
                System.out.println("Record Temp: " + recordTemp);
            }
            correctedAlliance.add(recordTemp);

        }
        return correctedAlliance;

    }

    @FXML
    public void refresh(Event e) {
        System.out.println("Refreshing");
        rootItem.getChildren().clear();
        try {
            databaseData = DatabaseManager.readDatabase(tableName);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        constructTree();
    }

    private void constructTree() {


        //need to sort the database data by match number and team position
        databaseData.sort(new MatchRecordComparator());//this is actually nessacary as it assures that the robot positon indexes appear sequentially
        List<DatabaseManager.MatchRecord> tempList = new LinkedList<>(databaseData.stream().toList());
        for (int i = 1; i <= databaseData.get(databaseData.size() - 1).matchNumber(); i++) {
            //for all the matches that there may or may not be data for up to the last match with data for it
            //i will be set to all valid match numbers

            //this is a branch item that will represent a match
            TreeItem<String> matchItem = new TreeItem<>("Match: " + i);


            //now we will loop through the data taking all relevant matches and adding them to this branch
            //then removing them from the temporart dataset to improve effiency
            int robotPositionIndex = 0;

            for (DatabaseManager.MatchRecord r : tempList) {
                //for each datapoint
                if (r.matchNumber() == i) {
                    if (robotPositionIndex == 0) {
                        rootItem.getChildren().add(matchItem);//we only want to do this if there is data for a match, but only once per match
                    }
                    //if the dataPoint we are looking at right now is the next position sequentially (we haven't skipped a position)
                    //add all this dataPoint's data to leaf items
                    TreeItem<String> positonItem = new TreeItem<String>(r.position().toString() + ": " + r.teamNumber());
                    matchItem.getChildren().add(robotPositionIndex, positonItem);

                    for (DataPoint d : r.getDataAsList()) {
                        positonItem.getChildren().add(new TreeItem<String>(d.getName() + ": " + d.getValue()));

                    }

                    robotPositionIndex++;
                } else {
                    //we have gone through all 6 robot positions for this match so it is guarenteed that there will be no more of this match
                    break;
                }
            }//end for each dataPoint loop
            final int finalI = i;
            tempList.removeIf(new Predicate<DatabaseManager.MatchRecord>() {
                @Override
                public boolean test(DatabaseManager.MatchRecord matchRecord) {
                    return matchRecord.matchNumber() == finalI;
                }
            });
        }//end for each match loop
    }


}

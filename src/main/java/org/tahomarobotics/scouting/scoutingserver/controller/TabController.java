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
import org.tahomarobotics.scouting.scoutingserver.DataValidator;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.MatchRecordComparator;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;

import java.io.File;
import java.io.FilterOutputStream;
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
        ArrayList<ArrayList<ArrayList<DataPoint>>> validatedData = DataValidator.validateData(eventCode, databaseData);
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

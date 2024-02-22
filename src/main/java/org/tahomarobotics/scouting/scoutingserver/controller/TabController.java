package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.dhatim.fastexcel.Color;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataValidator;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.MatchRecordComparator;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.Robot;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class TabController {


    Tab myTab;
    private ArrayList<Match> databaseData;
    public String tableName;

    private TreeView<Label> treeView;

    private TreeItem<Label> rootItem;


    public TabController(ArrayList<Match> databaseData, String table) {
        this.databaseData = databaseData;
        tableName = table;
    }


    public void export(Event e) {
        System.out.println("exporting");
        refresh(null);
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(Constants.DATABASE_FILEPATH));
        chooser.setTitle("Select Export Location");
        chooser.setInitialFileName("export");
        chooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter("Excel Files", ".xls"));

        File file = chooser.showSaveDialog(ScoutingServer.mainStage.getOwner());
        //SpreadsheetUtil.writeToSpreadSheet(databaseData, file, true);//should add button later if buranik insists to export raw wihtout formulas
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

    private void setExpansionAll(TreeItem<Label> treeItem, boolean val) {
        if (treeItem.getValue().getText().equals("root-item")) {
            //then we are dealing with the root item
            treeItem.setExpanded(true);
        } else {
            treeItem.setExpanded(val);

        }
        if (!treeItem.getChildren().isEmpty()) {
            for (TreeItem<Label> t : (ObservableList<TreeItem<Label>>) treeItem.getChildren()) {
                setExpansionAll(t, val);
            }
        }


    }


    public void initialize(TreeView<Label> view) {
        treeView = view;
        rootItem = new TreeItem<>(new Label("root-item"));
        if (!databaseData.isEmpty()) {
            constructTree(databaseData);
        }
        treeView.setShowRoot(false);
        treeView.setRoot(rootItem);


    }
    @FXML
    public void validateData(ActionEvent event) {

        String eventCode = "2024week0";
        databaseData = DataValidator.validateData(eventCode, databaseData);
        constructTree(databaseData);
    }



    @FXML
    public void refresh(Event e) {
        System.out.println("Refreshing");
        try {
            databaseData = DatabaseManager.getUnCorrectedDataFromDatabase(tableName);
        } catch (IOException ex) {
            Logging.logError(ex);
        }
        constructTree(databaseData);
    }

    private void constructTree(ArrayList<Match> matches) {
        rootItem.getChildren().clear();
        for (Match match : matches) {

            DataPoint.ErrorLevel maxErrorLevelInThisMatch = DataPoint.ErrorLevel.ZERO;
            Label matchLabel = new Label("Match: " + match.matchNumber());
            TreeItem<Label> matchItem = new TreeItem<>(matchLabel);


            for (Robot robot : match.robots()) {
                DataPoint.ErrorLevel maxErrorForThisRobot = DataPoint.ErrorLevel.ZERO;
                Label robotLabel = new Label(robot.robotPosition().toString() + ": " + robot.teamNumber());
                TreeItem<Label> robotItem = new TreeItem<>(robotLabel);
                matchItem.getChildren().add(robotItem);


                for (DataPoint dataPoint : robot.data()) {
                    Label l = new Label(dataPoint.toString());
                    l.setTextFill(DataPoint.color.get(dataPoint.getErrorLevel()));
                    TreeItem<Label> dataItem = new TreeItem<>(l);
                    robotItem.getChildren().add(dataItem);
                    if (dataPoint.getErrorLevel().ordinal() > maxErrorForThisRobot.ordinal()) {
                        maxErrorForThisRobot = dataPoint.getErrorLevel();
                    }
                }//end robot for
                if (maxErrorForThisRobot.ordinal() > maxErrorLevelInThisMatch.ordinal()) {
                    maxErrorLevelInThisMatch = maxErrorForThisRobot;
                }
                robotLabel.setTextFill(DataPoint.color.get(maxErrorForThisRobot));
            }//end match for
            matchLabel.setTextFill(DataPoint.color.get(maxErrorLevelInThisMatch));
            rootItem.getChildren().add(matchItem);
        }//end comp for
    }


}

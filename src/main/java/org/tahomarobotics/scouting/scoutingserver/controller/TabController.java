package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;
import org.tahomarobotics.scouting.scoutingserver.util.MatchDataComparator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

public class TabController {


    Tab myTab;
    private final LinkedList<DataHandler.MatchRecord> databaseData;
    public File database;

    private TreeView<String> treeView;

    private TreeItem<String> rootItem;


    public TabController(LinkedList<DataHandler.MatchRecord> databaseData, File d) {
        this.databaseData = databaseData;
        database = d;
        System.out.println("In contoller constuctor" + databaseData);
    }

    public void selectItem(Event event) {

    }

    public void expandAll(Event e) {
        setExpansionAll(rootItem, true);

    }

    private void setExpansionAll(TreeItem treeItem, boolean val) {
        treeItem.setExpanded(val);
        if (!treeItem.getChildren().isEmpty()) {
            for ( TreeItem t  :  (ObservableList<TreeItem>) treeItem.getChildren()) {
                setExpansionAll(t, val);
            }
        }

    }


    public void initialize(TreeView<String> view) {
        treeView = view;
        if (!databaseData.isEmpty()) {
            constructTree();
        }else {
            rootItem = new TreeItem<>("");
        }
        treeView.setShowRoot(false);
        treeView.setRoot(rootItem);


    }

    private void constructTree() {
        rootItem = new TreeItem<>(database.getName().substring(0, database.getName().length() - 4));

        //need to sort the database data by match number and team position
        databaseData.sort(new MatchDataComparator());//this is actually nessacary as it assures that the robot positon indexes appear sequentially
        List<DataHandler.MatchRecord> tempList = new LinkedList<>(databaseData.stream().toList());
        for (int i = 1; i <= databaseData.get(databaseData.size() - 1).matchNumber(); i++) {
            //for all the matches that there may or may not be data for up to the last match with data for it
            //i will be set to all valid match numbers

            //this is a branch item that will represent a match
            TreeItem<String> matchItem = new TreeItem<>("Match: " + i);



            //now we will loop through the data taking all relevant matches and adding them to this branch
            //then removing them from the temporart dataset to improve effiency
            int robotPositionIndex = 0;

            for (DataHandler.MatchRecord r : tempList) {
                //for each datapoint
                if (r.matchNumber() == i) {
                    if (robotPositionIndex == 0) {
                        rootItem.getChildren().add(matchItem);//we only want to do this if there is data for a match, but only once per match
                    }
                    //if the dataPoint we are looking at right now is the next position sequentially (we haven't skipped a position)
                    //add all this dataPoint's data to leaf items
                    TreeItem<String> positonItem = new TreeItem<String>(r.position().toString());
                    matchItem.getChildren().add(robotPositionIndex, positonItem);
                    robotPositionIndex++;
                }else {
                    //we have gone through all 6 robot positions for this match so it is guarenteed that there will be no more of this match
                    break;
                }
            }//end for each dataPoint loop
            final int finalI = i;
            tempList.removeIf(new Predicate<DataHandler.MatchRecord>() {
                @Override
                public boolean test(DataHandler.MatchRecord matchRecord) {
                    return matchRecord.matchNumber() == finalI;
                }
            });
        }//end for each match loop
    }


}

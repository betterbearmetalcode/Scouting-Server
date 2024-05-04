package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DuplicateData;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

//takes in a list of duplicates
public class DuplicateDataResolverDialog extends Dialog<ArrayList<JSONObject>>{

    ArrayList<CheckBox> checkBoxes = new ArrayList<>();
    public DuplicateDataResolverDialog(ArrayList<DuplicateData> duplicates) {
        this.setTitle("Select Data to Keep");
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK);


        Button toggleAllButton = new Button("Toggle All");
        toggleAllButton.setOnAction(event -> checkBoxes.forEach(checkBox -> checkBox.setSelected(!checkBox.isSelected())));
        VBox mainBox = new VBox(new Label("Some of the data you are trying to add had entrys with the same match and team number already in the database. For each entry, choose whether to keep the old data or overwrite it with the new data."));
        mainBox.getChildren().add(toggleAllButton);
        mainBox.getChildren().add(getVbox(duplicates));
        this.getDialogPane().setContent(new ScrollPane(mainBox));
        this.setResizable(true);
        this.getDialogPane().prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
        this.getDialogPane().setPrefWidth(400);
        this.setResultConverter(param -> {
            ArrayList<JSONObject> output = new ArrayList<>();
            for (int i = 0; i < duplicates.size(); i++) {
                if (checkBoxes.get(i).isSelected()) {
                    //if its selected, the we want to overwrite and should use the new data
                 //  output.add(duplicates.get(i).getNewData());
                }
                //otherwise, if we are not overwriting, the do nothing, as the data is already in the database and there is no need to delete and then add the same thing
            }
            return output;
        });

        //save a backup json of all data just in case the user screws up and delete stuff
/*        JSONArray backup = new JSONArray();
        for (DuplicateDataException duplicate : duplicates) {
            backup.put(duplicate.getOldData());
            backup.put(duplicate.getNewData());
        }


        File outputFile = new File(Constants.BASE_APP_DATA_FILEPATH + "/resources/duplicateDataBackups/Duplicate Data Backup " + new Date().toString().replaceAll(":", " ") + ".json");

        try {
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(backup.toString().getBytes());
            outputStream.flush();
            outputStream.close();
            Logging.logInfo("Created duplicate data backup");
        } catch (IOException e) {
            Logging.logInfo("Failed to create backup of duplicated data");
        }*/
    }

    private VBox getVbox(ArrayList<DuplicateData> duplicates) {
        VBox vbox = new VBox();
        vbox.setSpacing(10);
        for (DuplicateData duplicate : duplicates) {
            CheckBox checkBox = new CheckBox("Overwrite?");
            checkBox.setSelected(true);
            checkBoxes.add(checkBox);
            if (duplicate.duplicateData.isEmpty()) {
                continue;
            }
            HBox box = new HBox(getTree(duplicate),new VBox(checkBox,
                    new Label("Match: " + DatabaseViewerTabContent.getIntFromEntryJSONObject(Constants.SQLColumnName.MATCH_NUM, duplicate.duplicateData.get(0))),
                    new Label("Team: " + DatabaseViewerTabContent.getIntFromEntryJSONObject(Constants.SQLColumnName.TEAM_NUM, duplicate.duplicateData.get(0))),
                    new Label("Position" + DatabaseManager.getRobotPositionFromNum(DatabaseViewerTabContent.getIntFromEntryJSONObject(Constants.SQLColumnName.ALLIANCE_POS, duplicate.duplicateData.get(0))))));
            box.setPrefHeight(300);
           vbox.getChildren().add(box);
        }
        return vbox;

    }

    private TreeView<String> getTree(DuplicateData duplicate) {
        TreeView<String> treeView = new TreeView<>();
        treeView.setEditable(false);
        TreeItem<String> root = new TreeItem<>();
        root.setExpanded(true);
        treeView.setShowRoot(false);
        for (int i = 0; i < duplicate.duplicateData.size(); i++) {
            CheckBoxTreeItem<String> entryItem = new CheckBoxTreeItem<>("Entry: " + i);
            for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
                entryItem.getChildren().add(new TreeItem<>(rawDataMetric.getName() + ": " + duplicate.duplicateData.get(i).opt(rawDataMetric.getDatatypeAsString())));
            }
            entryItem.setSelected(false);
            root.getChildren().add(entryItem);
        }
        treeView.setRoot(root);
        return treeView;
    }

}

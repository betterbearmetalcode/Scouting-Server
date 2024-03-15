package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

//returns list of records to add
public class DuplicateDataResolvedDialog extends Dialog<ArrayList<DatabaseManager.QRRecord>>{

    ArrayList<CheckBox> checkBoxes = new ArrayList<>();
    public DuplicateDataResolvedDialog(ArrayList<DuplicateDataException> duplicates) {
        this.setTitle("Select Data to Keep");
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK);


        Button toggleAllButton = new Button("Toggle All");
        toggleAllButton.setOnAction(event -> checkBoxes.forEach(checkBox -> checkBox.setSelected(!checkBox.isSelected())));
        VBox mainBox = new VBox(new Label("Choose whether to overwrite old data with new data or not"));
        mainBox.getChildren().add(toggleAllButton);
        mainBox.getChildren().add(getVbox(duplicates));
        this.getDialogPane().setContent(new ScrollPane(mainBox));
        this.setResizable(true);
        this.getDialogPane().prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
        this.getDialogPane().setPrefWidth(400);
        this.setResultConverter(param -> {
            ArrayList<DatabaseManager.QRRecord> output = new ArrayList<>();
            for (int i = 0; i < duplicates.size(); i++) {
                if (checkBoxes.get(i).isSelected()) {
                    //if its selected, the we want to overwrite and should use the new data
                    output.add(duplicates.get(i).getNewData());
                }
                //otherwise, if we are not overwriting, the do nothing, as the data is already in the database and there is no need to delete and then add the same thing
            }
            return output;
        });

        //save a backup json of all data just in case the user screws up and delete stuff
        JSONObject backup = new JSONObject();
        for (DatabaseManager.RobotPosition position : DatabaseManager.RobotPosition.values()) {
            JSONArray positionArray = new JSONArray();
            for (DuplicateDataException duplicate : duplicates) {
                if (duplicate.getOldData().position() == position) {
                    positionArray.put(duplicate.getOldData().getQRString());
                }
                if (duplicate.getNewData().position() == position) {
                    positionArray.put(duplicate.getNewData().getQRString());
                }
            }
            backup.put(position.name(), positionArray);
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
        }
    }

    private VBox getVbox(ArrayList<DuplicateDataException> duplicates) {
        VBox vbox = new VBox();
        vbox.setSpacing(10);
        for (DuplicateDataException duplicate : duplicates) {
            CheckBox checkBox = new CheckBox("Overwrite?");
            checkBox.setSelected(true);
            checkBoxes.add(checkBox);
            HBox box = new HBox(getTree(duplicate),new VBox(checkBox, new Label("Match: " + duplicate.getOldData().matchNumber()), new Label("Team: " + duplicate.getOldData().teamNumber()), new Label("Position" + duplicate.getOldData().position().name())) );
            box.setPrefHeight(125);
           vbox.getChildren().add(box);
        }
        return vbox;

    }

    private TreeView<String> getTree(DuplicateDataException duplicate) {
        TreeView<String> treeView = new TreeView<>();
        treeView.setEditable(false);
        TreeItem<String> root = new TreeItem<>();
        root.setExpanded(true);
        treeView.setShowRoot(false);
        TreeItem<String> oldItem = new TreeItem<>();
        for (DataPoint dataPoint : duplicate.getOldData().getDataAsList()) {
            oldItem.getChildren().add(new TreeItem<>(dataPoint.toString()));
        }
        oldItem.setValue("Old Data");
        root.getChildren().add(oldItem);
        TreeItem<String> newItem = new TreeItem<>();
        for (DataPoint dataPoint : duplicate.getNewData().getDataAsList()) {
            newItem.getChildren().add(new TreeItem<>(dataPoint.toString()));
        }
        newItem.setValue("New Data");
        root.getChildren().add(newItem);
        treeView.setRoot(root);
        return treeView;
    }

}

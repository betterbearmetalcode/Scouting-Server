package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.controlsfx.control.CheckTreeView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.data.DuplicateData;
import org.tahomarobotics.scouting.scoutingserver.util.data.DuplicateResolution;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

//takes in a list of duplicates
public class DuplicateDataResolverDialog extends Dialog<ArrayList<DuplicateResolution>>{

    private TreeView<String> treeView = new TreeView<>();

    private ArrayList<ToggleGroup> toggleGroups = new ArrayList<>();
    TreeItem<String> root = new TreeItem<>();
    public DuplicateDataResolverDialog(ArrayList<DuplicateData> duplicates) {
        this.setTitle("Select Data to Keep");
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK);


        Button toggleAllButton = new Button("Toggle All");
        VBox mainBox = new VBox(new Label("Some of the data you are trying to add had entries with the same match and team number already in the database. For each entry, choose whether to keep the old data or overwrite it with the new data."));
        mainBox.getChildren().add(toggleAllButton);
        setUpTree(duplicates);
        mainBox.getChildren().add(treeView);
        this.getDialogPane().setContent(new ScrollPane(mainBox));
        this.setResizable(true);
        this.getDialogPane().prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
        this.getDialogPane().setPrefWidth(400);
        this.setResultConverter(param -> {
            ArrayList<DuplicateResolution> output = new ArrayList<>();
            for (int i = 0; i < duplicates.size(); i++) {
                for (int j = 0; j < toggleGroups.get(i).getToggles().size(); j++) {
                    if (toggleGroups.get(i).getToggles().get(j).isSelected()) {
                        output.add(new DuplicateResolution(duplicates.get(i).matchNum(), duplicates.get(i).teamNum(), duplicates.get(i).data().get(j)));
                    }
                }
            }
            return output;
        });

        //save a backup json of all data just in case the user screws up and delete stuff
        JSONArray backup = new JSONArray();
        for (DuplicateData duplicate : duplicates) {
            for (JSONObject datum : duplicate.data()) {
                backup.put(datum);
            }
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

    private void setUpTree(ArrayList<DuplicateData> duplicates) {

        treeView.setEditable(false);
        root.setExpanded(true);
        treeView.setShowRoot(false);
        for (DuplicateData duplicate : duplicates) {
            TreeItem<String> duplicateItem = new TreeItem<>("Match: " + duplicate.matchNum() + " Team: " + duplicate.teamNum());
            duplicateItem.setExpanded(true);
            ToggleGroup group = new ToggleGroup();
            toggleGroups.add(group);
            for (int i = 0; i < duplicate.data().size(); i++) {
                TreeItem<String> datumItem = new TreeItem<>("Duplicate Datum: " + i + " Alliance Pos: " + DatabaseManager.RobotPosition.values()[duplicate.data().get(i).getJSONObject(Constants.SQLColumnName.ALLIANCE_POS.name()).optInt("0",0)]);
                RadioButton radioButton = new RadioButton();
                if (i == 0) {
                    radioButton.setSelected(true);
                }
                group.getToggles().add(radioButton);
                datumItem.setGraphic(radioButton);
                for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
                    TreeItem<String> leafItem = new TreeItem<>(rawDataMetric.getName() + ": " + duplicate.data().get(i).getJSONObject(rawDataMetric.getName()).optString(rawDataMetric.getDatatypeAsString(),"Error"));
                    datumItem.getChildren().add(leafItem);
                }
                duplicateItem.getChildren().add(datumItem);
            }
            root.getChildren().add(duplicateItem);
        }

        treeView.setRoot(root);
    }


}

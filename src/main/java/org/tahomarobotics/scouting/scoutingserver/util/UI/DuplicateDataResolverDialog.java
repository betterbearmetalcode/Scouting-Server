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
import org.tahomarobotics.scouting.scoutingserver.util.data.DuplicateEntries;
import org.tahomarobotics.scouting.scoutingserver.util.data.DuplicateResolution;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

//takes in a list of duplicates
public class DuplicateDataResolverDialog extends Dialog<ArrayList<DuplicateResolution>>{

    private TreeView<String> treeView = new TreeView<>();

    private ArrayList<ToggleGroup> toggleGroups = new ArrayList<>();
    TreeItem<String> root = new TreeItem<>();

    //optimzied a bit by asking the user if they want to create a duplicate data backup

    /**
     * Constructs a Duplicate Data Resolver Dialog to help the user decide which data they want to keep
     * Sometimes there are data entries with identical match and team numbers in the new data or already in the dataset.
     * This dialog forces the user to decide which entry of each set of conflicting entries they want to keep.
     * The other entries are discarded, or saved at the user's request and the selected entries are imported.
     * Much of this process takes place in {@link DatabaseManager#importJSONArrayOfDataObjects(JSONArray, String)}
     * @see DatabaseManager#importJSONArrayOfDataObjects(JSONArray, String)
     * @param duplicates An array of {@link DuplicateEntries} objects which need to be resolved
     */
    public DuplicateDataResolverDialog(ArrayList<DuplicateEntries> duplicates) {
        Logging.logInfo("Throwing up duplicate data resolver dialog");
        this.setTitle("Select Data to Keep");
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        Button toggleAllButton = new Button("Toggle All");
        CheckBox createDuplicateBackupCheckbox = new CheckBox("Create duplicate data backup in case the duplication resoultion needs to be revisited?");
        createDuplicateBackupCheckbox.setSelected(false);
        VBox mainBox = new VBox(new Label("Some of the data you are trying to add had entries with the same match and team number already in the database. For each entry, choose whether to keep the old data or overwrite it with the new data."));
        mainBox.getChildren().add(new HBox(toggleAllButton, createDuplicateBackupCheckbox));
        setUpTree(duplicates);
        mainBox.getChildren().add(treeView);
        this.getDialogPane().setContent(new ScrollPane(mainBox));
        this.setResizable(true);
        this.getDialogPane().prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
        this.getDialogPane().setPrefWidth(400);


        this.setResultConverter(param -> {
            //1100 ms to do this
            if (createDuplicateBackupCheckbox.isSelected()) {
                //save a backup json of all data just in case the user screws up and delete stuff
                JSONArray backup = new JSONArray();
                for (DuplicateEntries duplicate : duplicates) {
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

            //3 ms to do this
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


    }

    private void setUpTree(ArrayList<DuplicateEntries> duplicates) {

        treeView.setEditable(false);
        root.setExpanded(true);
        treeView.setShowRoot(false);
        for (DuplicateEntries duplicate : duplicates) {
            TreeItem<String> duplicateItem = new TreeItem<>("Match: " + duplicate.matchNum() + " Team: " + duplicate.teamNum());
            duplicateItem.setExpanded(true);
            ToggleGroup group = new ToggleGroup();
            toggleGroups.add(group);
            for (int i = 0; i < duplicate.data().size(); i++) {
                TreeItem<String> datumItem = new TreeItem<>("Duplicate Datum: " + (i+1) + " Alliance Pos: " + DatabaseManager.RobotPosition.values()[duplicate.data().get(i).getJSONObject(Constants.SQLColumnName.ALLIANCE_POS.name()).optInt("0",0)]);
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

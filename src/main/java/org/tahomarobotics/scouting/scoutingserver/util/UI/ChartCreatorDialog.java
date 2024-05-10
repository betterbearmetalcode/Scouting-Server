package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.controller.MasterController;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import static org.tahomarobotics.scouting.scoutingserver.Constants.SQLColumnName.*;

public class ChartCreatorDialog extends Dialog<Optional<ChartTabContent>> {

    //needs to be able to have you select what sql colmns are going to be used as well as what the chart name should be and what the y axis and x axis should be named


    private final ArrayList<CheckBox> checkBoxes = new ArrayList<>();

    private final TextField xAxisNameField = new TextField("Teams");
    private final TextField yAzisNameField = new TextField("Y Axis");

    private final TextField chartTitleField = new TextField("Title");

    private final TextField fileNameField = new TextField();

    private final TextField saveLocationField = new TextField();

    private Optional<File> dataSourceFile = Optional.empty();

    private Optional<File> saveLocationFile = Optional.empty();



    public ChartCreatorDialog() {
        this.setTitle("Create Chart");
        ButtonType openType = new ButtonType("Open", ButtonType.OK.getButtonData());
        this.getDialogPane().getButtonTypes().addAll(openType, ButtonType.CANCEL);

        VBox chartSettings = new VBox();
        chartSettings.getChildren().add(new Label("Select Metrics to track"));
        for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
            if (Objects.equals(rawDataMetric.getName(), TEAM_NUM.name()) || Objects.equals(rawDataMetric.getName(), MATCH_NUM.name()) || Objects.equals(rawDataMetric.getName(), ALLIANCE_POS.name())) {
                //un graphable things
                continue;
            }
            if (rawDataMetric.getDatatype() != Configuration.Datatype.INTEGER) {
                continue;
            }
            CheckBox box = new CheckBox(rawDataMetric.getName());
            box.setSelected(false);
            checkBoxes.add(box);
            chartSettings.getChildren().add(box);
        }
        chartSettings.getChildren().add(new HBox(new Label("Chart Title: "), chartTitleField));
        chartSettings.getChildren().add(new HBox(new Label("X Axis Name"), xAxisNameField));
        chartSettings.getChildren().add(new HBox(new Label("Y Axis Name"), yAzisNameField));


        Button selectFileButton = Constants.getButtonWithIcon(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/icons/file-selector-icon.png"), "Select File");
        selectFileButton.setOnAction(event -> {
            File potentialFile = Constants.selectDataFile("Select Data Source File", false);
            if (potentialFile != null) {
                dataSourceFile = Optional.of(potentialFile);
            }
            dataSourceFile.ifPresent(file -> fileNameField.setText(file.getAbsolutePath()));

        });

        fileNameField.setEditable(false);
        fileNameField.setPromptText("Select Data file you would like to use");
        HBox sourceFileBox = new HBox(new Label("Data Source File"), fileNameField, selectFileButton);
        chartSettings.getChildren().add(sourceFileBox);
        saveLocationField.setEditable(false);
        saveLocationField.setPromptText("Select Save Location");

        Button selectSaveLocationButton = Constants.getButtonWithIcon(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/icons/file-selector-icon.png"), "Select File");
        selectSaveLocationButton.setOnAction(event -> {
            File potentialFile = Constants.selectDataFile("Select Save Location", false);
            if (potentialFile != null) {
                saveLocationFile = Optional.of(potentialFile);
            }
            saveLocationFile.ifPresent(file -> saveLocationField.setText(file.getAbsolutePath()));

        });
        HBox saveLocation = new HBox(new Label("Save Location"), saveLocationField, selectFileButton);
        chartSettings.getChildren().add(saveLocation);

        this.getDialogPane().setContent(chartSettings);
        this.setResultConverter(param -> {
            if (param == openType) {
                ArrayList<DataMetric> dataMetrics = new ArrayList<>();
                checkBoxes.forEach(checkBox -> {
                    if (checkBox.isSelected()) {
                        Optional<DataMetric> potentialMetric = Configuration.getMetric(checkBox.getText());
                        potentialMetric.ifPresent(dataMetrics::add);
                    }

                });
                if (dataMetrics.isEmpty()) {
                    return Optional.empty();
                }
                CategoryAxis xAxis = new CategoryAxis();
                xAxis.setLabel(xAxisNameField.getText().isEmpty()?"X Axis":xAxisNameField.getText());
                NumberAxis yAxis = new NumberAxis();
                yAxis.setLabel(yAzisNameField.getText().isEmpty()?"Y Axis":yAzisNameField.getText());
                String title = chartTitleField.getText().isEmpty()?"Chart":chartTitleField.getText();
                return Optional.of(new ChartTabContent(title, saveLocationFile,dataSourceFile, dataMetrics));

            }else {

                return Optional.empty();
            }

        });

    }
}

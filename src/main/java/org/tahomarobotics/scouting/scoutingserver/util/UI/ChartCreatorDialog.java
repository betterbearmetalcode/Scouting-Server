package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.controller.MasterController;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

import static org.tahomarobotics.scouting.scoutingserver.Constants.SQLColumnName.*;

public class ChartCreatorDialog extends Dialog<Optional<ChartTabContent>> {

    //needs to be able to have you select what sql colmns are going to be used as well as what the chart name should be and what the y axis and x axis should be named


    private final ArrayList<CheckBox> checkBoxes = new ArrayList<>();

    private final TextField xAxisNameField = new TextField("Teams");
    private final TextField yAzisNameField = new TextField("Y Axis");

    private final TextField chartTitleField = new TextField("Title");

    private final TextField fileNameField = new TextField();



    public ChartCreatorDialog() {
        this.setTitle("Create Chart");
        ButtonType openType = new ButtonType("Open", ButtonType.OK.getButtonData());
        this.getDialogPane().getButtonTypes().addAll(openType, ButtonType.CANCEL);

        VBox chartSettings = new VBox();
        chartSettings.getChildren().add(new Label("Select Metrics to track"));
        for (Constants.SQLColumnName value : Constants.SQLColumnName.values()) {
            //allow user to select these columns
            if (!(value.equals(AUTO_SPEAKER) ||
                    value.equals(AUTO_AMP) ||
                    value.equals(AUTO_SPEAKER_MISSED) ||
                    value.equals(AUTO_AMP_MISSED) ||
                    value.equals(TELE_SPEAKER) ||
                    value.equals(TELE_AMP) ||
                    value.equals(TELE_TRAP) ||
                    value.equals(TELE_SPEAKER_MISSED) ||
                    value.equals(TELE_AMP_MISSED) ||
                    value.equals(SPEAKER_RECEIVED) ||
                    value.equals(AMP_RECEIVED))
                    ) {
                continue;
            }
            CheckBox box = new CheckBox(value.name());
            box.setSelected(false);
            checkBoxes.add(box);
            chartSettings.getChildren().add(box);
        }
        chartSettings.getChildren().add(new HBox(new Label("Chart Title: "), chartTitleField));
        chartSettings.getChildren().add(new HBox(new Label("X Axis Name"), xAxisNameField));
        chartSettings.getChildren().add(new HBox(new Label("Y Axis Name"), yAzisNameField));


        Button selectFileButton = new Button();
        selectFileButton.setTooltip(new Tooltip("Select File"));
        File selectImageFile = new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/icons/file-selector-icon.png");
        Image selectFileImage = new Image(selectImageFile.toURI().toString());
        ImageView selectFileImageView = new ImageView();
        selectFileImageView.setImage(selectFileImage);
        selectFileButton.setGraphic(selectFileImageView);
        selectFileButton.setOnAction(event -> MasterController.openJSONFileIntoDatabase());

        fileNameField.setEditable(false);
        fileNameField.setPromptText("Select Data file you would like to use");
        HBox fileBox = new HBox(fileNameField, selectFileButton);


        this.getDialogPane().setContent(chartSettings);
        this.setResultConverter(param -> {
            if (param == openType) {
                String tableName = "wheep whoop";
                ArrayList<Constants.SQLColumnName> columnNames = new ArrayList<>();
                checkBoxes.forEach(checkBox -> {
                    if (checkBox.isSelected()) {
                        columnNames.add(Constants.SQLColumnName.valueOf(checkBox.getText()));
                    }

                });
                if (columnNames.isEmpty()) {
                    return null;
                }
                CategoryAxis xAxis = new CategoryAxis();
                xAxis.setLabel(xAxisNameField.getText().isEmpty()?"X Axis":xAxisNameField.getText());
                NumberAxis yAxis = new NumberAxis();
                yAxis.setLabel(yAzisNameField.getText().isEmpty()?"Y Axis":yAzisNameField.getText());
                String title = chartTitleField.getText().isEmpty()?"Chart":chartTitleField.getText();
                return Optional.of(new ChartTabContent(title, Optional.of(new File("bloop")), new File("another file"), new ArrayList<>()));

            }else {

                return Optional.empty();
            }

        });

    }
}

package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.Chart;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import static org.tahomarobotics.scouting.scoutingserver.Constants.SQLColumnName.*;

public class ChartCreatorDialog extends Dialog<Chart> {

    //needs to be able to have you select what sql colmns are going to be used as well as what the chart name should be and what the y axis and x axis should be named


    private final ListView<String> tableChooserListView;
    private final ArrayList<CheckBox> checkBoxes = new ArrayList<>();

    private TextField xAxisNameField = new TextField("Teams");
    private TextField yAzisNameField = new TextField("Y Axis");

    private TextField chartTitleField = new TextField("Title");



    public ChartCreatorDialog(ArrayList<String> tables) {
        this.setTitle("Create Chart");
        ButtonType openType = new ButtonType("Open", ButtonType.OK.getButtonData());
        this.getDialogPane().getButtonTypes().addAll(openType, ButtonType.CANCEL);

        tableChooserListView = new ListView<>();
        tableChooserListView.setEditable(true);
        tableChooserListView.setOnEditCommit(event -> {
            String oldValue = event.getSource().getItems().get(event.getIndex());
            if (!Objects.equals(oldValue, event.getNewValue())) {
                try {
                    tableChooserListView.getItems().set(event.getIndex(), event.getNewValue());
                    SQLUtil.execNoReturn("ALTER TABLE \"" + oldValue + "\" RENAME TO \"" + event.getNewValue() + "\"");
                } catch (SQLException  e) {
                    Logging.logError(e);
                }
            }


        });
        VBox mainBox = new VBox();
        SplitPane pane = new SplitPane();
        mainBox.getChildren().add(pane);
        VBox tableChooseVbox = getTableChooseVbox(tableChooserListView);
        tableChooserListView.setCellFactory(TextFieldListCell.forListView());
        tableChooserListView.getItems().addAll(tables);
        tableChooserListView.getSelectionModel().select(0);
        pane.getItems().add(tableChooseVbox);

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


        pane.getItems().add(chartSettings);
        this.getDialogPane().setContent(mainBox);
        this.setResultConverter(param -> {
            if (param == openType) {
                tableChooserListView.refresh();
                if (tableChooserListView.getSelectionModel().getSelectedItem() == null) {
                    return null;
                }else {
                    String tableName = tableChooserListView.getSelectionModel().getSelectedItem();
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
                    return new Chart(tableName, columnNames, xAxis, yAxis, title);
                }

            }else {

                return null;
            }

        });

    }

    private static VBox getTableChooseVbox(ListView<String> listView) {
        Button newCompetitionButton = new Button("New Competition");

        newCompetitionButton.setOnAction(event -> {
            try {
                String name = "New Database";
                listView.getItems().add(name);
                SQLUtil.addTableIfNotExists(name);
            } catch (SQLException  e) {
                Logging.logError(e);
            }
        });
        Button duplicateButton = getButton(listView);
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            try {
                SQLUtil.execNoReturn("DROP TABLE IF EXISTS '" + listView.getSelectionModel().getSelectedItem() + "'");
                listView.getItems().remove(listView.getSelectionModel().getSelectedItem());
            } catch (SQLException  e) {
                Logging.logError(e);
            }
        });
        FlowPane pane = new FlowPane(newCompetitionButton, duplicateButton, deleteButton);
        return new VBox(new Label("Competition"), listView, pane);
    }

    private static Button getButton(ListView<String> listView) {
        Button duplicateButton = new Button("Duplicate");
        duplicateButton.setOnAction(event -> {
            try {
                String name = listView.getSelectionModel().getSelectedItem() + "-Copy";
                while (true) {
                    if (listView.getItems().contains(name)) {
                        name = name.concat("-Copy ");
                    } else {
                        break;
                    }
                }


                SQLUtil.execNoReturn("CREATE TABLE '" + name + "' AS  SELECT *  FROM '" + listView.getSelectionModel().getSelectedItem() + "'");
                listView.getItems().add(name);
            } catch (SQLException  e) {
                Logging.logError(e);
            }
        });
        return duplicateButton;
    }
}

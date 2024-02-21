package org.tahomarobotics.scouting.scoutingserver.util;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.tahomarobotics.scouting.scoutingserver.Constants;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

public class TableChooserDialog extends Dialog<String> {
    private final ListView<String> listView;
    public TableChooserDialog(ArrayList<String> tables) {
        this.setTitle("Select Competition");
        this.setHeaderText("Do not include spaces in names");
        ButtonType openType = new ButtonType("Open", ButtonType.OK.getButtonData());
        ButtonType doneType = new ButtonType("Done", ButtonType.CANCEL.getButtonData());
        this.getDialogPane().getButtonTypes().addAll(openType, doneType, ButtonType.CANCEL);

        listView = new ListView<>();
        listView.setEditable(true);
        listView.setOnEditCommit(event -> {
            System.out.println("attempting to edit competions");
            String oldValue = event.getSource().getItems().get(event.getIndex());
            if (!Objects.equals(oldValue, event.getNewValue())) {
                try {
                    listView.getItems().set(event.getIndex(), event.getNewValue());
                    DatabaseManager.execNoReturn("ALTER TABLE \"" +  oldValue + "\" RENAME TO \"" + event.getNewValue() + "\"");

                    System.out.println("edit succesfully  commited");
                } catch (SQLException e) {
                    Logging.logError(e);
                }
            }else {
                System.out.println("No changes made");
            }


        });

        VBox vBox = getvBox(listView);

        listView.setCellFactory(TextFieldListCell.forListView());
        listView.getItems().addAll(tables);
        listView.getSelectionModel().select(0);
        this.getDialogPane().setContent(vBox);
        this.setResultConverter(param -> {
            if (param == openType) {
                listView.refresh();

                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

    }

    private static VBox getvBox(ListView<String> listView) {
        Button newCompetitionButton = new Button("New Competition");

        newCompetitionButton.setOnAction(event -> {
            try {
                String name = "New Database";
                listView.getItems().add(name);
                DatabaseManager.addTable(name, DatabaseManager.createTableSchem(Constants.RAW_TABLE_SCHEMA));
            } catch (SQLException e) {
                Logging.logError(e);
            }
        });
        Button duplicateButton = new Button("Duplicate");
        duplicateButton.setOnAction(event -> {
            try {
                String name = listView.getSelectionModel().getSelectedItem() + "-Copy";
                while (true) {
                    if (listView.getItems().contains(name)) {
                        name = name.concat("-Copy ");
                    }else {
                        break;
                    }
                }


                DatabaseManager.execNoReturn("CREATE TABLE \'" + name + "\' AS  SELECT *  FROM \'" + listView.getSelectionModel().getSelectedItem() + "\'");
                listView.getItems().add(name);
            } catch (SQLException e) {
                Logging.logError(e);
            }
        });
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    if (listView.getSelectionModel().getSelectedItem().equals(Constants.DEFAULT_SQL_TABLE_NAME)) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Default Table Cannot be Deleted");
                        alert.showAndWait();
                        return;
                    }
                    DatabaseManager.execNoReturn("DROP TABLE IF EXISTS \'" + listView.getSelectionModel().getSelectedItem() + "\'");
                    listView.getItems().remove(listView.getSelectionModel().getSelectedItem());
                } catch (SQLException e) {
                    Logging.logError(e);
                }
            }
        });
        FlowPane pane = new FlowPane(newCompetitionButton, duplicateButton, deleteButton);
        return new VBox(listView, pane);
    }


}

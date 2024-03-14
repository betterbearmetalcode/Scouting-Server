package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

public class TableChooserDialog extends Dialog<String> {
    private final ListView<String> listView;

    public TableChooserDialog(ArrayList<String> tables) {
        this.setTitle("Select Competition");
        ButtonType openType = new ButtonType("Open", ButtonType.OK.getButtonData());
        ButtonType doneType = new ButtonType("Done", ButtonType.CANCEL.getButtonData());
        this.getDialogPane().getButtonTypes().addAll(openType, doneType, ButtonType.CANCEL);

        listView = new ListView<>();
        listView.setEditable(true);
        listView.setOnEditCommit(event -> {
            String oldValue = event.getSource().getItems().get(event.getIndex());
            if (!Objects.equals(oldValue, event.getNewValue())) {
                try {
                    String processedNewValue = event.getNewValue().replaceAll("'", "");//we can't use ' in names as it breaks sql
                    SQLUtil.execNoReturn("ALTER TABLE \"" + oldValue + "\" RENAME TO \"" + processedNewValue + "\"");
                    listView.getItems().set(event.getIndex(), processedNewValue);

                } catch (SQLException | DuplicateDataException e) {
                    Logging.logError(e);
                }
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
                if (listView.getSelectionModel().getSelectedItem() == null) {
                    return "";
                }else {
                    try {
                        SQLUtil.addTableIfNotExists(listView.getSelectionModel().getSelectedItem(), SQLUtil.createTableSchem(Constants.RAW_TABLE_SCHEMA));
                    } catch (SQLException | DuplicateDataException e) {
                        Logging.logError(e, "Failed to create table");
                        return "";
                    }
                    return listView.getSelectionModel().getSelectedItem();
                }

            }
            return null;
        });

    }

    private static VBox getvBox(ListView<String> listView) {
        Button newCompetitionButton = new Button("New Competition");

        newCompetitionButton.setOnAction(event -> {
            try {
                String name = "New Database";
                SQLUtil.addTableIfNotExists(name, SQLUtil.createTableSchem(Constants.RAW_TABLE_SCHEMA));
                listView.getItems().add(name);

            } catch (SQLException | DuplicateDataException e) {
                Logging.logError(e);
            }
        });
        Button clearDatabaseButton = new Button("Clear Database");
        clearDatabaseButton.setOnAction(event -> {
            try {
                SQLUtil.execNoReturn("DELETE FROM \"" + listView.getSelectionModel().getSelectedItem() + "\"");
            } catch (SQLException | DuplicateDataException e) {
                Logging.logError(e);
            }
        });
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            try {
                SQLUtil.execNoReturn("DROP TABLE IF EXISTS '" + listView.getSelectionModel().getSelectedItem() + "'");
                listView.getItems().remove(listView.getSelectionModel().getSelectedItem());
            } catch (SQLException | DuplicateDataException e) {
                Logging.logError(e);
            }
        });
        FlowPane pane = new FlowPane(newCompetitionButton, clearDatabaseButton, deleteButton);
        return new VBox(listView, pane);
    }


}

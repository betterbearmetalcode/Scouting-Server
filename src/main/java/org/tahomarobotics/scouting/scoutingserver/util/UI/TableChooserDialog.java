package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

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

                } catch (SQLException  e) {
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
                        SQLUtil.addTableIfNotExists(listView.getSelectionModel().getSelectedItem());
                    } catch (SQLException  e) {
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
                int counter = 0;
                String name = "New Database";
                //this loop is to prevent the list from having multiple databases with the same name in the while in reality there
                //is only one in database with that name
                while (true) {
                    if (listView.getItems().contains(name)) {
                        name = name + " -copy";
                    }else {
                        break;
                    }
                    counter++;
                    if (counter > 100) {
                        //I don't want the app to ever get stuck here, i only expect this to happen if the user clicks new database 100 times
                        Logging.logInfo("You are a ding dong, delete or rename some databases", true);
                        return;
                    }
                }


                SQLUtil.addTableIfNotExists(name);
                listView.getItems().add(name);

            } catch (SQLException  e) {
                Logging.logError(e);
            }
        });
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            try {
                if (!Constants.askQuestion("Are you sure?")) {
                    return;
                }
                SQLUtil.execNoReturn("DROP TABLE IF EXISTS '" + listView.getSelectionModel().getSelectedItem() + "'");
                listView.getItems().remove(listView.getSelectionModel().getSelectedItem());
            } catch (SQLException  e) {
                Logging.logError(e);
            }
        });
        FlowPane pane = new FlowPane(newCompetitionButton, deleteButton);
        return new VBox(listView, pane);
    }


}

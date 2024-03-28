package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.controlsfx.control.CheckListView;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.auto.HeatmapCreationInformation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.jar.JarEntry;

public class AutoHeatMapCreatorDialog extends Dialog<HeatmapCreationInformation> {
    private  final int spacing = 10;

    private HashMap<DatabaseManager.RobotPosition, TextField> teamFields = new HashMap<>();

    private CheckListView<String> databaseView;
    public AutoHeatMapCreatorDialog() throws SQLException {
       setUpGUI();
       this.setResultConverter(param -> {
           if (param == ButtonType.CANCEL) {
               return null;
           }else {
               HashMap<DatabaseManager.RobotPosition, String> teams = new HashMap<>();
               teamFields.keySet().forEach(robotPosition -> teams.put(robotPosition, teamFields.get(robotPosition).getText()));
                return new HeatmapCreationInformation(teams, new ArrayList<>(databaseView.getCheckModel().getCheckedItems().stream().toList()));
           }
       });

    }

    private void setUpGUI() throws SQLException {
        this.setTitle("Generate Auto Heatmap");


        databaseView = new CheckListView<>();
        databaseView.getItems().addAll(SQLUtil.getTableNames());
        databaseView.setEditable(false);
        databaseView.getCheckModel().check(0);


        VBox redAllianceBox = new VBox(new Label("Red Alliance: "), getTeamTextField(DatabaseManager.RobotPosition.R1), getTeamTextField(DatabaseManager.RobotPosition.R2), getTeamTextField(DatabaseManager.RobotPosition.R3));
        redAllianceBox.setSpacing(spacing);

        VBox blueAllianceBox = new VBox(new Label("Blue Alliance: "), getTeamTextField(DatabaseManager.RobotPosition.B1), getTeamTextField(DatabaseManager.RobotPosition.B2), getTeamTextField(DatabaseManager.RobotPosition.B3));
        blueAllianceBox.setSpacing(spacing);

        HBox allianceBox = new HBox(redAllianceBox, new Separator(Orientation.VERTICAL), blueAllianceBox);
        allianceBox.setSpacing(spacing);
        VBox settingsBox = new VBox(allianceBox,new Separator());
        settingsBox.setSpacing(spacing);
        HBox rootNode = new HBox(new VBox(new Label("Select Databases to pull data from: "), databaseView), new Separator(Orientation.VERTICAL), settingsBox);
        rootNode.setSpacing(spacing);
        this.getDialogPane().setContent(rootNode);
        ButtonType createHeatMap = new ButtonType( "Create Heatmap", ButtonType.OK.getButtonData());
        this.getDialogPane().getButtonTypes().addAll(createHeatMap, ButtonType.CANCEL);

        final Button button = (Button) this.getDialogPane().lookupButton(createHeatMap);
        button.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    // Check whether some conditions are fulfilled
                    if (databaseView.getCheckModel().getCheckedItems().isEmpty()) {
                        Logging.logInfo("Select as least one data source", true);
                        event.consume();
                    }
                    //dont check for this, its not nessacary
                    /*else if (teamFields.keySet().stream().anyMatch(robotPosition -> Objects.equals(teamFields.get(robotPosition).getText(), ""))) {
                        //if there is at least one field with  no team number
                        Logging.logInfo("Please fill out all team number fields",true);
                        event.consume();
                    }*/
                }
        );
    }

    private HBox getTeamTextField(DatabaseManager.RobotPosition position) {
        TextField textField = new TextField();
        UnaryOperator<TextFormatter.Change> numberValidationFormatter = change -> {
            if((change.getText().matches("\\d+") || change.getText().matches(""))  &&  (change.getRangeStart() < 4)){
                return change; //if change is a number or is nothing (allows for backspaces) and is not longer than 4 charecters
            } else {
                change.setText(""); //else make no change
                change.setRange(    //don't remove any selected text either.
                        change.getRangeStart(),
                        change.getRangeStart()
                );
                return change;
            }
        };
        textField.setTextFormatter(new TextFormatter<>(numberValidationFormatter));
        textField.setPromptText("Team #");
        textField.setMaxWidth(55);
        textField.setMinWidth(55);
        textField.setPrefWidth(55);
        teamFields.put(position, textField);
        HBox output = new HBox(textField);
        output.getChildren().add((position.ordinal() < 3)?0:1, new Label(position.name()));
        output.setSpacing(spacing);
        return output;
    }

}

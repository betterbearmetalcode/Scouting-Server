package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.Chart;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import javax.swing.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.tahomarobotics.scouting.scoutingserver.Constants.SQLColumnName.*;

public class NewItemDialog extends Dialog<GenericTabContent> {

    //this is created whenever the user clicks file>new or the plus button in the main tab pane
    //right now you can create a new database, or create a chart bases off of some existing databases.
    //if I had time or was doing this before comp season I would also have included auto heatmaps as a thing you can create

    public NewItemDialog() {
        this.setTitle("New Item");
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox mainBox = new VBox();
        //a radio button for each type of Item you could create
        //so charts and database viewers
        RadioButton chartRadioButton = new RadioButton("New Chart");
        RadioButton databaseViewerButton = new RadioButton("New Database");


        TextField databaseNameField = new TextField("Database");
        ToggleGroup toggleGroup = new ToggleGroup();
        chartRadioButton.setToggleGroup(toggleGroup);
        databaseViewerButton.setToggleGroup(toggleGroup);
        databaseViewerButton.setSelected(true);
        databaseViewerButton.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (t1) {
                databaseNameField.setDisable(false);
            }else {
                databaseNameField.setDisable(true);
            }
        });
        mainBox.getChildren().add(new Label("Select Which type of Item you would like to create."));
        mainBox.getChildren().addAll(new Separator(Orientation.HORIZONTAL), databaseViewerButton, databaseNameField, new Separator(Orientation.HORIZONTAL), chartRadioButton);
        this.getDialogPane().setContent(mainBox);
        this.setResultConverter(param -> {
            if (param == ButtonType.OK) {
                if (chartRadioButton.isSelected()) {
                    //create a chart
                }else {
                    //create a database viewer
                    String text = databaseNameField.getText();
                    if (text.isEmpty()) {
                        text = "New Database";
                    }
                    return new DatabaseViewerTabContent(text);


                }
                return null;
            }else {

                return null;
            }

        });

    }

}

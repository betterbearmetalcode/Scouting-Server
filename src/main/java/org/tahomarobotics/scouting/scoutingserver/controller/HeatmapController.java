/*
package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.UI.AutoHeatMapCreatorDialog;
import org.tahomarobotics.scouting.scoutingserver.util.auto.HeatmapCreationInformation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class HeatmapController{
    @FXML
    public VBox dislpayBox;


    public void newHeatmap(HeatmapCreationInformation information) {


            //essentiall generates and displays a auto heatmap object
            //select which databases to use for heatmaps, ie, be able to use data from old competitions to early in the comp we can use our data from old comps
            //it could automatically select teams from a match schedule, but that takes internet and it would be better to be able
            //to customize teams for elim matches and speculation and not have to use internet

            //data presentation
            //this function will have to present the data collected somehow
            //it will plot the auto for different teams on a picture of the field you ought to be able to save this image as an export

            //first gather the data that will be used
        //generate heat map
        try {
            AutoHeatmap heatmap = new AutoHeatmap(information);
            //display heatmap
            dislpayBox.getChildren().clear();
            dislpayBox.getChildren().add(heatmap.getDisplayBox());
            dislpayBox.prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
            System.out.println(heatmap);
        } catch (SQLException | IOException e) {
            Logging.logError(e);
        }


    }

    public void newHeatnapButtonEventHandler(ActionEvent event) throws SQLException {
            AutoHeatMapCreatorDialog dialog = new AutoHeatMapCreatorDialog();
            Optional<HeatmapCreationInformation> result = dialog.showAndWait();
            result.ifPresent(this::newHeatmap);
    }


}
*/

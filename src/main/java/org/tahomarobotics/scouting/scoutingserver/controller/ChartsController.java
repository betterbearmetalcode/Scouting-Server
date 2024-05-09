package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.Chart;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.ChartCreatorDialog;

import java.awt.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class ChartsController  {
    @FXML
    public VBox mainBox;


    Chart chart;

    @FXML
    public  TabPane tabPane;


    public  enum Category {
        AUTO_NOTES,
        TELE_NOTES
    }
 /*   public void newTab(ActionEvent event) {
        Logging.logInfo("Making new tab");
        try {
            Tab tab = new Tab();
            tab.setClosable(true);

            ChartCreatorDialog dialog = new ChartCreatorDialog();
            Optional<Chart> result = dialog.showAndWait();

            result.ifPresent(chart -> {
                //add chart to tab

                tab.setId(chart.title);
                tab.setText(chart.title);
                tab.setContent(new VBox(chart));
                chart.prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
                try {
                    tabPane.setTabMaxHeight(Toolkit.getDefaultToolkit().getScreenSize().height);
                    tab.setClosable(true);
                    chart.update();
                    tabPane.getTabs().add(tab);
                    tabPane.getSelectionModel().select(tab);

                } catch (SQLException e) {

                    Logging.logError(e);
                }
            });

        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }*/



}

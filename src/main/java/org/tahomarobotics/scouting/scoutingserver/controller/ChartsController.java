package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class TeleTotalsController implements Initializable {

    @FXML
    public void generateTeleTotals(ActionEvent event) {
        Logging.logInfo("Generating Tele Totals");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}

package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;

public class MiscController {

    @FXML
    public void generateStratScoutingSchedule(ActionEvent event) {
        Logging.logInfo("Generating Strat Scouting Schedule");
        //get match schedule
        //for the selected team, go through all their matches and figure out who their alliance partners and opponents are
        //then for each match make a list of all teams to scout in that match. In order for them to need to be scouted, either their next match, or the one after that, they must be with us
        
    }
}

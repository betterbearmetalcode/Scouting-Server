package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.AutoHeatMapCreatorDialog;
import org.tahomarobotics.scouting.scoutingserver.util.auto.AutoHeatmap;
import org.tahomarobotics.scouting.scoutingserver.util.auto.AutoPath;
import org.tahomarobotics.scouting.scoutingserver.util.auto.HeatmapCreationInformation;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class MiscController {

    @FXML
    public void generateStratScoutingSchedule(ActionEvent event) {
        Logging.logInfo("Generating Strat Scouting Schedule");
        //get match schedule
        //for the selected team, go through all their matches and figure out who their alliance partners and opponents are
        //then for each match make a list of all teams to scout in that match. In order for them to need to be scouted, either their next match, or the one after that, they must be with us
        
    }


    //this is some code that mr buranik wanted to find all the matches that have had a trap done in the pnw so far.
    //just leave it in as it may be useful
    @FXML
    public void findTraps(ActionEvent event) {
        ArrayList<String> matchesWithTrap = new ArrayList<>();
        try {
            JSONArray data = APIUtil.get("/district/2024pnw/events/keys");
            for (Object o : data.toList()) {
                JSONArray eventData = APIUtil.get("/event/" + o.toString() + "/matches");
                for (Object match : eventData) {
                    JSONObject matchMap = (JSONObject) match;
                    try {
                        JSONObject breakDown = (JSONObject) matchMap.get("score_breakdown");
                        JSONObject redBreakdown = (JSONObject) breakDown.get("red");
                        JSONObject blueBreakdown = (JSONObject) breakDown.get("blue");

                        if (redBreakdown.getBoolean("trapStageLeft") || redBreakdown.getBoolean("trapCenterStage") || redBreakdown.getBoolean("trapStageRight") || blueBreakdown.getBoolean("trapStageLeft") || blueBreakdown.getBoolean("trapCenterStage") || blueBreakdown.getBoolean("trapStageRight")) {
                            matchesWithTrap.add(matchMap.get("key").toString());
                        }
                    }catch (Exception e) {
                        //e.printStackTrace();
                    }

                }
            }
            System.out.println(matchesWithTrap);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}

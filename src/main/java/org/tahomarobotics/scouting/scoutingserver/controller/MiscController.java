package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
import java.sql.SQLException;
import java.util.*;

public class MiscController {
    public VBox box;

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

    //game specific method to figure out where teams run what autos
    @FXML
    public void generateHeatmaps(ActionEvent event) {
        //essentiall generates and displays a auto heatmap object
        //select which databases to use for heatmaps, ie, be able to use data from old competitions to early in the comp we can use our data from old comps
        //it could automatically select teams from a match schedule, but that takes internet and it would be better to be able
        //to customize teams for elim matches and speculation and not have to use internet

        //data presentation
        //this function will have to present the data collected somehow
        //it will plot the auto for different teams on a picture of the field you ought to be able to save this image as an export

        //first gather the data that will be used

        try {
            AutoHeatMapCreatorDialog dialog = new AutoHeatMapCreatorDialog();
            Optional<HeatmapCreationInformation> result = dialog.showAndWait();
             if (result.isPresent()) {
                 //generate heat map
                 AutoHeatmap heatmap = createAlgorithmicHeatmap(result.get());
                 //display heatmap

                 box.getChildren().add(new ImageView(heatmap.getRenderedImage()));
                 System.out.println(heatmap);
             }
        } catch (SQLException | IOException e) {
            Logging.logError(e);
        }
    }

    private AutoHeatmap createAlgorithmicHeatmap(HeatmapCreationInformation input) throws SQLException {
        AutoHeatmap heatmap = new AutoHeatmap();
        //for all the data add autos to the heatmap as appropriate;
        //TODO add AI to recognize autos and correct scouting mistakes
        for (String tableName : input.dataTables()) {
            for (DatabaseManager.RobotPosition robotPosition : input.teams().keySet()) {
                String teamNumber = input.teams().get(robotPosition);
                //get all the auto data for this team in each cable in a loop but dont get duplicate autos
                ArrayList<HashMap<String, Object>> teamAutoData = SQLUtil.exec("SELECT " +
                        Constants.SQLColumnName.TEAM_NUM + ", " +
                        Constants.SQLColumnName.F1 + ", " +
                        Constants.SQLColumnName.F2 + ", " +
                        Constants.SQLColumnName.F3 + ", " +
                        Constants.SQLColumnName.M1 + ", " +
                        Constants.SQLColumnName.M2 + ", " +
                        Constants.SQLColumnName.M3 + ", " +
                        Constants.SQLColumnName.M4 + ", " +
                        Constants.SQLColumnName.M5 + " FROM \"" + tableName + "\" WHERE TEAM_NUM=?", new Object[]{teamNumber}, true);

                if (teamAutoData.isEmpty()) {
                    heatmap.addTeamWithNoData(robotPosition, teamNumber);
                    continue;
                }
                //add each auto to the heatmap
                for (HashMap<String, Object> teamAutoDatum : teamAutoData) {
                    ArrayList<AutoPath.Note> notes = new ArrayList<>();
                    //loop through the sql data and add notes to the array
                    teamAutoDatum.keySet().forEach(s -> {
                        if (!Objects.equals(s, Constants.SQLColumnName.TEAM_NUM.name())) {
                            //if this is a column relating to the auto data
                            //possible values should be 0,1,2
                            if (Integer.parseInt(teamAutoDatum.get(s).toString()) == 1 || Integer.parseInt(teamAutoDatum.get(s).toString()) == 2) {
                                //if the note was collected successfully or unsuccessfully
                                notes.add(AutoPath.getNoteFromSQLColumn(Constants.SQLColumnName.valueOf(s), robotPosition.ordinal() < 3));
                            }

                        }
                    });
                    //sort the notes to standardize auto names as souting data does not tell us when things are collected unfortunatly
                    notes.sort(Comparator.comparingInt(Enum::ordinal));
                    heatmap.addAuto(robotPosition, new AutoPath(teamNumber, notes, robotPosition));
                }//end for each sql column

            }//end for each team
        }//end for each data table
        return heatmap;

    }
}

package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import org.dhatim.fastexcel.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DataValidationCompetitionChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MiscController {

    @FXML
    public void generateStratScoutingSchedule(ActionEvent event) {
        Logging.logInfo("Generating Strat Scouting Schedule");
        //get match schedule
        //for the selected team, go through all their matches and figure out who their alliance partners and opponents are
        //then for each match make a list of all teams to scout in that match. In order for them to need to be scouted, either their next match, or the one after that, they must be with us
        DataValidationCompetitionChooser chooser = new DataValidationCompetitionChooser();
        Optional<String> result = chooser.showAndWait();
        if (result.isEmpty() || result.get().isEmpty()) {
            Logging.logInfo("Aborting generation of strategy scouting schedule");
            return;
        }
        try {
            ArrayList<HashMap<String,DatabaseManager.RobotPosition>> matchSchedule = new ArrayList<>();
            JSONArray apiResponse = APIUtil.get("/event/" + result.get() + "/matches/simple");
            ArrayList<Object> matchMaps = new ArrayList<>(apiResponse.toList());
            //filter so its only qualification matches
            ArrayList<Object> qmOnly = matchMaps.stream().filter(o -> {
                HashMap<String, Object> map = (HashMap<String, Object>) o;
                return map.get("key").toString().split("_")[1].startsWith("qm");
            }).collect(Collectors.toCollection(ArrayList::new));

            //sort by match number
            qmOnly.sort((o1, o2) -> {
                HashMap<String, Object> map1 = (HashMap<String, Object>) o1;
                HashMap<String, Object> map2 = (HashMap<String, Object>) o2;
                return Integer.compare(Integer.parseInt(map1.get("match_number").toString()), Integer.parseInt(map2.get("match_number").toString()));
            });
            //read data and add teams to match schedule
            qmOnly.forEach(o -> {
                HashMap<String, DatabaseManager.RobotPosition> teamsInThisMatch = getTeamsInThisMatch((HashMap<String, Object>) o);
                matchSchedule.add(teamsInThisMatch);
            });

            //now we have the matchs schedule and need to create a strat scouting schedule from it
            //to do that for each of our matches, we need to add the previous two occurances of all our teammates to a schedule
            //it is better to only query the API once and do scuffed array stuff then query it for the matches of each team speratly preformance wise
            HashMap<String, DatabaseManager.RobotPosition>[] stratScoutingSchedule = new HashMap[matchSchedule.size()];
            for (int i = 0; i < matchSchedule.size(); i++) {
                if (matchSchedule.get(i).containsKey("2046")) {
                    for (String teamNum : matchSchedule.get(i).keySet()) {
                        DatabaseManager.RobotPosition position = matchSchedule.get(i).get(teamNum);
                        if (Objects.equals(teamNum, "2046")) {
                            continue;
                        }
                        //get list of all of this teams' matches
                        ArrayList<Integer> theirMatchSchedule = new ArrayList<>();//matchSchedule.stream().filter(robotPositionStringHashMap -> robotPositionStringHashMap.containsValue(teamNum)).collect(Collectors.toCollection(ArrayList::new));
                        for (int k = 0; k < i; k++) {
                            if (matchSchedule.get(k).containsKey(teamNum)) {
                                theirMatchSchedule.add(k);
                            }
                        }
                        for (int j = theirMatchSchedule.size()- 1; j > (theirMatchSchedule.size() - 3); j--) {
                            //for the two matches before the match this team is with us
                            if (j < 0) {
                                //happens at the beginning of the schedule
                                continue;
                            }

                            int matchTheyAreInBeforeUs = theirMatchSchedule.get(j);
                            //it is possible for there to be a need to strat scout during our matches but i think we should skip our matches
                            if (matchSchedule.get(matchTheyAreInBeforeUs).containsKey("2046")) {
                                continue;
                            }
                            DatabaseManager.RobotPosition teamPosition = matchSchedule.get(matchTheyAreInBeforeUs).get(teamNum);
                            if (stratScoutingSchedule[matchTheyAreInBeforeUs] == null) {
                                HashMap<String, DatabaseManager.RobotPosition> teamsToScout = new HashMap<>();
                                teamsToScout.put(teamNum, teamPosition);
                                stratScoutingSchedule[matchTheyAreInBeforeUs] = teamsToScout;
                            }else {
                                stratScoutingSchedule[matchTheyAreInBeforeUs].put(teamNum, teamPosition);
                            }

                        }
                    }
                }
            }
            FileChooser chooser1 = new FileChooser();
            chooser1.setTitle("Save Strat Scouting Schedule");
            chooser1.setInitialDirectory(new File(System.getProperty("user.home")));
            chooser1.setInitialFileName("Strategy Scouting Schedule.txt");
            chooser1.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", ".txt"));
            File selectedFile = chooser1.showSaveDialog(ScoutingServer.mainStage.getOwner());

            FileOutputStream os = new FileOutputStream(selectedFile);
            for (int i = 0; i < stratScoutingSchedule.length; i++) {
                StringBuilder builder = new StringBuilder();
                builder.append("Match: ").append(i + 1).append(" ");
                if (stratScoutingSchedule[i] != null) {
                    for (String teamNum : stratScoutingSchedule[i].keySet()) {
                        builder.append(teamNum).append(", ");
                    }
                }else {
                    builder.append("No teams this match!  ");//spaces are because of the substring
                }

                os.write((builder.substring(0, builder.toString().length() - 2) + "\n").getBytes());
                os.flush();

            }
            os.close();
        } catch (IOException | InterruptedException e) {
            Logging.logError(e);
        }
    }

    private static HashMap<String, DatabaseManager.RobotPosition> getTeamsInThisMatch(HashMap<String, Object> o) {
        HashMap<String, DatabaseManager.RobotPosition> teamsInThisMatch = new HashMap<>();
        HashMap<String, Object> allianceMap = (HashMap<String, Object>) o.get("alliances");
        HashMap<String, Object> redAlliance = (HashMap<String, Object>) allianceMap.get("red");
        ArrayList<String> redTeamKeys = (ArrayList<String>) redAlliance.get("team_keys");
        HashMap<String, Object> blueAlliance = (HashMap<String, Object>) allianceMap.get("blue");
        ArrayList<String> blueTeamKeys = (ArrayList<String>) blueAlliance.get("team_keys");
        for (int i = 0; i < redTeamKeys.size(); i++) {
            DatabaseManager.RobotPosition redPosition = DatabaseManager.RobotPosition.values()[i];
            DatabaseManager.RobotPosition bluePosition = DatabaseManager.RobotPosition.values()[i + 3];
            teamsInThisMatch.put(redTeamKeys.get(i).substring(3), redPosition);
            teamsInThisMatch.put(blueTeamKeys.get(i).substring(3), bluePosition);
        }
        return teamsInThisMatch;
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

package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DataValidationCompetitionChooser;
import org.tahomarobotics.scouting.scoutingserver.util.data.TrapThing;

import javax.crypto.interfaces.PBEKey;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
            ArrayList<HashMap<String, DatabaseManager.RobotPosition>> matchSchedule = new ArrayList<>();
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

        ArrayList<Object> eventKeys;
        ArrayList<TrapThing> teamsWithTrap = new ArrayList<>();
        try {
            JSONArray events =  APIUtil.get("" +
                    "/events/2024/keys");
            eventKeys = new ArrayList<>(events.toList());
            for (Object eventKey : eventKeys) {
                System.out.println("Event: " + eventKey);
                if (eventKey == "2024marea") {
                        System.out.println();
                }
                JSONArray eventData = APIUtil.get("/event/" + eventKey.toString() + "/matches");
                for (Object match : eventData) {
                    JSONObject matchMap = (JSONObject) match;
                    try {
                        JSONObject alliances =  (JSONObject) matchMap.get("alliances");
                        JSONObject blueAlliance = (JSONObject) alliances.get("blue");
                        JSONObject redAlliance = (JSONObject) alliances.get("red");
                        JSONObject breakDown = (JSONObject) matchMap.get("score_breakdown");
                        JSONObject redBreakdown = (JSONObject) breakDown.get("red");
                        JSONObject blueBreakdown = (JSONObject) breakDown.get("blue");


                        if (redBreakdown.getBoolean("trapStageLeft") || redBreakdown.getBoolean("trapCenterStage") || redBreakdown.getBoolean("trapStageRight")) {
                            //then red alliance had a trap
                            addTeams(redAlliance, teamsWithTrap, eventKey.toString());


                        }else if ( blueBreakdown.getBoolean("trapStageLeft") || blueBreakdown.getBoolean("trapCenterStage") || blueBreakdown.getBoolean("trapStageRight")) {
                            //then blue had a trap
                            addTeams(blueAlliance, teamsWithTrap, eventKey.toString());
                        }
                    }catch (Exception e) {
                        //e.printStackTrace();
                    }

                }
            }
        } catch (Exception  e) {
            //e.printStackTrace();
        }
        teamsWithTrap.sort((o1, o2) -> Integer.compare(o2.getAverageTrap(), o1.getAverageTrap()));
        StringBuilder builder = new StringBuilder();
        builder.append("Teams With Trap: \n");
        for (TrapThing trap : teamsWithTrap) {
            builder.append("Team: " + trap.teamNumber() + " was in an average of " + trap.getAverageTrap() + " matches with trap\n");
        }
        System.out.println(builder);

    }

    private static void addTeams(JSONObject redAlliance, ArrayList<TrapThing> teamsWithTrap, String eventKey) {
        JSONArray teamKeys = (JSONArray) redAlliance.get("team_keys");
        ArrayList<String> teams = new ArrayList<>();
        teamKeys.forEach(o -> teams.add(o.toString().substring(3)));


        //for the teams in this alliance
        teams.forEach(s -> {
            //if we already have a team in the list, increment their frequence
            if (teamsWithTrap.stream().anyMatch(stringIntegerPair -> Objects.equals(stringIntegerPair.teamNumber(), s))) {
                //if we already have this team in increment their frequency
                final int[] oldFrequency = {0};
                final ArrayList<String>[] events = new ArrayList[]{new ArrayList<>()};
                teamsWithTrap.removeIf(stringIntegerPair -> {
                    if (Objects.equals(s, stringIntegerPair.teamNumber())) {
                        oldFrequency[0] = stringIntegerPair.numTraps();
                        events[0] = stringIntegerPair.events();
                        return true;
                    }
                    return false;
                });
                if (!events[0].contains(eventKey)) {
                    events[0].add(eventKey);
                }
                teamsWithTrap.add(new TrapThing(s, ++oldFrequency[0], events[0]));
            }else {
                //add them to the list
                teamsWithTrap.add(new TrapThing(s, 1, new ArrayList<>(List.of(eventKey))));
            }
        });
    }
    @FXML
    public void getCSVBackupTemplate(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save CSV Template");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", ".csv"));
        chooser.setInitialFileName("CSV Backup.csv");
        File selectedFile = chooser.showSaveDialog(ScoutingServer.mainStage.getOwner());
        if (selectedFile == null) {
            return;
        }

        Logging.logInfo("Getting backup template for CSV");
        StringBuilder builder = new StringBuilder();
        //title row
        builder.append(Constants.SQLColumnName.MATCH_NUM).append(",");
        builder.append(Constants.SQLColumnName.TEAM_NUM).append(",");
        builder.append(Constants.SQLColumnName.ALLIANCE_POS).append(",");
        builder.append(Constants.SQLColumnName.AUTO_SPEAKER).append(",");
        builder.append(Constants.SQLColumnName.AUTO_AMP).append(",");
        builder.append(Constants.SQLColumnName.AUTO_SPEAKER_MISSED).append(",");
        builder.append(Constants.SQLColumnName.AUTO_AMP_MISSED).append(",");
        builder.append(Constants.SQLColumnName.A_STOP).append(",");
        builder.append(Constants.SQLColumnName.SHUTTLED).append(",");
        builder.append(Constants.SQLColumnName.TELE_SPEAKER).append(",");
        builder.append(Constants.SQLColumnName.TELE_TRAP).append(", ");
        builder.append(Constants.SQLColumnName.TELE_AMP).append(",");
        builder.append(Constants.SQLColumnName.TELE_SPEAKER_MISSED).append(",");
        builder.append(Constants.SQLColumnName.TELE_AMP_MISSED).append(",");
        builder.append(Constants.SQLColumnName.SPEAKER_RECEIVED).append(",");
        builder.append(Constants.SQLColumnName.AMP_RECEIVED).append(",");
        builder.append(Constants.SQLColumnName.LOST_COMMS).append(",");
        builder.append(Constants.SQLColumnName.TELE_COMMENTS).append(",\n");
        //for each match
        for (int matchNum = 1; matchNum <= 130; matchNum++) {
            for (DatabaseManager.RobotPosition value : DatabaseManager.RobotPosition.values()) {
                //for each of the columns
                for (int i = 0; i < 18; i++) {
                    if (i == 0) {
                        builder.append(matchNum);
                    }else if (i == 2) {
                        builder.append(value);
                    }else if (i == 17) {
                        builder.append("No Comments");
                    }
                    builder.append(",");
                }
                builder.append("\n");

            }

        }

        try {
            if (!selectedFile.exists()) {
                selectedFile.createNewFile();
            }
            FileWriter writer = new FileWriter(selectedFile);
            writer.write(builder.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Logging.logError(e);
        }

    }

}

package org.tahomarobotics.scouting.scoutingserver;

import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.RobotPositon;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataValidator {


    public static  ArrayList<Match> validateData(String eventCode, ArrayList<Match> databaseData) {

            ArrayList<Match> output = new ArrayList<>();
            Optional<JSONArray> eventMatchesOptional = APIUtil.getEventMatches(eventCode);
            if (eventMatchesOptional.isEmpty()) {
                Logging.logInfo("Failed to validate data, likely causes: No Internet, No matches on TBA", true);
                return databaseData;
            }
            //this only happens if there is actually data we can use in the optional
            ArrayList<Object> rawList =  new ArrayList<>(eventMatchesOptional.get().toList());

            boolean checkTeamNumbers = true;
            for (Match matchData : databaseData) {
                ArrayList<RobotPositon> corectedRobotPositons = new ArrayList<>();
                Optional<Object> matchTBAData =  getTBAMatchObject(rawList, eventCode, matchData.matchNumber());
                if (matchTBAData.isEmpty()) {
                    //then we don't hava data for this matach and can't validate it so just add the original data and try the next match
                    output.add(matchData);
                    continue;
                }
                HashMap<String, Object> matchDatum = (HashMap<String, Object>) matchTBAData.get();
                if (!matchData.robotPositons().isEmpty()) {
                    //if we actually have data for this match then validate it
                    //otherwise forget about it because its just showing up as an empty match

                    if (checkTeamNumbers) {
                        //check if the teams we have for this match line up with what tba says to look for scouting mistakes
                        //get a list of team numbers correlated with the robot positions.
                        //doing these with seperate try catch blocks because some parts of TBA data can be updated at different times
                        try {
                            //get team objects
                            HashMap<String, Object> allianceMap =  (HashMap<String, Object>) matchDatum.getOrDefault("alliances", null);
                            HashMap<String, Object> blueAllianceTeams = (HashMap<String, Object>) allianceMap.getOrDefault("blue", null);
                            HashMap<String, Object> redAllianceTeams = (HashMap<String, Object>) allianceMap.getOrDefault("red", null);

                            //get array of teams
                            ArrayList<String> blueTeams = (ArrayList<String>) blueAllianceTeams.getOrDefault("team_keys", null);
                            ArrayList<String> teamKeys = ((ArrayList<String>) redAllianceTeams.getOrDefault("team_keys", null));
                            teamKeys.addAll(blueTeams);
                            ArrayList<String> teamNums =new ArrayList<>();
                            teamKeys.forEach(s -> teamNums.add(s.split("frc")[1]));
                            //get map of correct team numbers and robot positions
                            HashMap<Integer, DatabaseManager.RobotPosition> correctMatchConfiguration = new HashMap<>();
                            if (teamNums.size() != 6) {
                                Logging.logInfo("Aborting team validation for this match because TBA data is corrupt", true);
                                break;
                            }
                            for (DatabaseManager.RobotPosition robotPosition : DatabaseManager.RobotPosition.values()) {
                                correctMatchConfiguration.put(Integer.parseInt(teamNums.get(robotPosition.ordinal())), robotPosition);
                            }

                            //check the scouting data against this data
                            for (int teamNum : correctMatchConfiguration.keySet()) {
                                DatabaseManager.RobotPosition correctRobotPosition = correctMatchConfiguration.get(teamNum);
                                List<RobotPositon> scoutingDataForThisPosition = matchData.robotPositons().stream().filter(robotPositon -> robotPositon.robotPosition() == correctRobotPosition).collect(Collectors.toList());
                                if (scoutingDataForThisPosition.size() == 1) {
                                    //then there is only one robot entered for this position
                                    if (scoutingDataForThisPosition.get(0).teamNumber() != teamNum) {
                                        //then the scouting data has the incorrect team number
                                        //notify user and if they want to stop checking team numbers
                                        if (!Constants.askQuestion("Match: " + matchData.matchNumber() + " Position: " + correctRobotPosition.name() + " has the incorrect team entered, continue checking team numbers?")) {
                                            checkTeamNumbers = false;
                                            break;
                                        }
                                    }
                                }else if (scoutingDataForThisPosition.size() > 1) {
                                    //then there are multiple teams entered for the position
                                    Logging.logInfo("Multiple teams are entered for Match: " + matchData.matchNumber() + " Position: " + correctRobotPosition, true);
                                }else {
                                    //there is no scouting data for this team on TBA
                                    if (!Constants.askQuestion("Match: " + matchData.matchNumber() + " Position: " + correctRobotPosition.name() + " has no data, continue checking team numbers?")) {
                                        checkTeamNumbers = false;
                                        break;
                                    }
                                }
                            }//end for each team in this match

                        }catch (NullPointerException | ClassCastException e) {
                            //there is no breakdown or something went wrong so skip validation
                            Logging.logError(e, " skipping team validation for this match becuase TBA is not updated");
                        }
                    }




                    try {


                        //get scouting data for red and blue alliances sepratly
                        List<RobotPositon> redRobotPositons = matchData.robotPositons().stream().filter(robot -> robot.robotPosition().ordinal() < 3).toList();
                        List<RobotPositon> blueRobotPositons = matchData.robotPositons().stream().filter(robot -> robot.robotPosition().ordinal() > 2).toList();

                        //get TBA match score breakdowns for each alliance
                        HashMap<String, Object> matchScoreBreakdown = (HashMap<String, Object>) matchDatum.get("score_breakdown");
                        HashMap<String, Object> redAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("red");
                        HashMap<String, Object> blueAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("blue");


                        //use alliance score breakdown to generate corrected datapoint objects and add them to a match object
                        corectedRobotPositons.addAll(correctAlliance(redRobotPositons.size() == 3, redRobotPositons, redAllianceScoreBreakdown));
                        corectedRobotPositons.addAll(correctAlliance(blueRobotPositons.size() == 3, blueRobotPositons, blueAllianceScoreBreakdown));
                        output.add(new Match(matchData.matchNumber(), new ArrayList<>(corectedRobotPositons)));
                    }catch (NullPointerException e) {
                        //null pointer menas that there is no breakdown yet so skip validation
                        Logging.logInfo(e.getMessage() + " skipping validation of this match because TBA not updated");
                        //add old match so it doesn't disappear
                        output.add(matchData);
                    }

                }

            }

            return output;

    }


    private static ArrayList<RobotPositon> correctAlliance(boolean allianceComplete, List<RobotPositon> robotPositons, HashMap<String, Object> breakdown) {
        ArrayList<RobotPositon> correctedAlliance = new ArrayList<>();
        int autoSpeakerTrue = (int) breakdown.get("autoSpeakerNoteCount");

        int autoAmpTrue = (int) breakdown.get("autoAmpNoteCount");
        int teleSpeakerTrue = ((int) breakdown.get("teleopSpeakerNoteAmplifiedCount")) + ((int) breakdown.get("teleopSpeakerNoteCount"));
        int teleAmpTrue = (int) breakdown.get("teleopAmpNoteCount");
        int autoSpeakerMeasured = 0;
        int autoAmpMeasured = 0;
        int teleSpeakerMeasured = 0;
        int teleAmpMeasured = 0;
        if (allianceComplete) {
            //calculate measured values to compare against true
            if (robotPositons.size() != 3) {
                Logging.logError(null, "Internal Datavalidation Error for match");
                allianceComplete = false;
            }
            autoSpeakerMeasured = robotPositons.get(0).record().autoSpeaker() + robotPositons.get(1).record().autoSpeaker() + robotPositons.get(2).record().autoSpeaker();
            autoAmpMeasured = robotPositons.get(0).record().autoAmp() + robotPositons.get(1).record().autoAmp() + robotPositons.get(2).record().autoAmp();
            teleSpeakerMeasured = robotPositons.get(0).record().teleSpeaker() + robotPositons.get(1).record().teleSpeaker() + robotPositons.get(2).record().teleSpeaker();
            teleAmpMeasured = robotPositons.get(0).record().teleAmp() + robotPositons.get(1).record().teleAmp() + robotPositons.get(2).record().teleAmp();

        }
        for (RobotPositon robotPositon : robotPositons) {
            LinkedList<DataPoint> recordTemp = new LinkedList<>();
            //for each robot
            for (DataPoint dataPoint : robotPositon.data()) {
                if (!allianceComplete) {
                    //then there is missing data for at least on robot or there is excess data and the whole match will be marked as unknown
                    recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), Double.NaN));
                }else {
                    try {
                        switch (Constants.SQLColumnName.valueOf(dataPoint.getName())) {

                            case MATCH_NUM, TEAM_NUM, ALLIANCE_POS, TELE_COMMENTS, TELE_TRAP -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), 0));
                                break;
                            }
                            case AUTO_SPEAKER -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), (autoSpeakerMeasured - autoSpeakerTrue)));
                                break;
                            }
                            case AUTO_AMP -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), autoAmpMeasured - autoAmpTrue));
                            }
                            case TELE_SPEAKER -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), teleSpeakerMeasured- teleSpeakerTrue));
                                break;
                            }
                            case TELE_AMP -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), teleAmpMeasured - teleAmpTrue));
                                break;
                            }/*
                            case TELE_TRAP -> {
                                int robotPositonNum = (robotPositon.record().position().ordinal() % 3)+1;
                                String climb = (String) breakdown.get("endGameRobot" + robotPositonNum);
                                boolean trap = false;
                                if (!Objects.equals(climb, "None") && !Objects.equals(climb, "Parked")) {
                                    trap = (boolean) breakdown.get("trap" + climb);
                                }
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), (trap == (robotPositon.record().teleTrap() > 0))?(0):(3)));
                                break;
                            }*/
                            case TELE_SPEAKER_MISSED, TELE_AMP_MISSED, AUTO_SPEAKER_MISSED, AUTO_AMP_MISSED, NOTE_A, NOTE_B, NOTE_C, NOTE_1, NOTE_2, NOTE_3, NOTE_4, NOTE_5, A_STOP , LOST_COMMS-> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), Double.NaN));
                                break;
                            }
                        }//end switch
                    }catch (IllegalArgumentException messingwithAsher) {
                        //do nothing this just happens when you try to access the calculated values
                    }
                }


            }
            correctedAlliance.add(new RobotPositon(robotPositon.robotPosition(), robotPositon.teamNumber(), recordTemp, robotPositon.record()));

        }
        return correctedAlliance;

    }

    private static Optional<Object> getTBAMatchObject(ArrayList<Object> list, String eventKey, int matchNum) {
        //get the json representing this match if there is one
        return  list.stream().filter(o -> {
            HashMap<String, Object> dataum = (HashMap<String, Object>) o;
            String exptectedMatchKey = eventKey + "_qm"  + matchNum;
            return dataum.get("key").equals(exptectedMatchKey);
        }).findFirst();



    }

}

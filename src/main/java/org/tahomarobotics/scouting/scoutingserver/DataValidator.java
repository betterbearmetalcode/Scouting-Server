package org.tahomarobotics.scouting.scoutingserver;

import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.Robot;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class DataValidator {


    public static  ArrayList<Match> validateData(String eventCode, ArrayList<Match> databaseData) {
        try {
            JSONArray eventMatches = APIUtil.get("/event/" + eventCode + "/matches");//returns array of json objects each representing a match
            if (eventMatches.get(0).equals("NoInternet")) {
                Logging.logInfo("Cannot validate data, returning unmodified data");
                return databaseData;
            }
            ArrayList<Match> output = new ArrayList<>();
            ArrayList<Object> rawList =  new ArrayList<>(eventMatches.toList());

            for (Match matchData : databaseData) {
                ArrayList<Robot> corectedRobots = new ArrayList<>();
                Optional<Object> matchTBAData =  getTBAMatchObject(rawList, eventCode, matchData.matchNumber());
                if (matchTBAData.isEmpty()) {
                    //then we don't hava data for this matach and can't validate it so just add the original data and try the next match
                    output.add(matchData);
                    continue;
                }
                if (!matchData.robots().isEmpty()) {
                    List<Robot> redRobots = matchData.robots().stream().filter(robot -> robot.robotPosition().ordinal() < 3).toList();
                    List<Robot> blueRobots = matchData.robots().stream().filter(robot -> robot.robotPosition().ordinal() > 2).toList();
                    HashMap<String, Object> matchDatum = (HashMap<String, Object>) matchTBAData.get();
                    HashMap<String, Object> matchScoreBreakdown = (HashMap<String, Object>) matchDatum.get("score_breakdown");
                    HashMap<String, Object> redAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("red");
                    HashMap<String, Object> blueAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("blue");

                    corectedRobots.addAll(correctAlliance(redRobots.size() == 3, redRobots, redAllianceScoreBreakdown));
                    corectedRobots.addAll(correctAlliance(blueRobots.size() == 3, blueRobots, blueAllianceScoreBreakdown));
                    output.add(new Match(matchData.matchNumber(), new ArrayList<>(corectedRobots)));
                }

            }

            return output;


        } catch (IOException | InterruptedException e) {
            Logging.logError(e, "failed to validate data");
            return databaseData;
        }

    }

    private static ArrayList<Robot> correctAlliance(boolean matchComplete, List<Robot> robots, HashMap<String, Object> breakdown) {
        ArrayList<Robot> correctedAlliance = new ArrayList<>();
        int autoSpeakerTrue = (int) breakdown.get("autoSpeakerNoteCount");
        int autoAmpTrue = (int) breakdown.get("autoAmpNoteCount");
        int teleSpeakerTrue = ((int) breakdown.get("teleopSpeakerNoteAmplifiedCount")) + ((int) breakdown.get("teleopSpeakerNoteCount"));
        int teleAmpTrue = (int) breakdown.get("teleopAmpNoteCount");
        for (Robot robot : robots) {
            LinkedList<DataPoint> recordTemp = new LinkedList<>();
            //for each robot
            for (DataPoint dataPoint : robot.data()) {
                if (!matchComplete) {
                    //then there is missing data for at least on robot or there is excess data and the whole match will be marked as unknown
                    recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), Double.NaN));
                }else {
                    try {
                        switch (Constants.SQLColumnName.valueOf(dataPoint.getName())) {

                            case TIMESTAMP, MATCH_NUM, TEAM_NUM, ALLIANCE_POS,AUTO_COMMENTS, TELE_COMMENTS -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), 0));
                                break;
                            }
                            case AUTO_SPEAKER -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), (robot.record().autoSpeaker() - autoSpeakerTrue)));
                                break;
                            }
                            case AUTO_AMP -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), robot.record().autoAmp() - autoAmpTrue));
                            }
                            case TELE_SPEAKER -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), robot.record().teleSpeaker() - teleSpeakerTrue));
                                break;
                            }
                            case TELE_AMP -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), robot.record().teleAmp() - teleAmpTrue));
                                break;
                            }
                            case TELE_TRAP -> {
                                int robotPositonNum = (robot.record().position().ordinal() % 3)+1;
                                String climb = (String) breakdown.get("endGameRobot" + robotPositonNum);
                                boolean trap = false;
                                if (!Objects.equals(climb, "None") && !Objects.equals(climb, "Parked")) {
                                    trap = (boolean) breakdown.get("trap" + climb);
                                }
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), (trap == (robot.record().teleTrap() > 0))?(0):(3)));
                                break;
                            }
                            case TELE_SPEAKER_MISSED, TELE_AMP_MISSED, AUTO_SPEAKER_MISSED, AUTO_AMP_MISSED, F1, F2, F3, M1, M2, M3, M4, M5 -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), Double.NaN));
                                break;
                            }
                        }//end switch
                    }catch (IllegalArgumentException messingwithAsher) {
                        //do nothing this just happens when you try to access the calculated values
                    }
                }


            }
            correctedAlliance.add(new Robot(robot.robotPosition(), robot.teamNumber(), recordTemp, robot.record()));

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

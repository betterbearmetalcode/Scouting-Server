package org.tahomarobotics.scouting.scoutingserver;

import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.RobotPositon;

import java.io.IOException;
import java.util.*;

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
                ArrayList<RobotPositon> corectedRobotPositons = new ArrayList<>();
                Optional<Object> matchTBAData =  getTBAMatchObject(rawList, eventCode, matchData.matchNumber());
                if (matchTBAData.isEmpty()) {
                    //then we don't hava data for this matach and can't validate it so just add the original data and try the next match
                    output.add(matchData);
                    continue;
                }
                if (!matchData.robotPositons().isEmpty()) {
                    try {
                        List<RobotPositon> redRobotPositons = matchData.robotPositons().stream().filter(robot -> robot.robotPosition().ordinal() < 3).toList();
                        List<RobotPositon> blueRobotPositons = matchData.robotPositons().stream().filter(robot -> robot.robotPosition().ordinal() > 2).toList();
                        HashMap<String, Object> matchDatum = (HashMap<String, Object>) matchTBAData.get();
                        HashMap<String, Object> matchScoreBreakdown = (HashMap<String, Object>) matchDatum.get("score_breakdown");
                        HashMap<String, Object> redAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("red");
                        HashMap<String, Object> blueAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("blue");

                        corectedRobotPositons.addAll(correctAlliance(redRobotPositons.size() == 3, redRobotPositons, redAllianceScoreBreakdown));
                        corectedRobotPositons.addAll(correctAlliance(blueRobotPositons.size() == 3, blueRobotPositons, blueAllianceScoreBreakdown));
                        output.add(new Match(matchData.matchNumber(), new ArrayList<>(corectedRobotPositons)));
                    }catch (NullPointerException e) {
                        //null pointer menas that there is no breakdown yet so skip validation
                        Logging.logInfo(e.getMessage() + " skipping validation of this match because TBA not updated");
                    }

                }

            }

            return output;


        } catch (IOException | InterruptedException e) {
            Logging.logError(e, "failed to validate data");
            return databaseData;
        }

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

                            case MATCH_NUM, TEAM_NUM, ALLIANCE_POS, TELE_COMMENTS -> {
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
                            }
                            case TELE_TRAP -> {
                                int robotPositonNum = (robotPositon.record().position().ordinal() % 3)+1;
                                String climb = (String) breakdown.get("endGameRobot" + robotPositonNum);
                                boolean trap = false;
                                if (!Objects.equals(climb, "None") && !Objects.equals(climb, "Parked")) {
                                    trap = (boolean) breakdown.get("trap" + climb);
                                }
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), (trap == (robotPositon.record().teleTrap() > 0))?(0):(3)));
                                break;
                            }
                            case TELE_SPEAKER_MISSED, TELE_AMP_MISSED, AUTO_SPEAKER_MISSED, AUTO_AMP_MISSED, F1, F2, F3, M1, M2, M3, M4, M5, LOST_COMMS-> {
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

package org.tahomarobotics.scouting.scoutingserver;

import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.Robot;

import java.util.*;

public class DataValidator {

    public static ArrayList<Match> validateData(String eventCode, ArrayList<Match> databaseData) {
        try {
            ArrayList<Match> data = new ArrayList<>();
            JSONArray eventMatches = APIUtil.get("/event/" + eventCode + "/matches");//returns array of json objects each representing a match
            ArrayList<Object> rawList =  new ArrayList<>(eventMatches.toList());
            ArrayList<HashMap<String, Object>> processedData = removeDuplicatesFromRawTBAData(rawList, eventCode);//remove non qualifiaction matches
            processedData.sort(Comparator.comparingInt(o -> (Integer) o.get("match_number")));//sort by match number


            for (HashMap<String, Object> match : processedData) {
                //for each match
                HashMap<String, Object> matchScoreBreakdown = (HashMap<String, Object>) match.get("score_breakdown");
                ArrayList<Robot> corectedRobots = new ArrayList<>();
                int matchNum = (Integer) match.get("match_number");
               Optional<Match> result = databaseData.stream().filter(match1 -> match1.matchNumber() == matchNum).findFirst();
               List<Robot> robots = new ArrayList<>();
               result.ifPresent(match12 -> robots.addAll(result.get().robots()));
                if (!robots.isEmpty()) {
                    //if there is actually scouting data for this match that blue alliance has data for.
                    boolean matchComplete = robots.size() == 6;//is the whole match invalid
                    List<Robot> redRobots = robots.stream().filter(robot -> robot.robotPosition().ordinal() < 3).toList();
                    List<Robot> blueRobots = robots.stream().filter(robot -> robot.robotPosition().ordinal() > 2).toList();
                    HashMap<String, Object> redAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("red");
                    HashMap<String, Object> blueAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("blue");

                    corectedRobots.addAll(correctAlliance(matchComplete, redRobots, redAllianceScoreBreakdown));
                    corectedRobots.addAll(correctAlliance(matchComplete, blueRobots, blueAllianceScoreBreakdown));
                    data.add(new Match(matchNum, new ArrayList<>(corectedRobots)));
                }

            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
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

    private static ArrayList<HashMap<String, Object>> removeDuplicatesFromRawTBAData(ArrayList<Object> oldList, String eventKey) {
        // Create a new ArrayList
        ArrayList<HashMap<String, Object>> newList = new ArrayList<>();
        // Traverse through the first list
        for (Object element : oldList) {
            HashMap<String, Object> match = (HashMap<String, Object>) element;
            // If this element is not present in newList
            // then add it

            int matchNum = (int) match.get("match_number");
            String exptectedMatchKey = eventKey + "_qm"  + matchNum;
            if (match.get("key").equals(exptectedMatchKey)) {
                newList.add(match);
            }
        }
        // return the new list
        return newList;
    }

}

package org.tahomarobotics.scouting.scoutingserver;

import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.DataPoint;

import java.util.*;

public class DataValidator {

    public static  ArrayList<ArrayList<ArrayList<DataPoint>>> validateData(String eventCode, LinkedList<DatabaseManager.MatchRecord> databaseData) {
        try {
            ArrayList<ArrayList<ArrayList<DataPoint>>> correctedCompetition = new ArrayList<>();
            JSONArray eventMatches = APIUtil.get("/event/" + eventCode + "/matches");//returns array of json objects each representing a match
            ArrayList<Object> rawList =  new ArrayList<>(eventMatches.toList());

            rawList.sort((o1, o2) -> {
                HashMap<String, Object> thing1 = (HashMap<String, Object>) o1;
                HashMap<String, Object> thing2 = (HashMap<String, Object>) o2;
                return Integer.compare((Integer) thing1.get("match_number"), (Integer) thing2.get("match_number"));
            });//sort by match number
            ArrayList<HashMap<String, Object>> processedData = removeDuplicatesFromRawTBAData(rawList);//remove duplicates. now this array contains a bunch of json objects each representing a match

            for (HashMap<String, Object> match : processedData) {
                //for each match
                HashMap<String, Object> matchScoreBreakdown = (HashMap<String, Object>) match.get("score_breakdown");

                int matchNum = (Integer) match.get("match_number");
                List<DatabaseManager.MatchRecord> robots = databaseData.stream().filter(matchRecord -> matchRecord.matchNumber() == matchNum).toList();
                if (!robots.isEmpty()) {
                    //if there is actually scouting data for this match that blue alliance has data for.
                    boolean matchComplete = robots.size() == 6;//is the whole match invalid
                    List<DatabaseManager.MatchRecord> redRobots = robots.stream().filter(matchRecord -> matchRecord.position().ordinal() < 3).toList();
                    List<DatabaseManager.MatchRecord> blueRobots = robots.stream().filter(matchRecord -> matchRecord.position().ordinal() > 2).toList();
                    HashMap<String, Object> redAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("red");
                    HashMap<String, Object> blueAllianceScoreBreakdown  = (HashMap<String, Object>) matchScoreBreakdown.get("blue");
                    ArrayList<ArrayList<DataPoint>> correctedMatch = new ArrayList<>();
                    correctedMatch.addAll(correctAlliance(matchComplete, redRobots, redAllianceScoreBreakdown));
                    correctedMatch.addAll(correctAlliance(matchComplete, blueRobots, blueAllianceScoreBreakdown));
                    correctedCompetition.add(correctedMatch);
                }

            }
            return correctedCompetition;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static ArrayList<ArrayList<DataPoint>> correctAlliance(boolean matchComplete, List<DatabaseManager.MatchRecord> robots, HashMap<String, Object> breakdown) {
        ArrayList<ArrayList<DataPoint>> correctedAlliance = new ArrayList<>();
        int autoSpeakerTrue = (int) breakdown.get("autoSpeakerNoteCount");
        int autoAmpTrue = (int) breakdown.get("autoAmpNoteCount");
        int teleSpeakerTrue = ((int) breakdown.get("teleopSpeakerNoteAmplifiedCount")) + ((int) breakdown.get("teleopSpeakerNoteCount"));
        int teleAmpTrue = (int) breakdown.get("teleopAmpNoteCount");
        for (DatabaseManager.MatchRecord robot : robots) {
            ArrayList<DataPoint> recordTemp = new ArrayList<>();
            //for each robot
            for (DataPoint dataPoint : robot.getDataAsList()) {
                if (!matchComplete) {
                    //then there is missing data for at least on robot or there is excess data and the whole match will be marked as unknown
                    recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.ErrorLevel.UNKNOWN));
                }else {
                    try {
                        switch (Constants.ColumnName.valueOf(dataPoint.getName())) {

                            case TIMESTAMP, MATCH_NUM, TEAM_NUM, ALLIANCE_POS, AUTO_LEAVE, AUTO_COMMENTS, TELE_COMMENTS, ENDGAME_POS -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.ErrorLevel.ZERO));
                                break;
                            }
                            case AUTO_SPEAKER -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.translateErrorNum(robot.autoSpeaker() - autoSpeakerTrue)));
                                break;
                            }
                            case AUTO_AMP -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.translateErrorNum(robot.autoAmp() - autoAmpTrue)));
                            }
                            case AUTO_COLLECTED, AUTO_SPEAKER_MISSED, AUTO_AMP_MISSED -> {
                                //collected - missed = scored + error
                                //collected - missed - scored = error
                                int totalMissed = robot.autoAmpMissed() + robot.autoSpeakerMissed();
                                int totalScored = autoSpeakerTrue + autoAmpTrue;
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.translateErrorNum(robot.autoCollected() - totalScored - totalMissed)));
                                break;
                            }
                            case TELE_SPEAKER -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.translateErrorNum(robot.teleSpeaker() - teleSpeakerTrue)));
                                break;
                            }
                            case TELE_AMP -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.translateErrorNum(robot.teleAmp() - teleAmpTrue)));
                                break;
                            }
                            case TELE_TRAP -> {
                                int robotPositonNum = (robot.position().ordinal() % 3)+1;
                                String climb = (String) breakdown.get("endGameRobot" + robotPositonNum);
                                boolean trap = false;
                                if (!Objects.equals(climb, "None") && !Objects.equals(climb, "Parked")) {
                                    trap = (boolean) breakdown.get("trap" + climb);
                                }
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), (trap == (robot.teleTrap() > 0))?(DataPoint.ErrorLevel.ZERO):(DataPoint.ErrorLevel.HIGH)));
                                break;
                            }
                            case TELE_SPEAKER_MISSED, TELE_AMP_MISSED -> {
                                recordTemp.add(new DataPoint(dataPoint.getName(), dataPoint.getValue(), DataPoint.ErrorLevel.UNKNOWN));
                                break;
                            }
                        }//end switch
                    }catch (IllegalArgumentException messingwithAsher) {
                        //do nothing this just happens when you try to access the calculated values
                    }
                }


            }
            correctedAlliance.add(recordTemp);

        }
        return correctedAlliance;

    }

    private static ArrayList<HashMap<String, Object>> removeDuplicatesFromRawTBAData(ArrayList<Object> oldList) {
        // Create a new ArrayList
        ArrayList<HashMap<String, Object>> newList = new ArrayList<>();
        // Traverse through the first list
        for (Object element : oldList) {
            HashMap<String, Object> match = (HashMap<String, Object>) element;
            // If this element is not present in newList
            // then add it
            int matchNum = (int) match.get("match_number");
            if (newList.stream().noneMatch(stringObjectHashMap -> matchNum == (int) stringObjectHashMap.get("match_number"))) {

                newList.add(match);
            }
        }
        // return the new list
        return newList;
    }

}

package org.tahomarobotics.scouting.scoutingserver;

import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DatabaseViewerTabContent;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.OperationAbortedByUserException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration.Datatype.*;
import static org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration.getRawDataMetrics;

public class Exporter {

    private static JSONArray competitionData;
    private static String eventCode;

    private static ArrayList<HashMap<String, Object>> teamsScouted;

    private final String tableName;

    private static final ArrayList<Integer> teamsToSkip = new ArrayList<>();


    public Exporter(String theEventCode, String activeTableName) throws IOException, InterruptedException, OperationAbortedByUserException, SQLException {
        eventCode = theEventCode;
        tableName = activeTableName;

        teamsScouted = SQLUtil.exec("SELECT DISTINCT " + Constants.SQLColumnName.TEAM_NUM + " FROM \"" + tableName + "\"", true);
        teamsScouted.sort(Comparator.comparingInt(o -> Integer.parseInt(o.get(Constants.SQLColumnName.TEAM_NUM.toString()).toString())));


        Optional<JSONArray> optionalEventMatches = APIUtil.getEventMatches(eventCode);
        if (optionalEventMatches.isEmpty()) {
            Logging.logInfo("Failed to fetch matches from tba");
            //only confinue after alerting the user to the risks of exporing without internet
            if (!Constants.askQuestion("You are trying to export data without matches from tba, doing so will result in incorrect data because you cannot access TBA Continue?")) {

                throw new OperationAbortedByUserException("User aborted operation due to inability to fetch matches");
            }
            competitionData = null;
        }else {
            competitionData = optionalEventMatches.get();
        }


    }


    public static void export(String tableName, File outputFile) throws SQLException, IOException {
        ArrayList<ArrayList<String>> output = new ArrayList<>();
        //add title row to export
        ArrayList<String> titleRow = new ArrayList<>();
        Arrays.stream(Constants.SQLColumnName.values()).toList().forEach(sqlColumnName -> titleRow.add(sqlColumnName.name()));
        Arrays.stream(Constants.ExportedDataMetrics.values()).toList().forEach(exportedDataMetrics -> titleRow.add(exportedDataMetrics.name()));
        output.add(titleRow);
        //add raw and exported data
        ArrayList<HashMap<String, Object>> matchNums = SQLUtil.exec("SELECT DISTINCT " + Constants.SQLColumnName.MATCH_NUM + " FROM \"" + tableName + "\"", false);
        for (HashMap<String, Object> matchNumMap : matchNums) {
            //for each match
            int matchNum =(int) matchNumMap.get(Constants.SQLColumnName.MATCH_NUM.toString());
            HashMap<String, HashMap<String, Object>> tbaMatchBreakdown = null;
            if (competitionData != null) {

                Optional<Object> optional = competitionData.toList().stream().filter(o -> Objects.equals(((HashMap<String, String>) o).get("key"), eventCode + "_qm" + matchNum)).findFirst();
                if (optional.isPresent()) {
                    HashMap<String, HashMap<String, HashMap<String, Object>>> matchObject = (HashMap<String, HashMap<String, HashMap<String, Object>>>) optional.get();
                    tbaMatchBreakdown = matchObject.get("score_breakdown");
                }else {
                    Logging.logInfo("using null breakdown for match exporting "  + matchNum);
                }
        }
            ArrayList<HashMap<String, Object>> matchScoutData = SQLUtil.exec("SELECT * FROM \"" + tableName + "\"" + " WHERE " + Constants.SQLColumnName.MATCH_NUM + "=?", new Object[]{matchNum}, false);
            //we have all the data for a match
            //do the same thing to both the red and blue alliances

            ArrayList<HashMap<String, Object>> redDataForThisMatch = matchScoutData.stream().filter(map -> (int) map.getOrDefault(Constants.SQLColumnName.ALLIANCE_POS.toString(), 0) < 3).collect(Collectors.toCollection(ArrayList::new));
            exportAlliance(redDataForThisMatch, output, tbaMatchBreakdown);
            ArrayList<HashMap<String, Object>> blueDataForThisMatch = matchScoutData.stream().filter(map -> (int) map.getOrDefault(Constants.SQLColumnName.ALLIANCE_POS.toString(), 0) > 2).collect(Collectors.toCollection(ArrayList::new));
            exportAlliance(blueDataForThisMatch, output, tbaMatchBreakdown);


        }//end for each match
        for (HashMap<String, Object> map : teamsScouted) {
            int teamNum = (int) map.get(Constants.SQLColumnName.TEAM_NUM.toString());
            if (teamsToSkip.contains(teamNum)) {
                continue;
            }
            ArrayList<HashMap<String, Object>> teamsMatches = SQLUtil.exec("SELECT " + Constants.SQLColumnName.TELE_COMMENTS + " FROM \"" + tableName + "\" WHERE " + Constants.SQLColumnName.TEAM_NUM + "=?", new Object[]{String.valueOf(teamNum)}, false);
            ArrayList<String> row = new ArrayList<>();
            row.add(String.valueOf(teamNum));
            for (HashMap<String, Object> teamsMatch : teamsMatches) {
                try {
                    row.add(teamsMatch.get(Constants.SQLColumnName.TELE_COMMENTS.toString()).toString().split(":")[2].replace("No Comments", ""));

                }catch (Exception e) {
                    row.add(teamsMatch.get(Constants.SQLColumnName.TELE_COMMENTS.toString()).toString().replace("No Comments", ""));
                }
            }
            output.add(row);
        }
        SpreadsheetUtil.writeArrayToSpreadsheet(output, outputFile);

    }

    private static void exportAlliance(ArrayList<HashMap<String, Object>> dataForThisAlliance, ArrayList<ArrayList<String>> output, HashMap<String, HashMap<String, Object>> tbaMatchBreakdown) {
        ArrayList<HashMap<String, Object>> teamsWhoRecievedShuttledNotes = new ArrayList<>();
        ArrayList<HashMap<String, Object>> teamsWhoDidNotParticipateInShutteling = new ArrayList<>();
        ArrayList<HashMap<String, Object>> teamsWhoShuttled = new ArrayList<>();
        dataForThisAlliance.forEach(map -> {
            if (map.get(Constants.SQLColumnName.TELE_COMMENTS.toString()).toString().toLowerCase().contains(Constants.SHUTTLED_NOTE_IDENTIFIER)) {
                teamsWhoRecievedShuttledNotes.add(map);
            }else if ((int) map.get(Constants.SQLColumnName.SHUTTLED.toString()) <= Constants.SHUTTLED_NOTE_THRESHOLD) {
                teamsWhoDidNotParticipateInShutteling.add(map);
            }else {
                teamsWhoShuttled.add(map);
            }
        });
        final int[] maxShuttleing = {0};
        dataForThisAlliance.forEach(map -> maxShuttleing[0] = Math.max((int) map.get(Constants.SQLColumnName.SHUTTLED.toString()), maxShuttleing[0]));
        if (!teamsWhoRecievedShuttledNotes.isEmpty() && !teamsWhoShuttled.isEmpty()) {
            //so we have to adjust the notes for all the teams who shuttled
            //take the amount of notes scored according to tba and subtract notes scored by teams who are not recieving from it

            //for speaker
            //subtract notes scored by teams who did not participate
            final int[] notesToDistribute = {0};
            dataForThisAlliance.forEach(map -> notesToDistribute[0] = (int) map.get(Constants.SQLColumnName.TELE_SPEAKER.toString()) + notesToDistribute[0]);
            teamsWhoDidNotParticipateInShutteling.forEach(map -> notesToDistribute[0] = notesToDistribute[0] -  (int) map.get(Constants.SQLColumnName.TELE_SPEAKER.toString()));//subtract other teams contribtution
            double teleSpeakerAdjusted = (double) notesToDistribute[0] /(teamsWhoShuttled.size() + teamsWhoRecievedShuttledNotes.size());


            //add the data
            teamsWhoRecievedShuttledNotes.forEach(map -> output.add(getMatchScoreRow(map, tbaMatchBreakdown, true, teleSpeakerAdjusted)));
            teamsWhoShuttled.forEach(map -> output.add(getMatchScoreRow(map, tbaMatchBreakdown, true, teleSpeakerAdjusted)));
            teamsWhoDidNotParticipateInShutteling.forEach(map -> output.add(getMatchScoreRow(map, tbaMatchBreakdown, false, 0)));

        }else {
            //then nobody was recieving shuttled notes according to scouts
            for (HashMap<String, Object> matchScoutDatum : dataForThisAlliance) {
                output.add(getMatchScoreRow(matchScoutDatum, tbaMatchBreakdown, false, 0.0));
            }
        }
    }

    private static ArrayList<String> getMatchScoreRow(HashMap<String, Object> sqlRow, HashMap<String, HashMap<String, Object>> matchBreakdown, boolean isShuttlingMatch, double
            teleSpeakerAdjusted) {
        ArrayList<String> output = new ArrayList<>();
        //add all the raw data and adds to an arraylist with a corresponding #

        HashMap<Constants.SQLColumnName,Object> tempHashmap = new HashMap<>();
        ArrayList<HashMap<Constants.SQLColumnName,Object>> valueHolder = new ArrayList<>();
        ArrayList<Constants.SQLColumnName> nameHolder = new ArrayList<>(Arrays.asList(Constants.SQLColumnName.values()));
        AtomicInteger i = new AtomicInteger(0);
        //Raw
        getRawDataMetrics().forEach(dataMetric -> {
                    output.add((String) sqlRow.get(nameHolder.get(i.get()).toString()));
                    System.out.println(i.get() + ", " + nameHolder.get(i.get()).toString());
                    i.getAndIncrement();
                });
        Configuration.getMetric(Constants.SQLColumnName.TELE_SPEAKER.name());
        //set default values for data from tba
        boolean autoLeave = false;
        int climbPoints = 0;
        int endgame = 0;

        //try and set values if we have noteA breakdown avaliable
        if (matchBreakdown != null) {
            int robotPosition = (int) sqlRow.get(Constants.SQLColumnName.ALLIANCE_POS.toString());
            HashMap<String, Object> allianceBreakdown = matchBreakdown.get((robotPosition < 3)?"red":"blue");
            int tbaRobotNum = (((int) sqlRow.get(Constants.SQLColumnName.ALLIANCE_POS.toString())) % 3) + 1;
            autoLeave = Objects.equals(allianceBreakdown.get("autoLineRobot" + tbaRobotNum), "Yes");
            switch (allianceBreakdown.get("endGameRobot" + tbaRobotNum).toString()) {
                case "Parked": {
                    climbPoints = 1;
                    endgame = 1;
                    break;
                }
                case "CenterStage", "StageLeft", "StageRight": {
                    climbPoints = 3;
                    endgame = 2;
                    break;
                }

            }
        }
    int autoAmp = DatabaseViewerTabContent.getIntFromEntryMap(Constants.SQLColumnName.AUTO_AMP, sqlRow);
    int autoSpeaker = DatabaseViewerTabContent.getIntFromEntryMap(Constants.SQLColumnName.AUTO_SPEAKER, sqlRow);
    int teleAmp = DatabaseViewerTabContent.getIntFromEntryMap(Constants.SQLColumnName.TELE_AMP, sqlRow);
    int teleSpeaker = DatabaseViewerTabContent.getIntFromEntryMap(Constants.SQLColumnName.TELE_SPEAKER, sqlRow);
        //Custom
//need to set function to make added value types
        int teleAmpPoints = teleAmp * Constants.TELE_AMP_NOTE_POINTS;
        int teleSpeakerPoints = teleSpeaker * Constants.TELE_SPEAKER_NOTE_POINTS;
        int trapPoints = ((int) sqlRow.get(Constants.SQLColumnName.TELE_TRAP.toString())) * Constants.TELE_TRAP_POINTS;
        int telePoints = teleAmpPoints + teleSpeakerPoints + trapPoints + climbPoints;
        int teleNotes = teleAmp + teleSpeaker;
        int autoPoints = (autoAmp * Constants.AUTO_AMP_NOTE_POINTS) + (autoSpeaker * Constants.AUTO_SPEAKER_NOTE_POINTS) + (autoLeave ? 2 : 0);
        int autoNotes = autoAmp + autoSpeaker;
        int totalNotesMissed = ((int) sqlRow.get(Constants.SQLColumnName.AUTO_AMP_MISSED.toString())) + ((int) sqlRow.get(Constants.SQLColumnName.AUTO_SPEAKER_MISSED.toString())) + ((int) sqlRow.get(Constants.SQLColumnName.TELE_SPEAKER_MISSED.toString())) + ((int) sqlRow.get(Constants.SQLColumnName.TELE_AMP_MISSED.toString()));
        int totalPoints = autoPoints + telePoints;
        int totalNotesMade = autoAmp + autoSpeaker + teleAmp + teleSpeaker;
        int totalNotes = totalNotesMissed + totalNotesMade;
        //custom
        output.add(autoLeave?"1": "0");//auto leave
        output.add(String.valueOf(endgame));//endgame position
        output.add("End Raw Data");//raw data divider
        output.add(String.valueOf(autoNotes));//total auto notes
        output.add(String.valueOf(teleNotes));//total tele notes
        output.add(String.valueOf(autoPoints));//auto points added
        output.add(String.valueOf(telePoints));//tele points added
        output.add(String.valueOf(totalPoints));//total points added
        output.add(String.valueOf(totalNotesMade));//total notes scored
        output.add(String.valueOf(totalNotesMissed));//total notes missed
        output.add(String.valueOf(totalNotes));//total notes
        output.add(isShuttlingMatch?String.valueOf(teleSpeakerAdjusted):String.valueOf(teleSpeaker));
        output.add(isShuttlingMatch?"1":"0");//boolean which indicated whether there was a significant amount of shuttling done by this team

        //Comments V
        String scoutName = "No name provided";
        String[] commentData = sqlRow.get(Constants.SQLColumnName.TELE_COMMENTS.toString()).toString().split(":");
        if (commentData.length >= 4) {
            scoutName = commentData[3];
            output.add(scoutName);//scout name
            output.add(commentData[1]);//raw auto
            output.add(commentData[2]);//comments
        }else {
            output.add("Error, check tele comments raw");//scout name
            output.add("Error, check tele comments raw");//raw auto
            output.add("Error, check tele comments raw");//comments
        }

        return output;

    }
    public int PointValue(ArrayList output, int value, Constants.SQLColumnName n){
        int newValue;
        switch (n){
            case AUTO_SPEAKER -> {
                newValue = Constants.AUTO_SPEAKER_NOTE_POINTS * value;
            }
            case AUTO_AMP -> {
                newValue =Constants.AUTO_AMP_NOTE_POINTS * value;
                break;
            }
            case TELE_SPEAKER -> {
                newValue =Constants.TELE_SPEAKER_NOTE_POINTS * value;
                break;
            }
            case TELE_AMP -> {
                newValue =Constants.TELE_AMP_NOTE_POINTS * value;
                break;
            }
            case TELE_TRAP -> {
                newValue =Constants.TELE_TRAP_POINTS * value;
                break;
            }
            default -> {
                newValue = value;
            }
        }
        return newValue;
    }
}

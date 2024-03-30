package org.tahomarobotics.scouting.scoutingserver;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.util.APIUtil;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.OperationAbortedByUserException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Exporter {

    private JSONArray competitionData;
    private final String eventCode;
    private boolean haveMatches = true;

    private final ArrayList<HashMap<String, Object>> teamsScouted;

    private final String tableName;

    private final ArrayList<Integer> teamsToSkip = new ArrayList<>();

    public Exporter(String theEventCode, String activeTableName) throws IOException, InterruptedException, OperationAbortedByUserException, SQLException {
        eventCode = theEventCode;
        tableName = activeTableName;

        teamsScouted = SQLUtil.exec("SELECT DISTINCT " + Constants.SQLColumnName.TEAM_NUM + " FROM \"" + tableName + "\"", true);
        teamsScouted.sort(Comparator.comparingInt(o -> Integer.parseInt(o.get(Constants.SQLColumnName.TEAM_NUM.toString()).toString())));


        Optional<JSONArray> optionalEventMatches = APIUtil.getEventMatches(eventCode);
        if (optionalEventMatches.isEmpty()) {
            Logging.logInfo("Failed to fetch matches from tba");
            haveMatches = false;
            //only confinue after alerting the user to the risks of exporing without internet
            if (Constants.askQuestion("You are trying to export data without matches from tba, doing so will result in incorrect data because you cannot access TBA Continue?")) {

                throw new OperationAbortedByUserException("User aborted operation due to inability to fetch matches");
            }
        }
        competitionData = optionalEventMatches.get();

    }


    public ArrayList<ArrayList<String>> export(boolean exportNotes) throws SQLException {
        ArrayList<ArrayList<String>> output = new ArrayList<>();
        //add title row to export
        ArrayList<String> titleRow = new ArrayList<>();
        Arrays.stream(Constants.SQLColumnName.values()).toList().forEach(sqlColumnName -> titleRow.add(sqlColumnName.name()));
        Arrays.stream(Constants.ExportedDataMetrics.values()).toList().forEach(exportedDataMetrics -> titleRow.add(exportedDataMetrics.name()));
        output.add(titleRow);
        //add raw and exported data
        ArrayList<HashMap<String, Object>> matchNums = SQLUtil.exec("SELECT DISTINCT " + Constants.SQLColumnName.MATCH_NUM + " FROM \"" + tableName + "\"", false);
        for (HashMap<String, Object> matchNumMap : matchNums) {
            int matchNum =(int) matchNumMap.get(Constants.SQLColumnName.MATCH_NUM.toString());
            HashMap<String, HashMap<String, Object>> tbaMatchBreakdown = null;
            if (haveMatches) {

                Optional<Object> optional = competitionData.toList().stream().filter(o -> Objects.equals(((HashMap<String, String>) o).get("key"), eventCode + "_qm" + matchNum)).findFirst();
                if (optional.isPresent()) {
                    HashMap<String, HashMap<String, HashMap<String, Object>>> matchObject = (HashMap<String, HashMap<String, HashMap<String, Object>>>) optional.get();
                    tbaMatchBreakdown = matchObject.get("score_breakdown");
                }else {
                    Logging.logInfo("using null breakdown for match exporting "  + matchNum);
                }
            }
            ArrayList<HashMap<String, Object>> matchScoutData = SQLUtil.exec("SELECT * FROM \"" + tableName + "\"" + " WHERE " + Constants.SQLColumnName.MATCH_NUM + "=?", new Object[]{matchNum}, false);
            for (HashMap<String, Object> matchScoutDatum : matchScoutData) {
                output.add(getRow(matchScoutDatum, tbaMatchBreakdown));
            }
        }//end for ecah match
        //export notes if appropriate

        if (exportNotes) {
            for (HashMap<String, Object> map : teamsScouted) {
                int teamNum = (int) map.get(Constants.SQLColumnName.TEAM_NUM.toString());
                if (teamsToSkip.contains(teamNum)) {
                    continue;
                }
                ArrayList<HashMap<String, Object>> teamsMatches = SQLUtil.exec("SELECT " + Constants.SQLColumnName.TELE_COMMENTS + " FROM \"" + tableName + "\" WHERE " + Constants.SQLColumnName.TEAM_NUM + "=?", new Object[]{String.valueOf(teamNum)}, false);
                ArrayList<String> row = new ArrayList<>();
                row.add(String.valueOf(teamNum));
                for (HashMap<String, Object> teamsMatch : teamsMatches) {
                    row.add(teamsMatch.get(Constants.SQLColumnName.TELE_COMMENTS.toString()).toString().replace("No Comments", ""));
                }
                output.add(row);
            }
        }
        return output;

    }

    private ArrayList<String> getRow(HashMap<String, Object> sqlRow, HashMap<String, HashMap<String, Object>> matchBreakdown) {
        ArrayList<String> output = new ArrayList<>();
        //add all the raw data
        for (Constants.SQLColumnName sqlColumnName : Constants.SQLColumnName.values()) {
            output.add(sqlRow.get(sqlColumnName.toString()).toString());
        }
        //set default values for data from tba
        boolean autoLeave = false;
        int climbPoints = 0;
        int endgame = 0;

        //try and set values if we have a breakdown avaliable
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

        //calculate calculated values
        int autoAmp = (int) sqlRow.get(Constants.SQLColumnName.AUTO_AMP.toString());
        int autoSpeaker = (int) sqlRow.get(Constants.SQLColumnName.AUTO_SPEAKER.toString());
        int teleAmp = (int) sqlRow.get(Constants.SQLColumnName.TELE_AMP.toString());
        int teleSpeaker = (int) sqlRow.get(Constants.SQLColumnName.TELE_SPEAKER.toString());

        int teleAmpPoints = teleAmp * Constants.TELE_AMP_NOTE_POINTS;
        int teleSpeakerPoints = teleSpeaker * Constants.TELE_SPEAKER_NOTE_POINTS;
        int trapPoints = ((int) sqlRow.get(Constants.SQLColumnName.TELE_TRAP.toString())) * Constants.TELE_TRAP_POINTS;
        int telePoints = teleAmpPoints + teleSpeakerPoints + trapPoints + climbPoints;
        int autoPoints = (autoAmp * Constants.AUTO_AMP_NOTE_POINTS) + (autoSpeaker * Constants.AUTO_SPEAKER_NOTE_POINTS) + (autoLeave ? 2 : 0);
        int toalNotesMissed = ((int) sqlRow.get(Constants.SQLColumnName.AUTO_AMP_MISSED.toString())) + ((int) sqlRow.get(Constants.SQLColumnName.AUTO_SPEAKER_MISSED.toString())) + ((int) sqlRow.get(Constants.SQLColumnName.TELE_SPEAKER_MISSED.toString())) + ((int) sqlRow.get(Constants.SQLColumnName.TELE_AMP_MISSED.toString()));

        output.add(autoLeave?"1": "0");//auto leave
        output.add(String.valueOf(endgame));//endgame positon
        output.add("End Raw Data");//raw data divider
        output.add(String.valueOf(autoAmp + autoSpeaker));//total auto notes
        output.add(String.valueOf(teleAmp + teleSpeaker));//total tele notes
        output.add(String.valueOf(autoPoints));//auto points added
        output.add(String.valueOf(telePoints));//tele points added
        output.add(String.valueOf(autoPoints + telePoints));//total points added
        output.add(String.valueOf(autoAmp + autoSpeaker + teleAmp + teleSpeaker));//total notes scored
        output.add(String.valueOf(toalNotesMissed));//total notes missed
        output.add(String.valueOf(toalNotesMissed + autoAmp + autoSpeaker + teleAmp + teleSpeaker));//total notes
        return output;

    }




}

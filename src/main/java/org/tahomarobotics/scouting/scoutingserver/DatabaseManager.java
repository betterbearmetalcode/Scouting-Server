package org.tahomarobotics.scouting.scoutingserver;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.MatchRecordComparator;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.Robot;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;

public class DatabaseManager {


    public static void storeRawQRData(long timestamp, String dataRaw, String tablename) throws IOException {
        try {
            String[] data = dataRaw.split(Constants.QR_DATA_DELIMITER);
            MatchRecord m = new MatchRecord(timestamp,
                    Integer.parseInt(data[0]),//match num
                    Integer.parseInt(data[1]),//team num
                    getRobotPositionFromNum(Integer.parseInt(data[2])),//allinace pos
                    (Objects.equals(data[3], "1")),//auto leave
                    Integer.parseInt(data[4]),//auto speaker
                    Integer.parseInt(data[5]),//auto amp
                    Integer.parseInt(data[6]),//auto collected
                    Integer.parseInt(data[7]),//auto speaker missed
                    Integer.parseInt(data[8]),//auto amp missed
                    Integer.parseInt(data[9]),//tele speaker
                    Integer.parseInt(data[10]),//tele amp
                    Integer.parseInt(data[11]),//tele trap
                    Integer.parseInt(data[12]),//tele speakermissed
                    Integer.parseInt(data[13]),//tele amp missed

                    getEngamePositionFromNum(Integer.parseInt(data[14])),//endgame pos
                    data[15],//auto notes
                    data[16]);//tele notes
            SQLUtil.execNoReturn("INSERT INTO " + tablename + " VALUES (" + m.getDataForSQL() + ")");
        } catch (NumberFormatException e) {
            System.err.println("Failed to construct MatchRecord, likly corruppted Data");
            Logging.logError(e);
        } catch (SQLException e) {
            Logging.logError(e);
        }
    }

    public static void storeRawQRData(long timestamp, JSONObject dataJSON, String tablename) throws IOException {
        storeRawQRData(timestamp, dataJSON.getString( (String) Arrays.stream(dataJSON.keySet().toArray()).toList().get(0)), tablename);

    }


    public static LinkedList<MatchRecord> readDatabase(String tableName) throws IOException {
        LinkedList<MatchRecord> output = new LinkedList<>();
        try {
            ArrayList<HashMap<String, Object>> data = SQLUtil.exec("SELECT * FROM \"" + tableName + "\"");
            for (HashMap<String, Object> row : data) {
                //for each row in the sql database
                output.add(new MatchRecord(
                        (long) row.get(Constants.ColumnName.TIMESTAMP.toString().toUpperCase()),
                        (int) row.get(Constants.ColumnName.MATCH_NUM.toString()),
                        (int) row.get(Constants.ColumnName.TEAM_NUM.toString()),
                        getRobotPositionFromNum((int) row.get(Constants.ColumnName.ALLIANCE_POS.toString())),
                        (((int) row.get(Constants.ColumnName.AUTO_LEAVE.toString())) == 1),
                        (int) row.get(Constants.ColumnName.AUTO_SPEAKER.toString()),
                        (int) row.get(Constants.ColumnName.AUTO_AMP.toString()),
                        (int) row.get(Constants.ColumnName.AUTO_COLLECTED.toString()),
                        (int) row.get(Constants.ColumnName.AUTO_SPEAKER_MISSED.toString()),
                        (int) row.get(Constants.ColumnName.AUTO_AMP_MISSED.toString()),

                        (int) row.get(Constants.ColumnName.TELE_SPEAKER.toString()),
                        (int) row.get(Constants.ColumnName.TELE_AMP.toString()),
                        (int) row.get(Constants.ColumnName.TELE_TRAP.toString()),
                        (int) row.get(Constants.ColumnName.TELE_SPEAKER_MISSED.toString()),
                        (int) row.get(Constants.ColumnName.TELE_AMP_MISSED.toString()),
                        getEngamePositionFromNum((int) row.get(Constants.ColumnName.ENDGAME_POS.toString())),
                        (String) row.get(Constants.ColumnName.AUTO_COMMENTS.toString()),
                        (String) row.get(Constants.ColumnName.TELE_COMMENTS.toString())

                ));
            }
        } catch (SQLException e) {
            Logging.logError(e);
        }
        return output;
    }


    //utility methods

    public enum RobotPosition {
        R1,
        R2,
        R3,
        B1,
        B2,
        B3
    }


    public static RobotPosition getRobotPositionFromNum(int num) {
        switch (num) {

            case 0 -> {
                return RobotPosition.R1;
            }
            case 1 -> {
                return RobotPosition.R2;
            }
            case 2 -> {
                return RobotPosition.R3;
            }
            case 3 -> {
                return RobotPosition.B1;
            }
            case 4 -> {
                return RobotPosition.B2;
            }
            case 5 -> {
                return RobotPosition.B3;
            }
            default -> throw new IllegalStateException("Unexpected value: " + num);
        }
    }

    public enum EndgamePosition {
        NONE,
        PARKED,
        CLIMBED,
        HARMONIZED
    }


    public static EndgamePosition getEngamePositionFromNum(int pos) {
        switch (pos) {

            case 0 -> {
                return EndgamePosition.NONE;
            }
            case 1 -> {
                return EndgamePosition.PARKED;
            }
            case 2 -> {
                return EndgamePosition.CLIMBED;
            }
            case 3 -> {
                return EndgamePosition.HARMONIZED;
            }
            default -> throw new IllegalStateException("Unexpected value: " + pos);
        }
    }

    public record MatchRecord(long timestamp,
                              int matchNumber,
                              int teamNumber,
                              RobotPosition position,
                              boolean autoLeave,
                              int autoSpeaker,
                              int autoAmp,
                              int autoCollected,
                              int autoSpeakerMissed,
                              int autoAmpMissed,


                              int teleSpeaker,
                              int teleAmp,
                              int teleTrap,
                              int teleSpeakerMissed,
                              int teleAmpMissed,
                              EndgamePosition endgamePosition,
                              String autoNotes,
                              String teleNotes
    ) {

        //for exporting
        public LinkedList<DataPoint> getDataAsList() {
            LinkedList<DataPoint> output = new LinkedList<>();


            output.add(new DataPoint(Constants.ColumnName.TIMESTAMP.toString(), String.valueOf(timestamp)));
            output.add(new DataPoint(Constants.ColumnName.MATCH_NUM.toString(), String.valueOf(matchNumber)));
            output.add(new DataPoint(Constants.ColumnName.TEAM_NUM.toString(), String.valueOf(teamNumber)));
            output.add(new DataPoint(Constants.ColumnName.ALLIANCE_POS.toString(), String.valueOf(position.ordinal())));
            output.add(new DataPoint(Constants.ColumnName.AUTO_LEAVE.toString(), autoLeave ? ("1") : ("0")));
            output.add(new DataPoint(Constants.ColumnName.AUTO_SPEAKER.toString(), String.valueOf(autoSpeaker)));
            output.add(new DataPoint(Constants.ColumnName.AUTO_AMP.toString(), String.valueOf(autoAmp)));
            output.add(new DataPoint(Constants.ColumnName.AUTO_COLLECTED.toString(), String.valueOf(autoCollected)));
            output.add(new DataPoint(Constants.ColumnName.AUTO_SPEAKER_MISSED.toString(), String.valueOf(autoSpeakerMissed)));
            output.add(new DataPoint(Constants.ColumnName.AUTO_AMP_MISSED.toString(), String.valueOf(autoAmpMissed)));
            output.add(new DataPoint(Constants.ColumnName.TELE_SPEAKER.toString(), String.valueOf(teleSpeaker)));
            output.add(new DataPoint(Constants.ColumnName.TELE_AMP.toString(), String.valueOf(teleAmp)));
            output.add(new DataPoint(Constants.ColumnName.TELE_TRAP.toString(), String.valueOf(teleTrap)));
            output.add(new DataPoint(Constants.ColumnName.TELE_SPEAKER_MISSED.toString(), String.valueOf(teleSpeakerMissed)));
            output.add(new DataPoint(Constants.ColumnName.TELE_AMP_MISSED.toString(), String.valueOf(teleAmpMissed)));
            output.add(new DataPoint(Constants.ColumnName.ENDGAME_POS.toString(), String.valueOf(endgamePosition.ordinal())));
            output.add(new DataPoint(Constants.ColumnName.AUTO_COMMENTS.toString(), "\"" + autoNotes + "\""));
            output.add(new DataPoint(Constants.ColumnName.TELE_COMMENTS.toString(), "\"" + teleNotes + "\""));


            int teleAmpPoints = teleAmp * Constants.TELE_AMP_NOTE_POINTS;
            int teleSpeakerPoints = teleSpeaker * Constants.TELE_SPEAKER_NOTE_POINTS;
            int trapPoints = teleTrap * Constants.TELE_TRAP_POINTS;
            int climbPoints = Constants.endgamePoints.get(endgamePosition);
            int telePoints = teleAmpPoints + teleSpeakerPoints + trapPoints + climbPoints;
            int autoPoints = (autoAmp * Constants.AUTO_AMP_NOTE_POINTS) + (autoSpeaker * Constants.AUTO_SPEAKER_NOTE_POINTS);
            int toalNotesScored = autoAmp + autoSpeaker + teleAmp + teleSpeaker;
            int toalNotesMissed = autoAmpMissed + autoSpeakerMissed + teleAmpMissed + teleSpeakerMissed;
            output.add(new DataPoint("Total Auto Notes", String.valueOf(autoAmp + autoSpeaker)));
            output.add(new DataPoint("Total Tele Notes", String.valueOf(teleAmp + teleSpeaker)));
            output.add(new DataPoint("Auto Points Added", String.valueOf(autoPoints)));
            output.add(new DataPoint("Tele Points Added", String.valueOf(telePoints)));
            output.add(new DataPoint("Total Points Added", String.valueOf(autoPoints + telePoints)));
            output.add(new DataPoint("Total Notes Scored", String.valueOf(toalNotesScored)));
            output.add(new DataPoint("Total Notes Missed", String.valueOf(toalNotesMissed)));
            output.add(new DataPoint("Total Notes", String.valueOf(toalNotesMissed + toalNotesScored)));
            return output;
        }

        public String getDataForSQL() {
            return timestamp + ", " +
                    matchNumber + ", " +
                    teamNumber + ", " +
                    position.ordinal() + ", " +
                    (autoLeave ? ("1") : ("0")) + ", " +
                    autoSpeaker + ", " +
                    autoAmp + ", " +
                    autoCollected + ", " +
                    autoSpeakerMissed + ", " +
                    autoAmpMissed + ", " +
                    teleSpeaker + ", " +
                    teleAmp + ", " +
                    teleTrap + ", " +
                    teleSpeakerMissed + ", " +
                    teleAmpMissed + ", " +
                    endgamePosition.ordinal() + ", " +
                    "\"" + autoNotes + "\", " +
                    "\"" + teleNotes + "\"";
/*            StringBuilder  builder = new StringBuilder();
            LinkedList<Pair<String, String>> data = getDataAsList();
            for (Pair<String, String> pair : data) {
                if (data.peekLast().equals(pair)) {
                    builder.append(pair.getValue());
                }else {
                    builder.append(pair.getValue()).append(", ");
                }

            }
            return builder.toString();*/
        }

        public JSONArray toJSON() {
            JSONArray output = new JSONArray();
            LinkedList<DataPoint> data = this.getDataAsList();
            for (DataPoint dataPoint : data) {
                output.put(new JSONObject(dataPoint.getName(), dataPoint.getValue()));
            }
            return output;
        }

    }

    public static ArrayList<Match> getUnCorrectedDataFromDatabase(String tableName) throws IOException {
        LinkedList<MatchRecord> rawData = readDatabase(tableName);
        rawData.sort(new MatchRecordComparator());//this ensures that the data is in order of ascending match and ascending robot position
        ArrayList<Match> output = new ArrayList<>();
        if (!rawData.isEmpty()) {
            int numMatches = rawData.getLast().matchNumber;
            for (int i  =1; i < numMatches + 1; i++) {
                //for each match that we have data on

                ArrayList<Robot> robots = new ArrayList<>();
                final int finalI = i;
                List<MatchRecord> rawRobots = rawData.stream().filter(matchRecord -> matchRecord.matchNumber == finalI).toList();
                for (MatchRecord robot : rawRobots) {
                    //for each robot in this match
                    robots.add(new Robot(robot.position, robot.teamNumber, robot.getDataAsList(), robot));
                }
                output.add(new Match(i, robots));

            }
        }else {
            output = new ArrayList<>();
        }

        return output;
    }

}

package org.tahomarobotics.scouting.scoutingserver;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.util.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

public class DataHandler {




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
                    data[15] ,//auto notes
                    data[16]);//tele notes
            DatabaseManager.execNoReturn("INSERT INTO " + tablename + " VALUES (" +m.getDataForSQL() + ")");
        }catch (NumberFormatException e) {
            System.err.println("Failed to construct MatchRecord, likly corruppted Data");
            Logging.logError(e);
        } catch (SQLException e) {
            Logging.logError(e);
        }
    }

    public static void storeRawQRData(long timestamp, JSONArray dataJSON, String tablename) throws IOException {
        try {

            MatchRecord m = new MatchRecord(timestamp,
                    dataJSON.getInt(0),//match num
                    dataJSON.getInt(1),//team num
                    getRobotPositionFromNum(dataJSON.getInt(2)),
                    dataJSON.getInt(3) == 1,//auto leave
                    dataJSON.getInt(4),//auto speaker
                    dataJSON.getInt(5),//auto amp
                    dataJSON.getInt(6),//auto collected
                    dataJSON.getInt(7),//auto speaker missed
                    dataJSON.getInt(8),//auto amp missed
                    dataJSON.getInt(9),//tele speaker
                    dataJSON.getInt(10),//tele amp
                    dataJSON.getInt(11),//tele trap
                    dataJSON.getInt(12),//tele speaker missed
                    dataJSON.getInt(13),//tele amp missed
                    getEngamePositionFromNum(dataJSON.getInt(14)),//endgame pos
                    dataJSON.getString(15),//auto notes
                    dataJSON.getString(16)//tele notes
                    );

            DatabaseManager.execNoReturn("INSERT INTO " + tablename + " VALUES (" +m.getDataForSQL() + ")");
        }catch (NumberFormatException e) {
            System.err.println("Failed to construct MatchRecord, likly corruppted Data");
            Logging.logError(e);
        } catch (SQLException e) {
            Logging.logError(e);
        }
    }





    public static LinkedList<MatchRecord> readDatabase(String tableName) throws IOException {
        LinkedList<MatchRecord> output = new LinkedList<>();
        try {
            ArrayList<HashMap<String, Object>> data = DatabaseManager.exec("SELECT * FROM \"" + tableName + "\"");
            for (HashMap<String, Object> row : data) {
                //for each row in the sql database
                output.add(new MatchRecord(
                        (long) row.get(Constants.ColumnName.TIMESTAMP.toString().toUpperCase()),
                        (int) row.get(Constants.ColumnName.MATCH_NUM.toString()),
                        (int) row.get(Constants.ColumnName.TEAM_NUM.toString()),
                        getRobotPositionFromNum((int) row.get(Constants.ColumnName.ALLIANCE_POS.toString())),
                        ( ((int)row.get(Constants.ColumnName.AUTO_LEAVE.toString())) == 1),
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
                        (String) row.get(Constants.ColumnName.AUTO_NOTES.toString()),
                        (String) row.get(Constants.ColumnName.TELE_NOTES.toString())

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
        public LinkedList<Pair<String, String>> getDataAsList() {
            LinkedList<Pair<String, String>> output = new LinkedList<>();



            output.add(new Pair<>("Timestamp",String.valueOf(timestamp)));
            output.add(new Pair<>("Match Number",String.valueOf(matchNumber)));
            output.add(new Pair<>("Team Number",String.valueOf(teamNumber)));
            output.add(new Pair<>("Robot Position",String.valueOf(position.ordinal())));
            output.add(new Pair<>("Auto Leave",autoLeave?("1"):("0")));
            output.add(new Pair<>("Auto Speaker",String.valueOf(autoSpeaker)));
            output.add(new Pair<>("Auto Amp",String.valueOf(autoAmp)));
            output.add(new Pair<>("Auto Collected",String.valueOf(autoCollected)));
            output.add(new Pair<>("Auto Speaker Missed",String.valueOf(autoSpeakerMissed)));
            output.add(new Pair<>("Auto Amp Missed",String.valueOf(autoAmpMissed)));
            output.add(new Pair<>("Tele Speaker",String.valueOf(teleSpeaker)));
            output.add(new Pair<>("Tele Amp",String.valueOf(teleAmp)));
            output.add(new Pair<>("Tele Trap",String.valueOf(teleTrap)));
            output.add(new Pair<>("Tele Speaker Missed",String.valueOf(teleSpeakerMissed)));
            output.add(new Pair<>("Tele Amp Missed",String.valueOf(teleAmpMissed)));
            output.add(new Pair<>("Endgame Position",String.valueOf(endgamePosition.ordinal())));
            output.add(new Pair<>("Auto Comments","\"" + autoNotes + "\""));
            output.add(new Pair<>("Tele Comments","\"" + teleNotes + "\""));



            int teleAmpPoints = teleAmp * Constants.TELE_AMP_NOTE_POINTS;
            int teleSpeakerPoints = teleSpeaker * Constants.TELE_SPEAKER_NOTE_POINTS;
            int trapPoints = teleTrap * Constants.TELE_TRAP_POINTS;
            int climbPoints = Constants.endgamePoints.get(endgamePosition);
            int telePoints = teleAmpPoints + teleSpeakerPoints + trapPoints + climbPoints;
            int autoPoints = (autoAmp * Constants.AUTO_AMP_NOTE_POINTS) + (autoSpeaker * Constants.AUTO_SPEAKER_NOTE_POINTS);
            int toalNotesScored = autoAmp + autoSpeaker + teleAmp + teleSpeaker;
            int toalNotesMissed = autoAmpMissed + autoSpeakerMissed + teleAmpMissed + teleSpeakerMissed;
            output.add(new Pair<>("Total Auto Notes", String.valueOf(autoAmp + autoSpeaker)));
            output.add(new Pair<>("Total Tele Notes", String.valueOf(teleAmp + teleSpeaker)));
            output.add(new Pair<>("Auto Points Added", String.valueOf(autoPoints)));
            output.add(new Pair<>("Tele Points Added", String.valueOf(telePoints)));
            output.add(new Pair<>("Total Points Added", String.valueOf(autoPoints + telePoints)));
            output.add(new Pair<>("Total Notes Scored", String.valueOf(toalNotesScored)));
            output.add(new Pair<>("Total Notes Missed", String.valueOf(toalNotesMissed)));
            output.add(new Pair<>("Total Notes", String.valueOf(toalNotesMissed + toalNotesScored)));
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
            LinkedList<Pair<String, String>> data = this.getDataAsList();
            for (Pair<String, String> pair : data) {
                output.put(new JSONObject(pair.getKey(), pair.getValue()));
            }
            return output;
        }

    }



}

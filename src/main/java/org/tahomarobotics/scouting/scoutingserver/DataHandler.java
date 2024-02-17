package org.tahomarobotics.scouting.scoutingserver;

import org.tahomarobotics.scouting.scoutingserver.util.DatabaseManager;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

public class DataHandler {




    public static void storeRawQRData(long timestamp, String dataRaw, String tablename) throws IOException {


        MatchRecord m = contstrucMatchRecord(timestamp, dataRaw);

        try {
            DatabaseManager.execNoReturn("INSERT INTO " + tablename + " VALUES (" +m.getDataForSQL() + ")");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private static MatchRecord contstrucMatchRecord(long timestamp, String qrRAW) {
        try {
            String[] data = qrRAW.split(Constants.QR_DATA_DELIMITER);
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
            return m;
        }catch (NumberFormatException e) {
            System.err.println("Failed to construct MatchRecord, likly corruppted Data");
            throw new RuntimeException(e);
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

            throw new RuntimeException(e);
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

    public static int getRobotPositionNum(RobotPosition position) {
        switch (position) {

            case R1 -> {
                return 0;
            }
            case R2 -> {
                return 1;
            }
            case R3 -> {
                return 2;
            }
            case B1 -> {
                return 3;
            }
            case B2 -> {
                return 4;
            }
            case B3 -> {
                return 5;
            }
            default -> throw new IllegalStateException("Unexpected value: " + position);
        }
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

    public static int getEngamePosition(EndgamePosition pos) {
        switch (pos) {

            case NONE -> {
                return 0;
            }
            case PARKED -> {
                return 1;
            }
            case CLIMBED -> {
                return 2;
            }
            case HARMONIZED -> {
                return 3;
            }
            default -> throw new IllegalStateException("Unexpected value: " + pos);
        }
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
        //for displaying the data in the server
        public LinkedList<String> getDisplayableDataAsList() {
            LinkedList<String> output = new LinkedList<>();
            output.add("AutoLeave: " + (autoLeave?("Left"):("Didn't leave")));
            output.add("Auto Speaker: " + autoSpeaker);
            output.add("Auto Amp: " + autoAmp);
            output.add("Auto Collected: " + autoCollected);
            output.add("Auto Speaker Missed: " + autoSpeakerMissed);
            output.add("Auto Amp Missed: " + autoAmpMissed);

            output.add("Tele-OP Speaker: " + teleSpeaker);
            output.add("Tele-OP Amp: " + teleAmp);
            output.add("Tele-OP trap: " + teleTrap);
            output.add("Tele Speaker Missed: " + teleSpeakerMissed);
            output.add("Tele Amp Missed: " + teleAmpMissed);
            output.add("Endgame: " + endgamePosition);
            output.add("Auto Notes: " + autoNotes);
            output.add("Tele Notes: " + teleNotes);
            return output;
        }

        //for exporting
        public LinkedList<String> getDataAsList() {
            LinkedList<String> output = new LinkedList<>();
            output.add(String.valueOf(timestamp));
            output.add(String.valueOf(matchNumber));
            output.add(String.valueOf(teamNumber));
            output.add(String.valueOf(position.ordinal()));
            output.add(autoLeave?("1"):("0"));
            output.add(String.valueOf(autoSpeaker));
            output.add(String.valueOf(autoAmp));
            output.add(String.valueOf(autoCollected));
            output.add(String.valueOf(autoSpeakerMissed));
            output.add(String.valueOf(autoAmpMissed));
            output.add(String.valueOf(teleSpeaker));
            output.add(String.valueOf(teleAmp));
            output.add(String.valueOf(teleTrap));
            output.add(String.valueOf(teleSpeakerMissed));
            output.add(String.valueOf(teleAmpMissed));
            output.add(String.valueOf(endgamePosition.ordinal()));
            output.add(autoNotes);
            output.add(teleNotes);
            return output;
        }

        public String getDataForSQL() {
            StringBuilder builder = new StringBuilder();
            builder.append(timestamp).append(", ");
            builder.append(matchNumber).append(", ");
            builder.append(teamNumber).append(", ");
            builder.append(position.ordinal()).append(", ");
            builder.append(autoLeave?("1"):("0")).append(", ");
            builder.append(autoSpeaker).append(", ");
            builder.append(autoAmp).append(", ");
            builder.append(autoCollected).append(", ");
            builder.append(autoSpeakerMissed).append(", ");
            builder.append(autoAmpMissed).append(", ");
            builder.append(teleSpeaker).append(", ");
            builder.append(teleAmp).append(", ");
            builder.append(teleTrap).append(", ");
            builder.append(teleSpeakerMissed).append(", ");
            builder.append(teleAmpMissed).append(", ");
            builder.append(endgamePosition.ordinal()).append(", ");
            builder.append("\"").append(autoNotes).append("\", ");
            builder.append("\"").append(teleNotes).append("\"");
            return  builder.toString();

        }

    }



}

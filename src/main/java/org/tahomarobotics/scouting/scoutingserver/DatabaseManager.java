package org.tahomarobotics.scouting.scoutingserver;

import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.MatchRecordComparator;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.RobotPositon;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class DatabaseManager {


    public static void storeRawQRData(String dataRaw, String tablename) throws IOException {
        try {
            String[] data = dataRaw.split(Constants.QR_DATA_DELIMITER);
            QRRecord m = new QRRecord(Integer.parseInt(data[0]),//match num
                    Integer.parseInt(data[1]),//team num
                    getRobotPositionFromNum(Integer.parseInt(data[2])),//allinace pos
                    Integer.parseInt(data[3]),//auto speaker
                    Integer.parseInt(data[4]),//auto amp
                    Integer.parseInt(data[5]),//auto speaker missed
                    Integer.parseInt(data[6]),//auto amp missed
                    Integer.parseInt(data[7]),//F1
                    Integer.parseInt(data[8]),//F2
                    Integer.parseInt(data[9]),//F3
                    Integer.parseInt(data[10]),//M1
                    Integer.parseInt(data[11]),//M2
                    Integer.parseInt(data[12]),//M3
                    Integer.parseInt(data[13]),//M4
                    Integer.parseInt(data[14]),//M5
                    Integer.parseInt(data[15]),//tele speaker
                    Integer.parseInt(data[16]),//tele amp
                    Integer.parseInt(data[17]),//tele trap
                    Integer.parseInt(data[18]),//tele speakermissed
                    Integer.parseInt(data[19]),//tele amp missed
                    Integer.parseInt(data[20]),//lost comms
                    data[21]);//tele notes

            storeQrRecord(m, tablename);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            ScoutingServer.qrScannerController.writeToDataCollectionConsole("Failed to construct QrRecord, likly corrupted data", Color.RED);
            Logging.logError(e, "Failed to construct match record, most likly corrupted data");
        }
    }

    public static void importJSONObject(JSONObject object, String activeTable) {
        object.keys().forEachRemaining(s -> {
            JSONArray arr = object.getJSONArray(s);
            for (Object o : arr.toList()) {
                String string = o.toString();
                try {
                    DatabaseManager.storeRawQRData(string, activeTable);
                } catch (IOException e) {
                    Logging.logError(e, "failed to import qr string");
                }
            }
        });
    }

    public static void storeQrRecord(QRRecord record, String tablename) {
        try {
            SQLUtil.execNoReturn("INSERT INTO \"" + tablename + "\" VALUES (" + record.getDataForSQL() + ")", false);
            ScoutingServer.qrScannerController.writeToDataCollectionConsole("Wrote data to Database " + tablename + ": "+ record, Color.GREEN);
        } catch (SQLException e) {
            Logging.logError(e);
        } catch (DuplicateDataException e) {
            ScoutingServer.qrScannerController.writeToDataCollectionConsole("Duplicate Data Detected, skipping", Color.ORANGE);

        }

    }

    public static void storeRawQRData(JSONObject dataJSON, String tablename) throws IOException {
        storeRawQRData(dataJSON.getString( (String) Arrays.stream(dataJSON.keySet().toArray()).toList().get(0)), tablename);

    }


    public static LinkedList<QRRecord> readDatabase(String tableName) throws IOException {
        return readDatabase(tableName, "SELECT * FROM \"" + tableName + "\"", SQLUtil.EMPTY_PARAMS, true);
    }

    public static LinkedList<QRRecord> readDatabase(String tableName, String customStatement, Object[] params, boolean log) {
        LinkedList<QRRecord> output = new LinkedList<>();
        try {
            ArrayList<HashMap<String, Object>> data = SQLUtil.exec(customStatement, params, log);
            for (HashMap<String, Object> row : data) {
                //for each row in the sql database
                output.add(new QRRecord(
                        (int) row.get(Constants.SQLColumnName.MATCH_NUM.toString()),
                        (int) row.get(Constants.SQLColumnName.TEAM_NUM.toString()),
                        getRobotPositionFromNum((int) row.get(Constants.SQLColumnName.ALLIANCE_POS.toString())),
                        (int) row.get(Constants.SQLColumnName.AUTO_SPEAKER.toString()),
                        (int) row.get(Constants.SQLColumnName.AUTO_AMP.toString()),
                        (int) row.get(Constants.SQLColumnName.AUTO_SPEAKER_MISSED.toString()),
                        (int) row.get(Constants.SQLColumnName.AUTO_AMP_MISSED.toString()),
                        (int) row.get(Constants.SQLColumnName.F1.toString()),
                        (int) row.get(Constants.SQLColumnName.F2.toString()),
                        (int) row.get(Constants.SQLColumnName.F3.toString()),
                        (int) row.get(Constants.SQLColumnName.M1.toString()),
                        (int) row.get(Constants.SQLColumnName.M2.toString()),
                        (int) row.get(Constants.SQLColumnName.M3.toString()),
                        (int) row.get(Constants.SQLColumnName.M4.toString()),
                        (int) row.get(Constants.SQLColumnName.M5.toString()),

                        (int) row.get(Constants.SQLColumnName.TELE_SPEAKER.toString()),
                        (int) row.get(Constants.SQLColumnName.TELE_AMP.toString()),
                        (int) row.get(Constants.SQLColumnName.TELE_TRAP.toString()),
                        (int) row.get(Constants.SQLColumnName.TELE_SPEAKER_MISSED.toString()),
                        (int) row.get(Constants.SQLColumnName.TELE_AMP_MISSED.toString()),
                        (int) row.get(Constants.SQLColumnName.LOST_COMMS.toString()),
                        (String) row.get(Constants.SQLColumnName.TELE_COMMENTS.toString())

                ));
            }
        } catch (SQLException e) {
            Logging.logError(e, "failed to read database: " + tableName);
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
        B3,
        NO_DATA
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
            case 6 ->
            {
                return RobotPosition.NO_DATA;
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

    public record QRRecord(int matchNumber,
                           int teamNumber,
                           RobotPosition position,
                           int autoSpeaker,
                           int autoAmp,
                           int autoSpeakerMissed,
                           int autoAmpMissed,

                           int f1,//i have no idea what these are but they are being added anyway
                           int f2,
                           int f3,
                           int m1,
                           int m2,
                           int m3,
                           int m4,
                           int m5,


                           int teleSpeaker,
                           int teleAmp,
                           int teleTrap,
                           int teleSpeakerMissed,
                           int teleAmpMissed,
                           int lostComms,
                           String teleNotes
    ) {

        //for exporting
        public LinkedList<DataPoint> getDataAsList() {
            LinkedList<DataPoint> output = new LinkedList<>();

            output.add(new DataPoint(Constants.SQLColumnName.MATCH_NUM.toString(), String.valueOf(matchNumber)));
            output.add(new DataPoint(Constants.SQLColumnName.TEAM_NUM.toString(), String.valueOf(teamNumber)));
            output.add(new DataPoint(Constants.SQLColumnName.ALLIANCE_POS.toString(), String.valueOf(position.ordinal())));
            output.add(new DataPoint(Constants.SQLColumnName.AUTO_SPEAKER.toString(), String.valueOf(autoSpeaker)));
            output.add(new DataPoint(Constants.SQLColumnName.AUTO_AMP.toString(), String.valueOf(autoAmp)));
            output.add(new DataPoint(Constants.SQLColumnName.AUTO_SPEAKER_MISSED.toString(), String.valueOf(autoSpeakerMissed)));
            output.add(new DataPoint(Constants.SQLColumnName.AUTO_AMP_MISSED.toString(), String.valueOf(autoAmpMissed)));

            output.add(new DataPoint(Constants.SQLColumnName.F1.toString(), String.valueOf(f1)));
            output.add(new DataPoint(Constants.SQLColumnName.F2.toString(), String.valueOf(f2)));
            output.add(new DataPoint(Constants.SQLColumnName.F3.toString(), String.valueOf(f3)));
            output.add(new DataPoint(Constants.SQLColumnName.M1.toString(), String.valueOf(m1)));
            output.add(new DataPoint(Constants.SQLColumnName.M2.toString(), String.valueOf(m2)));
            output.add(new DataPoint(Constants.SQLColumnName.M3.toString(), String.valueOf(m3)));
            output.add(new DataPoint(Constants.SQLColumnName.M4.toString(), String.valueOf(m4)));
            output.add(new DataPoint(Constants.SQLColumnName.M5.toString(), String.valueOf(m5)));

            output.add(new DataPoint(Constants.SQLColumnName.TELE_SPEAKER.toString(), String.valueOf(teleSpeaker)));
            output.add(new DataPoint(Constants.SQLColumnName.TELE_AMP.toString(), String.valueOf(teleAmp)));
            output.add(new DataPoint(Constants.SQLColumnName.TELE_TRAP.toString(), String.valueOf(teleTrap)));
            output.add(new DataPoint(Constants.SQLColumnName.TELE_SPEAKER_MISSED.toString(), String.valueOf(teleSpeakerMissed)));
            output.add(new DataPoint(Constants.SQLColumnName.TELE_AMP_MISSED.toString(), String.valueOf(teleAmpMissed)));
            output.add(new DataPoint(Constants.SQLColumnName.LOST_COMMS.toString(), String.valueOf(lostComms)));
            output.add(new DataPoint(Constants.SQLColumnName.TELE_COMMENTS.toString(), "\"" + teleNotes + "\""));


            return output;
        }

        public String getDataForSQL() {
            return matchNumber + ", " +
                    teamNumber + ", " +
                    position.ordinal() + ", " +
                    autoSpeaker + ", " +
                    autoAmp + ", " +
                    autoSpeakerMissed + ", " +
                    autoAmpMissed + ", " +
                    f1 + ", " +
                    f2 + ", " +
                    f3 + ", " +
                    m1 + ", " +
                    m2 + ", " +
                    m3 + ", " +
                    m4 + ", " +
                    m5 + ", " +
                    teleSpeaker + ", " +
                    teleAmp + ", " +
                    teleTrap + ", " +
                    teleSpeakerMissed + ", " +
                    teleAmpMissed + ", " +
                    lostComms + ", " +
                    "\"" + teleNotes + "\"";
        }


    }

    public static ArrayList<Match> getUnCorrectedDataFromDatabase(String tableName) throws IOException {
        LinkedList<QRRecord> rawData = readDatabase(tableName);
        rawData.sort(new MatchRecordComparator());//this ensures that the data is in order of ascending match and ascending robot position
        ArrayList<Match> output = new ArrayList<>();
        if (!rawData.isEmpty()) {
            int numMatches = rawData.getLast().matchNumber;
            for (int i  =1; i < numMatches + 1; i++) {
                //for each match that we have data on

                ArrayList<RobotPositon> robotPositons = new ArrayList<>();
                final int finalI = i;
                List<QRRecord> rawRobots = rawData.stream().filter(matchRecord -> matchRecord.matchNumber == finalI).toList();
                for (QRRecord robot : rawRobots) {
                    //for each robot in this match
                    robotPositons.add(new RobotPositon(robot.position, robot.teamNumber, robot.getDataAsList(), robot));
                }
                output.add(new Match(i, robotPositons));

            }
        }else {
            output = new ArrayList<>();
        }

        return output;
    }

    public static double getAverage(Constants.SQLColumnName column, String teamName, String table, boolean log) throws SQLException {
        ArrayList<HashMap<String, Object>> raw = SQLUtil.exec("SELECT " + column + " FROM \"" + table + "\" WHERE " + Constants.SQLColumnName.TEAM_NUM + "=?", new Object[]{teamName}, log);
        ArrayList<Integer> data = new ArrayList<>();
        raw.forEach(map -> {data.add(Integer.valueOf(map.get(column.toString()).toString()));});
        return (double) data.stream().mapToInt(Integer::intValue).sum() /data.size();
    }
}

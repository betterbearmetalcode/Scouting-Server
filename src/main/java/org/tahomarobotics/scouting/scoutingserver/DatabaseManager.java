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

    //the king of storage methods, all the data storage methods eventually call this one, it is the final leg of the chain
    public static void storeQrRecord(QRRecord record, String tablename) throws DuplicateDataException {
        try {
            SQLUtil.execNoReturn("INSERT INTO \"" + tablename + "\" VALUES (" + record.getDataForSQL() + ")",new Object[]{}, false, record);
            ScoutingServer.qrScannerController.writeToDataCollectionConsole("Wrote data to Database " + tablename + ": "+ record, Color.GREEN);
        } catch (SQLException e) {
            Logging.logError(e);
        }

    }



    public static void storeRawQRData(String dataRaw, String tablename) throws IOException, DuplicateDataException {
        try {
            String[] data = dataRaw.split(Constants.QR_DATA_DELIMITER);
            if (data.length == 22) {
                //then its an old version of the data and we need to add a default A stop value
                String[] newData = new String[24];
                for (int i = 0; i < data.length + 2; i++) {
                    if (i < 15) {
                        newData[i] = data[i];
                    }else if (i == 15) {
                        newData[i] = "0";
                    }else if (i == 16){
                        newData[i] = "0";
                    }else {
                        newData[i] = data[i-2];
                    }

                }
                data = newData;
            }else if (data.length == 23) {
                //then its an old version of the data and we need to add a default A stop value
                String[] newData = new String[24];
                for (int i = 0; i < data.length + 1; i++) {
                    if (i < 15) {
                        newData[i] = data[i];
                    }else if (i == 16) {
                        newData[i] = "0";
                    }else {
                        newData[i] = data[i-1];
                    }

                }
                data = newData;
            }
           /* QRRecord m = new QRRecord(Integer.parseInt(data[0]),//match num
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
                    Integer.parseInt(data[15]),//a stop
                    Integer.parseInt(data[16]),
                    Integer.parseInt(data[17]),//tele speaker
                    Integer.parseInt(data[18]),//tele amp
                    Integer.parseInt(data[19]),//tele trap
                    Integer.parseInt(data[20]),//tele speakermissed
                    Integer.parseInt(data[21]),//tele amp missed
                    Integer.parseInt(data[22]),//lost comms
                    data[23]);//tele notes*/
            //scouting app is not coorrectly trasmitting which notes are where in qr string, this is to read them correctly
            //for future years  migrate to a new type of data transfer which has the column name attached to each data point
            //this will make compatibility and extension of what is colleted simpler
            QRRecord m = new QRRecord(Integer.parseInt(data[0]),//match num
                    Integer.parseInt(data[1]),//team num
                    getRobotPositionFromNum(Integer.parseInt(data[2])),//allinace pos
                    Integer.parseInt(data[3]),//auto speaker
                    Integer.parseInt(data[4]),//auto amp
                    Integer.parseInt(data[5]),//auto speaker missed
                    Integer.parseInt(data[6]),//auto amp missed
                    Integer.parseInt(data[9]),//F1
                    Integer.parseInt(data[8]),//F2
                    Integer.parseInt(data[7]),//F3
                    Integer.parseInt(data[14]),//M1
                    Integer.parseInt(data[13]),//M2
                    Integer.parseInt(data[12]),//M3
                    Integer.parseInt(data[11]),//M4
                    Integer.parseInt(data[10]),//M5
                    Integer.parseInt(data[15]),//a stop
                    Integer.parseInt(data[16]),//shuttled
                    Integer.parseInt(data[17]),//tele speaker
                    Integer.parseInt(data[18]),//tele amp
                    Integer.parseInt(data[19]),//tele trap
                    Integer.parseInt(data[20]),//tele speakermissed
                    Integer.parseInt(data[21]),//tele amp missed
                    Integer.parseInt(data[22]),//lost comms
                    data[23]);
            storeQrRecord(m, tablename);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | IllegalStateException e) {
            ScoutingServer.qrScannerController.writeToDataCollectionConsole("Failed to construct QrRecord, likly corrupted data", Color.RED);
            Logging.logError(e, "Failed to construct match record, most likly corrupted data");
        }
    }


    //this method will insert a json file into a sql table
    //if the table does not exist, one will be created, otherwise, it will be added
    //and duplicate data will be handled accordingly
    public static ArrayList<DuplicateDataException> saveJSONToSQLDatabase(JSONObject data, String tableName) {
        ArrayList<DuplicateDataException> duplicates = new ArrayList<>();
        JSONArray columnNames = data.getJSONArray(Constants.SCHEMA_DEFINTION_OBJECT_NAME);
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE IF NOT EXISTS \"").append(tableName).append("\"(");
        for (String key : data.keySet()) {
            //we only care about the robot positions
            if (Objects.equals(key, Constants.SCHEMA_DEFINTION_OBJECT_NAME)) {
                continue;
            }
            //each element of this array represents a scout's data for a match as defined by the column names array
            JSONArray robotPositionArray = data.getJSONArray(key);
            for (Object o : robotPositionArray) {
                JSONArray matchDatum = (JSONArray) o;//each of these arrays represpents a scout's data for one robot of each match in the order defined above

            }

            for (Object o : robotPositionArray.toList()) {
                String string = o.toString();
                try {
                    DatabaseManager.storeRawQRData(string, tableName);
                } catch (IOException e) {
                    Logging.logError(e, "failed to import qr string");
                } catch (DuplicateDataException e) {
                    duplicates.add(e);
                }
            }
        }
        return duplicates;
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
                output.add(getRecord(row));
            }
        } catch (SQLException e) {
            Logging.logError(e, "failed to read database: " + tableName);
        }
        return output;
    }

    public static QRRecord getRecord(HashMap<String, Object> rawData) {
        rawData.putIfAbsent(Constants.SQLColumnName.SHUTTLED.toString(), 0);
        rawData.putIfAbsent(Constants.SQLColumnName.A_STOP.toString(), 0);
        return new QRRecord(
                (int) rawData.get(Constants.SQLColumnName.MATCH_NUM.toString()),
                (int) rawData.get(Constants.SQLColumnName.TEAM_NUM.toString()),
                getRobotPositionFromNum((int) rawData.get(Constants.SQLColumnName.ALLIANCE_POS.toString())),
                (int) rawData.get(Constants.SQLColumnName.AUTO_SPEAKER.toString()),
                (int) rawData.get(Constants.SQLColumnName.AUTO_AMP.toString()),
                (int) rawData.get(Constants.SQLColumnName.AUTO_SPEAKER_MISSED.toString()),
                (int) rawData.get(Constants.SQLColumnName.AUTO_AMP_MISSED.toString()),
                (int) rawData.get(Constants.SQLColumnName.NOTE_A.toString()),
                (int) rawData.get(Constants.SQLColumnName.NOTE_B.toString()),
                (int) rawData.get(Constants.SQLColumnName.NOTE_C.toString()),
                (int) rawData.get(Constants.SQLColumnName.NOTE_1.toString()),
                (int) rawData.get(Constants.SQLColumnName.NOTE_2.toString()),
                (int) rawData.get(Constants.SQLColumnName.NOTE_3.toString()),
                (int) rawData.get(Constants.SQLColumnName.NOTE_4.toString()),
                (int) rawData.get(Constants.SQLColumnName.NOTE_5.toString()),
                (int) rawData.get(Constants.SQLColumnName.A_STOP.toString()),
                (int)rawData.get(Constants.SQLColumnName.SHUTTLED.toString()),
                (int) rawData.get(Constants.SQLColumnName.TELE_SPEAKER.toString()),
                (int) rawData.get(Constants.SQLColumnName.TELE_AMP.toString()),
                (int) rawData.get(Constants.SQLColumnName.TELE_TRAP.toString()),
                (int) rawData.get(Constants.SQLColumnName.TELE_SPEAKER_MISSED.toString()),
                (int) rawData.get(Constants.SQLColumnName.TELE_AMP_MISSED.toString()),
                (int) rawData.get(Constants.SQLColumnName.LOST_COMMS.toString()),
                (String) rawData.get(Constants.SQLColumnName.TELE_COMMENTS.toString()));
    }


    //utility methods

    public enum RobotPosition {
        R1,
        R2,
        R3,
        B1,
        B2,
        B3,
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
            default -> {
                Logging.logError(new IllegalStateException("Unexpected value when getting robot position from number, returning R1: " + num));
                return RobotPosition.R1;

            }
        }
    }

    public enum EndgamePosition {
        NONE,
        PARKED,
        CLIMBED,
        HARMONIZED
    }



    public record QRRecord(int matchNumber,
                           int teamNumber,
                           RobotPosition position,
                           int autoSpeaker,
                           int autoAmp,
                           int autoSpeakerMissed,
                           int autoAmpMissed,

                           int noteA,//i have no idea what these are but they are being added anyway
                           int noteB,
                           int noteC,
                           int note1,
                           int note2,
                           int note3,
                           int note4,
                           int note5,
                           int aStop,
                           int shuttled,
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

            output.add(new DataPoint(Constants.SQLColumnName.NOTE_A.toString(), String.valueOf(noteA)));
            output.add(new DataPoint(Constants.SQLColumnName.NOTE_B.toString(), String.valueOf(noteB)));
            output.add(new DataPoint(Constants.SQLColumnName.NOTE_C.toString(), String.valueOf(noteC)));
            output.add(new DataPoint(Constants.SQLColumnName.NOTE_1.toString(), String.valueOf(note1)));
            output.add(new DataPoint(Constants.SQLColumnName.NOTE_2.toString(), String.valueOf(note2)));
            output.add(new DataPoint(Constants.SQLColumnName.NOTE_3.toString(), String.valueOf(note3)));
            output.add(new DataPoint(Constants.SQLColumnName.NOTE_4.toString(), String.valueOf(note4)));
            output.add(new DataPoint(Constants.SQLColumnName.NOTE_5.toString(), String.valueOf(note5)));
            output.add(new DataPoint(Constants.SQLColumnName.A_STOP.toString(), String.valueOf(aStop)));
            output.add(new DataPoint(Constants.SQLColumnName.SHUTTLED.toString(), String.valueOf(shuttled)));

            output.add(new DataPoint(Constants.SQLColumnName.TELE_SPEAKER.toString(), String.valueOf(teleSpeaker)));
            output.add(new DataPoint(Constants.SQLColumnName.TELE_AMP.toString(), String.valueOf(teleAmp)));
            output.add(new DataPoint(Constants.SQLColumnName.TELE_TRAP.toString(), String.valueOf(teleTrap)));
            output.add(new DataPoint(Constants.SQLColumnName.TELE_SPEAKER_MISSED.toString(), String.valueOf(teleSpeakerMissed)));
            output.add(new DataPoint(Constants.SQLColumnName.TELE_AMP_MISSED.toString(), String.valueOf(teleAmpMissed)));
            output.add(new DataPoint(Constants.SQLColumnName.LOST_COMMS.toString(), String.valueOf(lostComms)));
            output.add(new DataPoint(Constants.SQLColumnName.TELE_COMMENTS.toString(), String.valueOf("\"" + teleNotes + "\"")));


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
                    noteA + ", " +
                    noteB + ", " +
                    noteC + ", " +
                    note1 + ", " +
                    note2 + ", " +
                    note3 + ", " +
                    note4 + ", " +
                    note5 + ", " +
                    aStop + ", " +
                    shuttled + ", " +
                    teleSpeaker + ", " +
                    teleAmp + ", " +
                    teleTrap + ", " +
                    teleSpeakerMissed + ", " +
                    teleAmpMissed + ", " +
                    lostComms + ", " +
                    "\"" + teleNotes + "\"";
        }

        public String getQRString() {
            StringBuilder qrBuilder = new StringBuilder();
            for (DataPoint dataPoint : this.getDataAsList()) {
                qrBuilder.append(dataPoint.getValue().replaceAll("\"", "")).append(Constants.QR_DATA_DELIMITER);
            }
            if (qrBuilder.toString().split(Constants.QR_DATA_DELIMITER).length == 21) {
                //this happens when there are no notes
                qrBuilder.replace(qrBuilder.length() - 1, qrBuilder.length(), "No Comments/");
            }
            return  qrBuilder.substring(0, qrBuilder.toString().length() - 1);
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

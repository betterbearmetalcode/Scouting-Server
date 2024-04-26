package org.tahomarobotics.scouting.scoutingserver;

import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.MatchRecordComparator;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.RobotPositon;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseManager {

    //the king of storage methods, all the data storage methods eventually call this one, it is the final leg of the chain
    public static void storeQrRecord(QRRecord record, String tablename) throws DuplicateDataException, SQLException {
        SQLUtil.execNoReturn("INSERT INTO \"" + tablename + "\" VALUES (" + record.getDataForSQL() + ")",new Object[]{}, false, record);
        ScoutingServer.dataCollectionController.writeToDataCollectionConsole("Wrote data to Database " + tablename + ": "+ record, Color.GREEN);

    }

   /* public static ArrayList<String> parseTeleCommentsToAutoData(String scoutInput) {
        if (scoutInput.contains(":")) {
            scoutInput = scoutInput.split(":")[1];
        }else {
            scoutInput = "";
        }
        ArrayList<String> output = new ArrayList<>();
        //standardize characters
        scoutInput = scoutInput.toLowerCase();
        //if there are any commas, only look at stuff before the comma, this is so the scout can write stuff with out messing up the
        //scouting server
        if (scoutInput.contains(",")) {
            scoutInput = scoutInput.split(",")[0];
        }
        //get rid of all characters we dont care about
        scoutInput = scoutInput.replaceAll("[^pabc12345m]+", "");
        //check if each note was collected and add the data accordingly
        Matcher m = Pattern.compile("[pabc12345]m?").matcher(scoutInput);
        while (m.find()) {
            if (output.size() < 9) {
                output.add(m.group());
            }

        }
        for (int i = output.size(); i < 9; i++) {
            output.add("None");
        }

        return output;
    }*/
    public static ArrayList<DuplicateDataException> importJSONArrayOfDataObjects(JSONArray data, String activeTable){
        try {
            Configuration.updateConfiguration();
        } catch (ParserConfigurationException | IOException | SAXException | ConfigFileFormatException e) {
           if (!Constants.askQuestion("Unable to update configuration, proceed with current configuration?")) {
               return new ArrayList<>();
           }
        }
        ArrayList<DuplicateDataException> duplicates = new ArrayList<>();
        int numErrors = 0;
        boolean showErrors = true;
        for (Object object : data) {
            try {
                SQLUtil.execNoReturn(getSQLStatementFromJSONJata((JSONObject) object, activeTable));
            } catch (DuplicateDataException e) {
                duplicates.add(e);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException | IllegalStateException |
                     SQLException e) {
                numErrors++;
                if (numErrors >= 3 && showErrors) {
                    showErrors = Constants.askQuestion("There have been " + numErrors + " errors so far in this import, continue showing alerts?");
                }
                ScoutingServer.dataCollectionController.writeToDataCollectionConsole("Failed to construct import datum", Color.RED);
                if (showErrors) {
                    Logging.logError(e, "Failed to import datum");
                }

            }


        }
        return duplicates;
    }

    public static String getSQLStatementFromJSONJata(JSONObject datum, String tableName) {
        StringBuilder statementBuilder = new StringBuilder("INSERT INTO \"" + tableName + "\" VALUES (");
        //for each data metric the scouting server is configured to care about add it into the database or handle duplicates
        if (Objects.equals(((JSONObject) datum.get("TEAM_NUM")).get("0").toString(), "1403")) {
            System.out.print("");
        }
        for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
            JSONObject potentialMetric = datum.optJSONObject(rawDataMetric.getName());//json object which represents the datatype and value for this metric
            if (potentialMetric == null) {
                //then this metric was not provided, but have no fear, defaults exist!
                potentialMetric = new JSONObject();
                potentialMetric.put(String.valueOf(rawDataMetric.getDatatype().ordinal()), rawDataMetric.getDefaultValue());
            }

            if (potentialMetric.keySet().size() != 1) {
                //not exptected
                continue;
            }
            String key = "";
            //this will only loop once
            for (String string : potentialMetric.keySet()) {
                key = string;
            }

            //check that the declared datatype is correct according to the config file
            if (rawDataMetric.getDatatype().ordinal() != Integer.parseInt(key)) {
                continue;
            }

            switch (rawDataMetric.getDatatype()) {

                case INTEGER, BOOLEAN -> {
                    statementBuilder.append(potentialMetric.get(key)).append(", ");
                }
                case STRING -> {
                    statementBuilder.append("\"").append(potentialMetric.get(key)).append("\" , ");
                }
            }
        }
        statementBuilder.replace(statementBuilder.toString().length() - 2, statementBuilder.length()  -1, "");
        statementBuilder.append(")");
        return statementBuilder.toString();
    }


    public static JSONArray readDatabaseNew(String tableName) throws SQLException, ConfigFileFormatException, ParserConfigurationException, IOException, SAXException {
        ArrayList<HashMap<String, Object>> rawList = SQLUtil.exec("SELECT * FROM \"" + tableName + "\"", true);
        JSONArray output = new JSONArray();
        Configuration.updateConfiguration();
        for (HashMap<String, Object> rawRow : rawList) {
            //export all the data we have regardless of whether or not the config file says we care about it
            //users can make mistakes and importing only imports what the config file wants to export everything in an effort to never lose data
            JSONObject rowObject = new JSONObject();
            for (String sqlColumnName : rawRow.keySet()) {
                //for each of columsn in the sql database
                JSONObject dataCarrier = new JSONObject();
                Optional<DataMetric> optionalMetric = Configuration.getMetric(sqlColumnName);
                //try and use this metric to ascertain the datatype, otherwise, just assume its a string
                if (optionalMetric.isPresent()) {
                    dataCarrier.put(String.valueOf(optionalMetric.get().getDatatype().ordinal()), rawRow.get(sqlColumnName));
                }else {
                    //assume string if all else fails
                    dataCarrier.put("1",  rawRow.get(sqlColumnName).toString());
                }
                rowObject.put(sqlColumnName, dataCarrier);
            }//end for each sql column
            output.put(rowObject);
        }//end for each row
        return output;
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
                rawData.get(Constants.SQLColumnName.NOTE_1.toString()).toString(),
                rawData.get(Constants.SQLColumnName.NOTE_2.toString()).toString(),
                rawData.get(Constants.SQLColumnName.NOTE_3.toString()).toString(),
                rawData.get(Constants.SQLColumnName.NOTE_4.toString()).toString(),
                rawData.get(Constants.SQLColumnName.NOTE_5.toString()).toString(),
                rawData.get(Constants.SQLColumnName.NOTE_6.toString()).toString(),
                rawData.get(Constants.SQLColumnName.NOTE_7.toString()).toString(),
                rawData.get(Constants.SQLColumnName.NOTE_8.toString()).toString(),
                rawData.get(Constants.SQLColumnName.NOTE_9.toString()).toString(),
                (int) rawData.get(Constants.SQLColumnName.A_STOP.toString()),
                (int)rawData.get(Constants.SQLColumnName.SHUTTLED.toString()),
                (int) rawData.get(Constants.SQLColumnName.TELE_SPEAKER.toString()),
                (int) rawData.get(Constants.SQLColumnName.TELE_AMP.toString()),
                (int) rawData.get(Constants.SQLColumnName.TELE_TRAP.toString()),
                (int) rawData.get(Constants.SQLColumnName.TELE_SPEAKER_MISSED.toString()),
                (int) rawData.get(Constants.SQLColumnName.TELE_AMP_MISSED.toString()),
                (int) rawData.getOrDefault(Constants.SQLColumnName.SPEAKER_RECEIVED.toString(), 0),
                (int) rawData.getOrDefault(Constants.SQLColumnName.AMP_RECEIVED.toString(), 0),
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
                           String note1,
                           String note2,
                           String note3,
                           String note4,
                           String note5,
                           String note6,
                           String note7,
                           String note8,
                           String note9,
                           int aStop,
                           int shuttled,
                           int teleSpeaker,
                           int teleAmp,
                           int teleTrap,
                           int teleSpeakerMissed,
                           int teleAmpMissed,
                           int speakerReceived,
                           int ampReceived,
                           int lostComms,
                           String teleNotes
    ) {

        public LinkedList<DataPoint<String>> getDataAsList(boolean includeAutoNotes)  {
            LinkedList<DataPoint<String>> output = new LinkedList<>();
            output.add(new DataPoint<>(Constants.SQLColumnName.MATCH_NUM.toString(), String.valueOf(matchNumber)));
            output.add(new DataPoint<>(Constants.SQLColumnName.TEAM_NUM.toString(), String.valueOf(teamNumber)));
            output.add(new DataPoint<>(Constants.SQLColumnName.ALLIANCE_POS.toString(), String.valueOf(position.ordinal())));
            output.add(new DataPoint<>(Constants.SQLColumnName.AUTO_SPEAKER.toString(), String.valueOf(autoSpeaker)));
            output.add(new DataPoint<>(Constants.SQLColumnName.AUTO_AMP.toString(), String.valueOf(autoAmp)));
            output.add(new DataPoint<>(Constants.SQLColumnName.AUTO_SPEAKER_MISSED.toString(), String.valueOf(autoSpeakerMissed)));
            output.add(new DataPoint<>(Constants.SQLColumnName.AUTO_AMP_MISSED.toString(), String.valueOf(autoAmpMissed)));

            if (includeAutoNotes) {
                output.add(new DataPoint<>(Constants.SQLColumnName.NOTE_1.toString(), note1));
                output.add(new DataPoint<>(Constants.SQLColumnName.NOTE_2.toString(), note2));
                output.add(new DataPoint<>(Constants.SQLColumnName.NOTE_3.toString(), note3));
                output.add(new DataPoint<>(Constants.SQLColumnName.NOTE_4.toString(), note4));
                output.add(new DataPoint<>(Constants.SQLColumnName.NOTE_5.toString(), note5));
                output.add(new DataPoint<>(Constants.SQLColumnName.NOTE_6.toString(), note6));
                output.add(new DataPoint<>(Constants.SQLColumnName.NOTE_7.toString(), note7));
                output.add(new DataPoint<>(Constants.SQLColumnName.NOTE_8.toString(), note8));
                output.add(new DataPoint<>(Constants.SQLColumnName.NOTE_9.toString(), note9));
            }

            output.add(new DataPoint<>(Constants.SQLColumnName.A_STOP.toString(), String.valueOf(aStop)));
            output.add(new DataPoint<>(Constants.SQLColumnName.SHUTTLED.toString(), String.valueOf(shuttled)));
            output.add(new DataPoint<>(Constants.SQLColumnName.TELE_SPEAKER.toString(), String.valueOf(teleSpeaker)));
            output.add(new DataPoint<>(Constants.SQLColumnName.TELE_AMP.toString(), String.valueOf(teleAmp)));
            output.add(new DataPoint<>(Constants.SQLColumnName.TELE_TRAP.toString(), String.valueOf(teleTrap)));
            output.add(new DataPoint<>(Constants.SQLColumnName.TELE_SPEAKER_MISSED.toString(), String.valueOf(teleSpeakerMissed)));
            output.add(new DataPoint<>(Constants.SQLColumnName.TELE_AMP_MISSED.toString(), String.valueOf(teleAmpMissed)));
            output.add(new DataPoint<>(Constants.SQLColumnName.SPEAKER_RECEIVED.toString(), String.valueOf(speakerReceived)));
            output.add(new DataPoint<>(Constants.SQLColumnName.AMP_RECEIVED.toString(), String.valueOf(ampReceived)));
            output.add(new DataPoint<>(Constants.SQLColumnName.LOST_COMMS.toString(), String.valueOf(lostComms)));
            output.add(new DataPoint<>(Constants.SQLColumnName.TELE_COMMENTS.toString(), "\""  + teleNotes + "\""));


            return output;
        }

        //for exporting
        public LinkedList<DataPoint<String>> getDataAsList() {
           return getDataAsList(true);
        }

        public String getDataForSQL() {
            return matchNumber + ", " +
                    teamNumber + ", " +
                    position.ordinal() + ", " +
                    autoSpeaker + ", " +
                    autoAmp + ", " +
                    autoSpeakerMissed + ", " +
                    autoAmpMissed + ", " +
                    "\"" + note1 + "\", " +
                    "\"" + note2 + "\", " +
                    "\"" + note3 + "\", " +
                    "\"" + note4 + "\", " +
                    "\"" + note5 + "\", " +
                    "\"" + note6 + "\", " +
                    "\"" + note7 + "\", " +
                    "\"" + note8 + "\", " +
                    "\"" + note9 + "\", " +
                    aStop + ", " +
                    shuttled + ", " +
                    teleSpeaker + ", " +
                    teleAmp + ", " +
                    teleTrap + ", " +
                    teleSpeakerMissed + ", " +
                    teleAmpMissed + ", " +
                    speakerReceived + ", " +
                    ampReceived + ", " +
                    lostComms + ", " +
                    "\"" + teleNotes + "\"";
        }

        public String getQRString() {
            StringBuilder qrBuilder = new StringBuilder();
            for (DataPoint<String> dataPoint : this.getDataAsList(false)) {
                qrBuilder.append(dataPoint.getValue().replaceAll("\"", "")).append(Constants.QR_DATA_DELIMITER);
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

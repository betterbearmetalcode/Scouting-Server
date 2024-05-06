package org.tahomarobotics.scouting.scoutingserver;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DuplicateDataResolverDialog;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.data.DuplicateData;
import org.tahomarobotics.scouting.scoutingserver.util.data.DuplicateResolution;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static org.tahomarobotics.scouting.scoutingserver.util.UI.DatabaseViewerTabContent.getIntFromEntryJSONObject;


public class DatabaseManager {



    public static void importJSONFile(File selectedFile, String activeTable) throws IOException {
        if (selectedFile == null) {
            return;
        }
        if (!selectedFile.exists()) {
            return;
        }
        FileInputStream inputStream = new FileInputStream(selectedFile);
        JSONArray object = new JSONArray(new String(inputStream.readAllBytes()));
        DatabaseManager.importJSONArrayOfDataObjects(object, activeTable);
        inputStream.close();
    }

    //the king of storage methods, all the data storage methods eventually call this one, it is the final leg of the chain

    //the way this will work is get the max match number and iterate through each match.
    //then it will go through each match and make sure everything is unique, if there are any entries with duplicate match and or team numbers then it will ask the user to
    //resolve the issue.
    //then the data will be added to the target database, two matches at a time and all duplicates will be rememberd
    //after all that data which can be added is added, any remaining duplicate data will be resolved by the user and added.

    public static void importJSONArrayOfDataObjects(JSONArray data, String activeTable){

        try {
            Configuration.updateConfiguration();
        } catch (ConfigFileFormatException e) {
           if (!Constants.askQuestion("Unable to update configuration, proceed with current configuration?")) {
                return;
           }
        }
        int maxMatchNumber = 1;
        for (int i = 0; i < data.length(); i++) {
            maxMatchNumber = Math.max(getIntFromEntryJSONObject(Constants.SQLColumnName.MATCH_NUM, data.getJSONObject(i)), maxMatchNumber);
        }

        JSONArray oldData = null;
        //find all duplicate data in the dataset we are importing and have the user resolve it
        try {
            //add all data from the table into dataset
            oldData = readDatabase(activeTable, false);
            data.putAll(oldData);
            ArrayList<DuplicateData> duplicates = findDuplicateDataInThisJSONArray(data, maxMatchNumber);
            if (!duplicates.isEmpty()) {
                DuplicateDataResolverDialog dialog = new DuplicateDataResolverDialog(findDuplicateDataInThisJSONArray(data, maxMatchNumber));
                Optional<ArrayList<DuplicateResolution>> optionalResolutions = dialog.showAndWait();
                //go through the old data and remove the duplicates and replace them with the ones the user decided they should be
                final JSONArray dataWithoutDuplicates = new JSONArray();
                if (optionalResolutions.isPresent()) {
                    ArrayList<DuplicateResolution> resolutions = optionalResolutions.get();
                    data.forEach(o -> {
                        JSONObject datumObject = (JSONObject) o;
                        int teamNum = getIntFromEntryJSONObject(Constants.SQLColumnName.TEAM_NUM, datumObject);
                        int matchNum = getIntFromEntryJSONObject(Constants.SQLColumnName.MATCH_NUM, datumObject);
                        boolean shouldAdd = true;
                        for (DuplicateResolution resolution : resolutions) {
                            if ((resolution.teamNum() == teamNum) && (resolution.matchNum() == matchNum)) {
                                shouldAdd = false;
                            }
                        }
                        if (shouldAdd) {
                            dataWithoutDuplicates.put(o);
                        }
                    });
                    for (DuplicateResolution resolution : resolutions) {
                        dataWithoutDuplicates.put(resolution.dataToUse());
                    }
                }else {
                    dataWithoutDuplicates.putAll(data);
                }
                data = dataWithoutDuplicates;
            }

        } catch (ConfigFileFormatException | SQLException e) {
            Logging.logError(e);
        }
        //we have now verified that every entry in the data array has a unique match and team number pair
        //so just add it into the sql table
        //first clear the contents of the table because we have already got everything in ram

        try {
            SQLUtil.execNoReturn("DELETE FROM \"" + activeTable + "\"");
            //insert data in batches to increase preformance

            int remainder = data.length() % Constants.DATA_IMPORTATION_BATCH_SIZE;
            StringBuilder remainderBuilder = new StringBuilder("INSERT INTO \"" + activeTable + "\" VALUES");
            int index;
            for (index = 0; index < remainder; index++) {
                remainderBuilder.append(getValuesStatementFromJSONJata(data.getJSONObject(index))).append(", ");
            }
            remainderBuilder.replace(remainderBuilder.toString().length() - 2, remainderBuilder.length()  -1, "");
            //insert some of the data so that the rest of the data is divisible by the batch size
            SQLUtil.execNoReturn(remainderBuilder.toString(), SQLUtil.EMPTY_PARAMS, false);
            for (; index < data.length();) {

                //for each batch
                StringBuilder batchBuilder = new StringBuilder("INSERT INTO \"" + activeTable + "\" VALUES ");
                int endOfThisBatch = index + Constants.DATA_IMPORTATION_BATCH_SIZE;
                for (; index < endOfThisBatch ; index++) {
                    batchBuilder.append(getValuesStatementFromJSONJata(data.getJSONObject(index))).append(", ");
                }
                batchBuilder.replace(batchBuilder.toString().length() - 2, batchBuilder.length()  -1, "");
                SQLUtil.execNoReturn(batchBuilder.toString(), SQLUtil.EMPTY_PARAMS, false);
            }


        } catch (NumberFormatException |SQLException | DuplicateDataException e) {
            Logging.logError(e);
        }


    }

    private static ArrayList<DuplicateData> findDuplicateDataInThisJSONArray(JSONArray data, int maxMatchNumber) {
        ArrayList<DuplicateData> duplicateData = new ArrayList<>();
        for (int matchum = 1; matchum <= maxMatchNumber; matchum++) {
            ArrayList<JSONObject> dataForThisMatch = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                if ( getIntFromEntryJSONObject(Constants.SQLColumnName.MATCH_NUM, data.getJSONObject(i)) == matchum) {
                        dataForThisMatch.add(data.getJSONObject(i));
                }
            }
            //we have all the data for one match
            //all team numbers must be unique
            //I don't care if there are more or less than 6 entries, becuase I don't want problems with data importation and having multiple entries for the same position of different tablets
            //it is the job of the database manager to resolve these issues or the scouting app devs/scouts to not create them in the first place :)
            ArrayList<Integer> teamsSeen = new ArrayList<>();
            ArrayList<Integer> duplicatedTeams = new ArrayList<>();
            for (JSONObject forThisMatch : dataForThisMatch) {
                int teamNum = getIntFromEntryJSONObject(Constants.SQLColumnName.TEAM_NUM, forThisMatch);
                if (!teamsSeen.contains(teamNum)) {
                    teamsSeen.add(teamNum);
                }else {
                    if (!duplicatedTeams.contains(teamNum)) {
                        duplicatedTeams.add(teamNum);
                    }

                }
            }
            for (Integer duplicatedTeam : duplicatedTeams) {
                //add all the duplicated teams' data
                ArrayList<JSONObject> dataAsJSONObjects = new ArrayList<>();
                dataForThisMatch.forEach(object -> {
                    if (getIntFromEntryJSONObject(Constants.SQLColumnName.TEAM_NUM, object) == duplicatedTeam) {
                        dataAsJSONObjects.add(object);
                    }
                });
                duplicateData.add(new DuplicateData(dataAsJSONObjects, matchum, duplicatedTeam));
            }
        }
        return duplicateData;
    }


    //returns (<data> INTEGER, <data> INTEGER, <data> TEXT, ...)
    public static String getValuesStatementFromJSONJata(JSONObject datum) {
        StringBuilder statementbuilder = new StringBuilder();
        statementbuilder.append("(");

        //for each data metric the scouting server is configured to care about add it into the database or handle duplicates
        for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
            JSONObject potentialMetric = datum.optJSONObject(rawDataMetric.getName());//json object which represents the datatype and value for this metric
            if (potentialMetric == null) {
                //then this metric was not provided, but have no fear, defaults exist!
                potentialMetric = new JSONObject();
                potentialMetric.put(rawDataMetric.getDatatypeAsString(), rawDataMetric.getDefaultValue());
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
                    statementbuilder.append(potentialMetric.get(key)).append(", ");
                }
                case STRING -> {
                    statementbuilder.append("\"").append(potentialMetric.get(key)).append("\" , ");
                }
            }
        }
        statementbuilder.replace(statementbuilder.toString().length() - 2, statementbuilder.length()  -1, "");
        statementbuilder.append(")");
        return statementbuilder.toString();
    }

    public static JSONArray readDatabase(String tableName, boolean sorted) throws ConfigFileFormatException, SQLException {
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
                    dataCarrier.put(optionalMetric.get().getDatatypeAsString(), rawRow.get(sqlColumnName));
                }else {
                    //assume string if all else fails
                    dataCarrier.put("1",  rawRow.get(sqlColumnName).toString());
                }
                rowObject.put(sqlColumnName, dataCarrier);
            }//end for each sql column
            output.put(rowObject);
        }//end for each row

        if (sorted) {
            List<Object> sortedOutput = output.toList();
            sortedOutput.sort((o1, o2) -> {
                HashMap<String, Object> rowObject1 = (HashMap<String, Object>) o1;
                HashMap<String, Object> rowObject2 = (HashMap<String, Object>) o2;
                int match1 = Integer.parseInt(((HashMap<String, Object>)rowObject1.get(Constants.SQLColumnName.MATCH_NUM.toString())).get(String.valueOf(Configuration.Datatype.INTEGER.ordinal())).toString());
                int match2 = Integer.parseInt(((HashMap<String, Object>)rowObject2.get(Constants.SQLColumnName.MATCH_NUM.toString())).get(String.valueOf(Configuration.Datatype.INTEGER.ordinal())).toString());
                if (match1 != match2) {
                    return Integer.compare(match1, match2);
                }else {
                    int robotPosition1 = Integer.parseInt(((HashMap<String, Object>)rowObject1.get(Constants.SQLColumnName.ALLIANCE_POS.toString())).get(String.valueOf(Configuration.Datatype.INTEGER.ordinal())).toString());
                    int robotPosition2 = Integer.parseInt(((HashMap<String, Object>)rowObject2.get(Constants.SQLColumnName.ALLIANCE_POS.toString())).get(String.valueOf(Configuration.Datatype.INTEGER.ordinal())).toString());
                    return Integer.compare(robotPosition1, robotPosition2);
                }
            });
            return new JSONArray().putAll(sortedOutput);
        }
        return output;
    }

    public static JSONArray readDatabase(String tableName) throws SQLException, ConfigFileFormatException {
        return readDatabase(tableName, false);
    }


    public static JSONObject getJSONDatum(HashMap<String, Object> rawData) {
        //given a SQl row, construct the JSON Datum
        JSONObject output = new JSONObject();
        for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
            if (rawData.containsKey(rawDataMetric.getName())) {
                //if the data we want is actually in the SQL database
                JSONObject dataCarrier = new JSONObject();
                dataCarrier.put(rawDataMetric.getDatatypeAsString(), rawData.get(rawDataMetric.getName()).toString());
                output.put(rawDataMetric.getName(), dataCarrier);
            }


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



    public static double getAverage(Constants.SQLColumnName column, String teamName, String table, boolean log) throws SQLException {
        ArrayList<HashMap<String, Object>> raw = SQLUtil.exec("SELECT " + column + " FROM \"" + table + "\" WHERE " + Constants.SQLColumnName.TEAM_NUM + "=?", new Object[]{teamName}, log);
        ArrayList<Integer> data = new ArrayList<>();
        raw.forEach(map -> {data.add(Integer.valueOf(map.get(column.toString()).toString()));});
        return (double) data.stream().mapToInt(Integer::intValue).sum() /data.size();
    }
}

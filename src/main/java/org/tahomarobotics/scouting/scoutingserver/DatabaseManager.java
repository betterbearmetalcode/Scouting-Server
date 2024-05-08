package org.tahomarobotics.scouting.scoutingserver;

import javafx.stage.FileChooser;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.controller.MainTabPane;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DuplicateDataResolverDialog;
import org.tahomarobotics.scouting.scoutingserver.util.UI.GenericTabContent;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.data.DuplicateEntries;
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


    /**
     * Imports data from a file see {@link DatabaseManager#importJSONArrayOfDataObjects(JSONArray, String)}
     * @see DatabaseManager#importJSONArrayOfDataObjects(JSONArray, String)
     * @param selectedFile The file data is being imported from
     * @param activeTable The table data is being imported to
     * @throws IOException if io stuff goes wrong
     */
    public static void importJSONFile(File selectedFile, String activeTable) throws IOException {
        if (selectedFile == null) {
            return;
        }
        if (!selectedFile.exists()) {
            return;
        }
        try {
            FileInputStream inputStream = new FileInputStream(selectedFile);
            JSONArray object = new JSONArray(new String(inputStream.readAllBytes()));
            DatabaseManager.importJSONArrayOfDataObjects(object, activeTable);
            inputStream.close();
        }catch (JSONException e) {
            Logging.logError(e);
        }
    }

    //



    /**
     * The king of storage methods, all the data storage methods eventually call this one, it is the final leg of the chain
     * the way this will work is get the max match number and iterate through each match.
     * then it will go through each match and make sure everything is unique, if there are any entries with duplicate match and or team numbers then it will ask the user to
     * resolve the issue.
     * then the data will be added to the target database, two matches at a time and all duplicates will be rememberd
     * after all that data which can be added is added, any remaining duplicate data will be resolved by the user and added.
     * @param data The data to be imported
     * @param activeTable the table to import it into
     */
    public static void importJSONArrayOfDataObjects(JSONArray data, String activeTable){
        if (data.isEmpty()) {
            Logging.logInfo("No data to input, returning");
            return;
        }
        int maxMatchNumber = 1;
        for (int i = 0; i < data.length(); i++) {
            maxMatchNumber = Math.max(getIntFromEntryJSONObject(Constants.SQLColumnName.MATCH_NUM, data.getJSONObject(i)), maxMatchNumber);
        }

        JSONArray oldData;
        //find all duplicate data in the dataset we are importing and have the user resolve it
        try {
            //add all data from the table into dataset
            oldData = readDatabase(activeTable, false);
            data.putAll(oldData);
            ArrayList<DuplicateEntries> duplicates = findDuplicateDataInThisJSONArray(data, maxMatchNumber);
            if (!duplicates.isEmpty()) {
                DuplicateDataResolverDialog dialog = new DuplicateDataResolverDialog(duplicates);
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
                                break;
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

        } catch (SQLException e) {
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
            while (index < data.length()) {

                //for each batch
                StringBuilder batchBuilder = new StringBuilder("INSERT INTO \"" + activeTable + "\" VALUES ");
                int endOfThisBatch = index + Constants.DATA_IMPORTATION_BATCH_SIZE;
                for (; index < endOfThisBatch ; index++) {
                    batchBuilder.append(getValuesStatementFromJSONJata(data.getJSONObject(index))).append(", ");
                }
                batchBuilder.replace(batchBuilder.toString().length() - 2, batchBuilder.length()  -1, "");
                SQLUtil.execNoReturn(batchBuilder.toString(), SQLUtil.EMPTY_PARAMS, false);
            }
            //data imported succesfully

        } catch (NumberFormatException |SQLException e) {
            Logging.logError(e);
        }

    }

    /**
     * used to locate dupliate data and prepare it for resolution
     * @param data the data
     * @param maxMatchNumber the last match
     * @return duplicates that need to be resolved
     */
    private static ArrayList<DuplicateEntries> findDuplicateDataInThisJSONArray(JSONArray data, int maxMatchNumber) {
        ArrayList<DuplicateEntries> duplicateData = new ArrayList<>();
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
                duplicateData.add(new DuplicateEntries(dataAsJSONObjects, matchum, duplicatedTeam));
            }
        }
        return duplicateData;
    }


    public void loadCSV() {
        FileChooser chooser = new FileChooser();
        File selectedFile = chooser.showOpenDialog(ScoutingServer.mainStage.getOwner());
        try {
            if (selectedFile == null) {
                return;
            }
            FileInputStream inputStream = new FileInputStream(selectedFile);
            String csv = new String(inputStream.readAllBytes());
            csv = csv.replaceAll("\r", "");
            JSONArray result = CDL.toJSONArray(csv);
            ArrayList<DuplicateDataException> duplicates = new ArrayList<>();
            for (Object o : result) {
                JSONObject rawData = (JSONObject) o;
                //check if the data is valid
                try {
                    //if any of these fail, then skip the data
                    int x = rawData.getInt(Constants.SQLColumnName.MATCH_NUM.toString());
                    int y = rawData.getInt(Constants.SQLColumnName.TEAM_NUM.toString());
                    DatabaseManager.RobotPosition.valueOf(rawData.getString(Constants.SQLColumnName.ALLIANCE_POS.toString()));
                }catch (Exception e) {
                    continue;
                }
                //convert to standard JSON format
                JSONObject dataToImport = new JSONObject();
                for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
                    //if it can't parse anything the set a default that no one will ever enter so that we can detect if we need to use the default for string datatypes
                    //and seriously, if the following was really what the scout said, then we might as well use the default...
                    String weirdDefault = "deeeeeeeeeeeeeeeeedddffeeeeeeeeefault!!";
                    String csvDatum = rawData.optString(rawDataMetric.getName(), weirdDefault);//if the csv data has something for this, metric, then good, otherwise nothing, sort out default in a sec
                    //in the csv templates the alliance positions are put in as letters to be human readable
                    if (Objects.equals(rawDataMetric.getName(), Constants.SQLColumnName.ALLIANCE_POS.name())) {
                        csvDatum = String.valueOf(DatabaseManager.RobotPosition.valueOf(csvDatum).ordinal());
                    }
                    JSONObject finalDataCarryingaObject = new JSONObject();
                    //then we have something
                    switch (rawDataMetric.getDatatype()) {

                        case INTEGER -> {
                            try {
                                finalDataCarryingaObject.put("0", Integer.parseInt(csvDatum));
                            }catch (NumberFormatException e) {
                                finalDataCarryingaObject.put("0", rawDataMetric.getDefaultValue());
                            }

                        }
                        case STRING -> {
                            if (!Objects.equals(csvDatum, weirdDefault)) {
                                finalDataCarryingaObject.put("1", csvDatum);
                            }else {
                                finalDataCarryingaObject.put("1", rawDataMetric.getDefaultValue());
                            }

                        }
                        case BOOLEAN -> {
                            try {
                                int i = Integer.parseInt(csvDatum);
                                if ((i != 0) && (i != 1)) {
                                    //then we have a non boolean value so go the catch block
                                    throw new NumberFormatException();
                                }
                                finalDataCarryingaObject.put("2", i);
                            }catch (NumberFormatException e) {
                                //use default
                                finalDataCarryingaObject.put("2", rawDataMetric.getDefaultValue());
                            }

                        }
                    }
                    dataToImport.put(rawDataMetric.getName(), finalDataCarryingaObject);
                }
                try {
                    SQLUtil.execNoReturn(DatabaseManager.getValuesStatementFromJSONJata(dataToImport));
                }catch (SQLException e) {
                    Logging.logError(e, "SQL Exception: ");
                }
            }

            inputStream.close();
        } catch (IOException  e) {
            Logging.logError(e);
        }
    }


    /**
     *  returns a string to be used in a SQL statement for inserting this data into a database in this format
     *  (<data> INTEGER, <data> INTEGER, <data> TEXT, ...)
     * @param datum the data entry we are looking at
     * @return SQL string
     */
    private static String getValuesStatementFromJSONJata(JSONObject datum) {
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

                case INTEGER, BOOLEAN -> statementbuilder.append(potentialMetric.get(key)).append(", ");
                case STRING -> statementbuilder.append("\"").append(potentialMetric.get(key)).append("\" , ");
            }
        }
        statementbuilder.replace(statementbuilder.toString().length() - 2, statementbuilder.length()  -1, "");
        statementbuilder.append(")");
        return statementbuilder.toString();
    }


    //optimized i think

    /**
     *Reads a SQL table returns the data in a json array for use
     *
     * @param tableName the sql table we are trying to read from
     * @param sorted whether the data which is returned should be sorted or not
     * @return a json array following the BMSS data transfer protocol
     * @throws SQLException if sql goes wrong
     */
    public static JSONArray readDatabase(String tableName, boolean sorted) throws SQLException {
        ArrayList<HashMap<String, Object>> rawList = SQLUtil.exec("SELECT * FROM \"" + tableName + "\"", true);
        JSONArray output = new JSONArray();
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

    /**
     * Reads given database without sorting
     * @see DatabaseManager#readDatabase(String, boolean)
     *
     */
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

    /**
     * Enum for different positions robots can start at
     */
    public enum RobotPosition {
        R1,
        R2,
        R3,
        B1,
        B2,
        B3,
    }

    //needs to be re-written in charts implementation
    public static double getAverage(Constants.SQLColumnName column, String teamName, String table, boolean log) throws SQLException {
        ArrayList<HashMap<String, Object>> raw = SQLUtil.exec("SELECT " + column + " FROM \"" + table + "\" WHERE " + Constants.SQLColumnName.TEAM_NUM + "=?", new Object[]{teamName}, log);
        ArrayList<Integer> data = new ArrayList<>();
        raw.forEach(map -> data.add(Integer.valueOf(map.get(column.toString()).toString())));
        return (double) data.stream().mapToInt(Integer::intValue).sum() /data.size();
    }
}

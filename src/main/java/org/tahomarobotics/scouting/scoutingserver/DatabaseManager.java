package org.tahomarobotics.scouting.scoutingserver;

import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.controller.MasterController;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DatabaseViewerTabContent;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DuplicateDataResolverDialog;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DuplicateData;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import javax.swing.text.html.ObjectView;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    public static void handleDuplicates(ArrayList<DuplicateDataException> duplicates) {
        if (duplicates.isEmpty()) {
            return;
        }
        /*Platform.runLater(() -> {
            //this method has to use a duplicate data handler to go through all the duplicates and generate a list of entryies which should be added
            //then for each of these emtries, all the ones in the database that have the same match and team number are deleted and re added
            DuplicateDataResolverDialog dialog = new DuplicateDataResolverDialog(duplicates);
            Optional<ArrayList<JSONObject>> dataToAdd = dialog.showAndWait();
            String tableName = duplicates.get(0).getTableName();
            dataToAdd.ifPresent(jsonObjects -> {
                for (JSONObject jsonObject: jsonObjects) {
                    //first delete the old record from the database that caused the duplicate then add the one we want to add
                    try {
                        SQLUtil.execNoReturn("DELETE FROM \"" + tableName + "\" WHERE " +
                                Constants.SQLColumnName.TEAM_NUM + "=? AND " +
                                Constants.SQLColumnName.MATCH_NUM + "=?",
                                new Object[]{getIntFromEntryJSONObject(Constants.SQLColumnName.TEAM_NUM, jsonObject),
                                        getIntFromEntryJSONObject(Constants.SQLColumnName.MATCH_NUM, jsonObject)}, true);
                    } catch (SQLException e) {
                        Logging.logError(e);
                    }
                }

                DatabaseManager.importJSONArrayOfDataObjects(new JSONArray(jsonObjects), tableName);
            });
        });*/


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
        List<Object> dataAsList = data.toList();
        int maxMatchNumber = 1;
        for (Object o : dataAsList) {
            maxMatchNumber = Math.max(getIntFromEntryJSONObject(Constants.SQLColumnName.MATCH_NUM, (JSONObject) o), maxMatchNumber);
        }
        ArrayList<DuplicateData> duplicateData = new ArrayList<>();
        //ensure each match has unique entries
        for (int matchum = 1; matchum <= maxMatchNumber; matchum++) {
            int finalMatchum = matchum;
            ArrayList<Object> dataForThisMatch = dataAsList.stream().filter(o -> getIntFromEntryJSONObject(Constants.SQLColumnName.MATCH_NUM, (JSONObject) o) == finalMatchum).collect(Collectors.toCollection(ArrayList::new));
            //we have all the data for one match
            //all team numbers must be unique
            //I don't care if there are more or less than 6 entries, becuase I don't want problems with data importation and having multiple entries for the same position of different tablets
            //it is the job of the database manager to resolve these issues or the scouting app devs/scouts to not create them in the first place :)
            ArrayList<Integer> teamsSeen = new ArrayList<>();
            for (Object forThisMatch : dataForThisMatch) {
                int teamNum = getIntFromEntryJSONObject(Constants.SQLColumnName.TEAM_NUM, (JSONObject) forThisMatch);
                if (!teamsSeen.contains(teamNum)) {
                    teamsSeen.add(teamNum);
                }else {
                    //then we have seen this team twice
                    ArrayList<JSONObject> dataAsJSONObjects = new ArrayList<>();
                    dataForThisMatch.forEach(o -> dataAsJSONObjects.add((JSONObject) o));
                    duplicateData.add(new DuplicateData(dataAsJSONObjects));
                }
            }
        }
        DuplicateDataResolverDialog dialog = new DuplicateDataResolverDialog(duplicateData);
        dialog.showAndWait();
        StringBuilder statementBuilder = new StringBuilder("INSERT INTO \"" + activeTable + "\" VALUES ");
        for (Object object : data) {
            statementBuilder.append(getValuesStatementFromJSONJata((JSONObject) object, activeTable)).append(", ");
        }
        statementBuilder.replace(statementBuilder.toString().length() - 2, statementBuilder.length()  -1, "");
        try {

            SQLUtil.execNoReturn(statementBuilder.toString(), SQLUtil.EMPTY_PARAMS, false, data);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | IllegalStateException |
                 SQLException e) {
            Logging.logError(e, "Failed to import datum");

        }
    }

    public static String getValuesStatementFromJSONJata(JSONObject datum, String tableName) {
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

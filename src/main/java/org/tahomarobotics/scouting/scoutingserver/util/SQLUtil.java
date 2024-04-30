package org.tahomarobotics.scouting.scoutingserver.util;

import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
public class SQLUtil {

    private static Connection connection;
    public static final Object[] EMPTY_PARAMS = {};


    public static void addTableIfNotExists(String tableName) throws SQLException, IllegalArgumentException, DuplicateDataException, ConfigFileFormatException {
        //first update configuration to ensure config file is read and then read the config file and use its columns to generate a SQL table that matches them
        Configuration.updateConfiguration();
        StringBuilder schema = new StringBuilder("(");
        for (DataMetric rawDataMetric : Configuration.getRawDataMetrics()) {
            schema.append(rawDataMetric.getName()).append(" ");
            switch (rawDataMetric.getDatatype()) {

                case INTEGER, BOOLEAN -> {
                    schema.append("INTEGER,");
                }
                case STRING -> {
                    schema.append("TEXT, ");
                }
            }
        }
        schema.replace(schema.length() - 2, schema.length(), "");
        schema.append(", PRIMARY KEY (").append(Constants.SQLColumnName.MATCH_NUM).append(", ").append(Constants.SQLColumnName.TEAM_NUM).append("))");
        String statement = "CREATE TABLE IF NOT EXISTS \"" + tableName + "\"" + schema;
        
        execNoReturn(statement);
    }


    private static void setParam(PreparedStatement statement, Integer index, Object param) throws SQLException {
        if (param instanceof String) {
            statement.setString(index, param.toString());
        } else if (param instanceof Integer) {
            statement.setInt(index, (int) param);
        } else if (param instanceof Boolean) {
            statement.setBoolean(index, (Boolean) param);
        }
    }

    public static void execNoReturn(String statement) throws SQLException, IllegalArgumentException, DuplicateDataException {
        execNoReturn(statement, SQLUtil.EMPTY_PARAMS, true);
    }

    public static void execNoReturn(String statement, boolean log) throws SQLException, IllegalArgumentException, DuplicateDataException {
        execNoReturn(statement, SQLUtil.EMPTY_PARAMS, log);
    }

    public static void execNoReturn(String statement, Object[] params, boolean log) throws SQLException, IllegalArgumentException, DuplicateDataException {
        execNoReturn(statement, params, log, null);
    }

    public static void execNoReturn(String statement, Object[] params, boolean log, DatabaseManager.QRRecord record) throws SQLException, IllegalArgumentException, DuplicateDataException {
        try {
            PreparedStatement toExec = connection.prepareStatement(statement);
            Integer count = 1;
            for (Object param : params) {
                setParam(toExec, count, param);
                count++;
            }
            toExec.executeUpdate();
            connection.commit();
            toExec.close();
            if (log)  {
                Logging.logInfo("Executed sql Query: " + statement);
            }

        } catch (SQLException e){
            if (e.getMessage().startsWith("[SQLITE_CONSTRAINT_PRIMARYKEY]")) {
                handleDuplicateData(statement, record);

            }else {
                connection.rollback();
                throw new SQLException("\"Executed sql Query:" + statement + "\\nRolling Back Transaction\"", e);

            }


        }
    }

    public static ArrayList<HashMap<String, Object>> exec(String statement, boolean log) throws SQLException, IllegalArgumentException {
        return exec(statement, SQLUtil.EMPTY_PARAMS, log);
    }

    public static ArrayList<HashMap<String, Object>> exec(String statement, Object[] params, boolean log) throws SQLException, IllegalArgumentException {

        try {
            PreparedStatement toExec = connection.prepareStatement(statement);
            Integer count = 1;
            for (Object param : params) {
                setParam(toExec, count, param);
                count++;
            }
            ResultSet results = toExec.executeQuery();
            connection.commit();
            ArrayList<HashMap<String, Object>> toReturn = processResultSet(results);
            results.close();
            toExec.close();
            if (log) {
                Logging.logInfo("Executed sql Query: " + statement);
            }

            return toReturn;
        } catch (SQLException e){
            if (log) {
                Logging.logError(e, "Executed sql Query: " + statement + "\nRolling Back Transaction");
            }

            connection.rollback();
        }
        return new ArrayList<>();
    }

    public static ArrayList<HashMap<String, Object>> processResultSet(ResultSet data) throws SQLException {
        ResultSetMetaData md = data.getMetaData();
        int columns = md.getColumnCount();
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        while (data.next()) {
            HashMap<String, Object> row = new HashMap<>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i), data.getObject(i));
            }
            list.add(row);
        }
        return list;
    }

    public static void initialize(String db) throws SQLException {
        Logging.logInfo("Opening connection to database: " + db);
        connection = DriverManager.getConnection("jdbc:sqlite:" + db);
        connection.setAutoCommit(false);
    }

    public enum SQLDatatype {
        INTEGER,
        TEXT

    }



    public static ArrayList<String> getTableNames() throws SQLException {
        ArrayList<String> output = new ArrayList<>();
        DatabaseMetaData md = connection.getMetaData();
        ResultSet rs = md.getTables(null, null, "%", null);
        while (rs.next()) {
            if (!rs.getString(3).startsWith("sqlite_")) {
                output.add(rs.getString(3));
            }

        }
        return output;
    }


    //deals with primary key constraint violations
    private static void handleDuplicateData(String statement, DatabaseManager.QRRecord newRecord) throws DuplicateDataException {
        String[] tokens = statement.split("\\(")[1].replaceAll(" ", "").split(",");
        String matchNum = tokens[0];
        String teamNum = tokens[1];
        String tableName = statement.split("INTO ")[1].split("\"")[1].replaceAll("\"", "");
        //first check if the new and old data is the same, if it is, then do nothing, if its different, then throw noteA duolicate data exception with both the new and old records
        try {
            ArrayList<HashMap<String, Object>> oldMap = SQLUtil.exec("SELECT * FROM \"" + tableName + "\" WHERE " + Constants.SQLColumnName.TEAM_NUM + "=? AND " + Constants.SQLColumnName.MATCH_NUM + "=?", new Object[]{teamNum, matchNum}, true );
            if (newRecord != null) {
                //record should not be null if called by method storing data
                DatabaseManager.QRRecord oldRecord = DatabaseManager.getRecord(oldMap.get(0));
                if (!newRecord.equals(oldRecord)) {
                    //if they are not the same then we need to throw a duplicate data exception
                    throw new DuplicateDataException("Duplicate Data Detected", new SQLException(statement), oldRecord, newRecord);
                }
            }
        } catch (SQLException e) {
            Logging.logError(e);
            //at this point whatever, just skip it
        }
    }

}

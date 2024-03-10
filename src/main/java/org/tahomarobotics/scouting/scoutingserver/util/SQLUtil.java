package org.tahomarobotics.scouting.scoutingserver.util;

import org.tahomarobotics.scouting.scoutingserver.Constants;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
public class SQLUtil {

    private static Connection connection;
    public static final Object[] EMPTY_PARAMS = {};


    public static void addTableIfNotExists(String tableName, String schema) throws SQLException, IllegalArgumentException, DuplicateDataException {
        String statement = "CREATE TABLE IF NOT EXISTS \"" + tableName + "\"(" + schema + ", PRIMARY KEY (" + Constants.SQLColumnName.MATCH_NUM + ", " + Constants.SQLColumnName.TEAM_NUM  + "))";
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
                throw new DuplicateDataException("Duplicate Data Detected, Skipping", e);

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

    public static String createTableSchem(ArrayList<Constants.ColumnType> columns) {//the input is a hashmap of all the colums for a table, the key is the column name and the value is the database name

        StringBuilder builder = new StringBuilder();
        for (Constants.ColumnType type : columns) {
            builder.append(type.name().toString());
            builder.append(" ").append(type.datatype().toString()).append(", ");
        }
        String str = builder.toString();
        return str.substring(0, str.length() - 2);
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

}

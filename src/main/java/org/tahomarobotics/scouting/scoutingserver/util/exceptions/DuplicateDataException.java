package org.tahomarobotics.scouting.scoutingserver.util.exceptions;

import org.json.JSONObject;

public class DuplicateDataException extends Exception {

    private final JSONObject oldData;
    private final JSONObject newData;
    private final String tableName;
    public DuplicateDataException(String message, Throwable err, JSONObject oldData, JSONObject newData, String tblName) {
        super(message, err);
        this.oldData = oldData;
        this.newData = newData;
        this.tableName = tblName;
    }


    public JSONObject getOldData() {
        return oldData;
    }

    public JSONObject getNewData() {
        return newData;
    }

    public String getTableName() {
        return tableName;
    }
}

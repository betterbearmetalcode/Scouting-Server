package org.tahomarobotics.scouting.scoutingserver.util.exceptions;

import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;

import java.sql.SQLException;

public class DuplicateDataException extends Exception {

    private DatabaseManager.QRRecord oldData;
    private DatabaseManager.QRRecord newData;
    public DuplicateDataException(String message, Throwable err, DatabaseManager.QRRecord oldRecord, DatabaseManager.QRRecord newRecord) {
        super(message, err);
        this.oldData = oldRecord;
        this.newData = newRecord;
    }


    public DatabaseManager.QRRecord getOldData() {
        return oldData;
    }

    public DatabaseManager.QRRecord getNewData() {
        return newData;
    }
}

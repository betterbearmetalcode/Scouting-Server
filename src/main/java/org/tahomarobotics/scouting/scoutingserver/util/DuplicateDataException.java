package org.tahomarobotics.scouting.scoutingserver.util;

import java.sql.SQLException;

public class DuplicateDataException extends Exception {
    public DuplicateDataException(String message, Throwable err) {
        super(message, err);
    }
}

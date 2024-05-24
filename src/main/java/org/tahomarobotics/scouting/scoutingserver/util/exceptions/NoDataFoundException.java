package org.tahomarobotics.scouting.scoutingserver.util.exceptions;

import java.io.IOException;

public class NoDataFoundException extends IOException {
    public NoDataFoundException (String msg) {
        super(msg);
    }
}

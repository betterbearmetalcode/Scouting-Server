package org.tahomarobotics.scouting.scoutingserver.util.exceptions;

public class OperationAbortedByUserException extends Throwable {
    public OperationAbortedByUserException() {
        super();
    }

    public OperationAbortedByUserException(String message) {
        super(message);
    }
}

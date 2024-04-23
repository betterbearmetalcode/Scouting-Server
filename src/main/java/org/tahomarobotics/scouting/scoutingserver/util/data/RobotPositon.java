package org.tahomarobotics.scouting.scoutingserver.util.data;

import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;

import java.util.LinkedList;

public record RobotPositon(DatabaseManager.RobotPosition robotPosition, int teamNumber, LinkedList<DataPoint<String>> data, DatabaseManager.QRRecord record) {
    public boolean isUnknown() {
        return  data.get(0).getErrorLevel() == DataPoint.ErrorLevel.UNKNOWN;
    }
}

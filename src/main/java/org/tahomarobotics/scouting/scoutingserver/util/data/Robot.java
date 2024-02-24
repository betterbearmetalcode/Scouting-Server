package org.tahomarobotics.scouting.scoutingserver.util.data;

import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;

import java.util.LinkedList;

public record Robot(DatabaseManager.RobotPosition robotPosition, int teamNumber, LinkedList<DataPoint> data, DatabaseManager.QRRecord record) {
}

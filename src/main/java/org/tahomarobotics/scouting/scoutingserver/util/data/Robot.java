package org.tahomarobotics.scouting.scoutingserver.util.data;

import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public record Robot(DatabaseManager.RobotPosition robotPosition, int teamNumber, LinkedList<DataPoint> data, DatabaseManager.MatchRecord record) {
}

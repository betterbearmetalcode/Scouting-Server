package org.tahomarobotics.scouting.scoutingserver.util.data;

import java.util.ArrayList;

public class Team {
    public ArrayList<RobotPositon> robotPositons;
    public int numMatches;

    public int teamNumber;

    public Team(ArrayList<RobotPositon> positons, int teamNum) {
        this.robotPositons = positons;
        this.numMatches = robotPositons.size();
        this.teamNumber = teamNum;
    }
}

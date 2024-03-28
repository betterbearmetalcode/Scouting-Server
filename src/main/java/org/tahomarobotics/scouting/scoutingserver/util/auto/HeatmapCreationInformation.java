package org.tahomarobotics.scouting.scoutingserver.util.auto;

import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;

import java.util.ArrayList;
import java.util.HashMap;

//records information nessacary to create heatmap and is returned as a dialog result
public record HeatmapCreationInformation(
                                        //team numbers of each allinace
                                        HashMap<DatabaseManager.RobotPosition, String> teams,

                                         //all the tables that data will be pulled from
                                         ArrayList<String> dataTables) {
    @Override
    public String toString() {
        return "Teams: " + teams.toString() +", Data tables: " +  dataTables.toString();
    }

}

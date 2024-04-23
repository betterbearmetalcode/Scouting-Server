package org.tahomarobotics.scouting.scoutingserver.util.auto;


import java.awt.*;
import java.util.HashMap;

public record TeamAutoHistory(String teamNumber,

                              //can be null if the team has no data
                              HashMap<AutoPath, Integer> paths, Color teamColor) {


    public boolean hasAuto(AutoPath pathToCompart) {
        if (paths == null) {
            return false;
        }
        for (AutoPath autoPath : paths.keySet()) {
            if (autoPath.equals(pathToCompart)) {
                return true;
            }
        }
        return false;
    }


}

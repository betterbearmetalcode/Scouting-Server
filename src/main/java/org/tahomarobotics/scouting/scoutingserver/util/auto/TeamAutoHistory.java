package org.tahomarobotics.scouting.scoutingserver.util.auto;

import java.util.ArrayList;
import java.util.HashMap;

public record TeamAutoHistory(String teamNumber,

                              //can be null if the team has no data
                              HashMap<AutoPath, Integer> paths) {


    public boolean hasAuto(AutoPath pathToCompart) {
        for (AutoPath autoPath : paths.keySet()) {
            if (autoPath.equals(pathToCompart)) {
                return true;
            }
        }
        return false;
    }


}

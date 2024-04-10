package org.tahomarobotics.scouting.scoutingserver.util.data;

import java.util.ArrayList;

public record TrapThing(String teamNumber,
                        int numTraps,
                        ArrayList<String> events) {


    public int getAverageTrap() {
        return numTraps/events.size();
    }

}

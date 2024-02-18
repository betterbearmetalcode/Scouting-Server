package org.tahomarobotics.scouting.scoutingserver.util;

import org.tahomarobotics.scouting.scoutingserver.DataHandler;

import java.util.Comparator;

public class MatchDataComparator implements Comparator<DataHandler.MatchRecord> {
    @Override
    public int compare(DataHandler.MatchRecord o1, DataHandler.MatchRecord o2) {
            //first check match number
            if (o1.matchNumber() > o2.matchNumber()) {
                //then the one with the lower match number should go first
                return 1;
            }else //then they are in the same match so check robot positions
                //then they are in the same match so check robot positions
                if (o2.matchNumber() > o1.matchNumber()) {
                return -1;
            }else return Integer.compare(o1.position().ordinal(), o2.position().ordinal());
    }
}

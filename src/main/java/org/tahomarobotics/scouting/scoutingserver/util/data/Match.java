package org.tahomarobotics.scouting.scoutingserver.util.data;

import java.util.ArrayList;

public record Match(int matchNumber, ArrayList<RobotPositon> robotPositons) {

        public boolean isUnknown() {
            return robotPositons.stream().anyMatch(RobotPositon::isUnknown) || (robotPositons.size() != 6);
        }
}

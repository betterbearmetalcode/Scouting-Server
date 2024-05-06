package org.tahomarobotics.scouting.scoutingserver.util.data;

import org.json.JSONObject;

public record DuplicateResolution(int matchNum, int teamNum, JSONObject dataToUse) {

    @Override
    public boolean equals(Object obj) {
        try {
            DuplicateResolution asdf = (DuplicateResolution) obj;
            return asdf.teamNum == teamNum() && asdf.matchNum == matchNum();
        }catch (Exception e) {
            return false;
        }
    }
}

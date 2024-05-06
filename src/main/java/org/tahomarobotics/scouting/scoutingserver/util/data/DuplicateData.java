package org.tahomarobotics.scouting.scoutingserver.util.data;

import org.json.JSONObject;

import java.util.ArrayList;

public record DuplicateData(ArrayList<JSONObject> data, int matchNum, int teamNum) {

}

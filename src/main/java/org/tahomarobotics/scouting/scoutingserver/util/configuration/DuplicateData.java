package org.tahomarobotics.scouting.scoutingserver.util.configuration;

import org.json.JSONObject;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class DuplicateData {
    public final ArrayList<JSONObject> duplicateData;

    public DuplicateData(ArrayList<JSONObject> duplicates) {
        duplicateData = duplicates;
    }
}

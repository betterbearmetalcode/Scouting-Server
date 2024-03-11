package org.tahomarobotics.scouting.scoutingserver.util;


import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.util.Callback;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.RobotPositon;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ALL")
public class SpreadsheetUtil {

    private static final String RAW_DATA_SHEET_NAME = "Raw Data";



    public static void writeArrayToSpreadsheet(ArrayList<ArrayList<String>> data, File spreadSheet) throws IOException {
        String path = spreadSheet.getAbsolutePath();
        try (OutputStream os = Files.newOutputStream(Paths.get(path)); Workbook wb = new Workbook(os, "Scouting Excel Database", "1.0")) {
            Worksheet ws = wb.newWorksheet(SpreadsheetUtil.RAW_DATA_SHEET_NAME);
            int rowNum = 0;
            for (ArrayList<String> row : data) {
                if (row != null) {
                    //there can be null values for some rows if there are teams in the data who are not at the competiion
                    int columnNumm = 0;
                    for (String value : row) {
                        ws.value(rowNum, columnNumm, value);
                        ws.width(columnNumm, 10);
                        columnNumm++;
                    }
                }else {
                    continue;
                }
                rowNum++;
            }
            for (int i = 0; i < data.size(); i++) {


            }
            ws.range(0, 0, data.get(0).size(), data.size()).style().wrapText(true);
        }//end try with resources
    }//end write to spreadsheet


}

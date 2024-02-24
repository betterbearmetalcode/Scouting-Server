package org.tahomarobotics.scouting.scoutingserver.util;


import org.dhatim.fastexcel.ConditionalFormattingRule;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.Robot;

import javax.print.attribute.standard.JobName;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class SpreadsheetUtil {

    private static final String RAW_DATA_SHEET_NAME = "Raw Data";

    private static final String CALCULATED_DATA_SHEET_NAME = "Calculated Data";

/*    public static Map<Integer, List<String>> readFromSpreadSheet(String fileLocation) {
        Map<Integer, List<String>> data = new HashMap<>();

        try (FileInputStream file = new FileInputStream(fileLocation); ReadableWorkbook wb = new ReadableWorkbook(file)) {
            Sheet sheet = wb.getFirstSheet();
            try (Stream<Row> rows = sheet.openStream()) {
                rows.forEach(r -> {
                    data.put(r.getRowNum(), new ArrayList<>());

                    for (Cell cell : r) {
                        data.get(r.getRowNum()).add(cell.getRawValue());
                    }
                });
            }
        } catch (IOException e) {
            Logging.logError(e);
        }

        return data;


    }*/


    public static void writeToSpreadSheet(ArrayList<Match> data, File currDir, String eventKey) throws IOException, InterruptedException {
        //query TBA for data not gathered by scouts
        //auto leave
        //tele climb
        JSONArray rawArr = APIUtil.get("/event/" + eventKey + "/matches");


        String path = currDir.getAbsolutePath();
        try (OutputStream os = Files.newOutputStream(Paths.get(path)); Workbook wb = new Workbook(os, "Scouting Excel Database", "1.0")) {
            Worksheet ws = wb.newWorksheet(SpreadsheetUtil.RAW_DATA_SHEET_NAME);

            int rowNum = 1;
            for (Match match : data) {
                HashMap<String, HashMap<String, HashMap<String, Object>>> matchObject = (HashMap<String, HashMap<String, HashMap<String, Object>>>) rawArr.toList().stream().filter(o -> ((HashMap<String,Integer>) o).get("match_number") == match.matchNumber()).findFirst().get();
                System.out.println(matchObject);
                HashMap<String, HashMap<String, Object>> breakdown = matchObject.get("score_breakdown");
                for (Robot robot : match.robots()) {
                    int robotNum = (robot.robotPosition().ordinal()%3) + 1;
                    HashMap<String, Object> allianceBreakdown = breakdown.get((robot.record().position().ordinal() < 3)?"red":"blue");
                    boolean autoLeave = Objects.equals(allianceBreakdown.get("autoLineRobot" + robotNum), "Yes");
                    int climbPoints = 0;
                    int endgame = 0;
                    switch (allianceBreakdown.get("endGameRobot" + robotNum).toString()) {
                        case "Parked": {
                            climbPoints = 1;
                            endgame = 1;
                            break;
                        }
                        case "CenterStage", "StageLeft", "StageRight": {
                            climbPoints = 3;
                            endgame = 2;
                            break;
                        }

                    }
                    int teleAmpPoints = robot.record().teleAmp() * Constants.TELE_AMP_NOTE_POINTS;
                    int teleSpeakerPoints = robot.record().teleSpeaker() * Constants.TELE_SPEAKER_NOTE_POINTS;
                    int trapPoints = robot.record().teleTrap() * Constants.TELE_TRAP_POINTS;
                    int telePoints = teleAmpPoints + teleSpeakerPoints + trapPoints + climbPoints;
                    int autoPoints = (robot.record().autoAmp() * Constants.AUTO_AMP_NOTE_POINTS) + (robot.record().autoSpeaker()* Constants.AUTO_SPEAKER_NOTE_POINTS) + (autoLeave?2:0);
                    int toalNotesScored = robot.record().autoAmp() + robot.record().autoSpeaker() + robot.record().teleAmp() + robot.record().teleSpeaker();
                    int toalNotesMissed = robot.record().autoAmpMissed() + robot.record().autoAmpMissed() + robot.record().teleAmpMissed() + robot.record().teleSpeakerMissed();
                    LinkedList<DataPoint> output = robot.data();
                    output.add(new DataPoint("Left In Auto", autoLeave?"2":"0"));
                    output.add(new DataPoint("Total Auto Notes", String.valueOf(robot.record().autoAmp() + robot.record().autoSpeaker())));
                    output.add(new DataPoint("Total Tele Notes", String.valueOf(robot.record().teleAmp() + robot.record().teleSpeaker())));
                    output.add(new DataPoint("Auto Points Added", String.valueOf(autoPoints)));
                    output.add(new DataPoint("Tele Points Added", String.valueOf(telePoints)));
                    output.add(new DataPoint("Total Points Added", String.valueOf(autoPoints + telePoints)));
                    output.add(new DataPoint("Total Notes Scored", String.valueOf(toalNotesScored)));
                    output.add(new DataPoint("Total Notes Missed", String.valueOf(toalNotesMissed)));
                    output.add(new DataPoint("Total Notes", String.valueOf(toalNotesMissed + toalNotesScored)));
                    output.add(new DataPoint("EndameResult", String.valueOf(endgame)));
                    for (int i = 0; i < output.size(); i++) {
                        if (rowNum == 1) {
                            //only need to do this once
                            ws.width(i, 20);
                            ws.value(0, i, output.get(i).getName());
                            ws.range(0, 0, 0, output.size()).style().fontSize(12).fillColor("FFFF33").set();
                        }
                        ws.value(rowNum, i, output.get(i).getValue());

                    }
                    rowNum++;
                }
            }//end for each match
        }//end try
    }//end method

}

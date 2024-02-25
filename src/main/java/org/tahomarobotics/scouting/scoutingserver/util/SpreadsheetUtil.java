package org.tahomarobotics.scouting.scoutingserver.util;


import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.util.Pair;
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
import java.util.concurrent.atomic.AtomicReference;
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
        boolean haveInternet = true;
        JSONArray rawArr = APIUtil.get("/event/" + eventKey + "/matches");
        if (rawArr.get(0).equals("NoInternet")) {
            Logging.logInfo("Cannot export TBA Data");
            haveInternet = false;

            //only confinue after alerting the user to the risks of exporing without internet
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "You are trying to export without internet, doing so will result in incorrect data because you cannot access TBA Continue?");
            Optional<ButtonType> result = alert.showAndWait();
            AtomicReference<ButtonType> reference = new AtomicReference<>(ButtonType.CANCEL);
            result.ifPresent(reference::set);
            if (reference.get() == ButtonType.CANCEL) {
                Logging.logInfo("Export aborted by user");
                return;
            }

        }

        String path = currDir.getAbsolutePath();
        try (OutputStream os = Files.newOutputStream(Paths.get(path)); Workbook wb = new Workbook(os, "Scouting Excel Database", "1.0")) {
            Worksheet ws = wb.newWorksheet(SpreadsheetUtil.RAW_DATA_SHEET_NAME);

            int rowNum = 1;
            for (Match match : data) {
                HashMap<String, HashMap<String, Object>> breakdown = null;
                if (haveInternet) {
                    HashMap<String, HashMap<String, HashMap<String, Object>>> matchObject = (HashMap<String, HashMap<String, HashMap<String, Object>>>) rawArr.toList().stream().filter(o -> ((HashMap<String,Integer>) o).get("match_number") == match.matchNumber()).findFirst().get();
                    Logging.logInfo("WroteRow: " + matchObject);
                    breakdown = matchObject.get("score_breakdown");
                }

                int numRobotsWritten = 0;


                for (Robot robot : match.robots()) {
                    if (numRobotsWritten >= 6) {
                        //we can't have more than six robots in a match, this would be an edge case though
                        continue;
                    }
                    int robotNum = (robot.robotPosition().ordinal() % 3) + 1;
                    int climbPoints = 0;
                    boolean autoLeave = false;
                    int endgame = 0;
                    if (breakdown != null) {
                        //if we have internet, set stuff, otherwise the default is used
                        HashMap<String, Object> allianceBreakdown = breakdown.get((robot.record().position().ordinal() < 3) ? "red" : "blue");
                        autoLeave = Objects.equals(allianceBreakdown.get("autoLineRobot" + robotNum), "Yes");
                        climbPoints = 0;
                        endgame = 0;
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
                    }

                    ;

                    int teleAmpPoints = robot.record().teleAmp() * Constants.TELE_AMP_NOTE_POINTS;
                    int teleSpeakerPoints = robot.record().teleSpeaker() * Constants.TELE_SPEAKER_NOTE_POINTS;
                    int trapPoints = robot.record().teleTrap() * Constants.TELE_TRAP_POINTS;
                    int telePoints = teleAmpPoints + teleSpeakerPoints + trapPoints + climbPoints;
                    int autoPoints = (robot.record().autoAmp() * Constants.AUTO_AMP_NOTE_POINTS) + (robot.record().autoSpeaker() * Constants.AUTO_SPEAKER_NOTE_POINTS) + (autoLeave ? 2 : 0);
                    int toalNotesScored = robot.record().autoAmp() + robot.record().autoSpeaker() + robot.record().teleAmp() + robot.record().teleSpeaker();
                    int toalNotesMissed = robot.record().autoAmpMissed() + robot.record().autoAmpMissed() + robot.record().teleAmpMissed() + robot.record().teleSpeakerMissed();
                    LinkedList<DataPoint> output = robot.data();
                    output.add(new DataPoint("Left In Auto", autoLeave ? "2" : "0"));
                    output.add(new DataPoint("EndameResult", String.valueOf(endgame)));
                    output.add(new DataPoint("End Raw Data", ""));
                    output.add(new DataPoint("Total Auto Notes", String.valueOf(robot.record().autoAmp() + robot.record().autoSpeaker())));
                    output.add(new DataPoint("Total Tele Notes", String.valueOf(robot.record().teleAmp() + robot.record().teleSpeaker())));
                    output.add(new DataPoint("Auto Points Added", String.valueOf(autoPoints)));
                    output.add(new DataPoint("Tele Points Added", String.valueOf(telePoints)));
                    output.add(new DataPoint("Total Points Added", String.valueOf(autoPoints + telePoints)));
                    output.add(new DataPoint("Total Notes Scored", String.valueOf(toalNotesScored)));
                    output.add(new DataPoint("Total Notes Missed", String.valueOf(toalNotesMissed)));
                    output.add(new DataPoint("Total Notes", String.valueOf(toalNotesMissed + toalNotesScored)));

                    for (int i = 0; i < output.size(); i++) {
                        if (rowNum == 1) {
                            //only need to do this once
                            ws.width(i, 20);
                            ws.value(0, i, output.get(i).getName());
                            ws.range(0, 0, 0, output.size()).style().fontSize(12).fillColor("FFFF33").set();
                        }
                        ws.value(rowNum, i, output.get(i).getValue());
                        if (output.get(i).getName() == "End Raw Data") {
                            ws.range(0, i, 1000, i).style().fillColor("0c0c0c").set();
                        }

                    }
                    rowNum++;
                    numRobotsWritten++;
                }
                //if there were less than six robots for this match, put in some default data for noshows
                int numMissing = Math.max(0, 6-numRobotsWritten);
                for (int i = 0; i < numMissing; i++) {
                    for (int j = 0; j < 100; j++) {//surly there will never be more than 100 columns, right?
                        if (j == 1) {
                            ws.value(rowNum, j, match.matchNumber());
                        }else if (j == 2){
                            ws.value(rowNum, j, "NoData");
                        }else {
                            ws.value(rowNum, j, "");
                        }

                    }
                    rowNum++;
                }
            }//end for each match
        }//end try
    }//end method

}

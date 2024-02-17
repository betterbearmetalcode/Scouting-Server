package org.tahomarobotics.scouting.scoutingserver.util;




import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class SpreadsheetUtil {

    private static final String RAW_DATA_SHEET_NAME = "Raw Data";

    private static final String CALCULATED_DATA_SHEET_NAME = "Calculated Data";

    public static Map<Integer, List<String>> readFromSpreadSheet(String fileLocation) {
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
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return data;


    }




    public static void initializeExcelDatabase(String filename) throws IOException {

        File currDir = new File(filename);
        String path = currDir.getAbsolutePath();
        String fileLocation = path.substring(0, path.length() - 1);
        try (OutputStream os = Files.newOutputStream(Paths.get(fileLocation)); Workbook wb = new Workbook(os, "Scouting Excel Database", "1.0")) {

            Worksheet ws = wb.newWorksheet(SpreadsheetUtil.RAW_DATA_SHEET_NAME);

            initializeTopRowOfRawSheet(ws);//write in the column headings


        }
        System.out.println("Finished initializing workbook");

    }

    public static void writeToSpreadSheet(LinkedList<DataHandler.MatchRecord> data, File currDir, boolean exportFormulas) throws IOException {
        String path = currDir.getAbsolutePath();
        try (OutputStream os = Files.newOutputStream(Paths.get(path)); Workbook wb = new Workbook(os, "Scouting Excel Database", "1.0")) {
            Worksheet ws = wb.newWorksheet(SpreadsheetUtil.RAW_DATA_SHEET_NAME);
           initializeTopRowOfRawSheet(ws);
           //export raw data
            for (int rowNum = 1; rowNum < data.size(); rowNum++) {
                //for each row in the data or spreadsheet
                for (int i = 0; i < data.get(rowNum).getDataAsList().size(); i ++) {
                    //for each element of data
                    ws.value(rowNum, i,data.get(rowNum).getDataAsList().get(i));
                }


                if (exportFormulas) {
                    //TODO make it export the formulas);

                    for (int i = 18; i < 24; i++) {
                        int autoNotes = (data.get(rowNum).autoAmp() + data.get(rowNum).autoSpeaker());
                        int teleNotes = (data.get(rowNum).teleAmp() + data.get(rowNum).teleSpeaker());

                        int teleAmpPoints = data.get(rowNum).teleAmp() * Constants.TELE_AMP_NOTE_POINTS;
                        int teleSpeakerPoints = data.get(rowNum).teleSpeaker() * Constants.TELE_SPEAKER_NOTE_POINTS;
                        int trapPoints = data.get(rowNum).teleTrap() * Constants.TELE_TRAP_POINTS;
                        int climbPoints = Constants.endgamePoints.get(data.get(rowNum).endgamePosition());
                        int telePoints = teleAmpPoints + teleSpeakerPoints + trapPoints + climbPoints;
                        int autoPoints = (data.get(rowNum).autoAmp() * Constants.AUTO_AMP_NOTE_POINTS) + (data.get(rowNum).autoSpeaker() * Constants.AUTO_SPEAKER_NOTE_POINTS);

                        switch (i) {
                            case 18: {
                                //total auto notes
                                ws.value(rowNum, i, autoNotes);
                                break;
                            }
                            case 19: {
                                //total tele notes
                                ws.value(rowNum, i, teleNotes);
                                break;
                            }
                            case 20: {
                                //auto points added
                                //this will only include points from notes
                                ws.value(rowNum, i, autoPoints);
                                break;
                            }
                            case 21: {
                                //tele will  include notes in speaker an amp, not counting it they are amplified,
                                //trap points
                                //clinb points
                                ws.value(rowNum, i, telePoints);
                                break;

                            } case 22: {
                                //total points added
                                ws.value(rowNum, i,(telePoints + autoPoints));
                                break;
                            } case 23 : {
                                //total notes
                                ws.value(rowNum, i, (teleNotes + autoNotes));
                                break;
                            }
                        }
                    }
                    //TODO also fix the webcam issue
                }//end exporting formulas for this row
            }




        }
    }

    private static void initializeTopRowOfRawSheet(Worksheet ws) {
        for (int i = 0; i < 23; i++) {
            ws.width(i, 20);
        }

        //first read all the data that may already be there and write it into the workbook.

        ws.range(0, 0, 0, 24).style().fontSize(12).fillColor("FFFF33").set();
        ws.value(0, 0, "Timestamp");
        ws.value(0, 1, "Match Number");
        ws.value(0, 2, "Team Number");
        ws.value(0, 3, "Alliance Position");
        ws.value(0, 4, "Auto Leave");
        ws.value(0, 5, "Auto Speaker");
        ws.value(0, 6, "Auto Amp");
        ws.value(0, 7, "Auto Collected");
        ws.value(0, 8, "Auto Speaker Missed");
        ws.value(0, 9, "Auto Amp Missed");
        ws.value(0, 10, "Tele Speaker");
        ws.value(0, 11, "Tele Amp");
        ws.value(0, 12, "Tele Trap");
        ws.value(0, 13, "Tele Speaker Missed");
        ws.value(0, 14, "Tele Amp Missed");
        ws.value(0, 15, "Endgame Position");
        ws.value(0, 16, "Auto Notes");
        ws.value(0, 17, "Tele Notes");

        ws.value(0,18,"Total Auto Notes");
        ws.value(0,19,"Total Tele Notes");
        ws.value(0,20,"Auto Points Added");
        ws.value(0,21,"Tele Points Added");
        ws.value(0,22,"Total Points Added");
        ws.value(0,23,"Total Notes");
    }
}

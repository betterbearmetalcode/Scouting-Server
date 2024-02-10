package org.tahomarobotics.scouting.scoutingserver.util;




import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SpreadsheetUtil {

    private static final String RAW_DATA_SHEET_NAME = "Raw Data";

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

            for (int i = 0; i < 13; i++) {
                ws.width(i, 15);
            }

            //first read all the data that may already be there and write it into the workbook.

            ws.range(0, 0, 0, 12).style().fontSize(12).fillColor("FFFF33").set();
            ws.value(0, 0, "Timestamp");
            ws.value(0, 1, "Match Number");
            ws.value(0, 2, "Team Number");
            ws.value(0, 3, "Alliance Position");
            ws.value(0, 4, "Auto Speaker");
            ws.value(0, 5, "Auto Amp");
            ws.value(0, 6, "Tele Speaker");
            ws.value(0, 7, "Tele Amp");
            ws.value(0, 8, "Tele Trap");
            ws.value(0, 9, "Endgame Position");
            ws.value(0, 10, "Lost Comms");
            ws.value(0, 11, "Auto Notes");
            ws.value(0, 12, "Tele Notes");

        }
        System.out.println("Finished initializing workbook");

    }

    public static void addDataRow(DataHandler.MatchRecord data, String filename) throws IOException {
        File currDir = new File(filename);
        String path = currDir.getAbsolutePath();
        String fileLocation = path.substring(0, path.length() - 1);
        try (OutputStream os = Files.newOutputStream(Paths.get(fileLocation)); Workbook wb = new Workbook(os, "Scouting Excel Database", "1.0")) {
            Worksheet ws = wb.newWorksheet(SpreadsheetUtil.RAW_DATA_SHEET_NAME);
            ws.value(1, 0 , "Testing");



        }
    }
}

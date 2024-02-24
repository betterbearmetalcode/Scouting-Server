package org.tahomarobotics.scouting.scoutingserver.util;


import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;

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
        } catch (IOException e) {
            Logging.logError(e);
        }

        return data;


    }


/*    public static void writeToSpreadSheet(LinkedList<DatabaseManager.QRRecord> data, File currDir, boolean exportFormulas) throws IOException {
        String path = currDir.getAbsolutePath();
        try (OutputStream os = Files.newOutputStream(Paths.get(path)); Workbook wb = new Workbook(os, "Scouting Excel Database", "1.0")) {
            Worksheet ws = wb.newWorksheet(SpreadsheetUtil.RAW_DATA_SHEET_NAME);

            for (int i = 0; i < data.get(0).getDataAsList().size(); i++) {
                ws.width(i, 20);
            }

            //first read all the data that may already be there and write it into the workbook.

            ws.range(0, 0, 0, data.get(0).getDataAsList().size()).style().fontSize(12).fillColor("FFFF33").set();
            //export raw data
            for (int rowNum = 0; rowNum < data.size(); rowNum++) {
                //for each row in the data or spreadsheet

                for (int i = 0; i < data.get(rowNum).getDataAsList().size(); i++) {
                    //for each element of data
                    if (rowNum == 0) {
                        //then we need to write the header
                        ws.value(rowNum, i, data.get(rowNum).getDataAsList().get(i).getName());
                    } else {
                        ws.value(rowNum, i, data.get(rowNum).getDataAsList().get(i).getValue());
                    }

                }
            }


        }
    }*/

}

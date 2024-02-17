package org.tahomarobotics.scouting.scoutingserver;

import org.tahomarobotics.scouting.scoutingserver.util.DatabaseManager.SQLDatatype;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class Constants {
    //qr codes images are cached in this folder. Filenames are the timestamp they were created and file extensions are .bmp
   // public static final String IMAGE_DATA_FILEPATH = "src/main/resources/org/tahomarobotics/scouting/scoutingserver/images/";
    public static final String IMAGE_DATA_FILEPATH = System.getProperty("user.dir") + "/resources/images/";
    //public static final String DATABASE_FILEPATH = "src/main/resources/org/tahomarobotics/scouting/scoutingserver/database/";
    public static final String DATABASE_FILEPATH = System.getProperty("user.dir") + "/resources/database/";

    public static final String SQL_DATABASE_NAME = "database.db";

    public static final String QR_DATA_DELIMITER = "/";

    public static final String STORED_DATA_DELIMITER= "@";

    public static final String TEST_QR_STRING_1 = "1/2046/0/1/1/2/3/4/5/6/7/8/9/10/0/autoNotesTest1/teleNotes";
    public static final String TEST_QR_STRING_2 = "1/4414/1/0/9/8/7/6/5/4/3/2/1/0/0/autoNotesTest2/teleNotes";

    public static final String TEST_QR_STRING_3 = "1/1323/2/0/9/8/7/6/5/4/3/2/1/0/0/autoNotesTest3/teleNotes";
    public static final String TEST_QR_STRING_4 = "2/2046/0/1/1/2/3/4/5/6/7/8/9/10/0/autoNotesTest4/teleNotes";
    public static final String TEST_QR_STRING_5 = "2/4414/1/0/9/8/7/6/5/4/3/2/1/0/0/autoNotesTest5/teleNotes";
    public static final String TEST_QR_STRING_6 = "2/1323/2/0/9/8/7/6/5/4/3/2/1/0/0/autoNotesTest6/teleNotes";
    public static final String TEST_QR_STRING_7 = "2/254/3/1/1/2/3/4/5/6/7/8/9/10/0/autoNotesTest7/teleNotes";
    public static final String TEST_QR_STRING_8 = "2/1678/4/0/9/8/7/6/5/4/3/2/1/0/0/autoNotesTest8/teleNotes";
    public static final String TEST_QR_STRING_9 = "2/2056/5/0/9/8/7/6/5/4/3/2/1/0/0/autoNotesTest9/teleNotes";


    public enum ColumnName {
        TIMESTAMP,
        MATCH_NUM,
        TEAM_NUM,
        ALLIANCE_POS,
        AUTO_LEAVE,
        AUTO_SPEAKER,
        AUTO_AMP,
        AUTO_COLLECTED,
        AUTO_SPEAKER_MISSED,
        AUTO_AMP_MISSED,
        TELE_SPEAKER,
        TELE_AMP,
        TELE_TRAP,
        TELE_SPEAKER_MISSED,
        TELE_AMP_MISSED,

        ENDGAME_POS,
        AUTO_NOTES,
        TELE_NOTES
    }


    public static final ArrayList<ColumnType> RAW_TABLE_SCHEMA =new ArrayList<>(List.of(
            new ColumnType(ColumnName.TIMESTAMP, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.MATCH_NUM, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TEAM_NUM, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.ALLIANCE_POS, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.AUTO_LEAVE, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.AUTO_SPEAKER, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.AUTO_AMP, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.AUTO_COLLECTED, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.AUTO_SPEAKER_MISSED, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.AUTO_AMP_MISSED, SQLDatatype.INTEGER),


            new ColumnType(ColumnName.TELE_SPEAKER, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TELE_AMP, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TELE_TRAP, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TELE_SPEAKER_MISSED, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TELE_AMP_MISSED, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.ENDGAME_POS, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.AUTO_NOTES, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TELE_NOTES, SQLDatatype.INTEGER)

            ));
    public static final String DEFAULT_SQL_TABLE_NAME = "Default_Table";

    public static final String TEST_SQL_TABLE_NAME = "debug_table";

    public record ColumnType(ColumnName name, SQLDatatype datatype){
    }


    public static int AUTO_AMP_NOTE_POINTS = 2;
    public static int AUTO_SPEAKER_NOTE_POINTS = 5;

    public static int TELE_SPEAKER_NOTE_POINTS = 2;
    public static int TELE_AMP_NOTE_POINTS = 1;
    public static int TELE_TRAP_POINTS = 5;

    public static Map<DataHandler.EndgamePosition, Integer> endgamePoints  = Map.ofEntries(entry(DataHandler.EndgamePosition.NONE, 0),
            entry(DataHandler.EndgamePosition.PARKED, 1),
            entry(DataHandler.EndgamePosition.CLIMBED, 3),
            entry(DataHandler.EndgamePosition.HARMONIZED, 5));



}
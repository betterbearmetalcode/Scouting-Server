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
    public static final String DEFAULT_JANK_DATABASE_NAME = "jankDatabase.txt";

    public static final String SQL_DATABASE_NAME = "database.db";

    public static final String QR_DATA_DELIMITER = "/";

    public static final String STORED_DATA_DELIMITER= "@";

    public enum ColumnName {
        TIMESTAMP,
        MATCH_NUM,
        TEAM_NUM,
        ALLIANCE_POS,
        AUTO_AMP,
        AUTO_SPEAKER,
        TELE_SPEAKER,
        TELE_AMP,
        TELE_TRAP,
        ENDGAME_POS,
        LOST_COMMS,
        AUTO_NOTES,
        TELE_NOTES
    }

    /*public static final ArrayList<ColumnType> RAW_TABLE_SCHEMA = new ColumnType(Map.ofEntries(
            entry(ColumnName.TIMESTAMP.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.MATCH_NUM.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.TEAM_NUM.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.ALLIANCE_POS.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.AUTO_AMP.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.AUTO_SPEAKER.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.TELE_SPEAKER.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.TELE_AMP.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.TELE_TRAP.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.ENDGAME_POS.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.LOST_COMMS.toString(), SQLDatatype.INTEGER),
            entry(ColumnName.AUTO_NOTES.toString(), SQLDatatype.TEXT),
            entry(ColumnName.TELE_NOTES.toString(), SQLDatatype.TEXT)
            ));*/

    public static final ArrayList<ColumnType> RAW_TABLE_SCHEMA =new ArrayList<>(List.of(
            new ColumnType(ColumnName.TIMESTAMP, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.MATCH_NUM, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TEAM_NUM, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.ALLIANCE_POS, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.AUTO_AMP, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.AUTO_SPEAKER, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TELE_SPEAKER, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TELE_AMP, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TELE_TRAP, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.ENDGAME_POS, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.LOST_COMMS, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.AUTO_NOTES, SQLDatatype.INTEGER),
            new ColumnType(ColumnName.TELE_NOTES, SQLDatatype.INTEGER)

            ));
    public static final String DEFAULT_SQL_TABLE_NAME = "matchData";

    public record ColumnType(ColumnName name, SQLDatatype datatype){
    }
}
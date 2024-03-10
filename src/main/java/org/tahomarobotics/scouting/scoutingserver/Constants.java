package org.tahomarobotics.scouting.scoutingserver;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Pair;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil.SQLDatatype;
import org.tahomarobotics.scouting.scoutingserver.util.data.RobotPositon;

import java.util.*;
import java.util.List;

public class Constants {
    //qr codes images are cached in this folder. Filenames are the timestamp they were created and file extensions are .bmp
    // public static final String IMAGE_DATA_FILEPATH = "src/main/resources/org/tahomarobotics/scouting/scoutingserver/images/";
    public static final String BASE_APP_DATA_FILEPATH = System.getenv("APPDATA") + "/BearMetalScoutingServer";//this is for files that are generated by the app
    public static final String BASE_READ_ONLY_FILEPATH = System.getProperty("user.dir");//this is for files that are installed with the app the files must be seperated so that the program can be run as a non-dmin
    public static final String IMAGE_DATA_FILEPATH = BASE_APP_DATA_FILEPATH + "/resources/images/";
    public static final String DATABASE_FILEPATH = BASE_APP_DATA_FILEPATH + "/resources/database/";

    public static String QR_IAMGE_QUERY_LOCATION = IMAGE_DATA_FILEPATH;//default to iamges directory in app data, but user wants to change, i'm not stopping them!


    public static final String SQL_DATABASE_NAME = "database.db";

    public static final String QR_DATA_DELIMITER = "/";

    public static final int WIRED_DATA_TRANSFER_PORT = 45482;

    public static final String TEST_QR_STRING_1 = "1/2046/1/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest1/teleNotes";
    public static final String TEST_QR_STRING_2 = "1/4414/1/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest2/teleNotes";

    public static final String TEST_QR_STRING_3 = "1/1323/1/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest3/teleNotes";
    public static final String TEST_QR_STRING_4 = "2/2046/1/3/0/4/5/0/0/0/0/0/0/0/0/9/2/0/9/0/autoNotesTest4/teleNotes";
    public static final String TEST_QR_STRING_5 = "2/4414/1/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest5/teleNotes";
    public static final String TEST_QR_STRING_6 = "2/1323/1/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest6/teleNotes";
    public static final String TEST_QR_STRING_7 = "2/2540/1/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest7/teleNotes";
    public static final String TEST_QR_STRING_8 = "2/1678/1/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest8/teleNotes";
    public static final String TEST_QR_STRING_9 = "2/2056/1/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest9/teleNotes";
    public static ArrayList<String> oneMatchOfDebufStrings = new ArrayList<>();
     static {

        oneMatchOfDebufStrings.add("1/4338/0/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest1/teleNotes");
        oneMatchOfDebufStrings.add("1/4320/1/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest2/teleNotes");
        oneMatchOfDebufStrings.add("1/5990/2/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest3/teleNotes");
        oneMatchOfDebufStrings.add("1/4319/3/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest4/teleNotes");
        oneMatchOfDebufStrings.add("1/1690/4/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest5/teleNotes");
        oneMatchOfDebufStrings.add("1/9303/5/2/3/4/5/0/0/0/0/0/0/0/0/6/7/8/9/0/autoNotesTest6/teleNotes");
    }
    public enum SQLColumnName {
        MATCH_NUM,
        TEAM_NUM,
        ALLIANCE_POS,
        AUTO_SPEAKER,
        AUTO_AMP,
        AUTO_SPEAKER_MISSED,
        AUTO_AMP_MISSED,
        F1,
        F2,
        F3,
        M1,
        M2,
        M3,
        M4,
        M5,

        TELE_SPEAKER,
        TELE_AMP,
        TELE_TRAP,
        TELE_SPEAKER_MISSED,
        TELE_AMP_MISSED,

        AUTO_COMMENTS,
        TELE_COMMENTS

    }


    public static final ArrayList<ColumnType> RAW_TABLE_SCHEMA = new ArrayList<>(List.of(
            new ColumnType(SQLColumnName.MATCH_NUM, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TEAM_NUM, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.ALLIANCE_POS, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.AUTO_SPEAKER, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.AUTO_AMP, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.AUTO_SPEAKER_MISSED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.AUTO_AMP_MISSED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.F1, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.F2, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.F3, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.M1, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.M2, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.M3, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.M4, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.M5, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_SPEAKER, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_AMP, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_TRAP, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_SPEAKER_MISSED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_AMP_MISSED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.AUTO_COMMENTS, SQLDatatype.TEXT),
            new ColumnType(SQLColumnName.TELE_COMMENTS, SQLDatatype.TEXT)

    ));

    public static final String TEST_SQL_TABLE_NAME = "debug_table";

    public record ColumnType(SQLColumnName name, SQLDatatype datatype) {
    }


    public static final int AUTO_AMP_NOTE_POINTS = 2;
    public static final int AUTO_SPEAKER_NOTE_POINTS = 5;

    public static final int TELE_SPEAKER_NOTE_POINTS = 2;
    public static final int TELE_AMP_NOTE_POINTS = 1;
    public static final int TELE_TRAP_POINTS = 5;


    public static int LOW_ERROR_THRESHOLD = 2;
    public static final String DEVELOPER_PASSWORD = "2046devPass";

    public static boolean devUnlocked = false;
    public static class UIValues {

        public static final double WIDTH_MULTIPLIER = .75;
        @SuppressWarnings("SuspiciousNameCombination")
        public static final double HEIGHT_MULTIPLIER = WIDTH_MULTIPLIER;
       // public static  double APP_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height * HEIGHT_MULTIPLIER;
        public static final int MIN_HAMBURGER_MENU_SIZE = 155;

        public static final int MIN_MAIN_BUTTON_BAR_HEIGHT = 50;


        //property for width of main content panes

        private static final DoubleProperty splitWidthProperty = new SimpleDoubleProperty();

        public static DoubleProperty splitWidthPropertyProperty() {
            return splitWidthProperty;
        }

        public  static double getSplitWidthProperty() {
            return splitWidthProperty.get();
        }

        public static void setSplitWidthProperty(double splitWidthProperty) {
            Constants.UIValues.splitWidthProperty.set(splitWidthProperty);
        }

        //property for height in main scnene

        private static final DoubleProperty mainScrollPaneHeightProperty = new SimpleDoubleProperty();
        public static DoubleProperty mainScrollPaneHeightProperty() {
            return mainScrollPaneHeightProperty;
        }

        public  static double getMainScrollPaneHeightProperty() {
            return mainScrollPaneHeightProperty.get();
        }

        public static void setMainScrollPaneHeightProperty(double value) {
            Constants.UIValues.mainScrollPaneHeightProperty.set(value);
        }

        //property for height in datacollection scnene

        private static final DoubleProperty dataCollectionScrollPaneHeightProperty = new SimpleDoubleProperty();
        public static DoubleProperty dataCollectionScrollPaneHeightProperty() {
            return dataCollectionScrollPaneHeightProperty;
        }

        public  static double getDataCollectionScrollPaneHeightProperty() {
            return mainScrollPaneHeightProperty.get();
        }

        public static void setCollectionScrollPaneHeightProperty(double value) {
            Constants.UIValues.dataCollectionScrollPaneHeightProperty.set(value);
        }

        //property for app Width in main scnene

        private static final DoubleProperty appWidthProperty = new SimpleDoubleProperty();
        public static DoubleProperty appWidtProperty() {
            return appWidthProperty;
        }

        public  static double getAppWidth() {
            return appWidthProperty.get();
        }

        public static void setAppWidthProperty(double value) {
            Constants.UIValues.appWidthProperty.set(value);
        }

        //property for app height in main scnene

        private static final DoubleProperty appHeightProperty = new SimpleDoubleProperty();
        public static DoubleProperty appHeightProperty() {
            return appHeightProperty;
        }

        public  static double getAppHeight() {
            return appHeightProperty.get();
        }

        public static void setAppHeight(double value) {
            Constants.UIValues.appHeightProperty.set(value);
        }


        //property for height in databaseManagement

        private static final DoubleProperty databaseHeightProperty = new SimpleDoubleProperty();
        public static DoubleProperty databaseHeightProperty() {
            return databaseHeightProperty;
        }

        public  static double getDatabaseHeightProperty() {
            return databaseHeightProperty.get();
        }

        public static void setDatabaseHeightProperty(double value) {
            Constants.UIValues.databaseHeightProperty.set(value);
        }

        public static final DoubleProperty buttonBarHeightProperty = new SimpleDoubleProperty();
        static {
            buttonBarHeightProperty.set(UIValues.MIN_MAIN_BUTTON_BAR_HEIGHT);
        }
    }


}
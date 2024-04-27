package org.tahomarobotics.scouting.scoutingserver;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil.SQLDatatype;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Constants {
    public static final String BASE_APP_DATA_FILEPATH = System.getenv("APPDATA") + "/BearMetalScoutingServer";//this is for files that are generated by the app
    public static final String BASE_READ_ONLY_FILEPATH = System.getProperty("user.dir");//this is for files that are installed with the app the files must be seperated so that the program can be run as noteA non-dmin
    public static final String IMAGE_DATA_FILEPATH = BASE_APP_DATA_FILEPATH + "/resources/images/";
    public static final String DATABASE_FILEPATH = BASE_APP_DATA_FILEPATH + "/resources/database/";

    public static final String LOG_PATH = Constants.BASE_APP_DATA_FILEPATH + "/resources/logs";

    public static final String CONFIG_FILE_LOCATION = BASE_APP_DATA_FILEPATH + "/resources/config/config.cfg";
    public static final String SQL_DATABASE_NAME = "database.db";

    public static final String QR_DATA_DELIMITER = "/";

    public static final int WIRED_DATA_TRANSFER_PORT = 45482;

    public enum SQLColumnName {
        MATCH_NUM,
        TEAM_NUM,
        ALLIANCE_POS,
        AUTO_SPEAKER,
        AUTO_AMP,
        AUTO_SPEAKER_MISSED,
        AUTO_AMP_MISSED,
        NOTE_1,
        NOTE_2,
        NOTE_3,
        NOTE_4,
        NOTE_5,
        NOTE_6,
        NOTE_7,
        NOTE_8,
        NOTE_9,
        A_STOP,
        SHUTTLED,

        TELE_SPEAKER,
        TELE_AMP,
        TELE_TRAP,
        TELE_SPEAKER_MISSED,
        TELE_AMP_MISSED,
        SPEAKER_RECEIVED,
        AMP_RECEIVED,

        LOST_COMMS,
        TELE_COMMENTS

    }
    public enum ExportedDataMetrics {
         LEFT_IN_AUTO,
         END_GAME_RESULT,
        END_RAW_DATA,
        TOTAL_AUTO_NOTES,
        TOTAL_TELE_NOTES,
        AUTO_POINTS_ADDED,
        TELE_POINTS_ADDED,
        TOTAL_POINTS_ADDED,
        TOTAL_NOTES_SCORED,
        TOTAL_NOTES_MISSED,
        TOTAL_NOTES,
        TELE_SPEAKER_ADJUSTED,
        IS_SHUTTELING_MATCH,

        SCOUT_NAME,
        RAW_AUTO_DESCRIPTION,
        TELE_COMMENTS
    }

    public static final String SHUTTLED_NOTE_IDENTIFIER = "poodle";//identifier which scouts put in comments to indicate that a team is reciving shuttled notes
    public static final int SHUTTLED_NOTE_THRESHOLD = 2;//threshold which determines if the amount of shuttled notes in this match ought to be accounted for


    public static final ArrayList<ColumnType> RAW_TABLE_SCHEMA = new ArrayList<>(List.of(
            new ColumnType(SQLColumnName.MATCH_NUM, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TEAM_NUM, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.ALLIANCE_POS, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.AUTO_SPEAKER, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.AUTO_AMP, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.AUTO_SPEAKER_MISSED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.AUTO_AMP_MISSED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.NOTE_1, SQLDatatype.TEXT),
            new ColumnType(SQLColumnName.NOTE_2, SQLDatatype.TEXT),
            new ColumnType(SQLColumnName.NOTE_3, SQLDatatype.TEXT),
            new ColumnType(SQLColumnName.NOTE_4, SQLDatatype.TEXT),
            new ColumnType(SQLColumnName.NOTE_5, SQLDatatype.TEXT),
            new ColumnType(SQLColumnName.NOTE_6, SQLDatatype.TEXT),
            new ColumnType(SQLColumnName.NOTE_7, SQLDatatype.TEXT),
            new ColumnType(SQLColumnName.NOTE_8, SQLDatatype.TEXT),
            new ColumnType(SQLColumnName.NOTE_9, SQLDatatype.TEXT),
            new ColumnType(SQLColumnName.A_STOP, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.SHUTTLED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_SPEAKER, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_AMP, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_TRAP, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_SPEAKER_MISSED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_AMP_MISSED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.SPEAKER_RECEIVED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.AMP_RECEIVED, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.LOST_COMMS, SQLDatatype.INTEGER),
            new ColumnType(SQLColumnName.TELE_COMMENTS, SQLDatatype.TEXT)

    ));


    public record ColumnType(SQLColumnName name, SQLDatatype datatype) {
    }


    public static final int AUTO_AMP_NOTE_POINTS = 2;
    public static final int AUTO_SPEAKER_NOTE_POINTS = 5;

    public static final int TELE_SPEAKER_NOTE_POINTS = 2;
    public static final int TELE_AMP_NOTE_POINTS = 1;
    public static final int TELE_TRAP_POINTS = 5;


    public static int LOW_ERROR_THRESHOLD = 3;
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

        private static final DoubleProperty halfsplitWidthProperty = new SimpleDoubleProperty();

        public static DoubleProperty halfSplitWidthProperty() {
            return halfsplitWidthProperty;
        }

        public static void setHalfsplitWidthProperty(double value) {
            halfsplitWidthProperty.set(value);
        }

        public static DoubleProperty splitWidthPropertyProperty() {
            return splitWidthProperty;
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
    public static boolean askQuestion(String question) {
        Dialog<Boolean> continueDialog = new Dialog<>();
        continueDialog.setTitle("Continue?");
        continueDialog.setHeaderText(question);
        continueDialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
        continueDialog.setResultConverter(buttonType -> {return buttonType == ButtonType.YES;});
        Optional<Boolean> result = continueDialog.showAndWait();
        boolean finalResult = result.orElse(false);
        Logging.logInfo("Asked question: " + question + " Answer was: " + finalResult);
        return finalResult;
    }

}
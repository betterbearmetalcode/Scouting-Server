package org.tahomarobotics.scouting.scoutingserver;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.tahomarobotics.scouting.scoutingserver.controller.DataCollectionController;
import org.tahomarobotics.scouting.scoutingserver.controller.MainTabPane;
import org.tahomarobotics.scouting.scoutingserver.controller.MasterController;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.ServerUtil;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;

import java.io.File;
import java.sql.SQLException;

import static org.tahomarobotics.scouting.scoutingserver.Constants.UIValues.*;

public class ScoutingServer extends Application {


    /*
    notes
    have a settings page with the following settings
    if a team number is inconsistent with the match schedule should we validate that entry or mark it as unknown
    when exporting do we skip overloaded datapoints
    warn if importing data with all default values
    -------
    when validationg matches which are over or under loaded will be skipped
    make unused tables be deleted so they don't clog up the database.



    buttons i need
    validate-no dialog popping up, just validate
    export
    text field to set the competiton and data validation threshold for a database
    open button

     */


  //  public static MenuController.SCENES currentScene;
    public static Scene mainScene;


    public static Stage mainStage;


    public static VBox root = new VBox();


    public static MainTabPane mainTabPane;

    public static DataCollectionController dataCollectionController = new DataCollectionController();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void start(Stage stage) {
        Logging.logInfo("Starting");
        mainStage = stage;
        mainStage.setTitle("Scouting Server");
        mainStage.setScene(mainScene);
        mainStage.getIcons().add(new Image(Constants.BASE_READ_ONLY_FILEPATH + "/resources/Logo.png"));
        mainStage.setOnCloseRequest(event -> {
            ServerUtil.setServerStatus(false);
        });
        mainStage.setHeight(getAppHeight());
        mainStage.setWidth(getAppWidth());
        mainStage.show();

        try {
            Configuration.updateConfiguration();
        } catch (ConfigFileFormatException e) {
            Logging.logError(e);
        }

        try {
            //set up resources folder if not already created
            File databseFilepath = new File(Constants.DATABASE_FILEPATH);
            if (!databseFilepath.exists()) {
                databseFilepath.mkdirs();
            }
            File imageDataFilepath = new File(Constants.IMAGE_DATA_FILEPATH);
            if (!imageDataFilepath.exists()) {
                imageDataFilepath.mkdirs();
            }
            File testImageFilepath = new File(Constants.BASE_APP_DATA_FILEPATH + "/resources/testImages");
            if (!testImageFilepath.exists()) {
                testImageFilepath.mkdirs();
            }

            File duplicateDataFilepath = new File(Constants.BASE_APP_DATA_FILEPATH + "/resources/duplicateDataBackups");
            if (!duplicateDataFilepath.exists()) {
                duplicateDataFilepath.mkdirs();
            }

            File configFilepath = new File(Constants.BASE_APP_DATA_FILEPATH + "/resources/config");
            if (!configFilepath.exists()) {
                duplicateDataFilepath.mkdirs();
            }
            //set up database
            SQLUtil.initialize(Constants.DATABASE_FILEPATH + Constants.SQL_DATABASE_NAME);
        }catch (SecurityException e) {
            Logging.logError(e, "Unable to initialize internal folders, click ok to exit app. Try running as administrator");
            System.exit(1);
        } catch (SQLException e) {
            Logging.logError(e, "Failed to create sql database, the app cannot function without it. Click ok to exit");
            System.exit(1);
        }

        mainStage.widthProperty().addListener((observable, oldValue, newValue) -> {setAppWidthProperty(newValue.doubleValue()); resize(); });
        mainStage.heightProperty().addListener((observable, oldValue, newValue) -> {setAppHeight(newValue.doubleValue());resize(); });

    }


    @Override
    public void init() throws Exception {
        Logging.logInfo("Initializing");
        setUpMenuBar();
        setUpButtonBar();
        mainTabPane = new MainTabPane();
        root.getChildren().add(mainTabPane);
        mainScene = new Scene(root);

    }

    private static void resize() {
        resize(getAppWidth(), getAppHeight());

    }

    private static void resize(double appWidth, double appHeight) {
        root.setPrefSize(appWidth, appHeight);
        Constants.UIValues.setSplitWidthProperty(appWidth - MIN_HAMBURGER_MENU_SIZE);
        Constants.UIValues.setHalfsplitWidthProperty((appWidth - MIN_HAMBURGER_MENU_SIZE)/2);
        Constants.UIValues.setMainScrollPaneHeightProperty(appHeight - MIN_MAIN_BUTTON_BAR_HEIGHT);
        Constants.UIValues.setCollectionScrollPaneHeightProperty(appHeight - MIN_MAIN_BUTTON_BAR_HEIGHT - MIN_MAIN_BUTTON_BAR_HEIGHT);
        Constants.UIValues.setDatabaseHeightProperty(appHeight- MIN_MAIN_BUTTON_BAR_HEIGHT - MIN_MAIN_BUTTON_BAR_HEIGHT);
//        root.resize(appWidth, appHeight);

    }


    private static void setUpButtonBar() {
        HBox buttonBarBox = new HBox();


        root.getChildren().add(buttonBarBox);
    }

    private static void setUpMenuBar() {
        //file menu
        Menu fileMenu = new Menu("File");
        MenuItem newItem = new MenuItem("New");
        newItem.setOnAction(event -> MasterController.newThing());
        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(event -> MasterController.openJSONFileIntoDatabase());
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(event -> MasterController.saveCurrentDatabase());
        MenuItem saveAsItem = new MenuItem("Save As");
        saveAsItem.setOnAction(event -> MasterController.saveCurrentDatabaseAs());
        MenuItem saveAllItem = new MenuItem("SaveAll");
        saveAllItem.setOnAction(event -> MasterController.saveAllOpenDatabases());
        fileMenu.getItems().addAll(newItem, openItem, saveItem, saveAsItem, saveAllItem);

        //data menu
        Menu dataMenu = new Menu("Data");

        MenuItem dataTransferItem = new MenuItem("Start Data Transfer Server");
        dataTransferItem.setOnAction(event -> dataTransferItem.setText(MasterController.toggleDataTransferServer()));
        MenuItem jsonImportItem = new MenuItem("Import JSON");
        jsonImportItem.setOnAction(event -> MasterController.addJSONFile());
        MenuItem csvImportItem = new MenuItem("Import CSV");
        csvImportItem.setOnAction(event -> MasterController.addCSVFile());
        MenuItem validateItem = new MenuItem("Validate");
        validateItem.setOnAction(event -> MasterController.validateDatabase());
        MenuItem mergeDatabaseItem = new MenuItem("Merge Database");
        mergeDatabaseItem.setOnAction(event -> MasterController.mergeDatabases());
        dataMenu.getItems().addAll(dataTransferItem, jsonImportItem, csvImportItem, validateItem, mergeDatabaseItem);

        //tools menu
        //all game specific?
        Menu toolsMenu = new Menu("Tools");
        MenuItem autoHeatmapItem = new MenuItem("Create Auto Heatmap");//not supported rn
        autoHeatmapItem.setDisable(true);//:(
        MenuItem findTrapsItem = new MenuItem("Find Traps");
        findTrapsItem.setOnAction(event -> MasterController.findTraps());
        MenuItem stratScoutingScheduleButton = new MenuItem("Generate Strat Scouting Schedule");
        stratScoutingScheduleButton.setOnAction(event -> MasterController.genenerateStratScoutingSchedule());
        toolsMenu.getItems().addAll(autoHeatmapItem, findTrapsItem, stratScoutingScheduleButton);


        //help menu
        Menu helpMenu = new Menu("Help");
        MenuItem helpItem = new MenuItem("Open Documentation");
        helpItem.setOnAction(event -> MasterController.openDocumentation());
        MenuItem openEmotionalSupportItem = new MenuItem("Emotional Support");//rickroll shh -Caleb
        openEmotionalSupportItem.setOnAction(event -> MasterController.rickrollUser());
        helpMenu.getItems().addAll(helpItem, openEmotionalSupportItem);
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, dataMenu, toolsMenu, helpMenu);

        root.getChildren().add(menuBar);
    }


    public static void main(String[] args) {
        launch();
    }

}
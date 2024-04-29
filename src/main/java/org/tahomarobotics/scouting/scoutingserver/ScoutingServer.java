package org.tahomarobotics.scouting.scoutingserver;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.tahomarobotics.scouting.scoutingserver.controller.DataCollectionController;
import org.tahomarobotics.scouting.scoutingserver.controller.MainTabPaneController;
import org.tahomarobotics.scouting.scoutingserver.controller.MasterController;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.ServerUtil;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;

import java.io.File;
import java.sql.SQLException;

public class ScoutingServer extends Application {



  //  public static MenuController.SCENES currentScene;
    public static Scene mainScene;


    public static Scene dataCorrectionScene;

    public static Scene dataCollectionScene;

    public static Scene dataScene;

    public static Stage mainStage;

    public static Scene chartsScene;

    public static Scene miscScene;

    public static Scene autoHeatmapScene;

    public static VBox root = new VBox();


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
            Logging.logError(e, "Failed to create sql database, app cannot function without it click ok to exit");
            System.exit(1);
        }



    }

/*    public static void setCurrentScene(Scene scene) {
        double oldWidth = getAppWidth();
        double oldHeight = getAppHeight();
        mainStage.setScene(scene);
        mainStage.setWidth(oldWidth);
        mainStage.setHeight(oldHeight);

    }*/

    @Override
    public void init() throws Exception {
        Logging.logInfo("Initializing");
        setUpMenuBar();

        TabPane mainTabPane = new TabPane();
        Tab newTabTab = new Tab();
        Button newTabButton = new Button("+");
        newTabButton.setStyle("-fx-background-color:  #f4f4f4");
        newTabButton.setOnAction(event -> MainTabPaneController.makeNewTab(mainTabPane));
        newTabTab.setGraphic(newTabButton);
        newTabTab.setClosable(false);

        mainTabPane.getTabs().add(newTabTab);
        root.getChildren().add(mainTabPane);

        mainScene = new Scene(root);

    }

    private static void setUpMenuBar() {
        //file menu
        Menu fileMenu = new Menu("File");
        MenuItem newItem = new MenuItem("New");
        newItem.setOnAction(event -> MasterController.createDatabase());
        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(event -> MasterController.openJSONFileInDatabase());
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
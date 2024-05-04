package org.tahomarobotics.scouting.scoutingserver;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
    data transfer port
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

    //menubar components
    private static final Menu fileMenu = new Menu("File");
    private static final MenuItem newItem = new MenuItem("New");
    private static final MenuItem openItem = new MenuItem("Open");
    private static final MenuItem saveItem = new MenuItem("Save");
    private static final MenuItem saveAsItem = new MenuItem("Save As");
    private static final MenuItem saveAllItem = new MenuItem("SaveAll");
    //data mstatic enu
    private static final Menu dataMenu = new Menu("Data");
    private static final MenuItem dataTransferItem = new MenuItem("Start Data Transfer Server");
    private static final MenuItem jsonImportItem = new MenuItem("Import JSON");
    private static final MenuItem csvImportItem = new MenuItem("Import CSV");
    private static final MenuItem validateItem = new MenuItem("Validate");
    private static final MenuItem mergeDatabaseItem = new MenuItem("Merge Database");
      //all static game specific?
    private static final Menu toolsMenu = new Menu("Tools");
    private static final MenuItem autoHeatmapItem = new MenuItem("Create Auto Heatmap");//not supported rn
    private static final MenuItem findTrapsItem = new MenuItem("Find Traps");
    private static final MenuItem stratScoutingScheduleButton = new MenuItem("Generate Strat Scouting Schedule");
    private static final Menu helpMenu = new Menu("Help");
    private static final MenuItem helpItem = new MenuItem("Open Documentation");
    private static final MenuItem openEmotionalSupportItem = new MenuItem("Emotional Support");//rickroll shh -Caleb

    private static final Button saveButton = new Button();
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
        buttonBarBox.setSpacing(10);
        Button newButton = new Button();
        File newImageFile = new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/icons/new-icon.png");
        Image newImage = new Image(newImageFile.toURI().toString());
        ImageView newImageView = new ImageView();
        newImageView.setImage(newImage);
        newButton.setGraphic(newImageView);
        newButton.setOnAction(event -> MasterController.newThing());
        newButton.setTooltip(new Tooltip("New File"));
        buttonBarBox.getChildren().add(newButton);

        Button openButton = new Button();
        openButton.setTooltip(new Tooltip("Open File"));
        File openImageFile = new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/icons/open-icon.png");
        Image openImage = new Image(openImageFile.toURI().toString());
        ImageView openImageView = new ImageView();
        openImageView.setImage(openImage);
        openButton.setGraphic(openImageView);
        openButton.setOnAction(event -> MasterController.openJSONFileIntoDatabase());
        buttonBarBox.getChildren().add(openButton);



        File iamgeFile = new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/icons/save-icon.png");
        Image image = new Image(iamgeFile.toURI().toString());
        ImageView iamgeView = new ImageView();
        iamgeView.setImage(image);
        saveButton.setTooltip(new Tooltip("Save"));
        saveButton.setDisable(true);
        saveButton.setGraphic(iamgeView);
        saveButton.setOnAction(event -> MasterController.saveCurrentTab());
        buttonBarBox.getChildren().add(saveButton);

        root.getChildren().add(buttonBarBox);
    }

    private void setUpMenuBar() {
        //file menu

        newItem.setOnAction(event -> MasterController.newThing());
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(event -> MasterController.openJSONFileIntoDatabase());
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        saveItem.setDisable(true);
        saveItem.setOnAction(event -> MasterController.saveCurrentTab());
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveAsItem.setDisable(true);
        saveAsItem.setOnAction(event -> MasterController.saveCurrentTabAs());

        saveAllItem.setDisable(true);
        saveAllItem.setOnAction(event -> MasterController.saveAllTabs());
        fileMenu.getItems().addAll(newItem, openItem, saveItem, saveAsItem, saveAllItem);


        dataTransferItem.setOnAction(event -> dataTransferItem.setText(MasterController.toggleDataTransferServer()));

        jsonImportItem.setOnAction(event -> MasterController.addJSONFile());

        csvImportItem.setOnAction(event -> MasterController.addCSVFile());

        validateItem.setOnAction(event -> MasterController.validateDatabase());

        mergeDatabaseItem.setOnAction(event -> MasterController.mergeDatabases());
        dataMenu.getItems().addAll(dataTransferItem, jsonImportItem, csvImportItem, validateItem, mergeDatabaseItem);
        setEnablingForDataMenu(false);//these need a database tab content tab to be open

        //tools menu


        autoHeatmapItem.setDisable(true);//:(

        findTrapsItem.setOnAction(event -> MasterController.findTraps());

        stratScoutingScheduleButton.setOnAction(event -> MasterController.genenerateStratScoutingSchedule());
        toolsMenu.getItems().addAll(autoHeatmapItem, findTrapsItem, stratScoutingScheduleButton);


        //help menu

        helpItem.setOnAction(event -> MasterController.openDocumentation());

        openEmotionalSupportItem.setOnAction(event -> MasterController.rickrollUser());
        helpMenu.getItems().addAll(helpItem, openEmotionalSupportItem);
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, dataMenu, toolsMenu, helpMenu);

        root.getChildren().add(menuBar);
    }

    public static void setEnablingForDataMenu(boolean val) {
        dataTransferItem.setDisable(!val);
        jsonImportItem.setDisable(!val);
        csvImportItem.setDisable(!val);
        validateItem.setDisable(!val);
        mergeDatabaseItem.setDisable(!val);
    }

    public static void setEnablingForSaveButtons(boolean val) {
        saveItem.setDisable(!val);
        saveAsItem.setDisable(!val);
        saveAllItem.setDisable(!val);
        saveButton.setDisable(!val);
    }


    public static void main(String[] args) {
        launch();
    }

}
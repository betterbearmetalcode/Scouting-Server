package org.tahomarobotics.scouting.scoutingserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.tahomarobotics.scouting.scoutingserver.controller.QRScannerController;
import org.tahomarobotics.scouting.scoutingserver.util.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;

import java.io.File;
import java.sql.SQLException;

public class ScoutingServer extends Application {

    public enum SCENES {
        MAIN_MENU,
        QR_SCANNER,
        API_INTERACTION,
        DATA_SCENE
    }

    public static SCENES currentScene;
    public static  Scene mainScene;

    public static AnchorPane mainHamburgerMenu;
    public static AnchorPane qrHamburgerMenu;

    public static AnchorPane dataHamburgerMenu;

    public static Scene dataCollectionScene;

    public static Scene dataScene;

    public static Stage mainStage;


    public static QRScannerController qrScannerController = new QRScannerController();
    @Override
    public void start(Stage stage) {
        mainStage = stage;
        mainStage.setTitle("Scouting Server");
        mainStage.setScene(mainScene);
        currentScene = SCENES.MAIN_MENU;
        mainStage.show();

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

        //set up database
        try {
            DatabaseManager.initialize(Constants.DATABASE_FILEPATH + Constants.SQL_DATABASE_NAME);
            DatabaseManager.addTable(Constants.DEFAULT_SQL_TABLE_NAME, DatabaseManager.createTableSchem(Constants.RAW_TABLE_SCHEMA));
        } catch (SQLException e) {
            Logging.logError(e);
        }



    }

    public static void setCurrentScene(Scene scene) {
        mainStage.setScene(scene);
    }

    @Override
    public void init() throws Exception {

        FXMLLoader mainLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/main-scene.fxml").toURI().toURL());
        mainScene = new Scene(mainLoader.load());

        FXMLLoader hamburgerLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
        mainHamburgerMenu = new AnchorPane((AnchorPane) hamburgerLoader.load());

        FXMLLoader dataCollectionHamburgerLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
        qrHamburgerMenu = new AnchorPane((AnchorPane) dataCollectionHamburgerLoader.load());
        FXMLLoader dataCollectionLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/data-collection.fxml").toURI().toURL());
        dataCollectionScene = new Scene(dataCollectionLoader.load());

        FXMLLoader dataLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/data-scene.fxml").toURI().toURL());
        dataScene = new Scene(dataLoader.load());

        FXMLLoader dataHamburgerLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
        dataHamburgerMenu = new AnchorPane((AnchorPane) dataHamburgerLoader.load());

       setUpQRScannerScene();

        setUpDataScene();


        setUpMainScene();


    }

    private void setUpDataScene() {

        VBox parent = (VBox) dataScene.getRoot();
        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane anchorPane = (AnchorPane) splitPane.getItems().get(0);
        anchorPane.getChildren().add(dataHamburgerMenu);
    }
    private void setUpMainScene() {

        VBox parent = (VBox) mainScene.getRoot();
        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane anchorPane = (AnchorPane) splitPane.getItems().get(0);
        anchorPane.getChildren().add(mainHamburgerMenu);

    }


    private void setUpQRScannerScene() {


        //add hamburger menu to qr scanner scene
        VBox parent = (VBox) dataCollectionScene.getRoot();
        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane anchorPane = (AnchorPane) splitPane.getItems().get(0);
        anchorPane.getChildren().add(qrHamburgerMenu);


    }

    public static void main(String[] args) {
        launch();
    }
}
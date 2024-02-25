package org.tahomarobotics.scouting.scoutingserver;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.tahomarobotics.scouting.scoutingserver.controller.QRScannerController;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;

import java.io.File;
import java.sql.SQLException;
import java.util.function.Consumer;

public class ScoutingServer extends Application {

    public enum SCENES {
        MAIN_MENU,
        QR_SCANNER,
        DATA_CORRECTION,
        DATA_SCENE
    }

    public static SCENES currentScene;
    public static Scene mainScene;

    public static AnchorPane mainHamburgerMenu;
    public static AnchorPane qrHamburgerMenu;

    public static AnchorPane dataHamburgerMenu;

    public static AnchorPane dataCorrectionHamburgerMenu;

    public static Scene dataCorrectionScene;

    public static Scene dataCollectionScene;

    public static Scene dataScene;

    public static Stage mainStage;
    VBox root;




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
            SQLUtil.initialize(Constants.DATABASE_FILEPATH + Constants.SQL_DATABASE_NAME);
            SQLUtil.addTable(Constants.DEFAULT_SQL_TABLE_NAME, SQLUtil.createTableSchem(Constants.RAW_TABLE_SCHEMA));
        } catch (SQLException e) {
            Logging.logError(e);
        }

        mainStage.widthProperty().addListener((observable, oldValue, newValue) -> {Constants.APP_WIDTH = newValue.doubleValue(); resize(Constants.APP_WIDTH, Constants.APP_HEIGHT); });
        mainStage.heightProperty().addListener((observable, oldValue, newValue) -> {Constants.APP_HEIGHT = newValue.doubleValue();resize(Constants.APP_WIDTH, Constants.APP_HEIGHT); });


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


        FXMLLoader dataCorrectionHamburgerLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
        dataCorrectionHamburgerMenu = new AnchorPane((AnchorPane) dataCorrectionHamburgerLoader.load());

        FXMLLoader dataCorrectionLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/data-correction-scene.fxml").toURI().toURL());
        dataCorrectionScene = new Scene(dataCorrectionLoader.load());

        setUpQRScannerScene();

        setUpDataScene();


        setUpMainScene(Constants.APP_WIDTH, Constants.APP_HEIGHT);

        setUpDataCorrectionScene();
        resize(Constants.APP_WIDTH, Constants.APP_HEIGHT);


    }

    private void setUpDataScene() {

        VBox parent = (VBox) dataScene.getRoot();
        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane anchorPane = (AnchorPane) splitPane.getItems().get(0);
        anchorPane.getChildren().add(dataHamburgerMenu);
    }

    private void setUpMainScene(double appWidth, double appHeight) {


        VBox parent = (VBox) mainScene.getRoot();
        root = parent;
        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane anchorPane = (AnchorPane) splitPane.getItems().get(0);
        anchorPane.getChildren().add(mainHamburgerMenu);

        root.setPrefSize(appWidth, appHeight);
        SplitPane mainSplitPane = (SplitPane) root.getChildren().get(0);
        mainSplitPane.prefHeightProperty().bind(root.heightProperty());
        mainSplitPane.prefWidthProperty().bind(root.widthProperty());


        AnchorPane menuPane = (AnchorPane) mainSplitPane.getItems().get(0);
        menuPane.prefHeightProperty().bind(root.heightProperty());
        mainHamburgerMenu.prefHeightProperty().bind(root.heightProperty());
        mainHamburgerMenu.setMinWidth(Constants.MIN_HAMBURGER_MENU_SIZE);
        mainHamburgerMenu.setMaxWidth(mainHamburgerMenu.getMinWidth());
       menuPane.prefWidthProperty().bind(mainHamburgerMenu.widthProperty());
       menuPane.setMinWidth(mainHamburgerMenu.getMinWidth());
        menuPane.setMaxWidth(mainHamburgerMenu.getMaxWidth());
        GridPane mainGridPane = (GridPane) mainSplitPane.getItems().get(1);
        mainGridPane.prefHeightProperty().bind(root.heightProperty());
        mainGridPane.prefWidthProperty().bind(new ObservableValue<>() {
            @Override
            public void addListener(ChangeListener<? super Number> listener) {
            }

            @Override
            public void removeListener(ChangeListener<? super Number> listener) {

            }

            @Override
            public Number getValue() {
                return root.widthProperty().doubleValue() - mainHamburgerMenu.prefWidthProperty().doubleValue();
            }

            @Override
            public void addListener(InvalidationListener listener) {

            }

            @Override
            public void removeListener(InvalidationListener listener) {

            }
        });
        ScrollPane scollPane = (ScrollPane) mainGridPane.getChildren().get(0);
        scollPane.setPrefSize(scollPane.getPrefWidth(), appHeight - 100);


    }


    private void setUpQRScannerScene() {


        //add hamburger menu to qr scanner scene
        VBox parent = (VBox) dataCollectionScene.getRoot();
        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane anchorPane = (AnchorPane) splitPane.getItems().get(0);
        anchorPane.getChildren().add(qrHamburgerMenu);


    }

    private void setUpDataCorrectionScene() {
        //add hamburger menu to qr scanner scene
        VBox parent = (VBox) dataCorrectionScene.getRoot();
        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane anchorPane = (AnchorPane) splitPane.getItems().get(0);
        anchorPane.getChildren().add(dataCorrectionHamburgerMenu);
    }

    public static void main(String[] args) {
        launch();
    }


    private void resize(double appWidth, double appHeight) {
            //main scene
        root.setPrefSize(appWidth, appHeight);
        SplitPane mainSplitPane = (SplitPane) root.getChildren().get(0);
        mainSplitPane.setDividerPosition(0, mainHamburgerMenu.getMinWidth()/root.getWidth());
    }

}
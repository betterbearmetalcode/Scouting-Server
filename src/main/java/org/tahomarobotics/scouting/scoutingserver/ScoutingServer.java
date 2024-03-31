package org.tahomarobotics.scouting.scoutingserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.tahomarobotics.scouting.scoutingserver.controller.QRScannerController;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.ServerUtil;

import java.awt.*;
import java.io.File;
import java.io.PipedReader;
import java.sql.SQLException;

import static org.tahomarobotics.scouting.scoutingserver.Constants.UIValues.*;

public class ScoutingServer extends Application {

    public enum SCENES {
        MAIN_MENU,
        QR_SCANNER,
        DATA_CORRECTION,
        DATA_SCENE,
        CHARTS
    }

    public static SCENES currentScene;
    public static Scene mainScene;

    public static GridPane mainHamburgerMenu;
    public static GridPane qrHamburgerMenu;

    public static GridPane dataHamburgerMenu;

    public static GridPane chartHamburgerMenu;

    public static Scene dataCorrectionScene;

    public static Scene dataCollectionScene;

    public static Scene dataScene;

    public static Stage mainStage;

    public static Scene chartsScene;
    static StackPane mainRoot;
    static StackPane dataCollectionRoot;
    static StackPane dataRoot;

    static StackPane chartsRoot;

    private static final StackPane actualRoot = new StackPane();

    public static QRScannerController qrScannerController = new QRScannerController();

    @Override
    public void start(Stage stage) {
        mainStage = stage;
        mainStage.setTitle("Scouting Server");
        //actualRoot.getChildren().add(dataCollectionScene.getRoot());
        mainStage.setScene(dataCollectionScene);
        currentScene = SCENES.QR_SCANNER;
        mainStage.getIcons().add(new Image(Constants.BASE_READ_ONLY_FILEPATH + "/resources/Logo.png"));
        mainStage.setOnCloseRequest(event -> {
            ServerUtil.setServerStatus(false);
        });
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

        File duplicateDataFilepath = new File(Constants.BASE_APP_DATA_FILEPATH + "/resources/duplicateDataBackups");
        if (!duplicateDataFilepath.exists()) {
            duplicateDataFilepath.mkdirs();
        }

        //set up database
        try {
            SQLUtil.initialize(Constants.DATABASE_FILEPATH + Constants.SQL_DATABASE_NAME);
        } catch (SQLException e) {
            Logging.logError(e);
        }

        mainStage.widthProperty().addListener((observable, oldValue, newValue) -> {setAppWidthProperty(newValue.doubleValue()); resize(); });//added becuase it makes the stuff actually resize and does  not cause problems
        mainStage.heightProperty().addListener((observable, oldValue, newValue) -> {setAppHeight(newValue.doubleValue());resize(); });


    }

    public static void setCurrentScene(Scene scene) {
        double oldWidth = getAppWidth();
        double oldHeight = getAppHeight();
        mainStage.setScene(scene);
        //mainStage.setWidth(oldWidth);
        //mainStage.setHeight(oldHeight);
        //actualRoot.getChildren().clear();
       // actualRoot.getChildren().add(scene.getRoot());
        resize();

    }

    @Override
    public void init() throws Exception {
        setAppWidthProperty(Toolkit.getDefaultToolkit().getScreenSize().width * WIDTH_MULTIPLIER);
        setAppHeight(Toolkit.getDefaultToolkit().getScreenSize().height * HEIGHT_MULTIPLIER);
        FXMLLoader mainLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/main-scene.fxml").toURI().toURL());
        mainScene = new Scene(mainLoader.load());

        FXMLLoader hamburgerLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
        mainHamburgerMenu = hamburgerLoader.load();

        FXMLLoader dataCollectionHamburgerLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
        qrHamburgerMenu = dataCollectionHamburgerLoader.load();
        FXMLLoader dataCollectionLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/data-collection.fxml").toURI().toURL());
        dataCollectionScene = new Scene(dataCollectionLoader.load());

        FXMLLoader dataLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/data-scene.fxml").toURI().toURL());
        dataScene = new Scene(dataLoader.load());

        FXMLLoader dataHamburgerLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
        dataHamburgerMenu = dataHamburgerLoader.load();

        FXMLLoader chartLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/chart-scene.fxml").toURI().toURL());
        chartsScene = new Scene(chartLoader.load());

        FXMLLoader chartsHamburgerMenuLoader = new FXMLLoader(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
        chartHamburgerMenu = chartsHamburgerMenuLoader.load();

        setUpMainScene();
        setUpQRScannerScene();

        setUpDataScene();

        setUpChartsScene();

        resize();

    }




    public static void main(String[] args) {
        launch();
    }


    private static void resize() {
        resize(getAppWidth(), getAppHeight());

    }

    private static void resize(double appWidth, double appHeight) {
        mainRoot.setPrefSize(appWidth, appHeight);
        dataCollectionRoot.setPrefSize(appWidth, appHeight);
        dataRoot.setPrefSize(appWidth, appHeight);
        Constants.UIValues.setSplitWidthProperty(appWidth - MIN_HAMBURGER_MENU_SIZE);
        Constants.UIValues.setMainScrollPaneHeightProperty(appHeight - MIN_MAIN_BUTTON_BAR_HEIGHT);
        Constants.UIValues.setCollectionScrollPaneHeightProperty(appHeight - MIN_MAIN_BUTTON_BAR_HEIGHT - MIN_MAIN_BUTTON_BAR_HEIGHT - MIN_MAIN_BUTTON_BAR_HEIGHT);
        Constants.UIValues.setDatabaseHeightProperty(appHeight- MIN_MAIN_BUTTON_BAR_HEIGHT - MIN_MAIN_BUTTON_BAR_HEIGHT - MIN_MAIN_BUTTON_BAR_HEIGHT - MIN_MAIN_BUTTON_BAR_HEIGHT);
        SplitPane mainSplitPane = (SplitPane) mainRoot.getChildren().get(0);
        mainSplitPane.setDividerPosition(0, MIN_HAMBURGER_MENU_SIZE/appWidth);
        mainRoot.resize(appWidth, appHeight);
        dataCollectionRoot.resize(appWidth, appHeight);
        dataRoot.resize(appWidth, appHeight);
        chartsRoot.resize(appWidth, appHeight);
    }

    private void setUpDataScene() {



        double appWidth = getAppWidth();
        double appHeight = getAppHeight();

        StackPane parent = (StackPane) dataScene.getRoot();
        dataRoot = parent;

        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane menuPane = (AnchorPane) splitPane.getItems().get(0);
        menuPane.getChildren().add(dataHamburgerMenu);


        parent.setPrefSize(appWidth, appHeight);
        splitPane.prefHeightProperty().bind(dataRoot.heightProperty());
        splitPane.prefWidthProperty().bind(dataRoot.widthProperty());
        menuPane.prefHeightProperty().bind(dataRoot.heightProperty());
        dataHamburgerMenu.prefHeightProperty().bind(dataRoot.heightProperty());
        dataHamburgerMenu.setMinWidth(MIN_HAMBURGER_MENU_SIZE);
        dataHamburgerMenu.setMaxWidth(dataHamburgerMenu.getMinWidth());
        menuPane.prefWidthProperty().bind(dataHamburgerMenu.widthProperty());
        menuPane.setMinWidth(dataHamburgerMenu.getMinWidth());
        menuPane.setMaxWidth(dataHamburgerMenu.getMaxWidth());

        VBox mainVbox = (VBox) splitPane.getItems().get(1);
        mainVbox.prefHeightProperty().bind(dataRoot.heightProperty());
        mainVbox.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());

        TabPane tabPane = (TabPane) mainVbox.getChildren().get(1);
        tabPane.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
        tabPane.prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
    }

    private void setUpMainScene() {

        double appWidth = getAppWidth();
        double appHeight = getAppHeight();
        StackPane parent = (StackPane) mainScene.getRoot();
        mainRoot = parent;

        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane anchorPane = (AnchorPane) splitPane.getItems().get(0);
        anchorPane.getChildren().add(mainHamburgerMenu);

        mainRoot.setPrefSize(appWidth, appHeight);
        SplitPane mainSplitPane = (SplitPane) mainRoot.getChildren().get(0);
        mainSplitPane.prefHeightProperty().bind(mainRoot.heightProperty());
        mainSplitPane.prefWidthProperty().bind(mainRoot.widthProperty());


        AnchorPane menuPane = (AnchorPane) mainSplitPane.getItems().get(0);
        menuPane.prefHeightProperty().bind(mainRoot.heightProperty());
        mainHamburgerMenu.prefHeightProperty().bind(mainRoot.heightProperty());
        mainHamburgerMenu.setMinWidth(MIN_HAMBURGER_MENU_SIZE);
        mainHamburgerMenu.setMaxWidth(mainHamburgerMenu.getMinWidth());
        menuPane.prefWidthProperty().bind(mainHamburgerMenu.widthProperty());
        menuPane.setMinWidth(mainHamburgerMenu.getMinWidth());
        menuPane.setMaxWidth(mainHamburgerMenu.getMaxWidth());
        GridPane mainGridPane = (GridPane) mainSplitPane.getItems().get(1);
        mainGridPane.prefHeightProperty().bind(mainRoot.heightProperty());
        mainGridPane.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
        ScrollPane scollPane = (ScrollPane) mainGridPane.getChildren().get(0);
        scollPane.prefHeightProperty().bind(Constants.UIValues.mainScrollPaneHeightProperty());
        TextArea textArea = (TextArea) scollPane.getContent();
        textArea.prefHeightProperty().bind(Constants.UIValues.mainScrollPaneHeightProperty());
        textArea.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
        FlowPane flowPane = (FlowPane) mainGridPane.getChildren().get(1);
        flowPane.setMinHeight(MIN_MAIN_BUTTON_BAR_HEIGHT);
        flowPane.setMaxHeight(MIN_MAIN_BUTTON_BAR_HEIGHT);



    }

    private void setUpChartsScene() {

        double appWidth = getAppWidth();
        double appHeight = getAppHeight();


        StackPane parent = (StackPane) chartsScene.getRoot();
        chartsRoot = parent;

        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane menuPane = (AnchorPane) splitPane.getItems().get(0);
        menuPane.getChildren().add(chartHamburgerMenu);


        parent.setPrefSize(appWidth, appHeight);
        splitPane.prefHeightProperty().bind(chartsRoot.heightProperty());
        splitPane.prefWidthProperty().bind(chartsRoot.widthProperty());
        menuPane.prefHeightProperty().bind(chartsRoot.heightProperty());
        chartHamburgerMenu.prefHeightProperty().bind(chartsRoot.heightProperty());
        chartHamburgerMenu.setMinWidth(MIN_HAMBURGER_MENU_SIZE);
        chartHamburgerMenu.setMaxWidth(chartHamburgerMenu.getMinWidth());
        menuPane.prefWidthProperty().bind(chartHamburgerMenu.widthProperty());
        menuPane.setMinWidth(chartHamburgerMenu.getMinWidth());
        menuPane.setMaxWidth(chartHamburgerMenu.getMaxWidth());

        VBox mainVbox = (VBox) splitPane.getItems().get(1);
        mainVbox.prefHeightProperty().bind(chartsRoot.heightProperty());
        mainVbox.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());

        TabPane tabPane = (TabPane) mainVbox.getChildren().get(0);
        tabPane.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
        tabPane.prefHeightProperty().bind(Constants.UIValues.appHeightProperty());


    }


    private static  void setUpQRScannerScene() {
        double appWidth = getAppWidth();
        double appHeight = getAppHeight();

        //add hamburger menu to qr scanner scene
        StackPane parent = (StackPane) dataCollectionScene.getRoot();
        dataCollectionRoot = parent;

        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane menuPane = (AnchorPane) splitPane.getItems().get(0);
        menuPane.getChildren().add(qrHamburgerMenu);

        parent.setPrefSize(appWidth, appHeight);
        splitPane.prefHeightProperty().bind(dataCollectionRoot.heightProperty());
        splitPane.prefWidthProperty().bind(dataCollectionRoot.widthProperty());
        menuPane.prefHeightProperty().bind(dataCollectionRoot.heightProperty());
        qrHamburgerMenu.prefHeightProperty().bind(dataCollectionRoot.heightProperty());
        qrHamburgerMenu.setMinWidth(MIN_HAMBURGER_MENU_SIZE);
        qrHamburgerMenu.setMaxWidth(qrHamburgerMenu.getMinWidth());
        menuPane.prefWidthProperty().bind(qrHamburgerMenu.widthProperty());
        menuPane.setMinWidth(qrHamburgerMenu.getMinWidth());
        menuPane.setMaxWidth(qrHamburgerMenu.getMaxWidth());
        VBox mainVbox = (VBox) splitPane.getItems().get(1);
        mainVbox.prefHeightProperty().bind(dataCollectionRoot.heightProperty());
        mainVbox.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());

        HBox topHbox = (HBox) mainVbox.getChildren().get(0);
        topHbox.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
        topHbox.setMaxHeight(MIN_MAIN_BUTTON_BAR_HEIGHT);
        topHbox.setMinHeight(MIN_MAIN_BUTTON_BAR_HEIGHT);
        ScrollPane scrollPane = (ScrollPane) mainVbox.getChildren().get(2);
        scrollPane.prefHeightProperty().bind(Constants.UIValues.dataCollectionScrollPaneHeightProperty());
        scrollPane.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
        VBox labelVbox = (VBox) scrollPane.getContent();
        labelVbox.prefHeightProperty().bind(Constants.UIValues.dataCollectionScrollPaneHeightProperty());
        labelVbox.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
        HBox lowerHBox = (HBox) mainVbox.getChildren().get(4);
        lowerHBox.setMinHeight(MIN_MAIN_BUTTON_BAR_HEIGHT);
        lowerHBox.setMaxHeight(MIN_MAIN_BUTTON_BAR_HEIGHT);

    }


}
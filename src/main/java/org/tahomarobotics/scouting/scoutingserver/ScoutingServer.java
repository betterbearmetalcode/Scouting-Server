package org.tahomarobotics.scouting.scoutingserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.tahomarobotics.scouting.scoutingserver.controller.QRScannerController;

import java.io.File;
import java.net.URL;

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

    public static Scene qrScannerScene;

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



    }

    public static void setCurrentScene(Scene scene) {
        mainStage.setScene(scene);
    }

    @Override
    public void init() throws Exception {

        FXMLLoader mainLoader = new FXMLLoader(new File(System.getProperty("user.dir") + "/resources/FXML/main-scene.fxml").toURI().toURL());
        mainScene = new Scene(mainLoader.load());

        FXMLLoader hamburgerLoader = new FXMLLoader(new File(System.getProperty("user.dir") + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
        mainHamburgerMenu = new AnchorPane((AnchorPane) hamburgerLoader.load());

        FXMLLoader qrhamburgerLoader = new FXMLLoader(new File(System.getProperty("user.dir") + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
        qrHamburgerMenu = new AnchorPane((AnchorPane) qrhamburgerLoader.load());
        FXMLLoader qrscannerLoader = new FXMLLoader(new File(System.getProperty("user.dir") + "/resources/FXML/qr-scanner-scene.fxml").toURI().toURL());
        qrScannerScene = new Scene(qrscannerLoader.load());

        FXMLLoader dataLoader = new FXMLLoader(new File(System.getProperty("user.dir") + "/resources/FXML/data-scene.fxml").toURI().toURL());
        dataScene = new Scene(dataLoader.load());

        FXMLLoader dataHamburgerLoader = new FXMLLoader(new File(System.getProperty("user.dir") + "/resources/FXML/hamburger-menu.fxml").toURI().toURL());
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
        VBox parent = (VBox) qrScannerScene.getRoot();
        SplitPane splitPane = (SplitPane) parent.getChildren().get(0);
        AnchorPane anchorPane = (AnchorPane) splitPane.getItems().get(0);
        anchorPane.getChildren().add(qrHamburgerMenu);


    }

    public static void main(String[] args) {
        launch();
    }
}
package org.tahomarobotics.scouting.scoutingserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.tahomarobotics.scouting.scoutingserver.controller.QRScannerController;

public class ScoutingServer extends Application {

    public enum SCENES {
        MAIN_MENU,
        QR_SCANNER,
        API_INTERACTION,
        DATA_MANIPULATION
    }

    public static SCENES currentScene;
    public static  Scene mainScene;

    public static AnchorPane mainHamburgerMenu;
    public static AnchorPane qrHamburgerMenu;

    public static Scene qrScannerScene;

    protected static Stage mainStage;

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

        FXMLLoader mainLoader = new FXMLLoader(ScoutingServer.class.getResource("FXML/main-scene.fxml"));
        mainScene = new Scene(mainLoader.load());



        FXMLLoader hamburgerLoader = new FXMLLoader(ScoutingServer.class.getResource("FXML/hamburger-menu.fxml"));
        mainHamburgerMenu = new AnchorPane((AnchorPane) hamburgerLoader.load());

        FXMLLoader qrhamburgerLoader = new FXMLLoader(ScoutingServer.class.getResource("FXML/hamburger-menu.fxml"));
        qrHamburgerMenu = new AnchorPane((AnchorPane) qrhamburgerLoader.load());
        FXMLLoader qrscannerLoader = new FXMLLoader(ScoutingServer.class.getResource("FXML/qr-scanner-scene.fxml"));
        qrScannerScene = new Scene(qrscannerLoader.load());

       setUpQRScannerScene();




        setUpMainScene();


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
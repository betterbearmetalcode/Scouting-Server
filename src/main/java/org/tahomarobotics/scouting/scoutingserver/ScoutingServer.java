package org.tahomarobotics.scouting.scoutingserver;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;

public class ScoutingServer extends Application {



    public static  Scene mainScene;

    public static AnchorPane mainHamburgerMenu;
    public static AnchorPane qrHamburgerMenu;

    public static Scene qrScannerScene;


    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("Scouting Server");
        stage.setScene(mainScene);
        stage.show();


    }


    @Override
    public void init() throws Exception {

        FXMLLoader mainLoader = new FXMLLoader(ScoutingServer.class.getResource("main-scene.fxml"));
        mainScene = new Scene(mainLoader.load());

        FXMLLoader hamburgerLoader = new FXMLLoader(ScoutingServer.class.getResource("hamburger-menu.fxml"));
        mainHamburgerMenu = new AnchorPane((AnchorPane) hamburgerLoader.load());

        FXMLLoader qrhamburgerLoader = new FXMLLoader(ScoutingServer.class.getResource("hamburger-menu.fxml"));
        qrHamburgerMenu = new AnchorPane((AnchorPane) qrhamburgerLoader.load());
        FXMLLoader qrscannerLoader = new FXMLLoader(ScoutingServer.class.getResource("qr-scanner-scene.fxml"));
        qrScannerScene = new Scene(qrscannerLoader.load());

        ((Pane) mainScene.getRoot()).getChildren().add(mainHamburgerMenu);
        ((Pane) qrScannerScene.getRoot()).getChildren().add(qrHamburgerMenu);



    }


    public static void main(String[] args) {
        launch();
    }
}
package org.tahomarobotics.scouting.scoutingserver;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.converter.IntegerStringConverter;
import org.tahomarobotics.scouting.scoutingserver.util.WebcamCapture;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class QRScannerController extends VBox {


    //fxml variables
    @FXML
    public Button takePictureButton = new Button();
    @FXML
    public ComboBox<String> selectCameraComboBox = new ComboBox<>();
    @FXML
    public TextField delayField = new TextField();

    public CheckBox previewCheckbox = new CheckBox();

    @FXML
    public void cameraSelectorClicked(ActionEvent event) {
        WebcamCapture.setSelectedWebcam(selectCameraComboBox.getValue());
        takePictureButton.setDisable(false);
    }
    @FXML
    public void updateCameraList(ActionEvent event) {
        try {
            ArrayList<String> devices = WebcamCapture.getDevices();
            ObservableList<String> arr = FXCollections.observableArrayList(devices);
            selectCameraComboBox.setItems(arr);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    //consider this https://www.tutorialspoint.com/java_mysql/java_mysql_quick_guide.html
    @FXML
    public void takePicture(ActionEvent event) {
    System.out.println("Taking picture on camera: " + WebcamCapture.getSelectedWebcam());
    int delay = Integer.parseInt(delayField.getText().replaceAll("[^0-9]", ""));
        try {
            WebcamCapture.snapshotWebcam(selectCameraComboBox.getValue(), previewCheckbox.isSelected(), delay);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }





}

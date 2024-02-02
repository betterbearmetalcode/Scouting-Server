package org.tahomarobotics.scouting.scoutingserver;

import com.google.zxing.NotFoundException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.util.QRCodeUtil;
import org.tahomarobotics.scouting.scoutingserver.util.WebcamCapture;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class QRScannerController  {


    //fxml variables
    @FXML
    public Button takePictureButton;
    @FXML
    public ComboBox<String> selectCameraComboBox;
    @FXML
    public TextField delayField;

    public CheckBox previewCheckbox;

    public ImageView imageView;

    @FXML
    public VBox imageViewBox;


    @FXML
    private void initialize() {
        try {
            ArrayList<String> devices = WebcamCapture.getDevices();
            ObservableList<String> arr = FXCollections.observableArrayList(devices);
            selectCameraComboBox.setItems(arr);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void cameraSelectorClicked(ActionEvent event) {
        WebcamCapture.setSelectedWebcam(selectCameraComboBox.getValue());
        takePictureButton.setDisable(false);
    }



    //consider this https://www.tutorialspoint.com/java_mysql/java_mysql_quick_guide.html
    @FXML
    public void takePicture(ActionEvent event) {
        System.out.println("Taking picture on camera: " + WebcamCapture.getSelectedWebcam());
        int delay = 0;
        try {
            delay = Integer.parseInt(delayField.getText().replaceAll("[^0-9]", ""));
        }catch (Exception e) {
            delay = 1000;//just in case the user screws things up, set a default delay
        }


        String filePath = QRCodeUtil.iamgeDataFilepath + System.currentTimeMillis()  +".bmp";
        try {

            WebcamCapture.snapshotWebcam(selectCameraComboBox.getValue(), previewCheckbox.isSelected(), delay, filePath);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        String qrData;
        try {

            //if we have got this far in the code, than the iamge has succesfully be written to the disk
            FileInputStream input = new FileInputStream(filePath);
            Image image = new Image(input);
            imageView.setImage(image);
            qrData = QRCodeUtil.readQRCode(filePath);
            input.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NotFoundException e) {
            qrData = "";
        }
        QRCodeUtil.qrData.add(qrData);
        System.out.println("Scanner QR Code: " + qrData);

    }





}

package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.*;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class QRScannerController  {
    private static String activeTable = Constants.DEFAULT_SQL_TABLE_NAME;

    //fxml variables
    @FXML
    public Button takePictureButton;
    @FXML
    public ComboBox<String> selectCameraComboBox;
    @FXML
    public TextField delayField;

    @FXML
    public Label selectedDatabaseLabel;

    public CheckBox previewCheckbox;

    public ImageView imageView;

    @FXML
    public VBox imageViewBox;


    @FXML
    private void initialize() {
        try {
            ArrayList<String> devices = WebcamUtil.getDevices();
            ObservableList<String> arr = FXCollections.observableArrayList(devices);
            selectCameraComboBox.setItems(arr);
            selectedDatabaseLabel.setText(Constants.DEFAULT_SQL_TABLE_NAME);
        } catch (IOException | InterruptedException e) {
            Logging.logError(e);
        }
    }

    public void selectTargetTable(ActionEvent event) {
        try {
            TableChooserDialog dialog = new TableChooserDialog(DatabaseManager.getTableNames());
            Optional<String> result = dialog.showAndWait();
            AtomicReference<String> selectedTable = new AtomicReference<>("");
            result.ifPresent(selectedDatabaseLabel::setText);
            result.ifPresent(this::setActiveTable);
            result.ifPresent(System.out::println);
        } catch (SQLException e) {
            Logging.logError(e);
        }
    }

    @FXML
    public void cameraSelectorClicked(ActionEvent event) {
        WebcamUtil.setSelectedWebcam(selectCameraComboBox.getValue());
        takePictureButton.setDisable(false);
    }
    @FXML
    public void importJSON(ActionEvent event) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select JSON File");
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            List<File> selectedFile = chooser.showOpenMultipleDialog(ScoutingServer.mainStage.getOwner());
            if (selectedFile != null) {
                for (File file : selectedFile) {
                    if (file.exists()) {
                        FileInputStream inputStream = new FileInputStream(file);
                        DataHandler.storeRawQRData(System.currentTimeMillis(), new JSONArray(new String(inputStream.readAllBytes())), Constants.DEFAULT_SQL_TABLE_NAME);
                        inputStream.close();
                    }

                }
            }


        } catch (IOException e) {
            Logging.logError(e);
        }
    }



    //consider this https://www.tutorialspoint.com/java_mysql/java_mysql_quick_guide.html
    @FXML
    public void takePicture(ActionEvent event) {
        System.out.println("Taking picture on camera: " + WebcamUtil.getSelectedWebcam());
        int delay = 0;
        try {
            delay = Integer.parseInt(delayField.getText().replaceAll("[^0-9]", ""));
        }catch (Exception e) {
            delay = 1000;//just in case the user screws things up, set a default delay
        }

        String filePath = Constants.IMAGE_DATA_FILEPATH + System.currentTimeMillis()  +".bmp";
        try {

            WebcamUtil.snapshotWebcam(selectCameraComboBox.getValue(), previewCheckbox.isSelected(), delay, filePath);
            FileInputStream input = new FileInputStream(filePath);
            Image image = new Image(input);
            imageView.setImage(image);
            input.close();
           readStoredImage(filePath, activeTable);

        } catch (InterruptedException | IOException e) {
            Logging.logError(e);
        } catch (NotFoundException e) {

            System.out.println("Failed to read QR Code");
            Alert aler = new Alert(Alert.AlertType.WARNING, "Failed to read QR Code");
            aler.showAndWait();

        }

    }

    public static void readStoredImage(String fp, String tableName) throws IOException, NotFoundException {
        //if we have got this far in the code, than the iamge has succesfully be written to the disk

        String qrData = QRCodeUtil.readQRCode(fp);
        System.out.println("Scanner QR Code: " + qrData);
        DataHandler.storeRawQRData(System.currentTimeMillis() , qrData, tableName);
    }


    public void setActiveTable(String s)  {
        activeTable = s;
    }



}

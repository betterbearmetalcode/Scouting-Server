package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.*;

import java.io.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

public class QRScannerController {
    public static String activeTable = Constants.DEFAULT_SQL_TABLE_NAME;

    //fxml variables
    @FXML
    public Button takePictureButton;

    private static boolean watchForQrCodes = true;

    @FXML
    public Label selectedDatabaseLabel;


    public ImageView imageView;

    @FXML
    public VBox imageViewBox;

    public static DirectoryWatcher watcher;
    @FXML
    private void initialize() {
        selectedDatabaseLabel.setText(Constants.DEFAULT_SQL_TABLE_NAME);
       watcher = new DirectoryWatcher(Constants.QR_IAMGE_QUERY_LOCATION);
    }

    public void selectTargetTable(ActionEvent event) {
        try {
            TableChooserDialog dialog = new TableChooserDialog(SQLUtil.getTableNames());
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
                        DatabaseManager.storeRawQRData(System.currentTimeMillis(), new JSONArray(new String(inputStream.readAllBytes())), Constants.DEFAULT_SQL_TABLE_NAME);
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
    public void loadScannedQRCodes(ActionEvent event) {
        File dir = new File(Constants.QR_IAMGE_QUERY_LOCATION);
        File[] arr = dir.listFiles(pathname -> (pathname.getName().toLowerCase().endsWith(".png") || pathname.getName().toLowerCase().endsWith(".jpg")));
        if (arr != null) {
            for (File file : arr) {
                try {
                    readStoredImage(Constants.QR_IAMGE_QUERY_LOCATION + file.getName(), activeTable);
                } catch (IOException e) {
                    Logging.logError(e);
                } catch (NotFoundException e) {
                    Logging.logError(e, "Could not read qr code");
                }
            }
        }

    }

    public static void readStoredImage(String fp, String tableName) throws IOException, NotFoundException {
        //if we have got this far in the code, than the iamge has succesfully be written to the disk

        String qrData = QRCodeUtil.readQRCode(fp);
        System.out.println("Scanner QR Code: " + qrData);
        DatabaseManager.storeRawQRData(System.currentTimeMillis(), qrData, tableName);
    }


    public void setActiveTable(String s) {
        activeTable = s;
    }

    private static String execCommand(String command) throws InterruptedException, IOException {
        final Process p = Runtime.getRuntime().exec(command);
        StringBuilder builder = new StringBuilder();
        new Thread(new Runnable() {
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = null;

                try {
                    while ((line = input.readLine()) != null) {
                        builder.append(line);
                    }

                } catch (IOException e) {
                    Logging.logError(e);
                }
            }
        }).start();

        p.waitFor();
        return builder.toString();

    }



}

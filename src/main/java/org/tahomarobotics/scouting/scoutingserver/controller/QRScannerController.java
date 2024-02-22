package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
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
    private static String activeTable = Constants.DEFAULT_SQL_TABLE_NAME;

    //fxml variables
    @FXML
    public Button takePictureButton;

    private static boolean watchForQrCodes = true;

    @FXML
    public Label selectedDatabaseLabel;


    public ImageView imageView;

    @FXML
    public VBox imageViewBox;


    @FXML
    private void initialize() {
        selectedDatabaseLabel.setText(Constants.DEFAULT_SQL_TABLE_NAME);
        registerWatcher(Constants.QR_IAMGE_QUERY_LOCATION);
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


        try {

            FileInputStream input = new FileInputStream(Constants.QR_IAMGE_QUERY_LOCATION);
            Image image = new Image(input);
            imageView.setImage(image);
            input.close();

        } catch (IOException e) {
            Logging.logError(e);
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


    private static void registerWatcher(String absPath) {
        try {
            watchForQrCodes = false;//kill any existing threads
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(new File(absPath).toURI());
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            AtomicReference<WatchKey> watchKey = new AtomicReference<>();
            watchForQrCodes = true;
            Thread thread = new Thread(() -> {

                while (watchForQrCodes) {
                    try {
                        watchKey.set(watchService.take());
                        watchKey.get().pollEvents()
                                .stream()
                                .filter(event -> event.kind() == ENTRY_CREATE)
                                .forEach(QRScannerController::userScannedImage);
                        if (!watchKey.get().reset()) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            thread.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @SuppressWarnings("unchecked")
    private static void userScannedImage(WatchEvent<?> event) {
        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path filename = ev.context();
        try {
            System.out.println("File detected: " + Constants.QR_IAMGE_QUERY_LOCATION + filename);
            readStoredImage(Constants.QR_IAMGE_QUERY_LOCATION + filename, activeTable);

        } catch (IOException ignored) {
           ignored.printStackTrace();
        } catch (NotFoundException e) {
            System.err.println("failed re read qr code");
        }


        System.out.println(Constants.QR_IAMGE_QUERY_LOCATION + filename + " has been created.");
    }


}

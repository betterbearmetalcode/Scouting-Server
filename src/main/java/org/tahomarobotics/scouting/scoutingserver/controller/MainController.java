package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;
import org.tahomarobotics.scouting.scoutingserver.util.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.QRCodeUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;

import java.io.IOException;


public class MainController extends VBox {

    public void debugggy(ActionEvent event) {
       System.out.println("Debug button pressed");
        try {
            QRCodeUtil.createQRCode(Constants.TEST_QR_STRING_1, System.getProperty("user.dir") + "/resources/test1.png" ,null, 500,500);
            QRCodeUtil.createQRCode(Constants.TEST_QR_STRING_2, System.getProperty("user.dir") + "/resources/test2.png" ,null, 500,500);
            QRCodeUtil.createQRCode(Constants.TEST_QR_STRING_3, System.getProperty("user.dir") + "/resources/test3.png" ,null, 500,500);
            QRCodeUtil.createQRCode(Constants.TEST_QR_STRING_4, System.getProperty("user.dir") + "/resources/test4.png" ,null, 500,500);
            QRCodeUtil.createQRCode(Constants.TEST_QR_STRING_5, System.getProperty("user.dir") + "/resources/test5.png" ,null, 500,500);
            QRCodeUtil.createQRCode(Constants.TEST_QR_STRING_6, System.getProperty("user.dir") + "/resources/test6.png" ,null, 500,500);
            QRCodeUtil.createQRCode(Constants.TEST_QR_STRING_7, System.getProperty("user.dir") + "/resources/test7.png" ,null, 500,500);
            QRCodeUtil.createQRCode(Constants.TEST_QR_STRING_8, System.getProperty("user.dir") + "/resources/test8.png" ,null, 500,500);
            QRCodeUtil.createQRCode(Constants.TEST_QR_STRING_9, System.getProperty("user.dir") + "/resources/test9.png" ,null, 500,500);

        } catch (WriterException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
    @FXML
    public void debugButton2(ActionEvent event) {
    }
}
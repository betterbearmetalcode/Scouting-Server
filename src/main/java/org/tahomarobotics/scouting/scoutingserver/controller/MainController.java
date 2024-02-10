package org.tahomarobotics.scouting.scoutingserver.controller;

import com.google.zxing.WriterException;
import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;

;
import org.tahomarobotics.scouting.scoutingserver.DataHandler;
import org.tahomarobotics.scouting.scoutingserver.util.QRCodeUtil;

import java.io.IOException;


public class MainController extends VBox {

    public void debugggy(ActionEvent event) {
       System.out.println("Debug button pressed");

        try {
            //System.out.println("Locally Cached QR Data: " + DataHandler.getCachedQRData());
            QRCodeUtil.createQRCode("5/2046/1/2/1/7/0/1/3/0/2046Rah!/code3", "C:\\Users\\Caleb\\IdeaProjects\\ScoutingServer\\src\\main\\resources\\org\\tahomarobotics\\scouting\\scoutingserver\\generatedQRCode.jpg", null,1000,1000);
            System.out.println("Successfullly created qrcode for funsies");
        } catch (WriterException | IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("QR data Cached in RAM" + DataHandler.getMatchData());
    }
}
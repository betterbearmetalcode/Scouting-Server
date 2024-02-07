package org.tahomarobotics.scouting.scoutingserver;

import com.google.zxing.WriterException;
import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.util.QRCodeUtil;
import org.tahomarobotics.scouting.scoutingserver.util.WebcamCapture;

import java.io.IOException;


public class MainController extends VBox {








    public void debugggy(ActionEvent event) {
       System.out.println("Debug button pressed");
        System.out.println(QRCodeUtil.getCachedQRData());
        try {
            QRCodeUtil.createQRCode("5,2046,1,2,1,7,0,1,3,0,2046Rah!,theseAreTEleNotes", "C:\\Users\\Caleb\\IdeaProjects\\ScoutingServer\\src\\main\\resources\\org\\tahomarobotics\\scouting\\scoutingserver\\generatedQRCode.jpg", null,1000,1000);
        } catch (WriterException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
package org.tahomarobotics.scouting.scoutingserver;

import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.util.WebcamCapture;


public class MainController extends VBox {





   protected void getTBAData(ActionEvent event) {
       //System.out.println("Attempting to fetch TBA Data");
       try {
           //JSONArray array = APInteraction.get("/event/2023vapor/matches");

          // System.out.println(array);
          // System.out.println("Finished fetching data");
           WebcamCapture.snapshotWebcam(WebcamCapture.getDevices().get(1));
           //System.out.println("Text in QR Code is: " + QRCodeReader.readQRCode("C:\\Users\\Caleb\\IdeaProjects\\ScoutingServer\\src\\main\\resources\\org\\tahomarobotics\\scouting\\scoutingserver\\harderQRCode.jpg"));
       }catch (Exception e) {
           e.printStackTrace();
       }
        System.out.println("Finished");

   }


    public void debugggy(ActionEvent event) {
       System.out.println("Debug button pressed");
    }
}
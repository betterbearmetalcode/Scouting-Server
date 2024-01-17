module org.tahomarobotics.scouting.scoutingserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires org.json;
    requires java.desktop;
    requires com.google.zxing;
    requires com.google.zxing.javase;



    opens org.tahomarobotics.scouting.scoutingserver to javafx.fxml;
    exports org.tahomarobotics.scouting.scoutingserver;
    exports org.tahomarobotics.scouting.scoutingserver.util;
    opens org.tahomarobotics.scouting.scoutingserver.util to javafx.fxml;
}
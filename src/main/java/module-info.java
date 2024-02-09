module org.tahomarobotics.scouting.scoutingserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;
    requires java.net.http;
    requires java.desktop;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires java.logging;
    requires java.sql;


    opens org.tahomarobotics.scouting.scoutingserver to javafx.fxml;
    exports org.tahomarobotics.scouting.scoutingserver;
    exports org.tahomarobotics.scouting.scoutingserver.util;
    opens org.tahomarobotics.scouting.scoutingserver.util to javafx.fxml;
}
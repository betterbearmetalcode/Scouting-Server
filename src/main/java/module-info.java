module org.tahomarobotics.scouting.scoutingserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;
    requires java.net.http;


    opens org.tahomarobotics.scouting.scoutingserver to javafx.fxml;
    exports org.tahomarobotics.scouting.scoutingserver;
}
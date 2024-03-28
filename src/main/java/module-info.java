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
    requires org.apache.commons.compress;
    requires com.fasterxml.aalto;
    requires org.controlsfx.controls;

    opens org.tahomarobotics.scouting.scoutingserver to javafx.fxml;
    exports org.tahomarobotics.scouting.scoutingserver;
    exports org.tahomarobotics.scouting.scoutingserver.util;
    opens org.tahomarobotics.scouting.scoutingserver.util to javafx.fxml;
    exports org.tahomarobotics.scouting.scoutingserver.controller;
    opens org.tahomarobotics.scouting.scoutingserver.controller to javafx.fxml;
    exports org.tahomarobotics.scouting.scoutingserver.util.data;
    opens org.tahomarobotics.scouting.scoutingserver.util.data to javafx.fxml;
    exports org.tahomarobotics.scouting.scoutingserver.util.UI;
    opens org.tahomarobotics.scouting.scoutingserver.util.UI to javafx.fxml;
    exports org.tahomarobotics.scouting.scoutingserver.util.exceptions;
    opens org.tahomarobotics.scouting.scoutingserver.util.exceptions to javafx.fxml;
    exports org.tahomarobotics.scouting.scoutingserver.util.auto;
    opens org.tahomarobotics.scouting.scoutingserver.util.auto to javafx.fxml;
}
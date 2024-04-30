package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;

public class RenameableTab extends Tab {


    //https://github.com/sirolf2009/caesar/blob/35006dc64da24a3ac37136c6d9c555df843ac3b8/src/main/java/com/sirolf2009/caesar/component/RenameableTab.java
    public RenameableTab(StringProperty name) {
        Label label = new Label();
        label.textProperty().bind(name);
        setGraphic(label);

        TextField textField = new TextField();
        label.setOnMouseClicked(event -> {
            if(event.getClickCount() == 2) {
                textField.setText(label.getText());
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }
        });

        textField.setOnAction(event -> {
            name.setValue(textField.getText());
            setGraphic(label);
        });

        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(!newValue) {
                name.setValue(textField.getText());
                setGraphic(label);
            }
        });
    }
}
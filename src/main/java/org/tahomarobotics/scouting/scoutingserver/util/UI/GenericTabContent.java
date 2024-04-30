package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public abstract class GenericTabContent extends VBox {
    protected StringProperty tabName = new SimpleStringProperty();
    public GenericTabContent(String name) {
        this.tabName.set(name);

    }


    public abstract Node getContent();

    public StringProperty nameProperty() {
        return tabName;
    }

    public abstract void updateDisplay();
}

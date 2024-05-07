package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.dhatim.fastexcel.Protection;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

public abstract class GenericTabContent {

    protected Optional<File> contentFileLocation;
    protected StringProperty tabName = new SimpleStringProperty();
    private final BooleanProperty needsSavingProperty = new SimpleBooleanProperty(false);
    public GenericTabContent(String name, Optional<File> file) {
        this.tabName.set(name);
        needsSavingProperty.addListener((observableValue, oldVal, newVal) -> ScoutingServer.mainTabPane.getTabs().forEach(tab -> {//Problem is here
            Label label = (Label) tab.getGraphic();
            if (Objects.equals(label.getText(), name)) {
                label.setStyle("-fx-font-weight: " + (needsSavingProperty.get()?"bold":"regular"));
            }
        }));
        this.contentFileLocation = file;


    }

    public void setNeedsSavingProperty(boolean val) {
        needsSavingProperty.setValue(val);
    }

    public boolean needsSaving() {
        return  needsSavingProperty.get();
    }


    public abstract Node getContent();

    public StringProperty nameProperty() {
        return tabName;
    }

    public abstract void updateDisplay();

    public abstract Constants.TabType getTabType();

    //saves data to file if there is a file location provided, otherwise asks user to select where to save it
    public abstract void save();

    public abstract void saveAs();
}

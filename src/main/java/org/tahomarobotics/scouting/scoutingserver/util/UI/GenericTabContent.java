package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    /**
     * Preforms the generic part of saving, that is, writes a string to a file and also creates and delete a backup file and attempts to handle all errors
     * this could be called by save or save as
     * @param selectedFile can be null, just pass what the user gives right in
     * @throws FileNotFoundException if the selected file is null
     */
    protected void saveFileWithCorruptionProtection(File selectedFile, String data) throws FileNotFoundException {
        Path backupFile = Paths.get(Constants.BASE_APP_DATA_FILEPATH + "/resources/tempBackup" + System.currentTimeMillis() + ".tmp");
        //maybe, but better safe than sorry
        if (selectedFile == null) {
            throw new FileNotFoundException("Selected File was null");
        }
        Logging.logInfo("Saving file: " + selectedFile.getAbsolutePath());

        //save backup of file

        Path originalPath = selectedFile.toPath();
        try {
            Files.copy(originalPath, backupFile);
        } catch (IOException e) {
            Logging.logInfo("Failed to save backup before saving file, will just risk corrupting the users file");
        }

        try {
            FileOutputStream os = new FileOutputStream(selectedFile);
            os.write(data.getBytes());
            os.flush();
            os.close();
        } catch (IOException e) {
            Logging.logError(e, "Failed to save database, will try and restore backup");
            Path target = selectedFile.toPath();
            try {
                Files.copy(backupFile, target);
            } catch (IOException l) {
                Logging.logInfo("Failed to restore file");
            }
        }
            if (!backupFile.toFile().delete()) {
                Logging.logInfo("Failed to delete backup file when saving tab: " + tabName);
            }
    }
}

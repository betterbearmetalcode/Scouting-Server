package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DatabaseViewerTabContent;
import org.tahomarobotics.scouting.scoutingserver.util.UI.GenericTabContent;
import org.tahomarobotics.scouting.scoutingserver.util.UI.NewItemDialog;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Consumer;

public class MasterController {
    
    //this class in intended to recieve method calls from all over the app and distribute those method calls to 
    //more speacalized controllers


    //called when file>new is clicked, or the plus tab button is clicked
    public static void newThing() {
        Logging.logInfo("Creating something new");
        ScoutingServer.mainTabPane.addTab(new NewItemDialog().showAndWait(),true);
    }

    public static void openJSONFileIntoDatabase() {
        Logging.logInfo("Opening JSON File Database");
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select JSON File");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File selectedFile = chooser.showOpenDialog(ScoutingServer.mainStage.getOwner());
        if (selectedFile == null) {
            return;
        }
        ScoutingServer.mainTabPane.addTab(Optional.of(new DatabaseViewerTabContent(selectedFile)), false);


    }

    public static void saveCurrentTab() {
        Logging.logInfo("Saving current tab");
        Optional<GenericTabContent> selectedTabContent = ScoutingServer.mainTabPane.getSelectedTabContent();
        selectedTabContent.ifPresent(GenericTabContent::save);

    }

    public static void saveCurrentTabAs() {
        Logging.logInfo("Saving current tab to new file");
        Optional<GenericTabContent> selectedTabContent = ScoutingServer.mainTabPane.getSelectedTabContent();
        selectedTabContent.ifPresent(GenericTabContent::saveAs);
    }

    public static void saveAllTabs() {
        Logging.logInfo("saving all open tabs");
        ScoutingServer.mainTabPane.tabContents.forEach(GenericTabContent::save);
    }

    //returns new name of item
    public static String toggleDataTransferServer() {
        Logging.logInfo("starting data transfer server");
        return String.valueOf(System.currentTimeMillis());
    }

    public static void addJSONFile() {
        Logging.logInfo("Adding JSON File to database");
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select JSON File");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File selectedFile = chooser.showOpenDialog(ScoutingServer.mainStage.getOwner());
        if (selectedFile == null) {
            return;
        }
        Optional<GenericTabContent> selectedContentOptional = ScoutingServer.mainTabPane.getSelectedTabContent();
        if (selectedContentOptional.isPresent()) {
            Constants.TabType type = selectedContentOptional.get().getTabType();
            if (type == Constants.TabType.DATABASE_VIEWER) {
                try {
                    DatabaseManager.importJSONFile(selectedFile, ((DatabaseViewerTabContent) selectedContentOptional.get()).tableName);
                    selectedContentOptional.get().setNeedsSavingProperty(true);
                } catch (IOException e) {
                    Logging.logError(e, "Failed to import data");
                }
            }
        }

    }

    public static void addCSVFile() {
        Logging.logInfo("Adding CSV File to database");
    }

    public static void validateDatabase() {
        Logging.logInfo("Validating database");
        Tab selectedTab = ScoutingServer.mainTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null || selectedTab.getId() == null) {
            return;
        }
        //the user should only be able to press this button if the selected tab is a database, but just as a failsafe.
        //maybe the code could accidentally call this at the wrong time
        if (Constants.TabType.valueOf(selectedTab.getId()) == Constants.TabType.DATABASE_VIEWER) {
            Optional<GenericTabContent> contentOptional = ScoutingServer.mainTabPane.getTabContent(((Label) selectedTab.getGraphic()).getText());
            if (contentOptional.isEmpty()) {
                return;
            }
            DatabaseViewerTabContent content = (DatabaseViewerTabContent) contentOptional.get();
            content.validateData();
        }
    }

    public static void mergeDatabases() {
        Logging.logInfo("Merging databases");
    }

    public static void findTraps() {
        Logging.logInfo("Finding traps");
    }

    public static void genenerateStratScoutingSchedule() {
        Logging.logInfo("Generating Strat scouting schedule");
    }

    public static void openDocumentation() {
        Logging.logInfo("Opening Documentation");
    }

    //( ͡° ͜ʖ ͡°)
    public static void rickrollUser() {
        Logging.logInfo("Rickrolling user lol");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
            } catch (IOException | URISyntaxException e) {
                Logging.logError(e);
            }
        }
    }

    public static void export() {
        Logging.logInfo("Exporting");
    }
}
package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DatabaseViewerTabContent;
import org.tahomarobotics.scouting.scoutingserver.util.UI.GenericTabContent;
import org.tahomarobotics.scouting.scoutingserver.util.UI.RenameableTab;
import org.tahomarobotics.scouting.scoutingserver.util.UI.TableChooserDialog;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MainTabPane extends TabPane{

    public List<GenericTabContent> tabContents = new ArrayList<>();
    public MainTabPane() {
        super();
        this.prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
        this.prefWidthProperty().bind(Constants.UIValues.appWidtProperty());
        this.getSelectionModel().selectedItemProperty().addListener((observableValue, oldTab, newTab) -> {
            //figure out which type of tab we are switching to and disbale buttons accordingly
                if (newTab != null) {
                    ScoutingServer.setEnablingForSaveButtons(true);
                    switch (Constants.TabType.valueOf(newTab.getId())) {

                        case DATABASE_VIEWER -> {
                            ScoutingServer.setEnablingForDataMenu(true);
                        }
                        case CHART -> {
                            ScoutingServer.setEnablingForDataMenu(false);
                        }
                    }
                }else {
                    //if we are not switiching to any tab then surly we can't be on a database viewer so set it disbaled
                    ScoutingServer.setEnablingForDataMenu(false);
                }
                if (getTabs().isEmpty()) {
                    ScoutingServer.setEnablingForSaveButtons(false);
                    ScoutingServer.setEnablingForDataMenu(false);
                }
        });

    }


    //called when the new tab button is clicked
    public void addTab(Optional<GenericTabContent> tabContent) {


        if (tabContent.isEmpty()) {
            return;
        }
        RenameableTab tab = new RenameableTab(tabContent.get().nameProperty());
        tab.setId(tabContent.get().getTabType().name());

        tab.setContent(tabContent.get().getContent());
        tab.setClosable(true);
        tab.setOnClosed(event1 -> {
            for (GenericTabContent c : tabContents) {
                if (Objects.equals(tab.getId(), c.getTabType().name())) {
                    if (c.getTabType() == Constants.TabType.DATABASE_VIEWER) {
                        String tableName = ((DatabaseViewerTabContent) c).tableName;
                        try {
                            SQLUtil.execNoReturn("DROP TABLE IF EXISTS \"" + tableName + "\"");
                        } catch (SQLException | DuplicateDataException e) {
                            Logging.logError(e, "Failed to delete internal SQL table on tab closing");
                        }
                    }

                    tabContents.remove(c);
                    break;
                }
            }
        });

        setTabMaxHeight(Toolkit.getDefaultToolkit().getScreenSize().height);


        getTabs().add(tab);
        getSelectionModel().select(tab);
        tabContents.add(tabContent.get());
        tabContent.get().updateDisplay();

    }


    public Optional<GenericTabContent> getTabContent(String tabText) {
        for (GenericTabContent tabContent : tabContents) {
            if (Objects.equals(tabContent.nameProperty().get(), tabText)) {
                return Optional.of(tabContent);
            }
        }
        return Optional.empty();
    }

    public Optional<GenericTabContent> getSelectedTabContent() {
        Label label = (Label) getSelectionModel().getSelectedItem().getGraphic();
        return getTabContent(label.getText());
    }

}

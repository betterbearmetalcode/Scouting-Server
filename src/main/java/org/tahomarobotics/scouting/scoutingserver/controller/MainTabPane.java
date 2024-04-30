package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.GenericTabContent;
import org.tahomarobotics.scouting.scoutingserver.util.UI.NewItemDialog;
import org.tahomarobotics.scouting.scoutingserver.util.UI.RenameableTab;
import org.tahomarobotics.scouting.scoutingserver.util.UI.TableChooserDialog;

import java.awt.Toolkit;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class MainTabPane extends TabPane{
    private final Tab newTabTab = new Tab();
    private final Button newTabButton;

    public List<GenericTabContent> tabContents = new ArrayList<>();
    public MainTabPane() {
        super();
        newTabButton = new Button("+");
        newTabButton.setOnAction(event -> addTab(new NewItemDialog().showAndWait()));
        newTabTab.setGraphic(newTabButton);
        newTabTab.setClosable(false);
        this.prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
        this.prefWidthProperty().bind(Constants.UIValues.appWidtProperty());
        this.getTabs().add(newTabTab);

    }


    //called when the new tab button is clicked
    public void addTab(Optional<GenericTabContent> tabContent) {


        if (tabContent.isEmpty()) {
            return;
        }
        RenameableTab tab = new RenameableTab(tabContent.get().nameProperty());
        tab.setId(tabContent.get().nameProperty().get());

        tab.setContent(tabContent.get().getContent());
        tab.setClosable(true);

        tab.setOnClosed(event1 -> {
            for (GenericTabContent c : tabContents) {
                if (Objects.equals(tab.getId(), c.nameProperty().get())) {
                    tabContents.remove(c);
                    break;
                }
            }
        });

        setTabMaxHeight(Toolkit.getDefaultToolkit().getScreenSize().height);


        getTabs().add(getTabs().size() - 1, tab);
        getSelectionModel().select(tab);
        tabContents.add(tabContent.get());
        tabContent.get().updateDisplay();

    }

    private static String getSelectedTableFromUser() {
        try {
            TableChooserDialog dialog = new TableChooserDialog(SQLUtil.getTableNames());
            Optional<String> result = dialog.showAndWait();
            AtomicReference<String> selectedTable = new AtomicReference<>("");
            result.ifPresent(selectedTable::set);
            if (result.isPresent()) {
                return selectedTable.get();
            }else {
                    return "";
            }
        } catch (SQLException e) {
            Logging.logError(e);
            return "";
        }
    }

    public void refreshTabs() {
        for (GenericTabContent controller : tabContents) {
            controller.updateDisplay();
        }
    }

}

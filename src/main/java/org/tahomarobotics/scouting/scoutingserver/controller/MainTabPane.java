package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.UI.TableChooserDialog;

import java.awt.*;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MainTabPane extends TabPane{
    private final Tab newTabTab = new Tab();
    private final Button newTabButton;

    public List<TabController> controllers = new LinkedList<>();
    public MainTabPane() {
        super();
        newTabButton = new Button("+");
        newTabButton.setOnAction(event -> addTab());
        newTabTab.setGraphic(newTabButton);
        newTabTab.setClosable(false);
        this.prefHeightProperty().bind(Constants.UIValues.appHeightProperty());
        this.prefWidthProperty().bind(Constants.UIValues.appWidtProperty());
        this.getTabs().add(newTabTab);

    }


    public void addTab() {

        //get selected table name or competition from user
        String selectedTable = getSelectedTableFromUser();
        if (Objects.equals(selectedTable, "")) {
            return;
        }


        Tab tab = new Tab();
        tab.setText(selectedTable);
        tab.setId(selectedTable);
        TabController controller =  new TabController(selectedTable);
        tab.setContent(controller);
        tab.setClosable(true);
        tab.setOnClosed(event1 -> {
            for (TabController c : controllers) {
                if (Objects.equals(tab.getId(), c.tableName)) {
                    controllers.remove(c);
                    break;
                }
            }
        });

        setTabMaxHeight(Toolkit.getDefaultToolkit().getScreenSize().height);


        getTabs().add(getTabs().size() - 1, tab);
        getSelectionModel().select(tab);
        controllers.add(controller);
        controller.updateDisplay(false);

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
        for (TabController controller : controllers) {
            controller.updateDisplay(false);
        }
    }




}

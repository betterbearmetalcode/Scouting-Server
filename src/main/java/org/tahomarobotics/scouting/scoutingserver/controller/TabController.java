package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataValidator;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.Robot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class TabController {


    Tab myTab;
    private ArrayList<Match> databaseData;
    public String tableName;

    public Button validateDataButton;

    private TreeView<Label> treeView;

    @FXML
    public Label selectedCompetitionLabel;

    private TreeItem<Label> rootItem;

    private JSONArray eventList;
    private final ArrayList<Pair<String, String>> otherEvents = new ArrayList<>();

    private AutoCompletionBinding<String> autoCompletionBinding;

    private String currentEventCode = "";


    public TabController(ArrayList<Match> databaseData, String table) {
        this.databaseData = databaseData;
        tableName = table;
    }


    public void export(Event e) {
        Logging.logInfo("Exporting");
        if (!validateData()) {
            return;//really only need to refresh, but want the data to look "the same" for the user after exporting
        }
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(Constants.DATABASE_FILEPATH));
        chooser.setTitle("Select Export Location");
        chooser.setInitialFileName("export");
        chooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter("Excel Files", ".xls"));

        if ("".equals(currentEventCode)) {
            if (!selectCompetition()) {
                Logging.logInfo("Export Aborted");
                return;
            }
        }
        File file = chooser.showSaveDialog(ScoutingServer.mainStage.getOwner());
        if (file != null) {
            try {
                SpreadsheetUtil.writeToSpreadSheet(databaseData, file, currentEventCode);//should add button later if buranik insists to export raw wihtout formulas
            } catch (IOException ex) {
                Logging.logError(ex, "IO error while exporting or fetching TBA Data");
            } catch (InterruptedException ex) {
                Logging.logError(ex, "Interrupted Exception while Fetching TBA data ");
            }
        }else {
            Logging.logInfo("Export Aborted");
        }

    }

    @FXML
    public void expandAll(Event e) {
        Logging.logInfo("Expanding Tree");
        setExpansionAll(rootItem, true);

    }

    @FXML
    public void collapseAll() {
        Logging.logInfo("Collapsing Tree");
        setExpansionAll(rootItem, false);
    }

    private void setExpansionAll(TreeItem<Label> treeItem, boolean val) {
        if (treeItem.getValue().getText().equals("root-item")) {
            //then we are dealing with the root item
            treeItem.setExpanded(true);
        } else {
            treeItem.setExpanded(val);

        }
        if (!treeItem.getChildren().isEmpty()) {
            for (TreeItem<Label> t : treeItem.getChildren()) {
                setExpansionAll(t, val);
            }
        }


    }


    public void initialize(TreeView<Label> view) {
        Logging.logInfo("Initializing Tab Controller: " + tableName);
        //init data stuff
        treeView = view;
        rootItem = new TreeItem<>(new Label("root-item"));
        if (!databaseData.isEmpty()) {
            constructTree(databaseData);
        }
        treeView.setShowRoot(false);
        treeView.setRoot(rootItem);

        //init list of events
        try {
            FileInputStream stream = new FileInputStream(Constants.BASE_READ_ONLY_FILEPATH + "/resources/TBAData/eventList.json");
            eventList = new JSONArray(new String(stream.readAllBytes()));
            stream.close();
        } catch (FileNotFoundException e) {
            Logging.logError(e, "Could Not Find list of Competitions");
        } catch (IOException e) {
            Logging.logError(e, "Whooop de doo, another IO exception, I guess your just screwed now,-C.H");
        }


    }
    @FXML
    public void validateDataButtonHandler(ActionEvent event) {
        validateData();
    }
    public boolean validateData() {
        Logging.logInfo("Validating Data");
        refresh();

        if (Objects.equals(currentEventCode, "")) {
            if (!selectCompetition()) {
                Logging.logInfo("Data Validation Aborted");
                return false;
            }
        }
        databaseData = DataValidator.validateData(currentEventCode, databaseData);
        constructTree(databaseData);
        return true;

    }



    @FXML
    public void refresh() {
        Logging.logInfo("Refreshing");
        try {
            databaseData = DatabaseManager.getUnCorrectedDataFromDatabase(tableName);
        } catch (IOException ex) {
            Logging.logError(ex);
        }
        constructTree(databaseData);
    }

    private void constructTree(ArrayList<Match> matches) {
        Logging.logInfo("Constructing tree");
        rootItem.getChildren().clear();
        for (Match match : matches) {

            DataPoint.ErrorLevel maxErrorLevelInThisMatch = DataPoint.ErrorLevel.ZERO;
            Label matchLabel = new Label("Match: " + match.matchNumber());
            TreeItem<Label> matchItem = new TreeItem<>(matchLabel);


            for (Robot robot : match.robots()) {
                DataPoint.ErrorLevel maxErrorForThisRobot = DataPoint.ErrorLevel.ZERO;
                Label robotLabel = new Label(robot.robotPosition().toString() + ": " + robot.teamNumber());
                TreeItem<Label> robotItem = new TreeItem<>(robotLabel);
                matchItem.getChildren().add(robotItem);


                for (DataPoint dataPoint : robot.data()) {
                    Label l = new Label(dataPoint.toString());
                    l.setTextFill(DataPoint.color.get(dataPoint.getErrorLevel()));
                    TreeItem<Label> dataItem = new TreeItem<>(l);
                    robotItem.getChildren().add(dataItem);
                    if (dataPoint.getErrorLevel().ordinal() > maxErrorForThisRobot.ordinal()) {
                        maxErrorForThisRobot = dataPoint.getErrorLevel();
                    }
                }//end robot for
                if (maxErrorForThisRobot.ordinal() > maxErrorLevelInThisMatch.ordinal()) {
                    maxErrorLevelInThisMatch = maxErrorForThisRobot;
                }
                robotLabel.setTextFill(DataPoint.color.get(maxErrorForThisRobot));
            }//end match for
            matchLabel.setTextFill(DataPoint.color.get(maxErrorLevelInThisMatch));
            rootItem.getChildren().add(matchItem);
        }//end comp for
    }
    public boolean selectCompetition() {
        Logging.logInfo("Selecting Competiton COde");
        Dialog<String> dialog = new Dialog<>();
        TextField autoCompetionField = new TextField();
        ArrayList<String> options = new ArrayList<>();
        otherEvents.clear();
        for (Object o : eventList.toList()) {
            HashMap<String, String> comp = (HashMap<String, String>) o;
            otherEvents.add(new Pair<>(comp.get("key"), comp.get("name")));
            options.add(comp.get("name"));
        }

        autoCompletionBinding = TextFields.bindAutoCompletion(autoCompetionField, options);

        FlowPane pane = new FlowPane(new Label("Enter Competition: "), autoCompetionField);
        dialog.getDialogPane().setContent(pane);
        dialog.setTitle("Select Competition For Data Validation");
        dialog.setHeaderText("");
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                return autoCompetionField.getText();
            }else {
                return "";
            }
        });
        Optional<String> result = dialog.showAndWait();
        AtomicReference<String> selectedEvent = new AtomicReference<>("");
        result.ifPresent(selectedEvent::set);
        String temp = selectedEvent.get();
        if (!Objects.equals(temp, "")) {

            Optional<Pair<String, String>> event = otherEvents.stream().filter(s -> s.getValue().equals(temp)).findFirst();
            AtomicReference<Pair<String,String>> selectedEventCode = new AtomicReference<>(new Pair<>("",""));
            event.ifPresent(selectedEventCode::set);
            currentEventCode = selectedEventCode.get().getKey();
            selectedCompetitionLabel.setText(temp);

        }else {
            currentEventCode =  "";
        }
        return !Objects.equals(currentEventCode, "");

    }


}

package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import javafx.util.StringConverter;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DataValidator;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.SpreadsheetUtil;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.Robot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class TabController {


    @FXML
    private TreeView<Label> treeView;
    @FXML
    public Label selectedCompetitionLabel;

    @FXML
    public TabPane pane;
    @FXML
    public ToggleButton editToggle;
    @FXML
    public Button validateDataButton;
    @FXML
    public Button exportButton;
    private ArrayList<Match> databaseData;
    public String tableName;

    private TreeItem<Label> rootItem;
    private JSONArray eventList;
    private final ArrayList<Pair<String, String>> otherEvents = new ArrayList<>();
    private String currentEventCode = "";


    public TabController(ArrayList<Match> databaseData, String table, TabPane thePane) {
        this.databaseData = databaseData;
        tableName = table;
        pane = thePane;
    }

    @FXML
    public void initialize(TreeView<Label> view) {
        Logging.logInfo("Initializing Tab Controller: " + tableName);
        //init data stuff
        treeView = view;
        rootItem = new TreeItem<>(new Label("root-item"));
        setEditMode(false);
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
    public void export(Event e) {
        Logging.logInfo("Exporting");
        if (!validateData()) {
            return;//really only need to refresh, but want the data to look "the same" for the user after exporting
        }
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(Constants.DATABASE_FILEPATH));
        chooser.setTitle("Select Export Location");
        chooser.setInitialFileName("export " + new Date(System.currentTimeMillis()).toString().replaceAll(":", "-"));
        chooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter("Excel Files", ".xls"));

        if ("".equals(currentEventCode)) {
            if (selectCompetition()) {
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
    public void validateDataButtonHandler(ActionEvent event) {
        validateData();
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

    @FXML
    public void collapseAll() {
        Logging.logInfo("Collapsing Tree");
        setExpansionAll(rootItem, false);
    }

    @FXML
    public void expandAll(Event e) {
        Logging.logInfo("Expanding Tree");
        setExpansionAll(rootItem, true);

    }

    @FXML
    public void toggleEditMode(ActionEvent event) {
        Logging.logInfo("Toggleing Edit Mode");
        setEditMode(editToggle.isSelected());

    }

    public boolean validateData() {
        Logging.logInfo("Validating Data");
        refresh();

        if (Objects.equals(currentEventCode, "")) {
            if (selectCompetition()) {
                Logging.logInfo("Data Validation Aborted");
                return false;
            }
        }
        databaseData = DataValidator.validateData(currentEventCode, databaseData);
        constructTree(databaseData);
        return true;

    }

    private void constructTree(ArrayList<Match> matches) {
        Logging.logInfo("Constructing tree");
        if (matches.isEmpty()) {
            return;
        }
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

        AutoCompletionBinding<String> autoCompletionBinding = TextFields.bindAutoCompletion(autoCompetionField, options);

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
        return Objects.equals(currentEventCode, "");

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


    private void setEditMode(boolean mode) {
        treeView.setEditable(mode);
        if (mode) {
            //set to edit mode
            treeView.setCellFactory(param -> new EditableTreeCell(this));
            validateDataButton.setDisable(true);
            exportButton.setDisable(true);
        }else {
            //get out of edit mode
            validateDataButton.setDisable(false);
            exportButton.setDisable(false);
            treeView.setCellFactory(null);
        }
        constructTree(databaseData);
    }

    private class EditableTreeCell extends TextFieldTreeCell<Label> {
        private final ContextMenu menu = new ContextMenu();
        private final TabController controller;
        public EditableTreeCell(TabController c) {
            super(new CustomStringConverter());
            controller = c;
            MenuItem renameItem = new MenuItem("Edit");
            menu.getItems().add(renameItem);
            renameItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent arg0) {
                    startEdit();
                }
            });
        }

        @Override
        public void updateItem(Label item, boolean empty) {
            super.updateItem(item, empty);
            if (!isEditing()) {
                setContextMenu(menu);
            }
        }
        @Override
        public void startEdit() {
            if (this.getTreeItem().isLeaf()) {
                String name = this.getTreeItem().getValue().getText().split(":")[0];
                if ((Objects.equals(name, Constants.SQLColumnName.ALLIANCE_POS.toString().replaceAll("_", " ").toLowerCase())) || (Objects.equals(name, Constants.SQLColumnName.TEAM_NUM.toString().replaceAll("_", " ").toLowerCase())) ||(Objects.equals(name, Constants.SQLColumnName.MATCH_NUM.toString().replaceAll("_", " ").toLowerCase())) || (Objects.equals(name, Constants.SQLColumnName.TIMESTAMP.toString().replaceAll("_", " ").toLowerCase())) ||(Objects.equals(name, Constants.SQLColumnName.AUTO_COMMENTS.toString().replaceAll("_", " ").toLowerCase())) || (Objects.equals(name, Constants.SQLColumnName.TELE_COMMENTS.toString().replaceAll("_", " ").toLowerCase())) ) {
                    //then this is a comment
                    cancelEdit();
                    Logging.logInfo("This data cannot be edited", true);


                }else {
                    super.startEdit();
                }

            }else {
                super.cancelEdit();
            }

        }
        @Override
        public void commitEdit(Label newLabel) {
            if (this.getTreeItem().isLeaf()) {
                //given the code in start edit, the newLabel is guarentted to be a numeric leaf item.
                try {
                    //see if they actually entered a number
                    Integer.valueOf(newLabel.getText());
                }catch (NumberFormatException e) {
                    cancelEdit();
                    Logging.logInfo("This needs to be a number, cancelling edit", true);

                }
                TreeItem<Label> dataItem = this.getTreeItem();
                TreeItem<Label> robotItem = dataItem.getParent();
                TreeItem<Label> matchItem = robotItem.getParent();

                //figure out what the new string should be
                String[] tokens = dataItem.getValue().getText().split(":");
                tokens[1] = newLabel.getText();
                StringBuilder builder = new StringBuilder();
                for (String token : tokens) {
                    builder.append(token).append(":");
                }
                String newString = builder.substring(0, builder.toString().length() - 1);
                super.commitEdit(new Label(newString));
                int matchNum = Integer.parseInt(matchItem.getValue().getText().split(" ")[1]);
                int teamNum = Integer.parseInt(robotItem.getValue().getText().split(" ")[1]);
                Match match = databaseData.stream().filter(match1 -> match1.matchNumber() == matchNum).findFirst().get();
                Robot robot = match.robots().stream().filter(robot1 -> robot1.teamNumber() == teamNum).findFirst().get();
                robot.data().replaceAll(dataPoint -> {
                    if (Objects.equals(dataPoint.getName().replaceAll("_", " ").toLowerCase(), tokens[0])) {
                        //then this is the datapoint we are looking for
                        DataPoint newData = new DataPoint(newString);
                        StringBuilder statementBuilder = new StringBuilder();
                        statementBuilder.append("UPDATE \"").append(tableName).append("\" ");
                        statementBuilder.append("SET ").append(newData.getName()).append("=?");
                        statementBuilder.append(" WHERE ").append(Constants.SQLColumnName.TEAM_NUM).append("=?");
                        statementBuilder.append(" AND ").append(Constants.SQLColumnName.MATCH_NUM).append("=?");
                        try {
                            SQLUtil.execNoReturn(statementBuilder.toString(), new String[] {newLabel.getText(), String.valueOf(teamNum), String.valueOf(matchNum)});
                        } catch (SQLException e) {
                            Logging.logError(e, "Error updatingSQLDatabase");
                            cancelEdit();
                            return dataPoint;
                        }
                        return newData;
                    }else {
                        return dataPoint;
                    }
                });
            }else {
                System.out.println("Cancelling branch edit");
                cancelEdit();
            }


        }


    }

    private static class CustomStringConverter extends StringConverter<Label> {
        Paint paint = Color.PINK;//default, if I see this, something is wrong
        @Override
        public String toString(javafx.scene.control.Label object) {
            return object.getText();
        }

        @Override
        public javafx.scene.control.Label fromString(String string) {
            return new Label(string);
        }
    }


}

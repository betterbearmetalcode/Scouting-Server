package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import javafx.util.StringConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.*;
import org.tahomarobotics.scouting.scoutingserver.util.*;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DataValidationCompetitionChooser;
import org.tahomarobotics.scouting.scoutingserver.util.data.DataPoint;
import org.tahomarobotics.scouting.scoutingserver.util.data.Match;
import org.tahomarobotics.scouting.scoutingserver.util.data.RobotPositon;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.OperationAbortedByUserException;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class TabController {


    @FXML
    private TreeView<Label> treeView;

    @FXML
    public TabPane pane;
    @FXML
    public ToggleButton editToggle;
    @FXML
    public Button validateDataButton;
    @FXML
    public Button exportButton;
    @FXML
    public CheckBox exportNotesCheckbox;
    private ArrayList<Match> databaseData;

    public String tableName;

    private TreeItem<Label> rootItem;
    private JSONArray eventList;
 //   private final ArrayList<Pair<String, String>> otherEvents = new ArrayList<>();
    private String currentEventCode = "";
    private  boolean editmode = false;


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
        treeView.setOnEditStart(new EventHandler<TreeView.EditEvent<Label>>() {
            @Override
            public void handle(TreeView.EditEvent<Label> event) {
                if (!editmode) {
                    setEditMode(true);
                    event.consume();
                }

            }
        });

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
    public void export(Event event) {
        Logging.logInfo("Exporting");
        //first gather the nessacary data from tba to export and check to make sure
        //everything is good and ready before showing the user a save dialog
        //save dialog should be the last dialog becuase in the user's mind the file is already created when that dialog comes up



        //gather TBA data
        //first figure out which competition we are at
        if ("".equals(currentEventCode)) {
            //if no event code has been selected
            if (selectCompetition()) {
                //if the selection fails or is canceled etc
                Logging.logInfo("Export Aborted");
                return;
            }
        }
        //gather TBA data and prepare a list of teams who are at the comp
        Exporter exporter;
        try {
            exporter = new Exporter(currentEventCode, tableName);
        } catch (IOException  | InterruptedException e) {
            Logging.logError(e, "Failed to construct exporter like while executing API requests, aborting export");
            return;
        }catch (OperationAbortedByUserException e) {
            Logging.logInfo("export cancelled by user because there is no internet");
            return;
        } catch (SQLException e) {
            Logging.logError(e, "Failed to select teams from database, aborting export");
            return;
        }
        //code here  will only run if exporter was successfully initialized
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        try {
            data = exporter.export(exportNotesCheckbox.isSelected());
        } catch (SQLException e) {
            Logging.logError(e, "failed to generate exported data");
        }

        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(Constants.DATABASE_FILEPATH));
        chooser.setTitle("Select Export Location");
        chooser.setInitialFileName("export " + new Date(System.currentTimeMillis()).toString().replaceAll(":", "-"));
        chooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter("Excel Files", ".xlsx"));


        File file = chooser.showSaveDialog(ScoutingServer.mainStage.getOwner());
        if (file != null) {
            try {

                SpreadsheetUtil.writeArrayToSpreadsheet(data, file);
            } catch (IOException ex) {
                Logging.logError(ex, "IO error while writing to spreadsheet");
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
        constructTree(databaseData, false);
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
        if (editmode != editToggle.isSelected()) {
            setEditMode(editToggle.isSelected());
        }


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
        constructTree(databaseData, false);
        return true;

    }

    private void constructTree(ArrayList<Match> matches, boolean switchingToEditMode) {
        Logging.logInfo("Constructing tree");
        if (matches.isEmpty()) {
            return;
        }
        //construct a array containing the expansion structure of the database, if there are changes (exceptions), then we can defualt the expansionto false
        ArrayList<Pair<Boolean, ArrayList<Boolean>>> expainsionStructure = new ArrayList<>();
        for (TreeItem<Label> matchItem : rootItem.getChildren()) {
            ArrayList<Boolean> matchExpansion = new ArrayList<>();
            for (TreeItem<Label> robotItem : matchItem.getChildren()) {
                matchExpansion.add(robotItem.isExpanded());
            }
            expainsionStructure.add(new Pair<>(matchItem.isExpanded(), matchExpansion));
        }
        rootItem.getChildren().clear();
        for (Match match : matches) {

            DataPoint.ErrorLevel maxErrorLevelInThisMatch = DataPoint.ErrorLevel.ZERO;
            Label matchLabel = new Label("Match: " + match.matchNumber());
            TreeItem<Label> matchItem = new TreeItem<>(matchLabel);
            try {
            matchItem.setExpanded(expainsionStructure.get(match.matchNumber() - 1).getKey());
            }catch (IndexOutOfBoundsException e) {
                matchItem.setExpanded(false);

               }


            for (RobotPositon robotPositon : match.robotPositons()) {
                DataPoint.ErrorLevel maxErrorForThisRobot = DataPoint.ErrorLevel.ZERO;
                Label robotLabel = new Label(robotPositon.robotPosition().toString() + ": " + robotPositon.teamNumber());
                TreeItem<Label> robotItem = new TreeItem<>(robotLabel);
                matchItem.getChildren().add(robotItem);
                try {
                    robotItem.setExpanded(expainsionStructure.get(match.matchNumber() - 1).getValue().get(robotPositon.robotPosition().ordinal()));
                }catch (IndexOutOfBoundsException e) {
                    robotItem.setExpanded(false);
                }

                for (DataPoint dataPoint : robotPositon.data()) {
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
                if (!robotPositon.isUnknown()) {
                    robotLabel.setTextFill(DataPoint.color.get(maxErrorForThisRobot));
                }else {
                    robotLabel.setTextFill(DataPoint.color.get(DataPoint.ErrorLevel.UNKNOWN));
                }

            }//end match for
            if (!match.isUnknown()) {
                matchLabel.setTextFill(DataPoint.color.get(maxErrorLevelInThisMatch));
            }else {
                matchLabel.setTextFill(DataPoint.color.get(DataPoint.ErrorLevel.UNKNOWN));
            }

            if (!matchItem.getChildren().isEmpty()) {
                rootItem.getChildren().add(matchItem);
            }

        }//end comp for


    }
    public boolean selectCompetition() {
        Logging.logInfo("asking user to select which competition they are at");
        DataValidationCompetitionChooser chooser = new DataValidationCompetitionChooser();
        Optional<String> result = chooser.showAndWait();
        AtomicReference<String> selectedEvent = new AtomicReference<>("");
        result.ifPresent(selectedEvent::set);
        currentEventCode = selectedEvent.get();
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
    @FXML
    public void saveJSONBackup(ActionEvent event) {
        Logging.logInfo("Making JSON Backup of " + tableName);
        JSONObject output = new JSONObject();
        for (DatabaseManager.RobotPosition robotPosition : DatabaseManager.RobotPosition.values()) {
            //get all the data for each of the positions
            LinkedList<DatabaseManager.QRRecord> data = DatabaseManager.readDatabase(tableName, "SELECT * FROM \"" + tableName + "\"" + " WHERE "  + Constants.SQLColumnName.ALLIANCE_POS + "=?", new Object[]{robotPosition.ordinal()}, false);
            JSONArray positionArray = new JSONArray();
            for (DatabaseManager.QRRecord record : data) {
                positionArray.put(record.getQRString());
            }
            output.put(robotPosition.name(), positionArray);
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Backup");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.setInitialFileName("Backup " + new Date().toString().replaceAll(":", " ") + ".json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", ".json"));
        File selectedFile = chooser.showSaveDialog(ScoutingServer.mainStage.getOwner());
        try {
            if (!selectedFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                selectedFile.createNewFile();
            }
            FileOutputStream os = new FileOutputStream(selectedFile);
            os.write(output.toString().getBytes());
            os.flush();
            os.close();
        } catch (IOException e) {
            Logging.logError(e, "Failed to save backup");
        }
    }

    public void clearDatabase() {
        Logging.logInfo("Clearing databse: " + tableName);
        try {
            SQLUtil.execNoReturn("DELETE FROM \"" + tableName + "\"");
        } catch (SQLException | DuplicateDataException e) {
            Logging.logError(e);
        }
        databaseData.clear();
        rootItem.getChildren().clear();
        refresh();
    }


    private void setEditMode(boolean mode) {
        setEditMode(mode, -1,-1,-1);
    }

    private void setEditMode(boolean mode, int matchNum, int teamNum, int dataumIndex) {
        treeView.setEditable(mode);
        if (mode) {
            //set to edit mode
            treeView.setCellFactory(param -> new EditableTreeCell(this));
            validateDataButton.setDisable(true);
            exportButton.setDisable(true);
            editToggle.setSelected(true);
            editmode = true;
            treeView.getSelectionModel().select(treeView.getRoot().getChildren().get(matchNum).getChildren().get(teamNum).getChildren().get(dataumIndex));//for some reason letting this line throw exceptions makes the app work
            //if you catch ten exceptions, then it will not work, if you iplement a check to stop them, it also won't work. Don't bother to try and fix this, it doesn't matter.
        }else {
            //get out of edit mode
            validateDataButton.setDisable(false);
            exportButton.setDisable(false);
            treeView.setCellFactory(null);
            treeView.setEditable(true);
            editToggle.setSelected(false);
            editmode = false;
        }
        constructTree(databaseData, true);
    }

    private class EditableTreeCell extends TextFieldTreeCell<Label> {
        private final ContextMenu menu = new ContextMenu();
        private final TabController controller;
        public EditableTreeCell(TabController c) {
            super(new CustomStringConverter());
            controller = c;
            MenuItem renameItem = new MenuItem("Edit");
            MenuItem deleteItem = new MenuItem("Delete");
            menu.getItems().addAll(renameItem, deleteItem);

            renameItem.setOnAction(arg0 -> startEdit());
            deleteItem.setOnAction(event -> {
                if (getTreeItem().getParent() == rootItem) {
                    //then this is a match item
                    System.out.println("match item");
                    for (TreeItem<Label> positionItem : getTreeItem().getChildren()) {
                        deletePositionItem(positionItem);
                    }
                }else if (getTreeItem().isLeaf()){
                    deletePositionItem(getTreeItem().getParent());
                }else {
                    deletePositionItem(getTreeItem());
                }
                refresh();


            });
        }

        private void deletePositionItem(TreeItem<Label> positionItem) {
            String teamNum = positionItem.getValue().getText().split(" ")[1];

            String match = positionItem.getParent().getValue().getText().split(" ")[1];
            try {
                SQLUtil.execNoReturn("DELETE FROM \"" + tableName + "\" WHERE " + Constants.SQLColumnName.MATCH_NUM.name() + "=? AND " + Constants.SQLColumnName.TEAM_NUM.name() + "=?", new Object[]{match, teamNum}, true);

            } catch (SQLException | DuplicateDataException e) {
                Logging.logError(e, "Failed to delete datapoint");
            }
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
                if ((Objects.equals(name, Constants.SQLColumnName.ALLIANCE_POS.toString().replaceAll("_", " ").toLowerCase())) || (Objects.equals(name, Constants.SQLColumnName.TEAM_NUM.toString().replaceAll("_", " ").toLowerCase())) ||(Objects.equals(name, Constants.SQLColumnName.MATCH_NUM.toString().replaceAll("_", " ").toLowerCase()))  || (Objects.equals(name, Constants.SQLColumnName.TELE_COMMENTS.toString().replaceAll("_", " ").toLowerCase())) ) {
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
        public void cancelEdit() {
            super.cancelEdit();
            setEditMode(false);
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
                RobotPositon robotPositon = match.robotPositons().stream().filter(robot1 -> robot1.teamNumber() == teamNum).findFirst().get();
                robotPositon.data().replaceAll(dataPoint -> {
                    if (Objects.equals(dataPoint.getName().replaceAll("_", " ").toLowerCase(), tokens[0])) {
                        //then this is the datapoint we are looking for
                        DataPoint newData = new DataPoint(newString);
                        StringBuilder statementBuilder = new StringBuilder();
                        statementBuilder.append("UPDATE \"").append(tableName).append("\" ");
                        statementBuilder.append("SET ").append(newData.getName()).append("=?");
                        statementBuilder.append(" WHERE ").append(Constants.SQLColumnName.TEAM_NUM).append("=?");
                        statementBuilder.append(" AND ").append(Constants.SQLColumnName.MATCH_NUM).append("=?");
                        try {
                            SQLUtil.execNoReturn(statementBuilder.toString(), new String[] {newLabel.getText(), String.valueOf(teamNum), String.valueOf(matchNum)}, true);
                            Timer timer = new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    setEditMode(false);
                                }
                            }, 10);
                        } catch (SQLException | DuplicateDataException e) {
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

package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;

public class ChartsController implements Initializable {
    @FXML
    public VBox mainBox;
    StackedBarChart<String, Number> chart;

    public  enum Category {
        AUTO_NOTES,
        TELE_NOTES
    }

    @FXML
    public void generateTeleTotals(ActionEvent event) {
        Logging.logInfo("Generating Tele Totals");
        try {
            if (QRScannerController.activeTable.equals("")) {
                ScoutingServer.qrScannerController.selectTargetTable(null);
                if (QRScannerController.activeTable.equals("")) {
                    //if the user doesn't select a database, then whatever
                    return;
                }
            }
            ArrayList<HashMap<String, Object>> teamsScouted = SQLUtil.exec("SELECT DISTINCT " + Constants.SQLColumnName.TEAM_NUM + " FROM \"" + QRScannerController.activeTable + "\"");
            ArrayList<String> teamNumbers = new ArrayList<>();
            teamsScouted.forEach(map -> {teamNumbers.add(map.get(Constants.SQLColumnName.TEAM_NUM.toString()).toString());});
            CategoryAxis xAxis = new CategoryAxis();

            xAxis.setCategories(FXCollections.observableArrayList(teamNumbers));
            xAxis.setLabel("Team");


            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("TeleContribution");

            chart.getData().clear();

            XYChart.Series<String, Number> autoNotesSeries = new XYChart.Series<>();
            autoNotesSeries.setName(Category.AUTO_NOTES.name().replaceAll("_", " ").toLowerCase());

            XYChart.Series<String, Number> teleNotesSeris = new XYChart.Series<>();
            teleNotesSeris.setName(Category.TELE_NOTES.name().replaceAll("_", " ").toLowerCase());
            HashMap<String, Double> ranks = new HashMap<>();
            for (String teamNumber : teamNumbers) {
                double autoAmp = getAverage(Constants.SQLColumnName.AUTO_AMP, teamNumber, QRScannerController.activeTable);
                double autoSpeaker = getAverage(Constants.SQLColumnName.TELE_SPEAKER, teamNumber, QRScannerController.activeTable);
                autoNotesSeries.getData().add(new XYChart.Data<>(teamNumber, autoAmp + autoSpeaker));
                double teleAmp = getAverage(Constants.SQLColumnName.TELE_AMP, teamNumber, QRScannerController.activeTable);
                double teleSpeaker = getAverage(Constants.SQLColumnName.TELE_SPEAKER, teamNumber, QRScannerController.activeTable);
                teleNotesSeris.getData().add(new XYChart.Data<>(teamNumber, teleAmp + teleSpeaker));
                ranks.put(teamNumber, (autoAmp + autoSpeaker + teleAmp + teleSpeaker));
            }

            xAxis.getCategories().sort((o1, o2) -> Double.compare(ranks.get(o2), ranks.get(o1)));
            setChart(xAxis, yAxis, "Total Contribution by team");
            chart.getData().addAll(autoNotesSeries, teleNotesSeris);



        } catch (SQLException e) {
            Logging.logError(e, "Failed to generate chart");
        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setChart(new CategoryAxis(), new NumberAxis(), "No chart Generated");
    }

    private void setChart(CategoryAxis xAxis, NumberAxis yAxis, String title) {
        if (mainBox != null) {
            chart = new StackedBarChart<>(xAxis, yAxis);
            chart.prefHeightProperty().bind(Constants.UIValues.databaseHeightProperty());
            chart.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
            mainBox.getChildren().removeIf(node -> node.getClass().equals(StackedBarChart.class));
            mainBox.getChildren().add(0, chart);
            chart.setTitle(title);
        }

    }

    private static double getAverage(Constants.SQLColumnName column, String teamName, String table) throws SQLException {
        ArrayList<HashMap<String, Object>> raw = SQLUtil.exec("SELECT " + column + " FROM \"" + table + "\" WHERE " + Constants.SQLColumnName.TEAM_NUM + "=?", new Object[]{teamName});
        ArrayList<Integer> data = new ArrayList<>();
        raw.forEach(map -> {data.add(Integer.valueOf(map.get(column.toString()).toString()));});
        return (double) data.stream().mapToInt(Integer::intValue).sum() /data.size();
    }
}

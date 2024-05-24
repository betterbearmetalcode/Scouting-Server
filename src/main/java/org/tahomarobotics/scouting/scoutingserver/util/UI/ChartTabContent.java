package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.Configuration;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.NoDataFoundException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

import static org.tahomarobotics.scouting.scoutingserver.DatabaseManager.getAverage;

public class ChartTabContent extends GenericTabContent{


    private CategoryAxis categoryAxis;
    private NumberAxis numberAxis;
    private File dataFile;

    private ArrayList<DataMetric>  metricsToGraph;
    String tablename = String.valueOf(System.currentTimeMillis());

    private StackedBarChart<String, Number> chart;
    public String title;
    private final VBox content = new VBox();
    public ChartTabContent(String name, File saveLocation, File dataSourceFile, ArrayList<DataMetric> stuffToGraph, CategoryAxis xAxis, NumberAxis theNumberAxis) {
        super(name, Optional.of(saveLocation));
        if (dataSourceFile == null || saveLocation == null) {
            throw new NullPointerException("No Source and/or save location File Found, aborting chart Construction");
        }
        numberAxis = theNumberAxis;
        dataFile = dataSourceFile;
        numberAxis.setTickLength(1);
        chart = new StackedBarChart<>(xAxis, numberAxis);
        categoryAxis = xAxis;
        chart.setTitle(title);
        metricsToGraph = stuffToGraph;
        updateDisplay();

    }

    @Override
    public Node getContent() {
        return content;
    }

    @Override
    public void updateDisplay() {
        Logging.logInfo("Updating Chart");
        chart.getData().clear();
        content.getChildren().clear();
        categoryAxis.getCategories().clear();

        try {
            SQLUtil.execNoReturn("DROP TABLE IF EXISTS \"" + tablename + "\"");
            SQLUtil.addTableIfNotExists(tablename);
            try {
                DatabaseManager.importJSONFile(dataFile, tablename);
            }catch (NoDataFoundException e) {
                Logging.logInfo("No, data found in this data file, chart will be blank", true);
                return;
            }

            ArrayList<HashMap<String, Object>> teamsScouted = SQLUtil.exec("SELECT DISTINCT " + Constants.SQLColumnName.TEAM_NUM + " FROM \"" + tablename + "\"", true);
            ArrayList<String> teamNumbers = new ArrayList<>();
            teamsScouted.forEach(map -> {teamNumbers.add(map.get(Constants.SQLColumnName.TEAM_NUM.toString()).toString());});
            categoryAxis.setCategories(FXCollections.observableArrayList(teamNumbers));
            numberAxis.setTickLength(1);
            HashMap<String, Double> ranks = new HashMap<>();

            for (DataMetric metric : metricsToGraph) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(metric.getName());
                for (String teamNumber : teamNumbers) {
                    double average = getAverage(metric, teamNumber,tablename, false);
                    series.getData().add(new XYChart.Data<>(teamNumber, average));
                    double val = ranks.getOrDefault(teamNumber, 0.0);
                    ranks.remove(teamNumber);
                    ranks.put(teamNumber, val + average);
                }
                chart.getData().add(series);
            }

            categoryAxis.getCategories().sort((o1, o2) -> Double.compare(ranks.get(o2), ranks.get(o1)));
            chart.prefHeightProperty().bind(Constants.UIValues.databaseHeightProperty());
            chart.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());

        } catch (IOException | SQLException e) {
            Logging.logError(e);
        }finally {
            content.getChildren().add(chart);
        }
    }

    @Override
    public Constants.TabType getTabType() {
        return Constants.TabType.CHART;
    }

    @Override
    public void save() {
        File selectedFile;
        boolean needToPickFile = true;
        if (contentFileLocation.isPresent()) {
            File potentialFile = contentFileLocation.get();
            if (potentialFile.exists() && potentialFile.isFile()) {
                if (potentialFile.getName().endsWith(Constants.DATA_FILE_EXTENSION)) {
                    needToPickFile = false;
                }
            } else  {
                try {
                    if (contentFileLocation.get().createNewFile()) {
                        needToPickFile = false;
                    }
                }catch (Exception ignored) {
                }

            }
        }

        if (needToPickFile) {
            selectedFile =  Constants.selectChartFile("Save Chart", true, tabName.get());
        }else {
            selectedFile =  contentFileLocation.get();
        }
        save(selectedFile);

    }

    @Override
    public void saveAs() {
        File selectedFile = Constants.selectChartFile("Save Chart", true, tabName.get());
        save(selectedFile);
    }

    private void save(File selectedFile) {
        JSONObject saveObject = new JSONObject();
        saveObject.put("chart-name", tabName.get());
        saveObject.put("chart-source-file", dataFile.getAbsolutePath());
        JSONObject stuffToGraph = new JSONObject();
        for (int i = 0; i < metricsToGraph.size(); i++) {
            stuffToGraph.put("metric: " + i, metricsToGraph.get(i).getName());//only put name, will read from config file later to get fill data metric object
        }
        saveObject.put("metrics-to-graph", stuffToGraph);
        saveObject.put("name-of-categoryAxis", categoryAxis.getLabel());
        saveObject.put("name-of-numberAxis", numberAxis.getLabel());
        try {
            saveFileWithCorruptionProtection(selectedFile, Base64.getEncoder().encodeToString(saveObject.toString(1).getBytes()));
        } catch (FileNotFoundException e) {
            Logging.logError(e);
        }
    }


}

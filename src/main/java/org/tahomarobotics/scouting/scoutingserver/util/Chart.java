package org.tahomarobotics.scouting.scoutingserver.util;

import javafx.collections.FXCollections;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import org.tahomarobotics.scouting.scoutingserver.Constants;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.tahomarobotics.scouting.scoutingserver.DatabaseManager.getAverage;

public class Chart extends StackedBarChart<String, Number> {
    private CategoryAxis categoryAxis;
    private NumberAxis numberAxis;

    private String activeTable;
    private List<Constants.SQLColumnName> cols;
    public String title;
    public Chart(String a, List<Constants.SQLColumnName> c, CategoryAxis xAxis, NumberAxis numberAxis, String t) {
        super(xAxis, numberAxis);
        this.categoryAxis = xAxis;
        this.numberAxis = numberAxis;
        this.activeTable = a;
        this.cols = c;
        this.title = t;
        this.setTitle(t);


    }//end constructor

/*    public void update() throws SQLException {
        getData().clear();
        categoryAxis.getCategories().clear();
        ArrayList<HashMap<String, Object>> teamsScouted = SQLUtil.exec("SELECT DISTINCT " + Constants.SQLColumnName.TEAM_NUM + " FROM \"" + activeTable + "\"", true);
        ArrayList<String> teamNumbers = new ArrayList<>();
        teamsScouted.forEach(map -> {teamNumbers.add(map.get(Constants.SQLColumnName.TEAM_NUM.toString()).toString());});
        categoryAxis.setCategories(FXCollections.observableArrayList(teamNumbers));

        numberAxis.setTickLength(1);
        HashMap<String, Double> ranks = new HashMap<>();
        for (Constants.SQLColumnName col : cols) {
            XYChart.Series<String, Number> series = new Series<>();
            series.setName(col.name().replaceAll("_", " ").toLowerCase());



            for (String teamNumber : teamNumbers) {
                double average = getAverage(col, teamNumber,activeTable, false);
                series.getData().add(new XYChart.Data<>(teamNumber, average));
                double val = ranks.getOrDefault(teamNumber, 0.0);
                ranks.remove(teamNumber);
                ranks.put(teamNumber, val + average);
            }
            getData().add(series);
        }

        categoryAxis.getCategories().sort((o1, o2) -> Double.compare(ranks.get(o2), ranks.get(o1)));
        this.prefHeightProperty().bind(Constants.UIValues.databaseHeightProperty());
        this.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
    }*/


}

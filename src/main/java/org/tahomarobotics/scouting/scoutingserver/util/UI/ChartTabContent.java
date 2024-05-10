package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.layout.VBox;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.configuration.DataMetric;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChartTabContent extends GenericTabContent{


    private CategoryAxis categoryAxis;
    private NumberAxis numberAxis;

    private List<Constants.SQLColumnName> cols;
    public String title;
    private final VBox content = new VBox();
    public ChartTabContent(String name, Optional<File> saveLocation, Optional<File> dataSourceFile, ArrayList<DataMetric> metricsToGraph) {
        super(name, saveLocation);
    }

    @Override
    public Node getContent() {
        return content;
    }

    @Override
    public void updateDisplay() {

    }

    @Override
    public Constants.TabType getTabType() {
        return null;
    }

    @Override
    public void save() {

    }

    @Override
    public void saveAs() {

    }
}

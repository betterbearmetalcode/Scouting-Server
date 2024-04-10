package org.tahomarobotics.scouting.scoutingserver.util.auto;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.controlsfx.control.CheckTreeView;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;
import org.tahomarobotics.scouting.scoutingserver.util.SQLUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.sql.SQLException;
import java.util.*;

public class AutoHeatmap {



    //correlates robot position with each robot's autos
    //each robot is represented by its team number and a list of all its autos
    private final HashMap<DatabaseManager.RobotPosition, TeamAutoHistory> heatmap;
    private final CheckTreeView<String> redAllianceView = new CheckTreeView<>();
    private final CheckTreeView<String> blueAllianceView = new CheckTreeView<>();
    private final VBox displayBox;
    public AutoHeatmap(HeatmapCreationInformation input) throws SQLException, IOException {
        heatmap = new HashMap<>();
        displayBox = new VBox();
        //for all the data add autos to the heatmap as appropriate;
        //TODO add AI to recognize autos and correct scouting mistakes
            for (DatabaseManager.RobotPosition robotPosition : input.teams().keySet()) {
                Color teamColor = colors.get(robotPosition);
                for (String tableName : input.dataTables()) {
                    String teamNumber = input.teams().get(robotPosition);
                    //get all the auto data for this team in each cable in a loop but dont get duplicate autos
                    ArrayList<HashMap<String, Object>> teamAutoData = SQLUtil.exec("SELECT " +
                            Constants.SQLColumnName.TEAM_NUM + ", " +
                            Constants.SQLColumnName.NOTE_1 + ", " +
                            Constants.SQLColumnName.NOTE_2 + ", " +
                            Constants.SQLColumnName.NOTE_3 + ", " +
                            Constants.SQLColumnName.NOTE_4 + ", " +
                            Constants.SQLColumnName.NOTE_5 + ", " +
                            Constants.SQLColumnName.NOTE_6 + ", " +
                            Constants.SQLColumnName.NOTE_7 + ", " +
                            Constants.SQLColumnName.NOTE_8 + ", " +
                            Constants.SQLColumnName.NOTE_9 + " FROM \"" + tableName + "\" WHERE TEAM_NUM=?", new Object[]{teamNumber}, true);

                    if (teamAutoData.isEmpty()) {
                        addTeamWithNoData(robotPosition, teamNumber);
                        continue;
                    }
                    //add each auto to the heatmap
                    for (HashMap<String, Object> teamAutoDatum : teamAutoData) {
                        ArrayList<AutoPath.Note> notes = new ArrayList<>();
                        //loop through the sql data and add notes to the array
                        teamAutoDatum.keySet().forEach(s -> {
                            if (!Objects.equals(s, Constants.SQLColumnName.TEAM_NUM.name())) {
                                //if this is a column relating to the auto data
                                //possible values should be 0,1,2
                                if (Integer.parseInt(teamAutoDatum.get(s).toString()) == 1 || Integer.parseInt(teamAutoDatum.get(s).toString()) == 2) {
                                    //if the note was collected successfully or unsuccessfully
                                    notes.add(AutoPath.getNoteFromSQLColumn(Constants.SQLColumnName.valueOf(s), robotPosition.ordinal() < 3));
                                }

                            }
                        });
                        //sort the notes to standardize auto names as souting data does not tell us when things are collected unfortunatly
                        notes.sort(Comparator.comparingInt(Enum::ordinal));
                        addAuto(robotPosition, new AutoPath(teamNumber, notes, robotPosition, teamColor));
                    }//end for each sql column

                }//end for each data table

            }//end for each team

        //now we have initalized the heatmap variable, so now we need to create the GUI and render stuff
        setUpGUI();
    }


    private void addAuto(DatabaseManager.RobotPosition position, AutoPath auto) {
        if (heatmap.containsKey(position)) {
            //if there is at least one auto for this team already
            TeamAutoHistory history = heatmap.get(position);
            //have to use weird comparison becuase the object memory values are not equal so contains key does not work
            if (history.hasAuto(auto)) {
                //if we already have this auto for this team the increment its frequency
                //if this auto is already added, increment its frequency value
                int frequency = history.paths().get(auto);
                history.paths().replace(auto, ++frequency);
                heatmap.replace(position, history);
                auto.setVisible(true);//make the auto visible because it now has a frequency greater than one
            }else {
                //add it as a new auto for this team

                //if a team has no data at one competiton, then paths could be null even though they have data to add here at a different competion.
                HashMap<AutoPath, Integer> paths = history.paths();
                if (paths == null) {
                    paths = new HashMap<>();//only initialize it if its null becuase we don't want to delete data
                }
                paths.put(auto, 1);

                auto.setVisible(false);//if the auto has a frequency of one, don't have it visible at first becuase it makes the heatmaps confusing
                heatmap.replace(position, new TeamAutoHistory(auto.getTeamNumber(), paths, auto.getColor()));
            }

        }else {
           //if there are no entries for this team yet, make a new one
            HashMap<AutoPath, Integer> paths = new HashMap<>();
            paths.put(auto, 1);
            heatmap.put(position, new TeamAutoHistory(auto.getTeamNumber(), paths, auto.getColor()));
        }
    }

    private void addTeamWithNoData(DatabaseManager.RobotPosition position, String teamNumber) {
        //if there is already data there from different databases, then it could get overwritten by this
        if (!heatmap.containsKey(position)) {
            heatmap.put(position, new TeamAutoHistory(teamNumber, null, colors.get(position)));
        }

    }



    private void setUpGUI() throws IOException {

        redAllianceView.setRoot(new CheckBoxTreeItem<>("root"));
        redAllianceView.setShowRoot(false);
        redAllianceView.getRoot().setExpanded(true);
        blueAllianceView.setRoot(new CheckBoxTreeItem<>("root"));
        blueAllianceView.setShowRoot(false);
        blueAllianceView.getRoot().setExpanded(true);
        redAllianceView.setCellFactory(tv ->  new ColoredCheckboxTreeCell());
        blueAllianceView.setCellFactory(tv -> new ColoredCheckboxTreeCell());


        for (DatabaseManager.RobotPosition value : DatabaseManager.RobotPosition.values()) {
            TeamAutoHistory history = heatmap.get(value);
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>(value.name() + ": " + history.teamNumber());
            ((value.ordinal() < 3)?redAllianceView:blueAllianceView).getRoot().getChildren().add(item);

            //if we have no data for this team, skip adding the autos
            if (history.paths() == null) {
                item.setValue(item.getValue() + " NO DATA");
                continue;
            }

            ArrayList<Pair<AutoPath, Integer>> pathsToSort = new ArrayList<>();
            //loop through all the autos we have and add them to the list
            history.paths().keySet().forEach(path -> {
               pathsToSort.add(new Pair<>(path, history.paths().get(path)));
            });
            pathsToSort.sort(Comparator.comparingInt(Pair::getValue));
            Collections.reverse(pathsToSort);
            pathsToSort.forEach(pair -> {
                CheckBoxTreeItem<String> leafItem = new CheckBoxTreeItem<>(pair.getKey().toString() +":Frequency: " +  pair.getValue());
                leafItem.setSelected(pair.getKey().isVisible());
                item.getChildren().add(leafItem);
                leafItem.selectedProperty().addListener((observable, oldValue, newValue) -> updateImage());
            });
            item.selectedProperty().addListener((observable, oldValue, newValue) -> updateImage());


        }

        blueAllianceView.prefWidthProperty().bind(Constants.UIValues.halfSplitWidthProperty());
        redAllianceView.prefWidthProperty().bind(Constants.UIValues.halfSplitWidthProperty());
        HBox treeBox = new HBox(new VBox(new Label("Blue alliance Autos:"), blueAllianceView), new Separator(Orientation.VERTICAL), new VBox(new Label("Red alliance autos: "), redAllianceView));
        treeBox.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
        treeBox.setSpacing(10);
        treeBox.setAlignment(Pos.CENTER);
        updateImage(true);
        displayBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        displayBox.getChildren().add(treeBox);
        displayBox.setAlignment(Pos.CENTER);

    }



    private void updateImage(boolean setIntialStates) {
        Logging.logInfo("updating image");
        //first update the visibility field for all the autos based on the tree views,
        //then replace the current image with a new one by calling get rendered image again.
        //this method should be hooked to the change listeneres for the tree views
        try {
            for (DatabaseManager.RobotPosition robotPosition : heatmap.keySet()) {
                //for each robot in the heatmap and for each branch item in both tree views
                CheckTreeView<String> view = robotPosition.ordinal() < 3?redAllianceView:blueAllianceView;
                CheckBoxTreeItem<String> robotItem = (CheckBoxTreeItem<String>) view.getRoot().getChildren().get((robotPosition.ordinal()%3));
                TeamAutoHistory history = heatmap.get(robotPosition);
                if (history.paths() != null) {

                    ArrayList<TreeItem<String>> checkItems = new ArrayList<>(view.getCheckModel().getCheckedItems().stream().toList());

                    history.paths().keySet().forEach(path -> {
                        //if the correspodning child of robotItem is checked, set visible true
                        robotItem.getChildren().forEach(stringTreeItem -> {
                            //for each of the leaf items
                            if (AutoPath.fromString(stringTreeItem.getValue()).hashCode() == path.hashCode()) {
                                //if this tree item represents the path we are working on right now
                                if (setIntialStates) {
                                    boolean shouldBeVisible = history.paths().get(path) > 1;
                                    path.setVisible(shouldBeVisible);
                                    if (shouldBeVisible) {
                                        view.getCheckModel().check(stringTreeItem);
                                    }else {
                                        view.getCheckModel().clearCheck(stringTreeItem);
                                    }

                                }else {
                                    path.setVisible(checkItems.contains(stringTreeItem));
                                }

                            }

                        });
                    });
                }
            }
            displayBox.getChildren().removeIf(node -> node.getClass() == ImageView.class);
            displayBox.getChildren().add(0, new ImageView(getRenderedImage()));


        }catch (NullPointerException | IOException e) {
            //ignored becuase it doesn't matter
            Logging.logError(e);
        }

    }
    private void updateImage() {
        updateImage(false);
    }

    public VBox getDisplayBox() throws IOException{
        return displayBox;
    }

    public Image getRenderedImage() throws IOException {
        BufferedImage output = ImageIO.read(new File("resources/fieldpictureNoBackground.png"));
        Graphics2D g = output.createGraphics();

        for (DatabaseManager.RobotPosition robotPosition : heatmap.keySet()) {
            TeamAutoHistory history = heatmap.get(robotPosition);
            if (history.paths() == null) {
                continue;
            }
            for (AutoPath autoPath : history.paths().keySet()) {
                //on the graph, stroke will indicate frequency
                //opacity and color will vary to differentiate autos drawn on top of eachother.
                if (autoPath.isVisible()) {
                    drawAuto(autoPath, history.paths().get(autoPath) , 255, g);
                }



            }
        }

        return getImage(output);
    }




    private void drawAuto(AutoPath path, int frequency, int alpha, Graphics2D g) {
        Color color = path.getColor();
        int stroke = (int) Math.round(frequency * 1.5);
        if (!path.isNoAuto()) {
            int diameter = stroke * 2;
            drawCircleAtNote(path.getPath().get(0),path.getStartingPosition(),  diameter, setAlpha(color, alpha), g);
            for (int i = 0; i < path.getPath().size() - 1; i++) {
                drawCircleAtNote(path.getPath().get(i+1), path.getStartingPosition(), diameter, setAlpha(color, alpha), g);
                drawLineBetweenTwoNotes(path.getPath().get(i), path.getPath().get(i + 1), path.getStartingPosition(), stroke, setAlpha(color, alpha), g);
            }
        }else {
            g.setPaint(color);
            g.setStroke(new BasicStroke(stroke));
            g.drawString("No Auto x" + frequency, (int) robotPositions.get(path.getStartingPosition()).getX(), (int) robotPositions.get(path.getStartingPosition()).getY());
        }

    }

    private void drawCircleAtNote(AutoPath.Note note, DatabaseManager.RobotPosition robotPosition, double diameter, java.awt.Color color, Graphics2D g) {
        g.setPaint(color);
        Point topLeft = new Point( (int) Math.round(getOffsetLocation(note, robotPosition).getX() - (diameter/2)),  (int) (Math.round(getOffsetLocation(note, robotPosition).getY() - (diameter/2))));
        Point widthHeight = new Point( (int) Math.round(diameter),  (int) (Math.round(diameter)));
        g.fillOval(topLeft.x, topLeft.y, widthHeight.x, widthHeight.y);
    }

    private void drawLineBetweenTwoNotes(AutoPath.Note start, AutoPath.Note end, DatabaseManager.RobotPosition robotPosition, int stroke, Color color, Graphics2D g) {
        g.setPaint(color);
        g.setStroke(new BasicStroke(stroke));
        Point startPoint = getOffsetLocation(start,robotPosition);
        Point endPoint = getOffsetLocation(end,robotPosition);
        g.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);

    }

    //copied from stack overflow hehe https://stackoverflow.com/questions/30970005/bufferedimage-to-javafx-image
    private Image getImage(BufferedImage img){
        //converting to a good type, read about types here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
        BufferedImage newImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        newImg.createGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);

        //converting the BufferedImage to an IntBuffer
        int[] type_int_agrb = ((DataBufferInt) newImg.getRaster().getDataBuffer()).getData();
        IntBuffer buffer = IntBuffer.wrap(type_int_agrb);

        //converting the IntBuffer to an Image, read more about it here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
        PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
        PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer(newImg.getWidth(), newImg.getHeight(), buffer, pixelFormat);
        return new WritableImage(pixelBuffer);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Begin auto heatmap==============\n");
        for (DatabaseManager.RobotPosition robotPosition : heatmap.keySet()) {
            builder.append("Autos for team: ");
            TeamAutoHistory history = heatmap.get(robotPosition);
            builder.append(heatmap.get(robotPosition).teamNumber()).append("\n");
            if (history.paths() == null) {
                builder.append("NO DATA\n");
            }else {
                for (AutoPath autoPath : history.paths().keySet()) {
                    builder.append("    ").append(autoPath).append(" Frequency: ").append(history.paths().get(autoPath)).append("\n");
                }
            }

        }
        builder.append("Ene auto heatmap===============");
        return builder.toString();
    }

    private static final HashMap<AutoPath.Note, Point> noteLocations = new HashMap<>();
    private static final HashMap<DatabaseManager.RobotPosition, Point> robotPositions = new HashMap<>();

    public static HashMap<DatabaseManager.RobotPosition, Color> colors = new HashMap<>();


    //use this to offset where the note's precived center is so you can see more than one auto on the heatmap
    private static final HashMap<DatabaseManager.RobotPosition, Point> offsets = new HashMap<>();


    private static final int offsetRadius = 10;
    static {
        noteLocations.put(AutoPath.Note.ONE, new Point(416, 92));
        noteLocations.put(AutoPath.Note.TWO, new Point(416, 148));
        noteLocations.put(AutoPath.Note.THREE, new Point(416, 200));
        noteLocations.put(AutoPath.Note.FOUR, new Point(416, 256));
        noteLocations.put(AutoPath.Note.FIVE, new Point(416, 310));
        noteLocations.put(AutoPath.Note.RED_A, new Point(590, 108));
        noteLocations.put(AutoPath.Note.RED_B, new Point(590, 154));
        noteLocations.put(AutoPath.Note.RED_C, new Point(590, 200));
        noteLocations.put(AutoPath.Note.BLUE_A, new Point(242, 108));
        noteLocations.put(AutoPath.Note.BLUE_B, new Point(242, 154));
        noteLocations.put(AutoPath.Note.BLUE_C, new Point(242, 200));

        robotPositions.put(DatabaseManager.RobotPosition.B1, new Point(165,100));
        robotPositions.put(DatabaseManager.RobotPosition.B2, new Point(165,200));
        robotPositions.put(DatabaseManager.RobotPosition.B3, new Point(165,270));
        robotPositions.put(DatabaseManager.RobotPosition.R1, new Point(628,100));
        robotPositions.put(DatabaseManager.RobotPosition.R2, new Point(628,200));
        robotPositions.put(DatabaseManager.RobotPosition.R3, new Point(628,270));

        colors.put(DatabaseManager.RobotPosition.R1,Color.RED);
        colors.put(DatabaseManager.RobotPosition.R2,Color.BLUE);
        colors.put(DatabaseManager.RobotPosition.R3,Color.GREEN);
        colors.put(DatabaseManager.RobotPosition.B1,Color.ORANGE);
        colors.put(DatabaseManager.RobotPosition.B2,Color.MAGENTA);
        colors.put(DatabaseManager.RobotPosition.B3,Color.BLACK);
      //make the offsets a circle of points around the goal
        //calculate change in angle for each color
        double deltaTheta = 2 * Math.PI/DatabaseManager.RobotPosition.values().length;//radians
        double theta = 0;
        for (DatabaseManager.RobotPosition value : DatabaseManager.RobotPosition.values()) {
            int xOffset = (int) Math.round(Math.cos(theta) * offsetRadius);
            int yOffset = (int) Math.round(Math.sin(theta) * offsetRadius);
            theta += deltaTheta;
            offsets.put(value, new Point(xOffset, yOffset));
        }


    }



    public Point getOffsetLocation(AutoPath.Note note, DatabaseManager.RobotPosition robotPosition) {
        Point center = noteLocations.get(note);
        Point offset = offsets.get(robotPosition);
        return new Point(center.x + offset.x, center.y + offset.y);
    }

    //because the color class can't set alpha
    private Color setAlpha(Color color, int alpha) {
        return new Color(color.getRed(),color.getGreen(),  color.getBlue(), alpha);
    }

    private javafx.scene.paint.Color convertColor(java.awt.Color awtColor) {
        int r = awtColor.getRed();
        int g = awtColor.getGreen();
        int b = awtColor.getBlue();
        int a = awtColor.getAlpha();
        double opacity = a / 255.0 ;
        return javafx.scene.paint.Color.rgb(r, g, b, opacity);

    }

    private static class ColoredCheckboxTreeCell extends CheckBoxTreeCell<String> {

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText("");
            } else {
                setText(item); // appropriate text for item
                DatabaseManager.RobotPosition position = DatabaseManager.RobotPosition.valueOf(item.split(":")[0]);
                Color color = colors.get(position);
                String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                String styleClass = " -fx-text-fill: " + hex ; // choose style class for item
                setStyle(styleClass);
            }
        }

    }//end inner class

}

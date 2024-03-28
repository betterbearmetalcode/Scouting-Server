package org.tahomarobotics.scouting.scoutingserver.util.auto;

import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
        for (String tableName : input.dataTables()) {
            for (DatabaseManager.RobotPosition robotPosition : input.teams().keySet()) {
                String teamNumber = input.teams().get(robotPosition);
                //get all the auto data for this team in each cable in a loop but dont get duplicate autos
                ArrayList<HashMap<String, Object>> teamAutoData = SQLUtil.exec("SELECT " +
                        Constants.SQLColumnName.TEAM_NUM + ", " +
                        Constants.SQLColumnName.F1 + ", " +
                        Constants.SQLColumnName.F2 + ", " +
                        Constants.SQLColumnName.F3 + ", " +
                        Constants.SQLColumnName.M1 + ", " +
                        Constants.SQLColumnName.M2 + ", " +
                        Constants.SQLColumnName.M3 + ", " +
                        Constants.SQLColumnName.M4 + ", " +
                        Constants.SQLColumnName.M5 + " FROM \"" + tableName + "\" WHERE TEAM_NUM=?", new Object[]{teamNumber}, true);

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
                    addAuto(robotPosition, new AutoPath(teamNumber, notes, robotPosition));
                }//end for each sql column

            }//end for each team
        }//end for each data table

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
            }else {
                //add it as a new auto for this team
                HashMap<AutoPath, Integer> paths = history.paths();
                paths.put(auto, 1);
                heatmap.replace(position, new TeamAutoHistory(auto.getTeamNumber(), paths));
            }

        }else {
           //if there are no entries for this team yet, make a new one
            HashMap<AutoPath, Integer> paths = new HashMap<>();
            paths.put(auto, 1);
            heatmap.put(position, new TeamAutoHistory(auto.getTeamNumber(), paths));
        }
    }

    private void addTeamWithNoData(DatabaseManager.RobotPosition position, String teamNumber) {
        heatmap.put(position, new TeamAutoHistory(teamNumber, null));
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
                    drawAuto(autoPath, history.paths().get(autoPath) + 2, 255, g);
                }



            }
        }

        return getImage(output);
    }




    private void drawAuto(AutoPath path, int stroke, int alpha, Graphics2D g) {
        Color color = path.getColor();
        if (!path.isNoAuto()) {
            int diameter = stroke * 2;
            drawCircleAtNote(path.getPath().get(0), diameter, setAlpha(color, alpha), g);
            for (int i = 0; i < path.getPath().size() - 1; i++) {
                drawCircleAtNote(path.getPath().get(i+1), diameter, setAlpha(color, alpha), g);
                drawLineBetweenTwoNotes(path.getPath().get(i), path.getPath().get(i + 1), stroke, setAlpha(color, alpha), g);
            }
        }else {
            g.setPaint(color);
            g.setStroke(new BasicStroke(stroke));
            g.drawString("No Auto", (int) robotPositions.get(path.getStartingPosition()).getX(), (int) robotPositions.get(path.getStartingPosition()).getY());
        }

    }

    private void drawCircleAtNote(AutoPath.Note note,double diameter, Color color, Graphics2D g) {
        g.setPaint(color);
        Point topLeft = new Point( (int) Math.round(getOffsetLocation(note, color).getX() - (diameter/2)),  (int) (Math.round(getOffsetLocation(note, color).getY() - (diameter/2))));
        Point widthHeight = new Point( (int) Math.round(diameter),  (int) (Math.round(diameter)));
        g.fillOval(topLeft.x, topLeft.y, widthHeight.x, widthHeight.y);
    }

    private void drawLineBetweenTwoNotes(AutoPath.Note start, AutoPath.Note end, int stroke, Color color,  Graphics2D g) {
        g.setPaint(color);
        g.setStroke(new BasicStroke(stroke));
        Point startPoint = getOffsetLocation(start,color);
        Point endPoint = getOffsetLocation(end,color);
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
    private static final ArrayList<Color> colors = new ArrayList<>();


    //use this to offset where the note's precived center is so you can see more than one auto on the heatmap
    private static final HashMap<Color, Point> colorOffsets = new HashMap<>();
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

        colors.add(java.awt.Color.RED);
        colors.add(java.awt.Color.BLUE);
        colors.add(java.awt.Color.GREEN);
        colors.add(java.awt.Color.WHITE);
        colors.add(java.awt.Color.YELLOW);
        colors.add(Color.BLACK);
        colors.add(java.awt.Color.ORANGE);
        colors.add(java.awt.Color.PINK);

        //make the offsets a circle of points around the goal
        //calculate change in angle for each color
        double deltaTheta = 2 * Math.PI/colors.size();//radians
        double theta = 0;
        for (Color color : colors) {
            int xOffset = (int) Math.round(Math.cos(theta) * offsetRadius);
            int yOffset = (int) Math.round(Math.sin(theta) * offsetRadius);
            theta += deltaTheta;
            colorOffsets.put(color, new Point(xOffset, yOffset));
        }


    }

    private static int i = 0;
    public static Color getNextColor() {
        Color color = colors.get(i);

        i++;
        if (i == colors.size()) {
            i = 0;
        }
        return color;
    }

    public Point getOffsetLocation(AutoPath.Note note, Color color) {
        Point center = noteLocations.get(note);
        Point offset = colorOffsets.get(color);
        return new Point(center.x + offset.x, center.y + offset.y);
    }

    //because the color class can't set alpha
    private Color setAlpha(Color color, int alpha) {
        return new Color(color.getRed(),color.getGreen(),  color.getBlue(), alpha);
    }

    private void setUpGUI() throws IOException {

        redAllianceView.setRoot(new CheckBoxTreeItem<>("root"));
        redAllianceView.setShowRoot(false);
        redAllianceView.getRoot().setExpanded(true);
        blueAllianceView.setRoot(new CheckBoxTreeItem<>("root"));
        blueAllianceView.setShowRoot(false);
        blueAllianceView.getRoot().setExpanded(true);
        for (DatabaseManager.RobotPosition value : DatabaseManager.RobotPosition.values()) {
            TeamAutoHistory history = heatmap.get(value);
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>(value.name() + ": " + history.teamNumber());
            ((value.ordinal() < 3)?redAllianceView:blueAllianceView).getRoot().getChildren().add(item);

            //if we have no data for this team, skip adding the autos
            if (history.paths() == null) {
                item.setValue(item.getValue() + " NO DATA");
                continue;
            }
            //loop through all the autos we have and add them to the list
            history.paths().keySet().forEach(path -> {
                CheckBoxTreeItem<String> leafItem = new CheckBoxTreeItem<>(path.toString());
                leafItem.setSelected(path.isVisible());
                item.getChildren().add(leafItem);
            });

        }

        blueAllianceView.prefWidthProperty().bind(Constants.UIValues.halfSplitWidthProperty());
        redAllianceView.prefWidthProperty().bind(Constants.UIValues.halfSplitWidthProperty());
        blueAllianceView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) c -> udpateImage());
        redAllianceView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) c -> udpateImage());
        HBox treeBox = new HBox(new VBox(new Label("Blue alliance Autos:"), blueAllianceView), new Separator(Orientation.VERTICAL), new VBox(new Label("Red alliance autos: "), redAllianceView));
        treeBox.prefWidthProperty().bind(Constants.UIValues.splitWidthPropertyProperty());
        treeBox.setSpacing(10);
        treeBox.setAlignment(Pos.CENTER);
        redAllianceView.getCheckModel().checkAll();
        blueAllianceView.getCheckModel().checkAll();
        udpateImage();
        displayBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        displayBox.getChildren().add(treeBox);
        displayBox.setAlignment(Pos.CENTER);

    }

    private void udpateImage() {
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
                        //if the correspodning child of robotItem is checket, set visible true
                        robotItem.getChildren().forEach(stringTreeItem -> {
                            if (AutoPath.fromString(stringTreeItem.getValue()).hashCode() == path.hashCode()) {
                                path.setVisible(checkItems.contains(stringTreeItem));
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
}

package org.tahomarobotics.scouting.scoutingserver.util.auto;

import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class AutoHeatmap {



    //correlates robot position with each robot's autos
    //each robot is represented by its team number and a list of all its autos
    private final HashMap<DatabaseManager.RobotPosition, TeamAutoHistory> heatmap = new HashMap<>();



    public void addAuto(DatabaseManager.RobotPosition position, AutoPath auto) {
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

    public void addTeamWithNoData(DatabaseManager.RobotPosition position, String teamNumber) {
        heatmap.put(position, new TeamAutoHistory(teamNumber, null));
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


    public Image getRenderedImage() throws IOException {
        BufferedImage output = ImageIO.read(new File("resources/fieldpictureNoBackground.png"));
        Graphics2D g = output.createGraphics();

        for (DatabaseManager.RobotPosition robotPosition : heatmap.keySet()) {
            TeamAutoHistory history = heatmap.get(robotPosition);
            int i =  0;
            for (AutoPath autoPath : history.paths().keySet()) {
                //on the graph, stroke will indicate frequency
                //opacity and color will vary to differentiate autos drawn on top of eachother.
                drawAuto(autoPath, history.paths().get(autoPath) + 2, colors.get(i), (int) Math.floor(Math.random() * 200) + 55, g);

                i++;
                if (i == colors.size()) {
                    i = 0;
                }
            }
        }

        return getImage(output);
    }


    private void drawAuto(AutoPath path, int stroke, Color color, int alpha, Graphics2D g) {
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
        Point topLeft = new Point( (int) Math.round(noteLocations.get(note).getX() - (diameter/2)),  (int) (Math.round(noteLocations.get(note).getY() - (diameter/2))));
        Point widthHeight = new Point( (int) Math.round(diameter),  (int) (Math.round(diameter)));
        g.fillOval(topLeft.x, topLeft.y, widthHeight.x, widthHeight.y);
    }

    private void drawLineBetweenTwoNotes(AutoPath.Note start, AutoPath.Note end, int stroke, Color color,  Graphics2D g) {
        g.setPaint(color);
        g.setStroke(new BasicStroke(stroke));
        Point startPoint = noteLocations.get(start);
        Point endPoint = noteLocations.get(end);
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

    private static HashMap<AutoPath.Note, Point> noteLocations = new HashMap<>();
    private static HashMap<DatabaseManager.RobotPosition, Point> robotPositions = new HashMap<>();

    private static ArrayList<Color> colors = new ArrayList<>();

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

        colors.add(Color.RED);
        colors.add(Color.BLUE);
        colors.add(Color.GREEN);
        colors.add(Color.WHITE);
        colors.add(Color.YELLOW);
        colors.add(Color.BLUE);
        colors.add(Color.ORANGE);
        colors.add(Color.PINK);


    }

    //because the color class can't set alpha
    private Color setAlpha(Color color, int alpha) {
        return new Color(color.getRed(),color.getGreen(),  color.getBlue(), alpha);
    }
}

package org.tahomarobotics.scouting.scoutingserver.util.auto;

import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

public class AutoPath {


    public AutoPath(String teamNumber, ArrayList<Note> path, DatabaseManager.RobotPosition startingPosition, Color theColor) {
        this.teamNumber = teamNumber;
        this.path = path;
        isNoAuto = path.isEmpty();
        this.startingPosition = startingPosition;
        this.color = theColor;
    }

    private final String teamNumber;
    private final ArrayList<Note> path;
    private final boolean isNoAuto;
    private final DatabaseManager.RobotPosition startingPosition;

    private final Color color;

    private boolean isVisible = true;



    public boolean isNoAuto() {
        return isNoAuto;
    }



    public String getTeamNumber() {
        return teamNumber;
    }


    public ArrayList<Note> getPath() {
        return path;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(startingPosition).append(":");
        builder.append(teamNumber).append(":");
        builder.append(" Auto:");
        if (isNoAuto) {
            builder.append("NO AUTO");
        }else {
            for (Note note : path) {
                builder.append(getDisplayStringFromNote(note));
            }
        }

        return builder.toString();
    }

    public static AutoPath fromString(String str) {
        String[] tokens = str.split(":");
        DatabaseManager.RobotPosition pos = DatabaseManager.RobotPosition.valueOf(tokens[0]);
        String teamNum = tokens[1];
        String[] notes = tokens[3].split("");
        boolean hasAuto = !(Objects.equals(tokens[3], "NO AUTO"));
        ArrayList<Note> thePath = new ArrayList<>();
        if (hasAuto) {
            for (String note : notes) {
                thePath.add(getNoteFromDisplayString(note, pos.ordinal() < 3));
            }
        }
        return new AutoPath(teamNum, thePath, pos, Color.GRAY);//this path is used for comparison purposers and its color does not matter
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AutoPath autoPath = (AutoPath) o;

        if (!teamNumber.equals(autoPath.teamNumber)) return false;
        return path.equals(autoPath.path);
    }

    @Override
    public int hashCode() {
        int result = teamNumber.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    public DatabaseManager.RobotPosition getStartingPosition() {
        return startingPosition;
    }

    public Color getColor() {
        return color;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public enum Note {
        RED_A,
        RED_B,
        RED_C,
        BLUE_A,
        BLUE_B,
        BLUE_C,
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE
    }

    public static Note getNoteFromSQLColumn(Constants.SQLColumnName column, boolean red) {
        switch (column) {
            case F1 -> {
                return red?Note.RED_A:Note.BLUE_A;
            }
            case F2 -> {
                return red?Note.RED_B:Note.BLUE_B;
            }
            case F3 -> {
                return red?Note.RED_C:Note.BLUE_C;
            }
            case M1 -> {
                return Note.ONE;
            }
            case M2 -> {
                return Note.TWO;
            }
            case M3 -> {
                return Note.THREE;
            }
            case M4 -> {
                return Note.FOUR;
            }
            case M5 -> {
                return Note.FIVE;
            }
            default -> throw new IllegalArgumentException("Argument is not a note");


        }
    }

    public static String getDisplayStringFromNote(Note note) {
        switch (note) {

            case RED_A,BLUE_A -> {
                return "a";
            }
            case RED_B,BLUE_B -> {
                return "b";
            }
            case RED_C,BLUE_C -> {
                return "c";
            }
            case ONE -> {
                return "1";
            }
            case TWO -> {
                return "2";
            }
            case THREE -> {
                return "3";
            }
            case FOUR -> {
                return "4";
            }
            case FIVE -> {
                return "5";
            }default -> throw new IllegalArgumentException(":LKASEFJKSDN:SLNE: idk what to say here whatever");
        }
    }

    public static Note getNoteFromDisplayString(String str, boolean red) {
        switch (str) {
            case "a" -> {
                return red?Note.RED_A:Note.BLUE_A;
            }
            case "b" -> {
                return red?Note.RED_B:Note.BLUE_B;
            }
            case "c" -> {
                return red?Note.RED_C:Note.BLUE_C;
            }
            case "1" -> {
                return Note.ONE;
            }
            case "2" -> {
                return Note.TWO;
            }
            case "3" -> {
                return Note.THREE;
            }
            case "4" -> {
                return Note.FOUR;
            }
            case "5" -> {
                return Note.FIVE;
            }default -> throw new IllegalArgumentException("this should never happed");

        }
    }
}

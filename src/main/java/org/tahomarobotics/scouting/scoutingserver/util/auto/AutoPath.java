package org.tahomarobotics.scouting.scoutingserver.util.auto;

import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;

import java.util.ArrayList;

public class AutoPath {


    public AutoPath(String teamNumber, ArrayList<Note> path, DatabaseManager.RobotPosition startingPosition) {
        this.teamNumber = teamNumber;
        this.path = path;
        isNoAuto = path.isEmpty();
        this.startingPosition = startingPosition;
    }

    private final String teamNumber;
    private final ArrayList<Note> path;
    private final boolean isNoAuto;
    private final DatabaseManager.RobotPosition startingPosition;

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
        builder.append("Auto for team: ").append(teamNumber).append(" Notes: ");
        if (isNoAuto) {
            builder.append("NO AUTO");
        }else {
            for (Note note : path) {
                builder.append(note.toString()).append(", ");
            }
        }

        return builder.toString();
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
}

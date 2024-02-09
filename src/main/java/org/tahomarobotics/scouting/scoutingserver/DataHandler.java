package org.tahomarobotics.scouting.scoutingserver.util;

import com.google.zxing.NotFoundException;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Vector;

public class DataHandler {

    private final static ArrayList<MatchRecord> qrData = new ArrayList<>();

    //untested, not used
    public void setUPConnection() throws ClassNotFoundException, SQLException {
        String url = "jdbc:mysql://localhost:3306/table_name"; // table details
        String username = "rootgfg"; // MySQL credentials
        String password = "gfg123";
        String query = "select *from students"; // query to be run
        Class.forName("com.mysql.cj.jdbc.Driver"); // Driver name
        Connection con = DriverManager.getConnection(url, username, password);
        System.out.println("Connection Established successfully");
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query); // Execute query
        rs.next();
        String name = rs.getString("name"); // Retrieve name from db

        System.out.println(name); // Print result on console
        st.close(); // close statement
        con.close(); // close connection
        System.out.println("Connection Closed....");
    }


    public static void storeRawQRData(long timestamp, String dataRaw) throws IOException {
        //adds the data to ram
        qrData.add(contstrucMatchRecord(timestamp, dataRaw));

        //creates new database if there is none already and then writed the data to it
        File data = new File(Constants.DATABASE_FILEPATH);
        if (!data.exists()) {
            data.createNewFile();
        }
            FileWriter fw = new FileWriter(data, true);
            BufferedWriter writer = new BufferedWriter(fw);
            writer.write(timestamp + Constants.STORED_DATA_DELIMITER + dataRaw);
            writer.newLine();
            writer.close();


    }


    private static MatchRecord contstrucMatchRecord(long timestamp, String qrRAW) {
        try {
            String[] data = qrRAW.split(Constants.QR_DATA_DELIMITER);
            MatchRecord m = new MatchRecord(timestamp, Integer.parseInt(data[0]),
                    Integer.parseInt(data[1]),
                    getRobotPositionFromNum(Integer.parseInt(data[2])),
                    Integer.parseInt(data[3]),
                    Integer.parseInt(data[4]),
                    Integer.parseInt(data[5]),
                    Integer.parseInt(data[6]),
                    Integer.parseInt(data[7]),
                    getEngamePositionFromNum(Integer.parseInt(data[8])),
                    (Objects.equals(data[9], "1")),
                    data[10] ,
                    data[11]);
            return m;
        }catch (NumberFormatException e) {
            System.err.println("Failed to construct MatchRecord, likly corruppted Data");
            throw new RuntimeException(e);
        }

    }



    public static ArrayList<MatchRecord> getCachedQRData() throws IOException {
        ArrayList<MatchRecord> output = new ArrayList<>();
        ArrayList<String> qrRaws= new ArrayList<>();
        File database = new File(Constants.DATABASE_FILEPATH);
        if (database.exists()) {
            FileReader reader = new FileReader(database);
            BufferedReader bufferedReader = new BufferedReader(reader);
            long beginTime = System.currentTimeMillis();
            while (true) {
                String line = bufferedReader.readLine();
                if ((line == null) || (line == "")) {
                    break;
                }else if ((beginTime + 1000) < System.currentTimeMillis()) {
                    //break after one second of reading data
                    System.out.println("Reading Database Timeout");
                    break;
                }
                String[] dataTokens = line.split(Constants.STORED_DATA_DELIMITER);
                output.add(contstrucMatchRecord(Long.parseLong(dataTokens[0]), dataTokens[1]));
            }
        return output;
        }else {
            //then the database does not exist, return nothing
            System.out.println("Database not found, returning nothing");
            return output;
        }//end if database exists


    }



    //utility methods

    public enum RobotPosition {
        R1,
        R2,
        R3,
        B1,
        B2,
        B3
    }

    public static int getRobotPositionNum(RobotPosition position) {
        switch (position) {

            case R1 -> {
                return 0;
            }
            case R2 -> {
                return 1;
            }
            case R3 -> {
                return 2;
            }
            case B1 -> {
                return 3;
            }
            case B2 -> {
                return 4;
            }
            case B3 -> {
                return 5;
            }
            default -> throw new IllegalStateException("Unexpected value: " + position);
        }
    }

    public static RobotPosition getRobotPositionFromNum(int num) {
        switch (num) {

            case 0 -> {
                return RobotPosition.R1;
            }
            case 1 -> {
                return RobotPosition.R2;
            }
            case 2 -> {
                return RobotPosition.R3;
            }
            case 3 -> {
                return RobotPosition.B1;
            }
            case 4 -> {
                return RobotPosition.B2;
            }
            case 5 -> {
                return RobotPosition.B3;
            }
            default -> throw new IllegalStateException("Unexpected value: " + num);
        }
    }

    public enum EndgamePosition {
        NONE,
        PARKED,
        CLIMBED,
        HARMONIZED
    }

    public static int getEngamePosition(EndgamePosition pos) {
        switch (pos) {

            case NONE -> {
                return 0;
            }
            case PARKED -> {
                return 1;
            }
            case CLIMBED -> {
                return 2;
            }
            case HARMONIZED -> {
                return 3;
            }
            default -> throw new IllegalStateException("Unexpected value: " + pos);
        }
    }

    public static EndgamePosition getEngamePositionFromNum(int pos) {
        switch (pos) {

            case 0 -> {
                return EndgamePosition.NONE;
            }
            case 1 -> {
                return EndgamePosition.PARKED;
            }
            case 2 -> {
                return EndgamePosition.CLIMBED;
            }
            case 3 -> {
                return EndgamePosition.HARMONIZED;
            }
            default -> throw new IllegalStateException("Unexpected value: " + pos);
        }
    }

    public record MatchRecord(long timestamp,
                              int matchNumber,
                              int teamNumber,
                              RobotPosition position,
                              int autoSpeaker,
                              int autoAmp,
                              int teleSpeaker,
                              int teleAmp,
                              int teleTrap,
                              EndgamePosition endgamePosition,
                              boolean lostComms,
                              String autoNotes,
                              String teleNotes
    ) {

    }


    public static ArrayList<MatchRecord> getMatchData() {
        return qrData;
    }


}

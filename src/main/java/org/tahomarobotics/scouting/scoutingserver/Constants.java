package org.tahomarobotics.scouting.scoutingserver;

public class Constants {
    //qr codes images are cached in this folder. Filenames are the timestamp they were created and file extensions are .bmp
   // public static final String IMAGE_DATA_FILEPATH = "src/main/resources/org/tahomarobotics/scouting/scoutingserver/images/";
    public static final String IMAGE_DATA_FILEPATH = System.getProperty("user.dir") + "/resources/images/";
    //public static final String DATABASE_FILEPATH = "src/main/resources/org/tahomarobotics/scouting/scoutingserver/database/";
    public static final String DATABASE_FILEPATH = System.getProperty("user.dir") + "/resources/database/";
    public static final String DEFAULT_DATABASE_NAME = "jankDatabase.txt";

    public static final String QR_DATA_DELIMITER = "/";

    public static final String STORED_DATA_DELIMITER= "@";
}
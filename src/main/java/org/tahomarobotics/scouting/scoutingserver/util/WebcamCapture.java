package org.tahomarobotics.scouting.scoutingserver.util;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebcamCapture {

    // Parse command line arguments. Available options:
    //
    //		/delay DELAY_IN_MILLISECONDS
    //		/filename OUTPUT_FILENAME
    //		/devnum DEVICE_NUMBER
    //		/devname DEVICE_NAME
    //		/preview
    //		/devlist
    //

    private static String selectedWebcam = "";
    private static final String exeFilepath = System.getProperty("user.dir") + "\\lib\\CommandCam.exe";
    public static void snapshotWebcam(String device) throws InterruptedException, IOException {
        System.out.println("Snapshotting");
        String command = exeFilepath +
                " /devname \"" + device + "\"";
        System.out.println(command);
        execCommand(command);
    }

    public static void snapshotWebcam(String device, boolean preview) throws InterruptedException, IOException {
        System.out.print("Snapshotting: ");
        String command = exeFilepath +
                " /devname \"" + device + "\" " +
                (preview?("/preview ):(""));

        System.out.println(command);
        execCommand(command);
    }

    public static void snapshotWebcam(String device, boolean preview, int delayInMillis) throws InterruptedException, IOException {
        System.out.print("Snapshotting: ");
        String command = exeFilepath +
                " /devname \"" + device + "\" " +
                (preview?("/preview "):("")) +
                "/delay " + delayInMillis;

        System.out.println(command);
        execCommand(command);
    }


    private static String execCommand(String command) throws InterruptedException, IOException {
        final Process p = Runtime.getRuntime().exec(command);
        StringBuilder builder = new StringBuilder();
        new Thread(new Runnable() {
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = null;

                try {
                    while ((line = input.readLine()) != null) {
                        builder.append(line);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        p.waitFor();
        return builder.toString();

    }

    public static ArrayList<String> getDevices() throws IOException, InterruptedException {
        ArrayList<String> deviceNames = new ArrayList<>();
        String input = execCommand(exeFilepath + " /devlist");
        Pattern p = Pattern.compile("Device name: ");
        Matcher m = p.matcher(input);
        int endLastMatch = -1;
        while (m.find()) {
            if (endLastMatch != -1) {
                deviceNames.add(input.substring(endLastMatch, m.start()).trim());
            }

            endLastMatch = m.end();

        }
        deviceNames.add(input.substring(endLastMatch).trim());
        return deviceNames;
    }


    public static void setSelectedWebcam(String str) {
        selectedWebcam = str;
    }

    public static String getSelectedWebcam() {
        return selectedWebcam;
    }
}

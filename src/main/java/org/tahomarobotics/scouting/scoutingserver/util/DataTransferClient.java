package org.tahomarobotics.scouting.scoutingserver.util;

import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.controller.QRScannerController;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class DataTransferClient extends Thread {

    private boolean alive = true;

    private Socket socket;

    public DataTransferClient(Socket sock) throws IOException {
        socket = sock;
        this.start();
    }


    private BufferedReader reader;
    @Override
    public void run() {
        try {


            // Close the socket

            while (alive) {
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);

                // Send a message to the client
                writer.println("Hello from the server!");

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = reader.readLine();
                while (line != null && alive) {
                    line = reader.readLine();
                    System.out.println("Revieved data: " + line);
                    DatabaseManager.storeRawQRData(line, QRScannerController.activeTable);

                }
                socket.close();
            }
            Logging.logInfo("Closing client Connection");
        } catch (IOException ignored) {
            Logging.logError(ignored, "error imprting data probably");
        }
    }

    public void kill() {
        Logging.logInfo("killing client");
        alive = false;
        this.interrupt();
    }
}


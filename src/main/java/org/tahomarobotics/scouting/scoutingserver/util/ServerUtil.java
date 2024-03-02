package org.tahomarobotics.scouting.scoutingserver.util;

import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.controller.QRScannerController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class ServerUtil {

    private static ServerSocket serverSocket;

    private static boolean acceptingConnections = false;

    private static boolean serverThreadRunning = false;

    private static Thread serverThread;

    static {
        try {
            serverSocket = new ServerSocket(Constants.WIRED_DATA_TRANSFER_PORT);
        } catch (IOException e) {
            Logging.logError(e, "Failed to set up wired data transfer server");
        }
    }

    public static ArrayList<DataTransferClient> clients = new ArrayList<>();

    public static InetAddress getInetAddress() {
        return serverSocket.getInetAddress();
    }

    public static void startServer() {
        if (!serverThreadRunning) {
            serverThreadRunning = true;
            //client acceptance loop
            //server loop
            serverThread = new Thread(() -> {
                Logging.logInfo("Server Started");
                while (serverThreadRunning) {
                    while (acceptingConnections) {
                        try {
                            Socket clientSocket = serverSocket.accept();

                            Logging.logInfo("Connection from Client", true);
                            clients.add(new DataTransferClient(clientSocket, QRScannerController.activeTable));
                        } catch (IOException e) {
                            Logging.logError(e, "Failed to accept client connection");
                        }
                    }//client acceptance loop
                }//server loop
                Logging.logInfo("Server Stopped");
            });
            serverThread.start();
            allowConnetions();
        }

    }

    public static void allowConnetions() {
        acceptingConnections = true;

    }

    public static void denyConnections() {
        acceptingConnections = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            if (serverThread != null) {
                serverThread.interrupt();
                serverThreadRunning = false;
                Logging.logError(e, "Failed to stop connections, so stopped server");
            }else {
                Logging.logError(e, "Failed to stop server");
            }

        }
    }

    public static void stopServer() {
        Logging.logInfo("Attempting to stop server");
        denyConnections();
        serverThreadRunning = false;
        clients.forEach(DataTransferClient::kill);
    }
}

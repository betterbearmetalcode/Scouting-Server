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


    public static boolean isServerThreadRunning() {
        return serverThreadRunning;
    }

    private static boolean serverThreadRunning = false;

    private static Thread serverThread;


    private  static final ArrayList<DataTransferClient> clients = new ArrayList<>();

    public static InetAddress getInetAddress() {
        return serverSocket.getInetAddress();
    }

    public static void setServerStatus(boolean running) {
        if (ServerUtil.isServerThreadRunning() != running) {
            //if the server is not in the state we want it
            if (running) {
                ServerUtil.startServer();
            }else {
                ServerUtil.stopServer();
            }
        }
    }

    private static void startServer() {
        if (!serverThreadRunning) {
            serverThreadRunning = true;
            //client acceptance loop
            try {
                serverSocket = new ServerSocket(Constants.WIRED_DATA_TRANSFER_PORT);
            } catch (IOException e) {
                Logging.logError(e, "Failed to set up wired data transfer server");
            }
            //server loop
            serverThread = new Thread(() -> {
                Logging.logInfo("Server Started");
                while (serverThreadRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();

                        Logging.logInfo("Connection from Client", true);
                        clients.add(new DataTransferClient(clientSocket));
                    } catch (IOException e) {
                        if (serverThreadRunning) {
                            Logging.logError(e, "Failed to accept client connection");
                            //if the server thread is not running, then we are trying to close, and this exception is thrown when closing so ignore it
                        }else {
                            Logging.logInfo("Ignoring IO excepion, when shutting down server");
                        }

                    }
                }//server loop
                Logging.logInfo("Server Stopped");
            });
            serverThread.start();
        }

    }



    private static void stopServer() {
        Logging.logInfo("Attempting to stop server");
        serverThreadRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            if (serverThread != null) {
                serverThread.interrupt();
            }

        }
        clients.forEach(DataTransferClient::kill);
    }
}

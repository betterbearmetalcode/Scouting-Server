package org.tahomarobotics.scouting.scoutingserver.util;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.ScoutingServer;
import org.tahomarobotics.scouting.scoutingserver.util.UI.DatabaseViewerTabContent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Optional;

/**
 * There can only be one Data Transfer Server running at a time. This transfer server will add data to a specific sql table
 * imagine this scenario, the user opens a tab and starts a server, then they go to another tab and start another server
 * in this scenario the second tab has to take the server from the other tab. Therefore the start server method will be private and starting a server is done with the
 * {@link ServerUtil#takeServer(String, DatabaseViewerTabContent)} method
 */
public class ServerUtil {


    private static ServerSocket serverSocket;




    private static BooleanProperty serverThreadRunning = new SimpleBooleanProperty(false);

    private static Thread serverThread;

    private static DatabaseViewerTabContent oldContent = null;

    private  static final ArrayList<DataTransferClient> clients = new ArrayList<>();

    static  {
        serverThreadRunning.addListener((observableValue, aBoolean, t1) -> ScoutingServer.dataTransferItem.setText(t1?"Stop Data Transfer Server":"Start Data Transfer Server"));

    }


    /**
     * The take server method is used to start a server. it will stop any and all other threads relating to data transfer and start a new one for the caller
     * so one tab starts a server, and then another tab starts a server, the first tab's server will be stopped and the second tab will get ownership of the server
     *and if there is a reference to the last tab which was open, its status will be updated
     * @param tableName the sql table the transfer client needs to add data to
     * @param caller the object which is calling this method
     */
    public static void takeServer(String tableName, DatabaseViewerTabContent caller) {
        stopServer();
        if (oldContent != null) {
            oldContent.setServerRunning(false);
        }
        startServer(tableName);
        oldContent = caller;
    }

    /**
     * starts a data transfer server thread. The server thread then waits for connections and creates clients to handle them.
     * To be clear this is not a blocking method, have no fear
     * @param tableName the sql table data needs to be added to
     */
    private static void startServer(String tableName) {
        if (!serverThreadRunning.get()) {
            serverThreadRunning.setValue(true);
            //client acceptance loop
            try {
                serverSocket = new ServerSocket(Constants.WIRED_DATA_TRANSFER_PORT);
            } catch (IOException e) {
                Logging.logError(e, "Failed to set up wired data transfer server");
            }
            System.out.println("Starting server");

            //server loop
            serverThread = new Thread(() -> {
                Logging.logInfo("Server Started");
                while (serverThreadRunning.get()) {
                    try {
                        Socket clientSocket = serverSocket.accept();

                        Logging.logInfo("Connection from Client", false);
                        clients.add(new DataTransferClient(clientSocket, tableName));
                    } catch (IOException e) {
                        if (serverThreadRunning.get()) {
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

    /**
     * Stops the server thread and kills all data transfer clients
     */
    public static void stopServer() {

        if (ServerUtil.isServerThreadRunning()) {
            System.out.println("Stopping server");
            Logging.logInfo("Attempting to stop server");
            serverThreadRunning.setValue(false);
            if (oldContent != null) {
                oldContent.setServerRunning(false);
            }
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

    public static boolean isServerThreadRunning() {
        return serverThreadRunning.get();
    }
}

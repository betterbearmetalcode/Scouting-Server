package org.tahomarobotics.scouting.scoutingserver.util;

import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.DatabaseManager;
import org.tahomarobotics.scouting.scoutingserver.controller.DataCollectionController;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.DuplicateDataException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

public class DataTransferClient extends Thread {



    private Socket socket;

    public DataTransferClient(Socket sock) throws IOException {
        socket = sock;
        this.start();
    }


    private BufferedReader reader;
    @Override
    public void run() {
        long start = System.currentTimeMillis();
        Logging.logInfo("Starting Data transfer");
        StringBuilder builder = new StringBuilder();
        try {

            InputStream inputStream = socket.getInputStream();


            reader = new BufferedReader(new InputStreamReader(inputStream));
            builder = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    builder.append(line);
                }else  {
                    break;
                }
            }
            Logging.logInfo("storing data, time elapsed: " + (System.currentTimeMillis() - start));
            ArrayList<DuplicateDataException> duplicates = DatabaseManager.importJSONArrayOfDataObjects(new JSONArray(builder.toString()), DataCollectionController.activeTable);
            DataCollectionController.handleDuplicates(duplicates);

            Logging.logInfo("data is" + builder);
            Logging.logInfo("Closing client Connection");
        } catch (IOException e) {
            Logging.logError(e, "error imprting data probably");
            if (!builder.isEmpty()) {
                Logging.logInfo("Recived some data but failed to recieve all, data saved to logs at " + Constants.BASE_APP_DATA_FILEPATH + "/resources/logs", true);
                Logging.logInfo("Time Elapsed: " + (System.currentTimeMillis() - start));
                Logging.logInfo(builder.toString());
            }

        }finally {
            try {
                socket.close();
            } catch (IOException e) {
                Logging.logError(e);
            }

        }
    }

    public void kill() {
        Logging.logInfo("killing client");
        this.interrupt();
    }
}


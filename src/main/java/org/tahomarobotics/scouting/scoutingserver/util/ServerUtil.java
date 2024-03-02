package org.tahomarobotics.scouting.scoutingserver.util;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerUtil extends Thread {
    private final ServerSocket serverSocket;
    private String message;
    public ServerUtil(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public InetAddress getInetAddress() {
        return serverSocket.getInetAddress();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public String getMessage() {
        return message;
    }

    private void handleClient(Socket socket) throws IOException {
        try {
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);

            // Send a message to the client
            writer.println("Hello from the server!");

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                message = line;
            }

            // Close the socket
            socket.close();
        } catch (IOException ignored) {

        }
    }
}

package org.tahomarobotics.scouting.scoutingserver.util;

import org.tahomarobotics.scouting.scoutingserver.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkingDebug {
    JTextField outgoing;
    JTextArea incoming;
    BufferedReader reader;
    PrintWriter writer;
    Socket sock;
    JTextField nameField;
    String name;
    JFrame nameFrame;
    boolean needName;
    String userName;

    public void go() {
        userName = this.getPort();
        JFrame frame = new JFrame("Ludicrously Simple Chat Client");
        JPanel mainPanel = new JPanel();
        outgoing = new JTextField(20);
        incoming = new JTextArea(15, 20);
        incoming.setLineWrap(true);
        incoming.setWrapStyleWord(true);
        incoming.setEditable(false);
        JScrollPane qScroller = new JScrollPane(incoming);
        qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new SendButtonListener());
        mainPanel.add(qScroller);
        mainPanel.add(outgoing);
        mainPanel.add(sendButton);
        frame.getContentPane().add(mainPanel);
        setUpNetworking();
        Thread readerThread = new Thread(new IncomingReader());
        readerThread.start();
        outgoing.addKeyListener(new EnterListener());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 350);
        frame.setResizable(false);

        frame.setVisible(true);
    }// close go

    private void setUpNetworking() {

        try {
            String[] tokens = name.split(":");
            sock = new Socket(tokens[0], Integer.parseInt(tokens[1]));
            InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
            reader = new BufferedReader(streamReader);
            writer = new PrintWriter(sock.getOutputStream());
            System.out.println("Networking Established");
        } catch (IOException ex) {
            System.out.println("Failed to set up networking");
        }

    }// close set up networking

    public class SendButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                writer.println(outgoing.getText());
                writer.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            outgoing.setText("");
            outgoing.requestFocus();
        }

    }// close inner class

    public class EnterListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent eve) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            // TODO Auto-generated method stub
            if (outgoing.getText() != "" && e.getKeyChar() == KeyEvent.VK_ENTER) {
                try {

                    writer.println(userName + ": " + outgoing.getText());
                    writer.flush();
                } catch (Exception ex) {
                    ex.printStackTrace();

                }
                outgoing.setText("");
                outgoing.requestFocus();
            }

        }

        @Override
        public void keyReleased(KeyEvent e) {
            // TODO Auto-generated method stub

        }

    }

    public class IncomingReader implements Runnable {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            String message;
            try {
                while ((message = reader.readLine()) != null) {

                    System.out.println("recieved: " + message);
                    incoming.append(message + "\n");
                } // close while
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }// close run
    }// close runnable

    public String getPort() {
        name = "";
        nameFrame = new JFrame("enter ip and port");
        JPanel namePanel = new JPanel();
        nameField = new JTextField(20);
        nameField.setText("127.0.0.1:" + Constants.WIRED_DATA_TRANSFER_PORT);
        JButton ok = new JButton("Ok");
        ok.addActionListener(new okListener());
        namePanel.add(nameField);
        namePanel.add(ok);
        nameFrame.getContentPane().add(namePanel);
        nameFrame.setDefaultCloseOperation(nameFrame.DO_NOTHING_ON_CLOSE);
        nameFrame.setSize(300, 100);
        nameFrame.setVisible(true);
        needName = true;
        while (needName) {
            System.out.print("");
        }
        nameFrame.setVisible(false);
        nameFrame.dispose();
        return name;
    }

    public void actuallyGetName() {
        if (nameField.getText() == "" || nameField.getText() == "Please enter your name") {
        } else {
            name = nameField.getText();
            needName = false;
        }
    }

    public class okListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            actuallyGetName();

        }
    }
}

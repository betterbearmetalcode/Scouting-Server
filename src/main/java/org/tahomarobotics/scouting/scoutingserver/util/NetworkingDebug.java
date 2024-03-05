package org.tahomarobotics.scouting.scoutingserver.util;

import org.tahomarobotics.scouting.scoutingserver.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class NetworkingDebug {
    JTextField outgoing;
    JTextArea incoming;
    BufferedReader reader;

    Socket sock;
    JTextField nameField;
    String name;
    JFrame nameFrame;
    boolean needName;
    String userName;
    public JFrame frame;

    public void go() {
        userName = this.getPort();
         frame = new JFrame("Ludicrously Simple Chat Client");
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
            System.out.println("Networking Established");
        } catch (IOException ex) {
            System.out.println("Failed to set up networking");
            ex.printStackTrace();
            frame.dispose();
        }

    }// close set up networking

    public class SendButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            sendData();
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
                    sendData();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    frame.dispose();

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

    public void sendData() {
        try {
            /*String text = outgoing.getText();*/
            String text = "{\n" +
                    "    \"0\":[\"27/4089/0/1/0/0/0/0/0/0/0/0/0/0/0/7/0/0/5/0/No Comments/No Comments\",\n" +
                    "    \"28/1318/0/0/0/1/0/0/0/0/0/0/0/0/0/1/4/0/0/3/No Comments/No Comments\",\n" +
                    "    \"29/9450/0/0/1/0/1/0/0/0/0/0/0/0/0/0/4/0/0/0/No Comments/No Comments\"],\n" +
                    "    \"1\":[\"27/4512/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/No Comments/defended\",\n" +
                    "    \"28/949/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/No Comments/No Comments\",\n" +
                    "    \"29/9036/1/1/0/0/0/0/0/0/0/0/0/0/0/7/0/0/3/0/No Comments/No Comments\",\n" +
                    "    \"32/4915/1/0/0/0/0/0/0/0/0/0/0/0/0/0/4/0/0/2/No Comments/No Comments\"],\n" +
                    "    \"2\":[\n" +
                    "        \"27/5827/2/0/0/1/0/0/0/0/0/0/0/0/0/4/0/0/4/0/No Comments/No Comments\",\n" +
                    "        \"28/2910/2/4/0/1/0/1/1/0/1/0/0/0/0/15/0/0/1/0/jack in the botttttttt!/really good cycles\",\n" +
                    "        \"29/2980/2/1/0/0/0/0/0/0/0/0/0/0/0/5/1/0/5/2/No Comments/No Comments\"\n" +
                    "    ],\n" +
                    "    \"3\":[\"27/4682/3/1/0/0/0/0/0/0/0/0/0/0/0/4/0/0/3/0/No Comments/shot out of the field manuallt aiming, really havd at aiming, \",\n" +
                    "    \"28/4513/3/1/0/0/0/0/0/0/0/0/0/0/0/5/0/0/2/0/No Comments/No Comments\",\n" +
                    "    \"29/7627/3/1/0/0/0/0/0/0/0/0/0/0/0/4/0/0/0/1/No Comments/No Comments\"],\n" +
                    "\n" +
                    "    \"4\":[\"27/2522/4/3/0/1/0/1/1/1/0/0/0/0/0/0/6/0/1/2/No Comments/No Comments\",\n" +
                    "    \"28/5941/4/1/0/0/0/0/0/0/0/0/0/0/0/0/4/0/0/0/No Comments/No Comments\",\n" +
                    "    \"29/3681/4/1/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/No Comments/tank-bad\",\n" +
                    "    \"30/2910/4/5/0/0/0/1/1/1/1/0/0/0/0/10/0/0/0/0/No Comments/basically 3v1 defense\",\n" +
                    "    \"31/2930/4/4/0/0/0/1/1/1/0/0/0/0/0/2/3/0/0/0/No Comments/broke\",\n" +
                    "    \"33/4131/4/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/No Comments/No Comments\"],\n" +
                    "    \"5\":[\"27/3826/5/0/0/0/0/0/0/0/0/0/0/0/0/4/0/0/2/0/No Comments/No Comments\",\n" +
                    "    \"28/5937/5/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/5/0/No Comments/No Comments\",\n" +
                    "    \"29/6350/5/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/No Comments/No Comments\"]    \n" +
                    "}";
            OutputStreamWriter writer = new OutputStreamWriter(sock.getOutputStream());
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            Logging.logError(e, "Darn");
        }


    }


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

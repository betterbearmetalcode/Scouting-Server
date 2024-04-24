package org.tahomarobotics.scouting.scoutingserver.util.configuration;

import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.exceptions.ConfigFileFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Configuration {

    //the order of this enum is essential and determines the protocol for json transfer
    public enum Datatype {
        INTEGER,
        STRING,
        BOOLEAN
    }

    public static  ArrayList<DataMetric> rawDataMetrics = new ArrayList<>();

    public static void updateConfiguration() throws ParserConfigurationException, IOException, SAXException, ConfigFileFormatException {
        ArrayList<DataMetric> backupDataMetrics = rawDataMetrics;//create backup in case of exceptions
        // Specify the file path as a File object
        File configFile = new File(Constants.CONFIG_FILE_LOCATION);

        // Create a DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Parse the XML file
        Document document = builder.parse(configFile);

        //read the config file and set appropriate fields

        //get a list of all the data metrics
        NodeList nodeList = document.getElementsByTagName("rawDataMetrics");//there should only be one of these
        if (nodeList.getLength() != 1) {
            throw new ConfigFileFormatException("Config File has multiple rawDataMetrics tags.");
        }
        NodeList dataMetrics = nodeList.item(0).getChildNodes();
        rawDataMetrics.clear();
        for (int i = 0; i < dataMetrics.getLength(); i++) {
            Node node = dataMetrics.item(i);
            if (!Objects.equals(node.getNodeName(), "dataMetric")) {
                //if there is a node with a name other than what we expect, skip it
                continue;
            }
            NodeList dataMetricFields = node.getChildNodes();
            if (dataMetricFields.getLength() != 9 ) {
                //if there are not the right number of children, skip
                continue;
            }
            Node nameNode = dataMetricFields.item(1);
            if (!Objects.equals(nameNode.getNodeName(), "name")) {
                continue;
            }
            String name = nameNode.getTextContent();

            Node dataTypeNode = dataMetricFields.item(3);
            if (!dataTypeNode.getNodeName().equals("dataType")) {
                continue;
            }
            try {
                Datatype dataType = Datatype.valueOf(dataTypeNode.getTextContent().toUpperCase());
                Node validateableNode = dataMetricFields.item(5);
                if (!validateableNode.getNodeName().equals("validateable")) {
                    continue;
                }
                boolean validateable = false;
                if (validateableNode.getTextContent().equalsIgnoreCase("yes")) {
                    validateable = true;
                }else if (!validateableNode.getTextContent().equalsIgnoreCase("no")) {
                    continue;
                }
                Node defaultValueNode = dataMetricFields.item(7);
                if (!defaultValueNode.getNodeName().equals("default")) {
                    continue;
                }
                switch (dataType) {

                    case INTEGER -> {
                        int val = 0;//default default is 0
                        try {
                            val = Integer.parseInt(defaultValueNode.getTextContent());//try and set to reasonable default
                        }catch (NumberFormatException ignored) {

                        }
                        rawDataMetrics.add(new DataMetric<>(dataType, name, validateable, val));
                    }
                    case STRING -> {
                        rawDataMetrics.add(new DataMetric<>(dataType, name, validateable, defaultValueNode.getTextContent()));
                    }
                    case BOOLEAN -> {
                        rawDataMetrics.add(new DataMetric<>(dataType, name, validateable, defaultValueNode.getTextContent().equalsIgnoreCase("true")));
                    }
                }

            }catch (IllegalArgumentException e) {
                rawDataMetrics = backupDataMetrics;//restore config to whatever we had before
                throw new ConfigFileFormatException("Unsupported Datatype: " + dataTypeNode.getTextContent() + " for Metric: " + name);
            }

        }
    }
}

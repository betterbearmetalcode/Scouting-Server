package org.tahomarobotics.scouting.scoutingserver.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tahomarobotics.scouting.scoutingserver.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;


public class DataCorrectionController implements Initializable {

    @FXML
    TextField text;
    private JSONArray eventList;
    private final ArrayList<Pair<String, String>> otherEvents = new ArrayList<>();

    private AutoCompletionBinding<String> autoCompletionBinding;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //load all competitions and keys
        try {
            FileInputStream stream = new FileInputStream(new File(Constants.BASE_READ_ONLY_FILEPATH + "/resources/TBAData/eventList.json"));
            eventList = new JSONArray(new String(stream.readAllBytes()));
            stream.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList<String> options = new ArrayList<>();
        otherEvents.clear();
        for (Object o : eventList.toList()) {
            HashMap<String, String> comp = (HashMap<String, String>) o;
            otherEvents.add(new Pair<>(comp.get("key"), comp.get("name")));
            options.add(comp.get("name"));
        }

        autoCompletionBinding = TextFields.bindAutoCompletion(text, options);

    }

    public void correctData(ActionEvent event) {
        System.out.println("Correcting Data for comp: " + otherEvents.stream().filter(s -> s.getValue().equals(text.getText())).findFirst().get());
    }




}

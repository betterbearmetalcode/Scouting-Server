package org.tahomarobotics.scouting.scoutingserver.util.UI;

import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.controlsfx.control.textfield.TextFields;
import org.json.JSONArray;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.util.Logging;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DataValidationCompetitionChooser extends Dialog<String> {
    private final TextField autoCompletionField = new TextField();

    public ArrayList<Pair<String, String>> getOtherEvents() {
        return otherEvents;
    }

    private final ArrayList<Pair<String, String>> otherEvents;

    //this tries to return whatever competition the user selects, and also sets the low error threshold in constants
    public DataValidationCompetitionChooser() {
        JSONArray eventList = new JSONArray();
        //init list of events
        try {
            FileInputStream stream = new FileInputStream(Constants.BASE_READ_ONLY_FILEPATH + "/resources/TBAData/eventList.json");
            eventList = new JSONArray(new String(stream.readAllBytes()));
            stream.close();
        }catch (IOException e) {
            Logging.logError(e, "Whooop de doo, another IO exception, I guess your just screwed now,-C.H, try closing things and dont delete the resources folder and maybe restart the app and/or your computer if competitons aren't showing up this is probably why, this will probably never show up, but I kind of hope it does because it would be funny and I sat here typing a unessacarily long error message for no good reason");
        }


        ArrayList<String> options = new ArrayList<>();
        otherEvents = new ArrayList<>();
        for (Object o : eventList.toList()) {
            HashMap<String, String> comp = (HashMap<String, String>) o;
            otherEvents.add(new Pair<>(comp.get("key"), comp.get("name")));
            options.add(comp.get("name"));
        }

        TextFields.bindAutoCompletion(autoCompletionField, options);
        Spinner<Integer> dataValidationThresholdSpinner = new Spinner<>();
        dataValidationThresholdSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,10,3));
        dataValidationThresholdSpinner.setPrefWidth(65);
        this.getDialogPane().setContent(new VBox(new FlowPane(new Label("Enter Competition: "), autoCompletionField), new FlowPane(new Label("High Error Threshold: "), dataValidationThresholdSpinner)));
        this.setTitle("Select Competition For Data Validation");
        this.setHeaderText("");
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);
        this.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                Constants.LOW_ERROR_THRESHOLD = dataValidationThresholdSpinner.getValue();
                String result = autoCompletionField.getText();
                if (!Objects.equals(result, "")) {
                    //if they actuall selected something
                    Optional<Pair<String, String>> event = getOtherEvents().stream().filter(s -> s.getValue().equals(result)).findFirst();
                    AtomicReference<Pair<String,String>> selectedEventCode = new AtomicReference<>(new Pair<>("",""));
                    event.ifPresent(selectedEventCode::set);
                    return selectedEventCode.get().getKey();

                }
            }
            return "";
        });
    }
}

package org.tahomarobotics.scouting.scoutingserver.util;


import javafx.scene.control.Alert;
import org.json.JSONArray;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class APIUtil {

    public static final String baseURL = "https://www.thebluealliance.com/api/v3";
    private static final String apiKey = "gkV8whv2viztnwQybXkOmyQMYYJEGNh7qgbUvG0riVVdDH2YMKk57JNaRwgiTSQB";


    public static String getAsString(String apiRequest) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + apiRequest + "?X-TBA-Auth-Key=" + apiKey))
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }catch (ConnectException e) {
            Logging.logInfo("No Internet Detected");
        }
        if (response != null) {
            return response.body();
        }else {
            //if the response is null, then it wasn't changed when we did the api request
            Logging.logInfo("No Internet", true);
            return null;
        }
    }

    public static JSONArray get(String apiRequest) throws IOException, InterruptedException {
        String str = getAsString(apiRequest);
        if (str != null) {
            return new JSONArray(str);
        }else {
            return null;
        }

    }

    public static Optional<JSONArray> getEventMatches(String eventCode) {
        try {
            JSONArray eventMatches = APIUtil.get("/event/" + eventCode + "/matches");

            //if the query succedes but there are no matches an empty array will be returned
            //check if their are no matches before next check to avoid exceptions
            if (eventMatches.isEmpty()) {
                return Optional.empty();
            }
            if (eventMatches.get(0).equals("NoInternet")) {
                return Optional.empty();
            }
            //if the code has gotten to here, then the array ought to be valid
            return Optional.of(eventMatches);
        } catch (IOException | InterruptedException e) {
            return Optional.empty();
        }


    }

}

package org.tahomarobotics.scouting.scoutingserver.util;


import org.json.JSONArray;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class APIUtil {

    public static final String baseURL = "https://www.thebluealliance.com/api/v3";
    private static final String apiKey = "gkV8whv2viztnwQybXkOmyQMYYJEGNh7qgbUvG0riVVdDH2YMKk57JNaRwgiTSQB";


    public static String getAsString(String apiRequest) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + apiRequest + "?X-TBA-Auth-Key=" + apiKey))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static JSONArray get(String apiRequest) throws IOException, InterruptedException {
        return new JSONArray(getAsString(apiRequest));

    }

}

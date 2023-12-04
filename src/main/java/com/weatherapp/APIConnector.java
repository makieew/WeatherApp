package com.weatherapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Scanner;


public class APIConnector {
    private final String apiUrl;

    public APIConnector(String urlString) throws MalformedURLException {
        this.apiUrl = urlString;
    }

    public JsonNode sendGetRequest(String query) throws IOException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(apiUrl + query);

            // Open connection
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // Get response code
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                StringBuilder response = new StringBuilder();
                Scanner scanner = new Scanner(url.openStream());

                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                // Parse JSON using Jackson
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readTree(response.toString());

            } else {
                throw new IOException("HTTP request failed with response code: " + responseCode);
            }
        } finally {
            // Close the connection
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}

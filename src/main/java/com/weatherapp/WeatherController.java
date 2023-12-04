package com.weatherapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.MalformedURLException;

public class WeatherController {
    private final String forecastAPI = "https://api.open-meteo.com/v1/forecast";
    @FXML
    private TextField searchBar;
    @FXML
    private Button searchButton;
    @FXML
    private Label locationText;

    @FXML
    protected void onCitySearch(MouseEvent event) throws IOException {
        System.out.println("Hello");
        String city = searchBar.getText();
        if (!city.isEmpty()) {
            JsonNode geoResponse = geocodeLocation(city);

            String country = geoResponse.get("results").get(0).get("country").asText();
            // TODO: Extract coordinates from the geocoding response
            double latitude = geoResponse.get("results").get(0).get("latitude").asDouble();
            double longitude = geoResponse.get("results").get(0).get("longitude").asDouble();

            // test
            System.out.println("Country: "+country+"\nlatitude: "+latitude+"\nlongitude: "+longitude);

            // TODO: Weather forecast

        }
    }

    private JsonNode geocodeLocation(String city) throws IOException {
        String geocodingAPI = "https://geocoding-api.open-meteo.com/v1/search";
        APIConnector connector = new APIConnector(geocodingAPI);
        return connector.sendGetRequest("?name="+city+"&count=1&language=en&format=json");
    }
}
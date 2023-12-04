package com.weatherapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.Objects;

public class WeatherController {
    @FXML
    private TextField searchBar;
    @FXML
    private Label locationText;
    @FXML
    private Label tempText;
    @FXML
    private Label humidityText;
    @FXML
    private Label windText;
    @FXML
    private Label visibilityText;
    @FXML
    private Label pressureText;
    @FXML
    private Label dewpointText;

    @FXML
    protected void onCitySearch(MouseEvent event) throws IOException {
        System.out.println("Hello");
        String city = searchBar.getText();
        if (!city.isEmpty()) {
            // City -> location information
            JsonNode geoResponse = geocodeLocation(city);

            locationText.setText(city + ", " + geoResponse.get("results").get(0).get("country").asText());

            // Extract coordinates from the geocoding response
            double latitude = geoResponse.get("results").get(0).get("latitude").asDouble();
            double longitude = geoResponse.get("results").get(0).get("longitude").asDouble();

            // Current and daily forecast data
            JsonNode currentForecastResponse = locationForecast(latitude, longitude, "current");
            JsonNode dailyForecastResponse = locationForecast(latitude, longitude, "daily");

            // Displaying data
            tempText.setText(currentForecastResponse.get("current").get("temperature_2m").asText());
            humidityText.setText(humidityText.getText() + currentForecastResponse.get("current").get("relative_humidity_2m").asText());
            windText.setText(windText.getText() + currentForecastResponse.get("current").get("wind_speed_10m").asText() + ", " + currentForecastResponse.get("current").get("wind_direction_10m").asText());
            visibilityText.setText(visibilityText.getText() + currentForecastResponse.get("current").get("visibility").asText());
            pressureText.setText(pressureText.getText() + currentForecastResponse.get("current").get("pressure_msl").asText());
            dewpointText.setText(dewpointText.getText() + currentForecastResponse.get("current").get("dew_point_2m").asText());

            // TODO: daily forecast data visualization
        }
    }

    private JsonNode geocodeLocation(String city) throws IOException {
        String geocodingAPI = "https://geocoding-api.open-meteo.com/v1/search";
        APIConnector connector = new APIConnector(geocodingAPI);
        return connector.sendGetRequest("?name="+city+"&count=1&language=en&format=json");
    }

    private JsonNode locationForecast(double latitude, double longitude, String param) throws IOException {
        String forecastAPI = "https://api.open-meteo.com/v1/forecast";
        APIConnector connector = new APIConnector(forecastAPI);

        String options = "";
        if (Objects.equals(param, "current")) {
            options = "&current=temperature_2m,relative_humidity_2m,visibility,apparent_temperature,is_day,weather_code,pressure_msl,dew_point_2m,wind_speed_10m,wind_direction_10m";
        } else if (Objects.equals(param, "daily")) {
            options = "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max";
        } else {
            System.out.println("error");
            return null;
        }
        return connector.sendGetRequest("?latitude="+latitude+"&longitude="+longitude+options);
    }
}
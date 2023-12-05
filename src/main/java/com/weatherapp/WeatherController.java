package com.weatherapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
    private HBox dailyForecastContainer;

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
            tempText.setText(currentForecastResponse.get("current").get("temperature_2m").asText()+" °C");
            humidityText.setText("Humidity " + currentForecastResponse.get("current").get("relative_humidity_2m").asText());
            windText.setText("Wind " + currentForecastResponse.get("current").get("wind_speed_10m").asText() + ", " + currentForecastResponse.get("current").get("wind_direction_10m").asText());
            visibilityText.setText("Visibility " + currentForecastResponse.get("current").get("visibility").asText());
            pressureText.setText("Pressure " + currentForecastResponse.get("current").get("pressure_msl").asText());
            dewpointText.setText("Dew point " + currentForecastResponse.get("current").get("dew_point_2m").asText());

            // TODO: daily forecast data visualization
            displayDailyForecast(dailyForecastResponse);
        }
    }

    private void displayDailyForecast(JsonNode dailyForecastData) {
        dailyForecastContainer.getChildren().clear();

        JsonNode dateNode = dailyForecastData.get("daily").get("time");
        int nDays = dateNode.size();

        for (int i = 0; i < nDays; i++) {
            Label date = new Label(dateNode.get(i).asText());
            // TODO weatherCode -> image
            String weatherCode = dailyForecastData.get("daily").get("weather_code").get(i).asText();
            Label maxTemp = new Label(dailyForecastData.get("daily").get("temperature_2m_max").get(i).asText()+" °C");
            Label minTemp = new Label(dailyForecastData.get("daily").get("temperature_2m_min").get(i).asText()+" °C");
            Label precip = new Label(dailyForecastData.get("daily").get("precipitation_probability_max").get(i).asText()+" %");

            VBox dayContainer = new VBox(date, maxTemp, minTemp, precip);
            dailyForecastContainer.getChildren().add(dayContainer);
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
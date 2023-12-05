package com.weatherapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private ImageView weatherImg;
    @FXML
    private HBox dailyForecastContainer;

    @FXML
    protected void onCitySearch(MouseEvent event) throws IOException {
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
            boolean day = currentForecastResponse.get("current").get("is_day").asBoolean();
            int weatherCode = currentForecastResponse.get("current").get("weather_code").asInt();
            weatherImg.setImage(getWeatherImage(day, weatherCode));

            displayDailyForecast(dailyForecastResponse);
        }
    }

    private void displayDailyForecast(JsonNode dailyForecastData) {
        dailyForecastContainer.getChildren().clear();

        JsonNode dateNode = dailyForecastData.get("daily").get("time");
        int nDays = dateNode.size();

        for (int i = 0; i < nDays; i++) {
            Label day = new Label(convertDateToDay(dateNode.get(i).asText()));
            int weatherCode = dailyForecastData.get("daily").get("weather_code").get(i).asInt();
            ImageView weatherImg = new ImageView(getWeatherImage(true, weatherCode));
            Label maxTemp = new Label(dailyForecastData.get("daily").get("temperature_2m_max").get(i).asText()+" °C");
            Label minTemp = new Label(dailyForecastData.get("daily").get("temperature_2m_min").get(i).asText()+" °C");
            Label precip = new Label(dailyForecastData.get("daily").get("precipitation_probability_max").get(i).asText()+" %");

            VBox dayContainer = new VBox(day);
            HBox dayContainerDisplay = new HBox();
            VBox dayContainerInfo = new VBox(maxTemp, minTemp, precip);

            dayContainerDisplay.getChildren().add(weatherImg);
            dayContainerDisplay.getChildren().add(dayContainerInfo);

            dayContainer.getChildren().add(dayContainerDisplay);

            dailyForecastContainer.getChildren().add(dayContainer);
        }
    }

    private String convertDateToDay(String date) {
        LocalDate ldate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        return ldate.format(DateTimeFormatter.ofPattern("E d"));
    }

    private Image getWeatherImage(boolean day, int weatherCode) {
        char d = day ? 'd' : 'n';
        String wc = String.valueOf(weatherCode);
        String imgPathPattern = "/com/weatherapp/media/weather_icons/%s.png";
        String imgPath = null;

        // clear->cloudy (night/day)
        if (weatherCode >= 0 && weatherCode <= 2) {
            imgPath = String.format(imgPathPattern, wc + d);
        }
        // fog
        else if (weatherCode == 45 || weatherCode == 48) {
            imgPath = String.format(imgPathPattern, "45");
        }
        // drizzle
        else if (weatherCode == 51 || weatherCode == 53 || weatherCode == 55) {
            imgPath = String.format(imgPathPattern, "51");
        }
        // freezing drizzle
        else if (weatherCode == 56 || weatherCode == 57) {
            imgPath = String.format(imgPathPattern, "56");
        }
        // rain light
        else if (weatherCode == 61 || weatherCode == 80) {
            imgPath = String.format(imgPathPattern, "61");
        }
        // rain moderate
        else if (weatherCode == 63 || weatherCode == 81) {
            imgPath = String.format(imgPathPattern, "63");
        }
        // rain heavy
        else if (weatherCode == 65 || weatherCode == 82) {
            imgPath = String.format(imgPathPattern, "65");
        }
        // snow light
        else if (weatherCode == 71 || weatherCode == 85) {
            imgPath = String.format(imgPathPattern, "71");
        }
        // snow heavy
        else if (weatherCode == 75 || weatherCode == 86) {
            imgPath = String.format(imgPathPattern, "75");
        }
        // hail
        else if (weatherCode == 96 || weatherCode == 99) {
            imgPath = String.format(imgPathPattern, "96");
        }
        // other
        else {
            imgPath = String.format(imgPathPattern, wc);
        }
        if (imgPath != null)
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(imgPath)));
        else
            return null;
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
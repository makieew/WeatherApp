package com.weatherapp;

import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class WeatherController {
    private final Timeline realTimeUpdateTimeline = new Timeline();
    LocalDate currentDate = LocalDate.now();
    String city = null;

    @FXML
    private TextField searchBar;
    @FXML
    private Label locationText;
    @FXML
    private Label lastUpdatedLabel;
    @FXML
    private Label tempText;
    @FXML
    private Label weatherTextLabel;
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
    private Label monthTextLabel;
    @FXML
    private Label avgTempMaxLabel;
    @FXML
    private Label avgTempMinLabel;
    @FXML
    private Label nSunnyDaysLabel;
    @FXML
    private Label nNotSunnyDaysLabel;

    @FXML
    protected void initialize() {
        // Initialize real-time updates
        realTimeUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        KeyFrame keyFrame = new KeyFrame(Duration.minutes(15), event -> {
            try {
                if (city != null)
                    refreshData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        realTimeUpdateTimeline.getKeyFrames().add(keyFrame);
        realTimeUpdateTimeline.play();
    }

    private void refreshData() throws IOException {
        if (city != null && !city.isEmpty()) {
            // City -> location information
            JsonNode geoResponse = geocodeLocation(city);

            if (geoResponse.has("results")) {
                locationText.setText(geoResponse.get("results").get(0).get("name").asText() + ", " + geoResponse.get("results").get(0).get("country").asText());

                // Extract coordinates from the geocoding response
                double latitude = geoResponse.get("results").get(0).get("latitude").asDouble();
                double longitude = geoResponse.get("results").get(0).get("longitude").asDouble();

                // Current and daily forecast data
                JsonNode currentForecastResponse = locationForecast(latitude, longitude, "current");
                JsonNode dailyForecastResponse = locationForecast(latitude, longitude, "daily");

                // Current month weather history
                LocalDate firstDayOfMonth = currentDate.withDayOfMonth(1);
                JsonNode historyForecastResponse = locationHistoryForecast(latitude, longitude, firstDayOfMonth.toString(), currentDate.minusDays(1).toString());

                // Displaying data
                LocalDateTime currentTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                lastUpdatedLabel.setText("Last updated at " + currentTime.format(formatter));

                tempText.setText(currentForecastResponse.get("current").get("temperature_2m").asText() + " °C");
                humidityText.setText("Humidity " + currentForecastResponse.get("current").get("relative_humidity_2m").asText() + "%");
                windText.setText("Wind " + currentForecastResponse.get("current").get("wind_speed_10m").asText() + " km/h");

                // Converting visibility from  meters to km
                float vis = (float) currentForecastResponse.get("current").get("visibility").asInt() / 1000;
                visibilityText.setText("Visibility " + vis + " km");

                pressureText.setText("Pressure " + currentForecastResponse.get("current").get("pressure_msl").asText() + " mb");
                dewpointText.setText("Dew point " + currentForecastResponse.get("current").get("dew_point_2m").asText() + "°");

                boolean day = currentForecastResponse.get("current").get("is_day").asBoolean();
                int weatherCode = currentForecastResponse.get("current").get("weather_code").asInt();
                weatherImg.setImage(getWeatherImage(day, weatherCode));
                weatherTextLabel.setText(getWeatherDescription(weatherCode));

                displayDailyForecast(dailyForecastResponse);
                displayMonthWeatherHistory(historyForecastResponse);

//                System.out.println("Real-time data refreshed");
            }
        }
    }

    @FXML
    protected void onCitySearch(MouseEvent event) throws IOException {
        city = searchBar.getText();
        refreshData();
    }

    private String getWeatherDescription(int weatherCode) {
        switch (weatherCode) {
            case 0:
                return "Clear sky";
            case 1:
                return "Mainly clear";
            case 2:
                return "Partly cloudy";
            case 3:
                return "Overcast";
            case 45:
                return "Fog";
            case 48:
                return "Depositing rime fog";
            case 51:
                return "Light drizzle";
            case 53:
                return "Moderate drizzle";
            case 55:
                return "Dense drizzle";
            case 56:
                return "Light freezing drizzle";
            case 57:
                return "Dense freezing drizzle";
            case 61:
                return "Slight rain";
            case 63:
                return "Moderate rain";
            case 65:
                return "Heavy rain";
            case 66:
                return "Light freezing rain";
            case 67:
                return "Heavy freezing rain";
            case 71:
                return "Slight snow fall";
            case 73:
                return "Moderate snow fall";
            case 75:
                return "Heavy snow fall";
            case 77:
                return "Snow grains";
            case 80:
                return "Slight rain showers";
            case 81:
                return "Moderate rain showers";
            case 82:
                return "Violent rain showers";
            case 85:
                return "Slight snow showers";
            case 86:
                return "Heavy snow showers";
            case 95:
                return "Thunderstorm";
            case 96:
                return "Thunderstorm with slight hail";
            case 99:
                return "Thunderstorm with heavy hail";
        }
        return "";
    }

    private JsonNode locationHistoryForecast(double latitude, double longitude, String start_date, String end_date) throws IOException {
        String forecastHistoryAPI = "https://archive-api.open-meteo.com/v1/archive";
        APIConnector connector = new APIConnector(forecastHistoryAPI);
        String options = "&daily=weather_code,temperature_2m_max,temperature_2m_min";
        return connector.sendGetRequest("?latitude="+latitude+"&longitude="+longitude+"&start_date="+start_date+"&end_date="+end_date+options);
    }

    private void displayMonthWeatherHistory(JsonNode historyForecastData) {
        // Data preparation
        String[] weatherCodes = arrayNodeToStringArray((ArrayNode) historyForecastData.get("daily").get("weather_code"));
        String[] maxTemps = arrayNodeToStringArray((ArrayNode) historyForecastData.get("daily").get("temperature_2m_max"));
        String[] minTemps = arrayNodeToStringArray((ArrayNode) historyForecastData.get("daily").get("temperature_2m_min"));

        int nSunnyDays = countSunnyDays(weatherCodes);
        double avgMax = getAverageValue(maxTemps);
        double avgMin = getAverageValue(minTemps);

        // Data display
        monthTextLabel.setText(currentDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " so far");

        avgTempMaxLabel.setText("Average high " + avgMax + "°");
        avgTempMinLabel.setText("Average low " + avgMin + "°");

        nSunnyDaysLabel.setText("Average high " + nSunnyDays + "°");
        nNotSunnyDaysLabel.setText("Average high " + (weatherCodes.length - nSunnyDays) + "°");
    }

    private double getAverageValue(String[] a) {
        double sum = 0;
        for (int i = 0; i < a.length - 1; i++) {
            sum += Double.parseDouble(a[i]);
        }
        double avg = sum/a.length;
        return Math.round(avg * 10.0) / 10.0;
    }

    private int countSunnyDays(String[] weatherCodes) {
        int counter = 0;
        for (int i = 0; i < weatherCodes.length - 1; i++) {
            if (Objects.equals(weatherCodes[i], "0") || Objects.equals(weatherCodes[i], "1")) {
                counter++;
            }
        }
        return counter;
    }

    private String[] arrayNodeToStringArray(ArrayNode an) {
        if (an == null || an.isEmpty()) {
            return new String[0];
        }
        List<String> a = new ArrayList<>();
        for (int i = 0; i < an.size(); i++) {
            JsonNode node = an.get(i);
            if (node != null && !node.isNull()) {
                a.add(node.asText());
            }
        }
        return a.toArray(new String[0]);
    }

    private void displayDailyForecast(JsonNode dailyForecastData) {
        dailyForecastContainer.getChildren().clear();

        Image droplet_img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/weatherapp/design/weather_icons/droplet-icon.png")));

        JsonNode dateNode = dailyForecastData.get("daily").get("time");
        int nDays = dateNode.size();

        for (int i = 0; i < nDays; i++) {
            Label day = new Label(convertDateToDay(dateNode.get(i).asText()));
            day.getStyleClass().add("day-label");

            int weatherCode = dailyForecastData.get("daily").get("weather_code").get(i).asInt();
            ImageView weatherImg = new ImageView(getWeatherImage(true, weatherCode));
            weatherImg.setFitHeight(50);
            weatherImg.setFitWidth(50);

            Label maxTemp = new Label(dailyForecastData.get("daily").get("temperature_2m_max").get(i).asText() + " °C");
            Label minTemp = new Label(dailyForecastData.get("daily").get("temperature_2m_min").get(i).asText() + " °C");
            Label precip = new Label(dailyForecastData.get("daily").get("precipitation_probability_max").get(i).asText() + " %");

            ImageView droplet = new ImageView(droplet_img);
            droplet.setFitHeight(12);
            droplet.setFitWidth(12);

            HBox precip_container = new HBox(droplet, precip);
            precip_container.getStyleClass().add("day-precip-container");

            VBox dayContainer = new VBox(day);
            dayContainer.getStyleClass().add("day-container");
            dayContainer.setMinWidth(130);

            HBox dayContainerDisplay = new HBox();
            dayContainerDisplay.getStyleClass().add("day-container-display");

            VBox dayContainerInfo = new VBox(maxTemp, minTemp, precip_container);
            dayContainerInfo.getStyleClass().add("day-container-info");

            dayContainerDisplay.getChildren().addAll(weatherImg, dayContainerInfo);
            dayContainer.getChildren().add(dayContainerDisplay);
            dayContainer.getStyleClass().add("daily-forecast-container");

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
        String imgPathPattern = "/com/weatherapp/design/weather_icons/%s.png";
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
            options = "&current=temperature_2m,relative_humidity_2m,visibility,apparent_temperature,is_day,weather_code,pressure_msl,dew_point_2m,wind_speed_10m";
        } else if (Objects.equals(param, "daily")) {
            options = "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max";
        }
        return connector.sendGetRequest("?latitude="+latitude+"&longitude="+longitude+options);
    }
}
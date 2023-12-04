module com.example.weatherapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;


    opens com.weatherapp to javafx.fxml;
    exports com.weatherapp;
}
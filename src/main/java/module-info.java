module com.example.weatherapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.weatherapp to javafx.fxml;
    exports com.example.weatherapp;
}
package com.nexus;

import com.nexus.database.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseManager.getInstance().init();

        Parent root = FXMLLoader.load(Objects.requireNonNull(
                App.class.getResource("/view/MainView.fxml"),
                "MainView.fxml not found"
        ));

        Scene scene = new Scene(root, 1200, 800);

        scene.getStylesheets().add(Objects.requireNonNull(
                App.class.getResource("/css/styles.css"),
                "styles.css not found"
        ).toExternalForm());

        stage.setTitle("Nexus");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
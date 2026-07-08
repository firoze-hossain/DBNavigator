package com.roze.dbnavigator;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.ui.MainWindow;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        MainWindow window = new MainWindow(stage);
        Scene scene = new Scene(window.getRoot(), 1440, 900);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.setTitle("DBNavigator Pro");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    @Override
    public void stop() {
        ClientRegistry.closeAll();
        AppExecutor.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

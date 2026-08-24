package com.roze.dbnavigator;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.ui.MainWindow;
import com.roze.dbnavigator.ui.ThemeManager;
import com.roze.dbnavigator.util.AppExecutor;
import com.roze.dbnavigator.update.AppUpdateService;
import com.roze.dbnavigator.ui.AppUpdateDialog;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        MainWindow window = new MainWindow(stage);
        Scene scene = new Scene(window.getRoot(), 1440, 900);
        ThemeManager.init(scene, AppSettingsStore.load().getTheme());

        stage.setTitle("DBNavigator Pro");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();

        // Non-blocking startup update check. The user controls this from Settings > Updates.
        var updateSettings = AppSettingsStore.load();
        if (updateSettings.isAutoUpdateEnabled()) {
            AppUpdateService.checkForUpdate().thenAccept(update -> {
                if (update != null && update.available) {
                    javafx.application.Platform.runLater(() ->
                            AppUpdateDialog.show(stage, update, null, updateSettings.isAutoDownloadUpdates(), false));
                }
            }).exceptionally(error -> null);
        }
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

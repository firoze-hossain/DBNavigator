package com.roze.dbnavigator;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.ui.MainWindow;
import com.roze.dbnavigator.ui.ThemeManager;
import com.roze.dbnavigator.util.AppExecutor;
import com.roze.dbnavigator.update.AppUpdateService;
import com.roze.dbnavigator.ui.AppUpdateDialog;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double screenW = visualBounds.getWidth();
        double screenH = visualBounds.getHeight();

        var settings = AppSettingsStore.load();

        double targetW;
        double targetH;
        double targetX;
        double targetY;
        boolean shouldMaximize;

        boolean hasSavedGeometry = settings.getWindowWidth() >= 400 && settings.getWindowHeight() >= 300;

        if (hasSavedGeometry) {
            targetW = settings.getWindowWidth();
            targetH = settings.getWindowHeight();
            targetX = settings.getWindowX();
            targetY = settings.getWindowY();
            shouldMaximize = settings.isWindowMaximized();

            // Validate that the saved window is visible on at least one currently active screen
            boolean isVisible = false;
            for (Screen screen : Screen.getScreens()) {
                Rectangle2D b = screen.getVisualBounds();
                if (targetX + 60 >= b.getMinX() && targetX + 60 <= b.getMaxX()
                        && targetY >= b.getMinY() && targetY <= b.getMaxY() - 40) {
                    isVisible = true;
                    break;
                }
            }

            if (!isVisible) {
                targetW = Math.min(1360.0, Math.max(760.0, screenW * 0.94));
                targetH = Math.min(840.0, Math.max(480.0, screenH * 0.90));
                targetX = visualBounds.getMinX() + Math.max(0, (screenW - targetW) / 2.0);
                targetY = visualBounds.getMinY() + Math.max(0, (screenH - targetH) / 2.0);
            }
        } else {
            // First run or default: responsive sizing for laptop displays
            targetW = Math.min(1360.0, Math.max(760.0, screenW * 0.94));
            targetH = Math.min(840.0, Math.max(480.0, screenH * 0.90));
            targetX = visualBounds.getMinX() + Math.max(0, (screenW - targetW) / 2.0);
            targetY = visualBounds.getMinY() + Math.max(0, (screenH - targetH) / 2.0);

            // Laptop screens (e.g. 1280x720, 1366x768, 1440x900) start maximized by default
            shouldMaximize = (screenW <= 1440 || screenH <= 800);
        }

        // Clamp target dimensions so the window never starts larger than the screen or positioned offscreen
        if (targetW > screenW) {
            targetW = Math.max(600.0, screenW * 0.96);
            targetX = visualBounds.getMinX() + (screenW - targetW) / 2.0;
        }
        if (targetH > screenH) {
            targetH = Math.max(400.0, screenH * 0.92);
            targetY = visualBounds.getMinY() + (screenH - targetH) / 2.0;
        }
        if (targetX < visualBounds.getMinX()) targetX = visualBounds.getMinX();
        if (targetY < visualBounds.getMinY()) targetY = visualBounds.getMinY();

        MainWindow window = new MainWindow(stage);
        Scene scene = new Scene(window.getRoot(), targetW, targetH);
        ThemeManager.init(scene, settings.getTheme());

        window.updateWindowTitle();
        stage.setScene(scene);

        // Responsive min dimensions so it can safely fit on 720p / 768p laptop screens
        stage.setMinWidth(Math.min(650, screenW * 0.60));
        stage.setMinHeight(Math.min(420, screenH * 0.60));

        stage.setWidth(targetW);
        stage.setHeight(targetH);
        stage.setX(targetX);
        stage.setY(targetY);

        if (shouldMaximize) {
            stage.setMaximized(true);
        }

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

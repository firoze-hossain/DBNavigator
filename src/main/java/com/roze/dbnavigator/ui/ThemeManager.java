package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.scene.Scene;

/**
 * Applies and switches the app's stylesheet at runtime. New dialogs created
 * after a switch correctly pick up the new theme (they copy the main
 * window's current stylesheets at creation time); already-open windows are
 * not retroactively re-themed in this version.
 */
public final class ThemeManager {

    private static Scene mainScene;
    private static AppSettingsStore.Theme current = AppSettingsStore.Theme.DARK;

    private ThemeManager() {}

    public static void init(Scene scene, AppSettingsStore.Theme initial) {
        mainScene = scene;
        current = initial;
        applyToScene(scene, initial);
    }

    public static AppSettingsStore.Theme getCurrent() {
        return current;
    }

    /** Switches the main window's theme; call after persisting the new setting. */
    public static void setTheme(AppSettingsStore.Theme theme) {
        current = theme;
        if (mainScene != null) applyToScene(mainScene, theme);
    }

    private static void applyToScene(Scene scene, AppSettingsStore.Theme theme) {
        scene.getStylesheets().clear();
        scene.getStylesheets().add(stylesheetUrl(theme));
    }

    public static String stylesheetUrl(AppSettingsStore.Theme theme) {
        String path = theme == AppSettingsStore.Theme.LIGHT ? "/css/app-light.css" : "/css/app.css";
        return ThemeManager.class.getResource(path).toExternalForm();
    }
}

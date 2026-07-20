package com.roze.dbnavigator.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * App-wide preferences (theme, editor font, etc.), persisted to
 * ~/.dbnavigator/settings.json — separate from per-connection state.
 */
public final class AppSettingsStore {

    public enum Theme { DARK, LIGHT }

    /** Plain data holder — Jackson needs a no-arg constructor and public fields/getters+setters. */
    public static class Settings {
        public Theme theme = Theme.DARK;
        public String editorFontFamily = "JetBrains Mono";
        public double editorFontSize = 14;
        public boolean ctrlScrollZoomEnabled = true;

        public Theme getTheme() { return theme; }
        public void setTheme(Theme theme) { this.theme = theme; }
        public String getEditorFontFamily() { return editorFontFamily; }
        public void setEditorFontFamily(String editorFontFamily) { this.editorFontFamily = editorFontFamily; }
        public double getEditorFontSize() { return editorFontSize; }
        public void setEditorFontSize(double editorFontSize) { this.editorFontSize = editorFontSize; }
        public boolean isCtrlScrollZoomEnabled() { return ctrlScrollZoomEnabled; }
        public void setCtrlScrollZoomEnabled(boolean ctrlScrollZoomEnabled) {
            this.ctrlScrollZoomEnabled = ctrlScrollZoomEnabled;
        }
    }

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "settings.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static Settings cached;

    private AppSettingsStore() {}

    public static synchronized Settings load() {
        if (cached != null) return cached;
        if (Files.exists(FILE)) {
            try {
                cached = MAPPER.readValue(FILE.toFile(), Settings.class);
                return cached;
            } catch (IOException e) {
                System.err.println("Could not read settings: " + e.getMessage());
            }
        }
        cached = new Settings();
        return cached;
    }

    public static synchronized void save(Settings settings) {
        cached = settings;
        try {
            Files.createDirectories(FILE.getParent());
            MAPPER.writeValue(FILE.toFile(), settings);
        } catch (IOException e) {
            System.err.println("Could not save settings: " + e.getMessage());
        }
    }
}

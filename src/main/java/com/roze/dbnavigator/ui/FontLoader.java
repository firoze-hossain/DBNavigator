package com.roze.dbnavigator.ui;

import javafx.scene.text.Font;

/**
 * Loads bundled DBNavigatorMono true-type fonts into the JavaFX runtime.
 */
public final class FontLoader {

    private FontLoader() {}

    public static synchronized void loadBundledFonts() {
        loadFont("/fonts/DBNavigatorMono-Regular.ttf");
        loadFont("/fonts/DBNavigatorMono-Bold.ttf");
        loadFont("/fonts/DBNavigatorMono-Italic.ttf");
    }

    private static void loadFont(String resourcePath) {
        try (var is = FontLoader.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                Font.loadFont(is, 13.5);
            }
        } catch (Exception ignored) {}
    }
}

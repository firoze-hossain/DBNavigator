package com.roze.dbnavigator;

import javafx.application.Application;

public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        // This ensures JavaFX application launches properly
        Application.launch(Main.class, args);
    }
}
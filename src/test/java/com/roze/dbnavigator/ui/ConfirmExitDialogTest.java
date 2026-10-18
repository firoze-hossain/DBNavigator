package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfirmExitDialogTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Test
    public void testResultRecord() {
        ConfirmExitDialog.Result r1 = new ConfirmExitDialog.Result(true, false);
        assertTrue(r1.isConfirmed());
        assertFalse(r1.isDontAskAgain());

        ConfirmExitDialog.Result r2 = new ConfirmExitDialog.Result(true, true);
        assertTrue(r2.isConfirmed());
        assertTrue(r2.isDontAskAgain());

        ConfirmExitDialog.Result r3 = new ConfirmExitDialog.Result(false, false);
        assertFalse(r3.isConfirmed());
        assertFalse(r3.isDontAskAgain());
    }

    @Test
    public void testConfirmExitDefaultSetting() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        assertTrue(settings.isConfirmExit(), "Confirm exit should default to true matching DataGrip");

        settings.setConfirmExit(false);
        assertFalse(settings.isConfirmExit());

        settings.setConfirmExit(true);
        assertTrue(settings.isConfirmExit());
    }

    @Test
    public void testConfirmExitWhenDisabledReturnsTrueImmediately() {
        AppSettingsStore.Settings settings = AppSettingsStore.get();
        boolean original = settings.isConfirmExit();
        try {
            settings.setConfirmExit(false);
            AppSettingsStore.save(settings);

            assertTrue(MainWindow.confirmExitIfEnabled(null), "When confirmExit is disabled, confirmExitIfEnabled must return true");
        } finally {
            settings.setConfirmExit(original);
            AppSettingsStore.save(settings);
        }
    }

    @Test
    public void testAppearanceConfirmExitSettingsDialogIntegration() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        settings.setConfirmExit(true);

        Map<String, Object> inputs = new HashMap<>();
        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                SettingsDialog.buildAppearancePanel(settings, inputs);
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        future.get(20, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(inputs.containsKey("appearance_confirmExit"), "inputs map must contain 'appearance_confirmExit'");
        Object obj = inputs.get("appearance_confirmExit");
        assertTrue(obj instanceof CheckBox, "appearance_confirmExit must be a CheckBox");
        CheckBox cb = (CheckBox) obj;
        assertTrue(cb.isSelected(), "CheckBox should reflect settings.isConfirmExit()");

        // Simulate user unchecking the box and applying settings
        cb.setSelected(false);
        SettingsDialog.applySettings(null, settings, inputs);
        assertFalse(settings.isConfirmExit(), "Settings must be updated when applySettings runs");
    }
}

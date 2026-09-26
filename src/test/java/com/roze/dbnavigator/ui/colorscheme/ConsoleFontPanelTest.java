package com.roze.dbnavigator.ui.colorscheme;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConsoleFontPanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testBuildConsoleFontPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = ConsoleFontPanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("consoleFont_useInsteadOfDefault"), "Should register master checkbox");
        assertTrue(inputs.containsKey("consoleFont_fontFamily"), "Should register font family dropdown");
        assertTrue(inputs.containsKey("consoleFont_fallbackFont"), "Should register fallback font dropdown");
        assertTrue(inputs.containsKey("consoleFont_fontSize"), "Should register font size spinner");
        assertTrue(inputs.containsKey("consoleFont_lineHeight"), "Should register line height spinner");
        assertTrue(inputs.containsKey("consoleFont_enableLigatures"), "Should register ligatures checkbox");
        assertTrue(inputs.containsKey("consoleFont_showOnlyMonospaced"), "Should register show only monospaced checkbox");
        assertTrue(inputs.containsKey("editorColorSchemeCombo"), "Should register scheme combo");
    }

    @Test
    public void testConsoleFontDefaultsAndToggle() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        assertFalse(settings.isUseConsoleFontInsteadOfDefault());

        Map<String, Object> inputs = new HashMap<>();
        ConsoleFontPanel.build(settings, inputs, null);

        CheckBox toggle = (CheckBox) inputs.get("consoleFont_useInsteadOfDefault");
        assertNotNull(toggle);
        assertFalse(toggle.isSelected());

        toggle.setSelected(true);
        assertTrue(toggle.isSelected());
    }

    @Test
    public void testConsoleFontSettingsAndPersistence() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        settings.setUseConsoleFontInsteadOfDefault(true);
        settings.setConsoleFontFamily("JetBrains Mono");
        settings.setConsoleFontFallbackFamily("<None>");
        settings.setConsoleFontSize(12.5);
        settings.setConsoleFontLineHeight(1.15);
        settings.setConsoleFontEnableLigatures(true);
        settings.setConsoleFontShowOnlyMonospaced(true);

        assertEquals(12.5, settings.getConsoleFontSize());
        assertEquals(1.15, settings.getConsoleFontLineHeight());
        assertTrue(settings.isUseConsoleFontInsteadOfDefault());
        assertTrue(settings.isConsoleFontEnableLigatures());
        assertTrue(settings.isConsoleFontShowOnlyMonospaced());
        assertEquals("JetBrains Mono", settings.getConsoleFontFamily());
        assertEquals("<None>", settings.getConsoleFontFallbackFamily());

        Map<String, Object> inputs = new HashMap<>();
        ConsoleFontPanel.build(settings, inputs, null);

        CheckBox toggle = (CheckBox) inputs.get("consoleFont_useInsteadOfDefault");
        assertTrue(toggle.isSelected());

        Spinner<Double> sizeSpinner = (Spinner<Double>) inputs.get("consoleFont_fontSize");
        assertEquals(12.5, sizeSpinner.getValue());

        Spinner<Double> lhSpinner = (Spinner<Double>) inputs.get("consoleFont_lineHeight");
        assertEquals(1.15, lhSpinner.getValue());

        CheckBox ligatures = (CheckBox) inputs.get("consoleFont_enableLigatures");
        assertTrue(ligatures.isSelected());
    }

    @Test
    public void testConsoleFontMonospacedFilter() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();
        ConsoleFontPanel.build(settings, inputs, null);

        ComboBox<String> fontCombo = (ComboBox<String>) inputs.get("consoleFont_fontFamily");
        CheckBox monoCheck = (CheckBox) inputs.get("consoleFont_showOnlyMonospaced");
        assertNotNull(fontCombo);
        assertNotNull(monoCheck);

        int countWithOnlyMono = fontCombo.getItems().size();
        assertTrue(countWithOnlyMono > 0);

        monoCheck.setSelected(false);
        monoCheck.getOnAction().handle(null);
        int countAll = fontCombo.getItems().size();
        assertTrue(countAll >= countWithOnlyMono);
    }
}

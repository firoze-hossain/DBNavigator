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

public class ColorSchemeFontPanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testBuildColorSchemeFontPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = ColorSchemeFontPanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("csFont_useInsteadOfDefault"), "Should register master checkbox");
        assertTrue(inputs.containsKey("csFont_fontFamily"), "Should register font family dropdown");
        assertTrue(inputs.containsKey("csFont_fallbackFont"), "Should register fallback font dropdown");
        assertTrue(inputs.containsKey("csFont_fontSize"), "Should register font size spinner");
        assertTrue(inputs.containsKey("csFont_lineHeight"), "Should register line height spinner");
        assertTrue(inputs.containsKey("csFont_enableLigatures"), "Should register ligatures checkbox");
        assertTrue(inputs.containsKey("csFont_showOnlyMonospaced"), "Should register show only monospaced checkbox");
        assertTrue(inputs.containsKey("editorColorSchemeCombo"), "Should register scheme combo");
    }

    @Test
    public void testMasterToggleDefaults() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        assertFalse(settings.isUseColorSchemeFontInsteadOfDefault());

        Map<String, Object> inputs = new HashMap<>();
        ColorSchemeFontPanel.build(settings, inputs, null);

        CheckBox toggle = (CheckBox) inputs.get("csFont_useInsteadOfDefault");
        assertNotNull(toggle);
        assertFalse(toggle.isSelected());

        // Toggle on
        toggle.setSelected(true);
        assertTrue(toggle.isSelected());
    }

    @Test
    public void testFontSettingsValuesAndPersistence() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        settings.setUseColorSchemeFontInsteadOfDefault(true);
        settings.setColorSchemeFontFamily("JetBrains Mono");
        settings.setColorSchemeFontFallbackFamily("<None>");
        settings.setColorSchemeFontSize(14.0);
        settings.setColorSchemeFontLineHeight(1.3);
        settings.setColorSchemeFontEnableLigatures(true);
        settings.setColorSchemeFontShowOnlyMonospaced(false);

        assertEquals(14.0, settings.getColorSchemeFontSize());
        assertEquals(1.3, settings.getColorSchemeFontLineHeight());
        assertTrue(settings.isUseColorSchemeFontInsteadOfDefault());
        assertTrue(settings.isColorSchemeFontEnableLigatures());
        assertFalse(settings.isColorSchemeFontShowOnlyMonospaced());
        assertEquals("JetBrains Mono", settings.getColorSchemeFontFamily());
        assertEquals("<None>", settings.getColorSchemeFontFallbackFamily());

        Map<String, Object> inputs = new HashMap<>();
        ColorSchemeFontPanel.build(settings, inputs, null);

        CheckBox toggle = (CheckBox) inputs.get("csFont_useInsteadOfDefault");
        assertTrue(toggle.isSelected());

        Spinner<Double> sizeSpinner = (Spinner<Double>) inputs.get("csFont_fontSize");
        assertEquals(14.0, sizeSpinner.getValue());

        Spinner<Double> lhSpinner = (Spinner<Double>) inputs.get("csFont_lineHeight");
        assertEquals(1.3, lhSpinner.getValue());

        CheckBox ligatures = (CheckBox) inputs.get("csFont_enableLigatures");
        assertTrue(ligatures.isSelected());
    }

    @Test
    public void testShowOnlyMonospacedFilter() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();
        ColorSchemeFontPanel.build(settings, inputs, null);

        ComboBox<String> fontCombo = (ComboBox<String>) inputs.get("csFont_fontFamily");
        CheckBox monoCheck = (CheckBox) inputs.get("csFont_showOnlyMonospaced");
        assertNotNull(fontCombo);
        assertNotNull(monoCheck);

        int countWithOnlyMono = fontCombo.getItems().size();
        assertTrue(countWithOnlyMono > 0);

        // Turn off filter
        monoCheck.setSelected(false);
        monoCheck.getOnAction().handle(null);
        int countAll = fontCombo.getItems().size();
        assertTrue(countAll >= countWithOnlyMono, "All fonts count should be >= monospaced count");
    }
}

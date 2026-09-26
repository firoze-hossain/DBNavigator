package com.roze.dbnavigator.ui.colorscheme;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.ColorSchemeAttribute;
import com.roze.dbnavigator.ui.colorscheme.ColorSchemeModel.ColorSchemeElement;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DataEditorViewerPanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testDataEditorElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getDataEditorViewerElements();
        assertEquals(4, elements.size(), "Data Editor and Viewer should have 4 elements matching DataGrip screenshot 2");

        List<String> categories = ColorSchemeModel.getDataEditorViewerCategories();
        assertEquals(1, categories.size());
        assertEquals("Data Grid", categories.get(0));

        // Test Error data defaults matching Image 2
        ColorSchemeElement errorData = ColorSchemeModel.getElement("data_editor.error_data");
        assertNotNull(errorData);
        assertEquals("Error data", errorData.getName());
        assertEquals("Data Grid", errorData.getCategory());
        assertEquals("Underwaved", errorData.getDefaultAttr().effectType);
        assertEquals("FA6675", errorData.getDefaultAttr().effectColor);
        assertEquals("D6405B", errorData.getDefaultAttr().errorStripe);
        assertTrue(errorData.hasInheritance());
        assertEquals("errors.error", errorData.getInheritFromKey());
        assertTrue(errorData.getInheritFromDisplay().contains("Error"));

        // Test Alternating row color
        ColorSchemeElement altRow = ColorSchemeModel.getElement("data_editor.alternating_row_color");
        assertNotNull(altRow);
        assertEquals("25272B", altRow.getDefaultAttr().background);

        // Test Image data
        ColorSchemeElement imgData = ColorSchemeModel.getElement("data_editor.image_data");
        assertNotNull(imgData);
        assertEquals("393B40", imgData.getDefaultAttr().background);

        // Test Null data
        ColorSchemeElement nullData = ColorSchemeModel.getElement("data_editor.null_data");
        assertNotNull(nullData);
        assertEquals("7A7E85", nullData.getDefaultAttr().foreground);
        assertTrue(nullData.getDefaultAttr().italic);
    }

    @Test
    public void testDataEditorOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("data_editor.alternating_row_color", new ColorSchemeAttribute(false, false, null, false, "333333", true, null, false, null, false, "Underscored", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "data_editor.alternating_row_color", overrides);
        assertNotNull(resolved);
        assertTrue(resolved.backgroundEnabled);
        assertEquals("333333", resolved.background);
    }

    @Test
    public void testBuildDataEditorPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = DataEditorViewerPanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

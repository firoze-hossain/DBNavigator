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

public class JsonColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testJsonElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getJsonElements();
        assertEquals(14, elements.size(), "JSON should have 14 elements matching DataGrip screenshot 4");

        String[] expectedIds = {
                "json.block_comment",
                "json.braces",
                "json.brackets",
                "json.colon",
                "json.comma",
                "json.invalid_escape",
                "json.keyword",
                "json.line_comment",
                "json.number",
                "json.parameter",
                "json.property_key",
                "json.semantic_highlighting",
                "json.string",
                "json.valid_escape"
        };

        for (String id : expectedIds) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected element: " + id);
            assertEquals("JSON", el.getPage());
        }

        // Test Keyword defaults matching Image 4
        ColorSchemeElement keyword = ColorSchemeModel.getElement("json.keyword");
        assertNotNull(keyword);
        assertEquals("Keyword", keyword.getName());
        assertEquals("CF8E6D", keyword.getDefaultAttr().foreground);
        assertEquals("Bordered", keyword.getDefaultAttr().effectType);
        assertTrue(keyword.hasInheritance());
        assertEquals("lang.keyword", keyword.getInheritFromKey());
        assertTrue(keyword.getInheritFromDisplay().contains("Keyword"));

        // Test Invalid escape sequence
        ColorSchemeElement invalidEscape = ColorSchemeModel.getElement("json.invalid_escape");
        assertNotNull(invalidEscape);
        assertEquals("FA6675", invalidEscape.getDefaultAttr().foreground);
        assertEquals("Underwaved", invalidEscape.getDefaultAttr().effectType);

        // Test String
        ColorSchemeElement stringEl = ColorSchemeModel.getElement("json.string");
        assertNotNull(stringEl);
        assertEquals("6A8759", stringEl.getDefaultAttr().foreground);

        // Test Number
        ColorSchemeElement numberEl = ColorSchemeModel.getElement("json.number");
        assertNotNull(numberEl);
        assertEquals("2AACB8", numberEl.getDefaultAttr().foreground);
    }

    @Test
    public void testJsonOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("json.keyword", new ColorSchemeAttribute(true, true, "FF0000", true, null, false, null, false, null, false, "Underscored", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "json.keyword", overrides);
        assertNotNull(resolved);
        assertTrue(resolved.bold);
        assertTrue(resolved.italic);
        assertEquals("FF0000", resolved.foreground);
    }

    @Test
    public void testBuildJsonPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = JsonColorSchemePanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

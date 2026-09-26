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

public class RegExpColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testRegExpElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getRegExpElements();
        assertEquals(18, elements.size(), "RegExp should have 18 elements matching DataGrip screenshot 1");

        String[] expectedIds = {
                "regexp.bad_character",
                "regexp.brace",
                "regexp.bracket",
                "regexp.character_class",
                "regexp.comma",
                "regexp.comment",
                "regexp.dot",
                "regexp.escaped_character",
                "regexp.inline_option",
                "regexp.invalid_escape_sequence",
                "regexp.matched_groups",
                "regexp.name",
                "regexp.operator_character",
                "regexp.parenthesis",
                "regexp.plain_character",
                "regexp.quantifier",
                "regexp.quote_escape",
                "regexp.redundant_escape_sequence"
        };

        for (String id : expectedIds) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected element: " + id);
            assertEquals("RegExp", el.getPage());
        }

        // Test Escaped character defaults matching Image 1
        ColorSchemeElement escaped = ColorSchemeModel.getElement("regexp.escaped_character");
        assertNotNull(escaped);
        assertEquals("Escaped character", escaped.getName());
        assertEquals("CF8E6D", escaped.getDefaultAttr().foreground);
        assertEquals("Bordered", escaped.getDefaultAttr().effectType);
        assertTrue(escaped.hasInheritance());
        assertEquals("lang.string.valid_escape", escaped.getInheritFromKey());
        assertTrue(escaped.getInheritFromDisplay().contains("Escape sequence"));

        // Test Invalid escape sequence
        ColorSchemeElement invalidEscape = ColorSchemeModel.getElement("regexp.invalid_escape_sequence");
        assertNotNull(invalidEscape);
        assertEquals("FA6675", invalidEscape.getDefaultAttr().foreground);
        assertEquals("Underwaved", invalidEscape.getDefaultAttr().effectType);

        // Test Matched groups
        ColorSchemeElement matched = ColorSchemeModel.getElement("regexp.matched_groups");
        assertNotNull(matched);
        assertEquals("2AACB8", matched.getDefaultAttr().foreground);

        // Test Bracket
        ColorSchemeElement bracket = ColorSchemeModel.getElement("regexp.bracket");
        assertNotNull(bracket);
        assertEquals("E8BF6A", bracket.getDefaultAttr().foreground);
    }

    @Test
    public void testRegExpOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("regexp.escaped_character", new ColorSchemeAttribute(true, false, "FFAA00", true, null, false, null, false, null, false, "Underscored", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "regexp.escaped_character", overrides);
        assertNotNull(resolved);
        assertTrue(resolved.bold);
        assertEquals("FFAA00", resolved.foreground);
    }

    @Test
    public void testBuildRegExpPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = RegExpColorSchemePanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

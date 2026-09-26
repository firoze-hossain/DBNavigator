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

public class XPathColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testXPathElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getXPathElements();
        assertEquals(11, elements.size(), "XPath should have 11 elements matching DataGrip screenshot 2");

        String[] expectedIds = {
                "xpath.brackets",
                "xpath.extension_prefix",
                "xpath.function",
                "xpath.keyword",
                "xpath.name",
                "xpath.number",
                "xpath.operator",
                "xpath.other",
                "xpath.parentheses",
                "xpath.string",
                "xpath.variable"
        };

        for (String id : expectedIds) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected XPath element: " + id);
            assertEquals("XPath", el.getPage());
        }

        // Test Number matching Screenshot 2
        ColorSchemeElement number = ColorSchemeModel.getElement("xpath.number");
        assertNotNull(number);
        assertEquals("Number", number.getName());
        assertEquals("2AACB8", number.getDefaultAttr().foreground);
        assertTrue(number.hasInheritance());
        assertEquals("lang.number", number.getInheritFromKey());
        assertTrue(number.getInheritFromDisplay().contains("Number"));

        // Test Keyword
        ColorSchemeElement keyword = ColorSchemeModel.getElement("xpath.keyword");
        assertNotNull(keyword);
        assertEquals("CF8E6D", keyword.getDefaultAttr().foreground);

        // Test Function
        ColorSchemeElement func = ColorSchemeModel.getElement("xpath.function");
        assertNotNull(func);
        assertEquals("56A8F5", func.getDefaultAttr().foreground);

        // Test String
        ColorSchemeElement str = ColorSchemeModel.getElement("xpath.string");
        assertNotNull(str);
        assertEquals("6A8759", str.getDefaultAttr().foreground);
    }

    @Test
    public void testXPathOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> darkOverrides = new LinkedHashMap<>();

        darkOverrides.put("xpath.number", new ColorSchemeAttribute(
                false, false, "FF4444", true, null, false, null, false, null, false, "Underscored", false, null
        ));
        overrides.put("Dark Theme default", darkOverrides);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "xpath.number", overrides);
        assertNotNull(resolved);
        assertEquals("FF4444", resolved.foreground);
    }

    @Test
    public void testXPathPanelBuild() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = XPathColorSchemePanel.build(settings, inputs, path -> {});
        assertNotNull(panel);
        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

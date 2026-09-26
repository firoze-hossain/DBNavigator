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

public class HtmlColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testHtmlElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getHtmlElements();
        assertEquals(15, elements.size(), "HTML should have 15 elements matching DataGrip screenshot 1");

        String[] expectedIds = {
                "html.attribute_name",
                "html.attribute_value",
                "html.comment",
                "html.custom_tag_name",
                "html.entity_reference",
                "html.html_code",
                "html.injected_language_fragment",
                "html.tag",
                "html.tag_name",
                "html.tag_tree_1",
                "html.tag_tree_2",
                "html.tag_tree_3",
                "html.tag_tree_4",
                "html.tag_tree_5",
                "html.tag_tree_6"
        };

        for (String id : expectedIds) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected element: " + id);
            assertEquals("HTML", el.getPage());
        }

        // Test Custom Tag Name defaults matching Image 1
        ColorSchemeElement customTag = ColorSchemeModel.getElement("html.custom_tag_name");
        assertNotNull(customTag);
        assertEquals("Custom Tag Name", customTag.getName());
        assertEquals("2FBAA3", customTag.getDefaultAttr().foreground);
        assertEquals("Bordered", customTag.getDefaultAttr().effectType);
        assertTrue(customTag.hasInheritance());
        assertEquals("html.tag_name", customTag.getInheritFromKey());
        assertTrue(customTag.getInheritFromDisplay().contains("Tag name"));

        // Test Comment
        ColorSchemeElement comment = ColorSchemeModel.getElement("html.comment");
        assertNotNull(comment);
        assertEquals("808080", comment.getDefaultAttr().foreground);

        // Test Tag Name
        ColorSchemeElement tagName = ColorSchemeModel.getElement("html.tag_name");
        assertNotNull(tagName);
        assertEquals("E8BF6A", tagName.getDefaultAttr().foreground);
    }

    @Test
    public void testHtmlOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("html.custom_tag_name", new ColorSchemeAttribute(true, false, "123456", true, null, false, null, false, null, false, "Underscored", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "html.custom_tag_name", overrides);
        assertNotNull(resolved);
        assertTrue(resolved.bold);
        assertEquals("123456", resolved.foreground);
    }

    @Test
    public void testBuildHtmlPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = HtmlColorSchemePanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

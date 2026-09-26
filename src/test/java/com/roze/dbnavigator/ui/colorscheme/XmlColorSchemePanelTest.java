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

public class XmlColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testXmlElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getXmlElements();
        assertEquals(12, elements.size(), "XML should have 12 elements matching DataGrip screenshot 3");

        String[] expectedIds = {
                "xml.attribute_name",
                "xml.attribute_value",
                "xml.comment",
                "xml.custom_tag_name",
                "xml.entity_reference",
                "xml.injected_language",
                "xml.matched_tag",
                "xml.namespace_prefix",
                "xml.prologue",
                "xml.tag",
                "xml.tag_data",
                "xml.tag_name"
        };

        for (String id : expectedIds) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected XML element: " + id);
            assertEquals("XML", el.getPage());
        }

        // Test Entity Reference matching Screenshot 3
        ColorSchemeElement entity = ColorSchemeModel.getElement("xml.entity_reference");
        assertNotNull(entity);
        assertEquals("Entity Reference", entity.getName());
        assertEquals("56A8F5", entity.getDefaultAttr().foreground);
        assertTrue(entity.hasInheritance());
        assertEquals("lang.markup.entity", entity.getInheritFromKey());
        assertTrue(entity.getInheritFromDisplay().contains("Entity"));

        // Test Custom Tag Name
        ColorSchemeElement customTag = ColorSchemeModel.getElement("xml.custom_tag_name");
        assertNotNull(customTag);
        assertEquals("2FBAA3", customTag.getDefaultAttr().foreground);
        assertEquals("Bordered", customTag.getDefaultAttr().effectType);

        // Test Matched Tag
        ColorSchemeElement matchedTag = ColorSchemeModel.getElement("xml.matched_tag");
        assertNotNull(matchedTag);
        assertEquals("3B514D", matchedTag.getDefaultAttr().background);

        // Test Comment
        ColorSchemeElement comment = ColorSchemeModel.getElement("xml.comment");
        assertNotNull(comment);
        assertEquals("808080", comment.getDefaultAttr().foreground);

        // Test Injected Language Fragment
        ColorSchemeElement injected = ColorSchemeModel.getElement("xml.injected_language");
        assertNotNull(injected);
        assertEquals("363636", injected.getDefaultAttr().background);
    }

    @Test
    public void testXmlOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> darkOverrides = new LinkedHashMap<>();

        darkOverrides.put("xml.entity_reference", new ColorSchemeAttribute(
                false, false, "FF9900", true, null, false, null, false, null, false, "Underscored", false, null
        ));
        overrides.put("Dark Theme default", darkOverrides);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "xml.entity_reference", overrides);
        assertNotNull(resolved);
        assertEquals("FF9900", resolved.foreground);
    }

    @Test
    public void testXmlPanelBuild() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = XmlColorSchemePanel.build(settings, inputs, path -> {});
        assertNotNull(panel);
        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

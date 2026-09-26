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

public class MarkdownColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testMarkdownElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getMarkdownElements();
        assertEquals(40, elements.size(), "Markdown should have 40 elements matching DataGrip screenshots 2-5");

        List<String> categories = ColorSchemeModel.getMarkdownCategories();
        assertTrue(categories.contains("Blockquote"));
        assertTrue(categories.contains("Code"));
        assertTrue(categories.contains("Definition list"));
        assertTrue(categories.contains("Header"));
        assertTrue(categories.contains("HTML"));
        assertTrue(categories.contains("Links"));
        assertTrue(categories.contains("Lists"));
        assertTrue(categories.contains("Style"));

        // Test Blockquote (Image 3)
        ColorSchemeElement bq = ColorSchemeModel.getElement("markdown.blockquote.blockquote");
        assertNotNull(bq);
        assertEquals("Blockquote", bq.getName());
        assertEquals("6AAB73", bq.getDefaultAttr().foreground);
        assertTrue(bq.hasInheritance());
        assertEquals("lang.string", bq.getInheritFromKey());

        // Test 2nd level header (Image 4)
        ColorSchemeElement h2 = ColorSchemeModel.getElement("markdown.header.level_2");
        assertNotNull(h2);
        assertEquals("2nd level header", h2.getName());
        assertEquals("C77DBB", h2.getDefaultAttr().foreground);
        assertTrue(h2.getDefaultAttr().italic);
        assertTrue(h2.hasInheritance());
        assertEquals("lang.constant", h2.getInheritFromKey());

        // Test Bold marker (Image 2)
        ColorSchemeElement boldMarker = ColorSchemeModel.getElement("markdown.style.bold_marker");
        assertNotNull(boldMarker);
        assertEquals("Bold marker", boldMarker.getName());
        assertEquals("CF8E6D", boldMarker.getDefaultAttr().foreground);
        assertTrue(boldMarker.hasInheritance());
        assertEquals("lang.keyword", boldMarker.getInheritFromKey());

        // Test Bold text
        ColorSchemeElement boldText = ColorSchemeModel.getElement("markdown.style.bold_text");
        assertNotNull(boldText);
        assertTrue(boldText.getDefaultAttr().bold);

        // Test Strikethrough
        ColorSchemeElement strike = ColorSchemeModel.getElement("markdown.style.strikethrough");
        assertNotNull(strike);
        assertEquals("Strikeout", strike.getDefaultAttr().effectType);

        // Test Horizontal rule
        ColorSchemeElement hr = ColorSchemeModel.getElement("markdown.horizontal_rule");
        assertNotNull(hr);
        assertEquals("7A7E85", hr.getDefaultAttr().foreground);
    }

    @Test
    public void testMarkdownOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("markdown.style.bold_marker", new ColorSchemeAttribute(true, true, "00FF00", true, null, false, null, false, null, false, "Underscored", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "markdown.style.bold_marker", overrides);
        assertNotNull(resolved);
        assertTrue(resolved.bold);
        assertTrue(resolved.italic);
        assertEquals("00FF00", resolved.foreground);
    }

    @Test
    public void testBuildMarkdownPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = MarkdownColorSchemePanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

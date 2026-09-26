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

public class UserDefinedFileTypesPanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testUserDefinedElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getUserDefinedFileTypeElements();
        assertEquals(10, elements.size(), "User-Defined File Types should have 10 elements matching DataGrip screenshot 3");

        String[] expectedIds = {
                "user_types.block_comment",
                "user_types.invalid_string_escape",
                "user_types.keyword1",
                "user_types.keyword2",
                "user_types.keyword3",
                "user_types.keyword4",
                "user_types.line_comment",
                "user_types.number",
                "user_types.string",
                "user_types.valid_string_escape"
        };

        for (String id : expectedIds) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected element: " + id);
            assertEquals("User-Defined File Types", el.getPage());
        }

        // Test Keyword1 defaults from Image 3
        ColorSchemeElement kw1 = ColorSchemeModel.getElement("user_types.keyword1");
        assertNotNull(kw1);
        assertEquals("Keyword1", kw1.getName());
        assertEquals("CF8E6D", kw1.getDefaultAttr().foreground);
        assertEquals("Bordered", kw1.getDefaultAttr().effectType);
        assertTrue(kw1.hasInheritance());
        assertEquals("lang.keyword", kw1.getInheritFromKey());
        assertEquals("Keyword (Language Defaults)", kw1.getInheritFromDisplay());
    }

    @Test
    public void testKeyword1InheritanceResolutionFromLanguageDefaults() {
        // By default without override, keyword1 has its own foreground CF8E6D (Image 3)
        ColorSchemeAttribute def = ColorSchemeModel.resolveAttribute("Dark Theme default", "user_types.keyword1", null);
        assertNotNull(def);
        assertEquals("CF8E6D", def.foreground);

        // When keyword1 has an override with inherit = true, it inherits from lang.keyword
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("lang.keyword", new ColorSchemeAttribute(false, false, "00FF00", true, null, false, null, false, null, false, "Bordered", false, null));
        schemeMap.put("user_types.keyword1", new ColorSchemeAttribute(false, false, null, false, null, false, null, false, null, false, "Bordered", true, "lang.keyword"));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolvedWithParent = ColorSchemeModel.resolveAttribute("Dark Theme default", "user_types.keyword1", overrides);
        assertEquals("00FF00", resolvedWithParent.foreground);

        // When keyword1 has its own direct override with inherit = false
        schemeMap.put("user_types.keyword1", new ColorSchemeAttribute(false, false, "112233", true, null, false, null, false, null, false, "Bordered", false, null));
        ColorSchemeAttribute direct = ColorSchemeModel.resolveAttribute("Dark Theme default", "user_types.keyword1", overrides);
        assertEquals("112233", direct.foreground);
    }

    @Test
    public void testBuildUserDefinedFileTypesPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = UserDefinedFileTypesPanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

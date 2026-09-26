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

public class SqlColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testSqlElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getSqlElements();
        assertEquals(21, elements.size(), "SQL should have 21 elements matching DataGrip screenshot 1");

        String[] expectedIds = {
                "sql.bad_token",
                "sql.braces",
                "sql.brackets",
                "sql.column",
                "sql.comma",
                "sql.comment",
                "sql.database_object",
                "sql.dot",
                "sql.external_command",
                "sql.keyword",
                "sql.label",
                "sql.number_token",
                "sql.outer_query_column",
                "sql.parameter",
                "sql.parentheses",
                "sql.procedure",
                "sql.quoted_identifier",
                "sql.semicolon",
                "sql.string",
                "sql.type",
                "sql.variable"
        };

        for (String id : expectedIds) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected SQL element: " + id);
            assertEquals("SQL", el.getPage());
        }

        // Test Keyword element matching Screenshot 1
        ColorSchemeElement keyword = ColorSchemeModel.getElement("sql.keyword");
        assertNotNull(keyword);
        assertEquals("Keyword", keyword.getName());
        assertEquals("CF8E6D", keyword.getDefaultAttr().foreground);
        assertTrue(keyword.hasInheritance());
        assertEquals("lang.keyword", keyword.getInheritFromKey());
        assertTrue(keyword.getInheritFromDisplay().contains("Language Defaults"));

        // Test Bad token
        ColorSchemeElement badToken = ColorSchemeModel.getElement("sql.bad_token");
        assertNotNull(badToken);
        assertEquals("FA6675", badToken.getDefaultAttr().foreground);
        assertEquals("Underwaved", badToken.getDefaultAttr().effectType);

        // Test Number token
        ColorSchemeElement number = ColorSchemeModel.getElement("sql.number_token");
        assertNotNull(number);
        assertEquals("2AACB8", number.getDefaultAttr().foreground);

        // Test String
        ColorSchemeElement str = ColorSchemeModel.getElement("sql.string");
        assertNotNull(str);
        assertEquals("6A8759", str.getDefaultAttr().foreground);
        assertTrue(str.hasInheritance());
        assertEquals("lang.string", str.getInheritFromKey());

        // Test Parameter
        ColorSchemeElement param = ColorSchemeModel.getElement("sql.parameter");
        assertNotNull(param);
        assertEquals("3D7A49", param.getDefaultAttr().foreground);
    }

    @Test
    public void testSqlOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> darkOverrides = new LinkedHashMap<>();

        // Override keyword foreground to 00FF00
        darkOverrides.put("sql.keyword", new ColorSchemeAttribute(
                true, false, "00FF00", true, null, false, null, false, null, false, "Underscored", false, null
        ));
        overrides.put("Dark Theme default", darkOverrides);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "sql.keyword", overrides);
        assertNotNull(resolved);
        assertTrue(resolved.bold);
        assertEquals("00FF00", resolved.foreground);

        // Default without override should inherit lang.keyword
        ColorSchemeAttribute defResolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "sql.keyword", null);
        assertNotNull(defResolved);
        assertEquals("CF8E6D", defResolved.foreground);
    }

    @Test
    public void testSqlPanelBuild() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = SqlColorSchemePanel.build(settings, inputs, path -> {});
        assertNotNull(panel);
        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

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

public class DatabaseColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testDatabaseElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getDatabaseElements();
        assertEquals(3, elements.size(), "Database should have 3 elements matching DataGrip screenshot 5");

        List<String> categories = ColorSchemeModel.getDatabaseCategories();
        assertEquals(2, categories.size());
        assertEquals("Console", categories.get(0));
        assertEquals("Parameters", categories.get(1));

        // Test Statement to execute defaults matching Image 5
        ColorSchemeElement stmt = ColorSchemeModel.getElement("database.statement_to_execute");
        assertNotNull(stmt);
        assertEquals("Statement to execute", stmt.getName());
        assertEquals("Console", stmt.getCategory());
        assertEquals("Bordered", stmt.getDefaultAttr().effectType);
        assertEquals("3D7A49", stmt.getDefaultAttr().effectColor);
        assertTrue(stmt.getDefaultAttr().effectEnabled);

        // Test Parameter
        ColorSchemeElement param = ColorSchemeModel.getElement("database.parameter");
        assertNotNull(param);
        assertEquals("Parameter", param.getName());
        assertEquals("Parameters", param.getCategory());
        assertEquals("3D7A49", param.getDefaultAttr().foreground);

        // Test Parameter usage
        ColorSchemeElement usage = ColorSchemeModel.getElement("database.parameter_usage");
        assertNotNull(usage);
        assertEquals("Parameter usage", usage.getName());
        assertEquals("Parameters", usage.getCategory());
        assertEquals("264D3B", usage.getDefaultAttr().background);
        assertEquals("3D7A49", usage.getDefaultAttr().effectColor);
        assertEquals("Bordered", usage.getDefaultAttr().effectType);
    }

    @Test
    public void testDatabaseOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("database.statement_to_execute", new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "FF00FF", true, "Bordered", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "database.statement_to_execute", overrides);
        assertNotNull(resolved);
        assertTrue(resolved.effectEnabled);
        assertEquals("FF00FF", resolved.effectColor);
    }

    @Test
    public void testBuildDatabasePanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = DatabaseColorSchemePanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

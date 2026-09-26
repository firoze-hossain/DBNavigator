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

public class TableDiffColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testTableDiffElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getTableDiffElements();
        assertEquals(3, elements.size(), "Table Diff should have 3 elements matching DataGrip screenshot 4");

        String[] expectedIds = {
                "table_diff.excluded",
                "table_diff.fuzzy_matched",
                "table_diff.fuzzy_mismatched"
        };

        for (String id : expectedIds) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected Table Diff element: " + id);
            assertEquals("Table Diff", el.getPage());
        }

        // Test Fuzzy match - mismatched element matching Screenshot 4
        ColorSchemeElement mismatched = ColorSchemeModel.getElement("table_diff.fuzzy_mismatched");
        assertNotNull(mismatched);
        assertEquals("Fuzzy match - mismatched", mismatched.getName());
        assertEquals("114957", mismatched.getDefaultAttr().background);
        assertEquals("72D6D6", mismatched.getDefaultAttr().errorStripe);
        assertEquals("165E70", mismatched.getDefaultAttr().effectColor);
        assertEquals("Bordered", mismatched.getDefaultAttr().effectType);
        assertTrue(mismatched.hasInheritance());
        assertEquals("gen.search_result", mismatched.getInheritFromKey());
        assertTrue(mismatched.getInheritFromDisplay().contains("Search Results"));

        // Test Excluded from diff
        ColorSchemeElement excluded = ColorSchemeModel.getElement("table_diff.excluded");
        assertNotNull(excluded);
        assertEquals("2E3A3B", excluded.getDefaultAttr().background);

        // Test Fuzzy match - matched
        ColorSchemeElement matched = ColorSchemeModel.getElement("table_diff.fuzzy_matched");
        assertNotNull(matched);
        assertEquals("1E3A4B", matched.getDefaultAttr().background);
    }

    @Test
    public void testTableDiffOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> darkOverrides = new LinkedHashMap<>();

        darkOverrides.put("table_diff.fuzzy_mismatched", new ColorSchemeAttribute(
                false, false, "FFFFFF", true, "225566", true, "88EEDD", true, "337788", true, "Bordered", false, null
        ));
        overrides.put("Dark Theme default", darkOverrides);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "table_diff.fuzzy_mismatched", overrides);
        assertNotNull(resolved);
        assertEquals("225566", resolved.background);
        assertEquals("88EEDD", resolved.errorStripe);
    }

    @Test
    public void testTableDiffPanelBuild() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = TableDiffColorSchemePanel.build(settings, inputs, path -> {});
        assertNotNull(panel);
        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

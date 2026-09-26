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

public class DiffMergePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testDiffMergeCategoriesAndElementsMatchDataGrip() {
        List<String> categories = ColorSchemeModel.getDiffMergeCategories();
        assertEquals(2, categories.size(), "Diff & Merge should have 2 categories");
        assertEquals("Changed lines", categories.get(0));
        assertEquals("Folded unchanged fragments", categories.get(1));

        List<ColorSchemeElement> elements = ColorSchemeModel.getDiffMergeElements();
        assertEquals(5, elements.size(), "Diff & Merge should have 5 elements matching DataGrip screenshots 2 & 4");

        // Verify Deleted lines (Image 2)
        ColorSchemeElement deleted = ColorSchemeModel.getElement("diff.deleted");
        assertNotNull(deleted);
        assertEquals("Deleted", deleted.getName());
        assertEquals("Changed lines", deleted.getCategory());
        assertEquals("484A4A", deleted.getDefaultAttr().background);
        assertEquals("656E76", deleted.getDefaultAttr().errorStripe);
        assertTrue(deleted.getDefaultAttr().inheritIgnored);

        // Verify Changed lines
        ColorSchemeElement changed = ColorSchemeModel.getElement("diff.changed");
        assertNotNull(changed);
        assertEquals("2E436E", changed.getDefaultAttr().background);

        // Verify Conflict
        ColorSchemeElement conflict = ColorSchemeModel.getElement("diff.conflict");
        assertNotNull(conflict);
        assertEquals("5E3838", conflict.getDefaultAttr().background);

        // Verify Inserted
        ColorSchemeElement inserted = ColorSchemeModel.getElement("diff.inserted");
        assertNotNull(inserted);
        assertEquals("294436", inserted.getDefaultAttr().background);

        // Verify Wave (Image 4)
        ColorSchemeElement wave = ColorSchemeModel.getElement("diff.folded_wave");
        assertNotNull(wave);
        assertEquals("Wave", wave.getName());
        assertEquals("Folded unchanged fragments", wave.getCategory());
        assertEquals("555555", wave.getDefaultAttr().foreground);
        assertTrue(wave.getDefaultAttr().foregroundEnabled);
    }

    @Test
    public void testDiffMergeOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("diff.deleted", new ColorSchemeAttribute(false, false, null, false, "FF0000", true, "AA0000", true, null, false, "Underscored", false, null, "330000", true, false));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "diff.deleted", overrides);
        assertNotNull(resolved);
        assertEquals("FF0000", resolved.background);
        assertEquals("AA0000", resolved.errorStripe);
        assertEquals("330000", resolved.ignoredColor);
        assertFalse(resolved.inheritIgnored);
    }

    @Test
    public void testBuildDiffMergePanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = DiffMergePanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

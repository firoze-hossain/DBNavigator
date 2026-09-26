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

public class VcsPanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testVcsCategoriesAndElementsMatchDataGrip() {
        List<String> categories = ColorSchemeModel.getVcsCategories();
        assertEquals(2, categories.size(), "VCS should have 2 categories");
        assertEquals("Editor Gutter", categories.get(0));
        assertEquals("VCS Annotations", categories.get(1));

        List<ColorSchemeElement> elements = ColorSchemeModel.getVcsElements();
        assertEquals(16, elements.size(), "VCS should have 16 elements (9 gutter + 7 annotations) matching DataGrip screenshot 1");

        // Verify Deleted lines (Image 1)
        ColorSchemeElement deletedLines = ColorSchemeModel.getElement("vcs.gutter.deleted_lines");
        assertNotNull(deletedLines);
        assertEquals("Deleted lines", deletedLines.getName());
        assertEquals("Editor Gutter", deletedLines.getCategory());
        assertTrue(deletedLines.getDefaultAttr().backgroundEnabled);
        assertEquals("868A91", deletedLines.getDefaultAttr().background);

        // Verify Added lines
        ColorSchemeElement addedLines = ColorSchemeModel.getElement("vcs.gutter.added_lines");
        assertNotNull(addedLines);
        assertEquals("436946", addedLines.getDefaultAttr().background);

        // Verify Modified lines
        ColorSchemeElement modifiedLines = ColorSchemeModel.getElement("vcs.gutter.modified_lines");
        assertNotNull(modifiedLines);
        assertEquals("385570", modifiedLines.getDefaultAttr().background);

        // Verify Annotations background #1 and foreground
        ColorSchemeElement a1 = ColorSchemeModel.getElement("vcs.annotations.bg_color_1");
        assertNotNull(a1);
        assertEquals("25324D", a1.getDefaultAttr().background);

        ColorSchemeElement aFg = ColorSchemeModel.getElement("vcs.annotations.foreground");
        assertNotNull(aFg);
        assertEquals("868A91", aFg.getDefaultAttr().foreground);
    }

    @Test
    public void testVcsOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("vcs.gutter.deleted_lines", new ColorSchemeAttribute(false, false, null, false, "FF5555", true, null, false, null, false, "Underscored", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "vcs.gutter.deleted_lines", overrides);
        assertNotNull(resolved);
        assertEquals("FF5555", resolved.background);
    }

    @Test
    public void testBuildVcsPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = VcsPanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

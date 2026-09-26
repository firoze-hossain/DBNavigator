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

public class ByScopeColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testByScopeElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getByScopeElements();
        assertEquals(4, elements.size(), "By Scope should have 4 elements matching DataGrip screenshot 2");

        ColorSchemeElement all = ColorSchemeModel.getElement("by_scope.all");
        assertNotNull(all);
        assertEquals("All", all.getName());
        assertEquals("By Scope", all.getPage());

        ColorSchemeElement nonProject = ColorSchemeModel.getElement("by_scope.non_project_files");
        assertNotNull(nonProject);
        assertEquals("Non-Project Files", nonProject.getName());
        assertEquals("Bordered", nonProject.getDefaultAttr().effectType);

        ColorSchemeElement openFiles = ColorSchemeModel.getElement("by_scope.open_files");
        assertNotNull(openFiles);
        assertEquals("Open Files", openFiles.getName());

        ColorSchemeElement scratches = ColorSchemeModel.getElement("by_scope.scratches_and_consoles");
        assertNotNull(scratches);
        assertEquals("Scratches and Consoles", scratches.getName());

        // Alias test
        ColorSchemeElement alias = ColorSchemeModel.getElement("by_scope.non_project");
        assertNotNull(alias);
        assertEquals("by_scope.non_project_files", alias.getId());
    }

    @Test
    public void testByScopeOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("by_scope.non_project_files", new ColorSchemeAttribute(true, false, "FF5555", true, "222222", true, null, false, null, false, "Bordered", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "by_scope.non_project_files", overrides);
        assertNotNull(resolved);
        assertTrue(resolved.bold);
        assertTrue(resolved.foregroundEnabled);
        assertEquals("FF5555", resolved.foreground);
        assertTrue(resolved.backgroundEnabled);
        assertEquals("222222", resolved.background);
    }

    @Test
    public void testBuildByScopePanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = ByScopeColorSchemePanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

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

public class ImagesColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testImagesElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getImagesElements();
        assertEquals(4, elements.size(), "Images should have 4 elements matching DataGrip screenshot 1");

        ColorSchemeElement blackCell = ColorSchemeModel.getElement("images.black_cell");
        assertNotNull(blackCell);
        assertEquals("'Black' cell", blackCell.getName());
        assertEquals("Images", blackCell.getPage());
        assertEquals("2B2D30", blackCell.getDefaultAttr().background);

        ColorSchemeElement whiteCell = ColorSchemeModel.getElement("images.white_cell");
        assertNotNull(whiteCell);
        assertEquals("'White' cell", whiteCell.getName());
        assertEquals("393B40", whiteCell.getDefaultAttr().background);

        ColorSchemeElement bg = ColorSchemeModel.getElement("images.background");
        assertNotNull(bg);
        assertEquals("Background", bg.getName());
        assertEquals("1E1F22", bg.getDefaultAttr().background);

        ColorSchemeElement gridLine = ColorSchemeModel.getElement("images.grid_line");
        assertNotNull(gridLine);
        assertEquals("Grid line", gridLine.getName());
        assertTrue(gridLine.getDefaultAttr().backgroundEnabled);
        assertEquals("C0C0C0", gridLine.getDefaultAttr().background);

        // Alias test
        ColorSchemeElement alias = ColorSchemeModel.getElement("images.grid");
        assertNotNull(alias);
        assertEquals("images.grid_line", alias.getId());
    }

    @Test
    public void testImagesOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("images.grid_line", new ColorSchemeAttribute(false, false, null, false, "FF0000", true, null, false, null, false, "Underscored", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "images.grid_line", overrides);
        assertNotNull(resolved);
        assertTrue(resolved.backgroundEnabled);
        assertEquals("FF0000", resolved.background);
    }

    @Test
    public void testBuildImagesPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = ImagesColorSchemePanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

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

public class DiagramsColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testDiagramsElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getDiagramsElements();
        assertEquals(25, elements.size(), "Diagrams should have 25 elements matching DataGrip screenshot 3");

        List<String> categories = ColorSchemeModel.getDiagramsCategories();
        assertTrue(categories.contains("Bends"));
        assertTrue(categories.contains("Edges"));
        assertTrue(categories.contains("Nodes"));
        assertTrue(categories.contains("Notes"));
        assertTrue(categories.contains("Selection Box"));

        // Test Edge selection defaults matching Image 3
        ColorSchemeElement edgeSel = ColorSchemeModel.getElement("diagrams.edges.edge_selection");
        assertNotNull(edgeSel);
        assertEquals("Edge selection", edgeSel.getName());
        assertEquals("Edges", edgeSel.getCategory());
        assertEquals("CC7832", edgeSel.getDefaultAttr().foreground);

        // Test Bad edge
        ColorSchemeElement badEdge = ColorSchemeModel.getElement("diagrams.edges.bad_edge");
        assertNotNull(badEdge);
        assertEquals("FF6B68", badEdge.getDefaultAttr().foreground);

        // Test Node background
        ColorSchemeElement nodeBg = ColorSchemeModel.getElement("diagrams.nodes.node_background");
        assertNotNull(nodeBg);
        assertEquals("313335", nodeBg.getDefaultAttr().background);

        // Test Snapping lines
        ColorSchemeElement snapping = ColorSchemeModel.getElement("diagrams.snapping_lines");
        assertNotNull(snapping);
        assertEquals("CC7832", snapping.getDefaultAttr().foreground);
    }

    @Test
    public void testDiagramsOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("diagrams.edges.edge_selection", new ColorSchemeAttribute(false, false, "FF9900", true, null, false, null, false, null, false, "Underscored", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "diagrams.edges.edge_selection", overrides);
        assertNotNull(resolved);
        assertTrue(resolved.foregroundEnabled);
        assertEquals("FF9900", resolved.foreground);
    }

    @Test
    public void testBuildDiagramsPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = DiagramsColorSchemePanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

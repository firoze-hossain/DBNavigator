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

public class DebuggerPanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testDebuggerElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getDebuggerElements();
        assertEquals(11, elements.size(), "Debugger should have exactly 11 elements matching DataGrip screenshot 5");

        String[] expectedIds = {
                "debugger.breakpoint_line",
                "debugger.evaluated_expression_text",
                "debugger.evaluated_expression_text_execution_line",
                "debugger.execution_point",
                "debugger.inlined_stack_frames",
                "debugger.inlined_modified_values",
                "debugger.inlined_values",
                "debugger.inlined_values_execution_line",
                "debugger.not_top_frame",
                "debugger.smart_step_into_selection",
                "debugger.smart_step_into_target"
        };

        for (String id : expectedIds) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected Debugger element: " + id);
            assertEquals("Debugger", el.getPage());
        }

        // Test Execution point defaults from Image 5
        ColorSchemeElement execPoint = ColorSchemeModel.getElement("debugger.execution_point");
        assertNotNull(execPoint);
        assertEquals("Execution point", execPoint.getName());
        assertTrue(execPoint.getDefaultAttr().backgroundEnabled);
        assertEquals("2A5091", execPoint.getDefaultAttr().background);
        assertEquals("Bordered", execPoint.getDefaultAttr().effectType);
    }

    @Test
    public void testDebuggerOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("debugger.execution_point", new ColorSchemeAttribute(true, false, "FFFFFF", true, "123456", true, null, false, null, false, "Bordered", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "debugger.execution_point", overrides);
        assertNotNull(resolved);
        assertEquals("123456", resolved.background);
        assertTrue(resolved.bold);
    }

    @Test
    public void testBuildDebuggerPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = DebuggerPanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

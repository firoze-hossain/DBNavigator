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

public class XsltColorSchemePanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testXsltElementsTaxonomyAndCountMatchDataGrip() {
        List<ColorSchemeElement> elements = ColorSchemeModel.getXsltElements();
        assertEquals(1, elements.size(), "XSLT should have 1 element matching DataGrip screenshot 5");

        ColorSchemeElement directive = ColorSchemeModel.getElement("xslt.directive");
        assertNotNull(directive, "Expected XSLT Directive element");
        assertEquals("XSLT", directive.getPage());
        assertEquals("XSLT Directive", directive.getName());
        assertEquals("2B2D30", directive.getDefaultAttr().background);
        assertTrue(directive.getDefaultAttr().backgroundEnabled);
        assertTrue(directive.hasInheritance());
        assertEquals("lang.template", directive.getInheritFromKey());
        assertTrue(directive.getInheritFromDisplay().contains("Template language"));
    }

    @Test
    public void testXsltOverrideResolution() {
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> darkOverrides = new LinkedHashMap<>();

        darkOverrides.put("xslt.directive", new ColorSchemeAttribute(
                false, false, null, false, "334455", true, null, false, null, false, "Underscored", false, null
        ));
        overrides.put("Dark Theme default", darkOverrides);

        ColorSchemeAttribute resolved = ColorSchemeModel.resolveAttribute("Dark Theme default", "xslt.directive", overrides);
        assertNotNull(resolved);
        assertEquals("334455", resolved.background);
    }

    @Test
    public void testXsltPanelBuild() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = XsltColorSchemePanel.build(settings, inputs, path -> {});
        assertNotNull(panel);
        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

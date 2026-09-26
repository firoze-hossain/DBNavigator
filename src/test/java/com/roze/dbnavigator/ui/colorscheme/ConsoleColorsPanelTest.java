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

public class ConsoleColorsPanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testConsoleColorCategoriesAndCountMatchDataGrip() {
        List<String> categories = ColorSchemeModel.getConsoleColorCategories();
        assertEquals(5, categories.size(), "Should have exactly 5 Console Colors categories");
        assertEquals("ANSI colors", categories.get(0));
        assertEquals("Console", categories.get(1));
        assertEquals("Log console", categories.get(2));
        assertEquals("Reworked terminal", categories.get(3));
        assertEquals("Terminal", categories.get(4));

        List<ColorSchemeElement> elements = ColorSchemeModel.getConsoleColorElements();
        assertEquals(62, elements.size(), "Should have exactly 62 Console Colors elements (16 ANSI + 5 Console + 6 Log console + 34 Reworked terminal + 1 Terminal)");
    }

    @Test
    public void testAnsiColorsTaxonomy() {
        String[] expectedAnsi = {
                "console.ansi.black", "console.ansi.blue", "console.ansi.bright_black",
                "console.ansi.bright_blue", "console.ansi.bright_cyan", "console.ansi.bright_green",
                "console.ansi.bright_magenta", "console.ansi.bright_red", "console.ansi.bright_white",
                "console.ansi.bright_yellow", "console.ansi.cyan", "console.ansi.green",
                "console.ansi.magenta", "console.ansi.red", "console.ansi.white", "console.ansi.yellow"
        };
        for (String id : expectedAnsi) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected ANSI color element to exist: " + id);
            assertEquals("Console Colors", el.getPage());
            assertEquals("ANSI colors", el.getCategory());
        }
    }

    @Test
    public void testConsoleAndLogCategoriesTaxonomy() {
        String[] expected = {
                "console.background", "console.error_output", "console.standard_output",
                "console.system_output", "console.user_input",
                "console.log.error", "console.log.warning", "console.log.info",
                "console.log.verbose", "console.log.debug", "console.log.expired"
        };
        for (String id : expected) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected console/log element to exist: " + id);
            assertEquals("Console Colors", el.getPage());
        }
    }

    @Test
    public void testReworkedTerminalAndTerminalTaxonomy() {
        String[] reworkedIds = {
                "console.reworked_terminal.black", "console.reworked_terminal.blue",
                "console.reworked_terminal.bright_black", "console.reworked_terminal.bright_blue",
                "console.reworked_terminal.bright_cyan", "console.reworked_terminal.bright_green",
                "console.reworked_terminal.bright_magenta", "console.reworked_terminal.bright_red",
                "console.reworked_terminal.bright_white", "console.reworked_terminal.bright_yellow",
                "console.reworked_terminal.command", "console.reworked_terminal.current_search_entry",
                "console.reworked_terminal.cyan", "console.reworked_terminal.default_background",
                "console.reworked_terminal.default_foreground", "console.reworked_terminal.error_block_border",
                "console.reworked_terminal.generate_command_caret_color",
                "console.reworked_terminal.generate_command_placeholder_foreground",
                "console.reworked_terminal.generate_command_prompt_text",
                "console.reworked_terminal.green", "console.reworked_terminal.inactive_selected_block_background",
                "console.reworked_terminal.inactive_selected_block_border",
                "console.reworked_terminal.magenta", "console.reworked_terminal.prompt_separator_color",
                "console.reworked_terminal.red", "console.reworked_terminal.reworked_background_gradient_end",
                "console.reworked_terminal.reworked_background_gradient_start",
                "console.reworked_terminal.reworked_hovered_background_gradient_end",
                "console.reworked_terminal.reworked_hovered_background_gradient_start",
                "console.reworked_terminal.search_entry", "console.reworked_terminal.selected_block_background",
                "console.reworked_terminal.selected_block_border", "console.reworked_terminal.white",
                "console.reworked_terminal.yellow"
        };
        assertEquals(34, reworkedIds.length);
        for (String id : reworkedIds) {
            ColorSchemeElement el = ColorSchemeModel.getElement(id);
            assertNotNull(el, "Expected Reworked terminal element to exist: " + id);
            assertEquals("Console Colors", el.getPage());
            assertEquals("Reworked terminal", el.getCategory());
        }

        // Test specific colors from DataGrip
        ColorSchemeElement blue = ColorSchemeModel.getElement("console.reworked_terminal.blue");
        assertEquals("5594FA", blue.getDefaultAttr().foreground);
        assertEquals("134EBF", blue.getDefaultAttr().background);

        ColorSchemeElement white = ColorSchemeModel.getElement("console.reworked_terminal.white");
        assertEquals("CED0D6", white.getDefaultAttr().foreground);
        assertEquals("CED0D6", white.getDefaultAttr().background);

        // Test Terminal: Command to run using IDE (DataGrip default bg #40503C)
        ColorSchemeElement cmdToRun = ColorSchemeModel.getElement("console.terminal.command_to_run_using_ide");
        assertNotNull(cmdToRun, "Command to run using IDE should exist in Terminal category");
        assertEquals("Command to run using IDE", cmdToRun.getName());
        assertEquals("Terminal", cmdToRun.getCategory());
        assertEquals("Console Colors", cmdToRun.getPage());
        assertTrue(cmdToRun.getDefaultAttr().backgroundEnabled);
        assertEquals("40503C", cmdToRun.getDefaultAttr().background);

        // Test backward compatibility aliases
        assertNotNull(ColorSchemeModel.getElement("console.reworked_terminal.cursor"));
        assertNotNull(ColorSchemeModel.getElement("console.reworked_terminal.selection"));
        assertNotNull(ColorSchemeModel.getElement("console.terminal.cursor"));
        assertNotNull(ColorSchemeModel.getElement("console.terminal.selection"));
    }

    @Test
    public void testInheritanceResolutionForBrightGreen() {
        ColorSchemeElement brightGreen = ColorSchemeModel.getElement("console.ansi.bright_green");
        assertNotNull(brightGreen);
        assertTrue(brightGreen.hasInheritance());
        assertEquals("console.ansi.green", brightGreen.getInheritFromKey());
        assertEquals("ANSI colors → Green (Console Colors)", brightGreen.getInheritFromDisplay());

        // Resolve default without overrides: should inherit from Green (57965C)
        ColorSchemeAttribute effective = ColorSchemeModel.resolveAttribute("Dark Theme default", "console.ansi.bright_green", null);
        assertNotNull(effective);
        assertEquals("57965C", effective.foreground);

        // When parent Green is overridden, child Bright Green should inherit the parent override
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        schemeMap.put("console.ansi.green", new ColorSchemeAttribute(false, false, "00FF00", true, null, false, null, false, null, false, "Underscored", false, null));
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolvedWithParentOverride = ColorSchemeModel.resolveAttribute("Dark Theme default", "console.ansi.bright_green", overrides);
        assertEquals("00FF00", resolvedWithParentOverride.foreground, "Should inherit parent green overridden foreground");

        // When Bright Green has its own non-inheriting override:
        schemeMap.put("console.ansi.bright_green", new ColorSchemeAttribute(true, false, "112233", true, null, false, null, false, null, false, "Underscored", false, null));
        ColorSchemeAttribute directOverride = ColorSchemeModel.resolveAttribute("Dark Theme default", "console.ansi.bright_green", overrides);
        assertEquals("112233", directOverride.foreground);
        assertTrue(directOverride.bold);
    }

    @Test
    public void testBuildConsoleColorsPanelRegistersInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        VBox panel = ConsoleColorsPanel.build(settings, inputs, null);
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
    }
}

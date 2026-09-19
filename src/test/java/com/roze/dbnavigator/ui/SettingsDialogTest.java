package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsDialogTest {

    @Test
    public void testRootCategoriesMatchDataGripScreenshots() {
        List<String> roots = SettingsDialog.getRootCategoryNames();
        assertEquals(10, roots.size(), "Should have exactly 10 root categories");

        assertEquals("Database", roots.get(0));
        assertEquals("Appearance & Behavior", roots.get(1));
        assertEquals("Keymap", roots.get(2));
        assertEquals("Editor", roots.get(3));
        assertEquals("Plugins", roots.get(4));
        assertEquals("Version Control", roots.get(5));
        assertEquals("Languages", roots.get(6));
        assertEquals("Tools", roots.get(7));
        assertEquals("Backup and Sync", roots.get(8));
        assertEquals("Advanced Settings", roots.get(9));
    }

    @Test
    public void testDatabaseSubcategoriesMatchDataGrip() {
        List<String> dbChildren = SettingsDialog.getChildCategoryNames("Database");
        assertNotNull(dbChildren);
        assertEquals(9, dbChildren.size(), "Database must have 9 direct children");

        assertEquals("Query Execution", dbChildren.get(0));
        assertEquals("Data Editor and Viewer", dbChildren.get(1));
        assertEquals("Database Explorer", dbChildren.get(2));
        assertEquals("CSV Formats", dbChildren.get(3));
        assertEquals("AI Tools", dbChildren.get(4));
        assertEquals("Query Files and Consoles", dbChildren.get(5));
        assertEquals("SQL Dialects", dbChildren.get(6));
        assertEquals("SQL Resolution Scopes", dbChildren.get(7));
        assertEquals("Other", dbChildren.get(8));

        // Subtree under Query Execution
        List<String> qChildren = SettingsDialog.getChildCategoryNames("Database / Query Execution");
        assertEquals(2, qChildren.size());
        assertEquals("Output and Results", qChildren.get(0));
        assertEquals("User Parameters", qChildren.get(1));
    }

    @Test
    public void testAppearanceBehaviorSubcategoriesMatchDataGrip() {
        List<String> appChildren = SettingsDialog.getChildCategoryNames("Appearance & Behavior");
        assertNotNull(appChildren);
        assertEquals(12, appChildren.size(), "Appearance & Behavior must have 12 direct children");

        assertEquals("Appearance", appChildren.get(0));
        assertEquals("Menus and Toolbars", appChildren.get(1));
        assertEquals("System Settings", appChildren.get(2));
        assertEquals("File Colors", appChildren.get(3));
        assertEquals("Scopes", appChildren.get(4));
        assertEquals("Notifications", appChildren.get(5));
        assertEquals("Data Editor and Viewer", appChildren.get(6));
        assertEquals("Quick Lists", appChildren.get(7));
        assertEquals("Required Plugins", appChildren.get(8));
        assertEquals("Trusted Locations", appChildren.get(9));
        assertEquals("Path Variables", appChildren.get(10));
        assertEquals("Presentation Assistant", appChildren.get(11));

        // Subtree under System Settings
        List<String> sysChildren = SettingsDialog.getChildCategoryNames("Appearance & Behavior / System Settings");
        assertEquals(7, sysChildren.size());
        assertEquals("Data Sharing", sysChildren.get(0));
        assertEquals("Date Formats", sysChildren.get(1));
        assertEquals("HTTP Proxy", sysChildren.get(2));
        assertEquals("Language and Region", sysChildren.get(3));
        assertEquals("Passwords", sysChildren.get(4));
        assertEquals("Server Certificates", sysChildren.get(5));
        assertEquals("Updates", sysChildren.get(6));
    }

    @Test
    public void testEditorSubcategoriesMatchDataGrip() {
        List<String> editorChildren = SettingsDialog.getChildCategoryNames("Editor");
        assertNotNull(editorChildren);
        assertEquals(16, editorChildren.size(), "Editor must have 16 direct children");

        assertEquals("General", editorChildren.get(0));
        assertEquals("Color Scheme", editorChildren.get(1));
        assertEquals("Code Style", editorChildren.get(2));
        assertEquals("Inspections", editorChildren.get(3));
        assertEquals("File and Code Templates", editorChildren.get(4));
        assertEquals("File Encodings", editorChildren.get(5));
        assertEquals("Live Templates", editorChildren.get(6));
        assertEquals("File Types", editorChildren.get(7));
        assertEquals("Inlay Hints", editorChildren.get(8));
        assertEquals("Duplicates", editorChildren.get(9));
        assertEquals("Intentions", editorChildren.get(10));
        assertEquals("Language Injections", editorChildren.get(11));
        assertEquals("Natural Languages", editorChildren.get(12));
        assertEquals("Reader Mode", editorChildren.get(13));
        assertEquals("TextMate Bundles", editorChildren.get(14));
        assertEquals("TODO", editorChildren.get(15));

        // Subtree under General
        List<String> genChildren = SettingsDialog.getChildCategoryNames("Editor / General");
        assertEquals(2, genChildren.size());
        assertEquals("Code Editing", genChildren.get(0));
        assertEquals("Font", genChildren.get(1));
    }

    @Test
    public void testCategoryDescriptionsMatchDataGripScreenshots() {
        String dbDesc = SettingsDialog.getCategoryDescription("Database");
        assertTrue(dbDesc.contains("Specify database console behavior, configure data views, and extraction options."));
        assertTrue(dbDesc.contains("Add custom parameter patterns in SQL queries and specify SQL dialects mapping for files."));

        String appDesc = SettingsDialog.getCategoryDescription("Appearance & Behavior");
        assertTrue(appDesc.contains("Customize IDE appearance and behavior: change themes and font size, tune the keymap,"));
        assertTrue(appDesc.contains("configure plugins and system settings, such as password policies, HTTP proxy, and updates."));

        String editorDesc = SettingsDialog.getCategoryDescription("Editor");
        assertTrue(editorDesc.contains("Personalize source code appearance by changing fonts, highlighting styles, indents, etc."));
        assertTrue(editorDesc.contains("Customize the Editor from line numbers, caret placement and tabs to source code inspections, setting up templates and file encodings."));
    }

    @Test
    public void testSearchFiltering() {
        List<String> fontResults = SettingsDialog.searchCategories("Font");
        assertFalse(fontResults.isEmpty());
        assertTrue(fontResults.stream().anyMatch(p -> p.contains("Font")));

        List<String> proxyResults = SettingsDialog.searchCategories("Proxy");
        assertFalse(proxyResults.isEmpty());
        assertTrue(proxyResults.stream().anyMatch(p -> p.contains("HTTP Proxy")));

        List<String> dialectResults = SettingsDialog.searchCategories("Dialect");
        assertFalse(dialectResults.isEmpty());
        assertTrue(dialectResults.stream().anyMatch(p -> p.contains("SQL Dialects")));

        List<String> emptyResults = SettingsDialog.searchCategories("");
        assertEquals(10, emptyResults.size());
    }

    @Test
    public void testTreeHierarchyGeneration() {
        TreeItem<String> root = SettingsDialog.buildCategoryHierarchy();
        assertNotNull(root);
        assertEquals(10, root.getChildren().size());

        TreeItem<String> dbItem = root.getChildren().stream()
                .filter(c -> "Database".equals(c.getValue()))
                .findFirst()
                .orElse(null);
        assertNotNull(dbItem);
        assertTrue(dbItem.isExpanded());
        assertEquals(9, dbItem.getChildren().size());
    }

    @Test
    public void testAppSettingsStoreConfiguration() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        assertEquals(30, settings.getQueryTimeoutSeconds());
        assertEquals(500, settings.getMaxResultRows());
        assertTrue(settings.isAutoCommit());
        assertEquals(100, settings.getPageSize());
        assertEquals(",", settings.getCsvDelimiter());
        assertEquals("\"", settings.getCsvQuoteChar());
        assertEquals("DataGrip Default", settings.getKeymapPreset());

        settings.setQueryTimeoutSeconds(60);
        settings.setMaxResultRows(1000);
        settings.setAutoCommit(false);
        settings.setPageSize(200);
        settings.setCsvDelimiter(";");
        settings.setCsvQuoteChar("'");
        settings.setKeymapPreset("macOS System");

        assertEquals(60, settings.getQueryTimeoutSeconds());
        assertEquals(1000, settings.getMaxResultRows());
        assertFalse(settings.isAutoCommit());
        assertEquals(200, settings.getPageSize());
        assertEquals(";", settings.getCsvDelimiter());
        assertEquals("'", settings.getCsvQuoteChar());
        assertEquals("macOS System", settings.getKeymapPreset());
    }

    @Test
    public void testVersionControlSubcategoriesMatchDataGrip() {
        List<String> vcsChildren = SettingsDialog.getChildCategoryNames("Version Control");
        assertNotNull(vcsChildren);
        assertEquals(8, vcsChildren.size(), "Version Control must have 8 direct children");

        assertEquals("Changelists", vcsChildren.get(0));
        assertEquals("Commit", vcsChildren.get(1));
        assertEquals("Confirmation", vcsChildren.get(2));
        assertEquals("Directory Mappings", vcsChildren.get(3));
        assertEquals("File Status Colors", vcsChildren.get(4));
        assertEquals("Issue Navigation", vcsChildren.get(5));
        assertEquals("Shelf", vcsChildren.get(6));
        assertEquals("Git", vcsChildren.get(7));
    }

    @Test
    public void testLanguagesSubcategoriesMatchDataGrip() {
        List<String> langChildren = SettingsDialog.getChildCategoryNames("Languages");
        assertNotNull(langChildren);
        assertEquals(5, langChildren.size(), "Languages must have 5 direct children");

        assertEquals("Markdown", langChildren.get(0));
        assertEquals("Mermaid", langChildren.get(1));
        assertEquals("Schemas and DTDs", langChildren.get(2));
        assertEquals("XSLT", langChildren.get(3));
        assertEquals("XSLT File Associations", langChildren.get(4));
    }

    @Test
    public void testToolsSubcategoriesMatchDataGrip() {
        List<String> toolsChildren = SettingsDialog.getChildCategoryNames("Tools");
        assertNotNull(toolsChildren);
        assertEquals(15, toolsChildren.size(), "Tools must have 15 direct children");

        assertEquals("Build Tools", toolsChildren.get(0));
        assertEquals("Actions on Save", toolsChildren.get(1));
        assertEquals("Coverage", toolsChildren.get(2));
        assertEquals("Debugger", toolsChildren.get(3));
        assertEquals("Diagrams", toolsChildren.get(4));
        assertEquals("Diff & Merge", toolsChildren.get(5));
        assertEquals("External Tools", toolsChildren.get(6));
        assertEquals("Features Suggester", toolsChildren.get(7));
        assertEquals("Features Trainer", toolsChildren.get(8));
        assertEquals("MCP Server", toolsChildren.get(9));
        assertEquals("Rsync", toolsChildren.get(10));
        assertEquals("SSH Configurations", toolsChildren.get(11));
        assertEquals("Terminal", toolsChildren.get(12));
        assertEquals("Web Browsers and Preview", toolsChildren.get(13));
        assertEquals("XPath Viewer", toolsChildren.get(14));
    }

    @Test
    public void testNewCategoryDescriptionsMatchScreenshots() {
        String vcsDesc = SettingsDialog.getCategoryDescription("Version Control");
        assertEquals("Configure the settings related to version control used in your project", vcsDesc);

        String langDesc = SettingsDialog.getCategoryDescription("Languages");
        assertEquals("Configure settings related to specific frameworks and technologies used in your project", langDesc);

        String toolsDesc = SettingsDialog.getCategoryDescription("Tools");
        assertEquals("Configure integration with third-party applications, specify the SSH Terminal connection settings, manage server certificates and tasks, configure diagrams layout, etc.", toolsDesc);
    }
}

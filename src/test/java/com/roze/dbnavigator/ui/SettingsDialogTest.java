package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.ColorSchemeAttribute;
import com.roze.dbnavigator.ui.action.KeyStrokeFormatter;
import com.roze.dbnavigator.ui.colorscheme.ColorSchemeModel;
import com.roze.dbnavigator.ui.colorscheme.ColorSchemeModel.ColorSchemeElement;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import javafx.application.Platform;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsDialogTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

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
        assertEquals(18, editorChildren.size(), "Editor must have 18 direct children");

        assertEquals("General", editorChildren.get(0));
        assertEquals("Code Editing", editorChildren.get(1));
        assertEquals("Font", editorChildren.get(2));
        assertEquals("Color Scheme", editorChildren.get(3));
        assertEquals("Code Style", editorChildren.get(4));
        assertEquals("Inspections", editorChildren.get(5));
        assertEquals("File and Code Templates", editorChildren.get(6));
        assertEquals("File Encodings", editorChildren.get(7));
        assertEquals("Live Templates", editorChildren.get(8));
        assertEquals("File Types", editorChildren.get(9));
        assertEquals("Inlay Hints", editorChildren.get(10));
        assertEquals("Duplicates", editorChildren.get(11));
        assertEquals("Intentions", editorChildren.get(12));
        assertEquals("Language Injections", editorChildren.get(13));
        assertEquals("Natural Languages", editorChildren.get(14));
        assertEquals("Reader Mode", editorChildren.get(15));
        assertEquals("TextMate Bundles", editorChildren.get(16));
        assertEquals("TODO", editorChildren.get(17));

        // Subtree under General
        List<String> genChildren = SettingsDialog.getChildCategoryNames("Editor / General");
        assertEquals(12, genChildren.size(), "General should have 12 children matching DataGrip");
        assertEquals("Auto Import", genChildren.get(0));
        assertEquals("Appearance", genChildren.get(1));
        assertEquals("Breadcrumbs", genChildren.get(2));
        assertEquals("Code Completion", genChildren.get(3));
        assertEquals("Code Folding", genChildren.get(4));
        assertEquals("Editor Tabs", genChildren.get(5));
        assertEquals("Gutter Icons", genChildren.get(6));
        assertEquals("Inline Completion", genChildren.get(7));
        assertEquals("Output Console", genChildren.get(8));
        assertEquals("Postfix Completion", genChildren.get(9));
        assertEquals("Smart Keys", genChildren.get(10));
        assertEquals("Sticky Lines", genChildren.get(11));

        // Subtree under Code Completion
        List<String> ccChildren = SettingsDialog.getChildCategoryNames("Editor / General / Code Completion");
        assertEquals(2, ccChildren.size());
        assertEquals("Popup", ccChildren.get(0));
        assertEquals("Inline", ccChildren.get(1));

        // Subtree under Smart Keys
        List<String> skChildren = SettingsDialog.getChildCategoryNames("Editor / General / Smart Keys");
        assertEquals(4, skChildren.size());
        assertEquals("HTML/CSS", skChildren.get(0));
        assertEquals("JSON", skChildren.get(1));
        assertEquals("Markdown", skChildren.get(2));
        assertEquals("SQL", skChildren.get(3));
    }

    @Test
    public void testCodeCompletionDescription() {
        String desc = SettingsDialog.getCategoryDescription("Editor / General / Code Completion");
        assertEquals("Provides code suggestions while typing, displayed either in a popup or inline in the editor", desc);
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
        assertEquals(AppSettingsStore.defaultKeymapPreset(), settings.getKeymapPreset());

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

    @Test
    public void testColorSchemeSubcategoriesMatchDataGrip() {
        List<String> csChildren = SettingsDialog.getChildCategoryNames("Editor / Color Scheme");
        assertNotNull(csChildren);
        assertEquals(26, csChildren.size(), "Color Scheme must have exactly 26 subcategories");

        assertEquals("General", csChildren.get(0));
        assertEquals("Language Defaults", csChildren.get(1));
        assertEquals("Color Scheme Font", csChildren.get(2));
        assertEquals("Console Font", csChildren.get(3));
        assertEquals("Code Review", csChildren.get(4));
        assertEquals("Console Colors", csChildren.get(5));
        assertEquals("Debugger", csChildren.get(6));
        assertEquals("Diff & Merge", csChildren.get(7));
        assertEquals("User-Defined File Types", csChildren.get(8));
        assertEquals("VCS", csChildren.get(9));
        assertEquals("Data Editor and Viewer", csChildren.get(10));
        assertEquals("Database", csChildren.get(11));
        assertEquals("Diagrams", csChildren.get(12));
        assertEquals("HTML", csChildren.get(13));
        assertEquals("JSON", csChildren.get(14));
        assertEquals("Markdown", csChildren.get(15));
        assertEquals("Mermaid", csChildren.get(16));
        assertEquals("RegExp", csChildren.get(17));
        assertEquals("SQL", csChildren.get(18));
        assertEquals("Table Diff", csChildren.get(19));
        assertEquals("XML", csChildren.get(20));
        assertEquals("XPath", csChildren.get(21));
        assertEquals("XSLT", csChildren.get(22));
        assertEquals("YAML", csChildren.get(23));
        assertEquals("By Scope", csChildren.get(24));
        assertEquals("Images", csChildren.get(25));

        String desc = SettingsDialog.getCategoryDescription("Editor / Color Scheme");
        assertEquals("Configure colors and the font for source code and console output:", desc);
    }

    @Test
    public void testCodeStyleSubcategoriesAndSqlDialectsMatchDataGrip() {
        List<String> codeStyleChildren = SettingsDialog.getChildCategoryNames("Editor / Code Style");
        assertNotNull(codeStyleChildren);
        assertEquals(8, codeStyleChildren.size(), "Code Style must have 8 file type / language subcategories");

        assertEquals("SQL", codeStyleChildren.get(0));
        assertEquals("HTML", codeStyleChildren.get(1));
        assertEquals("JSON", codeStyleChildren.get(2));
        assertEquals("Markdown", codeStyleChildren.get(3));
        assertEquals("Mermaid", codeStyleChildren.get(4));
        assertEquals("XML", codeStyleChildren.get(5));
        assertEquals("YAML", codeStyleChildren.get(6));
        assertEquals("Other File Types", codeStyleChildren.get(7));

        String csDesc = SettingsDialog.getCategoryDescription("Editor / Code Style");
        assertEquals("Configure code formatting rules, indentation, keyword casing, and line wrapping for SQL.", csDesc);

        // 12 SQL Dialects under Code Style > SQL
        List<String> sqlDialects = SettingsDialog.getChildCategoryNames("Editor / Code Style / SQL");
        assertNotNull(sqlDialects);
        assertEquals(12, sqlDialects.size(), "Code Style > SQL must have 12 dialect options");

        assertEquals("General", sqlDialects.get(0));
        assertEquals("SQL:2016, Generic", sqlDialects.get(1));
        assertEquals("Apache Derby", sqlDialects.get(2));
        assertEquals("Db2", sqlDialects.get(3));
        assertEquals("H2", sqlDialects.get(4));
        assertEquals("HSQLDB", sqlDialects.get(5));
        assertEquals("MS SQL Server, MS Azure", sqlDialects.get(6));
        assertEquals("MySQL, MariaDB", sqlDialects.get(7));
        assertEquals("Oracle", sqlDialects.get(8));
        assertEquals("PostgreSQL, Greenplum, Redshift", sqlDialects.get(9));
        assertEquals("SQLite", sqlDialects.get(10));
        assertEquals("Sybase ASE", sqlDialects.get(11));

        String sqlDesc = SettingsDialog.getCategoryDescription("Editor / Code Style / SQL");
        assertEquals("Set of code styles based on SQL.", sqlDesc);
    }

    @Test
    public void testNaturalLanguagesSubcategoriesMatchDataGrip() {
        List<String> nlChildren = SettingsDialog.getChildCategoryNames("Editor / Natural Languages");
        assertNotNull(nlChildren);
        assertEquals(2, nlChildren.size(), "Natural Languages must have 2 subcategories");

        assertEquals("Grammar and Style", nlChildren.get(0));
        assertEquals("Spelling", nlChildren.get(1));

        String nlDesc = SettingsDialog.getCategoryDescription("Editor / Natural Languages");
        assertEquals("Configure proofreading and natural language spelling checks.", nlDesc);
    }

    @Test
    public void testSchemasAndDtdsSubcategoriesMatchDataGrip() {
        List<String> schemaChildren = SettingsDialog.getChildCategoryNames("Languages / Schemas and DTDs");
        assertNotNull(schemaChildren);
        assertEquals(4, schemaChildren.size(), "Schemas and DTDs must have 4 subcategories");

        assertEquals("Default XML Schemas", schemaChildren.get(0));
        assertEquals("JSON Schema Mappings", schemaChildren.get(1));
        assertEquals("Remote JSON Schemas", schemaChildren.get(2));
        assertEquals("XML Catalog", schemaChildren.get(3));
    }

    @Test
    public void testDebuggerSubcategoriesMatchDataGrip() {
        List<String> debuggerChildren = SettingsDialog.getChildCategoryNames("Tools / Debugger");
        assertNotNull(debuggerChildren);
        assertEquals(2, debuggerChildren.size(), "Debugger must have 2 subcategories");

        assertEquals("Data Views", debuggerChildren.get(0));
        assertEquals("Stepping", debuggerChildren.get(1));
    }

    @Test
    public void testDiffMergeSubcategoriesMatchDataGrip() {
        List<String> diffChildren = SettingsDialog.getChildCategoryNames("Tools / Diff & Merge");
        assertNotNull(diffChildren);
        assertEquals(1, diffChildren.size(), "Diff & Merge must have 1 subcategory");

        assertEquals("External Diff Tools", diffChildren.get(0));
    }

    @Test
    public void testMcpServerSubcategoriesMatchDataGrip() {
        List<String> mcpChildren = SettingsDialog.getChildCategoryNames("Tools / MCP Server");
        assertNotNull(mcpChildren);
        assertEquals(1, mcpChildren.size(), "MCP Server must have 1 subcategory");

        assertEquals("Exposed Tools", mcpChildren.get(0));
    }

    @Test
    public void testDataEditorAndViewerSettings() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // Check defaults matching DataGrip screenshots
        assertTrue(settings.isLimitPageSize());
        assertEquals(100, settings.getPageSize());
        assertEquals(2000, settings.getResultSetPrefetchSize());
        assertEquals(10, settings.getFilterHistorySize());
        assertEquals(204800, settings.getMaxBytesLoadedPerValue());
        assertTrue(settings.isShowFirstDataRowsInPreview());
        assertEquals(10, settings.getPreviewDataRows());

        assertFalse(settings.isEnablePagingInEditorResults());
        assertEquals("Grid bottom (floating)", settings.getGridPaginationPosition());
        assertTrue(settings.isShowQuickActionsToolbar());
        assertTrue(settings.isEnableQuickActionsCustomization());

        assertFalse(settings.isUseCustomFont());
        assertEquals("JetBrains Mono", settings.getCustomFontFamily());
        assertEquals(13.0, settings.getCustomFontSize(), 0.001);
        assertEquals(1.2, settings.getCustomLineHeight(), 0.001);
        assertFalse(settings.isAlternateRowColors());
        assertEquals("Text", settings.getShowBooleanValuesAs());
        assertEquals("Never", settings.getAutomaticallyTransposeTables());
        assertTrue(settings.isDetectBinaryAsText());
        assertTrue(settings.isDetectBinaryAsUuid());
        assertTrue(settings.isEnableLocalFilterByDefault());
        assertTrue(settings.isEnableImmediateCompletionInGridTextCells());

        assertEquals(".", settings.getDecimalSeparator());
        assertFalse(settings.isEnableGroupingSeparator());
        assertEquals("", settings.getGroupingSeparator());
        assertEquals("Infinity", settings.getInfinityText());
        assertEquals("NaN", settings.getNanText());
        assertFalse(settings.isEnableNumberPattern());

        assertFalse(settings.isEnableDatetimeTimestamp());
        assertEquals("yyyy-MM-dd HH:mm:ss", settings.getDatetimeTimestampPattern());
        assertFalse(settings.isEnableDatetimeTimestampWithZone());
        assertEquals("yyyy-MM-dd HH:mm:ss Z", settings.getDatetimeTimestampWithZonePattern());
        assertFalse(settings.isEnableTime());
        assertEquals("HH:mm:ss", settings.getTimePattern());
        assertFalse(settings.isEnableTimeWithZone());
        assertEquals("HH:mm:ss Z", settings.getTimeWithZonePattern());
        assertFalse(settings.isEnableDate());
        assertEquals("yyyy-MM-dd", settings.getDatePattern());

        assertTrue(settings.isSortViaOrderBy());
        assertFalse(settings.isSortTablesByNumericPk());
        assertEquals("Ascending", settings.getSortTablesByNumericPkDirection());
        assertEquals("⌥Click", settings.getAddColumnsToSorting());

        assertFalse(settings.isSubmitChangesImmediately());
        assertTrue(settings.isEnableEditingForQueriesWithJoin());
        assertTrue(settings.isShowDmlPreviewForQueriesWithJoin());

        assertFalse(settings.isAllowOpenSecureLinks());
        assertFalse(settings.isAllowOpenStandardLinks());
        assertFalse(settings.isAllowOpenLocalFileLinks());
        assertFalse(settings.isAssumeHttpIfNoProtocol());

        // Test mutating and setters
        settings.setLimitPageSize(false);
        settings.setPageSize(500);
        settings.setResultSetPrefetchSize(5000);
        settings.setFilterHistorySize(25);
        settings.setMaxBytesLoadedPerValue(1024000);
        settings.setShowFirstDataRowsInPreview(false);
        settings.setPreviewDataRows(20);

        settings.setEnablePagingInEditorResults(true);
        settings.setGridPaginationPosition("Bottom");
        settings.setShowQuickActionsToolbar(false);
        settings.setEnableQuickActionsCustomization(false);

        settings.setUseCustomFont(true);
        settings.setCustomFontFamily("Consolas");
        settings.setCustomFontSize(14.0);
        settings.setCustomLineHeight(1.4);
        settings.setAlternateRowColors(true);
        settings.setShowBooleanValuesAs("Checkboxes");
        settings.setAutomaticallyTransposeTables("Always");
        settings.setDetectBinaryAsText(false);
        settings.setDetectBinaryAsUuid(false);
        settings.setEnableLocalFilterByDefault(false);
        settings.setEnableImmediateCompletionInGridTextCells(false);

        settings.setDecimalSeparator(",");
        settings.setEnableGroupingSeparator(true);
        settings.setGroupingSeparator(" ");
        settings.setInfinityText("INF");
        settings.setNanText("NONE");
        settings.setEnableNumberPattern(true);
        settings.setNumberPattern("#,##0.00");

        settings.setEnableDatetimeTimestamp(true);
        settings.setDatetimeTimestampPattern("dd/MM/yyyy HH:mm");
        settings.setEnableDatetimeTimestampWithZone(true);
        settings.setDatetimeTimestampWithZonePattern("dd/MM/yyyy HH:mm z");
        settings.setEnableTime(true);
        settings.setTimePattern("HH:mm");
        settings.setEnableTimeWithZone(true);
        settings.setTimeWithZonePattern("HH:mm z");
        settings.setEnableDate(true);
        settings.setDatePattern("dd/MM/yyyy");

        settings.setSortViaOrderBy(false);
        settings.setSortTablesByNumericPk(true);
        settings.setSortTablesByNumericPkDirection("Descending");
        settings.setAddColumnsToSorting("Click");

        settings.setSubmitChangesImmediately(true);
        settings.setEnableEditingForQueriesWithJoin(false);
        settings.setShowDmlPreviewForQueriesWithJoin(false);

        settings.setAllowOpenSecureLinks(true);
        settings.setAllowOpenStandardLinks(true);
        settings.setAllowOpenLocalFileLinks(true);
        settings.setAssumeHttpIfNoProtocol(true);

        // JSON serialization round-trip verification
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        assertFalse(loaded.isLimitPageSize());
        assertEquals(500, loaded.getPageSize());
        assertEquals(5000, loaded.getResultSetPrefetchSize());
        assertEquals(25, loaded.getFilterHistorySize());
        assertEquals(1024000, loaded.getMaxBytesLoadedPerValue());
        assertFalse(loaded.isShowFirstDataRowsInPreview());
        assertEquals(20, loaded.getPreviewDataRows());

        assertTrue(loaded.isEnablePagingInEditorResults());
        assertEquals("Bottom", loaded.getGridPaginationPosition());
        assertFalse(loaded.isShowQuickActionsToolbar());
        assertFalse(loaded.isEnableQuickActionsCustomization());

        assertTrue(loaded.isUseCustomFont());
        assertEquals("Consolas", loaded.getCustomFontFamily());
        assertEquals(14.0, loaded.getCustomFontSize(), 0.001);
        assertEquals(1.4, loaded.getCustomLineHeight(), 0.001);
        assertTrue(loaded.isAlternateRowColors());
        assertEquals("Checkboxes", loaded.getShowBooleanValuesAs());
        assertEquals("Always", loaded.getAutomaticallyTransposeTables());
        assertFalse(loaded.isDetectBinaryAsText());
        assertFalse(loaded.isDetectBinaryAsUuid());
        assertFalse(loaded.isEnableLocalFilterByDefault());
        assertFalse(loaded.isEnableImmediateCompletionInGridTextCells());

        assertEquals(",", loaded.getDecimalSeparator());
        assertTrue(loaded.isEnableGroupingSeparator());
        assertEquals(" ", loaded.getGroupingSeparator());
        assertEquals("INF", loaded.getInfinityText());
        assertEquals("NONE", loaded.getNanText());
        assertTrue(loaded.isEnableNumberPattern());
        assertEquals("#,##0.00", loaded.getNumberPattern());

        assertTrue(loaded.isEnableDatetimeTimestamp());
        assertEquals("dd/MM/yyyy HH:mm", loaded.getDatetimeTimestampPattern());
        assertTrue(loaded.isEnableDatetimeTimestampWithZone());
        assertEquals("dd/MM/yyyy HH:mm z", loaded.getDatetimeTimestampWithZonePattern());
        assertTrue(loaded.isEnableTime());
        assertEquals("HH:mm", loaded.getTimePattern());
        assertTrue(loaded.isEnableTimeWithZone());
        assertEquals("HH:mm z", loaded.getTimeWithZonePattern());
        assertTrue(loaded.isEnableDate());
        assertEquals("dd/MM/yyyy", loaded.getDatePattern());

        assertFalse(loaded.isSortViaOrderBy());
        assertTrue(loaded.isSortTablesByNumericPk());
        assertEquals("Descending", loaded.getSortTablesByNumericPkDirection());
        assertEquals("Click", loaded.getAddColumnsToSorting());

        assertTrue(loaded.isSubmitChangesImmediately());
        assertFalse(loaded.isEnableEditingForQueriesWithJoin());
        assertFalse(loaded.isShowDmlPreviewForQueriesWithJoin());

        assertTrue(loaded.isAllowOpenSecureLinks());
        assertTrue(loaded.isAllowOpenStandardLinks());
        assertTrue(loaded.isAllowOpenLocalFileLinks());
        assertTrue(loaded.isAssumeHttpIfNoProtocol());
    }

    @Test
    public void testProjectLevelCategoriesContainExpected() {
        Set<String> projectCategories = SettingsDialog.PROJECT_LEVEL_CATEGORIES;
        assertNotNull(projectCategories);
        assertTrue(projectCategories.contains("Database Explorer"));
        assertTrue(projectCategories.contains("SQL Dialects"));
        assertTrue(projectCategories.contains("SQL Resolution Scopes"));
        assertTrue(projectCategories.contains("Version Control"));
        assertTrue(projectCategories.contains("Build Tools"));
        assertTrue(projectCategories.contains("Actions on Save"));
        assertTrue(projectCategories.contains("Coverage"));
        assertTrue(projectCategories.contains("SSH Configurations"));
        assertTrue(projectCategories.contains("Terminal"));
    }

    @Test
    public void testResolveEditorTabTitleTokens() {
        // Fallbacks
        assertEquals("query.sql", SettingsDialog.resolveEditorTabTitle(null, "query.sql", "PostgreSQL", "public", "postgres", "public"));
        assertEquals("console", SettingsDialog.resolveEditorTabTitle("", null, "PostgreSQL", "public", "postgres", "public"));

        // Default template: $NAME$ [$DATASOURCE$]
        assertEquals("console_1 [PostgreSQL]", SettingsDialog.resolveEditorTabTitle(
                "$NAME$ [$DATASOURCE$]", "console_1", "PostgreSQL", "public", "postgres", "public"));

        // Full placeholders
        String template = "$SCHEMA$.$NAME$ ($DATABASE$ / $DATASOURCE$ / $SEARCH_PATH$)";
        String resolved = SettingsDialog.resolveEditorTabTitle(
                template, "test.sql", "MySQL_Prod", "myschema", "mydb", "myschema");
        assertEquals("myschema.test.sql (mydb / MySQL_Prod / myschema)", resolved);
    }

    @Test
    public void testDatabaseExplorerSettingsDefaultsAndSerialization() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // Defaults from DataGrip screenshot
        assertTrue(settings.isRememberFilterState());
        assertTrue(settings.isShowDatabaseColors());
        assertTrue(settings.isColorDatabaseExplorer());
        assertTrue(settings.isColorEditorTabHeaders());
        assertFalse(settings.isColorEditorBackgrounds());
        assertTrue(settings.isColorEditorToolbars());

        // Mutate
        settings.setRememberFilterState(false);
        settings.setShowDatabaseColors(false);
        settings.setColorDatabaseExplorer(false);
        settings.setColorEditorTabHeaders(false);
        settings.setColorEditorBackgrounds(true);
        settings.setColorEditorToolbars(false);

        // Serialization round trip
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        assertFalse(loaded.isRememberFilterState());
        assertFalse(loaded.isShowDatabaseColors());
        assertFalse(loaded.isColorDatabaseExplorer());
        assertFalse(loaded.isColorEditorTabHeaders());
        assertTrue(loaded.isColorEditorBackgrounds());
        assertFalse(loaded.isColorEditorToolbars());
    }

    @Test
    public void testAiToolsSettingsDefaultsAndSerialization() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // Defaults from DataGrip screenshot (all disabled by default)
        assertFalse(settings.isAiReadDatabaseSchemas());
        assertFalse(settings.isAiModifyDatabaseSchemas());
        assertFalse(settings.isAiReadDatabaseData());
        assertFalse(settings.isAiModifyDatabaseData());

        // Mutate
        settings.setAiReadDatabaseSchemas(true);
        settings.setAiModifyDatabaseSchemas(true);
        settings.setAiReadDatabaseData(true);
        settings.setAiModifyDatabaseData(true);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        assertTrue(loaded.isAiReadDatabaseSchemas());
        assertTrue(loaded.isAiModifyDatabaseSchemas());
        assertTrue(loaded.isAiReadDatabaseData());
        assertTrue(loaded.isAiModifyDatabaseData());
    }

    @Test
    public void testQueryFilesAndConsolesSettingsDefaultsAndSerialization() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // Defaults from DataGrip screenshot
        assertTrue(settings.isShowDataSourceNameInFileTree());
        assertTrue(settings.isUseAttachedSearchPathColorInFileTree());
        assertTrue(settings.isUseAttachedDataSourceIconForQueryFiles());
        assertEquals("console", settings.getDefaultConsoleFileName());
        assertEquals("$NAME$ [$DATASOURCE$]", settings.getEditorTabTitleTemplate());
        assertTrue(settings.isUseTemplateForQueryFiles());

        // Mutate
        settings.setShowDataSourceNameInFileTree(false);
        settings.setUseAttachedSearchPathColorInFileTree(false);
        settings.setUseAttachedDataSourceIconForQueryFiles(false);
        settings.setDefaultConsoleFileName("scratch_query");
        settings.setEditorTabTitleTemplate("$SCHEMA$.$NAME$");
        settings.setUseTemplateForQueryFiles(false);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        assertFalse(loaded.isShowDataSourceNameInFileTree());
        assertFalse(loaded.isUseAttachedSearchPathColorInFileTree());
        assertFalse(loaded.isUseAttachedDataSourceIconForQueryFiles());
        assertEquals("scratch_query", loaded.getDefaultConsoleFileName());
        assertEquals("$SCHEMA$.$NAME$", loaded.getEditorTabTitleTemplate());
        assertFalse(loaded.isUseTemplateForQueryFiles());
    }

    @Test
    public void testSqlDialectsSettingsDefaultsAndSerialization() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        assertEquals("<None>", settings.getGlobalSqlDialect());
        assertEquals("<None>", settings.getProjectSqlDialect());
        assertNotNull(settings.getSqlDialectMappings());
        assertTrue(settings.getSqlDialectMappings().isEmpty());

        List<String> dialects = AppSettingsStore.Settings.defaultDialectList();
        assertTrue(dialects.size() >= 30, "Should have at least 30 default dialects");
        assertTrue(dialects.contains("<None>"));
        assertTrue(dialects.contains("PostgreSQL"));
        assertTrue(dialects.contains("MySQL"));
        assertTrue(dialects.contains("Oracle"));
        assertTrue(dialects.contains("Microsoft SQL Server"));
        assertTrue(dialects.contains("Generic SQL"));
        assertTrue(dialects.contains("Snowflake"));
        assertTrue(dialects.contains("ClickHouse"));
        assertTrue(dialects.contains("Google BigQuery"));

        AppSettingsStore.SqlDialectMappingConfig m = new AppSettingsStore.SqlDialectMappingConfig("/path/file.sql", "PostgreSQL");
        assertEquals("/path/file.sql", m.getPath());
        assertEquals("PostgreSQL", m.getDialect());
        AppSettingsStore.SqlDialectMappingConfig copy = m.copy();
        assertEquals(m.getPath(), copy.getPath());
        assertEquals(m.getDialect(), copy.getDialect());
        assertTrue(m.toString().contains("/path/file.sql"));

        // Mutate
        settings.setGlobalSqlDialect("PostgreSQL");
        settings.setProjectSqlDialect("MySQL");
        settings.getSqlDialectMappings().add(new AppSettingsStore.SqlDialectMappingConfig("/proj/src/query.sql", "Oracle"));

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        assertEquals("PostgreSQL", loaded.getGlobalSqlDialect());
        assertEquals("MySQL", loaded.getProjectSqlDialect());
        assertEquals(1, loaded.getSqlDialectMappings().size());
        assertEquals("/proj/src/query.sql", loaded.getSqlDialectMappings().get(0).getPath());
        assertEquals("Oracle", loaded.getSqlDialectMappings().get(0).getDialect());
    }

    @Test
    public void testSqlResolutionScopesSettingsDefaultsAndSerialization() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        assertEquals("<Default> (<Everything>)", settings.getProjectResolutionScope());
        assertNotNull(settings.getSqlResolutionScopeMappings());
        assertTrue(settings.getSqlResolutionScopeMappings().isEmpty());

        AppSettingsStore.SqlResolutionScopeMappingConfig s = new AppSettingsStore.SqlResolutionScopeMappingConfig("/src/dir", "postgres@localhost/erpdb");
        assertEquals("/src/dir", s.getPath());
        assertEquals("postgres@localhost/erpdb", s.getScope());
        AppSettingsStore.SqlResolutionScopeMappingConfig copy = s.copy();
        assertEquals(s.getPath(), copy.getPath());
        assertEquals(s.getScope(), copy.getScope());
        assertTrue(s.toString().contains("/src/dir"));

        // Mutate
        settings.setProjectResolutionScope("postgres@localhost/nexadb");
        settings.getSqlResolutionScopeMappings().add(new AppSettingsStore.SqlResolutionScopeMappingConfig("/proj/queries", "postgres@localhost/postgres"));

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        assertEquals("postgres@localhost/nexadb", loaded.getProjectResolutionScope());
        assertEquals(1, loaded.getSqlResolutionScopeMappings().size());
        assertEquals("/proj/queries", loaded.getSqlResolutionScopeMappings().get(0).getPath());
        assertEquals("postgres@localhost/postgres", loaded.getSqlResolutionScopeMappings().get(0).getScope());
    }

    @Test
    public void testDatabaseOtherSettingsDefaultsAndSerialization() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // Check defaults matching DataGrip screenshot
        assertTrue(settings.isConfirmCancellationForModifySchemaDialogs());
        assertTrue(settings.isShowPreviewOfValidScriptWhenUpdatingSource());
        assertTrue(settings.isSuggestDumpingDdlForNewMappings());
        assertEquals("Append to existing console", settings.getGenerateContextTemplates());
        assertNotNull(settings.getVirtualForeignKeys());
        assertEquals(1, settings.getVirtualForeignKeys().size());
        assertEquals("(.*)_(?i)id", settings.getVirtualForeignKeys().get(0).getColumnPattern());
        assertEquals("$1\\.(?i)id", settings.getVirtualForeignKeys().get(0).getTargetColumnPattern());
        assertEquals("Playground", settings.getDefaultResolveModeForConsoles());
        assertEquals("", settings.getStatementDelimiter());

        AppSettingsStore.VirtualForeignKeyRule rule = new AppSettingsStore.VirtualForeignKeyRule("usr_(.*)", "$1\\.id");
        assertEquals("usr_(.*)", rule.getColumnPattern());
        assertEquals("$1\\.id", rule.getTargetColumnPattern());
        AppSettingsStore.VirtualForeignKeyRule copy = rule.copy();
        assertEquals(rule.getColumnPattern(), copy.getColumnPattern());
        assertEquals(rule.getTargetColumnPattern(), copy.getTargetColumnPattern());
        assertTrue(rule.toString().contains("usr_(.*)"));

        // Mutate
        settings.setConfirmCancellationForModifySchemaDialogs(false);
        settings.setShowPreviewOfValidScriptWhenUpdatingSource(false);
        settings.setSuggestDumpingDdlForNewMappings(false);
        settings.setGenerateContextTemplates("Create new console");
        settings.getVirtualForeignKeys().add(new AppSettingsStore.VirtualForeignKeyRule("fk_(.*)", "$1\\.guid"));
        settings.setDefaultResolveModeForConsoles("Auto-detect");
        settings.setStatementDelimiter(";;");

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        assertFalse(loaded.isConfirmCancellationForModifySchemaDialogs());
        assertFalse(loaded.isShowPreviewOfValidScriptWhenUpdatingSource());
        assertFalse(loaded.isSuggestDumpingDdlForNewMappings());
        assertEquals("Create new console", loaded.getGenerateContextTemplates());
        assertEquals(2, loaded.getVirtualForeignKeys().size());
        assertEquals("fk_(.*)", loaded.getVirtualForeignKeys().get(1).getColumnPattern());
        assertEquals("$1\\.guid", loaded.getVirtualForeignKeys().get(1).getTargetColumnPattern());
        assertEquals("Auto-detect", loaded.getDefaultResolveModeForConsoles());
        assertEquals(";;", loaded.getStatementDelimiter());
    }

    @Test
    public void testAppearanceSettingsDefaultsAndSerialization() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // Check defaults matching DataGrip screenshots
        assertEquals("Islands Dark", settings.getUiTheme());
        assertFalse(settings.isSyncThemeWithOs());
        assertEquals("Dark Theme default", settings.getEditorColorScheme());
        assertFalse(settings.isDifferentToolWindowBackground());

        assertEquals("100%", settings.getIdeZoom());
        assertFalse(settings.isUseCustomIdeFont());
        assertEquals("Inter", settings.getCustomIdeFontFamily());
        assertEquals(13, settings.getCustomIdeFontSize());
        assertFalse(settings.isSupportScreenReaders());
        assertFalse(settings.isUseContrastScrollbars());
        assertFalse(settings.isAdjustColorsForVisionDeficiency());

        assertFalse(settings.isCompactMode());
        assertFalse(settings.isAlwaysShowFullPathInWindowHeader());
        assertTrue(settings.isUseProjectColorsInMainToolbar());
        assertTrue(settings.isKeepPopupsOpenForToggleItems());
        assertFalse(settings.isDragAndDropWithAltPressedOnly());
        assertTrue(settings.isSmoothScrolling());
        assertTrue(settings.isEnableMnemonicsInControls());
        assertTrue(settings.isEnableMnemonicsInMenu());
        assertTrue(settings.isDisplayIconsInMenuItems());
        assertEquals("Hide under Hamburger Button", settings.getMainMenuPresentation());

        assertEquals("", settings.getBackgroundImagePath());
        assertEquals(15, settings.getBackgroundImageOpacity());
        assertEquals("Fill", settings.getBackgroundImagePlacement());
        assertFalse(settings.isBackgroundImageThisProjectOnly());
        assertEquals("Editor and Tools", settings.getBackgroundImageTarget());

        assertFalse(settings.isShowIndentGuides());
        assertFalse(settings.isUseSmallerIndents());

        assertFalse(settings.isShowToolWindowBars());
        assertFalse(settings.isShowToolWindowNames());
        assertFalse(settings.isSideBySideLayoutOnLeft());
        assertFalse(settings.isSideBySideLayoutOnRight());
        assertFalse(settings.isWidescreenToolWindowLayout());
        assertFalse(settings.isRememberSizeForEachToolWindow());

        assertEquals("175%", settings.getPresentationModeZoom());
        assertEquals("Subpixel", settings.getIdeAntialiasing());
        assertEquals("Subpixel", settings.getEditorAntialiasing());

        // Validate helper lists
        List<String> themes = AppSettingsStore.Settings.defaultUiThemes();
        assertTrue(themes.contains("Islands Dark"));
        assertTrue(themes.contains("Islands Light"));
        assertTrue(themes.contains("High Contrast"));
        assertTrue(themes.contains("Darcula"));

        List<String> schemes = AppSettingsStore.Settings.defaultEditorColorSchemes();
        assertTrue(schemes.contains("Dark Theme default"));
        assertTrue(schemes.contains("Classic Light"));

        List<String> menus = AppSettingsStore.Settings.defaultMainMenuOptions();
        assertTrue(menus.contains("Hide under Hamburger Button"));
        assertTrue(menus.contains("Merge with Main Toolbar"));
        assertTrue(menus.contains("Show above Main Toolbar"));

        // Mutate
        settings.setUiTheme("Islands Light");
        settings.setSyncThemeWithOs(true);
        settings.setEditorColorScheme("Light");
        settings.setDifferentToolWindowBackground(true);

        settings.setIdeZoom("125%");
        settings.setUseCustomIdeFont(true);
        settings.setCustomIdeFontFamily("JetBrains Sans");
        settings.setCustomIdeFontSize(14);
        settings.setSupportScreenReaders(true);
        settings.setUseContrastScrollbars(true);
        settings.setAdjustColorsForVisionDeficiency(true);

        settings.setCompactMode(true);
        settings.setAlwaysShowFullPathInWindowHeader(true);
        settings.setUseProjectColorsInMainToolbar(false);
        settings.setKeepPopupsOpenForToggleItems(false);
        settings.setDragAndDropWithAltPressedOnly(true);
        settings.setSmoothScrolling(false);
        settings.setEnableMnemonicsInControls(false);
        settings.setEnableMnemonicsInMenu(false);
        settings.setDisplayIconsInMenuItems(false);
        settings.setMainMenuPresentation("Merge with Main Toolbar");

        settings.setBackgroundImagePath("/path/to/bg.png");
        settings.setBackgroundImageOpacity(30);
        settings.setBackgroundImagePlacement("Center");
        settings.setBackgroundImageThisProjectOnly(true);
        settings.setBackgroundImageTarget("Empty Frame");

        settings.setShowIndentGuides(true);
        settings.setUseSmallerIndents(true);

        settings.setShowToolWindowBars(true);
        settings.setShowToolWindowNames(true);
        settings.setSideBySideLayoutOnLeft(true);
        settings.setSideBySideLayoutOnRight(true);
        settings.setWidescreenToolWindowLayout(true);
        settings.setRememberSizeForEachToolWindow(true);

        settings.setPresentationModeZoom("200%");
        settings.setIdeAntialiasing("Greyscale");
        settings.setEditorAntialiasing("No antialiasing");

        // Jackson Round-trip
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        assertEquals("Islands Light", loaded.getUiTheme());
        assertTrue(loaded.isSyncThemeWithOs());
        assertEquals("Light", loaded.getEditorColorScheme());
        assertTrue(loaded.isDifferentToolWindowBackground());

        assertEquals("125%", loaded.getIdeZoom());
        assertTrue(loaded.isUseCustomIdeFont());
        assertEquals("JetBrains Sans", loaded.getCustomIdeFontFamily());
        assertEquals(14, loaded.getCustomIdeFontSize());
        assertTrue(loaded.isSupportScreenReaders());
        assertTrue(loaded.isUseContrastScrollbars());
        assertTrue(loaded.isAdjustColorsForVisionDeficiency());

        assertTrue(loaded.isCompactMode());
        assertTrue(loaded.isAlwaysShowFullPathInWindowHeader());
        assertFalse(loaded.isUseProjectColorsInMainToolbar());
        assertFalse(loaded.isKeepPopupsOpenForToggleItems());
        assertTrue(loaded.isDragAndDropWithAltPressedOnly());
        assertFalse(loaded.isSmoothScrolling());
        assertFalse(loaded.isEnableMnemonicsInControls());
        assertFalse(loaded.isEnableMnemonicsInMenu());
        assertFalse(loaded.isDisplayIconsInMenuItems());
        assertEquals("Merge with Main Toolbar", loaded.getMainMenuPresentation());

        assertEquals("/path/to/bg.png", loaded.getBackgroundImagePath());
        assertEquals(30, loaded.getBackgroundImageOpacity());
        assertEquals("Center", loaded.getBackgroundImagePlacement());
        assertTrue(loaded.isBackgroundImageThisProjectOnly());
        assertEquals("Empty Frame", loaded.getBackgroundImageTarget());

        assertTrue(loaded.isShowIndentGuides());
        assertTrue(loaded.isUseSmallerIndents());

        assertTrue(loaded.isShowToolWindowBars());
        assertTrue(loaded.isShowToolWindowNames());
        assertTrue(loaded.isSideBySideLayoutOnLeft());
        assertTrue(loaded.isSideBySideLayoutOnRight());
        assertTrue(loaded.isWidescreenToolWindowLayout());
        assertTrue(loaded.isRememberSizeForEachToolWindow());

        assertEquals("200%", loaded.getPresentationModeZoom());
        assertEquals("Greyscale", loaded.getIdeAntialiasing());
        assertEquals("No antialiasing", loaded.getEditorAntialiasing());
    }

    @Test
    public void testEditorGeneralAutoImportBreadcrumbsAppearanceSettings() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Auto Import Defaults (Image 1)
        assertTrue(settings.isShowXmlAutoImportTooltip(), "Auto import XML tooltip should be enabled by default");

        // 2. Breadcrumbs Defaults (Image 2)
        assertTrue(settings.isShowBreadcrumbs(), "Breadcrumbs should be enabled by default");
        assertEquals("Bottom", settings.getBreadcrumbsPlacement(), "Breadcrumbs placement default should be Bottom");
        assertFalse(settings.isBreadcrumbsHtml());
        assertFalse(settings.isBreadcrumbsMarkdown());
        assertFalse(settings.isBreadcrumbsXhtml());
        assertFalse(settings.isBreadcrumbsJson());
        assertFalse(settings.isBreadcrumbsSql());
        assertFalse(settings.isBreadcrumbsXml());

        // 3. Editor Appearance Defaults (Images 3 & 4)
        assertTrue(settings.isCaretBlinking());
        assertEquals(500, settings.getCaretBlinkingMs());
        assertFalse(settings.isUseBlockCaret());
        assertFalse(settings.isUseFullLineHeightCaret());
        assertTrue(settings.isHighlightOccurrences());
        assertTrue(settings.isShowHardWrapAndVisualGuides());
        assertTrue(settings.isShowLineNumbers());
        assertEquals("Absolute", settings.getLineNumbersMode());
        assertFalse(settings.isShowLinesBetweenStatements());
        assertFalse(settings.isShowWhitespaces());
        assertTrue(settings.isShowWhitespacesLeading());
        assertTrue(settings.isShowWhitespacesInner());
        assertTrue(settings.isShowWhitespacesTrailing());
        assertTrue(settings.isShowWhitespacesSelection());
        assertTrue(settings.isShowEditorIndentGuides());
        assertTrue(settings.isShowIntentionBulb());
        assertTrue(settings.isShowPreviewForIntentionActions());
        assertFalse(settings.isRenderDocComments());
        assertTrue(settings.isShowCodeLensOnScrollbarHover());
        assertFalse(settings.isUseEditorFontForInlayHints());
        assertTrue(settings.isEnableTagTreeHighlighting());
        assertEquals(6, settings.getTagTreeLevelsToHighlight());
        assertEquals(0.1, settings.getTagTreeOpacity(), 0.001);

        // Mutate and set new values
        settings.setShowXmlAutoImportTooltip(false);

        settings.setShowBreadcrumbs(false);
        settings.setBreadcrumbsPlacement("Top");
        settings.setBreadcrumbsHtml(true);
        settings.setBreadcrumbsMarkdown(true);
        settings.setBreadcrumbsXhtml(true);
        settings.setBreadcrumbsJson(true);
        settings.setBreadcrumbsSql(true);
        settings.setBreadcrumbsXml(true);

        settings.setCaretBlinking(false);
        settings.setCaretBlinkingMs(750);
        settings.setUseBlockCaret(true);
        settings.setUseFullLineHeightCaret(true);
        settings.setHighlightOccurrences(false);
        settings.setShowHardWrapAndVisualGuides(false);
        settings.setShowLineNumbers(false);
        settings.setLineNumbersMode("Relative");
        settings.setShowLinesBetweenStatements(true);
        settings.setShowWhitespaces(true);
        settings.setShowWhitespacesLeading(false);
        settings.setShowWhitespacesInner(false);
        settings.setShowWhitespacesTrailing(false);
        settings.setShowWhitespacesSelection(false);
        settings.setShowEditorIndentGuides(false);
        settings.setShowIntentionBulb(false);
        settings.setShowPreviewForIntentionActions(false);
        settings.setRenderDocComments(true);
        settings.setShowCodeLensOnScrollbarHover(false);
        settings.setUseEditorFontForInlayHints(true);
        settings.setEnableTagTreeHighlighting(false);
        settings.setTagTreeLevelsToHighlight(10);
        settings.setTagTreeOpacity(0.35);

        // Serialization round-trip
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // Verify loaded properties
        assertFalse(loaded.isShowXmlAutoImportTooltip());

        assertFalse(loaded.isShowBreadcrumbs());
        assertEquals("Top", loaded.getBreadcrumbsPlacement());
        assertTrue(loaded.isBreadcrumbsHtml());
        assertTrue(loaded.isBreadcrumbsMarkdown());
        assertTrue(loaded.isBreadcrumbsXhtml());
        assertTrue(loaded.isBreadcrumbsJson());
        assertTrue(loaded.isBreadcrumbsSql());
        assertTrue(loaded.isBreadcrumbsXml());

        assertFalse(loaded.isCaretBlinking());
        assertEquals(750, loaded.getCaretBlinkingMs());
        assertTrue(loaded.isUseBlockCaret());
        assertTrue(loaded.isUseFullLineHeightCaret());
        assertFalse(loaded.isHighlightOccurrences());
        assertFalse(loaded.isShowHardWrapAndVisualGuides());
        assertFalse(loaded.isShowLineNumbers());
        assertEquals("Relative", loaded.getLineNumbersMode());
        assertTrue(loaded.isShowLinesBetweenStatements());
        assertTrue(loaded.isShowWhitespaces());
        assertFalse(loaded.isShowWhitespacesLeading());
        assertFalse(loaded.isShowWhitespacesInner());
        assertFalse(loaded.isShowWhitespacesTrailing());
        assertFalse(loaded.isShowWhitespacesSelection());
        assertFalse(loaded.isShowEditorIndentGuides());
        assertFalse(loaded.isShowIntentionBulb());
        assertFalse(loaded.isShowPreviewForIntentionActions());
        assertTrue(loaded.isRenderDocComments());
        assertFalse(loaded.isShowCodeLensOnScrollbarHover());
        assertTrue(loaded.isUseEditorFontForInlayHints());
        assertFalse(loaded.isEnableTagTreeHighlighting());
        assertEquals(10, loaded.getTagTreeLevelsToHighlight());
        assertEquals(0.35, loaded.getTagTreeOpacity(), 0.001);
    }

    @Test
    public void testCodeCompletionSettings() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Verify default values match DataGrip screenshots
        assertTrue(settings.isMatchCase());
        assertEquals("First letter only", settings.getMatchCaseMode());
        assertFalse(settings.isSortSuggestionsAlphabetically());
        assertTrue(settings.isShowSuggestionsAsYouType());
        assertFalse(settings.isInsertSelectedSuggestionByContextKeys());
        assertFalse(settings.isShowDocPopup());
        assertEquals(500, settings.getDocPopupDelayMs());
        assertTrue(settings.isInsertParenthesesAutomatically());

        assertTrue(settings.isMlSortSuggestions());
        assertTrue(settings.isMlSortSql());
        assertTrue(settings.isMlMarkPositionChanges());
        assertTrue(settings.isMlMarkMostRelevant());

        assertTrue(settings.isHtmlAutoPopupTagCompletion());

        assertTrue(settings.isShowParameterInfoPopup());
        assertEquals(1000, settings.getParameterInfoDelayMs());
        assertFalse(settings.isShowFullMethodSignatures());

        assertEquals("The current scope", settings.getSqlSuggestObjectsFrom());
        assertEquals("Always", settings.getQualifyWithDatabase());
        assertEquals("Always", settings.getQualifyWithSchema());
        assertEquals("Always", settings.getQualifyWithTableView());
        assertEquals("Always", settings.getQualifyWithTableAlias());
        assertEquals("On collisions", settings.getQualifyInBasicCompletion());
        assertEquals("Always", settings.getQualifyInJoinCompletion());
        assertEquals("On collisions", settings.getQualifyInRefactoring());
        assertEquals("On collisions", settings.getQualifyInLiveTemplates());
        assertEquals("On collisions", settings.getQualifyInDragDrop());

        assertTrue(settings.isJoinUseAliases());
        assertFalse(settings.isJoinInvertOperands());
        assertTrue(settings.isJoinSuggestNonStrictFk());

        assertFalse(settings.isTableAliasesAutoAdd());
        assertTrue(settings.isTableAliasesSuggest());
        assertNotNull(settings.getCustomTableAliases());
        assertTrue(settings.getCustomTableAliases().isEmpty());
        assertEquals("", settings.getAdditionalAcceptCharacters());

        // 2. Mutate settings
        settings.setMatchCase(false);
        settings.setMatchCaseMode("All letters");
        settings.setSortSuggestionsAlphabetically(true);
        settings.setShowSuggestionsAsYouType(false);
        settings.setInsertSelectedSuggestionByContextKeys(true);
        settings.setShowDocPopup(true);
        settings.setDocPopupDelayMs(300);
        settings.setInsertParenthesesAutomatically(false);

        settings.setMlSortSuggestions(false);
        settings.setMlSortSql(false);
        settings.setMlMarkPositionChanges(false);
        settings.setMlMarkMostRelevant(false);

        settings.setHtmlAutoPopupTagCompletion(false);

        settings.setShowParameterInfoPopup(false);
        settings.setParameterInfoDelayMs(1500);
        settings.setShowFullMethodSignatures(true);

        settings.setSqlSuggestObjectsFrom("The current search path only");
        settings.setQualifyWithDatabase("Never");
        settings.setQualifyWithSchema("Never");
        settings.setQualifyWithTableView("Never");
        settings.setQualifyWithTableAlias("Never");
        settings.setQualifyInBasicCompletion("Never");
        settings.setQualifyInJoinCompletion("Never");
        settings.setQualifyInRefactoring("Never");
        settings.setQualifyInLiveTemplates("Never");
        settings.setQualifyInDragDrop("Never");

        settings.setJoinUseAliases(false);
        settings.setJoinInvertOperands(true);
        settings.setJoinSuggestNonStrictFk(false);

        settings.setTableAliasesAutoAdd(true);
        settings.setTableAliasesSuggest(false);
        settings.getCustomTableAliases().add(new AppSettingsStore.TableAliasConfig("orders", "ord"));
        settings.getCustomTableAliases().add(new AppSettingsStore.TableAliasConfig("customers", "cust"));
        settings.setAdditionalAcceptCharacters(";,. ");

        // 3. Jackson JSON Roundtrip Serialization
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // 4. Verify deserialized values
        assertFalse(loaded.isMatchCase());
        assertEquals("All letters", loaded.getMatchCaseMode());
        assertTrue(loaded.isSortSuggestionsAlphabetically());
        assertFalse(loaded.isShowSuggestionsAsYouType());
        assertTrue(loaded.isInsertSelectedSuggestionByContextKeys());
        assertTrue(loaded.isShowDocPopup());
        assertEquals(300, loaded.getDocPopupDelayMs());
        assertFalse(loaded.isInsertParenthesesAutomatically());

        assertFalse(loaded.isMlSortSuggestions());
        assertFalse(loaded.isMlSortSql());
        assertFalse(loaded.isMlMarkPositionChanges());
        assertFalse(loaded.isMlMarkMostRelevant());

        assertFalse(loaded.isHtmlAutoPopupTagCompletion());

        assertFalse(loaded.isShowParameterInfoPopup());
        assertEquals(1500, loaded.getParameterInfoDelayMs());
        assertTrue(loaded.isShowFullMethodSignatures());

        assertEquals("The current search path only", loaded.getSqlSuggestObjectsFrom());
        assertEquals("Never", loaded.getQualifyWithDatabase());
        assertEquals("Never", loaded.getQualifyWithSchema());
        assertEquals("Never", loaded.getQualifyWithTableView());
        assertEquals("Never", loaded.getQualifyWithTableAlias());
        assertEquals("Never", loaded.getQualifyInBasicCompletion());
        assertEquals("Never", loaded.getQualifyInJoinCompletion());
        assertEquals("Never", loaded.getQualifyInRefactoring());
        assertEquals("Never", loaded.getQualifyInLiveTemplates());
        assertEquals("Never", loaded.getQualifyInDragDrop());

        assertFalse(loaded.isJoinUseAliases());
        assertTrue(loaded.isJoinInvertOperands());
        assertFalse(loaded.isJoinSuggestNonStrictFk());

        assertTrue(loaded.isTableAliasesAutoAdd());
        assertFalse(loaded.isTableAliasesSuggest());
        assertEquals(2, loaded.getCustomTableAliases().size());
        assertEquals("orders", loaded.getCustomTableAliases().get(0).getTableName());
        assertEquals("ord", loaded.getCustomTableAliases().get(0).getCustomAlias());
        assertEquals("customers", loaded.getCustomTableAliases().get(1).getTableName());
        assertEquals("cust", loaded.getCustomTableAliases().get(1).getCustomAlias());
        assertEquals(";,. ", loaded.getAdditionalAcceptCharacters());
    }

    @Test
    public void testCodeFoldingAndEditorTabsDescriptions() {
        String cfDesc = SettingsDialog.getCategoryDescription("Editor / General / Code Folding");
        assertEquals("Configure code folding for statements, comments, and subqueries.", cfDesc);

        String etDesc = SettingsDialog.getCategoryDescription("Editor / General / Editor Tabs");
        assertEquals("Configure tab placement, tab closing policy, and multi-row tabs.", etDesc);
    }

    @Test
    public void testCodeFoldingSettings() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Verify default values match DataGrip screenshots
        assertTrue(settings.isShowCodeFoldingArrows());
        assertEquals("On mouse hover", settings.getShowCodeFoldingArrowsMode());
        assertFalse(settings.isShowBottomArrows());

        assertTrue(settings.isFoldFileHeader());
        assertTrue(settings.isFoldImports());
        assertFalse(settings.isFoldDocComments());
        assertFalse(settings.isFoldMethodBodies());
        assertFalse(settings.isFoldCustomRegions());

        assertTrue(settings.isFoldMarkdownFrontMatter());
        assertTrue(settings.isFoldMarkdownLinks());
        assertFalse(settings.isFoldMarkdownTables());
        assertFalse(settings.isFoldMarkdownCodeFences());
        assertTrue(settings.isFoldMarkdownTableOfContents());

        assertFalse(settings.isFoldSqlUnderscoresInNumericLiterals());

        assertFalse(settings.isFoldXmlTags());
        assertTrue(settings.isFoldHtmlStyleAttribute());
        assertTrue(settings.isFoldXmlEntities());
        assertTrue(settings.isFoldDataUris());

        // 2. Mutate settings
        settings.setShowCodeFoldingArrows(false);
        settings.setShowCodeFoldingArrowsMode("Always");
        settings.setShowBottomArrows(true);

        settings.setFoldFileHeader(false);
        settings.setFoldImports(false);
        settings.setFoldDocComments(true);
        settings.setFoldMethodBodies(true);
        settings.setFoldCustomRegions(true);

        settings.setFoldMarkdownFrontMatter(false);
        settings.setFoldMarkdownLinks(false);
        settings.setFoldMarkdownTables(true);
        settings.setFoldMarkdownCodeFences(true);
        settings.setFoldMarkdownTableOfContents(false);

        settings.setFoldSqlUnderscoresInNumericLiterals(true);

        settings.setFoldXmlTags(true);
        settings.setFoldHtmlStyleAttribute(false);
        settings.setFoldXmlEntities(false);
        settings.setFoldDataUris(false);

        // 3. Jackson JSON Roundtrip Serialization
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // 4. Verify deserialized values
        assertFalse(loaded.isShowCodeFoldingArrows());
        assertEquals("Always", loaded.getShowCodeFoldingArrowsMode());
        assertTrue(loaded.isShowBottomArrows());

        assertFalse(loaded.isFoldFileHeader());
        assertFalse(loaded.isFoldImports());
        assertTrue(loaded.isFoldDocComments());
        assertTrue(loaded.isFoldMethodBodies());
        assertTrue(loaded.isFoldCustomRegions());

        assertFalse(loaded.isFoldMarkdownFrontMatter());
        assertFalse(loaded.isFoldMarkdownLinks());
        assertTrue(loaded.isFoldMarkdownTables());
        assertTrue(loaded.isFoldMarkdownCodeFences());
        assertFalse(loaded.isFoldMarkdownTableOfContents());

        assertTrue(loaded.isFoldSqlUnderscoresInNumericLiterals());

        assertTrue(loaded.isFoldXmlTags());
        assertFalse(loaded.isFoldHtmlStyleAttribute());
        assertFalse(loaded.isFoldXmlEntities());
        assertFalse(loaded.isFoldDataUris());
    }

    @Test
    public void testEditorTabsSettings() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Verify default values match DataGrip screenshots
        assertEquals("Top", settings.getEditorTabPlacement());
        assertEquals("One row", settings.getEditorTabsShowMode());
        assertEquals("Scroll the tabs panel", settings.getEditorTabsOverflowMode());
        assertFalse(settings.isShowPinnedTabsInSeparateRow());
        assertTrue(settings.isEditorTabsShowFileIcon());
        assertTrue(settings.isEditorTabsShowFileExtension());
        assertTrue(settings.isEditorTabsShowDirectoryForNonUnique());
        assertFalse(settings.isEditorTabsMarkModified());
        assertTrue(settings.isEditorTabsShowFullPathOnHover());
        assertEquals("Right", settings.getEditorTabsCloseButtonPosition());

        assertFalse(settings.isEditorTabsSortAlphabetically());
        assertFalse(settings.isEditorTabsOpenNewAtEnd());

        assertFalse(settings.isEditorTabsEnablePreviewTab());

        assertEquals(30, settings.getEditorTabsLimit());
        assertEquals("Close unused", settings.getEditorTabsExceedLimitPolicy());
        assertEquals("The tab on the left", settings.getEditorTabsCloseActivatePolicy());

        assertFalse(settings.isEditorTabsAlwaysShowQualifiedNames());
        assertTrue(settings.isEditorTabsShortenNames());

        // 2. Mutate settings
        settings.setEditorTabPlacement("Bottom");
        settings.setEditorTabsShowMode("Multiple rows");
        settings.setEditorTabsOverflowMode("Squeeze tabs");
        settings.setShowPinnedTabsInSeparateRow(true);
        settings.setEditorTabsShowFileIcon(false);
        settings.setEditorTabsShowFileExtension(false);
        settings.setEditorTabsShowDirectoryForNonUnique(false);
        settings.setEditorTabsMarkModified(true);
        settings.setEditorTabsShowFullPathOnHover(false);
        settings.setEditorTabsCloseButtonPosition("None");

        settings.setEditorTabsSortAlphabetically(true);
        settings.setEditorTabsOpenNewAtEnd(true);

        settings.setEditorTabsEnablePreviewTab(true);

        settings.setEditorTabsLimit(50);
        settings.setEditorTabsExceedLimitPolicy("Close unchanged");
        settings.setEditorTabsCloseActivatePolicy("Most recently opened tab");

        settings.setEditorTabsAlwaysShowQualifiedNames(true);
        settings.setEditorTabsShortenNames(false);

        // 3. Jackson JSON Roundtrip Serialization
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // 4. Verify deserialized values
        assertEquals("Bottom", loaded.getEditorTabPlacement());
        assertEquals("Multiple rows", loaded.getEditorTabsShowMode());
        assertEquals("Squeeze tabs", loaded.getEditorTabsOverflowMode());
        assertTrue(loaded.isShowPinnedTabsInSeparateRow());
        assertFalse(loaded.isEditorTabsShowFileIcon());
        assertFalse(loaded.isEditorTabsShowFileExtension());
        assertFalse(loaded.isEditorTabsShowDirectoryForNonUnique());
        assertTrue(loaded.isEditorTabsMarkModified());
        assertFalse(loaded.isEditorTabsShowFullPathOnHover());
        assertEquals("None", loaded.getEditorTabsCloseButtonPosition());

        assertTrue(loaded.isEditorTabsSortAlphabetically());
        assertTrue(loaded.isEditorTabsOpenNewAtEnd());

        assertTrue(loaded.isEditorTabsEnablePreviewTab());

        assertEquals(50, loaded.getEditorTabsLimit());
        assertEquals("Close unchanged", loaded.getEditorTabsExceedLimitPolicy());
        assertEquals("Most recently opened tab", loaded.getEditorTabsCloseActivatePolicy());

        assertTrue(loaded.isEditorTabsAlwaysShowQualifiedNames());
        assertFalse(loaded.isEditorTabsShortenNames());
    }

    @Test
    public void testOutputConsoleGutterIconsAndInlineCompletionDescriptions() {
        String ocDesc = SettingsDialog.getCategoryDescription("Editor / General / Output Console");
        assertEquals("Configure console buffer size, folding, and cyclic buffer limits.", ocDesc);

        String giDesc = SettingsDialog.getCategoryDescription("Editor / General / Gutter Icons");
        assertEquals("Configure run, breakpoint, and line-marker icons in the left gutter.", giDesc);

        String icDesc = SettingsDialog.getCategoryDescription("Editor / General / Inline Completion");
        assertEquals("Configure full line and inline completion suggestions and typing triggers.", icDesc);
    }

    @Test
    public void testOutputConsoleSettings() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Verify default values match DataGrip screenshots
        assertFalse(settings.isOutputConsoleUseSoftWraps());
        assertEquals(300, settings.getOutputConsoleHistorySize());
        assertFalse(settings.isOutputConsoleOverrideCycleBuffer());
        assertEquals(1024, settings.getOutputConsoleCycleBufferSizeKb());
        assertEquals("<System Default: UTF-8>", settings.getOutputConsoleDefaultEncoding());
        assertNotNull(settings.getOutputConsoleFoldingPatterns());
        assertTrue(settings.getOutputConsoleFoldingPatterns().isEmpty());
        assertNotNull(settings.getOutputConsoleFoldingExceptions());
        assertTrue(settings.getOutputConsoleFoldingExceptions().isEmpty());

        // 2. Mutate settings
        settings.setOutputConsoleUseSoftWraps(true);
        settings.setOutputConsoleHistorySize(500);
        settings.setOutputConsoleOverrideCycleBuffer(true);
        settings.setOutputConsoleCycleBufferSizeKb(2048);
        settings.setOutputConsoleDefaultEncoding("UTF-8");
        settings.getOutputConsoleFoldingPatterns().add("at org.springframework");
        settings.getOutputConsoleFoldingPatterns().add("at com.intellij");
        settings.getOutputConsoleFoldingExceptions().add("Caused by:");

        // 3. Jackson JSON Roundtrip Serialization
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // 4. Verify deserialized values
        assertTrue(loaded.isOutputConsoleUseSoftWraps());
        assertEquals(500, loaded.getOutputConsoleHistorySize());
        assertTrue(loaded.isOutputConsoleOverrideCycleBuffer());
        assertEquals(2048, loaded.getOutputConsoleCycleBufferSizeKb());
        assertEquals("UTF-8", loaded.getOutputConsoleDefaultEncoding());
        assertEquals(2, loaded.getOutputConsoleFoldingPatterns().size());
        assertEquals("at org.springframework", loaded.getOutputConsoleFoldingPatterns().get(0));
        assertEquals("at com.intellij", loaded.getOutputConsoleFoldingPatterns().get(1));
        assertEquals(1, loaded.getOutputConsoleFoldingExceptions().size());
        assertEquals("Caused by:", loaded.getOutputConsoleFoldingExceptions().get(0));
    }

    @Test
    public void testGutterIconsSettings() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Verify default values match DataGrip screenshots
        assertTrue(settings.isShowGutterIcons());
        assertTrue(settings.isGutterColorPreview());
        assertTrue(settings.isGutterDocComments());
        assertTrue(settings.isGutterRunLineMarker());
        assertTrue(settings.isGutterRecursiveCall());
        assertTrue(settings.isGutterVcsIgnoredDirectories());
        assertTrue(settings.isGutterConfigureHtmlImage());
        assertTrue(settings.isGutterConfigureMarkdownImage());
        assertTrue(settings.isGutterInstallPlantUml());

        // 2. Mutate settings
        settings.setShowGutterIcons(false);
        settings.setGutterColorPreview(false);
        settings.setGutterDocComments(false);
        settings.setGutterRunLineMarker(false);
        settings.setGutterRecursiveCall(false);
        settings.setGutterVcsIgnoredDirectories(false);
        settings.setGutterConfigureHtmlImage(false);
        settings.setGutterConfigureMarkdownImage(false);
        settings.setGutterInstallPlantUml(false);

        // 3. Jackson JSON Roundtrip Serialization
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // 4. Verify deserialized values
        assertFalse(loaded.isShowGutterIcons());
        assertFalse(loaded.isGutterColorPreview());
        assertFalse(loaded.isGutterDocComments());
        assertFalse(loaded.isGutterRunLineMarker());
        assertFalse(loaded.isGutterRecursiveCall());
        assertFalse(loaded.isGutterVcsIgnoredDirectories());
        assertFalse(loaded.isGutterConfigureHtmlImage());
        assertFalse(loaded.isGutterConfigureMarkdownImage());
        assertFalse(loaded.isGutterInstallPlantUml());
    }

    @Test
    public void testInlineCompletionSettings() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Verify default values match DataGrip screenshots
        assertTrue(settings.isInlineCompletionEnabled());
        assertTrue(settings.isInlineAutoOnTyping());
        assertTrue(settings.isInlineMultilineSuggestions());
        assertFalse(settings.isInlineSyncWithPopup());

        // 2. Mutate settings
        settings.setInlineCompletionEnabled(false);
        settings.setInlineAutoOnTyping(false);
        settings.setInlineMultilineSuggestions(false);
        settings.setInlineSyncWithPopup(true);

        // 3. Jackson JSON Roundtrip Serialization
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // 4. Verify deserialized values
        assertFalse(loaded.isInlineCompletionEnabled());
        assertFalse(loaded.isInlineAutoOnTyping());
        assertFalse(loaded.isInlineMultilineSuggestions());
        assertTrue(loaded.isInlineSyncWithPopup());
    }

    @Test
    public void testSmartKeysSettings() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Verify default values match DataGrip screenshots
        assertTrue(settings.isSmartKeysHomeMovesCaret());
        assertTrue(settings.isSmartKeysEndBlankLineMovesCaret());
        assertTrue(settings.isSmartKeysInsertPairedBrackets());
        assertTrue(settings.isSmartKeysInsertPairQuote());
        assertTrue(settings.isSmartKeysReformatBlockOnBrace());
        assertFalse(settings.isSmartKeysUseCamelHumps());
        assertTrue(settings.isSmartKeysHonorCamelHumpsOnDoubleClick());
        assertTrue(settings.isSmartKeysSurroundSelectionOnQuoteOrBrace());
        assertTrue(settings.isSmartKeysMultiCaretsOnDoubleModifier());
        assertTrue(settings.isSmartKeysJumpOutsideBracketWithTab());
        assertTrue(settings.isSmartKeysEnterSmartIndent());
        assertTrue(settings.isSmartKeysEnterInsertPairBrace());
        assertTrue(settings.isSmartKeysEnterCloseBlockComment());
        assertEquals("To proper indent position", settings.getSmartKeysUnindentOnBackspace());
        assertEquals("None", settings.getSmartKeysReformatOnPaste());
        assertFalse(settings.isSmartKeysReformatRemoveCustomLineBreaks());

        // 2. Verify options lists
        List<String> unindentOpts = AppSettingsStore.Settings.defaultSmartKeysUnindentOptions();
        assertEquals(3, unindentOpts.size());
        assertTrue(unindentOpts.contains("Disabled"));
        assertTrue(unindentOpts.contains("To proper indent position"));
        assertTrue(unindentOpts.contains("To nearest indent position"));

        List<String> reformatOpts = AppSettingsStore.Settings.defaultSmartKeysReformatOnPasteOptions();
        assertEquals(4, reformatOpts.size());
        assertTrue(reformatOpts.contains("None"));
        assertTrue(reformatOpts.contains("Indent block"));
        assertTrue(reformatOpts.contains("Indent each line"));
        assertTrue(reformatOpts.contains("Reformat block"));

        // 3. Mutate settings
        settings.setSmartKeysHomeMovesCaret(false);
        settings.setSmartKeysEndBlankLineMovesCaret(false);
        settings.setSmartKeysInsertPairedBrackets(false);
        settings.setSmartKeysInsertPairQuote(false);
        settings.setSmartKeysReformatBlockOnBrace(false);
        settings.setSmartKeysUseCamelHumps(true);
        settings.setSmartKeysHonorCamelHumpsOnDoubleClick(false);
        settings.setSmartKeysSurroundSelectionOnQuoteOrBrace(false);
        settings.setSmartKeysMultiCaretsOnDoubleModifier(false);
        settings.setSmartKeysJumpOutsideBracketWithTab(false);
        settings.setSmartKeysEnterSmartIndent(false);
        settings.setSmartKeysEnterInsertPairBrace(false);
        settings.setSmartKeysEnterCloseBlockComment(false);
        settings.setSmartKeysUnindentOnBackspace("Disabled");
        settings.setSmartKeysReformatOnPaste("Reformat block");
        settings.setSmartKeysReformatRemoveCustomLineBreaks(true);

        // 4. Jackson JSON Roundtrip Serialization
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // 5. Verify deserialized values
        assertFalse(loaded.isSmartKeysHomeMovesCaret());
        assertFalse(loaded.isSmartKeysEndBlankLineMovesCaret());
        assertFalse(loaded.isSmartKeysInsertPairedBrackets());
        assertFalse(loaded.isSmartKeysInsertPairQuote());
        assertFalse(loaded.isSmartKeysReformatBlockOnBrace());
        assertTrue(loaded.isSmartKeysUseCamelHumps());
        assertFalse(loaded.isSmartKeysHonorCamelHumpsOnDoubleClick());
        assertFalse(loaded.isSmartKeysSurroundSelectionOnQuoteOrBrace());
        assertFalse(loaded.isSmartKeysMultiCaretsOnDoubleModifier());
        assertFalse(loaded.isSmartKeysJumpOutsideBracketWithTab());
        assertFalse(loaded.isSmartKeysEnterSmartIndent());
        assertFalse(loaded.isSmartKeysEnterInsertPairBrace());
        assertFalse(loaded.isSmartKeysEnterCloseBlockComment());
        assertEquals("Disabled", loaded.getSmartKeysUnindentOnBackspace());
        assertEquals("Reformat block", loaded.getSmartKeysReformatOnPaste());
        assertTrue(loaded.isSmartKeysReformatRemoveCustomLineBreaks());
    }

    @Test
    public void testPostfixCompletionSettings() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Verify default values match DataGrip screenshots
        assertTrue(settings.isPostfixCompletionEnabled());
        assertEquals("Tab", settings.getPostfixCompletionExpandWith());
        assertNotNull(settings.getPostfixTemplates());
        assertEquals(5, settings.getPostfixTemplates().size(), "DataGrip provides 5 built-in SQL postfix templates");

        AppSettingsStore.PostfixTemplateConfig afrom = settings.getPostfixTemplates().get(0);
        assertEquals("afrom", afrom.getKey());
        assertEquals("SQL", afrom.getLanguage());
        assertTrue(afrom.isEnabled());

        AppSettingsStore.PostfixTemplateConfig cast = settings.getPostfixTemplates().get(1);
        assertEquals("cast", cast.getKey());
        assertTrue(cast.isEnabled());

        AppSettingsStore.PostfixTemplateConfig cfrom = settings.getPostfixTemplates().get(2);
        assertEquals("cfrom", cfrom.getKey());
        assertTrue(cfrom.isEnabled());

        AppSettingsStore.PostfixTemplateConfig from = settings.getPostfixTemplates().get(3);
        assertEquals("from", from.getKey());
        assertTrue(from.isEnabled());

        AppSettingsStore.PostfixTemplateConfig join = settings.getPostfixTemplates().get(4);
        assertEquals("join", join.getKey());
        assertTrue(join.isEnabled());

        // 2. Verify expand with options
        List<String> expandOpts = AppSettingsStore.Settings.defaultPostfixExpandWithOptions();
        assertEquals(3, expandOpts.size());
        assertTrue(expandOpts.contains("Tab"));
        assertTrue(expandOpts.contains("Space"));
        assertTrue(expandOpts.contains("Enter"));

        // 3. Mutate settings
        settings.setPostfixCompletionEnabled(false);
        settings.setPostfixCompletionExpandWith("Space");
        settings.getPostfixTemplates().get(0).setEnabled(false);
        settings.getPostfixTemplates().add(new AppSettingsStore.PostfixTemplateConfig(
                "cnt", "SQL", "select count(*) from $EXPR$", "count rows", true, "authors.cnt", "select count(*) from authors"
        ));

        // 4. Jackson JSON Roundtrip Serialization
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // 5. Verify deserialized values
        assertFalse(loaded.isPostfixCompletionEnabled());
        assertEquals("Space", loaded.getPostfixCompletionExpandWith());
        assertEquals(6, loaded.getPostfixTemplates().size());
        assertFalse(loaded.getPostfixTemplates().get(0).isEnabled());
        AppSettingsStore.PostfixTemplateConfig custom = loaded.getPostfixTemplates().get(5);
        assertEquals("cnt", custom.getKey());
        assertEquals("SQL", custom.getLanguage());
        assertEquals("select count(*) from $EXPR$", custom.getExpression());
        assertEquals("count rows", custom.getDescription());
        assertTrue(custom.isEnabled());
        assertEquals("authors.cnt", custom.getExampleBefore());
        assertEquals("select count(*) from authors", custom.getExampleAfter());
    }

    @Test
    public void testSmartKeysAndPostfixCategoryDescriptions() {
        String skDesc = SettingsDialog.getCategoryDescription("Editor / General / Smart Keys");
        assertNotNull(skDesc);
        assertEquals("Configure smart typing, auto-closing quotes/brackets, and indent behavior.", skDesc);

        String pcDesc = SettingsDialog.getCategoryDescription("Editor / General / Postfix Completion");
        assertNotNull(pcDesc);
        assertEquals("Configure postfix completion templates and expansions.", pcDesc);

        // Subcategory descriptions
        assertEquals("Configure tag auto-closing, attribute completion, and CSS identifier selection.",
                SettingsDialog.getCategoryDescription("Editor / General / Smart Keys / HTML/CSS"));
        assertEquals("Configure quote escaping, comma insertion, and property colon handling in JSON.",
                SettingsDialog.getCategoryDescription("Editor / General / Smart Keys / JSON"));
        assertEquals("Configure table formatting, list numbering, and link handling in Markdown.",
                SettingsDialog.getCategoryDescription("Editor / General / Smart Keys / Markdown"));
        assertEquals("Configure string concatenation and code block closing on Enter in SQL.",
                SettingsDialog.getCategoryDescription("Editor / General / Smart Keys / SQL"));
    }

    @Test
    public void testSmartKeysSubcategoriesDefaultsAndSerialization() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Verify SQL Smart Keys defaults
        assertTrue(settings.isSmartKeysSqlInsertStringConcatOnEnter());
        assertTrue(settings.isSmartKeysSqlCloseCodeBlocksOnEnter());

        // 2. Verify Markdown Smart Keys defaults
        assertTrue(settings.isSmartKeysMarkdownReformatTable());
        assertTrue(settings.isSmartKeysMarkdownInsertHtmlLineBreakInTable());
        assertTrue(settings.isSmartKeysMarkdownShiftEnterNewTableRow());
        assertTrue(settings.isSmartKeysMarkdownTabNavigateTable());
        assertTrue(settings.isSmartKeysMarkdownAdjustListIndent());
        assertTrue(settings.isSmartKeysMarkdownSmartEnterBackspace());
        assertFalse(settings.isSmartKeysMarkdownRenumberList());
        assertEquals("Sequentially", settings.getSmartKeysMarkdownListNumerating());
        assertTrue(settings.isSmartKeysMarkdownInsertLinksOnDrop());
        List<String> listOptions = AppSettingsStore.Settings.defaultMarkdownListNumeratingOptions();
        assertEquals(3, listOptions.size());
        assertTrue(listOptions.contains("Sequentially"));
        assertTrue(listOptions.contains("With '1.'"));
        assertTrue(listOptions.contains("With previous number"));

        // 3. Verify JSON Smart Keys defaults
        assertTrue(settings.isSmartKeysJsonInsertMissingCommaOnEnter());
        assertTrue(settings.isSmartKeysJsonInsertMissingCommaAfterMatching());
        assertTrue(settings.isSmartKeysJsonManageCommasOnPaste());
        assertTrue(settings.isSmartKeysJsonEscapeTextOnPaste());
        assertTrue(settings.isSmartKeysJsonAddQuotesToPropertyNames());
        assertTrue(settings.isSmartKeysJsonAddWhitespaceAfterColon());
        assertFalse(settings.isSmartKeysJsonMoveColonAfterPropertyName());
        assertFalse(settings.isSmartKeysJsonMoveCommaAfterPropertyValue());

        // 4. Verify HTML/CSS Smart Keys defaults
        assertTrue(settings.isSmartKeysHtmlInsertClosingTag());
        assertTrue(settings.isSmartKeysHtmlInsertRequiredAttributes());
        assertTrue(settings.isSmartKeysHtmlInsertRequiredSubtags());
        assertTrue(settings.isSmartKeysHtmlStartAttribute());
        assertTrue(settings.isSmartKeysHtmlAddQuotesForAttribute());
        assertTrue(settings.isSmartKeysHtmlAutoCloseTag());
        assertTrue(settings.isSmartKeysHtmlSimultaneousTagEditing());
        assertTrue(settings.isSmartKeysCssSelectWholeCssIdentifiers());

        // 5. Mutate all Smart Keys subcategory settings
        settings.setSmartKeysSqlInsertStringConcatOnEnter(false);
        settings.setSmartKeysSqlCloseCodeBlocksOnEnter(false);

        settings.setSmartKeysMarkdownReformatTable(false);
        settings.setSmartKeysMarkdownInsertHtmlLineBreakInTable(false);
        settings.setSmartKeysMarkdownShiftEnterNewTableRow(false);
        settings.setSmartKeysMarkdownTabNavigateTable(false);
        settings.setSmartKeysMarkdownAdjustListIndent(false);
        settings.setSmartKeysMarkdownSmartEnterBackspace(false);
        settings.setSmartKeysMarkdownRenumberList(true);
        settings.setSmartKeysMarkdownListNumerating("With '1.'");
        settings.setSmartKeysMarkdownInsertLinksOnDrop(false);

        settings.setSmartKeysJsonInsertMissingCommaOnEnter(false);
        settings.setSmartKeysJsonInsertMissingCommaAfterMatching(false);
        settings.setSmartKeysJsonManageCommasOnPaste(false);
        settings.setSmartKeysJsonEscapeTextOnPaste(false);
        settings.setSmartKeysJsonAddQuotesToPropertyNames(false);
        settings.setSmartKeysJsonAddWhitespaceAfterColon(false);
        settings.setSmartKeysJsonMoveColonAfterPropertyName(true);
        settings.setSmartKeysJsonMoveCommaAfterPropertyValue(true);

        settings.setSmartKeysHtmlInsertClosingTag(false);
        settings.setSmartKeysHtmlInsertRequiredAttributes(false);
        settings.setSmartKeysHtmlInsertRequiredSubtags(false);
        settings.setSmartKeysHtmlStartAttribute(false);
        settings.setSmartKeysHtmlAddQuotesForAttribute(false);
        settings.setSmartKeysHtmlAutoCloseTag(false);
        settings.setSmartKeysHtmlSimultaneousTagEditing(false);
        settings.setSmartKeysCssSelectWholeCssIdentifiers(false);

        // 6. Jackson JSON Roundtrip Serialization
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // 7. Verify deserialized values
        assertFalse(loaded.isSmartKeysSqlInsertStringConcatOnEnter());
        assertFalse(loaded.isSmartKeysSqlCloseCodeBlocksOnEnter());

        assertFalse(loaded.isSmartKeysMarkdownReformatTable());
        assertFalse(loaded.isSmartKeysMarkdownInsertHtmlLineBreakInTable());
        assertFalse(loaded.isSmartKeysMarkdownShiftEnterNewTableRow());
        assertFalse(loaded.isSmartKeysMarkdownTabNavigateTable());
        assertFalse(loaded.isSmartKeysMarkdownAdjustListIndent());
        assertFalse(loaded.isSmartKeysMarkdownSmartEnterBackspace());
        assertTrue(loaded.isSmartKeysMarkdownRenumberList());
        assertEquals("With '1.'", loaded.getSmartKeysMarkdownListNumerating());
        assertFalse(loaded.isSmartKeysMarkdownInsertLinksOnDrop());

        assertFalse(loaded.isSmartKeysJsonInsertMissingCommaOnEnter());
        assertFalse(loaded.isSmartKeysJsonInsertMissingCommaAfterMatching());
        assertFalse(loaded.isSmartKeysJsonManageCommasOnPaste());
        assertFalse(loaded.isSmartKeysJsonEscapeTextOnPaste());
        assertFalse(loaded.isSmartKeysJsonAddQuotesToPropertyNames());
        assertFalse(loaded.isSmartKeysJsonAddWhitespaceAfterColon());
        assertTrue(loaded.isSmartKeysJsonMoveColonAfterPropertyName());
        assertTrue(loaded.isSmartKeysJsonMoveCommaAfterPropertyValue());

        assertFalse(loaded.isSmartKeysHtmlInsertClosingTag());
        assertFalse(loaded.isSmartKeysHtmlInsertRequiredAttributes());
        assertFalse(loaded.isSmartKeysHtmlInsertRequiredSubtags());
        assertFalse(loaded.isSmartKeysHtmlStartAttribute());
        assertFalse(loaded.isSmartKeysHtmlAddQuotesForAttribute());
        assertFalse(loaded.isSmartKeysHtmlAutoCloseTag());
        assertFalse(loaded.isSmartKeysHtmlSimultaneousTagEditing());
        assertFalse(loaded.isSmartKeysCssSelectWholeCssIdentifiers());
    }

    @Test
    public void testStickyLinesCodeEditingAndFontCategoryDescriptions() {
        String stickyDesc = SettingsDialog.getCategoryDescription("Editor / General / Sticky Lines");
        assertNotNull(stickyDesc);
        assertEquals("Keep current scope header visible at the top of the editor while scrolling.", stickyDesc);

        String codeEditDesc = SettingsDialog.getCategoryDescription("Editor / Code Editing");
        assertNotNull(codeEditDesc);
        assertEquals("Configure caret movement highlighting, quick doc, refactoring options, error highlighting, and tooltips.", codeEditDesc);

        String fontDesc = SettingsDialog.getCategoryDescription("Editor / Font");
        assertNotNull(fontDesc);
        assertEquals("Customize the font family, font size, line height, ligatures, and typography for SQL consoles and editors.", fontDesc);
    }

    @Test
    public void testStickyLinesCodeEditingAndFontDefaultsAndSerialization() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();

        // 1. Sticky Lines defaults
        assertTrue(settings.isStickyLinesEnabled());
        assertEquals(5, settings.getStickyLinesMaxLines());
        assertTrue(settings.isStickyLinesHtml());
        assertTrue(settings.isStickyLinesMarkdown());
        assertTrue(settings.isStickyLinesXhtml());
        assertTrue(settings.isStickyLinesJson());
        assertTrue(settings.isStickyLinesSql());
        assertTrue(settings.isStickyLinesXml());

        // 2. Code Editing defaults
        assertTrue(settings.isCodeEditingHighlightMatchedBrace());
        assertFalse(settings.isCodeEditingHighlightCurrentScope());
        assertTrue(settings.isCodeEditingHighlightUsages());
        assertTrue(settings.isCodeEditingShowDocOnHover());
        assertEquals("In the editor", settings.getCodeEditingRefactoringOption());
        assertTrue(settings.isCodeEditingPreselectCurrentSymbol());
        assertTrue(settings.isCodeEditingShowInlineDialogForLocalVars());
        assertEquals(2, settings.getCodeEditingErrorStripeMarkMinHeight());
        assertEquals(300, settings.getCodeEditingAutoreparseDelayMs());
        assertEquals("The problems with the highest priority", settings.getCodeEditingNextErrorAction());
        assertEquals(500, settings.getCodeEditingTooltipDelayMs());

        assertEquals(2, AppSettingsStore.Settings.defaultNextErrorActionOptions().size());
        assertEquals(2, AppSettingsStore.Settings.defaultRefactoringOptions().size());

        // 3. Font defaults
        assertEquals("JetBrains Mono", settings.getEditorFontFamily());
        assertEquals(13.0, settings.getEditorFontSize());
        assertEquals(1.2, settings.getEditorLineHeight());
        assertFalse(settings.isEditorEnableLigatures());
        assertEquals("Regular", settings.getEditorFontMainWeight());
        assertEquals("Bold Recommended", settings.getEditorFontBoldWeight());
        assertEquals("<None>", settings.getEditorFallbackFont());

        assertEquals(8, AppSettingsStore.Settings.defaultFontWeights().size());
        assertEquals(8, AppSettingsStore.Settings.defaultFontBoldWeights().size());

        // 4. Mutate settings
        settings.setStickyLinesEnabled(false);
        settings.setStickyLinesMaxLines(8);
        settings.setStickyLinesHtml(false);
        settings.setStickyLinesMarkdown(false);
        settings.setStickyLinesXhtml(false);
        settings.setStickyLinesJson(false);
        settings.setStickyLinesSql(false);
        settings.setStickyLinesXml(false);

        settings.setCodeEditingHighlightMatchedBrace(false);
        settings.setCodeEditingHighlightCurrentScope(true);
        settings.setCodeEditingHighlightUsages(false);
        settings.setCodeEditingShowDocOnHover(false);
        settings.setCodeEditingRefactoringOption("In modal dialogs");
        settings.setCodeEditingPreselectCurrentSymbol(false);
        settings.setCodeEditingShowInlineDialogForLocalVars(false);
        settings.setCodeEditingErrorStripeMarkMinHeight(4);
        settings.setCodeEditingAutoreparseDelayMs(500);
        settings.setCodeEditingNextErrorAction("All problems");
        settings.setCodeEditingTooltipDelayMs(800);

        settings.setEditorFontFamily("Fira Code");
        settings.setEditorFontSize(15.0);
        settings.setEditorLineHeight(1.4);
        settings.setEditorEnableLigatures(true);
        settings.setEditorFontMainWeight("Medium");
        settings.setEditorFontBoldWeight("ExtraBold");
        settings.setEditorFallbackFont("Courier New");

        // 5. Jackson JSON Roundtrip Serialization
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);

        // 6. Verify deserialized values
        assertFalse(loaded.isStickyLinesEnabled());
        assertEquals(8, loaded.getStickyLinesMaxLines());
        assertFalse(loaded.isStickyLinesHtml());
        assertFalse(loaded.isStickyLinesMarkdown());
        assertFalse(loaded.isStickyLinesXhtml());
        assertFalse(loaded.isStickyLinesJson());
        assertFalse(loaded.isStickyLinesSql());
        assertFalse(loaded.isStickyLinesXml());

        assertFalse(loaded.isCodeEditingHighlightMatchedBrace());
        assertTrue(loaded.isCodeEditingHighlightCurrentScope());
        assertFalse(loaded.isCodeEditingHighlightUsages());
        assertFalse(loaded.isCodeEditingShowDocOnHover());
        assertEquals("In modal dialogs", loaded.getCodeEditingRefactoringOption());
        assertFalse(loaded.isCodeEditingPreselectCurrentSymbol());
        assertFalse(loaded.isCodeEditingShowInlineDialogForLocalVars());
        assertEquals(4, loaded.getCodeEditingErrorStripeMarkMinHeight());
        assertEquals(500, loaded.getCodeEditingAutoreparseDelayMs());
        assertEquals("All problems", loaded.getCodeEditingNextErrorAction());
        assertEquals(800, loaded.getCodeEditingTooltipDelayMs());

        assertEquals("Fira Code", loaded.getEditorFontFamily());
        assertEquals(15.0, loaded.getEditorFontSize());
        assertEquals(1.4, loaded.getEditorLineHeight());
        assertTrue(loaded.isEditorEnableLigatures());
        assertEquals("Medium", loaded.getEditorFontMainWeight());
        assertEquals("ExtraBold", loaded.getEditorFontBoldWeight());
        assertEquals("Courier New", loaded.getEditorFallbackFont());
    }

    @Test
    public void testKeymapPresetsInitialization() {
        List<String> presets = AppSettingsStore.defaultKeymapPresets();
        assertNotNull(presets);
        assertFalse(presets.isEmpty());
        if (AppSettingsStore.Settings.isMac()) {
            assertTrue(presets.contains("macOS"));
            assertTrue(presets.contains("Emacs"));
            assertTrue(presets.contains("IntelliJ IDEA Classic"));
            assertTrue(presets.contains("macOS System Shortcuts"));
            assertTrue(presets.contains("Sublime Text"));
            assertTrue(presets.contains("Sublime Text (macOS)"));
            assertEquals("macOS", AppSettingsStore.defaultKeymapPreset());
        } else {
            assertTrue(presets.contains("Windows"));
            assertTrue(presets.contains("IntelliJ IDEA Classic"));
            assertTrue(presets.contains("Emacs"));
            assertTrue(presets.contains("Eclipse"));
            assertTrue(presets.contains("Visual Studio"));
            assertTrue(presets.contains("Sublime Text"));
            assertEquals("Windows", AppSettingsStore.defaultKeymapPreset());
        }
    }

    @Test
    public void testKeymapActionItemShortcutsResolution() {
        SettingsDialog.KeymapActionItem item = new SettingsDialog.KeymapActionItem(
                "help.find.action", "Find Action…", "Find any action or settings entry",
                "Main Menu | Help", false, null, null, List.of("⇧⌘A"));

        // Preset resolution
        assertEquals(List.of("⇧⌘A"), item.resolvePresetShortcuts("macOS"));
        assertEquals(List.of("Ctrl+Shift+A"), item.resolvePresetShortcuts("Windows"));
        assertEquals(List.of("Alt+X"), item.resolvePresetShortcuts("Emacs"));
        assertTrue(item.resolvePresetShortcuts("Sublime Text (macOS)").contains("⇧⌘P"));

        // Effective shortcuts without overrides
        Map<String, List<String>> custom = new HashMap<>();
        Map<String, List<String>> removed = new HashMap<>();
        assertEquals(List.of("⇧⌘A"), item.getEffectiveShortcuts("macOS", custom, removed));
        assertFalse(item.isModified("macOS", custom, removed));

        // Custom override
        custom.put("help.find.action", List.of("⇧⌘F"));
        assertEquals(List.of("⇧⌘F"), item.getEffectiveShortcuts("macOS", custom, removed));
        assertTrue(item.isModified("macOS", custom, removed));

        // Removal
        custom.remove("help.find.action");
        removed.put("help.find.action", List.of("⇧⌘A"));
        assertTrue(item.getEffectiveShortcuts("macOS", custom, removed).isEmpty());
        assertTrue(item.isModified("macOS", custom, removed));
    }

    @Test
    public void testKeymapConflictDetection() {
        // macOS system conflict detection matching DataGrip
        assertEquals("Search man Page Index in Terminal in macOS shortcuts",
                KeyStrokeFormatter.checkMacConflict("⇧⌘A"));
        assertEquals("Spotlight Search in macOS shortcuts",
                KeyStrokeFormatter.checkMacConflict("⌘Space"));
        assertEquals("Minimize in macOS shortcuts",
                KeyStrokeFormatter.checkMacConflict("⌘M"));
        assertNull(KeyStrokeFormatter.checkMacConflict("Ctrl+Alt+Shift+Z"));
    }

    @Test
    public void testKeymapSettingsSerializationRoundTrip() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        AppSettingsStore.Settings original = new AppSettingsStore.Settings();
        original.setKeymapPreset("macOS copy");
        original.setCustomKeymapPresets(List.of("macOS copy", "Custom Keymap"));

        Map<String, List<String>> customShortcuts = new LinkedHashMap<>();
        customShortcuts.put("help.find.action", List.of("⇧⌘F"));
        customShortcuts.put("db.execute", List.of("⌥↵"));
        original.setCustomKeymapShortcuts(customShortcuts);

        Map<String, List<String>> removedShortcuts = new LinkedHashMap<>();
        removedShortcuts.put("file.new.scratch", List.of("⇧⌘N"));
        original.setRemovedKeymapShortcuts(removedShortcuts);

        String json = mapper.writeValueAsString(original);
        assertNotNull(json);
        assertTrue(json.contains("macOS copy"));
        assertTrue(json.contains("help.find.action"));
        assertTrue(json.contains("⇧⌘F"));

        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertEquals("macOS copy", loaded.getKeymapPreset());
        assertEquals(2, loaded.getCustomKeymapPresets().size());
        assertTrue(loaded.getCustomKeymapPresets().contains("Custom Keymap"));
        assertEquals(List.of("⇧⌘F"), loaded.getCustomKeymapShortcuts().get("help.find.action"));
        assertEquals(List.of("⇧⌘N"), loaded.getRemovedKeymapShortcuts().get("file.new.scratch"));
    }

    @Test
    public void testKeymapPanelConstructionAndInputsRegistration() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        settings.getCustomKeymapPresets().add("My Custom Keymap");
        settings.getCustomKeymapShortcuts().put("help.find.action", List.of("Ctrl+Shift+P"));

        Map<String, Object> inputs = new HashMap<>();
        javafx.scene.layout.VBox panel = SettingsDialog.buildKeymapPanel(settings, inputs, target -> {});

        assertNotNull(panel);
        assertTrue(inputs.containsKey("keymapCombo"));
        assertTrue(inputs.containsKey("customKeymapPresets"));
        assertTrue(inputs.containsKey("customKeymapShortcuts"));
        assertTrue(inputs.containsKey("removedKeymapShortcuts"));

        @SuppressWarnings("unchecked")
        javafx.scene.control.ComboBox<String> combo = (javafx.scene.control.ComboBox<String>) inputs.get("keymapCombo");
        assertNotNull(combo);
        assertTrue(combo.getItems().contains("My Custom Keymap"));
    }

    @Test
    public void testColorSchemeCategoriesAndElementsMatchDataGrip() {
        List<String> categories = ColorSchemeModel.getCategories();
        assertNotNull(categories);
        assertEquals(11, categories.size(), "Should have exactly 11 color scheme categories");

        List<String> expectedCategories = List.of(
                "Code",
                "Editor",
                "Errors and Warnings",
                "Hyperlinks",
                "Identifiers",
                "Line Coverage",
                "Live Templates",
                "Popups and Hints",
                "Preview",
                "Search Results",
                "Text"
        );
        assertEquals(expectedCategories, categories);

        // Check Code category elements
        List<ColorSchemeElement> codeElements = ColorSchemeModel.getElementsByCategory("Code");
        assertNotNull(codeElements);
        assertTrue(codeElements.stream().anyMatch(e -> "code.line_number".equals(e.getId()) && "Line number".equals(e.getName())));
        assertTrue(codeElements.stream().anyMatch(e -> "code.line_number_caret".equals(e.getId()) && "Line number on caret row".equals(e.getName())));
        assertTrue(codeElements.stream().anyMatch(e -> "code.matched_brace".equals(e.getId()) && "Matched brace".equals(e.getName())));
        assertTrue(codeElements.stream().anyMatch(e -> "code.unmatched_brace".equals(e.getId()) && "Unmatched brace".equals(e.getName())));
        assertTrue(codeElements.stream().anyMatch(e -> "code.todo".equals(e.getId()) && "TODO defaults".equals(e.getName())));
        assertTrue(codeElements.stream().anyMatch(e -> "code.identifier_caret".equals(e.getId()) && "Identifier under caret".equals(e.getName())));
        assertTrue(codeElements.stream().anyMatch(e -> "code.injected_fragment".equals(e.getId()) && "Injected language fragment".equals(e.getName())));

        // Check Editor subcategories
        List<ColorSchemeElement> editorElements = ColorSchemeModel.getElementsByCategory("Editor");
        assertNotNull(editorElements);
        assertTrue(editorElements.stream().anyMatch(e -> "Breadcrumbs".equals(e.getSubCategory()) && "Current".equals(e.getName())));
        assertTrue(editorElements.stream().anyMatch(e -> "Breadcrumbs".equals(e.getSubCategory()) && "Default".equals(e.getName())));
        assertTrue(editorElements.stream().anyMatch(e -> "Guides".equals(e.getSubCategory()) && "Hard wrap guide".equals(e.getName())));
        assertTrue(editorElements.stream().anyMatch(e -> "Sticky Lines".equals(e.getSubCategory()) && "Border".equals(e.getName())));
        assertTrue(editorElements.stream().anyMatch(e -> "Tabs".equals(e.getSubCategory()) && "Modified icon color".equals(e.getName())));
        assertTrue(editorElements.stream().anyMatch(e -> "Tabs".equals(e.getSubCategory()) && "Selected Tab".equals(e.getName())));
        assertTrue(editorElements.stream().anyMatch(e -> "Vertical Scrollbar".equals(e.getSubCategory())));
    }

    @Test
    public void testColorSchemeDefaultPalettesAndAttributes() {
        ColorSchemeElement lineNum = ColorSchemeModel.getElement("code.line_number");
        assertNotNull(lineNum);
        assertEquals("4B5059", lineNum.getDefaultAttr().foreground);
        assertTrue(lineNum.getDefaultAttr().foregroundEnabled);

        ColorSchemeElement breadcrumbCurrent = ColorSchemeModel.getElement("editor.breadcrumbs.current");
        assertNotNull(breadcrumbCurrent);
        assertEquals("DFE1E5", breadcrumbCurrent.getDefaultAttr().foreground);
        assertEquals("2B2D30", breadcrumbCurrent.getDefaultAttr().background);

        ColorSchemeElement modifiedIcon = ColorSchemeModel.getElement("editor.tabs.modified_icon");
        assertNotNull(modifiedIcon);
        assertEquals("4083C9", modifiedIcon.getDefaultAttr().foreground);
        assertTrue(modifiedIcon.getDefaultAttr().foregroundEnabled);

        ColorSchemeElement todo = ColorSchemeModel.getElement("code.todo");
        assertNotNull(todo);
        assertEquals("A8C023", todo.getDefaultAttr().foreground);
        assertTrue(todo.getDefaultAttr().italic);
        assertEquals("73AD2B", todo.getDefaultAttr().errorStripe);

        ColorSchemeElement matchedBrace = ColorSchemeModel.getElement("code.matched_brace");
        assertNotNull(matchedBrace);
        assertEquals("FFEF28", matchedBrace.getDefaultAttr().effectColor);
        assertEquals("Bold Underscored", matchedBrace.getDefaultAttr().effectType);
    }

    @Test
    public void testColorSchemeInheritanceResolution() {
        ColorSchemeElement stickyBorder = ColorSchemeModel.getElement("editor.sticky_lines.border");
        assertNotNull(stickyBorder);
        assertTrue(stickyBorder.hasInheritance());
        assertEquals("editor.guides.hard_wrap", stickyBorder.getInheritFromKey());
        assertEquals("Editor → Guides → Hard wrap guide (General)", stickyBorder.getInheritFromDisplay());

        // Default inheritance resolves to parent's default attributes
        ColorSchemeAttribute resolvedDefault = ColorSchemeModel.resolveAttribute("Dark Theme default", "editor.sticky_lines.border", null);
        assertNotNull(resolvedDefault);
        ColorSchemeElement hardWrap = ColorSchemeModel.getElement("editor.guides.hard_wrap");
        assertEquals(hardWrap.getDefaultAttr().foreground, resolvedDefault.foreground);

        // Custom override with inherit=false
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeMap = new LinkedHashMap<>();
        ColorSchemeAttribute customAttr = new ColorSchemeAttribute(
                true, false, "FF00FF", true, "112233", true, null, false, null, false, "Underscored", false, null);
        schemeMap.put("editor.sticky_lines.border", customAttr);
        overrides.put("Dark Theme default", schemeMap);

        ColorSchemeAttribute resolvedOverridden = ColorSchemeModel.resolveAttribute("Dark Theme default", "editor.sticky_lines.border", overrides);
        assertEquals("FF00FF", resolvedOverridden.foreground);
        assertEquals("112233", resolvedOverridden.background);
        assertTrue(resolvedOverridden.bold);

        // Parent overridden and child inherits
        Map<String, Map<String, ColorSchemeAttribute>> parentOverrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> parentSchemeMap = new LinkedHashMap<>();
        ColorSchemeAttribute parentAttr = new ColorSchemeAttribute(
                false, false, "00FFEE", true, null, false, null, false, null, false, "Underscored", false, null);
        parentSchemeMap.put("editor.guides.hard_wrap", parentAttr);
        // Child explicitly inherits
        ColorSchemeAttribute childInheritAttr = new ColorSchemeAttribute(
                false, false, null, false, null, false, null, false, null, false, "Underscored", true, "editor.guides.hard_wrap");
        parentSchemeMap.put("editor.sticky_lines.border", childInheritAttr);
        parentOverrides.put("Dark Theme default", parentSchemeMap);

        ColorSchemeAttribute resolvedFromParent = ColorSchemeModel.resolveAttribute("Dark Theme default", "editor.sticky_lines.border", parentOverrides);
        assertEquals("00FFEE", resolvedFromParent.foreground);
    }

    @Test
    public void testColorSchemeSerializationAndRoundTrip() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        AppSettingsStore.Settings original = new AppSettingsStore.Settings();
        original.setEditorColorScheme("Custom Pro Dark");
        original.getCustomColorSchemes().add("Custom Pro Dark");

        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeOverrides = new LinkedHashMap<>();
        schemeOverrides.put("code.line_number", new ColorSchemeAttribute(
                true, false, "123456", true, "654321", true, "ABCDEF", true, "FEDCBA", true, "Dotted line", false, null));
        schemeOverrides.put("preview.folded", new ColorSchemeAttribute(
                false, true, "AABBCC", true, null, false, null, false, null, false, "Bordered", false, null));
        overrides.put("Custom Pro Dark", schemeOverrides);
        original.setColorSchemeOverrides(overrides);

        String json = mapper.writeValueAsString(original);
        assertNotNull(json);
        assertTrue(json.contains("Custom Pro Dark"));
        assertTrue(json.contains("code.line_number"));
        assertTrue(json.contains("123456"));
        assertTrue(json.contains("Dotted line"));

        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertEquals("Custom Pro Dark", loaded.getEditorColorScheme());
        assertTrue(loaded.getCustomColorSchemes().contains("Custom Pro Dark"));
        assertNotNull(loaded.getColorSchemeOverrides().get("Custom Pro Dark"));

        ColorSchemeAttribute loadedLineNum = loaded.getColorSchemeOverrides().get("Custom Pro Dark").get("code.line_number");
        assertNotNull(loadedLineNum);
        assertTrue(loadedLineNum.bold);
        assertEquals("123456", loadedLineNum.foreground);
        assertEquals("654321", loadedLineNum.background);
        assertEquals("Dotted line", loadedLineNum.effectType);
    }

    @Test
    public void testColorSchemeGeneralPanelConstructionAndInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        settings.getCustomColorSchemes().add("Team Light Theme");
        Map<String, Object> inputs = new HashMap<>();

        javafx.scene.layout.VBox panel = SettingsDialog.buildColorSchemeGeneralPanel(settings, inputs, target -> {});

        assertNotNull(panel);
        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("customColorSchemes"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));

        @SuppressWarnings("unchecked")
        javafx.scene.control.ComboBox<String> combo =
                (javafx.scene.control.ComboBox<String>) inputs.get("editorColorSchemeCombo");
        assertNotNull(combo);
        assertTrue(combo.getItems().contains("Dark Theme default"));
        assertTrue(combo.getItems().contains("Team Light Theme"));

        @SuppressWarnings("unchecked")
        List<String> customs = (List<String>) inputs.get("customColorSchemes");
        assertTrue(customs.contains("Team Light Theme"));
    }

    @Test
    public void testColorSchemeBatch2ElementsMatchDataGrip() {
        // 1. Errors and Warnings 15 items in exact DataGrip order
        List<ColorSchemeElement> errorElements = ColorSchemeModel.getElementsByCategory("Errors and Warnings");
        assertNotNull(errorElements);
        assertEquals(15, errorElements.size(), "Should have exactly 15 Errors and Warnings items");

        List<String> expectedErrorNames = List.of(
                "Deprecated symbol",
                "Deprecated symbol marked for removal",
                "Duplicate from server",
                "Error",
                "Grammar error",
                "Problem from server",
                "Runtime problem",
                "Text style error",
                "Text style suggestion",
                "Text style warning",
                "Typo",
                "Unknown symbol",
                "Unused code",
                "Warning",
                "Weak Warning"
        );
        List<String> actualErrorNames = errorElements.stream().map(ColorSchemeElement::getName).toList();
        assertEquals(expectedErrorNames, actualErrorNames);

        // Deprecated symbol marked for removal (coral strikeout #F75464)
        ColorSchemeElement depRem = ColorSchemeModel.getElement("errors.deprecated_marked_for_removal");
        assertNotNull(depRem);
        assertTrue(depRem.getDefaultAttr().effectEnabled);
        assertEquals("F75464", depRem.getDefaultAttr().effectColor);
        assertEquals("Strikeout", depRem.getDefaultAttr().effectType);

        // Deprecated symbol (strikeout #868A91)
        ColorSchemeElement dep = ColorSchemeModel.getElement("errors.deprecated");
        assertNotNull(dep);
        assertTrue(dep.getDefaultAttr().effectEnabled);
        assertEquals("868A91", dep.getDefaultAttr().effectColor);
        assertEquals("Strikeout", dep.getDefaultAttr().effectType);

        // 2. Hyperlinks 4 items
        List<ColorSchemeElement> linkElements = ColorSchemeModel.getElementsByCategory("Hyperlinks");
        assertNotNull(linkElements);
        assertEquals(4, linkElements.size(), "Should have exactly 4 Hyperlinks items");

        List<String> expectedLinkNames = List.of("Followed", "Inactive", "Reference", "Unfollowed");
        List<String> actualLinkNames = linkElements.stream().map(ColorSchemeElement::getName).toList();
        assertEquals(expectedLinkNames, actualLinkNames);

        ColorSchemeElement inactiveLink = ColorSchemeModel.getElement("hyperlinks.inactive");
        assertNotNull(inactiveLink);
        assertTrue(inactiveLink.getDefaultAttr().effectEnabled);
        assertEquals("6B6C73", inactiveLink.getDefaultAttr().effectColor);
        assertEquals("Underscored", inactiveLink.getDefaultAttr().effectType);

        ColorSchemeElement refLink = ColorSchemeModel.getElement("hyperlinks.reference");
        assertNotNull(refLink);
        assertEquals("589DF6", refLink.getDefaultAttr().foreground);
        assertEquals("589DF6", refLink.getDefaultAttr().effectColor);

        // 3. Editor > Vertical Scrollbar
        ColorSchemeElement thumb = ColorSchemeModel.getElement("editor.scrollbar.thumb");
        assertNotNull(thumb);
        assertEquals("4E5157", thumb.getDefaultAttr().background);

        ColorSchemeElement thumbScrolling = ColorSchemeModel.getElement("editor.scrollbar.thumb_scrolling");
        assertNotNull(thumbScrolling);
        assertEquals("FFFFFF", thumbScrolling.getDefaultAttr().background);

        // 4. Editor > Guides
        ColorSchemeElement indent = ColorSchemeModel.getElement("editor.guides.indent");
        assertNotNull(indent);
        assertEquals("313438", indent.getDefaultAttr().background);

        ColorSchemeElement indentSel = ColorSchemeModel.getElement("editor.guides.indent_selected");
        assertNotNull(indentSel);
        assertEquals("4B5059", indentSel.getDefaultAttr().background);

        ColorSchemeElement braceGuide = ColorSchemeModel.getElement("editor.guides.matched_brace");
        assertNotNull(braceGuide);
        assertEquals("3B514D", braceGuide.getDefaultAttr().background);

        ColorSchemeElement visualGuide = ColorSchemeModel.getElement("editor.guides.visual");
        assertNotNull(visualGuide);
        assertEquals("323232", visualGuide.getDefaultAttr().foreground);

        // 5. Identifiers > Reassigned local variable
        ColorSchemeElement reassigned = ColorSchemeModel.getElement("identifiers.reassigned_local_variable");
        assertNotNull(reassigned);
        assertEquals("Identifiers", reassigned.getCategory());
        assertEquals("Reassigned local variable", reassigned.getName());
        assertEquals("Bordered", reassigned.getDefaultAttr().effectType);

        // 6. Compatibility aliases
        assertEquals(depRem, ColorSchemeModel.getElement("errors.marked_for_removal"));
        assertEquals(ColorSchemeModel.getElement("hyperlinks.unfollowed"), ColorSchemeModel.getElement("hyperlink.link"));
        assertEquals(inactiveLink, ColorSchemeModel.getElement("hyperlink.inactive"));
        assertEquals(thumbScrolling, ColorSchemeModel.getElement("editor.scrollbar.track"));
    }

    @Test
    public void testColorSchemeBatch3ElementsMatchDataGrip() {
        // 1. Line Coverage: Full, Partial, Uncovered
        List<ColorSchemeElement> covList = ColorSchemeModel.getElementsByCategory("Line Coverage");
        assertNotNull(covList);
        assertEquals(3, covList.size());
        assertEquals(List.of("Full", "Partial", "Uncovered"), covList.stream().map(ColorSchemeElement::getName).toList());

        ColorSchemeElement partial = ColorSchemeModel.getElement("coverage.partial");
        assertNotNull(partial);
        assertTrue(partial.getDefaultAttr().bold);
        assertEquals("5E4D33", partial.getDefaultAttr().foreground);
        assertTrue(partial.getDefaultAttr().foregroundEnabled);
        assertEquals("Bordered", partial.getDefaultAttr().effectType);

        // 2. Live Templates: Active Segment, Inactive Segment, Template Variable
        List<ColorSchemeElement> tmplList = ColorSchemeModel.getElementsByCategory("Live Templates");
        assertNotNull(tmplList);
        assertEquals(3, tmplList.size());
        assertEquals(List.of("Active Segment", "Inactive Segment", "Template Variable"), tmplList.stream().map(ColorSchemeElement::getName).toList());

        ColorSchemeElement inactiveSegment = ColorSchemeModel.getElement("templates.inactive");
        assertNotNull(inactiveSegment);
        assertTrue(inactiveSegment.getDefaultAttr().effectEnabled);
        assertEquals("9DA0A8", inactiveSegment.getDefaultAttr().effectColor);
        assertEquals("Bordered", inactiveSegment.getDefaultAttr().effectType);

        // 3. Search Results: Search result (write access)
        ColorSchemeElement writeAccess = ColorSchemeModel.getElement("search.result_write");
        assertNotNull(writeAccess);
        assertEquals("Search result (write access)", writeAccess.getName());
        assertTrue(writeAccess.getDefaultAttr().backgroundEnabled);
        assertEquals("66313F", writeAccess.getDefaultAttr().background);
        assertTrue(writeAccess.getDefaultAttr().errorStripeEnabled);
        assertEquals("FA7DB1", writeAccess.getDefaultAttr().errorStripe);

        // 4. Popups and Hints: all 11 items in exact DataGrip alphabetical order
        List<ColorSchemeElement> popupsList = ColorSchemeModel.getElementsByCategory("Popups and Hints");
        assertNotNull(popupsList);
        assertEquals(11, popupsList.size());
        List<String> expectedPopups = List.of(
                "Code lens",
                "Completion",
                "Documentation",
                "Error hint",
                "Hint border",
                "Information hint",
                "Promotion pane",
                "Question hint",
                "Recent locations selection",
                "Tooltip",
                "Warning hint"
        );
        assertEquals(expectedPopups, popupsList.stream().map(ColorSchemeElement::getName).toList());

        ColorSchemeElement completion = ColorSchemeModel.getElement("popups.completion");
        assertNotNull(completion);
        assertTrue(completion.getDefaultAttr().backgroundEnabled);
        assertEquals("2B2D30", completion.getDefaultAttr().background);

        // 5. Preview: exactly 2 items Background and Border, with Border inheriting from Indent guide
        List<ColorSchemeElement> previewList = ColorSchemeModel.getElementsByCategory("Preview");
        assertNotNull(previewList);
        assertEquals(2, previewList.size());
        assertEquals(List.of("Background", "Border"), previewList.stream().map(ColorSchemeElement::getName).toList());

        ColorSchemeElement previewBorder = ColorSchemeModel.getElement("preview.border");
        assertNotNull(previewBorder);
        assertTrue(previewBorder.hasInheritance());
        assertEquals("editor.guides.indent", previewBorder.getInheritFromKey());
        assertEquals("Editor → Guides → Indent guide (General)", previewBorder.getInheritFromDisplay());

        // Resolving preview.border inherits Indent guide's background 313438
        ColorSchemeAttribute resolvedBorder = ColorSchemeModel.resolveAttribute("Dark Theme default", "preview.border", null);
        assertNotNull(resolvedBorder);
        assertTrue(resolvedBorder.backgroundEnabled);
        assertEquals("313438", resolvedBorder.background);

        // 6. Text: all 9 items in exact DataGrip alphabetical order
        List<ColorSchemeElement> textList = ColorSchemeModel.getElementsByCategory("Text");
        assertNotNull(textList);
        assertEquals(9, textList.size());
        List<String> expectedTextItems = List.of(
                "Background in read-only files",
                "Default text",
                "Deleted text",
                "Folded text",
                "Folded text with highlighting",
                "Read-only fragment background",
                "Soft wrap sign",
                "Tabs",
                "Whitespaces"
        );
        assertEquals(expectedTextItems, textList.stream().map(ColorSchemeElement::getName).toList());

        ColorSchemeElement foldedHighlighted = ColorSchemeModel.getElement("text.folded_highlighted");
        assertNotNull(foldedHighlighted);
        assertTrue(foldedHighlighted.getDefaultAttr().backgroundEnabled);
        assertEquals("2B2D30", foldedHighlighted.getDefaultAttr().background);

        ColorSchemeElement readonlyBg = ColorSchemeModel.getElement("text.readonly_bg");
        assertNotNull(readonlyBg);
        assertTrue(readonlyBg.getDefaultAttr().backgroundEnabled);
        assertEquals("2B2D30", readonlyBg.getDefaultAttr().background);

        // 7. Aliases and backward compatibility
        assertEquals(ColorSchemeModel.getElement("text.folded"), ColorSchemeModel.getElement("preview.folded"));
        assertEquals(ColorSchemeModel.getElement("text.folded_highlighted"), ColorSchemeModel.getElement("preview.folded_highlighted"));
        assertEquals(ColorSchemeModel.getElement("text.deleted"), ColorSchemeModel.getElement("preview.deleted"));
        assertEquals(readonlyBg, ColorSchemeModel.getElement("text.background_readonly"));
        assertEquals(writeAccess, ColorSchemeModel.getElement("search.result_write_access"));
        assertEquals(ColorSchemeModel.getElement("coverage.full"), ColorSchemeModel.getElement("coverage.full_coverage"));
        assertEquals(partial, ColorSchemeModel.getElement("coverage.partial_coverage"));
    }

    @Test
    public void testLanguageDefaultsCategoriesAndElementsMatchDataGrip() {
        List<String> categories = ColorSchemeModel.getLanguageDefaultCategories();
        assertNotNull(categories);
        assertEquals(13, categories.size(), "Should have exactly 13 top-level language default categories");

        List<String> expectedCategories = List.of(
                "Bad character",
                "Braces and Operators",
                "Classes",
                "Comments",
                "Identifiers",
                "Inline hints",
                "Keyword",
                "Markup",
                "Metadata",
                "Number",
                "Semantic highlighting",
                "String",
                "Template language"
        );
        assertEquals(expectedCategories, categories);

        // Check Bad character leaf
        ColorSchemeElement badChar = ColorSchemeModel.getElement("lang.bad_character");
        assertNotNull(badChar);
        assertEquals("Bad character", badChar.getName());
        assertEquals("F75464", badChar.getDefaultAttr().foreground);
        assertTrue(badChar.getDefaultAttr().foregroundEnabled);
        assertEquals("Underwaved", badChar.getDefaultAttr().effectType);

        // Check Braces and Operators
        ColorSchemeElement brackets = ColorSchemeModel.getElement("lang.braces_and_operators.brackets");
        assertNotNull(brackets);
        assertEquals("Brackets", brackets.getName());
        assertEquals("BCBEC4", brackets.getDefaultAttr().foreground);
        assertTrue(brackets.getDefaultAttr().foregroundEnabled);

        // Check Classes: Instance field
        ColorSchemeElement instField = ColorSchemeModel.getElement("lang.classes.instance_field");
        assertNotNull(instField);
        assertEquals("Instance field", instField.getName());
        assertEquals("C77DBB", instField.getDefaultAttr().foreground);
        assertTrue(instField.hasInheritance());
        assertEquals("lang.identifiers.default", instField.getInheritFromKey());
        assertEquals("Identifiers → Default (Language Defaults)", instField.getInheritFromDisplay());

        // Check Comments: Doc comment > Link in rendered view
        ColorSchemeElement docLink = ColorSchemeModel.getElement("lang.comments.doc.link");
        assertNotNull(docLink);
        assertEquals("Link in rendered view", docLink.getName());
        assertEquals("3887A1", docLink.getDefaultAttr().foreground);
        assertTrue(docLink.getDefaultAttr().foregroundEnabled);
        assertEquals(List.of("Comments", "Doc comment", "Link in rendered view"), docLink.getCategoryPath());

        // Check Identifiers: Default inheriting from General
        ColorSchemeElement idDefault = ColorSchemeModel.getElement("lang.identifiers.default");
        assertNotNull(idDefault);
        assertEquals("Default", idDefault.getName());
        assertEquals("BCBEC4", idDefault.getDefaultAttr().foreground);
        assertTrue(idDefault.hasInheritance());
        assertEquals("text.default", idDefault.getInheritFromKey());
        assertEquals("Text → Default text (General)", idDefault.getInheritFromDisplay());

        // Check Keyword leaf
        ColorSchemeElement keyword = ColorSchemeModel.getElement("lang.keyword");
        assertNotNull(keyword);
        assertEquals("Keyword", keyword.getName());
        assertEquals("CF8E6D", keyword.getDefaultAttr().foreground);

        // Check Number leaf
        ColorSchemeElement number = ColorSchemeModel.getElement("lang.number");
        assertNotNull(number);
        assertEquals("Number", number.getName());
        assertEquals("2AACB8", number.getDefaultAttr().foreground);

        // Check String escape sequences
        ColorSchemeElement escInvalid = ColorSchemeModel.getElement("lang.string.escape.invalid");
        assertNotNull(escInvalid);
        assertEquals("Invalid", escInvalid.getName());
        assertEquals("Underwaved", escInvalid.getDefaultAttr().effectType);
        assertEquals("F75464", escInvalid.getDefaultAttr().effectColor);
        assertTrue(escInvalid.getDefaultAttr().effectEnabled);
    }

    @Test
    public void testLanguageDefaultsCrossPageInheritance() {
        // Base case: lang.classes.instance_field inherits from lang.identifiers.default
        // which in turn inherits from text.default in General
        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();

        ColorSchemeAttribute resolvedBase = ColorSchemeModel.resolveAttribute(
                "Dark Theme default", "lang.classes.instance_field", overrides);
        assertNotNull(resolvedBase);
        // By default, text.default has foreground A9B7C6 and background 1E1F22
        // lang.identifiers.default inherits from text.default
        // lang.classes.instance_field inherits from lang.identifiers.default
        assertEquals("A9B7C6", resolvedBase.foreground);
        assertEquals("1E1F22", resolvedBase.background);

        // When General's text.default is overridden, both child elements inherit the change
        Map<String, ColorSchemeAttribute> schemeOverrides = new LinkedHashMap<>();
        schemeOverrides.put("text.default", new ColorSchemeAttribute(
                false, false, "E0E0E0", true, "101010", true, null, false, null, false, "Underscored", false, null));
        overrides.put("Dark Theme default", schemeOverrides);

        ColorSchemeAttribute resolvedFromGeneral = ColorSchemeModel.resolveAttribute(
                "Dark Theme default", "lang.classes.instance_field", overrides);
        assertEquals("E0E0E0", resolvedFromGeneral.foreground);
        assertEquals("101010", resolvedFromGeneral.background);

        // When lang.identifiers.default has an override that does NOT inherit:
        schemeOverrides.put("lang.identifiers.default", new ColorSchemeAttribute(
                true, false, "FFD700", true, null, false, null, false, null, false, "Bordered", false, null));

        ColorSchemeAttribute resolvedFromIdDefault = ColorSchemeModel.resolveAttribute(
                "Dark Theme default", "lang.classes.instance_field", overrides);
        assertTrue(resolvedFromIdDefault.bold);
        assertEquals("FFD700", resolvedFromIdDefault.foreground);

        // When lang.classes.instance_field itself has an explicit override:
        schemeOverrides.put("lang.classes.instance_field", new ColorSchemeAttribute(
                false, true, "C77DBB", true, null, false, null, false, null, false, "Bordered", false, null));

        ColorSchemeAttribute resolvedDirect = ColorSchemeModel.resolveAttribute(
                "Dark Theme default", "lang.classes.instance_field", overrides);
        assertFalse(resolvedDirect.bold);
        assertTrue(resolvedDirect.italic);
        assertEquals("C77DBB", resolvedDirect.foreground);
    }

    @Test
    public void testColorSchemeLanguageDefaultsPanelConstruction() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new LinkedHashMap<>();

        javafx.scene.layout.VBox panel = SettingsDialog.buildColorSchemeLanguageDefaultsPanel(settings, inputs, target -> {});
        assertNotNull(panel);

        assertTrue(inputs.containsKey("editorColorSchemeCombo"));
        assertTrue(inputs.containsKey("colorSchemeOverrides"));
        assertTrue(inputs.containsKey("customColorSchemes"));

        @SuppressWarnings("unchecked")
        javafx.scene.control.ComboBox<String> combo =
                (javafx.scene.control.ComboBox<String>) inputs.get("editorColorSchemeCombo");
        assertNotNull(combo);
        assertEquals("Dark Theme default", combo.getValue());

        @SuppressWarnings("unchecked")
        Map<String, Map<String, ColorSchemeAttribute>> overrides =
                (Map<String, Map<String, ColorSchemeAttribute>>) inputs.get("colorSchemeOverrides");
        assertNotNull(overrides);
    }

    @Test
    public void testLanguageDefaultsOverridesPersistence() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        AppSettingsStore.Settings original = new AppSettingsStore.Settings();
        original.setEditorColorScheme("Dark Theme default");

        Map<String, Map<String, ColorSchemeAttribute>> overrides = new LinkedHashMap<>();
        Map<String, ColorSchemeAttribute> schemeOverrides = new LinkedHashMap<>();
        schemeOverrides.put("lang.bad_character", new ColorSchemeAttribute(
                false, false, "FF0000", true, null, false, "FF0000", true, null, false, "Underwaved", false, null));
        schemeOverrides.put("lang.classes.instance_field", new ColorSchemeAttribute(
                true, true, "D080C0", true, null, false, null, false, null, false, "Bordered", false, null));
        overrides.put("Dark Theme default", schemeOverrides);
        original.setColorSchemeOverrides(overrides);

        String json = mapper.writeValueAsString(original);
        assertNotNull(json);
        assertTrue(json.contains("lang.bad_character"));
        assertTrue(json.contains("lang.classes.instance_field"));
        assertTrue(json.contains("FF0000"));
        assertTrue(json.contains("D080C0"));

        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertNotNull(loaded);
        assertNotNull(loaded.getColorSchemeOverrides().get("Dark Theme default"));
        ColorSchemeAttribute loadedBadChar = loaded.getColorSchemeOverrides().get("Dark Theme default").get("lang.bad_character");
        assertNotNull(loadedBadChar);
        assertEquals("FF0000", loadedBadChar.foreground);

        ColorSchemeAttribute loadedInstField = loaded.getColorSchemeOverrides().get("Dark Theme default").get("lang.classes.instance_field");
        assertNotNull(loadedInstField);
        assertTrue(loadedInstField.bold);
        assertTrue(loadedInstField.italic);
        assertEquals("D080C0", loadedInstField.foreground);
    }
}

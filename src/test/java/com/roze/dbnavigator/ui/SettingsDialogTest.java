package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
        assertEquals(13, genChildren.size(), "General should have 13 children");
        assertEquals("Auto Import", genChildren.get(0));
        assertEquals("Appearance", genChildren.get(1));
        assertEquals("Breadcrumbs", genChildren.get(2));
        assertEquals("Code Completion", genChildren.get(3));
        assertEquals("Code Folding", genChildren.get(4));
        assertEquals("Editor Tabs", genChildren.get(5));
        assertEquals("Gutter Icons", genChildren.get(6));
        assertEquals("Output Console", genChildren.get(7));
        assertEquals("Postfix Completion", genChildren.get(8));
        assertEquals("Smart Keys", genChildren.get(9));
        assertEquals("Sticky Lines", genChildren.get(10));
        assertEquals("Code Editing", genChildren.get(11));
        assertEquals("Font", genChildren.get(12));

        // Subtree under Code Completion
        List<String> ccChildren = SettingsDialog.getChildCategoryNames("Editor / General / Code Completion");
        assertEquals(2, ccChildren.size());
        assertEquals("Popup", ccChildren.get(0));
        assertEquals("Inline", ccChildren.get(1));

        // Subtree under Smart Keys
        List<String> skChildren = SettingsDialog.getChildCategoryNames("Editor / General / Smart Keys");
        assertEquals(5, skChildren.size());
        assertEquals("YAML", skChildren.get(0));
        assertEquals("JSON", skChildren.get(1));
        assertEquals("Markdown", skChildren.get(2));
        assertEquals("HTML/CSS", skChildren.get(3));
        assertEquals("SQL", skChildren.get(4));
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
        assertEquals("Islands Dark Theme default", settings.getEditorColorScheme());
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
        assertTrue(schemes.contains("Islands Dark Theme default"));
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
}

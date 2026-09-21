package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.ConnectionStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.util.CsvFormatEngine;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Popup;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DataGrip-aligned Settings dialog:
 * - 10 root categories: Database, Appearance & Behavior, Keymap, Editor, Plugins,
 *   Version Control, Languages, Tools, Backup and Sync, Advanced Settings.
 * - Dynamic category tree with search filter field.
 * - Category landing views with header titles, DataGrip descriptions, and interactive blue links.
 * - Top navigation history (Back / Forward) and Pin toggle.
 * - Bottom Help (?) button and OK / Cancel / Apply action buttons.
 * - Backing persistence into AppSettingsStore.
 */
public final class SettingsDialog {

    private SettingsDialog() {}

    public static class CategoryDef {
        private final String id;
        private final String name;
        private final String path;
        private final String description;
        private final List<CategoryDef> children = new ArrayList<>();

        public CategoryDef(String id, String name, String path, String description) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPath() { return path; }
        public String getDescription() { return description; }
        public List<CategoryDef> getChildren() { return children; }
    }

    private static final List<CategoryDef> CATEGORIES = buildCategoryDefinitions();

    public static final Set<String> PROJECT_LEVEL_CATEGORIES = Set.of(
            "Database Explorer",
            "SQL Dialects",
            "SQL Resolution Scopes",
            "Version Control",
            "Build Tools",
            "Actions on Save",
            "Coverage",
            "SSH Configurations",
            "Terminal"
    );

    public static HBox buildTitledSectionLine(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #dfe1e5;");
        Separator sep = new Separator();
        HBox.setHgrow(sep, Priority.ALWAYS);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-opacity: 0.35;");
        HBox line = new HBox(8, label, sep);
        line.setAlignment(Pos.CENTER_LEFT);
        line.setPadding(new Insets(8, 0, 4, 0));
        return line;
    }

    public static Label createHelpTooltip(String text) {
        Label helpIcon = new Label("(?)");
        helpIcon.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-cursor: hand;");
        helpIcon.setTooltip(new Tooltip(text));
        return helpIcon;
    }

    public static List<CategoryDef> getCategoryDefinitions() {
        return Collections.unmodifiableList(CATEGORIES);
    }

    public static List<String> getRootCategoryNames() {
        List<String> names = new ArrayList<>();
        for (CategoryDef cat : CATEGORIES) {
            names.add(cat.getName());
        }
        return names;
    }

    public static List<String> getChildCategoryNames(String parentPath) {
        CategoryDef parent = findCategoryByPath(parentPath, CATEGORIES);
        if (parent == null) {
            parent = findCategoryByName(parentPath, CATEGORIES);
        }
        if (parent == null) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (CategoryDef child : parent.getChildren()) {
            names.add(child.getName());
        }
        return names;
    }

    public static String getCategoryDescription(String nameOrPath) {
        CategoryDef cat = findCategoryByPath(nameOrPath, CATEGORIES);
        if (cat == null) cat = findCategoryByName(nameOrPath, CATEGORIES);
        return cat != null ? cat.getDescription() : "";
    }

    private static CategoryDef findCategoryByPath(String path, List<CategoryDef> list) {
        for (CategoryDef cat : list) {
            if (cat.getPath().equalsIgnoreCase(path)) return cat;
            CategoryDef sub = findCategoryByPath(path, cat.getChildren());
            if (sub != null) return sub;
        }
        return null;
    }

    private static CategoryDef findCategoryByName(String name, List<CategoryDef> list) {
        for (CategoryDef cat : list) {
            if (cat.getName().equalsIgnoreCase(name)) return cat;
            CategoryDef sub = findCategoryByName(name, cat.getChildren());
            if (sub != null) return sub;
        }
        return null;
    }

    public static List<String> searchCategories(String query) {
        if (query == null || query.isBlank()) {
            return getRootCategoryNames();
        }
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<String> results = new ArrayList<>();
        collectSearchResults(CATEGORIES, q, results);
        return results;
    }

    private static void collectSearchResults(List<CategoryDef> list, String query, List<String> results) {
        for (CategoryDef cat : list) {
            if (cat.getName().toLowerCase(Locale.ROOT).contains(query)
                    || cat.getPath().toLowerCase(Locale.ROOT).contains(query)
                    || (cat.getDescription() != null && cat.getDescription().toLowerCase(Locale.ROOT).contains(query))) {
                results.add(cat.getPath());
            }
            collectSearchResults(cat.getChildren(), query, results);
        }
    }

    public static TreeItem<String> buildCategoryHierarchy() {
        TreeItem<String> root = new TreeItem<>("root");
        for (CategoryDef cat : CATEGORIES) {
            root.getChildren().add(buildTreeItem(cat));
        }
        return root;
    }

    private static TreeItem<String> buildTreeItem(CategoryDef cat) {
        TreeItem<String> item = new TreeItem<>(cat.getName());
        if ("Database".equals(cat.getName()) || "Query Execution".equals(cat.getName())) {
            item.setExpanded(true);
        }
        for (CategoryDef child : cat.getChildren()) {
            item.getChildren().add(buildTreeItem(child));
        }
        return item;
    }

    private static List<CategoryDef> buildCategoryDefinitions() {
        List<CategoryDef> roots = new ArrayList<>();

        // 1. Database
        CategoryDef database = new CategoryDef("db", "Database", "Database",
                "Specify database console behavior, configure data views, and extraction options. "
                        + "Add custom parameter patterns in SQL queries and specify SQL dialects mapping for files.");
        
        CategoryDef qExec = new CategoryDef("db.query_exec", "Query Execution", "Database / Query Execution",
                "Configure query execution parameters, timeouts, and transaction isolation.");
        qExec.getChildren().add(new CategoryDef("db.output_results", "Output and Results", "Database / Query Execution / Output and Results",
                "Configure result grid presentation, scroll behavior, and console output limits."));
        qExec.getChildren().add(new CategoryDef("db.user_parameters", "User Parameters", "Database / Query Execution / User Parameters",
                "Define custom parameter patterns (:param, $1) to prompt for parameter values when executing SQL queries."));
        database.getChildren().add(qExec);

        database.getChildren().add(new CategoryDef("db.data_editor", "Data Editor and Viewer", "Database / Data Editor and Viewer",
                "Configure table row limits, binary data formatting, and cell editing behaviors."));
        database.getChildren().add(new CategoryDef("db.explorer", "Database Explorer", "Database / Database Explorer",
                "Customize schema tree display, group data sources, and auto-sync behaviors."));
        database.getChildren().add(new CategoryDef("db.csv_formats", "CSV Formats", "Database / CSV Formats",
                "Configure data extraction and import CSV/TSV delimiters, quote characters, and headers."));
        database.getChildren().add(new CategoryDef("db.ai_tools", "AI Tools", "Database / AI Tools",
                "Configure AI database assistant, query explanations, and schema generation contexts."));
        database.getChildren().add(new CategoryDef("db.query_files", "Query Files and Consoles", "Database / Query Files and Consoles",
                "Manage default console bindings, file extensions, and scratch buffers."));
        database.getChildren().add(new CategoryDef("db.sql_dialects", "SQL Dialects", "Database / SQL Dialects",
                "Specify dialect mappings for project files (PostgreSQL, MySQL, SQLite, Generic SQL)."));
        database.getChildren().add(new CategoryDef("db.sql_resolution", "SQL Resolution Scopes", "Database / SQL Resolution Scopes",
                "Define schema resolution scopes for unqualified table and routine references."));
        database.getChildren().add(new CategoryDef("db.other", "Other", "Database / Other",
                "Miscellaneous database options and diagnostic tools."));
        roots.add(database);

        // 2. Appearance & Behavior
        CategoryDef appearanceBehavior = new CategoryDef("app_behavior", "Appearance & Behavior", "Appearance & Behavior",
                "Customize IDE appearance and behavior: change themes and font size, tune the keymap, "
                        + "configure plugins and system settings, such as password policies, HTTP proxy, and updates.");
        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.appearance", "Appearance", "Appearance & Behavior / Appearance",
                "Configure UI themes, fonts, accessibility, and window decorations."));
        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.menus_toolbars", "Menus and Toolbars", "Appearance & Behavior / Menus and Toolbars",
                "Customize main menu bar items, toolbar actions, and context menus."));

        CategoryDef systemSettings = new CategoryDef("app_behavior.system_settings", "System Settings", "Appearance & Behavior / System Settings",
                "System settings including password retention, proxy, update checks, and date formats.");
        systemSettings.getChildren().add(new CategoryDef("sys.data_sharing", "Data Sharing", "Appearance & Behavior / System Settings / Data Sharing",
                "Manage anonymous feature usage statistics."));
        systemSettings.getChildren().add(new CategoryDef("sys.date_formats", "Date Formats", "Appearance & Behavior / System Settings / Date Formats",
                "Configure localized date, time, and timestamp format displays."));
        systemSettings.getChildren().add(new CategoryDef("sys.http_proxy", "HTTP Proxy", "Appearance & Behavior / System Settings / HTTP Proxy",
                "Configure HTTP and SOCKS proxy settings for network operations."));
        systemSettings.getChildren().add(new CategoryDef("sys.lang_region", "Language and Region", "Appearance & Behavior / System Settings / Language and Region",
                "Configure locale, language preferences, and decimal separators."));
        systemSettings.getChildren().add(new CategoryDef("sys.passwords", "Passwords", "Appearance & Behavior / System Settings / Passwords",
                "Manage credential storage policies and master password settings."));
        systemSettings.getChildren().add(new CategoryDef("sys.certificates", "Server Certificates", "Appearance & Behavior / System Settings / Server Certificates",
                "Manage trusted SSL/TLS certificates and certificate authorities."));
        systemSettings.getChildren().add(new CategoryDef("sys.updates", "Updates", "Appearance & Behavior / System Settings / Updates",
                "Configure automatic update checks, release channels, and update endpoints."));
        appearanceBehavior.getChildren().add(systemSettings);

        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.file_colors", "File Colors", "Appearance & Behavior / File Colors",
                "Assign distinct highlight colors to scopes and directory roots."));
        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.scopes", "Scopes", "Appearance & Behavior / Scopes",
                "Define custom file and schema scopes for searches and inspections."));
        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.notifications", "Notifications", "Appearance & Behavior / Notifications",
                "Configure notification popups, sounds, and balloon alerts."));
        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.data_editor", "Data Editor and Viewer", "Appearance & Behavior / Data Editor and Viewer",
                "UI-level preferences for data grids and result viewers."));
        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.quick_lists", "Quick Lists", "Appearance & Behavior / Quick Lists",
                "Create custom popups containing sets of favorite actions."));
        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.required_plugins", "Required Plugins", "Appearance & Behavior / Required Plugins",
                "Specify required extensions for team workspaces."));
        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.trusted_locations", "Trusted Locations", "Appearance & Behavior / Trusted Locations",
                "Configure directories allowed for safe automated script execution."));
        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.path_variables", "Path Variables", "Appearance & Behavior / Path Variables",
                "Define path variables for portable project files."));
        appearanceBehavior.getChildren().add(new CategoryDef("app_behavior.pres_assistant", "Presentation Assistant", "Appearance & Behavior / Presentation Assistant",
                "Display action names and keyboard shortcuts on screen during presentations."));
        roots.add(appearanceBehavior);

        // 3. Keymap
        roots.add(new CategoryDef("keymap", "Keymap", "Keymap",
                "Configure keyboard shortcuts and select keymap presets (DataGrip Default, Windows, macOS, Eclipse)."));

        // 4. Editor
        CategoryDef editor = new CategoryDef("editor", "Editor", "Editor",
                "Personalize source code appearance by changing fonts, highlighting styles, indents, etc. "
                        + "Customize the Editor from line numbers, caret placement and tabs to source code inspections, setting up templates and file encodings.");

        CategoryDef general = new CategoryDef("editor.general", "General", "Editor / General",
                "Configure editor mouse control, scrolling, caret placement, and code folding.");
        general.getChildren().add(new CategoryDef("editor.general.auto_import", "Auto Import", "Editor / General / Auto Import",
                "Configure auto-import behavior for schemas, tables, and namespaces."));
        general.getChildren().add(new CategoryDef("editor.general.appearance", "Appearance", "Editor / General / Appearance",
                "Configure code appearance, line numbers, indent guides, and breadcrumbs."));
        general.getChildren().add(new CategoryDef("editor.general.breadcrumbs", "Breadcrumbs", "Editor / General / Breadcrumbs",
                "Configure breadcrumb placement at top or bottom of the editor."));

        CategoryDef codeCompletion = new CategoryDef("editor.general.code_completion", "Code Completion", "Editor / General / Code Completion",
                "Provides code suggestions while typing, displayed either in a popup or inline in the editor");
        codeCompletion.getChildren().add(new CategoryDef("editor.general.code_completion.popup", "Popup", "Editor / General / Code Completion / Popup",
                "Configure completion popup triggers, delay, and sorting."));
        codeCompletion.getChildren().add(new CategoryDef("editor.general.code_completion.inline", "Inline", "Editor / General / Code Completion / Inline",
                "Configure inline completion suggestions and tab acceptance."));
        general.getChildren().add(codeCompletion);

        general.getChildren().add(new CategoryDef("editor.general.code_folding", "Code Folding", "Editor / General / Code Folding",
                "Configure code folding for statements, comments, and subqueries."));
        general.getChildren().add(new CategoryDef("editor.general.editor_tabs", "Editor Tabs", "Editor / General / Editor Tabs",
                "Configure tab placement, tab closing policy, and multi-row tabs."));
        general.getChildren().add(new CategoryDef("editor.general.gutter_icons", "Gutter Icons", "Editor / General / Gutter Icons",
                "Configure run, breakpoint, and line-marker icons in the left gutter."));
        general.getChildren().add(new CategoryDef("editor.general.output_console", "Output Console", "Editor / General / Output Console",
                "Configure console buffer size, folding, and cyclic buffer limits."));
        general.getChildren().add(new CategoryDef("editor.general.postfix_completion", "Postfix Completion", "Editor / General / Postfix Completion",
                "Configure postfix completion templates and expansions."));

        CategoryDef smartKeys = new CategoryDef("editor.general.smart_keys", "Smart Keys", "Editor / General / Smart Keys",
                "Configure smart typing, auto-closing quotes/brackets, and indent behavior.");
        smartKeys.getChildren().add(new CategoryDef("editor.general.smart_keys.yaml", "YAML", "Editor / General / Smart Keys / YAML",
                "Configure smart indentation and key handling for YAML files."));
        smartKeys.getChildren().add(new CategoryDef("editor.general.smart_keys.json", "JSON", "Editor / General / Smart Keys / JSON",
                "Configure quote escaping and auto-comma in JSON files."));
        smartKeys.getChildren().add(new CategoryDef("editor.general.smart_keys.markdown", "Markdown", "Editor / General / Smart Keys / Markdown",
                "Configure table formatting and link wrapping in Markdown."));
        smartKeys.getChildren().add(new CategoryDef("editor.general.smart_keys.html_css", "HTML/CSS", "Editor / General / Smart Keys / HTML/CSS",
                "Configure tag auto-closing and attribute completion."));
        smartKeys.getChildren().add(new CategoryDef("editor.general.smart_keys.sql", "SQL", "Editor / General / Smart Keys / SQL",
                "Configure clause auto-capitalization and alias insertion."));
        general.getChildren().add(smartKeys);

        general.getChildren().add(new CategoryDef("editor.general.sticky_lines", "Sticky Lines", "Editor / General / Sticky Lines",
                "Keep current scope header visible at the top of the editor while scrolling."));
        general.getChildren().add(new CategoryDef("editor.code_editing", "Code Editing", "Editor / General / Code Editing",
                "Configure code completion, quote pairing, and auto-insertion rules."));
        general.getChildren().add(new CategoryDef("editor.font", "Font", "Editor / General / Font",
                "Customize the font family, font size, and line spacing for SQL consoles and editors."));
        editor.getChildren().add(general);

        // Color Scheme (26 subcategories from DataGrip)
        CategoryDef colorScheme = new CategoryDef("editor.color_scheme", "Color Scheme", "Editor / Color Scheme",
                "Configure colors and the font for source code and console output:");
        colorScheme.getChildren().add(new CategoryDef("cs.general", "General", "Editor / Color Scheme / General", "Configure general editor colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.lang_defaults", "Language Defaults", "Editor / Color Scheme / Language Defaults", "Configure default syntax highlight tokens."));
        colorScheme.getChildren().add(new CategoryDef("cs.font", "Color Scheme Font", "Editor / Color Scheme / Color Scheme Font", "Configure primary and fallback fonts for the color scheme."));
        colorScheme.getChildren().add(new CategoryDef("cs.console_font", "Console Font", "Editor / Color Scheme / Console Font", "Configure font for console output."));
        colorScheme.getChildren().add(new CategoryDef("cs.code_review", "Code Review", "Editor / Color Scheme / Code Review", "Colors for diff and code review comments."));
        colorScheme.getChildren().add(new CategoryDef("cs.console_colors", "Console Colors", "Editor / Color Scheme / Console Colors", "ANSI console output colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.debugger", "Debugger", "Editor / Color Scheme / Debugger", "Execution point and breakpoint colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.diff_merge", "Diff & Merge", "Editor / Color Scheme / Diff & Merge", "Colors for inserted, modified, and deleted lines."));
        colorScheme.getChildren().add(new CategoryDef("cs.user_types", "User-Defined File Types", "Editor / Color Scheme / User-Defined File Types", "Custom file type colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.vcs", "VCS", "Editor / Color Scheme / VCS", "Version control file status colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.data_editor", "Data Editor and Viewer", "Editor / Color Scheme / Data Editor and Viewer", "Grid table cell and row colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.database", "Database", "Editor / Color Scheme / Database", "Database object identifier colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.diagrams", "Diagrams", "Editor / Color Scheme / Diagrams", "ER diagram node and link colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.html", "HTML", "Editor / Color Scheme / HTML", "HTML tag and attribute colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.json", "JSON", "Editor / Color Scheme / JSON", "JSON key and value colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.markdown", "Markdown", "Editor / Color Scheme / Markdown", "Markdown header and code colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.mermaid", "Mermaid", "Editor / Color Scheme / Mermaid", "Mermaid node and relationship colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.regexp", "RegExp", "Editor / Color Scheme / RegExp", "Regular expression syntax colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.sql", "SQL", "Editor / Color Scheme / SQL", "SQL keyword, function, and string colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.table_diff", "Table Diff", "Editor / Color Scheme / Table Diff", "Data comparison diff colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.xml", "XML", "Editor / Color Scheme / XML", "XML tag and attribute colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.xpath", "XPath", "Editor / Color Scheme / XPath", "XPath expression colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.xslt", "XSLT", "Editor / Color Scheme / XSLT", "XSLT template colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.yaml", "YAML", "Editor / Color Scheme / YAML", "YAML key and value colors."));
        colorScheme.getChildren().add(new CategoryDef("cs.by_scope", "By Scope", "Editor / Color Scheme / By Scope", "Color highlighting by project scope."));
        colorScheme.getChildren().add(new CategoryDef("cs.images", "Images", "Editor / Color Scheme / Images", "Image preview background grid colors."));
        editor.getChildren().add(colorScheme);

        // Code Style with SQL (12 dialects) and file types
        CategoryDef codeStyle = new CategoryDef("editor.code_style", "Code Style", "Editor / Code Style",
                "Configure code formatting rules, indentation, keyword casing, and line wrapping for SQL.");

        CategoryDef codeStyleSql = new CategoryDef("editor.code_style.sql", "SQL", "Editor / Code Style / SQL",
                "Set of code styles based on SQL.");
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.general", "General", "Editor / Code Style / SQL / General", "General SQL formatting and casing."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.sql2016", "SQL:2016, Generic", "Editor / Code Style / SQL / SQL:2016, Generic", "ANSI SQL:2016 formatting rules."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.derby", "Apache Derby", "Editor / Code Style / SQL / Apache Derby", "Apache Derby SQL code style."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.db2", "Db2", "Editor / Code Style / SQL / Db2", "IBM Db2 code style."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.h2", "H2", "Editor / Code Style / SQL / H2", "H2 Database code style."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.hsqldb", "HSQLDB", "Editor / Code Style / SQL / HSQLDB", "HSQLDB code style."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.mssql", "MS SQL Server, MS Azure", "Editor / Code Style / SQL / MS SQL Server, MS Azure", "Microsoft T-SQL code style."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.mysql", "MySQL, MariaDB", "Editor / Code Style / SQL / MySQL, MariaDB", "MySQL and MariaDB code style."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.oracle", "Oracle", "Editor / Code Style / SQL / Oracle", "Oracle PL/SQL code style."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.postgres", "PostgreSQL, Greenplum, Redshift", "Editor / Code Style / SQL / PostgreSQL, Greenplum, Redshift", "PostgreSQL dialect code style."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.sqlite", "SQLite", "Editor / Code Style / SQL / SQLite", "SQLite SQL code style."));
        codeStyleSql.getChildren().add(new CategoryDef("code_style.sql.sybase", "Sybase ASE", "Editor / Code Style / SQL / Sybase ASE", "SAP Sybase ASE code style."));
        codeStyle.getChildren().add(codeStyleSql);

        codeStyle.getChildren().add(new CategoryDef("code_style.html", "HTML", "Editor / Code Style / HTML", "HTML formatting rules."));
        codeStyle.getChildren().add(new CategoryDef("code_style.json", "JSON", "Editor / Code Style / JSON", "JSON formatting rules."));
        codeStyle.getChildren().add(new CategoryDef("code_style.markdown", "Markdown", "Editor / Code Style / Markdown", "Markdown formatting rules."));
        codeStyle.getChildren().add(new CategoryDef("code_style.mermaid", "Mermaid", "Editor / Code Style / Mermaid", "Mermaid formatting rules."));
        codeStyle.getChildren().add(new CategoryDef("code_style.xml", "XML", "Editor / Code Style / XML", "XML formatting rules."));
        codeStyle.getChildren().add(new CategoryDef("code_style.yaml", "YAML", "Editor / Code Style / YAML", "YAML formatting rules."));
        codeStyle.getChildren().add(new CategoryDef("code_style.other", "Other File Types", "Editor / Code Style / Other File Types", "Fallback code styles."));
        editor.getChildren().add(codeStyle);

        editor.getChildren().add(new CategoryDef("editor.inspections", "Inspections", "Editor / Inspections",
                "Configure SQL inspections, code smell detection, and static analysis warnings."));
        editor.getChildren().add(new CategoryDef("editor.templates", "File and Code Templates", "Editor / File and Code Templates",
                "Create default file headers and new script templates."));
        editor.getChildren().add(new CategoryDef("editor.encodings", "File Encodings", "Editor / File Encodings",
                "Configure default character encoding (UTF-8) for files and consoles."));
        editor.getChildren().add(new CategoryDef("editor.live_templates", "Live Templates", "Editor / Live Templates",
                "Define shorthand snippet abbreviations (e.g. 'sel*' expanding to 'SELECT * FROM')."));
        editor.getChildren().add(new CategoryDef("editor.file_types", "File Types", "Editor / File Types",
                "Associate file extensions (.sql, .ddl, .csv) with editor highlighters."));
        editor.getChildren().add(new CategoryDef("editor.inlay_hints", "Inlay Hints", "Editor / Inlay Hints",
                "Configure inline hints for column names and function parameters."));
        editor.getChildren().add(new CategoryDef("editor.duplicates", "Duplicates", "Editor / Duplicates",
                "Configure duplicate SQL statement and table definition detection."));
        editor.getChildren().add(new CategoryDef("editor.intentions", "Intentions", "Editor / Intentions",
                "Configure quick-fix intentions for SQL statements and schemas."));
        editor.getChildren().add(new CategoryDef("editor.language_injections", "Language Injections", "Editor / Language Injections",
                "Inject SQL syntax highlighting inside string literals."));

        // Natural Languages with Grammar & Style and Spelling
        CategoryDef naturalLanguages = new CategoryDef("editor.natural_languages", "Natural Languages", "Editor / Natural Languages",
                "Configure proofreading and natural language spelling checks.");
        naturalLanguages.getChildren().add(new CategoryDef("nl.grammar_style", "Grammar and Style", "Editor / Natural Languages / Grammar and Style", "Grammar inspection rules."));
        naturalLanguages.getChildren().add(new CategoryDef("nl.spelling", "Spelling", "Editor / Natural Languages / Spelling", "Dictionaries and spelling checks."));
        editor.getChildren().add(naturalLanguages);

        editor.getChildren().add(new CategoryDef("editor.reader_mode", "Reader Mode", "Editor / Reader Mode",
                "Optimize editor presentation for read-only schema definitions and logs."));
        editor.getChildren().add(new CategoryDef("editor.textmate_bundles", "TextMate Bundles", "Editor / TextMate Bundles",
                "Manage syntax highlighting bundles for additional grammar syntaxes."));
        editor.getChildren().add(new CategoryDef("editor.todo", "TODO", "Editor / TODO",
                "Configure patterns to detect TODO and FIXME comment tags in SQL scripts."));
        roots.add(editor);

        // 5. Plugins
        roots.add(new CategoryDef("plugins", "Plugins", "Plugins",
                "Manage installed database drivers, formatting extensions, and IDE capabilities."));

        // 6. Version Control
        CategoryDef vcs = new CategoryDef("vcs", "Version Control", "Version Control",
                "Configure the settings related to version control used in your project");
        vcs.getChildren().add(new CategoryDef("vcs.changelists", "Changelists", "Version Control / Changelists",
                "Manage active change sets, tracked files, and local change list rules."));
        vcs.getChildren().add(new CategoryDef("vcs.commit", "Commit", "Version Control / Commit",
                "Configure commit checks, pre-commit formatting, and commit message history."));
        vcs.getChildren().add(new CategoryDef("vcs.confirmation", "Confirmation", "Version Control / Confirmation",
                "Confirmation dialogues for adding, deleting, and reverting version-controlled files."));
        vcs.getChildren().add(new CategoryDef("vcs.directory_mappings", "Directory Mappings", "Version Control / Directory Mappings",
                "Map project root directories and subdirectories to version control repositories."));
        vcs.getChildren().add(new CategoryDef("vcs.file_status_colors", "File Status Colors", "Version Control / File Status Colors",
                "Customize colors for modified, added, ignored, and untracked files."));
        vcs.getChildren().add(new CategoryDef("vcs.issue_navigation", "Issue Navigation", "Version Control / Issue Navigation",
                "Configure issue tracker regex patterns to link commit messages to bug trackers."));
        vcs.getChildren().add(new CategoryDef("vcs.shelf", "Shelf", "Version Control / Shelf",
                "Configure shelf location, patch storage, and automatic shelve options."));
        vcs.getChildren().add(new CategoryDef("vcs.git", "Git", "Version Control / Git",
                "Configure Git executable path, branch update method, and credential helper."));
        roots.add(vcs);

        // 7. Languages
        CategoryDef languages = new CategoryDef("languages", "Languages", "Languages",
                "Configure settings related to specific frameworks and technologies used in your project");
        languages.getChildren().add(new CategoryDef("lang.markdown", "Markdown", "Languages / Markdown",
                "Configure Markdown preview layout, syntax extensions, and code fence styling."));
        languages.getChildren().add(new CategoryDef("lang.mermaid", "Mermaid", "Languages / Mermaid",
                "Configure Mermaid diagram rendering engine, layout themes, and zoom controls."));

        CategoryDef schemasDtds = new CategoryDef("lang.schemas_dtds", "Schemas and DTDs", "Languages / Schemas and DTDs",
                "Manage XML/JSON schema catalogs, DTD declarations, and URI mappings.");
        schemasDtds.getChildren().add(new CategoryDef("schemas.xml_default", "Default XML Schemas", "Languages / Schemas and DTDs / Default XML Schemas",
                "Default XML schema catalog and namespace definitions."));
        schemasDtds.getChildren().add(new CategoryDef("schemas.json_mappings", "JSON Schema Mappings", "Languages / Schemas and DTDs / JSON Schema Mappings",
                "Map JSON files to schemas for auto-completion and validation."));
        schemasDtds.getChildren().add(new CategoryDef("schemas.remote_json", "Remote JSON Schemas", "Languages / Schemas and DTDs / Remote JSON Schemas",
                "Download and cache schemas from SchemaStore.org and custom URLs."));
        schemasDtds.getChildren().add(new CategoryDef("schemas.xml_catalog", "XML Catalog", "Languages / Schemas and DTDs / XML Catalog",
                "OASIS XML catalog definitions and public identifier mappings."));
        languages.getChildren().add(schemasDtds);

        languages.getChildren().add(new CategoryDef("lang.xslt", "XSLT", "Languages / XSLT",
                "Configure XSLT processor runtimes and template transformation parameters."));
        languages.getChildren().add(new CategoryDef("lang.xslt_associations", "XSLT File Associations", "Languages / XSLT File Associations",
                "Map file patterns to XSLT stylesheets and schema validations."));
        roots.add(languages);

        // 8. Tools
        CategoryDef tools = new CategoryDef("tools", "Tools", "Tools",
                "Configure integration with third-party applications, specify the SSH Terminal connection settings, manage server certificates and tasks, configure diagrams layout, etc.");
        tools.getChildren().add(new CategoryDef("tools.build_tools", "Build Tools", "Tools / Build Tools",
                "Configure build system runners, script executions, and compile triggers."));
        tools.getChildren().add(new CategoryDef("tools.actions_on_save", "Actions on Save", "Tools / Actions on Save",
                "Reformat code, optimize imports, and run script checks automatically on save."));
        tools.getChildren().add(new CategoryDef("tools.coverage", "Coverage", "Tools / Coverage",
                "Configure code coverage runners, suite tracking, and result gutters."));

        CategoryDef debugger = new CategoryDef("tools.debugger", "Debugger", "Tools / Debugger",
                "Configure SQL routine debugging, breakpoints, and value inspection.");
        debugger.getChildren().add(new CategoryDef("tools.debugger.data_views", "Data Views", "Tools / Debugger / Data Views",
                "Configure variables view, memory views, and type renderers."));
        debugger.getChildren().add(new CategoryDef("tools.debugger.stepping", "Stepping", "Tools / Debugger / Stepping",
                "Configure statement stepping filters and skip rules."));
        tools.getChildren().add(debugger);

        tools.getChildren().add(new CategoryDef("tools.diagrams", "Diagrams", "Tools / Diagrams",
                "Configure ER diagram layout engine, relationship link styles, and table nodes."));

        CategoryDef diffMerge = new CategoryDef("tools.diff_merge", "Diff & Merge", "Tools / Diff & Merge",
                "Configure difference viewers, external diff tools, and three-way merge tools.");
        diffMerge.getChildren().add(new CategoryDef("tools.diff_merge.external", "External Diff Tools", "Tools / Diff & Merge / External Diff Tools",
                "Configure third-party diff and merge utility executables."));
        tools.getChildren().add(diffMerge);

        tools.getChildren().add(new CategoryDef("tools.external_tools", "External Tools", "Tools / External Tools",
                "Define custom external tools, command arguments, and macro variables."));
        tools.getChildren().add(new CategoryDef("tools.features_suggester", "Features Suggester", "Tools / Features Suggester",
                "Configure smart tips suggesting IDE productivity features and shortcuts."));
        tools.getChildren().add(new CategoryDef("tools.features_trainer", "Features Trainer", "Tools / Features Trainer",
                "Interactive tutorials for learning database IDE workflows and navigation."));

        CategoryDef mcpServer = new CategoryDef("tools.mcp_server", "MCP Server", "Tools / MCP Server",
                "Configure Model Context Protocol (MCP) server endpoints, tools, and sidecar integration.");
        mcpServer.getChildren().add(new CategoryDef("tools.mcp_server.exposed_tools", "Exposed Tools", "Tools / MCP Server / Exposed Tools",
                "Manage tools and database capabilities exposed over MCP."));
        tools.getChildren().add(mcpServer);

        tools.getChildren().add(new CategoryDef("tools.rsync", "Rsync", "Tools / Rsync",
                "Configure remote synchronization commands, rsync executable path, and options."));
        tools.getChildren().add(new CategoryDef("tools.ssh", "SSH Configurations", "Tools / SSH Configurations",
                "Manage remote host SSH keys, port forwarding tunnels, and authentication."));
        tools.getChildren().add(new CategoryDef("tools.terminal", "Terminal", "Tools / Terminal",
                "Configure embedded terminal shell path, initial environment, and keybindings."));
        tools.getChildren().add(new CategoryDef("tools.browsers", "Web Browsers and Preview", "Tools / Web Browsers and Preview",
                "Manage system web browsers and embedded HTML preview options."));
        tools.getChildren().add(new CategoryDef("tools.xpath", "XPath Viewer", "Tools / XPath Viewer",
                "Configure XPath and XQuery expression evaluators and highlight results."));
        roots.add(tools);

        // 9. Backup and Sync
        roots.add(new CategoryDef("backup_sync", "Backup and Sync", "Backup and Sync",
                "Configure cloud synchronization of IDE settings, keymaps, color schemes, and project configurations."));

        // 10. Advanced Settings
        roots.add(new CategoryDef("advanced_settings", "Advanced Settings", "Advanced Settings",
                "Tune low-level internal IDE parameters, cache sizes, memory limits, and experimental features."));

        return roots;
    }

    // =========================================================================
    // UI Display
    // =========================================================================

    public static void show(MainWindow mainWindow) {
        show(mainWindow, null);
    }

    public static void show(MainWindow mainWindow, String initialCategory) {
        AppSettingsStore.Settings settings = AppSettingsStore.load();
        Stage stage = new Stage();
        Window owner = mainWindow.getOwnerWindow();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        String projectName = (mainWindow != null && mainWindow.getProject() != null && mainWindow.getProject().getName() != null && !mainWindow.getProject().getName().isBlank())
                ? mainWindow.getProject().getName() : "default";
        stage.setTitle("Settings \u2013 " + projectName);
        stage.setMinWidth(860);
        stage.setMinHeight(600);

        // Top Search Bar
        TextField searchField = new TextField();
        searchField.setPromptText("Search settings…");
        searchField.getStyleClass().add("settings-search-field");
        searchField.setPrefWidth(220);

        // Left Category Tree
        TreeItem<String> rootItem = buildCategoryHierarchy();
        TreeView<String> tree = new TreeView<>(rootItem);
        tree.setShowRoot(false);
        tree.setPrefWidth(240);
        VBox.setVgrow(tree, Priority.ALWAYS);

        tree.setCellFactory(tv -> new TreeCell<>() {
            private final Label nameLabel = new Label();
            private final Region spacer = new Region();
            private final Label badge = new Label();
            private final HBox container = new HBox(4, nameLabel, spacer, badge);
            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                container.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    nameLabel.setText(item);
                    nameLabel.setStyle("-fx-text-fill: inherit;");
                    if ("Plugins".equals(item)) {
                        badge.setText("1");
                        badge.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 0 5; -fx-background-radius: 8;");
                        badge.setVisible(true);
                    } else if (PROJECT_LEVEL_CATEGORIES.contains(item)) {
                        badge.setText("\uD83D\uDCC1");
                        badge.setStyle("-fx-font-size: 11px; -fx-opacity: 0.65;");
                        badge.setVisible(true);
                    } else {
                        badge.setVisible(false);
                        badge.setText("");
                    }
                    setText(null);
                    setGraphic(container);
                }
            }
        });

        VBox leftPane = new VBox(8, searchField, tree);
        leftPane.setPadding(new Insets(10, 8, 10, 10));
        leftPane.setPrefWidth(240);

        // Content Area Top Bar: Breadcrumb + Back/Forward/Pin
        Label breadcrumb = new Label("Database");
        breadcrumb.getStyleClass().add("panel-header");
        breadcrumb.setStyle("-fx-font-size: 13px; -fx-text-fill: #dfe1e5; -fx-font-weight: bold;");

        Button backBtn = new Button("←");
        backBtn.setTooltip(new Tooltip("Back"));
        backBtn.getStyleClass().add("nav-button");

        Button forwardBtn = new Button("→");
        forwardBtn.setTooltip(new Tooltip("Forward"));
        forwardBtn.getStyleClass().add("nav-button");

        Button pinBtn = new Button("📌");
        pinBtn.setTooltip(new Tooltip("Keep open"));
        pinBtn.getStyleClass().add("nav-button");

        HBox navControls = new HBox(4, backBtn, forwardBtn, pinBtn);
        navControls.setAlignment(Pos.CENTER_RIGHT);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox contentHeader = new HBox(10, breadcrumb, headerSpacer, navControls);
        contentHeader.setAlignment(Pos.CENTER_LEFT);
        contentHeader.setPadding(new Insets(4, 0, 10, 0));
        contentHeader.setStyle("-fx-border-color: transparent transparent #43454a transparent; -fx-border-width: 0 0 1 0;");

        // Map for TreeItem and Panels
        Map<String, TreeItem<String>> itemIndex = new HashMap<>();
        indexTreeItems(rootItem, "", itemIndex);

        // Functional Setting Inputs
        Map<String, Object> inputRegistry = new HashMap<>();

        // Content Panels
        Map<String, Node> panels = new HashMap<>();

        // Navigation History
        List<String> history = new ArrayList<>();
        int[] historyCursor = new int[]{-1};
        boolean[] navigatingHistory = new boolean[]{false};

        StackPane contentStack = new StackPane();
        ScrollPane scrollPane = new ScrollPane(contentStack);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox rightPane = new VBox(8, contentHeader, scrollPane);
        rightPane.setPadding(new Insets(10, 16, 10, 14));

        SplitPane split = new SplitPane(leftPane, rightPane);
        split.setDividerPositions(0.28);
        SplitPane.setResizableWithParent(leftPane, false);
        VBox.setVgrow(split, Priority.ALWAYS);

        // Navigator consumer
        java.util.function.Consumer<String> navigateTo = (pathOrName) -> {
            TreeItem<String> target = itemIndex.get(pathOrName);
            if (target == null) {
                for (Map.Entry<String, TreeItem<String>> entry : itemIndex.entrySet()) {
                    if (entry.getKey().endsWith("/ " + pathOrName) || entry.getKey().equals(pathOrName)
                            || entry.getValue().getValue().equalsIgnoreCase(pathOrName)) {
                        target = entry.getValue();
                        break;
                    }
                }
            }
            if (target != null) {
                // Ensure parents are expanded
                TreeItem<String> p = target.getParent();
                while (p != null) {
                    p.setExpanded(true);
                    p = p.getParent();
                }
                String targetPath = getFullPath(target);
                String formattedBreadcrumb = targetPath.replace(" / ", "  \u203a  ");
                if (PROJECT_LEVEL_CATEGORIES.contains(target.getValue())) {
                    formattedBreadcrumb += "  \uD83D\uDCC1";
                }
                breadcrumb.setText(formattedBreadcrumb);
                tree.getSelectionModel().select(target);
                tree.scrollTo(tree.getRow(target));
            }
        };

        // Selection listener
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String fullPath = getFullPath(newVal);
            String formattedBreadcrumb = fullPath.replace(" / ", "  \u203a  ");
            if (PROJECT_LEVEL_CATEGORIES.contains(newVal.getValue())) {
                formattedBreadcrumb += "  \uD83D\uDCC1";
            }
            breadcrumb.setText(formattedBreadcrumb);

            if (!navigatingHistory[0]) {
                if (historyCursor[0] < history.size() - 1) {
                    history.subList(historyCursor[0] + 1, history.size()).clear();
                }
                history.add(fullPath);
                historyCursor[0] = history.size() - 1;
            }
            backBtn.setDisable(historyCursor[0] <= 0);
            forwardBtn.setDisable(historyCursor[0] >= history.size() - 1);

            Node panel = panels.computeIfAbsent(fullPath, k -> buildPanelForPath(fullPath, settings, inputRegistry, navigateTo));
            contentStack.getChildren().setAll(panel);
        });

        // History Back / Forward
        backBtn.setOnAction(e -> {
            if (historyCursor[0] > 0) {
                historyCursor[0]--;
                navigatingHistory[0] = true;
                try {
                    navigateTo.accept(history.get(historyCursor[0]));
                } finally {
                    navigatingHistory[0] = false;
                }
                backBtn.setDisable(historyCursor[0] <= 0);
                forwardBtn.setDisable(historyCursor[0] >= history.size() - 1);
            }
        });

        forwardBtn.setOnAction(e -> {
            if (historyCursor[0] < history.size() - 1) {
                historyCursor[0]++;
                navigatingHistory[0] = true;
                try {
                    navigateTo.accept(history.get(historyCursor[0]));
                } finally {
                    navigatingHistory[0] = false;
                }
                backBtn.setDisable(historyCursor[0] <= 0);
                forwardBtn.setDisable(historyCursor[0] >= history.size() - 1);
            }
        });

        // Search filtering
        searchField.textProperty().addListener((obs, oldTxt, newTxt) -> {
            if (newTxt == null || newTxt.isBlank()) {
                tree.setRoot(rootItem);
            } else {
                TreeItem<String> filteredRoot = buildFilteredTree(rootItem, newTxt.toLowerCase(Locale.ROOT).trim());
                tree.setRoot(filteredRoot);
            }
        });

        // Bottom Controls
        Button helpBtn = new Button("?");
        helpBtn.setTooltip(new Tooltip("Help"));
        helpBtn.setStyle("-fx-font-weight: bold; -fx-min-width: 28px; -fx-min-height: 28px; -fx-background-radius: 14;");
        helpBtn.setOnAction(e -> mainWindow.showOnlineHelp());

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());

        Button apply = new Button("Apply");
        Button ok = new Button("OK");
        ok.getStyleClass().add("run-button");
        ok.setDefaultButton(true);

        Runnable applyAction = () -> applySettings(mainWindow, settings, inputRegistry);
        apply.setOnAction(e -> applyAction.run());
        ok.setOnAction(e -> {
            applyAction.run();
            stage.close();
        });

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        HBox buttonBar = new HBox(10, helpBtn, bottomSpacer, ok, cancel, apply);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 16, 12, 16));

        VBox rootBox = new VBox(split, buttonBar);
        rootBox.getStyleClass().add("app-root");
        Scene scene = new Scene(rootBox, 880, 600);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);

        // Pre-selection
        if (initialCategory != null && !initialCategory.isBlank()) {
            navigateTo.accept(initialCategory);
        } else {
            TreeItem<String> dbItem = itemIndex.get("Database");
            if (dbItem != null) {
                tree.getSelectionModel().select(dbItem);
                breadcrumb.setText("Database");
            }
        }

        stage.show();
    }

    private static void indexTreeItems(TreeItem<String> item, String parentPath, Map<String, TreeItem<String>> map) {
        String currentPath = parentPath.isEmpty() ? item.getValue() : parentPath + " / " + item.getValue();
        map.put(currentPath, item);
        map.put(item.getValue(), item);
        for (TreeItem<String> child : item.getChildren()) {
            indexTreeItems(child, currentPath, map);
        }
    }

    private static String getFullPath(TreeItem<String> item) {
        if (item == null) return "";
        StringBuilder sb = new StringBuilder(item.getValue());
        TreeItem<String> p = item.getParent();
        while (p != null && p.getValue() != null && !"root".equals(p.getValue())) {
            sb.insert(0, p.getValue() + " / ");
            p = p.getParent();
        }
        return sb.toString();
    }

    private static TreeItem<String> buildFilteredTree(TreeItem<String> source, String query) {
        TreeItem<String> copy = new TreeItem<>(source.getValue());
        copy.setExpanded(true);
        boolean matchesSelf = source.getValue() != null && source.getValue().toLowerCase(Locale.ROOT).contains(query);
        for (TreeItem<String> child : source.getChildren()) {
            TreeItem<String> filteredChild = buildFilteredTree(child, query);
            if (filteredChild != null) {
                copy.getChildren().add(filteredChild);
            }
        }
        if (matchesSelf || !copy.getChildren().isEmpty()) {
            return copy;
        }
        return null;
    }

    // =========================================================================
    // Panel Builders
    // =========================================================================

    private static Node buildPanelForPath(String fullPath, AppSettingsStore.Settings settings,
                                          Map<String, Object> inputs, java.util.function.Consumer<String> navigateTo) {
        CategoryDef cat = findCategoryByPath(fullPath, CATEGORIES);
        if (cat == null) {
            String leaf = fullPath.contains(" / ") ? fullPath.substring(fullPath.lastIndexOf(" / ") + 3) : fullPath;
            cat = findCategoryByName(leaf, CATEGORIES);
        }

        // Functional leaf panels
        if ("Appearance & Behavior / Appearance".equals(fullPath) || "Appearance".equals(fullPath)) {
            return buildAppearancePanel(settings, inputs, navigateTo);
        } else if ("Appearance & Behavior / System Settings / Updates".equals(fullPath) || "Updates".equals(fullPath)) {
            return buildUpdatesPanel(settings, inputs);
        } else if ("Editor / General".equals(fullPath)) {
            return buildGeneralEditorPanel(settings, inputs);
        } else if ("Editor / General / Font".equals(fullPath) || "Editor / Font".equals(fullPath) || "Font".equals(fullPath)) {
            return buildFontPanel(settings, inputs);
        } else if ("Database / Query Execution".equals(fullPath)) {
            return buildQueryExecutionPanel(settings, inputs, navigateTo);
        } else if ("Database / Query Execution / Output and Results".equals(fullPath) || "Output and Results".equals(fullPath)) {
            return buildOutputAndResultsPanel(settings, inputs);
        } else if ("Database / Query Execution / User Parameters".equals(fullPath) || "User Parameters".equals(fullPath)) {
            return buildUserParametersPanel(settings, inputs);
        } else if ("Database / Data Editor and Viewer".equals(fullPath) || "Appearance & Behavior / Data Editor and Viewer".equals(fullPath) || "Data Editor and Viewer".equals(fullPath)) {
            return buildDataEditorPanel(settings, inputs);
        } else if ("Database / Database Explorer".equals(fullPath) || "Database Explorer".equals(fullPath)) {
            return buildDatabaseExplorerPanel(settings, inputs);
        } else if ("Database / CSV Formats".equals(fullPath) || "CSV Formats".equals(fullPath)) {
            return buildCsvFormatsPanel(settings, inputs);
        } else if ("Database / AI Tools".equals(fullPath) || "AI Tools".equals(fullPath)) {
            return buildAiToolsPanel(settings, inputs);
        } else if ("Database / Query Files and Consoles".equals(fullPath) || "Query Files and Consoles".equals(fullPath)) {
            return buildQueryFilesAndConsolesPanel(settings, inputs);
        } else if ("Database / SQL Dialects".equals(fullPath) || "SQL Dialects".equals(fullPath)) {
            return buildSqlDialectsPanel(settings, inputs);
        } else if ("Database / SQL Resolution Scopes".equals(fullPath) || "SQL Resolution Scopes".equals(fullPath)) {
            return buildSqlResolutionScopesPanel(settings, inputs);
        } else if ("Database / Other".equals(fullPath) || "Other".equals(fullPath)) {
            return buildDatabaseOtherPanel(settings, inputs);
        } else if ("Keymap".equals(fullPath)) {
            return buildKeymapPanel(settings, inputs);
        } else if ("Plugins".equals(fullPath)) {
            return buildPluginsPanel();
        } else if ("Version Control / Git".equals(fullPath) || "Git".equals(fullPath)) {
            return buildVersionControlPanel(cat, navigateTo);
        } else if ("Version Control / Commit".equals(fullPath) || "Commit".equals(fullPath)) {
            return buildCommitPanel(cat);
        } else if ("Languages / Markdown".equals(fullPath) || "Markdown".equals(fullPath)) {
            return buildMarkdownPanel(cat);
        } else if ("Languages / Mermaid".equals(fullPath) || "Mermaid".equals(fullPath)) {
            return buildMermaidPanel(cat);
        } else if ("Languages / Schemas and DTDs".equals(fullPath) || "Schemas and DTDs".equals(fullPath)) {
            return buildSchemasDtdsPanel(cat);
        } else if ("Tools / Debugger".equals(fullPath) || "Debugger".equals(fullPath)) {
            return buildDebuggerPanel(cat);
        } else if ("Tools / Diff & Merge".equals(fullPath) || "Diff & Merge".equals(fullPath)) {
            return buildDiffMergePanel(cat);
        } else if ("Tools / Terminal".equals(fullPath) || "Terminal".equals(fullPath)) {
            return buildToolsPanel(cat, navigateTo);
        } else if ("Tools / SSH Configurations".equals(fullPath) || "SSH Configurations".equals(fullPath)) {
            return buildSshPanel(cat);
        } else if ("Tools / Diagrams".equals(fullPath) || "Diagrams".equals(fullPath)) {
            return buildDiagramsPanel(cat);
        } else if ("Tools / MCP Server".equals(fullPath) || "MCP Server".equals(fullPath)) {
            return buildMcpServerPanel(cat);
        } else if ("Editor / General / Smart Keys".equals(fullPath) || "Smart Keys".equals(fullPath)) {
            return buildSmartKeysPanel(cat, navigateTo);
        } else if ("Editor / Color Scheme".equals(fullPath) || "Color Scheme".equals(fullPath)) {
            return buildColorSchemeLandingPanel(cat, navigateTo);
        } else if ("Editor / Code Style / SQL".equals(fullPath)) {
            return buildCodeStyleSqlLandingPanel(cat, navigateTo);
        } else if ("Editor / Code Style".equals(fullPath) || "Code Style".equals(fullPath)) {
            return buildCodeStylePanel(cat, navigateTo);
        } else if ("Editor / Natural Languages".equals(fullPath) || "Natural Languages".equals(fullPath)) {
            return buildNaturalLanguagesPanel(cat, navigateTo);
        }

        // Category landing overview page (DataGrip style with description & blue clickable links)
        if (cat != null && !cat.getChildren().isEmpty()) {
            return buildCategoryLandingPanel(cat, navigateTo);
        }

        // Default DataGrip detailed page with breadcrumbs and options
        return buildGenericCategoryPanel(fullPath, cat);
    }

    private static VBox buildCategoryLandingPanel(CategoryDef cat, java.util.function.Consumer<String> navigateTo) {
        Label title = new Label(cat.getName());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label desc = new Label(cat.getDescription());
        desc.setWrapText(true);
        desc.setMaxWidth(620);
        desc.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px; -fx-line-spacing: 3;");

        VBox linkBox = new VBox(6);
        linkBox.setPadding(new Insets(10, 0, 0, 0));

        for (CategoryDef child : cat.getChildren()) {
            Hyperlink link = new Hyperlink(child.getName());
            link.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-width: 0;");
            link.setOnAction(e -> navigateTo.accept(child.getPath()));
            linkBox.getChildren().add(link);
        }

        VBox panel = new VBox(14, title, desc, linkBox);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    public static VBox buildAppearancePanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        return buildAppearancePanel(settings, inputs, null);
    }

    public static VBox buildAppearancePanel(AppSettingsStore.Settings settings, Map<String, Object> inputs, java.util.function.Consumer<String> navigateTo) {
        // Theme & Color Scheme Row
        Label themeLabel = new Label("Theme:");
        themeLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        themeLabel.setMinWidth(60);

        ComboBox<String> themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll(AppSettingsStore.Settings.defaultUiThemes());
        themeCombo.getSelectionModel().select(settings.getUiTheme());
        themeCombo.setPrefWidth(220);
        inputs.put("appearance_uiTheme", themeCombo);
        inputs.put("themeCombo", themeCombo);

        CheckBox syncWithOsCheck = new CheckBox("Sync with OS");
        syncWithOsCheck.setSelected(settings.isSyncThemeWithOs());
        inputs.put("appearance_syncWithOs", syncWithOsCheck);

        Button osSyncGearBtn = new Button("⚙");
        osSyncGearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-dim; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 4 2 4;");
        osSyncGearBtn.setTooltip(new Tooltip("Configure OS theme synchronization"));
        osSyncGearBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sync with OS");
            alert.setHeaderText("Operating System Theme Synchronization");
            alert.setContentText("When enabled, the IDE automatically switches between Light and Dark themes according to your system display settings.");
            alert.showAndWait();
        });

        HBox themeRow = new HBox(10, themeLabel, themeCombo, syncWithOsCheck, osSyncGearBtn);
        themeRow.setAlignment(Pos.CENTER_LEFT);

        // Editor color scheme
        Label schemeLabel = new Label("Editor color scheme:");
        schemeLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        schemeLabel.setMinWidth(140);
        schemeLabel.setPadding(new Insets(0, 0, 0, 16));

        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll(AppSettingsStore.Settings.defaultEditorColorSchemes());
        schemeCombo.getSelectionModel().select(settings.getEditorColorScheme());
        schemeCombo.setPrefWidth(220);
        inputs.put("appearance_editorColorScheme", schemeCombo);

        Button schemeGearBtn = new Button("⚙");
        schemeGearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-dim; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 4 2 4;");
        schemeGearBtn.setTooltip(new Tooltip("Open Editor Color Scheme settings"));
        if (navigateTo != null) {
            schemeGearBtn.setOnAction(e -> navigateTo.accept("Editor / Color Scheme"));
        }

        HBox schemeRow = new HBox(10, schemeLabel, schemeCombo, schemeGearBtn);
        schemeRow.setAlignment(Pos.CENTER_LEFT);

        // Different tool window background
        CheckBox diffToolWindowBgCheck = new CheckBox("Different tool window background");
        diffToolWindowBgCheck.setSelected(settings.isDifferentToolWindowBackground());
        diffToolWindowBgCheck.setPadding(new Insets(0, 0, 0, 16));
        inputs.put("appearance_differentToolWindowBackground", diffToolWindowBgCheck);

        Label diffToolWindowBgSubtext = new Label("Use lighter color in the dark theme and darker color in the light theme as a background");
        diffToolWindowBgSubtext.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        diffToolWindowBgSubtext.setPadding(new Insets(0, 0, 0, 38));

        VBox toolWinBgBox = new VBox(2, diffToolWindowBgCheck, diffToolWindowBgSubtext);

        // =========================================================================
        // Section: Accessibility
        // =========================================================================
        HBox accessSection = buildTitledSectionLine("Accessibility");

        Label zoomLabel = new Label("Zoom:");
        zoomLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        zoomLabel.setMinWidth(50);

        ComboBox<String> zoomCombo = new ComboBox<>();
        zoomCombo.getItems().addAll(AppSettingsStore.Settings.defaultIdeZooms());
        zoomCombo.getSelectionModel().select(settings.getIdeZoom());
        zoomCombo.setPrefWidth(110);
        inputs.put("appearance_ideZoom", zoomCombo);

        Label zoomHint = new Label("Change with Ctrl+Alt+Shift+= or Ctrl+Alt+Shift+Minus. Set to 100% with Ctrl+Alt+Shift+0");
        zoomHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        HBox zoomRow = new HBox(12, zoomLabel, zoomCombo, zoomHint);
        zoomRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox useCustomFontCheck = new CheckBox("Use custom font:");
        useCustomFontCheck.setSelected(settings.isUseCustomIdeFont());
        inputs.put("appearance_useCustomIdeFont", useCustomFontCheck);

        ComboBox<String> fontFamCombo = new ComboBox<>();
        fontFamCombo.getItems().addAll("Inter", "JetBrains Sans", "Segoe UI", "SF Pro Text", "Ubuntu", "Cantarell", "Roboto", "Arial", "System-ui");
        fontFamCombo.getSelectionModel().select(settings.getCustomIdeFontFamily());
        fontFamCombo.setPrefWidth(220);
        fontFamCombo.disableProperty().bind(useCustomFontCheck.selectedProperty().not());
        inputs.put("appearance_customIdeFontFamily", fontFamCombo);

        Label fontSizeLabel = new Label("Size:");
        fontSizeLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<Integer> fontSizeCombo = new ComboBox<>();
        fontSizeCombo.getItems().addAll(10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24);
        fontSizeCombo.getSelectionModel().select(Integer.valueOf(settings.getCustomIdeFontSize()));
        fontSizeCombo.setPrefWidth(80);
        fontSizeCombo.disableProperty().bind(useCustomFontCheck.selectedProperty().not());
        inputs.put("appearance_customIdeFontSize", fontSizeCombo);

        HBox fontRow = new HBox(10, useCustomFontCheck, fontFamCombo, fontSizeLabel, fontSizeCombo);
        fontRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox screenReaderCheck = new CheckBox("Support screen readers");
        screenReaderCheck.setSelected(settings.isSupportScreenReaders());
        inputs.put("appearance_supportScreenReaders", screenReaderCheck);

        Label reqRestart1 = new Label("Requires restart");
        reqRestart1.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        HBox screenReaderRow = new HBox(8, screenReaderCheck, reqRestart1);
        screenReaderRow.setAlignment(Pos.CENTER_LEFT);

        Label screenReaderSubtext = new Label("Ctrl+Tab and Ctrl+Shift+Tab will navigate UI controls in dialogs and will not be available for switching editor tabs or other IDE actions. Tooltips on mouse hover will be disabled.");
        screenReaderSubtext.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        screenReaderSubtext.setWrapText(true);
        screenReaderSubtext.setPadding(new Insets(0, 0, 0, 22));
        screenReaderSubtext.setMaxWidth(680);

        VBox screenReaderBox = new VBox(2, screenReaderRow, screenReaderSubtext);

        CheckBox contrastScrollbarsCheck = new CheckBox("Use contrast scrollbars");
        contrastScrollbarsCheck.setSelected(settings.isUseContrastScrollbars());
        inputs.put("appearance_useContrastScrollbars", contrastScrollbarsCheck);

        CheckBox visionDeficiencyCheck = new CheckBox("Adjust colors for red-green vision deficiency");
        visionDeficiencyCheck.setSelected(settings.isAdjustColorsForVisionDeficiency());
        inputs.put("appearance_adjustColorsForVisionDeficiency", visionDeficiencyCheck);

        Hyperlink howItWorksLink = new Hyperlink("How it works ↗");
        howItWorksLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-underline: false;");
        howItWorksLink.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Color Vision Deficiency Assistance");
            alert.setHeaderText("Adjust colors for protanopia and deuteranopia");
            alert.setContentText("This option replaces red and green highlighting in the editor, diff viewer, and UI indicators with high-contrast blue and orange palettes to ensure readability.");
            alert.showAndWait();
        });

        HBox visionRow = new HBox(8, visionDeficiencyCheck, howItWorksLink);
        visionRow.setAlignment(Pos.CENTER_LEFT);

        Label visionSubtext = new Label("Requires restart. For protanopia and deuteranopia.");
        visionSubtext.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        visionSubtext.setPadding(new Insets(0, 0, 0, 22));

        VBox visionBox = new VBox(2, visionRow, visionSubtext);

        // =========================================================================
        // Section: UI Options
        // =========================================================================
        HBox uiOptionsSection = buildTitledSectionLine("UI Options");

        // Left Column
        CheckBox compactModeCheck = new CheckBox("Compact mode");
        compactModeCheck.setSelected(settings.isCompactMode());
        inputs.put("appearance_compactMode", compactModeCheck);

        Label compactModeSubtext = new Label("UI elements take up less screen space");
        compactModeSubtext.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        compactModeSubtext.setPadding(new Insets(0, 0, 0, 22));
        VBox compactBox = new VBox(2, compactModeCheck, compactModeSubtext);

        CheckBox fullPathCheck = new CheckBox("Always show full path in window header");
        fullPathCheck.setSelected(settings.isAlwaysShowFullPathInWindowHeader());
        inputs.put("appearance_alwaysShowFullPathInWindowHeader", fullPathCheck);

        CheckBox projectColorsToolbarCheck = new CheckBox("Use project colors in main toolbar");
        projectColorsToolbarCheck.setSelected(settings.isUseProjectColorsInMainToolbar());
        inputs.put("appearance_useProjectColorsInMainToolbar", projectColorsToolbarCheck);

        Label projectColorsSubtext = new Label("Distinguish projects with different toolbar colors at a glance.");
        projectColorsSubtext.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        projectColorsSubtext.setPadding(new Insets(0, 0, 0, 22));
        VBox projectColorsBox = new VBox(2, projectColorsToolbarCheck, projectColorsSubtext);

        CheckBox keepPopupsCheck = new CheckBox("Keep popups open for toggle items");
        keepPopupsCheck.setSelected(settings.isKeepPopupsOpenForToggleItems());
        inputs.put("appearance_keepPopupsOpenForToggleItems", keepPopupsCheck);

        VBox leftUiCol = new VBox(10, compactBox, fullPathCheck, projectColorsBox, keepPopupsCheck);
        leftUiCol.setPrefWidth(340);

        // Right Column
        CheckBox dragDropAltCheck = new CheckBox("Drag-and-drop with Alt pressed only");
        dragDropAltCheck.setSelected(settings.isDragAndDropWithAltPressedOnly());
        inputs.put("appearance_dragAndDropWithAltPressedOnly", dragDropAltCheck);

        CheckBox smoothScrollingCheck = new CheckBox("Smooth scrolling");
        smoothScrollingCheck.setSelected(settings.isSmoothScrolling());
        inputs.put("appearance_smoothScrolling", smoothScrollingCheck);

        Label smoothScrollHelp = createHelpTooltip("Enables smooth kinetic scrolling animation in editors and table viewers");
        HBox smoothScrollRow = new HBox(6, smoothScrollingCheck, smoothScrollHelp);
        smoothScrollRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox mnemonicsControlsCheck = new CheckBox("Enable mnemonics in controls");
        mnemonicsControlsCheck.setSelected(settings.isEnableMnemonicsInControls());
        inputs.put("appearance_enableMnemonicsInControls", mnemonicsControlsCheck);

        CheckBox mnemonicsMenuCheck = new CheckBox("Enable mnemonics in menu");
        mnemonicsMenuCheck.setSelected(settings.isEnableMnemonicsInMenu());
        inputs.put("appearance_enableMnemonicsInMenu", mnemonicsMenuCheck);

        CheckBox displayIconsMenuCheck = new CheckBox("Display icons in menu items");
        displayIconsMenuCheck.setSelected(settings.isDisplayIconsInMenuItems());
        inputs.put("appearance_displayIconsInMenuItems", displayIconsMenuCheck);

        VBox rightUiCol = new VBox(10, dragDropAltCheck, smoothScrollRow, mnemonicsControlsCheck, mnemonicsMenuCheck, displayIconsMenuCheck);
        rightUiCol.setPrefWidth(340);

        HBox uiCols = new HBox(20, leftUiCol, rightUiCol);

        // Main menu
        Label mainMenuLabel = new Label("Main menu:");
        mainMenuLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        mainMenuLabel.setMinWidth(75);

        ComboBox<String> mainMenuCombo = new ComboBox<>();
        mainMenuCombo.getItems().addAll(AppSettingsStore.Settings.defaultMainMenuOptions());
        mainMenuCombo.getSelectionModel().select(settings.getMainMenuPresentation());
        mainMenuCombo.setPrefWidth(220);
        inputs.put("appearance_mainMenuPresentation", mainMenuCombo);

        HBox mainMenuRow = new HBox(10, mainMenuLabel, mainMenuCombo);
        mainMenuRow.setAlignment(Pos.CENTER_LEFT);

        Label mainMenuRestart = new Label("Requires restart");
        mainMenuRestart.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        mainMenuRestart.setPadding(new Insets(0, 0, 0, 85));

        VBox mainMenuBox = new VBox(2, mainMenuRow, mainMenuRestart);

        // Background Image button
        Button bgImageBtn = new Button("Background Image...");
        bgImageBtn.getStyleClass().add("connection-action-button");
        bgImageBtn.setOnAction(e -> showBackgroundImageDialog(settings, inputs));

        // =========================================================================
        // Section: Tree Views
        // =========================================================================
        HBox treeViewsSection = buildTitledSectionLine("Tree Views");

        CheckBox showIndentGuidesCheck = new CheckBox("Show indent guides");
        showIndentGuidesCheck.setSelected(settings.isShowIndentGuides());
        showIndentGuidesCheck.setPrefWidth(340);
        inputs.put("appearance_showIndentGuides", showIndentGuidesCheck);

        CheckBox useSmallerIndentsCheck = new CheckBox("Use smaller indents");
        useSmallerIndentsCheck.setSelected(settings.isUseSmallerIndents());
        useSmallerIndentsCheck.setPrefWidth(340);
        inputs.put("appearance_useSmallerIndents", useSmallerIndentsCheck);

        HBox treeViewsRow = new HBox(20, showIndentGuidesCheck, useSmallerIndentsCheck);

        // =========================================================================
        // Section: Tool Windows
        // =========================================================================
        HBox toolWindowsSection = buildTitledSectionLine("Tool Windows");

        CheckBox showToolBarsCheck = new CheckBox("Show tool window bars");
        showToolBarsCheck.setSelected(settings.isShowToolWindowBars());
        inputs.put("appearance_showToolWindowBars", showToolBarsCheck);

        CheckBox showToolNamesCheck = new CheckBox("Show tool window names");
        showToolNamesCheck.setSelected(settings.isShowToolWindowNames());
        inputs.put("appearance_showToolWindowNames", showToolNamesCheck);

        CheckBox sideBySideLeftCheck = new CheckBox("Side-by-side layout on the left");
        sideBySideLeftCheck.setSelected(settings.isSideBySideLayoutOnLeft());
        inputs.put("appearance_sideBySideLayoutOnLeft", sideBySideLeftCheck);

        CheckBox sideBySideRightCheck = new CheckBox("Side-by-side layout on the right");
        sideBySideRightCheck.setSelected(settings.isSideBySideLayoutOnRight());
        inputs.put("appearance_sideBySideLayoutOnRight", sideBySideRightCheck);

        CheckBox widescreenCheck = new CheckBox("Widescreen tool window layout");
        widescreenCheck.setSelected(settings.isWidescreenToolWindowLayout());
        inputs.put("appearance_widescreenToolWindowLayout", widescreenCheck);

        Label widescreenHelp = createHelpTooltip("Maximizes vertical height for tool windows on widescreen displays");
        HBox widescreenRow = new HBox(6, widescreenCheck, widescreenHelp);
        widescreenRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox rememberSizeCheck = new CheckBox("Remember size for each tool window");
        rememberSizeCheck.setSelected(settings.isRememberSizeForEachToolWindow());
        inputs.put("appearance_rememberSizeForEachToolWindow", rememberSizeCheck);

        VBox toolWinChecks = new VBox(10, showToolBarsCheck, showToolNamesCheck, sideBySideLeftCheck, sideBySideRightCheck, widescreenRow, rememberSizeCheck);
        toolWinChecks.setPrefWidth(340);

        Pane ideMockup = buildIdeToolWindowMockup(
                showToolBarsCheck.selectedProperty(),
                showToolNamesCheck.selectedProperty(),
                sideBySideLeftCheck.selectedProperty(),
                sideBySideRightCheck.selectedProperty(),
                widescreenCheck.selectedProperty()
        );

        HBox toolWindowsBox = new HBox(20, toolWinChecks, ideMockup);
        toolWindowsBox.setAlignment(Pos.CENTER_LEFT);

        // =========================================================================
        // Section: Presentation Mode
        // =========================================================================
        HBox presModeSection = buildTitledSectionLine("Presentation Mode");

        Label presZoomLabel = new Label("Zoom:");
        presZoomLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        presZoomLabel.setMinWidth(50);

        ComboBox<String> presZoomCombo = new ComboBox<>();
        presZoomCombo.getItems().addAll(AppSettingsStore.Settings.defaultPresentationModeZooms());
        presZoomCombo.getSelectionModel().select(settings.getPresentationModeZoom());
        presZoomCombo.setPrefWidth(110);
        inputs.put("appearance_presentationModeZoom", presZoomCombo);

        HBox presZoomRow = new HBox(12, presZoomLabel, presZoomCombo);
        presZoomRow.setAlignment(Pos.CENTER_LEFT);

        // =========================================================================
        // Section: Antialiasing
        // =========================================================================
        HBox aaSection = buildTitledSectionLine("Antialiasing");

        Label ideAaLabel = new Label("IDE:");
        ideAaLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> ideAaCombo = new ComboBox<>();
        ideAaCombo.getItems().addAll(AppSettingsStore.Settings.defaultAntialiasingOptions());
        ideAaCombo.getSelectionModel().select(settings.getIdeAntialiasing());
        ideAaCombo.setPrefWidth(130);
        inputs.put("appearance_ideAntialiasing", ideAaCombo);

        Label editorAaLabel = new Label("Editor:");
        editorAaLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        editorAaLabel.setPadding(new Insets(0, 0, 0, 20));

        ComboBox<String> editorAaCombo = new ComboBox<>();
        editorAaCombo.getItems().addAll(AppSettingsStore.Settings.defaultAntialiasingOptions());
        editorAaCombo.getSelectionModel().select(settings.getEditorAntialiasing());
        editorAaCombo.setPrefWidth(130);
        inputs.put("appearance_editorAntialiasing", editorAaCombo);

        HBox aaRow = new HBox(10, ideAaLabel, ideAaCombo, editorAaLabel, editorAaCombo);
        aaRow.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(10,
                themeRow, schemeRow, toolWinBgBox,
                accessSection, zoomRow, fontRow, screenReaderBox, contrastScrollbarsCheck, visionBox,
                uiOptionsSection, uiCols, mainMenuBox, bgImageBtn,
                treeViewsSection, treeViewsRow,
                toolWindowsSection, toolWindowsBox,
                presModeSection, presZoomRow,
                aaSection, aaRow
        );
        panel.setPadding(new Insets(4, 8, 24, 8));
        return panel;
    }

    private static Pane buildIdeToolWindowMockup(
            javafx.beans.value.ObservableValue<Boolean> showBars,
            javafx.beans.value.ObservableValue<Boolean> showNames,
            javafx.beans.value.ObservableValue<Boolean> sideBySideLeft,
            javafx.beans.value.ObservableValue<Boolean> sideBySideRight,
            javafx.beans.value.ObservableValue<Boolean> widescreen) {

        VBox frame = new VBox();
        frame.setPrefSize(340, 160);
        frame.setMaxSize(340, 160);
        frame.setMinSize(340, 160);
        frame.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        // Top bar with 3 dots
        HBox topBar = new HBox(5);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(4, 8, 4, 8));
        topBar.setStyle("-fx-background-color: #26282e; -fx-border-color: #393b40; -fx-border-width: 0 0 1 0; -fx-background-radius: 4 4 0 0;");

        Region dot1 = new Region();
        dot1.setPrefSize(6, 6);
        dot1.setStyle("-fx-background-color: #ed6a5e; -fx-background-radius: 3;");
        Region dot2 = new Region();
        dot2.setPrefSize(6, 6);
        dot2.setStyle("-fx-background-color: #f4bf4f; -fx-background-radius: 3;");
        Region dot3 = new Region();
        dot3.setPrefSize(6, 6);
        dot3.setStyle("-fx-background-color: #61c554; -fx-background-radius: 3;");
        topBar.getChildren().addAll(dot1, dot2, dot3);

        // Body with toolbars and panes
        HBox body = new HBox();
        VBox.setVgrow(body, Priority.ALWAYS);

        // Left tool strip
        VBox leftStrip = new VBox(4);
        leftStrip.setPrefWidth(12);
        leftStrip.setAlignment(Pos.TOP_CENTER);
        leftStrip.setPadding(new Insets(4, 2, 4, 2));
        leftStrip.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-width: 0 1 0 0;");
        leftStrip.visibleProperty().bind(showBars);
        leftStrip.managedProperty().bind(showBars);

        // Left pane (e.g. Database Explorer)
        VBox leftPane = new VBox();
        leftPane.setPrefWidth(60);
        leftPane.setStyle("-fx-background-color: #26282e; -fx-border-color: #393b40; -fx-border-width: 0 1 0 0;");

        // Center editor pane
        VBox centerPane = new VBox();
        HBox.setHgrow(centerPane, Priority.ALWAYS);
        centerPane.setStyle("-fx-background-color: #1e1f22;");

        // Right pane
        VBox rightPane = new VBox();
        rightPane.setPrefWidth(60);
        rightPane.setStyle("-fx-background-color: #26282e; -fx-border-color: #393b40; -fx-border-width: 0 0 0 1;");

        // Right tool strip
        VBox rightStrip = new VBox(4);
        rightStrip.setPrefWidth(12);
        rightStrip.setAlignment(Pos.TOP_CENTER);
        rightStrip.setPadding(new Insets(4, 2, 4, 2));
        rightStrip.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-width: 0 0 0 1;");
        rightStrip.visibleProperty().bind(showBars);
        rightStrip.managedProperty().bind(showBars);

        body.getChildren().addAll(leftStrip, leftPane, centerPane, rightPane, rightStrip);
        frame.getChildren().addAll(topBar, body);

        // Reactive layout adjustments
        sideBySideLeft.addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                leftPane.setPrefWidth(85);
            } else {
                leftPane.setPrefWidth(60);
            }
        });

        sideBySideRight.addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                rightPane.setPrefWidth(85);
            } else {
                rightPane.setPrefWidth(60);
            }
        });

        return frame;
    }

    private static void showBackgroundImageDialog(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Background Image");
        dialog.setResizable(true);

        Label imgLabel = new Label("Image:");
        imgLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        imgLabel.setMinWidth(60);

        TextField imgPathField = new TextField(settings.getBackgroundImagePath());
        imgPathField.setPromptText("Path or URL to image file");
        HBox.setHgrow(imgPathField, Priority.ALWAYS);

        Button browseBtn = new Button("...");
        browseBtn.setOnAction(ev -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Background Image");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.svg", "*.webp")
            );
            java.io.File file = chooser.showOpenDialog(dialog);
            if (file != null) {
                imgPathField.setText(file.getAbsolutePath());
            }
        });

        HBox imgRow = new HBox(8, imgLabel, imgPathField, browseBtn);
        imgRow.setAlignment(Pos.CENTER_LEFT);

        Label opLabel = new Label("Opacity:");
        opLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        opLabel.setMinWidth(60);

        Slider opSlider = new Slider(0, 100, settings.getBackgroundImageOpacity());
        HBox.setHgrow(opSlider, Priority.ALWAYS);

        Spinner<Integer> opSpinner = new Spinner<>(0, 100, settings.getBackgroundImageOpacity());
        opSpinner.setPrefWidth(70);
        opSpinner.setEditable(true);

        opSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!opSpinner.isFocused()) {
                opSpinner.getValueFactory().setValue(newVal.intValue());
            }
        });
        opSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                opSlider.setValue(newVal);
            }
        });

        HBox opRow = new HBox(8, opLabel, opSlider, opSpinner);
        opRow.setAlignment(Pos.CENTER_LEFT);

        // Placement buttons
        ToggleGroup placementGroup = new ToggleGroup();
        ToggleButton splitV = new ToggleButton("◫");
        splitV.setTooltip(new Tooltip("Split vertically"));
        ToggleButton splitH = new ToggleButton("⬒");
        splitH.setTooltip(new Tooltip("Split horizontally"));
        ToggleButton centerBtn = new ToggleButton("⊙");
        centerBtn.setTooltip(new Tooltip("Center"));
        centerBtn.setToggleGroup(placementGroup);
        ToggleButton fillBtn = new ToggleButton("🔲");
        fillBtn.setTooltip(new Tooltip("Scale to fill"));
        fillBtn.setToggleGroup(placementGroup);
        fillBtn.setSelected(true);
        ToggleButton tileBtn = new ToggleButton("▦");
        tileBtn.setTooltip(new Tooltip("Tile / repeat"));
        tileBtn.setToggleGroup(placementGroup);
        ToggleButton anchorBtn = new ToggleButton("☵");
        anchorBtn.setTooltip(new Tooltip("Anchor to grid"));
        anchorBtn.setToggleGroup(placementGroup);

        HBox placementRow = new HBox(8, splitV, splitH, new Separator(javafx.geometry.Orientation.VERTICAL),
                centerBtn, fillBtn, tileBtn, anchorBtn);
        placementRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox projectOnlyCheck = new CheckBox("This project only");
        projectOnlyCheck.setSelected(settings.isBackgroundImageThisProjectOnly());

        // Tabs for Editor and Tools vs Empty Frame
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab editorTab = new Tab("Editor and Tools");
        Tab emptyFrameTab = new Tab("Empty Frame");

        // Code preview
        TextArea codePreview = new TextArea(
                "---\n" +
                "title: Title example\n" +
                "---\n\n" +
                "flowchart LR\n" +
                "A --> B\n" +
                "subgraph name\n" +
                "C --> D\n" +
                "end\n" +
                "id1([This is the text in the box])\n" +
                "id2[\"This is the (text) in the box\"]\n\n" +
                "stateDiagram-v2\n" +
                "S1: The state with a note\n" +
                "note right of S1\n" +
                "This is note\n" +
                "end note"
        );
        codePreview.setEditable(false);
        codePreview.setStyle("-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px; -fx-control-inner-background: #1e1f22;");
        codePreview.setPrefRowCount(12);
        VBox.setVgrow(codePreview, Priority.ALWAYS);

        editorTab.setContent(codePreview);
        emptyFrameTab.setContent(new StackPane(new Label("Empty Frame Background Preview")));
        tabPane.getTabs().addAll(editorTab, emptyFrameTab);

        Button setBtn = new Button("Set for Editor and Tools");
        setBtn.setDefaultButton(true);
        setBtn.setOnAction(ev -> {
            inputs.put("appearance_backgroundImagePath", imgPathField.getText().trim());
            inputs.put("appearance_backgroundImageOpacity", (int) opSlider.getValue());
            inputs.put("appearance_backgroundImageThisProjectOnly", projectOnlyCheck.isSelected());
            inputs.put("appearance_backgroundImageTarget", tabPane.getSelectionModel().getSelectedItem().getText());
            settings.setBackgroundImagePath(imgPathField.getText().trim());
            settings.setBackgroundImageOpacity((int) opSlider.getValue());
            settings.setBackgroundImageThisProjectOnly(projectOnlyCheck.isSelected());
            settings.setBackgroundImageTarget(tabPane.getSelectionModel().getSelectedItem().getText());
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(ev -> dialog.close());

        Button clearBtn = new Button("Clear and Close");
        clearBtn.setOnAction(ev -> {
            imgPathField.setText("");
            inputs.put("appearance_backgroundImagePath", "");
            settings.setBackgroundImagePath("");
            dialog.close();
        });

        HBox btnBar = new HBox(10, setBtn, cancelBtn, clearBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(12, 0, 0, 0));

        VBox root = new VBox(12, imgRow, opRow, placementRow, projectOnlyCheck, tabPane, btnBar);
        root.setPadding(new Insets(16));
        root.setPrefWidth(680);
        root.setPrefHeight(520);
        root.setStyle("-fx-background-color: #2b2d30;");

        dialog.setScene(new Scene(root));
        dialog.show();
    }


    private static VBox buildUpdatesPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label title = new Label("Updates");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox autoCheck = new CheckBox("Automatically check for application updates");
        autoCheck.setSelected(settings.isAutoUpdateEnabled());

        CheckBox autoDownload = new CheckBox("Automatically download updates when available");
        autoDownload.setSelected(settings.isAutoDownloadUpdates());

        Label channelLabel = new Label("Update channel:");
        channelLabel.getStyleClass().add("connection-field-label");
        ComboBox<String> channel = new ComboBox<>();
        channel.getItems().addAll("Stable", "Beta", "Nightly");
        channel.getSelectionModel().select(settings.getUpdateChannel());
        channel.setPrefWidth(180);

        Label endpointLabel = new Label("RozeHub endpoint:");
        endpointLabel.getStyleClass().add("connection-field-label");
        TextField endpoint = new TextField(settings.getUpdateEndpoint());
        endpoint.setPromptText("Leave blank to use the configured RozeHub endpoint");
        endpoint.setPrefWidth(420);

        Label hint = new Label("Updates are verified with SHA-256 before installation.");
        hint.getStyleClass().add("console-status");
        hint.setWrapText(true);
        hint.setMaxWidth(500);

        Button check = new Button("Check for Updates…");
        check.setOnAction(e -> {
            Window owner = check.getScene() == null ? null : check.getScene().getWindow();
            AppUpdateDialog.check(owner, false);
        });

        inputs.put("autoCheck", autoCheck);
        inputs.put("autoDownload", autoDownload);
        inputs.put("channel", channel);
        inputs.put("endpoint", endpoint);

        VBox panel = new VBox(14, title, autoCheck, autoDownload, channelLabel, channel, endpointLabel, endpoint, hint, check);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildGeneralEditorPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label title = new Label("General");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        // 1. Mouse Control
        Label mouseControl = new Label("Mouse Control");
        mouseControl.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox ctrlScrollCheck = new CheckBox("Change font size with Ctrl+Mouse Wheel in:");
        ctrlScrollCheck.setSelected(settings.isCtrlScrollZoomEnabled());

        RadioButton activeEditorRadio = new RadioButton("Active editor");
        RadioButton allEditorsRadio = new RadioButton("All editors");
        ToggleGroup zoomGroup = new ToggleGroup();
        activeEditorRadio.setToggleGroup(zoomGroup);
        allEditorsRadio.setToggleGroup(zoomGroup);
        activeEditorRadio.setSelected(true);
        activeEditorRadio.disableProperty().bind(ctrlScrollCheck.selectedProperty().not());
        allEditorsRadio.disableProperty().bind(ctrlScrollCheck.selectedProperty().not());

        HBox zoomRow = new HBox(12, ctrlScrollCheck, activeEditorRadio, allEditorsRadio);
        zoomRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox moveCodeDnd = new CheckBox("Move code fragments with drag-and-drop");
        moveCodeDnd.setSelected(true);
        Label dndHint = new Label("To copy, hold Ctrl while dragging");
        dndHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        HBox dndRow = new HBox(8, moveCodeDnd, dndHint);
        dndRow.setAlignment(Pos.CENTER_LEFT);

        VBox mouseBox = new VBox(6, mouseControl, zoomRow, dndRow);

        // 2. Soft Wraps
        Label softWraps = new Label("Soft Wraps");
        softWraps.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox softWrapFilesCheck = new CheckBox("Soft-wrap these files:");
        softWrapFilesCheck.setSelected(false);
        TextField softWrapPattern = new TextField("*.md; *.txt; *.rst; *.adoc");
        softWrapPattern.setPrefWidth(240);
        HBox softWrapRow = new HBox(10, softWrapFilesCheck, softWrapPattern);
        softWrapRow.setAlignment(Pos.CENTER_LEFT);
        Label softWrapHint = new Label("Use * and ? as wildcards and ; to separate patterns");
        softWrapHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        CheckBox useOriginalIndent = new CheckBox("Use the original line's indent for wrapped fragments");
        useOriginalIndent.setSelected(true);

        Label addIndentLabel = new Label("Add additional indent:");
        Spinner<Integer> addIndentSpinner = new Spinner<>(0, 32, 0, 1);
        addIndentSpinner.setPrefWidth(70);
        Label symbolsLabel = new Label("symbols");
        HBox indentRow = new HBox(8, addIndentLabel, addIndentSpinner, symbolsLabel);
        indentRow.setAlignment(Pos.CENTER_LEFT);
        indentRow.setPadding(new Insets(0, 0, 0, 20));

        CheckBox softWrapIndicators = new CheckBox("Only show soft-wrap indicators for the current line");
        softWrapIndicators.setSelected(true);

        VBox softWrapBox = new VBox(6, softWraps, softWrapRow, softWrapHint, useOriginalIndent, indentRow, softWrapIndicators);

        // 3. Virtual Space
        Label virtualSpace = new Label("Virtual Space");
        virtualSpace.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        Label caretPlacementLabel = new Label("Allow caret placement:");
        CheckBox afterEndOfLine = new CheckBox("After the end of line");
        CheckBox insideTabs = new CheckBox("Inside tabs");
        HBox placementRow = new HBox(12, caretPlacementLabel, afterEndOfLine, insideTabs);
        placementRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox virtualSpaceBottom = new CheckBox("Show virtual space at the bottom of the file");
        virtualSpaceBottom.setSelected(false);

        VBox virtualSpaceBox = new VBox(6, virtualSpace, placementRow, virtualSpaceBottom);

        // 4. Scroll Offset
        Label scrollOffset = new Label("Scroll Offset");
        scrollOffset.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        Label vertOffsetLbl = new Label("Vertical scroll offset:");
        Spinner<Integer> vertOffset = new Spinner<>(0, 50, 1, 1);
        vertOffset.setPrefWidth(70);

        Label vertJumpLbl = new Label("Vertical scroll jump:");
        Spinner<Integer> vertJump = new Spinner<>(0, 50, 0, 1);
        vertJump.setPrefWidth(70);

        Label horizOffsetLbl = new Label("Horizontal scroll offset:");
        Spinner<Integer> horizOffset = new Spinner<>(0, 50, 3, 1);
        horizOffset.setPrefWidth(70);

        Label horizJumpLbl = new Label("Horizontal scroll jump:");
        Spinner<Integer> horizJump = new Spinner<>(0, 50, 0, 1);
        horizJump.setPrefWidth(70);

        GridPane scrollGrid = new GridPane();
        scrollGrid.setHgap(12);
        scrollGrid.setVgap(8);
        scrollGrid.add(vertOffsetLbl, 0, 0);
        scrollGrid.add(vertOffset, 1, 0);
        scrollGrid.add(vertJumpLbl, 0, 1);
        scrollGrid.add(vertJump, 1, 1);
        scrollGrid.add(horizOffsetLbl, 0, 2);
        scrollGrid.add(horizOffset, 1, 2);
        scrollGrid.add(horizJumpLbl, 0, 3);
        scrollGrid.add(horizJump, 1, 3);

        VBox scrollOffsetBox = new VBox(6, scrollOffset, scrollGrid);

        // 5. Caret Movement
        Label caretMovement = new Label("Caret Movement");
        caretMovement.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        Label wordsLbl = new Label("When moving by words:");
        ComboBox<String> wordsCombo = new ComboBox<>();
        wordsCombo.getItems().addAll(
                "Jump to the current word boundaries  DataGrip default",
                "Always jump to word start",
                "Always jump to word end"
        );
        wordsCombo.getSelectionModel().select(0);
        wordsCombo.setPrefWidth(320);

        Label lineBreakLbl = new Label("Upon line break:");
        ComboBox<String> lineBreakCombo = new ComboBox<>();
        lineBreakCombo.getItems().addAll(
                "Jump to the next/previous line boundaries  DataGrip default",
                "Jump to the beginning of next line"
        );
        lineBreakCombo.getSelectionModel().select(0);
        lineBreakCombo.setPrefWidth(320);

        GridPane caretGrid = new GridPane();
        caretGrid.setHgap(12);
        caretGrid.setVgap(8);
        caretGrid.add(wordsLbl, 0, 0);
        caretGrid.add(wordsCombo, 1, 0);
        caretGrid.add(lineBreakLbl, 0, 1);
        caretGrid.add(lineBreakCombo, 1, 1);

        VBox caretMoveBox = new VBox(6, caretMovement, caretGrid);

        // 6. Scrolling
        Label scrolling = new Label("Scrolling");
        scrolling.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox smoothScrolling = new CheckBox("Enable smooth scrolling");
        smoothScrolling.setSelected(true);

        Label caretBehaviorLbl = new Label("Caret behavior:");
        RadioButton keepCaretInPlace = new RadioButton("Keep the caret in place, scroll editor canvas");
        RadioButton moveCaret = new RadioButton("Move caret, minimize editor scrolling");
        ToggleGroup caretScrollGroup = new ToggleGroup();
        keepCaretInPlace.setToggleGroup(caretScrollGroup);
        moveCaret.setToggleGroup(caretScrollGroup);
        keepCaretInPlace.setSelected(true);

        VBox scrollingBox = new VBox(6, scrolling, smoothScrolling, caretBehaviorLbl,
                new VBox(4, keepCaretInPlace, moveCaret));
        ((VBox) scrollingBox.getChildren().get(3)).setPadding(new Insets(0, 0, 0, 16));

        // 7. Rich-Text Copy
        Label richTextCopy = new Label("Rich-Text Copy");
        richTextCopy.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox copyAsRichText = new CheckBox("Copy (Ctrl+C) as rich text");
        copyAsRichText.setSelected(true);
        Label richTextHint = new Label("All formatting will be copied, including font, colors and so on");
        richTextHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        HBox richTextRow = new HBox(8, copyAsRichText, richTextHint);
        richTextRow.setAlignment(Pos.CENTER_LEFT);

        Label schemeLbl = new Label("Color scheme for copied fragment:");
        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll("Active scheme", "Darcula", "High Contrast", "Light");
        schemeCombo.getSelectionModel().select(0);
        schemeCombo.setPrefWidth(200);
        HBox schemeRow = new HBox(10, schemeLbl, schemeCombo);
        schemeRow.setAlignment(Pos.CENTER_LEFT);

        VBox richTextBox = new VBox(6, richTextCopy, richTextRow, schemeRow);

        // 8. On Save
        Label onSave = new Label("On Save");
        onSave.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox removeTrailingSpaces = new CheckBox("Remove trailing spaces on:");
        removeTrailingSpaces.setSelected(true);
        ComboBox<String> trailingLinesCombo = new ComboBox<>();
        trailingLinesCombo.getItems().addAll("Modified lines", "All lines");
        trailingLinesCombo.getSelectionModel().select(0);
        trailingLinesCombo.setPrefWidth(140);
        HBox trailingRow = new HBox(10, removeTrailingSpaces, trailingLinesCombo);
        trailingRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox keepTrailingSpacesOnCaret = new CheckBox("Keep trailing spaces on caret line");
        keepTrailingSpacesOnCaret.setSelected(true);
        keepTrailingSpacesOnCaret.setPadding(new Insets(0, 0, 0, 16));

        CheckBox removeTrailingBlankLines = new CheckBox("Remove trailing blank lines at the end of saved files");
        removeTrailingBlankLines.setSelected(false);

        CheckBox ensureLineBreak = new CheckBox("Ensure every saved file ends with a line break");
        ensureLineBreak.setSelected(false);

        VBox onSaveBox = new VBox(6, onSave, trailingRow, keepTrailingSpacesOnCaret, removeTrailingBlankLines, ensureLineBreak);

        inputs.put("ctrlScrollCheck", ctrlScrollCheck);

        VBox panel = new VBox(14, title, mouseBox, new Separator(), softWrapBox, new Separator(),
                virtualSpaceBox, new Separator(), scrollOffsetBox, new Separator(), caretMoveBox, new Separator(),
                scrollingBox, new Separator(), richTextBox, new Separator(), onSaveBox);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildSmartKeysPanel(CategoryDef cat, java.util.function.Consumer<String> navigateTo) {
        Label title = new Label("Smart Keys");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox homeNonWhitespace = new CheckBox("Home moves caret to first non-whitespace character");
        homeNonWhitespace.setSelected(true);

        CheckBox endIndentPos = new CheckBox("End on blank line moves caret to indent position");
        endIndentPos.setSelected(true);

        CheckBox insertPairedBrackets = new CheckBox("Insert paired brackets (), [], {}, <>");
        insertPairedBrackets.setSelected(true);

        CheckBox insertPairQuote = new CheckBox("Insert pair quote");
        insertPairQuote.setSelected(true);

        CheckBox reformatOnCloseBrace = new CheckBox("Reformat block on typing '}'");
        reformatOnCloseBrace.setSelected(true);

        CheckBox useCamelHumps = new CheckBox("Use \"CamelHumps\" words");
        useCamelHumps.setSelected(false);

        CheckBox honorCamelHumps = new CheckBox("Honor \"CamelHumps\" words settings when selecting on double click");
        honorCamelHumps.setSelected(true);

        CheckBox surroundSelection = new CheckBox("Surround selection on typing quote or brace");
        surroundSelection.setSelected(true);

        CheckBox multiCarets = new CheckBox("Add multiple carets on double Ctrl with arrow keys");
        multiCarets.setSelected(true);

        CheckBox jumpOutsideBracket = new CheckBox("Jump outside closing bracket/quote with Tab when typing");
        jumpOutsideBracket.setSelected(true);

        Label enterSection = new Label("Enter");
        enterSection.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox smartIndent = new CheckBox("Smart indent");
        smartIndent.setSelected(true);

        CheckBox insertPairBraceEnter = new CheckBox("Insert pair '}'");
        insertPairBraceEnter.setSelected(true);

        CheckBox closeBlockComment = new CheckBox("Close block comment");
        closeBlockComment.setSelected(true);

        VBox enterBox = new VBox(6, enterSection, smartIndent, insertPairBraceEnter, closeBlockComment);

        Label unindentLabel = new Label("Unindent on Backspace:");
        ComboBox<String> unindentCombo = new ComboBox<>();
        unindentCombo.getItems().addAll("To proper indent position", "To nearest indent boundary", "Disabled");
        unindentCombo.getSelectionModel().select(0);
        unindentCombo.setPrefWidth(220);

        Label reformatPasteLabel = new Label("Reformat on paste:");
        ComboBox<String> reformatPasteCombo = new ComboBox<>();
        reformatPasteCombo.getItems().addAll("None", "Indent each line", "Reformat block");
        reformatPasteCombo.getSelectionModel().select(0);
        reformatPasteCombo.setPrefWidth(160);

        CheckBox reformatLineBreaks = new CheckBox("Reformat again to remove custom line breaks");
        reformatLineBreaks.setSelected(false);

        GridPane dropdownsGrid = new GridPane();
        dropdownsGrid.setHgap(12);
        dropdownsGrid.setVgap(8);
        dropdownsGrid.add(unindentLabel, 0, 0);
        dropdownsGrid.add(unindentCombo, 1, 0);
        dropdownsGrid.add(reformatPasteLabel, 0, 1);
        dropdownsGrid.add(reformatPasteCombo, 1, 1);

        VBox panel = new VBox(10, title,
                homeNonWhitespace, endIndentPos, insertPairedBrackets, insertPairQuote,
                reformatOnCloseBrace, useCamelHumps, honorCamelHumps, surroundSelection,
                multiCarets, jumpOutsideBracket,
                new Separator(),
                enterBox,
                new Separator(),
                dropdownsGrid, reformatLineBreaks);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildFontPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label title = new Label("Font");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label fontLabel = new Label("Font:");
        fontLabel.getStyleClass().add("connection-field-label");
        ComboBox<String> fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll(Font.getFamilies());
        fontCombo.getSelectionModel().select(settings.getEditorFontFamily());
        fontCombo.setPrefWidth(240);
        fontCombo.setEditable(true);

        Label sizeLabel = new Label("Size:");
        sizeLabel.getStyleClass().add("connection-field-label");
        Spinner<Double> sizeSpinner = new Spinner<>(8, 48, settings.getEditorFontSize(), 1);
        sizeSpinner.setEditable(true);
        sizeSpinner.setPrefWidth(90);

        Label spacingLabel = new Label("Line spacing:");
        spacingLabel.getStyleClass().add("connection-field-label");
        Spinner<Double> spacingSpinner = new Spinner<>(1.0, 2.0, 1.2, 0.1);
        spacingSpinner.setEditable(true);
        spacingSpinner.setPrefWidth(90);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.add(fontLabel, 0, 0);
        grid.add(fontCombo, 1, 0);
        grid.add(sizeLabel, 0, 1);
        grid.add(sizeSpinner, 1, 1);
        grid.add(spacingLabel, 0, 2);
        grid.add(spacingSpinner, 1, 2);

        TextArea preview = new TextArea(
                "SELECT u.id, u.username, count(o.id) AS order_count\n"
                        + "FROM users u\n"
                        + "LEFT JOIN orders o ON o.user_id = u.id\n"
                        + "WHERE u.active = true\n"
                        + "GROUP BY u.id, u.username\n"
                        + "ORDER BY order_count DESC;\n"
                        + "-- Live font preview matching DataGrip console");
        preview.setEditable(false);
        preview.setPrefRowCount(7);
        preview.getStyleClass().add("process-output");

        Runnable refreshPreview = () -> preview.setStyle(
                "-fx-font-family: '" + fontCombo.getValue() + "'; -fx-font-size: " + sizeSpinner.getValue() + "px;");
        fontCombo.valueProperty().addListener((o, a, b) -> refreshPreview.run());
        sizeSpinner.valueProperty().addListener((o, a, b) -> refreshPreview.run());
        refreshPreview.run();

        inputs.put("fontCombo", fontCombo);
        inputs.put("sizeSpinner", sizeSpinner);

        VBox panel = new VBox(12, title, grid, new Label("Preview:"), preview);
        VBox.setVgrow(preview, Priority.ALWAYS);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildQueryExecutionPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs,
                                                 java.util.function.Consumer<String> navigateTo) {
        // Deep copy working actions so modifications stay staged until Apply/OK
        List<AppSettingsStore.ExecuteActionConfig> workingActions = new ArrayList<>();
        for (AppSettingsStore.ExecuteActionConfig act : settings.getExecuteActions()) {
            workingActions.add(act.copy());
        }
        while (workingActions.size() < 3) {
            int idx = workingActions.size() + 1;
            String shortcut = idx == 1 ? "Ctrl+Enter" : (idx == 2 ? "Ctrl+Shift+Enter" : "Ctrl+Alt+Enter");
            workingActions.add(new AppSettingsStore.ExecuteActionConfig(
                    "Execute" + (idx > 1 ? " (" + idx + ")" : ""),
                    shortcut,
                    "Ask what to execute",
                    "Nothing",
                    "Exactly as separate statements",
                    false));
        }

        inputs.put("executeActions", workingActions);

        // Left Action List (Execute, Execute (2), Execute (3))
        ListView<AppSettingsStore.ExecuteActionConfig> actionList = new ListView<>();
        actionList.getItems().setAll(workingActions);
        actionList.setPrefWidth(210);
        actionList.setPrefHeight(160);
        actionList.getStyleClass().add("completion-list");
        actionList.setStyle("-fx-border-color: #43454a; -fx-border-width: 1px; -fx-border-radius: 4px;");

        actionList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AppSettingsStore.ExecuteActionConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label nameLabel = new Label(item.getName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 12px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label scLabel = new Label(item.getShortcut() != null ? item.getShortcut() : "");
                    scLabel.setStyle("-fx-text-fill: #7c7f86; -fx-font-size: 11px;");

                    HBox row = new HBox(8, nameLabel, spacer, scLabel);
                    row.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(row);
                    setText(null);
                }
            }
        });

        // Right side controls for selected execution action
        Label shortcutTitle = new Label("Shortcut:");
        shortcutTitle.setStyle("-fx-text-fill: -text;");
        Label shortcutLink = new Label("Ctrl+Enter");
        shortcutLink.setStyle("-fx-text-fill: #3574F0; -fx-font-weight: bold; -fx-cursor: hand;");
        HBox shortcutBox = new HBox(8, shortcutTitle, shortcutLink);
        shortcutBox.setAlignment(Pos.CENTER_LEFT);

        Label whenInsideLabel = new Label("When caret inside statement execute:");
        ComboBox<String> whenInsideCombo = new ComboBox<>();
        whenInsideCombo.getItems().addAll(
                "Ask what to execute",
                "Smallest subquery or statement",
                "Smallest statement",
                "Largest statement",
                "Largest statement or batch",
                "Whole script",
                "Everything from caret"
        );
        whenInsideCombo.setPrefWidth(300);

        Label whenOutsideLabel = new Label("When caret outside statement execute:");
        ComboBox<String> whenOutsideCombo = new ComboBox<>();
        whenOutsideCombo.getItems().addAll(
                "Nothing",
                "Whole script",
                "Everything below caret"
        );
        whenOutsideCombo.setPrefWidth(300);

        Label forSelectionLabel = new Label("For selection execute:");
        ComboBox<String> forSelectionCombo = new ComboBox<>();
        forSelectionCombo.getItems().addAll(
                "Exactly as a single statement",
                "Exactly as separate statements",
                "Smart expand to script"
        );
        forSelectionCombo.setPrefWidth(300);

        CheckBox openResultsCheck = new CheckBox("Open results in new tab");

        GridPane actionGrid = new GridPane();
        actionGrid.setHgap(14);
        actionGrid.setVgap(12);
        actionGrid.add(whenInsideLabel, 0, 0);
        actionGrid.add(whenInsideCombo, 1, 0);
        actionGrid.add(whenOutsideLabel, 0, 1);
        actionGrid.add(whenOutsideCombo, 1, 1);
        actionGrid.add(forSelectionLabel, 0, 2);
        actionGrid.add(forSelectionCombo, 1, 2);

        VBox rightActionBox = new VBox(12, shortcutBox, actionGrid, openResultsCheck);
        HBox.setHgrow(rightActionBox, Priority.ALWAYS);

        boolean[] updatingFromSelection = new boolean[]{false};

        Runnable loadSelectedAction = () -> {
            AppSettingsStore.ExecuteActionConfig selected = actionList.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            updatingFromSelection[0] = true;
            try {
                shortcutLink.setText(selected.getShortcut() != null && !selected.getShortcut().isBlank()
                        ? selected.getShortcut() : "(None)");
                whenInsideCombo.setValue(selected.getWhenCaretInside());
                whenOutsideCombo.setValue(selected.getWhenCaretOutside());
                forSelectionCombo.setValue(selected.getForSelection());
                openResultsCheck.setSelected(selected.isOpenResultsInNewTab());
            } finally {
                updatingFromSelection[0] = false;
            }
        };

        whenInsideCombo.valueProperty().addListener((o, oldVal, newVal) -> {
            if (!updatingFromSelection[0] && newVal != null) {
                AppSettingsStore.ExecuteActionConfig sel = actionList.getSelectionModel().getSelectedItem();
                if (sel != null) sel.setWhenCaretInside(newVal);
            }
        });

        whenOutsideCombo.valueProperty().addListener((o, oldVal, newVal) -> {
            if (!updatingFromSelection[0] && newVal != null) {
                AppSettingsStore.ExecuteActionConfig sel = actionList.getSelectionModel().getSelectedItem();
                if (sel != null) sel.setWhenCaretOutside(newVal);
            }
        });

        forSelectionCombo.valueProperty().addListener((o, oldVal, newVal) -> {
            if (!updatingFromSelection[0] && newVal != null) {
                AppSettingsStore.ExecuteActionConfig sel = actionList.getSelectionModel().getSelectedItem();
                if (sel != null) sel.setForSelection(newVal);
            }
        });

        openResultsCheck.selectedProperty().addListener((o, oldVal, newVal) -> {
            if (!updatingFromSelection[0] && newVal != null) {
                AppSettingsStore.ExecuteActionConfig sel = actionList.getSelectionModel().getSelectedItem();
                if (sel != null) sel.setOpenResultsInNewTab(newVal);
            }
        });

        actionList.getSelectionModel().selectedIndexProperty().addListener((o, oldIdx, newIdx) -> {
            loadSelectedAction.run();
        });

        actionList.getSelectionModel().select(0);
        loadSelectedAction.run();

        HBox topBox = new HBox(18, actionList, rightActionBox);
        topBox.setAlignment(Pos.TOP_LEFT);

        // Bottom Section
        Label scriptSplitLabel = new Label("Split a script for execution in Generic and ANSI SQL dialects:");
        ComboBox<String> scriptSplittingCombo = new ComboBox<>();
        scriptSplittingCombo.getItems().addAll(
                "Into valid ANSI SQL statements or by separator",
                "Into ANSI SQL statements",
                "By statement separator"
        );
        scriptSplittingCombo.setValue(settings.getScriptSplitting());
        scriptSplittingCombo.setPrefWidth(350);

        CheckBox reviewParamsCheck = new CheckBox("Review parameters before execution");
        reviewParamsCheck.setSelected(settings.isReviewParametersBeforeExecution());

        CheckBox warnUnsafeCheck = new CheckBox("Show warning before running potentially unsafe queries");
        warnUnsafeCheck.setSelected(settings.isWarnUnsafeQueries());

        inputs.put("scriptSplittingCombo", scriptSplittingCombo);
        inputs.put("reviewParamsCheck", reviewParamsCheck);
        inputs.put("warnUnsafeCheck", warnUnsafeCheck);

        VBox bottomBox = new VBox(10, scriptSplitLabel, scriptSplittingCombo, reviewParamsCheck, warnUnsafeCheck);

        VBox panel = new VBox(18, topBox, new Separator(), bottomBox);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildOutputAndResultsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label breadcrumb = new Label("Database \u203A Query Execution \u203A Output and Results");
        breadcrumb.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px; -fx-padding: 0 0 6 0;");

        // 1. Output
        Label outputHeader = new Label("Output");
        outputHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox showTimestamp = new CheckBox("Show timestamp for query output");
        showTimestamp.setSelected(settings.isShowTimestampForQueryOutput());

        CheckBox enableDbms = new CheckBox("Enable DBMS_OUTPUT");
        enableDbms.setSelected(settings.isEnableDbmsOutput());
        Label dbmsHint = new Label("Applicable for Oracle and IBM Db2 LUW only");
        dbmsHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        HBox dbmsRow = new HBox(8, enableDbms, dbmsHint);
        dbmsRow.setAlignment(Pos.CENTER_LEFT);

        VBox outputBox = new VBox(8, outputHeader, showTimestamp, dbmsRow);

        // 2. Results
        Label resultsHeader = new Label("Results");
        resultsHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox showResultsInEditor = new CheckBox("Show results in editor");
        showResultsInEditor.setSelected(settings.isShowResultsInEditor());

        CheckBox createTitle = new CheckBox("Create title for results from comment before query");
        createTitle.setSelected(settings.isCreateTitleFromComment());

        Label treatTitleLabel = new Label("Treat text as title after");
        treatTitleLabel.setStyle("-fx-text-fill: -text;");
        TextField treatTitleField = new TextField(settings.getTitleAfterCommentText().isEmpty()
                ? "comment beginning" : settings.getTitleAfterCommentText());
        treatTitleField.setPromptText("comment beginning");
        treatTitleField.setPrefWidth(160);
        HBox treatRow = new HBox(8, treatTitleLabel, treatTitleField);
        treatRow.setAlignment(Pos.CENTER_LEFT);
        treatRow.setPadding(new Insets(0, 0, 0, 22));
        treatRow.disableProperty().bind(createTitle.selectedProperty().not());

        VBox resultsBox = new VBox(8, resultsHeader, showResultsInEditor, createTitle, treatRow);

        // 3. Services Tool Window
        Label servicesHeader = new Label("Services Tool Window");
        servicesHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        Label showServicesLabel = new Label("Show Services tool window for query execution output:");
        ComboBox<String> showServicesCombo = new ComboBox<>();
        showServicesCombo.getItems().addAll(
                "For all output",
                "For query output, errors, and result sets",
                "For errors and result sets",
                "For result sets",
                "Never"
        );
        showServicesCombo.setValue(settings.getShowServicesOutput());
        showServicesCombo.setPrefWidth(320);

        CheckBox focusServices = new CheckBox("Focus on Services tool window in window mode");
        focusServices.setSelected(settings.isFocusServicesInWindowMode());

        CheckBox openNewServicesTab = new CheckBox("Open new Services tab for sessions");
        openNewServicesTab.setSelected(settings.isOpenNewServicesTabForSessions());

        CheckBox activateServicesSelectedFile = new CheckBox("Activate Services output pane for selected file only");
        activateServicesSelectedFile.setSelected(settings.isActivateServicesForSelectedFileOnly());

        VBox servicesBox = new VBox(8, servicesHeader, showServicesLabel, showServicesCombo,
                focusServices, openNewServicesTab, activateServicesSelectedFile);

        // Query limits
        Label timeoutLabel = new Label("Query timeout (seconds):");
        Spinner<Integer> timeoutSpinner = new Spinner<>(5, 600, settings.getQueryTimeoutSeconds(), 5);
        timeoutSpinner.setEditable(true);
        timeoutSpinner.setPrefWidth(100);

        Label maxRowsLabel = new Label("Limit retrieved rows to:");
        Spinner<Integer> maxRowsSpinner = new Spinner<>(50, 50000, settings.getMaxResultRows(), 50);
        maxRowsSpinner.setEditable(true);
        maxRowsSpinner.setPrefWidth(120);

        CheckBox autoCommit = new CheckBox("Auto-commit transactions by default");
        autoCommit.setSelected(settings.isAutoCommit());

        GridPane limitsGrid = new GridPane();
        limitsGrid.setHgap(14);
        limitsGrid.setVgap(8);
        limitsGrid.add(timeoutLabel, 0, 0);
        limitsGrid.add(timeoutSpinner, 1, 0);
        limitsGrid.add(maxRowsLabel, 0, 1);
        limitsGrid.add(maxRowsSpinner, 1, 1);
        limitsGrid.add(autoCommit, 0, 2, 2, 1);

        inputs.put("showTimestampForQueryOutput", showTimestamp);
        inputs.put("enableDbmsOutput", enableDbms);
        inputs.put("showResultsInEditor", showResultsInEditor);
        inputs.put("createTitleFromComment", createTitle);
        inputs.put("titleAfterCommentText", treatTitleField);
        inputs.put("showServicesOutput", showServicesCombo);
        inputs.put("focusServicesInWindowMode", focusServices);
        inputs.put("openNewServicesTabForSessions", openNewServicesTab);
        inputs.put("activateServicesForSelectedFileOnly", activateServicesSelectedFile);
        inputs.put("timeoutSpinner", timeoutSpinner);
        inputs.put("maxRowsSpinner", maxRowsSpinner);
        inputs.put("autoCommit", autoCommit);

        VBox panel = new VBox(14, breadcrumb, outputBox, new Separator(), resultsBox, new Separator(), servicesBox, new Separator(), limitsGrid);
        panel.setPadding(new Insets(6, 12, 16, 12));
        return panel;
    }

    private static VBox buildUserParametersPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label breadcrumb = new Label("Database \u203A Query Execution \u203A User Parameters");
        breadcrumb.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Hyperlink revertLink = new Hyperlink("Revert changes");
        revertLink.setStyle("-fx-font-size: 11px;");

        HBox topBar = new HBox(8, breadcrumb, spacer, revertLink);
        topBar.setAlignment(Pos.CENTER_LEFT);

        CheckBox enableInConsoles = new CheckBox("Enable in query consoles and SQL files");
        enableInConsoles.setSelected(settings.isEnableUserParameters());

        CheckBox enableInLiterals = new CheckBox("Enable in string literals with SQL injection");
        enableInLiterals.setSelected(settings.isEnableUserParametersInLiteralsWithInjection());

        CheckBox substituteInsideStrings = new CheckBox("Substitute inside SQL strings");
        substituteInsideStrings.setSelected(settings.isSubstituteInsideSqlStrings());

        Label patternsLabel = new Label("Parameter patterns:");
        patternsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        // Action buttons
        Button addBtn = new Button("+");
        addBtn.setPrefWidth(28);
        Button removeBtn = new Button("—");
        removeBtn.setPrefWidth(28);
        Button upBtn = new Button("↑");
        upBtn.setPrefWidth(28);
        Button downBtn = new Button("↓");
        downBtn.setPrefWidth(28);
        HBox listToolbar = new HBox(4, addBtn, removeBtn, upBtn, downBtn);
        listToolbar.setAlignment(Pos.CENTER_LEFT);

        ObservableList<AppSettingsStore.UserParameterPattern> items = FXCollections.observableArrayList();
        for (AppSettingsStore.UserParameterPattern p : settings.getUserParameterPatterns()) {
            items.add(p.copy());
        }

        TableView<AppSettingsStore.UserParameterPattern> table = new TableView<>(items);
        table.setPrefHeight(200);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AppSettingsStore.UserParameterPattern, String> patCol = new TableColumn<>("Pattern");
        patCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPattern()));
        patCol.setPrefWidth(180);

        TableColumn<AppSettingsStore.UserParameterPattern, String> scopeCol = new TableColumn<>("Scope");
        scopeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getScope()));
        scopeCol.setPrefWidth(100);

        TableColumn<AppSettingsStore.UserParameterPattern, String> langCol = new TableColumn<>("Languages");
        langCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLanguages()));
        langCol.setPrefWidth(160);

        table.getColumns().addAll(patCol, scopeCol, langCol);

        // Bottom Detail Editor (Image 4)
        VBox detailBox = new VBox(8);
        detailBox.setPadding(new Insets(8, 8, 8, 8));
        detailBox.setStyle("-fx-background-color: -surface; -fx-border-color: -border; -fx-border-width: 1px; -fx-border-radius: 4px;");

        Label patternLabel = new Label("Pattern:");
        TextField patternField = new TextField();
        patternField.setPrefWidth(350);
        HBox patternRow = new HBox(8, patternLabel, patternField);
        patternRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox inScriptsCheck = new CheckBox("In scripts");
        CheckBox inLiteralsCheck = new CheckBox("In literals");
        ComboBox<String> langCombo = new ComboBox<>();
        langCombo.getItems().addAll("All languages", "All excl. SQL", "XML", "Python", "JAVA, PHP, Python", "PostgreSQL");
        langCombo.setPrefWidth(160);

        HBox detailOptions = new HBox(16, inScriptsCheck, inLiteralsCheck, langCombo);
        detailOptions.setAlignment(Pos.CENTER_LEFT);

        detailBox.getChildren().addAll(patternRow, detailOptions);
        detailBox.setDisable(true);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                detailBox.setDisable(false);
                patternField.setText(selected.getPattern());
                inScriptsCheck.setSelected(selected.isInScripts());
                inLiteralsCheck.setSelected(selected.isInLiterals());
                langCombo.setValue(selected.getLanguages());
            } else {
                detailBox.setDisable(true);
            }
        });

        patternField.textProperty().addListener((obs, oldVal, newVal) -> {
            AppSettingsStore.UserParameterPattern sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && newVal != null) {
                sel.setPattern(newVal);
                table.refresh();
            }
        });

        inScriptsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            AppSettingsStore.UserParameterPattern sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                sel.setInScripts(newVal);
                table.refresh();
            }
        });

        inLiteralsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            AppSettingsStore.UserParameterPattern sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                sel.setInLiterals(newVal);
                table.refresh();
            }
        });

        langCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            AppSettingsStore.UserParameterPattern sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && newVal != null) {
                sel.setLanguages(newVal);
                table.refresh();
            }
        });

        addBtn.setOnAction(e -> {
            AppSettingsStore.UserParameterPattern newP = new AppSettingsStore.UserParameterPattern("\":name\"", "everywhere", "All languages", true, false);
            items.add(newP);
            table.getSelectionModel().select(newP);
            patternField.requestFocus();
        });

        removeBtn.setOnAction(e -> {
            AppSettingsStore.UserParameterPattern sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                int idx = items.indexOf(sel);
                items.remove(sel);
                if (!items.isEmpty()) {
                    table.getSelectionModel().select(Math.min(idx, items.size() - 1));
                }
            }
        });

        upBtn.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                AppSettingsStore.UserParameterPattern p = items.remove(idx);
                items.add(idx - 1, p);
                table.getSelectionModel().select(idx - 1);
            }
        });

        downBtn.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < items.size() - 1) {
                AppSettingsStore.UserParameterPattern p = items.remove(idx);
                items.add(idx + 1, p);
                table.getSelectionModel().select(idx + 1);
            }
        });

        revertLink.setOnAction(e -> {
            enableInConsoles.setSelected(true);
            enableInLiterals.setSelected(true);
            substituteInsideStrings.setSelected(false);
            items.clear();
            for (AppSettingsStore.UserParameterPattern p : AppSettingsStore.Settings.defaultUserParameterPatterns()) {
                items.add(p.copy());
            }
            if (!items.isEmpty()) table.getSelectionModel().selectFirst();
        });

        if (!items.isEmpty()) {
            table.getSelectionModel().selectFirst();
        }

        inputs.put("enableUserParameters", enableInConsoles);
        inputs.put("enableUserParametersInLiteralsWithInjection", enableInLiterals);
        inputs.put("substituteInsideSqlStrings", substituteInsideStrings);
        inputs.put("userParameterPatternsList", items);

        VBox panel = new VBox(10, topBar, enableInConsoles, enableInLiterals, substituteInsideStrings,
                patternsLabel, listToolbar, table, detailBox);
        panel.setPadding(new Insets(6, 12, 16, 12));
        return panel;
    }

    private static HBox createSectionHeader(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text;");
        Separator sep = new Separator();
        HBox.setHgrow(sep, Priority.ALWAYS);
        sep.setStyle("-fx-opacity: 0.35;");
        HBox box = new HBox(10, label, sep);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 0, 4, 0));
        return box;
    }

    private static String formatSampleNumber(String pattern, String decSep, boolean useGroup, String groupSep) {
        try {
            double val = 123456789.123456;
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            if (decSep != null && !decSep.isEmpty()) {
                symbols.setDecimalSeparator(decSep.charAt(0));
            }
            if (useGroup && groupSep != null && !groupSep.isEmpty()) {
                symbols.setGroupingSeparator(groupSep.charAt(0));
            }
            DecimalFormat df;
            if (pattern != null && !pattern.isBlank()) {
                df = new DecimalFormat(pattern, symbols);
            } else {
                df = new DecimalFormat("#,##0.######", symbols);
            }
            df.setGroupingUsed(useGroup);
            return df.format(val);
        } catch (Exception e) {
            return "123456789.123456";
        }
    }

    private static String formatSampleDateTime(String pattern) {
        if (pattern == null || pattern.isBlank()) return "";
        try {
            ZonedDateTime now = ZonedDateTime.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern, Locale.US);
            return dtf.format(now);
        } catch (Exception e) {
            return "[Invalid format]";
        }
    }

    private static void showHelpAlert(String title, String content) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception ignored) {}
    }

    private static VBox buildDataEditorPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        // 1. General / Fetch Limits (Top section in DataGrip)
        GridPane limitsGrid = new GridPane();
        limitsGrid.setHgap(14);
        limitsGrid.setVgap(8);

        CheckBox limitPageSizeCheck = new CheckBox("Limit page size to:");
        limitPageSizeCheck.setSelected(settings.isLimitPageSize());
        TextField pageSizeField = new TextField(String.valueOf(settings.getPageSize()));
        pageSizeField.setPrefWidth(90);
        pageSizeField.disableProperty().bind(limitPageSizeCheck.selectedProperty().not());
        HBox pageSizeBox = new HBox(8, pageSizeField);
        pageSizeBox.setAlignment(Pos.CENTER_LEFT);

        limitsGrid.add(limitPageSizeCheck, 0, 0);
        limitsGrid.add(pageSizeBox, 1, 0);

        Label prefetchLabel = new Label("Result set prefetch size:");
        TextField prefetchField = new TextField(String.valueOf(settings.getResultSetPrefetchSize()));
        prefetchField.setPrefWidth(90);
        limitsGrid.add(prefetchLabel, 0, 1);
        limitsGrid.add(prefetchField, 1, 1);

        Label filterHistLabel = new Label("Filter history size:");
        TextField filterHistField = new TextField(String.valueOf(settings.getFilterHistorySize()));
        filterHistField.setPrefWidth(90);
        limitsGrid.add(filterHistLabel, 0, 2);
        limitsGrid.add(filterHistField, 1, 2);

        Label maxBytesLabel = new Label("Maximum number of bytes loaded per value:");
        TextField maxBytesField = new TextField(String.valueOf(settings.getMaxBytesLoadedPerValue()));
        maxBytesField.setPrefWidth(90);
        limitsGrid.add(maxBytesLabel, 0, 3);
        limitsGrid.add(maxBytesField, 1, 3);

        CheckBox showFirstRowsCheck = new CheckBox("Show first");
        showFirstRowsCheck.setSelected(settings.isShowFirstDataRowsInPreview());
        TextField previewDataRowsField = new TextField(String.valueOf(settings.getPreviewDataRows()));
        previewDataRowsField.setPrefWidth(60);
        previewDataRowsField.disableProperty().bind(showFirstRowsCheck.selectedProperty().not());
        Label previewSuffix = new Label("data rows in preview");
        HBox showFirstBox = new HBox(6, showFirstRowsCheck, previewDataRowsField, previewSuffix);
        showFirstBox.setAlignment(Pos.CENTER_LEFT);
        limitsGrid.add(showFirstBox, 0, 4, 2, 1);

        // 2. Controls Customization
        HBox controlsSection = createSectionHeader("Controls Customization");

        CheckBox enablePagingCheck = new CheckBox("Enable paging in in-editor results by default");
        enablePagingCheck.setSelected(settings.isEnablePagingInEditorResults());

        Label paginationPosLabel = new Label("Position of the grid pagination control:");
        ComboBox<String> paginationPosCombo = new ComboBox<>();
        paginationPosCombo.getItems().addAll(
                "Grid bottom (floating)",
                "Grid bottom left (floating)",
                "Grid bottom right (floating)",
                "Data editor toolbar"
        );
        paginationPosCombo.setValue(settings.getGridPaginationPosition());
        paginationPosCombo.setPrefWidth(220);
        HBox paginationPosRow = new HBox(10, paginationPosLabel, paginationPosCombo);
        paginationPosRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox showQuickActionsCheck = new CheckBox("Show the quick actions popup toolbar for cells");
        showQuickActionsCheck.setSelected(settings.isShowQuickActionsToolbar());

        CheckBox enableQuickActionsCustomizationCheck = new CheckBox("Enable customization of the quick actions popup toolbar");
        enableQuickActionsCustomizationCheck.setSelected(settings.isEnableQuickActionsCustomization());
        enableQuickActionsCustomizationCheck.setPadding(new Insets(0, 0, 0, 22));
        enableQuickActionsCustomizationCheck.disableProperty().bind(showQuickActionsCheck.selectedProperty().not());

        VBox controlsBox = new VBox(8, enablePagingCheck, paginationPosRow, showQuickActionsCheck, enableQuickActionsCustomizationCheck);

        // 3. Data Presentation
        HBox dataPresSection = createSectionHeader("Data Presentation");

        CheckBox useCustomFontCheck = new CheckBox("Use custom font:");
        useCustomFontCheck.setSelected(settings.isUseCustomFont());

        ComboBox<String> fontCombo = new ComboBox<>();
        List<String> families = new ArrayList<>(Font.getFamilies());
        if (!families.contains("JetBrains Mono")) families.add(0, "JetBrains Mono");
        fontCombo.getItems().addAll(families);
        fontCombo.setValue(settings.getCustomFontFamily());
        fontCombo.setPrefWidth(220);

        Label sizeLabel = new Label("Size:");
        TextField sizeField = new TextField(String.valueOf(settings.getCustomFontSize()));
        sizeField.setPrefWidth(60);

        Label lineHeightLabel = new Label("Line height:");
        TextField lineHeightField = new TextField(String.valueOf(settings.getCustomLineHeight()));
        lineHeightField.setPrefWidth(60);

        fontCombo.disableProperty().bind(useCustomFontCheck.selectedProperty().not());
        sizeField.disableProperty().bind(useCustomFontCheck.selectedProperty().not());
        lineHeightField.disableProperty().bind(useCustomFontCheck.selectedProperty().not());

        HBox fontRow = new HBox(8, useCustomFontCheck, fontCombo, sizeLabel, sizeField, lineHeightLabel, lineHeightField);
        fontRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox alternateRowColorsCheck = new CheckBox("Alternate row colors");
        alternateRowColorsCheck.setSelected(settings.isAlternateRowColors());

        Label booleanLabel = new Label("Show boolean values as:");
        ComboBox<String> booleanCombo = new ComboBox<>();
        booleanCombo.getItems().addAll("Text", "Checkboxes");
        booleanCombo.setValue(settings.getShowBooleanValuesAs());
        booleanCombo.setPrefWidth(160);
        HBox booleanRow = new HBox(12, booleanLabel, booleanCombo);
        booleanRow.setAlignment(Pos.CENTER_LEFT);

        Label transposeLabel = new Label("Automatically transpose tables:");
        ComboBox<String> transposeCombo = new ComboBox<>();
        transposeCombo.getItems().addAll("Never", "If a table has one record", "Always");
        transposeCombo.setValue(settings.getAutomaticallyTransposeTables());
        transposeCombo.setPrefWidth(200);
        HBox transposeRow = new HBox(12, transposeLabel, transposeCombo);
        transposeRow.setAlignment(Pos.CENTER_LEFT);

        Label binaryLabel = new Label("Automatically detect binary values:");
        CheckBox binaryTextCheck = new CheckBox("Text");
        binaryTextCheck.setSelected(settings.isDetectBinaryAsText());
        CheckBox binaryUuidCheck = new CheckBox("UUID");
        binaryUuidCheck.setSelected(settings.isDetectBinaryAsUuid());
        HBox binaryRow = new HBox(12, binaryLabel, binaryTextCheck, binaryUuidCheck);
        binaryRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox localFilterCheck = new CheckBox("Enable local filter by default");
        localFilterCheck.setSelected(settings.isEnableLocalFilterByDefault());

        CheckBox immediateCompletionCheck = new CheckBox("Enable immediate completion in grid text cells");
        immediateCompletionCheck.setSelected(settings.isEnableImmediateCompletionInGridTextCells());

        Label temporalTzLabel = new Label("Display temporal data in time zone:");
        TextField temporalTzField = new TextField(settings.getDisplayTemporalDataInTimeZone());
        temporalTzField.setPromptText("e.g. UTC, GMT+6, America/New_York");
        temporalTzField.setPrefWidth(240);
        HBox temporalTzRow = new HBox(12, temporalTzLabel, temporalTzField);
        temporalTzRow.setAlignment(Pos.CENTER_LEFT);

        VBox dataPresBox = new VBox(8, fontRow, alternateRowColorsCheck, booleanRow, transposeRow, binaryRow, localFilterCheck, immediateCompletionCheck, temporalTzRow);

        // 4. Custom Number Formats
        HBox numFormatSection = createSectionHeader("Custom Number Formats");

        Hyperlink numPatternsLink = new Hyperlink("Number patterns ↗");
        numPatternsLink.setStyle("-fx-text-fill: #4da3ff; -fx-padding: 0;");
        numPatternsLink.setOnAction(e -> showHelpAlert("Number Patterns",
                "Number patterns follow java.text.DecimalFormat specifications:\n\n"
                + "• 0 : Digit, zero shows as 0\n"
                + "• # : Digit, zero shows as absent\n"
                + "• . : Decimal separator position\n"
                + "• , : Grouping separator position\n\n"
                + "Examples:\n"
                + "  #,##0.00  -> 123,456,789.12\n"
                + "  0.0000    -> 123456789.1234\n"
                + "  #,###     -> 123,456,789"));

        Label decSepLabel = new Label("Decimal separator:");
        TextField decSepField = new TextField(settings.getDecimalSeparator());
        decSepField.setPrefWidth(60);
        HBox decSepRow = new HBox(12, decSepLabel, decSepField);
        decSepRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox groupingSepCheck = new CheckBox("Grouping separator:");
        groupingSepCheck.setSelected(settings.isEnableGroupingSeparator());
        TextField groupingSepField = new TextField(settings.getGroupingSeparator());
        groupingSepField.setPrefWidth(120);
        groupingSepField.disableProperty().bind(groupingSepCheck.selectedProperty().not());
        VBox groupingBox = new VBox(4, groupingSepCheck, groupingSepField);

        Label infLabel = new Label("Infinity:");
        TextField infField = new TextField(settings.getInfinityText());
        infField.setPrefWidth(140);
        HBox infRow = new HBox(12, infLabel, infField);
        infRow.setAlignment(Pos.CENTER_LEFT);

        Label nanLabel = new Label("NaN:");
        TextField nanField = new TextField(settings.getNanText());
        nanField.setPrefWidth(140);
        HBox nanRow = new HBox(12, nanLabel, nanField);
        nanRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox numPatternCheck = new CheckBox("Number pattern");
        numPatternCheck.setSelected(settings.isEnableNumberPattern());
        TextField numPatternField = new TextField(settings.getNumberPattern());
        numPatternField.setPrefWidth(180);
        numPatternField.disableProperty().bind(numPatternCheck.selectedProperty().not());

        Label numPreviewLabel = new Label();
        numPreviewLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-family: monospace;");
        Runnable updateNumPreview = () -> {
            numPreviewLabel.setText(formatSampleNumber(
                    numPatternCheck.isSelected() ? numPatternField.getText() : null,
                    decSepField.getText(),
                    groupingSepCheck.isSelected(),
                    groupingSepField.getText()
            ));
        };
        decSepField.textProperty().addListener((o, ov, nv) -> updateNumPreview.run());
        groupingSepField.textProperty().addListener((o, ov, nv) -> updateNumPreview.run());
        groupingSepCheck.selectedProperty().addListener((o, ov, nv) -> updateNumPreview.run());
        numPatternField.textProperty().addListener((o, ov, nv) -> updateNumPreview.run());
        numPatternCheck.selectedProperty().addListener((o, ov, nv) -> updateNumPreview.run());
        updateNumPreview.run();

        HBox numPatternRow = new HBox(10, numPatternField, numPreviewLabel);
        numPatternRow.setAlignment(Pos.CENTER_LEFT);
        VBox numPatternBox = new VBox(4, numPatternCheck, numPatternRow);

        VBox numFormatsBox = new VBox(8, numPatternsLink, decSepRow, groupingBox, infRow, nanRow, numPatternBox);

        // 5. Custom Date/Time Formats
        HBox dateTimeSection = createSectionHeader("Custom Date/Time Formats");

        Hyperlink datePatternsLink = new Hyperlink("Date patterns ↗");
        datePatternsLink.setStyle("-fx-text-fill: #4da3ff; -fx-padding: 0;");
        datePatternsLink.setOnAction(e -> showHelpAlert("Date Patterns",
                "Date and time patterns follow java.time.format.DateTimeFormatter:\n\n"
                + "• yyyy : 4-digit year (e.g. 2026)\n"
                + "• MM   : 2-digit month (01-12)\n"
                + "• dd   : 2-digit day (01-31)\n"
                + "• HH   : 24-hour hour (00-23)\n"
                + "• mm   : 2-digit minute (00-59)\n"
                + "• ss   : 2-digit second (00-59)\n"
                + "• Z    : Zone offset (+0600)\n"
                + "• zzz  : Zone name (e.g. GMT)"));

        // Datetime/timestamp
        CheckBox dtTsCheck = new CheckBox("Datetime/timestamp");
        dtTsCheck.setSelected(settings.isEnableDatetimeTimestamp());
        TextField dtTsField = new TextField(settings.getDatetimeTimestampPattern());
        dtTsField.setPrefWidth(200);
        dtTsField.disableProperty().bind(dtTsCheck.selectedProperty().not());
        Label dtTsPreview = new Label(formatSampleDateTime(dtTsField.getText()));
        dtTsPreview.setStyle("-fx-text-fill: -text-dim; -fx-font-family: monospace;");
        dtTsField.textProperty().addListener((o, ov, nv) -> dtTsPreview.setText(formatSampleDateTime(nv)));
        HBox dtTsRow = new HBox(10, dtTsField, dtTsPreview);
        dtTsRow.setAlignment(Pos.CENTER_LEFT);
        VBox dtTsBox = new VBox(4, dtTsCheck, dtTsRow);

        // Datetime/timestamp with time zone
        CheckBox dtTzCheck = new CheckBox("Datetime/timestamp with time zone");
        dtTzCheck.setSelected(settings.isEnableDatetimeTimestampWithZone());
        TextField dtTzField = new TextField(settings.getDatetimeTimestampWithZonePattern());
        dtTzField.setPrefWidth(200);
        dtTzField.disableProperty().bind(dtTzCheck.selectedProperty().not());
        Label dtTzPreview = new Label(formatSampleDateTime(dtTzField.getText()));
        dtTzPreview.setStyle("-fx-text-fill: -text-dim; -fx-font-family: monospace;");
        dtTzField.textProperty().addListener((o, ov, nv) -> dtTzPreview.setText(formatSampleDateTime(nv)));
        HBox dtTzRow = new HBox(10, dtTzField, dtTzPreview);
        dtTzRow.setAlignment(Pos.CENTER_LEFT);
        VBox dtTzBox = new VBox(4, dtTzCheck, dtTzRow);

        // Time
        CheckBox timeCheck = new CheckBox("Time");
        timeCheck.setSelected(settings.isEnableTime());
        TextField timeField = new TextField(settings.getTimePattern());
        timeField.setPrefWidth(200);
        timeField.disableProperty().bind(timeCheck.selectedProperty().not());
        Label timePreview = new Label(formatSampleDateTime(timeField.getText()));
        timePreview.setStyle("-fx-text-fill: -text-dim; -fx-font-family: monospace;");
        timeField.textProperty().addListener((o, ov, nv) -> timePreview.setText(formatSampleDateTime(nv)));
        HBox timeRow = new HBox(10, timeField, timePreview);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        VBox timeBox = new VBox(4, timeCheck, timeRow);

        // Time with time zone
        CheckBox timeTzCheck = new CheckBox("Time with time zone");
        timeTzCheck.setSelected(settings.isEnableTimeWithZone());
        TextField timeTzField = new TextField(settings.getTimeWithZonePattern());
        timeTzField.setPrefWidth(200);
        timeTzField.disableProperty().bind(timeTzCheck.selectedProperty().not());
        Label timeTzPreview = new Label(formatSampleDateTime(timeTzField.getText()));
        timeTzPreview.setStyle("-fx-text-fill: -text-dim; -fx-font-family: monospace;");
        timeTzField.textProperty().addListener((o, ov, nv) -> timeTzPreview.setText(formatSampleDateTime(nv)));
        HBox timeTzRow = new HBox(10, timeTzField, timeTzPreview);
        timeTzRow.setAlignment(Pos.CENTER_LEFT);
        VBox timeTzBox = new VBox(4, timeTzCheck, timeTzRow);

        // Date
        CheckBox dateCheck = new CheckBox("Date");
        dateCheck.setSelected(settings.isEnableDate());
        TextField dateField = new TextField(settings.getDatePattern());
        dateField.setPrefWidth(200);
        dateField.disableProperty().bind(dateCheck.selectedProperty().not());
        Label datePreview = new Label(formatSampleDateTime(dateField.getText()));
        datePreview.setStyle("-fx-text-fill: -text-dim; -fx-font-family: monospace;");
        dateField.textProperty().addListener((o, ov, nv) -> datePreview.setText(formatSampleDateTime(nv)));
        HBox dateRow = new HBox(10, dateField, datePreview);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        VBox dateBox = new VBox(4, dateCheck, dateRow);

        VBox dateTimeFormatsBox = new VBox(8, datePatternsLink, dtTsBox, dtTzBox, timeBox, timeTzBox, dateBox);

        // 6. Data Sorting
        HBox dataSortingSection = createSectionHeader("Data Sorting");

        CheckBox sortViaOrderByCheck = new CheckBox("Sort via ORDER BY");
        sortViaOrderByCheck.setSelected(settings.isSortViaOrderBy());

        CheckBox sortNumericPkCheck = new CheckBox("Sort tables by numeric primary key:");
        sortNumericPkCheck.setSelected(settings.isSortTablesByNumericPk());

        ToggleGroup pkDirGroup = new ToggleGroup();
        RadioButton ascRadio = new RadioButton("Ascending");
        ascRadio.setToggleGroup(pkDirGroup);
        RadioButton descRadio = new RadioButton("Descending");
        descRadio.setToggleGroup(pkDirGroup);
        if ("Descending".equalsIgnoreCase(settings.getSortTablesByNumericPkDirection())) {
            descRadio.setSelected(true);
        } else {
            ascRadio.setSelected(true);
        }
        ascRadio.disableProperty().bind(sortNumericPkCheck.selectedProperty().not());
        descRadio.disableProperty().bind(sortNumericPkCheck.selectedProperty().not());
        HBox pkSortRow = new HBox(14, ascRadio, descRadio);
        pkSortRow.setPadding(new Insets(0, 0, 0, 22));

        Label addColsLabel = new Label("Add columns to sorting:");
        ToggleGroup addColsGroup = new ToggleGroup();
        RadioButton altClickRadio = new RadioButton("⌥Click");
        altClickRadio.setToggleGroup(addColsGroup);
        RadioButton clickRadio = new RadioButton("Click");
        clickRadio.setToggleGroup(addColsGroup);
        if ("Click".equalsIgnoreCase(settings.getAddColumnsToSorting())) {
            clickRadio.setSelected(true);
        } else {
            altClickRadio.setSelected(true);
        }
        HBox addColsRow = new HBox(14, altClickRadio, clickRadio);

        VBox sortingBox = new VBox(8, sortViaOrderByCheck, sortNumericPkCheck, pkSortRow, addColsLabel, addColsRow);

        // 7. Data Modification
        HBox modificationSection = createSectionHeader("Data Modification");

        CheckBox submitImmediatelyCheck = new CheckBox("Submit changes immediately");
        submitImmediatelyCheck.setSelected(settings.isSubmitChangesImmediately());

        CheckBox enableEditingJoinCheck = new CheckBox("Enable editing for queries with JOIN clauses");
        enableEditingJoinCheck.setSelected(settings.isEnableEditingForQueriesWithJoin());

        CheckBox showDmlPreviewJoinCheck = new CheckBox("Show DML preview before submitting changes for queries with JOIN clauses");
        showDmlPreviewJoinCheck.setSelected(settings.isShowDmlPreviewForQueriesWithJoin());
        showDmlPreviewJoinCheck.disableProperty().bind(enableEditingJoinCheck.selectedProperty().not());

        VBox modificationBox = new VBox(8, submitImmediatelyCheck, enableEditingJoinCheck, showDmlPreviewJoinCheck);

        // 8. URL Click Settings
        HBox urlSection = createSectionHeader("URL Click Settings");

        Label allowOpeningLabel = new Label("Allow opening:");
        CheckBox secureLinksCheck = new CheckBox("Secure links (HTTPS)");
        secureLinksCheck.setSelected(settings.isAllowOpenSecureLinks());
        CheckBox standardLinksCheck = new CheckBox("Standard links (HTTP)");
        standardLinksCheck.setSelected(settings.isAllowOpenStandardLinks());
        CheckBox localFileLinksCheck = new CheckBox("Local file links");
        localFileLinksCheck.setSelected(settings.isAllowOpenLocalFileLinks());
        VBox linksBox = new VBox(6, secureLinksCheck, standardLinksCheck, localFileLinksCheck);
        linksBox.setPadding(new Insets(0, 0, 0, 22));

        CheckBox assumeHttpCheck = new CheckBox("If no protocol is specified, assume HTTP for URLs");
        assumeHttpCheck.setSelected(settings.isAssumeHttpIfNoProtocol());

        VBox urlBox = new VBox(8, allowOpeningLabel, linksBox, assumeHttpCheck);

        // Store into input map for persistence on OK / Apply
        inputs.put("dataEditor_limitPageSizeCheck", limitPageSizeCheck);
        inputs.put("dataEditor_pageSizeField", pageSizeField);
        inputs.put("dataEditor_prefetchField", prefetchField);
        inputs.put("dataEditor_filterHistField", filterHistField);
        inputs.put("dataEditor_maxBytesField", maxBytesField);
        inputs.put("dataEditor_showFirstRowsCheck", showFirstRowsCheck);
        inputs.put("dataEditor_previewDataRowsField", previewDataRowsField);
        inputs.put("dataEditor_enablePagingCheck", enablePagingCheck);
        inputs.put("dataEditor_paginationPosCombo", paginationPosCombo);
        inputs.put("dataEditor_showQuickActionsCheck", showQuickActionsCheck);
        inputs.put("dataEditor_enableQuickActionsCustomizationCheck", enableQuickActionsCustomizationCheck);
        inputs.put("dataEditor_useCustomFontCheck", useCustomFontCheck);
        inputs.put("dataEditor_fontCombo", fontCombo);
        inputs.put("dataEditor_sizeField", sizeField);
        inputs.put("dataEditor_lineHeightField", lineHeightField);
        inputs.put("dataEditor_alternateRowColorsCheck", alternateRowColorsCheck);
        inputs.put("dataEditor_booleanCombo", booleanCombo);
        inputs.put("dataEditor_transposeCombo", transposeCombo);
        inputs.put("dataEditor_binaryTextCheck", binaryTextCheck);
        inputs.put("dataEditor_binaryUuidCheck", binaryUuidCheck);
        inputs.put("dataEditor_localFilterCheck", localFilterCheck);
        inputs.put("dataEditor_immediateCompletionCheck", immediateCompletionCheck);
        inputs.put("dataEditor_temporalTzField", temporalTzField);
        inputs.put("dataEditor_decSepField", decSepField);
        inputs.put("dataEditor_groupingSepCheck", groupingSepCheck);
        inputs.put("dataEditor_groupingSepField", groupingSepField);
        inputs.put("dataEditor_infField", infField);
        inputs.put("dataEditor_nanField", nanField);
        inputs.put("dataEditor_numPatternCheck", numPatternCheck);
        inputs.put("dataEditor_numPatternField", numPatternField);
        inputs.put("dataEditor_dtTsCheck", dtTsCheck);
        inputs.put("dataEditor_dtTsField", dtTsField);
        inputs.put("dataEditor_dtTzCheck", dtTzCheck);
        inputs.put("dataEditor_dtTzField", dtTzField);
        inputs.put("dataEditor_timeCheck", timeCheck);
        inputs.put("dataEditor_timeField", timeField);
        inputs.put("dataEditor_timeTzCheck", timeTzCheck);
        inputs.put("dataEditor_timeTzField", timeTzField);
        inputs.put("dataEditor_dateCheck", dateCheck);
        inputs.put("dataEditor_dateField", dateField);
        inputs.put("dataEditor_sortViaOrderByCheck", sortViaOrderByCheck);
        inputs.put("dataEditor_sortNumericPkCheck", sortNumericPkCheck);
        inputs.put("dataEditor_pkDirGroup", pkDirGroup);
        inputs.put("dataEditor_addColsGroup", addColsGroup);
        inputs.put("dataEditor_submitImmediatelyCheck", submitImmediatelyCheck);
        inputs.put("dataEditor_enableEditingJoinCheck", enableEditingJoinCheck);
        inputs.put("dataEditor_showDmlPreviewJoinCheck", showDmlPreviewJoinCheck);
        inputs.put("dataEditor_secureLinksCheck", secureLinksCheck);
        inputs.put("dataEditor_standardLinksCheck", standardLinksCheck);
        inputs.put("dataEditor_localFileLinksCheck", localFileLinksCheck);
        inputs.put("dataEditor_assumeHttpCheck", assumeHttpCheck);

        VBox panel = new VBox(12,
                limitsGrid,
                controlsSection, controlsBox,
                dataPresSection, dataPresBox,
                numFormatSection, numFormatsBox,
                dateTimeSection, dateTimeFormatsBox,
                dataSortingSection, sortingBox,
                modificationSection, modificationBox,
                urlSection, urlBox
        );
        panel.setPadding(new Insets(6, 12, 28, 12));
        return panel;
    }

    public static VBox buildDatabaseExplorerPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        // Section: Filter
        HBox filterSection = buildTitledSectionLine("Filter");

        CheckBox rememberFilter = new CheckBox("Remember whether the filter is ON");
        rememberFilter.setSelected(settings.isRememberFilterState());
        inputs.put("dbExplorer_rememberFilterState", rememberFilter);

        // Keep showEmpty registered for compatibility
        CheckBox showEmpty = new CheckBox("Show empty schemas");
        showEmpty.setSelected(settings.isShowEmptySchemas());
        inputs.put("showEmpty", showEmpty);

        // Section: Colors
        HBox colorsSection = buildTitledSectionLine("Colors");

        CheckBox showColors = new CheckBox("Show database colors");
        showColors.setSelected(settings.isShowDatabaseColors());
        inputs.put("dbExplorer_showDatabaseColors", showColors);

        Label colorsSubtext = new Label("Select where to show colors assigned to data sources and database objects");
        colorsSubtext.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

        CheckBox dbExplorerColor = new CheckBox("Database Explorer");
        dbExplorerColor.setSelected(settings.isColorDatabaseExplorer());
        inputs.put("dbExplorer_colorDatabaseExplorer", dbExplorerColor);

        CheckBox editorTabsColor = new CheckBox("Editor tab headers");
        editorTabsColor.setSelected(settings.isColorEditorTabHeaders());
        inputs.put("dbExplorer_colorEditorTabHeaders", editorTabsColor);

        CheckBox editorBgColor = new CheckBox("Editor backgrounds");
        editorBgColor.setSelected(settings.isColorEditorBackgrounds());
        inputs.put("dbExplorer_colorEditorBackgrounds", editorBgColor);

        CheckBox editorToolbarsColor = new CheckBox("Editor toolbars");
        editorToolbarsColor.setSelected(settings.isColorEditorToolbars());
        inputs.put("dbExplorer_colorEditorToolbars", editorToolbarsColor);

        VBox subColors = new VBox(8, dbExplorerColor, editorTabsColor, editorBgColor, editorToolbarsColor);
        subColors.setPadding(new Insets(2, 0, 0, 24));
        subColors.disableProperty().bind(showColors.selectedProperty().not());

        VBox colorsBox = new VBox(6, showColors, colorsSubtext, subColors);

        VBox panel = new VBox(10, filterSection, rememberFilter, colorsSection, colorsBox);
        panel.setPadding(new Insets(6, 12, 28, 12));
        return panel;
    }

    public static VBox buildAiToolsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        // Section: Permissions
        HBox permSection = buildTitledSectionLine("Permissions");

        Label overview = new Label("Controls whether AI tools can read or modify data and schema in all databases without confirmation");
        overview.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");
        overview.setWrapText(true);

        // 1. Read database schemas
        CheckBox readSchemas = new CheckBox("Read database schemas");
        readSchemas.setSelected(settings.isAiReadDatabaseSchemas());
        inputs.put("ai_readDatabaseSchemas", readSchemas);
        Label readSchemasDesc = new Label("Improves query generation quality. Note: this also results in higher quota consumption.");
        readSchemasDesc.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        readSchemasDesc.setPadding(new Insets(0, 0, 0, 22));
        VBox readSchemasBox = new VBox(3, readSchemas, readSchemasDesc);

        // 2. Modify database schemas
        CheckBox modifySchemas = new CheckBox("Modify database schemas");
        modifySchemas.setSelected(settings.isAiModifyDatabaseSchemas());
        inputs.put("ai_modifyDatabaseSchemas", modifySchemas);
        Label modifySchemasDesc = new Label("For example, by running CREATE, ALTER, or DROP statements");
        modifySchemasDesc.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        modifySchemasDesc.setPadding(new Insets(0, 0, 0, 22));
        VBox modifySchemasBox = new VBox(3, modifySchemas, modifySchemasDesc);

        // 3. Read database data
        CheckBox readData = new CheckBox("Read database data");
        readData.setSelected(settings.isAiReadDatabaseData());
        inputs.put("ai_readDatabaseData", readData);
        Label readDataDesc = new Label("For example, by running SELECT queries");
        readDataDesc.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        readDataDesc.setPadding(new Insets(0, 0, 0, 22));
        VBox readDataBox = new VBox(3, readData, readDataDesc);

        // 4. Modify database data
        CheckBox modifyData = new CheckBox("Modify database data");
        modifyData.setSelected(settings.isAiModifyDatabaseData());
        inputs.put("ai_modifyDatabaseData", modifyData);
        Label modifyDataDesc = new Label("For example, by running INSERT, UPDATE, or DELETE statements or calling routines");
        modifyDataDesc.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        modifyDataDesc.setPadding(new Insets(0, 0, 0, 22));
        VBox modifyDataBox = new VBox(3, modifyData, modifyDataDesc);

        VBox permissionsBox = new VBox(14, readSchemasBox, modifySchemasBox, readDataBox, modifyDataBox);
        permissionsBox.setPadding(new Insets(6, 0, 0, 0));

        VBox panel = new VBox(10, permSection, overview, permissionsBox);
        panel.setPadding(new Insets(6, 12, 28, 12));
        return panel;
    }

    public static VBox buildQueryFilesAndConsolesPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        // Section 1: Query Files Presentation
        HBox presSection = buildTitledSectionLine("Query Files Presentation");

        CheckBox showDsName = new CheckBox("Show data source name in file tree");
        showDsName.setSelected(settings.isShowDataSourceNameInFileTree());
        inputs.put("queryFiles_showDataSourceName", showDsName);

        CheckBox useColor = new CheckBox("Use attached search path color in file tree");
        useColor.setSelected(settings.isUseAttachedSearchPathColorInFileTree());
        inputs.put("queryFiles_useAttachedSearchPathColor", useColor);

        CheckBox useIcon = new CheckBox("Use attached data source icon for query files");
        useIcon.setSelected(settings.isUseAttachedDataSourceIconForQueryFiles());
        inputs.put("queryFiles_useAttachedDataSourceIcon", useIcon);

        VBox presBox = new VBox(8, showDsName, useColor, useIcon);

        // Section 2: Query Consoles
        HBox consolesSection = buildTitledSectionLine("Query Consoles");

        Label defFileNameLabel = new Label("Default file name:");
        defFileNameLabel.setMinWidth(120);
        defFileNameLabel.setStyle("-fx-text-fill: -text;");

        TextField defFileNameField = new TextField(settings.getDefaultConsoleFileName());
        HBox.setHgrow(defFileNameField, Priority.ALWAYS);
        defFileNameField.setMaxWidth(Double.MAX_VALUE);
        inputs.put("queryFiles_defaultConsoleFileName", defFileNameField);

        Hyperlink resetDefNameLink = new Hyperlink("Reset");
        resetDefNameLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-padding: 0 4; -fx-border-width: 0;");
        resetDefNameLink.setOnAction(e -> defFileNameField.setText("console"));

        HBox consoleRow = new HBox(8, defFileNameLabel, defFileNameField, resetDefNameLink);
        consoleRow.setAlignment(Pos.CENTER_LEFT);

        // Section 3: Editor Tab Display Names
        HBox editorTabsSection = buildTitledSectionLine("Editor Tab Display Names");

        Label editorTabDesc = new Label("Customize how query console names are displayed in editor tab headers");
        editorTabDesc.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

        Label tplLabel = new Label("Template:");
        tplLabel.setMinWidth(120);
        tplLabel.setStyle("-fx-text-fill: -text;");

        TextField tplField = new TextField(settings.getEditorTabTitleTemplate());
        HBox.setHgrow(tplField, Priority.ALWAYS);
        tplField.setMaxWidth(Double.MAX_VALUE);
        inputs.put("queryFiles_editorTabTitleTemplate", tplField);

        Hyperlink resetTplLink = new Hyperlink("Reset");
        resetTplLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-padding: 0 4; -fx-border-width: 0;");
        resetTplLink.setOnAction(e -> tplField.setText("$NAME$ [$DATASOURCE$]"));

        HBox tplRow = new HBox(8, tplLabel, tplField, resetTplLink);
        tplRow.setAlignment(Pos.CENTER_LEFT);

        // Token Pills
        String[] tokens = {"$NAME$", "$DATASOURCE$", "$SEARCH_PATH$", "$DATABASE$", "$SCHEMA$"};
        HBox tokensRow = new HBox(6);
        tokensRow.setAlignment(Pos.CENTER_LEFT);
        tokensRow.setPadding(new Insets(2, 0, 4, 128));

        for (String tok : tokens) {
            Button pill = new Button(tok);
            pill.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #dfe1e5; -fx-font-family: monospace; "
                    + "-fx-font-size: 11px; -fx-border-color: #4e5157; -fx-border-radius: 10; "
                    + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-cursor: hand;");
            pill.setOnAction(e -> {
                int caret = tplField.getCaretPosition();
                String cur = tplField.getText() != null ? tplField.getText() : "";
                if (caret >= 0 && caret <= cur.length()) {
                    tplField.setText(cur.substring(0, caret) + tok + cur.substring(caret));
                    tplField.positionCaret(caret + tok.length());
                } else {
                    tplField.setText(cur + tok);
                    tplField.positionCaret(tplField.getText().length());
                }
                tplField.requestFocus();
            });
            tokensRow.getChildren().add(pill);
        }

        Label helpIcon = new Label("(?)");
        helpIcon.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px; -fx-cursor: hand;");
        helpIcon.setTooltip(new Tooltip("Tokens:\n$NAME$ - Console or file name\n$DATASOURCE$ - Data source name\n$SEARCH_PATH$ - Active search path\n$DATABASE$ - Database name\n$SCHEMA$ - Schema name"));
        tokensRow.getChildren().add(helpIcon);

        // Preview Row
        Label prevLabel = new Label("Preview:");
        prevLabel.setMinWidth(120);
        prevLabel.setStyle("-fx-text-fill: -text;");

        Label prevIcon = new Label("\uD83D\uDDA5");
        prevIcon.setStyle("-fx-font-size: 12px;");
        Label prevText = new Label();
        prevText.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-text-fill: #dfe1e5;");

        HBox prevBadge = new HBox(6, prevIcon, prevText);
        prevBadge.setAlignment(Pos.CENTER_LEFT);
        prevBadge.setStyle("-fx-background-color: #212327; -fx-border-color: #3e4146; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 3 10;");

        Runnable updatePreview = () -> {
            prevText.setText(resolveEditorTabTitle(tplField.getText(), "query.sql", "PostgreSQL", "public", "postgres", "public"));
        };
        tplField.textProperty().addListener((obs, o, n) -> updatePreview.run());
        updatePreview.run();

        HBox previewRow = new HBox(8, prevLabel, prevBadge);
        previewRow.setAlignment(Pos.CENTER_LEFT);

        // Checkbox: Use this template for query files
        CheckBox useTplForFiles = new CheckBox("Use this template for query files");
        useTplForFiles.setSelected(settings.isUseTemplateForQueryFiles());
        inputs.put("queryFiles_useTemplateForQueryFiles", useTplForFiles);

        VBox panel = new VBox(10,
                presSection, presBox,
                consolesSection, consoleRow,
                editorTabsSection, editorTabDesc, tplRow, tokensRow, previewRow,
                useTplForFiles
        );
        panel.setPadding(new Insets(6, 12, 28, 12));
        return panel;
    }

    public static String resolveEditorTabTitle(String template, String name, String dataSource, String searchPath, String database, String schema) {
        if (template == null || template.isBlank()) {
            return (name != null && !name.isBlank()) ? name : "console";
        }
        String res = template;
        res = res.replace("$NAME$", (name != null && !name.isBlank()) ? name : "query.sql");
        res = res.replace("$DATASOURCE$", (dataSource != null && !dataSource.isBlank()) ? dataSource : "PostgreSQL");
        res = res.replace("$SEARCH_PATH$", (searchPath != null && !searchPath.isBlank()) ? searchPath : "public");
        res = res.replace("$DATABASE$", (database != null && !database.isBlank()) ? database : "postgres");
        res = res.replace("$SCHEMA$", (schema != null && !schema.isBlank()) ? schema : "public");
        return res;
    }

    public static VBox buildSqlDialectsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        // Global SQL Dialect
        Label globalLabel = new Label("Global SQL Dialect:");
        globalLabel.setMinWidth(150);
        globalLabel.setStyle("-fx-text-fill: -text;");

        ComboBox<String> globalCombo = new ComboBox<>(FXCollections.observableArrayList(AppSettingsStore.Settings.defaultDialectList()));
        globalCombo.setValue(settings.getGlobalSqlDialect() != null ? settings.getGlobalSqlDialect() : "<None>");
        globalCombo.setPrefWidth(240);
        inputs.put("sqlDialects_globalSqlDialect", globalCombo);

        HBox globalRow = new HBox(12, globalLabel, globalCombo);
        globalRow.setAlignment(Pos.CENTER_LEFT);

        // Project SQL Dialect
        Label projectLabel = new Label("Project SQL Dialect:");
        projectLabel.setMinWidth(150);
        projectLabel.setStyle("-fx-text-fill: -text;");

        ComboBox<String> projectCombo = new ComboBox<>(FXCollections.observableArrayList(AppSettingsStore.Settings.defaultDialectList()));
        projectCombo.setValue(settings.getProjectSqlDialect() != null ? settings.getProjectSqlDialect() : "<None>");
        projectCombo.setPrefWidth(240);
        inputs.put("sqlDialects_projectSqlDialect", projectCombo);

        HBox projectRow = new HBox(12, projectLabel, projectCombo);
        projectRow.setAlignment(Pos.CENTER_LEFT);

        // Table toolbar
        Button addBtn = new Button("+");
        addBtn.setPrefWidth(28);
        Button removeBtn = new Button("—");
        removeBtn.setPrefWidth(28);
        Button editBtn = new Button("✎");
        editBtn.setPrefWidth(28);

        HBox toolbar = new HBox(4, addBtn, removeBtn, editBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 0, 2, 0));

        // Mappings Table
        ObservableList<AppSettingsStore.SqlDialectMappingConfig> mappingsList = FXCollections.observableArrayList();
        if (settings.getSqlDialectMappings() != null) {
            for (AppSettingsStore.SqlDialectMappingConfig m : settings.getSqlDialectMappings()) {
                mappingsList.add(m.copy());
            }
        }
        inputs.put("sqlDialects_mappings", mappingsList);

        TableView<AppSettingsStore.SqlDialectMappingConfig> table = new TableView<>(mappingsList);
        table.setPlaceholder(new Label("New Mapping Alt+Insert"));
        table.setPrefHeight(280);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<AppSettingsStore.SqlDialectMappingConfig, String> pathCol = new TableColumn<>("Path ^");
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPath()));
        pathCol.setPrefWidth(340);

        TableColumn<AppSettingsStore.SqlDialectMappingConfig, String> dialectCol = new TableColumn<>("SQL Dialect");
        dialectCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDialect()));
        dialectCol.setPrefWidth(240);

        table.getColumns().addAll(pathCol, dialectCol);

        addBtn.setOnAction(e -> {
            Dialog<AppSettingsStore.SqlDialectMappingConfig> dialog = new Dialog<>();
            dialog.setTitle("New SQL Dialect Mapping");
            dialog.setHeaderText("Specify file/directory path and SQL dialect");

            ButtonType okType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

            TextField pathField = new TextField();
            pathField.setPromptText("Project or file path (e.g. /src/sql)");
            pathField.setPrefWidth(300);

            ComboBox<String> dCombo = new ComboBox<>(FXCollections.observableArrayList(AppSettingsStore.Settings.defaultDialectList()));
            dCombo.setValue("Generic SQL");
            dCombo.setPrefWidth(300);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));
            grid.add(new Label("Path:"), 0, 0);
            grid.add(pathField, 1, 0);
            grid.add(new Label("Dialect:"), 0, 1);
            grid.add(dCombo, 1, 1);

            dialog.getDialogPane().setContent(grid);
            dialog.setResultConverter(b -> {
                if (b == okType && pathField.getText() != null && !pathField.getText().isBlank()) {
                    return new AppSettingsStore.SqlDialectMappingConfig(pathField.getText().trim(), dCombo.getValue());
                }
                return null;
            });
            dialog.showAndWait().ifPresent(mappingsList::add);
        });

        removeBtn.setOnAction(e -> {
            AppSettingsStore.SqlDialectMappingConfig sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) mappingsList.remove(sel);
        });

        editBtn.setOnAction(e -> {
            AppSettingsStore.SqlDialectMappingConfig sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Dialog<AppSettingsStore.SqlDialectMappingConfig> dialog = new Dialog<>();
                dialog.setTitle("Edit SQL Dialect Mapping");
                ButtonType okType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

                TextField pathField = new TextField(sel.getPath());
                ComboBox<String> dCombo = new ComboBox<>(FXCollections.observableArrayList(AppSettingsStore.Settings.defaultDialectList()));
                dCombo.setValue(sel.getDialect());

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(10));
                grid.add(new Label("Path:"), 0, 0);
                grid.add(pathField, 1, 0);
                grid.add(new Label("Dialect:"), 0, 1);
                grid.add(dCombo, 1, 1);

                dialog.getDialogPane().setContent(grid);
                dialog.setResultConverter(b -> {
                    if (b == okType && pathField.getText() != null && !pathField.getText().isBlank()) {
                        sel.setPath(pathField.getText().trim());
                        sel.setDialect(dCombo.getValue());
                        table.refresh();
                        return sel;
                    }
                    return null;
                });
                dialog.showAndWait();
            }
        });

        // Bottom Help Text
        Label helpText = new Label("To change SQL dialect DataGrip uses for a file, a directory, or the entire project, add its path if necessary and then choose a dialect from the drop-down list. Advanced coding assistance may not be available for Generic SQL dialect.");
        helpText.setWrapText(true);
        helpText.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-line-spacing: 2;");
        helpText.setPadding(new Insets(6, 0, 0, 0));

        VBox panel = new VBox(8, globalRow, projectRow, toolbar, table, helpText);
        panel.setPadding(new Insets(6, 12, 28, 12));
        return panel;
    }

    public static VBox buildSqlResolutionScopesPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        // Top row with Project mapping and Revert changes link
        Label projectMapLabel = new Label("Project mapping:");
        projectMapLabel.setMinWidth(120);
        projectMapLabel.setStyle("-fx-text-fill: -text;");

        Button scopeDropdownBtn = new Button(settings.getProjectResolutionScope());
        scopeDropdownBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #dfe1e5; -fx-border-color: #4e5157; "
                + "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10; -fx-cursor: hand;");
        scopeDropdownBtn.setMinWidth(220);
        inputs.put("sqlResolution_projectScope", scopeDropdownBtn);

        Hyperlink revertLink = new Hyperlink("Revert changes");
        revertLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-border-width: 0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(8, projectMapLabel, scopeDropdownBtn, spacer, revertLink);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Mappings Table
        ObservableList<AppSettingsStore.SqlResolutionScopeMappingConfig> mappingsList = FXCollections.observableArrayList();
        if (settings.getSqlResolutionScopeMappings() != null) {
            for (AppSettingsStore.SqlResolutionScopeMappingConfig m : settings.getSqlResolutionScopeMappings()) {
                mappingsList.add(m.copy());
            }
        }
        inputs.put("sqlResolution_mappings", mappingsList);

        revertLink.setOnAction(e -> {
            scopeDropdownBtn.setText("<Default> (<Everything>)");
            mappingsList.clear();
        });

        // Interactive Tree Popup for Project Mapping
        Popup treePopup = new Popup();
        treePopup.setAutoHide(true);

        VBox popupContent = new VBox(6);
        popupContent.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; "
                + "-fx-background-radius: 4; -fx-padding: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 2);");
        popupContent.setPrefWidth(320);
        popupContent.setPrefHeight(280);

        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle("-fx-font-size: 11px; -fx-padding: 2 6;");
        Button expandBtn = new Button("↕");
        expandBtn.setStyle("-fx-font-size: 11px; -fx-padding: 2 6;");
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 2 6;");
        closeBtn.setOnAction(ev -> treePopup.hide());

        Region popSpacer = new Region();
        HBox.setHgrow(popSpacer, Priority.ALWAYS);
        HBox popHeader = new HBox(4, popSpacer, refreshBtn, expandBtn, closeBtn);
        popHeader.setAlignment(Pos.CENTER_RIGHT);

        CheckBoxTreeItem<String> rootItem = new CheckBoxTreeItem<>("All Data Sources");
        rootItem.setExpanded(true);
        rootItem.setSelected(settings.getProjectResolutionScope().contains("All Data Sources")
                || settings.getProjectResolutionScope().contains("<Default>"));

        List<ConnectionProfile> profiles = ConnectionStore.load();
        if (profiles.isEmpty()) {
            // Default sample matching DataGrip screenshot
            CheckBoxTreeItem<String> dsItem = new CheckBoxTreeItem<>("\uD83D\uDC18 postgres@localhost");
            dsItem.setExpanded(true);
            dsItem.getChildren().add(new CheckBoxTreeItem<>("All Databases"));
            dsItem.getChildren().add(new CheckBoxTreeItem<>("erpdb"));
            dsItem.getChildren().add(new CheckBoxTreeItem<>("nexadb"));
            dsItem.getChildren().add(new CheckBoxTreeItem<>("postgres"));
            rootItem.getChildren().add(dsItem);
        } else {
            for (ConnectionProfile cp : profiles) {
                String icon = cp.getType() != null && cp.getType().isRelational() ? "\uD83D\uDDA5 " : "\uD83D\uDCC1 ";
                CheckBoxTreeItem<String> dsItem = new CheckBoxTreeItem<>(icon + cp.getName());
                dsItem.setExpanded(true);
                dsItem.getChildren().add(new CheckBoxTreeItem<>("All Databases"));
                if (cp.getDatabase() != null && !cp.getDatabase().isBlank()) {
                    dsItem.getChildren().add(new CheckBoxTreeItem<>(cp.getDatabase()));
                }
                rootItem.getChildren().add(dsItem);
            }
        }

        TreeView<String> scopeTree = new TreeView<>(rootItem);
        scopeTree.setCellFactory(javafx.scene.control.cell.CheckBoxTreeCell.forTreeView());
        scopeTree.setShowRoot(true);
        VBox.setVgrow(scopeTree, Priority.ALWAYS);

        expandBtn.setOnAction(ev -> {
            boolean exp = !rootItem.isExpanded();
            rootItem.setExpanded(exp);
            for (TreeItem<String> c : rootItem.getChildren()) c.setExpanded(exp);
        });

        refreshBtn.setOnAction(ev -> {
            rootItem.getChildren().clear();
            List<ConnectionProfile> reloaded = ConnectionStore.load();
            if (reloaded.isEmpty()) {
                CheckBoxTreeItem<String> dsItem = new CheckBoxTreeItem<>("\uD83D\uDC18 postgres@localhost");
                dsItem.setExpanded(true);
                dsItem.getChildren().add(new CheckBoxTreeItem<>("All Databases"));
                dsItem.getChildren().add(new CheckBoxTreeItem<>("erpdb"));
                dsItem.getChildren().add(new CheckBoxTreeItem<>("nexadb"));
                dsItem.getChildren().add(new CheckBoxTreeItem<>("postgres"));
                rootItem.getChildren().add(dsItem);
            } else {
                for (ConnectionProfile cp : reloaded) {
                    CheckBoxTreeItem<String> dsItem = new CheckBoxTreeItem<>("\uD83D\uDDA5 " + cp.getName());
                    dsItem.setExpanded(true);
                    dsItem.getChildren().add(new CheckBoxTreeItem<>("All Databases"));
                    if (cp.getDatabase() != null && !cp.getDatabase().isBlank()) {
                        dsItem.getChildren().add(new CheckBoxTreeItem<>(cp.getDatabase()));
                    }
                    rootItem.getChildren().add(dsItem);
                }
            }
        });

        scopeTree.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                if (newV == rootItem) {
                    scopeDropdownBtn.setText("<Default> (<Everything>)");
                } else {
                    scopeDropdownBtn.setText(newV.getValue().replaceAll("^[\\p{So}\\p{Sk}\\s]+", ""));
                }
            }
        });

        popupContent.getChildren().addAll(popHeader, scopeTree);
        treePopup.getContent().add(popupContent);

        scopeDropdownBtn.setOnAction(e -> {
            if (!treePopup.isShowing()) {
                javafx.geometry.Point2D pt = scopeDropdownBtn.localToScreen(0, scopeDropdownBtn.getHeight());
                if (pt != null) {
                    treePopup.show(scopeDropdownBtn, pt.getX(), pt.getY());
                }
            } else {
                treePopup.hide();
            }
        });

        // Toolbar
        Button addBtn = new Button("+");
        addBtn.setPrefWidth(28);
        Button removeBtn = new Button("—");
        removeBtn.setPrefWidth(28);
        Button editBtn = new Button("✎");
        editBtn.setPrefWidth(28);

        HBox toolbar = new HBox(4, addBtn, removeBtn, editBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 0, 2, 0));

        TableView<AppSettingsStore.SqlResolutionScopeMappingConfig> table = new TableView<>(mappingsList);
        table.setPlaceholder(new Label("New Mapping Alt+Insert"));
        table.setPrefHeight(280);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<AppSettingsStore.SqlResolutionScopeMappingConfig, String> pathCol = new TableColumn<>("Path ^");
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPath()));
        pathCol.setPrefWidth(340);

        TableColumn<AppSettingsStore.SqlResolutionScopeMappingConfig, String> scopeCol = new TableColumn<>("Resolution Scope");
        scopeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getScope()));
        scopeCol.setPrefWidth(240);

        table.getColumns().addAll(pathCol, scopeCol);

        addBtn.setOnAction(e -> {
            Dialog<AppSettingsStore.SqlResolutionScopeMappingConfig> dialog = new Dialog<>();
            dialog.setTitle("New Resolution Scope Mapping");
            ButtonType okType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

            TextField pathField = new TextField();
            pathField.setPromptText("Project or file path (e.g. /src/queries)");
            pathField.setPrefWidth(300);

            TextField scField = new TextField("<Default> (<Everything>)");
            scField.setPrefWidth(300);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));
            grid.add(new Label("Path:"), 0, 0);
            grid.add(pathField, 1, 0);
            grid.add(new Label("Scope:"), 0, 1);
            grid.add(scField, 1, 1);

            dialog.getDialogPane().setContent(grid);
            dialog.setResultConverter(b -> {
                if (b == okType && pathField.getText() != null && !pathField.getText().isBlank()) {
                    return new AppSettingsStore.SqlResolutionScopeMappingConfig(pathField.getText().trim(), scField.getText().trim());
                }
                return null;
            });
            dialog.showAndWait().ifPresent(mappingsList::add);
        });

        removeBtn.setOnAction(e -> {
            AppSettingsStore.SqlResolutionScopeMappingConfig sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) mappingsList.remove(sel);
        });

        editBtn.setOnAction(e -> {
            AppSettingsStore.SqlResolutionScopeMappingConfig sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Dialog<AppSettingsStore.SqlResolutionScopeMappingConfig> dialog = new Dialog<>();
                dialog.setTitle("Edit Resolution Scope Mapping");
                ButtonType okType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

                TextField pathField = new TextField(sel.getPath());
                TextField scField = new TextField(sel.getScope());

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(10));
                grid.add(new Label("Path:"), 0, 0);
                grid.add(pathField, 1, 0);
                grid.add(new Label("Scope:"), 0, 1);
                grid.add(scField, 1, 1);

                dialog.getDialogPane().setContent(grid);
                dialog.setResultConverter(b -> {
                    if (b == okType && pathField.getText() != null && !pathField.getText().isBlank()) {
                        sel.setPath(pathField.getText().trim());
                        sel.setScope(scField.getText().trim());
                        table.refresh();
                        return sel;
                    }
                    return null;
                });
                dialog.showAndWait();
            }
        });

        // Bottom Help Text
        Label helpText = new Label("To configure the unqualified SQL names resolution for a file, a directory, or the entire project, add its path if necessary and then choose the desired data sources, databases and schemas in the drop-down.");
        helpText.setWrapText(true);
        helpText.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-line-spacing: 2;");
        helpText.setPadding(new Insets(6, 0, 0, 0));

        VBox panel = new VBox(8, topRow, toolbar, table, helpText);
        panel.setPadding(new Insets(6, 12, 28, 12));
        return panel;
    }

    public static VBox buildDatabaseOtherPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        // Section: Modify Object
        HBox modObjSection = buildTitledSectionLine("Modify Object");
        CheckBox confirmCancelCheck = new CheckBox("Confirm cancellation for dialogs that modify schema");
        confirmCancelCheck.setSelected(settings.isConfirmCancellationForModifySchemaDialogs());
        inputs.put("other_confirmCancellation", confirmCancelCheck);

        // Section: Refactoring
        HBox refactorSection = buildTitledSectionLine("Refactoring");
        CheckBox previewScriptCheck = new CheckBox("Show preview of valid script when updating source text");
        previewScriptCheck.setSelected(settings.isShowPreviewOfValidScriptWhenUpdatingSource());
        inputs.put("other_showPreviewOfValidScript", previewScriptCheck);

        // Section: DDL Mappings
        HBox ddlSection = buildTitledSectionLine("DDL Mappings");
        CheckBox suggestDdlCheck = new CheckBox("Suggest dumping DDL for new mappings");
        suggestDdlCheck.setSelected(settings.isSuggestDumpingDdlForNewMappings());
        inputs.put("other_suggestDumpingDdl", suggestDdlCheck);

        // Section: Code Generation
        HBox codeGenSection = buildTitledSectionLine("Code Generation");
        Label codeGenLabel = new Label("Generate context templates:");
        codeGenLabel.setMinWidth(180);
        codeGenLabel.setStyle("-fx-text-fill: -text;");

        ComboBox<String> codeGenCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Append to existing console",
                "Open in new console",
                "Ask"
        ));
        codeGenCombo.setValue(settings.getGenerateContextTemplates() != null ? settings.getGenerateContextTemplates() : "Append to existing console");
        codeGenCombo.setPrefWidth(220);
        inputs.put("other_generateContextTemplates", codeGenCombo);

        HBox codeGenRow = new HBox(8, codeGenLabel, codeGenCombo);
        codeGenRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Virtual Foreign Keys
        HBox vfkSection = buildTitledSectionLine("Virtual Foreign Keys");

        Button addVfkBtn = new Button("+");
        addVfkBtn.setPrefWidth(28);
        Button removeVfkBtn = new Button("—");
        removeVfkBtn.setPrefWidth(28);
        Button testVfkBtn = new Button("▶");
        testVfkBtn.setPrefWidth(28);

        HBox vfkToolbar = new HBox(4, addVfkBtn, removeVfkBtn, testVfkBtn);
        vfkToolbar.setAlignment(Pos.CENTER_LEFT);

        ObservableList<AppSettingsStore.VirtualForeignKeyRule> vfkList = FXCollections.observableArrayList();
        if (settings.getVirtualForeignKeys() != null) {
            for (AppSettingsStore.VirtualForeignKeyRule r : settings.getVirtualForeignKeys()) {
                vfkList.add(r.copy());
            }
        }
        if (vfkList.isEmpty()) {
            vfkList.add(new AppSettingsStore.VirtualForeignKeyRule("(.*)_(?i)id", "$1\\.(?i)id"));
        }
        inputs.put("other_virtualForeignKeys", vfkList);

        TableView<AppSettingsStore.VirtualForeignKeyRule> vfkTable = new TableView<>(vfkList);
        vfkTable.setPrefHeight(120);

        TableColumn<AppSettingsStore.VirtualForeignKeyRule, String> colPatternCol = new TableColumn<>("Column pattern");
        colPatternCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getColumnPattern()));
        colPatternCol.setPrefWidth(280);

        TableColumn<AppSettingsStore.VirtualForeignKeyRule, String> targetPatternCol = new TableColumn<>("Target column pattern");
        targetPatternCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTargetColumnPattern()));
        targetPatternCol.setPrefWidth(280);

        vfkTable.getColumns().addAll(colPatternCol, targetPatternCol);

        addVfkBtn.setOnAction(e -> {
            Dialog<AppSettingsStore.VirtualForeignKeyRule> dialog = new Dialog<>();
            dialog.setTitle("New Virtual Foreign Key Pattern");
            ButtonType okType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

            TextField colField = new TextField("(.*)_(?i)id");
            TextField targetField = new TextField("$1\\.(?i)id");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));
            grid.add(new Label("Column pattern:"), 0, 0);
            grid.add(colField, 1, 0);
            grid.add(new Label("Target pattern:"), 0, 1);
            grid.add(targetField, 1, 1);

            dialog.getDialogPane().setContent(grid);
            dialog.setResultConverter(b -> {
                if (b == okType && colField.getText() != null && !colField.getText().isBlank()) {
                    return new AppSettingsStore.VirtualForeignKeyRule(colField.getText().trim(), targetField.getText().trim());
                }
                return null;
            });
            dialog.showAndWait().ifPresent(vfkList::add);
        });

        removeVfkBtn.setOnAction(e -> {
            AppSettingsStore.VirtualForeignKeyRule sel = vfkTable.getSelectionModel().getSelectedItem();
            if (sel != null) vfkList.remove(sel);
        });

        testVfkBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Virtual Foreign Keys Pattern Tester");
            alert.setHeaderText("Pattern Validation");
            alert.setContentText("Configured rules: " + vfkList.size() + " regex pattern(s) active.");
            alert.showAndWait();
        });

        // Section: SQL Resolution
        HBox sqlResSection = buildTitledSectionLine("SQL Resolution");
        Label sqlResLabel = new Label("Default resolve mode for consoles:");
        sqlResLabel.setMinWidth(220);
        sqlResLabel.setStyle("-fx-text-fill: -text;");

        ComboBox<String> sqlResCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Playground",
                "Single data source",
                "Exact match"
        ));
        sqlResCombo.setValue(settings.getDefaultResolveModeForConsoles() != null ? settings.getDefaultResolveModeForConsoles() : "Playground");
        sqlResCombo.setPrefWidth(160);
        inputs.put("other_defaultResolveModeForConsoles", sqlResCombo);

        HBox sqlResRow = new HBox(8, sqlResLabel, sqlResCombo);
        sqlResRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Editor
        HBox editorSection = buildTitledSectionLine("Editor");
        Label stmtDelimLabel = new Label("Statement delimiter:");
        stmtDelimLabel.setMinWidth(160);
        stmtDelimLabel.setStyle("-fx-text-fill: -text;");

        TextField stmtDelimField = new TextField(settings.getStatementDelimiter() != null ? settings.getStatementDelimiter() : "");
        stmtDelimField.setPrefWidth(160);
        inputs.put("other_statementDelimiter", stmtDelimField);

        HBox stmtDelimRow = new HBox(8, stmtDelimLabel, stmtDelimField);
        stmtDelimRow.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(8,
                modObjSection, confirmCancelCheck,
                refactorSection, previewScriptCheck,
                ddlSection, suggestDdlCheck,
                codeGenSection, codeGenRow,
                vfkSection, vfkToolbar, vfkTable,
                sqlResSection, sqlResRow,
                editorSection, stmtDelimRow
        );
        panel.setPadding(new Insets(6, 12, 28, 12));
        return panel;
    }

    private static VBox buildCsvFormatsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label breadcrumb = new Label("Database \u203A CSV Formats");
        breadcrumb.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");

        ObservableList<AppSettingsStore.CsvFormatConfig> formatList = FXCollections.observableArrayList();
        for (AppSettingsStore.CsvFormatConfig cfg : settings.getCsvFormats()) {
            formatList.add(cfg.copy());
        }
        if (formatList.isEmpty()) {
            for (AppSettingsStore.CsvFormatConfig cfg : AppSettingsStore.Settings.defaultCsvFormats()) {
                formatList.add(cfg.copy());
            }
        }

        // Formats header & toolbar
        Label formatsLabel = new Label("Formats:");
        formatsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        Button addFormatBtn = new Button("+");
        addFormatBtn.setPrefWidth(26);
        Button removeFormatBtn = new Button("—");
        removeFormatBtn.setPrefWidth(26);
        Button duplicateFormatBtn = new Button("\uD83D\uDDD0");
        duplicateFormatBtn.setPrefWidth(26);
        duplicateFormatBtn.setTooltip(new Tooltip("Duplicate format"));
        Button upFormatBtn = new Button("↑");
        upFormatBtn.setPrefWidth(26);
        Button downFormatBtn = new Button("↓");
        downFormatBtn.setPrefWidth(26);

        HBox formatsToolbar = new HBox(3, addFormatBtn, removeFormatBtn, duplicateFormatBtn, upFormatBtn, downFormatBtn);
        formatsToolbar.setAlignment(Pos.CENTER_RIGHT);

        BorderPane formatsHeader = new BorderPane();
        formatsHeader.setLeft(formatsLabel);
        formatsHeader.setRight(formatsToolbar);

        ListView<AppSettingsStore.CsvFormatConfig> formatsListView = new ListView<>(formatList);
        formatsListView.setPrefHeight(105);
        formatsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AppSettingsStore.CsvFormatConfig item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });

        // Config controls
        Label valSepLabel = new Label("Value separator:");
        ComboBox<String> valSepCombo = new ComboBox<>();
        valSepCombo.setEditable(true);
        valSepCombo.getItems().addAll("Newline", "Space", "Tab", "Comma", "Semicolon", "Pipe");
        valSepCombo.setPrefWidth(150);

        Label rowSepLabel = new Label("Row separator:");
        ComboBox<String> rowSepCombo = new ComboBox<>();
        rowSepCombo.setEditable(true);
        rowSepCombo.getItems().addAll("Newline", "Space", "Tab", "Comma", "Semicolon", "Pipe");
        rowSepCombo.setPrefWidth(150);

        Label nullValLabel = new Label("Null value text:");
        ComboBox<String> nullValCombo = new ComboBox<>();
        nullValCombo.setEditable(true);
        nullValCombo.getItems().addAll("Undefined", "Empty string", "\\N");
        nullValCombo.setPrefWidth(150);

        Hyperlink addPrefixSuffixLink = new Hyperlink("Add row prefix/suffix");
        addPrefixSuffixLink.setStyle("-fx-font-size: 11px; -fx-padding: 1 0 1 0;");

        Label prefixLabel = new Label("Row prefix:");
        TextField prefixField = new TextField();
        prefixField.setPrefWidth(150);

        Label suffixLabel = new Label("Row suffix:");
        TextField suffixField = new TextField();
        suffixField.setPrefWidth(150);

        GridPane prefixSuffixGrid = new GridPane();
        prefixSuffixGrid.setHgap(8);
        prefixSuffixGrid.setVgap(4);
        prefixSuffixGrid.add(prefixLabel, 0, 0);
        prefixSuffixGrid.add(prefixField, 1, 0);
        prefixSuffixGrid.add(suffixLabel, 0, 1);
        prefixSuffixGrid.add(suffixField, 1, 1);
        prefixSuffixGrid.setVisible(false);
        prefixSuffixGrid.setManaged(false);

        addPrefixSuffixLink.setOnAction(e -> {
            boolean show = !prefixSuffixGrid.isVisible();
            prefixSuffixGrid.setVisible(show);
            prefixSuffixGrid.setManaged(show);
        });

        // Quotation
        Label quotationLabel = new Label("Quotation:");
        quotationLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        Button addQuoteBtn = new Button("+");
        addQuoteBtn.setPrefWidth(26);
        Button removeQuoteBtn = new Button("—");
        removeQuoteBtn.setPrefWidth(26);
        Button upQuoteBtn = new Button("↑");
        upQuoteBtn.setPrefWidth(26);
        Button downQuoteBtn = new Button("↓");
        downQuoteBtn.setPrefWidth(26);

        HBox quoteToolbar = new HBox(3, addQuoteBtn, removeQuoteBtn, upQuoteBtn, downQuoteBtn);
        quoteToolbar.setAlignment(Pos.CENTER_RIGHT);

        BorderPane quoteHeader = new BorderPane();
        quoteHeader.setLeft(quotationLabel);
        quoteHeader.setRight(quoteToolbar);

        ObservableList<AppSettingsStore.QuotationRule> quoteRulesList = FXCollections.observableArrayList();
        ListView<AppSettingsStore.QuotationRule> quoteListView = new ListView<>(quoteRulesList);
        quoteListView.setPrefHeight(60);

        Label quoteValuesLabel = new Label("Quote values:");
        ComboBox<String> quoteValuesCombo = new ComboBox<>();
        quoteValuesCombo.getItems().addAll("Never", "When needed", "Always");
        quoteValuesCombo.setPrefWidth(150);

        CheckBox trimWhitespaces = new CheckBox("Trim whitespaces");
        CheckBox firstRowHeader = new CheckBox("First row is header");
        CheckBox firstColHeader = new CheckBox("First column is header");

        GridPane fieldsGrid = new GridPane();
        fieldsGrid.setHgap(8);
        fieldsGrid.setVgap(6);
        fieldsGrid.add(valSepLabel, 0, 0);
        fieldsGrid.add(valSepCombo, 1, 0);
        fieldsGrid.add(rowSepLabel, 0, 1);
        fieldsGrid.add(rowSepCombo, 1, 1);
        fieldsGrid.add(nullValLabel, 0, 2);
        fieldsGrid.add(nullValCombo, 1, 2);

        GridPane quoteValuesGrid = new GridPane();
        quoteValuesGrid.setHgap(8);
        quoteValuesGrid.add(quoteValuesLabel, 0, 0);
        quoteValuesGrid.add(quoteValuesCombo, 1, 0);

        VBox leftColumn = new VBox(6,
                formatsHeader,
                formatsListView,
                fieldsGrid,
                addPrefixSuffixLink,
                prefixSuffixGrid,
                quoteHeader,
                quoteListView,
                quoteValuesGrid,
                trimWhitespaces,
                firstRowHeader,
                firstColHeader
        );
        leftColumn.setPrefWidth(315);
        leftColumn.setMinWidth(300);

        // Preview components
        TextArea rawTextArea = new TextArea();
        rawTextArea.setEditable(false);
        rawTextArea.setStyle("-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-control-inner-background: #1e1f22; -fx-text-fill: #bcbec4;");
        rawTextArea.setPrefHeight(170);

        TextArea lineNumArea = new TextArea();
        lineNumArea.setEditable(false);
        lineNumArea.setPrefWidth(34);
        lineNumArea.setMinWidth(34);
        lineNumArea.setMaxWidth(34);
        lineNumArea.setStyle("-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-control-inner-background: #1e1f22; -fx-text-fill: #565861; -fx-text-alignment: right;");

        HBox textPreviewBox = new HBox(0, lineNumArea, rawTextArea);
        HBox.setHgrow(rawTextArea, Priority.ALWAYS);
        textPreviewBox.setStyle("-fx-border-color: -border; -fx-border-width: 1px; -fx-border-radius: 4px;");

        TableView<List<String>> previewTable = new TableView<>();
        previewTable.setPrefHeight(190);
        previewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox rightColumn = new VBox(8, textPreviewBox, previewTable);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        VBox.setVgrow(previewTable, Priority.ALWAYS);

        HBox mainContent = new HBox(14, leftColumn, rightColumn);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        final boolean[] updating = {false};

        Runnable updatePreview = () -> {
            AppSettingsStore.CsvFormatConfig selected = formatsListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            if (!updating[0]) {
                selected.setValueSeparator(valSepCombo.getValue() != null ? valSepCombo.getValue() : "Comma");
                selected.setRowSeparator(rowSepCombo.getValue() != null ? rowSepCombo.getValue() : "Newline");
                selected.setNullValueText(nullValCombo.getValue() != null ? nullValCombo.getValue() : "Empty string");
                selected.setRowPrefix(prefixField.getText() != null ? prefixField.getText() : "");
                selected.setRowSuffix(suffixField.getText() != null ? suffixField.getText() : "");
                selected.setQuoteValues(quoteValuesCombo.getValue() != null ? quoteValuesCombo.getValue() : "When needed");
                selected.setTrimWhitespaces(trimWhitespaces.isSelected());
                selected.setFirstRowIsHeader(firstRowHeader.isSelected());
                selected.setFirstColumnIsHeader(firstColHeader.isSelected());
                selected.setQuotationRules(new ArrayList<>(quoteRulesList));
            }

            String raw = CsvFormatEngine.formatData(
                    CsvFormatEngine.SAMPLE_HEADERS,
                    CsvFormatEngine.SAMPLE_ROWS,
                    selected,
                    true
            );
            rawTextArea.setText(raw);

            int lineCount = raw.isEmpty() ? 0 : raw.split("\n", -1).length;
            StringBuilder linesSb = new StringBuilder();
            for (int i = 1; i <= Math.max(1, lineCount); i++) {
                linesSb.append(i).append("\n");
            }
            lineNumArea.setText(linesSb.toString());

            CsvFormatEngine.ParsedTable parsed = CsvFormatEngine.parseData(raw, selected);
            previewTable.getColumns().clear();
            previewTable.getItems().clear();

            TableColumn<List<String>, String> idxCol = new TableColumn<>("#");
            idxCol.setPrefWidth(32);
            idxCol.setSortable(false);
            idxCol.setCellValueFactory(data -> {
                int rowIdx = previewTable.getItems().indexOf(data.getValue()) + 1;
                return new SimpleStringProperty(String.valueOf(rowIdx));
            });
            previewTable.getColumns().add(idxCol);

            for (int colIdx = 0; colIdx < parsed.headers().size(); colIdx++) {
                final int ci = colIdx;
                String headerName = parsed.headers().get(colIdx);
                TableColumn<List<String>, String> col = new TableColumn<>(headerName);
                col.setCellValueFactory(data -> {
                    List<String> row = data.getValue();
                    String val = (ci < row.size()) ? row.get(ci) : "";
                    return new SimpleStringProperty(val != null ? val : "");
                });
                previewTable.getColumns().add(col);
            }

            for (List<String> row : parsed.rows()) {
                previewTable.getItems().add(row);
            }
        };

        Runnable loadSelectedFormat = () -> {
            AppSettingsStore.CsvFormatConfig selected = formatsListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            updating[0] = true;
            try {
                valSepCombo.setValue(selected.getValueSeparator());
                rowSepCombo.setValue(selected.getRowSeparator());
                nullValCombo.setValue(selected.getNullValueText());
                prefixField.setText(selected.getRowPrefix());
                suffixField.setText(selected.getRowSuffix());
                boolean hasPrefixSuffix = (selected.getRowPrefix() != null && !selected.getRowPrefix().isEmpty())
                        || (selected.getRowSuffix() != null && !selected.getRowSuffix().isEmpty());
                prefixSuffixGrid.setVisible(hasPrefixSuffix);
                prefixSuffixGrid.setManaged(hasPrefixSuffix);

                quoteRulesList.clear();
                for (AppSettingsStore.QuotationRule r : selected.getQuotationRules()) {
                    quoteRulesList.add(r.copy());
                }
                quoteValuesCombo.setValue(selected.getQuoteValues());
                trimWhitespaces.setSelected(selected.isTrimWhitespaces());
                firstRowHeader.setSelected(selected.isFirstRowIsHeader());
                firstColHeader.setSelected(selected.isFirstColumnIsHeader());
            } finally {
                updating[0] = false;
            }
            updatePreview.run();
        };

        formatsListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) loadSelectedFormat.run();
        });

        // Wire change listeners
        valSepCombo.valueProperty().addListener((o, ov, nv) -> { if (!updating[0]) updatePreview.run(); });
        rowSepCombo.valueProperty().addListener((o, ov, nv) -> { if (!updating[0]) updatePreview.run(); });
        nullValCombo.valueProperty().addListener((o, ov, nv) -> { if (!updating[0]) updatePreview.run(); });
        prefixField.textProperty().addListener((o, ov, nv) -> { if (!updating[0]) updatePreview.run(); });
        suffixField.textProperty().addListener((o, ov, nv) -> { if (!updating[0]) updatePreview.run(); });
        quoteValuesCombo.valueProperty().addListener((o, ov, nv) -> { if (!updating[0]) updatePreview.run(); });
        trimWhitespaces.selectedProperty().addListener((o, ov, nv) -> { if (!updating[0]) updatePreview.run(); });
        firstRowHeader.selectedProperty().addListener((o, ov, nv) -> { if (!updating[0]) updatePreview.run(); });
        firstColHeader.selectedProperty().addListener((o, ov, nv) -> { if (!updating[0]) updatePreview.run(); });

        // Formats toolbar buttons
        addFormatBtn.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog("Custom Format");
            dlg.setTitle("New CSV Format");
            dlg.setHeaderText("Enter name for new format:");
            dlg.showAndWait().ifPresent(name -> {
                if (!name.isBlank()) {
                    AppSettingsStore.CsvFormatConfig newFmt = new AppSettingsStore.CsvFormatConfig(
                            name.trim(), "Comma", "Newline", "Empty string",
                            AppSettingsStore.CsvFormatConfig.defaultQuotationRules(), "When needed", false, false, false);
                    formatList.add(newFmt);
                    formatsListView.getSelectionModel().select(newFmt);
                }
            });
        });

        removeFormatBtn.setOnAction(e -> {
            int sel = formatsListView.getSelectionModel().getSelectedIndex();
            if (sel >= 0 && formatList.size() > 1) {
                formatList.remove(sel);
                formatsListView.getSelectionModel().select(Math.max(0, sel - 1));
            }
        });

        duplicateFormatBtn.setOnAction(e -> {
            AppSettingsStore.CsvFormatConfig cur = formatsListView.getSelectionModel().getSelectedItem();
            if (cur != null) {
                AppSettingsStore.CsvFormatConfig copy = cur.copy();
                copy.setName(cur.getName() + " (copy)");
                formatList.add(copy);
                formatsListView.getSelectionModel().select(copy);
            }
        });

        upFormatBtn.setOnAction(e -> {
            int sel = formatsListView.getSelectionModel().getSelectedIndex();
            if (sel > 0) {
                AppSettingsStore.CsvFormatConfig item = formatList.remove(sel);
                formatList.add(sel - 1, item);
                formatsListView.getSelectionModel().select(sel - 1);
            }
        });

        downFormatBtn.setOnAction(e -> {
            int sel = formatsListView.getSelectionModel().getSelectedIndex();
            if (sel >= 0 && sel < formatList.size() - 1) {
                AppSettingsStore.CsvFormatConfig item = formatList.remove(sel);
                formatList.add(sel + 1, item);
                formatsListView.getSelectionModel().select(sel + 1);
            }
        });

        // Quotation toolbar buttons
        addQuoteBtn.setOnAction(e -> {
            quoteRulesList.add(new AppSettingsStore.QuotationRule("`", "`", "duplicate"));
            updatePreview.run();
        });

        removeQuoteBtn.setOnAction(e -> {
            int sel = quoteListView.getSelectionModel().getSelectedIndex();
            if (sel >= 0 && quoteRulesList.size() > 1) {
                quoteRulesList.remove(sel);
                updatePreview.run();
            }
        });

        upQuoteBtn.setOnAction(e -> {
            int sel = quoteListView.getSelectionModel().getSelectedIndex();
            if (sel > 0) {
                AppSettingsStore.QuotationRule item = quoteRulesList.remove(sel);
                quoteRulesList.add(sel - 1, item);
                quoteListView.getSelectionModel().select(sel - 1);
                updatePreview.run();
            }
        });

        downQuoteBtn.setOnAction(e -> {
            int sel = quoteListView.getSelectionModel().getSelectedIndex();
            if (sel >= 0 && sel < quoteRulesList.size() - 1) {
                AppSettingsStore.QuotationRule item = quoteRulesList.remove(sel);
                quoteRulesList.add(sel + 1, item);
                quoteListView.getSelectionModel().select(sel + 1);
                updatePreview.run();
            }
        });

        // Initial selection
        String targetFmtName = settings.getDefaultCsvFormat();
        AppSettingsStore.CsvFormatConfig initSelected = null;
        for (AppSettingsStore.CsvFormatConfig cfg : formatList) {
            if (cfg.getName().equalsIgnoreCase(targetFmtName)) {
                initSelected = cfg;
                break;
            }
        }
        if (initSelected == null && !formatList.isEmpty()) {
            initSelected = formatList.get(0);
        }
        if (initSelected != null) {
            formatsListView.getSelectionModel().select(initSelected);
            loadSelectedFormat.run();
        }

        inputs.put("csvFormatsList", formatList);
        inputs.put("formatsListView", formatsListView);

        VBox panel = new VBox(10, breadcrumb, mainContent);
        panel.setPadding(new Insets(4, 8, 8, 8));
        return panel;
    }

    private static VBox buildKeymapPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label title = new Label("Keymap");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label presetLabel = new Label("Keymap preset:");
        ComboBox<String> keymapCombo = new ComboBox<>();
        keymapCombo.getItems().addAll("DataGrip Default", "Windows 10+", "macOS System", "Eclipse", "VS Code");
        keymapCombo.getSelectionModel().select(settings.getKeymapPreset());
        keymapCombo.setPrefWidth(200);

        TextField searchFilter = new TextField();
        searchFilter.setPromptText("Search actions by shortcut or name…");
        searchFilter.setPrefWidth(300);

        HBox topBar = new HBox(12, presetLabel, keymapCombo, searchFilter);
        topBar.setAlignment(Pos.CENTER_LEFT);

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<String[], String> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));

        TableColumn<String[], String> colShortcut = new TableColumn<>("Shortcut");
        colShortcut.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));

        TableColumn<String[], String> colCategory = new TableColumn<>("Category");
        colCategory.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));

        table.getColumns().addAll(colAction, colShortcut, colCategory);

        ObservableList<String[]> rows = FXCollections.observableArrayList(
                new String[]{"Execute Statement", "Ctrl+Enter", "SQL Console"},
                new String[]{"Find Action…", "Ctrl+Shift+A", "Help"},
                new String[]{"Settings…", "Ctrl+Alt+S", "File"},
                new String[]{"Compare Data", "Ctrl+D", "Run"},
                new String[]{"Compare Schema Structure", "Ctrl+Shift+D", "Run"},
                new String[]{"Full-Text Search…", "Ctrl+Alt+Shift+F", "Run"},
                new String[]{"VCS Operations Popup…", "Alt+`", "VCS"},
                new String[]{"New Scratch File", "Ctrl+Alt+Shift+Insert", "File"},
                new String[]{"Select Next Tab", "Alt+Right", "Window / Editor Tabs"},
                new String[]{"Select Previous Tab", "Alt+Left", "Window / Editor Tabs"},
                new String[]{"Next Project Window", "Ctrl+Alt+]", "Window"},
                new String[]{"Previous Project Window", "Ctrl+Alt+[", "Window"}
        );
        table.setItems(rows);
        table.setPrefHeight(260);

        inputs.put("keymapCombo", keymapCombo);

        VBox panel = new VBox(12, title, topBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildPluginsPanel() {
        Label title = new Label("Plugins");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label badge = new Label("Installed (6)");
        badge.setStyle("-fx-background-color: #2e436e; -fx-text-fill: #dfe1e5; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 11px;");

        HBox titleRow = new HBox(10, title, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox list = new VBox(8);
        list.getChildren().addAll(
                createPluginRow("PostgreSQL Driver", "v42.7.2", "Bundled JDBC driver for PostgreSQL databases", true),
                createPluginRow("MySQL Connector/J", "v8.4.0", "Bundled JDBC driver for MySQL & MariaDB databases", true),
                createPluginRow("StratosDB Driver", "v1.0.7", "Bundled driver for StratosDB distributed instances", true),
                createPluginRow("SQLite Driver", "v3.45.1", "Bundled JDBC driver for SQLite embedded databases", true),
                createPluginRow("SQL Formatter", "v2.0.0", "Dialect-aware SQL beautifier and statement validator", true),
                createPluginRow("Git VCS Integration", "v2.0.0", "Git repository log, commit history, and diff tools", true)
        );

        VBox panel = new VBox(14, titleRow, list);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static HBox createPluginRow(String name, String ver, String desc, boolean enabled) {
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        Label verLbl = new Label(ver);
        verLbl.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

        VBox info = new VBox(2, new HBox(8, nameLbl, verLbl), descLbl);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox toggle = new CheckBox();
        toggle.setSelected(enabled);
        toggle.setDisable(true); // Built-in extensions

        HBox row = new HBox(10, info, spacer, toggle);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: -bg-mid; -fx-padding: 8 12; -fx-background-radius: 6;");
        return row;
    }

    private static VBox buildVersionControlPanel(CategoryDef cat, java.util.function.Consumer<String> navigateTo) {
        Label title = new Label("Version Control");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label gitPathLbl = new Label("Path to Git executable:");
        TextField gitPath = new TextField("git");
        gitPath.setPrefWidth(300);

        Button testGit = new Button("Test");
        testGit.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Git executable found in system PATH.", ButtonType.OK);
            alert.setHeaderText("Git Integration");
            alert.showAndWait();
        });

        HBox gitRow = new HBox(8, gitPathLbl, gitPath, testGit);
        gitRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox checkTodo = new CheckBox("Check TODO before commit");
        checkTodo.setSelected(true);

        CheckBox unifiedDiff = new CheckBox("Show diff in unified viewer");
        unifiedDiff.setSelected(false);

        VBox panel = new VBox(14, title, gitRow, checkTodo, unifiedDiff);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildToolsPanel(CategoryDef cat, java.util.function.Consumer<String> navigateTo) {
        Label title = new Label("Tools");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label shellLbl = new Label("Shell path:");
        TextField shellPath = new TextField(System.getProperty("os.name", "").toLowerCase().contains("win") ? "cmd.exe" : "/bin/bash");
        shellPath.setPrefWidth(280);

        Label sshLbl = new Label("SSH executable:");
        TextField sshPath = new TextField("ssh");
        sshPath.setPrefWidth(280);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(shellLbl, 0, 0);
        grid.add(shellPath, 1, 0);
        grid.add(sshLbl, 0, 1);
        grid.add(sshPath, 1, 1);

        VBox panel = new VBox(14, title, grid);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildCommitPanel(CategoryDef cat) {
        Label title = new Label("Commit");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox checkTodo = new CheckBox("Check TODO (show warning if commit has pending tasks)");
        checkTodo.setSelected(true);

        CheckBox reformatCode = new CheckBox("Reformat SQL and source code before commit");
        reformatCode.setSelected(false);

        CheckBox optimizeImports = new CheckBox("Optimize and clean schema references");
        optimizeImports.setSelected(true);

        CheckBox clearMessage = new CheckBox("Clear commit message history after successful commit");
        clearMessage.setSelected(false);

        VBox panel = new VBox(14, title, checkTodo, reformatCode, optimizeImports, clearMessage);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildMarkdownPanel(CategoryDef cat) {
        Label title = new Label("Markdown");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label layoutLabel = new Label("Default editor and preview layout:");
        ComboBox<String> layoutCombo = new ComboBox<>();
        layoutCombo.getItems().addAll("Split: Editor and Preview", "Preview Only", "Editor Only");
        layoutCombo.getSelectionModel().select(0);
        layoutCombo.setPrefWidth(220);

        CheckBox autoScroll = new CheckBox("Auto-scroll preview synchronously with editor");
        autoScroll.setSelected(true);

        CheckBox renderTables = new CheckBox("Enable GitHub Flavored Markdown (tables, task lists)");
        renderTables.setSelected(true);

        VBox panel = new VBox(14, title, layoutLabel, layoutCombo, autoScroll, renderTables);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildMermaidPanel(CategoryDef cat) {
        Label title = new Label("Mermaid");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label themeLabel = new Label("Mermaid diagram theme:");
        ComboBox<String> themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("dark", "default", "forest", "neutral");
        themeCombo.getSelectionModel().select("dark");
        themeCombo.setPrefWidth(180);

        CheckBox autoRender = new CheckBox("Automatically render diagrams on file save");
        autoRender.setSelected(true);

        VBox panel = new VBox(14, title, themeLabel, themeCombo, autoRender);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildDiagramsPanel(CategoryDef cat) {
        Label title = new Label("Diagrams");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label layoutLabel = new Label("Default ER diagram layout:");
        ComboBox<String> layoutCombo = new ComboBox<>();
        layoutCombo.getItems().addAll("Hierarchical", "Organic", "Orthogonal", "Circular");
        layoutCombo.getSelectionModel().select("Hierarchical");
        layoutCombo.setPrefWidth(200);

        CheckBox showDataTypes = new CheckBox("Show column data types in table nodes");
        showDataTypes.setSelected(true);

        CheckBox showKeys = new CheckBox("Highlight primary and foreign key columns");
        showKeys.setSelected(true);

        CheckBox showComments = new CheckBox("Show table and column comments");
        showComments.setSelected(false);

        VBox panel = new VBox(14, title, layoutLabel, layoutCombo, showDataTypes, showKeys, showComments);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildSchemasDtdsPanel(CategoryDef cat) {
        // 1. External schemas and DTDs
        Label externalLabel = new Label("External schemas and DTDs:");
        externalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        Button addExtBtn = new Button("+");
        addExtBtn.setTooltip(new Tooltip("Add External Schema or DTD"));
        addExtBtn.setStyle("-fx-min-width: 26px; -fx-min-height: 22px;");

        Button removeExtBtn = new Button("—");
        removeExtBtn.setTooltip(new Tooltip("Remove"));
        removeExtBtn.setStyle("-fx-min-width: 26px; -fx-min-height: 22px;");

        Button editExtBtn = new Button("✏");
        editExtBtn.setTooltip(new Tooltip("Edit"));
        editExtBtn.setStyle("-fx-min-width: 26px; -fx-min-height: 22px;");

        HBox extToolbar = new HBox(4, addExtBtn, removeExtBtn, editExtBtn);
        extToolbar.setAlignment(Pos.CENTER_LEFT);

        Label noResources = new Label("No external resources");
        noResources.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");

        StackPane emptyBox = new StackPane(noResources);
        emptyBox.setPrefHeight(130);
        emptyBox.setMaxHeight(150);
        emptyBox.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #3e4248; -fx-border-radius: 4; -fx-background-radius: 4;");

        VBox extBox = new VBox(6, externalLabel, extToolbar, emptyBox);

        // 2. Ignored schemas and DTDs
        Label ignoredLabel = new Label("Ignored schemas and DTDs:");
        ignoredLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 8 0 0 0;");

        Button addIgnoredBtn = new Button("+");
        addIgnoredBtn.setTooltip(new Tooltip("Add Ignored Schema or DTD"));
        addIgnoredBtn.setStyle("-fx-min-width: 26px; -fx-min-height: 22px;");

        Button removeIgnoredBtn = new Button("—");
        removeIgnoredBtn.setTooltip(new Tooltip("Remove"));
        removeIgnoredBtn.setStyle("-fx-min-width: 26px; -fx-min-height: 22px;");

        Button editIgnoredBtn = new Button("✏");
        editIgnoredBtn.setTooltip(new Tooltip("Edit"));
        editIgnoredBtn.setStyle("-fx-min-width: 26px; -fx-min-height: 22px;");

        HBox ignoredToolbar = new HBox(4, addIgnoredBtn, removeIgnoredBtn, editIgnoredBtn);
        ignoredToolbar.setAlignment(Pos.CENTER_LEFT);

        ListView<String> ignoredList = new ListView<>();
        ignoredList.getItems().addAll(
                "http://exslt.org/common",
                "http://exslt.org/dates-and-times",
                "http://exslt.org/dynamic",
                "http://exslt.org/math",
                "http://exslt.org/sets",
                "http://exslt.org/strings",
                "http://relaxng.org/ns/compatibility/annotations/1.0",
                "urn:idea:xslt-plugin#extensions"
        );
        ignoredList.setPrefHeight(180);
        ignoredList.setStyle("-fx-background-color: #2b2d30; -fx-control-inner-background: #2b2d30; -fx-font-family: monospace; -fx-font-size: 12px;");
        VBox.setVgrow(ignoredList, Priority.ALWAYS);

        VBox ignoredBox = new VBox(6, ignoredLabel, ignoredToolbar, ignoredList);
        VBox.setVgrow(ignoredBox, Priority.ALWAYS);

        VBox panel = new VBox(12, extBox, ignoredBox);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildDebuggerPanel(CategoryDef cat) {
        CheckBox showDebugWindow = new CheckBox("Show debug window on breakpoint");
        showDebugWindow.setSelected(true);

        CheckBox focusApp = new CheckBox("Focus application on breakpoint");
        focusApp.setSelected(true);
        focusApp.setPadding(new Insets(0, 0, 0, 20));
        focusApp.disableProperty().bind(showDebugWindow.selectedProperty().not());

        CheckBox hideOnTermination = new CheckBox("Hide debug window on process termination");
        hideOnTermination.setSelected(false);

        CheckBox scrollToCenter = new CheckBox("Scroll execution point to center");
        scrollToCenter.setSelected(false);

        CheckBox clickLineRunToCursor = new CheckBox("Click line number to perform run to cursor");
        clickLineRunToCursor.setSelected(true);

        VBox checksBox = new VBox(10, showDebugWindow, focusApp, hideOnTermination, scrollToCenter, clickLineRunToCursor);

        // Breakpoint removal section
        Label removeBreakpointLabel = new Label("Remove breakpoint:");
        removeBreakpointLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 8 0 2 0;");

        ToggleGroup removeGroup = new ToggleGroup();
        RadioButton clickLeftMouse = new RadioButton("Click with left mouse button");
        clickLeftMouse.setToggleGroup(removeGroup);
        clickLeftMouse.setSelected(true);

        RadioButton dragOrMiddleClick = new RadioButton("Drag to the editor or click with middle mouse button");
        dragOrMiddleClick.setToggleGroup(removeGroup);

        CheckBox confirmRemoval = new CheckBox("Confirm removal of conditional or logging breakpoints");
        confirmRemoval.setSelected(false);
        confirmRemoval.setPadding(new Insets(4, 0, 0, 0));

        VBox removeBox = new VBox(8, removeBreakpointLabel, clickLeftMouse, dragOrMiddleClick, confirmRemoval);

        VBox panel = new VBox(14, checksBox, removeBox);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildDiffMergePanel(CategoryDef cat) {
        // 1. Diff section
        Label diffHeader = new Label("Diff");
        diffHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label contextLinesLabel = new Label("Context lines:");
        contextLinesLabel.setPrefWidth(90);
        contextLinesLabel.setStyle("-fx-text-fill: -text;");

        Slider contextSlider = new Slider(0, 4, 2); // 0=1, 1=2, 2=4, 3=8, 4=Disable
        contextSlider.setMajorTickUnit(1);
        contextSlider.setMinorTickCount(0);
        contextSlider.setSnapToTicks(true);
        contextSlider.setShowTickMarks(true);
        contextSlider.setShowTickLabels(false);
        contextSlider.setPrefWidth(160);

        HBox sliderRow = new HBox(10, contextLinesLabel, contextSlider);
        sliderRow.setAlignment(Pos.CENTER_LEFT);

        Label tickLabel1 = new Label("1");
        Label tickLabel2 = new Label("2");
        Label tickLabel4 = new Label("4");
        Label tickLabel8 = new Label("8");
        Label tickLabelDisable = new Label("Disable");
        HBox tickLabels = new HBox(22, tickLabel1, tickLabel2, tickLabel4, tickLabel8, tickLabelDisable);
        tickLabels.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-dim;");
        tickLabels.setPadding(new Insets(-4, 0, 4, 102));

        CheckBox nextFileCheck = new CheckBox("Go to the next file after reaching last change");
        nextFileCheck.setSelected(true);

        Label histLabel = new Label("Include diffs in navigation history:");
        histLabel.setStyle("-fx-text-fill: -text;");
        ComboBox<String> histCombo = new ComboBox<>();
        histCombo.getItems().addAll("Until the diff is closed", "Always", "Never");
        histCombo.getSelectionModel().select(0);
        histCombo.setPrefWidth(180);
        HBox histRow = new HBox(10, histLabel, histCombo);
        histRow.setAlignment(Pos.CENTER_LEFT);

        VBox diffBox = new VBox(8, diffHeader, sliderRow, tickLabels, nextFileCheck, histRow);

        // 2. Merge section
        Label mergeHeader = new Label("Merge");
        mergeHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text; -fx-padding: 8 0 0 0;");

        CheckBox autoNonConflicting = new CheckBox("Automatically apply non-conflicting changes");
        autoNonConflicting.setSelected(false);

        CheckBox autoResolveImports = new CheckBox("Automatically resolve conflicts in import statements");
        autoResolveImports.setSelected(false);

        CheckBox highlightModifiedLines = new CheckBox("Highlight modified lines in gutter");
        highlightModifiedLines.setSelected(true);

        VBox mergeBox = new VBox(10, mergeHeader, autoNonConflicting, autoResolveImports, highlightModifiedLines);

        VBox panel = new VBox(14, diffBox, mergeBox);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildMcpServerPanel(CategoryDef cat) {
        CheckBox enableMcp = new CheckBox("Enable MCP Server");
        enableMcp.setSelected(false);
        enableMcp.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        Label desc1 = new Label("The MCP Server allows external AI clients to use functionality from the IDE, integrating the power of your IDE into your AI tools. ");
        desc1.setWrapText(true);
        desc1.setMaxWidth(620);
        desc1.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

        Hyperlink allMcpToolsLink = new Hyperlink("All MCP Tools ↗");
        allMcpToolsLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-padding: 0; -fx-border-width: 0;");

        HBox descRow = new HBox(4, desc1, allMcpToolsLink);
        descRow.setAlignment(Pos.CENTER_LEFT);

        Label desc2 = new Label("When enabled, these detected clients can be auto-configured to use IDE features in one click:\n• Junie\n• VSCode\n• Codex\n• GitHub Copilot CLI");
        desc2.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px; -fx-line-spacing: 2;");

        VBox infoBox = new VBox(8, enableMcp, descRow, desc2);

        // Terminal Sessions section
        Label terminalHeader = new Label("Terminal Sessions");
        terminalHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 8 0 0 0;");

        Separator sep = new Separator();

        CheckBox terminalSuggestions = new CheckBox("Show setup suggestions for Codex and Claude terminal sessions");
        terminalSuggestions.setSelected(true);

        Label terminalHint = new Label("Shows the terminal banner when Codex or Claude starts without a matching DataGrip MCP setup.");
        terminalHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");

        VBox terminalBox = new VBox(6, terminalHeader, sep, terminalSuggestions, terminalHint);

        VBox panel = new VBox(14, infoBox, terminalBox);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildSshPanel(CategoryDef cat) {
        Label title = new Label("SSH Configurations");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label sshPathLbl = new Label("Path to SSH executable:");
        TextField sshPath = new TextField("ssh");
        sshPath.setPrefWidth(280);

        Label timeoutLbl = new Label("Connection timeout (seconds):");
        Spinner<Integer> timeoutSpinner = new Spinner<>(5, 300, 30, 5);
        timeoutSpinner.setEditable(true);
        timeoutSpinner.setPrefWidth(90);

        CheckBox keepAlive = new CheckBox("Send Keep-Alive packets every 60 seconds");
        keepAlive.setSelected(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(sshPathLbl, 0, 0);
        grid.add(sshPath, 1, 0);
        grid.add(timeoutLbl, 0, 1);
        grid.add(timeoutSpinner, 1, 1);

        VBox panel = new VBox(14, title, grid, keepAlive);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildColorSchemeLandingPanel(CategoryDef cat, java.util.function.Consumer<String> navigateTo) {
        // Scheme selector row
        Label schemeLabel = new Label("Scheme:");
        schemeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll("Islands Dark Theme default", "Darcula", "High Contrast", "Light");
        schemeCombo.getSelectionModel().select(0);
        schemeCombo.setPrefWidth(240);

        Button gearBtn = new Button("⚙");
        gearBtn.setTooltip(new Tooltip("Scheme actions (Duplicate, Export, Restore)"));

        Hyperlink themeLink = new Hyperlink("Change IDE Theme...");
        themeLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px;");
        themeLink.setOnAction(e -> navigateTo.accept("Appearance & Behavior / Appearance"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button helpBtn = new Button("?");
        helpBtn.setTooltip(new Tooltip("Help on Color Schemes"));
        helpBtn.setStyle("-fx-font-weight: bold; -fx-min-width: 26px; -fx-min-height: 26px; -fx-background-radius: 13;");

        HBox topBar = new HBox(10, schemeLabel, schemeCombo, gearBtn, themeLink, spacer, helpBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Description
        String descText = cat != null && cat.getDescription() != null ? cat.getDescription()
                : "Configure colors and the font for source code and console output:";
        Label desc = new Label(descText);
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px; -fx-padding: 6 0 2 0;");

        // 2-column Grid of 26 subcategories
        GridPane linkGrid = new GridPane();
        linkGrid.setHgap(36);
        linkGrid.setVgap(6);
        linkGrid.setPadding(new Insets(6, 0, 10, 0));

        List<CategoryDef> children = cat != null ? cat.getChildren() : Collections.emptyList();
        int half = (children.size() + 1) / 2;
        for (int i = 0; i < children.size(); i++) {
            CategoryDef child = children.get(i);
            Hyperlink link = new Hyperlink(child.getName());
            link.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-width: 0;");
            link.setOnAction(e -> navigateTo.accept(child.getPath()));
            linkGrid.add(link, i / half, i % half);
        }

        VBox panel = new VBox(12, topBar, desc, linkGrid);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildCodeStylePanel(CategoryDef cat, java.util.function.Consumer<String> navigateTo) {
        // Scheme selector row
        Label schemeLabel = new Label("Scheme:");
        schemeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll("Default IDE", "Project");
        schemeCombo.getSelectionModel().select(0);
        schemeCombo.setPrefWidth(180);

        Button gearBtn = new Button("⚙");
        gearBtn.setTooltip(new Tooltip("Scheme actions (Duplicate, Import/Export)"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button helpBtn = new Button("?");
        helpBtn.setTooltip(new Tooltip("Help on Code Style"));
        helpBtn.setStyle("-fx-font-weight: bold; -fx-min-width: 26px; -fx-min-height: 26px; -fx-background-radius: 13;");

        HBox topBar = new HBox(10, schemeLabel, schemeCombo, gearBtn, spacer, helpBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Warning / Indents Detection banner
        Label warnIcon = new Label("⚠");
        warnIcon.setStyle("-fx-text-fill: #E5A84B; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label bannerText = new Label("Settings may be overridden by Indents Detection");
        bannerText.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");

        Hyperlink disableLink = new Hyperlink("Disable");
        disableLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-border-width: 0; -fx-padding: 0;");

        HBox banner = new HBox(8, warnIcon, bannerText, disableLink);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setStyle("-fx-background-color: #2b2d30; -fx-padding: 8 12; -fx-background-radius: 4; -fx-border-color: #3e4248; -fx-border-radius: 4;");

        // Segmented Tabs: [ General ] [ Formatter ]
        Button tabGeneral = new Button("General");
        tabGeneral.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4 0 0 4;");

        Button tabFormatter = new Button("Formatter");
        tabFormatter.setStyle("-fx-background-color: #393b40; -fx-text-fill: -text-dim; -fx-background-radius: 0 4 4 0;");

        HBox tabHeader = new HBox(0, tabGeneral, tabFormatter);
        tabHeader.setPadding(new Insets(4, 0, 4, 0));

        // General settings content
        // 1. Line separator
        Label lineSepLabel = new Label("Line separator:");
        lineSepLabel.setPrefWidth(120);
        lineSepLabel.setStyle("-fx-text-fill: -text;");
        ComboBox<String> lineSepCombo = new ComboBox<>();
        lineSepCombo.getItems().addAll("System-Dependent", "Unix and macOS (\n)", "Windows (\r\n)", "Classic macOS (\r)");
        lineSepCombo.getSelectionModel().select(0);
        lineSepCombo.setPrefWidth(180);
        Label lineSepHint = new Label("Applied to new files");
        lineSepHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        HBox lineSepRow = new HBox(10, lineSepLabel, lineSepCombo, lineSepHint);
        lineSepRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Hard wrap at
        Label hardWrapLabel = new Label("Hard wrap at:");
        hardWrapLabel.setPrefWidth(120);
        hardWrapLabel.setStyle("-fx-text-fill: -text;");
        TextField hardWrapField = new TextField("120");
        hardWrapField.setPrefWidth(60);
        Label columnsLabel = new Label("columns");
        columnsLabel.setStyle("-fx-text-fill: -text-dim;");
        CheckBox wrapOnTyping = new CheckBox("Wrap on typing");
        wrapOnTyping.setSelected(false);
        HBox hardWrapRow = new HBox(10, hardWrapLabel, hardWrapField, columnsLabel, wrapOnTyping);
        hardWrapRow.setAlignment(Pos.CENTER_LEFT);

        // 3. Visual guides
        Label visualGuidesLabel = new Label("Visual guides:");
        visualGuidesLabel.setPrefWidth(120);
        visualGuidesLabel.setStyle("-fx-text-fill: -text;");
        TextField visualGuidesField = new TextField("");
        visualGuidesField.setPromptText("Optional");
        visualGuidesField.setPrefWidth(180);
        Label guidesColumnsLabel = new Label("columns");
        guidesColumnsLabel.setStyle("-fx-text-fill: -text-dim;");
        HBox visualGuidesRow = new HBox(10, visualGuidesLabel, visualGuidesField, guidesColumnsLabel);
        visualGuidesRow.setAlignment(Pos.CENTER_LEFT);

        Label guidesHint = new Label("Specify one guide (80) or several (80, 120)");
        guidesHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 0 0 0 130;");

        // 4. Indents detection checkbox
        CheckBox indentDetectionCheck = new CheckBox("Detect and use existing file indents for editing");
        indentDetectionCheck.setSelected(true);
        disableLink.setOnAction(e -> {
            indentDetectionCheck.setSelected(false);
            banner.setVisible(false);
            banner.setManaged(false);
        });

        indentDetectionCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            banner.setVisible(newVal);
            banner.setManaged(newVal);
        });

        // Configured Code Styles subtree links
        Label codeStylesSubHeader = new Label("Configured Code Styles:");
        codeStylesSubHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 8 0 2 0;");

        GridPane stylesGrid = new GridPane();
        stylesGrid.setHgap(28);
        stylesGrid.setVgap(6);

        List<CategoryDef> children = cat != null ? cat.getChildren() : Collections.emptyList();
        int half = (children.size() + 1) / 2;
        for (int i = 0; i < children.size(); i++) {
            CategoryDef child = children.get(i);
            Hyperlink link = new Hyperlink(child.getName());
            link.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-width: 0;");
            link.setOnAction(e -> navigateTo.accept(child.getPath()));
            stylesGrid.add(link, i / half, i % half);
        }

        VBox generalContent = new VBox(10, lineSepRow, hardWrapRow, visualGuidesRow, guidesHint, indentDetectionCheck,
                codeStylesSubHeader, stylesGrid);

        // Tab switching behavior
        Label formatterPlaceholder = new Label("Formatter markers and external formatting tool options are configured per file type.");
        formatterPlaceholder.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px; -fx-padding: 10 0;");
        formatterPlaceholder.setVisible(false);
        formatterPlaceholder.setManaged(false);

        tabGeneral.setOnAction(e -> {
            tabGeneral.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4 0 0 4;");
            tabFormatter.setStyle("-fx-background-color: #393b40; -fx-text-fill: -text-dim; -fx-background-radius: 0 4 4 0;");
            generalContent.setVisible(true);
            generalContent.setManaged(true);
            formatterPlaceholder.setVisible(false);
            formatterPlaceholder.setManaged(false);
        });

        tabFormatter.setOnAction(e -> {
            tabFormatter.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0 4 4 0;");
            tabGeneral.setStyle("-fx-background-color: #393b40; -fx-text-fill: -text-dim; -fx-background-radius: 4 0 0 4;");
            generalContent.setVisible(false);
            generalContent.setManaged(false);
            formatterPlaceholder.setVisible(true);
            formatterPlaceholder.setManaged(true);
        });

        VBox panel = new VBox(12, topBar, banner, tabHeader, generalContent, formatterPlaceholder);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildCodeStyleSqlLandingPanel(CategoryDef cat, java.util.function.Consumer<String> navigateTo) {
        // Description
        String descText = cat != null && cat.getDescription() != null ? cat.getDescription()
                : "Set of code styles based on SQL.";
        Label desc = new Label(descText);
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px; -fx-padding: 2 0 6 0;");

        // 2-column Grid of 12 SQL Dialects
        GridPane dialectGrid = new GridPane();
        dialectGrid.setHgap(36);
        dialectGrid.setVgap(6);
        dialectGrid.setPadding(new Insets(6, 0, 10, 0));

        List<CategoryDef> children = cat != null ? cat.getChildren() : Collections.emptyList();
        int half = (children.size() + 1) / 2;
        for (int i = 0; i < children.size(); i++) {
            CategoryDef child = children.get(i);
            Hyperlink link = new Hyperlink(child.getName());
            link.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-width: 0;");
            link.setOnAction(e -> navigateTo.accept(child.getPath()));
            dialectGrid.add(link, i / half, i % half);
        }

        VBox panel = new VBox(12, desc, dialectGrid);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildNaturalLanguagesPanel(CategoryDef cat, java.util.function.Consumer<String> navigateTo) {
        // Languages table / list
        Label installedLabel = new Label("Installed languages:");
        installedLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        ListView<String> langListView = new ListView<>();
        langListView.getItems().addAll("English (USA)");
        langListView.getSelectionModel().select(0);
        langListView.setPrefHeight(90);
        langListView.setMaxHeight(110);
        langListView.setStyle("-fx-background-color: #2b2d30; -fx-control-inner-background: #2b2d30;");

        Button addLangBtn = new Button("+");
        addLangBtn.setTooltip(new Tooltip("Add Natural Language"));
        addLangBtn.setStyle("-fx-min-width: 26px; -fx-min-height: 24px;");

        Button removeLangBtn = new Button("—");
        removeLangBtn.setTooltip(new Tooltip("Remove Natural Language"));
        removeLangBtn.setStyle("-fx-min-width: 26px; -fx-min-height: 24px;");

        HBox langToolbar = new HBox(4, addLangBtn, removeLangBtn);
        langToolbar.setAlignment(Pos.CENTER_LEFT);

        VBox langBox = new VBox(6, installedLabel, langListView, langToolbar);

        // Hyperlink to proofreading inspections
        Hyperlink proofreadingLink = new Hyperlink("Configure 'Proofreading' inspections...");
        proofreadingLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 4 0; -fx-border-width: 0;");
        proofreadingLink.setOnAction(e -> navigateTo.accept("Editor / Inspections"));

        // Language Processing section
        Label processingHeader = new Label("Language processing");
        processingHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 8 0 2 0;");

        RadioButton localRadio = new RadioButton("Local");
        localRadio.setSelected(true);

        Hyperlink enableCloudLink = new Hyperlink("Enable Cloud");
        enableCloudLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 0; -fx-border-width: 0;");

        Label cloudHint = new Label("Powered by JetBrains AI Cloud Service. Provides improved grammar checks, style suggestions, and full-sentence rephrasing.");
        cloudHint.setWrapText(true);
        cloudHint.setMaxWidth(620);
        cloudHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        VBox cloudBox = new VBox(2, enableCloudLink, cloudHint);
        HBox processingRow = new HBox(16, localRadio, cloudBox);
        processingRow.setAlignment(Pos.TOP_LEFT);

        // Checkboxes
        CheckBox autoFixCheck = new CheckBox("Automatically fix simple issues as you type (e.g., convert hyphens to dashes)");
        autoFixCheck.setSelected(true);

        CheckBox oxfordSpellingCheck = new CheckBox("Use Oxford Spelling for British English");
        oxfordSpellingCheck.setSelected(true);

        // Subtree links (Grammar & Style, Spelling)
        Label subRulesHeader = new Label("Rules and Dictionaries:");
        subRulesHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 8 0 2 0;");

        VBox rulesBox = new VBox(6);
        List<CategoryDef> children = cat != null ? cat.getChildren() : Collections.emptyList();
        for (CategoryDef child : children) {
            Hyperlink link = new Hyperlink(child.getName());
            link.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-width: 0;");
            link.setOnAction(e -> navigateTo.accept(child.getPath()));
            rulesBox.getChildren().add(link);
        }

        VBox panel = new VBox(12, langBox, proofreadingLink, processingHeader, processingRow, autoFixCheck,
                oxfordSpellingCheck, subRulesHeader, rulesBox);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildGenericCategoryPanel(String fullPath, CategoryDef cat) {
        String leafName = fullPath.contains(" / ") ? fullPath.substring(fullPath.lastIndexOf(" / ") + 3) : fullPath;
        Label title = new Label(leafName);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        String descText = cat != null && cat.getDescription() != null ? cat.getDescription()
                : "Configure " + leafName + " settings and behaviors.";
        Label desc = new Label(descText);
        desc.setWrapText(true);
        desc.setMaxWidth(600);
        desc.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px; -fx-line-spacing: 3;");

        CheckBox enableFeature = new CheckBox("Enable " + leafName);
        enableFeature.setSelected(true);

        Label note = new Label("Settings are applied according to project dialect and workspace profile.");
        note.getStyleClass().add("console-status");

        VBox panel = new VBox(14, title, desc, enableFeature, note);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    // =========================================================================
    // Save & Apply
    // =========================================================================

    @SuppressWarnings("unchecked")
    private static void applySettings(MainWindow mainWindow, AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        // Theme & UI Appearance
        if (inputs.containsKey("appearance_uiTheme")) {
            Object obj = inputs.get("appearance_uiTheme");
            if (obj instanceof ComboBox<?> cb && cb.getValue() != null) {
                String themeName = cb.getValue().toString();
                settings.setUiTheme(themeName);
                AppSettingsStore.Theme t = themeName.toLowerCase().contains("light")
                        ? AppSettingsStore.Theme.LIGHT
                        : AppSettingsStore.Theme.DARK;
                settings.setTheme(t);
                ThemeManager.setTheme(t);
            }
        } else if (inputs.containsKey("themeCombo")) {
            Object obj = inputs.get("themeCombo");
            if (obj instanceof ComboBox<?> cb && cb.getValue() != null) {
                if (cb.getValue() instanceof AppSettingsStore.Theme t) {
                    settings.setTheme(t);
                    ThemeManager.setTheme(t);
                } else {
                    String themeName = cb.getValue().toString();
                    settings.setUiTheme(themeName);
                    AppSettingsStore.Theme t = themeName.toLowerCase().contains("light")
                            ? AppSettingsStore.Theme.LIGHT
                            : AppSettingsStore.Theme.DARK;
                    settings.setTheme(t);
                    ThemeManager.setTheme(t);
                }
            }
        }
        if (inputs.containsKey("appearance_syncWithOs")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_syncWithOs");
            settings.setSyncThemeWithOs(cb.isSelected());
        }
        if (inputs.containsKey("appearance_editorColorScheme")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("appearance_editorColorScheme");
            if (cb.getValue() != null) settings.setEditorColorScheme(cb.getValue());
        }
        if (inputs.containsKey("appearance_differentToolWindowBackground")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_differentToolWindowBackground");
            settings.setDifferentToolWindowBackground(cb.isSelected());
        }
        if (inputs.containsKey("appearance_ideZoom")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("appearance_ideZoom");
            if (cb.getValue() != null) settings.setIdeZoom(cb.getValue());
        }
        if (inputs.containsKey("appearance_useCustomIdeFont")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_useCustomIdeFont");
            settings.setUseCustomIdeFont(cb.isSelected());
        }
        if (inputs.containsKey("appearance_customIdeFontFamily")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("appearance_customIdeFontFamily");
            if (cb.getValue() != null) settings.setCustomIdeFontFamily(cb.getValue());
        }
        if (inputs.containsKey("appearance_customIdeFontSize")) {
            ComboBox<Integer> cb = (ComboBox<Integer>) inputs.get("appearance_customIdeFontSize");
            if (cb.getValue() != null) settings.setCustomIdeFontSize(cb.getValue());
        }
        if (inputs.containsKey("appearance_supportScreenReaders")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_supportScreenReaders");
            settings.setSupportScreenReaders(cb.isSelected());
        }
        if (inputs.containsKey("appearance_useContrastScrollbars")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_useContrastScrollbars");
            settings.setUseContrastScrollbars(cb.isSelected());
        }
        if (inputs.containsKey("appearance_adjustColorsForVisionDeficiency")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_adjustColorsForVisionDeficiency");
            settings.setAdjustColorsForVisionDeficiency(cb.isSelected());
        }
        if (inputs.containsKey("appearance_compactMode")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_compactMode");
            settings.setCompactMode(cb.isSelected());
        }
        if (inputs.containsKey("appearance_alwaysShowFullPathInWindowHeader")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_alwaysShowFullPathInWindowHeader");
            settings.setAlwaysShowFullPathInWindowHeader(cb.isSelected());
        }
        if (inputs.containsKey("appearance_useProjectColorsInMainToolbar")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_useProjectColorsInMainToolbar");
            settings.setUseProjectColorsInMainToolbar(cb.isSelected());
        }
        if (inputs.containsKey("appearance_keepPopupsOpenForToggleItems")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_keepPopupsOpenForToggleItems");
            settings.setKeepPopupsOpenForToggleItems(cb.isSelected());
        }
        if (inputs.containsKey("appearance_dragAndDropWithAltPressedOnly")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_dragAndDropWithAltPressedOnly");
            settings.setDragAndDropWithAltPressedOnly(cb.isSelected());
        }
        if (inputs.containsKey("appearance_smoothScrolling")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_smoothScrolling");
            settings.setSmoothScrolling(cb.isSelected());
        }
        if (inputs.containsKey("appearance_enableMnemonicsInControls")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_enableMnemonicsInControls");
            settings.setEnableMnemonicsInControls(cb.isSelected());
        }
        if (inputs.containsKey("appearance_enableMnemonicsInMenu")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_enableMnemonicsInMenu");
            settings.setEnableMnemonicsInMenu(cb.isSelected());
        }
        if (inputs.containsKey("appearance_displayIconsInMenuItems")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_displayIconsInMenuItems");
            settings.setDisplayIconsInMenuItems(cb.isSelected());
        }
        if (inputs.containsKey("appearance_mainMenuPresentation")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("appearance_mainMenuPresentation");
            if (cb.getValue() != null) settings.setMainMenuPresentation(cb.getValue());
        }
        if (inputs.containsKey("appearance_backgroundImagePath")) {
            settings.setBackgroundImagePath((String) inputs.get("appearance_backgroundImagePath"));
        }
        if (inputs.containsKey("appearance_backgroundImageOpacity")) {
            Object op = inputs.get("appearance_backgroundImageOpacity");
            if (op instanceof Integer i) settings.setBackgroundImageOpacity(i);
        }
        if (inputs.containsKey("appearance_backgroundImagePlacement")) {
            settings.setBackgroundImagePlacement((String) inputs.get("appearance_backgroundImagePlacement"));
        }
        if (inputs.containsKey("appearance_backgroundImageThisProjectOnly")) {
            Object val = inputs.get("appearance_backgroundImageThisProjectOnly");
            if (val instanceof Boolean b) settings.setBackgroundImageThisProjectOnly(b);
        }
        if (inputs.containsKey("appearance_backgroundImageTarget")) {
            settings.setBackgroundImageTarget((String) inputs.get("appearance_backgroundImageTarget"));
        }
        if (inputs.containsKey("appearance_showIndentGuides")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_showIndentGuides");
            settings.setShowIndentGuides(cb.isSelected());
        }
        if (inputs.containsKey("appearance_useSmallerIndents")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_useSmallerIndents");
            settings.setUseSmallerIndents(cb.isSelected());
        }
        if (inputs.containsKey("appearance_showToolWindowBars")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_showToolWindowBars");
            settings.setShowToolWindowBars(cb.isSelected());
        }
        if (inputs.containsKey("appearance_showToolWindowNames")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_showToolWindowNames");
            settings.setShowToolWindowNames(cb.isSelected());
        }
        if (inputs.containsKey("appearance_sideBySideLayoutOnLeft")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_sideBySideLayoutOnLeft");
            settings.setSideBySideLayoutOnLeft(cb.isSelected());
        }
        if (inputs.containsKey("appearance_sideBySideLayoutOnRight")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_sideBySideLayoutOnRight");
            settings.setSideBySideLayoutOnRight(cb.isSelected());
        }
        if (inputs.containsKey("appearance_widescreenToolWindowLayout")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_widescreenToolWindowLayout");
            settings.setWidescreenToolWindowLayout(cb.isSelected());
        }
        if (inputs.containsKey("appearance_rememberSizeForEachToolWindow")) {
            CheckBox cb = (CheckBox) inputs.get("appearance_rememberSizeForEachToolWindow");
            settings.setRememberSizeForEachToolWindow(cb.isSelected());
        }
        if (inputs.containsKey("appearance_presentationModeZoom")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("appearance_presentationModeZoom");
            if (cb.getValue() != null) settings.setPresentationModeZoom(cb.getValue());
        }
        if (inputs.containsKey("appearance_ideAntialiasing")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("appearance_ideAntialiasing");
            if (cb.getValue() != null) settings.setIdeAntialiasing(cb.getValue());
        }
        if (inputs.containsKey("appearance_editorAntialiasing")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("appearance_editorAntialiasing");
            if (cb.getValue() != null) settings.setEditorAntialiasing(cb.getValue());
        }

        // Editor font & size
        if (inputs.containsKey("fontCombo")) {
            ComboBox<String> fontCombo = (ComboBox<String>) inputs.get("fontCombo");
            if (fontCombo.getValue() != null && !fontCombo.getValue().isBlank()) {
                settings.setEditorFontFamily(fontCombo.getValue());
            }
        }
        if (inputs.containsKey("sizeSpinner")) {
            Spinner<Double> sizeSpinner = (Spinner<Double>) inputs.get("sizeSpinner");
            settings.setEditorFontSize(sizeSpinner.getValue());
        }

        // Ctrl Scroll Zoom
        if (inputs.containsKey("ctrlScrollCheck")) {
            CheckBox cb = (CheckBox) inputs.get("ctrlScrollCheck");
            settings.setCtrlScrollZoomEnabled(cb.isSelected());
        }

        // Updates
        if (inputs.containsKey("autoCheck")) {
            CheckBox cb = (CheckBox) inputs.get("autoCheck");
            settings.setAutoUpdateEnabled(cb.isSelected());
        }
        if (inputs.containsKey("autoDownload")) {
            CheckBox cb = (CheckBox) inputs.get("autoDownload");
            settings.setAutoDownloadUpdates(cb.isSelected());
        }
        if (inputs.containsKey("channel")) {
            ComboBox<String> combo = (ComboBox<String>) inputs.get("channel");
            if (combo.getValue() != null) settings.setUpdateChannel(combo.getValue());
        }
        if (inputs.containsKey("endpoint")) {
            TextField tf = (TextField) inputs.get("endpoint");
            if (tf.getText() != null) settings.setUpdateEndpoint(tf.getText().trim());
        }

        // Database settings
        if (inputs.containsKey("timeoutSpinner")) {
            Spinner<Integer> sp = (Spinner<Integer>) inputs.get("timeoutSpinner");
            settings.setQueryTimeoutSeconds(sp.getValue());
        }
        if (inputs.containsKey("maxRowsSpinner")) {
            Spinner<Integer> sp = (Spinner<Integer>) inputs.get("maxRowsSpinner");
            settings.setMaxResultRows(sp.getValue());
        }
        if (inputs.containsKey("autoCommit")) {
            CheckBox cb = (CheckBox) inputs.get("autoCommit");
            settings.setAutoCommit(cb.isSelected());
        }
        if (inputs.containsKey("executeActions")) {
            List<AppSettingsStore.ExecuteActionConfig> list = (List<AppSettingsStore.ExecuteActionConfig>) inputs.get("executeActions");
            settings.setExecuteActions(list);
        }
        if (inputs.containsKey("scriptSplittingCombo")) {
            ComboBox<String> combo = (ComboBox<String>) inputs.get("scriptSplittingCombo");
            if (combo.getValue() != null) settings.setScriptSplitting(combo.getValue());
        }
        if (inputs.containsKey("reviewParamsCheck")) {
            CheckBox cb = (CheckBox) inputs.get("reviewParamsCheck");
            settings.setReviewParametersBeforeExecution(cb.isSelected());
        }
        if (inputs.containsKey("warnUnsafeCheck")) {
            CheckBox cb = (CheckBox) inputs.get("warnUnsafeCheck");
            settings.setWarnUnsafeQueries(cb.isSelected());
        }
        if (inputs.containsKey("pageSizeCombo")) {
            ComboBox<Integer> combo = (ComboBox<Integer>) inputs.get("pageSizeCombo");
            if (combo.getValue() != null) settings.setPageSize(combo.getValue());
        }
        if (inputs.containsKey("showEmpty")) {
            CheckBox cb = (CheckBox) inputs.get("showEmpty");
            settings.setShowEmptySchemas(cb.isSelected());
        }
        if (inputs.containsKey("delimCombo")) {
            ComboBox<String> combo = (ComboBox<String>) inputs.get("delimCombo");
            if (combo.getValue() != null) settings.setCsvDelimiter(combo.getValue());
        }
        if (inputs.containsKey("quoteField")) {
            TextField tf = (TextField) inputs.get("quoteField");
            if (tf.getText() != null && !tf.getText().isEmpty()) settings.setCsvQuoteChar(tf.getText());
        }
        if (inputs.containsKey("csvFormatsList")) {
            @SuppressWarnings("unchecked")
            List<AppSettingsStore.CsvFormatConfig> list = (List<AppSettingsStore.CsvFormatConfig>) inputs.get("csvFormatsList");
            if (list != null && !list.isEmpty()) {
                settings.setCsvFormats(new ArrayList<>(list));
            }
        }
        if (inputs.containsKey("formatsListView")) {
            @SuppressWarnings("unchecked")
            ListView<AppSettingsStore.CsvFormatConfig> lv = (ListView<AppSettingsStore.CsvFormatConfig>) inputs.get("formatsListView");
            if (lv != null && lv.getSelectionModel().getSelectedItem() != null) {
                AppSettingsStore.CsvFormatConfig def = lv.getSelectionModel().getSelectedItem();
                settings.setDefaultCsvFormat(def.getName());
                settings.setCsvDelimiter(CsvFormatEngine.resolveSeparator(def.getValueSeparator()));
                if (!def.getQuotationRules().isEmpty()) {
                    settings.setCsvQuoteChar(def.getQuotationRules().get(0).getLeftQuote());
                }
            }
        }
        if (inputs.containsKey("keymapCombo")) {
            ComboBox<String> combo = (ComboBox<String>) inputs.get("keymapCombo");
            if (combo.getValue() != null) settings.setKeymapPreset(combo.getValue());
        }

        // Output and Results settings
        if (inputs.containsKey("showTimestampForQueryOutput")) {
            CheckBox cb = (CheckBox) inputs.get("showTimestampForQueryOutput");
            settings.setShowTimestampForQueryOutput(cb.isSelected());
        }
        if (inputs.containsKey("enableDbmsOutput")) {
            CheckBox cb = (CheckBox) inputs.get("enableDbmsOutput");
            settings.setEnableDbmsOutput(cb.isSelected());
        }
        if (inputs.containsKey("showResultsInEditor")) {
            CheckBox cb = (CheckBox) inputs.get("showResultsInEditor");
            settings.setShowResultsInEditor(cb.isSelected());
        }
        if (inputs.containsKey("createTitleFromComment")) {
            CheckBox cb = (CheckBox) inputs.get("createTitleFromComment");
            settings.setCreateTitleFromComment(cb.isSelected());
        }
        if (inputs.containsKey("titleAfterCommentText")) {
            TextField tf = (TextField) inputs.get("titleAfterCommentText");
            String t = tf.getText();
            if (t != null && !t.equalsIgnoreCase("comment beginning")) {
                settings.setTitleAfterCommentText(t.trim());
            } else {
                settings.setTitleAfterCommentText("");
            }
        }
        if (inputs.containsKey("showServicesOutput")) {
            ComboBox<String> combo = (ComboBox<String>) inputs.get("showServicesOutput");
            if (combo.getValue() != null) settings.setShowServicesOutput(combo.getValue());
        }
        if (inputs.containsKey("focusServicesInWindowMode")) {
            CheckBox cb = (CheckBox) inputs.get("focusServicesInWindowMode");
            settings.setFocusServicesInWindowMode(cb.isSelected());
        }
        if (inputs.containsKey("openNewServicesTabForSessions")) {
            CheckBox cb = (CheckBox) inputs.get("openNewServicesTabForSessions");
            settings.setOpenNewServicesTabForSessions(cb.isSelected());
        }
        if (inputs.containsKey("activateServicesForSelectedFileOnly")) {
            CheckBox cb = (CheckBox) inputs.get("activateServicesForSelectedFileOnly");
            settings.setActivateServicesForSelectedFileOnly(cb.isSelected());
        }

        // User Parameters settings
        if (inputs.containsKey("enableUserParameters")) {
            CheckBox cb = (CheckBox) inputs.get("enableUserParameters");
            settings.setEnableUserParameters(cb.isSelected());
        }
        if (inputs.containsKey("enableUserParametersInLiteralsWithInjection")) {
            CheckBox cb = (CheckBox) inputs.get("enableUserParametersInLiteralsWithInjection");
            settings.setEnableUserParametersInLiteralsWithInjection(cb.isSelected());
        }
        if (inputs.containsKey("substituteInsideSqlStrings")) {
            CheckBox cb = (CheckBox) inputs.get("substituteInsideSqlStrings");
            settings.setSubstituteInsideSqlStrings(cb.isSelected());
        }
        if (inputs.containsKey("userParameterPatternsList")) {
            List<AppSettingsStore.UserParameterPattern> list = (List<AppSettingsStore.UserParameterPattern>) inputs.get("userParameterPatternsList");
            settings.setUserParameterPatterns(new ArrayList<>(list));
        }

        // Data Editor and Viewer settings
        if (inputs.containsKey("dataEditor_limitPageSizeCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_limitPageSizeCheck");
            settings.setLimitPageSize(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_pageSizeField")) {
            TextField tf = (TextField) inputs.get("dataEditor_pageSizeField");
            try {
                settings.setPageSize(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("dataEditor_prefetchField")) {
            TextField tf = (TextField) inputs.get("dataEditor_prefetchField");
            try {
                settings.setResultSetPrefetchSize(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("dataEditor_filterHistField")) {
            TextField tf = (TextField) inputs.get("dataEditor_filterHistField");
            try {
                settings.setFilterHistorySize(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("dataEditor_maxBytesField")) {
            TextField tf = (TextField) inputs.get("dataEditor_maxBytesField");
            try {
                settings.setMaxBytesLoadedPerValue(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("dataEditor_showFirstRowsCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_showFirstRowsCheck");
            settings.setShowFirstDataRowsInPreview(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_previewDataRowsField")) {
            TextField tf = (TextField) inputs.get("dataEditor_previewDataRowsField");
            try {
                settings.setPreviewDataRows(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }

        // Controls Customization
        if (inputs.containsKey("dataEditor_enablePagingCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_enablePagingCheck");
            settings.setEnablePagingInEditorResults(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_paginationPosCombo")) {
            ComboBox<String> combo = (ComboBox<String>) inputs.get("dataEditor_paginationPosCombo");
            if (combo.getValue() != null) settings.setGridPaginationPosition(combo.getValue());
        }
        if (inputs.containsKey("dataEditor_showQuickActionsCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_showQuickActionsCheck");
            settings.setShowQuickActionsToolbar(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_enableQuickActionsCustomizationCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_enableQuickActionsCustomizationCheck");
            settings.setEnableQuickActionsCustomization(cb.isSelected());
        }

        // Data Presentation
        if (inputs.containsKey("dataEditor_useCustomFontCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_useCustomFontCheck");
            settings.setUseCustomFont(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_fontCombo")) {
            ComboBox<String> combo = (ComboBox<String>) inputs.get("dataEditor_fontCombo");
            if (combo.getValue() != null) settings.setCustomFontFamily(combo.getValue());
        }
        if (inputs.containsKey("dataEditor_sizeField")) {
            TextField tf = (TextField) inputs.get("dataEditor_sizeField");
            try {
                settings.setCustomFontSize(Double.parseDouble(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("dataEditor_lineHeightField")) {
            TextField tf = (TextField) inputs.get("dataEditor_lineHeightField");
            try {
                settings.setCustomLineHeight(Double.parseDouble(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("dataEditor_alternateRowColorsCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_alternateRowColorsCheck");
            settings.setAlternateRowColors(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_booleanCombo")) {
            ComboBox<String> combo = (ComboBox<String>) inputs.get("dataEditor_booleanCombo");
            if (combo.getValue() != null) settings.setShowBooleanValuesAs(combo.getValue());
        }
        if (inputs.containsKey("dataEditor_transposeCombo")) {
            ComboBox<String> combo = (ComboBox<String>) inputs.get("dataEditor_transposeCombo");
            if (combo.getValue() != null) settings.setAutomaticallyTransposeTables(combo.getValue());
        }
        if (inputs.containsKey("dataEditor_binaryTextCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_binaryTextCheck");
            settings.setDetectBinaryAsText(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_binaryUuidCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_binaryUuidCheck");
            settings.setDetectBinaryAsUuid(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_localFilterCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_localFilterCheck");
            settings.setEnableLocalFilterByDefault(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_immediateCompletionCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_immediateCompletionCheck");
            settings.setEnableImmediateCompletionInGridTextCells(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_temporalTzField")) {
            TextField tf = (TextField) inputs.get("dataEditor_temporalTzField");
            settings.setDisplayTemporalDataInTimeZone(tf.getText() != null ? tf.getText().trim() : "");
        }

        // Custom Number Formats
        if (inputs.containsKey("dataEditor_decSepField")) {
            TextField tf = (TextField) inputs.get("dataEditor_decSepField");
            settings.setDecimalSeparator(tf.getText() != null && !tf.getText().isBlank() ? tf.getText().trim() : ".");
        }
        if (inputs.containsKey("dataEditor_groupingSepCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_groupingSepCheck");
            settings.setEnableGroupingSeparator(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_groupingSepField")) {
            TextField tf = (TextField) inputs.get("dataEditor_groupingSepField");
            settings.setGroupingSeparator(tf.getText() != null ? tf.getText() : "");
        }
        if (inputs.containsKey("dataEditor_infField")) {
            TextField tf = (TextField) inputs.get("dataEditor_infField");
            settings.setInfinityText(tf.getText() != null && !tf.getText().isBlank() ? tf.getText().trim() : "Infinity");
        }
        if (inputs.containsKey("dataEditor_nanField")) {
            TextField tf = (TextField) inputs.get("dataEditor_nanField");
            settings.setNanText(tf.getText() != null && !tf.getText().isBlank() ? tf.getText().trim() : "NaN");
        }
        if (inputs.containsKey("dataEditor_numPatternCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_numPatternCheck");
            settings.setEnableNumberPattern(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_numPatternField")) {
            TextField tf = (TextField) inputs.get("dataEditor_numPatternField");
            settings.setNumberPattern(tf.getText() != null ? tf.getText().trim() : "");
        }

        // Custom Date/Time Formats
        if (inputs.containsKey("dataEditor_dtTsCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_dtTsCheck");
            settings.setEnableDatetimeTimestamp(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_dtTsField")) {
            TextField tf = (TextField) inputs.get("dataEditor_dtTsField");
            if (tf.getText() != null && !tf.getText().isBlank()) settings.setDatetimeTimestampPattern(tf.getText().trim());
        }
        if (inputs.containsKey("dataEditor_dtTzCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_dtTzCheck");
            settings.setEnableDatetimeTimestampWithZone(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_dtTzField")) {
            TextField tf = (TextField) inputs.get("dataEditor_dtTzField");
            if (tf.getText() != null && !tf.getText().isBlank()) settings.setDatetimeTimestampWithZonePattern(tf.getText().trim());
        }
        if (inputs.containsKey("dataEditor_timeCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_timeCheck");
            settings.setEnableTime(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_timeField")) {
            TextField tf = (TextField) inputs.get("dataEditor_timeField");
            if (tf.getText() != null && !tf.getText().isBlank()) settings.setTimePattern(tf.getText().trim());
        }
        if (inputs.containsKey("dataEditor_timeTzCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_timeTzCheck");
            settings.setEnableTimeWithZone(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_timeTzField")) {
            TextField tf = (TextField) inputs.get("dataEditor_timeTzField");
            if (tf.getText() != null && !tf.getText().isBlank()) settings.setTimeWithZonePattern(tf.getText().trim());
        }
        if (inputs.containsKey("dataEditor_dateCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_dateCheck");
            settings.setEnableDate(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_dateField")) {
            TextField tf = (TextField) inputs.get("dataEditor_dateField");
            if (tf.getText() != null && !tf.getText().isBlank()) settings.setDatePattern(tf.getText().trim());
        }

        // Data Sorting
        if (inputs.containsKey("dataEditor_sortViaOrderByCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_sortViaOrderByCheck");
            settings.setSortViaOrderBy(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_sortNumericPkCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_sortNumericPkCheck");
            settings.setSortTablesByNumericPk(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_pkDirGroup")) {
            ToggleGroup tg = (ToggleGroup) inputs.get("dataEditor_pkDirGroup");
            RadioButton sel = (RadioButton) tg.getSelectedToggle();
            if (sel != null) settings.setSortTablesByNumericPkDirection(sel.getText());
        }
        if (inputs.containsKey("dataEditor_addColsGroup")) {
            ToggleGroup tg = (ToggleGroup) inputs.get("dataEditor_addColsGroup");
            RadioButton sel = (RadioButton) tg.getSelectedToggle();
            if (sel != null) settings.setAddColumnsToSorting(sel.getText());
        }

        // Data Modification
        if (inputs.containsKey("dataEditor_submitImmediatelyCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_submitImmediatelyCheck");
            settings.setSubmitChangesImmediately(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_enableEditingJoinCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_enableEditingJoinCheck");
            settings.setEnableEditingForQueriesWithJoin(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_showDmlPreviewJoinCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_showDmlPreviewJoinCheck");
            settings.setShowDmlPreviewForQueriesWithJoin(cb.isSelected());
        }

        // URL Click Settings
        if (inputs.containsKey("dataEditor_secureLinksCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_secureLinksCheck");
            settings.setAllowOpenSecureLinks(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_standardLinksCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_standardLinksCheck");
            settings.setAllowOpenStandardLinks(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_localFileLinksCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_localFileLinksCheck");
            settings.setAllowOpenLocalFileLinks(cb.isSelected());
        }
        if (inputs.containsKey("dataEditor_assumeHttpCheck")) {
            CheckBox cb = (CheckBox) inputs.get("dataEditor_assumeHttpCheck");
            settings.setAssumeHttpIfNoProtocol(cb.isSelected());
        }

        // Database Explorer Settings
        if (inputs.containsKey("dbExplorer_rememberFilterState")) {
            CheckBox cb = (CheckBox) inputs.get("dbExplorer_rememberFilterState");
            settings.setRememberFilterState(cb.isSelected());
        }
        if (inputs.containsKey("dbExplorer_showDatabaseColors")) {
            CheckBox cb = (CheckBox) inputs.get("dbExplorer_showDatabaseColors");
            settings.setShowDatabaseColors(cb.isSelected());
        }
        if (inputs.containsKey("dbExplorer_colorDatabaseExplorer")) {
            CheckBox cb = (CheckBox) inputs.get("dbExplorer_colorDatabaseExplorer");
            settings.setColorDatabaseExplorer(cb.isSelected());
        }
        if (inputs.containsKey("dbExplorer_colorEditorTabHeaders")) {
            CheckBox cb = (CheckBox) inputs.get("dbExplorer_colorEditorTabHeaders");
            settings.setColorEditorTabHeaders(cb.isSelected());
        }
        if (inputs.containsKey("dbExplorer_colorEditorBackgrounds")) {
            CheckBox cb = (CheckBox) inputs.get("dbExplorer_colorEditorBackgrounds");
            settings.setColorEditorBackgrounds(cb.isSelected());
        }
        if (inputs.containsKey("dbExplorer_colorEditorToolbars")) {
            CheckBox cb = (CheckBox) inputs.get("dbExplorer_colorEditorToolbars");
            settings.setColorEditorToolbars(cb.isSelected());
        }

        // AI Tools Settings
        if (inputs.containsKey("ai_readDatabaseSchemas")) {
            CheckBox cb = (CheckBox) inputs.get("ai_readDatabaseSchemas");
            settings.setAiReadDatabaseSchemas(cb.isSelected());
        }
        if (inputs.containsKey("ai_modifyDatabaseSchemas")) {
            CheckBox cb = (CheckBox) inputs.get("ai_modifyDatabaseSchemas");
            settings.setAiModifyDatabaseSchemas(cb.isSelected());
        }
        if (inputs.containsKey("ai_readDatabaseData")) {
            CheckBox cb = (CheckBox) inputs.get("ai_readDatabaseData");
            settings.setAiReadDatabaseData(cb.isSelected());
        }
        if (inputs.containsKey("ai_modifyDatabaseData")) {
            CheckBox cb = (CheckBox) inputs.get("ai_modifyDatabaseData");
            settings.setAiModifyDatabaseData(cb.isSelected());
        }

        // Query Files and Consoles Settings
        if (inputs.containsKey("queryFiles_showDataSourceName")) {
            CheckBox cb = (CheckBox) inputs.get("queryFiles_showDataSourceName");
            settings.setShowDataSourceNameInFileTree(cb.isSelected());
        }
        if (inputs.containsKey("queryFiles_useAttachedSearchPathColor")) {
            CheckBox cb = (CheckBox) inputs.get("queryFiles_useAttachedSearchPathColor");
            settings.setUseAttachedSearchPathColorInFileTree(cb.isSelected());
        }
        if (inputs.containsKey("queryFiles_useAttachedDataSourceIcon")) {
            CheckBox cb = (CheckBox) inputs.get("queryFiles_useAttachedDataSourceIcon");
            settings.setUseAttachedDataSourceIconForQueryFiles(cb.isSelected());
        }
        if (inputs.containsKey("queryFiles_defaultConsoleFileName")) {
            TextField tf = (TextField) inputs.get("queryFiles_defaultConsoleFileName");
            if (tf.getText() != null && !tf.getText().isBlank()) {
                settings.setDefaultConsoleFileName(tf.getText().trim());
            }
        }
        if (inputs.containsKey("queryFiles_editorTabTitleTemplate")) {
            TextField tf = (TextField) inputs.get("queryFiles_editorTabTitleTemplate");
            if (tf.getText() != null && !tf.getText().isBlank()) {
                settings.setEditorTabTitleTemplate(tf.getText().trim());
            }
        }
        if (inputs.containsKey("queryFiles_useTemplateForQueryFiles")) {
            CheckBox cb = (CheckBox) inputs.get("queryFiles_useTemplateForQueryFiles");
            settings.setUseTemplateForQueryFiles(cb.isSelected());
        }

        // SQL Dialects Settings
        if (inputs.containsKey("sqlDialects_globalSqlDialect")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("sqlDialects_globalSqlDialect");
            if (cb.getValue() != null) settings.setGlobalSqlDialect(cb.getValue());
        }
        if (inputs.containsKey("sqlDialects_projectSqlDialect")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("sqlDialects_projectSqlDialect");
            if (cb.getValue() != null) settings.setProjectSqlDialect(cb.getValue());
        }
        if (inputs.containsKey("sqlDialects_mappings")) {
            ObservableList<AppSettingsStore.SqlDialectMappingConfig> list =
                    (ObservableList<AppSettingsStore.SqlDialectMappingConfig>) inputs.get("sqlDialects_mappings");
            settings.setSqlDialectMappings(new ArrayList<>(list));
        }

        // SQL Resolution Scopes Settings
        if (inputs.containsKey("sqlResolution_projectScope")) {
            Object val = inputs.get("sqlResolution_projectScope");
            if (val instanceof Button b) {
                settings.setProjectResolutionScope(b.getText());
            } else if (val instanceof String s) {
                settings.setProjectResolutionScope(s);
            }
        }
        if (inputs.containsKey("sqlResolution_mappings")) {
            ObservableList<AppSettingsStore.SqlResolutionScopeMappingConfig> list =
                    (ObservableList<AppSettingsStore.SqlResolutionScopeMappingConfig>) inputs.get("sqlResolution_mappings");
            settings.setSqlResolutionScopeMappings(new ArrayList<>(list));
        }

        // Other Settings
        if (inputs.containsKey("other_confirmCancellation")) {
            CheckBox cb = (CheckBox) inputs.get("other_confirmCancellation");
            settings.setConfirmCancellationForModifySchemaDialogs(cb.isSelected());
        }
        if (inputs.containsKey("other_showPreviewOfValidScript")) {
            CheckBox cb = (CheckBox) inputs.get("other_showPreviewOfValidScript");
            settings.setShowPreviewOfValidScriptWhenUpdatingSource(cb.isSelected());
        }
        if (inputs.containsKey("other_suggestDumpingDdl")) {
            CheckBox cb = (CheckBox) inputs.get("other_suggestDumpingDdl");
            settings.setSuggestDumpingDdlForNewMappings(cb.isSelected());
        }
        if (inputs.containsKey("other_generateContextTemplates")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("other_generateContextTemplates");
            if (cb.getValue() != null) settings.setGenerateContextTemplates(cb.getValue());
        }
        if (inputs.containsKey("other_virtualForeignKeys")) {
            ObservableList<AppSettingsStore.VirtualForeignKeyRule> list =
                    (ObservableList<AppSettingsStore.VirtualForeignKeyRule>) inputs.get("other_virtualForeignKeys");
            settings.setVirtualForeignKeys(new ArrayList<>(list));
        }
        if (inputs.containsKey("other_defaultResolveModeForConsoles")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("other_defaultResolveModeForConsoles");
            if (cb.getValue() != null) settings.setDefaultResolveModeForConsoles(cb.getValue());
        }
        if (inputs.containsKey("other_statementDelimiter")) {
            TextField tf = (TextField) inputs.get("other_statementDelimiter");
            if (tf.getText() != null) settings.setStatementDelimiter(tf.getText().trim());
        }

        // Persist
        AppSettingsStore.save(settings);

        // Apply to open consoles and notify
        mainWindow.applyEditorFontToOpenConsoles();
        mainWindow.setStatus("Settings applied");
    }
}

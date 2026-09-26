package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.ConnectionStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.util.CsvFormatEngine;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Popup;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.geometry.Side;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import com.roze.dbnavigator.ui.action.*;
import com.roze.dbnavigator.ui.colorscheme.ColorSchemeModel;
import com.roze.dbnavigator.ui.colorscheme.ColorSchemeModel.ColorSchemeElement;

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
        general.getChildren().add(new CategoryDef("editor.general.inline_completion", "Inline Completion", "Editor / General / Inline Completion",
                "Configure full line and inline completion suggestions and typing triggers."));
        general.getChildren().add(new CategoryDef("editor.general.output_console", "Output Console", "Editor / General / Output Console",
                "Configure console buffer size, folding, and cyclic buffer limits."));
        general.getChildren().add(new CategoryDef("editor.general.postfix_completion", "Postfix Completion", "Editor / General / Postfix Completion",
                "Configure postfix completion templates and expansions."));

        CategoryDef smartKeys = new CategoryDef("editor.general.smart_keys", "Smart Keys", "Editor / General / Smart Keys",
                "Configure smart typing, auto-closing quotes/brackets, and indent behavior.");
        smartKeys.getChildren().add(new CategoryDef("editor.general.smart_keys.html_css", "HTML/CSS", "Editor / General / Smart Keys / HTML/CSS",
                "Configure tag auto-closing, attribute completion, and CSS identifier selection."));
        smartKeys.getChildren().add(new CategoryDef("editor.general.smart_keys.json", "JSON", "Editor / General / Smart Keys / JSON",
                "Configure quote escaping, comma insertion, and property colon handling in JSON."));
        smartKeys.getChildren().add(new CategoryDef("editor.general.smart_keys.markdown", "Markdown", "Editor / General / Smart Keys / Markdown",
                "Configure table formatting, list numbering, and link handling in Markdown."));
        smartKeys.getChildren().add(new CategoryDef("editor.general.smart_keys.sql", "SQL", "Editor / General / Smart Keys / SQL",
                "Configure string concatenation and code block closing on Enter in SQL."));
        general.getChildren().add(smartKeys);

        general.getChildren().add(new CategoryDef("editor.general.sticky_lines", "Sticky Lines", "Editor / General / Sticky Lines",
                "Keep current scope header visible at the top of the editor while scrolling."));
        editor.getChildren().add(general);

        editor.getChildren().add(new CategoryDef("editor.code_editing", "Code Editing", "Editor / Code Editing",
                "Configure caret movement highlighting, quick doc, refactoring options, error highlighting, and tooltips."));
        editor.getChildren().add(new CategoryDef("editor.font", "Font", "Editor / Font",
                "Customize the font family, font size, line height, ligatures, and typography for SQL consoles and editors."));

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
        tree.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        VBox.setVgrow(tree, Priority.ALWAYS);

        tree.setCellFactory(tv -> new TreeCell<>() {
            private final Label nameLabel = new Label();
            private final Region spacer = new Region();
            private final Label badge = new Label();
            private final HBox container = new HBox(4, nameLabel, spacer, badge);
            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                container.setAlignment(Pos.CENTER_LEFT);
                selectedProperty().addListener((obs, wasSel, isSel) -> updateCellStyle());
                hoverProperty().addListener((obs, wasHov, isHov) -> updateCellStyle());
            }

            private void updateCellStyle() {
                if (isEmpty() || getItem() == null) {
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                if (isSelected()) {
                    setStyle("-fx-background-color: #2e436e; -fx-background-radius: 3; -fx-text-fill: #ffffff;");
                    nameLabel.setStyle("-fx-text-fill: #ffffff;");
                } else if (isHover()) {
                    setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 3; -fx-text-fill: #dfe1e5;");
                    nameLabel.setStyle("-fx-text-fill: #dfe1e5;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #dfe1e5;");
                    nameLabel.setStyle("-fx-text-fill: #dfe1e5;");
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    nameLabel.setText(item);
                    updateCellStyle();
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
        if ("Editor / General / Appearance".equals(fullPath)) {
            return buildEditorAppearancePanel(settings, inputs, navigateTo);
        } else if ("Appearance & Behavior / Appearance".equals(fullPath) || "Appearance".equals(fullPath)) {
            return buildAppearancePanel(settings, inputs, navigateTo);
        } else if ("Appearance & Behavior / System Settings / Updates".equals(fullPath) || "Updates".equals(fullPath)) {
            return buildUpdatesPanel(settings, inputs);
        } else if ("Editor / General / Auto Import".equals(fullPath) || "Auto Import".equals(fullPath)) {
            return buildAutoImportPanel(settings, inputs);
        } else if ("Editor / General / Breadcrumbs".equals(fullPath) || "Breadcrumbs".equals(fullPath)) {
            return buildBreadcrumbsPanel(settings, inputs, navigateTo);
        } else if ("Editor / General / Code Completion".equals(fullPath) || "Code Completion".equals(fullPath)
                || "Editor / General / Code Completion / Popup".equals(fullPath) || "Popup".equals(fullPath)) {
            return buildCodeCompletionPanel(settings, inputs, navigateTo);
        } else if ("Editor / General / Code Folding".equals(fullPath) || "Code Folding".equals(fullPath)) {
            return buildCodeFoldingPanel(settings, inputs);
        } else if ("Editor / General / Editor Tabs".equals(fullPath) || "Editor Tabs".equals(fullPath)) {
            return buildEditorTabsPanel(settings, inputs);
        } else if ("Editor / General / Gutter Icons".equals(fullPath) || "Gutter Icons".equals(fullPath)) {
            return buildGutterIconsPanel(settings, inputs);
        } else if ("Editor / General / Inline Completion".equals(fullPath) || "Inline Completion".equals(fullPath)
                || "Editor / General / Code Completion / Inline".equals(fullPath) || "Inline".equals(fullPath)) {
            return buildInlineCompletionPanel(settings, inputs, navigateTo);
        } else if ("Editor / General / Output Console".equals(fullPath) || "Output Console".equals(fullPath)) {
            return buildOutputConsolePanel(settings, inputs);
        } else if ("Editor / General / Postfix Completion".equals(fullPath) || "Postfix Completion".equals(fullPath)) {
            return buildPostfixCompletionPanel(settings, inputs);
        } else if ("Editor / General / Smart Keys / HTML/CSS".equals(fullPath) || "HTML/CSS".equals(fullPath)) {
            return buildSmartKeysHtmlCssPanel(settings, inputs);
        } else if ("Editor / General / Smart Keys / JSON".equals(fullPath) || "JSON".equals(fullPath)) {
            return buildSmartKeysJsonPanel(settings, inputs);
        } else if ("Editor / General / Smart Keys / Markdown".equals(fullPath) || "Markdown".equals(fullPath)) {
            return buildSmartKeysMarkdownPanel(settings, inputs);
        } else if ("Editor / General / Smart Keys / SQL".equals(fullPath) || "SQL".equals(fullPath)) {
            return buildSmartKeysSqlPanel(settings, inputs);
        } else if ("Editor / General / Smart Keys".equals(fullPath) || "Smart Keys".equals(fullPath)) {
            return buildSmartKeysPanel(settings, inputs, navigateTo);
        } else if ("Editor / General / Sticky Lines".equals(fullPath) || "Sticky Lines".equals(fullPath)) {
            return buildStickyLinesPanel(settings, inputs, navigateTo);
        } else if ("Editor / Code Editing".equals(fullPath) || "Editor / General / Code Editing".equals(fullPath) || "Code Editing".equals(fullPath)) {
            return buildCodeEditingPanel(settings, inputs);
        } else if ("Editor / Font".equals(fullPath) || "Editor / General / Font".equals(fullPath) || "Font".equals(fullPath)) {
            return buildFontPanel(settings, inputs, navigateTo);
        } else if ("Editor / General".equals(fullPath)) {
            return buildGeneralEditorPanel(settings, inputs);
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
            return buildKeymapPanel(settings, inputs, navigateTo);
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
        } else if ("Editor / Color Scheme / General".equals(fullPath) || (cat != null && "cs.general".equals(cat.getId()))) {
            return buildColorSchemeGeneralPanel(settings, inputs, navigateTo);
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

    private static VBox buildSmartKeysPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs,
                                            java.util.function.Consumer<String> navigateTo) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 16, 16));

        // 1. Home moves caret to first non-whitespace character
        CheckBox homeNonWhitespaceCheck = new CheckBox("Home moves caret to first non-whitespace character");
        homeNonWhitespaceCheck.setSelected(settings.isSmartKeysHomeMovesCaret());
        homeNonWhitespaceCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_homeMovesCaret", homeNonWhitespaceCheck);

        // 2. End on blank line moves caret to indent position
        CheckBox endIndentPosCheck = new CheckBox("End on blank line moves caret to indent position");
        endIndentPosCheck.setSelected(settings.isSmartKeysEndBlankLineMovesCaret());
        endIndentPosCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_endBlankLineMovesCaret", endIndentPosCheck);

        // 3. Insert paired brackets (), [], {}, <>
        CheckBox insertPairedBracketsCheck = new CheckBox("Insert paired brackets (), [], {}, <>");
        insertPairedBracketsCheck.setSelected(settings.isSmartKeysInsertPairedBrackets());
        insertPairedBracketsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_insertPairedBrackets", insertPairedBracketsCheck);

        // 4. Insert pair quote
        CheckBox insertPairQuoteCheck = new CheckBox("Insert pair quote");
        insertPairQuoteCheck.setSelected(settings.isSmartKeysInsertPairQuote());
        insertPairQuoteCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_insertPairQuote", insertPairQuoteCheck);

        // 5. Reformat block on typing '}'
        CheckBox reformatBlockOnBraceCheck = new CheckBox("Reformat block on typing '}'");
        reformatBlockOnBraceCheck.setSelected(settings.isSmartKeysReformatBlockOnBrace());
        reformatBlockOnBraceCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_reformatBlockOnBrace", reformatBlockOnBraceCheck);

        // 6. Use \"CamelHumps\" words
        CheckBox useCamelHumpsCheck = new CheckBox("Use \"CamelHumps\" words");
        useCamelHumpsCheck.setSelected(settings.isSmartKeysUseCamelHumps());
        useCamelHumpsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_useCamelHumps", useCamelHumpsCheck);

        // 7. Honor \"CamelHumps\" words settings when selecting on double click
        CheckBox honorCamelHumpsCheck = new CheckBox("Honor \"CamelHumps\" words settings when selecting on double click");
        honorCamelHumpsCheck.setSelected(settings.isSmartKeysHonorCamelHumpsOnDoubleClick());
        honorCamelHumpsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_honorCamelHumpsOnDoubleClick", honorCamelHumpsCheck);

        // 8. Surround selection on typing quote or brace
        CheckBox surroundSelectionCheck = new CheckBox("Surround selection on typing quote or brace");
        surroundSelectionCheck.setSelected(settings.isSmartKeysSurroundSelectionOnQuoteOrBrace());
        surroundSelectionCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_surroundSelectionOnQuoteOrBrace", surroundSelectionCheck);

        // 9. Add multiple carets on double modifier with arrow keys
        boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        String modLabel = isMac ? "\u2325" : "Ctrl";
        CheckBox multiCaretsCheck = new CheckBox("Add multiple carets on double " + modLabel + " with arrow keys");
        multiCaretsCheck.setSelected(settings.isSmartKeysMultiCaretsOnDoubleModifier());
        multiCaretsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_multiCaretsOnDoubleModifier", multiCaretsCheck);

        // 10. Jump outside closing bracket/quote with Tab when typing
        CheckBox jumpOutsideBracketCheck = new CheckBox("Jump outside closing bracket/quote with Tab when typing");
        jumpOutsideBracketCheck.setSelected(settings.isSmartKeysJumpOutsideBracketWithTab());
        jumpOutsideBracketCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_jumpOutsideBracketWithTab", jumpOutsideBracketCheck);

        // Enter Section
        HBox enterHeader = createSectionHeader("Enter");

        CheckBox smartIndentCheck = new CheckBox("Smart indent");
        smartIndentCheck.setSelected(settings.isSmartKeysEnterSmartIndent());
        smartIndentCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_enterSmartIndent", smartIndentCheck);

        CheckBox insertPairBraceCheck = new CheckBox("Insert pair '}'");
        insertPairBraceCheck.setSelected(settings.isSmartKeysEnterInsertPairBrace());
        insertPairBraceCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_enterInsertPairBrace", insertPairBraceCheck);

        CheckBox closeBlockCommentCheck = new CheckBox("Close block comment");
        closeBlockCommentCheck.setSelected(settings.isSmartKeysEnterCloseBlockComment());
        closeBlockCommentCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_enterCloseBlockComment", closeBlockCommentCheck);

        VBox enterBox = new VBox(6, smartIndentCheck, insertPairBraceCheck, closeBlockCommentCheck);
        enterBox.setPadding(new Insets(2, 0, 4, 18));

        // Unindent on Backspace
        Label unindentLabel = new Label("Unindent on Backspace:");
        unindentLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        unindentLabel.setMinWidth(160);

        ComboBox<String> unindentCombo = new ComboBox<>();
        unindentCombo.getItems().addAll(AppSettingsStore.Settings.defaultSmartKeysUnindentOptions());
        unindentCombo.setValue(settings.getSmartKeysUnindentOnBackspace());
        unindentCombo.setPrefWidth(200);
        unindentCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("smartKeys_unindentOnBackspace", unindentCombo);

        HBox unindentRow = new HBox(8, unindentLabel, unindentCombo);
        unindentRow.setAlignment(Pos.CENTER_LEFT);

        // Reformat on paste
        Label reformatPasteLabel = new Label("Reformat on paste:");
        reformatPasteLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        reformatPasteLabel.setMinWidth(160);

        ComboBox<String> reformatPasteCombo = new ComboBox<>();
        reformatPasteCombo.getItems().addAll(AppSettingsStore.Settings.defaultSmartKeysReformatOnPasteOptions());
        reformatPasteCombo.setValue(settings.getSmartKeysReformatOnPaste());
        reformatPasteCombo.setPrefWidth(140);
        reformatPasteCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("smartKeys_reformatOnPaste", reformatPasteCombo);

        HBox reformatPasteRow = new HBox(8, reformatPasteLabel, reformatPasteCombo);
        reformatPasteRow.setAlignment(Pos.CENTER_LEFT);

        // Reformat again to remove custom line breaks
        CheckBox reformatLineBreaksCheck = new CheckBox("Reformat again to remove custom line breaks");
        reformatLineBreaksCheck.setSelected(settings.isSmartKeysReformatRemoveCustomLineBreaks());
        reformatLineBreaksCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        reformatLineBreaksCheck.disableProperty().bind(reformatPasteCombo.valueProperty().isEqualTo("None"));
        inputs.put("smartKeys_reformatRemoveCustomLineBreaks", reformatLineBreaksCheck);

        panel.getChildren().addAll(
                homeNonWhitespaceCheck,
                endIndentPosCheck,
                insertPairedBracketsCheck,
                insertPairQuoteCheck,
                reformatBlockOnBraceCheck,
                useCamelHumpsCheck,
                honorCamelHumpsCheck,
                surroundSelectionCheck,
                multiCaretsCheck,
                jumpOutsideBracketCheck,
                enterHeader,
                enterBox,
                unindentRow,
                reformatPasteRow,
                reformatLineBreaksCheck
        );
        return panel;
    }

    private static VBox buildSmartKeysHtmlCssPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 16, 16));

        // XML/HTML Section
        HBox xmlHtmlHeader = createSectionHeader("XML/HTML");

        CheckBox insertClosingTagCheck = new CheckBox("Insert closing tag on tag completion");
        insertClosingTagCheck.setSelected(settings.isSmartKeysHtmlInsertClosingTag());
        insertClosingTagCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_html_insertClosingTag", insertClosingTagCheck);

        CheckBox insertReqAttributesCheck = new CheckBox("Insert required attributes on tag completion");
        insertReqAttributesCheck.setSelected(settings.isSmartKeysHtmlInsertRequiredAttributes());
        insertReqAttributesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_html_insertRequiredAttributes", insertReqAttributesCheck);

        CheckBox insertReqSubtagsCheck = new CheckBox("Insert required subtags on tag completion");
        insertReqSubtagsCheck.setSelected(settings.isSmartKeysHtmlInsertRequiredSubtags());
        insertReqSubtagsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_html_insertRequiredSubtags", insertReqSubtagsCheck);

        CheckBox startAttributeCheck = new CheckBox("Start attribute on tag completion");
        startAttributeCheck.setSelected(settings.isSmartKeysHtmlStartAttribute());
        startAttributeCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_html_startAttribute", startAttributeCheck);

        CheckBox addQuotesAttrCheck = new CheckBox("Add quotes for attribute value on typing '=' and attribute completion");
        addQuotesAttrCheck.setSelected(settings.isSmartKeysHtmlAddQuotesForAttribute());
        addQuotesAttrCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_html_addQuotesForAttribute", addQuotesAttrCheck);

        CheckBox autoCloseTagCheck = new CheckBox("Auto-close tag on typing '</'");
        autoCloseTagCheck.setSelected(settings.isSmartKeysHtmlAutoCloseTag());
        autoCloseTagCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_html_autoCloseTag", autoCloseTagCheck);

        CheckBox simultaneousTagCheck = new CheckBox("Simultaneous '<tag></tag>' editing");
        simultaneousTagCheck.setSelected(settings.isSmartKeysHtmlSimultaneousTagEditing());
        simultaneousTagCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_html_simultaneousTagEditing", simultaneousTagCheck);

        VBox xmlHtmlBox = new VBox(8,
                insertClosingTagCheck,
                insertReqAttributesCheck,
                insertReqSubtagsCheck,
                startAttributeCheck,
                addQuotesAttrCheck,
                autoCloseTagCheck,
                simultaneousTagCheck
        );
        xmlHtmlBox.setPadding(new Insets(2, 0, 8, 18));

        // CSS Section
        HBox cssHeader = createSectionHeader("CSS");

        CheckBox selectWholeCssIdCheck = new CheckBox("Select whole CSS identifiers on double click");
        selectWholeCssIdCheck.setSelected(settings.isSmartKeysCssSelectWholeCssIdentifiers());
        selectWholeCssIdCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_css_selectWholeCssIdentifiers", selectWholeCssIdCheck);

        VBox cssBox = new VBox(8, selectWholeCssIdCheck);
        cssBox.setPadding(new Insets(2, 0, 8, 18));

        panel.getChildren().addAll(xmlHtmlHeader, xmlHtmlBox, cssHeader, cssBox);
        return panel;
    }

    private static VBox buildSmartKeysJsonPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 16, 16));

        CheckBox insertMissingCommaOnEnterCheck = new CheckBox("Insert missing comma on Enter");
        insertMissingCommaOnEnterCheck.setSelected(settings.isSmartKeysJsonInsertMissingCommaOnEnter());
        insertMissingCommaOnEnterCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_json_insertMissingCommaOnEnter", insertMissingCommaOnEnterCheck);

        CheckBox insertMissingCommaAfterMatchingCheck = new CheckBox("Insert missing comma after matching braces and quotes");
        insertMissingCommaAfterMatchingCheck.setSelected(settings.isSmartKeysJsonInsertMissingCommaAfterMatching());
        insertMissingCommaAfterMatchingCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_json_insertMissingCommaAfterMatching", insertMissingCommaAfterMatchingCheck);

        CheckBox manageCommasOnPasteCheck = new CheckBox("Automatically manage commas when pasting JSON fragments");
        manageCommasOnPasteCheck.setSelected(settings.isSmartKeysJsonManageCommasOnPaste());
        manageCommasOnPasteCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_json_manageCommasOnPaste", manageCommasOnPasteCheck);

        CheckBox escapeTextOnPasteCheck = new CheckBox("Escape text on paste in string literals");
        escapeTextOnPasteCheck.setSelected(settings.isSmartKeysJsonEscapeTextOnPaste());
        escapeTextOnPasteCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_json_escapeTextOnPaste", escapeTextOnPasteCheck);

        CheckBox addQuotesPropertyNamesCheck = new CheckBox("Automatically add quotes to property names when typing ':'");
        addQuotesPropertyNamesCheck.setSelected(settings.isSmartKeysJsonAddQuotesToPropertyNames());
        addQuotesPropertyNamesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_json_addQuotesToPropertyNames", addQuotesPropertyNamesCheck);

        CheckBox addWhitespaceColonCheck = new CheckBox("Automatically add whitespace when typing ':' after property names");
        addWhitespaceColonCheck.setSelected(settings.isSmartKeysJsonAddWhitespaceAfterColon());
        addWhitespaceColonCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_json_addWhitespaceAfterColon", addWhitespaceColonCheck);

        CheckBox moveColonCheck = new CheckBox("Automatically move ':' after the property name if typed inside quotes");
        moveColonCheck.setSelected(settings.isSmartKeysJsonMoveColonAfterPropertyName());
        moveColonCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_json_moveColonAfterPropertyName", moveColonCheck);

        CheckBox moveCommaCheck = new CheckBox("Automatically move comma after the property value or array element if inside quotes");
        moveCommaCheck.setSelected(settings.isSmartKeysJsonMoveCommaAfterPropertyValue());
        moveCommaCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_json_moveCommaAfterPropertyValue", moveCommaCheck);

        panel.getChildren().addAll(
                insertMissingCommaOnEnterCheck,
                insertMissingCommaAfterMatchingCheck,
                manageCommasOnPasteCheck,
                escapeTextOnPasteCheck,
                addQuotesPropertyNamesCheck,
                addWhitespaceColonCheck,
                moveColonCheck,
                moveCommaCheck
        );
        return panel;
    }

    private static VBox buildSmartKeysMarkdownPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 16, 16));

        // Tables Section
        HBox tablesHeader = createSectionHeader("Tables");

        CheckBox reformatTableCheck = new CheckBox("Reformat table when typing");
        reformatTableCheck.setSelected(settings.isSmartKeysMarkdownReformatTable());
        reformatTableCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_markdown_reformatTable", reformatTableCheck);

        CheckBox insertHtmlLineBreakCheck = new CheckBox("Insert HTML line break ('<br/>') instead of new line inside table cells");
        insertHtmlLineBreakCheck.setSelected(settings.isSmartKeysMarkdownInsertHtmlLineBreakInTable());
        insertHtmlLineBreakCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_markdown_insertHtmlLineBreakInTable", insertHtmlLineBreakCheck);

        CheckBox shiftEnterNewTableRowCheck = new CheckBox("Use Shift+Enter to insert new table row");
        shiftEnterNewTableRowCheck.setSelected(settings.isSmartKeysMarkdownShiftEnterNewTableRow());
        shiftEnterNewTableRowCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_markdown_shiftEnterNewTableRow", shiftEnterNewTableRowCheck);

        CheckBox tabNavigateTableCheck = new CheckBox("Use Tab/Shift+Tab to navigate table cells");
        tabNavigateTableCheck.setSelected(settings.isSmartKeysMarkdownTabNavigateTable());
        tabNavigateTableCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_markdown_tabNavigateTable", tabNavigateTableCheck);

        VBox tablesBox = new VBox(8,
                reformatTableCheck,
                insertHtmlLineBreakCheck,
                shiftEnterNewTableRowCheck,
                tabNavigateTableCheck
        );
        tablesBox.setPadding(new Insets(2, 0, 8, 18));

        // Lists Section
        HBox listsHeader = createSectionHeader("Lists");

        CheckBox adjustListIndentCheck = new CheckBox("Adjust indentation on type");
        adjustListIndentCheck.setSelected(settings.isSmartKeysMarkdownAdjustListIndent());
        adjustListIndentCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_markdown_adjustListIndent", adjustListIndentCheck);

        CheckBox smartEnterBackspaceCheck = new CheckBox("Use smart Enter and Backspace");
        smartEnterBackspaceCheck.setSelected(settings.isSmartKeysMarkdownSmartEnterBackspace());
        smartEnterBackspaceCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_markdown_smartEnterBackspace", smartEnterBackspaceCheck);

        CheckBox renumberListCheck = new CheckBox("Renumber list when typing");
        renumberListCheck.setSelected(settings.isSmartKeysMarkdownRenumberList());
        renumberListCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_markdown_renumberList", renumberListCheck);

        Label listNumeratingLabel = new Label("List numerating:");
        listNumeratingLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        listNumeratingLabel.setMinWidth(120);

        ComboBox<String> listNumeratingCombo = new ComboBox<>();
        listNumeratingCombo.getItems().addAll(AppSettingsStore.Settings.defaultMarkdownListNumeratingOptions());
        listNumeratingCombo.setValue(settings.getSmartKeysMarkdownListNumerating());
        listNumeratingCombo.setPrefWidth(200);
        listNumeratingCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("smartKeys_markdown_listNumerating", listNumeratingCombo);

        HBox listNumeratingRow = new HBox(8, listNumeratingLabel, listNumeratingCombo);
        listNumeratingRow.setAlignment(Pos.CENTER_LEFT);

        VBox listsBox = new VBox(8,
                adjustListIndentCheck,
                smartEnterBackspaceCheck,
                renumberListCheck,
                listNumeratingRow
        );
        listsBox.setPadding(new Insets(2, 0, 8, 18));

        // Other Section
        HBox otherHeader = createSectionHeader("Other");

        CheckBox insertLinksOnDropCheck = new CheckBox("Insert links to images on drag and drop");
        insertLinksOnDropCheck.setSelected(settings.isSmartKeysMarkdownInsertLinksOnDrop());
        insertLinksOnDropCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_markdown_insertLinksOnDrop", insertLinksOnDropCheck);

        VBox otherBox = new VBox(8, insertLinksOnDropCheck);
        otherBox.setPadding(new Insets(2, 0, 8, 18));

        panel.getChildren().addAll(tablesHeader, tablesBox, listsHeader, listsBox, otherHeader, otherBox);
        return panel;
    }

    private static VBox buildSmartKeysSqlPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 16, 16));

        CheckBox insertStringConcatCheck = new CheckBox("Insert string concatenation on Enter");
        insertStringConcatCheck.setSelected(settings.isSmartKeysSqlInsertStringConcatOnEnter());
        insertStringConcatCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_sql_insertStringConcatOnEnter", insertStringConcatCheck);

        CheckBox closeCodeBlocksCheck = new CheckBox("Close code blocks on Enter");
        closeCodeBlocksCheck.setSelected(settings.isSmartKeysSqlCloseCodeBlocksOnEnter());
        closeCodeBlocksCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("smartKeys_sql_closeCodeBlocksOnEnter", closeCodeBlocksCheck);

        panel.getChildren().addAll(insertStringConcatCheck, closeCodeBlocksCheck);
        return panel;
    }

    private static final class PostfixTreeItemData {
        final boolean isGroup;
        final String language;
        final AppSettingsStore.PostfixTemplateConfig config;

        PostfixTreeItemData(String language) {
            this.isGroup = true;
            this.language = language;
            this.config = null;
        }

        PostfixTreeItemData(AppSettingsStore.PostfixTemplateConfig config) {
            this.isGroup = false;
            this.language = config != null ? config.getLanguage() : "SQL";
            this.config = config;
        }
    }

    private static VBox buildPostfixCompletionPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 16, 16));

        // 1. Enable postfix completion
        CheckBox enableCheck = new CheckBox("Enable postfix completion");
        enableCheck.setSelected(settings.isPostfixCompletionEnabled());
        enableCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");
        inputs.put("postfixCompletion_enabled", enableCheck);

        // 2. Expand templates with
        Label expandWithLabel = new Label("Expand templates with");
        expandWithLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> expandWithCombo = new ComboBox<>();
        expandWithCombo.getItems().addAll(AppSettingsStore.Settings.defaultPostfixExpandWithOptions());
        expandWithCombo.setValue(settings.getPostfixCompletionExpandWith());
        expandWithCombo.setPrefWidth(90);
        expandWithCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("postfixCompletion_expandWith", expandWithCombo);

        HBox expandRow = new HBox(8, expandWithLabel, expandWithCombo);
        expandRow.setAlignment(Pos.CENTER_LEFT);
        expandRow.disableProperty().bind(enableCheck.selectedProperty().not());

        // Working copy of templates list
        List<AppSettingsStore.PostfixTemplateConfig> templateList = new ArrayList<>();
        for (AppSettingsStore.PostfixTemplateConfig t : settings.getPostfixTemplates()) {
            templateList.add(t.copy());
        }
        inputs.put("postfixCompletion_templates", templateList);

        // TreeView with CheckBox items
        CheckBoxTreeItem<PostfixTreeItemData> rootItem = new CheckBoxTreeItem<>(new PostfixTreeItemData("Root"));
        rootItem.setExpanded(true);

        CheckBoxTreeItem<PostfixTreeItemData> sqlGroupItem = new CheckBoxTreeItem<>(new PostfixTreeItemData("SQL"));
        sqlGroupItem.setExpanded(true);
        rootItem.getChildren().add(sqlGroupItem);

        for (AppSettingsStore.PostfixTemplateConfig t : templateList) {
            CheckBoxTreeItem<PostfixTreeItemData> item = new CheckBoxTreeItem<>(new PostfixTreeItemData(t));
            item.setSelected(t.isEnabled());
            sqlGroupItem.getChildren().add(item);
        }

        boolean allEnabled = templateList.stream().allMatch(AppSettingsStore.PostfixTemplateConfig::isEnabled);
        sqlGroupItem.setSelected(allEnabled);

        TreeView<PostfixTreeItemData> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setStyle("-fx-background-color: -surface; -fx-border-color: -border; -fx-border-width: 1px;");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        treeView.setCellFactory(tv -> new TreeCell<>() {
            private final CheckBox cb = new CheckBox();
            private final Label keyLbl = new Label();
            private final Label descLbl = new Label();
            private final HBox cellContent = new HBox(6, cb, keyLbl, descLbl);

            {
                cellContent.setAlignment(Pos.CENTER_LEFT);
                keyLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 12px;");
                descLbl.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

                cb.setOnAction(e -> {
                    TreeItem<PostfixTreeItemData> ti = getTreeItem();
                    if (ti instanceof CheckBoxTreeItem<PostfixTreeItemData> cti) {
                        cti.setSelected(cb.isSelected());
                        PostfixTreeItemData data = ti.getValue();
                        if (data != null) {
                            if (data.isGroup) {
                                for (TreeItem<PostfixTreeItemData> ch : ti.getChildren()) {
                                    if (ch instanceof CheckBoxTreeItem<PostfixTreeItemData> chCti) {
                                        chCti.setSelected(cb.isSelected());
                                        if (ch.getValue() != null && ch.getValue().config != null) {
                                            ch.getValue().config.setEnabled(cb.isSelected());
                                        }
                                    }
                                }
                            } else if (data.config != null) {
                                data.config.setEnabled(cb.isSelected());
                            }
                        }
                    }
                });
            }

            @Override
            protected void updateItem(PostfixTreeItemData data, boolean empty) {
                super.updateItem(data, empty);
                if (empty || data == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    TreeItem<PostfixTreeItemData> ti = getTreeItem();
                    if (ti instanceof CheckBoxTreeItem<PostfixTreeItemData> cti) {
                        cb.setSelected(cti.isSelected());
                    }
                    if (data.isGroup) {
                        keyLbl.setText(data.language);
                        descLbl.setText("");
                    } else {
                        keyLbl.setText(data.config.getKey());
                        descLbl.setText(data.config.getDescription());
                    }
                    setGraphic(cellContent);
                    setText(null);
                }
            }
        });

        // Toolbar above TreeView
        Button addBtn = new Button("+");
        addBtn.setTooltip(new Tooltip("Add postfix template"));
        addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8;");

        Button removeBtn = new Button("—");
        removeBtn.setTooltip(new Tooltip("Remove template"));
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8;");

        Button editBtn = new Button("✎");
        editBtn.setTooltip(new Tooltip("Edit template"));
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8;");

        Button dupBtn = new Button("⧉");
        dupBtn.setTooltip(new Tooltip("Duplicate template"));
        dupBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8;");

        HBox toolbar = new HBox(2, addBtn, removeBtn, editBtn, dupBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2b2d30; -fx-border-color: -border; -fx-border-width: 1px 1px 0 1px; -fx-border-radius: 4px 4px 0 0;");

        VBox leftColumn = new VBox(0, toolbar, treeView);
        leftColumn.setPrefWidth(340);
        leftColumn.setMinWidth(300);

        // Right column: instructions & Before/After preview
        Label infoLabel = new Label("You have selected the postfix completion language.\nBy clicking the checkbox, you can enable/disable all postfix templates for the language.\nTo enable/disable a postfix template select it inside the group.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
        infoLabel.setMinHeight(46);

        Label beforeLabel = new Label("Before:");
        beforeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 12px;");

        TextArea beforeLineNums = createLineNumArea(2);
        TextArea beforeTextArea = createPreviewTextArea(110);
        beforeLineNums.setText("1\n2");
        beforeTextArea.setText("The sample code featuring selected template will be shown here.\n[Flashing rectangle] shows the place where the intention is applicable.");
        HBox beforeBox = new HBox(0, beforeLineNums, beforeTextArea);
        HBox.setHgrow(beforeTextArea, Priority.ALWAYS);
        beforeBox.setStyle("-fx-border-color: -border; -fx-border-width: 1px; -fx-border-radius: 4px;");

        Label afterLabel = new Label("After:");
        afterLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 12px;");

        TextArea afterLineNums = createLineNumArea(1);
        TextArea afterTextArea = createPreviewTextArea(110);
        afterLineNums.setText("1");
        afterTextArea.setText("Postfix completion invocation result will be shown here.");
        HBox afterBox = new HBox(0, afterLineNums, afterTextArea);
        HBox.setHgrow(afterTextArea, Priority.ALWAYS);
        afterBox.setStyle("-fx-border-color: -border; -fx-border-width: 1px; -fx-border-radius: 4px;");

        VBox rightColumn = new VBox(8, infoLabel, beforeLabel, beforeBox, afterLabel, afterBox);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        // Update preview based on tree selection
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.getValue() == null || newVal.getValue().isGroup) {
                infoLabel.setText("You have selected the postfix completion language.\nBy clicking the checkbox, you can enable/disable all postfix templates for the language.\nTo enable/disable a postfix template select it inside the group.");
                beforeLineNums.setText("1\n2");
                beforeTextArea.setText("The sample code featuring selected template will be shown here.\n[Flashing rectangle] shows the place where the intention is applicable.");
                afterLineNums.setText("1");
                afterTextArea.setText("Postfix completion invocation result will be shown here.");
            } else {
                AppSettingsStore.PostfixTemplateConfig cfg = newVal.getValue().config;
                infoLabel.setText("Expression: " + cfg.getExpression() + "\n" + (cfg.getDescription().isEmpty() ? "" : cfg.getDescription()));
                String beforeSample = (cfg.getExampleBefore() != null && !cfg.getExampleBefore().isEmpty())
                        ? cfg.getExampleBefore() : "authors." + cfg.getKey();
                String afterSample = (cfg.getExampleAfter() != null && !cfg.getExampleAfter().isEmpty())
                        ? cfg.getExampleAfter() : cfg.getExpression().replace("$EXPR$", "authors");
                int bLines = beforeSample.split("\n", -1).length;
                StringBuilder bNum = new StringBuilder();
                for (int i = 1; i <= bLines; i++) { if (i > 1) bNum.append("\n"); bNum.append(i); }
                beforeLineNums.setText(bNum.toString());
                beforeTextArea.setText(beforeSample);

                int aLines = afterSample.split("\n", -1).length;
                StringBuilder aNum = new StringBuilder();
                for (int i = 1; i <= aLines; i++) { if (i > 1) aNum.append("\n"); aNum.append(i); }
                afterLineNums.setText(aNum.toString());
                afterTextArea.setText(afterSample);
            }
        });

        // Add action
        addBtn.setOnAction(e -> {
            showEditPostfixTemplateDialog(panel.getScene() != null ? panel.getScene().getWindow() : null, null, newCfg -> {
                templateList.add(newCfg);
                CheckBoxTreeItem<PostfixTreeItemData> newItem = new CheckBoxTreeItem<>(new PostfixTreeItemData(newCfg));
                newItem.setSelected(newCfg.isEnabled());
                sqlGroupItem.getChildren().add(newItem);
                treeView.getSelectionModel().select(newItem);
            });
        });

        // Remove action
        removeBtn.setOnAction(e -> {
            TreeItem<PostfixTreeItemData> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null && !sel.getValue().isGroup) {
                templateList.remove(sel.getValue().config);
                sqlGroupItem.getChildren().remove(sel);
            }
        });

        // Edit action
        editBtn.setOnAction(e -> {
            TreeItem<PostfixTreeItemData> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null && !sel.getValue().isGroup) {
                AppSettingsStore.PostfixTemplateConfig target = sel.getValue().config;
                showEditPostfixTemplateDialog(panel.getScene() != null ? panel.getScene().getWindow() : null, target, updated -> {
                    target.setKey(updated.getKey());
                    target.setExpression(updated.getExpression());
                    target.setDescription(updated.getDescription());
                    target.setExampleBefore(updated.getExampleBefore());
                    target.setExampleAfter(updated.getExampleAfter());
                    treeView.refresh();
                });
            }
        });

        // Duplicate action
        dupBtn.setOnAction(e -> {
            TreeItem<PostfixTreeItemData> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null && !sel.getValue().isGroup) {
                AppSettingsStore.PostfixTemplateConfig copy = sel.getValue().config.copy();
                copy.setKey(copy.getKey() + "-copy");
                templateList.add(copy);
                CheckBoxTreeItem<PostfixTreeItemData> newItem = new CheckBoxTreeItem<>(new PostfixTreeItemData(copy));
                newItem.setSelected(copy.isEnabled());
                sqlGroupItem.getChildren().add(newItem);
                treeView.getSelectionModel().select(newItem);
            }
        });

        // Initial selection: select the sqlGroupItem or first template
        treeView.getSelectionModel().select(sqlGroupItem);

        HBox splitBox = new HBox(14, leftColumn, rightColumn);
        VBox.setVgrow(splitBox, Priority.ALWAYS);
        splitBox.disableProperty().bind(enableCheck.selectedProperty().not());

        panel.getChildren().addAll(enableCheck, expandRow, splitBox);
        return panel;
    }

    private static void showEditPostfixTemplateDialog(Window owner, AppSettingsStore.PostfixTemplateConfig initial,
                                                      java.util.function.Consumer<AppSettingsStore.PostfixTemplateConfig> onSave) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(initial == null ? "Add Postfix Template" : "Edit Postfix Template");

        TextField keyField = new TextField(initial != null ? initial.getKey() : "");
        keyField.setPromptText("Template key, e.g. from");

        TextField exprField = new TextField(initial != null ? initial.getExpression() : "");
        exprField.setPromptText("Expansion expression, e.g. select * from $EXPR$");

        TextField descField = new TextField(initial != null ? initial.getDescription() : "");
        descField.setPromptText("Description, e.g. select | from authors");

        TextField beforeField = new TextField(initial != null ? initial.getExampleBefore() : "");
        beforeField.setPromptText("Example before, e.g. authors.from");

        TextField afterField = new TextField(initial != null ? initial.getExampleAfter() : "");
        afterField.setPromptText("Example after, e.g. select * from authors");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0, new Label("Key:"), keyField);
        grid.addRow(1, new Label("Expression:"), exprField);
        grid.addRow(2, new Label("Description:"), descField);
        grid.addRow(3, new Label("Before:"), beforeField);
        grid.addRow(4, new Label("After:"), afterField);
        GridPane.setHgrow(keyField, Priority.ALWAYS);
        GridPane.setHgrow(exprField, Priority.ALWAYS);
        GridPane.setHgrow(descField, Priority.ALWAYS);
        GridPane.setHgrow(beforeField, Priority.ALWAYS);
        GridPane.setHgrow(afterField, Priority.ALWAYS);

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: -accent; -fx-text-fill: white; -fx-padding: 5 16;");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-padding: 5 16;");

        okBtn.setOnAction(e -> {
            String k = keyField.getText().trim();
            if (k.isEmpty()) return;
            AppSettingsStore.PostfixTemplateConfig res = new AppSettingsStore.PostfixTemplateConfig(
                    k, "SQL", exprField.getText().trim(), descField.getText().trim(),
                    true, beforeField.getText().trim(), afterField.getText().trim()
            );
            onSave.accept(res);
            dialog.close();
        });
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(8, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, grid, btnBox);
        root.setPadding(new Insets(16));
        dialog.setScene(new Scene(root, 440, 260));
        dialog.getScene().getStylesheets().add(ThemeManager.stylesheetUrl(ThemeManager.getCurrent()));
        dialog.showAndWait();
    }

    private static TextArea createLineNumArea(int lines) {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setFocusTraversable(false);
        area.setPrefWidth(34);
        area.setMinWidth(34);
        area.setMaxWidth(34);
        area.setStyle("-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-control-inner-background: #1e1f22; -fx-text-fill: #565861; -fx-text-alignment: right;");
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            if (i > 1) sb.append("\n");
            sb.append(i);
        }
        area.setText(sb.toString());
        return area;
    }

    private static TextArea createPreviewTextArea(int prefHeight) {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setFocusTraversable(false);
        area.setStyle("-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-control-inner-background: #1e1f22; -fx-text-fill: #bcbec4;");
        area.setPrefHeight(prefHeight);
        return area;
    }

    private static VBox buildAutoImportPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(10, 16, 16, 16));

        HBox xmlHeader = createSectionHeader("XML");

        CheckBox showXmlTooltipCheck = new CheckBox("Show auto-import tooltip");
        showXmlTooltipCheck.setSelected(settings.isShowXmlAutoImportTooltip());
        showXmlTooltipCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_autoImport_showXmlTooltip", showXmlTooltipCheck);

        panel.getChildren().addAll(xmlHeader, showXmlTooltipCheck);
        return panel;
    }

    private static VBox buildBreadcrumbsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs,
                                              java.util.function.Consumer<String> navigateTo) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(10, 16, 16, 16));

        CheckBox showBreadcrumbsCheck = new CheckBox("Show breadcrumbs");
        showBreadcrumbsCheck.setSelected(settings.isShowBreadcrumbs());
        showBreadcrumbsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_breadcrumbs_show", showBreadcrumbsCheck);

        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(6, 0, 6, 24));
        contentBox.disableProperty().bind(showBreadcrumbsCheck.selectedProperty().not());

        // Placement Row
        Label placementLabel = new Label("Placement:");
        placementLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        placementLabel.setMinWidth(80);

        ToggleGroup placementGroup = new ToggleGroup();
        RadioButton topRadio = new RadioButton("Top");
        topRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        topRadio.setToggleGroup(placementGroup);

        RadioButton bottomRadio = new RadioButton("Bottom");
        bottomRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        bottomRadio.setToggleGroup(placementGroup);

        if ("Top".equalsIgnoreCase(settings.getBreadcrumbsPlacement())) {
            topRadio.setSelected(true);
        } else {
            bottomRadio.setSelected(true);
        }
        inputs.put("editor_breadcrumbs_topRadio", topRadio);
        inputs.put("editor_breadcrumbs_bottomRadio", bottomRadio);

        HBox placementRow = new HBox(16, placementLabel, topRadio, bottomRadio);
        placementRow.setAlignment(Pos.CENTER_LEFT);

        // Languages Grid
        Label languagesLabel = new Label("Languages:");
        languagesLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        GridPane langGrid = new GridPane();
        langGrid.setHgap(36);
        langGrid.setVgap(10);
        langGrid.setPadding(new Insets(2, 0, 4, 0));

        CheckBox htmlCheck = new CheckBox("HTML");
        htmlCheck.setSelected(settings.isBreadcrumbsHtml());
        htmlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        htmlCheck.setPrefWidth(110);
        inputs.put("editor_breadcrumbs_html", htmlCheck);

        CheckBox mdCheck = new CheckBox("Markdown");
        mdCheck.setSelected(settings.isBreadcrumbsMarkdown());
        mdCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        mdCheck.setPrefWidth(110);
        inputs.put("editor_breadcrumbs_markdown", mdCheck);

        CheckBox xhtmlCheck = new CheckBox("XHTML");
        xhtmlCheck.setSelected(settings.isBreadcrumbsXhtml());
        xhtmlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        xhtmlCheck.setPrefWidth(110);
        inputs.put("editor_breadcrumbs_xhtml", xhtmlCheck);

        CheckBox jsonCheck = new CheckBox("JSON");
        jsonCheck.setSelected(settings.isBreadcrumbsJson());
        jsonCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        jsonCheck.setPrefWidth(110);
        inputs.put("editor_breadcrumbs_json", jsonCheck);

        CheckBox sqlCheck = new CheckBox("SQL");
        sqlCheck.setSelected(settings.isBreadcrumbsSql());
        sqlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        sqlCheck.setPrefWidth(110);
        inputs.put("editor_breadcrumbs_sql", sqlCheck);

        CheckBox xmlCheck = new CheckBox("XML");
        xmlCheck.setSelected(settings.isBreadcrumbsXml());
        xmlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        xmlCheck.setPrefWidth(110);
        inputs.put("editor_breadcrumbs_xml", xmlCheck);

        langGrid.add(htmlCheck, 0, 0);
        langGrid.add(mdCheck, 1, 0);
        langGrid.add(xhtmlCheck, 2, 0);
        langGrid.add(jsonCheck, 0, 1);
        langGrid.add(sqlCheck, 1, 1);
        langGrid.add(xmlCheck, 2, 1);

        Hyperlink manageColorsLink = new Hyperlink("Manage colors");
        manageColorsLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 8 0 0 0; -fx-border-color: transparent; -fx-cursor: hand;");
        manageColorsLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Editor / Color Scheme / General");
            }
        });

        contentBox.getChildren().addAll(placementRow, languagesLabel, langGrid, manageColorsLink);
        panel.getChildren().addAll(showBreadcrumbsCheck, contentBox);
        return panel;
    }

    private static VBox buildEditorAppearancePanel(AppSettingsStore.Settings settings, Map<String, Object> inputs,
                                                  java.util.function.Consumer<String> navigateTo) {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(10, 16, 16, 16));

        // 1. Caret blinking
        CheckBox caretBlinkCheck = new CheckBox("Caret blinking (ms):");
        caretBlinkCheck.setSelected(settings.isCaretBlinking());
        caretBlinkCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_caretBlinking", caretBlinkCheck);

        TextField caretBlinkMsField = new TextField(String.valueOf(settings.getCaretBlinkingMs()));
        caretBlinkMsField.setPrefWidth(65);
        caretBlinkMsField.setStyle("-fx-font-size: 12px;");
        caretBlinkMsField.disableProperty().bind(caretBlinkCheck.selectedProperty().not());
        inputs.put("editor_appearance_caretBlinkingMs", caretBlinkMsField);

        HBox caretRow = new HBox(8, caretBlinkCheck, caretBlinkMsField);
        caretRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Use block caret
        CheckBox useBlockCaretCheck = new CheckBox("Use block caret");
        useBlockCaretCheck.setSelected(settings.isUseBlockCaret());
        useBlockCaretCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_useBlockCaret", useBlockCaretCheck);

        // 3. Use full line height caret
        CheckBox useFullLineHeightCaretCheck = new CheckBox("Use full line height caret");
        useFullLineHeightCaretCheck.setSelected(settings.isUseFullLineHeightCaret());
        useFullLineHeightCaretCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_useFullLineHeightCaret", useFullLineHeightCaretCheck);

        // 4. Highlight occurrences of selected text
        CheckBox highlightOccurrencesCheck = new CheckBox("Highlight occurrences of selected text");
        highlightOccurrencesCheck.setSelected(settings.isHighlightOccurrences());
        highlightOccurrencesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_highlightOccurrences", highlightOccurrencesCheck);

        // 5. Show hard wrap and visual guides
        CheckBox showHardWrapCheck = new CheckBox("Show hard wrap and visual guides (configured in Code Style options)");
        showHardWrapCheck.setSelected(settings.isShowHardWrapAndVisualGuides());
        showHardWrapCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_showHardWrap", showHardWrapCheck);

        // 6. Show line numbers
        CheckBox showLineNumbersCheck = new CheckBox("Show line numbers:");
        showLineNumbersCheck.setSelected(settings.isShowLineNumbers());
        showLineNumbersCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_showLineNumbers", showLineNumbersCheck);

        ComboBox<String> lineNumbersCombo = new ComboBox<>();
        lineNumbersCombo.getItems().addAll("Absolute", "Relative", "Hybrid");
        lineNumbersCombo.setValue(settings.getLineNumbersMode());
        lineNumbersCombo.setPrefWidth(110);
        lineNumbersCombo.setStyle("-fx-font-size: 12px;");
        lineNumbersCombo.disableProperty().bind(showLineNumbersCheck.selectedProperty().not());
        inputs.put("editor_appearance_lineNumbersMode", lineNumbersCombo);

        HBox lineNumRow = new HBox(8, showLineNumbersCheck, lineNumbersCombo);
        lineNumRow.setAlignment(Pos.CENTER_LEFT);

        // 7. Show lines between statements or functions
        CheckBox showLinesBetweenStatementsCheck = new CheckBox("Show lines between statements or functions");
        showLinesBetweenStatementsCheck.setSelected(settings.isShowLinesBetweenStatements());
        showLinesBetweenStatementsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_showLinesBetweenStatements", showLinesBetweenStatementsCheck);

        // 8. Show whitespaces
        CheckBox showWhitespacesCheck = new CheckBox("Show whitespaces");
        showWhitespacesCheck.setSelected(settings.isShowWhitespaces());
        showWhitespacesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_showWhitespaces", showWhitespacesCheck);

        VBox wsSubBox = new VBox(6);
        wsSubBox.setPadding(new Insets(2, 0, 4, 20));
        wsSubBox.disableProperty().bind(showWhitespacesCheck.selectedProperty().not());

        CheckBox wsLeadingCheck = new CheckBox("Leading");
        wsLeadingCheck.setSelected(settings.isShowWhitespacesLeading());
        wsLeadingCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_wsLeading", wsLeadingCheck);

        CheckBox wsInnerCheck = new CheckBox("Inner");
        wsInnerCheck.setSelected(settings.isShowWhitespacesInner());
        wsInnerCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_wsInner", wsInnerCheck);

        CheckBox wsTrailingCheck = new CheckBox("Trailing");
        wsTrailingCheck.setSelected(settings.isShowWhitespacesTrailing());
        wsTrailingCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_wsTrailing", wsTrailingCheck);

        CheckBox wsSelectionCheck = new CheckBox("Selection");
        wsSelectionCheck.setSelected(settings.isShowWhitespacesSelection());
        wsSelectionCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_wsSelection", wsSelectionCheck);

        wsSubBox.getChildren().addAll(wsLeadingCheck, wsInnerCheck, wsTrailingCheck, wsSelectionCheck);

        // 9. Show indent guides
        CheckBox showIndentGuidesCheck = new CheckBox("Show indent guides");
        showIndentGuidesCheck.setSelected(settings.isShowEditorIndentGuides());
        showIndentGuidesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_showIndentGuides", showIndentGuidesCheck);

        // 10. Show intention bulb
        CheckBox showIntentionBulbCheck = new CheckBox("Show intention bulb");
        showIntentionBulbCheck.setSelected(settings.isShowIntentionBulb());
        showIntentionBulbCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_showIntentionBulb", showIntentionBulbCheck);

        // 11. Show preview for intention actions when available
        CheckBox showPreviewIntentionCheck = new CheckBox("Show preview for intention actions when available");
        showPreviewIntentionCheck.setSelected(settings.isShowPreviewForIntentionActions());
        showPreviewIntentionCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_showPreviewIntention", showPreviewIntentionCheck);

        // 12. Render documentation comments + link
        CheckBox renderDocCommentsCheck = new CheckBox("Render documentation comments");
        renderDocCommentsCheck.setSelected(settings.isRenderDocComments());
        renderDocCommentsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_renderDocComments", renderDocCommentsCheck);

        Label alsoInLabel = new Label("Also in");
        alsoInLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

        Hyperlink readerModeLink = new Hyperlink("Reader mode");
        readerModeLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-cursor: hand;");
        readerModeLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Editor / Reader Mode");
            }
        });

        HBox docCommentsRow = new HBox(6, renderDocCommentsCheck, alsoInLabel, readerModeLink);
        docCommentsRow.setAlignment(Pos.CENTER_LEFT);

        // 13. Show code lens on scrollbar hover
        CheckBox showCodeLensCheck = new CheckBox("Show code lens on scrollbar hover");
        showCodeLensCheck.setSelected(settings.isShowCodeLensOnScrollbarHover());
        showCodeLensCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_showCodeLens", showCodeLensCheck);

        // 14. Use editor font for inlay hints
        CheckBox useEditorFontInlayCheck = new CheckBox("Use editor font for inlay hints");
        useEditorFontInlayCheck.setSelected(settings.isUseEditorFontForInlayHints());
        useEditorFontInlayCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_useEditorFontInlay", useEditorFontInlayCheck);

        // 15. Enable HTML/XML tag tree highlighting
        CheckBox tagTreeCheck = new CheckBox("Enable HTML/XML tag tree highlighting");
        tagTreeCheck.setSelected(settings.isEnableTagTreeHighlighting());
        tagTreeCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_appearance_enableTagTree", tagTreeCheck);

        VBox tagTreeSubBox = new VBox(6);
        tagTreeSubBox.setPadding(new Insets(2, 0, 4, 20));
        tagTreeSubBox.disableProperty().bind(tagTreeCheck.selectedProperty().not());

        Label levelsLabel = new Label("Levels to highlight:");
        levelsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        levelsLabel.setMinWidth(140);
        Spinner<Integer> levelsSpinner = new Spinner<>(1, 20, settings.getTagTreeLevelsToHighlight(), 1);
        levelsSpinner.setPrefWidth(90);
        levelsSpinner.setEditable(true);
        inputs.put("editor_appearance_tagTreeLevels", levelsSpinner);
        HBox levelsRow = new HBox(12, levelsLabel, levelsSpinner);
        levelsRow.setAlignment(Pos.CENTER_LEFT);

        Label opacityLabel = new Label("Opacity:");
        opacityLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        opacityLabel.setMinWidth(140);
        Spinner<Double> opacitySpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.05, 1.0, settings.getTagTreeOpacity(), 0.05));
        opacitySpinner.setPrefWidth(90);
        opacitySpinner.setEditable(true);
        inputs.put("editor_appearance_tagTreeOpacity", opacitySpinner);
        HBox opacityRow = new HBox(12, opacityLabel, opacitySpinner);
        opacityRow.setAlignment(Pos.CENTER_LEFT);

        tagTreeSubBox.getChildren().addAll(levelsRow, opacityRow);

        panel.getChildren().addAll(
                caretRow,
                useBlockCaretCheck,
                useFullLineHeightCaretCheck,
                highlightOccurrencesCheck,
                showHardWrapCheck,
                lineNumRow,
                showLinesBetweenStatementsCheck,
                showWhitespacesCheck,
                wsSubBox,
                showIndentGuidesCheck,
                showIntentionBulbCheck,
                showPreviewIntentionCheck,
                docCommentsRow,
                showCodeLensCheck,
                useEditorFontInlayCheck,
                tagTreeCheck,
                tagTreeSubBox
        );
        return panel;
    }

    private static VBox buildCodeCompletionPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs,
                                                 java.util.function.Consumer<String> navigateTo) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 20, 16));

        // -------------------------------------------------------------
        // Top Section: General Code Completion options (DataGrip Alignment)
        // -------------------------------------------------------------
        CheckBox matchCaseCheck = new CheckBox("Match case:");
        matchCaseCheck.setSelected(settings.isMatchCase());
        matchCaseCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_matchCase", matchCaseCheck);

        ToggleGroup matchCaseGroup = new ToggleGroup();
        RadioButton firstLetterRadio = new RadioButton("First letter only");
        firstLetterRadio.setToggleGroup(matchCaseGroup);
        firstLetterRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        RadioButton allLettersRadio = new RadioButton("All letters");
        allLettersRadio.setToggleGroup(matchCaseGroup);
        allLettersRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if ("All letters".equalsIgnoreCase(settings.getMatchCaseMode())) {
            allLettersRadio.setSelected(true);
        } else {
            firstLetterRadio.setSelected(true);
        }
        firstLetterRadio.disableProperty().bind(matchCaseCheck.selectedProperty().not());
        allLettersRadio.disableProperty().bind(matchCaseCheck.selectedProperty().not());
        inputs.put("codeCompletion_firstLetterRadio", firstLetterRadio);
        inputs.put("codeCompletion_allLettersRadio", allLettersRadio);

        HBox matchCaseRow = new HBox(12, matchCaseCheck, firstLetterRadio, allLettersRadio);
        matchCaseRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox sortAlphaCheck = new CheckBox("Sort suggestions alphabetically");
        sortAlphaCheck.setSelected(settings.isSortSuggestionsAlphabetically());
        sortAlphaCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_sortAlphabetically", sortAlphaCheck);

        CheckBox showAsYouTypeCheck = new CheckBox("Show suggestions as you type");
        showAsYouTypeCheck.setSelected(settings.isShowSuggestionsAsYouType());
        showAsYouTypeCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_showSuggestionsAsYouType", showAsYouTypeCheck);

        CheckBox insertSelectedKeysCheck = new CheckBox("Insert selected suggestion by pressing space, dot, or other context-dependent keys");
        insertSelectedKeysCheck.setSelected(settings.isInsertSelectedSuggestionByContextKeys());
        insertSelectedKeysCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        insertSelectedKeysCheck.setPadding(new Insets(0, 0, 0, 24));
        insertSelectedKeysCheck.disableProperty().bind(showAsYouTypeCheck.selectedProperty().not());
        inputs.put("codeCompletion_insertSelectedByContextKeys", insertSelectedKeysCheck);

        CheckBox showDocCheck = new CheckBox("Show the documentation popup in");
        showDocCheck.setSelected(settings.isShowDocPopup());
        showDocCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_showDocPopup", showDocCheck);

        TextField docDelayField = new TextField(String.valueOf(settings.getDocPopupDelayMs()));
        docDelayField.setPrefWidth(55);
        docDelayField.setStyle("-fx-font-size: 12px;");
        docDelayField.disableProperty().bind(showDocCheck.selectedProperty().not());
        inputs.put("codeCompletion_docPopupDelay", docDelayField);

        Label docMsLabel = new Label("ms");
        docMsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        docMsLabel.disableProperty().bind(showDocCheck.selectedProperty().not());

        HBox docRow = new HBox(8, showDocCheck, docDelayField, docMsLabel);
        docRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox insertParensCheck = new CheckBox("Insert parentheses automatically when applicable");
        insertParensCheck.setSelected(settings.isInsertParenthesesAutomatically());
        insertParensCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_insertParentheses", insertParensCheck);

        // -------------------------------------------------------------
        // Machine Learning-Assisted Completion Section
        // -------------------------------------------------------------
        HBox mlHeader = createSectionHeader("Machine Learning-Assisted Completion");

        Label mlIntro1 = new Label("Go to ");
        mlIntro1.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        Hyperlink mlLink = new Hyperlink("Inline Completion settings page");
        mlLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 0; -fx-border-width: 0;");
        if (navigateTo != null) {
            mlLink.setOnAction(e -> navigateTo.accept("Editor / General / Code Completion / Inline"));
        }
        Label mlIntro2 = new Label(" to adjust inline completion (e.g. Full Line Code Completion) settings");
        mlIntro2.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        HBox mlIntroRow = new HBox(0, mlIntro1, mlLink, mlIntro2);
        mlIntroRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox mlSortSuggestionsCheck = new CheckBox("Sort completion suggestions based on machine learning");
        mlSortSuggestionsCheck.setSelected(settings.isMlSortSuggestions());
        mlSortSuggestionsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_mlSortSuggestions", mlSortSuggestionsCheck);

        Label mlHelp = new Label(" ⓘ");
        mlHelp.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px; -fx-cursor: hand;");
        mlHelp.setTooltip(new Tooltip("Order suggestions using machine learning models trained on code"));
        HBox mlSortRow = new HBox(4, mlSortSuggestionsCheck, mlHelp);
        mlSortRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox mlSqlCheck = new CheckBox("SQL");
        mlSqlCheck.setSelected(settings.isMlSortSql());
        mlSqlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        mlSqlCheck.setPadding(new Insets(0, 0, 0, 24));
        mlSqlCheck.disableProperty().bind(mlSortSuggestionsCheck.selectedProperty().not());
        inputs.put("codeCompletion_mlSortSql", mlSqlCheck);

        CheckBox mlMarkPosCheck = new CheckBox("Mark position changes in the completion popup ↑↓");
        mlMarkPosCheck.setSelected(settings.isMlMarkPositionChanges());
        mlMarkPosCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_mlMarkPositionChanges", mlMarkPosCheck);

        CheckBox mlMarkRelevantCheck = new CheckBox("Mark the most relevant item in the completion popup ★");
        mlMarkRelevantCheck.setSelected(settings.isMlMarkMostRelevant());
        mlMarkRelevantCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_mlMarkMostRelevant", mlMarkRelevantCheck);

        // -------------------------------------------------------------
        // HTML Section
        // -------------------------------------------------------------
        HBox htmlHeader = createSectionHeader("HTML");
        CheckBox htmlTagCheck = new CheckBox("Enable auto-popup of tag name code completion when typing in HTML text");
        htmlTagCheck.setSelected(settings.isHtmlAutoPopupTagCompletion());
        htmlTagCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_htmlAutoPopupTagCompletion", htmlTagCheck);

        // -------------------------------------------------------------
        // Parameter Info Section
        // -------------------------------------------------------------
        HBox paramHeader = createSectionHeader("Parameter Info");

        CheckBox paramInfoCheck = new CheckBox("Show the parameter info popup in");
        paramInfoCheck.setSelected(settings.isShowParameterInfoPopup());
        paramInfoCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_showParameterInfoPopup", paramInfoCheck);

        TextField paramDelayField = new TextField(String.valueOf(settings.getParameterInfoDelayMs()));
        paramDelayField.setPrefWidth(60);
        paramDelayField.setStyle("-fx-font-size: 12px;");
        paramDelayField.disableProperty().bind(paramInfoCheck.selectedProperty().not());
        inputs.put("codeCompletion_parameterInfoDelay", paramDelayField);

        Label paramMsLabel = new Label("ms");
        paramMsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        paramMsLabel.disableProperty().bind(paramInfoCheck.selectedProperty().not());

        HBox paramRow = new HBox(8, paramInfoCheck, paramDelayField, paramMsLabel);
        paramRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox showFullSignaturesCheck = new CheckBox("Show full method signatures");
        showFullSignaturesCheck.setSelected(settings.isShowFullMethodSignatures());
        showFullSignaturesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_showFullMethodSignatures", showFullSignaturesCheck);

        // -------------------------------------------------------------
        // SQL Section (DataGrip Alignment)
        // -------------------------------------------------------------
        HBox sqlHeader = createSectionHeader("SQL");

        Label suggestObjectsLabel = new Label("Suggest objects from:");
        suggestObjectsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ToggleGroup suggestGroup = new ToggleGroup();
        RadioButton searchPathRadio = new RadioButton("The current search path only");
        searchPathRadio.setToggleGroup(suggestGroup);
        searchPathRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton scopeRadio = new RadioButton("The current scope");
        scopeRadio.setToggleGroup(suggestGroup);
        scopeRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton allSchemasRadio = new RadioButton("All available schemas");
        allSchemasRadio.setToggleGroup(suggestGroup);
        allSchemasRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        String currentSuggest = settings.getSqlSuggestObjectsFrom();
        if ("The current search path only".equalsIgnoreCase(currentSuggest)) {
            searchPathRadio.setSelected(true);
        } else if ("All available schemas".equalsIgnoreCase(currentSuggest)) {
            allSchemasRadio.setSelected(true);
        } else {
            scopeRadio.setSelected(true);
        }
        inputs.put("codeCompletion_suggestSearchPathRadio", searchPathRadio);
        inputs.put("codeCompletion_suggestScopeRadio", scopeRadio);
        inputs.put("codeCompletion_suggestAllSchemasRadio", allSchemasRadio);

        VBox suggestRadioBox = new VBox(6, searchPathRadio, scopeRadio, allSchemasRadio);
        suggestRadioBox.setPadding(new Insets(0, 0, 0, 20));
        VBox suggestBox = new VBox(6, suggestObjectsLabel, suggestRadioBox);

        // Qualify object with:
        Label qualifyWithLabel = new Label("Qualify object with:");
        qualifyWithLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        GridPane qualifyWithGrid = new GridPane();
        qualifyWithGrid.setHgap(12);
        qualifyWithGrid.setVgap(8);
        qualifyWithGrid.setPadding(new Insets(2, 0, 4, 20));

        String[] qualifyOptions = new String[]{"Always", "On collisions", "Never"};

        Label dbLabel = new Label("Database:");
        dbLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        dbLabel.setMinWidth(110);
        ComboBox<String> dbCombo = new ComboBox<>();
        dbCombo.getItems().addAll(qualifyOptions);
        dbCombo.getSelectionModel().select(settings.getQualifyWithDatabase());
        dbCombo.setPrefWidth(130);
        inputs.put("codeCompletion_qualifyWithDatabase", dbCombo);
        qualifyWithGrid.addRow(0, dbLabel, dbCombo);

        Label schemaLabel = new Label("Schema:");
        schemaLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        schemaLabel.setMinWidth(110);
        ComboBox<String> schemaCombo = new ComboBox<>();
        schemaCombo.getItems().addAll(qualifyOptions);
        schemaCombo.getSelectionModel().select(settings.getQualifyWithSchema());
        schemaCombo.setPrefWidth(130);
        inputs.put("codeCompletion_qualifyWithSchema", schemaCombo);
        qualifyWithGrid.addRow(1, schemaLabel, schemaCombo);

        Label tableLabel = new Label("Table/View:");
        tableLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        tableLabel.setMinWidth(110);
        ComboBox<String> tableCombo = new ComboBox<>();
        tableCombo.getItems().addAll(qualifyOptions);
        tableCombo.getSelectionModel().select(settings.getQualifyWithTableView());
        tableCombo.setPrefWidth(130);
        inputs.put("codeCompletion_qualifyWithTableView", tableCombo);
        qualifyWithGrid.addRow(2, tableLabel, tableCombo);

        Label aliasLabel = new Label("Table/view alias:");
        aliasLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        aliasLabel.setMinWidth(110);
        ComboBox<String> aliasCombo = new ComboBox<>();
        aliasCombo.getItems().addAll(qualifyOptions);
        aliasCombo.getSelectionModel().select(settings.getQualifyWithTableAlias());
        aliasCombo.setPrefWidth(130);
        inputs.put("codeCompletion_qualifyWithTableAlias", aliasCombo);
        qualifyWithGrid.addRow(3, aliasLabel, aliasCombo);

        // Qualify object in:
        Label qualifyInLabel = new Label("Qualify object in:");
        qualifyInLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        GridPane qualifyInGrid = new GridPane();
        qualifyInGrid.setHgap(12);
        qualifyInGrid.setVgap(8);
        qualifyInGrid.setPadding(new Insets(2, 0, 4, 20));

        Label basicLabel = new Label("Basic completion");
        basicLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        basicLabel.setMinWidth(110);
        ComboBox<String> basicCombo = new ComboBox<>();
        basicCombo.getItems().addAll(qualifyOptions);
        basicCombo.getSelectionModel().select(settings.getQualifyInBasicCompletion());
        basicCombo.setPrefWidth(130);
        inputs.put("codeCompletion_qualifyInBasic", basicCombo);
        qualifyInGrid.addRow(0, basicLabel, basicCombo);

        Label joinLabel = new Label("JOIN completion");
        joinLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        joinLabel.setMinWidth(110);
        ComboBox<String> joinCombo = new ComboBox<>();
        joinCombo.getItems().addAll(qualifyOptions);
        joinCombo.getSelectionModel().select(settings.getQualifyInJoinCompletion());
        joinCombo.setPrefWidth(130);
        inputs.put("codeCompletion_qualifyInJoin", joinCombo);
        qualifyInGrid.addRow(1, joinLabel, joinCombo);

        Label refactorLabel = new Label("Refactoring");
        refactorLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        refactorLabel.setMinWidth(110);
        ComboBox<String> refactorCombo = new ComboBox<>();
        refactorCombo.getItems().addAll(qualifyOptions);
        refactorCombo.getSelectionModel().select(settings.getQualifyInRefactoring());
        refactorCombo.setPrefWidth(130);
        inputs.put("codeCompletion_qualifyInRefactoring", refactorCombo);
        qualifyInGrid.addRow(2, refactorLabel, refactorCombo);

        Label templatesLabel = new Label("Live templates");
        templatesLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        templatesLabel.setMinWidth(110);
        ComboBox<String> templatesCombo = new ComboBox<>();
        templatesCombo.getItems().addAll(qualifyOptions);
        templatesCombo.getSelectionModel().select(settings.getQualifyInLiveTemplates());
        templatesCombo.setPrefWidth(130);
        inputs.put("codeCompletion_qualifyInLiveTemplates", templatesCombo);
        qualifyInGrid.addRow(3, templatesLabel, templatesCombo);

        Label dndLabel = new Label("Drag-n-Drop");
        dndLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        dndLabel.setMinWidth(110);
        ComboBox<String> dndCombo = new ComboBox<>();
        dndCombo.getItems().addAll(qualifyOptions);
        dndCombo.getSelectionModel().select(settings.getQualifyInDragDrop());
        dndCombo.setPrefWidth(130);
        inputs.put("codeCompletion_qualifyInDragDrop", dndCombo);
        qualifyInGrid.addRow(4, dndLabel, dndCombo);

        Separator sqlSubSep = new Separator();
        sqlSubSep.setStyle("-fx-opacity: 0.35;");

        // JOIN clauses:
        Label joinClausesLabel = new Label("JOIN clauses:");
        joinClausesLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        CheckBox joinUseAliasesCheck = new CheckBox("Use aliases in completion for JOIN");
        joinUseAliasesCheck.setSelected(settings.isJoinUseAliases());
        joinUseAliasesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_joinUseAliases", joinUseAliasesCheck);

        CheckBox joinInvertOperandsCheck = new CheckBox("Invert order of operands in auto-generated ON clause");
        joinInvertOperandsCheck.setSelected(settings.isJoinInvertOperands());
        joinInvertOperandsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_joinInvertOperands", joinInvertOperandsCheck);

        CheckBox joinSuggestNonStrictFkCheck = new CheckBox("Suggest non-strict foreign keys based on the name matching");
        joinSuggestNonStrictFkCheck.setSelected(settings.isJoinSuggestNonStrictFk());
        joinSuggestNonStrictFkCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_joinSuggestNonStrictFk", joinSuggestNonStrictFkCheck);

        VBox joinClausesSubBox = new VBox(6, joinUseAliasesCheck, joinInvertOperandsCheck, joinSuggestNonStrictFkCheck);
        joinClausesSubBox.setPadding(new Insets(0, 0, 0, 20));
        VBox joinClausesBox = new VBox(6, joinClausesLabel, joinClausesSubBox);

        // Table aliases:
        Label tableAliasesLabel = new Label("Table aliases:");
        tableAliasesLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        CheckBox tableAliasesAutoAddCheck = new CheckBox("Automatically add aliases when completing table names");
        tableAliasesAutoAddCheck.setSelected(settings.isTableAliasesAutoAdd());
        tableAliasesAutoAddCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_tableAliasesAutoAdd", tableAliasesAutoAddCheck);

        CheckBox tableAliasesSuggestCheck = new CheckBox("Suggest alias names in completion after table names");
        tableAliasesSuggestCheck.setSelected(settings.isTableAliasesSuggest());
        tableAliasesSuggestCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeCompletion_tableAliasesSuggest", tableAliasesSuggestCheck);

        // Custom aliases table
        ObservableList<AppSettingsStore.TableAliasConfig> aliasItems = FXCollections.observableArrayList();
        if (settings.getCustomTableAliases() != null) {
            for (AppSettingsStore.TableAliasConfig tac : settings.getCustomTableAliases()) {
                aliasItems.add(tac.copy());
            }
        }
        inputs.put("codeCompletion_customTableAliases", aliasItems);

        TableView<AppSettingsStore.TableAliasConfig> aliasTable = new TableView<>(aliasItems);
        aliasTable.setEditable(true);
        aliasTable.setPrefHeight(150);
        aliasTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AppSettingsStore.TableAliasConfig, String> colTableName = new TableColumn<>("Table name");
        colTableName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTableName()));
        colTableName.setCellFactory(TextFieldTableCell.forTableColumn());
        colTableName.setOnEditCommit(e -> {
            AppSettingsStore.TableAliasConfig row = e.getRowValue();
            if (row != null) {
                row.setTableName(e.getNewValue() != null ? e.getNewValue().trim() : "");
            }
        });
        colTableName.setPrefWidth(220);

        TableColumn<AppSettingsStore.TableAliasConfig, String> colCustomAlias = new TableColumn<>("Custom alias");
        colCustomAlias.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomAlias()));
        colCustomAlias.setCellFactory(TextFieldTableCell.forTableColumn());
        colCustomAlias.setOnEditCommit(e -> {
            AppSettingsStore.TableAliasConfig row = e.getRowValue();
            if (row != null) {
                row.setCustomAlias(e.getNewValue() != null ? e.getNewValue().trim() : "");
            }
        });
        colCustomAlias.setPrefWidth(220);

        aliasTable.getColumns().addAll(colTableName, colCustomAlias);

        // Placeholder when empty
        Label noAliasLabel = new Label("No custom aliases");
        noAliasLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        Hyperlink addAliasLink = new Hyperlink("Add alias");
        addAliasLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 0; -fx-border-width: 0;");
        VBox placeholderBox = new VBox(4, noAliasLabel, addAliasLink);
        placeholderBox.setAlignment(Pos.CENTER);
        aliasTable.setPlaceholder(placeholderBox);

        // Table toolbar: + and —
        Button addAliasBtn = new Button("+");
        addAliasBtn.setPrefWidth(28);
        addAliasBtn.setStyle("-fx-font-size: 12px;");

        Button removeAliasBtn = new Button("—");
        removeAliasBtn.setPrefWidth(28);
        removeAliasBtn.setStyle("-fx-font-size: 12px;");
        removeAliasBtn.disableProperty().bind(aliasTable.getSelectionModel().selectedItemProperty().isNull());

        Runnable addAliasAction = () -> {
            AppSettingsStore.TableAliasConfig newItem = new AppSettingsStore.TableAliasConfig("", "");
            aliasItems.add(newItem);
            aliasTable.getSelectionModel().select(newItem);
            aliasTable.scrollTo(newItem);
        };
        addAliasBtn.setOnAction(e -> addAliasAction.run());
        addAliasLink.setOnAction(e -> addAliasAction.run());

        removeAliasBtn.setOnAction(e -> {
            AppSettingsStore.TableAliasConfig sel = aliasTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                aliasItems.remove(sel);
            }
        });

        HBox tableToolbar = new HBox(4, addAliasBtn, removeAliasBtn);
        tableToolbar.setAlignment(Pos.CENTER_LEFT);

        VBox aliasTableContainer = new VBox(4, tableToolbar, aliasTable);
        aliasTableContainer.setPadding(new Insets(0, 0, 0, 20));

        VBox tableAliasesSubBox = new VBox(6, tableAliasesAutoAddCheck, tableAliasesSuggestCheck);
        tableAliasesSubBox.setPadding(new Insets(0, 0, 0, 20));
        VBox tableAliasesBox = new VBox(6, tableAliasesLabel, tableAliasesSubBox, aliasTableContainer);

        // Additional characters to accept completion:
        Label addAcceptLabel = new Label("Additional characters to accept completion:");
        addAcceptLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField addAcceptField = new TextField(settings.getAdditionalAcceptCharacters());
        addAcceptField.setPrefWidth(200);
        addAcceptField.setStyle("-fx-font-size: 12px;");
        inputs.put("codeCompletion_additionalAcceptCharacters", addAcceptField);

        HBox addAcceptRow = new HBox(12, addAcceptLabel, addAcceptField);
        addAcceptRow.setAlignment(Pos.CENTER_LEFT);
        addAcceptRow.setPadding(new Insets(8, 0, 0, 0));

        // Assemble SQL section
        VBox sqlBox = new VBox(10,
                sqlHeader,
                suggestBox,
                qualifyWithLabel,
                qualifyWithGrid,
                qualifyInLabel,
                qualifyInGrid,
                sqlSubSep,
                joinClausesBox,
                tableAliasesBox,
                addAcceptRow
        );

        // Put everything together
        panel.getChildren().addAll(
                matchCaseRow,
                sortAlphaCheck,
                showAsYouTypeCheck,
                insertSelectedKeysCheck,
                docRow,
                insertParensCheck,
                mlHeader,
                mlIntroRow,
                mlSortRow,
                mlSqlCheck,
                mlMarkPosCheck,
                mlMarkRelevantCheck,
                htmlHeader,
                htmlTagCheck,
                paramHeader,
                paramRow,
                showFullSignaturesCheck,
                sqlBox
        );

        return panel;
    }

    private static VBox buildCodeFoldingPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 20, 16));

        // Top Row: Show code folding arrows + mode combo
        CheckBox showArrowsCheck = new CheckBox("Show code folding arrows");
        showArrowsCheck.setSelected(settings.isShowCodeFoldingArrows());
        showArrowsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_showArrows", showArrowsCheck);

        ComboBox<String> arrowsModeCombo = new ComboBox<>();
        arrowsModeCombo.getItems().addAll("Always", "On mouse hover");
        arrowsModeCombo.getSelectionModel().select(settings.getShowCodeFoldingArrowsMode());
        arrowsModeCombo.setPrefWidth(140);
        arrowsModeCombo.setStyle("-fx-font-size: 12px;");
        arrowsModeCombo.disableProperty().bind(showArrowsCheck.selectedProperty().not());
        inputs.put("codeFolding_showArrowsMode", arrowsModeCombo);

        HBox topRow = new HBox(10, showArrowsCheck, arrowsModeCombo);
        topRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox showBottomArrowsCheck = new CheckBox("Show bottom arrows");
        showBottomArrowsCheck.setSelected(settings.isShowBottomArrows());
        showBottomArrowsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        showBottomArrowsCheck.setPadding(new Insets(0, 0, 0, 24));
        showBottomArrowsCheck.disableProperty().bind(showArrowsCheck.selectedProperty().not());
        inputs.put("codeFolding_showBottomArrows", showBottomArrowsCheck);

        Label foldByDefaultLabel = new Label("Fold by default:");
        foldByDefaultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text; -fx-padding: 8 0 0 0;");

        // 1. General Section
        HBox genHeader = createSectionHeader("General");

        CheckBox foldFileHeaderCheck = new CheckBox("File header");
        foldFileHeaderCheck.setSelected(settings.isFoldFileHeader());
        foldFileHeaderCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldFileHeader", foldFileHeaderCheck);

        CheckBox foldImportsCheck = new CheckBox("Imports");
        foldImportsCheck.setSelected(settings.isFoldImports());
        foldImportsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldImports", foldImportsCheck);

        CheckBox foldDocCommentsCheck = new CheckBox("Documentation comments");
        foldDocCommentsCheck.setSelected(settings.isFoldDocComments());
        foldDocCommentsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldDocComments", foldDocCommentsCheck);

        CheckBox foldMethodBodiesCheck = new CheckBox("Method bodies");
        foldMethodBodiesCheck.setSelected(settings.isFoldMethodBodies());
        foldMethodBodiesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldMethodBodies", foldMethodBodiesCheck);

        CheckBox foldCustomRegionsCheck = new CheckBox("Custom folding regions");
        foldCustomRegionsCheck.setSelected(settings.isFoldCustomRegions());
        foldCustomRegionsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldCustomRegions", foldCustomRegionsCheck);

        VBox genBox = new VBox(6, foldFileHeaderCheck, foldImportsCheck, foldDocCommentsCheck, foldMethodBodiesCheck, foldCustomRegionsCheck);
        genBox.setPadding(new Insets(0, 0, 0, 20));

        // 2. Markdown Section
        HBox mdHeader = createSectionHeader("Markdown");

        CheckBox foldMdFrontMatterCheck = new CheckBox("Collapse front matter");
        foldMdFrontMatterCheck.setSelected(settings.isFoldMarkdownFrontMatter());
        foldMdFrontMatterCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldMarkdownFrontMatter", foldMdFrontMatterCheck);

        CheckBox foldMdLinksCheck = new CheckBox("Collapse links");
        foldMdLinksCheck.setSelected(settings.isFoldMarkdownLinks());
        foldMdLinksCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldMarkdownLinks", foldMdLinksCheck);

        CheckBox foldMdTablesCheck = new CheckBox("Collapse tables");
        foldMdTablesCheck.setSelected(settings.isFoldMarkdownTables());
        foldMdTablesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldMarkdownTables", foldMdTablesCheck);

        CheckBox foldMdCodeFencesCheck = new CheckBox("Collapse code fences");
        foldMdCodeFencesCheck.setSelected(settings.isFoldMarkdownCodeFences());
        foldMdCodeFencesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldMarkdownCodeFences", foldMdCodeFencesCheck);

        CheckBox foldMdTocCheck = new CheckBox("Collapse table of contents");
        foldMdTocCheck.setSelected(settings.isFoldMarkdownTableOfContents());
        foldMdTocCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldMarkdownTableOfContents", foldMdTocCheck);

        VBox mdBox = new VBox(6, foldMdFrontMatterCheck, foldMdLinksCheck, foldMdTablesCheck, foldMdCodeFencesCheck, foldMdTocCheck);
        mdBox.setPadding(new Insets(0, 0, 0, 20));

        // 3. SQL Section
        HBox sqlHeader = createSectionHeader("SQL");

        CheckBox foldSqlUnderscoresCheck = new CheckBox("Put underscores inside numeric literals (6-digit or longer)");
        foldSqlUnderscoresCheck.setSelected(settings.isFoldSqlUnderscoresInNumericLiterals());
        foldSqlUnderscoresCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldSqlUnderscores", foldSqlUnderscoresCheck);

        VBox sqlBox = new VBox(6, foldSqlUnderscoresCheck);
        sqlBox.setPadding(new Insets(0, 0, 0, 20));

        // 4. XML Section
        HBox xmlHeader = createSectionHeader("XML");

        CheckBox foldXmlTagsCheck = new CheckBox("XML tags");
        foldXmlTagsCheck.setSelected(settings.isFoldXmlTags());
        foldXmlTagsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldXmlTags", foldXmlTagsCheck);

        CheckBox foldHtmlStyleCheck = new CheckBox("HTML 'style' attribute");
        foldHtmlStyleCheck.setSelected(settings.isFoldHtmlStyleAttribute());
        foldHtmlStyleCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldHtmlStyle", foldHtmlStyleCheck);

        CheckBox foldXmlEntitiesCheck = new CheckBox("XML entities");
        foldXmlEntitiesCheck.setSelected(settings.isFoldXmlEntities());
        foldXmlEntitiesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldXmlEntities", foldXmlEntitiesCheck);

        CheckBox foldDataUrisCheck = new CheckBox("Data URIs");
        foldDataUrisCheck.setSelected(settings.isFoldDataUris());
        foldDataUrisCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeFolding_foldDataUris", foldDataUrisCheck);

        VBox xmlBox = new VBox(6, foldXmlTagsCheck, foldHtmlStyleCheck, foldXmlEntitiesCheck, foldDataUrisCheck);
        xmlBox.setPadding(new Insets(0, 0, 0, 20));

        panel.getChildren().addAll(
                topRow,
                showBottomArrowsCheck,
                foldByDefaultLabel,
                genHeader,
                genBox,
                mdHeader,
                mdBox,
                sqlHeader,
                sqlBox,
                xmlHeader,
                xmlBox
        );
        return panel;
    }

    private static VBox buildEditorTabsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 20, 16));

        // -------------------------------------------------------------
        // Appearance Section
        // -------------------------------------------------------------
        HBox appHeader = createSectionHeader("Appearance");

        Label placementLabel = new Label("Tab placement:");
        placementLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        placementLabel.setMinWidth(130);

        ComboBox<String> placementCombo = new ComboBox<>();
        placementCombo.getItems().addAll("Top", "Left", "Bottom", "Right", "None");
        placementCombo.getSelectionModel().select(settings.getEditorTabPlacement());
        placementCombo.setPrefWidth(120);
        placementCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("editorTabs_placement", placementCombo);

        HBox placementRow = new HBox(12, placementLabel, placementCombo);
        placementRow.setAlignment(Pos.CENTER_LEFT);

        Label showTabsLabel = new Label("Show tabs in:");
        showTabsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ToggleGroup showModeGroup = new ToggleGroup();
        RadioButton oneRowRadio = new RadioButton("One row, and if tabs don't fit:");
        oneRowRadio.setToggleGroup(showModeGroup);
        oneRowRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ToggleGroup overflowGroup = new ToggleGroup();
        RadioButton scrollRadio = new RadioButton("Scroll the tabs panel");
        scrollRadio.setToggleGroup(overflowGroup);
        scrollRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton squeezeRadio = new RadioButton("Squeeze tabs");
        squeezeRadio.setToggleGroup(overflowGroup);
        squeezeRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if ("Squeeze tabs".equalsIgnoreCase(settings.getEditorTabsOverflowMode())) {
            squeezeRadio.setSelected(true);
        } else {
            scrollRadio.setSelected(true);
        }

        VBox overflowBox = new VBox(6, scrollRadio, squeezeRadio);
        overflowBox.setPadding(new Insets(0, 0, 0, 24));
        overflowBox.disableProperty().bind(oneRowRadio.selectedProperty().not());

        RadioButton multiRowRadio = new RadioButton("Multiple rows");
        multiRowRadio.setToggleGroup(showModeGroup);
        multiRowRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if ("Multiple rows".equalsIgnoreCase(settings.getEditorTabsShowMode())) {
            multiRowRadio.setSelected(true);
        } else {
            oneRowRadio.setSelected(true);
        }

        inputs.put("editorTabs_oneRowRadio", oneRowRadio);
        inputs.put("editorTabs_multiRowRadio", multiRowRadio);
        inputs.put("editorTabs_scrollRadio", scrollRadio);
        inputs.put("editorTabs_squeezeRadio", squeezeRadio);

        VBox showTabsSubBox = new VBox(6, oneRowRadio, overflowBox, multiRowRadio);
        showTabsSubBox.setPadding(new Insets(0, 0, 0, 20));
        VBox showTabsBox = new VBox(6, showTabsLabel, showTabsSubBox);

        CheckBox showPinnedTabsCheck = new CheckBox("Show pinned tabs in a separate row");
        showPinnedTabsCheck.setSelected(settings.isShowPinnedTabsInSeparateRow());
        showPinnedTabsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_showPinnedTabs", showPinnedTabsCheck);

        CheckBox showFileIconCheck = new CheckBox("Show file icon");
        showFileIconCheck.setSelected(settings.isEditorTabsShowFileIcon());
        showFileIconCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_showFileIcon", showFileIconCheck);

        CheckBox showFileExtCheck = new CheckBox("Show file extension");
        showFileExtCheck.setSelected(settings.isEditorTabsShowFileExtension());
        showFileExtCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_showFileExtension", showFileExtCheck);

        CheckBox showDirNonUniqueCheck = new CheckBox("Show directory for non-unique file names");
        showDirNonUniqueCheck.setSelected(settings.isEditorTabsShowDirectoryForNonUnique());
        showDirNonUniqueCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_showDirNonUnique", showDirNonUniqueCheck);

        CheckBox markModifiedCheck = new CheckBox("Mark modified");
        markModifiedCheck.setSelected(settings.isEditorTabsMarkModified());
        markModifiedCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_markModified", markModifiedCheck);

        CheckBox showFullPathHoverCheck = new CheckBox("Show full path on mouse hover");
        showFullPathHoverCheck.setSelected(settings.isEditorTabsShowFullPathOnHover());
        showFullPathHoverCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_showFullPathHover", showFullPathHoverCheck);

        Label closePosLabel = new Label("Close button position:");
        closePosLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        closePosLabel.setMinWidth(150);

        ComboBox<String> closePosCombo = new ComboBox<>();
        closePosCombo.getItems().addAll("Right", "Left", "None");
        closePosCombo.getSelectionModel().select(settings.getEditorTabsCloseButtonPosition());
        closePosCombo.setPrefWidth(100);
        closePosCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("editorTabs_closeButtonPosition", closePosCombo);

        HBox closePosRow = new HBox(12, closePosLabel, closePosCombo);
        closePosRow.setAlignment(Pos.CENTER_LEFT);

        // -------------------------------------------------------------
        // Tab Order Section
        // -------------------------------------------------------------
        HBox orderHeader = createSectionHeader("Tab Order");

        CheckBox sortAlphabeticalCheck = new CheckBox("Sort tabs alphabetically");
        sortAlphabeticalCheck.setSelected(settings.isEditorTabsSortAlphabetically());
        sortAlphabeticalCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_sortAlphabetically", sortAlphabeticalCheck);

        CheckBox openNewAtEndCheck = new CheckBox("Open new tabs at the end");
        openNewAtEndCheck.setSelected(settings.isEditorTabsOpenNewAtEnd());
        openNewAtEndCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_openNewAtEnd", openNewAtEndCheck);

        VBox orderBox = new VBox(6, sortAlphabeticalCheck, openNewAtEndCheck);
        orderBox.setPadding(new Insets(0, 0, 0, 20));

        // -------------------------------------------------------------
        // Opening Policy Section
        // -------------------------------------------------------------
        HBox openPolicyHeader = createSectionHeader("Opening Policy");

        CheckBox enablePreviewCheck = new CheckBox("Enable preview tab");
        enablePreviewCheck.setSelected(settings.isEditorTabsEnablePreviewTab());
        enablePreviewCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_enablePreviewTab", enablePreviewCheck);

        Label previewSubtext = new Label("The preview tab is reused to show files selected with a single click in the Project tool window, and files opened during debugging.");
        previewSubtext.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        previewSubtext.setWrapText(true);
        previewSubtext.setPadding(new Insets(0, 0, 0, 24));

        VBox openPolicyBox = new VBox(4, enablePreviewCheck, previewSubtext);
        openPolicyBox.setPadding(new Insets(0, 0, 0, 20));

        // -------------------------------------------------------------
        // Closing Policy Section
        // -------------------------------------------------------------
        HBox closePolicyHeader = createSectionHeader("Closing Policy");

        Label limitLabel = new Label("Tab limit:");
        limitLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        limitLabel.setMinWidth(70);

        TextField limitField = new TextField(String.valueOf(settings.getEditorTabsLimit()));
        limitField.setPrefWidth(55);
        limitField.setStyle("-fx-font-size: 12px;");
        inputs.put("editorTabs_limit", limitField);

        HBox limitRow = new HBox(8, limitLabel, limitField);
        limitRow.setAlignment(Pos.CENTER_LEFT);

        Label exceedLabel = new Label("When tabs exceed the limit:");
        exceedLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ToggleGroup exceedGroup = new ToggleGroup();
        RadioButton closeUnchangedRadio = new RadioButton("Close unchanged");
        closeUnchangedRadio.setToggleGroup(exceedGroup);
        closeUnchangedRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton closeUnusedRadio = new RadioButton("Close unused");
        closeUnusedRadio.setToggleGroup(exceedGroup);
        closeUnusedRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if ("Close unchanged".equalsIgnoreCase(settings.getEditorTabsExceedLimitPolicy())) {
            closeUnchangedRadio.setSelected(true);
        } else {
            closeUnusedRadio.setSelected(true);
        }
        inputs.put("editorTabs_closeUnchangedRadio", closeUnchangedRadio);
        inputs.put("editorTabs_closeUnusedRadio", closeUnusedRadio);

        VBox exceedRadioBox = new VBox(6, closeUnchangedRadio, closeUnusedRadio);
        exceedRadioBox.setPadding(new Insets(0, 0, 0, 20));
        VBox exceedBox = new VBox(6, exceedLabel, exceedRadioBox);

        Label activateLabel = new Label("When the current tab is closed, activate:");
        activateLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ToggleGroup activateGroup = new ToggleGroup();
        RadioButton activateLeftRadio = new RadioButton("The tab on the left");
        activateLeftRadio.setToggleGroup(activateGroup);
        activateLeftRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton activateRightRadio = new RadioButton("The tab on the right");
        activateRightRadio.setToggleGroup(activateGroup);
        activateRightRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton activateRecentRadio = new RadioButton("Most recently opened tab");
        activateRecentRadio.setToggleGroup(activateGroup);
        activateRecentRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        String currentActivate = settings.getEditorTabsCloseActivatePolicy();
        if ("The tab on the right".equalsIgnoreCase(currentActivate)) {
            activateRightRadio.setSelected(true);
        } else if ("Most recently opened tab".equalsIgnoreCase(currentActivate)) {
            activateRecentRadio.setSelected(true);
        } else {
            activateLeftRadio.setSelected(true);
        }
        inputs.put("editorTabs_activateLeftRadio", activateLeftRadio);
        inputs.put("editorTabs_activateRightRadio", activateRightRadio);
        inputs.put("editorTabs_activateRecentRadio", activateRecentRadio);

        VBox activateRadioBox = new VBox(6, activateLeftRadio, activateRightRadio, activateRecentRadio);
        activateRadioBox.setPadding(new Insets(0, 0, 0, 20));
        VBox activateBox = new VBox(6, activateLabel, activateRadioBox);

        VBox closePolicyBox = new VBox(10, limitRow, exceedBox, activateBox);
        closePolicyBox.setPadding(new Insets(0, 0, 0, 20));

        // -------------------------------------------------------------
        // Database Section
        // -------------------------------------------------------------
        HBox dbHeader = createSectionHeader("Database");

        CheckBox alwaysQualifiedCheck = new CheckBox("Always show qualified names for database objects in tab titles");
        alwaysQualifiedCheck.setSelected(settings.isEditorTabsAlwaysShowQualifiedNames());
        alwaysQualifiedCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_alwaysQualified", alwaysQualifiedCheck);

        CheckBox shortenNamesCheck = new CheckBox("Shorten datasource and object names in tab titles");
        shortenNamesCheck.setSelected(settings.isEditorTabsShortenNames());
        shortenNamesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editorTabs_shortenNames", shortenNamesCheck);

        VBox dbBox = new VBox(6, alwaysQualifiedCheck, shortenNamesCheck);
        dbBox.setPadding(new Insets(0, 0, 0, 20));

        // Assemble panel
        panel.getChildren().addAll(
                appHeader,
                placementRow,
                showTabsBox,
                showPinnedTabsCheck,
                showFileIconCheck,
                showFileExtCheck,
                showDirNonUniqueCheck,
                markModifiedCheck,
                showFullPathHoverCheck,
                closePosRow,
                orderHeader,
                orderBox,
                openPolicyHeader,
                openPolicyBox,
                closePolicyHeader,
                closePolicyBox,
                dbHeader,
                dbBox
        );
        return panel;
    }

    private static VBox buildGutterIconsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 20, 16));

        CheckBox showGutterCheck = new CheckBox("Show gutter icons");
        showGutterCheck.setSelected(settings.isShowGutterIcons());
        showGutterCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");
        inputs.put("gutterIcons_showGutterIcons", showGutterCheck);

        VBox contentBox = new VBox(10);
        contentBox.disableProperty().bind(showGutterCheck.selectedProperty().not());

        // 1. Common
        HBox commonHeader = createSectionHeader("Common");

        CheckBox colorPreviewCheck = new CheckBox("Color preview");
        colorPreviewCheck.setSelected(settings.isGutterColorPreview());
        colorPreviewCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("gutterIcons_colorPreview", colorPreviewCheck);
        HBox colorPreviewRow = createGutterIconRow(Icons.of(FontAwesomeSolid.PALETTE, "#57965c", 12), colorPreviewCheck);

        CheckBox docCommentsCheck = new CheckBox("Documentation comments in-place rendering");
        docCommentsCheck.setSelected(settings.isGutterDocComments());
        docCommentsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("gutterIcons_docComments", docCommentsCheck);
        HBox docCommentsRow = createGutterIconRow(Icons.of(FontAwesomeSolid.ALIGN_LEFT, "#808080", 12), docCommentsCheck);

        CheckBox runMarkerCheck = new CheckBox("Run line marker");
        runMarkerCheck.setSelected(settings.isGutterRunLineMarker());
        runMarkerCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("gutterIcons_runLineMarker", runMarkerCheck);
        HBox runMarkerRow = createGutterIconRow(Icons.of(FontAwesomeSolid.PLAY, "#499c54", 12), runMarkerCheck);

        VBox commonBox = new VBox(6, colorPreviewRow, docCommentsRow, runMarkerRow);
        commonBox.setPadding(new Insets(0, 0, 0, 16));

        // 2. Database Tools and SQL
        HBox dbHeader = createSectionHeader("Database Tools and SQL");

        CheckBox recursiveCallCheck = new CheckBox("Recursive call");
        recursiveCallCheck.setSelected(settings.isGutterRecursiveCall());
        recursiveCallCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("gutterIcons_recursiveCall", recursiveCallCheck);
        HBox recursiveCallRow = createGutterIconRow(Icons.of(FontAwesomeSolid.REDO, "#6897bb", 12), recursiveCallCheck);

        VBox dbBox = new VBox(6, recursiveCallRow);
        dbBox.setPadding(new Insets(0, 0, 0, 16));

        // 3. Git
        HBox gitHeader = createSectionHeader("Git");

        CheckBox vcsIgnoredCheck = new CheckBox("Version control ignored directories");
        vcsIgnoredCheck.setSelected(settings.isGutterVcsIgnoredDirectories());
        vcsIgnoredCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("gutterIcons_vcsIgnoredDirectories", vcsIgnoredCheck);
        HBox vcsIgnoredRow = createGutterIconRow(Icons.of(FontAwesomeSolid.FOLDER, "#808080", 12), vcsIgnoredCheck);

        VBox gitBox = new VBox(6, vcsIgnoredRow);
        gitBox.setPadding(new Insets(0, 0, 0, 16));

        // 4. Markdown
        HBox mdHeader = createSectionHeader("Markdown");

        CheckBox htmlImgCheck = new CheckBox("Configure HTML image");
        htmlImgCheck.setSelected(settings.isGutterConfigureHtmlImage());
        htmlImgCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("gutterIcons_configureHtmlImage", htmlImgCheck);

        CheckBox mdImgCheck = new CheckBox("Configure Markdown image");
        mdImgCheck.setSelected(settings.isGutterConfigureMarkdownImage());
        mdImgCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("gutterIcons_configureMarkdownImage", mdImgCheck);

        CheckBox plantUmlCheck = new CheckBox("Install PlantUML");
        plantUmlCheck.setSelected(settings.isGutterInstallPlantUml());
        plantUmlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("gutterIcons_installPlantUml", plantUmlCheck);

        VBox mdBox = new VBox(6, htmlImgCheck, mdImgCheck, plantUmlCheck);
        mdBox.setPadding(new Insets(0, 0, 0, 36));

        contentBox.getChildren().addAll(
                commonHeader,
                commonBox,
                dbHeader,
                dbBox,
                gitHeader,
                gitBox,
                mdHeader,
                mdBox
        );

        panel.getChildren().addAll(showGutterCheck, contentBox);
        return panel;
    }

    private static HBox createGutterIconRow(Node icon, CheckBox checkBox) {
        StackPane iconPane = new StackPane(icon);
        iconPane.setMinWidth(20);
        iconPane.setAlignment(Pos.CENTER);
        HBox row = new HBox(8, iconPane, checkBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static VBox buildInlineCompletionPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs, java.util.function.Consumer<String> navigateTo) {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(10, 16, 20, 16));

        // Top link
        Label goLabel = new Label("Go to ");
        goLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        Hyperlink ccLink = new Hyperlink("Code Completion settings page");
        ccLink.setStyle("-fx-font-size: 13px; -fx-text-fill: #589df6; -fx-underline: true; -fx-padding: 0;");
        ccLink.setOnAction(e -> {
            if (navigateTo != null) navigateTo.accept("Editor / General / Code Completion");
        });

        Label adjustLabel = new Label(" to adjust lookup completion settings");
        adjustLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        HBox topLinkRow = new HBox(goLabel, ccLink, adjustLabel);
        topLinkRow.setAlignment(Pos.CENTER_LEFT);

        // 1. Enable local Full Line completion suggestions
        CheckBox fullLineCheck = new CheckBox("Enable local Full Line completion suggestions");
        fullLineCheck.setSelected(settings.isInlineCompletionEnabled());
        fullLineCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("inlineCompletion_enabled", fullLineCheck);

        Label fullLineSub1 = new Label("Runs entirely on your local device without sending anything over the internet");
        fullLineSub1.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        fullLineSub1.setWrapText(true);

        Label fullLineSub2 = new Label("Currently, Full Line is available only for Python, JS and TS languages, please install one of plugins or use PyCharm and WebStorm");
        fullLineSub2.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        fullLineSub2.setWrapText(true);

        VBox fullLineSubBox = new VBox(2, fullLineSub1, fullLineSub2);
        fullLineSubBox.setPadding(new Insets(0, 0, 0, 24));
        VBox fullLineBox = new VBox(4, fullLineCheck, fullLineSubBox);

        // 2. Enable automatic completion on typing
        CheckBox autoTypingCheck = new CheckBox("Enable automatic completion on typing");
        autoTypingCheck.setSelected(settings.isInlineAutoOnTyping());
        autoTypingCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("inlineCompletion_autoOnTyping", autoTypingCheck);

        Label autoTypingSub = new Label("If disabled, completion suggestions can still be invoked via ⌥⇧\\ shortcut");
        autoTypingSub.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        autoTypingSub.setWrapText(true);
        autoTypingSub.setPadding(new Insets(0, 0, 0, 24));

        VBox autoTypingBox = new VBox(4, autoTypingCheck, autoTypingSub);

        // 3. Enable multi-line suggestions
        CheckBox multilineCheck = new CheckBox("Enable multi-line suggestions");
        multilineCheck.setSelected(settings.isInlineMultilineSuggestions());
        multilineCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("inlineCompletion_multiline", multilineCheck);

        Label multilineSub = new Label("If disabled, only single-line suggestions will be shown");
        multilineSub.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        multilineSub.setWrapText(true);
        multilineSub.setPadding(new Insets(0, 0, 0, 24));

        VBox multilineBox = new VBox(4, multilineCheck, multilineSub);

        // 4. Synchronize inline and popup completions
        CheckBox syncPopupCheck = new CheckBox("Synchronize inline and popup completions");
        syncPopupCheck.setSelected(settings.isInlineSyncWithPopup());
        syncPopupCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("inlineCompletion_syncWithPopup", syncPopupCheck);

        Label syncPopupSub = new Label("When enabled, inline completions will be shown in the popup completion list to avoid shortcut conflicts");
        syncPopupSub.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        syncPopupSub.setWrapText(true);
        syncPopupSub.setPadding(new Insets(0, 0, 0, 24));

        VBox syncPopupBox = new VBox(4, syncPopupCheck, syncPopupSub);

        panel.getChildren().addAll(
                topLinkRow,
                fullLineBox,
                autoTypingBox,
                multilineBox,
                syncPopupBox
        );
        return panel;
    }

    private static VBox buildOutputConsolePanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(10, 16, 20, 16));

        // 1. Use soft wraps in console
        CheckBox softWrapsCheck = new CheckBox("Use soft wraps in console");
        softWrapsCheck.setSelected(settings.isOutputConsoleUseSoftWraps());
        softWrapsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("outputConsole_useSoftWraps", softWrapsCheck);

        // 2. Console commands history size: [ 300 ]
        Label historyLabel = new Label("Console commands history size:");
        historyLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        historyLabel.setMinWidth(210);

        TextField historyField = new TextField(String.valueOf(settings.getOutputConsoleHistorySize()));
        historyField.setPrefWidth(60);
        historyField.setStyle("-fx-font-size: 12px;");
        inputs.put("outputConsole_historySize", historyField);

        HBox historyRow = new HBox(8, historyLabel, historyField);
        historyRow.setAlignment(Pos.CENTER_LEFT);

        // 3. Override console cycle buffer size (1024 KB) [ 1024 ] KB
        CheckBox overrideCycleCheck = new CheckBox("Override console cycle buffer size (1024 KB)");
        overrideCycleCheck.setSelected(settings.isOutputConsoleOverrideCycleBuffer());
        overrideCycleCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("outputConsole_overrideCycleBuffer", overrideCycleCheck);

        TextField cycleBufferField = new TextField(String.valueOf(settings.getOutputConsoleCycleBufferSizeKb()));
        cycleBufferField.setPrefWidth(60);
        cycleBufferField.setStyle("-fx-font-size: 12px;");
        cycleBufferField.disableProperty().bind(overrideCycleCheck.selectedProperty().not());
        inputs.put("outputConsole_cycleBufferSizeKb", cycleBufferField);

        Label kbLabel = new Label("KB");
        kbLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        kbLabel.disableProperty().bind(overrideCycleCheck.selectedProperty().not());

        HBox cycleBufferRow = new HBox(8, overrideCycleCheck, cycleBufferField, kbLabel);
        cycleBufferRow.setAlignment(Pos.CENTER_LEFT);

        // 4. Default Encoding: <System Default: UTF-8>
        Label encLabel = new Label("Default Encoding:");
        encLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        encLabel.setMinWidth(115);

        ComboBox<String> encCombo = new ComboBox<>();
        encCombo.getItems().addAll(
                "<System Default: UTF-8>",
                "ISO-8859-1",
                "UTF-8",
                "UTF-16",
                "US-ASCII",
                "Big5",
                "Big5-HKSCS",
                "CESU-8",
                "EUC-JP",
                "EUC-KR",
                "GB18030"
        );
        encCombo.getSelectionModel().select(settings.getOutputConsoleDefaultEncoding());
        encCombo.setPrefWidth(200);
        encCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("outputConsole_defaultEncoding", encCombo);

        HBox encRow = new HBox(8, encLabel, encCombo);
        encRow.setAlignment(Pos.CENTER_LEFT);

        // 5. Fold console lines that contain:
        Label foldHeader = new Label("Fold console lines that contain:");
        foldHeader.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 6 0 0 0;");

        ObservableList<String> foldingPatterns = FXCollections.observableArrayList(settings.getOutputConsoleFoldingPatterns());
        inputs.put("outputConsole_foldingPatterns", foldingPatterns);

        ListView<String> foldingList = new ListView<>(foldingPatterns);
        foldingList.setPrefHeight(130);
        foldingList.setStyle("-fx-font-size: 12px;");

        Label foldPlaceholder = new Label("Fold nothing");
        foldPlaceholder.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        foldingList.setPlaceholder(foldPlaceholder);

        Button addFoldBtn = new Button("+");
        addFoldBtn.setPrefWidth(28);
        addFoldBtn.setStyle("-fx-font-size: 12px;");

        Button removeFoldBtn = new Button("—");
        removeFoldBtn.setPrefWidth(28);
        removeFoldBtn.setStyle("-fx-font-size: 12px;");
        removeFoldBtn.disableProperty().bind(foldingList.getSelectionModel().selectedItemProperty().isNull());

        Button editFoldBtn = new Button("✎");
        editFoldBtn.setPrefWidth(28);
        editFoldBtn.setStyle("-fx-font-size: 12px;");
        editFoldBtn.disableProperty().bind(foldingList.getSelectionModel().selectedItemProperty().isNull());

        addFoldBtn.setOnAction(e -> {
            showConsolePatternDialog("Folding Pattern",
                    "Enter a substring of a console line you'd like to see folded:",
                    "",
                    res -> {
                        foldingPatterns.add(res);
                        foldingList.getSelectionModel().select(res);
                    });
        });

        editFoldBtn.setOnAction(e -> {
            String selected = foldingList.getSelectionModel().getSelectedItem();
            int idx = foldingList.getSelectionModel().getSelectedIndex();
            if (selected != null && idx >= 0) {
                showConsolePatternDialog("Folding Pattern",
                        "Enter a substring of a console line you'd like to see folded:",
                        selected,
                        res -> foldingPatterns.set(idx, res));
            }
        });

        removeFoldBtn.setOnAction(e -> {
            String selected = foldingList.getSelectionModel().getSelectedItem();
            if (selected != null) foldingPatterns.remove(selected);
        });

        HBox foldToolbar = new HBox(4, addFoldBtn, removeFoldBtn, editFoldBtn);
        foldToolbar.setAlignment(Pos.CENTER_LEFT);
        foldToolbar.setPadding(new Insets(2, 4, 4, 4));

        VBox foldBox = new VBox(2, foldToolbar, foldingList);
        foldBox.setStyle("-fx-border-color: -panel-border; -fx-border-width: 1px; -fx-border-radius: 4px;");

        // 6. Exceptions:
        Label exceptHeader = new Label("Exceptions:");
        exceptHeader.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 6 0 0 0;");

        ObservableList<String> foldingExceptions = FXCollections.observableArrayList(settings.getOutputConsoleFoldingExceptions());
        inputs.put("outputConsole_foldingExceptions", foldingExceptions);

        ListView<String> exceptList = new ListView<>(foldingExceptions);
        exceptList.setPrefHeight(130);
        exceptList.setStyle("-fx-font-size: 12px;");

        Label exceptPlaceholder = new Label("No exceptions");
        exceptPlaceholder.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        exceptList.setPlaceholder(exceptPlaceholder);

        Button addExceptBtn = new Button("+");
        addExceptBtn.setPrefWidth(28);
        addExceptBtn.setStyle("-fx-font-size: 12px;");

        Button removeExceptBtn = new Button("—");
        removeExceptBtn.setPrefWidth(28);
        removeExceptBtn.setStyle("-fx-font-size: 12px;");
        removeExceptBtn.disableProperty().bind(exceptList.getSelectionModel().selectedItemProperty().isNull());

        Button editExceptBtn = new Button("✎");
        editExceptBtn.setPrefWidth(28);
        editExceptBtn.setStyle("-fx-font-size: 12px;");
        editExceptBtn.disableProperty().bind(exceptList.getSelectionModel().selectedItemProperty().isNull());

        addExceptBtn.setOnAction(e -> {
            showConsolePatternDialog("Folding Pattern Exception",
                    "Enter a substring of a console line you'd like to exclude from folding:",
                    "",
                    res -> {
                        foldingExceptions.add(res);
                        exceptList.getSelectionModel().select(res);
                    });
        });

        editExceptBtn.setOnAction(e -> {
            String selected = exceptList.getSelectionModel().getSelectedItem();
            int idx = exceptList.getSelectionModel().getSelectedIndex();
            if (selected != null && idx >= 0) {
                showConsolePatternDialog("Folding Pattern Exception",
                        "Enter a substring of a console line you'd like to exclude from folding:",
                        selected,
                        res -> foldingExceptions.set(idx, res));
            }
        });

        removeExceptBtn.setOnAction(e -> {
            String selected = exceptList.getSelectionModel().getSelectedItem();
            if (selected != null) foldingExceptions.remove(selected);
        });

        HBox exceptToolbar = new HBox(4, addExceptBtn, removeExceptBtn, editExceptBtn);
        exceptToolbar.setAlignment(Pos.CENTER_LEFT);
        exceptToolbar.setPadding(new Insets(2, 4, 4, 4));

        VBox exceptBox = new VBox(2, exceptToolbar, exceptList);
        exceptBox.setStyle("-fx-border-color: -panel-border; -fx-border-width: 1px; -fx-border-radius: 4px;");

        panel.getChildren().addAll(
                softWrapsCheck,
                historyRow,
                cycleBufferRow,
                encRow,
                foldHeader,
                foldBox,
                exceptHeader,
                exceptBox
        );
        return panel;
    }

    private static void showConsolePatternDialog(String title, String prompt, String initialValue, java.util.function.Consumer<String> onConfirm) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setPrefWidth(420);

        HBox promptRow = new HBox(10);
        promptRow.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("?");
        icon.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #3592c4; -fx-background-color: rgba(53, 146, 196, 0.2); -fx-background-radius: 12px; -fx-padding: 2 7 2 7;");
        Label promptLabel = new Label(prompt);
        promptLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        promptLabel.setWrapText(true);
        promptRow.getChildren().addAll(icon, promptLabel);

        TextField field = new TextField(initialValue != null ? initialValue : "");
        field.setStyle("-fx-font-size: 13px;");

        content.getChildren().addAll(promptRow, field);
        dialog.getDialogPane().setContent(content);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, okButtonType);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return field.getText().trim();
            }
            return null;
        });

        Platform.runLater(field::requestFocus);

        dialog.showAndWait().ifPresent(result -> {
            if (!result.isEmpty()) {
                onConfirm.accept(result);
            }
        });
    }

    private static VBox buildStickyLinesPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs,
                                              java.util.function.Consumer<String> navigateTo) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(10, 16, 16, 16));

        CheckBox showStickyLinesCheck = new CheckBox("Show sticky lines while scrolling");
        showStickyLinesCheck.setSelected(settings.isStickyLinesEnabled());
        showStickyLinesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("stickyLines_enabled", showStickyLinesCheck);

        VBox indented = new VBox(12);
        indented.setPadding(new Insets(4, 0, 8, 20));
        indented.disableProperty().bind(showStickyLinesCheck.selectedProperty().not());

        // Maximum number of lines
        Label maxLinesLabel = new Label("Maximum number of lines:");
        maxLinesLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        maxLinesLabel.setMinWidth(160);

        Spinner<Integer> maxLinesSpinner = new Spinner<>(1, 20, settings.getStickyLinesMaxLines(), 1);
        maxLinesSpinner.setEditable(true);
        maxLinesSpinner.setPrefWidth(65);
        maxLinesSpinner.setStyle("-fx-font-size: 12px;");
        inputs.put("stickyLines_maxLines", maxLinesSpinner);

        HBox maxLinesRow = new HBox(8, maxLinesLabel, maxLinesSpinner);
        maxLinesRow.setAlignment(Pos.CENTER_LEFT);

        // Languages
        Label languagesLabel = new Label("Languages:");
        languagesLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        GridPane langGrid = new GridPane();
        langGrid.setHgap(32);
        langGrid.setVgap(8);

        CheckBox htmlCheck = new CheckBox("HTML");
        htmlCheck.setSelected(settings.isStickyLinesHtml());
        htmlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("stickyLines_html", htmlCheck);

        CheckBox mdCheck = new CheckBox("Markdown");
        mdCheck.setSelected(settings.isStickyLinesMarkdown());
        mdCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("stickyLines_markdown", mdCheck);

        CheckBox xhtmlCheck = new CheckBox("XHTML");
        xhtmlCheck.setSelected(settings.isStickyLinesXhtml());
        xhtmlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("stickyLines_xhtml", xhtmlCheck);

        CheckBox jsonCheck = new CheckBox("JSON");
        jsonCheck.setSelected(settings.isStickyLinesJson());
        jsonCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("stickyLines_json", jsonCheck);

        CheckBox sqlCheck = new CheckBox("SQL");
        sqlCheck.setSelected(settings.isStickyLinesSql());
        sqlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("stickyLines_sql", sqlCheck);

        CheckBox xmlCheck = new CheckBox("XML");
        xmlCheck.setSelected(settings.isStickyLinesXml());
        xmlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("stickyLines_xml", xmlCheck);

        langGrid.add(htmlCheck, 0, 0);
        langGrid.add(mdCheck, 1, 0);
        langGrid.add(xhtmlCheck, 2, 0);
        langGrid.add(jsonCheck, 0, 1);
        langGrid.add(sqlCheck, 1, 1);
        langGrid.add(xmlCheck, 2, 1);

        // Manage colors hyperlink
        Hyperlink manageColorsLink = new Hyperlink("Manage colors");
        manageColorsLink.setStyle("-fx-text-fill: -accent; -fx-font-size: 13px; -fx-underline: true; -fx-padding: 4 0 0 0;");
        manageColorsLink.setOnAction(e -> {
            if (navigateTo != null) navigateTo.accept("Editor / Color Scheme / General");
        });

        indented.getChildren().addAll(maxLinesRow, languagesLabel, langGrid, manageColorsLink);
        panel.getChildren().addAll(showStickyLinesCheck, indented);
        return panel;
    }

    private static VBox buildCodeEditingPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10, 16, 16, 16));

        // 1. Highlight on Caret Movement
        HBox caretMoveHeader = createSectionHeader("Highlight on Caret Movement");

        CheckBox matchedBraceCheck = new CheckBox("Matched brace");
        matchedBraceCheck.setSelected(settings.isCodeEditingHighlightMatchedBrace());
        matchedBraceCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeEditing_highlightMatchedBrace", matchedBraceCheck);

        CheckBox currentScopeCheck = new CheckBox("Current scope");
        currentScopeCheck.setSelected(settings.isCodeEditingHighlightCurrentScope());
        currentScopeCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeEditing_highlightCurrentScope", currentScopeCheck);

        CheckBox usagesCheck = new CheckBox("Usages of element at caret");
        usagesCheck.setSelected(settings.isCodeEditingHighlightUsages());
        usagesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeEditing_highlightUsages", usagesCheck);

        VBox caretMoveBox = new VBox(8, matchedBraceCheck, currentScopeCheck, usagesCheck);
        caretMoveBox.setPadding(new Insets(2, 0, 8, 18));

        // 2. Quick Documentation
        HBox quickDocHeader = createSectionHeader("Quick Documentation");

        CheckBox showDocCheck = new CheckBox("Show quick documentation on hover");
        showDocCheck.setSelected(settings.isCodeEditingShowDocOnHover());
        showDocCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeEditing_showDocOnHover", showDocCheck);

        VBox quickDocBox = new VBox(8, showDocCheck);
        quickDocBox.setPadding(new Insets(2, 0, 8, 18));

        // 3. Refactorings
        HBox refactorHeader = createSectionHeader("Refactorings");

        Label specifyRefactorLabel = new Label("Specify refactoring options:");
        specifyRefactorLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ToggleGroup refactorGroup = new ToggleGroup();
        RadioButton inEditorRadio = new RadioButton("In the editor");
        inEditorRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inEditorRadio.setToggleGroup(refactorGroup);

        RadioButton inDialogsRadio = new RadioButton("In modal dialogs");
        inDialogsRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inDialogsRadio.setToggleGroup(refactorGroup);

        if ("In modal dialogs".equalsIgnoreCase(settings.getCodeEditingRefactoringOption())) {
            inDialogsRadio.setSelected(true);
        } else {
            inEditorRadio.setSelected(true);
        }
        inputs.put("codeEditing_refactoringOption", refactorGroup);

        VBox radioBox = new VBox(6, inEditorRadio, inDialogsRadio);
        radioBox.setPadding(new Insets(0, 0, 0, 16));

        CheckBox preselectSymbolCheck = new CheckBox("Preselect current symbol name for Rename refactoring");
        preselectSymbolCheck.setSelected(settings.isCodeEditingPreselectCurrentSymbol());
        preselectSymbolCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeEditing_preselectCurrentSymbol", preselectSymbolCheck);

        CheckBox showInlineDialogCheck = new CheckBox("Show inline dialog for local variables");
        showInlineDialogCheck.setSelected(settings.isCodeEditingShowInlineDialogForLocalVars());
        showInlineDialogCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("codeEditing_showInlineDialogForLocalVars", showInlineDialogCheck);

        VBox refactorBox = new VBox(8, specifyRefactorLabel, radioBox, preselectSymbolCheck, showInlineDialogCheck);
        refactorBox.setPadding(new Insets(2, 0, 8, 18));

        // 4. Error Highlighting
        HBox errorHighlightHeader = createSectionHeader("Error Highlighting");

        Label stripeHeightLabel = new Label("Error stripe mark min height:");
        stripeHeightLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        stripeHeightLabel.setMinWidth(180);

        Spinner<Integer> stripeSpinner = new Spinner<>(1, 100, settings.getCodeEditingErrorStripeMarkMinHeight(), 1);
        stripeSpinner.setEditable(true);
        stripeSpinner.setPrefWidth(60);
        stripeSpinner.setStyle("-fx-font-size: 12px;");
        inputs.put("codeEditing_errorStripeMarkMinHeight", stripeSpinner);

        Label pixelsLabel = new Label("pixels");
        pixelsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        HBox stripeRow = new HBox(8, stripeHeightLabel, stripeSpinner, pixelsLabel);
        stripeRow.setAlignment(Pos.CENTER_LEFT);

        Label autoreparseLabel = new Label("Autoreparse delay:");
        autoreparseLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        autoreparseLabel.setMinWidth(180);

        Spinner<Integer> autoreparseSpinner = new Spinner<>(0, 10000, settings.getCodeEditingAutoreparseDelayMs(), 50);
        autoreparseSpinner.setEditable(true);
        autoreparseSpinner.setPrefWidth(70);
        autoreparseSpinner.setStyle("-fx-font-size: 12px;");
        inputs.put("codeEditing_autoreparseDelayMs", autoreparseSpinner);

        Label msLabel = new Label("milliseconds");
        msLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        HBox autoreparseRow = new HBox(8, autoreparseLabel, autoreparseSpinner, msLabel);
        autoreparseRow.setAlignment(Pos.CENTER_LEFT);

        Label nextErrorLabel = new Label("The 'Next Error' action goes through:");
        nextErrorLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        nextErrorLabel.setMinWidth(240);

        ComboBox<String> nextErrorCombo = new ComboBox<>();
        nextErrorCombo.getItems().addAll(AppSettingsStore.Settings.defaultNextErrorActionOptions());
        nextErrorCombo.setValue(settings.getCodeEditingNextErrorAction());
        nextErrorCombo.setPrefWidth(260);
        nextErrorCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("codeEditing_nextErrorAction", nextErrorCombo);

        HBox nextErrorRow = new HBox(8, nextErrorLabel, nextErrorCombo);
        nextErrorRow.setAlignment(Pos.CENTER_LEFT);

        VBox errorHighlightBox = new VBox(8, stripeRow, autoreparseRow, nextErrorRow);
        errorHighlightBox.setPadding(new Insets(2, 0, 8, 18));

        // 5. Editor Tooltips
        HBox tooltipsHeader = createSectionHeader("Editor Tooltips");

        Label tooltipDelayLabel = new Label("Tooltip delay:");
        tooltipDelayLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        tooltipDelayLabel.setMinWidth(180);

        Spinner<Integer> tooltipDelaySpinner = new Spinner<>(0, 5000, settings.getCodeEditingTooltipDelayMs(), 50);
        tooltipDelaySpinner.setEditable(true);
        tooltipDelaySpinner.setPrefWidth(70);
        tooltipDelaySpinner.setStyle("-fx-font-size: 12px;");
        inputs.put("codeEditing_tooltipDelayMs", tooltipDelaySpinner);

        Label tooltipMsLabel = new Label("milliseconds");
        tooltipMsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        HBox tooltipDelayRow = new HBox(8, tooltipDelayLabel, tooltipDelaySpinner, tooltipMsLabel);
        tooltipDelayRow.setAlignment(Pos.CENTER_LEFT);

        VBox tooltipsBox = new VBox(8, tooltipDelayRow);
        tooltipsBox.setPadding(new Insets(2, 0, 8, 18));

        panel.getChildren().addAll(
                caretMoveHeader, caretMoveBox,
                quickDocHeader, quickDocBox,
                refactorHeader, refactorBox,
                errorHighlightHeader, errorHighlightBox,
                tooltipsHeader, tooltipsBox
        );
        return panel;
    }

    private static HBox buildFontPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs,
                                       java.util.function.Consumer<String> navigateTo) {
        HBox mainContainer = new HBox(20);
        mainContainer.setPadding(new Insets(10, 16, 16, 16));

        // --- Left Configuration Pane ---
        VBox leftPane = new VBox(12);
        leftPane.setMinWidth(410);
        leftPane.setPrefWidth(430);

        // Row 1: Font dropdown
        Label fontLabel = new Label("Font:");
        fontLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        fontLabel.setMinWidth(48);

        ComboBox<String> fontCombo = new ComboBox<>();
        // Monospaced fonts prioritized
        List<String> preferredFonts = List.of(
                "JetBrains Mono", "Fira Code", "Inconsolata", "Source Code Pro",
                "Menlo", "Monaco", "Courier New", "Droid Sans Mono", "Consolas"
        );
        List<String> installed = Font.getFamilies();
        for (String pf : preferredFonts) {
            if (installed.contains(pf) && !fontCombo.getItems().contains(pf)) {
                fontCombo.getItems().add(pf);
            }
        }
        for (String fam : installed) {
            if (!fontCombo.getItems().contains(fam)) {
                fontCombo.getItems().add(fam);
            }
        }
        if (!fontCombo.getItems().contains(settings.getEditorFontFamily())) {
            fontCombo.getItems().add(0, settings.getEditorFontFamily());
        }
        fontCombo.setValue(settings.getEditorFontFamily());
        fontCombo.setPrefWidth(260);
        fontCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("fontCombo", fontCombo);

        HBox fontRow = new HBox(8, fontLabel, fontCombo);
        fontRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Size & Line height
        Label sizeLabel = new Label("Size:");
        sizeLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        sizeLabel.setMinWidth(36);

        Spinner<Double> sizeSpinner = new Spinner<>(8.0, 48.0, settings.getEditorFontSize(), 0.5);
        sizeSpinner.setEditable(true);
        sizeSpinner.setPrefWidth(75);
        sizeSpinner.setStyle("-fx-font-size: 12px;");
        inputs.put("sizeSpinner", sizeSpinner);

        Label lineHeightLabel = new Label("Line height:");
        lineHeightLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        lineHeightLabel.setMinWidth(75);

        Spinner<Double> lineHeightSpinner = new Spinner<>(0.8, 3.0, settings.getEditorLineHeight(), 0.1);
        lineHeightSpinner.setEditable(true);
        lineHeightSpinner.setPrefWidth(65);
        lineHeightSpinner.setStyle("-fx-font-size: 12px;");
        inputs.put("editor_lineHeight", lineHeightSpinner);

        HBox sizeLineRow = new HBox(8, sizeLabel, sizeSpinner, lineHeightLabel, lineHeightSpinner);
        sizeLineRow.setAlignment(Pos.CENTER_LEFT);

        // Row 3: Enable ligatures
        CheckBox enableLigaturesCheck = new CheckBox("Enable ligatures");
        enableLigaturesCheck.setSelected(settings.isEditorEnableLigatures());
        enableLigaturesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("editor_enableLigatures", enableLigaturesCheck);

        Label infoIcon = new Label("?");
        infoIcon.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-border-color: -text-dim; -fx-border-radius: 10px; -fx-padding: 0 4 0 4;");
        Tooltip.install(infoIcon, new Tooltip("Enable typographic ligatures supported by the font (e.g. !=, >=, ->, =>)"));

        HBox ligaturesRow = new HBox(6, enableLigaturesCheck, infoIcon);
        ligaturesRow.setAlignment(Pos.CENTER_LEFT);

        // Row 4: Reader mode link
        Label readerModePre = new Label("See line height and ligatures also in ");
        readerModePre.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

        Hyperlink readerModeLink = new Hyperlink("Reader mode");
        readerModeLink.setStyle("-fx-text-fill: -accent; -fx-font-size: 12px; -fx-underline: true; -fx-padding: 0;");
        readerModeLink.setOnAction(e -> {
            if (navigateTo != null) navigateTo.accept("Editor / Reader Mode");
        });

        HBox readerModeRow = new HBox(readerModePre, readerModeLink);
        readerModeRow.setAlignment(Pos.CENTER_LEFT);

        // Typography Settings
        HBox typographyHeader = createSectionHeader("Typography Settings");

        Label mainWeightLabel = new Label("Main weight:");
        mainWeightLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        mainWeightLabel.setMinWidth(90);

        ComboBox<String> mainWeightCombo = new ComboBox<>();
        mainWeightCombo.getItems().addAll(AppSettingsStore.Settings.defaultFontWeights());
        mainWeightCombo.setValue(settings.getEditorFontMainWeight());
        mainWeightCombo.setPrefWidth(220);
        mainWeightCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("editor_fontMainWeight", mainWeightCombo);

        HBox mainWeightRow = new HBox(8, mainWeightLabel, mainWeightCombo);
        mainWeightRow.setAlignment(Pos.CENTER_LEFT);

        Label boldWeightLabel = new Label("Bold weight:");
        boldWeightLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        boldWeightLabel.setMinWidth(90);

        ComboBox<String> boldWeightCombo = new ComboBox<>();
        boldWeightCombo.getItems().addAll(AppSettingsStore.Settings.defaultFontBoldWeights());
        boldWeightCombo.setValue(settings.getEditorFontBoldWeight());
        boldWeightCombo.setPrefWidth(220);
        boldWeightCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("editor_fontBoldWeight", boldWeightCombo);

        HBox boldWeightRow = new HBox(8, boldWeightLabel, boldWeightCombo);
        boldWeightRow.setAlignment(Pos.CENTER_LEFT);

        Label boldHelperPre = new Label("Used for the bold settings in ");
        boldHelperPre.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        Hyperlink colorSchemeLink = new Hyperlink("color scheme");
        colorSchemeLink.setStyle("-fx-text-fill: -accent; -fx-font-size: 11px; -fx-underline: true; -fx-padding: 0;");
        colorSchemeLink.setOnAction(e -> {
            if (navigateTo != null) navigateTo.accept("Editor / Color Scheme / General");
        });

        HBox boldHelperRow = new HBox(boldHelperPre, colorSchemeLink);
        boldHelperRow.setPadding(new Insets(0, 0, 4, 98));

        Label fallbackLabel = new Label("Fallback font:");
        fallbackLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        fallbackLabel.setMinWidth(90);

        ComboBox<String> fallbackCombo = new ComboBox<>();
        fallbackCombo.getItems().add("<None>");
        for (String pf : preferredFonts) {
            if (installed.contains(pf) && !fallbackCombo.getItems().contains(pf)) {
                fallbackCombo.getItems().add(pf);
            }
        }
        fallbackCombo.setValue(settings.getEditorFallbackFont());
        fallbackCombo.setPrefWidth(220);
        fallbackCombo.setStyle("-fx-font-size: 12px;");
        inputs.put("editor_fallbackFont", fallbackCombo);

        HBox fallbackRow = new HBox(8, fallbackLabel, fallbackCombo);
        fallbackRow.setAlignment(Pos.CENTER_LEFT);

        Label fallbackHelper = new Label("Used for symbols not supported by the main font");
        fallbackHelper.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        fallbackHelper.setPadding(new Insets(0, 0, 0, 98));

        VBox typographyBox = new VBox(8,
                mainWeightRow,
                boldWeightRow,
                boldHelperRow,
                fallbackRow,
                fallbackHelper
        );
        typographyBox.setPadding(new Insets(2, 0, 8, 18));

        leftPane.getChildren().addAll(
                fontRow,
                sizeLineRow,
                ligaturesRow,
                readerModeRow,
                typographyHeader,
                typographyBox
        );

        // --- Right Preview Pane ---
        VBox rightPane = new VBox(8);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        String sampleText = "DataGrip is an Integrated\n"
                + "Development Environment (IDE) designed\n"
                + "to maximize productivity. It provides\n"
                + "clever code completion, static code\n"
                + "analysis, and refactorings, and lets\n"
                + "you focus on the bright side of\n"
                + "software development making\n"
                + "it an enjoyable experience.\n\n"
                + "Default:\n"
                + "abcdefghijklmnopqrstuvwxyz\n"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n"
                + "0123456789 (){}[]\n"
                + "+ - * / = . , ; : !? #&$%@|^\n\n"
                + "Bold:\n"
                + "abcdefghijklmnopqrstuvwxyz\n"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n"
                + "0123456789 (){}[]\n"
                + "+ - * / = . , ; : !? #&$%@|^\n\n"
                + "<!-- -- != := === >= >- >=> |-> -> <$>\n"
                + "</> #[ |||> |= ~@\n";

        TextArea previewArea = new TextArea(sampleText);
        previewArea.setEditable(false);
        previewArea.setWrapText(false);
        VBox.setVgrow(previewArea, Priority.ALWAYS);

        TextField customInput = new TextField();
        customInput.setPromptText("Enter any text to preview");
        customInput.setStyle("-fx-font-size: 12px;");

        Runnable refreshPreview = () -> {
            String fam = fontCombo.getValue() != null ? fontCombo.getValue() : "JetBrains Mono";
            Double sz = sizeSpinner.getValue() != null ? sizeSpinner.getValue() : 13.0;
            Double lh = lineHeightSpinner.getValue() != null ? lineHeightSpinner.getValue() : 1.2;
            String wt = mainWeightCombo.getValue() != null ? mainWeightCombo.getValue() : "Regular";
            double spacingPx = Math.max(0, (lh - 1.0) * sz);

            String fxWeight = "normal";
            if ("Bold".equalsIgnoreCase(wt) || "ExtraBold".equalsIgnoreCase(wt) || "Bold Recommended".equalsIgnoreCase(wt)) {
                fxWeight = "bold";
            } else if ("Light".equalsIgnoreCase(wt) || "ExtraLight".equalsIgnoreCase(wt) || "Thin".equalsIgnoreCase(wt)) {
                fxWeight = "100";
            }

            previewArea.setStyle(
                    "-fx-font-family: '" + fam + "'; "
                            + "-fx-font-size: " + sz + "px; "
                            + "-fx-line-spacing: " + spacingPx + "px; "
                            + "-fx-font-weight: " + fxWeight + "; "
                            + "-fx-control-inner-background: #1e1f22; "
                            + "-fx-text-fill: -text;"
            );
            customInput.setStyle("-fx-font-family: '" + fam + "'; -fx-font-size: " + sz + "px;");
        };

        fontCombo.valueProperty().addListener((o, a, b) -> refreshPreview.run());
        sizeSpinner.valueProperty().addListener((o, a, b) -> refreshPreview.run());
        lineHeightSpinner.valueProperty().addListener((o, a, b) -> refreshPreview.run());
        mainWeightCombo.valueProperty().addListener((o, a, b) -> refreshPreview.run());

        customInput.textProperty().addListener((o, a, b) -> {
            if (b != null && !b.isEmpty()) {
                previewArea.setText(sampleText + "\nUser Preview:\n" + b + "\n");
            } else {
                previewArea.setText(sampleText);
            }
        });

        refreshPreview.run();

        rightPane.getChildren().addAll(previewArea, customInput);

        mainContainer.getChildren().addAll(leftPane, rightPane);
        return mainContainer;
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

    public static class KeymapActionItem {
        private final String id;
        private final String name;
        private final String description;
        private final String categoryPath;
        private final boolean isCategory;
        private final FontAwesomeSolid icon;
        private final String iconColor;
        private final List<String> defaultShortcuts;

        public KeymapActionItem(String id, String name, String description, String categoryPath,
                                boolean isCategory, FontAwesomeSolid icon, String iconColor,
                                List<String> defaultShortcuts) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.categoryPath = categoryPath;
            this.isCategory = isCategory;
            this.icon = icon;
            this.iconColor = iconColor != null ? iconColor : "#a9b7c6";
            this.defaultShortcuts = defaultShortcuts != null ? defaultShortcuts : Collections.emptyList();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCategoryPath() { return categoryPath; }
        public boolean isCategory() { return isCategory; }
        public FontAwesomeSolid getIcon() { return icon; }
        public String getIconColor() { return iconColor; }
        public List<String> getDefaultShortcuts() { return defaultShortcuts; }

        public List<String> getEffectiveShortcuts(String preset,
                                                 Map<String, List<String>> customShortcuts,
                                                 Map<String, List<String>> removedShortcuts) {
            if (isCategory) return Collections.emptyList();
            if (customShortcuts != null && customShortcuts.containsKey(id)) {
                return customShortcuts.get(id);
            }
            List<String> list = new ArrayList<>(resolvePresetShortcuts(preset));
            if (removedShortcuts != null && removedShortcuts.containsKey(id)) {
                list.removeAll(removedShortcuts.get(id));
            }
            return list;
        }

        public List<String> resolvePresetShortcuts(String preset) {
            if (preset == null) preset = KeyStrokeFormatter.isMac() ? "macOS" : "Windows";
            boolean isMacPreset = preset.contains("macOS") || preset.contains("Mac");
            boolean isEmacs = preset.equalsIgnoreCase("Emacs");
            boolean isSublime = preset.toLowerCase().contains("sublime");

            if (isEmacs) {
                if ("help.find.action".equals(id)) return List.of("Alt+X");
                if ("file.save".equals(id)) return List.of("Ctrl+X, Ctrl+S");
                if ("file.open".equals(id)) return List.of("Ctrl+X, Ctrl+F");
                if ("edit.undo".equals(id)) return List.of("Ctrl+_");
                if ("edit.cut".equals(id)) return List.of("Ctrl+W");
                if ("edit.copy".equals(id)) return List.of("Alt+W");
                if ("edit.paste".equals(id)) return List.of("Ctrl+Y");
                if ("db.execute".equals(id) || "run.execute.statement".equals(id)) return List.of("Ctrl+Enter");
            }

            if (isSublime) {
                if ("help.find.action".equals(id)) return isMacPreset ? List.of("⇧⌘P") : List.of("Ctrl+Shift+P");
                if ("file.new.scratch".equals(id)) return isMacPreset ? List.of("⌘N") : List.of("Ctrl+N");
                if ("navigate.file".equals(id)) return isMacPreset ? List.of("⌘P") : List.of("Ctrl+P");
                if ("db.execute".equals(id) || "run.execute.statement".equals(id)) return isMacPreset ? List.of("⌘↵") : List.of("Ctrl+Enter");
            }

            if (isMacPreset) {
                if ("help.find.action".equals(id)) return List.of("⇧⌘A");
                if ("file.new.scratch".equals(id)) return List.of("⇧⌘N");
                if ("file.settings".equals(id)) return List.of("⌘,");
                if ("db.execute".equals(id) || "run.execute.statement".equals(id) || "db.execute.statement".equals(id)) return List.of("⌘↵");
                if ("run.execute.script".equals(id) || "db.execute.script".equals(id)) return List.of("⌥⌘↵");
                if ("db.console".equals(id) || "file.new.console".equals(id) || "db.open.console".equals(id)) return List.of("⇧⌘Q");
                if ("db.refresh".equals(id) || "db.refresh.schema".equals(id)) return List.of("⌥⌘Y");
                if ("run.compare".equals(id)) return List.of("⌘D");
                if ("run.compare.structure".equals(id) || "db.compare.schema".equals(id)) return List.of("⇧⌘D");
                if ("run.fulltext.search".equals(id)) return List.of("⌥⇧⌘F");
                if ("vcs.operations.popup".equals(id)) return List.of("⌃V");
                if ("vcs.commit".equals(id)) return List.of("⌘K");
                if ("vcs.push".equals(id)) return List.of("⇧⌘K");
                if ("vcs.update".equals(id)) return List.of("⌘T");
                if ("window.minimize".equals(id)) return List.of("⌘M");
                if ("window.next.tab".equals(id)) return List.of("⇧⌘]");
                if ("window.prev.tab".equals(id)) return List.of("⇧⌘[");
                if ("window.next.window".equals(id)) return List.of("⌘`");
                if ("window.prev.window".equals(id)) return List.of("⇧⌘`");
                if ("editor.comment.line".equals(id)) return List.of("⌘/");
                if ("editor.comment.block".equals(id)) return List.of("⌥⌘/");
                if ("editor.duplicate".equals(id)) return List.of("⌘D");
                if ("editor.delete.line".equals(id)) return List.of("⌘⌫");
                if ("editor.move.line.up".equals(id)) return List.of("⌥⇧Up");
                if ("editor.move.line.down".equals(id)) return List.of("⌥⇧Down");
                if ("editor.reformat".equals(id) || "code.reformat".equals(id)) return List.of("⌥⌘L");
                if ("editor.complete.basic".equals(id)) return List.of("⌃Space");
                if ("editor.complete.smart".equals(id)) return List.of("⌃⇧Space");
                if ("navigate.search.everywhere".equals(id)) return List.of("Shift+Shift");
                if ("navigate.class".equals(id)) return List.of("⌘O");
                if ("navigate.file".equals(id)) return List.of("⇧⌘O");
                if ("navigate.symbol".equals(id)) return List.of("⌥⌘O");
                if ("navigate.line".equals(id)) return List.of("⌘L");
                if ("edit.undo".equals(id)) return List.of("⌘Z");
                if ("edit.redo".equals(id)) return List.of("⇧⌘Z");
                if ("edit.cut".equals(id)) return List.of("⌘X");
                if ("edit.copy".equals(id)) return List.of("⌘C");
                if ("edit.paste".equals(id)) return List.of("⌘V");
                if ("edit.find".equals(id)) return List.of("⌘F");
                if ("edit.replace".equals(id)) return List.of("⌘R");
                if ("toolwindow.database".equals(id)) return List.of("⌘1");
                if ("toolwindow.terminal".equals(id) || "tools.terminal".equals(id)) return List.of("⌥F12");
            } else {
                if ("help.find.action".equals(id)) return List.of("Ctrl+Shift+A");
                if ("file.new.scratch".equals(id)) return List.of("Ctrl+Alt+Shift+Insert");
                if ("file.settings".equals(id)) return List.of("Ctrl+Alt+S");
                if ("db.execute".equals(id) || "run.execute.statement".equals(id) || "db.execute.statement".equals(id)) return List.of("Ctrl+Enter");
                if ("run.execute.script".equals(id) || "db.execute.script".equals(id)) return List.of("Ctrl+Shift+Enter");
                if ("db.console".equals(id) || "file.new.console".equals(id) || "db.open.console".equals(id)) return List.of("Ctrl+Shift+Q");
                if ("db.refresh".equals(id) || "db.refresh.schema".equals(id)) return List.of("Ctrl+Alt+Y");
                if ("run.compare".equals(id)) return List.of("Ctrl+D");
                if ("run.compare.structure".equals(id) || "db.compare.schema".equals(id)) return List.of("Ctrl+Shift+D");
                if ("run.fulltext.search".equals(id)) return List.of("Ctrl+Alt+Shift+F");
                if ("vcs.operations.popup".equals(id)) return List.of("Alt+`");
                if ("vcs.commit".equals(id)) return List.of("Ctrl+K");
                if ("vcs.push".equals(id)) return List.of("Ctrl+Shift+K");
                if ("vcs.update".equals(id)) return List.of("Ctrl+T");
                if ("window.minimize".equals(id)) return List.of("Win+Down");
                if ("window.next.tab".equals(id)) return List.of("Alt+Right");
                if ("window.prev.tab".equals(id)) return List.of("Alt+Left");
                if ("window.next.window".equals(id)) return List.of("Ctrl+Alt+]");
                if ("window.prev.window".equals(id)) return List.of("Ctrl+Alt+[");
                if ("editor.comment.line".equals(id)) return List.of("Ctrl+/");
                if ("editor.comment.block".equals(id)) return List.of("Ctrl+Shift+/");
                if ("editor.duplicate".equals(id)) return List.of("Ctrl+D");
                if ("editor.delete.line".equals(id)) return List.of("Ctrl+Y");
                if ("editor.move.line.up".equals(id)) return List.of("Alt+Shift+Up");
                if ("editor.move.line.down".equals(id)) return List.of("Alt+Shift+Down");
                if ("editor.reformat".equals(id) || "code.reformat".equals(id)) return List.of("Ctrl+Alt+L");
                if ("editor.complete.basic".equals(id)) return List.of("Ctrl+Space");
                if ("editor.complete.smart".equals(id)) return List.of("Ctrl+Shift+Space");
                if ("navigate.search.everywhere".equals(id)) return List.of("Shift+Shift");
                if ("navigate.class".equals(id)) return List.of("Ctrl+N");
                if ("navigate.file".equals(id)) return List.of("Ctrl+Shift+N");
                if ("navigate.symbol".equals(id)) return List.of("Ctrl+Alt+Shift+N");
                if ("navigate.line".equals(id)) return List.of("Ctrl+G");
                if ("edit.undo".equals(id)) return List.of("Ctrl+Z");
                if ("edit.redo".equals(id)) return List.of("Ctrl+Shift+Z");
                if ("edit.cut".equals(id)) return List.of("Ctrl+X");
                if ("edit.copy".equals(id)) return List.of("Ctrl+C");
                if ("edit.paste".equals(id)) return List.of("Ctrl+V");
                if ("edit.find".equals(id)) return List.of("Ctrl+F");
                if ("edit.replace".equals(id)) return List.of("Ctrl+R");
                if ("toolwindow.database".equals(id)) return List.of("Alt+1");
                if ("toolwindow.terminal".equals(id) || "tools.terminal".equals(id)) return List.of("Alt+F12");
            }
            return defaultShortcuts;
        }

        public boolean isModified(String preset,
                                  Map<String, List<String>> customShortcuts,
                                  Map<String, List<String>> removedShortcuts) {
            if (isCategory) return false;
            return (customShortcuts != null && customShortcuts.containsKey(id)) ||
                   (removedShortcuts != null && removedShortcuts.containsKey(id));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static VBox buildKeymapPanel(AppSettingsStore.Settings settings,
                                        Map<String, Object> inputs,
                                        java.util.function.Consumer<String> navigateTo) {
        // Ensure actions are registered in ActionManager
        try {
            ActionRegistry.initialize(ActionManager.getInstance());
        } catch (Exception ignored) {}

        // Working state maps (bound to inputs for applySettings)
        Map<String, List<String>> workingCustomShortcuts =
                new LinkedHashMap<>(settings.getCustomKeymapShortcuts());
        Map<String, List<String>> workingRemovedShortcuts =
                new LinkedHashMap<>(settings.getRemovedKeymapShortcuts());
        List<String> workingCustomPresets =
                new ArrayList<>(settings.getCustomKeymapPresets());

        inputs.put("customKeymapShortcuts", workingCustomShortcuts);
        inputs.put("removedKeymapShortcuts", workingRemovedShortcuts);
        inputs.put("customKeymapPresets", workingCustomPresets);

        // 1. Top Section: Keymap Title
        Label title = new Label("Keymap");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        // 2. Preset ComboBox + Gear ⚙ Menu
        ComboBox<String> keymapCombo = new ComboBox<>();
        keymapCombo.setPrefWidth(210);
        keymapCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Runnable refreshPresetsList = () -> {
            String current = keymapCombo.getValue();
            keymapCombo.getItems().clear();
            List<String> presets = AppSettingsStore.defaultKeymapPresets();
            keymapCombo.getItems().addAll(presets);
            for (String custom : workingCustomPresets) {
                if (!keymapCombo.getItems().contains(custom)) {
                    keymapCombo.getItems().add(custom);
                }
            }
            if (current != null && keymapCombo.getItems().contains(current)) {
                keymapCombo.getSelectionModel().select(current);
            } else if (keymapCombo.getItems().contains(settings.getKeymapPreset())) {
                keymapCombo.getSelectionModel().select(settings.getKeymapPreset());
            } else if (!keymapCombo.getItems().isEmpty()) {
                keymapCombo.getSelectionModel().select(0);
            }
        };
        refreshPresetsList.run();
        inputs.put("keymapCombo", keymapCombo);

        // Gear button with Duplicate, Restore Defaults, Remove, Rename
        Button gearBtn = new Button();
        gearBtn.setGraphic(Icons.of(FontAwesomeSolid.COG, "#a9b7c6", 13));
        gearBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6;");

        ContextMenu gearMenu = new ContextMenu();
        MenuItem miDuplicate = new MenuItem("Duplicate…");
        MenuItem miRestore = new MenuItem("Restore Defaults");
        MenuItem miRemove = new MenuItem("Remove");
        MenuItem miRename = new MenuItem("Rename…");
        gearMenu.getItems().addAll(miDuplicate, miRestore, miRemove, miRename);

        gearBtn.setOnAction(e -> {
            String activePreset = keymapCombo.getValue();
            boolean isCustom = workingCustomPresets.contains(activePreset);
            miRemove.setDisable(!isCustom);
            miRename.setDisable(!isCustom);
            gearMenu.show(gearBtn, Side.BOTTOM, 0, 0);
        });

        HBox presetRow = new HBox(8, keymapCombo, gearBtn);
        presetRow.setAlignment(Pos.CENTER_LEFT);

        // Hyperlink: Get more keymaps in Settings | Plugins
        Hyperlink pluginLink = new Hyperlink("Get more keymaps in Settings | Plugins");
        pluginLink.setStyle("-fx-text-fill: #589df6; -fx-padding: 2 0; -fx-border-color: transparent; -fx-underline: false; -fx-font-size: 12px;");
        pluginLink.setOnMouseEntered(ev -> pluginLink.setStyle("-fx-text-fill: #70aeff; -fx-padding: 2 0; -fx-border-color: transparent; -fx-underline: true; -fx-font-size: 12px;"));
        pluginLink.setOnMouseExited(ev -> pluginLink.setStyle("-fx-text-fill: #589df6; -fx-padding: 2 0; -fx-border-color: transparent; -fx-underline: false; -fx-font-size: 12px;"));
        pluginLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Plugins");
            }
        });

        VBox topControls = new VBox(6, title, presetRow, pluginLink);

        // 3. Hierarchical Action Tree Model
        TreeItem<KeymapActionItem> rootItem = new TreeItem<>(
                new KeymapActionItem("root", "Root", "", "", true, null, null, null));
        rootItem.setExpanded(true);

        List<TreeItem<KeymapActionItem>> allCategoryItems = buildActionTreeCategories();
        rootItem.getChildren().addAll(allCategoryItems);

        TreeView<KeymapActionItem> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // 4. Toolbar above TreeView
        Button btnCollapseAll = new Button();
        btnCollapseAll.setText("><");
        btnCollapseAll.setTooltip(new Tooltip("Collapse All"));
        btnCollapseAll.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #a9b7c6; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-font-size: 11px; -fx-padding: 3 6; -fx-cursor: hand;");

        Button btnExpandAll = new Button();
        btnExpandAll.setText("<>");
        btnExpandAll.setTooltip(new Tooltip("Expand All"));
        btnExpandAll.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #a9b7c6; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-font-size: 11px; -fx-padding: 3 6; -fx-cursor: hand;");

        Button btnEdit = new Button("✎");
        btnEdit.setTooltip(new Tooltip("Edit Shortcut (Enter)"));
        btnEdit.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #a9b7c6; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-font-size: 12px; -fx-padding: 2 6; -fx-cursor: hand;");
        btnEdit.setDisable(true);

        ToggleButton btnFilter = new ToggleButton("⚠️");
        btnFilter.setTooltip(new Tooltip("Show only modified or conflicting shortcuts"));
        btnFilter.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #a9b7c6; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-font-size: 11px; -fx-padding: 2 6; -fx-cursor: hand;");

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);

        TextField searchFilter = new TextField();
        searchFilter.setPromptText("Search actions by shortcut or name…");
        searchFilter.setPrefWidth(280);
        searchFilter.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");

        Button btnFindByShortcut = new Button("🔍⌨");
        btnFindByShortcut.setTooltip(new Tooltip("Find Action by Keystroke"));
        btnFindByShortcut.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #a9b7c6; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-font-size: 11px; -fx-padding: 3 6; -fx-cursor: hand;");

        HBox toolbar = new HBox(6, btnCollapseAll, btnExpandAll, btnEdit, btnFilter, toolbarSpacer, searchFilter, btnFindByShortcut);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 2, 0));

        // 5. Conflict Banner at the bottom
        VBox bottomConflictBanner = new VBox(2);
        bottomConflictBanner.setStyle("-fx-background-color: rgba(224, 164, 76, 0.08); -fx-border-color: rgba(224, 164, 76, 0.25); -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8 12;");

        // Helper to refresh conflict banner
        Runnable refreshConflictBanner = () -> {
            bottomConflictBanner.getChildren().clear();
            String currentPreset = keymapCombo.getValue();
            List<TreeItem<KeymapActionItem>> leaves = getAllLeafActions(rootItem);
            List<KeymapActionItem> conflicts = new ArrayList<>();

            for (TreeItem<KeymapActionItem> leaf : leaves) {
                KeymapActionItem item = leaf.getValue();
                if (item != null) {
                    List<String> shortcuts = item.getEffectiveShortcuts(currentPreset, workingCustomShortcuts, workingRemovedShortcuts);
                    for (String sc : shortcuts) {
                        if (KeyStrokeFormatter.checkMacConflict(sc) != null) {
                            conflicts.add(item);
                            break;
                        }
                    }
                }
            }

            if (conflicts.isEmpty() || !KeyStrokeFormatter.isMac()) {
                bottomConflictBanner.setVisible(false);
                bottomConflictBanner.setManaged(false);
                return;
            }

            bottomConflictBanner.setVisible(true);
            bottomConflictBanner.setManaged(true);

            HBox line1 = new HBox(4);
            line1.setAlignment(Pos.CENTER_LEFT);

            Label warnIcon = new Label("⚠️ ");
            warnIcon.setStyle("-fx-text-fill: #e0a44c; -fx-font-size: 12px;");
            line1.getChildren().add(warnIcon);

            int displayCount = Math.min(3, conflicts.size());
            for (int i = 0; i < displayCount; i++) {
                KeymapActionItem cItem = conflicts.get(i);
                Hyperlink actionLink = new Hyperlink(cItem.getName());
                actionLink.setStyle("-fx-text-fill: #589df6; -fx-padding: 0; -fx-border-color: transparent; -fx-font-size: 12px;");
                actionLink.setOnAction(ev -> {
                    selectActionInTree(treeView, rootItem, cItem.getId());
                });
                line1.getChildren().add(actionLink);
                if (i < displayCount - 1) {
                    Label comma = new Label(", ");
                    comma.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
                    line1.getChildren().add(comma);
                }
            }

            if (conflicts.size() > 3) {
                int more = conflicts.size() - 3;
                Label andLabel = new Label(" and ");
                andLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
                Hyperlink moreLink = new Hyperlink(more + " more");
                moreLink.setStyle("-fx-text-fill: #589df6; -fx-padding: 0; -fx-border-color: transparent; -fx-font-size: 12px;");
                moreLink.setOnAction(ev -> {
                    btnFilter.setSelected(true);
                    btnFilter.fire();
                });
                Label suffix = new Label(" shortcuts conflict with the macOS system shortcuts.");
                suffix.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
                line1.getChildren().addAll(andLabel, moreLink, suffix);
            } else {
                Label suffix = new Label(" shortcuts conflict with the macOS system shortcuts.");
                suffix.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
                line1.getChildren().add(suffix);
            }

            Label line2 = new Label("Assign custom shortcuts or change the macOS system settings.");
            line2.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px;");

            bottomConflictBanner.getChildren().addAll(line1, line2);
        };

        // Filter / Search engine
        Runnable applyFilter = () -> {
            String query = searchFilter.getText() != null ? searchFilter.getText().trim().toLowerCase() : "";
            boolean filterConflicts = btnFilter.isSelected();
            String currentPreset = keymapCombo.getValue();

            filterTreeRecursively(rootItem, allCategoryItems, query, filterConflicts, currentPreset,
                    workingCustomShortcuts, workingRemovedShortcuts);
            refreshConflictBanner.run();
        };

        searchFilter.textProperty().addListener((obs, oldV, newV) -> applyFilter.run());
        btnFilter.setOnAction(e -> applyFilter.run());

        // Find by Keystroke Popup
        btnFindByShortcut.setOnAction(e -> {
            Dialog<String> ksDialog = new Dialog<>();
            ksDialog.setTitle("Find Shortcut by Keystroke");
            DialogPane pane = ksDialog.getDialogPane();
            pane.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text;");

            Label prompt = new Label("Press the shortcut key combination to filter:");
            prompt.setStyle("-fx-text-fill: -text;");
            TextField strokeBox = new TextField();
            strokeBox.setPromptText("Press keys (e.g. ⇧⌘A)…");
            strokeBox.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: #dfe1e5; -fx-border-color: #3574f0; -fx-font-weight: bold;");

            strokeBox.setOnKeyPressed(ke -> {
                String formatted = KeyStrokeFormatter.formatFromEvent(ke);
                if (formatted != null) {
                    strokeBox.setText(formatted);
                    ksDialog.setResult(formatted);
                    ksDialog.close();
                }
                ke.consume();
            });

            VBox box = new VBox(10, prompt, strokeBox);
            box.setPadding(new Insets(14));
            pane.setContent(box);
            pane.getButtonTypes().add(ButtonType.CANCEL);
            ksDialog.showAndWait().ifPresent(res -> {
                if (res != null && !res.isBlank()) {
                    searchFilter.setText(res);
                }
            });
        });

        // Collapse / Expand handlers
        btnCollapseAll.setOnAction(e -> setTreeExpanded(rootItem, false));
        btnExpandAll.setOnAction(e -> setTreeExpanded(rootItem, true));

        // Cell Factory for custom display
        treeView.setCellFactory(tv -> new TreeCell<>() {
            {
                selectedProperty().addListener((obs, wasSel, isSel) -> updateCellStyle());
                hoverProperty().addListener((obs, wasHov, isHov) -> updateCellStyle());
            }

            private void updateCellStyle() {
                if (isEmpty() || getItem() == null) {
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                if (isSelected()) {
                    setStyle("-fx-background-color: #2e436e; -fx-background-radius: 3; -fx-text-fill: #ffffff;");
                } else if (isHover()) {
                    setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 3; -fx-text-fill: #dfe1e5;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #dfe1e5;");
                }
            }

            @Override
            protected void updateItem(KeymapActionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    setStyle("-fx-background-color: transparent;");
                } else if (item.isCategory()) {
                    setText(null);
                    updateCellStyle();
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER_LEFT);
                    box.getChildren().add(Icons.of(FontAwesomeSolid.FOLDER, "#e0a44c", 13));
                    Label catLabel = new Label(item.getName());
                    catLabel.setStyle("-fx-text-fill: #dfe1e5; -fx-font-weight: normal;");
                    box.getChildren().add(catLabel);
                    setGraphic(box);
                    setContextMenu(null);
                } else {
                    setText(null);
                    updateCellStyle();
                    HBox row = new HBox(6);
                    row.setAlignment(Pos.CENTER_LEFT);

                    if (item.getIcon() != null) {
                        row.getChildren().add(Icons.of(item.getIcon(), item.getIconColor(), 12));
                    } else {
                        Region space = new Region();
                        space.setPrefWidth(12);
                        row.getChildren().add(space);
                    }

                    Label nameLabel = new Label(item.getName());
                    nameLabel.setStyle("-fx-text-fill: -text;");
                    row.getChildren().add(nameLabel);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    row.getChildren().add(spacer);

                    String currentPreset = keymapCombo.getValue();
                    List<String> effective = item.getEffectiveShortcuts(currentPreset, workingCustomShortcuts, workingRemovedShortcuts);
                    boolean modified = item.isModified(currentPreset, workingCustomShortcuts, workingRemovedShortcuts);

                    if (!effective.isEmpty()) {
                        HBox badges = new HBox(4);
                        badges.setAlignment(Pos.CENTER_RIGHT);
                        for (String sc : effective) {
                            Label badge = new Label(sc);
                            if (modified) {
                                badge.setStyle("-fx-background-color: #2e436e; -fx-text-fill: #70aeff; -fx-font-weight: bold; -fx-padding: 1 6; -fx-background-radius: 4; -fx-font-size: 11px;");
                            } else {
                                badge.setStyle("-fx-background-color: #393b40; -fx-text-fill: #dfe1e5; -fx-padding: 1 6; -fx-background-radius: 4; -fx-font-size: 11px;");
                            }
                            badges.getChildren().add(badge);
                        }
                        row.getChildren().add(badges);
                    }

                    setGraphic(row);

                    // Context Menu for action
                    ContextMenu cm = new ContextMenu();
                    MenuItem miAdd = new MenuItem("Add Keyboard Shortcut…");
                    MenuItem miRemove = new MenuItem("Remove Shortcut");
                    MenuItem miReset = new MenuItem("Reset to Default");

                    miAdd.setOnAction(ev -> showKeyboardShortcutDialog(item, keymapCombo.getValue(),
                            workingCustomShortcuts, workingRemovedShortcuts, () -> {
                                treeView.refresh();
                                refreshConflictBanner.run();
                            }));

                    miRemove.setOnAction(ev -> {
                        List<String> current = item.getEffectiveShortcuts(keymapCombo.getValue(), workingCustomShortcuts, workingRemovedShortcuts);
                        if (!current.isEmpty()) {
                            workingRemovedShortcuts.computeIfAbsent(item.getId(), k -> new ArrayList<>()).addAll(current);
                            workingCustomShortcuts.remove(item.getId());
                            treeView.refresh();
                            refreshConflictBanner.run();
                        }
                    });

                    miReset.setOnAction(ev -> {
                        workingCustomShortcuts.remove(item.getId());
                        workingRemovedShortcuts.remove(item.getId());
                        treeView.refresh();
                        refreshConflictBanner.run();
                    });

                    cm.getItems().addAll(miAdd, miRemove, miReset);
                    setContextMenu(cm);
                }
            }
        });

        // Selection Listener enables Edit button
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean isLeaf = newV != null && newV.getValue() != null && !newV.getValue().isCategory();
            btnEdit.setDisable(!isLeaf);
        });

        // Edit button action
        btnEdit.setOnAction(e -> {
            TreeItem<KeymapActionItem> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue() != null && !selected.getValue().isCategory()) {
                showKeyboardShortcutDialog(selected.getValue(), keymapCombo.getValue(),
                        workingCustomShortcuts, workingRemovedShortcuts, () -> {
                            treeView.refresh();
                            refreshConflictBanner.run();
                        });
            }
        });

        // Double click & Enter key trigger shortcut dialog
        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<KeymapActionItem> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() != null && !selected.getValue().isCategory()) {
                    showKeyboardShortcutDialog(selected.getValue(), keymapCombo.getValue(),
                            workingCustomShortcuts, workingRemovedShortcuts, () -> {
                                treeView.refresh();
                                refreshConflictBanner.run();
                            });
                }
            }
        });

        treeView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                TreeItem<KeymapActionItem> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() != null && !selected.getValue().isCategory()) {
                    showKeyboardShortcutDialog(selected.getValue(), keymapCombo.getValue(),
                            workingCustomShortcuts, workingRemovedShortcuts, () -> {
                                treeView.refresh();
                                refreshConflictBanner.run();
                            });
                    e.consume();
                }
            }
        });

        // Gear Menu Actions: Duplicate, Restore, Remove, Rename
        miDuplicate.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog(keymapCombo.getValue() + " copy");
            tid.setTitle("Duplicate Keymap");
            tid.setHeaderText("Create a copy of '" + keymapCombo.getValue() + "'");
            tid.setContentText("Keymap name:");
            tid.showAndWait().ifPresent(name -> {
                String trimmed = name.trim();
                if (!trimmed.isEmpty() && !workingCustomPresets.contains(trimmed)) {
                    workingCustomPresets.add(trimmed);
                    refreshPresetsList.run();
                    keymapCombo.getSelectionModel().select(trimmed);
                    applyFilter.run();
                }
            });
        });

        miRestore.setOnAction(e -> {
            workingCustomShortcuts.clear();
            workingRemovedShortcuts.clear();
            treeView.refresh();
            refreshConflictBanner.run();
        });

        miRemove.setOnAction(e -> {
            String active = keymapCombo.getValue();
            if (workingCustomPresets.contains(active)) {
                workingCustomPresets.remove(active);
                refreshPresetsList.run();
                applyFilter.run();
            }
        });

        miRename.setOnAction(e -> {
            String active = keymapCombo.getValue();
            if (workingCustomPresets.contains(active)) {
                TextInputDialog tid = new TextInputDialog(active);
                tid.setTitle("Rename Keymap");
                tid.setHeaderText("Enter new name for '" + active + "'");
                tid.setContentText("New name:");
                tid.showAndWait().ifPresent(name -> {
                    String trimmed = name.trim();
                    if (!trimmed.isEmpty()) {
                        int idx = workingCustomPresets.indexOf(active);
                        if (idx >= 0) workingCustomPresets.set(idx, trimmed);
                        refreshPresetsList.run();
                        keymapCombo.getSelectionModel().select(trimmed);
                    }
                });
            }
        });

        keymapCombo.valueProperty().addListener((obs, oldV, newV) -> {
            treeView.refresh();
            refreshConflictBanner.run();
        });

        // Initialize conflict banner
        refreshConflictBanner.run();

        VBox panel = new VBox(10, topControls, toolbar, treeView, bottomConflictBanner);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        panel.setPadding(new Insets(4, 8, 12, 8));
        return panel;
    }

    private static void setTreeExpanded(TreeItem<?> item, boolean expanded) {
        if (item == null) return;
        for (TreeItem<?> child : item.getChildren()) {
            child.setExpanded(expanded);
            setTreeExpanded(child, expanded);
        }
    }

    private static List<TreeItem<KeymapActionItem>> getAllLeafActions(TreeItem<KeymapActionItem> root) {
        List<TreeItem<KeymapActionItem>> leaves = new ArrayList<>();
        collectLeaves(root, leaves);
        return leaves;
    }

    private static void collectLeaves(TreeItem<KeymapActionItem> node, List<TreeItem<KeymapActionItem>> leaves) {
        if (node == null) return;
        if (node.getValue() != null && !node.getValue().isCategory()) {
            leaves.add(node);
        }
        for (TreeItem<KeymapActionItem> child : node.getChildren()) {
            collectLeaves(child, leaves);
        }
    }

    private static void selectActionInTree(TreeView<KeymapActionItem> treeView,
                                          TreeItem<KeymapActionItem> root,
                                          String actionId) {
        TreeItem<KeymapActionItem> target = findActionItem(root, actionId);
        if (target != null) {
            TreeItem<KeymapActionItem> p = target.getParent();
            while (p != null) {
                p.setExpanded(true);
                p = p.getParent();
            }
            treeView.getSelectionModel().select(target);
            int row = treeView.getRow(target);
            if (row >= 0) treeView.scrollTo(row);
        }
    }

    private static TreeItem<KeymapActionItem> findActionItem(TreeItem<KeymapActionItem> current, String actionId) {
        if (current == null) return null;
        if (current.getValue() != null && actionId.equals(current.getValue().getId())) {
            return current;
        }
        for (TreeItem<KeymapActionItem> child : current.getChildren()) {
            TreeItem<KeymapActionItem> found = findActionItem(child, actionId);
            if (found != null) return found;
        }
        return null;
    }

    private static void filterTreeRecursively(TreeItem<KeymapActionItem> root,
                                              List<TreeItem<KeymapActionItem>> allCategories,
                                              String query,
                                              boolean filterConflicts,
                                              String preset,
                                              Map<String, List<String>> customShortcuts,
                                              Map<String, List<String>> removedShortcuts) {
        root.getChildren().clear();
        for (TreeItem<KeymapActionItem> cat : allCategories) {
            TreeItem<KeymapActionItem> filteredCat = filterItem(cat, query, filterConflicts, preset, customShortcuts, removedShortcuts);
            if (filteredCat != null) {
                root.getChildren().add(filteredCat);
                if (!query.isEmpty() || filterConflicts) {
                    filteredCat.setExpanded(true);
                }
            }
        }
    }

    private static TreeItem<KeymapActionItem> filterItem(TreeItem<KeymapActionItem> item,
                                                        String query,
                                                        boolean filterConflicts,
                                                        String preset,
                                                        Map<String, List<String>> customShortcuts,
                                                        Map<String, List<String>> removedShortcuts) {
        if (item == null || item.getValue() == null) return null;
        KeymapActionItem val = item.getValue();

        if (!val.isCategory()) {
            boolean matchesQuery = true;
            if (!query.isEmpty()) {
                boolean nameMatch = val.getName().toLowerCase().contains(query);
                boolean idMatch = val.getId().toLowerCase().contains(query);
                boolean descMatch = val.getDescription().toLowerCase().contains(query);
                boolean shortcutMatch = val.getEffectiveShortcuts(preset, customShortcuts, removedShortcuts).stream()
                        .anyMatch(s -> s.toLowerCase().contains(query));
                matchesQuery = nameMatch || idMatch || descMatch || shortcutMatch;
            }

            boolean matchesConflict = true;
            if (filterConflicts) {
                boolean isMod = val.isModified(preset, customShortcuts, removedShortcuts);
                boolean hasConflict = val.getEffectiveShortcuts(preset, customShortcuts, removedShortcuts).stream()
                        .anyMatch(s -> KeyStrokeFormatter.checkMacConflict(s) != null);
                matchesConflict = isMod || hasConflict;
            }

            if (matchesQuery && matchesConflict) {
                return new TreeItem<>(val);
            }
            return null;
        } else {
            TreeItem<KeymapActionItem> copyCat = new TreeItem<>(val);
            for (TreeItem<KeymapActionItem> child : item.getChildren()) {
                TreeItem<KeymapActionItem> filteredChild = filterItem(child, query, filterConflicts, preset, customShortcuts, removedShortcuts);
                if (filteredChild != null) {
                    copyCat.getChildren().add(filteredChild);
                }
            }
            if (!copyCat.getChildren().isEmpty()) {
                return copyCat;
            }
            if (!query.isEmpty() && val.getName().toLowerCase().contains(query)) {
                // If category name matches query, include all its children
                copyCat.getChildren().addAll(item.getChildren());
                return copyCat;
            }
            return null;
        }
    }

    /**
     * Builds the complete taxonomy of categories and actions matching DataGrip.
     */
    private static List<TreeItem<KeymapActionItem>> buildActionTreeCategories() {
        List<TreeItem<KeymapActionItem>> categories = new ArrayList<>();

        // 1. Editor Actions
        TreeItem<KeymapActionItem> editorActions = new TreeItem<>(
                new KeymapActionItem("cat.editor", "Editor Actions", "Actions performed within the code and SQL editor", "Editor Actions", true, null, null, null));
        editorActions.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("editor.complete.basic", "Basic", "Code completion popup", "Editor Actions", false, null, null, List.of("⌃Space"))),
                new TreeItem<>(new KeymapActionItem("editor.complete.smart", "SmartType", "Smart completion popup", "Editor Actions", false, null, null, List.of("⌃⇧Space"))),
                new TreeItem<>(new KeymapActionItem("editor.comment.line", "Comment with Line Comment", "Toggle single-line comment", "Editor Actions", false, null, null, List.of("⌘/"))),
                new TreeItem<>(new KeymapActionItem("editor.comment.block", "Comment with Block Comment", "Toggle multi-line block comment", "Editor Actions", false, null, null, List.of("⌥⌘/"))),
                new TreeItem<>(new KeymapActionItem("editor.duplicate", "Duplicate Line or Selection", "Duplicate current line or selection", "Editor Actions", false, null, null, List.of("⌘D"))),
                new TreeItem<>(new KeymapActionItem("editor.delete.line", "Delete Line", "Delete active line", "Editor Actions", false, null, null, List.of("⌘⌫"))),
                new TreeItem<>(new KeymapActionItem("editor.move.line.up", "Move Line Up", "Move current line up", "Editor Actions", false, null, null, List.of("⌥⇧Up"))),
                new TreeItem<>(new KeymapActionItem("editor.move.line.down", "Move Line Down", "Move current line down", "Editor Actions", false, null, null, List.of("⌥⇧Down"))),
                new TreeItem<>(new KeymapActionItem("editor.reformat", "Reformat Code", "Format SQL / code according to rules", "Editor Actions", false, null, null, List.of("⌥⌘L"))),
                new TreeItem<>(new KeymapActionItem("editor.indent", "Indent Selection", "Indent line or selection", "Editor Actions", false, null, null, List.of("⇥"))),
                new TreeItem<>(new KeymapActionItem("editor.unindent", "Unindent Selection", "Unindent line or selection", "Editor Actions", false, null, null, List.of("⇧⇥"))),
                new TreeItem<>(new KeymapActionItem("editor.toggle.case", "Toggle Case", "Toggle case of selected text", "Editor Actions", false, null, null, List.of("⇧⌘U"))),
                new TreeItem<>(new KeymapActionItem("editor.join.lines", "Join Lines", "Join lines into one", "Editor Actions", false, null, null, List.of("⌃⇧J")))
        );
        categories.add(editorActions);

        // 2. Main Menu
        TreeItem<KeymapActionItem> mainMenu = new TreeItem<>(
                new KeymapActionItem("cat.mainmenu", "Main Menu", "Application main menu items", "Main Menu", true, null, null, null));

        // Submenus: File, Edit, View, Navigate, Code, Refactor, Run, Tools, Database, Window, Help
        TreeItem<KeymapActionItem> menuFile = new TreeItem<>(new KeymapActionItem("menu.file", "File", "File operations", "Main Menu | File", true, null, null, null));
        TreeItem<KeymapActionItem> menuFileNew = new TreeItem<>(new KeymapActionItem("file.new", "New", "Create new items", "Main Menu | File | New", true, null, null, null));
        menuFileNew.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("file.new.project", "Project…", "Create a new project", "Main Menu | File | New", false, null, null, Collections.emptyList())),
                new TreeItem<>(new KeymapActionItem("file.new.sqlfile", "SQL File", "Create a new SQL file", "Main Menu | File | New", false, FontAwesomeSolid.FILE_CODE, "#a9b7c6", Collections.emptyList())),
                new TreeItem<>(new KeymapActionItem("file.new.scratch", "Scratch File", "Open a scratch SQL buffer", "Main Menu | File | New", false, FontAwesomeSolid.FILE_ALT, "#a9b7c6", List.of("⇧⌘N"))),
                new TreeItem<>(new KeymapActionItem("file.new.console", "Query Console", "Open a new query console", "Main Menu | File | New", false, FontAwesomeSolid.TERMINAL, "#6897bb", List.of("⇧⌘Q"))),
                new TreeItem<>(new KeymapActionItem("file.new.queryfile", "Query File…", "Create a new query file", "Main Menu | File | New", false, FontAwesomeSolid.FILE_CODE, "#4a88c7", Collections.emptyList())),
                new TreeItem<>(new KeymapActionItem("file.new.database", "Database", "Create a new database", "Main Menu | File | New", false, FontAwesomeSolid.DATABASE, "#4a88c7", Collections.emptyList())),
                new TreeItem<>(new KeymapActionItem("file.new.datasource", "Data Source", "Create a new connection profile", "Main Menu | File | New", false, FontAwesomeSolid.DATABASE, "#57965c", Collections.emptyList()))
        );
        menuFile.getChildren().addAll(
                menuFileNew,
                new TreeItem<>(new KeymapActionItem("file.open", "Open…", "Open a directory or file", "Main Menu | File", false, null, null, List.of("⌘O"))),
                new TreeItem<>(new KeymapActionItem("file.save", "Save All", "Save all modified files", "Main Menu | File", false, null, null, List.of("⌘S"))),
                new TreeItem<>(new KeymapActionItem("file.settings", "Settings…", "Open Settings dialog", "Main Menu | File", false, null, null, List.of("⌘,"))),
                new TreeItem<>(new KeymapActionItem("file.invalidate.caches", "Invalidate Caches…", "Invalidate IDE caches and restart", "Main Menu | File", false, null, null, Collections.emptyList())),
                new TreeItem<>(new KeymapActionItem("file.exit", "Exit", "Exit application", "Main Menu | File", false, null, null, List.of("⌘Q")))
        );

        TreeItem<KeymapActionItem> menuEdit = new TreeItem<>(new KeymapActionItem("menu.edit", "Edit", "Edit commands", "Main Menu | Edit", true, null, null, null));
        menuEdit.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("edit.undo", "Undo", "Undo last operation", "Main Menu | Edit", false, null, null, List.of("⌘Z"))),
                new TreeItem<>(new KeymapActionItem("edit.redo", "Redo", "Redo last operation", "Main Menu | Edit", false, null, null, List.of("⇧⌘Z"))),
                new TreeItem<>(new KeymapActionItem("edit.cut", "Cut", "Cut selection to clipboard", "Main Menu | Edit", false, null, null, List.of("⌘X"))),
                new TreeItem<>(new KeymapActionItem("edit.copy", "Copy", "Copy selection to clipboard", "Main Menu | Edit", false, null, null, List.of("⌘C"))),
                new TreeItem<>(new KeymapActionItem("edit.paste", "Paste", "Paste from clipboard", "Main Menu | Edit", false, null, null, List.of("⌘V"))),
                new TreeItem<>(new KeymapActionItem("edit.find", "Find", "Find text in current buffer", "Main Menu | Edit", false, null, null, List.of("⌘F"))),
                new TreeItem<>(new KeymapActionItem("edit.replace", "Replace", "Replace text in current buffer", "Main Menu | Edit", false, null, null, List.of("⌘R"))),
                new TreeItem<>(new KeymapActionItem("edit.find.in.files", "Find in Files…", "Search across project files", "Main Menu | Edit", false, null, null, List.of("⇧⌘F"))),
                new TreeItem<>(new KeymapActionItem("edit.replace.in.files", "Replace in Files…", "Replace across project files", "Main Menu | Edit", false, null, null, List.of("⇧⌘R")))
        );

        TreeItem<KeymapActionItem> menuView = new TreeItem<>(new KeymapActionItem("menu.view", "View", "View options", "Main Menu | View", true, null, null, null));
        menuView.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("view.quick.doc", "Quick Documentation", "Show documentation popup", "Main Menu | View", false, null, null, List.of("F1"))),
                new TreeItem<>(new KeymapActionItem("view.quick.definition", "Quick Definition", "Show definition popup", "Main Menu | View", false, null, null, List.of("⌥Space"))),
                new TreeItem<>(new KeymapActionItem("view.toolwindows", "Tool Windows", "Manage tool windows", "Main Menu | View", false, null, null, Collections.emptyList()))
        );

        TreeItem<KeymapActionItem> menuNavigate = new TreeItem<>(new KeymapActionItem("menu.navigate", "Navigate", "Navigation actions", "Main Menu | Navigate", true, null, null, null));
        menuNavigate.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("navigate.search.everywhere", "Search Everywhere", "Search for anything across the IDE", "Main Menu | Navigate", false, null, null, List.of("Shift+Shift"))),
                new TreeItem<>(new KeymapActionItem("navigate.class", "Class…", "Navigate to database entity or class", "Main Menu | Navigate", false, null, null, List.of("⌘O"))),
                new TreeItem<>(new KeymapActionItem("navigate.file", "File…", "Navigate to file", "Main Menu | Navigate", false, null, null, List.of("⇧⌘O"))),
                new TreeItem<>(new KeymapActionItem("navigate.symbol", "Symbol…", "Navigate to symbol", "Main Menu | Navigate", false, null, null, List.of("⌥⌘O"))),
                new TreeItem<>(new KeymapActionItem("navigate.line", "Line:Column…", "Jump to line and column", "Main Menu | Navigate", false, null, null, List.of("⌘L"))),
                new TreeItem<>(new KeymapActionItem("navigate.back", "Back", "Navigate back in history", "Main Menu | Navigate", false, null, null, List.of("⌘["))),
                new TreeItem<>(new KeymapActionItem("navigate.forward", "Forward", "Navigate forward in history", "Main Menu | Navigate", false, null, null, List.of("⌘]")))
        );

        TreeItem<KeymapActionItem> menuCode = new TreeItem<>(new KeymapActionItem("menu.code", "Code", "Code tools", "Main Menu | Code", true, null, null, null));
        menuCode.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("code.reformat", "Reformat Code", "Reformat code to standards", "Main Menu | Code", false, null, null, List.of("⌥⌘L"))),
                new TreeItem<>(new KeymapActionItem("code.generate", "Generate…", "Generate SQL / DDL / queries", "Main Menu | Code", false, null, null, List.of("⌘N")))
        );

        TreeItem<KeymapActionItem> menuRun = new TreeItem<>(new KeymapActionItem("menu.run", "Run", "Execution commands", "Main Menu | Run", true, null, null, null));
        menuRun.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("run.execute.statement", "Execute Statement", "Run current statement", "Main Menu | Run", false, FontAwesomeSolid.PLAY, "#57965c", List.of("⌘↵"))),
                new TreeItem<>(new KeymapActionItem("run.execute.script", "Execute Script", "Run entire script", "Main Menu | Run", false, FontAwesomeSolid.FAST_FORWARD, "#4a88c7", List.of("⌥⌘↵"))),
                new TreeItem<>(new KeymapActionItem("run.compare", "Compare Data", "Compare database tables or query results", "Main Menu | Run", false, null, null, List.of("⌘D"))),
                new TreeItem<>(new KeymapActionItem("run.compare.structure", "Compare Schema Structure", "Diff schemas between connections", "Main Menu | Run", false, null, null, List.of("⇧⌘D"))),
                new TreeItem<>(new KeymapActionItem("run.fulltext.search", "Full-Text Search…", "Search full-text across database tables", "Main Menu | Run", false, null, null, List.of("⌥⇧⌘F")))
        );

        TreeItem<KeymapActionItem> menuWindow = new TreeItem<>(new KeymapActionItem("menu.window", "Window", "Window management", "Main Menu | Window", true, null, null, null));
        menuWindow.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("window.minimize", "Minimize", "Minimize DBNavigator window", "Main Menu | Window", false, null, null, List.of("⌘M"))),
                new TreeItem<>(new KeymapActionItem("window.next.tab", "Select Next Tab", "Switch to next editor tab", "Main Menu | Window", false, null, null, List.of("⇧⌘]"))),
                new TreeItem<>(new KeymapActionItem("window.prev.tab", "Select Previous Tab", "Switch to previous editor tab", "Main Menu | Window", false, null, null, List.of("⇧⌘["))),
                new TreeItem<>(new KeymapActionItem("window.next.window", "Next Project Window", "Focus next project window", "Main Menu | Window", false, null, null, List.of("⌘`"))),
                new TreeItem<>(new KeymapActionItem("window.prev.window", "Previous Project Window", "Focus previous project window", "Main Menu | Window", false, null, null, List.of("⇧⌘`")))
        );

        TreeItem<KeymapActionItem> menuHelp = new TreeItem<>(new KeymapActionItem("menu.help", "Help", "Help and documentation", "Main Menu | Help", true, null, null, null));
        menuHelp.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("help.find.action", "Find Action…", "Find any action or settings entry", "Main Menu | Help", false, null, null, List.of("⇧⌘A"))),
                new TreeItem<>(new KeymapActionItem("help.about", "About DBNavigator", "Version and licensing info", "Main Menu | Help", false, null, null, Collections.emptyList()))
        );

        mainMenu.getChildren().addAll(menuFile, menuEdit, menuView, menuNavigate, menuCode, menuRun, menuWindow, menuHelp);
        categories.add(mainMenu);

        // 3. Tool Windows
        TreeItem<KeymapActionItem> toolWindows = new TreeItem<>(
                new KeymapActionItem("cat.toolwindows", "Tool Windows", "IDE Tool Windows", "Tool Windows", true, null, null, null));
        toolWindows.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("toolwindow.database", "Database", "Show Database explorer tool window", "Tool Windows", false, FontAwesomeSolid.DATABASE, "#57965c", List.of("⌘1"))),
                new TreeItem<>(new KeymapActionItem("toolwindow.files", "Files", "Show Files tool window", "Tool Windows", false, FontAwesomeSolid.FOLDER, "#e0a44c", List.of("⌘2"))),
                new TreeItem<>(new KeymapActionItem("toolwindow.commit", "Commit", "Show Commit tool window", "Tool Windows", false, FontAwesomeSolid.CODE_BRANCH, "#4a88c7", List.of("⌘0"))),
                new TreeItem<>(new KeymapActionItem("toolwindow.services", "Services", "Show Services tool window", "Tool Windows", false, FontAwesomeSolid.SERVER, "#e0a44c", List.of("⌘8"))),
                new TreeItem<>(new KeymapActionItem("toolwindow.terminal", "Terminal", "Show Terminal tool window", "Tool Windows", false, FontAwesomeSolid.TERMINAL, "#6897bb", List.of("⌥F12"))),
                new TreeItem<>(new KeymapActionItem("toolwindow.run", "Run", "Show Run tool window", "Tool Windows", false, FontAwesomeSolid.PLAY, "#57965c", List.of("⌘4"))),
                new TreeItem<>(new KeymapActionItem("toolwindow.output", "Output Console", "Show Output Console", "Tool Windows", false, FontAwesomeSolid.DESKTOP, "#868a91", List.of("⌘5")))
        );
        categories.add(toolWindows);

        // 4. External Tools
        categories.add(new TreeItem<>(new KeymapActionItem("cat.externaltools", "External Tools", "Configured external tools and scripts", "External Tools", true, null, null, null)));

        // 5. External Build Systems
        categories.add(new TreeItem<>(new KeymapActionItem("cat.buildsystems", "External Build Systems", "Build systems integration", "External Build Systems", true, null, null, null)));

        // 6. Version Control Systems
        TreeItem<KeymapActionItem> vcs = new TreeItem<>(
                new KeymapActionItem("cat.vcs", "Version Control Systems", "Git and version control", "Version Control Systems", true, null, null, null));
        vcs.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("vcs.operations.popup", "VCS Operations Popup…", "Open VCS quick popup", "Version Control Systems", false, null, null, List.of("⌃V"))),
                new TreeItem<>(new KeymapActionItem("vcs.commit", "Commit…", "Commit changes", "Version Control Systems", false, null, null, List.of("⌘K"))),
                new TreeItem<>(new KeymapActionItem("vcs.push", "Push…", "Push commits to remote", "Version Control Systems", false, null, null, List.of("⇧⌘K"))),
                new TreeItem<>(new KeymapActionItem("vcs.update", "Update Project…", "Pull and update project", "Version Control Systems", false, null, null, List.of("⌘T")))
        );
        categories.add(vcs);

        // 7. Database
        TreeItem<KeymapActionItem> database = new TreeItem<>(
                new KeymapActionItem("cat.database", "Database", "Database operations and query execution", "Database", true, null, null, null));
        database.getChildren().addAll(
                new TreeItem<>(new KeymapActionItem("db.execute.statement", "Execute Statement", "Execute query statement", "Database", false, FontAwesomeSolid.PLAY, "#57965c", List.of("⌘↵"))),
                new TreeItem<>(new KeymapActionItem("db.execute.script", "Execute Script", "Execute script file", "Database", false, FontAwesomeSolid.FAST_FORWARD, "#4a88c7", List.of("⌥⌘↵"))),
                new TreeItem<>(new KeymapActionItem("db.open.console", "Open Console", "Open new query console", "Database", false, FontAwesomeSolid.TERMINAL, "#6897bb", List.of("⇧⌘Q"))),
                new TreeItem<>(new KeymapActionItem("db.refresh.schema", "Refresh Schema", "Synchronize database schema metadata", "Database", false, FontAwesomeSolid.SYNC_ALT, "#4a88c7", List.of("⌥⌘Y"))),
                new TreeItem<>(new KeymapActionItem("db.compare.schema", "Compare Schema Structure", "Diff schemas between connections", "Database", false, null, null, List.of("⇧⌘D"))),
                new TreeItem<>(new KeymapActionItem("db.export.data", "Export Data…", "Export table or query results", "Database", false, FontAwesomeSolid.FILE_EXPORT, "#4a88c7", Collections.emptyList())),
                new TreeItem<>(new KeymapActionItem("db.import.data", "Import Data…", "Import CSV or SQL dump into database", "Database", false, FontAwesomeSolid.FILE_IMPORT, "#57965c", Collections.emptyList()))
        );
        categories.add(database);

        // 8. Macros
        categories.add(new TreeItem<>(new KeymapActionItem("cat.macros", "Macros", "Recorded keystroke macros", "Macros", true, null, null, null)));

        // 9. Intentions
        categories.add(new TreeItem<>(new KeymapActionItem("cat.intentions", "Intentions", "SQL and code intentions", "Intentions", true, null, null, null)));

        // 10. Quick Lists
        categories.add(new TreeItem<>(new KeymapActionItem("cat.quicklists", "Quick Lists", "Custom action quick lists", "Quick Lists", true, null, null, null)));

        // 11. Plugins
        categories.add(new TreeItem<>(new KeymapActionItem("cat.plugins", "Plugins", "Plugin-contributed actions", "Plugins", true, null, null, null)));

        // 12. Other
        categories.add(new TreeItem<>(new KeymapActionItem("cat.other", "Other", "Miscellaneous actions", "Other", true, null, null, null)));

        return categories;
    }

    /**
     * Shows the Keyboard Shortcut recording modal dialog (Image 2 - media_1790395087139.png).
     */
    public static void showKeyboardShortcutDialog(KeymapActionItem item,
                                                  String currentPreset,
                                                  Map<String, List<String>> customShortcuts,
                                                  Map<String, List<String>> removedShortcuts,
                                                  Runnable onSaved) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Keyboard Shortcut");
        dialog.initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #2b2d30;");
        pane.setPrefWidth(440);

        // Action Path Label: "Find Action... in Main Menu | Help"
        Text nameText = new Text(item.getName());
        nameText.setStyle("-fx-font-weight: bold; -fx-fill: #dfe1e5; -fx-font-size: 13px;");
        Text pathText = new Text(" in " + item.getCategoryPath());
        pathText.setStyle("-fx-fill: #868a91; -fx-font-size: 12px;");
        TextFlow pathFlow = new TextFlow(nameText, pathText);
        pathFlow.setPadding(new Insets(0, 0, 8, 0));

        // First stroke input
        List<String> currentShortcuts = item.getEffectiveShortcuts(currentPreset, customShortcuts, removedShortcuts);
        String initialStroke = currentShortcuts.isEmpty() ? "" : currentShortcuts.get(0);

        TextField firstStrokeField = new TextField(initialStroke);
        firstStrokeField.setPromptText("Press shortcut keys…");
        firstStrokeField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: #dfe1e5; -fx-font-weight: bold; -fx-border-color: #3574f0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 7 10; -fx-font-size: 13px;");
        HBox.setHgrow(firstStrokeField, Priority.ALWAYS);

        Button clearBtn = new Button("✕");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868a91; -fx-cursor: hand; -fx-font-size: 12px;");
        clearBtn.setOnAction(e -> firstStrokeField.clear());

        HBox firstStrokeBox = new HBox(4, firstStrokeField, clearBtn);
        firstStrokeBox.setAlignment(Pos.CENTER_LEFT);

        // Second stroke input
        CheckBox secondStrokeCheck = new CheckBox("Second stroke:");
        secondStrokeCheck.setStyle("-fx-text-fill: -text;");
        secondStrokeCheck.setPrefWidth(120);

        TextField secondStrokeField = new TextField();
        secondStrokeField.setDisable(true);
        secondStrokeField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: #dfe1e5; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10;");
        HBox.setHgrow(secondStrokeField, Priority.ALWAYS);

        secondStrokeCheck.selectedProperty().addListener((obs, oldV, newV) -> secondStrokeField.setDisable(!newV));

        HBox secondStrokeBox = new HBox(8, secondStrokeCheck, secondStrokeField);
        secondStrokeBox.setAlignment(Pos.CENTER_LEFT);

        // Conflict section
        VBox conflictBox = new VBox(4);
        conflictBox.setPadding(new Insets(6, 0, 0, 0));

        Label conflictTitle = new Label("⚠️ Already assigned to:");
        conflictTitle.setStyle("-fx-text-fill: #e0a44c; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label conflictDetail = new Label();
        conflictDetail.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-padding: 0 0 0 16;");
        conflictBox.getChildren().addAll(conflictTitle, conflictDetail);

        Runnable updateConflict = () -> {
            String stroke = firstStrokeField.getText().trim();
            if (stroke.isEmpty()) {
                conflictBox.setVisible(false);
                conflictBox.setManaged(false);
                return;
            }
            String macConflict = KeyStrokeFormatter.checkMacConflict(stroke);
            if (macConflict != null) {
                conflictDetail.setText(macConflict);
                conflictBox.setVisible(true);
                conflictBox.setManaged(true);
                return;
            }
            conflictBox.setVisible(false);
            conflictBox.setManaged(false);
        };

        firstStrokeField.setOnKeyPressed(e -> {
            String stroke = KeyStrokeFormatter.formatFromEvent(e);
            if (stroke != null) {
                firstStrokeField.setText(stroke);
                updateConflict.run();
            }
            e.consume();
        });

        secondStrokeField.setOnKeyPressed(e -> {
            String stroke = KeyStrokeFormatter.formatFromEvent(e);
            if (stroke != null) {
                secondStrokeField.setText(stroke);
            }
            e.consume();
        });

        updateConflict.run();

        VBox content = new VBox(10, pathFlow, firstStrokeBox, secondStrokeBox, conflictBox);
        content.setPadding(new Insets(16));
        pane.setContent(content);

        ButtonType btnTypeOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btnTypeHelp = new ButtonType("?", ButtonBar.ButtonData.HELP);

        pane.getButtonTypes().addAll(btnTypeHelp, btnTypeCancel, btnTypeOk);

        Button okBtn = (Button) pane.lookupButton(btnTypeOk);
        okBtn.setStyle("-fx-background-color: #3574f0; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 5 16;");

        Button cancelBtn = (Button) pane.lookupButton(btnTypeCancel);
        cancelBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 12;");

        Button helpBtn = (Button) pane.lookupButton(btnTypeHelp);
        helpBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #868a91; -fx-border-color: #393b40; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 2 8; -fx-font-weight: bold;");

        dialog.setResultConverter(btnType -> {
            if (btnType == btnTypeOk) {
                String stroke = firstStrokeField.getText().trim();
                if (secondStrokeCheck.isSelected() && !secondStrokeField.getText().trim().isEmpty()) {
                    stroke += ", " + secondStrokeField.getText().trim();
                }
                if (!stroke.isEmpty()) {
                    customShortcuts.put(item.getId(), new ArrayList<>(List.of(stroke)));
                    if (removedShortcuts.containsKey(item.getId())) {
                        removedShortcuts.get(item.getId()).remove(stroke);
                    }
                } else {
                    customShortcuts.remove(item.getId());
                    removedShortcuts.computeIfAbsent(item.getId(), k -> new ArrayList<>())
                            .addAll(item.resolvePresetShortcuts(currentPreset));
                }
                if (onSaved != null) onSaved.run();
                return true;
            }
            return false;
        });

        Platform.runLater(firstStrokeField::requestFocus);
        dialog.showAndWait();
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

    public static VBox buildColorSchemeGeneralPanel(AppSettingsStore.Settings settings,
                                                   Map<String, Object> inputs,
                                                   java.util.function.Consumer<String> navigateTo) {
        // Working state
        String currentScheme = settings.getEditorColorScheme();
        List<String> workingCustomSchemes = new ArrayList<>(settings.getCustomColorSchemes());
        Map<String, Map<String, AppSettingsStore.ColorSchemeAttribute>> workingOverrides = new LinkedHashMap<>();

        if (settings.getColorSchemeOverrides() != null) {
            for (Map.Entry<String, Map<String, AppSettingsStore.ColorSchemeAttribute>> entry : settings.getColorSchemeOverrides().entrySet()) {
                Map<String, AppSettingsStore.ColorSchemeAttribute> inner = new LinkedHashMap<>();
                for (Map.Entry<String, AppSettingsStore.ColorSchemeAttribute> attrEntry : entry.getValue().entrySet()) {
                    inner.put(attrEntry.getKey(), attrEntry.getValue().copy());
                }
                workingOverrides.put(entry.getKey(), inner);
            }
        }

        inputs.put("customColorSchemes", workingCustomSchemes);
        inputs.put("colorSchemeOverrides", workingOverrides);

        // 1. Top Section: Scheme Selector + Gear ⚙ Menu + Change IDE Theme...
        Label schemeLabel = new Label("Scheme:");
        schemeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.setPrefWidth(210);
        schemeCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Runnable refreshSchemesList = () -> {
            String current = schemeCombo.getValue();
            schemeCombo.getItems().clear();
            schemeCombo.getItems().addAll(AppSettingsStore.defaultEditorColorSchemes());
            for (String custom : workingCustomSchemes) {
                if (!schemeCombo.getItems().contains(custom)) {
                    schemeCombo.getItems().add(custom);
                }
            }
            if (current != null && schemeCombo.getItems().contains(current)) {
                schemeCombo.getSelectionModel().select(current);
            } else if (schemeCombo.getItems().contains(settings.getEditorColorScheme())) {
                schemeCombo.getSelectionModel().select(settings.getEditorColorScheme());
            } else if (!schemeCombo.getItems().isEmpty()) {
                schemeCombo.getSelectionModel().select(0);
            }
        };
        refreshSchemesList.run();
        inputs.put("editorColorSchemeCombo", schemeCombo);

        Button gearBtn = new Button();
        gearBtn.setGraphic(Icons.of(FontAwesomeSolid.COG, "#a9b7c6", 13));
        gearBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6;");

        ContextMenu gearMenu = new ContextMenu();
        MenuItem miDuplicate = new MenuItem("Duplicate…");
        MenuItem miRestore = new MenuItem("Restore Defaults");
        MenuItem miExport = new MenuItem("Export…");
        MenuItem miRename = new MenuItem("Rename…");
        MenuItem miDelete = new MenuItem("Delete");
        gearMenu.getItems().addAll(miDuplicate, miRestore, miExport, miRename, miDelete);

        gearBtn.setOnAction(e -> {
            String active = schemeCombo.getValue();
            boolean isCustom = workingCustomSchemes.contains(active);
            miRename.setDisable(!isCustom);
            miDelete.setDisable(!isCustom);
            gearMenu.show(gearBtn, Side.BOTTOM, 0, 0);
        });

        Hyperlink changeThemeLink = new Hyperlink("Change IDE Theme...");
        changeThemeLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-color: transparent; -fx-underline: false;");
        changeThemeLink.setOnMouseEntered(ev -> changeThemeLink.setStyle("-fx-text-fill: #70aeff; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-color: transparent; -fx-underline: true;"));
        changeThemeLink.setOnMouseExited(ev -> changeThemeLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-color: transparent; -fx-underline: false;"));
        changeThemeLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Appearance & Behavior / Appearance");
            }
        });

        Label helpIcon = new Label(" (?)");
        helpIcon.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-cursor: hand;");
        helpIcon.setTooltip(new Tooltip("Click to configure IDE theme colors"));

        HBox topBar = new HBox(10, schemeLabel, schemeCombo, gearBtn, changeThemeLink, helpIcon);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // 2. TreeView of Color Scheme Elements (Left Pane)
        TreeItem<ColorSchemeElement> rootItem = new TreeItem<>(new ColorSchemeElement("root", "Root", "", null, null, null, null));
        rootItem.setExpanded(true);

        populateColorSchemeTree(rootItem);

        TreeView<ColorSchemeElement> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        treeView.setPrefWidth(360);
        treeView.setPrefHeight(260);

        treeView.setCellFactory(tv -> new TreeCell<>() {
            {
                selectedProperty().addListener((obs, wasSel, isSel) -> updateCellStyle());
                hoverProperty().addListener((obs, wasHov, isHov) -> updateCellStyle());
            }

            private void updateCellStyle() {
                if (isEmpty() || getItem() == null) {
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                if (isSelected()) {
                    setStyle("-fx-background-color: #2e436e; -fx-background-radius: 3; -fx-text-fill: #ffffff;");
                } else if (isHover()) {
                    setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 3; -fx-text-fill: #dfe1e5;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #dfe1e5;");
                }
            }

            @Override
            protected void updateItem(ColorSchemeElement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getName());
                    updateCellStyle();
                }
            }
        });

        // 3. Attribute Editor (Right Pane)
        VBox attrEditor = new VBox(10);
        attrEditor.setPadding(new Insets(10, 14, 10, 14));
        attrEditor.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        attrEditor.setPrefWidth(360);
        attrEditor.setPrefHeight(260);

        CheckBox boldCheck = new CheckBox("Bold");
        boldCheck.setStyle("-fx-text-fill: -text;");
        CheckBox italicCheck = new CheckBox("Italic");
        italicCheck.setStyle("-fx-text-fill: -text;");
        Region fontSpacer = new Region();
        HBox.setHgrow(fontSpacer, Priority.ALWAYS);
        HBox fontStyleRow = new HBox(16, fontSpacer, boldCheck, italicCheck);
        fontStyleRow.setAlignment(Pos.CENTER_RIGHT);

        final Runnable[] commitAttrChangesRef = new Runnable[1];

        // Attribute rows helper
        class AttrRow {
            final CheckBox check;
            final Button colorBtn;
            final HBox row;
            String colorHex;

            AttrRow(String labelText) {
                check = new CheckBox(labelText);
                check.setStyle("-fx-text-fill: -text;");
                check.setPrefWidth(140);

                colorBtn = new Button();
                colorBtn.setPrefWidth(90);
                colorBtn.setPrefHeight(24);
                colorBtn.setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row = new HBox(8, check, spacer, colorBtn);
                row.setAlignment(Pos.CENTER_LEFT);

                colorBtn.setOnAction(e -> {
                    if (check.isSelected()) {
                        showColorPickerDialog(colorHex, colorBtn, newHex -> {
                            setColor(newHex);
                            if (commitAttrChangesRef[0] != null) commitAttrChangesRef[0].run();
                        });
                    }
                });

                check.selectedProperty().addListener((obs, oldV, newV) -> {
                    updateButtonState();
                });
            }

            void updateButtonState() {
                boolean sel = check.isSelected();
                colorBtn.setDisable(!sel);
                if (!sel) {
                    colorBtn.setText("");
                    colorBtn.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-background-radius: 3; -fx-border-radius: 3; -fx-cursor: default;");
                } else {
                    setColor(colorHex != null && !colorHex.isBlank() ? colorHex : "FFFFFF");
                }
            }

            void setColor(String hex) {
                this.colorHex = hex != null ? hex.replace("#", "").toUpperCase() : null;
                if (check.isSelected() && this.colorHex != null && !this.colorHex.isBlank()) {
                    colorBtn.setText(this.colorHex);
                    colorBtn.setStyle("-fx-background-color: #" + this.colorHex + "; -fx-text-fill: " +
                            (isColorDark(this.colorHex) ? "#ffffff" : "#000000") +
                            "; -fx-border-color: #393b40; -fx-background-radius: 3; -fx-border-radius: 3; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
                } else {
                    colorBtn.setText("");
                    colorBtn.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-background-radius: 3; -fx-border-radius: 3; -fx-cursor: default;");
                }
            }
        }

        AttrRow fgRow = new AttrRow("Foreground");
        AttrRow bgRow = new AttrRow("Background");
        AttrRow errorStripeRow = new AttrRow("Error stripe mark");
        AttrRow effectsRow = new AttrRow("Effects");

        ComboBox<String> effectTypeCombo = new ComboBox<>();
        effectTypeCombo.getItems().addAll("Underscored", "Bold Underscored", "Underwaved", "Strikeout", "Bordered", "Dotted line");
        effectTypeCombo.getSelectionModel().select("Underscored");
        effectTypeCombo.setPrefWidth(125);
        effectTypeCombo.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        Region effectSpacer = new Region();
        HBox.setHgrow(effectSpacer, Priority.ALWAYS);
        HBox effectComboRow = new HBox(effectSpacer, effectTypeCombo);
        effectComboRow.setAlignment(Pos.CENTER_RIGHT);
        VBox effectsContainer = new VBox(6, effectsRow.row, effectComboRow);

        effectsRow.check.selectedProperty().addListener((obs, oldV, newV) -> {
            effectTypeCombo.setDisable(!newV);
        });

        // Inheritance row
        CheckBox inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: -text;");

        Hyperlink inheritLink = new Hyperlink();
        inheritLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent;");

        VBox inheritBox = new VBox(4, inheritCheck, inheritLink);
        inheritBox.setPadding(new Insets(6, 0, 0, 0));

        inheritCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            boolean inh = Boolean.TRUE.equals(newV);
            boldCheck.setDisable(inh);
            italicCheck.setDisable(inh);
            fgRow.check.setDisable(inh);
            fgRow.colorBtn.setDisable(inh || !fgRow.check.isSelected());
            bgRow.check.setDisable(inh);
            bgRow.colorBtn.setDisable(inh || !bgRow.check.isSelected());
            errorStripeRow.check.setDisable(inh);
            errorStripeRow.colorBtn.setDisable(inh || !errorStripeRow.check.isSelected());
            effectsRow.check.setDisable(inh);
            effectsRow.colorBtn.setDisable(inh || !effectsRow.check.isSelected());
            effectTypeCombo.setDisable(inh || !effectsRow.check.isSelected());
        });

        attrEditor.getChildren().addAll(fontStyleRow, fgRow.row, bgRow.row, errorStripeRow.row, effectsContainer, inheritBox);

        final String[] currentSelectedElementId = new String[]{"code.line_number"};

        // 4. Live Code Preview (Bottom Pane)
        VBox previewPane = new VBox();
        previewPane.setStyle("-fx-background-color: #1e1f22; -fx-padding: 8 0 8 0;");

        HBox splitTop = new HBox(10, treeView, attrEditor);
        HBox.setHgrow(treeView, Priority.ALWAYS);
        HBox.setHgrow(attrEditor, Priority.ALWAYS);

        ScrollPane previewScroll = new ScrollPane(previewPane);
        previewScroll.setFitToWidth(true);
        previewScroll.setStyle("-fx-background: #1e1f22; -fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        previewScroll.setPrefHeight(230);
        VBox.setVgrow(previewScroll, Priority.ALWAYS);

        // Live preview builder and interactive token clicks
        Runnable refreshPreview = () -> {
            previewPane.getChildren().clear();
            String activeScheme = schemeCombo.getValue();

            // Resolve attributes for key preview elements
            AppSettingsStore.ColorSchemeAttribute lineNumAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "code.line_number", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute todoAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "code.todo", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute linkAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "hyperlinks.unfollowed", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute refLinkAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "hyperlinks.reference", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute inactiveLinkAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "hyperlinks.inactive", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute searchResultAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "search.result", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute reassignedAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "identifiers.reassigned_local_variable", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute foldedAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "text.folded", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute foldedHighAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "text.folded_highlighted", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute deletedAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "text.deleted", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute activeTemplateAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "templates.active", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute inactiveTemplateAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "templates.inactive", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute varTemplateAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "templates.variable", workingOverrides);
            AppSettingsStore.ColorSchemeAttribute injectedAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "code.injected_fragment", workingOverrides);

            String lineNumColor = (lineNumAttr.foregroundEnabled && lineNumAttr.foreground != null)
                    ? "#" + lineNumAttr.foreground : "#4B5059";

            // Helper to render preview row with line number, content, and right error stripe
            class PreviewRowBuilder {
                HBox build(int lineNum, Node content, String stripeColorHex, String elementId) {
                    HBox lineRow = new HBox(8);
                    lineRow.setAlignment(Pos.CENTER_LEFT);
                    lineRow.setPadding(new Insets(1, 10, 1, 10));

                    boolean isCurrent = currentSelectedElementId[0] != null && currentSelectedElementId[0].equals(elementId);
                    if (isCurrent) {
                        lineRow.setStyle("-fx-background-color: #26282E;");
                    } else {
                        lineRow.setStyle("-fx-background-color: transparent;");
                    }

                    // Gutter line number
                    Label numLabel = new Label(String.format("%2d", lineNum));
                    numLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: " + lineNumColor + "; -fx-cursor: hand;");
                    numLabel.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "code.line_number", currentSelectedElementId, previewScroll));
                    numLabel.setPrefWidth(26);

                    // Content spacer
                    HBox.setHgrow(content, Priority.ALWAYS);

                    // Error stripe
                    Region stripe = new Region();
                    stripe.setPrefWidth(4);
                    stripe.setPrefHeight(14);
                    if (stripeColorHex != null && !stripeColorHex.isBlank()) {
                        stripe.setStyle("-fx-background-color: #" + stripeColorHex.replace("#", "") + "; -fx-background-radius: 2;");
                    } else {
                        stripe.setStyle("-fx-background-color: transparent;");
                    }

                    lineRow.getChildren().addAll(numLabel, content, stripe);
                    return lineRow;
                }
            }

            PreviewRowBuilder pb = new PreviewRowBuilder();

            // Line 1: //TODO: Visit JB Web resources:
            Label todoLbl = new Label("//TODO: Visit JB Web resources:");
            String todoFg = (todoAttr.foregroundEnabled && todoAttr.foreground != null) ? "#" + todoAttr.foreground : "#A8C023";
            todoLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: " + todoFg +
                    "; -fx-font-style: italic; -fx-cursor: hand;");
            todoLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "code.todo", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(1, todoLbl, todoAttr.errorStripeEnabled ? todoAttr.errorStripe : "73AD2B", "code.todo"));

            // Line 2: JetBrains Home Page: http://www.jetbrains.com
            Label jbHome = new Label("JetBrains Home Page: ");
            jbHome.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            Label link1 = new Label("http://www.jetbrains.com");
            String linkFg = (linkAttr.foregroundEnabled && linkAttr.foreground != null) ? "#" + linkAttr.foreground : "#287BDE";
            link1.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: " + linkFg + "; -fx-underline: true; -fx-cursor: hand;");
            link1.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "hyperlinks.unfollowed", currentSelectedElementId, previewScroll));
            HBox l2 = new HBox(jbHome, link1);
            l2.setAlignment(Pos.CENTER_LEFT);
            previewPane.getChildren().add(pb.build(2, l2, "287BDE", "hyperlinks.unfollowed"));

            // Line 3: JetBrains Developer Community: https://www.jetbrains.com/devnet
            Label jbDev = new Label("JetBrains Developer Community: ");
            jbDev.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            Label link2 = new Label("https://www.jetbrains.com/devnet");
            link2.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: " + linkFg + "; -fx-underline: true; -fx-cursor: hand;");
            link2.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "hyperlinks.unfollowed", currentSelectedElementId, previewScroll));
            HBox l3 = new HBox(jbDev, link2);
            l3.setAlignment(Pos.CENTER_LEFT);
            previewPane.getChildren().add(pb.build(3, l3, "287BDE", "hyperlinks.unfollowed"));

            // Line 4: ReferenceHyperlink
            Label refLink = new Label("ReferenceHyperlink");
            String refLinkFg = (refLinkAttr.foregroundEnabled && refLinkAttr.foreground != null) ? "#" + refLinkAttr.foreground : "#589DF6";
            refLink.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: " + refLinkFg + "; -fx-font-weight: bold; -fx-underline: true; -fx-cursor: hand;");
            refLink.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "hyperlinks.reference", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(4, refLink, "589DF6", "hyperlinks.reference"));

            // Line 5: Inactive hyperlink in code: "http://jetbrains.com"
            Label inactPrefix = new Label("Inactive hyperlink in code: ");
            inactPrefix.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            Label inactLink = new Label("\"http://jetbrains.com\"");
            String inactEff = (inactiveLinkAttr.effectEnabled && inactiveLinkAttr.effectColor != null)
                    ? "#" + inactiveLinkAttr.effectColor : "#6B6C73";
            inactLink.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #868A91; -fx-border-color: transparent transparent " + inactEff + " transparent; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
            inactLink.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "hyperlinks.inactive", currentSelectedElementId, previewScroll));
            HBox l5 = new HBox(inactPrefix, inactLink);
            l5.setAlignment(Pos.CENTER_LEFT);
            previewPane.getChildren().add(pb.build(5, l5, null, "hyperlinks.inactive"));

            // Line 6: Blank
            previewPane.getChildren().add(pb.build(6, new Label(""), null, null));

            // Line 7: Search:
            Label searchLbl = new Label("Search:");
            searchLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            previewPane.getChildren().add(pb.build(7, searchLbl, null, null));

            // Line 8:   result = "text, text, text";
            Label resIndent = new Label("  ");
            Label resToken = new Label("result");
            String searchBg = (searchResultAttr.backgroundEnabled && searchResultAttr.background != null) ? "#" + searchResultAttr.background : "#32593D";
            resToken.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-background-color: " + searchBg +
                    "; -fx-text-fill: #dfe1e5; -fx-padding: 0 2; -fx-background-radius: 2; -fx-cursor: hand;");
            resToken.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "search.result", currentSelectedElementId, previewScroll));
            Label eqToken = new Label(" = \"");
            eqToken.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");

            Label t1 = new Label("text");
            t1.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-background-color: #2e596b; -fx-text-fill: #ffffff; -fx-padding: 0 2; -fx-background-radius: 2;");
            Label comma1 = new Label(", ");
            comma1.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            Label t2 = new Label("text");
            t2.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-background-color: #2e596b; -fx-text-fill: #ffffff; -fx-padding: 0 2; -fx-background-radius: 2;");
            Label comma2 = new Label(", ");
            comma2.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            Label t3 = new Label("text");
            t3.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-background-color: #2e596b; -fx-text-fill: #ffffff; -fx-padding: 0 2; -fx-background-radius: 2;");
            Label endQuote = new Label("\";");
            endQuote.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");

            HBox l8 = new HBox(resIndent, resToken, eqToken, t1, comma1, t2, comma2, t3, endQuote);
            l8.setAlignment(Pos.CENTER_LEFT);
            previewPane.getChildren().add(pb.build(8, l8, searchResultAttr.errorStripeEnabled ? searchResultAttr.errorStripe : "57965C", "search.result"));

            // Line 9:   i = result
            Label l9Indent = new Label("  ");
            Label iToken = new Label("i");
            if (reassignedAttr.effectEnabled && "Bordered".equals(reassignedAttr.effectType)) {
                iToken.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #dfe1e5; -fx-border-color: #3574F0; -fx-border-radius: 2; -fx-padding: 0 2; -fx-cursor: hand;");
            } else {
                iToken.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #dfe1e5; -fx-cursor: hand;");
            }
            iToken.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "identifiers.reassigned_local_variable", currentSelectedElementId, previewScroll));

            Label l9Eq = new Label(" = ");
            l9Eq.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            Label resToken2 = new Label("result");
            resToken2.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-background-color: " + searchBg +
                    "; -fx-text-fill: #dfe1e5; -fx-padding: 0 2; -fx-background-radius: 2; -fx-cursor: hand;");
            resToken2.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "search.result", currentSelectedElementId, previewScroll));
            HBox l9 = new HBox(l9Indent, iToken, l9Eq, resToken2);
            l9.setAlignment(Pos.CENTER_LEFT);
            previewPane.getChildren().add(pb.build(9, l9, null, "identifiers.reassigned_local_variable"));

            // Line 10:   return i;
            Label l10 = new Label("  return i;");
            l10.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            previewPane.getChildren().add(pb.build(10, l10, null, null));

            // Line 11: Blank
            previewPane.getChildren().add(pb.build(11, new Label(""), null, null));

            // Line 12: Folded text
            Label foldedLbl = new Label("Folded text");
            String foldBg = (foldedAttr.backgroundEnabled && foldedAttr.background != null) ? "#" + foldedAttr.background : "#3A3A3A";
            foldedLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-background-color: " + foldBg +
                    "; -fx-text-fill: #8c8c8c; -fx-padding: 0 6; -fx-background-radius: 3; -fx-cursor: hand;");
            foldedLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "text.folded", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(12, foldedLbl, "8C8C8C", "text.folded"));

            // Line 13: Folded text with highlighting
            Label foldedHighLbl = new Label("Folded text with highlighting");
            foldedHighLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-background-color: " + foldBg +
                    "; -fx-text-fill: #8c8c8c; -fx-border-color: #3574F0; -fx-border-radius: 3; -fx-padding: 0 6; -fx-background-radius: 3; -fx-cursor: hand;");
            foldedHighLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "text.folded_highlighted", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(13, foldedHighLbl, null, "text.folded_highlighted"));

            // Line 14: Deleted text
            Label delLbl = new Label("Deleted text");
            String delFg = (deletedAttr.foregroundEnabled && deletedAttr.foreground != null) ? "#" + deletedAttr.foreground : "#CC666E";
            delLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: " + delFg +
                    "; -fx-border-color: " + delFg + " transparent transparent transparent; -fx-border-style: dashed; -fx-cursor: hand;");
            delLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "text.deleted", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(14, delLbl, "CC666E", "text.deleted"));

            // Line 15: Live template: active inactive $VARIABLE$
            Label tmplPrefix = new Label("Live template: ");
            tmplPrefix.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            Label tmplActive = new Label("active");
            tmplActive.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #dfe1e5; -fx-border-color: #3574F0; -fx-border-radius: 2; -fx-padding: 0 4; -fx-cursor: hand;");
            tmplActive.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "templates.active", currentSelectedElementId, previewScroll));
            Label tmplSpace = new Label(" ");
            Label tmplInactive = new Label("inactive");
            tmplInactive.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #dfe1e5; -fx-border-color: #4E5157; -fx-border-radius: 2; -fx-padding: 0 4; -fx-cursor: hand;");
            tmplInactive.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "templates.inactive", currentSelectedElementId, previewScroll));
            Label tmplVar = new Label(" $VARIABLE$");
            String varFg = (varTemplateAttr.foregroundEnabled && varTemplateAttr.foreground != null) ? "#" + varTemplateAttr.foreground : "#9876AA";
            tmplVar.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: " + varFg + "; -fx-font-style: italic; -fx-cursor: hand;");
            tmplVar.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "templates.variable", currentSelectedElementId, previewScroll));

            HBox l15 = new HBox(tmplPrefix, tmplActive, tmplSpace, tmplInactive, tmplVar);
            l15.setAlignment(Pos.CENTER_LEFT);
            previewPane.getChildren().add(pb.build(15, l15, null, "templates.active"));

            // Line 16: Injected language: \.(gif|jpg|png)$
            Label injPrefix = new Label("Injected language: ");
            injPrefix.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            Label injContent = new Label("\\.(gif|jpg|png)$");
            String injBg = (injectedAttr.backgroundEnabled && injectedAttr.background != null) ? "#" + injectedAttr.background : "#363636";
            injContent.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-background-color: " + injBg + "; -fx-text-fill: #dfe1e5; -fx-padding: 0 4; -fx-background-radius: 2; -fx-cursor: hand;");
            injContent.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "code.injected_fragment", currentSelectedElementId, previewScroll));
            HBox l16 = new HBox(injPrefix, injContent);
            l16.setAlignment(Pos.CENTER_LEFT);
            previewPane.getChildren().add(pb.build(16, l16, null, "code.injected_fragment"));

            // Line 17: Blank
            previewPane.getChildren().add(pb.build(17, new Label(""), null, null));

            // Line 18: Code Inspections:
            Label inspectLbl = new Label("Code Inspections:");
            inspectLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;");
            previewPane.getChildren().add(pb.build(18, inspectLbl, null, null));

            // Line 19:   Warning
            AppSettingsStore.ColorSchemeAttribute warnAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "errors.warning", workingOverrides);
            Label warnLbl = new Label("  Warning");
            String warnEff = (warnAttr.effectColor != null) ? "#" + warnAttr.effectColor : "#F4AF3D";
            warnLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;" +
                    (warnAttr.effectEnabled ? " -fx-border-color: transparent transparent " + warnEff + " transparent; -fx-border-width: 0 0 1.5 0; -fx-border-style: dashed;" : "") +
                    "; -fx-cursor: hand;");
            warnLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "errors.warning", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(19, warnLbl, warnAttr.errorStripeEnabled ? warnAttr.errorStripe : "F4AF3D", "errors.warning"));

            // Line 20:   Weak warning
            AppSettingsStore.ColorSchemeAttribute weakWarnAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "errors.weak_warning", workingOverrides);
            Label weakWarnLbl = new Label("  Weak warning");
            String weakEff = (weakWarnAttr.effectColor != null) ? "#" + weakWarnAttr.effectColor : "#756D56";
            weakWarnLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6;" +
                    (weakWarnAttr.effectEnabled ? " -fx-border-color: transparent transparent " + weakEff + " transparent; -fx-border-width: 0 0 1 0; -fx-border-style: dashed;" : "") +
                    "; -fx-cursor: hand;");
            weakWarnLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "errors.weak_warning", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(20, weakWarnLbl, weakWarnAttr.errorStripeEnabled ? weakWarnAttr.errorStripe : "756D56", "errors.weak_warning"));

            // Line 21:   Deprecated symbol
            AppSettingsStore.ColorSchemeAttribute depAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "errors.deprecated", workingOverrides);
            Text depText = new Text("  Deprecated symbol");
            depText.setFont(Font.font("JetBrains Mono", 12));
            depText.setFill(javafx.scene.paint.Color.web("#868A91"));
            depText.setStrikethrough(depAttr.effectEnabled && "Strikeout".equals(depAttr.effectType));
            HBox depBox = new HBox(depText);
            depBox.setAlignment(Pos.CENTER_LEFT);
            depBox.setStyle("-fx-cursor: hand;");
            depBox.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "errors.deprecated", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(21, depBox, "868A91", "errors.deprecated"));

            // Line 22:   Deprecated symbol marked for removal
            AppSettingsStore.ColorSchemeAttribute depRemAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "errors.deprecated_marked_for_removal", workingOverrides);
            Text depRemText = new Text("  Deprecated symbol marked for removal");
            depRemText.setFont(Font.font("JetBrains Mono", 12));
            String depRemColor = (depRemAttr.effectColor != null) ? "#" + depRemAttr.effectColor : "#F75464";
            depRemText.setFill(javafx.scene.paint.Color.web(depRemColor));
            depRemText.setStrikethrough(depRemAttr.effectEnabled && "Strikeout".equals(depRemAttr.effectType));
            HBox depRemBox = new HBox(depRemText);
            depRemBox.setAlignment(Pos.CENTER_LEFT);
            depRemBox.setStyle("-fx-cursor: hand;");
            depRemBox.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "errors.deprecated_marked_for_removal", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(22, depRemBox, depRemAttr.effectEnabled ? depRemAttr.effectColor : "F75464", "errors.deprecated_marked_for_removal"));

            // Line 23:   Unused symbol
            AppSettingsStore.ColorSchemeAttribute unusedAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "errors.unused", workingOverrides);
            String unusedFg = (unusedAttr.foregroundEnabled && unusedAttr.foreground != null) ? "#" + unusedAttr.foreground : "#72737A";
            Label unusedLbl = new Label("  Unused symbol");
            unusedLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: " + unusedFg + "; -fx-cursor: hand;");
            unusedLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "errors.unused", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(23, unusedLbl, null, "errors.unused"));

            // Line 24:   Unknown symbol
            AppSettingsStore.ColorSchemeAttribute unkAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "errors.unknown_symbol", workingOverrides);
            String unkFg = (unkAttr.foregroundEnabled && unkAttr.foreground != null) ? "#" + unkAttr.foreground : "#BC3F3C";
            Label unkLbl = new Label("  Unknown symbol");
            unkLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: " + unkFg +
                    "; -fx-border-color: transparent transparent #BC3F3C transparent; -fx-border-width: 0 0 1.5 0; -fx-border-style: dashed; -fx-cursor: hand;");
            unkLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "errors.unknown_symbol", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(24, unkLbl, unkAttr.errorStripeEnabled ? unkAttr.errorStripe : "BC3F3C", "errors.unknown_symbol"));

            // Line 25:   Runtime problem
            AppSettingsStore.ColorSchemeAttribute rtAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "errors.runtime_problem", workingOverrides);
            Label rtLbl = new Label("  Runtime problem");
            rtLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6; -fx-border-color: transparent transparent #E05555 transparent; -fx-border-width: 0 0 1.5 0; -fx-border-style: dashed; -fx-cursor: hand;");
            rtLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "errors.runtime_problem", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(25, rtLbl, rtAttr.errorStripeEnabled ? rtAttr.errorStripe : "E05555", "errors.runtime_problem"));

            // Line 26:   Problem from server
            AppSettingsStore.ColorSchemeAttribute probAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "errors.problem_from_server", workingOverrides);
            Label probLbl = new Label("  Problem from server");
            probLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #a9b7c6; -fx-border-color: transparent transparent #F4AF3D transparent; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
            probLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "errors.problem_from_server", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(26, probLbl, probAttr.errorStripeEnabled ? probAttr.errorStripe : null, "errors.problem_from_server"));

            // Line 27:   Duplicate from server
            AppSettingsStore.ColorSchemeAttribute dupAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, "errors.duplicate_from_server", workingOverrides);
            String dupBg = (dupAttr.backgroundEnabled && dupAttr.background != null) ? "#" + dupAttr.background : "#544630";
            Label dupLbl = new Label("  Duplicate from server");
            dupLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-background-color: " + dupBg + "; -fx-text-fill: #dfe1e5; -fx-padding: 0 4; -fx-background-radius: 2; -fx-cursor: hand;");
            dupLbl.setOnMouseClicked(e -> selectColorSchemeElement(treeView, rootItem, "errors.duplicate_from_server", currentSelectedElementId, previewScroll));
            previewPane.getChildren().add(pb.build(27, dupLbl, null, "errors.duplicate_from_server"));
        };

        // TreeView selection listener updating the Attribute Editor
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || newV.getValue() == null || newV.getValue().getDefaultAttr() == null) {
                attrEditor.setDisable(true);
                return;
            }
            attrEditor.setDisable(false);
            ColorSchemeElement el = newV.getValue();
            currentSelectedElementId[0] = el.getId();
            String activeScheme = schemeCombo.getValue();

            AppSettingsStore.ColorSchemeAttribute currentAttr =
                    ColorSchemeModel.resolveAttribute(activeScheme, el.getId(), workingOverrides);

            boldCheck.setSelected(currentAttr.bold);
            italicCheck.setSelected(currentAttr.italic);

            fgRow.check.setSelected(currentAttr.foregroundEnabled);
            fgRow.setColor(currentAttr.foreground);

            bgRow.check.setSelected(currentAttr.backgroundEnabled);
            bgRow.setColor(currentAttr.background);

            errorStripeRow.check.setSelected(currentAttr.errorStripeEnabled);
            errorStripeRow.setColor(currentAttr.errorStripe);

            effectsRow.check.setSelected(currentAttr.effectEnabled);
            effectsRow.setColor(currentAttr.effectColor);
            if (currentAttr.effectType != null) {
                effectTypeCombo.getSelectionModel().select(currentAttr.effectType);
            }
            effectTypeCombo.setDisable(!currentAttr.effectEnabled);

            if (el.hasInheritance()) {
                inheritBox.setVisible(true);
                inheritBox.setManaged(true);
                inheritCheck.setSelected(currentAttr.inherit);
                inheritLink.setText(el.getInheritFromDisplay());
                inheritLink.setOnAction(ev -> selectColorSchemeElement(treeView, rootItem, el.getInheritFromKey(), currentSelectedElementId, previewScroll));
            } else {
                inheritBox.setVisible(false);
                inheritBox.setManaged(false);
            }

            boolean inh = currentAttr.inherit && el.hasInheritance();
            boldCheck.setDisable(inh);
            italicCheck.setDisable(inh);
            fgRow.check.setDisable(inh);
            fgRow.colorBtn.setDisable(inh || !currentAttr.foregroundEnabled);
            bgRow.check.setDisable(inh);
            bgRow.colorBtn.setDisable(inh || !currentAttr.backgroundEnabled);
            errorStripeRow.check.setDisable(inh);
            errorStripeRow.colorBtn.setDisable(inh || !currentAttr.errorStripeEnabled);
            effectsRow.check.setDisable(inh);
            effectsRow.colorBtn.setDisable(inh || !currentAttr.effectEnabled);
            effectTypeCombo.setDisable(inh || !currentAttr.effectEnabled);

            refreshPreview.run();

            if (el.getId().startsWith("errors.")) {
                previewScroll.setVvalue(1.0);
            } else if (el.getId().startsWith("code.") || el.getId().startsWith("hyperlinks.")) {
                previewScroll.setVvalue(0.0);
            }
        });

        // Attribute change listener that saves overrides and refreshes preview
        commitAttrChangesRef[0] = () -> {
            TreeItem<ColorSchemeElement> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel == null || sel.getValue() == null || sel.getValue().getDefaultAttr() == null) return;
            ColorSchemeElement el = sel.getValue();
            String activeScheme = schemeCombo.getValue();

            AppSettingsStore.ColorSchemeAttribute updated = new AppSettingsStore.ColorSchemeAttribute(
                    boldCheck.isSelected(),
                    italicCheck.isSelected(),
                    fgRow.colorHex,
                    fgRow.check.isSelected(),
                    bgRow.colorHex,
                    bgRow.check.isSelected(),
                    errorStripeRow.colorHex,
                    errorStripeRow.check.isSelected(),
                    effectsRow.colorHex,
                    effectsRow.check.isSelected(),
                    effectTypeCombo.getValue(),
                    inheritCheck.isSelected(),
                    el.getInheritFromKey()
            );

            workingOverrides.computeIfAbsent(activeScheme, k -> new LinkedHashMap<>()).put(el.getId(), updated);
            refreshPreview.run();
        };

        boldCheck.setOnAction(e -> commitAttrChangesRef[0].run());
        italicCheck.setOnAction(e -> commitAttrChangesRef[0].run());
        fgRow.check.setOnAction(e -> commitAttrChangesRef[0].run());
        bgRow.check.setOnAction(e -> commitAttrChangesRef[0].run());
        errorStripeRow.check.setOnAction(e -> commitAttrChangesRef[0].run());
        effectsRow.check.setOnAction(e -> commitAttrChangesRef[0].run());
        effectTypeCombo.setOnAction(e -> commitAttrChangesRef[0].run());
        inheritCheck.setOnAction(e -> {
            commitAttrChangesRef[0].run();
            TreeItem<ColorSchemeElement> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null && sel.getValue().hasInheritance()) {
                ColorSchemeElement el = sel.getValue();
                String activeScheme = schemeCombo.getValue();
                AppSettingsStore.ColorSchemeAttribute resolved =
                        ColorSchemeModel.resolveAttribute(activeScheme, el.getId(), workingOverrides);
                boldCheck.setSelected(resolved.bold);
                italicCheck.setSelected(resolved.italic);
                fgRow.check.setSelected(resolved.foregroundEnabled);
                fgRow.setColor(resolved.foreground);
                bgRow.check.setSelected(resolved.backgroundEnabled);
                bgRow.setColor(resolved.background);
                errorStripeRow.check.setSelected(resolved.errorStripeEnabled);
                errorStripeRow.setColor(resolved.errorStripe);
                effectsRow.check.setSelected(resolved.effectEnabled);
                effectsRow.setColor(resolved.effectColor);
                if (resolved.effectType != null) {
                    effectTypeCombo.getSelectionModel().select(resolved.effectType);
                }
            }
        });

        // Gear Menu Actions
        miDuplicate.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog(schemeCombo.getValue() + " copy");
            tid.setTitle("Duplicate Color Scheme");
            tid.setHeaderText("Create a copy of '" + schemeCombo.getValue() + "'");
            tid.setContentText("Scheme name:");
            tid.showAndWait().ifPresent(name -> {
                String trimmed = name.trim();
                if (!trimmed.isEmpty() && !workingCustomSchemes.contains(trimmed)) {
                    workingCustomSchemes.add(trimmed);
                    refreshSchemesList.run();
                    schemeCombo.getSelectionModel().select(trimmed);
                }
            });
        });

        miRestore.setOnAction(e -> {
            String active = schemeCombo.getValue();
            workingOverrides.remove(active);
            TreeItem<ColorSchemeElement> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                treeView.getSelectionModel().clearSelection();
                treeView.getSelectionModel().select(sel);
            }
            refreshPreview.run();
        });

        miDelete.setOnAction(e -> {
            String active = schemeCombo.getValue();
            if (workingCustomSchemes.contains(active)) {
                workingCustomSchemes.remove(active);
                workingOverrides.remove(active);
                refreshSchemesList.run();
            }
        });

        miRename.setOnAction(e -> {
            String active = schemeCombo.getValue();
            if (workingCustomSchemes.contains(active)) {
                TextInputDialog tid = new TextInputDialog(active);
                tid.setTitle("Rename Color Scheme");
                tid.setHeaderText("Enter new name for '" + active + "'");
                tid.setContentText("New name:");
                tid.showAndWait().ifPresent(name -> {
                    String trimmed = name.trim();
                    if (!trimmed.isEmpty()) {
                        int idx = workingCustomSchemes.indexOf(active);
                        if (idx >= 0) workingCustomSchemes.set(idx, trimmed);
                        Map<String, AppSettingsStore.ColorSchemeAttribute> moved = workingOverrides.remove(active);
                        if (moved != null) workingOverrides.put(trimmed, moved);
                        refreshSchemesList.run();
                        schemeCombo.getSelectionModel().select(trimmed);
                    }
                });
            }
        });

        schemeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            TreeItem<ColorSchemeElement> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                treeView.getSelectionModel().clearSelection();
                treeView.getSelectionModel().select(sel);
            }
            refreshPreview.run();
        });

        // Select first leaf item (Code > Line number)
        selectColorSchemeElement(treeView, rootItem, "code.line_number", currentSelectedElementId, previewScroll);

        // Initial preview render
        refreshPreview.run();

        VBox panel = new VBox(10, topBar, splitTop, previewScroll);
        VBox.setVgrow(splitTop, Priority.NEVER);
        VBox.setVgrow(previewScroll, Priority.ALWAYS);
        panel.setPadding(new Insets(4, 8, 12, 8));
        return panel;
    }

    private static void populateColorSchemeTree(TreeItem<ColorSchemeElement> root) {
        List<String> categories = ColorSchemeModel.getCategories();
        for (String cat : categories) {
            TreeItem<ColorSchemeElement> catItem = new TreeItem<>(new ColorSchemeElement("cat." + cat, cat, cat, null, null, null, null));
            List<ColorSchemeElement> elements = ColorSchemeModel.getElementsByCategory(cat);

            Map<String, TreeItem<ColorSchemeElement>> subCatMap = new LinkedHashMap<>();

            for (ColorSchemeElement el : elements) {
                if (el.getSubCategory() != null) {
                    TreeItem<ColorSchemeElement> subItem = subCatMap.computeIfAbsent(el.getSubCategory(),
                            sc -> new TreeItem<>(new ColorSchemeElement("sub." + sc, sc, cat, sc, null, null, null)));
                    subItem.getChildren().add(new TreeItem<>(el));
                } else {
                    catItem.getChildren().add(new TreeItem<>(el));
                }
            }

            for (TreeItem<ColorSchemeElement> subItem : subCatMap.values()) {
                catItem.getChildren().add(subItem);
            }

            root.getChildren().add(catItem);
        }
    }

    private static void selectColorSchemeElement(TreeView<ColorSchemeElement> treeView,
                                                 TreeItem<ColorSchemeElement> root,
                                                 String elementId,
                                                 String[] currentSelectedElementId,
                                                 ScrollPane previewScroll) {
        if (elementId == null || root == null) return;
        if (currentSelectedElementId != null) {
            currentSelectedElementId[0] = elementId;
        }
        TreeItem<ColorSchemeElement> target = findColorSchemeItem(root, elementId);
        if (target != null) {
            TreeItem<ColorSchemeElement> p = target.getParent();
            while (p != null) {
                p.setExpanded(true);
                p = p.getParent();
            }
            treeView.getSelectionModel().select(target);
            int row = treeView.getRow(target);
            if (row >= 0) treeView.scrollTo(row);
        }
        if (previewScroll != null) {
            if (elementId.startsWith("errors.")) {
                previewScroll.setVvalue(1.0);
            } else if (elementId.startsWith("code.") || elementId.startsWith("hyperlinks.")) {
                previewScroll.setVvalue(0.0);
            }
        }
    }

    private static void selectColorSchemeElement(TreeView<ColorSchemeElement> treeView,
                                                 TreeItem<ColorSchemeElement> root,
                                                 String elementId) {
        selectColorSchemeElement(treeView, root, elementId, null, null);
    }

    private static TreeItem<ColorSchemeElement> findColorSchemeItem(TreeItem<ColorSchemeElement> current, String id) {
        if (current == null || id == null) return null;
        ColorSchemeElement resolvedEl = ColorSchemeModel.getElement(id);
        String targetId = resolvedEl != null ? resolvedEl.getId() : id;
        if (current.getValue() != null && targetId.equals(current.getValue().getId())) {
            return current;
        }
        for (TreeItem<ColorSchemeElement> child : current.getChildren()) {
            TreeItem<ColorSchemeElement> found = findColorSchemeItem(child, targetId);
            if (found != null) return found;
        }
        return null;
    }

    private static boolean isColorDark(String hex) {
        if (hex == null || hex.length() < 6) return true;
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            double luminance = (0.299 * r + 0.587 * g + 0.114 * b);
            return luminance < 128;
        } catch (Exception e) {
            return true;
        }
    }

    private static void showColorPickerDialog(String currentHex, Node anchor, java.util.function.Consumer<String> onSelected) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Choose Color");
        dialog.initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text;");
        pane.setPrefWidth(300);

        Label prompt = new Label("Enter Hex Color (e.g. 4B5059 or #4B5059):");
        prompt.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");

        TextField hexInput = new TextField(currentHex != null ? currentHex : "DFE1E5");
        hexInput.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: #dfe1e5; -fx-border-color: #3574f0; -fx-font-weight: bold;");

        Region colorPreview = new Region();
        colorPreview.setPrefWidth(36);
        colorPreview.setPrefHeight(26);
        colorPreview.setStyle("-fx-background-color: #" + hexInput.getText().replace("#", "") + "; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-background-radius: 3;");

        hexInput.textProperty().addListener((obs, oldV, newV) -> {
            String clean = newV != null ? newV.replace("#", "").trim() : "";
            if (clean.matches("[0-9a-fA-F]{6}")) {
                colorPreview.setStyle("-fx-background-color: #" + clean + "; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-background-radius: 3;");
            }
        });

        HBox inputRow = new HBox(8, hexInput, colorPreview);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        // Palette presets
        Label paletteLabel = new Label("Standard Palette:");
        paletteLabel.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px;");

        FlowPane palette = new FlowPane(6, 6);
        List<String> swatches = List.of(
                "4B5059", "A4A3A3", "DFE1E5", "2B2D30", "1E1F22", "3574F0",
                "4083C9", "57965C", "A8C023", "F4AF3D", "BC3F3C", "9876AA",
                "FFC66D", "CC666E", "FFFFFF", "000000"
        );
        for (String sw : swatches) {
            Button swBtn = new Button();
            swBtn.setPrefSize(20, 20);
            swBtn.setStyle("-fx-background-color: #" + sw + "; -fx-border-color: #393b40; -fx-border-radius: 2; -fx-background-radius: 2; -fx-cursor: hand;");
            swBtn.setOnAction(ev -> {
                hexInput.setText(sw);
                colorPreview.setStyle("-fx-background-color: #" + sw + "; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-background-radius: 3;");
            });
            palette.getChildren().add(swBtn);
        }

        VBox content = new VBox(10, prompt, inputRow, paletteLabel, palette);
        content.setPadding(new Insets(14));
        pane.setContent(content);

        ButtonType btnOk = new ButtonType("Choose", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnCancel, btnOk);

        dialog.setResultConverter(bt -> {
            if (bt == btnOk) {
                String clean = hexInput.getText() != null ? hexInput.getText().replace("#", "").trim().toUpperCase() : "";
                return clean;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(hex -> {
            if (hex != null && !hex.isBlank() && onSelected != null) {
                onSelected.accept(hex);
            }
        });
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
        if (inputs.containsKey("editorColorSchemeCombo")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("editorColorSchemeCombo");
            if (cb.getValue() != null) settings.setEditorColorScheme(cb.getValue());
        }
        if (inputs.containsKey("customColorSchemes")) {
            List<String> list = (List<String>) inputs.get("customColorSchemes");
            settings.setCustomColorSchemes(new ArrayList<>(list));
        }
        if (inputs.containsKey("colorSchemeOverrides")) {
            Map<String, Map<String, AppSettingsStore.ColorSchemeAttribute>> map =
                    (Map<String, Map<String, AppSettingsStore.ColorSchemeAttribute>>) inputs.get("colorSchemeOverrides");
            settings.setColorSchemeOverrides(new LinkedHashMap<>(map));
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
        if (inputs.containsKey("editor_lineHeight")) {
            Spinner<Double> sp = (Spinner<Double>) inputs.get("editor_lineHeight");
            settings.setEditorLineHeight(sp.getValue());
        }
        if (inputs.containsKey("editor_enableLigatures")) {
            CheckBox cb = (CheckBox) inputs.get("editor_enableLigatures");
            settings.setEditorEnableLigatures(cb.isSelected());
        }
        if (inputs.containsKey("editor_fontMainWeight")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("editor_fontMainWeight");
            if (cb.getValue() != null) settings.setEditorFontMainWeight(cb.getValue());
        }
        if (inputs.containsKey("editor_fontBoldWeight")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("editor_fontBoldWeight");
            if (cb.getValue() != null) settings.setEditorFontBoldWeight(cb.getValue());
        }
        if (inputs.containsKey("editor_fallbackFont")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("editor_fallbackFont");
            if (cb.getValue() != null) settings.setEditorFallbackFont(cb.getValue());
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
        if (inputs.containsKey("customKeymapPresets")) {
            List<String> presets = (List<String>) inputs.get("customKeymapPresets");
            settings.setCustomKeymapPresets(new ArrayList<>(presets));
        }
        if (inputs.containsKey("customKeymapShortcuts")) {
            Map<String, List<String>> map = (Map<String, List<String>>) inputs.get("customKeymapShortcuts");
            settings.setCustomKeymapShortcuts(new LinkedHashMap<>(map));
        }
        if (inputs.containsKey("removedKeymapShortcuts")) {
            Map<String, List<String>> map = (Map<String, List<String>>) inputs.get("removedKeymapShortcuts");
            settings.setRemovedKeymapShortcuts(new LinkedHashMap<>(map));
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

        // Auto Import
        if (inputs.containsKey("editor_autoImport_showXmlTooltip")) {
            CheckBox cb = (CheckBox) inputs.get("editor_autoImport_showXmlTooltip");
            settings.setShowXmlAutoImportTooltip(cb.isSelected());
        }

        // Breadcrumbs
        if (inputs.containsKey("editor_breadcrumbs_show")) {
            CheckBox cb = (CheckBox) inputs.get("editor_breadcrumbs_show");
            settings.setShowBreadcrumbs(cb.isSelected());
        }
        if (inputs.containsKey("editor_breadcrumbs_topRadio")) {
            RadioButton rb = (RadioButton) inputs.get("editor_breadcrumbs_topRadio");
            settings.setBreadcrumbsPlacement(rb.isSelected() ? "Top" : "Bottom");
        }
        if (inputs.containsKey("editor_breadcrumbs_html")) {
            CheckBox cb = (CheckBox) inputs.get("editor_breadcrumbs_html");
            settings.setBreadcrumbsHtml(cb.isSelected());
        }
        if (inputs.containsKey("editor_breadcrumbs_markdown")) {
            CheckBox cb = (CheckBox) inputs.get("editor_breadcrumbs_markdown");
            settings.setBreadcrumbsMarkdown(cb.isSelected());
        }
        if (inputs.containsKey("editor_breadcrumbs_xhtml")) {
            CheckBox cb = (CheckBox) inputs.get("editor_breadcrumbs_xhtml");
            settings.setBreadcrumbsXhtml(cb.isSelected());
        }
        if (inputs.containsKey("editor_breadcrumbs_json")) {
            CheckBox cb = (CheckBox) inputs.get("editor_breadcrumbs_json");
            settings.setBreadcrumbsJson(cb.isSelected());
        }
        if (inputs.containsKey("editor_breadcrumbs_sql")) {
            CheckBox cb = (CheckBox) inputs.get("editor_breadcrumbs_sql");
            settings.setBreadcrumbsSql(cb.isSelected());
        }
        if (inputs.containsKey("editor_breadcrumbs_xml")) {
            CheckBox cb = (CheckBox) inputs.get("editor_breadcrumbs_xml");
            settings.setBreadcrumbsXml(cb.isSelected());
        }

        // Editor Appearance
        if (inputs.containsKey("editor_appearance_caretBlinking")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_caretBlinking");
            settings.setCaretBlinking(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_caretBlinkingMs")) {
            TextField tf = (TextField) inputs.get("editor_appearance_caretBlinkingMs");
            try {
                settings.setCaretBlinkingMs(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("editor_appearance_useBlockCaret")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_useBlockCaret");
            settings.setUseBlockCaret(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_useFullLineHeightCaret")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_useFullLineHeightCaret");
            settings.setUseFullLineHeightCaret(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_highlightOccurrences")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_highlightOccurrences");
            settings.setHighlightOccurrences(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_showHardWrap")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_showHardWrap");
            settings.setShowHardWrapAndVisualGuides(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_showLineNumbers")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_showLineNumbers");
            settings.setShowLineNumbers(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_lineNumbersMode")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("editor_appearance_lineNumbersMode");
            if (cb.getValue() != null) settings.setLineNumbersMode(cb.getValue());
        }
        if (inputs.containsKey("editor_appearance_showLinesBetweenStatements")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_showLinesBetweenStatements");
            settings.setShowLinesBetweenStatements(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_showWhitespaces")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_showWhitespaces");
            settings.setShowWhitespaces(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_wsLeading")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_wsLeading");
            settings.setShowWhitespacesLeading(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_wsInner")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_wsInner");
            settings.setShowWhitespacesInner(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_wsTrailing")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_wsTrailing");
            settings.setShowWhitespacesTrailing(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_wsSelection")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_wsSelection");
            settings.setShowWhitespacesSelection(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_showIndentGuides")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_showIndentGuides");
            settings.setShowEditorIndentGuides(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_showIntentionBulb")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_showIntentionBulb");
            settings.setShowIntentionBulb(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_showPreviewIntention")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_showPreviewIntention");
            settings.setShowPreviewForIntentionActions(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_renderDocComments")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_renderDocComments");
            settings.setRenderDocComments(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_showCodeLens")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_showCodeLens");
            settings.setShowCodeLensOnScrollbarHover(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_useEditorFontInlay")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_useEditorFontInlay");
            settings.setUseEditorFontForInlayHints(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_enableTagTree")) {
            CheckBox cb = (CheckBox) inputs.get("editor_appearance_enableTagTree");
            settings.setEnableTagTreeHighlighting(cb.isSelected());
        }
        if (inputs.containsKey("editor_appearance_tagTreeLevels")) {
            Spinner<Integer> sp = (Spinner<Integer>) inputs.get("editor_appearance_tagTreeLevels");
            if (sp.getValue() != null) settings.setTagTreeLevelsToHighlight(sp.getValue());
        }
        if (inputs.containsKey("editor_appearance_tagTreeOpacity")) {
            Spinner<Double> sp = (Spinner<Double>) inputs.get("editor_appearance_tagTreeOpacity");
            if (sp.getValue() != null) settings.setTagTreeOpacity(sp.getValue());
        }

        // Editor > General > Code Completion
        if (inputs.containsKey("codeCompletion_matchCase")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_matchCase");
            settings.setMatchCase(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_firstLetterRadio")) {
            RadioButton rb = (RadioButton) inputs.get("codeCompletion_firstLetterRadio");
            settings.setMatchCaseMode(rb.isSelected() ? "First letter only" : "All letters");
        }
        if (inputs.containsKey("codeCompletion_sortAlphabetically")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_sortAlphabetically");
            settings.setSortSuggestionsAlphabetically(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_showSuggestionsAsYouType")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_showSuggestionsAsYouType");
            settings.setShowSuggestionsAsYouType(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_insertSelectedByContextKeys")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_insertSelectedByContextKeys");
            settings.setInsertSelectedSuggestionByContextKeys(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_showDocPopup")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_showDocPopup");
            settings.setShowDocPopup(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_docPopupDelay")) {
            TextField tf = (TextField) inputs.get("codeCompletion_docPopupDelay");
            try {
                settings.setDocPopupDelayMs(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("codeCompletion_insertParentheses")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_insertParentheses");
            settings.setInsertParenthesesAutomatically(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_mlSortSuggestions")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_mlSortSuggestions");
            settings.setMlSortSuggestions(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_mlSortSql")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_mlSortSql");
            settings.setMlSortSql(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_mlMarkPositionChanges")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_mlMarkPositionChanges");
            settings.setMlMarkPositionChanges(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_mlMarkMostRelevant")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_mlMarkMostRelevant");
            settings.setMlMarkMostRelevant(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_htmlAutoPopupTagCompletion")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_htmlAutoPopupTagCompletion");
            settings.setHtmlAutoPopupTagCompletion(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_showParameterInfoPopup")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_showParameterInfoPopup");
            settings.setShowParameterInfoPopup(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_parameterInfoDelay")) {
            TextField tf = (TextField) inputs.get("codeCompletion_parameterInfoDelay");
            try {
                settings.setParameterInfoDelayMs(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("codeCompletion_showFullMethodSignatures")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_showFullMethodSignatures");
            settings.setShowFullMethodSignatures(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_suggestSearchPathRadio")) {
            RadioButton sp = (RadioButton) inputs.get("codeCompletion_suggestSearchPathRadio");
            RadioButton sc = (RadioButton) inputs.get("codeCompletion_suggestScopeRadio");
            RadioButton as = (RadioButton) inputs.get("codeCompletion_suggestAllSchemasRadio");
            if (sp.isSelected()) {
                settings.setSqlSuggestObjectsFrom("The current search path only");
            } else if (as.isSelected()) {
                settings.setSqlSuggestObjectsFrom("All available schemas");
            } else {
                settings.setSqlSuggestObjectsFrom("The current scope");
            }
        }
        if (inputs.containsKey("codeCompletion_qualifyWithDatabase")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeCompletion_qualifyWithDatabase");
            if (cb.getValue() != null) settings.setQualifyWithDatabase(cb.getValue());
        }
        if (inputs.containsKey("codeCompletion_qualifyWithSchema")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeCompletion_qualifyWithSchema");
            if (cb.getValue() != null) settings.setQualifyWithSchema(cb.getValue());
        }
        if (inputs.containsKey("codeCompletion_qualifyWithTableView")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeCompletion_qualifyWithTableView");
            if (cb.getValue() != null) settings.setQualifyWithTableView(cb.getValue());
        }
        if (inputs.containsKey("codeCompletion_qualifyWithTableAlias")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeCompletion_qualifyWithTableAlias");
            if (cb.getValue() != null) settings.setQualifyWithTableAlias(cb.getValue());
        }
        if (inputs.containsKey("codeCompletion_qualifyInBasic")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeCompletion_qualifyInBasic");
            if (cb.getValue() != null) settings.setQualifyInBasicCompletion(cb.getValue());
        }
        if (inputs.containsKey("codeCompletion_qualifyInJoin")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeCompletion_qualifyInJoin");
            if (cb.getValue() != null) settings.setQualifyInJoinCompletion(cb.getValue());
        }
        if (inputs.containsKey("codeCompletion_qualifyInRefactoring")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeCompletion_qualifyInRefactoring");
            if (cb.getValue() != null) settings.setQualifyInRefactoring(cb.getValue());
        }
        if (inputs.containsKey("codeCompletion_qualifyInLiveTemplates")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeCompletion_qualifyInLiveTemplates");
            if (cb.getValue() != null) settings.setQualifyInLiveTemplates(cb.getValue());
        }
        if (inputs.containsKey("codeCompletion_qualifyInDragDrop")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeCompletion_qualifyInDragDrop");
            if (cb.getValue() != null) settings.setQualifyInDragDrop(cb.getValue());
        }
        if (inputs.containsKey("codeCompletion_joinUseAliases")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_joinUseAliases");
            settings.setJoinUseAliases(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_joinInvertOperands")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_joinInvertOperands");
            settings.setJoinInvertOperands(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_joinSuggestNonStrictFk")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_joinSuggestNonStrictFk");
            settings.setJoinSuggestNonStrictFk(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_tableAliasesAutoAdd")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_tableAliasesAutoAdd");
            settings.setTableAliasesAutoAdd(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_tableAliasesSuggest")) {
            CheckBox cb = (CheckBox) inputs.get("codeCompletion_tableAliasesSuggest");
            settings.setTableAliasesSuggest(cb.isSelected());
        }
        if (inputs.containsKey("codeCompletion_customTableAliases")) {
            Object obj = inputs.get("codeCompletion_customTableAliases");
            if (obj instanceof List<?> list) {
                List<AppSettingsStore.TableAliasConfig> out = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof AppSettingsStore.TableAliasConfig tac) {
                        if (tac.getTableName() != null && !tac.getTableName().trim().isEmpty()) {
                            out.add(tac.copy());
                        }
                    }
                }
                settings.setCustomTableAliases(out);
            }
        }
        if (inputs.containsKey("codeCompletion_additionalAcceptCharacters")) {
            TextField tf = (TextField) inputs.get("codeCompletion_additionalAcceptCharacters");
            settings.setAdditionalAcceptCharacters(tf.getText());
        }

        // Editor > General > Code Folding
        if (inputs.containsKey("codeFolding_showArrows")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_showArrows");
            settings.setShowCodeFoldingArrows(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_showArrowsMode")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeFolding_showArrowsMode");
            if (cb.getValue() != null) settings.setShowCodeFoldingArrowsMode(cb.getValue());
        }
        if (inputs.containsKey("codeFolding_showBottomArrows")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_showBottomArrows");
            settings.setShowBottomArrows(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldFileHeader")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldFileHeader");
            settings.setFoldFileHeader(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldImports")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldImports");
            settings.setFoldImports(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldDocComments")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldDocComments");
            settings.setFoldDocComments(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldMethodBodies")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldMethodBodies");
            settings.setFoldMethodBodies(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldCustomRegions")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldCustomRegions");
            settings.setFoldCustomRegions(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldMarkdownFrontMatter")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldMarkdownFrontMatter");
            settings.setFoldMarkdownFrontMatter(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldMarkdownLinks")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldMarkdownLinks");
            settings.setFoldMarkdownLinks(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldMarkdownTables")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldMarkdownTables");
            settings.setFoldMarkdownTables(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldMarkdownCodeFences")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldMarkdownCodeFences");
            settings.setFoldMarkdownCodeFences(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldMarkdownTableOfContents")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldMarkdownTableOfContents");
            settings.setFoldMarkdownTableOfContents(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldSqlUnderscores")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldSqlUnderscores");
            settings.setFoldSqlUnderscoresInNumericLiterals(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldXmlTags")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldXmlTags");
            settings.setFoldXmlTags(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldHtmlStyle")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldHtmlStyle");
            settings.setFoldHtmlStyleAttribute(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldXmlEntities")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldXmlEntities");
            settings.setFoldXmlEntities(cb.isSelected());
        }
        if (inputs.containsKey("codeFolding_foldDataUris")) {
            CheckBox cb = (CheckBox) inputs.get("codeFolding_foldDataUris");
            settings.setFoldDataUris(cb.isSelected());
        }

        // Editor > General > Editor Tabs
        if (inputs.containsKey("editorTabs_placement")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("editorTabs_placement");
            if (cb.getValue() != null) settings.setEditorTabPlacement(cb.getValue());
        }
        if (inputs.containsKey("editorTabs_oneRowRadio")) {
            RadioButton rb = (RadioButton) inputs.get("editorTabs_oneRowRadio");
            settings.setEditorTabsShowMode(rb.isSelected() ? "One row" : "Multiple rows");
        }
        if (inputs.containsKey("editorTabs_squeezeRadio")) {
            RadioButton rb = (RadioButton) inputs.get("editorTabs_squeezeRadio");
            settings.setEditorTabsOverflowMode(rb.isSelected() ? "Squeeze tabs" : "Scroll the tabs panel");
        }
        if (inputs.containsKey("editorTabs_showPinnedTabs")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_showPinnedTabs");
            settings.setShowPinnedTabsInSeparateRow(cb.isSelected());
        }
        if (inputs.containsKey("editorTabs_showFileIcon")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_showFileIcon");
            settings.setEditorTabsShowFileIcon(cb.isSelected());
        }
        if (inputs.containsKey("editorTabs_showFileExtension")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_showFileExtension");
            settings.setEditorTabsShowFileExtension(cb.isSelected());
        }
        if (inputs.containsKey("editorTabs_showDirNonUnique")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_showDirNonUnique");
            settings.setEditorTabsShowDirectoryForNonUnique(cb.isSelected());
        }
        if (inputs.containsKey("editorTabs_markModified")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_markModified");
            settings.setEditorTabsMarkModified(cb.isSelected());
        }
        if (inputs.containsKey("editorTabs_showFullPathHover")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_showFullPathHover");
            settings.setEditorTabsShowFullPathOnHover(cb.isSelected());
        }
        if (inputs.containsKey("editorTabs_closeButtonPosition")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("editorTabs_closeButtonPosition");
            if (cb.getValue() != null) settings.setEditorTabsCloseButtonPosition(cb.getValue());
        }
        if (inputs.containsKey("editorTabs_sortAlphabetically")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_sortAlphabetically");
            settings.setEditorTabsSortAlphabetically(cb.isSelected());
        }
        if (inputs.containsKey("editorTabs_openNewAtEnd")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_openNewAtEnd");
            settings.setEditorTabsOpenNewAtEnd(cb.isSelected());
        }
        if (inputs.containsKey("editorTabs_enablePreviewTab")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_enablePreviewTab");
            settings.setEditorTabsEnablePreviewTab(cb.isSelected());
        }
        if (inputs.containsKey("editorTabs_limit")) {
            TextField tf = (TextField) inputs.get("editorTabs_limit");
            try {
                settings.setEditorTabsLimit(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("editorTabs_closeUnchangedRadio")) {
            RadioButton rb = (RadioButton) inputs.get("editorTabs_closeUnchangedRadio");
            settings.setEditorTabsExceedLimitPolicy(rb.isSelected() ? "Close unchanged" : "Close unused");
        }
        if (inputs.containsKey("editorTabs_activateLeftRadio")) {
            RadioButton right = (RadioButton) inputs.get("editorTabs_activateRightRadio");
            RadioButton recent = (RadioButton) inputs.get("editorTabs_activateRecentRadio");
            if (right != null && right.isSelected()) {
                settings.setEditorTabsCloseActivatePolicy("The tab on the right");
            } else if (recent != null && recent.isSelected()) {
                settings.setEditorTabsCloseActivatePolicy("Most recently opened tab");
            } else {
                settings.setEditorTabsCloseActivatePolicy("The tab on the left");
            }
        }
        if (inputs.containsKey("editorTabs_alwaysQualified")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_alwaysQualified");
            settings.setEditorTabsAlwaysShowQualifiedNames(cb.isSelected());
        }
        if (inputs.containsKey("editorTabs_shortenNames")) {
            CheckBox cb = (CheckBox) inputs.get("editorTabs_shortenNames");
            settings.setEditorTabsShortenNames(cb.isSelected());
        }

        // Editor > General > Output Console
        if (inputs.containsKey("outputConsole_useSoftWraps")) {
            CheckBox cb = (CheckBox) inputs.get("outputConsole_useSoftWraps");
            settings.setOutputConsoleUseSoftWraps(cb.isSelected());
        }
        if (inputs.containsKey("outputConsole_historySize")) {
            TextField tf = (TextField) inputs.get("outputConsole_historySize");
            try {
                settings.setOutputConsoleHistorySize(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("outputConsole_overrideCycleBuffer")) {
            CheckBox cb = (CheckBox) inputs.get("outputConsole_overrideCycleBuffer");
            settings.setOutputConsoleOverrideCycleBuffer(cb.isSelected());
        }
        if (inputs.containsKey("outputConsole_cycleBufferSizeKb")) {
            TextField tf = (TextField) inputs.get("outputConsole_cycleBufferSizeKb");
            try {
                settings.setOutputConsoleCycleBufferSizeKb(Integer.parseInt(tf.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (inputs.containsKey("outputConsole_defaultEncoding")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("outputConsole_defaultEncoding");
            if (cb.getValue() != null) settings.setOutputConsoleDefaultEncoding(cb.getValue());
        }
        if (inputs.containsKey("outputConsole_foldingPatterns")) {
            Object obj = inputs.get("outputConsole_foldingPatterns");
            if (obj instanceof ObservableList<?> ol) {
                List<String> list = new ArrayList<>();
                for (Object item : ol) {
                    if (item != null && !item.toString().isBlank()) list.add(item.toString().trim());
                }
                settings.setOutputConsoleFoldingPatterns(list);
            }
        }
        if (inputs.containsKey("outputConsole_foldingExceptions")) {
            Object obj = inputs.get("outputConsole_foldingExceptions");
            if (obj instanceof ObservableList<?> ol) {
                List<String> list = new ArrayList<>();
                for (Object item : ol) {
                    if (item != null && !item.toString().isBlank()) list.add(item.toString().trim());
                }
                settings.setOutputConsoleFoldingExceptions(list);
            }
        }

        // Editor > General > Gutter Icons
        if (inputs.containsKey("gutterIcons_showGutterIcons")) {
            CheckBox cb = (CheckBox) inputs.get("gutterIcons_showGutterIcons");
            settings.setShowGutterIcons(cb.isSelected());
        }
        if (inputs.containsKey("gutterIcons_colorPreview")) {
            CheckBox cb = (CheckBox) inputs.get("gutterIcons_colorPreview");
            settings.setGutterColorPreview(cb.isSelected());
        }
        if (inputs.containsKey("gutterIcons_docComments")) {
            CheckBox cb = (CheckBox) inputs.get("gutterIcons_docComments");
            settings.setGutterDocComments(cb.isSelected());
        }
        if (inputs.containsKey("gutterIcons_runLineMarker")) {
            CheckBox cb = (CheckBox) inputs.get("gutterIcons_runLineMarker");
            settings.setGutterRunLineMarker(cb.isSelected());
        }
        if (inputs.containsKey("gutterIcons_recursiveCall")) {
            CheckBox cb = (CheckBox) inputs.get("gutterIcons_recursiveCall");
            settings.setGutterRecursiveCall(cb.isSelected());
        }
        if (inputs.containsKey("gutterIcons_vcsIgnoredDirectories")) {
            CheckBox cb = (CheckBox) inputs.get("gutterIcons_vcsIgnoredDirectories");
            settings.setGutterVcsIgnoredDirectories(cb.isSelected());
        }
        if (inputs.containsKey("gutterIcons_configureHtmlImage")) {
            CheckBox cb = (CheckBox) inputs.get("gutterIcons_configureHtmlImage");
            settings.setGutterConfigureHtmlImage(cb.isSelected());
        }
        if (inputs.containsKey("gutterIcons_configureMarkdownImage")) {
            CheckBox cb = (CheckBox) inputs.get("gutterIcons_configureMarkdownImage");
            settings.setGutterConfigureMarkdownImage(cb.isSelected());
        }
        if (inputs.containsKey("gutterIcons_installPlantUml")) {
            CheckBox cb = (CheckBox) inputs.get("gutterIcons_installPlantUml");
            settings.setGutterInstallPlantUml(cb.isSelected());
        }

        // Editor > General > Inline Completion
        if (inputs.containsKey("inlineCompletion_enabled")) {
            CheckBox cb = (CheckBox) inputs.get("inlineCompletion_enabled");
            settings.setInlineCompletionEnabled(cb.isSelected());
        }
        if (inputs.containsKey("inlineCompletion_autoOnTyping")) {
            CheckBox cb = (CheckBox) inputs.get("inlineCompletion_autoOnTyping");
            settings.setInlineAutoOnTyping(cb.isSelected());
        }
        if (inputs.containsKey("inlineCompletion_multiline")) {
            CheckBox cb = (CheckBox) inputs.get("inlineCompletion_multiline");
            settings.setInlineMultilineSuggestions(cb.isSelected());
        }
        if (inputs.containsKey("inlineCompletion_syncWithPopup")) {
            CheckBox cb = (CheckBox) inputs.get("inlineCompletion_syncWithPopup");
            settings.setInlineSyncWithPopup(cb.isSelected());
        }

        // Editor > General > Smart Keys
        if (inputs.containsKey("smartKeys_homeMovesCaret")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_homeMovesCaret");
            settings.setSmartKeysHomeMovesCaret(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_endBlankLineMovesCaret")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_endBlankLineMovesCaret");
            settings.setSmartKeysEndBlankLineMovesCaret(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_insertPairedBrackets")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_insertPairedBrackets");
            settings.setSmartKeysInsertPairedBrackets(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_insertPairQuote")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_insertPairQuote");
            settings.setSmartKeysInsertPairQuote(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_reformatBlockOnBrace")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_reformatBlockOnBrace");
            settings.setSmartKeysReformatBlockOnBrace(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_useCamelHumps")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_useCamelHumps");
            settings.setSmartKeysUseCamelHumps(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_honorCamelHumpsOnDoubleClick")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_honorCamelHumpsOnDoubleClick");
            settings.setSmartKeysHonorCamelHumpsOnDoubleClick(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_surroundSelectionOnQuoteOrBrace")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_surroundSelectionOnQuoteOrBrace");
            settings.setSmartKeysSurroundSelectionOnQuoteOrBrace(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_multiCaretsOnDoubleModifier")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_multiCaretsOnDoubleModifier");
            settings.setSmartKeysMultiCaretsOnDoubleModifier(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_jumpOutsideBracketWithTab")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_jumpOutsideBracketWithTab");
            settings.setSmartKeysJumpOutsideBracketWithTab(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_enterSmartIndent")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_enterSmartIndent");
            settings.setSmartKeysEnterSmartIndent(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_enterInsertPairBrace")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_enterInsertPairBrace");
            settings.setSmartKeysEnterInsertPairBrace(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_enterCloseBlockComment")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_enterCloseBlockComment");
            settings.setSmartKeysEnterCloseBlockComment(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_unindentOnBackspace")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("smartKeys_unindentOnBackspace");
            if (cb.getValue() != null) settings.setSmartKeysUnindentOnBackspace(cb.getValue());
        }
        if (inputs.containsKey("smartKeys_reformatOnPaste")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("smartKeys_reformatOnPaste");
            if (cb.getValue() != null) settings.setSmartKeysReformatOnPaste(cb.getValue());
        }
        if (inputs.containsKey("smartKeys_reformatRemoveCustomLineBreaks")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_reformatRemoveCustomLineBreaks");
            settings.setSmartKeysReformatRemoveCustomLineBreaks(cb.isSelected());
        }

        // Editor > General > Smart Keys > SQL
        if (inputs.containsKey("smartKeys_sql_insertStringConcatOnEnter")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_sql_insertStringConcatOnEnter");
            settings.setSmartKeysSqlInsertStringConcatOnEnter(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_sql_closeCodeBlocksOnEnter")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_sql_closeCodeBlocksOnEnter");
            settings.setSmartKeysSqlCloseCodeBlocksOnEnter(cb.isSelected());
        }

        // Editor > General > Smart Keys > Markdown
        if (inputs.containsKey("smartKeys_markdown_reformatTable")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_markdown_reformatTable");
            settings.setSmartKeysMarkdownReformatTable(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_markdown_insertHtmlLineBreakInTable")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_markdown_insertHtmlLineBreakInTable");
            settings.setSmartKeysMarkdownInsertHtmlLineBreakInTable(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_markdown_shiftEnterNewTableRow")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_markdown_shiftEnterNewTableRow");
            settings.setSmartKeysMarkdownShiftEnterNewTableRow(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_markdown_tabNavigateTable")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_markdown_tabNavigateTable");
            settings.setSmartKeysMarkdownTabNavigateTable(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_markdown_adjustListIndent")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_markdown_adjustListIndent");
            settings.setSmartKeysMarkdownAdjustListIndent(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_markdown_smartEnterBackspace")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_markdown_smartEnterBackspace");
            settings.setSmartKeysMarkdownSmartEnterBackspace(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_markdown_renumberList")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_markdown_renumberList");
            settings.setSmartKeysMarkdownRenumberList(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_markdown_listNumerating")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("smartKeys_markdown_listNumerating");
            if (cb.getValue() != null) settings.setSmartKeysMarkdownListNumerating(cb.getValue());
        }
        if (inputs.containsKey("smartKeys_markdown_insertLinksOnDrop")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_markdown_insertLinksOnDrop");
            settings.setSmartKeysMarkdownInsertLinksOnDrop(cb.isSelected());
        }

        // Editor > General > Smart Keys > JSON
        if (inputs.containsKey("smartKeys_json_insertMissingCommaOnEnter")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_json_insertMissingCommaOnEnter");
            settings.setSmartKeysJsonInsertMissingCommaOnEnter(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_json_insertMissingCommaAfterMatching")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_json_insertMissingCommaAfterMatching");
            settings.setSmartKeysJsonInsertMissingCommaAfterMatching(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_json_manageCommasOnPaste")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_json_manageCommasOnPaste");
            settings.setSmartKeysJsonManageCommasOnPaste(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_json_escapeTextOnPaste")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_json_escapeTextOnPaste");
            settings.setSmartKeysJsonEscapeTextOnPaste(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_json_addQuotesToPropertyNames")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_json_addQuotesToPropertyNames");
            settings.setSmartKeysJsonAddQuotesToPropertyNames(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_json_addWhitespaceAfterColon")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_json_addWhitespaceAfterColon");
            settings.setSmartKeysJsonAddWhitespaceAfterColon(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_json_moveColonAfterPropertyName")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_json_moveColonAfterPropertyName");
            settings.setSmartKeysJsonMoveColonAfterPropertyName(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_json_moveCommaAfterPropertyValue")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_json_moveCommaAfterPropertyValue");
            settings.setSmartKeysJsonMoveCommaAfterPropertyValue(cb.isSelected());
        }

        // Editor > General > Smart Keys > HTML/CSS
        if (inputs.containsKey("smartKeys_html_insertClosingTag")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_html_insertClosingTag");
            settings.setSmartKeysHtmlInsertClosingTag(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_html_insertRequiredAttributes")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_html_insertRequiredAttributes");
            settings.setSmartKeysHtmlInsertRequiredAttributes(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_html_insertRequiredSubtags")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_html_insertRequiredSubtags");
            settings.setSmartKeysHtmlInsertRequiredSubtags(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_html_startAttribute")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_html_startAttribute");
            settings.setSmartKeysHtmlStartAttribute(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_html_addQuotesForAttribute")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_html_addQuotesForAttribute");
            settings.setSmartKeysHtmlAddQuotesForAttribute(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_html_autoCloseTag")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_html_autoCloseTag");
            settings.setSmartKeysHtmlAutoCloseTag(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_html_simultaneousTagEditing")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_html_simultaneousTagEditing");
            settings.setSmartKeysHtmlSimultaneousTagEditing(cb.isSelected());
        }
        if (inputs.containsKey("smartKeys_css_selectWholeCssIdentifiers")) {
            CheckBox cb = (CheckBox) inputs.get("smartKeys_css_selectWholeCssIdentifiers");
            settings.setSmartKeysCssSelectWholeCssIdentifiers(cb.isSelected());
        }

        // Editor > General > Postfix Completion
        if (inputs.containsKey("postfixCompletion_enabled")) {
            CheckBox cb = (CheckBox) inputs.get("postfixCompletion_enabled");
            settings.setPostfixCompletionEnabled(cb.isSelected());
        }
        if (inputs.containsKey("postfixCompletion_expandWith")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("postfixCompletion_expandWith");
            if (cb.getValue() != null) settings.setPostfixCompletionExpandWith(cb.getValue());
        }
        if (inputs.containsKey("postfixCompletion_templates")) {
            List<AppSettingsStore.PostfixTemplateConfig> list = (List<AppSettingsStore.PostfixTemplateConfig>) inputs.get("postfixCompletion_templates");
            if (list != null) {
                List<AppSettingsStore.PostfixTemplateConfig> copies = new ArrayList<>();
                for (AppSettingsStore.PostfixTemplateConfig t : list) {
                    copies.add(t.copy());
                }
                settings.setPostfixTemplates(copies);
            }
        }

        // Editor > General > Sticky Lines
        if (inputs.containsKey("stickyLines_enabled")) {
            CheckBox cb = (CheckBox) inputs.get("stickyLines_enabled");
            settings.setStickyLinesEnabled(cb.isSelected());
        }
        if (inputs.containsKey("stickyLines_maxLines")) {
            Spinner<Integer> sp = (Spinner<Integer>) inputs.get("stickyLines_maxLines");
            settings.setStickyLinesMaxLines(sp.getValue());
        }
        if (inputs.containsKey("stickyLines_html")) {
            CheckBox cb = (CheckBox) inputs.get("stickyLines_html");
            settings.setStickyLinesHtml(cb.isSelected());
        }
        if (inputs.containsKey("stickyLines_markdown")) {
            CheckBox cb = (CheckBox) inputs.get("stickyLines_markdown");
            settings.setStickyLinesMarkdown(cb.isSelected());
        }
        if (inputs.containsKey("stickyLines_xhtml")) {
            CheckBox cb = (CheckBox) inputs.get("stickyLines_xhtml");
            settings.setStickyLinesXhtml(cb.isSelected());
        }
        if (inputs.containsKey("stickyLines_json")) {
            CheckBox cb = (CheckBox) inputs.get("stickyLines_json");
            settings.setStickyLinesJson(cb.isSelected());
        }
        if (inputs.containsKey("stickyLines_sql")) {
            CheckBox cb = (CheckBox) inputs.get("stickyLines_sql");
            settings.setStickyLinesSql(cb.isSelected());
        }
        if (inputs.containsKey("stickyLines_xml")) {
            CheckBox cb = (CheckBox) inputs.get("stickyLines_xml");
            settings.setStickyLinesXml(cb.isSelected());
        }

        // Editor > Code Editing
        if (inputs.containsKey("codeEditing_highlightMatchedBrace")) {
            CheckBox cb = (CheckBox) inputs.get("codeEditing_highlightMatchedBrace");
            settings.setCodeEditingHighlightMatchedBrace(cb.isSelected());
        }
        if (inputs.containsKey("codeEditing_highlightCurrentScope")) {
            CheckBox cb = (CheckBox) inputs.get("codeEditing_highlightCurrentScope");
            settings.setCodeEditingHighlightCurrentScope(cb.isSelected());
        }
        if (inputs.containsKey("codeEditing_highlightUsages")) {
            CheckBox cb = (CheckBox) inputs.get("codeEditing_highlightUsages");
            settings.setCodeEditingHighlightUsages(cb.isSelected());
        }
        if (inputs.containsKey("codeEditing_showDocOnHover")) {
            CheckBox cb = (CheckBox) inputs.get("codeEditing_showDocOnHover");
            settings.setCodeEditingShowDocOnHover(cb.isSelected());
        }
        if (inputs.containsKey("codeEditing_refactoringOption")) {
            ToggleGroup tg = (ToggleGroup) inputs.get("codeEditing_refactoringOption");
            if (tg.getSelectedToggle() instanceof RadioButton rb) {
                settings.setCodeEditingRefactoringOption(rb.getText());
            }
        }
        if (inputs.containsKey("codeEditing_preselectCurrentSymbol")) {
            CheckBox cb = (CheckBox) inputs.get("codeEditing_preselectCurrentSymbol");
            settings.setCodeEditingPreselectCurrentSymbol(cb.isSelected());
        }
        if (inputs.containsKey("codeEditing_showInlineDialogForLocalVars")) {
            CheckBox cb = (CheckBox) inputs.get("codeEditing_showInlineDialogForLocalVars");
            settings.setCodeEditingShowInlineDialogForLocalVars(cb.isSelected());
        }
        if (inputs.containsKey("codeEditing_errorStripeMarkMinHeight")) {
            Spinner<Integer> sp = (Spinner<Integer>) inputs.get("codeEditing_errorStripeMarkMinHeight");
            settings.setCodeEditingErrorStripeMarkMinHeight(sp.getValue());
        }
        if (inputs.containsKey("codeEditing_autoreparseDelayMs")) {
            Spinner<Integer> sp = (Spinner<Integer>) inputs.get("codeEditing_autoreparseDelayMs");
            settings.setCodeEditingAutoreparseDelayMs(sp.getValue());
        }
        if (inputs.containsKey("codeEditing_nextErrorAction")) {
            ComboBox<String> cb = (ComboBox<String>) inputs.get("codeEditing_nextErrorAction");
            if (cb.getValue() != null) settings.setCodeEditingNextErrorAction(cb.getValue());
        }
        if (inputs.containsKey("codeEditing_tooltipDelayMs")) {
            Spinner<Integer> sp = (Spinner<Integer>) inputs.get("codeEditing_tooltipDelayMs");
            settings.setCodeEditingTooltipDelayMs(sp.getValue());
        }

        // Persist
        AppSettingsStore.save(settings);

        // Apply to open consoles and notify
        if (mainWindow != null) {
            mainWindow.applyEditorSettingsToOpenConsoles();
            mainWindow.setStatus("Settings applied");
        }
    }
}

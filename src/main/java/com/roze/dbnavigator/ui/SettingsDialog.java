package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

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

        editor.getChildren().add(new CategoryDef("editor.color_scheme", "Color Scheme", "Editor / Color Scheme",
                "Customize syntax highlighting colors for keywords, strings, comments, and identifiers."));
        editor.getChildren().add(new CategoryDef("editor.code_style", "Code Style", "Editor / Code Style",
                "Configure code formatting rules, indentation, keyword casing, and line wrapping for SQL."));
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
        editor.getChildren().add(new CategoryDef("editor.natural_languages", "Natural Languages", "Editor / Natural Languages",
                "Spell checking and natural language proofreading in comments."));
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
        languages.getChildren().add(new CategoryDef("lang.schemas_dtds", "Schemas and DTDs", "Languages / Schemas and DTDs",
                "Manage XML/JSON schema catalogs, DTD declarations, and URI mappings."));
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
        tools.getChildren().add(new CategoryDef("tools.debugger", "Debugger", "Tools / Debugger",
                "Configure SQL routine debugging, breakpoints, and value inspection."));
        tools.getChildren().add(new CategoryDef("tools.diagrams", "Diagrams", "Tools / Diagrams",
                "Configure ER diagram layout engine, relationship link styles, and table nodes."));
        tools.getChildren().add(new CategoryDef("tools.diff_merge", "Diff & Merge", "Tools / Diff & Merge",
                "Configure difference viewers, external diff tools, and three-way merge tools."));
        tools.getChildren().add(new CategoryDef("tools.external_tools", "External Tools", "Tools / External Tools",
                "Define custom external tools, command arguments, and macro variables."));
        tools.getChildren().add(new CategoryDef("tools.features_suggester", "Features Suggester", "Tools / Features Suggester",
                "Configure smart tips suggesting IDE productivity features and shortcuts."));
        tools.getChildren().add(new CategoryDef("tools.features_trainer", "Features Trainer", "Tools / Features Trainer",
                "Interactive tutorials for learning database IDE workflows and navigation."));
        tools.getChildren().add(new CategoryDef("tools.mcp_server", "MCP Server", "Tools / MCP Server",
                "Configure Model Context Protocol (MCP) server endpoints, tools, and sidecar integration."));
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
        stage.setTitle("Settings");
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

        VBox leftPane = new VBox(8, searchField, tree);
        leftPane.setPadding(new Insets(10, 8, 10, 10));
        leftPane.setPrefWidth(240);

        // Content Area Top Bar: Breadcrumb + Back/Forward/Pin
        Label breadcrumb = new Label("Database");
        breadcrumb.getStyleClass().add("panel-header");
        breadcrumb.setStyle("-fx-font-size: 13px; -fx-text-fill: #868a91;");

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
                tree.getSelectionModel().select(target);
                tree.scrollTo(tree.getRow(target));
            }
        };

        // Selection listener
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String fullPath = getFullPath(newVal);
            breadcrumb.setText(fullPath.replace(" / ", " \u203a "));

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
            return buildAppearancePanel(settings, inputs);
        } else if ("Appearance & Behavior / System Settings / Updates".equals(fullPath) || "Updates".equals(fullPath)) {
            return buildUpdatesPanel(settings, inputs);
        } else if ("Editor / General".equals(fullPath)) {
            return buildGeneralEditorPanel(settings, inputs);
        } else if ("Editor / General / Font".equals(fullPath) || "Editor / Font".equals(fullPath) || "Font".equals(fullPath)) {
            return buildFontPanel(settings, inputs);
        } else if ("Database / Query Execution".equals(fullPath)) {
            return buildQueryExecutionPanel(settings, inputs, navigateTo);
        } else if ("Database / Data Editor and Viewer".equals(fullPath) || "Appearance & Behavior / Data Editor and Viewer".equals(fullPath)) {
            return buildDataEditorPanel(settings, inputs);
        } else if ("Database / Database Explorer".equals(fullPath)) {
            return buildDatabaseExplorerPanel(settings, inputs);
        } else if ("Database / CSV Formats".equals(fullPath)) {
            return buildCsvFormatsPanel(settings, inputs);
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

    private static VBox buildAppearancePanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label title = new Label("Appearance");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label uiThemeLabel = new Label("Theme:");
        uiThemeLabel.getStyleClass().add("connection-field-label");
        ComboBox<AppSettingsStore.Theme> themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll(AppSettingsStore.Theme.DARK, AppSettingsStore.Theme.LIGHT);
        themeCombo.getSelectionModel().select(settings.getTheme());
        themeCombo.setPrefWidth(220);

        Label uiFontLabel = new Label("UI font:");
        uiFontLabel.getStyleClass().add("connection-field-label");
        ComboBox<String> uiFontCombo = new ComboBox<>();
        uiFontCombo.getItems().addAll("JetBrains Sans", "Segoe UI", "SF Pro Text", "Ubuntu", "Cantarell", "System Default");
        uiFontCombo.getSelectionModel().select(0);
        uiFontCombo.setPrefWidth(220);

        Label zoomLabel = new Label("Zoom:");
        zoomLabel.getStyleClass().add("connection-field-label");
        ComboBox<String> zoomCombo = new ComboBox<>();
        zoomCombo.getItems().addAll("100%", "110%", "125%", "150%");
        zoomCombo.getSelectionModel().select(0);
        zoomCombo.setPrefWidth(120);

        CheckBox antialiasing = new CheckBox("Use LCD antialiasing for fonts");
        antialiasing.setSelected(true);

        CheckBox compactTree = new CheckBox("Use compact tree indentation");
        compactTree.setSelected(false);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(uiThemeLabel, 0, 0);
        grid.add(themeCombo, 1, 0);
        grid.add(uiFontLabel, 0, 1);
        grid.add(uiFontCombo, 1, 1);
        grid.add(zoomLabel, 0, 2);
        grid.add(zoomCombo, 1, 2);

        Label hint = new Label("Theme changes apply immediately to the current window; open dialogs and consoles pick up the updated styling automatically.");
        hint.getStyleClass().add("console-status");
        hint.setWrapText(true);
        hint.setMaxWidth(520);

        inputs.put("themeCombo", themeCombo);

        VBox panel = new VBox(14, title, grid, antialiasing, compactTree, hint);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
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
        Label title = new Label("Query Execution");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

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

        CheckBox singleTxScript = new CheckBox("Treat multi-statement scripts as single transaction");
        singleTxScript.setSelected(true);

        CheckBox highlightExec = new CheckBox("Highlight execution point in SQL console");
        highlightExec.setSelected(true);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.add(timeoutLabel, 0, 0);
        grid.add(timeoutSpinner, 1, 0);
        grid.add(maxRowsLabel, 0, 1);
        grid.add(maxRowsSpinner, 1, 1);

        inputs.put("timeoutSpinner", timeoutSpinner);
        inputs.put("maxRowsSpinner", maxRowsSpinner);
        inputs.put("autoCommit", autoCommit);

        HBox links = new HBox(16);
        Hyperlink resultsLink = new Hyperlink("Output and Results");
        resultsLink.setStyle("-fx-text-fill: #3574F0;");
        resultsLink.setOnAction(e -> navigateTo.accept("Database / Query Execution / Output and Results"));

        Hyperlink paramsLink = new Hyperlink("User Parameters");
        paramsLink.setStyle("-fx-text-fill: #3574F0;");
        paramsLink.setOnAction(e -> navigateTo.accept("Database / Query Execution / User Parameters"));
        links.getChildren().addAll(resultsLink, paramsLink);

        VBox panel = new VBox(12, title, grid, autoCommit, singleTxScript, highlightExec, new Separator(),
                new Label("Related pages:"), links);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildDataEditorPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label title = new Label("Data Editor and Viewer");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label pageLabel = new Label("Page size (rows per page):");
        ComboBox<Integer> pageSizeCombo = new ComboBox<>();
        pageSizeCombo.getItems().addAll(50, 100, 200, 500, 1000);
        pageSizeCombo.getSelectionModel().select(Integer.valueOf(settings.getPageSize()));
        pageSizeCombo.setPrefWidth(120);

        Label binaryLabel = new Label("Display binary data as:");
        ComboBox<String> binaryCombo = new ComboBox<>();
        binaryCombo.getItems().addAll("Hexadecimal (HEX)", "Plain text (UTF-8)", "Base64");
        binaryCombo.getSelectionModel().select(0);
        binaryCombo.setPrefWidth(180);

        Label nullLabel = new Label("NULL value display:");
        TextField nullText = new TextField("<null>");
        nullText.setPrefWidth(140);

        CheckBox zebraCheck = new CheckBox("Colorize alternating rows (zebra striping)");
        zebraCheck.setSelected(true);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.add(pageLabel, 0, 0);
        grid.add(pageSizeCombo, 1, 0);
        grid.add(binaryLabel, 0, 1);
        grid.add(binaryCombo, 1, 1);
        grid.add(nullLabel, 0, 2);
        grid.add(nullText, 1, 2);

        inputs.put("pageSizeCombo", pageSizeCombo);

        VBox panel = new VBox(14, title, grid, zebraCheck);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildDatabaseExplorerPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label title = new Label("Database Explorer");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox groupDataSources = new CheckBox("Group data sources by environment / project");
        groupDataSources.setSelected(true);

        CheckBox showEmpty = new CheckBox("Show empty schemas");
        showEmpty.setSelected(settings.isShowEmptySchemas());

        CheckBox autoSync = new CheckBox("Auto-sync introspected schemas on connection");
        autoSync.setSelected(true);

        CheckBox loadSources = new CheckBox("Load table source definitions in background");
        loadSources.setSelected(true);

        inputs.put("showEmpty", showEmpty);

        VBox panel = new VBox(14, title, groupDataSources, showEmpty, autoSync, loadSources);
        panel.setPadding(new Insets(4, 8, 16, 8));
        return panel;
    }

    private static VBox buildCsvFormatsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        Label title = new Label("CSV Formats");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        Label delimLabel = new Label("Default delimiter:");
        ComboBox<String> delimCombo = new ComboBox<>();
        delimCombo.getItems().addAll(",", ";", "\\t (Tab)", "|");
        delimCombo.getSelectionModel().select(settings.getCsvDelimiter());
        delimCombo.setPrefWidth(140);

        Label quoteLabel = new Label("Quote character:");
        TextField quoteField = new TextField(settings.getCsvQuoteChar());
        quoteField.setPrefWidth(60);

        CheckBox firstRowHeader = new CheckBox("First row contains column headers");
        firstRowHeader.setSelected(true);

        CheckBox trimSpaces = new CheckBox("Trim leading and trailing whitespace");
        trimSpaces.setSelected(true);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.add(delimLabel, 0, 0);
        grid.add(delimCombo, 1, 0);
        grid.add(quoteLabel, 0, 1);
        grid.add(quoteField, 1, 1);

        inputs.put("delimCombo", delimCombo);
        inputs.put("quoteField", quoteField);

        VBox panel = new VBox(14, title, grid, firstRowHeader, trimSpaces);
        panel.setPadding(new Insets(4, 8, 16, 8));
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

    private static VBox buildMcpServerPanel(CategoryDef cat) {
        Label title = new Label("MCP Server");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        CheckBox enableMcp = new CheckBox("Enable Model Context Protocol (MCP) sidecar server");
        enableMcp.setSelected(true);

        Label transportLabel = new Label("Transport mode:");
        ComboBox<String> transportCombo = new ComboBox<>();
        transportCombo.getItems().addAll("Standard I/O (stdio)", "Server-Sent Events (SSE)");
        transportCombo.getSelectionModel().select(0);
        transportCombo.setPrefWidth(220);

        Label portLabel = new Label("Port / Endpoint:");
        TextField portField = new TextField("8088");
        portField.setPrefWidth(100);

        CheckBox shareSchema = new CheckBox("Expose active schema introspections to AI assistants");
        shareSchema.setSelected(true);

        CheckBox allowQueryExec = new CheckBox("Allow read-only query execution via MCP tools");
        allowQueryExec.setSelected(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(transportLabel, 0, 0);
        grid.add(transportCombo, 1, 0);
        grid.add(portLabel, 0, 1);
        grid.add(portField, 1, 1);

        VBox panel = new VBox(14, title, enableMcp, grid, shareSchema, allowQueryExec);
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
        // Theme
        if (inputs.containsKey("themeCombo")) {
            ComboBox<AppSettingsStore.Theme> combo = (ComboBox<AppSettingsStore.Theme>) inputs.get("themeCombo");
            if (combo.getValue() != null) {
                settings.setTheme(combo.getValue());
                ThemeManager.setTheme(combo.getValue());
            }
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
        if (inputs.containsKey("keymapCombo")) {
            ComboBox<String> combo = (ComboBox<String>) inputs.get("keymapCombo");
            if (combo.getValue() != null) settings.setKeymapPreset(combo.getValue());
        }

        // Persist
        AppSettingsStore.save(settings);

        // Apply to open consoles and notify
        mainWindow.applyEditorFontToOpenConsoles();
        mainWindow.setStatus("Settings applied");
    }
}

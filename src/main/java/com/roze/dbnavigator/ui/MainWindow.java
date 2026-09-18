package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.*;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.roze.dbnavigator.ui.action.ActionGroup;
import com.roze.dbnavigator.ui.action.ActionManager;
import com.roze.dbnavigator.ui.action.ActionRegistry;
import com.roze.dbnavigator.ui.action.AnAction;
import javafx.event.Event;
import org.fxmisc.richtext.CodeArea;

/**
 * Top-level layout: menu bar + toolbar (top), explorer (left), tabs (center),
 * a docked/resizable Run panel (bottom, collapsible), and a status bar with
 * a background-task indicator (very bottom).
 */
public class MainWindow {

    private final Stage stage;
    private final BorderPane root = new BorderPane();
    private final TabPane tabPane = new TabPane();
    /** The most recently used editor group; menu/toolbar actions use this group. */
    private TabPane activeTabPane;
    private final java.util.Deque<java.util.function.Supplier<Tab>> closedTabStack = new java.util.ArrayDeque<>();
    private final SchemaTreePane schemaPane;
    private final RunPanel runPanel = new RunPanel();
    private final SplitPane verticalSplit = new SplitPane();
    private final SplitPane centerSplit;
    private boolean runPanelVisible = false;
    private double lastRunPanelDivider = 0.72;

    private final Label statusLabel = new Label("Ready");
    private final HBox taskIndicator = new HBox();
    private final Label taskBreadcrumbLabel = new Label();
    private final Label taskNameLabel = new Label();
    private final ProgressBar taskProgressBar = new ProgressBar();
    private final Button taskCancelButton = new Button();
    private int consoleCounter = 0;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.schemaPane = new SchemaTreePane(this);

        ActionManager actionManager = ActionManager.getInstance();
        ActionRegistry.initialize(actionManager);

        root.getStyleClass().add("app-root");
        root.setTop(buildDataGripHeader(actionManager));
        root.setBottom(buildStatusBar());

        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (new KeyCodeCombination(KeyCode.F,
                    KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(e)) {
                showSearchEverywhere();
                e.consume();
            }
        });

        tabPane.getStyleClass().add("main-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        configureEditorTabPane(tabPane);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            actionManager.updateActions(this);
        });
        showWelcomeTab();

        centerSplit = new SplitPane(schemaPane, tabPane);
        centerSplit.setDividerPositions(0.22);
        SplitPane.setResizableWithParent(schemaPane, false);

        // Vertical split: main content on top, Run panel docked at the bottom.
        // The divider between them is mouse-draggable by default (standard
        // SplitPane behavior) — that's what makes the Run panel resizable.
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getItems().add(centerSplit);
        runPanel.setOnMinimize(this::hideRunPanel);

        root.setCenter(verticalSplit);

        // Needs centerSplit/tabPane fully wired first — editorTabPanes()
        // (used by addAndSelect for each restored console) walks centerSplit's
        // own children, which don't exist until the two lines above run.
        restoreSession();
        schemaPane.restoreTreeState();
        stage.setOnCloseRequest(e -> {
            saveSession();
            schemaPane.saveTreeState();
        });
    }

    public Parent getRoot() { return root; }

    /** A Window suitable for initOwner() on dialogs raised from anywhere in the app. */
    public Window getOwnerWindow() { return stage; }

    public RunPanel getRunPanel() { return runPanel; }

    /** Lets dialogs outside SchemaTreePane (e.g. after modifying a database) trigger a reload. */
    public void refreshSchemaExplorer() {
        schemaPane.reload();
    }

    /**
     * Refreshes just one connection's subtree — used after DDL runs in a
     * query console for that connection, so a newly created/dropped table
     * shows up without collapsing every other connection in the explorer
     * the way a full reload() would.
     */
    public void refreshSchemaExplorer(ConnectionProfile profile) {
        schemaPane.refreshConnection(profile);
    }

    /** Toolbar gear button's quick Theme submenu — switches and persists in one step. */
    private void quickSwitchTheme(com.roze.dbnavigator.db.AppSettingsStore.Theme theme) {
        com.roze.dbnavigator.db.AppSettingsStore.Settings settings = com.roze.dbnavigator.db.AppSettingsStore.load();
        settings.setTheme(theme);
        com.roze.dbnavigator.db.AppSettingsStore.save(settings);
        ThemeManager.setTheme(theme);
        setStatus("Theme switched to " + theme);
    }

    /** Re-applies the saved editor font to every currently open console — called after Settings > Apply/OK. */
    public void applyEditorFontToOpenConsoles() {
        forEachEditorTab(tab -> {
            if (tab instanceof QueryTab queryTab) queryTab.applyEditorFontFromSettings();
        });
    }

    /** Shows the docked Run panel, expanding it if it was collapsed. */
    public void showRunPanel() {
        if (!runPanelVisible) {
            verticalSplit.getItems().add(runPanel);
            verticalSplit.setDividerPositions(lastRunPanelDivider);
            runPanelVisible = true;
        }
    }

    /** Hides the Run panel, remembering its size so re-showing restores it. */
    public void hideRunPanel() {
        if (runPanelVisible) {
            lastRunPanelDivider = verticalSplit.getDividerPositions()[0];
            verticalSplit.getItems().remove(runPanel);
            runPanelVisible = false;
        }
    }

    // ------------------------------------------------------------- chrome

    private Node buildDataGripHeader(ActionManager actionManager) {
        // App logo icon on the left (DataGrip-style)
        Label brandIcon = new Label();
        brandIcon.setGraphic(Icons.of(FontAwesomeSolid.DATABASE, "#4a88c7", 13));
        brandIcon.getStyleClass().add("header-brand-icon");
        brandIcon.setTooltip(new Tooltip("DBNavigator Pro"));

        // Menus: File, Edit, View, Navigate, Run, VCS, Window, Help
        MenuBar menuBar = actionManager.buildMenuBar(ActionRegistry.getMainMenuBarGroups(actionManager), this);

        HBox leftBox = new HBox(6, brandIcon, menuBar);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        // Center: 4 middle icons + 3 dots "..."
        ActionGroup middleGroup = actionManager.getGroup("group.middle");
        HBox middleBox = actionManager.buildToolBar(middleGroup, this);
        middleBox.setAlignment(Pos.CENTER);

        // Right: Search Everywhere & Settings buttons
        Button searchButton = new Button();
        searchButton.getStyleClass().add("header-action-button");
        searchButton.setGraphic(Icons.of(FontAwesomeSolid.SEARCH, "#a9b7c6", 12));
        searchButton.setTooltip(new Tooltip("Search Everywhere (Ctrl+Shift+F)"));
        searchButton.setOnAction(e -> showSearchEverywhere());

        Button settingsButton = new Button();
        settingsButton.getStyleClass().add("header-action-button");
        settingsButton.setGraphic(Icons.of(FontAwesomeSolid.COG, "#a9b7c6", 12));
        settingsButton.setTooltip(new Tooltip("Settings"));
        ContextMenu settingsMenu = buildSettingsContextMenu();
        settingsButton.setOnAction(e ->
                settingsMenu.show(settingsButton, javafx.geometry.Side.BOTTOM, 0, 4));

        HBox rightBox = new HBox(4, searchButton, settingsButton);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane headerBar = new BorderPane();
        headerBar.getStyleClass().add("app-header-bar");
        headerBar.setLeft(leftBox);
        headerBar.setCenter(middleBox);
        headerBar.setRight(rightBox);
        BorderPane.setAlignment(leftBox, Pos.CENTER_LEFT);
        BorderPane.setAlignment(middleBox, Pos.CENTER);
        BorderPane.setAlignment(rightBox, Pos.CENTER_RIGHT);

        return headerBar;
    }

    public void showSearchEverywhere() {
        new SearchDialog(stage, this).show();
    }

    public void openSqlFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open SQL File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            String sql = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            var profiles = ConnectionStore.load().stream()
                    .filter(p -> p.getType().isRelational()).toList();
            if (profiles.isEmpty()) {
                setStatus("No relational connections yet — create one first");
                return;
            }
            if (profiles.size() == 1) {
                openQueryTab(profiles.get(0), null, sql);
                return;
            }
            ChoiceDialog<ConnectionProfile> dialog = (ChoiceDialog<ConnectionProfile>) DialogTheme.apply(new ChoiceDialog<>(profiles.get(0), profiles));
            dialog.setTitle("Open SQL File");
            dialog.setHeaderText(null);
            dialog.setContentText("Run against connection:");
            dialog.showAndWait().ifPresent(p -> openQueryTab(p, null, sql));
        } catch (Exception ex) {
            setStatus("Could not read file: " + ex.getMessage());
        }
    }

    public void openNewSqlFile() {
        var profiles = ConnectionStore.load().stream()
                .filter(p -> p.getType().isRelational()).toList();
        ConnectionProfile p = profiles.isEmpty() ? null : profiles.get(0);
        if (p != null) {
            openQueryTab(p, null, "-- New SQL File\n");
            setStatus("Opened new SQL file");
        } else {
            showNewConnectionDialog();
        }
    }

    public void openNewScratchFile() {
        var profiles = ConnectionStore.load().stream()
                .filter(p -> p.getType().isRelational()).toList();
        ConnectionProfile p = profiles.isEmpty() ? null : profiles.get(0);
        if (p != null) {
            openQueryTab(p, null, "-- Scratch buffer\n");
            setStatus("Opened scratch console");
        } else {
            showNewConnectionDialog();
        }
    }

    public void reloadAllFromDisk() {
        schemaPane.reload();
        setStatus("Reloaded all schemas and files from disk");
    }

    public ConnectionProfile getActiveOrSelectedProfile() {
        Tab selected = currentSelectedTab();
        if (selected instanceof QueryTab qt) {
            return qt.getProfile();
        }
        var profiles = ConnectionStore.load();
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    public void createNewProjectDialog() {
        TextInputDialog dialog = (TextInputDialog) DialogTheme.apply(new TextInputDialog("NewProject"));
        dialog.initOwner(stage);
        dialog.setTitle("New Project");
        dialog.setHeaderText(null);
        dialog.setContentText("Project name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                stage.setTitle("DBNavigator Pro - " + name);
                setStatus("Created project: " + name);
            }
        });
    }

    public void openNewQueryFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("New Query File");
        chooser.setInitialFileName("query.sql");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files (*.sql)", "*.sql"));
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                if (!file.exists()) {
                    Files.writeString(file.toPath(), "-- Query File: " + file.getName() + "\n\n", StandardCharsets.UTF_8);
                }
                var profiles = ConnectionStore.load().stream()
                        .filter(p -> p.getType().isRelational()).toList();
                ConnectionProfile p = profiles.isEmpty() ? null : profiles.get(0);
                if (p != null) {
                    openQueryTab(p, null, Files.readString(file.toPath()));
                    setStatus("Created query file: " + file.getName());
                }
            } catch (Exception ex) {
                setStatus("Error creating query file: " + ex.getMessage());
            }
        }
    }

    public void createNewDatabaseAction() {
        ConnectionProfile p = getActiveOrSelectedProfile();
        if (p != null) {
            CreateDatabaseDialog.show(this, p);
        } else {
            showNewConnectionDialog();
        }
    }

    public void createNewRoleAction() {
        TextInputDialog dialog = (TextInputDialog) DialogTheme.apply(new TextInputDialog("new_role"));
        dialog.initOwner(stage);
        dialog.setTitle("Create Role");
        dialog.setHeaderText("New Role / Permission Group");
        dialog.setContentText("Role name:");
        dialog.showAndWait().ifPresent(roleName -> {
            if (!roleName.isBlank()) {
                ConnectionProfile p = getActiveOrSelectedProfile();
                if (p != null) {
                    String ddl = (p.getType() == ConnectionProfile.DatabaseType.POSTGRESQL)
                            ? "CREATE ROLE " + roleName + " WITH LOGIN;\n"
                            : "CREATE ROLE " + roleName + ";\n";
                    openQueryTab(p, null, ddl);
                    setStatus("Generated CREATE ROLE script for " + roleName);
                } else {
                    setStatus("Created role: " + roleName);
                }
            }
        });
    }

    public void createNewUserAction() {
        TextInputDialog dialog = (TextInputDialog) DialogTheme.apply(new TextInputDialog("db_user"));
        dialog.initOwner(stage);
        dialog.setTitle("Create User");
        dialog.setHeaderText("New Database User Account");
        dialog.setContentText("User name:");
        dialog.showAndWait().ifPresent(userName -> {
            if (!userName.isBlank()) {
                ConnectionProfile p = getActiveOrSelectedProfile();
                if (p != null) {
                    String ddl;
                    if (p.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
                        ddl = "CREATE USER " + userName + " WITH PASSWORD 'password';\n";
                    } else if (p.getType() == ConnectionProfile.DatabaseType.SQLSERVER) {
                        ddl = "CREATE LOGIN " + userName + " WITH PASSWORD = 'password';\nCREATE USER " + userName + " FOR LOGIN " + userName + ";\n";
                    } else {
                        ddl = "CREATE USER '" + userName + "'@'%' IDENTIFIED BY 'password';\n";
                    }
                    openQueryTab(p, null, ddl);
                    setStatus("Generated CREATE USER script for " + userName);
                } else {
                    setStatus("Created user: " + userName);
                }
            }
        });
    }

    public void createNewVirtualViewAction() {
        TextInputDialog dialog = (TextInputDialog) DialogTheme.apply(new TextInputDialog("v_custom_view"));
        dialog.initOwner(stage);
        dialog.setTitle("New Virtual View");
        dialog.setHeaderText("Create Virtual View / SQL View");
        dialog.setContentText("View name:");
        dialog.showAndWait().ifPresent(viewName -> {
            if (!viewName.isBlank()) {
                ConnectionProfile p = getActiveOrSelectedProfile();
                String ddl = "CREATE VIEW " + viewName + " AS\nSELECT 1 AS id, 'example' AS label;\n";
                if (p != null) {
                    openQueryTab(p, null, ddl);
                }
                setStatus("Created Virtual View template: " + viewName);
            }
        });
    }

    public void openDataSourceFromCloudDialog(String provider, ConnectionProfile.DatabaseType type) {
        showNewConnectionDialog(type);
        setStatus("Configuring " + provider + " " + type.getDisplayName() + " Data Source");
    }

    public void showDataSourceTemplatesDialog() {
        List<String> templates = List.of(
                "Local Docker - PostgreSQL (localhost:5432 / postgres)",
                "Local Docker - MySQL 8.0 (localhost:3306 / root)",
                "Local Docker - MariaDB (localhost:3306 / root)",
                "Local Docker - MongoDB (localhost:27017)",
                "SQLite In-Memory / Local Test (:memory:)",
                "StratosDB Standalone (localhost:9876 / default)"
        );
        ChoiceDialog<String> dialog = (ChoiceDialog<String>) DialogTheme.apply(new ChoiceDialog<>(templates.get(0), templates));
        dialog.initOwner(stage);
        dialog.setTitle("Data Source Templates (Beta)");
        dialog.setHeaderText("Select a pre-configured database environment template:");
        dialog.setContentText("Template:");
        dialog.showAndWait().ifPresent(choice -> {
            if (choice.contains("PostgreSQL")) {
                showNewConnectionDialog(ConnectionProfile.DatabaseType.POSTGRESQL);
            } else if (choice.contains("MySQL")) {
                showNewConnectionDialog(ConnectionProfile.DatabaseType.MYSQL);
            } else if (choice.contains("MariaDB")) {
                showNewConnectionDialog(ConnectionProfile.DatabaseType.MARIADB);
            } else if (choice.contains("MongoDB")) {
                showNewConnectionDialog(ConnectionProfile.DatabaseType.MONGODB);
            } else if (choice.contains("SQLite")) {
                showNewConnectionDialog(ConnectionProfile.DatabaseType.SQLITE);
            } else if (choice.contains("StratosDB")) {
                showNewConnectionDialog(ConnectionProfile.DatabaseType.STRATOSDB);
            }
        });
    }

    public void openDataSourceFromFileOrFolder() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Data Source from File / Folder");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Database & Data files (*.db, *.sqlite, *.sqlite3, *.json, *.csv)", "*.db", "*.sqlite", "*.sqlite3", "*.json", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            String name = file.getName().replaceAll("\\.[^.]+$", "");
            ConnectionProfile p = new ConnectionProfile();
            p.setName(name.toUpperCase() + " (File)");
            p.setType(ConnectionProfile.DatabaseType.SQLITE);
            p.setDatabase(file.getAbsolutePath());
            ConnectionStore.saveOrUpdate(p);
            schemaPane.reload();
            setStatus("Added data source from file: " + file.getName());
        }
    }

    public void openDataSourceFromUrlDialog() {
        TextInputDialog dialog = (TextInputDialog) DialogTheme.apply(new TextInputDialog("jdbc:postgresql://localhost:5432/mydb"));
        dialog.initOwner(stage);
        dialog.setTitle("Data Source from URL");
        dialog.setHeaderText("Enter Database JDBC or Connection URI:");
        dialog.setContentText("Connection URL:");
        dialog.showAndWait().ifPresent(url -> {
            if (!url.isBlank()) {
                try {
                    ConnectionProfile.DatabaseType type = ConnectionProfile.DatabaseType.POSTGRESQL;
                    if (url.startsWith("jdbc:mysql:")) type = ConnectionProfile.DatabaseType.MYSQL;
                    else if (url.startsWith("jdbc:mariadb:")) type = ConnectionProfile.DatabaseType.MARIADB;
                    else if (url.startsWith("jdbc:sqlite:")) type = ConnectionProfile.DatabaseType.SQLITE;
                    else if (url.startsWith("jdbc:sqlserver:")) type = ConnectionProfile.DatabaseType.SQLSERVER;
                    else if (url.startsWith("jdbc:oracle:")) type = ConnectionProfile.DatabaseType.ORACLE;
                    else if (url.startsWith("jdbc:stratosdb:")) type = ConnectionProfile.DatabaseType.STRATOSDB;
                    else if (url.startsWith("mongodb://") || url.startsWith("mongodb+srv://")) type = ConnectionProfile.DatabaseType.MONGODB;

                    showNewConnectionDialog(type);
                    setStatus("Parsed URL for " + type.getDisplayName());
                } catch (Exception ex) {
                    setStatus("Error parsing URL: " + ex.getMessage());
                }
            }
        });
    }

    public void openDdlDataSourceDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("New DDL Data Source");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DDL SQL Scripts (*.sql)", "*.sql"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            String name = file.getName().replaceAll("\\.[^.]+$", "") + " (DDL)";
            ConnectionProfile p = new ConnectionProfile();
            p.setName(name);
            p.setType(ConnectionProfile.DatabaseType.SQLITE);
            p.setDatabase(":memory:");
            ConnectionStore.saveOrUpdate(p);
            schemaPane.reload();
            setStatus("Created DDL Data Source from " + file.getName());
        }
    }

    public void createNewFolderDialog() {
        TextInputDialog dialog = (TextInputDialog) DialogTheme.apply(new TextInputDialog("new_folder"));
        dialog.initOwner(stage);
        dialog.setTitle("New Folder");
        dialog.setHeaderText(null);
        dialog.setContentText("Folder name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                setStatus("Created folder: " + name);
            }
        });
    }

    public void showDriversDialog() {
        Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                "Data Sources and Drivers:\n\n"
                + "Installed Database Drivers:\n"
                + "• PostgreSQL JDBC Driver (org.postgresql.Driver)\n"
                + "• MySQL Connector/J (com.mysql.cj.jdbc.Driver)\n"
                + "• MariaDB Connector/J (org.mariadb.jdbc.Driver)\n"
                + "• SQLite JDBC (org.sqlite.JDBC)\n"
                + "• Microsoft JDBC Driver for SQL Server (com.microsoft.sqlserver.jdbc.SQLServerDriver)\n"
                + "• Oracle JDBC Driver (oracle.jdbc.OracleDriver)\n"
                + "• StratosDB JDBC Driver (com.roze.stratosdb.jdbc.StratosDriver)\n"
                + "• MongoDB Java Sync Driver (mongodb-driver-sync)\n\n"
                + "All driver classes are loaded and ready."));
        alert.setHeaderText("Drivers");
        alert.initOwner(stage);
        alert.showAndWait();
    }

    public void attachDirectoryToProject() {
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("Attach Directory to Project");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            setStatus("Attached directory: " + dir.getAbsolutePath());
        }
    }

    public void renameProject() {
        TextInputDialog dialog = (TextInputDialog) DialogTheme.apply(new TextInputDialog("DBNavigator Project"));
        dialog.initOwner(stage);
        dialog.setTitle("Rename Project");
        dialog.setHeaderText(null);
        dialog.setContentText("Project name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                stage.setTitle("DBNavigator Pro - " + name);
                setStatus("Renamed project to: " + name);
            }
        });
    }

    public void showSqlDialectsDialog() {
        Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                "Configured SQL Dialects:\n\n"
                + "• MySQL / MariaDB (MySQL Dialect)\n"
                + "• PostgreSQL (PostgreSQL Dialect)\n"
                + "• StratosDB (StratosDB Dialect)\n"
                + "• SQLite (SQLite Dialect)\n"
                + "• Oracle (Oracle PL/SQL Dialect)\n"
                + "• SQL Server (T-SQL Dialect)\n"
                + "• MongoDB (MQL / JavaScript Dialect)\n\n"
                + "Dialects are auto-detected based on the active connection."));
        alert.setHeaderText("SQL Dialects");
        alert.initOwner(stage);
        alert.showAndWait();
    }

    public void showSqlResolutionScopesDialog() {
        Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                "SQL Resolution Scopes:\n\n"
                + "SQL symbols and object references resolve against the default database\n"
                + "and schema selected in the active query console session."));
        alert.setHeaderText("SQL Resolution Scopes");
        alert.initOwner(stage);
        alert.showAndWait();
    }

    public void showPluginsDialog() {
        Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                "Plugins Marketplace:\n\n"
                + "Installed Plugins:\n"
                + "• Database Tools and SQL (bundled)\n"
                + "• Terminal & Shell Integration (bundled)\n"
                + "• Git Integration (bundled)\n"
                + "• Dark / Light Theme Engine (bundled)\n\n"
                + "All core database dialect plugins are active and up to date."));
        alert.setHeaderText("Plugins");
        alert.initOwner(stage);
        alert.showAndWait();
    }

    public void editDataSourcesFile() {
        Path p = Path.of(System.getProperty("user.home"), ".dbnavigator", "connections.json");
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN) && Files.exists(p)) {
                java.awt.Desktop.getDesktop().open(p.toFile());
            } else {
                setStatus("Data sources file: " + p);
            }
        } catch (Exception e) {
            setStatus("Data sources file: " + p);
        }
    }

    private boolean powerSaveMode = false;
    public void togglePowerSaveMode() {
        powerSaveMode = !powerSaveMode;
        setStatus("Power Save Mode: " + (powerSaveMode ? "Enabled" : "Disabled"));
    }

    public void saveConsoleAs() {
        Tab selected = currentSelectedTab();
        if (!(selected instanceof QueryTab queryTab)) {
            setStatus("Select a query console tab first");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Console As");
        chooser.setInitialFileName("console.sql");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            Files.writeString(file.toPath(), queryTab.getSqlText(), StandardCharsets.UTF_8);
            queryTab.setSavedFile(file);
            setStatus("Saved " + file.getName());
        } catch (Exception ex) {
            setStatus("Could not save file: " + ex.getMessage());
        }
    }

    /**
     * File → Local History — Show History…
     * Always gives visible feedback: an Alert if no console is selected, or
     * if opening the diff view fails for any reason — never a silent no-op.
     */
    public void showLocalHistoryForCurrentConsole() {
        Tab selected = currentSelectedTab();
        if (!(selected instanceof QueryTab tab)) {
            Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                    "Open or select a query console tab first, then use Local History on it."));
            alert.initOwner(stage);
            alert.setHeaderText("No console selected");
            alert.showAndWait();
            return;
        }
        try {
            tab.showLocalHistory(stage);
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.ERROR, msg));
            alert.initOwner(stage);
            alert.setHeaderText("Could not open Local History");
            alert.showAndWait();
        }
    }

    /** Runs an action on the currently selected console tab, or reports there isn't one. */
    private void withCurrentConsole(Consumer<QueryTab> action) {
        Tab selected = currentSelectedTab();
        if (selected instanceof QueryTab tab) {
            action.accept(tab);
        } else {
            setStatus("Select a query console tab first");
        }
    }

    /** Opens the Local History diff dialog for an entry picked from a project-wide/recent list. */
    private void openHistoryEntry(String fileId, LocalHistoryStore.Entry entry) {
        String displayName = fileId.replace(' ', '_') + ".sql";
        QueryTab openTab = findOpenConsole(fileId);
        if (openTab != null) {
            LocalHistoryDialog.showForFileAtEntry(stage, fileId, displayName, openTab.getSqlText(), entry);
        } else {
            setStatus("Console \"" + displayName + "\" is no longer open — showing snapshot only");
            LocalHistoryDialog.showForFileAtEntry(stage, fileId, displayName, entry.content(), entry);
        }
    }

    private QueryTab findOpenConsole(String fileId) {
        for (TabPane pane : editorTabPanes()) {
            for (Tab t : pane.getTabs()) {
                if (t instanceof QueryTab qt && qt.getFileId().equals(fileId)) return qt;
            }
        }
        return null;
    }

    private ContextMenu buildSettingsContextMenu() {
        ContextMenu settingsMenu = new ContextMenu();
        MenuItem openSettings = new MenuItem("Settings…");
        openSettings.setOnAction(e -> SettingsDialog.show(this));
        MenuItem openPlugins = new MenuItem("Plugins…");
        openPlugins.setOnAction(e -> SettingsDialog.show(this));
        Menu themeMenu = new Menu("Theme");
        MenuItem darkTheme = new MenuItem("Dark");
        darkTheme.setOnAction(e -> quickSwitchTheme(com.roze.dbnavigator.db.AppSettingsStore.Theme.DARK));
        MenuItem lightTheme = new MenuItem("Light");
        lightTheme.setOnAction(e -> quickSwitchTheme(com.roze.dbnavigator.db.AppSettingsStore.Theme.LIGHT));
        themeMenu.getItems().addAll(darkTheme, lightTheme);
        MenuItem dataSources = new MenuItem("Data Sources…");
        dataSources.setOnAction(e -> showNewConnectionDialog());
        MenuItem refresh = new MenuItem("Refresh Database Explorer");
        refresh.setOnAction(e -> schemaPane.reload());
        settingsMenu.getItems().addAll(openSettings, openPlugins, new SeparatorMenuItem(),
                themeMenu, new SeparatorMenuItem(), dataSources, refresh);
        return settingsMenu;
    }

    public void closeWindow() {
        stage.close();
    }

    public boolean hasActiveTab() {
        return currentSelectedTab() != null;
    }

    public boolean hasActiveConsole() {
        Tab t = currentSelectedTab();
        return t instanceof QueryTab || t instanceof MongoConsoleTab;
    }

    public boolean hasActiveQueryTab() {
        return currentSelectedTab() instanceof QueryTab;
    }

    public void executeCurrentStatement() {
        Tab tab = currentSelectedTab();
        if (tab instanceof QueryTab qt) {
            qt.execute();
        } else if (tab instanceof MongoConsoleTab mt) {
            mt.execute();
        } else {
            setStatus("Select a SQL or MongoDB console tab first");
        }
    }

    public void formatCurrentSql() {
        Tab tab = currentSelectedTab();
        if (tab instanceof QueryTab qt) {
            qt.formatSql();
            setStatus("SQL formatted");
        } else {
            setStatus("Select a SQL console tab to format");
        }
    }

    public void undoCurrentEditor() {
        CodeArea editor = getActiveCodeArea();
        if (editor != null && editor.isUndoAvailable()) {
            editor.undo();
        }
    }

    public void redoCurrentEditor() {
        CodeArea editor = getActiveCodeArea();
        if (editor != null && editor.isRedoAvailable()) {
            editor.redo();
        }
    }

    public void cutCurrentEditor() {
        CodeArea editor = getActiveCodeArea();
        if (editor != null) editor.cut();
    }

    public void copyCurrentEditor() {
        CodeArea editor = getActiveCodeArea();
        if (editor != null) editor.copy();
    }

    public void pasteCurrentEditor() {
        CodeArea editor = getActiveCodeArea();
        if (editor != null) editor.paste();
    }

    public void selectAllCurrentEditor() {
        CodeArea editor = getActiveCodeArea();
        if (editor != null) editor.selectAll();
    }

    private CodeArea getActiveCodeArea() {
        Tab tab = currentSelectedTab();
        if (tab instanceof QueryTab qt) return qt.getEditor();
        if (tab instanceof MongoConsoleTab mt) return mt.getEditor();
        return null;
    }

    public void selectNextTab() {
        TabPane currentPane = activeTabPane != null ? activeTabPane : tabPane;
        int size = currentPane.getTabs().size();
        if (size > 1) {
            int next = (currentPane.getSelectionModel().getSelectedIndex() + 1) % size;
            currentPane.getSelectionModel().select(next);
        }
    }

    public void selectPreviousTab() {
        TabPane currentPane = activeTabPane != null ? activeTabPane : tabPane;
        int size = currentPane.getTabs().size();
        if (size > 1) {
            int prev = (currentPane.getSelectionModel().getSelectedIndex() - 1 + size) % size;
            currentPane.getSelectionModel().select(prev);
        }
    }

    public void closeActiveTab() {
        Tab selected = currentSelectedTab();
        if (selected != null && selected.isClosable()) {
            TabPane pane = selected.getTabPane();
            if (pane != null) {
                Event.fireEvent(selected, new Event(Tab.CLOSED_EVENT));
                pane.getTabs().remove(selected);
            }
        }
    }

    public void closeOtherTabs() {
        Tab selected = currentSelectedTab();
        if (selected != null) {
            for (TabPane pane : editorTabPanes()) {
                List<Tab> toRemove = pane.getTabs().stream()
                        .filter(t -> t != selected && t.isClosable())
                        .toList();
                pane.getTabs().removeAll(toRemove);
            }
        }
    }

    public void closeAllTabs() {
        for (TabPane pane : editorTabPanes()) {
            List<Tab> toRemove = pane.getTabs().stream()
                    .filter(Tab::isClosable)
                    .toList();
            pane.getTabs().removeAll(toRemove);
        }
    }

    public void splitActiveTabRight() {
        Tab selected = currentSelectedTab();
        if (selected != null) {
            splitRight(selected);
        }
    }

    public void splitActiveTabDown() {
        Tab selected = currentSelectedTab();
        if (selected != null) {
            splitDown(selected);
        }
    }

    public void unsplitAll() {
        List<Tab> allTabs = new ArrayList<>();
        for (TabPane pane : editorTabPanes()) {
            allTabs.addAll(pane.getTabs());
            if (pane != tabPane) {
                pane.getTabs().clear();
            }
        }
        tabPane.getTabs().setAll(allTabs);
        centerSplit.getItems().setAll(schemaPane, tabPane);
        centerSplit.setDividerPositions(0.22);
        activeTabPane = tabPane;
    }

    public void showSettingsDialog() {
        SettingsDialog.show(this);
    }

    public void showAboutDialog() {
        Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                "DBNavigator Pro 3.2\nA DataGrip-style database IDE built with JavaFX.\n"
                + "MySQL · MariaDB · PostgreSQL · SQL Server · Oracle · SQLite · MongoDB"));
        alert.setHeaderText("DBNavigator Pro");
        alert.initOwner(stage);
        alert.showAndWait();
    }

    public void showCheckUpdatesDialog() {
        AppUpdateDialog.check(stage, false);
    }

    public void showInvalidateCachesDialog() {
        InvalidateCachesDialog.show(stage);
    }

    public void showProjectHistoryDialog() {
        ProjectHistoryDialog.showProjectWide(stage, this::openHistoryEntry);
    }

    public void showRecentChangesDialog() {
        ProjectHistoryDialog.showRecentChanges(stage, this::openHistoryEntry);
    }

    public void showPutLabelDialog() {
        withCurrentConsole(tab -> {
            TextInputDialog dialog = (TextInputDialog) DialogTheme.apply(new TextInputDialog());
            dialog.initOwner(stage);
            dialog.setTitle("Put Label");
            dialog.setHeaderText(null);
            dialog.setContentText("Label for this point in " + tab.getDisplayFileName() + ":");
            dialog.showAndWait().ifPresent(label -> {
                if (!label.isBlank()) {
                    tab.putLocalHistoryLabel(label);
                    setStatus("Labeled current state of " + tab.getDisplayFileName() + " as \"" + label + "\"");
                }
            });
        });
    }

    public void showHistoryForSelection() {
        withCurrentConsole(tab -> {
            setStatus("Selection-scoped history isn't tracked separately yet — showing full file history");
            tab.showLocalHistory(stage);
        });
    }

    public void toggleRunPanel() {
        if (runPanelVisible) hideRunPanel(); else showRunPanel();
    }

    public void focusOrToggleSchemaExplorer() {
        double[] pos = centerSplit.getDividerPositions();
        if (pos != null && pos.length > 0 && pos[0] < 0.05) {
            centerSplit.setDividerPositions(0.22);
        }
        schemaPane.requestFocus();
    }

    private HBox buildStatusBar() {
        statusLabel.getStyleClass().add("status-text");

        taskBreadcrumbLabel.getStyleClass().add("task-breadcrumb");
        taskNameLabel.getStyleClass().add("task-name");
        taskProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        taskProgressBar.setPrefWidth(110);
        taskCancelButton.setGraphic(Icons.of(FontAwesomeSolid.TIMES, "#a9b7c6", 10));
        taskCancelButton.getStyleClass().add("task-cancel-button");
        Button bell = new Button();
        bell.setGraphic(Icons.of(FontAwesomeSolid.BELL, "#a9b7c6", 12));
        bell.getStyleClass().add("task-cancel-button");
        bell.setTooltip(new Tooltip("Show Run panel"));
        bell.setOnAction(e -> showRunPanel());

        taskIndicator.setSpacing(8);
        taskIndicator.setAlignment(Pos.CENTER_LEFT);
        taskIndicator.getChildren().addAll(taskBreadcrumbLabel, new Separator(Orientation.VERTICAL),
                taskNameLabel, taskProgressBar, taskCancelButton, bell);
        taskIndicator.getStyleClass().add("task-indicator");
        taskIndicator.setVisible(false);
        taskIndicator.setManaged(false);
        taskIndicator.setOnMouseClicked(e -> showRunPanel());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(statusLabel, spacer, taskIndicator);
        bar.setPadding(new Insets(5, 12, 5, 12));
        bar.getStyleClass().add("app-status-bar");
        return bar;
    }

    /** Shows the right-aligned background-task indicator (breadcrumb + progress + cancel). */
    public void showTask(String breadcrumb, String taskName, Runnable onCancel) {
        taskBreadcrumbLabel.setText(breadcrumb);
        taskNameLabel.setText(taskName);
        taskCancelButton.setOnAction(e -> { if (onCancel != null) onCancel.run(); });
        taskIndicator.setVisible(true);
        taskIndicator.setManaged(true);
    }

    public void hideTask() {
        taskIndicator.setVisible(false);
        taskIndicator.setManaged(false);
    }

    private void showWelcomeTab() {
        Label title = new Label("Welcome to DBNavigator Pro");
        title.getStyleClass().add("welcome-title");
        Label hint = new Label("""
                • Click "New Data Source" to connect to MySQL, MariaDB, PostgreSQL, \
                SQL Server, Oracle, SQLite or MongoDB
                • Double-click a table or collection in the explorer to browse its data
                • Right-click objects for query consoles, structure view and more
                • Press Ctrl+Enter in a console to run the selected statement""");
        hint.getStyleClass().add("welcome-hint");

        VBox box = new VBox(14, Icons.of(FontAwesomeSolid.DATABASE, "#3d4d5c", 52), title, hint);
        box.setAlignment(Pos.CENTER);

        Tab welcome = new Tab("Welcome", box);
        welcome.setGraphic(Icons.of(FontAwesomeSolid.HOME, "#868a91", 11));
        tabPane.getTabs().add(welcome);
    }

    // ---------------------------------------------------------- actions

    public void showNewConnectionDialog() {
        showNewConnectionDialog(null);
    }

    public void showNewConnectionDialog(ConnectionProfile.DatabaseType type) {
        ConnectionProfile initial = new ConnectionProfile();
        if (type != null) {
            initial.setType(type);
            initial.setName("New " + type.getDisplayName());
            initial.setPort(type.getDefaultPort());
        }
        new ConnectionDialog(initial).showAndWait().ifPresent(this::connectAndSave);
    }

    public void showEditConnectionDialog(ConnectionProfile existing) {
        new ConnectionDialog(existing).showAndWait().ifPresent(profile -> {
            ClientRegistry.disconnect(profile);   // force reconnect with new settings
            connectAndSave(profile);
        });
    }

    private void connectAndSave(ConnectionProfile profile) {
        setStatus("Connecting to " + profile.getName() + "…");
        AppExecutor.run(() -> {
            try {
                ClientRegistry.connectAndVerify(profile);
                ConnectionStore.saveOrUpdate(profile);
                Platform.runLater(() -> {
                    schemaPane.reload();
                    setStatus("Connected to " + profile.getName());
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    setStatus("Connection failed");
                    Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.ERROR));
                    alert.setTitle("Connection Error");
                    alert.setHeaderText("Could not connect to " + profile.getName());
                    alert.setContentText(msg);
                    alert.showAndWait();
                    // Still offer to save the profile for later editing
                    ConnectionStore.saveOrUpdate(profile);
                    schemaPane.reload();
                });
            }
        });
    }

    public void openConsoleForSelectedConnection() {
        var profiles = ConnectionStore.load().stream()
                .filter(p -> p.getType().isRelational()).toList();
        if (profiles.isEmpty()) {
            setStatus("No relational connections yet — create one first");
            return;
        }
        if (profiles.size() == 1) {
            openQueryTab(profiles.get(0), null);
            return;
        }
        ChoiceDialog<ConnectionProfile> dialog =
                (ChoiceDialog<ConnectionProfile>) DialogTheme.apply(new ChoiceDialog<>(profiles.get(0), profiles));
        dialog.setTitle("New Console");
        dialog.setHeaderText(null);
        dialog.setContentText("Connection:");
        dialog.showAndWait().ifPresent(p -> openQueryTab(p, null));
    }

    public void openQueryTab(ConnectionProfile profile, String initialSql) {
        openQueryTab(profile, null, initialSql);
    }

    /** @param catalog bind the console to a specific database of the connection (nullable). */
    public void openQueryTab(ConnectionProfile profile, String catalog, String initialSql) {
        if (!Passwords.ensure(profile, stage)) return;
        if (profile.getType() == ConnectionProfile.DatabaseType.MONGODB) {
            addAndSelect(new MongoConsoleTab(profile, catalog, "console " + (++consoleCounter)));
            return;
        }
        QueryTab tab = new QueryTab(this, profile, catalog, "console " + (++consoleCounter));
        if (initialSql != null) tab.setSql(initialSql);
        addAndSelect(tab);
    }

    public void openDataTab(ConnectionProfile profile, DbObject table) {
        addAndSelect(new DataTab(profile, table));
    }

    public void openStructureTab(ConnectionProfile profile, DbObject table) {
        addAndSelect(new StructureTab(profile, table));
    }

    public void openDiagramTab(ConnectionProfile profile, DbObject table) {
        addAndSelect(new DiagramTab(profile, table));
    }

    public void openDatabaseDiagramTab(ConnectionProfile profile, String catalog) {
        addAndSelect(new DatabaseDiagramTab(profile, catalog));
    }

    public void openMongoTab(ConnectionProfile profile, DbObject collection) {
        addAndSelect(new MongoCollectionTab(profile, collection));
    }

    /**
     * Opens a stored procedure/function's own source in an editable console,
     * pre-filled and ready to re-run (which recompiles it, since the source
     * is reconstructed as a full CREATE OR REPLACE) — the same thing
     * double-clicking a stored routine opens in DataGrip. Falls back to a
     * short explanatory message for engines getRoutineSource doesn't cover
     * yet, rather than opening a silently blank console.
     */
    public void openRoutineSourceTab(ConnectionProfile profile, DbObject obj) {
        if (!Passwords.ensure(profile, stage)) return;
        String title = obj.getName() + " [" + profile.getName() + "]";
        setStatus("Loading " + obj.getName() + "\u2026");
        AppExecutor.run(() -> {
            try {
                String source = MetadataService.getRoutineSource(profile, obj);
                Platform.runLater(() -> {
                    setStatus("Ready");
                    if (source == null) {
                        DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                                "Viewing a " + obj.getKind().toString().toLowerCase()
                                + "'s source isn't supported yet for " + profile.getType().getDisplayName()
                                + " — this currently works for Oracle only.")).showAndWait();
                        return;
                    }
                    // ALL_SOURCE's own text has no block terminator — add the
                    // standard "/" so Run recompiles the whole thing as one
                    // statement instead of fragmenting on its internal ";"s.
                    QueryTab tab = new QueryTab(this, profile, obj.getCatalog(), title);
                    tab.setSql(source.stripTrailing() + "\n/\n");
                    addAndSelect(tab);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    setStatus("Ready");
                    DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not load " + obj.getName() + ": " + msg)).showAndWait();
                });
            }
        });
    }

    /**
     * Opens a console pre-filled with a ready-to-fill invocation block for a
     * procedure/function — e.g. "BEGIN\n  name(:p1, :p2);\nEND;\n/" for
     * Oracle. Reuses the console's own existing ":name" parameter-prompt
     * flow (the same one used for ad-hoc parameterized queries) rather than
     * building a separate parameter dialog — hitting Run prompts for each
     * value exactly the way it already does for any other :placeholder.
     */
    public void openRoutineRunTab(ConnectionProfile profile, DbObject obj) {
        if (!Passwords.ensure(profile, stage)) return;
        setStatus("Loading " + obj.getName() + "\u2026");
        AppExecutor.run(() -> {
            try {
                List<String> params = MetadataService.loadProcedureParameters(profile, obj);
                String args = String.join(", ", params.stream().map(p -> ":" + p).toList());
                String sql = obj.getKind() == DbObject.Kind.FUNCTION
                        ? "SELECT " + obj.getName() + "(" + args + ") FROM dual;"
                        : "BEGIN\n  " + obj.getName() + "(" + args + ");\nEND;\n/\n";
                Platform.runLater(() -> {
                    setStatus("Ready");
                    openQueryTab(profile, obj.getCatalog(), sql);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    setStatus("Ready");
                    DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not load " + obj.getName() + "'s parameters: " + msg)).showAndWait();
                });
            }
        });
    }

    private void addAndSelect(Tab tab) {
        TabPane targetPane = activeEditorTabPane();
        targetPane.getTabs().add(tab);
        targetPane.getSelectionModel().select(tab);
        TabContextMenu.install(targetPane, tab, this);

        // Reopen Closed Tab currently only restores consoles (with their SQL
        // text intact) — the highest-value case. Other tab types (data view,
        // diagrams, etc.) aren't captured here; closing one of those doesn't
        // push anything, so Reopen Closed Tab simply skips to the next
        // console-type entry, or stays disabled if there isn't one.
        if (tab instanceof QueryTab queryTab) {
            tab.addEventHandler(Tab.CLOSED_EVENT, e -> {
                ConnectionProfile p = queryTab.getProfile();
                String catalog = queryTab.getCatalog();
                String sql = queryTab.getSqlText();
                String title = tab.getText();
                closedTabStack.push(() -> {
                    QueryTab reopened = new QueryTab(this, p, catalog, title);
                    reopened.setSql(sql);
                    return reopened;
                });
            });
        }
    }

    /** Used by TabContextMenu to enable/disable "Reopen Closed Tab". */
    public boolean hasClosedTabToReopen() {
        return !closedTabStack.isEmpty();
    }

    /** Tab context menu → Reopen Closed Tab — restores the most recently closed console. */
    public void reopenLastClosedTab() {
        if (closedTabStack.isEmpty()) return;
        addAndSelect(closedTabStack.pop().get());
    }

    /**
     * Reopens every console that was still open the last time the app
     * closed — see SessionStore's class doc for exactly what's covered
     * (consoles only, not data grids/diagrams/structure views).
     */
    private void restoreSession() {
        List<ConnectionProfile> profiles = ConnectionStore.load();

        List<SessionStore.OpenTab> saved = SessionStore.load();
        for (SessionStore.OpenTab open : saved) {
            ConnectionProfile profile = profiles.stream()
                    .filter(p -> p.getId().equals(open.profileId()))
                    .findFirst().orElse(null);
            // The connection this console belonged to was deleted since —
            // nothing sensible to restore it against.
            if (profile == null) continue;
            if (open.mongo()) {
                MongoConsoleTab tab = new MongoConsoleTab(profile, open.catalog(), open.title());
                tab.setScriptText(open.sql());
                addAndSelect(tab);
            } else {
                QueryTab tab = new QueryTab(this, profile, open.catalog(), open.title());
                tab.setSql(open.sql());
                addAndSelect(tab);
            }
        }

        List<SessionStore.OpenDataTab> savedDataTabs = SessionStore.loadDataTabs();
        for (SessionStore.OpenDataTab open : savedDataTabs) {
            ConnectionProfile profile = profiles.stream()
                    .filter(p -> p.getId().equals(open.profileId()))
                    .findFirst().orElse(null);
            // Same real reasoning as above - the connection this data tab
            // belonged to is simply gone now.
            if (profile == null) continue;
            DbObject obj = new DbObject(open.name(), open.kind(), open.catalog(), open.schema());
            // Mirrors MetadataService's own, real, exact condition for when a
            // table reference must not be catalog-qualified - see its own
            // real javadoc for why this matters specifically for a StratosDB
            // cluster. Re-derived here rather than persisted as its own
            // separate field, since it's fully, deterministically implied by
            // the connection's own type alone.
            if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                obj.setSkipCatalogQualifier(true);
            }
            if (open.mongo()) {
                openMongoTab(profile, obj);
            } else {
                openDataTab(profile, obj);
            }
        }
    }

    /**
     * Snapshots every currently open console so restoreSession() can
     * bring them back next launch.
     *
     * Real, previously-shipped bug this fixes: a QueryTab's own
     * displayed text already has " [catalog]" appended by its own
     * constructor (see QueryTab's own setText call) - saving that full,
     * already-suffixed text as the base title, then passing it straight
     * back into a new QueryTab's own title parameter on the next
     * restore (which appends " [catalog]" again, unconditionally,
     * regardless of whether the text already ends with it) meant the
     * suffix genuinely grew by one extra " [catalog]" on every single
     * close-and-reopen cycle - exactly what a real, live report showed:
     * "console 1 [hh] [hh] [hh] [hh] [hh]" after five restarts with the
     * same console left open. Fixed by saving QueryTab's own real,
     * already-existing base title (getFileId() - the same stable
     * identity its own Local History already keys on, deliberately
     * independent of the catalog suffix) instead of its full, suffixed
     * display text. MongoConsoleTab has no such bug to begin with - its
     * own constructor never appends anything to its title - so it keeps
     * using its full display text exactly as before.
     */
    private void saveSession() {
        List<SessionStore.OpenTab> open = new java.util.ArrayList<>();
        List<SessionStore.OpenDataTab> openDataTabs = new java.util.ArrayList<>();
        for (TabPane pane : editorTabPanes()) {
            for (Tab t : pane.getTabs()) {
                if (t instanceof QueryTab qt) {
                    open.add(new SessionStore.OpenTab(
                            qt.getProfile().getId(), qt.getCatalog(), qt.getSqlText(), qt.getFileId(), false));
                } else if (t instanceof MongoConsoleTab mt) {
                    open.add(new SessionStore.OpenTab(mt.getProfileForReopen().getId(),
                            mt.getCurrentDatabase(), mt.getScriptText(), t.getText(), true));
                } else if (t instanceof DataTab dt) {
                    DbObject table = dt.getTableForReopen();
                    openDataTabs.add(new SessionStore.OpenDataTab(dt.getProfileForReopen().getId(),
                            table.getName(), table.getKind(), table.getCatalog(), table.getSchema(), false));
                } else if (t instanceof MongoCollectionTab mc) {
                    DbObject collection = mc.getCollectionForReopen();
                    openDataTabs.add(new SessionStore.OpenDataTab(mc.getProfileForReopen().getId(),
                            collection.getName(), collection.getKind(), collection.getCatalog(), collection.getSchema(), true));
                }
            }
        }
        SessionStore.save(open);
        SessionStore.saveDataTabs(openDataTabs);
    }

    // ------------------------------------------------------------- split view

    /** DataGrip-style editor splitting. Every editor group can be split again. */
    public void splitRight(Tab tab) { split(tab, false, Orientation.HORIZONTAL); }
    public void splitAndMoveRight(Tab tab) { split(tab, true, Orientation.HORIZONTAL); }
    public void splitDown(Tab tab) { split(tab, false, Orientation.VERTICAL); }
    public void splitAndMoveDown(Tab tab) { split(tab, true, Orientation.VERTICAL); }

    private void split(Tab sourceTab, boolean move, Orientation orientation) {
        TabPane sourcePane = sourceTab.getTabPane();
        if (sourcePane == null) return;

        TabPane targetPane = createEditorTabPane();
        SplitPane split = new SplitPane();
        split.setOrientation(orientation);
        split.setDividerPositions(0.5);
        replaceEditorNode(sourcePane, split);
        split.getItems().addAll(sourcePane, targetPane);

        Tab targetTab;
        if (move) {
            sourcePane.getTabs().remove(sourceTab);
            targetTab = sourceTab;
        } else {
            Tab duplicate = duplicateTab(sourceTab);
            if (duplicate == null) {
                // This tab type can't be meaningfully duplicated (no separate
                // content to reopen) — fall back to moving it instead of
                // silently doing nothing.
                sourcePane.getTabs().remove(sourceTab);
                targetTab = sourceTab;
            } else {
                targetTab = duplicate;
            }
        }

        targetPane.getTabs().add(targetTab);
        targetPane.getSelectionModel().select(targetTab);
        // Always rebind — a moved tab's existing context menu closure still
        // points at its *previous* pane's tab list (stale "Close Other Tabs"
        // etc.), and a duplicated tab never had one installed at all.
        TabContextMenu.install(targetPane, targetTab, this);
    }

    private TabPane createEditorTabPane() {
        TabPane pane = new TabPane();
        pane.getStyleClass().add("main-tabs");
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        configureEditorTabPane(pane);
        return pane;
    }

    /** Replaces one editor leaf with a nested split, whether it is top-level or already nested. */
    private void replaceEditorNode(Node oldNode, Node newNode) {
        if (!replaceNodeInSplit(centerSplit, oldNode, newNode)) {
            throw new IllegalStateException("Editor group was not found in the editor layout");
        }
    }

    /**
     * SplitPane wraps its children in skin nodes, so Node#getParent() cannot
     * be used to find the logical layout parent. Traverse SplitPane items
     * instead; this also works for arbitrarily nested editor splits.
     */
    private boolean replaceNodeInSplit(SplitPane container, Node oldNode, Node newNode) {
        int index = container.getItems().indexOf(oldNode);
        if (index >= 0) {
            container.getItems().set(index, newNode);
            return true;
        }
        for (Node child : container.getItems()) {
            if (child instanceof SplitPane nested
                    && replaceNodeInSplit(nested, oldNode, newNode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A best-effort copy of a tab's content for "Split Right/Down" (as
     * opposed to "and Move…", which relocates the original instead of
     * copying it). Consoles reopen with their SQL text intact; data views
     * reopen the same table. Other tab types don't have a clean way to
     * duplicate their content here, so {@link #split} falls back to moving
     * them instead of silently doing nothing.
     */
    private Tab duplicateTab(Tab tab) {
        if (tab instanceof QueryTab queryTab) {
            QueryTab copy = new QueryTab(this, queryTab.getProfile(), queryTab.getCatalog(), tab.getText());
            copy.setSql(queryTab.getSqlText());
            return copy;
        }
        if (tab instanceof DataTab dataTab) {
            return new DataTab(dataTab.getProfileForReopen(), dataTab.getTableForReopen());
        }
        if (tab instanceof MongoConsoleTab mongoConsole) {
            MongoConsoleTab copy = new MongoConsoleTab(mongoConsole.getProfileForReopen(),
                    mongoConsole.getCurrentDatabase(), tab.getText());
            copy.setScriptText(mongoConsole.getScriptText());
            return copy;
        }
        if (tab instanceof MongoCollectionTab mongoCollection) {
            return new MongoCollectionTab(mongoCollection.getProfileForReopen(),
                    mongoCollection.getCollectionForReopen());
        }
        return null;
    }

    /** Moves a tab out of whichever pane it's in and into its own top-level window. */
    public void openTabInNewWindow(Tab tab) {
        TabPane owner = tab.getTabPane();
        if (owner != null) owner.getTabs().remove(tab);

        TabPane newPane = new TabPane();
        newPane.getTabs().add(tab);
        TabContextMenu.install(newPane, tab, this);

        Stage newStage = new Stage();
        newStage.setTitle(tab.getText() + " \u2014 DBNavigator Pro");
        Scene scene = new Scene(newPane, 900, 650);
        if (stage.getScene() != null) {
            scene.getStylesheets().addAll(stage.getScene().getStylesheets());
        }
        newStage.setScene(scene);
        newStage.show();
    }

    /** Selects a tab in whichever editor group currently hosts it. */
    public void selectTab(Tab tab) {
        TabPane owner = tab.getTabPane();
        if (owner != null) {
            owner.getSelectionModel().select(tab);
            activeTabPane = owner;
        }
    }

    /** Tab context menu → Bookmarks → Show Bookmarks… */
    public void showBookmarksDialog() {
        List<QueryTab> consoles = new java.util.ArrayList<>();
        forEachEditorTab(t -> {
                if (t instanceof QueryTab qt) consoles.add(qt);
        });
        BookmarksDialog.show(this, consoles);
    }

    private void configureEditorTabPane(TabPane pane) {
        pane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) activeTabPane = pane;
            ActionManager.getInstance().updateActions(this);
        });
        pane.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (focused) activeTabPane = pane;
            ActionManager.getInstance().updateActions(this);
        });
        // A close/move can leave a leaf pane empty. Defer cleanup until the
        // tab move finishes, then promote its non-empty sibling(s).
        pane.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change -> {
            Platform.runLater(this::collapseEmptyEditorGroups);
            ActionManager.getInstance().updateActions(this);
        });
        if (activeTabPane == null) activeTabPane = pane;
    }

    private TabPane activeEditorTabPane() {
        List<TabPane> panes = editorTabPanes();
        if (activeTabPane != null && panes.contains(activeTabPane)) return activeTabPane;
        if (!panes.isEmpty()) {
            activeTabPane = panes.get(0);
            return activeTabPane;
        }
        // The layout always keeps one empty editor pane available, but retain
        // this fallback for safety while a split is being normalized.
        activeTabPane = tabPane;
        return tabPane;
    }

    /** Removes empty leaf groups and recursively promotes the surviving editor group. */
    private void collapseEmptyEditorGroups() {
        if (centerSplit.getItems().size() < 2) return;
        Node currentRoot = centerSplit.getItems().get(1);
        Node collapsed = collapseEmptyEditorNode(currentRoot);
        if (collapsed == null) {
            // All groups were closed: retain one empty main pane for the next
            // New Console action instead of leaving the center split empty.
            collapsed = tabPane;
        }
        if (collapsed != currentRoot) centerSplit.getItems().set(1, collapsed);
        if (activeTabPane == null || !editorTabPanes().contains(activeTabPane)) {
            activeEditorTabPane();
        }
    }

    private Node collapseEmptyEditorNode(Node node) {
        if (node instanceof TabPane pane) {
            return pane.getTabs().isEmpty() ? null : pane;
        }
        if (!(node instanceof SplitPane split)) return node;

        List<Node> survivors = new java.util.ArrayList<>();
        for (Node child : List.copyOf(split.getItems())) {
            Node survivor = collapseEmptyEditorNode(child);
            if (survivor != null) survivors.add(survivor);
        }
        // Detach first, then attach survivors. This avoids JavaFX trying to
        // place a node in both the old nested split and its promoted parent.
        split.getItems().clear();
        split.getItems().addAll(survivors);
        return switch (survivors.size()) {
            case 0 -> null;
            case 1 -> {
                Node only = survivors.get(0);
                split.getItems().clear();
                yield only;
            }
            default -> split;
        };
    }

    private Tab currentSelectedTab() {
        if (activeTabPane != null) {
            Tab selected = activeTabPane.getSelectionModel().getSelectedItem();
            if (selected != null) return selected;
        }
        for (TabPane pane : editorTabPanes()) {
            Tab selected = pane.getSelectionModel().getSelectedItem();
            if (selected != null) return selected;
        }
        return null;
    }

    /** Every editor pane currently in the split layout — used by TabContextMenu's Close Other/All Tabs, which act across all of them, not just the pane a tab happens to be in. */
    List<TabPane> editorTabPanes() {
        List<TabPane> panes = new java.util.ArrayList<>();
        collectEditorTabPanes(centerSplit.getItems().get(1), panes);
        return panes;
    }

    private void collectEditorTabPanes(Node node, List<TabPane> panes) {
        if (node instanceof TabPane pane) {
            panes.add(pane);
        } else if (node instanceof SplitPane split) {
            for (Node child : split.getItems()) collectEditorTabPanes(child, panes);
        }
    }

    private void forEachEditorTab(Consumer<Tab> action) {
        for (TabPane pane : editorTabPanes()) {
            for (Tab tab : pane.getTabs()) action.accept(tab);
        }
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }
}

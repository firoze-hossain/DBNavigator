package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.ConnectionStore;
import com.roze.dbnavigator.db.LocalHistoryStore;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;

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

        root.getStyleClass().add("app-root");
        root.setTop(new VBox(buildMenuBar(), buildToolbar()));
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
    }

    public Parent getRoot() { return root; }

    /** A Window suitable for initOwner() on dialogs raised from anywhere in the app. */
    public Window getOwnerWindow() { return stage; }

    public RunPanel getRunPanel() { return runPanel; }

    /** Lets dialogs outside SchemaTreePane (e.g. after modifying a database) trigger a reload. */
    public void refreshSchemaExplorer() {
        schemaPane.reload();
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

    private MenuBar buildMenuBar() {
        // ---- File ----
        MenuItem newDataSource = new MenuItem("New Data Source…");
        newDataSource.setGraphic(Icons.of(FontAwesomeSolid.PLUS_CIRCLE, "#57965c", 11));
        newDataSource.setOnAction(e -> showNewConnectionDialog());

        MenuItem newConsole = new MenuItem("New Query Console");
        newConsole.setAccelerator(new KeyCodeCombination(KeyCode.N,
                KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        newConsole.setOnAction(e -> openConsoleForSelectedConnection());

        MenuItem openSql = new MenuItem("Open SQL File in Console…");
        openSql.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openSql.setOnAction(e -> openSqlFile());

        MenuItem saveSql = new MenuItem("Save Console As…");
        saveSql.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveSql.setOnAction(e -> saveConsoleAs());

        // ---- Local History submenu ----
        // Only "Show History…" is wired reliably right now (it always gives
        // visible feedback — an Alert — instead of silently doing nothing).
        // The other four items are left as-is per request; they'll get the
        // same treatment in a follow-up.
        MenuItem showHistory = new MenuItem("Show History…");
        showHistory.setOnAction(e -> showLocalHistoryForCurrentConsole());
        MenuItem showHistoryForSelection = new MenuItem("Show History for Selection…");
        showHistoryForSelection.setOnAction(e -> withCurrentConsole(tab -> {
            setStatus("Selection-scoped history isn't tracked separately yet — showing full file history");
            tab.showLocalHistory(stage);
        }));
        MenuItem showProjectHistory = new MenuItem("Show Project History…");
        showProjectHistory.setOnAction(e -> ProjectHistoryDialog.showProjectWide(stage, this::openHistoryEntry));
        MenuItem recentChanges = new MenuItem("Recent Changes");
        recentChanges.setOnAction(e -> ProjectHistoryDialog.showRecentChanges(stage, this::openHistoryEntry));
        MenuItem putLabel = new MenuItem("Put Label…");
        putLabel.setOnAction(e -> withCurrentConsole(tab -> {
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
        }));
        Menu localHistoryMenu = new Menu("Local History", null, showHistory, showHistoryForSelection,
                showProjectHistory, recentChanges, new SeparatorMenuItem(), putLabel);

        MenuItem invalidateCaches = new MenuItem("Invalidate Caches…");
        invalidateCaches.setOnAction(e -> InvalidateCachesDialog.show(stage));

        MenuItem settingsMenuItem = new MenuItem("Settings…");
        settingsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN));
        settingsMenuItem.setOnAction(e -> SettingsDialog.show(this));

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> stage.close());

        Menu fileMenu = new Menu("File", null, newDataSource, newConsole,
                new SeparatorMenuItem(), openSql, saveSql, new SeparatorMenuItem(), localHistoryMenu,
                new SeparatorMenuItem(), invalidateCaches, settingsMenuItem, new SeparatorMenuItem(), exit);

        // ---- View ----
        MenuItem refreshExplorer = new MenuItem("Refresh Database Explorer");
        refreshExplorer.setOnAction(e -> schemaPane.reload());
        MenuItem toggleRunPanel = new MenuItem("Run Tool Window");
        toggleRunPanel.setOnAction(e -> {
            if (runPanelVisible) hideRunPanel(); else showRunPanel();
        });
        Menu viewMenu = new Menu("View", null, refreshExplorer, toggleRunPanel);

        // ---- Navigate ----
        MenuItem searchEverywhere = new MenuItem("Search Everywhere…");
        searchEverywhere.setAccelerator(new KeyCodeCombination(KeyCode.F,
                KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        searchEverywhere.setOnAction(e -> showSearchEverywhere());
        Menu navigateMenu = new Menu("Navigate", null, searchEverywhere);

        // ---- Help ----
        MenuItem about = new MenuItem("About DBNavigator Pro");
        about.setOnAction(e -> {
            Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                    "DBNavigator Pro 3.2\nA DataGrip-style database IDE built with JavaFX.\n"
                    + "MySQL · MariaDB · PostgreSQL · SQL Server · Oracle · SQLite · MongoDB"));
            alert.setHeaderText("DBNavigator Pro");
            alert.initOwner(stage);
            alert.showAndWait();
        });
        Menu helpMenu = new Menu("Help", null, about);

        MenuBar menuBar = new MenuBar(fileMenu, viewMenu, navigateMenu, helpMenu);
        menuBar.getStyleClass().add("app-menu-bar");
        return menuBar;
    }

    public void showSearchEverywhere() {
        new SearchDialog(stage, this).show();
    }

    private void openSqlFile() {
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

    private void saveConsoleAs() {
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
    private void showLocalHistoryForCurrentConsole() {
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

    private HBox buildToolbar() {
        Button newConnection = new Button("New Data Source");
        newConnection.setGraphic(Icons.of(FontAwesomeSolid.PLUS_CIRCLE, "#57965c", 12));
        newConnection.setOnAction(e -> showNewConnectionDialog());

        Button newConsole = new Button("New Console");
        newConsole.setGraphic(Icons.of(FontAwesomeSolid.TERMINAL, "#6897bb", 12));
        newConsole.setOnAction(e -> openConsoleForSelectedConnection());

        Button runToggle = new Button("Run");
        runToggle.setGraphic(Icons.of(FontAwesomeSolid.TERMINAL, "#57965c", 12));
        runToggle.setTooltip(new Tooltip("Show/hide the Run panel"));
        runToggle.setOnAction(e -> { if (runPanelVisible) hideRunPanel(); else showRunPanel(); });

        Label brand = new Label("DBNavigator Pro");
        brand.getStyleClass().add("brand-label");
        brand.setGraphic(Icons.of(FontAwesomeSolid.DATABASE, "#4a88c7", 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button searchButton = new Button();
        searchButton.setGraphic(Icons.of(FontAwesomeSolid.SEARCH, "#a9b7c6", 13));
        searchButton.setTooltip(new Tooltip("Search Everywhere (Ctrl+Shift+F)"));
        searchButton.setOnAction(e -> showSearchEverywhere());

        Button settingsButton = new Button();
        settingsButton.setGraphic(Icons.of(FontAwesomeSolid.COG, "#a9b7c6", 13));
        settingsButton.setTooltip(new Tooltip("Settings"));
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
        settingsButton.setOnAction(e ->
                settingsMenu.show(settingsButton, javafx.geometry.Side.BOTTOM, 0, 4));

        HBox toolbar = new HBox(10, brand, new Separator(Orientation.VERTICAL),
                newConnection, newConsole, runToggle, spacer, searchButton, settingsButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.getStyleClass().add("app-toolbar");
        return toolbar;
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
        new ConnectionDialog(null).showAndWait().ifPresent(this::connectAndSave);
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

    private void openConsoleForSelectedConnection() {
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
        java.util.List<QueryTab> consoles = new java.util.ArrayList<>();
        forEachEditorTab(t -> {
                if (t instanceof QueryTab qt) consoles.add(qt);
        });
        BookmarksDialog.show(this, consoles);
    }

    private void configureEditorTabPane(TabPane pane) {
        pane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) activeTabPane = pane;
        });
        pane.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (focused) activeTabPane = pane;
        });
        // A close/move can leave a leaf pane empty. Defer cleanup until the
        // tab move finishes, then promote its non-empty sibling(s).
        pane.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change ->
                Platform.runLater(this::collapseEmptyEditorGroups));
        if (activeTabPane == null) activeTabPane = pane;
    }

    private TabPane activeEditorTabPane() {
        java.util.List<TabPane> panes = editorTabPanes();
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

        java.util.List<Node> survivors = new java.util.ArrayList<>();
        for (Node child : java.util.List.copyOf(split.getItems())) {
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
    java.util.List<TabPane> editorTabPanes() {
        java.util.List<TabPane> panes = new java.util.ArrayList<>();
        collectEditorTabPanes(centerSplit.getItems().get(1), panes);
        return panes;
    }

    private void collectEditorTabPanes(Node node, java.util.List<TabPane> panes) {
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

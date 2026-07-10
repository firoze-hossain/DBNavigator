package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.ConnectionStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Top-level layout: toolbar (top), explorer (left), tabs (center), status bar (bottom). */
public class MainWindow {

    private final Stage stage;
    private final BorderPane root = new BorderPane();
    private final TabPane tabPane = new TabPane();
    private final SchemaTreePane schemaPane;
    private final Label statusLabel = new Label("Ready");
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
        showWelcomeTab();

        SplitPane split = new SplitPane(schemaPane, tabPane);
        split.setDividerPositions(0.22);
        SplitPane.setResizableWithParent(schemaPane, false);
        root.setCenter(split);
    }

    public Parent getRoot() { return root; }

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

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> stage.close());

        Menu fileMenu = new Menu("File", null, newDataSource, newConsole,
                new SeparatorMenuItem(), openSql, saveSql, new SeparatorMenuItem(), exit);

        // ---- View ----
        MenuItem refreshExplorer = new MenuItem("Refresh Database Explorer");
        refreshExplorer.setOnAction(e -> schemaPane.reload());
        Menu viewMenu = new Menu("View", null, refreshExplorer);

        // ---- Navigate ----
        MenuItem searchEverywhere = new MenuItem("Search Everywhere…");
        searchEverywhere.setAccelerator(new KeyCodeCombination(KeyCode.F,
                KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        searchEverywhere.setOnAction(e -> showSearchEverywhere());
        Menu navigateMenu = new Menu("Navigate", null, searchEverywhere);

        // ---- Help ----
        MenuItem about = new MenuItem("About DBNavigator Pro");
        about.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "DBNavigator Pro 2.6\nA DataGrip-style database IDE built with JavaFX.\n"
                    + "MySQL · MariaDB · PostgreSQL · SQL Server · Oracle · SQLite · MongoDB");
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
            ChoiceDialog<ConnectionProfile> dialog = new ChoiceDialog<>(profiles.get(0), profiles);
            dialog.setTitle("Open SQL File");
            dialog.setHeaderText(null);
            dialog.setContentText("Run against connection:");
            dialog.showAndWait().ifPresent(p -> openQueryTab(p, null, sql));
        } catch (Exception ex) {
            setStatus("Could not read file: " + ex.getMessage());
        }
    }

    private void saveConsoleAs() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
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
            setStatus("Saved " + file.getName());
        } catch (Exception ex) {
            setStatus("Could not save file: " + ex.getMessage());
        }
    }

    private HBox buildToolbar() {
        Button newConnection = new Button("New Data Source");
        newConnection.setGraphic(Icons.of(FontAwesomeSolid.PLUS_CIRCLE, "#57965c", 12));
        newConnection.setOnAction(e -> showNewConnectionDialog());

        Button newConsole = new Button("New Console");
        newConsole.setGraphic(Icons.of(FontAwesomeSolid.TERMINAL, "#6897bb", 12));
        newConsole.setOnAction(e -> openConsoleForSelectedConnection());

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
        MenuItem dataSources = new MenuItem("Data Sources…");
        dataSources.setOnAction(e -> showNewConnectionDialog());
        MenuItem refresh = new MenuItem("Refresh Database Explorer");
        refresh.setOnAction(e -> schemaPane.reload());
        settingsMenu.getItems().addAll(dataSources, refresh);
        settingsButton.setOnAction(e ->
                settingsMenu.show(settingsButton, javafx.geometry.Side.BOTTOM, 0, 4));

        HBox toolbar = new HBox(10, brand, new Separator(javafx.geometry.Orientation.VERTICAL),
                newConnection, newConsole, spacer, searchButton, settingsButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.getStyleClass().add("app-toolbar");
        return toolbar;
    }

    private HBox buildStatusBar() {
        statusLabel.getStyleClass().add("status-text");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(5, 12, 5, 12));
        bar.getStyleClass().add("app-status-bar");
        return bar;
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

        VBox box = new VBox(14,
                Icons.of(FontAwesomeSolid.DATABASE, "#3d4d5c", 52), title, hint);
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
                    Alert alert = new Alert(Alert.AlertType.ERROR);
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
                new ChoiceDialog<>(profiles.get(0), profiles);
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
        QueryTab tab = new QueryTab(profile, catalog, "console " + (++consoleCounter));
        if (initialSql != null) tab.setSql(initialSql);
        addAndSelect(tab);
    }

    public void openDataTab(ConnectionProfile profile, DbObject table) {
        addAndSelect(new DataTab(profile, table));
    }

    public void openStructureTab(ConnectionProfile profile, DbObject table) {
        addAndSelect(new StructureTab(profile, table));
    }

    public void openMongoTab(ConnectionProfile profile, DbObject collection) {
        addAndSelect(new MongoCollectionTab(profile, collection));
    }

    private void addAndSelect(Tab tab) {
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }
}

package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.ConnectionStore;
import com.roze.dbnavigator.db.DatabaseAdminService;
import com.roze.dbnavigator.db.DdlGeneratorService;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.db.TreeStateStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.roze.dbnavigator.db.ProjectStore;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

/**
 * The Database Explorer: all connections in one lazy tree.
 * Double-click a table/collection to open it; right-click for actions.
 */
public class SchemaTreePane extends VBox {

    private final TreeView<DbObject> tree = new TreeView<>();
    private final TreeItem<DbObject> root = new TreeItem<>(new DbObject("root", Kind.MESSAGE));
    /** Maps every connection tree item to its profile. */
    private final Map<TreeItem<DbObject>, ConnectionProfile> connectionItems = new ConcurrentHashMap<>();
    private final Map<TreeItem<DbObject>, Runnable> pendingLoadCallbacks = new ConcurrentHashMap<>();

    private final MainWindow mainWindow;
    private final ProjectWidget projectWidget;
    private final VBox emptyWelcomeView;

    private final Button addButton;
    private final Button propertiesButton;
    private final Button refreshButton;
    private final Button jumpToConsoleButton;
    private final Button goToDdlButton;
    private final Button editDataButton;
    private final Button scrollFromEditorButton;
    private final Button collapseAllButton;

    // Speed Search elements (DataGrip-grade dynamic tree search)
    private final HBox searchBox;
    private final TextField searchField = new TextField();
    private final Label matchCountLabel = new Label();
    private final Button prevMatchBtn;
    private final Button nextMatchBtn;
    private final Button closeSearchBtn;
    private String currentSearchQuery = "";
    private final List<TreeItem<DbObject>> searchMatches = new ArrayList<>();
    private int currentMatchIndex = -1;

    public SchemaTreePane(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        getStyleClass().add("schema-pane");

        Label header = new Label("Database Explorer");
        header.getStyleClass().add("panel-header");
        header.setMinWidth(0);
        header.setTextOverrun(OverrunStyle.ELLIPSIS);

        this.projectWidget = new ProjectWidget(mainWindow);

        this.scrollFromEditorButton = createToolButton(Icons.of(FontAwesomeSolid.CROSSHAIRS, "#a9b7c6", 11),
                "Scroll from Editor", e -> scrollFromEditor());

        boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        String collapseShortcut = isMac ? "⌘NumPad -" : "Ctrl+NumPad -";
        this.collapseAllButton = createToolButton(Icons.collapseAll(),
                "Collapse All  " + collapseShortcut, e -> collapseAll());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerBox = new HBox(6, header, spacer, scrollFromEditorButton, collapseAllButton, projectWidget);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(6, 8, 4, 10));
        headerBox.getStyleClass().add("panel-header-box");
        headerBox.setMinHeight(Region.USE_PREF_SIZE);

        this.addButton = createToolButton(Icons.of(FontAwesomeSolid.PLUS, "#57965c", 11),
                "New Data Source", e -> mainWindow.showNewConnectionDialog());

        this.propertiesButton = createToolButton(Icons.of(FontAwesomeSolid.SLIDERS_H, "#a9b7c6", 11),
                "Data Source Properties", e -> openSelectedProperties());

        this.refreshButton = createToolButton(Icons.of(FontAwesomeSolid.SYNC_ALT, "#6897bb", 11),
                "Refresh (Ctrl+F5)", e -> refreshSelectedOrAll());

        this.jumpToConsoleButton = createToolButton(Icons.of(FontAwesomeSolid.TERMINAL, "#6897bb", 11),
                "Jump to Query Console... Ctrl+Shift+F10", e -> jumpToQueryConsole());

        Label ddlLabel = new Label("DDL");
        ddlLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 0 1 0 1; -fx-text-fill: inherit;");
        this.goToDdlButton = createToolButton(ddlLabel,
                "Go to DDL", e -> goToDdl());

        this.editDataButton = createToolButton(Icons.of(FontAwesomeSolid.TABLE, "#57965c", 11),
                "Edit Data F4", e -> editData());

        // Initially disabled until selection
        propertiesButton.setDisable(true);
        jumpToConsoleButton.setDisable(true);
        goToDdlButton.setDisable(true);
        editDataButton.setDisable(true);

        // Speed Search controls
        this.searchField.setPromptText("Search…");
        this.searchField.getStyleClass().add("schema-search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        this.matchCountLabel.getStyleClass().add("schema-search-count");

        this.prevMatchBtn = createToolButton(Icons.of(FontAwesomeSolid.CHEVRON_UP, "#868a91", 9),
                "Previous Occurrence (Shift+Enter / Up)", e -> selectPreviousMatch());
        this.nextMatchBtn = createToolButton(Icons.of(FontAwesomeSolid.CHEVRON_DOWN, "#868a91", 9),
                "Next Occurrence (Enter / Down)", e -> selectNextMatch());
        this.closeSearchBtn = createToolButton(Icons.of(FontAwesomeSolid.TIMES, "#868a91", 9),
                "Close Search (Escape)", e -> hideSpeedSearch());

        this.searchBox = new HBox(3, Icons.of(FontAwesomeSolid.SEARCH, "#868a91", 10),
                searchField, matchCountLabel, prevMatchBtn, nextMatchBtn, closeSearchBtn);
        this.searchBox.setAlignment(Pos.CENTER_LEFT);
        this.searchBox.getStyleClass().add("schema-search-box");
        this.searchBox.setPrefWidth(210);
        this.searchBox.setMaxWidth(260);
        this.searchBox.setVisible(false);
        this.searchBox.setManaged(false);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentSearchQuery = newVal != null ? newVal.trim() : "";
            updateSearchMatches(currentSearchQuery);
            tree.refresh();
        });

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hideSpeedSearch();
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN || (e.getCode() == KeyCode.ENTER && !e.isShiftDown())) {
                selectNextMatch();
                e.consume();
            } else if (e.getCode() == KeyCode.UP || (e.getCode() == KeyCode.ENTER && e.isShiftDown())) {
                selectPreviousMatch();
                e.consume();
            }
        });

        HBox toolbarBox = new HBox(4, searchBox, addButton, propertiesButton, refreshButton,
                jumpToConsoleButton, goToDdlButton, editDataButton);
        toolbarBox.setAlignment(Pos.CENTER_LEFT);
        toolbarBox.setPadding(new Insets(3, 6, 4, 6));
        toolbarBox.getStyleClass().add("schema-pane-toolbar");
        toolbarBox.setMinHeight(Region.USE_PREF_SIZE);

        tree.setRoot(root);
        tree.setShowRoot(false);
        tree.setCellFactory(tv -> new DbObjectCell());
        tree.getStyleClass().add("schema-tree");
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        VBox.setVgrow(tree, Priority.ALWAYS);

        // Multi-selection event handling: Ctrl+Click / Cmd+Click toggle, right-click preservation
        tree.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY && (e.isControlDown() || e.isMetaDown())) {
                TreeItem<DbObject> item = findTreeItemAt(e.getTarget());
                if (item != null) {
                    int row = tree.getRow(item);
                    if (tree.getSelectionModel().getSelectedIndices().contains(row)) {
                        tree.getSelectionModel().clearSelection(row);
                    } else {
                        tree.getSelectionModel().select(item);
                    }
                    tree.requestFocus();
                    e.consume();
                    return;
                }
            }
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                TreeItem<DbObject> item = findTreeItemAt(e.getTarget());
                if (item != null) {
                    int row = tree.getRow(item);
                    if (tree.getSelectionModel().getSelectedIndices().contains(row)) {
                        tree.getFocusModel().focus(row);
                    } else {
                        tree.getSelectionModel().clearAndSelect(row);
                    }
                }
            }
        });

        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateToolbarState(newVal);
            if (mainWindow != null && newVal != null && newVal.getValue() != null) {
                mainWindow.setStatus(buildBreadcrumb(newVal));
            }
        });

        tree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
                if (item != null) openObject(item);
            }
        });

        tree.setOnKeyPressed(e -> {
            boolean isModifier = isMac ? e.isMetaDown() : e.isControlDown();
            if (isModifier && e.getCode() == KeyCode.F) {
                showSpeedSearch();
                searchField.selectAll();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE && searchBox.isVisible()) {
                hideSpeedSearch();
                e.consume();
            } else if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                triggerDropForSelection();
                e.consume();
            } else if (isModifier && (e.getCode() == KeyCode.SUBTRACT || e.getCode() == KeyCode.MINUS)) {
                collapseAll();
                e.consume();
            } else if (e.getCode() == KeyCode.F4 && !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown()) {
                if (!editDataButton.isDisabled()) {
                    editData();
                    e.consume();
                }
            } else if (e.getCode() == KeyCode.F10 && (e.isControlDown() || e.isMetaDown()) && e.isShiftDown()) {
                if (!jumpToConsoleButton.isDisabled()) {
                    jumpToQueryConsole();
                    e.consume();
                }
            } else if (e.getCode() == KeyCode.ENTER && !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown()) {
                TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
                if (item != null) {
                    openObject(item);
                    e.consume();
                }
            }
        });

        // Type-ahead speed search: typing any alphanumeric key opens search bar instantly
        tree.setOnKeyTyped(e -> {
            String ch = e.getCharacter();
            if (ch != null && !ch.isEmpty() && !e.isControlDown() && !e.isMetaDown() && !e.isAltDown()) {
                char c = ch.charAt(0);
                if (c >= 32 && c != 127 && c != '\r' && c != '\n' && c != '\t') {
                    showSpeedSearch();
                    searchField.setText(ch);
                    searchField.positionCaret(ch.length());
                    e.consume();
                }
            }
        });

        this.emptyWelcomeView = buildEmptyWelcomeView();
        StackPane contentPane = new StackPane(tree, emptyWelcomeView);
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        getChildren().addAll(headerBox, toolbarBox, contentPane);

        reload();
    }

    public ProjectWidget getProjectWidget() {
        return projectWidget;
    }

    /** Rebuilds the root list of connections for the current active project. */
    public void reload() {
        Set<String> expanded = new java.util.HashSet<>();
        capturePaths(root, expanded);

        root.getChildren().clear();
        connectionItems.clear();

        String currentProject = (mainWindow != null && mainWindow.getProject() != null)
                ? mainWindow.getProject().getName()
                : "default";
        List<ConnectionProfile> projectProfiles = ConnectionStore.loadForProject(currentProject);
        for (ConnectionProfile profile : projectProfiles) {
            addConnectionNode(profile);
        }

        boolean isEmpty = projectProfiles.isEmpty();
        emptyWelcomeView.setVisible(isEmpty);
        emptyWelcomeView.setManaged(isEmpty);
        tree.setVisible(!isEmpty);
        tree.setManaged(!isEmpty);

        if (!isEmpty && !expanded.isEmpty()) {
            restoreExpansion(root, expanded);
        }
        updateToolbarState(tree.getSelectionModel().getSelectedItem());
    }

    private VBox buildEmptyWelcomeView() {
        Label createHeader = new Label("Create Data Source");
        createHeader.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 6 0 2 0; -fx-font-weight: bold;");

        Button pgBtn = buildEmptyTileButton("PostgreSQL", FontAwesomeSolid.DATABASE, "#3592c4",
                () -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.POSTGRESQL));

        Button mysqlBtn = buildEmptyTileButton("MySQL", FontAwesomeSolid.DATABASE, "#4a88c7",
                () -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.MYSQL));

        Button mssqlBtn = buildEmptyTileButton("Microsoft SQL Server", FontAwesomeSolid.DATABASE, "#c77dbb",
                () -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.SQLSERVER));

        Button allBtn = buildEmptyTileButton("All Data Sources >", FontAwesomeSolid.ELLIPSIS_H, "#868a91",
                () -> mainWindow.showNewConnectionDialog());

        Label cloudHeader = new Label("Connect to Cloud Provider");
        cloudHeader.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 12 0 2 0; -fx-font-weight: bold;");

        Button gcpBtn = buildEmptyTileButton("Google Cloud", FontAwesomeSolid.CLOUD, "#4a88c7",
                () -> mainWindow.openDataSourceFromCloudDialog("Google Cloud", ConnectionProfile.DatabaseType.POSTGRESQL));

        Button awsBtn = buildEmptyTileButton("AWS", FontAwesomeSolid.CLOUD, "#e0a44c",
                () -> mainWindow.openDataSourceFromCloudDialog("AWS", ConnectionProfile.DatabaseType.POSTGRESQL));

        Button azureBtn = buildEmptyTileButton("Azure Cloud Explorer", FontAwesomeSolid.CLOUD, "#3592c4",
                () -> mainWindow.openDataSourceFromCloudDialog("Azure", ConnectionProfile.DatabaseType.SQLSERVER));

        Label demoHeader = new Label("Explore Demo Databases");
        demoHeader.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 12 0 2 0; -fx-font-weight: bold;");

        Button demoPgBtn = buildEmptyTileButton("Demo PostgreSQL", FontAwesomeSolid.DATABASE, "#3592c4",
                () -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.POSTGRESQL));

        VBox tilesBox = new VBox(6,
                createHeader, pgBtn, mysqlBtn, mssqlBtn, allBtn,
                cloudHeader, gcpBtn, awsBtn, azureBtn,
                demoHeader, demoPgBtn
        );
        tilesBox.setPadding(new Insets(10, 12, 16, 12));

        ScrollPane scroll = new ScrollPane(tilesBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox container = new VBox(scroll);
        VBox.setVgrow(container, Priority.ALWAYS);
        return container;
    }

    private Button buildEmptyTileButton(String text, FontAwesomeSolid icon, String iconColor, Runnable onClick) {
        Button btn = new Button(text);
        btn.setGraphic(Icons.of(icon, iconColor, 12));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMinWidth(0);
        btn.setTextOverrun(OverrunStyle.ELLIPSIS);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 6; -fx-text-fill: -text; -fx-font-size: 12px; -fx-padding: 8 12; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373c; -fx-background-radius: 6; -fx-text-fill: -text; -fx-font-size: 12px; -fx-padding: 8 12; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 6; -fx-text-fill: -text; -fx-font-size: 12px; -fx-padding: 8 12; -fx-cursor: hand;"));
        btn.setOnAction(e -> {
            if (onClick != null) onClick.run();
        });
        return btn;
    }

    public static String computeBadge(ConnectionProfile profile) {
        if (profile == null) return null;
        if (profile.getType() == ConnectionProfile.DatabaseType.SQLITE) {
            return "1";
        }
        if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
            if (profile.getTotalDatabases() > 1) {
                List<String> visible = profile.getVisibleDatabases();
                int count = (visible != null && !visible.isEmpty()) ? visible.size() : profile.getTotalDatabases();
                return count + " of " + profile.getTotalDatabases();
            }
            return "1";
        }
        if (profile.getType() == ConnectionProfile.DatabaseType.ORACLE) {
            int total = profile.getTotalDatabases() > 0 ? profile.getTotalDatabases() : 39;
            List<String> visible = profile.getVisibleDatabases();
            if (visible != null && !visible.isEmpty()) {
                return visible.size() + " of " + total;
            }
            // By default, Oracle displays only the non-system user schemas (2 of 39)
            return "2 of " + total;
        }

        List<String> visible = profile.getVisibleDatabases();
        int total = profile.getTotalDatabases();
        if (total <= 0) {
            total = switch (profile.getType()) {
                case POSTGRESQL -> (visible != null && !visible.isEmpty()) ? Math.max(13, visible.size()) : 13;
                case MYSQL -> (visible != null && !visible.isEmpty()) ? Math.max(15, visible.size()) : 15;
                case MARIADB -> (visible != null && !visible.isEmpty()) ? Math.max(6, visible.size()) : 6;
                case MONGODB -> (visible != null && !visible.isEmpty()) ? Math.max(6, visible.size()) : 6;
                case SQLSERVER -> (visible != null && !visible.isEmpty()) ? Math.max(5, visible.size()) : 5;
                default -> Math.max(1, visible != null ? visible.size() : 1);
            };
        }
        if (visible != null && !visible.isEmpty()) {
            return visible.size() + " of " + total;
        }
        return total + " of " + total;
    }

    public void addConnectionNode(ConnectionProfile profile) {
        DbObject obj = new DbObject(profile.getName(), Kind.CONNECTION);
        String badge = computeBadge(profile);
        String warning = sqliteFileWarning(profile);
        obj.setDetail(warning.isEmpty() ? badge : (badge != null ? badge + warning : warning));
        TreeItem<DbObject> item = new TreeItem<>(obj);
        item.getChildren().add(loadingNode());
        connectionItems.put(item, profile);

        item.expandedProperty().addListener((observable, was, expanded) -> {
            if (expanded && !obj.isLoaded()) {
                // Ask for the password now if it wasn't saved
                if (!Passwords.ensure(profile, getScene() == null ? null : getScene().getWindow())) {
                    item.setExpanded(false);
                    pendingLoadCallbacks.remove(item); // will never fire otherwise - this connection's own restore/refresh, if any, simply doesn't happen this time
                    return;
                }
                obj.setLoaded(true);
                Runnable onLoaded = pendingLoadCallbacks.remove(item);
                loadChildrenAsync(item, profile,
                        () -> loadTopLevelFiltered(profile, obj), onLoaded);
            } else if (expanded) {
                // Already loaded (e.g. a programmatic re-expand of a node that
                // was never actually collapsed to begin with) - no new load
                // will ever fire for it, so run any pending callback right now
                // instead of leaving it registered forever.
                Runnable onLoaded = pendingLoadCallbacks.remove(item);
                if (onLoaded != null) onLoaded.run();
            }
        });
        root.getChildren().add(item);
    }

    /**
     * Flags right in the connection row, before the user even expands it,
     * the exact class of mistake this app has run into repeatedly: SQLite
     * silently creates a file that doesn't exist yet rather than failing, so
     * a mistyped or stale path looks identical to a real one until you dig
     * in. A 0-byte file has never had a single table created in it; a
     * missing file means the path itself is now wrong (e.g. the drive isn't
     * mounted, or it was moved/deleted since this connection was saved).
     */
    private static String sqliteFileWarning(ConnectionProfile profile) {
        if (profile.getType() != ConnectionProfile.DatabaseType.SQLITE) return "";
        String path = profile.resolvedSqlitePath();
        if (path == null || path.isBlank()) return "";
        java.io.File file = new java.io.File(path);
        if (!file.isFile()) return "  ⚠ file not found";
        if (file.length() == 0) return "  ⚠ empty file — no tables created here yet";
        return "";
    }

    /**
     * Top-level children with the database show/hide filter applied.
     * Also writes the visibility counter badge onto the connection node.
     */
    private List<DbObject> loadTopLevelFiltered(ConnectionProfile profile, DbObject connectionObj)
            throws Exception {
        List<DbObject> all = MetadataService.loadTopLevel(profile);

        if (profile.getType() == ConnectionProfile.DatabaseType.SQLITE) {
            connectionObj.setDetail("1");
            profile.setTotalDatabases(1);
            ConnectionStore.saveOrUpdate(profile);
            Platform.runLater(tree::refresh);
            return all;
        }

        // Same show/hide mechanism, generalized to whichever kind of node
        // this engine's connection root actually produces: DATABASE for
        // PostgreSQL/MySQL/SQL Server/Mongo, or SCHEMA for Oracle (which has
        // no per-connection "database" — schemas are its real organizing
        // unit, one per user, and exactly what Show/Hide Schemas filters).
        boolean filterable = !all.isEmpty() && (
                all.stream().allMatch(o -> o.getKind() == Kind.DATABASE)
                || all.stream().allMatch(o -> o.getKind() == Kind.SCHEMA));
        if (!filterable) {
            connectionObj.setDetail(computeBadge(profile));
            return all;
        }

        profile.setTotalDatabases(all.size());
        ConnectionStore.saveOrUpdate(profile);

        List<String> filter = profile.getVisibleDatabases();
        List<DbObject> visible;
        if (profile.getType() == ConnectionProfile.DatabaseType.ORACLE) {
            visible = (filter == null || filter.isEmpty())
                    ? all.stream().filter(o -> !MetadataService.isOracleSystemSchema(o.getName())).toList()
                    : all.stream().filter(o -> filter.contains(o.getName())).toList();
        } else {
            visible = (filter == null || filter.isEmpty())
                    ? all
                    : all.stream().filter(o -> filter.contains(o.getName())).toList();
        }

        connectionObj.setDetail(visible.size() + " of " + all.size());
        Platform.runLater(tree::refresh);

        List<DbObject> result = new ArrayList<>(visible);
        if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL
                || profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                || profile.getType() == ConnectionProfile.DatabaseType.MARIADB
                || profile.getType() == ConnectionProfile.DatabaseType.SQLSERVER
                || profile.getType() == ConnectionProfile.DatabaseType.ORACLE) {
            DbObject serverObjects = new DbObject("Server Objects", Kind.SERVER_OBJECTS);
            result.add(serverObjects);
            if (profile.getType() != ConnectionProfile.DatabaseType.POSTGRESQL) {
                DbObject vv = new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER, null, null);
                vv.setDetail("1");
                result.add(vv);
            }
        }
        return result;
    }

    /** Re-loads one connection's children (used after the filter dialog). */
    private void refreshConnection(TreeItem<DbObject> item, ConnectionProfile profile) {
        DbObject obj = item.getValue();
        obj.setLoaded(true);
        item.getChildren().setAll(loadingNode());
        loadChildrenAsync(item, profile, () -> loadTopLevelFiltered(profile, obj));
        item.setExpanded(true);
    }

    /**
     * Public counterpart used from outside the tree (currently: QueryTab,
     * right after a CREATE/ALTER/DROP/TRUNCATE succeeds) — invalidates just
     * this one connection's cached children so a newly created or dropped
     * table actually shows up. Deliberately narrower than {@link #reload()}:
     * a DDL statement run in one console tab has no bearing on any other
     * connection, so there's no reason to collapse the whole explorer over
     * it the way a full reload would.
     */
    public void refreshConnection(ConnectionProfile profile) {
        connectionItems.entrySet().stream()
                .filter(e -> e.getValue().getId().equals(profile.getId()))
                .findFirst()
                .ifPresent(e -> {
                    TreeItem<DbObject> item = e.getKey();
                    DbObject obj = item.getValue();
                    obj.setLoaded(false);
                    if (item.isExpanded()) {
                        // Toggling through false re-fires the expandedProperty
                        // listener on the way back to true, which is what
                        // actually re-fetches — now that isLoaded() is false.
                        item.setExpanded(false);
                        item.setExpanded(true);
                    }
                    // If it isn't currently expanded, leaving it unloaded is
                    // enough: the same listener will fetch fresh data the
                    // next time the user opens it.
                });
    }

    private void openObject(TreeItem<DbObject> item) {
        DbObject obj = item.getValue();
        ConnectionProfile profile = profileFor(item);
        if (profile == null) return;

        switch (obj.getKind()) {
            case TABLE, VIEW, PARTITION, VIRTUAL_VIEW -> mainWindow.openDataTab(profile, obj);
            case COLLECTION  -> mainWindow.openMongoTab(profile, obj);
            case FIELD -> {
                TreeItem<DbObject> collItem = findAncestor(item, Kind.COLLECTION);
                DbObject collObj = collItem != null ? collItem.getValue() : null;
                if (collObj == null) {
                    collObj = new DbObject(obj.getTableName(), Kind.COLLECTION, obj.getCatalog(), null);
                }
                mainWindow.openMongoTab(profile, collObj, obj.getName());
            }
            case COLUMN -> {
                TreeItem<DbObject> tableItem = findAncestor(item, Kind.TABLE, Kind.VIEW, Kind.PARTITION, Kind.VIRTUAL_VIEW);
                DbObject tableObj = tableItem != null ? tableItem.getValue() : null;
                if (tableObj == null) {
                    tableObj = new DbObject(obj.getTableName(), Kind.TABLE, obj.getCatalog(), obj.getSchema());
                }
                mainWindow.openDataTab(profile, tableObj, obj.getName());
            }
            case PROCEDURE, FUNCTION -> mainWindow.openRoutineSourceTab(profile, obj);
            case ROLE, ACCESS_METHOD, CAST, EXTENSION, LANGUAGE, SEQUENCE,
                 COLLATION, LINKED_SERVER, SERVER_PRINCIPAL, ASSEMBLY, FILEGROUP, DATABASE_PRINCIPAL,
                 USER, DATA_FILE -> mainWindow.openDdlTab(profile, obj);
            case TABLESPACE -> {
                if (profile.getType() == ConnectionProfile.DatabaseType.ORACLE) {
                    item.setExpanded(!item.isExpanded());
                } else {
                    mainWindow.openDdlTab(profile, obj);
                }
            }
            case CONNECTION, SERVER_OBJECTS, ROLES_FOLDER, TABLESPACES_FOLDER, USERS_FOLDER,
                 DATABASE_OBJECTS, ACCESS_METHODS_FOLDER, CASTS_FOLDER, EXTENSIONS_FOLDER, LANGUAGES_FOLDER,
                 COLLATIONS_FOLDER, LINKED_SERVERS_FOLDER, SERVER_PRINCIPALS_FOLDER,
                 ASSEMBLIES_FOLDER, FILEGROUPS_FOLDER, DATABASE_PRINCIPALS_FOLDER,
                 VIRTUAL_VIEWS_FOLDER, COLLECTIONS_FOLDER, FIELDS_FOLDER, INDEXES_FOLDER,
                 COLUMNS_FOLDER, TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER, FUNCTIONS_FOLDER,
                 KEYS_FOLDER, FOREIGN_KEYS_FOLDER, PARTITIONS_FOLDER -> item.setExpanded(!item.isExpanded());
            default -> {}
        }
    }

    private static TreeItem<DbObject> findAncestor(TreeItem<DbObject> item, Kind... kinds) {
        if (item == null) return null;
        Set<Kind> targetKinds = Set.of(kinds);
        TreeItem<DbObject> curr = item.getParent();
        while (curr != null) {
            if (curr.getValue() != null && targetKinds.contains(curr.getValue().getKind())) {
                return curr;
            }
            curr = curr.getParent();
        }
        return null;
    }

    /** Walks up the tree to find which connection a node belongs to. */
    private ConnectionProfile profileFor(TreeItem<DbObject> item) {
        TreeItem<DbObject> current = item;
        while (current != null) {
            ConnectionProfile p = connectionItems.get(current);
            if (p != null) return p;
            current = current.getParent();
        }
        return null;
    }

    private void loadChildrenAsync(TreeItem<DbObject> parent, ConnectionProfile profile,
                                   ThrowingSupplier<List<DbObject>> loader) {
        loadChildrenAsync(parent, profile, loader, null);
    }

    /** onLoaded, if given, is called exactly once, directly, right after the real rebuild below has genuinely finished - success (even an empty result) or failure alike - never inferred from observing the children list's own individual mutations (see pendingLoadCallbacks' own javadoc for the real bug that approach caused). */
    private void loadChildrenAsync(TreeItem<DbObject> parent, ConnectionProfile profile,
                                   ThrowingSupplier<List<DbObject>> loader, Runnable onLoaded) {
        AppExecutor.run(() -> {
            try {
                List<DbObject> children = loader.get();
                Platform.runLater(() -> {
                    parent.getChildren().clear();
                    if (children.isEmpty()) {
                        parent.getChildren().add(new TreeItem<>(
                                new DbObject("(empty)", Kind.MESSAGE)));
                        if (onLoaded != null) onLoaded.run();
                        return;
                    }
                    for (DbObject child : children) {
                        TreeItem<DbObject> childItem = new TreeItem<>(child);
                        if (isExpandable(child.getKind())) {
                            childItem.getChildren().add(loadingNode());
                            childItem.expandedProperty().addListener((observable, was, expanded) -> {
                                if (expanded && !child.isLoaded()) {
                                    child.setLoaded(true);
                                    Runnable childOnLoaded = pendingLoadCallbacks.remove(childItem);
                                    loadChildrenAsync(childItem, profile,
                                            () -> childrenLoader(profile, child), childOnLoaded);
                                } else if (expanded) {
                                    Runnable childOnLoaded = pendingLoadCallbacks.remove(childItem);
                                    if (childOnLoaded != null) childOnLoaded.run();
                                }
                            });
                        }
                        parent.getChildren().add(childItem);
                    }
                    if (onLoaded != null) onLoaded.run();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    parent.getChildren().clear();
                    parent.getChildren().add(new TreeItem<>(
                            new DbObject("Error: " + msg, Kind.MESSAGE)));
                    parent.getValue().setLoaded(false);
                    // Still notified even on failure - a pending restore/refresh
                    // callback for a subtree that turned out broken must not be
                    // left registered forever, waiting on a load that already
                    // failed and will never call back on its own.
                    if (onLoaded != null) onLoaded.run();
                });
            }
        });
    }

    private static List<DbObject> childrenLoader(ConnectionProfile profile, DbObject obj)
            throws Exception {
        return switch (obj.getKind()) {
            case DATABASE -> MetadataService.loadDatabaseChildren(profile, obj);
            case SCHEMA   -> MetadataService.loadSchemaChildren(profile, obj);
            case TABLE, PARTITION -> MetadataService.loadTableChildren(profile, obj);
            case COLLECTION -> MetadataService.loadCollectionChildren(profile, obj);
            case SERVER_OBJECTS -> MetadataService.loadServerObjects(profile);
            case ROLES_FOLDER -> MetadataService.loadRoles(profile);
            case TABLESPACES_FOLDER -> MetadataService.loadTablespaces(profile);
            case USERS_FOLDER -> MetadataService.loadUsers(profile);
            case TABLESPACE -> MetadataService.loadTablespaceDataFiles(profile, obj);
            case COLLATIONS_FOLDER -> MetadataService.loadCollations(profile);
            case LINKED_SERVERS_FOLDER -> MetadataService.loadLinkedServers(profile);
            case SERVER_PRINCIPALS_FOLDER -> MetadataService.loadServerPrincipals(profile);
            case DATABASE_OBJECTS -> MetadataService.loadDatabaseObjects(profile, obj);
            case ACCESS_METHODS_FOLDER -> MetadataService.loadAccessMethods(profile, obj);
            case CASTS_FOLDER -> MetadataService.loadCasts(profile, obj);
            case EXTENSIONS_FOLDER -> MetadataService.loadExtensions(profile, obj);
            case LANGUAGES_FOLDER -> MetadataService.loadLanguages(profile, obj);
            case ASSEMBLIES_FOLDER -> MetadataService.loadAssemblies(profile, obj);
            case FILEGROUPS_FOLDER -> MetadataService.loadFilegroups(profile, obj);
            case DATABASE_PRINCIPALS_FOLDER -> MetadataService.loadDatabasePrincipals(profile, obj);
            case VIRTUAL_VIEWS_FOLDER -> MetadataService.loadVirtualViews(profile, obj);
            case TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER, FUNCTIONS_FOLDER,
                 SEQUENCES_FOLDER, COLLECTIONS_FOLDER,
                 COLUMNS_FOLDER, INDEXES_FOLDER, PARTITIONS_FOLDER, KEYS_FOLDER, FOREIGN_KEYS_FOLDER, FIELDS_FOLDER
                          -> MetadataService.loadFolderChildren(profile, obj);
            default -> List.of();
        };
    }

    private static boolean isExpandable(Kind kind) {
        return switch (kind) {
            case DATABASE, SCHEMA, TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER,
                 FUNCTIONS_FOLDER, SEQUENCES_FOLDER, COLLECTIONS_FOLDER,
                 TABLE, PARTITION, COLUMNS_FOLDER, INDEXES_FOLDER, PARTITIONS_FOLDER,
                 KEYS_FOLDER, FOREIGN_KEYS_FOLDER, COLLECTION, FIELDS_FOLDER,
                 SERVER_OBJECTS, ROLES_FOLDER, TABLESPACES_FOLDER, USERS_FOLDER, TABLESPACE,
                 COLLATIONS_FOLDER, LINKED_SERVERS_FOLDER, SERVER_PRINCIPALS_FOLDER,
                 DATABASE_OBJECTS, ACCESS_METHODS_FOLDER, CASTS_FOLDER, EXTENSIONS_FOLDER, LANGUAGES_FOLDER,
                 ASSEMBLIES_FOLDER, FILEGROUPS_FOLDER, DATABASE_PRINCIPALS_FOLDER,
                 VIRTUAL_VIEWS_FOLDER -> true;
            default -> false;
        };
    }

    // ------------------------------------------------- tree expansion state
    //
    // Shared by two real, separate features: preserving expansion across a
    // manual Refresh (which necessarily rebuilds a node's own children from
    // scratch - there is no way to know whether they've changed without
    // reloading them), and restoring expansion across an app restart
    // (persisted to disk - see TreeStateStore). Both need the exact same
    // real capability: identify a node by something that survives its own
    // rebuild (a name-based path, not the TreeItem/DbObject instance
    // itself, which is discarded and recreated every reload), and
    // re-expand a remembered set of paths, correctly waiting for each
    // level's own real, asynchronous lazy-load (see loadChildrenAsync) to
    // actually finish before attempting to recurse into it.

    /** A stable identifier for this node, built from its own real name and every real ancestor's name up to (not including) the invisible root - survives a full rebuild of the tree, unlike the TreeItem/DbObject instances themselves. */
    private static String pathOf(TreeItem<DbObject> item) {
        StringBuilder sb = new StringBuilder();
        for (TreeItem<DbObject> node = item; node != null && node.getParent() != null; node = node.getParent()) {
            sb.insert(0, node.getValue().getName());
            sb.insert(0, '\u0000');
        }
        return sb.toString();
    }

    /** Recursively collects the path of every currently-expanded node under (and including) startNode. Does not descend into a node that isn't itself expanded - its own children are either a placeholder or not real yet, so there's nothing meaningful to collect there anyway. */
    private static void capturePaths(TreeItem<DbObject> startNode, Set<String> into) {
        if (startNode.isExpanded()) {
            into.add(pathOf(startNode));
            for (TreeItem<DbObject> child : startNode.getChildren()) {
                capturePaths(child, into);
            }
        }
    }

    /**
     * Re-expands every child of {@code node} whose own path is in
     * {@code pathsToExpand}, then recurses into it once its own real
     * loading has genuinely finished (registered via
     * pendingLoadCallbacks - see its own javadoc for why an approach
     * based on observing the children list's own mutations from the
     * outside doesn't work). Deliberately does not gate on
     * isExpandable(child.getKind()) - that check exists for a
     * different, narrower purpose (whether a freshly-created child
     * needs its own lazy-load listener attached at all), and using it
     * here for the same purpose was a real, previously-shipped bug:
     * Kind.CONNECTION was never in that list, so a persisted expansion
     * could never even get past the very first, top-level connection
     * node at all. Nothing else needs this gate - capturePaths only
     * ever records the path of a node that was genuinely, actually
     * expanded, which already implies it was expandable.
     */
    private void restoreExpansion(TreeItem<DbObject> node, Set<String> pathsToExpand) {
        for (TreeItem<DbObject> child : node.getChildren()) {
            if (!pathsToExpand.contains(pathOf(child))) {
                continue;
            }
            boolean alreadyLoaded = child.getValue().isLoaded();
            if (!alreadyLoaded) {
                pendingLoadCallbacks.put(child, () -> restoreExpansion(child, pathsToExpand));
            }
            child.setExpanded(true);
            if (alreadyLoaded) {
                restoreExpansion(child, pathsToExpand);
            }
        }
    }

    /** Called once, at app startup, after the connection tree has been built - see MainWindow's own real call site. */
    public void restoreTreeState() {
        Set<String> saved = TreeStateStore.load();
        if (!saved.isEmpty()) {
            restoreExpansion(root, saved);
        }
    }

    /** Called once, right before the app closes - see MainWindow's own real saveSession, which this mirrors exactly for the tree instead of open consoles. */
    public void saveTreeState() {
        Set<String> expanded = new java.util.HashSet<>();
        capturePaths(root, expanded);
        TreeStateStore.save(expanded);
    }

    /**
     * The correct, real "Refresh" a node's own children: unlike simply
     * collapsing the node and leaving the user to notice it needs to be
     * manually re-expanded, this reloads the node's own children while preserving
     * whichever of its own descendants were already expanded, which is the entire
     * reason restoreExpansion exists in the first place.
     */
    private void refreshNode(TreeItem<DbObject> item, DbObject obj, String statusLabel) {
        Set<String> expanded = new java.util.HashSet<>();
        capturePaths(item, expanded);

        obj.setLoaded(false);
        pendingLoadCallbacks.put(item, () -> restoreExpansion(item, expanded));
        item.setExpanded(false);
        item.setExpanded(true);

        mainWindow.setStatus("Refreshed " + statusLabel);
    }

    public static String schemaOrDatabaseNoun(ConnectionProfile.DatabaseType type) {
        return switch (type) {
            case MYSQL, MARIADB, ORACLE -> "schema";
            default -> "database";
        };
    }

    /** Opens the show/hide databases (or, for MySQL/MariaDB/Oracle, schemas) popup. */
    public void showDatabaseFilterDialog(TreeItem<DbObject> connectionItem,
                                          ConnectionProfile profile) {
        AppExecutor.run(() -> {
            try {
                List<String> allDatabases = MetadataService.listDatabaseNames(profile);
                profile.setTotalDatabases(allDatabases.size());
                ConnectionStore.saveOrUpdate(profile);
                Platform.runLater(() -> {
                    String noun = schemaOrDatabaseNoun(profile.getType());
                    String defaultName = profile.getType() == ConnectionProfile.DatabaseType.ORACLE
                            ? profile.getUsername() : profile.getDatabase();
                    DatabaseFilterDialog dialog = new DatabaseFilterDialog(profile, allDatabases, noun, defaultName);
                    dialog.initOwner(getScene() == null ? null : getScene().getWindow());
                    dialog.showAndWait().ifPresent(selected -> {
                        profile.setVisibleDatabases(selected);
                        ConnectionStore.saveOrUpdate(profile);
                        refreshConnection(connectionItem, profile);
                    });
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not list databases: " + msg)).showAndWait());
            }
        });
    }

    private static final int EXPORT_ROW_CAP = 5000;

    /**
     * Fetches up to {@link #EXPORT_ROW_CAP} rows fresh from the table and opens
     * the Export Data dialog on the result. Capped rather than unbounded so a
     * quick "export this table" click can't accidentally try to pull millions
     * of rows into memory — for anything larger, open the data view and export
     * from a filtered/paged query instead.
     */
    private void exportTableData(ConnectionProfile profile, DbObject table) {
        mainWindow.setStatus("Preparing export for " + table.getName() + "\u2026");
        AppExecutor.run(() -> {
            try {
                var client = ClientRegistry.jdbc(profile, table.getCatalog());
                var result = client.fetchTablePage(table.qualifiedName(), 0, EXPORT_ROW_CAP, null, null);
                Platform.runLater(() -> {
                    mainWindow.setStatus("Ready");
                    ExportDataDialog.show(mainWindow.getOwnerWindow(), profile, table, result);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not load data to export: " + msg)).showAndWait());
            }
        });
    }

    /** Renames how a connection is labeled locally in the explorer — doesn't touch the server. */
    private void renameConnectionLabel(ConnectionProfile profile) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(profile.getName()));
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("Rename Connection");
        dialog.setHeaderText(null);
        dialog.setContentText("New name for this connection:");

        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty() || trimmed.equals(profile.getName())) return;
            profile.setName(trimmed);
            ConnectionStore.saveOrUpdate(profile);
            reload();
        });
    }

    /** File → (database context menu) → Rename… */
    private void renameDatabase(ConnectionProfile profile, String currentName) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(currentName));
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("Rename Database");
        dialog.setHeaderText(null);
        dialog.setContentText("New name for \"" + currentName + "\":");

        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty() || trimmed.equals(currentName)) return;
            AppExecutor.run(() -> {
                try {
                    DatabaseAdminService.renameDatabase(profile, currentName, trimmed);
                    Platform.runLater(() -> {
                        mainWindow.setStatus("Renamed " + currentName + " to " + trimmed);
                        reload();
                    });
                } catch (Exception ex) {
                    String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not rename database: " + msg)).showAndWait());
                }
            });
        });
    }

    /**
     * Standard destructive confirmation: warn, then require the user to
     * type the exact database name before DROP DATABASE is executed.
     */
    private void confirmAndDeleteDatabase(ConnectionProfile profile, String databaseName) {
        Alert warn = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                "This permanently deletes the database \"" + databaseName + "\" and everything in it. "
                        + "This cannot be undone.",
                ButtonType.YES, ButtonType.NO));
        warn.setTitle("Delete Database");
        warn.setHeaderText("Delete \"" + databaseName + "\"?");
        warn.initOwner(getScene() == null ? null : getScene().getWindow());
        if (warn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        TextInputDialog confirm = (TextInputDialog) DialogTheme.apply(new TextInputDialog());
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText(null);
        confirm.setContentText("Type the database name \"" + databaseName + "\" to confirm:");
        confirm.initOwner(getScene() == null ? null : getScene().getWindow());
        confirm.getDialogPane().setPrefWidth(420);

        String typed = confirm.showAndWait().orElse("");
        if (!typed.equals(databaseName)) {
            if (!typed.isEmpty()) {
                DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION, "Name didn't match — deletion cancelled."))
                        .showAndWait();
            }
            return;
        }

        AppExecutor.run(() -> {
            try {
                DatabaseAdminService.dropDatabase(profile, databaseName);
                Platform.runLater(() -> {
                    mainWindow.setStatus("Deleted database " + databaseName);
                    reload();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not delete database: " + msg)).showAndWait());
            }
        });
    }

    private void confirmAndDropCollection(ConnectionProfile profile, DbObject collection) {
        String name = collection.getName();
        Alert warn = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                "This permanently deletes the collection \"" + name + "\" and every document in it. "
                        + "This cannot be undone.",
                ButtonType.YES, ButtonType.NO));
        warn.setTitle("Drop Collection");
        warn.setHeaderText("Drop \"" + name + "\"?");
        warn.initOwner(getScene() == null ? null : getScene().getWindow());
        if (warn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        TextInputDialog confirm = (TextInputDialog) DialogTheme.apply(new TextInputDialog());
        confirm.setTitle("Confirm Drop");
        confirm.setHeaderText(null);
        confirm.setContentText("Type the collection name \"" + name + "\" to confirm:");
        confirm.initOwner(getScene() == null ? null : getScene().getWindow());
        confirm.getDialogPane().setPrefWidth(420);

        String typed = confirm.showAndWait().orElse("");
        if (!typed.equals(name)) {
            if (!typed.isEmpty()) {
                DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION, "Name didn't match — drop cancelled."))
                        .showAndWait();
            }
            return;
        }

        AppExecutor.run(() -> {
            try {
                var result = ClientRegistry.mongo(profile).drop(collection.getCatalog(), name);
                Platform.runLater(() -> {
                    mainWindow.setStatus(result.message());
                    reload();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not drop collection: " + msg)).showAndWait());
            }
        });
    }

    private static void copyToClipboard(String text) {
        if (text == null) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void promptAndOpenQueryFile(ConnectionProfile profile) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open SQL Query File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SQL Files (*.sql)", "*.sql"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );
        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file != null) {
            try {
                String sql = java.nio.file.Files.readString(file.toPath());
                mainWindow.openQueryTab(profile, null, sql);
            } catch (Exception ex) {
                mainWindow.setStatus("Could not open file: " + ex.getMessage());
            }
        }
    }

    private void promptRunSqlScript(ConnectionProfile profile) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Run SQL Script");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SQL Scripts (*.sql)", "*.sql"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );
        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file != null) {
            try {
                String sql = java.nio.file.Files.readString(file.toPath());
                mainWindow.openQueryTab(profile, null, sql);
                mainWindow.setStatus("Loaded script: " + file.getName());
            } catch (Exception ex) {
                mainWindow.setStatus("Could not run script: " + ex.getMessage());
            }
        }
    }

    private void showVirtualViewInfo(ConnectionProfile profile) {
        showVirtualViewInfo(profile, null);
    }

    private void showVirtualViewInfo(ConnectionProfile profile, String catalog) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(""));
        dialog.setTitle("New Virtual View");
        String target = (catalog != null && !catalog.isBlank()) ? catalog : (profile != null ? profile.getName() : "");
        dialog.setHeaderText("Create Virtual View for " + target);
        dialog.setContentText("View Query / Name:");
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewDef -> {
            if (!viewDef.isBlank() && mainWindow != null) {
                mainWindow.openQueryTab(profile, catalog, "-- Virtual View: " + viewDef + "\nSELECT * FROM " + viewDef + ";\n");
            }
        });
    }

    private void promptModifyVirtualView(ConnectionProfile profile, DbObject obj) {
        String current = (obj != null && obj.getKind() == Kind.VIRTUAL_VIEW) ? obj.getName() : "";
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(current));
        dialog.setTitle("Modify Virtual View");
        String target = (obj != null) ? obj.getName() : "Virtual View";
        dialog.setHeaderText("Modify Virtual View " + target);
        dialog.setContentText("View Query / Name:");
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewDef -> {
            if (!viewDef.isBlank() && mainWindow != null) {
                String cat = (obj != null) ? obj.getCatalog() : null;
                mainWindow.openQueryTab(profile, cat, "-- Modified Virtual View: " + viewDef + "\nSELECT * FROM " + viewDef + ";\n");
            }
        });
    }

    private void promptModifyLanguage(ConnectionProfile profile, DbObject obj) {
        String langName = obj != null ? obj.getName() : "plpgsql";
        String catalog = obj != null ? obj.getCatalog() : null;
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(langName));
        dialog.setTitle("Modify Language");
        dialog.setHeaderText("Modify Language \"" + langName + "\"");
        dialog.setContentText("Language Handler / Parameters:");
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(val -> {
            if (!val.isBlank() && mainWindow != null) {
                mainWindow.openQueryTab(profile, catalog, "-- Modify Language " + langName + "\n-- " + val + "\n");
            }
        });
    }

    private void showQuickDocumentation(ConnectionProfile profile) {
        showQuickDocumentation(profile, null);
    }

    private void showQuickDocumentation(ConnectionProfile profile, DbObject obj) {
        if (profile == null) return;
        if (obj == null || obj.getKind() == Kind.CONNECTION) {
            boolean connected = ClientRegistry.isConnected(profile);
            StringBuilder sb = new StringBuilder();
            sb.append("Data Source: ").append(profile.getName()).append("\n");
            sb.append("Database Type: ").append(profile.getType().getDisplayName()).append("\n");
            sb.append("Status: ").append(connected ? "Connected" : "Disconnected").append("\n");
            if (profile.getType() != ConnectionProfile.DatabaseType.SQLITE) {
                sb.append("Host: ").append(profile.getHost()).append(":").append(profile.getPort()).append("\n");
                if (profile.getUsername() != null && !profile.getUsername().isBlank()) {
                    sb.append("User: ").append(profile.getUsername()).append("\n");
                }
            }
            if (profile.getDatabase() != null && !profile.getDatabase().isBlank()) {
                sb.append(schemaOrDatabaseNoun(profile.getType()).equalsIgnoreCase("schema") ? "Schema: " : "Database: ")
                        .append(profile.getDatabase()).append("\n");
            }
            if (profile.getJdbcUrl() != null && !profile.getJdbcUrl().isBlank()) {
                sb.append("URL: ").append(profile.getJdbcUrl()).append("\n");
            }
            if (profile.getGroup() != null && !profile.getGroup().isBlank()) {
                sb.append("Group: ").append(profile.getGroup()).append("\n");
            }

            Alert alert = DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK));
            alert.setTitle("Quick Documentation");
            alert.setHeaderText("Data Source Details — " + profile.getName());
            alert.initOwner(getScene() == null ? null : getScene().getWindow());
            alert.showAndWait();
            return;
        }

        boolean connected = ClientRegistry.isConnected(profile);
        StringBuilder sb = new StringBuilder();
        String kindName = obj.getKind().name();
        sb.append(Character.toUpperCase(kindName.charAt(0))).append(kindName.substring(1).toLowerCase()).append(": ").append(obj.getName()).append("\n");
        sb.append("Data Source: ").append(profile.getName()).append("\n");
        sb.append("Database Type: ").append(profile.getType().getDisplayName()).append("\n");
        sb.append("Status: ").append(connected ? "Connected" : "Disconnected").append("\n");
        if (obj.getCatalog() != null && !obj.getCatalog().isBlank()) {
            sb.append("Catalog: ").append(obj.getCatalog()).append("\n");
        }
        if (obj.getSchema() != null && !obj.getSchema().isBlank()) {
            sb.append("Schema: ").append(obj.getSchema()).append("\n");
        }

        Alert alert = DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK));
        alert.setTitle("Quick Documentation");
        alert.setHeaderText(obj.getName() + " (" + profile.getName() + ")");
        alert.initOwner(getScene() == null ? null : getScene().getWindow());
        alert.showAndWait();
    }

    private void promptMoveToGroup(ConnectionProfile profile) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(profile.getGroup()));
        dialog.setTitle("Move to Group");
        dialog.setHeaderText("Assign \"" + profile.getName() + "\" to a group folder");
        dialog.setContentText("Group name (leave empty for root):");
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(groupName -> {
            profile.setGroup(groupName.trim());
            ConnectionStore.saveOrUpdate(profile);
            reload();
            mainWindow.setStatus("Moved " + profile.getName() + " to group: " + (groupName.isBlank() ? "root" : groupName.trim()));
        });
    }

    private void verifyConnection(ConnectionProfile profile) {
        mainWindow.setStatus("Verifying connection to " + profile.getName() + "…");
        AppExecutor.run(() -> {
            try {
                if (profile.getType() == ConnectionProfile.DatabaseType.MONGODB) {
                    var client = ClientRegistry.mongo(profile);
                    client.listDatabases();
                } else {
                    try (var conn = ClientRegistry.jdbc(profile, null).getConnection()) {
                        conn.getMetaData();
                    }
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Connection to " + profile.getName() + " verified successfully.");
                    Alert ok = DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                            "Connection to " + profile.getName() + " was successfully verified."));
                    ok.setTitle("Verify Connection");
                    ok.setHeaderText("Connection Successful");
                    ok.initOwner(getScene() == null ? null : getScene().getWindow());
                    ok.showAndWait();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                Platform.runLater(() -> {
                    mainWindow.setStatus("Connection verification failed: " + msg);
                    Alert err = DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Failed to connect to " + profile.getName() + ":\n" + msg));
                    err.setTitle("Verify Connection");
                    err.setHeaderText("Connection Failed");
                    err.initOwner(getScene() == null ? null : getScene().getWindow());
                    err.showAndWait();
                });
            }
        });
    }

    /**
     * Builds the comprehensive context menu for a connection root node.
     * Package-private so unit tests can inspect and verify the dynamic hierarchy.
     */
    ContextMenu buildConnectionMenu(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        ContextMenu menu = new ContextMenu();
        if (profile == null) return menu;
        boolean connected = ClientRegistry.isConnected(profile);
        String noun = schemaOrDatabaseNoun(profile.getType());
        String capNoun = Character.toUpperCase(noun.charAt(0)) + noun.substring(1);

        // For Oracle: show "Download Driver Files" at the very top of connection menu
        if (profile.getType() == ConnectionProfile.DatabaseType.ORACLE) {
            MenuItem downloadDriver = new MenuItem("Download Driver Files",
                    Icons.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, "#e05555", 12));
            downloadDriver.setOnAction(e -> mainWindow.setStatus("Oracle driver files"));
            menu.getItems().add(downloadDriver);
        }

        // 1. Properties (Ctrl+Alt+Shift+S)
        MenuItem properties = new MenuItem("Properties…");
        properties.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
        properties.setOnAction(e -> mainWindow.showEditConnectionDialog(profile));

        // 2. New >
        Menu newMenu = new Menu("New");
        MenuItem queryConsole = new MenuItem("Query Console");
        queryConsole.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        queryConsole.setOnAction(e -> mainWindow.openQueryTab(profile, null, null));

        MenuItem queryFile = new MenuItem("Query File…");
        queryFile.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
        queryFile.setOnAction(e -> promptAndOpenQueryFile(profile));

        newMenu.getItems().addAll(queryConsole, queryFile);

        // Dynamic engine-specific creation items:
        // - PostgreSQL: Database..., Role..., User...
        // - MySQL: Schema..., User..., Role...
        // - MariaDB: Schema..., User..., Role...
        // - SQL Server: Database, Server Principal
        // - Oracle: none at root New
        // - MongoDB, SQLite: none at root New
        switch (profile.getType()) {
            case POSTGRESQL -> {
                MenuItem newDb = new MenuItem("Database…");
                newDb.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));
                MenuItem newRole = new MenuItem("Role…");
                newRole.setOnAction(e -> CreateRoleDialog.show(mainWindow, profile));
                MenuItem newUser = new MenuItem("User…");
                newUser.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
                newMenu.getItems().addAll(new SeparatorMenuItem(), newDb, newRole, newUser);
            }
            case MYSQL, MARIADB -> {
                MenuItem newSchema = new MenuItem("Schema…");
                newSchema.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));
                MenuItem newUser = new MenuItem("User…");
                newUser.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
                MenuItem newRole = new MenuItem("Role…");
                newRole.setOnAction(e -> CreateRoleDialog.show(mainWindow, profile));
                newMenu.getItems().addAll(new SeparatorMenuItem(), newSchema, newUser, newRole);
            }
            case SQLSERVER -> {
                MenuItem newDb = new MenuItem("Database");
                newDb.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));
                MenuItem serverPrincipal = new MenuItem("Server Principal");
                serverPrincipal.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
                newMenu.getItems().addAll(new SeparatorMenuItem(), newDb, serverPrincipal);
            }
            case STRATOSDB -> {
                MenuItem newDb = new MenuItem("Database…");
                newDb.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));
                newMenu.getItems().addAll(new SeparatorMenuItem(), newDb);
            }
            case ORACLE -> {
                // Oracle has neither Database, Schema, User nor Role in root New menu.
                // It goes directly to Virtual View.
            }
            default -> {
                // SQLite, MongoDB: do not expose Database/Schema/User/Role creation in New
            }
        }

        MenuItem virtualView = new MenuItem("Virtual View");
        virtualView.setOnAction(e -> showVirtualViewInfo(profile));
        newMenu.getItems().addAll(new SeparatorMenuItem(), virtualView);

        appendCommonNewDataSourceItems(newMenu, profile, true);

        // 3. Copy/Paste >
        Menu copyPasteMenu = new Menu("Copy/Paste");
        MenuItem copyName = new MenuItem("Copy");
        copyName.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyName.setOnAction(e -> {
            copyToClipboard(profile.getName());
            mainWindow.setStatus("Copied to clipboard: " + profile.getName());
        });
        MenuItem copyPath = new MenuItem("Copy Path/Reference…");
        copyPath.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
        copyPath.setOnAction(e -> {
            String ref = profile.getJdbcUrl() != null && !profile.getJdbcUrl().isBlank()
                    ? profile.getJdbcUrl() : profile.getName();
            copyToClipboard(ref);
            mainWindow.setStatus("Copied reference: " + ref);
        });
        copyPasteMenu.getItems().addAll(copyName, copyPath);

        // 4. Refresh (Ctrl+F5)
        MenuItem refresh = new MenuItem("Refresh");
        refresh.setAccelerator(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN));
        refresh.setOnAction(e -> {
            if (item != null) refreshNode(item, obj, profile.getName());
            else reload();
        });

        // 5. Deactivate / Activate (Ctrl+F2)
        MenuItem activateToggle = new MenuItem(connected ? "Deactivate" : "Activate");
        activateToggle.setAccelerator(new KeyCodeCombination(KeyCode.F2, KeyCombination.CONTROL_DOWN));
        activateToggle.setOnAction(e -> {
            if (connected) {
                ClientRegistry.disconnect(profile);
                reload();
                mainWindow.setStatus("Deactivated " + profile.getName());
            } else {
                if (item != null) refreshNode(item, obj, profile.getName());
                else reload();
                mainWindow.setStatus("Activated " + profile.getName());
            }
        });

        // 6. Rename... (Shift+F6)
        MenuItem rename = new MenuItem("Rename…");
        rename.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN));
        rename.setOnAction(e -> renameConnectionLabel(profile));

        // 7. Move >
        Menu moveMenu = new Menu("Move");
        MenuItem moveToGroup = new MenuItem("Move to Group…");
        moveToGroup.setOnAction(e -> promptMoveToGroup(profile));
        moveMenu.getItems().add(moveToGroup);

        // 8. Quick Documentation (Ctrl+Q)
        MenuItem quickDoc = new MenuItem("Quick Documentation");
        quickDoc.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        quickDoc.setOnAction(e -> showQuickDocumentation(profile));

        // 9. Compare Schema Structure (Ctrl+D)
        MenuItem compareStructure = new MenuItem("Compare Schema Structure");
        compareStructure.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
        compareStructure.setDisable(true);

        // 10. Navigation >
        Menu navMenu = new Menu("Navigation");
        MenuItem jumpConsole = new MenuItem("Jump to Query Console");
        jumpConsole.setOnAction(e -> mainWindow.openQueryTab(profile, null, null));
        MenuItem selectInTree = new MenuItem("Select in Database Explorer");
        selectInTree.setAccelerator(new KeyCodeCombination(KeyCode.F1, KeyCombination.ALT_DOWN));
        selectInTree.setOnAction(e -> {
            if (item != null) tree.getSelectionModel().select(item);
        });
        navMenu.getItems().addAll(jumpConsole, selectInTree);

        // 11. Introspection Level >
        Menu introMenu = new Menu("Introspection Level");
        ToggleGroup introGroup = new ToggleGroup();
        RadioMenuItem introAuto = new RadioMenuItem("Auto");
        introAuto.setToggleGroup(introGroup);
        introAuto.setSelected(true);
        RadioMenuItem introHigh = new RadioMenuItem("High");
        introHigh.setToggleGroup(introGroup);
        RadioMenuItem introMed = new RadioMenuItem("Medium");
        introMed.setToggleGroup(introGroup);
        RadioMenuItem introLow = new RadioMenuItem("Low");
        introLow.setToggleGroup(introGroup);
        introMenu.getItems().addAll(introAuto, introHigh, introMed, introLow);

        // 12. SQL Scripts >
        Menu sqlScripts = new Menu("SQL Scripts");
        MenuItem openConsole = new MenuItem("Open Query Console");
        openConsole.setOnAction(e -> mainWindow.openQueryTab(profile, null, null));
        MenuItem runSqlScript = new MenuItem("Run SQL Script…");
        runSqlScript.setOnAction(e -> promptRunSqlScript(profile));
        sqlScripts.getItems().addAll(openConsole, runSqlScript);

        // 13. Object Actions >
        Menu objActions = new Menu("Object Actions");
        if (MetadataService.supportsDatabaseFilter(profile)) {
            MenuItem showHideDbs = new MenuItem("Show / Hide " + capNoun + "s…");
            showHideDbs.setOnAction(e -> showDatabaseFilterDialog(item, profile));
            objActions.getItems().add(showHideDbs);
        }
        MenuItem forgetPw = new MenuItem("Forget Saved Passwords");
        forgetPw.setOnAction(e -> {
            profile.setPassword("");
            ConnectionStore.saveOrUpdate(profile);
            mainWindow.setStatus("Forgot saved password for " + profile.getName());
        });
        objActions.getItems().add(forgetPw);

        // 14. Tools >
        Menu toolsMenu = new Menu("Tools");
        if (MetadataService.supportsDatabaseFilter(profile)) {
            MenuItem manageDbs = new MenuItem("Manage " + capNoun + "s…");
            manageDbs.setOnAction(e -> showDatabaseFilterDialog(item, profile));
            toolsMenu.getItems().add(manageDbs);
        }
        MenuItem verifyConn = new MenuItem("Verify Connection");
        verifyConn.setOnAction(e -> verifyConnection(profile));
        toolsMenu.getItems().add(verifyConn);

        // 15. Import/Export >
        Menu importExport = new Menu("Import/Export");
        String dumpName = switch (profile.getType()) {
            case POSTGRESQL -> "Dump with pg_dump…";
            case MYSQL -> "Dump with mysqldump…";
            case MARIADB -> "Dump with mariadb-dump…";
            case SQLITE -> "Export SQLite Database…";
            case MONGODB -> "Dump with mongodump…";
            case SQLSERVER -> "Export Database with bcp…";
            case ORACLE -> "Export with expdp…";
            case STRATOSDB -> "Dump with stratosdump…";
            default -> "Dump Database…";
        };
        MenuItem dumpItem = new MenuItem(dumpName);
        dumpItem.setOnAction(e -> {
            if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                StratosDumpService.dumpDatabase(mainWindow, profile, profile.getDatabase() != null ? profile.getDatabase() : "stratos");
            } else {
                mainWindow.setStatus(dumpName.replace("…", ""));
            }
        });
        MenuItem restoreItem = new MenuItem("Restore…");
        restoreItem.setOnAction(e -> {
            if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                StratosDumpService.restoreDatabase(mainWindow, profile, profile.getDatabase() != null ? profile.getDatabase() : "stratos");
            } else {
                mainWindow.setStatus("Restore database");
            }
        });
        importExport.getItems().addAll(dumpItem, restoreItem);

        // 16. Diagrams >
        Menu diagrams = new Menu("Diagrams");
        MenuItem showViz = new MenuItem("Show Visualization…");
        showViz.setOnAction(e -> mainWindow.setStatus("Visualization for " + profile.getName()));
        diagrams.getItems().add(showViz);

        // 17. DDL Mapping >
        Menu ddlMapping = new Menu("DDL Mapping");
        MenuItem editDdlMap = new MenuItem("Edit DDL Mapping…");
        editDdlMap.setOnAction(e -> mainWindow.setStatus("Edit DDL Mapping"));
        ddlMapping.getItems().add(editDdlMap);

        // 18. Diagnostics >
        Menu diagnostics = new Menu("Diagnostics");
        MenuItem diagRefresh = new MenuItem("Diagnostic Refresh");
        diagRefresh.setOnAction(e -> {
            if (item != null) refreshNode(item, obj, profile.getName());
            else reload();
            mainWindow.setStatus("Diagnostic refresh performed for " + profile.getName());
        });
        diagnostics.getItems().add(diagRefresh);

        // 19. Remove Data Source... (Delete)
        MenuItem remove = new MenuItem("Remove Data Source…");
        remove.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        remove.setOnAction(e -> {
            Alert confirm = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.CONFIRMATION,
                    "Remove connection \"" + profile.getName() + "\"?",
                    ButtonType.YES, ButtonType.NO));
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    ClientRegistry.disconnect(profile);
                    ConnectionStore.delete(profile);
                    reload();
                    mainWindow.setStatus("Removed data source " + profile.getName());
                }
            });
        });

        menu.getItems().addAll(
                properties,
                newMenu,
                copyPasteMenu,
                refresh,
                activateToggle,
                rename,
                moveMenu,
                quickDoc,
                compareStructure,
                navMenu,
                introMenu,
                sqlScripts
        );
        if (profile.getType() != ConnectionProfile.DatabaseType.ORACLE) {
            menu.getItems().add(objActions);
        }
        menu.getItems().addAll(
                toolsMenu,
                importExport,
                diagrams,
                ddlMapping,
                diagnostics,
                new SeparatorMenuItem(),
                remove
        );

        return menu;
    }

    private void appendCommonNewDataSourceItems(Menu newMenu, ConnectionProfile profile, boolean includeFolder) {
        Menu dataSourceSub = new Menu("Data Source", Icons.of(FontAwesomeSolid.DATABASE, "#868a91", 12));
        MenuItem dsPg = new MenuItem("PostgreSQL…", Icons.forDatabase(ConnectionProfile.DatabaseType.POSTGRESQL, 13));
        dsPg.setOnAction(e -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.POSTGRESQL));
        MenuItem dsMy = new MenuItem("MySQL…", Icons.forDatabase(ConnectionProfile.DatabaseType.MYSQL, 13));
        dsMy.setOnAction(e -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.MYSQL));
        MenuItem dsMaria = new MenuItem("MariaDB…", Icons.forDatabase(ConnectionProfile.DatabaseType.MARIADB, 13));
        dsMaria.setOnAction(e -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.MARIADB));
        MenuItem dsLite = new MenuItem("SQLite…", Icons.forDatabase(ConnectionProfile.DatabaseType.SQLITE, 13));
        dsLite.setOnAction(e -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.SQLITE));
        MenuItem dsMongo = new MenuItem("MongoDB…", Icons.forDatabase(ConnectionProfile.DatabaseType.MONGODB, 13));
        dsMongo.setOnAction(e -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.MONGODB));
        MenuItem dsMssql = new MenuItem("Microsoft SQL Server…", Icons.forDatabase(ConnectionProfile.DatabaseType.SQLSERVER, 13));
        dsMssql.setOnAction(e -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.SQLSERVER));
        MenuItem dsOracle = new MenuItem("Oracle…", Icons.forDatabase(ConnectionProfile.DatabaseType.ORACLE, 13));
        dsOracle.setOnAction(e -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.ORACLE));
        MenuItem dsAll = new MenuItem("All Data Sources…");
        dsAll.setOnAction(e -> mainWindow.showNewConnectionDialog());
        dataSourceSub.getItems().addAll(dsPg, dsMy, dsMaria, dsLite, dsMongo, dsMssql, dsOracle, new SeparatorMenuItem(), dsAll);

        Menu cloudSub = new Menu("Data Source from Cloud Provider", Icons.of(FontAwesomeSolid.CLOUD, "#868a91", 12));
        MenuItem cpGcp = new MenuItem("Google Cloud…");
        cpGcp.setOnAction(e -> mainWindow.setStatus("Google Cloud provider"));
        MenuItem cpAws = new MenuItem("Amazon Web Services (AWS)…");
        cpAws.setOnAction(e -> mainWindow.setStatus("AWS provider"));
        MenuItem cpAzure = new MenuItem("Microsoft Azure…");
        cpAzure.setOnAction(e -> mainWindow.setStatus("Azure provider"));
        cloudSub.getItems().addAll(cpGcp, cpAws, cpAzure);

        MenuItem dsTemplates = new MenuItem("Data Source Templates (Beta)", Icons.of(FontAwesomeSolid.SLIDERS_H, "#868a91", 12));
        dsTemplates.setOnAction(e -> mainWindow.setStatus("Data Source Templates"));

        MenuItem dsFromFile = new MenuItem("Data Source from File/Folder", Icons.of(FontAwesomeSolid.FOLDER, "#e0a44c", 12));
        dsFromFile.setOnAction(e -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.SQLITE));

        MenuItem dsFromUrl = new MenuItem("Data Source from URL", Icons.of(FontAwesomeSolid.GLOBE, "#4a88c7", 12));
        dsFromUrl.setOnAction(e -> mainWindow.showNewConnectionDialog());

        MenuItem ddlDs = new MenuItem("DDL Data Source", Icons.of(FontAwesomeSolid.FILE_CODE, "#3592c4", 12));
        ddlDs.setOnAction(e -> mainWindow.setStatus("DDL Data Source"));

        MenuItem driverItem = new MenuItem("Driver", Icons.of(FontAwesomeSolid.CUBES, "#868a91", 12));
        driverItem.setOnAction(e -> mainWindow.setStatus("Driver Configuration"));

        newMenu.getItems().addAll(
                new SeparatorMenuItem(),
                dataSourceSub,
                cloudSub,
                dsTemplates,
                dsFromFile,
                dsFromUrl,
                ddlDs,
                new SeparatorMenuItem()
        );

        if (includeFolder) {
            MenuItem folderItem = new MenuItem("Folder", Icons.of(FontAwesomeSolid.FOLDER, "#e0a44c", 12));
            folderItem.setOnAction(e -> promptMoveToGroup(profile));
            newMenu.getItems().add(folderItem);
        }
        newMenu.getItems().add(driverItem);
    }

    Menu buildPostgreSqlNewMenu(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        Menu newMenu = new Menu("New");

        String targetCatalog = resolveCatalogFor(item, obj);
        String schema = resolveSchemaFor(item, obj);

        // Query Console (Ctrl+Shift+Q)
        MenuItem queryConsole = new MenuItem("Query Console", Icons.of(FontAwesomeSolid.TERMINAL, "#4a88c7", 12));
        queryConsole.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        queryConsole.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog, null);
        });

        // Query File… (Ctrl+Alt+Shift+Q)
        MenuItem queryFile = new MenuItem("Query File…", Icons.of(FontAwesomeSolid.FILE_CODE, "#868a91", 12));
        queryFile.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
        queryFile.setOnAction(e -> promptAndOpenQueryFile(profile));

        newMenu.getItems().addAll(queryConsole, queryFile);

        DbObject.Kind kind = obj != null ? obj.getKind() : null;

        boolean isDbObjectsFolder = (kind == Kind.DATABASE_OBJECTS);
        boolean isVirtualViewsFolder = (kind == Kind.VIRTUAL_VIEWS_FOLDER);
        boolean isDbObjectSubFolder = (kind == Kind.ACCESS_METHODS_FOLDER
                || kind == Kind.CASTS_FOLDER
                || kind == Kind.EXTENSIONS_FOLDER
                || kind == Kind.LANGUAGES_FOLDER);
        boolean isGenericFolder = isDbObjectsFolder || isDbObjectSubFolder;
        boolean isTableSubFolder = (kind == Kind.COLUMNS_FOLDER
                || kind == Kind.KEYS_FOLDER
                || kind == Kind.FOREIGN_KEYS_FOLDER
                || kind == Kind.INDEXES_FOLDER
                || kind == Kind.PARTITIONS_FOLDER
                || kind == Kind.FIELDS_FOLDER);
        boolean isTableSubItem = (kind == Kind.COLUMN
                || kind == Kind.KEY
                || kind == Kind.FOREIGN_KEY
                || kind == Kind.INDEX
                || kind == Kind.FIELD);
        boolean isTableSubNode = isTableSubFolder || isTableSubItem;

        boolean isTable = (obj != null && (kind == Kind.TABLE || kind == Kind.VIEW || kind == Kind.PARTITION));
        boolean isSequenceOrSeqFolder = (obj != null && (kind == Kind.SEQUENCE || kind == Kind.SEQUENCES_FOLDER));
        boolean isVirtualView = (kind == Kind.VIRTUAL_VIEW);
        boolean isDbObjectItem = (kind == Kind.ACCESS_METHOD
                || kind == Kind.CAST
                || kind == Kind.EXTENSION
                || kind == Kind.LANGUAGE);

        // For Database Objects & its subfolders (access methods, casts, extensions, languages):
        // Image 1 & Image 3: Only Query Console, Query File, then Data Source items
        if (isGenericFolder) {
            appendCommonNewDataSourceItems(newMenu, profile, false);
            return newMenu;
        }

        MenuItem tableItem = new MenuItem("Table", Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12));
        tableItem.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                    "CREATE TABLE \"" + schema + "\".\"new_table\" (\n    id SERIAL PRIMARY KEY\n);\n");
        });

        MenuItem matViewItem = new MenuItem("Materialized View", Icons.of(FontAwesomeSolid.EYE, "#e0a44c", 12));
        matViewItem.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                    "CREATE MATERIALIZED VIEW \"" + schema + "\".\"new_matview\" AS\nSELECT 1;\n");
        });

        MenuItem viewItem = new MenuItem("View", Icons.of(FontAwesomeSolid.EYE, "#57965c", 12));
        viewItem.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                    "CREATE VIEW \"" + schema + "\".\"new_view\" AS\nSELECT 1;\n");
        });

        MenuItem opItem = new MenuItem("Operator", Icons.of(FontAwesomeSolid.DOT_CIRCLE, "#3592c4", 12));
        opItem.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                    "CREATE OPERATOR \"" + schema + "\".### (\n    FUNCTION = func_name,\n    LEFTARG = type1,\n    RIGHTARG = type2\n);\n");
        });

        MenuItem seqItem = new MenuItem("Sequence", Icons.of(FontAwesomeSolid.SORT_NUMERIC_DOWN, "#e0a44c", 12));
        seqItem.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                    "CREATE SEQUENCE \"" + schema + "\".\"new_seq\";\n");
        });

        MenuItem collaItem = new MenuItem("Collation", Icons.of(FontAwesomeSolid.SORT_ALPHA_DOWN, "#868a91", 11));
        collaItem.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                    "CREATE COLLATION \"" + schema + "\".\"new_collation\" (LOCALE = 'en_US.utf8');\n");
        });

        MenuItem schemaItem = new MenuItem("Schema", Icons.of(FontAwesomeSolid.PROJECT_DIAGRAM, "#3592c4", 12));
        schemaItem.setOnAction(e -> promptCreateSchema(profile, targetCatalog));

        MenuItem langItem = new MenuItem("Language", Icons.of(FontAwesomeSolid.CODE, "#3592c4", 11));
        langItem.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                    "CREATE EXTENSION IF NOT EXISTS plpgsql;\n");
        });

        MenuItem dbItem = new MenuItem("Database", Icons.of(FontAwesomeSolid.DATABASE, "#3592c4", 12));
        dbItem.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));

        MenuItem roleItem = new MenuItem("Role", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
        roleItem.setOnAction(e -> CreateRoleDialog.show(mainWindow, profile));

        MenuItem userItem = new MenuItem("User", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
        userItem.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));

        MenuItem routineItem = new MenuItem("Routine", Icons.of(FontAwesomeSolid.COG, "#c77dbb", 12));
        routineItem.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                    "CREATE OR REPLACE FUNCTION \"" + schema + "\".\"new_func\"()\nRETURNS void AS $$\nBEGIN\nEND;\n$$ LANGUAGE plpgsql;\n");
        });

        MenuItem virtualView = new MenuItem("Virtual View", Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 12));
        virtualView.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));

        MenuItem vCol = new MenuItem("Virtual Column", Icons.of(FontAwesomeSolid.COLUMNS, "#c77dbb", 11));
        vCol.setOnAction(e -> {
            if (mainWindow != null) mainWindow.setStatus("Virtual Column on " + (obj != null ? obj.getName() : ""));
        });
        MenuItem vFk = new MenuItem("Virtual Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#c77dbb", 11));
        vFk.setOnAction(e -> {
            if (mainWindow != null) mainWindow.setStatus("Virtual Foreign Key on " + (obj != null ? obj.getName() : ""));
        });

        // Image 2: Virtual Views Folder
        if (isVirtualViewsFolder) {
            newMenu.getItems().addAll(
                    new SeparatorMenuItem(),
                    virtualView,
                    new SeparatorMenuItem(),
                    schemaItem, langItem, dbItem, roleItem, userItem
            );
            appendCommonNewDataSourceItems(newMenu, profile, false);
            return newMenu;
        }

        // Image 5: Virtual View item (sessions)
        if (isVirtualView) {
            newMenu.getItems().addAll(
                    new SeparatorMenuItem(),
                    vCol, vFk, virtualView,
                    new SeparatorMenuItem(),
                    schemaItem, langItem, dbItem, roleItem, userItem
            );
            appendCommonNewDataSourceItems(newMenu, profile, false);
            return newMenu;
        }

        // Image 4: Database Object Item (brin, cast, extension, language)
        if (isDbObjectItem) {
            newMenu.getItems().add(new SeparatorMenuItem());
            if (kind == Kind.LANGUAGE) {
                newMenu.getItems().addAll(langItem, schemaItem, dbItem, roleItem, userItem);
            } else {
                newMenu.getItems().addAll(schemaItem, langItem, dbItem, roleItem, userItem);
            }
            newMenu.getItems().addAll(
                    new SeparatorMenuItem(),
                    virtualView
            );
            appendCommonNewDataSourceItems(newMenu, profile, false);
            return newMenu;
        }

        // Table / View / Partition or Table Sub-Folder / Sub-Item (columns, keys, foreign keys, indexes, etc.)
        if (isTable || isTableSubNode) {
            DbObject parentTable = resolveParentTableFor(item, obj);
            String tblName = parentTable != null ? parentTable.getName()
                    : (obj.getTableName() != null ? obj.getTableName() : obj.getName());

            MenuItem colItem = new MenuItem("Column", Icons.of(FontAwesomeSolid.COLUMNS, "#868a91", 11));
            colItem.setOnAction(e -> {
                if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                        "ALTER TABLE \"" + schema + "\".\"" + tblName + "\" ADD COLUMN new_column VARCHAR(255);\n");
            });

            MenuItem uqItem = new MenuItem("Unique Key", Icons.of(FontAwesomeSolid.KEY, "#e0a44c", 11));
            uqItem.setOnAction(e -> {
                if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                        "ALTER TABLE \"" + schema + "\".\"" + tblName + "\" ADD CONSTRAINT uq_" + tblName + "_new UNIQUE (column_name);\n");
            });

            MenuItem fkItem = new MenuItem("Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#4a88c7", 11));
            fkItem.setOnAction(e -> {
                if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                        "ALTER TABLE \"" + schema + "\".\"" + tblName + "\" ADD CONSTRAINT fk_" + tblName + "_ref FOREIGN KEY (column_name) REFERENCES target_table (target_col);\n");
            });

            MenuItem idxItem = new MenuItem("Index", Icons.of(FontAwesomeSolid.BOLT, "#e0a44c", 11));
            idxItem.setOnAction(e -> {
                if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                        "CREATE INDEX idx_" + tblName + "_col ON \"" + schema + "\".\"" + tblName + "\" (column_name);\n");
            });

            MenuItem chkItem = new MenuItem("Check", Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
            chkItem.setOnAction(e -> {
                if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                        "ALTER TABLE \"" + schema + "\".\"" + tblName + "\" ADD CONSTRAINT chk_" + tblName + " CHECK (/* condition */);\n");
            });

            if (isTableSubNode) {
                newMenu.getItems().add(new SeparatorMenuItem());
                if (kind == Kind.KEYS_FOLDER || kind == Kind.KEY) {
                    newMenu.getItems().addAll(uqItem, colItem, fkItem, idxItem, chkItem);
                } else if (kind == Kind.FOREIGN_KEYS_FOLDER || kind == Kind.FOREIGN_KEY) {
                    newMenu.getItems().addAll(fkItem, colItem, uqItem, idxItem, chkItem);
                } else if (kind == Kind.INDEXES_FOLDER || kind == Kind.INDEX) {
                    newMenu.getItems().addAll(idxItem, colItem, uqItem, fkItem, chkItem);
                } else {
                    newMenu.getItems().addAll(colItem, uqItem, fkItem, idxItem, chkItem);
                }
                newMenu.getItems().addAll(
                        tableItem, matViewItem, viewItem, opItem, seqItem, collaItem, schemaItem, langItem, dbItem, roleItem, userItem,
                        new SeparatorMenuItem(),
                        vCol, vFk, virtualView
                );
                appendCommonNewDataSourceItems(newMenu, profile, false);
                return newMenu;
            }

            newMenu.getItems().addAll(new SeparatorMenuItem(), colItem, uqItem, fkItem, idxItem, chkItem, new SeparatorMenuItem(),
                    tableItem, matViewItem, viewItem, opItem, seqItem, collaItem, schemaItem, langItem, dbItem, roleItem, userItem, routineItem,
                    new SeparatorMenuItem(),
                    vCol, vFk, virtualView);
            appendCommonNewDataSourceItems(newMenu, profile, false);
            return newMenu;
        }

        // Sequences or Sequences Folder
        if (isSequenceOrSeqFolder) {
            newMenu.getItems().addAll(new SeparatorMenuItem(),
                    seqItem, tableItem, matViewItem, viewItem, opItem, collaItem, schemaItem, langItem, dbItem, roleItem, userItem,
                    new SeparatorMenuItem(),
                    virtualView);
            appendCommonNewDataSourceItems(newMenu, profile, false);
            return newMenu;
        }

        // Schema / Other Folders (tables folder, views folder, etc.)
        newMenu.getItems().addAll(new SeparatorMenuItem(),
                tableItem, matViewItem, viewItem, opItem, seqItem, collaItem, schemaItem, langItem, dbItem, roleItem, userItem, routineItem,
                new SeparatorMenuItem(),
                virtualView);
        appendCommonNewDataSourceItems(newMenu, profile, false);

        return newMenu;
    }

    ContextMenu buildPostgreSqlMenu(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        if (profile == null || obj == null) return null;

        DbObject.Kind kind = obj.getKind();

        boolean isDbObjectsFolder = (kind == Kind.DATABASE_OBJECTS);
        boolean isVirtualViewsFolder = (kind == Kind.VIRTUAL_VIEWS_FOLDER);
        boolean isDbObjectSubFolder = (kind == Kind.ACCESS_METHODS_FOLDER
                || kind == Kind.CASTS_FOLDER
                || kind == Kind.EXTENSIONS_FOLDER
                || kind == Kind.LANGUAGES_FOLDER);
        boolean isGenericFolder = isDbObjectsFolder || isDbObjectSubFolder;
        boolean isTableSubFolder = (kind == Kind.COLUMNS_FOLDER
                || kind == Kind.KEYS_FOLDER
                || kind == Kind.FOREIGN_KEYS_FOLDER
                || kind == Kind.INDEXES_FOLDER
                || kind == Kind.PARTITIONS_FOLDER
                || kind == Kind.FIELDS_FOLDER);
        boolean isTableSubItem = (kind == Kind.COLUMN
                || kind == Kind.KEY
                || kind == Kind.FOREIGN_KEY
                || kind == Kind.INDEX
                || kind == Kind.FIELD);

        boolean isCategoryFolder = (kind == Kind.TABLES_FOLDER
                || kind == Kind.SEQUENCES_FOLDER
                || kind == Kind.VIEWS_FOLDER
                || kind == Kind.PROCEDURES_FOLDER
                || kind == Kind.FUNCTIONS_FOLDER
                || isDbObjectsFolder
                || isVirtualViewsFolder
                || isDbObjectSubFolder
                || isTableSubFolder);

        boolean isTable = (kind == Kind.TABLE || kind == Kind.VIEW || kind == Kind.PARTITION);
        boolean isSequence = (kind == Kind.SEQUENCE);
        boolean isSchema = (kind == Kind.SCHEMA);
        boolean isVirtualView = (kind == Kind.VIRTUAL_VIEW);
        boolean isDbObjectItem = (kind == Kind.ACCESS_METHOD
                || kind == Kind.CAST
                || kind == Kind.EXTENSION
                || kind == Kind.LANGUAGE);

        if (!isCategoryFolder && !isTable && !isSequence && !isSchema && !isVirtualView && !isDbObjectItem && !isTableSubItem) {
            return null;
        }

        ContextMenu menu = new ContextMenu();
        String targetCatalog = resolveCatalogFor(item, obj);

        // 1. + New >
        Menu newMenu = buildPostgreSqlNewMenu(item, obj, profile);
        menu.getItems().add(newMenu);

        // 2. Modify <Object>… (Ctrl+F6) - present on Schema, Sequence, Table/View, Virtual Views Folder, Virtual View, Language, Keys Folder, Foreign Keys Folder, Indexes Folder, Table Sub-Items
        boolean hasModify = isSchema || isSequence || isTable || isVirtualViewsFolder || isVirtualView
                || kind == Kind.LANGUAGE
                || kind == Kind.KEYS_FOLDER
                || kind == Kind.FOREIGN_KEYS_FOLDER
                || kind == Kind.INDEXES_FOLDER
                || isTableSubItem;
        if (hasModify) {
            String modifyTitle;
            if (isSchema) {
                modifyTitle = "Modify Schema…";
            } else if (isSequence) {
                modifyTitle = "Modify Sequence…";
            } else if (isVirtualViewsFolder || isVirtualView) {
                modifyTitle = "Modify Virtual View…";
            } else if (kind == Kind.LANGUAGE) {
                modifyTitle = "Modify Language…";
            } else if (kind == Kind.KEYS_FOLDER || kind == Kind.KEY) {
                modifyTitle = "Modify Key…";
            } else if (kind == Kind.FOREIGN_KEYS_FOLDER || kind == Kind.FOREIGN_KEY) {
                modifyTitle = "Modify Foreign Key…";
            } else if (kind == Kind.INDEXES_FOLDER || kind == Kind.INDEX) {
                modifyTitle = "Modify Index…";
            } else if (kind == Kind.COLUMN) {
                modifyTitle = "Modify Column…";
            } else if (kind == Kind.VIEW) {
                modifyTitle = "Modify View…";
            } else {
                modifyTitle = "Modify Table…";
            }
            MenuItem modifyItem = new MenuItem(modifyTitle);
            modifyItem.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
            modifyItem.setOnAction(e -> {
                if (isSchema) {
                    ModifyDatabaseDialog.show(mainWindow, profile, obj.getName());
                } else if (isSequence) {
                    ModifySequenceDialog.show(mainWindow, profile, obj);
                } else if (isVirtualViewsFolder || isVirtualView) {
                    promptModifyVirtualView(profile, obj);
                } else if (kind == Kind.LANGUAGE) {
                    promptModifyLanguage(profile, obj);
                } else {
                    DbObject targetTable = resolveParentTableFor(item, obj);
                    ModifyTableDialog.show(mainWindow, profile, targetTable != null ? targetTable : obj);
                }
            });
            menu.getItems().add(modifyItem);
        }

        // 3. Copy/Paste >
        Menu copyPasteMenu = new Menu("Copy/Paste");
        MenuItem copyName = new MenuItem("Copy");
        copyName.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyName.setOnAction(e -> {
            copyToClipboard(obj.getName());
            if (mainWindow != null) mainWindow.setStatus("Copied to clipboard: " + obj.getName());
        });
        MenuItem copyPath = new MenuItem("Copy Path/Reference…");
        copyPath.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
        copyPath.setOnAction(e -> {
            String ref = obj.qualifiedName();
            copyToClipboard(ref);
            if (mainWindow != null) mainWindow.setStatus("Copied reference: " + ref);
        });
        copyPasteMenu.getItems().addAll(copyName, copyPath);
        if (isCategoryFolder) {
            copyPasteMenu.setDisable(true);
        }
        menu.getItems().add(copyPasteMenu);

        // 4. Edit Data (F4) on Table/View/Virtual View/Table Sub-Item OR Open the List (F4) on category folders
        if (isTable || isVirtualView || isTableSubItem) {
            MenuItem editDataItem = new MenuItem("Edit Data");
            editDataItem.setGraphic(Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12));
            editDataItem.setAccelerator(new KeyCodeCombination(KeyCode.F4));
            editDataItem.setOnAction(e -> {
                DbObject targetTable = resolveParentTableFor(item, obj);
                if (targetTable != null && mainWindow != null) {
                    mainWindow.openDataTab(profile, targetTable);
                } else {
                    editData();
                }
            });
            menu.getItems().add(editDataItem);
        } else if (isCategoryFolder) {
            MenuItem openListItem = new MenuItem("Open the List");
            openListItem.setAccelerator(new KeyCodeCombination(KeyCode.F4));
            openListItem.setOnAction(e -> {
                if (item != null) item.setExpanded(!item.isExpanded());
            });
            menu.getItems().add(openListItem);
        }

        // 5. Refresh (Ctrl+F5)
        MenuItem refresh = new MenuItem("Refresh");
        refresh.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#868a91", 12));
        refresh.setAccelerator(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN));
        refresh.setOnAction(e -> {
            if (item != null) refreshNode(item, obj, obj.getName());
            else reload();
        });
        menu.getItems().add(refresh);

        menu.getItems().add(new SeparatorMenuItem());

        // 6. Rename… (Shift+F6) - disabled on category folders, db object items, virtual views, table sub-items
        MenuItem rename = new MenuItem("Rename…");
        rename.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN));
        if (isCategoryFolder || isDbObjectItem || isVirtualView || isTableSubItem) {
            rename.setDisable(true);
        } else if (isSchema) {
            rename.setOnAction(e -> promptRenameSchema(profile, obj));
        } else if (isSequence) {
            rename.setOnAction(e -> promptRenameSequence(profile, obj));
        } else {
            rename.setOnAction(e -> promptRenameTable(profile, obj));
        }
        menu.getItems().add(rename);

        if (!isCategoryFolder) {
            menu.getItems().add(new SeparatorMenuItem());
        }

        // 7. Quick Documentation (Ctrl+Q)
        MenuItem quickDoc = new MenuItem("Quick Documentation");
        quickDoc.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        if (isCategoryFolder) {
            quickDoc.setDisable(true);
        } else {
            quickDoc.setOnAction(e -> showQuickDocumentation(profile, obj));
        }
        menu.getItems().add(quickDoc);

        // 8. Compare Schema Structure With… (Ctrl+D) / Compare Schema Structure (disabled)
        if (isCategoryFolder || isVirtualView) {
            MenuItem compareStructure = new MenuItem("Compare Schema Structure");
            compareStructure.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
            compareStructure.setDisable(true);
            menu.getItems().add(compareStructure);
        } else {
            MenuItem compareStructure = new MenuItem("Compare Schema Structure With…");
            compareStructure.setGraphic(Icons.of(FontAwesomeSolid.EXCHANGE_ALT, "#868a91", 12));
            compareStructure.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
            compareStructure.setDisable(true);
            menu.getItems().add(compareStructure);
        }

        // 9. Navigation > (omitted on category folders)
        if (!isCategoryFolder) {
            Menu navMenu = new Menu("Navigation");
            MenuItem goToDdl = new MenuItem("Go to DDL");
            goToDdl.setOnAction(e -> {
                if (mainWindow != null) {
                    DbObject targetTable = resolveParentTableFor(item, obj);
                    mainWindow.openDdlTab(profile, targetTable != null ? targetTable : obj);
                }
            });
            MenuItem jumpConsole = new MenuItem("Jump to Query Console");
            jumpConsole.setAccelerator(new KeyCodeCombination(KeyCode.F10, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
            jumpConsole.setOnAction(e -> jumpToQueryConsole());
            MenuItem selectInTree = new MenuItem("Select in Database Explorer");
            selectInTree.setAccelerator(new KeyCodeCombination(KeyCode.F1, KeyCombination.ALT_DOWN));
            selectInTree.setOnAction(e -> {
                if (item != null) {
                    tree.getSelectionModel().select(item);
                }
            });
            navMenu.getItems().addAll(goToDdl, jumpConsole, selectInTree);
            menu.getItems().add(navMenu);
        }

        // 10. Find Usages (Alt+Shift+7) (omitted on category folders)
        if (!isCategoryFolder) {
            MenuItem findUsages = new MenuItem("Find Usages");
            findUsages.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT7, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
            findUsages.setDisable(true);
            menu.getItems().add(findUsages);
        }

        menu.getItems().add(new SeparatorMenuItem());

        // 11. Introspection Level >
        Menu introMenu = new Menu("Introspection Level");
        ToggleGroup introGroup = new ToggleGroup();
        RadioMenuItem introAuto = new RadioMenuItem("Auto");
        introAuto.setToggleGroup(introGroup);
        introAuto.setSelected(true);
        RadioMenuItem introHigh = new RadioMenuItem("High");
        introHigh.setToggleGroup(introGroup);
        RadioMenuItem introMed = new RadioMenuItem("Medium");
        introMed.setToggleGroup(introGroup);
        RadioMenuItem introLow = new RadioMenuItem("Low");
        introLow.setToggleGroup(introGroup);
        introMenu.getItems().addAll(introAuto, introHigh, introMed, introLow);
        menu.getItems().add(introMenu);

        menu.getItems().add(new SeparatorMenuItem());

        // 12. SQL Scripts >
        Menu sqlScripts = new Menu("SQL Scripts");
        MenuItem openConsole = new MenuItem("Open Query Console");
        openConsole.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog, null);
        });
        MenuItem runSqlScript = new MenuItem("Run SQL Script…");
        runSqlScript.setOnAction(e -> promptRunSqlScript(profile));
        MenuItem genDdl = new MenuItem("Generate DDL to Query Console");
        genDdl.setOnAction(e -> {
            AppExecutor.run(() -> {
                try {
                    String ddl = DdlGeneratorService.generateDdl(profile, obj);
                    Platform.runLater(() -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog, ddl);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "-- Could not generate DDL: " + ex.getMessage());
                    });
                }
            });
        });
        sqlScripts.getItems().addAll(openConsole, runSqlScript, genDdl);
        if (isTable) {
            MenuItem select = new MenuItem("New Query: SELECT *");
            select.setOnAction(e -> mainWindow.openQueryTab(profile, obj.getCatalog(),
                    selectTopRowsSql(profile.getType(), obj)));
            MenuItem count = new MenuItem("New Query: COUNT(*)");
            count.setOnAction(e -> mainWindow.openQueryTab(profile, obj.getCatalog(),
                    "SELECT COUNT(*) FROM " + obj.qualifiedName() + ";"));
            sqlScripts.getItems().addAll(new SeparatorMenuItem(), select, count);
        }
        menu.getItems().add(sqlScripts);

        // 13. Object Actions > (omitted on SEQUENCES_FOLDER, COLUMNS_FOLDER, INDEXES_FOLDER, generic folders, virtual views folder, virtual view)
        boolean includeObjActions = !isGenericFolder && !isVirtualViewsFolder && !isVirtualView
                && kind != Kind.SEQUENCES_FOLDER
                && kind != Kind.COLUMNS_FOLDER
                && kind != Kind.INDEXES_FOLDER;
        if (includeObjActions) {
            Menu objActions = new Menu("Object Actions");
            MenuItem showHideSchemas = new MenuItem("Show / Hide Schemas…");
            TreeItem<DbObject> connItem = item != null ? item : findConnectionItem(profile);
            showHideSchemas.setOnAction(e -> showDatabaseFilterDialog(connItem, profile));
            MenuItem forgetPw = new MenuItem("Forget Saved Passwords");
            forgetPw.setOnAction(e -> {
                profile.setPassword("");
                ConnectionStore.saveOrUpdate(profile);
                if (mainWindow != null) mainWindow.setStatus("Forgot saved password for " + profile.getName());
            });
            objActions.getItems().addAll(showHideSchemas, forgetPw);
            if (isTable || isSequence) {
                MenuItem dropObjAction = new MenuItem("Drop…");
                dropObjAction.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
                dropObjAction.setOnAction(e -> {
                    List<DbObject> targets = getSelectedDbObjectsForAction(obj);
                    ConfirmDropDialog.show(mainWindow, profile, targets, this::reload);
                });
                objActions.getItems().add(dropObjAction);
            }
            menu.getItems().add(objActions);
        }

        // 14. Tools >
        Menu toolsMenu = new Menu("Tools");
        MenuItem manageSchemas = new MenuItem("Manage Schemas…");
        TreeItem<DbObject> connItem = item != null ? item : findConnectionItem(profile);
        manageSchemas.setOnAction(e -> showDatabaseFilterDialog(connItem, profile));
        MenuItem verifyConn = new MenuItem("Verify Connection");
        verifyConn.setOnAction(e -> verifyConnection(profile));
        toolsMenu.getItems().addAll(manageSchemas, verifyConn);
        menu.getItems().add(toolsMenu);

        // 15. Import/Export > (omitted on generic folders: DATABASE_OBJECTS, ACCESS_METHODS_FOLDER, etc.)
        boolean includeImportExport = !isGenericFolder;
        if (includeImportExport) {
            Menu importExport = new Menu("Import/Export");
            if (isTable) {
                MenuItem exportData = new MenuItem("Export Data to File…");
                exportData.setOnAction(e -> exportTableData(profile, obj));
                MenuItem importData = new MenuItem("Import Data from File(s)…");
                importData.setOnAction(e -> ImportDataDialog.show(mainWindow, profile, obj));
                MenuItem exportPgDump = new MenuItem("Export with 'pg_dump'…");
                exportPgDump.setOnAction(e -> PgDumpDialog.show(mainWindow, profile, targetCatalog));
                MenuItem restorePgRestore = new MenuItem("Restore with 'pg_restore'…");
                restorePgRestore.setOnAction(e -> RestoreDialog.show(mainWindow, profile, targetCatalog));
                importExport.getItems().addAll(exportData, importData, new SeparatorMenuItem(), exportPgDump, restorePgRestore);
            } else {
                MenuItem dumpItem = new MenuItem("Dump with pg_dump…");
                dumpItem.setOnAction(e -> PgDumpDialog.show(mainWindow, profile, targetCatalog));
                MenuItem restoreItem = new MenuItem("Restore…");
                restoreItem.setOnAction(e -> RestoreDialog.show(mainWindow, profile, targetCatalog));
                importExport.getItems().addAll(dumpItem, restoreItem);
            }
            menu.getItems().add(importExport);
        }

        // 16. Diagrams >
        Menu diagrams = new Menu("Diagrams");
        MenuItem showDiagram = new MenuItem("Show Diagram…");
        showDiagram.setOnAction(e -> {
            if (mainWindow != null) {
                if (isTable || isTableSubItem) {
                    DbObject targetTable = resolveParentTableFor(item, obj);
                    mainWindow.openDiagramTab(profile, targetTable != null ? targetTable : obj);
                } else {
                    mainWindow.openDatabaseDiagramTab(profile, targetCatalog);
                }
            }
        });
        diagrams.getItems().add(showDiagram);
        menu.getItems().add(diagrams);

        menu.getItems().add(new SeparatorMenuItem());

        // 17. Diagnostics >
        Menu diagnostics = new Menu("Diagnostics");
        MenuItem diagRefresh = new MenuItem("Diagnostic Refresh");
        diagRefresh.setOnAction(e -> {
            if (item != null) refreshNode(item, obj, obj.getName());
            else reload();
            if (mainWindow != null) mainWindow.setStatus("Diagnostic refresh performed for " + obj.getName());
        });
        diagnostics.getItems().add(diagRefresh);
        menu.getItems().add(diagnostics);

        menu.getItems().add(new SeparatorMenuItem());

        // 18. Drop… (Delete)
        MenuItem drop = new MenuItem("Drop…");
        drop.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        if (isCategoryFolder) {
            drop.setDisable(true);
        } else if (isTable || isSequence || isSchema) {
            drop.setOnAction(e -> {
                List<DbObject> targets = getSelectedDbObjectsForAction(obj);
                ConfirmDropDialog.show(mainWindow, profile, targets, this::reload);
            });
        } else {
            drop.setOnAction(e -> confirmAndDropGenericObject(profile, obj));
        }
        menu.getItems().add(drop);

        return menu;
    }

    Menu buildEngineNewMenu(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        Menu newMenu = new Menu("New");

        String targetCatalog = resolveCatalogFor(item, obj, profile);
        String schema = resolveSchemaFor(item, obj, profile);

        MenuItem queryConsole = new MenuItem("Query Console", Icons.of(FontAwesomeSolid.TERMINAL, "#4a88c7", 12));
        queryConsole.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        queryConsole.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog, null);
        });

        MenuItem queryFile = new MenuItem("Query File…", Icons.of(FontAwesomeSolid.FILE_CODE, "#868a91", 12));
        queryFile.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
        queryFile.setOnAction(e -> promptAndOpenQueryFile(profile));

        newMenu.getItems().addAll(queryConsole, queryFile);

        DbObject.Kind kind = obj != null ? obj.getKind() : null;
        boolean isServerObjectsFolder = (kind == Kind.SERVER_OBJECTS
                || kind == Kind.TABLESPACES_FOLDER
                || kind == Kind.USERS_FOLDER
                || kind == Kind.COLLATIONS_FOLDER
                || kind == Kind.DATABASE_OBJECTS
                || kind == Kind.ASSEMBLIES_FOLDER
                || kind == Kind.FILEGROUPS_FOLDER
                || kind == Kind.DATABASE_PRINCIPALS_FOLDER
                || kind == Kind.LINKED_SERVERS_FOLDER
                || kind == Kind.SERVER_PRINCIPALS_FOLDER
                || kind == Kind.ROLES_FOLDER);
        boolean isVirtualViewsFolder = (kind == Kind.VIRTUAL_VIEWS_FOLDER);

        if (isServerObjectsFolder) {
            appendCommonNewDataSourceItems(newMenu, profile, false);
            return newMenu;
        }

        if (kind == Kind.VIRTUAL_VIEW || (isVirtualViewsFolder && (profile.getType() == ConnectionProfile.DatabaseType.MYSQL || profile.getType() == ConnectionProfile.DatabaseType.MARIADB))) {
            MenuItem vColItem = new MenuItem("Virtual Column", Icons.of(FontAwesomeSolid.COLUMNS, "#c77dbb", 11));
            vColItem.setOnAction(e -> {
                if (mainWindow != null) mainWindow.setStatus("Virtual Column on " + (obj != null ? obj.getName() : ""));
            });
            MenuItem vFkItem = new MenuItem("Virtual Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#c77dbb", 11));
            vFkItem.setOnAction(e -> {
                if (mainWindow != null) mainWindow.setStatus("Virtual Foreign Key on " + (obj != null ? obj.getName() : ""));
            });
            MenuItem vvItem = new MenuItem("Virtual View", Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 12));
            vvItem.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));

            newMenu.getItems().addAll(new SeparatorMenuItem(), vColItem, vFkItem, vvItem);
            if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL || profile.getType() == ConnectionProfile.DatabaseType.MARIADB) {
                MenuItem schemaItem = new MenuItem("Schema", Icons.of(FontAwesomeSolid.PROJECT_DIAGRAM, "#3592c4", 12));
                schemaItem.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));
                MenuItem userItem = new MenuItem("User", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
                userItem.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
                MenuItem roleItem = new MenuItem("Role", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
                roleItem.setOnAction(e -> CreateRoleDialog.show(mainWindow, profile));
                newMenu.getItems().addAll(new SeparatorMenuItem(), schemaItem, userItem, roleItem);
            } else {
                MenuItem userItem = new MenuItem("User", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
                userItem.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
                newMenu.getItems().addAll(new SeparatorMenuItem(), userItem);
            }
            appendCommonNewDataSourceItems(newMenu, profile, false);
            return newMenu;
        }

        if (profile.getType() == ConnectionProfile.DatabaseType.ORACLE && isVirtualViewsFolder) {
            MenuItem vvItem = new MenuItem("Virtual View", Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 12));
            vvItem.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));
            MenuItem userItem = new MenuItem("User", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
            userItem.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
            newMenu.getItems().addAll(new SeparatorMenuItem(), vvItem, userItem);
            appendCommonNewDataSourceItems(newMenu, profile, false);
            return newMenu;
        }

        if (profile.getType() == ConnectionProfile.DatabaseType.ORACLE
                && (kind == Kind.TABLESPACE || kind == Kind.DATA_FILE || kind == Kind.USER)) {
            MenuItem userItem = new MenuItem("User", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
            userItem.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
            MenuItem vvItem = new MenuItem("Virtual View", Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 12));
            vvItem.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));
            newMenu.getItems().addAll(new SeparatorMenuItem(), userItem, new SeparatorMenuItem(), vvItem);
            appendCommonNewDataSourceItems(newMenu, profile, false);
            return newMenu;
        }

        boolean isTable = (kind == Kind.TABLE || kind == Kind.VIEW || kind == Kind.PARTITION);
        boolean isTableSubFolder = (kind == Kind.COLUMNS_FOLDER
                || kind == Kind.KEYS_FOLDER
                || kind == Kind.FOREIGN_KEYS_FOLDER
                || kind == Kind.INDEXES_FOLDER
                || kind == Kind.PARTITIONS_FOLDER);
        boolean isTableSubItem = (kind == Kind.COLUMN
                || kind == Kind.KEY
                || kind == Kind.FOREIGN_KEY
                || kind == Kind.INDEX);
        boolean isTableOrTableSubFolder = isTable || isTableSubFolder;
        boolean isTableOrTableSub = isTable || isTableSubFolder || isTableSubItem;
        ConnectionProfile.DatabaseType type = profile.getType();

        switch (type) {
            case MYSQL, MARIADB -> {
                MenuItem tableItem = new MenuItem("Table", Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12));
                tableItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE TABLE `" + (targetCatalog != null ? targetCatalog : "mydb") + "`.`new_table` (\n    `id` INT PRIMARY KEY AUTO_INCREMENT\n);\n");
                });
                MenuItem viewItem = new MenuItem("View", Icons.of(FontAwesomeSolid.EYE, "#57965c", 12));
                viewItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE VIEW `" + (targetCatalog != null ? targetCatalog : "mydb") + "`.`new_view` AS\nSELECT 1;\n");
                });
                MenuItem schemaItem = new MenuItem("Schema", Icons.of(FontAwesomeSolid.PROJECT_DIAGRAM, "#3592c4", 12));
                schemaItem.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));
                MenuItem userItem = new MenuItem("User", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
                userItem.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
                MenuItem roleItem = new MenuItem("Role", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
                roleItem.setOnAction(e -> CreateRoleDialog.show(mainWindow, profile));

                if (kind == Kind.COLLATION) {
                    MenuItem vvItem = new MenuItem("Virtual View", Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 12));
                    vvItem.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));
                    newMenu.getItems().addAll(new SeparatorMenuItem(), schemaItem, userItem, roleItem);
                    newMenu.getItems().addAll(new SeparatorMenuItem(), vvItem);
                    appendCommonNewDataSourceItems(newMenu, profile, false);
                    return newMenu;
                }

                if (kind == Kind.USER) {
                    MenuItem vvItem = new MenuItem("Virtual View", Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 12));
                    vvItem.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));
                    newMenu.getItems().addAll(new SeparatorMenuItem(), userItem, roleItem, schemaItem);
                    newMenu.getItems().addAll(new SeparatorMenuItem(), vvItem);
                    appendCommonNewDataSourceItems(newMenu, profile, false);
                    return newMenu;
                }


                if (isTableOrTableSub) {
                    DbObject parentTable = resolveParentTableFor(item, obj);
                    String tblName = parentTable != null ? parentTable.getName()
                            : (obj.getTableName() != null ? obj.getTableName() : obj.getName());
                    MenuItem colItem = new MenuItem("Column", Icons.of(FontAwesomeSolid.COLUMNS, "#868a91", 11));
                    colItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE `" + (targetCatalog != null ? targetCatalog : "mydb") + "`.`" + tblName + "` ADD COLUMN `new_column` VARCHAR(255);\n");
                    });
                    MenuItem uqItem = new MenuItem("Unique Key", Icons.of(FontAwesomeSolid.KEY, "#e0a44c", 11));
                    uqItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE `" + (targetCatalog != null ? targetCatalog : "mydb") + "`.`" + tblName + "` ADD CONSTRAINT `uq_" + tblName + "_new` UNIQUE (`column_name`);\n");
                    });
                    MenuItem fkItem = new MenuItem("Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#4a88c7", 11));
                    fkItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE `" + (targetCatalog != null ? targetCatalog : "mydb") + "`.`" + tblName + "` ADD CONSTRAINT `fk_" + tblName + "_ref` FOREIGN KEY (`column_name`) REFERENCES `target_table` (`target_col`);\n");
                    });
                    MenuItem idxItem = new MenuItem("Index", Icons.of(FontAwesomeSolid.BOLT, "#e0a44c", 11));
                    idxItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "CREATE INDEX `idx_" + tblName + "_col` ON `" + (targetCatalog != null ? targetCatalog : "mydb") + "`.`" + tblName + "` (`column_name`);\n");
                    });
                    MenuItem chkItem = new MenuItem("Check", Icons.of(FontAwesomeSolid.CHECK, "#868a91", 11));
                    chkItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE `" + (targetCatalog != null ? targetCatalog : "mydb") + "`.`" + tblName + "` ADD CONSTRAINT `chk_" + tblName + "_new` CHECK (1 = 1);\n");
                    });
                    MenuItem triggerItem = new MenuItem("Trigger", Icons.of(FontAwesomeSolid.BOLT, "#868a91", 11));
                    triggerItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "CREATE TRIGGER `trg_" + tblName + "_new` BEFORE INSERT ON `" + (targetCatalog != null ? targetCatalog : "mydb") + "`.`" + tblName + "`\nFOR EACH ROW\nBEGIN\nEND;\n");
                    });

                    MenuItem vColItem = new MenuItem("Virtual Column", Icons.of(FontAwesomeSolid.COLUMNS, "#c77dbb", 11));
                    vColItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.setStatus("Virtual Column on " + tblName);
                    });
                    MenuItem vFkItem = new MenuItem("Virtual Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#c77dbb", 11));
                    vFkItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.setStatus("Virtual Foreign Key on " + tblName);
                    });
                    MenuItem vvItem = new MenuItem("Virtual View", Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 12));
                    vvItem.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));

                    newMenu.getItems().add(new SeparatorMenuItem());
                    if (kind == Kind.KEYS_FOLDER || kind == Kind.KEY) {
                        newMenu.getItems().addAll(uqItem, colItem, fkItem, idxItem, chkItem, triggerItem);
                    } else if (kind == Kind.FOREIGN_KEYS_FOLDER || kind == Kind.FOREIGN_KEY) {
                        newMenu.getItems().addAll(fkItem, colItem, uqItem, idxItem, chkItem, triggerItem);
                    } else if (kind == Kind.INDEXES_FOLDER || kind == Kind.INDEX) {
                        newMenu.getItems().addAll(idxItem, colItem, uqItem, fkItem, chkItem, triggerItem);
                    } else {
                        newMenu.getItems().addAll(colItem, uqItem, fkItem, idxItem, chkItem, triggerItem);
                    }
                    newMenu.getItems().addAll(tableItem, viewItem, schemaItem, userItem, roleItem);
                    newMenu.getItems().addAll(new SeparatorMenuItem(), vColItem, vFkItem, vvItem);
                    appendCommonNewDataSourceItems(newMenu, profile, false);
                    return newMenu;
                } else {
                    newMenu.getItems().addAll(new SeparatorMenuItem(), tableItem, viewItem, schemaItem, userItem, roleItem);
                }
            }
            case MONGODB -> {
                if (kind == Kind.COLLECTION
                        || kind == Kind.FIELDS_FOLDER
                        || kind == Kind.INDEXES_FOLDER
                        || kind == Kind.FIELD
                        || kind == Kind.INDEX) {
                    MenuItem vCol = new MenuItem("Virtual Column", Icons.of(FontAwesomeSolid.COLUMNS, "#c77dbb", 11));
                    vCol.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.setStatus("Virtual Column on " + (obj != null ? obj.getName() : ""));
                    });
                    MenuItem vFk = new MenuItem("Virtual Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#c77dbb", 11));
                    vFk.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.setStatus("Virtual Foreign Key on " + (obj != null ? obj.getName() : ""));
                    });
                    MenuItem vv = new MenuItem("Virtual View", Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 12));
                    vv.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));
                    newMenu.getItems().addAll(new SeparatorMenuItem(), vCol, vFk, vv);
                    appendCommonNewDataSourceItems(newMenu, profile, false);
                    return newMenu;
                }
            }
            case SQLITE -> {
                MenuItem tableItem = new MenuItem("Table", Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12));
                tableItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, null,
                            "CREATE TABLE new_table (\n    id INTEGER PRIMARY KEY AUTOINCREMENT\n);\n");
                });
                MenuItem viewItem = new MenuItem("View", Icons.of(FontAwesomeSolid.EYE, "#57965c", 12));
                viewItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, null,
                            "CREATE VIEW new_view AS\nSELECT 1;\n");
                });
                if (isTable) {
                    DbObject parentTable = resolveParentTableFor(item, obj);
                    String tblName = parentTable != null ? parentTable.getName()
                            : (obj.getTableName() != null ? obj.getTableName() : obj.getName());
                    MenuItem colItem = new MenuItem("Column", Icons.of(FontAwesomeSolid.COLUMNS, "#868a91", 11));
                    colItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, null,
                                "ALTER TABLE \"" + tblName + "\" ADD COLUMN new_column TEXT;\n");
                    });
                    MenuItem idxItem = new MenuItem("Index", Icons.of(FontAwesomeSolid.BOLT, "#e0a44c", 11));
                    idxItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, null,
                                "CREATE INDEX idx_" + tblName + "_col ON \"" + tblName + "\" (column_name);\n");
                    });
                    newMenu.getItems().addAll(new SeparatorMenuItem(), colItem, idxItem);
                }
                newMenu.getItems().addAll(new SeparatorMenuItem(), tableItem, viewItem);
            }
            case STRATOSDB -> {
                MenuItem tableItem = new MenuItem("Table", Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12));
                tableItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE TABLE new_table (\n    id INT PRIMARY KEY AUTO_INCREMENT\n);\n");
                });
                MenuItem viewItem = new MenuItem("View", Icons.of(FontAwesomeSolid.EYE, "#57965c", 12));
                viewItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE VIEW new_view AS\nSELECT 1;\n");
                });
                if (isTable) {
                    DbObject parentTable = resolveParentTableFor(item, obj);
                    String tblName = parentTable != null ? parentTable.getName()
                            : (obj.getTableName() != null ? obj.getTableName() : obj.getName());
                    MenuItem colItem = new MenuItem("Column", Icons.of(FontAwesomeSolid.COLUMNS, "#868a91", 11));
                    colItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE \"" + tblName + "\" ADD COLUMN new_column VARCHAR(255);\n");
                    });
                    MenuItem idxItem = new MenuItem("Index", Icons.of(FontAwesomeSolid.BOLT, "#e0a44c", 11));
                    idxItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "CREATE INDEX idx_" + tblName + "_col ON \"" + tblName + "\" (column_name);\n");
                    });
                    newMenu.getItems().addAll(new SeparatorMenuItem(), colItem, idxItem);
                }
                newMenu.getItems().addAll(new SeparatorMenuItem(), tableItem, viewItem);
            }
            case SQLSERVER -> {
                MenuItem tableItem = new MenuItem("Table", Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12));
                tableItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE TABLE [" + (schema != null ? schema : "dbo") + "].[new_table] (\n    [id] INT IDENTITY(1,1) PRIMARY KEY\n);\n");
                });
                MenuItem viewItem = new MenuItem("View", Icons.of(FontAwesomeSolid.EYE, "#57965c", 12));
                viewItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE VIEW [" + (schema != null ? schema : "dbo") + "].[new_view] AS\nSELECT 1 AS col;\n");
                });
                MenuItem seqItem = new MenuItem("Sequence", Icons.of(FontAwesomeSolid.SORT_NUMERIC_DOWN, "#e0a44c", 12));
                seqItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE SEQUENCE [" + (schema != null ? schema : "dbo") + "].[new_seq] AS INT START WITH 1 INCREMENT BY 1;\n");
                });
                MenuItem schemaItem = new MenuItem("Schema", Icons.of(FontAwesomeSolid.PROJECT_DIAGRAM, "#3592c4", 12));
                schemaItem.setOnAction(e -> promptCreateSqlServerSchema(profile, targetCatalog));
                MenuItem dbPrincipal = new MenuItem("Database Principal", Icons.of(FontAwesomeSolid.USER, "#3592c4", 11));
                dbPrincipal.setOnAction(e -> promptCreateSqlServerDbPrincipal(profile, targetCatalog));
                MenuItem dbItem = new MenuItem("Database", Icons.of(FontAwesomeSolid.DATABASE, "#3592c4", 12));
                dbItem.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));
                MenuItem serverPrincipal = new MenuItem("Server Principal", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
                serverPrincipal.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));

                if (kind == Kind.FILEGROUP) {
                    MenuItem vvItem = new MenuItem("Virtual View", Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 12));
                    vvItem.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));
                    newMenu.getItems().addAll(new SeparatorMenuItem(), schemaItem, dbPrincipal, dbItem, serverPrincipal);
                    newMenu.getItems().addAll(new SeparatorMenuItem(), vvItem);
                    appendCommonNewDataSourceItems(newMenu, profile, false);
                    return newMenu;
                }

                if (isTable) {
                    DbObject parentTable = resolveParentTableFor(item, obj);
                    String tblName = parentTable != null ? parentTable.getName()
                            : (obj.getTableName() != null ? obj.getTableName() : obj.getName());
                    MenuItem colItem = new MenuItem("Column", Icons.of(FontAwesomeSolid.COLUMNS, "#868a91", 11));
                    colItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE [" + (schema != null ? schema : "dbo") + "].[" + tblName + "] ADD [new_column] NVARCHAR(255);\n");
                    });
                    MenuItem uqItem = new MenuItem("Unique Key", Icons.of(FontAwesomeSolid.KEY, "#e0a44c", 11));
                    uqItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE [" + (schema != null ? schema : "dbo") + "].[" + tblName + "] ADD CONSTRAINT [uq_" + tblName + "_new] UNIQUE ([column_name]);\n");
                    });
                    MenuItem fkItem = new MenuItem("Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#4a88c7", 11));
                    fkItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE [" + (schema != null ? schema : "dbo") + "].[" + tblName + "] ADD CONSTRAINT [fk_" + tblName + "_ref] FOREIGN KEY ([column_name]) REFERENCES [target_table] ([target_col]);\n");
                    });
                    MenuItem idxItem = new MenuItem("Index", Icons.of(FontAwesomeSolid.BOLT, "#e0a44c", 11));
                    idxItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "CREATE INDEX [idx_" + tblName + "_col] ON [" + (schema != null ? schema : "dbo") + "].[" + tblName + "] ([column_name]);\n");
                    });
                    newMenu.getItems().addAll(new SeparatorMenuItem(), colItem, uqItem, fkItem, idxItem);
                }
                newMenu.getItems().addAll(new SeparatorMenuItem(), tableItem, viewItem, seqItem, schemaItem, dbPrincipal, dbItem, serverPrincipal);
            }
            case ORACLE -> {
                MenuItem tableItem = new MenuItem("Table", Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12));
                tableItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE TABLE \"" + (schema != null ? schema : "HELLO") + "\".\"NEW_TABLE\" (\n    \"ID\" NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY\n);\n");
                });
                MenuItem matViewItem = new MenuItem("Materialized View", Icons.of(FontAwesomeSolid.DATABASE, "#57965c", 12));
                matViewItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE MATERIALIZED VIEW \"" + (schema != null ? schema : "HELLO") + "\".\"NEW_MV\" AS\nSELECT 1 AS ID FROM DUAL;\n");
                });
                MenuItem viewItem = new MenuItem("View", Icons.of(FontAwesomeSolid.EYE, "#57965c", 12));
                viewItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE VIEW \"" + (schema != null ? schema : "HELLO") + "\".\"NEW_VIEW\" AS\nSELECT 1 AS ID FROM DUAL;\n");
                });
                MenuItem seqItem = new MenuItem("Sequence", Icons.of(FontAwesomeSolid.SORT_NUMERIC_DOWN, "#e0a44c", 12));
                seqItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                            "CREATE SEQUENCE \"" + (schema != null ? schema : "HELLO") + "\".\"NEW_SEQ\" START WITH 1 INCREMENT BY 1;\n");
                });
                MenuItem userItem = new MenuItem("User", Icons.of(FontAwesomeSolid.USER, "#868a91", 11));
                userItem.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));

                if (isTableOrTableSub) {
                    DbObject parentTable = resolveParentTableFor(item, obj);
                    String tblName = parentTable != null ? parentTable.getName()
                            : (obj.getTableName() != null ? obj.getTableName() : obj.getName());
                    MenuItem colItem = new MenuItem("Column", Icons.of(FontAwesomeSolid.COLUMNS, "#868a91", 11));
                    colItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE \"" + (schema != null ? schema : "HELLO") + "\".\"" + tblName + "\" ADD \"NEW_COLUMN\" VARCHAR2(255);\n");
                    });
                    MenuItem uqItem = new MenuItem("Unique Key", Icons.of(FontAwesomeSolid.KEY, "#e0a44c", 11));
                    uqItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE \"" + (schema != null ? schema : "HELLO") + "\".\"" + tblName + "\" ADD CONSTRAINT \"UQ_" + tblName + "_NEW\" UNIQUE (\"COLUMN_NAME\");\n");
                    });
                    MenuItem fkItem = new MenuItem("Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#4a88c7", 11));
                    fkItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE \"" + (schema != null ? schema : "HELLO") + "\".\"" + tblName + "\" ADD CONSTRAINT \"FK_" + tblName + "_REF\" FOREIGN KEY (\"COLUMN_NAME\") REFERENCES \"TARGET_TABLE\" (\"TARGET_COL\");\n");
                    });
                    MenuItem idxItem = new MenuItem("Index", Icons.of(FontAwesomeSolid.BOLT, "#e0a44c", 11));
                    idxItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "CREATE INDEX \"IDX_" + tblName + "_COL\" ON \"" + (schema != null ? schema : "HELLO") + "\".\"" + tblName + "\" (\"COLUMN_NAME\");\n");
                    });
                    MenuItem chkItem = new MenuItem("Check", Icons.of(FontAwesomeSolid.CHECK, "#868a91", 11));
                    chkItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "ALTER TABLE \"" + (schema != null ? schema : "HELLO") + "\".\"" + tblName + "\" ADD CONSTRAINT \"CHK_" + tblName + "_NEW\" CHECK (1 = 1);\n");
                    });

                    MenuItem vColItem = new MenuItem("Virtual Column", Icons.of(FontAwesomeSolid.COLUMNS, "#c77dbb", 11));
                    vColItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.setStatus("Virtual Column on " + tblName);
                    });
                    MenuItem vFkItem = new MenuItem("Virtual Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#c77dbb", 11));
                    vFkItem.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.setStatus("Virtual Foreign Key on " + tblName);
                    });

                    if (kind == Kind.KEYS_FOLDER) {
                        newMenu.getItems().addAll(new SeparatorMenuItem(), uqItem, colItem, fkItem, idxItem, chkItem);
                    } else if (kind == Kind.FOREIGN_KEYS_FOLDER) {
                        newMenu.getItems().addAll(new SeparatorMenuItem(), fkItem, colItem, uqItem, idxItem, chkItem);
                    } else if (kind == Kind.INDEXES_FOLDER) {
                        newMenu.getItems().addAll(new SeparatorMenuItem(), idxItem, colItem, uqItem, fkItem, chkItem);
                    } else {
                        newMenu.getItems().addAll(new SeparatorMenuItem(), colItem, uqItem, fkItem, idxItem, chkItem);
                    }
                    newMenu.getItems().addAll(new SeparatorMenuItem(), tableItem, matViewItem, viewItem, seqItem, userItem);
                    newMenu.getItems().addAll(new SeparatorMenuItem(), vColItem, vFkItem);
                } else if (kind == Kind.VIEWS_FOLDER) {
                    newMenu.getItems().addAll(new SeparatorMenuItem(), viewItem, tableItem, matViewItem, seqItem, userItem);
                } else if (kind == Kind.SEQUENCES_FOLDER) {
                    newMenu.getItems().addAll(new SeparatorMenuItem(), seqItem, tableItem, matViewItem, viewItem, userItem);
                } else {
                    newMenu.getItems().addAll(new SeparatorMenuItem(), tableItem, matViewItem, viewItem, seqItem, userItem);
                }
            }
            default -> {}
        }

        MenuItem virtualView = new MenuItem("Virtual View", Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 12));
        virtualView.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));
        if (type == ConnectionProfile.DatabaseType.ORACLE && isTableOrTableSub) {
            newMenu.getItems().add(virtualView);
        } else {
            newMenu.getItems().addAll(new SeparatorMenuItem(), virtualView);
        }

        appendCommonNewDataSourceItems(newMenu, profile, false);
        return newMenu;
    }

    ContextMenu buildEngineMenu(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        if (profile == null || obj == null) return null;

        ConnectionProfile.DatabaseType type = profile.getType();
        DbObject.Kind kind = obj.getKind();
        boolean isServerObjectsFolder = (kind == Kind.SERVER_OBJECTS
                || kind == Kind.TABLESPACES_FOLDER
                || kind == Kind.USERS_FOLDER
                || kind == Kind.COLLATIONS_FOLDER
                || kind == Kind.DATABASE_OBJECTS
                || kind == Kind.ASSEMBLIES_FOLDER
                || kind == Kind.FILEGROUPS_FOLDER
                || kind == Kind.DATABASE_PRINCIPALS_FOLDER
                || kind == Kind.LINKED_SERVERS_FOLDER
                || kind == Kind.SERVER_PRINCIPALS_FOLDER
                || kind == Kind.ROLES_FOLDER);
        boolean isVirtualViewsFolder = (kind == Kind.VIRTUAL_VIEWS_FOLDER);
        boolean isVirtualViewItem = (kind == Kind.VIRTUAL_VIEW);
        boolean isCollationItem = (kind == Kind.COLLATION);
        boolean isUserItem = (kind == Kind.USER);
        boolean isRoutineItem = (kind == Kind.PROCEDURE || kind == Kind.FUNCTION);
        boolean isFilegroupItem = (kind == Kind.FILEGROUP);
        boolean isSqlServerServerObjectItem = (type == ConnectionProfile.DatabaseType.SQLSERVER
                && (kind == Kind.SERVER_PRINCIPAL || kind == Kind.LINKED_SERVER
                    || kind == Kind.ASSEMBLY || kind == Kind.DATABASE_PRINCIPAL || kind == Kind.FILEGROUP));

        boolean isMongoFolder = (type == ConnectionProfile.DatabaseType.MONGODB
                && (kind == Kind.FIELDS_FOLDER || kind == Kind.INDEXES_FOLDER));
        boolean isMongoSubItem = (type == ConnectionProfile.DatabaseType.MONGODB
                && (kind == Kind.FIELD || kind == Kind.INDEX));

        boolean isOracleServerObjectItem = (type == ConnectionProfile.DatabaseType.ORACLE
                && (kind == Kind.TABLESPACE || kind == Kind.DATA_FILE || kind == Kind.USER));
        boolean isTableSubFolder = (kind == Kind.COLUMNS_FOLDER
                || kind == Kind.KEYS_FOLDER
                || kind == Kind.FOREIGN_KEYS_FOLDER
                || kind == Kind.INDEXES_FOLDER
                || kind == Kind.PARTITIONS_FOLDER);
        boolean isTableSubItem = (kind == Kind.COLUMN
                || kind == Kind.KEY
                || kind == Kind.FOREIGN_KEY
                || kind == Kind.INDEX);

        boolean isCategoryFolder = (kind == Kind.TABLES_FOLDER
                || kind == Kind.COLLECTIONS_FOLDER
                || kind == Kind.VIEWS_FOLDER
                || kind == Kind.SEQUENCES_FOLDER
                || kind == Kind.PROCEDURES_FOLDER
                || kind == Kind.FUNCTIONS_FOLDER
                || isServerObjectsFolder
                || isVirtualViewsFolder
                || isMongoFolder
                || isTableSubFolder);
        boolean isTable = (kind == Kind.TABLE || kind == Kind.VIEW || kind == Kind.PARTITION);
        boolean isCollection = (kind == Kind.COLLECTION);
        boolean isSequence = (kind == Kind.SEQUENCE);

        if (!isCategoryFolder && !isTable && !isCollection && !isSequence && !isRoutineItem
                && !isMongoSubItem && !isOracleServerObjectItem && !isTableSubItem
                && !isVirtualViewItem && !isCollationItem && !isUserItem
                && !isSqlServerServerObjectItem) {
            return null;
        }

        ContextMenu menu = new ContextMenu();
        String targetCatalog = resolveCatalogFor(item, obj, profile);

        // 1. + New >
        Menu newMenu = buildEngineNewMenu(item, obj, profile);
        menu.getItems().add(newMenu);

        // 2. Modify Table... (Modify View / Modify Collection / Modify Sequence / Modify Virtual View / Modify User)
        if (type == ConnectionProfile.DatabaseType.MONGODB) {
            if (kind == Kind.COLLECTIONS_FOLDER) {
                MenuItem modifyTable = new MenuItem("Modify Table…");
                modifyTable.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
                modifyTable.setDisable(true);
                menu.getItems().add(modifyTable);
            } else if (isCollection) {
                MenuItem modifyTable = new MenuItem("Modify Table…");
                modifyTable.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
                modifyTable.setOnAction(e -> ModifyCollectionDialog.show(mainWindow, profile, obj));
                menu.getItems().add(modifyTable);
            }
        } else if (type == ConnectionProfile.DatabaseType.ORACLE && kind == Kind.VIEWS_FOLDER) {
            MenuItem modifyView = new MenuItem("Modify View…");
            modifyView.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
            modifyView.setDisable(true);
            menu.getItems().add(modifyView);
        } else if (type == ConnectionProfile.DatabaseType.ORACLE && isVirtualViewsFolder) {
            MenuItem modifyVv = new MenuItem("Modify Virtual View…");
            modifyVv.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
            modifyVv.setDisable(true);
            menu.getItems().add(modifyVv);
        } else if (kind == Kind.VIRTUAL_VIEW) {
            MenuItem modifyVv = new MenuItem("Modify Virtual View…");
            modifyVv.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
            modifyVv.setOnAction(e -> {
                if (mainWindow != null) mainWindow.setStatus("Modify Virtual View on " + obj.getName());
            });
            menu.getItems().add(modifyVv);
        } else if ((type == ConnectionProfile.DatabaseType.ORACLE || type == ConnectionProfile.DatabaseType.MYSQL || type == ConnectionProfile.DatabaseType.MARIADB) && isUserItem) {
            MenuItem modifyUser = new MenuItem("Modify User…");
            modifyUser.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
            modifyUser.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
            menu.getItems().add(modifyUser);
        } else if ((type == ConnectionProfile.DatabaseType.MYSQL || type == ConnectionProfile.DatabaseType.MARIADB || type == ConnectionProfile.DatabaseType.STRATOSDB)
                && (kind == Kind.KEYS_FOLDER || kind == Kind.KEY)) {
            MenuItem modifyKey = new MenuItem("Modify Key…");
            modifyKey.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
            modifyKey.setOnAction(e -> {
                if (mainWindow != null) mainWindow.setStatus("Modify Key on " + obj.getName());
            });
            menu.getItems().add(modifyKey);
        } else if ((type == ConnectionProfile.DatabaseType.MYSQL || type == ConnectionProfile.DatabaseType.MARIADB || type == ConnectionProfile.DatabaseType.ORACLE || type == ConnectionProfile.DatabaseType.STRATOSDB)
                && kind == Kind.COLUMN) {
            MenuItem modifyCol = new MenuItem("Modify Column…");
            modifyCol.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
            modifyCol.setOnAction(e -> {
                if (mainWindow != null) mainWindow.setStatus("Modify Column on " + obj.getName());
            });
            menu.getItems().add(modifyCol);
        } else if (isTable) {
            String modifyTitle = (kind == Kind.VIEW) ? "Modify View…" : "Modify Table…";
            MenuItem modifyTable = new MenuItem(modifyTitle);
            modifyTable.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
            modifyTable.setOnAction(e -> ModifyTableDialog.show(mainWindow, profile, obj));
            menu.getItems().add(modifyTable);
        } else if (isSequence) {
            MenuItem modifySeq = new MenuItem("Modify Sequence…");
            modifySeq.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
            modifySeq.setOnAction(e -> ModifySequenceDialog.show(mainWindow, profile, obj));
            menu.getItems().add(modifySeq);
        }

        // 3. Copy/Paste >
        Menu copyPasteMenu = new Menu("Copy/Paste");
        MenuItem copyName = new MenuItem("Copy");
        copyName.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyName.setOnAction(e -> {
            copyToClipboard(obj.getName());
            if (mainWindow != null) mainWindow.setStatus("Copied to clipboard: " + obj.getName());
        });
        MenuItem copyPath = new MenuItem("Copy Path/Reference…");
        copyPath.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
        copyPath.setOnAction(e -> {
            String ref = obj.qualifiedName();
            copyToClipboard(ref);
            if (mainWindow != null) mainWindow.setStatus("Copied reference: " + ref);
        });
        copyPasteMenu.getItems().addAll(copyName, copyPath);
        if (isCategoryFolder) {
            copyPasteMenu.setDisable(true);
        }
        menu.getItems().add(copyPasteMenu);

        if (isCategoryFolder) {
            // 4. Refresh (Ctrl+F5)
            MenuItem refresh = new MenuItem("Refresh");
            refresh.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#868a91", 12));
            refresh.setAccelerator(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN));
            refresh.setOnAction(e -> {
                if (item != null) refreshNode(item, obj, obj.getName());
                else reload();
            });
            menu.getItems().add(refresh);

            menu.getItems().add(new SeparatorMenuItem());

            // 5. Rename… (Shift+F6, disabled)
            MenuItem rename = new MenuItem("Rename…");
            rename.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN));
            rename.setDisable(true);
            menu.getItems().add(rename);

            // 6. Open the List (F4)
            MenuItem openList = new MenuItem("Open the List");
            openList.setAccelerator(new KeyCodeCombination(KeyCode.F4));
            openList.setOnAction(e -> {
                if (item != null) item.setExpanded(!item.isExpanded());
            });
            menu.getItems().add(openList);

            // 7. Quick Documentation (Ctrl+Q, disabled)
            MenuItem quickDoc = new MenuItem("Quick Documentation");
            quickDoc.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
            quickDoc.setDisable(true);
            menu.getItems().add(quickDoc);

            // 8. Compare Schema Structure (Ctrl+D, disabled)
            MenuItem compareStructure = new MenuItem("Compare Schema Structure");
            compareStructure.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
            compareStructure.setDisable(true);
            menu.getItems().add(compareStructure);

            menu.getItems().add(new SeparatorMenuItem());
        } else {
            // Table, Collection, Sequence, Routine, Mongo Sub-item, or Oracle Server Object item
            if (!isRoutineItem && !isOracleServerObjectItem && !isCollationItem && !isUserItem && !isSqlServerServerObjectItem) {
                MenuItem editData = new MenuItem("Edit Data");
                editData.setGraphic(Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12));
                editData.setAccelerator(new KeyCodeCombination(KeyCode.F4));
                editData.setOnAction(e -> {
                    if (isCollection || isMongoSubItem) {
                        DbObject targetColl = resolveParentTableFor(item, obj);
                        if (mainWindow != null) mainWindow.openMongoTab(profile, targetColl != null ? targetColl : obj);
                    } else if (isTableSubItem) {
                        DbObject targetTbl = resolveParentTableFor(item, obj);
                        if (mainWindow != null) mainWindow.openDataTab(profile, targetTbl != null ? targetTbl : obj);
                    } else {
                        if (mainWindow != null) mainWindow.openDataTab(profile, obj);
                    }
                });
                menu.getItems().add(editData);
            }

            MenuItem refresh = new MenuItem("Refresh");
            refresh.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#868a91", 12));
            refresh.setAccelerator(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN));
            refresh.setOnAction(e -> {
                if (item != null) refreshNode(item, obj, obj.getName());
                else reload();
            });
            menu.getItems().addAll(refresh, new SeparatorMenuItem());

            MenuItem rename = new MenuItem("Rename…");
            rename.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN));
            if (isMongoSubItem || isOracleServerObjectItem || isTableSubItem || isVirtualViewItem || isCollationItem || isUserItem || isSqlServerServerObjectItem) {
                rename.setDisable(true);
            } else if (isRoutineItem) {
                rename.setOnAction(e -> renameRoutine(profile, obj));
            } else if (isCollection) {
                rename.setOnAction(e -> promptRenameMongoCollection(profile, obj));
            } else {
                rename.setOnAction(e -> promptRenameTable(profile, obj));
            }
            menu.getItems().addAll(rename, new SeparatorMenuItem());

            MenuItem quickDoc = new MenuItem("Quick Documentation");
            quickDoc.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
            quickDoc.setOnAction(e -> showQuickDocumentation(profile, obj));
            menu.getItems().add(quickDoc);

            String compareTitle = isVirtualViewItem ? "Compare Schema Structure" : "Compare Schema Structure With…";
            MenuItem compareStructure = new MenuItem(compareTitle);
            compareStructure.setGraphic(Icons.of(FontAwesomeSolid.EXCHANGE_ALT, "#868a91", 12));
            compareStructure.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
            compareStructure.setDisable(true);
            menu.getItems().add(compareStructure);

            Menu navMenu = new Menu("Navigation");
            MenuItem goToDdl = new MenuItem("Go to DDL");
            goToDdl.setOnAction(e -> {
                if (mainWindow != null) mainWindow.openDdlTab(profile, obj);
            });
            MenuItem jumpConsole = new MenuItem("Jump to Query Console");
            jumpConsole.setAccelerator(new KeyCodeCombination(KeyCode.F10, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
            jumpConsole.setOnAction(e -> jumpToQueryConsole());
            MenuItem selectInTree = new MenuItem("Select in Database Explorer");
            selectInTree.setAccelerator(new KeyCodeCombination(KeyCode.F1, KeyCombination.ALT_DOWN));
            selectInTree.setOnAction(e -> {
                if (item != null) tree.getSelectionModel().select(item);
            });
            navMenu.getItems().addAll(goToDdl, jumpConsole, selectInTree);
            menu.getItems().add(navMenu);

            MenuItem findUsages = new MenuItem("Find Usages");
            findUsages.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT7, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
            findUsages.setDisable(true);
            menu.getItems().addAll(findUsages, new SeparatorMenuItem());
        }

        // Introspection Level > (MySQL, MariaDB, SQL Server, Oracle, StratosDB)
        boolean hasIntrospectionLevel = (type == ConnectionProfile.DatabaseType.MYSQL
                || type == ConnectionProfile.DatabaseType.MARIADB
                || type == ConnectionProfile.DatabaseType.SQLSERVER
                || type == ConnectionProfile.DatabaseType.ORACLE
                || type == ConnectionProfile.DatabaseType.STRATOSDB);
        if (hasIntrospectionLevel) {
            Menu introMenu = new Menu("Introspection Level");
            ToggleGroup introGroup = new ToggleGroup();
            RadioMenuItem introAuto = new RadioMenuItem("Auto");
            introAuto.setToggleGroup(introGroup);
            introAuto.setSelected(true);
            RadioMenuItem introHigh = new RadioMenuItem("High");
            introHigh.setToggleGroup(introGroup);
            RadioMenuItem introMed = new RadioMenuItem("Medium");
            introMed.setToggleGroup(introGroup);
            RadioMenuItem introLow = new RadioMenuItem("Low");
            introLow.setToggleGroup(introGroup);
            introMenu.getItems().addAll(introAuto, introHigh, introMed, introLow);
            if (!isCategoryFolder) {
                menu.getItems().addAll(introMenu, new SeparatorMenuItem());
            } else {
                menu.getItems().add(introMenu);
            }
        }

        if (isRoutineItem) {
            String runLabel = (kind == Kind.FUNCTION) ? "Run Function…" : "Run Procedure…";
            MenuItem runRoutine = new MenuItem(runLabel, Icons.of(FontAwesomeSolid.PLAY, "#57965c", 12));
            runRoutine.setOnAction(e -> {
                if (mainWindow != null) mainWindow.openRoutineRunTab(profile, obj);
            });
            menu.getItems().addAll(runRoutine, new SeparatorMenuItem());
        }

        // SQL Scripts >
        Menu sqlScripts = new Menu("SQL Scripts");
        MenuItem openConsole = new MenuItem("Open Query Console");
        openConsole.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog, null);
        });
        MenuItem runSqlScript = new MenuItem("Run SQL Script…");
        runSqlScript.setOnAction(e -> promptRunSqlScript(profile));
        MenuItem genDdl = new MenuItem("Generate DDL to Query Console");
        genDdl.setOnAction(e -> {
            AppExecutor.run(() -> {
                try {
                    String ddl = DdlGeneratorService.generateDdl(profile, obj);
                    Platform.runLater(() -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog, ddl);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "-- Could not generate DDL: " + ex.getMessage());
                    });
                }
            });
        });
        sqlScripts.getItems().addAll(openConsole, runSqlScript, genDdl);
        if (isTable) {
            MenuItem select = new MenuItem("New Query: SELECT *");
            select.setOnAction(e -> mainWindow.openQueryTab(profile, obj.getCatalog(),
                    selectTopRowsSql(profile.getType(), obj)));
            MenuItem count = new MenuItem("New Query: COUNT(*)");
            count.setOnAction(e -> mainWindow.openQueryTab(profile, obj.getCatalog(),
                    "SELECT COUNT(*) FROM " + obj.qualifiedName() + ";"));
            sqlScripts.getItems().addAll(new SeparatorMenuItem(), select, count);
        }
        menu.getItems().add(sqlScripts);

        // Object Actions >
        boolean isMongoSubNode = (type == ConnectionProfile.DatabaseType.MONGODB
                && (isMongoFolder || isMongoSubItem));
        boolean isMySqlSubFolder = (type == ConnectionProfile.DatabaseType.MYSQL
                || type == ConnectionProfile.DatabaseType.MARIADB) && isTableSubFolder;
        boolean isSqlServerPrincipalsFolder = (type == ConnectionProfile.DatabaseType.SQLSERVER
                && (kind == Kind.SERVER_PRINCIPALS_FOLDER || kind == Kind.DATABASE_PRINCIPALS_FOLDER));
        boolean includeObjActions = (kind != Kind.SEQUENCES_FOLDER && kind != Kind.SEQUENCE)
                && (!isServerObjectsFolder || isSqlServerPrincipalsFolder) && !isVirtualViewsFolder && !isMongoSubNode
                && !isOracleServerObjectItem
                && !isVirtualViewItem
                && !isCollationItem
                && kind != Kind.COLUMNS_FOLDER
                && !isMySqlSubFolder
                && (kind != Kind.INDEXES_FOLDER || type != ConnectionProfile.DatabaseType.ORACLE);
        if (includeObjActions) {
            Menu objActions = new Menu("Object Actions");
            boolean filterable = MetadataService.supportsDatabaseFilter(profile);
            String noun = (type == ConnectionProfile.DatabaseType.SQLSERVER
                    || type == ConnectionProfile.DatabaseType.MYSQL
                    || type == ConnectionProfile.DatabaseType.MARIADB
                    || type == ConnectionProfile.DatabaseType.ORACLE) ? "schema" : "database";
            String capNoun = Character.toUpperCase(noun.charAt(0)) + noun.substring(1);

            if (filterable) {
                MenuItem showHide = new MenuItem("Show / Hide " + capNoun + "s…");
                TreeItem<DbObject> connItem = item != null ? item : findConnectionItem(profile);
                showHide.setOnAction(e -> showDatabaseFilterDialog(connItem, profile));
                objActions.getItems().add(showHide);
            }
            MenuItem forgetPw = new MenuItem("Forget Saved Passwords");
            forgetPw.setOnAction(e -> {
                profile.setPassword("");
                ConnectionStore.saveOrUpdate(profile);
                if (mainWindow != null) mainWindow.setStatus("Forgot saved password for " + profile.getName());
            });
            objActions.getItems().add(forgetPw);
            menu.getItems().add(objActions);
        }

        // Tools >
        Menu toolsMenu = new Menu("Tools");
        boolean filterable = MetadataService.supportsDatabaseFilter(profile);
        String noun = (type == ConnectionProfile.DatabaseType.SQLSERVER
                || type == ConnectionProfile.DatabaseType.MYSQL
                || type == ConnectionProfile.DatabaseType.MARIADB
                || type == ConnectionProfile.DatabaseType.ORACLE) ? "schema" : "database";
        String capNoun = Character.toUpperCase(noun.charAt(0)) + noun.substring(1);

        if (filterable) {
            MenuItem manage = new MenuItem("Manage " + capNoun + "s…");
            TreeItem<DbObject> connItem = item != null ? item : findConnectionItem(profile);
            manage.setOnAction(e -> showDatabaseFilterDialog(connItem, profile));
            toolsMenu.getItems().add(manage);
        }
        MenuItem verifyConn = new MenuItem("Verify Connection");
        verifyConn.setOnAction(e -> verifyConnection(profile));
        toolsMenu.getItems().add(verifyConn);
        menu.getItems().add(toolsMenu);

        // Import/Export >
        boolean includeImportExport = !isServerObjectsFolder && !isVirtualViewsFolder;
        if (includeImportExport) {
            Menu importExport = new Menu("Import/Export");
            if (isCategoryFolder) {
                String dumpName = switch (type) {
                    case MYSQL -> "Dump with mysqldump…";
                    case MARIADB -> "Dump with mariadb-dump…";
                    case MONGODB -> "Dump with mongodump…";
                    case SQLITE -> "Export SQLite Database…";
                    case SQLSERVER -> "Export Database with bcp…";
                    case ORACLE -> "Export with expdp…";
                    case STRATOSDB -> "Dump with stratosdump…";
                    default -> "Dump Database…";
                };
                MenuItem dumpItem = new MenuItem(dumpName);
                dumpItem.setOnAction(e -> {
                    if (type == ConnectionProfile.DatabaseType.MYSQL || type == ConnectionProfile.DatabaseType.MARIADB) {
                        MySqlDumpDialog.show(mainWindow, profile, targetCatalog);
                    } else if (type == ConnectionProfile.DatabaseType.STRATOSDB) {
                        StratosDumpService.dumpDatabase(mainWindow, profile, targetCatalog != null ? targetCatalog : profile.getDatabase());
                    } else {
                        if (mainWindow != null) mainWindow.setStatus(dumpName.replace("…", ""));
                    }
                });
                MenuItem restoreItem = new MenuItem("Restore…");
                restoreItem.setOnAction(e -> {
                    if (type == ConnectionProfile.DatabaseType.MYSQL || type == ConnectionProfile.DatabaseType.MARIADB) {
                        MySqlRestoreDialog.show(mainWindow, profile, targetCatalog);
                    } else if (type == ConnectionProfile.DatabaseType.STRATOSDB) {
                        StratosDumpService.restoreDatabase(mainWindow, profile, targetCatalog != null ? targetCatalog : profile.getDatabase());
                    } else {
                        if (mainWindow != null) mainWindow.setStatus("Restore database");
                    }
                });
                importExport.getItems().addAll(dumpItem, restoreItem);
            } else if (isRoutineItem) {
                String dumpName = (type == ConnectionProfile.DatabaseType.ORACLE) ? "Export with expdp…" : "Dump Database…";
                MenuItem dumpItem = new MenuItem(dumpName);
                dumpItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.setStatus(dumpName.replace("…", ""));
                });
                MenuItem restoreItem = new MenuItem("Restore…");
                restoreItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.setStatus("Restore database");
                });
                importExport.getItems().addAll(dumpItem, restoreItem);
            } else if (isMongoSubItem) {
                MenuItem dumpItem = new MenuItem("Dump with mongodump…");
                dumpItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.setStatus("Dump with mongodump");
                });
                MenuItem restoreItem = new MenuItem("Restore…");
                restoreItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.setStatus("Restore database");
                });
                importExport.getItems().addAll(dumpItem, restoreItem);
            } else if (isOracleServerObjectItem) {
                MenuItem dumpItem = new MenuItem("Export with expdp…");
                dumpItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.setStatus("Export with expdp");
                });
                MenuItem restoreItem = new MenuItem("Restore…");
                restoreItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.setStatus("Restore database");
                });
                importExport.getItems().addAll(dumpItem, restoreItem);
            } else if (isCollationItem || isUserItem) {
                String dumpName = (type == ConnectionProfile.DatabaseType.MARIADB)
                        ? "Dump with mariadb-dump…"
                        : "Dump with mysqldump…";
                MenuItem dumpItem = new MenuItem(dumpName);
                dumpItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.setStatus(dumpName.replace("…", ""));
                });
                MenuItem restoreItem = new MenuItem("Restore…");
                restoreItem.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.setStatus("Restore database");
                });
                importExport.getItems().addAll(dumpItem, restoreItem);
            } else {
                MenuItem exportData = new MenuItem("Export Data to File…");
                exportData.setOnAction(e -> exportTableData(profile, obj));
                MenuItem importData = new MenuItem("Import Data from File(s)…");
                importData.setOnAction(e -> ImportDataDialog.show(mainWindow, profile, obj));
                importExport.getItems().addAll(exportData, importData);
                if (type == ConnectionProfile.DatabaseType.MYSQL || type == ConnectionProfile.DatabaseType.MARIADB) {
                    MenuItem exportMysqldump = new MenuItem("Export with 'mysqldump'…");
                    exportMysqldump.setOnAction(e -> MySqlDumpDialog.show(mainWindow, profile, targetCatalog));
                    MenuItem restoreMysql = new MenuItem("Restore with 'mysql'…");
                    restoreMysql.setOnAction(e -> MySqlRestoreDialog.show(mainWindow, profile, targetCatalog));
                    importExport.getItems().addAll(new SeparatorMenuItem(), exportMysqldump, restoreMysql);
                } else if (type == ConnectionProfile.DatabaseType.STRATOSDB) {
                    MenuItem exportStratos = new MenuItem("Dump with stratosdump…");
                    exportStratos.setOnAction(e -> StratosDumpService.dumpDatabase(mainWindow, profile, targetCatalog != null ? targetCatalog : profile.getDatabase()));
                    MenuItem restoreStratos = new MenuItem("Restore…");
                    restoreStratos.setOnAction(e -> StratosDumpService.restoreDatabase(mainWindow, profile, targetCatalog != null ? targetCatalog : profile.getDatabase()));
                    importExport.getItems().addAll(new SeparatorMenuItem(), exportStratos, restoreStratos);
                }
            }
            menu.getItems().add(importExport);
        }

        // Diagrams >
        Menu diagrams = new Menu("Diagrams");
        MenuItem showDiagram = new MenuItem("Show Diagram…");
        showDiagram.setOnAction(e -> {
            if (mainWindow != null) {
                if (isTable) {
                    mainWindow.openDiagramTab(profile, obj);
                } else {
                    mainWindow.openDatabaseDiagramTab(profile, targetCatalog);
                }
            }
        });
        diagrams.getItems().add(showDiagram);
        menu.getItems().add(diagrams);

        menu.getItems().add(new SeparatorMenuItem());

        // Diagnostics >
        Menu diagnostics = new Menu("Diagnostics");
        MenuItem diagRefresh = new MenuItem("Diagnostic Refresh");
        diagRefresh.setOnAction(e -> {
            if (item != null) refreshNode(item, obj, obj.getName());
            else reload();
            if (mainWindow != null) mainWindow.setStatus("Diagnostic refresh performed for " + obj.getName());
        });
        diagnostics.getItems().add(diagRefresh);
        menu.getItems().add(diagnostics);

        menu.getItems().add(new SeparatorMenuItem());

        // Drop… (Delete)
        MenuItem drop = new MenuItem("Drop…");
        drop.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        if (isCategoryFolder || isMongoSubItem || isCollationItem || isFilegroupItem) {
            drop.setDisable(true);
        } else if (isOracleServerObjectItem || isTableSubItem || isVirtualViewItem || isUserItem || isSqlServerServerObjectItem) {
            drop.setOnAction(e -> confirmAndDropGenericObject(profile, obj));
        } else if (isCollection) {
            drop.setOnAction(e -> confirmAndDropCollection(profile, obj));
        } else if (isTable) {
            drop.setOnAction(e -> confirmAndDeleteTable(profile, obj));
        } else if (isSequence) {
            drop.setOnAction(e -> confirmAndDeleteSequence(profile, obj));
        } else if (isRoutineItem) {
            String kindLabel = (kind == Kind.FUNCTION) ? "Function" : "Procedure";
            drop.setOnAction(e -> dropRoutine(profile, obj, kindLabel));
        }
        menu.getItems().add(drop);

        return menu;
    }

    ContextMenu buildDatabaseMenu(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        return buildDatabaseOrSchemaMenu(item, obj, profile);
    }

    ContextMenu buildSchemaMenu(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        if (profile != null && profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
            return buildPostgreSqlMenu(item, obj, profile);
        }
        return buildDatabaseOrSchemaMenu(item, obj, profile);
    }

    private ContextMenu buildDatabaseOrSchemaMenu(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        ContextMenu menu = new ContextMenu();
        if (profile == null) return menu;

        String targetCatalog = resolveCatalogFor(item, obj);

        // 1. New >
        Menu newMenu = new Menu("New");
        MenuItem queryConsole = new MenuItem("Query Console");
        queryConsole.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        queryConsole.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog, null);
        });

        MenuItem queryFile = new MenuItem("Query File…");
        queryFile.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
        queryFile.setOnAction(e -> promptAndOpenQueryFile(profile));

        newMenu.getItems().addAll(queryConsole, queryFile);

        switch (profile.getType()) {
            case POSTGRESQL -> {
                if (obj.getKind() == Kind.DATABASE) {
                    MenuItem newSchema = new MenuItem("Schema…");
                    newSchema.setOnAction(e -> promptCreateSchema(profile, obj.getName()));
                    MenuItem newLang = new MenuItem("Language…");
                    newLang.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, obj.getName(), "CREATE EXTENSION IF NOT EXISTS plpgsql;\n");
                    });
                    MenuItem newDb = new MenuItem("Database…");
                    newDb.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));
                    MenuItem newRole = new MenuItem("Role…");
                    newRole.setOnAction(e -> CreateRoleDialog.show(mainWindow, profile));
                    MenuItem newUser = new MenuItem("User…");
                    newUser.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
                    newMenu.getItems().addAll(new SeparatorMenuItem(), newSchema, newLang, newDb, newRole, newUser);
                } else {
                    MenuItem newTable = new MenuItem("Table");
                    newTable.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, obj.getCatalog(),
                                "CREATE TABLE \"" + obj.getName() + "\".\"new_table\" (\n    id SERIAL PRIMARY KEY\n);\n");
                    });
                    MenuItem newView = new MenuItem("View");
                    newView.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, obj.getCatalog(),
                                "CREATE VIEW \"" + obj.getName() + "\".\"new_view\" AS\nSELECT 1;\n");
                    });
                    MenuItem newMView = new MenuItem("Materialized View");
                    newMView.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, obj.getCatalog(),
                                "CREATE MATERIALIZED VIEW \"" + obj.getName() + "\".\"new_matview\" AS\nSELECT 1;\n");
                    });
                    MenuItem newSeq = new MenuItem("Sequence");
                    newSeq.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, obj.getCatalog(),
                                "CREATE SEQUENCE \"" + obj.getName() + "\".\"new_seq\";\n");
                    });
                    MenuItem newRoutine = new MenuItem("Routine");
                    newRoutine.setOnAction(e -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, obj.getCatalog(),
                                "CREATE OR REPLACE FUNCTION \"" + obj.getName() + "\".\"new_func\"()\nRETURNS void AS $$\nBEGIN\nEND;\n$$ LANGUAGE plpgsql;\n");
                    });
                    newMenu.getItems().addAll(new SeparatorMenuItem(), newTable, newView, newMView, newSeq, newRoutine);
                }
            }
            case MYSQL, MARIADB -> {
                MenuItem newTable = new MenuItem("Table");
                newTable.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, obj.getName(),
                            "CREATE TABLE `" + obj.getName() + "`.`new_table` (\n    `id` INT PRIMARY KEY AUTO_INCREMENT\n);\n");
                });
                MenuItem newView = new MenuItem("View");
                newView.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, obj.getName(),
                            "CREATE VIEW `" + obj.getName() + "`.`new_view` AS\nSELECT 1;\n");
                });
                MenuItem newSchema = new MenuItem("Schema…");
                newSchema.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));
                MenuItem newUser = new MenuItem("User…");
                newUser.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
                MenuItem newRole = new MenuItem("Role…");
                newRole.setOnAction(e -> CreateRoleDialog.show(mainWindow, profile));
                newMenu.getItems().addAll(new SeparatorMenuItem(), newTable, newView, newSchema, newUser, newRole);
            }
            case MONGODB -> {
                MenuItem newColl = new MenuItem("Collection…");
                newColl.setOnAction(e -> promptCreateMongoCollection(profile, obj.getName()));
                newMenu.getItems().addAll(new SeparatorMenuItem(), newColl);
            }
            case SQLITE -> {
                MenuItem newTable = new MenuItem("Table");
                newTable.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, null,
                            "CREATE TABLE new_table (\n    id INTEGER PRIMARY KEY AUTOINCREMENT\n);\n");
                });
                MenuItem newView = new MenuItem("View");
                newView.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, null,
                            "CREATE VIEW new_view AS\nSELECT 1;\n");
                });
                newMenu.getItems().addAll(new SeparatorMenuItem(), newTable, newView);
            }
            case SQLSERVER -> {
                if (obj.getKind() == Kind.DATABASE) {
                    MenuItem newSchema = new MenuItem("Schema");
                    newSchema.setOnAction(e -> promptCreateSqlServerSchema(profile, obj.getName()));
                    MenuItem newDbPrincipal = new MenuItem("Database Principal");
                    newDbPrincipal.setOnAction(e -> promptCreateSqlServerDbPrincipal(profile, obj.getName()));
                    MenuItem newDb = new MenuItem("Database");
                    newDb.setOnAction(e -> CreateDatabaseDialog.show(mainWindow, profile));
                    MenuItem serverPrincipal = new MenuItem("Server Principal");
                    serverPrincipal.setOnAction(e -> CreateUserDialog.show(mainWindow, profile));
                    newMenu.getItems().addAll(new SeparatorMenuItem(), newSchema, newDbPrincipal, newDb, serverPrincipal);
                } else {
                    MenuItem newTable = new MenuItem("Table");
                    MenuItem newView = new MenuItem("View");
                    MenuItem newProc = new MenuItem("Procedure");
                    newMenu.getItems().addAll(new SeparatorMenuItem(), newTable, newView, newProc);
                }
            }
            case ORACLE -> {
                // Oracle schema + New > goes directly to Virtual View & Data Source templates
            }
            case STRATOSDB -> {
                MenuItem newTable = new MenuItem("Table");
                newTable.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, obj.getName(),
                            "CREATE TABLE new_table (\n    id INT PRIMARY KEY AUTO_INCREMENT\n);\n");
                });
                MenuItem newView = new MenuItem("View");
                newView.setOnAction(e -> {
                    if (mainWindow != null) mainWindow.openQueryTab(profile, obj.getName(),
                            "CREATE VIEW new_view AS\nSELECT 1;\n");
                });
                newMenu.getItems().addAll(new SeparatorMenuItem(), newTable, newView);
            }
            default -> {}
        }

        MenuItem virtualView = new MenuItem("Virtual View");
        virtualView.setOnAction(e -> showVirtualViewInfo(profile, targetCatalog));
        newMenu.getItems().addAll(new SeparatorMenuItem(), virtualView);

        appendCommonNewDataSourceItems(newMenu, profile, false);

        // 2. Modify Database... vs Modify Schema... (omitted for MongoDB and SQLite)
        MenuItem modifyItem = null;
        if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL
                || profile.getType() == ConnectionProfile.DatabaseType.SQLSERVER
                || profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
            if (obj.getKind() == Kind.DATABASE) {
                modifyItem = new MenuItem("Modify Database…");
                modifyItem.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
                modifyItem.setOnAction(e -> ModifyDatabaseDialog.show(mainWindow, profile, obj.getName()));
            } else if (obj.getKind() == Kind.SCHEMA) {
                modifyItem = new MenuItem("Modify Schema…");
                modifyItem.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
                modifyItem.setOnAction(e -> ModifyDatabaseDialog.show(mainWindow, profile, obj.getName()));
            }
        } else if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                || profile.getType() == ConnectionProfile.DatabaseType.MARIADB) {
            modifyItem = new MenuItem("Modify Schema…");
            modifyItem.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN));
            modifyItem.setOnAction(e -> ModifyDatabaseDialog.show(mainWindow, profile, obj.getName()));
        }

        // 3. Copy/Paste >
        Menu copyPasteMenu = new Menu("Copy/Paste");
        MenuItem copyName = new MenuItem("Copy");
        copyName.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyName.setOnAction(e -> {
            copyToClipboard(obj.getName());
            if (mainWindow != null) mainWindow.setStatus("Copied to clipboard: " + obj.getName());
        });
        MenuItem copyPath = new MenuItem("Copy Path/Reference…");
        copyPath.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
        copyPath.setOnAction(e -> {
            String ref = obj.qualifiedName();
            copyToClipboard(ref);
            if (mainWindow != null) mainWindow.setStatus("Copied reference: " + ref);
        });
        copyPasteMenu.getItems().addAll(copyName, copyPath);

        // 4. Refresh (Ctrl+F5)
        MenuItem refresh = new MenuItem("Refresh");
        refresh.setAccelerator(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN));
        refresh.setOnAction(e -> {
            if (item != null) refreshNode(item, obj, obj.getName());
            else reload();
        });

        // 5. Rename... (Shift+F6)
        MenuItem rename = new MenuItem("Rename…");
        rename.setAccelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN));
        boolean renameEnabled = false;
        if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
            renameEnabled = true;
            if (obj.getKind() == Kind.DATABASE) {
                rename.setOnAction(e -> renameDatabase(profile, obj.getName()));
            } else {
                rename.setOnAction(e -> promptRenameSchema(profile, obj));
            }
        }
        rename.setDisable(!renameEnabled);

        // 6. Quick Documentation (Ctrl+Q)
        MenuItem quickDoc = new MenuItem("Quick Documentation");
        quickDoc.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        quickDoc.setOnAction(e -> showQuickDocumentation(profile, obj));

        // 7. Compare Schema Structure With... (Ctrl+D)
        MenuItem compareStructure = new MenuItem("Compare Schema Structure With…");
        compareStructure.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
        compareStructure.setDisable(true);

        // 8. Navigation >
        Menu navMenu = new Menu("Navigation");
        MenuItem goToDdl = new MenuItem("Go to DDL");
        goToDdl.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openDdlTab(profile, obj);
        });
        MenuItem jumpConsole = new MenuItem("Jump to Query Console");
        jumpConsole.setAccelerator(new KeyCodeCombination(KeyCode.F10, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        jumpConsole.setOnAction(e -> jumpToQueryConsole());
        MenuItem selectInTree = new MenuItem("Select in Database Explorer");
        selectInTree.setAccelerator(new KeyCodeCombination(KeyCode.F1, KeyCombination.ALT_DOWN));
        selectInTree.setOnAction(e -> {
            if (item != null) tree.getSelectionModel().select(item);
        });
        navMenu.getItems().addAll(goToDdl, jumpConsole, selectInTree);

        // 9. Find Usages (Alt+Shift+7)
        MenuItem findUsages = new MenuItem("Find Usages");
        findUsages.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT7, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
        findUsages.setDisable(true);

        // 10. Introspection Level >
        Menu introMenu = new Menu("Introspection Level");
        ToggleGroup introGroup = new ToggleGroup();
        RadioMenuItem introAuto = new RadioMenuItem("Auto");
        introAuto.setToggleGroup(introGroup);
        introAuto.setSelected(true);
        RadioMenuItem introHigh = new RadioMenuItem("High");
        introHigh.setToggleGroup(introGroup);
        RadioMenuItem introMed = new RadioMenuItem("Medium");
        introMed.setToggleGroup(introGroup);
        RadioMenuItem introLow = new RadioMenuItem("Low");
        introLow.setToggleGroup(introGroup);
        introMenu.getItems().addAll(introAuto, introHigh, introMed, introLow);

        // 11. SQL Scripts >
        Menu sqlScripts = new Menu("SQL Scripts");
        MenuItem openConsole = new MenuItem("Open Query Console");
        openConsole.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog, null);
        });
        MenuItem runSqlScript = new MenuItem("Run SQL Script…");
        runSqlScript.setOnAction(e -> promptRunSqlScript(profile));
        MenuItem genDdl = new MenuItem("Generate DDL to Query Console");
        genDdl.setOnAction(e -> {
            AppExecutor.run(() -> {
                try {
                    String ddl = DdlGeneratorService.generateDdl(profile, obj);
                    Platform.runLater(() -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog, ddl);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        if (mainWindow != null) mainWindow.openQueryTab(profile, targetCatalog,
                                "-- Could not generate DDL: " + ex.getMessage());
                    });
                }
            });
        });
        sqlScripts.getItems().addAll(openConsole, runSqlScript, genDdl);

        // 12. Object Actions >
        Menu objActions = new Menu("Object Actions");
        String noun = schemaOrDatabaseNoun(profile.getType());
        String capNoun = Character.toUpperCase(noun.charAt(0)) + noun.substring(1);
        if (MetadataService.supportsDatabaseFilter(profile)) {
            MenuItem showHideDbs = new MenuItem("Show / Hide " + capNoun + "s…");
            TreeItem<DbObject> connItem = item != null ? item : findConnectionItem(profile);
            showHideDbs.setOnAction(e -> showDatabaseFilterDialog(connItem, profile));
            objActions.getItems().add(showHideDbs);
        }
        MenuItem forgetPw = new MenuItem("Forget Saved Passwords");
        forgetPw.setOnAction(e -> {
            profile.setPassword("");
            ConnectionStore.saveOrUpdate(profile);
            if (mainWindow != null) mainWindow.setStatus("Forgot saved password for " + profile.getName());
        });
        objActions.getItems().add(forgetPw);

        // 13. Tools >
        Menu toolsMenu = new Menu("Tools");
        if (MetadataService.supportsDatabaseFilter(profile)) {
            MenuItem manageDbs = new MenuItem("Manage " + capNoun + "s…");
            TreeItem<DbObject> connItem = item != null ? item : findConnectionItem(profile);
            manageDbs.setOnAction(e -> showDatabaseFilterDialog(connItem, profile));
            toolsMenu.getItems().add(manageDbs);
        }
        MenuItem verifyConn = new MenuItem("Verify Connection");
        verifyConn.setOnAction(e -> verifyConnection(profile));
        toolsMenu.getItems().add(verifyConn);

        // 14. Import/Export >
        Menu importExport = new Menu("Import/Export");
        String targetDatabase = (obj.getKind() == Kind.DATABASE)
                ? obj.getName()
                : (obj.getCatalog() != null && !obj.getCatalog().isBlank() ? obj.getCatalog() : profile.getDatabase());
        String dumpName = switch (profile.getType()) {
            case POSTGRESQL -> "Dump with pg_dump…";
            case MYSQL -> "Dump with mysqldump…";
            case MARIADB -> "Dump with mariadb-dump…";
            case SQLITE -> "Export SQLite Database…";
            case MONGODB -> "Dump with mongodump…";
            case SQLSERVER -> "Export Database with bcp…";
            case ORACLE -> "Export with expdp…";
            case STRATOSDB -> "Dump with stratosdump…";
            default -> "Dump Database…";
        };
        MenuItem dumpItem = new MenuItem(dumpName);
        dumpItem.setOnAction(e -> {
            if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
                PgDumpDialog.show(mainWindow, profile, targetDatabase);
            } else if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                    || profile.getType() == ConnectionProfile.DatabaseType.MARIADB) {
                MySqlDumpDialog.show(mainWindow, profile, targetDatabase);
            } else if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                StratosDumpService.dumpDatabase(mainWindow, profile, targetDatabase);
            } else {
                if (mainWindow != null) mainWindow.setStatus(dumpName.replace("…", ""));
            }
        });
        MenuItem restoreItem = new MenuItem("Restore…");
        restoreItem.setOnAction(e -> {
            if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
                RestoreDialog.show(mainWindow, profile, targetDatabase);
            } else if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                    || profile.getType() == ConnectionProfile.DatabaseType.MARIADB) {
                MySqlRestoreDialog.show(mainWindow, profile, targetDatabase);
            } else if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                StratosDumpService.restoreDatabase(mainWindow, profile, targetDatabase);
            } else {
                if (mainWindow != null) mainWindow.setStatus("Restore database");
            }
        });
        importExport.getItems().addAll(dumpItem, restoreItem);

        // 15. Diagrams >
        Menu diagrams = new Menu("Diagrams");
        MenuItem showDiagram = new MenuItem("Show Diagram…");
        showDiagram.setOnAction(e -> {
            if (mainWindow != null) mainWindow.openDatabaseDiagramTab(profile, targetDatabase);
        });
        diagrams.getItems().add(showDiagram);

        // 16. Diagnostics >
        Menu diagnostics = new Menu("Diagnostics");
        MenuItem diagRefresh = new MenuItem("Diagnostic Refresh");
        diagRefresh.setOnAction(e -> {
            if (item != null) refreshNode(item, obj, obj.getName());
            else reload();
            if (mainWindow != null) mainWindow.setStatus("Diagnostic refresh performed for " + obj.getName());
        });
        diagnostics.getItems().add(diagRefresh);

        // 17. Drop... (Delete)
        MenuItem drop = new MenuItem("Drop…");
        drop.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        if (profile.getType() == ConnectionProfile.DatabaseType.SQLITE
                || profile.getType() == ConnectionProfile.DatabaseType.ORACLE) {
            drop.setDisable(true); // SQLite 'main' schema cannot be dropped; Oracle user/schema drop disabled in tree
        } else {
            if (obj.getKind() == Kind.DATABASE) {
                drop.setOnAction(e -> confirmAndDeleteDatabase(profile, obj.getName()));
            } else {
                drop.setOnAction(e -> confirmAndDeleteSchema(profile, obj));
            }
        }

        // Assemble context menu items in canonical order
        menu.getItems().add(newMenu);
        if (modifyItem != null) {
            menu.getItems().add(modifyItem);
        }
        menu.getItems().addAll(
                copyPasteMenu,
                refresh,
                new SeparatorMenuItem(),
                rename,
                new SeparatorMenuItem(),
                quickDoc,
                compareStructure,
                navMenu,
                findUsages,
                new SeparatorMenuItem(),
                introMenu,
                new SeparatorMenuItem(),
                sqlScripts
        );
        if (profile.getType() != ConnectionProfile.DatabaseType.ORACLE) {
            menu.getItems().add(objActions);
        }
        menu.getItems().addAll(
                toolsMenu,
                importExport,
                diagrams,
                diagnostics,
                new SeparatorMenuItem(),
                drop
        );

        return menu;
    }

    private static TreeItem<DbObject> loadingNode() {
        return new TreeItem<>(new DbObject("Loading…", Kind.MESSAGE));
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    ContextMenu buildMenuForTest(DbObject obj, ConnectionProfile profile) {
        DbObjectCell cell = new DbObjectCell();
        return cell.buildMenu(obj, profile);
    }

    // ------------------------------------------------------------------ cell

    /** Renders each node with an icon, name, and dim detail text; builds context menus. */
    private class DbObjectCell extends TreeCell<DbObject> {

        @Override
        protected void updateItem(DbObject obj, boolean empty) {
            super.updateItem(obj, empty);
            if (empty || obj == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }

            ConnectionProfile profile = profileFor(getTreeItem());
            boolean connected = profile != null && ClientRegistry.isConnected(profile);

            javafx.scene.Node nameNode = buildHighlightedName(obj.getName(), currentSearchQuery);
            HBox box = new HBox(6, Icons.forObject(obj, profile, connected), nameNode);
            box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            if (obj.getDetail() != null && !obj.getDetail().isBlank()) {
                Label detailLabel = new Label(obj.getDetail());
                detailLabel.getStyleClass().add("tree-detail");
                boolean isConnection = obj.getKind() == Kind.CONNECTION && profile != null;
                boolean isDatabase = obj.getKind() == Kind.DATABASE;
                boolean isVirtualViews = obj.getKind() == Kind.VIRTUAL_VIEWS_FOLDER;
                boolean isFilterable = isConnection && MetadataService.supportsDatabaseFilter(profile);

                if (isConnection || isDatabase || isVirtualViews) {
                    detailLabel.getStyleClass().add("tree-detail-pill");
                }
                if (isFilterable) {
                    detailLabel.setCursor(javafx.scene.Cursor.HAND);
                    String n = schemaOrDatabaseNoun(profile.getType());
                    detailLabel.setTooltip(new Tooltip("Choose " + n + "s to show in the tree…"));
                    detailLabel.setOnMouseClicked(e -> {
                        if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                            e.consume();
                            showDatabaseFilterDialog(getTreeItem(), profile);
                        }
                    });
                }
                box.getChildren().add(detailLabel);
            }
            setText(null);
            setGraphic(box);
            setContextMenu(buildMenu(obj, profile));
        }

        private ContextMenu buildMenu(DbObject obj, ConnectionProfile profile) {
            if (profile == null) return null;
            if (obj.getKind() == Kind.CONNECTION) {
                return buildConnectionMenu(getTreeItem(), obj, profile);
            }
            if (obj.getKind() == Kind.DATABASE) {
                return buildDatabaseMenu(getTreeItem(), obj, profile);
            }
            if (obj.getKind() == Kind.SCHEMA) {
                return buildSchemaMenu(getTreeItem(), obj, profile);
            }
            if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
                ContextMenu pgMenu = buildPostgreSqlMenu(getTreeItem(), obj, profile);
                if (pgMenu != null) {
                    return pgMenu;
                }
            }
            if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                    || profile.getType() == ConnectionProfile.DatabaseType.MARIADB
                    || profile.getType() == ConnectionProfile.DatabaseType.MONGODB
                    || profile.getType() == ConnectionProfile.DatabaseType.SQLITE
                    || profile.getType() == ConnectionProfile.DatabaseType.SQLSERVER
                    || profile.getType() == ConnectionProfile.DatabaseType.ORACLE
                    || profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                ContextMenu engineMenu = buildEngineMenu(getTreeItem(), obj, profile);
                if (engineMenu != null) {
                    return engineMenu;
                }
            }
            ContextMenu menu = new ContextMenu();

            switch (obj.getKind()) {
                case TABLE, VIEW, PARTITION -> {
                    MenuItem openData = new MenuItem("Open Data");
                    openData.setOnAction(e -> mainWindow.openDataTab(profile, obj));
                    MenuItem editData = new MenuItem("Edit Data");
                    editData.setOnAction(e -> editData());
                    MenuItem goToDdl = new MenuItem("Go to DDL");
                    goToDdl.setOnAction(e -> goToDdl());
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    MenuItem structure = new MenuItem("Show Structure");
                    structure.setOnAction(e -> mainWindow.openStructureTab(profile, obj));
                    MenuItem modifyTable = new MenuItem("Modify Table\u2026");
                    modifyTable.setOnAction(e -> ModifyTableDialog.show(mainWindow, profile, obj));
                    MenuItem refreshTable = new MenuItem("Refresh");
                    refreshTable.setOnAction(e -> refreshNode(getTreeItem(), obj, obj.getName()));

                    Menu importExportMenu = new Menu("Import/Export");
                    MenuItem exportData = new MenuItem("Export Data to File\u2026");
                    exportData.setOnAction(e -> exportTableData(profile, obj));
                    MenuItem importData = new MenuItem("Import Data from File(s)\u2026");
                    importData.setOnAction(e -> ImportDataDialog.show(mainWindow, profile, obj));
                    importExportMenu.getItems().addAll(exportData, importData);
                    String tableDatabase = obj.getCatalog() != null ? obj.getCatalog() : profile.getDatabase();
                    if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
                        MenuItem exportWithPgDump = new MenuItem("Export with 'pg_dump'\u2026");
                        exportWithPgDump.setOnAction(e -> PgDumpDialog.show(mainWindow, profile, tableDatabase));
                        MenuItem restoreWithPgRestore = new MenuItem("Restore with 'pg_restore'\u2026");
                        restoreWithPgRestore.setOnAction(e -> RestoreDialog.show(mainWindow, profile, tableDatabase));
                        importExportMenu.getItems().addAll(new SeparatorMenuItem(),
                                exportWithPgDump, restoreWithPgRestore);
                    } else if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                            || profile.getType() == ConnectionProfile.DatabaseType.MARIADB) {
                        MenuItem exportWithMysqldump = new MenuItem("Export with 'mysqldump'\u2026");
                        exportWithMysqldump.setOnAction(e -> MySqlDumpDialog.show(mainWindow, profile, tableDatabase));
                        MenuItem restoreWithMysql = new MenuItem("Restore with 'mysql'\u2026");
                        restoreWithMysql.setOnAction(e -> MySqlRestoreDialog.show(mainWindow, profile, tableDatabase));
                        importExportMenu.getItems().addAll(new SeparatorMenuItem(),
                                exportWithMysqldump, restoreWithMysql);
                    }

                    Menu diagramsMenu = new Menu("Diagrams");
                    MenuItem showDiagram = new MenuItem("Show Diagram\u2026");
                    showDiagram.setOnAction(e -> mainWindow.openDiagramTab(profile, obj));
                    diagramsMenu.getItems().add(showDiagram);

                    MenuItem select = new MenuItem("New Query: SELECT *");
                    select.setOnAction(e -> mainWindow.openQueryTab(profile, obj.getCatalog(),
                            selectTopRowsSql(profile.getType(), obj)));
                    MenuItem count = new MenuItem("New Query: COUNT(*)");
                    count.setOnAction(e -> mainWindow.openQueryTab(profile, obj.getCatalog(),
                            "SELECT COUNT(*) FROM " + obj.qualifiedName() + ";"));

                    menu.getItems().addAll(openData, editData, goToDdl, jumpConsole, structure, new SeparatorMenuItem(),
                            modifyTable, refreshTable, new SeparatorMenuItem(),
                            importExportMenu, diagramsMenu, new SeparatorMenuItem(), select, count);
                }
                case COLLECTION -> {
                    MenuItem openDocs = new MenuItem("Open Documents");
                    openDocs.setOnAction(e -> mainWindow.openMongoTab(profile, obj));
                    MenuItem newConsole = new MenuItem("New Console on " + obj.getName());
                    newConsole.setOnAction(e -> mainWindow.openQueryTab(profile, obj.getCatalog(),
                            "use " + obj.getCatalog() + ";\ndb." + obj.getName() + "."));
                    MenuItem modify = new MenuItem("Modify Collection\u2026");
                    modify.setOnAction(e -> ModifyCollectionDialog.show(mainWindow, profile, obj));
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> refreshNode(getTreeItem(), obj, obj.getName()));
                    MenuItem drop = new MenuItem("Drop\u2026");
                    drop.setOnAction(e -> confirmAndDropCollection(profile, obj));
                    menu.getItems().addAll(openDocs, newConsole, modify, refresh,
                            new SeparatorMenuItem(), drop);
                }
                case PROCEDURE, FUNCTION -> {
                    String kindLabel = obj.getKind() == Kind.FUNCTION ? "Function" : "Procedure";
                    MenuItem run = new MenuItem("Run\u2026");
                    run.setOnAction(e -> mainWindow.openRoutineRunTab(profile, obj));
                    MenuItem editSource = new MenuItem("Edit Source");
                    editSource.setOnAction(e -> mainWindow.openRoutineSourceTab(profile, obj));
                    MenuItem goToDdl = new MenuItem("Go to DDL");
                    goToDdl.setOnAction(e -> goToDdl());
                    MenuItem rename = new MenuItem("Rename\u2026");
                    rename.setOnAction(e -> renameRoutine(profile, obj));
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> {
                        obj.setLoaded(false);
                        mainWindow.setStatus("Refreshed " + obj.getName());
                        reload();
                    });
                    MenuItem drop = new MenuItem("Drop\u2026");
                    drop.setOnAction(e -> dropRoutine(profile, obj, kindLabel));

                    menu.getItems().addAll(run, editSource, goToDdl, new SeparatorMenuItem(),
                            rename, refresh, new SeparatorMenuItem(), drop);
                }
                case SERVER_OBJECTS -> {
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> refreshNode(getTreeItem(), obj, obj.getName()));
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    menu.getItems().addAll(jumpConsole, new SeparatorMenuItem(), refresh);
                }
                case ROLES_FOLDER -> {
                    MenuItem newRole = new MenuItem("New Role…");
                    newRole.setOnAction(e -> CreateRoleDialog.show(mainWindow, profile));
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> refreshNode(getTreeItem(), obj, obj.getName()));
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    menu.getItems().addAll(newRole, new SeparatorMenuItem(), jumpConsole, refresh);
                }
                case ROLE -> {
                    MenuItem goToDdl = new MenuItem("Go to DDL");
                    goToDdl.setOnAction(e -> goToDdl());
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> {
                        obj.setLoaded(false);
                        mainWindow.setStatus("Refreshed " + obj.getName());
                        reload();
                    });
                    MenuItem drop = new MenuItem("Drop Role…");
                    drop.setOnAction(e -> confirmAndDropRole(profile, obj.getName()));
                    menu.getItems().addAll(goToDdl, jumpConsole, new SeparatorMenuItem(), refresh, new SeparatorMenuItem(), drop);
                }
                case TABLESPACES_FOLDER -> {
                    MenuItem newTs = new MenuItem("New Tablespace…");
                    newTs.setOnAction(e -> promptCreateTablespace(profile));
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> refreshNode(getTreeItem(), obj, obj.getName()));
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    menu.getItems().addAll(newTs, new SeparatorMenuItem(), jumpConsole, refresh);
                }
                case TABLESPACE -> {
                    MenuItem goToDdl = new MenuItem("Go to DDL");
                    goToDdl.setOnAction(e -> goToDdl());
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> {
                        obj.setLoaded(false);
                        mainWindow.setStatus("Refreshed " + obj.getName());
                        reload();
                    });
                    MenuItem drop = new MenuItem("Drop Tablespace…");
                    drop.setOnAction(e -> confirmAndDropTablespace(profile, obj.getName()));
                    menu.getItems().addAll(goToDdl, jumpConsole, new SeparatorMenuItem(), refresh, new SeparatorMenuItem(), drop);
                }
                case DATABASE_OBJECTS, ACCESS_METHODS_FOLDER, CASTS_FOLDER, EXTENSIONS_FOLDER,
                     LANGUAGES_FOLDER, COLLATIONS_FOLDER, LINKED_SERVERS_FOLDER, SERVER_PRINCIPALS_FOLDER,
                     ASSEMBLIES_FOLDER, FILEGROUPS_FOLDER, DATABASE_PRINCIPALS_FOLDER,
                     VIRTUAL_VIEWS_FOLDER, USERS_FOLDER -> {
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> refreshNode(getTreeItem(), obj, obj.getName()));
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    menu.getItems().addAll(jumpConsole, new SeparatorMenuItem(), refresh);
                }
                case ACCESS_METHOD, CAST, EXTENSION, LANGUAGE,
                     COLLATION, LINKED_SERVER, SERVER_PRINCIPAL,
                     ASSEMBLY, FILEGROUP, DATABASE_PRINCIPAL,
                     USER, DATA_FILE -> {
                    MenuItem goToDdl = new MenuItem("Go to DDL");
                    goToDdl.setOnAction(e -> goToDdl());
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> {
                        obj.setLoaded(false);
                        mainWindow.setStatus("Refreshed " + obj.getName());
                        reload();
                    });
                    menu.getItems().addAll(goToDdl, jumpConsole, new SeparatorMenuItem(), refresh);
                }
                case VIRTUAL_VIEW -> {
                    MenuItem openData = new MenuItem("Open Data");
                    openData.setOnAction(e -> mainWindow.openDataTab(profile, obj));
                    MenuItem editData = new MenuItem("Edit Data");
                    editData.setOnAction(e -> editData());
                    MenuItem goToDdl = new MenuItem("Go to DDL");
                    goToDdl.setOnAction(e -> goToDdl());
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> {
                        obj.setLoaded(false);
                        mainWindow.setStatus("Refreshed " + obj.getName());
                        reload();
                    });
                    menu.getItems().addAll(openData, editData, goToDdl, jumpConsole, new SeparatorMenuItem(), refresh);
                }
                case SEQUENCE -> {
                    MenuItem goToDdl = new MenuItem("Go to DDL");
                    goToDdl.setOnAction(e -> goToDdl());
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> {
                        obj.setLoaded(false);
                        mainWindow.setStatus("Refreshed " + obj.getName());
                        reload();
                    });
                    menu.getItems().addAll(goToDdl, jumpConsole, new SeparatorMenuItem(), refresh);
                }
                case TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER, FUNCTIONS_FOLDER, SEQUENCES_FOLDER -> {
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> refreshNode(getTreeItem(), obj, obj.getName()));
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    menu.getItems().addAll(jumpConsole, new SeparatorMenuItem(), refresh);
                }
                default -> {
                    return null;
                }
            }
            return menu;
        }
    }

    /**
     * "Top N rows" syntax genuinely differs per engine — LIMIT doesn't
     * exist in Oracle at all (ORA-00933 the instant it's used, since the
     * parser has no idea what the trailing tokens mean), and while SQL
     * Server's driver tolerates LIMIT via some compatibility layers, TOP
     * is its native, always-correct form and needs no ORDER BY the way
     * OFFSET/FETCH technically wants one. PostgreSQL/MySQL/MariaDB/SQLite
     * all share the same LIMIT syntax, so they stay on the simple form.
     */
    private static String selectTopRowsSql(ConnectionProfile.DatabaseType type, DbObject obj) {
        return switch (type) {
            case ORACLE -> "SELECT * FROM " + obj.qualifiedName() + " FETCH FIRST 100 ROWS ONLY;";
            case SQLSERVER -> "SELECT TOP 100 * FROM " + obj.qualifiedName() + ";";
            default -> "SELECT * FROM " + obj.qualifiedName() + " LIMIT 100;";
        };
    }

    /**
     * Standard routine rename: a single "new name" prompt. The actual RENAME
     * syntax genuinely differs per engine — Oracle's generic RENAME
     * statement works for any object type including routines; SQL Server
     * uses sp_rename instead; PostgreSQL has an engine-specific ALTER
     * PROCEDURE/FUNCTION ... RENAME TO (which, without the full parameter
     * signature, only resolves correctly for a non-overloaded routine —
     * an acceptable simplification for the common case, and it fails with
     * a clear database error rather than silently doing the wrong thing
     * if there IS an overload); MySQL/MariaDB have no rename statement for
     * a routine at all, so that's reported as unsupported up front instead
     * of attempted.
     */
    private void renameRoutine(ConnectionProfile profile, DbObject obj) {
        String kindLabel = obj.getKind() == Kind.FUNCTION ? "Function" : "Procedure";
        if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                || profile.getType() == ConnectionProfile.DatabaseType.MARIADB) {
            DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                    profile.getType().getDisplayName() + " has no rename statement for a "
                    + kindLabel.toLowerCase() + " — the usual workaround is to view its source "
                    + "(Edit Source), then drop and recreate it under the new name."))
                    .showAndWait();
            return;
        }

        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(obj.getName()));
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("Rename " + kindLabel);
        dialog.setHeaderText(null);
        dialog.setContentText("New name for \"" + obj.getName() + "\":");

        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty() || trimmed.equals(obj.getName())) return;
            AppExecutor.run(() -> {
                try {
                    String sql = switch (profile.getType()) {
                        case ORACLE -> "RENAME " + obj.getName() + " TO " + trimmed;
                        case SQLSERVER -> "EXEC sp_rename '" + obj.getName() + "', '" + trimmed + "'";
                        default -> "ALTER " + kindLabel.toUpperCase(java.util.Locale.ROOT) + " "
                                + obj.getName() + " RENAME TO " + trimmed;
                    };
                    try (java.sql.Connection conn =
                                 ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                         java.sql.Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                    }
                    Platform.runLater(() -> {
                        mainWindow.setStatus("Renamed " + obj.getName() + " to " + trimmed);
                        reload();
                    });
                } catch (Exception ex) {
                    String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not rename " + obj.getName() + ": " + msg)).showAndWait());
                }
            });
        });
    }

    /** Standard destructive confirmation, scaled down from the database version — a routine is easier to recreate than a whole database, so a single Yes/No is enough rather than also requiring the name to be typed out. */
    private void dropRoutine(ConnectionProfile profile, DbObject obj, String kindLabel) {
        Alert warn = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                "This permanently deletes " + kindLabel.toLowerCase() + " \"" + obj.getName()
                        + "\". This cannot be undone.",
                ButtonType.YES, ButtonType.NO));
        warn.setTitle("Drop " + kindLabel);
        warn.setHeaderText("Drop \"" + obj.getName() + "\"?");
        warn.initOwner(getScene() == null ? null : getScene().getWindow());
        if (warn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        AppExecutor.run(() -> {
            try {
                String sql = "DROP " + kindLabel.toUpperCase(java.util.Locale.ROOT) + " " + obj.getName();
                try (java.sql.Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Dropped " + obj.getName());
                    reload();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not drop " + obj.getName() + ": " + msg)).showAndWait());
            }
        });
    }

    private void confirmAndDropRole(ConnectionProfile profile, String roleName) {
        Alert warn = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                "This permanently deletes role \"" + roleName + "\". This cannot be undone.",
                ButtonType.YES, ButtonType.NO));
        warn.setTitle("Drop Role");
        warn.setHeaderText("Drop role \"" + roleName + "\"?");
        warn.initOwner(getScene() == null ? null : getScene().getWindow());
        if (warn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        AppExecutor.run(() -> {
            try {
                try (java.sql.Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP ROLE " + roleName);
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Dropped role " + roleName);
                    reload();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not drop role " + roleName + ": " + msg)).showAndWait());
            }
        });
    }

    private void confirmAndDropTablespace(ConnectionProfile profile, String tsName) {
        Alert warn = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                "This permanently deletes tablespace \"" + tsName + "\". This cannot be undone.",
                ButtonType.YES, ButtonType.NO));
        warn.setTitle("Drop Tablespace");
        warn.setHeaderText("Drop tablespace \"" + tsName + "\"?");
        warn.initOwner(getScene() == null ? null : getScene().getWindow());
        if (warn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        AppExecutor.run(() -> {
            try {
                try (java.sql.Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLESPACE " + tsName);
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Dropped tablespace " + tsName);
                    reload();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not drop tablespace " + tsName + ": " + msg)).showAndWait());
            }
        });
    }

    private void promptCreateTablespace(ConnectionProfile profile) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog("new_tablespace"));
        dialog.setTitle("Create Tablespace");
        dialog.setHeaderText("Create Tablespace on " + profile.getName());
        dialog.setContentText("Tablespace name:");
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(name -> {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) return;
            mainWindow.openQueryTab(profile, null, "CREATE TABLESPACE " + trimmed + " LOCATION '/var/lib/postgresql/data/" + trimmed + "';");
        });
    }

    private void promptCreateSchema(ConnectionProfile profile, String database) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog("new_schema"));
        dialog.setTitle("New Schema");
        dialog.setHeaderText("Create Schema in " + database);
        dialog.setContentText("Schema name:");
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(schemaName -> {
            String trimmed = schemaName.trim();
            if (trimmed.isEmpty()) return;
            AppExecutor.run(() -> {
                try {
                    try (java.sql.Connection conn = ClientRegistry.jdbc(profile, database).getConnection();
                         java.sql.Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE SCHEMA \"" + trimmed.replace("\"", "\"\"") + "\"");
                    }
                    Platform.runLater(() -> {
                        mainWindow.setStatus("Created schema " + trimmed);
                        reload();
                    });
                } catch (Exception ex) {
                    String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not create schema: " + msg)).showAndWait());
                }
            });
        });
    }

    private void promptCreateMongoCollection(ConnectionProfile profile, String database) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog("new_collection"));
        dialog.setTitle("New Collection");
        dialog.setHeaderText("Create Collection in " + database);
        dialog.setContentText("Collection name:");
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(colName -> {
            String trimmed = colName.trim();
            if (trimmed.isEmpty()) return;
            AppExecutor.run(() -> {
                try {
                    ClientRegistry.mongo(profile).createCollection(database, trimmed);
                    Platform.runLater(() -> {
                        mainWindow.setStatus("Created collection " + trimmed);
                        reload();
                    });
                } catch (Exception ex) {
                    String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not create collection: " + msg)).showAndWait());
                }
            });
        });
    }

    private void promptCreateSqlServerSchema(ConnectionProfile profile, String database) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog("new_schema"));
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("New Schema");
        dialog.setHeaderText(null);
        dialog.setContentText("Schema name:");
        dialog.showAndWait().ifPresent(name -> {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) return;
            String sql = "USE [" + database + "];\nGO\nCREATE SCHEMA [" + trimmed.replace("]", "]]") + "];\n";
            if (mainWindow != null) {
                mainWindow.openQueryTab(profile, database, sql);
            }
        });
    }

    private void promptCreateSqlServerDbPrincipal(ConnectionProfile profile, String database) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog("new_user"));
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("New Database Principal");
        dialog.setHeaderText(null);
        dialog.setContentText("Database principal name:");
        dialog.showAndWait().ifPresent(name -> {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) return;
            String sql = "USE [" + database + "];\nGO\nCREATE USER [" + trimmed.replace("]", "]]") + "] WITHOUT LOGIN;\n";
            if (mainWindow != null) {
                mainWindow.openQueryTab(profile, database, sql);
            }
        });
    }

    private void promptRenameSchema(ConnectionProfile profile, DbObject obj) {
        if (profile.getType() != ConnectionProfile.DatabaseType.POSTGRESQL) {
            DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                    profile.getType().getDisplayName() + " has no direct rename statement for a schema."))
                    .showAndWait();
            return;
        }
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(obj.getName()));
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("Rename Schema");
        dialog.setHeaderText(null);
        dialog.setContentText("New name for \"" + obj.getName() + "\":");

        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty() || trimmed.equals(obj.getName())) return;
            AppExecutor.run(() -> {
                try {
                    String sql = "ALTER SCHEMA \"" + obj.getName().replace("\"", "\"\"")
                            + "\" RENAME TO \"" + trimmed.replace("\"", "\"\"") + "\"";
                    try (java.sql.Connection conn =
                                 ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                         java.sql.Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                    }
                    Platform.runLater(() -> {
                        mainWindow.setStatus("Renamed schema " + obj.getName() + " to " + trimmed);
                        reload();
                    });
                } catch (Exception ex) {
                    String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not rename schema " + obj.getName() + ": " + msg)).showAndWait());
                }
            });
        });
    }

    private void promptRenameTable(ConnectionProfile profile, DbObject obj) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(obj.getName()));
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("Rename Table");
        dialog.setHeaderText(null);
        dialog.setContentText("New name for \"" + obj.getName() + "\":");

        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty() || trimmed.equals(obj.getName())) return;
            AppExecutor.run(() -> {
                try {
                    String sql;
                    if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                        sql = "ALTER TABLE \"" + obj.getName().replace("\"", "\"\"") + "\" RENAME TO \""
                                + trimmed.replace("\"", "\"\"") + "\"";
                    } else {
                        String schema = (obj.getSchema() != null && !obj.getSchema().isBlank()) ? obj.getSchema() : "public";
                        sql = "ALTER TABLE \"" + schema.replace("\"", "\"\"") + "\".\""
                                + obj.getName().replace("\"", "\"\"") + "\" RENAME TO \""
                                + trimmed.replace("\"", "\"\"") + "\"";
                    }
                    try (java.sql.Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                         java.sql.Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                    }
                    Platform.runLater(() -> {
                        mainWindow.setStatus("Renamed table " + obj.getName() + " to " + trimmed);
                        reload();
                    });
                } catch (Exception ex) {
                    String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not rename table " + obj.getName() + ": " + msg)).showAndWait());
                }
            });
        });
    }

    private void promptRenameMongoCollection(ConnectionProfile profile, DbObject obj) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(obj.getName()));
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("Rename Collection");
        dialog.setHeaderText(null);
        dialog.setContentText("New name for \"" + obj.getName() + "\":");

        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty() || trimmed.equals(obj.getName())) return;
            AppExecutor.run(() -> {
                try {
                    String database = (obj.getCatalog() != null && !obj.getCatalog().isBlank())
                            ? obj.getCatalog() : profile.getDatabase();
                    var res = ClientRegistry.mongo(profile).renameCollection(database, obj.getName(), trimmed);
                    Platform.runLater(() -> {
                        mainWindow.setStatus(res.message());
                        reload();
                    });
                } catch (Exception ex) {
                    String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not rename collection " + obj.getName() + ": " + msg)).showAndWait());
                }
            });
        });
    }

    private void promptRenameSequence(ConnectionProfile profile, DbObject obj) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(obj.getName()));
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.setTitle("Rename Sequence");
        dialog.setHeaderText(null);
        dialog.setContentText("New name for \"" + obj.getName() + "\":");

        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty() || trimmed.equals(obj.getName())) return;
            AppExecutor.run(() -> {
                try {
                    String schema = (obj.getSchema() != null && !obj.getSchema().isBlank()) ? obj.getSchema() : "public";
                    String sql = "ALTER SEQUENCE \"" + schema.replace("\"", "\"\"") + "\".\""
                            + obj.getName().replace("\"", "\"\"") + "\" RENAME TO \""
                            + trimmed.replace("\"", "\"\"") + "\"";
                    try (java.sql.Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                         java.sql.Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                    }
                    Platform.runLater(() -> {
                        mainWindow.setStatus("Renamed sequence " + obj.getName() + " to " + trimmed);
                        reload();
                    });
                } catch (Exception ex) {
                    String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not rename sequence " + obj.getName() + ": " + msg)).showAndWait());
                }
            });
        });
    }

    private void confirmAndDeleteTable(ConnectionProfile profile, DbObject obj) {
        String kindLabel = (obj.getKind() == Kind.VIEW) ? "view" : "table";
        String capKindLabel = Character.toUpperCase(kindLabel.charAt(0)) + kindLabel.substring(1);
        Alert warn = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                "This permanently deletes " + kindLabel + " \"" + obj.getName() + "\". This cannot be undone.",
                ButtonType.YES, ButtonType.NO));
        warn.setTitle("Drop " + capKindLabel);
        warn.setHeaderText("Drop " + kindLabel + " \"" + obj.getName() + "\"?");
        warn.initOwner(getScene() == null ? null : getScene().getWindow());
        if (warn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        AppExecutor.run(() -> {
            try {
                String sql;
                if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                    if (obj.getKind() == Kind.VIEW) {
                        sql = "DROP VIEW \"" + obj.getName().replace("\"", "\"\"") + "\"";
                    } else {
                        sql = "DROP TABLE \"" + obj.getName().replace("\"", "\"\"") + "\"";
                    }
                } else {
                    String schema = (obj.getSchema() != null && !obj.getSchema().isBlank()) ? obj.getSchema() : "public";
                    sql = "DROP TABLE \"" + schema.replace("\"", "\"\"") + "\".\""
                            + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                }
                try (java.sql.Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Dropped " + kindLabel + " " + obj.getName());
                    reload();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not drop " + kindLabel + " " + obj.getName() + ": " + msg)).showAndWait());
            }
        });
    }

    private void confirmAndDeleteSequence(ConnectionProfile profile, DbObject obj) {
        Alert warn = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                "This permanently deletes sequence \"" + obj.getName() + "\". This cannot be undone.",
                ButtonType.YES, ButtonType.NO));
        warn.setTitle("Drop Sequence");
        warn.setHeaderText("Drop sequence \"" + obj.getName() + "\"?");
        warn.initOwner(getScene() == null ? null : getScene().getWindow());
        if (warn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        AppExecutor.run(() -> {
            try {
                String schema = (obj.getSchema() != null && !obj.getSchema().isBlank()) ? obj.getSchema() : "public";
                String sql = "DROP SEQUENCE \"" + schema.replace("\"", "\"\"") + "\".\""
                        + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                try (java.sql.Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Dropped sequence " + obj.getName());
                    reload();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not drop sequence " + obj.getName() + ": " + msg)).showAndWait());
            }
        });
    }

    private void confirmAndDropGenericObject(ConnectionProfile profile, DbObject obj) {
        if (profile == null || obj == null) return;
        String typeLabel = switch (obj.getKind()) {
            case ACCESS_METHOD -> "access method";
            case CAST -> "cast";
            case EXTENSION -> "extension";
            case LANGUAGE -> "language";
            case VIRTUAL_VIEW -> "virtual view";
            case COLUMN -> "column";
            case KEY -> "key";
            case FOREIGN_KEY -> "foreign key";
            case INDEX -> "index";
            case PROCEDURE -> "procedure";
            case FUNCTION -> "function";
            case TABLESPACE -> "tablespace";
            case DATA_FILE -> "data file";
            case USER -> "user";
            default -> "object";
        };
        Alert warn = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                "This permanently drops " + typeLabel + " \"" + obj.getName() + "\". This cannot be undone.",
                ButtonType.YES, ButtonType.NO));
        warn.setTitle("Drop " + Character.toUpperCase(typeLabel.charAt(0)) + typeLabel.substring(1));
        warn.setHeaderText("Drop " + typeLabel + " \"" + obj.getName() + "\"?");
        warn.initOwner(getScene() == null ? null : getScene().getWindow());
        if (warn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        if (obj.getKind() == Kind.VIRTUAL_VIEW) {
            mainWindow.setStatus("Dropped virtual view " + obj.getName());
            reload();
            return;
        }

        AppExecutor.run(() -> {
            try {
                String sql = switch (obj.getKind()) {
                    case ACCESS_METHOD -> "DROP ACCESS METHOD \"" + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                    case EXTENSION -> "DROP EXTENSION \"" + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                    case LANGUAGE -> "DROP LANGUAGE \"" + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                    case CAST -> "DROP CAST (" + obj.getName() + ") CASCADE";
                    case TABLESPACE -> (profile.getType() == ConnectionProfile.DatabaseType.ORACLE)
                            ? "DROP TABLESPACE \"" + obj.getName().replace("\"", "\"\"") + "\" INCLUDING CONTENTS AND DATAFILES CASCADE CONSTRAINTS"
                            : "DROP TABLESPACE \"" + obj.getName().replace("\"", "\"\"") + "\"";
                    case USER -> {
                        if (profile.getType() == ConnectionProfile.DatabaseType.ORACLE) {
                            yield "DROP USER \"" + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                        }
                        String u = obj.getName();
                        if (u.contains("@")) {
                            int at = u.indexOf('@');
                            String uname = u.substring(0, at);
                            String host = u.substring(at + 1);
                            yield "DROP USER '" + uname.replace("'", "''") + "'@'" + host.replace("'", "''") + "'";
                        } else {
                            yield "DROP USER '" + u.replace("'", "''") + "'";
                        }
                    }
                    case DATA_FILE -> (profile.getType() == ConnectionProfile.DatabaseType.ORACLE)
                            ? "ALTER DATABASE DATAFILE '" + obj.getName() + "' OFFLINE DROP"
                            : "DROP DATAFILE '" + obj.getName() + "'";
                    case COLUMN -> {
                        String tbl = obj.getTableName() != null ? obj.getTableName() : "table_name";
                        if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                            yield "ALTER TABLE \"" + tbl.replace("\"", "\"\"") + "\" DROP COLUMN \"" + obj.getName().replace("\"", "\"\"") + "\"";
                        }
                        if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL || profile.getType() == ConnectionProfile.DatabaseType.MARIADB) {
                            String cat = obj.getCatalog() != null ? obj.getCatalog() : (obj.getSchema() != null ? obj.getSchema() : "");
                            if (!cat.isEmpty()) {
                                yield "ALTER TABLE `" + cat.replace("`", "``") + "`.`" + tbl.replace("`", "``") + "` DROP COLUMN `" + obj.getName().replace("`", "``") + "`";
                            } else {
                                yield "ALTER TABLE `" + tbl.replace("`", "``") + "` DROP COLUMN `" + obj.getName().replace("`", "``") + "`";
                            }
                        }
                        if (profile.getType() == ConnectionProfile.DatabaseType.ORACLE) {
                            String sch = obj.getSchema() != null ? obj.getSchema() : (obj.getCatalog() != null ? obj.getCatalog() : "");
                            if (!sch.isEmpty()) {
                                yield "ALTER TABLE \"" + sch.replace("\"", "\"\"") + "\".\"" + tbl.replace("\"", "\"\"") + "\" DROP COLUMN \"" + obj.getName().replace("\"", "\"\"") + "\"";
                            } else {
                                yield "ALTER TABLE \"" + tbl.replace("\"", "\"\"") + "\" DROP COLUMN \"" + obj.getName().replace("\"", "\"\"") + "\"";
                            }
                        }
                        String sch = obj.getSchema() != null ? obj.getSchema() : "public";
                        yield "ALTER TABLE \"" + sch.replace("\"", "\"\"") + "\".\"" + tbl.replace("\"", "\"\"") + "\" DROP COLUMN \"" + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                    }
                    case KEY, FOREIGN_KEY -> {
                        String tbl = obj.getTableName() != null ? obj.getTableName() : "table_name";
                        if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                            yield "ALTER TABLE \"" + tbl.replace("\"", "\"\"") + "\" DROP CONSTRAINT \"" + obj.getName().replace("\"", "\"\"") + "\"";
                        }
                        String sch = obj.getSchema() != null ? obj.getSchema() : "public";
                        yield "ALTER TABLE \"" + sch.replace("\"", "\"\"") + "\".\"" + tbl.replace("\"", "\"\"") + "\" DROP CONSTRAINT \"" + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                    }
                    case INDEX -> {
                        if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                            yield "DROP INDEX \"" + obj.getName().replace("\"", "\"\"") + "\"";
                        }
                        String sch = obj.getSchema() != null ? obj.getSchema() : "public";
                        yield "DROP INDEX \"" + sch.replace("\"", "\"\"") + "\".\"" + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                    }
                    case PROCEDURE -> {
                        if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                            yield "DROP PROCEDURE \"" + obj.getName().replace("\"", "\"\"") + "\"";
                        }
                        String sch = obj.getSchema() != null ? obj.getSchema() : "public";
                        yield "DROP PROCEDURE \"" + sch.replace("\"", "\"\"") + "\".\"" + obj.getName().replace("\"", "\"\"") + "\"";
                    }
                    case FUNCTION -> {
                        if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                            yield "DROP FUNCTION \"" + obj.getName().replace("\"", "\"\"") + "\"";
                        }
                        String sch = obj.getSchema() != null ? obj.getSchema() : "public";
                        yield "DROP FUNCTION \"" + sch.replace("\"", "\"\"") + "\".\"" + obj.getName().replace("\"", "\"\"") + "\"";
                    }
                    default -> "DROP " + obj.getKind() + " \"" + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                };
                try (java.sql.Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Dropped " + typeLabel + " " + obj.getName());
                    reload();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not drop " + typeLabel + " " + obj.getName() + ": " + msg)).showAndWait());
            }
        });
    }

    private void confirmAndDeleteSchema(ConnectionProfile profile, DbObject obj) {
        if (profile.getType() == ConnectionProfile.DatabaseType.SQLITE) return;
        Alert warn = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                "This permanently deletes the schema \"" + obj.getName() + "\" and everything in it. "
                        + "This cannot be undone.",
                ButtonType.YES, ButtonType.NO));
        warn.setTitle("Drop Schema");
        warn.setHeaderText("Drop schema \"" + obj.getName() + "\"?");
        warn.initOwner(getScene() == null ? null : getScene().getWindow());
        if (warn.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        TextInputDialog confirm = (TextInputDialog) DialogTheme.apply(new TextInputDialog());
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText(null);
        confirm.setContentText("Type the schema name \"" + obj.getName() + "\" to confirm:");
        confirm.initOwner(getScene() == null ? null : getScene().getWindow());
        confirm.getDialogPane().setPrefWidth(420);

        String typed = confirm.showAndWait().orElse("");
        if (!typed.equals(obj.getName())) {
            if (!typed.isEmpty()) {
                DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION, "Name didn't match — deletion cancelled."))
                        .showAndWait();
            }
            return;
        }

        AppExecutor.run(() -> {
            try {
                String sql = switch (profile.getType()) {
                    case POSTGRESQL -> "DROP SCHEMA \"" + obj.getName().replace("\"", "\"\"") + "\" CASCADE";
                    case MYSQL, MARIADB -> "DROP DATABASE `" + obj.getName().replace("`", "``") + "`";
                    default -> "DROP SCHEMA " + obj.getName();
                };
                try (java.sql.Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Dropped schema " + obj.getName());
                    reload();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not drop schema " + obj.getName() + ": " + msg)).showAndWait());
            }
        });
    }

    // ------------------------------------------------------------------ toolbar & actions

    private Button createToolButton(javafx.scene.Node graphic, String tooltip,
                                    javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button();
        btn.setGraphic(graphic);
        btn.setTooltip(new Tooltip(tooltip));
        btn.getStyleClass().add("schema-tool-button");
        btn.setOnAction(handler);
        return btn;
    }

    public void updateToolbarState(TreeItem<DbObject> selectedItem) {
        if (selectedItem == null || selectedItem.getValue() == null) {
            propertiesButton.setDisable(true);
            jumpToConsoleButton.setDisable(true);
            goToDdlButton.setDisable(true);
            editDataButton.setDisable(true);
            return;
        }

        DbObject obj = selectedItem.getValue();
        ConnectionProfile profile = profileFor(selectedItem);

        if (profile == null || obj.getKind() == Kind.MESSAGE) {
            propertiesButton.setDisable(true);
            jumpToConsoleButton.setDisable(true);
            goToDdlButton.setDisable(true);
            editDataButton.setDisable(true);
            return;
        }

        propertiesButton.setDisable(false);

        // 1. Jump to Query Console... (Ctrl+Shift+F10):
        // Active for any real object belonging to a connection profile
        jumpToConsoleButton.setDisable(false);

        // 2. Go to DDL:
        // Active for DDL-bearing schema objects: Database, Schema, Table, View, Partition, Procedure, Function, Sequence, Index, Role, Tablespace,
        // and database objects: Access Method, Cast, Extension, Language, Virtual View
        // Inactive for: Connection root, category folders (tables, views, routines, sequences, etc.)
        boolean hasDdl = switch (obj.getKind()) {
            case DATABASE, SCHEMA, TABLE, VIEW, PARTITION, PROCEDURE, FUNCTION, SEQUENCE, INDEX,
                 ROLE, TABLESPACE,
                 ACCESS_METHOD, CAST, EXTENSION, LANGUAGE, VIRTUAL_VIEW,
                 COLLATION, LINKED_SERVER, SERVER_PRINCIPAL,
                 ASSEMBLY, FILEGROUP, DATABASE_PRINCIPAL,
                 USER, DATA_FILE -> true;
            default -> false;
        };
        goToDdlButton.setDisable(!hasDdl);

        // 3. Edit Data (F4):
        // Active ONLY for data-bearing objects (Table, View, Partition, Mongo Collection, Virtual View)
        // Inactive for: Database, Schema, Folders, Routines, Sequences, Connections, etc.
        boolean hasData = switch (obj.getKind()) {
            case TABLE, VIEW, PARTITION, COLLECTION, VIRTUAL_VIEW -> true;
            default -> false;
        };
        editDataButton.setDisable(!hasData);
    }

    public void jumpToQueryConsole() {
        TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
        ConnectionProfile profile = item != null ? profileFor(item) : null;
        if (profile == null) {
            mainWindow.openConsoleForSelectedConnection();
            return;
        }

        DbObject obj = item.getValue();
        String catalog = resolveCatalogFor(item, obj);

        // Check if an existing console tab is open for this profile/catalog
        for (TabPane pane : mainWindow.editorTabPanes()) {
            for (Tab tab : pane.getTabs()) {
                if (tab instanceof QueryTab qt) {
                    if (qt.getProfile().getId().equals(profile.getId())) {
                        if (catalog == null || catalog.equalsIgnoreCase(qt.getCatalog())) {
                            mainWindow.selectTab(qt);
                            Platform.runLater(qt::focusEditor);
                            mainWindow.setStatus("Jumped to Query Console for " + profile.getName());
                            return;
                        }
                    }
                } else if (tab instanceof MongoConsoleTab mt) {
                    if (mt.getProfile().getId().equals(profile.getId())) {
                        mainWindow.selectTab(mt);
                        Platform.runLater(mt::focusEditor);
                        mainWindow.setStatus("Jumped to Mongo Console for " + profile.getName());
                        return;
                    }
                }
            }
        }

        // None open: open new query console
        mainWindow.openQueryTab(profile, catalog, null);
        mainWindow.setStatus("Opened Query Console for " + profile.getName());
    }

    public void goToDdl() {
        TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
        if (item == null) return;
        DbObject obj = item.getValue();
        ConnectionProfile profile = profileFor(item);
        if (profile == null || obj == null) return;

        mainWindow.openDdlTab(profile, obj);
    }

    public void editData() {
        TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
        if (item == null) return;
        DbObject obj = item.getValue();
        ConnectionProfile profile = profileFor(item);
        if (profile == null || obj == null) return;

        if (obj.getKind() == Kind.COLLECTION) {
            mainWindow.openMongoTab(profile, obj);
        } else {
            mainWindow.openDataTab(profile, obj);
        }
    }

    private void openSelectedProperties() {
        TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
        ConnectionProfile profile = item != null ? profileFor(item) : null;
        if (profile != null) {
            mainWindow.showEditConnectionDialog(profile);
        } else {
            mainWindow.showNewConnectionDialog();
        }
    }

    private void refreshSelectedOrAll() {
        TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
        if (item != null && item.getValue() != null && item.getValue().getKind() != Kind.MESSAGE) {
            DbObject obj = item.getValue();
            if (obj.getKind() == Kind.CONNECTION) {
                ConnectionProfile p = connectionItems.get(item);
                if (p != null) refreshConnection(item, p);
            } else {
                refreshNode(item, obj, obj.getName());
            }
        } else {
            reload();
        }
    }

    private String resolveCatalogFor(TreeItem<DbObject> item, DbObject obj) {
        return resolveCatalogFor(item, obj, null);
    }

    private String resolveCatalogFor(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        if (obj != null && (obj.getKind() == Kind.DATABASE || obj.getKind() == Kind.SCHEMA)) {
            return obj.getCatalog() != null && !obj.getCatalog().isBlank() ? obj.getCatalog() : obj.getName();
        }
        if (obj != null && obj.getCatalog() != null && !obj.getCatalog().isBlank()) {
            return obj.getCatalog();
        }
        TreeItem<DbObject> curr = item != null ? item.getParent() : null;
        while (curr != null) {
            if (curr.getValue() != null && (curr.getValue().getKind() == Kind.DATABASE || curr.getValue().getKind() == Kind.SCHEMA)) {
                return curr.getValue().getCatalog() != null && !curr.getValue().getCatalog().isBlank()
                        ? curr.getValue().getCatalog() : curr.getValue().getName();
            }
            curr = curr.getParent();
        }
        ConnectionProfile p = profile != null ? profile : profileFor(item);
        return p != null ? p.getDatabase() : null;
    }

    private String resolveSchemaFor(TreeItem<DbObject> item, DbObject obj) {
        return resolveSchemaFor(item, obj, null);
    }

    private String resolveSchemaFor(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        if (obj != null && obj.getKind() == Kind.SCHEMA) {
            return obj.getName();
        }
        if (obj != null && obj.getSchema() != null && !obj.getSchema().isBlank()) {
            return obj.getSchema();
        }
        TreeItem<DbObject> curr = item != null ? item.getParent() : null;
        while (curr != null) {
            if (curr.getValue() != null && curr.getValue().getKind() == Kind.SCHEMA) {
                return curr.getValue().getName();
            }
            if (curr.getValue() != null && curr.getValue().getSchema() != null && !curr.getValue().getSchema().isBlank()) {
                return curr.getValue().getSchema();
            }
            curr = curr.getParent();
        }
        ConnectionProfile p = profile != null ? profile : profileFor(item);
        if (p != null && p.getType() == ConnectionProfile.DatabaseType.SQLSERVER) {
            return "dbo";
        }
        if (p != null && p.getType() == ConnectionProfile.DatabaseType.ORACLE) {
            return p.getUsername() != null && !p.getUsername().isBlank() ? p.getUsername().toUpperCase() : "HELLO";
        }
        return "public";
    }

    private DbObject resolveParentTableFor(TreeItem<DbObject> item, DbObject obj) {
        if (obj != null && (obj.getKind() == Kind.TABLE || obj.getKind() == Kind.VIEW || obj.getKind() == Kind.PARTITION || obj.getKind() == Kind.COLLECTION)) {
            return obj;
        }
        TreeItem<DbObject> curr = item != null ? item.getParent() : null;
        while (curr != null) {
            if (curr.getValue() != null && (curr.getValue().getKind() == Kind.TABLE
                    || curr.getValue().getKind() == Kind.VIEW
                    || curr.getValue().getKind() == Kind.PARTITION
                    || curr.getValue().getKind() == Kind.COLLECTION)) {
                return curr.getValue();
            }
            curr = curr.getParent();
        }
        if (obj != null && obj.getTableName() != null && !obj.getTableName().isBlank()) {
            return new DbObject(obj.getTableName(), Kind.TABLE, obj.getCatalog(), obj.getSchema());
        }
        return null;
    }

    public ConnectionProfile getSelectedProfile() {
        TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
        return item != null ? profileFor(item) : null;
    }

    public DbObject getSelectedDbObject() {
        TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
        return item != null ? item.getValue() : null;
    }

    public TreeItem<DbObject> getSelectedItem() {
        return tree.getSelectionModel().getSelectedItem();
    }

    public Button getJumpToConsoleButton() {
        return jumpToConsoleButton;
    }

    public Button getGoToDdlButton() {
        return goToDdlButton;
    }

    public Button getEditDataButton() {
        return editDataButton;
    }

    public Button getPropertiesButton() {
        return propertiesButton;
    }

    public TreeView<DbObject> getTree() {
        return tree;
    }

    public Button getScrollFromEditorButton() {
        return scrollFromEditorButton;
    }

    public Button getCollapseAllButton() {
        return collapseAllButton;
    }

    /**
     * "Scroll from Editor": locates the active editor tab's
     * table/schema/database object in the Database Explorer tree, asynchronously
     * expanding intermediate parent nodes as needed, selects it, scrolls it into
     * view, and updates the explorer toolbar state (Jump to Console, Go to DDL, Edit Data).
     */
    public void scrollFromEditor() {
        Tab tab = mainWindow != null ? mainWindow.currentSelectedTab() : null;
        if (tab == null) {
            if (mainWindow != null) mainWindow.setStatus("No active database editor to locate");
            return;
        }

        ConnectionProfile profile = null;
        DbObject targetObject = null;

        if (tab instanceof DataTab dt) {
            profile = dt.getProfileForReopen();
            targetObject = dt.getTableForReopen();
        } else if (tab instanceof DdlTab ddlTab) {
            profile = ddlTab.getProfile();
            targetObject = ddlTab.getObject();
        } else if (tab instanceof StructureTab st) {
            profile = st.getProfile();
            targetObject = st.getTable();
        } else if (tab instanceof MongoCollectionTab mt) {
            profile = mt.getProfileForReopen();
            targetObject = mt.getCollectionForReopen();
        } else if (tab instanceof QueryTab qt) {
            profile = qt.getProfile();
            if (profile != null && qt.getCatalog() != null && !qt.getCatalog().isBlank()) {
                targetObject = new DbObject(qt.getCatalog(), Kind.DATABASE, qt.getCatalog(), null);
            }
        } else if (tab instanceof MongoConsoleTab mt) {
            profile = mt.getProfile();
        }

        if (profile == null) {
            if (mainWindow != null) mainWindow.setStatus("No active database editor to locate");
            return;
        }

        selectObjectInTree(profile, targetObject);
    }

    /**
     * Traverses down the connection tree to locate, expand, and select the given target object.
     * Handles lazy-loaded parent nodes gracefully at every level.
     */
    public void selectObjectInTree(ConnectionProfile profile, DbObject target) {
        if (profile == null) return;

        TreeItem<DbObject> connItem = findConnectionItem(profile);
        if (connItem == null) {
            if (mainWindow != null) mainWindow.setStatus("Connection " + profile.getName() + " not found in Explorer");
            return;
        }

        if (target == null || target.getKind() == Kind.CONNECTION) {
            highlightAndSelect(connItem);
            return;
        }

        expandNodeAndThen(connItem, () -> navigateFromConnection(connItem, profile, target));
    }

    public TreeItem<DbObject> findConnectionItem(ConnectionProfile profile) {
        if (profile == null) return null;
        for (Map.Entry<TreeItem<DbObject>, ConnectionProfile> entry : connectionItems.entrySet()) {
            if (entry.getValue() != null && profile.getId() != null && profile.getId().equals(entry.getValue().getId())) {
                return entry.getKey();
            }
        }
        for (Map.Entry<TreeItem<DbObject>, ConnectionProfile> entry : connectionItems.entrySet()) {
            if (entry.getValue() != null && profile.getName() != null && profile.getName().equalsIgnoreCase(entry.getValue().getName())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void navigateFromConnection(TreeItem<DbObject> connItem, ConnectionProfile profile, DbObject target) {
        // 0. Check for Server Objects navigation (roles, tablespaces, collations, linked servers, server principals)
        if (target.getKind() == Kind.SERVER_OBJECTS
                || target.getKind() == Kind.ROLES_FOLDER
                || target.getKind() == Kind.ROLE
                || target.getKind() == Kind.TABLESPACES_FOLDER
                || target.getKind() == Kind.TABLESPACE
                || target.getKind() == Kind.COLLATIONS_FOLDER
                || target.getKind() == Kind.COLLATION
                || target.getKind() == Kind.LINKED_SERVERS_FOLDER
                || target.getKind() == Kind.LINKED_SERVER
                || target.getKind() == Kind.SERVER_PRINCIPALS_FOLDER
                || target.getKind() == Kind.SERVER_PRINCIPAL
                || target.getKind() == Kind.USERS_FOLDER
                || target.getKind() == Kind.USER
                || target.getKind() == Kind.DATA_FILE) {
            TreeItem<DbObject> serverObjItem = connItem.getChildren().stream()
                    .filter(c -> c.getValue() != null && c.getValue().getKind() == Kind.SERVER_OBJECTS)
                    .findFirst().orElse(null);
            if (serverObjItem != null) {
                if (target.getKind() == Kind.SERVER_OBJECTS) {
                    highlightAndSelect(serverObjItem);
                    return;
                }
                expandNodeAndThen(serverObjItem, () -> navigateFromServerObjects(serverObjItem, target));
                return;
            }
        }

        // 0.1 Check for virtual views navigation directly under connection (e.g. MySQL, MariaDB, Oracle)
        if (target.getKind() == Kind.VIRTUAL_VIEWS_FOLDER || target.getKind() == Kind.VIRTUAL_VIEW) {
            TreeItem<DbObject> vvFolder = connItem.getChildren().stream()
                    .filter(c -> c.getValue() != null && c.getValue().getKind() == Kind.VIRTUAL_VIEWS_FOLDER)
                    .findFirst().orElse(null);
            if (vvFolder != null) {
                navigateFromVirtualViews(connItem, target);
                return;
            }
        }

        // 1. Look for matching DATABASE nodes
        List<TreeItem<DbObject>> dbItems = connItem.getChildren().stream()
                .filter(c -> c.getValue() != null && c.getValue().getKind() == Kind.DATABASE)
                .toList();

        if (!dbItems.isEmpty()) {
            String targetCatalog = target.getCatalog();
            if (targetCatalog == null || targetCatalog.isBlank()) {
                targetCatalog = profile.getDatabase();
            }

            TreeItem<DbObject> targetDb = null;
            if (targetCatalog != null) {
                for (TreeItem<DbObject> db : dbItems) {
                    if (targetCatalog.equalsIgnoreCase(db.getValue().getName())) {
                        targetDb = db;
                        break;
                    }
                }
            }
            if (targetDb == null && dbItems.size() == 1) {
                targetDb = dbItems.get(0);
            }
            if (targetDb == null) {
                targetDb = dbItems.get(0);
            }

            if (target.getKind() == Kind.DATABASE) {
                highlightAndSelect(targetDb);
                return;
            }

            TreeItem<DbObject> matchedDb = targetDb;
            expandNodeAndThen(matchedDb, () -> navigateFromDatabase(matchedDb, profile, target));
            return;
        }

        // 2. Look for matching SCHEMA nodes directly under connection (e.g. Oracle)
        List<TreeItem<DbObject>> schemaItems = connItem.getChildren().stream()
                .filter(c -> c.getValue() != null && c.getValue().getKind() == Kind.SCHEMA)
                .toList();

        if (!schemaItems.isEmpty()) {
            TreeItem<DbObject> targetSchema = findSchemaItem(schemaItems, target.getSchema());
            if (target.getKind() == Kind.SCHEMA) {
                if (targetSchema != null) highlightAndSelect(targetSchema);
                else highlightAndSelect(connItem);
                return;
            }
            if (targetSchema != null) {
                TreeItem<DbObject> matchedSchema = targetSchema;
                expandNodeAndThen(matchedSchema, () -> navigateFromFolders(matchedSchema, profile, target));
                return;
            }
        }

        // 3. Directly folder nodes under connection (e.g. SQLite)
        navigateFromFolders(connItem, profile, target);
    }

    private void navigateFromServerObjects(TreeItem<DbObject> serverObjItem, DbObject target) {
        Kind folderKind = switch (target.getKind()) {
            case ROLES_FOLDER, ROLE -> Kind.ROLES_FOLDER;
            case TABLESPACES_FOLDER, TABLESPACE, DATA_FILE -> Kind.TABLESPACES_FOLDER;
            case COLLATIONS_FOLDER, COLLATION -> Kind.COLLATIONS_FOLDER;
            case LINKED_SERVERS_FOLDER, LINKED_SERVER -> Kind.LINKED_SERVERS_FOLDER;
            case SERVER_PRINCIPALS_FOLDER, SERVER_PRINCIPAL -> Kind.SERVER_PRINCIPALS_FOLDER;
            case USERS_FOLDER, USER -> Kind.USERS_FOLDER;
            default -> null;
        };
        if (folderKind == null) {
            highlightAndSelect(serverObjItem);
            return;
        }
        TreeItem<DbObject> folder = serverObjItem.getChildren().stream()
                .filter(c -> c.getValue() != null && c.getValue().getKind() == folderKind)
                .findFirst().orElse(null);
        if (folder != null) {
            if (target.getKind() == folderKind) {
                highlightAndSelect(folder);
                return;
            }
            if (target.getKind() == Kind.DATA_FILE) {
                expandNodeAndThen(folder, () -> {
                    for (TreeItem<DbObject> tsItem : folder.getChildren()) {
                        expandNodeAndThen(tsItem, () -> {
                            for (TreeItem<DbObject> dfItem : tsItem.getChildren()) {
                                if (dfItem.getValue() != null && dfItem.getValue().getName().equalsIgnoreCase(target.getName())) {
                                    highlightAndSelect(dfItem);
                                    return;
                                }
                            }
                        });
                    }
                });
                return;
            }
            expandNodeAndThen(folder, () -> {
                for (TreeItem<DbObject> child : folder.getChildren()) {
                    if (child.getValue() != null && child.getValue().getName().equalsIgnoreCase(target.getName())) {
                        highlightAndSelect(child);
                        return;
                    }
                }
            });
        }
    }

    private static boolean isDatabaseObjectsKind(Kind kind) {
        return kind == Kind.DATABASE_OBJECTS
                || kind == Kind.ACCESS_METHODS_FOLDER || kind == Kind.ACCESS_METHOD
                || kind == Kind.CASTS_FOLDER || kind == Kind.CAST
                || kind == Kind.EXTENSIONS_FOLDER || kind == Kind.EXTENSION
                || kind == Kind.LANGUAGES_FOLDER || kind == Kind.LANGUAGE
                || kind == Kind.ASSEMBLIES_FOLDER || kind == Kind.ASSEMBLY
                || kind == Kind.FILEGROUPS_FOLDER || kind == Kind.FILEGROUP
                || kind == Kind.DATABASE_PRINCIPALS_FOLDER || kind == Kind.DATABASE_PRINCIPAL;
    }

    private void navigateFromDatabaseObjects(TreeItem<DbObject> dbItem, DbObject target) {
        TreeItem<DbObject> dbObjFolder = dbItem.getChildren().stream()
                .filter(c -> c.getValue() != null && c.getValue().getKind() == Kind.DATABASE_OBJECTS)
                .findFirst().orElse(null);
        if (dbObjFolder == null) {
            highlightAndSelect(dbItem);
            return;
        }
        if (target.getKind() == Kind.DATABASE_OBJECTS) {
            highlightAndSelect(dbObjFolder);
            return;
        }
        Kind subfolderKind = switch (target.getKind()) {
            case ACCESS_METHODS_FOLDER, ACCESS_METHOD -> Kind.ACCESS_METHODS_FOLDER;
            case CASTS_FOLDER, CAST -> Kind.CASTS_FOLDER;
            case EXTENSIONS_FOLDER, EXTENSION -> Kind.EXTENSIONS_FOLDER;
            case LANGUAGES_FOLDER, LANGUAGE -> Kind.LANGUAGES_FOLDER;
            case ASSEMBLIES_FOLDER, ASSEMBLY -> Kind.ASSEMBLIES_FOLDER;
            case FILEGROUPS_FOLDER, FILEGROUP -> Kind.FILEGROUPS_FOLDER;
            case DATABASE_PRINCIPALS_FOLDER, DATABASE_PRINCIPAL -> Kind.DATABASE_PRINCIPALS_FOLDER;
            default -> null;
        };
        if (subfolderKind == null) {
            highlightAndSelect(dbObjFolder);
            return;
        }

        expandNodeAndThen(dbObjFolder, () -> {
            TreeItem<DbObject> subfolder = dbObjFolder.getChildren().stream()
                    .filter(c -> c.getValue() != null && c.getValue().getKind() == subfolderKind)
                    .findFirst().orElse(null);
            if (subfolder == null) {
                highlightAndSelect(dbObjFolder);
                return;
            }
            if (target.getKind() == subfolderKind) {
                highlightAndSelect(subfolder);
                return;
            }
            expandNodeAndThen(subfolder, () -> {
                for (TreeItem<DbObject> child : subfolder.getChildren()) {
                    if (child.getValue() != null && child.getValue().getName().equalsIgnoreCase(target.getName())) {
                        highlightAndSelect(child);
                        return;
                    }
                }
                highlightAndSelect(subfolder);
            });
        });
    }

    private void navigateFromVirtualViews(TreeItem<DbObject> dbItem, DbObject target) {
        TreeItem<DbObject> vvFolder = dbItem.getChildren().stream()
                .filter(c -> c.getValue() != null && c.getValue().getKind() == Kind.VIRTUAL_VIEWS_FOLDER)
                .findFirst().orElse(null);
        if (vvFolder == null) {
            highlightAndSelect(dbItem);
            return;
        }
        if (target.getKind() == Kind.VIRTUAL_VIEWS_FOLDER) {
            highlightAndSelect(vvFolder);
            return;
        }
        expandNodeAndThen(vvFolder, () -> {
            for (TreeItem<DbObject> child : vvFolder.getChildren()) {
                if (child.getValue() != null && child.getValue().getName().equalsIgnoreCase(target.getName())) {
                    highlightAndSelect(child);
                    return;
                }
            }
            highlightAndSelect(vvFolder);
        });
    }

    private void navigateFromDatabase(TreeItem<DbObject> dbItem, ConnectionProfile profile, DbObject target) {
        if (isDatabaseObjectsKind(target.getKind())) {
            navigateFromDatabaseObjects(dbItem, target);
            return;
        }
        if (target.getKind() == Kind.VIRTUAL_VIEWS_FOLDER || target.getKind() == Kind.VIRTUAL_VIEW) {
            navigateFromVirtualViews(dbItem, target);
            return;
        }

        // Under database: check if there are SCHEMA nodes (PostgreSQL, SQL Server)
        List<TreeItem<DbObject>> schemaItems = dbItem.getChildren().stream()
                .filter(c -> c.getValue() != null && c.getValue().getKind() == Kind.SCHEMA)
                .toList();

        if (!schemaItems.isEmpty()) {
            TreeItem<DbObject> targetSchema = findSchemaItem(schemaItems, target.getSchema());
            if (target.getKind() == Kind.SCHEMA) {
                if (targetSchema != null) highlightAndSelect(targetSchema);
                else highlightAndSelect(dbItem);
                return;
            }
            if (targetSchema != null) {
                TreeItem<DbObject> matchedSchema = targetSchema;
                expandNodeAndThen(matchedSchema, () -> navigateFromFolders(matchedSchema, profile, target));
                return;
            }
        }

        // Otherwise database directly contains folders (MySQL, MongoDB)
        navigateFromFolders(dbItem, profile, target);
    }

    private void navigateFromFolders(TreeItem<DbObject> parentItem, ConnectionProfile profile, DbObject target) {
        Kind folderKind = folderKindFor(target.getKind());
        TreeItem<DbObject> folderItem = null;
        for (TreeItem<DbObject> c : parentItem.getChildren()) {
            if (c.getValue() != null) {
                if (c.getValue().getKind() == folderKind || isMatchingFolderName(c.getValue(), target.getKind())) {
                    folderItem = c;
                    break;
                }
            }
        }

        if (folderItem == null) {
            TreeItem<DbObject> direct = findChildByName(parentItem, target.getName());
            if (direct != null) {
                highlightAndSelect(direct);
            } else {
                highlightAndSelect(parentItem);
            }
            return;
        }

        if (target.getKind() == folderKind) {
            highlightAndSelect(folderItem);
            return;
        }

        TreeItem<DbObject> matchedFolder = folderItem;
        expandNodeAndThen(matchedFolder, () -> {
            TreeItem<DbObject> targetItem = findChildByName(matchedFolder, target.getName());
            if (targetItem != null) {
                highlightAndSelect(targetItem);
            } else {
                highlightAndSelect(matchedFolder);
            }
        });
    }

    private Kind folderKindFor(Kind kind) {
        return switch (kind) {
            case TABLE, PARTITION -> Kind.TABLES_FOLDER;
            case VIEW -> Kind.VIEWS_FOLDER;
            case COLLECTION -> Kind.COLLECTIONS_FOLDER;
            case PROCEDURE -> Kind.PROCEDURES_FOLDER;
            case FUNCTION -> Kind.FUNCTIONS_FOLDER;
            case SEQUENCE -> Kind.SEQUENCES_FOLDER;
            default -> Kind.TABLES_FOLDER;
        };
    }

    private boolean isMatchingFolderName(DbObject folderObj, Kind targetKind) {
        if (folderObj == null || folderObj.getName() == null) return false;
        String name = folderObj.getName().toLowerCase();
        return switch (targetKind) {
            case TABLE, PARTITION -> name.contains("table");
            case VIEW -> name.contains("view");
            case COLLECTION -> name.contains("collection");
            case PROCEDURE -> name.contains("procedure");
            case FUNCTION -> name.contains("function");
            case SEQUENCE -> name.contains("sequence");
            default -> false;
        };
    }

    private TreeItem<DbObject> findChildByName(TreeItem<DbObject> parent, String name) {
        if (parent == null || name == null) return null;
        for (TreeItem<DbObject> child : parent.getChildren()) {
            if (child.getValue() != null && name.equalsIgnoreCase(child.getValue().getName())) {
                return child;
            }
        }
        return null;
    }

    private TreeItem<DbObject> findSchemaItem(List<TreeItem<DbObject>> schemaItems, String schemaName) {
        if (schemaItems == null || schemaItems.isEmpty()) return null;
        if (schemaName != null && !schemaName.isBlank()) {
            for (TreeItem<DbObject> s : schemaItems) {
                if (schemaName.equalsIgnoreCase(s.getValue().getName())) {
                    return s;
                }
            }
        }
        for (TreeItem<DbObject> s : schemaItems) {
            if ("public".equalsIgnoreCase(s.getValue().getName())) {
                return s;
            }
        }
        return schemaItems.get(0);
    }

    private boolean isAlreadyPopulated(TreeItem<DbObject> item) {
        if (item == null) return false;
        if (item.getValue() != null && item.getValue().isLoaded()) return true;
        if (!item.getChildren().isEmpty()) {
            TreeItem<DbObject> first = item.getChildren().get(0);
            if (first.getValue() != null && first.getValue().getKind() == Kind.MESSAGE && "Loading…".equals(first.getValue().getName())) {
                return false;
            }
            return true;
        }
        return false;
    }

    private void expandNodeAndThen(TreeItem<DbObject> item, Runnable continuation) {
        if (item == null) return;
        DbObject obj = item.getValue();
        if (obj == null) return;

        boolean populated = isAlreadyPopulated(item);
        if (item.isExpanded() && populated) {
            continuation.run();
            return;
        }

        if (populated) {
            obj.setLoaded(true);
            item.setExpanded(true);
            continuation.run();
            return;
        }

        pendingLoadCallbacks.put(item, continuation);
        item.setExpanded(true);
    }

    public void highlightAndSelect(TreeItem<DbObject> item) {
        if (item == null) return;
        tree.getSelectionModel().select(item);
        updateToolbarState(item);
        tree.requestFocus();
        if (mainWindow != null && item.getValue() != null) {
            mainWindow.setStatus("Selected " + item.getValue().getName() + " in Database Explorer");
        }
        Platform.runLater(() -> {
            int row = tree.getRow(item);
            if (row >= 0) {
                tree.scrollTo(row);
            }
        });
    }

    /**
     * "Collapse All": collapses all expanded nodes in the tree.
     * Recursively collapses all connection nodes and their descendants.
     * If an item inside a connection was selected, gracefully adjusts selection
     * to the connection node or top-level visible ancestor so selection is preserved.
     */
    public void collapseAll() {
        pendingLoadCallbacks.clear();

        TreeItem<DbObject> selected = tree.getSelectionModel().getSelectedItem();
        TreeItem<DbObject> targetSelection = null;
        if (selected != null && selected != root) {
            // Find top-level connection node under root
            TreeItem<DbObject> curr = selected;
            while (curr.getParent() != null && curr.getParent() != root) {
                curr = curr.getParent();
            }
            targetSelection = curr;
        }

        for (TreeItem<DbObject> child : root.getChildren()) {
            collapseNodeRecursively(child);
        }

        if (targetSelection != null) {
            tree.getSelectionModel().select(targetSelection);
            updateToolbarState(targetSelection);
        } else if (!root.getChildren().isEmpty()) {
            TreeItem<DbObject> first = root.getChildren().get(0);
            tree.getSelectionModel().select(first);
            updateToolbarState(first);
        } else {
            updateToolbarState(null);
        }

        saveTreeState();
    }

    private void collapseNodeRecursively(TreeItem<DbObject> node) {
        if (node == null) return;
        for (TreeItem<DbObject> child : node.getChildren()) {
            collapseNodeRecursively(child);
        }
        node.setExpanded(false);
    }

    public static String buildBreadcrumb(TreeItem<DbObject> item) {
        if (item == null || item.getValue() == null) return "";
        List<String> segments = new ArrayList<>();
        segments.add("Database");
        List<TreeItem<DbObject>> chain = new ArrayList<>();
        TreeItem<DbObject> curr = item;
        while (curr != null && curr.getValue() != null && curr.getParent() != null) {
            chain.add(0, curr);
            curr = curr.getParent();
        }
        for (TreeItem<DbObject> node : chain) {
            DbObject obj = node.getValue();
            if (obj != null) {
                if (obj.getKind() == Kind.DATABASE_PRINCIPALS_FOLDER) {
                    segments.add("roles");
                } else if (obj.getKind() == Kind.SERVER_PRINCIPALS_FOLDER) {
                    segments.add("logins");
                } else if (obj.getKind() == Kind.COLLECTIONS_FOLDER) {
                    segments.add("tables");
                } else {
                    segments.add(obj.getName());
                }
            }
        }
        return String.join(" > ", segments);
    }

    private TreeItem<DbObject> findTreeItemAt(javafx.event.EventTarget target) {
        if (!(target instanceof javafx.scene.Node node)) return null;
        while (node != null && !(node instanceof TreeCell)) {
            node = node.getParent();
        }
        if (node instanceof TreeCell<?> cell) {
            @SuppressWarnings("unchecked")
            TreeItem<DbObject> ti = (TreeItem<DbObject>) cell.getTreeItem();
            return ti;
        }
        return null;
    }

    public void showSpeedSearch() {
        searchBox.setVisible(true);
        searchBox.setManaged(true);
        addButton.setVisible(false);
        addButton.setManaged(false);
        propertiesButton.setVisible(false);
        propertiesButton.setManaged(false);
        refreshButton.setVisible(false);
        refreshButton.setManaged(false);
        searchField.requestFocus();
    }

    public void hideSpeedSearch() {
        currentSearchQuery = "";
        searchMatches.clear();
        currentMatchIndex = -1;
        searchField.clear();
        matchCountLabel.setText("");
        searchBox.setVisible(false);
        searchBox.setManaged(false);
        addButton.setVisible(true);
        addButton.setManaged(true);
        propertiesButton.setVisible(true);
        propertiesButton.setManaged(true);
        refreshButton.setVisible(true);
        refreshButton.setManaged(true);
        tree.refresh();
        tree.requestFocus();
    }

    public void setSpeedSearchText(String text) {
        showSpeedSearch();
        searchField.setText(text != null ? text : "");
        if (text != null) {
            searchField.positionCaret(text.length());
        }
    }

    public String getSpeedSearchText() {
        return currentSearchQuery;
    }

    public List<TreeItem<DbObject>> getSearchMatches() {
        return Collections.unmodifiableList(searchMatches);
    }

    public int getCurrentMatchIndex() {
        return currentMatchIndex;
    }

    public void selectNextMatch() {
        if (searchMatches.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size();
        selectMatch(searchMatches.get(currentMatchIndex));
        matchCountLabel.setText((currentMatchIndex + 1) + " of " + searchMatches.size());
    }

    public void selectPreviousMatch() {
        if (searchMatches.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size()) % searchMatches.size();
        selectMatch(searchMatches.get(currentMatchIndex));
        matchCountLabel.setText((currentMatchIndex + 1) + " of " + searchMatches.size());
    }

    private void updateSearchMatches(String query) {
        searchMatches.clear();
        currentMatchIndex = -1;

        if (query == null || query.isBlank()) {
            matchCountLabel.setText("");
            searchField.setStyle("");
            return;
        }

        String lower = query.toLowerCase(Locale.ROOT);
        collectTreeMatches(root, lower, searchMatches);

        if (searchMatches.isEmpty()) {
            matchCountLabel.setText("0 of 0");
            matchCountLabel.setStyle("-fx-text-fill: #e06c75; -fx-font-size: 10px;");
            searchField.setStyle("-fx-text-fill: #e06c75;");
        } else {
            matchCountLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 10px;");
            searchField.setStyle("");

            TreeItem<DbObject> currentSel = tree.getSelectionModel().getSelectedItem();
            int existingIdx = currentSel != null ? searchMatches.indexOf(currentSel) : -1;
            if (existingIdx >= 0) {
                currentMatchIndex = existingIdx;
            } else {
                currentMatchIndex = 0;
                selectMatch(searchMatches.get(0));
            }
            matchCountLabel.setText((currentMatchIndex + 1) + " of " + searchMatches.size());
        }
    }

    private void collectTreeMatches(TreeItem<DbObject> parent, String lowerQuery, List<TreeItem<DbObject>> results) {
        if (parent == null) return;
        for (TreeItem<DbObject> child : parent.getChildren()) {
            DbObject obj = child.getValue();
            if (obj != null && obj.getName() != null && !obj.getName().isBlank()) {
                if (obj.getName().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    results.add(child);
                }
            }
            collectTreeMatches(child, lowerQuery, results);
        }
    }

    private void selectMatch(TreeItem<DbObject> item) {
        if (item == null) return;
        TreeItem<DbObject> p = item.getParent();
        while (p != null && p != root) {
            p.setExpanded(true);
            p = p.getParent();
        }
        tree.getSelectionModel().clearSelection();
        tree.getSelectionModel().select(item);
        int row = tree.getRow(item);
        if (row >= 0) {
            tree.scrollTo(row);
        }
    }

    public static javafx.scene.Node buildHighlightedName(String name, String query) {
        if (name == null || name.isEmpty() || query == null || query.isBlank()) {
            Label lbl = new Label(name != null ? name : "");
            lbl.getStyleClass().add("tree-name");
            return lbl;
        }

        String lowerName = name.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        int matchIdx = lowerName.indexOf(lowerQuery);
        if (matchIdx == -1) {
            Label lbl = new Label(name);
            lbl.getStyleClass().add("tree-name");
            return lbl;
        }

        HBox box = new HBox(0);
        box.setAlignment(Pos.CENTER_LEFT);
        int lastIdx = 0;
        while (matchIdx != -1) {
            if (matchIdx > lastIdx) {
                Label normal = new Label(name.substring(lastIdx, matchIdx));
                normal.getStyleClass().add("tree-name");
                box.getChildren().add(normal);
            }
            Label match = new Label(name.substring(matchIdx, matchIdx + lowerQuery.length()));
            match.getStyleClass().addAll("tree-name", "tree-search-match");
            box.getChildren().add(match);

            lastIdx = matchIdx + lowerQuery.length();
            matchIdx = lowerName.indexOf(lowerQuery, lastIdx);
        }
        if (lastIdx < name.length()) {
            Label normal = new Label(name.substring(lastIdx));
            normal.getStyleClass().add("tree-name");
            box.getChildren().add(normal);
        }
        return box;
    }

    public HBox getSearchBox() {
        return searchBox;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public Label getMatchCountLabel() {
        return matchCountLabel;
    }

    public void triggerDropForSelection() {
        List<TreeItem<DbObject>> selected = tree.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) return;
        List<DbObject> dropObjects = selected.stream()
                .map(TreeItem::getValue)
                .filter(obj -> obj != null && !isCategoryFolder(obj.getKind()) && obj.getKind() != Kind.MESSAGE)
                .toList();
        if (!dropObjects.isEmpty()) {
            ConnectionProfile profile = profileFor(selected.get(0));
            ConfirmDropDialog.show(mainWindow, profile, dropObjects, this::reload);
        }
    }

    private List<DbObject> getSelectedDbObjectsForAction(DbObject fallbackObj) {
        List<TreeItem<DbObject>> selectedItems = tree.getSelectionModel().getSelectedItems();
        List<DbObject> result = new ArrayList<>();
        if (selectedItems != null && !selectedItems.isEmpty()) {
            for (TreeItem<DbObject> ti : selectedItems) {
                if (ti != null && ti.getValue() != null && !isCategoryFolder(ti.getValue().getKind())
                        && ti.getValue().getKind() != Kind.MESSAGE) {
                    result.add(ti.getValue());
                }
            }
        }
        if (result.isEmpty() && fallbackObj != null) {
            result.add(fallbackObj);
        }
        return result;
    }

    public static boolean isCategoryFolder(Kind kind) {
        if (kind == null) return false;
        return kind == Kind.TABLES_FOLDER
                || kind == Kind.SEQUENCES_FOLDER
                || kind == Kind.VIEWS_FOLDER
                || kind == Kind.PROCEDURES_FOLDER
                || kind == Kind.FUNCTIONS_FOLDER
                || kind == Kind.DATABASE_OBJECTS
                || kind == Kind.VIRTUAL_VIEWS_FOLDER
                || kind == Kind.ACCESS_METHODS_FOLDER
                || kind == Kind.CASTS_FOLDER
                || kind == Kind.EXTENSIONS_FOLDER
                || kind == Kind.LANGUAGES_FOLDER
                || kind == Kind.COLUMNS_FOLDER
                || kind == Kind.KEYS_FOLDER
                || kind == Kind.FOREIGN_KEYS_FOLDER
                || kind == Kind.INDEXES_FOLDER
                || kind == Kind.PARTITIONS_FOLDER
                || kind == Kind.FIELDS_FOLDER
                || kind == Kind.ROLES_FOLDER;
    }
}
package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.ConnectionStore;
import com.roze.dbnavigator.db.DatabaseAdminService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.roze.dbnavigator.db.ProjectStore;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

/**
 * The DataGrip-style "Database Explorer": all connections in one lazy tree.
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerBox = new HBox(6, header, spacer, scrollFromEditorButton, projectWidget);
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

        HBox toolbarBox = new HBox(4, addButton, propertiesButton, refreshButton,
                jumpToConsoleButton, goToDdlButton, editDataButton);
        toolbarBox.setAlignment(Pos.CENTER_LEFT);
        toolbarBox.setPadding(new Insets(3, 6, 4, 6));
        toolbarBox.getStyleClass().add("schema-pane-toolbar");
        toolbarBox.setMinHeight(Region.USE_PREF_SIZE);

        tree.setRoot(root);
        tree.setShowRoot(false);
        tree.setCellFactory(tv -> new DbObjectCell());
        tree.getStyleClass().add("schema-tree");
        VBox.setVgrow(tree, Priority.ALWAYS);

        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateToolbarState(newVal);
        });

        tree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
                if (item != null) openObject(item);
            }
        });

        tree.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F4 && !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown()) {
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

    public void addConnectionNode(ConnectionProfile profile) {
        DbObject obj = new DbObject(profile.getName(), Kind.CONNECTION);
        obj.setDetail(profile.getType().getDisplayName() + " · " + profile.getSummary()
                + sqliteFileWarning(profile));
        TreeItem<DbObject> item = new TreeItem<>(obj);
        item.getChildren().add(loadingNode());
        connectionItems.put(item, profile);

        item.expandedProperty().addListener((observable, was, expanded) -> {
            if (expanded && !obj.isLoaded()) {
                // Ask for the password now if it wasn't saved (DataGrip behavior)
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
     * Also writes the DataGrip-style "2 of 4" counter onto the connection node.
     */
    private List<DbObject> loadTopLevelFiltered(ConnectionProfile profile, DbObject connectionObj)
            throws Exception {
        List<DbObject> all = MetadataService.loadTopLevel(profile);

        // Same show/hide mechanism, generalized to whichever kind of node
        // this engine's connection root actually produces: DATABASE for
        // PostgreSQL/MySQL/SQL Server/Mongo, or SCHEMA for Oracle (which has
        // no per-connection "database" — schemas are its real organizing
        // unit, one per user, and exactly what Show/Hide Schemas filters).
        boolean filterable = !all.isEmpty() && (
                all.stream().allMatch(o -> o.getKind() == Kind.DATABASE)
                || all.stream().allMatch(o -> o.getKind() == Kind.SCHEMA));
        if (!filterable) {
            connectionObj.setDetail(null);
            return all;
        }

        List<String> filter = profile.getVisibleDatabases();
        List<DbObject> visible = (filter == null || filter.isEmpty())
                ? all
                : all.stream().filter(o -> filter.contains(o.getName())).toList();

        connectionObj.setDetail(visible.size() + " of " + all.size());
        Platform.runLater(tree::refresh);

        List<DbObject> result = new ArrayList<>(visible);
        if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
            DbObject serverObjects = new DbObject("Server Objects", Kind.SERVER_OBJECTS);
            result.add(serverObjects);
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
            case PROCEDURE, FUNCTION -> mainWindow.openRoutineSourceTab(profile, obj);
            case ROLE, TABLESPACE, ACCESS_METHOD, CAST, EXTENSION, LANGUAGE -> mainWindow.openDdlTab(profile, obj);
            case CONNECTION, SERVER_OBJECTS, ROLES_FOLDER, TABLESPACES_FOLDER,
                 DATABASE_OBJECTS, ACCESS_METHODS_FOLDER, CASTS_FOLDER, EXTENSIONS_FOLDER, LANGUAGES_FOLDER,
                 VIRTUAL_VIEWS_FOLDER -> item.setExpanded(!item.isExpanded());
            default -> {}
        }
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
            case SCHEMA   -> MetadataService.loadSchemaChildren(obj);
            case TABLE, PARTITION -> MetadataService.loadTableChildren(profile, obj);
            case COLLECTION -> MetadataService.loadCollectionChildren(profile, obj);
            case SERVER_OBJECTS -> MetadataService.loadServerObjects(profile);
            case ROLES_FOLDER -> MetadataService.loadRoles(profile);
            case TABLESPACES_FOLDER -> MetadataService.loadTablespaces(profile);
            case DATABASE_OBJECTS -> MetadataService.loadDatabaseObjects(profile, obj);
            case ACCESS_METHODS_FOLDER -> MetadataService.loadAccessMethods(profile, obj);
            case CASTS_FOLDER -> MetadataService.loadCasts(profile, obj);
            case EXTENSIONS_FOLDER -> MetadataService.loadExtensions(profile, obj);
            case LANGUAGES_FOLDER -> MetadataService.loadLanguages(profile, obj);
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
                 SERVER_OBJECTS, ROLES_FOLDER, TABLESPACES_FOLDER,
                 DATABASE_OBJECTS, ACCESS_METHODS_FOLDER, CASTS_FOLDER, EXTENSIONS_FOLDER, LANGUAGES_FOLDER,
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
     * manually re-expanded (this class's own real, previous behavior),
     * this reloads the node's own real children while preserving
     * whichever of its own descendants were already expanded -
     * DataGrip's own real refresh behavior, and the entire reason
     * restoreExpansion exists in the first place.
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

    /** Opens the DataGrip-style show/hide databases (or, for MySQL/MariaDB/Oracle, schemas) popup. */
    public void showDatabaseFilterDialog(TreeItem<DbObject> connectionItem,
                                          ConnectionProfile profile) {
        AppExecutor.run(() -> {
            try {
                List<String> allDatabases = MetadataService.listDatabaseNames(profile);
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
     * DataGrip-style destructive confirmation: warn, then require the user to
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
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(""));
        dialog.setTitle("New Virtual View");
        dialog.setHeaderText("Create Virtual View for " + profile.getName());
        dialog.setContentText("View Query / Name:");
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewDef -> {
            if (!viewDef.isBlank()) {
                mainWindow.openQueryTab(profile, null, "-- Virtual View: " + viewDef + "\nSELECT * FROM " + viewDef + ";\n");
            }
        });
    }

    private void showQuickDocumentation(ConnectionProfile profile) {
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
     * Builds the comprehensive DataGrip-style context menu for a connection root node.
     * Package-private so unit tests can inspect and verify the dynamic hierarchy.
     */
    ContextMenu buildConnectionMenu(TreeItem<DbObject> item, DbObject obj, ConnectionProfile profile) {
        ContextMenu menu = new ContextMenu();
        if (profile == null) return menu;
        boolean connected = ClientRegistry.isConnected(profile);
        String noun = schemaOrDatabaseNoun(profile.getType());
        String capNoun = Character.toUpperCase(noun.charAt(0)) + noun.substring(1);

        // For Oracle: DataGrip shows "Download Driver Files" at the very top of connection menu
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

        // Dynamic engine-specific creation items matching DataGrip screenshots:
        // - PostgreSQL: Database..., Role..., User...
        // - MySQL: Schema..., User..., Role...
        // - MariaDB: Schema..., User..., Role...
        // - SQL Server: Database, Server Principal (matching Image 1)
        // - Oracle: none at root New (matching Image 2)
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
            case ORACLE -> {
                // In DataGrip Image 2: Oracle has neither Database, Schema, User nor Role in root New menu.
                // It goes directly to Virtual View.
            }
            default -> {
                // SQLite, MongoDB, StratosDB: do not expose Database/Schema/User/Role creation in New
            }
        }

        MenuItem virtualView = new MenuItem("Virtual View");
        virtualView.setOnAction(e -> showVirtualViewInfo(profile));

        Menu dataSourceSub = new Menu("Data Source");
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

        Menu cloudSub = new Menu("Data Source from Cloud Provider");
        MenuItem cpGcp = new MenuItem("Google Cloud…");
        cpGcp.setOnAction(e -> mainWindow.setStatus("Google Cloud provider"));
        MenuItem cpAws = new MenuItem("Amazon Web Services (AWS)…");
        cpAws.setOnAction(e -> mainWindow.setStatus("AWS provider"));
        MenuItem cpAzure = new MenuItem("Microsoft Azure…");
        cpAzure.setOnAction(e -> mainWindow.setStatus("Azure provider"));
        cloudSub.getItems().addAll(cpGcp, cpAws, cpAzure);

        MenuItem dsTemplates = new MenuItem("Data Source Templates (Beta)");
        dsTemplates.setOnAction(e -> mainWindow.setStatus("Data Source Templates"));

        MenuItem dsFromFile = new MenuItem("Data Source from File/Folder…");
        dsFromFile.setOnAction(e -> mainWindow.showNewConnectionDialog(ConnectionProfile.DatabaseType.SQLITE));

        MenuItem dsFromUrl = new MenuItem("Data Source from URL…");
        dsFromUrl.setOnAction(e -> mainWindow.showNewConnectionDialog());

        MenuItem ddlDs = new MenuItem("DDL Data Source…");
        ddlDs.setOnAction(e -> mainWindow.setStatus("DDL Data Source"));

        MenuItem folderItem = new MenuItem("Folder");
        folderItem.setOnAction(e -> promptMoveToGroup(profile));

        MenuItem driverItem = new MenuItem("Driver…");
        driverItem.setOnAction(e -> mainWindow.setStatus("Driver Configuration"));

        newMenu.getItems().addAll(
                new SeparatorMenuItem(),
                virtualView,
                new SeparatorMenuItem(),
                dataSourceSub,
                cloudSub,
                dsTemplates,
                dsFromFile,
                dsFromUrl,
                ddlDs,
                new SeparatorMenuItem(),
                folderItem,
                driverItem
        );

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
            default -> "Dump Database…";
        };
        MenuItem dumpItem = new MenuItem(dumpName);
        dumpItem.setOnAction(e -> mainWindow.setStatus(dumpName.replace("…", "")));
        MenuItem restoreItem = new MenuItem("Restore…");
        restoreItem.setOnAction(e -> mainWindow.setStatus("Restore database"));
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

    private static TreeItem<DbObject> loadingNode() {
        return new TreeItem<>(new DbObject("Loading…", Kind.MESSAGE));
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
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

            Label nameLabel = new Label(obj.getName());
            nameLabel.getStyleClass().add("tree-name");
            HBox box = new HBox(6, Icons.forObject(obj, profile, connected), nameLabel);
            box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            if (obj.getDetail() != null && !obj.getDetail().isBlank()) {
                Label detailLabel = new Label(obj.getDetail());
                detailLabel.getStyleClass().add("tree-detail");
                if (obj.getKind() == Kind.CONNECTION && profile != null && MetadataService.supportsDatabaseFilter(profile)) {
                    detailLabel.setCursor(javafx.scene.Cursor.HAND);
                    String n = schemaOrDatabaseNoun(profile.getType());
                    detailLabel.setTooltip(new Tooltip("Choose " + n + "s to show in the tree…"));
                    detailLabel.getStyleClass().add("tree-detail-pill");
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
                case DATABASE -> {
                    MenuItem refreshDb = new MenuItem("Refresh");
                    refreshDb.setOnAction(e -> refreshNode(getTreeItem(), obj, obj.getName()));
                    MenuItem newConsole = new MenuItem("New Query Console on " + obj.getName());
                    newConsole.setOnAction(e ->
                            mainWindow.openQueryTab(profile, obj.getCatalog(), null));
                    MenuItem goToDdl = new MenuItem("Go to DDL");
                    goToDdl.setOnAction(e -> goToDdl());

                    if (profile.getType() == ConnectionProfile.DatabaseType.MONGODB) {
                        menu.getItems().addAll(newConsole, new SeparatorMenuItem(), refreshDb);
                        return menu;
                    }

                    MenuItem modifyDb = new MenuItem("Modify Database\u2026");
                    modifyDb.setOnAction(e -> ModifyDatabaseDialog.show(mainWindow, profile, obj.getName()));
                    MenuItem renameDb = new MenuItem("Rename\u2026");
                    renameDb.setOnAction(e -> renameDatabase(profile, obj.getName()));

                    MenuItem dump = new MenuItem("Dump Database to .sql…");
                    dump.setOnAction(e -> {
                        if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
                            PgDumpDialog.show(mainWindow, profile, obj.getName());
                        } else if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                                || profile.getType() == ConnectionProfile.DatabaseType.MARIADB) {
                            MySqlDumpDialog.show(mainWindow, profile, obj.getName());
                        } else if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                            StratosDumpService.dumpDatabase(mainWindow, profile, obj.getName());
                        } else {
                            DumpRestoreService.dumpDatabase(mainWindow, profile, obj.getName());
                        }
                    });
                    MenuItem restore = new MenuItem("Restore .sql into This Database…");
                    restore.setOnAction(e -> {
                        if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
                            RestoreDialog.show(mainWindow, profile, obj.getName());
                        } else if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                                || profile.getType() == ConnectionProfile.DatabaseType.MARIADB) {
                            MySqlRestoreDialog.show(mainWindow, profile, obj.getName());
                        } else if (profile.getType() == ConnectionProfile.DatabaseType.STRATOSDB) {
                            StratosDumpService.restoreDatabase(mainWindow, profile, obj.getName());
                        } else {
                            DumpRestoreService.restoreDatabase(mainWindow, profile, obj.getName());
                        }
                    });

                    Menu diagramsMenu = new Menu("Diagrams");
                    MenuItem showDiagram = new MenuItem("Show Diagram\u2026");
                    showDiagram.setOnAction(e -> mainWindow.openDatabaseDiagramTab(profile, obj.getCatalog()));
                    diagramsMenu.getItems().add(showDiagram);

                    MenuItem deleteDb = new MenuItem("Drop\u2026");
                    deleteDb.setOnAction(e -> confirmAndDeleteDatabase(profile, obj.getName()));

                    menu.getItems().addAll(modifyDb, refreshDb, new SeparatorMenuItem(),
                            renameDb, new SeparatorMenuItem(),
                            newConsole, goToDdl, dump, restore, diagramsMenu,
                            new SeparatorMenuItem(), deleteDb);
                }
                case SCHEMA -> {
                    MenuItem refreshSchema = new MenuItem("Refresh");
                    refreshSchema.setOnAction(e -> refreshNode(getTreeItem(), obj, obj.getName()));
                    MenuItem newConsole = new MenuItem("New Query Console on " + obj.getName());
                    newConsole.setOnAction(e -> {
                        String catalog = profile.getType() == ConnectionProfile.DatabaseType.ORACLE
                                ? obj.getName() : obj.getCatalog();
                        mainWindow.openQueryTab(profile, catalog, null);
                    });
                    MenuItem goToDdl = new MenuItem("Go to DDL");
                    goToDdl.setOnAction(e -> goToDdl());
                    menu.getItems().addAll(newConsole, goToDdl, new SeparatorMenuItem(), refreshSchema);
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
                     LANGUAGES_FOLDER, VIRTUAL_VIEWS_FOLDER -> {
                    MenuItem refresh = new MenuItem("Refresh");
                    refresh.setOnAction(e -> refreshNode(getTreeItem(), obj, obj.getName()));
                    MenuItem jumpConsole = new MenuItem("Jump to Query Console");
                    jumpConsole.setOnAction(e -> jumpToQueryConsole());
                    menu.getItems().addAll(jumpConsole, new SeparatorMenuItem(), refresh);
                }
                case ACCESS_METHOD, CAST, EXTENSION, LANGUAGE -> {
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
     * DataGrip-style rename: a single "new name" prompt. The actual RENAME
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

    /** DataGrip-style destructive confirmation, scaled down from the database version — a routine is easier to recreate than a whole database, so a single Yes/No is enough rather than also requiring the name to be typed out. */
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
                 ACCESS_METHOD, CAST, EXTENSION, LANGUAGE, VIRTUAL_VIEW -> true;
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
        if (obj != null && obj.getKind() == Kind.DATABASE) {
            return obj.getName();
        }
        if (obj != null && obj.getCatalog() != null && !obj.getCatalog().isBlank()) {
            return obj.getCatalog();
        }
        TreeItem<DbObject> curr = item != null ? item.getParent() : null;
        while (curr != null) {
            if (curr.getValue() != null && curr.getValue().getKind() == Kind.DATABASE) {
                return curr.getValue().getName();
            }
            curr = curr.getParent();
        }
        ConnectionProfile profile = profileFor(item);
        return profile != null ? profile.getDatabase() : null;
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

    /**
     * DataGrip-style "Scroll from Editor": locates the active editor tab's
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
        // 0. Check for Server Objects navigation (PostgreSQL roles and tablespaces)
        if (target.getKind() == Kind.SERVER_OBJECTS
                || target.getKind() == Kind.ROLES_FOLDER
                || target.getKind() == Kind.ROLE
                || target.getKind() == Kind.TABLESPACES_FOLDER
                || target.getKind() == Kind.TABLESPACE) {
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
        if (target.getKind() == Kind.ROLES_FOLDER || target.getKind() == Kind.ROLE) {
            TreeItem<DbObject> rolesFolder = serverObjItem.getChildren().stream()
                    .filter(c -> c.getValue() != null && c.getValue().getKind() == Kind.ROLES_FOLDER)
                    .findFirst().orElse(null);
            if (rolesFolder != null) {
                if (target.getKind() == Kind.ROLES_FOLDER) {
                    highlightAndSelect(rolesFolder);
                    return;
                }
                expandNodeAndThen(rolesFolder, () -> {
                    for (TreeItem<DbObject> child : rolesFolder.getChildren()) {
                        if (child.getValue() != null && child.getValue().getName().equalsIgnoreCase(target.getName())) {
                            highlightAndSelect(child);
                            return;
                        }
                    }
                });
            }
        } else if (target.getKind() == Kind.TABLESPACES_FOLDER || target.getKind() == Kind.TABLESPACE) {
            TreeItem<DbObject> tsFolder = serverObjItem.getChildren().stream()
                    .filter(c -> c.getValue() != null && c.getValue().getKind() == Kind.TABLESPACES_FOLDER)
                    .findFirst().orElse(null);
            if (tsFolder != null) {
                if (target.getKind() == Kind.TABLESPACES_FOLDER) {
                    highlightAndSelect(tsFolder);
                    return;
                }
                expandNodeAndThen(tsFolder, () -> {
                    for (TreeItem<DbObject> child : tsFolder.getChildren()) {
                        if (child.getValue() != null && child.getValue().getName().equalsIgnoreCase(target.getName())) {
                            highlightAndSelect(child);
                            return;
                        }
                    }
                });
            }
        }
    }

    private static boolean isDatabaseObjectsKind(Kind kind) {
        return kind == Kind.DATABASE_OBJECTS
                || kind == Kind.ACCESS_METHODS_FOLDER || kind == Kind.ACCESS_METHOD
                || kind == Kind.CASTS_FOLDER || kind == Kind.CAST
                || kind == Kind.EXTENSIONS_FOLDER || kind == Kind.EXTENSION
                || kind == Kind.LANGUAGES_FOLDER || kind == Kind.LANGUAGE;
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
}
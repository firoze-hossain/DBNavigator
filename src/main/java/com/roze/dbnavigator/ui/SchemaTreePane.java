package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.ConnectionStore;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The DataGrip-style "Database Explorer": all connections in one lazy tree.
 * Double-click a table/collection to open it; right-click for actions.
 */
public class SchemaTreePane extends VBox {

    private final TreeView<DbObject> tree = new TreeView<>();
    private final TreeItem<DbObject> root = new TreeItem<>(new DbObject("root", Kind.MESSAGE));
    /** Maps every connection tree item to its profile. */
    private final Map<TreeItem<DbObject>, ConnectionProfile> connectionItems = new ConcurrentHashMap<>();

    private final MainWindow mainWindow;

    public SchemaTreePane(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        getStyleClass().add("schema-pane");

        Label header = new Label("Database Explorer");
        header.getStyleClass().add("panel-header");

        Button addButton = new Button();
        addButton.setGraphic(Icons.of(FontAwesomeSolid.PLUS, "#57965c", 11));
        addButton.setTooltip(new Tooltip("New Data Source"));
        addButton.setOnAction(e -> mainWindow.showNewConnectionDialog());

        Button refreshButton = new Button();
        refreshButton.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#6897bb", 11));
        refreshButton.setTooltip(new Tooltip("Reload connections"));
        refreshButton.setOnAction(e -> reload());

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerBox = new HBox(6, header, spacer, addButton, refreshButton);
        headerBox.setPadding(new Insets(8, 8, 8, 10));
        headerBox.getStyleClass().add("panel-header-box");

        tree.setRoot(root);
        tree.setShowRoot(false);
        tree.setCellFactory(tv -> new DbObjectCell());
        tree.getStyleClass().add("schema-tree");
        VBox.setVgrow(tree, Priority.ALWAYS);

        tree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<DbObject> item = tree.getSelectionModel().getSelectedItem();
                if (item != null) openObject(item);
            }
        });

        getChildren().addAll(headerBox, tree);
        reload();
    }

    /** Rebuilds the root list of connections from the store. */
    public void reload() {
        root.getChildren().clear();
        connectionItems.clear();
        for (ConnectionProfile profile : ConnectionStore.load()) {
            addConnectionNode(profile);
        }
        if (root.getChildren().isEmpty()) {
            root.getChildren().add(new TreeItem<>(
                    new DbObject("No connections — click + to add one", Kind.MESSAGE)));
        }
    }

    public void addConnectionNode(ConnectionProfile profile) {
        DbObject obj = new DbObject(profile.getName(), Kind.CONNECTION);
        obj.setDetail(profile.getType().getDisplayName() + " · " + profile.getSummary());
        TreeItem<DbObject> item = new TreeItem<>(obj);
        item.getChildren().add(loadingNode());
        connectionItems.put(item, profile);

        item.expandedProperty().addListener((observable, was, expanded) -> {
            if (expanded && !obj.isLoaded()) {
                obj.setLoaded(true);
                loadChildrenAsync(item, profile,
                        () -> MetadataService.loadTopLevel(profile));
            }
        });
        root.getChildren().add(item);
    }

    private void openObject(TreeItem<DbObject> item) {
        DbObject obj = item.getValue();
        ConnectionProfile profile = profileFor(item);
        if (profile == null) return;

        switch (obj.getKind()) {
            case TABLE, VIEW -> mainWindow.openDataTab(profile, obj);
            case COLLECTION  -> mainWindow.openMongoTab(profile, obj);
            case CONNECTION  -> item.setExpanded(!item.isExpanded());
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
        AppExecutor.run(() -> {
            try {
                List<DbObject> children = loader.get();
                Platform.runLater(() -> {
                    parent.getChildren().clear();
                    if (children.isEmpty()) {
                        parent.getChildren().add(new TreeItem<>(
                                new DbObject("(empty)", Kind.MESSAGE)));
                        return;
                    }
                    for (DbObject child : children) {
                        TreeItem<DbObject> childItem = new TreeItem<>(child);
                        if (isExpandable(child.getKind())) {
                            childItem.getChildren().add(loadingNode());
                            childItem.expandedProperty().addListener((observable, was, expanded) -> {
                                if (expanded && !child.isLoaded()) {
                                    child.setLoaded(true);
                                    loadChildrenAsync(childItem, profile,
                                            () -> childrenLoader(profile, child));
                                }
                            });
                        }
                        parent.getChildren().add(childItem);
                    }
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    parent.getChildren().clear();
                    parent.getChildren().add(new TreeItem<>(
                            new DbObject("Error: " + msg, Kind.MESSAGE)));
                    parent.getValue().setLoaded(false);
                });
            }
        });
    }

    private static List<DbObject> childrenLoader(ConnectionProfile profile, DbObject obj)
            throws Exception {
        return switch (obj.getKind()) {
            case DATABASE -> MetadataService.loadDatabaseChildren(profile, obj);
            case SCHEMA   -> MetadataService.loadSchemaChildren(obj);
            case TABLE    -> MetadataService.loadTableChildren(profile, obj);
            case TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER, FUNCTIONS_FOLDER,
                 SEQUENCES_FOLDER, COLLECTIONS_FOLDER,
                 COLUMNS_FOLDER, INDEXES_FOLDER, PARTITIONS_FOLDER
                          -> MetadataService.loadFolderChildren(profile, obj);
            default -> List.of();
        };
    }

    private static boolean isExpandable(Kind kind) {
        return switch (kind) {
            case DATABASE, SCHEMA, TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER,
                 FUNCTIONS_FOLDER, SEQUENCES_FOLDER, COLLECTIONS_FOLDER,
                 TABLE, COLUMNS_FOLDER, INDEXES_FOLDER, PARTITIONS_FOLDER -> true;
            default -> false;
        };
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
            HBox box = new HBox(6, Icons.forObject(obj, connected), nameLabel);
            box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            if (obj.getDetail() != null && !obj.getDetail().isBlank()) {
                Label detailLabel =
                        new Label(obj.getDetail());
                detailLabel.getStyleClass().add("tree-detail");
                box.getChildren().add(detailLabel);
            }
            setText(null);
            setGraphic(box);
            setContextMenu(buildMenu(obj, profile));
        }

        private ContextMenu buildMenu(DbObject obj, ConnectionProfile profile) {
            if (profile == null) return null;
            ContextMenu menu = new ContextMenu();

            switch (obj.getKind()) {
                case CONNECTION -> {
                    MenuItem newConsole = new MenuItem("New Query Console");
                    newConsole.setOnAction(e -> mainWindow.openQueryTab(profile, null, null));
                    MenuItem edit = new MenuItem("Edit Connection…");
                    edit.setOnAction(e -> mainWindow.showEditConnectionDialog(profile));
                    MenuItem disconnect = new MenuItem("Disconnect");
                    disconnect.setOnAction(e -> {
                        ClientRegistry.disconnect(profile);
                        reload();
                    });
                    MenuItem delete = new MenuItem("Remove Connection");
                    delete.setOnAction(e -> {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                                "Remove connection \"" + profile.getName() + "\"?",
                                ButtonType.YES, ButtonType.NO);
                        confirm.showAndWait().ifPresent(bt -> {
                            if (bt == ButtonType.YES) {
                                ClientRegistry.disconnect(profile);
                                ConnectionStore.delete(profile);
                                reload();
                            }
                        });
                    });
                    if (profile.getType().isRelational()) {
                        menu.getItems().addAll(newConsole, new SeparatorMenuItem());
                    }
                    menu.getItems().addAll(edit, disconnect, new SeparatorMenuItem(), delete);
                }
                case TABLE, VIEW -> {
                    MenuItem openData = new MenuItem("Open Data");
                    openData.setOnAction(e -> mainWindow.openDataTab(profile, obj));
                    MenuItem structure = new MenuItem("Show Structure");
                    structure.setOnAction(e -> mainWindow.openStructureTab(profile, obj));
                    MenuItem select = new MenuItem("New Query: SELECT *");
                    select.setOnAction(e -> mainWindow.openQueryTab(profile, obj.getCatalog(),
                            "SELECT * FROM " + obj.qualifiedName() + " LIMIT 100;"));
                    MenuItem count = new MenuItem("New Query: COUNT(*)");
                    count.setOnAction(e -> mainWindow.openQueryTab(profile, obj.getCatalog(),
                            "SELECT COUNT(*) FROM " + obj.qualifiedName() + ";"));
                    menu.getItems().addAll(openData, structure, new SeparatorMenuItem(), select, count);
                }
                case COLLECTION -> {
                    MenuItem openDocs = new MenuItem("Open Documents");
                    openDocs.setOnAction(e -> mainWindow.openMongoTab(profile, obj));
                    menu.getItems().add(openDocs);
                }
                case DATABASE -> {
                    if (profile.getType() == ConnectionProfile.DatabaseType.MONGODB) return null;
                    MenuItem newConsole = new MenuItem("New Query Console on " + obj.getName());
                    newConsole.setOnAction(e ->
                            mainWindow.openQueryTab(profile, obj.getCatalog(), null));
                    MenuItem dump = new MenuItem("Dump Database to .sql…");
                    dump.setOnAction(e -> DumpRestoreService.dumpDatabase(
                            getScene().getWindow(), profile, obj.getName()));
                    MenuItem restore = new MenuItem("Restore .sql into This Database…");
                    restore.setOnAction(e -> DumpRestoreService.restoreDatabase(
                            getScene().getWindow(), profile, obj.getName()));
                    menu.getItems().addAll(newConsole, new SeparatorMenuItem(), dump, restore);
                }
                default -> {
                    return null;
                }
            }
            return menu;
        }
    }
}

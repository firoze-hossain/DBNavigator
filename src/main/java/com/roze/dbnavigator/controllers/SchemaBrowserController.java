package com.roze.dbnavigator.controllers;


import com.roze.dbnavigator.models.ConnectionProfile;
import com.roze.dbnavigator.models.DatabaseObject;
import com.roze.dbnavigator.models.TableIndex;
import com.roze.dbnavigator.models.TableColumnInfo;
import com.roze.dbnavigator.services.ConnectionService;
import com.roze.dbnavigator.services.SchemaService;
import com.roze.dbnavigator.views.components.DatabaseObjectCell;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SchemaBrowserController {
    @FXML
    private TreeView<DatabaseObject> schemaTree;
    private ConnectionProfile currentProfile;
    @FXML
    private TextField searchField;
    @FXML
    private Label statusLabel;
    private final Image tableIcon = new Image(getClass().getResourceAsStream("/assets/icons/objects/table.png"));
    private final Image viewIcon = new Image(getClass().getResourceAsStream("/assets/icons/objects/view.png"));
    private final Image procedureIcon = new Image(getClass().getResourceAsStream("/assets/icons/objects/procedure.png"));
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    @FXML
    public void initialize() {
        schemaTree.setCellFactory(tv -> new DatabaseObjectCell());
        schemaTree.setShowRoot(true);

        schemaTree.addEventHandler(TreeItem.<DatabaseObject>branchExpandedEvent(), event -> {
            TreeItem<DatabaseObject> item = event.getTreeItem();
            if (item != null && !item.getValue().isLoaded()) {
                try {
                    DatabaseObject obj = item.getValue();

                    if (obj.getType() == DatabaseObject.Type.TABLES_FOLDER) {
                        SchemaService.loadTables(item, currentProfile);
                    }
                    else if (obj.getType() == DatabaseObject.Type.VIEWS_FOLDER) {
                        SchemaService.loadViews(item, currentProfile);
                    }
                    else if (obj.getType() == DatabaseObject.Type.PROCEDURES_FOLDER) {
                        SchemaService.loadProcedures(item, currentProfile);
                    }
                    else if (obj.getType() == DatabaseObject.Type.SEQUENCES_FOLDER) {
                        SchemaService.loadSequences(item, currentProfile);
                    }
                    else if (obj.getType() == DatabaseObject.Type.FUNCTIONS_FOLDER) {
                        SchemaService.loadFunctions(item, currentProfile);
                    }

                    obj.setLoaded(true);
                } catch (Exception e) {
                    statusLabel.setText("Error loading: " + e.getMessage());
                    item.getChildren().clear();
                    item.getChildren().add(new TreeItem<>(
                            new DatabaseObject("Error: " + e.getMessage(),
                                    DatabaseObject.Type.SYSTEM_FOLDER)));
                }
            }
        });
        // Handle double-click
        schemaTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<DatabaseObject> item = schemaTree.getSelectionModel().getSelectedItem();
                if (item != null) {
                    handleObjectSelection(item.getValue());
                }
            }
        });

        // Setup context menu
        setupContextMenu();
    }

    private void updateTableStructureView(DatabaseObject table, List<TableColumnInfo> columns,
                                          List<String> primaryKeys, List<TableIndex> indexes) {
        try {
            // Use absolute path with leading slash
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/views/fxml/table-structure-view.fxml"));
            Tab tab = new Tab(table.getName() + " Structure");
            tab.setContent(loader.load());

            TableStructureController controller = loader.getController();
            controller.initializeTable(table, columns, primaryKeys, indexes);

            mainController.getTabPane().getTabs().add(tab);
            mainController.getTabPane().getSelectionModel().select(tab);
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading structure view: " + e.getMessage());
        }
    }
    public void loadConnection(ConnectionProfile profile) {

        this.currentProfile = profile;
        statusLabel.setText("Loading schema...");

        Task<TreeItem<DatabaseObject>> loadTask = new Task<>() {
            @Override
            protected TreeItem<DatabaseObject> call() throws Exception {
                try {
                    return SchemaService.loadServerTree(profile);
                } catch (SQLException e) {
                    throw new Exception("Failed to load schema: " + e.getMessage(), e);
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            schemaTree.setRoot(loadTask.getValue());
            statusLabel.setText("Ready");
            // Expand the database node by default
            if (schemaTree.getRoot() != null && !schemaTree.getRoot().getChildren().isEmpty()) {
                schemaTree.getRoot().getChildren().get(0).setExpanded(true);
            }
        });
        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            String errorMsg = ex.getMessage();
            // Get to the root cause
            while (ex.getCause() != null) {
                ex = ex.getCause();
                errorMsg = ex.getMessage();
            }
            statusLabel.setText("Load failed: " + errorMsg);
            schemaTree.setRoot(new TreeItem<>(
                    new DatabaseObject("Error loading schema", DatabaseObject.Type.SYSTEM_FOLDER)));
        });

        new Thread(loadTask).start();
    }
    private void loadChildren(TreeItem<DatabaseObject> parentItem) {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                SchemaService.loadChildren(parentItem, currentProfile);
                parentItem.getValue().setLoaded(true);
                return null;
            }
        };

        loadTask.setOnFailed(event -> {
            parentItem.getChildren().clear();
            parentItem.getChildren().add(new TreeItem<>(
                    new DatabaseObject("Load failed: " + loadTask.getException().getMessage(),
                            DatabaseObject.Type.SYSTEM_FOLDER)));
        });

        new Thread(loadTask).start();
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(event -> refreshSelectedItem());

        schemaTree.setContextMenu(contextMenu);

        schemaTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            contextMenu.getItems().clear();
            if (newVal != null) {
                contextMenu.getItems().add(refreshItem);

                // Add context-specific items
                DatabaseObject selectedObj = newVal.getValue();
                if (selectedObj.getType() == DatabaseObject.Type.TABLE) {
                    MenuItem viewDataItem = new MenuItem("View Data");
                    viewDataItem.setOnAction(event -> showTableData(selectedObj));
                    contextMenu.getItems().add(viewDataItem);

                    MenuItem designTableItem = new MenuItem("Design Table");
                    designTableItem.setOnAction(event -> designTable(selectedObj));
                    contextMenu.getItems().add(designTableItem);
                }
            }
        });
    }

    @FXML
    public void refreshSchema() {
        if (currentProfile != null) {
            statusLabel.setText("Refreshing...");
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    schemaTree.setRoot(SchemaService.loadServerTree(currentProfile));
                    return null;
                }
            };
            task.setOnSucceeded(e -> statusLabel.setText("Ready"));
            task.setOnFailed(e -> statusLabel.setText("Refresh failed"));
            new Thread(task).start();
        }
    }

    private void handleObjectSelection(DatabaseObject object) {
        // Implement based on your needs
    }

//    private void showTableData(DatabaseObject table) {
//        // Implement table data viewing
//    }

    private void designTable(DatabaseObject table) {
        // Implement table design
    }

    private void refreshSelectedItem() {
        TreeItem<DatabaseObject> selectedItem = schemaTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            selectedItem.getValue().setLoaded(false);
            selectedItem.getChildren().clear();
            selectedItem.getChildren().add(new TreeItem<>(
                    new DatabaseObject("Loading...", DatabaseObject.Type.SYSTEM_FOLDER)));
            loadChildren(selectedItem);
        }
    }

    public void loadSchema(ConnectionProfile profile) {
        TreeItem<DatabaseObject> root = new TreeItem<>(new DatabaseObject(profile.getName(), DatabaseObject.Type.DATABASE));
        root.setExpanded(true);

        // Load schemas
        SchemaService.getSchemas(profile).forEach(schema -> {
            TreeItem<DatabaseObject> schemaItem = new TreeItem<>(schema);

            // Tables
            TreeItem<DatabaseObject> tablesItem = new TreeItem<>(
                    new DatabaseObject("Tables", DatabaseObject.Type.TABLES_FOLDER));
            SchemaService.getTables(profile, schema.getName()).forEach(table ->
                    tablesItem.getChildren().add(createObjectItem(table, tableIcon)));

            // Views
            TreeItem<DatabaseObject> viewsItem = new TreeItem<>(
                    new DatabaseObject("Views", DatabaseObject.Type.VIEWS_FOLDER));
            SchemaService.getViews(profile, schema.getName()).forEach(view ->
                    viewsItem.getChildren().add(createObjectItem(view, viewIcon)));

            // Procedures
            TreeItem<DatabaseObject> proceduresItem = new TreeItem<>(
                    new DatabaseObject("Procedures", DatabaseObject.Type.PROCEDURES_FOLDER));
            SchemaService.getProcedures(profile, schema.getName()).forEach(proc ->
                    proceduresItem.getChildren().add(createObjectItem(proc, procedureIcon)));

            schemaItem.getChildren().addAll(tablesItem, viewsItem, proceduresItem);
            root.getChildren().add(schemaItem);
        });

        schemaTree.setRoot(root);
        schemaTree.setCellFactory(tv -> new DatabaseObjectCell());

        // Handle double-click to load object into editor
        schemaTree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<DatabaseObject> item = schemaTree.getSelectionModel().getSelectedItem();
                if (item != null && item.getValue().getType() == DatabaseObject.Type.TABLE) {
                    loadTableIntoEditor(item.getValue());
                }
            }
        });
    }
// SchemaBrowserController.java

    private void setupTreeViewInteractions() {
        // Single click - show table structure
        schemaTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue().getType() == DatabaseObject.Type.TABLE) {
                showTableStructure(newVal.getValue());
            }
        });

        // Double click - show table data
        schemaTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<DatabaseObject> item = schemaTree.getSelectionModel().getSelectedItem();
                if (item != null && item.getValue().getType() == DatabaseObject.Type.TABLE) {
                    showTableData(item.getValue());
                }
            }
        });
    }

    private void showTableStructure(DatabaseObject table) {
        System.out.println("Attempting to load structure for table: " + table.getFullName());


        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = ConnectionService.getConnection(currentProfile)) {
                    System.out.println("Connection obtained for metadata");
                    DatabaseMetaData meta = conn.getMetaData();

                    // Get columns
                    System.out.println("Fetching columns...");
                    try (ResultSet columns = meta.getColumns(null, table.getSchema(), table.getName(), "%")) {
                        List<TableColumnInfo> columnList = new ArrayList<>();
                        while (columns.next()) {
                            columnList.add(new TableColumnInfo(
                                    columns.getString("COLUMN_NAME"),
                                    columns.getString("TYPE_NAME"),
                                    columns.getInt("COLUMN_SIZE"),
                                    columns.getString("IS_NULLABLE"),
                                    columns.getString("COLUMN_DEF")
                            ));
                        }

                        // Get primary keys
                        List<String> primaryKeys = new ArrayList<>();
                        try (ResultSet pk = meta.getPrimaryKeys(null, table.getSchema(), table.getName())) {
                            while (pk.next()) {
                                primaryKeys.add(pk.getString("COLUMN_NAME"));
                            }
                        }

                        // Get indexes
                        List<TableIndex> indexes = new ArrayList<>();
                        try (ResultSet idx = meta.getIndexInfo(null, table.getSchema(), table.getName(), false, false)) {
                            while (idx.next()) {
                                indexes.add(new TableIndex(
                                        idx.getString("INDEX_NAME"),
                                        idx.getString("COLUMN_NAME"),
                                        idx.getBoolean("NON_UNIQUE")
                                ));
                            }
                        }

                        Platform.runLater(() -> {
                            // Update UI with table structure
                            updateTableStructureView(table, columnList, primaryKeys, indexes);
                        });
                    }
                }
                return null;
            }
        };

        new Thread(task).start();
    }

    private void showTableData(DatabaseObject table) {
        if (mainController != null) {
            mainController.openDataViewTab(table);
        }
    }
    private TreeItem<DatabaseObject> createObjectItem(DatabaseObject object, Image icon) {
        TreeItem<DatabaseObject> item = new TreeItem<>(object);
        item.setGraphic(new ImageView(icon));
        return item;
    }

    private void loadTableIntoEditor(DatabaseObject table) {
        // Load table structure or data into editor
    }
}
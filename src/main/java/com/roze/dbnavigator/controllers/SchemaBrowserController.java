package com.roze.dbnavigator.controllers;


import com.roze.dbnavigator.models.ConnectionProfile;
import com.roze.dbnavigator.models.DatabaseObject;
import com.roze.dbnavigator.services.SchemaService;
import com.roze.dbnavigator.views.components.DatabaseObjectCell;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.sql.SQLException;

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

    @FXML
    public void initialize() {
        schemaTree.setCellFactory(tv -> new DatabaseObjectCell());
        schemaTree.setShowRoot(true);

// Handle expansion events for lazy loading
        schemaTree.addEventHandler(TreeItem.<DatabaseObject>branchExpandedEvent(), event -> {
            TreeItem<DatabaseObject> item = event.getTreeItem();
            if (item != null && !item.getValue().isLoaded()) {
                DatabaseObject obj = item.getValue();

                if (obj.getType() == DatabaseObject.Type.TABLES_FOLDER) {
                    SchemaService.loadTables(item, currentProfile);
                    obj.setLoaded(true);
                }
                else if (obj.getType() == DatabaseObject.Type.PROCEDURES_FOLDER) {
                    SchemaService.loadProcedures(item, currentProfile);
                    obj.setLoaded(true);
                }
                // Add similar handlers for other types as needed
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
            // Expand the root node
            if (schemaTree.getRoot() != null) {
                schemaTree.getRoot().setExpanded(true);
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

    private void showTableData(DatabaseObject table) {
        // Implement table data viewing
    }

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

    private TreeItem<DatabaseObject> createObjectItem(DatabaseObject object, Image icon) {
        TreeItem<DatabaseObject> item = new TreeItem<>(object);
        item.setGraphic(new ImageView(icon));
        return item;
    }

    private void loadTableIntoEditor(DatabaseObject table) {
        // Load table structure or data into editor
    }
}
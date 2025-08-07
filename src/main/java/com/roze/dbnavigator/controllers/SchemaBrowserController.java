package com.roze.dbnavigator.controllers;


import com.roze.dbnavigator.models.ConnectionProfile;
import com.roze.dbnavigator.models.DatabaseObject;
import com.roze.dbnavigator.services.SchemaService;
import com.roze.dbnavigator.views.components.DatabaseObjectCell;
import javafx.fxml.FXML;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SchemaBrowserController {
    @FXML private TreeView<DatabaseObject> schemaTree;
    
    private final Image tableIcon = new Image(getClass().getResourceAsStream("/assets/icons/objects/table.png"));
    private final Image viewIcon = new Image(getClass().getResourceAsStream("/assets/icons/objects/view.png"));
    private final Image procedureIcon = new Image(getClass().getResourceAsStream("/assets/icons/objects/procedure.png"));
    
    public void loadSchema(ConnectionProfile profile) {
        TreeItem<DatabaseObject> root = new TreeItem<>(new DatabaseObject(profile.getName(), DatabaseObject.Type.DATABASE));
        root.setExpanded(true);
        
        // Load schemas
        SchemaService.getSchemas(profile).forEach(schema -> {
            TreeItem<DatabaseObject> schemaItem = new TreeItem<>(schema);
            
            // Tables
            TreeItem<DatabaseObject> tablesItem = new TreeItem<>(
                new DatabaseObject("Tables", DatabaseObject.Type.FOLDER));
            SchemaService.getTables(profile, schema.getName()).forEach(table -> 
                tablesItem.getChildren().add(createObjectItem(table, tableIcon)));
            
            // Views
            TreeItem<DatabaseObject> viewsItem = new TreeItem<>(
                new DatabaseObject("Views", DatabaseObject.Type.FOLDER));
            SchemaService.getViews(profile, schema.getName()).forEach(view -> 
                viewsItem.getChildren().add(createObjectItem(view, viewIcon)));
            
            // Procedures
            TreeItem<DatabaseObject> proceduresItem = new TreeItem<>(
                new DatabaseObject("Procedures", DatabaseObject.Type.FOLDER));
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
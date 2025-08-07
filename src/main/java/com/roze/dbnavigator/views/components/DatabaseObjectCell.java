package com.roze.dbnavigator.views.components;

import com.roze.dbnavigator.models.DatabaseObject;
import javafx.scene.control.TreeCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

public class DatabaseObjectCell extends TreeCell<DatabaseObject> {
    private final ImageView defaultIcon = new ImageView();
    
    @Override
    protected void updateItem(DatabaseObject item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setTooltip(null);
        } else {
            setText(item.toString());
            
            // Set graphic (icon) if available, otherwise use default
            if (item.getIcon() != null) {
                setGraphic(new ImageView(item.getIcon()));
            } else {
                setGraphic(defaultIcon);
            }
            
            // Set tooltip with additional information
            if (item.getType() != DatabaseObject.Type.FOLDER) {
                Tooltip tooltip = new Tooltip();
                tooltip.setText(String.format(
                    "Type: %s\nSchema: %s\nName: %s",
                    item.getType().getDisplayName(),
                    item.getSchema() != null ? item.getSchema() : "N/A",
                    item.getName()
                ));
                setTooltip(tooltip);
            } else {
                setTooltip(null);
            }
        }
    }
}
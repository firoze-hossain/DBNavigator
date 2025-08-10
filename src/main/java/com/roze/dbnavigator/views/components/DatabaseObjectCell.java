package com.roze.dbnavigator.views.components;

import com.roze.dbnavigator.models.DatabaseObject;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class DatabaseObjectCell extends TreeCell<DatabaseObject> {
    private final ImageView defaultIcon = new ImageView();

    public DatabaseObjectCell() {
        // Set default size for icons
        defaultIcon.setFitWidth(16);
        defaultIcon.setFitHeight(16);
    }

    @Override
    protected void updateItem(DatabaseObject item, boolean empty) {
        super.updateItem(item, empty);


        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setTooltip(null);
        } else {
            setText(item.toString());
            setGraphic(getIconForType(item.getType()));

            // Set tooltip with additional information
            if (item.getType().isFolder()) {
                setTooltip(null);
            } else {
                Tooltip tooltip = new Tooltip();
                tooltip.setText(getTooltipText(item));
                setTooltip(tooltip);
            }
        }
    }

    private ImageView getIconForType(DatabaseObject.Type type) {
        String iconPath = getIconPathForType(type);
        if (iconPath != null) {
            try {
                Image icon = new Image(getClass().getResourceAsStream(iconPath));
                ImageView iconView = new ImageView(icon);
                iconView.setFitWidth(16);
                iconView.setFitHeight(16);
                return iconView;
            } catch (Exception e) {
                // Fall through to default
            }
        }
        return defaultIcon;
    }

    private String getIconPathForType(DatabaseObject.Type type) {
        switch (type) {
            case SERVER:
                return "/assets/icons/server.png";
            case DATABASE:
                return "/assets/icons/database.png";
            case SCHEMA:
                return "/assets/icons/schema.png";
            case TABLE:
                return "/assets/icons/table.png";
            case VIEW:
                return "/assets/icons/view.png";
            case COLUMN:
                return "/assets/icons/column.png";
            case INDEX:
                return "/assets/icons/index.png";
            case PROCEDURE:
                return "/assets/icons/procedure.png";
            case FUNCTION:
                return "/assets/icons/function.png";
            case SEQUENCE:
                return "/assets/icons/sequence.png";
            case USER:
                return "/assets/icons/user.png";
            case ROLE:
                return "/assets/icons/role.png";
            default:
                return "/assets/icons/folder.png";
        }
    }

    private String getTooltipText(DatabaseObject item) {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: ").append(item.getType().getDisplayName()).append("\n");

        if (item.getSchema() != null) {
            sb.append("Schema: ").append(item.getSchema()).append("\n");
        }

        if (item.getAdditionalInfo() != null) {
            sb.append("\n").append(item.getAdditionalInfo());
        }

        return sb.toString();
    }
}
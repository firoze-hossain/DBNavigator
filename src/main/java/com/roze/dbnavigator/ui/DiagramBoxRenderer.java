package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.MetadataService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders one table's box (header + column list, PK marked) for an ER diagram. */
public final class DiagramBoxRenderer {

    private DiagramBoxRenderer() {}

    /**
     * @param columnAnchors filled in with "table.column" -> the column's Label,
     *                      so the caller can later compute connector line endpoints
     */
    public static VBox buildTableBox(String tableName, List<MetadataService.ColumnInfo> columns,
                                     boolean isRoot, double boxWidth,
                                     Map<String, Label> columnAnchors) {
        VBox box = new VBox();
        box.setPrefWidth(boxWidth);
        box.getStyleClass().add(isRoot ? "diagram-table-box-root" : "diagram-table-box");

        HBox header = new HBox(6, Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12), new Label(tableName));
        header.getStyleClass().add("diagram-table-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 8, 6, 8));
        box.getChildren().add(header);

        for (MetadataService.ColumnInfo col : columns) {
            HBox row = new HBox(6);
            row.getStyleClass().add("diagram-column-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 8, 2, 8));
            row.getChildren().add(col.primaryKey()
                    ? Icons.of(FontAwesomeSolid.KEY, "#e0a44c", 10)
                    : Icons.of(FontAwesomeSolid.COLUMNS, "#6f7680", 10));
            Label nameLabel = new Label(col.name());
            nameLabel.getStyleClass().add(col.primaryKey() ? "diagram-column-pk" : "diagram-column-name");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Label typeLabel = new Label(col.typeName().toLowerCase(Locale.ROOT));
            typeLabel.getStyleClass().add("diagram-column-type");
            row.getChildren().addAll(nameLabel, gap, typeLabel);
            box.getChildren().add(row);

            if (columnAnchors != null) {
                columnAnchors.put(tableName.toLowerCase(Locale.ROOT) + "."
                        + col.name().toLowerCase(Locale.ROOT), nameLabel);
            }
        }
        return box;
    }

    /** Rough box height estimate for layout purposes, before the real layout pass runs. */
    public static double estimatedHeight(List<MetadataService.ColumnInfo> columns) {
        return 30 + columns.size() * 22;
    }
}

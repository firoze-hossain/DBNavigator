package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Table structure viewer: Columns, Indexes, and an editable DDL tab with a
 * syntax-highlighted, copyable CREATE TABLE script (DataGrip's DDL view).
 */
public class StructureTab extends Tab {

    public StructureTab(ConnectionProfile profile, DbObject table) {
        setText(table.getName() + " [structure]");
        setGraphic(Icons.of(FontAwesomeSolid.SITEMAP, "#c77dbb", 11));
        setTooltip(new Tooltip(profile.getName()
                + (table.getCatalog() != null ? " ▸ " + table.getCatalog() : "")
                + (table.getSchema() != null ? " ▸ " + table.getSchema() : "")
                + " ▸ " + table.getName()));

        ResultGrid columnsGrid = new ResultGrid();
        ResultGrid indexesGrid = new ResultGrid();

        // ---- DDL tab: editable highlighted editor + copy button ----
        CodeArea ddlEditor = SqlHighlighter.createEditor();
        Button copyButton = new Button("Copy DDL");
        copyButton.setGraphic(Icons.of(FontAwesomeSolid.COPY, "#6897bb", 11));
        copyButton.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(ddlEditor.getText());
            Clipboard.getSystemClipboard().setContent(content);
            copyButton.setText("Copied ✓");
        });
        ddlEditor.plainTextChanges().subscribe(c -> copyButton.setText("Copy DDL"));

        Label hint = new Label("Generated from metadata — edit freely, then copy into a console to run");
        hint.getStyleClass().add("console-status");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox ddlToolbar = new HBox(8, copyButton, spacer, hint);
        ddlToolbar.setAlignment(Pos.CENTER_LEFT);
        ddlToolbar.setPadding(new Insets(6, 10, 6, 10));
        ddlToolbar.getStyleClass().add("console-toolbar");

        VBox ddlBox = new VBox(ddlToolbar, new VirtualizedScrollPane<>(ddlEditor));
        VBox.setVgrow(ddlBox.getChildren().get(1), Priority.ALWAYS);

        Tab columnsTab = new Tab("Columns", columnsGrid);
        columnsTab.setClosable(false);
        Tab indexesTab = new Tab("Indexes", indexesGrid);
        indexesTab.setClosable(false);
        Tab ddlTab = new Tab("DDL", ddlBox);
        ddlTab.setClosable(false);

        TabPane inner = new TabPane(columnsTab, indexesTab, ddlTab);
        inner.getStyleClass().add("structure-tabs");
        setContent(inner);

        AppExecutor.run(() -> {
            try {
                QueryResult columns = MetadataService.loadTableStructure(profile, table);
                QueryResult indexes = MetadataService.loadTableIndexes(profile, table);
                List<String> pk = new ArrayList<>();
                try {
                    pk.addAll(MetadataService.loadPrimaryKeys(profile, table));
                } catch (Exception ignored) {}
                String ddl = buildDdl(table, columns, indexes, pk);
                Platform.runLater(() -> {
                    columnsGrid.showResult(columns);
                    indexesGrid.showResult(indexes);
                    ddlEditor.replaceText(ddl);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    columnsGrid.setPlaceholder(new Label("Error: " + msg));
                    ddlEditor.replaceText("-- Error loading structure: " + msg);
                });
            }
        });
    }

    // ------------------------------------------------------------ DDL builder

    /** Builds a portable CREATE TABLE + CREATE INDEX script from JDBC metadata. */
    private static String buildDdl(DbObject table, QueryResult columns,
                                   QueryResult indexes, List<String> pk) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(table.qualifiedName()).append(" (\n");

        List<String> lines = new ArrayList<>();
        for (List<String> row : columns.getRows()) {
            // row: [Column, Type, Size, Nullable, Default, Key]
            String name = row.get(0);
            String type = row.get(1);
            String size = row.get(2);
            boolean nullable = "YES".equalsIgnoreCase(row.get(3));
            String def = row.get(4);

            StringBuilder line = new StringBuilder("    ")
                    .append(DbObject.quote(name)).append(' ').append(type);
            if (needsSize(type) && size != null && !size.equals("0")) {
                line.append('(').append(size).append(')');
            }
            if (def != null && !def.isBlank() && !def.equalsIgnoreCase("null")) {
                line.append(" DEFAULT ").append(def);
            }
            if (!nullable) line.append(" NOT NULL");
            lines.add(line.toString());
        }
        if (!pk.isEmpty()) {
            lines.add("    PRIMARY KEY (" + String.join(", ", pk) + ")");
        }
        sb.append(String.join(",\n", lines)).append("\n);\n");

        // group index rows [Index, Column, Unique, Type] by index name
        Map<String, List<String>> indexColumns = new LinkedHashMap<>();
        Map<String, Boolean> unique = new LinkedHashMap<>();
        for (List<String> row : indexes.getRows()) {
            indexColumns.computeIfAbsent(row.get(0), k -> new ArrayList<>()).add(row.get(1));
            unique.put(row.get(0), "YES".equalsIgnoreCase(row.get(2)));
        }
        for (var entry : indexColumns.entrySet()) {
            sb.append('\n')
              .append(Boolean.TRUE.equals(unique.get(entry.getKey()))
                      ? "CREATE UNIQUE INDEX " : "CREATE INDEX ")
              .append(DbObject.quote(entry.getKey()))
              .append(" ON ").append(table.qualifiedName())
              .append(" (").append(String.join(", ", entry.getValue())).append(");");
        }
        sb.append('\n');
        return sb.toString();
    }

    private static boolean needsSize(String type) {
        String t = type.toLowerCase(Locale.ROOT);
        return t.contains("char") || t.equals("numeric") || t.equals("decimal")
                || t.equals("varbinary") || t.equals("bit");
    }
}

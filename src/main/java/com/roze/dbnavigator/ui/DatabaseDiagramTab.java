package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DataGrip-style whole-database ER diagram: every table in the database,
 * laid out in a grid, connected by lines wherever a real foreign key exists.
 *
 * Honest scope note: this uses a simple alphabetical grid layout rather than
 * yFiles-style automatic graph layout (force-directed/orthogonal routing
 * that clusters related tables and minimizes line crossings) — for
 * databases with many tables the grid will have more crossing lines than
 * DataGrip's own diagram, but it shows the same real relationships. Capped
 * at a maximum table count to stay renderable; a status message says so if
 * the database has more than that.
 */
public class DatabaseDiagramTab extends Tab {

    private static final int MAX_TABLES = 100;
    private static final double BOX_WIDTH = 220;
    private static final double COLUMN_GAP = 60;
    private static final double ROW_GAP = 60;

    private final ConnectionProfile profile;
    private final String catalog;
    private final Pane diagramPane = new Pane();
    private final Group zoomGroup = new Group();
    private final Scale scale = new Scale(1, 1, 0, 0);
    private final Map<String, Label> columnAnchors = new LinkedHashMap<>();
    private final Label statusLabel = new Label();

    public DatabaseDiagramTab(ConnectionProfile profile, String catalog) {
        this.profile = profile;
        this.catalog = catalog;

        setText((catalog != null ? catalog : profile.getName()) + " (all tables)");
        setGraphic(Icons.of(FontAwesomeSolid.PROJECT_DIAGRAM, "#c77dbb", 11));

        Button zoomIn = toolbarButton(FontAwesomeSolid.SEARCH_PLUS, "Zoom in", () -> zoomBy(1.15));
        Button zoomOut = toolbarButton(FontAwesomeSolid.SEARCH_MINUS, "Zoom out", () -> zoomBy(1 / 1.15));
        Button resetZoom = toolbarButton(FontAwesomeSolid.EXPAND, "Reset zoom", this::resetZoom);
        Button refresh = toolbarButton(FontAwesomeSolid.SYNC_ALT, "Refresh", this::reload);
        Button exportPng = toolbarButton(FontAwesomeSolid.FILE_IMAGE, "Export as PNG",
                () -> DiagramExport.exportToPng(diagramPane.getScene() == null ? null : diagramPane.getScene().getWindow(),
                        zoomGroup, (catalog != null ? catalog : "database") + "-diagram"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusLabel.getStyleClass().add("console-status");
        HBox toolbar = new HBox(6, zoomIn, zoomOut, resetZoom, refresh, exportPng, spacer, statusLabel);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        diagramPane.getStyleClass().add("diagram-pane");
        diagramPane.setPrefSize(3000, 2000);
        zoomGroup.getChildren().add(diagramPane);
        zoomGroup.getTransforms().add(scale);

        ScrollPane scroll = new ScrollPane(zoomGroup);
        scroll.setPannable(true);
        scroll.getStyleClass().add("diagram-scroll");
        scroll.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                zoomBy(e.getDeltaY() > 0 ? 1.08 : 1 / 1.08);
                e.consume();
            }
        });

        VBox root = new VBox(toolbar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        setContent(root);

        diagramPane.getChildren().add(loadingLabel());
        reload();
    }

    private Label loadingLabel() {
        Label l = new Label("Loading every table in the database\u2026");
        l.setLayoutX(20);
        l.setLayoutY(20);
        return l;
    }

    private Button toolbarButton(FontAwesomeSolid icon, String tooltip, Runnable action) {
        Button b = new Button();
        b.setGraphic(Icons.of(icon, "#a9b7c6", 12));
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> action.run());
        return b;
    }

    private void zoomBy(double factor) {
        scale.setX(clamp(scale.getX() * factor));
        scale.setY(clamp(scale.getY() * factor));
    }

    private void resetZoom() {
        scale.setX(1);
        scale.setY(1);
    }

    private static double clamp(double v) {
        return Math.max(0.1, Math.min(3.0, v));
    }

    private void reload() {
        diagramPane.getChildren().setAll(loadingLabel());
        columnAnchors.clear();

        AppExecutor.run(() -> {
            try {
                List<DbObject> allTables = MetadataService.loadDatabaseTables(profile, catalog);
                boolean truncated = allTables.size() > MAX_TABLES;
                List<DbObject> tables = new java.util.ArrayList<>(
                        truncated ? allTables.subList(0, MAX_TABLES) : allTables);
                tables.sort((a, b) -> {
                    String an = (a.getSchema() != null ? a.getSchema() + "." : "") + a.getName();
                    String bn = (b.getSchema() != null ? b.getSchema() + "." : "") + b.getName();
                    return an.compareToIgnoreCase(bn);
                });

                Map<DbObject, List<MetadataService.ColumnInfo>> columnsByTable = new LinkedHashMap<>();
                for (DbObject table : tables) {
                    try {
                        columnsByTable.put(table, MetadataService.loadColumnInfo(profile, table));
                    } catch (Exception ignored) {
                        columnsByTable.put(table, List.of());
                    }
                }

                List<MetadataService.ForeignKey> relationships =
                        MetadataService.loadForeignKeysForTables(profile, catalog, tables);

                int total = allTables.size();
                Platform.runLater(() -> render(tables, columnsByTable, relationships, total, truncated));
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> diagramPane.getChildren().setAll(errorLabel(msg)));
            }
        });
    }

    private Label errorLabel(String msg) {
        Label l = new Label("Could not load diagram: " + msg);
        l.setLayoutX(20);
        l.setLayoutY(20);
        return l;
    }

    private void render(List<DbObject> tables, Map<DbObject, List<MetadataService.ColumnInfo>> columnsByTable,
                        List<MetadataService.ForeignKey> relationships, int totalTableCount, boolean truncated) {
        diagramPane.getChildren().clear();
        columnAnchors.clear();

        if (tables.isEmpty()) {
            diagramPane.getChildren().add(errorLabel("No tables found in this database"));
            statusLabel.setText("");
            return;
        }

        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(tables.size())));
        double[] columnX = new double[columns];
        double x = 40;
        for (int c = 0; c < columns; c++) {
            columnX[c] = x;
            x += BOX_WIDTH + COLUMN_GAP;
        }
        double[] rowY = new double[(tables.size() + columns - 1) / columns];
        rowY[0] = 40;

        for (int i = 0; i < tables.size(); i++) {
            DbObject table = tables.get(i);
            int row = i / columns;
            int col = i % columns;
            List<MetadataService.ColumnInfo> cols = columnsByTable.get(table);
            VBox box = DiagramBoxRenderer.buildTableBox(table.getName(), cols, false, BOX_WIDTH, columnAnchors);
            diagramPane.getChildren().add(box);
            box.setLayoutX(columnX[col]);
            box.setLayoutY(rowY[row]);

            boolean lastInRow = (col == columns - 1) || (i == tables.size() - 1);
            if (lastInRow && row + 1 < rowY.length) {
                double rowHeight = 0;
                for (int back = i - col; back <= i; back++) {
                    if (back >= 0 && back < tables.size()) {
                        rowHeight = Math.max(rowHeight,
                                DiagramBoxRenderer.estimatedHeight(columnsByTable.get(tables.get(back))));
                    }
                }
                rowY[row + 1] = rowY[row] + rowHeight + ROW_GAP;
            }
        }

        statusLabel.setText(truncated
                ? "Showing first " + tables.size() + " of " + totalTableCount + " tables (diagram capped for readability)"
                : tables.size() + " table(s), " + relationships.size() + " relationship(s)");

        Platform.runLater(() -> drawConnectors(relationships));
    }

    private void drawConnectors(List<MetadataService.ForeignKey> relationships) {
        for (MetadataService.ForeignKey fk : relationships) {
            Label from = columnAnchors.get(fk.fromTable().toLowerCase(Locale.ROOT) + "."
                    + fk.fromColumn().toLowerCase(Locale.ROOT));
            Label to = columnAnchors.get(fk.toTable().toLowerCase(Locale.ROOT) + "."
                    + fk.toColumn().toLowerCase(Locale.ROOT));
            if (from == null || to == null || from.getScene() == null || to.getScene() == null) continue;

            // Going through scene coordinates (rather than chaining .localToParent()
            // one level at a time) correctly handles any nesting depth between the
            // column Label and diagramPane — a manual parent-by-parent chain here
            // was the bug: it only converted Label -> row, missing row -> table box
            // -> diagramPane, which threw every line off by that box's own position.
            var fromBounds = diagramPane.sceneToLocal(from.localToScene(from.getBoundsInLocal()));
            var toBounds = diagramPane.sceneToLocal(to.localToScene(to.getBoundsInLocal()));

            double x1 = fromBounds.getMinX();
            double y1 = fromBounds.getCenterY();
            double x2 = toBounds.getMinX();
            double y2 = toBounds.getCenterY();

            boolean fromLeftOfTo = x1 < x2;
            Line line = new Line(fromLeftOfTo ? x1 : x1 + BOX_WIDTH, y1,
                    fromLeftOfTo ? x2 + BOX_WIDTH : x2, y2);
            line.getStyleClass().add("diagram-connector");
            diagramPane.getChildren().add(line);
            line.toBack();
        }
    }
}

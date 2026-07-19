package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DataGrip-style ER diagram: the selected table plus every table one FK hop
 * away, each rendered as a box listing its columns (PK marked with a key
 * icon), connected by lines labeled "localColumn:foreignColumn".
 *
 * Honest scope note: layout is a simple three-row arrangement (referenced
 * tables above, the focus table in the middle, referencing tables below)
 * rather than a full force-directed/orthogonal auto-router like yFiles —
 * it conveys the same relationships, just with straight connector lines
 * and simpler positioning. Capped at 8 related tables to stay readable.
 */
public class DiagramTab extends Tab {

    private static final int MAX_RELATED_TABLES = 8;
    private static final double BOX_WIDTH = 220;
    private static final double ROW_HEIGHT = 22;

    private final ConnectionProfile profile;
    private final DbObject rootTable;
    private final Pane diagramPane = new Pane();
    private final Group zoomGroup = new Group();
    private final Scale scale = new Scale(1, 1, 0, 0);
    private final Map<String, Label> columnAnchors = new LinkedHashMap<>();

    public DiagramTab(ConnectionProfile profile, DbObject rootTable) {
        this.profile = profile;
        this.rootTable = rootTable;

        setText(rootTable.getName());
        setGraphic(Icons.of(FontAwesomeSolid.PROJECT_DIAGRAM, "#c77dbb", 11));

        Button zoomIn = toolbarButton(FontAwesomeSolid.SEARCH_PLUS, "Zoom in", () -> zoomBy(1.15));
        Button zoomOut = toolbarButton(FontAwesomeSolid.SEARCH_MINUS, "Zoom out", () -> zoomBy(1 / 1.15));
        Button resetZoom = toolbarButton(FontAwesomeSolid.EXPAND, "Reset zoom", this::resetZoom);
        Button refresh = toolbarButton(FontAwesomeSolid.SYNC_ALT, "Refresh", this::reload);
        Button exportPng = toolbarButton(FontAwesomeSolid.FILE_IMAGE, "Export as PNG",
                () -> DiagramExport.exportToPng(diagramPane.getScene() == null ? null : diagramPane.getScene().getWindow(),
                        zoomGroup, rootTable.getName() + "-diagram"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label hint = new Label("Scroll to pan \u00b7 Ctrl+Scroll to zoom");
        hint.getStyleClass().add("console-status");
        HBox toolbar = new HBox(6, zoomIn, zoomOut, resetZoom, refresh, exportPng, spacer, hint);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        diagramPane.getStyleClass().add("diagram-pane");
        diagramPane.setPrefSize(2000, 1400);
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
        Label l = new Label("Loading diagram\u2026");
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
        return Math.max(0.25, Math.min(3.0, v));
    }

    // ------------------------------------------------------------- reload

    private void reload() {
        diagramPane.getChildren().setAll(loadingLabel());
        columnAnchors.clear();

        AppExecutor.run(() -> {
            try {
                List<MetadataService.ColumnInfo> rootColumns = MetadataService.loadColumnInfo(profile, rootTable);
                List<MetadataService.ForeignKey> relationships =
                        MetadataService.loadRelatedForeignKeys(profile, rootTable);

                // Split into "parents" (tables this one references) and
                // "children" (tables that reference this one), each capped.
                Set<String> parentNames = new LinkedHashSet<>();
                Set<String> childNames = new LinkedHashSet<>();
                for (MetadataService.ForeignKey fk : relationships) {
                    if (fk.fromTable().equalsIgnoreCase(rootTable.getName())) {
                        parentNames.add(fk.toTable());
                    } else {
                        childNames.add(fk.fromTable());
                    }
                }
                trimTo(parentNames, MAX_RELATED_TABLES / 2 + 1);
                trimTo(childNames, MAX_RELATED_TABLES / 2 + 1);

                Map<String, List<MetadataService.ColumnInfo>> relatedColumns = new LinkedHashMap<>();
                for (String name : parentNames) loadIfAbsent(relatedColumns, name);
                for (String name : childNames) loadIfAbsent(relatedColumns, name);

                Platform.runLater(() -> render(rootColumns, relatedColumns, parentNames, childNames, relationships));
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> diagramPane.getChildren().setAll(errorLabel(msg)));
            }
        });
    }

    private void loadIfAbsent(Map<String, List<MetadataService.ColumnInfo>> map, String tableName) {
        if (map.containsKey(tableName)) return;
        try {
            DbObject ref = new DbObject(tableName, DbObject.Kind.TABLE, rootTable.getCatalog(), rootTable.getSchema());
            map.put(tableName, MetadataService.loadColumnInfo(profile, ref));
        } catch (Exception ignored) {
            map.put(tableName, List.of());
        }
    }

    private static void trimTo(Set<String> set, int max) {
        if (set.size() <= max) return;
        List<String> asList = new ArrayList<>(set);
        set.clear();
        set.addAll(asList.subList(0, max));
    }

    private Label errorLabel(String msg) {
        Label l = new Label("Could not load diagram: " + msg);
        l.setLayoutX(20);
        l.setLayoutY(20);
        return l;
    }

    // -------------------------------------------------------------- render

    private void render(List<MetadataService.ColumnInfo> rootColumns,
                        Map<String, List<MetadataService.ColumnInfo>> relatedColumns,
                        Set<String> parentNames, Set<String> childNames,
                        List<MetadataService.ForeignKey> relationships) {
        diagramPane.getChildren().clear();
        columnAnchors.clear();

        double centerX = 900;
        double rootY = 500;
        VBox rootBox = buildTableBox(rootTable.getName(), rootColumns, true);
        diagramPane.getChildren().add(rootBox);
        placeAfterLayout(rootBox, centerX - BOX_WIDTH / 2, rootY);

        layoutRow(parentNames, relatedColumns, centerX, 60, 40);
        layoutRow(childNames, relatedColumns, centerX, rootY + estimatedHeight(rootColumns) + 80, 40);

        // Draw connectors once every box has been laid out (next pulse).
        Platform.runLater(() -> drawConnectors(relationships));
    }

    private void layoutRow(Set<String> names, Map<String, List<MetadataService.ColumnInfo>> relatedColumns,
                           double centerX, double y, double gap) {
        if (names.isEmpty()) return;
        List<VBox> boxes = new ArrayList<>();
        for (String name : names) {
            VBox box = buildTableBox(name, relatedColumns.getOrDefault(name, List.of()), false);
            diagramPane.getChildren().add(box);
            boxes.add(box);
        }
        double totalWidth = boxes.size() * BOX_WIDTH + (boxes.size() - 1) * gap;
        double startX = centerX - totalWidth / 2;
        for (int i = 0; i < boxes.size(); i++) {
            double x = startX + i * (BOX_WIDTH + gap);
            placeAfterLayout(boxes.get(i), x, y);
        }
    }

    private void placeAfterLayout(VBox box, double x, double y) {
        box.setLayoutX(x);
        box.setLayoutY(y);
    }

    private double estimatedHeight(List<MetadataService.ColumnInfo> columns) {
        return 30 + columns.size() * ROW_HEIGHT;
    }

    private VBox buildTableBox(String tableName, List<MetadataService.ColumnInfo> columns, boolean isRoot) {
        return DiagramBoxRenderer.buildTableBox(tableName, columns, isRoot, BOX_WIDTH, columnAnchors);
    }

    private void drawConnectors(List<MetadataService.ForeignKey> relationships) {
        for (MetadataService.ForeignKey fk : relationships) {
            Label from = columnAnchors.get(fk.fromTable().toLowerCase(java.util.Locale.ROOT) + "."
                    + fk.fromColumn().toLowerCase(java.util.Locale.ROOT));
            Label to = columnAnchors.get(fk.toTable().toLowerCase(java.util.Locale.ROOT) + "."
                    + fk.toColumn().toLowerCase(java.util.Locale.ROOT));
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

            Label label = new Label(fk.fromColumn() + ":" + fk.toColumn());
            label.getStyleClass().add("diagram-connector-label");
            label.setLayoutX((line.getStartX() + line.getEndX()) / 2 - 20);
            label.setLayoutY((line.getStartY() + line.getEndY()) / 2 - 8);

            diagramPane.getChildren().addAll(line, label);
            line.toBack();
        }
    }
}

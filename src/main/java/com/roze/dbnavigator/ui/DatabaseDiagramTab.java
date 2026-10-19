package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.ui.diagram.DiagramCanvas;
import com.roze.dbnavigator.ui.diagram.DiagramGraph;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.ArrayList;
import java.util.List;

/**
 * DataGrip-grade ER diagram tab for the whole database/schema.
 * Uses Sugiyama hierarchical layout, orthogonal edge routing, and interactive DDL definition popups.
 */
public class DatabaseDiagramTab extends Tab {

    private static final int MAX_TABLES = 120;

    private final ConnectionProfile profile;
    private final String catalog;
    private final DiagramCanvas canvas;
    private final VBox loadingBox = new VBox(12);

    public DatabaseDiagramTab(ConnectionProfile profile, String catalog) {
        this.profile = profile;
        this.catalog = catalog;

        setText((catalog != null ? catalog : profile.getName()) + " (all tables)");
        setGraphic(Icons.of(FontAwesomeSolid.PROJECT_DIAGRAM, "#c77dbb", 11));

        this.canvas = new DiagramCanvas(profile, catalog);

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(28, 28);
        Label loadingLbl = new Label("Loading database schema and analyzing relationships\u2026");
        loadingLbl.getStyleClass().add("console-status");
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.getChildren().addAll(pi, loadingLbl);

        setContent(loadingBox);
        reload();
    }

    public void reload() {
        setContent(loadingBox);

        AppExecutor.run(() -> {
            try {
                List<DbObject> allTables = MetadataService.loadDatabaseTables(profile, catalog);
                boolean truncated = allTables.size() > MAX_TABLES;
                List<DbObject> tables = new ArrayList<>(
                        truncated ? allTables.subList(0, MAX_TABLES) : allTables);

                tables.sort((a, b) -> {
                    String an = (a.getSchema() != null ? a.getSchema() + "." : "") + a.getName();
                    String bn = (b.getSchema() != null ? b.getSchema() + "." : "") + b.getName();
                    return an.compareToIgnoreCase(bn);
                });

                DiagramGraph graph = new DiagramGraph();

                for (DbObject table : tables) {
                    List<MetadataService.ColumnInfo> cols;
                    try {
                        cols = MetadataService.loadColumnInfo(profile, table);
                    } catch (Exception ignored) {
                        cols = List.of();
                    }
                    graph.addNode(table, cols);
                }

                List<MetadataService.ForeignKey> relationships =
                        MetadataService.loadForeignKeysForTables(profile, catalog, tables);

                for (MetadataService.ForeignKey fk : relationships) {
                    graph.addEdge(fk.fromTable(), fk.fromColumn(),
                            fk.toTable(), fk.toColumn(), fk.constraintName());
                }

                int total = allTables.size();
                Platform.runLater(() -> {
                    canvas.setGraph(graph, truncated, total);
                    setContent(canvas);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    Label err = new Label("Could not load diagram: " + msg);
                    err.getStyleClass().add("console-status");
                    VBox errorBox = new VBox(err);
                    errorBox.setAlignment(Pos.CENTER);
                    setContent(errorBox);
                });
            }
        });
    }

    public DiagramCanvas getCanvas() {
        return canvas;
    }
}

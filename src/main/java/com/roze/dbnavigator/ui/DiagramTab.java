package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.db.VirtualForeignKeyService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DataGrip-grade ER diagram tab focused on a single table and its 1-hop relationships.
 * Uses Sugiyama hierarchical layout, orthogonal edge routing, and interactive DDL definition popups.
 */
public class DiagramTab extends Tab {

    private static final int MAX_RELATED_TABLES = 16;

    private final ConnectionProfile profile;
    private final DbObject rootTable;
    private final DiagramCanvas canvas;
    private final VBox loadingBox = new VBox(12);

    public DiagramTab(ConnectionProfile profile, DbObject rootTable) {
        this.profile = profile;
        this.rootTable = rootTable;

        setText(rootTable.getName());
        setGraphic(Icons.of(FontAwesomeSolid.PROJECT_DIAGRAM, "#c77dbb", 11));

        this.canvas = new DiagramCanvas(profile, rootTable.getCatalog());

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(28, 28);
        Label loadingLbl = new Label("Loading diagram for " + rootTable.getName() + "\u2026");
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
                List<MetadataService.ColumnInfo> rootColumns = MetadataService.loadColumnInfo(profile, rootTable);
                List<MetadataService.ForeignKey> relationships = new ArrayList<>(
                        MetadataService.loadRelatedForeignKeys(profile, rootTable));

                List<MetadataService.ForeignKey> virtualFks =
                        VirtualForeignKeyService.inferRelatedForeignKeys(profile, rootTable, rootColumns, relationships);
                relationships.addAll(virtualFks);

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

                DiagramGraph graph = new DiagramGraph();
                graph.addNode(rootTable, rootColumns);

                Set<String> allRelated = new LinkedHashSet<>();
                allRelated.addAll(parentNames);
                allRelated.addAll(childNames);

                for (String relName : allRelated) {
                    DbObject refObj = new DbObject(relName, DbObject.Kind.TABLE, rootTable.getCatalog(), rootTable.getSchema());
                    List<MetadataService.ColumnInfo> cols;
                    try {
                        cols = MetadataService.loadColumnInfo(profile, refObj);
                    } catch (Exception ignored) {
                        cols = List.of();
                    }
                    graph.addNode(refObj, cols);
                }

                for (MetadataService.ForeignKey fk : relationships) {
                    if (graph.getNode(fk.fromTable()) != null && graph.getNode(fk.toTable()) != null) {
                        graph.addEdge(fk.fromTable(), fk.fromColumn(),
                                fk.toTable(), fk.toColumn(), fk.constraintName());
                    }
                }

                Platform.runLater(() -> {
                    canvas.setGraph(graph, false, allRelated.size() + 1);
                    canvas.selectNode(graph.getNode(rootTable.getName()));
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

    private static void trimTo(Set<String> set, int max) {
        if (set.size() <= max) return;
        List<String> asList = new ArrayList<>(set);
        set.clear();
        set.addAll(asList.subList(0, max));
    }

    public DiagramCanvas getCanvas() {
        return canvas;
    }
}

package com.roze.dbnavigator.ui.diagram;

import com.roze.dbnavigator.db.DdlGeneratorService;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.ui.DiagramExport;
import com.roze.dbnavigator.ui.Icons;
import com.roze.dbnavigator.ui.MainWindow;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Interactive DataGrip-grade ER diagram canvas component supporting:
 * - Sugiyama hierarchical layout, orthogonal compact, and organic force-directed modes
 * - Orthogonal Manhattan 90° edge routing with exact column-to-column port anchoring
 * - Interactive node dragging with real-time dynamic edge re-routing
 * - Table & edge selection with bi-directional relationship illumination
 * - Quick Definition / DDL popup matching DataGrip Images 3 & 4
 * - Full DataGrip right-click context menu matching Image 5
 * - Blueprint dot-grid canvas background
 * - Table search & filter field in toolbar
 * - Export to PNG & Copy to Clipboard
 */
public class DiagramCanvas extends BorderPane {

    private final ConnectionProfile profile;
    private final String catalog;
    private DiagramGraph graph;
    private DiagramLayoutEngine.LayoutMode currentLayoutMode = DiagramLayoutEngine.LayoutMode.HIERARCHICAL_TOP_TO_BOTTOM;

    // Visual elements
    private final Pane diagramPane = new Pane();
    private final Group zoomGroup = new Group();
    private final Scale scale = new Scale(1.0, 1.0, 0, 0);
    private final ScrollPane scrollPane = new ScrollPane(zoomGroup);

    // Node & Edge views
    private final Map<DiagramGraph.Node, VBox> nodeBoxes = new LinkedHashMap<>();
    private final List<DiagramEdgeRouter.EdgeView> edgeViews = new ArrayList<>();
    private final Map<String, Label> columnAnchors = new LinkedHashMap<>();

    // State
    private DiagramGraph.Node selectedNode = null;
    private DiagramEdgeRouter.EdgeView selectedEdge = null;
    private DiagramDdlPopup activeDdlPopup = null;
    private boolean showGrid = true;
    private boolean showEdgeLabels = true;
    private boolean showColumnTypes = true;

    // Toolbar controls
    private final TextField searchField = new TextField();
    private final Label statusLabel = new Label();

    // Drag-to-pan state on empty canvas
    private double panStartX, panStartY;
    private double scrollHStart, scrollVStart;

    public DiagramCanvas(ConnectionProfile profile, String catalog) {
        this.profile = profile;
        this.catalog = catalog;

        getStyleClass().add("diagram-canvas-root");

        // Center scroll area
        setupDiagramArea();

        // Top toolbar
        setTop(buildToolbar());
        setCenter(scrollPane);

        // Global key bindings
        setupKeyBindings();

        // Right-click context menu on canvas
        setupContextMenu();
    }

    private void setupDiagramArea() {
        diagramPane.getStyleClass().add("diagram-pane");
        diagramPane.setPrefSize(4000, 3000);

        zoomGroup.getChildren().add(diagramPane);
        zoomGroup.getTransforms().add(scale);

        scrollPane.setPannable(false); // custom pan handler handles both pan and node dragging smoothly
        scrollPane.getStyleClass().add("diagram-scroll");

        // Pan with middle mouse or left click on empty background
        diagramPane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.MIDDLE) {
                if (e.getTarget() == diagramPane) {
                    clearSelection();
                    panStartX = e.getSceneX();
                    panStartY = e.getSceneY();
                    scrollHStart = scrollPane.getHvalue();
                    scrollVStart = scrollPane.getVvalue();
                }
            }
        });

        diagramPane.setOnMouseDragged(e -> {
            if (e.getTarget() == diagramPane && (e.isPrimaryButtonDown() || e.isMiddleButtonDown())) {
                double deltaX = (e.getSceneX() - panStartX) / (diagramPane.getWidth() * scale.getX());
                double deltaY = (e.getSceneY() - panStartY) / (diagramPane.getHeight() * scale.getY());
                scrollPane.setHvalue(Math.max(0, Math.min(1.0, scrollHStart - deltaX)));
                scrollPane.setVvalue(Math.max(0, Math.min(1.0, scrollVStart - deltaY)));
            }
        });

        // Zoom with Ctrl + Scroll
        scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                zoomBy(e.getDeltaY() > 0 ? 1.08 : 1 / 1.08);
                e.consume();
            }
        });
    }

    private HBox buildToolbar() {
        Button zoomIn = toolbarButton(FontAwesomeSolid.SEARCH_PLUS, "Zoom in (Ctrl++)", () -> zoomBy(1.15));
        Button zoomOut = toolbarButton(FontAwesomeSolid.SEARCH_MINUS, "Zoom out (Ctrl+-)", () -> zoomBy(1 / 1.15));
        Button resetZoom = toolbarButton(FontAwesomeSolid.EXPAND, "Reset zoom (1:1)", this::resetZoom);
        Button fitZoom = toolbarButton(FontAwesomeSolid.COMPRESS_ARROWS_ALT, "Fit to content", this::zoomToFit);

        // Layout dropdown menu button
        Button layoutBtn = new Button("Hierarchical");
        layoutBtn.setGraphic(Icons.of(FontAwesomeSolid.SITEMAP, "#3574f0", 11));
        layoutBtn.getStyleClass().add("diagram-toolbar-btn");
        setupLayoutButton(layoutBtn);

        Button autoRouteBtn = toolbarButton(FontAwesomeSolid.RANDOM, "Auto Route Edges (F5)", this::autoRouteEdges);
        Button showDdlBtn = toolbarButton(FontAwesomeSolid.CODE, "Show DDL / Table Definition (Space)", this::showSelectedTableDdl);

        // Search field to filter / highlight tables
        searchField.setPromptText("Filter tables\u2026");
        searchField.setPrefWidth(140);
        searchField.getStyleClass().add("diagram-search-field");
        searchField.textProperty().addListener((obs, old, text) -> applySearchFilter(text));

        Button exportPng = toolbarButton(FontAwesomeSolid.FILE_IMAGE, "Export Diagram as PNG", this::exportToPng);
        Button copyClip = toolbarButton(FontAwesomeSolid.COPY, "Copy Diagram to Clipboard", this::copyDiagramToClipboard);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel.getStyleClass().add("console-status");

        HBox toolbar = new HBox(6,
                zoomIn, zoomOut, resetZoom, fitZoom,
                new Label("|"),
                layoutBtn, autoRouteBtn, showDdlBtn,
                new Label("|"),
                searchField, exportPng, copyClip,
                spacer, statusLabel
        );
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");
        return toolbar;
    }

    private void setupLayoutButton(Button btn) {
        ContextMenu menu = new ContextMenu();

        MenuItem hiTop = new MenuItem("Hierarchical (Top to Bottom)");
        hiTop.setOnAction(e -> {
            btn.setText("Hierarchical");
            setLayoutMode(DiagramLayoutEngine.LayoutMode.HIERARCHICAL_TOP_TO_BOTTOM);
        });

        MenuItem hiLeft = new MenuItem("Hierarchical (Left to Right)");
        hiLeft.setOnAction(e -> {
            btn.setText("Hierarchical (LTR)");
            setLayoutMode(DiagramLayoutEngine.LayoutMode.HIERARCHICAL_LEFT_TO_RIGHT);
        });

        MenuItem ortho = new MenuItem("Orthogonal (Compact Grid)");
        ortho.setOnAction(e -> {
            btn.setText("Orthogonal");
            setLayoutMode(DiagramLayoutEngine.LayoutMode.ORTHOGONAL_COMPACT);
        });

        MenuItem organic = new MenuItem("Organic (Force Directed)");
        organic.setOnAction(e -> {
            btn.setText("Organic");
            setLayoutMode(DiagramLayoutEngine.LayoutMode.ORGANIC);
        });

        menu.getItems().addAll(hiTop, hiLeft, ortho, organic);
        btn.setOnAction(e -> menu.show(btn, javafx.geometry.Side.BOTTOM, 0, 0));
    }

    private Button toolbarButton(FontAwesomeSolid icon, String tooltip, Runnable action) {
        Button b = new Button();
        b.setGraphic(Icons.of(icon, "#a9b7c6", 12));
        b.setTooltip(new Tooltip(tooltip));
        b.getStyleClass().add("diagram-toolbar-btn");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void setupKeyBindings() {
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F5) {
                if (e.isShiftDown()) {
                    applyLayoutAndRender();
                } else {
                    autoRouteEdges();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.SPACE && selectedNode != null) {
                showSelectedTableDdl();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                if (activeDdlPopup != null) {
                    closeDdlPopup();
                } else {
                    clearSelection();
                }
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.C) {
                copyDiagramToClipboard();
                e.consume();
            }
        });
    }

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();

        // Selected Element Actions
        Menu elemActions = new Menu("Selected Element Actions");
        MenuItem showDdlItem = new MenuItem("Show DDL (Quick Definition)");
        showDdlItem.setOnAction(e -> showSelectedTableDdl());
        MenuItem openConsoleItem = new MenuItem("Open in Query Console");
        openConsoleItem.setOnAction(e -> {
            if (selectedNode != null && profile != null) {
                MainWindow mw = MainWindow.getInstance();
                if (mw != null) {
                    mw.openQueryTab(profile, selectedNode.getTable().getCatalog(),
                            "SELECT * FROM " + selectedNode.getTableName() + " LIMIT 100;");
                }
            }
        });
        elemActions.getItems().addAll(showDdlItem, openConsoleItem);

        // Layout options
        Menu layoutMenu = new Menu("Layout");
        MenuItem lTop = new MenuItem("Hierarchical (Top to Bottom)");
        lTop.setOnAction(e -> setLayoutMode(DiagramLayoutEngine.LayoutMode.HIERARCHICAL_TOP_TO_BOTTOM));
        MenuItem lLeft = new MenuItem("Hierarchical (Left to Right)");
        lLeft.setOnAction(e -> setLayoutMode(DiagramLayoutEngine.LayoutMode.HIERARCHICAL_LEFT_TO_RIGHT));
        MenuItem lOrtho = new MenuItem("Orthogonal (Compact Grid)");
        lOrtho.setOnAction(e -> setLayoutMode(DiagramLayoutEngine.LayoutMode.ORTHOGONAL_COMPACT));
        MenuItem lOrg = new MenuItem("Organic (Force Directed)");
        lOrg.setOnAction(e -> setLayoutMode(DiagramLayoutEngine.LayoutMode.ORGANIC));
        layoutMenu.getItems().addAll(lTop, lLeft, lOrtho, lOrg);

        // Orientation options
        Menu orientMenu = new Menu("Orientation");
        MenuItem oTB = new MenuItem("Top to Bottom");
        oTB.setOnAction(e -> setLayoutMode(DiagramLayoutEngine.LayoutMode.HIERARCHICAL_TOP_TO_BOTTOM));
        MenuItem oLR = new MenuItem("Left to Right");
        oLR.setOnAction(e -> setLayoutMode(DiagramLayoutEngine.LayoutMode.HIERARCHICAL_LEFT_TO_RIGHT));
        orientMenu.getItems().addAll(oTB, oLR);

        MenuItem applyLayout = new MenuItem("Apply Current Layout (Shift+F5)");
        applyLayout.setOnAction(e -> applyLayoutAndRender());

        MenuItem autoRoute = new MenuItem("Auto Route Edges (F5)");
        autoRoute.setOnAction(e -> autoRouteEdges());

        // Appearance
        Menu appearanceMenu = new Menu("Appearance");
        CheckMenuItem gridItem = new CheckMenuItem("Show Grid");
        gridItem.setSelected(showGrid);
        gridItem.setOnAction(e -> {
            showGrid = gridItem.isSelected();
            updateGridAppearance();
        });

        CheckMenuItem edgeLabelsItem = new CheckMenuItem("Show Edge Labels");
        edgeLabelsItem.setSelected(showEdgeLabels);
        edgeLabelsItem.setOnAction(e -> {
            showEdgeLabels = edgeLabelsItem.isSelected();
            autoRouteEdges();
        });

        CheckMenuItem columnTypesItem = new CheckMenuItem("Show Column Types");
        columnTypesItem.setSelected(showColumnTypes);
        columnTypesItem.setOnAction(e -> {
            showColumnTypes = columnTypesItem.isSelected();
            updateColumnTypesVisibility();
        });
        appearanceMenu.getItems().addAll(gridItem, edgeLabelsItem, columnTypesItem);

        MenuItem copyDiag = new MenuItem("Copy Diagram to Clipboard");
        copyDiag.setOnAction(e -> copyDiagramToClipboard());

        MenuItem copySel = new MenuItem("Copy Selection to Clipboard");
        copySel.setOnAction(e -> copySelectionToClipboard());

        Menu exportMenu = new Menu("Export Diagram");
        MenuItem expPng = new MenuItem("Export as PNG…");
        expPng.setOnAction(e -> exportToPng());
        exportMenu.getItems().add(expPng);

        menu.getItems().addAll(
                elemActions,
                new SeparatorMenuItem(),
                layoutMenu,
                orientMenu,
                applyLayout,
                autoRoute,
                new SeparatorMenuItem(),
                appearanceMenu,
                new SeparatorMenuItem(),
                copyDiag,
                copySel,
                exportMenu
        );

        diagramPane.setOnContextMenuRequested(e -> {
            menu.show(diagramPane, e.getScreenX(), e.getScreenY());
        });
    }

    private void updateGridAppearance() {
        if (showGrid) {
            if (!diagramPane.getStyleClass().contains("diagram-canvas-grid")) {
                diagramPane.getStyleClass().add("diagram-canvas-grid");
            }
        } else {
            diagramPane.getStyleClass().remove("diagram-canvas-grid");
        }
    }

    private void updateColumnTypesVisibility() {
        for (VBox box : nodeBoxes.values()) {
            for (Node child : box.getChildren()) {
                if (child instanceof HBox row) {
                    for (Node n : row.getChildren()) {
                        if (n.getStyleClass().contains("diagram-column-type")) {
                            n.setVisible(showColumnTypes);
                            n.setManaged(showColumnTypes);
                        }
                    }
                }
            }
        }
        autoRouteEdges();
    }

    // =========================================================================
    // POPULATION & RENDERING
    // =========================================================================

    public void setGraph(DiagramGraph graph, boolean truncated, int totalTableCount) {
        this.graph = graph;
        statusLabel.setText(truncated
                ? "Showing first " + graph.getNodes().size() + " of " + totalTableCount + " tables (diagram capped for readability)"
                : graph.getNodes().size() + " table(s), " + graph.getEdges().size() + " relationship(s)");

        applyLayoutAndRender();
        updateGridAppearance();
    }

    public void setLayoutMode(DiagramLayoutEngine.LayoutMode mode) {
        this.currentLayoutMode = mode;
        applyLayoutAndRender();
    }

    public void applyLayoutAndRender() {
        if (graph == null) return;

        // Apply algorithmic layout to graph nodes
        DiagramLayoutEngine.applyLayout(graph, currentLayoutMode);

        renderGraph();
    }

    private void renderGraph() {
        diagramPane.getChildren().clear();
        nodeBoxes.clear();
        edgeViews.clear();
        columnAnchors.clear();
        closeDdlPopup();

        if (graph == null || graph.getNodes().isEmpty()) {
            Label empty = new Label("No tables found to display");
            empty.setLayoutX(40);
            empty.setLayoutY(40);
            diagramPane.getChildren().add(empty);
            return;
        }

        // Render table boxes
        for (DiagramGraph.Node node : graph.getNodes()) {
            VBox box = buildInteractiveNodeBox(node);
            nodeBoxes.put(node, box);
            diagramPane.getChildren().add(box);

            box.setLayoutX(node.getX());
            box.setLayoutY(node.getY());
        }

        // Route orthogonal edges
        autoRouteEdges();
    }

    private VBox buildInteractiveNodeBox(DiagramGraph.Node node) {
        VBox box = new VBox();
        box.setPrefWidth(node.getWidth());
        box.getStyleClass().add("diagram-table-box");

        // Header: icon + table name + quick DDL button
        Node tableIcon = Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12);
        Label titleLabel = new Label(node.getTableName());
        titleLabel.getStyleClass().add("diagram-table-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button quickDdl = new Button();
        quickDdl.setGraphic(Icons.of(FontAwesomeSolid.CODE, "#a9b7c6", 10));
        quickDdl.setTooltip(new Tooltip("Quick Definition / DDL (Space)"));
        quickDdl.getStyleClass().add("diagram-node-ddl-btn");
        quickDdl.setOnAction(e -> {
            selectNode(node);
            showSelectedTableDdl();
            e.consume();
        });

        HBox header = new HBox(6, tableIcon, titleLabel, spacer, quickDdl);
        header.getStyleClass().add("diagram-table-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 8, 6, 8));
        box.getChildren().add(header);

        // Columns
        for (MetadataService.ColumnInfo col : node.getColumns()) {
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
            typeLabel.setVisible(showColumnTypes);
            typeLabel.setManaged(showColumnTypes);

            row.getChildren().addAll(nameLabel, gap, typeLabel);
            box.getChildren().add(row);

            columnAnchors.put(node.getTableName().toLowerCase(Locale.ROOT) + "."
                    + col.name().toLowerCase(Locale.ROOT), nameLabel);
        }

        // Selection handling on click
        box.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.getClickCount() == 2) {
                    selectNode(node);
                    showSelectedTableDdl();
                } else {
                    selectNode(node);
                }
                e.consume();
            }
        });

        // Draggable table box with live edge re-routing
        final double[] dragDelta = new double[2];
        box.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragDelta[0] = box.getLayoutX() - e.getSceneX();
                dragDelta[1] = box.getLayoutY() - e.getSceneY();
                selectNode(node);
                box.toFront();
                e.consume();
            }
        });

        box.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double newX = Math.max(10, (e.getSceneX() + dragDelta[0]));
                double newY = Math.max(10, (e.getSceneY() + dragDelta[1]));
                box.setLayoutX(newX);
                box.setLayoutY(newY);
                node.setX(newX);
                node.setY(newY);

                // Live re-route connected edges
                reRouteEdgesForNode(node);
                e.consume();
            }
        });

        return box;
    }

    public void autoRouteEdges() {
        if (graph == null) return;

        // Remove old edge views
        for (DiagramEdgeRouter.EdgeView ev : edgeViews) {
            diagramPane.getChildren().remove(ev);
        }
        edgeViews.clear();

        // Route each edge orthogonally
        for (DiagramGraph.Edge edge : graph.getEdges()) {
            DiagramEdgeRouter.EdgeView ev = DiagramEdgeRouter.routeEdge(edge, showEdgeLabels);
            edgeViews.add(ev);
            diagramPane.getChildren().add(ev);
            ev.toBack();

            // Click handling on edge
            ev.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    selectEdge(ev);
                    e.consume();
                }
            });
        }

        // Restore highlight if a node or edge was selected
        if (selectedNode != null) {
            highlightConnected(selectedNode);
        } else if (selectedEdge != null) {
            highlightEdge(selectedEdge);
        }
    }

    private void reRouteEdgesForNode(DiagramGraph.Node node) {
        if (graph == null) return;

        List<DiagramGraph.Edge> connected = graph.getConnectedEdges(node);
        for (DiagramGraph.Edge edge : connected) {
            // Find existing view
            DiagramEdgeRouter.EdgeView oldView = null;
            for (DiagramEdgeRouter.EdgeView ev : edgeViews) {
                if (ev.getEdge() == edge) {
                    oldView = ev;
                    break;
                }
            }

            if (oldView != null) {
                diagramPane.getChildren().remove(oldView);
                edgeViews.remove(oldView);

                DiagramEdgeRouter.EdgeView newView = DiagramEdgeRouter.routeEdge(edge, showEdgeLabels);
                edgeViews.add(newView);
                diagramPane.getChildren().add(newView);
                newView.toBack();

                if (selectedNode != null && (edge.getFromNode() == selectedNode || edge.getToNode() == selectedNode)) {
                    newView.setHighlighted(true);
                }

                newView.setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY) {
                        selectEdge(newView);
                        e.consume();
                    }
                });
            }
        }
    }

    // =========================================================================
    // SELECTION & ILLUMINATION
    // =========================================================================

    public void selectNode(DiagramGraph.Node node) {
        clearSelectionStyles();
        selectedNode = node;
        selectedEdge = null;

        if (node == null) return;

        VBox box = nodeBoxes.get(node);
        if (box != null) {
            box.getStyleClass().add("diagram-table-box-selected");
            box.toFront();
        }

        highlightConnected(node);
    }

    public void selectEdge(DiagramEdgeRouter.EdgeView edgeView) {
        clearSelectionStyles();
        selectedEdge = edgeView;
        selectedNode = null;

        if (edgeView == null) return;
        highlightEdge(edgeView);
    }

    public void clearSelection() {
        clearSelectionStyles();
        selectedNode = null;
        selectedEdge = null;
    }

    private void clearSelectionStyles() {
        for (VBox box : nodeBoxes.values()) {
            box.getStyleClass().remove("diagram-table-box-selected");
            box.getStyleClass().remove("diagram-table-box-connected");
        }
        for (DiagramEdgeRouter.EdgeView ev : edgeViews) {
            ev.setHighlighted(false);
        }
    }

    private void highlightConnected(DiagramGraph.Node node) {
        if (graph == null || node == null) return;

        List<DiagramGraph.Edge> connectedEdges = graph.getConnectedEdges(node);
        Set<DiagramGraph.Node> connectedNodes = new HashSet<>();

        for (DiagramEdgeRouter.EdgeView ev : edgeViews) {
            if (connectedEdges.contains(ev.getEdge())) {
                ev.setHighlighted(true);
                DiagramGraph.Node other = (ev.getEdge().getFromNode() == node)
                        ? ev.getEdge().getToNode()
                        : ev.getEdge().getFromNode();
                if (other != null && other != node) {
                    connectedNodes.add(other);
                }
            }
        }

        for (DiagramGraph.Node cNode : connectedNodes) {
            VBox cBox = nodeBoxes.get(cNode);
            if (cBox != null && !cBox.getStyleClass().contains("diagram-table-box-connected")) {
                cBox.getStyleClass().add("diagram-table-box-connected");
            }
        }
    }

    private void highlightEdge(DiagramEdgeRouter.EdgeView edgeView) {
        edgeView.setHighlighted(true);

        DiagramGraph.Node from = edgeView.getEdge().getFromNode();
        DiagramGraph.Node to = edgeView.getEdge().getToNode();

        if (from != null && nodeBoxes.containsKey(from)) {
            nodeBoxes.get(from).getStyleClass().add("diagram-table-box-connected");
        }
        if (to != null && nodeBoxes.containsKey(to)) {
            nodeBoxes.get(to).getStyleClass().add("diagram-table-box-connected");
        }
    }

    // =========================================================================
    // QUICK DEFINITION / DDL POPUP (Images 3 & 4)
    // =========================================================================

    public void showSelectedTableDdl() {
        if (selectedNode == null) return;
        closeDdlPopup();

        activeDdlPopup = new DiagramDdlPopup(profile, selectedNode.getTable(), this::closeDdlPopup);

        // Position neatly next to the selected table box
        VBox box = nodeBoxes.get(selectedNode);
        double targetX = (box != null) ? box.getLayoutX() + box.getWidth() + 20.0 : 100.0;
        double targetY = (box != null) ? box.getLayoutY() : 100.0;

        activeDdlPopup.setLayoutX(targetX);
        activeDdlPopup.setLayoutY(targetY);

        diagramPane.getChildren().add(activeDdlPopup);
        activeDdlPopup.toFront();
    }

    public void closeDdlPopup() {
        if (activeDdlPopup != null) {
            diagramPane.getChildren().remove(activeDdlPopup);
            activeDdlPopup = null;
        }
    }

    // =========================================================================
    // SEARCH & FILTER
    // =========================================================================

    private void applySearchFilter(String query) {
        if (query == null || query.isBlank()) {
            for (VBox b : nodeBoxes.values()) {
                b.setOpacity(1.0);
            }
            return;
        }

        String q = query.trim().toLowerCase(Locale.ROOT);
        DiagramGraph.Node firstMatch = null;

        for (Map.Entry<DiagramGraph.Node, VBox> entry : nodeBoxes.entrySet()) {
            DiagramGraph.Node node = entry.getKey();
            VBox box = entry.getValue();

            boolean matches = node.getTableName().toLowerCase(Locale.ROOT).contains(q);
            if (!matches) {
                // Check if any column name matches
                for (MetadataService.ColumnInfo col : node.getColumns()) {
                    if (col.name().toLowerCase(Locale.ROOT).contains(q)) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) {
                box.setOpacity(1.0);
                if (firstMatch == null) firstMatch = node;
            } else {
                box.setOpacity(0.25);
            }
        }

        if (firstMatch != null) {
            selectNode(firstMatch);
        }
    }

    // =========================================================================
    // ZOOM & NAVIGATION
    // =========================================================================

    public void zoomBy(double factor) {
        double newX = clampZoom(scale.getX() * factor);
        double newY = clampZoom(scale.getY() * factor);
        scale.setX(newX);
        scale.setY(newY);
    }

    public void resetZoom() {
        scale.setX(1.0);
        scale.setY(1.0);
    }

    public void zoomToFit() {
        if (graph == null || graph.getNodes().isEmpty()) return;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (DiagramGraph.Node n : graph.getNodes()) {
            minX = Math.min(minX, n.getX());
            minY = Math.min(minY, n.getY());
            maxX = Math.max(maxX, n.getX() + n.getWidth());
            maxY = Math.max(maxY, n.getY() + n.getHeight());
        }

        double contentW = maxX - minX + 100.0;
        double contentH = maxY - minY + 100.0;

        double viewportW = scrollPane.getViewportBounds().getWidth();
        double viewportH = scrollPane.getViewportBounds().getHeight();

        if (viewportW > 0 && viewportH > 0 && contentW > 0 && contentH > 0) {
            double fitScale = Math.min(viewportW / contentW, viewportH / contentH);
            double clamped = clampZoom(fitScale * 0.95);
            scale.setX(clamped);
            scale.setY(clamped);
        }
    }

    private static double clampZoom(double v) {
        return Math.max(0.15, Math.min(3.0, v));
    }

    // =========================================================================
    // EXPORT & CLIPBOARD
    // =========================================================================

    public void exportToPng() {
        DiagramExport.exportToPng(
                getScene() == null ? null : getScene().getWindow(),
                zoomGroup,
                (catalog != null ? catalog : (profile != null ? profile.getName() : "database")) + "-diagram"
        );
    }

    public void copyDiagramToClipboard() {
        try {
            SnapshotParameters params = new SnapshotParameters();
            WritableImage snapshot = zoomGroup.snapshot(params, null);
            ClipboardContent content = new ClipboardContent();
            content.putImage(snapshot);
            Clipboard.getSystemClipboard().setContent(content);
            statusLabel.setText("Diagram copied to clipboard!");
        } catch (Exception ex) {
            statusLabel.setText("Could not copy diagram: " + ex.getMessage());
        }
    }

    public void copySelectionToClipboard() {
        if (selectedNode == null) {
            copyDiagramToClipboard();
            return;
        }
        VBox box = nodeBoxes.get(selectedNode);
        if (box != null) {
            try {
                SnapshotParameters params = new SnapshotParameters();
                WritableImage snapshot = box.snapshot(params, null);
                ClipboardContent content = new ClipboardContent();
                content.putImage(snapshot);
                Clipboard.getSystemClipboard().setContent(content);
                statusLabel.setText("Selected table copied to clipboard!");
            } catch (Exception ex) {
                statusLabel.setText("Could not copy selection: " + ex.getMessage());
            }
        }
    }
}

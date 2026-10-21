package com.roze.dbnavigator.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;

/**
 * Enables smooth mouse drag-and-drop tab reordering (left-to-right and right-to-left)
 * and cross-pane tab dragging, exactly matching DataGrip / IntelliJ tab behavior.
 */
public final class TabDragHandler {

    private static final double DRAG_THRESHOLD = 6.0;

    private final TabPane tabPane;
    private final MainWindow mainWindow;

    private Tab draggedTab;
    private Node draggedHeaderNode;
    private Pane headersRegion;
    private double startSceneX;
    private double startSceneY;
    private boolean isDragging;

    private TabDragHandler(TabPane tabPane, MainWindow mainWindow) {
        this.tabPane = tabPane;
        this.mainWindow = mainWindow;
        installHandlers();
    }

    public static TabDragHandler install(TabPane tabPane, MainWindow mainWindow) {
        return new TabDragHandler(tabPane, mainWindow);
    }

    private void installHandlers() {
        tabPane.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        tabPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        tabPane.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
    }

    private void handleMousePressed(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) {
            resetDrag();
            return;
        }

        Node target = (e.getTarget() instanceof Node n) ? n : null;
        if (target == null) return;

        // If user clicked the close button ('x'), let standard close event handle it
        if (isCloseButton(target)) {
            resetDrag();
            return;
        }

        // Find the enclosing tab header node (.tab)
        Node tabHeader = findTabHeader(target);
        if (tabHeader == null) {
            resetDrag();
            return;
        }

        if (tabHeader.getParent() instanceof Pane parentPane) {
            this.headersRegion = parentPane;
        }

        Tab tab = findTabForHeader(tabHeader);
        if (tab == null) {
            resetDrag();
            return;
        }

        this.draggedTab = tab;
        this.draggedHeaderNode = tabHeader;
        this.startSceneX = e.getSceneX();
        this.startSceneY = e.getSceneY();
        this.isDragging = false;
    }

    private void handleMouseDragged(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY || draggedTab == null) return;

        double deltaX = e.getSceneX() - startSceneX;
        double deltaY = e.getSceneY() - startSceneY;

        if (!isDragging) {
            if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
                isDragging = true;
                tabPane.setCursor(Cursor.CLOSED_HAND);
                if (draggedHeaderNode != null) {
                    draggedHeaderNode.setOpacity(0.72);
                }
                tabPane.getSelectionModel().select(draggedTab);
            }
        }

        if (isDragging) {
            e.consume();

            // 1. Reorder within current TabPane if mouse is over sibling tabs
            reorderWithinTabPane(e.getSceneX());

            // 2. Check cross-pane drag if user dragged tab into another split pane
            if (mainWindow != null) {
                checkCrossPaneDrag(e.getSceneX(), e.getSceneY());
            }
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (isDragging) {
            e.consume();
            if (draggedHeaderNode != null) {
                draggedHeaderNode.setOpacity(1.0);
            }
            tabPane.setCursor(Cursor.DEFAULT);
            if (draggedTab != null) {
                tabPane.getSelectionModel().select(draggedTab);
                if (mainWindow != null) {
                    mainWindow.updateNavigationBarBreadcrumb(draggedTab);
                }
            }
        }
        resetDrag();
    }

    private void resetDrag() {
        if (draggedHeaderNode != null) {
            draggedHeaderNode.setOpacity(1.0);
        }
        tabPane.setCursor(Cursor.DEFAULT);
        this.draggedTab = null;
        this.draggedHeaderNode = null;
        this.isDragging = false;
    }

    private void reorderWithinTabPane(double sceneX) {
        if (draggedTab == null) return;
        List<Node> headers = getTabHeaders();
        int currentIdx = tabPane.getTabs().indexOf(draggedTab);
        if (currentIdx < 0 || headers.size() != tabPane.getTabs().size()) return;

        // Check dragging to the right (higher index)
        for (int i = currentIdx + 1; i < headers.size(); i++) {
            Node nextHeader = headers.get(i);
            Bounds bounds = nextHeader.localToScene(nextHeader.getBoundsInLocal());
            double threshold = bounds.getMinX() + bounds.getWidth() * 0.40;
            if (sceneX > threshold) {
                moveTab(tabPane, currentIdx, i, mainWindow);
                return;
            }
        }

        // Check dragging to the left (lower index)
        for (int i = currentIdx - 1; i >= 0; i--) {
            Node prevHeader = headers.get(i);
            Bounds bounds = prevHeader.localToScene(prevHeader.getBoundsInLocal());
            double threshold = bounds.getMaxX() - bounds.getWidth() * 0.40;
            if (sceneX < threshold) {
                moveTab(tabPane, currentIdx, i, mainWindow);
                return;
            }
        }
    }

    public static void moveTab(TabPane pane, int fromIndex, int toIndex, MainWindow mw) {
        if (pane == null || fromIndex == toIndex) return;
        ObservableList<Tab> tabs = pane.getTabs();
        if (fromIndex < 0 || fromIndex >= tabs.size() || toIndex < 0 || toIndex >= tabs.size()) return;

        Tab tab = tabs.get(fromIndex);
        try {
            if (mw != null) mw.setReorderingTabs(true);
            tabs.remove(fromIndex);
            tabs.add(toIndex, tab);
            pane.getSelectionModel().select(tab);
        } finally {
            if (mw != null) mw.setReorderingTabs(false);
        }

        pane.layout();

        if (mw != null) {
            mw.updateNavigationBarBreadcrumb(tab);
        }
    }

    private void checkCrossPaneDrag(double sceneX, double sceneY) {
        if (mainWindow == null || draggedTab == null) return;
        List<TabPane> panes = mainWindow.editorTabPanes();
        for (TabPane targetPane : panes) {
            if (targetPane == tabPane) continue;
            Bounds bounds = targetPane.localToScene(targetPane.getBoundsInLocal());
            if (bounds.contains(sceneX, sceneY)) {
                // Dragged into another editor TabPane group!
                int fromIndex = tabPane.getTabs().indexOf(draggedTab);
                if (fromIndex >= 0) {
                    try {
                        mainWindow.setReorderingTabs(true);
                        tabPane.getTabs().remove(fromIndex);
                        targetPane.getTabs().add(draggedTab);
                        targetPane.getSelectionModel().select(draggedTab);
                        mainWindow.setActiveTabPane(targetPane);
                    } finally {
                        mainWindow.setReorderingTabs(false);
                    }
                    mainWindow.updateNavigationBarBreadcrumb(draggedTab);
                    resetDrag();
                    return;
                }
            }
        }
    }

    private List<Node> getTabHeaders() {
        if (headersRegion != null) {
            List<Node> list = new ArrayList<>();
            for (Node child : headersRegion.getChildrenUnmodifiable()) {
                if (isTabHeaderNode(child)) {
                    list.add(child);
                }
            }
            if (!list.isEmpty()) return list;
        }

        // Fallback: search tabPane
        List<Node> list = new ArrayList<>();
        for (Node n : tabPane.lookupAll(".tab")) {
            if (isTabHeaderNode(n)) {
                list.add(n);
            }
        }
        return list;
    }

    private Tab findTabForHeader(Node tabHeader) {
        List<Node> headers = getTabHeaders();
        int index = headers.indexOf(tabHeader);
        if (index >= 0 && index < tabPane.getTabs().size()) {
            return tabPane.getTabs().get(index);
        }
        return null;
    }

    private boolean isTabHeaderNode(Node node) {
        return node != null && node.getStyleClass().contains("tab")
                && !node.getStyleClass().contains("tab-pane");
    }

    private Node findTabHeader(Node node) {
        Node current = node;
        while (current != null && current != tabPane) {
            if (isTabHeaderNode(current)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private boolean isCloseButton(Node node) {
        Node current = node;
        while (current != null && current != tabPane) {
            if (current.getStyleClass().contains("tab-close-button")) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}

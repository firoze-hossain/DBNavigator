package com.roze.dbnavigator.ui.diagram;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.DbObject;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DiagramLayoutAndRoutingTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
        try {
            Platform.setImplicitExit(false);
        } catch (Exception ignored) {}
    }

    private DbObject createTable(String name) {
        return new DbObject(name, DbObject.Kind.TABLE, "testdb", "public");
    }

    private List<MetadataService.ColumnInfo> createColumns(String... cols) {
        return java.util.Arrays.stream(cols)
                .map(c -> new MetadataService.ColumnInfo(c, "varchar", 255, true, null, c.equalsIgnoreCase("id")))
                .toList();
    }

    @Test
    public void testDiagramGraphConstructionAndOffset() {
        DiagramGraph graph = new DiagramGraph();

        DbObject users = createTable("users");
        DbObject orders = createTable("orders");

        graph.addNode(users, createColumns("id", "email", "created_at"));
        graph.addNode(orders, createColumns("id", "user_id", "total"));

        graph.addEdge("orders", "user_id", "users", "id", "fk_orders_user");

        assertEquals(2, graph.getNodes().size());
        assertEquals(1, graph.getEdges().size());

        DiagramGraph.Node usersNode = graph.getNode("users");
        DiagramGraph.Node ordersNode = graph.getNode("orders");

        assertNotNull(usersNode);
        assertNotNull(ordersNode);

        // Test column Y offset computation
        double usersIdOffset = usersNode.getColumnYOffset("id");
        double usersEmailOffset = usersNode.getColumnYOffset("email");
        assertTrue(usersEmailOffset > usersIdOffset, "Second column should have greater vertical offset than first");

        // Edge connections
        assertEquals(1, graph.getOutgoingEdges(ordersNode).size());
        assertEquals(1, graph.getIncomingEdges(usersNode).size());
        assertEquals(1, graph.getConnectedEdges(ordersNode).size());
    }

    @Test
    public void testHierarchicalSugiyamaLayoutRanking() {
        DiagramGraph graph = new DiagramGraph();

        // 3-tier hierarchy: users (root) <- workspaces <- projects <- tasks
        graph.addNode(createTable("users"), createColumns("id", "name"));
        graph.addNode(createTable("workspaces"), createColumns("id", "user_id", "name"));
        graph.addNode(createTable("projects"), createColumns("id", "workspace_id", "title"));
        graph.addNode(createTable("tasks"), createColumns("id", "project_id", "status"));

        graph.addEdge("workspaces", "user_id", "users", "id", "fk_ws_user");
        graph.addEdge("projects", "workspace_id", "workspaces", "id", "fk_proj_ws");
        graph.addEdge("tasks", "project_id", "projects", "id", "fk_task_proj");

        DiagramLayoutEngine.applyLayout(graph, DiagramLayoutEngine.LayoutMode.HIERARCHICAL_TOP_TO_BOTTOM);

        DiagramGraph.Node usersNode = graph.getNode("users");
        DiagramGraph.Node wsNode = graph.getNode("workspaces");
        DiagramGraph.Node projNode = graph.getNode("projects");
        DiagramGraph.Node taskNode = graph.getNode("tasks");

        // Root referenced entity (users) must have rank 0 (top-most)
        assertEquals(0, usersNode.getRank());
        assertTrue(wsNode.getRank() > usersNode.getRank(), "workspaces must be on lower rank than users");
        assertTrue(projNode.getRank() > wsNode.getRank(), "projects must be on lower rank than workspaces");
        assertTrue(taskNode.getRank() > projNode.getRank(), "tasks must be on lower rank than projects");

        // Coordinates must reflect hierarchy
        assertTrue(usersNode.getY() < wsNode.getY(), "users Y must be above workspaces Y");
        assertTrue(wsNode.getY() < projNode.getY(), "workspaces Y must be above projects Y");
        assertTrue(projNode.getY() < taskNode.getY(), "projects Y must be above tasks Y");
    }

    @Test
    public void testCycleBreakingInLayout() {
        DiagramGraph graph = new DiagramGraph();

        // Circular reference: Table A references Table B, and Table B references Table A
        graph.addNode(createTable("table_a"), createColumns("id", "b_id"));
        graph.addNode(createTable("table_b"), createColumns("id", "a_id"));

        graph.addEdge("table_a", "b_id", "table_b", "id", "fk_a_b");
        graph.addEdge("table_b", "a_id", "table_a", "id", "fk_b_a");

        // Must not crash or infinitely loop
        assertDoesNotThrow(() ->
                DiagramLayoutEngine.applyLayout(graph, DiagramLayoutEngine.LayoutMode.HIERARCHICAL_TOP_TO_BOTTOM));

        // Both nodes must have assigned coordinates
        DiagramGraph.Node nodeA = graph.getNode("table_a");
        DiagramGraph.Node nodeB = graph.getNode("table_b");
        assertNotNull(nodeA);
        assertNotNull(nodeB);
        assertTrue(nodeA.getY() >= 0 && nodeB.getY() >= 0);
    }

    @Test
    public void testIndependentComponentPartitioning() {
        DiagramGraph graph = new DiagramGraph();

        // Cluster 1: users <- profiles
        graph.addNode(createTable("users"), createColumns("id"));
        graph.addNode(createTable("profiles"), createColumns("id", "user_id"));
        graph.addEdge("profiles", "user_id", "users", "id", "fk_prof_user");

        // Cluster 2: orders <- order_items
        graph.addNode(createTable("orders"), createColumns("id"));
        graph.addNode(createTable("order_items"), createColumns("id", "order_id"));
        graph.addEdge("order_items", "order_id", "orders", "id", "fk_item_order");

        // Standalone table: audit_log (0 FKs)
        graph.addNode(createTable("audit_log"), createColumns("id", "event"));

        DiagramLayoutEngine.applyLayout(graph, DiagramLayoutEngine.LayoutMode.HIERARCHICAL_TOP_TO_BOTTOM);

        DiagramGraph.Node users = graph.getNode("users");
        DiagramGraph.Node orders = graph.getNode("orders");
        DiagramGraph.Node audit = graph.getNode("audit_log");

        // Nodes must not overlap
        assertNotEquals(users.getX(), orders.getX(), "Different clusters should be horizontally separated");
        assertTrue(audit.getY() >= 0);
    }

    @Test
    public void testOrthogonalEdgeRouting() {
        DiagramGraph graph = new DiagramGraph();

        DiagramGraph.Node source = new DiagramGraph.Node(createTable("orders"), createColumns("id", "customer_id"));
        DiagramGraph.Node target = new DiagramGraph.Node(createTable("customers"), createColumns("id", "name"));

        source.setX(100.0);
        source.setY(200.0);
        target.setX(500.0);
        target.setY(100.0);

        DiagramGraph.Edge edge = new DiagramGraph.Edge(source, "customer_id", target, "id", "fk_orders_cust");

        DiagramEdgeRouter.EdgeView edgeView = DiagramEdgeRouter.routeEdge(edge, true);
        assertNotNull(edgeView);
        assertNotNull(edgeView.getPolyline());
        assertNotNull(edgeView.getArrowHead());
        assertNotNull(edgeView.getLabel());

        assertEquals("customer_id:id", edgeView.getLabel().getText());

        // Check orthogonal 90 degree points
        List<DiagramEdgeRouter.Point> pts = edgeView.getRoutePoints();
        assertTrue(pts.size() >= 3, "Orthogonal path must have at least 3 points for Z/S bend");

        for (int i = 0; i < pts.size() - 1; i++) {
            DiagramEdgeRouter.Point pA = pts.get(i);
            DiagramEdgeRouter.Point pB = pts.get(i + 1);
            boolean isHorizontal = Math.abs(pA.y() - pB.y()) < 0.001;
            boolean isVertical = Math.abs(pA.x() - pB.x()) < 0.001;
            assertTrue(isHorizontal || isVertical, "Every segment in orthogonal route must be strictly horizontal or vertical (90 deg bend)");
        }
    }

    @Test
    public void testSelfReferencingEdgeRouting() {
        DiagramGraph.Node node = new DiagramGraph.Node(createTable("categories"), createColumns("id", "parent_id", "title"));
        node.setX(200.0);
        node.setY(200.0);

        DiagramGraph.Edge loopEdge = new DiagramGraph.Edge(node, "parent_id", node, "id", "fk_cat_parent");
        assertTrue(loopEdge.isSelfReference());

        DiagramEdgeRouter.EdgeView edgeView = DiagramEdgeRouter.routeEdge(loopEdge, true);
        assertNotNull(edgeView);
        List<DiagramEdgeRouter.Point> pts = edgeView.getRoutePoints();
        assertEquals(4, pts.size(), "Self-referencing loop must have 4 points");

        for (int i = 0; i < pts.size() - 1; i++) {
            DiagramEdgeRouter.Point pA = pts.get(i);
            DiagramEdgeRouter.Point pB = pts.get(i + 1);
            boolean isHorizontal = Math.abs(pA.y() - pB.y()) < 0.001;
            boolean isVertical = Math.abs(pA.x() - pB.x()) < 0.001;
            assertTrue(isHorizontal || isVertical, "Self-reference segment must be orthogonal");
        }
    }

    @Test
    public void testAlternativeLayoutModes() {
        DiagramGraph graph = new DiagramGraph();
        graph.addNode(createTable("users"), createColumns("id"));
        graph.addNode(createTable("orders"), createColumns("id", "user_id"));
        graph.addEdge("orders", "user_id", "users", "id", "fk_o_u");

        // Test Left to Right
        assertDoesNotThrow(() ->
                DiagramLayoutEngine.applyLayout(graph, DiagramLayoutEngine.LayoutMode.HIERARCHICAL_LEFT_TO_RIGHT));
        assertTrue(graph.getNode("users").getX() < graph.getNode("orders").getX(),
                "In LTR hierarchical, users (PK) should be left of orders (FK)");

        // Test Orthogonal Compact
        assertDoesNotThrow(() ->
                DiagramLayoutEngine.applyLayout(graph, DiagramLayoutEngine.LayoutMode.ORTHOGONAL_COMPACT));

        // Test Organic
        assertDoesNotThrow(() ->
                DiagramLayoutEngine.applyLayout(graph, DiagramLayoutEngine.LayoutMode.ORGANIC));
    }
}

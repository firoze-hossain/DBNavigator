package com.roze.dbnavigator.ui.diagram;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class DiagramCanvasTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
        try {
            Platform.setImplicitExit(false);
        } catch (Exception ignored) {}
    }

    private void runOnFx(Runnable action) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for FX thread");
        if (error.get() != null) {
            if (error.get() instanceof AssertionError) throw (AssertionError) error.get();
            if (error.get() instanceof Exception) throw (Exception) error.get();
            throw new RuntimeException(error.get());
        }
    }

    @Test
    public void testCanvasPopulationAndSelection() throws Exception {
        runOnFx(() -> {
            ConnectionProfile profile = new ConnectionProfile();
            profile.setName("test-conn");
            profile.setDatabase("testdb");

            DiagramCanvas canvas = new DiagramCanvas(profile, "testdb");

            DiagramGraph graph = new DiagramGraph();
            DbObject users = new DbObject("users", DbObject.Kind.TABLE, "testdb", "public");
            DbObject orders = new DbObject("orders", DbObject.Kind.TABLE, "testdb", "public");

            graph.addNode(users, List.of(
                    new MetadataService.ColumnInfo("id", "int8", 8, false, null, true),
                    new MetadataService.ColumnInfo("name", "varchar", 255, true, null, false)
            ));
            graph.addNode(orders, List.of(
                    new MetadataService.ColumnInfo("id", "int8", 8, false, null, true),
                    new MetadataService.ColumnInfo("user_id", "int8", 8, false, null, false)
            ));
            graph.addEdge("orders", "user_id", "users", "id", "fk_order_user");

            canvas.setGraph(graph, false, 2);

            // Select orders table
            DiagramGraph.Node ordersNode = graph.getNode("orders");
            assertNotNull(ordersNode);
            canvas.selectNode(ordersNode);

            // DDL popup invocation
            assertDoesNotThrow(canvas::showSelectedTableDdl);
            assertDoesNotThrow(canvas::closeDdlPopup);

            // Auto-route edges
            assertDoesNotThrow(canvas::autoRouteEdges);

            // Clear selection
            assertDoesNotThrow(canvas::clearSelection);
        });
    }
}

package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.Project;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TabDragHandlerTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private void runOnFx(Runnable action) throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        boolean ok = latch.await(10, TimeUnit.SECONDS);
        if (!ok) {
            fail("JavaFX operation timed out");
        }
        if (err.get() != null) {
            throw err.get();
        }
    }

    @Test
    public void testTabDragAndReorderMatchingDataGripImages() throws Throwable {
        runOnFx(() -> {
            Stage stage = new Stage();
            Project testProj = new Project("test-drag-proj", "/tmp/test-drag-proj");
            MainWindow mw = new MainWindow(stage, testProj);

            TabPane pane = mw.getActiveTabPane();
            assertNotNull(pane, "Active TabPane must not be null");

            ConnectionProfile profile223 = new ConnectionProfile();
            profile223.setId("pg-223");
            profile223.setName("postgres@223");
            profile223.setType(DatabaseType.POSTGRESQL);

            ConnectionProfile profileLocal = new ConnectionProfile();
            profileLocal.setId("pg-local");
            profileLocal.setName("postgres@localhost");
            profileLocal.setType(DatabaseType.POSTGRESQL);

            // Tab 1: environment_variables [postgres@223]
            Tab t1 = new Tab("environment_variables [postgres@223]", new Label("Variables data"));
            // Tab 2: allergy_type [postgres@localhost]
            Tab t2 = new Tab("allergy_type [postgres@localhost]", new Label("Allergy data"));
            // Tab 3: console_2 [postgres@localhost]
            QueryTab t3 = new QueryTab(mw, profileLocal, null, "console_2");

            pane.getTabs().clear();
            pane.getTabs().addAll(t1, t2, t3);
            pane.getSelectionModel().select(t2);

            // Verify initial state matching DataGrip Image 1:
            // Tabs: [t1: environment_variables, t2: allergy_type, t3: console_2]
            assertEquals(3, pane.getTabs().size());
            assertSame(t1, pane.getTabs().get(0));
            assertSame(t2, pane.getTabs().get(1));
            assertSame(t3, pane.getTabs().get(2));

            // Select console_2
            pane.getSelectionModel().select(t3);
            assertSame(t3, pane.getSelectionModel().getSelectedItem());
            assertEquals("Database Consoles › postgres@localhost › console_2", mw.getNavBarBreadcrumbText());

            // -------------------------------------------------------------
            // Step 1: User drags console_2 left past allergy_type (Image 1 -> Image 2)
            // -------------------------------------------------------------
            TabDragHandler.moveTab(pane, 2, 1, mw);

            // Verify state matching DataGrip Image 2:
            // Tabs: [t1: environment_variables, t3: console_2, t2: allergy_type]
            assertSame(t1, pane.getTabs().get(0));
            assertSame(t3, pane.getTabs().get(1));
            assertSame(t2, pane.getTabs().get(2));
            assertSame(t3, pane.getSelectionModel().getSelectedItem(), "Dragged tab must remain selected");
            assertEquals("Database Consoles › postgres@localhost › console_2", mw.getNavBarBreadcrumbText());

            // -------------------------------------------------------------
            // Step 2: User continues dragging console_2 left to index 0 (Image 2 -> Image 3)
            // -------------------------------------------------------------
            TabDragHandler.moveTab(pane, 1, 0, mw);

            // Verify state matching DataGrip Image 3:
            // Tabs: [t3: console_2, t1: environment_variables, t2: allergy_type]
            assertSame(t3, pane.getTabs().get(0));
            assertSame(t1, pane.getTabs().get(1));
            assertSame(t2, pane.getTabs().get(2));
            assertSame(t3, pane.getSelectionModel().getSelectedItem(), "Dragged tab must remain selected");
            assertEquals("Database Consoles › postgres@localhost › console_2", mw.getNavBarBreadcrumbText());

            // -------------------------------------------------------------
            // Step 3: User drags console_2 from left to right (Image 3 back to Image 1)
            // -------------------------------------------------------------
            TabDragHandler.moveTab(pane, 0, 2, mw);

            assertSame(t1, pane.getTabs().get(0));
            assertSame(t2, pane.getTabs().get(1));
            assertSame(t3, pane.getTabs().get(2));
            assertSame(t3, pane.getSelectionModel().getSelectedItem());

            // -------------------------------------------------------------
            // Edge cases: No-op and bounds safety
            // -------------------------------------------------------------
            TabDragHandler.moveTab(pane, 1, 1, mw); // same index -> no-op
            assertEquals(3, pane.getTabs().size());
            assertSame(t2, pane.getTabs().get(1));

            TabDragHandler.moveTab(pane, -1, 2, mw); // invalid fromIndex -> safe
            TabDragHandler.moveTab(pane, 1, 99, mw); // invalid toIndex -> safe
            assertEquals(3, pane.getTabs().size());
        });
    }
}

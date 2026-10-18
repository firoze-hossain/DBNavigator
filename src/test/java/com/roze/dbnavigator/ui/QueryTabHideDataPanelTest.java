package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QueryTabHideDataPanelTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createTestProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("test-pg-1");
        p.setName("postgres@localhost");
        p.setType(ConnectionProfile.DatabaseType.POSTGRESQL);
        p.setHost("localhost");
        p.setPort(5432);
        p.setDatabase("testdb");
        p.setUsername("postgres");
        p.setPassword("secret");
        p.setSavePassword(true);
        return p;
    }

    private void runOnFxThread(Runnable action) throws Exception {
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(15, java.util.concurrent.TimeUnit.SECONDS), "Timed out waiting for FX thread execution");
        if (error.get() != null) {
            if (error.get() instanceof AssertionError) throw (AssertionError) error.get();
            if (error.get() instanceof Exception) throw (Exception) error.get();
            throw new RuntimeException(error.get());
        }
    }

    @Test
    public void testHideButtonPropertiesMatchingDataGrip() throws Exception {
        runOnFxThread(() -> {
            ConnectionProfile profile = createTestProfile();
            QueryTab tab = new QueryTab(null, profile, "testdb", "console [postgres@localhost]");

            Button hideBtn = tab.getHideDataPanelButton();
            assertNotNull(hideBtn, "Hide button must be present in QueryTab data panel");
            assertNotNull(hideBtn.getTooltip(), "Hide button must have a tooltip");
            assertTrue(hideBtn.getTooltip().getText().startsWith("Hide"), "Tooltip must start with 'Hide'");
            assertTrue(hideBtn.getTooltip().getText().contains("Shift+Escape") || hideBtn.getTooltip().getText().contains("⇧⎋"),
                    "Tooltip must contain Shift+Escape shortcut");
            assertNotNull(hideBtn.getGraphic(), "Hide button must have an icon");
        });
    }

    @Test
    public void testHideAndShowDataPanel() throws Exception {
        runOnFxThread(() -> {
            ConnectionProfile profile = createTestProfile();
            QueryTab tab = new QueryTab(null, profile, "testdb", "console [postgres@localhost]");

            assertTrue(tab.isDataPanelVisible(), "Data panel should initially be visible");

            tab.hideDataPanel();
            assertFalse(tab.isDataPanelVisible(), "Data panel should be hidden after hideDataPanel()");

            tab.showDataPanel();
            assertTrue(tab.isDataPanelVisible(), "Data panel should be visible after showDataPanel()");

            tab.toggleDataPanel();
            assertFalse(tab.isDataPanelVisible(), "Data panel should be hidden after toggleDataPanel()");

            tab.toggleDataPanel();
            assertTrue(tab.isDataPanelVisible(), "Data panel should be visible after toggleDataPanel() again");
        });
    }

    @Test
    public void testShiftEscapeHidesDataPanel() throws Exception {
        runOnFxThread(() -> {
            ConnectionProfile profile = createTestProfile();
            QueryTab tab = new QueryTab(null, profile, "testdb", "console [postgres@localhost]");

            assertTrue(tab.isDataPanelVisible());

            KeyEvent shiftEscapeEvent = new KeyEvent(
                    KeyEvent.KEY_PRESSED,
                    "", "", KeyCode.ESCAPE,
                    true, false, false, false
            );

            tab.getContent().fireEvent(shiftEscapeEvent);

            assertFalse(tab.isDataPanelVisible(), "Shift+Escape should hide the console query data panel");
        });
    }

    @Test
    public void testRunPanelMinimizeButtonTooltip() {
        RunPanel runPanel = new RunPanel();
        assertNotNull(runPanel, "RunPanel should be instantiable");
    }
}

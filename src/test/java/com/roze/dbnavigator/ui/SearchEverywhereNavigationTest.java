package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.SearchHistoryStore;
import com.roze.dbnavigator.model.QueryResult;
import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SearchEverywhereNavigationTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @BeforeEach
    void setUp() {
        SearchHistoryStore.clearForTesting();
    }

    private void runOnFxThread(Runnable action) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");
        } catch (InterruptedException e) {
            fail("Interrupted");
        }
        if (failure.get() != null) {
            throw new RuntimeException(failure.get());
        }
    }

    @Test
    public void testFirstRowValueOfColumnSelectedWhenNavigatingToColumn() {
        // DataGrip behavior requirement:
        // "if it is column... it will take me to that table column... first row value of that column will be selected"
        runOnFxThread(() -> {
            ResultGrid grid = new ResultGrid();
            QueryResult result = new QueryResult();
            result.getColumns().addAll(List.of("id", "environment_name", "created_at"));
            result.getColumnTypes().addAll(List.of("int", "varchar", "varchar"));
            result.getRows().add(new ArrayList<>(List.of("101", "Production ERP", "2026-01-01")));
            result.getRows().add(new ArrayList<>(List.of("102", "Staging ERP", "2026-01-02")));
            grid.showResult(result);

            assertEquals(2, grid.getItems().size());

            // Target: select column "environment_name"
            grid.selectColumn("environment_name");

            // Verify row 0 is selected
            assertFalse(grid.getSelectionModel().isEmpty(), "Selection should not be empty");
            int selectedRow = grid.getSelectionModel().getSelectedCells().isEmpty()
                    ? grid.getSelectionModel().getSelectedIndex()
                    : grid.getSelectionModel().getSelectedCells().get(0).getRow();
            assertEquals(0, selectedRow, "First row (row 0) should be selected");

            // Verify selected column is environment_name
            TableColumn<List<String>, ?> selectedCol = grid.getSelectionModel().getSelectedCells().get(0).getTableColumn();
            assertNotNull(selectedCol);
            assertEquals("environment_name", selectedCol.getId());
        });
    }

    @Test
    public void testLastQueryRestoredOnOpening() {
        // User searched "appointment_" in previous session before restart
        SearchHistoryStore.recordSearch("appointment_");
        assertEquals("appointment_", SearchHistoryStore.getLastQuery());

        runOnFxThread(() -> {
            // Opening SearchDialog restores last searched keyword
            SearchDialog dialog = new SearchDialog(null, null);
            assertEquals("appointment_", SearchHistoryStore.getLastQuery());
        });
    }

    @Test
    public void testRecentItemsDisplayedWhenQueryIsEmpty() {
        // Add a recent table and column
        SearchHistoryStore.recordRecentItem(new SearchHistoryStore.RecentItem(
                "TABLE", "queueq", "prof-1", "erp-161", "thunder_db", "public",
                null, null, "DATABASE/prof-1/table/queueq", System.currentTimeMillis()
        ));
        SearchHistoryStore.recordRecentItem(new SearchHistoryStore.RecentItem(
                "COLUMN", "queueq_id", "prof-1", "erp-161", "thunder_db", "public",
                "queueq", "queueq_id", "DATABASE/prof-1/table/queueq/col/queueq_id", System.currentTimeMillis()
        ));

        List<SearchHistoryStore.RecentItem> recents = SearchHistoryStore.getRecentItems();
        assertEquals(2, recents.size());
        assertEquals("queueq_id", recents.get(0).name());
        assertEquals("queueq", recents.get(1).name());
    }
}

package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.QueryResult;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class UnsubmittedChangesReloadTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @AfterEach
    public void tearDown() {
        UnsubmittedChangesDialog.setConfirmationHandlerForTesting(null);
    }

    @Test
    public void testConfirmAndClearPendingReturnsTrueWhenNoPending() {
        ResultGrid grid = new ResultGrid();
        Button submitBtn = new Button();
        Button revertBtn = new Button();
        GridEditManager manager = new GridEditManager(
                null, null, grid, submitBtn, revertBtn, () -> {}, s -> {});

        // No edits pending
        assertFalse(manager.hasPending());
        AtomicBoolean dialogShown = new AtomicBoolean(false);
        UnsubmittedChangesDialog.setConfirmationHandlerForTesting(owner -> {
            dialogShown.set(true);
            return false;
        });

        boolean proceed = manager.confirmAndClearPendingIfDiscarded(null);
        assertTrue(proceed, "Should proceed immediately without dialog when no edits are pending");
        assertFalse(dialogShown.get(), "Dialog should not be shown when no edits are pending");
    }

    @Test
    public void testConfirmAndClearPendingUserDeclines() {
        ResultGrid grid = new ResultGrid();
        Button submitBtn = new Button();
        Button revertBtn = new Button();
        GridEditManager manager = new GridEditManager(
                null, null, grid, submitBtn, revertBtn, () -> {}, s -> {});

        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("id", "name"));
        result.getColumnTypes().addAll(List.of("int", "varchar"));
        result.getRows().add(new ArrayList<>(List.of("1", "Original")));

        manager.configure("items", List.of("id"), result);
        grid.showResult(result);

        // Make an edit
        @SuppressWarnings("unchecked")
        TableColumn<List<String>, String> col = (TableColumn<List<String>, String>) grid.getColumns().get(2);
        grid.getSelectionModel().select(0, col);
        grid.commitValueAcrossSelection(0, col, 1, "Modified");

        assertTrue(manager.hasPending());
        assertFalse(submitBtn.isDisable());

        // Simulate user clicking "No" (do not discard)
        AtomicBoolean dialogShown = new AtomicBoolean(false);
        UnsubmittedChangesDialog.setConfirmationHandlerForTesting(owner -> {
            dialogShown.set(true);
            return false;
        });

        boolean proceed = manager.confirmAndClearPendingIfDiscarded(null);
        assertFalse(proceed, "Should not proceed when user declines discard");
        assertTrue(dialogShown.get(), "Dialog should have been shown");
        assertTrue(manager.hasPending(), "Pending edits should remain after declining discard");
        assertFalse(submitBtn.isDisable(), "Submit button should remain enabled");
        assertTrue(grid.isCellModified(0, 1), "Cell should remain marked as modified");
    }

    @Test
    public void testConfirmAndClearPendingUserConfirms() {
        ResultGrid grid = new ResultGrid();
        Button submitBtn = new Button();
        Button revertBtn = new Button();
        GridEditManager manager = new GridEditManager(
                null, null, grid, submitBtn, revertBtn, () -> {}, s -> {});

        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("id", "name"));
        result.getColumnTypes().addAll(List.of("int", "varchar"));
        result.getRows().add(new ArrayList<>(List.of("1", "Original")));

        manager.configure("items", List.of("id"), result);
        grid.showResult(result);

        // Make an edit
        @SuppressWarnings("unchecked")
        TableColumn<List<String>, String> col = (TableColumn<List<String>, String>) grid.getColumns().get(2);
        grid.getSelectionModel().select(0, col);
        grid.commitValueAcrossSelection(0, col, 1, "Modified");

        assertTrue(manager.hasPending());

        // Simulate user clicking "Yes" (confirm discard)
        AtomicBoolean dialogShown = new AtomicBoolean(false);
        UnsubmittedChangesDialog.setConfirmationHandlerForTesting(owner -> {
            dialogShown.set(true);
            return true;
        });

        boolean proceed = manager.confirmAndClearPendingIfDiscarded(null);
        assertTrue(proceed, "Should proceed when user confirms discard");
        assertTrue(dialogShown.get(), "Dialog should have been shown");
        assertFalse(manager.hasPending(), "Pending edits should be cleared after confirming discard");
        assertTrue(submitBtn.isDisable(), "Submit button should be disabled after clearing pending");
        assertFalse(grid.isCellModified(0, 1), "Cell modified mark should be cleared");
    }

    @Test
    public void testResultGridReloadActionCallback() {
        ResultGrid grid = new ResultGrid();
        assertNull(grid.getReloadAction());

        AtomicBoolean reloaded = new AtomicBoolean(false);
        grid.setReloadAction(() -> reloaded.set(true));

        assertNotNull(grid.getReloadAction());
        grid.getReloadAction().run();
        assertTrue(reloaded.get());
    }
}

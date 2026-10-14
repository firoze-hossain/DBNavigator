package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.QueryResult;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GridRowDeletionAndSelectionTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ResultGrid createSampleGrid(int rowCount) {
        ResultGrid grid = new ResultGrid();
        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("id", "name", "status"));
        result.getColumnTypes().addAll(List.of("int", "varchar", "varchar"));
        for (int i = 0; i < rowCount; i++) {
            result.getRows().add(new ArrayList<>(List.of(
                    String.valueOf(i + 1),
                    "User_" + (i + 1),
                    "Active"
            )));
        }
        grid.showResult(result);
        return grid;
    }

    @Test
    public void testSerialClickSingleSelect() {
        ResultGrid grid = createSampleGrid(5);

        // Click row 1 serial header
        grid.handleSerialClick(1, false, false);

        assertTrue(grid.isRowSelected(1));
        assertFalse(grid.isRowSelected(0));
        assertFalse(grid.isRowSelected(2));

        // Column 0 is serialCol, 1 is id, 2 is name, 3 is status -> 4 total columns
        assertEquals(4, grid.getColumns().size());
        assertEquals(4, grid.getSelectionModel().getSelectedCells().size());
        assertEquals(List.of(1), grid.getSelectedRowIndexes());
    }

    @Test
    public void testSerialCtrlClickToggleSelect() {
        ResultGrid grid = createSampleGrid(5);

        // Select row 1
        grid.handleSerialClick(1, false, false);
        assertTrue(grid.isRowSelected(1));

        // Ctrl+Click row 3
        grid.handleSerialClick(3, false, true);
        assertTrue(grid.isRowSelected(1));
        assertTrue(grid.isRowSelected(3));
        assertFalse(grid.isRowSelected(2));

        List<Integer> selected = grid.getSelectedRowIndexes();
        assertEquals(2, selected.size());
        assertTrue(selected.contains(1));
        assertTrue(selected.contains(3));

        // Ctrl+Click row 1 again -> toggles off
        grid.handleSerialClick(1, false, true);
        assertFalse(grid.isRowSelected(1));
        assertTrue(grid.isRowSelected(3));
    }

    @Test
    public void testSerialShiftClickRangeSelect() {
        ResultGrid grid = createSampleGrid(6);

        // Click row 1
        grid.handleSerialClick(1, false, false);
        assertEquals(List.of(1), grid.getSelectedRowIndexes());

        // Shift+Click row 3 -> selects rows 1, 2, 3
        grid.handleSerialClick(3, true, false);
        List<Integer> selected = grid.getSelectedRowIndexes();
        assertEquals(List.of(3, 2, 1), selected);

        // Expand range with Shift+Click row 5 -> anchor (row 1) preserved, selects 1..5
        grid.handleSerialClick(5, true, false);
        List<Integer> expanded = grid.getSelectedRowIndexes();
        assertEquals(List.of(5, 4, 3, 2, 1), expanded);
        assertFalse(grid.isRowSelected(0));

        // Shrink range with Shift+Click row 2 -> anchor (row 1) preserved, selects 1..2
        grid.handleSerialClick(2, true, false);
        List<Integer> shrunk = grid.getSelectedRowIndexes();
        assertEquals(List.of(2, 1), shrunk);
        assertFalse(grid.isRowSelected(3));
    }

    @Test
    public void testShiftClickUpwards() {
        ResultGrid grid = createSampleGrid(6);

        // Click row 4 first
        grid.handleSerialClick(4, false, false);
        assertEquals(List.of(4), grid.getSelectedRowIndexes());

        // Shift+Click row 1 upwards -> selects rows 1, 2, 3, 4
        grid.handleSerialClick(1, true, false);
        assertEquals(List.of(4, 3, 2, 1), grid.getSelectedRowIndexes());

        // Shift+Click row 0 upwards -> expands to rows 0..4
        grid.handleSerialClick(0, true, false);
        assertEquals(List.of(4, 3, 2, 1, 0), grid.getSelectedRowIndexes());
    }

    @Test
    public void testLargeRangeSelect() {
        ResultGrid grid = createSampleGrid(100);

        // Click row 10
        grid.handleSerialClick(10, false, false);
        assertEquals(List.of(10), grid.getSelectedRowIndexes());

        // Shift+Click row 50 -> selects 10..50
        grid.handleSerialClick(50, true, false);
        List<Integer> selected = grid.getSelectedRowIndexes();
        assertEquals(41, selected.size());
        assertEquals(50, (int) selected.get(0));
        assertEquals(10, (int) selected.get(selected.size() - 1));
    }

    @Test
    public void testSelectOneByOneWithCtrlClick() {
        ResultGrid grid = createSampleGrid(6);

        // Click row 0
        grid.handleSerialClick(0, false, false);
        assertTrue(grid.isRowFullySelected(0));
        assertEquals(List.of(0), grid.getSelectedRowIndexes());

        // Ctrl+Click row 2
        grid.handleSerialClick(2, false, true);
        assertTrue(grid.isRowFullySelected(0));
        assertTrue(grid.isRowFullySelected(2));
        assertEquals(List.of(2, 0), grid.getSelectedRowIndexes());

        // Ctrl+Click row 4
        grid.handleSerialClick(4, false, true);
        assertTrue(grid.isRowFullySelected(0));
        assertTrue(grid.isRowFullySelected(2));
        assertTrue(grid.isRowFullySelected(4));
        assertEquals(List.of(4, 2, 0), grid.getSelectedRowIndexes());

        // Ctrl+Click row 2 to toggle off
        grid.handleSerialClick(2, false, true);
        assertTrue(grid.isRowFullySelected(0));
        assertFalse(grid.isRowFullySelected(2));
        assertTrue(grid.isRowFullySelected(4));
        assertEquals(List.of(4, 0), grid.getSelectedRowIndexes());
    }

    @Test
    public void testRowDeletedTrackingAndPseudoClassState() {
        ResultGrid grid = createSampleGrid(3);

        assertFalse(grid.isRowDeleted(1));
        grid.markRowDeleted(1);
        assertTrue(grid.isRowDeleted(1));
        assertFalse(grid.isRowDeleted(0));
        assertFalse(grid.isRowDeleted(2));

        // Modified cell in a deleted row should not be reported as modified
        grid.markCellModified(1, 0);
        assertFalse(grid.isCellModified(1, 0));

        grid.unmarkRowDeleted(1);
        assertFalse(grid.isRowDeleted(1));
        // Once unmarked, modified status should be reflected
        assertTrue(grid.isCellModified(1, 0));

        grid.clearDeletedRows();
        assertTrue(grid.getDeletedRowIndices().isEmpty());
    }

    @Test
    public void testGridEditManagerMarkSelectedRowsDeletedRetainsRowsInGrid() {
        ResultGrid grid = new ResultGrid();
        Button submitBtn = new Button();
        Button revertBtn = new Button();
        List<String> statuses = new ArrayList<>();

        GridEditManager manager = new GridEditManager(
                null, null, grid, submitBtn, revertBtn, () -> {}, statuses::add);

        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("id", "name"));
        result.getColumnTypes().addAll(List.of("int", "varchar"));
        result.getRows().add(new ArrayList<>(List.of("1", "Alice")));
        result.getRows().add(new ArrayList<>(List.of("2", "Bob")));
        result.getRows().add(new ArrayList<>(List.of("3", "Charlie")));

        manager.configure("users", List.of("id"), result);
        grid.showResult(result);

        assertEquals(3, grid.getItems().size());
        assertFalse(manager.hasPending());
        assertTrue(submitBtn.isDisable());

        // Select row 1 by serial header
        grid.handleSerialClick(1, false, false);

        // Mark row deleted
        manager.markSelectedRowsDeleted();

        // Row MUST NOT be removed from table items before submit
        assertEquals(3, grid.getItems().size());
        assertTrue(grid.isRowDeleted(1));
        assertFalse(grid.isRowDeleted(0));
        assertFalse(grid.isRowDeleted(2));

        // Buttons must reflect pending delete
        assertTrue(manager.hasPending());
        assertFalse(submitBtn.isDisable());
        assertEquals("Submit (1)", submitBtn.getText());
        assertFalse(revertBtn.isDisable());

        // Unmarking row restores state
        manager.unmarkRowDeleted(1);
        assertFalse(manager.hasPending());
        assertTrue(submitBtn.isDisable());
        assertEquals("Submit", submitBtn.getText());
    }

    @Test
    public void testDeleteMultipleRowsWithShiftSelectInEditManager() {
        ResultGrid grid = new ResultGrid();
        Button submitBtn = new Button();
        Button revertBtn = new Button();

        GridEditManager manager = new GridEditManager(
                null, null, grid, submitBtn, revertBtn, () -> {}, s -> {});

        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("id", "name"));
        result.getColumnTypes().addAll(List.of("int", "varchar"));
        result.getRows().add(new ArrayList<>(List.of("10", "Ten")));
        result.getRows().add(new ArrayList<>(List.of("20", "Twenty")));
        result.getRows().add(new ArrayList<>(List.of("30", "Thirty")));
        result.getRows().add(new ArrayList<>(List.of("40", "Forty")));

        manager.configure("numbers", List.of("id"), result);
        grid.showResult(result);

        // Select rows 1..2 via shift click
        grid.handleSerialClick(1, false, false);
        grid.handleSerialClick(2, true, false);

        manager.markSelectedRowsDeleted();

        assertEquals(4, grid.getItems().size());
        assertTrue(grid.isRowDeleted(1));
        assertTrue(grid.isRowDeleted(2));
        assertFalse(grid.isRowDeleted(0));
        assertFalse(grid.isRowDeleted(3));

        assertTrue(manager.hasPending());
        assertEquals("Submit (2)", submitBtn.getText());

        // Revert clears deleted rows
        manager.clearPending();
        assertFalse(manager.hasPending());
        assertEquals("Submit", submitBtn.getText());
        assertFalse(grid.isRowDeleted(1));
        assertFalse(grid.isRowDeleted(2));
    }

    @Test
    public void testDeleteRowSupercedesPendingUpdate() {
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

        // Edit row 0 name column
        @SuppressWarnings("unchecked")
        TableColumn<List<String>, String> nameCol = (TableColumn<List<String>, String>) grid.getColumns().get(2);
        grid.getSelectionModel().select(0, nameCol);
        grid.commitValueAcrossSelection(0, nameCol, 1, "Modified");

        assertTrue(manager.hasPending());
        assertEquals("Submit (1)", submitBtn.getText());

        // Now mark row 0 as deleted -> supersede update
        grid.handleSerialClick(0, false, false);
        manager.markSelectedRowsDeleted();

        assertTrue(manager.hasPending());
        // Should still be 1 total change (delete, not update + delete)
        assertEquals("Submit (1)", submitBtn.getText());
        assertTrue(grid.isRowDeleted(0));
    }

    @Test
    public void testSpecificColumnShiftClickSelectsOnlyThatColumnValuesNotWholeRows() {
        ResultGrid grid = createSampleGrid(6);
        // columns: [0] = #, [1] = id, [2] = name, [3] = status
        assertEquals(4, grid.getColumns().size());

        @SuppressWarnings("unchecked")
        TableColumn<List<String>, ?> nameCol = (TableColumn<List<String>, ?>) grid.getColumns().get(2);

        // Select cell at row 1 in column "name"
        grid.selectCellRange(1, nameCol, 3, nameCol, false);

        var selectedCells = grid.getSelectionModel().getSelectedCells();
        assertEquals(3, selectedCells.size());
        for (var pos : selectedCells) {
            assertEquals(nameCol, pos.getTableColumn());
            assertTrue(pos.getRow() >= 1 && pos.getRow() <= 3);
        }

        // Whole rows MUST NOT be selected
        assertFalse(grid.isRowFullySelected(1));
        assertFalse(grid.isRowFullySelected(2));
        assertFalse(grid.isRowFullySelected(3));

        // Column 0 (#), 1 (id), 3 (status) must have no selected cells
        assertFalse(grid.getSelectionModel().isSelected(1, grid.getColumns().get(0)));
        assertFalse(grid.getSelectionModel().isSelected(1, grid.getColumns().get(1)));
        assertFalse(grid.getSelectionModel().isSelected(1, grid.getColumns().get(3)));
    }

    @Test
    public void testSpecificColumnShiftClickAcrossColumns() {
        ResultGrid grid = createSampleGrid(6);
        // columns: [0] = #, [1] = id, [2] = name, [3] = status
        @SuppressWarnings("unchecked")
        TableColumn<List<String>, ?> idCol = (TableColumn<List<String>, ?>) grid.getColumns().get(1);
        @SuppressWarnings("unchecked")
        TableColumn<List<String>, ?> nameCol = (TableColumn<List<String>, ?>) grid.getColumns().get(2);

        // Select range from (row 1, idCol) to (row 3, nameCol)
        grid.selectCellRange(1, idCol, 3, nameCol, false);

        var selectedCells = grid.getSelectionModel().getSelectedCells();
        // 3 rows (1..3) * 2 columns (id, name) = 6 cells
        assertEquals(6, selectedCells.size());

        // Serial column (#) must NOT be selected
        for (int r = 1; r <= 3; r++) {
            assertFalse(grid.getSelectionModel().isSelected(r, grid.getColumns().get(0)));
            assertFalse(grid.getSelectionModel().isSelected(r, grid.getColumns().get(3)));
            assertTrue(grid.getSelectionModel().isSelected(r, idCol));
            assertTrue(grid.getSelectionModel().isSelected(r, nameCol));
            assertFalse(grid.isRowFullySelected(r));
        }
    }
}

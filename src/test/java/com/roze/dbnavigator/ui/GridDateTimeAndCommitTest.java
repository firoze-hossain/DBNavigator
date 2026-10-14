package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.QueryResult;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class GridDateTimeAndCommitTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Test
    public void testGridDateTimePickerParsesTimestamp() {
        AtomicReference<String> changedValue = new AtomicReference<>();
        GridDateTimePicker picker = new GridDateTimePicker(changedValue::set);

        String timestamp = "2025-02-06 15:58:00.414000";
        picker.parseAndUpdate(timestamp);

        assertEquals(2025, picker.getYearSpinner().getValue());
        assertEquals("February", picker.getMonthCombo().getValue());
        assertEquals(6, picker.getDayCombo().getValue());
        assertEquals(15, picker.getHourSpinner().getValue());
        assertEquals(58, picker.getMinuteSpinner().getValue());
        assertEquals(0, picker.getSecondSpinner().getValue());

        assertEquals(timestamp, picker.buildFormattedString());
    }

    @Test
    public void testGridDateTimePickerComponentChangeUpdatesValue() {
        AtomicReference<String> changedValue = new AtomicReference<>();
        GridDateTimePicker picker = new GridDateTimePicker(changedValue::set);

        picker.parseAndUpdate("2025-02-06 15:58:00.414000");

        // Change Hour to 18
        picker.getHourSpinner().getValueFactory().setValue(18);
        assertEquals("2025-02-06 18:58:00.414000", changedValue.get());

        // Change Day to 20
        picker.getDayCombo().setValue(20);
        assertEquals("2025-02-20 18:58:00.414000", changedValue.get());
    }

    @Test
    public void testGridDateTimePickerAdjustsDaysForMonthAndLeapYear() {
        GridDateTimePicker picker = new GridDateTimePicker(s -> {});

        // Feb 2024 is a leap year (29 days)
        picker.parseAndUpdate("2024-02-15 12:00:00");
        assertEquals(29, picker.getDayCombo().getItems().size());

        // Change year to 2025 (non-leap year, 28 days)
        picker.getYearSpinner().getValueFactory().setValue(2025);
        assertEquals(28, picker.getDayCombo().getItems().size());

        // Change month to March (31 days)
        picker.getMonthCombo().setValue("March");
        assertEquals(31, picker.getDayCombo().getItems().size());
    }

    @Test
    public void testGridDateTimePickerPureDateAndPureTime() {
        GridDateTimePicker picker = new GridDateTimePicker(s -> {});

        // Pure date
        picker.parseAndUpdate("2026-10-14");
        assertEquals("2026-10-14", picker.buildFormattedString());

        // Pure time
        picker.parseAndUpdate("14:30:45");
        assertEquals("14:30:45", picker.buildFormattedString());
    }

    @Test
    public void testGridBooleanPickerSelectsValue() {
        AtomicReference<String> selectedVal = new AtomicReference<>();
        GridBooleanPicker picker = new GridBooleanPicker(selectedVal::set);

        assertNotNull(picker.getPopup());
        assertFalse(picker.isShowing());
    }

    @Test
    public void testResultGridCellSelectionAndMultiCellCommit() {
        ResultGrid grid = new ResultGrid();
        assertTrue(grid.getSelectionModel().isCellSelectionEnabled());

        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("id", "name", "deleted"));
        result.getColumnTypes().addAll(List.of("int", "varchar", "boolean"));
        result.getRows().add(new ArrayList<>(List.of("1", "Product A", "false")));
        result.getRows().add(new ArrayList<>(List.of("2", "Product B", "false")));

        List<String> edits = new ArrayList<>();
        grid.enableEditing((row, col, oldV, newV) -> edits.add(row + ":" + col + ":" + newV));
        grid.setEditable(true);
        grid.showResult(result);

        // Select row 0 and row 1 in column 2 ("deleted", which is index 3 in TableView due to '#' serial col)
        @SuppressWarnings("unchecked")
        TableColumn<List<String>, String> deletedCol = (TableColumn<List<String>, String>) grid.getColumns().get(3);
        grid.getSelectionModel().select(0, deletedCol);
        grid.getSelectionModel().select(1, deletedCol);

        assertEquals(2, grid.getSelectionModel().getSelectedCells().size());

        // Batch commit "true" across selection
        grid.commitValueAcrossSelection(0, deletedCol, 2, "true");

        assertEquals("true", grid.getItems().get(0).get(2));
        assertEquals("true", grid.getItems().get(1).get(2));

        assertTrue(grid.isCellModified(0, 2));
        assertTrue(grid.isCellModified(1, 2));

        assertEquals(2, edits.size());
        assertEquals("0:2:true", edits.get(0));
        assertEquals("1:2:true", edits.get(1));
    }

    @Test
    public void testResultGridModifiedCellTracking() {
        ResultGrid grid = new ResultGrid();
        assertFalse(grid.isCellModified(0, 1));

        grid.markCellModified(0, 1);
        assertTrue(grid.isCellModified(0, 1));
        assertFalse(grid.isCellModified(1, 1));

        grid.clearModifiedCells();
        assertFalse(grid.isCellModified(0, 1));
    }

    @Test
    public void testGridEditManagerBatchUpdatesAndSubmitButton() {
        ResultGrid grid = new ResultGrid();
        Button submitBtn = new Button();
        Button revertBtn = new Button();
        List<String> statuses = new ArrayList<>();

        GridEditManager manager = new GridEditManager(
                null, null, grid, submitBtn, revertBtn, () -> {}, statuses::add);

        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("id", "tags", "deleted"));
        result.getColumnTypes().addAll(List.of("int", "varchar", "boolean"));
        result.getRows().add(new ArrayList<>(List.of("1", "initial tag 1", "false")));
        result.getRows().add(new ArrayList<>(List.of("2", "initial tag 2", "false")));

        manager.configure("products", List.of("id"), result);
        grid.showResult(result);

        assertFalse(manager.hasPending());
        assertTrue(submitBtn.isDisable());
        assertEquals("Submit", submitBtn.getText());

        // Multi-cell batch edit on column 1 ("tags")
        @SuppressWarnings("unchecked")
        TableColumn<List<String>, String> tagCol = (TableColumn<List<String>, String>) grid.getColumns().get(2);
        grid.getSelectionModel().select(0, tagCol);
        grid.getSelectionModel().select(1, tagCol);

        grid.commitValueAcrossSelection(0, tagCol, 1, "ccc");

        assertTrue(manager.hasPending());
        assertFalse(submitBtn.isDisable());
        assertEquals("Submit (2)", submitBtn.getText());
        assertEquals("ccc", grid.getItems().get(0).get(1));
        assertEquals("ccc", grid.getItems().get(1).get(1));
        assertTrue(grid.isCellModified(0, 1));
        assertTrue(grid.isCellModified(1, 1));
    }
}

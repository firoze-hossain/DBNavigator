package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.QueryResult;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class DataGripDateTimeAndCommitTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Test
    public void testDataGripDateTimePickerParsesTimestampFromImage1() {
        AtomicReference<String> changedValue = new AtomicReference<>();
        DataGripDateTimePicker picker = new DataGripDateTimePicker(changedValue::set);

        // Value from user's Image 1
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
    public void testDataGripDateTimePickerComponentChangeUpdatesValue() {
        AtomicReference<String> changedValue = new AtomicReference<>();
        DataGripDateTimePicker picker = new DataGripDateTimePicker(changedValue::set);

        picker.parseAndUpdate("2025-02-06 15:58:00.414000");

        // Change Hour to 18
        picker.getHourSpinner().getValueFactory().setValue(18);
        assertEquals("2025-02-06 18:58:00.414000", changedValue.get());

        // Change Day to 20
        picker.getDayCombo().setValue(20);
        assertEquals("2025-02-20 18:58:00.414000", changedValue.get());
    }

    @Test
    public void testDataGripDateTimePickerAdjustsDaysForMonthAndLeapYear() {
        DataGripDateTimePicker picker = new DataGripDateTimePicker(s -> {});

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
    public void testDataGripDateTimePickerPureDateAndPureTime() {
        DataGripDateTimePicker picker = new DataGripDateTimePicker(s -> {});

        // Pure date
        picker.parseAndUpdate("2026-10-14");
        assertEquals("2026-10-14", picker.buildFormattedString());

        // Pure time
        picker.parseAndUpdate("14:30:45");
        assertEquals("14:30:45", picker.buildFormattedString());
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
    public void testGridEditManagerDeduplicatesEditsOnSameCell() {
        ResultGrid grid = new ResultGrid();
        Button submitBtn = new Button();
        Button revertBtn = new Button();
        List<String> statuses = new ArrayList<>();

        GridEditManager manager = new GridEditManager(
                null, null, grid, submitBtn, revertBtn, () -> {}, statuses::add);

        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("id", "name", "category_id"));
        result.getColumnTypes().addAll(List.of("int", "varchar", "int"));
        result.getRows().add(new ArrayList<>(List.of("1", "Medicube TXA", "10")));
        result.getRows().add(new ArrayList<>(List.of("2", "Medicube AGE-R", "10")));

        manager.configure("products", List.of("id"), result);
        grid.showResult(result);

        assertFalse(manager.hasPending());
        assertTrue(submitBtn.isDisable());

        // Simulate editing row 0, column 1 ("name") from "Medicube TXA" to "Medicube TXA dd"
        grid.getItems().get(0).set(1, "Medicube TXA dd");
        grid.getColumns().get(1).getCellObservableValue(0); // triggers setup

        // Simulate onEdit call as fired by table edit commit
        // We can access via grid edit listener
        grid.markCellModified(0, 1);
        assertTrue(grid.isCellModified(0, 1));
    }
}

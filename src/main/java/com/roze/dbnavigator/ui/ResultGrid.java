package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.QueryResult;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Spreadsheet-like grid used everywhere results are shown.
 * Supports multi-selection (Shift/Ctrl+click), optional inline editing with a
 * date picker for date/timestamp columns, and a pluggable row-delete action.
 */
public class ResultGrid extends TableView<List<String>> {

    /** Fired when the user commits an inline cell edit. */
    @FunctionalInterface
    public interface CellEditListener {
        void onEdit(int rowIndex, int columnIndex, String oldValue, String newValue);
    }

    private CellEditListener editListener;
    private Runnable deleteRowsAction;
    private List<String> columnTypes = List.of();

    public ResultGrid() {
        getStyleClass().add("result-grid");
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setPlaceholder(new Label("No data"));

        getSelectionModel().setCellSelectionEnabled(true);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        MenuItem copyCell = new MenuItem("Copy Cell");
        copyCell.setOnAction(e -> copySelectedCell());
        MenuItem copyRow = new MenuItem("Copy Row (TSV)");
        copyRow.setOnAction(e -> copySelectedRow());
        MenuItem deleteRows = new MenuItem("Delete Selected Row(s)");
        deleteRows.setOnAction(e -> { if (deleteRowsAction != null) deleteRowsAction.run(); });
        setContextMenu(new ContextMenu(copyCell, copyRow, new SeparatorMenuItem(), deleteRows));

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE && deleteRowsAction != null && !isEditingCell()) {
                deleteRowsAction.run();
                e.consume();
            }
        });
    }

    private boolean isEditingCell() {
        return getEditingCell() != null;
    }

    /** Registers the edit listener; actual editability is toggled with setEditable(). */
    public void enableEditing(CellEditListener listener) {
        this.editListener = listener;
    }

    /** Action invoked by the context menu item / Delete key. */
    public void setDeleteRowsAction(Runnable action) {
        this.deleteRowsAction = action;
    }

    /** Distinct row indexes touched by the current (cell) selection, descending. */
    public List<Integer> getSelectedRowIndexes() {
        Set<Integer> rows = new LinkedHashSet<>();
        for (TablePosition<?, ?> pos : getSelectionModel().getSelectedCells()) {
            rows.add(pos.getRow());
        }
        List<Integer> sorted = new ArrayList<>(rows);
        sorted.sort((a, b) -> b - a);
        return sorted;
    }

    public void showResult(QueryResult result) {
        getColumns().clear();
        getItems().clear();
        if (result == null || !result.isResultSet()) return;

        this.columnTypes = result.getColumnTypes();
        List<String> columnNames = result.getColumns();
        for (int i = 0; i < columnNames.size(); i++) {
            final int index = i;
            TableColumn<List<String>, String> col = new TableColumn<>(columnNames.get(i));
            col.setCellValueFactory(data -> {
                List<String> row = data.getValue();
                String value = index < row.size() ? row.get(index) : null;
                return new ReadOnlyStringWrapper(value == null ? "NULL" : value);
            });
            col.setPrefWidth(Math.max(90, Math.min(280, columnNames.get(i).length() * 12 + 40)));

            if (editListener != null) {
                col.setCellFactory(c -> new EditCell(index, isDateColumn(index)));
                col.setOnEditCommit(event -> {
                    List<String> row = event.getRowValue();
                    String oldValue = index < row.size() ? row.get(index) : null;
                    String newValue = event.getNewValue();
                    if (newValue != null && !newValue.equals(oldValue == null ? "NULL" : oldValue)) {
                        row.set(index, newValue);
                        editListener.onEdit(event.getTablePosition().getRow(), index,
                                oldValue, newValue);
                        refresh();
                    }
                });
            }
            getColumns().add(col);
        }

        ObservableList<List<String>> items = FXCollections.observableArrayList(result.getRows());
        setItems(items);
    }

    private boolean isDateColumn(int index) {
        if (index >= columnTypes.size()) return false;
        String type = columnTypes.get(index).toLowerCase(Locale.ROOT);
        return type.contains("date") || type.contains("timestamp");
    }

    // ------------------------------------------------------------ edit cell

    /**
     * Text editor cell; date/timestamp columns additionally get a calendar
     * button that opens a DatePicker — picking a date replaces the date part
     * and keeps any time portion. Values can also be typed or pasted directly.
     */
    private class EditCell extends TableCell<List<String>, String> {

        private final int columnIndex;
        private final boolean dateColumn;
        private TextField field;
        private HBox editor;

        EditCell(int columnIndex, boolean dateColumn) {
            this.columnIndex = columnIndex;
            this.dateColumn = dateColumn;
        }

        @Override
        public void startEdit() {
            if (isEmpty() || !getTableView().isEditable()) return;
            super.startEdit();
            if (editor == null) buildEditor();
            field.setText(getItem() == null ? "" : getItem());
            setText(null);
            setGraphic(editor);
            field.requestFocus();
            field.selectAll();
        }

        private void buildEditor() {
            field = new TextField();
            field.getStyleClass().add("cell-editor");
            field.setOnAction(e -> commitEdit(field.getText()));
            field.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) cancelEdit();
            });
            HBox.setHgrow(field, javafx.scene.layout.Priority.ALWAYS);
            editor = new HBox(2, field);
            editor.setAlignment(Pos.CENTER_LEFT);

            if (dateColumn) {
                Button calendar = new Button();
                calendar.setGraphic(Icons.of(FontAwesomeSolid.CALENDAR_ALT, "#6897bb", 11));
                calendar.getStyleClass().add("calendar-button");
                calendar.setFocusTraversable(false);
                calendar.setOnAction(e -> openDatePicker());
                editor.getChildren().add(calendar);
            }
        }

        private void openDatePicker() {
            DatePicker picker = new DatePicker();
            try {
                String text = field.getText();
                if (text != null && text.length() >= 10) {
                    picker.setValue(LocalDate.parse(text.substring(0, 10)));
                }
            } catch (Exception ignored) {}

            Popup popup = new Popup();
            popup.setAutoHide(true);
            popup.getContent().add(picker);
            picker.setOnAction(e -> {
                LocalDate date = picker.getValue();
                if (date != null) {
                    String text = field.getText();
                    // keep the time part of a timestamp, replace only the date
                    if (text != null && text.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                        field.setText(date + text.substring(10));
                    } else {
                        field.setText(date.toString());
                    }
                }
                popup.hide();
                field.requestFocus();
                field.end();
            });
            var bounds = localToScreen(getBoundsInLocal());
            popup.show(this, bounds.getMinX(), bounds.getMaxY());
            picker.show();   // open the calendar immediately
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setGraphic(null);
            setText(getItem());
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing() && field != null) {
                field.setText(item == null ? "" : item);
                setText(null);
                setGraphic(editor);
            } else {
                setText(item);
                setGraphic(null);
            }
        }
    }

    // ---------------------------------------------------------- clipboard

    private void copySelectedCell() {
        TablePosition<?, ?> pos = getSelectionModel().getSelectedCells().stream()
                .findFirst().orElse(null);
        if (pos == null) return;
        List<String> row = getItems().get(pos.getRow());
        String value = pos.getColumn() < row.size() ? row.get(pos.getColumn()) : "";
        put(value == null ? "" : value);
    }

    private void copySelectedRow() {
        List<String> row = getSelectionModel().getSelectedItem();
        if (row == null) return;
        put(String.join("\t", row.stream().map(v -> v == null ? "" : v).toList()));
    }

    private static void put(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    /** Exports the currently displayed data to CSV. */
    public void exportCsv() {
        if (getItems().isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.setInitialFileName("export.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(getColumns().stream()
                    .map(c -> csvEscape(c.getText()))
                    .reduce((a, b) -> a + "," + b).orElse(""));
            for (List<String> row : getItems()) {
                out.println(row.stream()
                        .map(v -> csvEscape(v == null ? "" : v))
                        .reduce((a, b) -> a + "," + b).orElse(""));
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).showAndWait();
        }
    }

    private static String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}

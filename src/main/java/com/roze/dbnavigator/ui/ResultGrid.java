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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.util.CsvFormatEngine;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spreadsheet-like grid used everywhere results are shown.
 *
 * Uses standard ROW selection (not cell selection) with MULTIPLE mode: this
 * is the well-tested JavaFX pattern where a single click highlights an
 * entire row, Shift/Ctrl+click extends the highlighted rows exactly like
 * DataGrip, and double-clicking any individual cell still starts inline
 * editing for just that cell — the two behaviors are independent in JavaFX
 * and don't require cell-selection mode. (An earlier version of this class
 * used cell-selection mode, which is fragile in combination with a custom
 * editable cell factory and was the root cause of edits and multi-row
 * selection being unreliable.)
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
    private List<String> columnNames = List.of();

    public ResultGrid() {
        getStyleClass().add("result-grid");
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setPlaceholder(new Label("No data"));

        // Row selection (the JavaFX default) + MULTIPLE: click selects one row,
        // Shift+click selects a contiguous range, Ctrl+click toggles individual
        // rows — all highlighted across the full row, exactly like DataGrip.
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        MenuItem copyCell = new MenuItem("Copy Cell");
        copyCell.setOnAction(e -> copyFocusedCell());
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

        setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2
                    || isEditingCell() || !isEditable()) return;

            // Use the cell under the pointer instead of the focused cell. The
            // focus model can still point at the previous row while JavaFX is
            // processing a double-click, which made edits appear to do nothing
            // (or target a different cell).
            javafx.scene.Node node = e.getPickResult().getIntersectedNode();
            while (node != null && !(node instanceof TableCell<?, ?>)) node = node.getParent();
            if (!(node instanceof TableCell<?, ?> cell) || cell.isEmpty()
                    || cell.getTableColumn() == null || cell.getIndex() < 0) return;

            @SuppressWarnings({"rawtypes", "unchecked"})
            TableColumn<List<String>, ?> column = (TableColumn) cell.getTableColumn();
            // Column zero is the generated row number, not a database value.
            if (getColumns().indexOf(column) > 0) edit(cell.getIndex(), column);
        });

        applySettings();
    }

    public void applySettings() {
        try {
            AppSettingsStore.Settings settings = AppSettingsStore.load();
            if (settings.isUseCustomFont()) {
                setStyle(String.format(Locale.US,
                        "-fx-font-family: '%s'; -fx-font-size: %.1fpt;",
                        settings.getCustomFontFamily(), settings.getCustomFontSize()));
            } else {
                setStyle("");
            }
            if (settings.isAlternateRowColors()) {
                getStyleClass().remove("no-zebra");
            } else {
                if (!getStyleClass().contains("no-zebra")) {
                    getStyleClass().add("no-zebra");
                }
            }
        } catch (Exception ignored) {}
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

    /** Selected row indexes, descending (safe order for sequential removal). */
    public List<Integer> getSelectedRowIndexes() {
        List<Integer> rows = new ArrayList<>(getSelectionModel().getSelectedIndices());
        rows.sort((a, b) -> b - a);
        return rows;
    }

    /** How the owner (QueryTab/DataTab) should re-fetch when a sort icon is clicked — see class javadoc. */
    public interface SortRequestListener {
        void onSortRequested(String columnName, String direction);
    }

    private SortRequestListener sortRequestListener;
    private String currentSortColumn;
    private String currentSortDirection;   // "ASC", "DESC", or null

    public void setSortRequestListener(SortRequestListener listener) {
        this.sortRequestListener = listener;
    }

    /** Called by the owner once it knows what's actually applied, so the right column's icon reflects it. */
    public void setCurrentSort(String columnName, String direction) {
        this.currentSortColumn = columnName;
        this.currentSortDirection = direction;
    }

    private int rowNumberOffset = 0;

    /** Lets a paginated view show true overall row numbers (e.g. page 2 starts at 501, not 1). */
    public void setRowNumberOffset(int offset) {
        this.rowNumberOffset = offset;
    }

    public void showResult(QueryResult result) {
        getColumns().clear();
        getItems().clear();
        if (result == null || !result.isResultSet()) return;

        applySettings();

        TableColumn<List<String>, Void> serialCol = new TableColumn<>("#");
        serialCol.setSortable(false);
        serialCol.setPrefWidth(56);
        serialCol.getStyleClass().add("serial-column");
        serialCol.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("serial-cell");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1 + rowNumberOffset));
            }
        });
        getColumns().add(serialCol);

        this.columnTypes = result.getColumnTypes();
        List<String> columnNames = result.getColumns();
        this.columnNames = List.copyOf(columnNames);
        for (int i = 0; i < columnNames.size(); i++) {
            final int index = i;
            String columnName = columnNames.get(i);
            TableColumn<List<String>, String> col = new TableColumn<>(columnName);
            col.setCellValueFactory(data -> {
                List<String> row = data.getValue();
                String raw = index < row.size() ? row.get(index) : null;
                return new ReadOnlyStringWrapper(formatDisplayValue(raw, index));
            });
            col.setPrefWidth(Math.max(90, Math.min(280, columnNames.get(i).length() * 12 + 40)));
            if (columnNames.get(i).equalsIgnoreCase("ctid")
                    || columnNames.get(i).equalsIgnoreCase("tableoid")) col.setVisible(false);

            col.setSortable(false);
            col.setText(null);
            col.setGraphic(buildSortableHeader(columnName));

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
            } else {
                col.setCellFactory(c -> new ReadOnlyCell(index));
            }
            getColumns().add(col);
        }

        ObservableList<List<String>> items = FXCollections.observableArrayList(result.getRows());
        setItems(items);
    }

    /**
     * DataGrip-style sort control: a name label plus a small icon that always
     * shows a stacked up/down arrow, brightening the relevant arrow when this
     * column is the active sort. Clicking cycles none → ascending →
     * descending → none, each step calling {@link #sortRequestListener} so
     * the owner can actually re-fetch sorted data — the icon itself never
     * reorders anything on its own.
     */
    private HBox buildSortableHeader(String columnName) {
        Label nameLabel = new Label(columnName);
        nameLabel.getStyleClass().add("grid-header-label");

        boolean isActive = columnName.equals(currentSortColumn);
        String activeDirection = isActive ? currentSortDirection : null;

        FontIcon sortIcon = Icons.of(
                "DESC".equals(activeDirection) ? FontAwesomeSolid.SORT_DOWN
                        : "ASC".equals(activeDirection) ? FontAwesomeSolid.SORT_UP
                        : FontAwesomeSolid.SORT,
                isActive ? "#6897bb" : "#6f7680", 12);
        sortIcon.getStyleClass().add("grid-sort-icon");

        HBox box = new HBox(6, nameLabel, sortIcon);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setOnMouseClicked(e -> {
            if (sortRequestListener == null) return;
            String next = "ASC".equals(activeDirection) ? "DESC" : isActive ? null : "ASC";
            sortRequestListener.onSortRequested(columnName, next);
        });
        return box;
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
     * Commits on Enter or when focus leaves the field; Escape cancels.
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
            // Commit even if the user clicks elsewhere instead of pressing Enter —
            // matches spreadsheet/DataGrip behavior where focus-out saves the cell.
            field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused && isEditing()) {
                    commitEdit(field.getText());
                }
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
            } else if (isBooleanColumn(columnIndex) && "Checkboxes".equalsIgnoreCase(AppSettingsStore.load().getShowBooleanValuesAs()) && item != null && !item.equalsIgnoreCase("NULL")) {
                CheckBox cb = new CheckBox();
                cb.setSelected("true".equalsIgnoreCase(item) || "1".equals(item) || "t".equalsIgnoreCase(item));
                cb.setDisable(!isEditable());
                cb.setStyle("-fx-opacity: 1.0;");
                if (isEditable()) {
                    cb.setOnAction(e -> {
                        String newVal = cb.isSelected() ? "true" : "false";
                        commitEdit(newVal);
                    });
                }
                setGraphic(cb);
                setText(null);
            } else {
                setText(item);
                setGraphic(null);
            }
        }
    }

    private class ReadOnlyCell extends TableCell<List<String>, String> {
        private final int columnIndex;

        ReadOnlyCell(int columnIndex) {
            this.columnIndex = columnIndex;
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else if (isBooleanColumn(columnIndex) && "Checkboxes".equalsIgnoreCase(AppSettingsStore.load().getShowBooleanValuesAs()) && !item.equalsIgnoreCase("NULL")) {
                CheckBox cb = new CheckBox();
                cb.setSelected("true".equalsIgnoreCase(item) || "1".equals(item) || "t".equalsIgnoreCase(item));
                cb.setDisable(true);
                cb.setStyle("-fx-opacity: 1.0;");
                setGraphic(cb);
                setText(null);
            } else {
                setGraphic(null);
                setText(item);
            }
        }
    }

    private String formatDisplayValue(String rawValue, int columnIndex) {
        if (rawValue == null) return "NULL";
        AppSettingsStore.Settings settings = AppSettingsStore.load();

        // Byte limit truncation
        int maxBytes = settings.getMaxBytesLoadedPerValue();
        if (maxBytes > 0 && rawValue.length() > maxBytes) {
            rawValue = rawValue.substring(0, maxBytes) + "… [truncated]";
        }

        // Binary detection
        if (isBinaryColumn(columnIndex)) {
            if (settings.isDetectBinaryAsUuid() && isUuidLike(rawValue)) {
                return formatUuid(rawValue);
            }
        }

        // Numeric column formatting
        if (isNumericColumn(columnIndex)) {
            if (rawValue.equalsIgnoreCase("Infinity")) return settings.getInfinityText();
            if (rawValue.equalsIgnoreCase("-Infinity")) return "-" + settings.getInfinityText();
            if (rawValue.equalsIgnoreCase("NaN")) return settings.getNanText();

            if (settings.isEnableNumberPattern() || settings.isEnableGroupingSeparator() || !settings.getDecimalSeparator().equals(".")) {
                try {
                    double num = Double.parseDouble(rawValue);
                    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                    if (!settings.getDecimalSeparator().isEmpty()) {
                        symbols.setDecimalSeparator(settings.getDecimalSeparator().charAt(0));
                    }
                    if (settings.isEnableGroupingSeparator() && !settings.getGroupingSeparator().isEmpty()) {
                        symbols.setGroupingSeparator(settings.getGroupingSeparator().charAt(0));
                    }
                    DecimalFormat df;
                    if (settings.isEnableNumberPattern() && !settings.getNumberPattern().isBlank()) {
                        df = new DecimalFormat(settings.getNumberPattern(), symbols);
                    } else {
                        df = new DecimalFormat("#,##0.######", symbols);
                    }
                    df.setGroupingUsed(settings.isEnableGroupingSeparator());
                    return df.format(num);
                } catch (Exception ignored) {}
            }
        }

        // Date/Time formatting
        if (isDateColumn(columnIndex)) {
            try {
                if (settings.isEnableDate() && rawValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    LocalDate d = LocalDate.parse(rawValue);
                    return d.format(DateTimeFormatter.ofPattern(settings.getDatePattern(), Locale.US));
                }
                if (settings.isEnableDatetimeTimestamp() && rawValue.contains(" ") && !rawValue.contains("+") && !rawValue.contains("Z")) {
                    String clean = rawValue.length() > 19 ? rawValue.substring(0, 19) : rawValue;
                    clean = clean.replace('T', ' ');
                    DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);
                    LocalDateTime dt = LocalDateTime.parse(clean, inFmt);
                    return dt.format(DateTimeFormatter.ofPattern(settings.getDatetimeTimestampPattern(), Locale.US));
                }
            } catch (Exception ignored) {}
        }

        return rawValue;
    }

    private boolean isBooleanColumn(int index) {
        if (index >= columnTypes.size()) return false;
        String type = columnTypes.get(index).toLowerCase(Locale.ROOT);
        return type.contains("bool") || type.contains("bit");
    }

    private boolean isNumericColumn(int index) {
        if (index >= columnTypes.size()) return false;
        String type = columnTypes.get(index).toLowerCase(Locale.ROOT);
        return type.contains("int") || type.contains("num") || type.contains("dec")
                || type.contains("float") || type.contains("double") || type.contains("real");
    }

    private boolean isBinaryColumn(int index) {
        if (index >= columnTypes.size()) return false;
        String type = columnTypes.get(index).toLowerCase(Locale.ROOT);
        return type.contains("blob") || type.contains("byte") || type.contains("binary") || type.contains("raw");
    }

    private static boolean isUuidLike(String s) {
        return s != null && s.replace("-", "").matches("[0-9a-fA-F]{32}");
    }

    private static String formatUuid(String s) {
        String clean = s.replace("-", "");
        if (clean.length() == 32) {
            return clean.substring(0, 8) + "-" + clean.substring(8, 12) + "-" + clean.substring(12, 16) + "-" + clean.substring(16, 20) + "-" + clean.substring(20);
        }
        return s;
    }

    // ---------------------------------------------------------- clipboard

    /** Copies the value of whatever cell last had focus (works in row-selection mode). */
    private void copyFocusedCell() {
        TablePosition<?, ?> pos = getFocusModel().getFocusedCell();
        if (pos == null || pos.getRow() < 0 || pos.getTableColumn() == null) return;
        List<String> row = getItems().get(pos.getRow());
        int colIndex = getColumns().indexOf(pos.getTableColumn()) - 1;   // -1: serial column is index 0
        if (colIndex < 0) return;   // the serial column itself has nothing meaningful to copy
        String value = (colIndex < row.size()) ? row.get(colIndex) : "";
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

        AppSettingsStore.Settings settings = AppSettingsStore.load();
        AppSettingsStore.CsvFormatConfig config = settings.getCsvFormatByName(settings.getDefaultCsvFormat());

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            String csvText = CsvFormatEngine.formatData(columnNames, getItems(), config, true);
            out.print(csvText);
        } catch (IOException e) {
            DialogTheme.apply(new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage())).showAndWait();
        }
    }

    /**
     * Exports the current rows as a JSON array of objects, keyed by column
     * name. Any cell whose text already looks like a JSON object or array —
     * which is how nested documents/arrays are rendered for display, since
     * the grid only ever holds flattened strings — is embedded as real
     * nested JSON rather than a doubly-escaped string, so the output stays
     * structurally faithful to the original document shape.
     */
    public void exportJson() {
        if (getItems().isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export JSON");
        chooser.setInitialFileName("export.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println("[");
            List<List<String>> rows = getItems();
            for (int r = 0; r < rows.size(); r++) {
                List<String> row = rows.get(r);
                StringBuilder obj = new StringBuilder("  {");
                for (int c = 0; c < columnNames.size(); c++) {
                    if (c > 0) obj.append(", ");
                    String value = c < row.size() ? row.get(c) : null;
                    obj.append('"').append(jsonEscape(columnNames.get(c))).append("\": ")
                            .append(jsonValue(value));
                }
                obj.append('}');
                if (r < rows.size() - 1) obj.append(',');
                out.println(obj);
            }
            out.println("]");
        } catch (IOException e) {
            DialogTheme.apply(new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage())).showAndWait();
        }
    }

    private static String jsonValue(String value) {
        if (value == null || value.equals("NULL")) return "null";
        String trimmed = value.strip();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;   // already-serialized nested document/array — embed as-is
        }
        return '"' + jsonEscape(value) + '"';
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}

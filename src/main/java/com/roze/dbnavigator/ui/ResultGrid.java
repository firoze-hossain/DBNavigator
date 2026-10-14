package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.QueryResult;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Spreadsheet-like grid used everywhere results are shown.
 *
 * Uses cell selection mode with MULTIPLE mode: single click selects an
 * individual cell, Ctrl+click / Shift+click extends the cell selection,
 * double-clicking or pressing Enter starts inline editing, and editing
 * commits across all selected cells in that column.
 */
public class ResultGrid extends TableView<List<String>> {

    /** Fired when the user commits an inline cell edit. */
    @FunctionalInterface
    public interface CellEditListener {
        void onEdit(int rowIndex, int columnIndex, String oldValue, String newValue);
    }

    private static final PseudoClass MODIFIED_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("cell-modified");
    private static final PseudoClass DELETED_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("row-deleted");

    public record CellKey(int rowIndex, int columnIndex) {}
    private final Set<CellKey> modifiedCells = new HashSet<>();
    private final Set<Integer> deletedRowIndices = new HashSet<>();
    private java.util.function.Consumer<Integer> revertRowAction;
    private int lastRowHeaderIndex = -1;
    private int anchorRowIndex = -1;
    private TableColumn<List<String>, ?> anchorColumn = null;
    private boolean isHandlingShiftClick = false;
    private boolean consumedPress = false;

    public void markCellModified(int rowIndex, int columnIndex) {
        modifiedCells.add(new CellKey(rowIndex, columnIndex));
        refresh();
    }

    public void clearModifiedCells() {
        modifiedCells.clear();
        refresh();
    }

    public boolean isCellModified(int rowIndex, int columnIndex) {
        return !isRowDeleted(rowIndex) && modifiedCells.contains(new CellKey(rowIndex, columnIndex));
    }

    public void markRowDeleted(int rowIndex) {
        deletedRowIndices.add(rowIndex);
        refresh();
    }

    public void unmarkRowDeleted(int rowIndex) {
        deletedRowIndices.remove(rowIndex);
        refresh();
    }

    public boolean isRowDeleted(int rowIndex) {
        return deletedRowIndices.contains(rowIndex);
    }

    public void clearDeletedRows() {
        deletedRowIndices.clear();
        refresh();
    }

    public Set<Integer> getDeletedRowIndices() {
        return java.util.Collections.unmodifiableSet(deletedRowIndices);
    }

    public void setRevertRowAction(java.util.function.Consumer<Integer> action) {
        this.revertRowAction = action;
    }

    private CellEditListener editListener;
    private Runnable deleteRowsAction;
    private Runnable addRowAction;
    private Runnable reloadAction;
    private List<String> columnTypes = List.of();
    private List<String> columnNames = List.of();

    public ResultGrid() {
        getStyleClass().add("result-grid");
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setPlaceholder(new Label("No data"));

        // Cell selection mode: single click selects a cell, Ctrl+click adds cells,
        // Shift+click selects cell ranges.
        getSelectionModel().setCellSelectionEnabled(true);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        getFocusModel().focusedCellProperty().addListener((obs, oldVal, newVal) -> {
            if (!isHandlingShiftClick && newVal != null && newVal.getRow() >= 0) {
                lastRowHeaderIndex = newVal.getRow();
            }
        });

        setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(List<String> item, boolean empty) {
                super.updateItem(item, empty);
                pseudoClassStateChanged(DELETED_PSEUDO_CLASS, !empty && isRowDeleted(getIndex()));
            }
        });

        // Clicking outside an active editor cell automatically commits the edit
        // and enables the submit button.
        this.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            TablePosition<?, ?> editing = getEditingCell();
            if (editing != null && editing.getRow() >= 0 && editing.getColumn() >= 0) {
                if (event.getTarget() instanceof Node targetNode) {
                    if (isInsideActiveEditor(targetNode)) {
                        return;
                    }
                }
                edit(-1, null);
            }

            if (event.getButton() == MouseButton.PRIMARY) {
                consumedPress = false;
                Node node = event.getPickResult().getIntersectedNode();
                while (node != null && !(node instanceof TableCell<?, ?>)) node = node.getParent();
                if (node instanceof TableCell<?, ?> cell && !cell.isEmpty()
                        && cell.getIndex() >= 0 && cell.getIndex() < getItems().size()) {
                    int r = cell.getIndex();
                    boolean isSerial = cell.getTableColumn() == null
                            || (getColumns().size() > 0 && cell.getTableColumn() == getColumns().get(0));
                    boolean shift = event.isShiftDown();
                    boolean ctrl = event.isControlDown() || event.isShortcutDown();

                    if (isSerial) {
                        handleSerialClick(r, shift, ctrl);
                        requestFocus();
                        event.consume();
                        consumedPress = true;
                        return;
                    } else if (shift) {
                        int anchor = anchorRowIndex >= 0 ? anchorRowIndex : (lastRowHeaderIndex >= 0 ? lastRowHeaderIndex : 0);
                        @SuppressWarnings("unchecked")
                        TableColumn<List<String>, ?> col = (TableColumn<List<String>, ?>) cell.getTableColumn();
                        TableColumn<List<String>, ?> anchorCol = anchorColumn != null ? anchorColumn : col;
                        isHandlingShiftClick = true;
                        try {
                            if (anchorColumn == null && hasFullRowSelected()) {
                                selectRowRange(anchor, r, ctrl);
                            } else {
                                selectCellRange(anchor, anchorCol, r, col, ctrl);
                            }
                            if (col != null) {
                                getFocusModel().focus(r, col);
                            }
                        } finally {
                            isHandlingShiftClick = false;
                        }
                        requestFocus();
                        event.consume();
                        consumedPress = true;
                        return;
                    } else if (ctrl && hasFullRowSelected() && anchorColumn == null) {
                        toggleRowSelection(r);
                        anchorRowIndex = r;
                        anchorColumn = null;
                        lastRowHeaderIndex = r;
                        if (cell.getTableColumn() != null) {
                            @SuppressWarnings("unchecked")
                            TableColumn<List<String>, ?> col = (TableColumn<List<String>, ?>) cell.getTableColumn();
                            getFocusModel().focus(r, col);
                        }
                        requestFocus();
                        event.consume();
                        consumedPress = true;
                        return;
                    } else {
                        anchorRowIndex = r;
                        @SuppressWarnings("unchecked")
                        TableColumn<List<String>, ?> col = (TableColumn<List<String>, ?>) cell.getTableColumn();
                        anchorColumn = col;
                        lastRowHeaderIndex = r;
                    }
                }
            }

            // Right-click: if right-clicking on an already selected cell, preserve
            // the multi-cell selection. If clicked on an unselected cell, select it.
            if (event.getButton() == MouseButton.SECONDARY) {
                Node node = event.getPickResult().getIntersectedNode();
                while (node != null && !(node instanceof TableCell<?, ?>)) node = node.getParent();
                if (node instanceof TableCell<?, ?> cell && !cell.isEmpty() && cell.getTableColumn() != null && cell.getIndex() >= 0) {
                    @SuppressWarnings("unchecked")
                    TableColumn<List<String>, ?> col = (TableColumn<List<String>, ?>) cell.getTableColumn();
                    int r = cell.getIndex();
                    boolean alreadySelected = false;
                    for (TablePosition<?, ?> pos : getSelectionModel().getSelectedCells()) {
                        if (pos.getRow() == r && pos.getTableColumn() == col) {
                            alreadySelected = true;
                            break;
                        }
                    }
                    if (!alreadySelected) {
                        getSelectionModel().clearAndSelect(r, col);
                    }
                }
            }
        });

        this.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (consumedPress) {
                event.consume();
            }
        });

        this.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (consumedPress) {
                consumedPress = false;
                event.consume();
            }
        });

        buildContextMenu();

        setOnKeyPressed(e -> {
            if (isEditingCell()) return;
            if (((e.isControlDown() && e.getCode() == KeyCode.Y) || e.getCode() == KeyCode.DELETE) && deleteRowsAction != null) {
                deleteRowsAction.run();
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.F2 || e.getCode() == KeyCode.SPACE) {
                if (isEditable()) {
                    editFocusedCell();
                    e.consume();
                }
            } else if (e.isControlDown() && e.getCode() == KeyCode.C) {
                copySelection();
                e.consume();
            } else if (e.isControlDown() && e.isAltDown() && e.getCode() == KeyCode.N) {
                if (isEditable()) {
                    setSelectedCellsValue("NULL");
                    e.consume();
                }
            } else if (e.isControlDown() && e.isAltDown() && e.getCode() == KeyCode.D) {
                if (isEditable()) {
                    setSelectedCellsValue("DEFAULT");
                    e.consume();
                }
            } else if ((e.isControlDown() && e.getCode() == KeyCode.F5) || e.getCode() == KeyCode.F5) {
                if (reloadAction != null) {
                    reloadAction.run();
                    e.consume();
                }
            } else if (!e.isControlDown() && !e.isAltDown() && !e.isMetaDown() && e.getText() != null && !e.getText().isEmpty()) {
                char ch = e.getText().charAt(0);
                if (ch >= 32 && ch != 127) {
                    if (isEditable()) {
                        editFocusedCell();
                        Platform.runLater(() -> {
                            Node focusOwner = getScene() != null ? getScene().getFocusOwner() : null;
                            if (focusOwner instanceof TextField tf) {
                                tf.setText(String.valueOf(ch));
                                tf.positionCaret(1);
                            }
                        });
                        e.consume();
                    }
                }
            }
        });

        setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || isEditingCell() || !isEditable()) return;

            Node node = e.getPickResult().getIntersectedNode();
            while (node != null && !(node instanceof TableCell<?, ?>)) node = node.getParent();
            if (!(node instanceof TableCell<?, ?> cell) || cell.isEmpty()
                    || cell.getTableColumn() == null || cell.getIndex() < 0) return;

            @SuppressWarnings({"rawtypes", "unchecked"})
            TableColumn<List<String>, ?> column = (TableColumn) cell.getTableColumn();
            int colIdx = getColumns().indexOf(column);
            if (colIdx <= 0) return; // Column zero is the generated row number

            if (e.getClickCount() == 2) {
                if (!isRowDeleted(cell.getIndex())) {
                    edit(cell.getIndex(), column);
                }
            }
        });

        applySettings();
    }

    public void setAddRowAction(Runnable action) {
        this.addRowAction = action;
    }

    public void editFocusedCell() {
        if (!isEditable()) return;
        var selected = getSelectionModel().getSelectedCells();
        TablePosition<?, ?> pos = null;
        if (!selected.isEmpty()) {
            TablePosition<?, ?> focused = getFocusModel().getFocusedCell();
            if (focused != null && selected.contains(focused)) {
                pos = focused;
            } else {
                pos = selected.get(0);
            }
        } else {
            pos = getFocusModel().getFocusedCell();
        }
        if (pos != null && pos.getRow() >= 0 && pos.getTableColumn() != null) {
            if (isRowDeleted(pos.getRow())) return;
            int colIdx = getColumns().indexOf(pos.getTableColumn());
            if (colIdx > 0) {
                edit(pos.getRow(), (TableColumn<List<String>, ?>) pos.getTableColumn());
            }
        }
    }

    public void selectFullRow(int rowIndex, boolean keepExisting) {
        if (!keepExisting) {
            getSelectionModel().clearSelection();
        }
        for (int c = 0; c < getColumns().size(); c++) {
            getSelectionModel().select(rowIndex, getColumns().get(c));
        }
    }

    public void selectRowRange(int fromRow, int toRow, boolean keepExisting) {
        if (!keepExisting) {
            getSelectionModel().clearSelection();
        }
        if (getItems().isEmpty()) return;
        int start = Math.max(0, Math.min(fromRow, toRow));
        int end = Math.min(getItems().size() - 1, Math.max(fromRow, toRow));
        for (int r = start; r <= end; r++) {
            for (int c = 0; c < getColumns().size(); c++) {
                getSelectionModel().select(r, getColumns().get(c));
            }
        }
    }

    public void selectCellRange(int fromRow, TableColumn<List<String>, ?> fromCol,
                                int toRow, TableColumn<List<String>, ?> toCol,
                                boolean keepExisting) {
        if (!keepExisting) {
            getSelectionModel().clearSelection();
        }
        if (getItems().isEmpty() || getColumns().isEmpty()) return;
        int startRow = Math.max(0, Math.min(fromRow, toRow));
        int endRow = Math.min(getItems().size() - 1, Math.max(fromRow, toRow));

        int startColIdx = fromCol != null ? getColumns().indexOf(fromCol) : (getColumns().size() > 1 ? 1 : 0);
        int endColIdx = toCol != null ? getColumns().indexOf(toCol) : startColIdx;
        if (startColIdx < 0) startColIdx = getColumns().size() > 1 ? 1 : 0;
        if (endColIdx < 0) endColIdx = startColIdx;

        int minCol = Math.min(startColIdx, endColIdx);
        int maxCol = Math.max(startColIdx, endColIdx);

        for (int r = startRow; r <= endRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                getSelectionModel().select(r, getColumns().get(c));
            }
        }
    }

    public boolean isRowSelected(int rowIndex) {
        for (TablePosition<?, ?> pos : getSelectionModel().getSelectedCells()) {
            if (pos.getRow() == rowIndex) {
                return true;
            }
        }
        return false;
    }

    public boolean isRowFullySelected(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= getItems().size() || getColumns().isEmpty()) return false;
        for (int c = 0; c < getColumns().size(); c++) {
            if (!getSelectionModel().isSelected(rowIndex, getColumns().get(c))) {
                return false;
            }
        }
        return true;
    }

    public boolean hasFullRowSelected() {
        for (int r : getSelectedRowIndexes()) {
            if (isRowFullySelected(r)) return true;
        }
        return false;
    }

    public void toggleRowSelection(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= getItems().size()) return;
        boolean fullySelected = isRowFullySelected(rowIndex);
        if (fullySelected) {
            for (int c = 0; c < getColumns().size(); c++) {
                getSelectionModel().clearSelection(rowIndex, getColumns().get(c));
            }
        } else {
            for (int c = 0; c < getColumns().size(); c++) {
                getSelectionModel().select(rowIndex, getColumns().get(c));
            }
        }
    }

    public void handleSerialClick(int rowIndex, boolean shift, boolean ctrl) {
        if (rowIndex < 0 || rowIndex >= getItems().size()) return;

        if (shift) {
            int anchor = anchorRowIndex >= 0 ? anchorRowIndex : (lastRowHeaderIndex >= 0 ? lastRowHeaderIndex : 0);
            isHandlingShiftClick = true;
            try {
                selectRowRange(anchor, rowIndex, ctrl);
                if (!getColumns().isEmpty()) {
                    getFocusModel().focus(rowIndex, getColumns().get(0));
                }
            } finally {
                isHandlingShiftClick = false;
            }
        } else if (ctrl) {
            toggleRowSelection(rowIndex);
            anchorRowIndex = rowIndex;
            anchorColumn = null;
            lastRowHeaderIndex = rowIndex;
            if (!getColumns().isEmpty()) {
                getFocusModel().focus(rowIndex, getColumns().get(0));
            }
        } else {
            selectFullRow(rowIndex, false);
            anchorRowIndex = rowIndex;
            anchorColumn = null;
            lastRowHeaderIndex = rowIndex;
            if (!getColumns().isEmpty()) {
                getFocusModel().focus(rowIndex, getColumns().get(0));
            }
        }
    }

    public void setSelectedCellsValue(String value) {
        if (!isEditable() || editListener == null) return;
        List<TablePosition> cells = new ArrayList<>(getSelectionModel().getSelectedCells());
        if (cells.isEmpty()) return;

        for (TablePosition<?, ?> pos : cells) {
            int r = pos.getRow();
            int c = getColumns().indexOf(pos.getTableColumn()) - 1;
            if (r >= 0 && r < getItems().size() && c >= 0) {
                List<String> row = getItems().get(r);
                String oldVal = c < row.size() ? row.get(c) : null;
                if (value != null && !value.equals(oldVal == null ? "NULL" : oldVal)) {
                    row.set(c, value);
                    editListener.onEdit(r, c, oldVal, value);
                    markCellModified(r, c);
                }
            }
        }
        Platform.runLater(this::refresh);
    }

    public void commitValueAcrossSelection(int primaryRow, TableColumn<List<String>, String> column, int colIndex, String newValue) {
        List<Integer> targetRows = new ArrayList<>();
        targetRows.add(primaryRow);
        for (TablePosition<?, ?> pos : getSelectionModel().getSelectedCells()) {
            if (pos.getTableColumn() == column && pos.getRow() >= 0 && pos.getRow() != primaryRow) {
                if (!targetRows.contains(pos.getRow())) {
                    targetRows.add(pos.getRow());
                }
            }
        }

        for (int r : targetRows) {
            if (r >= 0 && r < getItems().size()) {
                List<String> row = getItems().get(r);
                String oldValue = colIndex < row.size() ? row.get(colIndex) : null;
                if (newValue != null && !newValue.equals(oldValue == null ? "NULL" : oldValue)) {
                    row.set(colIndex, newValue);
                    if (editListener != null) {
                        editListener.onEdit(r, colIndex, oldValue, newValue);
                    }
                    markCellModified(r, colIndex);
                }
            }
        }
        Platform.runLater(this::refresh);
    }

    private void buildContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setAccelerator(new KeyCodeCombination(KeyCode.ENTER));
        editItem.setOnAction(e -> editFocusedCell());

        MenuItem showRecordView = new MenuItem("Show Record View");
        showRecordView.setAccelerator(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        showRecordView.setOnAction(e -> openRecordView());

        MenuItem valueEditor = new MenuItem("Open in Value Editor");
        valueEditor.setAccelerator(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN));
        valueEditor.setOnAction(e -> openValueEditor());

        MenuItem showAggregateView = new MenuItem("Show Aggregate View");
        showAggregateView.setOnAction(e -> showAggregateSummary());

        MenuItem revertSelected = new MenuItem("Revert Selected");
        revertSelected.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN));
        revertSelected.setOnAction(e -> revertSelectedCells());

        Menu setHighlighting = new Menu("Set Highlighting Language...");
        for (String lang : List.of("Text", "JSON", "SQL", "XML", "HTML")) {
            MenuItem langItem = new MenuItem(lang);
            langItem.setOnAction(ev -> {});
            setHighlighting.getItems().add(langItem);
        }

        MenuItem setDefaultItem = new MenuItem("Set DEFAULT");
        setDefaultItem.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN));
        setDefaultItem.setOnAction(e -> setSelectedCellsValue("DEFAULT"));

        MenuItem setNullItem = new MenuItem("Set NULL");
        setNullItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN));
        setNullItem.setOnAction(e -> setSelectedCellsValue("NULL"));

        MenuItem copySelectionItem = new MenuItem("Copy Using Data Extractor (CSV)");
        copySelectionItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copySelectionItem.setOnAction(e -> copySelection());

        Menu changeExtractor = new Menu("Change Data Extractor (CSV)");
        for (String ext : List.of("CSV", "TSV", "JSON", "SQL Insert Statements", "HTML Table", "Markdown")) {
            MenuItem extItem = new MenuItem(ext);
            extItem.setOnAction(ev -> {});
            changeExtractor.getItems().add(extItem);
        }

        MenuItem copyRowItem = new MenuItem("Copy Selected Row (TSV)");
        copyRowItem.setOnAction(e -> copySelectedRow());

        MenuItem copyAggregationItem = new MenuItem("Copy Aggregation Result (SUM)");
        copyAggregationItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        copyAggregationItem.setOnAction(e -> copyAggregationResult());

        MenuItem addRowItem = new MenuItem("Add Row");
        addRowItem.setAccelerator(new KeyCodeCombination(KeyCode.INSERT, KeyCombination.ALT_DOWN));
        addRowItem.setOnAction(e -> { if (addRowAction != null) addRowAction.run(); });

        MenuItem deleteRowsItem = new MenuItem("Delete Rows");
        deleteRowsItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        deleteRowsItem.setOnAction(e -> { if (deleteRowsAction != null) deleteRowsAction.run(); });

        MenuItem compareCells = new MenuItem("Compare Cells...");
        compareCells.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        compareCells.setOnAction(e -> compareSelectedCells());

        Menu goToMenu = new Menu("Go To");
        MenuItem goToFirst = new MenuItem("First Row");
        goToFirst.setOnAction(ev -> {
            if (!getItems().isEmpty()) {
                getSelectionModel().clearAndSelect(0, getColumns().size() > 1 ? getColumns().get(1) : getColumns().get(0));
                scrollTo(0);
            }
        });
        MenuItem goToLast = new MenuItem("Last Row");
        goToLast.setOnAction(ev -> {
            if (!getItems().isEmpty()) {
                int last = getItems().size() - 1;
                getSelectionModel().clearAndSelect(last, getColumns().size() > 1 ? getColumns().get(1) : getColumns().get(0));
                scrollTo(last);
            }
        });
        goToMenu.getItems().addAll(goToFirst, goToLast);

        Menu filterByMenu = new Menu("Filter by");
        MenuItem filterVal = new MenuItem("Filter by Cell Value");
        filterVal.setOnAction(ev -> filterBySelectedValue(true));
        MenuItem filterNotVal = new MenuItem("Filter by Not Equal");
        filterNotVal.setOnAction(ev -> filterBySelectedValue(false));
        filterByMenu.getItems().addAll(filterVal, filterNotVal);

        MenuItem fullTextSearch = new MenuItem("Full-Text Search...");
        fullTextSearch.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));

        MenuItem exportTable = new MenuItem("Export Table to Clipboard");
        exportTable.setOnAction(ev -> exportTableToClipboard());

        MenuItem setLocalFilter = new MenuItem("Set Local Filter");
        MenuItem localFilterByCell = new MenuItem("Local Filter By Cell Value");
        localFilterByCell.setOnAction(ev -> filterBySelectedValue(true));

        MenuItem quickDoc = new MenuItem("Quick Documentation");
        quickDoc.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        quickDoc.setOnAction(e -> showQuickDocumentation());

        MenuItem reloadPage = new MenuItem("Reload Page");
        reloadPage.setAccelerator(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN));
        reloadPage.setOnAction(e -> {
            if (reloadAction != null) reloadAction.run();
        });

        menu.getItems().addAll(
                editItem,
                new SeparatorMenuItem(),
                showRecordView,
                valueEditor,
                showAggregateView,
                new SeparatorMenuItem(),
                revertSelected,
                setHighlighting,
                new SeparatorMenuItem(),
                setDefaultItem,
                setNullItem,
                new SeparatorMenuItem(),
                copySelectionItem,
                changeExtractor,
                copyRowItem,
                copyAggregationItem,
                new SeparatorMenuItem(),
                addRowItem,
                deleteRowsItem,
                new SeparatorMenuItem(),
                compareCells,
                new SeparatorMenuItem(),
                goToMenu,
                filterByMenu,
                fullTextSearch,
                exportTable,
                new SeparatorMenuItem(),
                setLocalFilter,
                localFilterByCell,
                new SeparatorMenuItem(),
                quickDoc,
                new SeparatorMenuItem(),
                reloadPage
        );

        menu.setOnShowing(e -> {
            boolean canEdit = isEditable();
            boolean hasSelection = !getSelectionModel().getSelectedCells().isEmpty();
            editItem.setDisable(!canEdit || !hasSelection);
            setDefaultItem.setDisable(!canEdit || !hasSelection);
            setNullItem.setDisable(!canEdit || !hasSelection);
            addRowItem.setDisable(!canEdit || addRowAction == null);
            deleteRowsItem.setDisable(!canEdit || deleteRowsAction == null || getSelectedRowIndexes().isEmpty());
            valueEditor.setDisable(!hasSelection);
            copySelectionItem.setDisable(!hasSelection);
            compareCells.setDisable(getSelectionModel().getSelectedCells().size() < 2);
        });

        setContextMenu(menu);
    }

    private void openValueEditor() {
        TablePosition<?, ?> pos = getFocusModel().getFocusedCell();
        if (pos == null || pos.getRow() < 0 || pos.getTableColumn() == null) {
            var selected = getSelectionModel().getSelectedCells();
            if (!selected.isEmpty()) pos = selected.get(0);
        }
        if (pos == null || pos.getRow() < 0 || pos.getTableColumn() == null) return;
        int rowIdx = pos.getRow();
        int colIdx = getColumns().indexOf(pos.getTableColumn()) - 1;
        if (colIdx < 0 || rowIdx >= getItems().size()) return;

        List<String> row = getItems().get(rowIdx);
        String val = colIdx < row.size() ? row.get(colIdx) : "";
        String colName = colIdx < columnNames.size() ? columnNames.get(colIdx) : "Value";

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Value Editor - " + colName);
        dialog.setHeaderText("Row " + (rowIdx + 1) + " ▸ " + colName);
        DialogTheme.apply(dialog);

        TextArea textArea = new TextArea(val == null ? "" : val);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(12);
        textArea.setPrefColumnCount(45);
        textArea.setEditable(isEditable());

        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? textArea.getText() : null);

        @SuppressWarnings("unchecked")
        TableColumn<List<String>, String> targetCol = (TableColumn<List<String>, String>) pos.getTableColumn();
        dialog.showAndWait().ifPresent(newVal -> {
            if (isEditable()) {
                commitValueAcrossSelection(rowIdx, targetCol, colIdx, newVal);
            }
        });
    }

    private void openRecordView() {
        TablePosition<?, ?> pos = getFocusModel().getFocusedCell();
        int rowIdx = pos != null && pos.getRow() >= 0 ? pos.getRow() : 0;
        if (rowIdx >= getItems().size() || getItems().isEmpty()) return;

        List<String> row = getItems().get(rowIdx);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Record View - Row " + (rowIdx + 1));
        dialog.setHeaderText("Record Details (#" + (rowIdx + 1) + " of " + getItems().size() + ")");
        DialogTheme.apply(dialog);

        VBox content = new VBox(8);
        content.setPrefWidth(420);
        for (int i = 0; i < columnNames.size(); i++) {
            String colName = columnNames.get(i);
            String val = i < row.size() ? row.get(i) : "";
            HBox fieldBox = new HBox(8);
            Label colLabel = new Label(colName + ":");
            colLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 120px;");
            Label valLabel = new Label(val == null ? "NULL" : val);
            valLabel.setWrapText(true);
            fieldBox.getChildren().addAll(colLabel, valLabel);
            content.getChildren().add(fieldBox);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showAggregateSummary() {
        var selected = getSelectionModel().getSelectedCells();
        if (selected.isEmpty()) return;
        int count = selected.size();
        double sum = 0;
        int numCount = 0;
        for (TablePosition<?, ?> pos : selected) {
            String val = getCellValue(pos);
            try {
                double d = Double.parseDouble(val.trim());
                sum += d;
                numCount++;
            } catch (Exception ignored) {}
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Aggregate Summary");
        alert.setHeaderText("Selected Cells (" + count + ")");
        String msg = "Count: " + count + "\n";
        if (numCount > 0) {
            msg += "Numeric Count: " + numCount + "\nSum: " + sum + "\nAverage: " + (sum / numCount);
        } else {
            msg += "(No numeric values in selection)";
        }
        alert.setContentText(msg);
        DialogTheme.apply(alert);
        alert.showAndWait();
    }

    private void copyAggregationResult() {
        var selected = getSelectionModel().getSelectedCells();
        if (selected.isEmpty()) return;
        double sum = 0;
        int numCount = 0;
        for (TablePosition<?, ?> pos : selected) {
            String val = getCellValue(pos);
            try {
                double d = Double.parseDouble(val.trim());
                sum += d;
                numCount++;
            } catch (Exception ignored) {}
        }
        put(numCount > 0 ? String.valueOf(sum) : String.valueOf(selected.size()));
    }

    private void revertSelectedCells() {
        var selected = getSelectionModel().getSelectedCells();
        for (TablePosition<?, ?> pos : selected) {
            int r = pos.getRow();
            int c = getColumns().indexOf(pos.getTableColumn()) - 1;
            modifiedCells.remove(new CellKey(r, c));
        }
        for (int r : getSelectedRowIndexes()) {
            if (isRowDeleted(r)) {
                unmarkRowDeleted(r);
                if (revertRowAction != null) {
                    revertRowAction.accept(r);
                }
            }
        }
        Platform.runLater(this::refresh);
    }

    private void compareSelectedCells() {
        var selected = getSelectionModel().getSelectedCells();
        if (selected.size() < 2) return;
        TablePosition<?, ?> p1 = selected.get(0);
        TablePosition<?, ?> p2 = selected.get(1);

        String v1 = getCellValue(p1);
        String v2 = getCellValue(p2);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Compare Cells");
        alert.setHeaderText("Comparison of 2 selected cells");
        String content = "Cell 1 (" + (p1.getRow() + 1) + ", " + p1.getTableColumn().getText() + "):\n" + v1 + "\n\n"
                + "Cell 2 (" + (p2.getRow() + 1) + ", " + p2.getTableColumn().getText() + "):\n" + v2 + "\n\n"
                + (v1.equals(v2) ? "✓ Values are IDENTICAL" : "✗ Values are DIFFERENT (length " + v1.length() + " vs " + v2.length() + ")");
        alert.setContentText(content);
        DialogTheme.apply(alert);
        alert.showAndWait();
    }

    private void showQuickDocumentation() {
        TablePosition<?, ?> pos = getFocusModel().getFocusedCell();
        if (pos == null || pos.getTableColumn() == null) return;
        int colIdx = getColumns().indexOf(pos.getTableColumn()) - 1;
        if (colIdx < 0 || colIdx >= columnNames.size()) return;
        String colName = columnNames.get(colIdx);
        String colType = colIdx < columnTypes.size() ? columnTypes.get(colIdx) : "UNKNOWN";
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Column Info");
        alert.setHeaderText("Column: " + colName);
        alert.setContentText("Type: " + colType + "\nIndex: " + (colIdx + 1));
        DialogTheme.apply(alert);
        alert.showAndWait();
    }

    private void filterBySelectedValue(boolean equals) {
        TablePosition<?, ?> pos = getFocusModel().getFocusedCell();
        if (pos == null || pos.getRow() < 0 || pos.getTableColumn() == null) {
            var selected = getSelectionModel().getSelectedCells();
            if (!selected.isEmpty()) pos = selected.get(0);
        }
        if (pos == null || pos.getRow() < 0 || pos.getTableColumn() == null) return;
        int rowIdx = pos.getRow();
        int colIdx = getColumns().indexOf(pos.getTableColumn()) - 1;
        if (colIdx < 0 || rowIdx >= getItems().size()) return;
        String val = getCellValue(pos);
        String colName = colIdx < columnNames.size() ? columnNames.get(colIdx) : null;
        if (colName != null && sortRequestListener != null) {
            sortRequestListener.onSortRequested(colName, equals ? "FILTER_EQ:" + val : "FILTER_NEQ:" + val);
        }
    }

    private void exportTableToClipboard() {
        if (columnNames == null || getItems() == null || getItems().isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", columnNames)).append("\n");
        for (List<String> row : getItems()) {
            List<String> escaped = new ArrayList<>();
            for (String val : row) {
                if (val == null) escaped.add("");
                else if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                    escaped.add("\"" + val.replace("\"", "\"\"") + "\"");
                } else {
                    escaped.add(val);
                }
            }
            sb.append(String.join(",", escaped)).append("\n");
        }
        put(sb.toString());
    }

    private String getCellValue(TablePosition<?, ?> pos) {
        int r = pos.getRow();
        int c = getColumns().indexOf(pos.getTableColumn()) - 1;
        if (r >= 0 && r < getItems().size() && c >= 0) {
            List<String> row = getItems().get(r);
            return c < row.size() && row.get(c) != null ? row.get(c) : "NULL";
        }
        return "NULL";
    }

    private boolean isInsideActiveEditor(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur instanceof EditCell editCell && editCell.isEditing()) {
                return true;
            }
            if (cur.getStyleClass().contains("cell-editor")
                    || cur.getStyleClass().contains("cell-more-button")
                    || cur.getStyleClass().contains("grid-datetime-picker")
                    || cur.getStyleClass().contains("grid-boolean-picker")
                    || cur.getStyleClass().contains("boolean-picker-item")) {
                return true;
            }
            cur = cur.getParent();
        }
        return false;
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

    public Runnable getDeleteRowsAction() {
        return deleteRowsAction;
    }


    public Runnable getAddRowAction() {
        return addRowAction;
    }

    public void setReloadAction(Runnable action) {
        this.reloadAction = action;
    }

    public Runnable getReloadAction() {
        return reloadAction;
    }

    /** Selected row indexes, descending (safe order for sequential removal). */
    public List<Integer> getSelectedRowIndexes() {
        Set<Integer> distinct = new HashSet<>();
        for (TablePosition<?, ?> pos : getSelectionModel().getSelectedCells()) {
            if (pos.getRow() >= 0 && pos.getRow() < getItems().size()) {
                distinct.add(pos.getRow());
            }
        }
        List<Integer> rows = new ArrayList<>(distinct);
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
        modifiedCells.clear();
        deletedRowIndices.clear();
        anchorRowIndex = -1;
        anchorColumn = null;
        lastRowHeaderIndex = -1;
        if (result == null || !result.isResultSet()) return;

        applySettings();

        TableColumn<List<String>, Void> serialCol = new TableColumn<>("#");
        serialCol.setSortable(false);
        serialCol.setPrefWidth(56);
        serialCol.getStyleClass().add("serial-column");
        serialCol.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("serial-cell");
                setOnMousePressed(e -> {
                    if (e.getButton() == MouseButton.PRIMARY && getIndex() >= 0 && getIndex() < getItems().size()) {
                        handleSerialClick(getIndex(), e.isShiftDown(), e.isControlDown() || e.isShortcutDown());
                        e.consume();
                    }
                });
                setOnMouseReleased(e -> {
                    if (e.getButton() == MouseButton.PRIMARY) e.consume();
                });
                setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY) e.consume();
                });
                setOnContextMenuRequested(e -> {
                    if (getIndex() >= 0 && getIndex() < getItems().size()) {
                        if (!isRowSelected(getIndex())) {
                            selectFullRow(getIndex(), false);
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                boolean isDeleted = !empty && isRowDeleted(getIndex());
                pseudoClassStateChanged(DELETED_PSEUDO_CLASS, isDeleted);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (isDeleted) {
                    Text strikeText = new Text(String.valueOf(getIndex() + 1 + rowNumberOffset));
                    strikeText.setStrikethrough(true);
                    strikeText.getStyleClass().add("deleted-cell-text");
                    setGraphic(strikeText);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(String.valueOf(getIndex() + 1 + rowNumberOffset));
                }
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
                col.setCellFactory(c -> new EditCell(index));
                col.setOnEditCommit(event -> {
                    int primaryRow = event.getTablePosition().getRow();
                    String newValue = event.getNewValue();
                    commitValueAcrossSelection(primaryRow, col, index, newValue);
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
     * Column sort control: a name label plus a small icon that always
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
        if (index < columnTypes.size()) {
            String type = columnTypes.get(index).toLowerCase(Locale.ROOT);
            if (type.contains("date") || type.contains("timestamp") || type.contains("time")) {
                return true;
            }
        }
        if (getItems() != null) {
            for (List<String> row : getItems()) {
                if (row != null && index < row.size()) {
                    String val = row.get(index);
                    if (val != null && !val.isBlank() && !val.equalsIgnoreCase("NULL")) {
                        if (val.matches("^\\d{4}-\\d{2}-\\d{2}.*") || val.matches("^\\d{1,2}:\\d{2}:\\d{2}.*")) {
                            return true;
                        }
                        break;
                    }
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------ edit cell

    /**
     * Inline cell editor:
     * - Boolean columns get a quick-choice popup (true, false, null, default) matching Image 3.
     * - Date/time/timestamp columns get a 2-row modification popup with Day, Month,
     *   Year and Hour, Minute, Second spinners with 2-way real-time binding and a "..." button.
     * - Commits across all selected rows in this column (Images 4 & 5).
     * - Commits automatically when clicking outside the editor cell, or when pressing Enter.
     * - Escape cancels the edit and restores the original cell content.
     */
    private class EditCell extends TableCell<List<String>, String> {

        private final int columnIndex;
        private TextField field;
        private HBox editor;
        private Button pickerButton;
        private GridDateTimePicker dateTimePicker;
        private GridBooleanPicker booleanPicker;
        private boolean isUpdatingFromPicker = false;
        private boolean isCommitting = false;
        private boolean explicitEscape = false;

        EditCell(int columnIndex) {
            this.columnIndex = columnIndex;
        }

        @Override
        public void startEdit() {
            if (isEmpty() || !getTableView().isEditable() || isRowDeleted(getIndex())) return;
            super.startEdit();
            boolean isDate = isDateColumn(columnIndex);
            boolean isBool = isBooleanColumn(columnIndex);

            if (isBool) {
                if (booleanPicker == null) {
                    booleanPicker = new GridBooleanPicker(chosenValue -> {
                        @SuppressWarnings("unchecked")
                        TableColumn<List<String>, String> col = (TableColumn<List<String>, String>) getTableColumn();
                        commitValueAcrossSelection(getIndex(), col, columnIndex, chosenValue);
                        commitEdit(chosenValue);
                    });
                    booleanPicker.setOnPopupClosed(() -> {
                        if (isEditing()) {
                            cancelEdit();
                        }
                    });
                }
                Platform.runLater(() -> {
                    if (isEditing()) {
                        booleanPicker.show(this, getItem());
                    }
                });
                return;
            }

            if (editor == null) {
                buildEditor(isDate);
            } else {
                updateEditorForColumnType(isDate);
            }
            String initialText = getItem() == null ? "" : getItem();
            field.setText(initialText);
            setText(null);
            setGraphic(editor);
            field.requestFocus();
            field.selectAll();

            if (isDate && dateTimePicker != null) {
                dateTimePicker.parseAndUpdate(initialText);
                Platform.runLater(() -> {
                    if (isEditing()) {
                        dateTimePicker.show(editor);
                    }
                });
            }
        }

        private void buildEditor(boolean isDate) {
            field = new TextField();
            field.getStyleClass().add("cell-editor");
            field.setOnAction(e -> commitCurrentValue());
            field.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    explicitEscape = true;
                    try {
                        cancelEdit();
                    } finally {
                        explicitEscape = false;
                    }
                } else if (e.getCode() == KeyCode.ENTER) {
                    commitCurrentValue();
                }
            });

            // Commit when focus leaves the cell editor without pressing Enter —
            // saves the cell when mouse-clicking outside.
            field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused && !explicitEscape && isEditing()) {
                    if (isEditorFocused()) {
                        return;
                    }
                    commitCurrentValue();
                }
            });

            HBox.setHgrow(field, javafx.scene.layout.Priority.ALWAYS);
            editor = new HBox(2, field);
            editor.setAlignment(Pos.CENTER_LEFT);

            pickerButton = new Button("...");
            pickerButton.getStyleClass().add("cell-more-button");
            pickerButton.setFocusTraversable(false);
            pickerButton.setOnAction(e -> {
                if (dateTimePicker != null) {
                    if (dateTimePicker.isShowing()) {
                        dateTimePicker.hide();
                    } else {
                        dateTimePicker.parseAndUpdate(field.getText());
                        dateTimePicker.show(editor);
                    }
                }
            });

            dateTimePicker = new GridDateTimePicker(formattedVal -> {
                isUpdatingFromPicker = true;
                try {
                    field.setText(formattedVal);
                } finally {
                    isUpdatingFromPicker = false;
                }
            });

            dateTimePicker.setOnCommitRequested(v -> commitCurrentValue());
            dateTimePicker.setOnCancelRequested(v -> {
                explicitEscape = true;
                try {
                    cancelEdit();
                } finally {
                    explicitEscape = false;
                }
            });

            dateTimePicker.setOnPopupClosed(() -> {
                if (isEditing() && !explicitEscape) {
                    Platform.runLater(() -> {
                        if (isEditing() && !explicitEscape && !isEditorFocused()) {
                            commitCurrentValue();
                        }
                    });
                }
            });

            field.textProperty().addListener((obs, oldV, newV) -> {
                if (!isUpdatingFromPicker && dateTimePicker != null && dateTimePicker.isShowing()) {
                    dateTimePicker.parseAndUpdate(newV);
                }
            });

            if (isDate) {
                editor.getChildren().add(pickerButton);
            }
        }

        private void updateEditorForColumnType(boolean isDate) {
            if (isDate) {
                if (!editor.getChildren().contains(pickerButton)) {
                    editor.getChildren().add(pickerButton);
                }
            } else {
                editor.getChildren().remove(pickerButton);
                if (dateTimePicker != null) {
                    dateTimePicker.hide();
                }
            }
        }

        private boolean isEditorFocused() {
            if (field != null && field.isFocused()) return true;
            if (pickerButton != null && pickerButton.isFocused()) return true;
            if (editor != null && editor.isFocused()) return true;
            if (dateTimePicker != null && (dateTimePicker.isShowing() || dateTimePicker.isFocused())) return true;
            if (booleanPicker != null && booleanPicker.isShowing()) return true;
            return false;
        }

        private void commitCurrentValue() {
            if (isCommitting || !isEditing()) return;
            if (getIndex() < 0 || getTableView() == null || getIndex() >= getTableView().getItems().size()) return;
            isCommitting = true;
            try {
                if (dateTimePicker != null) {
                    dateTimePicker.hide();
                }
                if (booleanPicker != null) {
                    booleanPicker.hide();
                }
                String val = field != null ? field.getText() : getItem();
                @SuppressWarnings("unchecked")
                TableColumn<List<String>, String> col = (TableColumn<List<String>, String>) getTableColumn();
                commitValueAcrossSelection(getIndex(), col, columnIndex, val);
                commitEdit(val);
            } finally {
                isCommitting = false;
            }
        }

        @Override
        public void cancelEdit() {
            if (!explicitEscape && (field != null || booleanPicker != null) && !isCommitting && isEditing()) {
                commitCurrentValue();
                return;
            }
            if (dateTimePicker != null) {
                dateTimePicker.hide();
            }
            if (booleanPicker != null) {
                booleanPicker.hide();
            }
            super.cancelEdit();
            setGraphic(null);
            setText(getItem());
        }

        @Override
        public void commitEdit(String newValue) {
            if (dateTimePicker != null) {
                dateTimePicker.hide();
            }
            if (booleanPicker != null) {
                booleanPicker.hide();
            }
            super.commitEdit(newValue);
            setGraphic(null);
            setText(newValue);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            boolean isDeleted = !empty && isRowDeleted(getIndex());
            pseudoClassStateChanged(DELETED_PSEUDO_CLASS, isDeleted);
            pseudoClassStateChanged(MODIFIED_PSEUDO_CLASS, !empty && !isDeleted && isCellModified(getIndex(), columnIndex));
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isDeleted) {
                Text strikeText = new Text(item == null ? "NULL" : item);
                strikeText.setStrikethrough(true);
                strikeText.getStyleClass().add("deleted-cell-text");
                setGraphic(strikeText);
                setText(null);
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
                        @SuppressWarnings("unchecked")
                        TableColumn<List<String>, String> col = (TableColumn<List<String>, String>) getTableColumn();
                        commitValueAcrossSelection(getIndex(), col, columnIndex, newVal);
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
            boolean isDeleted = !empty && isRowDeleted(getIndex());
            pseudoClassStateChanged(DELETED_PSEUDO_CLASS, isDeleted);
            pseudoClassStateChanged(MODIFIED_PSEUDO_CLASS, !empty && !isDeleted && isCellModified(getIndex(), columnIndex));
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isDeleted) {
                Text strikeText = new Text(item == null ? "NULL" : item);
                strikeText.setStrikethrough(true);
                strikeText.getStyleClass().add("deleted-cell-text");
                setGraphic(strikeText);
                setText(null);
            } else if (item == null) {
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
        if (columnTypes != null && index < columnTypes.size()) {
            String type = columnTypes.get(index).toLowerCase(Locale.ROOT);
            if (type.contains("bool") || type.contains("bit") || type.equals("tinyint(1)")) {
                return true;
            }
        }
        if (columnNames != null && index < columnNames.size()) {
            String name = columnNames.get(index).toLowerCase(Locale.ROOT);
            if (name.equals("deleted") || name.startsWith("is_") || name.startsWith("has_")
                    || name.equals("active") || name.equals("enabled") || name.contains("boolean")) {
                return true;
            }
        }
        if (getItems() != null && !getItems().isEmpty()) {
            boolean hasNonEmpty = false;
            int count = 0;
            for (List<String> row : getItems()) {
                if (row != null && index < row.size()) {
                    String val = row.get(index);
                    if (val != null && !val.isBlank() && !val.equalsIgnoreCase("NULL")) {
                        String lower = val.trim().toLowerCase(Locale.ROOT);
                        if (!lower.equals("true") && !lower.equals("false") && !lower.equals("t") && !lower.equals("f")) {
                            return false;
                        }
                        hasNonEmpty = true;
                    }
                }
                if (++count > 25) break;
            }
            if (hasNonEmpty) return true;
        }
        return false;
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

    public void copySelection() {
        var selected = getSelectionModel().getSelectedCells();
        if (selected.isEmpty()) {
            copyFocusedCell();
            return;
        }
        if (selected.size() == 1) {
            TablePosition<?, ?> pos = selected.get(0);
            put(getCellValue(pos));
            return;
        }
        Map<Integer, List<TablePosition<?, ?>>> byRow = new TreeMap<>();
        for (TablePosition<?, ?> pos : selected) {
            byRow.computeIfAbsent(pos.getRow(), k -> new ArrayList<>()).add(pos);
        }
        StringBuilder sb = new StringBuilder();
        for (var entry : byRow.entrySet()) {
            List<TablePosition<?, ?>> cells = entry.getValue();
            cells.sort((a, b) -> Integer.compare(getColumns().indexOf(a.getTableColumn()), getColumns().indexOf(b.getTableColumn())));
            List<String> vals = new ArrayList<>();
            for (TablePosition<?, ?> pos : cells) {
                vals.add(getCellValue(pos));
            }
            if (sb.length() > 0) sb.append("\n");
            sb.append(String.join("\t", vals));
        }
        put(sb.toString());
    }

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

package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.SqlValueBinder;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.scene.control.Button;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Shared editing controller for a ResultGrid bound to one table:
 * collects pending cell UPDATEs and row DELETEs, then writes them all in a
 * single transaction on Submit (rollback on any failure). Used by the table
 * data view and by editable SELECT results in the query console.
 *
 * Values are bound as typed PreparedStatement parameters (via SqlValueBinder)
 * rather than pasted into SQL text, so dates, booleans, numerics, binary data
 * and quoting-hostile strings all round-trip correctly.
 */
public class GridEditManager {

    private record Update(Map<String, String> pkValues, String column, String newValue) {}

    /** Sentinel JDBC type meaning "this is PostgreSQL's ctid row id, cast as ::tid". */
    private static final int CTID_PSEUDO_TYPE = Integer.MIN_VALUE;

    private final ConnectionProfile profile;
    private final String catalog;
    private final ResultGrid grid;
    private final Button submitButton;
    private final Button revertButton;
    private final Runnable reloader;
    private final Consumer<String> status;

    private String qualifiedTable;
    private List<String> pkColumns = List.of();
    private Map<String, Integer> columnTypes = Map.of();
    private QueryResult currentResult;
    private boolean editable;

    private final List<Update> updates = new ArrayList<>();
    private final List<Map<String, String>> deletes = new ArrayList<>();

    public GridEditManager(ConnectionProfile profile, String catalog, ResultGrid grid,
                           Button submitButton, Button revertButton,
                           Runnable reloader, Consumer<String> status) {
        this.profile = profile;
        this.catalog = catalog;
        this.grid = grid;
        this.submitButton = submitButton;
        this.revertButton = revertButton;
        this.reloader = reloader;
        this.status = status;

        grid.enableEditing(this::onCellEdited);
        grid.setDeleteRowsAction(this::markSelectedRowsDeleted);
        submitButton.setOnAction(e -> submit());
        revertButton.setOnAction(e -> {
            clearPending();
            reloader.run();
        });
        updateButtons();
    }

    /**
     * Binds the manager to the table behind the current result. Editing is on
     * only when the table has a primary key and every pk column is present in
     * the result. Call BEFORE grid.showResult(result).
     *
     * @param columnTypes JDBC type code (java.sql.Types) per column name, used
     *                    to bind edits as typed parameters; pass an empty map
     *                    to fall back to string binding for every column.
     */
    public void configure(String qualifiedTable, List<String> pkColumns,
                          Map<String, Integer> columnTypes, QueryResult result) {
        this.qualifiedTable = qualifiedTable;
        this.pkColumns = pkColumns == null ? List.of() : pkColumns;
        this.columnTypes = columnTypes == null ? Map.of() : columnTypes;
        this.currentResult = result;
        clearPending();

        editable = qualifiedTable != null && !this.pkColumns.isEmpty() && result != null
                && this.pkColumns.stream()
                    .allMatch(pk -> indexOfColumn(result.getColumns(), pk) >= 0);
        grid.setEditable(editable);
    }

    /** Convenience overload when no column type map is available. */
    public void configure(String qualifiedTable, List<String> pkColumns, QueryResult result) {
        configure(qualifiedTable, pkColumns, Map.of(), result);
    }

    /** Read-only mode (e.g. aggregate query results). Call BEFORE showResult. */
    public void configureReadOnly(QueryResult result) {
        configure(null, List.of(), Map.of(), result);
    }

    public boolean isEditable() { return editable; }
    public boolean hasPending() { return !updates.isEmpty() || !deletes.isEmpty(); }

    // ------------------------------------------------------------- editing

    private void onCellEdited(int rowIndex, int columnIndex, String oldValue, String newValue) {
        if (!editable || currentResult == null) return;
        List<String> row = grid.getItems().get(rowIndex);
        Map<String, String> pkValues = pkSnapshot(row, columnIndex, oldValue);
        if (pkValues == null) return;
        updates.add(new Update(pkValues, currentResult.getColumns().get(columnIndex), newValue));
        updateButtons();
    }

    /** Marks every row touched by the current selection as deleted (pending). */
    public void markSelectedRowsDeleted() {
        if (!editable) {
            status.accept("Rows cannot be deleted: table has no usable primary key");
            return;
        }
        List<Integer> rowIndexes = grid.getSelectedRowIndexes();
        if (rowIndexes.isEmpty()) return;

        for (int rowIndex : rowIndexes) {                    // descending order
            List<String> row = grid.getItems().get(rowIndex);
            Map<String, String> pkValues = pkSnapshot(row, -1, null);
            if (pkValues == null) return;
            deletes.add(pkValues);
            grid.getItems().remove(rowIndex);
        }
        updateButtons();
    }

    /** Primary-key values identifying a row; pre-edit value used for the edited column. */
    private Map<String, String> pkSnapshot(List<String> row, int editedColumn, String oldValue) {
        Map<String, String> pkValues = new LinkedHashMap<>();
        for (String pk : pkColumns) {
            int pkIndex = indexOfColumn(currentResult.getColumns(), pk);
            if (pkIndex < 0) {
                status.accept("Cannot edit: primary key column " + pk + " missing from result");
                return null;
            }
            pkValues.put(pk, pkIndex == editedColumn ? oldValue : row.get(pkIndex));
        }
        return pkValues;
    }

    private static int indexOfColumn(List<String> columns, String name) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private void clearPending() {
        updates.clear();
        deletes.clear();
        updateButtons();
    }

    private void updateButtons() {
        boolean has = hasPending();
        submitButton.setDisable(!has);
        revertButton.setDisable(!has);
        int total = updates.size() + deletes.size();
        submitButton.setText(has ? "Submit (" + total + ")" : "Submit");
        if (has) {
            status.accept(updates.size() + " update(s), " + deletes.size()
                    + " delete(s) pending — press Submit to save");
        }
    }

    // -------------------------------------------------------------- submit

    private void submit() {
        if (!hasPending()) return;
        List<Update> updatesToApply = new ArrayList<>(updates);
        List<Map<String, String>> deletesToApply = new ArrayList<>(deletes);
        submitButton.setDisable(true);
        status.accept("Submitting…");

        AppExecutor.run(() -> {
            try (Connection conn = ClientRegistry.jdbc(profile, catalog).getConnection()) {
                conn.setAutoCommit(false);
                try {
                    for (Update update : updatesToApply) {
                        try (PreparedStatement stmt = conn.prepareStatement(buildUpdateSql(update))) {
                            int index = 1;
                            index = bindValue(stmt, index, update.column(), update.newValue());
                            bindWhereClause(stmt, index, update.pkValues());
                            stmt.executeUpdate();
                        }
                    }
                    for (Map<String, String> pkValues : deletesToApply) {
                        try (PreparedStatement stmt = conn.prepareStatement(buildDeleteSql(pkValues))) {
                            bindWhereClause(stmt, 1, pkValues);
                            stmt.executeUpdate();
                        }
                    }
                    conn.commit();
                } catch (Exception inner) {
                    conn.rollback();
                    throw inner;
                }
                Platform.runLater(() -> {
                    clearPending();
                    status.accept("✓ " + updatesToApply.size() + " update(s), "
                            + deletesToApply.size() + " delete(s) committed");
                    reloader.run();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    status.accept("Commit failed (rolled back): " + msg);
                    submitButton.setDisable(false);
                });
            }
        });
    }

    // ------------------------------------------------------ SQL + binding

    private String buildUpdateSql(Update update) {
        return "UPDATE " + qualifiedTable
                + " SET " + DbObject.quote(update.column()) + " = ?"
                + " WHERE " + whereSql(update.pkValues());
    }

    private String buildDeleteSql(Map<String, String> pkValues) {
        return "DELETE FROM " + qualifiedTable + " WHERE " + whereSql(pkValues);
    }

    private String whereSql(Map<String, String> pkValues) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String column : pkValues.keySet()) {
            if (!first) sb.append(" AND ");
            sb.append(DbObject.quote(column));
            sb.append(typeOf(column) == CTID_PSEUDO_TYPE ? " = ?::tid" : " = ?");
            first = false;
        }
        return sb.toString();
    }

    /** Binds the SET value for an UPDATE; returns the next free parameter index. */
    private int bindValue(PreparedStatement stmt, int index, String column, String value)
            throws java.sql.SQLException {
        Integer type = columnTypes.get(column);
        SqlValueBinder.bind(stmt, index, type, value);
        return index + 1;
    }

    /** Binds every pk parameter of a WHERE clause built by {@link #whereSql}. */
    private void bindWhereClause(PreparedStatement stmt, int startIndex, Map<String, String> pkValues)
            throws java.sql.SQLException {
        int index = startIndex;
        for (var pk : pkValues.entrySet()) {
            int type = typeOf(pk.getKey());
            if (type == CTID_PSEUDO_TYPE) {
                stmt.setString(index, pk.getValue());   // cast to ::tid is in the SQL text
            } else {
                SqlValueBinder.bind(stmt, index, type == 0 ? null : type, pk.getValue());
            }
            index++;
        }
    }

    private int typeOf(String column) {
        if (column.equalsIgnoreCase("ctid") && !columnTypes.containsKey(column)) {
            return CTID_PSEUDO_TYPE;
        }
        return columnTypes.getOrDefault(column, 0);
    }
}

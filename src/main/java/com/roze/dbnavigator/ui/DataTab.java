package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * DataGrip-style table data view: breadcrumb showing exactly which
 * connection ▸ database ▸ schema ▸ table is open, paged grid with WHERE/ORDER BY,
 * inline editing (incl. date picker), multi-row delete, transactional Submit.
 * Uses the same {@link ResultPager} component as the query console for a
 * consistent look, with row numbers reflecting true absolute position.
 */
public class DataTab extends Tab {

    private static final int PAGE_SIZE = 500;

    private final ConnectionProfile profile;
    private final DbObject table;

    /** Used by TabContextMenu's "Copy Path/Reference…". */
    public String getQualifiedTableReference() {
        return profile.getName() + "/" + table.qualifiedName();
    }

    /** Used by MainWindow's Split Right/Down to reopen the same table in a new pane. */
    public ConnectionProfile getProfileForReopen() { return profile; }
    public DbObject getTableForReopen() { return table; }
    public ConnectionProfile getProfile() { return profile; }
    public DbObject getTable() { return table; }
    private final ResultGrid grid = new ResultGrid();
    private final TextField filterField = new TextField();
    private final TextField orderField = new TextField();
    private final Label whereLabel = new Label("WHERE");
    private final Label orderLabel = new Label("ORDER BY");
    private final Popup clauseCompletionPopup = new Popup();
    private final ListView<String> clauseCompletionList = new ListView<>();
    private final ResultPager pager = new ResultPager();
    private final Label statusLabel = new Label("Loading…");
    private final Button submitButton = new Button("Submit");
    private final Button revertButton = new Button("Revert");
    private final GridEditManager editManager;

    private final List<String> pkColumns = new ArrayList<>();
    private Map<String, Integer> columnTypes = Map.of();
    private boolean useCtid = false;
    private int page = 0;
    private long totalRows = -1;
    private boolean totalRowsExact = false;

    public DataTab(ConnectionProfile profile, DbObject table) {
        this.profile = profile;
        this.table = table;

        if (profile != null && profile.getType() == ConnectionProfile.DatabaseType.SQLSERVER
                && table.getKind() == DbObject.Kind.VIRTUAL_VIEW
                && "sessions".equalsIgnoreCase(table.getName())
                && (table.getQualifiedNameOverride() == null || table.getQualifiedNameOverride().isBlank())) {
            table.setQualifiedNameOverride(
                "(SELECT spid AS SPID, rtrim(status) AS Status, rtrim(loginame) AS Login, " +
                "CASE WHEN rtrim(hostname) = '' THEN '.' ELSE rtrim(hostname) END AS HostName, " +
                "CASE WHEN blocked = 0 THEN '.' ELSE cast(blocked as varchar(10)) END AS BlkBy, " +
                "db_name(dbid) AS DBName, rtrim(cmd) AS Command " +
                "FROM master.dbo.sysprocesses) AS sessions"
            );
        } else if (profile != null && profile.getType() == ConnectionProfile.DatabaseType.ORACLE
                && table.getKind() == DbObject.Kind.VIRTUAL_VIEW
                && "sessions".equalsIgnoreCase(table.getName())
                && (table.getQualifiedNameOverride() == null || table.getQualifiedNameOverride().isBlank())) {
            table.setQualifiedNameOverride("v$session");
        }

        if (table.getCatalog() == null && table.getKind() == DbObject.Kind.VIRTUAL_VIEW) {
            setText(table.getName() + " [" + profile.getName() + "]");
        } else {
            setText(table.getName());
        }
        setGraphic(table.getKind() == DbObject.Kind.VIRTUAL_VIEW
                ? Icons.of(FontAwesomeSolid.TABLE, "#c77dbb", 11)
                : Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 11));
        setTooltip(new Tooltip(breadcrumbText()));

        submitButton.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
        submitButton.setTooltip(new Tooltip("Write pending edits/deletes to the database"));
        revertButton.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#e05555", 11));
        revertButton.setTooltip(new Tooltip("Discard pending edits and reload"));

        editManager = new GridEditManager(profile, table.getCatalog(), grid,
                submitButton, revertButton, this::loadPage, statusLabel::setText);

        filterField.setPromptText("condition…  (e.g. status = 'active')");
        filterField.setPrefWidth(210);
        filterField.setOnAction(e -> handleReloadFromStart());

        orderField.setPromptText("column…  (e.g. id DESC)");
        orderField.setPrefWidth(160);
        orderField.setOnAction(e -> handleReloadFromStart());

        setupClauseControls();

        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> handleReloadFromStart());

        Button refreshButton = new Button();
        refreshButton.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#6897bb", 11));
        refreshButton.setTooltip(new Tooltip("Reload Page  Ctrl+F5"));
        refreshButton.setOnAction(e -> handleReload());

        Button exportButton = new Button();
        exportButton.setGraphic(Icons.of(FontAwesomeSolid.FILE_CSV, "#e0a44c", 11));
        exportButton.setTooltip(new Tooltip("Export current page to CSV"));
        exportButton.setOnAction(e -> grid.exportCsv());

        pager.setOnFirst(() -> {
            if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) return;
            if (page != 0) { page = 0; loadPage(); }
        });
        pager.setOnPrev(() -> {
            if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) return;
            if (page > 0) { page--; loadPage(); }
        });
        pager.setOnNext(() -> {
            if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) return;
            page++; loadPage();
        });
        pager.setOnLast(() -> {
            if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) return;
            jumpToLastPage();
        });
        pager.addOverflowItem("Get exact row count", this::fetchExactCount);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox whereControl = new HBox(5, whereLabel, filterField);
        whereControl.getStyleClass().add("data-clause-control");
        whereControl.setAlignment(Pos.CENTER_LEFT);
        HBox orderControl = new HBox(5, orderLabel, orderField);
        orderControl.getStyleClass().add("data-clause-control");
        orderControl.setAlignment(Pos.CENTER_LEFT);

        Button addRowBtn = new Button();
        addRowBtn.setGraphic(Icons.of(FontAwesomeSolid.PLUS, "#57965c", 11));
        addRowBtn.setTooltip(new Tooltip("Add Row"));
        addRowBtn.disableProperty().bind(grid.editableProperty().not());
        addRowBtn.setOnAction(e -> {
            if (grid.getAddRowAction() != null) grid.getAddRowAction().run();
        });

        Button deleteRowsBtn = new Button();
        deleteRowsBtn.setGraphic(Icons.of(FontAwesomeSolid.MINUS, "#e05555", 11));
        deleteRowsBtn.setTooltip(new Tooltip("Delete Selected Row(s)"));
        deleteRowsBtn.disableProperty().bind(grid.editableProperty().not()
                .or(javafx.beans.binding.Bindings.isEmpty(grid.getSelectionModel().getSelectedCells())));
        deleteRowsBtn.setOnAction(e -> {
            if (grid.getDeleteRowsAction() != null) grid.getDeleteRowsAction().run();
        });

        HBox toolbar = new HBox(8, addRowBtn, deleteRowsBtn, submitButton, revertButton, refreshButton,
                whereControl, orderControl, applyButton, exportButton, spacer, pager);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        // Breadcrumb bar: connection ▸ database ▸ schema ▸ table
        Label crumb = new Label(breadcrumbText());
        crumb.getStyleClass().add("breadcrumb");
        crumb.setGraphic(Icons.of(FontAwesomeSolid.MAP_MARKER_ALT, "#868a91", 10));
        HBox crumbBar = new HBox(crumb);
        crumbBar.setPadding(new Insets(4, 10, 4, 10));
        crumbBar.getStyleClass().add("breadcrumb-bar");

        statusLabel.getStyleClass().add("console-status");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.getStyleClass().add("console-status-bar");

        VBox root = new VBox(crumbBar, toolbar, grid, statusBar);
        VBox.setVgrow(grid, Priority.ALWAYS);
        setContent(root);

        grid.setReloadAction(this::handleReload);
        grid.setSortRequestListener((columnName, direction) -> {
            if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) return;
            orderField.setText(direction == null ? "" : DbObject.quote(columnName) + " " + direction);
            grid.setCurrentSort(columnName, direction);
            reloadFromStart();
        });

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if ((e.isControlDown() && e.getCode() == KeyCode.F5) || e.getCode() == KeyCode.F5) {
                handleReload();
                e.consume();
            }
        });

        if (AppSettingsStore.load().isEnableLocalFilterByDefault()) {
            Platform.runLater(filterField::requestFocus);
        }

        detectPrimaryKeyThenLoad();
    }

    /** DataGrip-like WHERE / ORDER BY controls with local table-column completion. */
    private void setupClauseControls() {
        whereLabel.getStyleClass().add("data-clause-label");
        orderLabel.getStyleClass().add("data-clause-label");
        filterField.getStyleClass().add("data-clause-field");
        orderField.getStyleClass().add("data-clause-field");

        clauseCompletionList.getStyleClass().add("completion-list");
        clauseCompletionList.setPrefSize(300, 180);
        clauseCompletionPopup.getContent().add(clauseCompletionList);
        clauseCompletionPopup.setAutoHide(true);
        clauseCompletionList.setOnMouseClicked(e -> acceptClauseCompletion());
        clauseCompletionList.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER, TAB -> { acceptClauseCompletion(); e.consume(); }
                case ESCAPE -> { clauseCompletionPopup.hide(); e.consume(); }
                default -> {}
            }
        });

        configureClauseField(filterField, whereLabel, false);
        configureClauseField(orderField, orderLabel, true);
    }

    private void configureClauseField(TextField field, Label label, boolean orderBy) {
        field.textProperty().addListener((obs, oldText, newText) -> {
            label.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("clause-active"),
                    !newText.isBlank());
            if (field.isFocused()) showClauseCompletions(field, orderBy);
        });
        field.focusedProperty().addListener((obs, oldFocus, focused) -> {
            if (focused) showClauseCompletions(field, orderBy);
            else if (!clauseCompletionList.isFocused()) clauseCompletionPopup.hide();
        });
        field.setOnKeyPressed(e -> {
            if (!clauseCompletionPopup.isShowing()) return;
            switch (e.getCode()) {
                case DOWN -> { clauseCompletionList.getSelectionModel().selectNext(); e.consume(); }
                case UP -> { clauseCompletionList.getSelectionModel().selectPrevious(); e.consume(); }
                case ENTER, TAB -> { acceptClauseCompletion(); e.consume(); }
                case ESCAPE -> { clauseCompletionPopup.hide(); e.consume(); }
                default -> { }
            }
        });
    }

    private void showClauseCompletions(TextField field, boolean orderBy) {
        String token = trailingIdentifier(field.getText(), field.getCaretPosition());
        if (token.isBlank() || columnTypes.isEmpty()) {
            clauseCompletionPopup.hide();
            return;
        }
        String prefix = token.toLowerCase(java.util.Locale.ROOT);
        Stream<String> columns = columnTypes.keySet().stream()
                .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(prefix));
        List<String> matches = Stream.concat(columns, orderBy
                        ? Stream.of("ASC", "DESC").filter(word -> word.toLowerCase(java.util.Locale.ROOT).startsWith(prefix))
                        : Stream.empty())
                .sorted(Comparator.comparing(String::toLowerCase))
                .limit(20)
                .toList();
        if (matches.isEmpty() || (matches.size() == 1 && matches.getFirst().equalsIgnoreCase(token))) {
            clauseCompletionPopup.hide();
            return;
        }
        clauseCompletionList.getItems().setAll(matches);
        clauseCompletionList.getSelectionModel().selectFirst();
        var bounds = field.localToScreen(field.getBoundsInLocal());
        if (bounds != null) clauseCompletionPopup.show(field, bounds.getMinX(), bounds.getMaxY() + 2);
    }

    private void acceptClauseCompletion() {
        String selected = clauseCompletionList.getSelectionModel().getSelectedItem();
        if (selected == null || !(clauseCompletionPopup.getOwnerNode() instanceof TextField field)) return;
        int end = field.getCaretPosition();
        int start = end;
        while (start > 0 && isIdentifierPart(field.getText().charAt(start - 1))) start--;
        field.replaceText(start, end, selected);
        field.positionCaret(start + selected.length());
        clauseCompletionPopup.hide();
        field.requestFocus();
    }

    private static String trailingIdentifier(String text, int caret) {
        int start = Math.min(caret, text.length());
        while (start > 0 && isIdentifierPart(text.charAt(start - 1))) start--;
        return text.substring(start, Math.min(caret, text.length()));
    }

    private static boolean isIdentifierPart(char character) {
        return Character.isLetterOrDigit(character) || character == '_' || character == '.';
    }

    private String breadcrumbText() {
        StringBuilder sb = new StringBuilder(profile.getName());
        if (table.getCatalog() != null && !table.getCatalog().isBlank()) {
            sb.append("  ▸  ").append(table.getCatalog());
        }
        if (table.getKind() == DbObject.Kind.VIRTUAL_VIEW) {
            sb.append("  ▸  virtual views");
        } else if (table.getSchema() != null && !table.getSchema().isBlank()) {
            sb.append("  ▸  ").append(table.getSchema());
        }
        sb.append("  ▸  ").append(table.getName());
        return sb.toString();
    }

    private void detectPrimaryKeyThenLoad() {
        AppExecutor.run(() -> {
            try {
                pkColumns.addAll(MetadataService.loadPrimaryKeys(profile, table));
            } catch (Exception ignored) {}
            try {
                columnTypes = MetadataService.loadColumnTypes(profile, table);
            } catch (Exception ignored) {}
            // PostgreSQL tables without a PK (typically partitions) are still
            // editable through their physical row identity. Both values are
            // needed because ctid can repeat in different child partitions.
            // Views and virtual views do NOT have physical row identity (tableoid/ctid).
            if (pkColumns.isEmpty()
                    && profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL
                    && (table.getKind() == DbObject.Kind.TABLE || table.getKind() == DbObject.Kind.PARTITION)) {
                useCtid = true;
                pkColumns.add("tableoid");
                pkColumns.add("ctid");
            }
            AppSettingsStore.Settings s = AppSettingsStore.load();
            if (s.isSortTablesByNumericPk() && !pkColumns.isEmpty() && orderField.getText().isBlank()) {
                String firstPk = pkColumns.get(0);
                Integer type = columnTypes.get(firstPk);
                if (type != null && (type == java.sql.Types.INTEGER || type == java.sql.Types.BIGINT
                        || type == java.sql.Types.SMALLINT || type == java.sql.Types.TINYINT
                        || type == java.sql.Types.NUMERIC || type == java.sql.Types.DECIMAL)) {
                    String dir = "Descending".equalsIgnoreCase(s.getSortTablesByNumericPkDirection()) ? "DESC" : "ASC";
                    Platform.runLater(() -> orderField.setText(DbObject.quote(firstPk) + " " + dir));
                }
            }
            Platform.runLater(this::loadPage);
        });
    }

    private int getEffectivePageSize() {
        AppSettingsStore.Settings s = AppSettingsStore.load();
        if (!s.isLimitPageSize()) {
            return 50000;
        }
        return s.getPageSize() > 0 ? s.getPageSize() : 500;
    }

    private void reloadFromStart() {
        page = 0;
        totalRows = -1;
        totalRowsExact = false;
        loadPage();
    }

    /** Runs a real COUNT(*) in the background and refreshes the pager. */
    private void fetchExactCount() {
        String where = filterField.getText().trim();
        statusLabel.setText("Counting rows\u2026");
        AppExecutor.run(() -> {
            try {
                long exact = ClientRegistry.jdbc(profile, table.getCatalog())
                        .countRows(table.qualifiedName(), where);
                Platform.runLater(() -> {
                    totalRows = exact;
                    totalRowsExact = true;
                    refreshPager();
                    statusLabel.setText("Exact count: " + exact + " row(s)");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Could not count rows: " + ex.getMessage()));
            }
        });
    }

    /** Jumps straight to the last page if the exact total is known, otherwise counts first. */
    private void jumpToLastPage() {
        int pageSize = getEffectivePageSize();
        if (totalRowsExact) {
            page = totalRows == 0 ? 0 : (int) ((totalRows - 1) / pageSize);
            loadPage();
        } else {
            String where = filterField.getText().trim();
            statusLabel.setText("Counting rows\u2026");
            AppExecutor.run(() -> {
                try {
                    long exact = ClientRegistry.jdbc(profile, table.getCatalog())
                            .countRows(table.qualifiedName(), where);
                    Platform.runLater(() -> {
                        totalRows = exact;
                        totalRowsExact = true;
                        page = exact == 0 ? 0 : (int) ((exact - 1) / pageSize);
                        loadPage();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> statusLabel.setText("Could not count rows: " + ex.getMessage()));
                }
            });
        }
    }

    private void refreshPager() {
        int pageSize = getEffectivePageSize();
        long shown = grid.getItems().size();
        long from = shown == 0 ? 0 : (long) page * pageSize + 1;
        long to = shown == 0 ? 0 : from + shown - 1;
        pager.update(from, to, totalRows, totalRowsExact);
    }

    private void loadPage() {
        statusLabel.setText("Loading…");
        String where = filterField.getText().trim();
        String order = orderField.getText().trim();
        int currentPage = page;
        int pageSize = getEffectivePageSize();

        AppExecutor.run(() -> {
            try {
                var client = ClientRegistry.jdbc(profile, table.getCatalog());
                QueryResult result;
                try {
                    result = client.fetchTablePage(
                            table.qualifiedName(), currentPage * pageSize, pageSize,
                            where, order, useCtid);
                } catch (Exception ex) {
                    if (useCtid) {
                        useCtid = false;
                        pkColumns.remove("tableoid");
                        pkColumns.remove("ctid");
                        result = client.fetchTablePage(
                                table.qualifiedName(), currentPage * pageSize, pageSize,
                                where, order, false);
                    } else {
                        throw ex;
                    }
                }
                if (totalRows < 0) {
                    if (where.isBlank() && profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL
                            && (table.getKind() == DbObject.Kind.TABLE || table.getKind() == DbObject.Kind.PARTITION)) {
                        long estimate = MetadataService.estimateRowCount(profile, table);
                        if (estimate >= 0) {
                            totalRows = estimate;
                            totalRowsExact = false;
                        } else {
                            try {
                                totalRows = client.countRows(table.qualifiedName(), where);
                                totalRowsExact = true;
                            } catch (Exception ignore) {
                                totalRows = -1;
                            }
                        }
                    } else {
                        try {
                            totalRows = client.countRows(table.qualifiedName(), where);
                            totalRowsExact = true;
                        } catch (Exception ignore) {
                            totalRows = -1;
                        }
                    }
                }
                final QueryResult finalResult = result;
                Platform.runLater(() -> {
                    editManager.configure(table.qualifiedName(), pkColumns, columnTypes, finalResult);
                    grid.setRowNumberOffset(currentPage * PAGE_SIZE);
                    grid.showResult(finalResult);

                    // Fewer rows than a full page means we've now directly observed the
                    // true end, regardless of what an earlier estimate suggested.
                    if (finalResult.getRows().size() < PAGE_SIZE) {
                        totalRows = (long) currentPage * PAGE_SIZE + finalResult.getRows().size();
                        totalRowsExact = true;
                    }
                    refreshPager();

                    statusLabel.setText(finalResult.getRows().size() + " row(s) in "
                            + finalResult.getExecutionMillis() + " ms"
                            + (editManager.isEditable()
                                ? (useCtid
                                    ? "  ·  editable via row id (no primary key)"
                                    : "  ·  double-click a cell to edit, select rows + Delete to remove")
                                : (table.getKind() == DbObject.Kind.VIRTUAL_VIEW
                                    ? "  ·  virtual view (read-only)"
                                    : "  ·  read-only (no primary key)")));
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> statusLabel.setText("Error: " + msg));
            }
        });
    }

    public void handleReload() {
        if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) {
            return;
        }
        loadPage();
    }

    public void handleReloadFromStart() {
        if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) {
            return;
        }
        reloadFromStart();
    }

    private Window getOwnerWindow() {
        if (getTabPane() != null && getTabPane().getScene() != null) {
            return getTabPane().getScene().getWindow();
        }
        return null;
    }
}

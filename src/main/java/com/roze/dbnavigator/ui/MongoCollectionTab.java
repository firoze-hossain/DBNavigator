package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * MongoDB collection viewer: JSON filter, pagination, flattened document
 * grid, real column sorting, and editable cells with a Submit/Revert
 * pattern — same shape as the SQL data view, via {@link MongoGridEditManager}.
 */
public class MongoCollectionTab extends Tab {

    private static final int PAGE_SIZE = 200;

    private final ConnectionProfile profile;
    private final DbObject collection;

    /** Used by MainWindow's Split Right/Down and Reopen Closed Tab. */
    public ConnectionProfile getProfileForReopen() { return profile; }
    public DbObject getCollectionForReopen() { return collection; }
    private final ResultGrid grid = new ResultGrid();
    private final MongoGridEditManager editManager;
    private final TextField filterField = new TextField();
    private final Label pageLabel = new Label();
    private final Label statusLabel = new Label("Loading…");
    private final Button prevButton = new Button();
    private final Button nextButton = new Button();
    private final Button submitButton = new Button("Submit");
    private final Button revertButton = new Button("Revert");

    private int page = 0;
    private long totalDocs = -1;
    private String sortField;
    private String sortDirection;   // "ASC", "DESC", or null

    public MongoCollectionTab(ConnectionProfile profile, DbObject collection) {
        this.profile = profile;
        this.collection = collection;

        setText(collection.getCatalog() + "." + collection.getName());
        setGraphic(Icons.of(FontAwesomeSolid.LEAF, "#57965c", 11));

        grid.setSortRequestListener((columnName, direction) -> {
            sortField = direction == null ? null : columnName;
            sortDirection = direction;
            grid.setCurrentSort(columnName, direction);
            reloadFromStart();
        });

        submitButton.getStyleClass().add("run-button");
        submitButton.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#ffffff", 11));
        revertButton.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#a9b7c6", 11));
        editManager = new MongoGridEditManager(profile, grid,
                submitButton, revertButton, this::loadPage, statusLabel::setText);

        filterField.setPromptText("Filter JSON, e.g. {\"status\": \"active\", \"age\": {\"$gt\": 21}}");
        HBox.setHgrow(filterField, Priority.ALWAYS);
        filterField.setOnAction(e -> reloadFromStart());

        Button applyButton = new Button("Find");
        applyButton.setGraphic(Icons.of(FontAwesomeSolid.SEARCH, "#6897bb", 11));
        applyButton.setOnAction(e -> reloadFromStart());

        MenuButton exportButton = new MenuButton();
        exportButton.setGraphic(Icons.of(FontAwesomeSolid.DOWNLOAD, "#e0a44c", 11));
        exportButton.setTooltip(new Tooltip("Export current page"));
        MenuItem exportCsv = new MenuItem("Export to CSV\u2026");
        exportCsv.setOnAction(e -> grid.exportCsv());
        MenuItem exportJson = new MenuItem("Export to JSON\u2026");
        exportJson.setOnAction(e -> grid.exportJson());
        exportButton.getItems().addAll(exportCsv, exportJson);

        prevButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_LEFT, "#a9b7c6", 11));
        prevButton.setOnAction(e -> { if (page > 0) { page--; loadPage(); } });
        nextButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_RIGHT, "#a9b7c6", 11));
        nextButton.setOnAction(e -> { page++; loadPage(); });

        Region spacer = new Region();
        HBox toolbar = new HBox(8, submitButton, revertButton, new Separator(),
                filterField, applyButton, exportButton,
                spacer, prevButton, pageLabel, nextButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        statusLabel.getStyleClass().add("console-status");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.getStyleClass().add("console-status-bar");

        VBox root = new VBox(toolbar, grid, statusBar);
        VBox.setVgrow(grid, Priority.ALWAYS);
        setContent(root);

        loadPage();
    }

    private void reloadFromStart() {
        page = 0;
        totalDocs = -1;
        loadPage();
    }

    private void loadPage() {
        statusLabel.setText("Loading…");
        String filter = filterField.getText().trim();
        int currentPage = page;

        AppExecutor.run(() -> {
            try {
                var client = ClientRegistry.mongo(profile);
                QueryResult result = client.find(collection.getCatalog(), collection.getName(),
                        filter, sortField, "DESC".equals(sortDirection), currentPage * PAGE_SIZE, PAGE_SIZE);
                if (totalDocs < 0) {
                    try {
                        totalDocs = client.countDocuments(
                                collection.getCatalog(), collection.getName(), filter);
                    } catch (Exception ignore) {
                        totalDocs = -1;
                    }
                }
                Platform.runLater(() -> {
                    editManager.configure(collection.getCatalog(), collection.getName(), result);
                    grid.showResult(result);
                    long from = (long) currentPage * PAGE_SIZE + 1;
                    long to = from + result.getRows().size() - 1;
                    pageLabel.setText(result.getRows().isEmpty()
                            ? "0 docs"
                            : from + "–" + to + (totalDocs >= 0 ? " of " + totalDocs : ""));
                    statusLabel.setText(result.getRows().size() + " document(s) in "
                            + result.getExecutionMillis() + " ms");
                    prevButton.setDisable(currentPage == 0);
                    nextButton.setDisable(result.getRows().size() < PAGE_SIZE);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> statusLabel.setText("Error: " + msg));
            }
        });
    }
}

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

/** MongoDB collection viewer: JSON filter, pagination, flattened document grid. */
public class MongoCollectionTab extends Tab {

    private static final int PAGE_SIZE = 200;

    private final ConnectionProfile profile;
    private final DbObject collection;
    private final ResultGrid grid = new ResultGrid();
    private final TextField filterField = new TextField();
    private final Label pageLabel = new Label();
    private final Label statusLabel = new Label("Loading…");
    private final Button prevButton = new Button();
    private final Button nextButton = new Button();

    private int page = 0;
    private long totalDocs = -1;

    public MongoCollectionTab(ConnectionProfile profile, DbObject collection) {
        this.profile = profile;
        this.collection = collection;

        setText(collection.getCatalog() + "." + collection.getName());
        setGraphic(Icons.of(FontAwesomeSolid.LEAF, "#57965c", 11));

        filterField.setPromptText("Filter JSON, e.g. {\"status\": \"active\", \"age\": {\"$gt\": 21}}");
        HBox.setHgrow(filterField, Priority.ALWAYS);
        filterField.setOnAction(e -> reloadFromStart());

        Button applyButton = new Button("Find");
        applyButton.setGraphic(Icons.of(FontAwesomeSolid.SEARCH, "#6897bb", 11));
        applyButton.setOnAction(e -> reloadFromStart());

        Button exportButton = new Button();
        exportButton.setGraphic(Icons.of(FontAwesomeSolid.FILE_CSV, "#e0a44c", 11));
        exportButton.setTooltip(new Tooltip("Export current page to CSV"));
        exportButton.setOnAction(e -> grid.exportCsv());

        prevButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_LEFT, "#a9b7c6", 11));
        prevButton.setOnAction(e -> { if (page > 0) { page--; loadPage(); } });
        nextButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_RIGHT, "#a9b7c6", 11));
        nextButton.setOnAction(e -> { page++; loadPage(); });

        Region spacer = new Region();
        HBox toolbar = new HBox(8, filterField, applyButton, exportButton,
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
                        filter, currentPage * PAGE_SIZE, PAGE_SIZE);
                if (totalDocs < 0) {
                    try {
                        totalDocs = client.countDocuments(
                                collection.getCatalog(), collection.getName(), filter);
                    } catch (Exception ignore) {
                        totalDocs = -1;
                    }
                }
                Platform.runLater(() -> {
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

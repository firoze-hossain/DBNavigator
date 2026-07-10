package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.ConnectionStore;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DataGrip-style "Search Everywhere": type to find tables/views across every
 * connected data source; Enter or double-click opens the table's data.
 */
public class SearchDialog {

    private record Hit(ConnectionProfile profile, DbObject table) {
        String display() {
            StringBuilder sb = new StringBuilder(table.getName()).append("   ");
            if (table.getCatalog() != null) sb.append(table.getCatalog()).append('.');
            if (table.getSchema() != null) sb.append(table.getSchema()).append('.');
            sb.append(table.getName()).append("  [").append(profile.getName()).append(']');
            return sb.toString();
        }
    }

    private final Stage stage = new Stage();
    private final TextField searchField = new TextField();
    private final ListView<Hit> resultList = new ListView<>();
    private final Label statusLabel = new Label("Type a table name — searches all connected data sources");
    private final MainWindow mainWindow;
    private final AtomicInteger generation = new AtomicInteger();

    public SearchDialog(Window owner, MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Search Everywhere");

        searchField.setPromptText("Search tables and views everywhere…");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox top = new HBox(8, Icons.of(FontAwesomeSolid.SEARCH, "#868a91", 13), searchField);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10));

        resultList.getStyleClass().add("completion-list");
        resultList.setPrefHeight(320);
        resultList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Hit hit, boolean empty) {
                super.updateItem(hit, empty);
                if (empty || hit == null) { setGraphic(null); setText(null); return; }
                Label name = new Label(hit.table().getName());
                name.getStyleClass().addAll("completion-name", "completion-table");
                String location = (hit.table().getCatalog() != null ? hit.table().getCatalog() + "." : "")
                        + (hit.table().getSchema() != null ? hit.table().getSchema() + "." : "")
                        + hit.table().getName() + "  [" + hit.profile().getName() + "]";
                Label detail = new Label(location);
                detail.getStyleClass().add("completion-detail");
                Region gap = new Region();
                HBox.setHgrow(gap, Priority.ALWAYS);
                HBox box = new HBox(10, name, gap, detail);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        statusLabel.getStyleClass().add("console-status");
        HBox bottom = new HBox(statusLabel);
        bottom.setPadding(new Insets(6, 10, 8, 10));

        VBox root = new VBox(top, resultList, bottom);
        root.getStyleClass().add("search-dialog");
        VBox.setVgrow(resultList, Priority.ALWAYS);

        Scene scene = new Scene(root, 620, 400);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);

        // ---- behavior ----
        searchField.textProperty().addListener((obs, old, text) -> search(text));
        searchField.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> { resultList.getSelectionModel().selectNext(); e.consume(); }
                case UP -> { resultList.getSelectionModel().selectPrevious(); e.consume(); }
                case ENTER -> { openSelected(); e.consume(); }
                case ESCAPE -> stage.close();
                default -> {}
            }
        });
        resultList.setOnMouseClicked(e -> { if (e.getClickCount() == 2) openSelected(); });
        resultList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) openSelected();
            else if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });
        // close when focus leaves (click outside)
        stage.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) stage.close();
        });
    }

    public void show() {
        stage.show();
        searchField.requestFocus();
    }

    private void openSelected() {
        Hit hit = resultList.getSelectionModel().getSelectedItem();
        if (hit == null) hit = resultList.getItems().isEmpty() ? null : resultList.getItems().get(0);
        if (hit == null) return;
        stage.close();
        mainWindow.openDataTab(hit.profile(), hit.table());
    }

    private void search(String query) {
        if (query == null || query.strip().length() < 2) {
            resultList.getItems().clear();
            statusLabel.setText("Type at least 2 characters");
            return;
        }
        String q = query.strip();
        int gen = generation.incrementAndGet();
        statusLabel.setText("Searching…");

        AppExecutor.run(() -> {
            List<Hit> hits = new ArrayList<>();
            for (ConnectionProfile profile : ConnectionStore.load()) {
                if (generation.get() != gen) return;         // superseded by newer keystroke
                if (profile.getType() == DatabaseType.MONGODB) continue;
                if (!ClientRegistry.isConnected(profile)) continue;

                // Which databases to search: default + any explicitly visible ones (PG)
                Set<String> catalogs = new LinkedHashSet<>();
                catalogs.add(null);                            // default connection
                if (profile.getType() == DatabaseType.POSTGRESQL) {
                    catalogs.addAll(profile.getVisibleDatabases());
                }
                for (String catalog : catalogs) {
                    for (DbObject table : MetadataService.searchTables(profile, catalog, q, 30)) {
                        hits.add(new Hit(profile, table));
                        if (hits.size() >= 60) break;
                    }
                    if (hits.size() >= 60) break;
                }
            }
            if (generation.get() != gen) return;
            Platform.runLater(() -> {
                resultList.getItems().setAll(hits);
                if (!hits.isEmpty()) resultList.getSelectionModel().selectFirst();
                statusLabel.setText(hits.isEmpty()
                        ? "No matches in connected data sources (connect/expand a data source first)"
                        : hits.size() + " match(es) — Enter opens the table");
            });
        });
    }
}

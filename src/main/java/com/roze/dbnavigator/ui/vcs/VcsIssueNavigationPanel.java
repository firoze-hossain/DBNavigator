package com.roze.dbnavigator.ui.vcs;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Settings panel for Version Control > Issue Navigation,
 * matching DataGrip screenshots media_1791679091821_8922323a.png and media_1791679101365_2f105b45.png.
 */
public class VcsIssueNavigationPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public VcsIssueNavigationPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.VcsIssueNavigationConfig cfg = settings.getVcsIssueNavigation();

        VBox content = new VBox(10);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Top description label
        Label descLabel = new Label("DataGrip will search for the specified patterns in check-in comments and link them to issues in your issue tracker");
        descLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px; -fx-wrap-text: true;");

        // Working list of patterns
        ObservableList<AppSettingsStore.IssueNavigationPattern> patternList = FXCollections.observableArrayList();
        if (cfg.getPatterns() != null) {
            for (AppSettingsStore.IssueNavigationPattern p : cfg.getPatterns()) {
                patternList.add(p.copy());
            }
        }
        inputs.put("vcs_issueNavigationPatterns", patternList);

        // Action Toolbar
        Button addBtn = new Button("+");
        addBtn.setTooltip(new Tooltip("Add Issue Navigation Pattern"));
        addBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-font-weight: bold; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 28px; -fx-cursor: hand;");

        ContextMenu addMenu = new ContextMenu();
        MenuItem youtrackItem = new MenuItem("Add YouTrack Pattern");
        MenuItem jiraItem = new MenuItem("Add JIRA Pattern");
        MenuItem customItem = new MenuItem("Add Issue Navigation Link");
        addMenu.getItems().addAll(youtrackItem, jiraItem, customItem);

        addBtn.setOnAction(e -> {
            addMenu.show(addBtn, Side.BOTTOM, 0, 0);
        });

        Button editBtn = new Button("✎");
        editBtn.setTooltip(new Tooltip("Edit Pattern"));
        editBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-font-weight: bold; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 28px; -fx-cursor: hand;");
        editBtn.setDisable(true);

        Button removeBtn = new Button("-");
        removeBtn.setTooltip(new Tooltip("Remove Pattern"));
        removeBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-font-weight: bold; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 28px; -fx-cursor: hand;");
        removeBtn.setDisable(true);

        HBox toolbar = new HBox(4, addBtn, editBtn, removeBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Table
        TableView<AppSettingsStore.IssueNavigationPattern> table = new TableView<>(patternList);
        table.setPrefHeight(260);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label placeholder = new Label("No patterns configured");
        placeholder.setStyle("-fx-text-fill: #868a91; -fx-font-size: 13px;");
        table.setPlaceholder(placeholder);

        TableColumn<AppSettingsStore.IssueNavigationPattern, String> issueCol = new TableColumn<>("Issue");
        issueCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIssue()));
        issueCol.setPrefWidth(220);

        TableColumn<AppSettingsStore.IssueNavigationPattern, String> linkCol = new TableColumn<>("Link");
        linkCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLink()));
        linkCol.setPrefWidth(320);

        table.getColumns().addAll(issueCol, linkCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            boolean hasSel = selected != null;
            editBtn.setDisable(!hasSel);
            removeBtn.setDisable(!hasSel);
        });

        // Add menu handlers
        youtrackItem.setOnAction(e -> {
            Window owner = table.getScene() != null ? table.getScene().getWindow() : null;
            IssueNavigationPatternDialog dlg = new IssueNavigationPatternDialog(owner, "YouTrack", null);
            AppSettingsStore.IssueNavigationPattern p = dlg.showAndWait();
            if (p != null) {
                patternList.add(p);
                table.getSelectionModel().select(p);
            }
        });

        jiraItem.setOnAction(e -> {
            Window owner = table.getScene() != null ? table.getScene().getWindow() : null;
            IssueNavigationPatternDialog dlg = new IssueNavigationPatternDialog(owner, "JIRA", null);
            AppSettingsStore.IssueNavigationPattern p = dlg.showAndWait();
            if (p != null) {
                patternList.add(p);
                table.getSelectionModel().select(p);
            }
        });

        customItem.setOnAction(e -> {
            Window owner = table.getScene() != null ? table.getScene().getWindow() : null;
            IssueNavigationPatternDialog dlg = new IssueNavigationPatternDialog(owner, "Issue Navigation Link", null);
            AppSettingsStore.IssueNavigationPattern p = dlg.showAndWait();
            if (p != null) {
                patternList.add(p);
                table.getSelectionModel().select(p);
            }
        });

        // Edit handler
        Runnable editAction = () -> {
            AppSettingsStore.IssueNavigationPattern selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Window owner = table.getScene() != null ? table.getScene().getWindow() : null;
            IssueNavigationPatternDialog dlg = new IssueNavigationPatternDialog(owner, selected.getTrackerType(), selected);
            AppSettingsStore.IssueNavigationPattern p = dlg.showAndWait();
            if (p != null) {
                int idx = patternList.indexOf(selected);
                if (idx >= 0) {
                    patternList.set(idx, p);
                    table.getSelectionModel().select(idx);
                }
            }
        };

        editBtn.setOnAction(e -> editAction.run());

        table.setRowFactory(tv -> {
            TableRow<AppSettingsStore.IssueNavigationPattern> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editAction.run();
                }
            });
            return row;
        });

        removeBtn.setOnAction(e -> {
            AppSettingsStore.IssueNavigationPattern selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                patternList.remove(selected);
            }
        });

        content.getChildren().addAll(descLabel, toolbar, table);
        return content;
    }
}

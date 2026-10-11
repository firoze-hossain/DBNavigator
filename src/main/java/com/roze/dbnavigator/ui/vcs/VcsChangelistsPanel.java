package com.roze.dbnavigator.ui.vcs;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.SettingsDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Settings panel for Version Control > Changelists,
 * matching DataGrip screenshot media_1791678903680_f5fdba47.png.
 */
public class VcsChangelistsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public VcsChangelistsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.VcsChangelistsConfig cfg = settings.getVcsChangelists();

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Top checkboxes
        CheckBox createAutoCheck = new CheckBox("Create changelists automatically");
        createAutoCheck.setSelected(cfg.isCreateChangelistsAutomatically());
        createAutoCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_createChangelistsAutomatically", createAutoCheck);

        CheckBox allowMultipleCheck = new CheckBox("Allow putting changes within one file into different changelists");
        allowMultipleCheck.setSelected(cfg.isAllowPuttingChangesInDifferentChangelists());
        allowMultipleCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_allowPuttingChangesInDifferentChangelists", allowMultipleCheck);

        // Section: Inactive Changelist
        HBox inactiveSection = SettingsDialog.buildTitledSectionLine("Inactive Changelist");

        CheckBox highlightInactiveCheck = new CheckBox("Highlight files from inactive changelists");
        highlightInactiveCheck.setSelected(cfg.isHighlightFilesFromInactiveChangelists());
        highlightInactiveCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_highlightFilesFromInactiveChangelists", highlightInactiveCheck);

        CheckBox showDialogInactiveCheck = new CheckBox("Show dialog on attempt to edit file from inactive changelist");
        showDialogInactiveCheck.setSelected(cfg.isShowDialogOnEditFromInactiveChangelist());
        showDialogInactiveCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_showDialogOnEditFromInactiveChangelist", showDialogInactiveCheck);

        Label emptyActionLabel = new Label("When an empty changelist becomes inactive:");
        emptyActionLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> emptyActionCombo = new ComboBox<>();
        emptyActionCombo.getItems().addAll("Show options", "Do nothing", "Remove silently");
        emptyActionCombo.setValue(cfg.getWhenEmptyChangelistBecomesInactive());
        emptyActionCombo.setPrefWidth(180);
        emptyActionCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("vcs_whenEmptyChangelistBecomesInactive", emptyActionCombo);

        HBox emptyActionRow = new HBox(8, emptyActionLabel, emptyActionCombo);
        emptyActionRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Conflicts
        HBox conflictsSection = SettingsDialog.buildTitledSectionLine("Conflicts");

        CheckBox highlightConflictsCheck = new CheckBox("Highlight files with changelist conflicts");
        highlightConflictsCheck.setSelected(cfg.isHighlightFilesWithChangelistConflicts());
        highlightConflictsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_highlightFilesWithChangelistConflicts", highlightConflictsCheck);

        Label ignoredConflictsLabel = new Label("Files with ignored conflicts:");
        ignoredConflictsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        // Ignored files list / placeholder area
        ObservableList<String> ignoredFiles = FXCollections.observableArrayList(
                cfg.getIgnoredConflictFiles() != null ? cfg.getIgnoredConflictFiles() : new ArrayList<>()
        );
        inputs.put("vcs_ignoredConflictFiles", ignoredFiles);

        ListView<String> listView = new ListView<>(ignoredFiles);
        listView.setPrefHeight(120);
        listView.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label placeholder = new Label("No ignored files");
        placeholder.setStyle("-fx-text-fill: #868a91; -fx-font-size: 13px;");
        listView.setPlaceholder(placeholder);

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14; -fx-cursor: hand;");
        clearBtn.setDisable(ignoredFiles.isEmpty());

        clearBtn.setOnAction(e -> {
            ignoredFiles.clear();
            clearBtn.setDisable(true);
        });

        ignoredFiles.addListener((javafx.collections.ListChangeListener<String>) c -> {
            clearBtn.setDisable(ignoredFiles.isEmpty());
        });

        HBox clearRow = new HBox(clearBtn);
        clearRow.setAlignment(Pos.CENTER_RIGHT);
        clearRow.setPadding(new Insets(4, 0, 0, 0));

        content.getChildren().addAll(
                createAutoCheck,
                allowMultipleCheck,
                inactiveSection,
                highlightInactiveCheck,
                showDialogInactiveCheck,
                emptyActionRow,
                conflictsSection,
                highlightConflictsCheck,
                ignoredConflictsLabel,
                listView,
                clearRow
        );

        return content;
    }
}

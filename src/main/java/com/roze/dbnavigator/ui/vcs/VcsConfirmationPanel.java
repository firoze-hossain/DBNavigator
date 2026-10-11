package com.roze.dbnavigator.ui.vcs;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.SettingsDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Settings panel for Version Control > Confirmation,
 * matching DataGrip screenshot media_1791678941278_3d931368.png.
 */
public class VcsConfirmationPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public VcsConfirmationPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.VcsConfirmationConfig cfg = settings.getVcsConfirmation();

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Section: Confirmation
        HBox confirmSection = SettingsDialog.buildTitledSectionLine("Confirmation");

        Label createdLabel = new Label("When files are created:");
        createdLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> createdCombo = new ComboBox<>();
        createdCombo.getItems().addAll("Ask", "Add silently", "Do not add");
        createdCombo.setValue(cfg.getWhenFilesCreated());
        createdCombo.setPrefWidth(160);
        createdCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("vcs_whenFilesCreated", createdCombo);

        HBox createdRow = new HBox(8, createdLabel, createdCombo);
        createdRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox applyOutsideCheck = new CheckBox("Apply to files created outside DataGrip");
        applyOutsideCheck.setSelected(cfg.isApplyToFilesCreatedOutside());
        applyOutsideCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        applyOutsideCheck.setPadding(new Insets(0, 0, 0, 24));
        applyOutsideCheck.setDisable("Do not add".equals(createdCombo.getValue()));
        createdCombo.valueProperty().addListener((obs, oldV, newV) -> {
            applyOutsideCheck.setDisable("Do not add".equals(newV));
        });
        inputs.put("vcs_applyToFilesCreatedOutside", applyOutsideCheck);

        Label deletedLabel = new Label("When files are deleted:");
        deletedLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> deletedCombo = new ComboBox<>();
        deletedCombo.getItems().addAll("Ask", "Remove silently", "Do not remove");
        deletedCombo.setValue(cfg.getWhenFilesDeleted());
        deletedCombo.setPrefWidth(160);
        deletedCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("vcs_whenFilesDeleted", deletedCombo);

        HBox deletedRow = new HBox(8, deletedLabel, deletedCombo);
        deletedRow.setAlignment(Pos.CENTER_LEFT);

        Label showOptionsLabel = new Label("Show options before:");
        showOptionsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        CheckBox updateCheck = new CheckBox("Update");
        updateCheck.setSelected(cfg.isShowOptionsBeforeUpdate());
        updateCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_showOptionsBeforeUpdate", updateCheck);

        HBox updateRow = new HBox(8, showOptionsLabel, updateCheck);
        updateRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox dropCommitsCheck = new CheckBox("Ask for confirmation to drop commits");
        dropCommitsCheck.setSelected(cfg.isAskConfirmationToDropCommits());
        dropCommitsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_askConfirmationToDropCommits", dropCommitsCheck);

        Label dropCommitsHint = new Label("Applicable to Git");
        dropCommitsHint.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-padding: 0 0 0 24;");

        // Section: Changes
        HBox changesSection = SettingsDialog.buildTitledSectionLine("Changes");

        CheckBox serverConflictsCheck = new CheckBox("Check for conflicts with the server every");
        serverConflictsCheck.setSelected(cfg.isCheckConflictsWithServer());
        serverConflictsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_checkConflictsWithServer", serverConflictsCheck);

        Spinner<Integer> serverConflictsSpinner = new Spinner<>(1, 1440, cfg.getCheckConflictsMinutes(), 5);
        serverConflictsSpinner.setEditable(true);
        serverConflictsSpinner.setPrefWidth(80);
        serverConflictsSpinner.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        serverConflictsSpinner.disableProperty().bind(serverConflictsCheck.selectedProperty().not());
        inputs.put("vcs_checkConflictsMinutes", serverConflictsSpinner);

        Label minutesLabel = new Label("minutes");
        minutesLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        HBox serverConflictsRow = new HBox(6, serverConflictsCheck, serverConflictsSpinner, minutesLabel);
        serverConflictsRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox changedDaysCheck = new CheckBox("Highlight files changed in the last");
        changedDaysCheck.setSelected(cfg.isHighlightFilesChangedInLastDays());
        changedDaysCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_highlightFilesChangedInLastDays", changedDaysCheck);

        Spinner<Integer> changedDaysSpinner = new Spinner<>(1, 365, cfg.getHighlightFilesDays(), 1);
        changedDaysSpinner.setEditable(true);
        changedDaysSpinner.setPrefWidth(70);
        changedDaysSpinner.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        changedDaysSpinner.disableProperty().bind(changedDaysCheck.selectedProperty().not());
        inputs.put("vcs_highlightFilesDays", changedDaysSpinner);

        Label daysLabel = new Label("days");
        daysLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        HBox changedDaysRow = new HBox(6, changedDaysCheck, changedDaysSpinner, daysLabel);
        changedDaysRow.setAlignment(Pos.CENTER_LEFT);

        Label changedDaysHint = new Label("Modified files will be highlighted in the external stack trace and while debugging.");
        changedDaysHint.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-padding: 0 0 0 24;");

        CheckBox highlightDirsCheck = new CheckBox("Highlight directories that contain modified files in the Project tree");
        highlightDirsCheck.setSelected(cfg.isHighlightDirectoriesWithModifiedFiles());
        highlightDirsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_highlightDirectoriesWithModifiedFiles", highlightDirsCheck);

        Label patchLabel = new Label("When patch is created:");
        patchLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> patchCombo = new ComboBox<>();
        patchCombo.getItems().addAll("Ask", "Open in editor", "Do not open");
        patchCombo.setValue(cfg.getWhenPatchCreated());
        patchCombo.setPrefWidth(160);
        patchCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("vcs_whenPatchCreated", patchCombo);

        HBox patchRow = new HBox(8, patchLabel, patchCombo);
        patchRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox restoreWorkspaceCheck = new CheckBox("Restore workspace when switching branches");
        restoreWorkspaceCheck.setSelected(cfg.isRestoreWorkspaceWhenSwitchingBranches());
        restoreWorkspaceCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_restoreWorkspaceWhenSwitchingBranches", restoreWorkspaceCheck);

        Label restoreWorkspaceHint = new Label("A workspace is a set of opened files, the current run configuration and breakpoints associated with a branch.");
        restoreWorkspaceHint.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-padding: 0 0 0 24;");

        CheckBox limitHistoryCheck = new CheckBox("Limit history to");
        limitHistoryCheck.setSelected(cfg.isLimitHistoryRows());
        limitHistoryCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_limitHistoryRows", limitHistoryCheck);

        Spinner<Integer> limitHistorySpinner = new Spinner<>(10, 50000, cfg.getMaxHistoryRows(), 100);
        limitHistorySpinner.setEditable(true);
        limitHistorySpinner.setPrefWidth(90);
        limitHistorySpinner.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        limitHistorySpinner.disableProperty().bind(limitHistoryCheck.selectedProperty().not());
        inputs.put("vcs_maxHistoryRows", limitHistorySpinner);

        Label rowsLabel = new Label("rows");
        rowsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        HBox limitHistoryRow = new HBox(6, limitHistoryCheck, limitHistorySpinner, rowsLabel);
        limitHistoryRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Gutter
        HBox gutterSection = SettingsDialog.buildTitledSectionLine("Gutter");

        CheckBox gutterCheck = new CheckBox("Highlight modified lines in the gutter");
        gutterCheck.setSelected(cfg.isHighlightModifiedLinesInGutter());
        gutterCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_highlightModifiedLinesInGutter", gutterCheck);

        CheckBox errorStripeCheck = new CheckBox("Highlight modified lines in error stripe on the scrollbar");
        errorStripeCheck.setSelected(cfg.isHighlightModifiedLinesInErrorStripe());
        errorStripeCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        errorStripeCheck.setPadding(new Insets(0, 0, 0, 24));
        errorStripeCheck.disableProperty().bind(gutterCheck.selectedProperty().not());
        inputs.put("vcs_highlightModifiedLinesInErrorStripe", errorStripeCheck);

        CheckBox whitespaceCheck = new CheckBox("Highlight lines with whitespace-only modifications with a different color");
        whitespaceCheck.setSelected(cfg.isHighlightWhitespaceOnlyModifications());
        whitespaceCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        whitespaceCheck.setPadding(new Insets(0, 0, 0, 24));
        whitespaceCheck.disableProperty().bind(gutterCheck.selectedProperty().not());
        inputs.put("vcs_highlightWhitespaceOnlyModifications", whitespaceCheck);

        content.getChildren().addAll(
                confirmSection,
                createdRow,
                applyOutsideCheck,
                deletedRow,
                updateRow,
                dropCommitsCheck,
                dropCommitsHint,
                changesSection,
                serverConflictsRow,
                changedDaysRow,
                changedDaysHint,
                highlightDirsCheck,
                patchRow,
                restoreWorkspaceCheck,
                restoreWorkspaceHint,
                limitHistoryRow,
                gutterSection,
                gutterCheck,
                errorStripeCheck,
                whitespaceCheck
        );

        return content;
    }
}

package com.roze.dbnavigator.ui.vcs;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.SettingsDialog;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.function.Consumer;

/**
 * Settings panel for Version Control > Commit,
 * matching DataGrip screenshot media_1791678916727_19658857.png.
 */
public class VcsCommitPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;
    private final Consumer<String> navigateTo;

    public VcsCommitPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs, Consumer<String> navigateTo) {
        this.settings = settings;
        this.inputs = inputs;
        this.navigateTo = navigateTo;
    }

    public Node createPanel() {
        AppSettingsStore.VcsCommitConfig cfg = settings.getVcsCommit();

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Top Checkbox
        CheckBox clearMessageCheck = new CheckBox("Clear initial commit message");
        clearMessageCheck.setSelected(cfg.isClearInitialCommitMessage());
        clearMessageCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_clearInitialCommitMessage", clearMessageCheck);

        // Commit message inspections section
        Label inspectionsLabel = new Label("Commit message inspections:");
        inspectionsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

        List<AppSettingsStore.CommitInspectionConfig> inspectionList = new ArrayList<>();
        if (cfg.getInspections() != null) {
            for (AppSettingsStore.CommitInspectionConfig i : cfg.getInspections()) {
                inspectionList.add(i.copy());
            }
        }
        if (inspectionList.isEmpty()) {
            inspectionList.addAll(AppSettingsStore.VcsCommitConfig.defaultInspections());
        }
        inputs.put("vcs_commitInspections", inspectionList);

        VBox inspectionsBox = new VBox(4);
        inspectionsBox.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");

        for (AppSettingsStore.CommitInspectionConfig insp : inspectionList) {
            CheckBox cb = new CheckBox(insp.getName());
            cb.setSelected(insp.isEnabled());
            cb.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
            cb.selectedProperty().addListener((obs, oldV, newV) -> insp.setEnabled(newV));

            ComboBox<String> severityCombo = new ComboBox<>();
            severityCombo.getItems().addAll("Warning", "Error", "Weak Warning", "Information", "Server problem");
            severityCombo.setValue(insp.getSeverity());
            severityCombo.setPrefWidth(140);
            severityCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 11px;");
            severityCombo.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) insp.setSeverity(newV);
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(8, cb, spacer, severityCombo);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 4, 2, 4));
            inspectionsBox.getChildren().add(row);
        }

        // Section: Commit Checks
        HBox commitChecksSection = SettingsDialog.buildTitledSectionLine("Commit Checks");

        CheckBox reformatCodeCheck = new CheckBox("Reformat code");
        reformatCodeCheck.setSelected(cfg.isReformatCode());
        reformatCodeCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_reformatCode", reformatCodeCheck);

        CheckBox cleanupCheck = new CheckBox("Cleanup");
        cleanupCheck.setSelected(cfg.isCleanup());
        cleanupCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_cleanup", cleanupCheck);

        Hyperlink cleanupProfileLink = new Hyperlink("Choose profile");
        cleanupProfileLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        cleanupProfileLink.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(cfg.getCleanupProfile());
            dialog.setTitle("Choose Cleanup Profile");
            dialog.setHeaderText("Specify code inspection and cleanup profile:");
            dialog.showAndWait().ifPresent(val -> {
                cfg.setCleanupProfile(val);
                inputs.put("vcs_cleanupProfile", val);
            });
        });

        HBox cleanupRow = new HBox(8, cleanupCheck, cleanupProfileLink);
        cleanupRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Advanced Commit Checks
        HBox advancedSection = SettingsDialog.buildTitledSectionLine("Advanced Commit Checks");

        CheckBox analyzeCodeCheck = new CheckBox("Analyze code");
        analyzeCodeCheck.setSelected(cfg.isAnalyzeCode());
        analyzeCodeCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_analyzeCode", analyzeCodeCheck);

        Hyperlink analyzeProfileLink = new Hyperlink("Choose profile");
        analyzeProfileLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        analyzeProfileLink.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(cfg.getAnalyzeCodeProfile());
            dialog.setTitle("Choose Code Analysis Profile");
            dialog.setHeaderText("Specify inspection profile for pre-commit analysis:");
            dialog.showAndWait().ifPresent(val -> {
                cfg.setAnalyzeCodeProfile(val);
                inputs.put("vcs_analyzeCodeProfile", val);
            });
        });

        HBox analyzeRow = new HBox(8, analyzeCodeCheck, analyzeProfileLink);
        analyzeRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox checkTodoCheck = new CheckBox("Check TODO");
        checkTodoCheck.setSelected(cfg.isCheckTodo());
        checkTodoCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_checkTodo", checkTodoCheck);

        Hyperlink configureTodoLink = new Hyperlink("Configure");
        configureTodoLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        configureTodoLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Editor / TODO");
            }
        });

        HBox todoRow = new HBox(8, checkTodoCheck, configureTodoLink);
        todoRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox runConfigCheck = new CheckBox("Run Configuration");
        runConfigCheck.setSelected(cfg.isRunConfiguration());
        runConfigCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_runConfiguration", runConfigCheck);

        Hyperlink chooseConfigLink = new Hyperlink("Choose configuration");
        chooseConfigLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        chooseConfigLink.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(cfg.getRunConfigurationName());
            dialog.setTitle("Choose Run Configuration");
            dialog.setHeaderText("Run configuration to execute during commit check:");
            dialog.showAndWait().ifPresent(val -> {
                cfg.setRunConfigurationName(val);
                inputs.put("vcs_runConfigurationName", val);
            });
        });

        HBox runConfigRow = new HBox(8, runConfigCheck, chooseConfigLink);
        runConfigRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox runAdvancedAfterCheck = new CheckBox("Run advanced checks after a commit is done");
        runAdvancedAfterCheck.setSelected(cfg.isRunAdvancedChecksAfterCommit());
        runAdvancedAfterCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_runAdvancedChecksAfterCommit", runAdvancedAfterCheck);

        Label failedChecksHint = new Label("Failed checks will not prevent a commit");
        failedChecksHint.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-padding: 0 0 0 20;");

        content.getChildren().addAll(
                clearMessageCheck,
                inspectionsLabel,
                inspectionsBox,
                commitChecksSection,
                reformatCodeCheck,
                cleanupRow,
                advancedSection,
                analyzeRow,
                todoRow,
                runConfigRow,
                runAdvancedAfterCheck,
                failedChecksHint
        );

        return content;
    }
}

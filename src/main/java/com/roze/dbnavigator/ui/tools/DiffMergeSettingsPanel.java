package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * Settings panel for Tools > Diff & Merge.
 * Matches DataGrip screenshot media_1791725795072_28350d30.png.
 */
public class DiffMergeSettingsPanel {

    public static final List<String> CONTEXT_VALUES = List.of("1", "2", "4", "8", "Disable");

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public DiffMergeSettingsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.DiffMergeConfig cfg = settings.getDiffMerge();

        VBox content = new VBox(16);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // ---------------------------------------------------------------------
        // Section: Diff
        // ---------------------------------------------------------------------
        Label diffHeader = new Label("Diff");
        diffHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        Separator diffSep = new Separator();
        HBox diffTitleBox = new HBox(10, diffHeader, diffSep);
        diffTitleBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(diffSep, Priority.ALWAYS);

        // Context lines slider
        Label contextLinesLabel = new Label("Context lines:");
        contextLinesLabel.setPrefWidth(100);
        contextLinesLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        int initialIndex = CONTEXT_VALUES.indexOf(cfg.getContextLines());
        if (initialIndex < 0) initialIndex = 2; // Default "4"

        Slider contextSlider = new Slider(0, 4, initialIndex);
        contextSlider.setMajorTickUnit(1);
        contextSlider.setMinorTickCount(0);
        contextSlider.setSnapToTicks(true);
        contextSlider.setShowTickMarks(true);
        contextSlider.setShowTickLabels(false);
        contextSlider.setPrefWidth(160);
        inputs.put("tools_diffmerge_contextSlider", contextSlider);

        HBox sliderRow = new HBox(10, contextLinesLabel, contextSlider);
        sliderRow.setAlignment(Pos.CENTER_LEFT);

        Label tickLabel1 = new Label("1");
        Label tickLabel2 = new Label("2");
        Label tickLabel4 = new Label("4");
        Label tickLabel8 = new Label("8");
        Label tickLabelDisable = new Label("Disable");
        HBox tickLabels = new HBox(22, tickLabel1, tickLabel2, tickLabel4, tickLabel8, tickLabelDisable);
        tickLabels.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-dim;");
        tickLabels.setPadding(new Insets(-4, 0, 4, 110));

        VBox sliderBox = new VBox(2, sliderRow, tickLabels);

        CheckBox nextFileCheck = new CheckBox("Go to the next file after reaching last change");
        nextFileCheck.setSelected(cfg.isGoToNextFileAfterLastChange());
        nextFileCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_diffmerge_goToNextFile", nextFileCheck);

        Label histLabel = new Label("Include diffs in navigation history:");
        histLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> histCombo = new ComboBox<>();
        histCombo.getItems().addAll("Until the diff is closed", "Always", "Never");
        histCombo.setValue(cfg.getIncludeDiffsInNavigationHistory());
        histCombo.setPrefWidth(200);
        inputs.put("tools_diffmerge_navHistory", histCombo);

        HBox histRow = new HBox(10, histLabel, histCombo);
        histRow.setAlignment(Pos.CENTER_LEFT);

        VBox diffBox = new VBox(10, diffTitleBox, sliderBox, nextFileCheck, histRow);

        // ---------------------------------------------------------------------
        // Section: Merge
        // ---------------------------------------------------------------------
        Label mergeHeader = new Label("Merge");
        mergeHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        Separator mergeSep = new Separator();
        HBox mergeTitleBox = new HBox(10, mergeHeader, mergeSep);
        mergeTitleBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(mergeSep, Priority.ALWAYS);

        CheckBox autoNonConflicting = new CheckBox("Automatically apply non-conflicting changes");
        autoNonConflicting.setSelected(cfg.isAutoApplyNonConflictingChanges());
        autoNonConflicting.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_diffmerge_autoApply", autoNonConflicting);

        CheckBox autoResolveImports = new CheckBox("Automatically resolve conflicts in import statements");
        autoResolveImports.setSelected(cfg.isAutoResolveConflictsInImports());
        autoResolveImports.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_diffmerge_autoResolveImports", autoResolveImports);

        CheckBox highlightModifiedLines = new CheckBox("Highlight modified lines in gutter");
        highlightModifiedLines.setSelected(cfg.isHighlightModifiedLinesInGutter());
        highlightModifiedLines.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_diffmerge_highlightModified", highlightModifiedLines);

        VBox mergeBox = new VBox(10, mergeTitleBox, autoNonConflicting, autoResolveImports, highlightModifiedLines);

        content.getChildren().addAll(diffBox, mergeBox);
        return content;
    }
}

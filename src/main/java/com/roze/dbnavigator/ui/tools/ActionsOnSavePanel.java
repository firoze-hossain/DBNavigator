package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Settings panel for Tools > Actions on Save.
 * Matches DataGrip screenshots media_1791723216612_e07055b9.png and media_1791723232323_3c450799.png.
 */
public class ActionsOnSavePanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;
    private final Consumer<String> navigateTo;

    public ActionsOnSavePanel(AppSettingsStore.Settings settings, Map<String, Object> inputs, Consumer<String> navigateTo) {
        this.settings = settings;
        this.inputs = inputs;
        this.navigateTo = navigateTo;
    }

    public Node createPanel() {
        AppSettingsStore.ActionsOnSaveConfig cfg = settings.getActionsOnSave();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Header Row: Action on left, Activated On on right
        Label actionHeader = new Label("Action");
        actionHeader.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        Label activatedOnHeader = new Label("Activated On");
        activatedOnHeader.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox headerRow = new HBox(actionHeader, headerSpacer, activatedOnHeader);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(0, 4, 4, 0));

        // Separator below header
        Separator headerSep = new Separator();

        // Main Action Row: Reformat code
        CheckBox reformatCheck = new CheckBox("Reformat code");
        reformatCheck.setSelected(cfg.isReformatCode());
        reformatCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_actionsOnSave_reformatCode", reformatCheck);

        Hyperlink scopeLink = new Hyperlink("Configure scope...");
        scopeLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-padding: 0 4 0 4;");
        scopeLink.disableProperty().bind(reformatCheck.selectedProperty().not());
        scopeLink.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Reformat Code scope is configured across all modified project files.", ButtonType.OK);
            if (scopeLink.getScene() != null && scopeLink.getScene().getWindow() != null) {
                alert.initOwner(scopeLink.getScene().getWindow());
            }
            alert.showAndWait();
        });

        // File types selector popup matching DataGrip Screenshot 4!
        List<String> allAvailableTypes = AppSettingsStore.ActionsOnSaveConfig.defaultFileTypes();
        Set<String> selectedTypes = new LinkedHashSet<>(cfg.getSelectedFileTypes());
        boolean initialAll = cfg.isAllFileTypes();

        Button fileTypesBtn = new Button();
        fileTypesBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #589df6; -fx-font-size: 12px; -fx-underline: false; -fx-padding: 0 4 0 4;");
        fileTypesBtn.disableProperty().bind(reformatCheck.selectedProperty().not());

        Runnable updateButtonText = () -> {
            if (selectedTypes.size() == allAvailableTypes.size()) {
                fileTypesBtn.setText("All file types ▾");
            } else if (selectedTypes.isEmpty()) {
                fileTypesBtn.setText("No file types ▾");
            } else {
                fileTypesBtn.setText(selectedTypes.size() + " file types ▾");
            }
        };
        updateButtonText.run();

        // Store selected types in inputs
        inputs.put("tools_actionsOnSave_selectedFileTypes", selectedTypes);

        fileTypesBtn.setOnAction(e -> {
            ContextMenu menu = new ContextMenu();
            menu.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

            // Master "All file types" checkbox
            CustomMenuItem allItem = new CustomMenuItem();
            CheckBox allCheck = new CheckBox("All file types");
            allCheck.setStyle("-fx-text-fill: -text; -fx-font-weight: bold; -fx-font-size: 12px;");
            allCheck.setSelected(selectedTypes.size() == allAvailableTypes.size());
            allItem.setContent(allCheck);
            allItem.setHideOnClick(false);

            menu.getItems().add(allItem);
            menu.getItems().add(new SeparatorMenuItem());

            List<CheckBox> subChecks = new ArrayList<>();
            for (String typeName : allAvailableTypes) {
                CustomMenuItem item = new CustomMenuItem();
                CheckBox cb = new CheckBox(typeName);
                cb.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
                cb.setSelected(selectedTypes.contains(typeName));
                subChecks.add(cb);

                cb.selectedProperty().addListener((obs, oldV, newV) -> {
                    if (newV) {
                        selectedTypes.add(typeName);
                    } else {
                        selectedTypes.remove(typeName);
                    }
                    allCheck.setSelected(selectedTypes.size() == allAvailableTypes.size());
                    updateButtonText.run();
                });

                item.setContent(cb);
                item.setHideOnClick(false);
                menu.getItems().add(item);
            }

            allCheck.setOnAction(ev -> {
                boolean sel = allCheck.isSelected();
                for (CheckBox cb : subChecks) {
                    cb.setSelected(sel);
                }
                if (sel) {
                    selectedTypes.addAll(allAvailableTypes);
                } else {
                    selectedTypes.clear();
                }
                updateButtonText.run();
            });

            menu.show(fileTypesBtn, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        HBox actionLeft = new HBox(8, reformatCheck, scopeLink, fileTypesBtn);
        actionLeft.setAlignment(Pos.CENTER_LEFT);

        // Right side dropdown: Activated On
        ComboBox<String> activatedOnCombo = new ComboBox<>();
        activatedOnCombo.getItems().addAll("Any save", "Explicit save only");
        activatedOnCombo.setValue(cfg.getActivatedOn() != null ? cfg.getActivatedOn() : "Any save");
        activatedOnCombo.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-dim; -fx-font-size: 12px; -fx-border-color: transparent;");
        activatedOnCombo.setPrefWidth(130);
        activatedOnCombo.disableProperty().bind(reformatCheck.selectedProperty().not());
        inputs.put("tools_actionsOnSave_activatedOn", activatedOnCombo);

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        HBox actionRow = new HBox(actionLeft, actionSpacer, activatedOnCombo);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox tableBox = new VBox(8, headerRow, headerSep, actionRow);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        // Bottom Link: Configure autosave options...
        Hyperlink autosaveLink = new Hyperlink("Configure autosave options...");
        autosaveLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-padding: 10 0 0 0;");
        autosaveLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Appearance & Behavior / System Settings");
            }
        });

        content.getChildren().addAll(tableBox, autosaveLink);
        return content;
    }
}

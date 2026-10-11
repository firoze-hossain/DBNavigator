package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Settings panel for Tools > Diagrams.
 * Matches DataGrip screenshot media_1791725764506_47a60e86.png.
 */
public class DiagramsSettingsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public DiagramsSettingsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.DiagramsConfig cfg = settings.getDiagrams();

        HBox mainContainer = new HBox(24);
        mainContainer.setPadding(new Insets(10, 16, 16, 14));
        mainContainer.setStyle("-fx-background-color: transparent;");

        // ---------------------------------------------------------------------
        // LEFT COLUMN: Categories Tree
        // ---------------------------------------------------------------------
        Label leftHeader = new Label("Select categories that will be enabled on showing the diagram");
        leftHeader.setWrapText(true);
        leftHeader.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");
        leftHeader.setPrefWidth(260);

        Set<String> selectedCategories = new HashSet<>(cfg.getEnabledCategories());
        inputs.put("tools_diagrams_enabledCategories", selectedCategories);

        CheckBoxTreeItem<String> rootItem = new CheckBoxTreeItem<>("Root");
        rootItem.setExpanded(true);

        addCategoryBranch(rootItem, "Show Difference", List.of("Details"), selectedCategories);
        addCategoryBranch(rootItem, "Database Schema Diagram",
                List.of("Key columns", "Columns", "Virtual foreign keys", "Comments"),
                selectedCategories);
        addCategoryBranch(rootItem, "Graphical Explain Plan", List.of("Attributes"), selectedCategories);

        TreeView<String> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setCellFactory(CheckBoxTreeCell.forTreeView());
        treeView.setPrefWidth(270);
        treeView.setPrefHeight(420);
        treeView.setStyle("-fx-background-color: -surface; -fx-border-color: -border-subtle; -fx-border-radius: 4; -fx-background-radius: 4;");

        VBox leftColumn = new VBox(8, leftHeader, treeView);
        leftColumn.setPrefWidth(270);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // ---------------------------------------------------------------------
        // RIGHT COLUMN: Scope, Appearance, Layout
        // ---------------------------------------------------------------------
        VBox rightColumn = new VBox(14);
        rightColumn.setPrefWidth(380);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        // Default Scope
        Label defaultScopeLabel = new Label("Default scope:");
        defaultScopeLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        defaultScopeLabel.setPrefWidth(180);

        ComboBox<String> scopeCombo = new ComboBox<>();
        scopeCombo.getItems().addAll("All", "Project", "Current Schema");
        scopeCombo.setValue(cfg.getDefaultScope());
        scopeCombo.setPrefWidth(180);
        inputs.put("tools_diagrams_defaultScope", scopeCombo);

        HBox scopeRow = new HBox(10, defaultScopeLabel, scopeCombo);
        scopeRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Appearance
        Label appearanceLabel = new Label("Appearance");
        appearanceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");
        Separator sepAppearance = new Separator();
        HBox appearanceHeader = new HBox(10, appearanceLabel, sepAppearance);
        appearanceHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sepAppearance, Priority.ALWAYS);

        Label nodeItemStyleLabel = new Label("Node item style:");
        nodeItemStyleLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        nodeItemStyleLabel.setPrefWidth(180);

        ComboBox<String> nodeItemStyleCombo = new ComboBox<>();
        nodeItemStyleCombo.getItems().addAll("With types aligned", "Compact", "UML");
        nodeItemStyleCombo.setValue(cfg.getNodeItemStyle());
        nodeItemStyleCombo.setPrefWidth(180);
        inputs.put("tools_diagrams_nodeItemStyle", nodeItemStyleCombo);

        HBox styleRow = new HBox(10, nodeItemStyleLabel, nodeItemStyleCombo);
        styleRow.setAlignment(Pos.CENTER_LEFT);

        Label shortenLengthLabel = new Label("Shorten node items on length:");
        shortenLengthLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        shortenLengthLabel.setPrefWidth(180);

        Spinner<Integer> shortenSpinner = new Spinner<>(10, 500, cfg.getShortenNodeItemsLength(), 5);
        shortenSpinner.setEditable(true);
        shortenSpinner.setPrefWidth(180);
        inputs.put("tools_diagrams_shortenLength", shortenSpinner);

        HBox shortenRow = new HBox(10, shortenLengthLabel, shortenSpinner);
        shortenRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox showGridCheck = new CheckBox("Show grid by default");
        showGridCheck.setSelected(cfg.isShowGridByDefault());
        showGridCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_diagrams_showGrid", showGridCheck);

        CheckBox enableAnimationsCheck = new CheckBox("Enable animations");
        enableAnimationsCheck.setSelected(cfg.isEnableAnimations());
        enableAnimationsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_diagrams_enableAnimations", enableAnimationsCheck);

        CheckBox syntaxHighlightingCheck = new CheckBox("Enable node items syntax highlighting");
        syntaxHighlightingCheck.setSelected(cfg.isEnableNodeItemsSyntaxHighlighting());
        syntaxHighlightingCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_diagrams_syntaxHighlighting", syntaxHighlightingCheck);

        VBox appearanceBox = new VBox(10, appearanceHeader, styleRow, shortenRow, showGridCheck, enableAnimationsCheck, syntaxHighlightingCheck);

        // Section: Layout
        Label layoutLabel = new Label("Layout");
        layoutLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");
        Separator sepLayout = new Separator();
        HBox layoutHeader = new HBox(10, layoutLabel, sepLayout);
        layoutHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sepLayout, Priority.ALWAYS);

        Label defaultLayoutLabel = new Label("Default layout:");
        defaultLayoutLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        defaultLayoutLabel.setPrefWidth(180);

        ComboBox<String> layoutCombo = new ComboBox<>();
        layoutCombo.getItems().addAll("Hierarchic Compact", "Hierarchical", "Organic", "Orthogonal", "Circular");
        layoutCombo.setValue(cfg.getDefaultLayout());
        layoutCombo.setPrefWidth(180);
        inputs.put("tools_diagrams_defaultLayout", layoutCombo);

        HBox layoutRow = new HBox(10, defaultLayoutLabel, layoutCombo);
        layoutRow.setAlignment(Pos.CENTER_LEFT);

        Label switchLayoutLabel = new Label("Layout on category switch:");
        switchLayoutLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        switchLayoutLabel.setPrefWidth(180);

        ComboBox<String> switchCombo = new ComboBox<>();
        switchCombo.getItems().addAll("With light layouter", "Keep positions", "Relayout all");
        switchCombo.setValue(cfg.getLayoutOnCategorySwitch());
        switchCombo.setPrefWidth(180);
        inputs.put("tools_diagrams_layoutOnCategorySwitch", switchCombo);

        HBox switchRow = new HBox(10, switchLayoutLabel, switchCombo);
        switchRow.setAlignment(Pos.CENTER_LEFT);

        Label durationLabel = new Label("Layout animation duration:");
        durationLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        durationLabel.setPrefWidth(180);

        Spinner<Integer> durationSpinner = new Spinner<>(0, 10000, cfg.getLayoutAnimationDuration(), 100);
        durationSpinner.setEditable(true);
        durationSpinner.setPrefWidth(180);
        inputs.put("tools_diagrams_animationDuration", durationSpinner);

        HBox durationRow = new HBox(10, durationLabel, durationSpinner);
        durationRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox fitContentCheck = new CheckBox("Fit content after layout");
        fitContentCheck.setSelected(cfg.isFitContentAfterLayout());
        fitContentCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_diagrams_fitContent", fitContentCheck);

        CheckBox relayoutOnNewCheck = new CheckBox("Do relayout when new elements were added");
        relayoutOnNewCheck.setSelected(cfg.isRelayoutWhenNewElementsAdded());
        relayoutOnNewCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_diagrams_relayoutOnNewElements", relayoutOnNewCheck);

        VBox layoutBox = new VBox(10, layoutHeader, layoutRow, switchRow, durationRow, fitContentCheck, relayoutOnNewCheck);

        rightColumn.getChildren().addAll(scopeRow, appearanceBox, layoutBox);

        mainContainer.getChildren().addAll(leftColumn, rightColumn);
        return mainContainer;
    }

    private void addCategoryBranch(CheckBoxTreeItem<String> parent, String parentName,
                                   List<String> childNames, Set<String> selectedCategories) {
        CheckBoxTreeItem<String> parentItem = new CheckBoxTreeItem<>(parentName);
        parentItem.setIndependent(true);
        parentItem.setSelected(selectedCategories.contains(parentName));
        parentItem.setExpanded(true);
        parentItem.selectedProperty().addListener((obs, oldV, newV) -> {
            if (Boolean.TRUE.equals(newV)) {
                selectedCategories.add(parentName);
            } else {
                selectedCategories.remove(parentName);
            }
        });
        parent.getChildren().add(parentItem);

        for (String childName : childNames) {
            CheckBoxTreeItem<String> childItem = new CheckBoxTreeItem<>(childName);
            childItem.setIndependent(true);
            childItem.setSelected(selectedCategories.contains(childName));
            childItem.selectedProperty().addListener((obs, oldV, newV) -> {
                if (Boolean.TRUE.equals(newV)) {
                    selectedCategories.add(childName);
                } else {
                    selectedCategories.remove(childName);
                }
            });
            parentItem.getChildren().add(childItem);
        }
    }
}

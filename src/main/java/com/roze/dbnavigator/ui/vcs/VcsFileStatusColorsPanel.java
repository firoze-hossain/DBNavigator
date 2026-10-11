package com.roze.dbnavigator.ui.vcs;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Settings panel for Version Control > File Status Colors,
 * matching DataGrip screenshot media_1791678981739_3bc089e1.png.
 */
public class VcsFileStatusColorsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public VcsFileStatusColorsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.VcsFileStatusColorsConfig cfg = settings.getVcsFileStatusColors();

        // Working list of status colors
        ObservableList<AppSettingsStore.VcsFileStatusColorEntry> statusList = FXCollections.observableArrayList();
        if (cfg.getStatusColors() != null) {
            for (AppSettingsStore.VcsFileStatusColorEntry e : cfg.getStatusColors()) {
                statusList.add(e.copy());
            }
        }
        if (statusList.isEmpty()) {
            statusList.addAll(AppSettingsStore.VcsFileStatusColorsConfig.defaultStatusColors());
        }
        inputs.put("vcs_fileStatusColors", statusList);

        // Left Pane: Status List + Footer note
        ListView<AppSettingsStore.VcsFileStatusColorEntry> listView = new ListView<>(statusList);
        listView.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(listView, Priority.ALWAYS);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AppSettingsStore.VcsFileStatusColorEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    String prefix = item.isCustomized() ? "* " : "  ";
                    setText(prefix + item.getStatusName());

                    String hex = item.getColorHex();
                    if (hex == null || hex.isBlank()) hex = "A9B7C6";
                    String colorStyle = "-fx-text-fill: #" + hex + ";";

                    if (isSelected()) {
                        setStyle("-fx-background-color: #2e436e; " + colorStyle + " -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: transparent; " + colorStyle);
                    }
                }
            }
        });

        Label customizedLegend = new Label("* Customized");
        customizedLegend.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px; -fx-padding: 4 0 0 4;");

        VBox leftPane = new VBox(6, listView, customizedLegend);
        leftPane.setPrefWidth(300);
        leftPane.setMinWidth(220);

        // Right Pane: Editor for selected status color
        VBox rightPane = new VBox(14);
        rightPane.setPadding(new Insets(10, 14, 14, 14));
        rightPane.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        CheckBox colorCheck = new CheckBox("File Status Color:");
        colorCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        Button colorBtn = new Button();
        colorBtn.setPrefWidth(90);
        colorBtn.setPrefHeight(26);
        colorBtn.setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");

        Button restoreDefaultBtn = new Button("Restore Default");
        restoreDefaultBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 14; -fx-cursor: hand;");

        HBox colorControlRow = new HBox(12, colorCheck, colorBtn);
        colorControlRow.setAlignment(Pos.CENTER_LEFT);

        HBox restoreRow = new HBox(restoreDefaultBtn);
        restoreRow.setAlignment(Pos.CENTER_LEFT);

        rightPane.getChildren().addAll(colorControlRow, restoreRow);

        // Update right pane when selection changes
        Consumer<AppSettingsStore.VcsFileStatusColorEntry> updateEditor = (entry) -> {
            if (entry == null) {
                colorCheck.setSelected(false);
                colorCheck.setDisable(true);
                colorBtn.setDisable(true);
                colorBtn.setText("");
                colorBtn.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40;");
                restoreDefaultBtn.setDisable(true);
                return;
            }

            colorCheck.setDisable(false);
            colorCheck.setSelected(true);

            String hex = entry.getColorHex();
            if (hex == null || hex.isBlank()) hex = entry.getDefaultColorHex();
            colorBtn.setDisable(false);
            colorBtn.setText(hex);
            boolean dark = isColorDark(hex);
            colorBtn.setStyle("-fx-background-color: #" + hex + "; -fx-text-fill: " + (dark ? "#ffffff" : "#000000") +
                    "; -fx-border-color: #393b40; -fx-background-radius: 3; -fx-border-radius: 3; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");

            boolean canRestore = !hex.equalsIgnoreCase(entry.getDefaultColorHex()) || entry.isCustomized();
            restoreDefaultBtn.setDisable(!canRestore);
        };

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            updateEditor.accept(selected);
        });

        // Color Picker dialog action
        colorBtn.setOnAction(e -> {
            AppSettingsStore.VcsFileStatusColorEntry selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            showColorPickerDialog(selected.getColorHex(), colorBtn, newHex -> {
                selected.setColorHex(newHex);
                selected.setCustomized(true);
                listView.refresh();
                updateEditor.accept(selected);
            });
        });

        // Restore Default action
        restoreDefaultBtn.setOnAction(e -> {
            AppSettingsStore.VcsFileStatusColorEntry selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            selected.setColorHex(selected.getDefaultColorHex());
            selected.setCustomized(false);
            listView.refresh();
            updateEditor.accept(selected);
        });

        // Pre-select "Changelist conflict" as shown in Image 5, or first item
        int selectIdx = 0;
        for (int i = 0; i < statusList.size(); i++) {
            if ("Changelist conflict".equalsIgnoreCase(statusList.get(i).getStatusName())) {
                selectIdx = i;
                break;
            }
        }
        if (!statusList.isEmpty()) {
            listView.getSelectionModel().select(selectIdx);
        }

        // Layout
        SplitPane splitPane = new SplitPane(leftPane, rightPane);
        splitPane.setDividerPositions(0.48);
        splitPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        VBox content = new VBox(splitPane);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(content, Priority.ALWAYS);

        return content;
    }

    private static boolean isColorDark(String hex) {
        if (hex == null || hex.length() < 6) return true;
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            double lum = (0.299 * r + 0.587 * g + 0.114 * b);
            return lum < 128;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static void showColorPickerDialog(String currentHex, Button anchor, Consumer<String> onColorSelected) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Choose Color");

        VBox content = new VBox(10);
        content.setPadding(new Insets(14));
        content.setStyle("-fx-background-color: #2b2d30;");

        Color initial;
        try {
            initial = Color.web("#" + (currentHex != null && !currentHex.isBlank() ? currentHex : "FFFFFF"));
        } catch (Exception ex) {
            initial = Color.WHITE;
        }
        ColorPicker cp = new ColorPicker(initial);
        cp.setStyle("-fx-background-color: #1e1f22; -fx-color-label-visible: true;");

        TextField hexField = new TextField(currentHex != null ? currentHex : "FFFFFF");
        hexField.setPrefWidth(80);
        hexField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-background-radius: 3;");

        cp.valueProperty().addListener((obs, oldC, newC) -> {
            if (newC != null) {
                String h = String.format("%02X%02X%02X",
                        (int) (newC.getRed() * 255),
                        (int) (newC.getGreen() * 255),
                        (int) (newC.getBlue() * 255));
                hexField.setText(h);
            }
        });

        HBox pickRow = new HBox(8, new Label("Color:"), cp, new Label("#"), hexField);
        pickRow.setAlignment(Pos.CENTER_LEFT);

        FlowPane presets = new FlowPane(4, 4);
        presets.setPrefWrapLength(240);
        String[] palette = {
                "DE6A66", "629755", "6897BB", "BC3F3C", "848504", "9876AA", "FF6B68", "E26E60",
                "0E82C7", "32B8CD", "507874", "6E6E6E", "777777", "8F9D6A", "D1675A", "A9B7C6"
        };
        for (String c : palette) {
            Button swatch = new Button();
            swatch.setPrefSize(18, 18);
            swatch.setStyle("-fx-background-color: #" + c + "; -fx-border-color: #393b40; -fx-background-radius: 2; -fx-border-radius: 2; -fx-cursor: hand;");
            swatch.setOnAction(e -> {
                hexField.setText(c);
                try {
                    cp.setValue(Color.web("#" + c));
                } catch (Exception ignored) {}
            });
            presets.getChildren().add(swatch);
        }

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574f0; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 4 14; -fx-cursor: hand;");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393b40; -fx-text-fill: -text; -fx-background-radius: 4; -fx-padding: 4 14; -fx-cursor: hand;");

        okBtn.setOnAction(e -> {
            String val = hexField.getText().trim().replace("#", "").toUpperCase();
            if (val.length() == 6) {
                onColorSelected.accept(val);
                dialog.close();
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(8, okBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(pickRow, presets, buttons);

        Scene scene = new Scene(content);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}

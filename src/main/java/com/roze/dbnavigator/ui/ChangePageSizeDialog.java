package com.roze.dbnavigator.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Custom "Change Page Size" modal dialog matching the reference IDE (Image 3):
 * - Checkbox: "Limit page size to"
 * - Numeric input field for page size (auto-focused & selected)
 * - Styled "OK" (primary blue) and "Cancel" buttons
 */
public class ChangePageSizeDialog extends Dialog<Integer> {

    private final CheckBox limitCheckBox;
    private final TextField sizeField;

    public ChangePageSizeDialog(int currentPageSize) {
        DialogTheme.apply(this);
        setTitle("Change Page Size");
        setHeaderText(null);
        setGraphic(null);

        getDialogPane().getStyleClass().add("change-page-size-dialog");

        if (ThemeManager.getCurrent() == com.roze.dbnavigator.db.AppSettingsStore.Theme.LIGHT) {
            getDialogPane().getStylesheets().clear();
            getDialogPane().getStylesheets().add(ThemeManager.stylesheetUrl(com.roze.dbnavigator.db.AppSettingsStore.Theme.LIGHT));
        }

        boolean isLimited = currentPageSize < 100000;
        int displaySize = isLimited && currentPageSize > 0 ? currentPageSize : 500;

        limitCheckBox = new CheckBox("Limit page size to");
        limitCheckBox.getStyleClass().add("limit-page-size-checkbox");
        limitCheckBox.setSelected(isLimited);

        sizeField = new TextField(String.valueOf(displaySize));
        sizeField.getStyleClass().add("page-size-field");
        sizeField.setPrefWidth(65);
        sizeField.setMaxWidth(65);
        sizeField.setAlignment(Pos.CENTER);
        sizeField.disableProperty().bind(limitCheckBox.selectedProperty().not());

        // Numeric only input
        sizeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                sizeField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        HBox inputRow = new HBox(8, limitCheckBox, sizeField);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12, inputRow);
        content.setPadding(new Insets(16, 16, 16, 16));
        content.setAlignment(Pos.CENTER_LEFT);
        getDialogPane().setContent(content);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.getStyleClass().add("ok-button");
            okBtn.setDefaultButton(true);
        }

        Button cancelBtn = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.getStyleClass().add("cancel-button");
            cancelBtn.setCancelButton(true);
        }

        sizeField.setOnAction(e -> {
            Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
            if (ok != null) {
                ok.fire();
            }
        });

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (limitCheckBox.isSelected()) {
                    try {
                        int val = Integer.parseInt(sizeField.getText().trim());
                        return val > 0 ? val : 500;
                    } catch (NumberFormatException e) {
                        return 500;
                    }
                } else {
                    return 100000; // Unlimited / All
                }
            }
            return null;
        });

        Platform.runLater(() -> {
            if (limitCheckBox.isSelected()) {
                sizeField.requestFocus();
                sizeField.selectAll();
            } else {
                limitCheckBox.requestFocus();
            }
        });
    }

    public CheckBox getLimitCheckBox() {
        return limitCheckBox;
    }

    public TextField getSizeField() {
        return sizeField;
    }

    public static Optional<Integer> show(int currentSize, Window owner) {
        ChangePageSizeDialog dlg = new ChangePageSizeDialog(currentSize);
        if (owner != null) {
            dlg.initOwner(owner);
        }
        return dlg.showAndWait();
    }
}

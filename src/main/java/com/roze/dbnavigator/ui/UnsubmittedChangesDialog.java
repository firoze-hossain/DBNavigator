package com.roze.dbnavigator.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * Confirmation dialog shown before reloading data or re-executing queries
 * when uncommitted/unsubmitted cell edits exist in the grid:
 *
 *   [Warning] Ignore Unsubmitted Changes
 *   Changes are not submitted. Data will be lost.
 *   Continue?
 *
 *   [ Yes ]   [ No ]
 */
public final class UnsubmittedChangesDialog {

    private static java.util.function.Function<Window, Boolean> confirmationHandler = null;

    public static void setConfirmationHandlerForTesting(java.util.function.Function<Window, Boolean> handler) {
        confirmationHandler = handler;
    }

    private UnsubmittedChangesDialog() {}

    /**
     * Prompts the user to confirm discarding unsubmitted changes.
     *
     * @param owner the owner window, or null
     * @return true if the user clicks "Yes" (confirm discard), false otherwise
     */
    public static boolean confirm(Window owner) {
        if (confirmationHandler != null) {
            return confirmationHandler.apply(owner);
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ignore Unsubmitted Changes");
        alert.setHeaderText("Ignore Unsubmitted Changes");
        alert.setContentText("Changes are not submitted. Data will be lost.\nContinue?");

        alert.setGraphic(Icons.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, "#e0a44c", 28));

        ButtonType yesType = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noType = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesType, noType);

        DialogTheme.apply(alert);
        if (owner != null) {
            alert.initOwner(owner);
        }

        Button yesBtn = (Button) alert.getDialogPane().lookupButton(yesType);
        if (yesBtn != null) {
            yesBtn.setDefaultButton(true);
            yesBtn.setStyle("-fx-background-color: #3574f0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 16;");
        }
        Button noBtn = (Button) alert.getDialogPane().lookupButton(noType);
        if (noBtn != null) {
            noBtn.setCancelButton(true);
            noBtn.setStyle("-fx-padding: 4 16;");
        }

        var result = alert.showAndWait();
        return result.isPresent() && result.get() == yesType;
    }
}

package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ConnectionStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * DataGrip-style password prompt: connections saved without "Save password"
 * ask for the password on first use each session.
 */
public final class Passwords {

    private Passwords() {}

    /**
     * Makes sure the profile has a usable password, prompting the user if not.
     * Must be called on the FX application thread.
     *
     * @return true when the connection can proceed, false when the user cancelled
     */
    public static boolean ensure(ConnectionProfile profile, Window owner) {
        if (!needsPassword(profile)) return true;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Password Required");
        dialog.setHeaderText("Enter password for " + profile.getUsername()
                + "@" + profile.getHost() + ":" + profile.getPort());

        PasswordField passwordField = new PasswordField();
        passwordField.setPrefWidth(280);
        CheckBox saveBox = new CheckBox("Save password for future sessions");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Password:"), 0, 0);
        grid.add(passwordField, 1, 0);
        grid.add(saveBox, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        javafx.application.Platform.runLater(passwordField::requestFocus);

        var result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return false;

        profile.setPassword(passwordField.getText());
        if (saveBox.isSelected()) {
            profile.setSavePassword(true);
            ConnectionStore.saveOrUpdate(profile);
        }
        return true;
    }

    /** True for server databases that authenticate with a user but have no password yet. */
    public static boolean needsPassword(ConnectionProfile profile) {
        if (profile.getType() == DatabaseType.SQLITE) return false;
        boolean hasUser = profile.getUsername() != null && !profile.getUsername().isBlank();
        boolean hasPassword = profile.getPassword() != null && !profile.getPassword().isEmpty();
        return hasUser && !hasPassword;
    }
}

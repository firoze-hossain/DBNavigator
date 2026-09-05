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
     * Once the user has answered the prompt this session — even by
     * submitting a genuinely blank password, e.g. for a trust-auth local
     * server — there's no way to tell "not yet asked" apart from "asked,
     * and there really is no password" just by looking at
     * profile.getPassword() being empty. Without tracking this separately,
     * every fresh connection (every schema expand, every new console) looked
     * like an unanswered prompt again and asked again. Session-scoped only,
     * matching the reference IDE: resets on app restart, not persisted
     * unless "Save password" was actually checked.
     */
    private static final java.util.Set<String> answeredThisSession = new java.util.HashSet<>();

    /**
     * Makes sure the profile has a usable password, prompting the user if not.
     * Must be called on the FX application thread.
     *
     * @return true when the connection can proceed, false when the user cancelled
     */
    public static boolean ensure(ConnectionProfile profile, Window owner) {
        if (!needsPassword(profile)) return true;
        if (answeredThisSession.contains(profile.getId())) return true;

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
        // Remember the answer for the rest of this session regardless of
        // whether what was submitted was blank — that's exactly what stops
        // the dialog from reappearing on every subsequent connection.
        answeredThisSession.add(profile.getId());
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
        // A real, previously-latent bug: a connection whose real, correct
        // password genuinely IS blank (e.g. a trust-auth server like a
        // freshly-started StratosDB instance) has no way to look any
        // different from "never confirmed yet" just from an empty password
        // string alone - both are password="" with no other signal. "Save
        // password" being checked is itself real, explicit confirmation
        // that the user already answered this exact prompt once (even by
        // submitting a blank password on purpose) and chose to persist
        // that answer - so it alone is enough to skip asking again, on
        // every subsequent app restart, without also requiring a non-empty
        // password to be true. Without this, a trust-auth connection with
        // "Save password" checked would show this dialog again every
        // single time the app restarted, no matter how many times the
        // user confirmed the blank password was correct.
        return hasUser && !hasPassword && !profile.isSavePassword();
    }
}

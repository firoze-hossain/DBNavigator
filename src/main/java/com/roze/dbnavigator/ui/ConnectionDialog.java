package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;

/** DataGrip-style "Data Source" dialog: driver picker, host/port/db/user/pass, test button. */
public class ConnectionDialog extends Dialog<ConnectionProfile> {

    private final ConnectionProfile profile;

    private final TextField nameField = new TextField();
    private final ComboBox<DatabaseType> typeCombo = new ComboBox<>();
    private final TextField hostField = new TextField();
    private final TextField portField = new TextField();
    private final TextField databaseField = new TextField();
    private final TextField userField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final CheckBox savePasswordCheck = new CheckBox("Save password");
    private final CheckBox sslCheck = new CheckBox("Use SSL");
    private final Label testResultLabel = new Label();
    private final Button browseButton = new Button("...");

    public ConnectionDialog(ConnectionProfile existing) {
        this.profile = existing != null ? existing.copy() : new ConnectionProfile();

        setTitle(existing == null ? "New Data Source" : "Edit Data Source");
        setHeaderText(null);
        getDialogPane().getStyleClass().add("connection-dialog");
        getDialogPane().setPrefWidth(520);

        typeCombo.getItems().addAll(DatabaseType.values());
        typeCombo.getSelectionModel().select(profile.getType());
        typeCombo.valueProperty().addListener((obs, old, type) -> applyTypeDefaults(type));

        nameField.setText(profile.getName());
        hostField.setText(profile.getHost());
        portField.setText(profile.getPort() > 0
                ? String.valueOf(profile.getPort())
                : String.valueOf(profile.getType().getDefaultPort()));
        databaseField.setText(profile.getDatabase());
        userField.setText(profile.getUsername());
        passwordField.setText(profile.getPassword());
        savePasswordCheck.setSelected(profile.isSavePassword());
        sslCheck.setSelected(profile.isUseSsl());

        browseButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select SQLite database file");
            File file = chooser.showOpenDialog(getDialogPane().getScene().getWindow());
            if (file != null) databaseField.setText(file.getAbsolutePath());
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        int row = 0;
        grid.add(new Label("Name:"), 0, row);
        grid.add(nameField, 1, row++, 2, 1);
        grid.add(new Label("Type:"), 0, row);
        grid.add(typeCombo, 1, row++, 2, 1);
        grid.add(new Label("Host:"), 0, row);
        grid.add(hostField, 1, row++, 2, 1);
        grid.add(new Label("Port:"), 0, row);
        grid.add(portField, 1, row++, 2, 1);
        grid.add(new Label("Database:"), 0, row);
        grid.add(databaseField, 1, row);
        grid.add(browseButton, 2, row++);
        grid.add(new Label("User:"), 0, row);
        grid.add(userField, 1, row++, 2, 1);
        grid.add(new Label("Password:"), 0, row);
        grid.add(passwordField, 1, row++, 2, 1);
        grid.add(new HBox(16, savePasswordCheck, sslCheck), 1, row++, 2, 1);

        Button testButton = new Button("Test Connection");
        testButton.setGraphic(Icons.of(FontAwesomeSolid.PLUG, "#57965c", 12));
        testButton.setOnAction(e -> testConnection(testButton));
        HBox testBox = new HBox(10, testButton, testResultLabel);
        testBox.setPadding(new Insets(4, 0, 0, 0));
        grid.add(testBox, 1, row++, 2, 1);

        GridPane.setHgrow(nameField, Priority.ALWAYS);
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        applyTypeDefaults(profile.getType());

        setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            collectInto(profile);
            if (profile.getName() == null || profile.getName().isBlank()) {
                profile.setName(profile.getType().getDisplayName() + " @ " + profile.getSummary());
            }
            return profile;
        });
    }

    private void applyTypeDefaults(DatabaseType type) {
        boolean sqlite = type == DatabaseType.SQLITE;
        boolean mongo = type == DatabaseType.MONGODB;

        hostField.setDisable(sqlite);
        portField.setDisable(sqlite);
        userField.setDisable(sqlite);
        passwordField.setDisable(sqlite);
        sslCheck.setDisable(sqlite || mongo);
        browseButton.setVisible(sqlite);
        browseButton.setManaged(sqlite);
        databaseField.setPromptText(sqlite ? "Path to .db / .sqlite file"
                : mongo ? "(optional — browse all databases)" : "database name");

        // Only overwrite the port if the user hasn't customized it
        try {
            int current = Integer.parseInt(portField.getText().trim());
            for (DatabaseType t : DatabaseType.values()) {
                if (current == t.getDefaultPort()) {
                    portField.setText(String.valueOf(type.getDefaultPort()));
                    return;
                }
            }
        } catch (NumberFormatException e) {
            portField.setText(String.valueOf(type.getDefaultPort()));
        }
    }

    private void collectInto(ConnectionProfile p) {
        p.setName(nameField.getText().trim());
        p.setType(typeCombo.getValue());
        p.setHost(hostField.getText().trim());
        try {
            p.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException e) {
            p.setPort(typeCombo.getValue().getDefaultPort());
        }
        p.setDatabase(databaseField.getText().trim());
        p.setUsername(userField.getText().trim());
        p.setPassword(passwordField.getText());
        p.setSavePassword(savePasswordCheck.isSelected());
        p.setUseSsl(sslCheck.isSelected());
    }

    private void testConnection(Button testButton) {
        ConnectionProfile temp = new ConnectionProfile();
        collectInto(temp);
        testButton.setDisable(true);
        testResultLabel.setText("Connecting…");
        testResultLabel.setStyle("-fx-text-fill: #868a91;");

        AppExecutor.run(() -> {
            try {
                ClientRegistry.connectAndVerify(temp);
                ClientRegistry.disconnect(temp);   // test client only — close it again
                Platform.runLater(() -> {
                    testResultLabel.setText("✓ Connection successful");
                    testResultLabel.setStyle("-fx-text-fill: #57965c;");
                    testButton.setDisable(false);
                });
            } catch (Exception ex) {
                ClientRegistry.disconnect(temp);
                String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                Platform.runLater(() -> {
                    testResultLabel.setText("✗ " + shorten(msg));
                    testResultLabel.setStyle("-fx-text-fill: #e05555;");
                    testButton.setDisable(false);
                });
            }
        });
    }

    private static String shorten(String s) {
        return s.length() > 90 ? s.substring(0, 90) + "…" : s;
    }
}

package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;

/**
 * DataGrip-style "Data Source" dialog: driver picker, host/port/db/user/pass,
 * test button. Uses {@link DialogTheme} so it renders with the app's dark
 * theme instead of the native OS dialog look, and groups fields into labeled
 * sections for a more polished, professional layout.
 */
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
    private final Button browseButton = new Button();
    private final javafx.scene.layout.StackPane typeIconHolder = new javafx.scene.layout.StackPane();

    public ConnectionDialog(ConnectionProfile existing) {
        this.profile = existing != null ? existing.copy() : new ConnectionProfile();
        DialogTheme.apply(this);

        setTitle(existing == null ? "New Data Source" : "Edit Data Source");
        setHeaderText(null);
        getDialogPane().getStyleClass().add("connection-dialog");
        getDialogPane().setPrefWidth(540);

        typeCombo.getItems().addAll(DatabaseType.values());
        typeCombo.getSelectionModel().select(profile.getType());
        typeCombo.setPrefWidth(280);
        typeCombo.valueProperty().addListener((obs, old, type) -> {
            applyTypeDefaults(type);
            updateTypeIcon(type);
        });

        nameField.setText(profile.getName());
        nameField.setPromptText("My PostgreSQL Server");
        hostField.setText(profile.getHost());
        portField.setText(profile.getPort() > 0
                ? String.valueOf(profile.getPort())
                : String.valueOf(profile.getType().getDefaultPort()));
        databaseField.setText(profile.getDatabase());
        userField.setText(profile.getUsername());
        passwordField.setText(profile.getPassword());
        savePasswordCheck.setSelected(profile.isSavePassword());
        sslCheck.setSelected(profile.isUseSsl());

        browseButton.setGraphic(Icons.of(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 12));
        browseButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select SQLite database file");
            File file = chooser.showOpenDialog(getDialogPane().getScene().getWindow());
            if (file != null) databaseField.setText(file.getAbsolutePath());
        });

        // ---- header: colored icon for the selected engine + name field ----
        typeIconHolder.setPrefSize(40, 40);
        typeIconHolder.getStyleClass().add("connection-dialog-icon");
        updateTypeIcon(profile.getType());

        VBox headerText = new VBox(2);
        Label headerTitle = new Label(existing == null ? "New Data Source" : "Edit Data Source");
        headerTitle.getStyleClass().add("connection-dialog-title");
        Label headerSubtitle = new Label("Connect to a relational database or MongoDB");
        headerSubtitle.getStyleClass().add("connection-dialog-subtitle");
        headerText.getChildren().addAll(headerTitle, headerSubtitle);

        HBox header = new HBox(12, typeIconHolder, headerText);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.getStyleClass().add("connection-dialog-header");

        // ---- "General" section ----
        GridPane generalGrid = sectionGrid();
        int row = 0;
        generalGrid.add(fieldLabel("Name:"), 0, row);
        generalGrid.add(nameField, 1, row++, 2, 1);
        generalGrid.add(fieldLabel("Type:"), 0, row);
        generalGrid.add(typeCombo, 1, row++, 2, 1);

        // ---- "Connection" section ----
        GridPane connGrid = sectionGrid();
        row = 0;
        connGrid.add(fieldLabel("Host:"), 0, row);
        connGrid.add(hostField, 1, row++, 2, 1);
        connGrid.add(fieldLabel("Port:"), 0, row);
        connGrid.add(portField, 1, row++, 2, 1);
        connGrid.add(fieldLabel("Database:"), 0, row);
        connGrid.add(databaseField, 1, row);
        connGrid.add(browseButton, 2, row++);
        connGrid.add(fieldLabel("User:"), 0, row);
        connGrid.add(userField, 1, row++, 2, 1);
        connGrid.add(fieldLabel("Password:"), 0, row);
        connGrid.add(passwordField, 1, row++, 2, 1);
        connGrid.add(new HBox(20, savePasswordCheck, sslCheck), 1, row++, 2, 1);

        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(hostField, Priority.ALWAYS);
        GridPane.setHgrow(databaseField, Priority.ALWAYS);
        GridPane.setHgrow(userField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        Button testButton = new Button("Test Connection");
        testButton.getStyleClass().add("run-button");
        testButton.setGraphic(Icons.of(FontAwesomeSolid.PLUG, "#57965c", 12));
        testButton.setOnAction(e -> testConnection(testButton));
        testResultLabel.getStyleClass().add("connection-test-result");
        Region testSpacer = new Region();
        HBox.setHgrow(testSpacer, Priority.ALWAYS);
        HBox testBox = new HBox(10, testButton, testResultLabel);
        testBox.setAlignment(Pos.CENTER_LEFT);
        testBox.setPadding(new Insets(6, 4, 0, 4));

        VBox body = new VBox(4,
                sectionLabel("General"), generalGrid,
                new Separator(), sectionLabel("Connection"), connGrid,
                testBox);
        body.setPadding(new Insets(4, 20, 16, 20));

        VBox root = new VBox(header, body);
        getDialogPane().setContent(root);
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

    private static GridPane sectionGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(8, 0, 8, 0));
        return grid;
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setMinWidth(90);
        l.getStyleClass().add("connection-field-label");
        return l;
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("connection-section-label");
        return l;
    }

    private void updateTypeIcon(DatabaseType type) {
        String color = switch (type) {
            case POSTGRESQL -> "#4a88c7";
            case MYSQL, MARIADB -> "#e0a44c";
            case SQLSERVER -> "#c77dbb";
            case ORACLE -> "#e05555";
            case SQLITE -> "#868a91";
            case MONGODB -> "#57965c";
        };
        typeIconHolder.getChildren().setAll(Icons.of(FontAwesomeSolid.DATABASE, color, 20));
        typeIconHolder.setStyle("-fx-background-color: " + color + "22;");
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
        testResultLabel.getStyleClass().removeAll("test-ok", "test-fail");

        AppExecutor.run(() -> {
            try {
                ClientRegistry.connectAndVerify(temp);
                ClientRegistry.disconnect(temp);   // test client only — close it again
                Platform.runLater(() -> {
                    testResultLabel.setText("✓ Connection successful");
                    testResultLabel.getStyleClass().add("test-ok");
                    testButton.setDisable(false);
                });
            } catch (Exception ex) {
                ClientRegistry.disconnect(temp);
                String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                Platform.runLater(() -> {
                    testResultLabel.setText("✗ " + shorten(msg));
                    testResultLabel.getStyleClass().add("test-fail");
                    testButton.setDisable(false);
                });
            }
        });
    }

    private static String shorten(String s) {
        return s.length() > 90 ? s.substring(0, 90) + "…" : s;
    }
}

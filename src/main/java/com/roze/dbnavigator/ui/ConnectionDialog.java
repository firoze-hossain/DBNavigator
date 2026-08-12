package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
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
    private final Label sqlitePathHint = new Label();
    private final Button browseButton = new Button();
    private final javafx.scene.layout.StackPane typeIconHolder = new javafx.scene.layout.StackPane();

    // MongoDB-only fields — mirrors DataGrip's own "default / Atlas SRV /
    // URL only" toggle plus its Replica set and Read preference fields.
    private final ToggleButton mongoDefaultToggle = new ToggleButton("default");
    private final ToggleButton mongoSrvToggle = new ToggleButton("Atlas SRV");
    private final ToggleButton mongoUrlOnlyToggle = new ToggleButton("URL only");
    private final ToggleGroup mongoConnTypeGroup = new ToggleGroup();
    private final TextField replicaSetField = new TextField();
    private final ComboBox<String> readPreferenceCombo = new ComboBox<>();
    private final TextField mongoUrlField = new TextField();
    private final Label mongoUrlLabel = fieldLabel("URL:");
    private final Label mongoUrlNote = new Label("Overrides settings above");
    private java.util.List<javafx.scene.Node> mongoOnlyNodes;
    private java.util.List<Control> mongoOverridableFields;

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

        // ---- MongoDB-only fields ----
        mongoDefaultToggle.setToggleGroup(mongoConnTypeGroup);
        mongoSrvToggle.setToggleGroup(mongoConnTypeGroup);
        mongoUrlOnlyToggle.setToggleGroup(mongoConnTypeGroup);
        mongoDefaultToggle.getStyleClass().add("mongo-conn-type-toggle");
        mongoSrvToggle.getStyleClass().add("mongo-conn-type-toggle");
        mongoUrlOnlyToggle.getStyleClass().add("mongo-conn-type-toggle");
        mongoDefaultToggle.setUserData(ConnectionProfile.MongoConnectionType.DEFAULT);
        mongoSrvToggle.setUserData(ConnectionProfile.MongoConnectionType.SRV);
        mongoUrlOnlyToggle.setUserData(ConnectionProfile.MongoConnectionType.URL_ONLY);
        ToggleButton initialToggle = switch (profile.getMongoConnectionType()) {
            case SRV -> mongoSrvToggle;
            case URL_ONLY -> mongoUrlOnlyToggle;
            case DEFAULT -> mongoDefaultToggle;
        };
        mongoConnTypeGroup.selectToggle(initialToggle);
        // A segmented control needs at least one selected at all times — a
        // plain ToggleGroup otherwise lets the active button be clicked back
        // off, leaving nothing selected.
        mongoConnTypeGroup.selectedToggleProperty().addListener((obs, old, current) -> {
            if (current == null) mongoConnTypeGroup.selectToggle(old);
            else updateMongoFieldsVisibility();
        });

        replicaSetField.setText(profile.getReplicaSet());
        replicaSetField.setPromptText("(optional)");
        readPreferenceCombo.getItems().addAll(
                "", "primary", "primaryPreferred", "secondary", "secondaryPreferred", "nearest");
        readPreferenceCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(String s) { return s == null || s.isEmpty() ? "Default" : s; }
            @Override public String fromString(String s) { return s; }
        });
        readPreferenceCombo.setValue(profile.getReadPreference());
        mongoUrlField.setText(profile.getMongoUrlOverride());
        mongoUrlField.setPromptText("mongodb+srv://user:pass@cluster.example.mongodb.net/mydb");
        mongoUrlNote.getStyleClass().add("connection-field-hint");

        browseButton.setGraphic(Icons.of(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 12));
        browseButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select SQLite database file");
            File file = chooser.showOpenDialog(getDialogPane().getScene().getWindow());
            if (file != null) databaseField.setText(file.getAbsolutePath());
        });

        // Live preview so a bare/relative filename doesn't silently resolve
        // to the wrong file — shown as the user types, before they even
        // reach Test Connection.
        sqlitePathHint.getStyleClass().add("connection-sqlite-hint");
        sqlitePathHint.setWrapText(true);
        sqlitePathHint.setManaged(false);
        sqlitePathHint.setVisible(false);
        databaseField.textProperty().addListener((obs, old, val) -> updateSqlitePathHint());
        typeCombo.valueProperty().addListener((obs, old, type) -> updateSqlitePathHint());

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
        Label mongoConnTypeLabel = fieldLabel("Connection type:");
        HBox mongoConnTypeBox = new HBox(6, mongoDefaultToggle, mongoSrvToggle, mongoUrlOnlyToggle);
        connGrid.add(mongoConnTypeLabel, 0, row);
        connGrid.add(mongoConnTypeBox, 1, row++, 2, 1);

        connGrid.add(fieldLabel("Host:"), 0, row);
        connGrid.add(hostField, 1, row++, 2, 1);
        connGrid.add(fieldLabel("Port:"), 0, row);
        connGrid.add(portField, 1, row++, 2, 1);
        connGrid.add(fieldLabel("Database:"), 0, row);
        connGrid.add(databaseField, 1, row);
        connGrid.add(browseButton, 2, row++);
        connGrid.add(sqlitePathHint, 1, row++, 2, 1);
        connGrid.add(fieldLabel("User:"), 0, row);
        connGrid.add(userField, 1, row++, 2, 1);
        connGrid.add(fieldLabel("Password:"), 0, row);
        connGrid.add(passwordField, 1, row++, 2, 1);
        connGrid.add(new HBox(20, savePasswordCheck, sslCheck), 1, row++, 2, 1);

        Label replicaSetLabel = fieldLabel("Replica set:");
        connGrid.add(replicaSetLabel, 0, row);
        connGrid.add(replicaSetField, 1, row++, 2, 1);
        Label readPreferenceLabel = fieldLabel("Read preference:");
        connGrid.add(readPreferenceLabel, 0, row);
        connGrid.add(readPreferenceCombo, 1, row++, 2, 1);
        connGrid.add(mongoUrlLabel, 0, row);
        connGrid.add(mongoUrlField, 1, row, 2, 1);
        row++;
        connGrid.add(mongoUrlNote, 1, row++, 2, 1);

        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(hostField, Priority.ALWAYS);
        GridPane.setHgrow(databaseField, Priority.ALWAYS);
        GridPane.setHgrow(userField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);
        GridPane.setHgrow(sqlitePathHint, Priority.ALWAYS);
        GridPane.setHgrow(replicaSetField, Priority.ALWAYS);
        GridPane.setHgrow(readPreferenceCombo, Priority.ALWAYS);
        GridPane.setHgrow(mongoUrlField, Priority.ALWAYS);
        readPreferenceCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(sqlitePathHint, true);
        sqlitePathHint.setMaxWidth(Double.MAX_VALUE);
        sqlitePathHint.setMinWidth(0);   // without this, Label sizes to its unwrapped text and wrapText is ignored

        // Everything above tagged as mongo-only toggles together, in one
        // place, instead of scattering visible/managed calls across every
        // row individually.
        mongoOnlyNodes = java.util.List.of(
                mongoConnTypeLabel, mongoConnTypeBox, replicaSetLabel, replicaSetField,
                readPreferenceLabel, readPreferenceCombo, mongoUrlLabel, mongoUrlField, mongoUrlNote);
        // In URL-only mode these are ignored (the pasted URL wins instead),
        // so they're disabled rather than hidden — same as DataGrip greys
        // them out instead of removing them, since switching back to
        // "default" should find them exactly as they were left.
        mongoOverridableFields = java.util.List.of(
                hostField, portField, databaseField, userField, passwordField,
                replicaSetField, readPreferenceCombo);

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

        // This whole class of mistake — a typo'd or half-remembered filename
        // quietly becoming a brand-new empty database instead of failing —
        // is exactly what a warning label after the fact keeps failing to
        // prevent. Blocking OK itself, right when it's about to happen, is
        // the only point that actually stops it: an event filter (not the
        // result converter, which only runs once the dialog is already
        // closing) can still consume() the click and keep the dialog open.
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (typeCombo.getValue() != DatabaseType.SQLITE) return;
            String resolved = ConnectionProfile.resolveSqlitePath(resolveDatabaseField());
            if (resolved == null || resolved.isBlank() || new File(resolved).isFile()) return;

            Alert confirm = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.CONFIRMATION,
                    "No existing file was found at:\n\n" + resolved
                    + "\n\nContinuing creates a brand-new, empty database there — it will NOT contain "
                    + "any tables from another file that happens to share this name.\n\n"
                    + "If you meant to open a file you already have, click Cancel and use Browse instead.",
                    ButtonType.YES, ButtonType.NO));
            confirm.setHeaderText("Create a new, empty database?");
            confirm.initOwner(getDialogPane().getScene().getWindow());
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                event.consume();
            }
        });

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
        databaseField.setPromptText(sqlite ? "Path to .db / .sqlite file (not a jdbc: URL)"
                : mongo ? "(optional — browse all databases)" : "database name");
        updateSqlitePathHint();
        updateMongoFieldsVisibility();

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

    /**
     * Shows the MongoDB-only rows (connection-type toggle, replica set, read
     * preference, URL override) only for a MongoDB connection, and — within
     * that — greys out every field the "URL only" toggle position makes
     * irrelevant, matching DataGrip's own behavior for that same toggle.
     */
    private void updateMongoFieldsVisibility() {
        if (mongoOnlyNodes == null) return;   // fields not laid out yet — selectToggle() below fires this early
        boolean mongo = typeCombo.getValue() == DatabaseType.MONGODB;
        for (javafx.scene.Node node : mongoOnlyNodes) {
            node.setVisible(mongo);
            node.setManaged(mongo);
        }
        if (!mongo) return;

        boolean urlOnly = mongoConnTypeGroup.getSelectedToggle() == mongoUrlOnlyToggle;
        mongoUrlLabel.setVisible(urlOnly);
        mongoUrlLabel.setManaged(urlOnly);
        mongoUrlField.setVisible(urlOnly);
        mongoUrlField.setManaged(urlOnly);
        mongoUrlNote.setVisible(urlOnly);
        mongoUrlNote.setManaged(urlOnly);
        for (Control field : mongoOverridableFields) {
            field.setDisable(urlOnly);
        }
        boolean srv = mongoConnTypeGroup.getSelectedToggle() == mongoSrvToggle;
        // An SRV record supplies each host's own port, so a manually entered
        // one here would be misleading as well as unused.
        portField.setDisable(urlOnly || srv);
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
        p.setDatabase(resolveDatabaseField());
        p.setUsername(userField.getText().trim());
        p.setPassword(passwordField.getText());
        p.setSavePassword(savePasswordCheck.isSelected());
        p.setUseSsl(sslCheck.isSelected());
        if (typeCombo.getValue() == DatabaseType.MONGODB) {
            Toggle selected = mongoConnTypeGroup.getSelectedToggle();
            p.setMongoConnectionType(selected == null
                    ? ConnectionProfile.MongoConnectionType.DEFAULT
                    : (ConnectionProfile.MongoConnectionType) selected.getUserData());
            p.setReplicaSet(replicaSetField.getText().trim());
            p.setReadPreference(readPreferenceCombo.getValue());
            p.setMongoUrlOverride(mongoUrlField.getText().trim());
        }
    }

    /**
     * SQLite's driver never rejects a path — if the file doesn't exist yet it
     * silently creates a brand-new, empty database instead of failing. Combined
     * with a bare relative filename like "identifier.sqlite", that's what
     * produced several different, empty files with the same name during
     * testing: relative paths used to resolve against wherever the app's
     * process happened to be launched from, which isn't stable across runs.
     * The text here is now saved exactly as typed/browsed (minus an
     * accidental jdbc: prefix) — actual resolution to one stable file, for
     * both relative and absolute input, happens centrally in
     * {@link ConnectionProfile#resolveSqlitePath}, which every other part of
     * the app (this dialog's hint, the tree, the real JDBC connection) also
     * goes through, so they can never disagree about which file is meant.
     */
    private String resolveDatabaseField() {
        String text = databaseField.getText().trim();
        if (typeCombo.getValue() != DatabaseType.SQLITE || text.isEmpty()) return text;
        return stripJdbcSqlitePrefix(text);
    }

    /**
     * Defensive: this field wants a plain file path, but it's an easy, real
     * mistake to paste a full "jdbc:sqlite:..." URL into it out of habit from
     * other tools. Left alone, the app then adds its own "jdbc:sqlite:" on
     * top when building the real connection URL, producing a doubly-prefixed
     * path (literally containing colons) that no real file will ever exist
     * at. Stripping a leading scheme here means pasting either form works.
     */
    private static String stripJdbcSqlitePrefix(String text) {
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("jdbc:sqlite://")) return text.substring("jdbc:sqlite://".length());
        if (lower.startsWith("jdbc:sqlite:")) return text.substring("jdbc:sqlite:".length());
        return text;
    }

    /**
     * Shows, as the user types, exactly what file a SQLite entry will
     * resolve to and whether it already exists there — the same ambiguity
     * that made "Test Connection" misleading in the first place, surfaced
     * before the user ever clicks it. With the field still empty, shows
     * static guidance instead, since that's the point where "just a
     * filename" is usually assumed to mean more than it does.
     */
    private void updateSqlitePathHint() {
        if (typeCombo.getValue() != DatabaseType.SQLITE) {
            sqlitePathHint.setVisible(false);
            sqlitePathHint.setManaged(false);
            return;
        }
        String text = databaseField.getText().trim();
        if (text.isEmpty()) {
            sqlitePathHint.setText("A bare filename is saved under " + ConnectionProfile.sqliteHomeDir()
                    + " — use Browse to pick an existing file elsewhere.");
            sqlitePathHint.getStyleClass().removeAll("hint-ok", "hint-warn");
            sqlitePathHint.getStyleClass().add("hint-ok");
            sqlitePathHint.setVisible(true);
            sqlitePathHint.setManaged(true);
            return;
        }
        String resolved = ConnectionProfile.resolveSqlitePath(stripJdbcSqlitePrefix(text));
        boolean exists = new File(resolved).isFile();
        sqlitePathHint.setText((exists ? "✓ " : "⚠ new file — ") + resolved);
        sqlitePathHint.getStyleClass().removeAll("hint-ok", "hint-warn");
        sqlitePathHint.getStyleClass().add(exists ? "hint-ok" : "hint-warn");
        sqlitePathHint.setVisible(true);
        sqlitePathHint.setManaged(true);
    }

    private void testConnection(Button testButton) {
        ConnectionProfile temp = new ConnectionProfile();
        collectInto(temp);
        testButton.setDisable(true);
        testResultLabel.setText("Connecting…");
        testResultLabel.getStyleClass().removeAll("test-ok", "test-fail", "test-warn");

        // Checked before connecting: SQLite creates this file itself the
        // instant a connection is opened, so this is the only point at which
        // "did the file already exist" can still be answered honestly.
        boolean sqliteFileExistedBefore = temp.getType() != DatabaseType.SQLITE
                || new File(temp.resolvedSqlitePath()).isFile();

        AppExecutor.run(() -> {
            try {
                ClientRegistry.connectAndVerify(temp);
                ClientRegistry.disconnect(temp);   // test client only — close it again
                Platform.runLater(() -> {
                    if (sqliteFileExistedBefore) {
                        testResultLabel.setText("✓ Connection successful");
                        testResultLabel.getStyleClass().add("test-ok");
                    } else {
                        testResultLabel.setText(
                                "⚠ New, empty database created at " + temp.resolvedSqlitePath());
                        testResultLabel.getStyleClass().add("test-warn");
                    }
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

package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Settings panel for Tools > SSH Configurations.
 * Matches DataGrip screenshots media_179172611669_96d0490a.png and media_179172626162_7c6b0408.png.
 */
public class SshConfigurationsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;
    private final ObservableList<AppSettingsStore.SshConfigurationEntry> workingList = FXCollections.observableArrayList();

    public SshConfigurationsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        // Load copy of existing configurations
        workingList.clear();
        for (AppSettingsStore.SshConfigurationEntry entry : settings.getSshConfigurations().getConfigurations()) {
            workingList.add(entry.copy());
        }
        inputs.put("tools_ssh_configurations_list", workingList);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: transparent;");

        // Top bar with Revert changes
        Hyperlink revertLink = new Hyperlink("Revert changes");
        revertLink.setStyle("-fx-text-fill: -accent; -fx-font-size: 12px; -fx-underline: false;");
        HBox topBar = new HBox(revertLink);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(4, 16, 6, 16));

        // Master-Detail SplitPane
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");

        // --- Left side: Master list & toolbar ---
        VBox leftPane = new VBox(6);
        leftPane.setPadding(new Insets(4, 8, 8, 8));
        leftPane.setMinWidth(220);
        leftPane.setPrefWidth(260);

        Button addBtn = new Button("+");
        addBtn.setTooltip(new Tooltip("Add SSH configuration (Alt+Insert)"));
        addBtn.setStyle("-fx-font-weight: bold; -fx-padding: 3 8 3 8;");

        Button removeBtn = new Button("-");
        removeBtn.setTooltip(new Tooltip("Remove SSH configuration (Delete)"));
        removeBtn.setStyle("-fx-font-weight: bold; -fx-padding: 3 8 3 8;");

        Button copyBtn = new Button("⧉");
        copyBtn.setTooltip(new Tooltip("Duplicate configuration"));
        copyBtn.setStyle("-fx-padding: 3 7 3 7;");

        Button renameBtn = new Button("✏");
        renameBtn.setTooltip(new Tooltip("Rename configuration"));
        renameBtn.setStyle("-fx-padding: 3 7 3 7;");

        HBox toolbar = new HBox(4, addBtn, removeBtn, copyBtn, renameBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        ListView<AppSettingsStore.SshConfigurationEntry> listView = new ListView<>(workingList);
        listView.setStyle("-fx-background-color: -control-bg; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        Label emptyLabel = new Label("Nothing to show");
        emptyLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");
        listView.setPlaceholder(emptyLabel);
        VBox.setVgrow(listView, Priority.ALWAYS);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AppSettingsStore.SshConfigurationEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                    setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
                }
            }
        });

        leftPane.getChildren().addAll(toolbar, listView);

        // --- Right side: Detail view ---
        StackPane detailContainer = new StackPane();
        detailContainer.setPadding(new Insets(4, 16, 16, 16));

        Label noSelectionLabel = new Label("Add SSH configuration");
        noSelectionLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        detailContainer.getChildren().add(noSelectionLabel);

        // Selection Listener
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            removeBtn.setDisable(!hasSelection);
            copyBtn.setDisable(!hasSelection);
            renameBtn.setDisable(!hasSelection);

            if (newVal != null) {
                detailContainer.getChildren().setAll(buildDetailForm(newVal, listView));
            } else {
                detailContainer.getChildren().setAll(noSelectionLabel);
            }
        });

        // Add action
        addBtn.setOnAction(e -> {
            AppSettingsStore.SshConfigurationEntry newEntry = new AppSettingsStore.SshConfigurationEntry(
                    "localhost", "localhost", 22, System.getProperty("user.name", "root"), "Password"
            );
            workingList.add(newEntry);
            listView.getSelectionModel().select(newEntry);
        });

        // Remove action
        removeBtn.setDisable(true);
        removeBtn.setOnAction(e -> {
            AppSettingsStore.SshConfigurationEntry selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                int idx = workingList.indexOf(selected);
                workingList.remove(selected);
                if (!workingList.isEmpty()) {
                    int next = Math.min(idx, workingList.size() - 1);
                    listView.getSelectionModel().select(next);
                }
            }
        });

        // Duplicate action
        copyBtn.setDisable(true);
        copyBtn.setOnAction(e -> {
            AppSettingsStore.SshConfigurationEntry selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                AppSettingsStore.SshConfigurationEntry copy = selected.copy();
                copy.setId(UUID.randomUUID().toString());
                copy.setName(selected.getName() + " copy");
                workingList.add(copy);
                listView.getSelectionModel().select(copy);
            }
        });

        // Rename action
        renameBtn.setDisable(true);
        renameBtn.setOnAction(e -> {
            AppSettingsStore.SshConfigurationEntry selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                TextInputDialog dialog = new TextInputDialog(selected.getName());
                dialog.setTitle("Rename SSH Configuration");
                dialog.setHeaderText("Specify configuration name:");
                dialog.setContentText("Name:");
                dialog.showAndWait().ifPresent(name -> {
                    selected.setName(name);
                    listView.refresh();
                });
            }
        });

        // Revert action
        revertLink.setOnAction(e -> {
            workingList.clear();
            for (AppSettingsStore.SshConfigurationEntry entry : settings.getSshConfigurations().getConfigurations()) {
                workingList.add(entry.copy());
            }
            if (!workingList.isEmpty()) {
                listView.getSelectionModel().select(0);
            }
        });

        // Select first if available
        if (!workingList.isEmpty()) {
            listView.getSelectionModel().select(0);
        }

        splitPane.getItems().addAll(leftPane, detailContainer);
        SplitPane.setResizableWithParent(leftPane, false);
        splitPane.setDividerPositions(0.32);

        root.setTop(topBar);
        root.setCenter(splitPane);
        return root;
    }

    private Node buildDetailForm(AppSettingsStore.SshConfigurationEntry entry, ListView<AppSettingsStore.SshConfigurationEntry> listView) {
        VBox form = new VBox(12);
        form.setStyle("-fx-background-color: transparent;");

        // Visible only for this project
        CheckBox projectOnlyCheck = new CheckBox("Visible only for this project");
        projectOnlyCheck.setSelected(entry.isVisibleOnlyForThisProject());
        projectOnlyCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        projectOnlyCheck.selectedProperty().addListener((obs, o, n) -> entry.setVisibleOnlyForThisProject(n));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Host and Port
        Label hostLbl = new Label("Host:");
        hostLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        TextField hostField = new TextField(entry.getHost());
        hostField.setPrefWidth(240);
        hostField.setStyle("-fx-background-color: -control-bg; -fx-text-fill: -text; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        hostField.textProperty().addListener((obs, o, n) -> {
            entry.setHost(n);
            listView.refresh();
        });

        Label portLbl = new Label("Port:");
        portLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        Spinner<Integer> portSpinner = new Spinner<>(1, 65535, entry.getPort());
        portSpinner.setEditable(true);
        portSpinner.setPrefWidth(90);
        portSpinner.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                entry.setPort(n);
                listView.refresh();
            }
        });

        HBox hostPortBox = new HBox(12, hostField, portLbl, portSpinner);
        hostPortBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(hostLbl, 0, 0);
        grid.add(hostPortBox, 1, 0);

        // Username
        Label userLbl = new Label("Username:");
        userLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        TextField userField = new TextField(entry.getUsername());
        userField.setPrefWidth(240);
        userField.setStyle("-fx-background-color: -control-bg; -fx-text-fill: -text; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        userField.textProperty().addListener((obs, o, n) -> {
            entry.setUsername(n);
            listView.refresh();
        });

        grid.add(userLbl, 0, 1);
        grid.add(userField, 1, 1);

        // Authentication type
        Label authLbl = new Label("Authentication type:");
        authLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        ComboBox<String> authCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Password", "Key pair", "OpenSSH config and agent", "OpenSSH config"
        ));
        authCombo.setValue(entry.getAuthType());
        authCombo.setStyle("-fx-background-color: -control-bg; -fx-border-color: -border-color; -fx-border-radius: 4px;");

        grid.add(authLbl, 0, 2);
        grid.add(authCombo, 1, 2);

        // Dynamic Auth Pane
        StackPane authDetailsPane = new StackPane();

        // Password row
        Label passLbl = new Label("Password:");
        passLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        PasswordField passField = new PasswordField();
        passField.setText(entry.getPassword());
        passField.setPrefWidth(240);
        passField.setStyle("-fx-background-color: -control-bg; -fx-text-fill: -text; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        passField.textProperty().addListener((obs, o, n) -> entry.setPassword(n));

        CheckBox savePassCheck = new CheckBox("Save password");
        savePassCheck.setSelected(entry.isSavePassword());
        savePassCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        savePassCheck.selectedProperty().addListener((obs, o, n) -> entry.setSavePassword(n));

        HBox passBox = new HBox(12, passField, savePassCheck);
        passBox.setAlignment(Pos.CENTER_LEFT);

        GridPane passGrid = new GridPane();
        passGrid.setHgap(10);
        passGrid.add(passLbl, 0, 0);
        passGrid.add(passBox, 1, 0);

        // Key pair grid
        GridPane keyPairGrid = new GridPane();
        keyPairGrid.setHgap(10);
        keyPairGrid.setVgap(8);

        Label keyFileLbl = new Label("Private key file:");
        keyFileLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        TextField keyFileField = new TextField(entry.getPrivateKeyPath());
        keyFileField.setPrefWidth(240);
        keyFileField.textProperty().addListener((obs, o, n) -> entry.setPrivateKeyPath(n));

        Button keyBrowseBtn = new Button("📁");
        keyBrowseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Private Key");
            File f = chooser.showOpenDialog(keyBrowseBtn.getScene().getWindow());
            if (f != null) keyFileField.setText(f.getAbsolutePath());
        });
        HBox keyFileBox = new HBox(6, keyFileField, keyBrowseBtn);

        Label passphLbl = new Label("Passphrase:");
        passphLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        PasswordField passphField = new PasswordField();
        passphField.setText(entry.getPassphrase());
        passphField.setPrefWidth(240);
        passphField.textProperty().addListener((obs, o, n) -> entry.setPassphrase(n));

        keyPairGrid.add(keyFileLbl, 0, 0);
        keyPairGrid.add(keyFileBox, 1, 0);
        keyPairGrid.add(passphLbl, 0, 1);
        keyPairGrid.add(passphField, 1, 1);

        // OpenSSH info label
        Label opensshLbl = new Label("Host and credentials will be resolved using ~/.ssh/config and ssh-agent.");
        opensshLbl.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

        Runnable updateAuthPane = () -> {
            String selected = authCombo.getValue();
            entry.setAuthType(selected);
            listView.refresh();
            if ("Password".equals(selected)) {
                authDetailsPane.getChildren().setAll(passGrid);
            } else if ("Key pair".equals(selected)) {
                authDetailsPane.getChildren().setAll(keyPairGrid);
            } else {
                authDetailsPane.getChildren().setAll(opensshLbl);
            }
        };
        authCombo.valueProperty().addListener((obs, o, n) -> updateAuthPane.run());
        updateAuthPane.run();

        // Parse ~/.ssh/config check
        CheckBox parseConfigCheck = new CheckBox("Parse config file ~/.ssh/config");
        parseConfigCheck.setSelected(entry.isParseConfigFile());
        parseConfigCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        parseConfigCheck.selectedProperty().addListener((obs, o, n) -> entry.setParseConfigFile(n));

        // Test Connection button
        Button testConnBtn = new Button("Test Connection");
        testConnBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 12 4 12;");
        testConnBtn.setOnAction(e -> testSshConnection(entry));

        // --- Collapsible: Connection Parameters ---
        TitledPane connParamsPane = new TitledPane();
        connParamsPane.setText("Connection Parameters");
        connParamsPane.setExpanded(true);

        GridPane connParamsGrid = new GridPane();
        connParamsGrid.setHgap(10);
        connParamsGrid.setVgap(10);
        connParamsGrid.setPadding(new Insets(8));

        CheckBox keepAliveCheck = new CheckBox("Send keep-alive messages every");
        keepAliveCheck.setSelected(entry.isSendKeepAlive());
        keepAliveCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        keepAliveCheck.selectedProperty().addListener((obs, o, n) -> entry.setSendKeepAlive(n));

        Spinner<Integer> keepAliveSpinner = new Spinner<>(1, 3600, entry.getKeepAliveIntervalSec());
        keepAliveSpinner.setEditable(true);
        keepAliveSpinner.setPrefWidth(90);
        keepAliveSpinner.disableProperty().bind(keepAliveCheck.selectedProperty().not());
        keepAliveSpinner.valueProperty().addListener((obs, o, n) -> {
            if (n != null) entry.setKeepAliveIntervalSec(n);
        });

        Label secondsLbl = new Label("seconds");
        secondsLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        HBox keepAliveBox = new HBox(8, keepAliveCheck, keepAliveSpinner, secondsLbl);
        keepAliveBox.setAlignment(Pos.CENTER_LEFT);

        Label strictLbl = new Label("Strict host key checking:");
        strictLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> strictCombo = new ComboBox<>(FXCollections.observableArrayList("Ask", "Yes", "No"));
        strictCombo.setValue(entry.getStrictHostKeyMode());
        strictCombo.setStyle("-fx-background-color: -control-bg; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        strictCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                entry.setStrictHostKeyMode(n);
                entry.setStrictHostKeyChecking(!"No".equals(n));
            }
        });

        HBox strictBox = new HBox(10, strictLbl, strictCombo);
        strictBox.setAlignment(Pos.CENTER_LEFT);

        CheckBox hashHostsCheck = new CheckBox("Hash hosts in known_hosts file");
        hashHostsCheck.setSelected(entry.isHashHostsInKnownHosts());
        hashHostsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        hashHostsCheck.selectedProperty().addListener((obs, o, n) -> entry.setHashHostsInKnownHosts(n));

        connParamsGrid.add(keepAliveBox, 0, 0);
        connParamsGrid.add(strictBox, 0, 1);
        connParamsGrid.add(hashHostsCheck, 0, 2);
        connParamsPane.setContent(connParamsGrid);

        // --- Collapsible: HTTP/SOCKS Proxy ---
        TitledPane proxyPane = new TitledPane();
        proxyPane.setText("HTTP/SOCKS Proxy");
        proxyPane.setExpanded(false);

        GridPane proxyGrid = new GridPane();
        proxyGrid.setHgap(10);
        proxyGrid.setVgap(10);
        proxyGrid.setPadding(new Insets(8));

        CheckBox useGlobalProxyCheck = new CheckBox("Use global IDE proxy settings");
        useGlobalProxyCheck.setSelected(entry.isUseGlobalProxy());
        useGlobalProxyCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        useGlobalProxyCheck.selectedProperty().addListener((obs, o, n) -> entry.setUseGlobalProxy(n));

        Hyperlink configureProxyLink = new Hyperlink("Configure...");
        configureProxyLink.setStyle("-fx-text-fill: -accent; -fx-font-size: 12px; -fx-underline: false;");

        HBox globalProxyRow = new HBox(12, useGlobalProxyCheck, configureProxyLink);
        globalProxyRow.setAlignment(Pos.CENTER_LEFT);

        Label proxyTypeLbl = new Label("Proxy:");
        proxyTypeLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        ComboBox<String> proxyTypeCombo = new ComboBox<>(FXCollections.observableArrayList("No proxy", "HTTP", "SOCKS"));
        proxyTypeCombo.setValue(entry.getProxyType());
        proxyTypeCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) entry.setProxyType(n);
        });

        Label proxyHostLbl = new Label("Hostname:");
        proxyHostLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        TextField proxyHostField = new TextField(entry.getProxyHost());
        proxyHostField.textProperty().addListener((obs, o, n) -> entry.setProxyHost(n));

        Label proxyPortLbl = new Label("Port:");
        proxyPortLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        Spinner<Integer> proxyPortSpinner = new Spinner<>(1, 65535, entry.getProxyPort());
        proxyPortSpinner.setEditable(true);
        proxyPortSpinner.setPrefWidth(90);
        proxyPortSpinner.valueProperty().addListener((obs, o, n) -> {
            if (n != null) entry.setProxyPort(n);
        });

        HBox proxyHostPortBox = new HBox(10, proxyHostField, proxyPortLbl, proxyPortSpinner);
        proxyHostPortBox.setAlignment(Pos.CENTER_LEFT);

        Label proxyAuthLbl = new Label("Authentication:");
        proxyAuthLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        ComboBox<String> proxyAuthCombo = new ComboBox<>(FXCollections.observableArrayList("No authentication", "Password"));
        proxyAuthCombo.setValue(entry.getProxyAuthType());
        proxyAuthCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) entry.setProxyAuthType(n);
        });

        Label proxyUserLbl = new Label("User:");
        proxyUserLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        TextField proxyUserField = new TextField(entry.getProxyUser());
        proxyUserField.textProperty().addListener((obs, o, n) -> entry.setProxyUser(n));

        Label proxyPassLbl = new Label("Password:");
        proxyPassLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        PasswordField proxyPassField = new PasswordField();
        proxyPassField.setText(entry.getProxyPassword());
        proxyPassField.textProperty().addListener((obs, o, n) -> entry.setProxyPassword(n));

        proxyGrid.add(globalProxyRow, 0, 0, 2, 1);
        proxyGrid.add(proxyTypeLbl, 0, 1);
        proxyGrid.add(proxyTypeCombo, 1, 1);
        proxyGrid.add(proxyHostLbl, 0, 2);
        proxyGrid.add(proxyHostPortBox, 1, 2);
        proxyGrid.add(proxyAuthLbl, 0, 3);
        proxyGrid.add(proxyAuthCombo, 1, 3);
        proxyGrid.add(proxyUserLbl, 0, 4);
        proxyGrid.add(proxyUserField, 1, 4);
        proxyGrid.add(proxyPassLbl, 0, 5);
        proxyGrid.add(proxyPassField, 1, 5);

        proxyPane.setContent(proxyGrid);

        form.getChildren().addAll(
                projectOnlyCheck,
                grid,
                authDetailsPane,
                parseConfigCheck,
                testConnBtn,
                connParamsPane,
                proxyPane
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private void testSshConnection(AppSettingsStore.SshConfigurationEntry entry) {
        new Thread(() -> {
            boolean success = false;
            String errorMsg = null;
            long start = System.currentTimeMillis();

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(entry.getHost(), entry.getPort()), 3000);
                success = true;
            } catch (Exception ex) {
                errorMsg = ex.getMessage();
            }

            long elapsed = System.currentTimeMillis() - start;
            boolean finalSuccess = success;
            String finalError = errorMsg;

            Platform.runLater(() -> {
                Alert alert = new Alert(finalSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                alert.setTitle("Test SSH Connection");
                if (finalSuccess) {
                    alert.setHeaderText("Connection Successful");
                    alert.setContentText(String.format("Successfully connected to %s:%d in %d ms.",
                            entry.getHost(), entry.getPort(), elapsed));
                } else {
                    alert.setHeaderText("Connection Failed");
                    alert.setContentText(String.format("Failed to connect to %s:%d:\n%s",
                            entry.getHost(), entry.getPort(), finalError != null ? finalError : "Timeout / Host unreachable"));
                }
                alert.showAndWait();
            });
        }).start();
    }
}

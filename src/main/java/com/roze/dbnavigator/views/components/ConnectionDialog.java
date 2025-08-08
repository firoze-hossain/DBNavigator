package com.roze.dbnavigator.views.components;

import com.roze.dbnavigator.models.ConnectionProfile;
import com.roze.dbnavigator.services.ConnectionService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionDialog extends Dialog<ConnectionProfile> {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionDialog.class);
    private final TextField nameField = new TextField();
    private final ComboBox<ConnectionProfile.DatabaseType> dbTypeCombo = new ComboBox<>();
    private final TextField hostField = new TextField();
    private final TextField portField = new TextField();
    private final TextField databaseField = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final CheckBox savePasswordCheck = new CheckBox("Save password");

    // Add these new fields
    private final BooleanProperty testingInProgress = new SimpleBooleanProperty(false);
    private Button connectButton;
    private Button cancelButton;
    private Button testButton;

    private final Label statusLabel = new Label();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
    ButtonType testButtonType = new ButtonType("Test", ButtonBar.ButtonData.OTHER);

    public ConnectionDialog() {
        setTitle("New Connection");
        setHeaderText("Enter database connection details");

        // Setup dialog buttons

        getDialogPane().getButtonTypes().addAll(testButtonType, connectButtonType, ButtonType.CANCEL);

        // Create and configure the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);

        // Add form elements
        grid.add(new Label("Connection Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Database Type:"), 0, 1);
        dbTypeCombo.getItems().addAll(ConnectionProfile.DatabaseType.values());
        dbTypeCombo.getSelectionModel().selectFirst();
        grid.add(dbTypeCombo, 1, 1);

        grid.add(new Label("Host:"), 0, 2);
        grid.add(hostField, 1, 2);

        grid.add(new Label("Port:"), 0, 3);
        grid.add(portField, 1, 3);

        grid.add(new Label("Database:"), 0, 4);
        grid.add(databaseField, 1, 4);

        grid.add(new Label("Username:"), 0, 5);
        usernameField.setText("root");
        grid.add(usernameField, 1, 5);

        grid.add(new Label("Password:"), 0, 6);
        grid.add(passwordField, 1, 6);

        grid.add(savePasswordCheck, 1, 7);

        // Status area
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(16, 16);
        HBox statusBox = new HBox(5, progressIndicator, statusLabel);
        statusBox.setStyle("-fx-padding: 5 0 0 0;");
        grid.add(statusBox, 0, 8, 2, 1);

        // Set default ports based on database type
        dbTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            portField.setText(newVal == ConnectionProfile.DatabaseType.MYSQL ? "3306" :
                    newVal == ConnectionProfile.DatabaseType.POSTGRESQL ? "5432" : "");
        });
// Get button references
        connectButton = (Button) getDialogPane().lookupButton(connectButtonType);
        cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        testButton = (Button) getDialogPane().lookupButton(testButtonType);
// Bind button states
        connectButton.disableProperty().bind(
                nameField.textProperty().isEmpty()
                        .or(hostField.textProperty().isEmpty())
                        .or(portField.textProperty().isEmpty())
                        .or(databaseField.textProperty().isEmpty())
                        .or(testingInProgress)
        );
        testButton.disableProperty().bind(testingInProgress);
        // Set the content
        getDialogPane().setContent(new VBox(10, grid));

        // Handle test connection button

        testButton.setOnAction(e -> testConnection());

        // Convert the result to a ConnectionProfile when the connect button is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return createConnectionProfile();
            }
            return null;
        });
    }

    private void testConnection() {
        ConnectionProfile testProfile = createConnectionProfile();
        statusLabel.setText("Testing connection...");
        statusLabel.setStyle("-fx-text-fill: #666;");
        progressIndicator.setVisible(true);
        testingInProgress.set(true);

        executor.submit(() -> {
            try {
                logger.info("Attempting to connect to database: {}", testProfile.getName());
                long startTime = System.currentTimeMillis();

                // Use appropriate test method based on database type
                Connection conn = testProfile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL
                        ? ConnectionService.testPostgreSQLConnection(testProfile)
                        : ConnectionService.testConnection(testProfile);

                long endTime = System.currentTimeMillis();

                Platform.runLater(() -> {
                    String successMsg = String.format("Connection successful! (%d ms)", endTime - startTime);
                    statusLabel.setText(successMsg);
                    statusLabel.setStyle("-fx-text-fill: #2e7d32;");
                    progressIndicator.setVisible(false);
                    logger.info(successMsg);

                    try {
                        if (conn != null) conn.close();
                    } catch (SQLException e) {
                        logger.warn("Error closing test connection", e);
                    }
                });
            } catch (SQLException e) {
                String errorMsg = "Connection failed: " + getFriendlyErrorMessage(e);
                logger.error(errorMsg, e);
                Platform.runLater(() -> {
                    statusLabel.setText(errorMsg);
                    statusLabel.setStyle("-fx-text-fill: #c62828;");
                    progressIndicator.setVisible(false);
                });
            } finally {
                Platform.runLater(() -> testingInProgress.set(false));
            }
        });
    }

    private ConnectionProfile createConnectionProfile() {
        ConnectionProfile profile = new ConnectionProfile();
        profile.setName(nameField.getText());
        profile.setType(dbTypeCombo.getValue());
        profile.setHost(hostField.getText());
        profile.setPort(Integer.parseInt(portField.getText()));
        profile.setDatabase(databaseField.getText());
        profile.setUsername(usernameField.getText());

        // Debug the password before setting it
        String password = passwordField.getText();
        System.out.println("DEBUG - Password from field: '" + password + "'");
        System.out.println("DEBUG - Password length: " + password.length());

        profile.setPassword(password);
        profile.setSavePassword(savePasswordCheck.isSelected());

        // Debug the profile object
        System.out.println("DEBUG - Profile password: '" + profile.getPassword() + "'");

        return profile;
    }

    private String getFriendlyErrorMessage(SQLException e) {
        // Provide user-friendly error messages
        if (e.getMessage().contains("Access denied")) {
            return "Invalid username/password";
        } else if (e.getMessage().contains("Unknown database")) {
            return "Database does not exist";
        } else if (e.getMessage().contains("Communications link failure")) {
            return "Cannot connect to server - check host/port";
        } else if (e.getMessage().contains("Connection refused")) {
            return "Server not available at specified host/port";
        }
        return e.getMessage();
    }
}
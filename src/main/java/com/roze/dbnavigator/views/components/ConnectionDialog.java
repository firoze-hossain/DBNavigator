package com.roze.dbnavigator.views.components;

import com.roze.dbnavigator.models.ConnectionProfile;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ConnectionDialog extends Dialog<ConnectionProfile> {
    private final TextField nameField = new TextField();
    private final ComboBox<ConnectionProfile.DatabaseType> dbTypeCombo = new ComboBox<>();
    private final TextField hostField = new TextField();
    private final TextField portField = new TextField();
    private final TextField databaseField = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final CheckBox savePasswordCheck = new CheckBox("Save password");

    public ConnectionDialog() {
        setTitle("New Connection");
        setHeaderText("Enter database connection details");

        // Setup dialog buttons
        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

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
        grid.add(usernameField, 1, 5);
        
        grid.add(new Label("Password:"), 0, 6);
        grid.add(passwordField, 1, 6);
        
        grid.add(savePasswordCheck, 1, 7);

        // Set default ports based on database type
        dbTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            portField.setText(newVal == ConnectionProfile.DatabaseType.MYSQL ? "3306" : 
                            newVal == ConnectionProfile.DatabaseType.POSTGRESQL ? "5432" : "");
        });

        // Enable/Disable connect button depending on whether a name was entered
        getDialogPane().lookupButton(connectButtonType).disableProperty().bind(
            nameField.textProperty().isEmpty()
        );

        // Set the content
        getDialogPane().setContent(new VBox(10, grid));

        // Convert the result to a ConnectionProfile when the connect button is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                ConnectionProfile profile = new ConnectionProfile();
                profile.setName(nameField.getText());
                profile.setType(dbTypeCombo.getValue());
                profile.setHost(hostField.getText());
                profile.setPort(Integer.parseInt(portField.getText()));
                profile.setDatabase(databaseField.getText());
                profile.setUsername(usernameField.getText());
                profile.setPassword(passwordField.getText());
                profile.setSavePassword(savePasswordCheck.isSelected());
                return profile;
            }
            return null;
        });
    }
}
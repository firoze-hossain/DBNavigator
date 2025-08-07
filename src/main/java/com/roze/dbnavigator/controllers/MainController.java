package com.roze.dbnavigator.controllers;

import com.roze.dbnavigator.models.ConnectionProfile;
import com.roze.dbnavigator.views.components.ConnectionDialog;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.StatusBar;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.IOException;
import java.util.Optional;

public class MainController {
    @FXML
    private ComboBox<String> connectionCombo;
    @FXML
    private TreeView<String> schemaTree;
    @FXML
    private ListView<String> savedQueries;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private CodeArea sqlEditor;
    @FXML
    private TableView<?> resultTable;
    @FXML
    private Label connectionStatus;
    @FXML
    private Label executionTimeLabel;

    @FXML
    private BorderPane mainPane;
    @FXML
    private StatusBar statusBar;

    private ConnectionProfile currentConnection;

    @FXML
    public void initialize() {
        setupConnectionCombo();
        setupSchemaTree();
        setupSavedQueries();
        setupCodeEditor();

        // New setup from suggested code
        setupTabs();
        setupStatusBar();
    }

    private void setupConnectionCombo() {
        connectionCombo.getItems().addAll("MySQL", "PostgreSQL");
        connectionCombo.getSelectionModel().selectFirst();
    }

    private void setupSchemaTree() {
        TreeItem<String> root = new TreeItem<>("Databases");
        root.getChildren().addAll(
                new TreeItem<>("Tables"),
                new TreeItem<>("Views"),
                new TreeItem<>("Procedures")
        );
        schemaTree.setRoot(root);
        schemaTree.setShowRoot(false);
    }

    private void setupSavedQueries() {
        savedQueries.getItems().addAll(
                "SELECT * FROM users",
                "SELECT COUNT(*) FROM orders",
                "SHOW TABLES"
        );
    }

    private void setupCodeEditor() {
        sqlEditor.setParagraphGraphicFactory(LineNumberFactory.get(sqlEditor));
        sqlEditor.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 14px;");
        sqlEditor.replaceText(0, 0, "-- Enter your SQL here\n");
    }

    @FXML
    private void minimizeWindow() {
        ((Stage) connectionCombo.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = (Stage) connectionCombo.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void closeWindow() {
        ((Stage) connectionCombo.getScene().getWindow()).close();
    }

    @FXML
    private void showConnectionDialog() {
        ConnectionDialog dialog = new ConnectionDialog();
        Optional<ConnectionProfile> result = dialog.showAndWait();

        result.ifPresent(profile -> {
            currentConnection = profile;
            connectionStatus.setText("Connected to: " + profile.getName());
//            statusBar.getRightItems().clear();
//            statusBar.getRightItems().add(new Label("Connected to: " + profile.getName()));

            // Load schema browser with real data
            loadSchemaBrowser(profile);
        });
    }

    private void loadSchemaBrowser(ConnectionProfile profile) {
        // Replace with actual schema loading
        setupSchemaTree(); // Temporary - keep your existing setup until implemented

        // TODO: Implement real schema loading using SchemaService
        // schemaTree.setRoot(SchemaService.loadSchema(profile));
    }

    // New methods from suggested code
    private void setupTabs() {
        // Add initial query tab
        addNewQueryTab();

        // Add "+" button for new tabs
        Tab addTab = new Tab();
        addTab.setClosable(false);
        addTab.setGraphic(new Label("+"));
        addTab.setOnSelectionChanged(e -> {
            if (addTab.isSelected()) {
                addNewQueryTab();
                mainTabPane.getSelectionModel().select(mainTabPane.getTabs().size() - 2);
            }
        });
        mainTabPane.getTabs().add(addTab);
    }

//    private void addNewQueryTab() {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/views/fxml/query-tab.fxml"));
//            BorderPane content = loader.load();
//            QueryTabController controller = loader.getController();
//
//            Tab tab = new Tab("Query " + (mainTabPane.getTabs().size() + 1));
//            tab.setContent(content);
//            tab.setOnCloseRequest(e -> {
//                if (controller.hasUnsavedChanges()) {
//                    // Prompt to save
//                    if (!confirmTabClose()) {
//                        e.consume(); // Cancel closing
//                    }
//                }
//            });
//
//            mainTabPane.getTabs().add(mainTabPane.getTabs().size() - 1, tab);
//            mainTabPane.getSelectionModel().select(tab);
//        } catch (IOException e) {
//            e.printStackTrace();
//            showErrorDialog("Failed to create new query tab", e.getMessage());
//        }
//    }
private void addNewQueryTab() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/views/fxml/query-tab.fxml"));
        VBox content = loader.load(); // Changed from BorderPane to VBox
        QueryTabController controller = loader.getController();

        Tab tab = new Tab("Query " + (mainTabPane.getTabs().size() + 1));
        tab.setContent(content);
        tab.setOnCloseRequest(e -> {
            if (controller.hasUnsavedChanges()) {
                if (!confirmTabClose()) {
                    e.consume();
                }
            }
        });

        mainTabPane.getTabs().add(mainTabPane.getTabs().size() - 1, tab);
        mainTabPane.getSelectionModel().select(tab);
    } catch (IOException e) {
        e.printStackTrace();
        showErrorDialog("Failed to create new query tab", e.getMessage());
    }
}
    private boolean confirmTabClose() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes");
        alert.setContentText("Do you want to save before closing?");

        ButtonType saveButton = new ButtonType("Save");
        ButtonType discardButton = new ButtonType("Discard");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == saveButton) {
            // Save logic here

            return true;
        } else if (result.get() == discardButton) {
            return true;
        } else {
            return false;
        }
    }

    //    private void setupStatusBar() {
//        if (statusBar != null) {
//            statusBar.setText("Ready");
//            if (statusBar.getRightItems() != null) {
//                statusBar.getRightItems().add(new Label("Not connected"));
//            }
//        }
//    }
    private void setupStatusBar() {
        // Remove statusBar related code since we're using HBox instead
        connectionStatus.setText("Not connected");
        executionTimeLabel.setText("Ready");
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
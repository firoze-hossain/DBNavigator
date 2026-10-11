package com.roze.dbnavigator.ui.languages;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 * Settings panel for Languages > XSLT File Associations.
 * Matches DataGrip screenshot media_1791723153609_2bda34c5.png.
 */
public class XsltFileAssociationsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public XsltFileAssociationsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.XsltFileAssociationsConfig cfg = settings.getXsltFileAssociations();

        ObservableList<AppSettingsStore.XsltAssociationEntry> entries = FXCollections.observableArrayList();
        for (AppSettingsStore.XsltAssociationEntry e : cfg.getEntries()) {
            if (e != null) entries.add(e.copy());
        }
        inputs.put("languages_xslt_fileAssociations", entries);

        // --- Left Pane: Project XSLT Files ---
        Label projectXsltLabel = new Label("Project XSLT Files:");
        projectXsltLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        ListView<AppSettingsStore.XsltAssociationEntry> projectListView = new ListView<>(entries);
        projectListView.setStyle("-fx-background-color: #1e1f22; -fx-control-inner-background: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        projectListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AppSettingsStore.XsltAssociationEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("📁 " + item.getXsltFileOrDir());
                    setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
                }
            }
        });
        VBox.setVgrow(projectListView, Priority.ALWAYS);

        VBox leftPane = new VBox(8, projectXsltLabel, projectListView);
        leftPane.setPrefWidth(260);
        leftPane.setMinWidth(200);

        // --- Right Pane: Associated Files ---
        Label associatedLabel = new Label("Associated Files:");
        associatedLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        Button addBtn = new Button("+");
        addBtn.setTooltip(new Tooltip("Add Associated XML File"));
        addBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Button removeBtn = new Button("—");
        removeBtn.setTooltip(new Tooltip("Remove"));
        removeBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        removeBtn.setDisable(true);

        HBox rightToolbar = new HBox(4, addBtn, removeBtn);
        rightToolbar.setAlignment(Pos.CENTER_LEFT);

        ObservableList<String> associatedItems = FXCollections.observableArrayList();
        ListView<String> associatedListView = new ListView<>(associatedItems);
        associatedListView.setStyle("-fx-background-color: #1e1f22; -fx-control-inner-background: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(associatedListView, Priority.ALWAYS);

        Label emptyLabel = new Label("No associated files");
        emptyLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        associatedListView.setPlaceholder(emptyLabel);

        // Selection logic on left pane
        projectListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            associatedItems.clear();
            if (newVal != null && newVal.getAssociatedFiles() != null) {
                associatedItems.addAll(newVal.getAssociatedFiles());
            }
            removeBtn.setDisable(associatedListView.getSelectionModel().getSelectedItem() == null);
        });

        associatedListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            removeBtn.setDisable(newVal == null);
        });

        addBtn.setOnAction(e -> {
            AppSettingsStore.XsltAssociationEntry selectedEntry = projectListView.getSelectionModel().getSelectedItem();
            if (selectedEntry == null) return;

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Associated XML File");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("XML Files (*.xml, *.xsd, *.xsl, *.xslt)", "*.xml", "*.xsd", "*.xsl", "*.xslt"),
                    new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
            );
            Window win = addBtn.getScene() != null ? addBtn.getScene().getWindow() : null;
            File f = chooser.showOpenDialog(win);
            if (f != null) {
                String path = f.getAbsolutePath();
                if (!associatedItems.contains(path)) {
                    associatedItems.add(path);
                    selectedEntry.setAssociatedFiles(new ArrayList<>(associatedItems));
                    associatedListView.getSelectionModel().select(path);
                }
            }
        });

        removeBtn.setOnAction(e -> {
            AppSettingsStore.XsltAssociationEntry selectedEntry = projectListView.getSelectionModel().getSelectedItem();
            String selectedFile = associatedListView.getSelectionModel().getSelectedItem();
            if (selectedEntry != null && selectedFile != null) {
                associatedItems.remove(selectedFile);
                selectedEntry.setAssociatedFiles(new ArrayList<>(associatedItems));
            }
        });

        if (!entries.isEmpty()) {
            projectListView.getSelectionModel().select(0);
        }

        VBox rightPane = new VBox(6, associatedLabel, rightToolbar, associatedListView);
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        VBox.setVgrow(rightPane, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(leftPane, rightPane);
        splitPane.setDividerPositions(0.35);
        splitPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        VBox content = new VBox(splitPane);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");
        return content;
    }
}

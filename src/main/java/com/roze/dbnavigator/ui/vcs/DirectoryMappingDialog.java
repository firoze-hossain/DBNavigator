package com.roze.dbnavigator.ui.vcs;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;

/**
 * Modal dialog for creating or editing VCS Directory Mappings,
 * matching DataGrip screenshot "Directory Mapping".
 */
public class DirectoryMappingDialog {

    private final Stage stage;
    private final ComboBox<String> vcsCombo;
    private final RadioButton dirRadio;
    private final TextField dirField;
    private final Button browseBtn;
    private final RadioButton projectRadio;
    private boolean saved = false;
    private AppSettingsStore.VcsDirectoryMappingEntry resultEntry;

    public DirectoryMappingDialog(Window owner, AppSettingsStore.VcsDirectoryMappingEntry initialEntry) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Directory Mapping");
        stage.setResizable(false);

        // Version Control combo row
        Label vcsLabel = new Label("Version control:");
        vcsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        vcsCombo = new ComboBox<>();
        vcsCombo.getItems().addAll("Git", "None");
        vcsCombo.setEditable(true);
        vcsCombo.setPrefWidth(160);
        vcsCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Hyperlink moreViaPlugins = new Hyperlink("More via plugins...");
        moreViaPlugins.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        moreViaPlugins.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Additional VCS integrations (Subversion, Mercurial, Perforce) can be installed from Plugins.", ButtonType.OK);
            alert.initOwner(stage);
            alert.setHeaderText("VCS Plugins");
            alert.showAndWait();
        });

        HBox vcsRow = new HBox(10, vcsLabel, vcsCombo, moreViaPlugins);
        vcsRow.setAlignment(Pos.CENTER_LEFT);

        // Radio group for Directory vs Project
        ToggleGroup group = new ToggleGroup();

        dirRadio = new RadioButton("Directory:");
        dirRadio.setToggleGroup(group);
        dirRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        dirField = new TextField();
        dirField.setPrefWidth(340);
        dirField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(dirField, Priority.ALWAYS);

        browseBtn = new Button("📁");
        browseBtn.setTooltip(new Tooltip("Browse Folder"));
        browseBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select VCS Root Directory");
            String cur = dirField.getText();
            if (cur != null && !cur.isBlank()) {
                File f = new File(cur.trim());
                if (f.exists() && f.isDirectory()) {
                    chooser.setInitialDirectory(f);
                }
            }
            File selected = chooser.showDialog(stage);
            if (selected != null) {
                dirField.setText(selected.getAbsolutePath());
            }
        });

        HBox dirRow = new HBox(8, dirRadio, dirField, browseBtn);
        dirRow.setAlignment(Pos.CENTER_LEFT);

        projectRadio = new RadioButton("Project: all content roots, including modules and the .idea directory");
        projectRadio.setToggleGroup(group);
        projectRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        // Radio group change listener
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isDir = (newVal == dirRadio);
            dirField.setDisable(!isDir);
            browseBtn.setDisable(!isDir);
        });

        // Initialize values
        if (initialEntry != null && !"<Project>".equalsIgnoreCase(initialEntry.getDirectory())) {
            vcsCombo.setValue(initialEntry.getVcs() != null ? initialEntry.getVcs() : "Git");
            dirRadio.setSelected(true);
            dirField.setText(initialEntry.getDirectory());
        } else {
            vcsCombo.setValue(initialEntry != null && initialEntry.getVcs() != null ? initialEntry.getVcs() : "Git");
            projectRadio.setSelected(true);
            String userHome = System.getProperty("user.home");
            dirField.setText(userHome + File.separator + "DataGripProjects" + File.separator + "Db");
        }

        // Action buttons
        Button saveBtn = new Button("Save Mapping");
        saveBtn.setDefaultButton(true);
        saveBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 6 16; -fx-cursor: hand;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 14; -fx-cursor: hand;");

        saveBtn.setOnAction(e -> {
            String vcsVal = vcsCombo.getValue() != null ? vcsCombo.getValue().trim() : "Git";
            String dirVal;
            if (projectRadio.isSelected()) {
                dirVal = "<Project>";
            } else {
                dirVal = dirField.getText() != null && !dirField.getText().isBlank() ? dirField.getText().trim() : "<Project>";
            }
            resultEntry = new AppSettingsStore.VcsDirectoryMappingEntry(dirVal, vcsVal);
            saved = true;
            stage.close();
        });

        cancelBtn.setOnAction(e -> {
            saved = false;
            stage.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonBar = new HBox(10, spacer, saveBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(14, 0, 0, 0));

        VBox content = new VBox(14, vcsRow, dirRow, projectRadio, buttonBar);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setStyle("-fx-background-color: #1e1f22;");
        content.setPrefWidth(540);

        Scene scene = new Scene(content);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
    }

    public AppSettingsStore.VcsDirectoryMappingEntry showAndWait() {
        stage.showAndWait();
        return saved ? resultEntry : null;
    }
}

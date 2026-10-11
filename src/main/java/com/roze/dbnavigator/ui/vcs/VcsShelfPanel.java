package com.roze.dbnavigator.ui.vcs;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Map;

/**
 * Settings panel for Version Control > Shelf,
 * matching DataGrip screenshot media_1791679114312_25dde94d.png.
 */
public class VcsShelfPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public VcsShelfPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.VcsShelfConfig cfg = settings.getVcsShelf();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        CheckBox removeAppliedCheck = new CheckBox("Remove successfully applied files from the shelf");
        removeAppliedCheck.setSelected(cfg.isRemoveAppliedFiles());
        removeAppliedCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_removeAppliedFiles", removeAppliedCheck);

        CheckBox shelveBaseCheck = new CheckBox("Shelve base revisions of files under distributed version control systems");
        shelveBaseCheck.setSelected(cfg.isShelveBaseRevisions());
        shelveBaseCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_shelveBaseRevisions", shelveBaseCheck);

        Label shelveBaseHint = new Label("The base content of files larger than 500K will not be stored");
        shelveBaseHint.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-padding: 0 0 0 24;");

        // Shelves location row
        String defaultLocation = System.getProperty("user.home") + File.separator + "DataGripProjects" + File.separator + "Db" + File.separator + ".idea" + File.separator + "shelf";
        String initialLocation = cfg.getCustomShelvesLocation() != null && !cfg.getCustomShelvesLocation().isBlank()
                ? cfg.getCustomShelvesLocation() : defaultLocation;

        Button changeLocationBtn = new Button("Change Shelves Location...");
        changeLocationBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 14; -fx-cursor: hand;");

        Label locationLabel = new Label("Current location is " + initialLocation);
        locationLabel.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px;");

        final String[] activeLocation = new String[]{initialLocation};
        inputs.put("vcs_shelvesLocation", activeLocation[0]);

        changeLocationBtn.setOnAction(e -> {
            Window owner = changeLocationBtn.getScene() != null ? changeLocationBtn.getScene().getWindow() : null;
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Shelf Directory");
            File initialDir = new File(activeLocation[0]);
            if (initialDir.exists() && initialDir.isDirectory()) {
                chooser.setInitialDirectory(initialDir);
            }
            File chosen = chooser.showDialog(owner);
            if (chosen != null) {
                activeLocation[0] = chosen.getAbsolutePath();
                locationLabel.setText("Current location is " + activeLocation[0]);
                inputs.put("vcs_shelvesLocation", activeLocation[0]);
            }
        });

        HBox locationRow = new HBox(12, changeLocationBtn, locationLabel);
        locationRow.setAlignment(Pos.CENTER_LEFT);
        locationRow.setPadding(new Insets(6, 0, 0, 0));

        content.getChildren().addAll(
                removeAppliedCheck,
                shelveBaseCheck,
                shelveBaseHint,
                locationRow
        );

        return content;
    }
}

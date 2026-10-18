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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Modal dialog for creating and editing Issue Navigation links,
 * supporting YouTrack, JIRA, and custom navigation patterns.
 */
public class IssueNavigationPatternDialog {

    private final Stage stage;
    private final TextField issueField;
    private final TextField linkField;
    private boolean saved = false;
    private AppSettingsStore.IssueNavigationPattern result;

    public IssueNavigationPatternDialog(Window owner, String trackerType, AppSettingsStore.IssueNavigationPattern initial) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        String titleType = trackerType != null ? trackerType : (initial != null ? initial.getTrackerType() : "Issue Navigation Link");
        stage.setTitle("Add " + titleType);
        stage.setResizable(false);

        Label issueLabel = new Label("Issue ID regex / pattern:");
        issueLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        issueField = new TextField();
        issueField.setPrefWidth(360);
        issueField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label linkLabel = new Label("Issue navigation link URL (use $0, $1):");
        linkLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        linkField = new TextField();
        linkField.setPrefWidth(360);
        linkField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        // Defaults based on type
        if (initial != null) {
            issueField.setText(initial.getIssue());
            linkField.setText(initial.getLink());
        } else if ("YouTrack".equalsIgnoreCase(trackerType) || "YouTrack Pattern".equalsIgnoreCase(trackerType)) {
            issueField.setText("([A-Z]+-\\d+)");
            linkField.setText("https://youtrack.example.com/issue/$1");
        } else if ("JIRA".equalsIgnoreCase(trackerType) || "JIRA Pattern".equalsIgnoreCase(trackerType)) {
            issueField.setText("([A-Z]+-\\d+)");
            linkField.setText("https://jira.atlassian.com/browse/$1");
        } else {
            issueField.setText("[A-Z]+-\\d+");
            linkField.setText("https://example.com/issue/$0");
        }

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 6 16; -fx-cursor: hand;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 14; -fx-cursor: hand;");

        okBtn.setOnAction(e -> {
            String issueVal = issueField.getText() != null ? issueField.getText().trim() : "";
            String linkVal = linkField.getText() != null ? linkField.getText().trim() : "";
            if (!issueVal.isEmpty() && !linkVal.isEmpty()) {
                result = new AppSettingsStore.IssueNavigationPattern(issueVal, linkVal, trackerType != null ? trackerType : "Custom");
                saved = true;
                stage.close();
            }
        });

        cancelBtn.setOnAction(e -> {
            saved = false;
            stage.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonBar = new HBox(10, spacer, okBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(12, issueLabel, issueField, linkLabel, linkField, buttonBar);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setStyle("-fx-background-color: #1e1f22;");
        content.setPrefWidth(420);

        Scene scene = new Scene(content);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
    }

    public AppSettingsStore.IssueNavigationPattern showAndWait() {
        stage.showAndWait();
        return saved ? result : null;
    }
}

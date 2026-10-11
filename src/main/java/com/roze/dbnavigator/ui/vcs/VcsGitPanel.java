package com.roze.dbnavigator.ui.vcs;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.SettingsDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Map;

/**
 * Settings panel for Version Control > Git,
 * matching DataGrip screenshots media_1791679162915_e4ffc4df.png and media_1791679190258_ffb7a70f.png.
 */
public class VcsGitPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public VcsGitPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.VcsGitConfig cfg = settings.getVcsGit();

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Top Row: Path to Git executable
        Label gitPathLabel = new Label("Path to Git executable:");
        gitPathLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        String detectedGit = autoDetectGit(cfg.getGitPath());
        TextField gitPathField = new TextField(detectedGit);
        gitPathField.setPrefWidth(380);
        gitPathField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("vcs_gitPath", gitPathField);

        Button browseGitBtn = new Button("📁");
        browseGitBtn.setTooltip(new Tooltip("Browse Git executable"));
        browseGitBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;");
        browseGitBtn.setOnAction(e -> {
            Window owner = browseGitBtn.getScene() != null ? browseGitBtn.getScene().getWindow() : null;
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Git Executable");
            File file = chooser.showOpenDialog(owner);
            if (file != null) {
                gitPathField.setText(file.getAbsolutePath());
            }
        });

        Button testGitBtn = new Button("Test");
        testGitBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14; -fx-cursor: hand;");
        testGitBtn.setOnAction(e -> {
            String path = gitPathField.getText();
            if (path != null && path.startsWith("Auto-detected: ")) {
                path = path.substring("Auto-detected: ".length());
            }
            String testResult = testGitExecutable(path);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, testResult, ButtonType.OK);
            alert.setHeaderText("Git Integration Test");
            alert.showAndWait();
        });

        HBox gitPathRow = new HBox(8, gitPathLabel, gitPathField, browseGitBtn, testGitBtn);
        gitPathRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox projectOnlyCheck = new CheckBox("Set this path only for the current project");
        projectOnlyCheck.setSelected(cfg.isSetPathOnlyForCurrentProject());
        projectOnlyCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 0 0 0 150;");
        inputs.put("vcs_gitSetPathOnlyForCurrentProject", projectOnlyCheck);

        CheckBox autoExcludeCheck = new CheckBox("Automatically exclude ignored directories from the analysis");
        autoExcludeCheck.setSelected(cfg.isAutomaticallyExcludeIgnoredDirectories());
        autoExcludeCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitAutomaticallyExcludeIgnoredDirectories", autoExcludeCheck);

        // Section: Commit
        HBox commitSection = SettingsDialog.buildTitledSectionLine("Commit");

        CheckBox stagingAreaCheck = new CheckBox("Enable staging area");
        stagingAreaCheck.setSelected(cfg.isEnableStagingArea());
        stagingAreaCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitEnableStagingArea", stagingAreaCheck);

        Label stagingHint = new Label("This will disable changelists support. Only for non-modal commit interface.");
        stagingHint.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-padding: 0 0 0 24;");

        CheckBox crlfCheck = new CheckBox("Warn if CRLF line separators are about to be committed");
        crlfCheck.setSelected(cfg.isWarnCrlfLineSeparators());
        crlfCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitWarnCrlfLineSeparators", crlfCheck);

        CheckBox detachedHeadCheck = new CheckBox("Warn when committing in detached HEAD or during rebase");
        detachedHeadCheck.setSelected(cfg.isWarnCommittingInDetachedHead());
        detachedHeadCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitWarnCommittingInDetachedHead", detachedHeadCheck);

        CheckBox largeFilesCheck = new CheckBox("Warn when committing files larger than");
        largeFilesCheck.setSelected(cfg.isWarnCommitFilesLargerThan());
        largeFilesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitWarnCommitFilesLargerThan", largeFilesCheck);

        Spinner<Integer> largeFilesSpinner = new Spinner<>(1, 10000, cfg.getWarnCommitFilesLargerThanMb(), 10);
        largeFilesSpinner.setEditable(true);
        largeFilesSpinner.setPrefWidth(80);
        largeFilesSpinner.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        largeFilesSpinner.disableProperty().bind(largeFilesCheck.selectedProperty().not());
        inputs.put("vcs_gitWarnCommitFilesLargerThanMb", largeFilesSpinner);

        Label mbLabel = new Label("MB");
        mbLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        HBox largeFilesRow = new HBox(6, largeFilesCheck, largeFilesSpinner, mbLabel);
        largeFilesRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox issueNamesCheck = new CheckBox("Warn when committing files with names that might cause issues on other systems");
        issueNamesCheck.setSelected(cfg.isWarnCommitNamesIssueOnOtherSystems());
        issueNamesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitWarnCommitNamesIssueOnOtherSystems", issueNamesCheck);

        CheckBox cherryPickCheck = new CheckBox("Add the 'cherry-picked from <hash>' suffix when picking commits pushed to protected branches");
        cherryPickCheck.setSelected(cfg.isAddCherryPickedSuffix());
        cherryPickCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitAddCherryPickedSuffix", cherryPickCheck);

        Button gpgBtn = new Button("Configure GPG Key...");
        gpgBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 14; -fx-cursor: hand;");

        Label gpgHint = new Label(cfg.getGpgKey() != null && !cfg.getGpgKey().isBlank() ? "Key: " + cfg.getGpgKey() : "No Git roots in the project");
        gpgHint.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px;");

        gpgBtn.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog(cfg.getGpgKey());
            dlg.setTitle("Configure GPG Key");
            dlg.setHeaderText("Enter GPG Key ID for commit signing:");
            dlg.showAndWait().ifPresent(val -> {
                cfg.setGpgKey(val);
                inputs.put("vcs_gitGpgKey", val);
                gpgHint.setText(!val.isBlank() ? "Key: " + val : "No Git roots in the project");
            });
        });

        HBox gpgRow = new HBox(12, gpgBtn, gpgHint);
        gpgRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Push
        HBox pushSection = SettingsDialog.buildTitledSectionLine("Push");

        CheckBox autoUpdatePushCheck = new CheckBox("Auto-update if push of the current branch was rejected");
        autoUpdatePushCheck.setSelected(cfg.isAutoUpdateIfPushRejected());
        autoUpdatePushCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitAutoUpdateIfPushRejected", autoUpdatePushCheck);

        CheckBox showPushDialogCheck = new CheckBox("Show Push dialog for Commit and Push");
        showPushDialogCheck.setSelected(cfg.isShowPushDialogForCommitAndPush());
        showPushDialogCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitShowPushDialogForCommitAndPush", showPushDialogCheck);

        CheckBox pushProtectedOnlyCheck = new CheckBox("Show Push dialog only when committing to protected branches");
        pushProtectedOnlyCheck.setSelected(cfg.isShowPushDialogOnlyForProtectedBranches());
        pushProtectedOnlyCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 0 0 0 24;");
        pushProtectedOnlyCheck.disableProperty().bind(showPushDialogCheck.selectedProperty().not());
        inputs.put("vcs_gitShowPushDialogOnlyForProtectedBranches", pushProtectedOnlyCheck);

        Label protectedBranchesLabel = new Label("Protected branches:");
        protectedBranchesLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField protectedBranchesField = new TextField(cfg.getProtectedBranches());
        protectedBranchesField.setPrefWidth(320);
        protectedBranchesField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("vcs_gitProtectedBranches", protectedBranchesField);

        HBox protectedBranchesRow = new HBox(8, protectedBranchesLabel, protectedBranchesField);
        protectedBranchesRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox loadGitHubRulesCheck = new CheckBox("Load branch protection rules from GitHub");
        loadGitHubRulesCheck.setSelected(cfg.isLoadBranchProtectionFromGitHub());
        loadGitHubRulesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 0 0 0 24;");
        inputs.put("vcs_gitLoadBranchProtectionFromGitHub", loadGitHubRulesCheck);

        Label githubRulesHint = new Label("GitHub rules are added to the local rules and synced on every fetch");
        githubRulesHint.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-padding: 0 0 0 48;");

        // Section: Update
        HBox updateSection = SettingsDialog.buildTitledSectionLine("Update");

        Label updateMethodLabel = new Label("Update method:");
        updateMethodLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ToggleGroup updateMethodGroup = new ToggleGroup();
        RadioButton mergeRadio = new RadioButton("Merge");
        mergeRadio.setToggleGroup(updateMethodGroup);
        mergeRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton rebaseRadio = new RadioButton("Rebase");
        rebaseRadio.setToggleGroup(updateMethodGroup);
        rebaseRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if ("Rebase".equalsIgnoreCase(cfg.getUpdateMethod())) {
            rebaseRadio.setSelected(true);
        } else {
            mergeRadio.setSelected(true);
        }
        inputs.put("vcs_gitUpdateMethodGroup", updateMethodGroup);

        HBox updateMethodRow = new HBox(12, updateMethodLabel, mergeRadio, rebaseRadio);
        updateMethodRow.setAlignment(Pos.CENTER_LEFT);

        Label cleanTreeLabel = new Label("Clean working tree using:");
        cleanTreeLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ToggleGroup cleanTreeGroup = new ToggleGroup();
        RadioButton stashRadio = new RadioButton("Stash");
        stashRadio.setToggleGroup(cleanTreeGroup);
        stashRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton shelveRadio = new RadioButton("Shelve");
        shelveRadio.setToggleGroup(cleanTreeGroup);
        shelveRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if ("Stash".equalsIgnoreCase(cfg.getCleanWorkingTreeUsing())) {
            stashRadio.setSelected(true);
        } else {
            shelveRadio.setSelected(true);
        }
        inputs.put("vcs_gitCleanTreeGroup", cleanTreeGroup);

        HBox cleanTreeRow = new HBox(12, cleanTreeLabel, stashRadio, shelveRadio);
        cleanTreeRow.setAlignment(Pos.CENTER_LEFT);

        Label filterUpdatePathsLabel = new Label("Filter \"Update Project\" information by paths:");
        filterUpdatePathsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> filterUpdatePathsCombo = new ComboBox<>();
        filterUpdatePathsCombo.getItems().addAll("All", "None");
        filterUpdatePathsCombo.setValue(cfg.getFilterUpdateProjectInfoByPaths());
        filterUpdatePathsCombo.setPrefWidth(120);
        filterUpdatePathsCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("vcs_gitFilterUpdateProjectInfoByPaths", filterUpdatePathsCombo);

        HBox filterUpdatePathsRow = new HBox(8, filterUpdatePathsLabel, filterUpdatePathsCombo);
        filterUpdatePathsRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox fetchAutoCheck = new CheckBox("Fetch remote changes automatically");
        fetchAutoCheck.setSelected(cfg.isFetchRemoteChangesAutomatically());
        fetchAutoCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitFetchRemoteChangesAutomatically", fetchAutoCheck);

        CheckBox checkIncomingCheck = new CheckBox("Check remote for incoming changes");
        checkIncomingCheck.setSelected(cfg.isCheckRemoteForIncomingChanges());
        checkIncomingCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitCheckRemoteForIncomingChanges", checkIncomingCheck);

        // Fetch tags and Credential Helper
        Label fetchTagsLabel = new Label("Fetch tags:");
        fetchTagsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> fetchTagsCombo = new ComboBox<>();
        fetchTagsCombo.getItems().addAll("Auto", "All", "None");
        fetchTagsCombo.setValue(cfg.getFetchTags());
        fetchTagsCombo.setPrefWidth(120);
        fetchTagsCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("vcs_gitFetchTags", fetchTagsCombo);

        Label fetchTagsHint = new Label("Follow git config");
        fetchTagsHint.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px;");

        HBox fetchTagsRow = new HBox(8, fetchTagsLabel, fetchTagsCombo, fetchTagsHint);
        fetchTagsRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox credentialHelperCheck = new CheckBox("Use credential helper");
        credentialHelperCheck.setSelected(cfg.isUseCredentialHelper());
        credentialHelperCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitUseCredentialHelper", credentialHelperCheck);

        // Section: Stash
        HBox stashSection = SettingsDialog.buildTitledSectionLine("Stash");

        CheckBox combineStashesShelvesCheck = new CheckBox("Combine stashes and shelves in one tab");
        combineStashesShelvesCheck.setSelected(cfg.isCombineStashesAndShelvesInOneTab());
        combineStashesShelvesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_gitCombineStashesAndShelvesInOneTab", combineStashesShelvesCheck);

        Label compareStashedLabel = new Label("When \"Show Diff\" is called, compare stashed changes:");
        compareStashedLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ToggleGroup compareStashedGroup = new ToggleGroup();
        RadioButton compareLocalRadio = new RadioButton("With the local version of a file");
        compareLocalRadio.setToggleGroup(compareStashedGroup);
        compareLocalRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton compareParentRadio = new RadioButton("With the parent commit");
        compareParentRadio.setToggleGroup(compareStashedGroup);
        compareParentRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if ("parent".equalsIgnoreCase(cfg.getCompareStashedChangesWith())) {
            compareParentRadio.setSelected(true);
        } else {
            compareLocalRadio.setSelected(true);
        }
        inputs.put("vcs_gitCompareStashedGroup", compareStashedGroup);

        VBox compareStashedBox = new VBox(6, compareStashedLabel, compareLocalRadio, compareParentRadio);
        compareStashedBox.setPadding(new Insets(0, 0, 0, 24));

        content.getChildren().addAll(
                gitPathRow,
                projectOnlyCheck,
                autoExcludeCheck,
                commitSection,
                stagingAreaCheck,
                stagingHint,
                crlfCheck,
                detachedHeadCheck,
                largeFilesRow,
                issueNamesCheck,
                cherryPickCheck,
                gpgRow,
                pushSection,
                autoUpdatePushCheck,
                showPushDialogCheck,
                pushProtectedOnlyCheck,
                protectedBranchesRow,
                loadGitHubRulesCheck,
                githubRulesHint,
                updateSection,
                updateMethodRow,
                cleanTreeRow,
                filterUpdatePathsRow,
                fetchAutoCheck,
                checkIncomingCheck,
                fetchTagsRow,
                credentialHelperCheck,
                stashSection,
                combineStashesShelvesCheck,
                compareStashedBox
        );

        return content;
    }

    private static String autoDetectGit(String configured) {
        if (configured != null && !configured.isBlank() && !"git".equalsIgnoreCase(configured)) {
            return configured;
        }
        String[] possiblePaths = {
                "/usr/bin/git",
                "/usr/local/bin/git",
                "C:\\Program Files\\Git\\bin\\git.exe",
                "C:\\Program Files\\Git\\cmd\\git.exe"
        };
        for (String p : possiblePaths) {
            if (new File(p).canExecute()) {
                return "Auto-detected: " + p;
            }
        }
        return "Auto-detected: /usr/bin/git";
    }

    private static String testGitExecutable(String path) {
        try {
            Process process = new ProcessBuilder(path, "--version").start();
            String output = new String(process.getInputStream().readAllBytes());
            int code = process.waitFor();
            if (code == 0 && !output.isBlank()) {
                return output.trim();
            }
        } catch (Exception ignored) {}
        return "Git executable located at: " + path;
    }
}

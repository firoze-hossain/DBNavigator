package com.roze.dbnavigator.ui.vcs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.SettingsDialog;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class VcsSettingsTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    public void testVcsChangelistsDefaultsAndCopy() {
        AppSettingsStore.VcsChangelistsConfig cfg = new AppSettingsStore.VcsChangelistsConfig();
        assertFalse(cfg.isCreateChangelistsAutomatically(), "Create changelists automatically should default to false");
        assertTrue(cfg.isAllowPuttingChangesInDifferentChangelists(), "Allow putting changes within one file into different changelists should default to true");
        assertFalse(cfg.isHighlightFilesFromInactiveChangelists(), "Highlight files from inactive changelists should default to false");
        assertFalse(cfg.isShowDialogOnEditFromInactiveChangelist(), "Show dialog on edit from inactive changelist should default to false");
        assertEquals("Show options", cfg.getWhenEmptyChangelistBecomesInactive());
        assertTrue(cfg.isHighlightFilesWithChangelistConflicts(), "Highlight files with changelist conflicts should default to true");
        assertNotNull(cfg.getIgnoredConflictFiles());
        assertTrue(cfg.getIgnoredConflictFiles().isEmpty());

        cfg.getIgnoredConflictFiles().add("/path/to/conflict.sql");
        cfg.setCreateChangelistsAutomatically(true);
        AppSettingsStore.VcsChangelistsConfig copy = cfg.copy();
        assertTrue(copy.isCreateChangelistsAutomatically());
        assertEquals(1, copy.getIgnoredConflictFiles().size());
        assertEquals("/path/to/conflict.sql", copy.getIgnoredConflictFiles().get(0));

        // Ensure deep copy
        copy.getIgnoredConflictFiles().clear();
        assertEquals(1, cfg.getIgnoredConflictFiles().size());
    }

    @Test
    public void testVcsCommitDefaultsAndInspections() {
        AppSettingsStore.VcsCommitConfig cfg = new AppSettingsStore.VcsCommitConfig();
        assertFalse(cfg.isClearInitialCommitMessage(), "Clear initial commit message should default to false");
        assertFalse(cfg.isReformatCode(), "Reformat code should default to false");
        assertFalse(cfg.isCleanup(), "Cleanup should default to false");
        assertEquals("Project Default", cfg.getCleanupProfile());
        assertTrue(cfg.isAnalyzeCode(), "Analyze code should default to true");
        assertEquals("Project Default", cfg.getAnalyzeCodeProfile());
        assertTrue(cfg.isCheckTodo(), "Check TODO should default to true");
        assertEquals("All TODOs", cfg.getCheckTodoFilter());
        assertFalse(cfg.isRunConfiguration(), "Run Configuration should default to false");
        assertEquals("None", cfg.getRunConfigurationName());
        assertTrue(cfg.isRunAdvancedChecksAfterCommit(), "Run advanced checks after commit should default to true");

        List<AppSettingsStore.CommitInspectionConfig> inspections = cfg.getInspections();
        assertNotNull(inspections);
        assertEquals(3, inspections.size(), "Should have exactly 3 commit message inspections");
        assertEquals("Blank line between subject and body", inspections.get(0).getName());
        assertTrue(inspections.get(0).isEnabled());
        assertEquals("Warning", inspections.get(0).getSeverity());

        assertEquals("Limit body line", inspections.get(1).getName());
        assertTrue(inspections.get(1).isEnabled());
        assertEquals("Warning", inspections.get(1).getSeverity());

        assertEquals("Limit subject line", inspections.get(2).getName());
        assertTrue(inspections.get(2).isEnabled());
        assertEquals("Warning", inspections.get(2).getSeverity());

        AppSettingsStore.VcsCommitConfig copy = cfg.copy();
        assertEquals(3, copy.getInspections().size());
        copy.getInspections().get(0).setEnabled(false);
        assertTrue(cfg.getInspections().get(0).isEnabled(), "Copy should be decoupled");
    }

    @Test
    public void testVcsConfirmationDefaults() {
        AppSettingsStore.VcsConfirmationConfig cfg = new AppSettingsStore.VcsConfirmationConfig();
        assertEquals("Ask", cfg.getWhenFilesCreated());
        assertFalse(cfg.isApplyToFilesCreatedOutside());
        assertEquals("Ask", cfg.getWhenFilesDeleted());
        assertTrue(cfg.isShowOptionsBeforeUpdate());
        assertTrue(cfg.isAskConfirmationToDropCommits());

        assertFalse(cfg.isCheckConflictsWithServer());
        assertEquals(60, cfg.getCheckConflictsMinutes());
        assertFalse(cfg.isHighlightFilesChangedInLastDays());
        assertEquals(31, cfg.getHighlightFilesDays());
        assertFalse(cfg.isHighlightDirectoriesWithModifiedFiles());
        assertEquals("Ask", cfg.getWhenPatchCreated());
        assertTrue(cfg.isRestoreWorkspaceWhenSwitchingBranches());
        assertTrue(cfg.isLimitHistoryRows());
        assertEquals(1000, cfg.getMaxHistoryRows());

        assertTrue(cfg.isHighlightModifiedLinesInGutter());
        assertTrue(cfg.isHighlightModifiedLinesInErrorStripe());
        assertTrue(cfg.isHighlightWhitespaceOnlyModifications());

        AppSettingsStore.VcsConfirmationConfig copy = cfg.copy();
        copy.setCheckConflictsMinutes(120);
        assertEquals(60, cfg.getCheckConflictsMinutes());
    }

    @Test
    public void testVcsDirectoryMappingDefaultsAndOperations() {
        AppSettingsStore.VcsDirectoryMappingConfig cfg = new AppSettingsStore.VcsDirectoryMappingConfig();
        assertTrue(cfg.isEnableAutomaticMappingDetection());
        assertNotNull(cfg.getMappings());
        assertEquals(1, cfg.getMappings().size());
        assertEquals("<Project>", cfg.getMappings().get(0).getDirectory());
        assertEquals("Git", cfg.getMappings().get(0).getVcs());

        AppSettingsStore.VcsDirectoryMappingEntry newEntry = new AppSettingsStore.VcsDirectoryMappingEntry("/path/to/repo", "Git");
        cfg.getMappings().add(newEntry);
        assertEquals(2, cfg.getMappings().size());

        AppSettingsStore.VcsDirectoryMappingConfig copy = cfg.copy();
        assertEquals(2, copy.getMappings().size());
        copy.getMappings().remove(1);
        assertEquals(2, cfg.getMappings().size());
    }

    @Test
    public void testVcsFileStatusColorsDefaults() {
        AppSettingsStore.VcsFileStatusColorsConfig cfg = new AppSettingsStore.VcsFileStatusColorsConfig();
        List<AppSettingsStore.VcsFileStatusColorEntry> list = cfg.getStatusColors();
        assertNotNull(list);
        assertEquals(20, list.size(), "Must contain all 20 file status color entries");

        Map<String, AppSettingsStore.VcsFileStatusColorEntry> map = new HashMap<>();
        for (AppSettingsStore.VcsFileStatusColorEntry e : list) {
            map.put(e.getStatusName(), e);
        }

        assertTrue(map.containsKey("Added"));
        assertEquals("629755", map.get("Added").getColorHex());
        assertTrue(map.get("Added").isCustomized());

        assertTrue(map.containsKey("Changelist conflict"));
        assertEquals("DE6A66", map.get("Changelist conflict").getColorHex());
        assertTrue(map.get("Changelist conflict").isCustomized());

        assertTrue(map.containsKey("Deleted"));
        assertEquals("6E6E6E", map.get("Deleted").getColorHex());
        assertTrue(map.get("Deleted").isCustomized());

        assertTrue(map.containsKey("Modified"));
        assertEquals("6897BB", map.get("Modified").getColorHex());
        assertTrue(map.get("Modified").isCustomized());

        assertTrue(map.containsKey("Hijacked"));
        assertEquals("BC3F3C", map.get("Hijacked").getColorHex());
        assertFalse(map.get("Hijacked").isCustomized());

        assertTrue(map.containsKey("Up to date"));
        assertEquals("A9B7C6", map.get("Up to date").getColorHex());
        assertFalse(map.get("Up to date").isCustomized());

        AppSettingsStore.VcsFileStatusColorsConfig copy = cfg.copy();
        assertEquals(20, copy.getStatusColors().size());
        copy.getStatusColors().get(0).setColorHex("FFFFFF");
        assertEquals("629755", cfg.getStatusColors().get(0).getColorHex());
    }

    @Test
    public void testJacksonSerializationRoundtrip() throws Exception {
        AppSettingsStore.Settings original = new AppSettingsStore.Settings();
        original.getVcsChangelists().setCreateChangelistsAutomatically(true);
        original.getVcsChangelists().setWhenEmptyChangelistBecomesInactive("Remove silently");
        original.getVcsCommit().setClearInitialCommitMessage(true);
        original.getVcsConfirmation().setCheckConflictsWithServer(true);
        original.getVcsConfirmation().setCheckConflictsMinutes(45);
        original.getVcsDirectoryMappings().getMappings().add(new AppSettingsStore.VcsDirectoryMappingEntry("/test/dir", "Git"));
        original.getVcsFileStatusColors().getStatusColors().get(0).setColorHex("123456");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(original);
        assertNotNull(json);

        AppSettingsStore.Settings deserialized = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertNotNull(deserialized.getVcsChangelists());
        assertTrue(deserialized.getVcsChangelists().isCreateChangelistsAutomatically());
        assertEquals("Remove silently", deserialized.getVcsChangelists().getWhenEmptyChangelistBecomesInactive());
        assertTrue(deserialized.getVcsCommit().isClearInitialCommitMessage());
        assertTrue(deserialized.getVcsConfirmation().isCheckConflictsWithServer());
        assertEquals(45, deserialized.getVcsConfirmation().getCheckConflictsMinutes());
        assertEquals(2, deserialized.getVcsDirectoryMappings().getMappings().size());
        assertEquals("123456", deserialized.getVcsFileStatusColors().getStatusColors().get(0).getColorHex());
    }

    @Test
    public void testVcsPanelsBuildAndRegisterInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // Changelists panel
        VcsChangelistsPanel clPanel = new VcsChangelistsPanel(settings, inputs);
        Node clNode = clPanel.createPanel();
        assertNotNull(clNode);
        assertTrue(inputs.containsKey("vcs_createChangelistsAutomatically"));
        assertTrue(inputs.containsKey("vcs_allowPuttingChangesInDifferentChangelists"));
        assertTrue(inputs.containsKey("vcs_whenEmptyChangelistBecomesInactive"));
        assertTrue(inputs.containsKey("vcs_ignoredConflictFiles"));

        // Commit panel
        VcsCommitPanel commitPanel = new VcsCommitPanel(settings, inputs, null);
        Node commitNode = commitPanel.createPanel();
        assertNotNull(commitNode);
        assertTrue(inputs.containsKey("vcs_clearInitialCommitMessage"));
        assertTrue(inputs.containsKey("vcs_commitInspections"));
        assertTrue(inputs.containsKey("vcs_analyzeCode"));

        // Confirmation panel
        VcsConfirmationPanel confirmPanel = new VcsConfirmationPanel(settings, inputs);
        Node confirmNode = confirmPanel.createPanel();
        assertNotNull(confirmNode);
        assertTrue(inputs.containsKey("vcs_whenFilesCreated"));
        assertTrue(inputs.containsKey("vcs_whenFilesDeleted"));
        assertTrue(inputs.containsKey("vcs_highlightModifiedLinesInGutter"));

        // Directory mappings panel
        VcsDirectoryMappingsPanel dirPanel = new VcsDirectoryMappingsPanel(settings, inputs);
        Node dirNode = dirPanel.createPanel();
        assertNotNull(dirNode);
        assertTrue(inputs.containsKey("vcs_directoryMappings"));
        assertTrue(inputs.containsKey("vcs_enableAutomaticMappingDetection"));

        // File status colors panel
        VcsFileStatusColorsPanel colorsPanel = new VcsFileStatusColorsPanel(settings, inputs);
        Node colorsNode = colorsPanel.createPanel();
        assertNotNull(colorsNode);
        assertTrue(inputs.containsKey("vcs_fileStatusColors"));
    }

    @Test
    public void testSettingsDialogBuildPanelForPathRoutesVcsCategories() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        Node p1 = SettingsDialog.buildPanelForPath("Version Control / Changelists", settings, inputs, null);
        assertNotNull(p1);

        Node p2 = SettingsDialog.buildPanelForPath("Version Control / Commit", settings, inputs, null);
        assertNotNull(p2);

        Node p3 = SettingsDialog.buildPanelForPath("Version Control / Confirmation", settings, inputs, null);
        assertNotNull(p3);

        Node p4 = SettingsDialog.buildPanelForPath("Version Control / Directory Mappings", settings, inputs, null);
        assertNotNull(p4);

        Node p5 = SettingsDialog.buildPanelForPath("Version Control / File Status Colors", settings, inputs, null);
        assertNotNull(p5);
    }

    @Test
    public void testSettingsDialogApplySettingsSavesVcsInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // Set inputs to modified values
        CheckBox autoCl = new CheckBox();
        autoCl.setSelected(true);
        inputs.put("vcs_createChangelistsAutomatically", autoCl);

        ComboBox<String> whenInactive = new ComboBox<>();
        whenInactive.setValue("Do nothing");
        inputs.put("vcs_whenEmptyChangelistBecomesInactive", whenInactive);

        CheckBox clearMsg = new CheckBox();
        clearMsg.setSelected(true);
        inputs.put("vcs_clearInitialCommitMessage", clearMsg);

        ComboBox<String> whenCreated = new ComboBox<>();
        whenCreated.setValue("Add silently");
        inputs.put("vcs_whenFilesCreated", whenCreated);

        Spinner<Integer> daysSpinner = new Spinner<>(1, 365, 14);
        inputs.put("vcs_highlightFilesDays", daysSpinner);

        List<AppSettingsStore.VcsDirectoryMappingEntry> mappings = new ArrayList<>();
        mappings.add(new AppSettingsStore.VcsDirectoryMappingEntry("<Project>", "Git"));
        mappings.add(new AppSettingsStore.VcsDirectoryMappingEntry("/extra/repo", "None"));
        inputs.put("vcs_directoryMappings", mappings);

        SettingsDialog.applySettings(null, settings, inputs);

        assertTrue(settings.getVcsChangelists().isCreateChangelistsAutomatically());
        assertEquals("Do nothing", settings.getVcsChangelists().getWhenEmptyChangelistBecomesInactive());
        assertTrue(settings.getVcsCommit().isClearInitialCommitMessage());
        assertEquals("Add silently", settings.getVcsConfirmation().getWhenFilesCreated());
        assertEquals(14, settings.getVcsConfirmation().getHighlightFilesDays());
        assertEquals(2, settings.getVcsDirectoryMappings().getMappings().size());
        assertEquals("/extra/repo", settings.getVcsDirectoryMappings().getMappings().get(1).getDirectory());
    }

    @Test
    public void testVcsIssueNavigationDefaultsAndOperations() {
        AppSettingsStore.VcsIssueNavigationConfig cfg = new AppSettingsStore.VcsIssueNavigationConfig();
        assertNotNull(cfg.getPatterns());
        assertTrue(cfg.getPatterns().isEmpty());

        AppSettingsStore.IssueNavigationPattern p1 = new AppSettingsStore.IssueNavigationPattern("([A-Z]+-\\d+)", "https://youtrack.jetbrains.com/issue/$1", "YouTrack");
        cfg.getPatterns().add(p1);
        assertEquals(1, cfg.getPatterns().size());

        AppSettingsStore.VcsIssueNavigationConfig copy = cfg.copy();
        assertEquals(1, copy.getPatterns().size());
        assertEquals("YouTrack", copy.getPatterns().get(0).getTrackerType());
        copy.getPatterns().clear();
        assertEquals(1, cfg.getPatterns().size());
    }

    @Test
    public void testVcsShelfDefaults() {
        AppSettingsStore.VcsShelfConfig cfg = new AppSettingsStore.VcsShelfConfig();
        assertFalse(cfg.isRemoveAppliedFiles());
        assertTrue(cfg.isShelveBaseRevisions());
        assertEquals("", cfg.getCustomShelvesLocation());

        AppSettingsStore.VcsShelfConfig copy = cfg.copy();
        copy.setCustomShelvesLocation("/custom/shelf");
        assertEquals("", cfg.getCustomShelvesLocation());
        assertEquals("/custom/shelf", copy.getCustomShelvesLocation());
    }

    @Test
    public void testVcsGitDefaults() {
        AppSettingsStore.VcsGitConfig cfg = new AppSettingsStore.VcsGitConfig();
        assertEquals("git", cfg.getGitPath());
        assertFalse(cfg.isSetPathOnlyForCurrentProject());
        assertFalse(cfg.isAutomaticallyExcludeIgnoredDirectories());

        assertFalse(cfg.isEnableStagingArea());
        assertTrue(cfg.isWarnCrlfLineSeparators());
        assertTrue(cfg.isWarnCommittingInDetachedHead());
        assertTrue(cfg.isWarnCommitFilesLargerThan());
        assertEquals(50, cfg.getWarnCommitFilesLargerThanMb());
        assertTrue(cfg.isWarnCommitNamesIssueOnOtherSystems());
        assertTrue(cfg.isAddCherryPickedSuffix());
        assertEquals("", cfg.getGpgKey());

        assertFalse(cfg.isAutoUpdateIfPushRejected());
        assertTrue(cfg.isShowPushDialogForCommitAndPush());
        assertFalse(cfg.isShowPushDialogOnlyForProtectedBranches());
        assertEquals("master;main", cfg.getProtectedBranches());
        assertTrue(cfg.isLoadBranchProtectionFromGitHub());

        assertEquals("Merge", cfg.getUpdateMethod());
        assertEquals("Shelve", cfg.getCleanWorkingTreeUsing());
        assertEquals("All", cfg.getFilterUpdateProjectInfoByPaths());
        assertFalse(cfg.isFetchRemoteChangesAutomatically());
        assertTrue(cfg.isCheckRemoteForIncomingChanges());

        assertEquals("Auto", cfg.getFetchTags());
        assertFalse(cfg.isUseCredentialHelper());

        assertFalse(cfg.isCombineStashesAndShelvesInOneTab());
        assertEquals("local", cfg.getCompareStashedChangesWith());

        AppSettingsStore.VcsGitConfig copy = cfg.copy();
        copy.setUpdateMethod("Rebase");
        assertEquals("Merge", cfg.getUpdateMethod());
        assertEquals("Rebase", copy.getUpdateMethod());
    }

    @Test
    public void testIssueNavigationShelfGitPanelsAndRouting() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // Issue Navigation Panel
        VcsIssueNavigationPanel issuePanel = new VcsIssueNavigationPanel(settings, inputs);
        Node issueNode = issuePanel.createPanel();
        assertNotNull(issueNode);
        assertTrue(inputs.containsKey("vcs_issueNavigationPatterns"));

        // Shelf Panel
        VcsShelfPanel shelfPanel = new VcsShelfPanel(settings, inputs);
        Node shelfNode = shelfPanel.createPanel();
        assertNotNull(shelfNode);
        assertTrue(inputs.containsKey("vcs_removeAppliedFiles"));
        assertTrue(inputs.containsKey("vcs_shelveBaseRevisions"));
        assertTrue(inputs.containsKey("vcs_shelvesLocation"));

        // Git Panel
        VcsGitPanel gitPanel = new VcsGitPanel(settings, inputs);
        Node gitNode = gitPanel.createPanel();
        assertNotNull(gitNode);
        assertTrue(inputs.containsKey("vcs_gitPath"));
        assertTrue(inputs.containsKey("vcs_gitWarnCrlfLineSeparators"));
        assertTrue(inputs.containsKey("vcs_gitProtectedBranches"));
        assertTrue(inputs.containsKey("vcs_gitUpdateMethodGroup"));

        // Routing via SettingsDialog.buildPanelForPath
        Node r1 = SettingsDialog.buildPanelForPath("Version Control / Issue Navigation", settings, inputs, null);
        assertNotNull(r1);
        Node r2 = SettingsDialog.buildPanelForPath("Version Control / Shelf", settings, inputs, null);
        assertNotNull(r2);
        Node r3 = SettingsDialog.buildPanelForPath("Version Control / Git", settings, inputs, null);
        assertNotNull(r3);

        // Apply settings test
        List<AppSettingsStore.IssueNavigationPattern> pats = new ArrayList<>();
        pats.add(new AppSettingsStore.IssueNavigationPattern("JIRA-\\d+", "https://jira.com/$0", "JIRA"));
        inputs.put("vcs_issueNavigationPatterns", pats);

        javafx.scene.control.TextField gitPathField = new javafx.scene.control.TextField("/opt/git/bin/git");
        inputs.put("vcs_gitPath", gitPathField);

        inputs.put("vcs_shelvesLocation", "/tmp/shelf");

        SettingsDialog.applySettings(null, settings, inputs);

        assertEquals(1, settings.getVcsIssueNavigation().getPatterns().size());
        assertEquals("JIRA-\\d+", settings.getVcsIssueNavigation().getPatterns().get(0).getIssue());
        assertEquals("/opt/git/bin/git", settings.getVcsGit().getGitPath());
        assertEquals("/tmp/shelf", settings.getVcsShelf().getCustomShelvesLocation());
    }
}

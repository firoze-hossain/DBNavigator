package com.roze.dbnavigator.ui.advanced;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.SettingsDialog;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AdvancedSettingsTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX already initialized
        }
    }

    @Test
    public void testAdvancedSettingsDefaults() {
        AppSettingsStore.AdvancedSettingsConfig cfg = new AppSettingsStore.AdvancedSettingsConfig();

        // 1. Appearance
        assertFalse(cfg.isEnableIslandsUIForCustomThemes());

        // 2. Bookmarks
        assertTrue(cfg.isShowOnlyLineBookmarksInPopup());
        assertTrue(cfg.isEnableBookmarkContextRestorationOnBranchSwitching());

        // 3. Code Completion
        assertTrue(cfg.isEnableAutomaticCompletionOnTyping());

        // 4. Code Review Tools
        assertFalse(cfg.isAllInOneDiffForCodeReviews());
        assertTrue(cfg.isKeepIncreasedMarginsForDiffThreads());

        // 5. Container Environments
        assertEquals("Login Interactive shell (`$SHELL -l -i -c printenv`)", cfg.getShellModeForFetchingEnvVars());

        // 6. Database
        assertFalse(cfg.isShowDatabaseTabInSearchEverywhere());
        assertEquals("All", cfg.getOpenFileAsTableIfDetectedByScriptedLoader());
        assertEquals("Navigate and filter all", cfg.getRelatedRowsActionBehavior());
        assertFalse(cfg.isShowClassesTabInSearchEverywhere());

        // 7. Debugger
        assertEquals(50, cfg.getMaxRecentExpressions());
        assertTrue(cfg.isShowInlayRunToCursorPopup());

        // 8. Documentation Components
        assertEquals("As plain code", cfg.getBasicSyntaxHighlightingOfInlineCode());
        assertTrue(cfg.isBasicSyntaxHighlightingOfMultilineCodeBlocks());
        assertFalse(cfg.isSyntaxHighlightingOfLinksOnCodeElements());
        assertTrue(cfg.isAddBackgroundToInlineAndMultilineCodeBlocks());
        assertFalse(cfg.isShowQuickDocAutomaticallyDuringCompletionInModal());

        // 9. Editor
        assertFalse(cfg.isUseUndoTransparentCaretMovement());
        assertFalse(cfg.isDontCopyCutCurrentLineWithNoSelection());
        assertFalse(cfg.isDontSelectCopiedLineAfterCopyNoSelection());
        assertEquals("Paste above the caret line", cfg.getWhenPastingLineCopiedWithNoSelection());
        assertTrue(cfg.isRenderSpecialCharactersUsingUnicodeAbbreviations());
        assertTrue(cfg.isShowZoomIndicator());
        assertEquals("Horizontal line", cfg.getTabCharacterRendering());
        assertEquals(-1, cfg.getLeftMarginInDistractionFreeMode());
        assertEquals(-1, cfg.getLineNumberFontDelta());
        assertEquals(100000, cfg.getForceSoftWrapLongLinesThreshold());
        assertTrue(cfg.isSelectWhitespaceWithExtendSelection());
        assertTrue(cfg.isMoveCaretDownAfterCommentWithLineComment());
        assertFalse(cfg.isHideFloatingToolbarForCodeEditing());

        // 10. Editor Tabs
        assertTrue(cfg.isPreferExistingTabInInactiveSplitPane());
        assertFalse(cfg.isOpenDeclarationSourceInSameTab());
        assertFalse(cfg.isOpenDeclarationSourceFromDetachedWindowInMain());
        assertTrue(cfg.isPerformHideRestoreToolWindowsWithDoubleClickOnEditorTab());
        assertFalse(cfg.isPerformMaximizeNormalizeSplitsWithDoubleClickOnEditorTab());
        assertFalse(cfg.isEqualizeProportionsInNestedSplits());
        assertTrue(cfg.isKeepPinnedTabsOnLeft());

        // 11. Find/Replace
        assertTrue(cfg.isEnableSimilarUsagesClustering());
        assertEquals(100, cfg.getMaxResultsInFindInFilesPreview());

        // 12. IDE
        assertEquals(50, cfg.getMaxRecentProjects());
        assertEquals(50, cfg.getMaxRecentFiles());
        assertEquals(25, cfg.getMaxRecentLocations());
        assertEquals(5, cfg.getLocalHistoryDurationDays());

        // 13. Markdown
        assertFalse(cfg.isMarkdownHideFloatingToolbar());
        assertFalse(cfg.isSquashMultipleDashesInHeaderAnchors());

        // 14. Project View
        assertFalse(cfg.isProjectViewIncreaseFontSize());
        assertTrue(cfg.isProjectViewMoveFocusToEditorOnEnter());
        assertTrue(cfg.isProjectViewCollapseAllExpandedUnderNode());
        assertTrue(cfg.isEnableScratchesAndConsolesInProjectView());

        // 15. Run/Debug
        assertEquals(5, cfg.getTemporaryConfigurationsLimit());
        assertTrue(cfg.isConfirmRerunWithProcessTermination());
        assertFalse(cfg.isMakeConfigurationsPinnedByDefault());
        assertEquals(5, cfg.getMaxRecentRunConfigurations());

        // 16. SSH
        assertEquals("OpenSSH", cfg.getSshConfigFilesParser());
        assertEquals("ssh", cfg.getCustomPathToOpenSshTool());

        // 17. Search Everywhere
        assertTrue(cfg.isSearchEverywhereWaitForContributors());
        assertTrue(cfg.isSearchEverywhereShowNotificationsAboveResults());
        assertEquals(2000, cfg.getSearchEverywhereContributorsTimeoutMs());
        assertTrue(cfg.isSearchEverywhereShowRecentFilesAtTop());
        assertTrue(cfg.isSearchEverywhereShowTextSearchResults());
        assertTrue(cfg.isSearchEverywhereSortActionsMl());
        assertTrue(cfg.isSearchEverywhereSortFilesMl());
        assertTrue(cfg.isSearchEverywhereSortClassesMl());
        assertTrue(cfg.isSearchEverywhereTypoTolerantSearch());

        // 18. Search Scopes
        assertFalse(cfg.isKeepLastSelectedSearchScope());

        // 19. Startup
        assertTrue(cfg.isOpenReadmeOnStartupIfNoOpenFiles());

        // 20. Suggested refactoring
        assertFalse(cfg.isShowHintForSuggestedRefactoring());

        // 21. Terminal
        assertEquals(5000, cfg.getTerminalScrollbackBufferSize());
        assertTrue(cfg.isTerminalTypeahead());
        assertEquals(100, cfg.getTerminalTypeaheadLatencyThresholdMs());
        assertTrue(cfg.isTerminalLineSpacingAltScreenBuffer());
        assertFalse(cfg.isTerminalFillCharacterBackgroundIncludingLineSpacing());
        assertEquals("UTF-8", cfg.getTerminalCharacterEncoding());
        assertEquals(1024, cfg.getTerminalNewOutputCapacityKb());
        assertTrue(cfg.isTerminalSendEscCrOnShiftEnter());

        // 22. Tool Windows
        assertFalse(cfg.isAlwaysShowToolWindowHeaderIcons());
        assertTrue(cfg.isAllowDraggingToolWindowsByHeader());

        // 23. User Interface
        assertTrue(cfg.isUseNativeFileChooserDialog());
        assertTrue(cfg.isMergeMainMenuWithWindowTitle());
        assertFalse(cfg.isShowFileTypeIconInIdeFrameHeader());
        assertFalse(cfg.isUseWordsInsteadOfSymbolsForMacShortcuts());
        assertTrue(cfg.isActivateMenuItemsOnRightMouseButtonRelease());
        assertTrue(cfg.isCyclicScrollingInListsAndTrees());
        assertFalse(cfg.isPositionMouseCursorOnDefaultButtonInDialogs());
        assertFalse(cfg.isDisableDoubleModifierKeyShortcuts());
        assertFalse(cfg.isShowSettingsWindowAsEditorTab());

        // 24. Version Control
        assertTrue(cfg.isOpenDiffAsEditorTab());
        assertFalse(cfg.isVcsAllInOneDiff());
        assertFalse(cfg.isLoadFileAnnotationsFromVcsOnOpen());
        assertTrue(cfg.isHighlightIgnoredFiles());
        assertTrue(cfg.isEnableCommitToolWindow());
        assertFalse(cfg.isToggleCommitControls());
        assertTrue(cfg.isAutoCloseCommitToolWindowInFloatMode());
        assertFalse(cfg.isSelectAllReposWithNewCommitsForPush());
        assertFalse(cfg.isDoNotShowWarningWhenForcePushing());
        assertFalse(cfg.isUseModalCommitInterface());

        // 25. Version Control. Git
        assertTrue(cfg.isRecursivelyCloneSubmodulesInProject());
        assertEquals("Filters", cfg.getApplyContentTransformationReadingFromGit());
        assertTrue(cfg.isUseSafeForcePush());
        assertTrue(cfg.isCheckForIncomingOutgoingCommits());
        assertTrue(cfg.isAutoOpenUpdateInfoTabAfterUpdate());
        assertFalse(cfg.isDoNotRunGitCommitHooks());
        assertEquals("-", cfg.getBranchNameCleanupSymbol());
        assertEquals("All projects", cfg.getShowBranchNamesInRecentProjects());
        assertFalse(cfg.isShowGitTabInSearchEverywhere());
    }

    @Test
    public void testAdvancedSettingsCopyImmutability() {
        AppSettingsStore.AdvancedSettingsConfig orig = new AppSettingsStore.AdvancedSettingsConfig();
        orig.setEnableIslandsUIForCustomThemes(true);
        orig.setMaxRecentProjects(120);
        orig.setSshConfigFilesParser("Legacy");

        AppSettingsStore.AdvancedSettingsConfig copy = orig.copy();
        assertEquals(true, copy.isEnableIslandsUIForCustomThemes());
        assertEquals(120, copy.getMaxRecentProjects());
        assertEquals("Legacy", copy.getSshConfigFilesParser());

        // Modify copy
        copy.setEnableIslandsUIForCustomThemes(false);
        copy.setMaxRecentProjects(200);
        copy.setSshConfigFilesParser("OpenSSH");

        // Verify original unchanged
        assertTrue(orig.isEnableIslandsUIForCustomThemes());
        assertEquals(120, orig.getMaxRecentProjects());
        assertEquals("Legacy", orig.getSshConfigFilesParser());
    }

    @Test
    public void testAdvancedSettingsSerializationRoundtrip() throws Exception {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        AppSettingsStore.AdvancedSettingsConfig cfg = settings.getAdvancedSettings();
        cfg.setEnableIslandsUIForCustomThemes(true);
        cfg.setMaxRecentExpressions(75);
        cfg.setCustomPathToOpenSshTool("/usr/bin/ssh");
        cfg.setProperty("custom_key", "custom_value");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(settings);
        assertNotNull(json);
        assertTrue(json.contains("advancedSettings"));
        assertTrue(json.contains("enableIslandsUIForCustomThemes"));

        AppSettingsStore.Settings deserialized = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertNotNull(deserialized.getAdvancedSettings());
        assertTrue(deserialized.getAdvancedSettings().isEnableIslandsUIForCustomThemes());
        assertEquals(75, deserialized.getAdvancedSettings().getMaxRecentExpressions());
        assertEquals("/usr/bin/ssh", deserialized.getAdvancedSettings().getCustomPathToOpenSshTool());
        assertEquals("custom_value", deserialized.getAdvancedSettings().getProperty("custom_key"));
    }

    @Test
    public void testRegistryCompleteness() {
        List<AdvancedSettingsPanel.AdvancedSettingItem> registry = AdvancedSettingsPanel.getRegistry();
        assertNotNull(registry);
        assertTrue(registry.size() >= 43, "Registry should have at least 43 items, found: " + registry.size());

        Set<String> groups = new HashSet<>();
        Set<String> ids = new HashSet<>();

        for (AdvancedSettingsPanel.AdvancedSettingItem item : registry) {
            assertNotNull(item.getId(), "Item id cannot be null");
            assertNotNull(item.getGroup(), "Item group cannot be null: " + item.getId());
            assertNotNull(item.getTitle(), "Item title cannot be null: " + item.getId());
            assertNotNull(item.getType(), "Item type cannot be null: " + item.getId());
            assertNotNull(item.getDefaultValue(), "Default value cannot be null: " + item.getId());
            assertNotNull(item.getGetter(), "Getter cannot be null: " + item.getId());
            assertNotNull(item.getSetter(), "Setter cannot be null: " + item.getId());

            assertTrue(ids.add(item.getId()), "Duplicate item id: " + item.getId());
            groups.add(item.getGroup());
        }

        // All 25 groups and 106 items present
        assertEquals(106, registry.size(), "Should have exactly 106 items, found: " + registry.size());
        assertEquals(25, groups.size(), "Should have exactly 25 groups, found: " + groups.size());
        assertTrue(groups.contains("Appearance"));
        assertTrue(groups.contains("Bookmarks"));
        assertTrue(groups.contains("Code Completion"));
        assertTrue(groups.contains("Code Review Tools"));
        assertTrue(groups.contains("Container Environments: WSL, Docker"));
        assertTrue(groups.contains("Database"));
        assertTrue(groups.contains("Debugger"));
        assertTrue(groups.contains("Documentation Components"));
        assertTrue(groups.contains("Editor"));
        assertTrue(groups.contains("Editor Tabs"));
        assertTrue(groups.contains("Find/Replace"));
        assertTrue(groups.contains("IDE"));
        assertTrue(groups.contains("Markdown"));
        assertTrue(groups.contains("Project View"));
        assertTrue(groups.contains("Run/Debug"));
        assertTrue(groups.contains("SSH"));
        assertTrue(groups.contains("Search Everywhere"));
        assertTrue(groups.contains("Search Scopes in Find, Replace, Rename"));
        assertTrue(groups.contains("Startup"));
        assertTrue(groups.contains("Suggested refactoring"));
        assertTrue(groups.contains("Terminal"));
        assertTrue(groups.contains("Tool Windows"));
        assertTrue(groups.contains("User Interface"));
        assertTrue(groups.contains("Version Control"));
        assertTrue(groups.contains("Version Control. Git"));
    }

    @Test
    public void testPanelCreationAndInputsMap() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        AdvancedSettingsPanel panel = new AdvancedSettingsPanel(settings, inputs);
        Node node = panel.createPanel();
        assertNotNull(node);

        // Verify top bar controls registered
        assertTrue(inputs.containsKey("adv_search_field"));
        assertTrue(inputs.get("adv_search_field") instanceof TextField);

        assertTrue(inputs.containsKey("adv_show_modified_only"));
        assertTrue(inputs.get("adv_show_modified_only") instanceof CheckBox);

        // Verify registry items registered in inputs
        for (AdvancedSettingsPanel.AdvancedSettingItem item : AdvancedSettingsPanel.getRegistry()) {
            assertTrue(inputs.containsKey(item.getId()), "Missing input binding for: " + item.getId());
        }
    }

    @Test
    public void testLiveSearchFilter() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        AdvancedSettingsPanel panel = new AdvancedSettingsPanel(settings, inputs);
        panel.createPanel();

        TextField searchField = (TextField) inputs.get("adv_search_field");
        assertNotNull(searchField);

        // Search for "Islands"
        searchField.setText("Islands");
        assertEquals("Islands", searchField.getText());

        // Clear search
        searchField.setText("");
        assertEquals("", searchField.getText());
    }

    @Test
    public void testShowModifiedOnlyFilter() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        AdvancedSettingsPanel panel = new AdvancedSettingsPanel(settings, inputs);
        panel.createPanel();

        CheckBox showModified = (CheckBox) inputs.get("adv_show_modified_only");
        assertNotNull(showModified);

        // Toggle on
        showModified.setSelected(true);
        assertTrue(showModified.isSelected());

        // Modify a control value
        CheckBox islandsCheck = (CheckBox) inputs.get("adv_appearance_islands_ui");
        assertNotNull(islandsCheck);
        islandsCheck.setSelected(true);

        // Toggle off
        showModified.setSelected(false);
        assertFalse(showModified.isSelected());
    }

    @Test
    public void testSettingsDialogRouting() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        Node panel1 = SettingsDialog.buildPanelForPath("Advanced Settings", settings, inputs, path -> {});
        assertNotNull(panel1);

        Node panel2 = SettingsDialog.buildPanelForPath("advanced_settings", settings, inputs, path -> {});
        assertNotNull(panel2);
    }

    @Test
    public void testApplySettingsPersistence() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        AdvancedSettingsPanel panel = new AdvancedSettingsPanel(settings, inputs);
        panel.createPanel();

        // Mutate some inputs
        CheckBox islandsCheck = (CheckBox) inputs.get("adv_appearance_islands_ui");
        islandsCheck.setSelected(true);

        TextField recentExpr = (TextField) inputs.get("adv_debugger_max_recent_expr");
        recentExpr.setText("99");

        @SuppressWarnings("unchecked")
        ComboBox<String> shellMode = (ComboBox<String>) inputs.get("adv_container_shell_mode");
        shellMode.setValue("Login shell (`$SHELL -l -c printenv`)");

        TextField sshTool = (TextField) inputs.get("adv_ssh_custom_openssh_path");
        sshTool.setText("/custom/ssh");

        CheckBox nativeChooser = (CheckBox) inputs.get("adv_ui_use_native_file_chooser");
        nativeChooser.setSelected(false);

        TextField terminalEncoding = (TextField) inputs.get("adv_terminal_encoding");
        terminalEncoding.setText("ISO-8859-1");

        TextField branchCleanup = (TextField) inputs.get("adv_git_branch_name_cleanup_symbol");
        branchCleanup.setText("_");

        @SuppressWarnings("unchecked")
        ComboBox<String> gitTransform = (ComboBox<String>) inputs.get("adv_git_apply_content_transformation");
        gitTransform.setValue("TextConv");

        CheckBox allInOneDiff = (CheckBox) inputs.get("adv_vcs_all_in_one_diff");
        allInOneDiff.setSelected(true);

        // Apply
        AdvancedSettingsPanel.applySettings(inputs, settings);

        // Verify config updated
        AppSettingsStore.AdvancedSettingsConfig cfg = settings.getAdvancedSettings();
        assertTrue(cfg.isEnableIslandsUIForCustomThemes());
        assertEquals(99, cfg.getMaxRecentExpressions());
        assertEquals("Login shell (`$SHELL -l -c printenv`)", cfg.getShellModeForFetchingEnvVars());
        assertEquals("/custom/ssh", cfg.getCustomPathToOpenSshTool());
        assertFalse(cfg.isUseNativeFileChooserDialog());
        assertEquals("ISO-8859-1", cfg.getTerminalCharacterEncoding());
        assertEquals("_", cfg.getBranchNameCleanupSymbol());
        assertEquals("TextConv", cfg.getApplyContentTransformationReadingFromGit());
        assertTrue(cfg.isVcsAllInOneDiff());
    }
}

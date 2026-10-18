package com.roze.dbnavigator.ui.advanced;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Settings panel for Advanced Settings in DBNavigator.
 * Features a dynamic, data-driven registry of all 21 categories and 43+ advanced settings,
 * with real-time search filtering, modified-only filtering, and full persistence.
 */
public class AdvancedSettingsPanel {

    public enum SettingType {
        BOOLEAN,
        COMBO,
        INTEGER,
        TEXT
    }

    public static class AdvancedSettingItem {
        private final String id;
        private final String group;
        private final String title;
        private final String subtext;
        private final SettingType type;
        private final Object defaultValue;
        private final List<String> options;
        private final String unit;
        private final Function<AppSettingsStore.AdvancedSettingsConfig, Object> getter;
        private final BiConsumer<AppSettingsStore.AdvancedSettingsConfig, Object> setter;

        public AdvancedSettingItem(String id, String group, String title, String subtext,
                                   SettingType type, Object defaultValue, List<String> options,
                                   String unit,
                                   Function<AppSettingsStore.AdvancedSettingsConfig, Object> getter,
                                   BiConsumer<AppSettingsStore.AdvancedSettingsConfig, Object> setter) {
            this.id = id;
            this.group = group;
            this.title = title;
            this.subtext = subtext;
            this.type = type;
            this.defaultValue = defaultValue;
            this.options = options != null ? options : Collections.emptyList();
            this.unit = unit;
            this.getter = getter;
            this.setter = setter;
        }

        public String getId() { return id; }
        public String getGroup() { return group; }
        public String getTitle() { return title; }
        public String getSubtext() { return subtext; }
        public SettingType getType() { return type; }
        public Object getDefaultValue() { return defaultValue; }
        public List<String> getOptions() { return options; }
        public String getUnit() { return unit; }
        public Function<AppSettingsStore.AdvancedSettingsConfig, Object> getGetter() { return getter; }
        public BiConsumer<AppSettingsStore.AdvancedSettingsConfig, Object> getSetter() { return setter; }
    }

    private static final List<AdvancedSettingItem> REGISTRY = new ArrayList<>();

    static {
        // 1. Appearance
        register(new AdvancedSettingItem("adv_appearance_islands_ui", "Appearance",
                "Enable Islands UI for custom themes", "Requires restart",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isEnableIslandsUIForCustomThemes,
                (c, v) -> c.setEnableIslandsUIForCustomThemes((Boolean) v)));

        // 2. Bookmarks
        register(new AdvancedSettingItem("adv_bookmarks_only_line_bookmarks", "Bookmarks",
                "Show only line bookmarks in popup", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isShowOnlyLineBookmarksInPopup,
                (c, v) -> c.setShowOnlyLineBookmarksInPopup((Boolean) v)));
        register(new AdvancedSettingItem("adv_bookmarks_restore_context", "Bookmarks",
                "Enable bookmark context restoration on branch switching", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isEnableBookmarkContextRestorationOnBranchSwitching,
                (c, v) -> c.setEnableBookmarkContextRestorationOnBranchSwitching((Boolean) v)));

        // 3. Code Completion
        register(new AdvancedSettingItem("adv_completion_auto_typing", "Code Completion",
                "Enable automatic completion on typing", "If disabled, completion suggestions can still be invoked via shortcut",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isEnableAutomaticCompletionOnTyping,
                (c, v) -> c.setEnableAutomaticCompletionOnTyping((Boolean) v)));

        // 4. Code Review Tools
        register(new AdvancedSettingItem("adv_codereview_all_in_one_diff", "Code Review Tools",
                "All-in-one Diff for Code Reviews", "If enabled, all changed files in code reviews are shown in a single scrollable Diff view",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isAllInOneDiffForCodeReviews,
                (c, v) -> c.setAllInOneDiffForCodeReviews((Boolean) v)));
        register(new AdvancedSettingItem("adv_codereview_keep_increased_margins", "Code Review Tools",
                "Keep increased margins for Diff threads", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isKeepIncreasedMarginsForDiffThreads,
                (c, v) -> c.setKeepIncreasedMarginsForDiffThreads((Boolean) v)));

        // 5. Container Environments: WSL, Docker
        register(new AdvancedSettingItem("adv_container_shell_mode", "Container Environments: WSL, Docker",
                "Shell mode for fetching environment variables:", null,
                SettingType.COMBO, "Login Interactive shell (`$SHELL -l -i -c printenv`)",
                List.of("Login Interactive shell (`$SHELL -l -i -c printenv`)",
                        "Login shell (`$SHELL -l -c printenv`)",
                        "Interactive shell (`$SHELL -i -c printenv`)",
                        "None"),
                null,
                AppSettingsStore.AdvancedSettingsConfig::getShellModeForFetchingEnvVars,
                (c, v) -> c.setShellModeForFetchingEnvVars((String) v)));

        // 6. Database
        register(new AdvancedSettingItem("adv_db_search_everywhere_tab", "Database",
                "Show Database tab in Search Everywhere", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isShowDatabaseTabInSearchEverywhere,
                (c, v) -> c.setShowDatabaseTabInSearchEverywhere((Boolean) v)));
        register(new AdvancedSettingItem("adv_db_open_file_as_table", "Database",
                "Open file as table if detected by scripted loader:", "Automatically add \"Data\" tab with table representation if there is a scripted loader for that file",
                SettingType.COMBO, "All", List.of("All", "None", "Prompt"), null,
                AppSettingsStore.AdvancedSettingsConfig::getOpenFileAsTableIfDetectedByScriptedLoader,
                (c, v) -> c.setOpenFileAsTableIfDetectedByScriptedLoader((String) v)));
        register(new AdvancedSettingItem("adv_db_related_rows_behavior", "Database",
                "Related Rows action behavior:", null,
                SettingType.COMBO, "Navigate and filter all", List.of("Navigate and filter all", "Navigate only", "Filter only"), null,
                AppSettingsStore.AdvancedSettingsConfig::getRelatedRowsActionBehavior,
                (c, v) -> c.setRelatedRowsActionBehavior((String) v)));
        register(new AdvancedSettingItem("adv_db_classes_tab", "Database",
                "Show Classes tab in Search Everywhere", "Shows Classes tab like in other IDEs",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isShowClassesTabInSearchEverywhere,
                (c, v) -> c.setShowClassesTabInSearchEverywhere((Boolean) v)));

        // 7. Debugger
        register(new AdvancedSettingItem("adv_debugger_max_recent_expr", "Debugger",
                "Maximum number of recent expressions:", null,
                SettingType.INTEGER, 50, null, null,
                AppSettingsStore.AdvancedSettingsConfig::getMaxRecentExpressions,
                (c, v) -> c.setMaxRecentExpressions((Integer) v)));
        register(new AdvancedSettingItem("adv_debugger_run_to_cursor_popup", "Debugger",
                "Show inlay Run to Cursor popup", "Note, inlay Run to Cursor is implemented for the new UI only. In the classic UI, the click on line number will execute Run to Cursor.",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isShowInlayRunToCursorPopup,
                (c, v) -> c.setShowInlayRunToCursorPopup((Boolean) v)));

        // 8. Documentation Components
        register(new AdvancedSettingItem("adv_doc_inline_code_highlighting", "Documentation Components",
                "Basic syntax highlighting of inline code:", null,
                SettingType.COMBO, "As plain code", List.of("As plain code", "Highlighted", "Disabled"), null,
                AppSettingsStore.AdvancedSettingsConfig::getBasicSyntaxHighlightingOfInlineCode,
                (c, v) -> c.setBasicSyntaxHighlightingOfInlineCode((String) v)));
        register(new AdvancedSettingItem("adv_doc_multiline_code_blocks", "Documentation Components",
                "Basic syntax highlighting of multiline code blocks", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isBasicSyntaxHighlightingOfMultilineCodeBlocks,
                (c, v) -> c.setBasicSyntaxHighlightingOfMultilineCodeBlocks((Boolean) v)));
        register(new AdvancedSettingItem("adv_doc_links_on_code_elements", "Documentation Components",
                "Syntax highlighting of links on code elements", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSyntaxHighlightingOfLinksOnCodeElements,
                (c, v) -> c.setSyntaxHighlightingOfLinksOnCodeElements((Boolean) v)));
        register(new AdvancedSettingItem("adv_doc_background_code_blocks", "Documentation Components",
                "Add background to inline and multiline code blocks", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isAddBackgroundToInlineAndMultilineCodeBlocks,
                (c, v) -> c.setAddBackgroundToInlineAndMultilineCodeBlocks((Boolean) v)));
        register(new AdvancedSettingItem("adv_doc_quickdoc_modal_dialogs", "Documentation Components",
                "Show QuickDoc automatically during code completion in modal dialogs", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isShowQuickDocAutomaticallyDuringCompletionInModal,
                (c, v) -> c.setShowQuickDocAutomaticallyDuringCompletionInModal((Boolean) v)));

        // 9. Editor
        register(new AdvancedSettingItem("adv_editor_undo_transparent_caret", "Editor",
                "Use undo transparent caret movement", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isUseUndoTransparentCaretMovement,
                (c, v) -> c.setUseUndoTransparentCaretMovement((Boolean) v)));
        register(new AdvancedSettingItem("adv_editor_dont_copy_cut_empty_line", "Editor",
                "Don't copy/cut the current line when invoking the Copy or Cut action with no selection", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isDontCopyCutCurrentLineWithNoSelection,
                (c, v) -> c.setDontCopyCutCurrentLineWithNoSelection((Boolean) v)));
        register(new AdvancedSettingItem("adv_editor_dont_select_copied_empty_line", "Editor",
                "Don't select the copied line after invoking the Copy action with no selection", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isDontSelectCopiedLineAfterCopyNoSelection,
                (c, v) -> c.setDontSelectCopiedLineAfterCopyNoSelection((Boolean) v)));
        register(new AdvancedSettingItem("adv_editor_pasting_line_no_selection", "Editor",
                "When pasting a line copied with no selection:", null,
                SettingType.COMBO, "Paste above the caret line", List.of("Paste above the caret line", "Paste at caret"), null,
                AppSettingsStore.AdvancedSettingsConfig::getWhenPastingLineCopiedWithNoSelection,
                (c, v) -> c.setWhenPastingLineCopiedWithNoSelection((String) v)));
        register(new AdvancedSettingItem("adv_editor_unicode_abbr_control_codes", "Editor",
                "Render special characters, such as control codes, using their Unicode name abbreviations", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isRenderSpecialCharactersUsingUnicodeAbbreviations,
                (c, v) -> c.setRenderSpecialCharactersUsingUnicodeAbbreviations((Boolean) v)));
        register(new AdvancedSettingItem("adv_editor_zoom_indicator", "Editor",
                "Show zoom indicator", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isShowZoomIndicator,
                (c, v) -> c.setShowZoomIndicator((Boolean) v)));
        register(new AdvancedSettingItem("adv_editor_tab_rendering", "Editor",
                "Tab character rendering:", null,
                SettingType.COMBO, "Horizontal line", List.of("Horizontal line", "Arrow", "Long line"), null,
                AppSettingsStore.AdvancedSettingsConfig::getTabCharacterRendering,
                (c, v) -> c.setTabCharacterRendering((String) v)));
        register(new AdvancedSettingItem("adv_editor_left_margin_distraction_free", "Editor",
                "Left margin in distraction free mode:", "Specify -1 to use automatically calculated margin (centering the editor)",
                SettingType.INTEGER, -1, null, "pixels",
                AppSettingsStore.AdvancedSettingsConfig::getLeftMarginInDistractionFreeMode,
                (c, v) -> c.setLeftMarginInDistractionFreeMode((Integer) v)));
        register(new AdvancedSettingItem("adv_editor_line_number_font_delta", "Editor",
                "Line number's font delta:", "This value determines the increase or decrease in the editor line number font size compared to the editor's font size",
                SettingType.INTEGER, -1, null, "points",
                AppSettingsStore.AdvancedSettingsConfig::getLineNumberFontDelta,
                (c, v) -> c.setLineNumberFontDelta((Integer) v)));
        register(new AdvancedSettingItem("adv_editor_force_soft_wrap_threshold", "Editor",
                "Force soft wrap in documents with lines longer than:", "Increasing this limit will negatively affect editor performance",
                SettingType.INTEGER, 100000, null, "characters",
                AppSettingsStore.AdvancedSettingsConfig::getForceSoftWrapLongLinesThreshold,
                (c, v) -> c.setForceSoftWrapLongLinesThreshold((Integer) v)));
        register(new AdvancedSettingItem("adv_editor_select_whitespace_extend", "Editor",
                "Select whitespace characters with 'Extend Selection'", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSelectWhitespaceWithExtendSelection,
                (c, v) -> c.setSelectWhitespaceWithExtendSelection((Boolean) v)));
        register(new AdvancedSettingItem("adv_editor_move_caret_down_comment", "Editor",
                "Move caret down after Comment with Line Comment action", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isMoveCaretDownAfterCommentWithLineComment,
                (c, v) -> c.setMoveCaretDownAfterCommentWithLineComment((Boolean) v)));
        register(new AdvancedSettingItem("adv_editor_hide_floating_toolbar", "Editor",
                "Hide floating toolbar for code editing", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isHideFloatingToolbarForCodeEditing,
                (c, v) -> c.setHideFloatingToolbarForCodeEditing((Boolean) v)));

        // 10. Editor Tabs
        register(new AdvancedSettingItem("adv_tabs_prefer_inactive_split", "Editor Tabs",
                "When navigating to a file, prefer selecting existing tab in inactive split pane", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isPreferExistingTabInInactiveSplitPane,
                (c, v) -> c.setPreferExistingTabInInactiveSplitPane((Boolean) v)));
        register(new AdvancedSettingItem("adv_tabs_open_decl_same_tab", "Editor Tabs",
                "Open declaration source in the same tab", "When navigating to a method/class/variable declaration, the source file that contains the declaration will replace the current tab if there are no changes",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isOpenDeclarationSourceInSameTab,
                (c, v) -> c.setOpenDeclarationSourceInSameTab((Boolean) v)));
        register(new AdvancedSettingItem("adv_tabs_open_decl_detached_window", "Editor Tabs",
                "Open declaration source called from a detached window in the main IDE window", "When navigating to a method/class/variable declaration from a tab in the detached window, new tabs will be opened in the main IDE window. Overrides 'Open declaration source in the same tab'.",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isOpenDeclarationSourceFromDetachedWindowInMain,
                (c, v) -> c.setOpenDeclarationSourceFromDetachedWindowInMain((Boolean) v)));
        register(new AdvancedSettingItem("adv_tabs_double_click_hide_restore", "Editor Tabs",
                "Perform 'Hide All Tool Windows' / 'Restore Windows' with double-click on editor tab", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isPerformHideRestoreToolWindowsWithDoubleClickOnEditorTab,
                (c, v) -> c.setPerformHideRestoreToolWindowsWithDoubleClickOnEditorTab((Boolean) v)));
        register(new AdvancedSettingItem("adv_tabs_double_click_maximize_normalize", "Editor Tabs",
                "Perform 'Maximize Editor' / 'Normalize Splits' with double-click on editor tab", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isPerformMaximizeNormalizeSplitsWithDoubleClickOnEditorTab,
                (c, v) -> c.setPerformMaximizeNormalizeSplitsWithDoubleClickOnEditorTab((Boolean) v)));
        register(new AdvancedSettingItem("adv_tabs_equalize_proportions_splits", "Editor Tabs",
                "Equalize proportions in nested splits", "Adjust splitters to keep space equally distributed after split/unsplit.",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isEqualizeProportionsInNestedSplits,
                (c, v) -> c.setEqualizeProportionsInNestedSplits((Boolean) v)));
        register(new AdvancedSettingItem("adv_tabs_keep_pinned_left", "Editor Tabs",
                "Keep pinned tabs on the left", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isKeepPinnedTabsOnLeft,
                (c, v) -> c.setKeepPinnedTabsOnLeft((Boolean) v)));

        // 11. Find/Replace
        register(new AdvancedSettingItem("adv_find_similar_usages_clustering", "Find/Replace",
                "Enable similar usages clustering in Find Usages view", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isEnableSimilarUsagesClustering,
                (c, v) -> c.setEnableSimilarUsagesClustering((Boolean) v)));
        register(new AdvancedSettingItem("adv_find_max_results_preview", "Find/Replace",
                "Maximum number of results to show in Find in Files/Show Usages preview:", "Increasing this limit can significantly increase IDE memory usage",
                SettingType.INTEGER, 100, null, null,
                AppSettingsStore.AdvancedSettingsConfig::getMaxResultsInFindInFilesPreview,
                (c, v) -> c.setMaxResultsInFindInFilesPreview((Integer) v)));

        // 12. IDE
        register(new AdvancedSettingItem("adv_ide_max_recent_projects", "IDE",
                "Maximum number of recent projects:", null,
                SettingType.INTEGER, 50, null, null,
                AppSettingsStore.AdvancedSettingsConfig::getMaxRecentProjects,
                (c, v) -> c.setMaxRecentProjects((Integer) v)));
        register(new AdvancedSettingItem("adv_ide_max_recent_files", "IDE",
                "Maximum number of recent files:", null,
                SettingType.INTEGER, 50, null, null,
                AppSettingsStore.AdvancedSettingsConfig::getMaxRecentFiles,
                (c, v) -> c.setMaxRecentFiles((Integer) v)));
        register(new AdvancedSettingItem("adv_ide_max_recent_locations", "IDE",
                "Maximum number of recent locations:", null,
                SettingType.INTEGER, 25, null, null,
                AppSettingsStore.AdvancedSettingsConfig::getMaxRecentLocations,
                (c, v) -> c.setMaxRecentLocations((Integer) v)));
        register(new AdvancedSettingItem("adv_ide_local_history_duration", "IDE",
                "Duration of storing changes in Local History:", "Do not use Local History as the primary version control system since it may become corrupted if the IDE hangs",
                SettingType.INTEGER, 5, null, "days",
                AppSettingsStore.AdvancedSettingsConfig::getLocalHistoryDurationDays,
                (c, v) -> c.setLocalHistoryDurationDays((Integer) v)));

        // 13. Markdown
        register(new AdvancedSettingItem("adv_markdown_hide_floating_toolbar", "Markdown",
                "Hide floating toolbar", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isMarkdownHideFloatingToolbar,
                (c, v) -> c.setMarkdownHideFloatingToolbar((Boolean) v)));
        register(new AdvancedSettingItem("adv_markdown_squash_dashes_anchors", "Markdown",
                "Squash multiple dashes in header anchors", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSquashMultipleDashesInHeaderAnchors,
                (c, v) -> c.setSquashMultipleDashesInHeaderAnchors((Boolean) v)));

        // 14. Project View
        register(new AdvancedSettingItem("adv_project_view_increase_font", "Project View",
                "Increase font size in Project view", "Requires IDE restart",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isProjectViewIncreaseFontSize,
                (c, v) -> c.setProjectViewIncreaseFontSize((Boolean) v)));
        register(new AdvancedSettingItem("adv_project_view_enter_move_focus", "Project View",
                "Move focus to editor when Enter is pressed", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isProjectViewMoveFocusToEditorOnEnter,
                (c, v) -> c.setProjectViewMoveFocusToEditorOnEnter((Boolean) v)));
        register(new AdvancedSettingItem("adv_project_view_collapse_all_under", "Project View",
                "When collapsing a node, also collapse all expanded nodes under it", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isProjectViewCollapseAllExpandedUnderNode,
                (c, v) -> c.setProjectViewCollapseAllExpandedUnderNode((Boolean) v)));
        register(new AdvancedSettingItem("adv_project_view_scratches_consoles", "Project View",
                "Enable Scratches and Consoles in the Project view", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isEnableScratchesAndConsolesInProjectView,
                (c, v) -> c.setEnableScratchesAndConsolesInProjectView((Boolean) v)));

        // 15. Run/Debug
        register(new AdvancedSettingItem("adv_rundebug_temp_configs_limit", "Run/Debug",
                "Temporary configurations limit:", null,
                SettingType.INTEGER, 5, null, null,
                AppSettingsStore.AdvancedSettingsConfig::getTemporaryConfigurationsLimit,
                (c, v) -> c.setTemporaryConfigurationsLimit((Integer) v)));
        register(new AdvancedSettingItem("adv_rundebug_confirm_rerun_terminate", "Run/Debug",
                "Confirm rerun with process termination", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isConfirmRerunWithProcessTermination,
                (c, v) -> c.setConfirmRerunWithProcessTermination((Boolean) v)));
        register(new AdvancedSettingItem("adv_rundebug_configs_pinned_default", "Run/Debug",
                "Make configurations pinned by default", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isMakeConfigurationsPinnedByDefault,
                (c, v) -> c.setMakeConfigurationsPinnedByDefault((Boolean) v)));
        register(new AdvancedSettingItem("adv_rundebug_max_recent_configs", "Run/Debug",
                "Maximum number of recent run configurations:", null,
                SettingType.INTEGER, 5, null, null,
                AppSettingsStore.AdvancedSettingsConfig::getMaxRecentRunConfigurations,
                (c, v) -> c.setMaxRecentRunConfigurations((Integer) v)));

        // 16. SSH
        register(new AdvancedSettingItem("adv_ssh_config_files_parser", "SSH",
                "Configuration files parser:", "If you select Legacy, ~/.ssh/config is parsed the same way as in 2021.3 and previous releases. If you select OpenSSH, the config file is parsed using the 'ssh -G' command. In this case, OpenSSH must be installed in your system.",
                SettingType.COMBO, "OpenSSH", List.of("OpenSSH", "Legacy"), null,
                AppSettingsStore.AdvancedSettingsConfig::getSshConfigFilesParser,
                (c, v) -> c.setSshConfigFilesParser((String) v)));
        register(new AdvancedSettingItem("adv_ssh_custom_openssh_path", "SSH",
                "Custom path to an OpenSSH tool:", "This option is used only when the OpenSSH config parser is selected.",
                SettingType.TEXT, "ssh", null, null,
                AppSettingsStore.AdvancedSettingsConfig::getCustomPathToOpenSshTool,
                (c, v) -> c.setCustomPathToOpenSshTool((String) v)));

        // 17. Search Everywhere
        register(new AdvancedSettingItem("adv_se_wait_contributors", "Search Everywhere",
                "Wait for all contributors to finish before showing results", "Enabling this option will stop the search items to change their position in the displayed search results. This process might cause a delay.",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSearchEverywhereWaitForContributors,
                (c, v) -> c.setSearchEverywhereWaitForContributors((Boolean) v)));
        register(new AdvancedSettingItem("adv_se_show_notifications", "Search Everywhere",
                "Show notifications above search results", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSearchEverywhereShowNotificationsAboveResults,
                (c, v) -> c.setSearchEverywhereShowNotificationsAboveResults((Boolean) v)));
        register(new AdvancedSettingItem("adv_se_contributors_timeout", "Search Everywhere",
                "Contributors waiting timeout (ms):", null,
                SettingType.INTEGER, 2000, null, null,
                AppSettingsStore.AdvancedSettingsConfig::getSearchEverywhereContributorsTimeoutMs,
                (c, v) -> c.setSearchEverywhereContributorsTimeoutMs((Integer) v)));
        register(new AdvancedSettingItem("adv_se_recent_files_at_top", "Search Everywhere",
                "Show recent files at the top of results list", "When enabled, recent files are always displayed at the top of the results list. Therefore, even if these files match the search pattern less precisely than other results, they will remain at the top.",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSearchEverywhereShowRecentFilesAtTop,
                (c, v) -> c.setSearchEverywhereShowRecentFilesAtTop((Boolean) v)));
        register(new AdvancedSettingItem("adv_se_text_search_results", "Search Everywhere",
                "Show text search results in Search Everywhere", "Show text search results in the \"All\" tab and display the \"Text\" tab in Search Everywhere",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSearchEverywhereShowTextSearchResults,
                (c, v) -> c.setSearchEverywhereShowTextSearchResults((Boolean) v)));
        register(new AdvancedSettingItem("adv_se_sort_actions_ml", "Search Everywhere",
                "Sort results in the Actions tab based on machine learning", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSearchEverywhereSortActionsMl,
                (c, v) -> c.setSearchEverywhereSortActionsMl((Boolean) v)));
        register(new AdvancedSettingItem("adv_se_sort_files_ml", "Search Everywhere",
                "Sort results in the Files tab based on machine learning", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSearchEverywhereSortFilesMl,
                (c, v) -> c.setSearchEverywhereSortFilesMl((Boolean) v)));
        register(new AdvancedSettingItem("adv_se_sort_classes_ml", "Search Everywhere",
                "Sort results in the Classes tab based on machine learning", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSearchEverywhereSortClassesMl,
                (c, v) -> c.setSearchEverywhereSortClassesMl((Boolean) v)));
        register(new AdvancedSettingItem("adv_se_typo_tolerant", "Search Everywhere",
                "Enable typo-tolerant search in the Search Everywhere Action tab", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSearchEverywhereTypoTolerantSearch,
                (c, v) -> c.setSearchEverywhereTypoTolerantSearch((Boolean) v)));

        // 18. Search Scopes in Find, Replace, Rename
        register(new AdvancedSettingItem("adv_scopes_keep_last_selected", "Search Scopes in Find, Replace, Rename",
                "Keep last selected search scope", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isKeepLastSelectedSearchScope,
                (c, v) -> c.setKeepLastSelectedSearchScope((Boolean) v)));

        // 19. Startup
        register(new AdvancedSettingItem("adv_startup_open_readme", "Startup",
                "Open README.md file if there are no open files on project startup", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isOpenReadmeOnStartupIfNoOpenFiles,
                (c, v) -> c.setOpenReadmeOnStartupIfNoOpenFiles((Boolean) v)));

        // 20. Suggested refactoring
        register(new AdvancedSettingItem("adv_refactor_show_hint", "Suggested refactoring",
                "Show a hint for suggested refactoring in the editor", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isShowHintForSuggestedRefactoring,
                (c, v) -> c.setShowHintForSuggestedRefactoring((Boolean) v)));

        // 21. Terminal
        register(new AdvancedSettingItem("adv_terminal_scrollback_size", "Terminal",
                "Terminal scrollback buffer size:", null,
                SettingType.INTEGER, 5000, null, "lines",
                AppSettingsStore.AdvancedSettingsConfig::getTerminalScrollbackBufferSize,
                (c, v) -> c.setTerminalScrollbackBufferSize((Integer) v)));
        register(new AdvancedSettingItem("adv_terminal_typeahead", "Terminal",
                "Typeahead", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isTerminalTypeahead,
                (c, v) -> c.setTerminalTypeahead((Boolean) v)));
        register(new AdvancedSettingItem("adv_terminal_typeahead_threshold", "Terminal",
                "Typeahead latency threshold:", null,
                SettingType.INTEGER, 100, null, "ms",
                AppSettingsStore.AdvancedSettingsConfig::getTerminalTypeaheadLatencyThresholdMs,
                (c, v) -> c.setTerminalTypeaheadLatencyThresholdMs((Integer) v)));
        register(new AdvancedSettingItem("adv_terminal_line_spacing_alt_screen", "Terminal",
                "Use 1.0 line spacing for alternative screen buffer (mc, vim, nano, etc.)", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isTerminalLineSpacingAltScreenBuffer,
                (c, v) -> c.setTerminalLineSpacingAltScreenBuffer((Boolean) v)));
        register(new AdvancedSettingItem("adv_terminal_fill_char_background", "Terminal",
                "Fill character background including line spacing", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isTerminalFillCharacterBackgroundIncludingLineSpacing,
                (c, v) -> c.setTerminalFillCharacterBackgroundIncludingLineSpacing((Boolean) v)));
        register(new AdvancedSettingItem("adv_terminal_encoding", "Terminal",
                "Terminal character encoding:", null,
                SettingType.TEXT, "UTF-8", null, null,
                AppSettingsStore.AdvancedSettingsConfig::getTerminalCharacterEncoding,
                (c, v) -> c.setTerminalCharacterEncoding((String) v)));
        register(new AdvancedSettingItem("adv_terminal_output_capacity", "Terminal",
                "New terminal output capacity:", null,
                SettingType.INTEGER, 1024, null, "KB",
                AppSettingsStore.AdvancedSettingsConfig::getTerminalNewOutputCapacityKb,
                (c, v) -> c.setTerminalNewOutputCapacityKb((Integer) v)));
        register(new AdvancedSettingItem("adv_terminal_send_esc_cr_shift_enter", "Terminal",
                "Send Esc+CR on Shift+Enter", "Enables sending Esc+Carriage Return on Shift+Enter, allowing CLI applications to distinguish Shift+Enter from Enter.",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isTerminalSendEscCrOnShiftEnter,
                (c, v) -> c.setTerminalSendEscCrOnShiftEnter((Boolean) v)));

        // 22. Tool Windows
        register(new AdvancedSettingItem("adv_toolwindows_always_show_header_icons", "Tool Windows",
                "Always show tool window header icons", "If this option is disabled, tool window header icons are shown only on mouse hover or when the tool window is focused",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isAlwaysShowToolWindowHeaderIcons,
                (c, v) -> c.setAlwaysShowToolWindowHeaderIcons((Boolean) v)));
        register(new AdvancedSettingItem("adv_toolwindows_allow_dragging_by_header", "Tool Windows",
                "Allow dragging tool windows by header to move to another location", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isAllowDraggingToolWindowsByHeader,
                (c, v) -> c.setAllowDraggingToolWindowsByHeader((Boolean) v)));

        // 23. User Interface
        register(new AdvancedSettingItem("adv_ui_use_native_file_chooser", "User Interface",
                "Use native file chooser dialog on Windows/macOS", "When enabled, the IDE will use an OS-specific file chooser dialog instead of the custom one for opening files/directories.",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isUseNativeFileChooserDialog,
                (c, v) -> c.setUseNativeFileChooserDialog((Boolean) v)));
        register(new AdvancedSettingItem("adv_ui_merge_menu_with_title", "User Interface",
                "Merge main menu with window title", "Requires restart",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isMergeMainMenuWithWindowTitle,
                (c, v) -> c.setMergeMainMenuWithWindowTitle((Boolean) v)));
        register(new AdvancedSettingItem("adv_ui_show_file_type_icon_frame_header", "User Interface",
                "Show file type icon in IDE frame header", "On macOS, the icon can be used to drag a file to a different application",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isShowFileTypeIconInIdeFrameHeader,
                (c, v) -> c.setShowFileTypeIconInIdeFrameHeader((Boolean) v)));
        register(new AdvancedSettingItem("adv_ui_words_instead_of_symbols_mac", "User Interface",
                "Use words instead of symbols for macOS keyboard shortcuts", "Change shortcut symbols ⇧, ⌫, ⌥, and others to Esc, Backspace, Option, etc",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isUseWordsInsteadOfSymbolsForMacShortcuts,
                (c, v) -> c.setUseWordsInsteadOfSymbolsForMacShortcuts((Boolean) v)));
        register(new AdvancedSettingItem("adv_ui_activate_menu_right_click", "User Interface",
                "Activate menu items on right mouse button release", "For macOS and Linux only",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isActivateMenuItemsOnRightMouseButtonRelease,
                (c, v) -> c.setActivateMenuItemsOnRightMouseButtonRelease((Boolean) v)));
        register(new AdvancedSettingItem("adv_ui_cyclic_scrolling", "User Interface",
                "Cyclic scrolling in lists and trees", "Select the last element when pressing Up on the first element, and vice versa",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isCyclicScrollingInListsAndTrees,
                (c, v) -> c.setCyclicScrollingInListsAndTrees((Boolean) v)));
        register(new AdvancedSettingItem("adv_ui_position_mouse_default_button", "User Interface",
                "Position mouse cursor on default button in dialogs", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isPositionMouseCursorOnDefaultButtonInDialogs,
                (c, v) -> c.setPositionMouseCursorOnDefaultButtonInDialogs((Boolean) v)));
        register(new AdvancedSettingItem("adv_ui_disable_double_modifier_shortcuts", "User Interface",
                "Disable double modifier key shortcuts", "Shift-Shift for Search Everywhere, Ctrl-Ctrl for Run Anything",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isDisableDoubleModifierKeyShortcuts,
                (c, v) -> c.setDisableDoubleModifierKeyShortcuts((Boolean) v)));
        register(new AdvancedSettingItem("adv_ui_show_settings_window_editor_tab", "User Interface",
                "Show settings window as an editor tab", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isShowSettingsWindowAsEditorTab,
                (c, v) -> c.setShowSettingsWindowAsEditorTab((Boolean) v)));

        // 24. Version Control
        register(new AdvancedSettingItem("adv_vcs_open_diff_editor_tab", "Version Control",
                "Open Diff as Editor Tab", "If enabled, Diff is shown in an editor tab instead of a separate window",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isOpenDiffAsEditorTab,
                (c, v) -> c.setOpenDiffAsEditorTab((Boolean) v)));
        register(new AdvancedSettingItem("adv_vcs_all_in_one_diff", "Version Control",
                "All-in-One Diff", "If enabled, all changed files are shown in a single scrollable Diff view",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isVcsAllInOneDiff,
                (c, v) -> c.setVcsAllInOneDiff((Boolean) v)));
        register(new AdvancedSettingItem("adv_vcs_load_annotations_on_open", "Version Control",
                "Load file annotations from VCS when file is opened in editor", "This allows showing them faster on demand",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isLoadFileAnnotationsFromVcsOnOpen,
                (c, v) -> c.setLoadFileAnnotationsFromVcsOnOpen((Boolean) v)));
        register(new AdvancedSettingItem("adv_vcs_highlight_ignored_files", "Version Control",
                "Highlight ignored files", "Request the list of the ignored files from the VCS and show their status in the IDE",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isHighlightIgnoredFiles,
                (c, v) -> c.setHighlightIgnoredFiles((Boolean) v)));
        register(new AdvancedSettingItem("adv_vcs_enable_commit_tool_window", "Version Control",
                "Enable Commit tool window", "Show the Local Changes and Shelf tabs in the separate Commit tool window",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isEnableCommitToolWindow,
                (c, v) -> c.setEnableCommitToolWindow((Boolean) v)));
        register(new AdvancedSettingItem("adv_vcs_toggle_commit_controls", "Version Control",
                "Toggle commit controls", "Hide commit panel and checkboxes after commit is performed and allow toggling commit UI in Local Changes",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isToggleCommitControls,
                (c, v) -> c.setToggleCommitControls((Boolean) v)));
        register(new AdvancedSettingItem("adv_vcs_auto_close_commit_tool_window", "Version Control",
                "Automatically close the Commit tool window in Window/Float mode after committing", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isAutoCloseCommitToolWindowInFloatMode,
                (c, v) -> c.setAutoCloseCommitToolWindowInFloatMode((Boolean) v)));
        register(new AdvancedSettingItem("adv_vcs_select_all_repos_push", "Version Control",
                "Select all repositories with new commits for push", "Preselect all repositories with commits to be pushed by default",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isSelectAllReposWithNewCommitsForPush,
                (c, v) -> c.setSelectAllReposWithNewCommitsForPush((Boolean) v)));
        register(new AdvancedSettingItem("adv_vcs_no_warning_force_pushing", "Version Control",
                "Do not show a warning when force pushing", "When force pushing to unprotected branches, no confirmation will be asked",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isDoNotShowWarningWhenForcePushing,
                (c, v) -> c.setDoNotShowWarningWhenForcePushing((Boolean) v)));
        register(new AdvancedSettingItem("adv_vcs_modal_commit_interface", "Version Control",
                "Use modal commit interface for Git and Mercurial", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isUseModalCommitInterface,
                (c, v) -> c.setUseModalCommitInterface((Boolean) v)));

        // 25. Version Control. Git
        register(new AdvancedSettingItem("adv_git_recursively_clone_submodules", "Version Control. Git",
                "Recursively clone submodules in the project", "When enabled, Checkout from Git calls 'git clone --recurse-submodules', i.e. clones the main repository with all submodules if there are any",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isRecursivelyCloneSubmodulesInProject,
                (c, v) -> c.setRecursivelyCloneSubmodulesInProject((Boolean) v)));
        register(new AdvancedSettingItem("adv_git_apply_content_transformation", "Version Control. Git",
                "Apply content transformation when reading from Git:", "Use '--filters' or '--textconv' flags for 'git cat-file' command when reading file content from Git",
                SettingType.COMBO, "Filters", List.of("None", "Filters", "TextConv"), null,
                AppSettingsStore.AdvancedSettingsConfig::getApplyContentTransformationReadingFromGit,
                (c, v) -> c.setApplyContentTransformationReadingFromGit((String) v)));
        register(new AdvancedSettingItem("adv_git_safe_force_push", "Version Control. Git",
                "Use Safe Force Push", "Use '--force-with-lease' when calling force push from IDE instead of just '--force'",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isUseSafeForcePush,
                (c, v) -> c.setUseSafeForcePush((Boolean) v)));
        register(new AdvancedSettingItem("adv_git_check_incoming_outgoing_commits", "Version Control. Git",
                "Check for incoming and outgoing commits", "Show incoming and outgoing commits for branches",
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isCheckForIncomingOutgoingCommits,
                (c, v) -> c.setCheckForIncomingOutgoingCommits((Boolean) v)));
        register(new AdvancedSettingItem("adv_git_auto_open_update_info_tab", "Version Control. Git",
                "Automatically open the 'Update Info' tab after updating the project", null,
                SettingType.BOOLEAN, true, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isAutoOpenUpdateInfoTabAfterUpdate,
                (c, v) -> c.setAutoOpenUpdateInfoTabAfterUpdate((Boolean) v)));
        register(new AdvancedSettingItem("adv_git_do_not_run_commit_hooks", "Version Control. Git",
                "Do not run Git commit hooks", "When enabled, commit always runs with '--no-verify' option",
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isDoNotRunGitCommitHooks,
                (c, v) -> c.setDoNotRunGitCommitHooks((Boolean) v)));
        register(new AdvancedSettingItem("adv_git_branch_name_cleanup_symbol", "Version Control. Git",
                "Branch name cleanup symbol:", "Symbol that will be used instead prohibited symbols in Git branch names",
                SettingType.TEXT, "-", null, null,
                AppSettingsStore.AdvancedSettingsConfig::getBranchNameCleanupSymbol,
                (c, v) -> c.setBranchNameCleanupSymbol((String) v)));
        register(new AdvancedSettingItem("adv_git_show_branch_names_recent_projects", "Version Control. Git",
                "Show branch names in the list of recent projects:", "Branch names will be shown on the Welcome screen and in the list of recent projects in the main menu",
                SettingType.COMBO, "All projects", List.of("Never", "Projects with identical names", "All projects"), null,
                AppSettingsStore.AdvancedSettingsConfig::getShowBranchNamesInRecentProjects,
                (c, v) -> c.setShowBranchNamesInRecentProjects((String) v)));
        register(new AdvancedSettingItem("adv_git_show_git_tab_in_search_everywhere", "Version Control. Git",
                "Show Git tab in Search Everywhere", null,
                SettingType.BOOLEAN, false, null, null,
                AppSettingsStore.AdvancedSettingsConfig::isShowGitTabInSearchEverywhere,
                (c, v) -> c.setShowGitTabInSearchEverywhere((Boolean) v)));
    }

    public static void register(AdvancedSettingItem item) {
        REGISTRY.add(item);
    }

    public static List<AdvancedSettingItem> getRegistry() {
        return Collections.unmodifiableList(REGISTRY);
    }

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    private static class SettingRowView {
        final AdvancedSettingItem item;
        final VBox node;
        final Object control;

        SettingRowView(AdvancedSettingItem item, VBox node, Object control) {
            this.item = item;
            this.node = node;
            this.control = control;
        }

        Object getCurrentValue() {
            if (control instanceof CheckBox cb) {
                return cb.isSelected();
            } else if (control instanceof ComboBox<?> combo) {
                return combo.getValue();
            } else if (control instanceof TextField tf) {
                if (item.getType() == SettingType.INTEGER) {
                    try {
                        return Integer.parseInt(tf.getText().trim());
                    } catch (NumberFormatException e) {
                        return item.getDefaultValue();
                    }
                }
                return tf.getText();
            }
            return null;
        }

        boolean isModified() {
            return !Objects.equals(getCurrentValue(), item.getDefaultValue());
        }

        boolean matchesQuery(String query) {
            if (query == null || query.isBlank()) return true;
            String q = query.toLowerCase();
            if (item.getTitle().toLowerCase().contains(q)) return true;
            if (item.getGroup().toLowerCase().contains(q)) return true;
            if (item.getSubtext() != null && item.getSubtext().toLowerCase().contains(q)) return true;
            return false;
        }
    }

    private static class GroupSectionView {
        final String groupName;
        final HBox headerNode;
        final VBox container;
        final List<SettingRowView> rowViews = new ArrayList<>();

        GroupSectionView(String groupName, HBox headerNode, VBox container) {
            this.groupName = groupName;
            this.headerNode = headerNode;
            this.container = container;
        }

        boolean applyFilter(String query, boolean showModifiedOnly) {
            boolean anyVisible = false;
            for (SettingRowView row : rowViews) {
                boolean matchesQ = row.matchesQuery(query);
                boolean matchesMod = !showModifiedOnly || row.isModified();
                boolean visible = matchesQ && matchesMod;
                row.node.setVisible(visible);
                row.node.setManaged(visible);
                if (visible) {
                    anyVisible = true;
                }
            }
            headerNode.setVisible(anyVisible);
            headerNode.setManaged(anyVisible);
            container.setVisible(anyVisible);
            container.setManaged(anyVisible);
            return anyVisible;
        }
    }

    public AdvancedSettingsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.AdvancedSettingsConfig cfg = settings.getAdvancedSettings();

        VBox root = new VBox(12);
        root.setPadding(new Insets(10, 16, 20, 16));
        root.setStyle("-fx-background-color: transparent;");

        // Top Search Bar matching DataGrip
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search advanced settings");
        searchField.setPrefWidth(400);
        searchField.setStyle("-fx-background-color: -control-bg; -fx-text-fill: -text; -fx-prompt-text-fill: -text-dim; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        inputs.put("adv_search_field", searchField);

        CheckBox showModifiedCheck = new CheckBox("Show modified only");
        showModifiedCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
        inputs.put("adv_show_modified_only", showModifiedCheck);

        topBar.getChildren().addAll(searchField, showModifiedCheck);

        // Group the registry items by group preserves order of discovery
        Map<String, List<AdvancedSettingItem>> groups = new LinkedHashMap<>();
        for (AdvancedSettingItem item : REGISTRY) {
            groups.computeIfAbsent(item.getGroup(), k -> new ArrayList<>()).add(item);
        }

        List<GroupSectionView> sectionViews = new ArrayList<>();
        VBox sectionsContainer = new VBox(14);
        sectionsContainer.setStyle("-fx-background-color: transparent;");

        for (Map.Entry<String, List<AdvancedSettingItem>> entry : groups.entrySet()) {
            String groupName = entry.getKey();
            List<AdvancedSettingItem> items = entry.getValue();

            // Group Header with extending divider line
            HBox groupHeader = new HBox(8);
            groupHeader.setAlignment(Pos.CENTER_LEFT);
            groupHeader.setPadding(new Insets(6, 0, 2, 0));

            Label groupTitleLbl = new Label(groupName);
            groupTitleLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

            Region line = new Region();
            HBox.setHgrow(line, Priority.ALWAYS);
            line.setPrefHeight(1);
            line.setMaxHeight(1);
            line.setStyle("-fx-background-color: -border-color; -fx-opacity: 0.6;");

            groupHeader.getChildren().addAll(groupTitleLbl, line);

            VBox groupContainer = new VBox(6);
            groupContainer.setStyle("-fx-background-color: transparent;");

            GroupSectionView sectionView = new GroupSectionView(groupName, groupHeader, groupContainer);

            for (AdvancedSettingItem item : items) {
                Object currentVal = item.getGetter().apply(cfg);
                VBox itemRow = new VBox(3);
                itemRow.setPadding(new Insets(2, 0, 4, 12));

                Object control = null;

                switch (item.getType()) {
                    case BOOLEAN -> {
                        CheckBox cb = new CheckBox(item.getTitle());
                        cb.setSelected(Boolean.TRUE.equals(currentVal));
                        cb.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
                        inputs.put(item.getId(), cb);
                        control = cb;
                        itemRow.getChildren().add(cb);

                        if (item.getSubtext() != null && !item.getSubtext().isBlank()) {
                            Label sub = new Label(item.getSubtext());
                            sub.setWrapText(true);
                            sub.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");
                            itemRow.getChildren().add(sub);
                        }
                    }
                    case COMBO -> {
                        HBox comboRow = new HBox(8);
                        comboRow.setAlignment(Pos.CENTER_LEFT);

                        Label lbl = new Label(item.getTitle());
                        lbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

                        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(item.getOptions()));
                        combo.setValue(currentVal != null ? currentVal.toString() : (String) item.getDefaultValue());
                        combo.setStyle("-fx-background-color: -control-bg; -fx-border-color: -border-color; -fx-border-radius: 4px;");
                        inputs.put(item.getId(), combo);
                        control = combo;

                        comboRow.getChildren().addAll(lbl, combo);
                        itemRow.getChildren().add(comboRow);

                        if (item.getSubtext() != null && !item.getSubtext().isBlank()) {
                            Label sub = new Label(item.getSubtext());
                            sub.setWrapText(true);
                            sub.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 0 0 0 0;");
                            itemRow.getChildren().add(sub);
                        }
                    }
                    case INTEGER -> {
                        HBox intRow = new HBox(8);
                        intRow.setAlignment(Pos.CENTER_LEFT);

                        Label lbl = new Label(item.getTitle());
                        lbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

                        TextField tf = new TextField(currentVal != null ? currentVal.toString() : String.valueOf(item.getDefaultValue()));
                        tf.setPrefWidth(75);
                        tf.setStyle("-fx-background-color: -control-bg; -fx-text-fill: -text; -fx-border-color: -border-color; -fx-border-radius: 4px;");
                        inputs.put(item.getId(), tf);
                        control = tf;

                        intRow.getChildren().addAll(lbl, tf);

                        if (item.getUnit() != null && !item.getUnit().isBlank()) {
                            Label unitLbl = new Label(item.getUnit());
                            unitLbl.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");
                            intRow.getChildren().add(unitLbl);
                        }

                        itemRow.getChildren().add(intRow);

                        if (item.getSubtext() != null && !item.getSubtext().isBlank()) {
                            Label sub = new Label(item.getSubtext());
                            sub.setWrapText(true);
                            sub.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
                            itemRow.getChildren().add(sub);
                        }
                    }
                    case TEXT -> {
                        HBox textRow = new HBox(8);
                        textRow.setAlignment(Pos.CENTER_LEFT);

                        Label lbl = new Label(item.getTitle());
                        lbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

                        TextField tf = new TextField(currentVal != null ? currentVal.toString() : (String) item.getDefaultValue());
                        tf.setPrefWidth(320);
                        tf.setStyle("-fx-background-color: -control-bg; -fx-text-fill: -text; -fx-border-color: -border-color; -fx-border-radius: 4px;");
                        inputs.put(item.getId(), tf);
                        control = tf;

                        textRow.getChildren().addAll(lbl, tf);
                        itemRow.getChildren().add(textRow);

                        if (item.getSubtext() != null && !item.getSubtext().isBlank()) {
                            Label sub = new Label(item.getSubtext());
                            sub.setWrapText(true);
                            sub.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
                            itemRow.getChildren().add(sub);
                        }
                    }
                }

                SettingRowView rowView = new SettingRowView(item, itemRow, control);
                sectionView.rowViews.add(rowView);
                groupContainer.getChildren().add(itemRow);

                // Add change listeners to trigger re-filter if showModifiedOnly is active
                if (control instanceof CheckBox cb) {
                    cb.selectedProperty().addListener((obs, oldV, newV) -> {
                        if (showModifiedCheck.isSelected()) {
                            applyFilter(searchField.getText(), showModifiedCheck.isSelected(), sectionViews);
                        }
                    });
                } else if (control instanceof ComboBox<?> combo) {
                    combo.valueProperty().addListener((obs, oldV, newV) -> {
                        if (showModifiedCheck.isSelected()) {
                            applyFilter(searchField.getText(), showModifiedCheck.isSelected(), sectionViews);
                        }
                    });
                } else if (control instanceof TextField tf) {
                    tf.textProperty().addListener((obs, oldV, newV) -> {
                        if (showModifiedCheck.isSelected()) {
                            applyFilter(searchField.getText(), showModifiedCheck.isSelected(), sectionViews);
                        }
                    });
                }
            }

            sectionViews.add(sectionView);
            sectionsContainer.getChildren().addAll(groupHeader, groupContainer);
        }

        // Live filtering listener
        searchField.textProperty().addListener((obs, oldVal, newVal) ->
                applyFilter(newVal, showModifiedCheck.isSelected(), sectionViews));

        showModifiedCheck.selectedProperty().addListener((obs, oldVal, newVal) ->
                applyFilter(searchField.getText(), newVal, sectionViews));

        ScrollPane scroll = new ScrollPane(sectionsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(topBar, scroll);
        return root;
    }

    private static void applyFilter(String query, boolean showModifiedOnly, List<GroupSectionView> sectionViews) {
        for (GroupSectionView section : sectionViews) {
            section.applyFilter(query, showModifiedOnly);
        }
    }

    /**
     * Helper to apply input registry values into target settings' AdvancedSettingsConfig.
     */
    public static void applySettings(Map<String, Object> inputs, AppSettingsStore.Settings targetSettings) {
        if (inputs == null || targetSettings == null) return;
        AppSettingsStore.AdvancedSettingsConfig cfg = targetSettings.getAdvancedSettings();

        for (AdvancedSettingItem item : REGISTRY) {
            if (!inputs.containsKey(item.getId())) continue;
            Object ctrl = inputs.get(item.getId());
            if (ctrl instanceof CheckBox cb) {
                item.getSetter().accept(cfg, cb.isSelected());
            } else if (ctrl instanceof ComboBox<?> combo && combo.getValue() != null) {
                item.getSetter().accept(cfg, combo.getValue().toString());
            } else if (ctrl instanceof TextField tf) {
                if (item.getType() == SettingType.INTEGER) {
                    try {
                        item.getSetter().accept(cfg, Integer.parseInt(tf.getText().trim()));
                    } catch (NumberFormatException ignored) {}
                } else {
                    item.getSetter().accept(cfg, tf.getText().trim());
                }
            }
        }
    }
}

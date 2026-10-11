package com.roze.dbnavigator.ui.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.SettingsDialog;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ToolsSettingsTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX already started
        }
    }

    @Test
    public void testBuildToolsDefaultsAndCopy() {
        AppSettingsStore.BuildToolsConfig cfg = new AppSettingsStore.BuildToolsConfig();
        assertTrue(cfg.isSyncProjectAfterChanges());
        assertEquals("External changes", cfg.getSyncTrigger());

        AppSettingsStore.BuildToolsConfig copy = cfg.copy();
        copy.setSyncProjectAfterChanges(false);
        copy.setSyncTrigger("Any changes");

        assertTrue(cfg.isSyncProjectAfterChanges());
        assertEquals("External changes", cfg.getSyncTrigger());
        assertFalse(copy.isSyncProjectAfterChanges());
        assertEquals("Any changes", copy.getSyncTrigger());
    }

    @Test
    public void testActionsOnSaveDefaultsAndCopy() {
        AppSettingsStore.ActionsOnSaveConfig cfg = new AppSettingsStore.ActionsOnSaveConfig();
        assertFalse(cfg.isReformatCode());
        assertTrue(cfg.isAllFileTypes());
        assertNotNull(cfg.getSelectedFileTypes());
        assertEquals(14, cfg.getSelectedFileTypes().size());
        assertEquals("Any save", cfg.getActivatedOn());

        AppSettingsStore.ActionsOnSaveConfig copy = cfg.copy();
        copy.setReformatCode(true);
        copy.setAllFileTypes(false);
        copy.setSelectedFileTypes(List.of("SQL", "JSON"));
        copy.setActivatedOn("Explicit save only");

        assertFalse(cfg.isReformatCode());
        assertTrue(cfg.isAllFileTypes());
        assertEquals(14, cfg.getSelectedFileTypes().size());
        assertEquals("Any save", cfg.getActivatedOn());

        assertTrue(copy.isReformatCode());
        assertFalse(copy.isAllFileTypes());
        assertEquals(2, copy.getSelectedFileTypes().size());
        assertEquals("Explicit save only", copy.getActivatedOn());
    }

    @Test
    public void testCoverageDefaultsAndCopy() {
        AppSettingsStore.CoverageConfig cfg = new AppSettingsStore.CoverageConfig();
        assertEquals("Show options before applying coverage to the editor", cfg.getWhenNewCoverageGathered());
        assertTrue(cfg.isActivateCoverageView());
        assertTrue(cfg.isShowCoverageInProjectView());

        AppSettingsStore.CoverageConfig copy = cfg.copy();
        copy.setWhenNewCoverageGathered("Do not apply collected coverage");
        copy.setActivateCoverageView(false);
        copy.setShowCoverageInProjectView(false);

        assertEquals("Show options before applying coverage to the editor", cfg.getWhenNewCoverageGathered());
        assertTrue(cfg.isActivateCoverageView());
        assertTrue(cfg.isShowCoverageInProjectView());

        assertEquals("Do not apply collected coverage", copy.getWhenNewCoverageGathered());
        assertFalse(copy.isActivateCoverageView());
        assertFalse(copy.isShowCoverageInProjectView());
    }

    @Test
    public void testDebuggerDefaultsAndCopy() {
        AppSettingsStore.DebuggerConfig cfg = new AppSettingsStore.DebuggerConfig();
        assertTrue(cfg.isShowDebugWindowOnBreakpoint());
        assertTrue(cfg.isFocusApplicationOnBreakpoint());
        assertFalse(cfg.isHideDebugWindowOnProcessTermination());
        assertFalse(cfg.isScrollExecutionPointToCenter());
        assertTrue(cfg.isClickLineNumberToRunToCursor());
        assertEquals("Click with left mouse button", cfg.getRemoveBreakpointMode());
        assertFalse(cfg.isConfirmRemovalOfConditionalOrLoggingBreakpoints());

        AppSettingsStore.DebuggerConfig copy = cfg.copy();
        copy.setShowDebugWindowOnBreakpoint(false);
        copy.setFocusApplicationOnBreakpoint(false);
        copy.setHideDebugWindowOnProcessTermination(true);
        copy.setScrollExecutionPointToCenter(true);
        copy.setClickLineNumberToRunToCursor(false);
        copy.setRemoveBreakpointMode("Drag to the editor or click with middle mouse button");
        copy.setConfirmRemovalOfConditionalOrLoggingBreakpoints(true);

        assertTrue(cfg.isShowDebugWindowOnBreakpoint());
        assertFalse(copy.isShowDebugWindowOnBreakpoint());
        assertFalse(copy.isFocusApplicationOnBreakpoint());
        assertTrue(copy.isHideDebugWindowOnProcessTermination());
        assertTrue(copy.isScrollExecutionPointToCenter());
        assertFalse(copy.isClickLineNumberToRunToCursor());
        assertEquals("Drag to the editor or click with middle mouse button", copy.getRemoveBreakpointMode());
        assertTrue(copy.isConfirmRemovalOfConditionalOrLoggingBreakpoints());
    }

    @Test
    public void testDebuggerDataViewsDefaultsAndCopy() {
        AppSettingsStore.DebuggerDataViewsConfig cfg = new AppSettingsStore.DebuggerDataViewsConfig();
        assertFalse(cfg.isSortValuesAlphabetically());
        assertTrue(cfg.isEnableAutoExpressions());
        assertTrue(cfg.isShowValuesInline());
        assertTrue(cfg.isShowValueTooltip());
        assertEquals(700, cfg.getValueTooltipDelayMs());
        assertFalse(cfg.isShowValueTooltipOnCodeSelection());

        AppSettingsStore.DebuggerDataViewsConfig copy = cfg.copy();
        copy.setSortValuesAlphabetically(true);
        copy.setEnableAutoExpressions(false);
        copy.setShowValuesInline(false);
        copy.setShowValueTooltip(false);
        copy.setValueTooltipDelayMs(1200);
        copy.setShowValueTooltipOnCodeSelection(true);

        assertFalse(cfg.isSortValuesAlphabetically());
        assertTrue(copy.isSortValuesAlphabetically());
        assertFalse(copy.isEnableAutoExpressions());
        assertFalse(copy.isShowValuesInline());
        assertFalse(copy.isShowValueTooltip());
        assertEquals(1200, copy.getValueTooltipDelayMs());
        assertTrue(copy.isShowValueTooltipOnCodeSelection());
    }

    @Test
    public void testDebuggerSteppingDefaultsAndCopy() {
        AppSettingsStore.DebuggerSteppingConfig cfg = new AppSettingsStore.DebuggerSteppingConfig();
        assertEquals("Graceful", cfg.getSteppingMode());
        assertFalse(cfg.isPauseAtBegin());

        AppSettingsStore.DebuggerSteppingConfig copy = cfg.copy();
        copy.setSteppingMode("Step into");
        copy.setPauseAtBegin(true);

        assertEquals("Graceful", cfg.getSteppingMode());
        assertFalse(cfg.isPauseAtBegin());
        assertEquals("Step into", copy.getSteppingMode());
        assertTrue(copy.isPauseAtBegin());
    }

    @Test
    public void testDiagramsDefaultsAndCopy() {
        AppSettingsStore.DiagramsConfig cfg = new AppSettingsStore.DiagramsConfig();
        assertNotNull(cfg.getEnabledCategories());
        assertEquals(9, cfg.getEnabledCategories().size());
        assertEquals("All", cfg.getDefaultScope());
        assertEquals("With types aligned", cfg.getNodeItemStyle());
        assertEquals(60, cfg.getShortenNodeItemsLength());
        assertTrue(cfg.isShowGridByDefault());
        assertTrue(cfg.isEnableAnimations());
        assertTrue(cfg.isEnableNodeItemsSyntaxHighlighting());
        assertEquals("Hierarchic Compact", cfg.getDefaultLayout());
        assertEquals("With light layouter", cfg.getLayoutOnCategorySwitch());
        assertEquals(1500, cfg.getLayoutAnimationDuration());
        assertTrue(cfg.isFitContentAfterLayout());
        assertTrue(cfg.isRelayoutWhenNewElementsAdded());

        AppSettingsStore.DiagramsConfig copy = cfg.copy();
        copy.setEnabledCategories(Set.of("Columns", "Details"));
        copy.setDefaultScope("Current Schema");
        copy.setNodeItemStyle("Compact");
        copy.setShortenNodeItemsLength(80);
        copy.setShowGridByDefault(false);
        copy.setEnableAnimations(false);
        copy.setEnableNodeItemsSyntaxHighlighting(false);
        copy.setDefaultLayout("Circular");
        copy.setLayoutOnCategorySwitch("Relayout all");
        copy.setLayoutAnimationDuration(2000);
        copy.setFitContentAfterLayout(false);
        copy.setRelayoutWhenNewElementsAdded(false);

        assertEquals(9, cfg.getEnabledCategories().size());
        assertEquals(2, copy.getEnabledCategories().size());
        assertEquals("Current Schema", copy.getDefaultScope());
        assertEquals("Compact", copy.getNodeItemStyle());
        assertEquals(80, copy.getShortenNodeItemsLength());
        assertFalse(copy.isShowGridByDefault());
        assertFalse(copy.isEnableAnimations());
        assertFalse(copy.isEnableNodeItemsSyntaxHighlighting());
        assertEquals("Circular", copy.getDefaultLayout());
        assertEquals("Relayout all", copy.getLayoutOnCategorySwitch());
        assertEquals(2000, copy.getLayoutAnimationDuration());
        assertFalse(copy.isFitContentAfterLayout());
        assertFalse(copy.isRelayoutWhenNewElementsAdded());
    }

    @Test
    public void testDiffMergeDefaultsAndCopy() {
        AppSettingsStore.DiffMergeConfig cfg = new AppSettingsStore.DiffMergeConfig();
        assertEquals("4", cfg.getContextLines());
        assertTrue(cfg.isGoToNextFileAfterLastChange());
        assertEquals("Until the diff is closed", cfg.getIncludeDiffsInNavigationHistory());
        assertFalse(cfg.isAutoApplyNonConflictingChanges());
        assertFalse(cfg.isAutoResolveConflictsInImports());
        assertTrue(cfg.isHighlightModifiedLinesInGutter());

        AppSettingsStore.DiffMergeConfig copy = cfg.copy();
        copy.setContextLines("Disable");
        copy.setGoToNextFileAfterLastChange(false);
        copy.setIncludeDiffsInNavigationHistory("Always");
        copy.setAutoApplyNonConflictingChanges(true);
        copy.setAutoResolveConflictsInImports(true);
        copy.setHighlightModifiedLinesInGutter(false);

        assertEquals("4", cfg.getContextLines());
        assertTrue(cfg.isGoToNextFileAfterLastChange());
        assertEquals("Disable", copy.getContextLines());
        assertFalse(copy.isGoToNextFileAfterLastChange());
        assertEquals("Always", copy.getIncludeDiffsInNavigationHistory());
        assertTrue(copy.isAutoApplyNonConflictingChanges());
        assertTrue(copy.isAutoResolveConflictsInImports());
        assertFalse(copy.isHighlightModifiedLinesInGutter());
    }

    @Test
    public void testExternalDiffToolsDefaultsAndCopy() {
        AppSettingsStore.ExternalDiffToolsConfig cfg = new AppSettingsStore.ExternalDiffToolsConfig();
        assertFalse(cfg.isEnableExternalTools());
        assertNotNull(cfg.getCustomTools());
        assertTrue(cfg.getCustomTools().isEmpty());
        assertNotNull(cfg.getAssociations());
        assertEquals(1, cfg.getAssociations().size());
        assertEquals("Default", cfg.getAssociations().get(0).getFileType());
        assertEquals("Built-in", cfg.getAssociations().get(0).getDiffTool());
        assertEquals("Built-in", cfg.getAssociations().get(0).getMergeTool());

        AppSettingsStore.ExternalDiffToolsConfig copy = cfg.copy();
        copy.setEnableExternalTools(true);
        copy.setCustomTools(List.of(new AppSettingsStore.ExternalToolDefinition("KDiff3", "/usr/bin/kdiff3", "%1 %2", "%1 %2 %3 %4")));
        copy.setAssociations(List.of(new AppSettingsStore.FileTypeToolAssociation("*.sql", "KDiff3", "KDiff3")));

        assertFalse(cfg.isEnableExternalTools());
        assertTrue(cfg.getCustomTools().isEmpty());
        assertEquals(1, cfg.getAssociations().size());

        assertTrue(copy.isEnableExternalTools());
        assertEquals(1, copy.getCustomTools().size());
        assertEquals("KDiff3", copy.getCustomTools().get(0).getName());
        assertEquals(1, copy.getAssociations().size());
        assertEquals("*.sql", copy.getAssociations().get(0).getFileType());
    }

    @Test
    public void testExternalToolsDefaultsAndCopy() {
        AppSettingsStore.ExternalToolsConfig cfg = new AppSettingsStore.ExternalToolsConfig();
        assertNotNull(cfg.getTools());
        assertTrue(cfg.getTools().isEmpty());

        AppSettingsStore.ExternalToolItem item = new AppSettingsStore.ExternalToolItem("Format SQL", "Format", "Formats sql files", "sql-formatter", "-i $FilePath$", "$FileDir$");
        item.setSynchronizeFiles(true);
        item.setOpenConsole(true);
        item.setConsoleActiveOnStdout(true);
        item.setConsoleActiveOnStderr(false);
        item.setOutputFilters("(?<FILE>.*):(?<LINE>\\d+):");

        AppSettingsStore.ExternalToolsConfig copy = cfg.copy();
        copy.setTools(List.of(item));

        assertTrue(cfg.getTools().isEmpty());
        assertEquals(1, copy.getTools().size());
        assertEquals("Format SQL", copy.getTools().get(0).getName());
        assertEquals("Format", copy.getTools().get(0).getGroup());
        assertEquals("sql-formatter", copy.getTools().get(0).getProgram());
        assertTrue(copy.getTools().get(0).isConsoleActiveOnStdout());
    }

    @Test
    public void testMcpServerDefaultsAndCopy() {
        AppSettingsStore.McpServerConfig cfg = new AppSettingsStore.McpServerConfig();
        assertFalse(cfg.isEnableMcpServer());
        assertNotNull(cfg.getEnabledExposedTools());
        assertEquals(26, cfg.getEnabledExposedTools().size());
        assertTrue(cfg.getEnabledExposedTools().contains("AnalysisToolset.build_project"));
        assertTrue(cfg.getEnabledExposedTools().contains("VcsToolset.get_repositories"));

        AppSettingsStore.McpServerConfig copy = cfg.copy();
        copy.setEnableMcpServer(true);
        copy.setEnabledExposedTools(Set.of("AnalysisToolset.build_project"));

        assertFalse(cfg.isEnableMcpServer());
        assertEquals(26, cfg.getEnabledExposedTools().size());
        assertTrue(copy.isEnableMcpServer());
        assertEquals(1, copy.getEnabledExposedTools().size());
    }

    @Test
    public void testToolsJsonSerializationRoundTrip() throws Exception {
        AppSettingsStore.Settings original = new AppSettingsStore.Settings();
        original.getBuildTools().setSyncProjectAfterChanges(false);
        original.getBuildTools().setSyncTrigger("Any changes");

        original.getActionsOnSave().setReformatCode(true);
        original.getActionsOnSave().setActivatedOn("Explicit save only");
        original.getActionsOnSave().setSelectedFileTypes(List.of("SQL", "Markdown"));
        original.getActionsOnSave().setAllFileTypes(false);

        original.getCoverage().setWhenNewCoverageGathered("Replace active suites with the new one");
        original.getCoverage().setActivateCoverageView(false);

        original.getDebugger().setShowDebugWindowOnBreakpoint(false);
        original.getDebugger().setRemoveBreakpointMode("Drag to the editor or click with middle mouse button");

        original.getDebuggerDataViews().setSortValuesAlphabetically(true);
        original.getDebuggerDataViews().setValueTooltipDelayMs(950);

        original.getDebuggerStepping().setSteppingMode("Step into");
        original.getDebuggerStepping().setPauseAtBegin(true);

        original.getDiagrams().setDefaultLayout("Circular");
        original.getDiagrams().setShortenNodeItemsLength(120);

        original.getDiffMerge().setContextLines("8");
        original.getDiffMerge().setAutoApplyNonConflictingChanges(true);

        original.getExternalDiffTools().setEnableExternalTools(true);
        original.getExternalDiffTools().setCustomTools(List.of(new AppSettingsStore.ExternalToolDefinition("Meld", "/usr/bin/meld", "%1 %2", "%1 %2 %3")));
        original.getExternalDiffTools().setAssociations(List.of(new AppSettingsStore.FileTypeToolAssociation("*.java", "Meld", "Meld")));

        original.getExternalTools().setTools(List.of(new AppSettingsStore.ExternalToolItem("Echo", "General", "Echoes", "echo", "$FileName$", "$FileDir$")));

        original.getMcpServer().setEnableMcpServer(true);
        original.getMcpServer().setEnabledExposedTools(Set.of("AnalysisToolset.build_project", "VcsToolset.get_repositories"));

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(original);
        assertNotNull(json);

        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertNotNull(loaded);

        assertFalse(loaded.getBuildTools().isSyncProjectAfterChanges());
        assertEquals("Any changes", loaded.getBuildTools().getSyncTrigger());

        assertTrue(loaded.getActionsOnSave().isReformatCode());
        assertEquals("Explicit save only", loaded.getActionsOnSave().getActivatedOn());
        assertEquals(2, loaded.getActionsOnSave().getSelectedFileTypes().size());
        assertFalse(loaded.getActionsOnSave().isAllFileTypes());

        assertEquals("Replace active suites with the new one", loaded.getCoverage().getWhenNewCoverageGathered());
        assertFalse(loaded.getCoverage().isActivateCoverageView());
        assertTrue(loaded.getCoverage().isShowCoverageInProjectView());

        assertFalse(loaded.getDebugger().isShowDebugWindowOnBreakpoint());
        assertEquals("Drag to the editor or click with middle mouse button", loaded.getDebugger().getRemoveBreakpointMode());

        assertTrue(loaded.getDebuggerDataViews().isSortValuesAlphabetically());
        assertEquals(950, loaded.getDebuggerDataViews().getValueTooltipDelayMs());

        assertEquals("Step into", loaded.getDebuggerStepping().getSteppingMode());
        assertTrue(loaded.getDebuggerStepping().isPauseAtBegin());

        assertEquals("Circular", loaded.getDiagrams().getDefaultLayout());
        assertEquals(120, loaded.getDiagrams().getShortenNodeItemsLength());

        assertEquals("8", loaded.getDiffMerge().getContextLines());
        assertTrue(loaded.getDiffMerge().isAutoApplyNonConflictingChanges());

        assertTrue(loaded.getExternalDiffTools().isEnableExternalTools());
        assertEquals(1, loaded.getExternalDiffTools().getCustomTools().size());
        assertEquals("Meld", loaded.getExternalDiffTools().getCustomTools().get(0).getName());
        assertEquals("*.java", loaded.getExternalDiffTools().getAssociations().get(0).getFileType());

        assertEquals(1, loaded.getExternalTools().getTools().size());
        assertEquals("Echo", loaded.getExternalTools().getTools().get(0).getName());

        assertTrue(loaded.getMcpServer().isEnableMcpServer());
        assertEquals(2, loaded.getMcpServer().getEnabledExposedTools().size());
        assertTrue(loaded.getMcpServer().getEnabledExposedTools().contains("VcsToolset.get_repositories"));
    }

    @Test
    public void testToolsPanelsCreationAndInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // 1. Build Tools Panel
        BuildToolsPanel buildPanel = new BuildToolsPanel(settings, inputs);
        Node buildNode = buildPanel.createPanel();
        assertNotNull(buildNode);
        assertTrue(inputs.containsKey("tools_build_syncProjectAfterChanges"));
        assertTrue(inputs.containsKey("tools_build_syncTriggerGroup"));

        // 2. Actions on Save Panel
        ActionsOnSavePanel actionsPanel = new ActionsOnSavePanel(settings, inputs, null);
        Node actionsNode = actionsPanel.createPanel();
        assertNotNull(actionsNode);
        assertTrue(inputs.containsKey("tools_actionsOnSave_reformatCode"));
        assertTrue(inputs.containsKey("tools_actionsOnSave_selectedFileTypes"));
        assertTrue(inputs.containsKey("tools_actionsOnSave_activatedOn"));

        // 3. Coverage Panel
        CoveragePanel coveragePanel = new CoveragePanel(settings, inputs);
        Node coverageNode = coveragePanel.createPanel();
        assertNotNull(coverageNode);
        assertTrue(inputs.containsKey("tools_coverage_whenGatheredGroup"));
        assertTrue(inputs.containsKey("tools_coverage_activateView"));
        assertTrue(inputs.containsKey("tools_coverage_showInProjectView"));
    }

    @Test
    public void testSettingsDialogRoutingAndApplySettingsForTools() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // Routing
        Node r1 = SettingsDialog.buildPanelForPath("Tools / Build Tools", settings, inputs, null);
        assertNotNull(r1);

        Node r2 = SettingsDialog.buildPanelForPath("Tools / Actions on Save", settings, inputs, null);
        assertNotNull(r2);

        Node r3 = SettingsDialog.buildPanelForPath("Tools / Coverage", settings, inputs, null);
        assertNotNull(r3);

        // Apply settings
        CheckBox syncBuild = new CheckBox();
        syncBuild.setSelected(false);
        inputs.put("tools_build_syncProjectAfterChanges", syncBuild);

        ToggleGroup triggerGroup = new ToggleGroup();
        RadioButton anyChanges = new RadioButton("Any changes");
        anyChanges.setToggleGroup(triggerGroup);
        anyChanges.setSelected(true);
        inputs.put("tools_build_syncTriggerGroup", triggerGroup);

        CheckBox reformatCode = new CheckBox();
        reformatCode.setSelected(true);
        inputs.put("tools_actionsOnSave_reformatCode", reformatCode);

        Set<String> customTypes = new LinkedHashSet<>(List.of("SQL", "HTML"));
        inputs.put("tools_actionsOnSave_selectedFileTypes", customTypes);

        ComboBox<String> actOn = new ComboBox<>();
        actOn.setValue("Explicit save only");
        inputs.put("tools_actionsOnSave_activatedOn", actOn);

        ToggleGroup covGroup = new ToggleGroup();
        RadioButton rbDoNotApply = new RadioButton("Do not apply collected coverage");
        rbDoNotApply.setToggleGroup(covGroup);
        rbDoNotApply.setSelected(true);
        inputs.put("tools_coverage_whenGatheredGroup", covGroup);

        CheckBox actView = new CheckBox();
        actView.setSelected(false);
        inputs.put("tools_coverage_activateView", actView);

        CheckBox showProject = new CheckBox();
        showProject.setSelected(false);
        inputs.put("tools_coverage_showInProjectView", showProject);

        SettingsDialog.applySettings(null, settings, inputs);

        assertFalse(settings.getBuildTools().isSyncProjectAfterChanges());
        assertEquals("Any changes", settings.getBuildTools().getSyncTrigger());

        assertTrue(settings.getActionsOnSave().isReformatCode());
        assertEquals("Explicit save only", settings.getActionsOnSave().getActivatedOn());
        assertEquals(2, settings.getActionsOnSave().getSelectedFileTypes().size());
        assertFalse(settings.getActionsOnSave().isAllFileTypes());

        assertEquals("Do not apply collected coverage", settings.getCoverage().getWhenNewCoverageGathered());
        assertFalse(settings.getCoverage().isActivateCoverageView());
        assertFalse(settings.getCoverage().isShowCoverageInProjectView());
    }

    @Test
    public void testDebuggerPanelsCreationAndInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // Debugger Main Panel
        DebuggerSettingsPanel debugPanel = new DebuggerSettingsPanel(settings, inputs);
        Node debugNode = debugPanel.createPanel();
        assertNotNull(debugNode);
        assertTrue(inputs.containsKey("tools_debugger_showDebugWindow"));
        assertTrue(inputs.containsKey("tools_debugger_focusApp"));
        assertTrue(inputs.containsKey("tools_debugger_hideOnTermination"));
        assertTrue(inputs.containsKey("tools_debugger_scrollToCenter"));
        assertTrue(inputs.containsKey("tools_debugger_clickLineRunToCursor"));
        assertTrue(inputs.containsKey("tools_debugger_removeBreakpointGroup"));
        assertTrue(inputs.containsKey("tools_debugger_confirmRemoval"));

        // Data Views Panel
        DebuggerDataViewsPanel viewsPanel = new DebuggerDataViewsPanel(settings, inputs);
        Node viewsNode = viewsPanel.createPanel();
        assertNotNull(viewsNode);
        assertTrue(inputs.containsKey("tools_debugger_sortValuesAlphabetically"));
        assertTrue(inputs.containsKey("tools_debugger_enableAutoExpressions"));
        assertTrue(inputs.containsKey("tools_debugger_showValuesInline"));
        assertTrue(inputs.containsKey("tools_debugger_showValueTooltip"));
        assertTrue(inputs.containsKey("tools_debugger_valueTooltipDelayMs"));
        assertTrue(inputs.containsKey("tools_debugger_showValueTooltipOnCodeSelection"));

        // Stepping Panel
        DebuggerSteppingPanel stepPanel = new DebuggerSteppingPanel(settings, inputs);
        Node stepNode = stepPanel.createPanel();
        assertNotNull(stepNode);
        assertTrue(inputs.containsKey("tools_debugger_steppingMode"));
        assertTrue(inputs.containsKey("tools_debugger_pauseAtBegin"));
    }

    @Test
    public void testDiagramsAndDiffMergePanelsCreationAndInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // Diagrams Panel
        DiagramsSettingsPanel diagramsPanel = new DiagramsSettingsPanel(settings, inputs);
        Node diagramsNode = diagramsPanel.createPanel();
        assertNotNull(diagramsNode);
        assertTrue(inputs.containsKey("tools_diagrams_enabledCategories"));
        assertTrue(inputs.containsKey("tools_diagrams_defaultScope"));
        assertTrue(inputs.containsKey("tools_diagrams_nodeItemStyle"));
        assertTrue(inputs.containsKey("tools_diagrams_shortenLength"));
        assertTrue(inputs.containsKey("tools_diagrams_showGrid"));
        assertTrue(inputs.containsKey("tools_diagrams_enableAnimations"));
        assertTrue(inputs.containsKey("tools_diagrams_syntaxHighlighting"));
        assertTrue(inputs.containsKey("tools_diagrams_defaultLayout"));
        assertTrue(inputs.containsKey("tools_diagrams_layoutOnCategorySwitch"));
        assertTrue(inputs.containsKey("tools_diagrams_animationDuration"));
        assertTrue(inputs.containsKey("tools_diagrams_fitContent"));
        assertTrue(inputs.containsKey("tools_diagrams_relayoutOnNewElements"));

        // Diff & Merge Panel
        DiffMergeSettingsPanel diffMergePanel = new DiffMergeSettingsPanel(settings, inputs);
        Node diffMergeNode = diffMergePanel.createPanel();
        assertNotNull(diffMergeNode);
        assertTrue(inputs.containsKey("tools_diffmerge_contextSlider"));
        assertTrue(inputs.containsKey("tools_diffmerge_goToNextFile"));
        assertTrue(inputs.containsKey("tools_diffmerge_navHistory"));
        assertTrue(inputs.containsKey("tools_diffmerge_autoApply"));
        assertTrue(inputs.containsKey("tools_diffmerge_autoResolveImports"));
        assertTrue(inputs.containsKey("tools_diffmerge_highlightModified"));
    }

    @Test
    public void testSettingsDialogRoutingAndApplySettingsForNewTools() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // Routing checks
        assertNotNull(SettingsDialog.buildPanelForPath("Tools / Debugger", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Tools / Debugger / Data Views", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Tools / Debugger / Stepping", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Tools / Diagrams", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Tools / Diff & Merge", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Tools / Diff & Merge / External Diff Tools", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Tools / External Tools", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Tools / MCP Server", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Tools / MCP Server / Exposed Tools", settings, inputs, null));

        // Modify inputs for Debugger
        CheckBox showDebug = new CheckBox();
        showDebug.setSelected(false);
        inputs.put("tools_debugger_showDebugWindow", showDebug);

        CheckBox focusApp = new CheckBox();
        focusApp.setSelected(false);
        inputs.put("tools_debugger_focusApp", focusApp);

        CheckBox hideOnTerm = new CheckBox();
        hideOnTerm.setSelected(true);
        inputs.put("tools_debugger_hideOnTermination", hideOnTerm);

        CheckBox scrollCenter = new CheckBox();
        scrollCenter.setSelected(true);
        inputs.put("tools_debugger_scrollToCenter", scrollCenter);

        CheckBox clickLine = new CheckBox();
        clickLine.setSelected(false);
        inputs.put("tools_debugger_clickLineRunToCursor", clickLine);

        ToggleGroup rmGroup = new ToggleGroup();
        RadioButton rbDrag = new RadioButton("Drag to the editor or click with middle mouse button");
        rbDrag.setToggleGroup(rmGroup);
        rbDrag.setSelected(true);
        inputs.put("tools_debugger_removeBreakpointGroup", rmGroup);

        CheckBox confirmRm = new CheckBox();
        confirmRm.setSelected(true);
        inputs.put("tools_debugger_confirmRemoval", confirmRm);

        // Data Views
        CheckBox sortVals = new CheckBox();
        sortVals.setSelected(true);
        inputs.put("tools_debugger_sortValuesAlphabetically", sortVals);

        CheckBox autoExpr = new CheckBox();
        autoExpr.setSelected(false);
        inputs.put("tools_debugger_enableAutoExpressions", autoExpr);

        CheckBox inlineVals = new CheckBox();
        inlineVals.setSelected(false);
        inputs.put("tools_debugger_showValuesInline", inlineVals);

        CheckBox showTip = new CheckBox();
        showTip.setSelected(false);
        inputs.put("tools_debugger_showValueTooltip", showTip);

        Spinner<Integer> tipDelay = new Spinner<>(0, 10000, 1250);
        inputs.put("tools_debugger_valueTooltipDelayMs", tipDelay);

        CheckBox showTipOnSel = new CheckBox();
        showTipOnSel.setSelected(true);
        inputs.put("tools_debugger_showValueTooltipOnCodeSelection", showTipOnSel);

        // Stepping
        ComboBox<String> stepMode = new ComboBox<>();
        stepMode.setValue("Force step");
        inputs.put("tools_debugger_steppingMode", stepMode);

        CheckBox pauseBegin = new CheckBox();
        pauseBegin.setSelected(true);
        inputs.put("tools_debugger_pauseAtBegin", pauseBegin);

        // Diagrams
        inputs.put("tools_diagrams_enabledCategories", Set.of("Columns", "Details"));

        ComboBox<String> scopeCb = new ComboBox<>();
        scopeCb.setValue("Project");
        inputs.put("tools_diagrams_defaultScope", scopeCb);

        ComboBox<String> itemStyleCb = new ComboBox<>();
        itemStyleCb.setValue("UML");
        inputs.put("tools_diagrams_nodeItemStyle", itemStyleCb);

        Spinner<Integer> shortenSp = new Spinner<>(10, 500, 75);
        inputs.put("tools_diagrams_shortenLength", shortenSp);

        CheckBox showGrid = new CheckBox();
        showGrid.setSelected(false);
        inputs.put("tools_diagrams_showGrid", showGrid);

        CheckBox anim = new CheckBox();
        anim.setSelected(false);
        inputs.put("tools_diagrams_enableAnimations", anim);

        CheckBox synHi = new CheckBox();
        synHi.setSelected(false);
        inputs.put("tools_diagrams_syntaxHighlighting", synHi);

        ComboBox<String> layoutCb = new ComboBox<>();
        layoutCb.setValue("Organic");
        inputs.put("tools_diagrams_defaultLayout", layoutCb);

        ComboBox<String> switchLayCb = new ComboBox<>();
        switchLayCb.setValue("Keep positions");
        inputs.put("tools_diagrams_layoutOnCategorySwitch", switchLayCb);

        Spinner<Integer> animDur = new Spinner<>(0, 10000, 2500);
        inputs.put("tools_diagrams_animationDuration", animDur);

        CheckBox fitContent = new CheckBox();
        fitContent.setSelected(false);
        inputs.put("tools_diagrams_fitContent", fitContent);

        CheckBox relayoutNew = new CheckBox();
        relayoutNew.setSelected(false);
        inputs.put("tools_diagrams_relayoutOnNewElements", relayoutNew);

        // Diff & Merge
        Slider slider = new Slider(0, 4, 3); // index 3 -> "8"
        inputs.put("tools_diffmerge_contextSlider", slider);

        CheckBox nextFile = new CheckBox();
        nextFile.setSelected(false);
        inputs.put("tools_diffmerge_goToNextFile", nextFile);

        ComboBox<String> navHist = new ComboBox<>();
        navHist.setValue("Always");
        inputs.put("tools_diffmerge_navHistory", navHist);

        CheckBox autoApply = new CheckBox();
        autoApply.setSelected(true);
        inputs.put("tools_diffmerge_autoApply", autoApply);

        CheckBox autoImports = new CheckBox();
        autoImports.setSelected(true);
        inputs.put("tools_diffmerge_autoResolveImports", autoImports);

        CheckBox highlightLines = new CheckBox();
        highlightLines.setSelected(false);
        inputs.put("tools_diffmerge_highlightModified", highlightLines);

        // External Diff Tools
        CheckBox extDiffEnable = new CheckBox();
        extDiffEnable.setSelected(true);
        inputs.put("tools_external_diff_enable", extDiffEnable);

        List<AppSettingsStore.ExternalToolDefinition> customDiffTools = List.of(
                new AppSettingsStore.ExternalToolDefinition("P4Merge", "/opt/p4merge", "%1 %2", "%1 %2 %3 %4")
        );
        inputs.put("tools_external_diff_customTools", customDiffTools);

        List<AppSettingsStore.FileTypeToolAssociation> diffAssocs = List.of(
                new AppSettingsStore.FileTypeToolAssociation("*.xml", "P4Merge", "P4Merge")
        );
        inputs.put("tools_external_diff_associations", diffAssocs);

        // External Tools
        List<AppSettingsStore.ExternalToolItem> extToolsList = List.of(
                new AppSettingsStore.ExternalToolItem("Prettier", "Format", "Run prettier", "npx prettier", "--write $FilePath$", "$FileDir$")
        );
        inputs.put("tools_external_tools_list", extToolsList);

        // MCP Server
        CheckBox mcpEnable = new CheckBox();
        mcpEnable.setSelected(true);
        inputs.put("tools_mcp_enable", mcpEnable);

        inputs.put("tools_mcp_exposed_tools", Set.of("FileToolset.create_new_file", "SearchToolset.search_file"));

        // Apply settings
        SettingsDialog.applySettings(null, settings, inputs);

        // Verify Debugger persistence
        assertFalse(settings.getDebugger().isShowDebugWindowOnBreakpoint());
        assertFalse(settings.getDebugger().isFocusApplicationOnBreakpoint());
        assertTrue(settings.getDebugger().isHideDebugWindowOnProcessTermination());
        assertTrue(settings.getDebugger().isScrollExecutionPointToCenter());
        assertFalse(settings.getDebugger().isClickLineNumberToRunToCursor());
        assertEquals("Drag to the editor or click with middle mouse button", settings.getDebugger().getRemoveBreakpointMode());
        assertTrue(settings.getDebugger().isConfirmRemovalOfConditionalOrLoggingBreakpoints());

        // Verify Data Views persistence
        assertTrue(settings.getDebuggerDataViews().isSortValuesAlphabetically());
        assertFalse(settings.getDebuggerDataViews().isEnableAutoExpressions());
        assertFalse(settings.getDebuggerDataViews().isShowValuesInline());
        assertFalse(settings.getDebuggerDataViews().isShowValueTooltip());
        assertEquals(1250, settings.getDebuggerDataViews().getValueTooltipDelayMs());
        assertTrue(settings.getDebuggerDataViews().isShowValueTooltipOnCodeSelection());

        // Verify Stepping persistence
        assertEquals("Force step", settings.getDebuggerStepping().getSteppingMode());
        assertTrue(settings.getDebuggerStepping().isPauseAtBegin());

        // Verify Diagrams persistence
        assertEquals(Set.of("Columns", "Details"), settings.getDiagrams().getEnabledCategories());
        assertEquals("Project", settings.getDiagrams().getDefaultScope());
        assertEquals("UML", settings.getDiagrams().getNodeItemStyle());
        assertEquals(75, settings.getDiagrams().getShortenNodeItemsLength());
        assertFalse(settings.getDiagrams().isShowGridByDefault());
        assertFalse(settings.getDiagrams().isEnableAnimations());
        assertFalse(settings.getDiagrams().isEnableNodeItemsSyntaxHighlighting());
        assertEquals("Organic", settings.getDiagrams().getDefaultLayout());
        assertEquals("Keep positions", settings.getDiagrams().getLayoutOnCategorySwitch());
        assertEquals(2500, settings.getDiagrams().getLayoutAnimationDuration());
        assertFalse(settings.getDiagrams().isFitContentAfterLayout());
        assertFalse(settings.getDiagrams().isRelayoutWhenNewElementsAdded());

        // Verify Diff & Merge persistence
        assertEquals("8", settings.getDiffMerge().getContextLines());
        assertFalse(settings.getDiffMerge().isGoToNextFileAfterLastChange());
        assertEquals("Always", settings.getDiffMerge().getIncludeDiffsInNavigationHistory());
        assertTrue(settings.getDiffMerge().isAutoApplyNonConflictingChanges());
        assertTrue(settings.getDiffMerge().isAutoResolveConflictsInImports());
        assertFalse(settings.getDiffMerge().isHighlightModifiedLinesInGutter());

        // Verify External Diff Tools persistence
        assertTrue(settings.getExternalDiffTools().isEnableExternalTools());
        assertEquals(1, settings.getExternalDiffTools().getCustomTools().size());
        assertEquals("P4Merge", settings.getExternalDiffTools().getCustomTools().get(0).getName());
        assertEquals(1, settings.getExternalDiffTools().getAssociations().size());
        assertEquals("*.xml", settings.getExternalDiffTools().getAssociations().get(0).getFileType());

        // Verify External Tools persistence
        assertEquals(1, settings.getExternalTools().getTools().size());
        assertEquals("Prettier", settings.getExternalTools().getTools().get(0).getName());

        // Verify MCP Server persistence
        assertTrue(settings.getMcpServer().isEnableMcpServer());
        assertEquals(2, settings.getMcpServer().getEnabledExposedTools().size());
        assertTrue(settings.getMcpServer().getEnabledExposedTools().contains("FileToolset.create_new_file"));
    }

    @Test
    public void testExternalDiffAndToolsPanelsCreation() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        ExternalDiffToolsPanel diffToolsPanel = new ExternalDiffToolsPanel(settings, inputs);
        Node diffToolsNode = diffToolsPanel.createPanel();
        assertNotNull(diffToolsNode);
        assertTrue(inputs.containsKey("tools_external_diff_enable"));
        assertTrue(inputs.containsKey("tools_external_diff_customTools"));
        assertTrue(inputs.containsKey("tools_external_diff_associations"));

        ExternalToolsPanel toolsPanel = new ExternalToolsPanel(settings, inputs);
        Node toolsNode = toolsPanel.createPanel();
        assertNotNull(toolsNode);
        assertTrue(inputs.containsKey("tools_external_tools_list"));
    }

    @Test
    public void testMcpPanelsCreation() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        McpServerPanel mcpServerPanel = new McpServerPanel(settings, inputs, null);
        Node mcpServerNode = mcpServerPanel.createPanel();
        assertNotNull(mcpServerNode);
        assertTrue(inputs.containsKey("tools_mcp_enable"));

        McpExposedToolsPanel exposedPanel = new McpExposedToolsPanel(settings, inputs);
        Node exposedNode = exposedPanel.createPanel();
        assertNotNull(exposedNode);
        assertTrue(inputs.containsKey("tools_mcp_exposed_tools"));
    }

    @Test
    public void testRsyncDefaultsAndCopy() {
        AppSettingsStore.RsyncConfig cfg = new AppSettingsStore.RsyncConfig();
        assertEquals("rsync", cfg.getRsyncExecutablePath());
        assertEquals("-zar", cfg.getRsyncOptions());
        assertEquals("ssh", cfg.getShellExecutablePath());

        AppSettingsStore.RsyncConfig copy = cfg.copy();
        copy.setRsyncExecutablePath("/usr/local/bin/rsync");
        copy.setRsyncOptions("-avz --progress");
        copy.setShellExecutablePath("/usr/bin/ssh");

        assertEquals("rsync", cfg.getRsyncExecutablePath());
        assertEquals("-zar", cfg.getRsyncOptions());
        assertEquals("ssh", cfg.getShellExecutablePath());

        assertEquals("/usr/local/bin/rsync", copy.getRsyncExecutablePath());
        assertEquals("-avz --progress", copy.getRsyncOptions());
        assertEquals("/usr/bin/ssh", copy.getShellExecutablePath());
    }

    @Test
    public void testSshConfigurationsDefaultsAndCopy() {
        AppSettingsStore.SshConfigurationsConfig cfg = new AppSettingsStore.SshConfigurationsConfig();
        assertNotNull(cfg.getConfigurations());
        assertTrue(cfg.getConfigurations().isEmpty());

        AppSettingsStore.SshConfigurationEntry entry = new AppSettingsStore.SshConfigurationEntry(
                "prod-server", "192.168.1.50", 2222, "deployer", "Key pair"
        );
        assertEquals("prod-server", entry.getName());
        assertEquals("192.168.1.50", entry.getHost());
        assertEquals(2222, entry.getPort());
        assertEquals("deployer", entry.getUsername());
        assertEquals("Key pair", entry.getAuthType());
        assertTrue(entry.isSendKeepAlive());
        assertEquals(300, entry.getKeepAliveIntervalSec());
        assertEquals("Ask", entry.getStrictHostKeyMode());
        assertEquals("No proxy", entry.getProxyType());

        cfg.getConfigurations().add(entry);
        assertEquals(1, cfg.getConfigurations().size());

        AppSettingsStore.SshConfigurationsConfig copy = cfg.copy();
        assertEquals(1, copy.getConfigurations().size());

        copy.getConfigurations().get(0).setHost("10.0.0.1");
        copy.getConfigurations().get(0).setPort(22);
        copy.getConfigurations().get(0).setAuthType("Password");

        assertEquals("192.168.1.50", cfg.getConfigurations().get(0).getHost());
        assertEquals(2222, cfg.getConfigurations().get(0).getPort());
        assertEquals("Key pair", cfg.getConfigurations().get(0).getAuthType());

        assertEquals("10.0.0.1", copy.getConfigurations().get(0).getHost());
        assertEquals(22, copy.getConfigurations().get(0).getPort());
        assertEquals("Password", copy.getConfigurations().get(0).getAuthType());
    }

    @Test
    public void testTerminalDefaultsAndCopy() {
        AppSettingsStore.TerminalConfig cfg = new AppSettingsStore.TerminalConfig();
        assertEquals("Reworked 2025", cfg.getTerminalEngine());
        assertTrue(cfg.isShowCompletionPopup());
        assertEquals("Only for parameters", cfg.getCompletionTrigger());
        assertTrue(cfg.isShowCompletionShortcutEnabled());
        assertEquals("Ctrl+Space", cfg.getCompletionShortcut());
        assertEquals("Enter", cfg.getInsertSuggestionWith());
        assertEquals("JetBrains Mono", cfg.getFont());
        assertEquals(13.0, cfg.getFontSize());
        assertEquals(1.0, cfg.getLineHeight());
        assertEquals(1.0, cfg.getColumnWidth());
        assertEquals("Local", cfg.getDefaultTabName());
        assertTrue(cfg.isUseApplicationTitleAsTabName());
        assertEquals("When command is running", cfg.getApplicationTitleMode());
        assertTrue(cfg.isEnforceMinimumContrastRatio());
        assertEquals(4.5, cfg.getMinimumContrastRatio());
        assertTrue(cfg.isShowSeparatorsBetweenExecutedCommands());
        assertTrue(cfg.isAudibleBell());
        assertTrue(cfg.isCloseSessionWhenItEnds());
        assertTrue(cfg.isMouseReporting());
        assertFalse(cfg.isMoveFocusToEditor());
        assertEquals("Escape", cfg.getMoveFocusKey());
        assertTrue(cfg.isPasteOnMiddleMouseButtonClick());
        assertTrue(cfg.isOverrideIdeShortcuts());
        assertTrue(cfg.isShellIntegration());
        assertTrue(cfg.isHighlightHyperlinks());
        assertEquals("Block", cfg.getCursorShape());

        AppSettingsStore.TerminalConfig copy = cfg.copy();
        copy.setTerminalEngine("Classic");
        copy.setCompletionTrigger("Always");
        copy.setFontSize(14.5);
        copy.setCursorShape("Underline");
        copy.setMoveFocusToEditor(true);

        assertEquals("Reworked 2025", cfg.getTerminalEngine());
        assertEquals("Only for parameters", cfg.getCompletionTrigger());
        assertEquals(13.0, cfg.getFontSize());
        assertEquals("Block", cfg.getCursorShape());
        assertFalse(cfg.isMoveFocusToEditor());

        assertEquals("Classic", copy.getTerminalEngine());
        assertEquals("Always", copy.getCompletionTrigger());
        assertEquals(14.5, copy.getFontSize());
        assertEquals("Underline", copy.getCursorShape());
        assertTrue(copy.isMoveFocusToEditor());
    }

    @Test
    public void testRsyncSshTerminalJsonSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        settings.getRsync().setRsyncOptions("-avz");
        settings.getSshConfigurations().getConfigurations().add(
                new AppSettingsStore.SshConfigurationEntry("remote", "10.0.0.2", 2222, "admin", "Password")
        );
        settings.getTerminal().setTerminalEngine("Classic");
        settings.getTerminal().setFontSize(15.0);

        String json = mapper.writeValueAsString(settings);
        assertNotNull(json);

        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertEquals("-avz", loaded.getRsync().getRsyncOptions());
        assertEquals(1, loaded.getSshConfigurations().getConfigurations().size());
        assertEquals("remote", loaded.getSshConfigurations().getConfigurations().get(0).getName());
        assertEquals("10.0.0.2", loaded.getSshConfigurations().getConfigurations().get(0).getHost());
        assertEquals(2222, loaded.getSshConfigurations().getConfigurations().get(0).getPort());
        assertEquals("Classic", loaded.getTerminal().getTerminalEngine());
        assertEquals(15.0, loaded.getTerminal().getFontSize());
    }

    @Test
    public void testRsyncSshTerminalPanelsCreation() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        RsyncPanel rsyncPanel = new RsyncPanel(settings, inputs);
        Node rsyncNode = rsyncPanel.createPanel();
        assertNotNull(rsyncNode);
        assertTrue(inputs.containsKey("tools_rsync_rsyncExecutablePath"));
        assertTrue(inputs.containsKey("tools_rsync_rsyncOptions"));
        assertTrue(inputs.containsKey("tools_rsync_shellExecutablePath"));

        SshConfigurationsPanel sshPanel = new SshConfigurationsPanel(settings, inputs);
        Node sshNode = sshPanel.createPanel();
        assertNotNull(sshNode);
        assertTrue(inputs.containsKey("tools_ssh_configurations_list"));

        TerminalPanel terminalPanel = new TerminalPanel(settings, inputs, null);
        Node terminalNode = terminalPanel.createPanel();
        assertNotNull(terminalNode);
        assertTrue(inputs.containsKey("tools_terminal_engine"));
        assertTrue(inputs.containsKey("tools_terminal_showCompletionPopup"));
        assertTrue(inputs.containsKey("tools_terminal_font"));
        assertTrue(inputs.containsKey("tools_terminal_fontSize"));
        assertTrue(inputs.containsKey("tools_terminal_shellPath"));
    }

    @Test
    public void testSettingsDialogRoutingForRsyncSshTerminal() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        Node rsyncNode = SettingsDialog.buildPanelForPath("Tools / Rsync", settings, inputs, null);
        assertNotNull(rsyncNode);

        Node sshNode = SettingsDialog.buildPanelForPath("Tools / SSH Configurations", settings, inputs, null);
        assertNotNull(sshNode);

        Node termNode = SettingsDialog.buildPanelForPath("Tools / Terminal", settings, inputs, null);
        assertNotNull(termNode);
    }

    @Test
    public void testApplySettingsForRsyncSshTerminal() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // Rsync inputs
        TextField rsyncPath = new TextField("/opt/rsync/bin/rsync");
        TextField rsyncOpts = new TextField("-azvP --delete");
        TextField shellPath = new TextField("/usr/bin/ssh");
        inputs.put("tools_rsync_rsyncExecutablePath", rsyncPath);
        inputs.put("tools_rsync_rsyncOptions", rsyncOpts);
        inputs.put("tools_rsync_shellExecutablePath", shellPath);

        // SSH Configurations input
        List<AppSettingsStore.SshConfigurationEntry> sshList = List.of(
                new AppSettingsStore.SshConfigurationEntry("staging", "staging.internal", 2202, "dev", "Password")
        );
        inputs.put("tools_ssh_configurations_list", sshList);

        // Terminal inputs
        ComboBox<String> engineCombo = new ComboBox<>();
        engineCombo.setValue("Classic");
        inputs.put("tools_terminal_engine", engineCombo);

        CheckBox completionCheck = new CheckBox();
        completionCheck.setSelected(false);
        inputs.put("tools_terminal_showCompletionPopup", completionCheck);

        ToggleGroup trigGroup = new ToggleGroup();
        RadioButton rbAlways = new RadioButton("Always");
        rbAlways.setToggleGroup(trigGroup);
        rbAlways.setSelected(true);
        inputs.put("tools_terminal_completionTriggerGroup", trigGroup);

        TextField startDir = new TextField("/home/user/workspace");
        inputs.put("tools_terminal_startDirectory", startDir);

        Spinner<Double> fontSize = new Spinner<>(8.0, 48.0, 16.5);
        inputs.put("tools_terminal_fontSize", fontSize);

        TextField termShell = new TextField("/bin/zsh");
        inputs.put("tools_terminal_shellPath", termShell);

        ComboBox<String> cursorCombo = new ComboBox<>();
        cursorCombo.setValue("Vertical bar");
        inputs.put("tools_terminal_cursorShape", cursorCombo);

        // Apply settings
        SettingsDialog.applySettings(null, settings, inputs);

        // Verify Rsync
        assertEquals("/opt/rsync/bin/rsync", settings.getRsync().getRsyncExecutablePath());
        assertEquals("-azvP --delete", settings.getRsync().getRsyncOptions());
        assertEquals("/usr/bin/ssh", settings.getRsync().getShellExecutablePath());

        // Verify SSH Configurations
        assertEquals(1, settings.getSshConfigurations().getConfigurations().size());
        assertEquals("staging", settings.getSshConfigurations().getConfigurations().get(0).getName());
        assertEquals("staging.internal", settings.getSshConfigurations().getConfigurations().get(0).getHost());
        assertEquals(2202, settings.getSshConfigurations().getConfigurations().get(0).getPort());

        // Verify Terminal
        assertEquals("Classic", settings.getTerminal().getTerminalEngine());
        assertFalse(settings.getTerminal().isShowCompletionPopup());
        assertEquals("Always", settings.getTerminal().getCompletionTrigger());
        assertEquals("/home/user/workspace", settings.getTerminal().getStartDirectory());
        assertEquals(16.5, settings.getTerminal().getFontSize());
        assertEquals("/bin/zsh", settings.getTerminal().getShellPath());
        assertEquals("Vertical bar", settings.getTerminal().getCursorShape());
    }

    @Test
    public void testWebBrowsersDefaultsAndCopy() {
        AppSettingsStore.WebBrowsersConfig cfg = new AppSettingsStore.WebBrowsersConfig();
        assertNotNull(cfg.getBrowsers());
        assertEquals(6, cfg.getBrowsers().size());
        assertEquals("System default", cfg.getDefaultBrowser());
        assertEquals("", cfg.getCustomBrowserPath());
        assertTrue(cfg.isShowPopupForHtml());
        assertFalse(cfg.isShowPopupForXml());
        assertEquals("On Save", cfg.getReloadPageInBrowser());
        assertEquals("On Save", cfg.getReloadPageInBuiltInPreview());
        assertEquals(63342, cfg.getBuiltInServerPort());
        assertFalse(cfg.isAcceptExternalConnections());
        assertFalse(cfg.isAllowUnsignedRequests());

        AppSettingsStore.WebBrowsersConfig copy = cfg.copy();
        copy.setDefaultBrowser("Firefox");
        copy.setCustomBrowserPath("/usr/bin/firefox");
        copy.setShowPopupForHtml(false);
        copy.setShowPopupForXml(true);
        copy.setReloadPageInBrowser("Disabled");
        copy.setBuiltInServerPort(8080);
        copy.setAcceptExternalConnections(true);
        copy.getBrowsers().get(0).setName("Custom Chrome");

        assertEquals("System default", cfg.getDefaultBrowser());
        assertEquals("", cfg.getCustomBrowserPath());
        assertTrue(cfg.isShowPopupForHtml());
        assertFalse(cfg.isShowPopupForXml());
        assertEquals(63342, cfg.getBuiltInServerPort());
        assertEquals("Chrome", cfg.getBrowsers().get(0).getName());

        assertEquals("Firefox", copy.getDefaultBrowser());
        assertEquals("/usr/bin/firefox", copy.getCustomBrowserPath());
        assertFalse(copy.isShowPopupForHtml());
        assertTrue(copy.isShowPopupForXml());
        assertEquals("Disabled", copy.getReloadPageInBrowser());
        assertEquals(8080, copy.getBuiltInServerPort());
        assertTrue(copy.isAcceptExternalConnections());
        assertEquals("Custom Chrome", copy.getBrowsers().get(0).getName());
    }

    @Test
    public void testXPathViewerDefaultsAndCopy() {
        AppSettingsStore.XPathViewerConfig cfg = new AppSettingsStore.XPathViewerConfig();
        assertTrue(cfg.isScrollFirstHitIntoVisibleArea());
        assertTrue(cfg.isUseNodeAtCursorAsContextNode());
        assertTrue(cfg.isHighlightOnlyStartTag());
        assertTrue(cfg.isAddErrorStripeMarkers());
        assertEquals("#FFD578", cfg.getHighlightColor());
        assertEquals("#C2FFD4", cfg.getContextNodeColor());

        AppSettingsStore.XPathViewerConfig copy = cfg.copy();
        copy.setScrollFirstHitIntoVisibleArea(false);
        copy.setUseNodeAtCursorAsContextNode(false);
        copy.setHighlightOnlyStartTag(false);
        copy.setAddErrorStripeMarkers(false);
        copy.setHighlightColor("#FF0000");
        copy.setContextNodeColor("#00FF00");

        assertTrue(cfg.isScrollFirstHitIntoVisibleArea());
        assertTrue(cfg.isUseNodeAtCursorAsContextNode());
        assertTrue(cfg.isHighlightOnlyStartTag());
        assertTrue(cfg.isAddErrorStripeMarkers());
        assertEquals("#FFD578", cfg.getHighlightColor());
        assertEquals("#C2FFD4", cfg.getContextNodeColor());

        assertFalse(copy.isScrollFirstHitIntoVisibleArea());
        assertFalse(copy.isUseNodeAtCursorAsContextNode());
        assertFalse(copy.isHighlightOnlyStartTag());
        assertFalse(copy.isAddErrorStripeMarkers());
        assertEquals("#FF0000", copy.getHighlightColor());
        assertEquals("#00FF00", copy.getContextNodeColor());
    }

    @Test
    public void testBackupSyncDefaultsAndCopy() {
        AppSettingsStore.BackupSyncConfig cfg = new AppSettingsStore.BackupSyncConfig();
        assertFalse(cfg.isEnableBackupAndSync());
        assertEquals("15103202@iubat.edu", cfg.getSyncAccount());
        assertNotNull(cfg.getAvailableAccounts());
        assertTrue(cfg.getAvailableAccounts().contains("15103202@iubat.edu"));

        AppSettingsStore.BackupSyncConfig copy = cfg.copy();
        copy.setEnableBackupAndSync(true);
        copy.setSyncAccount("user@company.org");

        assertFalse(cfg.isEnableBackupAndSync());
        assertEquals("15103202@iubat.edu", cfg.getSyncAccount());

        assertTrue(copy.isEnableBackupAndSync());
        assertEquals("user@company.org", copy.getSyncAccount());
    }

    @Test
    public void testWebBrowsersXPathBackupSyncJsonSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        settings.getWebBrowsers().setDefaultBrowser("Chrome");
        settings.getWebBrowsers().setBuiltInServerPort(7070);
        settings.getXpathViewer().setHighlightColor("#AABBCC");
        settings.getBackupSync().setEnableBackupAndSync(true);

        String json = mapper.writeValueAsString(settings);
        assertNotNull(json);

        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertEquals("Chrome", loaded.getWebBrowsers().getDefaultBrowser());
        assertEquals(7070, loaded.getWebBrowsers().getBuiltInServerPort());
        assertEquals("#AABBCC", loaded.getXpathViewer().getHighlightColor());
        assertTrue(loaded.getBackupSync().isEnableBackupAndSync());
    }

    @Test
    public void testWebBrowsersXPathBackupSyncPanelsCreation() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        WebBrowsersPanel browsersPanel = new WebBrowsersPanel(settings, inputs);
        Node browsersNode = browsersPanel.createPanel();
        assertNotNull(browsersNode);
        assertTrue(inputs.containsKey("tools_browsers_list"));
        assertTrue(inputs.containsKey("tools_browsers_defaultBrowser"));
        assertTrue(inputs.containsKey("tools_browsers_popupHtml"));
        assertTrue(inputs.containsKey("tools_browsers_reloadBrowser"));
        assertTrue(inputs.containsKey("tools_browsers_serverPort"));

        XPathViewerPanel xpathPanel = new XPathViewerPanel(settings, inputs);
        Node xpathNode = xpathPanel.createPanel();
        assertNotNull(xpathNode);
        assertTrue(inputs.containsKey("tools_xpath_scrollFirstHit"));
        assertTrue(inputs.containsKey("tools_xpath_highlightColor"));
        assertTrue(inputs.containsKey("tools_xpath_contextNodeColor"));

        BackupSyncPanel backupPanel = new BackupSyncPanel(settings, inputs);
        Node backupNode = backupPanel.createPanel();
        assertNotNull(backupNode);
        assertTrue(inputs.containsKey("backup_sync_enable"));
        assertTrue(inputs.containsKey("backup_sync_account"));
    }

    @Test
    public void testSettingsDialogRoutingForBrowsersXPathBackupSync() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        Node browsersNode = SettingsDialog.buildPanelForPath("Tools / Web Browsers and Preview", settings, inputs, null);
        assertNotNull(browsersNode);

        Node xpathNode = SettingsDialog.buildPanelForPath("Tools / XPath Viewer", settings, inputs, null);
        assertNotNull(xpathNode);

        Node backupNode = SettingsDialog.buildPanelForPath("Backup and Sync", settings, inputs, null);
        assertNotNull(backupNode);
    }

    @Test
    public void testApplySettingsForBrowsersXPathBackupSync() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // Web Browsers
        List<AppSettingsStore.WebBrowserItem> list = List.of(
                new AppSettingsStore.WebBrowserItem(true, "Brave", "Chrome", "/usr/bin/brave")
        );
        inputs.put("tools_browsers_list", list);

        ComboBox<String> defBrowser = new ComboBox<>();
        defBrowser.setValue("Brave");
        inputs.put("tools_browsers_defaultBrowser", defBrowser);

        TextField customPath = new TextField("/custom/browser");
        inputs.put("tools_browsers_customPath", customPath);

        CheckBox htmlPopup = new CheckBox();
        htmlPopup.setSelected(false);
        inputs.put("tools_browsers_popupHtml", htmlPopup);

        CheckBox xmlPopup = new CheckBox();
        xmlPopup.setSelected(true);
        inputs.put("tools_browsers_popupXml", xmlPopup);

        ComboBox<String> reloadBrowser = new ComboBox<>();
        reloadBrowser.setValue("Disabled");
        inputs.put("tools_browsers_reloadBrowser", reloadBrowser);

        Spinner<Integer> serverPort = new Spinner<>(1024, 65535, 9090);
        inputs.put("tools_browsers_serverPort", serverPort);

        CheckBox acceptExternal = new CheckBox();
        acceptExternal.setSelected(true);
        inputs.put("tools_browsers_acceptExternal", acceptExternal);

        // XPath Viewer
        CheckBox scrollFirst = new CheckBox();
        scrollFirst.setSelected(false);
        inputs.put("tools_xpath_scrollFirstHit", scrollFirst);

        inputs.put("tools_xpath_highlightColor", "#112233");
        inputs.put("tools_xpath_contextNodeColor", "#445566");

        // Backup and Sync
        CheckBox enableSync = new CheckBox();
        enableSync.setSelected(true);
        inputs.put("backup_sync_enable", enableSync);

        ComboBox<String> accountCombo = new ComboBox<>();
        accountCombo.setValue("admin@custom.org");
        inputs.put("backup_sync_account", accountCombo);

        // Apply settings
        SettingsDialog.applySettings(null, settings, inputs);

        // Assert Web Browsers
        assertEquals(1, settings.getWebBrowsers().getBrowsers().size());
        assertEquals("Brave", settings.getWebBrowsers().getBrowsers().get(0).getName());
        assertEquals("Brave", settings.getWebBrowsers().getDefaultBrowser());
        assertEquals("/custom/browser", settings.getWebBrowsers().getCustomBrowserPath());
        assertFalse(settings.getWebBrowsers().isShowPopupForHtml());
        assertTrue(settings.getWebBrowsers().isShowPopupForXml());
        assertEquals("Disabled", settings.getWebBrowsers().getReloadPageInBrowser());
        assertEquals(9090, settings.getWebBrowsers().getBuiltInServerPort());
        assertTrue(settings.getWebBrowsers().isAcceptExternalConnections());

        // Assert XPath Viewer
        assertFalse(settings.getXpathViewer().isScrollFirstHitIntoVisibleArea());
        assertEquals("#112233", settings.getXpathViewer().getHighlightColor());
        assertEquals("#445566", settings.getXpathViewer().getContextNodeColor());

        // Assert Backup and Sync
        assertTrue(settings.getBackupSync().isEnableBackupAndSync());
        assertEquals("admin@custom.org", settings.getBackupSync().getSyncAccount());
    }
}

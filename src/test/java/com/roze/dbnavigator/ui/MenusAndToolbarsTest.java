package com.roze.dbnavigator.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.MenuItemConfig;
import com.roze.dbnavigator.ui.action.ActionGroup;
import com.roze.dbnavigator.ui.action.ActionManager;
import com.roze.dbnavigator.ui.action.ActionRegistry;
import com.roze.dbnavigator.ui.action.AnAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MenusAndToolbarsTest {

    private AppSettingsStore.Settings settings;

    @BeforeEach
    public void setUp() {
        settings = new AppSettingsStore.Settings();
    }

    @Test
    @DisplayName("Verify all 23 root categories exist in defaultMenusAndToolbars")
    public void testDefaultMenusAndToolbarsRoots() {
        List<MenuItemConfig> roots = settings.getMenusAndToolbars();
        assertNotNull(roots);
        assertEquals(23, roots.size(), "Should have exactly 23 standard menu and toolbar root categories");

        List<String> expectedNames = List.of(
                "Main Menu",
                "Main Toolbar",
                "Editor Popup Menu",
                "Editor Gutter Popup Menu",
                "Editor Tab Popup Menu",
                "Project View Popup Menu",
                "Scope View Popup Menu",
                "Navigation Bar Popup Menu",
                "Navigation Bar Toolbar",
                "Debug Header More Popup",
                "Debug Header Toolbar",
                "Debug Watches Toolbar",
                "File History Toolbar",
                "Floating Code Toolbar",
                "Markdown Editor Floating Toolbar",
                "Quick Actions Popup Toolbar",
                "Run Tool Window Header More Popup",
                "Run Tool Window Header Toolbar",
                "SQL Floating Toolbar",
                "VCS Local Changes Toolbar",
                "VCS Log Changes Browser Toolbar",
                "VCS Log Toolbar",
                "VCS Operations Popup"
        );

        for (String name : expectedNames) {
            boolean found = roots.stream().anyMatch(r -> name.equalsIgnoreCase(r.getText()));
            assertTrue(found, "Expected root category to be present: " + name);
        }
    }

    @Test
    @DisplayName("Verify Main Menu hierarchy contains standard menus and actions")
    public void testMainMenuHierarchy() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        assertNotNull(mainMenu);
        assertEquals("Main Menu", mainMenu.getText());

        List<String> topMenus = mainMenu.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();

        assertTrue(topMenus.contains("File"));
        assertTrue(topMenus.contains("Edit"));
        assertTrue(topMenus.contains("View"));
        assertTrue(topMenus.contains("Navigate"));
        assertFalse(topMenus.contains("Code"));
        assertFalse(topMenus.contains("Refactor"));
        assertTrue(topMenus.contains("Run"));
        assertFalse(topMenus.contains("Tools"));
        assertTrue(topMenus.contains("Git") || topMenus.contains("VCS"));
        assertTrue(topMenus.contains("Window"));
        assertTrue(topMenus.contains("Help"));

        // Verify File > New group
        MenuItemConfig fileMenu = mainMenu.getChildren().stream()
                .filter(m -> "File".equals(m.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(fileMenu);

        MenuItemConfig newGroup = fileMenu.getChildren().stream()
                .filter(m -> "New".equals(m.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(newGroup);

        List<String> newGroupItems = newGroup.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();
        assertTrue(newGroupItems.contains("Project\u2026"));
        assertTrue(newGroupItems.contains("SQL File"));
        assertTrue(newGroupItems.contains("Scratch File"));
        assertTrue(newGroupItems.contains("Query Console"));
        assertTrue(newGroupItems.contains("Table"));
        assertTrue(newGroupItems.contains("View"));
        assertTrue(newGroupItems.contains("Data Source"));
        assertTrue(newGroupItems.contains("Data Source from Cloud Provider"));
        assertTrue(newGroupItems.contains("Driver"));
    }

    @Test
    @DisplayName("Verify Main Toolbar structure has Left, Center, and Right groups matching DataGrip")
    public void testMainToolbarStructure() {
        MenuItemConfig toolbar = settings.getMenuConfig("root.main.toolbar");
        assertNotNull(toolbar);
        assertEquals(3, toolbar.getChildren().size());

        List<String> subGroups = toolbar.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();
        assertTrue(subGroups.contains("Left"));
        assertTrue(subGroups.contains("Center"));
        assertTrue(subGroups.contains("Right"));

        // Left
        MenuItemConfig leftGroup = toolbar.getChildren().stream()
                .filter(g -> "Left".equals(g.getText()))
                .findFirst().orElse(null);
        assertNotNull(leftGroup);
        List<String> leftActions = leftGroup.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();
        assertTrue(leftActions.contains("Project Widget"));
        assertTrue(leftActions.contains("VCS Group"));
        assertTrue(leftActions.contains("General Actions"));

        // Center
        MenuItemConfig centerGroup = toolbar.getChildren().stream()
                .filter(g -> "Center".equals(g.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(centerGroup);

        List<String> centerActions = centerGroup.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();
        assertFalse(centerActions.contains("Single Tool Window Bar"));
        assertFalse(centerActions.stream().anyMatch(t -> t != null && t.contains("File Name Widget")));

        // Right
        MenuItemConfig rightGroup = toolbar.getChildren().stream()
                .filter(g -> "Right".equals(g.getText()))
                .findFirst().orElse(null);
        assertNotNull(rightGroup);
        List<String> rightActions = rightGroup.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();
        assertTrue(rightActions.contains("ExecutionTargetsToolbarGroup"));
        assertTrue(rightActions.contains("AIAssistantHubPopupAction"));
        assertTrue(rightActions.contains("Search Everywhere"));
        assertTrue(rightActions.contains("IDE and Project Settings"));
    }

    @Test
    @DisplayName("Verify Editor Popup Menu structure contains DataGrip actions, submenus, and folding groups")
    public void testEditorPopupMenuStructure() {
        MenuItemConfig editorPopup = settings.getMenuConfig("root.editor.popup");
        assertNotNull(editorPopup);

        List<String> items = editorPopup.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();
        assertTrue(items.contains("ShowIntentionsGroup"));
        assertTrue(items.contains("LightEditModePopup"));
        assertTrue(items.contains("Cut"));
        assertTrue(items.contains("Copy"));
        assertTrue(items.contains("Copy as Rich Text"));
        assertTrue(items.contains("Paste"));
        assertTrue(items.contains("Copy / Paste Special"));
        assertTrue(items.contains("Copy JSON Pointer"));
        assertTrue(items.contains("Column Selection Mode"));
        assertTrue(items.contains("Markdown.EditorContextMenuGroup"));
        assertTrue(items.contains("Editor Popup Menu Actions (1)"));
        assertTrue(items.contains("EditorPopupMenu2"));
        assertTrue(items.contains("EditorPopupMenu3"));
        assertTrue(items.contains("Search with Google"));
        assertTrue(items.contains("Editor Popup Menu Actions (2)"));
        assertTrue(items.contains("Git"));
        assertTrue(items.contains("Diagrams"));
        assertTrue(items.contains("Change Template Data Language"));
        assertTrue(items.contains("XPathView.EditorPopup"));

        // Check Markdown group Table sub-items
        MenuItemConfig mdGroup = editorPopup.getChildren().stream()
                .filter(i -> "Markdown.EditorContextMenuGroup".equals(i.getText()))
                .findFirst().orElse(null);
        assertNotNull(mdGroup);
        MenuItemConfig tableGroup = mdGroup.getChildren().stream()
                .filter(i -> "Table".equals(i.getText()))
                .findFirst().orElse(null);
        assertNotNull(tableGroup);
        List<String> tableActions = tableGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(tableActions.contains("Insert Column Left"));
        assertTrue(tableActions.contains("Column Alignment"));

        // Check Editor Popup Menu Actions (1) -> EditorPopupMenu1.FindRefactor -> Go To and Folding
        MenuItemConfig actions1 = editorPopup.getChildren().stream()
                .filter(i -> "Editor Popup Menu Actions (1)".equals(i.getText()))
                .findFirst().orElse(null);
        assertNotNull(actions1);
        assertTrue(actions1.getChildren().stream().anyMatch(i -> "Find in Files".equals(i.getText())));

        MenuItemConfig findRefactor = actions1.getChildren().stream()
                .filter(i -> "EditorPopupMenu1.FindRefactor".equals(i.getText()))
                .findFirst().orElse(null);
        assertNotNull(findRefactor);
        assertTrue(findRefactor.getChildren().stream().anyMatch(i -> "Find Usages".equals(i.getText())));

        MenuItemConfig goToGroup = findRefactor.getChildren().stream()
                .filter(i -> "Go To".equals(i.getText()))
                .findFirst().orElse(null);
        assertNotNull(goToGroup);
        assertTrue(goToGroup.getChildren().stream().anyMatch(i -> "Console.TableResult.Navigate.Group".equals(i.getText())));
        assertTrue(goToGroup.getChildren().stream().anyMatch(i -> "Console.TableResult.Database.GoTo".equals(i.getText())));

        MenuItemConfig foldingGroup = findRefactor.getChildren().stream()
                .filter(i -> "Folding".equals(i.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(foldingGroup);
        List<String> foldActions = foldingGroup.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();
        assertTrue(foldActions.contains("Expand"));
        assertTrue(foldActions.contains("Collapse"));
        assertTrue(foldActions.contains("Expand All"));
        assertTrue(foldActions.contains("Collapse All"));
        assertTrue(foldActions.contains("Expand to Level"));
        assertTrue(foldActions.contains("Expand All to Level"));
        assertTrue(foldActions.contains("LanguageSpecificFoldingGroup"));
        assertTrue(foldActions.contains("Toggle Folding"));
        assertTrue(foldActions.contains("Fold Selection / Remove Region"));
        assertTrue(foldActions.contains("Fold Code Block"));

        // Check EditorPopupMenu2 -> SQL Scripts (Image 1)
        MenuItemConfig menu2 = editorPopup.getChildren().stream()
                .filter(i -> "EditorPopupMenu2".equals(i.getText()))
                .findFirst().orElse(null);
        assertNotNull(menu2);
        MenuItemConfig sqlScripts = menu2.getChildren().stream()
                .filter(i -> "SQL Scripts".equals(i.getText()))
                .findFirst().orElse(null);
        assertNotNull(sqlScripts);
        List<String> sqlActions = sqlScripts.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(sqlActions.stream().anyMatch(t -> t.contains("SQL Generator")));
        assertTrue(sqlActions.contains("Request and Copy Original DDL"));
        assertTrue(sqlActions.contains("Generate DDL to Clipboard"));
        assertTrue(sqlActions.contains("Generate DDL to Query File"));
        assertTrue(sqlActions.contains("Context Templates"));
        assertTrue(sqlActions.stream().anyMatch(t -> t.contains("Run SQL Script")));
        assertTrue(sqlActions.contains("Regenerate definition"));
        assertTrue(sqlActions.stream().anyMatch(t -> t.contains("Import to Database")));
        assertTrue(sqlActions.contains("View as Table"));
        assertTrue(sqlActions.contains("Edit as Table"));
        assertTrue(sqlActions.contains("Change File Language"));
        assertTrue(sqlActions.contains("Change SQL Dialect"));

        // Check EditorPopupMenu3 -> Set Background Image (Image 1)
        MenuItemConfig menu3 = editorPopup.getChildren().stream()
                .filter(i -> "EditorPopupMenu3".equals(i.getText()))
                .findFirst().orElse(null);
        assertNotNull(menu3);
        assertTrue(menu3.getChildren().stream().anyMatch(i -> "Set Background Image".equals(i.getText())));

        // Check Editor Popup Menu Actions (2) (Images 2, 3, 4, 5)
        MenuItemConfig actions2 = editorPopup.getChildren().stream()
                .filter(i -> "Editor Popup Menu Actions (2)".equals(i.getText()))
                .findFirst().orElse(null);
        assertNotNull(actions2);
        List<String> actions2Names = actions2.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(actions2Names.contains("Refactor"));
        assertTrue(actions2Names.contains("Debug Actions"));
        assertTrue(actions2Names.contains("Compile/Run Actions"));
        assertTrue(actions2Names.contains("SplitRevealGroup"));
        assertTrue(actions2Names.contains("VCS/LVCS Actions"));

        // Refactor -> Extract/Introduce (Image 2)
        MenuItemConfig refactorMenu = actions2.getChildren().stream()
                .filter(i -> "Refactor".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(refactorMenu);
        MenuItemConfig extractIntro = refactorMenu.getChildren().stream()
                .filter(i -> "Extract/Introduce".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(extractIntro);
        List<String> extractActions = extractIntro.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(extractActions.stream().anyMatch(t -> t.contains("Introduce Variable")));
        assertTrue(extractActions.stream().anyMatch(t -> t.contains("Extract Routine")));
        assertTrue(extractActions.contains("Subquery as CTE"));

        // Debug Actions (Image 3)
        MenuItemConfig debugActions = actions2.getChildren().stream()
                .filter(i -> "Debug Actions".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(debugActions);
        List<String> dbgNames = debugActions.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(dbgNames.stream().anyMatch(t -> t.contains("Evaluate Expression")));
        assertTrue(dbgNames.contains("Run to Cursor"));
        assertTrue(dbgNames.contains("EditorPopupMenuDebugHotSwap"));

        // Compile/Run Actions -> Run Configurations -> RunContextGroup -> Console.Jdbc.RunContextGroup -> Explain Plan (Image 4)
        MenuItemConfig compileRun = actions2.getChildren().stream()
                .filter(i -> "Compile/Run Actions".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(compileRun);
        MenuItemConfig runConfigs = compileRun.getChildren().stream()
                .filter(i -> "Run Configurations".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(runConfigs);
        MenuItemConfig runContext = runConfigs.getChildren().stream()
                .filter(i -> "RunContextGroup".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(runContext);
        MenuItemConfig jdbcRunContext = runContext.getChildren().stream()
                .filter(i -> "Console.Jdbc.RunContextGroup".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(jdbcRunContext);
        assertTrue(jdbcRunContext.getChildren().stream().anyMatch(i -> "Attach Data Source".equals(i.getText())));
        MenuItemConfig explainPlan = jdbcRunContext.getChildren().stream()
                .filter(i -> "Explain Plan".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(explainPlan);
        List<String> planNames = explainPlan.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(planNames.contains("Explain Plan"));
        assertTrue(planNames.contains("Explain Plan (Raw)"));
        assertTrue(planNames.contains("Explain Analyse"));
        assertTrue(planNames.contains("Explain Analyse (Raw)"));

        // SplitRevealGroup (Image 4)
        MenuItemConfig splitReveal = actions2.getChildren().stream()
                .filter(i -> "SplitRevealGroup".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(splitReveal);
        assertTrue(splitReveal.getChildren().stream().anyMatch(i -> "Open in Right Split".equals(i.getText())));
        MenuItemConfig openIn = splitReveal.getChildren().stream()
                .filter(i -> "Open In".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(openIn);
        List<String> openInNames = openIn.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(openInNames.contains("Show in File Manager"));
        assertTrue(openInNames.contains("Open in Browser"));
        assertTrue(openInNames.contains("Open in Terminal"));

        // VCS/LVCS Actions -> Local History (Image 5)
        MenuItemConfig vcsLvcs = actions2.getChildren().stream()
                .filter(i -> "VCS/LVCS Actions".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(vcsLvcs);
        MenuItemConfig localHistory = vcsLvcs.getChildren().stream()
                .filter(i -> "Local History".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(localHistory);
        List<String> lhNames = localHistory.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(lhNames.stream().anyMatch(t -> t.contains("Show History")));
        assertTrue(lhNames.contains("Recent Changes"));

        // Git (Image 5)
        MenuItemConfig git = editorPopup.getChildren().stream()
                .filter(i -> "Git".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(git);
        List<String> gitNames = git.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(gitNames.contains("Show Local Version"));
        assertTrue(gitNames.contains("Compare with HEAD Version"));
        assertTrue(gitNames.contains("Compare with Local Version"));
        assertTrue(gitNames.contains("Compare HEAD, Staged and Local Versions"));

        // Diagrams (Image 5)
        MenuItemConfig diagrams = editorPopup.getChildren().stream()
                .filter(i -> "Diagrams".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(diagrams);
        List<String> diagNames = diagrams.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(diagNames.contains("ShowUmlDiagram"));
        assertTrue(diagNames.contains("ShowUmlDiagramPopup"));

        // XPathView.EditorPopup (Image 5)
        MenuItemConfig xpath = editorPopup.getChildren().stream()
                .filter(i -> "XPathView.EditorPopup".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(xpath);
        List<String> xpathNames = xpath.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(xpathNames.stream().anyMatch(t -> t.contains("Evaluate XPath")));
        assertTrue(xpathNames.contains("Show Unique XPath"));
        assertTrue(xpathNames.contains("File Associations"));
    }

    @Test
    @DisplayName("Test MenuItemConfig copy and manipulation")
    public void testMenuItemConfigCopyAndManipulation() {
        MenuItemConfig action = MenuItemConfig.action("custom.action", "My Action");
        action.setIconName("DATABASE");
        action.setIconPath("/path/to/icon.png");

        MenuItemConfig copy = action.copy();
        assertEquals(action.getId(), copy.getId());
        assertEquals(action.getText(), copy.getText());
        assertEquals(action.getIconName(), copy.getIconName());
        assertEquals(action.getIconPath(), copy.getIconPath());
        assertEquals(action.getType(), copy.getType());

        // Test group with children
        MenuItemConfig group = MenuItemConfig.group("custom.group", "My Group", List.of(action, MenuItemConfig.separator()));
        MenuItemConfig groupCopy = group.copy();
        assertEquals(2, groupCopy.getChildren().size());
        assertEquals(MenuItemConfig.Type.ACTION, groupCopy.getChildren().get(0).getType());
        assertEquals(MenuItemConfig.Type.SEPARATOR, groupCopy.getChildren().get(1).getType());

        // Ensure deep copy
        groupCopy.getChildren().remove(0);
        assertEquals(1, groupCopy.getChildren().size());
        assertEquals(2, group.getChildren().size());
    }

    @Test
    @DisplayName("Test restore single menu and restore all menus")
    public void testRestoreMenus() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        assertNotNull(mainMenu);
        int originalSize = mainMenu.getChildren().size();

        // Mutate
        mainMenu.getChildren().clear();
        assertEquals(0, settings.getMenuConfig("root.main.menu").getChildren().size());

        // Restore single menu
        settings.restoreMenu("root.main.menu");
        assertEquals(originalSize, settings.getMenuConfig("root.main.menu").getChildren().size());

        // Mutate multiple menus
        settings.getMenuConfig("root.main.menu").getChildren().clear();
        settings.getMenuConfig("root.main.toolbar").getChildren().clear();

        // Restore all
        settings.restoreAllMenus();
        assertEquals(originalSize, settings.getMenuConfig("root.main.menu").getChildren().size());
        assertEquals(3, settings.getMenuConfig("root.main.toolbar").getChildren().size());
    }

    @Test
    @DisplayName("Test Jackson JSON serialization and deserialization roundtrip")
    public void testJsonRoundtrip() throws Exception {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        String json = mapper.writeValueAsString(settings);
        assertNotNull(json);
        assertTrue(json.contains("root.main.menu"));
        assertTrue(json.contains("root.main.toolbar"));
        assertTrue(json.contains("root.editor.popup"));

        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertNotNull(loaded);
        assertEquals(23, loaded.getMenusAndToolbars().size());

        MenuItemConfig reloadedMainMenu = loaded.getMenuConfig("root.main.menu");
        assertNotNull(reloadedMainMenu);
        assertFalse(reloadedMainMenu.getChildren().isEmpty());
    }

    @Test
    @DisplayName("Verify Editor Gutter and Tab Popup Menu detailed hierarchies from DataGrip")
    public void testEditorGutterAndTabPopupMenus() {
        MenuItemConfig gutter = settings.getMenuConfig("root.editor.gutter.popup");
        assertNotNull(gutter);
        List<String> gutterItems = gutter.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(gutterItems.contains("EditorGutterVcsPopupMenu"));
        assertTrue(gutterItems.contains("popup@BookmarkContextMenu"));
        assertTrue(gutterItems.contains("Soft-Wrap"));
        assertTrue(gutterItems.contains("Configure Soft Wraps\u2026"));
        assertTrue(gutterItems.contains("Appearance"));
        assertTrue(gutterItems.contains("Remove other breakpoints"));
        assertTrue(gutterItems.contains("Disable other breakpoints"));

        // Gutter VCS
        MenuItemConfig gutterVcs = gutter.getChildren().stream()
                .filter(c -> "EditorGutterVcsPopupMenu".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(gutterVcs);
        assertFalse(gutterVcs.isPopup());
        List<String> vcsActions = gutterVcs.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(vcsActions.contains("Annotate"));

        // Gutter Bookmarks
        MenuItemConfig gutterBm = gutter.getChildren().stream()
                .filter(c -> "popup@BookmarkContextMenu".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(gutterBm);
        assertFalse(gutterBm.isPopup());
        List<String> bmActions = gutterBm.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(bmActions.contains("Add Bookmark to Another List"));
        assertTrue(bmActions.contains("Rename Bookmark\u2026"));
        assertTrue(bmActions.contains("Toggle Bookmark"));
        assertTrue(bmActions.contains("Remove Mnemonic"));
        assertTrue(bmActions.contains("Toggle Bookmark Mnemonic\u2026"));

        // Gutter Appearance & Breadcrumbs
        MenuItemConfig appearance = gutter.getChildren().stream()
                .filter(c -> "Appearance".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(appearance);
        assertTrue(appearance.isPopup());
        List<String> appActions = appearance.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(appActions.contains("Show Line Numbers"));
        assertTrue(appActions.contains("Breakpoints Over Line Numbers"));
        assertTrue(appActions.contains("Show Indent Guides"));
        assertTrue(appActions.contains("Show Sticky Lines"));
        assertTrue(appActions.contains("Breadcrumbs"));
        assertTrue(appActions.contains("Configure Gutter Icons\u2026"));

        MenuItemConfig breadcrumbs = appearance.getChildren().stream()
                .filter(c -> "Breadcrumbs".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(breadcrumbs);
        assertTrue(breadcrumbs.isPopup());
        List<String> bcActions = breadcrumbs.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(bcActions.contains("Top"));
        assertTrue(bcActions.contains("Bottom"));
        assertTrue(bcActions.contains("Don't Show"));

        // Tab Popup Menu
        MenuItemConfig tabPopup = settings.getMenuConfig("root.editor.tab.popup");
        assertNotNull(tabPopup);
        List<String> tabItems = tabPopup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(tabItems.contains("Editor Close Actions"));
        assertTrue(tabItems.contains("Copy Paths"));
        assertTrue(tabItems.contains("Copy Reference"));
        assertTrue(tabItems.contains("Copy JSON Pointer"));
        assertTrue(tabItems.contains("Change Template Data Language"));
        assertTrue(tabItems.contains("Copy Path/Reference\u2026"));
        assertTrue(tabItems.contains("Show Diff in Separate Window"));
        assertTrue(tabItems.contains("Show All Files in One Diff View"));
        assertTrue(tabItems.contains("Vcs.Diff.EditorTabs.Group"));
        assertTrue(tabItems.contains("Split Right"));
        assertTrue(tabItems.contains("Split and Move Right"));
        assertTrue(tabItems.contains("Split Down"));
        assertTrue(tabItems.contains("Split and Move Down"));
        assertTrue(tabItems.contains("Move to Opposite Group"));
        assertTrue(tabItems.contains("Open in Opposite Group"));
        assertTrue(tabItems.contains("Change Splitter Orientation"));
        assertTrue(tabItems.contains("Unsplit"));
        assertTrue(tabItems.contains("Unsplit All"));
        assertTrue(tabItems.contains("Pin Active Tab"));
        assertTrue(tabItems.contains("Keep Tab Open"));
        assertTrue(tabItems.contains("Open Tab in New Window"));
        assertTrue(tabItems.contains("Configure Editor Tabs\u2026"));
        assertTrue(tabItems.contains("Reopen Closed Tab"));
        assertTrue(tabItems.contains("Database.EditorTabPopupMenu"));
        assertTrue(tabItems.contains("Bookmarks"));
        assertTrue(tabItems.contains("Editor Tab Popup Menu Actions (1)"));

        // Editor Close Actions
        MenuItemConfig closeActions = tabPopup.getChildren().stream()
                .filter(c -> "Editor Close Actions".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(closeActions);
        assertFalse(closeActions.isPopup());
        List<String> closeItems = closeActions.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(closeItems.contains("Close Tab"));
        assertTrue(closeItems.contains("Close Other Tabs"));
        assertTrue(closeItems.contains("Close All Tabs"));
        assertTrue(closeItems.contains("Close Unmodified Tabs"));
        assertTrue(closeItems.contains("Close All but Pinned"));
        assertTrue(closeItems.contains("Close Tabs to the Left"));
        assertTrue(closeItems.contains("Close Tabs to the Right"));
        assertTrue(closeItems.contains("Close All Read-Only Tabs"));
        assertTrue(closeItems.contains("Open as Editor Tab"));

        // Copy Path/Reference...
        MenuItemConfig copyPathRef = tabPopup.getChildren().stream()
                .filter(c -> "Copy Path/Reference\u2026".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(copyPathRef);
        assertTrue(copyPathRef.isPopup());
        MenuItemConfig copyFileRef = copyPathRef.getChildren().stream()
                .filter(c -> "CopyFileReference".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(copyFileRef);
        List<String> fileRefActions = copyFileRef.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(fileRefActions.contains("Absolute Path"));
        assertTrue(fileRefActions.contains("File Name"));
        assertTrue(fileRefActions.contains("Path with Line Number"));
        assertTrue(fileRefActions.contains("Path from Content Root"));
        assertTrue(fileRefActions.contains("Path from Source Root"));
        assertTrue(fileRefActions.contains("Path From Repository Root"));
        assertTrue(fileRefActions.contains("Git.Hosting.Copy.Link.Group"));

        MenuItemConfig copyExtRef = copyPathRef.getChildren().stream()
                .filter(c -> "CopyExternalReferenceGroup".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(copyExtRef);
        assertTrue(copyExtRef.getChildren().stream().anyMatch(c -> "Toolbox URL".equals(c.getText())));

        // Database.EditorTabPopupMenu & Bookmarks
        MenuItemConfig dbTabPopup = tabPopup.getChildren().stream()
                .filter(c -> "Database.EditorTabPopupMenu".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(dbTabPopup);
        assertTrue(dbTabPopup.getChildren().stream().anyMatch(c -> "Shorten Tab Titles".equals(c.getText())));

        MenuItemConfig tabBm = tabPopup.getChildren().stream()
                .filter(c -> "Bookmarks".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(tabBm);
        List<String> tabBmActions = tabBm.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(tabBmActions.contains("Add Bookmark to Another List"));
        assertTrue(tabBmActions.contains("Rename Bookmark\u2026"));
        assertTrue(tabBmActions.contains("Toggle Bookmark"));

        // Editor Tab Popup Menu Actions (1)
        MenuItemConfig tabActions1 = tabPopup.getChildren().stream()
                .filter(c -> "Editor Tab Popup Menu Actions (1)".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(tabActions1);
        List<String> a1Items = tabActions1.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(a1Items.contains("Change File Language"));
        assertTrue(a1Items.contains("Associate with File Type\u2026"));
        assertTrue(a1Items.contains("Mark File As"));
        assertTrue(a1Items.contains("Run Configurations"));

        MenuItemConfig markFileAs = tabActions1.getChildren().stream()
                .filter(c -> "Mark File As".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(markFileAs);
        assertTrue(markFileAs.isPopup());
        List<String> markActions = markFileAs.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(markActions.contains("Override File Type"));
        assertTrue(markActions.contains("Revert File Type Override"));

        MenuItemConfig runConfigs = tabActions1.getChildren().stream()
                .filter(c -> "Run Configurations".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(runConfigs);
        MenuItemConfig runContext = runConfigs.getChildren().stream()
                .filter(c -> "RunContextGroup".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(runContext);
        MenuItemConfig runContextInner = runContext.getChildren().stream()
                .filter(c -> "RunContextGroupInner".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(runContextInner);
        MenuItemConfig executors = runContextInner.getChildren().stream()
                .filter(c -> "RunContextExecutorsGroup".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(executors);
        List<String> execActions = executors.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(execActions.contains("Run context configuration"));
        assertTrue(execActions.contains("Debug context configuration"));

        MenuItemConfig moreRunDebug = runContextInner.getChildren().stream()
                .filter(c -> "More Run/Debug".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(moreRunDebug);
        List<String> moreActions = moreRunDebug.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(moreActions.contains("Run with Coverage context configuration"));
        assertTrue(moreActions.contains("Run with Profiler"));
        assertTrue(moreActions.contains("Create Run Configuration"));
        assertTrue(moreActions.contains("Modify Run Configuration\u2026"));

        MenuItemConfig consoleJdbc = runContext.getChildren().stream()
                .filter(c -> "Console.Jdbc.RunContextGroup".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(consoleJdbc);
        List<String> jdbcActions = consoleJdbc.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(jdbcActions.contains("Attach Data Source"));
        assertTrue(jdbcActions.contains("Recompile\u2026"));
        assertTrue(jdbcActions.contains("Explain Plan"));
        assertTrue(jdbcActions.contains("Execute"));
        assertTrue(jdbcActions.contains("Execute Selection as Single Statement"));
        assertTrue(jdbcActions.contains("Export Data\u2026"));
        assertTrue(jdbcActions.contains("Debug"));
        assertTrue(jdbcActions.contains("Debug Routine\u2026"));
        assertTrue(jdbcActions.contains("Migrate Query Consoles to Query Files\u2026"));

        MenuItemConfig explainPlan = consoleJdbc.getChildren().stream()
                .filter(c -> "Explain Plan".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(explainPlan);
        assertTrue(explainPlan.isPopup());
        List<String> expActions = explainPlan.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(expActions.contains("Explain Plan"));
        assertTrue(expActions.contains("Explain Plan (Raw)"));
        assertTrue(expActions.contains("Explain Analyse"));
        assertTrue(expActions.contains("Explain Analyse (Raw)"));

        // SplitRevealGroup
        MenuItemConfig splitReveal = tabActions1.getChildren().stream()
                .filter(c -> "SplitRevealGroup".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(splitReveal);
        assertFalse(splitReveal.isPopup());
        List<String> srActions = splitReveal.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(srActions.contains("Open in Right Split"));
        assertTrue(srActions.contains("Open in Split with Chooser\u2026"));
        assertTrue(srActions.contains("Open In"));

        MenuItemConfig openIn = splitReveal.getChildren().stream()
                .filter(c -> "Open In".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(openIn);
        assertTrue(openIn.isPopup());
        List<String> openInActions = openIn.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(openInActions.contains("Show in File Manager"));
        assertTrue(openInActions.contains("Open in Associated Application"));
        assertTrue(openInActions.contains("Open in Browser"));
        assertTrue(openInActions.contains("File Path"));
        assertTrue(openInActions.contains("Open in Terminal"));
        assertTrue(openInActions.contains("Git.Hosting.Open.In.Browser.Group"));

        // VCS/LVCS Actions
        MenuItemConfig vcsLvcs = tabActions1.getChildren().stream()
                .filter(c -> "VCS/LVCS Actions".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(vcsLvcs);
        assertFalse(vcsLvcs.isPopup());
        List<String> vlActions = vcsLvcs.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(vlActions.contains("Local History"));
        assertTrue(vlActions.contains("External Tools"));
        assertTrue(vlActions.contains("Rename File\u2026"));

        MenuItemConfig localHistory = vcsLvcs.getChildren().stream()
                .filter(c -> "Local History".equals(c.getText())).findFirst().orElse(null);
        assertNotNull(localHistory);
        assertTrue(localHistory.isPopup());
        List<String> lhActions = localHistory.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(lhActions.contains("Show History\u2026"));
        assertTrue(lhActions.contains("Show History for Selection\u2026"));
        assertTrue(lhActions.contains("Show Project History\u2026"));
        assertTrue(lhActions.contains("Recent Changes"));
        assertTrue(lhActions.contains("Put Label\u2026"));
        assertTrue(lhActions.contains("Version Control Group"));

        // Catalog categories verification
        ActionManager manager = ActionManager.getInstance();
        List<ActionManager.ActionCatalogCategory> catalog = manager.getActionCatalog();
        List<String> catNames = catalog.stream().map(ActionManager.ActionCatalogCategory::getName).toList();
        assertTrue(catNames.contains("Editor Gutter Popup Menu"));
        assertTrue(catNames.contains("Editor Tab Popup Menu"));
        assertTrue(catNames.contains("Project View Popup Menu"));
    }

    @Test
    @DisplayName("Verify Project View Popup Menu detailed hierarchy from DataGrip")
    public void testProjectViewPopupMenu() {
        MenuItemConfig projectPopup = settings.getMenuConfig("root.project.view.popup");
        assertNotNull(projectPopup);
        List<String> items = projectPopup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(items.contains("Attach Directory to Project\u2026"));
        assertTrue(items.contains("New"));
        assertTrue(items.contains("Associate with File Type\u2026"));
        assertTrue(items.contains("Restore Default Extensions"));
        assertTrue(items.contains("Cut/Copy/Paste Actions"));
        assertTrue(items.contains("FileEditor.ImportToDatabase.Group"));
        assertTrue(items.contains("Edit Source"));
        assertTrue(items.contains("ChangesView.ApplyPatch.LangGroup"));
        assertTrue(items.contains("Find Usages"));
        assertTrue(items.contains("Find in Files\u2026"));
        assertTrue(items.contains("InspectCodeActionInPopupMenus"));
        assertTrue(items.contains("Rename\u2026"));
        assertTrue(items.contains("Project View Popup Refactoring Group"));
        assertTrue(items.contains("<anonymous-group-0>"));
        assertTrue(items.contains("Project View Popup Menu Modify Group"));
        assertTrue(items.contains("Project View Popup Menu Run Group"));
        assertTrue(items.contains("SplitRevealGroup"));
        assertTrue(items.contains("VCS/LVCS Actions"));
        assertTrue(items.contains("Cache Recovery"));
        assertTrue(items.contains("Reload from Disk"));
        assertTrue(items.contains("Go to Link Target"));
        assertTrue(items.contains("Compare Files"));
        assertTrue(items.contains("Compare File with Editor"));
        assertTrue(items.contains("External Tools"));
        assertTrue(items.contains("Project View Popup Menu Settings Group"));
        assertTrue(items.contains("Set Background Image"));
        assertTrue(items.contains("Diagrams"));

        // Verify Attach Directory icon
        MenuItemConfig attachDir = projectPopup.getChildren().stream()
                .filter(c -> "file.attach.directory".equals(c.getId()))
                .findFirst().orElseThrow();
        assertEquals("FOLDER_PLUS", attachDir.getIconName());

        // Verify New Submenu (Image 3)
        MenuItemConfig newGroup = projectPopup.getChildren().stream()
                .filter(c -> "project.new".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(newGroup.isPopup());
        List<String> newGroupSub = newGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(newGroupSub.contains("New File"));
        assertTrue(newGroupSub.contains("New"));

        MenuItemConfig newFileGroup = newGroup.getChildren().stream()
                .filter(c -> "file.new.file.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(newFileGroup.isPopup());
        List<String> newFileChildren = newFileGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(newFileChildren.contains("SQL File"));
        assertTrue(newFileChildren.contains("File"));
        assertTrue(newFileChildren.contains("Scratch File"));
        assertTrue(newFileChildren.contains("Directory/Package"));
        assertTrue(newFileChildren.contains("FileTemplateSeparatorGroup"));
        assertTrue(newFileChildren.contains("Web Development Templates"));

        MenuItemConfig webDevTemplates = newFileGroup.getChildren().stream()
                .filter(c -> "file.web.dev.templates".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(webDevTemplates.isPopup());
        List<String> webDevChildren = webDevTemplates.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(webDevChildren.contains("XML"));
        assertTrue(webDevChildren.contains("Microservices Templates"));
        assertTrue(webDevChildren.contains("From Template"));
        assertTrue(webDevChildren.contains("XML Configuration File"));

        MenuItemConfig xmlSub = webDevTemplates.getChildren().stream()
                .filter(c -> "file.web.dev.xml".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(xmlSub.getChildren().stream().map(MenuItemConfig::getText).toList().contains("HTML File"));

        MenuItemConfig newDbGroup = newGroup.getChildren().stream()
                .filter(c -> "file.new.db.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(newDbGroup.isPopup());
        MenuItemConfig addGroup = newDbGroup.getChildren().stream()
                .filter(c -> "file.new.add.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> addChildren = addGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(addChildren.contains("Query File"));
        assertTrue(addChildren.contains("Query File\u2026"));
        assertTrue(addChildren.contains("Scratch Query File"));
        assertTrue(addChildren.contains("Add Ddl Object"));
        assertTrue(addChildren.contains("Create Data Source"));

        MenuItemConfig createDs = addGroup.getChildren().stream()
                .filter(c -> "file.new.create.datasource".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(createDs.isPopup());
        List<String> dsChildren = createDs.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(dsChildren.contains("Data Source from Cloud Provider"));
        assertTrue(dsChildren.contains("Data Source Templates"));
        assertTrue(dsChildren.contains("Data Source from File/Folder"));
        assertTrue(dsChildren.contains("Data Source from URL"));
        assertTrue(dsChildren.contains("Add Data Source from Selection\u2026"));
        assertTrue(dsChildren.contains("Data Source in Path"));
        assertTrue(dsChildren.contains("Import from Clipboard"));
        assertTrue(dsChildren.contains("Create a New Folder"));
        assertTrue(dsChildren.contains("Driver"));

        // Verify Cut/Copy/Paste Actions (Image 4)
        MenuItemConfig ccpGroup = projectPopup.getChildren().stream()
                .filter(c -> "project.cut.copy.paste".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(ccpGroup.isPopup());
        List<String> ccpItems = ccpGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(ccpItems.contains("Cut"));
        assertTrue(ccpItems.contains("Copy"));
        assertTrue(ccpItems.contains("Copy Paths"));
        assertTrue(ccpItems.contains("Copy as Plain Text"));
        assertTrue(ccpItems.contains("Copy as Rich Text"));
        assertTrue(ccpItems.contains("Copy Path/Reference\u2026"));
        assertTrue(ccpItems.contains("Paste"));
        assertTrue(ccpItems.contains("Copy JSON Pointer"));

        MenuItemConfig copyPathRef = ccpGroup.getChildren().stream()
                .filter(c -> "project.copy.path.ref.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(copyPathRef.isPopup());
        MenuItemConfig copyFileRef = copyPathRef.getChildren().stream()
                .filter(c -> "project.copy.file.ref".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> fileRefItems = copyFileRef.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(fileRefItems.contains("Absolute Path"));
        assertTrue(fileRefItems.contains("File Name"));
        assertTrue(fileRefItems.contains("Path with Line Number"));
        assertTrue(fileRefItems.contains("Path from Content Root"));
        assertTrue(fileRefItems.contains("Path from Source Root"));
        assertTrue(fileRefItems.contains("Path From Repository Root"));
        assertTrue(fileRefItems.contains("Git.Hosting.Copy.Link.Group"));

        MenuItemConfig copyExtRef = copyPathRef.getChildren().stream()
                .filter(c -> "project.copy.ext.ref".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(copyExtRef.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Toolbox URL"));
        assertTrue(copyPathRef.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Copy Reference"));

        MenuItemConfig pasteGroup = ccpGroup.getChildren().stream()
                .filter(c -> "project.paste.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(pasteGroup.isPopup());
        List<String> pasteItems = pasteGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(pasteItems.contains("Paste"));
        assertTrue(pasteItems.contains("Paste from History\u2026"));
        assertTrue(pasteItems.contains("Paste as Plain Text"));

        // Verify FileEditor.ImportToDatabase.Group (Image 4)
        MenuItemConfig importGroup = projectPopup.getChildren().stream()
                .filter(c -> "fileeditor.import.to.database.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(importGroup.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Import to Database\u2026"));

        // Verify ChangesView.ApplyPatch.LangGroup (Image 4)
        MenuItemConfig applyPatchGroup = projectPopup.getChildren().stream()
                .filter(c -> "changesview.applypatch.langgroup".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(applyPatchGroup.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Apply Patch\u2026"));

        // Verify InspectCodeActionInPopupMenus (Image 5)
        MenuItemConfig inspectGroup = projectPopup.getChildren().stream()
                .filter(c -> "project.inspect.code.action".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(inspectGroup.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Inspect Code\u2026"));

        // Verify Project View Popup Refactoring Group (Image 5)
        MenuItemConfig refactorGroup = projectPopup.getChildren().stream()
                .filter(c -> "project.refactoring.group".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig refactorSubmenu = refactorGroup.getChildren().stream()
                .filter(c -> "project.refactor.submenu".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(refactorSubmenu.isPopup());
        List<String> refactorItems = refactorSubmenu.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(refactorItems.contains("Refactor This\u2026"));
        assertTrue(refactorItems.contains("Rename\u2026"));
        assertTrue(refactorItems.contains("Change Signature\u2026"));
        assertTrue(refactorItems.contains("Modify Object\u2026"));
        assertTrue(refactorItems.contains("Extract/Introduce"));
        assertTrue(refactorItems.contains("Inline\u2026"));
        assertTrue(refactorItems.contains("Move\u2026"));
        assertTrue(refactorItems.contains("Copy\u2026"));
        assertTrue(refactorItems.contains("Safe Delete\u2026"));
        assertTrue(refactorItems.contains("Pull Members Up\u2026"));
        assertTrue(refactorItems.contains("Push Members Down\u2026"));
        assertTrue(refactorItems.contains("Invert Boolean\u2026"));

        MenuItemConfig extractIntro = refactorSubmenu.getChildren().stream()
                .filter(c -> "project.refactor.extract.introduce".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(extractIntro.isPopup());
        List<String> extractIntroItems = extractIntro.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(extractIntroItems.contains("Introduce Variable\u2026"));
        assertTrue(extractIntroItems.contains("Extract Routine\u2026"));
        assertTrue(extractIntroItems.contains("Table alias\u2026"));
        assertTrue(extractIntroItems.contains("Introduce Constant\u2026"));
        assertTrue(extractIntroItems.contains("Introduce Field\u2026"));
        assertTrue(extractIntroItems.contains("Introduce Parameter\u2026"));
        assertTrue(extractIntroItems.contains("Introduce Parameter Object\u2026"));
        assertTrue(extractIntroItems.contains("Extract Method\u2026"));
        assertTrue(extractIntroItems.contains("Extract Delegate\u2026"));
        assertTrue(extractIntroItems.contains("Include File\u2026"));
        assertTrue(extractIntroItems.contains("Extract Interface\u2026"));
        assertTrue(extractIntroItems.contains("Extract Superclass\u2026"));
        assertTrue(extractIntroItems.contains("Extract Module\u2026"));
        assertTrue(extractIntroItems.contains("Subquery as CTE"));

        // Verify <anonymous-group-0> (Image 1)
        MenuItemConfig anonGroup0 = projectPopup.getChildren().stream()
                .filter(c -> "project.anonymous.group.0".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig bookmarksGroup = anonGroup0.getChildren().stream()
                .filter(c -> "project.bookmarks".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(bookmarksGroup.isPopup());
        List<String> bmItems = bookmarksGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(bmItems.contains("Add Bookmark to Another List"));
        assertTrue(bmItems.contains("Rename Bookmark\u2026"));
        assertTrue(bmItems.contains("Toggle Bookmark"));

        // Verify Project View Popup Menu Modify Group (Image 1)
        MenuItemConfig modifyGroup = projectPopup.getChildren().stream()
                .filter(c -> "project.modify.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> modItems = modifyGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(modItems.contains("Reformat Code"));
        assertTrue(modItems.contains("Reformat JSON"));
        assertTrue(modItems.contains("Optimize Imports"));
        assertTrue(modItems.contains("Delete"));
        assertTrue(modItems.contains("Change File Language"));
        assertTrue(modItems.contains("Change SQL Dialect"));
        assertTrue(modItems.contains("Mark File As"));

        MenuItemConfig markFileAs = modifyGroup.getChildren().stream()
                .filter(c -> "project.mark.file.as".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(markFileAs.isPopup());
        List<String> markItems = markFileAs.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(markItems.contains("Override File Type"));
        assertTrue(markItems.contains("Revert File Type Override"));
        assertTrue(markItems.contains("Detach Directory from Project\u2026"));
        assertTrue(markItems.contains("Exclude from Project"));
        assertTrue(markItems.contains("Include to Project"));

        // Verify Project View Popup Menu Run Group (Images 1 & 2)
        MenuItemConfig runGroup = projectPopup.getChildren().stream()
                .filter(c -> "project.run.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> runGroupItems = runGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(runGroupItems.contains("Run Configurations"));
        assertTrue(runGroupItems.contains("Console.Jdbc.RunContextGroup"));

        MenuItemConfig consoleJdbc = runGroup.getChildren().stream()
                .filter(c -> "console.jdbc.run.context.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> jdbcItems = consoleJdbc.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(jdbcItems.contains("Attach Data Source"));
        assertTrue(jdbcItems.contains("Recompile\u2026"));
        assertTrue(jdbcItems.contains("Explain Plan"));
        assertTrue(jdbcItems.contains("Execute"));
        assertTrue(jdbcItems.contains("Execute Selection as Single Statement"));
        assertTrue(jdbcItems.contains("Export Data\u2026"));
        assertTrue(jdbcItems.contains("Debug"));
        assertTrue(jdbcItems.contains("Debug Routine\u2026"));
        assertTrue(jdbcItems.contains("Migrate Query Consoles to Query Files\u2026"));

        MenuItemConfig explainPlan = consoleJdbc.getChildren().stream()
                .filter(c -> "console.explain.plan.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(explainPlan.isPopup());
        List<String> epItems = explainPlan.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(epItems.contains("Explain Plan"));
        assertTrue(epItems.contains("Explain Plan (Raw)"));
        assertTrue(epItems.contains("Explain Analyse"));
        assertTrue(epItems.contains("Explain Analyse (Raw)"));

        // Verify SplitRevealGroup (Images 2 & 3)
        MenuItemConfig splitReveal = projectPopup.getChildren().stream()
                .filter(c -> "project.split.reveal.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> srItems = splitReveal.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(srItems.contains("Open in Right Split"));
        assertTrue(srItems.contains("Open in Split with Chooser\u2026"));
        assertTrue(srItems.contains("Open In"));

        MenuItemConfig openIn = splitReveal.getChildren().stream()
                .filter(c -> "editor.open.in.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(openIn.isPopup());
        List<String> openInItems = openIn.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(openInItems.contains("Show in File Manager"));
        assertTrue(openInItems.contains("Open in Associated Application"));
        assertTrue(openInItems.contains("Open in Browser"));
        assertTrue(openInItems.contains("File Path"));
        assertTrue(openInItems.contains("Open in Terminal"));
        assertTrue(openInItems.contains("Git.Hosting.Open.In.Browser.Group"));

        // Verify VCS/LVCS Actions (Image 3)
        MenuItemConfig vcsActions = projectPopup.getChildren().stream()
                .filter(c -> "project.vcs.lvcs.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig localHistory = vcsActions.getChildren().stream()
                .filter(c -> "editor.local.history.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(localHistory.isPopup());
        List<String> lhItems = localHistory.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(lhItems.contains("Show History\u2026"));
        assertTrue(lhItems.contains("Show History for Selection\u2026"));
        assertTrue(lhItems.contains("Show Project History\u2026"));
        assertTrue(lhItems.contains("Recent Changes"));
        assertTrue(lhItems.contains("Put Label\u2026"));
        assertTrue(lhItems.contains("Version Control Group"));

        // Verify Project View Popup Menu Settings Group (Image 3)
        MenuItemConfig settingsGroup = projectPopup.getChildren().stream()
                .filter(c -> "project.settings.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(settingsGroup.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Mark Directory As"));

        // Verify Diagrams (Image 3)
        MenuItemConfig diagrams = projectPopup.getChildren().stream()
                .filter(c -> "project.diagrams".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(diagrams.isPopup());
        List<String> diagItems = diagrams.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(diagItems.contains("ShowUmlDiagram"));
        assertTrue(diagItems.contains("ShowUmlDiagramPopup"));
        assertTrue(diagItems.contains("Show Local Changes as UML"));
        assertTrue(diagItems.contains("File Associations"));
        assertTrue(diagItems.contains("Jump to External Editor"));
        assertTrue(diagItems.contains("Convert to PNG"));
    }

    @Test
    @DisplayName("Verify auto-migration of Project View Popup Menu when modify or run group is outdated")
    public void testAutoMigrationOfProjectViewPopupMenu() {
        AppSettingsStore.Settings staleSettings = new AppSettingsStore.Settings();
        MenuItemConfig staleProjectPopup = MenuItemConfig.group("root.project.view.popup", "Project View Popup Menu", List.of(
                MenuItemConfig.action("file.attach.directory", "Attach Directory to Project\u2026"),
                MenuItemConfig.group("project.modify.group", "Project View Popup Menu Modify Group", List.of()),
                MenuItemConfig.group("project.run.group", "Project View Popup Menu Run Group", List.of())
        ));
        staleSettings.setMenusAndToolbars(List.of(staleProjectPopup));

        List<MenuItemConfig> migratedRoots = staleSettings.getMenusAndToolbars();
        MenuItemConfig migrated = migratedRoots.stream()
                .filter(r -> "root.project.view.popup".equalsIgnoreCase(r.getId()))
                .findFirst().orElse(null);
        assertNotNull(migrated);
        MenuItemConfig modifyGroup = migrated.getChildren().stream()
                .filter(c -> "project.modify.group".equalsIgnoreCase(c.getId()))
                .findFirst().orElse(null);
        assertNotNull(modifyGroup);
        assertFalse(modifyGroup.getChildren().isEmpty(), "Modify group should be migrated with children");

        MenuItemConfig runGroup = migrated.getChildren().stream()
                .filter(c -> "project.run.group".equalsIgnoreCase(c.getId()))
                .findFirst().orElse(null);
        assertNotNull(runGroup);
        assertFalse(runGroup.getChildren().isEmpty(), "Run group should be migrated with children");
    }

    @Test
    @DisplayName("Verify Navigation Bar Toolbar and Debug Header structures from DataGrip")
    public void testNavBarAndDebugStructures() {
        MenuItemConfig navBarToolbar = settings.getMenuConfig("root.navigation.bar.toolbar");
        assertNotNull(navBarToolbar);
        List<String> navBarItems = navBarToolbar.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(navBarItems.contains("Toolbar Run Actions"));
        assertTrue(navBarItems.contains("NavBarVcsGroup"));
        assertTrue(navBarItems.contains("NavBarToolBarOthers"));
        assertTrue(navBarItems.contains("AIAssistantHubPopupAction") || navBarItems.contains("@ AIAssistantHubPopupAction"));
        assertTrue(navBarItems.contains("Search Everywhere"));
        assertTrue(navBarItems.contains("IDE and Project Settings"));

        MenuItemConfig debugMore = settings.getMenuConfig("root.debug.header.more.popup");
        assertNotNull(debugMore);
        List<String> moreItems = debugMore.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(moreItems.contains("Force Step Over"));
        assertTrue(moreItems.contains("Force Step Into"));
        assertTrue(moreItems.contains("Smart Step Into"));
        assertTrue(moreItems.contains("Run to Cursor"));
        assertTrue(moreItems.contains("Force Run to Cursor"));
        assertTrue(moreItems.contains("Show Execution Point"));
        assertTrue(moreItems.contains("Evaluate Expression\u2026"));
        assertTrue(moreItems.contains("Reset Frame"));

        MenuItemConfig debugToolbar = settings.getMenuConfig("root.debug.header.toolbar");
        assertNotNull(debugToolbar);
        List<String> dbgItems = debugToolbar.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(dbgItems.contains("Run"));
        assertTrue(dbgItems.contains("Debug"));
        assertTrue(dbgItems.contains("Rerun"));
        assertTrue(dbgItems.contains("Stop"));
        assertTrue(dbgItems.contains("Resume.Ref"));
        assertTrue(dbgItems.contains("Pause.Ref"));
        assertTrue(dbgItems.contains("StepOver.Ref"));

        MenuItemConfig stepOverRef = debugToolbar.getChildren().stream()
                .filter(c -> "debug.stepover.ref".equals(c.getId()))
                .findFirst().orElse(null);
        List<String> stepItems = stepOverRef != null
                ? stepOverRef.getChildren().stream().map(MenuItemConfig::getText).toList()
                : List.of();
        assertTrue(dbgItems.contains("Step Into") || stepItems.contains("Step Into"));
        assertTrue(dbgItems.contains("Step Out") || stepItems.contains("Step Out"));
        assertTrue(dbgItems.contains("View Breakpoints\u2026") || stepItems.contains("View Breakpoints\u2026"));
        assertTrue(dbgItems.contains("Mute Breakpoints") || stepItems.contains("Mute Breakpoints"));

        MenuItemConfig watches = settings.getMenuConfig("root.debug.watches.toolbar");
        assertNotNull(watches);
        List<String> watchItems = watches.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(watchItems.contains("+ New Watch\u2026") || watchItems.contains("New Watch\u2026"));
        assertTrue(watchItems.contains("\u2014 Remove Watch") || watchItems.contains("Remove Watch"));
        assertTrue(watchItems.contains("Move Watch Up"));
        assertTrue(watchItems.contains("Move Watch Down"));
        assertTrue(watchItems.contains("Duplicate Watch"));
    }

    @Test
    @DisplayName("Verify File History and Floating Code Toolbars hierarchies from DataGrip")
    public void testFileHistoryAndFloatingCodeToolbars() {
        MenuItemConfig history = settings.getMenuConfig("root.file.history.toolbar");
        assertNotNull(history);
        List<String> histItems = history.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(histItems.contains("Refresh"));
        assertTrue(histItems.contains("Show Diff"));
        assertTrue(histItems.contains("Show All Affected Files"));
        assertTrue(histItems.contains("View Options"));
        assertTrue(histItems.contains("VcsHistoryActionsGroup.Toolbar"));

        MenuItemConfig vcsHistToolbar = history.getChildren().stream()
                .filter(c -> "vcs.history.actions.group.toolbar".equals(c.getId()))
                .findFirst().orElse(null);
        List<String> vcsHistItems = vcsHistToolbar != null
                ? vcsHistToolbar.getChildren().stream().map(MenuItemConfig::getText).toList()
                : List.of();
        assertTrue(histItems.contains("Resume Indexing") || vcsHistItems.contains("Resume Indexing"));

        MenuItemConfig codeToolbar = settings.getMenuConfig("root.floating.code.toolbar");
        assertNotNull(codeToolbar);
        List<String> codeItems = codeToolbar.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(codeItems.contains("Extract"));
        assertTrue(codeItems.contains("Surround"));
        assertTrue(codeItems.contains("XDebugger.Code.Toolbar"));
        assertTrue(codeItems.contains("// Comment with Line Comment"));
        assertTrue(codeItems.contains("Reformat Code"));
    }

    @Test
    @DisplayName("Verify Markdown, Quick Actions, Run Header, and SQL Floating Toolbars from DataGrip")
    public void testMarkdownAndQuickActionsAndRunToolbars() {
        MenuItemConfig md = settings.getMenuConfig("root.markdown.editor.floating.toolbar");
        assertNotNull(md);
        List<String> mdItems = md.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(mdItems.contains("Set Header Style"));
        assertTrue(mdItems.contains("Bold"));
        assertTrue(mdItems.contains("Italic"));
        assertTrue(mdItems.contains("Strikethrough"));
        assertTrue(mdItems.contains("<> Code"));
        assertTrue(mdItems.contains("Create Link"));
        assertTrue(mdItems.contains("Create Or Change List"));

        MenuItemConfig qa = settings.getMenuConfig("root.quick.actions.popup.toolbar");
        assertNotNull(qa);
        List<String> qaItems = qa.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(qaItems.contains("Load Full Cell"));
        assertTrue(qaItems.contains("Related Rows"));
        assertTrue(qaItems.contains("Open URL"));
        assertTrue(qaItems.contains("Open File URI"));

        MenuItemConfig runHeader = settings.getMenuConfig("root.run.toolwindow.header.toolbar");
        assertNotNull(runHeader);
        List<String> runItems = runHeader.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(runItems.contains("Run"));
        assertTrue(runItems.contains("Debug"));
        assertTrue(runItems.contains("Rerun"));
        assertTrue(runItems.contains("Stop"));

        MenuItemConfig sqlFloating = settings.getMenuConfig("root.sql.floating.toolbar");
        assertNotNull(sqlFloating);
        List<String> sqlItems = sqlFloating.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(sqlItems.contains("Floating Code Toolbar"));
        MenuItemConfig fct = sqlFloating.getChildren().stream()
                .filter(c -> "sql.floating.code.toolbar".equals(c.getId()) || "Floating Code Toolbar".equals(c.getText()))
                .findFirst().orElse(null);
        List<String> fctItems = fct != null ? fct.getChildren().stream().map(MenuItemConfig::getText).toList() : List.of();
        assertTrue(sqlItems.contains("Execute") || fctItems.contains("Execute"));
        assertTrue(sqlItems.contains("Explain Plan") || fctItems.contains("Explain Plan"));
    }

    @Test
    @DisplayName("Verify VCS Local Changes, VCS Log Browser, VCS Log Toolbar, and VCS Operations Popup from DataGrip")
    public void testVcsToolbarsAndOperationsPopup() {
        MenuItemConfig local = settings.getMenuConfig("root.vcs.local.changes.toolbar");
        assertNotNull(local);
        List<String> localItems = local.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(localItems.contains("Refresh"));
        assertTrue(localItems.contains("Commit\u2026"));
        assertTrue(localItems.contains("Toggle Commit UI\u2026"));
        assertTrue(localItems.contains("Rollback\u2026"));
        assertTrue(localItems.contains("Show Diff"));
        assertTrue(localItems.contains("Changelists"));
        assertTrue(localItems.contains("Shelve Silently"));

        MenuItemConfig logChanges = settings.getMenuConfig("root.vcs.log.changes.browser.toolbar");
        assertNotNull(logChanges);
        List<String> logChangeItems = logChanges.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(logChangeItems.contains("Vcs.RepositoryChangesBrowserToolbar"));
        assertTrue(logChangeItems.contains("Show Only Affected Changes"));
        assertTrue(logChangeItems.contains("View Options"));

        MenuItemConfig logToolbar = settings.getMenuConfig("root.vcs.log.toolbar");
        assertNotNull(logToolbar);
        List<String> logToolbarItems = logToolbar.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(logToolbarItems.contains("Resume Indexing"));
        assertTrue(logToolbarItems.contains("Refresh"));
        assertTrue(logToolbarItems.contains("Vcs.Log.Toolbar"));
        assertTrue(logToolbarItems.contains("View Options"));
        assertTrue(logToolbarItems.contains("Go To Hash/Branch/Tag"));

        MenuItemConfig vcsOps = settings.getMenuConfig("root.vcs.operations.popup");
        assertNotNull(vcsOps);
        List<String> vcsOpsItems = vcsOps.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(vcsOpsItems.contains("Vcs.Operations.Popup.VcsAware"));
        assertTrue(vcsOpsItems.contains("Vcs.Operations.Popup.Vcs.Providers"));
        assertTrue(vcsOpsItems.contains("Vcs.Operations.Popup.NonVcsAware"));
        assertTrue(vcsOpsItems.contains("Annotated Line"));
        assertTrue(vcsOpsItems.contains("Show History\u2026"));
    }

    @Test
    @DisplayName("Test ActionManager action catalog categories")
    public void testActionManagerCatalog() {
        ActionManager manager = ActionManager.getInstance();
        List<ActionManager.ActionCatalogCategory> catalog = manager.getActionCatalog();
        assertNotNull(catalog);
        assertFalse(catalog.isEmpty());

        List<String> catNames = catalog.stream()
                .map(ActionManager.ActionCatalogCategory::getName)
                .toList();

        assertTrue(catNames.contains("Editor Actions"));
        assertTrue(catNames.contains("Main Menu"));
        assertTrue(catNames.contains("External Build Systems"));
        assertTrue(catNames.contains("Version Control Systems"));
        assertTrue(catNames.contains("Database"));
        assertTrue(catNames.contains("Debug Actions"));
        assertTrue(catNames.contains("Plugins"));
        assertTrue(catNames.contains("Other"));
    }

    @Test
    @DisplayName("Verify File menu contains exact options matching DataGrip Images 1-4")
    public void testFileMenuCompletenessMatchingActionRegistry() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        assertNotNull(mainMenu);

        MenuItemConfig fileMenu = mainMenu.getChildren().stream()
                .filter(m -> "File".equals(m.getText()) || "menu.file".equals(m.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(fileMenu, "File menu should exist under Main Menu");

        List<String> actionIds = fileMenu.getChildren().stream()
                .map(MenuItemConfig::getId)
                .toList();

        // Check top-level items from Screenshot 1 are present
        assertTrue(actionIds.contains("file.new"), "Should contain New group");
        assertTrue(actionIds.contains("file.open.sql"), "Should contain Open...");
        assertTrue(actionIds.contains("file.save.as"), "Should contain Save As...");
        assertTrue(actionIds.contains("file.recent"), "Should contain Recent Projects");
        assertTrue(actionIds.contains("file.close.project"), "Should contain Close Project");
        assertTrue(actionIds.contains("file.rename.project"), "Should contain Rename Project...");
        assertTrue(actionIds.contains("file.attach.directory"), "Should contain Attach Directory...");
        assertTrue(actionIds.contains("file.settings"), "Should contain Settings...");
        assertTrue(actionIds.contains("file.data.sources"), "Should contain Data Sources...");
        assertTrue(actionIds.contains("file.sql.dialects"), "Should contain SQL Dialects...");
        assertTrue(actionIds.contains("file.sql.scopes"), "Should contain SQL Resolution Scopes...");
        assertTrue(actionIds.contains("file.edit.datasources.xml"), "Should contain Edit dataSources.xml");
        assertTrue(actionIds.contains("file.properties"), "Should contain File Properties");
        assertTrue(actionIds.contains("file.local.history"), "Should contain Local History");
        assertTrue(actionIds.contains("file.save.all"), "Should contain Save All");
        assertTrue(actionIds.contains("file.reload.all"), "Should contain Reload All from Disk");
        assertTrue(actionIds.contains("file.manage.settings"), "Should contain Manage IDE Settings");
        assertTrue(actionIds.contains("file.export"), "Should contain Export");
        assertTrue(actionIds.contains("file.print"), "Should contain Print...");
        assertTrue(actionIds.contains("file.power.save"), "Should contain Power Save Mode");
        assertTrue(actionIds.contains("file.exit"), "Should contain Exit");

        // Verify Recent Projects children (Screenshot 5)
        MenuItemConfig recentGroup = fileMenu.getChildren().stream()
                .filter(c -> "file.recent".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> recentIds = recentGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(recentIds.contains("file.manage.projects"));
    }

    @Test
    @DisplayName("Verify Cloud Data Source submenu has Google Cloud, AWS, and Azure Cloud Explorer")
    public void testFileMenuCloudProviders() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig fileMenu = mainMenu.getChildren().stream()
                .filter(m -> "File".equals(m.getText()))
                .findFirst()
                .orElseThrow();

        MenuItemConfig newGroup = fileMenu.getChildren().stream()
                .filter(m -> "New".equals(m.getText()))
                .findFirst()
                .orElseThrow();

        MenuItemConfig cloudGroup = newGroup.getChildren().stream()
                .filter(m -> "file.new.datasource.cloud".equals(m.getId()))
                .findFirst()
                .orElseThrow();

        List<String> cloudNames = cloudGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(cloudNames.contains("Google Cloud"));
        assertTrue(cloudNames.contains("AWS"));
        assertTrue(cloudNames.contains("Azure Cloud Explorer"));
    }

    @Test
    @DisplayName("Verify File submenus match DataGrip: File Properties, Local History, Manage IDE Settings, Export")
    public void testFileMenuSubmenusMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig fileMenu = mainMenu.getChildren().stream()
                .filter(m -> "File".equals(m.getText()) || "menu.file".equals(m.getId()))
                .findFirst()
                .orElseThrow();

        // 1. File Properties & Line Separators (Screenshot 1)
        MenuItemConfig fileProps = fileMenu.getChildren().stream()
                .filter(c -> "file.properties".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> propIds = fileProps.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertEquals(5, propIds.size(), "File Properties should have 5 items (4 actions + Line Separators submenu)");
        assertEquals("file.props.encoding", propIds.get(0));
        assertEquals("file.remove.bom", propIds.get(1));
        assertEquals("file.add.bom", propIds.get(2));
        assertEquals("file.associate.file.type", propIds.get(3));
        assertEquals("file.line.separators", propIds.get(4));

        MenuItemConfig lineSep = fileProps.getChildren().stream()
                .filter(c -> "file.line.separators".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> lineSepIds = lineSep.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertEquals(3, lineSepIds.size(), "Line Separators should contain CRLF, LF, CR");
        assertEquals("file.line.sep.crlf", lineSepIds.get(0));
        assertEquals("file.line.sep.lf", lineSepIds.get(1));
        assertEquals("file.line.sep.cr", lineSepIds.get(2));

        // 2. Local History (Screenshot 2)
        MenuItemConfig localHist = fileMenu.getChildren().stream()
                .filter(c -> "file.local.history".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> histIds = localHist.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertEquals(4, histIds.size(), "Local History should have exactly 4 items without separators");
        assertEquals("history.show", histIds.get(0));
        assertEquals("history.show.project", histIds.get(1));
        assertEquals("history.recent.changes", histIds.get(2));
        assertEquals("history.put.label", histIds.get(3));

        // 3. Manage IDE Settings (Screenshot 3)
        MenuItemConfig manageSettings = fileMenu.getChildren().stream()
                .filter(c -> "file.manage.settings".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> manageIds = manageSettings.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertEquals(4, manageIds.size(), "Manage IDE Settings should have exactly 4 items without separators");
        assertEquals("file.settings.import", manageIds.get(0));
        assertEquals("file.settings.export", manageIds.get(1));
        assertEquals("file.settings.restore", manageIds.get(2));
        assertEquals("file.settings.backup.sync", manageIds.get(3));

        // 4. Export (Screenshot 4)
        MenuItemConfig exportGroup = fileMenu.getChildren().stream()
                .filter(c -> "file.export".equals(c.getId()))
                .findFirst().orElseThrow();
        List<MenuItemConfig> exportChildren = exportGroup.getChildren();
        assertEquals(1, exportChildren.size(), "Export should contain Files or Selection to HTML");
        assertEquals("file.export.html", exportChildren.get(0).getId());
        assertEquals("Files or Selection to HTML\u2026", exportChildren.get(0).getText());

        // 5. Root File menu 27 items and 6 separators matching DataGrip Screenshot 1
        List<MenuItemConfig> fileItems = fileMenu.getChildren();
        assertEquals(27, fileItems.size(), "File menu must contain 21 items and 6 separators matching DataGrip Screenshot 1");
        assertEquals("file.new", fileItems.get(0).getId());
        assertEquals("file.open.sql", fileItems.get(1).getId());
        assertEquals("file.save.as", fileItems.get(2).getId());
        assertEquals("file.recent", fileItems.get(3).getId());
        assertEquals("file.close.project", fileItems.get(4).getId());
        assertEquals("file.rename.project", fileItems.get(5).getId());
        assertEquals("file.attach.directory", fileItems.get(6).getId());
        assertEquals(MenuItemConfig.Type.SEPARATOR, fileItems.get(7).getType());
        assertEquals("file.settings", fileItems.get(8).getId());
        assertEquals("file.data.sources", fileItems.get(9).getId());
        assertEquals(MenuItemConfig.Type.SEPARATOR, fileItems.get(10).getType());
        assertEquals("file.sql.dialects", fileItems.get(11).getId());
        assertEquals("file.sql.scopes", fileItems.get(12).getId());
        assertEquals("file.edit.datasources.xml", fileItems.get(13).getId());
        assertEquals(MenuItemConfig.Type.SEPARATOR, fileItems.get(14).getType());
        assertEquals("file.properties", fileItems.get(15).getId());
        assertEquals("file.local.history", fileItems.get(16).getId());
        assertEquals(MenuItemConfig.Type.SEPARATOR, fileItems.get(17).getType());
        assertEquals("file.save.all", fileItems.get(18).getId());
        assertEquals("file.reload.all", fileItems.get(19).getId());
        assertEquals(MenuItemConfig.Type.SEPARATOR, fileItems.get(20).getType());
        assertEquals("file.manage.settings", fileItems.get(21).getId());
        assertEquals("file.export", fileItems.get(22).getId());
        assertEquals("file.print", fileItems.get(23).getId());
        assertEquals("file.power.save", fileItems.get(24).getId());
        assertEquals(MenuItemConfig.Type.SEPARATOR, fileItems.get(25).getType());
        assertEquals("file.exit", fileItems.get(26).getId());
    }

    @Test
    @DisplayName("Verify automatic migration of old truncated File menu in settings")
    public void testAutoMigrationOfOutdatedMainMenu() {
        AppSettingsStore.Settings staleSettings = new AppSettingsStore.Settings();
        // Create an outdated File menu without file.close.project or file.new.table
        MenuItemConfig staleFileMenu = MenuItemConfig.group("menu.file", "File", List.of(
                MenuItemConfig.action("file.new", "New"),
                MenuItemConfig.action("file.open.sql", "Open\u2026"),
                MenuItemConfig.action("file.exit", "Exit")
        ));
        MenuItemConfig staleMainMenu = MenuItemConfig.group("root.main.menu", "Main Menu", List.of(staleFileMenu));
        staleSettings.setMenusAndToolbars(List.of(staleMainMenu));

        // When getMenusAndToolbars is called, auto-migration should kick in
        List<MenuItemConfig> migratedRoots = staleSettings.getMenusAndToolbars();
        MenuItemConfig migratedMain = migratedRoots.stream()
                .filter(r -> "root.main.menu".equals(r.getId()))
                .findFirst()
                .orElseThrow();

        MenuItemConfig migratedFile = migratedMain.getChildren().stream()
                .filter(c -> "menu.file".equals(c.getId()))
                .findFirst()
                .orElseThrow();

        List<String> ids = migratedFile.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(ids.contains("file.close.project"), "Migrated File menu must now include file.close.project");
        assertTrue(ids.contains("file.save.as"), "Migrated File menu must now include file.save.as");
        assertTrue(ids.contains("file.recent"), "Migrated File menu must now include file.recent");
        assertTrue(ids.contains("file.settings"), "Migrated File menu must now include file.settings");
    }

    @Test
    @DisplayName("Verify Edit Menu completeness matching DataGrip Images 1-3")
    public void testEditMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig editMenu = mainMenu.getChildren().stream()
                .filter(m -> "Edit".equals(m.getText()) || "menu.edit".equals(m.getId()))
                .findFirst()
                .orElseThrow();

        List<String> editIds = editMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(editIds.contains("edit.undo"));
        assertTrue(editIds.contains("edit.redo"));
        assertTrue(editIds.contains("edit.cut.copy.paste.actions"));
        assertTrue(editIds.contains("edit.delete"));
        assertTrue(editIds.contains("edit.find"));
        assertTrue(editIds.contains("edit.replace"));
        assertTrue(editIds.contains("edit.find.in.files"));
        assertTrue(editIds.contains("edit.replace.in.files"));
        assertTrue(editIds.contains("edit.find.usages"));
        assertTrue(editIds.contains("edit.generate.root.group"));
        assertTrue(editIds.contains("edit.insert.live.template"));
        assertTrue(editIds.contains("edit.surround.with"));
        assertTrue(editIds.contains("edit.format.code"));
        assertTrue(editIds.contains("edit.format.file"));
        assertTrue(editIds.contains("edit.comment.line"));
        assertTrue(editIds.contains("edit.comment.block"));
        assertTrue(editIds.contains("edit.auto.indent"));
        assertTrue(editIds.contains("edit.refactor"));
        assertTrue(editIds.contains("edit.selection"));
        assertTrue(editIds.contains("edit.toggle.case"));
        assertTrue(editIds.contains("edit.join.lines"));
        assertTrue(editIds.contains("edit.duplicate.lines"));
        assertTrue(editIds.contains("edit.sort.lines"));
        assertTrue(editIds.contains("edit.toggle.bookmark"));
        assertTrue(editIds.contains("edit.show.bookmarks"));

        // Verify Cut/Copy/Paste Actions children (Image 1)
        MenuItemConfig ccpGroup = editMenu.getChildren().stream()
                .filter(c -> "edit.cut.copy.paste.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(ccpGroup.isPopup(), "Cut/Copy/Paste Actions should be non-popup group");
        List<String> ccpIds = ccpGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(ccpIds.contains("edit.cut"));
        assertTrue(ccpIds.contains("edit.copy"));
        assertTrue(ccpIds.contains("edit.copy.paths"));
        assertTrue(ccpIds.contains("edit.copy.plain"));
        assertTrue(ccpIds.contains("edit.copy.rich"));
        assertTrue(ccpIds.contains("edit.copy.path.reference.group"));
        assertTrue(ccpIds.contains("edit.paste.group"));
        assertTrue(ccpIds.contains("edit.copy.json.pointer"));

        // Verify Copy Path/Reference... group (Image 1)
        MenuItemConfig copyPathRefGroup = ccpGroup.getChildren().stream()
                .filter(c -> "edit.copy.path.reference.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(copyPathRefGroup.isPopup(), "Copy Path/Reference should be popup submenu");
        List<String> copyPathRefIds = copyPathRefGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(copyPathRefIds.contains("edit.copy.file.reference.group"));
        assertTrue(copyPathRefIds.contains("edit.copy.external.reference.group"));
        assertTrue(copyPathRefIds.contains("edit.copy.reference"));

        MenuItemConfig copyFileRefGroup = copyPathRefGroup.getChildren().stream()
                .filter(c -> "edit.copy.file.reference.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> fileRefIds = copyFileRefGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(fileRefIds.contains("edit.copy.path.absolute"));
        assertTrue(fileRefIds.contains("edit.copy.path.filename"));
        assertTrue(fileRefIds.contains("edit.copy.path.line.number"));
        assertTrue(fileRefIds.contains("edit.copy.path.content.root"));
        assertTrue(fileRefIds.contains("edit.copy.path.source.root"));
        assertTrue(fileRefIds.contains("edit.copy.path.repo.root"));
        assertTrue(fileRefIds.contains("edit.copy.git.hosting.link"));

        // Verify Generate... children (Image 2)
        MenuItemConfig genRoot = editMenu.getChildren().stream()
                .filter(c -> "edit.generate.root.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(genRoot.isPopup(), "Generate root group should be popup");
        List<String> genRootIds = genRoot.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(genRootIds.contains("edit.generate.function"));
        assertTrue(genRootIds.contains("edit.generate.procedure"));
        assertTrue(genRootIds.contains("edit.generate.table"));
        assertTrue(genRootIds.contains("edit.generate.index"));
        assertTrue(genRootIds.contains("edit.generate.xml.tag"));
        assertTrue(genRootIds.contains("edit.generate.implement.methods"));
        assertTrue(genRootIds.contains("edit.generate.markdown.toc"));

        // Verify Refactor children (Image 3)
        MenuItemConfig refactorGroup = editMenu.getChildren().stream()
                .filter(c -> "edit.refactor".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(refactorGroup.isPopup(), "Refactor should be a popup submenu");
        List<String> refactorIds = refactorGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(refactorIds.contains("edit.refactor.rename"));
        assertTrue(refactorIds.contains("edit.refactor.expand.column.list"));
        assertTrue(refactorIds.contains("edit.refactor.convert.subquery"));
        assertTrue(refactorIds.contains("edit.refactor.extract.introduce.group"));
        assertTrue(refactorIds.contains("edit.refactor.qualify.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.unqualify.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.quote.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.unquote.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.flip.expression"));
        assertTrue(refactorIds.contains("edit.refactor.inject.language"));
        assertTrue(refactorIds.contains("edit.refactor.uninject.language"));

        // Verify Selection children (Image 4)
        MenuItemConfig selectionGroup = editMenu.getChildren().stream()
                .filter(c -> "edit.selection".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(selectionGroup.isPopup(), "Selection should be a popup submenu");
        List<String> selIds = selectionGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(selIds.contains("edit.select.all"));
        assertTrue(selIds.contains("edit.selection.add.carets.ends"));
        assertTrue(selIds.contains("edit.selection.extend"));
        assertTrue(selIds.contains("edit.selection.shrink"));
    }

    @Test
    @DisplayName("Verify View Menu completeness matching DataGrip Images 1-4")
    public void testViewMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig viewMenu = mainMenu.getChildren().stream()
                .filter(m -> "View".equals(m.getText()) || "menu.view".equals(m.getId()))
                .findFirst()
                .orElseThrow();

        List<String> viewIds = viewMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(viewIds.contains("view.toolwindows"));
        assertTrue(viewIds.contains("view.appearance"));
        assertTrue(viewIds.contains("view.recent.locations"));
        assertTrue(viewIds.contains("view.recent.files"));
        assertTrue(viewIds.contains("view.recent.changed.files"));
        assertTrue(viewIds.contains("view.recent.changes"));
        assertTrue(viewIds.contains("view.font.increase"));
        assertTrue(viewIds.contains("view.font.decrease"));
        assertTrue(viewIds.contains("view.font.reset"));

        // Verify Tool Windows (Image 1)
        MenuItemConfig twGroup = viewMenu.getChildren().stream()
                .filter(c -> "view.toolwindows".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(twGroup.isPopup(), "Tool Windows should be popup submenu");
        List<String> twIds = twGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(twIds.contains("view.toolwindow.commit"));
        assertTrue(twIds.contains("view.toolwindow.database"));
        assertTrue(twIds.contains("view.toolwindow.files"));
        assertTrue(twIds.contains("view.toolwindow.find"));
        assertTrue(twIds.contains("view.toggle.run"));
        assertTrue(twIds.contains("view.toolwindow.debug"));
        assertTrue(twIds.contains("view.toolwindow.problems"));
        assertTrue(twIds.contains("view.toolwindow.structure"));
        assertTrue(twIds.contains("view.toolwindow.services"));
        assertTrue(twIds.contains("view.toolwindow.vcs"));
        assertTrue(twIds.contains("view.toolwindow.ai"));
        assertTrue(twIds.contains("view.toolwindow.backup.sync"));
        assertTrue(twIds.contains("view.toolwindow.bookmarks"));
        assertTrue(twIds.contains("view.toolwindow.coverage"));
        assertTrue(twIds.contains("view.toolwindow.dbchanges"));
        assertTrue(twIds.contains("view.toolwindow.hierarchy"));
        assertTrue(twIds.contains("view.toolwindow.learn"));
        assertTrue(twIds.contains("view.toolwindow.notifications"));
        assertTrue(twIds.contains("view.toolwindow.terminal"));
        assertTrue(twIds.contains("view.toolwindow.todo"));

        // Verify Appearance (Image 2)
        MenuItemConfig appGroup = viewMenu.getChildren().stream()
                .filter(c -> "view.appearance".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(appGroup.isPopup(), "Appearance should be popup submenu");
        List<String> appIds = appGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(appIds.contains("view.appearance.presentation.mode"));
        assertTrue(appIds.contains("view.appearance.distraction.free"));
        assertTrue(appIds.contains("view.appearance.full.screen"));
        assertTrue(appIds.contains("view.appearance.zen.mode"));
        assertTrue(appIds.contains("view.appearance.compact.mode"));
        assertTrue(appIds.contains("view.appearance.zoom.ide"));
        assertTrue(appIds.contains("view.appearance.presentation.assistant"));
        assertTrue(appIds.contains("view.appearance.mainmenu"));
        assertTrue(appIds.contains("view.appearance.toolbar"));
        assertTrue(appIds.contains("view.appearance.navbar"));
        assertTrue(appIds.contains("view.appearance.toolwindowbars"));
        assertTrue(appIds.contains("view.appearance.statusbar"));
        assertTrue(appIds.contains("view.appearance.statusbar.widgets"));

        // Verify Main Menu Placement Submenu (Image 2)
        MenuItemConfig mmGroup = appGroup.getChildren().stream()
                .filter(c -> "view.appearance.mainmenu".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(mmGroup.isPopup(), "Main Menu placement should be popup submenu");
        List<String> mmIds = mmGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(mmIds.contains("view.appearance.mainmenu.hamburger"));
        assertTrue(mmIds.contains("view.appearance.mainmenu.merge"));
        assertTrue(mmIds.contains("view.appearance.mainmenu.above"));

        // Verify Navigation Bar Submenu (Image 3)
        MenuItemConfig nbGroup = appGroup.getChildren().stream()
                .filter(c -> "view.appearance.navbar".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(nbGroup.isPopup(), "Navigation Bar should be popup submenu");
        List<String> nbIds = nbGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(nbIds.contains("view.appearance.navbar.top"));
        assertTrue(nbIds.contains("view.appearance.navbar.bottom"));
        assertTrue(nbIds.contains("view.appearance.navbar.hide"));
        assertTrue(nbIds.contains("view.appearance.navbar.members"));

        // Verify Status Bar Widgets Submenu (Image 4)
        MenuItemConfig sbGroup = appGroup.getChildren().stream()
                .filter(c -> "view.appearance.statusbar.widgets".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(sbGroup.isPopup(), "Status Bar Widgets should be popup submenu");
        List<String> sbIds = sbGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertEquals(17, sbIds.size());
        assertTrue(sbIds.contains("view.widget.status.text"));
        assertTrue(sbIds.contains("view.widget.fs.sync"));
        assertTrue(sbIds.contains("view.widget.remote.wire"));
        assertTrue(sbIds.contains("view.widget.aggregator"));
        assertTrue(sbIds.contains("view.widget.grid.pos"));
        assertTrue(sbIds.contains("view.widget.line.col"));
        assertTrue(sbIds.contains("view.widget.lang.services"));
        assertTrue(sbIds.contains("view.widget.line.sep"));
        assertTrue(sbIds.contains("view.widget.file.enc"));
        assertTrue(sbIds.contains("view.widget.power.save"));
        assertTrue(sbIds.contains("view.widget.editor.sel"));
        assertTrue(sbIds.contains("view.widget.indentation"));
        assertTrue(sbIds.contains("view.widget.json.schema"));
        assertTrue(sbIds.contains("view.widget.mcp.server"));
        assertTrue(sbIds.contains("view.widget.readonly"));
        assertTrue(sbIds.contains("view.widget.notifications"));
        assertTrue(sbIds.contains("view.widget.memory"));
    }

    @Test
    @DisplayName("Verify Navigate Menu completeness matching DataGrip Image 1")
    public void testNavigateMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig navMenu = mainMenu.getChildren().stream()
                .filter(m -> "Navigate".equals(m.getText()) || "menu.navigate".equals(m.getId()))
                .findFirst()
                .orElseThrow();

        List<String> navIds = navMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(navIds.contains("nav.back"), "Should contain Back");
        assertTrue(navIds.contains("nav.forward"), "Should contain Forward");
        assertTrue(navIds.contains("header.search"), "Should contain Search Everywhere");
        assertTrue(navIds.contains("nav.goto.database.object"), "Should contain Database Object...");
        assertTrue(navIds.contains("nav.goto.file"), "Should contain File...");
        assertTrue(navIds.contains("nav.goto.symbol"), "Should contain Code...");
        assertTrue(navIds.contains("nav.goto.text"), "Should contain Text...");
        assertTrue(navIds.contains("navigate.show.diagram"), "Should contain Show Diagram...");
        assertTrue(navIds.contains("navigate.jump.query.console"), "Should contain Jump to Query Console...");
        assertTrue(navIds.contains("nav.select.in"), "Should contain Select In...");
        assertTrue(navIds.contains("nav.goto.declaration"), "Should contain Declaration or Usages");
        assertTrue(navIds.contains("nav.goto.ddl"), "Should contain Go to DDL");
        assertTrue(navIds.contains("navigate.scroll.from.editor"), "Should contain Scroll from Editor");
        assertTrue(navIds.contains("nav.jump.to.nav.bar"), "Should contain Jump to Navigation Bar");
        assertTrue(navIds.contains("nav.file.path"), "Should contain File Path");
        assertTrue(navIds.contains("nav.next.statement"), "Should contain Next Statement");
        assertTrue(navIds.contains("nav.prev.statement"), "Should contain Previous Statement");
    }

    @Test
    @DisplayName("Verify Code Menu is removed from Main Menu per user specification")
    public void testCodeMenuRemovedFromMainMenu() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        assertNotNull(mainMenu);
        assertFalse(mainMenu.getChildren().stream()
                .anyMatch(m -> "Code".equalsIgnoreCase(m.getText()) || "menu.code".equalsIgnoreCase(m.getId())),
                "Code menu must not be present in Main Menu");
    }

    @Test
    @DisplayName("Verify Refactor Menu is removed from Main Menu per user specification")
    public void testRefactorMenuRemovedFromMainMenu() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        assertNotNull(mainMenu);
        assertFalse(mainMenu.getChildren().stream()
                .anyMatch(m -> "Refactor".equalsIgnoreCase(m.getText()) || "menu.refactor".equalsIgnoreCase(m.getId())),
                "Refactor menu must not be present in Main Menu");
    }

    @Test
    @DisplayName("Verify ActionCatalog contains Navigate, Code, and Refactor under Main Menu")
    public void testActionCatalogNavigateAndCodeCategories() {
        ActionManager manager = ActionManager.getInstance();
        List<ActionManager.ActionCatalogCategory> catalog = manager.getActionCatalog();
        ActionManager.ActionCatalogCategory mainMenuCat = catalog.stream()
                .filter(c -> "Main Menu".equals(c.getName()))
                .findFirst().orElseThrow();

        List<String> subNames = mainMenuCat.getSubCategories().stream()
                .map(ActionManager.ActionCatalogCategory::getName)
                .toList();

        assertTrue(subNames.contains("Navigate"), "Main Menu must contain Navigate subcategory");
        assertTrue(subNames.contains("Code"), "Main Menu must contain Code subcategory");
        assertTrue(subNames.contains("Refactor"), "Main Menu must contain Refactor subcategory");

        ActionManager.ActionCatalogCategory navCat = mainMenuCat.getSubCategories().stream()
                .filter(c -> "Navigate".equals(c.getName()))
                .findFirst().orElseThrow();
        List<String> navEntryIds = navCat.getEntries().stream()
                .map(ActionManager.ActionCatalogEntry::getId)
                .toList();
        assertTrue(navEntryIds.contains("nav.back"));
        assertTrue(navEntryIds.contains("nav.forward"));
        assertTrue(navEntryIds.contains("nav.goto.class"));
        assertTrue(navEntryIds.contains("nav.goto.file"));
        assertTrue(navEntryIds.contains("nav.related.rows"));

        ActionManager.ActionCatalogCategory codeCat = mainMenuCat.getSubCategories().stream()
                .filter(c -> "Code".equals(c.getName()))
                .findFirst().orElseThrow();
        List<String> codeEntryIds = codeCat.getEntries().stream()
                .map(ActionManager.ActionCatalogEntry::getId)
                .toList();
        assertTrue(codeEntryIds.contains("code.override.methods"));
        assertTrue(codeEntryIds.contains("code.implement.methods"));
        assertTrue(codeEntryIds.contains("code.completion.basic"));
        assertTrue(codeEntryIds.contains("code.completion.call.inline"));
        assertTrue(codeEntryIds.contains("code.inspect.code"));
        assertTrue(codeEntryIds.contains("code.folding.expand"));
        assertTrue(codeEntryIds.contains("code.comment.line"));
        assertTrue(codeEntryIds.contains("code.reformat"));

        ActionManager.ActionCatalogCategory refactorCat = mainMenuCat.getSubCategories().stream()
                .filter(c -> "Refactor".equals(c.getName()))
                .findFirst().orElseThrow();
        List<String> refactorEntryIds = refactorCat.getEntries().stream()
                .map(ActionManager.ActionCatalogEntry::getId)
                .toList();
        assertTrue(refactorEntryIds.contains("refactor.this"));
        assertTrue(refactorEntryIds.contains("refactor.rename"));
        assertTrue(refactorEntryIds.contains("refactor.change.signature"));
        assertTrue(refactorEntryIds.contains("refactor.introduce.variable"));
        assertTrue(refactorEntryIds.contains("refactor.extract.routine"));
        assertTrue(refactorEntryIds.contains("refactor.safe.delete"));

        assertTrue(subNames.contains("Run"), "Main Menu must contain Run subcategory");
        ActionManager.ActionCatalogCategory runCat = mainMenuCat.getSubCategories().stream()
                .filter(c -> "Run".equals(c.getName()))
                .findFirst().orElseThrow();
        List<String> runEntryIds = runCat.getEntries().stream()
                .map(ActionManager.ActionCatalogEntry::getId)
                .toList();
        assertTrue(runEntryIds.contains("run.run"));
        assertTrue(runEntryIds.contains("run.debug"));
        assertTrue(runEntryIds.contains("run.coverage"));
        assertTrue(runEntryIds.contains("run.profiler"));
        assertTrue(runEntryIds.contains("run.step.over"));
        assertTrue(runEntryIds.contains("run.view.breakpoints"));
        assertTrue(runEntryIds.contains("run.test.history"));

        assertTrue(subNames.contains("Tools"), "Main Menu must contain Tools subcategory");
        ActionManager.ActionCatalogCategory toolsCat = mainMenuCat.getSubCategories().stream()
                .filter(c -> "Tools".equals(c.getName()))
                .findFirst().orElseThrow();
        List<String> toolsEntryIds = toolsCat.getEntries().stream()
                .map(ActionManager.ActionCatalogEntry::getId)
                .toList();
        assertTrue(toolsEntryIds.contains("tools.view.psi.structure"));
        assertTrue(toolsEntryIds.contains("tools.create.command.line.launcher"));
        assertTrue(toolsEntryIds.contains("tools.services"));
        assertTrue(toolsEntryIds.contains("tools.convert.schema"));
        assertTrue(toolsEntryIds.contains("tools.markdown.import.word"));

        assertTrue(subNames.contains("Git"), "Main Menu must contain Git subcategory");
        ActionManager.ActionCatalogCategory gitCat = mainMenuCat.getSubCategories().stream()
                .filter(c -> "Git".equals(c.getName()))
                .findFirst().orElseThrow();
        List<String> gitEntryIds = gitCat.getEntries().stream()
                .map(ActionManager.ActionCatalogEntry::getId)
                .toList();
        assertTrue(gitEntryIds.contains("vcs.commit"));
        assertTrue(gitEntryIds.contains("vcs.update.project"));
        assertTrue(gitEntryIds.contains("git.commit.file"));
        assertTrue(gitEntryIds.contains("git.add"));
        assertTrue(gitEntryIds.contains("git.rollback"));
        assertTrue(gitEntryIds.contains("git.push"));
        assertTrue(gitEntryIds.contains("git.branches"));
    }

    @Test
    @DisplayName("Verify Run Menu completeness matching DataGrip Image 2")
    public void testRunMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig runMenu = mainMenu.getChildren().stream()
                .filter(m -> "Run".equals(m.getText()) || "menu.run".equals(m.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(runMenu.isPopup(), "Run should be a popup menu");

        List<String> runIds = runMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(runIds.contains("run.edit.configurations"), "Should contain Edit Configurations...");
        assertTrue(runIds.contains("run.compare.data"), "Should contain Compare Data");
        assertTrue(runIds.contains("run.compare.schema"), "Should contain Compare Schema Structure");
        assertTrue(runIds.contains("run.fulltext.search"), "Should contain Full-Text Search...");
        assertTrue(runIds.contains("run.import.data"), "Should contain Import Data from File(s)...");
    }

    @Test
    @DisplayName("Verify Tools Menu is removed from Main Menu per user specification")
    public void testToolsMenuRemovedFromMainMenu() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        assertNotNull(mainMenu);
        assertFalse(mainMenu.getChildren().stream()
                .anyMatch(m -> "Tools".equalsIgnoreCase(m.getText()) || "menu.tools".equalsIgnoreCase(m.getId())),
                "Tools menu must not be present in Main Menu");
    }

    @Test
    @DisplayName("Verify getMainMenuBarGroups excludes Code, Refactor, and Tools menus")
    public void testMainMenuBarGroupsExcludesCodeRefactorTools() {
        ActionManager manager = ActionManager.getInstance();
        ActionRegistry.initialize(manager);
        List<ActionGroup> groups = ActionRegistry.getMainMenuBarGroups(manager);
        List<String> groupIds = groups.stream().map(ActionGroup::getId).toList();
        assertFalse(groupIds.contains("menu.code"));
        assertFalse(groupIds.contains("menu.refactor"));
        assertFalse(groupIds.contains("menu.tools"));
        assertTrue(groupIds.contains("menu.file"));
        assertTrue(groupIds.contains("menu.edit"));
        assertTrue(groupIds.contains("menu.view"));
        assertTrue(groupIds.contains("menu.navigate"));
        assertTrue(groupIds.contains("menu.run"));
        assertTrue(groupIds.contains("menu.vcs"));
        assertTrue(groupIds.contains("menu.window"));
        assertTrue(groupIds.contains("menu.help"));
    }

    @Test
    @DisplayName("Verify stale settings containing Code, Refactor, Tools or toolbar icons are stripped upon loading")
    public void testStaleCodeRefactorToolsAndToolbarIconsMigrated() {
        AppSettingsStore.Settings staleSettings = new AppSettingsStore.Settings();
        MenuItemConfig staleMenu = MenuItemConfig.group("root.main.menu", "Main Menu", List.of(
                MenuItemConfig.group("menu.file", "File", true, List.of()),
                MenuItemConfig.group("menu.code", "Code", true, List.of()),
                MenuItemConfig.group("menu.refactor", "Refactor", true, List.of()),
                MenuItemConfig.group("menu.tools", "Tools", true, List.of())
        ));
        MenuItemConfig staleToolbar = MenuItemConfig.group("root.main.toolbar", "Main Toolbar", List.of(
                MenuItemConfig.group("toolbar.left", "Left", List.of()),
                MenuItemConfig.group("toolbar.center", "Center", List.of(
                        MenuItemConfig.action("toolbar.single.tool.window.bar", "Single Tool Window Bar", "DESKTOP"),
                        MenuItemConfig.action("toolbar.file.name.widget", "File Name Widget", "FILE_ALT")
                )),
                MenuItemConfig.group("toolbar.right", "Right", List.of())
        ));
        List<MenuItemConfig> modified = new ArrayList<>(staleSettings.getMenusAndToolbars());
        for (int i = 0; i < modified.size(); i++) {
            if ("root.main.menu".equalsIgnoreCase(modified.get(i).getId())) {
                modified.set(i, staleMenu);
            } else if ("root.main.toolbar".equalsIgnoreCase(modified.get(i).getId())) {
                modified.set(i, staleToolbar);
            }
        }
        staleSettings.setMenusAndToolbars(modified);

        MenuItemConfig reloadedMainMenu = staleSettings.getMenuConfig("root.main.menu");
        assertNotNull(reloadedMainMenu);
        assertFalse(reloadedMainMenu.getChildren().stream().anyMatch(c -> "menu.code".equalsIgnoreCase(c.getId())));
        assertFalse(reloadedMainMenu.getChildren().stream().anyMatch(c -> "menu.refactor".equalsIgnoreCase(c.getId())));
        assertFalse(reloadedMainMenu.getChildren().stream().anyMatch(c -> "menu.tools".equalsIgnoreCase(c.getId())));

        MenuItemConfig reloadedToolbar = staleSettings.getMenuConfig("root.main.toolbar");
        assertNotNull(reloadedToolbar);
        MenuItemConfig centerGroup = reloadedToolbar.getChildren().stream()
                .filter(c -> "toolbar.center".equalsIgnoreCase(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(centerGroup.getChildren().stream().anyMatch(c -> "toolbar.single.tool.window.bar".equalsIgnoreCase(c.getId())));
        assertFalse(centerGroup.getChildren().stream().anyMatch(c -> "toolbar.file.name.widget".equalsIgnoreCase(c.getId())));
    }

    @Test
    @DisplayName("Verify VCS Menu completeness matching DataGrip Image 3")
    public void testGitMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig vcsMenu = mainMenu.getChildren().stream()
                .filter(m -> "VCS".equals(m.getText()) || "Git".equals(m.getText()) || "menu.vcs".equals(m.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(vcsMenu.isPopup(), "VCS should be a popup menu");
        assertEquals("VCS", vcsMenu.getText());

        List<String> topIds = vcsMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(topIds.contains("vcs.enable.integration"), "Should contain Enable Version Control Integration...");
        assertTrue(topIds.contains("vcs.operations.popup"), "Should contain VCS Operations Popup...");
        assertTrue(topIds.contains("vcs.apply.patch"), "Should contain Apply Patch...");
        assertTrue(topIds.contains("vcs.apply.patch.clipboard"), "Should contain Apply Patch from Clipboard...");
        assertTrue(topIds.contains("vcs.get.from.vcs"), "Should contain Get from Version Control...");
        assertTrue(topIds.contains("vcs.browse.repository"), "Should contain Browse VCS Repository");
        assertTrue(topIds.contains("vcs.create.git.repository"), "Should contain Create Git Repository...");

        // Browse VCS Repository submenu
        MenuItemConfig browseVcs = vcsMenu.getChildren().stream()
                .filter(c -> "vcs.browse.repository".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(browseVcs.isPopup());
        assertTrue(browseVcs.getChildren().stream().anyMatch(c -> "vcs.show.git.log".equals(c.getId())));
    }

    @Test
    @DisplayName("Verify Window Menu completeness matching DataGrip Images 1-5")
    public void testWindowMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig windowMenu = mainMenu.getChildren().stream()
                .filter(m -> "Window".equals(m.getText()) || "menu.window".equals(m.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(windowMenu.isPopup(), "Window should be a popup menu");

        // Image 4: Window Top-Level items
        List<String> winIds = windowMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(winIds.contains("window.layouts"));
        assertTrue(winIds.contains("window.active.tool.window"));
        assertTrue(winIds.contains("window.editor.tabs"));
        assertTrue(winIds.contains("window.notifications"));
        assertTrue(winIds.contains("window.processes"));
        assertTrue(winIds.contains("window.next.project"));
        assertTrue(winIds.contains("window.prev.project"));
        assertTrue(winIds.contains("window.project.active"));

        // Image 4: Layouts submenu
        MenuItemConfig layouts = windowMenu.getChildren().stream()
                .filter(c -> "window.layouts".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(layouts.isPopup());
        assertEquals("Layouts", layouts.getText());
        List<String> layoutIds = layouts.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(layoutIds.contains("window.layouts.default"));
        assertTrue(layoutIds.contains("window.layouts.custom"));
        assertTrue(layoutIds.contains("window.layouts.save.as.new"));

        // Image 5: Active Tool Window submenu & Resize popup
        MenuItemConfig activeTw = windowMenu.getChildren().stream()
                .filter(c -> "window.active.tool.window".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(activeTw.isPopup());
        List<String> activeTwIds = activeTw.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(activeTwIds.contains("window.toolwindow.hide.active"));
        assertTrue(activeTwIds.contains("window.toolwindow.hide.side"));
        assertTrue(activeTwIds.contains("window.toolwindow.hide.bottom"));
        assertTrue(activeTwIds.contains("window.toolwindow.hide.all"));
        assertTrue(activeTwIds.contains("window.toolwindow.jump.last"));
        assertTrue(activeTwIds.contains("window.toolwindow.maximize"));
        assertTrue(activeTwIds.contains("window.toolwindow.next.tab"));
        assertTrue(activeTwIds.contains("window.toolwindow.prev.tab"));
        assertTrue(activeTwIds.contains("window.toolwindow.close.active.tab"));
        assertTrue(activeTwIds.contains("window.toolwindow.close.other.tabs"));
        assertTrue(activeTwIds.contains("window.toolwindow.close.all.tabs"));
        assertTrue(activeTwIds.contains("window.toolwindow.view.mode"));
        assertTrue(activeTwIds.contains("window.toolwindow.move.to"));
        assertTrue(activeTwIds.contains("window.toolwindow.group.tabs"));
        assertTrue(activeTwIds.contains("window.toolwindow.resize"));

        MenuItemConfig viewMode = activeTw.getChildren().stream()
                .filter(c -> "window.toolwindow.view.mode".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(viewMode.isPopup());
        List<String> viewModeIds = viewMode.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(viewModeIds.contains("window.tw.viewmode.dock.pinned"));
        assertTrue(viewModeIds.contains("window.tw.viewmode.dock.unpinned"));
        assertTrue(viewModeIds.contains("window.tw.viewmode.undock"));
        assertTrue(viewModeIds.contains("window.tw.viewmode.float"));
        assertTrue(viewModeIds.contains("window.tw.viewmode.window"));

        MenuItemConfig resize = activeTw.getChildren().stream()
                .filter(c -> "window.toolwindow.resize".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(resize.isPopup());
        List<String> resizeIds = resize.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(resizeIds.contains("window.tw.resize.left"));
        assertTrue(resizeIds.contains("window.tw.resize.right"));
        assertTrue(resizeIds.contains("window.tw.resize.top"));
        assertTrue(resizeIds.contains("window.tw.resize.bottom"));

        // Image 4: Notifications & Processes submenus
        MenuItemConfig notifications = windowMenu.getChildren().stream()
                .filter(c -> "window.notifications".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(notifications.isPopup());
        List<String> notifIds = notifications.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(notifIds.contains("window.notifications.close.first"));
        assertTrue(notifIds.contains("window.notifications.close.all"));

        MenuItemConfig processes = windowMenu.getChildren().stream()
                .filter(c -> "window.processes".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(processes.isPopup());
        assertEquals("Processes", processes.getText());
        List<String> procIds = processes.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(procIds.contains("window.background.tasks.show"));
        assertTrue(procIds.contains("window.background.tasks.auto.show"));

        // Image 3: Editor Tabs submenu matching DataGrip
        MenuItemConfig editorTabs = windowMenu.getChildren().stream()
                .filter(c -> "window.editor.tabs".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(editorTabs.isPopup());
        List<String> tabIds = editorTabs.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(tabIds.contains("window.editor.next.tab"));
        assertTrue(tabIds.contains("window.editor.prev.tab"));
        assertTrue(tabIds.contains("window.editor.keep.tab.open"));
        assertTrue(tabIds.contains("window.close.tab"));
        assertTrue(tabIds.contains("window.close.other.tabs"));
        assertTrue(tabIds.contains("window.close.all.tabs"));
        assertTrue(tabIds.contains("window.close.unmodified.tabs"));
        assertTrue(tabIds.contains("window.close.all.but.pinned"));
        assertTrue(tabIds.contains("window.close.tabs.left"));
        assertTrue(tabIds.contains("window.close.tabs.right"));
        assertTrue(tabIds.contains("window.close.all.readonly"));
        assertTrue(tabIds.contains("window.reopen.tab"));
        assertTrue(tabIds.contains("window.editor.split.chooser.navigation"));
        assertTrue(tabIds.contains("window.editor.stretch.top"));
        assertTrue(tabIds.contains("window.editor.stretch.left"));
        assertTrue(tabIds.contains("window.editor.stretch.bottom"));
        assertTrue(tabIds.contains("window.editor.stretch.right"));
        assertTrue(tabIds.contains("window.editor.change.splitter.orientation"));
        assertTrue(tabIds.contains("window.editor.maximize.splits"));
        assertTrue(tabIds.contains("window.editor.unsplit"));
        assertTrue(tabIds.contains("window.unsplit.all"));
        assertTrue(tabIds.contains("window.editor.goto.next.splitter"));
        assertTrue(tabIds.contains("window.editor.goto.prev.splitter"));
        assertTrue(tabIds.contains("window.editor.configure.tabs"));

        // Image 5: Split with Chooser Navigation popup
        MenuItemConfig splitChooserNav = editorTabs.getChildren().stream()
                .filter(c -> "window.editor.split.chooser.navigation".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(splitChooserNav.isPopup());
        List<String> splitNavIds = splitChooserNav.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(splitNavIds.contains("window.split.next"));
        assertTrue(splitNavIds.contains("window.split.prev"));
        assertTrue(splitNavIds.contains("window.split.exit.chooser"));
        assertTrue(splitNavIds.contains("window.split.chooser.split"));
        assertTrue(splitNavIds.contains("window.split.chooser.duplicate"));
        assertTrue(splitNavIds.contains("window.split.chooser.without.split"));
        assertTrue(splitNavIds.contains("window.split.switch.up"));
        assertTrue(splitNavIds.contains("window.split.switch.left"));
        assertTrue(splitNavIds.contains("window.split.switch.down"));
        assertTrue(splitNavIds.contains("window.split.switch.right"));

        // Verify ActionCatalog contains Window category and entries
        ActionManager actionManager = ActionManager.getInstance();
        var catalog = actionManager.getActionCatalog();
        var mainMenuCat = catalog.stream().filter(c -> "Main Menu".equals(c.getName())).findFirst().orElseThrow();
        var windowCat = mainMenuCat.getSubCategories().stream().filter(c -> "Window".equals(c.getName())).findFirst().orElseThrow();
        assertFalse(windowCat.getEntries().isEmpty());
        List<String> catalogIds = windowCat.getEntries().stream().map(ActionManager.ActionCatalogEntry::getId).toList();
        assertTrue(catalogIds.contains("window.minimize"));
        assertTrue(catalogIds.contains("window.zoom"));
        assertTrue(catalogIds.contains("window.layouts.default"));
        assertTrue(catalogIds.contains("window.editor.open.as.editor.tab"));
        assertTrue(catalogIds.contains("window.background.tasks.show"));
        assertTrue(catalogIds.contains("window.project.merge.all"));
    }

    @Test
    @DisplayName("Verify Help Menu structure and completeness matching DataGrip screenshot")
    public void testHelpMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        assertNotNull(mainMenu);

        MenuItemConfig helpMenu = mainMenu.getChildren().stream()
                .filter(c -> "menu.help".equals(c.getId()))
                .findFirst().orElseThrow();
        assertEquals("Help", helpMenu.getText());
        assertTrue(helpMenu.isPopup());

        List<MenuItemConfig> children = helpMenu.getChildren();
        List<String> childIds = children.stream().map(MenuItemConfig::getId).toList();

        // 8 sections: 26 actions/submenus + 7 separators = 33 entries matching DataGrip screenshot
        assertEquals(33, children.size(), "Help menu should contain 33 entries (26 items + 7 separators)");

        // Top-level Help menu items matching screenshot
        assertTrue(childIds.contains("help.welcome"));
        assertTrue(childIds.contains("help.find.action"));
        assertTrue(childIds.contains("help.help"));
        assertTrue(childIds.contains("help.tip.of.the.day"));
        assertTrue(childIds.contains("help.my.productivity"));
        assertTrue(childIds.contains("help.learn.features"));
        assertTrue(childIds.contains("help.whats.new"));
        assertTrue(childIds.contains("help.getting.started"));
        assertTrue(childIds.contains("help.youtube"));
        assertTrue(childIds.contains("help.shortcuts.pdf"));
        assertTrue(childIds.contains("help.contact.support"));
        assertTrue(childIds.contains("help.bug.report"));
        assertTrue(childIds.contains("help.submit.feedback"));
        assertTrue(childIds.contains("help.show.log.in.files"));
        assertTrue(childIds.contains("help.show.sql.log.in.files"));
        assertTrue(childIds.contains("help.collect.logs"));
        assertTrue(childIds.contains("help.cpu.profiling"));
        assertTrue(childIds.contains("help.capture.memory.snapshot"));
        assertTrue(childIds.contains("help.diagnostic.tools"));
        assertTrue(childIds.contains("help.change.memory.settings"));
        assertTrue(childIds.contains("help.custom.properties"));
        assertTrue(childIds.contains("help.custom.vm.options"));
        assertTrue(childIds.contains("help.delete.leftover.dirs"));
        assertTrue(childIds.contains("help.registration.register"));
        assertTrue(childIds.contains("help.updates"));
        assertTrue(childIds.contains("help.about"));

        // Diagnostic Tools popup submenu matching screenshot: 9 items + 4 separators = 13 entries
        MenuItemConfig diagnosticTools = children.stream()
                .filter(c -> "help.diagnostic.tools".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(diagnosticTools.isPopup());
        assertEquals(13, diagnosticTools.getChildren().size(), "Diagnostic Tools should contain 13 entries (9 items + 4 separators)");
        List<String> diagIds = diagnosticTools.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(diagIds.contains("help.diagnostic.activity.monitor"));
        assertTrue(diagIds.contains("help.diagnostic.dump.threads"));
        assertTrue(diagIds.contains("help.diagnostic.debug.log.settings"));
        assertTrue(diagIds.contains("help.diagnostic.special.files"));
        assertTrue(diagIds.contains("help.diagnostic.start.cpu.profiling"));
        assertTrue(diagIds.contains("help.diagnostic.start.async.profiler"));
        assertTrue(diagIds.contains("help.diagnostic.capture.memory.snapshot"));
        assertTrue(diagIds.contains("help.diagnostic.profile.indexing"));
        assertTrue(diagIds.contains("help.diagnostic.open.indexing.diagnostics"));
        long diagSeparators = diagnosticTools.getChildren().stream()
                .filter(c -> c.getType() == MenuItemConfig.Type.SEPARATOR).count();
        assertEquals(4, diagSeparators, "Diagnostic Tools should contain 4 separators");

        // Verify ActionCatalog contains Help category and entries
        ActionManager actionManager = ActionManager.getInstance();
        var catalog = actionManager.getActionCatalog();
        var mainMenuCat = catalog.stream().filter(c -> "Main Menu".equals(c.getName())).findFirst().orElseThrow();
        var helpCat = mainMenuCat.getSubCategories().stream().filter(c -> "Help".equals(c.getName())).findFirst().orElseThrow();
        assertFalse(helpCat.getEntries().isEmpty());
        List<String> catalogIds = helpCat.getEntries().stream().map(ActionManager.ActionCatalogEntry::getId).toList();
        assertTrue(catalogIds.contains("help.welcome"));
        assertTrue(catalogIds.contains("help.find.action"));
        assertTrue(catalogIds.contains("help.help"));
        assertTrue(catalogIds.contains("help.learn.features"));
        assertTrue(catalogIds.contains("help.whats.new"));
        assertTrue(catalogIds.contains("help.configure.new.ui"));
        assertTrue(catalogIds.contains("help.tip.of.the.day"));
        assertTrue(catalogIds.contains("help.my.productivity"));
        assertTrue(catalogIds.contains("help.diagnostic.activity.monitor"));
        assertTrue(catalogIds.contains("help.diagnostic.run.memory.tester"));
        assertTrue(catalogIds.contains("help.diagnostic.start.cpu.profiling"));
        assertTrue(catalogIds.contains("help.diagnostic.start.async.profiler"));
        assertTrue(catalogIds.contains("help.diagnostic.capture.memory.snapshot"));
        assertTrue(catalogIds.contains("help.diagnostic.profile.indexing"));
        assertTrue(catalogIds.contains("help.diagnostic.open.indexing.diagnostics"));
        assertTrue(catalogIds.contains("help.registration.register"));
        assertTrue(catalogIds.contains("help.updates"));
        assertTrue(catalogIds.contains("help.about"));
    }

    @Test
    @DisplayName("Verify Scope View Popup Menu structure, sub-groups, and action catalog matching DataGrip (Images 1-5)")
    public void testScopeViewPopupMenuStructureAndOrderMatchingDataGrip() {
        MenuItemConfig scopeViewRoot = settings.getMenuConfig("root.scope.view.popup");
        assertNotNull(scopeViewRoot, "root.scope.view.popup should exist");
        assertEquals("Scope View Popup Menu", scopeViewRoot.getText());

        MenuItemConfig projPopup = scopeViewRoot.getChildren().stream()
                .filter(c -> "scope.project.view.popup".equals(c.getId()))
                .findFirst().orElseThrow();
        assertEquals("Project View Popup Menu", projPopup.getText());
        assertTrue(projPopup.isPopup());

        List<String> items = projPopup.getChildren().stream().map(MenuItemConfig::getText).toList();

        // Image 1 & 2: Top-level entries
        assertTrue(items.contains("Attach Directory to Project\u2026"));
        assertTrue(items.contains("New"));
        assertTrue(items.contains("Associate with File Type\u2026"));
        assertTrue(items.contains("Restore Default Extensions"));
        assertTrue(items.contains("Cut/Copy/Paste Actions"));
        assertTrue(items.contains("FileEditor.ImportToDatabase.Group"));
        assertTrue(items.contains("Edit Source"));
        assertTrue(items.contains("ChangesView.ApplyPatch.LangGroup"));
        assertTrue(items.contains("Find Usages"));
        assertTrue(items.contains("Find in Files\u2026"));
        assertTrue(items.contains("Replace in Files\u2026"));
        assertTrue(items.contains("InspectCodeActionInPopupMenus"));
        assertTrue(items.contains("Rename\u2026"));
        assertTrue(items.contains("Project View Popup Refactoring Group"));
        assertTrue(items.contains("<anonymous-group-0>"));
        assertTrue(items.contains("Project View Popup Menu Modify Group"));
        assertTrue(items.contains("Project View Popup Menu Run Group"));
        assertTrue(items.contains("SplitRevealGroup"));
        assertTrue(items.contains("VCS/LVCS Actions"));
        assertTrue(items.contains("Cache Recovery"));
        assertTrue(items.contains("Reload from Disk"));
        assertTrue(items.contains("Go to Link Target"));
        assertTrue(items.contains("Compare Files"));
        assertTrue(items.contains("Compare File with Editor"));
        assertTrue(items.contains("External Tools"));
        assertTrue(items.contains("Project View Popup Menu Settings Group"));
        assertTrue(items.contains("Set Background Image"));
        assertTrue(items.contains("Diagrams"));
        assertTrue(items.contains("Jump to External Editor"));
        assertTrue(items.contains("Convert to PNG"));
        assertTrue(items.contains("File Associations"));

        // Image 3: New Submenu
        MenuItemConfig newGroup = projPopup.getChildren().stream()
                .filter(c -> "project.new".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(newGroup.isPopup());
        List<String> newGroupTexts = newGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(newGroupTexts.contains("DBE.NewFile"));
        assertTrue(newGroupTexts.contains("Web Development Templates"));
        assertTrue(newGroupTexts.contains("DatabaseView.NewGroup"));

        MenuItemConfig dbeNewFile = newGroup.getChildren().stream()
                .filter(c -> "project.new.dbe.file".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> dbeChildren = dbeNewFile.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(dbeChildren.contains("SQL File"));
        assertTrue(dbeChildren.contains("File"));
        assertTrue(dbeChildren.contains("Scratch File"));
        assertTrue(dbeChildren.contains("Directory/Package"));
        assertTrue(dbeChildren.contains("FileTemplateSeparatorGroup"));

        MenuItemConfig webDevTemplates = newGroup.getChildren().stream()
                .filter(c -> "file.web.dev.templates".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> webDevChildren = webDevTemplates.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(webDevChildren.contains("XML"));
        assertTrue(webDevChildren.contains("Microservices Templates"));
        assertTrue(webDevChildren.contains("From Template"));
        assertTrue(webDevChildren.contains("XML Configuration File"));

        MenuItemConfig dbViewNewGroup = newGroup.getChildren().stream()
                .filter(c -> "project.databaseview.new.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> dbViewChildren = dbViewNewGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(dbViewChildren.contains("DatabaseView.AddActionGroup"));
        assertTrue(dbViewChildren.contains("Create Data Source"));

        MenuItemConfig dbAddAction = dbViewNewGroup.getChildren().stream()
                .filter(c -> "project.databaseview.add.action.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(dbAddAction.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Query Console"));
        assertTrue(dbAddAction.getChildren().stream().map(MenuItemConfig::getText).toList().contains("DatabaseView.Ddl.AddObject"));

        MenuItemConfig createDs = dbViewNewGroup.getChildren().stream()
                .filter(c -> "file.new.create.datasource".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> createDsChildren = createDs.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(createDsChildren.contains("Data Source from URL"));
        assertTrue(createDsChildren.contains("Data Source from Path"));
        assertTrue(createDsChildren.contains("Add Data Source from Selection\u2026"));
        assertTrue(createDsChildren.contains("Data Source in Path"));
        assertTrue(createDsChildren.contains("Import from Clipboard"));
        assertTrue(createDsChildren.contains("Driver"));

        // Image 4: Cut/Copy/Paste Actions
        MenuItemConfig ccpGroup = projPopup.getChildren().stream()
                .filter(c -> "project.cut.copy.paste".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> ccpTexts = ccpGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(ccpTexts.contains("Cut"));
        assertTrue(ccpTexts.contains("Copy"));
        assertTrue(ccpTexts.contains("Copy Paths"));
        assertTrue(ccpTexts.contains("Copy as Plain Text"));
        assertTrue(ccpTexts.contains("Copy as Rich Text"));
        assertTrue(ccpTexts.contains("Copy Path/Reference\u2026"));
        assertTrue(ccpTexts.contains("Paste"));
        assertTrue(ccpTexts.contains("Copy JSON Pointer"));

        MenuItemConfig copyPathRef = ccpGroup.getChildren().stream()
                .filter(c -> "project.copy.path.ref.group".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig copyFileRef = copyPathRef.getChildren().stream()
                .filter(c -> "project.copy.file.ref".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> copyFileRefTexts = copyFileRef.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(copyFileRefTexts.contains("Absolute Path"));
        assertTrue(copyFileRefTexts.contains("File Name"));
        assertTrue(copyFileRefTexts.contains("Path With Line Number"));
        assertTrue(copyFileRefTexts.contains("Path From Content Root"));
        assertTrue(copyFileRefTexts.contains("Path From Source Root"));
        assertTrue(copyFileRefTexts.contains("Path From Repository Root"));
        assertTrue(copyFileRefTexts.contains("Git.Hosting.Copy.Link.Group"));

        MenuItemConfig copyExtRef = copyPathRef.getChildren().stream()
                .filter(c -> "project.copy.ext.ref".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(copyExtRef.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Toolbox URL"));

        MenuItemConfig fileEditorImport = projPopup.getChildren().stream()
                .filter(c -> "fileeditor.import.to.database.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(fileEditorImport.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Import to Database\u2026"));

        MenuItemConfig changesViewApplyPatch = projPopup.getChildren().stream()
                .filter(c -> "changesview.applypatch.langgroup".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(changesViewApplyPatch.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Apply Patch\u2026"));

        // Image 5: InspectCodeActionInPopupMenus
        MenuItemConfig inspectCode = projPopup.getChildren().stream()
                .filter(c -> "project.inspect.code.action".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(inspectCode.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Inspect Code\u2026"));

        // Image 5: Project View Popup Refactoring Group
        MenuItemConfig refactorGroup = projPopup.getChildren().stream()
                .filter(c -> "project.refactoring.group".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig refactorSubmenu = refactorGroup.getChildren().stream()
                .filter(c -> "project.refactor.submenu".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> refactorSubmenuTexts = refactorSubmenu.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(refactorSubmenuTexts.contains("Refactor This\u2026"));
        assertTrue(refactorSubmenuTexts.contains("Rename\u2026"));
        assertTrue(refactorSubmenuTexts.contains("Change Signature\u2026"));
        assertTrue(refactorSubmenuTexts.contains("Modify Object\u2026"));
        assertTrue(refactorSubmenuTexts.contains("Extract/Introduce"));
        assertTrue(refactorSubmenuTexts.contains("Inline\u2026"));
        assertTrue(refactorSubmenuTexts.contains("Move\u2026"));

        MenuItemConfig extractIntro = refactorSubmenu.getChildren().stream()
                .filter(c -> "project.refactor.extract.introduce".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> extractTexts = extractIntro.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(extractTexts.contains("Introduce Variable\u2026"));
        assertTrue(extractTexts.contains("Extract Routine\u2026"));
        assertTrue(extractTexts.contains("Table alias\u2026"));
        assertTrue(extractTexts.contains("Introduce Constant\u2026"));
        assertTrue(extractTexts.contains("Introduce Field\u2026"));
        assertTrue(extractTexts.contains("Introduce Parameter\u2026"));
        assertTrue(extractTexts.contains("Introduce Parameter Object\u2026"));
        assertTrue(extractTexts.contains("Extract Method\u2026"));
        assertTrue(extractTexts.contains("Extract Delegate\u2026"));
        assertTrue(extractTexts.contains("Include File\u2026"));
        assertTrue(extractTexts.contains("Extract Interface\u2026"));
        assertTrue(extractTexts.contains("Extract Superclass\u2026"));
        assertTrue(extractTexts.contains("Extract Module\u2026"));
        assertTrue(extractTexts.contains("Subquery as CTE"));

        // Verify Image 1-3 detailed sub-structures in Scope View Popup Menu:
        // Bookmarks in <anonymous-group-0>
        MenuItemConfig anon0 = projPopup.getChildren().stream()
                .filter(c -> "project.anonymous.group.0".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig bookmarks = anon0.getChildren().stream()
                .filter(c -> "project.bookmarks".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> bookmarkItems = bookmarks.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(bookmarkItems.contains("Add Bookmark to Another List"));
        assertTrue(bookmarkItems.contains("Rename Bookmark\u2026"));
        assertTrue(bookmarkItems.contains("Toggle Bookmark"));

        // Modify Group
        MenuItemConfig modGroup = projPopup.getChildren().stream()
                .filter(c -> "project.modify.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> modItems = modGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(modItems.contains("Reformat Code"));
        assertTrue(modItems.contains("Reformat JSON"));
        assertTrue(modItems.contains("Optimize Imports"));
        assertTrue(modItems.contains("Delete"));
        assertTrue(modItems.contains("Change File Language"));
        assertTrue(modItems.contains("Change SQL Dialect"));
        assertTrue(modItems.contains("Mark File as"));

        // Run Group
        MenuItemConfig runGroup = projPopup.getChildren().stream()
                .filter(c -> "project.run.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> runGroupChildren = runGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(runGroupChildren.contains("Run Configurations"));
        assertTrue(runGroupChildren.contains("Console.Jdbc.RunContextGroup"));

        MenuItemConfig consoleJdbc = runGroup.getChildren().stream()
                .filter(c -> "console.jdbc.run.context.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> consoleJdbcTexts = consoleJdbc.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(consoleJdbcTexts.contains("Attach Data Source"));
        assertTrue(consoleJdbcTexts.contains("Recompile\u2026"));
        assertTrue(consoleJdbcTexts.contains("Explain Plan"));
        assertTrue(consoleJdbcTexts.contains("Execute"));
        assertTrue(consoleJdbcTexts.contains("Export Data\u2026"));
        assertTrue(consoleJdbcTexts.contains("Debug"));
        assertTrue(consoleJdbcTexts.contains("Debug Routine\u2026"));

        // VCS/LVCS Actions
        MenuItemConfig vcsLvcs = projPopup.getChildren().stream()
                .filter(c -> "project.vcs.lvcs.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> vcsLvcsTexts = vcsLvcs.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(vcsLvcsTexts.contains("Local History"));
        assertTrue(vcsLvcsTexts.contains("Version Control Group"));

        MenuItemConfig localHist = vcsLvcs.getChildren().stream()
                .filter(c -> "editor.local.history.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> localHistTexts = localHist.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(localHistTexts.contains("Show History\u2026"));
        assertTrue(localHistTexts.contains("Show History for Selection\u2026"));
        assertTrue(localHistTexts.contains("Show Project History\u2026"));
        assertTrue(localHistTexts.contains("Recent Changes"));
        assertTrue(localHistTexts.contains("Put Label\u2026"));

        // Diagrams
        MenuItemConfig diagrams = projPopup.getChildren().stream()
                .filter(c -> "project.diagrams".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> diagTexts = diagrams.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(diagTexts.contains("Show Diagram\u2026"));
        assertTrue(diagTexts.contains("Show Diagram Popup\u2026"));
        assertTrue(diagTexts.contains("Show Local Changes as UML"));

        // Verify ActionCatalog contains Scope View Popup Menu category
        ActionManager actionManager = ActionManager.getInstance();
        var catalog = actionManager.getActionCatalog();
        var scopeCat = catalog.stream().filter(c -> "Scope View Popup Menu".equals(c.getName())).findFirst().orElseThrow();
        assertFalse(scopeCat.getEntries().isEmpty());
        List<String> catalogIds = scopeCat.getEntries().stream().map(ActionManager.ActionCatalogEntry::getId).toList();
        assertTrue(catalogIds.contains("file.attach.directory"));
        assertTrue(catalogIds.contains("file.new.sqlfile"));
        assertTrue(catalogIds.contains("edit.cut"));
        assertTrue(catalogIds.contains("edit.copy"));
        assertTrue(catalogIds.contains("edit.paste"));
        assertTrue(catalogIds.contains("refactor.rename"));
        assertTrue(catalogIds.contains("refactor.this"));
        assertTrue(catalogIds.contains("code.inspect.code"));
        assertTrue(catalogIds.contains("project.cache.recovery"));
        assertTrue(catalogIds.contains("project.reload.from.disk"));
        assertTrue(catalogIds.contains("diagrams.show.uml"));

        // Verify Auto-migration for root.scope.view.popup
        MenuItemConfig staleScopePopup = MenuItemConfig.group("root.scope.view.popup", "Scope View Popup Menu", List.of(
                MenuItemConfig.group("scope.project.view.popup", "Project View Popup Menu", List.of())
        ));
        List<MenuItemConfig> testMenus = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testMenus.size(); i++) {
            if ("root.scope.view.popup".equalsIgnoreCase(testMenus.get(i).getId())) {
                testMenus.set(i, staleScopePopup);
                break;
            }
        }
        settings.setMenusAndToolbars(testMenus);
        List<MenuItemConfig> migrated = settings.getMenusAndToolbars();
        MenuItemConfig migratedScope = migrated.stream()
                .filter(r -> "root.scope.view.popup".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig migratedInner = migratedScope.getChildren().stream()
                .filter(c -> "scope.project.view.popup".equalsIgnoreCase(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(migratedInner.getChildren().isEmpty(), "Stale empty scope popup must be migrated with full children");
    }

    @Test
    @DisplayName("Verify Navigation Bar Popup Menu structure, sub-groups, and action catalog matching DataGrip (Images 4-5)")
    public void testNavigationBarPopupMenuStructureAndOrderMatchingDataGrip() {
        MenuItemConfig navBarRoot = settings.getMenuConfig("root.navigation.bar.popup");
        assertNotNull(navBarRoot, "root.navigation.bar.popup should exist");
        assertEquals("Navigation Bar Popup Menu", navBarRoot.getText());

        List<String> items = navBarRoot.getChildren().stream().map(MenuItemConfig::getText).toList();

        // Image 4: Top-level entries
        assertTrue(items.contains("New"));
        assertTrue(items.contains("Associate with File Type\u2026"));
        assertTrue(items.contains("Cut/Copy/Paste Actions"));
        assertTrue(items.contains("Jump to Source"));
        assertTrue(items.contains("ChangesView.ApplyPatch.LangGroup"));
        assertTrue(items.contains("Navigation Bar"));
        assertTrue(items.contains("Members in Navigation Bar"));
        assertTrue(items.contains("Find Usages"));
        assertTrue(items.contains("Find in Files\u2026"));
        assertTrue(items.contains("Replace in Files\u2026"));
        assertTrue(items.contains("Rename\u2026"));
        assertTrue(items.contains("Project View Popup Refactoring Group"));
        assertTrue(items.contains("Project View Popup Menu Modify Group"));
        assertTrue(items.contains("Project View Popup Menu Run Group"));
        assertTrue(items.contains("SplitRevealGroup"));
        assertTrue(items.contains("VCS/LVCS Actions"));
        assertTrue(items.contains("Reload from Disk"));
        assertTrue(items.contains("External Tools"));
        assertTrue(items.contains("Project View Popup Menu Settings Group"));
        assertTrue(items.contains("Diagrams"));

        // Verify Jump to Source has EDIT icon, Reload from Disk has SYNC icon
        MenuItemConfig jumpSource = navBarRoot.getChildren().stream()
                .filter(c -> "navbar.jump.source".equals(c.getId()))
                .findFirst().orElseThrow();
        assertEquals("EDIT", jumpSource.getIconName());

        MenuItemConfig reloadDisk = navBarRoot.getChildren().stream()
                .filter(c -> "navbar.reload.disk".equals(c.getId()))
                .findFirst().orElseThrow();
        assertEquals("SYNC", reloadDisk.getIconName());

        // Image 5: New Submenu
        MenuItemConfig newGroup = navBarRoot.getChildren().stream()
                .filter(c -> "navbar.new".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(newGroup.isPopup());
        List<String> newGroupTexts = newGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(newGroupTexts.contains("DBE.NewFile"));
        assertTrue(newGroupTexts.contains("Web Development Templates"));
        assertTrue(newGroupTexts.contains("DatabaseView.NewGroup"));

        // DBE.NewFile
        MenuItemConfig dbeNewFile = newGroup.getChildren().stream()
                .filter(c -> "project.new.dbe.file".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> dbeChildren = dbeNewFile.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(dbeChildren.contains("SQL File"));
        assertTrue(dbeChildren.contains("File"));
        assertTrue(dbeChildren.contains("Scratch File"));
        assertTrue(dbeChildren.contains("Directory/Package"));
        assertTrue(dbeChildren.contains("FileTemplateSeparatorGroup"));

        // Web Development Templates
        MenuItemConfig webDevTemplates = newGroup.getChildren().stream()
                .filter(c -> "file.web.dev.templates".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> webDevChildren = webDevTemplates.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(webDevChildren.contains("XML"));
        assertTrue(webDevChildren.contains("Microservices Templates"));
        assertTrue(webDevChildren.contains("From Template"));
        assertTrue(webDevChildren.contains("XML Configuration File"));

        // DatabaseView.NewGroup
        MenuItemConfig dbViewNewGroup = newGroup.getChildren().stream()
                .filter(c -> "project.databaseview.new.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> dbViewChildren = dbViewNewGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(dbViewChildren.contains("DatabaseView.AddActionGroup"));
        assertTrue(dbViewChildren.contains("Create Data Source"));

        MenuItemConfig dbAddAction = dbViewNewGroup.getChildren().stream()
                .filter(c -> "project.databaseview.add.action.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(dbAddAction.getChildren().stream().map(MenuItemConfig::getText).toList().contains("Query Console"));
        assertTrue(dbAddAction.getChildren().stream().map(MenuItemConfig::getText).toList().contains("DatabaseView.Ddl.AddObject"));

        MenuItemConfig createDs = dbViewNewGroup.getChildren().stream()
                .filter(c -> "file.new.create.datasource".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> createDsChildren = createDs.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(createDsChildren.contains("Data Source from URL"));
        assertTrue(createDsChildren.contains("Data Source from Path"));
        assertTrue(createDsChildren.contains("Add Data Source from Selection\u2026"));
        assertTrue(createDsChildren.contains("Data Source in Path"));
        assertTrue(createDsChildren.contains("Import from Clipboard"));
        assertTrue(createDsChildren.contains("Driver"));

        // Cut/Copy/Paste Actions
        MenuItemConfig ccpGroup = navBarRoot.getChildren().stream()
                .filter(c -> "navbar.cut.copy.paste".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> ccpTexts = ccpGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(ccpTexts.contains("Cut"));
        assertTrue(ccpTexts.contains("Copy"));
        assertTrue(ccpTexts.contains("Copy Paths"));
        assertTrue(ccpTexts.contains("Copy as Plain Text"));
        assertTrue(ccpTexts.contains("Copy as Rich Text"));
        MenuItemConfig copyPathRef = ccpGroup.getChildren().stream()
                .filter(c -> "project.copy.path.ref.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertEquals(4, copyPathRef.getChildren().size());
        assertEquals("project.copy.file.ref", copyPathRef.getChildren().get(0).getId());
        assertEquals(MenuItemConfig.Type.SEPARATOR, copyPathRef.getChildren().get(1).getType());
        assertEquals("project.copy.ext.ref", copyPathRef.getChildren().get(2).getId());
        assertEquals("project.copy.ref.action", copyPathRef.getChildren().get(3).getId());

        // Refactoring Submenu
        MenuItemConfig refactorGroup = navBarRoot.getChildren().stream()
                .filter(c -> "navbar.refactoring.group".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig refactorSubmenu = refactorGroup.getChildren().stream()
                .filter(c -> "project.refactor.submenu".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> refactorSubmenuTexts = refactorSubmenu.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(refactorSubmenuTexts.contains("Refactor This\u2026"));
        assertTrue(refactorSubmenuTexts.contains("Rename\u2026"));
        assertTrue(refactorSubmenuTexts.contains("Change Signature\u2026"));
        assertTrue(refactorSubmenuTexts.contains("Modify Object\u2026"));
        assertTrue(refactorSubmenuTexts.contains("Extract/Introduce"));
        assertTrue(refactorSubmenuTexts.contains("Inline\u2026"));
        assertTrue(refactorSubmenuTexts.contains("Move\u2026"));

        // Modify Group
        MenuItemConfig modGroup = navBarRoot.getChildren().stream()
                .filter(c -> "navbar.modify.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> modTexts = modGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(modTexts.contains("Reformat Code"));
        assertTrue(modTexts.contains("Reformat JSON"));
        assertTrue(modTexts.contains("Optimize Imports"));
        assertTrue(modTexts.contains("Delete"));
        assertTrue(modTexts.contains("Change File Language"));
        assertTrue(modTexts.contains("Change SQL Dialect"));
        assertTrue(modTexts.contains("Mark File as"));

        // Run Group
        MenuItemConfig runGroup = navBarRoot.getChildren().stream()
                .filter(c -> "navbar.run.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> runTexts = runGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(runTexts.contains("Run Configurations"));
        assertTrue(runTexts.contains("Console.Jdbc.RunContextGroup"));

        // SplitRevealGroup
        MenuItemConfig splitGroup = navBarRoot.getChildren().stream()
                .filter(c -> "navbar.split.reveal.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> splitTexts = splitGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(splitTexts.contains("Open in Right Split"));
        assertTrue(splitTexts.contains("Open in Split with Chooser\u2026"));
        assertTrue(splitTexts.contains("Open In"));

        // VCS/LVCS Actions
        MenuItemConfig vcsGroup = navBarRoot.getChildren().stream()
                .filter(c -> "navbar.vcs.lvcs.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> vcsTexts = vcsGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(vcsTexts.contains("Local History"));
        assertTrue(vcsTexts.contains("Version Control Group"));

        // Diagrams (Image 5)
        MenuItemConfig diagramsGroup = navBarRoot.getChildren().stream()
                .filter(c -> "navbar.diagrams".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> diagTexts = diagramsGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(diagTexts.contains("Show Diagram\u2026"));
        assertTrue(diagTexts.contains("Show Diagram Popup\u2026"));
        assertTrue(diagTexts.contains("Show Local Changes as UML"));
        assertEquals(3, diagramsGroup.getChildren().size());

        // Action Catalog
        ActionManager actionManager = ActionManager.getInstance();
        var catalog = actionManager.getActionCatalog();
        var navBarCat = catalog.stream().filter(c -> "Navigation Bar Popup Menu".equals(c.getName())).findFirst().orElseThrow();
        assertFalse(navBarCat.getEntries().isEmpty());
        List<String> catIds = navBarCat.getEntries().stream().map(ActionManager.ActionCatalogEntry::getId).toList();
        assertTrue(catIds.contains("file.new.sqlfile"));
        assertTrue(catIds.contains("navbar.associate.file.type"));
        assertTrue(catIds.contains("edit.cut"));
        assertTrue(catIds.contains("edit.copy"));
        assertTrue(catIds.contains("edit.paste"));
        assertTrue(catIds.contains("navbar.jump.source"));
        assertTrue(catIds.contains("navbar.navigation.bar"));
        assertTrue(catIds.contains("navbar.members.navigation.bar"));
        assertTrue(catIds.contains("refactor.rename"));
        assertTrue(catIds.contains("code.reformat"));
        assertTrue(catIds.contains("navbar.reload.disk"));
        assertTrue(catIds.contains("navbar.external.tools"));
        assertTrue(catIds.contains("diagrams.show.uml"));

        // Verify Auto-migration for root.navigation.bar.popup
        MenuItemConfig staleNavBarPopup = MenuItemConfig.group("root.navigation.bar.popup", "Navigation Bar Popup Menu", List.of(
                MenuItemConfig.group("navbar.new", "New", List.of())
        ));
        List<MenuItemConfig> testMenus = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testMenus.size(); i++) {
            if ("root.navigation.bar.popup".equalsIgnoreCase(testMenus.get(i).getId())) {
                testMenus.set(i, staleNavBarPopup);
                break;
            }
        }
        settings.setMenusAndToolbars(testMenus);
        List<MenuItemConfig> migrated = settings.getMenusAndToolbars();
        MenuItemConfig migratedNavBar = migrated.stream()
                .filter(r -> "root.navigation.bar.popup".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig migratedNew = migratedNavBar.getChildren().stream()
                .filter(c -> "navbar.new".equalsIgnoreCase(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(migratedNew.getChildren().isEmpty(), "Stale empty navbar popup must be migrated with full children");
    }

    @Test
    @DisplayName("Verify Navigation Bar Toolbar hierarchy, groups, icons, catalog, and auto-migration matching DataGrip Image 1")
    public void testNavigationBarToolbarStructureAndOrderMatchingDataGrip() {
        MenuItemConfig navToolbar = settings.getMenuConfig("root.navigation.bar.toolbar");
        assertNotNull(navToolbar);
        assertEquals("Navigation Bar Toolbar", navToolbar.getText());

        List<MenuItemConfig> children = navToolbar.getChildren();
        assertFalse(children.isEmpty());

        // 1. Toolbar Run Actions group
        MenuItemConfig runActions = children.stream()
                .filter(c -> "navbar.toolbar.run.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        assertEquals("Toolbar Run Actions", runActions.getText());
        assertFalse(runActions.isPopup());

        List<MenuItemConfig> runChildren = runActions.getChildren();
        assertEquals("navbar.select.run.debug.config", runChildren.get(0).getId());
        assertEquals("Select Run/Debug Configuration", runChildren.get(0).getText());

        // Run/Debug popup group
        MenuItemConfig runDebugGrp = runChildren.get(1);
        assertEquals("navbar.toolbar.run.debug.group", runDebugGrp.getId());
        assertEquals("Run/Debug", runDebugGrp.getText());
        assertTrue(runDebugGrp.isPopup());
        List<MenuItemConfig> rdChildren = runDebugGrp.getChildren();
        assertEquals("middle.run", rdChildren.get(0).getId());
        assertEquals("Run", rdChildren.get(0).getText());
        assertEquals("PLAY", rdChildren.get(0).getIconName());
        assertEquals("debug.start", rdChildren.get(1).getId());
        assertEquals("Debug", rdChildren.get(1).getText());
        assertEquals("BUG", rdChildren.get(1).getIconName());
        assertEquals("run.coverage.context.configuration", rdChildren.get(2).getId());
        assertEquals("Run with Coverage", rdChildren.get(2).getText());
        assertEquals("SHIELD_ALT", rdChildren.get(2).getIconName());
        assertEquals("run.profiler.context.configuration", rdChildren.get(3).getId());
        assertEquals("Run with Profiler", rdChildren.get(3).getText());
        assertEquals("TACHOMETER_ALT", rdChildren.get(3).getIconName());

        assertEquals("debug.stop", runChildren.get(2).getId());
        assertEquals("Stop", runChildren.get(2).getText());
        assertEquals("STOP", runChildren.get(2).getIconName());
        assertEquals(MenuItemConfig.Type.SEPARATOR, runChildren.get(3).getType());

        // 2. NavBarVcsGroup
        MenuItemConfig vcsGroup = children.stream()
                .filter(c -> "navbar.vcs.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertEquals("NavBarVcsGroup", vcsGroup.getText());
        assertFalse(vcsGroup.isPopup());

        MenuItemConfig vcsActions = vcsGroup.getChildren().get(0);
        assertEquals("navbar.vcs.actions", vcsActions.getId());
        assertEquals("VcsNavBarToolbarActions", vcsActions.getText());
        List<MenuItemConfig> vcsItems = vcsActions.getChildren();
        assertEquals("navbar.vcs.label", vcsItems.get(0).getId());
        assertEquals("vcs.update.project", vcsItems.get(1).getId());
        assertEquals("DOWNLOAD", vcsItems.get(1).getIconName());
        assertEquals("vcs.commit.primary", vcsItems.get(2).getId());
        assertEquals("CHECK", vcsItems.get(2).getIconName());
        assertEquals("vcs.commit.secondary", vcsItems.get(3).getId());
        assertEquals("CHECK", vcsItems.get(3).getIconName());
        assertEquals("vcs.toggle.commit.ui", vcsItems.get(4).getId());
        assertEquals("vcs.push", vcsItems.get(5).getId());
        assertEquals("UPLOAD", vcsItems.get(5).getIconName());
        assertEquals("vcs.show.history", vcsItems.get(6).getId());
        assertEquals("HISTORY", vcsItems.get(6).getIconName());
        assertEquals("vcs.rollback", vcsItems.get(7).getId());
        assertEquals("UNDO", vcsItems.get(7).getIconName());
        assertEquals(MenuItemConfig.Type.SEPARATOR, vcsItems.get(8).getType());

        // Separator, Others, Separator, AIAssistantHubPopupAction, Search Everywhere, IDE and Project Settings
        assertTrue(children.stream().anyMatch(c -> "navbar.toolbar.others".equals(c.getId())));
        MenuItemConfig aiAction = children.stream().filter(c -> "navbar.ai.assistant".equals(c.getId())).findFirst().orElseThrow();
        assertEquals("AIAssistantHubPopupAction", aiAction.getText());
        assertEquals("ROBOT", aiAction.getIconName());

        MenuItemConfig searchAction = children.stream().filter(c -> "nav.search.everywhere".equals(c.getId())).findFirst().orElseThrow();
        assertEquals("Search Everywhere", searchAction.getText());
        assertEquals("SEARCH", searchAction.getIconName());

        MenuItemConfig settingsAction = children.stream().filter(c -> "file.settings".equals(c.getId())).findFirst().orElseThrow();
        assertEquals("IDE and Project Settings", settingsAction.getText());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "Navigation Bar Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertFalse(cat.getEntries().isEmpty());
        List<String> entryIds = cat.getEntries().stream().map(ActionManager.ActionCatalogEntry::getId).toList();
        assertTrue(entryIds.contains("middle.run"));
        assertTrue(entryIds.contains("debug.start"));
        assertTrue(entryIds.contains("navbar.ai.assistant"));
        assertTrue(entryIds.contains("nav.search.everywhere"));

        // Auto-migration test
        MenuItemConfig staleTb = MenuItemConfig.group("root.navigation.bar.toolbar", "Navigation Bar Toolbar", List.of(
                MenuItemConfig.group("navbar.toolbar.run.actions", "Toolbar Run Actions", List.of())
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.navigation.bar.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleTb);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.navigation.bar.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig migRun = migrated.getChildren().stream()
                .filter(c -> "navbar.toolbar.run.actions".equalsIgnoreCase(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(migRun.getChildren().isEmpty(), "Stale navbar toolbar must be migrated with full children");
    }

    @Test
    @DisplayName("Verify Debug Header More Popup hierarchy, icons, catalog, and auto-migration matching DataGrip Image 2")
    public void testDebugHeaderMorePopupStructureAndOrderMatchingDataGrip() {
        MenuItemConfig debugMore = settings.getMenuConfig("root.debug.header.more.popup");
        assertNotNull(debugMore);
        assertEquals("Debug Header More Popup", debugMore.getText());

        List<MenuItemConfig> items = debugMore.getChildren();
        assertEquals(11, items.size());

        assertEquals("debug.force.step.over", items.get(0).getId());
        assertEquals("Force Step Over", items.get(0).getText());
        assertEquals("STEP_FORWARD", items.get(0).getIconName());

        assertEquals("debug.force.step.into", items.get(1).getId());
        assertEquals("Force Step Into", items.get(1).getText());
        assertEquals("ARROW_DOWN", items.get(1).getIconName());

        assertEquals("debug.smart.step.into", items.get(2).getId());
        assertEquals("Smart Step Into", items.get(2).getText());
        assertEquals("ARROW_RIGHT", items.get(2).getIconName());

        assertEquals(MenuItemConfig.Type.SEPARATOR, items.get(3).getType());

        assertEquals("debug.run.to.cursor", items.get(4).getId());
        assertEquals("Run to Cursor", items.get(4).getText());
        assertEquals("PLAY", items.get(4).getIconName());

        assertEquals("debug.force.run.to.cursor", items.get(5).getId());
        assertEquals("Force Run to Cursor", items.get(5).getText());
        assertEquals("PLAY", items.get(5).getIconName());

        assertEquals(MenuItemConfig.Type.SEPARATOR, items.get(6).getType());

        assertEquals("debug.show.point", items.get(7).getId());
        assertEquals("Show Execution Point", items.get(7).getText());
        assertEquals("BARS", items.get(7).getIconName());

        assertEquals(MenuItemConfig.Type.SEPARATOR, items.get(8).getType());

        assertEquals("debug.evaluate.expression", items.get(9).getId());
        assertEquals("Evaluate Expression\u2026", items.get(9).getText());
        assertEquals("CALCULATOR", items.get(9).getIconName());

        assertEquals("debug.reset.frame", items.get(10).getId());
        assertEquals("Reset Frame", items.get(10).getText());
        assertEquals("UNDO", items.get(10).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "Debug Header More Popup".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(8, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleMore = MenuItemConfig.group("root.debug.header.more.popup", "Debug Header More Popup", List.of(
                MenuItemConfig.action("debug.run.to.cursor", "Run to Cursor")
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.debug.header.more.popup".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleMore);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.debug.header.more.popup".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        assertTrue(migrated.getChildren().size() >= 10, "Stale debug more popup must be migrated with full children");
    }

    @Test
    @DisplayName("Verify Debug Header Toolbar hierarchy, nested StepOver.Ref, icons, catalog, and auto-migration matching DataGrip Image 3")
    public void testDebugHeaderToolbarStructureAndOrderMatchingDataGrip() {
        MenuItemConfig debugTb = settings.getMenuConfig("root.debug.header.toolbar");
        assertNotNull(debugTb);
        assertEquals("Debug Header Toolbar", debugTb.getText());

        List<MenuItemConfig> items = debugTb.getChildren();
        assertEquals(8, items.size());

        assertEquals("middle.run", items.get(0).getId());
        assertEquals("Run", items.get(0).getText());
        assertEquals("PLAY", items.get(0).getIconName());

        assertEquals("debug.start", items.get(1).getId());
        assertEquals("Debug", items.get(1).getText());
        assertEquals("BUG", items.get(1).getIconName());

        assertEquals("debug.rerun", items.get(2).getId());
        assertEquals("Rerun", items.get(2).getText());

        assertEquals("debug.stop", items.get(3).getId());
        assertEquals("Stop", items.get(3).getText());
        assertEquals("STOP", items.get(3).getIconName());

        assertEquals(MenuItemConfig.Type.SEPARATOR, items.get(4).getType());

        // Resume.Ref
        MenuItemConfig resumeRef = items.get(5);
        assertEquals("debug.resume.ref", resumeRef.getId());
        assertEquals("Resume.Ref", resumeRef.getText());
        assertFalse(resumeRef.isPopup());
        assertEquals("debug.resume.program", resumeRef.getChildren().get(0).getId());
        assertEquals("Resume Program", resumeRef.getChildren().get(0).getText());
        assertEquals("PLAY", resumeRef.getChildren().get(0).getIconName());

        // Pause.Ref
        MenuItemConfig pauseRef = items.get(6);
        assertEquals("debug.pause.ref", pauseRef.getId());
        assertEquals("Pause.Ref", pauseRef.getText());
        assertFalse(pauseRef.isPopup());
        assertEquals("debug.pause.program", pauseRef.getChildren().get(0).getId());
        assertEquals("Pause Program", pauseRef.getChildren().get(0).getText());
        assertEquals("PAUSE", pauseRef.getChildren().get(0).getIconName());

        // StepOver.Ref
        MenuItemConfig stepOverRef = items.get(7);
        assertEquals("debug.stepover.ref", stepOverRef.getId());
        assertEquals("StepOver.Ref", stepOverRef.getText());
        assertFalse(stepOverRef.isPopup());
        List<MenuItemConfig> stepChildren = stepOverRef.getChildren();
        assertEquals(6, stepChildren.size());
        assertEquals("debug.step.over", stepChildren.get(0).getId());
        assertEquals("Step Over", stepChildren.get(0).getText());
        assertEquals("STEP_FORWARD", stepChildren.get(0).getIconName());

        assertEquals("debug.step.into", stepChildren.get(1).getId());
        assertEquals("Step Into", stepChildren.get(1).getText());
        assertEquals("ARROW_DOWN", stepChildren.get(1).getIconName());

        assertEquals("debug.step.out", stepChildren.get(2).getId());
        assertEquals("Step Out", stepChildren.get(2).getText());
        assertEquals("ARROW_UP", stepChildren.get(2).getIconName());

        assertEquals(MenuItemConfig.Type.SEPARATOR, stepChildren.get(3).getType());

        assertEquals("debug.view.breakpoints", stepChildren.get(4).getId());
        assertEquals("View Breakpoints\u2026", stepChildren.get(4).getText());
        assertEquals("CIRCLE", stepChildren.get(4).getIconName());

        assertEquals("debug.mute.breakpoints", stepChildren.get(5).getId());
        assertEquals("Mute Breakpoints", stepChildren.get(5).getText());
        assertEquals("BAN", stepChildren.get(5).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "Debug Header Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(11, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleDbgTb = MenuItemConfig.group("root.debug.header.toolbar", "Debug Header Toolbar", List.of(
                MenuItemConfig.action("middle.run", "Run")
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.debug.header.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleDbgTb);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.debug.header.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig migStep = migrated.getChildren().stream()
                .filter(c -> "debug.stepover.ref".equalsIgnoreCase(c.getId()))
                .findFirst().orElseThrow();
        assertEquals(6, migStep.getChildren().size(), "Stale debug toolbar must be migrated with StepOver.Ref full children");
    }

    @Test
    @DisplayName("Verify Debug Watches Toolbar hierarchy, icons, catalog, and auto-migration matching DataGrip Image 4")
    public void testDebugWatchesToolbarStructureAndOrderMatchingDataGrip() {
        MenuItemConfig watches = settings.getMenuConfig("root.debug.watches.toolbar");
        assertNotNull(watches);
        assertEquals("Debug Watches Toolbar", watches.getText());

        List<MenuItemConfig> items = watches.getChildren();
        assertEquals(5, items.size());

        assertEquals("watches.new", items.get(0).getId());
        assertEquals("New Watch\u2026", items.get(0).getText());
        assertEquals("PLUS", items.get(0).getIconName());

        assertEquals("watches.remove", items.get(1).getId());
        assertEquals("Remove Watch", items.get(1).getText());
        assertEquals("MINUS", items.get(1).getIconName());

        assertEquals("watches.move.up", items.get(2).getId());
        assertEquals("Move Watch Up", items.get(2).getText());
        assertEquals("ARROW_UP", items.get(2).getIconName());

        assertEquals("watches.move.down", items.get(3).getId());
        assertEquals("Move Watch Down", items.get(3).getText());
        assertEquals("ARROW_DOWN", items.get(3).getIconName());

        assertEquals("watches.duplicate", items.get(4).getId());
        assertEquals("Duplicate Watch", items.get(4).getText());
        assertEquals("COPY", items.get(4).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "Debug Watches Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(5, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleWatches = MenuItemConfig.group("root.debug.watches.toolbar", "Debug Watches Toolbar", List.of(
                MenuItemConfig.action("watches.unknown", "Unknown")
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.debug.watches.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleWatches);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.debug.watches.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        assertEquals("watches.new", migrated.getChildren().get(0).getId(), "Stale watches toolbar must be migrated");
    }

    @Test
    @DisplayName("Verify File History Toolbar hierarchy, View Options submenus, icons, catalog, and auto-migration matching DataGrip Image 5")
    public void testFileHistoryToolbarStructureAndOrderMatchingDataGrip() {
        MenuItemConfig histTb = settings.getMenuConfig("root.file.history.toolbar");
        assertNotNull(histTb);
        assertEquals("File History Toolbar", histTb.getText());

        List<MenuItemConfig> items = histTb.getChildren();
        assertEquals(6, items.size());

        assertEquals("vcs.history.refresh", items.get(0).getId());
        assertEquals("Refresh", items.get(0).getText());
        assertEquals("SYNC", items.get(0).getIconName());

        assertEquals("vcs.history.diff", items.get(1).getId());
        assertEquals("Show Diff", items.get(1).getText());
        assertEquals("EXCHANGE_ALT", items.get(1).getIconName());

        assertEquals("vcs.history.show.affected.files", items.get(2).getId());
        assertEquals("Show All Affected Files", items.get(2).getText());
        assertEquals("LIST", items.get(2).getIconName());

        assertEquals(MenuItemConfig.Type.SEPARATOR, items.get(3).getType());

        // View Options group
        MenuItemConfig viewOptions = items.get(4);
        assertEquals("vcs.history.view.options", viewOptions.getId());
        assertEquals("View Options", viewOptions.getText());
        assertTrue(viewOptions.isPopup());
        List<MenuItemConfig> voChildren = viewOptions.getChildren();
        assertEquals(5, voChildren.size());
        assertEquals(MenuItemConfig.Type.SEPARATOR, voChildren.get(0).getType());
        assertEquals("vcs.history.commit.timestamp", voChildren.get(1).getId());
        assertEquals("Commit Timestamp", voChildren.get(1).getText());
        assertEquals("vcs.history.columns", voChildren.get(2).getId());
        assertEquals("Columns", voChildren.get(2).getText());

        // Configure Layout group
        MenuItemConfig configLayout = voChildren.get(3);
        assertEquals("vcs.history.configure.layout", configLayout.getId());
        assertEquals("Configure Layout", configLayout.getText());
        assertTrue(configLayout.isPopup());
        List<MenuItemConfig> clChildren = configLayout.getChildren();
        assertEquals(4, clChildren.size());
        assertEquals(MenuItemConfig.Type.SEPARATOR, clChildren.get(0).getType());
        assertEquals("vcs.history.show.details", clChildren.get(1).getId());
        assertEquals("Show Details", clChildren.get(1).getText());
        assertEquals("vcs.history.show.diff.preview", clChildren.get(2).getId());
        assertEquals("Show Diff Preview", clChildren.get(2).getText());

        // Diff Preview Location group
        MenuItemConfig diffPreviewLoc = clChildren.get(3);
        assertEquals("vcs.history.diff.preview.location", diffPreviewLoc.getId());
        assertEquals("Diff Preview Location", diffPreviewLoc.getText());
        assertTrue(diffPreviewLoc.isPopup());
        List<MenuItemConfig> dplChildren = diffPreviewLoc.getChildren();
        assertEquals(2, dplChildren.size());
        assertEquals("vcs.history.diff.preview.bottom", dplChildren.get(0).getId());
        assertEquals("Bottom", dplChildren.get(0).getText());
        assertEquals("vcs.history.diff.preview.right", dplChildren.get(1).getId());
        assertEquals("Right", dplChildren.get(1).getText());

        assertEquals(MenuItemConfig.Type.SEPARATOR, voChildren.get(4).getType());

        // VcsHistoryActionsGroup.Toolbar
        MenuItemConfig vcsHistGrp = items.get(5);
        assertEquals("vcs.history.actions.group.toolbar", vcsHistGrp.getId());
        assertEquals("VcsHistoryActionsGroup.Toolbar", vcsHistGrp.getText());
        assertFalse(vcsHistGrp.isPopup());
        List<MenuItemConfig> vhgChildren = vcsHistGrp.getChildren();
        assertEquals(2, vhgChildren.size());
        assertEquals("git.hosting.open.in.browser.group", vhgChildren.get(0).getId());
        assertEquals("Git.Hosting.Open.In.Browser.Group", vhgChildren.get(0).getText());
        assertEquals("vcs.history.resume.indexing", vhgChildren.get(1).getId());
        assertEquals("Resume Indexing", vhgChildren.get(1).getText());
        assertEquals("PLAY", vhgChildren.get(1).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "File History Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(11, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleHist = MenuItemConfig.group("root.file.history.toolbar", "File History Toolbar", List.of(
                MenuItemConfig.group("vcs.history.view.options", "View Options", List.of())
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.file.history.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleHist);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.file.history.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig migVo = migrated.getChildren().stream()
                .filter(c -> "vcs.history.view.options".equalsIgnoreCase(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(migVo.getChildren().isEmpty(), "Stale file history toolbar must be migrated with full children");
    }

    @Test
    @DisplayName("Verify Floating Code Toolbar hierarchy, Extract/Surround groups, icons, catalog, and auto-migration matching DataGrip Image 1")
    public void testFloatingCodeToolbarStructureAndOrderMatchingDataGrip() {
        MenuItemConfig fct = settings.getMenuConfig("root.floating.code.toolbar");
        assertNotNull(fct);
        assertEquals("Floating Code Toolbar", fct.getText());

        List<MenuItemConfig> items = fct.getChildren();
        assertEquals(5, items.size());

        // 1. Extract group
        MenuItemConfig extract = items.get(0);
        assertEquals("code.extract.group", extract.getId());
        assertEquals("Extract", extract.getText());
        assertFalse(extract.isPopup());
        List<MenuItemConfig> extChildren = extract.getChildren();
        assertEquals(4, extChildren.size());
        assertEquals("refactor.extract.method", extChildren.get(0).getId());
        assertEquals("refactor.introduce.variable", extChildren.get(1).getId());
        assertEquals("refactor.introduce.constant", extChildren.get(2).getId());
        assertEquals("refactor.introduce.parameter", extChildren.get(3).getId());

        // 2. Surround group
        MenuItemConfig surround = items.get(1);
        assertEquals("code.surround.group", surround.getId());
        assertEquals("Surround", surround.getText());
        assertFalse(surround.isPopup());
        List<MenuItemConfig> surChildren = surround.getChildren();
        assertEquals(3, surChildren.size());
        assertEquals("code.surround.try.catch", surChildren.get(0).getId());
        assertEquals("try / catch", surChildren.get(0).getText());
        assertEquals("code.surround.try.catch.finally", surChildren.get(1).getId());
        assertEquals("try / catch / finally", surChildren.get(1).getText());
        assertEquals("code.surround.if", surChildren.get(2).getId());
        assertEquals("if", surChildren.get(2).getText());

        // 3. Comment with Line Comment
        assertEquals("code.comment.line", items.get(2).getId());
        assertEquals("// Comment with Line Comment", items.get(2).getText());

        // 4. Reformat Code
        assertEquals("code.reformat", items.get(3).getId());
        assertEquals("Reformat Code", items.get(3).getText());
        assertEquals("ALIGN_LEFT", items.get(3).getIconName());

        // 5. XDebugger.Code.Toolbar
        MenuItemConfig xdbg = items.get(4);
        assertEquals("xdebugger.code.toolbar", xdbg.getId());
        assertEquals("XDebugger.Code.Toolbar", xdbg.getText());
        assertFalse(xdbg.isPopup());
        List<MenuItemConfig> xdbgChildren = xdbg.getChildren();
        assertEquals(3, xdbgChildren.size());
        assertEquals(MenuItemConfig.Type.SEPARATOR, xdbgChildren.get(0).getType());
        assertEquals("debug.evaluate.expression", xdbgChildren.get(1).getId());
        assertEquals("CALCULATOR", xdbgChildren.get(1).getIconName());
        assertEquals("debug.add.to.watches", xdbgChildren.get(2).getId());
        assertEquals("Add to Watches", xdbgChildren.get(2).getText());
        assertEquals("PLUS", xdbgChildren.get(2).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "Floating Code Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(11, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleFct = MenuItemConfig.group("root.floating.code.toolbar", "Floating Code Toolbar", List.of(
                MenuItemConfig.group("code.extract.group", "Extract", List.of())
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.floating.code.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleFct);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.floating.code.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig migExt = migrated.getChildren().stream()
                .filter(c -> "code.extract.group".equalsIgnoreCase(c.getId()))
                .findFirst().orElseThrow();
        assertEquals(4, migExt.getChildren().size(), "Stale floating code toolbar must be migrated with extract group");
    }

    @Test
    @DisplayName("Verify Markdown Editor Floating Toolbar hierarchy, formatting icons, catalog, and auto-migration matching DataGrip Image 2")
    public void testMarkdownEditorFloatingToolbarStructureAndOrderMatchingDataGrip() {
        MenuItemConfig md = settings.getMenuConfig("root.markdown.editor.floating.toolbar");
        assertNotNull(md);
        assertEquals("Markdown Editor Floating Toolbar", md.getText());

        List<MenuItemConfig> items = md.getChildren();
        assertEquals(9, items.size());

        assertEquals("md.set.header.style", items.get(0).getId());
        assertEquals("Set Header Style", items.get(0).getText());

        assertEquals(MenuItemConfig.Type.SEPARATOR, items.get(1).getType());

        assertEquals("md.bold", items.get(2).getId());
        assertEquals("Bold", items.get(2).getText());
        assertEquals("BOLD", items.get(2).getIconName());

        assertEquals("md.italic", items.get(3).getId());
        assertEquals("Italic", items.get(3).getText());
        assertEquals("ITALIC", items.get(3).getIconName());

        assertEquals("md.strikethrough", items.get(4).getId());
        assertEquals("Strikethrough", items.get(4).getText());
        assertEquals("STRIKETHROUGH", items.get(4).getIconName());

        assertEquals("md.code", items.get(5).getId());
        assertEquals("CODE", items.get(5).getIconName());

        assertEquals("md.create.link", items.get(6).getId());
        assertEquals("Create Link", items.get(6).getText());
        assertEquals("LINK", items.get(6).getIconName());

        assertEquals(MenuItemConfig.Type.SEPARATOR, items.get(7).getType());

        assertEquals("md.create.or.change.list", items.get(8).getId());
        assertEquals("Create Or Change List", items.get(8).getText());
        assertEquals("LIST", items.get(8).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "Markdown Editor Floating Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(7, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleMd = MenuItemConfig.group("root.markdown.editor.floating.toolbar", "Markdown Editor Floating Toolbar", List.of(
                MenuItemConfig.action("md.bold", "Bold")
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.markdown.editor.floating.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleMd);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.markdown.editor.floating.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        assertEquals(9, migrated.getChildren().size(), "Stale markdown toolbar must be migrated");
    }

    @Test
    @DisplayName("Verify Quick Actions Popup Toolbar hierarchy, icons, catalog, and auto-migration matching DataGrip Image 3")
    public void testQuickActionsPopupToolbarStructureAndOrderMatchingDataGrip() {
        MenuItemConfig qa = settings.getMenuConfig("root.quick.actions.popup.toolbar");
        assertNotNull(qa);
        assertEquals("Quick Actions Popup Toolbar", qa.getText());

        List<MenuItemConfig> items = qa.getChildren();
        assertEquals(4, items.size());

        assertEquals("quick.actions.load.full.cell", items.get(0).getId());
        assertEquals("Load Full Cell", items.get(0).getText());
        assertEquals("ARROWS_ALT_V", items.get(0).getIconName());

        assertEquals("quick.actions.related.rows", items.get(1).getId());
        assertEquals("Related Rows", items.get(1).getText());
        assertEquals("TABLE", items.get(1).getIconName());

        assertEquals("quick.actions.open.url", items.get(2).getId());
        assertEquals("Open URL", items.get(2).getText());
        assertEquals("GLOBE", items.get(2).getIconName());

        assertEquals("quick.actions.open.file.uri", items.get(3).getId());
        assertEquals("Open File URI", items.get(3).getText());
        assertEquals("FOLDER", items.get(3).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "Quick Actions Popup Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(4, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleQa = MenuItemConfig.group("root.quick.actions.popup.toolbar", "Quick Actions Popup Toolbar", List.of(
                MenuItemConfig.action("quick.actions.open.url", "Open URL")
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.quick.actions.popup.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleQa);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.quick.actions.popup.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig migUrl = migrated.getChildren().stream()
                .filter(c -> "quick.actions.open.url".equalsIgnoreCase(c.getId()))
                .findFirst().orElseThrow();
        assertEquals("GLOBE", migUrl.getIconName(), "Stale quick actions toolbar must be migrated with icons");
    }

    @Test
    @DisplayName("Verify Run Tool Window Header Toolbar and More Popup hierarchy, icons, catalog, and auto-migration matching DataGrip Image 4")
    public void testRunToolWindowHeaderToolbarStructureAndOrderMatchingDataGrip() {
        MenuItemConfig runMore = settings.getMenuConfig("root.run.toolwindow.header.more.popup");
        assertNotNull(runMore);
        assertEquals("Run Tool Window Header More Popup", runMore.getText());
        assertTrue(runMore.getChildren().isEmpty(), "Run Tool Window Header More Popup should be empty in DataGrip");

        MenuItemConfig runTb = settings.getMenuConfig("root.run.toolwindow.header.toolbar");
        assertNotNull(runTb);
        assertEquals("Run Tool Window Header Toolbar", runTb.getText());

        List<MenuItemConfig> items = runTb.getChildren();
        assertEquals(4, items.size());

        assertEquals("middle.run", items.get(0).getId());
        assertEquals("Run", items.get(0).getText());
        assertEquals("PLAY", items.get(0).getIconName());

        assertEquals("debug.start", items.get(1).getId());
        assertEquals("Debug", items.get(1).getText());
        assertEquals("BUG", items.get(1).getIconName());

        assertEquals("run.header.rerun", items.get(2).getId());
        assertEquals("Rerun", items.get(2).getText());

        assertEquals("debug.stop", items.get(3).getId());
        assertEquals("Stop", items.get(3).getText());
        assertEquals("STOP", items.get(3).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "Run Tool Window Header Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(4, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleRunTb = MenuItemConfig.group("root.run.toolwindow.header.toolbar", "Run Tool Window Header Toolbar", List.of(
                MenuItemConfig.action("middle.run", "Run")
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.run.toolwindow.header.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleRunTb);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.run.toolwindow.header.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig migStop = migrated.getChildren().stream()
                .filter(c -> "debug.stop".equalsIgnoreCase(c.getId()))
                .findFirst().orElseThrow();
        assertEquals("STOP", migStop.getIconName(), "Stale run toolbar must be migrated with icons");
    }

    @Test
    @DisplayName("Verify SQL Floating Toolbar hierarchy, nested Floating Code Toolbar, Execute & Explain Plan, catalog, and auto-migration matching DataGrip Image 5")
    public void testSqlFloatingToolbarStructureAndOrderMatchingDataGrip() {
        MenuItemConfig sqlTb = settings.getMenuConfig("root.sql.floating.toolbar");
        assertNotNull(sqlTb);
        assertEquals("SQL Floating Toolbar", sqlTb.getText());

        assertEquals(1, sqlTb.getChildren().size());
        MenuItemConfig fctGroup = sqlTb.getChildren().get(0);
        assertEquals("sql.floating.code.toolbar", fctGroup.getId());
        assertEquals("Floating Code Toolbar", fctGroup.getText());
        assertFalse(fctGroup.isPopup());

        List<MenuItemConfig> fctChildren = fctGroup.getChildren();
        assertEquals(7, fctChildren.size());

        // 1. Extract group
        assertEquals("code.extract.group", fctChildren.get(0).getId());
        assertEquals("Extract", fctChildren.get(0).getText());
        assertEquals(4, fctChildren.get(0).getChildren().size());

        // 2. Surround group
        assertEquals("code.surround.group", fctChildren.get(1).getId());
        assertEquals("Surround", fctChildren.get(1).getText());
        assertEquals(3, fctChildren.get(1).getChildren().size());

        // 3. Comment with Line Comment
        assertEquals("code.comment.line", fctChildren.get(2).getId());

        // 4. Reformat Code
        assertEquals("code.reformat", fctChildren.get(3).getId());
        assertEquals("ALIGN_LEFT", fctChildren.get(3).getIconName());

        // 5. XDebugger.Code.Toolbar
        assertEquals("xdebugger.code.toolbar", fctChildren.get(4).getId());
        assertEquals(3, fctChildren.get(4).getChildren().size());

        // 6. Execute
        assertEquals("middle.run", fctChildren.get(5).getId());
        assertEquals("Execute", fctChildren.get(5).getText());
        assertEquals("PLAY", fctChildren.get(5).getIconName());

        // 7. Explain Plan
        assertEquals("editor.explain.plan", fctChildren.get(6).getId());
        assertEquals("Explain Plan", fctChildren.get(6).getText());
        assertEquals("PROJECT_DIAGRAM", fctChildren.get(6).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "SQL Floating Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(13, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleSqlTb = MenuItemConfig.group("root.sql.floating.toolbar", "SQL Floating Toolbar", List.of(
                MenuItemConfig.group("sql.floating.code.toolbar", "Floating Code Toolbar", List.of())
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.sql.floating.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleSqlTb);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.sql.floating.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig migFct = migrated.getChildren().get(0);
        assertEquals(7, migFct.getChildren().size(), "Stale SQL floating toolbar must be migrated with full children");
    }

    @Test
    @DisplayName("Verify VCS Local Changes Toolbar detailed hierarchy, icons, catalog, and auto-migration")
    public void testVcsLocalChangesToolbarHierarchyAndIcons() {
        MenuItemConfig localRoot = settings.getMenuConfig("root.vcs.local.changes.toolbar");
        assertNotNull(localRoot);
        assertEquals("VCS Local Changes Toolbar", localRoot.getText());
        assertEquals(7, localRoot.getChildren().size());

        assertEquals("vcs.local.refresh", localRoot.getChildren().get(0).getId());
        assertEquals("Refresh", localRoot.getChildren().get(0).getText());
        assertEquals("SYNC", localRoot.getChildren().get(0).getIconName());

        assertEquals("vcs.local.commit", localRoot.getChildren().get(1).getId());
        assertEquals("Commit\u2026", localRoot.getChildren().get(1).getText());
        assertEquals("CODE_BRANCH", localRoot.getChildren().get(1).getIconName());

        assertEquals("vcs.local.toggle.commit.ui", localRoot.getChildren().get(2).getId());
        assertEquals("Toggle Commit UI\u2026", localRoot.getChildren().get(2).getText());
        assertEquals("CODE_BRANCH", localRoot.getChildren().get(2).getIconName());

        assertEquals("vcs.local.rollback", localRoot.getChildren().get(3).getId());
        assertEquals("Rollback\u2026", localRoot.getChildren().get(3).getText());
        assertEquals("UNDO", localRoot.getChildren().get(3).getIconName());

        assertEquals("vcs.local.show.diff", localRoot.getChildren().get(4).getId());
        assertEquals("Show Diff", localRoot.getChildren().get(4).getText());
        assertEquals("ARROW_RIGHT", localRoot.getChildren().get(4).getIconName());

        MenuItemConfig changelists = localRoot.getChildren().get(5);
        assertEquals("vcs.local.changelists", changelists.getId());
        assertEquals("Changelists", changelists.getText());
        assertEquals(MenuItemConfig.Type.GROUP, changelists.getType());
        assertEquals(5, changelists.getChildren().size());

        assertEquals("vcs.local.changelists.new", changelists.getChildren().get(0).getId());
        assertEquals("New Changelist\u2026", changelists.getChildren().get(0).getText());
        assertEquals("PLUS", changelists.getChildren().get(0).getIconName());

        assertEquals("vcs.local.changelists.edit", changelists.getChildren().get(1).getId());
        assertEquals("Edit Changelist\u2026", changelists.getChildren().get(1).getText());
        assertEquals("EDIT", changelists.getChildren().get(1).getIconName());

        assertEquals("vcs.local.changelists.delete", changelists.getChildren().get(2).getId());
        assertEquals("Delete Changelist", changelists.getChildren().get(2).getText());
        assertEquals("MINUS", changelists.getChildren().get(2).getIconName());

        assertEquals("vcs.local.changelists.set.active", changelists.getChildren().get(3).getId());
        assertEquals("Set Active Changelist", changelists.getChildren().get(3).getText());
        assertEquals("CHECK_SQUARE", changelists.getChildren().get(3).getIconName());

        assertEquals("vcs.local.changelists.move", changelists.getChildren().get(4).getId());
        assertEquals("Move to Another Changelist\u2026", changelists.getChildren().get(4).getText());
        assertNull(changelists.getChildren().get(4).getIconName());

        assertEquals("vcs.local.shelve.silently", localRoot.getChildren().get(6).getId());
        assertEquals("Shelve Silently", localRoot.getChildren().get(6).getText());
        assertEquals("DOWNLOAD", localRoot.getChildren().get(6).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "VCS Local Changes Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(11, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleLocal = MenuItemConfig.group("root.vcs.local.changes.toolbar", "VCS Local Changes Toolbar", List.of(
                MenuItemConfig.action("vcs.local.refresh", "Refresh")
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.vcs.local.changes.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleLocal);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.vcs.local.changes.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        assertEquals(7, migrated.getChildren().size(), "Stale VCS local changes toolbar must be migrated with full children");
    }

    @Test
    @DisplayName("Verify VCS Log Changes Browser Toolbar detailed hierarchy, icons, catalog, and auto-migration")
    public void testVcsLogChangesBrowserToolbarHierarchyAndIcons() {
        MenuItemConfig logChangesRoot = settings.getMenuConfig("root.vcs.log.changes.browser.toolbar");
        assertNotNull(logChangesRoot);
        assertEquals("VCS Log Changes Browser Toolbar", logChangesRoot.getText());
        assertEquals(3, logChangesRoot.getChildren().size());

        MenuItemConfig repoChanges = logChangesRoot.getChildren().get(0);
        assertEquals("vcs.repository.changes.browser.toolbar", repoChanges.getId());
        assertEquals("Vcs.RepositoryChangesBrowserToolbar", repoChanges.getText());
        assertEquals(2, repoChanges.getChildren().size());
        assertEquals("vcs.repository.changes.browser.revert", repoChanges.getChildren().get(0).getId());
        assertEquals("Revert Selected Changes", repoChanges.getChildren().get(0).getText());
        assertEquals("UNDO", repoChanges.getChildren().get(0).getIconName());
        assertEquals("vcs.repository.changes.browser.history.up.to.here", repoChanges.getChildren().get(1).getId());
        assertEquals("History Up to Here", repoChanges.getChildren().get(1).getText());
        assertEquals("HISTORY", repoChanges.getChildren().get(1).getIconName());

        assertEquals("vcs.log.show.affected.changes", logChangesRoot.getChildren().get(1).getId());
        assertEquals("Show Only Affected Changes", logChangesRoot.getChildren().get(1).getText());
        assertEquals("FOLDER", logChangesRoot.getChildren().get(1).getIconName());

        MenuItemConfig viewOptions = logChangesRoot.getChildren().get(2);
        assertEquals("vcs.log.changes.view.options", viewOptions.getId());
        assertEquals("View Options", viewOptions.getText());
        assertEquals(3, viewOptions.getChildren().size());

        MenuItemConfig groupBy = viewOptions.getChildren().get(0);
        assertEquals("vcs.log.changes.group.by", groupBy.getId());
        assertEquals("Group By", groupBy.getText());
        assertEquals(3, groupBy.getChildren().size());
        assertEquals(MenuItemConfig.Type.SEPARATOR, groupBy.getChildren().get(0).getType());
        assertEquals("vcs.log.changes.group.by.directory", groupBy.getChildren().get(1).getId());
        assertEquals("Directory", groupBy.getChildren().get(1).getText());
        assertEquals("vcs.log.changes.group.by.repository", groupBy.getChildren().get(2).getId());
        assertEquals("Repository", groupBy.getChildren().get(2).getText());

        MenuItemConfig filterBy = viewOptions.getChildren().get(1);
        assertEquals("vcs.log.changes.filter.by", filterBy.getId());
        assertEquals("Filter By", filterBy.getText());
        assertEquals("FILTER", filterBy.getIconName());

        MenuItemConfig configLayout = viewOptions.getChildren().get(2);
        assertEquals("vcs.log.changes.configure.layout", configLayout.getId());
        assertEquals("Configure Layout", configLayout.getText());
        assertEquals(4, configLayout.getChildren().size());
        assertEquals(MenuItemConfig.Type.SEPARATOR, configLayout.getChildren().get(0).getType());
        assertEquals("vcs.log.changes.show.details", configLayout.getChildren().get(1).getId());
        assertEquals("Show Details", configLayout.getChildren().get(1).getText());
        assertEquals("vcs.log.changes.show.diff.preview", configLayout.getChildren().get(2).getId());
        assertEquals("Show Diff Preview", configLayout.getChildren().get(2).getText());

        MenuItemConfig diffPreviewLoc = configLayout.getChildren().get(3);
        assertEquals("vcs.log.changes.diff.preview.location", diffPreviewLoc.getId());
        assertEquals("Diff Preview Location", diffPreviewLoc.getText());
        assertEquals(2, diffPreviewLoc.getChildren().size());
        assertEquals("vcs.log.changes.diff.preview.bottom", diffPreviewLoc.getChildren().get(0).getId());
        assertEquals("Bottom", diffPreviewLoc.getChildren().get(0).getText());
        assertEquals("vcs.log.changes.diff.preview.right", diffPreviewLoc.getChildren().get(1).getId());
        assertEquals("Right", diffPreviewLoc.getChildren().get(1).getText());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "VCS Log Changes Browser Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(10, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleBrowser = MenuItemConfig.group("root.vcs.log.changes.browser.toolbar", "VCS Log Changes Browser Toolbar", List.of(
                MenuItemConfig.group("vcs.repository.changes.browser.toolbar", "Vcs.RepositoryChangesBrowserToolbar", List.of())
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.vcs.log.changes.browser.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleBrowser);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.vcs.log.changes.browser.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        assertEquals(3, migrated.getChildren().size(), "Stale VCS log changes browser toolbar must be migrated with full children");
    }

    @Test
    @DisplayName("Verify VCS Log Toolbar detailed hierarchy, icons, catalog, and auto-migration")
    public void testVcsLogToolbarHierarchyAndIcons() {
        MenuItemConfig logTbRoot = settings.getMenuConfig("root.vcs.log.toolbar");
        assertNotNull(logTbRoot);
        assertEquals("VCS Log Toolbar", logTbRoot.getText());
        assertEquals(5, logTbRoot.getChildren().size());

        assertEquals("vcs.log.resume.indexing", logTbRoot.getChildren().get(0).getId());
        assertEquals("Resume Indexing", logTbRoot.getChildren().get(0).getText());
        assertEquals("PLAY", logTbRoot.getChildren().get(0).getIconName());

        assertEquals("vcs.log.refresh", logTbRoot.getChildren().get(1).getId());
        assertEquals("Refresh", logTbRoot.getChildren().get(1).getText());
        assertEquals("SYNC", logTbRoot.getChildren().get(1).getIconName());

        MenuItemConfig vcsLogTbGroup = logTbRoot.getChildren().get(2);
        assertEquals("vcs.log.toolbar.group", vcsLogTbGroup.getId());
        assertEquals("Vcs.Log.Toolbar", vcsLogTbGroup.getText());
        assertEquals(1, vcsLogTbGroup.getChildren().size());

        MenuItemConfig dvcsTb = vcsLogTbGroup.getChildren().get(0);
        assertEquals("vcs.log.dvcs.toolbar", dvcsTb.getId());
        assertEquals("Dvcs.Log.Toolbar", dvcsTb.getText());
        assertEquals(1, dvcsTb.getChildren().size());
        assertEquals("vcs.log.cherry.pick", dvcsTb.getChildren().get(0).getId());
        assertEquals("Cherry-Pick", dvcsTb.getChildren().get(0).getText());
        assertEquals("CODE_BRANCH", dvcsTb.getChildren().get(0).getIconName());

        MenuItemConfig viewOptions = logTbRoot.getChildren().get(3);
        assertEquals("vcs.log.view.options", viewOptions.getId());
        assertEquals("View Options", viewOptions.getText());
        assertEquals(11, viewOptions.getChildren().size());
        assertEquals(MenuItemConfig.Type.SEPARATOR, viewOptions.getChildren().get(0).getType());
        assertEquals("vcs.log.view.options.root.names", viewOptions.getChildren().get(1).getId());
        assertEquals("Root Names", viewOptions.getChildren().get(1).getText());
        assertEquals("vcs.log.view.options.compact.references", viewOptions.getChildren().get(2).getId());
        assertEquals("Compact References View", viewOptions.getChildren().get(2).getText());
        assertEquals("vcs.log.view.options.tag.names", viewOptions.getChildren().get(3).getId());
        assertEquals("Tag Names", viewOptions.getChildren().get(3).getText());
        assertEquals("vcs.log.view.options.long.edges", viewOptions.getChildren().get(4).getId());
        assertEquals("Long Edges", viewOptions.getChildren().get(4).getText());
        assertEquals("vcs.log.view.options.commit.timestamp", viewOptions.getChildren().get(5).getId());
        assertEquals("Commit Timestamp", viewOptions.getChildren().get(5).getText());
        assertEquals("vcs.log.view.options.references.on.left", viewOptions.getChildren().get(6).getId());
        assertEquals("References on the Left", viewOptions.getChildren().get(6).getText());
        assertEquals("vcs.log.view.options.columns", viewOptions.getChildren().get(7).getId());
        assertEquals("Columns", viewOptions.getChildren().get(7).getText());
        assertEquals(MenuItemConfig.Type.SEPARATOR, viewOptions.getChildren().get(8).getType());
        assertEquals("vcs.log.highlighters.action.group", viewOptions.getChildren().get(9).getId());
        assertEquals("Vcs.Log.HighlightersActionGroup", viewOptions.getChildren().get(9).getText());

        MenuItemConfig gitLogTb = viewOptions.getChildren().get(10);
        assertEquals("vcs.log.git.log.toolbar", gitLogTb.getId());
        assertEquals("Git.Log.Toolbar", gitLogTb.getText());
        assertEquals(1, gitLogTb.getChildren().size());
        assertEquals("vcs.log.git.not.cherry.picked.commits", gitLogTb.getChildren().get(0).getId());
        assertEquals("Not Cherry-Picked Commits", gitLogTb.getChildren().get(0).getText());

        assertEquals("vcs.log.goto.hash", logTbRoot.getChildren().get(4).getId());
        assertEquals("Go To Hash/Branch/Tag", logTbRoot.getChildren().get(4).getText());
        assertEquals("SEARCH", logTbRoot.getChildren().get(4).getIconName());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "VCS Log Toolbar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(13, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleLogTb = MenuItemConfig.group("root.vcs.log.toolbar", "VCS Log Toolbar", List.of(
                MenuItemConfig.action("vcs.log.refresh", "Refresh")
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.vcs.log.toolbar".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleLogTb);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.vcs.log.toolbar".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        assertEquals(5, migrated.getChildren().size(), "Stale VCS log toolbar must be migrated with full children");
    }

    @Test
    @DisplayName("Verify VCS Operations Popup detailed hierarchy, icons, catalog, and auto-migration")
    public void testVcsOperationsPopupHierarchyAndIcons() {
        MenuItemConfig opsRoot = settings.getMenuConfig("root.vcs.operations.popup");
        assertNotNull(opsRoot);
        assertEquals("VCS Operations Popup", opsRoot.getText());
        assertEquals(7, opsRoot.getChildren().size());

        MenuItemConfig vcsAware = opsRoot.getChildren().get(0);
        assertEquals("vcs.operations.popup.vcsaware", vcsAware.getId());
        assertEquals("Vcs.Operations.Popup.VcsAware", vcsAware.getText());
        assertEquals(10, vcsAware.getChildren().size());
        assertEquals("vcs.operations.popup.vcsnameseparator", vcsAware.getChildren().get(0).getId());
        assertEquals("Vcs.Operations.Popup.VcsNameSeparator", vcsAware.getChildren().get(0).getText());
        assertEquals("vcs.operations.popup.add", vcsAware.getChildren().get(1).getId());
        assertEquals("Add to VCS", vcsAware.getChildren().get(1).getText());
        assertEquals("vcs.operations.popup.commit", vcsAware.getChildren().get(2).getId());
        assertEquals("Commit\u2026", vcsAware.getChildren().get(2).getText());
        assertEquals("CODE_BRANCH", vcsAware.getChildren().get(2).getIconName());
        assertEquals("vcs.operations.popup.toggle.commit.ui", vcsAware.getChildren().get(3).getId());
        assertEquals("Toggle Commit UI\u2026", vcsAware.getChildren().get(3).getText());
        assertEquals("CODE_BRANCH", vcsAware.getChildren().get(3).getIconName());
        assertEquals("vcs.operations.popup.commit.file", vcsAware.getChildren().get(4).getId());
        assertEquals("Commit File", vcsAware.getChildren().get(4).getText());
        assertEquals("vcs.operations.popup.rollback", vcsAware.getChildren().get(5).getId());
        assertEquals("Rollback\u2026", vcsAware.getChildren().get(5).getText());
        assertEquals("UNDO", vcsAware.getChildren().get(5).getIconName());
        assertEquals(MenuItemConfig.Type.SEPARATOR, vcsAware.getChildren().get(6).getType());
        assertEquals("vcs.operations.popup.show.history", vcsAware.getChildren().get(7).getId());
        assertEquals("Show History", vcsAware.getChildren().get(7).getText());
        assertEquals("HISTORY", vcsAware.getChildren().get(7).getIconName());
        assertEquals("vcs.operations.popup.annotate", vcsAware.getChildren().get(8).getId());
        assertEquals("Annotate", vcsAware.getChildren().get(8).getText());
        assertEquals("vcs.operations.popup.compare.same.repo", vcsAware.getChildren().get(9).getId());
        assertEquals("Compare with Same Repository Version", vcsAware.getChildren().get(9).getText());
        assertEquals("ARROW_RIGHT", vcsAware.getChildren().get(9).getIconName());

        assertEquals(MenuItemConfig.Type.SEPARATOR, opsRoot.getChildren().get(1).getType());

        assertEquals("vcs.operations.popup.vcs.providers", opsRoot.getChildren().get(2).getId());
        assertEquals("Vcs.Operations.Popup.Vcs.Providers", opsRoot.getChildren().get(2).getText());

        MenuItemConfig nonVcsAware = opsRoot.getChildren().get(3);
        assertEquals("vcs.operations.popup.nonvcsaware", nonVcsAware.getId());
        assertEquals("Vcs.Operations.Popup.NonVcsAware", nonVcsAware.getText());
        assertEquals(2, nonVcsAware.getChildren().size());
        assertEquals("vcs.operations.popup.create.git.repo", nonVcsAware.getChildren().get(0).getId());
        assertEquals("Create Git Repository\u2026", nonVcsAware.getChildren().get(0).getText());
        assertEquals("vcs.operations.popup.enable.vcs.integration", nonVcsAware.getChildren().get(1).getId());
        assertEquals("Enable Version Control Integration\u2026", nonVcsAware.getChildren().get(1).getText());

        assertEquals(MenuItemConfig.Type.SEPARATOR, opsRoot.getChildren().get(4).getType());

        assertEquals("vcs.operations.annotated.line", opsRoot.getChildren().get(5).getId());
        assertEquals("Annotated Line", opsRoot.getChildren().get(5).getText());

        assertEquals("vcs.operations.show.history", opsRoot.getChildren().get(6).getId());
        assertEquals("Show History\u2026", opsRoot.getChildren().get(6).getText());

        // Action catalog check
        ActionManager actionManager = ActionManager.getInstance();
        var cat = actionManager.getActionCatalog().stream()
                .filter(c -> "VCS Operations Popup".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(14, cat.getEntries().size());

        // Auto-migration test
        MenuItemConfig staleOps = MenuItemConfig.group("root.vcs.operations.popup", "VCS Operations Popup", List.of(
                MenuItemConfig.action("vcs.operations.show.history", "Show History\u2026")
        ));
        List<MenuItemConfig> testList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < testList.size(); i++) {
            if ("root.vcs.operations.popup".equalsIgnoreCase(testList.get(i).getId())) {
                testList.set(i, staleOps);
                break;
            }
        }
        settings.setMenusAndToolbars(testList);
        MenuItemConfig migrated = settings.getMenusAndToolbars().stream()
                .filter(r -> "root.vcs.operations.popup".equalsIgnoreCase(r.getId()))
                .findFirst().orElseThrow();
        assertEquals(7, migrated.getChildren().size(), "Stale VCS operations popup must be migrated with full children");
    }

    @Test
    @DisplayName("Verify Edit menu dynamic hierarchy, action catalog, migration, and state handling matching DataGrip")
    public void testEditMenuDynamicStatesAndParity() {
        // 1. Hierarchy in default settings
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        assertNotNull(mainMenu);
        MenuItemConfig editMenu = mainMenu.getChildren().stream()
                .filter(m -> "Edit".equalsIgnoreCase(m.getText()) || "menu.edit".equalsIgnoreCase(m.getId()))
                .findFirst().orElseThrow();

        // 1a. Cut/Copy/Paste Actions group and Paste > popup
        MenuItemConfig cutCopyPaste = editMenu.getChildren().stream()
                .filter(m -> "edit.cut.copy.paste.actions".equalsIgnoreCase(m.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig pasteGroup = cutCopyPaste.getChildren().stream()
                .filter(m -> "edit.paste.group".equalsIgnoreCase(m.getId()))
                .findFirst().orElseThrow();
        assertTrue(pasteGroup.isPopup(), "Paste group must be configured as a popup submenu (Image 3)");
        List<String> pasteChildrenIds = pasteGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(pasteChildrenIds.contains("edit.paste"));
        assertTrue(pasteChildrenIds.contains("edit.paste.history"));
        assertTrue(pasteChildrenIds.contains("edit.paste.plain"));

        // 1b. Generate... > popup with 31 DDL database objects (Image 4)
        MenuItemConfig generateRoot = editMenu.getChildren().stream()
                .filter(m -> "edit.generate.root.group".equalsIgnoreCase(m.getId()))
                .findFirst().orElseThrow();
        assertTrue(generateRoot.isPopup(), "Generate root group must be a popup submenu (Image 4)");
        List<String> genIds = generateRoot.getChildren().stream().map(MenuItemConfig::getId).toList();
        List<String> expectedDdlIds = List.of(
                "edit.generate.table", "edit.generate.view", "edit.generate.materialized.view",
                "edit.generate.index", "edit.generate.unique.key", "edit.generate.primary.key",
                "edit.generate.foreign.key", "edit.generate.check.constraint", "edit.generate.column",
                "edit.generate.trigger", "edit.generate.procedure", "edit.generate.function",
                "edit.generate.package", "edit.generate.sequence", "edit.generate.synonym",
                "edit.generate.type", "edit.generate.rule", "edit.generate.domain",
                "edit.generate.user.mapping", "edit.generate.server", "edit.generate.fdw",
                "edit.generate.event", "edit.generate.aggregate", "edit.generate.extension",
                "edit.generate.operator", "edit.generate.cast", "edit.generate.collation",
                "edit.generate.conversion", "edit.generate.language", "edit.generate.policy",
                "edit.generate.database.link"
        );
        for (String expectedId : expectedDdlIds) {
            assertTrue(genIds.contains(expectedId), "Generate submenu must contain: " + expectedId);
        }

        // 1c. Refactor > popup with DataGrip actions (Image 3)
        MenuItemConfig refactorMenu = editMenu.getChildren().stream()
                .filter(m -> "edit.refactor".equalsIgnoreCase(m.getId()))
                .findFirst().orElseThrow();
        assertTrue(refactorMenu.isPopup(), "Refactor menu must be a popup submenu (Image 3)");
        List<String> refactorIds = refactorMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(refactorIds.contains("edit.refactor.rename"));
        assertTrue(refactorIds.contains("edit.refactor.expand.column.list"));
        assertTrue(refactorIds.contains("edit.refactor.convert.subquery"));
        assertTrue(refactorIds.contains("edit.refactor.extract.introduce.group"));
        assertTrue(refactorIds.contains("edit.refactor.qualify.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.unqualify.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.quote.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.unquote.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.flip.expression"));
        assertTrue(refactorIds.contains("edit.refactor.inject.language"));
        assertTrue(refactorIds.contains("edit.refactor.uninject.language"));

        // 2. Action Catalog entries check
        ActionManager actionManager = ActionManager.getInstance();
        ActionRegistry.initialize(actionManager);
        var catalog = actionManager.getActionCatalog();
        var mainMenuCat = catalog.stream().filter(c -> "Main Menu".equalsIgnoreCase(c.getName())).findFirst().orElseThrow();
        var editCat = mainMenuCat.getSubCategories().stream().filter(c -> "Edit".equalsIgnoreCase(c.getName())).findFirst().orElseThrow();
        List<String> editEntryIds = editCat.getEntries().stream().map(ActionManager.ActionCatalogEntry::getId).toList();

        for (String expectedId : expectedDdlIds) {
            assertTrue(editEntryIds.contains(expectedId), "ActionCatalog must include DDL generator: " + expectedId);
        }
        assertTrue(editEntryIds.contains("edit.refactor.rename"));
        assertTrue(editEntryIds.contains("edit.refactor.expand.column.list"));
        assertTrue(editEntryIds.contains("edit.refactor.convert.subquery"));
        assertTrue(editEntryIds.contains("edit.refactor.qualify.identifier"));
        assertTrue(editEntryIds.contains("edit.refactor.unqualify.identifier"));
        assertTrue(editEntryIds.contains("edit.refactor.quote.identifier"));
        assertTrue(editEntryIds.contains("edit.refactor.unquote.identifier"));
        assertTrue(editEntryIds.contains("edit.refactor.flip.expression"));
        assertTrue(editEntryIds.contains("edit.refactor.inject.language"));
        assertTrue(editEntryIds.contains("edit.refactor.uninject.language"));

        // 3. Dynamic enabling/disabling states (Image 1 vs Image 2, 3, 4)
        AnAction cut = actionManager.getAction("edit.cut");
        AnAction copy = actionManager.getAction("edit.copy");
        AnAction copyRef = actionManager.getAction("edit.copy.reference");
        AnAction paste = actionManager.getAction("edit.paste");
        AnAction deleteAction = actionManager.getAction("edit.delete");
        AnAction find = actionManager.getAction("edit.find");
        AnAction replace = actionManager.getAction("edit.replace");
        AnAction findInFiles = actionManager.getAction("edit.find.in.files");
        AnAction replaceInFiles = actionManager.getAction("edit.replace.in.files");
        AnAction findUsages = actionManager.getAction("edit.find.usages");
        ActionGroup refactorGroupAction = actionManager.getGroup("edit.refactor");
        ActionGroup generateRootGroupAction = actionManager.getGroup("edit.generate.root.group");
        ActionGroup bookmarksGroupAction = actionManager.getGroup("edit.bookmarks.group");

        assertNotNull(cut);
        assertNotNull(copy);
        assertNotNull(copyRef);
        assertNotNull(paste);
        assertNotNull(deleteAction);
        assertNotNull(find);
        assertNotNull(replace);
        assertNotNull(findInFiles);
        assertNotNull(replaceInFiles);
        assertNotNull(findUsages);
        assertNotNull(refactorGroupAction);
        assertNotNull(generateRootGroupAction);
        assertNotNull(bookmarksGroupAction);

        // Outside console (Image 1, 3, 4):
        // Cut, Copy, Delete disabled; Find, Replace disabled
        // Copy Path/Reference, Paste, Find in Files, Replace in Files, Find Usages, Bookmarks enabled
        assertFalse(cut.isEnabled(null), "Cut must be disabled outside console");
        assertFalse(copy.isEnabled(null), "Copy must be disabled outside console");
        assertFalse(deleteAction.isEnabled(null), "Delete must be disabled outside console");
        assertFalse(find.isEnabled(null), "Find must be disabled outside console");
        assertFalse(replace.isEnabled(null), "Replace must be disabled outside console");

        assertTrue(copyRef.isEnabled(null), "Copy Path/Reference must be enabled outside console (Image 1)");
        assertTrue(paste.isEnabled(null), "Paste must be enabled outside console (Image 1)");
        assertTrue(findInFiles.isEnabled(null), "Find in Files must be enabled outside console (Image 1)");
        assertTrue(replaceInFiles.isEnabled(null), "Replace in Files must be enabled outside console (Image 1)");
        assertTrue(findUsages.isEnabled(null), "Find Usages must be enabled outside console (Image 1)");
        assertTrue(bookmarksGroupAction.isEnabled(null), "Bookmarks must be enabled outside console (Image 1)");

        // Dynamic Generate behavior (Image 1 vs Image 2)
        AnAction tableGen = actionManager.getAction("edit.generate.table");
        assertNotNull(tableGen);
        assertFalse(tableGen.isVisible(null), "DDL Generator must be hidden outside console (Image 1)");
        assertFalse(tableGen.isEnabled(null), "DDL Generator must be disabled outside console (Image 1)");

        AnAction xmlTag = actionManager.getAction("edit.generate.xml.tag");
        assertNotNull(xmlTag);
        assertTrue(xmlTag.isVisible(null), "XML Tag must be visible outside console (Image 1)");
        assertFalse(xmlTag.isEnabled(null), "XML Tag must be disabled outside console (Image 1)");

        // Dynamic Refactor behavior (Image 3)
        AnAction renameAction = actionManager.getAction("edit.refactor.rename");
        assertNotNull(renameAction);
        assertTrue(renameAction.isEnabled(null), "Rename must be enabled outside console (Image 3)");

        AnAction expandCols = actionManager.getAction("edit.refactor.expand.column.list");
        assertNotNull(expandCols);
        assertFalse(expandCols.isEnabled(null), "Expand Column List must be disabled outside console (Image 3)");

        AnAction convertSubquery = actionManager.getAction("edit.refactor.convert.subquery");
        assertNotNull(convertSubquery);
        assertFalse(convertSubquery.isEnabled(null), "Convert to Subquery must be disabled outside console (Image 3)");

        // Dynamic Selection behavior (Image 4)
        AnAction selectAll = actionManager.getAction("edit.select.all");
        assertNotNull(selectAll);
        assertFalse(selectAll.isEnabled(null), "Select All must be disabled outside console (Image 4)");

        AnAction addCarets = actionManager.getAction("edit.selection.add.carets.ends");
        assertNotNull(addCarets);
        assertFalse(addCarets.isEnabled(null), "Add Carets must be disabled outside console (Image 4)");

        AnAction extendSel = actionManager.getAction("edit.selection.extend");
        assertNotNull(extendSel);
        assertFalse(extendSel.isEnabled(null), "Extend Selection must be disabled outside console (Image 4)");

        AnAction shrinkSel = actionManager.getAction("edit.selection.shrink");
        assertNotNull(shrinkSel);
        assertFalse(shrinkSel.isEnabled(null), "Shrink Selection must be disabled outside console (Image 4)");

        // Image 2 Parity: Actions must be VISIBLE but DISABLED when outside console
        List<String> visibleDisabledOutsideConsole = List.of(
                "edit.copy.plain",
                "edit.insert.live.template",
                "edit.surround.with",
                "edit.format.code",
                "edit.format.file",
                "edit.comment.line",
                "edit.comment.block",
                "edit.auto.indent",
                "edit.toggle.case",
                "edit.join.lines",
                "edit.duplicate.lines",
                "edit.sort.lines"
        );
        for (String id : visibleDisabledOutsideConsole) {
            AnAction action = actionManager.getAction(id);
            assertNotNull(action, "Action must exist: " + id);
            assertTrue(action.isVisible(null), "Action must be visible outside console (Image 2): " + id);
            assertFalse(action.isEnabled(null), "Action must be disabled outside console (Image 2): " + id);
        }
        ActionGroup selGroup = actionManager.getGroup("edit.selection");
        assertNotNull(selGroup, "Selection group must exist");
        assertTrue(selGroup.isVisible(null), "Selection group must be visible outside console (Image 4)");

        // 4. Auto-migration test for stale settings without popup paste or without DDL objects
        MenuItemConfig staleEdit = MenuItemConfig.group("menu.edit", "Edit", true, List.of(
                MenuItemConfig.action("edit.undo", "Undo"),
                MenuItemConfig.group("edit.cut.copy.paste.actions", "Cut/Copy/Paste Actions", false, List.of(
                        MenuItemConfig.action("edit.paste", "Paste") // non-popup paste!
                ))
        ));
        List<MenuItemConfig> modifiedList = new ArrayList<>(settings.getMenusAndToolbars());
        for (int i = 0; i < modifiedList.size(); i++) {
            if ("root.main.menu".equalsIgnoreCase(modifiedList.get(i).getId())) {
                MenuItemConfig mainCopy = modifiedList.get(i).copy();
                for (int j = 0; j < mainCopy.getChildren().size(); j++) {
                    if ("menu.edit".equalsIgnoreCase(mainCopy.getChildren().get(j).getId())
                            || "Edit".equalsIgnoreCase(mainCopy.getChildren().get(j).getText())) {
                        mainCopy.getChildren().set(j, staleEdit);
                        break;
                    }
                }
                modifiedList.set(i, mainCopy);
                break;
            }
        }
        settings.setMenusAndToolbars(modifiedList);

        MenuItemConfig reloadedMainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig reloadedEdit = reloadedMainMenu.getChildren().stream()
                .filter(m -> "menu.edit".equalsIgnoreCase(m.getId()) || "Edit".equalsIgnoreCase(m.getText()))
                .findFirst().orElseThrow();
        MenuItemConfig reloadedCutCopyPaste = reloadedEdit.getChildren().stream()
                .filter(m -> "edit.cut.copy.paste.actions".equalsIgnoreCase(m.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig reloadedPasteGroup = reloadedCutCopyPaste.getChildren().stream()
                .filter(m -> "edit.paste.group".equalsIgnoreCase(m.getId()))
                .findFirst().orElseThrow();
        assertTrue(reloadedPasteGroup.isPopup(), "Stale Edit menu must be migrated so paste group is popup: true");

        MenuItemConfig reloadedGenerateRoot = reloadedEdit.getChildren().stream()
                .filter(m -> "edit.generate.root.group".equalsIgnoreCase(m.getId()))
                .findFirst().orElseThrow();
        assertTrue(reloadedGenerateRoot.getChildren().size() >= 31, "Migrated generate group must have all 31 DDL objects");
    }
}



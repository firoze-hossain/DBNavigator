package com.roze.dbnavigator.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.MenuItemConfig;
import com.roze.dbnavigator.ui.action.ActionManager;
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
        assertTrue(topMenus.contains("Code"));
        assertTrue(topMenus.contains("Refactor"));
        assertTrue(topMenus.contains("Run"));
        assertTrue(topMenus.contains("Tools"));
        assertTrue(topMenus.contains("Git"));
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
        assertTrue(newGroupItems.contains("New File"));
        assertTrue(newGroupItems.contains("New"));

        MenuItemConfig newFileGroup = newGroup.getChildren().stream()
                .filter(m -> "New File".equals(m.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(newFileGroup);
        List<String> newFileItems = newFileGroup.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();
        assertTrue(newFileItems.contains("SQL File"));
        assertTrue(newFileItems.contains("File"));
        assertTrue(newFileItems.contains("Scratch File"));
        assertTrue(newFileItems.contains("Directory/Package"));
        assertTrue(newFileItems.contains("Web Development Templates"));
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
        assertTrue(centerActions.contains("Single Tool Window Bar"));
        assertTrue(centerActions.stream().anyMatch(t -> t.contains("File Name Widget")));

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
        assertTrue(sqlItems.contains("Execute"));
        assertTrue(sqlItems.contains("Explain Plan"));
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

        // Check top-level items from Images 1-4 are present
        assertTrue(actionIds.contains("file.open.actions"), "Should contain File Open Actions");
        assertTrue(actionIds.contains("file.new"), "Should contain New group");
        assertTrue(actionIds.contains("file.open.sql"), "Should contain Open...");
        assertTrue(actionIds.contains("file.save.as"), "Should contain Save As...");
        assertTrue(actionIds.contains("file.recent"), "Should contain Recent Projects");
        assertTrue(actionIds.contains("file.remote.dev.actions"), "Should contain Remote Development actions");
        assertTrue(actionIds.contains("file.settings.actions"), "Should contain Settings actions");
        assertTrue(actionIds.contains("file.sql.dialects"), "Should contain SQL Dialects...");
        assertTrue(actionIds.contains("file.sql.scopes"), "Should contain SQL Resolution Scopes...");
        assertTrue(actionIds.contains("file.edit.datasources.xml"), "Should contain Edit dataSources.xml");
        assertTrue(actionIds.contains("file.properties"), "Should contain File Properties");
        assertTrue(actionIds.contains("file.local.history.main.group"), "Should contain Local History main group");
        assertTrue(actionIds.contains("file.save.all"), "Should contain Save All");
        assertTrue(actionIds.contains("file.reload.all"), "Should contain Reload All from Disk");
        assertTrue(actionIds.contains("file.manage.settings"), "Should contain Manage IDE Settings");
        assertTrue(actionIds.contains("file.print.export.actions"), "Should contain Print/Export actions");
        assertTrue(actionIds.contains("file.power.save"), "Should contain Power Save Mode");
        assertTrue(actionIds.contains("file.exit"), "Should contain Exit");

        // Verify Recent Projects children (Image 2)
        MenuItemConfig recentGroup = fileMenu.getChildren().stream()
                .filter(c -> "file.recent".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> recentIds = recentGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(recentIds.contains("file.reopen.project"));
        assertTrue(recentIds.contains("file.manage.projects"));
        assertTrue(recentIds.contains("file.close.project"));
        assertTrue(recentIds.contains("file.rename.project"));
        assertTrue(recentIds.contains("file.attach.directory"));

        // Verify Settings Actions children (Image 2 & 4)
        MenuItemConfig settingsGroup = fileMenu.getChildren().stream()
                .filter(c -> "file.settings.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(settingsGroup.isPopup(), "Settings Actions should be non-popup group");
        List<String> settingsIds = settingsGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(settingsIds.contains("file.settings"));
        assertTrue(settingsIds.contains("file.project.structure"));
    }

    @Test
    @DisplayName("Verify Cloud Data Source submenu has AWS, GCP, and Azure")
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

        MenuItemConfig newDbGroup = newGroup.getChildren().stream()
                .filter(m -> "file.new.db.group".equals(m.getId()))
                .findFirst()
                .orElseThrow();

        MenuItemConfig createDsGroup = newDbGroup.getChildren().stream()
                .filter(m -> "file.new.create.datasource".equals(m.getId()))
                .findFirst()
                .orElseThrow();

        MenuItemConfig cloudGroup = createDsGroup.getChildren().stream()
                .filter(m -> "file.new.datasource.cloud".equals(m.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(3, cloudGroup.getChildren().size());
        List<String> cloudNames = cloudGroup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(cloudNames.contains("Amazon AWS"));
        assertTrue(cloudNames.contains("Google Cloud"));
        assertTrue(cloudNames.contains("Microsoft Azure"));
    }

    @Test
    @DisplayName("Verify automatic migration of old truncated File menu in settings")
    public void testAutoMigrationOfOutdatedMainMenu() {
        AppSettingsStore.Settings staleSettings = new AppSettingsStore.Settings();
        // Create an outdated File menu without file.open.actions
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
        assertTrue(ids.contains("file.open.actions"), "Migrated File menu must now include file.open.actions");
        assertTrue(ids.contains("file.save.as"), "Migrated File menu must now include file.save.as");
        assertTrue(ids.contains("file.recent"), "Migrated File menu must now include file.recent");
        assertTrue(ids.contains("file.settings.actions"), "Migrated File menu must now include file.settings.actions");
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
        MenuItemConfig genInner = genRoot.getChildren().stream()
                .filter(c -> "edit.generate.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> genInnerIds = genInner.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(genInnerIds.contains("edit.generate.sql.group"));
        assertTrue(genInnerIds.contains("edit.generate.xml.tag"));
        assertTrue(genInnerIds.contains("edit.generate.override.methods"));
        assertTrue(genInnerIds.contains("edit.generate.implement.methods"));
        assertTrue(genInnerIds.contains("edit.generate.delegate.methods"));
        assertTrue(genInnerIds.contains("edit.generate.test.creators.group"));
        assertTrue(genInnerIds.contains("edit.generate.markdown.group"));

        MenuItemConfig mdGroup = genInner.getChildren().stream()
                .filter(c -> "edit.generate.markdown.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> mdIds = mdGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(mdIds.contains("edit.generate.markdown.link"));
        assertTrue(mdIds.contains("edit.generate.markdown.table"));
        assertTrue(mdIds.contains("edit.generate.markdown.image"));
        assertTrue(mdIds.contains("edit.generate.markdown.toc"));

        // Verify Refactor children (Image 3)
        MenuItemConfig refactorGroup = editMenu.getChildren().stream()
                .filter(c -> "edit.refactor".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(refactorGroup.isPopup(), "Refactor should be a popup submenu");
        List<String> refactorIds = refactorGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(refactorIds.contains("edit.refactor.rename"));
        assertTrue(refactorIds.contains("edit.refactor.expand.column.list"));
        assertTrue(refactorIds.contains("edit.refactor.convert.subquery"));
        assertTrue(refactorIds.contains("edit.refactor.subquery.cte"));
        assertTrue(refactorIds.contains("edit.refactor.extract.introduce.group"));
        assertTrue(refactorIds.contains("edit.refactor.qualify.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.unqualify.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.quote.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.unquote.identifier"));
        assertTrue(refactorIds.contains("edit.refactor.flip.expression"));
        assertTrue(refactorIds.contains("edit.refactor.inject.language"));
        assertTrue(refactorIds.contains("edit.refactor.uninject.language"));

        // Verify Selection children (Image 2)
        MenuItemConfig selectionGroup = editMenu.getChildren().stream()
                .filter(c -> "edit.selection".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(selectionGroup.isPopup(), "Selection should be a popup submenu");
        List<String> selIds = selectionGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(selIds.contains("edit.selection.column.mode"));
        assertTrue(selIds.contains("edit.selection.editor.select.actions"));
        assertTrue(selIds.contains("edit.toggle.case"));
        assertTrue(selIds.contains("edit.join.lines"));
        assertTrue(selIds.contains("edit.duplicate.lines"));
        assertTrue(selIds.contains("edit.sort.lines"));

        MenuItemConfig editorSelect = selectionGroup.getChildren().stream()
                .filter(c -> "edit.selection.editor.select.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> esIds = editorSelect.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(esIds.contains("edit.select.all"));
        assertTrue(esIds.contains("edit.selection.add.carets.ends"));
        assertTrue(esIds.contains("edit.selection.word.actions"));
    }

    @Test
    @DisplayName("Verify View Menu completeness matching DataGrip Images 4, 5")
    public void testViewMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig viewMenu = mainMenu.getChildren().stream()
                .filter(m -> "View".equals(m.getText()) || "menu.view".equals(m.getId()))
                .findFirst()
                .orElseThrow();

        List<String> viewIds = viewMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(viewIds.contains("view.tool.windows"));
        assertTrue(viewIds.contains("view.appearance"));
        assertTrue(viewIds.contains("view.recent.actions.group"));
        assertTrue(viewIds.contains("view.font.increase"));
        assertTrue(viewIds.contains("view.font.decrease"));
        assertTrue(viewIds.contains("view.font.reset"));

        // Verify Tool Windows (Image 4)
        MenuItemConfig twGroup = viewMenu.getChildren().stream()
                .filter(c -> "view.tool.windows".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(twGroup.isPopup(), "Tool Windows should be popup submenu");
        List<String> twIds = twGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(twIds.contains("middle.database"));
        assertTrue(twIds.contains("view.tool.files"));
        assertTrue(twIds.contains("view.tool.terminal"));
        assertTrue(twIds.contains("file.new.console"));

        // Verify Appearance (Image 4 & 5)
        MenuItemConfig appGroup = viewMenu.getChildren().stream()
                .filter(c -> "view.appearance".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(appGroup.isPopup(), "Appearance should be popup submenu");
        List<String> appIds = appGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(appIds.contains("view.toggle.fullscreen.group"));
        assertTrue(appIds.contains("view.zoom.ide"));
        assertTrue(appIds.contains("view.ui.toggle.actions"));

        MenuItemConfig fsGroup = appGroup.getChildren().stream()
                .filter(c -> "view.toggle.fullscreen.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(fsGroup.isPopup(), "ToggleFullScreenGroup should be non-popup");
        List<String> fsIds = fsGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(fsIds.contains("view.toggle.presentation.mode"));
        assertTrue(fsIds.contains("view.toggle.distraction.free.mode"));
        assertTrue(fsIds.contains("view.toggle.fullscreen.mode"));
        assertTrue(fsIds.contains("view.toggle.zen.mode"));
        assertTrue(fsIds.contains("view.compact.mode"));

        MenuItemConfig uiToggleGroup = appGroup.getChildren().stream()
                .filter(c -> "view.ui.toggle.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(uiToggleGroup.isPopup(), "UIToggleActions should be non-popup");
        List<String> uiIds = uiToggleGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(uiIds.contains("view.toggle.presentation.assistant"));
        assertTrue(uiIds.contains("view.toggle.main.menu"));
        assertTrue(uiIds.contains("view.toggle.main.menu.separate"));
        assertTrue(uiIds.contains("view.toggle.toolbar"));
        assertTrue(uiIds.contains("view.toggle.toolbar.classic"));
        assertTrue(uiIds.contains("view.toggle.navigation.bar"));
        assertTrue(uiIds.contains("view.toolbar.actions.group"));

        MenuItemConfig tbGroup = uiToggleGroup.getChildren().stream()
                .filter(c -> "view.toolbar.actions.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> tbIds = tbGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(tbIds.contains("view.toolbar.actions.toolbar"));
        assertTrue(tbIds.contains("view.toolbar.actions.navbar"));
        assertTrue(tbIds.contains("view.toolbar.actions.navbar.group"));
        assertTrue(tbIds.contains("view.toggle.tool.window.bars"));
        assertTrue(tbIds.contains("view.toggle.status.bar"));
        assertTrue(tbIds.contains("view.status.bar.widgets"));
        assertTrue(tbIds.contains("view.toggle.members.in.nav.bar"));

        // Verify View Recent Actions Group (Image 5)
        MenuItemConfig recentGroup = viewMenu.getChildren().stream()
                .filter(c -> "view.recent.actions.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(recentGroup.isPopup(), "View Recent Actions Group should be non-popup");
        List<String> recentIds = recentGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(recentIds.contains("view.recent.files"));
        assertTrue(recentIds.contains("view.recent.toggle.changed.only"));
        assertTrue(recentIds.contains("view.recent.iterate.files"));
        assertTrue(recentIds.contains("view.recently.changed.files"));
        assertTrue(recentIds.contains("view.recent.locations"));
        assertTrue(recentIds.contains("view.recent.changes"));
    }

    @Test
    @DisplayName("Verify Navigate Menu completeness matching DataGrip Images 1-3")
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
        assertTrue(navIds.contains("nav.goto.by.name.group"), "Should contain Goto by Name Actions group");
        assertTrue(navIds.contains("nav.goto.row"), "Should contain Row...");
        assertTrue(navIds.contains("nav.open.file.uri"), "Should contain Open File URI");
        assertTrue(navIds.contains("nav.open.url"), "Should contain Open URL");
        assertTrue(navIds.contains("nav.related.rows"), "Should contain Related Rows");
        assertTrue(navIds.contains("nav.goto.line.column"), "Should contain Go to Line:Column...");
        assertTrue(navIds.contains("nav.goto.error.bookmark.group"), "Should contain Goto Error/Bookmark Actions");
        assertTrue(navIds.contains("nav.goto.edit.point.group"), "Should contain GoToEditPointGroup");
        assertTrue(navIds.contains("nav.last.edit.location"), "Should contain Last Edit Location");
        assertTrue(navIds.contains("nav.next.edit.location"), "Should contain Next Edit Location");
        assertTrue(navIds.contains("nav.navigate.in.file.group"), "Should contain Navigate in File");
        assertTrue(navIds.contains("nav.goto.by.reference.group"), "Should contain Goto by Reference Actions");
        assertTrue(navIds.contains("nav.prev.occurrence"), "Should contain Previous Occurrence");
        assertTrue(navIds.contains("nav.next.occurrence"), "Should contain Next Occurrence");
        assertTrue(navIds.contains("nav.dbe.goto.menu.ex"), "Should contain DBE.GoToMenuEx");

        // Verify Goto by Name Actions subgroup (Image 1)
        MenuItemConfig gotoByName = navMenu.getChildren().stream()
                .filter(c -> "nav.goto.by.name.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(gotoByName.isPopup());
        List<String> byNameIds = gotoByName.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(byNameIds.contains("nav.goto.class"));
        assertTrue(byNameIds.contains("nav.goto.file"));
        assertTrue(byNameIds.contains("nav.goto.symbol"));
        assertTrue(byNameIds.contains("nav.goto.text"));
        assertTrue(byNameIds.contains("nav.goto.database.object"));

        // Verify Goto Error/Bookmark Actions subgroup (Image 1)
        MenuItemConfig gotoError = navMenu.getChildren().stream()
                .filter(c -> "nav.goto.error.bookmark.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(gotoError.isPopup());
        List<String> errorIds = gotoError.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(errorIds.contains("nav.next.highlighted.error"));
        assertTrue(errorIds.contains("nav.prev.highlighted.error"));

        // Verify GoToEditPointGroup (Image 1)
        MenuItemConfig editPoint = navMenu.getChildren().stream()
                .filter(c -> "nav.goto.edit.point.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(editPoint.isPopup());
        List<String> epIds = editPoint.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(epIds.contains("nav.next.emmet.edit.point"));
        assertTrue(epIds.contains("nav.prev.emmet.edit.point"));

        // Verify Navigate in File subgroup (Image 2)
        MenuItemConfig navInFile = navMenu.getChildren().stream()
                .filter(c -> "nav.navigate.in.file.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(navInFile.isPopup());
        List<String> fileIds = navInFile.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(fileIds.contains("nav.file.next.statement"));
        assertTrue(fileIds.contains("nav.file.prev.statement"));
        assertTrue(fileIds.contains("nav.file.matching.brace"));
        assertTrue(fileIds.contains("nav.template.parameters.group"));
        assertTrue(fileIds.contains("nav.custom.folding"));
        assertTrue(fileIds.contains("nav.change.navigation.group"));

        // Verify TemplateParametersNavigation & Change Navigation Actions (Image 2)
        MenuItemConfig tplParams = navInFile.getChildren().stream()
                .filter(c -> "nav.template.parameters.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> tplIds = tplParams.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(tplIds.contains("nav.next.template.parameter"));
        assertTrue(tplIds.contains("nav.prev.template.parameter"));

        MenuItemConfig changeNav = navInFile.getChildren().stream()
                .filter(c -> "nav.change.navigation.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> changeIds = changeNav.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(changeIds.contains("nav.next.change"));
        assertTrue(changeIds.contains("nav.prev.change"));

        // Verify Goto by Reference Actions subgroup (Images 2-3)
        MenuItemConfig gotoByRef = navMenu.getChildren().stream()
                .filter(c -> "nav.goto.by.reference.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(gotoByRef.isPopup());
        List<String> refIds = gotoByRef.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(refIds.contains("nav.select.in"));
        assertTrue(refIds.contains("nav.jump.to.nav.bar"));
        assertTrue(refIds.contains("nav.goto.declaration"));
        assertTrue(refIds.contains("nav.goto.implementation"));
        assertTrue(refIds.contains("nav.goto.type.declaration"));
        assertTrue(refIds.contains("nav.goto.super.method"));
        assertTrue(refIds.contains("nav.goto.test"));
        assertTrue(refIds.contains("nav.related.symbol"));
        assertTrue(refIds.contains("nav.file.structure"));
        assertTrue(refIds.contains("nav.file.path"));
        assertTrue(refIds.contains("nav.hierarchy.actions.group"));

        // Verify Hierarchy Actions
        MenuItemConfig hierarchyGroup = gotoByRef.getChildren().stream()
                .filter(c -> "nav.hierarchy.actions.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> hierIds = hierarchyGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(hierIds.contains("nav.type.hierarchy"));
        assertTrue(hierIds.contains("nav.method.hierarchy"));
        assertTrue(hierIds.contains("nav.call.hierarchy"));

        // Verify DBE.GoToMenuEx (Image 3)
        MenuItemConfig dbeGoto = navMenu.getChildren().stream()
                .filter(c -> "nav.dbe.goto.menu.ex".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(dbeGoto.isPopup());
        List<String> dbeIds = dbeGoto.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(dbeIds.contains("nav.dbe.next.statement"));
        assertTrue(dbeIds.contains("nav.dbe.prev.statement"));
    }

    @Test
    @DisplayName("Verify Code Menu and Build completeness matching DataGrip Images 4-5")
    public void testCodeMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");

        // Verify Build exists on Main Menu (Image 4)
        MenuItemConfig buildItem = mainMenu.getChildren().stream()
                .filter(m -> "Build".equals(m.getText()) || "main.menu.build".equals(m.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(buildItem, "Build should exist on Main Menu between Refactor and Run");

        // Verify Code Menu
        MenuItemConfig codeMenu = mainMenu.getChildren().stream()
                .filter(m -> "Code".equals(m.getText()) || "menu.code".equals(m.getId()))
                .findFirst()
                .orElseThrow();

        List<String> codeIds = codeMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(codeIds.contains("code.override.methods"), "Should contain Override Methods...");
        assertTrue(codeIds.contains("code.implement.methods"), "Should contain Implement Methods...");
        assertTrue(codeIds.contains("code.generate"), "Should contain Generate...");
        assertTrue(codeIds.contains("code.completion.group"), "Should contain Code Completion group");
        assertTrue(codeIds.contains("code.inspect.group"), "Should contain InspectCodeInCodeMenuGroup");
        assertTrue(codeIds.contains("code.insert.live.template"), "Should contain Insert Live Template...");
        assertTrue(codeIds.contains("code.save.live.template"), "Should contain Save as Live Template...");
        assertTrue(codeIds.contains("code.surround.with"), "Should contain Surround With...");
        assertTrue(codeIds.contains("code.unwrap.remove"), "Should contain Unwrap/Remove...");
        assertTrue(codeIds.contains("code.folding.group"), "Should contain Folding group");
        assertTrue(codeIds.contains("code.comment.actions.group"), "Should contain Comment Actions group");
        assertTrue(codeIds.contains("code.formatting.actions.group"), "Should contain Code Formatting Actions group");
        assertTrue(codeIds.contains("code.move.statement.down"), "Should contain Move Statement Down");
        assertTrue(codeIds.contains("code.move.statement.up"), "Should contain Move Statement Up");
        assertTrue(codeIds.contains("code.move.element.left"), "Should contain Move Element Left");
        assertTrue(codeIds.contains("code.move.element.right"), "Should contain Move Element Right");
        assertTrue(codeIds.contains("code.move.line.down"), "Should contain Move Line Down");
        assertTrue(codeIds.contains("code.move.line.up"), "Should contain Move Line Up");

        // Verify Code Completion submenu (Image 5)
        MenuItemConfig completionGroup = codeMenu.getChildren().stream()
                .filter(c -> "code.completion.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(completionGroup.isPopup(), "Code Completion should be popup submenu");
        List<String> compIds = completionGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(compIds.contains("code.completion.basic"));
        assertTrue(compIds.contains("code.completion.type.matching"));
        assertTrue(compIds.contains("code.completion.complete.statement"));
        assertTrue(compIds.contains("code.completion.cyclic.expand.word"));
        assertTrue(compIds.contains("code.completion.cyclic.expand.word.backward"));
        assertTrue(compIds.contains("code.completion.call.inline"));
        assertTrue(compIds.contains("code.completion.insert.inline.proposal"));
        assertTrue(compIds.contains("code.completion.insert.inline.word"));
        assertTrue(compIds.contains("code.completion.insert.inline.line"));

        // Verify InspectCodeInCodeMenuGroup (Image 1)
        MenuItemConfig inspect = codeMenu.getChildren().stream()
                .filter(c -> "code.inspect.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(inspect.isPopup(), "InspectCodeInCodeMenuGroup is non-popup group");
        MenuItemConfig inspectActions = inspect.getChildren().stream()
                .filter(c -> "code.inspect.actions.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> inspectActionIds = inspectActions.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(inspectActionIds.contains("code.inspect.code"));
        assertTrue(inspectActionIds.contains("code.cleanup"));

        MenuItemConfig analyzeCode = inspect.getChildren().stream()
                .filter(c -> "code.analyze.code.group".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig analyzeActions = analyzeCode.getChildren().stream()
                .filter(c -> "code.analyze.actions.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> analyzeActionIds = analyzeActions.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(analyzeActionIds.contains("code.silent.cleanup"));
        assertTrue(analyzeActionIds.contains("code.run.inspection.by.name"));
        assertTrue(analyzeActionIds.contains("code.configure.analysis"));
        assertTrue(analyzeActionIds.contains("code.view.offline.results"));
        assertTrue(analyzeActionIds.contains("code.analyze.dataflow.to.here"));
        assertTrue(analyzeActionIds.contains("code.analyze.dataflow.from.here"));
        assertTrue(analyzeCode.getChildren().stream().anyMatch(c -> "code.analyze.platform.menu".equals(c.getId())));

        // Verify Folding submenu (Image 2)
        MenuItemConfig folding = codeMenu.getChildren().stream()
                .filter(c -> "code.folding.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(folding.isPopup(), "Folding should be a popup submenu");
        List<String> foldChildIds = folding.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(foldChildIds.contains("code.folding.expand"));
        assertTrue(foldChildIds.contains("code.folding.expand.recursively"));
        assertTrue(foldChildIds.contains("code.folding.expand.all"));
        assertTrue(foldChildIds.contains("code.folding.collapse"));
        assertTrue(foldChildIds.contains("code.folding.collapse.recursively"));
        assertTrue(foldChildIds.contains("code.folding.collapse.all"));
        assertTrue(foldChildIds.contains("code.folding.toggle"));
        assertTrue(foldChildIds.contains("code.folding.fold.selection"));
        assertTrue(foldChildIds.contains("code.folding.fold.block"));

        MenuItemConfig expandToLevel = folding.getChildren().stream()
                .filter(c -> "code.folding.expand.to.level.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertEquals(5, expandToLevel.getChildren().size());
        assertTrue(expandToLevel.getChildren().stream().allMatch(c -> c.getId().startsWith("code.folding.expand.level.")));

        MenuItemConfig expandAllToLevel = folding.getChildren().stream()
                .filter(c -> "code.folding.expand.all.to.level.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertEquals(5, expandAllToLevel.getChildren().size());
        assertTrue(expandAllToLevel.getChildren().stream().allMatch(c -> c.getId().startsWith("code.folding.expand.all.level.")));

        MenuItemConfig langFolding = folding.getChildren().stream()
                .filter(c -> "code.folding.language.specific.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> langFoldIds = langFolding.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(langFoldIds.contains("code.folding.expand.doc.comments"));
        assertTrue(langFoldIds.contains("code.folding.collapse.doc.comments"));

        // Verify Comment Actions (Image 3)
        MenuItemConfig comments = codeMenu.getChildren().stream()
                .filter(c -> "code.comment.actions.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(comments.isPopup(), "Comment Actions is non-popup group");
        List<String> commentIds = comments.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(commentIds.contains("code.comment.line"));
        assertTrue(commentIds.contains("code.comment.block"));

        // Verify Code Formatting Actions (Image 3)
        MenuItemConfig formatting = codeMenu.getChildren().stream()
                .filter(c -> "code.formatting.actions.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(formatting.isPopup(), "Code Formatting Actions is non-popup group");
        List<String> formatIds = formatting.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(formatIds.contains("code.reformat"));
        assertTrue(formatIds.contains("code.reformat.json"));
        assertTrue(formatIds.contains("code.reformat.file"));
        assertTrue(formatIds.contains("code.auto.indent"));
        assertTrue(formatIds.contains("code.optimize.imports"));
        assertTrue(formatIds.contains("code.rearrange.code"));
    }

    @Test
    @DisplayName("Verify Refactor Menu completeness matching DataGrip Images 4-5")
    public void testRefactorMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig refactorMenu = mainMenu.getChildren().stream()
                .filter(m -> "Refactor".equals(m.getText()) || "menu.refactor".equals(m.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(refactorMenu.isPopup(), "Refactor should be a popup menu");

        List<String> refactorIds = refactorMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(refactorIds.contains("refactor.this"), "Should contain Refactor This...");
        assertTrue(refactorIds.contains("refactor.rename"), "Should contain Rename...");
        assertTrue(refactorIds.contains("refactor.change.signature"), "Should contain Change Signature...");
        assertTrue(refactorIds.contains("refactor.modify.object"), "Should contain Modify Object...");
        assertTrue(refactorIds.contains("refactor.extract.introduce.group"), "Should contain Extract/Introduce popup submenu");
        assertTrue(refactorIds.contains("refactor.inline"), "Should contain Inline...");
        assertTrue(refactorIds.contains("refactor.move"), "Should contain Move...");
        assertTrue(refactorIds.contains("refactor.copy"), "Should contain Copy...");
        assertTrue(refactorIds.contains("refactor.safe.delete"), "Should contain Safe Delete...");
        assertTrue(refactorIds.contains("refactor.pull.members.up"), "Should contain Pull Members Up...");
        assertTrue(refactorIds.contains("refactor.push.members.down"), "Should contain Push Members Down...");
        assertTrue(refactorIds.contains("refactor.invert.boolean"), "Should contain Invert Boolean...");

        // Verify Extract/Introduce submenu (Image 5)
        MenuItemConfig extractIntro = refactorMenu.getChildren().stream()
                .filter(c -> "refactor.extract.introduce.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(extractIntro.isPopup(), "Extract/Introduce should be popup submenu");
        List<String> extractIds = extractIntro.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(extractIds.contains("refactor.introduce.variable"));
        assertTrue(extractIds.contains("refactor.extract.routine"));
        assertTrue(extractIds.contains("refactor.table.alias"));
        assertTrue(extractIds.contains("refactor.introduce.constant"));
        assertTrue(extractIds.contains("refactor.introduce.field"));
        assertTrue(extractIds.contains("refactor.introduce.parameter"));
        assertTrue(extractIds.contains("refactor.introduce.parameter.object"));
        assertTrue(extractIds.contains("refactor.extract.method"));
        assertTrue(extractIds.contains("refactor.extract.delegate"));
        assertTrue(extractIds.contains("refactor.include.file"));
        assertTrue(extractIds.contains("refactor.extract.interface"));
        assertTrue(extractIds.contains("refactor.extract.superclass"));
        assertTrue(extractIds.contains("refactor.extract.module"));
        assertTrue(extractIds.contains("refactor.subquery.cte"));
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
    @DisplayName("Verify Run Menu completeness matching DataGrip Images 1-4")
    public void testRunMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig runMenu = mainMenu.getChildren().stream()
                .filter(m -> "Run".equals(m.getText()) || "menu.run".equals(m.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(runMenu.isPopup(), "Run should be a popup menu");

        List<String> runIds = runMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(runIds.contains("run.run.debug.group"), "Should contain Run/Debug group");
        assertTrue(runIds.contains("run.run.ellipsis"), "Should contain Run...");
        assertTrue(runIds.contains("run.debug.ellipsis"), "Should contain Debug...");
        assertTrue(runIds.contains("run.xdebugger.attach.group"), "Should contain XDebugger.AttachGroup");
        assertTrue(runIds.contains("run.edit.configurations"), "Should contain Edit Configurations...");
        assertTrue(runIds.contains("run.manage.targets"), "Should contain Manage Targets...");
        assertTrue(runIds.contains("run.stop"), "Should contain Stop");
        assertTrue(runIds.contains("run.stop.background.processes"), "Should contain Stop Background Processes...");
        assertTrue(runIds.contains("run.show.running.list"), "Should contain Show Running List");
        assertTrue(runIds.contains("run.debugger.actions.group"), "Should contain Debugger Actions group");
        assertTrue(runIds.contains("run.test.group"), "Should contain RunTestGroup");
        assertTrue(runIds.contains("run.profiler.actions.group"), "Should contain ProfilerActions group");

        // Verify Run/Debug group (Image 2)
        MenuItemConfig runDebugGroup = runMenu.getChildren().stream()
                .filter(c -> "run.run.debug.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> runDebugIds = runDebugGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(runDebugIds.contains("run.run"));
        assertTrue(runDebugIds.contains("run.debug"));
        assertTrue(runDebugIds.contains("run.coverage"));
        assertTrue(runDebugIds.contains("run.profiler"));

        // Verify XDebugger.AttachGroup (Image 2)
        MenuItemConfig attachGroup = runMenu.getChildren().stream()
                .filter(c -> "run.xdebugger.attach.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(attachGroup.getChildren().stream().anyMatch(c -> "run.attach.to.process".equals(c.getId())));

        // Verify Debugger Actions (Image 3)
        MenuItemConfig debuggerActions = runMenu.getChildren().stream()
                .filter(c -> "run.debugger.actions.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> dbgIds = debuggerActions.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(dbgIds.contains("run.debugging.actions.group"));
        assertTrue(dbgIds.contains("run.toggle.breakpoint.group"));
        assertTrue(dbgIds.contains("run.view.breakpoints"));

        // Debugging Actions
        MenuItemConfig debuggingActions = debuggerActions.getChildren().stream()
                .filter(c -> "run.debugging.actions.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> dbgActIds = debuggingActions.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(dbgActIds.contains("run.debug.reload.group"));
        assertTrue(dbgActIds.contains("run.step.over.ref"));
        assertTrue(dbgActIds.contains("run.pause.ref"));
        assertTrue(dbgActIds.contains("run.resume.ref"));
        assertTrue(dbgActIds.contains("run.evaluate.expression"));
        assertTrue(dbgActIds.contains("run.show.execution.point"));

        // StepOver.Ref
        MenuItemConfig stepOverRef = debuggingActions.getChildren().stream()
                .filter(c -> "run.step.over.ref".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> stepIds = stepOverRef.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(stepIds.contains("run.step.over"));
        assertTrue(stepIds.contains("run.force.step.over"));
        assertTrue(stepIds.contains("run.step.into"));
        assertTrue(stepIds.contains("run.force.step.into"));
        assertTrue(stepIds.contains("run.smart.step.into"));
        assertTrue(stepIds.contains("run.step.out"));
        assertTrue(stepIds.contains("run.run.to.cursor"));
        assertTrue(stepIds.contains("run.force.run.to.cursor"));
        assertTrue(stepIds.contains("run.reset.frame"));

        // Toggle Breakpoint group
        MenuItemConfig toggleBreakpoint = debuggerActions.getChildren().stream()
                .filter(c -> "run.toggle.breakpoint.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> bpIds = toggleBreakpoint.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(bpIds.contains("run.restore.breakpoint"));
        assertTrue(bpIds.contains("run.toggle.line.breakpoint"));
        assertTrue(bpIds.contains("run.toggle.temporary.line.breakpoint"));

        // Verify RunTestGroup (Image 4)
        MenuItemConfig runTestGroup = runMenu.getChildren().stream()
                .filter(c -> "run.test.group".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig smRunTestGroup = runTestGroup.getChildren().stream()
                .filter(c -> "run.sm.run.test.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> smIds = smRunTestGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(smIds.contains("run.test.history"));
        assertTrue(smIds.contains("run.import.tests.from.file"));

        MenuItemConfig coveragePlatform = runTestGroup.getChildren().stream()
                .filter(c -> "run.coverage.platform.menu".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig coverageMenu = coveragePlatform.getChildren().stream()
                .filter(c -> "run.coverage.menu".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> covIds = coverageMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(covIds.contains("run.manage.coverage.reports"));
        assertTrue(covIds.contains("run.generate.coverage.report"));
        assertTrue(covIds.contains("run.hide.coverage"));

        // Verify ProfilerActions (Image 2)
        MenuItemConfig profilerGroup = runMenu.getChildren().stream()
                .filter(c -> "run.profiler.actions.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> profIds = profilerGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(profIds.contains("run.attach.profiler.to.process"));
        assertTrue(profIds.contains("run.open.profiler.snapshot"));
    }

    @Test
    @DisplayName("Verify Tools Menu completeness matching DataGrip Image 1")
    public void testToolsMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig toolsMenu = mainMenu.getChildren().stream()
                .filter(m -> "Tools".equals(m.getText()) || "menu.tools".equals(m.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(toolsMenu.isPopup(), "Tools should be a popup menu");

        List<String> toolsIds = toolsMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(toolsIds.contains("tools.psi.viewer.actions"), "Should contain Dev.PsiViewerActions");
        assertTrue(toolsIds.contains("tools.basic.group"), "Should contain Tools Basic Group");
        assertTrue(toolsIds.contains("tools.create.command.line.launcher"), "Should contain Create Command Line Launcher...");
        assertTrue(toolsIds.contains("tools.create.desktop.entry"), "Should contain Create Desktop Entry...");
        assertTrue(toolsIds.contains("tools.other.menu"), "Should contain OtherMenu");
        assertTrue(toolsIds.contains("tools.services"), "Should contain Services");
        assertTrue(toolsIds.contains("tools.xml.actions"), "Should contain XML Actions");
        assertTrue(toolsIds.contains("tools.markdown"), "Should contain Markdown");
        assertTrue(toolsIds.contains("tools.external.tools"), "Should contain External Tools");

        // Dev.PsiViewerActions
        MenuItemConfig psiViewer = toolsMenu.getChildren().stream()
                .filter(c -> "tools.psi.viewer.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> psiIds = psiViewer.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(psiIds.contains("tools.view.psi.structure"));
        assertTrue(psiIds.contains("tools.view.psi.structure.current.file"));

        // XML Actions
        MenuItemConfig xmlActions = toolsMenu.getChildren().stream()
                .filter(c -> "tools.xml.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> xmlIds = xmlActions.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(xmlIds.contains("tools.convert.schema"));

        // Markdown
        MenuItemConfig markdown = toolsMenu.getChildren().stream()
                .filter(c -> "tools.markdown".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> mdIds = markdown.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(mdIds.contains("tools.markdown.import.word"));
        assertTrue(mdIds.contains("tools.markdown.export.file.to"));
        assertTrue(mdIds.contains("tools.markdown.configure.pandoc"));
    }

    @Test
    @DisplayName("Verify Git Menu completeness matching DataGrip Images 2-5")
    public void testGitMenuCompletenessMatchingDataGrip() {
        MenuItemConfig mainMenu = settings.getMenuConfig("root.main.menu");
        MenuItemConfig gitMenu = mainMenu.getChildren().stream()
                .filter(m -> "Git".equals(m.getText()) || "menu.vcs".equals(m.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(gitMenu.isPopup(), "Git should be a popup menu");

        List<String> topIds = gitMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(topIds.contains("vcs.main.menu"), "Should contain Vcs.MainMenu");
        assertTrue(topIds.contains("git.main.menu"), "Should contain Git.MainMenu");

        // Vcs.MainMenu (Image 2)
        MenuItemConfig vcsMainMenu = gitMenu.getChildren().stream()
                .filter(c -> "vcs.main.menu".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> vcsMainIds = vcsMainMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(vcsMainIds.contains("vcs.enable.integration"));
        assertTrue(vcsMainIds.contains("vcs.group"));
        assertTrue(vcsMainIds.contains("vcs.get.from.vcs"));
        assertTrue(vcsMainIds.contains("vcs.browse.repository"));
        assertTrue(vcsMainIds.contains("vcs.import.into.vcs"));

        // VCS Group (Image 3)
        MenuItemConfig vcsGroup = vcsMainMenu.getChildren().stream()
                .filter(c -> "vcs.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> vcsGroupIds = vcsGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(vcsGroupIds.contains("vcs.operations.popup"));
        assertTrue(vcsGroupIds.contains("vcs.commit"));
        assertTrue(vcsGroupIds.contains("vcs.toggle.commit.ui"));
        assertTrue(vcsGroupIds.contains("vcs.update.project"));
        assertTrue(vcsGroupIds.contains("vcs.integrate.project"));
        assertTrue(vcsGroupIds.contains("vcs.refresh"));
        assertTrue(vcsGroupIds.contains("vcs.show.local.changes.uml"));
        assertTrue(vcsGroupIds.contains("vcs.specific"));
        assertTrue(vcsGroupIds.contains("vcs.git.group"));

        // Git inside VCS Group (Image 3, 4, 5)
        MenuItemConfig vcsGit = vcsGroup.getChildren().stream()
                .filter(c -> "vcs.git.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> vcsGitIds = vcsGit.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(vcsGitIds.contains("git.file.actions"));
        assertTrue(vcsGitIds.contains("git.rollback"));
        assertTrue(vcsGitIds.contains("git.repository.actions"));
        assertTrue(vcsGitIds.contains("git.create.patch"));
        assertTrue(vcsGitIds.contains("git.apply.patch"));
        assertTrue(vcsGitIds.contains("git.apply.patch.from.clipboard"));
        assertTrue(vcsGitIds.contains("git.shelve.changes"));

        // Git.FileActions (Image 4)
        MenuItemConfig fileActions = vcsGit.getChildren().stream()
                .filter(c -> "git.file.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> fileActionIds = fileActions.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(fileActionIds.contains("git.commit.file"));
        assertTrue(fileActionIds.contains("git.add"));
        assertTrue(fileActionIds.contains("git.add.to.gitignore"));
        assertTrue(fileActionIds.contains("git.annotate"));
        assertTrue(fileActionIds.contains("git.compare.same.version"));
        assertTrue(fileActionIds.contains("git.compare.with.revision"));
        assertTrue(fileActionIds.contains("git.compare.with.branch"));
        assertTrue(fileActionIds.contains("git.show.history"));
        assertTrue(fileActionIds.contains("git.show.history.for.selection"));

        // GitRepositoryActions (Image 5)
        MenuItemConfig repoActions = vcsGit.getChildren().stream()
                .filter(c -> "git.repository.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> repoActionIds = repoActions.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(repoActionIds.contains("git.push"));
        assertTrue(repoActionIds.contains("git.pull"));
        assertTrue(repoActionIds.contains("git.fetch"));
        assertTrue(repoActionIds.contains("git.merge"));
        assertTrue(repoActionIds.contains("git.merge.group"));
        assertTrue(repoActionIds.contains("git.rebase"));
        assertTrue(repoActionIds.contains("git.rebase.group"));
        assertTrue(repoActionIds.contains("git.branches"));
        assertTrue(repoActionIds.contains("git.new.branch"));
        assertTrue(repoActionIds.contains("git.new.tag"));
        assertTrue(repoActionIds.contains("git.reset.head"));
        assertTrue(repoActionIds.contains("git.stash.changes"));
        assertTrue(repoActionIds.contains("git.unstash.changes"));
        assertTrue(repoActionIds.contains("git.manage.remotes"));
        assertTrue(repoActionIds.contains("git.clone"));
        assertTrue(repoActionIds.contains("git.abort.revert"));
        assertTrue(repoActionIds.contains("git.abort.cherry.pick"));

        // Merge group & Rebase group
        MenuItemConfig mergeGroup = repoActions.getChildren().stream()
                .filter(c -> "git.merge.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(mergeGroup.getChildren().stream().anyMatch(c -> "git.abort.merge".equals(c.getId())));

        MenuItemConfig rebaseGroup = repoActions.getChildren().stream()
                .filter(c -> "git.rebase.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> rebaseIds = rebaseGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(rebaseIds.contains("git.abort.rebase"));
        assertTrue(rebaseIds.contains("git.continue.rebase"));
        assertTrue(rebaseIds.contains("git.skip.commit"));

        // Image 1: Browse VCS Repository and Import into Version Control children
        MenuItemConfig browseVcs = vcsMainMenu.getChildren().stream()
                .filter(c -> "vcs.browse.repository".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(browseVcs.getChildren().stream().anyMatch(c -> "vcs.browse.git.log".equals(c.getId())));

        MenuItemConfig importVcs = vcsMainMenu.getChildren().stream()
                .filter(c -> "vcs.import.into.vcs".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(importVcs.getChildren().stream().anyMatch(c -> "vcs.create.git.repository".equals(c.getId())));

        // Images 1-4: Git.MainMenu verification
        MenuItemConfig gitMainMenu = gitMenu.getChildren().stream()
                .filter(c -> "git.main.menu".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> gitMainIds = gitMainMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(gitMainIds.contains("git.main.commit"));
        assertTrue(gitMainIds.contains("git.main.toggle.commit.ui"));
        assertTrue(gitMainIds.contains("git.push"));
        assertTrue(gitMainIds.contains("vcs.update.project"));
        assertTrue(gitMainIds.contains("git.pull"));
        assertTrue(gitMainIds.contains("git.fetch"));
        assertTrue(gitMainIds.contains("git.unshallow"));
        assertTrue(gitMainIds.contains("git.merge"));
        assertTrue(gitMainIds.contains("git.main.merge.group"));
        assertTrue(gitMainIds.contains("git.rebase"));
        assertTrue(gitMainIds.contains("git.main.rebase.group"));
        assertTrue(gitMainIds.contains("git.resolve.conflicts"));
        assertTrue(gitMainIds.contains("git.revert.resolved"));
        assertTrue(gitMainIds.contains("git.branches"));
        assertTrue(gitMainIds.contains("git.new.branch"));
        assertTrue(gitMainIds.contains("git.new.tag"));
        assertTrue(gitMainIds.contains("git.reset.head"));
        assertTrue(gitMainIds.contains("git.show.vcs.log"));
        assertTrue(gitMainIds.contains("git.patch.group"));
        assertTrue(gitMainIds.contains("git.uncommitted.changes.group"));
        assertTrue(gitMainIds.contains("git.main.menu.file.actions"));
        assertTrue(gitMainIds.contains("git.manage.remotes"));
        assertTrue(gitMainIds.contains("git.clone"));
        assertTrue(gitMainIds.contains("vcs.operations.popup"));
        assertTrue(gitMainIds.contains("git.abort.revert"));
        assertTrue(gitMainIds.contains("git.abort.cherry.pick"));

        // Image 3: Patch group under Git.MainMenu
        MenuItemConfig patchGroup = gitMainMenu.getChildren().stream()
                .filter(c -> "git.patch.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> patchIds = patchGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(patchIds.contains("git.create.patch"));
        assertTrue(patchIds.contains("git.apply.patch"));
        assertTrue(patchIds.contains("git.apply.patch.from.clipboard"));

        // Image 4: Uncommitted Changes group under Git.MainMenu
        MenuItemConfig uncommittedGroup = gitMainMenu.getChildren().stream()
                .filter(c -> "git.uncommitted.changes.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> uncommittedIds = uncommittedGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(uncommittedIds.contains("git.shelve.changes"));
        assertTrue(uncommittedIds.contains("git.show.shelf"));
        assertTrue(uncommittedIds.contains("git.show.git.stash"));
        assertTrue(uncommittedIds.contains("git.stash.changes"));
        assertTrue(uncommittedIds.contains("git.unstash.changes"));
        assertTrue(uncommittedIds.contains("git.rollback"));
        assertTrue(uncommittedIds.contains("vcs.uml.diff"));

        MenuItemConfig umlDiff = uncommittedGroup.getChildren().stream()
                .filter(c -> "vcs.uml.diff".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(umlDiff.getChildren().stream().anyMatch(c -> "vcs.show.local.changes.uml".equals(c.getId())));

        // Image 4: Git.MainMenu.FileActions > Git.FileActions
        MenuItemConfig gitMainMenuFileActions = gitMainMenu.getChildren().stream()
                .filter(c -> "git.main.menu.file.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig fileActionsRef = gitMainMenuFileActions.getChildren().stream()
                .filter(c -> "git.file.actions.ref".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> refIds = fileActionsRef.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(refIds.contains("git.commit.file"));
        assertTrue(refIds.contains("git.add"));
        assertTrue(refIds.contains("git.add.to.gitignore"));
        assertTrue(refIds.contains("git.annotate"));
        assertTrue(refIds.contains("git.compare.same.version"));
        assertTrue(refIds.contains("git.compare.with.revision"));
        assertTrue(refIds.contains("git.compare.with.branch"));
        assertTrue(refIds.contains("git.show.history"));
        assertTrue(refIds.contains("git.show.history.for.selection"));
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

        // Image 1: Window Top-Level items
        List<String> winIds = windowMenu.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(winIds.contains("window.minimize"));
        assertTrue(winIds.contains("window.zoom"));
        assertTrue(winIds.contains("window.layouts"));
        assertTrue(winIds.contains("window.active.tool.window"));
        assertTrue(winIds.contains("window.editor.tabs"));
        assertTrue(winIds.contains("window.notifications"));
        assertTrue(winIds.contains("window.background.tasks"));
        assertTrue(winIds.contains("window.open.project.windows"));

        // Image 2: Tool Window Layouts submenu
        MenuItemConfig layouts = windowMenu.getChildren().stream()
                .filter(c -> "window.layouts".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(layouts.isPopup());
        assertEquals("Tool Window Layouts", layouts.getText());
        List<String> layoutIds = layouts.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(layoutIds.contains("window.layouts.default"));
        assertTrue(layoutIds.contains("window.layouts.list"));
        assertTrue(layoutIds.contains("window.layouts.restore.current"));
        assertTrue(layoutIds.contains("window.layouts.save.changes.current"));
        assertTrue(layoutIds.contains("window.layouts.save.as.new"));

        // Image 2: Open Project Windows submenu
        MenuItemConfig openProj = windowMenu.getChildren().stream()
                .filter(c -> "window.open.project.windows".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(openProj.isPopup());
        assertEquals("Open Project Windows", openProj.getText());
        List<String> openProjIds = openProj.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(openProjIds.contains("window.next.project"));
        assertTrue(openProjIds.contains("window.prev.project"));
        assertTrue(openProjIds.contains("window.project.merge.all"));

        // Image 3: Active Tool Window submenu & Resize popup
        MenuItemConfig activeTw = windowMenu.getChildren().stream()
                .filter(c -> "window.active.tool.window".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(activeTw.isPopup());
        List<String> activeTwIds = activeTw.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(activeTwIds.contains("window.toolwindow.hide.active"));
        assertTrue(activeTwIds.contains("window.toolwindow.hide.side"));
        assertTrue(activeTwIds.contains("window.toolwindow.hide.all"));
        assertTrue(activeTwIds.contains("window.toolwindow.open.as.editor.tab"));
        assertTrue(activeTwIds.contains("window.toolwindow.pin.tab"));
        assertTrue(activeTwIds.contains("window.toolwindow.close.active.tab"));
        assertTrue(activeTwIds.contains("window.toolwindow.jump.last"));
        assertTrue(activeTwIds.contains("window.toolwindow.maximize"));
        assertTrue(activeTwIds.contains("window.toolwindow.dock"));
        assertTrue(activeTwIds.contains("window.toolwindow.view.mode"));
        assertTrue(activeTwIds.contains("window.toolwindow.move.to"));
        assertTrue(activeTwIds.contains("window.toolwindow.group.tabs"));
        assertTrue(activeTwIds.contains("window.toolwindow.show.list.of.tabs"));
        assertTrue(activeTwIds.contains("window.toolwindow.resize"));

        MenuItemConfig resize = activeTw.getChildren().stream()
                .filter(c -> "window.toolwindow.resize".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(resize.isPopup());
        List<String> resizeIds = resize.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(resizeIds.contains("window.tw.resize.left"));
        assertTrue(resizeIds.contains("window.tw.resize.right"));
        assertTrue(resizeIds.contains("window.tw.resize.top"));
        assertTrue(resizeIds.contains("window.tw.resize.bottom"));

        // Image 3: Notifications & Background Tasks submenus
        MenuItemConfig notifications = windowMenu.getChildren().stream()
                .filter(c -> "window.notifications".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(notifications.isPopup());
        List<String> notifIds = notifications.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(notifIds.contains("window.notifications.close.first"));
        assertTrue(notifIds.contains("window.notifications.close.all"));

        MenuItemConfig bgTasks = windowMenu.getChildren().stream()
                .filter(c -> "window.background.tasks".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(bgTasks.isPopup());
        assertEquals("Background Tasks", bgTasks.getText());
        List<String> bgTaskIds = bgTasks.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(bgTaskIds.contains("window.background.tasks.show"));
        assertTrue(bgTaskIds.contains("window.background.tasks.auto.show"));

        // Images 4 & 5: Editor Tabs submenu, Editor Close Actions, Split with Chooser Navigation
        MenuItemConfig editorTabs = windowMenu.getChildren().stream()
                .filter(c -> "window.editor.tabs".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(editorTabs.isPopup());
        List<String> tabIds = editorTabs.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(tabIds.contains("window.editor.next.tab"));
        assertTrue(tabIds.contains("window.editor.prev.tab"));
        assertTrue(tabIds.contains("window.editor.pin.tab"));
        assertTrue(tabIds.contains("window.editor.keep.tab.open"));
        assertTrue(tabIds.contains("window.editor.show.hidden.tabs"));
        assertTrue(tabIds.contains("window.editor.close.actions"));
        assertTrue(tabIds.contains("window.reopen.tab"));
        assertTrue(tabIds.contains("window.split.right"));
        assertTrue(tabIds.contains("window.split.and.move.right"));
        assertTrue(tabIds.contains("window.split.down"));
        assertTrue(tabIds.contains("window.split.and.move.down"));
        assertTrue(tabIds.contains("window.split.chooser.open"));
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

        // Image 4: Editor Close Actions popup
        MenuItemConfig closeActions = editorTabs.getChildren().stream()
                .filter(c -> "window.editor.close.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(closeActions.isPopup());
        List<String> closeIds = closeActions.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(closeIds.contains("window.close.tab"));
        assertTrue(closeIds.contains("window.close.other.tabs"));
        assertTrue(closeIds.contains("window.close.all.tabs"));
        assertTrue(closeIds.contains("window.close.unmodified.tabs"));
        assertTrue(closeIds.contains("window.close.all.but.pinned"));
        assertTrue(closeIds.contains("window.close.tabs.left"));
        assertTrue(closeIds.contains("window.close.tabs.right"));
        assertTrue(closeIds.contains("window.close.all.readonly"));
        assertTrue(closeIds.contains("window.editor.open.as.editor.tab"));

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
    @DisplayName("Verify Help Menu structure and nested groups match DataGrip (Images 1-4)")
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

        // Image 1: Top-level Help menu items
        assertTrue(childIds.contains("help.find.action"));
        assertTrue(childIds.contains("help.help"));
        assertTrue(childIds.contains("help.learn.group"));
        assertTrue(childIds.contains("help.whats.new"));
        assertTrue(childIds.contains("help.configure.new.ui"));
        assertTrue(childIds.contains("help.getting.started"));
        assertTrue(childIds.contains("help.youtube"));
        assertTrue(childIds.contains("help.shortcuts.pdf"));
        assertTrue(childIds.contains("help.productivity.features"));
        assertTrue(childIds.contains("help.contact.support"));
        assertTrue(childIds.contains("help.bug.report"));
        assertTrue(childIds.contains("help.submit.feedback"));
        assertTrue(childIds.contains("help.show.log.in.files"));
        assertTrue(childIds.contains("help.show.sql.log.in.files"));
        assertTrue(childIds.contains("help.collect.logs"));
        assertTrue(childIds.contains("help.delete.leftover.dirs"));
        assertTrue(childIds.contains("help.diagnostic.tools"));
        assertTrue(childIds.contains("help.change.memory.settings"));
        assertTrue(childIds.contains("help.custom.properties"));
        assertTrue(childIds.contains("help.custom.vm.options"));
        assertTrue(childIds.contains("help.registration.actions"));
        assertTrue(childIds.contains("help.updates"));
        assertTrue(childIds.contains("help.about"));

        // Verify LearnGroup (non-popup)
        MenuItemConfig learnGroup = children.stream()
                .filter(c -> "help.learn.group".equals(c.getId()))
                .findFirst().orElseThrow();
        assertFalse(learnGroup.isPopup());
        List<String> learnIds = learnGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(learnIds.contains("help.learn.features"));

        // Image 2: ProductivityFeatures popup
        MenuItemConfig productivityFeatures = children.stream()
                .filter(c -> "help.productivity.features".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(productivityFeatures.isPopup());
        List<String> prodIds = productivityFeatures.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(prodIds.contains("help.tip.of.the.day"));
        assertTrue(prodIds.contains("help.my.productivity"));
        long prodSeps = productivityFeatures.getChildren().stream()
                .filter(c -> c.getType() == MenuItemConfig.Type.SEPARATOR).count();
        assertEquals(1, prodSeps);

        // Images 2 & 3: Diagnostic Tools popup and nested groups
        MenuItemConfig diagnosticTools = children.stream()
                .filter(c -> "help.diagnostic.tools".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(diagnosticTools.isPopup());
        List<String> diagIds = diagnosticTools.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(diagIds.contains("help.diagnostic.activity.monitor"));
        assertTrue(diagIds.contains("help.diagnostic.dump.threads"));
        assertTrue(diagIds.contains("help.diagnostic.run.memory.tester"));
        assertTrue(diagIds.contains("help.diagnostic.debug.log.settings"));
        assertTrue(diagIds.contains("help.diagnostic.special.files"));
        assertTrue(diagIds.contains("help.diagnostic.start.profile.group"));
        assertTrue(diagIds.contains("help.diagnostic.diagnostic.group"));
        assertTrue(diagIds.contains("help.diagnostic.indexing.diagnostic.group"));

        // Image 3: StartProfileGroup -> AsyncGroup
        MenuItemConfig startProfileGroup = diagnosticTools.getChildren().stream()
                .filter(c -> "help.diagnostic.start.profile.group".equals(c.getId()))
                .findFirst().orElseThrow();
        MenuItemConfig asyncGroup = startProfileGroup.getChildren().stream()
                .filter(c -> "help.diagnostic.async.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> asyncIds = asyncGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(asyncIds.contains("help.diagnostic.start.cpu.profiling"));
        assertTrue(asyncIds.contains("help.diagnostic.start.async.profiler"));

        // Image 3: DiagnosticGroup -> AsyncDiagnosticGroup
        MenuItemConfig diagGroup = diagnosticTools.getChildren().stream()
                .filter(c -> "help.diagnostic.diagnostic.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> diagGroupIds = diagGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(diagGroupIds.contains("help.diagnostic.capture.memory.snapshot"));
        MenuItemConfig asyncDiagGroup = diagGroup.getChildren().stream()
                .filter(c -> "help.diagnostic.async.diagnostic.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> asyncDiagIds = asyncDiagGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(asyncDiagIds.contains("help.diagnostic.profile.indexing"));

        // Image 3: IndexingDiagnosticGroup
        MenuItemConfig indexingDiagGroup = diagnosticTools.getChildren().stream()
                .filter(c -> "help.diagnostic.indexing.diagnostic.group".equals(c.getId()))
                .findFirst().orElseThrow();
        List<String> indexingDiagIds = indexingDiagGroup.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(indexingDiagIds.contains("help.diagnostic.open.indexing.diagnostics"));

        // Image 4: Registration Actions popup
        MenuItemConfig registrationActions = children.stream()
                .filter(c -> "help.registration.actions".equals(c.getId()))
                .findFirst().orElseThrow();
        assertTrue(registrationActions.isPopup());
        List<String> regIds = registrationActions.getChildren().stream().map(MenuItemConfig::getId).toList();
        assertTrue(regIds.contains("help.registration.register"));

        // Verify ActionCatalog contains Help category and entries
        ActionManager actionManager = ActionManager.getInstance();
        var catalog = actionManager.getActionCatalog();
        var mainMenuCat = catalog.stream().filter(c -> "Main Menu".equals(c.getName())).findFirst().orElseThrow();
        var helpCat = mainMenuCat.getSubCategories().stream().filter(c -> "Help".equals(c.getName())).findFirst().orElseThrow();
        assertFalse(helpCat.getEntries().isEmpty());
        List<String> catalogIds = helpCat.getEntries().stream().map(ActionManager.ActionCatalogEntry::getId).toList();
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
}



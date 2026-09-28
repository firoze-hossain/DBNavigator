package com.roze.dbnavigator.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.MenuItemConfig;
import com.roze.dbnavigator.ui.action.ActionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        assertTrue(newGroupItems.contains("SQL File"));
        assertTrue(newGroupItems.contains("Scratch File"));
        assertTrue(newGroupItems.contains("Query Console"));
        assertTrue(newGroupItems.contains("Data Source"));
    }

    @Test
    @DisplayName("Verify Main Toolbar structure has Left, Center, and Right groups")
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

        MenuItemConfig centerGroup = toolbar.getChildren().stream()
                .filter(g -> "Center".equals(g.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(centerGroup);

        List<String> centerActions = centerGroup.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();
        assertTrue(centerActions.contains("Database Explorer"));
        assertTrue(centerActions.contains("Execute"));
        assertTrue(centerActions.contains("New Query Console"));
        assertTrue(centerActions.contains("Open SQL File"));
        assertTrue(centerActions.contains("More Actions"));
    }

    @Test
    @DisplayName("Verify Editor Popup Menu structure contains actions and folding group")
    public void testEditorPopupMenuStructure() {
        MenuItemConfig editorPopup = settings.getMenuConfig("root.editor.popup");
        assertNotNull(editorPopup);

        List<String> items = editorPopup.getChildren().stream()
                .map(MenuItemConfig::getText)
                .toList();
        assertTrue(items.contains("Execute"));
        assertTrue(items.contains("Explain Plan"));
        assertTrue(items.contains("Cut"));
        assertTrue(items.contains("Copy"));
        assertTrue(items.contains("Paste"));
        assertTrue(items.contains("Reformat Code"));
        assertTrue(items.contains("Folding"));

        // Check Folding group sub-items
        MenuItemConfig foldingGroup = editorPopup.getChildren().stream()
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

        MenuItemConfig tabPopup = settings.getMenuConfig("root.editor.tab.popup");
        assertNotNull(tabPopup);
        List<String> tabItems = tabPopup.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(tabItems.contains("Editor Close Actions"));
        assertTrue(tabItems.contains("Copy Paths"));
        assertTrue(tabItems.contains("Copy Reference"));
        assertTrue(tabItems.contains("Copy JSON Pointer"));
        assertTrue(tabItems.contains("Change Template Data Language"));
        assertTrue(tabItems.contains("Vcs.Diff.EditorTabs.Group"));
        assertTrue(tabItems.contains("Split Right"));
        assertTrue(tabItems.contains("Split Down"));
        assertTrue(tabItems.contains("Pin Active Tab"));
        assertTrue(tabItems.contains("Keep Tab Open"));
        assertTrue(tabItems.contains("Open Tab in New Window"));
        assertTrue(tabItems.contains("Configure Editor Tabs\u2026"));
        assertTrue(tabItems.contains("Reopen Closed Tab"));
        assertTrue(tabItems.contains("Bookmarks"));
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
        assertTrue(items.contains("Cut/Copy/Paste Actions"));
        assertTrue(items.contains("FileEditor.ImportToDatabase.Group"));
        assertTrue(items.contains("Edit Source"));
        assertTrue(items.contains("Find Usages"));
        assertTrue(items.contains("Find in Files\u2026"));
        assertTrue(items.contains("Rename\u2026"));
        assertTrue(items.contains("Project View Popup Refactoring Group"));
        assertTrue(items.contains("VCS/LVCS Actions"));
        assertTrue(items.contains("Cache Recovery"));
        assertTrue(items.contains("Reload from Disk"));
        assertTrue(items.contains("Compare Files"));
        assertTrue(items.contains("Compare File with Editor"));
        assertTrue(items.contains("External Tools"));
        assertTrue(items.contains("Set Background Image"));
        assertTrue(items.contains("Diagrams"));
        assertTrue(items.contains("Convert to PNG"));
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
        assertTrue(navBarItems.contains("@ AIAssistantHubPopupAction"));
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
        assertTrue(dbgItems.contains("Step Into"));
        assertTrue(dbgItems.contains("Step Out"));
        assertTrue(dbgItems.contains("View Breakpoints\u2026"));
        assertTrue(dbgItems.contains("Mute Breakpoints"));

        MenuItemConfig watches = settings.getMenuConfig("root.debug.watches.toolbar");
        assertNotNull(watches);
        List<String> watchItems = watches.getChildren().stream().map(MenuItemConfig::getText).toList();
        assertTrue(watchItems.contains("+ New Watch\u2026"));
        assertTrue(watchItems.contains("\u2014 Remove Watch"));
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
        assertTrue(histItems.contains("Resume Indexing"));

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
    @DisplayName("Verify File menu contains all 21 options matching DataGrip and ActionRegistry")
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

        // Check all 21 items from Image 1 / ActionRegistry are present
        assertTrue(actionIds.contains("file.new"), "Should contain New group");
        assertTrue(actionIds.contains("file.open.sql"), "Should contain Open...");
        assertTrue(actionIds.contains("file.save.as"), "Should contain Save As...");
        assertTrue(actionIds.contains("file.recent"), "Should contain Recent Projects");
        assertTrue(actionIds.contains("file.rename.project"), "Should contain Rename Project...");
        assertTrue(actionIds.contains("file.attach.directory"), "Should contain Attach Directory to Project...");
        assertTrue(actionIds.contains("file.settings"), "Should contain Settings...");
        assertTrue(actionIds.contains("file.data.sources"), "Should contain Data Sources...");
        assertTrue(actionIds.contains("file.plugins"), "Should contain Plugins...");
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

        MenuItemConfig cloudGroup = newGroup.getChildren().stream()
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
        // Create an outdated File menu without file.save.as
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
        assertTrue(ids.contains("file.save.as"), "Migrated File menu must now include file.save.as");
        assertTrue(ids.contains("file.recent"), "Migrated File menu must now include file.recent");
        assertTrue(ids.contains("file.data.sources"), "Migrated File menu must now include file.data.sources");
    }
}


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
}


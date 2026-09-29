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
}


package com.roze.dbnavigator.ui.action;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ActionRegistryTest {

    private static ActionManager actionManager;

    @BeforeAll
    public static void setUp() {
        actionManager = ActionManager.getInstance();
        ActionRegistry.initialize(actionManager);
    }

    @Test
    public void testFileMenuRegistration() {
        ActionGroup fileMenu = actionManager.getGroup("menu.file");
        assertNotNull(fileMenu, "menu.file group should be registered");
        assertEquals("File", fileMenu.getText());

        ActionGroup newGroup = (ActionGroup) fileMenu.getChildren().stream()
                .filter(item -> item instanceof ActionGroup group && "file.new".equals(group.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(newGroup, "file.new submenu group must be present under File");
        assertEquals("New", newGroup.getText());
    }

    @Test
    public void testFileNewMenuStructureAndOrder() {
        ActionGroup newGroup = actionManager.getGroup("file.new");
        assertNotNull(newGroup, "file.new group should be registered in ActionManager");

        List<AnAction> items = newGroup.getChildren();
        assertFalse(items.isEmpty(), "file.new items should not be empty");

        int idx = 0;

        // 1. Project...
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.project", items.get(idx).getId());
        assertEquals("Project…", ((AnAction) items.get(idx)).getText());
        idx++;

        // Separator 1
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // 2. SQL File
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.sqlfile", items.get(idx).getId());
        assertEquals("SQL File", ((AnAction) items.get(idx)).getText());
        idx++;

        // 3. Scratch File
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.scratch", items.get(idx).getId());
        assertEquals("Scratch File", ((AnAction) items.get(idx)).getText());
        idx++;

        // 4. Query Console
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.console", items.get(idx).getId());
        assertEquals("Query Console", ((AnAction) items.get(idx)).getText());
        idx++;

        // 5. Query File...
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.queryfile", items.get(idx).getId());
        assertEquals("Query File…", ((AnAction) items.get(idx)).getText());
        idx++;

        // Separator 2
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // 6. Database
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.database", items.get(idx).getId());
        assertEquals("Database", ((AnAction) items.get(idx)).getText());
        idx++;

        // 7. Role
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.role", items.get(idx).getId());
        assertEquals("Role", ((AnAction) items.get(idx)).getText());
        idx++;

        // 8. User
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.user", items.get(idx).getId());
        assertEquals("User", ((AnAction) items.get(idx)).getText());
        idx++;

        // Separator 3
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // 9. Virtual View
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.virtualview", items.get(idx).getId());
        assertEquals("Virtual View", ((AnAction) items.get(idx)).getText());
        idx++;

        // Separator 4
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // 10. Data Source >
        assertInstanceOf(ActionGroup.class, items.get(idx));
        assertEquals("file.new.datasource.group", items.get(idx).getId());
        assertEquals("Data Source", ((ActionGroup) items.get(idx)).getText());
        idx++;

        // 11. Data Source from Cloud Provider >
        assertInstanceOf(ActionGroup.class, items.get(idx));
        assertEquals("file.new.datasource.cloud", items.get(idx).getId());
        assertEquals("Data Source from Cloud Provider", ((ActionGroup) items.get(idx)).getText());
        idx++;

        // 12. Data Source Templates
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.datasource.templates", items.get(idx).getId());
        assertEquals("Data Source Templates", ((AnAction) items.get(idx)).getText());
        idx++;

        // 13. Data Source from File/Folder
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.datasource.file", items.get(idx).getId());
        assertEquals("Data Source from File/Folder", ((AnAction) items.get(idx)).getText());
        idx++;

        // 14. Data Source from URL
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.datasource.url", items.get(idx).getId());
        assertEquals("Data Source from URL", ((AnAction) items.get(idx)).getText());
        idx++;

        // 15. DDL Data Source
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.datasource.ddl", items.get(idx).getId());
        assertEquals("DDL Data Source", ((AnAction) items.get(idx)).getText());
        idx++;

        // Separator 5
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // 16. Folder
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.folder", items.get(idx).getId());
        assertEquals("Folder", ((AnAction) items.get(idx)).getText());
        idx++;

        // 17. Driver
        assertInstanceOf(AnAction.class, items.get(idx));
        assertEquals("file.new.driver", items.get(idx).getId());
        assertEquals("Driver", ((AnAction) items.get(idx)).getText());
        idx++;

        assertEquals(idx, items.size(), "All items and separators accounted for");
    }

    @Test
    public void testAccelerators() {
        ActionGroup newGroup = actionManager.getGroup("file.new");

        // Scratch File: Ctrl+Alt+Shift+Insert
        AnAction scratchAction = (AnAction) newGroup.getChildren().stream()
                .filter(i -> "file.new.scratch".equals(i.getId())).findFirst().orElseThrow();
        assertNotNull(scratchAction.getAccelerator());
        KeyCodeCombination scratchAcc = (KeyCodeCombination) scratchAction.getAccelerator();
        assertEquals(KeyCode.INSERT, scratchAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, scratchAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, scratchAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, scratchAcc.getShift());

        // Query Console: Ctrl+Shift+Q
        AnAction consoleAction = (AnAction) newGroup.getChildren().stream()
                .filter(i -> "file.new.console".equals(i.getId())).findFirst().orElseThrow();
        assertNotNull(consoleAction.getAccelerator());
        KeyCodeCombination consoleAcc = (KeyCodeCombination) consoleAction.getAccelerator();
        assertEquals(KeyCode.Q, consoleAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, consoleAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, consoleAcc.getShift());

        // Query File...: Ctrl+Alt+Shift+Q
        AnAction queryFileAction = (AnAction) newGroup.getChildren().stream()
                .filter(i -> "file.new.queryfile".equals(i.getId())).findFirst().orElseThrow();
        assertNotNull(queryFileAction.getAccelerator());
        KeyCodeCombination qfAcc = (KeyCodeCombination) queryFileAction.getAccelerator();
        assertEquals(KeyCode.Q, qfAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, qfAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, qfAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, qfAcc.getShift());
    }

    @Test
    public void testCloudProviderSubmenu() {
        ActionGroup newGroup = actionManager.getGroup("file.new");
        ActionGroup cloudGroup = (ActionGroup) newGroup.getChildren().stream()
                .filter(i -> "file.new.datasource.cloud".equals(i.getId())).findFirst().orElseThrow();

        assertEquals(3, cloudGroup.getChildren().size());
        ActionGroup aws = (ActionGroup) cloudGroup.getChildren().get(0);
        ActionGroup gcp = (ActionGroup) cloudGroup.getChildren().get(1);
        ActionGroup azure = (ActionGroup) cloudGroup.getChildren().get(2);

        assertEquals("Amazon AWS", aws.getText());
        assertEquals("Google Cloud", gcp.getText());
        assertEquals("Microsoft Azure", azure.getText());
    }

    @Test
    public void testDataSourceEnginesPreserved() {
        ActionGroup newGroup = actionManager.getGroup("file.new");
        ActionGroup dsGroup = (ActionGroup) newGroup.getChildren().stream()
                .filter(i -> "file.new.datasource.group".equals(i.getId())).findFirst().orElseThrow();

        List<String> expectedEngines = List.of(
                "MySQL", "MariaDB", "PostgreSQL", "StratosDB", "SQLite", "Oracle", "Microsoft SQL Server", "MongoDB"
        );

        for (String engine : expectedEngines) {
            boolean found = dsGroup.getChildren().stream()
                    .anyMatch(i -> i instanceof AnAction a && engine.equals(a.getText()));
            assertTrue(found, "Data Source menu should retain " + engine);
        }
    }

    @Test
    public void testEditMenuStructureAndOrder() {
        ActionGroup editMenu = actionManager.getGroup("menu.edit");
        assertNotNull(editMenu, "menu.edit group should be registered");
        assertEquals("Edit", editMenu.getText());

        List<AnAction> items = editMenu.getChildren();
        assertEquals(36, items.size(), "Edit menu must contain 29 actions/groups and 7 separators");

        int idx = 0;

        // Section 1: Undo, Redo
        assertEquals("edit.undo", items.get(idx).getId());
        assertEquals("Undo", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.redo", items.get(idx).getId());
        assertEquals("Redo", ((AnAction) items.get(idx++)).getText());

        // Separator 1
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 2: Cut, Copy, Copy as Plain Text, Copy Path/Reference…, Paste, Delete
        assertEquals("edit.cut", items.get(idx).getId());
        assertEquals("Cut", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.copy", items.get(idx).getId());
        assertEquals("Copy", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.copy.plain", items.get(idx).getId());
        assertEquals("Copy as Plain Text", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.copy.reference", items.get(idx).getId());
        assertEquals("Copy Path/Reference…", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.paste", items.get(idx).getId());
        assertEquals("Paste", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.delete", items.get(idx).getId());
        assertEquals("Delete", ((AnAction) items.get(idx++)).getText());

        // Separator 2
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 3: Find…, Replace…, Find in Files…, Replace in Files…, Find Usages
        assertEquals("edit.find", items.get(idx).getId());
        assertEquals("Find…", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.replace", items.get(idx).getId());
        assertEquals("Replace…", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.find.in.files", items.get(idx).getId());
        assertEquals("Find in Files…", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.replace.in.files", items.get(idx).getId());
        assertEquals("Replace in Files…", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.find.usages", items.get(idx).getId());
        assertEquals("Find Usages", ((AnAction) items.get(idx++)).getText());

        // Separator 3
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 4: Generate…, Insert Live Template…, Surround With…
        assertEquals("edit.generate", items.get(idx).getId());
        assertEquals("Generate…", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.insert.live.template", items.get(idx).getId());
        assertEquals("Insert Live Template…", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.surround.with", items.get(idx).getId());
        assertEquals("Surround With…", ((AnAction) items.get(idx++)).getText());

        // Separator 4
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 5: Reformat Code, Reformat File…, Comment with Line Comment, Comment with Block Comment, Auto-Indent Lines
        assertEquals("edit.format.code", items.get(idx).getId());
        assertEquals("Reformat Code", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.format.file", items.get(idx).getId());
        assertEquals("Reformat File…", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.comment.line", items.get(idx).getId());
        assertEquals("Comment with Line Comment", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.comment.block", items.get(idx).getId());
        assertEquals("Comment with Block Comment", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.auto.indent", items.get(idx).getId());
        assertEquals("Auto-Indent Lines", ((AnAction) items.get(idx++)).getText());

        // Separator 5
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 6: Refactor >
        assertInstanceOf(ActionGroup.class, items.get(idx));
        ActionGroup refactorGroup = (ActionGroup) items.get(idx);
        assertEquals("edit.refactor", refactorGroup.getId());
        assertEquals("Refactor", refactorGroup.getText());
        assertEquals(3, refactorGroup.getChildren().size());
        assertEquals("edit.refactor.rename", refactorGroup.getChildren().get(0).getId());
        assertEquals("Rename…", ((AnAction) refactorGroup.getChildren().get(0)).getText());
        assertEquals("edit.refactor.extract.view", refactorGroup.getChildren().get(1).getId());
        assertEquals("Extract View…", ((AnAction) refactorGroup.getChildren().get(1)).getText());
        assertEquals("edit.refactor.extract.subquery", refactorGroup.getChildren().get(2).getId());
        assertEquals("Extract Subquery…", ((AnAction) refactorGroup.getChildren().get(2)).getText());
        idx++;

        // Separator 6
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 7: Selection >, Toggle Case, Join Lines, Duplicate Entire Lines, Sort Lines
        assertInstanceOf(ActionGroup.class, items.get(idx));
        ActionGroup selectionGroup = (ActionGroup) items.get(idx);
        assertEquals("edit.selection", selectionGroup.getId());
        assertEquals("Selection", selectionGroup.getText());
        assertEquals(3, selectionGroup.getChildren().size());
        assertEquals("edit.select.all", selectionGroup.getChildren().get(0).getId());
        assertEquals("edit.selection.extend", selectionGroup.getChildren().get(1).getId());
        assertEquals("edit.selection.shrink", selectionGroup.getChildren().get(2).getId());
        idx++;

        assertEquals("edit.toggle.case", items.get(idx).getId());
        assertEquals("Toggle Case", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.join.lines", items.get(idx).getId());
        assertEquals("Join Lines", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.duplicate.lines", items.get(idx).getId());
        assertEquals("Duplicate Entire Lines", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.sort.lines", items.get(idx).getId());
        assertEquals("Sort Lines", ((AnAction) items.get(idx++)).getText());

        // Separator 7
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 8: Toggle Bookmark, Show Line Bookmarks…
        assertEquals("edit.toggle.bookmark", items.get(idx).getId());
        assertEquals("Toggle Bookmark", ((AnAction) items.get(idx++)).getText());
        assertEquals("edit.show.bookmarks", items.get(idx).getId());
        assertEquals("Show Line Bookmarks…", ((AnAction) items.get(idx++)).getText());

        assertEquals(idx, items.size());
    }

    @Test
    public void testEditMenuAccelerators() {
        ActionGroup editMenu = actionManager.getGroup("menu.edit");

        // Undo: Ctrl+Z
        AnAction undo = (AnAction) actionManager.getAction("edit.undo");
        assertNotNull(undo.getAccelerator());
        KeyCodeCombination undoAcc = (KeyCodeCombination) undo.getAccelerator();
        assertEquals(KeyCode.Z, undoAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, undoAcc.getControl());

        // Redo: Ctrl+Shift+Z
        AnAction redo = (AnAction) actionManager.getAction("edit.redo");
        assertNotNull(redo.getAccelerator());
        KeyCodeCombination redoAcc = (KeyCodeCombination) redo.getAccelerator();
        assertEquals(KeyCode.Z, redoAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, redoAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, redoAcc.getShift());

        // Find Usages: Alt+Shift+7
        AnAction findUsages = (AnAction) actionManager.getAction("edit.find.usages");
        assertNotNull(findUsages.getAccelerator());
        KeyCodeCombination fuAcc = (KeyCodeCombination) findUsages.getAccelerator();
        assertEquals(KeyCode.DIGIT7, fuAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, fuAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, fuAcc.getShift());

        // Toggle Bookmark: F11
        AnAction toggleBookmark = (AnAction) actionManager.getAction("edit.toggle.bookmark");
        assertNotNull(toggleBookmark.getAccelerator());
        KeyCodeCombination bmAcc = (KeyCodeCombination) toggleBookmark.getAccelerator();
        assertEquals(KeyCode.F11, bmAcc.getCode());

        // Show Line Bookmarks: Shift+F11
        AnAction showBookmarks = (AnAction) actionManager.getAction("edit.show.bookmarks");
        assertNotNull(showBookmarks.getAccelerator());
        KeyCodeCombination sbmAcc = (KeyCodeCombination) showBookmarks.getAccelerator();
        assertEquals(KeyCode.F11, sbmAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, sbmAcc.getShift());

        // Toggle Case: Ctrl+Shift+U
        AnAction toggleCase = (AnAction) actionManager.getAction("edit.toggle.case");
        assertNotNull(toggleCase.getAccelerator());
        KeyCodeCombination tcAcc = (KeyCodeCombination) toggleCase.getAccelerator();
        assertEquals(KeyCode.U, tcAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, tcAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, tcAcc.getShift());
    }

    @Test
    public void testViewMenuStructureAndOrder() {
        ActionGroup viewMenu = actionManager.getGroup("menu.view");
        assertNotNull(viewMenu, "menu.view should be registered");
        assertEquals("View", viewMenu.getText());

        List<AnAction> items = viewMenu.getChildren();
        assertEquals(11, items.size(), "View menu must contain 9 actions/groups and 2 separators");

        int idx = 0;

        // 1. Tool Windows >
        assertInstanceOf(ActionGroup.class, items.get(idx));
        assertEquals("view.toolwindows", items.get(idx).getId());
        assertEquals("Tool Windows", ((ActionGroup) items.get(idx++)).getText());

        // 2. Appearance >
        assertInstanceOf(ActionGroup.class, items.get(idx));
        assertEquals("view.appearance", items.get(idx).getId());
        assertEquals("Appearance", ((ActionGroup) items.get(idx++)).getText());

        // Separator 1
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // 3. Recent Locations (Ctrl+Shift+E)
        assertEquals("view.recent.locations", items.get(idx).getId());
        assertEquals("Recent Locations", ((AnAction) items.get(idx++)).getText());

        // 4. Recent Files (Ctrl+E)
        assertEquals("view.recent.files", items.get(idx).getId());
        assertEquals("Recent Files", ((AnAction) items.get(idx++)).getText());

        // 5. Recently Changed Files
        assertEquals("view.recent.changed.files", items.get(idx).getId());
        assertEquals("Recently Changed Files", ((AnAction) items.get(idx++)).getText());

        // 6. Recent Changes (Alt+Shift+C)
        assertEquals("view.recent.changes", items.get(idx).getId());
        assertEquals("Recent Changes", ((AnAction) items.get(idx++)).getText());

        // Separator 2
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // 7. Increase Font Size in All Editors (Alt+Shift+.)
        assertEquals("view.font.increase", items.get(idx).getId());
        assertEquals("Increase Font Size in All Editors", ((AnAction) items.get(idx++)).getText());

        // 8. Decrease Font Size in All Editors (Alt+Shift+,)
        assertEquals("view.font.decrease", items.get(idx).getId());
        assertEquals("Decrease Font Size in All Editors", ((AnAction) items.get(idx++)).getText());

        // 9. Reset Font Size in All Editors
        assertEquals("view.font.reset", items.get(idx).getId());
        assertEquals("Reset Font Size in All Editors", ((AnAction) items.get(idx++)).getText());

        assertEquals(idx, items.size());
    }

    @Test
    public void testToolWindowsSubmenu() {
        ActionGroup twGroup = actionManager.getGroup("view.toolwindows");
        assertNotNull(twGroup, "view.toolwindows group must be registered");
        assertEquals("Tool Windows", twGroup.getText());

        List<AnAction> items = twGroup.getChildren();
        assertEquals(21, items.size(), "Tool Windows must have 20 tool windows + 1 separator");

        int idx = 0;
        assertEquals("view.toolwindow.commit", items.get(idx).getId());
        assertEquals("Commit", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.database", items.get(idx).getId());
        assertEquals("Database Explorer", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.files", items.get(idx).getId());
        assertEquals("Files", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.find", items.get(idx).getId());
        assertEquals("Find", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toggle.run", items.get(idx).getId());
        assertEquals("Run", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.debug", items.get(idx).getId());
        assertEquals("Debug", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.problems", items.get(idx).getId());
        assertEquals("Problems", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.structure", items.get(idx).getId());
        assertEquals("Structure", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.services", items.get(idx).getId());
        assertEquals("Services", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.vcs", items.get(idx).getId());
        assertEquals("Version Control", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.ai", items.get(idx).getId());
        assertEquals("AI Assistant", ((AnAction) items.get(idx++)).getText());

        // Separator
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        assertEquals("view.toolwindow.backup.sync", items.get(idx).getId());
        assertEquals("Backup and Sync History", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.bookmarks", items.get(idx).getId());
        assertEquals("Bookmarks", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.coverage", items.get(idx).getId());
        assertEquals("Coverage", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.dbchanges", items.get(idx).getId());
        assertEquals("Database Changes", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.hierarchy", items.get(idx).getId());
        assertEquals("Hierarchy", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.learn", items.get(idx).getId());
        assertEquals("Learn", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.notifications", items.get(idx).getId());
        assertEquals("Notifications", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.terminal", items.get(idx).getId());
        assertEquals("Terminal", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.toolwindow.todo", items.get(idx).getId());
        assertEquals("TODO", ((AnAction) items.get(idx++)).getText());

        assertEquals(idx, items.size());
    }

    @Test
    public void testAppearanceSubmenu() {
        ActionGroup appGroup = actionManager.getGroup("view.appearance");
        assertNotNull(appGroup, "view.appearance group must be registered");
        assertEquals("Appearance", appGroup.getText());

        List<AnAction> items = appGroup.getChildren();
        assertEquals(16, items.size(), "Appearance must contain modes, toggles, submenus, and 3 separators");

        int idx = 0;
        assertEquals("view.appearance.presentation.mode", items.get(idx).getId());
        assertEquals("Enter Presentation Mode", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.appearance.distraction.free", items.get(idx).getId());
        assertEquals("Enter Distraction Free Mode", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.appearance.full.screen", items.get(idx).getId());
        assertEquals("Enter Full Screen", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.appearance.zen.mode", items.get(idx).getId());
        assertEquals("Enter Zen Mode", ((AnAction) items.get(idx++)).getText());

        // Separator 1
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        assertEquals("view.appearance.compact.mode", items.get(idx).getId());
        assertEquals("Compact Mode", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.appearance.zoom.ide", items.get(idx).getId());
        assertEquals("Zoom IDE (Current: 100%)…", ((AnAction) items.get(idx++)).getText());

        assertEquals("view.appearance.presentation.assistant", items.get(idx).getId());
        assertEquals("Presentation Assistant", ((AnAction) items.get(idx++)).getText());

        // Separator 2
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        assertEquals("view.appearance.mainmenu", items.get(idx).getId());
        assertEquals("Main Menu", ((ActionGroup) items.get(idx++)).getText());

        assertInstanceOf(ToggleAction.class, items.get(idx));
        assertEquals("view.appearance.toolbar", items.get(idx).getId());
        assertEquals("Toolbar", ((ToggleAction) items.get(idx++)).getText());

        assertEquals("view.appearance.navbar", items.get(idx).getId());
        assertEquals("Navigation Bar", ((ActionGroup) items.get(idx++)).getText());

        assertEquals("view.appearance.toolwindowbars", items.get(idx).getId());
        assertEquals("Tool Window Bars", ((AnAction) items.get(idx++)).getText());

        // Separator 3
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        assertInstanceOf(ToggleAction.class, items.get(idx));
        assertEquals("view.appearance.statusbar", items.get(idx).getId());
        assertEquals("Status Bar", ((ToggleAction) items.get(idx++)).getText());

        assertEquals("view.appearance.statusbar.widgets", items.get(idx).getId());
        assertEquals("Status Bar Widgets", ((ActionGroup) items.get(idx++)).getText());

        assertEquals(idx, items.size());
    }

    @Test
    public void testMainMenuSubmenu() {
        ActionGroup mmGroup = actionManager.getGroup("view.appearance.mainmenu");
        assertNotNull(mmGroup, "view.appearance.mainmenu should be registered");

        List<AnAction> items = mmGroup.getChildren();
        assertEquals(3, items.size());
        assertEquals("Hide under Hamburger Button", ((AnAction) items.get(0)).getText());
        assertEquals("Merge with Main Toolbar", ((AnAction) items.get(1)).getText());
        assertEquals("Show above Main Toolbar", ((AnAction) items.get(2)).getText());
    }

    @Test
    public void testStatusBarWidgetsSubmenu() {
        ActionGroup sbGroup = actionManager.getGroup("view.appearance.statusbar.widgets");
        assertNotNull(sbGroup, "view.appearance.statusbar.widgets should be registered");

        List<AnAction> items = sbGroup.getChildren();
        assertEquals(20, items.size(), "Status Bar Widgets must have 17 widgets and 3 separators");

        int idx = 0;
        // Section 1
        assertEquals("Status Text", ((AnAction) items.get(idx++)).getText());
        assertEquals("File System Sync", ((AnAction) items.get(idx++)).getText());
        assertEquals("Remote Development Wire Stats", ((AnAction) items.get(idx++)).getText());
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 2
        assertEquals("Aggregator", ((AnAction) items.get(idx++)).getText());
        assertEquals("Grid Position", ((AnAction) items.get(idx++)).getText());
        assertEquals("Line:Column Number", ((AnAction) items.get(idx++)).getText());
        assertEquals("Language Services", ((AnAction) items.get(idx++)).getText());
        assertEquals("Line Separator", ((AnAction) items.get(idx++)).getText());
        assertEquals("File Encoding", ((AnAction) items.get(idx++)).getText());
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 3
        assertEquals("Power Save Mode", ((AnAction) items.get(idx++)).getText());
        assertEquals("Editor Selection Mode", ((AnAction) items.get(idx++)).getText());
        assertEquals("Indentation", ((AnAction) items.get(idx++)).getText());
        assertEquals("JSON Schema", ((AnAction) items.get(idx++)).getText());
        assertEquals("MCP Server", ((AnAction) items.get(idx++)).getText());
        assertEquals("Read-Only Attribute", ((AnAction) items.get(idx++)).getText());
        assertEquals("Notifications", ((AnAction) items.get(idx++)).getText());
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 4
        assertEquals("Memory Indicator", ((AnAction) items.get(idx++)).getText());

        assertEquals(idx, items.size());
    }

    @Test
    public void testViewAccelerators() {
        // Recent Locations: Ctrl+Shift+E
        AnAction recentLoc = actionManager.getAction("view.recent.locations");
        assertNotNull(recentLoc.getAccelerator());
        KeyCodeCombination rlAcc = (KeyCodeCombination) recentLoc.getAccelerator();
        assertEquals(KeyCode.E, rlAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, rlAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, rlAcc.getShift());

        // Recent Files: Ctrl+E
        AnAction recentFiles = actionManager.getAction("view.recent.files");
        assertNotNull(recentFiles.getAccelerator());
        KeyCodeCombination rfAcc = (KeyCodeCombination) recentFiles.getAccelerator();
        assertEquals(KeyCode.E, rfAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, rfAcc.getControl());

        // Recent Changes: Alt+Shift+C
        AnAction recentChanges = actionManager.getAction("view.recent.changes");
        assertNotNull(recentChanges.getAccelerator());
        KeyCodeCombination rcAcc = (KeyCodeCombination) recentChanges.getAccelerator();
        assertEquals(KeyCode.C, rcAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, rcAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, rcAcc.getShift());

        // Increase Font Size: Alt+Shift+.
        AnAction incFont = actionManager.getAction("view.font.increase");
        assertNotNull(incFont.getAccelerator());
        KeyCodeCombination ifAcc = (KeyCodeCombination) incFont.getAccelerator();
        assertEquals(KeyCode.PERIOD, ifAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, ifAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, ifAcc.getShift());

        // Decrease Font Size: Alt+Shift+,
        AnAction decFont = actionManager.getAction("view.font.decrease");
        assertNotNull(decFont.getAccelerator());
        KeyCodeCombination dfAcc = (KeyCodeCombination) decFont.getAccelerator();
        assertEquals(KeyCode.COMMA, dfAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, dfAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, dfAcc.getShift());

        // Tool Windows: Database Explorer (Alt+1), Run (Alt+4), Terminal (Alt+F12)
        AnAction twDb = actionManager.getAction("view.toolwindow.database");
        assertEquals(KeyCode.DIGIT1, ((KeyCodeCombination) twDb.getAccelerator()).getCode());

        AnAction twRun = actionManager.getAction("view.toggle.run");
        assertEquals(KeyCode.DIGIT4, ((KeyCodeCombination) twRun.getAccelerator()).getCode());

        AnAction twTerm = actionManager.getAction("view.toolwindow.terminal");
        assertEquals(KeyCode.F12, ((KeyCodeCombination) twTerm.getAccelerator()).getCode());
    }

    @Test
    public void testExistingActionsPreserved() {
        AnAction refresh = actionManager.getAction("view.refresh.explorer");
        assertNotNull(refresh, "view.refresh.explorer must remain registered in ActionManager");
        assertEquals("Refresh Database Explorer", refresh.getText());
        assertEquals(KeyCode.F5, ((KeyCodeCombination) refresh.getAccelerator()).getCode());

        AnAction run = actionManager.getAction("view.toggle.run");
        assertNotNull(run, "view.toggle.run must remain registered in ActionManager");
        assertEquals("Run", run.getText());
        assertEquals(KeyCode.DIGIT4, ((KeyCodeCombination) run.getAccelerator()).getCode());
    }
}

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

    @Test
    public void testNavigateMenuStructureAndOrder() {
        ActionGroup navMenu = actionManager.getGroup("menu.navigate");
        assertNotNull(navMenu, "menu.navigate group must be registered");
        assertEquals("Navigate", navMenu.getText());

        List<AnAction> items = navMenu.getChildren();
        assertEquals(22, items.size(), "Navigate menu must have 16 actions and 6 separators");

        int idx = 0;

        // Section 1: Back, Forward
        assertEquals("navigate.back", items.get(idx).getId());
        assertEquals("Back", ((AnAction) items.get(idx++)).getText());
        assertEquals("navigate.forward", items.get(idx).getId());
        assertEquals("Forward", ((AnAction) items.get(idx++)).getText());

        // Separator 1
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 2: Search Everywhere, Database Object..., File..., Code..., Text...
        assertEquals("navigate.search.everywhere", items.get(idx).getId());
        assertEquals("Search Everywhere", ((AnAction) items.get(idx++)).getText());
        assertEquals("navigate.database.object", items.get(idx).getId());
        assertEquals("Database Object…", ((AnAction) items.get(idx++)).getText());
        assertEquals("navigate.file", items.get(idx).getId());
        assertEquals("File…", ((AnAction) items.get(idx++)).getText());
        assertEquals("navigate.code", items.get(idx).getId());
        assertEquals("Code…", ((AnAction) items.get(idx++)).getText());
        assertEquals("navigate.text", items.get(idx).getId());
        assertEquals("Text…", ((AnAction) items.get(idx++)).getText());

        // Separator 2
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 3: Show Diagram...
        assertEquals("navigate.show.diagram", items.get(idx).getId());
        assertEquals("Show Diagram…", ((AnAction) items.get(idx++)).getText());

        // Separator 3
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 4: Jump to Query Console..., Select In..., Declaration or Usages
        assertEquals("navigate.jump.query.console", items.get(idx).getId());
        assertEquals("Jump to Query Console…", ((AnAction) items.get(idx++)).getText());
        assertEquals("navigate.select.in", items.get(idx).getId());
        assertEquals("Select In…", ((AnAction) items.get(idx++)).getText());
        assertEquals("navigate.declaration.usages", items.get(idx).getId());
        assertEquals("Declaration or Usages", ((AnAction) items.get(idx++)).getText());

        // Separator 4
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 5: Scroll from Editor
        assertEquals("navigate.scroll.from.editor", items.get(idx).getId());
        assertEquals("Scroll from Editor", ((AnAction) items.get(idx++)).getText());

        // Separator 5
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 6: Jump to Navigation Bar, File Path
        assertEquals("navigate.jump.navbar", items.get(idx).getId());
        assertEquals("Jump to Navigation Bar", ((AnAction) items.get(idx++)).getText());
        assertEquals("navigate.file.path", items.get(idx).getId());
        assertEquals("File Path", ((AnAction) items.get(idx++)).getText());

        // Separator 6
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 7: Next Statement, Previous Statement
        assertEquals("navigate.next.statement", items.get(idx).getId());
        assertEquals("Next Statement", ((AnAction) items.get(idx++)).getText());
        assertEquals("navigate.prev.statement", items.get(idx).getId());
        assertEquals("Previous Statement", ((AnAction) items.get(idx++)).getText());

        assertEquals(idx, items.size());
    }

    @Test
    public void testNavigateAccelerators() {
        // Back: Alt+Shift+Left
        AnAction back = actionManager.getAction("navigate.back");
        assertNotNull(back.getAccelerator());
        KeyCodeCombination backAcc = (KeyCodeCombination) back.getAccelerator();
        assertEquals(KeyCode.LEFT, backAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, backAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, backAcc.getShift());

        // Forward: Alt+Shift+Right
        AnAction forward = actionManager.getAction("navigate.forward");
        assertNotNull(forward.getAccelerator());
        KeyCodeCombination fwdAcc = (KeyCodeCombination) forward.getAccelerator();
        assertEquals(KeyCode.RIGHT, fwdAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, fwdAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, fwdAcc.getShift());

        // Database Object: Ctrl+N
        AnAction dbObj = actionManager.getAction("navigate.database.object");
        assertNotNull(dbObj.getAccelerator());
        assertEquals(KeyCode.N, ((KeyCodeCombination) dbObj.getAccelerator()).getCode());

        // File: Ctrl+Shift+N
        AnAction file = actionManager.getAction("navigate.file");
        assertNotNull(file.getAccelerator());
        KeyCodeCombination fileAcc = (KeyCodeCombination) file.getAccelerator();
        assertEquals(KeyCode.N, fileAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, fileAcc.getShift());

        // Code: Ctrl+Alt+Shift+N
        AnAction code = actionManager.getAction("navigate.code");
        assertNotNull(code.getAccelerator());
        KeyCodeCombination codeAcc = (KeyCodeCombination) code.getAccelerator();
        assertEquals(KeyCode.N, codeAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, codeAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, codeAcc.getShift());

        // Text: Ctrl+Alt+Shift+E
        AnAction text = actionManager.getAction("navigate.text");
        assertNotNull(text.getAccelerator());
        KeyCodeCombination textAcc = (KeyCodeCombination) text.getAccelerator();
        assertEquals(KeyCode.E, textAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, textAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, textAcc.getShift());

        // Show Diagram: Ctrl+Alt+Shift+U
        AnAction diagram = actionManager.getAction("navigate.show.diagram");
        assertNotNull(diagram.getAccelerator());
        KeyCodeCombination diagAcc = (KeyCodeCombination) diagram.getAccelerator();
        assertEquals(KeyCode.U, diagAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, diagAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, diagAcc.getShift());

        // Jump to Query Console: Ctrl+Shift+F10
        AnAction jumpConsole = actionManager.getAction("navigate.jump.query.console");
        assertNotNull(jumpConsole.getAccelerator());
        KeyCodeCombination jcAcc = (KeyCodeCombination) jumpConsole.getAccelerator();
        assertEquals(KeyCode.F10, jcAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, jcAcc.getShift());

        // Select In: Alt+Shift+1
        AnAction selectIn = actionManager.getAction("navigate.select.in");
        assertNotNull(selectIn.getAccelerator());
        KeyCodeCombination siAcc = (KeyCodeCombination) selectIn.getAccelerator();
        assertEquals(KeyCode.DIGIT1, siAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, siAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, siAcc.getShift());

        // Declaration or Usages: Ctrl+B
        AnAction decl = actionManager.getAction("navigate.declaration.usages");
        assertNotNull(decl.getAccelerator());
        KeyCodeCombination declAcc = (KeyCodeCombination) decl.getAccelerator();
        assertEquals(KeyCode.B, declAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, declAcc.getControl());

        // Jump to Navigation Bar: Alt+Home
        AnAction jumpNav = actionManager.getAction("navigate.jump.navbar");
        assertNotNull(jumpNav.getAccelerator());
        KeyCodeCombination jnAcc = (KeyCodeCombination) jumpNav.getAccelerator();
        assertEquals(KeyCode.HOME, jnAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, jnAcc.getAlt());

        // File Path: Ctrl+Alt+Shift+2
        AnAction filePath = actionManager.getAction("navigate.file.path");
        assertNotNull(filePath.getAccelerator());
        KeyCodeCombination fpAcc = (KeyCodeCombination) filePath.getAccelerator();
        assertEquals(KeyCode.DIGIT2, fpAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, fpAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, fpAcc.getShift());

        // Next Statement: Alt+Down
        AnAction nextStmt = actionManager.getAction("navigate.next.statement");
        assertNotNull(nextStmt.getAccelerator());
        KeyCodeCombination nsAcc = (KeyCodeCombination) nextStmt.getAccelerator();
        assertEquals(KeyCode.DOWN, nsAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, nsAcc.getAlt());

        // Previous Statement: Alt+Up
        AnAction prevStmt = actionManager.getAction("navigate.prev.statement");
        assertNotNull(prevStmt.getAccelerator());
        KeyCodeCombination psAcc = (KeyCodeCombination) prevStmt.getAccelerator();
        assertEquals(KeyCode.UP, psAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, psAcc.getAlt());
    }

    @Test
    public void testExistingTabNavigationPreserved() {
        AnAction nextTab = actionManager.getAction("navigate.next.tab");
        assertNotNull(nextTab, "navigate.next.tab must remain registered");
        assertEquals("Select Next Tab", nextTab.getText());
        assertEquals(KeyCode.RIGHT, ((KeyCodeCombination) nextTab.getAccelerator()).getCode());

        AnAction prevTab = actionManager.getAction("navigate.prev.tab");
        assertNotNull(prevTab, "navigate.prev.tab must remain registered");
        assertEquals("Select Previous Tab", prevTab.getText());
        assertEquals(KeyCode.LEFT, ((KeyCodeCombination) prevTab.getAccelerator()).getCode());
    }

    @Test
    public void testRunMenuRegistration() {
        ActionGroup runMenu = actionManager.getGroup("menu.run");
        assertNotNull(runMenu, "menu.run group should be registered");
        assertEquals("Run", runMenu.getText());
    }

    @Test
    public void testRunMenuStructureAndOrder() {
        ActionGroup runMenu = actionManager.getGroup("menu.run");
        assertNotNull(runMenu, "menu.run group should be registered");

        List<AnAction> items = runMenu.getChildren();
        assertEquals(6, items.size(), "Run menu should have 4 actions and 2 separators");

        // Section 1: Edit Configurations…
        assertInstanceOf(AnAction.class, items.get(0));
        assertEquals("run.edit.configurations", items.get(0).getId());
        assertEquals("Edit Configurations…", items.get(0).getText());

        // Separator 1
        assertInstanceOf(ActionSeparator.class, items.get(1));

        // Section 2: Compare Data & Compare Schema Structure
        assertInstanceOf(AnAction.class, items.get(2));
        assertEquals("run.compare.data", items.get(2).getId());
        assertEquals("Compare Data", items.get(2).getText());

        assertInstanceOf(AnAction.class, items.get(3));
        assertEquals("run.compare.schema", items.get(3).getId());
        assertEquals("Compare Schema Structure", items.get(3).getText());

        // Separator 2
        assertInstanceOf(ActionSeparator.class, items.get(4));

        // Section 3: Full-Text Search…
        assertInstanceOf(AnAction.class, items.get(5));
        assertEquals("run.fulltext.search", items.get(5).getId());
        assertEquals("Full-Text Search…", items.get(5).getText());
    }

    @Test
    public void testRunMenuAccelerators() {
        // Compare Schema Structure: Ctrl+D
        AnAction compareSchema = actionManager.getAction("run.compare.schema");
        assertNotNull(compareSchema, "run.compare.schema should be registered");
        assertNotNull(compareSchema.getAccelerator());
        KeyCodeCombination csAcc = (KeyCodeCombination) compareSchema.getAccelerator();
        assertEquals(KeyCode.D, csAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, csAcc.getControl());

        // Full-Text Search…: Ctrl+Alt+Shift+F
        AnAction fullTextSearch = actionManager.getAction("run.fulltext.search");
        assertNotNull(fullTextSearch, "run.fulltext.search should be registered");
        assertNotNull(fullTextSearch.getAccelerator());
        KeyCodeCombination ftsAcc = (KeyCodeCombination) fullTextSearch.getAccelerator();
        assertEquals(KeyCode.F, ftsAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, ftsAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, ftsAcc.getAlt());
        assertEquals(KeyCombination.ModifierValue.DOWN, ftsAcc.getShift());
    }

    @Test
    public void testRunExecutionActionsPreserved() {
        // run.statement: Ctrl+Enter
        AnAction runStatement = actionManager.getAction("run.statement");
        assertNotNull(runStatement, "run.statement must remain registered");
        assertEquals("Execute Statement", runStatement.getText());
        assertNotNull(runStatement.getAccelerator());
        KeyCodeCombination rsAcc = (KeyCodeCombination) runStatement.getAccelerator();
        assertEquals(KeyCode.ENTER, rsAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, rsAcc.getControl());

        // run.tool.window: Alt+4
        AnAction runTool = actionManager.getAction("run.tool.window");
        assertNotNull(runTool, "run.tool.window must remain registered");
        assertEquals("Run Tool Window", runTool.getText());
        assertNotNull(runTool.getAccelerator());
        KeyCodeCombination rtAcc = (KeyCodeCombination) runTool.getAccelerator();
        assertEquals(KeyCode.DIGIT4, rtAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, rtAcc.getAlt());
    }

    @Test
    public void testVcsMenuRegistration() {
        ActionGroup vcsMenu = actionManager.getGroup("menu.vcs");
        assertNotNull(vcsMenu, "menu.vcs group should be registered");
        assertEquals("VCS", vcsMenu.getText());
    }

    @Test
    public void testVcsMenuStructureAndOrder() {
        ActionGroup vcsMenu = actionManager.getGroup("menu.vcs");
        assertNotNull(vcsMenu, "menu.vcs group should be registered");

        List<AnAction> items = vcsMenu.getChildren();
        assertEquals(9, items.size(), "VCS menu should contain 9 entries (6 actions, 1 submenu group, 2 separators)");

        // Section 1: Integration & Operations Popup
        assertInstanceOf(AnAction.class, items.get(0));
        assertEquals("vcs.enable.integration", items.get(0).getId());
        assertEquals("Enable Version Control Integration…", items.get(0).getText());

        assertInstanceOf(AnAction.class, items.get(1));
        assertEquals("vcs.operations.popup", items.get(1).getId());
        assertEquals("VCS Operations Popup…", items.get(1).getText());

        // Separator 1
        assertInstanceOf(ActionSeparator.class, items.get(2));

        // Section 2: Patches
        assertInstanceOf(AnAction.class, items.get(3));
        assertEquals("vcs.apply.patch", items.get(3).getId());
        assertEquals("Apply Patch…", items.get(3).getText());

        assertInstanceOf(AnAction.class, items.get(4));
        assertEquals("vcs.apply.patch.clipboard", items.get(4).getId());
        assertEquals("Apply Patch from Clipboard…", items.get(4).getText());

        // Separator 2
        assertInstanceOf(ActionSeparator.class, items.get(5));

        // Section 3: VCS Checkout, Browse, Init
        assertInstanceOf(AnAction.class, items.get(6));
        assertEquals("vcs.get.from.vcs", items.get(6).getId());
        assertEquals("Get from Version Control…", items.get(6).getText());

        assertInstanceOf(ActionGroup.class, items.get(7));
        assertEquals("vcs.browse.repository", items.get(7).getId());
        assertEquals("Browse VCS Repository", items.get(7).getText());
        assertTrue(((ActionGroup) items.get(7)).isPopup());

        assertInstanceOf(AnAction.class, items.get(8));
        assertEquals("vcs.create.git.repository", items.get(8).getId());
        assertEquals("Create Git Repository…", items.get(8).getText());
    }

    @Test
    public void testVcsBrowseRepositorySubmenu() {
        ActionGroup browseRepo = actionManager.getGroup("vcs.browse.repository");
        assertNotNull(browseRepo, "vcs.browse.repository submenu group should be registered");
        assertTrue(browseRepo.isPopup());

        List<AnAction> children = browseRepo.getChildren();
        assertEquals(1, children.size(), "Browse VCS Repository should have 1 child");

        assertInstanceOf(AnAction.class, children.get(0));
        assertEquals("vcs.browse.git.log", children.get(0).getId());
        assertEquals("Show Git Repository Log…", children.get(0).getText());
    }

    @Test
    public void testVcsOperationsAccelerator() {
        AnAction vcsOps = actionManager.getAction("vcs.operations.popup");
        assertNotNull(vcsOps, "vcs.operations.popup should be registered");
        assertNotNull(vcsOps.getAccelerator());

        KeyCodeCombination acc = (KeyCodeCombination) vcsOps.getAccelerator();
        assertEquals(KeyCode.BACK_QUOTE, acc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, acc.getAlt());
    }

    @Test
    public void testLocalHistoryPreserved() {
        ActionGroup localHistory = actionManager.getGroup("file.local.history");
        assertNotNull(localHistory, "file.local.history should remain registered");
        assertEquals("Local History", localHistory.getText());

        assertNotNull(actionManager.getAction("history.show"));
        assertNotNull(actionManager.getAction("history.show.selection"));
        assertNotNull(actionManager.getAction("history.show.project"));
        assertNotNull(actionManager.getAction("history.recent.changes"));
        assertNotNull(actionManager.getAction("history.put.label"));
    }

    @Test
    public void testWindowMenuRegistration() {
        ActionGroup windowMenu = actionManager.getGroup("menu.window");
        assertNotNull(windowMenu, "menu.window group should be registered");
        assertEquals("Window", windowMenu.getText());
    }

    @Test
    public void testWindowMenuStructureAndOrder() {
        ActionGroup windowMenu = actionManager.getGroup("menu.window");
        assertNotNull(windowMenu, "menu.window group should be registered");

        List<AnAction> items = windowMenu.getChildren();
        assertEquals(10, items.size(), "Window menu should contain 10 entries (5 submenus, 2 separators, 3 actions)");

        // 5 Submenus
        assertInstanceOf(ActionGroup.class, items.get(0));
        assertEquals("window.layouts", items.get(0).getId());
        assertEquals("Layouts", items.get(0).getText());

        assertInstanceOf(ActionGroup.class, items.get(1));
        assertEquals("window.active.tool.window", items.get(1).getId());
        assertEquals("Active Tool Window", items.get(1).getText());

        assertInstanceOf(ActionGroup.class, items.get(2));
        assertEquals("window.editor.tabs", items.get(2).getId());
        assertEquals("Editor Tabs", items.get(2).getText());

        assertInstanceOf(ActionGroup.class, items.get(3));
        assertEquals("window.notifications", items.get(3).getId());
        assertEquals("Notifications", items.get(3).getText());

        assertInstanceOf(ActionGroup.class, items.get(4));
        assertEquals("window.processes", items.get(4).getId());
        assertEquals("Processes", items.get(4).getText());

        // Separator 1
        assertInstanceOf(ActionSeparator.class, items.get(5));

        // Next & Previous Project Window
        assertInstanceOf(AnAction.class, items.get(6));
        assertEquals("window.next.project", items.get(6).getId());
        assertEquals("Next Project Window", items.get(6).getText());

        assertInstanceOf(AnAction.class, items.get(7));
        assertEquals("window.prev.project", items.get(7).getId());
        assertEquals("Previous Project Window", items.get(7).getText());

        // Separator 2
        assertInstanceOf(ActionSeparator.class, items.get(8));

        // Active project indicator
        assertInstanceOf(AnAction.class, items.get(9));
        assertEquals("window.project.active", items.get(9).getId());
        assertEquals("default", items.get(9).getText());
    }

    @Test
    public void testWindowLayoutsSubmenu() {
        ActionGroup layouts = actionManager.getGroup("window.layouts");
        assertNotNull(layouts);
        assertTrue(layouts.isPopup());

        List<AnAction> items = layouts.getChildren();
        assertEquals(4, items.size());
        assertEquals("window.layouts.default", items.get(0).getId());
        assertEquals("window.layouts.custom", items.get(1).getId());
        assertInstanceOf(ActionSeparator.class, items.get(2));
        assertEquals("window.layouts.save.as.new", items.get(3).getId());
    }

    @Test
    public void testWindowActiveToolWindowSubmenu() {
        ActionGroup activeTw = actionManager.getGroup("window.active.tool.window");
        assertNotNull(activeTw);
        assertTrue(activeTw.isPopup());

        // Hide Active Tool Window: Shift+Escape
        AnAction hideActive = actionManager.getAction("window.toolwindow.hide.active");
        assertNotNull(hideActive);
        KeyCodeCombination haAcc = (KeyCodeCombination) hideActive.getAccelerator();
        assertEquals(KeyCode.ESCAPE, haAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, haAcc.getShift());

        // Hide All Windows: Ctrl+Shift+F12
        AnAction hideAll = actionManager.getAction("window.toolwindow.hide.all");
        assertNotNull(hideAll);
        KeyCodeCombination hallAcc = (KeyCodeCombination) hideAll.getAccelerator();
        assertEquals(KeyCode.F12, hallAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, hallAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, hallAcc.getShift());

        // Jump to Last Tool Window: F12
        AnAction jumpLast = actionManager.getAction("window.toolwindow.jump.last");
        assertNotNull(jumpLast);
        KeyCodeCombination jlAcc = (KeyCodeCombination) jumpLast.getAccelerator();
        assertEquals(KeyCode.F12, jlAcc.getCode());

        // Maximize Tool Window: Ctrl+Shift+Quote
        AnAction maxTw = actionManager.getAction("window.toolwindow.maximize");
        assertNotNull(maxTw);
        KeyCodeCombination maxAcc = (KeyCodeCombination) maxTw.getAccelerator();
        assertEquals(KeyCode.QUOTE, maxAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, maxAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, maxAcc.getShift());

        // Close Active Tab (tool window): Ctrl+Shift+F4
        AnAction closeTwTab = actionManager.getAction("window.toolwindow.close.active.tab");
        assertNotNull(closeTwTab);
        KeyCodeCombination cttAcc = (KeyCodeCombination) closeTwTab.getAccelerator();
        assertEquals(KeyCode.F4, cttAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, cttAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, cttAcc.getShift());

        // Child submenus: View Mode, Move to, Resize
        assertNotNull(actionManager.getGroup("window.toolwindow.view.mode"));
        assertNotNull(actionManager.getGroup("window.toolwindow.move.to"));
        assertNotNull(actionManager.getGroup("window.toolwindow.resize"));
        assertNotNull(actionManager.getAction("window.toolwindow.group.tabs"));
    }

    @Test
    public void testWindowEditorTabsSubmenu() {
        ActionGroup editorTabs = actionManager.getGroup("window.editor.tabs");
        assertNotNull(editorTabs);
        assertTrue(editorTabs.isPopup());

        // Close Tab: Ctrl+F4
        AnAction closeTab = actionManager.getAction("window.close.tab");
        assertNotNull(closeTab);
        assertEquals("Close Tab", closeTab.getText());
        KeyCodeCombination ctAcc = (KeyCodeCombination) closeTab.getAccelerator();
        assertEquals(KeyCode.F4, ctAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, ctAcc.getControl());

        // Split with Chooser Navigation submenu
        ActionGroup splitChooser = actionManager.getGroup("window.editor.split.chooser");
        assertNotNull(splitChooser);
        assertEquals(2, splitChooser.getChildren().size());
        assertEquals("window.split.right", splitChooser.getChildren().get(0).getId());
        assertEquals("window.split.down", splitChooser.getChildren().get(1).getId());

        // Other editor tab actions
        assertNotNull(actionManager.getAction("window.close.other.tabs"));
        assertNotNull(actionManager.getAction("window.close.all.tabs"));
        assertNotNull(actionManager.getAction("window.close.unmodified.tabs"));
        assertNotNull(actionManager.getAction("window.close.all.but.pinned"));
        assertNotNull(actionManager.getAction("window.close.tabs.left"));
        assertNotNull(actionManager.getAction("window.close.tabs.right"));
        assertNotNull(actionManager.getAction("window.close.all.readonly"));
        assertNotNull(actionManager.getAction("window.editor.unsplit"));
        assertNotNull(actionManager.getAction("window.unsplit.all"));
        assertNotNull(actionManager.getAction("window.editor.configure.tabs"));
    }

    @Test
    public void testWindowNotificationsAndProcessesSubmenus() {
        ActionGroup notifications = actionManager.getGroup("window.notifications");
        assertNotNull(notifications);
        assertEquals(2, notifications.getChildren().size());
        assertEquals("window.notifications.close.first", notifications.getChildren().get(0).getId());
        assertEquals("window.notifications.close.all", notifications.getChildren().get(1).getId());

        ActionGroup processes = actionManager.getGroup("window.processes");
        assertNotNull(processes);
        assertEquals(2, processes.getChildren().size());
        assertEquals("window.processes.show", processes.getChildren().get(0).getId());
        assertEquals("window.processes.auto.show", processes.getChildren().get(1).getId());
    }

    @Test
    public void testWindowProjectAccelerators() {
        AnAction nextProj = actionManager.getAction("window.next.project");
        assertNotNull(nextProj);
        KeyCodeCombination npAcc = (KeyCodeCombination) nextProj.getAccelerator();
        assertEquals(KeyCode.CLOSE_BRACKET, npAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, npAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, npAcc.getAlt());

        AnAction prevProj = actionManager.getAction("window.prev.project");
        assertNotNull(prevProj);
        KeyCodeCombination ppAcc = (KeyCodeCombination) prevProj.getAccelerator();
        assertEquals(KeyCode.OPEN_BRACKET, ppAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, ppAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, ppAcc.getAlt());

        // Reopen Closed Tab preserved: Ctrl+Shift+T
        AnAction reopenTab = actionManager.getAction("window.reopen.tab");
        assertNotNull(reopenTab);
        KeyCodeCombination rtAcc = (KeyCodeCombination) reopenTab.getAccelerator();
        assertEquals(KeyCode.T, rtAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, rtAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, rtAcc.getShift());
    }

    @Test
    public void testHelpMenuRegistration() {
        ActionGroup helpMenu = actionManager.getGroup("menu.help");
        assertNotNull(helpMenu, "menu.help group should be registered");
        assertEquals("Help", helpMenu.getText());
    }

    @Test
    public void testHelpMenuStructureAndOrder() {
        ActionGroup helpMenu = actionManager.getGroup("menu.help");
        assertNotNull(helpMenu, "menu.help group should be registered");

        List<AnAction> items = helpMenu.getChildren();
        // 9 sections: 2 + 1 + 3 + 1 + 3 + 3 + 6 + 4 + 2 = 25 items + 8 separators = 33 entries
        assertEquals(33, items.size(), "Help menu should contain 33 entries (25 actions/submenus and 8 separators)");

        int idx = 0;

        // Section 1: Welcome & Find Action
        assertEquals("help.welcome", items.get(idx++).getId());
        assertEquals("help.find.action", items.get(idx++).getId());

        // Separator 1
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 2: Help
        assertEquals("help.help", items.get(idx++).getId());

        // Separator 2
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 3: Tip of the Day, My Productivity, Learn IDE Features
        assertEquals("help.tip.of.the.day", items.get(idx++).getId());
        assertEquals("help.my.productivity", items.get(idx++).getId());
        assertEquals("help.learn.features", items.get(idx++).getId());

        // Separator 3
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 4: What's New
        assertEquals("help.whats.new", items.get(idx++).getId());

        // Separator 4
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 5: Getting Started, YouTube, Shortcuts PDF
        assertEquals("help.getting.started", items.get(idx++).getId());
        assertEquals("help.youtube", items.get(idx++).getId());
        assertEquals("help.shortcuts.pdf", items.get(idx++).getId());

        // Separator 5
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 6: Support, Bug Report, Feedback
        assertEquals("help.contact.support", items.get(idx++).getId());
        assertEquals("help.bug.report", items.get(idx++).getId());
        assertEquals("help.submit.feedback", items.get(idx++).getId());

        // Separator 6
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 7: Logs, Profiling, Snapshot, Diagnostic Tools
        assertEquals("help.show.log.in.files", items.get(idx++).getId());
        assertEquals("help.show.sql.log.in.files", items.get(idx++).getId());
        assertEquals("help.collect.logs", items.get(idx++).getId());
        assertEquals("help.cpu.profiling", items.get(idx++).getId());
        assertEquals("help.capture.memory.snapshot", items.get(idx++).getId());
        assertEquals("help.diagnostic.tools", items.get(idx++).getId());

        // Separator 7
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 8: Memory & VM Options
        assertEquals("help.change.memory.settings", items.get(idx++).getId());
        assertEquals("help.custom.properties", items.get(idx++).getId());
        assertEquals("help.custom.vm.options", items.get(idx++).getId());
        assertEquals("help.delete.leftover.dirs", items.get(idx++).getId());

        // Separator 8
        assertInstanceOf(ActionSeparator.class, items.get(idx++));

        // Section 9: Updates & About (Register... excluded)
        assertEquals("help.updates", items.get(idx++).getId());
        assertEquals("help.about", items.get(idx++).getId());
    }

    @Test
    public void testRegisterOptionExcluded() {
        assertNull(actionManager.getAction("help.register"), "Register option must be excluded");
        ActionGroup helpMenu = actionManager.getGroup("menu.help");
        for (AnAction action : helpMenu.getChildren()) {
            if (!(action instanceof ActionSeparator)) {
                assertFalse(action.getText().toLowerCase().contains("register"),
                        "Help menu should not contain 'Register' option: " + action.getText());
            }
        }
    }

    @Test
    public void testDiagnosticToolsSubmenu() {
        ActionGroup diagnosticTools = actionManager.getGroup("help.diagnostic.tools");
        assertNotNull(diagnosticTools, "help.diagnostic.tools must be registered");
        assertTrue(diagnosticTools.isPopup());

        List<AnAction> children = diagnosticTools.getChildren();
        assertEquals(9, children.size(), "Diagnostic Tools should contain 9 items");

        assertEquals("help.diagnostic.activity.monitor", children.get(0).getId());
        assertEquals("help.diagnostic.dump.threads", children.get(1).getId());
        assertEquals("help.diagnostic.debug.log.settings", children.get(2).getId());
        assertEquals("help.diagnostic.special.files", children.get(3).getId());
        assertEquals("help.diagnostic.start.cpu.profiling", children.get(4).getId());
        assertEquals("help.diagnostic.start.async.profiler", children.get(5).getId());
        assertEquals("help.diagnostic.capture.memory.snapshot", children.get(6).getId());
        assertEquals("help.diagnostic.profile.indexing", children.get(7).getId());
        assertEquals("help.diagnostic.open.indexing.diagnostics", children.get(8).getId());
    }

    @Test
    public void testHelpFindActionAccelerator() {
        AnAction findAction = actionManager.getAction("help.find.action");
        assertNotNull(findAction);
        assertNotNull(findAction.getAccelerator());

        KeyCodeCombination faAcc = (KeyCodeCombination) findAction.getAccelerator();
        assertEquals(KeyCode.A, faAcc.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, faAcc.getControl());
        assertEquals(KeyCombination.ModifierValue.DOWN, faAcc.getShift());
    }
}

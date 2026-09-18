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
    public void testViewMenuPreserved() {
        ActionGroup viewMenu = actionManager.getGroup("menu.view");
        assertNotNull(viewMenu, "menu.view should be registered");
        assertEquals("View", viewMenu.getText());

        assertTrue(viewMenu.getChildren().stream().anyMatch(a -> "view.refresh.explorer".equals(a.getId())),
                "View menu should retain view.refresh.explorer");
        assertTrue(viewMenu.getChildren().stream().anyMatch(a -> "view.toggle.run".equals(a.getId())),
                "View menu should retain view.toggle.run");
    }
}

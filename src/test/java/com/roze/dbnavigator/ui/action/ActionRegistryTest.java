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
}

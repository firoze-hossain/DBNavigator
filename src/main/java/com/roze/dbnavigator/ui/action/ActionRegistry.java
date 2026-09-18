package com.roze.dbnavigator.ui.action;

import com.roze.dbnavigator.ui.MainWindow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.List;

/**
 * DataGrip-style action registry.
 * Defines and registers all actions and action groups for menus,
 * center header action icons (4 icons + 3 dots), and toolbars dynamically.
 */
public final class ActionRegistry {

    private ActionRegistry() {}

    public static void initialize(ActionManager manager) {
        // =========================================================================
        // 1. FILE ACTIONS
        // =========================================================================
        AnAction newDataSource = AnAction.builder("file.new.datasource", "New Data Source…")
                .description("Create a new database connection")
                .icon(FontAwesomeSolid.PLUS_CIRCLE, "#57965c", 11)
                .onAction(MainWindow::showNewConnectionDialog)
                .build();

        AnAction newConsole = AnAction.builder("file.new.console", "New Query Console")
                .description("Open a new query console for the selected connection")
                .icon(FontAwesomeSolid.TERMINAL, "#6897bb", 11)
                .accelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::openConsoleForSelectedConnection)
                .build();

        AnAction openSql = AnAction.builder("file.open.sql", "Open SQL File in Console…")
                .description("Open a SQL script file from disk into a console")
                .icon(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::openSqlFile)
                .build();

        AnAction saveSql = AnAction.builder("file.save.console", "Save Console As…")
                .description("Save active console contents to a file")
                .icon(FontAwesomeSolid.SAVE, "#4a88c7", 11)
                .accelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::saveConsoleAs)
                .build();

        // Local History Submenu Group
        ActionGroup localHistoryGroup = new ActionGroup("file.local.history", "Local History");
        localHistoryGroup.add(AnAction.builder("history.show", "Show History…")
                .description("Show local revision history for current console")
                .icon(FontAwesomeSolid.HISTORY, "#a9b7c6", 11)
                .onAction(MainWindow::showLocalHistoryForCurrentConsole)
                .build());
        localHistoryGroup.add(AnAction.builder("history.show.selection", "Show History for Selection…")
                .onAction(MainWindow::showHistoryForSelection)
                .build());
        localHistoryGroup.add(AnAction.builder("history.show.project", "Show Project History…")
                .onAction(MainWindow::showProjectHistoryDialog)
                .build());
        localHistoryGroup.add(AnAction.builder("history.recent.changes", "Recent Changes")
                .onAction(MainWindow::showRecentChangesDialog)
                .build());
        localHistoryGroup.addSeparator();
        localHistoryGroup.add(AnAction.builder("history.put.label", "Put Label…")
                .icon(FontAwesomeSolid.TAG, "#a9b7c6", 11)
                .onAction(MainWindow::showPutLabelDialog)
                .build());

        AnAction invalidateCaches = AnAction.builder("file.invalidate.caches", "Invalidate Caches…")
                .description("Invalidate metadata caches and restart")
                .icon(FontAwesomeSolid.SYNC_ALT, "#a9b7c6", 11)
                .onAction(MainWindow::showInvalidateCachesDialog)
                .build();

        AnAction settings = AnAction.builder("file.settings", "Settings…")
                .description("Preferences and application configuration")
                .icon(FontAwesomeSolid.COG, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::showSettingsDialog)
                .build();

        AnAction exit = AnAction.builder("file.exit", "Exit")
                .description("Exit DBNavigator Pro")
                .onAction(MainWindow::closeWindow)
                .build();

        ActionGroup fileMenu = new ActionGroup("menu.file", "File");
        fileMenu.addAll(newDataSource, newConsole)
                .addSeparator()
                .addAll(openSql, saveSql)
                .addSeparator()
                .add(localHistoryGroup)
                .addSeparator()
                .addAll(invalidateCaches, settings)
                .addSeparator()
                .add(exit);

        // =========================================================================
        // 2. EDIT ACTIONS
        // =========================================================================
        AnAction undo = AnAction.builder("edit.undo", "Undo")
                .icon(FontAwesomeSolid.UNDO, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::undoCurrentEditor)
                .build();

        AnAction redo = AnAction.builder("edit.redo", "Redo")
                .icon(FontAwesomeSolid.REDO, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::redoCurrentEditor)
                .build();

        AnAction cut = AnAction.builder("edit.cut", "Cut")
                .icon(FontAwesomeSolid.CUT, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::cutCurrentEditor)
                .build();

        AnAction copy = AnAction.builder("edit.copy", "Copy")
                .icon(FontAwesomeSolid.COPY, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyCurrentEditor)
                .build();

        AnAction paste = AnAction.builder("edit.paste", "Paste")
                .icon(FontAwesomeSolid.PASTE, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::pasteCurrentEditor)
                .build();

        AnAction selectAll = AnAction.builder("edit.select.all", "Select All")
                .accelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::selectAllCurrentEditor)
                .build();

        AnAction findInFiles = AnAction.builder("edit.find.in.files", "Find in Files…")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchEverywhere)
                .build();

        AnAction formatCode = AnAction.builder("edit.format.code", "Reformat Code")
                .description("Reformat SQL queries with clean indentation and capitalized keywords")
                .icon(FontAwesomeSolid.INDENT, "#4a88c7", 11)
                .accelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveQueryTab)
                .onAction(MainWindow::formatCurrentSql)
                .build();

        ActionGroup editMenu = new ActionGroup("menu.edit", "Edit");
        editMenu.addAll(undo, redo)
                .addSeparator()
                .addAll(cut, copy, paste)
                .addSeparator()
                .add(selectAll)
                .addSeparator()
                .add(findInFiles)
                .addSeparator()
                .add(formatCode);

        // =========================================================================
        // 3. VIEW ACTIONS
        // =========================================================================
        AnAction refreshExplorer = AnAction.builder("view.refresh.explorer", "Refresh Database Explorer")
                .description("Reload connection metadata schemas")
                .icon(FontAwesomeSolid.SYNC_ALT, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::refreshSchemaExplorer)
                .build();

        AnAction toggleRunPanel = AnAction.builder("view.toggle.run", "Run Tool Window")
                .description("Show or hide the bottom Run tool window")
                .icon(FontAwesomeSolid.TERMINAL, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleRunPanel)
                .build();

        ActionGroup viewMenu = new ActionGroup("menu.view", "View");
        viewMenu.addAll(refreshExplorer, toggleRunPanel);

        // =========================================================================
        // 4. NAVIGATE ACTIONS
        // =========================================================================
        AnAction searchEverywhere = AnAction.builder("navigate.search.everywhere", "Search Everywhere…")
                .description("Search tables, views, columns, connections, and actions")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchEverywhere)
                .build();

        AnAction nextEditorTab = AnAction.builder("navigate.next.tab", "Select Next Tab")
                .accelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::selectNextTab)
                .build();

        AnAction prevEditorTab = AnAction.builder("navigate.prev.tab", "Select Previous Tab")
                .accelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::selectPreviousTab)
                .build();

        ActionGroup navigateMenu = new ActionGroup("menu.navigate", "Navigate");
        navigateMenu.add(searchEverywhere)
                .addSeparator()
                .addAll(nextEditorTab, prevEditorTab);

        // =========================================================================
        // 5. RUN ACTIONS
        // =========================================================================
        AnAction runStatement = AnAction.builder("run.statement", "Execute Statement")
                .description("Execute query statement at caret")
                .icon(FontAwesomeSolid.PLAY, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::executeCurrentStatement)
                .build();

        AnAction runToolWindow = AnAction.builder("run.tool.window", "Run Tool Window")
                .icon(FontAwesomeSolid.TERMINAL, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleRunPanel)
                .build();

        ActionGroup runMenu = new ActionGroup("menu.run", "Run");
        runMenu.add(runStatement)
                .addSeparator()
                .add(runToolWindow);

        // =========================================================================
        // 6. VCS ACTIONS
        // =========================================================================
        ActionGroup vcsMenu = new ActionGroup("menu.vcs", "VCS");
        vcsMenu.add(localHistoryGroup);

        // =========================================================================
        // 7. WINDOW ACTIONS
        // =========================================================================
        AnAction closeTab = AnAction.builder("window.close.tab", "Close Active Tab")
                .accelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeActiveTab)
                .build();

        AnAction closeOtherTabs = AnAction.builder("window.close.other.tabs", "Close Other Tabs")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeOtherTabs)
                .build();

        AnAction closeAllTabs = AnAction.builder("window.close.all.tabs", "Close All Tabs")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeAllTabs)
                .build();

        AnAction reopenTab = AnAction.builder("window.reopen.tab", "Reopen Closed Tab")
                .accelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::reopenLastClosedTab)
                .build();

        AnAction splitRight = AnAction.builder("window.split.right", "Split Right")
                .icon(FontAwesomeSolid.COLUMNS, "#a9b7c6", 11)
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::splitActiveTabRight)
                .build();

        AnAction splitDown = AnAction.builder("window.split.down", "Split Down")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::splitActiveTabDown)
                .build();

        AnAction unsplitAll = AnAction.builder("window.unsplit.all", "Unsplit All")
                .onAction(MainWindow::unsplitAll)
                .build();

        ActionGroup windowMenu = new ActionGroup("menu.window", "Window");
        windowMenu.addAll(nextEditorTab, prevEditorTab)
                .addSeparator()
                .addAll(closeTab, closeOtherTabs, closeAllTabs, reopenTab)
                .addSeparator()
                .addAll(splitRight, splitDown, unsplitAll);

        // =========================================================================
        // 8. HELP ACTIONS
        // =========================================================================
        AnAction checkUpdates = AnAction.builder("help.updates", "Check for Updates…")
                .icon(FontAwesomeSolid.DOWNLOAD, "#a9b7c6", 11)
                .onAction(MainWindow::showCheckUpdatesDialog)
                .build();

        AnAction about = AnAction.builder("help.about", "About DBNavigator Pro")
                .icon(FontAwesomeSolid.INFO_CIRCLE, "#a9b7c6", 11)
                .onAction(MainWindow::showAboutDialog)
                .build();

        ActionGroup helpMenu = new ActionGroup("menu.help", "Help");
        helpMenu.add(checkUpdates)
                .addSeparator()
                .add(about);

        // =========================================================================
        // 9. MIDDLE HEADER ACTIONS (4 ICONS + THREE DOTS ...)
        // =========================================================================
        // Icon 1: Database
        AnAction middleDatabase = AnAction.builder("middle.database", "Database Explorer")
                .description("Database Explorer / Data Sources (Alt+1)")
                .icon(FontAwesomeSolid.DATABASE, "#4a88c7", 13)
                .onAction(MainWindow::focusOrToggleSchemaExplorer)
                .build();

        // Icon 2: Run / Execute
        AnAction middleRun = AnAction.builder("middle.run", "Execute")
                .description("Execute Statement (Ctrl+Enter)")
                .icon(FontAwesomeSolid.PLAY, "#57965c", 13)
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::executeCurrentStatement)
                .build();

        // Icon 3: Console / Terminal
        AnAction middleConsole = AnAction.builder("middle.console", "New Query Console")
                .description("New Query Console (Ctrl+Shift+N)")
                .icon(FontAwesomeSolid.TERMINAL, "#6897bb", 13)
                .onAction(MainWindow::openConsoleForSelectedConnection)
                .build();

        // Icon 4: Folder / Open
        AnAction middleFolder = AnAction.builder("middle.folder", "Open SQL File")
                .description("Open SQL File in Console (Ctrl+O)")
                .icon(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 13)
                .onAction(MainWindow::openSqlFile)
                .build();

        // Icon 5: Three dots "..." More Actions dropdown
        ActionGroup moreActionsGroup = new ActionGroup("middle.more", "More Actions", true,
                FontAwesomeSolid.ELLIPSIS_H, "#a9b7c6", 13, "More Actions");
        moreActionsGroup.add(invalidateCaches)
                .add(localHistoryGroup)
                .addSeparator()
                .add(formatCode)
                .add(searchEverywhere)
                .add(refreshExplorer)
                .addSeparator()
                .add(settings);

        ActionGroup middleGroup = new ActionGroup("group.middle", "Center Actions");
        middleGroup.addAll(middleDatabase, middleRun, middleConsole, middleFolder, moreActionsGroup);

        // Register all groups
        manager.registerGroup(fileMenu);
        manager.registerGroup(editMenu);
        manager.registerGroup(viewMenu);
        manager.registerGroup(navigateMenu);
        manager.registerGroup(runMenu);
        manager.registerGroup(vcsMenu);
        manager.registerGroup(windowMenu);
        manager.registerGroup(helpMenu);
        manager.registerGroup(middleGroup);
    }

    public static List<ActionGroup> getMainMenuBarGroups(ActionManager manager) {
        return List.of(
                manager.getGroup("menu.file"),
                manager.getGroup("menu.edit"),
                manager.getGroup("menu.view"),
                manager.getGroup("menu.navigate"),
                manager.getGroup("menu.run"),
                manager.getGroup("menu.vcs"),
                manager.getGroup("menu.window"),
                manager.getGroup("menu.help")
        );
    }
}

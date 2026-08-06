package com.roze.dbnavigator.ui;

import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * DataGrip-style tab right-click menu. Real, working functionality for the
 * items that make sense in this app: Close variants, Copy Path/Reference,
 * Split Right/Down and their "and Move" variants (which can be nested to
 * create any number of side-by-side or stacked editor groups), Open Tab in New Window, Pin/
 * Unpin, Reopen Closed Tab (console tabs only — see
 * {@link MainWindow#reopenLastClosedTab()}), Rename Tab, Local History (for
 * consoles), and for consoles specifically:
 * <ul>
 *   <li><b>Bookmarks</b> — Toggle Bookmark marks the line the caret is on;
 *       Show Bookmarks… lists every bookmarked line across every open
 *       console, jump to any of them from there.</li>
 *   <li><b>Override File Type</b> — SQL (syntax highlighting, the default)
 *       or Plain Text (none) for that specific console.</li>
 *   <li><b>Open In</b> — reveals or opens the console's saved file, once
 *       it's actually been saved via File → Save Console As…; before that,
 *       explains what to do instead of just sitting there disabled.</li>
 * </ul>
 * "Configure Editor Tabs…" opens Settings, since this app doesn't have a
 * dedicated tab-behavior settings page to land on yet.
 *
 * Honest scope note: Bookmarks/Override File Type/Open In are console-only
 * — other tab types (data views, diagrams) don't have an editable text
 * buffer, a syntax mode, or a backing file to act on, so those three stay
 * disabled for anything that isn't a {@link QueryTab}.
 */
public final class TabContextMenu {

    /** Tracked by identity, not equals — a WeakHashMap-backed set survives tabs being GC'd after closing. */
    private static final Set<Tab> pinnedTabs = Collections.newSetFromMap(new WeakHashMap<>());

    private TabContextMenu() {}

    /**
     * {@code Tab} isn't a {@code Node} (it's not part of the scene graph
     * directly — it has no {@code setOnContextMenuRequested}), so a Tab's
     * own {@code setContextMenu(...)} is the actual API for this, not an
     * event-driven per-click builder. Since that menu is built once but
     * needs to reflect state that can change between right-clicks (the Pin/
     * Unpin label, whether Reopen Closed Tab has anything to reopen), the
     * dynamic bits are refreshed in the menu's own {@code onShowing} handler
     * instead of by rebuilding the whole menu each time.
     */
    public static void install(TabPane tabPane, Tab tab, MainWindow mainWindow) {
        ContextMenu menu = build(tabPane, tab, mainWindow);
        tab.setContextMenu(menu);
    }

    private static ContextMenu build(TabPane tabPane, Tab tab, MainWindow mainWindow) {
        ContextMenu menu = new ContextMenu();

        MenuItem close = new MenuItem("Close");
        close.setOnAction(e -> closeTab(tab));

        MenuItem closeOthers = new MenuItem("Close Other Tabs");
        closeOthers.setOnAction(e -> closeMatching(tabPane, t -> t != tab && !pinnedTabs.contains(t)));

        MenuItem closeAll = new MenuItem("Close All Tabs");
        closeAll.setOnAction(e -> closeMatching(tabPane, t -> !pinnedTabs.contains(t)));

        MenuItem closeLeft = new MenuItem("Close Tabs to the Left");
        closeLeft.setOnAction(e -> {
            int idx = tabPane.getTabs().indexOf(tab);
            closeMatching(tabPane, t -> tabPane.getTabs().indexOf(t) < idx && !pinnedTabs.contains(t));
        });

        MenuItem closeRight = new MenuItem("Close Tabs to the Right");
        closeRight.setOnAction(e -> {
            int idx = tabPane.getTabs().indexOf(tab);
            closeMatching(tabPane, t -> tabPane.getTabs().indexOf(t) > idx && !pinnedTabs.contains(t));
        });

        MenuItem copyPath = new MenuItem("Copy Path/Reference\u2026");
        copyPath.setOnAction(e -> copyReference(tab));

        MenuItem pin = new MenuItem("Pin Tab");
        pin.setOnAction(e -> togglePin(tabPane, tab));

        MenuItem reopen = new MenuItem("Reopen Closed Tab");
        reopen.setOnAction(e -> mainWindow.reopenLastClosedTab());

        MenuItem rename = new MenuItem("Rename Tab\u2026");
        rename.setOnAction(e -> renameTab(tab, mainWindow));

        MenuItem splitRight = new MenuItem("Split Right");
        splitRight.setOnAction(e -> mainWindow.splitRight(tab));
        MenuItem splitMoveRight = new MenuItem("Split and Move Right");
        splitMoveRight.setOnAction(e -> mainWindow.splitAndMoveRight(tab));
        MenuItem splitDown = new MenuItem("Split Down");
        splitDown.setOnAction(e -> mainWindow.splitDown(tab));
        MenuItem splitMoveDown = new MenuItem("Split and Move Down");
        splitMoveDown.setOnAction(e -> mainWindow.splitAndMoveDown(tab));
        MenuItem openNewWindow = new MenuItem("Open Tab in New Window");
        openNewWindow.setOnAction(e -> mainWindow.openTabInNewWindow(tab));
        MenuItem configureTabs = new MenuItem("Configure Editor Tabs\u2026");
        configureTabs.setOnAction(e -> SettingsDialog.show(mainWindow));
        CheckMenuItem shortenTitles = new CheckMenuItem("Shorten Tab Titles");
        shortenTitles.setSelected(true);
        shortenTitles.setDisable(true);

        Menu bookmarks;
        MenuItem overrideFileType;
        Menu openIn;
        if (tab instanceof QueryTab queryTab) {
            bookmarks = new Menu("Bookmarks");
            MenuItem toggleBookmark = new MenuItem("Toggle Bookmark");
            toggleBookmark.setOnAction(e -> queryTab.toggleBookmarkAtCaret());
            MenuItem showBookmarks = new MenuItem("Show Bookmarks\u2026");
            showBookmarks.setOnAction(e -> mainWindow.showBookmarksDialog());
            bookmarks.getItems().addAll(toggleBookmark, showBookmarks);

            Menu fileTypeMenu = new Menu("Override File Type");
            RadioMenuItem sqlType = new RadioMenuItem("SQL");
            RadioMenuItem plainType = new RadioMenuItem("Plain Text");
            ToggleGroup fileTypeGroup = new ToggleGroup();
            sqlType.setToggleGroup(fileTypeGroup);
            plainType.setToggleGroup(fileTypeGroup);
            sqlType.setSelected(!queryTab.isPlainTextMode());
            plainType.setSelected(queryTab.isPlainTextMode());
            sqlType.setOnAction(e -> queryTab.setPlainTextMode(false));
            plainType.setOnAction(e -> queryTab.setPlainTextMode(true));
            fileTypeMenu.getItems().addAll(sqlType, plainType);
            overrideFileType = fileTypeMenu;

            openIn = new Menu("Open In");
            java.io.File savedFile = queryTab.getSavedFile();
            if (savedFile != null) {
                MenuItem reveal = new MenuItem("File Manager");
                reveal.setOnAction(e -> openInFileManager(savedFile, mainWindow));
                MenuItem openApp = new MenuItem("Default Application");
                openApp.setOnAction(e -> openInDefaultApp(savedFile, mainWindow));
                openIn.getItems().addAll(reveal, openApp);
            } else {
                MenuItem notSaved = new MenuItem("Save Console As\u2026 first");
                notSaved.setOnAction(e -> mainWindow.setStatus(
                        "This console isn't saved to a file yet — use File \u2192 Save Console As\u2026"));
                openIn.getItems().add(notSaved);
            }
        } else {
            bookmarks = disabledMenu("Bookmarks");
            overrideFileType = disabled("Override File Type");
            openIn = disabledMenu("Open In");
        }

        Menu localHistory = new Menu("Local History");
        if (tab instanceof QueryTab queryTab) {
            MenuItem showHistory = new MenuItem("Show History\u2026");
            showHistory.setOnAction(e -> queryTab.showLocalHistory(mainWindow.getOwnerWindow()));
            localHistory.getItems().add(showHistory);
        } else {
            localHistory.setDisable(true);
        }

        menu.getItems().addAll(
                close, closeOthers, closeAll, closeLeft, closeRight,
                new SeparatorMenuItem(), copyPath,
                new SeparatorMenuItem(), splitRight, splitMoveRight, splitDown, splitMoveDown,
                new SeparatorMenuItem(), pin, openNewWindow, configureTabs,
                new SeparatorMenuItem(), reopen,
                new SeparatorMenuItem(), shortenTitles, bookmarks,
                new SeparatorMenuItem(), overrideFileType, openIn, localHistory,
                new SeparatorMenuItem(), rename);

        // Refresh the bits that can go stale between right-clicks right
        // before the menu actually becomes visible.
        menu.setOnShowing(e -> {
            pin.setText(pinnedTabs.contains(tab) ? "Unpin Tab" : "Pin Tab");
            reopen.setDisable(!mainWindow.hasClosedTabToReopen());
        });

        return menu;
    }

    private static MenuItem disabled(String text) {
        MenuItem item = new MenuItem(text);
        item.setDisable(true);
        return item;
    }

    private static Menu disabledMenu(String text) {
        Menu menu = new Menu(text);
        menu.setDisable(true);
        return menu;
    }

    /**
     * Fires the tab's own close request first (so anything like an unsaved-
     * changes prompt can still veto it), then removes it — removal alone is
     * what makes TabPane fire CLOSED_EVENT internally (that's what runs a
     * console's Local History save / cursor cleanup), so nothing else needs
     * to be triggered manually here.
     */
    private static void closeTab(Tab tab) {
        TabPane pane = tab.getTabPane();
        if (pane == null) return;
        Event closeEvent = new Event(tab, tab, Tab.TAB_CLOSE_REQUEST_EVENT);
        Event.fireEvent(tab, closeEvent);
        if (!closeEvent.isConsumed()) {
            pane.getTabs().remove(tab);
        }
    }

    private static void closeMatching(TabPane tabPane, Predicate<Tab> predicate) {
        List<Tab> toClose = List.copyOf(tabPane.getTabs());
        for (Tab t : toClose) {
            if (predicate.test(t)) closeTab(t);
        }
    }

    private static void togglePin(TabPane tabPane, Tab tab) {
        if (pinnedTabs.contains(tab)) {
            pinnedTabs.remove(tab);
        } else {
            pinnedTabs.add(tab);
            // Pinned tabs move to the front, after any other already-pinned tabs
            tabPane.getTabs().remove(tab);
            int insertAt = 0;
            while (insertAt < tabPane.getTabs().size() && pinnedTabs.contains(tabPane.getTabs().get(insertAt))) {
                insertAt++;
            }
            tabPane.getTabs().add(insertAt, tab);
            tabPane.getSelectionModel().select(tab);
        }
        tab.getStyleClass().removeAll("pinned-tab");
        if (pinnedTabs.contains(tab)) tab.getStyleClass().add("pinned-tab");
    }

    private static void copyReference(Tab tab) {
        String text = switch (tab) {
            case QueryTab qt -> qt.getProfile().getName()
                    + (qt.getCatalog() != null ? "/" + qt.getCatalog() : "") + "/" + qt.getFileId();
            case DataTab dt -> dt.getQualifiedTableReference();
            default -> tab.getText();
        };
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static void openInFileManager(java.io.File file, MainWindow mainWindow) {
        try {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop.isSupported(java.awt.Desktop.Action.BROWSE_FILE_DIR)) {
                desktop.browseFileDirectory(file);
            } else {
                desktop.open(file.getParentFile());
            }
        } catch (Exception ex) {
            mainWindow.setStatus("Could not open file manager: " + ex.getMessage());
        }
    }

    private static void openInDefaultApp(java.io.File file, MainWindow mainWindow) {
        try {
            java.awt.Desktop.getDesktop().open(file);
        } catch (Exception ex) {
            mainWindow.setStatus("Could not open " + file.getName() + ": " + ex.getMessage());
        }
    }

    private static void renameTab(Tab tab, MainWindow mainWindow) {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog(tab.getText()));
        dialog.initOwner(mainWindow.getOwnerWindow());
        dialog.setTitle("Rename Tab");
        dialog.setHeaderText(null);
        dialog.setContentText("New tab name:");
        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.isBlank()) tab.setText(newName.trim());
        });
    }
}

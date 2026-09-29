package com.roze.dbnavigator.ui.action;

import com.roze.dbnavigator.db.AppSettingsStore.MenuItemConfig;
import com.roze.dbnavigator.ui.Icons;
import com.roze.dbnavigator.ui.MainWindow;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataGrip/IntelliJ-style ActionManager.
 * Central registry that creates and manages actions, action groups, menus,
 * and header toolbars, and dynamically updates action states.
 */
public class ActionManager {

    private static final ActionManager INSTANCE = new ActionManager();

    private final Map<String, AnAction> actionMap = new HashMap<>();
    private final Map<String, ActionGroup> groupMap = new HashMap<>();

    private record BoundItem(WeakReference<MenuItem> itemRef, AnAction action) {}
    private record BoundButton(WeakReference<Button> buttonRef, AnAction action) {}

    private final List<BoundItem> boundMenuItems = new ArrayList<>();
    private final List<BoundButton> boundButtons = new ArrayList<>();

    private ActionManager() {}

    public static ActionManager getInstance() {
        return INSTANCE;
    }

    public void registerAction(AnAction action) {
        actionMap.put(action.getId(), action);
    }

    public void registerGroup(ActionGroup group) {
        groupMap.put(group.getId(), group);
        actionMap.put(group.getId(), group);
        for (AnAction child : group.getChildren()) {
            if (child instanceof ActionGroup subGroup) {
                registerGroup(subGroup);
            } else if (!(child instanceof ActionSeparator)) {
                registerAction(child);
            }
        }
    }

    public AnAction getAction(String id) {
        return actionMap.get(id);
    }

    public ActionGroup getGroup(String id) {
        return groupMap.get(id);
    }

    public Map<String, AnAction> getActionMap() {
        return Collections.unmodifiableMap(actionMap);
    }

    public Map<String, ActionGroup> getGroupMap() {
        return Collections.unmodifiableMap(groupMap);
    }

    public static Node createIconNode(String iconName, String iconPath, int size) {
        if (iconPath != null && !iconPath.isBlank()) {
            try {
                File file = new File(iconPath);
                if (file.exists()) {
                    Image img = new Image(file.toURI().toString(), size, size, true, true);
                    ImageView view = new ImageView(img);
                    view.setFitWidth(size);
                    view.setFitHeight(size);
                    return view;
                }
            } catch (Exception ignored) {}
        }
        if (iconName != null && !iconName.isBlank()) {
            try {
                FontAwesomeSolid fa = FontAwesomeSolid.valueOf(iconName.toUpperCase().replace('-', '_'));
                return Icons.of(fa, "#a9b7c6", size);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Builds a JavaFX MenuBar dynamically from a list of ActionGroups.
     */
    public MenuBar buildMenuBar(List<ActionGroup> groups, MainWindow ctx) {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("app-menu-bar");

        for (ActionGroup group : groups) {
            menuBar.getMenus().add(buildMenu(group, ctx));
        }

        return menuBar;
    }

    /**
     * Builds a JavaFX MenuBar dynamically from a configured MenuItemConfig.
     */
    public MenuBar buildMenuBarFromConfig(MenuItemConfig mainMenuConfig, MainWindow ctx) {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("app-menu-bar");
        if (mainMenuConfig == null || mainMenuConfig.getChildren().isEmpty()) {
            return buildMenuBar(ActionRegistry.getMainMenuBarGroups(this), ctx);
        }

        for (MenuItemConfig groupConfig : mainMenuConfig.getChildren()) {
            if (groupConfig.getType() == MenuItemConfig.Type.GROUP) {
                menuBar.getMenus().add(buildMenuFromConfig(groupConfig, ctx));
            }
        }
        return menuBar;
    }

    private javafx.scene.control.Menu buildMenu(ActionGroup group, MainWindow ctx) {
        javafx.scene.control.Menu menu = new javafx.scene.control.Menu(group.getText());
        if (group.getIcon() != null) {
            menu.setGraphic(com.roze.dbnavigator.ui.Icons.of(group.getIcon(), group.getIconColor(), group.getIconSize()));
        }
        if (group.getOnMenuShowing() != null) {
            menu.setOnShowing(ev -> group.getOnMenuShowing().accept(menu, ctx));
        }

        for (AnAction child : group.getChildren()) {
            appendActionToMenu(child, menu, ctx);
        }

        return menu;
    }

    private void appendActionToMenu(AnAction child, javafx.scene.control.Menu targetMenu, MainWindow ctx) {
        if (child instanceof ActionSeparator) {
            addSeparatorToMenu(targetMenu);
        } else if (child instanceof ActionGroup subGroup) {
            if (subGroup.isPopup()) {
                targetMenu.getItems().add(buildMenu(subGroup, ctx));
            } else {
                for (AnAction grandChild : subGroup.getChildren()) {
                    appendActionToMenu(grandChild, targetMenu, ctx);
                }
            }
        } else {
            MenuItem item = child.createMenuItem(ctx);
            boundMenuItems.add(new BoundItem(new WeakReference<>(item), child));
            targetMenu.getItems().add(item);
        }
    }

    private javafx.scene.control.Menu buildMenuFromConfig(MenuItemConfig groupConfig, MainWindow ctx) {
        javafx.scene.control.Menu menu = new javafx.scene.control.Menu(groupConfig.getText());
        Node iconNode = createIconNode(groupConfig.getIconName(), groupConfig.getIconPath(), 11);
        if (iconNode != null) {
            menu.setGraphic(iconNode);
        }

        ActionGroup regGroup = getGroup(groupConfig.getId());
        if (regGroup != null && regGroup.getOnMenuShowing() != null) {
            menu.setOnShowing(e -> regGroup.getOnMenuShowing().accept(menu, ctx));
        }

        List<MenuItemConfig> children = groupConfig.getChildren();
        if ((children == null || children.isEmpty()) && regGroup != null && !regGroup.getChildren().isEmpty()) {
            for (AnAction childAction : regGroup.getChildren()) {
                appendActionToMenu(childAction, menu, ctx);
            }
            return menu;
        }

        if (children != null) {
            for (MenuItemConfig child : children) {
                appendConfigItemToMenu(child, menu, ctx);
            }
        }
        return menu;
    }

    private void appendConfigItemToMenu(MenuItemConfig child, javafx.scene.control.Menu targetMenu, MainWindow ctx) {
        if (child.getType() == MenuItemConfig.Type.SEPARATOR) {
            addSeparatorToMenu(targetMenu);
        } else if (child.getType() == MenuItemConfig.Type.GROUP) {
            if (child.isPopup()) {
                targetMenu.getItems().add(buildMenuFromConfig(child, ctx));
            } else {
                if (child.getChildren() != null) {
                    for (MenuItemConfig grandChild : child.getChildren()) {
                        appendConfigItemToMenu(grandChild, targetMenu, ctx);
                    }
                }
            }
        } else {
            ActionGroup childGroup = getGroup(child.getId());
            if (childGroup != null) {
                if (childGroup.isPopup()) {
                    targetMenu.getItems().add(buildMenu(childGroup, ctx));
                } else {
                    for (AnAction a : childGroup.getChildren()) {
                        appendActionToMenu(a, targetMenu, ctx);
                    }
                }
            } else {
                MenuItem item = createConfiguredMenuItem(child, ctx);
                targetMenu.getItems().add(item);
            }
        }
    }

    private void addSeparatorToMenu(javafx.scene.control.Menu menu) {
        if (!menu.getItems().isEmpty() && !(menu.getItems().get(menu.getItems().size() - 1) instanceof SeparatorMenuItem)) {
            menu.getItems().add(new SeparatorMenuItem());
        }
    }

    private MenuItem createConfiguredMenuItem(MenuItemConfig config, MainWindow ctx) {
        AnAction action = getAction(config.getId());
        MenuItem item;
        if (action != null) {
            item = action.createMenuItem(ctx);
            if (config.getText() != null && !config.getText().isBlank()) {
                item.setText(config.getText());
            }
            boundMenuItems.add(new BoundItem(new WeakReference<>(item), action));
        } else {
            item = new MenuItem(config.getText());
            item.setDisable(true);
        }
        Node customIcon = createIconNode(config.getIconName(), config.getIconPath(), 11);
        if (customIcon != null) {
            item.setGraphic(customIcon);
        }
        return item;
    }

    /**
     * Builds a middle action toolbar dynamically from an ActionGroup.
     */
    public HBox buildToolBar(ActionGroup group, MainWindow ctx) {
        HBox toolbar = new HBox(3);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER);
        toolbar.getStyleClass().add("header-middle-bar");

        for (AnAction child : group.getChildren()) {
            if (child instanceof ActionSeparator sep) {
                toolbar.getChildren().add(sep.createToolbarSeparator());
            } else if (child instanceof ActionGroup subGroup && subGroup.isPopup()) {
                Button popupBtn = subGroup.createPopupButton(ctx);
                boundButtons.add(new BoundButton(new WeakReference<>(popupBtn), subGroup));
                toolbar.getChildren().add(popupBtn);
            } else {
                Button btn = child.createIconButton(ctx);
                boundButtons.add(new BoundButton(new WeakReference<>(btn), child));
                toolbar.getChildren().add(btn);
            }
        }

        return toolbar;
    }

    /**
     * Builds an action toolbar dynamically from a MenuItemConfig.
     */
    public HBox buildToolBarFromConfig(MenuItemConfig toolbarConfig, MainWindow ctx) {
        HBox toolbar = new HBox(3);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER);
        toolbar.getStyleClass().add("header-middle-bar");

        if (toolbarConfig == null) {
            ActionGroup middleGroup = getGroup("group.middle");
            if (middleGroup != null) return buildToolBar(middleGroup, ctx);
            return toolbar;
        }

        List<MenuItemConfig> items = toolbarConfig.getChildren();
        for (MenuItemConfig child : toolbarConfig.getChildren()) {
            if ("toolbar.center".equalsIgnoreCase(child.getId())) {
                items = child.getChildren();
                break;
            }
        }

        for (MenuItemConfig child : items) {
            if (child.getType() == MenuItemConfig.Type.SEPARATOR) {
                toolbar.getChildren().add(new ActionSeparator().createToolbarSeparator());
            } else if (child.getType() == MenuItemConfig.Type.GROUP) {
                ActionGroup group = getGroup(child.getId());
                if (group != null && group.isPopup()) {
                    Button popupBtn = group.createPopupButton(ctx);
                    boundButtons.add(new BoundButton(new WeakReference<>(popupBtn), group));
                    toolbar.getChildren().add(popupBtn);
                } else {
                    Button groupBtn = new Button(child.getText());
                    groupBtn.getStyleClass().add("header-action-button");
                    ContextMenu cm = buildContextMenuFromConfig(child, ctx);
                    groupBtn.setOnAction(e -> cm.show(groupBtn, javafx.geometry.Side.BOTTOM, 0, 4));
                    toolbar.getChildren().add(groupBtn);
                }
            } else {
                AnAction action = getAction(child.getId());
                Button btn;
                if (action != null) {
                    btn = action.createIconButton(ctx);
                    boundButtons.add(new BoundButton(new WeakReference<>(btn), action));
                } else {
                    btn = new Button();
                    btn.getStyleClass().add("header-action-button");
                    btn.setTooltip(new Tooltip(child.getText()));
                }
                Node customIcon = createIconNode(child.getIconName(), child.getIconPath(), 13);
                if (customIcon != null) {
                    btn.setGraphic(customIcon);
                }
                toolbar.getChildren().add(btn);
            }
        }
        return toolbar;
    }

    /**
     * Builds a ContextMenu dynamically from a configured MenuItemConfig.
     */
    public ContextMenu buildContextMenuFromConfig(MenuItemConfig config, MainWindow ctx) {
        ContextMenu menu = new ContextMenu();
        if (config == null || config.getChildren().isEmpty()) return menu;

        for (MenuItemConfig child : config.getChildren()) {
            appendConfigItemToContextMenu(child, menu, ctx);
        }
        return menu;
    }

    private void appendConfigItemToContextMenu(MenuItemConfig child, ContextMenu targetMenu, MainWindow ctx) {
        if (child.getType() == MenuItemConfig.Type.SEPARATOR) {
            if (!targetMenu.getItems().isEmpty() && !(targetMenu.getItems().get(targetMenu.getItems().size() - 1) instanceof SeparatorMenuItem)) {
                targetMenu.getItems().add(new SeparatorMenuItem());
            }
        } else if (child.getType() == MenuItemConfig.Type.GROUP) {
            if (child.isPopup()) {
                targetMenu.getItems().add(buildMenuFromConfig(child, ctx));
            } else {
                if (child.getChildren() != null) {
                    for (MenuItemConfig grandChild : child.getChildren()) {
                        appendConfigItemToContextMenu(grandChild, targetMenu, ctx);
                    }
                }
            }
        } else {
            ActionGroup childGroup = getGroup(child.getId());
            if (childGroup != null) {
                if (childGroup.isPopup()) {
                    targetMenu.getItems().add(buildMenu(childGroup, ctx));
                } else {
                    for (AnAction a : childGroup.getChildren()) {
                        appendActionToContextMenu(a, targetMenu, ctx);
                    }
                }
            } else {
                targetMenu.getItems().add(createConfiguredMenuItem(child, ctx));
            }
        }
    }

    private void appendActionToContextMenu(AnAction child, ContextMenu targetMenu, MainWindow ctx) {
        if (child instanceof ActionSeparator) {
            if (!targetMenu.getItems().isEmpty() && !(targetMenu.getItems().get(targetMenu.getItems().size() - 1) instanceof SeparatorMenuItem)) {
                targetMenu.getItems().add(new SeparatorMenuItem());
            }
        } else if (child instanceof ActionGroup subGroup) {
            if (subGroup.isPopup()) {
                targetMenu.getItems().add(buildMenu(subGroup, ctx));
            } else {
                for (AnAction grandChild : subGroup.getChildren()) {
                    appendActionToContextMenu(grandChild, targetMenu, ctx);
                }
            }
        } else {
            MenuItem item = child.createMenuItem(ctx);
            boundMenuItems.add(new BoundItem(new WeakReference<>(item), child));
            targetMenu.getItems().add(item);
        }
    }

    /**
     * Categorized model for the Add Action dialog.
     */
    public static class ActionCatalogCategory {
        private final String name;
        private final List<ActionCatalogCategory> subCategories = new ArrayList<>();
        private final List<ActionCatalogEntry> entries = new ArrayList<>();

        public ActionCatalogCategory(String name) { this.name = name; }
        public String getName() { return name; }
        public List<ActionCatalogCategory> getSubCategories() { return subCategories; }
        public List<ActionCatalogEntry> getEntries() { return entries; }
        public void addSubCategory(ActionCatalogCategory sub) { subCategories.add(sub); }
        public void addEntry(ActionCatalogEntry entry) { entries.add(entry); }
    }

    public static class ActionCatalogEntry {
        private final String id;
        private final String name;
        private final String iconName;
        private final MenuItemConfig.Type type;

        public ActionCatalogEntry(String id, String name, String iconName, MenuItemConfig.Type type) {
            this.id = id;
            this.name = name;
            this.iconName = iconName;
            this.type = type;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getIconName() { return iconName; }
        public MenuItemConfig.Type getType() { return type; }

        @Override
        public String toString() { return name; }
    }

    public List<ActionCatalogCategory> getActionCatalog() {
        List<ActionCatalogCategory> rootCategories = new ArrayList<>();

        // 1. Editor Actions
        ActionCatalogCategory editorActions = new ActionCatalogCategory("Editor Actions");
        editorActions.addEntry(new ActionCatalogEntry("edit.cut", "Cut", "CUT", MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("edit.copy", "Copy", "COPY", MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("edit.paste", "Paste", "PASTE", MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("edit.select.all", "Select All", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("edit.delete", "Delete", "TRASH", MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("code.reformat", "Reformat Code", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("code.indent", "Auto-Indent Lines", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("code.comment.line", "Comment with Line Comment", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("code.comment.block", "Comment with Block Comment", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("folding.expand", "Folding: Expand", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("folding.collapse", "Folding: Collapse", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("folding.expand.all", "Folding: Expand All", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("folding.collapse.all", "Folding: Collapse All", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("editor.explain.plan", "Show Execution Plan", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("editor.jump.ddl", "Jump to DDL", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("editor.modify.table", "Modify Table\u2026", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("editor.split.right", "Split Right", null, MenuItemConfig.Type.ACTION));
        editorActions.addEntry(new ActionCatalogEntry("editor.split.down", "Split Down", null, MenuItemConfig.Type.ACTION));
        rootCategories.add(editorActions);

        // 2. Main Menu
        ActionCatalogCategory mainMenuCat = new ActionCatalogCategory("Main Menu");
        ActionCatalogCategory fileCat = new ActionCatalogCategory("File");
        fileCat.addEntry(new ActionCatalogEntry("file.open.actions", "File Open Actions", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.project", "New Project\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.sqlfile", "New SQL File", "FILE_CODE", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.generic.file", "New File", "FILE", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.scratch", "New Scratch File", "FILE_ALT", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.directory.package", "Directory/Package", "FOLDER", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.html", "HTML File", "FILE_CODE", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.microservices.templates", "Microservices Templates", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.from.template", "From Template", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.xml.config.file", "XML Configuration File", "CODE", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.queryfile.active", "Query File", "TERMINAL", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.queryfile", "Query File\u2026", "FILE_CODE", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.scratch.queryfile", "Scratch Query File", "TERMINAL", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.add.ddl.object", "Add Ddl Object", "DATABASE", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.datasource.cloud", "Data Source from Cloud Provider", "CLOUD", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.datasource.templates", "Data Source Templates", "LAYER_GROUP", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.datasource.file", "Data Source from File/Folder", "FOLDER_OPEN", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.datasource.url", "Data Source from URL", "LINK", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.datasource.selection", "Add Data Source from Selection\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.datasource.path", "Data Source in Path", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.datasource.clipboard", "Import from Clipboard", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.folder", "Create a New Folder", "FOLDER", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.new.driver", "Driver", "PLUG", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.open.sql", "Open\u2026", "FOLDER_OPEN", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.save.as", "Save As\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.recent", "Recent Projects", null, MenuItemConfig.Type.GROUP));
        fileCat.addEntry(new ActionCatalogEntry("file.reopen.project", "Reopen Project", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.manage.projects", "Manage Projects\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.close.project", "Close Project", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.rename.project", "Rename Project\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.attach.directory", "Attach Directory to Project\u2026", "FOLDER_PLUS", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.remote.dev", "Remote Development", "DESKTOP", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.remote.dev.daemon.action", "Remote Development\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.settings", "Settings\u2026", "COG", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.project.structure", "Project Structure\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.sql.dialects", "SQL Dialects\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.sql.scopes", "SQL Resolution Scopes\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.edit.datasources.xml", "Edit dataSources.xml", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.props.encoding", "File Encoding", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.remove.bom", "Remove BOM", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.add.bom", "Add BOM", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.associate.file.type", "Associate with File Type\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.change.template.lang", "Change Template Data Language", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.toggle.readonly", "Toggle Read-Only Attribute", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.line.sep.crlf", "CRLF - Windows (\\r\\n)", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.line.sep.lf", "LF - Unix and macOS (\\n)", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.line.sep.cr", "CR - Classic Mac OS (\\r)", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("history.show", "Show History\u2026", "HISTORY", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("history.show.selection", "Show History for Selection\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("history.show.project", "Show Project History\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("history.recent.changes", "Recent Changes", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("history.put.label", "Put Label\u2026", "TAG", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.save.all", "Save All", "SAVE", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.reload.all", "Reload All from Disk", "SYNC_ALT", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.settings.import", "Import Settings\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.settings.export", "Export Settings\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.settings.restore", "Restore Default Settings\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.settings.backup.sync", "Backup and Sync\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.export.html", "Export Files or Selection to HTML\u2026", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.print", "Print\u2026", "PRINT", MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.power.save", "Power Save Mode", null, MenuItemConfig.Type.ACTION));
        fileCat.addEntry(new ActionCatalogEntry("file.exit", "Exit", null, MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(fileCat);

        ActionCatalogCategory editCat = new ActionCatalogCategory("Edit");
        editCat.addEntry(new ActionCatalogEntry("edit.undo", "Undo", "UNDO", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.redo", "Redo", "REDO", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.cut", "Cut", "CUT", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy", "Copy", "COPY", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.paths", "Copy Paths", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.plain", "Copy as Plain Text", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.rich", "Copy as Rich Text", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.path.absolute", "Copy Absolute Path", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.path.filename", "Copy File Name", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.path.line.number", "Copy Path with Line Number", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.path.content.root", "Copy Path from Content Root", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.path.source.root", "Copy Path from Source Root", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.path.repo.root", "Copy Path from Repository Root", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.git.hosting.link", "Copy Git Hosting Link", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.toolbox.url", "Copy Toolbox URL", "CUBES", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.reference", "Copy Reference", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.paste", "Paste", "PASTE", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.paste.history", "Paste from History…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.paste.plain", "Paste as Plain Text", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.copy.json.pointer", "Copy JSON Pointer", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.delete", "Delete", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.find", "Find…", "SEARCH", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.replace", "Replace…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.find.in.files", "Find in Files…", "SEARCH", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.replace.in.files", "Replace in Files…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.find.usages", "Find Usages", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate.sql.group", "SqlGenerateGroup", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate.xml.tag", "XML Tag…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate.override.methods", "Override Methods…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate.implement.methods", "Implement Methods…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate.delegate.methods", "Delegate Methods…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate.test.creators.group", "GenerateFromTestCreatorsGroup", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate.markdown.link", "Create Link", "LINK", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate.markdown.table", "Insert Table", "TABLE", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate.markdown.image", "Insert Image", "IMAGE", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate.markdown.toc", "Generate Table Of Contents", "LIST", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.generate", "Generate…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.insert.live.template", "Insert Live Template…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.surround.with", "Surround With…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.format.code", "Reformat Code", "INDENT", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.format.file", "Reformat File…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.comment.line", "// Comment with Line Comment", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.comment.block", "Comment with Block Comment", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.auto.indent", "Auto-Indent Lines", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.rename", "Rename…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.expand.column.list", "Expand Column List", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.convert.subquery", "Convert to Subquery", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.subquery.cte", "Subquery as CTE", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.table.alias", "Table alias…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.introduce.variable", "Introduce Variable…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.extract.routine", "Extract Routine…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.qualify.identifier", "Qualify Identifier", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.unqualify.identifier", "Unqualify Identifier", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.quote.identifier", "Quote Identifier", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.unquote.identifier", "Unquote Identifier", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.flip.expression", "Flip Expression", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.inject.language", "Inject Language or Reference", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.uninject.language", "Uninject Language or Reference", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.extract.view", "Extract View…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.refactor.extract.subquery", "Extract Subquery…", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.selection.column.mode", "Column Selection Mode", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.select.all", "Select All", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.selection.add.carets.ends", "Add Carets to Ends of Selected Lines", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.selection.extend", "Extend Selection", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.selection.shrink", "Shrink Selection", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.toggle.case", "Toggle Case", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.join.lines", "Join Lines", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.duplicate.lines", "Duplicate Entire Lines", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.sort.lines", "Sort Lines", null, MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.toggle.bookmark", "Toggle Bookmark", "BOOKMARK", MenuItemConfig.Type.ACTION));
        editCat.addEntry(new ActionCatalogEntry("edit.show.bookmarks", "Show Bookmarks…", null, MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(editCat);

        ActionCatalogCategory viewCat = new ActionCatalogCategory("View");
        viewCat.addEntry(new ActionCatalogEntry("middle.database", "Database Explorer", "DATABASE", MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.tool.files", "Files Tool Window", "FOLDER", MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.tool.terminal", "Terminal Tool Window", "TERMINAL", MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("file.new.console", "Query Console", "TERMINAL", MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.presentation.mode", "Toggle Presentation Mode", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.distraction.free.mode", "Toggle Distraction Free Mode", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.fullscreen.mode", "Toggle Full Screen Mode", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.zen.mode", "Toggle Zen Mode", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.compact.mode", "Compact Mode", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.zoom.ide", "Zoom IDE", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.presentation.assistant", "Presentation Assistant", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.main.menu", "Main Menu", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.toolbar", "Toolbar", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.toolbar.classic", "Toolbar Classic", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.navigation.bar", "Navigation Bar", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.tool.window.bars", "Tool Window Bars", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.status.bar", "Status Bar", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.status.bar.widget.status.text", "Status Text", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.toggle.members.in.nav.bar", "Members in Navigation Bar", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.recent.files", "Recent Files", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.recent.locations", "Recent Locations", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.recent.changes", "Recent Changes", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.recently.changed.files", "Recently Changed Files", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.font.increase", "Increase Font Size in All Editors", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.font.decrease", "Decrease Font Size in All Editors", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.font.reset", "Reset Font Size in All Editors", null, MenuItemConfig.Type.ACTION));
        viewCat.addEntry(new ActionCatalogEntry("view.fullscreen", "Enter Full Screen", "EXPAND", MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(viewCat);

        ActionCatalogCategory runCat = new ActionCatalogCategory("Run");
        runCat.addEntry(new ActionCatalogEntry("middle.run", "Execute Statement", "PLAY", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.execute.query", "Execute Query\u2026", "PLAY", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.stop", "Stop Execution", "STOP", MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(runCat);
        rootCategories.add(mainMenuCat);

        // 3. External Build Systems
        ActionCatalogCategory buildCat = new ActionCatalogCategory("External Build Systems");
        buildCat.addEntry(new ActionCatalogEntry("maven.reload", "Maven: Reload Project", "SYNC", MenuItemConfig.Type.ACTION));
        buildCat.addEntry(new ActionCatalogEntry("maven.generate", "Maven: Generate Sources", "COG", MenuItemConfig.Type.ACTION));
        buildCat.addEntry(new ActionCatalogEntry("gradle.sync", "Gradle: Sync Project", "SYNC", MenuItemConfig.Type.ACTION));
        rootCategories.add(buildCat);

        // 4. Version Control Systems
        ActionCatalogCategory vcsCat = new ActionCatalogCategory("Version Control Systems");
        vcsCat.addEntry(new ActionCatalogEntry("vcs.commit", "Commit\u2026", "CHECK", MenuItemConfig.Type.ACTION));
        vcsCat.addEntry(new ActionCatalogEntry("vcs.push", "Push\u2026", "UPLOAD", MenuItemConfig.Type.ACTION));
        vcsCat.addEntry(new ActionCatalogEntry("vcs.update", "Update Project\u2026", "SYNC", MenuItemConfig.Type.ACTION));
        vcsCat.addEntry(new ActionCatalogEntry("vcs.pull", "Pull\u2026", "DOWNLOAD", MenuItemConfig.Type.ACTION));
        vcsCat.addEntry(new ActionCatalogEntry("vcs.branches", "Branches\u2026", "CODE_BRANCH", MenuItemConfig.Type.ACTION));
        vcsCat.addEntry(new ActionCatalogEntry("vcs.merge", "Merge Changes\u2026", null, MenuItemConfig.Type.ACTION));
        vcsCat.addEntry(new ActionCatalogEntry("vcs.diff", "Show Diff", null, MenuItemConfig.Type.ACTION));
        vcsCat.addEntry(new ActionCatalogEntry("vcs.rollback", "Rollback\u2026", "UNDO", MenuItemConfig.Type.ACTION));
        rootCategories.add(vcsCat);

        // 5. Database
        ActionCatalogCategory dbCat = new ActionCatalogCategory("Database");
        dbCat.addEntry(new ActionCatalogEntry("file.new.console", "New Query Console", "TERMINAL", MenuItemConfig.Type.ACTION));
        dbCat.addEntry(new ActionCatalogEntry("middle.database", "Database Explorer", "DATABASE", MenuItemConfig.Type.ACTION));
        dbCat.addEntry(new ActionCatalogEntry("file.export.data", "Export Data\u2026", "FILE_EXPORT", MenuItemConfig.Type.ACTION));
        dbCat.addEntry(new ActionCatalogEntry("file.import.data", "Import Data\u2026", "FILE_IMPORT", MenuItemConfig.Type.ACTION));
        dbCat.addEntry(new ActionCatalogEntry("file.new.database", "New Database", "DATABASE", MenuItemConfig.Type.ACTION));
        dbCat.addEntry(new ActionCatalogEntry("file.new.role", "New Role", "USER_SHIELD", MenuItemConfig.Type.ACTION));
        dbCat.addEntry(new ActionCatalogEntry("file.new.user", "New User", "USER", MenuItemConfig.Type.ACTION));
        dbCat.addEntry(new ActionCatalogEntry("file.new.virtualview", "New Virtual View", "TABLE", MenuItemConfig.Type.ACTION));
        dbCat.addEntry(new ActionCatalogEntry("file.new.datasource.templates", "Data Source Templates", "LAYER_GROUP", MenuItemConfig.Type.ACTION));
        dbCat.addEntry(new ActionCatalogEntry("file.new.driver", "Manage Drivers\u2026", "PLUG", MenuItemConfig.Type.ACTION));
        rootCategories.add(dbCat);

        // 6. Debug Actions
        ActionCatalogCategory debugCat = new ActionCatalogCategory("Debug Actions");
        debugCat.addEntry(new ActionCatalogEntry("middle.run", "Run", "PLAY", MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.start", "Debug", "BUG", MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.stop", "Stop", "STOP", MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.step.into", "Step Into", null, MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.step.out", "Step Out", null, MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.force.step.over", "Force Step Over", null, MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.force.step.into", "Force Step Into", null, MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.smart.step.into", "Smart Step Into", null, MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.run.to.cursor", "Run to Cursor", null, MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.evaluate.expression", "Evaluate Expression\u2026", null, MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.view.breakpoints", "View Breakpoints\u2026", null, MenuItemConfig.Type.ACTION));
        debugCat.addEntry(new ActionCatalogEntry("debug.mute.breakpoints", "Mute Breakpoints", null, MenuItemConfig.Type.ACTION));
        rootCategories.add(debugCat);

        // 7. Plugins
        ActionCatalogCategory pluginsCat = new ActionCatalogCategory("Plugins");
        pluginsCat.addEntry(new ActionCatalogEntry("plugin.dbtools", "Database Tools and SQL", "DATABASE", MenuItemConfig.Type.ACTION));
        pluginsCat.addEntry(new ActionCatalogEntry("plugin.git", "Git Integration", "CODE_BRANCH", MenuItemConfig.Type.ACTION));
        pluginsCat.addEntry(new ActionCatalogEntry("plugin.terminal", "Terminal Integration", "TERMINAL", MenuItemConfig.Type.ACTION));
        pluginsCat.addEntry(new ActionCatalogEntry("plugin.markdown", "Markdown Support", "FILE_ALT", MenuItemConfig.Type.ACTION));
        rootCategories.add(pluginsCat);

        // 7. Other
        ActionCatalogCategory otherCat = new ActionCatalogCategory("Other");
        otherCat.addEntry(new ActionCatalogEntry("header.search", "Search Everywhere", "SEARCH", MenuItemConfig.Type.ACTION));
        otherCat.addEntry(new ActionCatalogEntry("file.settings", "Settings", "COG", MenuItemConfig.Type.ACTION));
        otherCat.addEntry(new ActionCatalogEntry("view.presentation", "Toggle Presentation Mode", null, MenuItemConfig.Type.ACTION));
        otherCat.addEntry(new ActionCatalogEntry("help.check.updates", "Check for Updates\u2026", null, MenuItemConfig.Type.ACTION));
        otherCat.addEntry(new ActionCatalogEntry("help.about", "About", "INFO_CIRCLE", MenuItemConfig.Type.ACTION));
        rootCategories.add(otherCat);

        return rootCategories;
    }

    /**
     * Dynamically updates the enabled/disabled state of all active menus and buttons.
     */
    public void updateActions(MainWindow ctx) {
        Platform.runLater(() -> {
            boundMenuItems.removeIf(b -> {
                MenuItem item = b.itemRef().get();
                if (item == null) return true;
                item.setDisable(!b.action().isEnabled(ctx));
                if (item instanceof CheckMenuItem checkItem && b.action() instanceof ToggleAction toggle) {
                    checkItem.setSelected(toggle.isSelected(ctx));
                }
                return false;
            });

            boundButtons.removeIf(b -> {
                Button btn = b.buttonRef().get();
                if (btn == null) return true;
                btn.setDisable(!b.action().isEnabled(ctx));
                return false;
            });
        });
    }
}

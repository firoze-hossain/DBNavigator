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

    public void registerAction(String id, AnAction action) {
        actionMap.put(id, action);
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

        ActionCatalogCategory navCat = new ActionCatalogCategory("Navigate");
        navCat.addEntry(new ActionCatalogEntry("nav.back", "Back", "ARROW_LEFT", MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.forward", "Forward", "ARROW_RIGHT", MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("header.search", "Search Everywhere", "SEARCH", MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.class", "Go to Class\u2026", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.file", "Go to File\u2026", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.symbol", "Go to Symbol\u2026", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.text", "Text\u2026", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.database.object", "Go To Database Object", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.row", "Row\u2026", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.open.file.uri", "Open File URI", "FOLDER", MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.open.url", "Open URL", "GLOBE", MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.related.rows", "Related Rows", "TABLE", MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.line.column", "Go to Line:Column\u2026", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.next.highlighted.error", "Next Highlighted Error", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.prev.highlighted.error", "Previous Highlighted Error", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.next.emmet.edit.point", "Next Emmet Edit Point", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.prev.emmet.edit.point", "Previous Emmet Edit Point", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.last.edit.location", "Last Edit Location", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.next.edit.location", "Next Edit Location", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.file.next.statement", "Next Statement", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.file.prev.statement", "Previous Statement", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.file.matching.brace", "Move Caret to Matching Brace", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.next.template.parameter", "Next Template Parameter", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.prev.template.parameter", "Previous Template Parameter", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.custom.folding", "Custom Folding\u2026", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.next.change", "Next Change", "ARROW_DOWN", MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.prev.change", "Previous Change", "ARROW_UP", MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.select.in", "Select In\u2026", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.jump.to.nav.bar", "Jump to Navigation Bar", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.declaration", "Declaration or Usages", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.implementation", "Implementation(s)", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.type.declaration", "Type Declaration", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.super.method", "Super Method", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.goto.test", "Test", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.related.symbol", "Related Symbol\u2026", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.file.structure", "File Structure", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.file.path", "File Path", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.type.hierarchy", "Type Hierarchy", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.method.hierarchy", "Method Hierarchy", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.call.hierarchy", "Call Hierarchy", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.prev.occurrence", "Previous Occurrence", "ARROW_UP", MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.next.occurrence", "Next Occurrence", "ARROW_DOWN", MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.dbe.next.statement", "Next Statement", null, MenuItemConfig.Type.ACTION));
        navCat.addEntry(new ActionCatalogEntry("nav.dbe.prev.statement", "Previous Statement", null, MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(navCat);

        ActionCatalogCategory codeCat = new ActionCatalogCategory("Code");
        codeCat.addEntry(new ActionCatalogEntry("code.override.methods", "Override Methods\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.implement.methods", "Implement Methods\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.generate", "Generate\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.completion.basic", "Basic", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.completion.type.matching", "Type-Matching", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.completion.complete.statement", "Complete Current Statement", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.completion.cyclic.expand.word", "Cyclic Expand Word", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.completion.cyclic.expand.word.backward", "Cyclic Expand Word (Backward)", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.completion.call.inline", "Call Inline Completion", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.completion.insert.inline.proposal", "Insert Inline Proposal", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.completion.insert.inline.word", "Insert Inline Proposal's Word", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.completion.insert.inline.line", "Insert Inline Proposal's Line", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.insert.live.template", "Insert Live Template\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.save.live.template", "Save as Live Template\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.surround.with", "Surround With\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.inspect.code", "Inspect Code\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.cleanup", "Code Cleanup\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.silent.cleanup", "Silent Code Cleanup", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.run.inspection.by.name", "Run Inspection by Name\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.configure.analysis", "Configure Current File Analysis\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.view.offline.results", "View Offline Inspection Results\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.analyze.dataflow.to.here", "Analyze Data Flow to Here\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.analyze.dataflow.from.here", "Analyze Data Flow from Here\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.analyze.platform.menu", "AnalyzePlatformMenu", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.expand", "Expand", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.expand.recursively", "Expand Recursively", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.expand.all", "Expand All", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.collapse", "Collapse", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.collapse.recursively", "Collapse Recursively", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.collapse.all", "Collapse All", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.expand.doc.comments", "Expand Doc Comments", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.collapse.doc.comments", "Collapse Doc Comments", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.toggle", "Toggle Folding", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.fold.selection", "Fold Selection / Remove region", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.folding.fold.block", "Fold Code Block", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.comment.line", "// Comment with Line Comment", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.comment.block", "Comment with Block Comment", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.reformat", "Reformat Code", "INDENT", MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.reformat.json", "Reformat JSON", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.reformat.file", "Reformat File\u2026", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.auto.indent", "Auto-Indent Lines", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.optimize.imports", "Optimize Imports", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.rearrange.code", "Rearrange Code", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.move.statement.down", "Move Statement Down", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.move.statement.up", "Move Statement Up", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.move.element.left", "Move Element Left", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.move.element.right", "Move Element Right", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.move.line.down", "Move Line Down", null, MenuItemConfig.Type.ACTION));
        codeCat.addEntry(new ActionCatalogEntry("code.move.line.up", "Move Line Up", null, MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(codeCat);

        ActionCatalogCategory refactorCat = new ActionCatalogCategory("Refactor");
        refactorCat.addEntry(new ActionCatalogEntry("refactor.this", "Refactor This\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.rename", "Rename\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.change.signature", "Change Signature\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.modify.object", "Modify Object\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.introduce.variable", "Introduce Variable\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.extract.routine", "Extract Routine\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.table.alias", "Table alias\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.introduce.constant", "Introduce Constant\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.introduce.field", "Introduce Field\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.introduce.parameter", "Introduce Parameter\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.introduce.parameter.object", "Introduce Parameter Object\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.extract.method", "Extract Method\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.extract.delegate", "Extract Delegate\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.include.file", "Include File\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.extract.interface", "Extract Interface\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.extract.superclass", "Extract Superclass\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.extract.module", "Extract Module\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.subquery.cte", "Subquery as CTE", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.inline", "Inline\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.move", "Move\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.copy", "Copy\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.safe.delete", "Safe Delete\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.pull.members.up", "Pull Members Up\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.push.members.down", "Push Members Down\u2026", null, MenuItemConfig.Type.ACTION));
        refactorCat.addEntry(new ActionCatalogEntry("refactor.invert.boolean", "Invert Boolean\u2026", null, MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(refactorCat);

        ActionCatalogCategory runCat = new ActionCatalogCategory("Run");
        runCat.addEntry(new ActionCatalogEntry("run.run", "Run", "PLAY", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.debug", "Debug", "BUG", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.coverage", "Run with Coverage", "SHIELD", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.profiler", "Run with Profiler", "TACHOMETER", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.run.ellipsis", "Run\u2026", "PLAY", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.debug.ellipsis", "Debug\u2026", "BUG", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.attach.to.process", "Attach to Process\u2026", "RETWEET", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.edit.configurations", "Edit Configurations\u2026", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.manage.targets", "Manage Targets\u2026", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.stop", "Stop", "STOP", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.stop.background.processes", "Stop Background Processes\u2026", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.show.running.list", "Show Running List", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.compile.reload.modified.files", "Compile and Reload Modified Files", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.update.running.app", "Update Running Application", "REFRESH", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.step.over", "Step Over", "STEP_FORWARD", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.force.step.over", "Force Step Over", "STEP_FORWARD", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.step.into", "Step Into", "ARROW_DOWN", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.force.step.into", "Force Step Into", "ANGLE_DOUBLE_DOWN", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.smart.step.into", "Smart Step Into", "ARROW_RIGHT", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.step.out", "Step Out", "ARROW_UP", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.run.to.cursor", "Run to Cursor", "CROSSHAIRS", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.force.run.to.cursor", "Force Run to Cursor", "CROSSHAIRS", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.reset.frame", "Reset Frame", "UNDO", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.pause.program", "Pause Program", "PAUSE", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.resume.program", "Resume Program", "FORWARD", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.evaluate.expression", "Evaluate Expression\u2026", "CALCULATOR", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.show.execution.point", "Show Execution Point", "CROSSHAIRS", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.restore.breakpoint", "Restore Breakpoint", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.toggle.line.breakpoint", "Toggle Line Breakpoint", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.toggle.temporary.line.breakpoint", "Toggle Temporary Line Breakpoint", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.view.breakpoints", "View Breakpoints\u2026", "CIRCLE", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.test.history", "Test History", "HISTORY", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.import.tests.from.file", "Import Tests from File\u2026", "DOWNLOAD", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.manage.coverage.reports", "Manage Coverage Reports\u2026", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.generate.coverage.report", "Generate Coverage Report\u2026", "SHARE_SQUARE", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.hide.coverage", "Hide Coverage", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.attach.profiler.to.process", "Attach Profiler to Process\u2026", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.open.profiler.snapshot", "Open Profiler Snapshot", null, MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("middle.run", "Execute Statement", "PLAY", MenuItemConfig.Type.ACTION));
        runCat.addEntry(new ActionCatalogEntry("run.execute.query", "Execute Query\u2026", "PLAY", MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(runCat);

        ActionCatalogCategory toolsCat = new ActionCatalogCategory("Tools");
        toolsCat.addEntry(new ActionCatalogEntry("tools.view.psi.structure", "View PSI Structure\u2026", null, MenuItemConfig.Type.ACTION));
        toolsCat.addEntry(new ActionCatalogEntry("tools.view.psi.structure.current.file", "View PSI Structure of Current File\u2026", null, MenuItemConfig.Type.ACTION));
        toolsCat.addEntry(new ActionCatalogEntry("tools.create.command.line.launcher", "Create Command Line Launcher\u2026", null, MenuItemConfig.Type.ACTION));
        toolsCat.addEntry(new ActionCatalogEntry("tools.create.desktop.entry", "Create Desktop Entry\u2026", null, MenuItemConfig.Type.ACTION));
        toolsCat.addEntry(new ActionCatalogEntry("tools.services", "Services", null, MenuItemConfig.Type.ACTION));
        toolsCat.addEntry(new ActionCatalogEntry("tools.convert.schema", "Convert Schema\u2026", null, MenuItemConfig.Type.ACTION));
        toolsCat.addEntry(new ActionCatalogEntry("tools.markdown.import.word", "Import Word Document\u2026", null, MenuItemConfig.Type.ACTION));
        toolsCat.addEntry(new ActionCatalogEntry("tools.markdown.export.file.to", "Export Markdown File To\u2026", null, MenuItemConfig.Type.ACTION));
        toolsCat.addEntry(new ActionCatalogEntry("tools.markdown.configure.pandoc", "Configure Pandoc\u2026", null, MenuItemConfig.Type.ACTION));
        toolsCat.addEntry(new ActionCatalogEntry("tools.external.tools", "External Tools", null, MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(toolsCat);

        ActionCatalogCategory gitCat = new ActionCatalogCategory("Git");
        gitCat.addEntry(new ActionCatalogEntry("vcs.enable.integration", "Enable Version Control Integration\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("vcs.operations.popup", "VCS Operations Popup\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("vcs.commit", "Commit\u2026", "CHECK_CIRCLE", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("vcs.toggle.commit.ui", "Toggle Commit UI\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("vcs.update.project", "Update Project", "DOWNLOAD", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("vcs.integrate.project", "Integrate Project", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("vcs.refresh", "Refresh", "REFRESH", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("vcs.show.local.changes.uml", "Show Local Changes as UML", "DIAGRAM", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.commit.file", "Commit File", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.add", "Add", "PLUS", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.add.to.gitignore", "Add to .gitignore", "BAN", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.annotate", "Annotate", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.compare.same.version", "Compare with Same Repository Version", "COMPARE", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.compare.with.revision", "Compare with Revision\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.compare.with.branch", "Compare with Branch or Tag\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.show.history", "Show History", "HISTORY", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.show.history.for.selection", "Show History for Selection\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.rollback", "Rollback\u2026", "UNDO", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.push", "Push\u2026", "UPLOAD", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.pull", "Pull\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.fetch", "Fetch", "DOWNLOAD", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.merge", "Merge\u2026", "BRANCHES", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.abort.merge", "Abort Merge", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.rebase", "Rebase\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.abort.rebase", "Abort Rebase", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.continue.rebase", "Continue Rebase", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.skip.commit", "Skip Commit", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.branches", "Branches\u2026", "BRANCHES", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.new.branch", "New Branch\u2026", "PLUS", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.new.tag", "New Tag\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.reset.head", "Reset HEAD\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.stash.changes", "Stash Changes\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.unstash.changes", "Unstash Changes\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.manage.remotes", "Manage Remotes\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.clone", "Clone\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.abort.revert", "Abort Revert", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.abort.cherry.pick", "Abort Cherry-Pick", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.create.patch", "Create Patch from Local Changes\u2026", "PLUS", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.apply.patch", "Apply Patch\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.apply.patch.from.clipboard", "Apply Patch from Clipboard\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.shelve.changes", "Shelve Changes\u2026", "SAVE", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("vcs.get.from.vcs", "Get from Version Control\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("vcs.browse.git.log", "Show Git Repository Log\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("vcs.create.git.repository", "Create Git Repository\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.unshallow", "Unshallow repository", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.resolve.conflicts", "Resolve Conflicts\u2026", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.revert.resolved", "Revert Resolved", "UNDO", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.show.vcs.log", "Show VCS Log", "BRANCHES", MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.show.shelf", "Show Shelf", null, MenuItemConfig.Type.ACTION));
        gitCat.addEntry(new ActionCatalogEntry("git.show.git.stash", "Show Git Stash", null, MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(gitCat);

        ActionCatalogCategory windowCat = new ActionCatalogCategory("Window");
        windowCat.addEntry(new ActionCatalogEntry("window.minimize", "Minimize", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.zoom", "Zoom", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.layouts.default", "Default", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.layouts.list", "Tool Window Layout List", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.layouts.restore.current", "Restore Current Layout", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.layouts.save.changes.current", "Save Changes in Current Layout", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.layouts.save.as.new", "Save Current Layout as New\u2026", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.hide.active", "Hide Active Tool Window", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.hide.side", "Hide Side Tool Windows", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.hide.all", "Hide All Tool Windows", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.open.as.editor.tab", "Open as Editor Tab", "EXTERNAL_LINK_ALT", MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.pin.tab", "Pin Active Tool Window Tab", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.close.active.tab", "Close Active Tab", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.jump.last", "Jump to Last Tool Window", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.maximize", "Maximize Tool Window", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.dock", "Dock", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.view.mode", "View Mode", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.move.to", "Move to", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.group.tabs", "Group Tabs", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.toolwindow.show.list.of.tabs", "Show List of Tabs", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.tw.resize.left", "Stretch to Left", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.tw.resize.right", "Stretch to Right", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.tw.resize.top", "Stretch to Top", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.tw.resize.bottom", "Stretch to Bottom", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.next.tab", "Select Next Tab", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.prev.tab", "Select Previous Tab", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.pin.tab", "Pin Active Tab", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.keep.tab.open", "Keep Tab Open", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.show.hidden.tabs", "Show Hidden Tabs", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.close.tab", "Close Tab", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.close.other.tabs", "Close Other Tabs", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.close.all.tabs", "Close All Tabs", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.close.unmodified.tabs", "Close Unmodified Tabs", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.close.all.but.pinned", "Close All but Pinned", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.close.tabs.left", "Close Tabs to the Left", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.close.tabs.right", "Close Tabs to the Right", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.close.all.readonly", "Close All Read-Only Tabs", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.open.as.editor.tab", "Open as Editor Tab", "EXTERNAL_LINK_ALT", MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.reopen.tab", "Reopen Closed Tab", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.right", "Split Right", "COLUMNS", MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.and.move.right", "Split and Move Right", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.down", "Split Down", "COLUMNS", MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.and.move.down", "Split and Move Down", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.chooser.open", "Open in Split with Chooser\u2026", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.next", "Next Split", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.prev", "Previous Split", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.exit.chooser", "Exit Chooser", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.chooser.split", "Split", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.chooser.duplicate", "Duplicate", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.chooser.without.split", "Without Split", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.switch.up", "Use Top Split or Switch Up", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.switch.left", "Use Left Split or Switch Left", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.switch.down", "Use down Split or Switch Down", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.split.switch.right", "Use Right Split or Switch Right", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.stretch.top", "Stretch Editor to Top", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.stretch.left", "Stretch Editor to Left", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.stretch.bottom", "Stretch Editor to Bottom", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.stretch.right", "Stretch Editor to Right", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.change.splitter.orientation", "Change Splitter Orientation", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.maximize.splits", "Maximize Editor/Normalize Splits", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.unsplit", "Unsplit", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.unsplit.all", "Unsplit All", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.goto.next.splitter", "Goto Next Splitter", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.goto.prev.splitter", "Goto Previous Splitter", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.editor.configure.tabs", "Configure Editor Tabs\u2026", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.notifications.close.first", "Close First", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.notifications.close.all", "Close All", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.background.tasks.show", "Show", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.background.tasks.auto.show", "Auto Show", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.next.project", "Next Project Window", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.prev.project", "Previous Project Window", null, MenuItemConfig.Type.ACTION));
        windowCat.addEntry(new ActionCatalogEntry("window.project.merge.all", "Merge All Project Windows", null, MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(windowCat);

        ActionCatalogCategory helpCat = new ActionCatalogCategory("Help");
        helpCat.addEntry(new ActionCatalogEntry("help.find.action", "Find Action\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.help", "Help", "QUESTION_CIRCLE", MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.learn.features", "Learn IDE Features", "GRADUATION_CAP", MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.whats.new", "What's New", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.configure.new.ui", "Configure the New UI", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.getting.started", "Getting Started", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.youtube", "DataGrip on YouTube", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.shortcuts.pdf", "Keyboard Shortcuts PDF", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.tip.of.the.day", "Tip of the Day", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.my.productivity", "My Productivity", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.contact.support", "Contact Support\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.bug.report", "Submit a Bug Report\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.submit.feedback", "Submit Feedback\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.show.log.in.files", "Show Log in Finder", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.show.sql.log.in.files", "Show SQL Log", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.collect.logs", "Collect Logs and Diagnostic Data", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.delete.leftover.dirs", "Delete Leftover IDE Directories\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.diagnostic.activity.monitor", "Activity Monitor\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.diagnostic.dump.threads", "Dump Threads", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.diagnostic.run.memory.tester", "Run Memory Tester\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.diagnostic.debug.log.settings", "Debug Log Settings\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.diagnostic.special.files", "Special Files and Folders", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.diagnostic.start.cpu.profiling", "Start CPU Usage Profiling", "TACHOMETER_ALT", MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.diagnostic.start.async.profiler", "Start Async Profiler", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.diagnostic.capture.memory.snapshot", "Capture Memory Snapshot", "CAMERA", MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.diagnostic.profile.indexing", "Profile Indexing", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.diagnostic.open.indexing.diagnostics", "Open Indexing Diagnostics", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.change.memory.settings", "Change Memory Settings", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.custom.properties", "Edit Custom Properties\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.custom.vm.options", "Edit Custom VM Options\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.registration.register", "Register\u2026", null, MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.updates", "Check for Updates\u2026", "DOWNLOAD", MenuItemConfig.Type.ACTION));
        helpCat.addEntry(new ActionCatalogEntry("help.about", "About", "INFO_CIRCLE", MenuItemConfig.Type.ACTION));
        mainMenuCat.addSubCategory(helpCat);

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

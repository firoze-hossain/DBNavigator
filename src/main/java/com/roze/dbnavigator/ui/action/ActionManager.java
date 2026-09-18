package com.roze.dbnavigator.ui.action;

import com.roze.dbnavigator.ui.MainWindow;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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

    private javafx.scene.control.Menu buildMenu(ActionGroup group, MainWindow ctx) {
        javafx.scene.control.Menu menu = new javafx.scene.control.Menu(group.getText());
        if (group.getIcon() != null) {
            menu.setGraphic(com.roze.dbnavigator.ui.Icons.of(group.getIcon(), group.getIconColor(), group.getIconSize()));
        }

        for (AnAction child : group.getChildren()) {
            if (child instanceof ActionSeparator) {
                menu.getItems().add(new SeparatorMenuItem());
            } else if (child instanceof ActionGroup subGroup) {
                menu.getItems().add(buildMenu(subGroup, ctx));
            } else {
                MenuItem item = child.createMenuItem(ctx);
                boundMenuItems.add(new BoundItem(new WeakReference<>(item), child));
                menu.getItems().add(item);
            }
        }

        return menu;
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
     * Dynamically updates the enabled/disabled state of all active menus and buttons.
     */
    public void updateActions(MainWindow ctx) {
        Platform.runLater(() -> {
            boundMenuItems.removeIf(b -> {
                MenuItem item = b.itemRef().get();
                if (item == null) return true;
                item.setDisable(!b.action().isEnabled(ctx));
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

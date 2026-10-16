package com.roze.dbnavigator.ui.action;

import com.roze.dbnavigator.ui.Icons;
import com.roze.dbnavigator.ui.MainWindow;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DataGrip/IntelliJ-style ActionGroup.
 * Groups multiple actions into menus, submenus, toolbars, or popup buttons.
 */
public class ActionGroup extends AnAction {

    private final List<AnAction> children = new ArrayList<>();
    private final boolean popup;
    private java.util.function.Predicate<MainWindow> groupEnabledPredicate;
    private java.util.function.Predicate<MainWindow> groupVisiblePredicate;

    public ActionGroup(String id, String text, boolean popup, org.kordamp.ikonli.fontawesome5.FontAwesomeSolid icon, String iconColor, int iconSize, String description, java.util.function.Predicate<MainWindow> enabledPredicate) {
        super(AnAction.builder(id, text)
                .icon(icon, iconColor, iconSize)
                .description(description)
                .enabledWhen(enabledPredicate)
                .onAction(ctx -> {}));
        this.popup = popup;
        this.groupEnabledPredicate = enabledPredicate;
    }

    public ActionGroup(String id, String text, boolean popup, org.kordamp.ikonli.fontawesome5.FontAwesomeSolid icon, String iconColor, int iconSize, String description) {
        this(id, text, popup, icon, iconColor, iconSize, description, null);
    }

    public ActionGroup(String id, String text, boolean popup, java.util.function.Predicate<MainWindow> enabledPredicate) {
        this(id, text, popup, null, null, 12, null, enabledPredicate);
    }

    public ActionGroup(String id, String text, boolean popup) {
        this(id, text, popup, null, null, 12, null, null);
    }

    public ActionGroup(String id, String text) {
        this(id, text, false, null, null, 12, null, null);
    }

    public ActionGroup enabledWhen(java.util.function.Predicate<MainWindow> predicate) {
        this.groupEnabledPredicate = predicate;
        return this;
    }

    public ActionGroup visibleWhen(java.util.function.Predicate<MainWindow> predicate) {
        this.groupVisiblePredicate = predicate;
        return this;
    }

    @Override
    public boolean isEnabled(MainWindow ctx) {
        if (groupEnabledPredicate != null) {
            try {
                return groupEnabledPredicate.test(ctx);
            } catch (Exception e) {
                return false;
            }
        }
        return super.isEnabled(ctx);
    }

    @Override
    public boolean isVisible(MainWindow ctx) {
        if (groupVisiblePredicate != null) {
            try {
                return groupVisiblePredicate.test(ctx);
            } catch (Exception e) {
                return false;
            }
        }
        return super.isVisible(ctx);
    }

    public boolean isPopup() { return popup; }

    public ActionGroup add(AnAction action) {
        children.add(action);
        return this;
    }

    public ActionGroup addAll(AnAction... actions) {
        Collections.addAll(children, actions);
        return this;
    }

    public ActionGroup addSeparator() {
        children.add(ActionSeparator.getInstance());
        return this;
    }

    private java.util.function.BiConsumer<Menu, MainWindow> onMenuShowing;
    private java.util.function.Function<MainWindow, String> groupDynamicTextFunction;

    public ActionGroup dynamicText(java.util.function.Function<MainWindow, String> dynamicTextFunction) {
        this.groupDynamicTextFunction = dynamicTextFunction;
        return this;
    }

    @Override
    public boolean hasDynamicText() {
        return groupDynamicTextFunction != null || super.hasDynamicText();
    }

    @Override
    public String getText(MainWindow ctx) {
        if (groupDynamicTextFunction != null) {
            try {
                return groupDynamicTextFunction.apply(ctx);
            } catch (Exception e) {
                return getText();
            }
        }
        return super.getText(ctx);
    }

    public ActionGroup setOnMenuShowing(java.util.function.BiConsumer<Menu, MainWindow> onMenuShowing) {
        this.onMenuShowing = onMenuShowing;
        return this;
    }

    public java.util.function.BiConsumer<Menu, MainWindow> getOnMenuShowing() {
        return onMenuShowing;
    }

    public List<AnAction> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /** Creates a JavaFX Menu containing all children. */
    public Menu createMenu(MainWindow ctx) {
        Menu menu = new Menu(getText(ctx));
        if (getIcon() != null) {
            menu.setGraphic(Icons.of(getIcon(), "#a9b7c6", 11));
        }
        populateMenuItems(menu.getItems(), ctx);
        if (onMenuShowing != null) {
            menu.setOnShowing(e -> onMenuShowing.accept(menu, ctx));
        }
        return menu;
    }

    /** Creates a JavaFX ContextMenu containing all children. */
    public ContextMenu createContextMenu(MainWindow ctx) {
        ContextMenu contextMenu = new ContextMenu();
        populateMenuItems(contextMenu.getItems(), ctx);
        return contextMenu;
    }

    private void populateMenuItems(List<MenuItem> targetList, MainWindow ctx) {
        for (AnAction child : children) {
            if (child instanceof ActionSeparator) {
                targetList.add(new SeparatorMenuItem());
            } else if (child instanceof ActionGroup group) {
                if (group.isPopup()) {
                    targetList.add(group.createMenu(ctx));
                } else {
                    group.populateMenuItems(targetList, ctx);
                }
            } else {
                targetList.add(child.createMenuItem(ctx));
            }
        }
    }

    /** Creates an HBox toolbar containing buttons for each child action. */
    public HBox createToolBar(MainWindow ctx) {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        for (AnAction child : children) {
            if (child instanceof ActionSeparator sep) {
                bar.getChildren().add(sep.createToolbarSeparator());
            } else if (child instanceof ActionGroup group && group.isPopup()) {
                bar.getChildren().add(group.createPopupButton(ctx));
            } else {
                bar.getChildren().add(child.createIconButton(ctx));
            }
        }
        return bar;
    }

    /**
     * Creates a button (like the three dots "...") that displays a popup
     * ContextMenu containing its child actions when clicked.
     */
    public Button createPopupButton(MainWindow ctx) {
        Button btn = new Button();
        btn.getStyleClass().add("header-action-button");
        if (getIcon() != null) {
            btn.setGraphic(Icons.of(getIcon(), "#a9b7c6", 13));
        } else {
            btn.setText(getText());
        }

        String tip = getDescription() != null && !getDescription().isBlank()
                ? getDescription() : getText();
        btn.setTooltip(new Tooltip(tip));

        btn.setOnAction(e -> {
            ContextMenu menu = createContextMenu(ctx);
            menu.show(btn, Side.BOTTOM, 0, 4);
        });

        return btn;
    }
}

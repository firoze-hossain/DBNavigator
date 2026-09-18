package com.roze.dbnavigator.ui.action;

import com.roze.dbnavigator.ui.MainWindow;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Marker action representing a separator in a menu, context menu, or toolbar.
 */
public class ActionSeparator extends AnAction {

    private static final ActionSeparator INSTANCE = new ActionSeparator();

    public ActionSeparator() {
        super(AnAction.builder("separator", "").onAction(ctx -> {}));
    }

    public static ActionSeparator getInstance() {
        return INSTANCE;
    }

    @Override
    public MenuItem createMenuItem(MainWindow ctx) {
        return new SeparatorMenuItem();
    }

    public Node createToolbarSeparator() {
        Separator sep = new Separator(Orientation.VERTICAL);
        sep.getStyleClass().add("action-separator");
        return sep;
    }
}

package com.roze.dbnavigator.ui.action;

import com.roze.dbnavigator.ui.Icons;
import com.roze.dbnavigator.ui.MainWindow;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * DataGrip/IntelliJ-style action representation.
 * Represents an executable action in menus, toolbars, or context menus.
 */
public class AnAction {

    private final String id;
    private final String text;
    private final String description;
    private final FontAwesomeSolid icon;
    private final String iconColor;
    private final int iconSize;
    private final KeyCombination accelerator;
    private final Consumer<MainWindow> actionHandler;
    private final Predicate<MainWindow> enabledPredicate;

    public AnAction(Builder builder) {
        this.id = builder.id;
        this.text = builder.text;
        this.description = builder.description;
        this.icon = builder.icon;
        this.iconColor = builder.iconColor != null ? builder.iconColor : "#a9b7c6";
        this.iconSize = builder.iconSize > 0 ? builder.iconSize : 12;
        this.accelerator = builder.accelerator;
        this.actionHandler = builder.actionHandler;
        this.enabledPredicate = builder.enabledPredicate;
    }

    public String getId() { return id; }
    public String getText() { return text; }
    public String getDescription() { return description; }
    public FontAwesomeSolid getIcon() { return icon; }
    public KeyCombination getAccelerator() { return accelerator; }

    public boolean isEnabled(MainWindow ctx) {
        if (enabledPredicate == null) return true;
        try {
            return enabledPredicate.test(ctx);
        } catch (Exception e) {
            return true;
        }
    }

    public void perform(MainWindow ctx) {
        if (isEnabled(ctx) && actionHandler != null) {
            actionHandler.accept(ctx);
        }
    }

    /** Creates a JavaFX MenuItem wired to this action. */
    public MenuItem createMenuItem(MainWindow ctx) {
        MenuItem item = new MenuItem(text);
        if (icon != null) {
            item.setGraphic(Icons.of(icon, iconColor, iconSize));
        }
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        item.setOnAction(e -> perform(ctx));
        item.setDisable(!isEnabled(ctx));
        return item;
    }

    /** Creates a JavaFX icon Button for toolbars or header bars. */
    public Button createIconButton(MainWindow ctx) {
        Button btn = new Button();
        btn.getStyleClass().add("header-action-button");
        if (icon != null) {
            btn.setGraphic(Icons.of(icon, iconColor, iconSize));
        } else {
            btn.setText(text);
        }

        String tipText = text;
        if (description != null && !description.isBlank()) {
            tipText = description;
        }
        if (accelerator != null) {
            tipText += " (" + accelerator.getDisplayText() + ")";
        }
        btn.setTooltip(new Tooltip(tipText));
        btn.setOnAction(e -> perform(ctx));
        btn.setDisable(!isEnabled(ctx));
        return btn;
    }

    public static Builder builder(String id, String text) {
        return new Builder(id, text);
    }

    public static class Builder {
        private final String id;
        private final String text;
        private String description;
        private FontAwesomeSolid icon;
        private String iconColor;
        private int iconSize = 12;
        private KeyCombination accelerator;
        private Consumer<MainWindow> actionHandler;
        private Predicate<MainWindow> enabledPredicate;

        public Builder(String id, String text) {
            this.id = id;
            this.text = text;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder icon(FontAwesomeSolid icon, String color, int size) {
            this.icon = icon;
            this.iconColor = color;
            this.iconSize = size;
            return this;
        }

        public Builder icon(FontAwesomeSolid icon) {
            this.icon = icon;
            return this;
        }

        public Builder accelerator(KeyCombination accelerator) {
            this.accelerator = accelerator;
            return this;
        }

        public Builder onAction(Consumer<MainWindow> handler) {
            this.actionHandler = handler;
            return this;
        }

        public Builder onActionRunnable(Runnable handler) {
            this.actionHandler = ctx -> handler.run();
            return this;
        }

        public Builder enabledWhen(Predicate<MainWindow> predicate) {
            this.enabledPredicate = predicate;
            return this;
        }

        public AnAction build() {
            return new AnAction(this);
        }
    }
}

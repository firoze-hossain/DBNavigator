package com.roze.dbnavigator.ui.action;

import com.roze.dbnavigator.ui.Icons;
import com.roze.dbnavigator.ui.MainWindow;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * DataGrip/IntelliJ-style ToggleAction.
 * Represents a checkable action that can be toggled ON or OFF in menus.
 */
public class ToggleAction extends AnAction {

    private final Predicate<MainWindow> selectedPredicate;
    private final BiConsumer<MainWindow, Boolean> toggleConsumer;

    public ToggleAction(Builder builder) {
        super(builder);
        this.selectedPredicate = builder.selectedPredicate;
        this.toggleConsumer = builder.toggleConsumer;
    }

    public boolean isSelected(MainWindow ctx) {
        if (selectedPredicate == null) return false;
        try {
            return selectedPredicate.test(ctx);
        } catch (Exception e) {
            return false;
        }
    }

    public void setSelected(MainWindow ctx, boolean state) {
        if (toggleConsumer != null) {
            toggleConsumer.accept(ctx, state);
        }
    }

    @Override
    public MenuItem createMenuItem(MainWindow ctx) {
        CheckMenuItem item = new CheckMenuItem(getText());
        if (getIcon() != null) {
            item.setGraphic(Icons.of(getIcon(), getIconColor(), getIconSize()));
        }
        if (getAccelerator() != null) {
            item.setAccelerator(getAccelerator());
        }
        item.setSelected(isSelected(ctx));
        item.setDisable(!isEnabled(ctx));
        item.setOnAction(e -> {
            boolean selected = item.isSelected();
            setSelected(ctx, selected);
            perform(ctx);
        });
        return item;
    }

    public static Builder toggleBuilder(String id, String text) {
        return new Builder(id, text);
    }

    public static class Builder extends AnAction.Builder {
        private Predicate<MainWindow> selectedPredicate;
        private BiConsumer<MainWindow, Boolean> toggleConsumer;

        public Builder(String id, String text) {
            super(id, text);
        }

        public Builder isSelected(Predicate<MainWindow> predicate) {
            this.selectedPredicate = predicate;
            return this;
        }

        public Builder onToggle(BiConsumer<MainWindow, Boolean> consumer) {
            this.toggleConsumer = consumer;
            return this;
        }

        @Override
        public Builder description(String description) {
            super.description(description);
            return this;
        }

        @Override
        public Builder icon(FontAwesomeSolid icon, String color, int size) {
            super.icon(icon, color, size);
            return this;
        }

        @Override
        public Builder icon(FontAwesomeSolid icon) {
            super.icon(icon);
            return this;
        }

        @Override
        public Builder accelerator(KeyCombination accelerator) {
            super.accelerator(accelerator);
            return this;
        }

        @Override
        public Builder onAction(Consumer<MainWindow> handler) {
            super.onAction(handler);
            return this;
        }

        @Override
        public Builder onActionRunnable(Runnable handler) {
            super.onActionRunnable(handler);
            return this;
        }

        @Override
        public Builder enabledWhen(Predicate<MainWindow> predicate) {
            super.enabledWhen(predicate);
            return this;
        }

        @Override
        public ToggleAction build() {
            return new ToggleAction(this);
        }
    }
}

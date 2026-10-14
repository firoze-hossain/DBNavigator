package com.roze.dbnavigator.ui;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;

import java.util.List;
import java.util.function.Consumer;

/**
 * Quick-choice dropdown popup for boolean grid cells:
 * provides direct selection among "true", "false", "null", and "default".
 */
public class GridBooleanPicker {

    private static final List<String> OPTIONS = List.of("true", "false", "null", "default");

    private final Popup popup = new Popup();
    private final Consumer<String> onSelected;
    private final VBox container = new VBox(0);
    private int selectedIndex = 0;
    private final Label[] itemLabels = new Label[OPTIONS.size()];
    private Runnable onPopupClosed;

    public GridBooleanPicker(Consumer<String> onSelected) {
        this.onSelected = onSelected;

        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        container.getStyleClass().add("grid-boolean-picker");
        container.setFocusTraversable(true);

        for (int i = 0; i < OPTIONS.size(); i++) {
            final int idx = i;
            final String val = OPTIONS.get(i);
            Label label = new Label(val);
            label.getStyleClass().add("boolean-picker-item");
            label.setAlignment(Pos.CENTER_LEFT);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setOnMouseEntered(e -> setFocusedIndex(idx));
            label.setOnMouseClicked(e -> selectValue(val));
            itemLabels[i] = label;
            container.getChildren().add(label);
        }

        container.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP) {
                setFocusedIndex((selectedIndex - 1 + OPTIONS.size()) % OPTIONS.size());
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN) {
                setFocusedIndex((selectedIndex + 1) % OPTIONS.size());
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                selectValue(OPTIONS.get(selectedIndex));
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hide();
                e.consume();
            } else {
                String text = e.getText();
                if (text != null && !text.isEmpty()) {
                    char c = Character.toLowerCase(text.charAt(0));
                    if (c == 't') selectValue("true");
                    else if (c == 'f') selectValue("false");
                    else if (c == 'n') selectValue("null");
                    else if (c == 'd') selectValue("default");
                }
            }
        });

        popup.getContent().add(container);
        popup.setOnHidden(e -> {
            if (!committed && onPopupClosed != null) {
                onPopupClosed.run();
            }
        });
    }

    private boolean committed = false;

    private void setFocusedIndex(int index) {
        selectedIndex = index;
        for (int i = 0; i < itemLabels.length; i++) {
            if (i == index) {
                if (!itemLabels[i].getStyleClass().contains("selected")) {
                    itemLabels[i].getStyleClass().add("selected");
                }
            } else {
                itemLabels[i].getStyleClass().remove("selected");
            }
        }
    }

    private void selectValue(String value) {
        committed = true;
        try {
            if (onSelected != null) {
                onSelected.accept(value);
            }
        } finally {
            hide();
        }
    }

    public void setOnPopupClosed(Runnable r) {
        this.onPopupClosed = r;
    }

    public void show(Node anchor, String initialValue) {
        committed = false;
        if (anchor == null || anchor.getScene() == null || anchor.getScene().getWindow() == null) return;
        if (!anchor.getScene().getStylesheets().isEmpty()) {
            container.getStylesheets().setAll(anchor.getScene().getStylesheets());
        }

        int initialIdx = 0;
        if (initialValue != null) {
            for (int i = 0; i < OPTIONS.size(); i++) {
                if (OPTIONS.get(i).equalsIgnoreCase(initialValue.trim())) {
                    initialIdx = i;
                    break;
                }
            }
        }
        setFocusedIndex(initialIdx);

        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds == null) return;

        double width = Math.max(bounds.getWidth(), 80);
        container.setMinWidth(width);

        double x = bounds.getMinX();
        double y = bounds.getMaxY();

        try {
            Screen screen = Screen.getPrimary();
            for (Screen s : Screen.getScreensForRectangle(x, y, width, 120)) {
                screen = s;
                break;
            }
            double screenBottom = screen.getVisualBounds().getMaxY();
            if (y + 120 > screenBottom) {
                y = Math.max(screen.getVisualBounds().getMinY(), bounds.getMinY() - 120);
            }
        } catch (Exception ignored) {}

        popup.show(anchor, x, y);
        container.requestFocus();
    }

    public void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public Popup getPopup() {
        return popup;
    }
}

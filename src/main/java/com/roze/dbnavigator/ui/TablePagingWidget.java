package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modern table paging pill widget matching the reference IDE:
 * {@code [ <count> rows ⌄ | ⋮ ]}.
 * 
 * Includes:
 * - Count Rows
 * - Page Size selection (10, 100, 250, 500, 1000, All, Custom)
 * - Set as Default
 * - Move This Paging To Left / Right / Editor Toolbar
 */
public class TablePagingWidget extends HBox {

    public enum Placement {
        BOTTOM_CENTER,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOOLBAR
    }

    private final Button rowsButton = new Button();
    private final Button optionsButton = new Button();
    private final Region divider = new Region();

    private long currentFrom = 0;
    private long currentTo = 0;
    private long currentTotal = -1;
    private boolean currentExact = false;

    private int activePageSize = 500;
    private Placement placement = Placement.BOTTOM_CENTER;

    private Runnable onCountRows;
    private Consumer<Integer> onPageSizeChanged;
    private Consumer<Placement> onPlacementChanged;
    private Runnable onReload;
    private Runnable onFirst;
    private Runnable onPrev;
    private Runnable onNext;
    private Runnable onLast;

    public TablePagingWidget() {
        getStyleClass().add("result-paging-pill");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);

        rowsButton.getStyleClass().add("rows-button");
        rowsButton.setGraphicTextGap(6);
        rowsButton.setContentDisplay(ContentDisplay.RIGHT);
        rowsButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_DOWN, "#8c919a", 8));
        rowsButton.setOnAction(e -> showRowsMenu());

        divider.getStyleClass().add("pill-divider");

        optionsButton.getStyleClass().add("options-button");
        optionsButton.setGraphic(Icons.of(FontAwesomeSolid.ELLIPSIS_V, "#8c919a", 10));
        optionsButton.setTooltip(new Tooltip("Paging Options"));
        optionsButton.setOnAction(e -> showOptionsMenu());

        getChildren().addAll(rowsButton, divider, optionsButton);
        updateText();
    }

    public void setOnCountRows(Runnable r) {
        this.onCountRows = r;
    }

    public void setOnPageSizeChanged(Consumer<Integer> c) {
        this.onPageSizeChanged = c;
    }

    public void setOnPlacementChanged(Consumer<Placement> c) {
        this.onPlacementChanged = c;
    }

    public void setOnReload(Runnable r) {
        this.onReload = r;
    }

    public void setOnFirst(Runnable r) {
        this.onFirst = r;
    }

    public void setOnPrev(Runnable r) {
        this.onPrev = r;
    }

    public void setOnNext(Runnable r) {
        this.onNext = r;
    }

    public void setOnLast(Runnable r) {
        this.onLast = r;
    }

    public int getActivePageSize() {
        return activePageSize;
    }

    public void setActivePageSize(int size) {
        this.activePageSize = size;
        updateText();
    }

    public Placement getPlacement() {
        return placement;
    }

    public void setPlacement(Placement p) {
        this.placement = p;
    }

    public Button getRowsButton() {
        return rowsButton;
    }

    public Button getOptionsButton() {
        return optionsButton;
    }

    public void update(long from, long to, long total, boolean isExact) {
        this.currentFrom = from;
        this.currentTo = to;
        this.currentTotal = total;
        this.currentExact = isExact;
        updateText();
    }

    private void updateText() {
        long count = currentTo >= currentFrom && currentFrom > 0 ? (currentTo - currentFrom + 1) : 0;
        if (currentTotal >= 0 && currentExact && count == currentTotal) {
            rowsButton.setText(count + (count == 1 ? " row" : " rows"));
        } else if (currentFrom == 0 && currentTo == 0) {
            rowsButton.setText("0 rows");
        } else if (currentTotal >= 0 && currentExact) {
            rowsButton.setText(currentFrom + "\u2013" + currentTo + " of " + currentTotal + " rows");
        } else if (currentTotal > 0 && !currentExact) {
            rowsButton.setText(currentFrom + "\u2013" + currentTo + " of " + currentTotal + "+ rows");
        } else if (count > 0) {
            rowsButton.setText(count + (count == 1 ? " row" : " rows"));
        } else {
            rowsButton.setText("0 rows");
        }
    }

    public void showRowsMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem countRows = new MenuItem("Count Rows");
        countRows.setOnAction(e -> {
            if (onCountRows != null) onCountRows.run();
        });
        menu.getItems().add(countRows);

        boolean hasMultiPages = currentFrom > 1 || (currentTotal > 0 && currentTo < currentTotal)
                || (currentTotal < 0 && (currentTo - currentFrom + 1) >= activePageSize);
        if (hasMultiPages && (onPrev != null || onNext != null)) {
            menu.getItems().add(new SeparatorMenuItem());
            if (onPrev != null && currentFrom > 1) {
                MenuItem prev = new MenuItem("Previous Page");
                prev.setOnAction(e -> onPrev.run());
                menu.getItems().add(prev);
            }
            if (onNext != null && (!currentExact || currentTo < currentTotal)) {
                MenuItem next = new MenuItem("Next Page");
                next.setOnAction(e -> onNext.run());
                menu.getItems().add(next);
            }
        }

        menu.getItems().add(new SeparatorMenuItem());

        MenuItem header = new MenuItem("Page Size");
        header.setDisable(true);
        header.getStyleClass().add("menu-item-header");
        menu.getItems().add(header);

        int defaultPageSize = AppSettingsStore.load().getPageSize();
        if (defaultPageSize <= 0) defaultPageSize = 500;

        List<Integer> standardSizes = List.of(10, 100, 250, 500, 1000);
        for (int size : standardSizes) {
            String label = NumberFormat.getIntegerInstance(Locale.US).format(size);
            if (size == defaultPageSize) {
                label += " Default";
            }
            if (size == activePageSize) {
                label = "\u2713 " + label;
            } else {
                label = "   " + label;
            }
            MenuItem item = new MenuItem(label);
            item.setOnAction(e -> applyPageSize(size));
            menu.getItems().add(item);
        }

        // "All" option
        String allLabel = "All";
        if (defaultPageSize >= 100000) allLabel += " Default";
        if (activePageSize >= 100000) allLabel = "\u2713 " + allLabel;
        else allLabel = "   " + allLabel;
        MenuItem allItem = new MenuItem(allLabel);
        allItem.setOnAction(e -> applyPageSize(100000));
        menu.getItems().add(allItem);

        // "Custom..." option
        boolean isCustom = !standardSizes.contains(activePageSize) && activePageSize < 100000;
        String customLabel = (isCustom ? "\u2713 Custom (" + activePageSize + ")\u2026" : "   Custom\u2026");
        MenuItem customItem = new MenuItem(customLabel);
        customItem.setOnAction(e -> promptCustomPageSize());
        menu.getItems().add(customItem);

        menu.getItems().add(new SeparatorMenuItem());

        MenuItem setDefault = new MenuItem("Set as Default");
        setDefault.setOnAction(e -> {
            AppSettingsStore.Settings settings = AppSettingsStore.load();
            settings.setPageSize(activePageSize >= 100000 ? 100000 : activePageSize);
            AppSettingsStore.save(settings);
            if (onReload != null) onReload.run();
        });
        menu.getItems().add(setDefault);

        if (rowsButton.getScene() != null && rowsButton.getScene().getWindow() != null) {
            Side popupSide = placement == Placement.TOOLBAR ? Side.BOTTOM : Side.TOP;
            menu.show(rowsButton, popupSide, 0, 0);
        }
    }

    public void showOptionsMenu() {
        ContextMenu menu = new ContextMenu();

        if (placement != Placement.BOTTOM_CENTER) {
            MenuItem moveMiddle = new MenuItem("Move This Paging To Middle");
            moveMiddle.setOnAction(e -> changePlacement(Placement.BOTTOM_CENTER));
            menu.getItems().add(moveMiddle);
        }

        if (placement != Placement.BOTTOM_LEFT) {
            MenuItem moveLeft = new MenuItem("Move This Paging To Left");
            moveLeft.setOnAction(e -> changePlacement(Placement.BOTTOM_LEFT));
            menu.getItems().add(moveLeft);
        }

        if (placement != Placement.BOTTOM_RIGHT) {
            MenuItem moveRight = new MenuItem("Move This Paging To Right");
            moveRight.setOnAction(e -> changePlacement(Placement.BOTTOM_RIGHT));
            menu.getItems().add(moveRight);
        }

        if (placement != Placement.TOOLBAR) {
            MenuItem moveToolbar = new MenuItem("Move This Paging To Editor Toolbar");
            moveToolbar.setOnAction(e -> changePlacement(Placement.TOOLBAR));
            menu.getItems().add(moveToolbar);
        }

        if (optionsButton.getScene() != null && optionsButton.getScene().getWindow() != null) {
            Side popupSide = placement == Placement.TOOLBAR ? Side.BOTTOM : Side.TOP;
            menu.show(optionsButton, popupSide, 0, 0);
        }
    }

    public void applyPageSize(int size) {
        this.activePageSize = size;
        if (onPageSizeChanged != null) {
            onPageSizeChanged.accept(size);
        }
    }

    public void promptCustomPageSize() {
        javafx.stage.Window owner = null;
        if (getScene() != null && getScene().getWindow() != null) {
            owner = getScene().getWindow();
        }
        Optional<Integer> res = ChangePageSizeDialog.show(activePageSize, owner);
        res.ifPresent(this::applyPageSize);
    }

    public void changePlacement(Placement p) {
        this.placement = p;
        if (onPlacementChanged != null) {
            onPlacementChanged.accept(p);
        }
    }
}

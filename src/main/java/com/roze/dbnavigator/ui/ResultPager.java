package com.roze.dbnavigator.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * DataGrip-style result pager: {@code |<  <  1-500 of 501+  >  >|  ⋮}.
 * Purely a display + button-click component — the owner (QueryTab/DataTab)
 * wires each button to its own fetch logic and calls {@link #update} to
 * refresh the range text.
 *
 * Honest simplification: the range label is informational only here, not an
 * interactive jump-to-page dropdown like the reference's chevron suggests —
 * building true jump-to-arbitrary-page needs random access into the result
 * set, which the forward-only cursor backing console pagination intentionally
 * doesn't support (see {@code PagedResultCursor}).
 */
public class ResultPager extends HBox {

    private final Button firstButton = new Button();
    private final Button prevButton = new Button();
    private final Button nextButton = new Button();
    private final Button lastButton = new Button();
    private final Label rangeLabel = new Label();
    private final MenuButton overflow = new MenuButton();

    public ResultPager() {
        getStyleClass().add("result-pager");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(2);

        firstButton.setGraphic(Icons.of(FontAwesomeSolid.ANGLE_DOUBLE_LEFT, "#a9b7c6", 11));
        firstButton.setTooltip(new Tooltip("First page"));
        prevButton.setGraphic(Icons.of(FontAwesomeSolid.ANGLE_LEFT, "#a9b7c6", 11));
        prevButton.setTooltip(new Tooltip("Previous page"));
        nextButton.setGraphic(Icons.of(FontAwesomeSolid.ANGLE_RIGHT, "#a9b7c6", 11));
        nextButton.setTooltip(new Tooltip("Next page"));
        lastButton.setGraphic(Icons.of(FontAwesomeSolid.ANGLE_DOUBLE_RIGHT, "#a9b7c6", 11));
        lastButton.setTooltip(new Tooltip("Last page (fetches all remaining rows)"));

        rangeLabel.getStyleClass().add("result-pager-label");

        overflow.setGraphic(Icons.of(FontAwesomeSolid.ELLIPSIS_V, "#a9b7c6", 11));
        overflow.getStyleClass().add("result-pager-overflow");

        for (Button b : java.util.List.of(firstButton, prevButton, nextButton, lastButton)) {
            b.getStyleClass().add("result-pager-button");
        }

        getChildren().addAll(firstButton, prevButton, rangeLabel, nextButton, lastButton, overflow);
    }

    public void setOnFirst(Runnable r) { firstButton.setOnAction(e -> r.run()); }
    public void setOnPrev(Runnable r) { prevButton.setOnAction(e -> r.run()); }
    public void setOnNext(Runnable r) { nextButton.setOnAction(e -> r.run()); }
    public void setOnLast(Runnable r) { lastButton.setOnAction(e -> r.run()); }

    public void addOverflowItem(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> action.run());
        overflow.getItems().add(item);
    }

    /**
     * @param from 1-based index of the first row on the current page (0 if empty)
     * @param to 1-based index of the last row on the current page
     * @param total known/estimated total; -1 if genuinely unknown
     * @param totalIsExact whether total is exact (shows "of N") or a floor (shows "of N+")
     */
    public void update(long from, long to, long total, boolean totalIsExact) {
        String range = from == 0 ? "0 rows" : from + "\u2013" + to;
        String totalText = total < 0 ? "" : "  of " + total + (totalIsExact ? "" : "+");
        rangeLabel.setText(range + totalText);

        firstButton.setDisable(from <= 1);
        prevButton.setDisable(from <= 1);
        boolean atOrPastEnd = totalIsExact && to >= total;
        nextButton.setDisable(atOrPastEnd);
        lastButton.setDisable(atOrPastEnd);
    }
}

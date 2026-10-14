package com.roze.dbnavigator.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Floating datetime modification popup for grid cells:
 * - Row 1 (Date): Day dropdown (1..31), Month dropdown (January..December), Year spinner
 * - Row 2 (Time): Hour spinner (0..23), Minute spinner (0..59), Second spinner (0..59)
 *
 * Dynamically binds with cell text in real time: changing any picker component updates
 * the editor text field, and typing in the editor text field parses and updates the picker controls.
 */
public class GridDateTimePicker {

    public static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private static final Pattern DT_PATTERN = Pattern.compile(
            "^(\\d{4})-(\\d{1,2})-(\\d{1,2})(?:[T\\s](\\d{1,2}):(\\d{1,2})(?::(\\d{1,2}))?([.\\d]*.*)?)?$");
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "^(\\d{1,2}):(\\d{1,2})(?::(\\d{1,2}))?([.\\d]*.*)?$");

    private final Popup popup = new Popup();
    private final Consumer<String> onValueChanged;
    private Runnable onPopupClosed;

    private final ComboBox<Integer> dayCombo = new ComboBox<>();
    private final ComboBox<String> monthCombo = new ComboBox<>(FXCollections.observableArrayList(MONTH_NAMES));
    private final Spinner<Integer> yearSpinner;
    private final Spinner<Integer> hourSpinner;
    private final Spinner<Integer> minuteSpinner;
    private final Spinner<Integer> secondSpinner;

    private final HBox dateRow;
    private final HBox timeRow;

    private boolean hasDate = true;
    private boolean hasTime = true;
    private boolean useIsoSeparator = false;
    private String fractionalSuffix = "";
    private boolean isUpdating = false;
    private final VBox container;
    private Consumer<Void> onCommitRequested;
    private Consumer<Void> onCancelRequested;

    public GridDateTimePicker(Consumer<String> onValueChanged) {
        this.onValueChanged = onValueChanged;

        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        yearSpinner = createSpinner(1900, 2100, LocalDate.now().getYear(), 76);
        hourSpinner = createSpinner(0, 23, 0, 56);
        minuteSpinner = createSpinner(0, 59, 0, 56);
        secondSpinner = createSpinner(0, 59, 0, 56);

        // Day combo styling and list
        dayCombo.setPrefWidth(60);
        dayCombo.setVisibleRowCount(10);
        dayCombo.getStyleClass().add("picker-combo");
        setupComboCellFactory(dayCombo);

        // Month combo styling
        monthCombo.setPrefWidth(115);
        monthCombo.setVisibleRowCount(12);
        monthCombo.getStyleClass().add("picker-combo");
        setupComboCellFactory(monthCombo);

        // While combos are showing their popup list, disable parent popup's autoHide so it doesn't dismiss
        dayCombo.setOnShowing(e -> popup.setAutoHide(false));
        dayCombo.setOnHiding(e -> popup.setAutoHide(true));
        monthCombo.setOnShowing(e -> popup.setAutoHide(false));
        monthCombo.setOnHiding(e -> popup.setAutoHide(true));

        // Connect listeners
        dayCombo.valueProperty().addListener((obs, oldV, newV) -> onComponentChanged());
        monthCombo.valueProperty().addListener((obs, oldV, newV) -> {
            adjustDaysInMonth();
            onComponentChanged();
        });
        yearSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            adjustDaysInMonth();
            onComponentChanged();
        });
        hourSpinner.valueProperty().addListener((obs, oldV, newV) -> onComponentChanged());
        minuteSpinner.valueProperty().addListener((obs, oldV, newV) -> onComponentChanged());
        secondSpinner.valueProperty().addListener((obs, oldV, newV) -> onComponentChanged());

        Label colon1 = new Label(":");
        colon1.getStyleClass().add("time-separator");
        Label colon2 = new Label(":");
        colon2.getStyleClass().add("time-separator");

        dateRow = new HBox(6, dayCombo, monthCombo, yearSpinner);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        timeRow = new HBox(4, hourSpinner, colon1, minuteSpinner, colon2, secondSpinner);
        timeRow.setAlignment(Pos.CENTER_LEFT);

        container = new VBox(6, dateRow, timeRow);
        container.getStyleClass().add("grid-datetime-picker");
        container.setAlignment(Pos.CENTER_LEFT);

        container.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                if (onCommitRequested != null) {
                    onCommitRequested.accept(null);
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                if (onCancelRequested != null) {
                    onCancelRequested.accept(null);
                } else {
                    hide();
                }
            }
        });

        popup.getContent().add(container);
        popup.setOnHidden(e -> {
            if (onPopupClosed != null) {
                onPopupClosed.run();
            }
        });
    }

    public void setOnCommitRequested(Consumer<Void> onCommitRequested) {
        this.onCommitRequested = onCommitRequested;
    }

    public void setOnCancelRequested(Consumer<Void> onCancelRequested) {
        this.onCancelRequested = onCancelRequested;
    }

    private void adjustDaysInMonth() {
        int year = yearSpinner.getValue() != null ? yearSpinner.getValue() : LocalDate.now().getYear();
        int monthIdx = monthCombo.getSelectionModel().getSelectedIndex();
        int month = (monthIdx >= 0 && monthIdx < 12) ? monthIdx + 1 : 1;
        int maxDays = YearMonth.of(year, month).lengthOfMonth();

        Integer curDay = dayCombo.getValue();
        if (dayCombo.getItems().size() != maxDays) {
            boolean prevUpdating = isUpdating;
            isUpdating = true;
            try {
                dayCombo.getItems().setAll(IntStream.rangeClosed(1, maxDays).boxed().toList());
                int selected = curDay != null ? Math.min(curDay, maxDays) : 1;
                dayCombo.setValue(selected);
            } finally {
                isUpdating = prevUpdating;
            }
        }
    }

    private <T> void setupComboCellFactory(ComboBox<T> combo) {
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                }
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                }
            }
        });
    }

    private Spinner<Integer> createSpinner(int min, int max, int initial, int width) {
        Spinner<Integer> spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial));
        spinner.setEditable(true);
        spinner.setPrefWidth(width);
        spinner.getStyleClass().add("picker-spinner");
        spinner.getEditor().getStyleClass().add("picker-spinner-editor");
        spinner.getEditor().setOnAction(e -> {
            commitSpinnerEditor(spinner);
            onComponentChanged();
            if (onCommitRequested != null) {
                onCommitRequested.accept(null);
            }
        });
        spinner.getEditor().setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                if (onCancelRequested != null) {
                    onCancelRequested.accept(null);
                } else {
                    hide();
                }
            }
        });
        spinner.getEditor().focusedProperty().addListener((obs, wasF, isF) -> {
            if (!isF) {
                commitSpinnerEditor(spinner);
                onComponentChanged();
            }
        });
        return spinner;
    }

    private void commitSpinnerEditor(Spinner<Integer> spinner) {
        try {
            String text = spinner.getEditor().getText();
            if (text != null && !text.isBlank()) {
                int val = Integer.parseInt(text.trim());
                SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                        (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
                int clamped = Math.max(factory.getMin(), Math.min(factory.getMax(), val));
                factory.setValue(clamped);
            }
        } catch (Exception ignored) {}
    }

    private void onComponentChanged() {
        if (isUpdating) return;
        isUpdating = true;
        try {
            String formatted = buildFormattedString();
            if (onValueChanged != null) {
                onValueChanged.accept(formatted);
            }
        } finally {
            isUpdating = false;
        }
    }

    public void parseAndUpdate(String text) {
        if (isUpdating) return;
        isUpdating = true;
        try {
            if (text == null || text.isBlank() || "NULL".equalsIgnoreCase(text)) {
                LocalDateTime now = LocalDateTime.now();
                setValues(now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                        now.getHour(), now.getMinute(), now.getSecond(), "", true, true, false);
                return;
            }

            Matcher m = DT_PATTERN.matcher(text.trim());
            if (m.matches()) {
                int y = Integer.parseInt(m.group(1));
                int mo = Math.max(1, Math.min(12, Integer.parseInt(m.group(2))));
                int d = Integer.parseInt(m.group(3));
                boolean hasTimePart = m.group(4) != null;
                int h = hasTimePart ? Integer.parseInt(m.group(4)) : 0;
                int mi = m.group(5) != null ? Integer.parseInt(m.group(5)) : 0;
                int s = m.group(6) != null ? Integer.parseInt(m.group(6)) : 0;
                String suffix = m.group(7) != null ? m.group(7) : "";
                boolean isIso = text.contains("T");
                setValues(y, mo, d, h, mi, s, suffix, true, hasTimePart, isIso);
                return;
            }

            Matcher tm = TIME_PATTERN.matcher(text.trim());
            if (tm.matches()) {
                int h = Integer.parseInt(tm.group(1));
                int mi = Integer.parseInt(tm.group(2));
                int s = tm.group(3) != null ? Integer.parseInt(tm.group(3)) : 0;
                String suffix = tm.group(4) != null ? tm.group(4) : "";
                LocalDate now = LocalDate.now();
                setValues(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), h, mi, s, suffix, false, true, false);
            }
        } finally {
            isUpdating = false;
        }
    }

    private void setValues(int y, int mo, int d, int h, int mi, int s, String suffix,
                           boolean showDate, boolean showTime, boolean isIso) {
        this.hasDate = showDate;
        this.hasTime = showTime;
        this.useIsoSeparator = isIso;
        this.fractionalSuffix = suffix != null ? suffix : "";

        dateRow.setVisible(hasDate);
        dateRow.setManaged(hasDate);
        timeRow.setVisible(hasTime);
        timeRow.setManaged(hasTime);

        yearSpinner.getValueFactory().setValue(y);
        monthCombo.setValue(MONTH_NAMES[mo - 1]);

        int maxDays = YearMonth.of(y, mo).lengthOfMonth();
        dayCombo.getItems().setAll(IntStream.rangeClosed(1, maxDays).boxed().toList());
        dayCombo.setValue(Math.max(1, Math.min(maxDays, d)));

        hourSpinner.getValueFactory().setValue(Math.max(0, Math.min(23, h)));
        minuteSpinner.getValueFactory().setValue(Math.max(0, Math.min(59, mi)));
        secondSpinner.getValueFactory().setValue(Math.max(0, Math.min(59, s)));
    }

    public String buildFormattedString() {
        int year = yearSpinner.getValue() != null ? yearSpinner.getValue() : 2025;
        int monthIdx = monthCombo.getSelectionModel().getSelectedIndex();
        int month = (monthIdx >= 0 && monthIdx < 12) ? monthIdx + 1 : 1;
        int day = dayCombo.getValue() != null ? dayCombo.getValue() : 1;
        int hour = hourSpinner.getValue() != null ? hourSpinner.getValue() : 0;
        int minute = minuteSpinner.getValue() != null ? minuteSpinner.getValue() : 0;
        int second = secondSpinner.getValue() != null ? secondSpinner.getValue() : 0;

        if (hasDate && hasTime) {
            char sep = useIsoSeparator ? 'T' : ' ';
            return String.format(Locale.ROOT, "%04d-%02d-%02d%c%02d:%02d:%02d%s",
                    year, month, day, sep, hour, minute, second, fractionalSuffix);
        } else if (hasDate) {
            return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day);
        } else if (hasTime) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d%s", hour, minute, second, fractionalSuffix);
        }
        return "";
    }

    public boolean isUpdating() {
        return isUpdating;
    }

    public boolean isFocused() {
        if (dayCombo.isFocused() || monthCombo.isFocused() || yearSpinner.isFocused()
                || yearSpinner.getEditor().isFocused()
                || hourSpinner.isFocused() || hourSpinner.getEditor().isFocused()
                || minuteSpinner.isFocused() || minuteSpinner.getEditor().isFocused()
                || secondSpinner.isFocused() || secondSpinner.getEditor().isFocused()) {
            return true;
        }
        return false;
    }

    public void setOnPopupClosed(Runnable r) {
        this.onPopupClosed = r;
    }

    public void show(Node anchor) {
        if (anchor == null || anchor.getScene() == null || anchor.getScene().getWindow() == null) return;
        if (!anchor.getScene().getStylesheets().isEmpty()) {
            container.getStylesheets().setAll(anchor.getScene().getStylesheets());
        }
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds == null) return;

        double x = bounds.getMinX();
        double y = bounds.getMaxY() + 2;

        try {
            Screen screen = Screen.getPrimary();
            for (Screen s : Screen.getScreensForRectangle(x, y, 280, 80)) {
                screen = s;
                break;
            }
            double screenBottom = screen.getVisualBounds().getMaxY();
            if (y + 80 > screenBottom) {
                y = Math.max(screen.getVisualBounds().getMinY(), bounds.getMinY() - 80 - 2);
            }
        } catch (Exception ignored) {}

        popup.show(anchor, x, y);
    }

    public void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public Popup getPopup() { return popup; }
    public ComboBox<Integer> getDayCombo() { return dayCombo; }
    public ComboBox<String> getMonthCombo() { return monthCombo; }
    public Spinner<Integer> getYearSpinner() { return yearSpinner; }
    public Spinner<Integer> getHourSpinner() { return hourSpinner; }
    public Spinner<Integer> getMinuteSpinner() { return minuteSpinner; }
    public Spinner<Integer> getSecondSpinner() { return secondSpinner; }
}

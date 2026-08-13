package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DataGrip-style visibility popup: "All databases"/"All schemas" master
 * checkbox plus one checkbox per item, with a "(Default)" marker on
 * whichever one the profile actually connects to/as.
 *
 * Result: empty list = show all; otherwise only the checked ones.
 */
public class DatabaseFilterDialog extends Dialog<List<String>> {

    private final CheckBox allBox;
    private final Map<String, CheckBox> databaseBoxes = new LinkedHashMap<>();

    public DatabaseFilterDialog(ConnectionProfile profile, List<String> allDatabases) {
        this(profile, allDatabases, "database", profile.getDatabase());
    }

    /**
     * @param noun        "database" or "schema" — drives every label in the dialog
     * @param defaultName the one item to mark "(Default)"; for Oracle this is the
     *                    connected user/schema (profile.getUsername()), not
     *                    profile.getDatabase() (which is Service Name/SID/TNS
     *                    depending on connection type, and not a schema name at all)
     */
    public DatabaseFilterDialog(ConnectionProfile profile, List<String> allDatabases,
                                String noun, String defaultName) {
        DialogTheme.apply(this);
        String nounPlural = noun + "s";
        setTitle("Show / Hide " + capitalize(nounPlural));
        setHeaderText("Choose which " + nounPlural + " appear under " + profile.getName());
        getDialogPane().setPrefWidth(420);

        List<String> filter = profile.getVisibleDatabases();
        boolean showAll = filter == null || filter.isEmpty();

        VBox list = new VBox(8);
        list.setPadding(new Insets(12));

        allBox = new CheckBox("All " + nounPlural);
        allBox.setSelected(showAll);
        allBox.selectedProperty().addListener((obs, was, all) -> {
            for (CheckBox box : databaseBoxes.values()) {
                box.setDisable(all);
                if (all) box.setSelected(true);
            }
        });
        list.getChildren().addAll(allBox, new Separator());

        for (String db : allDatabases) {
            boolean isDefault = db.equalsIgnoreCase(defaultName);
            CheckBox box = new CheckBox(db + (isDefault ? "   (Default)" : ""));
            box.setSelected(showAll || filter.contains(db));
            box.setDisable(showAll);
            databaseBoxes.put(db, box);
            list.getChildren().add(box);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(Math.min(70 + allDatabases.size() * 32, 420));
        scroll.getStyleClass().add("database-filter-scroll");
        getDialogPane().setContent(scroll);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            if (allBox.isSelected()) return new ArrayList<>();   // empty = all
            List<String> selected = new ArrayList<>();
            for (var entry : databaseBoxes.entrySet()) {
                if (entry.getValue().isSelected()) selected.add(entry.getKey());
            }
            // Selecting everything manually is the same as "all"
            return selected.size() == allDatabases.size() ? new ArrayList<>() : selected;
        });
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SpeedSearchAndMultiSelectTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createTestProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("pg-test");
        p.setName("thunder_db");
        p.setType(ConnectionProfile.DatabaseType.POSTGRESQL);
        p.setHost("localhost");
        p.setPort(5432);
        p.setDatabase("thunder_db");
        p.setUsername("postgres");
        return p;
    }

    @Test
    public void testSpeedSearchInitiallyHidden() {
        SchemaTreePane pane = new SchemaTreePane(null);
        assertFalse(pane.getSearchBox().isVisible(), "Search box should be hidden initially");
        assertFalse(pane.getSearchBox().isManaged(), "Search box should be unmanaged initially");
    }

    @Test
    public void testShowAndHideSpeedSearch() {
        SchemaTreePane pane = new SchemaTreePane(null);

        pane.showSpeedSearch();
        assertTrue(pane.getSearchBox().isVisible(), "Search box should be visible after showSpeedSearch");
        assertTrue(pane.getSearchBox().isManaged(), "Search box should be managed after showSpeedSearch");

        pane.hideSpeedSearch();
        assertFalse(pane.getSearchBox().isVisible(), "Search box should be hidden after hideSpeedSearch");
        assertFalse(pane.getSearchBox().isManaged(), "Search box should be unmanaged after hideSpeedSearch");
        assertEquals("", pane.getSpeedSearchText(), "Search query should be reset to empty");
    }

    @Test
    public void testSpeedSearchMatchAndNavigation() {
        SchemaTreePane pane = new SchemaTreePane(null);
        TreeItem<DbObject> root = pane.getTree().getRoot();

        TreeItem<DbObject> tablesFolder = new TreeItem<>(new DbObject("tables", Kind.TABLES_FOLDER));
        TreeItem<DbObject> seqFolder = new TreeItem<>(new DbObject("sequences", Kind.SEQUENCES_FOLDER));
        root.getChildren().addAll(tablesFolder, seqFolder);

        TreeItem<DbObject> t1 = new TreeItem<>(new DbObject("monitor_runs", Kind.TABLE, "thunder_db", "public"));
        TreeItem<DbObject> t2 = new TreeItem<>(new DbObject("monitors", Kind.TABLE, "thunder_db", "public"));
        TreeItem<DbObject> t3 = new TreeItem<>(new DbObject("users", Kind.TABLE, "thunder_db", "public"));
        tablesFolder.getChildren().addAll(t1, t2, t3);

        TreeItem<DbObject> s1 = new TreeItem<>(new DbObject("monitor_runs_id_seq", Kind.SEQUENCE, "thunder_db", "public"));
        TreeItem<DbObject> s2 = new TreeItem<>(new DbObject("monitors_id_seq", Kind.SEQUENCE, "thunder_db", "public"));
        seqFolder.getChildren().addAll(s1, s2);

        // Search for "mon"
        pane.setSpeedSearchText("mon");

        List<TreeItem<DbObject>> matches = pane.getSearchMatches();
        assertEquals(4, matches.size(), "Should match monitor_runs, monitors, monitor_runs_id_seq, monitors_id_seq");
        assertEquals(0, pane.getCurrentMatchIndex(), "First match should be active");
        assertEquals(t1, pane.getTree().getSelectionModel().getSelectedItem(), "First match should be selected in tree");
        assertEquals("1 of 4", pane.getMatchCountLabel().getText());

        // Navigate next
        pane.selectNextMatch();
        assertEquals(1, pane.getCurrentMatchIndex());
        assertEquals(t2, pane.getTree().getSelectionModel().getSelectedItem());
        assertEquals("2 of 4", pane.getMatchCountLabel().getText());

        pane.selectNextMatch();
        assertEquals(2, pane.getCurrentMatchIndex());
        assertEquals(s1, pane.getTree().getSelectionModel().getSelectedItem());

        pane.selectNextMatch();
        assertEquals(3, pane.getCurrentMatchIndex());
        assertEquals(s2, pane.getTree().getSelectionModel().getSelectedItem());

        // Wrap around to first match
        pane.selectNextMatch();
        assertEquals(0, pane.getCurrentMatchIndex());
        assertEquals(t1, pane.getTree().getSelectionModel().getSelectedItem());

        // Navigate previous (wraps to last)
        pane.selectPreviousMatch();
        assertEquals(3, pane.getCurrentMatchIndex());
        assertEquals(s2, pane.getTree().getSelectionModel().getSelectedItem());
    }

    @Test
    public void testBuildHighlightedName() {
        // 1. Match at beginning
        Node node1 = SchemaTreePane.buildHighlightedName("monitor_runs", "mon");
        assertTrue(node1 instanceof HBox, "Matched name should be an HBox of labels");
        HBox box1 = (HBox) node1;
        assertEquals(2, box1.getChildren().size());

        Label part1 = (Label) box1.getChildren().get(0);
        assertEquals("mon", part1.getText());
        assertTrue(part1.getStyleClass().contains("tree-search-match"), "Matching chunk must have tree-search-match class");

        Label part2 = (Label) box1.getChildren().get(1);
        assertEquals("itor_runs", part2.getText());
        assertFalse(part2.getStyleClass().contains("tree-search-match"));

        // 2. Match in middle
        Node node2 = SchemaTreePane.buildHighlightedName("monitor_runs_id_seq", "runs");
        assertTrue(node2 instanceof HBox);
        HBox box2 = (HBox) node2;
        assertEquals(3, box2.getChildren().size());
        assertEquals("monitor_", ((Label) box2.getChildren().get(0)).getText());
        assertEquals("runs", ((Label) box2.getChildren().get(1)).getText());
        assertTrue(box2.getChildren().get(1).getStyleClass().contains("tree-search-match"));
        assertEquals("_id_seq", ((Label) box2.getChildren().get(2)).getText());

        // 3. No match returns single plain label
        Node node3 = SchemaTreePane.buildHighlightedName("users", "mon");
        assertTrue(node3 instanceof Label, "Non-matching name should be a single Label");
        assertEquals("users", ((Label) node3).getText());
        assertFalse(node3.getStyleClass().contains("tree-search-match"));
    }

    @Test
    public void testMultiSelectionMode() {
        SchemaTreePane pane = new SchemaTreePane(null);
        assertEquals(SelectionMode.MULTIPLE, pane.getTree().getSelectionModel().getSelectionMode(),
                "Tree selection mode must be MULTIPLE to allow Ctrl/Cmd multi-selection");

        TreeItem<DbObject> root = pane.getTree().getRoot();
        TreeItem<DbObject> t1 = new TreeItem<>(new DbObject("monitors", Kind.TABLE, "thunder_db", "public"));
        TreeItem<DbObject> s1 = new TreeItem<>(new DbObject("monitors_id_seq", Kind.SEQUENCE, "thunder_db", "public"));
        root.getChildren().addAll(t1, s1);

        pane.getTree().getSelectionModel().select(t1);
        pane.getTree().getSelectionModel().select(s1);

        assertEquals(2, pane.getTree().getSelectionModel().getSelectedItems().size(),
                "Both table and sequence should be selected simultaneously");
        assertTrue(pane.getTree().getSelectionModel().getSelectedItems().contains(t1));
        assertTrue(pane.getTree().getSelectionModel().getSelectedItems().contains(s1));
    }

    @Test
    public void testConfirmDropDialogDimensionsAndPreview() throws Exception {
        ConnectionProfile profile = createTestProfile();
        DbObject table = new DbObject("monitors", Kind.TABLE, "thunder_db", "public");
        DbObject seq = new DbObject("monitors_id_seq", Kind.SEQUENCE, "thunder_db", "public");

        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                ConfirmDropDialog dialog = new ConfirmDropDialog(null, profile, List.of(table, seq), null);
                assertFalse(dialog.isResizable(), "Confirm Drop dialog should not be resizable to prevent screen overflow");
                assertEquals(500, dialog.getDialogPane().getPrefWidth(), 1.0, "Dialog pref width should be constrained to 500px");
                assertEquals(500, dialog.getDialogPane().getMaxWidth(), 1.0, "Dialog max width should be capped to 500px");
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        future.get(10, java.util.concurrent.TimeUnit.SECONDS);
    }
}

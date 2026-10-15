package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.QueryHistoryStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import static org.junit.jupiter.api.Assertions.*;

public class MongoConsoleHistoryTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createMongoProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("mongo-history-test");
        p.setName("mongodb@local");
        p.setType(DatabaseType.MONGODB);
        p.setHost("localhost");
        p.setPort(27017);
        p.setDatabase("studentdb");
        return p;
    }

    @Test
    public void testMongoConsoleHistoryButtonBesideRunButton() {
        ConnectionProfile profile = createMongoProfile();
        MongoConsoleTab tab = new MongoConsoleTab(profile, "studentdb", "console 1");

        VBox root = (VBox) tab.getContent();
        assertNotNull(root);
        assertTrue(root.getChildren().get(0) instanceof HBox);
        HBox toolbar = (HBox) root.getChildren().get(0);

        // Child 0 is runButton, Child 1 is historyButton
        assertTrue(toolbar.getChildren().size() >= 2);
        assertTrue(toolbar.getChildren().get(0) instanceof Button);
        Button runBtn = (Button) toolbar.getChildren().get(0);
        assertEquals("Run", runBtn.getText());

        assertTrue(toolbar.getChildren().get(1) instanceof Button);
        Button histBtn = (Button) toolbar.getChildren().get(1);
        assertSame(tab.getHistoryButton(), histBtn);

        assertNotNull(histBtn.getTooltip());
        assertTrue(histBtn.getTooltip().getText().contains("Browse Query History"));

        assertNotNull(histBtn.getGraphic());
        assertTrue(histBtn.getGraphic() instanceof FontIcon);
        FontIcon icon = (FontIcon) histBtn.getGraphic();
        assertEquals(FontAwesomeSolid.HISTORY.getDescription(), icon.getIconCode().getDescription());
    }

    @Test
    public void testMongoConsoleHistoryStoreRecordingAndShowHistory() {
        ConnectionProfile profile = createMongoProfile();
        MongoConsoleTab tab = new MongoConsoleTab(profile, "studentdb", "console 1");

        QueryHistoryStore.clear(profile.getId());
        assertEquals(0, QueryHistoryStore.forConnection(profile.getId()).size());

        // Set statement in editor and run
        tab.setScriptText("db.student.find();");
        tab.runCurrentStatement();

        // Statement should be recorded in QueryHistoryStore
        var entries = QueryHistoryStore.forConnection(profile.getId());
        assertFalse(entries.isEmpty());
        assertEquals("db.student.find()", entries.get(0).sql());

        // Calling showHistory should not throw
        assertDoesNotThrow(tab::showHistory);
    }
}

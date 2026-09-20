package com.roze.dbnavigator.util;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.ExecuteActionConfig;
import javafx.scene.control.IndexRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QueryExecutionResolverTest {

    @Test
    public void testSelectionExactlyAsSingleStatement() {
        String sql = "SELECT 1;\nSELECT 2;";
        IndexRange sel = new IndexRange(0, sql.length());
        ExecuteActionConfig config = new ExecuteActionConfig(
                "Execute", "Ctrl+Enter", "Ask what to execute", "Nothing",
                "Exactly as a single statement", false);

        QueryExecutionResolver.ResolvedExecution resolved = QueryExecutionResolver.resolve(
                sql, sel, 0, config, SqlStatementSplitter.MODE_ANSI_OR_SEPARATOR);

        assertEquals(QueryExecutionResolver.ResolvedExecution.ActionType.EXECUTE, resolved.getActionType());
        assertEquals(1, resolved.getStatements().size());
        assertEquals("SELECT 1;\nSELECT 2", resolved.getStatements().get(0));
    }

    @Test
    public void testSelectionExactlyAsSeparateStatements() {
        String sql = "SELECT 1;\nSELECT 2;";
        IndexRange sel = new IndexRange(0, sql.length());
        ExecuteActionConfig config = new ExecuteActionConfig(
                "Execute", "Ctrl+Enter", "Ask what to execute", "Nothing",
                "Exactly as separate statements", false);

        QueryExecutionResolver.ResolvedExecution resolved = QueryExecutionResolver.resolve(
                sql, sel, 0, config, SqlStatementSplitter.MODE_ANSI_OR_SEPARATOR);

        assertEquals(QueryExecutionResolver.ResolvedExecution.ActionType.EXECUTE, resolved.getActionType());
        assertEquals(2, resolved.getStatements().size());
        assertEquals("SELECT 1", resolved.getStatements().get(0));
        assertEquals("SELECT 2", resolved.getStatements().get(1));
    }

    @Test
    public void testCaretInsideSmallestStatement() {
        String sql = "SELECT * FROM users;\nSELECT * FROM orders;";
        // Caret is inside "SELECT * FROM orders;"
        int caretPos = sql.indexOf("orders");
        ExecuteActionConfig config = new ExecuteActionConfig(
                "Execute", "Ctrl+Enter", "Smallest statement", "Nothing",
                "Exactly as separate statements", false);

        QueryExecutionResolver.ResolvedExecution resolved = QueryExecutionResolver.resolve(
                sql, null, caretPos, config, SqlStatementSplitter.MODE_ANSI_OR_SEPARATOR);

        assertEquals(QueryExecutionResolver.ResolvedExecution.ActionType.EXECUTE, resolved.getActionType());
        assertEquals(1, resolved.getStatements().size());
        assertEquals("SELECT * FROM orders", resolved.getStatements().get(0));
    }

    @Test
    public void testCaretInsideAskWhatToExecute() {
        String sql = "SELECT * FROM (SELECT id FROM users) u;\nSELECT 2;";
        // Caret is inside the subquery "SELECT id FROM users"
        int caretPos = sql.indexOf("users");
        ExecuteActionConfig config = new ExecuteActionConfig(
                "Execute", "Ctrl+Enter", "Ask what to execute", "Nothing",
                "Exactly as separate statements", false);

        QueryExecutionResolver.ResolvedExecution resolved = QueryExecutionResolver.resolve(
                sql, null, caretPos, config, SqlStatementSplitter.MODE_ANSI_OR_SEPARATOR);

        assertEquals(QueryExecutionResolver.ResolvedExecution.ActionType.ASK_CHOOSER, resolved.getActionType());
        assertFalse(resolved.getCandidates().isEmpty());

        // Should include both statement, subquery, and whole script options
        assertTrue(resolved.getCandidates().stream().anyMatch(c -> c.title().startsWith("Statement:")));
        assertTrue(resolved.getCandidates().stream().anyMatch(c -> c.title().startsWith("Subquery:")
                && c.statements().contains("SELECT id FROM users")));
        assertTrue(resolved.getCandidates().stream().anyMatch(c -> c.title().startsWith("Whole script")));
    }

    @Test
    public void testCaretInsideSmallestSubqueryOrStatement() {
        String sql = "SELECT * FROM (SELECT id, name FROM users) u;";
        int caretPos = sql.indexOf("name");
        ExecuteActionConfig config = new ExecuteActionConfig(
                "Execute", "Ctrl+Enter", "Smallest subquery or statement", "Nothing",
                "Exactly as separate statements", false);

        QueryExecutionResolver.ResolvedExecution resolved = QueryExecutionResolver.resolve(
                sql, null, caretPos, config, SqlStatementSplitter.MODE_ANSI_OR_SEPARATOR);

        assertEquals(QueryExecutionResolver.ResolvedExecution.ActionType.EXECUTE, resolved.getActionType());
        assertEquals(1, resolved.getStatements().size());
        assertEquals("SELECT id, name FROM users", resolved.getStatements().get(0));
    }

    @Test
    public void testCaretInsideWholeScript() {
        String sql = "SELECT 1;\nSELECT 2;\nSELECT 3;";
        int caretPos = 3;
        ExecuteActionConfig config = new ExecuteActionConfig(
                "Execute", "Ctrl+Enter", "Whole script", "Nothing",
                "Exactly as separate statements", false);

        QueryExecutionResolver.ResolvedExecution resolved = QueryExecutionResolver.resolve(
                sql, null, caretPos, config, SqlStatementSplitter.MODE_ANSI_OR_SEPARATOR);

        assertEquals(QueryExecutionResolver.ResolvedExecution.ActionType.EXECUTE, resolved.getActionType());
        assertEquals(3, resolved.getStatements().size());
        assertEquals("SELECT 1", resolved.getStatements().get(0));
        assertEquals("SELECT 2", resolved.getStatements().get(1));
        assertEquals("SELECT 3", resolved.getStatements().get(2));
    }

    @Test
    public void testCaretOutsideNothing() {
        String sql = "   \n\n   ";
        int caretPos = 2;
        ExecuteActionConfig config = new ExecuteActionConfig(
                "Execute", "Ctrl+Enter", "Ask what to execute", "Nothing",
                "Exactly as separate statements", false);

        QueryExecutionResolver.ResolvedExecution resolved = QueryExecutionResolver.resolve(
                sql, null, caretPos, config, SqlStatementSplitter.MODE_ANSI_OR_SEPARATOR);

        assertEquals(QueryExecutionResolver.ResolvedExecution.ActionType.NO_OP, resolved.getActionType());
    }

    @Test
    public void testCaretOutsideWholeScript() {
        String sql = "SELECT 1;\n\n   ";
        // Caret is at the trailing whitespace
        int caretPos = sql.length() - 1;
        ExecuteActionConfig config = new ExecuteActionConfig(
                "Execute", "Ctrl+Enter", "Ask what to execute", "Whole script",
                "Exactly as separate statements", false);

        QueryExecutionResolver.ResolvedExecution resolved = QueryExecutionResolver.resolve(
                sql, null, caretPos, config, SqlStatementSplitter.MODE_ANSI_OR_SEPARATOR);

        assertEquals(QueryExecutionResolver.ResolvedExecution.ActionType.EXECUTE, resolved.getActionType());
        assertEquals(1, resolved.getStatements().size());
        assertEquals("SELECT 1", resolved.getStatements().get(0));
    }

    @Test
    public void testUnsafeQueryDetector() {
        // Safe queries
        assertNull(UnsafeQueryDetector.check("SELECT * FROM users;"));
        assertNull(UnsafeQueryDetector.check("DELETE FROM users WHERE id = 5;"));
        assertNull(UnsafeQueryDetector.check("UPDATE users SET active = false WHERE status = 'expired';"));
        assertNull(UnsafeQueryDetector.check("-- DROP TABLE test;\nSELECT 1;"));

        // Unsafe queries
        assertNotNull(UnsafeQueryDetector.check("DELETE FROM users;"));
        assertNotNull(UnsafeQueryDetector.check("UPDATE users SET active = 0;"));
        assertNotNull(UnsafeQueryDetector.check("DROP TABLE users;"));
        assertNotNull(UnsafeQueryDetector.check("DROP DATABASE staging;"));
        assertNotNull(UnsafeQueryDetector.check("TRUNCATE TABLE audit_log;"));
    }

    @Test
    public void testDefaultSettingsActionConfigs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        List<ExecuteActionConfig> actions = settings.getExecuteActions();
        assertNotNull(actions);
        assertEquals(3, actions.size(), "Should have 3 default execute action profiles");

        ExecuteActionConfig a0 = actions.get(0);
        assertEquals("Execute", a0.getName());
        assertEquals("Ctrl+Enter", a0.getShortcut());
        assertEquals("Ask what to execute", a0.getWhenCaretInside());
        assertEquals("Nothing", a0.getWhenCaretOutside());
        assertEquals("Exactly as separate statements", a0.getForSelection());
        assertFalse(a0.isOpenResultsInNewTab());

        ExecuteActionConfig a1 = actions.get(1);
        assertEquals("Execute (2)", a1.getName());
        assertEquals("Ctrl+Shift+Enter", a1.getShortcut());
        assertEquals("Smallest statement", a1.getWhenCaretInside());

        ExecuteActionConfig a2 = actions.get(2);
        assertEquals("Execute (3)", a2.getName());
        assertEquals("Ctrl+Alt+Enter", a2.getShortcut());
        assertEquals("Whole script", a2.getWhenCaretInside());

        assertTrue(settings.isReviewParametersBeforeExecution());
        assertTrue(settings.isWarnUnsafeQueries());
        assertEquals("Into valid ANSI SQL statements or by separator", settings.getScriptSplitting());
    }

    @Test
    public void testSqlStatementSplitterModes() {
        String sql = "SELECT 1;\nSELECT 2;";

        // MODE_SEPARATOR_ONLY
        List<SqlStatementSplitter.Statement> sepOnly = SqlStatementSplitter.split(sql, SqlStatementSplitter.MODE_SEPARATOR_ONLY);
        assertEquals(2, sepOnly.size());

        // MODE_ANSI_OR_SEPARATOR
        List<SqlStatementSplitter.Statement> ansiOrSep = SqlStatementSplitter.split(sql, SqlStatementSplitter.MODE_ANSI_OR_SEPARATOR);
        assertEquals(2, ansiOrSep.size());

        // Statements without trailing semicolons separated by blank lines / ANSI keyword
        String unseparated = "SELECT 1\n\nSELECT 2";
        List<SqlStatementSplitter.Statement> ansiOnly = SqlStatementSplitter.split(unseparated, SqlStatementSplitter.MODE_ANSI_ONLY);
        assertEquals(2, ansiOnly.size());
    }

    @Test
    public void testSettingsJsonRoundtrip() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        settings.setScriptSplitting(SqlStatementSplitter.MODE_ANSI_ONLY);
        settings.setReviewParametersBeforeExecution(false);
        settings.setWarnUnsafeQueries(false);

        List<ExecuteActionConfig> list = settings.getExecuteActions();
        list.get(0).setOpenResultsInNewTab(true);

        String json = mapper.writeValueAsString(settings);
        AppSettingsStore.Settings restored = mapper.readValue(json, AppSettingsStore.Settings.class);

        assertEquals(SqlStatementSplitter.MODE_ANSI_ONLY, restored.getScriptSplitting());
        assertFalse(restored.isReviewParametersBeforeExecution());
        assertFalse(restored.isWarnUnsafeQueries());
        assertEquals(3, restored.getExecuteActions().size());
        assertTrue(restored.getExecuteActions().get(0).isOpenResultsInNewTab());
    }
}

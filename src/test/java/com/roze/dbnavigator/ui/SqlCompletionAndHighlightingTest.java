package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SqlCompletionAndHighlightingTest {

    private ConnectionProfile createProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("test-pg");
        p.setName("postgres@localhost");
        p.setType(ConnectionProfile.DatabaseType.POSTGRESQL);
        return p;
    }

    @Test
    public void testLiveTemplatesAtStatementStart() {
        ConnectionProfile profile = createProfile();
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "public", "", "sel", CompletionService.Context.STATEMENT_START);

        assertFalse(suggestions.isEmpty(), "Should suggest completions for 'sel'");

        // Verify 'sel' template is suggested
        CompletionService.Suggestion selTmpl = suggestions.stream()
                .filter(s -> s.text().equalsIgnoreCase("sel") && s.kind() == CompletionService.Kind.TEMPLATE)
                .findFirst()
                .orElse(null);

        assertNotNull(selTmpl, "Template 'sel' must be present");
        assertEquals("select all rows from a table", selTmpl.detail());
        assertNotNull(selTmpl.templateExpansion());
        assertTrue(selTmpl.templateExpansion().contains("from"));
        assertTrue(selTmpl.templateExpansion().contains("$END$"));

        // Verify 'select' keyword is also suggested
        boolean hasSelectKw = suggestions.stream()
                .anyMatch(s -> s.text().equalsIgnoreCase("select") && s.kind() == CompletionService.Kind.KEYWORD);
        assertTrue(hasSelectKw, "'select' keyword must be suggested");

        // Verify 'selc' and 'selw' templates are also present
        boolean hasSelc = suggestions.stream().anyMatch(s -> s.text().equalsIgnoreCase("selc"));
        boolean hasSelw = suggestions.stream().anyMatch(s -> s.text().equalsIgnoreCase("selw"));
        assertTrue(hasSelc, "'selc' template must be suggested");
        assertTrue(hasSelw, "'selw' template must be suggested");
    }

    @Test
    public void testFromSuggestedAfterSelectProjection() {
        ConnectionProfile profile = createProfile();
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "public", "select * ", "", CompletionService.Context.AFTER_SELECT_PROJECTION);

        assertFalse(suggestions.isEmpty(), "Should suggest keywords after select projection");

        CompletionService.Suggestion first = suggestions.get(0);
        assertEquals("from", first.text().toLowerCase(), "'from' should be top suggestion after select *");
        assertEquals(CompletionService.Kind.KEYWORD, first.kind());
    }

    @Test
    public void testCascadeSuggestedAfterDropTable() {
        ConnectionProfile profile = createProfile();
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "public", "drop table environments cas", "cas", CompletionService.Context.POST_OBJECT_DDL);

        assertFalse(suggestions.isEmpty(), "Should suggest cascade after drop table");

        CompletionService.Suggestion cascade = suggestions.stream()
                .filter(s -> s.text().equalsIgnoreCase("cascade"))
                .findFirst()
                .orElse(null);

        assertNotNull(cascade, "'cascade' must be suggested in POST_OBJECT_DDL");
        assertEquals(CompletionService.Kind.KEYWORD, cascade.kind());
    }

    @Test
    public void testCascadeHighlightingInSqlHighlighter() {
        String sql = "drop table environments cascade;\ndrop sequence environments_id_seq cascade;";
        StyleSpans<Collection<String>> spans = SqlHighlighter.computeHighlighting(sql);
        assertNotNull(spans);

        // Check that cascade appears as sql-keyword
        boolean foundCascadeKeyword = false;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\bcascade\\b").matcher(sql);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            int currentOffset = 0;
            for (org.fxmisc.richtext.model.StyleSpan<Collection<String>> span : spans) {
                int nextOffset = currentOffset + span.getLength();
                if (currentOffset <= start && nextOffset >= end) {
                    if (span.getStyle().contains("sql-keyword")) {
                        foundCascadeKeyword = true;
                        break;
                    }
                }
                currentOffset = nextOffset;
            }
        }
        assertTrue(foundCascadeKeyword, "'cascade' must be highlighted as 'sql-keyword'");
    }

    @Test
    public void testForceSuggestedInDropDatabaseOption() {
        ConnectionProfile profile = createProfile();
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "public", "drop database empdb (for", "for", CompletionService.Context.DROP_DATABASE_OPTION);

        assertFalse(suggestions.isEmpty(), "Should suggest completions for 'for' in DROP_DATABASE_OPTION");

        CompletionService.Suggestion forceSug = suggestions.stream()
                .filter(s -> s.text().equalsIgnoreCase("force"))
                .findFirst()
                .orElse(null);

        assertNotNull(forceSug, "'FORCE' must be suggested in DROP_DATABASE_OPTION");
        assertEquals(CompletionService.Kind.KEYWORD, forceSug.kind());
    }

    @Test
    public void testForceHighlightingInSqlHighlighter() {
        String sql = "drop database empdb (force);";
        StyleSpans<Collection<String>> spans = SqlHighlighter.computeHighlighting(sql);
        assertNotNull(spans);

        boolean foundForceKeyword = false;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\bforce\\b").matcher(sql);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            int currentOffset = 0;
            for (org.fxmisc.richtext.model.StyleSpan<Collection<String>> span : spans) {
                int nextOffset = currentOffset + span.getLength();
                if (currentOffset <= start && nextOffset >= end) {
                    if (span.getStyle().contains("sql-keyword")) {
                        foundForceKeyword = true;
                        break;
                    }
                }
                currentOffset = nextOffset;
            }
        }
        assertTrue(foundForceKeyword, "'force' must be highlighted as 'sql-keyword'");
    }

    @Test
    public void testDropDataSuggestsDatabase() {
        ConnectionProfile profile = createProfile();
        // 1. Verify context detection at tokenStart for "drop data" (pos 5, after "drop ")
        CompletionService.Context ctx = QueryTab.contextAt("drop data", 5);
        assertEquals(CompletionService.Context.DDL_OBJECT_TYPE, ctx, "Context after 'drop ' must be DDL_OBJECT_TYPE");

        // 2. Verify suggestion for prefix 'data'
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "public", "drop data", "data", ctx);

        assertFalse(suggestions.isEmpty(), "Should suggest completions for 'data' after 'drop '");

        CompletionService.Suggestion dbSug = suggestions.stream()
                .filter(s -> s.text().equalsIgnoreCase("database"))
                .findFirst()
                .orElse(null);

        assertNotNull(dbSug, "'database' keyword must be suggested when typing 'drop data'");
        assertEquals(CompletionService.Kind.KEYWORD, dbSug.kind());
    }

    @Test
    public void testDropSuggestsDdlObjectTypes() {
        ConnectionProfile profile = createProfile();
        CompletionService.Context ctx = QueryTab.contextAt("drop ", 5);
        assertEquals(CompletionService.Context.DDL_OBJECT_TYPE, ctx);

        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "public", "drop ", "", ctx);

        List<String> names = suggestions.stream().map(s -> s.text().toLowerCase()).toList();
        assertTrue(names.contains("database"), "Must contain 'database'");
        assertTrue(names.contains("schema"), "Must contain 'schema'");
        assertTrue(names.contains("table"), "Must contain 'table'");
        assertTrue(names.contains("view"), "Must contain 'view'");
    }

    @Test
    public void testDropDatabaseAndSchemaContexts() {
        assertEquals(CompletionService.Context.DATABASES, QueryTab.contextAt("drop database ", 14));
        assertEquals(CompletionService.Context.DATABASES, QueryTab.contextAt("drop database if exists ", 24));
        assertEquals(CompletionService.Context.DATABASES, QueryTab.contextAt("alter database ", 15));
        assertEquals(CompletionService.Context.DATABASES, QueryTab.contextAt("use ", 4));

        assertEquals(CompletionService.Context.SCHEMAS, QueryTab.contextAt("drop schema ", 12));
        assertEquals(CompletionService.Context.SCHEMAS, QueryTab.contextAt("alter schema ", 13));
    }

    @Test
    public void testDatabaseLiveTemplates() {
        ConnectionProfile profile = createProfile();
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "public", "", "drop", CompletionService.Context.STATEMENT_START);

        boolean hasDropd = suggestions.stream().anyMatch(s -> s.text().equalsIgnoreCase("dropd"));
        boolean hasDropt = suggestions.stream().anyMatch(s -> s.text().equalsIgnoreCase("dropt"));
        assertTrue(hasDropd, "'dropd' template must be suggested");
        assertTrue(hasDropt, "'dropt' template must be suggested");
    }

    @Test
    public void testPostgresTypeCastContextAndDateSuggestions() {
        ConnectionProfile profile = createProfile();
        String sql = "select b.id as invoiceId, b.uhid from bbill b where b.bill_date_time::d";

        // 1. Verify context detection for ::d at end of statement and at tokenStart
        CompletionService.Context ctxEnd = QueryTab.contextAt(sql, sql.length());
        assertEquals(CompletionService.Context.TYPE_CAST, ctxEnd, "Context at '::d' must be TYPE_CAST");

        CompletionService.Context ctxStart = QueryTab.contextAt(sql, sql.length() - 1);
        assertEquals(CompletionService.Context.TYPE_CAST, ctxStart, "Context right after '::' must be TYPE_CAST");

        // 2. Verify suggestions for prefix 'd'
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, "erpdb", sql, "d", ctxEnd);

        assertFalse(suggestions.isEmpty(), "Should suggest completions for 'd' after '::'");

        // Verify top suggestion is date with pg_catalog qualification
        CompletionService.Suggestion first = suggestions.get(0);
        assertEquals("date", first.text().toLowerCase(), "'date' must be the top suggestion");
        assertEquals(CompletionService.Kind.TYPE, first.kind(), "'date' must have Kind.TYPE");
        assertTrue(first.detail().contains("pg_catalog"), "Detail should indicate pg_catalog");

        // Verify dec, decimal, double precision are present
        List<String> names = suggestions.stream().map(s -> s.text().toLowerCase()).toList();
        assertTrue(names.contains("dec"), "Must contain 'dec'");
        assertTrue(names.contains("decimal"), "Must contain 'decimal'");
        assertTrue(names.contains("double precision"), "Must contain 'double precision'");
    }

    @Test
    public void testStandardCastAndConvertAcrossDialects() {
        // 1. Standard CAST context
        String castSql = "select cast(b.amount as d";
        assertEquals(CompletionService.Context.TYPE_CAST, QueryTab.contextAt(castSql, castSql.length()));

        // MySQL
        ConnectionProfile mysql = new ConnectionProfile();
        mysql.setId("test-mysql");
        mysql.setType(ConnectionProfile.DatabaseType.MYSQL);
        List<CompletionService.Suggestion> mySqlSug =
                CompletionService.suggest(mysql, null, castSql, "d", CompletionService.Context.TYPE_CAST);
        List<String> mySqlNames = mySqlSug.stream().map(s -> s.text().toLowerCase()).toList();
        assertTrue(mySqlNames.contains("date"), "MySQL must suggest 'DATE'");
        assertTrue(mySqlNames.contains("datetime"), "MySQL must suggest 'DATETIME'");
        assertTrue(mySqlNames.contains("decimal"), "MySQL must suggest 'DECIMAL'");

        // Oracle
        ConnectionProfile oracle = new ConnectionProfile();
        oracle.setId("test-oracle");
        oracle.setType(ConnectionProfile.DatabaseType.ORACLE);
        List<CompletionService.Suggestion> oraSug =
                CompletionService.suggest(oracle, null, castSql, "d", CompletionService.Context.TYPE_CAST);
        List<String> oraNames = oraSug.stream().map(s -> s.text().toLowerCase()).toList();
        assertTrue(oraNames.contains("date"), "Oracle must suggest 'DATE'");
        assertTrue(oraNames.contains("decimal"), "Oracle must suggest 'DECIMAL'");

        // SQL Server CONVERT context
        String convertSql = "select convert(d";
        assertEquals(CompletionService.Context.TYPE_CAST, QueryTab.contextAt(convertSql, convertSql.length()));
        ConnectionProfile sqlServer = new ConnectionProfile();
        sqlServer.setId("test-mssql");
        sqlServer.setType(ConnectionProfile.DatabaseType.SQLSERVER);
        List<CompletionService.Suggestion> msSug =
                CompletionService.suggest(sqlServer, null, convertSql, "d", CompletionService.Context.TYPE_CAST);
        List<String> msNames = msSug.stream().map(s -> s.text().toLowerCase()).toList();
        assertTrue(msNames.contains("date"), "SQL Server must suggest 'DATE'");
        assertTrue(msNames.contains("datetime"), "SQL Server must suggest 'DATETIME'");
        assertTrue(msNames.contains("datetime2"), "SQL Server must suggest 'DATETIME2'");

        // SQLite
        ConnectionProfile sqlite = new ConnectionProfile();
        sqlite.setId("test-sqlite");
        sqlite.setType(ConnectionProfile.DatabaseType.SQLITE);
        List<CompletionService.Suggestion> sqliteSug =
                CompletionService.suggest(sqlite, null, castSql, "d", CompletionService.Context.TYPE_CAST);
        List<String> sqliteNames = sqliteSug.stream().map(s -> s.text().toLowerCase()).toList();
        assertTrue(sqliteNames.contains("date"), "SQLite must suggest 'DATE'");
        assertTrue(sqliteNames.contains("datetime"), "SQLite must suggest 'DATETIME'");
    }

    @Test
    public void testTableColumnDefinitionTypeContext() {
        String createSql = "create table employees (id int, created_at d";
        assertEquals(CompletionService.Context.TYPE_CAST, QueryTab.contextAt(createSql, createSql.length()));

        String alterSql = "alter table employees add column created_at d";
        assertEquals(CompletionService.Context.TYPE_CAST, QueryTab.contextAt(alterSql, alterSql.length()));

        String alterNoColSql = "alter table employees add created_at d";
        assertEquals(CompletionService.Context.TYPE_CAST, QueryTab.contextAt(alterNoColSql, alterNoColSql.length()));
    }

    @Test
    public void testDateKeywordsInGeneralExpressions() {
        // PostgreSQL date functions & constants
        ConnectionProfile pg = createProfile();
        List<CompletionService.Suggestion> pgSug =
                CompletionService.suggest(pg, "public", "where b.col >= curr", "curr", CompletionService.Context.COLUMNS);
        List<String> pgNames = pgSug.stream().map(s -> s.text().toLowerCase()).toList();
        assertTrue(pgNames.contains("current_date"), "Postgres should suggest CURRENT_DATE");

        // MySQL date functions
        ConnectionProfile mysql = new ConnectionProfile();
        mysql.setId("test-mysql");
        mysql.setType(ConnectionProfile.DatabaseType.MYSQL);
        List<CompletionService.Suggestion> mySug =
                CompletionService.suggest(mysql, null, "select cur", "cur", CompletionService.Context.AFTER_SELECT_PROJECTION);
        List<String> myNames = mySug.stream().map(s -> s.text().toLowerCase()).toList();
        assertTrue(myNames.contains("curdate()"), "MySQL should suggest CURDATE()");
        assertTrue(myNames.contains("current_date"), "MySQL should suggest CURRENT_DATE");
    }

    @Test
    public void testDateAndTypeHighlighting() {
        String sql = "select b.bill_date_time::date, current_date from bbill b;";
        StyleSpans<Collection<String>> spans = SqlHighlighter.computeHighlighting(sql);
        assertNotNull(spans);

        // Verify 'date' is highlighted as sql-type
        boolean foundDateType = false;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\bdate\\b").matcher(sql);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            int currentOffset = 0;
            for (org.fxmisc.richtext.model.StyleSpan<Collection<String>> span : spans) {
                int nextOffset = currentOffset + span.getLength();
                if (currentOffset <= start && nextOffset >= end) {
                    if (span.getStyle().contains("sql-type")) {
                        foundDateType = true;
                        break;
                    }
                }
                currentOffset = nextOffset;
            }
        }
        assertTrue(foundDateType, "'date' must be highlighted as 'sql-type'");

        // Verify 'current_date' is highlighted as sql-keyword
        boolean foundCurrentDate = false;
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("(?i)\\bcurrent_date\\b").matcher(sql);
        while (m2.find()) {
            int start = m2.start();
            int end = m2.end();
            int currentOffset = 0;
            for (org.fxmisc.richtext.model.StyleSpan<Collection<String>> span : spans) {
                int nextOffset = currentOffset + span.getLength();
                if (currentOffset <= start && nextOffset >= end) {
                    if (span.getStyle().contains("sql-keyword")) {
                        foundCurrentDate = true;
                        break;
                    }
                }
                currentOffset = nextOffset;
            }
        }
        assertTrue(foundCurrentDate, "'current_date' must be highlighted as 'sql-keyword'");
    }
}


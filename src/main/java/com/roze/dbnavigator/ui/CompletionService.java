package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.util.AppExecutor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Professional contextual SQL code completion and live templates:
 *   - Live Templates: sel, selc, selw, ins, upd, del, etc.
 *   - After SELECT projection: FROM keyword as #1 suggestion
 *   - After FROM / JOIN: tables and views
 *   - After JOIN <table>: ON keyword
 *   - After ON: Smart join conditions (foreign key matches & common column equality)
 *   - After DROP TABLE / SEQUENCE / VIEW: CASCADE, RESTRICT, CASCADE CONSTRAINTS
 *   - After ON DELETE / ON UPDATE: CASCADE, RESTRICT, SET NULL, NO ACTION
 *   - "tableName." / "alias.": that table's columns
 */
public final class CompletionService {

    public enum Kind { KEYWORD, TABLE, COLUMN, SEQUENCE, TEMPLATE, JOIN_CONDITION }

    /** What the surrounding query structure tells us the user is about to type. */
    public enum Context {
        ANY,
        STATEMENT_START,
        AFTER_SELECT_PROJECTION,
        TABLES,
        AFTER_TABLE,
        AFTER_JOIN_TABLE,
        AFTER_ON,
        COLUMNS,
        SEQUENCES,
        POST_OBJECT_DDL,
        FOREIGN_KEY_ACTION
    }

    public record Suggestion(String text, Kind kind, String detail, String templateExpansion) {
        public Suggestion(String text, Kind kind, String detail) {
            this(text, kind, detail, null);
        }
        @Override public String toString() { return text; }
    }

    public record LiveTemplate(String abbreviation, String description, String expansion) {}

    private static final List<LiveTemplate> LIVE_TEMPLATES = List.of(
            new LiveTemplate("sel", "select all rows from a table", "select *\nfrom $END$"),
            new LiveTemplate("selc", "select the number of specific rows in a table", "select count(*)\nfrom $END$"),
            new LiveTemplate("selw", "select specific rows from a table", "select *\nfrom $END$\nwhere "),
            new LiveTemplate("ins", "insert statement", "insert into $END$ () values ();"),
            new LiveTemplate("upd", "update statement", "update $END$\nset \nwhere ;"),
            new LiveTemplate("del", "delete statement", "delete from $END$\nwhere ;"),
            new LiveTemplate("crat", "create table", "create table $END$ (\n    id serial primary key\n);"),
            new LiveTemplate("crav", "create view", "create view $END$ as\nselect * from ;"),
            new LiveTemplate("crai", "create index", "create index idx_$END$ on ()"),
            new LiveTemplate("altt", "alter table", "alter table $END$ add column "),
            new LiveTemplate("dropt", "drop table with cascade", "drop table $END$ cascade;"),
            new LiveTemplate("drops", "drop sequence with cascade", "drop sequence $END$ cascade;")
    );

    private static final List<String> STATEMENT_KEYWORDS = List.of(
            "SELECT", "INSERT INTO", "UPDATE", "DELETE FROM", "WITH",
            "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "TRUNCATE TABLE",
            "CREATE VIEW", "ALTER VIEW", "DROP VIEW",
            "CREATE SEQUENCE", "ALTER SEQUENCE", "DROP SEQUENCE",
            "CREATE INDEX", "DROP INDEX", "SECURITY LABEL",
            "BEGIN", "COMMIT", "ROLLBACK", "GRANT", "REVOKE",
            "SHOW", "DESCRIBE", "EXPLAIN", "USE");

    private static final List<String> GENERAL_KEYWORDS = List.of(
            "SELECT", "FROM", "WHERE", "INSERT INTO", "VALUES", "UPDATE", "SET",
            "DELETE FROM", "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "CREATE INDEX",
            "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "CROSS JOIN", "ON",
            "GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT",
            "AND", "OR", "NOT", "NULL", "IS NULL", "IS NOT NULL", "IN", "BETWEEN",
            "LIKE", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "AS",
            "CASCADE", "RESTRICT", "CASCADE CONSTRAINTS", "PURGE",
            "COUNT(*)", "SUM(", "AVG(", "MIN(", "MAX(", "COALESCE(", "CAST(",
            "PRIMARY KEY", "FOREIGN KEY", "REFERENCES", "DEFAULT", "UNIQUE", "CONSTRAINT",
            "BEGIN", "COMMIT", "ROLLBACK", "TRUNCATE TABLE", "ASC", "DESC",
            "SEQUENCE", "CREATE SEQUENCE", "ALTER SEQUENCE", "DROP SEQUENCE",
            "NEXTVAL(", "CURRVAL(", "SETVAL(");

    private static final List<String> SQLSERVER_KEYWORDS = List.of(
            "TOP", "IDENTITY(", "MERGE", "USING", "WHEN MATCHED THEN", "WHEN NOT MATCHED THEN",
            "OUTPUT", "INSERTED.", "DELETED.", "CROSS APPLY", "OUTER APPLY",
            "WITH (NOLOCK)", "OFFSET 0 ROWS", "FETCH NEXT", "ROWS ONLY",
            "OVER (", "PARTITION BY", "ROW_NUMBER()", "RANK()", "DENSE_RANK()",
            "ISNULL(", "IIF(", "TRY_CAST(", "TRY_CONVERT(", "CONVERT(",
            "GETDATE()", "GETUTCDATE()", "SYSDATETIME()", "DATEADD(", "DATEDIFF(", "DATEPART(",
            "LEN(", "SUBSTRING(", "STUFF(", "CHARINDEX(", "PATINDEX(", "STRING_AGG(", "FORMAT(",
            "DECLARE", "PRINT", "EXEC", "EXECUTE", "RAISERROR(", "THROW", "WAITFOR DELAY",
            "BEGIN TRAN", "BEGIN TRANSACTION", "COMMIT TRAN", "ROLLBACK TRAN", "GO",
            "NVARCHAR(", "VARCHAR(", "NCHAR(", "DATETIME2", "UNIQUEIDENTIFIER", "BIT",
            "SCOPE_IDENTITY()", "@@IDENTITY", "IDENT_CURRENT(", "@@ROWCOUNT");

    private static final List<String> ORACLE_KEYWORDS = List.of(
            "ROWNUM", "ROWID", "DUAL", "MINUS", "CONNECT BY", "START WITH", "PRIOR",
            "CASCADE CONSTRAINTS", "PURGE",
            "MERGE INTO", "USING", "WHEN MATCHED THEN", "WHEN NOT MATCHED THEN",
            "NVL(", "NVL2(", "DECODE(", "TO_CHAR(", "TO_DATE(", "TO_NUMBER(", "TO_TIMESTAMP(",
            "SYSDATE", "SYSTIMESTAMP", "TRUNC(", "EXTRACT(", "ADD_MONTHS(", "MONTHS_BETWEEN(",
            "SUBSTR(", "INSTR(", "LENGTH(", "LPAD(", "RPAD(", "LISTAGG(", "REGEXP_LIKE(",
            "REGEXP_SUBSTR(", "REGEXP_REPLACE(",
            "OVER (", "PARTITION BY", "ROW_NUMBER()", "RANK()", "DENSE_RANK()",
            "FETCH FIRST", "ROWS ONLY", "OFFSET", "ROWS FETCH NEXT",
            "VARCHAR2(", "NUMBER(", "CLOB", "BLOB", "DATE", "TIMESTAMP",
            "CREATE OR REPLACE", "PACKAGE", "PACKAGE BODY", "PROCEDURE", "FUNCTION",
            "TRIGGER", "IS", "AS", "DECLARE", "BEGIN", "EXCEPTION", "WHEN OTHERS THEN",
            "RAISE", "RAISE_APPLICATION_ERROR(", "PRAGMA", "END;", "DBMS_OUTPUT.PUT_LINE(",
            "CREATE USER", "IDENTIFIED BY", "GRANT", "REVOKE", "QUOTA UNLIMITED ON",
            "CREATE OR REPLACE VIEW", "CREATE SYNONYM", "ALL_TABLES", "USER_TABLES", "ALL_TAB_COLUMNS");

    private static final int MAX_SUGGESTIONS = 35;

    /** cacheKey = profileId::catalog */
    private static final Map<String, List<String>> tableCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> allColumnsCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> sequenceCache = new ConcurrentHashMap<>();
    /** cacheKey = profileId::catalog::table */
    private static final Map<String, List<String>> columnCache = new ConcurrentHashMap<>();

    private CompletionService() {}

    private static String key(ConnectionProfile profile, String catalog) {
        return profile.getId() + "::" + (catalog == null ? "" : catalog);
    }

    /** Kick off background loading of table, column, and sequence names for a console. */
    public static void preload(ConnectionProfile profile, String catalog) {
        String key = key(profile, catalog);
        if (tableCache.containsKey(key)) return;
        tableCache.put(key, List.of());
        allColumnsCache.put(key, List.of());
        sequenceCache.put(key, List.of());
        AppExecutor.run(() -> {
            tableCache.put(key, MetadataService.listAllTables(profile, catalog));
            allColumnsCache.put(key, MetadataService.listAllColumns(profile, catalog));
            sequenceCache.put(key, MetadataService.listAllSequences(profile, catalog));
        });
    }

    /** Used by "Invalidate Caches" — forces every table/column cache to reload on next use. */
    public static void clearAllCaches() {
        tableCache.clear();
        allColumnsCache.clear();
        sequenceCache.clear();
        columnCache.clear();
    }

    /**
     * Ranked suggestions for the token under the caret.
     */
    public static List<Suggestion> suggest(ConnectionProfile profile, String catalog,
                                           String fullText, String token, Context context) {
        if (token == null) token = "";

        // "users.na" or "b.na" (alias) → columns of the real table
        int dot = token.lastIndexOf('.');
        if (dot > 0) {
            String aliasOrTable = token.substring(0, dot);
            String prefix = token.substring(dot + 1).toLowerCase(Locale.ROOT);
            String resolvedTable = com.roze.dbnavigator.util.SqlAliases.resolve(fullText)
                    .getOrDefault(aliasOrTable.toLowerCase(Locale.ROOT), aliasOrTable);

            List<Suggestion> matches = new ArrayList<>();
            for (String col : columnsOf(profile, catalog, resolvedTable)) {
                if (col.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    matches.add(new Suggestion(aliasOrTable + "." + col, Kind.COLUMN, "column of " + resolvedTable));
                    if (matches.size() >= MAX_SUGGESTIONS) break;
                }
            }
            return matches;
        }

        if (token.isBlank() && context == Context.ANY) return List.of();

        String prefix = token.toLowerCase(Locale.ROOT);
        boolean isUpper = isAllUpperCase(token);
        List<Suggestion> matches = new ArrayList<>();

        switch (context) {
            case STATEMENT_START -> {
                addLiveTemplates(prefix, isUpper, matches);
                addKeywordsList(profile, STATEMENT_KEYWORDS, prefix, isUpper, matches);
                addTables(profile, catalog, prefix, matches);
            }
            case AFTER_SELECT_PROJECTION -> {
                addSpecificKeyword("FROM", "table source clause", prefix, isUpper, matches);
                addSpecificKeyword("DISTINCT", "filter duplicates", prefix, isUpper, matches);
                addSpecificKeyword("AS", "alias", prefix, isUpper, matches);
                addFunctions(prefix, isUpper, matches);
                addScopedColumns(profile, catalog, fullText, prefix, matches);
            }
            case TABLES -> {
                addTables(profile, catalog, prefix, matches);
                if ("(".startsWith(prefix) || prefix.startsWith("(")) {
                    matches.add(new Suggestion("(SELECT ", Kind.KEYWORD, "subquery"));
                }
            }
            case AFTER_TABLE -> {
                addAfterTableKeywords(prefix, isUpper, matches);
            }
            case AFTER_JOIN_TABLE -> {
                addSpecificKeyword("ON", "join condition clause", prefix, isUpper, matches);
            }
            case AFTER_ON -> {
                addSmartJoinConditions(profile, catalog, fullText, prefix, matches);
                addScopedColumns(profile, catalog, fullText, prefix, matches);
            }
            case POST_OBJECT_DDL -> {
                addSpecificKeyword("CASCADE", "drop dependent objects (Postgres/MySQL/Oracle)", prefix, isUpper, matches);
                addSpecificKeyword("RESTRICT", "restrict drop if dependencies exist", prefix, isUpper, matches);
                if (profile != null && profile.getType() == ConnectionProfile.DatabaseType.ORACLE) {
                    addSpecificKeyword("CASCADE CONSTRAINTS", "Oracle drop referential integrity constraints", prefix, isUpper, matches);
                    addSpecificKeyword("PURGE", "Oracle bypass recycle bin", prefix, isUpper, matches);
                }
            }
            case FOREIGN_KEY_ACTION -> {
                addSpecificKeyword("CASCADE", "cascade delete or update", prefix, isUpper, matches);
                addSpecificKeyword("RESTRICT", "restrict delete or update", prefix, isUpper, matches);
                addSpecificKeyword("SET NULL", "set foreign key columns to NULL", prefix, isUpper, matches);
                addSpecificKeyword("SET DEFAULT", "set foreign key columns to default", prefix, isUpper, matches);
                addSpecificKeyword("NO ACTION", "prevent delete or update", prefix, isUpper, matches);
            }
            case SEQUENCES -> {
                addSequences(profile, catalog, prefix, matches);
                addKeywordsList(profile, GENERAL_KEYWORDS, prefix, isUpper, matches);
            }
            case COLUMNS -> {
                addScopedColumns(profile, catalog, fullText, prefix, matches);
                addKeywordsList(profile, GENERAL_KEYWORDS, prefix, isUpper, matches);
            }
            case ANY -> {
                addLiveTemplates(prefix, isUpper, matches);
                addTables(profile, catalog, prefix, matches);
                addScopedColumns(profile, catalog, fullText, prefix, matches);
                addSequences(profile, catalog, prefix, matches);
                addKeywordsList(profile, GENERAL_KEYWORDS, prefix, isUpper, matches);
            }
        }
        return matches;
    }

    private static void addLiveTemplates(String prefix, boolean isUpper, List<Suggestion> matches) {
        for (LiveTemplate tmpl : LIVE_TEMPLATES) {
            if (matches.size() >= MAX_SUGGESTIONS) return;
            if (tmpl.abbreviation().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                String exp = isUpper ? tmpl.expansion().toUpperCase(Locale.ROOT).replace("$end$", "$END$") : tmpl.expansion();
                String name = isUpper ? tmpl.abbreviation().toUpperCase(Locale.ROOT) : tmpl.abbreviation();
                matches.add(new Suggestion(name, Kind.TEMPLATE, tmpl.description(), exp));
            }
        }
    }

    private static void addSpecificKeyword(String keyword, String detail, String prefix, boolean isUpper, List<Suggestion> matches) {
        String formatted = isUpper ? keyword.toUpperCase(Locale.ROOT) : keyword.toLowerCase(Locale.ROOT);
        if (prefix.isBlank() || formatted.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            if (notPresent(matches, formatted)) {
                matches.add(new Suggestion(formatted, Kind.KEYWORD, detail));
            }
        }
    }

    private static void addAfterTableKeywords(String prefix, boolean isUpper, List<Suggestion> matches) {
        List<String> nextKeywords = List.of(
                "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "CROSS JOIN",
                "WHERE", "GROUP BY", "HAVING", "ORDER BY", "LIMIT", "OFFSET", "UNION");
        for (String kw : nextKeywords) {
            if (matches.size() >= MAX_SUGGESTIONS) return;
            String formatted = isUpper ? kw.toUpperCase(Locale.ROOT) : kw.toLowerCase(Locale.ROOT);
            if (prefix.isBlank() || formatted.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                if (notPresent(matches, formatted)) {
                    matches.add(new Suggestion(formatted, Kind.KEYWORD, "clause"));
                }
            }
        }
    }

    private static void addFunctions(String prefix, boolean isUpper, List<Suggestion> matches) {
        List<String> funcs = List.of("COUNT(*)", "SUM(", "AVG(", "MIN(", "MAX(", "COALESCE(", "CAST(");
        for (String fn : funcs) {
            if (matches.size() >= MAX_SUGGESTIONS) return;
            String formatted = isUpper ? fn.toUpperCase(Locale.ROOT) : fn.toLowerCase(Locale.ROOT);
            if (prefix.isBlank() || formatted.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                if (notPresent(matches, formatted)) {
                    matches.add(new Suggestion(formatted, Kind.KEYWORD, "aggregate function"));
                }
            }
        }
    }

    private static void addSmartJoinConditions(ConnectionProfile profile, String catalog,
                                               String fullText, String prefix, List<Suggestion> matches) {
        Map<String, String> aliasMap = com.roze.dbnavigator.util.SqlAliases.resolve(fullText);
        List<String> tables = com.roze.dbnavigator.util.SqlAliases.referencedTables(fullText);

        if (tables.size() >= 2) {
            String table2 = tables.get(tables.size() - 1);
            String table1 = tables.get(tables.size() - 2);

            String alias1 = findAlias(aliasMap, table1);
            String alias2 = findAlias(aliasMap, table2);

            String prefix1 = alias1 != null ? alias1 : table1;
            String prefix2 = alias2 != null ? alias2 : table2;

            List<String> cols1 = columnsOf(profile, catalog, table1);
            List<String> cols2 = columnsOf(profile, catalog, table2);

            // 1. FK pattern: table1_id in table2 = id in table1
            for (String c2 : cols2) {
                String c2Low = c2.toLowerCase(Locale.ROOT);
                for (String c1 : cols1) {
                    String c1Low = c1.toLowerCase(Locale.ROOT);
                    if (c2Low.equals(table1.toLowerCase(Locale.ROOT) + "_id") && c1Low.equals("id")) {
                        String cond = prefix1 + "." + c1 + " = " + prefix2 + "." + c2;
                        if (prefix.isBlank() || cond.toLowerCase(Locale.ROOT).contains(prefix)) {
                            if (notPresent(matches, cond)) {
                                matches.add(new Suggestion(cond, Kind.JOIN_CONDITION, "foreign key match"));
                            }
                        }
                    } else if (c1Low.equals(table2.toLowerCase(Locale.ROOT) + "_id") && c2Low.equals("id")) {
                        String cond = prefix1 + "." + c1 + " = " + prefix2 + "." + c2;
                        if (prefix.isBlank() || cond.toLowerCase(Locale.ROOT).contains(prefix)) {
                            if (notPresent(matches, cond)) {
                                matches.add(new Suggestion(cond, Kind.JOIN_CONDITION, "foreign key match"));
                            }
                        }
                    }
                }
            }

            // 2. Common column name match
            for (String c1 : cols1) {
                for (String c2 : cols2) {
                    if (c1.equalsIgnoreCase(c2)) {
                        String cond = prefix1 + "." + c1 + " = " + prefix2 + "." + c2;
                        if (prefix.isBlank() || cond.toLowerCase(Locale.ROOT).contains(prefix)) {
                            if (notPresent(matches, cond)) {
                                matches.add(new Suggestion(cond, Kind.JOIN_CONDITION, "matching column"));
                            }
                        }
                    }
                }
            }
        }
    }

    private static String findAlias(Map<String, String> aliasMap, String table) {
        for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(table)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void addTables(ConnectionProfile profile, String catalog,
                                  String prefix, List<Suggestion> matches) {
        for (String table : tableCache.getOrDefault(key(profile, catalog), List.of())) {
            if (matches.size() >= MAX_SUGGESTIONS) return;
            if (prefix.isBlank() || table.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                if (notPresent(matches, table)) {
                    matches.add(new Suggestion(table, Kind.TABLE, "table"));
                }
            }
        }
    }

    private static void addScopedColumns(ConnectionProfile profile, String catalog,
                                         String fullText, String prefix, List<Suggestion> matches) {
        Set<String> seenColumns = new HashSet<>();
        List<String> tables = com.roze.dbnavigator.util.SqlAliases.referencedTables(fullText);
        if (!tables.isEmpty()) {
            for (String table : tables) {
                for (String col : columnsOf(profile, catalog, table)) {
                    if (matches.size() >= MAX_SUGGESTIONS) return;
                    if (prefix.isBlank() || col.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        String text = col;
                        if (notPresent(matches, text) && seenColumns.add(text.toLowerCase(Locale.ROOT))) {
                            matches.add(new Suggestion(text, Kind.COLUMN, "column of " + table));
                        }
                    }
                }
            }
        }
        addColumns(profile, catalog, prefix, matches);
    }

    private static void addColumns(ConnectionProfile profile, String catalog,
                                   String prefix, List<Suggestion> matches) {
        for (String column : allColumnsCache.getOrDefault(key(profile, catalog), List.of())) {
            if (matches.size() >= MAX_SUGGESTIONS) return;
            if (prefix.isBlank() || column.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                if (notPresent(matches, column)) {
                    matches.add(new Suggestion(column, Kind.COLUMN, "column"));
                }
            }
        }
    }

    private static void addSequences(ConnectionProfile profile, String catalog,
                                     String prefix, List<Suggestion> matches) {
        for (String sequence : sequenceCache.getOrDefault(key(profile, catalog), List.of())) {
            if (matches.size() >= MAX_SUGGESTIONS) return;
            if (prefix.isBlank() || sequence.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                if (notPresent(matches, sequence)) {
                    matches.add(new Suggestion(sequence, Kind.SEQUENCE, "sequence"));
                }
            }
        }
    }

    private static void addKeywordsList(ConnectionProfile profile, List<String> keywords,
                                        String prefix, boolean isUpper, List<Suggestion> matches) {
        if (prefix.isBlank()) return;
        for (String keyword : keywords) {
            if (matches.size() >= MAX_SUGGESTIONS) return;
            String formatted = isUpper ? keyword.toUpperCase(Locale.ROOT) : keyword.toLowerCase(Locale.ROOT);
            if (formatted.toLowerCase(Locale.ROOT).startsWith(prefix) && notPresent(matches, formatted)) {
                matches.add(new Suggestion(formatted, Kind.KEYWORD, "keyword"));
            }
        }
        if (profile != null && profile.getType() == ConnectionProfile.DatabaseType.SQLSERVER) {
            for (String keyword : SQLSERVER_KEYWORDS) {
                if (matches.size() >= MAX_SUGGESTIONS) return;
                String formatted = isUpper ? keyword.toUpperCase(Locale.ROOT) : keyword.toLowerCase(Locale.ROOT);
                if (formatted.toLowerCase(Locale.ROOT).startsWith(prefix) && notPresent(matches, formatted)) {
                    matches.add(new Suggestion(formatted, Kind.KEYWORD, "T-SQL"));
                }
            }
        }
        if (profile != null && profile.getType() == ConnectionProfile.DatabaseType.ORACLE) {
            for (String keyword : ORACLE_KEYWORDS) {
                if (matches.size() >= MAX_SUGGESTIONS) return;
                String formatted = isUpper ? keyword.toUpperCase(Locale.ROOT) : keyword.toLowerCase(Locale.ROOT);
                if (formatted.toLowerCase(Locale.ROOT).startsWith(prefix) && notPresent(matches, formatted)) {
                    matches.add(new Suggestion(formatted, Kind.KEYWORD, "PL/SQL"));
                }
            }
        }
    }

    private static boolean notPresent(List<Suggestion> matches, String text) {
        return matches.stream().noneMatch(s -> s.text().equalsIgnoreCase(text));
    }

    private static boolean isAllUpperCase(String text) {
        if (text == null || text.isEmpty()) return false;
        boolean hasLetter = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
                if (!Character.isUpperCase(c)) return false;
            }
        }
        return hasLetter;
    }

    private static List<String> columnsOf(ConnectionProfile profile, String catalog, String table) {
        String cacheKey = key(profile, catalog) + "::" + table.toLowerCase(Locale.ROOT);
        List<String> cached = columnCache.get(cacheKey);
        if (cached != null) return cached;
        List<String> columns = MetadataService.listColumns(profile, catalog, table);
        columnCache.put(cacheKey, columns);
        return columns;
    }
}

package com.roze.dbnavigator.search;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.ui.action.AnAction;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;

/**
 * DataGrip-grade search item representing tables, views, columns, sequences,
 * keys, constraints, indexes, consoles, SQL files, actions, and text occurrences.
 */
public class SearchHit {

    public enum Type {
        TABLE, VIEW, COLUMN, SEQUENCE, PRIMARY_KEY, INDEX, CONSTRAINT,
        CONSOLE, FILE, ACTION, TEXT
    }

    public enum Category {
        ALL, DATABASE, FILES, CODE, ACTIONS, TEXT
    }

    private final Type type;
    private final String name;
    private final String detail;
    private final String fullUri;
    private final ConnectionProfile profile;
    private final DbObject dbObject;
    private final String parentTable;
    private final String columnName;
    private final AnAction action;
    private final File file;
    private final String lineSnippet;
    private final int lineNumber;
    private final boolean isPrimaryKey;
    private final boolean isForeignKey;
    private final boolean isUniqueIndex;

    public SearchHit(Builder b) {
        this.type = b.type;
        this.name = b.name;
        this.detail = b.detail;
        this.fullUri = b.fullUri;
        this.profile = b.profile;
        this.dbObject = b.dbObject;
        this.parentTable = b.parentTable;
        this.columnName = b.columnName;
        this.action = b.action;
        this.file = b.file;
        this.lineSnippet = b.lineSnippet;
        this.lineNumber = b.lineNumber;
        this.isPrimaryKey = b.isPrimaryKey;
        this.isForeignKey = b.isForeignKey;
        this.isUniqueIndex = b.isUniqueIndex;
    }

    public Type getType() { return type; }
    public String getName() { return name; }
    public String getDetail() { return detail; }
    public String getFullUri() { return fullUri; }
    public ConnectionProfile getProfile() { return profile; }
    public DbObject getDbObject() { return dbObject; }
    public String getParentTable() { return parentTable; }
    public String getColumnName() { return columnName; }
    public AnAction getAction() { return action; }
    public File getFile() { return file; }
    public String getLineSnippet() { return lineSnippet; }
    public int getLineNumber() { return lineNumber; }
    public boolean isPrimaryKey() { return isPrimaryKey; }
    public boolean isForeignKey() { return isForeignKey; }
    public boolean isUniqueIndex() { return isUniqueIndex; }

    public Category getCategory() {
        return switch (type) {
            case TABLE, VIEW, COLUMN, SEQUENCE, PRIMARY_KEY, INDEX, CONSTRAINT -> Category.DATABASE;
            case CONSOLE, FILE -> Category.FILES;
            case ACTION -> Category.ACTIONS;
            case TEXT -> Category.TEXT;
        };
    }

    public FontAwesomeSolid getIcon() {
        return switch (type) {
            case TABLE -> FontAwesomeSolid.TABLE;
            case VIEW -> FontAwesomeSolid.EYE;
            case COLUMN -> FontAwesomeSolid.COLUMNS;
            case SEQUENCE -> FontAwesomeSolid.SORT_NUMERIC_DOWN;
            case PRIMARY_KEY, CONSTRAINT -> FontAwesomeSolid.KEY;
            case INDEX -> FontAwesomeSolid.FINGERPRINT;
            case CONSOLE -> FontAwesomeSolid.TERMINAL;
            case FILE -> FontAwesomeSolid.FILE_CODE;
            case ACTION -> FontAwesomeSolid.COG;
            case TEXT -> FontAwesomeSolid.FONT;
        };
    }

    public String getIconColor() {
        return switch (type) {
            case TABLE, VIEW -> "#4a88c7";
            case COLUMN -> isPrimaryKey ? "#e5a50a" : (isForeignKey ? "#4a88c7" : "#8ca0b8");
            case SEQUENCE -> "#6897bb";
            case PRIMARY_KEY -> "#e5a50a";
            case CONSTRAINT -> "#c59e38";
            case INDEX -> "#389fd6";
            case CONSOLE -> "#5c9bb7";
            case FILE -> "#a9b7c6";
            case ACTION -> "#9876aa";
            case TEXT -> "#cfaf60";
        };
    }

    public String getBadge() {
        if (isPrimaryKey) return "PK";
        if (isForeignKey) return "FK";
        if (type == Type.SEQUENCE) return "123";
        if (type == Type.INDEX && isUniqueIndex) return "i_u";
        if (type == Type.INDEX) return "idx";
        return null;
    }

    /**
     * DataGrip ranking score:
     * - Exact match on name gets top priority.
     * - Prefix match gets secondary boost.
     * - Tables and Columns match higher than actions or text.
     */
    public int calculateScore(String query) {
        if (query == null || query.isBlank()) return 0;
        String q = query.trim().toLowerCase();
        String n = name != null ? name.toLowerCase() : "";
        int score = 0;

        if (n.equals(q)) {
            score += 2000;
        } else if (n.startsWith(q)) {
            score += 1000;
        } else if (n.contains(q)) {
            score += 500;
        } else {
            // Check token sequence match (e.g. "app tx" or "app_tx" matching "appointment_tx")
            String[] tokens = q.split("[ _-]+");
            if (tokens.length > 1) {
                boolean allMatch = true;
                int lastIdx = 0;
                for (String token : tokens) {
                    if (token.isEmpty()) continue;
                    int found = n.indexOf(token, lastIdx);
                    if (found < 0) {
                        allMatch = false;
                        break;
                    }
                    lastIdx = found + token.length();
                }
                if (allMatch) {
                    score += 350;
                }
            }
        }

        if (parentTable != null && parentTable.toLowerCase().contains(q)) {
            score += 200;
        }
        if (detail != null && detail.toLowerCase().contains(q)) {
            score += 50;
        }

        // Object kind priority boost (Database objects strongly dominate over actions)
        score += switch (type) {
            case TABLE -> 1200;
            case COLUMN -> 1000;
            case VIEW -> 900;
            case SEQUENCE -> 800;
            case PRIMARY_KEY, INDEX, CONSTRAINT -> 700;
            case CONSOLE, FILE -> 400;
            case ACTION -> 50;
            case TEXT -> 20;
        };

        return score;
    }

    public static class Builder {
        private Type type = Type.TABLE;
        private String name = "";
        private String detail = "";
        private String fullUri = "";
        private ConnectionProfile profile;
        private DbObject dbObject;
        private String parentTable;
        private String columnName;
        private AnAction action;
        private File file;
        private String lineSnippet;
        private int lineNumber = 0;
        private boolean isPrimaryKey;
        private boolean isForeignKey;
        private boolean isUniqueIndex;

        public Builder type(Type t) { this.type = t; return this; }
        public Builder name(String n) { this.name = n; return this; }
        public Builder detail(String d) { this.detail = d; return this; }
        public Builder fullUri(String u) { this.fullUri = u; return this; }
        public Builder profile(ConnectionProfile p) { this.profile = p; return this; }
        public Builder dbObject(DbObject obj) { this.dbObject = obj; return this; }
        public Builder parentTable(String pt) { this.parentTable = pt; return this; }
        public Builder columnName(String col) { this.columnName = col; return this; }
        public Builder action(AnAction a) { this.action = a; return this; }
        public Builder file(File f) { this.file = f; return this; }
        public Builder lineSnippet(String s) { this.lineSnippet = s; return this; }
        public Builder lineNumber(int ln) { this.lineNumber = ln; return this; }
        public Builder primaryKey(boolean pk) { this.isPrimaryKey = pk; return this; }
        public Builder foreignKey(boolean fk) { this.isForeignKey = fk; return this; }
        public Builder uniqueIndex(boolean u) { this.isUniqueIndex = u; return this; }

        public SearchHit build() {
            return new SearchHit(this);
        }
    }
}

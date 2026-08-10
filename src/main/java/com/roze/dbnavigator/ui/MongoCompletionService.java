package com.roze.dbnavigator.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autocomplete for the MongoDB shell console, matching the reference IDE's
 * three completion contexts: top-level ({@code db}/{@code use}), right
 * after {@code db.} (collection names plus database-level methods), and
 * right after {@code db.collection.} (the methods this console actually
 * supports — only real, working ones are suggested, so accepting a
 * suggestion never leads to an "unsupported method" error).
 */
public final class MongoCompletionService {

    public enum Kind { KEYWORD, COLLECTION, METHOD }

    public record Suggestion(String text, Kind kind, String detail) {}

    public enum Context { TOP_LEVEL, DB_MEMBER, COLLECTION_MEMBER }

    private static final Pattern COLLECTION_MEMBER_PATTERN =
            Pattern.compile("(?s)db\\s*\\.\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\.\\s*[A-Za-z0-9_]*$");
    private static final Pattern DB_MEMBER_PATTERN =
            Pattern.compile("(?s)db\\s*\\.\\s*[A-Za-z0-9_]*$");

    private static final List<Suggestion> DB_METHODS = List.of(
            new Suggestion("createCollection", Kind.METHOD, "db.createCollection(name)"),
            new Suggestion("dropDatabase", Kind.METHOD, "db.dropDatabase()"),
            new Suggestion("getCollectionNames", Kind.METHOD, "db.getCollectionNames()"),
            new Suggestion("stats", Kind.METHOD, "db.stats()"),
            new Suggestion("getSiblingDB", Kind.METHOD, "db.getSiblingDB(name)"),
            new Suggestion("serverStatus", Kind.METHOD, "db.serverStatus()"),
            new Suggestion("version", Kind.METHOD, "db.version()")
    );

    // Only methods this console's parser + MongoDbClient actually execute —
    // suggesting anything else would just lead to "Unsupported method".
    private static final List<Suggestion> COLLECTION_METHODS = List.of(
            new Suggestion("find", Kind.METHOD, "db.collection.find(filter)"),
            new Suggestion("findOne", Kind.METHOD, "db.collection.findOne(filter)"),
            new Suggestion("insertOne", Kind.METHOD, "db.collection.insertOne(doc)"),
            new Suggestion("insertMany", Kind.METHOD, "db.collection.insertMany([docs])"),
            new Suggestion("updateOne", Kind.METHOD, "db.collection.updateOne(filter, update)"),
            new Suggestion("updateMany", Kind.METHOD, "db.collection.updateMany(filter, update)"),
            new Suggestion("deleteOne", Kind.METHOD, "db.collection.deleteOne(filter)"),
            new Suggestion("deleteMany", Kind.METHOD, "db.collection.deleteMany(filter)"),
            new Suggestion("countDocuments", Kind.METHOD, "db.collection.countDocuments(filter)"),
            new Suggestion("drop", Kind.METHOD, "db.collection.drop()")
    );

    private MongoCompletionService() {}

    public static Context contextAt(String textBeforeCaret) {
        if (COLLECTION_MEMBER_PATTERN.matcher(textBeforeCaret).find()) return Context.COLLECTION_MEMBER;
        if (DB_MEMBER_PATTERN.matcher(textBeforeCaret).find()) return Context.DB_MEMBER;
        return Context.TOP_LEVEL;
    }

    /** The collection name typed before the trailing dot, for a COLLECTION_MEMBER context — or null otherwise. */
    public static String collectionNameAt(String textBeforeCaret) {
        Matcher m = COLLECTION_MEMBER_PATTERN.matcher(textBeforeCaret);
        return m.find() ? m.group(1) : null;
    }

    public static List<Suggestion> suggest(Context context, String token, List<String> collectionNames) {
        List<Suggestion> pool = switch (context) {
            case TOP_LEVEL -> List.of(
                    new Suggestion("use", Kind.KEYWORD, "use <database>;"),
                    new Suggestion("db", Kind.KEYWORD, "current database"));
            case DB_MEMBER -> {
                List<Suggestion> l = new ArrayList<>();
                for (String name : collectionNames) l.add(new Suggestion(name, Kind.COLLECTION, "collection"));
                l.addAll(DB_METHODS);
                yield l;
            }
            case COLLECTION_MEMBER -> COLLECTION_METHODS;
        };
        if (token.isBlank()) return pool;
        String lower = token.toLowerCase(Locale.ROOT);
        List<Suggestion> filtered = new ArrayList<>();
        for (Suggestion s : pool) {
            if (s.text().toLowerCase(Locale.ROOT).startsWith(lower)) filtered.add(s);
        }
        return filtered;
    }
}

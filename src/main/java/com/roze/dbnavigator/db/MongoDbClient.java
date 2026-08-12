package com.roze.dbnavigator.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.QueryResult;
import org.bson.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** MongoDB client — browses databases/collections and runs JSON filter queries. */
public class MongoDbClient implements AutoCloseable {

    private final ConnectionProfile profile;
    private final MongoClient client;

    public MongoDbClient(ConnectionProfile profile) {
        this.profile = profile;
        this.client = MongoClients.create(profile.getMongoUri());
    }

    public ConnectionProfile getProfile() { return profile; }

    public List<String> listDatabases() {
        List<String> names = new ArrayList<>();
        client.listDatabaseNames().forEach(names::add);
        return names;
    }

    public List<String> listCollections(String database) {
        List<String> names = new ArrayList<>();
        client.getDatabase(database).listCollectionNames().forEach(names::add);
        return names;
    }

    public long countDocuments(String database, String collection, String jsonFilter) {
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        Document filter = parseFilter(jsonFilter);
        return coll.countDocuments(filter);
    }

    /**
     * Runs a find() with an optional JSON filter and returns documents flattened
     * into a table (union of top-level keys across the page).
     */
    public QueryResult find(String database, String collection, String jsonFilter,
                            int skip, int limit) {
        return find(database, collection, jsonFilter, null, false, skip, limit);
    }

    /**
     * @param sortField null for no sort (natural order); otherwise a real
     *                  server-side {@code .sort({field: 1|-1})} — this sorts
     *                  the complete collection, not just whatever page is
     *                  currently loaded, same principle as the SQL grids.
     */
    public QueryResult find(String database, String collection, String jsonFilter,
                            String sortField, boolean descending, int skip, int limit) {
        long start = System.currentTimeMillis();
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        Document filter = parseFilter(jsonFilter);

        var cursor = coll.find(filter);
        if (sortField != null && !sortField.isBlank()) {
            cursor = cursor.sort(new Document(sortField, descending ? -1 : 1));
        }

        List<Document> docs = new ArrayList<>();
        cursor.skip(skip).limit(limit).forEach(docs::add);

        QueryResult result = new QueryResult();
        Set<String> keys = new LinkedHashSet<>();
        keys.add("_id");
        for (Document doc : docs) keys.addAll(doc.keySet());
        result.getColumns().addAll(keys);

        for (Document doc : docs) {
            List<String> row = new ArrayList<>(keys.size());
            for (String key : keys) {
                Object value = doc.get(key);
                if (value == null) {
                    row.add(null);
                } else if (value instanceof Document nested) {
                    row.add(nested.toJson());
                } else {
                    row.add(String.valueOf(value));
                }
            }
            result.getRows().add(row);
        }

        result.setExecutionMillis(System.currentTimeMillis() - start);
        return result;
    }

    /** Runs an arbitrary database command, e.g. {"dbStats": 1}. */
    public String runCommand(String database, String jsonCommand) {
        MongoDatabase db = client.getDatabase(database);
        Document command = Document.parse(jsonCommand);
        return db.runCommand(command).toJson();
    }

    /** Collection names for the current database — used by the console's autocomplete after "db.". */
    public List<String> listCollectionNames(String database) {
        List<String> names = new ArrayList<>();
        client.getDatabase(database).listCollectionNames().forEach(names::add);
        return names;
    }

    public void ping() {
        client.getDatabase("admin").runCommand(new Document("ping", 1));
    }

    /** One inferred field, from sampling documents — Mongo has no fixed schema, so this is a best effort. */
    public record FieldInfo(String name, String type) {}

    /**
     * Infers a collection's top-level fields and their types by sampling
     * documents, since MongoDB collections have no fixed schema to read
     * directly. The first type seen for each field name wins if documents
     * disagree — good enough for the tree view's purposes; the actual data
     * grid already shows each document's real values regardless.
     */
    public List<FieldInfo> inferFields(String database, String collection, int sampleSize) {
        Map<String, String> typeByField = new LinkedHashMap<>();
        typeByField.put("_id", "ObjectId");   // present on virtually every document; guarantee it's first

        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        for (Document doc : coll.find().limit(sampleSize)) {
            collectFields(doc, "", typeByField, 0);
        }

        List<FieldInfo> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : typeByField.entrySet()) {
            result.add(new FieldInfo(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * Recursively walks a document's fields, adding a dot-notation entry
     * for each nested sub-field (e.g. "address.city") right alongside the
     * parent object field itself ("address") — matching the reference
     * IDE's own field tree exactly, which lists both. Capped at a modest
     * depth purely as a safety net against unusually deep documents.
     */
    private static void collectFields(Document doc, String prefix, Map<String, String> typeByField, int depth) {
        if (depth > 4) return;
        for (String key : doc.keySet()) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = doc.get(key);
            typeByField.putIfAbsent(fullKey, bsonTypeName(value));
            if (value instanceof Document nested) {
                collectFields(nested, fullKey, typeByField, depth + 1);
            }
        }
    }

    /** One index, with a DataGrip-style "(keys) UNIQUE" detail string ready to display. */
    public record IndexInfo(String name, String detail) {}

    public List<IndexInfo> listIndexes(String database, String collection) {
        List<IndexInfo> result = new ArrayList<>();
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        for (Document index : coll.listIndexes()) {
            String name = index.getString("name");
            Document key = index.get("key", Document.class);
            String keyDescription = key == null ? "" : String.join(", ", key.keySet());
            boolean unique = Boolean.TRUE.equals(index.getBoolean("unique"));
            result.add(new IndexInfo(name, "(" + keyDescription + ")" + (unique ? " UNIQUE" : "")));
        }
        return result;
    }

    /** Same shape as find(), but for a single document — renders in the same grid as a one-row result. */
    public QueryResult findOne(String database, String collection, String jsonFilter) {
        return find(database, collection, jsonFilter, null, false, 0, 1);
    }

    /** Result of a write/admin operation, for the console's Output log — no rows to show, just a summary. */
    public record CommandResult(String message) {}

    public CommandResult insertOne(String database, String collection, String docJson) {
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        Document doc = Document.parse(docJson);
        coll.insertOne(doc);
        Object id = doc.get("_id");
        return new CommandResult("Inserted 1 document" + (id != null ? " (_id: " + id + ")" : ""));
    }

    public CommandResult insertMany(String database, String collection, List<Document> docs) {
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        coll.insertMany(docs);
        return new CommandResult("Inserted " + docs.size() + " document(s)");
    }

    public CommandResult updateOne(String database, String collection, String filterJson, String updateJson) {
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        var result = coll.updateOne(Document.parse(filterJson), Document.parse(updateJson));
        return new CommandResult("Matched " + result.getMatchedCount()
                + ", modified " + result.getModifiedCount() + " document(s)");
    }

    public CommandResult updateMany(String database, String collection, String filterJson, String updateJson) {
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        var result = coll.updateMany(Document.parse(filterJson), Document.parse(updateJson));
        return new CommandResult("Matched " + result.getMatchedCount()
                + ", modified " + result.getModifiedCount() + " document(s)");
    }

    public CommandResult deleteOne(String database, String collection, String filterJson) {
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        var result = coll.deleteOne(Document.parse(filterJson));
        return new CommandResult("Deleted " + result.getDeletedCount() + " document(s)");
    }

    public CommandResult deleteMany(String database, String collection, String filterJson) {
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        var result = coll.deleteMany(Document.parse(filterJson));
        return new CommandResult("Deleted " + result.getDeletedCount() + " document(s)");
    }

    public CommandResult drop(String database, String collection) {
        client.getDatabase(database).getCollection(collection).drop();
        return new CommandResult("Dropped collection \"" + collection + "\"");
    }

    public CommandResult renameCollection(String database, String oldName, String newName) {
        client.getDatabase(database).getCollection(oldName)
                .renameCollection(new com.mongodb.MongoNamespace(database, newName));
        return new CommandResult("Renamed \"" + oldName + "\" to \"" + newName + "\"");
    }

    public CommandResult createIndex(String database, String collection, String fieldName,
                                     boolean descending, boolean unique) {
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        Document keys = new Document(fieldName, descending ? -1 : 1);
        var options = new com.mongodb.client.model.IndexOptions().unique(unique);
        String name = coll.createIndex(keys, options);
        return new CommandResult("Created index \"" + name + "\"");
    }

    public CommandResult dropIndex(String database, String collection, String indexName) {
        client.getDatabase(database).getCollection(collection).dropIndex(indexName);
        return new CommandResult("Dropped index \"" + indexName + "\"");
    }

    private static String bsonTypeName(Object value) {
        if (value == null) return "Null";
        if (value instanceof org.bson.types.ObjectId) return "ObjectId";
        if (value instanceof String) return "String";
        if (value instanceof Integer) return "Int32";
        if (value instanceof Long) return "Int64";
        if (value instanceof Double) return "Double";
        if (value instanceof Boolean) return "Boolean";
        if (value instanceof java.util.Date || value instanceof java.time.Instant) return "ISODate";
        if (value instanceof org.bson.types.Decimal128) return "Decimal128";
        if (value instanceof List) return "Array";
        if (value instanceof Document) return "Object";
        return value.getClass().getSimpleName();
    }

    private static Document parseFilter(String jsonFilter) {
        if (jsonFilter == null || jsonFilter.isBlank()) return new Document();
        return Document.parse(jsonFilter);
    }

    @Override
    public void close() {
        client.close();
    }
}

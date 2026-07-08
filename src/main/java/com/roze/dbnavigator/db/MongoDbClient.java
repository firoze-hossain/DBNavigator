package com.roze.dbnavigator.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.QueryResult;
import org.bson.Document;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
        long start = System.currentTimeMillis();
        MongoCollection<Document> coll = client.getDatabase(database).getCollection(collection);
        Document filter = parseFilter(jsonFilter);

        List<Document> docs = new ArrayList<>();
        coll.find(filter).skip(skip).limit(limit).forEach(docs::add);

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

    public void ping() {
        client.getDatabase("admin").runCommand(new Document("ping", 1));
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

package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * DataGrip-grade Virtual Foreign Key & Inferred Relationship Engine.
 *
 * Automatically detects and infers logical relationships between database tables
 * based on column naming patterns (e.g. queueqtype_id -> queueqtype.id,
 * queueqsub_type_id -> queueqsub_type.id) when physical foreign key constraints
 * do not exist in the database (e.g. ClickHouse, Cassandra, SQLite, legacy schemas,
 * or Hibernate/JPA schemas without DDL constraints).
 */
public final class VirtualForeignKeyService {

    private VirtualForeignKeyService() {}

    /**
     * Infers relationships (both outgoing parent tables and incoming child tables)
     * for a root table in a 1-hop ER diagram.
     */
    public static List<MetadataService.ForeignKey> inferRelatedForeignKeys(
            ConnectionProfile profile,
            DbObject rootTable,
            List<MetadataService.ColumnInfo> rootColumns,
            List<MetadataService.ForeignKey> existingPhysicalFks) {

        if (profile == null || rootTable == null) {
            return Collections.emptyList();
        }

        // Check if Virtual Foreign Keys are enabled in Settings
        AppSettingsStore.DiagramsConfig cfg = AppSettingsStore.load().getDiagrams();
        if (cfg != null && cfg.getEnabledCategories() != null
                && !cfg.getEnabledCategories().contains("Virtual foreign keys")) {
            return Collections.emptyList();
        }

        List<MetadataService.ForeignKey> inferred = new ArrayList<>();
        Set<String> existingPairs = new HashSet<>();
        if (existingPhysicalFks != null) {
            for (MetadataService.ForeignKey fk : existingPhysicalFks) {
                existingPairs.add(normKey(fk.fromTable(), fk.fromColumn(), fk.toTable(), fk.toColumn()));
            }
        }

        List<DbObject> allTables;
        try {
            allTables = MetadataService.loadDatabaseTables(profile, rootTable.getCatalog());
        } catch (Exception e) {
            allTables = Collections.emptyList();
        }

        if (allTables.isEmpty()) {
            return inferred;
        }

        // Filter to current schema if available, else all tables
        String rootSchema = rootTable.getSchema();
        List<DbObject> candidateTables = new ArrayList<>();
        for (DbObject t : allTables) {
            if (rootSchema == null || rootSchema.isBlank() || rootSchema.equalsIgnoreCase(t.getSchema())) {
                candidateTables.add(t);
            }
        }
        if (candidateTables.isEmpty()) {
            candidateTables.addAll(allTables);
        }

        TableIndex index = TableIndex.build(candidateTables);

        // 1. Outgoing Relationships (rootTable.column -> parentTable.id)
        if (rootColumns != null) {
            for (MetadataService.ColumnInfo col : rootColumns) {
                if (col.primaryKey()) continue;

                String colName = col.name();
                DbObject targetTable = resolveTargetTable(colName, index, rootTable);
                if (targetTable != null) {
                    List<MetadataService.ColumnInfo> targetCols;
                    try {
                        targetCols = MetadataService.loadColumnInfo(profile, targetTable);
                    } catch (Exception ignored) {
                        targetCols = Collections.emptyList();
                    }

                    MetadataService.ColumnInfo targetPk = findTargetPkColumn(targetCols);
                    String targetPkName = targetPk != null ? targetPk.name() : "id";
                    String targetPkType = targetPk != null ? targetPk.typeName() : null;

                    if (isTypeCompatible(col.typeName(), targetPkType)) {
                        String pairKey = normKey(rootTable.getName(), colName, targetTable.getName(), targetPkName);
                        if (!existingPairs.contains(pairKey)) {
                            inferred.add(new MetadataService.ForeignKey(
                                    rootTable.getSchema(), rootTable.getName(), colName,
                                    targetTable.getSchema(), targetTable.getName(), targetPkName,
                                    "inferred:" + colName + " -> " + targetPkName
                            ));
                            existingPairs.add(pairKey);
                        }
                    }
                }
            }
        }

        // 2. Incoming Relationships (childTable.column -> rootTable.id)
        MetadataService.ColumnInfo rootPk = findTargetPkColumn(rootColumns);
        String rootPkName = rootPk != null ? rootPk.name() : "id";
        String rootPkType = rootPk != null ? rootPk.typeName() : null;

        List<String> candidateColNames = generateCandidateForeignKeyColumnNames(rootTable.getName());
        discoverIncomingForeignKeys(profile, rootTable, rootPkName, rootPkType, candidateTables, candidateColNames, existingPairs, inferred);

        return inferred;
    }

    /**
     * Infers relationships among all given tables for the database-wide diagram.
     */
    public static List<MetadataService.ForeignKey> inferForeignKeysForTables(
            ConnectionProfile profile,
            String catalog,
            List<DbObject> tables,
            Map<String, List<MetadataService.ColumnInfo>> tableColumnsMap,
            List<MetadataService.ForeignKey> existingPhysicalFks) {

        if (tables == null || tables.isEmpty()) {
            return Collections.emptyList();
        }

        AppSettingsStore.DiagramsConfig cfg = AppSettingsStore.load().getDiagrams();
        if (cfg != null && cfg.getEnabledCategories() != null
                && !cfg.getEnabledCategories().contains("Virtual foreign keys")) {
            return Collections.emptyList();
        }

        List<MetadataService.ForeignKey> inferred = new ArrayList<>();
        Set<String> existingPairs = new HashSet<>();
        if (existingPhysicalFks != null) {
            for (MetadataService.ForeignKey fk : existingPhysicalFks) {
                existingPairs.add(normKey(fk.fromTable(), fk.fromColumn(), fk.toTable(), fk.toColumn()));
            }
        }

        TableIndex index = TableIndex.build(tables);

        for (DbObject sourceTable : tables) {
            List<MetadataService.ColumnInfo> cols = tableColumnsMap != null ? tableColumnsMap.get(sourceTable.getName()) : null;
            if (cols == null && profile != null) {
                try {
                    cols = MetadataService.loadColumnInfo(profile, sourceTable);
                } catch (Exception ignored) {
                    cols = Collections.emptyList();
                }
            }
            if (cols == null) continue;

            for (MetadataService.ColumnInfo col : cols) {
                if (col.primaryKey()) continue;

                String colName = col.name();
                DbObject targetTable = resolveTargetTable(colName, index, sourceTable);
                if (targetTable != null) {
                    List<MetadataService.ColumnInfo> targetCols = tableColumnsMap != null ? tableColumnsMap.get(targetTable.getName()) : null;
                    if (targetCols == null && profile != null) {
                        try {
                            targetCols = MetadataService.loadColumnInfo(profile, targetTable);
                        } catch (Exception ignored) {
                            targetCols = Collections.emptyList();
                        }
                    }

                    MetadataService.ColumnInfo targetPk = findTargetPkColumn(targetCols);
                    String targetPkName = targetPk != null ? targetPk.name() : "id";
                    String targetPkType = targetPk != null ? targetPk.typeName() : null;

                    if (isTypeCompatible(col.typeName(), targetPkType)) {
                        String pairKey = normKey(sourceTable.getName(), colName, targetTable.getName(), targetPkName);
                        if (!existingPairs.contains(pairKey)) {
                            inferred.add(new MetadataService.ForeignKey(
                                    sourceTable.getSchema(), sourceTable.getName(), colName,
                                    targetTable.getSchema(), targetTable.getName(), targetPkName,
                                    "inferred:" + colName + " -> " + targetPkName
                            ));
                            existingPairs.add(pairKey);
                        }
                    }
                }
            }
        }

        return inferred;
    }

    /**
     * Resolves which target table a foreign key column candidate points to.
     */
    public static DbObject resolveTargetTable(String colName, TableIndex index, DbObject selfTable) {
        if (colName == null || colName.isBlank() || index == null) return null;
        String lower = colName.toLowerCase(Locale.ROOT);

        // Audit, measurement, and non-FK columns to ignore
        if (lower.equals("id") || lower.endsWith("_at") || lower.endsWith("_time")
                || lower.endsWith("_date") || lower.endsWith("_datetime") || lower.endsWith("_by")
                || lower.endsWith("_count") || lower.endsWith("_version")
                || lower.endsWith("_num") || lower.endsWith("_number")
                || lower.endsWith("_amount") || lower.endsWith("_total")
                || lower.endsWith("_percent") || lower.endsWith("_flag")
                || lower.endsWith("_status") || lower.endsWith("_state")
                || lower.endsWith("_description") || lower.endsWith("_desc")
                || lower.endsWith("_address") || lower.endsWith("_ip")) {
            return null;
        }

        List<String> prefixes = extractCandidatePrefixes(lower);
        for (String prefix : prefixes) {
            if (prefix.isBlank()) continue;

            // Self-referencing (parent_id, master_id, prev_id)
            if (prefix.equals("parent") || prefix.equals("master") || prefix.equals("prev") || prefix.equals("next")) {
                return selfTable;
            }

            // 1. Exact match
            DbObject match = index.exactMap.get(prefix);
            if (match != null && !isSameTable(match, selfTable)) return match;

            // 2. Normalized match (without underscores/hyphens)
            String norm = prefix.replaceAll("[^a-zA-Z0-9]", "");
            match = index.normMap.get(norm);
            if (match != null && !isSameTable(match, selfTable)) return match;

            // 3. Plural match (user_id -> users table, category_id -> categories table)
            String plural = toPlural(prefix);
            match = index.exactMap.get(plural);
            if (match != null && !isSameTable(match, selfTable)) return match;
            match = index.normMap.get(plural.replaceAll("[^a-zA-Z0-9]", ""));
            if (match != null && !isSameTable(match, selfTable)) return match;

            // 4. Singular match (orders_id -> order table, categories_id -> category table)
            String singular = toSingular(prefix);
            match = index.exactMap.get(singular);
            if (match != null && !isSameTable(match, selfTable)) return match;
            match = index.normMap.get(singular.replaceAll("[^a-zA-Z0-9]", ""));
            if (match != null && !isSameTable(match, selfTable)) return match;
        }

        return null;
    }

    /**
     * Extracts potential entity/table prefixes from a column name.
     */
    public static List<String> extractCandidatePrefixes(String lower) {
        List<String> prefixes = new ArrayList<>();
        if (lower.endsWith("_id") && lower.length() > 3) {
            prefixes.add(lower.substring(0, lower.length() - 3));
        }
        if (lower.endsWith("_key") && lower.length() > 4) {
            prefixes.add(lower.substring(0, lower.length() - 4));
        }
        if (lower.endsWith("_code") && lower.length() > 5) {
            prefixes.add(lower.substring(0, lower.length() - 5));
        }
        if (lower.endsWith("_no") && lower.length() > 3) {
            prefixes.add(lower.substring(0, lower.length() - 3));
        }
        if (lower.startsWith("id_") && lower.length() > 3) {
            prefixes.add(lower.substring(3));
        }
        if (lower.endsWith("id") && lower.length() > 3 && !lower.endsWith("_id")) {
            prefixes.add(lower.substring(0, lower.length() - 2));
        }
        return prefixes;
    }

    /**
     * Generates likely column names on child tables that would reference the given parent table.
     */
    public static List<String> generateCandidateForeignKeyColumnNames(String parentTableName) {
        List<String> list = new ArrayList<>();
        if (parentTableName == null || parentTableName.isBlank()) return list;

        String lower = parentTableName.toLowerCase(Locale.ROOT);
        list.add(lower + "_id");
        list.add(lower + "id");
        list.add("id_" + lower);

        String singular = toSingular(lower);
        if (!singular.equalsIgnoreCase(lower)) {
            list.add(singular + "_id");
            list.add(singular + "id");
            list.add("id_" + singular);
        }

        return list;
    }

    /**
     * Finds the primary key column or defaults to 'id'.
     */
    public static MetadataService.ColumnInfo findTargetPkColumn(List<MetadataService.ColumnInfo> cols) {
        if (cols == null || cols.isEmpty()) return null;
        for (MetadataService.ColumnInfo c : cols) {
            if (c.primaryKey()) return c;
        }
        for (MetadataService.ColumnInfo c : cols) {
            if ("id".equalsIgnoreCase(c.name())) return c;
        }
        return cols.get(0);
    }

    /**
     * Verifies type compatibility between foreign key column and target primary key column.
     */
    public static boolean isTypeCompatible(String typeA, String typeB) {
        if (typeA == null || typeB == null || typeA.isBlank() || typeB.isBlank()) return true;
        String a = typeA.toLowerCase(Locale.ROOT);
        String b = typeB.toLowerCase(Locale.ROOT);

        boolean numA = isNumericType(a);
        boolean numB = isNumericType(b);
        if (numA && numB) return true;

        boolean strA = isStringType(a);
        boolean strB = isStringType(b);
        if (strA && strB) return true;

        boolean uuidA = a.contains("uuid") || a.contains("guid");
        boolean uuidB = b.contains("uuid") || b.contains("guid");
        if (uuidA && uuidB) return true;

        return a.equals(b);
    }

    private static boolean isNumericType(String t) {
        return t.contains("int") || t.contains("serial") || t.contains("number")
                || t.contains("numeric") || t.contains("decimal") || t.contains("long");
    }

    private static boolean isStringType(String t) {
        return t.contains("char") || t.contains("text") || t.contains("clob") || t.contains("string");
    }

    public static String toPlural(String singular) {
        if (singular == null || singular.isBlank()) return singular;
        if (singular.endsWith("y") && !singular.endsWith("ay") && !singular.endsWith("ey") && !singular.endsWith("oy") && !singular.endsWith("uy")) {
            return singular.substring(0, singular.length() - 1) + "ies";
        }
        if (singular.endsWith("s") || singular.endsWith("x") || singular.endsWith("z") || singular.endsWith("ch") || singular.endsWith("sh")) {
            return singular + "es";
        }
        return singular + "s";
    }

    public static String toSingular(String plural) {
        if (plural == null || plural.isBlank()) return plural;
        if (plural.endsWith("ies") && plural.length() > 3) {
            return plural.substring(0, plural.length() - 3) + "y";
        }
        if (plural.endsWith("es") && plural.length() > 3) {
            return plural.substring(0, plural.length() - 2);
        }
        if (plural.endsWith("s") && plural.length() > 2) {
            return plural.substring(0, plural.length() - 1);
        }
        return plural;
    }

    private static boolean isSameTable(DbObject a, DbObject b) {
        if (a == null || b == null) return false;
        return a.getName().equalsIgnoreCase(b.getName());
    }

    private static String normKey(String fromTable, String fromCol, String toTable, String toCol) {
        return (fromTable != null ? fromTable.toLowerCase(Locale.ROOT) : "") + "."
                + (fromCol != null ? fromCol.toLowerCase(Locale.ROOT) : "") + "->"
                + (toTable != null ? toTable.toLowerCase(Locale.ROOT) : "") + "."
                + (toCol != null ? toCol.toLowerCase(Locale.ROOT) : "");
    }

    private static void discoverIncomingForeignKeys(
            ConnectionProfile profile,
            DbObject rootTable,
            String rootPkName,
            String rootPkType,
            List<DbObject> candidateTables,
            List<String> candidateColNames,
            Set<String> existingPairs,
            List<MetadataService.ForeignKey> outInferred) {

        if (profile == null || candidateColNames.isEmpty()) return;

        // Try fast schema query on information_schema.columns if database supports it
        try (Connection conn = MetadataService.client(profile, rootTable.getCatalog()).getConnection()) {
            String schema = rootTable.getSchema();
            StringBuilder sql = new StringBuilder("SELECT table_name, column_name, data_type FROM information_schema.columns WHERE ");
            if (schema != null && !schema.isBlank()) {
                sql.append("table_schema = ? AND ");
            }
            sql.append("column_name IN (");
            for (int i = 0; i < candidateColNames.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
            }
            sql.append(")");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int paramIdx = 1;
                if (schema != null && !schema.isBlank()) {
                    stmt.setString(paramIdx++, schema);
                }
                for (String cName : candidateColNames) {
                    stmt.setString(paramIdx++, cName);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String childTable = rs.getString("table_name");
                        String childCol = rs.getString("column_name");
                        String childType = rs.getString("data_type");

                        if (!childTable.equalsIgnoreCase(rootTable.getName()) && isTypeCompatible(childType, rootPkType)) {
                            String pairKey = normKey(childTable, childCol, rootTable.getName(), rootPkName);
                            if (!existingPairs.contains(pairKey)) {
                                outInferred.add(new MetadataService.ForeignKey(
                                        schema, childTable, childCol,
                                        schema, rootTable.getName(), rootPkName,
                                        "inferred:" + childCol + " -> " + rootPkName
                                ));
                                existingPairs.add(pairKey);
                            }
                        }
                    }
                    return; // Successfully queried
                }
            } catch (SQLException ignored) {
                // information_schema not supported or permission denied; fallback to metadata inspect
            }

            // Fallback: JDBC DatabaseMetaData.getColumns
            DatabaseMetaData meta = conn.getMetaData();
            String cat = MetadataService.metaCatalog(profile, rootTable.getCatalog());
            for (String colNamePattern : candidateColNames) {
                try (ResultSet rs = meta.getColumns(cat, schema, "%", colNamePattern)) {
                    while (rs.next()) {
                        String childTable = rs.getString("TABLE_NAME");
                        String childCol = rs.getString("COLUMN_NAME");
                        String childType = rs.getString("TYPE_NAME");
                        if (!childTable.equalsIgnoreCase(rootTable.getName()) && isTypeCompatible(childType, rootPkType)) {
                            String pairKey = normKey(childTable, childCol, rootTable.getName(), rootPkName);
                            if (!existingPairs.contains(pairKey)) {
                                outInferred.add(new MetadataService.ForeignKey(
                                        schema, childTable, childCol,
                                        schema, rootTable.getName(), rootPkName,
                                        "inferred:" + childCol + " -> " + rootPkName
                                ));
                                existingPairs.add(pairKey);
                            }
                        }
                    }
                } catch (SQLException ignored) {}
            }
        } catch (Exception ignored) {}
    }

    /**
     * Fast in-memory lookup index for candidate tables.
     */
    public static class TableIndex {
        public final Map<String, DbObject> exactMap = new HashMap<>();
        public final Map<String, DbObject> normMap = new HashMap<>();

        public static TableIndex build(Collection<DbObject> tables) {
            TableIndex idx = new TableIndex();
            if (tables == null) return idx;
            for (DbObject t : tables) {
                if (t == null || t.getName() == null) continue;
                String lower = t.getName().toLowerCase(Locale.ROOT);
                idx.exactMap.putIfAbsent(lower, t);

                String norm = lower.replaceAll("[^a-zA-Z0-9]", "");
                idx.normMap.putIfAbsent(norm, t);
            }
            return idx;
        }
    }
}

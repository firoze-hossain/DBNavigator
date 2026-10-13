package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Service to generate DataGrip-compatible DDL for database objects
 * (tables, views, schemas, databases, routines, sequences, indexes).
 */
public final class DdlGeneratorService {

    private DdlGeneratorService() {}

    /**
     * Generates a DataGrip-style DDL definition for the specified object.
     */
    public static String generateDdl(ConnectionProfile profile, DbObject obj) throws Exception {
        if (profile == null || obj == null) return "-- No object selected";

        return switch (obj.getKind()) {
            case TABLE, PARTITION -> generateTableDdl(profile, obj);
            case VIEW -> generateViewDdl(profile, obj);
            case SCHEMA -> generateSchemaDdl(profile, obj);
            case DATABASE -> generateDatabaseDdl(profile, obj);
            case PROCEDURE, FUNCTION -> generateRoutineDdl(profile, obj);
            case SEQUENCE -> generateSequenceDdl(profile, obj);
            case INDEX -> generateIndexDdl(profile, obj);
            case ROLE -> generateRoleDdl(profile, obj);
            case TABLESPACE -> generateTablespaceDdl(profile, obj);
            case ACCESS_METHOD -> generateAccessMethodDdl(profile, obj);
            case CAST -> generateCastDdl(profile, obj);
            case EXTENSION -> generateExtensionDdl(profile, obj);
            case LANGUAGE -> generateLanguageDdl(profile, obj);
            case VIRTUAL_VIEW -> generateVirtualViewDdl(profile, obj);
            default -> "-- DDL not available for " + obj.getKind() + ": " + obj.getName();
        };
    }

    // ------------------------------------------------------------ Table DDL

    private static String generateTableDdl(ConnectionProfile profile, DbObject table) throws Exception {
        QueryResult columns = MetadataService.loadTableStructure(profile, table);
        QueryResult indexes = MetadataService.loadTableIndexes(profile, table);
        List<String> pk = new ArrayList<>();
        try {
            pk.addAll(MetadataService.loadPrimaryKeys(profile, table));
        } catch (Exception ignored) {}

        // Foreign keys map: fkColumnName -> ForeignKeyDef
        Map<String, ForeignKeyDef> singleColFks = new LinkedHashMap<>();
        List<CompositeForeignKeyDef> compositeFks = new ArrayList<>();
        loadForeignKeys(profile, table, singleColFks, compositeFks);

        // Fetch owner if PostgreSQL
        String owner = (profile.getType() == DatabaseType.POSTGRESQL)
                ? queryTableOwner(profile, table)
                : null;

        StringBuilder sb = new StringBuilder();
        sb.append("-- auto-generated definition\n");
        sb.append("create table ").append(table.getName()).append("\n(\n");

        List<String> bodyLines = new ArrayList<>();

        for (List<String> row : columns.getRows()) {
            // row: [Column, Type, Size, Nullable, Default, Key]
            String colName = row.get(0);
            String rawType = row.get(1);
            String size = row.get(2);
            boolean nullable = "YES".equalsIgnoreCase(row.get(3));
            String def = row.get(4);

            String formattedType = formatDataType(profile.getType(), rawType, size);
            StringBuilder line = new StringBuilder("    ")
                    .append(colName).append(' ').append(formattedType);

            // Default value (if not null / not empty)
            if (def != null && !def.isBlank() && !def.equalsIgnoreCase("null")) {
                line.append(" default ").append(def);
            }

            // Not null
            if (!nullable) {
                line.append(" not null");
            }

            // Column-level constraint/foreign key (DataGrip style)
            ForeignKeyDef fk = singleColFks.get(colName);
            if (fk != null) {
                line.append("\n        constraint ").append(fk.name())
                        .append("\n            references ").append(fk.targetTable());
                if (fk.targetColumn() != null && !fk.targetColumn().equalsIgnoreCase("id")
                        && !fk.targetColumn().equalsIgnoreCase(colName)) {
                    line.append(" (").append(fk.targetColumn()).append(')');
                }
            }

            bodyLines.add(line.toString());
        }

        // Composite foreign keys
        for (CompositeForeignKeyDef cfk : compositeFks) {
            String cfkLine = "    constraint " + cfk.name() + " foreign key ("
                    + String.join(", ", cfk.localColumns()) + ") references "
                    + cfk.targetTable() + " (" + String.join(", ", cfk.foreignColumns()) + ")";
            bodyLines.add(cfkLine);
        }

        // Primary key
        if (!pk.isEmpty()) {
            bodyLines.add("    primary key (" + String.join(", ", pk) + ")");
        }

        sb.append(String.join(",\n", bodyLines));
        sb.append("\n);\n");

        // Owner statement
        if (owner != null && !owner.isBlank()) {
            sb.append("\nalter table ").append(table.getName())
                    .append("\n    owner to ").append(owner).append(";\n");
        }

        // Secondary Indexes (excluding primary key indexes)
        Map<String, List<String>> indexColumns = new LinkedHashMap<>();
        Map<String, Boolean> uniqueMap = new LinkedHashMap<>();
        for (List<String> row : indexes.getRows()) {
            String idxName = row.get(0);
            if (idxName == null || isPkIndex(idxName, pk)) continue;
            indexColumns.computeIfAbsent(idxName, k -> new ArrayList<>()).add(row.get(1));
            uniqueMap.put(idxName, "YES".equalsIgnoreCase(row.get(2)));
        }

        for (var entry : indexColumns.entrySet()) {
            String idxName = entry.getKey();
            boolean isUnique = Boolean.TRUE.equals(uniqueMap.get(idxName));
            sb.append('\n');
            if (isUnique) {
                sb.append("create unique index ").append(idxName)
                        .append("\n    on ").append(table.getName())
                        .append(" (").append(String.join(", ", entry.getValue())).append(");\n");
            } else {
                sb.append("create index ").append(idxName)
                        .append("\n    on ").append(table.getName())
                        .append(" (").append(String.join(", ", entry.getValue())).append(");\n");
            }
        }

        return sb.toString();
    }

    private static boolean isPkIndex(String indexName, List<String> pk) {
        if (indexName == null) return false;
        String lower = indexName.toLowerCase(Locale.ROOT);
        return lower.endsWith("_pkey") || lower.equals("primary") || lower.startsWith("pk_");
    }

    private static String formatDataType(DatabaseType dbType, String rawType, String size) {
        String type = (rawType != null ? rawType.toLowerCase(Locale.ROOT) : "text");

        // Map PostgreSQL internal type names to standard SQL names
        if (dbType == DatabaseType.POSTGRESQL) {
            type = switch (type) {
                case "int8" -> "bigint";
                case "int4" -> "integer";
                case "int2" -> "smallint";
                case "bool" -> "boolean";
                case "character varying" -> "varchar";
                case "timestamptz" -> "timestamp with time zone";
                case "bpchar" -> "char";
                default -> type;
            };
        }

        if (needsSize(type) && size != null && !size.equals("0") && !size.equals("2147483647")) {
            return type + "(" + size + ")";
        }
        return type;
    }

    private static boolean needsSize(String type) {
        return type.contains("varchar") || type.equals("char")
                || type.equals("varbinary") || type.equals("binary");
    }

    private static void loadForeignKeys(ConnectionProfile profile, DbObject table,
                                       Map<String, ForeignKeyDef> singleColFks,
                                       List<CompositeForeignKeyDef> compositeFks) {
        try (Connection conn = ClientRegistry.jdbc(profile, table.getCatalog()).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String cat = table.getCatalog();
            if (profile.getType() == DatabaseType.POSTGRESQL) cat = null;

            Map<String, List<String>> localCols = new LinkedHashMap<>();
            Map<String, List<String>> targetCols = new LinkedHashMap<>();
            Map<String, String> targetTables = new LinkedHashMap<>();

            try (ResultSet rs = meta.getImportedKeys(cat, table.getSchema(), table.getName())) {
                while (rs.next()) {
                    String fkName = rs.getString("FK_NAME");
                    String fkCol = rs.getString("FKCOLUMN_NAME");
                    String pkTable = rs.getString("PKTABLE_NAME");
                    String pkCol = rs.getString("PKCOLUMN_NAME");

                    if (fkName == null) fkName = "fk_" + table.getName() + "_" + fkCol;

                    localCols.computeIfAbsent(fkName, k -> new ArrayList<>()).add(fkCol);
                    targetCols.computeIfAbsent(fkName, k -> new ArrayList<>()).add(pkCol);
                    targetTables.putIfAbsent(fkName, pkTable);
                }
            }

            for (String fkName : localCols.keySet()) {
                List<String> lCols = localCols.get(fkName);
                List<String> tCols = targetCols.get(fkName);
                String tTable = targetTables.get(fkName);

                if (lCols.size() == 1) {
                    singleColFks.put(lCols.get(0), new ForeignKeyDef(fkName, tTable, tCols.get(0)));
                } else {
                    compositeFks.add(new CompositeForeignKeyDef(fkName, tTable, lCols, tCols));
                }
            }
        } catch (Exception ignored) {
            // Non-fatal, DDL will still generate without foreign key constraints
        }
    }

    private static String queryTableOwner(ConnectionProfile profile, DbObject table) {
        String sql = "SELECT tableowner FROM pg_tables WHERE schemaname = ? AND tablename = ?";
        try (Connection conn = ClientRegistry.jdbc(profile, table.getCatalog()).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, table.getSchema() != null ? table.getSchema() : "public");
            stmt.setString(2, table.getName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception ignored) {}
        return profile.getUsername() != null && !profile.getUsername().isBlank()
                ? profile.getUsername()
                : "postgres";
    }

    // ------------------------------------------------------------ Schema DDL

    private static String generateSchemaDdl(ConnectionProfile profile, DbObject schema) {
        String owner = (profile.getType() == DatabaseType.POSTGRESQL)
                ? querySchemaOwner(profile, schema.getName())
                : profile.getUsername();

        StringBuilder sb = new StringBuilder();
        sb.append("-- auto-generated definition\n");
        sb.append("create schema ").append(schema.getName()).append(";\n");

        if (owner != null && !owner.isBlank()) {
            sb.append("\nalter schema ").append(schema.getName())
                    .append(" owner to ").append(owner).append(";\n");
        }
        return sb.toString();
    }

    private static String querySchemaOwner(ConnectionProfile profile, String schemaName) {
        String sql = "SELECT schema_owner FROM information_schema.schemata WHERE schema_name = ?";
        try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception ignored) {}
        return profile.getUsername() != null && !profile.getUsername().isBlank()
                ? profile.getUsername()
                : "postgres";
    }

    // ------------------------------------------------------------ Database DDL

    private static String generateDatabaseDdl(ConnectionProfile profile, DbObject database) {
        String owner = (profile.getType() == DatabaseType.POSTGRESQL)
                ? queryDatabaseOwner(profile, database.getName())
                : profile.getUsername();

        StringBuilder sb = new StringBuilder();
        sb.append("-- auto-generated definition\n");
        sb.append("create database ").append(database.getName());
        if (owner != null && !owner.isBlank() && profile.getType() == DatabaseType.POSTGRESQL) {
            sb.append("\n    with owner ").append(owner);
        }
        sb.append(";\n");
        return sb.toString();
    }

    private static String queryDatabaseOwner(ConnectionProfile profile, String databaseName) {
        String sql = "SELECT pg_catalog.pg_get_userbyid(datdba) FROM pg_database WHERE datname = ?";
        try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, databaseName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception ignored) {}
        return profile.getUsername() != null && !profile.getUsername().isBlank()
                ? profile.getUsername()
                : "postgres";
    }

    // ------------------------------------------------------------ View DDL

    private static String generateViewDdl(ConnectionProfile profile, DbObject view) {
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            String sql = "SELECT definition FROM pg_views WHERE schemaname = ? AND viewname = ?";
            try (Connection conn = ClientRegistry.jdbc(profile, view.getCatalog()).getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, view.getSchema() != null ? view.getSchema() : "public");
                stmt.setString(2, view.getName());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String def = rs.getString(1);
                        if (def != null && !def.isBlank()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("-- auto-generated definition\n");
                            sb.append("create view ").append(view.getName()).append(" as\n");
                            sb.append(def.trim());
                            if (!def.trim().endsWith(";")) sb.append(';');
                            sb.append('\n');

                            String owner = queryTableOwner(profile, view);
                            if (owner != null) {
                                sb.append("\nalter table ").append(view.getName())
                                        .append(" owner to ").append(owner).append(";\n");
                            }
                            return sb.toString();
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Generic fallback
        return "-- auto-generated definition\n"
                + "create view " + view.getName() + " as\n"
                + "select * from " + view.qualifiedName() + ";\n";
    }

    // ------------------------------------------------------------ Routine DDL

    private static String generateRoutineDdl(ConnectionProfile profile, DbObject routine) throws Exception {
        String source = MetadataService.getRoutineSource(profile, routine);
        if (source != null && !source.isBlank()) {
            if (!source.startsWith("--")) {
                return "-- auto-generated definition\n" + source;
            }
            return source;
        }
        return "-- Routine source definition for " + routine.getName() + "\n"
                + "CREATE OR REPLACE FUNCTION " + routine.qualifiedName() + "() ...;\n";
    }

    // ------------------------------------------------------------ Sequence DDL

    private static String generateSequenceDdl(ConnectionProfile profile, DbObject seq) {
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            String sql = "SELECT sequence_name, start_value, minimum_value, maximum_value, increment " +
                    "FROM information_schema.sequences WHERE sequence_schema = ? AND sequence_name = ?";
            try (Connection conn = ClientRegistry.jdbc(profile, seq.getCatalog()).getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, seq.getSchema() != null ? seq.getSchema() : "public");
                stmt.setString(2, seq.getName());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("-- auto-generated definition\n");
                        sb.append("create sequence ").append(seq.getName());
                        String inc = rs.getString("increment");
                        String min = rs.getString("minimum_value");
                        String max = rs.getString("maximum_value");
                        String start = rs.getString("start_value");

                        if (inc != null) sb.append("\n    increment by ").append(inc);
                        if (min != null) sb.append("\n    minvalue ").append(min);
                        if (max != null) sb.append("\n    maxvalue ").append(max);
                        if (start != null) sb.append("\n    start with ").append(start);
                        sb.append(";\n");

                        String owner = queryTableOwner(profile, seq);
                        if (owner != null) {
                            sb.append("\nalter sequence ").append(seq.getName())
                                    .append(" owner to ").append(owner).append(";\n");
                        }
                        return sb.toString();
                    }
                }
            } catch (Exception ignored) {}
        }

        return "-- auto-generated definition\ncreate sequence " + seq.getName() + ";\n";
    }

    // ------------------------------------------------------------ Index DDL

    private static String generateIndexDdl(ConnectionProfile profile, DbObject index) {
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            String sql = "SELECT indexdef FROM pg_indexes WHERE schemaname = ? AND indexname = ?";
            try (Connection conn = ClientRegistry.jdbc(profile, index.getCatalog()).getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, index.getSchema() != null ? index.getSchema() : "public");
                stmt.setString(2, index.getName());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String def = rs.getString(1);
                        if (def != null && !def.isBlank()) {
                            return "-- auto-generated definition\n" + def.trim() + ";\n";
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return "-- auto-generated definition\ncreate index " + index.getName() + ";\n";
    }

    // ------------------------------------------------------------ Role DDL

    private static String generateRoleDdl(ConnectionProfile profile, DbObject obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- auto-generated definition\n");
        String roleName = obj.getName();
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            String sql = "SELECT rolname, rolsuper, rolinherit, rolcreaterole, rolcreatedb, " +
                         "rolcanlogin, rolreplication, rolconnlimit FROM pg_roles WHERE rolname = ?";
            try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, roleName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        boolean superuser = rs.getBoolean("rolsuper");
                        boolean inherit = rs.getBoolean("rolinherit");
                        boolean createRole = rs.getBoolean("rolcreaterole");
                        boolean createDb = rs.getBoolean("rolcreatedb");
                        boolean canLogin = rs.getBoolean("rolcanlogin");
                        boolean replication = rs.getBoolean("rolreplication");
                        int connLimit = rs.getInt("rolconnlimit");

                        sb.append("create role ").append(roleName);
                        List<String> options = new ArrayList<>();
                        if (superuser) options.add("superuser");
                        if (createDb) options.add("createdb");
                        if (createRole) options.add("createrole");
                        if (!inherit) options.add("noinherit");
                        if (canLogin) options.add("login");
                        if (replication) options.add("replication");
                        if (connLimit >= 0) options.add("connection limit " + connLimit);

                        if (!options.isEmpty()) {
                            sb.append(" with ").append(String.join(" ", options));
                        }
                        sb.append(";\n");
                        return sb.toString();
                    }
                }
            } catch (Exception ignored) {}
        }
        sb.append("create role ").append(roleName).append(";\n");
        return sb.toString();
    }

    // ------------------------------------------------------------ Tablespace DDL

    private static String generateTablespaceDdl(ConnectionProfile profile, DbObject obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- auto-generated definition\n");
        String spcName = obj.getName();
        String owner = "postgres";
        String location = null;
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            String sql = "SELECT spcname, pg_get_userbyid(spcowner) AS spcowner, " +
                         "pg_tablespace_location(oid) AS spclocation FROM pg_tablespace WHERE spcname = ?";
            try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, spcName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String o = rs.getString("spcowner");
                        if (o != null && !o.isBlank()) owner = o;
                        location = rs.getString("spclocation");
                    }
                }
            } catch (Exception ignored) {}
        }
        sb.append("create tablespace ").append(spcName);
        if (owner != null && !owner.isBlank()) {
            sb.append(" owner ").append(owner);
        }
        if (location != null && !location.isBlank()) {
            sb.append(" location '").append(location).append("';\n");
        } else {
            sb.append(" location ???;\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ Access Method DDL

    private static String generateAccessMethodDdl(ConnectionProfile profile, DbObject obj) {
        StringBuilder sb = new StringBuilder();
        String amName = obj.getName();
        sb.append("-- access method ").append(amName).append("\n\n");
        String typeStr = "index";
        String handler = amName + "handler";
        String desc = null;

        if (profile.getType() == DatabaseType.POSTGRESQL) {
            String sql = "SELECT amname, amtype, amhandler::regproc::text AS handler, " +
                         "obj_description(oid, 'pg_am') AS description FROM pg_am WHERE amname = ?";
            try (Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, amName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String t = rs.getString("amtype");
                        if ("t".equalsIgnoreCase(t)) typeStr = "table";
                        else if ("i".equalsIgnoreCase(t)) typeStr = "index";
                        String h = rs.getString("handler");
                        if (h != null && !h.isBlank()) handler = h;
                        desc = rs.getString("description");
                    }
                }
            } catch (Exception ignored) {}
        }

        sb.append("create access method ").append(amName).append(" type ").append(typeStr);
        if (handler != null && !handler.isBlank()) {
            sb.append(" handler ").append(handler);
        }
        sb.append(";\n");

        if (desc != null && !desc.isBlank()) {
            sb.append("\ncomment on access method ").append(amName).append(" is '")
              .append(desc.replace("'", "''")).append("';\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ Cast DDL

    private static String generateCastDdl(ConnectionProfile profile, DbObject obj) {
        StringBuilder sb = new StringBuilder();
        String name = obj.getName();
        String source = name;
        String target = "";
        if (name.contains(" as ")) {
            String[] parts = name.split(" as ", 2);
            source = parts[0].trim();
            target = parts[1].trim();
        }

        sb.append("-- cast ").append(source).append(" as ").append(target).append("\n\n");
        sb.append("create cast (").append(source).append(" as ").append(target).append(")");

        String castFunc = target + "(" + source + ")";
        String castContext = null;
        String castMethod = "f";
        String desc = null;

        if (profile.getType() == DatabaseType.POSTGRESQL) {
            String sql = """
                SELECT
                    CASE WHEN c.castfunc = 0 THEN NULL ELSE c.castfunc::regprocedure::text END AS cast_func,
                    c.castcontext,
                    c.castmethod,
                    obj_description(c.oid, 'pg_cast') AS description
                FROM pg_cast c
                JOIN pg_type t1 ON c.castsource = t1.oid
                JOIN pg_type t2 ON c.casttarget = t2.oid
                WHERE t1.typname = ? AND t2.typname = ?
            """;
            try (Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, source);
                stmt.setString(2, target);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        castFunc = rs.getString("cast_func");
                        castContext = rs.getString("castcontext");
                        castMethod = rs.getString("castmethod");
                        desc = rs.getString("description");
                    }
                }
            } catch (Exception ignored) {}
        }

        if ("b".equalsIgnoreCase(castMethod)) {
            sb.append(" without function");
        } else if ("i".equalsIgnoreCase(castMethod)) {
            sb.append(" with inout");
        } else if (castFunc != null && !castFunc.isBlank()) {
            sb.append(" with function ").append(castFunc);
        }

        if ("a".equalsIgnoreCase(castContext)) {
            sb.append(" as assignment");
        } else if ("i".equalsIgnoreCase(castContext)) {
            sb.append(" as implicit");
        }
        sb.append(";\n");

        if (desc != null && !desc.isBlank()) {
            sb.append("\ncomment on cast (").append(source).append(" as ").append(target)
              .append(") is '").append(desc.replace("'", "''")).append("';\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ Extension DDL

    private static String generateExtensionDdl(ConnectionProfile profile, DbObject obj) {
        StringBuilder sb = new StringBuilder();
        String extName = obj.getName();
        sb.append("-- extension ").append(extName).append("\n\n");
        String schema = "pg_catalog";
        String version = "1.0";
        String desc = null;

        if (profile.getType() == DatabaseType.POSTGRESQL) {
            String sql = """
                SELECT e.extname, n.nspname AS extnamespace, e.extversion,
                       obj_description(e.oid, 'pg_extension') AS description
                FROM pg_extension e
                LEFT JOIN pg_namespace n ON e.extnamespace = n.oid
                WHERE e.extname = ?
            """;
            try (Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, extName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String s = rs.getString("extnamespace");
                        if (s != null && !s.isBlank()) schema = s;
                        String v = rs.getString("extversion");
                        if (v != null && !v.isBlank()) version = v;
                        desc = rs.getString("description");
                    }
                }
            } catch (Exception ignored) {}
        }

        sb.append("create extension ").append(extName);
        if (schema != null && !schema.isBlank()) {
            sb.append(" schema ").append(schema);
        }
        if (version != null && !version.isBlank()) {
            sb.append(" version '").append(version).append("'");
        }
        sb.append(";\n");

        if (desc != null && !desc.isBlank()) {
            sb.append("\ncomment on extension ").append(extName).append(" is '")
              .append(desc.replace("'", "''")).append("';\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ Language DDL

    private static String generateLanguageDdl(ConnectionProfile profile, DbObject obj) {
        StringBuilder sb = new StringBuilder();
        String langName = obj.getName();
        sb.append("-- language ").append(langName).append("\n\n");
        boolean isPl = false;
        boolean trusted = false;
        String callHandler = null;
        String inlineHandler = null;
        String validatorHandler = "c".equalsIgnoreCase(langName) ? "pg_catalog.fmgr_c_validator" : null;
        String desc = "c".equalsIgnoreCase(langName) ? "ISO C language for functions" : null;

        if (profile.getType() == DatabaseType.POSTGRESQL) {
            String sql = """
                SELECT
                    l.lanname,
                    l.lanispl,
                    l.lanpltrusted,
                    (SELECT n.nspname || '.' || p.proname FROM pg_proc p JOIN pg_namespace n ON p.pronamespace = n.oid WHERE p.oid = l.lanplcallfoid) AS call_handler,
                    (SELECT n.nspname || '.' || p.proname FROM pg_proc p JOIN pg_namespace n ON p.pronamespace = n.oid WHERE p.oid = l.laninline) AS inline_handler,
                    (SELECT n.nspname || '.' || p.proname FROM pg_proc p JOIN pg_namespace n ON p.pronamespace = n.oid WHERE p.oid = l.lanvalidator) AS validator_handler,
                    obj_description(l.oid, 'pg_language') AS description
                FROM pg_language l
                WHERE l.lanname = ?
            """;
            try (Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, langName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        isPl = rs.getBoolean("lanispl");
                        trusted = rs.getBoolean("lanpltrusted");
                        callHandler = rs.getString("call_handler");
                        inlineHandler = rs.getString("inline_handler");
                        validatorHandler = rs.getString("validator_handler");
                        desc = rs.getString("description");
                    }
                }
            } catch (Exception ignored) {}
        }

        sb.append("create ");
        if (trusted && isPl) {
            sb.append("trusted procedural ");
        } else if (isPl) {
            sb.append("procedural ");
        }
        sb.append("language ").append(langName);
        if (callHandler != null && !callHandler.isBlank()) {
            sb.append(" handler ").append(callHandler);
        }
        if (inlineHandler != null && !inlineHandler.isBlank()) {
            sb.append(" inline ").append(inlineHandler);
        }
        if (validatorHandler != null && !validatorHandler.isBlank()) {
            sb.append(" validator ").append(validatorHandler);
        }
        sb.append(";\n");

        if (desc != null && !desc.isBlank()) {
            sb.append("\ncomment on language ").append(langName).append(" is '")
              .append(desc.replace("'", "''")).append("';\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ Virtual View DDL

    private static String generateVirtualViewDdl(ConnectionProfile profile, DbObject obj) {
        StringBuilder sb = new StringBuilder();
        String catalog = obj.getCatalog() != null ? obj.getCatalog() : profile.getDatabase();
        sb.append("-- virtual view ").append(catalog != null ? catalog + "." : "").append(obj.getName()).append("\n\n");
        sb.append("create virtual view ").append(obj.getName()).append(" as\n");

        List<String> cols = new ArrayList<>();
        if (profile.getType() == DatabaseType.POSTGRESQL && "sessions".equalsIgnoreCase(obj.getName())) {
            String sql = "SELECT column_name FROM information_schema.columns " +
                         "WHERE table_name = 'pg_stat_activity' AND table_schema = 'pg_catalog' " +
                         "ORDER BY ordinal_position";
            try (Connection conn = ClientRegistry.jdbc(profile, obj.getCatalog()).getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    cols.add(rs.getString(1));
                }
            } catch (Exception ignored) {}
        }

        if (cols.isEmpty()) {
            cols = List.of(
                    "pid", "datname", "usename", "application_name",
                    "client_addr", "client_port", "backend_start", "xact_start",
                    "query_start", "state_change", "wait_event_type", "wait_event",
                    "state", "backend_xid", "backend_xmin", "query_id", "query",
                    "backend_type"
            );
        }

        sb.append("select ").append(cols.get(0));
        for (int i = 1; i < cols.size(); i++) {
            sb.append(",\n       ").append(cols.get(i));
        }
        sb.append("\nfrom pg_stat_activity;\n");
        return sb.toString();
    }



    // ------------------------------------------------------------ Records

    private record ForeignKeyDef(String name, String targetTable, String targetColumn) {}

    private record CompositeForeignKeyDef(String name, String targetTable,
                                          List<String> localColumns, List<String> foreignColumns) {}
}

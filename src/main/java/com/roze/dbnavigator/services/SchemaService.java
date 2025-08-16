package com.roze.dbnavigator.services;

import com.roze.dbnavigator.models.ConnectionProfile;
import com.roze.dbnavigator.models.DatabaseObject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchemaService {
    private static final String LOADING_NODE = "Loading...";

public static TreeItem<DatabaseObject> loadServerTree(ConnectionProfile profile) throws SQLException {
    System.out.println("Loading server tree for profile: " + profile.getName());
    DatabaseObject serverObj = new DatabaseObject(profile.getName(), DatabaseObject.Type.SERVER);
    TreeItem<DatabaseObject> rootItem = new TreeItem<>(serverObj);

    try (Connection conn = ConnectionService.getConnection(profile)) {
        System.out.println("Connection obtained, loading schemas...");
        if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
            DatabaseObject dbObj = new DatabaseObject(profile.getDatabase(), DatabaseObject.Type.DATABASE);
            TreeItem<DatabaseObject> dbItem = new TreeItem<>(dbObj);
            rootItem.getChildren().add(dbItem);

            loadDatabaseObjects(dbItem, conn, profile);
            System.out.println("Loaded " + dbItem.getChildren().size() + " schemas");
        }
    }
    return rootItem;
}

public static void loadChildren(TreeItem<DatabaseObject> parentItem, ConnectionProfile profile) {
    DatabaseObject parentObj = parentItem.getValue();

    try (Connection conn = ConnectionService.getConnection(profile)) {
        if (parentObj.getType() == DatabaseObject.Type.SERVER) {
            loadDatabases(parentItem, conn, profile);
        } else if (parentObj.getType() == DatabaseObject.Type.DATABASE) {
            loadDatabaseObjects(parentItem, conn, profile);
        } else if (parentObj.getType() == DatabaseObject.Type.SCHEMA) {
            String schemaName = parentObj.getName();
            loadSchemaObjects(parentItem, conn, profile, schemaName);
        } else if (parentObj.getType() == DatabaseObject.Type.TABLES_FOLDER) {
            loadTables(parentItem, profile);
        } else if (parentObj.getType() == DatabaseObject.Type.VIEWS_FOLDER) {
            loadViews(parentItem, profile);
        } else if (parentObj.getType() == DatabaseObject.Type.PROCEDURES_FOLDER) {
            loadProcedures(parentItem, profile);
        } else if (parentObj.getType() == DatabaseObject.Type.SEQUENCES_FOLDER) {
            loadSequences(parentItem, profile);
        }else if (parentObj.getType() == DatabaseObject.Type.FUNCTIONS_FOLDER) {
            loadFunctions(parentItem, profile);
        }
    } catch (SQLException e) {
        parentItem.getChildren().add(new TreeItem<>(
                new DatabaseObject("Error: " + e.getMessage(), DatabaseObject.Type.SYSTEM_FOLDER)));
    }
}

    public static void loadProcedures(TreeItem<DatabaseObject> procsItem, ConnectionProfile profile) {
        String schema = procsItem.getParent().getValue().getName(); // Get schema name from parent

        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = ConnectionService.getConnection(profile)) {
                    DatabaseMetaData meta = conn.getMetaData();
                    try (ResultSet rs = meta.getProcedures(null, schema, "%")) {
                        List<TreeItem<DatabaseObject>> procedures = new ArrayList<>();
                        while (rs.next()) {
                            String procName = rs.getString("PROCEDURE_NAME");
                            DatabaseObject procObj = new DatabaseObject(procName,
                                    DatabaseObject.Type.PROCEDURE, schema);
                            procedures.add(new TreeItem<>(procObj));
                        }

                        // Update UI on JavaFX thread
                        Platform.runLater(() -> {
                            procsItem.getChildren().clear();
                            procsItem.getChildren().addAll(procedures);
                            procsItem.getValue().setChildCount(procedures.size());
                        });
                    }
                }
                return null;
            }
        };

        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                procsItem.getChildren().clear();
                procsItem.getChildren().add(new TreeItem<>(
                        new DatabaseObject("Load failed: " + e.getSource().getException().getMessage(),
                                DatabaseObject.Type.SYSTEM_FOLDER)));
            });
        });

        new Thread(loadTask).start();
    }

    // Add this new method to load sequences
    public static void loadSequences(TreeItem<DatabaseObject> seqItem, ConnectionProfile profile) {
        String schema = seqItem.getParent().getValue().getName();

        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = ConnectionService.getConnection(profile)) {
                    // PostgreSQL specific query to get sequences
                    String sql = "SELECT sequence_name FROM information_schema.sequences " +
                            "WHERE sequence_schema = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, schema);
                        ResultSet rs = stmt.executeQuery();

                        List<TreeItem<DatabaseObject>> sequences = new ArrayList<>();
                        while (rs.next()) {
                            String seqName = rs.getString("sequence_name");
                            DatabaseObject seqObj = new DatabaseObject(seqName,
                                    DatabaseObject.Type.SEQUENCE, schema);
                            sequences.add(new TreeItem<>(seqObj));
                        }

                        Platform.runLater(() -> {
                            seqItem.getChildren().clear();
                            seqItem.getChildren().addAll(sequences);
                            seqItem.getValue().setChildCount(sequences.size());
                        });
                    }
                }
                return null;
            }
        };

        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                seqItem.getChildren().clear();
                seqItem.getChildren().add(new TreeItem<>(
                        new DatabaseObject("Load failed: " + e.getSource().getException().getMessage(),
                                DatabaseObject.Type.SYSTEM_FOLDER)));
            });
        });

        new Thread(loadTask).start();
    }

    private static void loadDatabases(TreeItem<DatabaseObject> parentItem, Connection conn, ConnectionProfile profile)
            throws SQLException {

        parentItem.getChildren().clear();

        if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {

                while (rs.next()) {
                    String dbName = rs.getString(1);
                    DatabaseObject dbObj = new DatabaseObject(dbName, DatabaseObject.Type.DATABASE);
                    TreeItem<DatabaseObject> dbItem = new TreeItem<>(dbObj);

                    // Add loading placeholder
                    dbItem.getChildren().add(new TreeItem<>(
                            new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER)));

                    parentItem.getChildren().add(dbItem);
                }
            }
        } else if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
            DatabaseObject dbObj = new DatabaseObject(profile.getDatabase(), DatabaseObject.Type.DATABASE);
            TreeItem<DatabaseObject> dbItem = new TreeItem<>(dbObj);

            // Add loading placeholder
            dbItem.getChildren().add(new TreeItem<>(
                    new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER)));

            parentItem.getChildren().add(dbItem);
        }
    }

    private static void loadDatabaseObjects(TreeItem<DatabaseObject> dbItem, Connection conn,
                                            ConnectionProfile profile) throws SQLException {

        dbItem.getChildren().clear();

        if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getSchemas()) {
                while (rs.next()) {
                    String schemaName = rs.getString("TABLE_SCHEM");
                    if (!schemaName.startsWith("pg_") && !schemaName.equals("information_schema")) {
                        DatabaseObject schemaObj = new DatabaseObject(schemaName, DatabaseObject.Type.SCHEMA);
                        TreeItem<DatabaseObject> schemaItem = new TreeItem<>(schemaObj);

                        // Create folders
                        TreeItem<DatabaseObject> tablesFolder = createFolderItem("Tables", DatabaseObject.Type.TABLES_FOLDER, schemaName);
                        TreeItem<DatabaseObject> viewsFolder = createFolderItem("Views", DatabaseObject.Type.VIEWS_FOLDER, schemaName);
                        TreeItem<DatabaseObject> procsFolder = createFolderItem("Procedures", DatabaseObject.Type.PROCEDURES_FOLDER, schemaName);
                        TreeItem<DatabaseObject> seqFolder = createFolderItem("Sequences", DatabaseObject.Type.SEQUENCES_FOLDER, schemaName);
                        TreeItem<DatabaseObject> funcFolder = createFolderItem("Functions", DatabaseObject.Type.FUNCTIONS_FOLDER, schemaName);

                        schemaItem.getChildren().addAll(tablesFolder, viewsFolder, procsFolder, seqFolder, funcFolder);
                        dbItem.getChildren().add(schemaItem);
                    }
                }
            }
        }
        // Keep existing logic for other database types
        else {
            // Add schemas folder
            DatabaseObject schemasObj = new DatabaseObject("Schemas", DatabaseObject.Type.SCHEMAS_FOLDER);
            TreeItem<DatabaseObject> schemasItem = new TreeItem<>(schemasObj);
            schemasItem.getChildren().add(new TreeItem<>(
                    new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER)));
            dbItem.getChildren().add(schemasItem);

            // Add security folder
            DatabaseObject securityObj = new DatabaseObject("Security", DatabaseObject.Type.SECURITY_FOLDER);
            TreeItem<DatabaseObject> securityItem = new TreeItem<>(securityObj);
            securityItem.getChildren().add(new TreeItem<>(
                    new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER)));
            dbItem.getChildren().add(securityItem);
        }
    }
    private static TreeItem<DatabaseObject> createFolderItem(String name, DatabaseObject.Type type, String schema) {
        TreeItem<DatabaseObject> folder = new TreeItem<>(new DatabaseObject(name, type, schema));
        folder.getChildren().add(new TreeItem<>(new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER)));
        return folder;
    }
    // Add this new method to load functions
    public static void loadFunctions(TreeItem<DatabaseObject> funcItem, ConnectionProfile profile) {
        String schema = funcItem.getParent().getValue().getName();

        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = ConnectionService.getConnection(profile)) {
                    DatabaseMetaData meta = conn.getMetaData();
                    try (ResultSet rs = meta.getFunctions(null, schema, "%")) {
                        List<TreeItem<DatabaseObject>> functions = new ArrayList<>();
                        while (rs.next()) {
                            String funcName = rs.getString("FUNCTION_NAME");
                            DatabaseObject funcObj = new DatabaseObject(funcName,
                                    DatabaseObject.Type.FUNCTION, schema);
                            functions.add(new TreeItem<>(funcObj));
                        }

                        Platform.runLater(() -> {
                            funcItem.getChildren().clear();
                            funcItem.getChildren().addAll(functions);
                            funcItem.getValue().setChildCount(functions.size());
                        });
                    }
                }
                return null;
            }
        };

        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                funcItem.getChildren().clear();
                funcItem.getChildren().add(new TreeItem<>(
                        new DatabaseObject("Load failed: " + e.getSource().getException().getMessage(),
                                DatabaseObject.Type.SYSTEM_FOLDER)));
            });
        });

        new Thread(loadTask).start();
    }


    private static void loadSchemaObjects(TreeItem<DatabaseObject> schemaItem, Connection conn,
                                          ConnectionProfile profile, String schemaName) throws SQLException {
        schemaItem.getChildren().clear();

        DatabaseObject tablesObj = new DatabaseObject("Tables", DatabaseObject.Type.TABLES_FOLDER, schemaName);
        TreeItem<DatabaseObject> tablesItem = new TreeItem<>(tablesObj);
        loadTables(tablesItem, profile);

        DatabaseObject viewsObj = new DatabaseObject("Views", DatabaseObject.Type.VIEWS_FOLDER, schemaName);
        TreeItem<DatabaseObject> viewsItem = new TreeItem<>(viewsObj);
        loadViews(viewsItem, profile);

        DatabaseObject procsObj = new DatabaseObject("Procedures", DatabaseObject.Type.PROCEDURES_FOLDER, schemaName);
        TreeItem<DatabaseObject> procsItem = new TreeItem<>(procsObj);
        loadProcedures(procsItem, profile);

        DatabaseObject seqObj = new DatabaseObject("Sequences", DatabaseObject.Type.SEQUENCES_FOLDER, schemaName);
        TreeItem<DatabaseObject> seqItem = new TreeItem<>(seqObj);
        loadSequences(seqItem, profile);

        DatabaseObject funcObj = new DatabaseObject("Functions", DatabaseObject.Type.FUNCTIONS_FOLDER, schemaName);
        TreeItem<DatabaseObject> funcItem = new TreeItem<>(funcObj);
        loadFunctions(funcItem, profile);

        schemaItem.getChildren().addAll(tablesItem, viewsItem, procsItem, seqItem, funcItem);
    }

public static void loadTables(TreeItem<DatabaseObject> tablesItem, ConnectionProfile profile) {
    String schema = tablesItem.getParent().getValue().getName();

    Task<Void> loadTask = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
            try (Connection conn = ConnectionService.getConnection(profile)) {
                // Handle case sensitivity for PostgreSQL
                String schemaPattern = profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL
                        ? schema
                        : schema.toUpperCase();

                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getTables(null, schemaPattern, "%", new String[]{"TABLE"})) {
                    List<TreeItem<DatabaseObject>> tables = new ArrayList<>();
                    while (rs.next()) {
                        String tableName = rs.getString("TABLE_NAME");
                        DatabaseObject tableObj = new DatabaseObject(tableName,
                                DatabaseObject.Type.TABLE, schema);
                        tables.add(new TreeItem<>(tableObj));
                    }

                    Platform.runLater(() -> {
                        tablesItem.getChildren().clear();
                        if (tables.isEmpty()) {
                            tablesItem.getChildren().add(new TreeItem<>(
                                    new DatabaseObject("No tables found", DatabaseObject.Type.SYSTEM_FOLDER)));
                        } else {
                            tablesItem.getChildren().addAll(tables);
                        }
                        tablesItem.getValue().setChildCount(tables.size());
                        tablesItem.getValue().setLoaded(true);
                    });
                }
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    tablesItem.getChildren().clear();
                    tablesItem.getChildren().add(new TreeItem<>(
                            new DatabaseObject("Error: " + e.getMessage(), DatabaseObject.Type.SYSTEM_FOLDER)));
                });
                throw e;
            }
            return null;
        }
    };

    loadTask.setOnFailed(e -> {
        Platform.runLater(() -> {
            tablesItem.getChildren().clear();
            tablesItem.getChildren().add(new TreeItem<>(
                    new DatabaseObject("Load failed: " + e.getSource().getException().getMessage(),
                            DatabaseObject.Type.SYSTEM_FOLDER)));
        });
    });

    new Thread(loadTask).start();
}
    public static List<DatabaseObject> getSchemas(ConnectionProfile profile) {
        List<DatabaseObject> schemas = new ArrayList<>();

        try (Connection conn = ConnectionService.getConnection(profile)) {
            DatabaseMetaData metaData = conn.getMetaData();

            // For MySQL
            if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL) {
                try (ResultSet rs = conn.createStatement().executeQuery("SHOW SCHEMAS")) {
                    while (rs.next()) {
                        schemas.add(new DatabaseObject(rs.getString(1), DatabaseObject.Type.SCHEMA));
                    }
                }
            }
            // For PostgreSQL
            else if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
                try (ResultSet rs = metaData.getSchemas()) {
                    while (rs.next()) {
                        schemas.add(new DatabaseObject(rs.getString("TABLE_SCHEM"), DatabaseObject.Type.SCHEMA));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return schemas;
    }

    public static List<DatabaseObject> getTables(ConnectionProfile profile, String schema) {
        List<DatabaseObject> tables = new ArrayList<>();

        try (Connection conn = ConnectionService.getConnection(profile)) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(new DatabaseObject(rs.getString("TABLE_NAME"),
                            DatabaseObject.Type.TABLE,
                            schema));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tables;
    }

    public static List<DatabaseObject> getViews(ConnectionProfile profile, String schema) {
        List<DatabaseObject> views = new ArrayList<>();

        try (Connection conn = ConnectionService.getConnection(profile)) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"VIEW"})) {
                while (rs.next()) {
                    views.add(new DatabaseObject(rs.getString("TABLE_NAME"),
                            DatabaseObject.Type.VIEW,
                            schema));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return views;
    }

    public static List<DatabaseObject> getProcedures(ConnectionProfile profile, String schema) {
        List<DatabaseObject> procedures = new ArrayList<>();

        try (Connection conn = ConnectionService.getConnection(profile)) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet rs = metaData.getProcedures(null, schema, "%")) {
                while (rs.next()) {
                    procedures.add(new DatabaseObject(rs.getString("PROCEDURE_NAME"),
                            DatabaseObject.Type.PROCEDURE,
                            schema));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return procedures;
    }

    public static List<String> getTableColumns(ConnectionProfile profile, String schema, String tableName) {
        List<String> columns = new ArrayList<>();

        try (Connection conn = ConnectionService.getConnection(profile)) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet rs = metaData.getColumns(null, schema, tableName, "%")) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columns;
    }

    public static void loadViews(TreeItem<DatabaseObject> viewsItem, ConnectionProfile profile) {
        String schema = viewsItem.getParent().getValue().getName();

        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = ConnectionService.getConnection(profile)) {
                    DatabaseMetaData meta = conn.getMetaData();
                    try (ResultSet rs = meta.getTables(null, schema, "%", new String[]{"VIEW"})) {
                        List<TreeItem<DatabaseObject>> views = new ArrayList<>();
                        while (rs.next()) {
                            String viewName = rs.getString("TABLE_NAME");
                            DatabaseObject viewObj = new DatabaseObject(viewName,
                                    DatabaseObject.Type.VIEW, schema);
                            views.add(new TreeItem<>(viewObj));
                        }

                        Platform.runLater(() -> {
                            viewsItem.getChildren().clear();
                            viewsItem.getChildren().addAll(views);
                            viewsItem.getValue().setChildCount(views.size());
                        });
                    }
                }
                return null;
            }
        };

        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                viewsItem.getChildren().clear();
                viewsItem.getChildren().add(new TreeItem<>(
                        new DatabaseObject("Load failed: " + e.getSource().getException().getMessage(),
                                DatabaseObject.Type.SYSTEM_FOLDER)));
            });
        });

        new Thread(loadTask).start();
    }
}
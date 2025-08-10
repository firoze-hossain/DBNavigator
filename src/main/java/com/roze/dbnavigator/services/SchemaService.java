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
    DatabaseObject serverObj = new DatabaseObject(profile.getName(), DatabaseObject.Type.SERVER);
    TreeItem<DatabaseObject> rootItem = new TreeItem<>(serverObj);

    try (Connection conn = ConnectionService.getConnection(profile)) {
        if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
            // For PostgreSQL, we already know the database name
            DatabaseObject dbObj = new DatabaseObject(profile.getDatabase(), DatabaseObject.Type.DATABASE);
            TreeItem<DatabaseObject> dbItem = new TreeItem<>(dbObj);
            rootItem.getChildren().add(dbItem);

            // Load schemas immediately for PostgreSQL
            loadDatabaseObjects(dbItem, conn, profile);
        } else {
            // For MySQL, add loading placeholder
            TreeItem<DatabaseObject> loadingItem = new TreeItem<>(
                    new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER));
            rootItem.getChildren().add(loadingItem);
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
                // Get the schema name from the parent object
                String schemaName = parentObj.getName();
                loadSchemaObjects(parentItem, conn, profile, schemaName);
            } else if (parentObj.getType() == DatabaseObject.Type.TABLES_FOLDER) {
                loadTables(parentItem, profile);
            }
            // Add other cases as needed...
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
        // Load schemas for PostgreSQL
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getSchemas()) {
            while (rs.next()) {
                String schemaName = rs.getString("TABLE_SCHEM");
                // Skip system schemas
                if (!schemaName.startsWith("pg_") && !schemaName.equals("information_schema")) {
                    DatabaseObject schemaObj = new DatabaseObject(schemaName, DatabaseObject.Type.SCHEMA);
                    TreeItem<DatabaseObject> schemaItem = new TreeItem<>(schemaObj);

                    // Add folders for tables, views, etc.
                    TreeItem<DatabaseObject> tablesFolder = new TreeItem<>(
                            new DatabaseObject("Tables", DatabaseObject.Type.TABLES_FOLDER, schemaName));
                    TreeItem<DatabaseObject> viewsFolder = new TreeItem<>(
                            new DatabaseObject("Views", DatabaseObject.Type.VIEWS_FOLDER, schemaName));
                    TreeItem<DatabaseObject> procsFolder = new TreeItem<>(
                            new DatabaseObject("Procedures", DatabaseObject.Type.PROCEDURES_FOLDER, schemaName));

                    // Add loading placeholders
                    tablesFolder.getChildren().add(new TreeItem<>(
                            new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER)));
                    viewsFolder.getChildren().add(new TreeItem<>(
                            new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER)));
                    procsFolder.getChildren().add(new TreeItem<>(
                            new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER)));

                    schemaItem.getChildren().addAll(tablesFolder, viewsFolder, procsFolder);
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
//    private static void loadSchemaObjects(TreeItem<DatabaseObject> schemaItem, Connection conn, ConnectionProfile profile)
//            throws SQLException {
//
//        schemaItem.getChildren().clear();
//
//        // Add tables folder
//        DatabaseObject tablesObj = new DatabaseObject("Tables", DatabaseObject.Type.TABLES_FOLDER);
//        TreeItem<DatabaseObject> tablesItem = new TreeItem<>(tablesObj);
//        tablesItem.getChildren().add(new TreeItem<>(
//                new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER)));
//        schemaItem.getChildren().add(tablesItem);
//
//        // Add views folder
//        DatabaseObject viewsObj = new DatabaseObject("Views", DatabaseObject.Type.VIEWS_FOLDER);
//        TreeItem<DatabaseObject> viewsItem = new TreeItem<>(viewsObj);
//        viewsItem.getChildren().add(new TreeItem<>(
//                new DatabaseObject(LOADING_NODE, DatabaseObject.Type.SYSTEM_FOLDER)));
//        schemaItem.getChildren().add(viewsItem);
//
//        // Add other folders as needed...
//    }
private static void loadSchemaObjects(TreeItem<DatabaseObject> schemaItem, Connection conn,
                                      ConnectionProfile profile, String schemaName) throws SQLException {
    schemaItem.getChildren().clear();

    // Tables folder with actual tables
    DatabaseObject tablesObj = new DatabaseObject("Tables", DatabaseObject.Type.TABLES_FOLDER, schemaName);
    TreeItem<DatabaseObject> tablesItem = new TreeItem<>(tablesObj);
    loadTables(tablesItem, profile); // Load tables immediately

    // Views folder
    DatabaseObject viewsObj = new DatabaseObject("Views", DatabaseObject.Type.VIEWS_FOLDER, schemaName);
    TreeItem<DatabaseObject> viewsItem = new TreeItem<>(viewsObj);
    loadViews(viewsItem, profile); // Load views immediately

    // Procedures folder
    DatabaseObject procsObj = new DatabaseObject("Procedures", DatabaseObject.Type.PROCEDURES_FOLDER, schemaName);
    TreeItem<DatabaseObject> procsItem = new TreeItem<>(procsObj);
    loadProcedures(procsItem, profile); // Load procedures immediately

    schemaItem.getChildren().addAll(tablesItem, viewsItem, procsItem);
}

public static void loadTables(TreeItem<DatabaseObject> tablesItem,  ConnectionProfile profile) {
    String schema = tablesItem.getParent().getValue().getName(); // Get schema name from parent

    Task<Void> loadTask = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
            try (Connection conn = ConnectionService.getConnection(profile)) {
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getTables(null, schema, "%", new String[]{"TABLE"})) {
                    List<TreeItem<DatabaseObject>> tables = new ArrayList<>();
                    while (rs.next()) {
                        String tableName = rs.getString("TABLE_NAME");
                        DatabaseObject tableObj = new DatabaseObject(tableName,
                                DatabaseObject.Type.TABLE, schema);
                        tables.add(new TreeItem<>(tableObj));
                    }

                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        tablesItem.getChildren().clear();
                        tablesItem.getChildren().addAll(tables);
                        tablesItem.getValue().setChildCount(tables.size());
                    });
                }
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
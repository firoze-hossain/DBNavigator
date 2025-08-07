package com.roze.dbnavigator.services;

import com.roze.dbnavigator.models.ConnectionProfile;
import com.roze.dbnavigator.models.DatabaseObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchemaService {
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
}
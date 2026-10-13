package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DdlGeneratorServiceTest {

    private ConnectionProfile createPostgreSqlProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("pg-1");
        p.setName("postgres@localhost");
        p.setType(ConnectionProfile.DatabaseType.POSTGRESQL);
        p.setHost("localhost");
        p.setPort(5432);
        p.setDatabase("thunder_db");
        p.setUsername("postgres");
        return p;
    }

    @Test
    public void testGenerateSchemaDdl() throws Exception {
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject schemaObj = new DbObject("public", Kind.SCHEMA, "thunder_db", "public");

        String ddl = DdlGeneratorService.generateDdl(profile, schemaObj);

        assertNotNull(ddl);
        assertTrue(ddl.contains("-- auto-generated definition"), "Should contain DataGrip auto-generated header");
        assertTrue(ddl.contains("create schema public;"), "Should contain create schema statement");
        assertTrue(ddl.contains("alter schema public owner to postgres;"), "Should set schema owner");
    }

    @Test
    public void testGenerateDatabaseDdl() throws Exception {
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject dbObj = new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null);

        String ddl = DdlGeneratorService.generateDdl(profile, dbObj);

        assertNotNull(ddl);
        assertTrue(ddl.contains("-- auto-generated definition"), "Should contain DataGrip auto-generated header");
        assertTrue(ddl.contains("create database thunder_db"), "Should contain create database statement");
    }

    @Test
    public void testGenerateNullHandling() throws Exception {
        String ddl = DdlGeneratorService.generateDdl(null, null);
        assertTrue(ddl.contains("-- No object selected"));
    }
}

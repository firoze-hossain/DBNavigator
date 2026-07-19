module com.roze.dbnavigator {
    // JavaFX
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.swing;

    // JDBC API
    requires java.sql;

    // SQL editor (RichTextFX + its deps)
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires reactfx;

    // Icons
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    // Connection pooling
    requires com.zaxxer.hikari;

    // JSON persistence
    requires com.fasterxml.jackson.databind;

    // MongoDB driver
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;

    // JDBC drivers — required explicitly so they are resolved onto the
    // module graph and their java.sql.Driver services are found
    requires mysql.connector.j;
    requires org.mariadb.jdbc;
    requires org.postgresql.jdbc;
    requires org.xerial.sqlitejdbc;
    requires com.microsoft.sqlserver.jdbc;
    requires com.oracle.database.jdbc;
    requires java.desktop;

    // JavaFX launches Main reflectively
    opens com.roze.dbnavigator to javafx.graphics;

    // Jackson reads/writes ConnectionProfile reflectively
    opens com.roze.dbnavigator.model to com.fasterxml.jackson.databind;

    exports com.roze.dbnavigator;
    exports com.roze.dbnavigator.model;
    exports com.roze.dbnavigator.ui;
    exports com.roze.dbnavigator.db;
    exports com.roze.dbnavigator.util;
}

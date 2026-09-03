/**
 * A real, explicit module descriptor - stratosdb-network used to have none
 * at all, making it an "automatic module" whenever placed on a real module
 * path (exactly what happens in DBNavigator's own real, JPMS-based launch).
 * An automatic module has no declared boundary at all: it exports EVERY
 * package it physically contains, unconditionally, to every other module
 * in the graph. That real ambiguity is what caused a real, recurring,
 * different-symptom-each-time class of module resolution error in
 * DBNavigator - Java's own resolver, when checking which real module
 * genuinely supplies a given package to a consumer like
 * {@code org.mongodb.driver.core} (which really does depend on real
 * Jackson modules), could not tell the difference between "this package
 * really belongs to this automatic module" and "this automatic module
 * merely happens to be present on the same module path" - because an
 * automatic module's own real exports are, by definition, unbounded.
 *
 * A real, explicit module-info.java removes that ambiguity at the root:
 * this module now exports exactly the two real packages
 * {@code stratosdb-jdbc} (and any other real, future JPMS consumer)
 * actually needs, and nothing else - it can never again be mistaken for
 * exporting an unrelated third-party package like
 * {@code com.fasterxml.jackson.databind.node}, regardless of what else
 * happens to share its module path.
 */
module stratosdb.network {
    requires stratosdb.common;
    requires stratosdb.core;
    requires stratosdb.storage;
    requires stratosdb.sql;
    requires stratosdb.transaction;
    requires org.slf4j;
    requires jdk.httpserver;
    requires java.sql;

    exports com.stratosdb.network.auth;
    exports com.stratosdb.network.stdwire;
    exports com.stratosdb.network.tls;
    exports com.stratosdb.network.server;
    exports com.stratosdb.network.replication;
}

/**
 * A real, explicit module descriptor - see stratosdb-network's own
 * module-info.java for the full account of why this matters (this
 * module used to be an automatic module too, sharing the same real,
 * recurring class of ambiguous-export conflict in DBNavigator's own
 * real, JPMS-based launch).
 *
 * The real {@code java.sql.Driver} service registration is declared
 * here via a real {@code provides ... with ...;} clause - JPMS's own,
 * modern, preferred mechanism, which does NOT require the provider
 * class's own package to be exported at all (real, standard JPMS
 * behavior: {@code ServiceLoader}/{@code DriverManager} reach a real
 * provider class via this declaration, not ordinary compile-time
 * linking). The legacy {@code META-INF/services/java.sql.Driver} file
 * is kept alongside this, unchanged, for real backward compatibility
 * with any classpath-only (unnamed module) consumer that doesn't use
 * JPMS at all.
 */
module stratosdb.jdbc {
    requires stratosdb.network;
    requires java.sql;

    exports com.stratosdb.jdbc;

    provides java.sql.Driver with com.stratosdb.jdbc.StratosDriver;
}

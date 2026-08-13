package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Administrative operations that act on whole databases rather than tables. */
public final class DatabaseAdminService {

    private DatabaseAdminService() {}

    /**
     * Drops a database from the server. For PostgreSQL, this closes the
     * app's own pooled connection to that database first, best-effort
     * terminates other sessions connected to it, and runs DROP DATABASE from
     * a different maintenance database — PostgreSQL refuses to drop a
     * database that has any active connection, including its own.
     */
    public static void dropDatabase(ConnectionProfile profile, String databaseName) throws Exception {
        if (profile.getType() == DatabaseType.MONGODB) {
            throw new UnsupportedOperationException(
                    "Dropping MongoDB databases isn't supported yet — use a console command.");
        }
        switch (profile.getType()) {
            case POSTGRESQL -> dropPostgresDatabase(profile, databaseName);
            case SQLSERVER -> dropSqlServerDatabase(profile, databaseName);
            default -> dropGenericDatabase(profile, databaseName);
        }
    }

    private static void dropPostgresDatabase(ConnectionProfile profile, String databaseName) throws Exception {
        ClientRegistry.disconnectCatalog(profile, databaseName);

        // Run the DROP from a database other than the target — Postgres won't
        // drop the database a connection is currently using.
        String adminCatalog = databaseName.equals(profile.getDatabase())
                ? "postgres" : profile.getDatabase();

        try (Connection conn = ClientRegistry.jdbc(profile, adminCatalog).getConnection();
             Statement stmt = conn.createStatement()) {

            // Best-effort: kick out any other sessions (other tools, other users)
            // still connected to the target database.
            try {
                stmt.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity "
                        + "WHERE datname = '" + databaseName.replace("'", "''")
                        + "' AND pid <> pg_backend_pid()");
            } catch (SQLException ignored) {
                // insufficient privilege or old server version — proceed anyway
            }

            String quoted = quotePostgres(databaseName);
            try {
                stmt.execute("DROP DATABASE " + quoted + " WITH (FORCE)");
            } catch (SQLException forceUnsupported) {
                // WITH (FORCE) needs PostgreSQL 13+ — retry the plain form
                stmt.execute("DROP DATABASE " + quoted);
            }
        }
    }

    private static void dropGenericDatabase(ConnectionProfile profile, String databaseName) throws Exception {
        ClientRegistry.disconnectCatalog(profile, databaseName);
        try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE " + quoteBacktick(databaseName));
        }
    }

    /**
     * SQL Server, like PostgreSQL, refuses to drop a database with any
     * active connection — including the one issuing the DROP if it's
     * currently using that database. SET SINGLE_USER WITH ROLLBACK
     * IMMEDIATE forces every other session off first (SQL Server's
     * equivalent of PostgreSQL's pg_terminate_backend loop above), and the
     * DROP itself runs from "master" rather than the target database.
     */
    private static void dropSqlServerDatabase(ConnectionProfile profile, String databaseName) throws Exception {
        ClientRegistry.disconnectCatalog(profile, databaseName);
        String adminCatalog = databaseName.equalsIgnoreCase(profile.getDatabase())
                ? "master" : profile.getDatabase();
        try (Connection conn = ClientRegistry.jdbc(profile, adminCatalog).getConnection();
             Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("ALTER DATABASE " + bracket(databaseName)
                        + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
            } catch (SQLException ignored) {
                // insufficient privilege, or already single-user — proceed anyway
            }
            stmt.execute("DROP DATABASE " + bracket(databaseName));
        }
    }

    /**
     * Renames a database. PostgreSQL supports this directly with
     * ALTER DATABASE ... RENAME TO ...; SQL Server has its own equivalent,
     * ALTER DATABASE ... MODIFY NAME = ... (also requires exclusive access,
     * same as dropping). MySQL/MariaDB have no equivalent statement (the
     * common workaround is dump + recreate + restore), so that's reported
     * as unsupported rather than attempted.
     */
    public static void renameDatabase(ConnectionProfile profile, String oldName, String newName) throws Exception {
        if (profile.getType() == DatabaseType.SQLSERVER) {
            ClientRegistry.disconnectCatalog(profile, oldName);
            String adminCatalog = oldName.equalsIgnoreCase(profile.getDatabase()) ? "master" : profile.getDatabase();
            try (Connection conn = ClientRegistry.jdbc(profile, adminCatalog).getConnection();
                 Statement stmt = conn.createStatement()) {
                try {
                    stmt.execute("ALTER DATABASE " + bracket(oldName) + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                } catch (SQLException ignored) {
                    // insufficient privilege, or already single-user — proceed anyway
                }
                stmt.execute("ALTER DATABASE " + bracket(oldName) + " MODIFY NAME = " + bracket(newName));
            }
            if (oldName.equals(profile.getDatabase())) profile.setDatabase(newName);
            return;
        }
        if (profile.getType() != DatabaseType.POSTGRESQL) {
            throw new UnsupportedOperationException(
                    profile.getType().getDisplayName() + " has no direct \"rename database\" statement — "
                    + "the usual workaround is dump, create a new database, and restore into it.");
        }
        ClientRegistry.disconnectCatalog(profile, oldName);
        String adminCatalog = oldName.equals(profile.getDatabase()) ? "postgres" : profile.getDatabase();
        try (Connection conn = ClientRegistry.jdbc(profile, adminCatalog).getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER DATABASE " + quotePostgres(oldName) + " RENAME TO " + quotePostgres(newName));
        }
        if (oldName.equals(profile.getDatabase())) {
            profile.setDatabase(newName);
        }
    }

    /** Changes a PostgreSQL database's owner role. */
    public static void changeOwner(ConnectionProfile profile, String database, String newOwner) throws Exception {
        if (profile.getType() != DatabaseType.POSTGRESQL) {
            throw new UnsupportedOperationException(
                    "Changing a database's owner isn't supported for " + profile.getType().getDisplayName() + " here.");
        }
        try (Connection conn = ClientRegistry.jdbc(profile, database).getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER DATABASE " + quotePostgres(database) + " OWNER TO " + quotePostgres(newOwner));
        }
    }

    /** Current owner role of a PostgreSQL database, or null if unavailable/not Postgres. */
    public static String currentOwner(ConnectionProfile profile, String database) {
        if (profile.getType() != DatabaseType.POSTGRESQL) return null;
        String sql = "SELECT pg_catalog.pg_get_userbyid(datdba) FROM pg_database WHERE datname = ?";
        try (Connection conn = ClientRegistry.jdbc(profile, database).getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, database);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            return null;
        }
    }

    private static String quotePostgres(String name) {
        if (name.matches("[A-Za-z_][A-Za-z0-9_]*")) return name;
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    private static String quoteBacktick(String name) {
        return "`" + name.replace("`", "``") + "`";
    }

    /** SQL Server's bracket-quoted identifier form — [name], with ] doubled if present. */
    private static String bracket(String name) {
        return "[" + name.replace("]", "]]") + "]";
    }
}

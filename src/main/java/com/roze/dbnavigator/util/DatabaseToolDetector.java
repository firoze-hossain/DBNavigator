package com.roze.dbnavigator.util;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatically detects executable paths for database client tools
 * across macOS, Windows, and Linux — mirroring DataGrip's executable discovery:
 *
 * <ul>
 *   <li><b>PostgreSQL:</b> pg_dump, pg_restore, psql, pg_dumpall</li>
 *   <li><b>MySQL:</b> mysqldump, mysql</li>
 *   <li><b>MariaDB:</b> mariadb-dump, mariadb, mysqldump, mysql</li>
 *   <li><b>SQLite:</b> sqlite3</li>
 *   <li><b>MongoDB:</b> mongodump, mongorestore, mongosh, mongo</li>
 *   <li><b>SQL Server:</b> bcp, sqlcmd</li>
 *   <li><b>Oracle:</b> expdp, impdp, sqlplus</li>
 * </ul>
 *
 * <p>Lookup order:</p>
 * <ol>
 *   <li>Saved path from user settings (if valid and still existing on disk)</li>
 *   <li>Operating-system specific standard installation paths (sorted by newest/matching version)</li>
 *   <li>Package managers (Homebrew, MacPorts, Chocolatey, Scoop, Debian/RHEL packages)</li>
 *   <li>System PATH and common terminal directories (critical on macOS GUI apps which lack full shell PATH)</li>
 *   <li>Native {@code which} / {@code where} command verification</li>
 *   <li>Fallback to bare executable name</li>
 * </ol>
 */
public final class DatabaseToolDetector {

    public enum OS {
        WINDOWS, MAC, LINUX, OTHER;

        public static OS current() {
            String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (name.contains("win")) return WINDOWS;
            if (name.contains("mac") || name.contains("darwin")) return MAC;
            if (name.contains("nux") || name.contains("nix") || name.contains("aix")) return LINUX;
            return OTHER;
        }
    }

    private static final Pattern NUMERIC_VERSION_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private DatabaseToolDetector() {}

    /**
     * Resolves the executable path for the given tool, utilizing the connection profile
     * to prioritize matching database versions if available.
     *
     * @param toolName name of the tool, e.g. "pg_dump", "psql", "pg_restore", "mysql", "mysqldump", etc.
     * @param profile  optional connection profile for version hints and database type
     * @return absolute path to executable if found, or canonical tool name as fallback
     */
    public static String detectToolPath(String toolName, ConnectionProfile profile) {
        if (toolName == null || toolName.isBlank()) {
            return "";
        }

        String canonicalTool = canonicalToolName(toolName);
        OS os = OS.current();

        // 1. Check user-saved preferences first
        try {
            var settings = AppSettingsStore.load();
            if (settings != null && settings.getDatabaseClientToolPaths() != null) {
                String saved = settings.getDatabaseClientToolPaths().get(canonicalTool);
                if (saved != null && !saved.isBlank() && isValidExecutable(new File(saved))) {
                    return saved;
                }
            }
        } catch (Exception ignored) {}

        // Version hint extracted from profile name / connection URL if any (e.g. "16", "15", "8.0")
        String versionHint = extractVersionHint(profile);

        // 2. Search OS-specific standard directories
        String detected = switch (os) {
            case MAC -> detectOnMac(canonicalTool, versionHint, profile);
            case WINDOWS -> detectOnWindows(canonicalTool, versionHint, profile);
            case LINUX -> detectOnLinux(canonicalTool, versionHint, profile);
            default -> null;
        };

        if (detected != null && isValidExecutable(new File(detected))) {
            return detected;
        }

        // 3. Search system PATH and extended common directories
        detected = searchInPath(canonicalTool, os);
        if (detected != null && isValidExecutable(new File(detected))) {
            return detected;
        }

        // 4. Try native which / where
        detected = lookupWithWhichOrWhere(canonicalTool, os);
        if (detected != null && isValidExecutable(new File(detected))) {
            return detected;
        }

        // 5. Fallback: bare tool name (with .exe on Windows)
        return (os == OS.WINDOWS && !canonicalTool.toLowerCase(Locale.ROOT).endsWith(".exe"))
                ? canonicalTool + ".exe"
                : canonicalTool;
    }

    /**
     * Convenience method to detect a tool path without a profile.
     */
    public static String detectToolPath(String toolName) {
        return detectToolPath(toolName, null);
    }

    /**
     * Checks if a tool can be detected on disk as an executable file.
     */
    public static boolean hasTool(String toolName) {
        String path = detectToolPath(toolName, null);
        if (path == null || path.isBlank()) return false;
        File f = new File(path);
        return f.isAbsolute() && isValidExecutable(f);
    }

    /**
     * Persists the user-specified executable path for a client tool.
     */
    public static void saveToolPath(String toolName, String path) {
        if (toolName == null || toolName.isBlank() || path == null || path.isBlank()) return;
        String canonical = canonicalToolName(toolName);
        try {
            var settings = AppSettingsStore.load();
            if (settings != null) {
                settings.getDatabaseClientToolPaths().put(canonical, path.trim());
                AppSettingsStore.save(settings);
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // macOS Discovery
    // =========================================================================

    private static String detectOnMac(String tool, String versionHint, ConnectionProfile profile) {
        String binaryName = tool;

        // PostgreSQL tools: pg_dump, pg_restore, psql, pg_dumpall
        if (isPostgreSqlTool(tool)) {
            // A. /Library/PostgreSQL/<version>/bin/<tool> (Official EnterpriseDB installer - DataGrip default)
            File pgDir = new File("/Library/PostgreSQL");
            String found = scanVersionDirs(pgDir, "bin", binaryName, versionHint);
            if (found != null) return found;

            // B. Postgres.app: /Applications/Postgres.app/Contents/Versions/latest/bin/<tool>
            File appLatest = new File("/Applications/Postgres.app/Contents/Versions/latest/bin/" + binaryName);
            if (isValidExecutable(appLatest)) return appLatest.getAbsolutePath();

            File appVersions = new File("/Applications/Postgres.app/Contents/Versions");
            found = scanVersionDirs(appVersions, "bin", binaryName, versionHint);
            if (found != null) return found;

            // C. Homebrew Apple Silicon (/opt/homebrew)
            found = scanBrewOptDirs(new File("/opt/homebrew/opt"), "postgresql", binaryName, versionHint);
            if (found != null) return found;

            File brewLibpq = new File("/opt/homebrew/opt/libpq/bin/" + binaryName);
            if (isValidExecutable(brewLibpq)) return brewLibpq.getAbsolutePath();

            File brewBin = new File("/opt/homebrew/bin/" + binaryName);
            if (isValidExecutable(brewBin)) return brewBin.getAbsolutePath();

            // D. Homebrew Intel (/usr/local)
            found = scanBrewOptDirs(new File("/usr/local/opt"), "postgresql", binaryName, versionHint);
            if (found != null) return found;

            File intelLibpq = new File("/usr/local/opt/libpq/bin/" + binaryName);
            if (isValidExecutable(intelLibpq)) return intelLibpq.getAbsolutePath();

            File intelBin = new File("/usr/local/bin/" + binaryName);
            if (isValidExecutable(intelBin)) return intelBin.getAbsolutePath();

            // E. MacPorts (/opt/local/lib/postgresql<ver>/bin)
            File macports = new File("/opt/local/lib");
            found = scanPrefixDirs(macports, "postgresql", "bin", binaryName, versionHint);
            if (found != null) return found;

            File macportsBin = new File("/opt/local/bin/" + binaryName);
            if (isValidExecutable(macportsBin)) return macportsBin.getAbsolutePath();

            // F. Nix
            File nix = new File("/nix/var/nix/profiles/default/bin/" + binaryName);
            if (isValidExecutable(nix)) return nix.getAbsolutePath();

            // G. System /usr/bin
            File usrBin = new File("/usr/bin/" + binaryName);
            if (isValidExecutable(usrBin)) return usrBin.getAbsolutePath();
        }

        // MySQL & MariaDB tools: mysqldump, mysql, mariadb-dump, mariadb
        if (isMySqlOrMariaDbTool(tool)) {
            // A. Official MySQL DMG installer: /usr/local/mysql/bin/<tool>
            File mysqlDir = new File("/usr/local/mysql/bin/" + binaryName);
            if (isValidExecutable(mysqlDir)) return mysqlDir.getAbsolutePath();

            // Scan /usr/local/mysql-<ver>/bin/<tool>
            String found = scanPrefixDirs(new File("/usr/local"), "mysql-", "bin", binaryName, versionHint);
            if (found != null) return found;

            // B. Homebrew
            found = scanBrewOptDirs(new File("/opt/homebrew/opt"), "mysql", binaryName, versionHint);
            if (found != null) return found;
            found = scanBrewOptDirs(new File("/opt/homebrew/opt"), "mariadb", binaryName, versionHint);
            if (found != null) return found;

            File brewBin = new File("/opt/homebrew/bin/" + binaryName);
            if (isValidExecutable(brewBin)) return brewBin.getAbsolutePath();

            found = scanBrewOptDirs(new File("/usr/local/opt"), "mysql", binaryName, versionHint);
            if (found != null) return found;
            found = scanBrewOptDirs(new File("/usr/local/opt"), "mariadb", binaryName, versionHint);
            if (found != null) return found;

            File intelBin = new File("/usr/local/bin/" + binaryName);
            if (isValidExecutable(intelBin)) return intelBin.getAbsolutePath();

            // C. MAMP & XAMPP
            File mamp = new File("/Applications/MAMP/Library/bin/" + binaryName);
            if (isValidExecutable(mamp)) return mamp.getAbsolutePath();

            File xampp = new File("/Applications/XAMPP/xamppfiles/bin/" + binaryName);
            if (isValidExecutable(xampp)) return xampp.getAbsolutePath();
        }

        // SQLite: sqlite3
        if ("sqlite3".equalsIgnoreCase(tool)) {
            File usrBin = new File("/usr/bin/sqlite3");
            if (isValidExecutable(usrBin)) return usrBin.getAbsolutePath();

            File brewBin = new File("/opt/homebrew/bin/sqlite3");
            if (isValidExecutable(brewBin)) return brewBin.getAbsolutePath();

            File intelBin = new File("/usr/local/bin/sqlite3");
            if (isValidExecutable(intelBin)) return intelBin.getAbsolutePath();
        }

        // MongoDB tools: mongodump, mongorestore, mongosh, mongo
        if (isMongoDbTool(tool)) {
            File brewTools = new File("/opt/homebrew/opt/mongodb-database-tools/bin/" + binaryName);
            if (isValidExecutable(brewTools)) return brewTools.getAbsolutePath();

            File intelTools = new File("/usr/local/opt/mongodb-database-tools/bin/" + binaryName);
            if (isValidExecutable(intelTools)) return intelTools.getAbsolutePath();

            String found = scanBrewOptDirs(new File("/opt/homebrew/opt"), "mongodb-community", binaryName, versionHint);
            if (found != null) return found;

            File brewBin = new File("/opt/homebrew/bin/" + binaryName);
            if (isValidExecutable(brewBin)) return brewBin.getAbsolutePath();

            File intelBin = new File("/usr/local/bin/" + binaryName);
            if (isValidExecutable(intelBin)) return intelBin.getAbsolutePath();
        }

        // SQL Server tools: bcp, sqlcmd
        if (isSqlServerTool(tool)) {
            File brewBin = new File("/opt/homebrew/bin/" + binaryName);
            if (isValidExecutable(brewBin)) return brewBin.getAbsolutePath();

            File intelBin = new File("/usr/local/bin/" + binaryName);
            if (isValidExecutable(intelBin)) return intelBin.getAbsolutePath();

            File mssql18 = new File("/opt/mssql-tools18/bin/" + binaryName);
            if (isValidExecutable(mssql18)) return mssql18.getAbsolutePath();

            File mssql = new File("/opt/mssql-tools/bin/" + binaryName);
            if (isValidExecutable(mssql)) return mssql.getAbsolutePath();
        }

        // Oracle tools: expdp, impdp, sqlplus
        if (isOracleTool(tool)) {
            String oracleHome = System.getenv("ORACLE_HOME");
            if (oracleHome != null && !oracleHome.isBlank()) {
                File f = new File(oracleHome, "bin/" + binaryName);
                if (isValidExecutable(f)) return f.getAbsolutePath();
            }
            String found = scanPrefixDirs(new File("/opt/oracle"), "instantclient", "", binaryName, versionHint);
            if (found != null) return found;

            found = scanPrefixDirs(new File("/usr/lib/oracle"), "", "client64/bin", binaryName, versionHint);
            if (found != null) return found;
        }

        return null;
    }

    // =========================================================================
    // Windows Discovery
    // =========================================================================

    private static String detectOnWindows(String tool, String versionHint, ConnectionProfile profile) {
        String exeName = tool.toLowerCase(Locale.ROOT).endsWith(".exe") ? tool : tool + ".exe";

        List<File> rootDrives = new ArrayList<>();
        File[] roots = File.listRoots();
        if (roots != null) rootDrives.addAll(Arrays.asList(roots));
        if (rootDrives.isEmpty()) rootDrives.add(new File("C:\\"));

        // PostgreSQL tools
        if (isPostgreSqlTool(tool)) {
            for (File drive : rootDrives) {
                File pf = new File(drive, "Program Files\\PostgreSQL");
                String found = scanVersionDirs(pf, "bin", exeName, versionHint);
                if (found != null) return found;

                File pf86 = new File(drive, "Program Files (x86)\\PostgreSQL");
                found = scanVersionDirs(pf86, "bin", exeName, versionHint);
                if (found != null) return found;
            }

            for (String envVar : List.of("POSTGRESQL_HOME", "PG_HOME", "PGBIN")) {
                String val = System.getenv(envVar);
                if (val != null && !val.isBlank()) {
                    File f = new File(val.endsWith("bin") ? val : val + "\\bin", exeName);
                    if (isValidExecutable(f)) return f.getAbsolutePath();
                }
            }

            String localApp = System.getenv("LOCALAPPDATA");
            if (localApp != null) {
                File laPg = new File(localApp, "Programs\\PostgreSQL");
                String found = scanVersionDirs(laPg, "bin", exeName, versionHint);
                if (found != null) return found;
            }

            File choco = new File("C:\\ProgramData\\chocolatey\\bin\\" + exeName);
            if (isValidExecutable(choco)) return choco.getAbsolutePath();

            File chocoTools = new File("C:\\tools\\postgresql\\bin\\" + exeName);
            if (isValidExecutable(chocoTools)) return chocoTools.getAbsolutePath();

            String userHome = System.getProperty("user.home", "");
            File scoop = new File(userHome, "scoop\\apps\\postgresql\\current\\bin\\" + exeName);
            if (isValidExecutable(scoop)) return scoop.getAbsolutePath();
        }

        // MySQL & MariaDB tools
        if (isMySqlOrMariaDbTool(tool)) {
            for (File drive : rootDrives) {
                // Program Files\MySQL\MySQL Server <ver>\bin
                File mysqlDir = new File(drive, "Program Files\\MySQL");
                String found = scanPrefixDirs(mysqlDir, "MySQL Server", "bin", exeName, versionHint);
                if (found != null) return found;

                found = scanPrefixDirs(mysqlDir, "MySQL Shell", "bin", exeName, versionHint);
                if (found != null) return found;

                File mysqlDir86 = new File(drive, "Program Files (x86)\\MySQL");
                found = scanPrefixDirs(mysqlDir86, "MySQL Server", "bin", exeName, versionHint);
                if (found != null) return found;

                // MariaDB
                File mariaDir = new File(drive, "Program Files\\MariaDB");
                found = scanPrefixDirs(mariaDir, "", "bin", exeName, versionHint);
                if (found != null) return found;

                File mariaDir86 = new File(drive, "Program Files (x86)\\MariaDB");
                found = scanPrefixDirs(mariaDir86, "", "bin", exeName, versionHint);
                if (found != null) return found;

                // Stacks: XAMPP, WAMP, Laragon
                File xampp = new File(drive, "xampp\\mysql\\bin\\" + exeName);
                if (isValidExecutable(xampp)) return xampp.getAbsolutePath();

                found = scanPrefixDirs(new File(drive, "wamp64\\bin\\mysql"), "mysql", "bin", exeName, versionHint);
                if (found != null) return found;

                found = scanPrefixDirs(new File(drive, "wamp\\bin\\mysql"), "mysql", "bin", exeName, versionHint);
                if (found != null) return found;

                found = scanPrefixDirs(new File(drive, "laragon\\bin\\mysql"), "mysql", "bin", exeName, versionHint);
                if (found != null) return found;

                found = scanPrefixDirs(new File(drive, "laragon\\bin\\mariadb"), "mariadb", "bin", exeName, versionHint);
                if (found != null) return found;
            }

            String mysqlHome = System.getenv("MYSQL_HOME");
            if (mysqlHome != null && !mysqlHome.isBlank()) {
                File f = new File(mysqlHome.endsWith("bin") ? mysqlHome : mysqlHome + "\\bin", exeName);
                if (isValidExecutable(f)) return f.getAbsolutePath();
            }

            String userHome = System.getProperty("user.home", "");
            File scoop = new File(userHome, "scoop\\apps\\mysql\\current\\bin\\" + exeName);
            if (isValidExecutable(scoop)) return scoop.getAbsolutePath();
            File scoopMaria = new File(userHome, "scoop\\apps\\mariadb\\current\\bin\\" + exeName);
            if (isValidExecutable(scoopMaria)) return scoopMaria.getAbsolutePath();

            File choco = new File("C:\\ProgramData\\chocolatey\\bin\\" + exeName);
            if (isValidExecutable(choco)) return choco.getAbsolutePath();
        }

        // SQLite
        if ("sqlite3".equalsIgnoreCase(tool)) {
            for (String p : List.of("C:\\sqlite\\sqlite3.exe", "C:\\Program Files\\SQLite\\sqlite3.exe", "C:\\tools\\sqlite\\sqlite3.exe")) {
                File f = new File(p);
                if (isValidExecutable(f)) return f.getAbsolutePath();
            }
            String userHome = System.getProperty("user.home", "");
            File scoop = new File(userHome, "scoop\\apps\\sqlite\\current\\sqlite3.exe");
            if (isValidExecutable(scoop)) return scoop.getAbsolutePath();
            File choco = new File("C:\\ProgramData\\chocolatey\\bin\\sqlite3.exe");
            if (isValidExecutable(choco)) return choco.getAbsolutePath();
        }

        // MongoDB
        if (isMongoDbTool(tool)) {
            for (File drive : rootDrives) {
                File toolsDir = new File(drive, "Program Files\\MongoDB\\Tools");
                String found = scanVersionDirs(toolsDir, "bin", exeName, versionHint);
                if (found != null) return found;

                File serverDir = new File(drive, "Program Files\\MongoDB\\Server");
                found = scanVersionDirs(serverDir, "bin", exeName, versionHint);
                if (found != null) return found;
            }
            String userHome = System.getProperty("user.home", "");
            File scoop = new File(userHome, "scoop\\apps\\mongodb-database-tools\\current\\bin\\" + exeName);
            if (isValidExecutable(scoop)) return scoop.getAbsolutePath();
        }

        // SQL Server (bcp, sqlcmd)
        if (isSqlServerTool(tool)) {
            for (File drive : rootDrives) {
                File odbcDir = new File(drive, "Program Files\\Microsoft SQL Server\\Client SDK\\ODBC");
                String found = scanVersionDirs(odbcDir, "Tools\\Binn", exeName, versionHint);
                if (found != null) return found;

                File sqlDir = new File(drive, "Program Files\\Microsoft SQL Server");
                found = scanVersionDirs(sqlDir, "Tools\\Binn", exeName, versionHint);
                if (found != null) return found;

                File sqlDir86 = new File(drive, "Program Files (x86)\\Microsoft SQL Server");
                found = scanVersionDirs(sqlDir86, "Tools\\Binn", exeName, versionHint);
                if (found != null) return found;
            }
        }

        // Oracle
        if (isOracleTool(tool)) {
            String oracleHome = System.getenv("ORACLE_HOME");
            if (oracleHome != null && !oracleHome.isBlank()) {
                File f = new File(oracleHome, "bin\\" + exeName);
                if (isValidExecutable(f)) return f.getAbsolutePath();
            }
            for (File drive : rootDrives) {
                String found = scanPrefixDirs(new File(drive, "oracle\\product"), "", "dbhome_1\\bin", exeName, versionHint);
                if (found != null) return found;

                found = scanPrefixDirs(drive, "instantclient", "", exeName, versionHint);
                if (found != null) return found;

                found = scanPrefixDirs(new File(drive, "oracle"), "instantclient", "", exeName, versionHint);
                if (found != null) return found;
            }
        }

        return null;
    }

    // =========================================================================
    // Linux Discovery
    // =========================================================================

    private static String detectOnLinux(String tool, String versionHint, ConnectionProfile profile) {
        String binaryName = tool;

        // PostgreSQL tools
        if (isPostgreSqlTool(tool)) {
            // Debian / Ubuntu: /usr/lib/postgresql/<ver>/bin/<tool>
            File debianDir = new File("/usr/lib/postgresql");
            String found = scanVersionDirs(debianDir, "bin", binaryName, versionHint);
            if (found != null) return found;

            // RHEL / CentOS / Fedora: /usr/pgsql-<ver>/bin/<tool>
            found = scanPrefixDirs(new File("/usr"), "pgsql-", "bin", binaryName, versionHint);
            if (found != null) return found;

            File lib64 = new File("/usr/lib64/postgresql");
            found = scanVersionDirs(lib64, "bin", binaryName, versionHint);
            if (found != null) return found;

            File pgsqlLocal = new File("/usr/local/pgsql/bin/" + binaryName);
            if (isValidExecutable(pgsqlLocal)) return pgsqlLocal.getAbsolutePath();

            File usrLocalBin = new File("/usr/local/bin/" + binaryName);
            if (isValidExecutable(usrLocalBin)) return usrLocalBin.getAbsolutePath();

            File usrBin = new File("/usr/bin/" + binaryName);
            if (isValidExecutable(usrBin)) return usrBin.getAbsolutePath();
        }

        // MySQL & MariaDB
        if (isMySqlOrMariaDbTool(tool)) {
            File usrBin = new File("/usr/bin/" + binaryName);
            if (isValidExecutable(usrBin)) return usrBin.getAbsolutePath();

            File usrLocalBin = new File("/usr/local/bin/" + binaryName);
            if (isValidExecutable(usrLocalBin)) return usrLocalBin.getAbsolutePath();

            File mysqlLocal = new File("/usr/local/mysql/bin/" + binaryName);
            if (isValidExecutable(mysqlLocal)) return mysqlLocal.getAbsolutePath();

            File lampp = new File("/opt/lampp/bin/" + binaryName);
            if (isValidExecutable(lampp)) return lampp.getAbsolutePath();
        }

        // SQLite
        if ("sqlite3".equalsIgnoreCase(tool)) {
            File usrBin = new File("/usr/bin/sqlite3");
            if (isValidExecutable(usrBin)) return usrBin.getAbsolutePath();

            File usrLocalBin = new File("/usr/local/bin/sqlite3");
            if (isValidExecutable(usrLocalBin)) return usrLocalBin.getAbsolutePath();
        }

        // MongoDB
        if (isMongoDbTool(tool)) {
            File usrBin = new File("/usr/bin/" + binaryName);
            if (isValidExecutable(usrBin)) return usrBin.getAbsolutePath();

            File usrLocalBin = new File("/usr/local/bin/" + binaryName);
            if (isValidExecutable(usrLocalBin)) return usrLocalBin.getAbsolutePath();

            File opt = new File("/opt/mongodb/bin/" + binaryName);
            if (isValidExecutable(opt)) return opt.getAbsolutePath();
        }

        // SQL Server
        if (isSqlServerTool(tool)) {
            File mssql18 = new File("/opt/mssql-tools18/bin/" + binaryName);
            if (isValidExecutable(mssql18)) return mssql18.getAbsolutePath();

            File mssql = new File("/opt/mssql-tools/bin/" + binaryName);
            if (isValidExecutable(mssql)) return mssql.getAbsolutePath();

            File usrBin = new File("/usr/bin/" + binaryName);
            if (isValidExecutable(usrBin)) return usrBin.getAbsolutePath();
        }

        // Oracle
        if (isOracleTool(tool)) {
            String oracleHome = System.getenv("ORACLE_HOME");
            if (oracleHome != null && !oracleHome.isBlank()) {
                File f = new File(oracleHome, "bin/" + binaryName);
                if (isValidExecutable(f)) return f.getAbsolutePath();
            }
            String found = scanPrefixDirs(new File("/usr/lib/oracle"), "", "client64/bin", binaryName, versionHint);
            if (found != null) return found;
        }

        return null;
    }

    // =========================================================================
    // System PATH & Shell Lookup
    // =========================================================================

    private static String searchInPath(String canonicalTool, OS os) {
        String exeName = (os == OS.WINDOWS && !canonicalTool.toLowerCase(Locale.ROOT).endsWith(".exe"))
                ? canonicalTool + ".exe"
                : canonicalTool;

        Set<String> dirs = new LinkedHashSet<>();

        // 1. PATH environment variable
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null && !pathEnv.isBlank()) {
            for (String p : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                if (!p.isBlank()) dirs.add(p.trim());
            }
        }

        // 2. Common directories often missing from GUI app environment on macOS/Linux
        if (os == OS.MAC) {
            dirs.add("/opt/homebrew/bin");
            dirs.add("/opt/homebrew/sbin");
            dirs.add("/usr/local/bin");
            dirs.add("/usr/bin");
            dirs.add("/bin");
            dirs.add("/usr/sbin");
            dirs.add("/sbin");
        } else if (os == OS.LINUX) {
            dirs.add("/usr/local/bin");
            dirs.add("/usr/bin");
            dirs.add("/bin");
            dirs.add("/usr/local/sbin");
            dirs.add("/usr/sbin");
            dirs.add("/sbin");
            String home = System.getProperty("user.home", "");
            if (!home.isBlank()) dirs.add(home + "/.local/bin");
        }

        for (String dirStr : dirs) {
            File dir = new File(dirStr);
            if (dir.isDirectory()) {
                File candidate = new File(dir, exeName);
                if (isValidExecutable(candidate)) {
                    return candidate.getAbsolutePath();
                }
            }
        }

        return null;
    }

    private static String lookupWithWhichOrWhere(String canonicalTool, OS os) {
        String lookupCmd = (os == OS.WINDOWS) ? "where" : "which";
        try {
            Process process = new ProcessBuilder(lookupCmd, canonicalTool)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(800, TimeUnit.MILLISECONDS);
            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) {
                        File f = new File(line.trim());
                        if (isValidExecutable(f)) {
                            return f.getAbsolutePath();
                        }
                    }
                }
            } else if (!finished) {
                process.destroyForcibly();
            }
        } catch (Exception ignored) {}
        return null;
    }

    // =========================================================================
    // Directory Scanning & Sorting Helpers
    // =========================================================================

    /**
     * Scans version subdirectories under baseDir, sorting descending (newest version first),
     * and prioritizing the versionHint if specified.
     */
    private static String scanVersionDirs(File baseDir, String binSubdir, String binaryName, String versionHint) {
        if (baseDir == null || !baseDir.isDirectory()) return null;
        File[] subdirs = baseDir.listFiles(File::isDirectory);
        if (subdirs == null || subdirs.length == 0) return null;

        List<File> list = new ArrayList<>(Arrays.asList(subdirs));
        list.sort(createVersionComparator(versionHint));

        for (File dir : list) {
            File targetDir = (binSubdir == null || binSubdir.isBlank()) ? dir : new File(dir, binSubdir);
            File candidate = new File(targetDir, binaryName);
            if (isValidExecutable(candidate)) {
                return candidate.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Scans subdirectories matching prefix under baseDir.
     */
    private static String scanPrefixDirs(File baseDir, String prefix, String binSubdir, String binaryName, String versionHint) {
        if (baseDir == null || !baseDir.isDirectory()) return null;
        File[] subdirs = baseDir.listFiles(f -> f.isDirectory() && (prefix == null || prefix.isBlank() || f.getName().toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))));
        if (subdirs == null || subdirs.length == 0) return null;

        List<File> list = new ArrayList<>(Arrays.asList(subdirs));
        list.sort(createVersionComparator(versionHint));

        for (File dir : list) {
            File targetDir = (binSubdir == null || binSubdir.isBlank()) ? dir : new File(dir, binSubdir);
            File candidate = new File(targetDir, binaryName);
            if (isValidExecutable(candidate)) {
                return candidate.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Scans Homebrew opt directory for a formula prefix, e.g. "postgresql" or "mysql".
     * Checks postgresql@16, postgresql@15, postgresql, etc.
     */
    private static String scanBrewOptDirs(File brewOptDir, String formulaPrefix, String binaryName, String versionHint) {
        if (brewOptDir == null || !brewOptDir.isDirectory()) return null;
        File[] subdirs = brewOptDir.listFiles(f -> {
            String name = f.getName().toLowerCase(Locale.ROOT);
            return f.isDirectory() && (name.equals(formulaPrefix) || name.startsWith(formulaPrefix + "@"));
        });
        if (subdirs == null || subdirs.length == 0) return null;

        List<File> list = new ArrayList<>(Arrays.asList(subdirs));
        list.sort(createVersionComparator(versionHint));

        for (File dir : list) {
            File candidate = new File(new File(dir, "bin"), binaryName);
            if (isValidExecutable(candidate)) {
                return candidate.getAbsolutePath();
            }
        }
        return null;
    }

    private static Comparator<File> createVersionComparator(String versionHint) {
        return (f1, f2) -> {
            String n1 = f1.getName();
            String n2 = f2.getName();

            // If a version hint is provided (e.g. "16"), give exact match top priority
            if (versionHint != null && !versionHint.isBlank()) {
                boolean m1 = n1.equals(versionHint) || n1.contains("@" + versionHint) || n1.contains("-" + versionHint) || n1.contains(" " + versionHint);
                boolean m2 = n2.equals(versionHint) || n2.contains("@" + versionHint) || n2.contains("-" + versionHint) || n2.contains(" " + versionHint);
                if (m1 && !m2) return -1;
                if (!m1 && m2) return 1;
            }

            // Otherwise sort numerically descending
            double v1 = extractNumericVersion(n1);
            double v2 = extractNumericVersion(n2);
            return Double.compare(v2, v1);
        };
    }

    private static double extractNumericVersion(String name) {
        Matcher matcher = NUMERIC_VERSION_PATTERN.matcher(name);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (Exception ignored) {}
        }
        return -1.0;
    }

    private static boolean isValidExecutable(File f) {
        if (f == null) return false;
        if (!f.isFile()) return false;
        if (OS.current() == OS.WINDOWS) {
            return f.exists() && f.length() > 0;
        }
        return f.canExecute();
    }

    private static String canonicalToolName(String tool) {
        String name = new File(tool).getName();
        if (name.toLowerCase(Locale.ROOT).endsWith(".exe")) {
            name = name.substring(0, name.length() - 4);
        }
        return name.trim();
    }

    private static String extractVersionHint(ConnectionProfile profile) {
        if (profile == null) return "";
        String name = profile.getName();
        if (name != null) {
            // E.g. "erp-161" -> check for common versions: 17, 16, 15, 14, 13, 12, 11, 8.0, etc.
            for (int v = 18; v >= 10; v--) {
                if (name.contains(String.valueOf(v))) {
                    return String.valueOf(v);
                }
            }
        }
        return "";
    }

    private static boolean isPostgreSqlTool(String tool) {
        String t = tool.toLowerCase(Locale.ROOT);
        return t.equals("pg_dump") || t.equals("pg_restore") || t.equals("psql") || t.equals("pg_dumpall");
    }

    private static boolean isMySqlOrMariaDbTool(String tool) {
        String t = tool.toLowerCase(Locale.ROOT);
        return t.equals("mysqldump") || t.equals("mysql") || t.equals("mariadb-dump") || t.equals("mariadb");
    }

    private static boolean isMongoDbTool(String tool) {
        String t = tool.toLowerCase(Locale.ROOT);
        return t.equals("mongodump") || t.equals("mongorestore") || t.equals("mongosh") || t.equals("mongo");
    }

    private static boolean isSqlServerTool(String tool) {
        String t = tool.toLowerCase(Locale.ROOT);
        return t.equals("bcp") || t.equals("sqlcmd");
    }

    private static boolean isOracleTool(String tool) {
        String t = tool.toLowerCase(Locale.ROOT);
        return t.equals("expdp") || t.equals("impdp") || t.equals("sqlplus") || t.equals("exp") || t.equals("imp");
    }
}

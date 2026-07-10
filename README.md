# DBNavigator Pro 2.0

A DataGrip-style desktop database IDE built with JavaFX. One unified explorer
for relational and NoSQL databases.

## Supported databases

| Database    | Driver                          | Browse | Query console | Data grid |
|-------------|---------------------------------|--------|---------------|-----------|
| MySQL       | mysql-connector-j               | ✅     | ✅ SQL        | ✅ paged  |
| MariaDB     | mariadb-java-client             | ✅     | ✅ SQL        | ✅ paged  |
| PostgreSQL  | postgresql                      | ✅     | ✅ SQL        | ✅ paged  |
| SQL Server  | mssql-jdbc                      | ✅     | ✅ SQL        | ✅ paged  |
| Oracle      | ojdbc11                         | ✅     | ✅ SQL        | ✅ paged  |
| SQLite      | sqlite-jdbc (file-based)        | ✅     | ✅ SQL        | ✅ paged  |
| MongoDB     | mongodb-driver-sync             | ✅     | ✅ JSON find  | ✅ paged  |

## Features

- **Database Explorer** — all connections in one lazy-loading tree:
  databases → schemas → tables / views / procedures / functions / sequences,
  or databases → collections for MongoDB.
- **SQL console tabs** — syntax-highlighted editor (keywords, strings,
  numbers, comments), line numbers, `Ctrl+Enter` to run, run only the
  selected text, configurable row limit, execution time.
- **Data browser** — double-click any table: paged grid (500 rows/page)
  with free-form `WHERE` filter and `ORDER BY`, row counts, CSV export.
- **Structure viewer** — columns (type, size, nullable, default, PK) and
  indexes for any table.
- **MongoDB browser** — double-click a collection: paged document grid with
  JSON filter support (e.g. `{"age": {"$gt": 21}}`), CSV export.
- **Connection manager** — test connection before saving; profiles persisted
  to `~/.dbnavigator/connections.json`.
- **Professional dark theme** throughout (Darcula-inspired).
- All database work runs on background threads — the UI never freezes.

## Requirements

- JDK 21+
- Maven 3.8+

## Run

```bash
mvn clean javafx:run
```

## Package a distributable app (optional)

```bash
mvn clean package
# then use jpackage to build a native installer, e.g.:
jpackage --name "DBNavigator Pro" --input target --main-jar DBNavigatorPro-2.0.0.jar
```

(For a production build you'd typically use the `javafx-maven-plugin` jlink
goal or the `maven-shade-plugin`; ask if you want that set up.)

## Security note

Saved passwords are Base64-obfuscated in the JSON file — that is **not**
encryption. For real protection, leave "Save password" unchecked (you'll be
asked when connecting), or integrate an OS keychain library such as
`java-keyring`.

## Project layout

```
src/main/java/com/roze/dbnavigator/
├── Main.java                     entry point
├── model/                        ConnectionProfile, DbObject, QueryResult
├── db/
│   ├── JdbcClient.java           pooled JDBC execution (HikariCP)
│   ├── MongoDbClient.java        MongoDB driver wrapper
│   ├── ClientRegistry.java       one live client per profile
│   ├── MetadataService.java      lazy schema-tree loading for every DB type
│   └── ConnectionStore.java      JSON persistence
├── ui/
│   ├── MainWindow.java           toolbar + explorer + tabs + status bar
│   ├── SchemaTreePane.java       lazy tree with context menus
│   ├── ConnectionDialog.java     data source dialog with Test Connection
│   ├── QueryTab.java             SQL console
│   ├── DataTab.java              paged table data browser
│   ├── StructureTab.java         columns & indexes
│   ├── MongoCollectionTab.java   Mongo document browser
│   ├── ResultGrid.java           shared result grid + CSV export
│   ├── SqlHighlighter.java       regex SQL highlighting
│   └── Icons.java                FontAwesome icon factory
└── resources/css/app.css         dark theme
```

## v2.1 — DataGrip-parity update

- **Multi-database PostgreSQL**: one `postgres@localhost` connection now lists
  *every* database on the server (erpdb, nexadb, …). Each database gets its own
  pooled connection behind the scenes.
- **Table sub-tree**: expand any table → `columns 64`, `indexes 3`,
  `partitions 31` (PostgreSQL) folders with counts, then the actual columns
  (with type / PK / not-null), indexes (with column list / unique) and partitions.
- **Console autocomplete**: type to get keyword + table suggestions,
  `tablename.` suggests that table's columns, `Ctrl+Space` forces the popup,
  `↑/↓` navigate, `Enter`/`Tab` insert, `Esc` closes.
- **Editable data grid**: if the table has a primary key, double-click a cell,
  type a new value, press Enter. Edits queue up as pending changes —
  `Submit (n)` writes them in one transaction (rolls back on any failure),
  `Revert` discards. Typing `NULL` sets SQL NULL.
- **Dump / Restore**: right-click a database node → *Dump Database to .sql…*
  (pg_dump / mysqldump) or *Restore .sql into This Database…* (psql / mysql).
  Passwords are passed via `PGPASSWORD` / `MYSQL_PWD` env vars, never on the
  command line. The client tools must be installed; on Windows point the dialog
  at e.g. `C:\Program Files\PostgreSQL\16\bin\pg_dump.exe`.
- **Per-database consoles**: right-click a database → *New Query Console on X*.

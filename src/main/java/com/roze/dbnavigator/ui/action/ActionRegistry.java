package com.roze.dbnavigator.ui.action;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.ui.MainWindow;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.List;

/**
 * DataGrip-style action registry.
 * Defines and registers all actions and action groups for menus,
 * center header action icons (4 icons + 3 dots), and toolbars dynamically.
 */
public final class ActionRegistry {

    private ActionRegistry() {}

    public static void initialize(ActionManager manager) {
        // =========================================================================
        // 1. FILE ACTIONS (Matching DataGrip)
        // =========================================================================
        // Submenu: New >
        ActionGroup newGroup = new ActionGroup("file.new", "New", true);

        // 1. Project...
        AnAction newProject = AnAction.builder("file.new.project", "Project…")
                .description("Create or initialize a new DBNavigator project")
                .onAction(MainWindow::createNewProjectDialog)
                .build();

        // 2. SQL File
        AnAction newSqlFile = AnAction.builder("file.new.sqlfile", "SQL File")
                .description("Create a new SQL file")
                .icon(FontAwesomeSolid.FILE_CODE, "#a9b7c6", 11)
                .onAction(MainWindow::openNewSqlFile)
                .build();

        // 3. Scratch File (Ctrl+Alt+Shift+Insert)
        AnAction newScratchFile = AnAction.builder("file.new.scratch", "Scratch File")
                .description("Open a scratch SQL buffer")
                .icon(FontAwesomeSolid.FILE_ALT, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.INSERT, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::openNewScratchFile)
                .build();

        // 4. Query Console (Ctrl+Shift+Q)
        AnAction newConsole = AnAction.builder("file.new.console", "Query Console")
                .description("Open a new query console for the selected connection")
                .icon(FontAwesomeSolid.TERMINAL, "#6897bb", 11)
                .accelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::openConsoleForSelectedConnection)
                .build();

        // 5. Query File... (Ctrl+Alt+Shift+Q)
        AnAction newQueryFile = AnAction.builder("file.new.queryfile", "Query File…")
                .description("Create a new SQL query file attached to the current connection")
                .icon(FontAwesomeSolid.FILE_CODE, "#4a88c7", 11)
                .accelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::openNewQueryFile)
                .build();

        // 6. Database
        AnAction newDatabase = AnAction.builder("file.new.database", "Database")
                .description("Create a new database on the active server")
                .icon(FontAwesomeSolid.DATABASE, "#4a88c7", 11)
                .onAction(MainWindow::createNewDatabaseAction)
                .build();

        // 7. Role
        AnAction newRole = AnAction.builder("file.new.role", "Role")
                .description("Create a new database role or permission group")
                .icon(FontAwesomeSolid.USER_SHIELD, "#e0a44c", 11)
                .onAction(MainWindow::createNewRoleAction)
                .build();

        // 8. User
        AnAction newUser = AnAction.builder("file.new.user", "User")
                .description("Create a new database user account")
                .icon(FontAwesomeSolid.USER, "#57965c", 11)
                .onAction(MainWindow::createNewUserAction)
                .build();

        // 9. Virtual View
        AnAction newVirtualView = AnAction.builder("file.new.virtualview", "Virtual View")
                .description("Create a new virtual view or custom query view")
                .icon(FontAwesomeSolid.TABLE, "#c77dbb", 11)
                .onAction(MainWindow::createNewVirtualViewAction)
                .build();

        // 10. Data Source >
        ActionGroup dataSourceGroup = new ActionGroup("file.new.datasource.group", "Data Source", true,
                FontAwesomeSolid.DATABASE, "#57965c", 11, "Data Source");
        dataSourceGroup.add(AnAction.builder("file.new.datasource.mysql", "MySQL")
                .icon(FontAwesomeSolid.DATABASE, "#4a88c7", 11)
                .onAction(ctx -> ctx.showNewConnectionDialog(ConnectionProfile.DatabaseType.MYSQL))
                .build());
        dataSourceGroup.add(AnAction.builder("file.new.datasource.mariadb", "MariaDB")
                .icon(FontAwesomeSolid.DATABASE, "#4a88c7", 11)
                .onAction(ctx -> ctx.showNewConnectionDialog(ConnectionProfile.DatabaseType.MARIADB))
                .build());
        dataSourceGroup.add(AnAction.builder("file.new.datasource.postgres", "PostgreSQL")
                .icon(FontAwesomeSolid.DATABASE, "#3592c4", 11)
                .onAction(ctx -> ctx.showNewConnectionDialog(ConnectionProfile.DatabaseType.POSTGRESQL))
                .build());
        dataSourceGroup.add(AnAction.builder("file.new.datasource.stratos", "StratosDB")
                .icon(FontAwesomeSolid.DATABASE, "#57965c", 11)
                .onAction(ctx -> ctx.showNewConnectionDialog(ConnectionProfile.DatabaseType.STRATOSDB))
                .build());
        dataSourceGroup.add(AnAction.builder("file.new.datasource.sqlite", "SQLite")
                .icon(FontAwesomeSolid.DATABASE, "#e0a44c", 11)
                .onAction(ctx -> ctx.showNewConnectionDialog(ConnectionProfile.DatabaseType.SQLITE))
                .build());
        dataSourceGroup.add(AnAction.builder("file.new.datasource.oracle", "Oracle")
                .icon(FontAwesomeSolid.DATABASE, "#e05555", 11)
                .onAction(ctx -> ctx.showNewConnectionDialog(ConnectionProfile.DatabaseType.ORACLE))
                .build());
        dataSourceGroup.add(AnAction.builder("file.new.datasource.sqlserver", "Microsoft SQL Server")
                .icon(FontAwesomeSolid.DATABASE, "#c77dbb", 11)
                .onAction(ctx -> ctx.showNewConnectionDialog(ConnectionProfile.DatabaseType.SQLSERVER))
                .build());
        dataSourceGroup.add(AnAction.builder("file.new.datasource.mongodb", "MongoDB")
                .icon(FontAwesomeSolid.LEAF, "#57965c", 11)
                .onAction(ctx -> ctx.showNewConnectionDialog(ConnectionProfile.DatabaseType.MONGODB))
                .build());
        dataSourceGroup.addSeparator();
        dataSourceGroup.add(AnAction.builder("file.new.datasource.generic", "More / Connection Wizard…")
                .icon(FontAwesomeSolid.PLUS_CIRCLE, "#57965c", 11)
                .onAction(MainWindow::showNewConnectionDialog)
                .build());

        // 11. Data Source from Cloud Provider >
        ActionGroup cloudGroup = new ActionGroup("file.new.datasource.cloud", "Data Source from Cloud Provider", true,
                FontAwesomeSolid.CLOUD, "#4a88c7", 11, "Data Source from Cloud Provider");

        ActionGroup awsGroup = new ActionGroup("file.new.datasource.cloud.aws", "Amazon AWS", true);
        awsGroup.add(AnAction.builder("cloud.aws.aurora.mysql", "Amazon Aurora MySQL")
                .icon(FontAwesomeSolid.DATABASE, "#e0a44c", 11)
                .onAction(ctx -> ctx.openDataSourceFromCloudDialog("AWS", ConnectionProfile.DatabaseType.MYSQL))
                .build());
        awsGroup.add(AnAction.builder("cloud.aws.aurora.postgres", "Amazon Aurora PostgreSQL")
                .icon(FontAwesomeSolid.DATABASE, "#3592c4", 11)
                .onAction(ctx -> ctx.openDataSourceFromCloudDialog("AWS", ConnectionProfile.DatabaseType.POSTGRESQL))
                .build());
        awsGroup.add(AnAction.builder("cloud.aws.redshift", "Amazon Redshift")
                .icon(FontAwesomeSolid.DATABASE, "#e05555", 11)
                .onAction(ctx -> ctx.openDataSourceFromCloudDialog("AWS", ConnectionProfile.DatabaseType.POSTGRESQL))
                .build());

        ActionGroup gcpGroup = new ActionGroup("file.new.datasource.cloud.gcp", "Google Cloud", true);
        gcpGroup.add(AnAction.builder("cloud.gcp.mysql", "Google Cloud SQL for MySQL")
                .icon(FontAwesomeSolid.DATABASE, "#4a88c7", 11)
                .onAction(ctx -> ctx.openDataSourceFromCloudDialog("Google Cloud", ConnectionProfile.DatabaseType.MYSQL))
                .build());
        gcpGroup.add(AnAction.builder("cloud.gcp.postgres", "Google Cloud SQL for PostgreSQL")
                .icon(FontAwesomeSolid.DATABASE, "#3592c4", 11)
                .onAction(ctx -> ctx.openDataSourceFromCloudDialog("Google Cloud", ConnectionProfile.DatabaseType.POSTGRESQL))
                .build());

        ActionGroup azureGroup = new ActionGroup("file.new.datasource.cloud.azure", "Microsoft Azure", true);
        azureGroup.add(AnAction.builder("cloud.azure.sql", "Azure SQL Database")
                .icon(FontAwesomeSolid.DATABASE, "#c77dbb", 11)
                .onAction(ctx -> ctx.openDataSourceFromCloudDialog("Azure", ConnectionProfile.DatabaseType.SQLSERVER))
                .build());
        azureGroup.add(AnAction.builder("cloud.azure.mysql", "Azure Database for MySQL")
                .icon(FontAwesomeSolid.DATABASE, "#4a88c7", 11)
                .onAction(ctx -> ctx.openDataSourceFromCloudDialog("Azure", ConnectionProfile.DatabaseType.MYSQL))
                .build());
        azureGroup.add(AnAction.builder("cloud.azure.postgres", "Azure Database for PostgreSQL")
                .icon(FontAwesomeSolid.DATABASE, "#3592c4", 11)
                .onAction(ctx -> ctx.openDataSourceFromCloudDialog("Azure", ConnectionProfile.DatabaseType.POSTGRESQL))
                .build());

        cloudGroup.add(awsGroup).add(gcpGroup).add(azureGroup);

        // 12. Data Source Templates
        AnAction dsTemplates = AnAction.builder("file.new.datasource.templates", "Data Source Templates")
                .description("Pre-configured Docker, Localhost, and In-Memory database templates")
                .icon(FontAwesomeSolid.LAYER_GROUP, "#a9b7c6", 11)
                .onAction(MainWindow::showDataSourceTemplatesDialog)
                .build();

        // 13. Data Source from File/Folder
        AnAction dsFromFile = AnAction.builder("file.new.datasource.file", "Data Source from File/Folder")
                .description("Create a data source from a SQLite database file, JSON, or CSV file")
                .icon(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 11)
                .onAction(MainWindow::openDataSourceFromFileOrFolder)
                .build();

        // 14. Data Source from URL
        AnAction dsFromUrl = AnAction.builder("file.new.datasource.url", "Data Source from URL")
                .description("Connect via JDBC URL or MongoDB URI")
                .icon(FontAwesomeSolid.LINK, "#3592c4", 11)
                .onAction(MainWindow::openDataSourceFromUrlDialog)
                .build();

        // 15. DDL Data Source
        AnAction ddlDataSource = AnAction.builder("file.new.datasource.ddl", "DDL Data Source")
                .description("Create an offline schema data source generated from DDL SQL files")
                .icon(FontAwesomeSolid.DATABASE, "#57965c", 11)
                .onAction(MainWindow::openDdlDataSourceDialog)
                .build();

        // 16. Folder
        AnAction newFolder = AnAction.builder("file.new.folder", "Folder")
                .description("Create a new organizational folder")
                .icon(FontAwesomeSolid.FOLDER, "#e0a44c", 11)
                .onAction(MainWindow::createNewFolderDialog)
                .build();

        // 17. Driver
        AnAction driverAction = AnAction.builder("file.new.driver", "Driver")
                .description("View and manage installed JDBC and database drivers")
                .icon(FontAwesomeSolid.PLUG, "#57965c", 11)
                .onAction(MainWindow::showDriversDialog)
                .build();

        // Assembling file.new matching DataGrip screenshot
        newGroup.add(newProject)
                .addSeparator()
                .add(newSqlFile)
                .add(newScratchFile)
                .add(newConsole)
                .add(newQueryFile)
                .addSeparator()
                .add(newDatabase)
                .add(newRole)
                .add(newUser)
                .addSeparator()
                .add(newVirtualView)
                .addSeparator()
                .add(dataSourceGroup)
                .add(cloudGroup)
                .add(dsTemplates)
                .add(dsFromFile)
                .add(dsFromUrl)
                .add(ddlDataSource)
                .addSeparator()
                .add(newFolder)
                .add(driverAction);

        // Open...
        AnAction openSql = AnAction.builder("file.open.sql", "Open…")
                .description("Open a SQL script file from disk into a console")
                .icon(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::openSqlFile)
                .build();

        AnAction saveAs = AnAction.builder("file.save.as", "Save As…")
                .description("Save active console script to disk")
                .accelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::saveConsoleAs)
                .build();

        // Recent Projects
        ActionGroup recentProjectsGroup = new ActionGroup("file.recent", "Recent Projects");
        recentProjectsGroup.setOnMenuShowing((menu, ctx) -> {
            menu.getItems().clear();
            java.util.List<com.roze.dbnavigator.model.Project> recents = com.roze.dbnavigator.db.ProjectStore.getRecentProjects();
            if (recents.isEmpty()) {
                MenuItem none = new MenuItem("No Recent Projects");
                none.setDisable(true);
                menu.getItems().add(none);
            } else {
                for (com.roze.dbnavigator.model.Project p : recents) {
                    MenuItem item = new MenuItem(p.getName() + " (" + p.getDisplayPath() + ")");
                    item.setOnAction(e -> ctx.switchProjectFlow(p));
                    menu.getItems().add(item);
                }
            }
        });
        recentProjectsGroup.add(AnAction.builder("file.recent.none", "No Recent Projects")
                .onAction(ctx -> {})
                .build());

        AnAction renameProject = AnAction.builder("file.rename.project", "Rename Project…")
                .description("Rename the current project")
                .onAction(MainWindow::renameProject)
                .build();

        AnAction attachDir = AnAction.builder("file.attach.directory", "Attach Directory to Project…")
                .description("Attach an external directory to the project")
                .icon(FontAwesomeSolid.FOLDER_PLUS, "#e0a44c", 11)
                .onAction(MainWindow::attachDirectoryToProject)
                .build();

        AnAction settings = AnAction.builder("file.settings", "Settings…")
                .description("Preferences and application configuration")
                .icon(FontAwesomeSolid.COG, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::showSettingsDialog)
                .build();

        AnAction dataSources = AnAction.builder("file.data.sources", "Data Sources…")
                .description("Manage Data Sources and Drivers")
                .icon(FontAwesomeSolid.DATABASE, "#4a88c7", 11)
                .accelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showNewConnectionDialog)
                .build();

        AnAction plugins = AnAction.builder("file.plugins", "Plugins…")
                .description("Manage installed database and IDE plugins")
                .onAction(MainWindow::showPluginsDialog)
                .build();

        AnAction sqlDialects = AnAction.builder("file.sql.dialects", "SQL Dialects…")
                .description("Configure SQL dialects per project and file")
                .onAction(MainWindow::showSqlDialectsDialog)
                .build();

        AnAction sqlScopes = AnAction.builder("file.sql.scopes", "SQL Resolution Scopes…")
                .description("Configure database and schema resolution scopes")
                .onAction(MainWindow::showSqlResolutionScopesDialog)
                .build();

        AnAction editDataSourcesXml = AnAction.builder("file.edit.datasources.xml", "Edit dataSources.xml")
                .description("Open connections dataSources configuration file")
                .onAction(MainWindow::editDataSourcesFile)
                .build();

        ActionGroup fileProps = new ActionGroup("file.properties", "File Properties");
        fileProps.add(AnAction.builder("file.props.encoding", "File Encoding: UTF-8")
                .onAction(ctx -> ctx.setStatus("File Encoding: UTF-8"))
                .build());
        fileProps.add(AnAction.builder("file.props.line.sep", "Line Separators: LF - Unix and macOS")
                .onAction(ctx -> ctx.setStatus("Line Separator: LF"))
                .build());

        // Local History Submenu Group
        ActionGroup localHistoryGroup = new ActionGroup("file.local.history", "Local History");
        localHistoryGroup.add(AnAction.builder("history.show", "Show History…")
                .description("Show local revision history for current console")
                .icon(FontAwesomeSolid.HISTORY, "#a9b7c6", 11)
                .onAction(MainWindow::showLocalHistoryForCurrentConsole)
                .build());
        localHistoryGroup.add(AnAction.builder("history.show.selection", "Show History for Selection…")
                .onAction(MainWindow::showHistoryForSelection)
                .build());
        localHistoryGroup.add(AnAction.builder("history.show.project", "Show Project History…")
                .onAction(MainWindow::showProjectHistoryDialog)
                .build());
        localHistoryGroup.add(AnAction.builder("history.recent.changes", "Recent Changes")
                .onAction(MainWindow::showRecentChangesDialog)
                .build());
        localHistoryGroup.addSeparator();
        localHistoryGroup.add(AnAction.builder("history.put.label", "Put Label…")
                .icon(FontAwesomeSolid.TAG, "#a9b7c6", 11)
                .onAction(MainWindow::showPutLabelDialog)
                .build());

        AnAction saveAll = AnAction.builder("file.save.all", "Save All")
                .description("Save active console contents to a file")
                .icon(FontAwesomeSolid.SAVE, "#4a88c7", 11)
                .accelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::saveConsoleAs)
                .build();

        AnAction reloadAll = AnAction.builder("file.reload.all", "Reload All from Disk")
                .description("Reload metadata and schema files from disk")
                .icon(FontAwesomeSolid.SYNC_ALT, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::reloadAllFromDisk)
                .build();

        // Kept for Center 3-dots toolbar
        AnAction invalidateCaches = AnAction.builder("file.invalidate.caches", "Invalidate Caches…")
                .description("Invalidate metadata caches and restart")
                .icon(FontAwesomeSolid.SYNC_ALT, "#a9b7c6", 11)
                .onAction(MainWindow::showInvalidateCachesDialog)
                .build();

        ActionGroup manageSettings = new ActionGroup("file.manage.settings", "Manage IDE Settings");
        manageSettings.add(AnAction.builder("file.settings.export", "Export Settings…")
                .onAction(ctx -> ctx.setStatus("Settings exported"))
                .build());
        manageSettings.add(AnAction.builder("file.settings.restore", "Restore Default Settings…")
                .onAction(ctx -> ctx.setStatus("Default settings active"))
                .build());

        ActionGroup exportGroup = new ActionGroup("file.export", "Export");
        exportGroup.add(AnAction.builder("file.export.data", "Export Data…")
                .icon(FontAwesomeSolid.FILE_EXPORT, "#e0a44c", 11)
                .onAction(ctx -> ctx.setStatus("Use Result Grid to export query results"))
                .build());

        AnAction printAction = AnAction.builder("file.print", "Print…")
                .icon(FontAwesomeSolid.PRINT, "#a9b7c6", 11)
                .enabledWhen(ctx -> false)
                .onAction(ctx -> {})
                .build();

        AnAction powerSave = AnAction.builder("file.power.save", "Power Save Mode")
                .onAction(MainWindow::togglePowerSaveMode)
                .build();

        AnAction exit = AnAction.builder("file.exit", "Exit")
                .description("Exit DBNavigator Pro")
                .onAction(MainWindow::closeWindow)
                .build();

        ActionGroup fileMenu = new ActionGroup("menu.file", "File");
        fileMenu.add(newGroup)
                .add(openSql)
                .add(saveAs)
                .add(recentProjectsGroup)
                .add(renameProject)
                .add(attachDir)
                .addSeparator()
                .add(settings)
                .add(dataSources)
                .add(plugins)
                .add(sqlDialects)
                .add(sqlScopes)
                .add(editDataSourcesXml)
                .addSeparator()
                .add(fileProps)
                .add(localHistoryGroup)
                .addSeparator()
                .add(saveAll)
                .add(reloadAll)
                .add(manageSettings)
                .add(exportGroup)
                .add(printAction)
                .addSeparator()
                .add(powerSave)
                .addSeparator()
                .add(exit);

        // =========================================================================
        // 2. EDIT ACTIONS (Matching DataGrip)
        // =========================================================================
        // Section 1: Undo / Redo
        AnAction undo = AnAction.builder("edit.undo", "Undo")
                .icon(FontAwesomeSolid.UNDO, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::undoCurrentEditor)
                .build();

        AnAction redo = AnAction.builder("edit.redo", "Redo")
                .icon(FontAwesomeSolid.REDO, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::redoCurrentEditor)
                .build();

        // Section 2: Cut, Copy, Copy as Plain Text, Copy Path/Reference, Paste, Delete
        AnAction cut = AnAction.builder("edit.cut", "Cut")
                .icon(FontAwesomeSolid.CUT, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::cutCurrentEditor)
                .build();

        AnAction copy = AnAction.builder("edit.copy", "Copy")
                .icon(FontAwesomeSolid.COPY, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyCurrentEditor)
                .build();

        AnAction copyPlain = AnAction.builder("edit.copy.plain", "Copy as Plain Text")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyAsPlainTextCurrentEditor)
                .build();

        AnAction copyRef = AnAction.builder("edit.copy.reference", "Copy Path/Reference…")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyPathOrReferenceCurrentEditor)
                .build();

        AnAction paste = AnAction.builder("edit.paste", "Paste")
                .icon(FontAwesomeSolid.PASTE, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::pasteCurrentEditor)
                .build();

        AnAction deleteAction = AnAction.builder("edit.delete", "Delete")
                .accelerator(new KeyCodeCombination(KeyCode.DELETE))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::deleteCurrentEditor)
                .build();

        // Section 3: Find, Replace, Find in Files, Replace in Files, Find Usages
        AnAction findAction = AnAction.builder("edit.find", "Find…")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::showFindDialog)
                .build();

        AnAction replaceAction = AnAction.builder("edit.replace", "Replace…")
                .accelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::showReplaceDialog)
                .build();

        AnAction findInFiles = AnAction.builder("edit.find.in.files", "Find in Files…")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchEverywhere)
                .build();

        AnAction replaceInFiles = AnAction.builder("edit.replace.in.files", "Replace in Files…")
                .accelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showReplaceInFilesDialog)
                .build();

        AnAction findUsages = AnAction.builder("edit.find.usages", "Find Usages")
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT7, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::findUsagesCurrentSymbol)
                .build();

        // Section 4: Generate, Insert Live Template, Surround With
        AnAction generate = AnAction.builder("edit.generate", "Generate…")
                .accelerator(new KeyCodeCombination(KeyCode.INSERT, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::generateSqlSnippet)
                .build();

        AnAction insertLiveTemplate = AnAction.builder("edit.insert.live.template", "Insert Live Template…")
                .accelerator(new KeyCodeCombination(KeyCode.J, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::insertLiveTemplate)
                .build();

        AnAction surroundWith = AnAction.builder("edit.surround.with", "Surround With…")
                .accelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::surroundWithTemplate)
                .build();

        // Section 5: Reformat Code, Reformat File, Comment Line, Comment Block, Auto-Indent
        AnAction formatCode = AnAction.builder("edit.format.code", "Reformat Code")
                .description("Reformat SQL queries with clean indentation and capitalized keywords")
                .icon(FontAwesomeSolid.INDENT, "#4a88c7", 11)
                .accelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveQueryTab)
                .onAction(MainWindow::formatCurrentSql)
                .build();

        AnAction formatFile = AnAction.builder("edit.format.file", "Reformat File…")
                .accelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveQueryTab)
                .onAction(MainWindow::formatCurrentSql)
                .build();

        AnAction commentLine = AnAction.builder("edit.comment.line", "Comment with Line Comment")
                .accelerator(new KeyCodeCombination(KeyCode.SLASH, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::toggleLineCommentCurrentEditor)
                .build();

        AnAction commentBlock = AnAction.builder("edit.comment.block", "Comment with Block Comment")
                .accelerator(new KeyCodeCombination(KeyCode.SLASH, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::toggleBlockCommentCurrentEditor)
                .build();

        AnAction autoIndent = AnAction.builder("edit.auto.indent", "Auto-Indent Lines")
                .accelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::autoIndentCurrentEditor)
                .build();

        // Section 6: Refactor >
        ActionGroup refactorGroup = new ActionGroup("edit.refactor", "Refactor", true);
        refactorGroup.add(AnAction.builder("edit.refactor.rename", "Rename…")
                .accelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Refactor: Rename symbol"))
                .build());
        refactorGroup.add(AnAction.builder("edit.refactor.extract.view", "Extract View…")
                .onAction(MainWindow::createNewVirtualViewAction)
                .build());
        refactorGroup.add(AnAction.builder("edit.refactor.extract.subquery", "Extract Subquery…")
                .onAction(ctx -> ctx.setStatus("Refactor: Extract subquery"))
                .build());

        // Section 7: Selection >, Toggle Case, Join Lines, Duplicate Entire Lines, Sort Lines
        ActionGroup selectionGroup = new ActionGroup("edit.selection", "Selection", true);
        AnAction selectAll = AnAction.builder("edit.select.all", "Select All")
                .accelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::selectAllCurrentEditor)
                .build();
        AnAction extendSelection = AnAction.builder("edit.selection.extend", "Extend Selection")
                .accelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::extendSelectionCurrentEditor)
                .build();
        AnAction shrinkSelection = AnAction.builder("edit.selection.shrink", "Shrink Selection")
                .accelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::shrinkSelectionCurrentEditor)
                .build();
        selectionGroup.addAll(selectAll, extendSelection, shrinkSelection);

        AnAction toggleCase = AnAction.builder("edit.toggle.case", "Toggle Case")
                .accelerator(new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::toggleCaseCurrentEditor)
                .build();

        AnAction joinLines = AnAction.builder("edit.join.lines", "Join Lines")
                .accelerator(new KeyCodeCombination(KeyCode.J, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::joinLinesCurrentEditor)
                .build();

        AnAction duplicateLines = AnAction.builder("edit.duplicate.lines", "Duplicate Entire Lines")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::duplicateLinesCurrentEditor)
                .build();

        AnAction sortLines = AnAction.builder("edit.sort.lines", "Sort Lines")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::sortLinesCurrentEditor)
                .build();

        // Section 8: Bookmarks
        AnAction toggleBookmark = AnAction.builder("edit.toggle.bookmark", "Toggle Bookmark")
                .icon(FontAwesomeSolid.BOOKMARK, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F11))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::toggleBookmarkCurrentEditor)
                .build();

        AnAction showBookmarks = AnAction.builder("edit.show.bookmarks", "Show Line Bookmarks…")
                .accelerator(new KeyCodeCombination(KeyCode.F11, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showBookmarksDialog)
                .build();

        ActionGroup editMenu = new ActionGroup("menu.edit", "Edit");
        editMenu.addAll(undo, redo)
                .addSeparator()
                .addAll(cut, copy, copyPlain, copyRef, paste, deleteAction)
                .addSeparator()
                .addAll(findAction, replaceAction, findInFiles, replaceInFiles, findUsages)
                .addSeparator()
                .addAll(generate, insertLiveTemplate, surroundWith)
                .addSeparator()
                .addAll(formatCode, formatFile, commentLine, commentBlock, autoIndent)
                .addSeparator()
                .add(refactorGroup)
                .addSeparator()
                .addAll(selectionGroup, toggleCase, joinLines, duplicateLines, sortLines)
                .addSeparator()
                .addAll(toggleBookmark, showBookmarks);

        // =========================================================================
        // 3. VIEW ACTIONS (Matching DataGrip)
        // =========================================================================

        // --- Submenu: View -> Tool Windows ---
        ActionGroup toolWindowsGroup = new ActionGroup("view.toolwindows", "Tool Windows", true);
        AnAction twCommit = AnAction.builder("view.toolwindow.commit", "Commit")
                .icon(FontAwesomeSolid.CHECK, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleCommitToolWindow)
                .build();
        AnAction twDatabase = AnAction.builder("view.toolwindow.database", "Database Explorer")
                .icon(FontAwesomeSolid.DATABASE, "#4a88c7", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleDatabaseExplorer)
                .build();
        AnAction twFiles = AnAction.builder("view.toolwindow.files", "Files")
                .icon(FontAwesomeSolid.FOLDER, "#e0a44c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleFilesToolWindow)
                .build();
        AnAction twFind = AnAction.builder("view.toolwindow.find", "Find")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleFindToolWindow)
                .build();
        AnAction twRun = AnAction.builder("view.toggle.run", "Run")
                .icon(FontAwesomeSolid.PLAY, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleRunPanel)
                .build();
        AnAction twDebug = AnAction.builder("view.toolwindow.debug", "Debug")
                .icon(FontAwesomeSolid.BUG, "#e05555", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT5, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleDebugToolWindow)
                .build();
        AnAction twProblems = AnAction.builder("view.toolwindow.problems", "Problems")
                .icon(FontAwesomeSolid.EXCLAMATION_TRIANGLE, "#e0a44c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT6, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleProblemsToolWindow)
                .build();
        AnAction twStructure = AnAction.builder("view.toolwindow.structure", "Structure")
                .icon(FontAwesomeSolid.SITEMAP, "#c77dbb", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT7, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleStructureToolWindow)
                .build();
        AnAction twServices = AnAction.builder("view.toolwindow.services", "Services")
                .icon(FontAwesomeSolid.CUBES, "#4a88c7", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT8, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleServicesToolWindow)
                .build();
        AnAction twVcs = AnAction.builder("view.toolwindow.vcs", "Version Control")
                .icon(FontAwesomeSolid.CODE_BRANCH, "#4a88c7", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT9, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleVersionControlToolWindow)
                .build();
        AnAction twAi = AnAction.builder("view.toolwindow.ai", "AI Assistant")
                .icon(FontAwesomeSolid.ROBOT, "#6897bb", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::toggleAiAssistantToolWindow)
                .build();

        AnAction twBackupSync = AnAction.builder("view.toolwindow.backup.sync", "Backup and Sync History")
                .icon(FontAwesomeSolid.HISTORY, "#a9b7c6", 11)
                .onAction(MainWindow::showBackupAndSyncHistory)
                .build();
        AnAction twBookmarks = AnAction.builder("view.toolwindow.bookmarks", "Bookmarks")
                .icon(FontAwesomeSolid.BOOKMARK, "#a9b7c6", 11)
                .onAction(MainWindow::showBookmarksDialog)
                .build();
        AnAction twCoverage = AnAction.builder("view.toolwindow.coverage", "Coverage")
                .icon(FontAwesomeSolid.SHIELD_ALT, "#57965c", 11)
                .onAction(MainWindow::showCoverageToolWindow)
                .build();
        AnAction twDbChanges = AnAction.builder("view.toolwindow.dbchanges", "Database Changes")
                .icon(FontAwesomeSolid.DATABASE, "#e0a44c", 11)
                .onAction(MainWindow::showDatabaseChanges)
                .build();
        AnAction twHierarchy = AnAction.builder("view.toolwindow.hierarchy", "Hierarchy")
                .icon(FontAwesomeSolid.SITEMAP, "#a9b7c6", 11)
                .onAction(MainWindow::showHierarchyToolWindow)
                .build();
        AnAction twLearn = AnAction.builder("view.toolwindow.learn", "Learn")
                .icon(FontAwesomeSolid.GRADUATION_CAP, "#6897bb", 11)
                .onAction(MainWindow::showLearnToolWindow)
                .build();
        AnAction twNotifications = AnAction.builder("view.toolwindow.notifications", "Notifications")
                .icon(FontAwesomeSolid.BELL, "#a9b7c6", 11)
                .onAction(MainWindow::showNotificationsToolWindow)
                .build();
        AnAction twTerminal = AnAction.builder("view.toolwindow.terminal", "Terminal")
                .icon(FontAwesomeSolid.TERMINAL, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F12, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::showTerminalToolWindow)
                .build();
        AnAction twTodo = AnAction.builder("view.toolwindow.todo", "TODO")
                .icon(FontAwesomeSolid.TASKS, "#a9b7c6", 11)
                .onAction(MainWindow::showTodoToolWindow)
                .build();

        toolWindowsGroup.addAll(twCommit, twDatabase, twFiles, twFind, twRun, twDebug, twProblems, twStructure, twServices, twVcs, twAi)
                .addSeparator()
                .addAll(twBackupSync, twBookmarks, twCoverage, twDbChanges, twHierarchy, twLearn, twNotifications, twTerminal, twTodo);

        // --- Submenu: View -> Appearance -> Main Menu ---
        ActionGroup mainMenuPlacementGroup = new ActionGroup("view.appearance.mainmenu", "Main Menu", true);
        mainMenuPlacementGroup.add(ToggleAction.toggleBuilder("view.appearance.mainmenu.hamburger", "Hide under Hamburger Button")
                .isSelected(ctx -> ctx.getMainMenuPlacement() == MainWindow.MainMenuPlacement.HAMBURGER)
                .onToggle((ctx, sel) -> ctx.setMainMenuPlacement(MainWindow.MainMenuPlacement.HAMBURGER))
                .build());
        mainMenuPlacementGroup.add(ToggleAction.toggleBuilder("view.appearance.mainmenu.merge", "Merge with Main Toolbar")
                .isSelected(ctx -> ctx.getMainMenuPlacement() == MainWindow.MainMenuPlacement.MERGE_TOOLBAR)
                .onToggle((ctx, sel) -> ctx.setMainMenuPlacement(MainWindow.MainMenuPlacement.MERGE_TOOLBAR))
                .build());
        mainMenuPlacementGroup.add(ToggleAction.toggleBuilder("view.appearance.mainmenu.above", "Show above Main Toolbar")
                .isSelected(ctx -> ctx.getMainMenuPlacement() == MainWindow.MainMenuPlacement.ABOVE_TOOLBAR)
                .onToggle((ctx, sel) -> ctx.setMainMenuPlacement(MainWindow.MainMenuPlacement.ABOVE_TOOLBAR))
                .build());

        // --- Submenu: View -> Appearance -> Navigation Bar ---
        ActionGroup navBarGroup = new ActionGroup("view.appearance.navbar", "Navigation Bar", true);
        navBarGroup.add(ToggleAction.toggleBuilder("view.appearance.navbar.top", "Top")
                .isSelected(ctx -> ctx.getNavigationBarPlacement() == MainWindow.NavigationBarPlacement.TOP)
                .onToggle((ctx, sel) -> ctx.setNavigationBarPlacement(MainWindow.NavigationBarPlacement.TOP))
                .build());
        navBarGroup.add(ToggleAction.toggleBuilder("view.appearance.navbar.bottom", "Bottom")
                .isSelected(ctx -> ctx.getNavigationBarPlacement() == MainWindow.NavigationBarPlacement.BOTTOM)
                .onToggle((ctx, sel) -> ctx.setNavigationBarPlacement(MainWindow.NavigationBarPlacement.BOTTOM))
                .build());
        navBarGroup.add(ToggleAction.toggleBuilder("view.appearance.navbar.hide", "Don't Show")
                .isSelected(ctx -> ctx.getNavigationBarPlacement() == MainWindow.NavigationBarPlacement.DONT_SHOW)
                .onToggle((ctx, sel) -> ctx.setNavigationBarPlacement(MainWindow.NavigationBarPlacement.DONT_SHOW))
                .build());

        // --- Submenu: View -> Appearance -> Status Bar Widgets ---
        ActionGroup sbWidgetsGroup = new ActionGroup("view.appearance.statusbar.widgets", "Status Bar Widgets", true);
        // Section 1
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.status.text", "Status Text")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("statusText"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("statusText", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.fs.sync", "File System Sync")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("fsSync"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("fsSync", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.remote.wire", "Remote Development Wire Stats")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("remoteWire"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("remoteWire", sel))
                .build());
        sbWidgetsGroup.addSeparator();

        // Section 2
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.aggregator", "Aggregator")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("aggregator"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("aggregator", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.grid.pos", "Grid Position")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("gridPos"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("gridPos", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.line.col", "Line:Column Number")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("lineCol"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("lineCol", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.lang.services", "Language Services")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("langServices"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("langServices", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.line.sep", "Line Separator")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("lineSep"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("lineSep", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.file.enc", "File Encoding")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("fileEnc"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("fileEnc", sel))
                .build());
        sbWidgetsGroup.addSeparator();

        // Section 3
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.power.save", "Power Save Mode")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("powerSave"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("powerSave", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.editor.sel", "Editor Selection Mode")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("editorSel"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("editorSel", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.indentation", "Indentation")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("indentation"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("indentation", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.json.schema", "JSON Schema")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("jsonSchema"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("jsonSchema", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.mcp.server", "MCP Server")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("mcpServer"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("mcpServer", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.readonly", "Read-Only Attribute")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("readOnly"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("readOnly", sel))
                .build());
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.notifications", "Notifications")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("notifications"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("notifications", sel))
                .build());
        sbWidgetsGroup.addSeparator();

        // Section 4
        sbWidgetsGroup.add(ToggleAction.toggleBuilder("view.widget.memory", "Memory Indicator")
                .isSelected(ctx -> ctx.isStatusBarWidgetActive("memory"))
                .onToggle((ctx, sel) -> ctx.setStatusBarWidgetActive("memory", sel))
                .build());

        // --- Submenu: View -> Appearance ---
        ActionGroup appearanceGroup = new ActionGroup("view.appearance", "Appearance", true);
        AnAction presMode = AnAction.builder("view.appearance.presentation.mode", "Enter Presentation Mode")
                .onAction(MainWindow::togglePresentationMode)
                .build();
        AnAction distractMode = AnAction.builder("view.appearance.distraction.free", "Enter Distraction Free Mode")
                .onAction(MainWindow::toggleDistractionFreeMode)
                .build();
        AnAction fullScreen = AnAction.builder("view.appearance.full.screen", "Enter Full Screen")
                .onAction(MainWindow::toggleFullScreen)
                .build();
        AnAction zenMode = AnAction.builder("view.appearance.zen.mode", "Enter Zen Mode")
                .onAction(MainWindow::toggleZenMode)
                .build();

        AnAction compactMode = AnAction.builder("view.appearance.compact.mode", "Compact Mode")
                .onAction(MainWindow::toggleCompactMode)
                .build();
        AnAction zoomIde = AnAction.builder("view.appearance.zoom.ide", "Zoom IDE (Current: 100%)…")
                .onAction(MainWindow::showZoomIdeDialog)
                .build();
        AnAction presAssistant = AnAction.builder("view.appearance.presentation.assistant", "Presentation Assistant")
                .onAction(MainWindow::togglePresentationAssistant)
                .build();

        ToggleAction toolbarToggle = ToggleAction.toggleBuilder("view.appearance.toolbar", "Toolbar")
                .isSelected(MainWindow::isToolbarVisible)
                .onToggle(MainWindow::setToolbarVisible)
                .build();
        AnAction toolWindowBars = AnAction.builder("view.appearance.toolwindowbars", "Tool Window Bars")
                .onAction(ctx -> ctx.setToolWindowBarsVisible(!ctx.isToolWindowBarsVisible()))
                .build();

        ToggleAction statusBarToggle = ToggleAction.toggleBuilder("view.appearance.statusbar", "Status Bar")
                .isSelected(MainWindow::isStatusBarVisible)
                .onToggle(MainWindow::setStatusBarVisible)
                .build();

        appearanceGroup.addAll(presMode, distractMode, fullScreen, zenMode)
                .addSeparator()
                .addAll(compactMode, zoomIde, presAssistant)
                .addSeparator()
                .addAll(mainMenuPlacementGroup, toolbarToggle, navBarGroup, toolWindowBars)
                .addSeparator()
                .addAll(statusBarToggle, sbWidgetsGroup);

        // --- Main View Menu Items ---
        AnAction recentLocations = AnAction.builder("view.recent.locations", "Recent Locations")
                .accelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showRecentLocationsDialog)
                .build();
        AnAction recentFiles = AnAction.builder("view.recent.files", "Recent Files")
                .accelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::showRecentFilesDialog)
                .build();
        AnAction recentChangedFiles = AnAction.builder("view.recent.changed.files", "Recently Changed Files")
                .onAction(MainWindow::showRecentlyChangedFilesDialog)
                .build();
        AnAction recentChanges = AnAction.builder("view.recent.changes", "Recent Changes")
                .accelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showRecentChangesDialog)
                .build();

        AnAction increaseFont = AnAction.builder("view.font.increase", "Increase Font Size in All Editors")
                .accelerator(new KeyCodeCombination(KeyCode.PERIOD, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::increaseFontSizeInAllEditors)
                .build();
        AnAction decreaseFont = AnAction.builder("view.font.decrease", "Decrease Font Size in All Editors")
                .accelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::decreaseFontSizeInAllEditors)
                .build();
        AnAction resetFont = AnAction.builder("view.font.reset", "Reset Font Size in All Editors")
                .onAction(MainWindow::resetFontSizeInAllEditors)
                .build();

        // Refresh Database Explorer kept for backward compatibility and shortcut Ctrl+F5
        AnAction refreshExplorer = AnAction.builder("view.refresh.explorer", "Refresh Database Explorer")
                .description("Reload connection metadata schemas")
                .icon(FontAwesomeSolid.SYNC_ALT, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::refreshSchemaExplorer)
                .build();
        manager.registerAction(refreshExplorer);

        ActionGroup viewMenu = new ActionGroup("menu.view", "View");
        viewMenu.addAll(toolWindowsGroup, appearanceGroup)
                .addSeparator()
                .addAll(recentLocations, recentFiles, recentChangedFiles, recentChanges)
                .addSeparator()
                .addAll(increaseFont, decreaseFont, resetFont);

        // =========================================================================
        // 4. NAVIGATE ACTIONS (Matching DataGrip)
        // =========================================================================
        // Section 1: Back, Forward
        AnAction navBack = AnAction.builder("navigate.back", "Back")
                .icon(FontAwesomeSolid.ARROW_LEFT, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::canNavigateBack)
                .onAction(MainWindow::navigateBack)
                .build();

        AnAction navForward = AnAction.builder("navigate.forward", "Forward")
                .icon(FontAwesomeSolid.ARROW_RIGHT, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::canNavigateForward)
                .onAction(MainWindow::navigateForward)
                .build();

        // Section 2: Search Everywhere, Database Object..., File..., Code..., Text...
        AnAction searchEverywhere = AnAction.builder("navigate.search.everywhere", "Search Everywhere")
                .description("Search tables, views, columns, connections, and actions (Double Shift)")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                .onAction(MainWindow::showSearchEverywhere)
                .build();

        AnAction navDbObject = AnAction.builder("navigate.database.object", "Database Object…")
                .accelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::showSearchDatabaseObjectsDialog)
                .build();

        AnAction navFile = AnAction.builder("navigate.file", "File…")
                .accelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchFilesDialog)
                .build();

        AnAction navCode = AnAction.builder("navigate.code", "Code…")
                .accelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchCodeDialog)
                .build();

        AnAction navText = AnAction.builder("navigate.text", "Text…")
                .accelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchTextDialog)
                .build();

        // Section 3: Show Diagram...
        AnAction navDiagram = AnAction.builder("navigate.show.diagram", "Show Diagram…")
                .icon(FontAwesomeSolid.PROJECT_DIAGRAM, "#c77dbb", 11)
                .accelerator(new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showCurrentDiagram)
                .build();

        // Section 4: Jump to Query Console..., Select In..., Declaration or Usages
        AnAction jumpQueryConsole = AnAction.builder("navigate.jump.query.console", "Jump to Query Console…")
                .icon(FontAwesomeSolid.TERMINAL, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F10, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::jumpToQueryConsole)
                .build();

        AnAction selectIn = AnAction.builder("navigate.select.in", "Select In…")
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::selectInTarget)
                .build();

        AnAction declarationOrUsages = AnAction.builder("navigate.declaration.usages", "Declaration or Usages")
                .accelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::navigateDeclarationOrUsages)
                .build();

        // Section 5: Scroll from Editor
        AnAction scrollFromEditor = AnAction.builder("navigate.scroll.from.editor", "Scroll from Editor")
                .icon(FontAwesomeSolid.CROSSHAIRS, "#a9b7c6", 11)
                .onAction(MainWindow::scrollFromEditor)
                .build();

        // Section 6: Jump to Navigation Bar, File Path
        AnAction jumpNavBar = AnAction.builder("navigate.jump.navbar", "Jump to Navigation Bar")
                .accelerator(new KeyCodeCombination(KeyCode.HOME, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::jumpToNavigationBar)
                .build();

        AnAction filePath = AnAction.builder("navigate.file.path", "File Path")
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showFilePathPopup)
                .build();

        // Section 7: Next Statement, Previous Statement
        AnAction nextStatement = AnAction.builder("navigate.next.statement", "Next Statement")
                .accelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::navigateToNextStatement)
                .build();

        AnAction prevStatement = AnAction.builder("navigate.prev.statement", "Previous Statement")
                .accelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::navigateToPreviousStatement)
                .build();

        // Retain nextEditorTab and prevEditorTab for backward compatibility
        AnAction nextEditorTab = AnAction.builder("navigate.next.tab", "Select Next Tab")
                .accelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::selectNextTab)
                .build();
        AnAction prevEditorTab = AnAction.builder("navigate.prev.tab", "Select Previous Tab")
                .accelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::selectPreviousTab)
                .build();
        manager.registerAction(nextEditorTab);
        manager.registerAction(prevEditorTab);

        ActionGroup navigateMenu = new ActionGroup("menu.navigate", "Navigate");
        navigateMenu.addAll(navBack, navForward)
                .addSeparator()
                .addAll(searchEverywhere, navDbObject, navFile, navCode, navText)
                .addSeparator()
                .add(navDiagram)
                .addSeparator()
                .addAll(jumpQueryConsole, selectIn, declarationOrUsages)
                .addSeparator()
                .add(scrollFromEditor)
                .addSeparator()
                .addAll(jumpNavBar, filePath)
                .addSeparator()
                .addAll(nextStatement, prevStatement);

        // =========================================================================
        // 5. RUN ACTIONS (Matching DataGrip)
        // =========================================================================
        // Section 1: Edit Configurations…
        AnAction editConfigurations = AnAction.builder("run.edit.configurations", "Edit Configurations…")
                .description("Edit run and execution configurations")
                .onAction(MainWindow::showEditConfigurationsDialog)
                .build();

        // Section 2: Compare Data & Compare Schema Structure
        AnAction compareData = AnAction.builder("run.compare.data", "Compare Data")
                .description("Compare data between tables or result sets")
                .icon(FontAwesomeSolid.EXCHANGE_ALT, "#a9b7c6", 11)
                .onAction(MainWindow::showCompareDataDialog)
                .build();

        AnAction compareSchema = AnAction.builder("run.compare.schema", "Compare Schema Structure")
                .description("Compare DDL schema structures")
                .icon(FontAwesomeSolid.EXCHANGE_ALT, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::showCompareSchemaDialog)
                .build();

        // Section 3: Full-Text Search…
        AnAction fullTextSearch = AnAction.builder("run.fulltext.search", "Full-Text Search…")
                .description("Full-text search across database tables and columns")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showFullTextSearchDialog)
                .build();

        ActionGroup runMenu = new ActionGroup("menu.run", "Run");
        runMenu.add(editConfigurations)
                .addSeparator()
                .add(compareData)
                .add(compareSchema)
                .addSeparator()
                .add(fullTextSearch);

        // Retain run.statement and run.tool.window in ActionManager for execution shortcuts and buttons
        AnAction runStatement = AnAction.builder("run.statement", "Execute Statement")
                .description("Execute query statement at caret")
                .icon(FontAwesomeSolid.PLAY, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::executeCurrentStatement)
                .build();

        AnAction runToolWindow = AnAction.builder("run.tool.window", "Run Tool Window")
                .icon(FontAwesomeSolid.TERMINAL, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleRunPanel)
                .build();

        manager.registerAction(runStatement);
        manager.registerAction(runToolWindow);

        // =========================================================================
        // 6. VCS ACTIONS (Matching DataGrip)
        // =========================================================================
        // Section 1: Integration & Operations Popup
        AnAction enableIntegration = AnAction.builder("vcs.enable.integration", "Enable Version Control Integration…")
                .description("Select a version control system to associate with this project")
                .onAction(MainWindow::showEnableVcsIntegrationDialog)
                .build();

        AnAction vcsOperations = AnAction.builder("vcs.operations.popup", "VCS Operations Popup…")
                .description("Show quick popup with VCS operations")
                .accelerator(new KeyCodeCombination(KeyCode.BACK_QUOTE, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::showVcsOperationsPopup)
                .build();

        // Section 2: Patches
        AnAction applyPatch = AnAction.builder("vcs.apply.patch", "Apply Patch…")
                .description("Apply a patch from file to project")
                .onAction(MainWindow::showApplyPatchDialog)
                .build();

        AnAction applyPatchClipboard = AnAction.builder("vcs.apply.patch.clipboard", "Apply Patch from Clipboard…")
                .description("Apply a patch from clipboard to project")
                .onAction(MainWindow::showApplyPatchFromClipboardDialog)
                .build();

        // Section 3: VCS Checkout, Browse, Init
        AnAction getFromVcs = AnAction.builder("vcs.get.from.vcs", "Get from Version Control…")
                .description("Clone repository from version control")
                .onAction(MainWindow::showGetFromVcsDialog)
                .build();

        ActionGroup browseVcsRepo = new ActionGroup("vcs.browse.repository", "Browse VCS Repository", true);
        AnAction showGitLog = AnAction.builder("vcs.browse.git.log", "Show Git Repository Log…")
                .description("Show Git repository log and commit history")
                .onAction(MainWindow::showGitRepositoryLogDialog)
                .build();
        browseVcsRepo.add(showGitLog);

        AnAction createGitRepo = AnAction.builder("vcs.create.git.repository", "Create Git Repository…")
                .description("Initialize a new Git repository")
                .onAction(MainWindow::showCreateGitRepositoryDialog)
                .build();

        ActionGroup vcsMenu = new ActionGroup("menu.vcs", "VCS");
        vcsMenu.add(enableIntegration)
                .add(vcsOperations)
                .addSeparator()
                .add(applyPatch)
                .add(applyPatchClipboard)
                .addSeparator()
                .add(getFromVcs)
                .add(browseVcsRepo)
                .add(createGitRepo);

        // =========================================================================
        // 7. WINDOW ACTIONS (Matching DataGrip)
        // =========================================================================
        // Submenu 1: Layouts >
        ActionGroup layoutsSubmenu = new ActionGroup("window.layouts", "Layouts", true);
        AnAction layoutDefault = AnAction.builder("window.layouts.default", "Default")
                .description("Restore default tool window layout")
                .onAction(MainWindow::applyDefaultLayout)
                .build();

        ActionGroup customLayoutGroup = new ActionGroup("window.layouts.custom", "Custom", true);
        AnAction customApply = AnAction.builder("window.layouts.custom.apply", "Apply")
                .onAction(MainWindow::applyCustomLayout)
                .build();
        AnAction customSave = AnAction.builder("window.layouts.custom.save", "Save Changes into Current Layout")
                .onAction(MainWindow::saveChangesIntoCurrentLayout)
                .build();
        AnAction customRestore = AnAction.builder("window.layouts.custom.restore", "Restore Current Layout")
                .onAction(MainWindow::restoreCurrentLayout)
                .build();
        customLayoutGroup.addAll(customApply, customSave, customRestore);

        AnAction saveLayoutAsNew = AnAction.builder("window.layouts.save.as.new", "Save Current Layout as New…")
                .description("Save current tool window layout as a new preset")
                .onAction(MainWindow::saveCurrentLayoutAsNew)
                .build();

        layoutsSubmenu.add(layoutDefault)
                .add(customLayoutGroup)
                .addSeparator()
                .add(saveLayoutAsNew);

        // Submenu 2: Active Tool Window >
        ActionGroup activeToolWindowSubmenu = new ActionGroup("window.active.tool.window", "Active Tool Window", true);
        AnAction hideActiveTw = AnAction.builder("window.toolwindow.hide.active", "Hide Active Tool Window")
                .accelerator(new KeyCodeCombination(KeyCode.ESCAPE, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::hideActiveToolWindow)
                .build();
        AnAction hideSideTw = AnAction.builder("window.toolwindow.hide.side", "Hide Side Tool Windows")
                .onAction(MainWindow::hideSideToolWindows)
                .build();
        AnAction hideBottomTw = AnAction.builder("window.toolwindow.hide.bottom", "Hide Bottom Tool Windows")
                .onAction(MainWindow::hideBottomToolWindows)
                .build();
        AnAction hideAllTw = AnAction.builder("window.toolwindow.hide.all", "Hide All Windows")
                .accelerator(new KeyCodeCombination(KeyCode.F12, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::hideAllToolWindows)
                .build();
        AnAction jumpLastTw = AnAction.builder("window.toolwindow.jump.last", "Jump to Last Tool Window")
                .accelerator(new KeyCodeCombination(KeyCode.F12))
                .onAction(MainWindow::jumpToLastToolWindow)
                .build();
        AnAction maximizeTw = AnAction.builder("window.toolwindow.maximize", "Maximize Tool Window")
                .accelerator(new KeyCodeCombination(KeyCode.QUOTE, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::maximizeToolWindow)
                .build();

        AnAction selectNextTwTab = AnAction.builder("window.toolwindow.next.tab", "Select Next Tab")
                .accelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::selectNextToolWindowTab)
                .build();
        AnAction selectPrevTwTab = AnAction.builder("window.toolwindow.prev.tab", "Select Previous Tab")
                .accelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::selectPrevToolWindowTab)
                .build();
        AnAction closeActiveTwTab = AnAction.builder("window.toolwindow.close.active.tab", "Close Active Tab")
                .accelerator(new KeyCodeCombination(KeyCode.F4, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::closeActiveToolWindowTab)
                .build();

        ActionGroup viewModeSubmenu = new ActionGroup("window.toolwindow.view.mode", "View Mode", true);
        viewModeSubmenu.addAll(
                AnAction.builder("window.tw.viewmode.dock.pinned", "Dock Pinned").build(),
                AnAction.builder("window.tw.viewmode.dock.unpinned", "Dock Unpinned").build(),
                AnAction.builder("window.tw.viewmode.undock", "Undock").build(),
                AnAction.builder("window.tw.viewmode.float", "Float").build(),
                AnAction.builder("window.tw.viewmode.window", "Window").build()
        );

        ActionGroup moveToSubmenu = new ActionGroup("window.toolwindow.move.to", "Move to", true);
        moveToSubmenu.addAll(
                AnAction.builder("window.tw.moveto.left.top", "Left Top").build(),
                AnAction.builder("window.tw.moveto.left.bottom", "Left Bottom").build(),
                AnAction.builder("window.tw.moveto.bottom.left", "Bottom Left").build(),
                AnAction.builder("window.tw.moveto.bottom.right", "Bottom Right").build(),
                AnAction.builder("window.tw.moveto.right.top", "Right Top").build(),
                AnAction.builder("window.tw.moveto.right.bottom", "Right Bottom").build()
        );

        AnAction groupTabs = AnAction.builder("window.toolwindow.group.tabs", "Group Tabs")
                .onAction(MainWindow::toggleGroupToolWindowTabs)
                .build();

        ActionGroup resizeSubmenu = new ActionGroup("window.toolwindow.resize", "Resize", true);
        resizeSubmenu.addAll(
                AnAction.builder("window.tw.resize.left", "Stretch to Left").build(),
                AnAction.builder("window.tw.resize.right", "Stretch to Right").build(),
                AnAction.builder("window.tw.resize.top", "Stretch to Top").build(),
                AnAction.builder("window.tw.resize.bottom", "Stretch to Bottom").build()
        );

        activeToolWindowSubmenu.addAll(hideActiveTw, hideSideTw, hideBottomTw, hideAllTw, jumpLastTw, maximizeTw)
                .addSeparator()
                .addAll(selectNextTwTab, selectPrevTwTab, closeActiveTwTab)
                .addSeparator()
                .addAll(viewModeSubmenu, moveToSubmenu, groupTabs, resizeSubmenu);

        // Submenu 3: Editor Tabs >
        ActionGroup editorTabsSubmenu = new ActionGroup("window.editor.tabs", "Editor Tabs", true);
        AnAction editorNextTab = AnAction.builder("window.editor.next.tab", "Select Next Tab")
                .accelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::selectNextTab)
                .build();
        AnAction editorPrevTab = AnAction.builder("window.editor.prev.tab", "Select Previous Tab")
                .accelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::selectPreviousTab)
                .build();
        AnAction pinTab = AnAction.builder("window.editor.pin.tab", "Pin Tab")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::pinActiveTab)
                .build();
        AnAction keepTabOpen = AnAction.builder("window.editor.keep.tab.open", "Keep Tab Open")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::keepTabOpen)
                .build();

        AnAction closeTab = AnAction.builder("window.close.tab", "Close Tab")
                .accelerator(new KeyCodeCombination(KeyCode.F4, KeyCombination.CONTROL_DOWN))
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeActiveTab)
                .build();
        AnAction closeOtherTabs = AnAction.builder("window.close.other.tabs", "Close Other Tabs")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeOtherTabs)
                .build();
        AnAction closeAllTabs = AnAction.builder("window.close.all.tabs", "Close All Tabs")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeAllTabs)
                .build();
        AnAction closeUnmodified = AnAction.builder("window.close.unmodified.tabs", "Close Unmodified Tabs")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeUnmodifiedTabs)
                .build();
        AnAction closeAllButPinned = AnAction.builder("window.close.all.but.pinned", "Close All but Pinned")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeAllButPinnedTabs)
                .build();
        AnAction closeTabsLeft = AnAction.builder("window.close.tabs.left", "Close Tabs to the Left")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeTabsToLeft)
                .build();
        AnAction closeTabsRight = AnAction.builder("window.close.tabs.right", "Close Tabs to the Right")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeTabsToRight)
                .build();
        AnAction closeAllReadOnly = AnAction.builder("window.close.all.readonly", "Close All Read-Only")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::closeAllReadOnlyTabs)
                .build();

        ActionGroup splitChooser = new ActionGroup("window.editor.split.chooser", "Split with Chooser Navigation", true);
        AnAction splitRight = AnAction.builder("window.split.right", "Split Right")
                .icon(FontAwesomeSolid.COLUMNS, "#a9b7c6", 11)
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::splitActiveTabRight)
                .build();
        AnAction splitDown = AnAction.builder("window.split.down", "Split Down")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::splitActiveTabDown)
                .build();
        splitChooser.addAll(splitRight, splitDown);

        AnAction stretchTop = AnAction.builder("window.editor.stretch.top", "Stretch Editor to Top")
                .onAction(MainWindow::stretchEditorTop).build();
        AnAction stretchLeft = AnAction.builder("window.editor.stretch.left", "Stretch Editor to Left")
                .onAction(MainWindow::stretchEditorLeft).build();
        AnAction stretchBottom = AnAction.builder("window.editor.stretch.bottom", "Stretch Editor to Bottom")
                .onAction(MainWindow::stretchEditorBottom).build();
        AnAction stretchRight = AnAction.builder("window.editor.stretch.right", "Stretch Editor to Right")
                .onAction(MainWindow::stretchEditorRight).build();
        AnAction changeSplitter = AnAction.builder("window.editor.change.splitter.orientation", "Change Splitter Orientation")
                .onAction(MainWindow::changeSplitterOrientation).build();
        AnAction maximizeSplits = AnAction.builder("window.editor.maximize.splits", "Maximize Editor/Normalize Splits")
                .onAction(MainWindow::maximizeEditorSplits).build();
        AnAction unsplit = AnAction.builder("window.editor.unsplit", "Unsplit")
                .onAction(MainWindow::unsplitActive).build();
        AnAction unsplitAll = AnAction.builder("window.unsplit.all", "Unsplit All")
                .onAction(MainWindow::unsplitAll).build();
        AnAction gotoNextSplitter = AnAction.builder("window.editor.goto.next.splitter", "Go to Next Splitter")
                .onAction(MainWindow::goToNextSplitter).build();
        AnAction gotoPrevSplitter = AnAction.builder("window.editor.goto.prev.splitter", "Go to Previous Splitter")
                .onAction(MainWindow::goToPrevSplitter).build();

        AnAction configEditorTabs = AnAction.builder("window.editor.configure.tabs", "Configure Editor Tabs…")
                .onAction(MainWindow::showSettingsDialog)
                .build();

        editorTabsSubmenu.addAll(editorNextTab, editorPrevTab, pinTab, keepTabOpen)
                .addSeparator()
                .addAll(closeTab, closeOtherTabs, closeAllTabs, closeUnmodified, closeAllButPinned, closeTabsLeft, closeTabsRight, closeAllReadOnly)
                .addSeparator()
                .addAll(splitChooser, stretchTop, stretchLeft, stretchBottom, stretchRight, changeSplitter, maximizeSplits, unsplit, unsplitAll, gotoNextSplitter, gotoPrevSplitter)
                .addSeparator()
                .add(configEditorTabs);

        // Submenu 4: Notifications >
        ActionGroup notificationsSubmenu = new ActionGroup("window.notifications", "Notifications", true);
        notificationsSubmenu.addAll(
                AnAction.builder("window.notifications.close.first", "Close First")
                        .onAction(MainWindow::closeFirstNotification).build(),
                AnAction.builder("window.notifications.close.all", "Close All")
                        .onAction(MainWindow::closeAllNotifications).build()
        );

        // Submenu 5: Processes >
        ActionGroup processesSubmenu = new ActionGroup("window.processes", "Processes", true);
        processesSubmenu.addAll(
                AnAction.builder("window.processes.show", "Show")
                        .onAction(MainWindow::showProcesses).build(),
                AnAction.builder("window.processes.auto.show", "Auto Show")
                        .onAction(MainWindow::toggleAutoShowProcesses).build()
        );

        // Top-Level Window Menu
        AnAction nextProjectWindow = AnAction.builder("window.next.project", "Next Project Window")
                .accelerator(new KeyCodeCombination(KeyCode.CLOSE_BRACKET, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::nextProjectWindow)
                .build();
        AnAction prevProjectWindow = AnAction.builder("window.prev.project", "Previous Project Window")
                .accelerator(new KeyCodeCombination(KeyCode.OPEN_BRACKET, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::prevProjectWindow)
                .build();

        AnAction activeProjectWindow = AnAction.builder("window.project.active", "default")
                .icon(FontAwesomeSolid.CHECK, "#a9b7c6", 11)
                .onAction(MainWindow::showActiveProjectWindow)
                .build();

        ActionGroup windowMenu = new ActionGroup("menu.window", "Window");
        windowMenu.addAll(layoutsSubmenu, activeToolWindowSubmenu, editorTabsSubmenu, notificationsSubmenu, processesSubmenu)
                .addSeparator()
                .addAll(nextProjectWindow, prevProjectWindow)
                .addSeparator()
                .add(activeProjectWindow);

        // Retain reopenTab in ActionManager for Ctrl+Shift+T
        AnAction reopenTab = AnAction.builder("window.reopen.tab", "Reopen Closed Tab")
                .accelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::reopenLastClosedTab)
                .build();
        manager.registerAction(reopenTab);

        // =========================================================================
        // 8. HELP ACTIONS (Matching DataGrip - Register... excluded per user instruction)
        // =========================================================================
        // Section 1: Welcome & Find Action
        AnAction welcomeAction = AnAction.builder("help.welcome", "Welcome")
                .description("Open Welcome dialog")
                .onAction(MainWindow::showWelcomeDialog)
                .build();

        AnAction findHelpAction = AnAction.builder("help.find.action", "Find Action…")
                .description("Find action or command across IDE (Ctrl+Shift+A)")
                .accelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showFindActionDialog)
                .build();

        // Section 2: Help
        AnAction helpAction = AnAction.builder("help.help", "Help")
                .icon(FontAwesomeSolid.QUESTION_CIRCLE, "#a9b7c6", 11)
                .onAction(MainWindow::showOnlineHelp)
                .build();

        // Section 3: Tip of the Day, My Productivity, Learn IDE Features
        AnAction tipOfDay = AnAction.builder("help.tip.of.the.day", "Tip of the Day")
                .onAction(MainWindow::showTipOfTheDayDialog)
                .build();

        AnAction myProductivity = AnAction.builder("help.my.productivity", "My Productivity")
                .onAction(MainWindow::showProductivityGuideDialog)
                .build();

        AnAction learnFeatures = AnAction.builder("help.learn.features", "Learn IDE Features")
                .icon(FontAwesomeSolid.GRADUATION_CAP, "#a9b7c6", 11)
                .onAction(MainWindow::showLearnIdeFeaturesDialog)
                .build();

        // Section 4: What's New
        AnAction whatsNew = AnAction.builder("help.whats.new", "What's New in DBNavigator")
                .onAction(MainWindow::showWhatsNewDialog)
                .build();

        // Section 5: Getting Started, YouTube, Shortcuts PDF
        AnAction gettingStarted = AnAction.builder("help.getting.started", "Getting Started")
                .onAction(MainWindow::showGettingStartedDialog)
                .build();

        AnAction youtube = AnAction.builder("help.youtube", "DataGrip on YouTube")
                .onAction(MainWindow::showYouTubeChannel)
                .build();

        AnAction shortcutsPdf = AnAction.builder("help.shortcuts.pdf", "Keyboard Shortcuts PDF")
                .onAction(MainWindow::showKeyboardShortcutsPdf)
                .build();

        // Section 6: Support, Bug Report, Feedback
        AnAction contactSupport = AnAction.builder("help.contact.support", "Contact Support…")
                .onAction(MainWindow::showContactSupportDialog)
                .build();

        AnAction bugReport = AnAction.builder("help.bug.report", "Submit a Bug Report…")
                .onAction(MainWindow::showSubmitBugReportDialog)
                .build();

        AnAction submitFeedback = AnAction.builder("help.submit.feedback", "Submit Feedback…")
                .onAction(MainWindow::showSubmitFeedbackDialog)
                .build();

        // Section 7: Logs, Profiling, Memory Snapshot, Diagnostic Tools >
        AnAction showLogInFiles = AnAction.builder("help.show.log.in.files", "Show Log in Files")
                .onAction(MainWindow::showLogInFiles)
                .build();

        AnAction showSqlLogInFiles = AnAction.builder("help.show.sql.log.in.files", "Show SQL Log in Files")
                .onAction(MainWindow::showSqlLogInFiles)
                .build();

        AnAction collectLogs = AnAction.builder("help.collect.logs", "Collect Logs and Diagnostic Data")
                .onAction(MainWindow::showCollectLogsDialog)
                .build();

        AnAction cpuProfiling = AnAction.builder("help.cpu.profiling", "Start CPU Usage Profiling")
                .onAction(MainWindow::startCpuProfiling)
                .build();

        AnAction captureMemory = AnAction.builder("help.capture.memory.snapshot", "Capture Memory Snapshot")
                .icon(FontAwesomeSolid.CAMERA, "#a9b7c6", 11)
                .onAction(MainWindow::captureMemorySnapshot)
                .build();

        // Submenu: Diagnostic Tools >
        ActionGroup diagnosticTools = new ActionGroup("help.diagnostic.tools", "Diagnostic Tools", true);
        diagnosticTools.addAll(
                AnAction.builder("help.diagnostic.activity.monitor", "Activity Monitor…")
                        .onAction(MainWindow::showActivityMonitorDialog).build(),
                AnAction.builder("help.diagnostic.dump.threads", "Dump Threads")
                        .onAction(MainWindow::dumpThreads).build(),
                AnAction.builder("help.diagnostic.debug.log.settings", "Debug Log Settings…")
                        .onAction(MainWindow::showDebugLogSettingsDialog).build(),
                AnAction.builder("help.diagnostic.special.files", "Special Files and Folders…")
                        .onAction(MainWindow::showSpecialFilesFoldersDialog).build(),
                AnAction.builder("help.diagnostic.start.cpu.profiling", "Start CPU Usage Profiling")
                        .onAction(MainWindow::startCpuProfiling).build(),
                AnAction.builder("help.diagnostic.start.async.profiler", "Start Async profiler")
                        .onAction(MainWindow::startAsyncProfiler).build(),
                AnAction.builder("help.diagnostic.capture.memory.snapshot", "Capture Memory Snapshot")
                        .icon(FontAwesomeSolid.CAMERA, "#a9b7c6", 11)
                        .onAction(MainWindow::captureMemorySnapshot).build(),
                AnAction.builder("help.diagnostic.profile.indexing", "Profile Indexing")
                        .onAction(MainWindow::profileIndexing).build(),
                AnAction.builder("help.diagnostic.open.indexing.diagnostics", "Open Indexing Diagnostics")
                        .onAction(MainWindow::openIndexingDiagnostics).build()
        );

        // Section 8: Memory, Custom Properties & VM Options, Delete Leftover Dirs
        AnAction changeMemory = AnAction.builder("help.change.memory.settings", "Change Memory Settings")
                .onAction(MainWindow::showChangeMemorySettingsDialog)
                .build();

        AnAction customProperties = AnAction.builder("help.custom.properties", "Edit Custom Properties…")
                .onAction(MainWindow::editCustomProperties)
                .build();

        AnAction customVmOptions = AnAction.builder("help.custom.vm.options", "Edit Custom VM Options…")
                .onAction(MainWindow::editCustomVmOptions)
                .build();

        AnAction deleteLeftoverDirs = AnAction.builder("help.delete.leftover.dirs", "Delete Leftover IDE Directories…")
                .onAction(MainWindow::deleteLeftoverDirectories)
                .build();

        // Section 9: Updates & About (Register... is excluded per user instruction)
        AnAction checkUpdates = AnAction.builder("help.updates", "Check for Updates…")
                .icon(FontAwesomeSolid.DOWNLOAD, "#a9b7c6", 11)
                .onAction(MainWindow::showCheckUpdatesDialog)
                .build();

        AnAction about = AnAction.builder("help.about", "About DBNavigator Pro")
                .icon(FontAwesomeSolid.INFO_CIRCLE, "#a9b7c6", 11)
                .onAction(MainWindow::showAboutDialog)
                .build();

        ActionGroup helpMenu = new ActionGroup("menu.help", "Help");
        helpMenu.addAll(welcomeAction, findHelpAction)
                .addSeparator()
                .add(helpAction)
                .addSeparator()
                .addAll(tipOfDay, myProductivity, learnFeatures)
                .addSeparator()
                .add(whatsNew)
                .addSeparator()
                .addAll(gettingStarted, youtube, shortcutsPdf)
                .addSeparator()
                .addAll(contactSupport, bugReport, submitFeedback)
                .addSeparator()
                .addAll(showLogInFiles, showSqlLogInFiles, collectLogs, cpuProfiling, captureMemory, diagnosticTools)
                .addSeparator()
                .addAll(changeMemory, customProperties, customVmOptions, deleteLeftoverDirs)
                .addSeparator()
                .addAll(checkUpdates, about);

        // =========================================================================
        // 9. MIDDLE HEADER ACTIONS (4 ICONS + THREE DOTS ...)
        // =========================================================================
        // Icon 1: Database
        AnAction middleDatabase = AnAction.builder("middle.database", "Database Explorer")
                .description("Database Explorer / Data Sources (Alt+1)")
                .icon(FontAwesomeSolid.DATABASE, "#4a88c7", 13)
                .onAction(MainWindow::focusOrToggleSchemaExplorer)
                .build();

        // Icon 2: Run / Execute
        AnAction middleRun = AnAction.builder("middle.run", "Execute")
                .description("Execute Statement (Ctrl+Enter)")
                .icon(FontAwesomeSolid.PLAY, "#57965c", 13)
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::executeCurrentStatement)
                .build();

        // Icon 3: Console / Terminal
        AnAction middleConsole = AnAction.builder("middle.console", "New Query Console")
                .description("New Query Console (Ctrl+Shift+N)")
                .icon(FontAwesomeSolid.TERMINAL, "#6897bb", 13)
                .onAction(MainWindow::openConsoleForSelectedConnection)
                .build();

        // Icon 4: Folder / Open
        AnAction middleFolder = AnAction.builder("middle.folder", "Open SQL File")
                .description("Open SQL File in Console (Ctrl+O)")
                .icon(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 13)
                .onAction(MainWindow::openSqlFile)
                .build();

        // Icon 5: Three dots "..." More Actions dropdown
        ActionGroup moreActionsGroup = new ActionGroup("middle.more", "More Actions", true,
                FontAwesomeSolid.ELLIPSIS_H, "#a9b7c6", 13, "More Actions");
        moreActionsGroup.add(invalidateCaches)
                .add(localHistoryGroup)
                .addSeparator()
                .add(formatCode)
                .add(searchEverywhere)
                .add(refreshExplorer)
                .addSeparator()
                .add(settings);

        ActionGroup middleGroup = new ActionGroup("group.middle", "Center Actions");
        middleGroup.addAll(middleDatabase, middleRun, middleConsole, middleFolder, moreActionsGroup);

        // Register all groups
        manager.registerGroup(fileMenu);
        manager.registerGroup(editMenu);
        manager.registerGroup(viewMenu);
        manager.registerGroup(navigateMenu);
        manager.registerGroup(runMenu);
        manager.registerGroup(vcsMenu);
        manager.registerGroup(windowMenu);
        manager.registerGroup(helpMenu);
        manager.registerGroup(middleGroup);
    }

    public static List<ActionGroup> getMainMenuBarGroups(ActionManager manager) {
        return List.of(
                manager.getGroup("menu.file"),
                manager.getGroup("menu.edit"),
                manager.getGroup("menu.view"),
                manager.getGroup("menu.navigate"),
                manager.getGroup("menu.run"),
                manager.getGroup("menu.vcs"),
                manager.getGroup("menu.window"),
                manager.getGroup("menu.help")
        );
    }
}

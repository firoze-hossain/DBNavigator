package com.roze.dbnavigator.ui.action;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.ui.MainWindow;
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
        // 3. VIEW ACTIONS
        // =========================================================================
        AnAction refreshExplorer = AnAction.builder("view.refresh.explorer", "Refresh Database Explorer")
                .description("Reload connection metadata schemas")
                .icon(FontAwesomeSolid.SYNC_ALT, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F5, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::refreshSchemaExplorer)
                .build();

        AnAction toggleRunPanel = AnAction.builder("view.toggle.run", "Run Tool Window")
                .description("Show or hide the bottom Run tool window")
                .icon(FontAwesomeSolid.TERMINAL, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleRunPanel)
                .build();

        ActionGroup viewMenu = new ActionGroup("menu.view", "View");
        viewMenu.addAll(refreshExplorer, toggleRunPanel);

        // =========================================================================
        // 4. NAVIGATE ACTIONS
        // =========================================================================
        AnAction searchEverywhere = AnAction.builder("navigate.search.everywhere", "Search Everywhere…")
                .description("Search tables, views, columns, connections, and actions")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchEverywhere)
                .build();

        AnAction nextEditorTab = AnAction.builder("navigate.next.tab", "Select Next Tab")
                .accelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::selectNextTab)
                .build();

        AnAction prevEditorTab = AnAction.builder("navigate.prev.tab", "Select Previous Tab")
                .accelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::selectPreviousTab)
                .build();

        ActionGroup navigateMenu = new ActionGroup("menu.navigate", "Navigate");
        navigateMenu.add(searchEverywhere)
                .addSeparator()
                .addAll(nextEditorTab, prevEditorTab);

        // =========================================================================
        // 5. RUN ACTIONS
        // =========================================================================
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

        ActionGroup runMenu = new ActionGroup("menu.run", "Run");
        runMenu.add(runStatement)
                .addSeparator()
                .add(runToolWindow);

        // =========================================================================
        // 6. VCS ACTIONS
        // =========================================================================
        ActionGroup vcsMenu = new ActionGroup("menu.vcs", "VCS");
        vcsMenu.add(localHistoryGroup);

        // =========================================================================
        // 7. WINDOW ACTIONS
        // =========================================================================
        AnAction closeTab = AnAction.builder("window.close.tab", "Close Active Tab")
                .accelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN))
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

        AnAction reopenTab = AnAction.builder("window.reopen.tab", "Reopen Closed Tab")
                .accelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::reopenLastClosedTab)
                .build();

        AnAction splitRight = AnAction.builder("window.split.right", "Split Right")
                .icon(FontAwesomeSolid.COLUMNS, "#a9b7c6", 11)
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::splitActiveTabRight)
                .build();

        AnAction splitDown = AnAction.builder("window.split.down", "Split Down")
                .enabledWhen(MainWindow::hasActiveTab)
                .onAction(MainWindow::splitActiveTabDown)
                .build();

        AnAction unsplitAll = AnAction.builder("window.unsplit.all", "Unsplit All")
                .onAction(MainWindow::unsplitAll)
                .build();

        ActionGroup windowMenu = new ActionGroup("menu.window", "Window");
        windowMenu.addAll(nextEditorTab, prevEditorTab)
                .addSeparator()
                .addAll(closeTab, closeOtherTabs, closeAllTabs, reopenTab)
                .addSeparator()
                .addAll(splitRight, splitDown, unsplitAll);

        // =========================================================================
        // 8. HELP ACTIONS
        // =========================================================================
        AnAction checkUpdates = AnAction.builder("help.updates", "Check for Updates…")
                .icon(FontAwesomeSolid.DOWNLOAD, "#a9b7c6", 11)
                .onAction(MainWindow::showCheckUpdatesDialog)
                .build();

        AnAction about = AnAction.builder("help.about", "About DBNavigator Pro")
                .icon(FontAwesomeSolid.INFO_CIRCLE, "#a9b7c6", 11)
                .onAction(MainWindow::showAboutDialog)
                .build();

        ActionGroup helpMenu = new ActionGroup("menu.help", "Help");
        helpMenu.add(checkUpdates)
                .addSeparator()
                .add(about);

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

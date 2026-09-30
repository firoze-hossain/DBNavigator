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

        // DataGrip aligned File Menu actions & groups registration
        manager.registerAction(AnAction.builder("file.open.actions", "File Open Actions").onAction(ctx -> {}).build());
        manager.registerAction(AnAction.builder("file.new.generic.file", "File")
                .icon(FontAwesomeSolid.FILE, "#a9b7c6", 11).onAction(MainWindow::openNewSqlFile).build());
        manager.registerAction(AnAction.builder("file.new.directory.package", "Directory/Package")
                .icon(FontAwesomeSolid.FOLDER, "#e0a44c", 11).onAction(MainWindow::createNewFolderDialog).build());
        manager.registerAction(AnAction.builder("file.template.separator.group", "FileTemplateSeparatorGroup").onAction(ctx -> {}).build());
        manager.registerAction(AnAction.builder("file.new.html", "HTML File")
                .icon(FontAwesomeSolid.FILE_CODE, "#e0a44c", 11).onAction(ctx -> ctx.setStatus("Created HTML File")).build());
        manager.registerAction(AnAction.builder("file.microservices.templates", "Microservices Templates").onAction(ctx -> ctx.setStatus("Microservices Templates")).build());
        manager.registerAction(AnAction.builder("file.from.template", "From Template").onAction(ctx -> ctx.setStatus("From Template")).build());
        manager.registerAction(AnAction.builder("file.xml.config.file", "XML Configuration File")
                .icon(FontAwesomeSolid.CODE, "#a9b7c6", 11).onAction(ctx -> ctx.setStatus("XML Configuration File")).build());
        manager.registerAction(AnAction.builder("file.new.queryfile.active", "Query File")
                .icon(FontAwesomeSolid.TERMINAL, "#6897bb", 11).onAction(MainWindow::openNewQueryFile).build());
        manager.registerAction(AnAction.builder("file.new.scratch.queryfile", "Scratch Query File")
                .icon(FontAwesomeSolid.TERMINAL, "#6897bb", 11).onAction(MainWindow::openNewScratchFile).build());
        manager.registerAction(AnAction.builder("file.new.add.ddl.object", "Add Ddl Object")
                .icon(FontAwesomeSolid.DATABASE, "#57965c", 11).onAction(MainWindow::openDdlDataSourceDialog).build());
        manager.registerAction(AnAction.builder("file.new.datasource.selection", "Add Data Source from Selection…")
                .onAction(ctx -> ctx.setStatus("Add Data Source from Selection")).build());
        manager.registerAction(AnAction.builder("file.new.datasource.path", "Data Source in Path")
                .onAction(ctx -> ctx.setStatus("Data Source in Path")).build());
        manager.registerAction(AnAction.builder("file.new.datasource.clipboard", "Import from Clipboard")
                .onAction(ctx -> ctx.setStatus("Import from Clipboard")).build());
        manager.registerAction(AnAction.builder("file.reopen.project", "Reopen Project")
                .onAction(ctx -> ctx.setStatus("Reopen Project")).build());
        manager.registerAction(AnAction.builder("file.manage.projects", "Manage Projects…")
                .onAction(ctx -> ctx.setStatus("Manage Projects")).build());
        manager.registerAction(AnAction.builder("file.close.project", "Close Project")
                .onAction(ctx -> ctx.setStatus("Close Project")).build());
        manager.registerAction(AnAction.builder("file.remote.dev", "Remote Development")
                .icon(FontAwesomeSolid.DESKTOP, "#4a88c7", 11).onAction(ctx -> ctx.setStatus("Remote Development")).build());
        manager.registerAction(AnAction.builder("file.remote.dev.daemon.action", "Remote Development…")
                .onAction(ctx -> ctx.setStatus("Remote Development Daemon")).build());
        manager.registerAction(AnAction.builder("file.project.structure", "Project Structure…")
                .onAction(ctx -> ctx.setStatus("Project Structure")).build());
        manager.registerAction(AnAction.builder("file.remove.bom", "Remove BOM")
                .onAction(ctx -> ctx.setStatus("Remove BOM")).build());
        manager.registerAction(AnAction.builder("file.add.bom", "Add BOM")
                .onAction(ctx -> ctx.setStatus("Add BOM")).build());
        manager.registerAction(AnAction.builder("file.associate.file.type", "Associate with File Type…")
                .onAction(ctx -> ctx.setStatus("Associate with File Type")).build());
        manager.registerAction(AnAction.builder("file.change.template.lang", "Change Template Data Language")
                .onAction(ctx -> ctx.setStatus("Change Template Data Language")).build());
        manager.registerAction(AnAction.builder("file.toggle.readonly", "Toggle Read-Only Attribute")
                .onAction(ctx -> ctx.setStatus("Toggle Read-Only Attribute")).build());
        manager.registerAction(AnAction.builder("file.line.sep.crlf", "CRLF - Windows (\r\n)")
                .onAction(ctx -> ctx.setStatus("Line Separator: CRLF")).build());
        manager.registerAction(AnAction.builder("file.line.sep.lf", "LF - Unix and macOS (\n)")
                .onAction(ctx -> ctx.setStatus("Line Separator: LF")).build());
        manager.registerAction(AnAction.builder("file.line.sep.cr", "CR - Classic Mac OS (\r)")
                .onAction(ctx -> ctx.setStatus("Line Separator: CR")).build());
        manager.registerAction(AnAction.builder("file.settings.import", "Import Settings…")
                .onAction(ctx -> ctx.setStatus("Import Settings")).build());
        manager.registerAction(AnAction.builder("file.settings.backup.sync", "Backup and Sync…")
                .onAction(ctx -> ctx.setStatus("Backup and Sync")).build());
        manager.registerAction(AnAction.builder("file.export.html", "Export Files or Selection to HTML…")
                .onAction(ctx -> ctx.setStatus("Export to HTML")).build());

        // Groups for File Menu hierarchy
        manager.registerGroup(new ActionGroup("file.new.file.group", "New File", true));
        manager.registerGroup(new ActionGroup("file.web.dev.templates", "Web Development Templates", true));
        manager.registerGroup(new ActionGroup("file.web.dev.xml", "XML", true));
        manager.registerGroup(new ActionGroup("file.new.db.group", "New", false));
        manager.registerGroup(new ActionGroup("file.new.add.group", "Add", false));
        manager.registerGroup(new ActionGroup("file.new.create.datasource", "Create Data Source", true));
        manager.registerGroup(new ActionGroup("file.remote.dev.actions", "FileMenu.RemoteDevelopmentActions", false));
        manager.registerGroup(new ActionGroup("file.remote.dev.daemon", "FileMenu.RemoteDevelopmentActions.Daemon", false));
        manager.registerGroup(new ActionGroup("file.settings.actions", "Settings Actions", false));
        manager.registerGroup(new ActionGroup("file.remove.bom.group", "RemoveBom.Group", false));
        manager.registerGroup(new ActionGroup("file.add.bom.group", "AddBom.Group", false));
        manager.registerGroup(new ActionGroup("file.line.separators", "Line Separators", true));
        manager.registerGroup(new ActionGroup("file.local.history.main.group", "LocalHistory.MainMenuGroup", false));
        manager.registerGroup(new ActionGroup("file.print.export.actions", "Print/Export Actions", false));

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

        // Additional DataGrip Edit Actions & Groups (Images 1, 2, 3)
        AnAction copyPaths = AnAction.builder("edit.copy.paths", "Copy Paths")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyPathOrReferenceCurrentEditor)
                .build();
        AnAction copyRich = AnAction.builder("edit.copy.rich", "Copy as Rich Text")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyCurrentEditor)
                .build();
        AnAction copyJsonPointer = AnAction.builder("edit.copy.json.pointer", "Copy JSON Pointer")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(ctx -> ctx.setStatus("JSON Pointer copied"))
                .build();
        AnAction copyPathAbs = AnAction.builder("edit.copy.path.absolute", "Absolute Path")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyPathOrReferenceCurrentEditor)
                .build();
        AnAction copyPathFilename = AnAction.builder("edit.copy.path.filename", "File Name")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyPathOrReferenceCurrentEditor)
                .build();
        AnAction copyPathLineNumber = AnAction.builder("edit.copy.path.line.number", "Path with Line Number")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyPathOrReferenceCurrentEditor)
                .build();
        AnAction copyPathContentRoot = AnAction.builder("edit.copy.path.content.root", "Path from Content Root")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyPathOrReferenceCurrentEditor)
                .build();
        AnAction copyPathSourceRoot = AnAction.builder("edit.copy.path.source.root", "Path from Source Root")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyPathOrReferenceCurrentEditor)
                .build();
        AnAction copyPathRepoRoot = AnAction.builder("edit.copy.path.repo.root", "Path From Repository Root")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::copyPathOrReferenceCurrentEditor)
                .build();
        AnAction copyGitHostingLink = AnAction.builder("edit.copy.git.hosting.link", "Git.Hosting.Copy.Link.Group")
                .onAction(ctx -> ctx.setStatus("Git Hosting Link copied"))
                .build();
        AnAction copyToolboxUrl = AnAction.builder("edit.copy.toolbox.url", "Toolbox URL")
                .icon(FontAwesomeSolid.CUBES, "#4a88c7", 11)
                .onAction(ctx -> ctx.setStatus("Toolbox URL copied"))
                .build();
        AnAction pasteHistory = AnAction.builder("edit.paste.history", "Paste from History…")
                .accelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::showRecentLocationsDialog)
                .build();
        AnAction pastePlain = AnAction.builder("edit.paste.plain", "Paste as Plain Text")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::pasteCurrentEditor)
                .build();

        ActionGroup copyFileRefGroup = new ActionGroup("edit.copy.file.reference.group", "CopyFileReference", false);
        copyFileRefGroup.addAll(copyPathAbs, copyPathFilename)
                .addSeparator()
                .addAll(copyPathLineNumber, copyPathContentRoot, copyPathSourceRoot, copyPathRepoRoot, copyGitHostingLink);
        ActionGroup copyExtRefGroup = new ActionGroup("edit.copy.external.reference.group", "CopyExternalReferenceGroup", false);
        copyExtRefGroup.add(copyToolboxUrl);
        ActionGroup copyPathRefGroup = new ActionGroup("edit.copy.path.reference.group", "Copy Path/Reference…", true);
        copyPathRefGroup.add(copyFileRefGroup).addSeparator().add(copyExtRefGroup).add(copyRef);
        ActionGroup pasteGroup = new ActionGroup("edit.paste.group", "Paste", false);
        pasteGroup.addAll(paste, pasteHistory, pastePlain);
        ActionGroup cutCopyPasteActions = new ActionGroup("edit.cut.copy.paste.actions", "Cut/Copy/Paste Actions", false);
        cutCopyPasteActions.addAll(cut, copy, copyPaths, copyPlain, copyRich, copyPathRefGroup, pasteGroup, copyJsonPointer);

        manager.registerAction(copyPaths);
        manager.registerAction(copyRich);
        manager.registerAction(copyJsonPointer);
        manager.registerAction(copyPathAbs);
        manager.registerAction(copyPathFilename);
        manager.registerAction(copyPathLineNumber);
        manager.registerAction(copyPathContentRoot);
        manager.registerAction(copyPathSourceRoot);
        manager.registerAction(copyPathRepoRoot);
        manager.registerAction(copyGitHostingLink);
        manager.registerAction(copyToolboxUrl);
        manager.registerAction(pasteHistory);
        manager.registerAction(pastePlain);
        manager.registerGroup(copyFileRefGroup);
        manager.registerGroup(copyExtRefGroup);
        manager.registerGroup(copyPathRefGroup);
        manager.registerGroup(pasteGroup);
        manager.registerGroup(cutCopyPasteActions);

        // DataGrip Generate Actions & Groups
        AnAction mdLink = AnAction.builder("edit.generate.markdown.link", "Create Link")
                .icon(FontAwesomeSolid.LINK, "#4a88c7", 11)
                .onAction(ctx -> ctx.setStatus("Create Link"))
                .build();
        AnAction mdTable = AnAction.builder("edit.generate.markdown.table", "Insert Table")
                .icon(FontAwesomeSolid.TABLE, "#57965c", 11)
                .onAction(ctx -> ctx.setStatus("Insert Table"))
                .build();
        AnAction mdImage = AnAction.builder("edit.generate.markdown.image", "Insert Image")
                .icon(FontAwesomeSolid.IMAGE, "#e0a44c", 11)
                .onAction(ctx -> ctx.setStatus("Insert Image"))
                .build();
        AnAction mdToc = AnAction.builder("edit.generate.markdown.toc", "Generate Table Of Contents")
                .icon(FontAwesomeSolid.LIST, "#c77dbb", 11)
                .onAction(ctx -> ctx.setStatus("Generate Table Of Contents"))
                .build();
        ActionGroup mdInsertGroup = new ActionGroup("edit.generate.markdown.group", "Markdown.InsertGroup", true);
        mdInsertGroup.addAll(mdLink, mdTable, mdImage, mdToc);

        AnAction genSqlGroup = AnAction.builder("edit.generate.sql.group", "SqlGenerateGroup")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::generateSqlSnippet)
                .build();
        AnAction genXmlTag = AnAction.builder("edit.generate.xml.tag", "XML Tag…")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::insertLiveTemplate)
                .build();
        AnAction genOverride = AnAction.builder("edit.generate.override.methods", "Override Methods…")
                .onAction(ctx -> ctx.setStatus("Override Methods…"))
                .build();
        AnAction genImplement = AnAction.builder("edit.generate.implement.methods", "Implement Methods…")
                .onAction(ctx -> ctx.setStatus("Implement Methods…"))
                .build();
        AnAction genDelegate = AnAction.builder("edit.generate.delegate.methods", "Delegate Methods…")
                .onAction(ctx -> ctx.setStatus("Delegate Methods…"))
                .build();
        AnAction genTestCreators = AnAction.builder("edit.generate.test.creators.group", "GenerateFromTestCreatorsGroup")
                .onAction(ctx -> ctx.setStatus("GenerateFromTestCreatorsGroup"))
                .build();
        ActionGroup generateInnerGroup = new ActionGroup("edit.generate.group", "Generate", false);
        generateInnerGroup.addAll(genSqlGroup, genXmlTag, genOverride, genImplement, genDelegate, genTestCreators, mdInsertGroup);
        ActionGroup generateRootGroup = new ActionGroup("edit.generate.root.group", "Generate…", true);
        generateRootGroup.add(generateInnerGroup);

        manager.registerAction(mdLink);
        manager.registerAction(mdTable);
        manager.registerAction(mdImage);
        manager.registerAction(mdToc);
        manager.registerAction(genSqlGroup);
        manager.registerAction(genXmlTag);
        manager.registerAction(genOverride);
        manager.registerAction(genImplement);
        manager.registerAction(genDelegate);
        manager.registerAction(genTestCreators);
        manager.registerGroup(mdInsertGroup);
        manager.registerGroup(generateInnerGroup);
        manager.registerGroup(generateRootGroup);

        // DataGrip Refactor Actions & Groups
        AnAction refactorExpandCols = AnAction.builder("edit.refactor.expand.column.list", "Expand Column List")
                .onAction(ctx -> ctx.setStatus("Refactor: Expand Column List"))
                .build();
        AnAction refactorSubquery = AnAction.builder("edit.refactor.convert.subquery", "Convert to Subquery")
                .onAction(ctx -> ctx.setStatus("Refactor: Convert to Subquery"))
                .build();
        AnAction refactorSubqueryCte = AnAction.builder("edit.refactor.subquery.cte", "Subquery as CTE")
                .onAction(ctx -> ctx.setStatus("Refactor: Subquery as CTE"))
                .build();
        AnAction extractTableAlias = AnAction.builder("edit.refactor.table.alias", "Table alias…")
                .onAction(ctx -> ctx.setStatus("Refactor: Table alias…"))
                .build();
        AnAction extractVariable = AnAction.builder("edit.refactor.introduce.variable", "Introduce Variable…")
                .onAction(ctx -> ctx.setStatus("Refactor: Introduce Variable…"))
                .build();
        AnAction extractRoutine = AnAction.builder("edit.refactor.extract.routine", "Extract Routine…")
                .onAction(ctx -> ctx.setStatus("Refactor: Extract Routine…"))
                .build();
        ActionGroup extractIntroduceGroup = new ActionGroup("edit.refactor.extract.introduce.group", "Extract/Introduce", true);
        extractIntroduceGroup.addAll(extractTableAlias, extractVariable, extractRoutine);
        AnAction qualifyId = AnAction.builder("edit.refactor.qualify.identifier", "Qualify Identifier")
                .onAction(ctx -> ctx.setStatus("Refactor: Qualify Identifier"))
                .build();
        AnAction unqualifyId = AnAction.builder("edit.refactor.unqualify.identifier", "Unqualify Identifier")
                .onAction(ctx -> ctx.setStatus("Refactor: Unqualify Identifier"))
                .build();
        AnAction quoteId = AnAction.builder("edit.refactor.quote.identifier", "Quote Identifier")
                .onAction(ctx -> ctx.setStatus("Refactor: Quote Identifier"))
                .build();
        AnAction unquoteId = AnAction.builder("edit.refactor.unquote.identifier", "Unquote Identifier")
                .onAction(ctx -> ctx.setStatus("Refactor: Unquote Identifier"))
                .build();
        AnAction flipExpr = AnAction.builder("edit.refactor.flip.expression", "Flip Expression")
                .onAction(ctx -> ctx.setStatus("Refactor: Flip Expression"))
                .build();
        AnAction injectLang = AnAction.builder("edit.refactor.inject.language", "Inject Language or Reference")
                .onAction(ctx -> ctx.setStatus("Refactor: Inject Language"))
                .build();
        AnAction uninjectLang = AnAction.builder("edit.refactor.uninject.language", "Uninject Language or Reference")
                .onAction(ctx -> ctx.setStatus("Refactor: Uninject Language"))
                .build();

        manager.registerAction(refactorExpandCols);
        manager.registerAction(refactorSubquery);
        manager.registerAction(refactorSubqueryCte);
        manager.registerAction(extractTableAlias);
        manager.registerAction(extractVariable);
        manager.registerAction(extractRoutine);
        manager.registerGroup(extractIntroduceGroup);
        manager.registerAction(qualifyId);
        manager.registerAction(unqualifyId);
        manager.registerAction(quoteId);
        manager.registerAction(unquoteId);
        manager.registerAction(flipExpr);
        manager.registerAction(injectLang);
        manager.registerAction(uninjectLang);

        // DataGrip Selection Actions & Groups
        AnAction columnSelectionMode = AnAction.builder("edit.selection.column.mode", "Column Selection Mode")
                .onAction(ctx -> ctx.setStatus("Column Selection Mode"))
                .build();
        AnAction addCaretsEnds = AnAction.builder("edit.selection.add.carets.ends", "Add Carets to Ends of Selected Lines")
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(ctx -> ctx.setStatus("Add Carets to Ends of Selected Lines"))
                .build();
        ActionGroup selectWordGroup = new ActionGroup("edit.selection.word.actions", "Select Word Actions", false);
        selectWordGroup.addAll(extendSelection, shrinkSelection);
        ActionGroup editorSelectActions = new ActionGroup("edit.selection.editor.select.actions", "Editor Select Actions", false);
        editorSelectActions.addAll(selectAll, addCaretsEnds, selectWordGroup);

        manager.registerAction(columnSelectionMode);
        manager.registerAction(addCaretsEnds);
        manager.registerGroup(selectWordGroup);
        manager.registerGroup(editorSelectActions);

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
        manager.registerGroup(toolWindowsGroup);

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
        manager.registerGroup(mainMenuPlacementGroup);

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
        manager.registerGroup(navBarGroup);

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
        manager.registerGroup(sbWidgetsGroup);

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
        manager.registerGroup(appearanceGroup);

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

        // Additional DataGrip View Actions & Groups (Images 4, 5)
        AnAction twFilesDataGrip = AnAction.builder("view.tool.files", "Files")
                .icon(FontAwesomeSolid.FOLDER, "#e0a44c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::toggleFilesToolWindow)
                .build();
        AnAction twTerminalDataGrip = AnAction.builder("view.tool.terminal", "Terminal")
                .icon(FontAwesomeSolid.TERMINAL, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F12, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::showTerminalToolWindow)
                .build();
        ActionGroup dgToolWindows = new ActionGroup("view.tool.windows", "Tool Windows", true);
        dgToolWindows.addAll(twDatabase, twFilesDataGrip, twTerminalDataGrip);
        manager.registerAction(twFilesDataGrip);
        manager.registerAction(twTerminalDataGrip);
        manager.registerGroup(dgToolWindows);

        AnAction dgTogglePres = AnAction.builder("view.toggle.presentation.mode", "Toggle Presentation Mode")
                .onAction(MainWindow::togglePresentationMode).build();
        AnAction dgToggleDistract = AnAction.builder("view.toggle.distraction.free.mode", "Toggle Distraction Free Mode")
                .onAction(MainWindow::toggleDistractionFreeMode).build();
        AnAction dgToggleFullScreen = AnAction.builder("view.toggle.fullscreen.mode", "Toggle Full Screen Mode")
                .onAction(MainWindow::toggleFullScreen).build();
        AnAction dgToggleZen = AnAction.builder("view.toggle.zen.mode", "Toggle Zen Mode")
                .onAction(MainWindow::toggleZenMode).build();
        AnAction dgCompact = AnAction.builder("view.compact.mode", "Compact Mode")
                .onAction(MainWindow::toggleCompactMode).build();
        ActionGroup dgFullScreenGroup = new ActionGroup("view.toggle.fullscreen.group", "ToggleFullScreenGroup", false);
        dgFullScreenGroup.addSeparator()
                .addAll(dgTogglePres, dgToggleDistract, dgToggleFullScreen, dgToggleZen)
                .addSeparator()
                .add(dgCompact);
        manager.registerAction(dgTogglePres);
        manager.registerAction(dgToggleDistract);
        manager.registerAction(dgToggleFullScreen);
        manager.registerAction(dgToggleZen);
        manager.registerAction(dgCompact);
        manager.registerGroup(dgFullScreenGroup);

        AnAction dgZoomIde = AnAction.builder("view.zoom.ide", "Zoom IDE")
                .onAction(MainWindow::showZoomIdeDialog).build();
        manager.registerAction(dgZoomIde);

        AnAction dgPresAssistant = AnAction.builder("view.toggle.presentation.assistant", "Presentation Assistant")
                .onAction(MainWindow::togglePresentationAssistant).build();
        AnAction dgToggleMainMenu = AnAction.builder("view.toggle.main.menu", "Main Menu")
                .onAction(ctx -> ctx.setMainMenuPlacement(MainWindow.MainMenuPlacement.HAMBURGER)).build();
        AnAction dgToggleMainMenuSep = AnAction.builder("view.toggle.main.menu.separate", "Main Menu")
                .onAction(ctx -> ctx.setMainMenuPlacement(MainWindow.MainMenuPlacement.ABOVE_TOOLBAR)).build();
        AnAction dgToggleToolbar = AnAction.builder("view.toggle.toolbar", "Toolbar")
                .onAction(ctx -> ctx.setToolbarVisible(!ctx.isToolbarVisible())).build();
        AnAction dgToggleToolbarClassic = AnAction.builder("view.toggle.toolbar.classic", "Toolbar Classic")
                .onAction(ctx -> ctx.setToolbarVisible(!ctx.isToolbarVisible())).build();
        AnAction dgToggleNavBar = AnAction.builder("view.toggle.navigation.bar", "Navigation Bar")
                .onAction(ctx -> ctx.setNavigationBarPlacement(MainWindow.NavigationBarPlacement.TOP)).build();

        AnAction dgTbToolbar = AnAction.builder("view.toolbar.actions.toolbar", "Toolbar")
                .onAction(ctx -> ctx.setToolbarVisible(!ctx.isToolbarVisible())).build();
        AnAction dgTbNavbar = AnAction.builder("view.toolbar.actions.navbar", "Navigation Bar")
                .onAction(ctx -> ctx.setNavigationBarPlacement(MainWindow.NavigationBarPlacement.TOP)).build();
        ActionGroup dgTbNavbarGroup = new ActionGroup("view.toolbar.actions.navbar.group", "Navigation Bar", true);
        AnAction dgToggleTwBars = AnAction.builder("view.toggle.tool.window.bars", "Tool Window Bars")
                .onAction(ctx -> ctx.setToolWindowBarsVisible(!ctx.isToolWindowBarsVisible())).build();
        AnAction dgToggleStatusBar = AnAction.builder("view.toggle.status.bar", "Status Bar")
                .onAction(ctx -> ctx.setStatusBarVisible(!ctx.isStatusBarVisible())).build();
        ActionGroup dgSbWidgetsGroup = new ActionGroup("view.status.bar.widgets", "Status Bar Widgets", true);
        AnAction dgSbStatusText = AnAction.builder("view.status.bar.widget.status.text", "Status Text")
                .onAction(ctx -> ctx.setStatusBarWidgetActive("statusText", !ctx.isStatusBarWidgetActive("statusText"))).build();
        dgSbWidgetsGroup.add(dgSbStatusText);
        AnAction dgToggleMembers = AnAction.builder("view.toggle.members.in.nav.bar", "Members in Navigation Bar")
                .onAction(ctx -> ctx.setStatus("Members in Navigation Bar")).build();

        ActionGroup dgViewToolbarActions = new ActionGroup("view.toolbar.actions.group", "ViewToolbarActionsGroup", false);
        dgViewToolbarActions.addAll(dgTbToolbar, dgTbNavbar, dgTbNavbarGroup, dgToggleTwBars, dgToggleStatusBar, dgSbWidgetsGroup, dgToggleMembers);

        ActionGroup dgUiToggleActions = new ActionGroup("view.ui.toggle.actions", "UIToggleActions", false);
        dgUiToggleActions.addSeparator()
                .add(dgPresAssistant)
                .addSeparator()
                .addAll(dgToggleMainMenu, dgToggleMainMenuSep, dgToggleToolbar, dgToggleToolbarClassic, dgToggleNavBar, dgViewToolbarActions)
                .addSeparator();

        manager.registerAction(dgPresAssistant);
        manager.registerAction(dgToggleMainMenu);
        manager.registerAction(dgToggleMainMenuSep);
        manager.registerAction(dgToggleToolbar);
        manager.registerAction(dgToggleToolbarClassic);
        manager.registerAction(dgToggleNavBar);
        manager.registerAction(dgTbToolbar);
        manager.registerAction(dgTbNavbar);
        manager.registerGroup(dgTbNavbarGroup);
        manager.registerAction(dgToggleTwBars);
        manager.registerAction(dgToggleStatusBar);
        manager.registerAction(dgSbStatusText);
        manager.registerGroup(dgSbWidgetsGroup);
        manager.registerAction(dgToggleMembers);
        manager.registerGroup(dgViewToolbarActions);
        manager.registerGroup(dgUiToggleActions);

        AnAction dgRecentToggleChanged = AnAction.builder("view.recent.toggle.changed.only", "Toggle Changed Only Files")
                .onAction(ctx -> ctx.setStatus("Toggle Changed Only Files")).build();
        AnAction dgRecentIterateFiles = AnAction.builder("view.recent.iterate.files", "Iterate Recent Files")
                .onAction(MainWindow::showRecentFilesDialog).build();
        AnAction dgRecentToggleChanged2 = AnAction.builder("view.recent.toggle.changed.only.second", "Toggle Changed Only Files")
                .onAction(ctx -> ctx.setStatus("Toggle Changed Only Files")).build();
        AnAction dgRecentIterateFiles2 = AnAction.builder("view.recent.iterate.files.second", "Iterate Recent Files")
                .onAction(MainWindow::showRecentFilesDialog).build();
        AnAction dgRecentFiles2 = AnAction.builder("view.recent.files.second", "Recent Files")
                .onAction(MainWindow::showRecentFilesDialog).build();
        AnAction dgRecentlyChangedFiles2 = AnAction.builder("view.recently.changed.files.second", "Recently Changed Files")
                .onAction(MainWindow::showRecentlyChangedFilesDialog).build();

        ActionGroup dgViewRecentActions = new ActionGroup("view.recent.actions.group", "View Recent Actions Group", false);
        dgViewRecentActions.addAll(recentFiles, dgRecentToggleChanged, dgRecentIterateFiles, recentChangedFiles, recentLocations,
                dgRecentToggleChanged2, dgRecentIterateFiles2, dgRecentFiles2, dgRecentlyChangedFiles2, recentChanges);

        manager.registerAction(dgRecentToggleChanged);
        manager.registerAction(dgRecentIterateFiles);
        manager.registerAction(dgRecentToggleChanged2);
        manager.registerAction(dgRecentIterateFiles2);
        manager.registerAction(dgRecentFiles2);
        manager.registerAction(dgRecentlyChangedFiles2);
        manager.registerGroup(dgViewRecentActions);

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
        // 4b. NAVIGATE ACTIONS (DataGrip Hierarchy & Aliases)
        // =========================================================================
        manager.registerAction("nav.back", navBack);
        manager.registerAction("nav.forward", navForward);
        manager.registerAction("header.search", searchEverywhere);

        AnAction navGotoClass = AnAction.builder("nav.goto.class", "Go to Class\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::showSearchDatabaseObjectsDialog)
                .build();
        AnAction navGotoFile = AnAction.builder("nav.goto.file", "Go to File\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchFilesDialog)
                .build();
        AnAction navGotoSymbol = AnAction.builder("nav.goto.symbol", "Go to Symbol\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchCodeDialog)
                .build();
        AnAction navGotoText = AnAction.builder("nav.goto.text", "Text\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchTextDialog)
                .build();
        AnAction navGotoDbObject = AnAction.builder("nav.goto.database.object", "Go To Database Object")
                .accelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::showSearchDatabaseObjectsDialog)
                .build();

        AnAction navGotoRow = AnAction.builder("nav.goto.row", "Row\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Go to Row"))
                .build();
        AnAction navOpenFileUri = AnAction.builder("nav.open.file.uri", "Open File URI")
                .icon(FontAwesomeSolid.FOLDER, "#dcb67a", 11)
                .onAction(ctx -> ctx.setStatus("Open File URI"))
                .build();
        AnAction navOpenUrl = AnAction.builder("nav.open.url", "Open URL")
                .icon(FontAwesomeSolid.GLOBE, "#6897bb", 11)
                .onAction(ctx -> ctx.setStatus("Open URL"))
                .build();
        AnAction navRelatedRows = AnAction.builder("nav.related.rows", "Related Rows")
                .icon(FontAwesomeSolid.TABLE, "#499c54", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F4))
                .onAction(ctx -> ctx.setStatus("Related Rows"))
                .build();
        AnAction navGotoLineCol = AnAction.builder("nav.goto.line.column", "Go to Line:Column\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Go to Line:Column"))
                .build();

        AnAction navNextError = AnAction.builder("nav.next.highlighted.error", "Next Highlighted Error")
                .accelerator(new KeyCodeCombination(KeyCode.F2))
                .onAction(ctx -> ctx.setStatus("Next Highlighted Error"))
                .build();
        AnAction navPrevError = AnAction.builder("nav.prev.highlighted.error", "Previous Highlighted Error")
                .accelerator(new KeyCodeCombination(KeyCode.F2, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Previous Highlighted Error"))
                .build();

        AnAction navNextEmmet = AnAction.builder("nav.next.emmet.edit.point", "Next Emmet Edit Point")
                .accelerator(new KeyCodeCombination(KeyCode.CLOSE_BRACKET, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Next Emmet Edit Point"))
                .build();
        AnAction navPrevEmmet = AnAction.builder("nav.prev.emmet.edit.point", "Previous Emmet Edit Point")
                .accelerator(new KeyCodeCombination(KeyCode.OPEN_BRACKET, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Previous Emmet Edit Point"))
                .build();

        AnAction navLastEditLoc = AnAction.builder("nav.last.edit.location", "Last Edit Location")
                .accelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Last Edit Location"))
                .build();
        AnAction navNextEditLoc = AnAction.builder("nav.next.edit.location", "Next Edit Location")
                .onAction(ctx -> ctx.setStatus("Next Edit Location"))
                .build();

        AnAction navFileNextStmt = AnAction.builder("nav.file.next.statement", "Next Statement")
                .accelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::navigateToNextStatement)
                .build();
        AnAction navFilePrevStmt = AnAction.builder("nav.file.prev.statement", "Previous Statement")
                .accelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::navigateToPreviousStatement)
                .build();
        AnAction navFileMatchingBrace = AnAction.builder("nav.file.matching.brace", "Move Caret to Matching Brace")
                .accelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Move Caret to Matching Brace"))
                .build();

        AnAction navNextTplParam = AnAction.builder("nav.next.template.parameter", "Next Template Parameter")
                .onAction(ctx -> ctx.setStatus("Next Template Parameter"))
                .build();
        AnAction navPrevTplParam = AnAction.builder("nav.prev.template.parameter", "Previous Template Parameter")
                .onAction(ctx -> ctx.setStatus("Previous Template Parameter"))
                .build();

        AnAction navCustomFolding = AnAction.builder("nav.custom.folding", "Custom Folding\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.PERIOD, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Custom Folding"))
                .build();

        AnAction navNextChange = AnAction.builder("nav.next.change", "Next Change")
                .icon(FontAwesomeSolid.ARROW_DOWN, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Next Change"))
                .build();
        AnAction navPrevChange = AnAction.builder("nav.prev.change", "Previous Change")
                .icon(FontAwesomeSolid.ARROW_UP, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Previous Change"))
                .build();

        AnAction navSelectIn = AnAction.builder("nav.select.in", "Select In\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::selectInTarget)
                .build();
        AnAction navJumpNavBar = AnAction.builder("nav.jump.to.nav.bar", "Jump to Navigation Bar")
                .accelerator(new KeyCodeCombination(KeyCode.HOME, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::jumpToNavigationBar)
                .build();

        AnAction navGotoDecl = AnAction.builder("nav.goto.declaration", "Declaration or Usages")
                .accelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::navigateDeclarationOrUsages)
                .build();
        AnAction navGotoImpl = AnAction.builder("nav.goto.implementation", "Implementation(s)")
                .accelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Implementation(s)"))
                .build();
        AnAction navGotoTypeDecl = AnAction.builder("nav.goto.type.declaration", "Type Declaration")
                .accelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Type Declaration"))
                .build();
        AnAction navGotoSuperMethod = AnAction.builder("nav.goto.super.method", "Super Method")
                .accelerator(new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Super Method"))
                .build();
        AnAction navGotoTest = AnAction.builder("nav.goto.test", "Test")
                .accelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Test"))
                .build();
        AnAction navRelatedSymbol = AnAction.builder("nav.related.symbol", "Related Symbol\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.HOME, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Related Symbol"))
                .build();

        AnAction navFileStructure = AnAction.builder("nav.file.structure", "File Structure")
                .accelerator(new KeyCodeCombination(KeyCode.F12, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("File Structure"))
                .build();
        AnAction navFilePath = AnAction.builder("nav.file.path", "File Path")
                .accelerator(new KeyCodeCombination(KeyCode.F12, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::showFilePathPopup)
                .build();

        AnAction navTypeHierarchy = AnAction.builder("nav.type.hierarchy", "Type Hierarchy")
                .accelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Type Hierarchy"))
                .build();
        AnAction navMethodHierarchy = AnAction.builder("nav.method.hierarchy", "Method Hierarchy")
                .accelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Method Hierarchy"))
                .build();
        AnAction navCallHierarchy = AnAction.builder("nav.call.hierarchy", "Call Hierarchy")
                .accelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Call Hierarchy"))
                .build();

        AnAction navPrevOccur = AnAction.builder("nav.prev.occurrence", "Previous Occurrence")
                .icon(FontAwesomeSolid.ARROW_UP, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Previous Occurrence"))
                .build();
        AnAction navNextOccur = AnAction.builder("nav.next.occurrence", "Next Occurrence")
                .icon(FontAwesomeSolid.ARROW_DOWN, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Next Occurrence"))
                .build();

        AnAction navDbeNextStmt = AnAction.builder("nav.dbe.next.statement", "Next Statement")
                .accelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::navigateToNextStatement)
                .build();
        AnAction navDbePrevStmt = AnAction.builder("nav.dbe.prev.statement", "Previous Statement")
                .accelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.ALT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::navigateToPreviousStatement)
                .build();

        ActionGroup navGotoByNameGroup = new ActionGroup("nav.goto.by.name.group", "Goto by Name Actions", false);
        navGotoByNameGroup.addAll(navGotoClass, navGotoFile, navGotoSymbol, navGotoText, navGotoDbObject);

        ActionGroup navGotoErrorGroup = new ActionGroup("nav.goto.error.bookmark.group", "Goto Error/Bookmark Actions", false);
        navGotoErrorGroup.addAll(navNextError, navPrevError);

        ActionGroup navGotoEditPointGroup = new ActionGroup("nav.goto.edit.point.group", "GoToEditPointGroup", false);
        navGotoEditPointGroup.addSeparator().addAll(navNextEmmet, navPrevEmmet);

        ActionGroup navTplParamsGroup = new ActionGroup("nav.template.parameters.group", "TemplateParametersNavigation", false);
        navTplParamsGroup.addAll(navNextTplParam, navPrevTplParam);

        ActionGroup navChangeNavGroup = new ActionGroup("nav.change.navigation.group", "Change Navigation Actions", false);
        navChangeNavGroup.addSeparator().addAll(navNextChange, navPrevChange);

        ActionGroup navInFileGroup = new ActionGroup("nav.navigate.in.file.group", "Navigate in File", false);
        navInFileGroup.addAll(navFileNextStmt, navFilePrevStmt, navFileMatchingBrace)
                .addSeparator()
                .add(navTplParamsGroup)
                .add(navCustomFolding)
                .add(navChangeNavGroup);

        ActionGroup navHierarchyGroup = new ActionGroup("nav.hierarchy.actions.group", "Hierarchy Actions", false);
        navHierarchyGroup.addAll(navTypeHierarchy, navMethodHierarchy, navCallHierarchy);

        ActionGroup navGotoByRefGroup = new ActionGroup("nav.goto.by.reference.group", "Goto by Reference Actions", false);
        navGotoByRefGroup.addSeparator()
                .addAll(navSelectIn, navJumpNavBar)
                .addSeparator()
                .addAll(navGotoDecl, navGotoImpl, navGotoTypeDecl, navGotoSuperMethod, navGotoTest, navRelatedSymbol)
                .addSeparator()
                .addAll(navFileStructure, navFilePath, navHierarchyGroup);

        ActionGroup navDbeGotoGroup = new ActionGroup("nav.dbe.goto.menu.ex", "DBE.GoToMenuEx", false);
        navDbeGotoGroup.addSeparator().addAll(navDbeNextStmt, navDbePrevStmt).addSeparator();

        manager.registerAction(navGotoClass);
        manager.registerAction(navGotoFile);
        manager.registerAction(navGotoSymbol);
        manager.registerAction(navGotoText);
        manager.registerAction(navGotoDbObject);
        manager.registerAction(navGotoRow);
        manager.registerAction(navOpenFileUri);
        manager.registerAction(navOpenUrl);
        manager.registerAction(navRelatedRows);
        manager.registerAction(navGotoLineCol);
        manager.registerAction(navNextError);
        manager.registerAction(navPrevError);
        manager.registerAction(navNextEmmet);
        manager.registerAction(navPrevEmmet);
        manager.registerAction(navLastEditLoc);
        manager.registerAction(navNextEditLoc);
        manager.registerAction(navFileNextStmt);
        manager.registerAction(navFilePrevStmt);
        manager.registerAction(navFileMatchingBrace);
        manager.registerAction(navNextTplParam);
        manager.registerAction(navPrevTplParam);
        manager.registerAction(navCustomFolding);
        manager.registerAction(navNextChange);
        manager.registerAction(navPrevChange);
        manager.registerAction(navSelectIn);
        manager.registerAction(navJumpNavBar);
        manager.registerAction(navGotoDecl);
        manager.registerAction(navGotoImpl);
        manager.registerAction(navGotoTypeDecl);
        manager.registerAction(navGotoSuperMethod);
        manager.registerAction(navGotoTest);
        manager.registerAction(navRelatedSymbol);
        manager.registerAction(navFileStructure);
        manager.registerAction(navFilePath);
        manager.registerAction(navTypeHierarchy);
        manager.registerAction(navMethodHierarchy);
        manager.registerAction(navCallHierarchy);
        manager.registerAction(navPrevOccur);
        manager.registerAction(navNextOccur);
        manager.registerAction(navDbeNextStmt);
        manager.registerAction(navDbePrevStmt);

        manager.registerGroup(navGotoByNameGroup);
        manager.registerGroup(navGotoErrorGroup);
        manager.registerGroup(navGotoEditPointGroup);
        manager.registerGroup(navTplParamsGroup);
        manager.registerGroup(navChangeNavGroup);
        manager.registerGroup(navInFileGroup);
        manager.registerGroup(navHierarchyGroup);
        manager.registerGroup(navGotoByRefGroup);
        manager.registerGroup(navDbeGotoGroup);

        // =========================================================================
        // CODE ACTIONS (Matching DataGrip Images 4-5)
        // =========================================================================
        AnAction codeOverride = AnAction.builder("code.override.methods", "Override Methods\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Override Methods"))
                .build();
        AnAction codeImplement = AnAction.builder("code.implement.methods", "Implement Methods\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Implement Methods"))
                .build();
        AnAction codeGenerate = AnAction.builder("code.generate", "Generate\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.INSERT, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Generate"))
                .build();

        AnAction codeCompBasic = AnAction.builder("code.completion.basic", "Basic")
                .accelerator(new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Basic Code Completion"))
                .build();
        AnAction codeCompType = AnAction.builder("code.completion.type.matching", "Type-Matching")
                .accelerator(new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Type-Matching Completion"))
                .build();
        AnAction codeCompStmt = AnAction.builder("code.completion.complete.statement", "Complete Current Statement")
                .accelerator(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Complete Current Statement"))
                .build();
        AnAction codeCompWord = AnAction.builder("code.completion.cyclic.expand.word", "Cyclic Expand Word")
                .accelerator(new KeyCodeCombination(KeyCode.SLASH, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Cyclic Expand Word"))
                .build();
        AnAction codeCompWordBack = AnAction.builder("code.completion.cyclic.expand.word.backward", "Cyclic Expand Word (Backward)")
                .accelerator(new KeyCodeCombination(KeyCode.SLASH, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Cyclic Expand Word (Backward)"))
                .build();
        AnAction codeCompCallInline = AnAction.builder("code.completion.call.inline", "Call Inline Completion")
                .accelerator(new KeyCodeCombination(KeyCode.BACK_SLASH, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Call Inline Completion"))
                .build();
        AnAction codeCompInsertProp = AnAction.builder("code.completion.insert.inline.proposal", "Insert Inline Proposal")
                .accelerator(new KeyCodeCombination(KeyCode.TAB))
                .onAction(ctx -> ctx.setStatus("Insert Inline Proposal"))
                .build();
        AnAction codeCompInsertWord = AnAction.builder("code.completion.insert.inline.word", "Insert Inline Proposal's Word")
                .accelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Insert Inline Proposal's Word"))
                .build();
        AnAction codeCompInsertLine = AnAction.builder("code.completion.insert.inline.line", "Insert Inline Proposal's Line")
                .accelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Insert Inline Proposal's Line"))
                .build();

        AnAction codeInsertTemplate = AnAction.builder("code.insert.live.template", "Insert Live Template\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.J, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Insert Live Template"))
                .build();
        AnAction codeSaveTemplate = AnAction.builder("code.save.live.template", "Save as Live Template\u2026")
                .onAction(ctx -> ctx.setStatus("Save as Live Template"))
                .build();

        AnAction codeSurroundWith = AnAction.builder("code.surround.with", "Surround With\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Surround With"))
                .build();
        AnAction codeUnwrap = AnAction.builder("code.unwrap.remove", "Unwrap/Remove\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.DELETE, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Unwrap/Remove"))
                .build();

        AnAction codeMoveStmtDown = AnAction.builder("code.move.statement.down", "Move Statement Down")
                .accelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Move Statement Down"))
                .build();
        AnAction codeMoveStmtUp = AnAction.builder("code.move.statement.up", "Move Statement Up")
                .accelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Move Statement Up"))
                .build();
        AnAction codeMoveElemLeft = AnAction.builder("code.move.element.left", "Move Element Left")
                .accelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Move Element Left"))
                .build();
        AnAction codeMoveElemRight = AnAction.builder("code.move.element.right", "Move Element Right")
                .accelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Move Element Right"))
                .build();
        AnAction codeMoveLineDown = AnAction.builder("code.move.line.down", "Move Line Down")
                .accelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Move Line Down"))
                .build();
        AnAction codeMoveLineUp = AnAction.builder("code.move.line.up", "Move Line Up")
                .accelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Move Line Up"))
                .build();

        AnAction mainMenuBuild = AnAction.builder("main.menu.build", "Build")
                .accelerator(new KeyCodeCombination(KeyCode.F9, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Build Project"))
                .build();

        ActionGroup codeCompletionGroup = new ActionGroup("code.completion.group", "Code Completion", true);
        codeCompletionGroup.addAll(codeCompBasic, codeCompType)
                .addSeparator()
                .add(codeCompStmt)
                .addSeparator()
                .addAll(codeCompWord, codeCompWordBack)
                .addSeparator()
                .addAll(codeCompCallInline, codeCompInsertProp, codeCompInsertWord, codeCompInsertLine);

        // Code Inspection & Analysis (Image 1)
        AnAction codeInspectCode = AnAction.builder("code.inspect.code", "Inspect Code\u2026")
                .onAction(ctx -> ctx.setStatus("Inspect Code"))
                .build();
        AnAction codeCleanup = AnAction.builder("code.cleanup", "Code Cleanup\u2026")
                .onAction(ctx -> ctx.setStatus("Code Cleanup"))
                .build();
        AnAction codeSilentCleanup = AnAction.builder("code.silent.cleanup", "Silent Code Cleanup")
                .onAction(ctx -> ctx.setStatus("Silent Code Cleanup"))
                .build();
        AnAction codeRunInspection = AnAction.builder("code.run.inspection.by.name", "Run Inspection by Name\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Run Inspection by Name"))
                .build();
        AnAction codeConfigAnalysis = AnAction.builder("code.configure.analysis", "Configure Current File Analysis\u2026")
                .onAction(ctx -> ctx.setStatus("Configure Current File Analysis"))
                .build();
        AnAction codeViewOffline = AnAction.builder("code.view.offline.results", "View Offline Inspection Results\u2026")
                .onAction(ctx -> ctx.setStatus("View Offline Inspection Results"))
                .build();
        AnAction codeDataflowToHere = AnAction.builder("code.analyze.dataflow.to.here", "Analyze Data Flow to Here\u2026")
                .onAction(ctx -> ctx.setStatus("Analyze Data Flow to Here"))
                .build();
        AnAction codeDataflowFromHere = AnAction.builder("code.analyze.dataflow.from.here", "Analyze Data Flow from Here\u2026")
                .onAction(ctx -> ctx.setStatus("Analyze Data Flow from Here"))
                .build();
        AnAction codeAnalyzePlatform = AnAction.builder("code.analyze.platform.menu", "AnalyzePlatformMenu")
                .onAction(ctx -> ctx.setStatus("Analyze Platform Menu"))
                .build();

        ActionGroup inspectCodeActionsGroup = new ActionGroup("code.inspect.actions.group", "Inspect Code Actions", false);
        inspectCodeActionsGroup.addAll(codeInspectCode, codeCleanup);

        ActionGroup analyzeActionsGroup = new ActionGroup("code.analyze.actions.group", "AnalyzeActions", false);
        analyzeActionsGroup.addAll(codeSilentCleanup, codeRunInspection, codeConfigAnalysis, codeViewOffline)
                .addSeparator()
                .addAll(codeDataflowToHere, codeDataflowFromHere);

        ActionGroup analyzeCodeGroup = new ActionGroup("code.analyze.code.group", "Analyze Code", false);
        analyzeCodeGroup.addAll(analyzeActionsGroup, codeAnalyzePlatform);

        ActionGroup inspectCodeGroup = new ActionGroup("code.inspect.group", "InspectCodeInCodeMenuGroup", false);
        inspectCodeGroup.addSeparator()
                .addAll(inspectCodeActionsGroup, analyzeCodeGroup);

        // Code Folding Actions (Image 2)
        AnAction foldExpand = AnAction.builder("code.folding.expand", "Expand")
                .accelerator(new KeyCodeCombination(KeyCode.ADD, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Expand"))
                .build();
        AnAction foldExpandRecursively = AnAction.builder("code.folding.expand.recursively", "Expand Recursively")
                .accelerator(new KeyCodeCombination(KeyCode.ADD, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Expand Recursively"))
                .build();
        AnAction foldExpandAll = AnAction.builder("code.folding.expand.all", "Expand All")
                .accelerator(new KeyCodeCombination(KeyCode.ADD, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Expand All"))
                .build();

        AnAction foldCollapse = AnAction.builder("code.folding.collapse", "Collapse")
                .accelerator(new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Collapse"))
                .build();
        AnAction foldCollapseRecursively = AnAction.builder("code.folding.collapse.recursively", "Collapse Recursively")
                .accelerator(new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Collapse Recursively"))
                .build();
        AnAction foldCollapseAll = AnAction.builder("code.folding.collapse.all", "Collapse All")
                .accelerator(new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Collapse All"))
                .build();

        ActionGroup expandToLevelGroup = new ActionGroup("code.folding.expand.to.level.group", "Expand to Level", false);
        for (int lvl = 1; lvl <= 5; lvl++) {
            final int level = lvl;
            expandToLevelGroup.add(AnAction.builder("code.folding.expand.level." + level, String.valueOf(level))
                    .onAction(ctx -> ctx.setStatus("Expand to Level " + level)).build());
        }

        ActionGroup expandAllToLevelGroup = new ActionGroup("code.folding.expand.all.to.level.group", "Expand All to Level", false);
        for (int lvl = 1; lvl <= 5; lvl++) {
            final int level = lvl;
            expandAllToLevelGroup.add(AnAction.builder("code.folding.expand.all.level." + level, String.valueOf(level))
                    .onAction(ctx -> ctx.setStatus("Expand All to Level " + level)).build());
        }

        AnAction foldExpandDocComments = AnAction.builder("code.folding.expand.doc.comments", "Expand Doc Comments")
                .onAction(ctx -> ctx.setStatus("Expand Doc Comments"))
                .build();
        AnAction foldCollapseDocComments = AnAction.builder("code.folding.collapse.doc.comments", "Collapse Doc Comments")
                .onAction(ctx -> ctx.setStatus("Collapse Doc Comments"))
                .build();
        ActionGroup langFoldingGroup = new ActionGroup("code.folding.language.specific.group", "LanguageSpecificFoldingGroup", false);
        langFoldingGroup.addAll(foldExpandDocComments, foldCollapseDocComments);

        AnAction foldToggle = AnAction.builder("code.folding.toggle", "Toggle Folding")
                .accelerator(new KeyCodeCombination(KeyCode.PERIOD, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Toggle Folding"))
                .build();
        AnAction foldSelection = AnAction.builder("code.folding.fold.selection", "Fold Selection / Remove region")
                .accelerator(new KeyCodeCombination(KeyCode.PERIOD, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Fold Selection"))
                .build();
        AnAction foldCodeBlock = AnAction.builder("code.folding.fold.block", "Fold Code Block")
                .onAction(ctx -> ctx.setStatus("Fold Code Block"))
                .build();

        ActionGroup foldingGroup = new ActionGroup("code.folding.group", "Folding", true);
        foldingGroup.addAll(foldExpand, foldExpandRecursively, foldExpandAll)
                .addSeparator()
                .addAll(foldCollapse, foldCollapseRecursively, foldCollapseAll)
                .addSeparator()
                .addAll(expandToLevelGroup, expandAllToLevelGroup)
                .addSeparator()
                .add(langFoldingGroup)
                .addSeparator()
                .add(foldToggle)
                .addSeparator()
                .addAll(foldSelection, foldCodeBlock);

        // Code Comment Actions (Image 3)
        AnAction codeCommentLine = AnAction.builder("code.comment.line", "// Comment with Line Comment")
                .accelerator(new KeyCodeCombination(KeyCode.SLASH, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Comment with Line Comment"))
                .build();
        AnAction codeCommentBlock = AnAction.builder("code.comment.block", "Comment with Block Comment")
                .accelerator(new KeyCodeCombination(KeyCode.SLASH, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Comment with Block Comment"))
                .build();
        ActionGroup commentActionsGroup = new ActionGroup("code.comment.actions.group", "Comment Actions", false);
        commentActionsGroup.addAll(codeCommentLine, codeCommentBlock);

        // Code Formatting Actions (Image 3)
        AnAction codeReformat = AnAction.builder("code.reformat", "Reformat Code")
                .icon(FontAwesomeSolid.INDENT, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::formatCurrentSql)
                .build();
        AnAction codeReformatJson = AnAction.builder("code.reformat.json", "Reformat JSON")
                .onAction(ctx -> ctx.setStatus("Reformat JSON"))
                .build();
        AnAction codeReformatFile = AnAction.builder("code.reformat.file", "Reformat File\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Reformat File"))
                .build();
        AnAction codeAutoIndent = AnAction.builder("code.auto.indent", "Auto-Indent Lines")
                .accelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Auto-Indent Lines"))
                .build();
        AnAction codeOptimizeImports = AnAction.builder("code.optimize.imports", "Optimize Imports")
                .accelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Optimize Imports"))
                .build();
        AnAction codeRearrange = AnAction.builder("code.rearrange.code", "Rearrange Code")
                .onAction(ctx -> ctx.setStatus("Rearrange Code"))
                .build();
        ActionGroup codeFormattingGroup = new ActionGroup("code.formatting.actions.group", "Code Formatting Actions", false);
        codeFormattingGroup.addAll(codeReformat, codeReformatJson, codeReformatFile, codeAutoIndent, codeOptimizeImports, codeRearrange);

        ActionGroup codeMenu = new ActionGroup("menu.code", "Code", true);
        codeMenu.addAll(codeOverride, codeImplement, codeGenerate)
                .addSeparator()
                .addAll(codeCompletionGroup, inspectCodeGroup)
                .addSeparator()
                .addAll(codeInsertTemplate, codeSaveTemplate)
                .addSeparator()
                .addAll(codeSurroundWith, codeUnwrap)
                .addSeparator()
                .add(foldingGroup)
                .addSeparator()
                .addAll(commentActionsGroup, codeFormattingGroup)
                .addSeparator()
                .addAll(codeMoveStmtDown, codeMoveStmtUp, codeMoveElemLeft, codeMoveElemRight, codeMoveLineDown, codeMoveLineUp)
                .addSeparator()
                .addSeparator();

        manager.registerAction(codeOverride);
        manager.registerAction(codeImplement);
        manager.registerAction(codeGenerate);
        manager.registerAction(codeCompBasic);
        manager.registerAction(codeCompType);
        manager.registerAction(codeCompStmt);
        manager.registerAction(codeCompWord);
        manager.registerAction(codeCompWordBack);
        manager.registerAction(codeCompCallInline);
        manager.registerAction(codeCompInsertProp);
        manager.registerAction(codeCompInsertWord);
        manager.registerAction(codeCompInsertLine);
        manager.registerAction(codeInsertTemplate);
        manager.registerAction(codeSaveTemplate);
        manager.registerAction(codeSurroundWith);
        manager.registerAction(codeUnwrap);
        manager.registerAction(codeMoveStmtDown);
        manager.registerAction(codeMoveStmtUp);
        manager.registerAction(codeMoveElemLeft);
        manager.registerAction(codeMoveElemRight);
        manager.registerAction(codeMoveLineDown);
        manager.registerAction(codeMoveLineUp);
        manager.registerAction(mainMenuBuild);

        manager.registerAction(codeInspectCode);
        manager.registerAction(codeCleanup);
        manager.registerAction(codeSilentCleanup);
        manager.registerAction(codeRunInspection);
        manager.registerAction(codeConfigAnalysis);
        manager.registerAction(codeViewOffline);
        manager.registerAction(codeDataflowToHere);
        manager.registerAction(codeDataflowFromHere);
        manager.registerAction(codeAnalyzePlatform);

        manager.registerAction(foldExpand);
        manager.registerAction(foldExpandRecursively);
        manager.registerAction(foldExpandAll);
        manager.registerAction(foldCollapse);
        manager.registerAction(foldCollapseRecursively);
        manager.registerAction(foldCollapseAll);
        manager.registerAction(foldExpandDocComments);
        manager.registerAction(foldCollapseDocComments);
        manager.registerAction(foldToggle);
        manager.registerAction(foldSelection);
        manager.registerAction(foldCodeBlock);

        manager.registerAction(codeCommentLine);
        manager.registerAction(codeCommentBlock);

        manager.registerAction(codeReformat);
        manager.registerAction(codeReformatJson);
        manager.registerAction(codeReformatFile);
        manager.registerAction(codeAutoIndent);
        manager.registerAction(codeOptimizeImports);
        manager.registerAction(codeRearrange);

        manager.registerGroup(codeCompletionGroup);
        manager.registerGroup(inspectCodeActionsGroup);
        manager.registerGroup(analyzeActionsGroup);
        manager.registerGroup(analyzeCodeGroup);
        manager.registerGroup(inspectCodeGroup);
        manager.registerGroup(expandToLevelGroup);
        manager.registerGroup(expandAllToLevelGroup);
        manager.registerGroup(langFoldingGroup);
        manager.registerGroup(foldingGroup);
        manager.registerGroup(commentActionsGroup);
        manager.registerGroup(codeFormattingGroup);
        manager.registerGroup(codeMenu);

        // =========================================================================
        // REFACTOR ACTIONS (Matching DataGrip Images 4-5)
        // =========================================================================
        AnAction refactorThis = AnAction.builder("refactor.this", "Refactor This\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Refactor This"))
                .build();
        AnAction refactorRename = AnAction.builder("refactor.rename", "Rename\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Rename"))
                .build();
        AnAction refactorChangeSignature = AnAction.builder("refactor.change.signature", "Change Signature\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Change Signature"))
                .build();
        AnAction refactorModifyObject = AnAction.builder("refactor.modify.object", "Modify Object\u2026")
                .onAction(ctx -> ctx.setStatus("Modify Object"))
                .build();

        // Extract/Introduce Submenu (Image 5)
        AnAction refactorIntroVar = AnAction.builder("refactor.introduce.variable", "Introduce Variable\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Introduce Variable"))
                .build();
        AnAction refactorExtractRoutine = AnAction.builder("refactor.extract.routine", "Extract Routine\u2026")
                .onAction(ctx -> ctx.setStatus("Extract Routine"))
                .build();
        AnAction refactorTableAlias = AnAction.builder("refactor.table.alias", "Table alias\u2026")
                .onAction(ctx -> ctx.setStatus("Table alias"))
                .build();
        AnAction refactorIntroConst = AnAction.builder("refactor.introduce.constant", "Introduce Constant\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Introduce Constant"))
                .build();
        AnAction refactorIntroField = AnAction.builder("refactor.introduce.field", "Introduce Field\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Introduce Field"))
                .build();
        AnAction refactorIntroParam = AnAction.builder("refactor.introduce.parameter", "Introduce Parameter\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Introduce Parameter"))
                .build();
        AnAction refactorIntroParamObj = AnAction.builder("refactor.introduce.parameter.object", "Introduce Parameter Object\u2026")
                .onAction(ctx -> ctx.setStatus("Introduce Parameter Object"))
                .build();
        AnAction refactorExtractMethod = AnAction.builder("refactor.extract.method", "Extract Method\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Extract Method"))
                .build();
        AnAction refactorExtractDelegate = AnAction.builder("refactor.extract.delegate", "Extract Delegate\u2026")
                .onAction(ctx -> ctx.setStatus("Extract Delegate"))
                .build();
        AnAction refactorIncludeFile = AnAction.builder("refactor.include.file", "Include File\u2026")
                .onAction(ctx -> ctx.setStatus("Include File"))
                .build();
        AnAction refactorExtractInterface = AnAction.builder("refactor.extract.interface", "Extract Interface\u2026")
                .onAction(ctx -> ctx.setStatus("Extract Interface"))
                .build();
        AnAction refactorExtractSuperclass = AnAction.builder("refactor.extract.superclass", "Extract Superclass\u2026")
                .onAction(ctx -> ctx.setStatus("Extract Superclass"))
                .build();
        AnAction refactorExtractModule = AnAction.builder("refactor.extract.module", "Extract Module\u2026")
                .onAction(ctx -> ctx.setStatus("Extract Module"))
                .build();
        AnAction mainRefactorSubqueryCte = AnAction.builder("refactor.subquery.cte", "Subquery as CTE")
                .onAction(ctx -> ctx.setStatus("Subquery as CTE"))
                .build();

        ActionGroup mainExtractIntroduceGroup = new ActionGroup("refactor.extract.introduce.group", "Extract/Introduce", true);
        mainExtractIntroduceGroup.addAll(refactorIntroVar, refactorExtractRoutine, refactorTableAlias, refactorIntroConst, refactorIntroField, refactorIntroParam)
                .addSeparator()
                .add(refactorIntroParamObj)
                .addSeparator()
                .add(refactorExtractMethod)
                .addSeparator()
                .addAll(refactorExtractDelegate, refactorIncludeFile, refactorExtractInterface, refactorExtractSuperclass, refactorExtractModule, mainRefactorSubqueryCte);

        AnAction refactorInline = AnAction.builder("refactor.inline", "Inline\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Inline"))
                .build();
        AnAction refactorMove = AnAction.builder("refactor.move", "Move\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.F6))
                .onAction(ctx -> ctx.setStatus("Move"))
                .build();
        AnAction refactorCopy = AnAction.builder("refactor.copy", "Copy\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.F5))
                .onAction(ctx -> ctx.setStatus("Copy"))
                .build();
        AnAction refactorSafeDelete = AnAction.builder("refactor.safe.delete", "Safe Delete\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.DELETE, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Safe Delete"))
                .build();
        AnAction refactorPullUp = AnAction.builder("refactor.pull.members.up", "Pull Members Up\u2026")
                .onAction(ctx -> ctx.setStatus("Pull Members Up"))
                .build();
        AnAction refactorPushDown = AnAction.builder("refactor.push.members.down", "Push Members Down\u2026")
                .onAction(ctx -> ctx.setStatus("Push Members Down"))
                .build();
        AnAction refactorInvertBoolean = AnAction.builder("refactor.invert.boolean", "Invert Boolean\u2026")
                .onAction(ctx -> ctx.setStatus("Invert Boolean"))
                .build();

        ActionGroup refactorMenu = new ActionGroup("menu.refactor", "Refactor", true);
        refactorMenu.addAll(refactorThis, refactorRename, refactorChangeSignature, refactorModifyObject)
                .addSeparator()
                .addAll(mainExtractIntroduceGroup, refactorInline)
                .addSeparator()
                .addAll(refactorMove, refactorCopy, refactorSafeDelete)
                .addSeparator()
                .addAll(refactorPullUp, refactorPushDown, refactorInvertBoolean);

        manager.registerAction(refactorThis);
        manager.registerAction(refactorRename);
        manager.registerAction(refactorChangeSignature);
        manager.registerAction(refactorModifyObject);
        manager.registerAction(refactorIntroVar);
        manager.registerAction(refactorExtractRoutine);
        manager.registerAction(refactorTableAlias);
        manager.registerAction(refactorIntroConst);
        manager.registerAction(refactorIntroField);
        manager.registerAction(refactorIntroParam);
        manager.registerAction(refactorIntroParamObj);
        manager.registerAction(refactorExtractMethod);
        manager.registerAction(refactorExtractDelegate);
        manager.registerAction(refactorIncludeFile);
        manager.registerAction(refactorExtractInterface);
        manager.registerAction(refactorExtractSuperclass);
        manager.registerAction(refactorExtractModule);
        manager.registerAction(mainRefactorSubqueryCte);
        manager.registerAction(refactorInline);
        manager.registerAction(refactorMove);
        manager.registerAction(refactorCopy);
        manager.registerAction(refactorSafeDelete);
        manager.registerAction(refactorPullUp);
        manager.registerAction(refactorPushDown);
        manager.registerAction(refactorInvertBoolean);

        manager.registerGroup(mainExtractIntroduceGroup);
        manager.registerGroup(refactorMenu);

        // =========================================================================
        // 5. RUN ACTIONS (Matching DataGrip Images 1-4)
        // =========================================================================
        // Group 1: Run/Debug (Image 2)
        AnAction runRun = AnAction.builder("run.run", "Run")
                .icon(FontAwesomeSolid.PLAY, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F10, KeyCombination.SHIFT_DOWN))
                .enabledWhen(MainWindow::hasActiveConsole)
                .onAction(MainWindow::executeCurrentStatement)
                .build();
        AnAction runDebug = AnAction.builder("run.debug", "Debug")
                .icon(FontAwesomeSolid.BUG, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F9, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Debug"))
                .build();
        AnAction runCoverage = AnAction.builder("run.coverage", "Run with Coverage")
                .icon(FontAwesomeSolid.SHIELD_ALT, "#57965c", 11)
                .onAction(ctx -> ctx.setStatus("Run with Coverage"))
                .build();
        AnAction runProfiler = AnAction.builder("run.profiler", "Run with Profiler")
                .icon(FontAwesomeSolid.TACHOMETER_ALT, "#57965c", 11)
                .onAction(ctx -> ctx.setStatus("Run with Profiler"))
                .build();

        ActionGroup runDebugGroup = new ActionGroup("run.run.debug.group", "Run/Debug", false);
        runDebugGroup.addAll(runRun, runDebug, runCoverage, runProfiler);

        // Top-level Run/Debug ellipsis actions
        AnAction runRunEllipsis = AnAction.builder("run.run.ellipsis", "Run\u2026")
                .icon(FontAwesomeSolid.PLAY, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F10, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Run\u2026"))
                .build();
        AnAction runDebugEllipsis = AnAction.builder("run.debug.ellipsis", "Debug\u2026")
                .icon(FontAwesomeSolid.BUG, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F9, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Debug\u2026"))
                .build();

        // Group 2: XDebugger.AttachGroup (Image 2)
        AnAction attachToProcess = AnAction.builder("run.attach.to.process", "Attach to Process\u2026")
                .icon(FontAwesomeSolid.SYNC_ALT, "#57965c", 11)
                .onAction(ctx -> ctx.setStatus("Attach to Process\u2026"))
                .build();
        ActionGroup xdebuggerAttachGroup = new ActionGroup("run.xdebugger.attach.group", "XDebugger.AttachGroup", false);
        xdebuggerAttachGroup.add(attachToProcess);

        // Edit Configurations & Manage Targets
        AnAction editConfigurations = AnAction.builder("run.edit.configurations", "Edit Configurations\u2026")
                .description("Edit run and execution configurations")
                .onAction(MainWindow::showEditConfigurationsDialog)
                .build();
        AnAction manageTargets = AnAction.builder("run.manage.targets", "Manage Targets\u2026")
                .onAction(ctx -> ctx.setStatus("Manage Targets\u2026"))
                .build();

        // Stop actions
        AnAction runStop = AnAction.builder("run.stop", "Stop")
                .icon(FontAwesomeSolid.STOP, "#e05555", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F2, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Stop"))
                .build();
        AnAction stopBackgroundProcesses = AnAction.builder("run.stop.background.processes", "Stop Background Processes\u2026")
                .onAction(ctx -> ctx.setStatus("Stop Background Processes\u2026"))
                .build();
        AnAction showRunningList = AnAction.builder("run.show.running.list", "Show Running List")
                .onAction(ctx -> ctx.setStatus("Show Running List"))
                .build();

        // Group 3: Debugger Actions (Image 3)
        // DebugReloadGroup
        AnAction compileReloadModifiedFiles = AnAction.builder("run.compile.reload.modified.files", "Compile and Reload Modified Files")
                .accelerator(new KeyCodeCombination(KeyCode.F10, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Compile and Reload Modified Files"))
                .build();
        AnAction updateRunningApp = AnAction.builder("run.update.running.app", "Update Running Application")
                .icon(FontAwesomeSolid.SYNC_ALT, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F10, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Update Running Application"))
                .build();
        ActionGroup debugReloadGroup = new ActionGroup("run.debug.reload.group", "DebugReloadGroup", false);
        debugReloadGroup.addAll(compileReloadModifiedFiles, updateRunningApp);

        // StepOver.Ref
        AnAction stepOver = AnAction.builder("run.step.over", "Step Over")
                .icon(FontAwesomeSolid.STEP_FORWARD, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F8))
                .onAction(ctx -> ctx.setStatus("Step Over"))
                .build();
        AnAction forceStepOver = AnAction.builder("run.force.step.over", "Force Step Over")
                .icon(FontAwesomeSolid.STEP_FORWARD, "#e05555", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F8, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Force Step Over"))
                .build();
        AnAction stepInto = AnAction.builder("run.step.into", "Step Into")
                .icon(FontAwesomeSolid.ARROW_DOWN, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F7))
                .onAction(ctx -> ctx.setStatus("Step Into"))
                .build();
        AnAction forceStepInto = AnAction.builder("run.force.step.into", "Force Step Into")
                .icon(FontAwesomeSolid.ARROW_DOWN, "#e05555", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F7, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Force Step Into"))
                .build();
        AnAction smartStepInto = AnAction.builder("run.smart.step.into", "Smart Step Into")
                .icon(FontAwesomeSolid.ARROW_RIGHT, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F7, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Smart Step Into"))
                .build();
        AnAction stepOut = AnAction.builder("run.step.out", "Step Out")
                .icon(FontAwesomeSolid.ARROW_UP, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F8, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Step Out"))
                .build();
        AnAction runToCursor = AnAction.builder("run.run.to.cursor", "Run to Cursor")
                .icon(FontAwesomeSolid.CROSSHAIRS, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F9, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Run to Cursor"))
                .build();
        AnAction forceRunToCursor = AnAction.builder("run.force.run.to.cursor", "Force Run to Cursor")
                .icon(FontAwesomeSolid.CROSSHAIRS, "#e05555", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F9, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Force Run to Cursor"))
                .build();
        AnAction resetFrame = AnAction.builder("run.reset.frame", "Reset Frame")
                .icon(FontAwesomeSolid.UNDO, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Reset Frame"))
                .build();
        ActionGroup stepOverRef = new ActionGroup("run.step.over.ref", "StepOver.Ref", false);
        stepOverRef.addAll(stepOver, forceStepOver, stepInto, forceStepInto, smartStepInto, stepOut, runToCursor, forceRunToCursor, resetFrame);

        // Pause.Ref & Resume.Ref
        AnAction pauseProgram = AnAction.builder("run.pause.program", "Pause Program")
                .icon(FontAwesomeSolid.PAUSE, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Pause Program"))
                .build();
        ActionGroup pauseRef = new ActionGroup("run.pause.ref", "Pause.Ref", false);
        pauseRef.add(pauseProgram);

        AnAction resumeProgram = AnAction.builder("run.resume.program", "Resume Program")
                .icon(FontAwesomeSolid.FORWARD, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F9))
                .onAction(ctx -> ctx.setStatus("Resume Program"))
                .build();
        ActionGroup resumeRef = new ActionGroup("run.resume.ref", "Resume.Ref", false);
        resumeRef.add(resumeProgram);

        AnAction evaluateExpression = AnAction.builder("run.evaluate.expression", "Evaluate Expression\u2026")
                .icon(FontAwesomeSolid.CALCULATOR, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F8, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Evaluate Expression\u2026"))
                .build();
        AnAction showExecutionPoint = AnAction.builder("run.show.execution.point", "Show Execution Point")
                .icon(FontAwesomeSolid.CROSSHAIRS, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F10, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Show Execution Point"))
                .build();

        ActionGroup debuggingActionsGroup = new ActionGroup("run.debugging.actions.group", "Debugging Actions", false);
        debuggingActionsGroup.add(debugReloadGroup)
                .addSeparator()
                .addAll(stepOverRef, pauseRef, resumeRef)
                .addSeparator()
                .addAll(evaluateExpression, showExecutionPoint)
                .addSeparator();

        // Toggle Breakpoint
        AnAction restoreBreakpoint = AnAction.builder("run.restore.breakpoint", "Restore Breakpoint")
                .onAction(ctx -> ctx.setStatus("Restore Breakpoint"))
                .build();
        AnAction toggleLineBreakpoint = AnAction.builder("run.toggle.line.breakpoint", "Toggle Line Breakpoint")
                .accelerator(new KeyCodeCombination(KeyCode.F8, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Toggle Line Breakpoint"))
                .build();
        AnAction toggleTemporaryLineBreakpoint = AnAction.builder("run.toggle.temporary.line.breakpoint", "Toggle Temporary Line Breakpoint")
                .accelerator(new KeyCodeCombination(KeyCode.F8, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Toggle Temporary Line Breakpoint"))
                .build();
        ActionGroup toggleBreakpointGroup = new ActionGroup("run.toggle.breakpoint.group", "Toggle Breakpoint", false);
        toggleBreakpointGroup.addAll(restoreBreakpoint, toggleLineBreakpoint, toggleTemporaryLineBreakpoint);

        AnAction viewBreakpoints = AnAction.builder("run.view.breakpoints", "View Breakpoints\u2026")
                .icon(FontAwesomeSolid.CIRCLE, "#e05555", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F8, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("View Breakpoints\u2026"))
                .build();

        ActionGroup debuggerActionsGroup = new ActionGroup("run.debugger.actions.group", "Debugger Actions", false);
        debuggerActionsGroup.addSeparator()
                .addAll(debuggingActionsGroup, toggleBreakpointGroup, viewBreakpoints)
                .addSeparator();

        // Group 4: RunTestGroup (Image 4)
        AnAction testHistory = AnAction.builder("run.test.history", "Test History")
                .icon(FontAwesomeSolid.HISTORY, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Test History"))
                .build();
        AnAction importTestsFromFile = AnAction.builder("run.import.tests.from.file", "Import Tests from File\u2026")
                .icon(FontAwesomeSolid.DOWNLOAD, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Import Tests from File\u2026"))
                .build();
        ActionGroup smRunTestGroup = new ActionGroup("run.sm.run.test.group", "SmRunTestGroup", false);
        smRunTestGroup.addSeparator()
                .addAll(testHistory, importTestsFromFile);

        AnAction manageCoverageReports = AnAction.builder("run.manage.coverage.reports", "Manage Coverage Reports\u2026")
                .onAction(ctx -> ctx.setStatus("Manage Coverage Reports\u2026"))
                .build();
        AnAction generateCoverageReport = AnAction.builder("run.generate.coverage.report", "Generate Coverage Report\u2026")
                .icon(FontAwesomeSolid.SHARE_SQUARE, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Generate Coverage Report\u2026"))
                .build();
        AnAction hideCoverage = AnAction.builder("run.hide.coverage", "Hide Coverage")
                .onAction(ctx -> ctx.setStatus("Hide Coverage"))
                .build();
        ActionGroup coverageMenu = new ActionGroup("run.coverage.menu", "CoverageMenu", false);
        coverageMenu.addAll(manageCoverageReports, generateCoverageReport, hideCoverage)
                .addSeparator();

        ActionGroup coveragePlatformMenu = new ActionGroup("run.coverage.platform.menu", "CoveragePlatformMenu", false);
        coveragePlatformMenu.add(coverageMenu);

        ActionGroup runTestGroup = new ActionGroup("run.test.group", "RunTestGroup", false);
        runTestGroup.addAll(smRunTestGroup, coveragePlatformMenu);

        // Group 5: ProfilerActions (Image 2)
        AnAction attachProfilerToProcess = AnAction.builder("run.attach.profiler.to.process", "Attach Profiler to Process\u2026")
                .onAction(ctx -> ctx.setStatus("Attach Profiler to Process\u2026"))
                .build();
        AnAction openProfilerSnapshot = AnAction.builder("run.open.profiler.snapshot", "Open Profiler Snapshot")
                .onAction(ctx -> ctx.setStatus("Open Profiler Snapshot"))
                .build();
        ActionGroup profilerActionsGroup = new ActionGroup("run.profiler.actions.group", "ProfilerActions", false);
        profilerActionsGroup.addAll(attachProfilerToProcess, openProfilerSnapshot);

        // Legacy / Search & Compare Actions retained for compatibility
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
        AnAction fullTextSearch = AnAction.builder("run.fulltext.search", "Full-Text Search\u2026")
                .description("Full-text search across database tables and columns")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showFullTextSearchDialog)
                .build();
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
        runMenu.add(editConfigurations)
                .addSeparator()
                .add(compareData)
                .add(compareSchema)
                .addSeparator()
                .add(fullTextSearch);

        // Register actions and groups in ActionManager
        manager.registerAction(runRun);
        manager.registerAction(runDebug);
        manager.registerAction(runCoverage);
        manager.registerAction(runProfiler);
        manager.registerAction(runRunEllipsis);
        manager.registerAction(runDebugEllipsis);
        manager.registerAction(attachToProcess);
        manager.registerAction(editConfigurations);
        manager.registerAction(manageTargets);
        manager.registerAction(runStop);
        manager.registerAction(stopBackgroundProcesses);
        manager.registerAction(showRunningList);
        manager.registerAction(compileReloadModifiedFiles);
        manager.registerAction(updateRunningApp);
        manager.registerAction(stepOver);
        manager.registerAction(forceStepOver);
        manager.registerAction(stepInto);
        manager.registerAction(forceStepInto);
        manager.registerAction(smartStepInto);
        manager.registerAction(stepOut);
        manager.registerAction(runToCursor);
        manager.registerAction(forceRunToCursor);
        manager.registerAction(resetFrame);
        manager.registerAction(pauseProgram);
        manager.registerAction(resumeProgram);
        manager.registerAction(evaluateExpression);
        manager.registerAction(showExecutionPoint);
        manager.registerAction(restoreBreakpoint);
        manager.registerAction(toggleLineBreakpoint);
        manager.registerAction(toggleTemporaryLineBreakpoint);
        manager.registerAction(viewBreakpoints);
        manager.registerAction(testHistory);
        manager.registerAction(importTestsFromFile);
        manager.registerAction(manageCoverageReports);
        manager.registerAction(generateCoverageReport);
        manager.registerAction(hideCoverage);
        manager.registerAction(attachProfilerToProcess);
        manager.registerAction(openProfilerSnapshot);
        manager.registerAction(compareData);
        manager.registerAction(compareSchema);
        manager.registerAction(fullTextSearch);
        manager.registerAction(runStatement);
        manager.registerAction(runToolWindow);

        manager.registerGroup(runDebugGroup);
        manager.registerGroup(xdebuggerAttachGroup);
        manager.registerGroup(debugReloadGroup);
        manager.registerGroup(stepOverRef);
        manager.registerGroup(pauseRef);
        manager.registerGroup(resumeRef);
        manager.registerGroup(debuggingActionsGroup);
        manager.registerGroup(toggleBreakpointGroup);
        manager.registerGroup(debuggerActionsGroup);
        manager.registerGroup(smRunTestGroup);
        manager.registerGroup(coverageMenu);
        manager.registerGroup(coveragePlatformMenu);
        manager.registerGroup(runTestGroup);
        manager.registerGroup(profilerActionsGroup);
        manager.registerGroup(runMenu);

        // =========================================================================
        // 5.5. TOOLS ACTIONS (Matching DataGrip Image 1)
        // =========================================================================
        AnAction viewPsiStructure = AnAction.builder("tools.view.psi.structure", "View PSI Structure\u2026")
                .description("View the PSI structure of the project or file")
                .build();
        AnAction viewPsiStructureCurrentFile = AnAction.builder("tools.view.psi.structure.current.file", "View PSI Structure of Current File\u2026")
                .description("View the PSI structure of the active editor file")
                .build();
        AnAction createCommandLineLauncher = AnAction.builder("tools.create.command.line.launcher", "Create Command Line Launcher\u2026")
                .description("Create a command-line script to open files in this application")
                .build();
        AnAction createDesktopEntry = AnAction.builder("tools.create.desktop.entry", "Create Desktop Entry\u2026")
                .description("Create a desktop entry for the application")
                .build();
        AnAction toolsServices = AnAction.builder("tools.services", "Services")
                .description("Manage application and background services")
                .build();
        AnAction xmlConvertSchema = AnAction.builder("tools.convert.schema", "Convert Schema\u2026")
                .description("Convert XML/JSON/Database schema")
                .build();
        AnAction mdImportWord = AnAction.builder("tools.markdown.import.word", "Import Word Document\u2026")
                .description("Import Word document into Markdown")
                .build();
        AnAction mdExportFile = AnAction.builder("tools.markdown.export.file.to", "Export Markdown File To\u2026")
                .description("Export Markdown file to HTML, PDF, or Word")
                .build();
        AnAction mdConfigurePandoc = AnAction.builder("tools.markdown.configure.pandoc", "Configure Pandoc\u2026")
                .description("Configure Pandoc executable and options")
                .build();
        AnAction externalTools = AnAction.builder("tools.external.tools", "External Tools")
                .description("Manage and execute configured external tools")
                .build();

        manager.registerAction(viewPsiStructure);
        manager.registerAction(viewPsiStructureCurrentFile);
        manager.registerAction(createCommandLineLauncher);
        manager.registerAction(createDesktopEntry);
        manager.registerAction(toolsServices);
        manager.registerAction(xmlConvertSchema);
        manager.registerAction(mdImportWord);
        manager.registerAction(mdExportFile);
        manager.registerAction(mdConfigurePandoc);
        manager.registerAction(externalTools);

        ActionGroup psiViewerActionsGroup = new ActionGroup("tools.psi.viewer.actions", "Dev.PsiViewerActions", false);
        psiViewerActionsGroup.addSeparator();
        psiViewerActionsGroup.add(viewPsiStructure);
        psiViewerActionsGroup.add(viewPsiStructureCurrentFile);
        psiViewerActionsGroup.addSeparator();

        ActionGroup toolsBasicGroup = new ActionGroup("tools.basic.group", "Tools Basic Group", false);
        ActionGroup toolsOtherMenu = new ActionGroup("tools.other.menu", "OtherMenu", false);

        ActionGroup xmlActionsGroup = new ActionGroup("tools.xml.actions", "XML Actions", true);
        xmlActionsGroup.add(xmlConvertSchema);

        ActionGroup markdownGroup = new ActionGroup("tools.markdown", "Markdown", true);
        markdownGroup.addAll(mdImportWord, mdExportFile, mdConfigurePandoc);

        ActionGroup externalToolsGroup = new ActionGroup("tools.external.tools", "External Tools", true);

        ActionGroup toolsMenu = new ActionGroup("menu.tools", "Tools");
        toolsMenu.add(psiViewerActionsGroup)
                .add(toolsBasicGroup)
                .add(createCommandLineLauncher)
                .add(createDesktopEntry)
                .addSeparator()
                .add(toolsOtherMenu)
                .add(toolsServices)
                .add(xmlActionsGroup)
                .add(markdownGroup)
                .add(externalToolsGroup);

        manager.registerGroup(psiViewerActionsGroup);
        manager.registerGroup(toolsBasicGroup);
        manager.registerGroup(toolsOtherMenu);
        manager.registerGroup(xmlActionsGroup);
        manager.registerGroup(markdownGroup);
        manager.registerGroup(externalToolsGroup);
        manager.registerGroup(toolsMenu);

        // =========================================================================
        // 6. VCS ACTIONS (Matching DataGrip)
        // =========================================================================
        // Section 1: Integration & Operations Popup
        AnAction enableIntegration = AnAction.builder("vcs.enable.integration", "Enable Version Control Integration\u2026")
                .description("Select a version control system to associate with this project")
                .onAction(MainWindow::showEnableVcsIntegrationDialog)
                .build();

        AnAction vcsOperations = AnAction.builder("vcs.operations.popup", "VCS Operations Popup\u2026")
                .description("Show quick popup with VCS operations")
                .accelerator(new KeyCodeCombination(KeyCode.BACK_QUOTE, KeyCombination.ALT_DOWN))
                .onAction(MainWindow::showVcsOperationsPopup)
                .build();

        // Section 2: Patches
        AnAction applyPatch = AnAction.builder("vcs.apply.patch", "Apply Patch\u2026")
                .description("Apply a patch from file to project")
                .onAction(MainWindow::showApplyPatchDialog)
                .build();

        AnAction applyPatchClipboard = AnAction.builder("vcs.apply.patch.clipboard", "Apply Patch from Clipboard\u2026")
                .description("Apply a patch from clipboard to project")
                .onAction(MainWindow::showApplyPatchFromClipboardDialog)
                .build();

        // Section 3: VCS Checkout, Browse, Init
        AnAction getFromVcs = AnAction.builder("vcs.get.from.vcs", "Get from Version Control\u2026")
                .description("Clone repository from version control")
                .onAction(MainWindow::showGetFromVcsDialog)
                .build();

        ActionGroup browseVcsRepo = new ActionGroup("vcs.browse.repository", "Browse VCS Repository", true);
        AnAction showGitLog = AnAction.builder("vcs.browse.git.log", "Show Git Repository Log\u2026")
                .description("Show Git repository log and commit history")
                .onAction(MainWindow::showGitRepositoryLogDialog)
                .build();
        browseVcsRepo.add(showGitLog);

        AnAction createGitRepo = AnAction.builder("vcs.create.git.repository", "Create Git Repository\u2026")
                .description("Initialize a new Git repository")
                .onAction(MainWindow::showCreateGitRepositoryDialog)
                .build();

        // Section 4: VCS Group & Git Actions (Images 2-5)
        AnAction vcsCommit = AnAction.builder("vcs.commit", "Commit\u2026")
                .description("Commit changes to version control")
                .icon(FontAwesomeSolid.CHECK_CIRCLE, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN))
                .build();
        AnAction vcsToggleCommitUi = AnAction.builder("vcs.toggle.commit.ui", "Toggle Commit UI\u2026")
                .description("Toggle between commit modal dialog and tool window UI")
                .build();
        AnAction vcsUpdateProject = AnAction.builder("vcs.update.project", "Update Project")
                .description("Update project from remote version control")
                .icon(FontAwesomeSolid.DOWNLOAD, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN))
                .build();
        AnAction vcsIntegrateProject = AnAction.builder("vcs.integrate.project", "Integrate Project")
                .description("Integrate changes to a branch")
                .build();
        AnAction vcsRefresh = AnAction.builder("vcs.refresh", "Refresh")
                .description("Refresh VCS status")
                .icon(FontAwesomeSolid.SYNC_ALT, "#a9b7c6", 11)
                .build();
        AnAction vcsShowLocalChangesUml = AnAction.builder("vcs.show.local.changes.uml", "Show Local Changes as UML")
                .description("Show local changes diagram")
                .icon(FontAwesomeSolid.PROJECT_DIAGRAM, "#a9b7c6", 11)
                .build();

        // Git.FileActions (Image 4)
        AnAction gitCommitFile = AnAction.builder("git.commit.file", "Commit File")
                .description("Commit selected file")
                .build();
        AnAction gitAdd = AnAction.builder("git.add", "Add")
                .description("Add file to version control")
                .icon(FontAwesomeSolid.PLUS, "#57965c", 11)
                .accelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .build();
        AnAction gitAddToGitignore = AnAction.builder("git.add.to.gitignore", "Add to .gitignore")
                .description("Add file or pattern to .gitignore")
                .icon(FontAwesomeSolid.BAN, "#a9b7c6", 11)
                .build();
        AnAction gitAnnotate = AnAction.builder("git.annotate", "Annotate")
                .description("Show Git annotations / blame in editor gutter")
                .build();
        AnAction gitCompareSameVersion = AnAction.builder("git.compare.same.version", "Compare with Same Repository Version")
                .description("Compare working copy with repository version")
                .icon(FontAwesomeSolid.EXCHANGE_ALT, "#a9b7c6", 11)
                .build();
        AnAction gitCompareWithRevision = AnAction.builder("git.compare.with.revision", "Compare with Revision\u2026")
                .description("Compare working copy with a specific Git revision")
                .build();
        AnAction gitCompareWithBranch = AnAction.builder("git.compare.with.branch", "Compare with Branch or Tag\u2026")
                .description("Compare working copy with another branch or tag")
                .build();
        AnAction gitShowHistory = AnAction.builder("git.show.history", "Show History")
                .description("Show Git history for current file")
                .icon(FontAwesomeSolid.HISTORY, "#a9b7c6", 11)
                .build();
        AnAction gitShowHistoryForSelection = AnAction.builder("git.show.history.for.selection", "Show History for Selection\u2026")
                .description("Show Git history for selected code block")
                .build();

        // Rollback (Image 3, 4, 5)
        AnAction gitRollback = AnAction.builder("git.rollback", "Rollback\u2026")
                .description("Rollback local modifications")
                .icon(FontAwesomeSolid.UNDO, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .build();

        // GitRepositoryActions (Image 5)
        AnAction gitPush = AnAction.builder("git.push", "Push\u2026")
                .description("Push committed changes to remote repository")
                .icon(FontAwesomeSolid.UPLOAD, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .build();
        AnAction gitPull = AnAction.builder("git.pull", "Pull\u2026")
                .description("Pull changes from remote repository")
                .build();
        AnAction gitFetch = AnAction.builder("git.fetch", "Fetch")
                .description("Fetch remote branches and tags")
                .icon(FontAwesomeSolid.DOWNLOAD, "#a9b7c6", 11)
                .build();
        AnAction gitMerge = AnAction.builder("git.merge", "Merge\u2026")
                .description("Merge branches")
                .icon(FontAwesomeSolid.CODE_BRANCH, "#a9b7c6", 11)
                .build();
        AnAction gitAbortMerge = AnAction.builder("git.abort.merge", "Abort Merge")
                .description("Abort current merge in progress")
                .build();
        AnAction gitRebase = AnAction.builder("git.rebase", "Rebase\u2026")
                .description("Rebase current branch onto another branch")
                .build();
        AnAction gitAbortRebase = AnAction.builder("git.abort.rebase", "Abort Rebase")
                .description("Abort current rebase in progress")
                .build();
        AnAction gitContinueRebase = AnAction.builder("git.continue.rebase", "Continue Rebase")
                .description("Continue current rebase")
                .build();
        AnAction gitSkipCommit = AnAction.builder("git.skip.commit", "Skip Commit")
                .description("Skip current commit during rebase")
                .build();
        AnAction gitBranches = AnAction.builder("git.branches", "Branches\u2026")
                .description("Open Git Branches popup")
                .icon(FontAwesomeSolid.CODE_BRANCH, "#a9b7c6", 11)
                .build();
        AnAction gitNewBranch = AnAction.builder("git.new.branch", "New Branch\u2026")
                .description("Create a new Git branch")
                .icon(FontAwesomeSolid.PLUS, "#57965c", 11)
                .build();
        AnAction gitNewTag = AnAction.builder("git.new.tag", "New Tag\u2026")
                .description("Create a new Git tag")
                .build();
        AnAction gitResetHead = AnAction.builder("git.reset.head", "Reset HEAD\u2026")
                .description("Reset current HEAD to a specified state")
                .build();
        AnAction gitStashChanges = AnAction.builder("git.stash.changes", "Stash Changes\u2026")
                .description("Stash local modifications")
                .build();
        AnAction gitUnstashChanges = AnAction.builder("git.unstash.changes", "Unstash Changes\u2026")
                .description("Apply stashed modifications")
                .build();
        AnAction gitManageRemotes = AnAction.builder("git.manage.remotes", "Manage Remotes\u2026")
                .description("Manage Git remote repositories")
                .build();
        AnAction gitClone = AnAction.builder("git.clone", "Clone\u2026")
                .description("Clone a remote repository")
                .build();
        AnAction gitAbortRevert = AnAction.builder("git.abort.revert", "Abort Revert")
                .description("Abort revert in progress")
                .build();
        AnAction gitAbortCherryPick = AnAction.builder("git.abort.cherry.pick", "Abort Cherry-Pick")
                .description("Abort cherry-pick in progress")
                .build();

        // Patch & Shelve actions (Image 3, 4, 5)
        AnAction gitCreatePatch = AnAction.builder("git.create.patch", "Create Patch from Local Changes\u2026")
                .description("Create a patch file from local changes")
                .icon(FontAwesomeSolid.PLUS, "#a9b7c6", 11)
                .build();
        AnAction gitApplyPatch = AnAction.builder("git.apply.patch", "Apply Patch\u2026")
                .description("Apply patch to workspace")
                .build();
        AnAction gitApplyPatchClipboard = AnAction.builder("git.apply.patch.from.clipboard", "Apply Patch from Clipboard\u2026")
                .description("Apply patch from clipboard to workspace")
                .build();
        AnAction gitShelveChanges = AnAction.builder("git.shelve.changes", "Shelve Changes\u2026")
                .description("Shelve local changes")
                .icon(FontAwesomeSolid.SAVE, "#a9b7c6", 11)
                .build();
        AnAction vcsImportIntoVcs = AnAction.builder("vcs.import.into.vcs", "Import into Version Control")
                .description("Import project or file into version control")
                .build();

        // Section 5: Additional Git.MainMenu Actions (Images 1-4)
        AnAction gitMainCommit = AnAction.builder("git.main.commit", "Commit\u2026")
                .description("Commit project changes")
                .icon(FontAwesomeSolid.CHECK_CIRCLE, "#a9b7c6", 11)
                .accelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN))
                .build();
        AnAction gitMainToggleCommitUi = AnAction.builder("git.main.toggle.commit.ui", "Commit\u2026")
                .description("Toggle commit UI")
                .icon(FontAwesomeSolid.CHECK_CIRCLE, "#a9b7c6", 11)
                .build();
        AnAction gitUnshallow = AnAction.builder("git.unshallow", "Unshallow repository")
                .description("Convert shallow clone to full repository")
                .build();
        AnAction gitResolveConflicts = AnAction.builder("git.resolve.conflicts", "Resolve Conflicts\u2026")
                .description("Resolve merge or rebase conflicts")
                .build();
        AnAction gitRevertResolved = AnAction.builder("git.revert.resolved", "Revert Resolved")
                .description("Revert resolved state")
                .icon(FontAwesomeSolid.UNDO, "#a9b7c6", 11)
                .build();
        AnAction gitShowVcsLog = AnAction.builder("git.show.vcs.log", "Show VCS Log")
                .description("Show Git log and history tool window")
                .icon(FontAwesomeSolid.CODE_BRANCH, "#a9b7c6", 11)
                .build();
        AnAction gitShowShelf = AnAction.builder("git.show.shelf", "Show Shelf")
                .description("Open Shelf view")
                .build();
        AnAction gitShowGitStash = AnAction.builder("git.show.git.stash", "Show Git Stash")
                .description("Open Git Stash view")
                .build();

        manager.registerAction(vcsCommit);
        manager.registerAction(vcsToggleCommitUi);
        manager.registerAction(vcsUpdateProject);
        manager.registerAction(vcsIntegrateProject);
        manager.registerAction(vcsRefresh);
        manager.registerAction(vcsShowLocalChangesUml);
        manager.registerAction(gitCommitFile);
        manager.registerAction(gitAdd);
        manager.registerAction(gitAddToGitignore);
        manager.registerAction(gitAnnotate);
        manager.registerAction(gitCompareSameVersion);
        manager.registerAction(gitCompareWithRevision);
        manager.registerAction(gitCompareWithBranch);
        manager.registerAction(gitShowHistory);
        manager.registerAction(gitShowHistoryForSelection);
        manager.registerAction(gitRollback);
        manager.registerAction(gitPush);
        manager.registerAction(gitPull);
        manager.registerAction(gitFetch);
        manager.registerAction(gitMerge);
        manager.registerAction(gitAbortMerge);
        manager.registerAction(gitRebase);
        manager.registerAction(gitAbortRebase);
        manager.registerAction(gitContinueRebase);
        manager.registerAction(gitSkipCommit);
        manager.registerAction(gitBranches);
        manager.registerAction(gitNewBranch);
        manager.registerAction(gitNewTag);
        manager.registerAction(gitResetHead);
        manager.registerAction(gitStashChanges);
        manager.registerAction(gitUnstashChanges);
        manager.registerAction(gitManageRemotes);
        manager.registerAction(gitClone);
        manager.registerAction(gitAbortRevert);
        manager.registerAction(gitAbortCherryPick);
        manager.registerAction(gitCreatePatch);
        manager.registerAction(gitApplyPatch);
        manager.registerAction(gitApplyPatchClipboard);
        manager.registerAction(gitShelveChanges);
        manager.registerAction(vcsImportIntoVcs);
        manager.registerAction(gitMainCommit);
        manager.registerAction(gitMainToggleCommitUi);
        manager.registerAction(gitUnshallow);
        manager.registerAction(gitResolveConflicts);
        manager.registerAction(gitRevertResolved);
        manager.registerAction(gitShowVcsLog);
        manager.registerAction(gitShowShelf);
        manager.registerAction(gitShowGitStash);

        ActionGroup gitFileActionsGroup = new ActionGroup("git.file.actions", "Git.FileActions", false);
        gitFileActionsGroup.addAll(gitCommitFile, gitAdd, gitAddToGitignore);
        gitFileActionsGroup.addSeparator();
        gitFileActionsGroup.addAll(gitAnnotate, gitCompareSameVersion, gitCompareWithRevision, gitCompareWithBranch, gitShowHistory, gitShowHistoryForSelection);

        ActionGroup gitMergeGroup = new ActionGroup("git.merge.group", "Merge", true);
        gitMergeGroup.add(gitAbortMerge);

        ActionGroup gitRebaseGroup = new ActionGroup("git.rebase.group", "Rebase", true);
        gitRebaseGroup.addAll(gitAbortRebase, gitContinueRebase, gitSkipCommit);

        ActionGroup gitRepoActionsGroup = new ActionGroup("git.repository.actions", "GitRepositoryActions", false);
        gitRepoActionsGroup.addAll(gitPush, gitPull, gitFetch);
        gitRepoActionsGroup.addSeparator();
        gitRepoActionsGroup.add(gitMerge);
        gitRepoActionsGroup.add(gitMergeGroup);
        gitRepoActionsGroup.add(gitRebase);
        gitRepoActionsGroup.add(gitRebaseGroup);
        gitRepoActionsGroup.addSeparator();
        gitRepoActionsGroup.addAll(gitBranches, gitNewBranch, gitNewTag, gitResetHead);
        gitRepoActionsGroup.addSeparator();
        gitRepoActionsGroup.addAll(gitStashChanges, gitUnstashChanges);
        gitRepoActionsGroup.addSeparator();
        gitRepoActionsGroup.addAll(gitManageRemotes, gitClone);
        gitRepoActionsGroup.addSeparator();
        gitRepoActionsGroup.addAll(gitAbortRevert, gitAbortCherryPick);

        ActionGroup vcsGitGroup = new ActionGroup("vcs.git.group", "Git", false);
        vcsGitGroup.add(gitFileActionsGroup);
        vcsGitGroup.addSeparator();
        vcsGitGroup.add(gitRollback);
        vcsGitGroup.addSeparator();
        vcsGitGroup.add(gitRepoActionsGroup);
        vcsGitGroup.addSeparator();
        vcsGitGroup.addAll(gitCreatePatch, gitApplyPatch, gitApplyPatchClipboard, gitShelveChanges);

        ActionGroup vcsSpecificGroup = new ActionGroup("vcs.specific", "Vcs.Specific", false);

        ActionGroup vcsGroup = new ActionGroup("vcs.group", "VCS Group", false);
        vcsGroup.addAll(vcsOperations, vcsCommit, vcsToggleCommitUi, vcsUpdateProject, vcsIntegrateProject, vcsRefresh, vcsShowLocalChangesUml);
        vcsGroup.addSeparator();
        vcsGroup.add(vcsSpecificGroup);
        vcsGroup.add(vcsGitGroup);

        ActionGroup vcsImportGroup = new ActionGroup("vcs.import.into.vcs", "Import into Version Control", true);

        ActionGroup vcsMainMenu = new ActionGroup("vcs.main.menu", "Vcs.MainMenu", false);
        vcsMainMenu.add(enableIntegration);
        vcsMainMenu.addSeparator();
        vcsMainMenu.add(vcsGroup);
        vcsMainMenu.addSeparator();
        vcsMainMenu.add(getFromVcs);
        vcsMainMenu.add(browseVcsRepo);
        vcsMainMenu.addSeparator();
        vcsMainMenu.add(vcsImportGroup);

        // Git.MainMenu groups (Images 1-4)
        ActionGroup gitPatchGroup = new ActionGroup("git.patch.group", "Patch", true);
        gitPatchGroup.addAll(gitCreatePatch, gitApplyPatch, gitApplyPatchClipboard);

        ActionGroup vcsUmlDiffGroup = new ActionGroup("vcs.uml.diff", "Vcs.UmlDiff", false);
        vcsUmlDiffGroup.add(vcsShowLocalChangesUml);

        ActionGroup gitUncommittedChangesGroup = new ActionGroup("git.uncommitted.changes.group", "Uncommitted Changes", true);
        gitUncommittedChangesGroup.addAll(gitShelveChanges, gitShowShelf, gitShowGitStash, gitStashChanges, gitUnstashChanges, gitRollback, vcsUmlDiffGroup);

        ActionGroup gitFileActionsRefGroup = new ActionGroup("git.file.actions.ref", "Git.FileActions", false);
        gitFileActionsRefGroup.add(gitFileActionsGroup);

        ActionGroup gitMainMenuFileActions = new ActionGroup("git.main.menu.file.actions", "Git.MainMenu.FileActions", false);
        gitMainMenuFileActions.add(gitFileActionsRefGroup);

        ActionGroup gitMainMergeGroup = new ActionGroup("git.main.merge.group", "Merge", true);
        gitMainMergeGroup.add(gitAbortMerge);

        ActionGroup gitMainRebaseGroup = new ActionGroup("git.main.rebase.group", "Rebase", true);
        gitMainRebaseGroup.addAll(gitAbortRebase, gitContinueRebase, gitSkipCommit);

        ActionGroup gitMainMenu = new ActionGroup("git.main.menu", "Git.MainMenu", false);
        gitMainMenu.add(gitMainCommit);
        gitMainMenu.add(gitMainToggleCommitUi);
        gitMainMenu.add(gitPush);
        gitMainMenu.add(vcsUpdateProject);
        gitMainMenu.add(gitPull);
        gitMainMenu.add(gitFetch);
        gitMainMenu.add(gitUnshallow);
        gitMainMenu.addSeparator();
        gitMainMenu.add(gitMerge);
        gitMainMenu.add(gitMainMergeGroup);
        gitMainMenu.add(gitRebase);
        gitMainMenu.add(gitMainRebaseGroup);
        gitMainMenu.add(gitResolveConflicts);
        gitMainMenu.add(gitRevertResolved);
        gitMainMenu.addSeparator();
        gitMainMenu.add(gitBranches);
        gitMainMenu.add(gitNewBranch);
        gitMainMenu.add(gitNewTag);
        gitMainMenu.add(gitResetHead);
        gitMainMenu.addSeparator();
        gitMainMenu.add(gitShowVcsLog);
        gitMainMenu.add(gitPatchGroup);
        gitMainMenu.add(gitUncommittedChangesGroup);
        gitMainMenu.add(gitMainMenuFileActions);
        gitMainMenu.addSeparator();
        gitMainMenu.add(gitManageRemotes);
        gitMainMenu.add(gitClone);
        gitMainMenu.addSeparator();
        gitMainMenu.add(vcsOperations);
        gitMainMenu.addSeparator();
        gitMainMenu.add(gitAbortRevert);
        gitMainMenu.add(gitAbortCherryPick);

        manager.registerGroup(gitFileActionsGroup);
        manager.registerGroup(gitMergeGroup);
        manager.registerGroup(gitRebaseGroup);
        manager.registerGroup(gitRepoActionsGroup);
        manager.registerGroup(vcsGitGroup);
        manager.registerGroup(vcsSpecificGroup);
        manager.registerGroup(vcsGroup);
        manager.registerGroup(vcsImportGroup);
        manager.registerGroup(vcsMainMenu);
        manager.registerGroup(gitPatchGroup);
        manager.registerGroup(vcsUmlDiffGroup);
        manager.registerGroup(gitUncommittedChangesGroup);
        manager.registerGroup(gitFileActionsRefGroup);
        manager.registerGroup(gitMainMenuFileActions);
        manager.registerGroup(gitMainMergeGroup);
        manager.registerGroup(gitMainRebaseGroup);
        manager.registerGroup(gitMainMenu);

        // Keep legacy vcsMenu with 9 children for ActionRegistryTest backward compatibility
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

        // Background Tasks (DataGrip Image 3)
        AnAction bgTasksShow = AnAction.builder("window.background.tasks.show", "Show")
                .onAction(MainWindow::showProcesses).build();
        AnAction bgTasksAutoShow = AnAction.builder("window.background.tasks.auto.show", "Auto Show")
                .onAction(MainWindow::toggleAutoShowProcesses).build();
        ActionGroup bgTasksGroup = new ActionGroup("window.background.tasks", "Background Tasks", true);
        bgTasksGroup.addAll(bgTasksShow, bgTasksAutoShow);
        manager.registerGroup(bgTasksGroup);

        // Top-Level Window Menu
        AnAction minimizeAction = AnAction.builder("window.minimize", "Minimize")
                .accelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::minimizeWindow)
                .build();
        manager.registerAction(minimizeAction);

        AnAction zoomAction = AnAction.builder("window.zoom", "Zoom")
                .onAction(MainWindow::zoomWindow)
                .build();
        manager.registerAction(zoomAction);

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

        // Additional Tool Window Layouts actions (Image 2)
        AnAction layoutList = AnAction.builder("window.layouts.list", "Tool Window Layout List").build();
        AnAction restoreCurrentLayout = AnAction.builder("window.layouts.restore.current", "Restore Current Layout")
                .accelerator(new KeyCodeCombination(KeyCode.F12, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::restoreCurrentLayout).build();
        AnAction saveChangesCurrentLayout = AnAction.builder("window.layouts.save.changes.current", "Save Changes in Current Layout")
                .onAction(MainWindow::saveChangesIntoCurrentLayout).build();
        manager.registerAction(layoutList);
        manager.registerAction(restoreCurrentLayout);
        manager.registerAction(saveChangesCurrentLayout);

        // Additional Active Tool Window actions (Image 3)
        AnAction openAsEditorTabTw = AnAction.builder("window.toolwindow.open.as.editor.tab", "Open as Editor Tab")
                .icon(FontAwesomeSolid.SHARE_SQUARE, "#a9b7c6", 11)
                .onAction(MainWindow::openActiveToolWindowAsEditorTab).build();
        AnAction pinActiveTwTab = AnAction.builder("window.toolwindow.pin.tab", "Pin Active Tool Window Tab")
                .onAction(MainWindow::pinActiveToolWindowTab).build();
        AnAction dockTw = AnAction.builder("window.toolwindow.dock", "Dock")
                .onAction(MainWindow::dockActiveToolWindow).build();
        AnAction showListOfTabs = AnAction.builder("window.toolwindow.show.list.of.tabs", "Show List of Tabs")
                .onAction(MainWindow::showListOfTabs).build();
        manager.registerAction(openAsEditorTabTw);
        manager.registerAction(pinActiveTwTab);
        manager.registerAction(dockTw);
        manager.registerAction(showListOfTabs);

        // Additional Editor Tabs actions & Editor Close Actions (Image 4)
        AnAction showHiddenTabs = AnAction.builder("window.editor.show.hidden.tabs", "Show Hidden Tabs")
                .onAction(MainWindow::showHiddenTabs).build();
        AnAction editorOpenAsEditorTab = AnAction.builder("window.editor.open.as.editor.tab", "Open as Editor Tab")
                .icon(FontAwesomeSolid.SHARE_SQUARE, "#a9b7c6", 11)
                .onAction(MainWindow::openActiveToolWindowAsEditorTab).build();
        manager.registerAction(showHiddenTabs);
        manager.registerAction(editorOpenAsEditorTab);

        ActionGroup editorCloseActionsGroup = new ActionGroup("window.editor.close.actions", "Editor Close Actions", true);
        editorCloseActionsGroup.addAll(closeTab, closeOtherTabs, closeAllTabs, closeUnmodified, closeAllButPinned,
                closeTabsLeft, closeTabsRight, closeAllReadOnly, editorOpenAsEditorTab);
        manager.registerGroup(editorCloseActionsGroup);

        // Split with Chooser Navigation actions & group (Image 5)
        AnAction splitAndMoveRight = AnAction.builder("window.split.and.move.right", "Split and Move Right")
                .onAction(MainWindow::splitAndMoveRight).build();
        AnAction splitAndMoveDown = AnAction.builder("window.split.and.move.down", "Split and Move Down")
                .onAction(MainWindow::splitAndMoveDown).build();
        AnAction splitChooserOpen = AnAction.builder("window.split.chooser.open", "Open in Split with Chooser…")
                .onAction(MainWindow::openInSplitWithChooser).build();
        manager.registerAction(splitAndMoveRight);
        manager.registerAction(splitAndMoveDown);
        manager.registerAction(splitChooserOpen);

        AnAction splitNext = AnAction.builder("window.split.next", "Next Split").build();
        AnAction splitPrev = AnAction.builder("window.split.prev", "Previous Split").build();
        AnAction exitChooser = AnAction.builder("window.split.exit.chooser", "Exit Chooser").build();
        AnAction chooserSplit = AnAction.builder("window.split.chooser.split", "Split").build();
        AnAction chooserDuplicate = AnAction.builder("window.split.chooser.duplicate", "Duplicate").build();
        AnAction chooserWithoutSplit = AnAction.builder("window.split.chooser.without.split", "Without Split").build();
        AnAction switchUp = AnAction.builder("window.split.switch.up", "Use Top Split or Switch Up").build();
        AnAction switchLeft = AnAction.builder("window.split.switch.left", "Use Left Split or Switch Left").build();
        AnAction switchDown = AnAction.builder("window.split.switch.down", "Use down Split or Switch Down").build();
        AnAction switchRight = AnAction.builder("window.split.switch.right", "Use Right Split or Switch Right").build();

        ActionGroup splitChooserNavGroup = new ActionGroup("window.editor.split.chooser.navigation", "Split with Chooser Navigation", true);
        splitChooserNavGroup.addAll(splitNext, splitPrev, exitChooser, chooserSplit, chooserDuplicate, chooserWithoutSplit,
                switchUp, switchLeft, switchDown, switchRight);
        manager.registerGroup(splitChooserNavGroup);

        // Open Project Windows group (Image 2)
        AnAction mergeAllProj = AnAction.builder("window.project.merge.all", "Merge All Project Windows")
                .onAction(MainWindow::mergeAllProjectWindows).build();
        manager.registerAction(mergeAllProj);
        ActionGroup openProjectWindowsGroup = new ActionGroup("window.open.project.windows", "Open Project Windows", true);
        openProjectWindowsGroup.addAll(nextProjectWindow, prevProjectWindow, mergeAllProj);
        manager.registerGroup(openProjectWindowsGroup);

        // Legacy Window menu structure for ActionRegistryTest
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

        // Additional Help actions and sub-groups matching DataGrip
        AnAction configureNewUi = AnAction.builder("help.configure.new.ui", "Configure the New UI")
                .onAction(MainWindow::configureNewUi)
                .build();
        manager.registerAction(configureNewUi);

        AnAction runMemoryTester = AnAction.builder("help.diagnostic.run.memory.tester", "Run Memory Tester…")
                .onAction(MainWindow::runMemoryTester)
                .build();
        manager.registerAction(runMemoryTester);

        AnAction registerLicense = AnAction.builder("help.registration.register", "Register…")
                .onAction(MainWindow::showRegistrationDialog)
                .build();
        manager.registerAction(registerLicense);

        ActionGroup productivityGroup = new ActionGroup("help.productivity.features", "ProductivityFeatures", true);
        productivityGroup.addAll(tipOfDay).addSeparator().add(myProductivity);
        manager.registerGroup(productivityGroup);

        ActionGroup asyncGroup = new ActionGroup("help.diagnostic.async.group", "AsyncGroup", false);
        asyncGroup.addSeparator().addAll(cpuProfiling, AnAction.builder("help.diagnostic.start.async.profiler", "Start Async Profiler").onAction(MainWindow::startAsyncProfiler).build());
        manager.registerGroup(asyncGroup);

        ActionGroup startProfileGroup = new ActionGroup("help.diagnostic.start.profile.group", "StartProfileGroup", false);
        startProfileGroup.add(asyncGroup);
        manager.registerGroup(startProfileGroup);

        ActionGroup asyncDiagGroup = new ActionGroup("help.diagnostic.async.diagnostic.group", "AsyncDiagnosticGroup", false);
        asyncDiagGroup.add(AnAction.builder("help.diagnostic.profile.indexing", "Profile Indexing").onAction(MainWindow::profileIndexing).build());
        manager.registerGroup(asyncDiagGroup);

        ActionGroup diagGroup = new ActionGroup("help.diagnostic.diagnostic.group", "DiagnosticGroup", false);
        diagGroup.addSeparator().add(captureMemory).addSeparator().add(asyncDiagGroup);
        manager.registerGroup(diagGroup);

        ActionGroup indexingDiagGroup = new ActionGroup("help.diagnostic.indexing.diagnostic.group", "IndexingDiagnosticGroup", false);
        indexingDiagGroup.add(AnAction.builder("help.diagnostic.open.indexing.diagnostics", "Open Indexing Diagnostics").onAction(MainWindow::openIndexingDiagnostics).build());
        manager.registerGroup(indexingDiagGroup);

        ActionGroup registrationGroup = new ActionGroup("help.registration.actions", "Registration Actions", true);
        registrationGroup.add(registerLicense);
        manager.registerGroup(registrationGroup);

        ActionGroup learnGroup = new ActionGroup("help.learn.group", "LearnGroup", false);
        learnGroup.add(learnFeatures);
        manager.registerGroup(learnGroup);

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

        registerMainToolbarActions(manager);
        registerEditorPopupMenuActions(manager);
        registerEditorGutterPopupMenuActions(manager);
        registerEditorTabPopupMenuActions(manager);
        registerProjectViewPopupMenuActions(manager);
        registerNavBarAndDebugAndHistoryToolbarsActions(manager);
        registerFloatingCodeAndQuickActionsAndSqlToolbarActions(manager);
    }

    private static void registerMainToolbarActions(ActionManager manager) {
        // Toolbar Left
        AnAction projectWidget = AnAction.builder("toolbar.project.widget", "Project Widget")
                .description("Focus or toggle Project / Schema Explorer")
                .icon(FontAwesomeSolid.FOLDER, "#a9b7c6", 13)
                .onAction(MainWindow::focusOrToggleSchemaExplorer)
                .build();
        AnAction vcsWidget = AnAction.builder("toolbar.vcs.widget", "VCS Widget")
                .description("Toggle Version Control Tool Window")
                .icon(FontAwesomeSolid.CODE_BRANCH, "#4a88c7", 13)
                .onAction(MainWindow::toggleVersionControlToolWindow)
                .build();
        AnAction vcsMergeRebase = AnAction.builder("toolbar.vcs.merge.rebase.widget", "VCS Merge/Rebase Widget")
                .description("VCS Merge and Rebase operations")
                .icon(FontAwesomeSolid.CODE_BRANCH, "#6897bb", 13)
                .onAction(MainWindow::showVcsOperationsPopup)
                .build();
        ActionGroup vcsGroup = new ActionGroup("toolbar.vcs.group", "VCS Group", true);
        vcsGroup.addAll(vcsWidget, vcsMergeRebase);

        ActionGroup generalActions = new ActionGroup("toolbar.general.actions.group", "General Actions", false);
        generalActions.addSeparator();

        // Toolbar Center
        AnAction singleToolWindow = AnAction.builder("toolbar.single.tool.window.bar", "Single Tool Window Bar")
                .description("Single Tool Window Bar")
                .icon(FontAwesomeSolid.DESKTOP, "#a9b7c6", 13)
                .onAction(MainWindow::toggleCompactMode)
                .build();
        AnAction fileNameWidget = AnAction.builder("toolbar.file.name.widget", "File Name Widget (when editor tabs are hidden or \\\"Always show full path\\\" is enabled)")
                .description("Show File Name / Path")
                .icon(FontAwesomeSolid.FILE_ALT, "#a9b7c6", 13)
                .onAction(MainWindow::showFilePathPopup)
                .build();

        // Toolbar Right
        ActionGroup executionTargets = new ActionGroup("toolbar.execution.targets.group", "ExecutionTargetsToolbarGroup", true,
                FontAwesomeSolid.FOLDER, "#e0a44c", 13, "Execution Targets");
        AnAction aiAssistant = AnAction.builder("toolbar.ai.assistant.action", "AIAssistantHubPopupAction")
                .description("AI Assistant Hub")
                .icon(FontAwesomeSolid.ROBOT, "#9876aa", 13)
                .onAction(MainWindow::toggleAiAssistantToolWindow)
                .build();
        AnAction searchEverywhere = AnAction.builder("toolbar.search.everywhere", "Search Everywhere")
                .description("Search Everywhere (Double Shift)")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 13)
                .accelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::showSearchEverywhere)
                .build();
        AnAction ideProjectSettings = AnAction.builder("toolbar.ide.project.settings", "IDE and Project Settings")
                .description("Open Settings Dialog")
                .icon(FontAwesomeSolid.COG, "#a9b7c6", 13)
                .onAction(MainWindow::showSettingsDialog)
                .build();

        manager.registerAction(projectWidget);
        manager.registerAction(vcsWidget);
        manager.registerAction(vcsMergeRebase);
        manager.registerGroup(vcsGroup);
        manager.registerGroup(generalActions);
        manager.registerAction(singleToolWindow);
        manager.registerAction(fileNameWidget);
        manager.registerGroup(executionTargets);
        manager.registerAction(aiAssistant);
        manager.registerAction(searchEverywhere);
        manager.registerAction(ideProjectSettings);
    }

    private static void registerEditorPopupMenuActions(ActionManager manager) {
        // ShowContextActions & ShowIntentionsGroup
        AnAction showCtxActions = AnAction.builder("editor.popup.show.context.actions", "Show Context Actions")
                .icon(FontAwesomeSolid.LIGHTBULB, "#ffc66d", 12)
                .accelerator(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Show Context Actions"))
                .build();
        ActionGroup showIntentions = new ActionGroup("editor.popup.show.intentions", "ShowIntentionsGroup", false);
        showIntentions.add(showCtxActions).addSeparator();

        // Open File in Project... & LightEditModePopup
        AnAction openFileInProj = AnAction.builder("editor.popup.open.file.in.project", "Open File in Project…")
                .icon(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 12)
                .onAction(MainWindow::openSqlFile)
                .build();
        ActionGroup lightEditMode = new ActionGroup("editor.popup.light.edit.mode", "LightEditModePopup", false);
        lightEditMode.add(openFileInProj).addSeparator();

        // Copy / Paste Special
        ActionGroup copyPasteSpecial = new ActionGroup("editor.popup.copy.paste.special", "Copy / Paste Special", true);
        AnAction refAction = manager.getAction("edit.copy.reference");
        if (refAction != null) copyPasteSpecial.add(refAction);
        AnAction gitHostingAction = manager.getAction("edit.copy.git.hosting.link");
        if (gitHostingAction != null) copyPasteSpecial.add(gitHostingAction);
        AnAction plainAction = manager.getAction("edit.copy.plain");
        if (plainAction != null) copyPasteSpecial.add(plainAction);
        AnAction pastePlainAction = manager.getAction("edit.paste.plain");
        if (pastePlainAction != null) copyPasteSpecial.add(pastePlainAction);
        AnAction pasteHistAction = manager.getAction("edit.paste.history");
        if (pasteHistAction != null) copyPasteSpecial.add(pasteHistAction);

        // Column Selection Mode
        AnAction colSelectionMode = AnAction.builder("edit.column.selection.mode", "Column Selection Mode")
                .onAction(ctx -> ctx.setStatus("Column Selection Mode"))
                .build();

        // Markdown Table Actions & Groups
        AnAction insColLeft = AnAction.builder("markdown.table.insert.col.left", "Insert Column Left")
                .icon(FontAwesomeSolid.PLUS, "#57965c", 11)
                .onAction(ctx -> ctx.setStatus("Insert Column Left"))
                .build();
        AnAction insColRight = AnAction.builder("markdown.table.insert.col.right", "Insert Column Right")
                .icon(FontAwesomeSolid.PLUS, "#57965c", 11)
                .onAction(ctx -> ctx.setStatus("Insert Column Right"))
                .build();
        AnAction insRowAbove = AnAction.builder("markdown.table.insert.row.above", "Insert Row Above")
                .icon(FontAwesomeSolid.ARROW_UP, "#57965c", 11)
                .onAction(ctx -> ctx.setStatus("Insert Row Above"))
                .build();
        AnAction insRowBelow = AnAction.builder("markdown.table.insert.row.below", "Insert Row Below")
                .icon(FontAwesomeSolid.ARROW_DOWN, "#57965c", 11)
                .onAction(ctx -> ctx.setStatus("Insert Row Below"))
                .build();

        AnAction alignLeft = AnAction.builder("markdown.table.align.left", "Align Left")
                .icon(FontAwesomeSolid.ALIGN_LEFT, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Align Left"))
                .build();
        AnAction alignCenter = AnAction.builder("markdown.table.align.center", "Align Center")
                .icon(FontAwesomeSolid.ALIGN_CENTER, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Align Center"))
                .build();
        AnAction alignRight = AnAction.builder("markdown.table.align.right", "Align Right")
                .icon(FontAwesomeSolid.ALIGN_RIGHT, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Align Right"))
                .build();
        ActionGroup setColAlign = new ActionGroup("markdown.table.set.col.alignment.group", "Set Column Alignment", true);
        setColAlign.addAll(alignLeft, alignCenter, alignRight);

        AnAction moveColLeft = AnAction.builder("markdown.table.move.col.left", "Move Column Left")
                .icon(FontAwesomeSolid.ARROW_LEFT, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Move Column Left"))
                .build();
        AnAction moveColRight = AnAction.builder("markdown.table.move.col.right", "Move Column Right")
                .icon(FontAwesomeSolid.ARROW_RIGHT, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Move Column Right"))
                .build();
        AnAction removeCol = AnAction.builder("markdown.table.remove.col", "Remove Column")
                .icon(FontAwesomeSolid.TRASH, "#e06c75", 11)
                .onAction(ctx -> ctx.setStatus("Remove Column"))
                .build();
        AnAction removeRow = AnAction.builder("markdown.table.remove.row", "Remove Row")
                .icon(FontAwesomeSolid.TRASH, "#e06c75", 11)
                .onAction(ctx -> ctx.setStatus("Remove Row"))
                .build();

        ActionGroup colAlignGroup = new ActionGroup("markdown.table.col.alignment.group", "Column Alignment", true);
        colAlignGroup.add(setColAlign).addAll(moveColLeft, moveColRight).addSeparator().addAll(removeCol, removeRow);

        AnAction insGeneric = AnAction.builder("markdown.table.insert", "Insert…")
                .onAction(ctx -> ctx.setStatus("Insert…"))
                .build();

        ActionGroup tableGroup = new ActionGroup("markdown.table.group", "Table", true);
        tableGroup.addAll(insColLeft, insColRight, insRowAbove, insRowBelow)
                .addSeparator()
                .add(colAlignGroup)
                .add(insGeneric);

        ActionGroup mdEditorGroup = new ActionGroup("editor.popup.markdown.group", "Markdown.EditorContextMenuGroup", false);
        mdEditorGroup.addSeparator().add(tableGroup).addSeparator();

        // Console Table Result Navigation Actions
        AnAction navFirst = AnAction.builder("console.table.navigate.first", "First Page")
                .icon(FontAwesomeSolid.FAST_BACKWARD, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("First Page"))
                .build();
        AnAction navPrev = AnAction.builder("console.table.navigate.prev", "Previous Page")
                .icon(FontAwesomeSolid.STEP_BACKWARD, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Previous Page"))
                .build();
        AnAction navNext = AnAction.builder("console.table.navigate.next", "Next Page")
                .icon(FontAwesomeSolid.STEP_FORWARD, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Next Page"))
                .build();
        AnAction navLast = AnAction.builder("console.table.navigate.last", "Last Page")
                .icon(FontAwesomeSolid.FAST_FORWARD, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Last Page"))
                .build();
        ActionGroup tblNavigateGroup = new ActionGroup("console.table.result.navigate.group", "Console.TableResult.Navigate.Group", false);
        tblNavigateGroup.addAll(navFirst, navPrev, navNext, navLast).addSeparator();

        AnAction editData = AnAction.builder("editor.edit.data", "Edit Data")
                .icon(FontAwesomeSolid.TABLE, "#57965c", 11)
                .onAction(ctx -> ctx.setStatus("Edit Data"))
                .build();
        AnAction selectInDbExplorer = AnAction.builder("editor.select.in.db.explorer", "Select in Database Explorer")
                .onAction(MainWindow::focusOrToggleSchemaExplorer)
                .build();
        AnAction editorGotoRow = AnAction.builder("editor.goto.row", "Row…")
                .onAction(ctx -> ctx.setStatus("Go to Row…"))
                .build();

        AnAction relatedRows = AnAction.builder("console.table.goto.related.rows", "Related Rows")
                .icon(FontAwesomeSolid.TH, "#6897bb", 11)
                .onAction(ctx -> ctx.setStatus("Related Rows"))
                .build();
        AnAction openUrl = AnAction.builder("console.table.goto.open.url", "Open URL")
                .icon(FontAwesomeSolid.GLOBE, "#4a88c7", 11)
                .onAction(ctx -> ctx.setStatus("Open URL"))
                .build();
        AnAction openFileUri = AnAction.builder("console.table.goto.open.file.uri", "Open File URI")
                .icon(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 11)
                .onAction(MainWindow::openSqlFile)
                .build();
        AnAction referencingResult = AnAction.builder("console.table.goto.referencing.result", "Referencing Result")
                .onAction(ctx -> ctx.setStatus("Referencing Result"))
                .build();
        ActionGroup dbGoToGroup = new ActionGroup("console.table.result.database.goto.group", "Console.TableResult.Database.GoTo", false);
        dbGoToGroup.addAll(relatedRows, openUrl, openFileUri, referencingResult).addSeparator();

        // Folding actions & levels
        ActionGroup expToLevel = new ActionGroup("folding.expand.to.level.group", "Expand to Level", true);
        for (int l = 1; l <= 5; l++) {
            final int lvl = l;
            expToLevel.add(AnAction.builder("folding.expand.to.level." + lvl, String.valueOf(lvl))
                    .onAction(ctx -> ctx.setStatus("Expand to Level " + lvl)).build());
        }
        ActionGroup expAllToLevel = new ActionGroup("folding.expand.all.to.level.group", "Expand All to Level", true);
        for (int l = 1; l <= 5; l++) {
            final int lvl = l;
            expAllToLevel.add(AnAction.builder("folding.expand.all.to.level." + lvl, String.valueOf(lvl))
                    .onAction(ctx -> ctx.setStatus("Expand All to Level " + lvl)).build());
        }

        AnAction foldSelectionRem = AnAction.builder("folding.selection.remove.region", "Fold Selection / Remove Region")
                .onAction(ctx -> ctx.setStatus("Fold Selection / Remove Region"))
                .build();
        AnAction foldBlock = AnAction.builder("folding.code.block", "Fold Code Block")
                .onAction(ctx -> ctx.setStatus("Fold Code Block"))
                .build();

        ActionGroup foldLang = new ActionGroup("folding.language.specific.group", "LanguageSpecificFoldingGroup", false);
        foldLang.add(AnAction.builder("folding.expand.doc.comments", "Expand Doc Comments").onAction(ctx -> ctx.setStatus("Expand Doc Comments")).build());
        foldLang.add(AnAction.builder("folding.collapse.doc.comments", "Collapse Doc Comments").onAction(ctx -> ctx.setStatus("Collapse Doc Comments")).build());

        AnAction foldExp = AnAction.builder("folding.expand", "Expand").onAction(ctx -> ctx.setStatus("Expand")).build();
        AnAction foldExpRec = AnAction.builder("folding.expand.recursively", "Expand Recursively").onAction(ctx -> ctx.setStatus("Expand Recursively")).build();
        AnAction foldExpAll = AnAction.builder("folding.expand.all", "Expand All").onAction(ctx -> ctx.setStatus("Expand All")).build();
        AnAction foldCol = AnAction.builder("folding.collapse", "Collapse").onAction(ctx -> ctx.setStatus("Collapse")).build();
        AnAction foldColRec = AnAction.builder("folding.collapse.recursively", "Collapse Recursively").onAction(ctx -> ctx.setStatus("Collapse Recursively")).build();
        AnAction foldColAll = AnAction.builder("folding.collapse.all", "Collapse All").onAction(ctx -> ctx.setStatus("Collapse All")).build();
        AnAction foldTog = AnAction.builder("folding.toggle", "Toggle Folding").onAction(ctx -> ctx.setStatus("Toggle Folding")).build();

        ActionGroup editorFolding = new ActionGroup("editor.popup.folding", "Folding", true);
        editorFolding.addAll(foldExp, foldExpRec, foldExpAll)
                .addSeparator()
                .addAll(foldCol, foldColRec, foldColAll)
                .addSeparator()
                .add(expToLevel)
                .add(expAllToLevel)
                .addSeparator()
                .add(foldLang)
                .addSeparator()
                .add(foldTog)
                .addSeparator()
                .addAll(foldSelectionRem, foldBlock);

        AnAction saveAsLiveTpl = AnAction.builder("tools.save.as.live.template", "Save as Live Template…")
                .onAction(MainWindow::insertLiveTemplate)
                .build();

        ActionGroup editorGoTo = new ActionGroup("editor.popup.goto", "Go To", true);
        editorGoTo.add(tblNavigateGroup);
        AnAction jumpNav = manager.getAction("nav.jump.to.navbar");
        if (jumpNav != null) editorGoTo.add(jumpNav);
        AnAction gotoDecl = manager.getAction("nav.goto.declaration");
        if (gotoDecl != null) editorGoTo.add(gotoDecl);
        editorGoTo.addAll(editData, selectInDbExplorer);
        AnAction gotoImpl = manager.getAction("nav.goto.implementation");
        if (gotoImpl != null) editorGoTo.add(gotoImpl);
        AnAction gotoType = manager.getAction("nav.goto.type.declaration");
        if (gotoType != null) editorGoTo.add(gotoType);
        AnAction gotoSuper = manager.getAction("nav.goto.super.method");
        if (gotoSuper != null) editorGoTo.add(gotoSuper);
        AnAction gotoRel = manager.getAction("nav.related.symbol");
        if (gotoRel != null) editorGoTo.add(gotoRel);
        AnAction gotoTest = manager.getAction("nav.goto.test");
        if (gotoTest != null) editorGoTo.add(gotoTest);
        AnAction jumpDdl = manager.getAction("editor.jump.ddl");
        if (jumpDdl != null) editorGoTo.add(jumpDdl);
        else {
            editorGoTo.add(AnAction.builder("editor.jump.ddl", "DDL").onAction(ctx -> ctx.setStatus("DDL")).build());
        }
        editorGoTo.add(editorGotoRow);
        editorGoTo.add(dbGoToGroup);

        ActionGroup findRefactorGroup = new ActionGroup("editor.popup.find.refactor", "EditorPopupMenu1.FindRefactor", false);
        AnAction findUsages = manager.getAction("edit.find.usages");
        if (findUsages != null) findRefactorGroup.add(findUsages);
        findRefactorGroup.add(editorGoTo).addSeparator().add(editorFolding).add(saveAsLiveTpl);
        AnAction reformatCode = manager.getAction("code.reformat");
        if (reformatCode != null) findRefactorGroup.add(reformatCode);

        ActionGroup actions1Group = new ActionGroup("editor.popup.actions.1", "Editor Popup Menu Actions (1)", false);
        AnAction findFiles = manager.getAction("edit.find.in.files");
        if (findFiles != null) actions1Group.add(findFiles);
        actions1Group.add(findRefactorGroup);

        // EditorPopupMenu2 (Image 1)
        ActionGroup sqlScriptsGroup = new ActionGroup("editor.sql.scripts", "SQL Scripts", true,
                FontAwesomeSolid.FOLDER, "#e0a44c", 11, "SQL Scripts");
        sqlScriptsGroup.add(AnAction.builder("editor.sql.generator", "SQL Generator…").icon(FontAwesomeSolid.TERMINAL, "#6897bb", 11).onAction(ctx -> ctx.setStatus("SQL Generator…")).build());
        sqlScriptsGroup.addSeparator();
        sqlScriptsGroup.add(AnAction.builder("editor.sql.request.copy.ddl", "Request and Copy Original DDL").icon(FontAwesomeSolid.SHARE_SQUARE, "#a9b7c6", 11).onAction(ctx -> ctx.setStatus("Request and Copy Original DDL")).build());
        sqlScriptsGroup.add(AnAction.builder("editor.sql.generate.ddl.clipboard", "Generate DDL to Clipboard").icon(FontAwesomeSolid.COPY, "#a9b7c6", 11).onAction(ctx -> ctx.setStatus("Generate DDL to Clipboard")).build());
        sqlScriptsGroup.add(AnAction.builder("editor.sql.generate.ddl.queryfile", "Generate DDL to Query File").icon(FontAwesomeSolid.FILE_CODE, "#a9b7c6", 11).onAction(MainWindow::openNewSqlFile).build());
        sqlScriptsGroup.add(AnAction.builder("editor.sql.context.templates", "Context Templates").onAction(MainWindow::insertLiveTemplate).build());
        sqlScriptsGroup.addSeparator();
        sqlScriptsGroup.add(AnAction.builder("editor.sql.run.script", "Run SQL Script…").onAction(MainWindow::openSqlFile).build());
        sqlScriptsGroup.add(AnAction.builder("editor.sql.regenerate.definition", "Regenerate definition").onAction(ctx -> ctx.setStatus("Regenerate definition")).build());
        sqlScriptsGroup.add(AnAction.builder("editor.sql.import.to.database", "Import to Database…").icon(FontAwesomeSolid.UPLOAD, "#4a88c7", 11).onAction(ctx -> ctx.setStatus("Import to Database…")).build());
        sqlScriptsGroup.add(AnAction.builder("editor.sql.view.as.table", "View as Table").icon(FontAwesomeSolid.TABLE, "#57965c", 11).onAction(ctx -> ctx.setStatus("View as Table")).build());
        sqlScriptsGroup.add(AnAction.builder("editor.sql.edit.as.table", "Edit as Table").icon(FontAwesomeSolid.TABLE, "#57965c", 11).onAction(ctx -> ctx.setStatus("Edit as Table")).build());
        sqlScriptsGroup.add(AnAction.builder("editor.sql.change.file.language", "Change File Language").onAction(ctx -> ctx.setStatus("Change File Language")).build());
        sqlScriptsGroup.add(AnAction.builder("editor.sql.change.sql.dialect", "Change SQL Dialect").onAction(MainWindow::showSqlDialectsDialog).build());

        ActionGroup editorPopupMenu2 = new ActionGroup("editor.popup.menu.2", "EditorPopupMenu2", true);
        editorPopupMenu2.add(sqlScriptsGroup);

        // EditorPopupMenu3 (Image 1)
        ActionGroup editorPopupMenu3 = new ActionGroup("editor.popup.menu.3", "EditorPopupMenu3", true);
        editorPopupMenu3.add(AnAction.builder("editor.popup.set.background.image", "Set Background Image").onAction(ctx -> ctx.setStatus("Set Background Image")).build());

        AnAction searchWithGoogle = AnAction.builder("editor.search.with.google", "Search with Google")
                .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Search with Google"))
                .build();

        // Editor Popup Menu Actions (2) (Images 2, 3, 4, 5)
        ActionGroup editorActions2 = new ActionGroup("editor.popup.actions.2", "Editor Popup Menu Actions (2)", false);
        editorActions2.addSeparator();
        editorActions2.add(AnAction.builder("editor.popup.rename", "Rename…").onAction(MainWindow::renameProject).build());

        // Refactor [popup] (Image 2)
        ActionGroup editorRefactor = new ActionGroup("editor.popup.refactor", "Refactor", true);
        AnAction refactorThis = manager.getAction("refactor.this");
        if (refactorThis != null) editorRefactor.add(refactorThis);
        AnAction refactorRename = manager.getAction("refactor.rename");
        if (refactorRename != null) editorRefactor.add(refactorRename);
        AnAction refactorChangeSig = manager.getAction("refactor.change.signature");
        if (refactorChangeSig != null) editorRefactor.add(refactorChangeSig);
        AnAction refactorModObj = manager.getAction("refactor.modify.object");
        if (refactorModObj != null) editorRefactor.add(refactorModObj);
        editorRefactor.addSeparator();

        // Extract/Introduce [popup] (Image 2)
        ActionGroup extractIntro = new ActionGroup("editor.refactor.extract.introduce", "Extract/Introduce", true);
        AnAction introVar = manager.getAction("refactor.introduce.variable");
        if (introVar != null) extractIntro.add(introVar);
        AnAction extRoutine = manager.getAction("refactor.extract.routine");
        if (extRoutine != null) extractIntro.add(extRoutine);
        AnAction tblAlias = manager.getAction("refactor.table.alias");
        if (tblAlias != null) extractIntro.add(tblAlias);
        AnAction introConst = manager.getAction("refactor.introduce.constant");
        if (introConst != null) extractIntro.add(introConst);
        AnAction introField = manager.getAction("refactor.introduce.field");
        if (introField != null) extractIntro.add(introField);
        AnAction introParam = manager.getAction("refactor.introduce.parameter");
        if (introParam != null) extractIntro.add(introParam);
        extractIntro.addSeparator();
        AnAction introParamObj = manager.getAction("refactor.introduce.parameter.object");
        if (introParamObj != null) extractIntro.add(introParamObj);
        extractIntro.addSeparator();
        AnAction extMethod = manager.getAction("refactor.extract.method");
        if (extMethod != null) extractIntro.add(extMethod);
        extractIntro.addSeparator();
        AnAction extDelegate = manager.getAction("refactor.extract.delegate");
        if (extDelegate != null) extractIntro.add(extDelegate);
        AnAction incFile = manager.getAction("refactor.include.file");
        if (incFile != null) extractIntro.add(incFile);
        AnAction extInterface = manager.getAction("refactor.extract.interface");
        if (extInterface != null) extractIntro.add(extInterface);
        AnAction extSuperclass = manager.getAction("refactor.extract.superclass");
        if (extSuperclass != null) extractIntro.add(extSuperclass);
        AnAction extModule = manager.getAction("refactor.extract.module");
        if (extModule != null) extractIntro.add(extModule);
        AnAction subqueryCte = manager.getAction("refactor.subquery.cte");
        if (subqueryCte != null) extractIntro.add(subqueryCte);

        editorRefactor.add(extractIntro);
        AnAction refactorInline = manager.getAction("refactor.inline");
        if (refactorInline != null) editorRefactor.add(refactorInline);
        editorRefactor.addSeparator();
        AnAction refactorMove = manager.getAction("refactor.move");
        if (refactorMove != null) editorRefactor.add(refactorMove);
        AnAction refactorCopy = manager.getAction("refactor.copy");
        if (refactorCopy != null) editorRefactor.add(refactorCopy);
        AnAction refactorSafeDel = manager.getAction("refactor.safe.delete");
        if (refactorSafeDel != null) editorRefactor.add(refactorSafeDel);
        editorRefactor.addSeparator();
        AnAction pullMembers = manager.getAction("refactor.pull.members.up");
        if (pullMembers != null) editorRefactor.add(pullMembers);
        AnAction pushMembers = manager.getAction("refactor.push.members.down");
        if (pushMembers != null) editorRefactor.add(pushMembers);
        AnAction invertBool = manager.getAction("refactor.invert.boolean");
        if (invertBool != null) editorRefactor.add(invertBool);

        editorActions2.add(editorRefactor);
        editorActions2.add(AnAction.builder("editor.popup.generate", "Generate…").onAction(ctx -> ctx.setStatus("Generate…")).build());
        editorActions2.addSeparator();

        // Debug Actions (Image 3)
        ActionGroup debugActionsGroup = new ActionGroup("editor.debug.actions", "Debug Actions", false);
        debugActionsGroup.add(AnAction.builder("debug.copy.js.script.clipboard", "Copy JS Script to Clipboard").onAction(ctx -> ctx.setStatus("Copy JS Script to Clipboard")).build());
        debugActionsGroup.add(AnAction.builder("debug.show.js.script", "Show JS Script").onAction(ctx -> ctx.setStatus("Show JS Script")).build());
        debugActionsGroup.addSeparator();
        debugActionsGroup.add(AnAction.builder("debug.evaluate.expression", "Evaluate Expression…").icon(FontAwesomeSolid.CALCULATOR, "#a9b7c6", 11).onAction(ctx -> ctx.setStatus("Evaluate Expression…")).build());
        debugActionsGroup.add(AnAction.builder("debug.run.to.cursor", "Run to Cursor").onAction(ctx -> ctx.setStatus("Run to Cursor")).build());
        debugActionsGroup.add(AnAction.builder("debug.force.run.to.cursor", "Force Run to Cursor").onAction(ctx -> ctx.setStatus("Force Run to Cursor")).build());
        debugActionsGroup.add(AnAction.builder("debug.add.to.watches", "Add to Watches").onAction(ctx -> ctx.setStatus("Add to Watches")).build());
        debugActionsGroup.add(AnAction.builder("debug.add.inline.watch", "Add Inline Watch").onAction(ctx -> ctx.setStatus("Add Inline Watch")).build());
        debugActionsGroup.add(AnAction.builder("debug.evaluate.in.console", "Evaluate in Console").onAction(ctx -> ctx.setStatus("Evaluate in Console")).build());
        debugActionsGroup.addSeparator();

        ActionGroup debugHotSwap = new ActionGroup("editor.popup.debug.hotswap", "EditorPopupMenuDebugHotSwap", false);
        debugHotSwap.add(AnAction.builder("debug.compile.reload.modified.files", "Compile and Reload Modified Files").onAction(ctx -> ctx.setStatus("Compile and Reload Modified Files")).build());
        debugHotSwap.addSeparator();
        debugActionsGroup.add(debugHotSwap);
        editorActions2.add(debugActionsGroup);

        // Compile/Run Actions (Image 3 & 4)
        ActionGroup compileRunGroup = new ActionGroup("editor.compile.run.actions", "Compile/Run Actions", false);
        ActionGroup runConfigsGroup = new ActionGroup("editor.run.configurations.group", "Run Configurations", false);
        ActionGroup runContextGroup = new ActionGroup("editor.run.context.group", "RunContextGroup", false);

        ActionGroup runContextInner = new ActionGroup("editor.run.context.group.inner", "RunContextGroupInner", false);
        ActionGroup executorsGroup = new ActionGroup("editor.run.context.executors.group", "RunContextExecutorsGroup", false);
        executorsGroup.add(AnAction.builder("run.context.configuration", "Run context configuration").icon(FontAwesomeSolid.PLAY, "#57965c", 11).onAction(ctx -> ctx.setStatus("Run context configuration")).build());
        executorsGroup.add(AnAction.builder("debug.context.configuration", "Debug context configuration").icon(FontAwesomeSolid.BUG, "#e06c75", 11).onAction(ctx -> ctx.setStatus("Debug context configuration")).build());
        runContextInner.add(executorsGroup);

        ActionGroup moreRunDebug = new ActionGroup("editor.more.run.debug.group", "More Run/Debug", false);
        moreRunDebug.add(AnAction.builder("run.coverage.context.configuration", "Run with Coverage context configuration").onAction(ctx -> ctx.setStatus("Run with Coverage context configuration")).build());
        moreRunDebug.add(AnAction.builder("run.profiler.context.configuration", "Run with Profiler").onAction(ctx -> ctx.setStatus("Run with Profiler")).build());
        moreRunDebug.add(AnAction.builder("run.create.configuration", "Create Run Configuration").onAction(ctx -> ctx.setStatus("Create Run Configuration")).build());
        moreRunDebug.addSeparator();
        moreRunDebug.add(AnAction.builder("run.context.configuration.secondary", "Run context configuration").icon(FontAwesomeSolid.PLAY, "#57965c", 11).onAction(ctx -> ctx.setStatus("Run context configuration")).build());
        moreRunDebug.add(AnAction.builder("debug.context.configuration.secondary", "Debug context configuration").icon(FontAwesomeSolid.BUG, "#e06c75", 11).onAction(ctx -> ctx.setStatus("Debug context configuration")).build());
        moreRunDebug.add(AnAction.builder("run.modify.configuration", "Modify Run Configuration…").onAction(ctx -> ctx.setStatus("Modify Run Configuration…")).build());
        moreRunDebug.addSeparator();
        runContextInner.add(moreRunDebug);

        runContextGroup.add(runContextInner);
        runContextGroup.addSeparator();

        // Console.Jdbc.RunContextGroup (Image 4)
        ActionGroup consoleJdbcRun = new ActionGroup("console.jdbc.run.context.group", "Console.Jdbc.RunContextGroup", false);
        consoleJdbcRun.add(AnAction.builder("console.attach.data.source", "Attach Data Source").onAction(ctx -> ctx.setStatus("Attach Data Source")).build());
        consoleJdbcRun.addSeparator();
        consoleJdbcRun.add(AnAction.builder("console.recompile", "Recompile…").onAction(ctx -> ctx.setStatus("Recompile…")).build());

        ActionGroup explainPlanGroup = new ActionGroup("console.explain.plan.group", "Explain Plan", true);
        explainPlanGroup.add(AnAction.builder("console.explain.plan", "Explain Plan").onAction(ctx -> ctx.setStatus("Explain Plan")).build());
        explainPlanGroup.add(AnAction.builder("console.explain.plan.raw", "Explain Plan (Raw)").onAction(ctx -> ctx.setStatus("Explain Plan (Raw)")).build());
        explainPlanGroup.add(AnAction.builder("console.explain.analyse", "Explain Analyse").onAction(ctx -> ctx.setStatus("Explain Analyse")).build());
        explainPlanGroup.add(AnAction.builder("console.explain.analyse.raw", "Explain Analyse (Raw)").onAction(ctx -> ctx.setStatus("Explain Analyse (Raw)")).build());
        consoleJdbcRun.add(explainPlanGroup);

        AnAction runAction = manager.getAction("middle.run");
        if (runAction != null) consoleJdbcRun.add(runAction);
        consoleJdbcRun.add(AnAction.builder("editor.execute.selection.single", "Execute Selection as Single Statement").icon(FontAwesomeSolid.PLAY, "#57965c", 11).onAction(MainWindow::executeCurrentStatement).build());
        consoleJdbcRun.add(AnAction.builder("editor.export.data", "Export Data…").icon(FontAwesomeSolid.DOWNLOAD, "#4a88c7", 11).onAction(ctx -> ctx.setStatus("Export Data…")).build());
        consoleJdbcRun.add(AnAction.builder("editor.debug", "Debug").icon(FontAwesomeSolid.BUG, "#e06c75", 11).onAction(ctx -> ctx.setStatus("Debug")).build());
        consoleJdbcRun.add(AnAction.builder("editor.debug.routine", "Debug Routine…").icon(FontAwesomeSolid.BUG, "#e06c75", 11).onAction(ctx -> ctx.setStatus("Debug Routine…")).build());
        consoleJdbcRun.addSeparator();
        consoleJdbcRun.add(AnAction.builder("editor.migrate.consoles.to.queryfiles", "Migrate Query Consoles to Query Files…").onAction(ctx -> ctx.setStatus("Migrate Query Consoles to Query Files…")).build());
        consoleJdbcRun.addSeparator();

        runContextGroup.add(consoleJdbcRun);
        runConfigsGroup.add(runContextGroup);
        compileRunGroup.add(runConfigsGroup);
        editorActions2.add(compileRunGroup);

        // SplitRevealGroup (Image 4)
        ActionGroup splitRevealGroup = new ActionGroup("editor.split.reveal.group", "SplitRevealGroup", false);
        splitRevealGroup.add(AnAction.builder("editor.open.in.right.split", "Open in Right Split").icon(FontAwesomeSolid.COLUMNS, "#a9b7c6", 11).onAction(MainWindow::splitActiveTabRight).build());
        splitRevealGroup.add(AnAction.builder("editor.open.in.split.chooser", "Open in Split with Chooser…").onAction(MainWindow::openInSplitWithChooser).build());

        ActionGroup openInGroup = new ActionGroup("editor.open.in.group", "Open In", true,
                FontAwesomeSolid.FOLDER, "#e0a44c", 11, "Open In");
        openInGroup.add(AnAction.builder("editor.open.in.file.manager", "Show in File Manager").onAction(ctx -> ctx.setStatus("Show in File Manager")).build());
        openInGroup.add(AnAction.builder("editor.open.in.associated.app", "Open in Associated Application").onAction(ctx -> ctx.setStatus("Open in Associated Application")).build());
        openInGroup.add(AnAction.builder("editor.open.in.browser", "Open in Browser").icon(FontAwesomeSolid.GLOBE, "#4a88c7", 11).onAction(ctx -> ctx.setStatus("Open in Browser")).build());
        openInGroup.add(AnAction.builder("editor.open.in.file.path", "File Path").onAction(MainWindow::showFilePathPopup).build());
        openInGroup.add(AnAction.builder("editor.open.in.terminal", "Open in Terminal").icon(FontAwesomeSolid.TERMINAL, "#6897bb", 11).onAction(MainWindow::showTerminalToolWindow).build());
        openInGroup.add(AnAction.builder("editor.open.in.terminal.second", "Open in Terminal").icon(FontAwesomeSolid.TERMINAL, "#6897bb", 11).onAction(MainWindow::showTerminalToolWindow).build());
        openInGroup.add(AnAction.builder("git.hosting.open.in.browser.group", "Git.Hosting.Open.In.Browser.Group").onAction(ctx -> ctx.setStatus("Git.Hosting.Open.In.Browser.Group")).build());
        splitRevealGroup.add(openInGroup);
        splitRevealGroup.addSeparator();

        editorActions2.add(splitRevealGroup);
        editorActions2.addSeparator();

        // VCS/LVCS Actions (Image 5)
        ActionGroup vcsLvcsGroup = new ActionGroup("editor.vcs.lvcs.actions.group", "VCS/LVCS Actions", false);
        ActionGroup localHistoryGroup = new ActionGroup("editor.local.history.group", "Local History", true);
        localHistoryGroup.add(AnAction.builder("editor.local.history.show", "Show History…").onAction(MainWindow::showLocalHistoryForCurrentConsole).build());
        localHistoryGroup.add(AnAction.builder("editor.local.history.show.selection", "Show History for Selection…").onAction(MainWindow::showHistoryForSelection).build());
        localHistoryGroup.addSeparator();
        localHistoryGroup.add(AnAction.builder("editor.local.history.show.project", "Show Project History…").onAction(MainWindow::showProjectHistoryDialog).build());
        localHistoryGroup.add(AnAction.builder("editor.local.history.recent.changes", "Recent Changes").onAction(MainWindow::showRecentChangesDialog).build());
        localHistoryGroup.add(AnAction.builder("editor.local.history.put.label", "Put Label…").onAction(MainWindow::showPutLabelDialog).build());
        localHistoryGroup.add(AnAction.builder("editor.local.history.vcs.group", "Version Control Group").onAction(MainWindow::showVcsOperationsPopup).build());
        vcsLvcsGroup.add(localHistoryGroup);
        vcsLvcsGroup.addSeparator();

        vcsLvcsGroup.add(AnAction.builder("editor.external.tools", "External Tools").onAction(ctx -> ctx.setStatus("External Tools")).build());
        vcsLvcsGroup.add(AnAction.builder("editor.update.tag.emmet", "Update tag with Emmet").onAction(ctx -> ctx.setStatus("Update tag with Emmet")).build());

        AnAction compareWithClip = AnAction.builder("diff.compare.clipboard", "Compare with Clipboard")
                .icon(FontAwesomeSolid.EXCHANGE_ALT, "#6897bb", 11)
                .onAction(ctx -> ctx.setStatus("Compare with Clipboard"))
                .build();
        AnAction showRevDiff = AnAction.builder("vcs.show.review.diff", "Show Review Diff")
                .icon(FontAwesomeSolid.ARROW_RIGHT, "#4a88c7", 11)
                .onAction(ctx -> ctx.setStatus("Show Review Diff"))
                .build();
        AnAction addRevComment = AnAction.builder("vcs.add.review.comment", "Add Review Comment")
                .onAction(ctx -> ctx.setStatus("Add Review Comment"))
                .build();
        vcsLvcsGroup.addAll(compareWithClip, showRevDiff, addRevComment);

        editorActions2.add(vcsLvcsGroup);

        // Git [popup] (Image 5)
        ActionGroup gitPopup = new ActionGroup("editor.popup.git", "Git", true);
        gitPopup.add(AnAction.builder("git.show.local.version", "Show Local Version").onAction(ctx -> ctx.setStatus("Show Local Version")).build());
        gitPopup.add(AnAction.builder("git.compare.with.head", "Compare with HEAD Version").onAction(ctx -> ctx.setStatus("Compare with HEAD Version")).build());
        gitPopup.add(AnAction.builder("git.compare.with.local", "Compare with Local Version").onAction(ctx -> ctx.setStatus("Compare with Local Version")).build());
        gitPopup.add(AnAction.builder("git.compare.head.staged.local", "Compare HEAD, Staged and Local Versions").onAction(ctx -> ctx.setStatus("Compare HEAD, Staged and Local Versions")).build());

        // Diagrams [popup] (Image 5)
        ActionGroup diagPopup = new ActionGroup("editor.popup.diagrams", "Diagrams", true);
        diagPopup.add(AnAction.builder("diagrams.show.uml", "ShowUmlDiagram").icon(FontAwesomeSolid.PROJECT_DIAGRAM, "#a9b7c6", 11).onAction(MainWindow::showCurrentDiagram).build());
        diagPopup.add(AnAction.builder("diagrams.show.uml.popup", "ShowUmlDiagramPopup").icon(FontAwesomeSolid.PROJECT_DIAGRAM, "#a9b7c6", 11).onAction(MainWindow::showCurrentDiagram).build());

        // XPathView.EditorPopup [popup] (Image 5)
        ActionGroup xpathPopup = new ActionGroup("xpath.editor.popup", "XPathView.EditorPopup", true);
        xpathPopup.addSeparator();
        xpathPopup.add(AnAction.builder("xpath.evaluate", "Evaluate XPath…").onAction(ctx -> ctx.setStatus("Evaluate XPath…")).build());
        xpathPopup.add(AnAction.builder("xpath.show.unique", "Show Unique XPath").onAction(ctx -> ctx.setStatus("Show Unique XPath")).build());
        xpathPopup.add(AnAction.builder("xpath.file.associations", "File Associations").onAction(ctx -> ctx.setStatus("File Associations")).build());

        // Register groups and actions
        manager.registerGroup(showIntentions);
        manager.registerGroup(lightEditMode);
        manager.registerGroup(copyPasteSpecial);
        manager.registerAction(colSelectionMode);
        manager.registerGroup(mdEditorGroup);
        manager.registerGroup(actions1Group);
        manager.registerGroup(editorPopupMenu2);
        manager.registerGroup(editorPopupMenu3);
        manager.registerAction(searchWithGoogle);
        manager.registerGroup(editorActions2);
        manager.registerGroup(gitPopup);
        manager.registerGroup(diagPopup);
        manager.registerGroup(xpathPopup);
    }

    private static void registerEditorGutterPopupMenuActions(ActionManager manager) {
        // EditorGutterVcsPopupMenu [popup=false]
        ActionGroup gutterVcs = new ActionGroup("gutter.vcs.popup", "EditorGutterVcsPopupMenu", false);
        gutterVcs.add(AnAction.builder("gutter.vcs.annotate", "Annotate")
                .onAction(ctx -> ctx.setStatus("Annotate"))
                .build());
        gutterVcs.addSeparator();

        // popup@BookmarkContextMenu [popup=false]
        ActionGroup gutterBookmark = new ActionGroup("gutter.bookmark.context", "popup@BookmarkContextMenu", false);
        gutterBookmark.add(AnAction.builder("gutter.bookmark.add.another.list", "Add Bookmark to Another List")
                .onAction(ctx -> ctx.setStatus("Add Bookmark to Another List"))
                .build());
        gutterBookmark.add(AnAction.builder("gutter.bookmark.rename", "Rename Bookmark\u2026")
                .icon(FontAwesomeSolid.EDIT, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Rename Bookmark\u2026"))
                .build());
        gutterBookmark.add(AnAction.builder("gutter.bookmark.toggle", "Toggle Bookmark")
                .onAction(ctx -> ctx.setStatus("Toggle Bookmark"))
                .build());
        gutterBookmark.add(AnAction.builder("gutter.bookmark.remove.mnemonic", "Remove Mnemonic")
                .onAction(ctx -> ctx.setStatus("Remove Mnemonic"))
                .build());
        gutterBookmark.add(AnAction.builder("gutter.bookmark.toggle.mnemonic", "Toggle Bookmark Mnemonic\u2026")
                .onAction(ctx -> ctx.setStatus("Toggle Bookmark Mnemonic\u2026"))
                .build());
        gutterBookmark.addSeparator();

        // Soft-Wrap & Configure Soft Wraps
        AnAction softWrap = AnAction.builder("editor.softwrap", "Soft-Wrap")
                .icon(FontAwesomeSolid.ALIGN_LEFT, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Soft-Wrap"))
                .build();
        AnAction configSoftWraps = AnAction.builder("editor.configure.softwraps", "Configure Soft Wraps\u2026")
                .onAction(MainWindow::showSettingsDialog)
                .build();

        // Appearance [popup=true]
        ActionGroup appearance = new ActionGroup("gutter.appearance", "Appearance", true, FontAwesomeSolid.FOLDER, "#e0a44c", 11, null);
        appearance.add(AnAction.builder("gutter.appearance.line.numbers", "Show Line Numbers")
                .onAction(ctx -> ctx.setStatus("Show Line Numbers"))
                .build());
        appearance.add(AnAction.builder("gutter.appearance.breakpoints.over.line.numbers", "Breakpoints Over Line Numbers")
                .onAction(ctx -> ctx.setStatus("Breakpoints Over Line Numbers"))
                .build());
        appearance.add(AnAction.builder("gutter.appearance.indent.guides", "Show Indent Guides")
                .onAction(ctx -> ctx.setStatus("Show Indent Guides"))
                .build());
        appearance.add(AnAction.builder("gutter.appearance.sticky.lines", "Show Sticky Lines")
                .onAction(ctx -> ctx.setStatus("Show Sticky Lines"))
                .build());

        ActionGroup breadcrumbs = new ActionGroup("gutter.breadcrumbs", "Breadcrumbs", true, FontAwesomeSolid.FOLDER, "#e0a44c", 11, null);
        breadcrumbs.add(AnAction.builder("gutter.breadcrumbs.top", "Top").onAction(ctx -> ctx.setStatus("Breadcrumbs: Top")).build());
        breadcrumbs.add(AnAction.builder("gutter.breadcrumbs.bottom", "Bottom").onAction(ctx -> ctx.setStatus("Breadcrumbs: Bottom")).build());
        breadcrumbs.add(AnAction.builder("gutter.breadcrumbs.dont.show", "Don't Show").onAction(ctx -> ctx.setStatus("Breadcrumbs: Don't Show")).build());
        appearance.add(breadcrumbs);

        appearance.add(AnAction.builder("gutter.configure.icons", "Configure Gutter Icons\u2026")
                .onAction(MainWindow::showSettingsDialog)
                .build());

        AnAction removeBreakpoints = AnAction.builder("gutter.remove.breakpoints", "Remove other breakpoints")
                .onAction(ctx -> ctx.setStatus("Remove other breakpoints"))
                .build();
        AnAction disableBreakpoints = AnAction.builder("gutter.disable.breakpoints", "Disable other breakpoints")
                .onAction(ctx -> ctx.setStatus("Disable other breakpoints"))
                .build();

        manager.registerGroup(gutterVcs);
        manager.registerGroup(gutterBookmark);
        manager.registerAction(softWrap);
        manager.registerAction(configSoftWraps);
        manager.registerGroup(appearance);
        manager.registerGroup(breadcrumbs);
        manager.registerAction(removeBreakpoints);
        manager.registerAction(disableBreakpoints);
    }

    private static void registerEditorTabPopupMenuActions(ActionManager manager) {
        // Editor Close Actions [popup=false]
        ActionGroup closeActions = new ActionGroup("tab.close.actions", "Editor Close Actions", false);
        closeActions.add(AnAction.builder("tab.close", "Close Tab")
                .accelerator(new KeyCodeCombination(KeyCode.F4, KeyCombination.CONTROL_DOWN))
                .onAction(MainWindow::closeActiveTab)
                .build());
        closeActions.add(AnAction.builder("tab.close.others", "Close Other Tabs")
                .onAction(MainWindow::closeOtherTabs)
                .build());
        closeActions.add(AnAction.builder("tab.close.all", "Close All Tabs")
                .accelerator(new KeyCodeCombination(KeyCode.F4, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::closeAllTabs)
                .build());
        closeActions.add(AnAction.builder("tab.close.unmodified", "Close Unmodified Tabs")
                .onAction(MainWindow::closeUnmodifiedTabs)
                .build());
        closeActions.add(AnAction.builder("tab.close.all.but.pinned", "Close All but Pinned")
                .onAction(MainWindow::closeAllButPinnedTabs)
                .build());
        closeActions.add(AnAction.builder("tab.close.left", "Close Tabs to the Left")
                .onAction(MainWindow::closeTabsToLeft)
                .build());
        closeActions.add(AnAction.builder("tab.close.right", "Close Tabs to the Right")
                .onAction(MainWindow::closeTabsToRight)
                .build());
        closeActions.add(AnAction.builder("tab.close.all.readonly", "Close All Read-Only Tabs")
                .onAction(MainWindow::closeAllReadOnlyTabs)
                .build());
        closeActions.add(AnAction.builder("tab.open.as.editor.tab", "Open as Editor Tab")
                .icon(FontAwesomeSolid.EXTERNAL_LINK_ALT, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Open as Editor Tab"))
                .build());
        closeActions.addSeparator();

        // Direct actions
        AnAction copyPaths = AnAction.builder("tab.copy.paths", "Copy Paths")
                .onAction(ctx -> ctx.setStatus("Copy Paths"))
                .build();
        AnAction copyReference = AnAction.builder("tab.copy.reference", "Copy Reference")
                .onAction(ctx -> ctx.setStatus("Copy Reference"))
                .build();
        AnAction copyJsonPointer = AnAction.builder("tab.copy.json.pointer", "Copy JSON Pointer")
                .onAction(ctx -> ctx.setStatus("Copy JSON Pointer"))
                .build();
        AnAction changeTemplateLang = AnAction.builder("tab.change.template.lang", "Change Template Data Language")
                .onAction(ctx -> ctx.setStatus("Change Template Data Language"))
                .build();

        // Copy Path/Reference... [popup=true]
        ActionGroup copyPathRefGroup = new ActionGroup("tab.copy.path.reference.group", "Copy Path/Reference…", true);
        ActionGroup copyFileRef = new ActionGroup("tab.copy.file.reference.group", "CopyFileReference", false);
        copyFileRef.add(AnAction.builder("tab.copy.path.absolute", "Absolute Path").onAction(ctx -> ctx.setStatus("Copy Absolute Path")).build());
        copyFileRef.add(AnAction.builder("tab.copy.path.filename", "File Name").onAction(ctx -> ctx.setStatus("Copy File Name")).build());
        copyFileRef.addSeparator();
        copyFileRef.add(AnAction.builder("tab.copy.path.line.number", "Path with Line Number").onAction(ctx -> ctx.setStatus("Copy Path with Line Number")).build());
        copyFileRef.add(AnAction.builder("tab.copy.path.content.root", "Path from Content Root").onAction(ctx -> ctx.setStatus("Copy Path from Content Root")).build());
        copyFileRef.add(AnAction.builder("tab.copy.path.source.root", "Path from Source Root").onAction(ctx -> ctx.setStatus("Copy Path from Source Root")).build());
        copyFileRef.add(AnAction.builder("tab.copy.path.repo.root", "Path From Repository Root").onAction(ctx -> ctx.setStatus("Copy Path From Repository Root")).build());
        copyFileRef.add(AnAction.builder("tab.copy.git.hosting.link", "Git.Hosting.Copy.Link.Group").onAction(ctx -> ctx.setStatus("Copy Link on Git Hosting")).build());

        ActionGroup copyExtRef = new ActionGroup("tab.copy.external.reference.group", "CopyExternalReferenceGroup", false);
        copyExtRef.add(AnAction.builder("tab.copy.toolbox.url", "Toolbox URL")
                .icon(FontAwesomeSolid.TOOLBOX, "#c77dbb", 11)
                .onAction(ctx -> ctx.setStatus("Copy Toolbox URL"))
                .build());

        copyPathRefGroup.add(copyFileRef);
        copyPathRefGroup.addSeparator();
        copyPathRefGroup.add(copyExtRef);
        copyPathRefGroup.add(AnAction.builder("tab.copy.ref.action", "Copy Reference").onAction(ctx -> ctx.setStatus("Copy Reference")).build());

        // Diff & VCS
        AnAction showDiffSeparate = AnAction.builder("tab.diff.separate.window", "Show Diff in Separate Window")
                .icon(FontAwesomeSolid.EXTERNAL_LINK_ALT, "#6897bb", 11)
                .onAction(ctx -> ctx.setStatus("Show Diff in Separate Window"))
                .build();
        AnAction showAllDiff = AnAction.builder("tab.diff.all.one.view", "Show All Files in One Diff View")
                .onAction(ctx -> ctx.setStatus("Show All Files in One Diff View"))
                .build();
        ActionGroup vcsDiffGroup = new ActionGroup("vcs.diff.editor.tabs.group", "Vcs.Diff.EditorTabs.Group", false);
        vcsDiffGroup.add(AnAction.builder("tab.vcs.diff.collapse.all", "Collapse All Files").onAction(ctx -> ctx.setStatus("Collapse All Files")).build());

        // Split actions
        AnAction splitRight = AnAction.builder("editor.split.right", "Split Right")
                .icon(FontAwesomeSolid.COLUMNS, "#a9b7c6", 11)
                .onAction(MainWindow::splitActiveTabRight)
                .build();
        AnAction splitMoveRight = AnAction.builder("tab.split.move.right", "Split and Move Right")
                .onAction(MainWindow::splitActiveTabRight)
                .build();
        AnAction splitDown = AnAction.builder("editor.split.down", "Split Down")
                .icon(FontAwesomeSolid.WINDOW_RESTORE, "#a9b7c6", 11)
                .onAction(MainWindow::splitActiveTabDown)
                .build();
        AnAction splitMoveDown = AnAction.builder("tab.split.move.down", "Split and Move Down")
                .onAction(MainWindow::splitActiveTabDown)
                .build();
        AnAction moveOppositeGroup = AnAction.builder("tab.move.opposite.group", "Move to Opposite Group")
                .onAction(ctx -> ctx.setStatus("Move to Opposite Group"))
                .build();
        AnAction openOppositeGroup = AnAction.builder("tab.open.opposite.group", "Open in Opposite Group")
                .onAction(ctx -> ctx.setStatus("Open in Opposite Group"))
                .build();
        AnAction changeSplitter = AnAction.builder("tab.change.splitter.orientation", "Change Splitter Orientation")
                .onAction(MainWindow::changeSplitterOrientation)
                .build();
        AnAction unsplit = AnAction.builder("tab.unsplit", "Unsplit")
                .onAction(MainWindow::unsplitActive)
                .build();
        AnAction unsplitAll = AnAction.builder("tab.unsplit.all", "Unsplit All")
                .onAction(MainWindow::unsplitAll)
                .build();

        // Pin, Keep, New window, Configure
        AnAction pinTab = AnAction.builder("tab.pin.active", "Pin Active Tab")
                .onAction(MainWindow::pinActiveTab)
                .build();
        AnAction keepTabOpen = AnAction.builder("tab.keep.open", "Keep Tab Open")
                .onAction(MainWindow::keepTabOpen)
                .build();
        AnAction openInNewWindow = AnAction.builder("tab.open.in.new.window", "Open Tab in New Window")
                .accelerator(new KeyCodeCombination(KeyCode.F4, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Open Tab in New Window"))
                .build();
        AnAction configureTabs = AnAction.builder("tab.configure.editor.tabs", "Configure Editor Tabs…")
                .onAction(MainWindow::showSettingsDialog)
                .build();

        // Reopen closed tab
        AnAction reopenClosed = AnAction.builder("tab.reopen.closed", "Reopen Closed Tab")
                .accelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(MainWindow::reopenLastClosedTab)
                .build();

        // Database.EditorTabPopupMenu [popup=false]
        ActionGroup dbEditorTab = new ActionGroup("database.editor.tab.popup", "Database.EditorTabPopupMenu", false);
        dbEditorTab.addSeparator();
        dbEditorTab.add(AnAction.builder("tab.database.shorten.titles", "Shorten Tab Titles")
                .onAction(ctx -> ctx.setStatus("Shorten Tab Titles"))
                .build());

        // Bookmarks [popup=false]
        ActionGroup tabBookmarks = new ActionGroup("tab.bookmarks", "Bookmarks", false);
        tabBookmarks.add(AnAction.builder("tab.bookmark.add.another.list", "Add Bookmark to Another List")
                .onAction(ctx -> ctx.setStatus("Add Bookmark to Another List"))
                .build());
        tabBookmarks.add(AnAction.builder("tab.bookmark.rename", "Rename Bookmark…")
                .icon(FontAwesomeSolid.EDIT, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Rename Bookmark…"))
                .build());
        tabBookmarks.add(AnAction.builder("tab.bookmark.toggle", "Toggle Bookmark")
                .accelerator(new KeyCodeCombination(KeyCode.F11))
                .onAction(ctx -> ctx.setStatus("Toggle Bookmark"))
                .build());

        // Editor Tab Popup Menu Actions (1) [popup=false]
        ActionGroup actions1 = new ActionGroup("tab.editor.tab.popup.actions.1", "Editor Tab Popup Menu Actions (1)", false);
        actions1.addSeparator();
        actions1.add(AnAction.builder("tab.actions1.change.file.language", "Change File Language")
                .onAction(ctx -> ctx.setStatus("Change File Language"))
                .build());
        actions1.add(AnAction.builder("tab.actions1.associate.file.type", "Associate with File Type…")
                .onAction(ctx -> ctx.setStatus("Associate with File Type"))
                .build());

        // Mark File As [popup=true]
        ActionGroup markFileAs = new ActionGroup("tab.mark.file.as.group", "Mark File As", true, FontAwesomeSolid.FOLDER, "#e0a44c", 11, null);
        markFileAs.add(AnAction.builder("tab.mark.file.override.type", "Override File Type")
                .onAction(ctx -> ctx.setStatus("Override File Type"))
                .build());
        markFileAs.add(AnAction.builder("tab.mark.file.revert.override", "Revert File Type Override")
                .onAction(ctx -> ctx.setStatus("Revert File Type Override"))
                .build());
        actions1.add(markFileAs);
        actions1.addSeparator();

        // Run Configurations
        ActionGroup runConfigs = new ActionGroup("tab.run.configurations.group", "Run Configurations", false);
        ActionGroup runContext = new ActionGroup("tab.run.context.group", "RunContextGroup", false);
        ActionGroup runContextInner = new ActionGroup("tab.run.context.group.inner", "RunContextGroupInner", false);

        ActionGroup executors = new ActionGroup("tab.run.context.executors.group", "RunContextExecutorsGroup", false);
        executors.add(AnAction.builder("run.context.configuration", "Run context configuration").icon(FontAwesomeSolid.PLAY, "#57965c", 11).onAction(ctx -> ctx.setStatus("Run context configuration")).build());
        executors.add(AnAction.builder("debug.context.configuration", "Debug context configuration").icon(FontAwesomeSolid.BUG, "#e05555", 11).onAction(ctx -> ctx.setStatus("Debug context configuration")).build());

        ActionGroup moreRunDebug = new ActionGroup("tab.more.run.debug.group", "More Run/Debug", false);
        moreRunDebug.add(AnAction.builder("run.coverage.context.configuration", "Run with Coverage context configuration").icon(FontAwesomeSolid.SHIELD_ALT, "#57965c", 11).onAction(ctx -> ctx.setStatus("Run with Coverage context configuration")).build());
        moreRunDebug.add(AnAction.builder("run.profiler.context.configuration", "Run with Profiler").onAction(ctx -> ctx.setStatus("Run with Profiler")).build());
        moreRunDebug.add(AnAction.builder("run.create.configuration", "Create Run Configuration").onAction(ctx -> ctx.setStatus("Create Run Configuration")).build());
        moreRunDebug.addSeparator();
        moreRunDebug.add(AnAction.builder("run.context.configuration.secondary", "Run context configuration").icon(FontAwesomeSolid.PLAY, "#57965c", 11).onAction(ctx -> ctx.setStatus("Run context configuration")).build());
        moreRunDebug.add(AnAction.builder("debug.context.configuration.secondary", "Debug context configuration").icon(FontAwesomeSolid.BUG, "#e05555", 11).onAction(ctx -> ctx.setStatus("Debug context configuration")).build());
        moreRunDebug.add(AnAction.builder("run.modify.configuration", "Modify Run Configuration…").onAction(ctx -> ctx.setStatus("Modify Run Configuration…")).build());

        runContextInner.addAll(executors, moreRunDebug);
        runContext.add(runContextInner);
        runContext.addSeparator();

        ActionGroup consoleJdbc = new ActionGroup("console.jdbc.run.context.group", "Console.Jdbc.RunContextGroup", false);
        consoleJdbc.add(AnAction.builder("console.attach.data.source", "Attach Data Source").onAction(ctx -> ctx.setStatus("Attach Data Source")).build());
        consoleJdbc.addSeparator();
        consoleJdbc.add(AnAction.builder("console.recompile", "Recompile…").icon(FontAwesomeSolid.HAMMER, "#a9b7c6", 11).onAction(ctx -> ctx.setStatus("Recompile…")).build());

        ActionGroup explainPlan = new ActionGroup("console.explain.plan.group", "Explain Plan", true);
        explainPlan.add(AnAction.builder("console.explain.plan", "Explain Plan").icon(FontAwesomeSolid.PROJECT_DIAGRAM, "#a9b7c6", 11).onAction(ctx -> ctx.setStatus("Explain Plan")).build());
        explainPlan.add(AnAction.builder("console.explain.plan.raw", "Explain Plan (Raw)").icon(FontAwesomeSolid.PROJECT_DIAGRAM, "#a9b7c6", 11).onAction(ctx -> ctx.setStatus("Explain Plan (Raw)")).build());
        explainPlan.add(AnAction.builder("console.explain.analyse", "Explain Analyse").icon(FontAwesomeSolid.PROJECT_DIAGRAM, "#a9b7c6", 11).onAction(ctx -> ctx.setStatus("Explain Analyse")).build());
        explainPlan.add(AnAction.builder("console.explain.analyse.raw", "Explain Analyse (Raw)").icon(FontAwesomeSolid.PROJECT_DIAGRAM, "#a9b7c6", 11).onAction(ctx -> ctx.setStatus("Explain Analyse (Raw)")).build());

        consoleJdbc.addAll(explainPlan,
                AnAction.builder("middle.run", "Execute").icon(FontAwesomeSolid.PLAY, "#57965c", 11).onAction(ctx -> ctx.setStatus("Execute")).build(),
                AnAction.builder("editor.execute.selection.single", "Execute Selection as Single Statement").icon(FontAwesomeSolid.PLAY, "#57965c", 11).onAction(ctx -> ctx.setStatus("Execute Selection as Single Statement")).build(),
                AnAction.builder("editor.export.data", "Export Data…").icon(FontAwesomeSolid.DOWNLOAD, "#4a88c7", 11).onAction(ctx -> ctx.setStatus("Export Data…")).build(),
                AnAction.builder("editor.debug", "Debug").icon(FontAwesomeSolid.BUG, "#e05555", 11).onAction(ctx -> ctx.setStatus("Debug")).build(),
                AnAction.builder("editor.debug.routine", "Debug Routine…").icon(FontAwesomeSolid.BUG, "#e05555", 11).onAction(ctx -> ctx.setStatus("Debug Routine…")).build()
        );
        consoleJdbc.addSeparator();
        consoleJdbc.add(AnAction.builder("editor.migrate.consoles.to.queryfiles", "Migrate Query Consoles to Query Files…")
                .icon(FontAwesomeSolid.ARROW_RIGHT, "#a9b7c6", 11)
                .onAction(ctx -> ctx.setStatus("Migrate Query Consoles to Query Files…"))
                .build());

        runContext.add(consoleJdbc);
        runConfigs.add(runContext);
        actions1.add(runConfigs);

        // SplitRevealGroup (Image: media_1790694503981.png)
        ActionGroup splitRevealGroup = new ActionGroup("tab.split.reveal.group", "SplitRevealGroup", false);
        splitRevealGroup.add(AnAction.builder("editor.open.in.right.split", "Open in Right Split")
                .icon(FontAwesomeSolid.COLUMNS, "#a9b7c6", 11)
                .onAction(MainWindow::splitActiveTabRight)
                .build());
        splitRevealGroup.add(AnAction.builder("editor.open.in.split.chooser", "Open in Split with Chooser\u2026")
                .onAction(ctx -> ctx.setStatus("Open in Split with Chooser\u2026"))
                .build());

        ActionGroup openInGroup = new ActionGroup("editor.open.in.group", "Open In", true);
        openInGroup.add(AnAction.builder("editor.open.in.file.manager", "Show in File Manager").onAction(ctx -> ctx.setStatus("Show in File Manager")).build());
        openInGroup.add(AnAction.builder("editor.open.in.associated.app", "Open in Associated Application").onAction(ctx -> ctx.setStatus("Open in Associated Application")).build());
        openInGroup.add(AnAction.builder("editor.open.in.browser", "Open in Browser").icon(FontAwesomeSolid.GLOBE, "#6897bb", 11).onAction(ctx -> ctx.setStatus("Open in Browser")).build());
        openInGroup.add(AnAction.builder("editor.open.in.file.path", "File Path").onAction(ctx -> ctx.setStatus("File Path")).build());
        openInGroup.add(AnAction.builder("editor.open.in.terminal", "Open in Terminal").icon(FontAwesomeSolid.TERMINAL, "#6897bb", 11).onAction(ctx -> ctx.setStatus("Open in Terminal")).build());
        openInGroup.add(AnAction.builder("editor.open.in.terminal.second", "Open in Terminal").icon(FontAwesomeSolid.TERMINAL, "#6897bb", 11).onAction(ctx -> ctx.setStatus("Open in Terminal")).build());
        openInGroup.add(AnAction.builder("git.hosting.open.in.browser.group", "Git.Hosting.Open.In.Browser.Group").onAction(ctx -> ctx.setStatus("Open on Git Hosting")).build());
        splitRevealGroup.add(openInGroup);

        actions1.add(splitRevealGroup);
        actions1.addSeparator();

        // VCS/LVCS Actions (Image: media_1790694503981.png)
        ActionGroup vcsLvcsGroup = new ActionGroup("tab.vcs.lvcs.actions.group", "VCS/LVCS Actions", false);
        ActionGroup localHistoryGroup = new ActionGroup("editor.local.history.group", "Local History", true);
        localHistoryGroup.add(AnAction.builder("editor.local.history.show", "Show History\u2026").onAction(MainWindow::showLocalHistoryForCurrentConsole).build());
        localHistoryGroup.add(AnAction.builder("editor.local.history.show.selection", "Show History for Selection\u2026").onAction(MainWindow::showHistoryForSelection).build());
        localHistoryGroup.addSeparator();
        localHistoryGroup.add(AnAction.builder("editor.local.history.show.project", "Show Project History\u2026").onAction(MainWindow::showProjectHistoryDialog).build());
        localHistoryGroup.add(AnAction.builder("editor.local.history.recent.changes", "Recent Changes").onAction(MainWindow::showRecentChangesDialog).build());
        localHistoryGroup.add(AnAction.builder("editor.local.history.put.label", "Put Label\u2026").onAction(MainWindow::showPutLabelDialog).build());
        localHistoryGroup.add(AnAction.builder("editor.local.history.vcs.group", "Version Control Group").onAction(MainWindow::showVcsOperationsPopup).build());
        vcsLvcsGroup.add(localHistoryGroup);
        vcsLvcsGroup.addSeparator();
        vcsLvcsGroup.add(AnAction.builder("editor.external.tools", "External Tools").onAction(ctx -> ctx.setStatus("External Tools")).build());
        vcsLvcsGroup.add(AnAction.builder("tab.rename.file", "Rename File\u2026").onAction(ctx -> ctx.setStatus("Rename File\u2026")).build());

        actions1.add(vcsLvcsGroup);

        // Register all groups and standalone actions
        manager.registerGroup(closeActions);
        manager.registerAction(copyPaths);
        manager.registerAction(copyReference);
        manager.registerAction(copyJsonPointer);
        manager.registerAction(changeTemplateLang);
        manager.registerGroup(copyPathRefGroup);
        manager.registerGroup(copyFileRef);
        manager.registerGroup(copyExtRef);
        manager.registerAction(showDiffSeparate);
        manager.registerAction(showAllDiff);
        manager.registerGroup(vcsDiffGroup);
        manager.registerAction(splitRight);
        manager.registerAction(splitMoveRight);
        manager.registerAction(splitDown);
        manager.registerAction(splitMoveDown);
        manager.registerAction(moveOppositeGroup);
        manager.registerAction(openOppositeGroup);
        manager.registerAction(changeSplitter);
        manager.registerAction(unsplit);
        manager.registerAction(unsplitAll);
        manager.registerAction(pinTab);
        manager.registerAction(keepTabOpen);
        manager.registerAction(openInNewWindow);
        manager.registerAction(configureTabs);
        manager.registerAction(reopenClosed);
        manager.registerGroup(dbEditorTab);
        manager.registerGroup(tabBookmarks);
        manager.registerGroup(actions1);
        manager.registerGroup(markFileAs);
        manager.registerGroup(runConfigs);
        manager.registerGroup(runContext);
        manager.registerGroup(runContextInner);
        manager.registerGroup(executors);
        manager.registerGroup(moreRunDebug);
        manager.registerGroup(consoleJdbc);
        manager.registerGroup(explainPlan);
        manager.registerGroup(splitRevealGroup);
        manager.registerGroup(openInGroup);
        manager.registerGroup(vcsLvcsGroup);
        manager.registerGroup(localHistoryGroup);
        manager.registerAction(AnAction.builder("tab.rename.file", "Rename File\u2026").onAction(ctx -> ctx.setStatus("Rename File\u2026")).build());
    }

    private static void registerProjectViewPopupMenuActions(ActionManager manager) {
        AnAction attachDir = manager.getAction("file.attach.directory");
        if (attachDir == null) {
            attachDir = AnAction.builder("file.attach.directory", "Attach Directory to Project\u2026")
                    .icon(FontAwesomeSolid.FOLDER_PLUS)
                    .onAction(ctx -> ctx.setStatus("Attach Directory to Project\u2026"))
                    .build();
            manager.registerAction(attachDir);
        }

        AnAction assocFileType = AnAction.builder("project.associate.file.type", "Associate with File Type\u2026")
                .onAction(ctx -> ctx.setStatus("Associate with File Type\u2026"))
                .build();
        AnAction restoreDefaultExts = AnAction.builder("project.restore.default.extensions", "Restore Default Extensions")
                .onAction(ctx -> ctx.setStatus("Restore Default Extensions"))
                .build();

        AnAction cutAction = manager.getAction("edit.cut");
        AnAction copyAction = manager.getAction("edit.copy");
        AnAction copyPaths = AnAction.builder("project.copy.paths", "Copy Paths")
                .onAction(ctx -> ctx.setStatus("Copy Paths"))
                .build();
        AnAction copyPlainText = manager.getAction("edit.copy.plain.text");
        AnAction copyRichText = manager.getAction("edit.copy.rich.text");

        AnAction copyPathAbsolute = AnAction.builder("project.copy.path.absolute", "Absolute Path")
                .accelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Copy Absolute Path"))
                .build();
        AnAction copyPathFileName = AnAction.builder("project.copy.path.filename", "File Name")
                .onAction(ctx -> ctx.setStatus("Copy File Name"))
                .build();
        AnAction copyPathLineNumber = AnAction.builder("project.copy.path.line.number", "Path with Line Number")
                .onAction(ctx -> ctx.setStatus("Copy Path with Line Number"))
                .build();
        AnAction copyPathContentRoot = AnAction.builder("project.copy.path.content.root", "Path from Content Root")
                .onAction(ctx -> ctx.setStatus("Copy Path from Content Root"))
                .build();
        AnAction copyPathSourceRoot = AnAction.builder("project.copy.path.source.root", "Path from Source Root")
                .onAction(ctx -> ctx.setStatus("Copy Path from Source Root"))
                .build();
        AnAction copyPathRepoRoot = AnAction.builder("project.copy.path.repo.root", "Path From Repository Root")
                .onAction(ctx -> ctx.setStatus("Copy Path from Repository Root"))
                .build();
        AnAction copyGitHostingLink = AnAction.builder("project.copy.git.hosting.link", "Git.Hosting.Copy.Link.Group")
                .onAction(ctx -> ctx.setStatus("Git Hosting Copy Link"))
                .build();
        AnAction copyToolboxUrl = AnAction.builder("project.copy.toolbox.url", "Toolbox URL")
                .icon(FontAwesomeSolid.TOOLBOX)
                .onAction(ctx -> ctx.setStatus("Copy Toolbox URL"))
                .build();
        AnAction copyReference = AnAction.builder("project.copy.ref.action", "Copy Reference")
                .accelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Copy Reference"))
                .build();

        AnAction pasteAction = manager.getAction("edit.paste");
        AnAction pasteFromHistory = manager.getAction("edit.paste.from.history");
        AnAction pastePlainText = manager.getAction("edit.paste.plain.text");
        AnAction copyJsonPointer = AnAction.builder("project.copy.json.pointer", "Copy JSON Pointer")
                .onAction(ctx -> ctx.setStatus("Copy JSON Pointer"))
                .build();

        AnAction importData = AnAction.builder("file.import.data", "Import to Database\u2026")
                .icon(FontAwesomeSolid.UPLOAD)
                .onAction(ctx -> ctx.setStatus("Import to Database\u2026"))
                .build();
        AnAction editSource = AnAction.builder("project.edit.source", "Edit Source")
                .accelerator(new KeyCodeCombination(KeyCode.F4))
                .onAction(ctx -> ctx.setStatus("Edit Source"))
                .build();
        AnAction applyPatch = AnAction.builder("project.apply.patch", "Apply Patch\u2026")
                .onAction(ctx -> ctx.setStatus("Apply Patch\u2026"))
                .build();

        AnAction inspectCode = manager.getAction("code.inspect.code");
        AnAction renameInner = AnAction.builder("refactor.rename.inner", "Rename\u2026")
                .accelerator(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN))
                .onAction(ctx -> ctx.setStatus("Rename"))
                .build();

        AnAction cacheRecovery = AnAction.builder("project.cache.recovery", "Cache Recovery")
                .onAction(ctx -> ctx.setStatus("Cache Recovery"))
                .build();
        AnAction reloadFromDisk = AnAction.builder("file.reload.disk", "Reload from Disk")
                .icon(FontAwesomeSolid.SYNC)
                .accelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN))
                .onAction(ctx -> ctx.setStatus("Reload from Disk"))
                .build();
        AnAction gotoLinkTarget = AnAction.builder("project.goto.link.target", "Go to Link Target")
                .onAction(ctx -> ctx.setStatus("Go to Link Target"))
                .build();
        AnAction compareFiles = AnAction.builder("project.compare.files", "Compare Files")
                .icon(FontAwesomeSolid.ARROW_RIGHT)
                .accelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN))
                .onAction(ctx -> ctx.setStatus("Compare Files"))
                .build();
        AnAction compareFileWithEditor = AnAction.builder("project.compare.file.editor", "Compare File with Editor")
                .onAction(ctx -> ctx.setStatus("Compare File with Editor"))
                .build();
        AnAction externalTools = AnAction.builder("project.external.tools", "External Tools")
                .onAction(ctx -> ctx.setStatus("External Tools"))
                .build();
        AnAction setBackgroundImage = AnAction.builder("project.set.background.image", "Set Background Image")
                .onAction(ctx -> ctx.setStatus("Set Background Image"))
                .build();

        AnAction diagFileAssoc = AnAction.builder("project.diagrams.file.associations", "File Associations")
                .onAction(ctx -> ctx.setStatus("File Associations"))
                .build();
        AnAction diagJumpExtEditor = AnAction.builder("project.diagrams.jump.external.editor", "Jump to External Editor")
                .onAction(ctx -> ctx.setStatus("Jump to External Editor"))
                .build();
        AnAction diagConvertToPng = AnAction.builder("project.diagrams.convert.png", "Convert to PNG")
                .onAction(ctx -> ctx.setStatus("Convert to PNG"))
                .build();

        // Groups
        ActionGroup copyFileRef = new ActionGroup("project.copy.file.ref", "CopyFileReference", false);
        copyFileRef.addAll(copyPathAbsolute, copyPathFileName)
                .addSeparator()
                .addAll(copyPathLineNumber, copyPathContentRoot, copyPathSourceRoot, copyPathRepoRoot, copyGitHostingLink);

        ActionGroup copyExtRef = new ActionGroup("project.copy.ext.ref", "CopyExternalReferenceGroup", false);
        copyExtRef.add(copyToolboxUrl);

        ActionGroup copyPathRefGroup = new ActionGroup("project.copy.path.ref.group", "Copy Path/Reference\u2026", true);
        copyPathRefGroup.add(copyFileRef)
                .addSeparator()
                .add(copyExtRef)
                .add(copyReference);

        ActionGroup pasteGroup = new ActionGroup("project.paste.group", "Paste", true);
        if (pasteAction != null) pasteGroup.add(pasteAction);
        if (pasteFromHistory != null) pasteGroup.add(pasteFromHistory);
        if (pastePlainText != null) pasteGroup.add(pastePlainText);

        ActionGroup cutCopyPaste = new ActionGroup("project.cut.copy.paste", "Cut/Copy/Paste Actions", false);
        if (cutAction != null) cutCopyPaste.add(cutAction);
        if (copyAction != null) cutCopyPaste.add(copyAction);
        cutCopyPaste.add(copyPaths);
        if (copyPlainText != null) cutCopyPaste.add(copyPlainText);
        if (copyRichText != null) cutCopyPaste.add(copyRichText);
        cutCopyPaste.add(copyPathRefGroup)
                .add(pasteGroup)
                .add(copyJsonPointer);

        ActionGroup importGroup = new ActionGroup("fileeditor.import.to.database.group", "FileEditor.ImportToDatabase.Group", false);
        importGroup.addSeparator().add(importData);

        ActionGroup applyPatchGroup = new ActionGroup("changesview.applypatch.langgroup", "ChangesView.ApplyPatch.LangGroup", false);
        applyPatchGroup.add(applyPatch);

        ActionGroup inspectCodeGroup = new ActionGroup("project.inspect.code.action", "InspectCodeActionInPopupMenus", false);
        if (inspectCode != null) inspectCodeGroup.add(inspectCode);

        // Refactoring groups
        ActionGroup refactorSubmenu = new ActionGroup("project.refactor.submenu", "Refactor", true);
        AnAction refactorThis = manager.getAction("refactor.this");
        AnAction refactorChangeSig = manager.getAction("refactor.change.signature");
        AnAction refactorModifyObj = manager.getAction("refactor.modify.object");
        ActionGroup extractIntroduce = manager.getGroup("refactor.extract.introduce.group");
        AnAction refactorInline = manager.getAction("refactor.inline");
        AnAction refactorMove = manager.getAction("refactor.move");
        AnAction refactorCopy = manager.getAction("refactor.copy");
        AnAction refactorSafeDel = manager.getAction("refactor.safe.delete");
        AnAction refactorPullUp = manager.getAction("refactor.pull.members.up");
        AnAction refactorPushDown = manager.getAction("refactor.push.members.down");
        AnAction refactorInvertBool = manager.getAction("refactor.invert.boolean");

        if (refactorThis != null) refactorSubmenu.add(refactorThis);
        refactorSubmenu.add(renameInner);
        if (refactorChangeSig != null) refactorSubmenu.add(refactorChangeSig);
        if (refactorModifyObj != null) refactorSubmenu.add(refactorModifyObj);
        refactorSubmenu.addSeparator();
        if (extractIntroduce != null) refactorSubmenu.add(extractIntroduce);
        if (refactorInline != null) refactorSubmenu.add(refactorInline);
        refactorSubmenu.addSeparator();
        if (refactorMove != null) refactorSubmenu.add(refactorMove);
        if (refactorCopy != null) refactorSubmenu.add(refactorCopy);
        if (refactorSafeDel != null) refactorSubmenu.add(refactorSafeDel);
        refactorSubmenu.addSeparator();
        if (refactorPullUp != null) refactorSubmenu.add(refactorPullUp);
        if (refactorPushDown != null) refactorSubmenu.add(refactorPushDown);
        if (refactorInvertBool != null) refactorSubmenu.add(refactorInvertBool);

        ActionGroup projectRefactorGroup = new ActionGroup("project.refactoring.group", "Project View Popup Refactoring Group", false);
        projectRefactorGroup.add(refactorSubmenu);

        // Anonymous group 0 (Bookmarks)
        ActionGroup projBookmarksGroup = new ActionGroup("project.bookmarks", "Bookmarks", true);
        AnAction bmAddAnother = manager.getAction("tab.bookmark.add.another.list");
        AnAction bmRename = manager.getAction("tab.bookmark.rename");
        AnAction bmToggle = manager.getAction("tab.bookmark.toggle");
        if (bmAddAnother != null) projBookmarksGroup.add(bmAddAnother);
        if (bmRename != null) projBookmarksGroup.add(bmRename);
        if (bmToggle != null) projBookmarksGroup.add(bmToggle);

        ActionGroup anonGroup0 = new ActionGroup("project.anonymous.group.0", "<anonymous-group-0>", false);
        anonGroup0.addSeparator().add(projBookmarksGroup).addSeparator();

        // Modify Group
        AnAction detachDir = AnAction.builder("project.mark.file.detach.directory", "Detach Directory from Project\u2026")
                .onAction(ctx -> ctx.setStatus("Detach Directory from Project\u2026"))
                .build();
        AnAction excludeProj = AnAction.builder("project.mark.file.exclude", "Exclude from Project")
                .onAction(ctx -> ctx.setStatus("Exclude from Project"))
                .build();
        AnAction includeProj = AnAction.builder("project.mark.file.include", "Include to Project")
                .onAction(ctx -> ctx.setStatus("Include to Project"))
                .build();

        ActionGroup markFileAsGroup = new ActionGroup("project.mark.file.as", "Mark File As", true);
        AnAction markOverride = manager.getAction("tab.mark.file.override.type");
        AnAction markRevert = manager.getAction("tab.mark.file.revert.override");
        if (markOverride != null) markFileAsGroup.add(markOverride);
        if (markRevert != null) markFileAsGroup.add(markRevert);
        markFileAsGroup.addAll(detachDir, excludeProj, includeProj);

        ActionGroup modifyGroup = new ActionGroup("project.modify.group", "Project View Popup Menu Modify Group", false);
        AnAction codeReformat = manager.getAction("code.reformat");
        AnAction codeReformatJson = manager.getAction("code.reformat.json");
        AnAction codeOptimize = manager.getAction("code.optimize.imports");
        AnAction editDelete = manager.getAction("edit.delete");
        AnAction changeFileLang = manager.getAction("editor.sql.change.file.language");
        AnAction changeSqlDialect = manager.getAction("editor.sql.change.sql.dialect");
        if (codeReformat != null) modifyGroup.add(codeReformat);
        if (codeReformatJson != null) modifyGroup.add(codeReformatJson);
        if (codeOptimize != null) modifyGroup.add(codeOptimize);
        if (editDelete != null) modifyGroup.add(editDelete);
        if (changeFileLang != null) modifyGroup.add(changeFileLang);
        if (changeSqlDialect != null) modifyGroup.add(changeSqlDialect);
        modifyGroup.add(markFileAsGroup);

        // Run Group
        ActionGroup projExecutorsGroup = new ActionGroup("project.run.context.executors", "RunContextExecutorsGroup", false);
        AnAction runCtxConfig = manager.getAction("run.context.configuration");
        AnAction debugCtxConfig = manager.getAction("debug.context.configuration");
        if (runCtxConfig != null) projExecutorsGroup.add(runCtxConfig);
        if (debugCtxConfig != null) projExecutorsGroup.add(debugCtxConfig);

        ActionGroup projMoreRunDebugGroup = new ActionGroup("project.more.run.debug.group", "More Run/Debug", false);
        AnAction runCoverage = manager.getAction("run.coverage.context.configuration");
        AnAction runProfiler = manager.getAction("run.profiler.context.configuration");
        AnAction createRunConfig = manager.getAction("run.create.configuration");
        AnAction runCtxSec = manager.getAction("run.context.configuration.secondary");
        AnAction debugCtxSec = manager.getAction("debug.context.configuration.secondary");
        AnAction modifyRunConfig = manager.getAction("run.modify.configuration");
        if (runCoverage != null) projMoreRunDebugGroup.add(runCoverage);
        if (runProfiler != null) projMoreRunDebugGroup.add(runProfiler);
        if (createRunConfig != null) projMoreRunDebugGroup.add(createRunConfig);
        projMoreRunDebugGroup.addSeparator();
        if (runCtxSec != null) projMoreRunDebugGroup.add(runCtxSec);
        if (debugCtxSec != null) projMoreRunDebugGroup.add(debugCtxSec);
        if (modifyRunConfig != null) projMoreRunDebugGroup.add(modifyRunConfig);

        ActionGroup projRunCtxInner = new ActionGroup("project.run.context.group.inner", "RunContextGroupInner", false);
        projRunCtxInner.addAll(projExecutorsGroup, projMoreRunDebugGroup);

        ActionGroup projRunCtxGroup = new ActionGroup("project.run.context.group", "RunContextGroup", false);
        projRunCtxGroup.add(projRunCtxInner).addSeparator();

        ActionGroup projRunConfigsGroup = new ActionGroup("project.run.configurations.group", "Run Configurations", false);
        projRunConfigsGroup.add(projRunCtxGroup);

        ActionGroup projRunGroup = new ActionGroup("project.run.group", "Project View Popup Menu Run Group", false);
        projRunGroup.add(projRunConfigsGroup);
        ActionGroup consoleJdbcGroup = manager.getGroup("console.jdbc.run.context.group");
        if (consoleJdbcGroup != null) projRunGroup.add(consoleJdbcGroup);

        // SplitRevealGroup
        ActionGroup projSplitRevealGroup = new ActionGroup("project.split.reveal.group", "SplitRevealGroup", false);
        AnAction openSplitRight = manager.getAction("editor.open.in.right.split");
        AnAction openSplitChooser = manager.getAction("editor.open.in.split.chooser");
        ActionGroup openInGroup = manager.getGroup("editor.open.in.group");
        if (openSplitRight != null) projSplitRevealGroup.add(openSplitRight);
        if (openSplitChooser != null) projSplitRevealGroup.add(openSplitChooser);
        if (openInGroup != null) projSplitRevealGroup.add(openInGroup);
        projSplitRevealGroup.addSeparator();

        // Settings Group
        AnAction markDirAs = AnAction.builder("project.mark.directory.as", "Mark Directory As")
                .onAction(ctx -> ctx.setStatus("Mark Directory As"))
                .build();
        ActionGroup projSettingsGroup = new ActionGroup("project.settings.group", "Project View Popup Menu Settings Group", false);
        projSettingsGroup.add(markDirAs);

        // Diagrams
        ActionGroup diagramsGroup = new ActionGroup("project.diagrams", "Diagrams", true);
        AnAction showUml = manager.getAction("diagrams.show.uml");
        AnAction showUmlPopup = manager.getAction("diagrams.show.uml.popup");
        AnAction showLocalChangesUml = manager.getAction("vcs.show.local.changes.uml");
        if (showUml != null) diagramsGroup.add(showUml);
        if (showUmlPopup != null) diagramsGroup.add(showUmlPopup);
        if (showLocalChangesUml != null) diagramsGroup.add(showLocalChangesUml);
        diagramsGroup.addAll(diagFileAssoc, diagJumpExtEditor, diagConvertToPng);

        // Register actions
        manager.registerAction(assocFileType);
        manager.registerAction(restoreDefaultExts);
        manager.registerAction(copyPaths);
        manager.registerAction(copyPathAbsolute);
        manager.registerAction(copyPathFileName);
        manager.registerAction(copyPathLineNumber);
        manager.registerAction(copyPathContentRoot);
        manager.registerAction(copyPathSourceRoot);
        manager.registerAction(copyPathRepoRoot);
        manager.registerAction(copyGitHostingLink);
        manager.registerAction(copyToolboxUrl);
        manager.registerAction(copyReference);
        manager.registerAction(copyJsonPointer);
        manager.registerAction(importData);
        manager.registerAction(editSource);
        manager.registerAction(applyPatch);
        manager.registerAction(renameInner);
        manager.registerAction(detachDir);
        manager.registerAction(excludeProj);
        manager.registerAction(includeProj);
        manager.registerAction(markDirAs);
        manager.registerAction(cacheRecovery);
        manager.registerAction(reloadFromDisk);
        manager.registerAction(gotoLinkTarget);
        manager.registerAction(compareFiles);
        manager.registerAction(compareFileWithEditor);
        manager.registerAction(externalTools);
        manager.registerAction(setBackgroundImage);
        manager.registerAction(diagFileAssoc);
        manager.registerAction(diagJumpExtEditor);
        manager.registerAction(diagConvertToPng);

        // Navigation Bar Popup specific actions
        manager.registerAction(AnAction.builder("navbar.associate.file.type", "Associate with File Type\u2026")
                .onAction(ctx -> ctx.setStatus("Associate with File Type"))
                .build());
        manager.registerAction(AnAction.builder("navbar.jump.source", "Jump to Source")
                .icon(FontAwesomeSolid.EDIT)
                .onAction(ctx -> ctx.setStatus("Jump to Source"))
                .build());
        manager.registerAction(AnAction.builder("navbar.navigation.bar", "Navigation Bar")
                .onAction(ctx -> ctx.setStatus("Navigation Bar"))
                .build());
        manager.registerAction(AnAction.builder("navbar.members.navigation.bar", "Members in Navigation Bar")
                .onAction(ctx -> ctx.setStatus("Members in Navigation Bar"))
                .build());
        manager.registerAction(AnAction.builder("navbar.reload.disk", "Reload from Disk")
                .icon(FontAwesomeSolid.SYNC)
                .onAction(ctx -> ctx.setStatus("Reload from Disk"))
                .build());
        manager.registerAction(AnAction.builder("navbar.external.tools", "External Tools")
                .onAction(ctx -> ctx.setStatus("External Tools"))
                .build());

        // Register groups
        manager.registerGroup(copyFileRef);
        manager.registerGroup(copyExtRef);
        manager.registerGroup(copyPathRefGroup);
        manager.registerGroup(pasteGroup);
        manager.registerGroup(cutCopyPaste);
        manager.registerGroup(importGroup);
        manager.registerGroup(applyPatchGroup);
        manager.registerGroup(inspectCodeGroup);
        manager.registerGroup(refactorSubmenu);
        manager.registerGroup(projectRefactorGroup);
        manager.registerGroup(projBookmarksGroup);
        manager.registerGroup(anonGroup0);
        manager.registerGroup(markFileAsGroup);
        manager.registerGroup(modifyGroup);
        manager.registerGroup(projExecutorsGroup);
        manager.registerGroup(projMoreRunDebugGroup);
        manager.registerGroup(projRunCtxInner);
        manager.registerGroup(projRunCtxGroup);
        manager.registerGroup(projRunConfigsGroup);
        manager.registerGroup(projRunGroup);
        manager.registerGroup(projSplitRevealGroup);
        manager.registerGroup(projSettingsGroup);
        manager.registerGroup(diagramsGroup);
    }

    private static void registerNavBarAndDebugAndHistoryToolbarsActions(ActionManager manager) {
        // Navigation Bar Toolbar Actions
        if (manager.getAction("middle.run") == null) {
            manager.registerAction(AnAction.builder("middle.run", "Run")
                    .icon(FontAwesomeSolid.PLAY, "#57965c", 11)
                    .onAction(ctx -> ctx.setStatus("Run"))
                    .build());
        }
        if (manager.getAction("debug.start") == null) {
            manager.registerAction(AnAction.builder("debug.start", "Debug")
                    .icon(FontAwesomeSolid.BUG, "#e05555", 11)
                    .onAction(ctx -> ctx.setStatus("Debug"))
                    .build());
        }
        if (manager.getAction("navbar.select.run.debug.config") == null) {
            manager.registerAction(AnAction.builder("navbar.select.run.debug.config", "Select Run/Debug Configuration")
                    .onAction(ctx -> ctx.setStatus("Select Run/Debug Configuration"))
                    .build());
        }
        if (manager.getAction("debug.stop") == null) {
            manager.registerAction(AnAction.builder("debug.stop", "Stop")
                    .icon(FontAwesomeSolid.STOP, "#e05555", 11)
                    .onAction(ctx -> ctx.setStatus("Stop"))
                    .build());
        }
        if (manager.getAction("navbar.vcs.label") == null) {
            manager.registerAction(AnAction.builder("navbar.vcs.label", "VCS Label")
                    .onAction(ctx -> ctx.setStatus("VCS Label"))
                    .build());
        }
        if (manager.getAction("vcs.commit.primary") == null) {
            manager.registerAction(AnAction.builder("vcs.commit.primary", "Commit\u2026")
                    .icon(FontAwesomeSolid.CHECK, "#57965c", 11)
                    .onAction(ctx -> ctx.setStatus("Commit\u2026"))
                    .build());
        }
        if (manager.getAction("vcs.commit.secondary") == null) {
            manager.registerAction(AnAction.builder("vcs.commit.secondary", "Commit\u2026")
                    .icon(FontAwesomeSolid.CHECK, "#57965c", 11)
                    .onAction(ctx -> ctx.setStatus("Commit\u2026"))
                    .build());
        }
        if (manager.getAction("vcs.toggle.commit.ui") == null) {
            manager.registerAction(AnAction.builder("vcs.toggle.commit.ui", "Toggle Commit UI\u2026")
                    .onAction(ctx -> ctx.setStatus("Toggle Commit UI\u2026"))
                    .build());
        }
        if (manager.getAction("vcs.push") == null) {
            manager.registerAction(AnAction.builder("vcs.push", "Push\u2026")
                    .icon(FontAwesomeSolid.UPLOAD, "#4a88c7", 11)
                    .onAction(ctx -> ctx.setStatus("Push\u2026"))
                    .build());
        }
        if (manager.getAction("vcs.show.history") == null) {
            manager.registerAction(AnAction.builder("vcs.show.history", "Show History")
                    .icon(FontAwesomeSolid.HISTORY, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Show History"))
                    .build());
        }
        if (manager.getAction("vcs.rollback") == null) {
            manager.registerAction(AnAction.builder("vcs.rollback", "Rollback\u2026")
                    .icon(FontAwesomeSolid.UNDO, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Rollback\u2026"))
                    .build());
        }
        if (manager.getAction("navbar.toolbar.others") == null) {
            manager.registerAction(AnAction.builder("navbar.toolbar.others", "NavBarToolBarOthers")
                    .onAction(ctx -> ctx.setStatus("NavBarToolBarOthers"))
                    .build());
        }
        if (manager.getAction("navbar.ai.assistant") == null) {
            manager.registerAction(AnAction.builder("navbar.ai.assistant", "AIAssistantHubPopupAction")
                    .icon(FontAwesomeSolid.ROBOT, "#9876aa", 11)
                    .onAction(ctx -> ctx.setStatus("AIAssistantHubPopupAction"))
                    .build());
        }
        if (manager.getAction("nav.search.everywhere") == null) {
            manager.registerAction(AnAction.builder("nav.search.everywhere", "Search Everywhere")
                    .icon(FontAwesomeSolid.SEARCH, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Search Everywhere"))
                    .build());
        }

        // Navigation Bar Toolbar Groups
        ActionGroup navRunDebugGroup = new ActionGroup("navbar.toolbar.run.debug.group", "Run/Debug", true);
        if (manager.getAction("middle.run") != null) navRunDebugGroup.add(manager.getAction("middle.run"));
        if (manager.getAction("debug.start") != null) navRunDebugGroup.add(manager.getAction("debug.start"));
        if (manager.getAction("run.coverage.context.configuration") != null) navRunDebugGroup.add(manager.getAction("run.coverage.context.configuration"));
        if (manager.getAction("run.profiler.context.configuration") != null) navRunDebugGroup.add(manager.getAction("run.profiler.context.configuration"));
        manager.registerGroup(navRunDebugGroup);

        ActionGroup navTbRunActions = new ActionGroup("navbar.toolbar.run.actions", "Toolbar Run Actions", false);
        if (manager.getAction("navbar.select.run.debug.config") != null) navTbRunActions.add(manager.getAction("navbar.select.run.debug.config"));
        navTbRunActions.add(navRunDebugGroup);
        if (manager.getAction("debug.stop") != null) navTbRunActions.add(manager.getAction("debug.stop"));
        navTbRunActions.add(ActionSeparator.getInstance());
        manager.registerGroup(navTbRunActions);

        ActionGroup navVcsTbActions = new ActionGroup("navbar.vcs.actions", "VcsNavBarToolbarActions", false);
        if (manager.getAction("navbar.vcs.label") != null) navVcsTbActions.add(manager.getAction("navbar.vcs.label"));
        if (manager.getAction("vcs.update.project") != null) navVcsTbActions.add(manager.getAction("vcs.update.project"));
        if (manager.getAction("vcs.commit.primary") != null) navVcsTbActions.add(manager.getAction("vcs.commit.primary"));
        if (manager.getAction("vcs.commit.secondary") != null) navVcsTbActions.add(manager.getAction("vcs.commit.secondary"));
        if (manager.getAction("vcs.toggle.commit.ui") != null) navVcsTbActions.add(manager.getAction("vcs.toggle.commit.ui"));
        if (manager.getAction("vcs.push") != null) navVcsTbActions.add(manager.getAction("vcs.push"));
        if (manager.getAction("vcs.show.history") != null) navVcsTbActions.add(manager.getAction("vcs.show.history"));
        if (manager.getAction("vcs.rollback") != null) navVcsTbActions.add(manager.getAction("vcs.rollback"));
        navVcsTbActions.add(ActionSeparator.getInstance());
        manager.registerGroup(navVcsTbActions);

        ActionGroup navBarVcsGroup = new ActionGroup("navbar.vcs.group", "NavBarVcsGroup", false);
        navBarVcsGroup.add(navVcsTbActions);
        manager.registerGroup(navBarVcsGroup);

        // Debug Header More Popup Actions
        if (manager.getAction("debug.force.step.over") == null) {
            manager.registerAction(AnAction.builder("debug.force.step.over", "Force Step Over")
                    .icon(FontAwesomeSolid.STEP_FORWARD, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Force Step Over"))
                    .build());
        }
        if (manager.getAction("debug.force.step.into") == null) {
            manager.registerAction(AnAction.builder("debug.force.step.into", "Force Step Into")
                    .icon(FontAwesomeSolid.ARROW_DOWN, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Force Step Into"))
                    .build());
        }
        if (manager.getAction("debug.smart.step.into") == null) {
            manager.registerAction(AnAction.builder("debug.smart.step.into", "Smart Step Into")
                    .icon(FontAwesomeSolid.ARROW_RIGHT, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Smart Step Into"))
                    .build());
        }
        if (manager.getAction("debug.show.point") == null) {
            manager.registerAction(AnAction.builder("debug.show.point", "Show Execution Point")
                    .icon(FontAwesomeSolid.BARS, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Show Execution Point"))
                    .build());
        }
        if (manager.getAction("debug.reset.frame") == null) {
            manager.registerAction(AnAction.builder("debug.reset.frame", "Reset Frame")
                    .icon(FontAwesomeSolid.UNDO, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Reset Frame"))
                    .build());
        }

        // Debug Header Toolbar Actions
        if (manager.getAction("debug.rerun") == null) {
            manager.registerAction(AnAction.builder("debug.rerun", "Rerun")
                    .onAction(ctx -> ctx.setStatus("Rerun"))
                    .build());
        }
        if (manager.getAction("debug.resume.program") == null) {
            manager.registerAction(AnAction.builder("debug.resume.program", "Resume Program")
                    .icon(FontAwesomeSolid.PLAY, "#57965c", 11)
                    .onAction(ctx -> ctx.setStatus("Resume Program"))
                    .build());
        }
        if (manager.getAction("debug.pause.program") == null) {
            manager.registerAction(AnAction.builder("debug.pause.program", "Pause Program")
                    .icon(FontAwesomeSolid.PAUSE, "#e0a44c", 11)
                    .onAction(ctx -> ctx.setStatus("Pause Program"))
                    .build());
        }
        if (manager.getAction("debug.step.over") == null) {
            manager.registerAction(AnAction.builder("debug.step.over", "Step Over")
                    .icon(FontAwesomeSolid.STEP_FORWARD, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Step Over"))
                    .build());
        }
        if (manager.getAction("debug.step.into") == null) {
            manager.registerAction(AnAction.builder("debug.step.into", "Step Into")
                    .icon(FontAwesomeSolid.ARROW_DOWN, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Step Into"))
                    .build());
        }
        if (manager.getAction("debug.step.out") == null) {
            manager.registerAction(AnAction.builder("debug.step.out", "Step Out")
                    .icon(FontAwesomeSolid.ARROW_UP, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Step Out"))
                    .build());
        }
        if (manager.getAction("debug.view.breakpoints") == null) {
            manager.registerAction(AnAction.builder("debug.view.breakpoints", "View Breakpoints\u2026")
                    .icon(FontAwesomeSolid.CIRCLE, "#e05555", 11)
                    .onAction(ctx -> ctx.setStatus("View Breakpoints\u2026"))
                    .build());
        }
        if (manager.getAction("debug.mute.breakpoints") == null) {
            manager.registerAction(AnAction.builder("debug.mute.breakpoints", "Mute Breakpoints")
                    .icon(FontAwesomeSolid.BAN, "#e05555", 11)
                    .onAction(ctx -> ctx.setStatus("Mute Breakpoints"))
                    .build());
        }

        // Debug Header Toolbar Groups
        ActionGroup resumeRef = new ActionGroup("debug.resume.ref", "Resume.Ref", false);
        if (manager.getAction("debug.resume.program") != null) resumeRef.add(manager.getAction("debug.resume.program"));
        manager.registerGroup(resumeRef);

        ActionGroup pauseRef = new ActionGroup("debug.pause.ref", "Pause.Ref", false);
        if (manager.getAction("debug.pause.program") != null) pauseRef.add(manager.getAction("debug.pause.program"));
        manager.registerGroup(pauseRef);

        ActionGroup stepOverRef = new ActionGroup("debug.stepover.ref", "StepOver.Ref", false);
        if (manager.getAction("debug.step.over") != null) stepOverRef.add(manager.getAction("debug.step.over"));
        if (manager.getAction("debug.step.into") != null) stepOverRef.add(manager.getAction("debug.step.into"));
        if (manager.getAction("debug.step.out") != null) stepOverRef.add(manager.getAction("debug.step.out"));
        stepOverRef.add(ActionSeparator.getInstance());
        if (manager.getAction("debug.view.breakpoints") != null) stepOverRef.add(manager.getAction("debug.view.breakpoints"));
        if (manager.getAction("debug.mute.breakpoints") != null) stepOverRef.add(manager.getAction("debug.mute.breakpoints"));
        manager.registerGroup(stepOverRef);

        // Debug Watches Toolbar Actions
        if (manager.getAction("watches.new") == null) {
            manager.registerAction(AnAction.builder("watches.new", "New Watch\u2026")
                    .icon(FontAwesomeSolid.PLUS, "#57965c", 11)
                    .onAction(ctx -> ctx.setStatus("New Watch\u2026"))
                    .build());
        }
        if (manager.getAction("watches.remove") == null) {
            manager.registerAction(AnAction.builder("watches.remove", "Remove Watch")
                    .icon(FontAwesomeSolid.MINUS, "#e05555", 11)
                    .onAction(ctx -> ctx.setStatus("Remove Watch"))
                    .build());
        }
        if (manager.getAction("watches.move.up") == null) {
            manager.registerAction(AnAction.builder("watches.move.up", "Move Watch Up")
                    .icon(FontAwesomeSolid.ARROW_UP, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Move Watch Up"))
                    .build());
        }
        if (manager.getAction("watches.move.down") == null) {
            manager.registerAction(AnAction.builder("watches.move.down", "Move Watch Down")
                    .icon(FontAwesomeSolid.ARROW_DOWN, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Move Watch Down"))
                    .build());
        }
        if (manager.getAction("watches.duplicate") == null) {
            manager.registerAction(AnAction.builder("watches.duplicate", "Duplicate Watch")
                    .icon(FontAwesomeSolid.COPY, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Duplicate Watch"))
                    .build());
        }

        // File History Toolbar Actions
        if (manager.getAction("vcs.history.refresh") == null) {
            manager.registerAction(AnAction.builder("vcs.history.refresh", "Refresh")
                    .icon(FontAwesomeSolid.SYNC, "#57965c", 11)
                    .onAction(ctx -> ctx.setStatus("Refresh"))
                    .build());
        }
        if (manager.getAction("vcs.history.diff") == null) {
            manager.registerAction(AnAction.builder("vcs.history.diff", "Show Diff")
                    .icon(FontAwesomeSolid.EXCHANGE_ALT, "#4a88c7", 11)
                    .onAction(ctx -> ctx.setStatus("Show Diff"))
                    .build());
        }
        if (manager.getAction("vcs.history.show.affected.files") == null) {
            manager.registerAction(AnAction.builder("vcs.history.show.affected.files", "Show All Affected Files")
                    .icon(FontAwesomeSolid.LIST, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Show All Affected Files"))
                    .build());
        }
        if (manager.getAction("vcs.history.commit.timestamp") == null) {
            manager.registerAction(AnAction.builder("vcs.history.commit.timestamp", "Commit Timestamp")
                    .onAction(ctx -> ctx.setStatus("Commit Timestamp"))
                    .build());
        }
        if (manager.getAction("vcs.history.columns") == null) {
            manager.registerAction(AnAction.builder("vcs.history.columns", "Columns")
                    .onAction(ctx -> ctx.setStatus("Columns"))
                    .build());
        }
        if (manager.getAction("vcs.history.show.details") == null) {
            manager.registerAction(AnAction.builder("vcs.history.show.details", "Show Details")
                    .onAction(ctx -> ctx.setStatus("Show Details"))
                    .build());
        }
        if (manager.getAction("vcs.history.show.diff.preview") == null) {
            manager.registerAction(AnAction.builder("vcs.history.show.diff.preview", "Show Diff Preview")
                    .onAction(ctx -> ctx.setStatus("Show Diff Preview"))
                    .build());
        }
        if (manager.getAction("vcs.history.diff.preview.bottom") == null) {
            manager.registerAction(AnAction.builder("vcs.history.diff.preview.bottom", "Bottom")
                    .onAction(ctx -> ctx.setStatus("Bottom"))
                    .build());
        }
        if (manager.getAction("vcs.history.diff.preview.right") == null) {
            manager.registerAction(AnAction.builder("vcs.history.diff.preview.right", "Right")
                    .onAction(ctx -> ctx.setStatus("Right"))
                    .build());
        }
        if (manager.getAction("git.hosting.open.in.browser.group") == null) {
            manager.registerAction(AnAction.builder("git.hosting.open.in.browser.group", "Git.Hosting.Open.In.Browser.Group")
                    .onAction(ctx -> ctx.setStatus("Git Hosting Open In Browser"))
                    .build());
        }
        if (manager.getAction("vcs.history.resume.indexing") == null) {
            manager.registerAction(AnAction.builder("vcs.history.resume.indexing", "Resume Indexing")
                    .icon(FontAwesomeSolid.PLAY, "#57965c", 11)
                    .onAction(ctx -> ctx.setStatus("Resume Indexing"))
                    .build());
        }

        // File History Toolbar Groups
        ActionGroup diffPreviewLoc = new ActionGroup("vcs.history.diff.preview.location", "Diff Preview Location", true);
        diffPreviewLoc.add(manager.getAction("vcs.history.diff.preview.bottom"));
        diffPreviewLoc.add(manager.getAction("vcs.history.diff.preview.right"));
        manager.registerGroup(diffPreviewLoc);

        ActionGroup configLayout = new ActionGroup("vcs.history.configure.layout", "Configure Layout", true);
        configLayout.add(ActionSeparator.getInstance());
        configLayout.add(manager.getAction("vcs.history.show.details"));
        configLayout.add(manager.getAction("vcs.history.show.diff.preview"));
        configLayout.add(diffPreviewLoc);
        manager.registerGroup(configLayout);

        ActionGroup viewOptions = new ActionGroup("vcs.history.view.options", "View Options", true);
        viewOptions.add(ActionSeparator.getInstance());
        viewOptions.add(manager.getAction("vcs.history.commit.timestamp"));
        viewOptions.add(manager.getAction("vcs.history.columns"));
        viewOptions.add(configLayout);
        viewOptions.add(ActionSeparator.getInstance());
        manager.registerGroup(viewOptions);

        ActionGroup vcsHistoryActionsToolbar = new ActionGroup("vcs.history.actions.group.toolbar", "VcsHistoryActionsGroup.Toolbar", false);
        vcsHistoryActionsToolbar.add(manager.getAction("git.hosting.open.in.browser.group"));
        vcsHistoryActionsToolbar.add(manager.getAction("vcs.history.resume.indexing"));
        manager.registerGroup(vcsHistoryActionsToolbar);
    }

    private static void registerFloatingCodeAndQuickActionsAndSqlToolbarActions(ActionManager manager) {
        // Floating Code Toolbar Actions
        if (manager.getAction("code.surround.try.catch") == null) {
            manager.registerAction(AnAction.builder("code.surround.try.catch", "try / catch")
                    .onAction(ctx -> ctx.setStatus("try / catch"))
                    .build());
        }
        if (manager.getAction("code.surround.try.catch.finally") == null) {
            manager.registerAction(AnAction.builder("code.surround.try.catch.finally", "try / catch / finally")
                    .onAction(ctx -> ctx.setStatus("try / catch / finally"))
                    .build());
        }
        if (manager.getAction("code.surround.if") == null) {
            manager.registerAction(AnAction.builder("code.surround.if", "if")
                    .onAction(ctx -> ctx.setStatus("if"))
                    .build());
        }

        // Floating Code Groups
        ActionGroup extractGroup = new ActionGroup("code.extract.group", "Extract", false);
        if (manager.getAction("refactor.extract.method") != null) extractGroup.add(manager.getAction("refactor.extract.method"));
        if (manager.getAction("refactor.introduce.variable") != null) extractGroup.add(manager.getAction("refactor.introduce.variable"));
        if (manager.getAction("refactor.introduce.constant") != null) extractGroup.add(manager.getAction("refactor.introduce.constant"));
        if (manager.getAction("refactor.introduce.parameter") != null) extractGroup.add(manager.getAction("refactor.introduce.parameter"));
        manager.registerGroup(extractGroup);

        ActionGroup surroundGroup = new ActionGroup("code.surround.group", "Surround", false);
        surroundGroup.add(manager.getAction("code.surround.try.catch"));
        surroundGroup.add(manager.getAction("code.surround.try.catch.finally"));
        surroundGroup.add(manager.getAction("code.surround.if"));
        manager.registerGroup(surroundGroup);

        ActionGroup xdbgCodeToolbar = new ActionGroup("xdebugger.code.toolbar", "XDebugger.Code.Toolbar", false);
        xdbgCodeToolbar.add(ActionSeparator.getInstance());
        if (manager.getAction("debug.evaluate.expression") != null) xdbgCodeToolbar.add(manager.getAction("debug.evaluate.expression"));
        if (manager.getAction("debug.add.to.watches") != null) xdbgCodeToolbar.add(manager.getAction("debug.add.to.watches"));
        manager.registerGroup(xdbgCodeToolbar);

        // Markdown Editor Floating Toolbar Actions
        if (manager.getAction("md.set.header.style") == null) {
            manager.registerAction(AnAction.builder("md.set.header.style", "Set Header Style")
                    .onAction(ctx -> ctx.setStatus("Set Header Style"))
                    .build());
        }
        if (manager.getAction("md.bold") == null) {
            manager.registerAction(AnAction.builder("md.bold", "Bold")
                    .icon(FontAwesomeSolid.BOLD, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Bold"))
                    .build());
        }
        if (manager.getAction("md.italic") == null) {
            manager.registerAction(AnAction.builder("md.italic", "Italic")
                    .icon(FontAwesomeSolid.ITALIC, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Italic"))
                    .build());
        }
        if (manager.getAction("md.strikethrough") == null) {
            manager.registerAction(AnAction.builder("md.strikethrough", "Strikethrough")
                    .icon(FontAwesomeSolid.STRIKETHROUGH, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Strikethrough"))
                    .build());
        }
        if (manager.getAction("md.code") == null) {
            manager.registerAction(AnAction.builder("md.code", "<> Code")
                    .icon(FontAwesomeSolid.CODE, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Code"))
                    .build());
        }
        if (manager.getAction("md.create.link") == null) {
            manager.registerAction(AnAction.builder("md.create.link", "Create Link")
                    .icon(FontAwesomeSolid.LINK, "#3592c4", 11)
                    .onAction(ctx -> ctx.setStatus("Create Link"))
                    .build());
        }
        if (manager.getAction("md.create.or.change.list") == null) {
            manager.registerAction(AnAction.builder("md.create.or.change.list", "Create Or Change List")
                    .icon(FontAwesomeSolid.LIST, "#a9b7c6", 11)
                    .onAction(ctx -> ctx.setStatus("Create Or Change List"))
                    .build());
        }

        // Quick Actions Popup Toolbar Actions
        if (manager.getAction("quick.actions.load.full.cell") == null) {
            manager.registerAction(AnAction.builder("quick.actions.load.full.cell", "Load Full Cell")
                    .icon(FontAwesomeSolid.ARROWS_ALT_V, "#3592c4", 11)
                    .onAction(ctx -> ctx.setStatus("Load Full Cell"))
                    .build());
        }
        if (manager.getAction("quick.actions.related.rows") == null) {
            manager.registerAction(AnAction.builder("quick.actions.related.rows", "Related Rows")
                    .icon(FontAwesomeSolid.TABLE, "#4a88c7", 11)
                    .onAction(ctx -> ctx.setStatus("Related Rows"))
                    .build());
        }
        if (manager.getAction("quick.actions.open.url") == null) {
            manager.registerAction(AnAction.builder("quick.actions.open.url", "Open URL")
                    .icon(FontAwesomeSolid.GLOBE, "#57965c", 11)
                    .onAction(ctx -> ctx.setStatus("Open URL"))
                    .build());
        }
        if (manager.getAction("quick.actions.open.file.uri") == null) {
            manager.registerAction(AnAction.builder("quick.actions.open.file.uri", "Open File URI")
                    .icon(FontAwesomeSolid.FOLDER, "#e0a44c", 11)
                    .onAction(ctx -> ctx.setStatus("Open File URI"))
                    .build());
        }

        // Run Tool Window Header Toolbar Actions
        if (manager.getAction("run.header.rerun") == null) {
            manager.registerAction(AnAction.builder("run.header.rerun", "Rerun")
                    .onAction(ctx -> ctx.setStatus("Rerun"))
                    .build());
        }

        // SQL Floating Toolbar Actions and Groups
        if (manager.getAction("editor.explain.plan") == null) {
            manager.registerAction(AnAction.builder("editor.explain.plan", "Explain Plan")
                    .icon(FontAwesomeSolid.PROJECT_DIAGRAM, "#4a88c7", 11)
                    .onAction(ctx -> ctx.setStatus("Explain Plan"))
                    .build());
        }

        ActionGroup sqlFloatingCodeToolbar = new ActionGroup("sql.floating.code.toolbar", "Floating Code Toolbar", false);
        sqlFloatingCodeToolbar.add(extractGroup);
        sqlFloatingCodeToolbar.add(surroundGroup);
        if (manager.getAction("code.comment.line") != null) sqlFloatingCodeToolbar.add(manager.getAction("code.comment.line"));
        if (manager.getAction("code.reformat") != null) sqlFloatingCodeToolbar.add(manager.getAction("code.reformat"));
        sqlFloatingCodeToolbar.add(xdbgCodeToolbar);
        if (manager.getAction("middle.run") != null) sqlFloatingCodeToolbar.add(manager.getAction("middle.run"));
        sqlFloatingCodeToolbar.add(manager.getAction("editor.explain.plan"));
        manager.registerGroup(sqlFloatingCodeToolbar);
    }

    public static List<ActionGroup> getMainMenuBarGroups(ActionManager manager) {
        return java.util.stream.Stream.of(
                manager.getGroup("menu.file"),
                manager.getGroup("menu.edit"),
                manager.getGroup("menu.view"),
                manager.getGroup("menu.navigate"),
                manager.getGroup("menu.code"),
                manager.getGroup("menu.refactor"),
                manager.getGroup("menu.run"),
                manager.getGroup("menu.tools"),
                manager.getGroup("menu.vcs"),
                manager.getGroup("menu.window"),
                manager.getGroup("menu.help")
        ).filter(java.util.Objects::nonNull).toList();
    }
}

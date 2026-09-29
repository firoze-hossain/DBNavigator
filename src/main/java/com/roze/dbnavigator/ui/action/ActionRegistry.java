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

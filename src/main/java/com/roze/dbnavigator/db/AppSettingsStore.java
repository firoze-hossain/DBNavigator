package com.roze.dbnavigator.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * App-wide preferences (theme, editor font, etc.), persisted to
 * ~/.dbnavigator/settings.json — separate from per-connection state.
 */
public final class AppSettingsStore {

    public enum Theme { DARK, LIGHT }

    /** Plain data holder — Jackson needs a no-arg constructor and public fields/getters+setters. */
    public static class Settings {
        public Theme theme = Theme.DARK;
        public String editorFontFamily = "JetBrains Mono";
        public double editorFontSize = 14;
        public boolean ctrlScrollZoomEnabled = true;
        public boolean autoUpdateEnabled = true;
        public boolean autoDownloadUpdates = false;
        public String updateChannel = "Stable";
        public String updateEndpoint = "";
        public int queryTimeoutSeconds = 30;
        public int maxResultRows = 500;
        public boolean autoCommit = true;
        public int pageSize = 100;
        public boolean showEmptySchemas = false;
        public String csvDelimiter = ",";
        public String csvQuoteChar = "\"";
        public String keymapPreset = "DataGrip Default";
        public List<ExecuteActionConfig> executeActions = defaultExecuteActions();
        public String scriptSplitting = "Into valid ANSI SQL statements or by separator";
        public boolean reviewParametersBeforeExecution = true;
        public boolean warnUnsafeQueries = true;

        // Output and Results (Images 1 & 2)
        public boolean showTimestampForQueryOutput = false;
        public boolean enableDbmsOutput = false;
        public boolean showResultsInEditor = false;
        public boolean createTitleFromComment = true;
        public String titleAfterCommentText = "";
        public String showServicesOutput = "For all output";
        public boolean focusServicesInWindowMode = false;
        public boolean openNewServicesTabForSessions = false;
        public boolean activateServicesForSelectedFileOnly = false;

        // User Parameters (Images 3 & 4)
        public boolean enableUserParameters = true;
        public boolean enableUserParametersInLiteralsWithInjection = true;
        public boolean substituteInsideSqlStrings = false;
        public List<UserParameterPattern> userParameterPatterns = defaultUserParameterPatterns();

        // CSV Formats (DataGrip Alignment)
        public List<CsvFormatConfig> csvFormats = defaultCsvFormats();
        public String defaultCsvFormat = "CSV";

        // Data Editor and Viewer (DataGrip Alignment)
        // 1. General / Fetch Limits
        public boolean limitPageSize = true;
        public int resultSetPrefetchSize = 2000;
        public int filterHistorySize = 10;
        public int maxBytesLoadedPerValue = 204800;
        public boolean showFirstDataRowsInPreview = true;
        public int previewDataRows = 10;

        // 2. Controls Customization
        public boolean enablePagingInEditorResults = false;
        public String gridPaginationPosition = "Grid bottom (floating)";
        public boolean showQuickActionsToolbar = true;
        public boolean enableQuickActionsCustomization = true;

        // 3. Data Presentation
        public boolean useCustomFont = false;
        public String customFontFamily = "JetBrains Mono";
        public double customFontSize = 13.0;
        public double customLineHeight = 1.2;
        public boolean alternateRowColors = false;
        public String showBooleanValuesAs = "Text";
        public String automaticallyTransposeTables = "Never";
        public boolean detectBinaryAsText = true;
        public boolean detectBinaryAsUuid = true;
        public boolean enableLocalFilterByDefault = true;
        public boolean enableImmediateCompletionInGridTextCells = true;
        public String displayTemporalDataInTimeZone = "";

        // 4. Custom Number Formats
        public String decimalSeparator = ".";
        public boolean enableGroupingSeparator = false;
        public String groupingSeparator = "";
        public String infinityText = "Infinity";
        public String nanText = "NaN";
        public boolean enableNumberPattern = false;
        public String numberPattern = "";

        // 5. Custom Date/Time Formats
        public boolean enableDatetimeTimestamp = false;
        public String datetimeTimestampPattern = "yyyy-MM-dd HH:mm:ss";
        public boolean enableDatetimeTimestampWithZone = false;
        public String datetimeTimestampWithZonePattern = "yyyy-MM-dd HH:mm:ss Z";
        public boolean enableTime = false;
        public String timePattern = "HH:mm:ss";
        public boolean enableTimeWithZone = false;
        public String timeWithZonePattern = "HH:mm:ss Z";
        public boolean enableDate = false;
        public String datePattern = "yyyy-MM-dd";

        // 6. Data Sorting
        public boolean sortViaOrderBy = true;
        public boolean sortTablesByNumericPk = false;
        public String sortTablesByNumericPkDirection = "Ascending";
        public String addColumnsToSorting = "⌥Click";

        // 7. Data Modification
        public boolean submitChangesImmediately = false;
        public boolean enableEditingForQueriesWithJoin = true;
        public boolean showDmlPreviewForQueriesWithJoin = true;

        // 8. URL Click Settings
        public boolean allowOpenSecureLinks = false;
        public boolean allowOpenStandardLinks = false;
        public boolean allowOpenLocalFileLinks = false;
        public boolean assumeHttpIfNoProtocol = false;

        // Database Explorer Settings (DataGrip Alignment)
        public boolean rememberFilterState = true;
        public boolean showDatabaseColors = true;
        public boolean colorDatabaseExplorer = true;
        public boolean colorEditorTabHeaders = true;
        public boolean colorEditorBackgrounds = false;
        public boolean colorEditorToolbars = true;

        // AI Tools Settings (DataGrip Alignment)
        public boolean aiReadDatabaseSchemas = false;
        public boolean aiModifyDatabaseSchemas = false;
        public boolean aiReadDatabaseData = false;
        public boolean aiModifyDatabaseData = false;

        // Query Files and Consoles Settings (DataGrip Alignment)
        public boolean showDataSourceNameInFileTree = true;
        public boolean useAttachedSearchPathColorInFileTree = true;
        public boolean useAttachedDataSourceIconForQueryFiles = true;
        public String defaultConsoleFileName = "console";
        public String editorTabTitleTemplate = "$NAME$ [$DATASOURCE$]";
        public boolean useTemplateForQueryFiles = true;

        // SQL Dialects Settings (DataGrip Alignment)
        public String globalSqlDialect = "<None>";
        public String projectSqlDialect = "<None>";
        public List<SqlDialectMappingConfig> sqlDialectMappings = new ArrayList<>();

        // SQL Resolution Scopes Settings (DataGrip Alignment)
        public String projectResolutionScope = "<Default> (<Everything>)";
        public List<SqlResolutionScopeMappingConfig> sqlResolutionScopeMappings = new ArrayList<>();

        // Other Settings (DataGrip Alignment)
        public boolean confirmCancellationForModifySchemaDialogs = true;
        public boolean showPreviewOfValidScriptWhenUpdatingSource = true;
        public boolean suggestDumpingDdlForNewMappings = true;
        public String generateContextTemplates = "Append to existing console";
        public List<VirtualForeignKeyRule> virtualForeignKeys = defaultVirtualForeignKeys();
        public String defaultResolveModeForConsoles = "Playground";
        public String statementDelimiter = "";

        // Appearance: Theme & Colors (DataGrip Alignment)
        public String uiTheme = "Islands Dark";
        public boolean syncThemeWithOs = false;
        public String editorColorScheme = "Islands Dark Theme default";
        public boolean differentToolWindowBackground = false;

        // Appearance: Accessibility (DataGrip Alignment)
        public String ideZoom = "100%";
        public boolean useCustomIdeFont = false;
        public String customIdeFontFamily = "Inter";
        public int customIdeFontSize = 13;
        public boolean supportScreenReaders = false;
        public boolean useContrastScrollbars = false;
        public boolean adjustColorsForVisionDeficiency = false;

        // Appearance: UI Options (DataGrip Alignment)
        public boolean compactMode = false;
        public boolean alwaysShowFullPathInWindowHeader = false;
        public boolean useProjectColorsInMainToolbar = true;
        public boolean keepPopupsOpenForToggleItems = true;
        public boolean dragAndDropWithAltPressedOnly = false;
        public boolean smoothScrolling = true;
        public boolean enableMnemonicsInControls = true;
        public boolean enableMnemonicsInMenu = true;
        public boolean displayIconsInMenuItems = true;
        public String mainMenuPresentation = "Hide under Hamburger Button";

        // Appearance: Background Image (DataGrip Alignment)
        public String backgroundImagePath = "";
        public int backgroundImageOpacity = 15;
        public String backgroundImagePlacement = "Fill";
        public boolean backgroundImageThisProjectOnly = false;
        public String backgroundImageTarget = "Editor and Tools";

        // Appearance: Tree Views (DataGrip Alignment)
        public boolean showIndentGuides = false;
        public boolean useSmallerIndents = false;

        // Appearance: Tool Windows (DataGrip Alignment)
        public boolean showToolWindowBars = false;
        public boolean showToolWindowNames = false;
        public boolean sideBySideLayoutOnLeft = false;
        public boolean sideBySideLayoutOnRight = false;
        public boolean widescreenToolWindowLayout = false;
        public boolean rememberSizeForEachToolWindow = false;

        // Appearance: Presentation Mode (DataGrip Alignment)
        public String presentationModeZoom = "175%";

        // Appearance: Antialiasing (DataGrip Alignment)
        public String ideAntialiasing = "Subpixel";
        public String editorAntialiasing = "Subpixel";

        // Editor > General > Auto Import (DataGrip Alignment)
        public boolean showXmlAutoImportTooltip = true;

        // Editor > General > Breadcrumbs (DataGrip Alignment)
        public boolean showBreadcrumbs = true;
        public String breadcrumbsPlacement = "Bottom";
        public boolean breadcrumbsHtml = false;
        public boolean breadcrumbsMarkdown = false;
        public boolean breadcrumbsXhtml = false;
        public boolean breadcrumbsJson = false;
        public boolean breadcrumbsSql = false;
        public boolean breadcrumbsXml = false;

        // Editor > General > Appearance (DataGrip Alignment)
        public boolean caretBlinking = true;
        public int caretBlinkingMs = 500;
        public boolean useBlockCaret = false;
        public boolean useFullLineHeightCaret = false;
        public boolean highlightOccurrences = true;
        public boolean showHardWrapAndVisualGuides = true;
        public boolean showLineNumbers = true;
        public String lineNumbersMode = "Absolute";
        public boolean showLinesBetweenStatements = false;
        public boolean showWhitespaces = false;
        public boolean showWhitespacesLeading = true;
        public boolean showWhitespacesInner = true;
        public boolean showWhitespacesTrailing = true;
        public boolean showWhitespacesSelection = true;
        public boolean showEditorIndentGuides = true;
        public boolean showIntentionBulb = true;
        public boolean showPreviewForIntentionActions = true;
        public boolean renderDocComments = false;
        public boolean showCodeLensOnScrollbarHover = true;
        public boolean useEditorFontForInlayHints = false;
        public boolean enableTagTreeHighlighting = true;
        public int tagTreeLevelsToHighlight = 6;
        public double tagTreeOpacity = 0.1;

        // Editor > General > Code Completion (DataGrip Alignment)
        public boolean matchCase = true;
        public String matchCaseMode = "First letter only";
        public boolean sortSuggestionsAlphabetically = false;
        public boolean showSuggestionsAsYouType = true;
        public boolean insertSelectedSuggestionByContextKeys = false;
        public boolean showDocPopup = false;
        public int docPopupDelayMs = 500;
        public boolean insertParenthesesAutomatically = true;
        public boolean mlSortSuggestions = true;
        public boolean mlSortSql = true;
        public boolean mlMarkPositionChanges = true;
        public boolean mlMarkMostRelevant = true;
        public boolean htmlAutoPopupTagCompletion = true;
        public boolean showParameterInfoPopup = true;
        public int parameterInfoDelayMs = 1000;
        public boolean showFullMethodSignatures = false;
        public String sqlSuggestObjectsFrom = "The current scope";
        public String qualifyWithDatabase = "Always";
        public String qualifyWithSchema = "Always";
        public String qualifyWithTableView = "Always";
        public String qualifyWithTableAlias = "Always";
        public String qualifyInBasicCompletion = "On collisions";
        public String qualifyInJoinCompletion = "Always";
        public String qualifyInRefactoring = "On collisions";
        public String qualifyInLiveTemplates = "On collisions";
        public String qualifyInDragDrop = "On collisions";
        public boolean joinUseAliases = true;
        public boolean joinInvertOperands = false;
        public boolean joinSuggestNonStrictFk = true;
        public boolean tableAliasesAutoAdd = false;
        public boolean tableAliasesSuggest = true;
        public List<TableAliasConfig> customTableAliases = new ArrayList<>();
        public String additionalAcceptCharacters = "";

        // Editor > General > Code Folding (DataGrip Alignment)
        public boolean showCodeFoldingArrows = true;
        public String showCodeFoldingArrowsMode = "On mouse hover";
        public boolean showBottomArrows = false;
        public boolean foldFileHeader = true;
        public boolean foldImports = true;
        public boolean foldDocComments = false;
        public boolean foldMethodBodies = false;
        public boolean foldCustomRegions = false;
        public boolean foldMarkdownFrontMatter = true;
        public boolean foldMarkdownLinks = true;
        public boolean foldMarkdownTables = false;
        public boolean foldMarkdownCodeFences = false;
        public boolean foldMarkdownTableOfContents = true;
        public boolean foldSqlUnderscoresInNumericLiterals = false;
        public boolean foldXmlTags = false;
        public boolean foldHtmlStyleAttribute = true;
        public boolean foldXmlEntities = true;
        public boolean foldDataUris = true;

        // Editor > General > Editor Tabs (DataGrip Alignment)
        public String editorTabPlacement = "Top";
        public String editorTabsShowMode = "One row";
        public String editorTabsOverflowMode = "Scroll the tabs panel";
        public boolean showPinnedTabsInSeparateRow = false;
        public boolean editorTabsShowFileIcon = true;
        public boolean editorTabsShowFileExtension = true;
        public boolean editorTabsShowDirectoryForNonUnique = true;
        public boolean editorTabsMarkModified = false;
        public boolean editorTabsShowFullPathOnHover = true;
        public String editorTabsCloseButtonPosition = "Right";
        public boolean editorTabsSortAlphabetically = false;
        public boolean editorTabsOpenNewAtEnd = false;
        public boolean editorTabsEnablePreviewTab = false;
        public int editorTabsLimit = 30;
        public String editorTabsExceedLimitPolicy = "Close unused";
        public String editorTabsCloseActivatePolicy = "The tab on the left";
        public boolean editorTabsAlwaysShowQualifiedNames = false;
        public boolean editorTabsShortenNames = true;

        // Editor > General > Output Console (DataGrip Alignment)
        public boolean outputConsoleUseSoftWraps = false;
        public int outputConsoleHistorySize = 300;
        public boolean outputConsoleOverrideCycleBuffer = false;
        public int outputConsoleCycleBufferSizeKb = 1024;
        public String outputConsoleDefaultEncoding = "<System Default: UTF-8>";
        public List<String> outputConsoleFoldingPatterns = new ArrayList<>();
        public List<String> outputConsoleFoldingExceptions = new ArrayList<>();

        // Editor > General > Gutter Icons (DataGrip Alignment)
        public boolean showGutterIcons = true;
        public boolean gutterColorPreview = true;
        public boolean gutterDocComments = true;
        public boolean gutterRunLineMarker = true;
        public boolean gutterRecursiveCall = true;
        public boolean gutterVcsIgnoredDirectories = true;
        public boolean gutterConfigureHtmlImage = true;
        public boolean gutterConfigureMarkdownImage = true;
        public boolean gutterInstallPlantUml = true;

        // Editor > General > Inline Completion (DataGrip Alignment)
        public boolean inlineCompletionEnabled = true;
        public boolean inlineAutoOnTyping = true;
        public boolean inlineMultilineSuggestions = true;
        public boolean inlineSyncWithPopup = false;

        // Editor > General > Smart Keys (DataGrip Alignment)
        public boolean smartKeysHomeMovesCaret = true;
        public boolean smartKeysEndBlankLineMovesCaret = true;
        public boolean smartKeysInsertPairedBrackets = true;
        public boolean smartKeysInsertPairQuote = true;
        public boolean smartKeysReformatBlockOnBrace = true;
        public boolean smartKeysUseCamelHumps = false;
        public boolean smartKeysHonorCamelHumpsOnDoubleClick = true;
        public boolean smartKeysSurroundSelectionOnQuoteOrBrace = true;
        public boolean smartKeysMultiCaretsOnDoubleModifier = true;
        public boolean smartKeysJumpOutsideBracketWithTab = true;
        public boolean smartKeysEnterSmartIndent = true;
        public boolean smartKeysEnterInsertPairBrace = true;
        public boolean smartKeysEnterCloseBlockComment = true;
        public String smartKeysUnindentOnBackspace = "To proper indent position";
        public String smartKeysReformatOnPaste = "None";
        public boolean smartKeysReformatRemoveCustomLineBreaks = false;

        // Editor > General > Smart Keys > SQL (DataGrip Alignment)
        public boolean smartKeysSqlInsertStringConcatOnEnter = true;
        public boolean smartKeysSqlCloseCodeBlocksOnEnter = true;

        // Editor > General > Smart Keys > Markdown (DataGrip Alignment)
        public boolean smartKeysMarkdownReformatTable = true;
        public boolean smartKeysMarkdownInsertHtmlLineBreakInTable = true;
        public boolean smartKeysMarkdownShiftEnterNewTableRow = true;
        public boolean smartKeysMarkdownTabNavigateTable = true;
        public boolean smartKeysMarkdownAdjustListIndent = true;
        public boolean smartKeysMarkdownSmartEnterBackspace = true;
        public boolean smartKeysMarkdownRenumberList = false;
        public String smartKeysMarkdownListNumerating = "Sequentially";
        public boolean smartKeysMarkdownInsertLinksOnDrop = true;

        // Editor > General > Smart Keys > JSON (DataGrip Alignment)
        public boolean smartKeysJsonInsertMissingCommaOnEnter = true;
        public boolean smartKeysJsonInsertMissingCommaAfterMatching = true;
        public boolean smartKeysJsonManageCommasOnPaste = true;
        public boolean smartKeysJsonEscapeTextOnPaste = true;
        public boolean smartKeysJsonAddQuotesToPropertyNames = true;
        public boolean smartKeysJsonAddWhitespaceAfterColon = true;
        public boolean smartKeysJsonMoveColonAfterPropertyName = false;
        public boolean smartKeysJsonMoveCommaAfterPropertyValue = false;

        // Editor > General > Smart Keys > HTML/CSS (DataGrip Alignment)
        public boolean smartKeysHtmlInsertClosingTag = true;
        public boolean smartKeysHtmlInsertRequiredAttributes = true;
        public boolean smartKeysHtmlInsertRequiredSubtags = true;
        public boolean smartKeysHtmlStartAttribute = true;
        public boolean smartKeysHtmlAddQuotesForAttribute = true;
        public boolean smartKeysHtmlAutoCloseTag = true;
        public boolean smartKeysHtmlSimultaneousTagEditing = true;
        public boolean smartKeysCssSelectWholeCssIdentifiers = true;

        // Editor > General > Postfix Completion (DataGrip Alignment)
        public boolean postfixCompletionEnabled = true;
        public String postfixCompletionExpandWith = "Tab";
        public List<PostfixTemplateConfig> postfixTemplates = defaultPostfixTemplates();

        public static List<String> defaultUiThemes() {
            return List.of(
                    "Islands Dark",
                    "Islands Light",
                    "Islands Darcula",
                    "High Contrast",
                    "Dark",
                    "Light",
                    "Light with Light Header",
                    "Darcula",
                    "Get More Themes..."
            );
        }

        public static List<String> defaultEditorColorSchemes() {
            return List.of(
                    "Islands Dark Theme default",
                    "Darcula",
                    "High Contrast",
                    "Light",
                    "Classic Light"
            );
        }

        public static List<String> defaultMainMenuOptions() {
            return List.of(
                    "Hide under Hamburger Button",
                    "Merge with Main Toolbar",
                    "Show above Main Toolbar"
            );
        }

        public static List<String> defaultIdeZooms() {
            return List.of("100%", "110%", "125%", "150%", "175%", "200%", "250%", "300%");
        }

        public static List<String> defaultPresentationModeZooms() {
            return List.of("100%", "125%", "150%", "175%", "200%", "225%", "250%", "300%");
        }

        public static List<String> defaultAntialiasingOptions() {
            return List.of("Subpixel", "Greyscale", "No antialiasing");
        }

        public static List<String> defaultSmartKeysUnindentOptions() {
            return List.of("Disabled", "To proper indent position", "To nearest indent position");
        }

        public static List<String> defaultSmartKeysReformatOnPasteOptions() {
            return List.of("None", "Indent block", "Indent each line", "Reformat block");
        }

        public static List<String> defaultMarkdownListNumeratingOptions() {
            return List.of("Sequentially", "With '1.'", "With previous number");
        }

        public static List<String> defaultPostfixExpandWithOptions() {
            return List.of("Tab", "Space", "Enter");
        }

        public static List<PostfixTemplateConfig> defaultPostfixTemplates() {
            List<PostfixTemplateConfig> list = new ArrayList<>();
            list.add(new PostfixTemplateConfig("afrom", "SQL", "select $COLUMNS$ from $EXPR$", "select [c1 as a1, ...] from authors", true, "authors.afrom", "select [c1 as a1, ...] from authors"));
            list.add(new PostfixTemplateConfig("cast", "SQL", "CAST($EXPR$ as JSON)", "CAST('foo' as JSON)", true, "'foo'.cast", "CAST('foo' as JSON)"));
            list.add(new PostfixTemplateConfig("cfrom", "SQL", "select * from $EXPR$", "select [all columns] from authors", true, "authors.cfrom", "select [all columns] from authors"));
            list.add(new PostfixTemplateConfig("from", "SQL", "select * from $EXPR$", "select | from authors", true, "authors.from", "select | from authors"));
            list.add(new PostfixTemplateConfig("join", "SQL", "select * from $EXPR$ join | on |", "select * from authors join | on |", true, "authors.join", "select * from authors join | on |"));
            return list;
        }

        public static List<UserParameterPattern> defaultUserParameterPatterns() {
            List<UserParameterPattern> list = new ArrayList<>();
            list.add(new UserParameterPattern("\"#name#\"", "everywhere", "XML", true, false));
            list.add(new UserParameterPattern("\"$a.b.c$?\"", "everywhere", "All excl. SQL", true, false));
            list.add(new UserParameterPattern("\"#a.b.c#?\"", "everywhere", "All excl. SQL", true, false));
            list.add(new UserParameterPattern("\"%(name)s\"", "everywhere", "Python", true, false));
            list.add(new UserParameterPattern("\"%name\"", "everywhere", "JAVA, PHP, Python", true, false));
            list.add(new UserParameterPattern("\":'name'\"", "everywhere", "PostgreSQL", true, false));
            list.add(new UserParameterPattern("\"${name}\"", "everywhere", "All languages", true, false));
            list.add(new UserParameterPattern("\"$name\"", "everywhere", "All languages", true, false));
            list.add(new UserParameterPattern("\":name\"", "everywhere", "All languages", true, false));
            return list;
        }

        private static List<ExecuteActionConfig> defaultExecuteActions() {
            List<ExecuteActionConfig> list = new ArrayList<>();
            list.add(new ExecuteActionConfig("Execute", "Ctrl+Enter", "Ask what to execute", "Nothing", "Exactly as separate statements", false));
            list.add(new ExecuteActionConfig("Execute (2)", "Ctrl+Shift+Enter", "Smallest statement", "Nothing", "Exactly as separate statements", false));
            list.add(new ExecuteActionConfig("Execute (3)", "Ctrl+Alt+Enter", "Whole script", "Nothing", "Exactly as separate statements", false));
            return list;
        }

        public static List<CsvFormatConfig> defaultCsvFormats() {
            List<CsvFormatConfig> list = new ArrayList<>();
            list.add(new CsvFormatConfig("CSV", "Comma", "Newline", "Empty string",
                    CsvFormatConfig.defaultQuotationRules(), "When needed", false, false, false));
            list.add(new CsvFormatConfig("TSV", "Tab", "Newline", "Empty string",
                    CsvFormatConfig.defaultQuotationRules(), "When needed", false, false, false));
            list.add(new CsvFormatConfig("Pipe-separated", "Pipe", "Newline", "Empty string",
                    CsvFormatConfig.defaultQuotationRules(), "When needed", false, false, false));
            list.add(new CsvFormatConfig("Semicolon-separated", "Semicolon", "Newline", "Empty string",
                    CsvFormatConfig.defaultQuotationRules(), "When needed", false, false, false));
            return list;
        }

        public static List<VirtualForeignKeyRule> defaultVirtualForeignKeys() {
            List<VirtualForeignKeyRule> list = new ArrayList<>();
            list.add(new VirtualForeignKeyRule("(.*)_(?i)id", "$1\\.(?i)id"));
            return list;
        }

        public static List<String> defaultDialectList() {
            return List.of(
                    "<None>",
                    "Amazon DynamoDB",
                    "Amazon Redshift",
                    "Apache Cassandra",
                    "Apache Derby",
                    "Apache Hive",
                    "Apache Spark",
                    "Azure SQL Database",
                    "ClickHouse",
                    "CockroachDB",
                    "Couchbase Server",
                    "Databricks",
                    "Exasol",
                    "Generic SQL",
                    "Google BigQuery",
                    "Greenplum",
                    "H2",
                    "HSQLDB",
                    "IBM Db2 iSeries",
                    "IBM Db2 LUW",
                    "IBM Db2 z/OS",
                    "MariaDB",
                    "Microsoft SQL Server",
                    "MongoDB",
                    "MySQL",
                    "Oracle",
                    "Oracle NetSuite",
                    "Oracle SQL*Plus",
                    "PostgreSQL",
                    "Redis",
                    "Snowflake"
            );
        }

        public Theme getTheme() { return theme; }
        public void setTheme(Theme theme) { this.theme = theme; }
        public String getEditorFontFamily() { return editorFontFamily; }
        public void setEditorFontFamily(String editorFontFamily) { this.editorFontFamily = editorFontFamily; }
        public double getEditorFontSize() { return editorFontSize; }
        public void setEditorFontSize(double editorFontSize) { this.editorFontSize = editorFontSize; }
        public boolean isCtrlScrollZoomEnabled() { return ctrlScrollZoomEnabled; }
        public void setCtrlScrollZoomEnabled(boolean ctrlScrollZoomEnabled) {
            this.ctrlScrollZoomEnabled = ctrlScrollZoomEnabled;
        }
        public boolean isAutoUpdateEnabled() { return autoUpdateEnabled; }
        public void setAutoUpdateEnabled(boolean value) { this.autoUpdateEnabled = value; }
        public boolean isAutoDownloadUpdates() { return autoDownloadUpdates; }
        public void setAutoDownloadUpdates(boolean value) { this.autoDownloadUpdates = value; }
        public String getUpdateChannel() { return updateChannel; }
        public void setUpdateChannel(String value) { this.updateChannel = value; }
        public String getUpdateEndpoint() { return updateEndpoint; }
        public void setUpdateEndpoint(String value) { this.updateEndpoint = value; }
        public int getQueryTimeoutSeconds() { return queryTimeoutSeconds; }
        public void setQueryTimeoutSeconds(int queryTimeoutSeconds) { this.queryTimeoutSeconds = queryTimeoutSeconds; }
        public int getMaxResultRows() { return maxResultRows; }
        public void setMaxResultRows(int maxResultRows) { this.maxResultRows = maxResultRows; }
        public boolean isAutoCommit() { return autoCommit; }
        public void setAutoCommit(boolean autoCommit) { this.autoCommit = autoCommit; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public boolean isShowEmptySchemas() { return showEmptySchemas; }
        public void setShowEmptySchemas(boolean showEmptySchemas) { this.showEmptySchemas = showEmptySchemas; }
        public String getCsvDelimiter() { return csvDelimiter; }
        public void setCsvDelimiter(String csvDelimiter) { this.csvDelimiter = csvDelimiter; }
        public String getCsvQuoteChar() { return csvQuoteChar; }
        public void setCsvQuoteChar(String csvQuoteChar) { this.csvQuoteChar = csvQuoteChar; }
        public String getKeymapPreset() { return keymapPreset; }
        public void setKeymapPreset(String keymapPreset) { this.keymapPreset = keymapPreset; }
        public List<ExecuteActionConfig> getExecuteActions() {
            if (executeActions == null || executeActions.isEmpty()) {
                executeActions = defaultExecuteActions();
            }
            return executeActions;
        }
        public void setExecuteActions(List<ExecuteActionConfig> executeActions) {
            this.executeActions = (executeActions == null || executeActions.isEmpty())
                    ? defaultExecuteActions() : executeActions;
        }
        public ExecuteActionConfig getExecuteAction(int index) {
            List<ExecuteActionConfig> list = getExecuteActions();
            if (index >= 0 && index < list.size()) return list.get(index);
            return list.get(0);
        }
        public String getScriptSplitting() { return scriptSplitting; }
        public void setScriptSplitting(String scriptSplitting) { this.scriptSplitting = scriptSplitting; }
        public boolean isReviewParametersBeforeExecution() { return reviewParametersBeforeExecution; }
        public void setReviewParametersBeforeExecution(boolean reviewParametersBeforeExecution) {
            this.reviewParametersBeforeExecution = reviewParametersBeforeExecution;
        }
        public boolean isWarnUnsafeQueries() { return warnUnsafeQueries; }
        public void setWarnUnsafeQueries(boolean warnUnsafeQueries) { this.warnUnsafeQueries = warnUnsafeQueries; }

        public boolean isShowTimestampForQueryOutput() { return showTimestampForQueryOutput; }
        public void setShowTimestampForQueryOutput(boolean showTimestampForQueryOutput) { this.showTimestampForQueryOutput = showTimestampForQueryOutput; }

        public boolean isEnableDbmsOutput() { return enableDbmsOutput; }
        public void setEnableDbmsOutput(boolean enableDbmsOutput) { this.enableDbmsOutput = enableDbmsOutput; }

        public boolean isShowResultsInEditor() { return showResultsInEditor; }
        public void setShowResultsInEditor(boolean showResultsInEditor) { this.showResultsInEditor = showResultsInEditor; }

        public boolean isCreateTitleFromComment() { return createTitleFromComment; }
        public void setCreateTitleFromComment(boolean createTitleFromComment) { this.createTitleFromComment = createTitleFromComment; }

        public String getTitleAfterCommentText() { return titleAfterCommentText; }
        public void setTitleAfterCommentText(String titleAfterCommentText) { this.titleAfterCommentText = titleAfterCommentText != null ? titleAfterCommentText : ""; }

        public String getShowServicesOutput() { return showServicesOutput; }
        public void setShowServicesOutput(String showServicesOutput) { this.showServicesOutput = showServicesOutput != null ? showServicesOutput : "For all output"; }

        public boolean isFocusServicesInWindowMode() { return focusServicesInWindowMode; }
        public void setFocusServicesInWindowMode(boolean focusServicesInWindowMode) { this.focusServicesInWindowMode = focusServicesInWindowMode; }

        public boolean isOpenNewServicesTabForSessions() { return openNewServicesTabForSessions; }
        public void setOpenNewServicesTabForSessions(boolean openNewServicesTabForSessions) { this.openNewServicesTabForSessions = openNewServicesTabForSessions; }

        public boolean isActivateServicesForSelectedFileOnly() { return activateServicesForSelectedFileOnly; }
        public void setActivateServicesForSelectedFileOnly(boolean activateServicesForSelectedFileOnly) { this.activateServicesForSelectedFileOnly = activateServicesForSelectedFileOnly; }

        public boolean isEnableUserParameters() { return enableUserParameters; }
        public void setEnableUserParameters(boolean enableUserParameters) { this.enableUserParameters = enableUserParameters; }

        public boolean isEnableUserParametersInLiteralsWithInjection() { return enableUserParametersInLiteralsWithInjection; }
        public void setEnableUserParametersInLiteralsWithInjection(boolean val) { this.enableUserParametersInLiteralsWithInjection = val; }

        public boolean isSubstituteInsideSqlStrings() { return substituteInsideSqlStrings; }
        public void setSubstituteInsideSqlStrings(boolean substituteInsideSqlStrings) { this.substituteInsideSqlStrings = substituteInsideSqlStrings; }

        public List<UserParameterPattern> getUserParameterPatterns() {
            if (userParameterPatterns == null || userParameterPatterns.isEmpty()) {
                userParameterPatterns = defaultUserParameterPatterns();
            }
            return userParameterPatterns;
        }
        public void setUserParameterPatterns(List<UserParameterPattern> patterns) {
            this.userParameterPatterns = (patterns == null || patterns.isEmpty()) ? defaultUserParameterPatterns() : patterns;
        }

        public List<CsvFormatConfig> getCsvFormats() {
            if (csvFormats == null || csvFormats.isEmpty()) {
                csvFormats = defaultCsvFormats();
            }
            return csvFormats;
        }
        public void setCsvFormats(List<CsvFormatConfig> csvFormats) {
            this.csvFormats = (csvFormats == null || csvFormats.isEmpty()) ? defaultCsvFormats() : csvFormats;
        }

        public String getDefaultCsvFormat() { return defaultCsvFormat; }
        public void setDefaultCsvFormat(String defaultCsvFormat) {
            this.defaultCsvFormat = (defaultCsvFormat != null && !defaultCsvFormat.isBlank()) ? defaultCsvFormat : "CSV";
        }

        public CsvFormatConfig getCsvFormatByName(String name) {
            if (name != null) {
                for (CsvFormatConfig cfg : getCsvFormats()) {
                    if (name.equalsIgnoreCase(cfg.getName())) return cfg;
                }
            }
            return getCsvFormats().get(0);
        }

        // Data Editor and Viewer Getters and Setters
        public boolean isLimitPageSize() { return limitPageSize; }
        public void setLimitPageSize(boolean limitPageSize) { this.limitPageSize = limitPageSize; }

        public int getResultSetPrefetchSize() { return resultSetPrefetchSize; }
        public void setResultSetPrefetchSize(int resultSetPrefetchSize) { this.resultSetPrefetchSize = resultSetPrefetchSize; }

        public int getFilterHistorySize() { return filterHistorySize; }
        public void setFilterHistorySize(int filterHistorySize) { this.filterHistorySize = filterHistorySize; }

        public int getMaxBytesLoadedPerValue() { return maxBytesLoadedPerValue; }
        public void setMaxBytesLoadedPerValue(int maxBytesLoadedPerValue) { this.maxBytesLoadedPerValue = maxBytesLoadedPerValue; }

        public boolean isShowFirstDataRowsInPreview() { return showFirstDataRowsInPreview; }
        public void setShowFirstDataRowsInPreview(boolean showFirstDataRowsInPreview) { this.showFirstDataRowsInPreview = showFirstDataRowsInPreview; }

        public int getPreviewDataRows() { return previewDataRows; }
        public void setPreviewDataRows(int previewDataRows) { this.previewDataRows = previewDataRows; }

        public boolean isEnablePagingInEditorResults() { return enablePagingInEditorResults; }
        public void setEnablePagingInEditorResults(boolean enablePagingInEditorResults) { this.enablePagingInEditorResults = enablePagingInEditorResults; }

        public String getGridPaginationPosition() { return gridPaginationPosition; }
        public void setGridPaginationPosition(String gridPaginationPosition) {
            this.gridPaginationPosition = gridPaginationPosition != null ? gridPaginationPosition : "Grid bottom (floating)";
        }

        public boolean isShowQuickActionsToolbar() { return showQuickActionsToolbar; }
        public void setShowQuickActionsToolbar(boolean showQuickActionsToolbar) { this.showQuickActionsToolbar = showQuickActionsToolbar; }

        public boolean isEnableQuickActionsCustomization() { return enableQuickActionsCustomization; }
        public void setEnableQuickActionsCustomization(boolean enableQuickActionsCustomization) { this.enableQuickActionsCustomization = enableQuickActionsCustomization; }

        public boolean isUseCustomFont() { return useCustomFont; }
        public void setUseCustomFont(boolean useCustomFont) { this.useCustomFont = useCustomFont; }

        public String getCustomFontFamily() { return customFontFamily; }
        public void setCustomFontFamily(String customFontFamily) {
            this.customFontFamily = (customFontFamily != null && !customFontFamily.isBlank()) ? customFontFamily : "JetBrains Mono";
        }

        public double getCustomFontSize() { return customFontSize; }
        public void setCustomFontSize(double customFontSize) { this.customFontSize = customFontSize > 0 ? customFontSize : 13.0; }

        public double getCustomLineHeight() { return customLineHeight; }
        public void setCustomLineHeight(double customLineHeight) { this.customLineHeight = customLineHeight > 0 ? customLineHeight : 1.2; }

        public boolean isAlternateRowColors() { return alternateRowColors; }
        public void setAlternateRowColors(boolean alternateRowColors) { this.alternateRowColors = alternateRowColors; }

        public String getShowBooleanValuesAs() { return showBooleanValuesAs; }
        public void setShowBooleanValuesAs(String showBooleanValuesAs) {
            this.showBooleanValuesAs = (showBooleanValuesAs != null && !showBooleanValuesAs.isBlank()) ? showBooleanValuesAs : "Text";
        }

        public String getAutomaticallyTransposeTables() { return automaticallyTransposeTables; }
        public void setAutomaticallyTransposeTables(String automaticallyTransposeTables) {
            this.automaticallyTransposeTables = (automaticallyTransposeTables != null && !automaticallyTransposeTables.isBlank()) ? automaticallyTransposeTables : "Never";
        }

        public boolean isDetectBinaryAsText() { return detectBinaryAsText; }
        public void setDetectBinaryAsText(boolean detectBinaryAsText) { this.detectBinaryAsText = detectBinaryAsText; }

        public boolean isDetectBinaryAsUuid() { return detectBinaryAsUuid; }
        public void setDetectBinaryAsUuid(boolean detectBinaryAsUuid) { this.detectBinaryAsUuid = detectBinaryAsUuid; }

        public boolean isEnableLocalFilterByDefault() { return enableLocalFilterByDefault; }
        public void setEnableLocalFilterByDefault(boolean enableLocalFilterByDefault) { this.enableLocalFilterByDefault = enableLocalFilterByDefault; }

        public boolean isEnableImmediateCompletionInGridTextCells() { return enableImmediateCompletionInGridTextCells; }
        public void setEnableImmediateCompletionInGridTextCells(boolean enableImmediateCompletionInGridTextCells) {
            this.enableImmediateCompletionInGridTextCells = enableImmediateCompletionInGridTextCells;
        }

        public String getDisplayTemporalDataInTimeZone() { return displayTemporalDataInTimeZone; }
        public void setDisplayTemporalDataInTimeZone(String displayTemporalDataInTimeZone) {
            this.displayTemporalDataInTimeZone = displayTemporalDataInTimeZone != null ? displayTemporalDataInTimeZone : "";
        }

        public String getDecimalSeparator() { return decimalSeparator; }
        public void setDecimalSeparator(String decimalSeparator) {
            this.decimalSeparator = (decimalSeparator != null && !decimalSeparator.isBlank()) ? decimalSeparator : ".";
        }

        public boolean isEnableGroupingSeparator() { return enableGroupingSeparator; }
        public void setEnableGroupingSeparator(boolean enableGroupingSeparator) { this.enableGroupingSeparator = enableGroupingSeparator; }

        public String getGroupingSeparator() { return groupingSeparator; }
        public void setGroupingSeparator(String groupingSeparator) {
            this.groupingSeparator = groupingSeparator != null ? groupingSeparator : "";
        }

        public String getInfinityText() { return infinityText; }
        public void setInfinityText(String infinityText) {
            this.infinityText = (infinityText != null && !infinityText.isBlank()) ? infinityText : "Infinity";
        }

        public String getNanText() { return nanText; }
        public void setNanText(String nanText) {
            this.nanText = (nanText != null && !nanText.isBlank()) ? nanText : "NaN";
        }

        public boolean isEnableNumberPattern() { return enableNumberPattern; }
        public void setEnableNumberPattern(boolean enableNumberPattern) { this.enableNumberPattern = enableNumberPattern; }

        public String getNumberPattern() { return numberPattern; }
        public void setNumberPattern(String numberPattern) {
            this.numberPattern = numberPattern != null ? numberPattern : "";
        }

        public boolean isEnableDatetimeTimestamp() { return enableDatetimeTimestamp; }
        public void setEnableDatetimeTimestamp(boolean enableDatetimeTimestamp) { this.enableDatetimeTimestamp = enableDatetimeTimestamp; }

        public String getDatetimeTimestampPattern() { return datetimeTimestampPattern; }
        public void setDatetimeTimestampPattern(String datetimeTimestampPattern) {
            this.datetimeTimestampPattern = (datetimeTimestampPattern != null && !datetimeTimestampPattern.isBlank()) ? datetimeTimestampPattern : "yyyy-MM-dd HH:mm:ss";
        }

        public boolean isEnableDatetimeTimestampWithZone() { return enableDatetimeTimestampWithZone; }
        public void setEnableDatetimeTimestampWithZone(boolean enableDatetimeTimestampWithZone) {
            this.enableDatetimeTimestampWithZone = enableDatetimeTimestampWithZone;
        }

        public String getDatetimeTimestampWithZonePattern() { return datetimeTimestampWithZonePattern; }
        public void setDatetimeTimestampWithZonePattern(String datetimeTimestampWithZonePattern) {
            this.datetimeTimestampWithZonePattern = (datetimeTimestampWithZonePattern != null && !datetimeTimestampWithZonePattern.isBlank()) ? datetimeTimestampWithZonePattern : "yyyy-MM-dd HH:mm:ss Z";
        }

        public boolean isEnableTime() { return enableTime; }
        public void setEnableTime(boolean enableTime) { this.enableTime = enableTime; }

        public String getTimePattern() { return timePattern; }
        public void setTimePattern(String timePattern) {
            this.timePattern = (timePattern != null && !timePattern.isBlank()) ? timePattern : "HH:mm:ss";
        }

        public boolean isEnableTimeWithZone() { return enableTimeWithZone; }
        public void setEnableTimeWithZone(boolean enableTimeWithZone) { this.enableTimeWithZone = enableTimeWithZone; }

        public String getTimeWithZonePattern() { return timeWithZonePattern; }
        public void setTimeWithZonePattern(String timeWithZonePattern) {
            this.timeWithZonePattern = (timeWithZonePattern != null && !timeWithZonePattern.isBlank()) ? timeWithZonePattern : "HH:mm:ss Z";
        }

        public boolean isEnableDate() { return enableDate; }
        public void setEnableDate(boolean enableDate) { this.enableDate = enableDate; }

        public String getDatePattern() { return datePattern; }
        public void setDatePattern(String datePattern) {
            this.datePattern = (datePattern != null && !datePattern.isBlank()) ? datePattern : "yyyy-MM-dd";
        }

        public boolean isSortViaOrderBy() { return sortViaOrderBy; }
        public void setSortViaOrderBy(boolean sortViaOrderBy) { this.sortViaOrderBy = sortViaOrderBy; }

        public boolean isSortTablesByNumericPk() { return sortTablesByNumericPk; }
        public void setSortTablesByNumericPk(boolean sortTablesByNumericPk) { this.sortTablesByNumericPk = sortTablesByNumericPk; }

        public String getSortTablesByNumericPkDirection() { return sortTablesByNumericPkDirection; }
        public void setSortTablesByNumericPkDirection(String sortTablesByNumericPkDirection) {
            this.sortTablesByNumericPkDirection = (sortTablesByNumericPkDirection != null && !sortTablesByNumericPkDirection.isBlank()) ? sortTablesByNumericPkDirection : "Ascending";
        }

        public String getAddColumnsToSorting() { return addColumnsToSorting; }
        public void setAddColumnsToSorting(String addColumnsToSorting) {
            this.addColumnsToSorting = (addColumnsToSorting != null && !addColumnsToSorting.isBlank()) ? addColumnsToSorting : "⌥Click";
        }

        public boolean isSubmitChangesImmediately() { return submitChangesImmediately; }
        public void setSubmitChangesImmediately(boolean submitChangesImmediately) { this.submitChangesImmediately = submitChangesImmediately; }

        public boolean isEnableEditingForQueriesWithJoin() { return enableEditingForQueriesWithJoin; }
        public void setEnableEditingForQueriesWithJoin(boolean enableEditingForQueriesWithJoin) { this.enableEditingForQueriesWithJoin = enableEditingForQueriesWithJoin; }

        public boolean isShowDmlPreviewForQueriesWithJoin() { return showDmlPreviewForQueriesWithJoin; }
        public void setShowDmlPreviewForQueriesWithJoin(boolean showDmlPreviewForQueriesWithJoin) { this.showDmlPreviewForQueriesWithJoin = showDmlPreviewForQueriesWithJoin; }

        public boolean isAllowOpenSecureLinks() { return allowOpenSecureLinks; }
        public void setAllowOpenSecureLinks(boolean allowOpenSecureLinks) { this.allowOpenSecureLinks = allowOpenSecureLinks; }

        public boolean isAllowOpenStandardLinks() { return allowOpenStandardLinks; }
        public void setAllowOpenStandardLinks(boolean allowOpenStandardLinks) { this.allowOpenStandardLinks = allowOpenStandardLinks; }

        public boolean isAllowOpenLocalFileLinks() { return allowOpenLocalFileLinks; }
        public void setAllowOpenLocalFileLinks(boolean allowOpenLocalFileLinks) { this.allowOpenLocalFileLinks = allowOpenLocalFileLinks; }

        public boolean isAssumeHttpIfNoProtocol() { return assumeHttpIfNoProtocol; }
        public void setAssumeHttpIfNoProtocol(boolean assumeHttpIfNoProtocol) { this.assumeHttpIfNoProtocol = assumeHttpIfNoProtocol; }

        // Database Explorer Getters and Setters
        public boolean isRememberFilterState() { return rememberFilterState; }
        public void setRememberFilterState(boolean rememberFilterState) { this.rememberFilterState = rememberFilterState; }

        public boolean isShowDatabaseColors() { return showDatabaseColors; }
        public void setShowDatabaseColors(boolean showDatabaseColors) { this.showDatabaseColors = showDatabaseColors; }

        public boolean isColorDatabaseExplorer() { return colorDatabaseExplorer; }
        public void setColorDatabaseExplorer(boolean colorDatabaseExplorer) { this.colorDatabaseExplorer = colorDatabaseExplorer; }

        public boolean isColorEditorTabHeaders() { return colorEditorTabHeaders; }
        public void setColorEditorTabHeaders(boolean colorEditorTabHeaders) { this.colorEditorTabHeaders = colorEditorTabHeaders; }

        public boolean isColorEditorBackgrounds() { return colorEditorBackgrounds; }
        public void setColorEditorBackgrounds(boolean colorEditorBackgrounds) { this.colorEditorBackgrounds = colorEditorBackgrounds; }

        public boolean isColorEditorToolbars() { return colorEditorToolbars; }
        public void setColorEditorToolbars(boolean colorEditorToolbars) { this.colorEditorToolbars = colorEditorToolbars; }

        // AI Tools Getters and Setters
        public boolean isAiReadDatabaseSchemas() { return aiReadDatabaseSchemas; }
        public void setAiReadDatabaseSchemas(boolean aiReadDatabaseSchemas) { this.aiReadDatabaseSchemas = aiReadDatabaseSchemas; }

        public boolean isAiModifyDatabaseSchemas() { return aiModifyDatabaseSchemas; }
        public void setAiModifyDatabaseSchemas(boolean aiModifyDatabaseSchemas) { this.aiModifyDatabaseSchemas = aiModifyDatabaseSchemas; }

        public boolean isAiReadDatabaseData() { return aiReadDatabaseData; }
        public void setAiReadDatabaseData(boolean aiReadDatabaseData) { this.aiReadDatabaseData = aiReadDatabaseData; }

        public boolean isAiModifyDatabaseData() { return aiModifyDatabaseData; }
        public void setAiModifyDatabaseData(boolean aiModifyDatabaseData) { this.aiModifyDatabaseData = aiModifyDatabaseData; }

        // Query Files and Consoles Getters and Setters
        public boolean isShowDataSourceNameInFileTree() { return showDataSourceNameInFileTree; }
        public void setShowDataSourceNameInFileTree(boolean showDataSourceNameInFileTree) { this.showDataSourceNameInFileTree = showDataSourceNameInFileTree; }

        public boolean isUseAttachedSearchPathColorInFileTree() { return useAttachedSearchPathColorInFileTree; }
        public void setUseAttachedSearchPathColorInFileTree(boolean useAttachedSearchPathColorInFileTree) { this.useAttachedSearchPathColorInFileTree = useAttachedSearchPathColorInFileTree; }

        public boolean isUseAttachedDataSourceIconForQueryFiles() { return useAttachedDataSourceIconForQueryFiles; }
        public void setUseAttachedDataSourceIconForQueryFiles(boolean useAttachedDataSourceIconForQueryFiles) { this.useAttachedDataSourceIconForQueryFiles = useAttachedDataSourceIconForQueryFiles; }

        public String getDefaultConsoleFileName() { return defaultConsoleFileName; }
        public void setDefaultConsoleFileName(String defaultConsoleFileName) {
            this.defaultConsoleFileName = (defaultConsoleFileName != null && !defaultConsoleFileName.isBlank()) ? defaultConsoleFileName.trim() : "console";
        }

        public String getEditorTabTitleTemplate() { return editorTabTitleTemplate; }
        public void setEditorTabTitleTemplate(String editorTabTitleTemplate) {
            this.editorTabTitleTemplate = (editorTabTitleTemplate != null && !editorTabTitleTemplate.isBlank()) ? editorTabTitleTemplate.trim() : "$NAME$ [$DATASOURCE$]";
        }

        public boolean isUseTemplateForQueryFiles() { return useTemplateForQueryFiles; }
        public void setUseTemplateForQueryFiles(boolean useTemplateForQueryFiles) { this.useTemplateForQueryFiles = useTemplateForQueryFiles; }

        // SQL Dialects Getters and Setters
        public String getGlobalSqlDialect() { return globalSqlDialect; }
        public void setGlobalSqlDialect(String globalSqlDialect) {
            this.globalSqlDialect = (globalSqlDialect != null && !globalSqlDialect.isBlank()) ? globalSqlDialect : "<None>";
        }

        public String getProjectSqlDialect() { return projectSqlDialect; }
        public void setProjectSqlDialect(String projectSqlDialect) {
            this.projectSqlDialect = (projectSqlDialect != null && !projectSqlDialect.isBlank()) ? projectSqlDialect : "<None>";
        }

        public List<SqlDialectMappingConfig> getSqlDialectMappings() {
            if (sqlDialectMappings == null) sqlDialectMappings = new ArrayList<>();
            return sqlDialectMappings;
        }
        public void setSqlDialectMappings(List<SqlDialectMappingConfig> sqlDialectMappings) {
            this.sqlDialectMappings = (sqlDialectMappings != null) ? sqlDialectMappings : new ArrayList<>();
        }

        // SQL Resolution Scopes Getters and Setters
        public String getProjectResolutionScope() { return projectResolutionScope; }
        public void setProjectResolutionScope(String projectResolutionScope) {
            this.projectResolutionScope = (projectResolutionScope != null && !projectResolutionScope.isBlank()) ? projectResolutionScope : "<Default> (<Everything>)";
        }

        public List<SqlResolutionScopeMappingConfig> getSqlResolutionScopeMappings() {
            if (sqlResolutionScopeMappings == null) sqlResolutionScopeMappings = new ArrayList<>();
            return sqlResolutionScopeMappings;
        }
        public void setSqlResolutionScopeMappings(List<SqlResolutionScopeMappingConfig> sqlResolutionScopeMappings) {
            this.sqlResolutionScopeMappings = (sqlResolutionScopeMappings != null) ? sqlResolutionScopeMappings : new ArrayList<>();
        }

        // Other Settings Getters and Setters
        public boolean isConfirmCancellationForModifySchemaDialogs() { return confirmCancellationForModifySchemaDialogs; }
        public void setConfirmCancellationForModifySchemaDialogs(boolean val) { this.confirmCancellationForModifySchemaDialogs = val; }

        public boolean isShowPreviewOfValidScriptWhenUpdatingSource() { return showPreviewOfValidScriptWhenUpdatingSource; }
        public void setShowPreviewOfValidScriptWhenUpdatingSource(boolean val) { this.showPreviewOfValidScriptWhenUpdatingSource = val; }

        public boolean isSuggestDumpingDdlForNewMappings() { return suggestDumpingDdlForNewMappings; }
        public void setSuggestDumpingDdlForNewMappings(boolean val) { this.suggestDumpingDdlForNewMappings = val; }

        public String getGenerateContextTemplates() { return generateContextTemplates; }
        public void setGenerateContextTemplates(String val) {
            this.generateContextTemplates = (val != null && !val.isBlank()) ? val : "Append to existing console";
        }

        public List<VirtualForeignKeyRule> getVirtualForeignKeys() {
            if (virtualForeignKeys == null || virtualForeignKeys.isEmpty()) {
                virtualForeignKeys = defaultVirtualForeignKeys();
            }
            return virtualForeignKeys;
        }
        public void setVirtualForeignKeys(List<VirtualForeignKeyRule> virtualForeignKeys) {
            this.virtualForeignKeys = (virtualForeignKeys != null && !virtualForeignKeys.isEmpty()) ? virtualForeignKeys : defaultVirtualForeignKeys();
        }

        public String getDefaultResolveModeForConsoles() { return defaultResolveModeForConsoles; }
        public void setDefaultResolveModeForConsoles(String val) {
            this.defaultResolveModeForConsoles = (val != null && !val.isBlank()) ? val : "Playground";
        }

        public String getStatementDelimiter() { return statementDelimiter; }
        public void setStatementDelimiter(String statementDelimiter) {
            this.statementDelimiter = statementDelimiter != null ? statementDelimiter : "";
        }

        // Appearance: Theme & Colors Getters and Setters
        public String getUiTheme() { return uiTheme; }
        public void setUiTheme(String uiTheme) {
            this.uiTheme = (uiTheme != null && !uiTheme.isBlank()) ? uiTheme : "Islands Dark";
        }

        public boolean isSyncThemeWithOs() { return syncThemeWithOs; }
        public void setSyncThemeWithOs(boolean syncThemeWithOs) { this.syncThemeWithOs = syncThemeWithOs; }

        public String getEditorColorScheme() { return editorColorScheme; }
        public void setEditorColorScheme(String editorColorScheme) {
            this.editorColorScheme = (editorColorScheme != null && !editorColorScheme.isBlank()) ? editorColorScheme : "Islands Dark Theme default";
        }

        public boolean isDifferentToolWindowBackground() { return differentToolWindowBackground; }
        public void setDifferentToolWindowBackground(boolean differentToolWindowBackground) { this.differentToolWindowBackground = differentToolWindowBackground; }

        // Appearance: Accessibility Getters and Setters
        public String getIdeZoom() { return ideZoom; }
        public void setIdeZoom(String ideZoom) {
            this.ideZoom = (ideZoom != null && !ideZoom.isBlank()) ? ideZoom : "100%";
        }

        public boolean isUseCustomIdeFont() { return useCustomIdeFont; }
        public void setUseCustomIdeFont(boolean useCustomIdeFont) { this.useCustomIdeFont = useCustomIdeFont; }

        public String getCustomIdeFontFamily() { return customIdeFontFamily; }
        public void setCustomIdeFontFamily(String customIdeFontFamily) {
            this.customIdeFontFamily = (customIdeFontFamily != null && !customIdeFontFamily.isBlank()) ? customIdeFontFamily : "Inter";
        }

        public int getCustomIdeFontSize() { return customIdeFontSize; }
        public void setCustomIdeFontSize(int customIdeFontSize) {
            this.customIdeFontSize = (customIdeFontSize > 0) ? customIdeFontSize : 13;
        }

        public boolean isSupportScreenReaders() { return supportScreenReaders; }
        public void setSupportScreenReaders(boolean supportScreenReaders) { this.supportScreenReaders = supportScreenReaders; }

        public boolean isUseContrastScrollbars() { return useContrastScrollbars; }
        public void setUseContrastScrollbars(boolean useContrastScrollbars) { this.useContrastScrollbars = useContrastScrollbars; }

        public boolean isAdjustColorsForVisionDeficiency() { return adjustColorsForVisionDeficiency; }
        public void setAdjustColorsForVisionDeficiency(boolean adjustColorsForVisionDeficiency) { this.adjustColorsForVisionDeficiency = adjustColorsForVisionDeficiency; }

        // Appearance: UI Options Getters and Setters
        public boolean isCompactMode() { return compactMode; }
        public void setCompactMode(boolean compactMode) { this.compactMode = compactMode; }

        public boolean isAlwaysShowFullPathInWindowHeader() { return alwaysShowFullPathInWindowHeader; }
        public void setAlwaysShowFullPathInWindowHeader(boolean alwaysShowFullPathInWindowHeader) { this.alwaysShowFullPathInWindowHeader = alwaysShowFullPathInWindowHeader; }

        public boolean isUseProjectColorsInMainToolbar() { return useProjectColorsInMainToolbar; }
        public void setUseProjectColorsInMainToolbar(boolean useProjectColorsInMainToolbar) { this.useProjectColorsInMainToolbar = useProjectColorsInMainToolbar; }

        public boolean isKeepPopupsOpenForToggleItems() { return keepPopupsOpenForToggleItems; }
        public void setKeepPopupsOpenForToggleItems(boolean keepPopupsOpenForToggleItems) { this.keepPopupsOpenForToggleItems = keepPopupsOpenForToggleItems; }

        public boolean isDragAndDropWithAltPressedOnly() { return dragAndDropWithAltPressedOnly; }
        public void setDragAndDropWithAltPressedOnly(boolean dragAndDropWithAltPressedOnly) { this.dragAndDropWithAltPressedOnly = dragAndDropWithAltPressedOnly; }

        public boolean isSmoothScrolling() { return smoothScrolling; }
        public void setSmoothScrolling(boolean smoothScrolling) { this.smoothScrolling = smoothScrolling; }

        public boolean isEnableMnemonicsInControls() { return enableMnemonicsInControls; }
        public void setEnableMnemonicsInControls(boolean enableMnemonicsInControls) { this.enableMnemonicsInControls = enableMnemonicsInControls; }

        public boolean isEnableMnemonicsInMenu() { return enableMnemonicsInMenu; }
        public void setEnableMnemonicsInMenu(boolean enableMnemonicsInMenu) { this.enableMnemonicsInMenu = enableMnemonicsInMenu; }

        public boolean isDisplayIconsInMenuItems() { return displayIconsInMenuItems; }
        public void setDisplayIconsInMenuItems(boolean displayIconsInMenuItems) { this.displayIconsInMenuItems = displayIconsInMenuItems; }

        public String getMainMenuPresentation() { return mainMenuPresentation; }
        public void setMainMenuPresentation(String mainMenuPresentation) {
            this.mainMenuPresentation = (mainMenuPresentation != null && !mainMenuPresentation.isBlank()) ? mainMenuPresentation : "Hide under Hamburger Button";
        }

        // Appearance: Background Image Getters and Setters
        public String getBackgroundImagePath() { return backgroundImagePath; }
        public void setBackgroundImagePath(String backgroundImagePath) {
            this.backgroundImagePath = backgroundImagePath != null ? backgroundImagePath : "";
        }

        public int getBackgroundImageOpacity() { return backgroundImageOpacity; }
        public void setBackgroundImageOpacity(int backgroundImageOpacity) {
            this.backgroundImageOpacity = Math.max(0, Math.min(100, backgroundImageOpacity));
        }

        public String getBackgroundImagePlacement() { return backgroundImagePlacement; }
        public void setBackgroundImagePlacement(String backgroundImagePlacement) {
            this.backgroundImagePlacement = (backgroundImagePlacement != null && !backgroundImagePlacement.isBlank()) ? backgroundImagePlacement : "Fill";
        }

        public boolean isBackgroundImageThisProjectOnly() { return backgroundImageThisProjectOnly; }
        public void setBackgroundImageThisProjectOnly(boolean val) { this.backgroundImageThisProjectOnly = val; }

        public String getBackgroundImageTarget() { return backgroundImageTarget; }
        public void setBackgroundImageTarget(String val) {
            this.backgroundImageTarget = (val != null && !val.isBlank()) ? val : "Editor and Tools";
        }

        // Appearance: Tree Views Getters and Setters
        public boolean isShowIndentGuides() { return showIndentGuides; }
        public void setShowIndentGuides(boolean showIndentGuides) { this.showIndentGuides = showIndentGuides; }

        public boolean isUseSmallerIndents() { return useSmallerIndents; }
        public void setUseSmallerIndents(boolean useSmallerIndents) { this.useSmallerIndents = useSmallerIndents; }

        // Appearance: Tool Windows Getters and Setters
        public boolean isShowToolWindowBars() { return showToolWindowBars; }
        public void setShowToolWindowBars(boolean showToolWindowBars) { this.showToolWindowBars = showToolWindowBars; }

        public boolean isShowToolWindowNames() { return showToolWindowNames; }
        public void setShowToolWindowNames(boolean showToolWindowNames) { this.showToolWindowNames = showToolWindowNames; }

        public boolean isSideBySideLayoutOnLeft() { return sideBySideLayoutOnLeft; }
        public void setSideBySideLayoutOnLeft(boolean sideBySideLayoutOnLeft) { this.sideBySideLayoutOnLeft = sideBySideLayoutOnLeft; }

        public boolean isSideBySideLayoutOnRight() { return sideBySideLayoutOnRight; }
        public void setSideBySideLayoutOnRight(boolean sideBySideLayoutOnRight) { this.sideBySideLayoutOnRight = sideBySideLayoutOnRight; }

        public boolean isWidescreenToolWindowLayout() { return widescreenToolWindowLayout; }
        public void setWidescreenToolWindowLayout(boolean widescreenToolWindowLayout) { this.widescreenToolWindowLayout = widescreenToolWindowLayout; }

        public boolean isRememberSizeForEachToolWindow() { return rememberSizeForEachToolWindow; }
        public void setRememberSizeForEachToolWindow(boolean rememberSizeForEachToolWindow) { this.rememberSizeForEachToolWindow = rememberSizeForEachToolWindow; }

        // Appearance: Presentation Mode Getters and Setters
        public String getPresentationModeZoom() { return presentationModeZoom; }
        public void setPresentationModeZoom(String presentationModeZoom) {
            this.presentationModeZoom = (presentationModeZoom != null && !presentationModeZoom.isBlank()) ? presentationModeZoom : "175%";
        }

        // Appearance: Antialiasing Getters and Setters
        public String getIdeAntialiasing() { return ideAntialiasing; }
        public void setIdeAntialiasing(String ideAntialiasing) {
            this.ideAntialiasing = (ideAntialiasing != null && !ideAntialiasing.isBlank()) ? ideAntialiasing : "Subpixel";
        }

        public String getEditorAntialiasing() { return editorAntialiasing; }
        public void setEditorAntialiasing(String editorAntialiasing) {
            this.editorAntialiasing = (editorAntialiasing != null && !editorAntialiasing.isBlank()) ? editorAntialiasing : "Subpixel";
        }

        // Editor > General > Auto Import Getters and Setters
        public boolean isShowXmlAutoImportTooltip() { return showXmlAutoImportTooltip; }
        public void setShowXmlAutoImportTooltip(boolean showXmlAutoImportTooltip) { this.showXmlAutoImportTooltip = showXmlAutoImportTooltip; }

        // Editor > General > Breadcrumbs Getters and Setters
        public boolean isShowBreadcrumbs() { return showBreadcrumbs; }
        public void setShowBreadcrumbs(boolean showBreadcrumbs) { this.showBreadcrumbs = showBreadcrumbs; }
        public String getBreadcrumbsPlacement() { return breadcrumbsPlacement; }
        public void setBreadcrumbsPlacement(String breadcrumbsPlacement) {
            this.breadcrumbsPlacement = (breadcrumbsPlacement != null && !breadcrumbsPlacement.isBlank()) ? breadcrumbsPlacement : "Bottom";
        }
        public boolean isBreadcrumbsHtml() { return breadcrumbsHtml; }
        public void setBreadcrumbsHtml(boolean breadcrumbsHtml) { this.breadcrumbsHtml = breadcrumbsHtml; }
        public boolean isBreadcrumbsMarkdown() { return breadcrumbsMarkdown; }
        public void setBreadcrumbsMarkdown(boolean breadcrumbsMarkdown) { this.breadcrumbsMarkdown = breadcrumbsMarkdown; }
        public boolean isBreadcrumbsXhtml() { return breadcrumbsXhtml; }
        public void setBreadcrumbsXhtml(boolean breadcrumbsXhtml) { this.breadcrumbsXhtml = breadcrumbsXhtml; }
        public boolean isBreadcrumbsJson() { return breadcrumbsJson; }
        public void setBreadcrumbsJson(boolean breadcrumbsJson) { this.breadcrumbsJson = breadcrumbsJson; }
        public boolean isBreadcrumbsSql() { return breadcrumbsSql; }
        public void setBreadcrumbsSql(boolean breadcrumbsSql) { this.breadcrumbsSql = breadcrumbsSql; }
        public boolean isBreadcrumbsXml() { return breadcrumbsXml; }
        public void setBreadcrumbsXml(boolean breadcrumbsXml) { this.breadcrumbsXml = breadcrumbsXml; }

        // Editor > General > Appearance Getters and Setters
        public boolean isCaretBlinking() { return caretBlinking; }
        public void setCaretBlinking(boolean caretBlinking) { this.caretBlinking = caretBlinking; }
        public int getCaretBlinkingMs() { return caretBlinkingMs; }
        public void setCaretBlinkingMs(int caretBlinkingMs) { this.caretBlinkingMs = caretBlinkingMs > 0 ? caretBlinkingMs : 500; }
        public boolean isUseBlockCaret() { return useBlockCaret; }
        public void setUseBlockCaret(boolean useBlockCaret) { this.useBlockCaret = useBlockCaret; }
        public boolean isUseFullLineHeightCaret() { return useFullLineHeightCaret; }
        public void setUseFullLineHeightCaret(boolean useFullLineHeightCaret) { this.useFullLineHeightCaret = useFullLineHeightCaret; }
        public boolean isHighlightOccurrences() { return highlightOccurrences; }
        public void setHighlightOccurrences(boolean highlightOccurrences) { this.highlightOccurrences = highlightOccurrences; }
        public boolean isShowHardWrapAndVisualGuides() { return showHardWrapAndVisualGuides; }
        public void setShowHardWrapAndVisualGuides(boolean showHardWrapAndVisualGuides) { this.showHardWrapAndVisualGuides = showHardWrapAndVisualGuides; }
        public boolean isShowLineNumbers() { return showLineNumbers; }
        public void setShowLineNumbers(boolean showLineNumbers) { this.showLineNumbers = showLineNumbers; }
        public String getLineNumbersMode() { return lineNumbersMode; }
        public void setLineNumbersMode(String lineNumbersMode) {
            this.lineNumbersMode = (lineNumbersMode != null && !lineNumbersMode.isBlank()) ? lineNumbersMode : "Absolute";
        }
        public boolean isShowLinesBetweenStatements() { return showLinesBetweenStatements; }
        public void setShowLinesBetweenStatements(boolean showLinesBetweenStatements) { this.showLinesBetweenStatements = showLinesBetweenStatements; }
        public boolean isShowWhitespaces() { return showWhitespaces; }
        public void setShowWhitespaces(boolean showWhitespaces) { this.showWhitespaces = showWhitespaces; }
        public boolean isShowWhitespacesLeading() { return showWhitespacesLeading; }
        public void setShowWhitespacesLeading(boolean showWhitespacesLeading) { this.showWhitespacesLeading = showWhitespacesLeading; }
        public boolean isShowWhitespacesInner() { return showWhitespacesInner; }
        public void setShowWhitespacesInner(boolean showWhitespacesInner) { this.showWhitespacesInner = showWhitespacesInner; }
        public boolean isShowWhitespacesTrailing() { return showWhitespacesTrailing; }
        public void setShowWhitespacesTrailing(boolean showWhitespacesTrailing) { this.showWhitespacesTrailing = showWhitespacesTrailing; }
        public boolean isShowWhitespacesSelection() { return showWhitespacesSelection; }
        public void setShowWhitespacesSelection(boolean showWhitespacesSelection) { this.showWhitespacesSelection = showWhitespacesSelection; }
        public boolean showEditorIndentGuides() { return showEditorIndentGuides; }
        public boolean isShowEditorIndentGuides() { return showEditorIndentGuides; }
        public void setShowEditorIndentGuides(boolean showEditorIndentGuides) { this.showEditorIndentGuides = showEditorIndentGuides; }
        public boolean showIntentionBulb() { return showIntentionBulb; }
        public boolean isShowIntentionBulb() { return showIntentionBulb; }
        public void setShowIntentionBulb(boolean showIntentionBulb) { this.showIntentionBulb = showIntentionBulb; }
        public boolean isShowPreviewForIntentionActions() { return showPreviewForIntentionActions; }
        public void setShowPreviewForIntentionActions(boolean showPreviewForIntentionActions) { this.showPreviewForIntentionActions = showPreviewForIntentionActions; }
        public boolean isRenderDocComments() { return renderDocComments; }
        public void setRenderDocComments(boolean renderDocComments) { this.renderDocComments = renderDocComments; }
        public boolean isShowCodeLensOnScrollbarHover() { return showCodeLensOnScrollbarHover; }
        public void setShowCodeLensOnScrollbarHover(boolean showCodeLensOnScrollbarHover) { this.showCodeLensOnScrollbarHover = showCodeLensOnScrollbarHover; }
        public boolean isUseEditorFontForInlayHints() { return useEditorFontForInlayHints; }
        public void setUseEditorFontForInlayHints(boolean useEditorFontForInlayHints) { this.useEditorFontForInlayHints = useEditorFontForInlayHints; }
        public boolean isEnableTagTreeHighlighting() { return enableTagTreeHighlighting; }
        public void setEnableTagTreeHighlighting(boolean enableTagTreeHighlighting) { this.enableTagTreeHighlighting = enableTagTreeHighlighting; }
        public int getTagTreeLevelsToHighlight() { return tagTreeLevelsToHighlight; }
        public void setTagTreeLevelsToHighlight(int tagTreeLevelsToHighlight) { this.tagTreeLevelsToHighlight = tagTreeLevelsToHighlight > 0 ? tagTreeLevelsToHighlight : 6; }
        public double getTagTreeOpacity() { return tagTreeOpacity; }
        public void setTagTreeOpacity(double tagTreeOpacity) { this.tagTreeOpacity = (tagTreeOpacity >= 0.0 && tagTreeOpacity <= 1.0) ? tagTreeOpacity : 0.1; }

        public boolean isMatchCase() { return matchCase; }
        public void setMatchCase(boolean matchCase) { this.matchCase = matchCase; }
        public String getMatchCaseMode() { return matchCaseMode; }
        public void setMatchCaseMode(String matchCaseMode) { this.matchCaseMode = matchCaseMode != null ? matchCaseMode : "First letter only"; }
        public boolean isSortSuggestionsAlphabetically() { return sortSuggestionsAlphabetically; }
        public void setSortSuggestionsAlphabetically(boolean sortSuggestionsAlphabetically) { this.sortSuggestionsAlphabetically = sortSuggestionsAlphabetically; }
        public boolean isShowSuggestionsAsYouType() { return showSuggestionsAsYouType; }
        public void setShowSuggestionsAsYouType(boolean showSuggestionsAsYouType) { this.showSuggestionsAsYouType = showSuggestionsAsYouType; }
        public boolean isInsertSelectedSuggestionByContextKeys() { return insertSelectedSuggestionByContextKeys; }
        public void setInsertSelectedSuggestionByContextKeys(boolean insertSelectedSuggestionByContextKeys) { this.insertSelectedSuggestionByContextKeys = insertSelectedSuggestionByContextKeys; }
        public boolean isShowDocPopup() { return showDocPopup; }
        public void setShowDocPopup(boolean showDocPopup) { this.showDocPopup = showDocPopup; }
        public int getDocPopupDelayMs() { return docPopupDelayMs; }
        public void setDocPopupDelayMs(int docPopupDelayMs) { this.docPopupDelayMs = docPopupDelayMs >= 0 ? docPopupDelayMs : 500; }
        public boolean isInsertParenthesesAutomatically() { return insertParenthesesAutomatically; }
        public void setInsertParenthesesAutomatically(boolean insertParenthesesAutomatically) { this.insertParenthesesAutomatically = insertParenthesesAutomatically; }
        public boolean isMlSortSuggestions() { return mlSortSuggestions; }
        public void setMlSortSuggestions(boolean mlSortSuggestions) { this.mlSortSuggestions = mlSortSuggestions; }
        public boolean isMlSortSql() { return mlSortSql; }
        public void setMlSortSql(boolean mlSortSql) { this.mlSortSql = mlSortSql; }
        public boolean isMlMarkPositionChanges() { return mlMarkPositionChanges; }
        public void setMlMarkPositionChanges(boolean mlMarkPositionChanges) { this.mlMarkPositionChanges = mlMarkPositionChanges; }
        public boolean isMlMarkMostRelevant() { return mlMarkMostRelevant; }
        public void setMlMarkMostRelevant(boolean mlMarkMostRelevant) { this.mlMarkMostRelevant = mlMarkMostRelevant; }
        public boolean isHtmlAutoPopupTagCompletion() { return htmlAutoPopupTagCompletion; }
        public void setHtmlAutoPopupTagCompletion(boolean htmlAutoPopupTagCompletion) { this.htmlAutoPopupTagCompletion = htmlAutoPopupTagCompletion; }
        public boolean isShowParameterInfoPopup() { return showParameterInfoPopup; }
        public void setShowParameterInfoPopup(boolean showParameterInfoPopup) { this.showParameterInfoPopup = showParameterInfoPopup; }
        public int getParameterInfoDelayMs() { return parameterInfoDelayMs; }
        public void setParameterInfoDelayMs(int parameterInfoDelayMs) { this.parameterInfoDelayMs = parameterInfoDelayMs >= 0 ? parameterInfoDelayMs : 1000; }
        public boolean isShowFullMethodSignatures() { return showFullMethodSignatures; }
        public void setShowFullMethodSignatures(boolean showFullMethodSignatures) { this.showFullMethodSignatures = showFullMethodSignatures; }
        public String getSqlSuggestObjectsFrom() { return sqlSuggestObjectsFrom; }
        public void setSqlSuggestObjectsFrom(String sqlSuggestObjectsFrom) { this.sqlSuggestObjectsFrom = sqlSuggestObjectsFrom != null ? sqlSuggestObjectsFrom : "The current scope"; }
        public String getQualifyWithDatabase() { return qualifyWithDatabase; }
        public void setQualifyWithDatabase(String qualifyWithDatabase) { this.qualifyWithDatabase = qualifyWithDatabase != null ? qualifyWithDatabase : "Always"; }
        public String getQualifyWithSchema() { return qualifyWithSchema; }
        public void setQualifyWithSchema(String qualifyWithSchema) { this.qualifyWithSchema = qualifyWithSchema != null ? qualifyWithSchema : "Always"; }
        public String getQualifyWithTableView() { return qualifyWithTableView; }
        public void setQualifyWithTableView(String qualifyWithTableView) { this.qualifyWithTableView = qualifyWithTableView != null ? qualifyWithTableView : "Always"; }
        public String getQualifyWithTableAlias() { return qualifyWithTableAlias; }
        public void setQualifyWithTableAlias(String qualifyWithTableAlias) { this.qualifyWithTableAlias = qualifyWithTableAlias != null ? qualifyWithTableAlias : "Always"; }
        public String getQualifyInBasicCompletion() { return qualifyInBasicCompletion; }
        public void setQualifyInBasicCompletion(String qualifyInBasicCompletion) { this.qualifyInBasicCompletion = qualifyInBasicCompletion != null ? qualifyInBasicCompletion : "On collisions"; }
        public String getQualifyInJoinCompletion() { return qualifyInJoinCompletion; }
        public void setQualifyInJoinCompletion(String qualifyInJoinCompletion) { this.qualifyInJoinCompletion = qualifyInJoinCompletion != null ? qualifyInJoinCompletion : "Always"; }
        public String getQualifyInRefactoring() { return qualifyInRefactoring; }
        public void setQualifyInRefactoring(String qualifyInRefactoring) { this.qualifyInRefactoring = qualifyInRefactoring != null ? qualifyInRefactoring : "On collisions"; }
        public String getQualifyInLiveTemplates() { return qualifyInLiveTemplates; }
        public void setQualifyInLiveTemplates(String qualifyInLiveTemplates) { this.qualifyInLiveTemplates = qualifyInLiveTemplates != null ? qualifyInLiveTemplates : "On collisions"; }
        public String getQualifyInDragDrop() { return qualifyInDragDrop; }
        public void setQualifyInDragDrop(String qualifyInDragDrop) { this.qualifyInDragDrop = qualifyInDragDrop != null ? qualifyInDragDrop : "On collisions"; }
        public boolean isJoinUseAliases() { return joinUseAliases; }
        public void setJoinUseAliases(boolean joinUseAliases) { this.joinUseAliases = joinUseAliases; }
        public boolean isJoinInvertOperands() { return joinInvertOperands; }
        public void setJoinInvertOperands(boolean joinInvertOperands) { this.joinInvertOperands = joinInvertOperands; }
        public boolean isJoinSuggestNonStrictFk() { return joinSuggestNonStrictFk; }
        public void setJoinSuggestNonStrictFk(boolean joinSuggestNonStrictFk) { this.joinSuggestNonStrictFk = joinSuggestNonStrictFk; }
        public boolean isTableAliasesAutoAdd() { return tableAliasesAutoAdd; }
        public void setTableAliasesAutoAdd(boolean tableAliasesAutoAdd) { this.tableAliasesAutoAdd = tableAliasesAutoAdd; }
        public boolean isTableAliasesSuggest() { return tableAliasesSuggest; }
        public void setTableAliasesSuggest(boolean tableAliasesSuggest) { this.tableAliasesSuggest = tableAliasesSuggest; }
        public List<TableAliasConfig> getCustomTableAliases() { return customTableAliases; }
        public void setCustomTableAliases(List<TableAliasConfig> customTableAliases) { this.customTableAliases = customTableAliases != null ? customTableAliases : new ArrayList<>(); }
        public String getAdditionalAcceptCharacters() { return additionalAcceptCharacters; }
        public void setAdditionalAcceptCharacters(String additionalAcceptCharacters) { this.additionalAcceptCharacters = additionalAcceptCharacters != null ? additionalAcceptCharacters : ""; }

        // Code Folding getters & setters
        public boolean isShowCodeFoldingArrows() { return showCodeFoldingArrows; }
        public void setShowCodeFoldingArrows(boolean showCodeFoldingArrows) { this.showCodeFoldingArrows = showCodeFoldingArrows; }
        public String getShowCodeFoldingArrowsMode() { return showCodeFoldingArrowsMode; }
        public void setShowCodeFoldingArrowsMode(String showCodeFoldingArrowsMode) { this.showCodeFoldingArrowsMode = showCodeFoldingArrowsMode != null ? showCodeFoldingArrowsMode : "On mouse hover"; }
        public boolean isShowBottomArrows() { return showBottomArrows; }
        public void setShowBottomArrows(boolean showBottomArrows) { this.showBottomArrows = showBottomArrows; }
        public boolean isFoldFileHeader() { return foldFileHeader; }
        public void setFoldFileHeader(boolean foldFileHeader) { this.foldFileHeader = foldFileHeader; }
        public boolean isFoldImports() { return foldImports; }
        public void setFoldImports(boolean foldImports) { this.foldImports = foldImports; }
        public boolean isFoldDocComments() { return foldDocComments; }
        public void setFoldDocComments(boolean foldDocComments) { this.foldDocComments = foldDocComments; }
        public boolean isFoldMethodBodies() { return foldMethodBodies; }
        public void setFoldMethodBodies(boolean foldMethodBodies) { this.foldMethodBodies = foldMethodBodies; }
        public boolean isFoldCustomRegions() { return foldCustomRegions; }
        public void setFoldCustomRegions(boolean foldCustomRegions) { this.foldCustomRegions = foldCustomRegions; }
        public boolean isFoldMarkdownFrontMatter() { return foldMarkdownFrontMatter; }
        public void setFoldMarkdownFrontMatter(boolean foldMarkdownFrontMatter) { this.foldMarkdownFrontMatter = foldMarkdownFrontMatter; }
        public boolean isFoldMarkdownLinks() { return foldMarkdownLinks; }
        public void setFoldMarkdownLinks(boolean foldMarkdownLinks) { this.foldMarkdownLinks = foldMarkdownLinks; }
        public boolean isFoldMarkdownTables() { return foldMarkdownTables; }
        public void setFoldMarkdownTables(boolean foldMarkdownTables) { this.foldMarkdownTables = foldMarkdownTables; }
        public boolean isFoldMarkdownCodeFences() { return foldMarkdownCodeFences; }
        public void setFoldMarkdownCodeFences(boolean foldMarkdownCodeFences) { this.foldMarkdownCodeFences = foldMarkdownCodeFences; }
        public boolean isFoldMarkdownTableOfContents() { return foldMarkdownTableOfContents; }
        public void setFoldMarkdownTableOfContents(boolean foldMarkdownTableOfContents) { this.foldMarkdownTableOfContents = foldMarkdownTableOfContents; }
        public boolean isFoldSqlUnderscoresInNumericLiterals() { return foldSqlUnderscoresInNumericLiterals; }
        public void setFoldSqlUnderscoresInNumericLiterals(boolean foldSqlUnderscoresInNumericLiterals) { this.foldSqlUnderscoresInNumericLiterals = foldSqlUnderscoresInNumericLiterals; }
        public boolean isFoldXmlTags() { return foldXmlTags; }
        public void setFoldXmlTags(boolean foldXmlTags) { this.foldXmlTags = foldXmlTags; }
        public boolean isFoldHtmlStyleAttribute() { return foldHtmlStyleAttribute; }
        public void setFoldHtmlStyleAttribute(boolean foldHtmlStyleAttribute) { this.foldHtmlStyleAttribute = foldHtmlStyleAttribute; }
        public boolean isFoldXmlEntities() { return foldXmlEntities; }
        public void setFoldXmlEntities(boolean foldXmlEntities) { this.foldXmlEntities = foldXmlEntities; }
        public boolean isFoldDataUris() { return foldDataUris; }
        public void setFoldDataUris(boolean foldDataUris) { this.foldDataUris = foldDataUris; }

        // Editor Tabs getters & setters
        public String getEditorTabPlacement() { return editorTabPlacement; }
        public void setEditorTabPlacement(String editorTabPlacement) { this.editorTabPlacement = editorTabPlacement != null ? editorTabPlacement : "Top"; }
        public String getEditorTabsShowMode() { return editorTabsShowMode; }
        public void setEditorTabsShowMode(String editorTabsShowMode) { this.editorTabsShowMode = editorTabsShowMode != null ? editorTabsShowMode : "One row"; }
        public String getEditorTabsOverflowMode() { return editorTabsOverflowMode; }
        public void setEditorTabsOverflowMode(String editorTabsOverflowMode) { this.editorTabsOverflowMode = editorTabsOverflowMode != null ? editorTabsOverflowMode : "Scroll the tabs panel"; }
        public boolean isShowPinnedTabsInSeparateRow() { return showPinnedTabsInSeparateRow; }
        public void setShowPinnedTabsInSeparateRow(boolean showPinnedTabsInSeparateRow) { this.showPinnedTabsInSeparateRow = showPinnedTabsInSeparateRow; }
        public boolean isEditorTabsShowFileIcon() { return editorTabsShowFileIcon; }
        public void setEditorTabsShowFileIcon(boolean editorTabsShowFileIcon) { this.editorTabsShowFileIcon = editorTabsShowFileIcon; }
        public boolean isEditorTabsShowFileExtension() { return editorTabsShowFileExtension; }
        public void setEditorTabsShowFileExtension(boolean editorTabsShowFileExtension) { this.editorTabsShowFileExtension = editorTabsShowFileExtension; }
        public boolean isEditorTabsShowDirectoryForNonUnique() { return editorTabsShowDirectoryForNonUnique; }
        public void setEditorTabsShowDirectoryForNonUnique(boolean editorTabsShowDirectoryForNonUnique) { this.editorTabsShowDirectoryForNonUnique = editorTabsShowDirectoryForNonUnique; }
        public boolean isEditorTabsMarkModified() { return editorTabsMarkModified; }
        public void setEditorTabsMarkModified(boolean editorTabsMarkModified) { this.editorTabsMarkModified = editorTabsMarkModified; }
        public boolean isEditorTabsShowFullPathOnHover() { return editorTabsShowFullPathOnHover; }
        public void setEditorTabsShowFullPathOnHover(boolean editorTabsShowFullPathOnHover) { this.editorTabsShowFullPathOnHover = editorTabsShowFullPathOnHover; }
        public String getEditorTabsCloseButtonPosition() { return editorTabsCloseButtonPosition; }
        public void setEditorTabsCloseButtonPosition(String editorTabsCloseButtonPosition) { this.editorTabsCloseButtonPosition = editorTabsCloseButtonPosition != null ? editorTabsCloseButtonPosition : "Right"; }
        public boolean isEditorTabsSortAlphabetically() { return editorTabsSortAlphabetically; }
        public void setEditorTabsSortAlphabetically(boolean editorTabsSortAlphabetically) { this.editorTabsSortAlphabetically = editorTabsSortAlphabetically; }
        public boolean isEditorTabsOpenNewAtEnd() { return editorTabsOpenNewAtEnd; }
        public void setEditorTabsOpenNewAtEnd(boolean editorTabsOpenNewAtEnd) { this.editorTabsOpenNewAtEnd = editorTabsOpenNewAtEnd; }
        public boolean isEditorTabsEnablePreviewTab() { return editorTabsEnablePreviewTab; }
        public void setEditorTabsEnablePreviewTab(boolean editorTabsEnablePreviewTab) { this.editorTabsEnablePreviewTab = editorTabsEnablePreviewTab; }
        public int getEditorTabsLimit() { return editorTabsLimit; }
        public void setEditorTabsLimit(int editorTabsLimit) { this.editorTabsLimit = editorTabsLimit > 0 ? editorTabsLimit : 30; }
        public String getEditorTabsExceedLimitPolicy() { return editorTabsExceedLimitPolicy; }
        public void setEditorTabsExceedLimitPolicy(String editorTabsExceedLimitPolicy) { this.editorTabsExceedLimitPolicy = editorTabsExceedLimitPolicy != null ? editorTabsExceedLimitPolicy : "Close unused"; }
        public String getEditorTabsCloseActivatePolicy() { return editorTabsCloseActivatePolicy; }
        public void setEditorTabsCloseActivatePolicy(String editorTabsCloseActivatePolicy) { this.editorTabsCloseActivatePolicy = editorTabsCloseActivatePolicy != null ? editorTabsCloseActivatePolicy : "The tab on the left"; }
        public boolean isEditorTabsAlwaysShowQualifiedNames() { return editorTabsAlwaysShowQualifiedNames; }
        public void setEditorTabsAlwaysShowQualifiedNames(boolean editorTabsAlwaysShowQualifiedNames) { this.editorTabsAlwaysShowQualifiedNames = editorTabsAlwaysShowQualifiedNames; }
        public boolean isEditorTabsShortenNames() { return editorTabsShortenNames; }
        public void setEditorTabsShortenNames(boolean editorTabsShortenNames) { this.editorTabsShortenNames = editorTabsShortenNames; }

        // Output Console getters & setters
        public boolean isOutputConsoleUseSoftWraps() { return outputConsoleUseSoftWraps; }
        public void setOutputConsoleUseSoftWraps(boolean outputConsoleUseSoftWraps) { this.outputConsoleUseSoftWraps = outputConsoleUseSoftWraps; }
        public int getOutputConsoleHistorySize() { return outputConsoleHistorySize; }
        public void setOutputConsoleHistorySize(int outputConsoleHistorySize) { this.outputConsoleHistorySize = outputConsoleHistorySize >= 0 ? outputConsoleHistorySize : 300; }
        public boolean isOutputConsoleOverrideCycleBuffer() { return outputConsoleOverrideCycleBuffer; }
        public void setOutputConsoleOverrideCycleBuffer(boolean outputConsoleOverrideCycleBuffer) { this.outputConsoleOverrideCycleBuffer = outputConsoleOverrideCycleBuffer; }
        public int getOutputConsoleCycleBufferSizeKb() { return outputConsoleCycleBufferSizeKb; }
        public void setOutputConsoleCycleBufferSizeKb(int outputConsoleCycleBufferSizeKb) { this.outputConsoleCycleBufferSizeKb = outputConsoleCycleBufferSizeKb > 0 ? outputConsoleCycleBufferSizeKb : 1024; }
        public String getOutputConsoleDefaultEncoding() { return outputConsoleDefaultEncoding; }
        public void setOutputConsoleDefaultEncoding(String outputConsoleDefaultEncoding) { this.outputConsoleDefaultEncoding = outputConsoleDefaultEncoding != null ? outputConsoleDefaultEncoding : "<System Default: UTF-8>"; }
        public List<String> getOutputConsoleFoldingPatterns() {
            if (outputConsoleFoldingPatterns == null) outputConsoleFoldingPatterns = new ArrayList<>();
            return outputConsoleFoldingPatterns;
        }
        public void setOutputConsoleFoldingPatterns(List<String> outputConsoleFoldingPatterns) {
            this.outputConsoleFoldingPatterns = outputConsoleFoldingPatterns != null ? outputConsoleFoldingPatterns : new ArrayList<>();
        }
        public List<String> getOutputConsoleFoldingExceptions() {
            if (outputConsoleFoldingExceptions == null) outputConsoleFoldingExceptions = new ArrayList<>();
            return outputConsoleFoldingExceptions;
        }
        public void setOutputConsoleFoldingExceptions(List<String> outputConsoleFoldingExceptions) {
            this.outputConsoleFoldingExceptions = outputConsoleFoldingExceptions != null ? outputConsoleFoldingExceptions : new ArrayList<>();
        }

        // Gutter Icons getters & setters
        public boolean isShowGutterIcons() { return showGutterIcons; }
        public void setShowGutterIcons(boolean showGutterIcons) { this.showGutterIcons = showGutterIcons; }
        public boolean isGutterColorPreview() { return gutterColorPreview; }
        public void setGutterColorPreview(boolean gutterColorPreview) { this.gutterColorPreview = gutterColorPreview; }
        public boolean isGutterDocComments() { return gutterDocComments; }
        public void setGutterDocComments(boolean gutterDocComments) { this.gutterDocComments = gutterDocComments; }
        public boolean isGutterRunLineMarker() { return gutterRunLineMarker; }
        public void setGutterRunLineMarker(boolean gutterRunLineMarker) { this.gutterRunLineMarker = gutterRunLineMarker; }
        public boolean isGutterRecursiveCall() { return gutterRecursiveCall; }
        public void setGutterRecursiveCall(boolean gutterRecursiveCall) { this.gutterRecursiveCall = gutterRecursiveCall; }
        public boolean isGutterVcsIgnoredDirectories() { return gutterVcsIgnoredDirectories; }
        public void setGutterVcsIgnoredDirectories(boolean gutterVcsIgnoredDirectories) { this.gutterVcsIgnoredDirectories = gutterVcsIgnoredDirectories; }
        public boolean isGutterConfigureHtmlImage() { return gutterConfigureHtmlImage; }
        public void setGutterConfigureHtmlImage(boolean gutterConfigureHtmlImage) { this.gutterConfigureHtmlImage = gutterConfigureHtmlImage; }
        public boolean isGutterConfigureMarkdownImage() { return gutterConfigureMarkdownImage; }
        public void setGutterConfigureMarkdownImage(boolean gutterConfigureMarkdownImage) { this.gutterConfigureMarkdownImage = gutterConfigureMarkdownImage; }
        public boolean isGutterInstallPlantUml() { return gutterInstallPlantUml; }
        public void setGutterInstallPlantUml(boolean gutterInstallPlantUml) { this.gutterInstallPlantUml = gutterInstallPlantUml; }

        // Inline Completion getters & setters
        public boolean isInlineCompletionEnabled() { return inlineCompletionEnabled; }
        public void setInlineCompletionEnabled(boolean inlineCompletionEnabled) { this.inlineCompletionEnabled = inlineCompletionEnabled; }
        public boolean isInlineAutoOnTyping() { return inlineAutoOnTyping; }
        public void setInlineAutoOnTyping(boolean inlineAutoOnTyping) { this.inlineAutoOnTyping = inlineAutoOnTyping; }
        public boolean isInlineMultilineSuggestions() { return inlineMultilineSuggestions; }
        public void setInlineMultilineSuggestions(boolean inlineMultilineSuggestions) { this.inlineMultilineSuggestions = inlineMultilineSuggestions; }
        public boolean isInlineSyncWithPopup() { return inlineSyncWithPopup; }
        public void setInlineSyncWithPopup(boolean inlineSyncWithPopup) { this.inlineSyncWithPopup = inlineSyncWithPopup; }

        // Smart Keys getters & setters
        public boolean isSmartKeysHomeMovesCaret() { return smartKeysHomeMovesCaret; }
        public void setSmartKeysHomeMovesCaret(boolean val) { this.smartKeysHomeMovesCaret = val; }
        public boolean isSmartKeysEndBlankLineMovesCaret() { return smartKeysEndBlankLineMovesCaret; }
        public void setSmartKeysEndBlankLineMovesCaret(boolean val) { this.smartKeysEndBlankLineMovesCaret = val; }
        public boolean isSmartKeysInsertPairedBrackets() { return smartKeysInsertPairedBrackets; }
        public void setSmartKeysInsertPairedBrackets(boolean val) { this.smartKeysInsertPairedBrackets = val; }
        public boolean isSmartKeysInsertPairQuote() { return smartKeysInsertPairQuote; }
        public void setSmartKeysInsertPairQuote(boolean val) { this.smartKeysInsertPairQuote = val; }
        public boolean isSmartKeysReformatBlockOnBrace() { return smartKeysReformatBlockOnBrace; }
        public void setSmartKeysReformatBlockOnBrace(boolean val) { this.smartKeysReformatBlockOnBrace = val; }
        public boolean isSmartKeysUseCamelHumps() { return smartKeysUseCamelHumps; }
        public void setSmartKeysUseCamelHumps(boolean val) { this.smartKeysUseCamelHumps = val; }
        public boolean isSmartKeysHonorCamelHumpsOnDoubleClick() { return smartKeysHonorCamelHumpsOnDoubleClick; }
        public void setSmartKeysHonorCamelHumpsOnDoubleClick(boolean val) { this.smartKeysHonorCamelHumpsOnDoubleClick = val; }
        public boolean isSmartKeysSurroundSelectionOnQuoteOrBrace() { return smartKeysSurroundSelectionOnQuoteOrBrace; }
        public void setSmartKeysSurroundSelectionOnQuoteOrBrace(boolean val) { this.smartKeysSurroundSelectionOnQuoteOrBrace = val; }
        public boolean isSmartKeysMultiCaretsOnDoubleModifier() { return smartKeysMultiCaretsOnDoubleModifier; }
        public void setSmartKeysMultiCaretsOnDoubleModifier(boolean val) { this.smartKeysMultiCaretsOnDoubleModifier = val; }
        public boolean isSmartKeysJumpOutsideBracketWithTab() { return smartKeysJumpOutsideBracketWithTab; }
        public void setSmartKeysJumpOutsideBracketWithTab(boolean val) { this.smartKeysJumpOutsideBracketWithTab = val; }
        public boolean isSmartKeysEnterSmartIndent() { return smartKeysEnterSmartIndent; }
        public void setSmartKeysEnterSmartIndent(boolean val) { this.smartKeysEnterSmartIndent = val; }
        public boolean isSmartKeysEnterInsertPairBrace() { return smartKeysEnterInsertPairBrace; }
        public void setSmartKeysEnterInsertPairBrace(boolean val) { this.smartKeysEnterInsertPairBrace = val; }
        public boolean isSmartKeysEnterCloseBlockComment() { return smartKeysEnterCloseBlockComment; }
        public void setSmartKeysEnterCloseBlockComment(boolean val) { this.smartKeysEnterCloseBlockComment = val; }
        public String getSmartKeysUnindentOnBackspace() { return smartKeysUnindentOnBackspace; }
        public void setSmartKeysUnindentOnBackspace(String val) { this.smartKeysUnindentOnBackspace = val != null ? val : "To proper indent position"; }
        public String getSmartKeysReformatOnPaste() { return smartKeysReformatOnPaste; }
        public void setSmartKeysReformatOnPaste(String val) { this.smartKeysReformatOnPaste = val != null ? val : "None"; }
        public boolean isSmartKeysReformatRemoveCustomLineBreaks() { return smartKeysReformatRemoveCustomLineBreaks; }
        public void setSmartKeysReformatRemoveCustomLineBreaks(boolean val) { this.smartKeysReformatRemoveCustomLineBreaks = val; }

        // Smart Keys > SQL
        public boolean isSmartKeysSqlInsertStringConcatOnEnter() { return smartKeysSqlInsertStringConcatOnEnter; }
        public void setSmartKeysSqlInsertStringConcatOnEnter(boolean val) { this.smartKeysSqlInsertStringConcatOnEnter = val; }
        public boolean isSmartKeysSqlCloseCodeBlocksOnEnter() { return smartKeysSqlCloseCodeBlocksOnEnter; }
        public void setSmartKeysSqlCloseCodeBlocksOnEnter(boolean val) { this.smartKeysSqlCloseCodeBlocksOnEnter = val; }

        // Smart Keys > Markdown
        public boolean isSmartKeysMarkdownReformatTable() { return smartKeysMarkdownReformatTable; }
        public void setSmartKeysMarkdownReformatTable(boolean val) { this.smartKeysMarkdownReformatTable = val; }
        public boolean isSmartKeysMarkdownInsertHtmlLineBreakInTable() { return smartKeysMarkdownInsertHtmlLineBreakInTable; }
        public void setSmartKeysMarkdownInsertHtmlLineBreakInTable(boolean val) { this.smartKeysMarkdownInsertHtmlLineBreakInTable = val; }
        public boolean isSmartKeysMarkdownShiftEnterNewTableRow() { return smartKeysMarkdownShiftEnterNewTableRow; }
        public void setSmartKeysMarkdownShiftEnterNewTableRow(boolean val) { this.smartKeysMarkdownShiftEnterNewTableRow = val; }
        public boolean isSmartKeysMarkdownTabNavigateTable() { return smartKeysMarkdownTabNavigateTable; }
        public void setSmartKeysMarkdownTabNavigateTable(boolean val) { this.smartKeysMarkdownTabNavigateTable = val; }
        public boolean isSmartKeysMarkdownAdjustListIndent() { return smartKeysMarkdownAdjustListIndent; }
        public void setSmartKeysMarkdownAdjustListIndent(boolean val) { this.smartKeysMarkdownAdjustListIndent = val; }
        public boolean isSmartKeysMarkdownSmartEnterBackspace() { return smartKeysMarkdownSmartEnterBackspace; }
        public void setSmartKeysMarkdownSmartEnterBackspace(boolean val) { this.smartKeysMarkdownSmartEnterBackspace = val; }
        public boolean isSmartKeysMarkdownRenumberList() { return smartKeysMarkdownRenumberList; }
        public void setSmartKeysMarkdownRenumberList(boolean val) { this.smartKeysMarkdownRenumberList = val; }
        public String getSmartKeysMarkdownListNumerating() { return smartKeysMarkdownListNumerating; }
        public void setSmartKeysMarkdownListNumerating(String val) { this.smartKeysMarkdownListNumerating = val != null ? val : "Sequentially"; }
        public boolean isSmartKeysMarkdownInsertLinksOnDrop() { return smartKeysMarkdownInsertLinksOnDrop; }
        public void setSmartKeysMarkdownInsertLinksOnDrop(boolean val) { this.smartKeysMarkdownInsertLinksOnDrop = val; }

        // Smart Keys > JSON
        public boolean isSmartKeysJsonInsertMissingCommaOnEnter() { return smartKeysJsonInsertMissingCommaOnEnter; }
        public void setSmartKeysJsonInsertMissingCommaOnEnter(boolean val) { this.smartKeysJsonInsertMissingCommaOnEnter = val; }
        public boolean isSmartKeysJsonInsertMissingCommaAfterMatching() { return smartKeysJsonInsertMissingCommaAfterMatching; }
        public void setSmartKeysJsonInsertMissingCommaAfterMatching(boolean val) { this.smartKeysJsonInsertMissingCommaAfterMatching = val; }
        public boolean isSmartKeysJsonManageCommasOnPaste() { return smartKeysJsonManageCommasOnPaste; }
        public void setSmartKeysJsonManageCommasOnPaste(boolean val) { this.smartKeysJsonManageCommasOnPaste = val; }
        public boolean isSmartKeysJsonEscapeTextOnPaste() { return smartKeysJsonEscapeTextOnPaste; }
        public void setSmartKeysJsonEscapeTextOnPaste(boolean val) { this.smartKeysJsonEscapeTextOnPaste = val; }
        public boolean isSmartKeysJsonAddQuotesToPropertyNames() { return smartKeysJsonAddQuotesToPropertyNames; }
        public void setSmartKeysJsonAddQuotesToPropertyNames(boolean val) { this.smartKeysJsonAddQuotesToPropertyNames = val; }
        public boolean isSmartKeysJsonAddWhitespaceAfterColon() { return smartKeysJsonAddWhitespaceAfterColon; }
        public void setSmartKeysJsonAddWhitespaceAfterColon(boolean val) { this.smartKeysJsonAddWhitespaceAfterColon = val; }
        public boolean isSmartKeysJsonMoveColonAfterPropertyName() { return smartKeysJsonMoveColonAfterPropertyName; }
        public void setSmartKeysJsonMoveColonAfterPropertyName(boolean val) { this.smartKeysJsonMoveColonAfterPropertyName = val; }
        public boolean isSmartKeysJsonMoveCommaAfterPropertyValue() { return smartKeysJsonMoveCommaAfterPropertyValue; }
        public void setSmartKeysJsonMoveCommaAfterPropertyValue(boolean val) { this.smartKeysJsonMoveCommaAfterPropertyValue = val; }

        // Smart Keys > HTML/CSS
        public boolean isSmartKeysHtmlInsertClosingTag() { return smartKeysHtmlInsertClosingTag; }
        public void setSmartKeysHtmlInsertClosingTag(boolean val) { this.smartKeysHtmlInsertClosingTag = val; }
        public boolean isSmartKeysHtmlInsertRequiredAttributes() { return smartKeysHtmlInsertRequiredAttributes; }
        public void setSmartKeysHtmlInsertRequiredAttributes(boolean val) { this.smartKeysHtmlInsertRequiredAttributes = val; }
        public boolean isSmartKeysHtmlInsertRequiredSubtags() { return smartKeysHtmlInsertRequiredSubtags; }
        public void setSmartKeysHtmlInsertRequiredSubtags(boolean val) { this.smartKeysHtmlInsertRequiredSubtags = val; }
        public boolean isSmartKeysHtmlStartAttribute() { return smartKeysHtmlStartAttribute; }
        public void setSmartKeysHtmlStartAttribute(boolean val) { this.smartKeysHtmlStartAttribute = val; }
        public boolean isSmartKeysHtmlAddQuotesForAttribute() { return smartKeysHtmlAddQuotesForAttribute; }
        public void setSmartKeysHtmlAddQuotesForAttribute(boolean val) { this.smartKeysHtmlAddQuotesForAttribute = val; }
        public boolean isSmartKeysHtmlAutoCloseTag() { return smartKeysHtmlAutoCloseTag; }
        public void setSmartKeysHtmlAutoCloseTag(boolean val) { this.smartKeysHtmlAutoCloseTag = val; }
        public boolean isSmartKeysHtmlSimultaneousTagEditing() { return smartKeysHtmlSimultaneousTagEditing; }
        public void setSmartKeysHtmlSimultaneousTagEditing(boolean val) { this.smartKeysHtmlSimultaneousTagEditing = val; }
        public boolean isSmartKeysCssSelectWholeCssIdentifiers() { return smartKeysCssSelectWholeCssIdentifiers; }
        public void setSmartKeysCssSelectWholeCssIdentifiers(boolean val) { this.smartKeysCssSelectWholeCssIdentifiers = val; }

        // Postfix Completion getters & setters
        public boolean isPostfixCompletionEnabled() { return postfixCompletionEnabled; }
        public void setPostfixCompletionEnabled(boolean val) { this.postfixCompletionEnabled = val; }
        public String getPostfixCompletionExpandWith() { return postfixCompletionExpandWith; }
        public void setPostfixCompletionExpandWith(String val) { this.postfixCompletionExpandWith = val != null ? val : "Tab"; }
        public List<PostfixTemplateConfig> getPostfixTemplates() {
            if (postfixTemplates == null || postfixTemplates.isEmpty()) {
                postfixTemplates = defaultPostfixTemplates();
            }
            return postfixTemplates;
        }
        public void setPostfixTemplates(List<PostfixTemplateConfig> postfixTemplates) {
            this.postfixTemplates = (postfixTemplates == null || postfixTemplates.isEmpty())
                    ? defaultPostfixTemplates() : postfixTemplates;
        }
    }

    public static class UserParameterPattern {
        private String pattern = ":name";
        private String scope = "everywhere";
        private String languages = "All languages";
        private boolean inScripts = true;
        private boolean inLiterals = false;
        private boolean enabled = true;

        public UserParameterPattern() {}

        public UserParameterPattern(String pattern, String scope, String languages, boolean inScripts, boolean inLiterals) {
            this.pattern = pattern;
            this.scope = scope;
            this.languages = languages;
            this.inScripts = inScripts;
            this.inLiterals = inLiterals;
            this.enabled = true;
        }

        public UserParameterPattern copy() {
            UserParameterPattern p = new UserParameterPattern(pattern, scope, languages, inScripts, inLiterals);
            p.enabled = this.enabled;
            return p;
        }

        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public String getLanguages() { return languages; }
        public void setLanguages(String languages) { this.languages = languages; }
        public boolean isInScripts() { return inScripts; }
        public void setInScripts(boolean inScripts) { this.inScripts = inScripts; }
        public boolean isInLiterals() { return inLiterals; }
        public void setInLiterals(boolean inLiterals) { this.inLiterals = inLiterals; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class ExecuteActionConfig {
        private String name = "Execute";
        private String shortcut = "Ctrl+Enter";
        private String whenCaretInside = "Ask what to execute";
        private String whenCaretOutside = "Nothing";
        private String forSelection = "Exactly as separate statements";
        private boolean openResultsInNewTab = false;

        public ExecuteActionConfig() {}

        public ExecuteActionConfig(String name, String shortcut, String whenCaretInside,
                                   String whenCaretOutside, String forSelection, boolean openResultsInNewTab) {
            this.name = name;
            this.shortcut = shortcut;
            this.whenCaretInside = whenCaretInside;
            this.whenCaretOutside = whenCaretOutside;
            this.forSelection = forSelection;
            this.openResultsInNewTab = openResultsInNewTab;
        }

        public ExecuteActionConfig copy() {
            return new ExecuteActionConfig(name, shortcut, whenCaretInside, whenCaretOutside, forSelection, openResultsInNewTab);
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getShortcut() { return shortcut; }
        public void setShortcut(String shortcut) { this.shortcut = shortcut; }
        public String getWhenCaretInside() { return whenCaretInside; }
        public void setWhenCaretInside(String whenCaretInside) { this.whenCaretInside = whenCaretInside; }
        public String getWhenCaretOutside() { return whenCaretOutside; }
        public void setWhenCaretOutside(String whenCaretOutside) { this.whenCaretOutside = whenCaretOutside; }
        public String getForSelection() { return forSelection; }
        public void setForSelection(String forSelection) { this.forSelection = forSelection; }
        public boolean isOpenResultsInNewTab() { return openResultsInNewTab; }
        public void setOpenResultsInNewTab(boolean openResultsInNewTab) { this.openResultsInNewTab = openResultsInNewTab; }
    }

    public static class QuotationRule {
        private String leftQuote = "\"";
        private String rightQuote = "\"";
        private String escapeMode = "duplicate";

        public QuotationRule() {}

        public QuotationRule(String leftQuote, String rightQuote, String escapeMode) {
            this.leftQuote = leftQuote;
            this.rightQuote = rightQuote;
            this.escapeMode = escapeMode;
        }

        public QuotationRule copy() {
            return new QuotationRule(leftQuote, rightQuote, escapeMode);
        }

        public String getLeftQuote() { return leftQuote; }
        public void setLeftQuote(String leftQuote) { this.leftQuote = leftQuote; }
        public String getRightQuote() { return rightQuote; }
        public void setRightQuote(String rightQuote) { this.rightQuote = rightQuote; }
        public String getEscapeMode() { return escapeMode; }
        public void setEscapeMode(String escapeMode) { this.escapeMode = escapeMode; }

        @Override
        public String toString() {
            return (leftQuote != null ? leftQuote : "") + "   " + (rightQuote != null ? rightQuote : "")
                    + "   Escape: " + (escapeMode != null ? escapeMode : "duplicate");
        }
    }

    public static class CsvFormatConfig {
        private String name = "CSV";
        private String valueSeparator = "Comma";
        private String rowSeparator = "Newline";
        private String nullValueText = "Empty string";
        private String rowPrefix = "";
        private String rowSuffix = "";
        private List<QuotationRule> quotationRules = defaultQuotationRules();
        private String quoteValues = "When needed";
        private boolean trimWhitespaces = false;
        private boolean firstRowIsHeader = false;
        private boolean firstColumnIsHeader = false;

        public CsvFormatConfig() {}

        public CsvFormatConfig(String name, String valueSeparator, String rowSeparator, String nullValueText,
                               List<QuotationRule> quotationRules, String quoteValues,
                               boolean trimWhitespaces, boolean firstRowIsHeader, boolean firstColumnIsHeader) {
            this.name = name;
            this.valueSeparator = valueSeparator;
            this.rowSeparator = rowSeparator;
            this.nullValueText = nullValueText;
            this.quotationRules = quotationRules != null ? new ArrayList<>(quotationRules) : defaultQuotationRules();
            this.quoteValues = quoteValues;
            this.trimWhitespaces = trimWhitespaces;
            this.firstRowIsHeader = firstRowIsHeader;
            this.firstColumnIsHeader = firstColumnIsHeader;
        }

        public static List<QuotationRule> defaultQuotationRules() {
            List<QuotationRule> list = new ArrayList<>();
            list.add(new QuotationRule("\"", "\"", "duplicate"));
            list.add(new QuotationRule("'", "'", "duplicate"));
            return list;
        }

        public CsvFormatConfig copy() {
            CsvFormatConfig copy = new CsvFormatConfig();
            copy.name = this.name;
            copy.valueSeparator = this.valueSeparator;
            copy.rowSeparator = this.rowSeparator;
            copy.nullValueText = this.nullValueText;
            copy.rowPrefix = this.rowPrefix;
            copy.rowSuffix = this.rowSuffix;
            copy.quoteValues = this.quoteValues;
            copy.trimWhitespaces = this.trimWhitespaces;
            copy.firstRowIsHeader = this.firstRowIsHeader;
            copy.firstColumnIsHeader = this.firstColumnIsHeader;
            if (this.quotationRules != null) {
                copy.quotationRules = new ArrayList<>();
                for (QuotationRule r : this.quotationRules) {
                    copy.quotationRules.add(r.copy());
                }
            }
            return copy;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValueSeparator() { return valueSeparator; }
        public void setValueSeparator(String valueSeparator) { this.valueSeparator = valueSeparator; }
        public String getRowSeparator() { return rowSeparator; }
        public void setRowSeparator(String rowSeparator) { this.rowSeparator = rowSeparator; }
        public String getNullValueText() { return nullValueText; }
        public void setNullValueText(String nullValueText) { this.nullValueText = nullValueText; }
        public String getRowPrefix() { return rowPrefix; }
        public void setRowPrefix(String rowPrefix) { this.rowPrefix = rowPrefix; }
        public String getRowSuffix() { return rowSuffix; }
        public void setRowSuffix(String rowSuffix) { this.rowSuffix = rowSuffix; }
        public List<QuotationRule> getQuotationRules() {
            if (quotationRules == null) quotationRules = defaultQuotationRules();
            return quotationRules;
        }
        public void setQuotationRules(List<QuotationRule> quotationRules) {
            this.quotationRules = quotationRules != null ? quotationRules : defaultQuotationRules();
        }
        public String getQuoteValues() { return quoteValues; }
        public void setQuoteValues(String quoteValues) { this.quoteValues = quoteValues; }
        public boolean isTrimWhitespaces() { return trimWhitespaces; }
        public void setTrimWhitespaces(boolean trimWhitespaces) { this.trimWhitespaces = trimWhitespaces; }
        public boolean isFirstRowIsHeader() { return firstRowIsHeader; }
        public void setFirstRowIsHeader(boolean firstRowIsHeader) { this.firstRowIsHeader = firstRowIsHeader; }
        public boolean isFirstColumnIsHeader() { return firstColumnIsHeader; }
        public void setFirstColumnIsHeader(boolean firstColumnIsHeader) { this.firstColumnIsHeader = firstColumnIsHeader; }

        @Override
        public String toString() {
            return name != null ? name : "";
        }
    }

    public static class SqlDialectMappingConfig {
        private String path = "";
        private String dialect = "Generic SQL";

        public SqlDialectMappingConfig() {}

        public SqlDialectMappingConfig(String path, String dialect) {
            this.path = path != null ? path : "";
            this.dialect = dialect != null ? dialect : "Generic SQL";
        }

        public SqlDialectMappingConfig copy() {
            return new SqlDialectMappingConfig(path, dialect);
        }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path != null ? path : ""; }
        public String getDialect() { return dialect; }
        public void setDialect(String dialect) { this.dialect = dialect != null ? dialect : "Generic SQL"; }

        @Override
        public String toString() {
            return path + " -> " + dialect;
        }
    }

    public static class SqlResolutionScopeMappingConfig {
        private String path = "";
        private String scope = "<Default> (<Everything>)";

        public SqlResolutionScopeMappingConfig() {}

        public SqlResolutionScopeMappingConfig(String path, String scope) {
            this.path = path != null ? path : "";
            this.scope = scope != null ? scope : "<Default> (<Everything>)";
        }

        public SqlResolutionScopeMappingConfig copy() {
            return new SqlResolutionScopeMappingConfig(path, scope);
        }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path != null ? path : ""; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope != null ? scope : "<Default> (<Everything>)"; }

        @Override
        public String toString() {
            return path + " -> " + scope;
        }
    }

    public static class VirtualForeignKeyRule {
        private String columnPattern = "(.*)_(?i)id";
        private String targetColumnPattern = "$1\\.(?i)id";

        public VirtualForeignKeyRule() {}

        public VirtualForeignKeyRule(String columnPattern, String targetColumnPattern) {
            this.columnPattern = columnPattern != null ? columnPattern : "(.*)_(?i)id";
            this.targetColumnPattern = targetColumnPattern != null ? targetColumnPattern : "$1\\.(?i)id";
        }

        public VirtualForeignKeyRule copy() {
            return new VirtualForeignKeyRule(columnPattern, targetColumnPattern);
        }

        public String getColumnPattern() { return columnPattern; }
        public void setColumnPattern(String columnPattern) { this.columnPattern = columnPattern; }
        public String getTargetColumnPattern() { return targetColumnPattern; }
        public void setTargetColumnPattern(String targetColumnPattern) { this.targetColumnPattern = targetColumnPattern; }

        @Override
        public String toString() {
            return columnPattern + " -> " + targetColumnPattern;
        }
    }

    public static class TableAliasConfig {
        private String tableName = "";
        private String customAlias = "";

        public TableAliasConfig() {}

        public TableAliasConfig(String tableName, String customAlias) {
            this.tableName = tableName != null ? tableName : "";
            this.customAlias = customAlias != null ? customAlias : "";
        }

        public TableAliasConfig copy() {
            return new TableAliasConfig(tableName, customAlias);
        }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName != null ? tableName : ""; }

        public String getCustomAlias() { return customAlias; }
        public void setCustomAlias(String customAlias) { this.customAlias = customAlias != null ? customAlias : ""; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TableAliasConfig that = (TableAliasConfig) o;
            return Objects.equals(tableName, that.tableName) && Objects.equals(customAlias, that.customAlias);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tableName, customAlias);
        }

        @Override
        public String toString() {
            return tableName + " -> " + customAlias;
        }
    }

    public static class PostfixTemplateConfig {
        private String key = "";
        private String language = "SQL";
        private String expression = "";
        private String description = "";
        private boolean enabled = true;
        private String exampleBefore = "";
        private String exampleAfter = "";

        public PostfixTemplateConfig() {}

        public PostfixTemplateConfig(String key, String language, String expression, String description,
                                     boolean enabled, String exampleBefore, String exampleAfter) {
            this.key = key != null ? key : "";
            this.language = language != null ? language : "SQL";
            this.expression = expression != null ? expression : "";
            this.description = description != null ? description : "";
            this.enabled = enabled;
            this.exampleBefore = exampleBefore != null ? exampleBefore : "";
            this.exampleAfter = exampleAfter != null ? exampleAfter : "";
        }

        public PostfixTemplateConfig copy() {
            return new PostfixTemplateConfig(key, language, expression, description, enabled, exampleBefore, exampleAfter);
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key != null ? key : ""; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language != null ? language : "SQL"; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression != null ? expression : ""; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description != null ? description : ""; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getExampleBefore() { return exampleBefore; }
        public void setExampleBefore(String exampleBefore) { this.exampleBefore = exampleBefore != null ? exampleBefore : ""; }
        public String getExampleAfter() { return exampleAfter; }
        public void setExampleAfter(String exampleAfter) { this.exampleAfter = exampleAfter != null ? exampleAfter : ""; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostfixTemplateConfig that = (PostfixTemplateConfig) o;
            return Objects.equals(key, that.key) && Objects.equals(language, that.language);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, language);
        }

        @Override
        public String toString() {
            return key + " (" + language + ") - " + description;
        }
    }

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "settings.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static Settings cached;

    private AppSettingsStore() {}

    public static synchronized Settings load() {
        if (cached != null) return cached;
        if (Files.exists(FILE)) {
            try {
                cached = MAPPER.readValue(FILE.toFile(), Settings.class);
                return cached;
            } catch (IOException e) {
                System.err.println("Could not read settings: " + e.getMessage());
            }
        }
        cached = new Settings();
        return cached;
    }

    public static synchronized void save(Settings settings) {
        cached = settings;
        try {
            Files.createDirectories(FILE.getParent());
            MAPPER.writeValue(FILE.toFile(), settings);
        } catch (IOException e) {
            System.err.println("Could not save settings: " + e.getMessage());
        }
    }
}

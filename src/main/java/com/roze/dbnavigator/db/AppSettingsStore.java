package com.roze.dbnavigator.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * App-wide preferences (theme, editor font, etc.), persisted to
 * ~/.dbnavigator/settings.json — separate from per-connection state.
 */
public final class AppSettingsStore {

    public static String defaultKeymapPreset() {
        return Settings.defaultKeymapPreset();
    }

    public static List<String> defaultKeymapPresets() {
        return Settings.defaultKeymapPresets();
    }

    public static List<String> defaultEditorColorSchemes() {
        return Settings.defaultEditorColorSchemes();
    }

    public enum Theme { DARK, LIGHT }

    /**
     * Color scheme attribute holding visual styles for an editor token/element.
     */
    public static class ColorSchemeAttribute {
        public boolean bold;
        public boolean italic;
        public String foreground;
        public boolean foregroundEnabled;
        public String background;
        public boolean backgroundEnabled;
        public String errorStripe;
        public boolean errorStripeEnabled;
        public String effectColor;
        public boolean effectEnabled;
        public String effectType = "Underscored";
        public boolean inherit;
        public String inheritFromKey;
        public String ignoredColor;
        public boolean ignoredEnabled;
        public boolean inheritIgnored = true;

        public ColorSchemeAttribute() {}

        public ColorSchemeAttribute(boolean bold, boolean italic,
                                    String foreground, boolean foregroundEnabled,
                                    String background, boolean backgroundEnabled,
                                    String errorStripe, boolean errorStripeEnabled,
                                    String effectColor, boolean effectEnabled, String effectType,
                                    boolean inherit, String inheritFromKey) {
            this.bold = bold;
            this.italic = italic;
            this.foreground = foreground;
            this.foregroundEnabled = foregroundEnabled;
            this.background = background;
            this.backgroundEnabled = backgroundEnabled;
            this.errorStripe = errorStripe;
            this.errorStripeEnabled = errorStripeEnabled;
            this.effectColor = effectColor;
            this.effectEnabled = effectEnabled;
            this.effectType = effectType != null ? effectType : "Underscored";
            this.inherit = inherit;
            this.inheritFromKey = inheritFromKey;
        }

        public ColorSchemeAttribute(boolean bold, boolean italic,
                                    String foreground, boolean foregroundEnabled,
                                    String background, boolean backgroundEnabled,
                                    String errorStripe, boolean errorStripeEnabled,
                                    String effectColor, boolean effectEnabled, String effectType,
                                    boolean inherit, String inheritFromKey,
                                    String ignoredColor, boolean ignoredEnabled, boolean inheritIgnored) {
            this(bold, italic, foreground, foregroundEnabled, background, backgroundEnabled,
                    errorStripe, errorStripeEnabled, effectColor, effectEnabled, effectType, inherit, inheritFromKey);
            this.ignoredColor = ignoredColor;
            this.ignoredEnabled = ignoredEnabled;
            this.inheritIgnored = inheritIgnored;
        }

        public ColorSchemeAttribute copy() {
            ColorSchemeAttribute c = new ColorSchemeAttribute(bold, italic, foreground, foregroundEnabled,
                    background, backgroundEnabled, errorStripe, errorStripeEnabled,
                    effectColor, effectEnabled, effectType, inherit, inheritFromKey);
            c.ignoredColor = this.ignoredColor;
            c.ignoredEnabled = this.ignoredEnabled;
            c.inheritIgnored = this.inheritIgnored;
            return c;
        }
    }

    /** Plain data holder — Jackson needs a no-arg constructor and public fields/getters+setters. */
    public static class Settings {
        public Theme theme = Theme.DARK;
        public String editorFontFamily = "JetBrains Mono";
        public double editorFontSize = 13.0;
        public double editorLineHeight = 1.2;
        public boolean editorEnableLigatures = false;
        public String editorFontMainWeight = "Regular";
        public String editorFontBoldWeight = "Bold Recommended";
        public String editorFallbackFont = "<None>";
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
        public String keymapPreset = defaultKeymapPreset();
        public List<String> customKeymapPresets = new ArrayList<>();
        public Map<String, List<String>> customKeymapShortcuts = new LinkedHashMap<>();
        public Map<String, List<String>> removedKeymapShortcuts = new LinkedHashMap<>();
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

        // Menus and Toolbars (DataGrip Alignment)
        public List<MenuItemConfig> menusAndToolbars = defaultMenusAndToolbars();

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
        public String editorColorScheme = "Dark Theme default";
        public List<String> customColorSchemes = new ArrayList<>();
        public Map<String, Map<String, ColorSchemeAttribute>> colorSchemeOverrides = new LinkedHashMap<>();
        public boolean differentToolWindowBackground = false;

        // Editor > Color Scheme > Color Scheme Font
        public boolean useColorSchemeFontInsteadOfDefault = false;
        public String colorSchemeFontFamily = "JetBrains Mono";
        public String colorSchemeFontFallbackFamily = "<None>";
        public double colorSchemeFontSize = 13.0;
        public double colorSchemeFontLineHeight = 1.2;
        public boolean colorSchemeFontEnableLigatures = false;
        public boolean colorSchemeFontShowOnlyMonospaced = true;

        // Editor > Color Scheme > Console Font
        public boolean useConsoleFontInsteadOfDefault = false;
        public String consoleFontFamily = "JetBrains Mono";
        public String consoleFontFallbackFamily = "<None>";
        public double consoleFontSize = 13.0;
        public double consoleFontLineHeight = 1.2;
        public boolean consoleFontEnableLigatures = false;
        public boolean consoleFontShowOnlyMonospaced = true;

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

        // Editor > General > Sticky Lines (DataGrip Alignment)
        public boolean stickyLinesEnabled = true;
        public int stickyLinesMaxLines = 5;
        public boolean stickyLinesHtml = true;
        public boolean stickyLinesMarkdown = true;
        public boolean stickyLinesXhtml = true;
        public boolean stickyLinesJson = true;
        public boolean stickyLinesSql = true;
        public boolean stickyLinesXml = true;

        // Editor > Code Editing (DataGrip Alignment)
        // Highlight on Caret Movement
        public boolean codeEditingHighlightMatchedBrace = true;
        public boolean codeEditingHighlightCurrentScope = false;
        public boolean codeEditingHighlightUsages = true;
        // Quick Documentation
        public boolean codeEditingShowDocOnHover = true;
        // Refactorings
        public String codeEditingRefactoringOption = "In the editor";
        public boolean codeEditingPreselectCurrentSymbol = true;
        public boolean codeEditingShowInlineDialogForLocalVars = true;
        // Error Highlighting
        public int codeEditingErrorStripeMarkMinHeight = 2;
        public int codeEditingAutoreparseDelayMs = 300;
        public String codeEditingNextErrorAction = "The problems with the highest priority";
        // Editor Tooltips
        public int codeEditingTooltipDelayMs = 500;

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
                    "Dark Theme default",
                    "Light",
                    "Darcula",
                    "High Contrast",
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

        public static List<String> defaultNextErrorActionOptions() {
            return List.of("The problems with the highest priority", "All problems");
        }

        public static List<String> defaultRefactoringOptions() {
            return List.of("In the editor", "In modal dialogs");
        }

        public static List<String> defaultFontWeights() {
            return List.of("Thin", "ExtraLight", "Light", "Regular", "Medium", "SemiBold", "Bold", "ExtraBold");
        }

        public static List<String> defaultFontBoldWeights() {
            return List.of("Thin", "ExtraLight", "Light", "Regular", "Medium", "SemiBold", "Bold Recommended", "ExtraBold");
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

        public static List<MenuItemConfig> defaultMenusAndToolbars() {
            List<MenuItemConfig> roots = new ArrayList<>();

            // 1. Main Menu
            List<MenuItemConfig> mainMenuChildren = new ArrayList<>();

            // -------------------------------------------------------------
            // Main Menu > File (Exact match to DataGrip Images 1, 2, 3, 4)
            // -------------------------------------------------------------
            // Image 1: New -> New File
            List<MenuItemConfig> xmlChildren = new ArrayList<>();
            xmlChildren.add(MenuItemConfig.action("file.new.html", "HTML File", "FILE_CODE"));

            List<MenuItemConfig> webDevChildren = new ArrayList<>();
            webDevChildren.add(MenuItemConfig.group("file.web.dev.xml", "XML", true, xmlChildren));
            webDevChildren.add(MenuItemConfig.action("file.microservices.templates", "Microservices Templates"));
            webDevChildren.add(MenuItemConfig.action("file.from.template", "From Template"));
            webDevChildren.add(MenuItemConfig.action("file.xml.config.file", "XML Configuration File", "CODE"));

            List<MenuItemConfig> newFileChildren = new ArrayList<>();
            newFileChildren.add(MenuItemConfig.action("file.new.sqlfile", "SQL File", "FILE_CODE"));
            newFileChildren.add(MenuItemConfig.separator());
            newFileChildren.add(MenuItemConfig.action("file.new.generic.file", "File", "FILE"));
            newFileChildren.add(MenuItemConfig.action("file.new.scratch", "Scratch File", "FILE_ALT"));
            newFileChildren.add(MenuItemConfig.action("file.new.directory.package", "Directory/Package", "FOLDER"));
            newFileChildren.add(MenuItemConfig.action("file.template.separator.group", "FileTemplateSeparatorGroup"));
            newFileChildren.add(MenuItemConfig.group("file.web.dev.templates", "Web Development Templates", true, webDevChildren));

            // Image 1: New -> New (Add + Create Data Source)
            List<MenuItemConfig> addChildren = new ArrayList<>();
            addChildren.add(MenuItemConfig.action("file.new.queryfile.active", "Query File", "TERMINAL"));
            addChildren.add(MenuItemConfig.action("file.new.queryfile", "Query File\u2026", "FILE_CODE"));
            addChildren.add(MenuItemConfig.action("file.new.scratch.queryfile", "Scratch Query File", "TERMINAL"));
            addChildren.add(MenuItemConfig.separator());
            addChildren.add(MenuItemConfig.action("file.new.add.ddl.object", "Add Ddl Object", "DATABASE"));
            addChildren.add(MenuItemConfig.separator());

            List<MenuItemConfig> cloudChildren = new ArrayList<>();
            cloudChildren.add(MenuItemConfig.action("cloud.aws", "Amazon AWS", "DATABASE"));
            cloudChildren.add(MenuItemConfig.action("cloud.gcp", "Google Cloud", "DATABASE"));
            cloudChildren.add(MenuItemConfig.action("cloud.azure", "Microsoft Azure", "DATABASE"));

            List<MenuItemConfig> createDsChildren = new ArrayList<>();
            createDsChildren.add(MenuItemConfig.group("file.new.datasource.cloud", "Data Source from Cloud Provider", true, "CLOUD", cloudChildren));
            createDsChildren.add(MenuItemConfig.action("file.new.datasource.templates", "Data Source Templates", "LAYER_GROUP"));
            createDsChildren.add(MenuItemConfig.action("file.new.datasource.file", "Data Source from File/Folder", "FOLDER_OPEN"));
            createDsChildren.add(MenuItemConfig.action("file.new.datasource.url", "Data Source from URL", "LINK"));
            createDsChildren.add(MenuItemConfig.action("file.new.datasource.selection", "Add Data Source from Selection\u2026"));
            createDsChildren.add(MenuItemConfig.action("file.new.datasource.path", "Data Source in Path"));
            createDsChildren.add(MenuItemConfig.action("file.new.datasource.clipboard", "Import from Clipboard"));
            createDsChildren.add(MenuItemConfig.separator());
            createDsChildren.add(MenuItemConfig.action("file.new.folder", "Create a New Folder", "FOLDER"));
            createDsChildren.add(MenuItemConfig.action("file.new.driver", "Driver", "PLUG"));

            List<MenuItemConfig> newDbChildren = new ArrayList<>();
            newDbChildren.add(MenuItemConfig.group("file.new.add.group", "Add", false, addChildren));
            newDbChildren.add(MenuItemConfig.group("file.new.create.datasource", "Create Data Source", true, createDsChildren));

            List<MenuItemConfig> fileNewChildren = new ArrayList<>();
            fileNewChildren.add(MenuItemConfig.group("file.new.file.group", "New File", true, newFileChildren));
            fileNewChildren.add(MenuItemConfig.group("file.new.db.group", "New", false, newDbChildren));

            // Image 2: Recent Projects
            List<MenuItemConfig> recentChildren = new ArrayList<>();
            recentChildren.add(MenuItemConfig.action("file.reopen.project", "Reopen Project"));
            recentChildren.add(MenuItemConfig.separator());
            recentChildren.add(MenuItemConfig.action("file.manage.projects", "Manage Projects\u2026"));
            recentChildren.add(MenuItemConfig.action("file.close.project", "Close Project"));
            recentChildren.add(MenuItemConfig.action("file.rename.project", "Rename Project\u2026"));
            recentChildren.add(MenuItemConfig.separator());
            recentChildren.add(MenuItemConfig.action("file.attach.directory", "Attach Directory to Project\u2026", "FOLDER_PLUS"));

            // Image 2: Remote Development
            List<MenuItemConfig> remoteDaemonChildren = new ArrayList<>();
            remoteDaemonChildren.add(MenuItemConfig.action("file.remote.dev.daemon.action", "Remote Development\u2026"));

            List<MenuItemConfig> remoteDevChildren = new ArrayList<>();
            remoteDevChildren.add(MenuItemConfig.action("file.remote.dev", "Remote Development", "DESKTOP"));
            remoteDevChildren.add(MenuItemConfig.group("file.remote.dev.daemon", "FileMenu.RemoteDevelopmentActions.Daemon", false, remoteDaemonChildren));

            // Image 2 & 4: Settings Actions
            List<MenuItemConfig> settingsChildren = new ArrayList<>();
            settingsChildren.add(MenuItemConfig.action("file.settings", "Settings\u2026", "COG"));
            settingsChildren.add(MenuItemConfig.action("file.project.structure", "Project Structure\u2026"));

            // Image 3: File Properties
            List<MenuItemConfig> removeBomChildren = new ArrayList<>();
            removeBomChildren.add(MenuItemConfig.action("file.remove.bom", "Remove BOM"));

            List<MenuItemConfig> addBomChildren = new ArrayList<>();
            addBomChildren.add(MenuItemConfig.action("file.add.bom", "Add BOM"));

            List<MenuItemConfig> lineSepChildren = new ArrayList<>();
            lineSepChildren.add(MenuItemConfig.action("file.line.sep.crlf", "CRLF - Windows (\\r\\n)"));
            lineSepChildren.add(MenuItemConfig.action("file.line.sep.lf", "LF - Unix and macOS (\\n)"));
            lineSepChildren.add(MenuItemConfig.action("file.line.sep.cr", "CR - Classic Mac OS (\\r)"));

            List<MenuItemConfig> filePropsChildren = new ArrayList<>();
            filePropsChildren.add(MenuItemConfig.action("file.props.encoding", "File Encoding"));
            filePropsChildren.add(MenuItemConfig.group("file.remove.bom.group", "RemoveBom.Group", false, removeBomChildren));
            filePropsChildren.add(MenuItemConfig.group("file.add.bom.group", "AddBom.Group", false, addBomChildren));
            filePropsChildren.add(MenuItemConfig.action("file.associate.file.type", "Associate with File Type\u2026"));
            filePropsChildren.add(MenuItemConfig.action("file.change.template.lang", "Change Template Data Language"));
            filePropsChildren.add(MenuItemConfig.action("file.toggle.readonly", "Toggle Read-Only Attribute"));
            filePropsChildren.add(MenuItemConfig.group("file.line.separators", "Line Separators", true, lineSepChildren));
            filePropsChildren.add(MenuItemConfig.separator());

            // Image 3: LocalHistory.MainMenuGroup
            List<MenuItemConfig> localHistChildren = new ArrayList<>();
            localHistChildren.add(MenuItemConfig.action("history.show", "Show History\u2026", "HISTORY"));
            localHistChildren.add(MenuItemConfig.action("history.show.selection", "Show History for Selection\u2026"));
            localHistChildren.add(MenuItemConfig.separator());
            localHistChildren.add(MenuItemConfig.action("history.show.project", "Show Project History\u2026"));
            localHistChildren.add(MenuItemConfig.action("history.recent.changes", "Recent Changes"));
            localHistChildren.add(MenuItemConfig.action("history.put.label", "Put Label\u2026"));

            List<MenuItemConfig> localHistMainChildren = new ArrayList<>();
            localHistMainChildren.add(MenuItemConfig.separator());
            localHistMainChildren.add(MenuItemConfig.group("file.local.history", "Local History", true, localHistChildren));
            localHistMainChildren.add(MenuItemConfig.separator());

            // Image 4: Manage IDE Settings
            List<MenuItemConfig> manageSettingsChildren = new ArrayList<>();
            manageSettingsChildren.add(MenuItemConfig.action("file.settings.import", "Import Settings\u2026"));
            manageSettingsChildren.add(MenuItemConfig.action("file.settings.export", "Export Settings\u2026"));
            manageSettingsChildren.add(MenuItemConfig.separator());
            manageSettingsChildren.add(MenuItemConfig.action("file.settings.restore", "Restore Default Settings\u2026"));
            manageSettingsChildren.add(MenuItemConfig.action("file.settings.backup.sync", "Backup and Sync\u2026"));

            // Image 4: Print/Export Actions
            List<MenuItemConfig> exportSubChildren = new ArrayList<>();
            exportSubChildren.add(MenuItemConfig.action("file.export.html", "Export Files or Selection to HTML\u2026"));

            List<MenuItemConfig> printExportChildren = new ArrayList<>();
            printExportChildren.add(MenuItemConfig.separator());
            printExportChildren.add(MenuItemConfig.group("file.export", "Export", true, exportSubChildren));
            printExportChildren.add(MenuItemConfig.action("file.print", "Print\u2026", "PRINT"));

            // Full File Menu assembling Images 1-4
            List<MenuItemConfig> fileMenu = new ArrayList<>();
            fileMenu.add(MenuItemConfig.action("file.open.actions", "File Open Actions"));
            fileMenu.add(MenuItemConfig.group("file.new", "New", true, fileNewChildren));
            fileMenu.add(MenuItemConfig.action("file.open.sql", "Open\u2026", "FOLDER_OPEN"));
            fileMenu.add(MenuItemConfig.action("file.save.as", "Save As\u2026"));
            fileMenu.add(MenuItemConfig.group("file.recent", "Recent Projects", true, recentChildren));
            fileMenu.add(MenuItemConfig.separator());
            fileMenu.add(MenuItemConfig.group("file.remote.dev.actions", "FileMenu.RemoteDevelopmentActions", false, remoteDevChildren));
            fileMenu.add(MenuItemConfig.separator());
            fileMenu.add(MenuItemConfig.group("file.settings.actions", "Settings Actions", false, settingsChildren));
            fileMenu.add(MenuItemConfig.separator());
            fileMenu.add(MenuItemConfig.action("file.sql.dialects", "SQL Dialects\u2026"));
            fileMenu.add(MenuItemConfig.action("file.sql.scopes", "SQL Resolution Scopes\u2026"));
            fileMenu.add(MenuItemConfig.action("file.edit.datasources.xml", "Edit dataSources.xml"));
            fileMenu.add(MenuItemConfig.separator());
            fileMenu.add(MenuItemConfig.group("file.properties", "File Properties", true, filePropsChildren));
            fileMenu.add(MenuItemConfig.separator());
            fileMenu.add(MenuItemConfig.group("file.local.history.main.group", "LocalHistory.MainMenuGroup", false, localHistMainChildren));
            fileMenu.add(MenuItemConfig.separator());
            fileMenu.add(MenuItemConfig.action("file.save.all", "Save All", "SAVE"));
            fileMenu.add(MenuItemConfig.action("file.reload.all", "Reload All from Disk", "SYNC_ALT"));
            fileMenu.add(MenuItemConfig.separator());
            fileMenu.add(MenuItemConfig.group("file.manage.settings", "Manage IDE Settings", true, manageSettingsChildren));
            fileMenu.add(MenuItemConfig.separator());
            fileMenu.add(MenuItemConfig.group("file.print.export.actions", "Print/Export Actions", false, printExportChildren));
            fileMenu.add(MenuItemConfig.action("file.power.save", "Power Save Mode"));
            fileMenu.add(MenuItemConfig.separator());
            fileMenu.add(MenuItemConfig.action("file.exit", "Exit"));
            mainMenuChildren.add(MenuItemConfig.group("menu.file", "File", true, fileMenu));

            // -------------------------------------------------------------
            // Main Menu > Edit (Exact match to DataGrip Images 1, 2, 3)
            // -------------------------------------------------------------
            // Image 1: Cut/Copy/Paste Actions > Copy Path/Reference... > CopyFileReference
            List<MenuItemConfig> copyFileRefChildren = new ArrayList<>();
            copyFileRefChildren.add(MenuItemConfig.action("edit.copy.path.absolute", "Absolute Path"));
            copyFileRefChildren.add(MenuItemConfig.action("edit.copy.path.filename", "File Name"));
            copyFileRefChildren.add(MenuItemConfig.separator());
            copyFileRefChildren.add(MenuItemConfig.action("edit.copy.path.line.number", "Path with Line Number"));
            copyFileRefChildren.add(MenuItemConfig.action("edit.copy.path.content.root", "Path from Content Root"));
            copyFileRefChildren.add(MenuItemConfig.action("edit.copy.path.source.root", "Path from Source Root"));
            copyFileRefChildren.add(MenuItemConfig.action("edit.copy.path.repo.root", "Path From Repository Root"));
            copyFileRefChildren.add(MenuItemConfig.action("edit.copy.git.hosting.link", "Git.Hosting.Copy.Link.Group"));

            List<MenuItemConfig> copyExtRefChildren = new ArrayList<>();
            copyExtRefChildren.add(MenuItemConfig.action("edit.copy.toolbox.url", "Toolbox URL", "CUBES"));

            List<MenuItemConfig> copyPathRefChildren = new ArrayList<>();
            copyPathRefChildren.add(MenuItemConfig.group("edit.copy.file.reference.group", "CopyFileReference", false, copyFileRefChildren));
            copyPathRefChildren.add(MenuItemConfig.separator());
            copyPathRefChildren.add(MenuItemConfig.group("edit.copy.external.reference.group", "CopyExternalReferenceGroup", false, copyExtRefChildren));
            copyPathRefChildren.add(MenuItemConfig.action("edit.copy.reference", "Copy Reference"));

            List<MenuItemConfig> pasteChildren = new ArrayList<>();
            pasteChildren.add(MenuItemConfig.action("edit.paste", "Paste", "PASTE"));
            pasteChildren.add(MenuItemConfig.action("edit.paste.history", "Paste from History\u2026"));
            pasteChildren.add(MenuItemConfig.action("edit.paste.plain", "Paste as Plain Text"));

            List<MenuItemConfig> cutCopyPasteChildren = new ArrayList<>();
            cutCopyPasteChildren.add(MenuItemConfig.action("edit.cut", "Cut", "CUT"));
            cutCopyPasteChildren.add(MenuItemConfig.action("edit.copy", "Copy", "COPY"));
            cutCopyPasteChildren.add(MenuItemConfig.action("edit.copy.paths", "Copy Paths"));
            cutCopyPasteChildren.add(MenuItemConfig.action("edit.copy.plain", "Copy as Plain Text"));
            cutCopyPasteChildren.add(MenuItemConfig.action("edit.copy.rich", "Copy as Rich Text"));
            cutCopyPasteChildren.add(MenuItemConfig.group("edit.copy.path.reference.group", "Copy Path/Reference\u2026", true, copyPathRefChildren));
            cutCopyPasteChildren.add(MenuItemConfig.group("edit.paste.group", "Paste", false, pasteChildren));
            cutCopyPasteChildren.add(MenuItemConfig.action("edit.copy.json.pointer", "Copy JSON Pointer"));

            // Image 2: Generate...
            List<MenuItemConfig> markdownChildren = new ArrayList<>();
            markdownChildren.add(MenuItemConfig.action("edit.generate.markdown.link", "Create Link", "LINK"));
            markdownChildren.add(MenuItemConfig.action("edit.generate.markdown.table", "Insert Table", "TABLE"));
            markdownChildren.add(MenuItemConfig.action("edit.generate.markdown.image", "Insert Image", "IMAGE"));
            markdownChildren.add(MenuItemConfig.action("edit.generate.markdown.toc", "Generate Table Of Contents", "LIST"));

            List<MenuItemConfig> generateInnerChildren = new ArrayList<>();
            generateInnerChildren.add(MenuItemConfig.action("edit.generate.sql.group", "SqlGenerateGroup"));
            generateInnerChildren.add(MenuItemConfig.action("edit.generate.xml.tag", "XML Tag\u2026"));
            generateInnerChildren.add(MenuItemConfig.action("edit.generate.override.methods", "Override Methods\u2026"));
            generateInnerChildren.add(MenuItemConfig.action("edit.generate.implement.methods", "Implement Methods\u2026"));
            generateInnerChildren.add(MenuItemConfig.action("edit.generate.delegate.methods", "Delegate Methods\u2026"));
            generateInnerChildren.add(MenuItemConfig.action("edit.generate.test.creators.group", "GenerateFromTestCreatorsGroup"));
            generateInnerChildren.add(MenuItemConfig.group("edit.generate.markdown.group", "Markdown.InsertGroup", true, markdownChildren));

            List<MenuItemConfig> generateRootChildren = new ArrayList<>();
            generateRootChildren.add(MenuItemConfig.group("edit.generate.group", "Generate", false, generateInnerChildren));

            // Image 3: Refactor
            List<MenuItemConfig> extractIntroduceChildren = new ArrayList<>();
            extractIntroduceChildren.add(MenuItemConfig.action("edit.refactor.table.alias", "Table alias\u2026"));
            extractIntroduceChildren.add(MenuItemConfig.action("edit.refactor.introduce.variable", "Introduce Variable\u2026"));
            extractIntroduceChildren.add(MenuItemConfig.action("edit.refactor.extract.routine", "Extract Routine\u2026"));

            List<MenuItemConfig> refactorChildren = new ArrayList<>();
            refactorChildren.add(MenuItemConfig.action("edit.refactor.rename", "Rename\u2026"));
            refactorChildren.add(MenuItemConfig.separator());
            refactorChildren.add(MenuItemConfig.action("edit.refactor.expand.column.list", "Expand Column List"));
            refactorChildren.add(MenuItemConfig.separator());
            refactorChildren.add(MenuItemConfig.action("edit.refactor.convert.subquery", "Convert to Subquery"));
            refactorChildren.add(MenuItemConfig.action("edit.refactor.subquery.cte", "Subquery as CTE"));
            refactorChildren.add(MenuItemConfig.separator());
            refactorChildren.add(MenuItemConfig.group("edit.refactor.extract.introduce.group", "Extract/Introduce", true, extractIntroduceChildren));
            refactorChildren.add(MenuItemConfig.action("edit.refactor.qualify.identifier", "Qualify Identifier"));
            refactorChildren.add(MenuItemConfig.action("edit.refactor.unqualify.identifier", "Unqualify Identifier"));
            refactorChildren.add(MenuItemConfig.action("edit.refactor.quote.identifier", "Quote Identifier"));
            refactorChildren.add(MenuItemConfig.action("edit.refactor.unquote.identifier", "Unquote Identifier"));
            refactorChildren.add(MenuItemConfig.separator());
            refactorChildren.add(MenuItemConfig.action("edit.refactor.flip.expression", "Flip Expression"));
            refactorChildren.add(MenuItemConfig.separator());
            refactorChildren.add(MenuItemConfig.action("edit.refactor.inject.language", "Inject Language or Reference"));
            refactorChildren.add(MenuItemConfig.action("edit.refactor.uninject.language", "Uninject Language or Reference"));

            // Image 2: Selection
            List<MenuItemConfig> selectWordChildren = new ArrayList<>();
            selectWordChildren.add(MenuItemConfig.action("edit.selection.extend", "Extend Selection"));
            selectWordChildren.add(MenuItemConfig.action("edit.selection.shrink", "Shrink Selection"));

            List<MenuItemConfig> editorSelectChildren = new ArrayList<>();
            editorSelectChildren.add(MenuItemConfig.action("edit.select.all", "Select All"));
            editorSelectChildren.add(MenuItemConfig.action("edit.selection.add.carets.ends", "Add Carets to Ends of Selected Lines"));
            editorSelectChildren.add(MenuItemConfig.group("edit.selection.word.actions", "Select Word Actions", false, selectWordChildren));

            List<MenuItemConfig> selectionChildren = new ArrayList<>();
            selectionChildren.add(MenuItemConfig.action("edit.selection.column.mode", "Column Selection Mode"));
            selectionChildren.add(MenuItemConfig.group("edit.selection.editor.select.actions", "Editor Select Actions", false, editorSelectChildren));
            selectionChildren.add(MenuItemConfig.action("edit.toggle.case", "Toggle Case"));
            selectionChildren.add(MenuItemConfig.action("edit.join.lines", "Join Lines"));
            selectionChildren.add(MenuItemConfig.action("edit.duplicate.lines", "Duplicate Entire Lines"));
            selectionChildren.add(MenuItemConfig.action("edit.sort.lines", "Sort Lines"));

            List<MenuItemConfig> editMenu = new ArrayList<>();
            editMenu.add(MenuItemConfig.action("edit.undo", "Undo", "UNDO"));
            editMenu.add(MenuItemConfig.action("edit.redo", "Redo", "REDO"));
            editMenu.add(MenuItemConfig.separator());
            editMenu.add(MenuItemConfig.group("edit.cut.copy.paste.actions", "Cut/Copy/Paste Actions", false, cutCopyPasteChildren));
            editMenu.add(MenuItemConfig.action("edit.delete", "Delete"));
            editMenu.add(MenuItemConfig.separator());
            editMenu.add(MenuItemConfig.action("edit.find", "Find\u2026", "SEARCH"));
            editMenu.add(MenuItemConfig.action("edit.replace", "Replace\u2026"));
            editMenu.add(MenuItemConfig.action("edit.find.in.files", "Find in Files\u2026", "SEARCH"));
            editMenu.add(MenuItemConfig.action("edit.replace.in.files", "Replace in Files\u2026"));
            editMenu.add(MenuItemConfig.action("edit.find.usages", "Find Usages"));
            editMenu.add(MenuItemConfig.separator());
            editMenu.add(MenuItemConfig.group("edit.generate.root.group", "Generate\u2026", true, generateRootChildren));
            editMenu.add(MenuItemConfig.separator());
            editMenu.add(MenuItemConfig.action("edit.insert.live.template", "Insert Live Template\u2026"));
            editMenu.add(MenuItemConfig.action("edit.surround.with", "Surround With\u2026"));
            editMenu.add(MenuItemConfig.separator());
            editMenu.add(MenuItemConfig.action("edit.format.code", "Reformat Code", "INDENT"));
            editMenu.add(MenuItemConfig.action("edit.format.file", "Reformat File\u2026"));
            editMenu.add(MenuItemConfig.action("edit.comment.line", "// Comment with Line Comment"));
            editMenu.add(MenuItemConfig.action("edit.comment.block", "Comment with Block Comment"));
            editMenu.add(MenuItemConfig.action("edit.auto.indent", "Auto-Indent Lines"));
            editMenu.add(MenuItemConfig.separator());
            editMenu.add(MenuItemConfig.group("edit.refactor", "Refactor", true, refactorChildren));
            editMenu.add(MenuItemConfig.separator());
            editMenu.add(MenuItemConfig.group("edit.selection", "Selection", true, selectionChildren));
            editMenu.add(MenuItemConfig.separator());
            editMenu.add(MenuItemConfig.action("edit.toggle.bookmark", "Toggle Bookmark", "BOOKMARK"));
            editMenu.add(MenuItemConfig.action("edit.show.bookmarks", "Show Bookmarks\u2026"));
            mainMenuChildren.add(MenuItemConfig.group("menu.edit", "Edit", true, editMenu));

            // -------------------------------------------------------------
            // Main Menu > View (Exact match to DataGrip Images 4, 5)
            // -------------------------------------------------------------
            List<MenuItemConfig> toolWindows = new ArrayList<>();
            toolWindows.add(MenuItemConfig.action("middle.database", "Database Explorer", "DATABASE"));
            toolWindows.add(MenuItemConfig.action("view.tool.files", "Files", "FOLDER"));
            toolWindows.add(MenuItemConfig.action("view.tool.terminal", "Terminal", "TERMINAL"));
            toolWindows.add(MenuItemConfig.action("file.new.console", "Query Console", "TERMINAL"));

            // Image 4: Appearance > ToggleFullScreenGroup
            List<MenuItemConfig> fullScreenGroupChildren = new ArrayList<>();
            fullScreenGroupChildren.add(MenuItemConfig.separator());
            fullScreenGroupChildren.add(MenuItemConfig.action("view.toggle.presentation.mode", "Toggle Presentation Mode"));
            fullScreenGroupChildren.add(MenuItemConfig.action("view.toggle.distraction.free.mode", "Toggle Distraction Free Mode"));
            fullScreenGroupChildren.add(MenuItemConfig.action("view.toggle.fullscreen.mode", "Toggle Full Screen Mode"));
            fullScreenGroupChildren.add(MenuItemConfig.action("view.toggle.zen.mode", "Toggle Zen Mode"));
            fullScreenGroupChildren.add(MenuItemConfig.separator());
            fullScreenGroupChildren.add(MenuItemConfig.action("view.compact.mode", "Compact Mode"));

            // Image 5: Appearance > UIToggleActions > ViewToolbarActionsGroup
            List<MenuItemConfig> statusBarWidgetChildren = new ArrayList<>();
            statusBarWidgetChildren.add(MenuItemConfig.action("view.status.bar.widget.status.text", "Status Text"));

            List<MenuItemConfig> navBarGroupChildren = new ArrayList<>();

            List<MenuItemConfig> viewToolbarActionsChildren = new ArrayList<>();
            viewToolbarActionsChildren.add(MenuItemConfig.action("view.toolbar.actions.toolbar", "Toolbar"));
            viewToolbarActionsChildren.add(MenuItemConfig.action("view.toolbar.actions.navbar", "Navigation Bar"));
            viewToolbarActionsChildren.add(MenuItemConfig.group("view.toolbar.actions.navbar.group", "Navigation Bar", true, navBarGroupChildren));
            viewToolbarActionsChildren.add(MenuItemConfig.action("view.toggle.tool.window.bars", "Tool Window Bars"));
            viewToolbarActionsChildren.add(MenuItemConfig.action("view.toggle.status.bar", "Status Bar"));
            viewToolbarActionsChildren.add(MenuItemConfig.group("view.status.bar.widgets", "Status Bar Widgets", true, statusBarWidgetChildren));
            viewToolbarActionsChildren.add(MenuItemConfig.action("view.toggle.members.in.nav.bar", "Members in Navigation Bar"));

            List<MenuItemConfig> uiToggleActionsChildren = new ArrayList<>();
            uiToggleActionsChildren.add(MenuItemConfig.separator());
            uiToggleActionsChildren.add(MenuItemConfig.action("view.toggle.presentation.assistant", "Presentation Assistant"));
            uiToggleActionsChildren.add(MenuItemConfig.separator());
            uiToggleActionsChildren.add(MenuItemConfig.action("view.toggle.main.menu", "Main Menu"));
            uiToggleActionsChildren.add(MenuItemConfig.action("view.toggle.main.menu.separate", "Main Menu"));
            uiToggleActionsChildren.add(MenuItemConfig.action("view.toggle.toolbar", "Toolbar"));
            uiToggleActionsChildren.add(MenuItemConfig.action("view.toggle.toolbar.classic", "Toolbar Classic"));
            uiToggleActionsChildren.add(MenuItemConfig.action("view.toggle.navigation.bar", "Navigation Bar"));
            uiToggleActionsChildren.add(MenuItemConfig.group("view.toolbar.actions.group", "ViewToolbarActionsGroup", false, viewToolbarActionsChildren));
            uiToggleActionsChildren.add(MenuItemConfig.separator());

            List<MenuItemConfig> appearanceChildren = new ArrayList<>();
            appearanceChildren.add(MenuItemConfig.group("view.toggle.fullscreen.group", "ToggleFullScreenGroup", false, fullScreenGroupChildren));
            appearanceChildren.add(MenuItemConfig.action("view.zoom.ide", "Zoom IDE"));
            appearanceChildren.add(MenuItemConfig.group("view.ui.toggle.actions", "UIToggleActions", false, uiToggleActionsChildren));
            appearanceChildren.add(MenuItemConfig.separator());

            // Image 5: View Recent Actions Group
            List<MenuItemConfig> viewRecentActionsChildren = new ArrayList<>();
            viewRecentActionsChildren.add(MenuItemConfig.action("view.recent.files", "Recent Files"));
            viewRecentActionsChildren.add(MenuItemConfig.action("view.recent.toggle.changed.only", "Toggle Changed Only Files"));
            viewRecentActionsChildren.add(MenuItemConfig.action("view.recent.iterate.files", "Iterate Recent Files"));
            viewRecentActionsChildren.add(MenuItemConfig.action("view.recently.changed.files", "Recently Changed Files"));
            viewRecentActionsChildren.add(MenuItemConfig.action("view.recent.locations", "Recent Locations"));
            viewRecentActionsChildren.add(MenuItemConfig.action("view.recent.toggle.changed.only.second", "Toggle Changed Only Files"));
            viewRecentActionsChildren.add(MenuItemConfig.action("view.recent.iterate.files.second", "Iterate Recent Files"));
            viewRecentActionsChildren.add(MenuItemConfig.action("view.recent.files.second", "Recent Files"));
            viewRecentActionsChildren.add(MenuItemConfig.action("view.recently.changed.files.second", "Recently Changed Files"));
            viewRecentActionsChildren.add(MenuItemConfig.action("view.recent.changes", "Recent Changes"));

            List<MenuItemConfig> viewMenu = new ArrayList<>();
            viewMenu.add(MenuItemConfig.group("view.tool.windows", "Tool Windows", true, toolWindows));
            viewMenu.add(MenuItemConfig.group("view.appearance", "Appearance", true, appearanceChildren));
            viewMenu.add(MenuItemConfig.group("view.recent.actions.group", "View Recent Actions Group", false, viewRecentActionsChildren));
            viewMenu.add(MenuItemConfig.separator());
            viewMenu.add(MenuItemConfig.action("view.font.increase", "Increase Font Size in All Editors"));
            viewMenu.add(MenuItemConfig.action("view.font.decrease", "Decrease Font Size in All Editors"));
            viewMenu.add(MenuItemConfig.action("view.font.reset", "Reset Font Size in All Editors"));
            mainMenuChildren.add(MenuItemConfig.group("menu.view", "View", true, viewMenu));

            // -------------------------------------------------------------
            // Main Menu > Navigate (Exact match to DataGrip Images 1, 2, 3)
            // -------------------------------------------------------------
            // Image 1: Goto by Name Actions
            List<MenuItemConfig> gotoByNameChildren = new ArrayList<>();
            gotoByNameChildren.add(MenuItemConfig.action("nav.goto.class", "Go to Class\u2026"));
            gotoByNameChildren.add(MenuItemConfig.action("nav.goto.file", "Go to File\u2026"));
            gotoByNameChildren.add(MenuItemConfig.action("nav.goto.symbol", "Go to Symbol\u2026"));
            gotoByNameChildren.add(MenuItemConfig.action("nav.goto.text", "Text\u2026"));
            gotoByNameChildren.add(MenuItemConfig.action("nav.goto.database.object", "Go To Database Object"));

            // Image 2: Goto Error/Bookmark Actions
            List<MenuItemConfig> gotoErrorChildren = new ArrayList<>();
            gotoErrorChildren.add(MenuItemConfig.action("nav.next.highlighted.error", "Next Highlighted Error"));
            gotoErrorChildren.add(MenuItemConfig.action("nav.prev.highlighted.error", "Previous Highlighted Error"));

            // Image 2: GoToEditPointGroup
            List<MenuItemConfig> gotoEditPointChildren = new ArrayList<>();
            gotoEditPointChildren.add(MenuItemConfig.separator());
            gotoEditPointChildren.add(MenuItemConfig.action("nav.next.emmet.edit.point", "Next Emmet Edit Point"));
            gotoEditPointChildren.add(MenuItemConfig.action("nav.prev.emmet.edit.point", "Previous Emmet Edit Point"));

            // Image 2: Navigate in File -> TemplateParametersNavigation
            List<MenuItemConfig> templateNavChildren = new ArrayList<>();
            templateNavChildren.add(MenuItemConfig.action("nav.next.template.parameter", "Next Live Template Parameter"));
            templateNavChildren.add(MenuItemConfig.action("nav.prev.template.parameter", "Previous Live Template Parameter"));

            // Image 2: Navigate in File -> Change Navigation Actions
            List<MenuItemConfig> changeNavChildren = new ArrayList<>();
            changeNavChildren.add(MenuItemConfig.separator());
            changeNavChildren.add(MenuItemConfig.action("nav.next.change", "Next Change", "ARROW_DOWN"));
            changeNavChildren.add(MenuItemConfig.action("nav.prev.change", "Previous Change", "ARROW_UP"));

            List<MenuItemConfig> navInFileChildren = new ArrayList<>();
            navInFileChildren.add(MenuItemConfig.action("nav.file.next.statement", "Next Statement"));
            navInFileChildren.add(MenuItemConfig.action("nav.file.prev.statement", "Previous Statement"));
            navInFileChildren.add(MenuItemConfig.action("nav.file.matching.brace", "Move Caret to Matching Brace"));
            navInFileChildren.add(MenuItemConfig.separator());
            navInFileChildren.add(MenuItemConfig.group("nav.template.parameters.group", "TemplateParametersNavigation", false, templateNavChildren));
            navInFileChildren.add(MenuItemConfig.action("nav.custom.folding", "Custom Folding\u2026"));
            navInFileChildren.add(MenuItemConfig.group("nav.change.navigation.group", "Change Navigation Actions", false, changeNavChildren));

            // Image 3: Goto by Reference Actions -> Hierarchy Actions
            List<MenuItemConfig> hierarchyChildren = new ArrayList<>();
            hierarchyChildren.add(MenuItemConfig.action("nav.type.hierarchy", "Type Hierarchy"));
            hierarchyChildren.add(MenuItemConfig.action("nav.method.hierarchy", "Method Hierarchy"));
            hierarchyChildren.add(MenuItemConfig.action("nav.call.hierarchy", "Call Hierarchy"));

            List<MenuItemConfig> gotoByRefChildren = new ArrayList<>();
            gotoByRefChildren.add(MenuItemConfig.separator());
            gotoByRefChildren.add(MenuItemConfig.action("nav.select.in", "Select In\u2026"));
            gotoByRefChildren.add(MenuItemConfig.action("nav.jump.to.nav.bar", "Jump to Navigation Bar"));
            gotoByRefChildren.add(MenuItemConfig.separator());
            gotoByRefChildren.add(MenuItemConfig.action("nav.goto.declaration", "Go to Declaration or Usages"));
            gotoByRefChildren.add(MenuItemConfig.action("nav.goto.implementation", "Go to Implementation(s)"));
            gotoByRefChildren.add(MenuItemConfig.action("nav.goto.type.declaration", "Go to Type Declaration"));
            gotoByRefChildren.add(MenuItemConfig.action("nav.goto.super.method", "Go to Super Method"));
            gotoByRefChildren.add(MenuItemConfig.action("nav.goto.test", "Go to Test"));
            gotoByRefChildren.add(MenuItemConfig.action("nav.related.symbol", "Related Symbol\u2026"));
            gotoByRefChildren.add(MenuItemConfig.separator());
            gotoByRefChildren.add(MenuItemConfig.action("nav.file.structure", "File Structure"));
            gotoByRefChildren.add(MenuItemConfig.action("nav.file.path", "File Path"));
            gotoByRefChildren.add(MenuItemConfig.group("nav.hierarchy.actions.group", "Hierarchy Actions", false, hierarchyChildren));

            // Image 3: DBE.GoToMenuEx
            List<MenuItemConfig> dbeGotoChildren = new ArrayList<>();
            dbeGotoChildren.add(MenuItemConfig.separator());
            dbeGotoChildren.add(MenuItemConfig.action("nav.dbe.next.statement", "Next Statement"));
            dbeGotoChildren.add(MenuItemConfig.action("nav.dbe.prev.statement", "Previous Statement"));
            dbeGotoChildren.add(MenuItemConfig.separator());

            // Full Navigate Menu assembling Images 1-3
            List<MenuItemConfig> navMenu = new ArrayList<>();
            navMenu.add(MenuItemConfig.action("nav.back", "Back", "ARROW_LEFT"));
            navMenu.add(MenuItemConfig.action("nav.forward", "Forward", "ARROW_RIGHT"));
            navMenu.add(MenuItemConfig.separator());
            navMenu.add(MenuItemConfig.action("header.search", "Search Everywhere", "SEARCH"));
            navMenu.add(MenuItemConfig.separator());
            navMenu.add(MenuItemConfig.group("nav.goto.by.name.group", "Goto by Name Actions", false, gotoByNameChildren));
            navMenu.add(MenuItemConfig.action("nav.goto.row", "Row\u2026"));
            navMenu.add(MenuItemConfig.action("nav.open.file.uri", "Open File URI", "FOLDER"));
            navMenu.add(MenuItemConfig.action("nav.open.url", "Open URL", "GLOBE"));
            navMenu.add(MenuItemConfig.action("nav.related.rows", "Related Rows", "TABLE"));
            navMenu.add(MenuItemConfig.action("nav.goto.line.column", "Go to Line:Column\u2026"));
            navMenu.add(MenuItemConfig.separator());
            navMenu.add(MenuItemConfig.group("nav.goto.error.bookmark.group", "Goto Error/Bookmark Actions", false, gotoErrorChildren));
            navMenu.add(MenuItemConfig.group("nav.goto.edit.point.group", "GoToEditPointGroup", false, gotoEditPointChildren));
            navMenu.add(MenuItemConfig.action("nav.last.edit.location", "Last Edit Location"));
            navMenu.add(MenuItemConfig.action("nav.next.edit.location", "Next Edit Location"));
            navMenu.add(MenuItemConfig.separator());
            navMenu.add(MenuItemConfig.group("nav.navigate.in.file.group", "Navigate in File", false, navInFileChildren));
            navMenu.add(MenuItemConfig.group("nav.goto.by.reference.group", "Goto by Reference Actions", false, gotoByRefChildren));
            navMenu.add(MenuItemConfig.separator());
            navMenu.add(MenuItemConfig.action("nav.prev.occurrence", "Previous Occurrence", "ARROW_UP"));
            navMenu.add(MenuItemConfig.action("nav.next.occurrence", "Next Occurrence", "ARROW_DOWN"));
            navMenu.add(MenuItemConfig.separator());
            navMenu.add(MenuItemConfig.group("nav.dbe.goto.menu.ex", "DBE.GoToMenuEx", false, dbeGotoChildren));
            mainMenuChildren.add(MenuItemConfig.group("menu.navigate", "Navigate", true, navMenu));

            // -------------------------------------------------------------
            // Main Menu > Code (Exact match to DataGrip Images 4, 5)
            // -------------------------------------------------------------
            // Image 5: Code Completion (Submenu popup)
            List<MenuItemConfig> codeCompletionChildren = new ArrayList<>();
            codeCompletionChildren.add(MenuItemConfig.action("code.completion.basic", "Basic"));
            codeCompletionChildren.add(MenuItemConfig.action("code.completion.type.matching", "Type-Matching"));
            codeCompletionChildren.add(MenuItemConfig.separator());
            codeCompletionChildren.add(MenuItemConfig.action("code.completion.complete.statement", "Complete Current Statement"));
            codeCompletionChildren.add(MenuItemConfig.separator());
            codeCompletionChildren.add(MenuItemConfig.action("code.completion.cyclic.expand.word", "Cyclic Expand Word"));
            codeCompletionChildren.add(MenuItemConfig.action("code.completion.cyclic.expand.word.backward", "Cyclic Expand Word (Backward)"));
            codeCompletionChildren.add(MenuItemConfig.separator());
            codeCompletionChildren.add(MenuItemConfig.action("code.completion.call.inline", "Call Inline Completion"));
            codeCompletionChildren.add(MenuItemConfig.action("code.completion.insert.inline.proposal", "Insert Inline Proposal"));
            codeCompletionChildren.add(MenuItemConfig.action("code.completion.insert.inline.word", "Insert Inline Proposal's Word"));
            codeCompletionChildren.add(MenuItemConfig.action("code.completion.insert.inline.line", "Insert Inline Proposal's Line"));

            // Image 1: InspectCodeInCodeMenuGroup
            List<MenuItemConfig> inspectCodeActionsChildren = new ArrayList<>();
            inspectCodeActionsChildren.add(MenuItemConfig.action("code.inspect.code", "Inspect Code\u2026"));
            inspectCodeActionsChildren.add(MenuItemConfig.action("code.cleanup", "Code Cleanup\u2026"));

            List<MenuItemConfig> analyzeActionsChildren = new ArrayList<>();
            analyzeActionsChildren.add(MenuItemConfig.action("code.silent.cleanup", "Silent Code Cleanup"));
            analyzeActionsChildren.add(MenuItemConfig.action("code.run.inspection.by.name", "Run Inspection by Name\u2026"));
            analyzeActionsChildren.add(MenuItemConfig.action("code.configure.analysis", "Configure Current File Analysis\u2026"));
            analyzeActionsChildren.add(MenuItemConfig.action("code.view.offline.results", "View Offline Inspection Results\u2026"));
            analyzeActionsChildren.add(MenuItemConfig.separator());
            analyzeActionsChildren.add(MenuItemConfig.action("code.analyze.dataflow.to.here", "Analyze Data Flow to Here\u2026"));
            analyzeActionsChildren.add(MenuItemConfig.action("code.analyze.dataflow.from.here", "Analyze Data Flow from Here\u2026"));

            List<MenuItemConfig> analyzeCodeChildren = new ArrayList<>();
            analyzeCodeChildren.add(MenuItemConfig.group("code.analyze.actions.group", "AnalyzeActions", false, analyzeActionsChildren));
            analyzeCodeChildren.add(MenuItemConfig.action("code.analyze.platform.menu", "AnalyzePlatformMenu"));

            List<MenuItemConfig> inspectCodeChildren = new ArrayList<>();
            inspectCodeChildren.add(MenuItemConfig.separator());
            inspectCodeChildren.add(MenuItemConfig.group("code.inspect.actions.group", "Inspect Code Actions", false, inspectCodeActionsChildren));
            inspectCodeChildren.add(MenuItemConfig.group("code.analyze.code.group", "Analyze Code", false, analyzeCodeChildren));

            // Image 2: Folding (Popup submenu)
            List<MenuItemConfig> expandToLevelChildren = new ArrayList<>();
            expandToLevelChildren.add(MenuItemConfig.action("code.folding.expand.level.1", "1"));
            expandToLevelChildren.add(MenuItemConfig.action("code.folding.expand.level.2", "2"));
            expandToLevelChildren.add(MenuItemConfig.action("code.folding.expand.level.3", "3"));
            expandToLevelChildren.add(MenuItemConfig.action("code.folding.expand.level.4", "4"));
            expandToLevelChildren.add(MenuItemConfig.action("code.folding.expand.level.5", "5"));

            List<MenuItemConfig> expandAllToLevelChildren = new ArrayList<>();
            expandAllToLevelChildren.add(MenuItemConfig.action("code.folding.expand.all.level.1", "1"));
            expandAllToLevelChildren.add(MenuItemConfig.action("code.folding.expand.all.level.2", "2"));
            expandAllToLevelChildren.add(MenuItemConfig.action("code.folding.expand.all.level.3", "3"));
            expandAllToLevelChildren.add(MenuItemConfig.action("code.folding.expand.all.level.4", "4"));
            expandAllToLevelChildren.add(MenuItemConfig.action("code.folding.expand.all.level.5", "5"));

            List<MenuItemConfig> langFoldingChildren = new ArrayList<>();
            langFoldingChildren.add(MenuItemConfig.action("code.folding.expand.doc.comments", "Expand Doc Comments"));
            langFoldingChildren.add(MenuItemConfig.action("code.folding.collapse.doc.comments", "Collapse Doc Comments"));

            List<MenuItemConfig> foldingChildren = new ArrayList<>();
            foldingChildren.add(MenuItemConfig.action("code.folding.expand", "Expand"));
            foldingChildren.add(MenuItemConfig.action("code.folding.expand.recursively", "Expand Recursively"));
            foldingChildren.add(MenuItemConfig.action("code.folding.expand.all", "Expand All"));
            foldingChildren.add(MenuItemConfig.separator());
            foldingChildren.add(MenuItemConfig.action("code.folding.collapse", "Collapse"));
            foldingChildren.add(MenuItemConfig.action("code.folding.collapse.recursively", "Collapse Recursively"));
            foldingChildren.add(MenuItemConfig.action("code.folding.collapse.all", "Collapse All"));
            foldingChildren.add(MenuItemConfig.separator());
            foldingChildren.add(MenuItemConfig.group("code.folding.expand.to.level.group", "Expand to Level", false, expandToLevelChildren));
            foldingChildren.add(MenuItemConfig.group("code.folding.expand.all.to.level.group", "Expand All to Level", false, expandAllToLevelChildren));
            foldingChildren.add(MenuItemConfig.separator());
            foldingChildren.add(MenuItemConfig.group("code.folding.language.specific.group", "LanguageSpecificFoldingGroup", false, langFoldingChildren));
            foldingChildren.add(MenuItemConfig.separator());
            foldingChildren.add(MenuItemConfig.action("code.folding.toggle", "Toggle Folding"));
            foldingChildren.add(MenuItemConfig.separator());
            foldingChildren.add(MenuItemConfig.action("code.folding.fold.selection", "Fold Selection / Remove region"));
            foldingChildren.add(MenuItemConfig.action("code.folding.fold.block", "Fold Code Block"));

            // Image 3: Comment Actions
            List<MenuItemConfig> commentActionsChildren = new ArrayList<>();
            commentActionsChildren.add(MenuItemConfig.action("code.comment.line", "// Comment with Line Comment"));
            commentActionsChildren.add(MenuItemConfig.action("code.comment.block", "Comment with Block Comment"));

            // Image 3: Code Formatting Actions
            List<MenuItemConfig> codeFormattingChildren = new ArrayList<>();
            codeFormattingChildren.add(MenuItemConfig.action("code.reformat", "Reformat Code", "INDENT"));
            codeFormattingChildren.add(MenuItemConfig.action("code.reformat.json", "Reformat JSON"));
            codeFormattingChildren.add(MenuItemConfig.action("code.reformat.file", "Reformat File\u2026"));
            codeFormattingChildren.add(MenuItemConfig.action("code.auto.indent", "Auto-Indent Lines"));
            codeFormattingChildren.add(MenuItemConfig.action("code.optimize.imports", "Optimize Imports"));
            codeFormattingChildren.add(MenuItemConfig.action("code.rearrange.code", "Rearrange Code"));

            // Full Code Menu assembling Images 1-5
            List<MenuItemConfig> codeMenu = new ArrayList<>();
            codeMenu.add(MenuItemConfig.action("code.override.methods", "Override Methods\u2026"));
            codeMenu.add(MenuItemConfig.action("code.implement.methods", "Implement Methods\u2026"));
            codeMenu.add(MenuItemConfig.action("code.generate", "Generate\u2026"));
            codeMenu.add(MenuItemConfig.separator());
            codeMenu.add(MenuItemConfig.group("code.completion.group", "Code Completion", true, codeCompletionChildren));
            codeMenu.add(MenuItemConfig.group("code.inspect.group", "InspectCodeInCodeMenuGroup", false, inspectCodeChildren));
            codeMenu.add(MenuItemConfig.separator());
            codeMenu.add(MenuItemConfig.action("code.insert.live.template", "Insert Live Template\u2026"));
            codeMenu.add(MenuItemConfig.action("code.save.live.template", "Save as Live Template\u2026"));
            codeMenu.add(MenuItemConfig.separator());
            codeMenu.add(MenuItemConfig.action("code.surround.with", "Surround With\u2026"));
            codeMenu.add(MenuItemConfig.action("code.unwrap.remove", "Unwrap/Remove\u2026"));
            codeMenu.add(MenuItemConfig.separator());
            codeMenu.add(MenuItemConfig.group("code.folding.group", "Folding", true, foldingChildren));
            codeMenu.add(MenuItemConfig.separator());
            codeMenu.add(MenuItemConfig.group("code.comment.actions.group", "Comment Actions", false, commentActionsChildren));
            codeMenu.add(MenuItemConfig.group("code.formatting.actions.group", "Code Formatting Actions", false, codeFormattingChildren));
            codeMenu.add(MenuItemConfig.separator());
            codeMenu.add(MenuItemConfig.action("code.move.statement.down", "Move Statement Down"));
            codeMenu.add(MenuItemConfig.action("code.move.statement.up", "Move Statement Up"));
            codeMenu.add(MenuItemConfig.action("code.move.element.left", "Move Element Left"));
            codeMenu.add(MenuItemConfig.action("code.move.element.right", "Move Element Right"));
            codeMenu.add(MenuItemConfig.action("code.move.line.down", "Move Line Down"));
            codeMenu.add(MenuItemConfig.action("code.move.line.up", "Move Line Up"));
            codeMenu.add(MenuItemConfig.separator());
            codeMenu.add(MenuItemConfig.separator());
            mainMenuChildren.add(MenuItemConfig.group("menu.code", "Code", true, codeMenu));

            // -------------------------------------------------------------
            // Main Menu > Refactor (Exact match to DataGrip Images 4, 5)
            // -------------------------------------------------------------
            List<MenuItemConfig> refactorExtractIntroduceChildren = new ArrayList<>();
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.introduce.variable", "Introduce Variable\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.extract.routine", "Extract Routine\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.table.alias", "Table alias\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.introduce.constant", "Introduce Constant\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.introduce.field", "Introduce Field\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.introduce.parameter", "Introduce Parameter\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.separator());
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.introduce.parameter.object", "Introduce Parameter Object\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.separator());
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.extract.method", "Extract Method\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.separator());
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.extract.delegate", "Extract Delegate\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.include.file", "Include File\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.extract.interface", "Extract Interface\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.extract.superclass", "Extract Superclass\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.extract.module", "Extract Module\u2026"));
            refactorExtractIntroduceChildren.add(MenuItemConfig.action("refactor.subquery.cte", "Subquery as CTE"));

            List<MenuItemConfig> refactorMenu = new ArrayList<>();
            refactorMenu.add(MenuItemConfig.action("refactor.this", "Refactor This\u2026"));
            refactorMenu.add(MenuItemConfig.action("refactor.rename", "Rename\u2026"));
            refactorMenu.add(MenuItemConfig.action("refactor.change.signature", "Change Signature\u2026"));
            refactorMenu.add(MenuItemConfig.action("refactor.modify.object", "Modify Object\u2026"));
            refactorMenu.add(MenuItemConfig.separator());
            refactorMenu.add(MenuItemConfig.group("refactor.extract.introduce.group", "Extract/Introduce", true, refactorExtractIntroduceChildren));
            refactorMenu.add(MenuItemConfig.action("refactor.inline", "Inline\u2026"));
            refactorMenu.add(MenuItemConfig.separator());
            refactorMenu.add(MenuItemConfig.action("refactor.move", "Move\u2026"));
            refactorMenu.add(MenuItemConfig.action("refactor.copy", "Copy\u2026"));
            refactorMenu.add(MenuItemConfig.action("refactor.safe.delete", "Safe Delete\u2026"));
            refactorMenu.add(MenuItemConfig.separator());
            refactorMenu.add(MenuItemConfig.action("refactor.pull.members.up", "Pull Members Up\u2026"));
            refactorMenu.add(MenuItemConfig.action("refactor.push.members.down", "Push Members Down\u2026"));
            refactorMenu.add(MenuItemConfig.action("refactor.invert.boolean", "Invert Boolean\u2026"));
            mainMenuChildren.add(MenuItemConfig.group("menu.refactor", "Refactor", true, refactorMenu));

            // Build (Image 4)
            mainMenuChildren.add(MenuItemConfig.action("main.menu.build", "Build"));

            // -------------------------------------------------------------
            // Main Menu > Run (Exact match to DataGrip Images 1-4)
            // -------------------------------------------------------------
            // Image 2: Run/Debug (Group)
            List<MenuItemConfig> runDebugChildren = new ArrayList<>();
            runDebugChildren.add(MenuItemConfig.action("run.run", "Run", "PLAY"));
            runDebugChildren.add(MenuItemConfig.action("run.debug", "Debug", "BUG"));
            runDebugChildren.add(MenuItemConfig.action("run.coverage", "Run with Coverage", "SHIELD"));
            runDebugChildren.add(MenuItemConfig.action("run.profiler", "Run with Profiler", "TACHOMETER"));

            // Image 2: XDebugger.AttachGroup (Group)
            List<MenuItemConfig> attachGroupChildren = new ArrayList<>();
            attachGroupChildren.add(MenuItemConfig.action("run.attach.to.process", "Attach to Process\u2026", "RETWEET"));

            // Image 3: Debugger Actions > Debugging Actions
            // DebugReloadGroup
            List<MenuItemConfig> debugReloadChildren = new ArrayList<>();
            debugReloadChildren.add(MenuItemConfig.action("run.compile.reload.modified.files", "Compile and Reload Modified Files"));
            debugReloadChildren.add(MenuItemConfig.action("run.update.running.app", "Update Running Application", "REFRESH"));

            // StepOver.Ref
            List<MenuItemConfig> stepOverChildren = new ArrayList<>();
            stepOverChildren.add(MenuItemConfig.action("run.step.over", "Step Over", "STEP_FORWARD"));
            stepOverChildren.add(MenuItemConfig.action("run.force.step.over", "Force Step Over", "STEP_FORWARD"));
            stepOverChildren.add(MenuItemConfig.action("run.step.into", "Step Into", "ARROW_DOWN"));
            stepOverChildren.add(MenuItemConfig.action("run.force.step.into", "Force Step Into", "ANGLE_DOUBLE_DOWN"));
            stepOverChildren.add(MenuItemConfig.action("run.smart.step.into", "Smart Step Into", "ARROW_RIGHT"));
            stepOverChildren.add(MenuItemConfig.action("run.step.out", "Step Out", "ARROW_UP"));
            stepOverChildren.add(MenuItemConfig.action("run.run.to.cursor", "Run to Cursor", "CROSSHAIRS"));
            stepOverChildren.add(MenuItemConfig.action("run.force.run.to.cursor", "Force Run to Cursor", "CROSSHAIRS"));
            stepOverChildren.add(MenuItemConfig.action("run.reset.frame", "Reset Frame", "UNDO"));

            // Pause.Ref
            List<MenuItemConfig> pauseChildren = new ArrayList<>();
            pauseChildren.add(MenuItemConfig.action("run.pause.program", "Pause Program", "PAUSE"));

            // Resume.Ref
            List<MenuItemConfig> resumeChildren = new ArrayList<>();
            resumeChildren.add(MenuItemConfig.action("run.resume.program", "Resume Program", "FORWARD"));

            // Debugging Actions group assembling the above
            List<MenuItemConfig> debuggingActionsChildren = new ArrayList<>();
            debuggingActionsChildren.add(MenuItemConfig.group("run.debug.reload.group", "DebugReloadGroup", false, debugReloadChildren));
            debuggingActionsChildren.add(MenuItemConfig.separator());
            debuggingActionsChildren.add(MenuItemConfig.group("run.step.over.ref", "StepOver.Ref", false, stepOverChildren));
            debuggingActionsChildren.add(MenuItemConfig.group("run.pause.ref", "Pause.Ref", false, pauseChildren));
            debuggingActionsChildren.add(MenuItemConfig.group("run.resume.ref", "Resume.Ref", false, resumeChildren));
            debuggingActionsChildren.add(MenuItemConfig.separator());
            debuggingActionsChildren.add(MenuItemConfig.action("run.evaluate.expression", "Evaluate Expression\u2026", "CALCULATOR"));
            debuggingActionsChildren.add(MenuItemConfig.action("run.show.execution.point", "Show Execution Point", "CROSSHAIRS"));
            debuggingActionsChildren.add(MenuItemConfig.separator());

            // Image 3: Toggle Breakpoint group
            List<MenuItemConfig> toggleBreakpointChildren = new ArrayList<>();
            toggleBreakpointChildren.add(MenuItemConfig.action("run.restore.breakpoint", "Restore Breakpoint"));
            toggleBreakpointChildren.add(MenuItemConfig.action("run.toggle.line.breakpoint", "Toggle Line Breakpoint"));
            toggleBreakpointChildren.add(MenuItemConfig.action("run.toggle.temporary.line.breakpoint", "Toggle Temporary Line Breakpoint"));

            // Debugger Actions group assembling Debugging Actions + Toggle Breakpoint + View Breakpoints...
            List<MenuItemConfig> debuggerActionsChildren = new ArrayList<>();
            debuggerActionsChildren.add(MenuItemConfig.separator());
            debuggerActionsChildren.add(MenuItemConfig.group("run.debugging.actions.group", "Debugging Actions", false, debuggingActionsChildren));
            debuggerActionsChildren.add(MenuItemConfig.group("run.toggle.breakpoint.group", "Toggle Breakpoint", false, toggleBreakpointChildren));
            debuggerActionsChildren.add(MenuItemConfig.action("run.view.breakpoints", "View Breakpoints\u2026", "CIRCLE"));
            debuggerActionsChildren.add(MenuItemConfig.separator());

            // Image 4: RunTestGroup > SmRunTestGroup & CoveragePlatformMenu
            List<MenuItemConfig> smRunTestChildren = new ArrayList<>();
            smRunTestChildren.add(MenuItemConfig.separator());
            smRunTestChildren.add(MenuItemConfig.action("run.test.history", "Test History", "HISTORY"));
            smRunTestChildren.add(MenuItemConfig.action("run.import.tests.from.file", "Import Tests from File\u2026", "DOWNLOAD"));

            List<MenuItemConfig> coverageMenuChildren = new ArrayList<>();
            coverageMenuChildren.add(MenuItemConfig.action("run.manage.coverage.reports", "Manage Coverage Reports\u2026"));
            coverageMenuChildren.add(MenuItemConfig.action("run.generate.coverage.report", "Generate Coverage Report\u2026", "SHARE_SQUARE"));
            coverageMenuChildren.add(MenuItemConfig.action("run.hide.coverage", "Hide Coverage"));
            coverageMenuChildren.add(MenuItemConfig.separator());

            List<MenuItemConfig> coveragePlatformChildren = new ArrayList<>();
            coveragePlatformChildren.add(MenuItemConfig.group("run.coverage.menu", "CoverageMenu", false, coverageMenuChildren));

            List<MenuItemConfig> runTestGroupChildren = new ArrayList<>();
            runTestGroupChildren.add(MenuItemConfig.group("run.sm.run.test.group", "SmRunTestGroup", false, smRunTestChildren));
            runTestGroupChildren.add(MenuItemConfig.group("run.coverage.platform.menu", "CoveragePlatformMenu", false, coveragePlatformChildren));

            // Image 2: ProfilerActions
            List<MenuItemConfig> profilerActionsChildren = new ArrayList<>();
            profilerActionsChildren.add(MenuItemConfig.action("run.attach.profiler.to.process", "Attach Profiler to Process\u2026"));
            profilerActionsChildren.add(MenuItemConfig.action("run.open.profiler.snapshot", "Open Profiler Snapshot"));

            // Full Run Menu assembling Images 1-4
            List<MenuItemConfig> runMenu = new ArrayList<>();
            runMenu.add(MenuItemConfig.group("run.run.debug.group", "Run/Debug", false, runDebugChildren));
            runMenu.add(MenuItemConfig.separator());
            runMenu.add(MenuItemConfig.action("run.run.ellipsis", "Run\u2026", "PLAY"));
            runMenu.add(MenuItemConfig.action("run.debug.ellipsis", "Debug\u2026", "BUG"));
            runMenu.add(MenuItemConfig.group("run.xdebugger.attach.group", "XDebugger.AttachGroup", false, attachGroupChildren));
            runMenu.add(MenuItemConfig.action("run.edit.configurations", "Edit Configurations\u2026"));
            runMenu.add(MenuItemConfig.action("run.manage.targets", "Manage Targets\u2026"));
            runMenu.add(MenuItemConfig.separator());
            runMenu.add(MenuItemConfig.action("run.stop", "Stop", "STOP"));
            runMenu.add(MenuItemConfig.action("run.stop.background.processes", "Stop Background Processes\u2026"));
            runMenu.add(MenuItemConfig.action("run.show.running.list", "Show Running List"));
            runMenu.add(MenuItemConfig.group("run.debugger.actions.group", "Debugger Actions", false, debuggerActionsChildren));
            runMenu.add(MenuItemConfig.group("run.test.group", "RunTestGroup", false, runTestGroupChildren));
            runMenu.add(MenuItemConfig.group("run.profiler.actions.group", "ProfilerActions", false, profilerActionsChildren));
            mainMenuChildren.add(MenuItemConfig.group("menu.run", "Run", true, runMenu));

            // Tools Menu (Image 1)
            List<MenuItemConfig> psiViewerChildren = new ArrayList<>();
            psiViewerChildren.add(MenuItemConfig.separator());
            psiViewerChildren.add(MenuItemConfig.action("tools.view.psi.structure", "View PSI Structure\u2026"));
            psiViewerChildren.add(MenuItemConfig.action("tools.view.psi.structure.current.file", "View PSI Structure of Current File\u2026"));
            psiViewerChildren.add(MenuItemConfig.separator());

            List<MenuItemConfig> xmlActionsChildren = new ArrayList<>();
            xmlActionsChildren.add(MenuItemConfig.action("tools.convert.schema", "Convert Schema\u2026"));

            List<MenuItemConfig> toolsMarkdownChildren = new ArrayList<>();
            toolsMarkdownChildren.add(MenuItemConfig.action("tools.markdown.import.word", "Import Word Document\u2026"));
            toolsMarkdownChildren.add(MenuItemConfig.action("tools.markdown.export.file.to", "Export Markdown File To\u2026"));
            toolsMarkdownChildren.add(MenuItemConfig.action("tools.markdown.configure.pandoc", "Configure Pandoc\u2026"));

            List<MenuItemConfig> toolsMenu = new ArrayList<>();
            toolsMenu.add(MenuItemConfig.group("tools.psi.viewer.actions", "Dev.PsiViewerActions", false, psiViewerChildren));
            toolsMenu.add(MenuItemConfig.group("tools.basic.group", "Tools Basic Group", false, List.of()));
            toolsMenu.add(MenuItemConfig.action("tools.create.command.line.launcher", "Create Command Line Launcher\u2026"));
            toolsMenu.add(MenuItemConfig.action("tools.create.desktop.entry", "Create Desktop Entry\u2026"));
            toolsMenu.add(MenuItemConfig.separator());
            toolsMenu.add(MenuItemConfig.group("tools.other.menu", "OtherMenu", false, List.of()));
            toolsMenu.add(MenuItemConfig.action("tools.services", "Services"));
            toolsMenu.add(MenuItemConfig.group("tools.xml.actions", "XML Actions", true, xmlActionsChildren));
            toolsMenu.add(MenuItemConfig.group("tools.markdown", "Markdown", true, toolsMarkdownChildren));
            toolsMenu.add(MenuItemConfig.group("tools.external.tools", "External Tools", true, List.of()));
            mainMenuChildren.add(MenuItemConfig.group("menu.tools", "Tools", true, toolsMenu));

            // Git Menu (Images 2-5)
            // Image 4: Git.FileActions
            List<MenuItemConfig> gitFileActionsChildren = new ArrayList<>();
            gitFileActionsChildren.add(MenuItemConfig.action("git.commit.file", "Commit File"));
            gitFileActionsChildren.add(MenuItemConfig.action("git.add", "Add", "PLUS"));
            gitFileActionsChildren.add(MenuItemConfig.action("git.add.to.gitignore", "Add to .gitignore", "BAN"));
            gitFileActionsChildren.add(MenuItemConfig.separator());
            gitFileActionsChildren.add(MenuItemConfig.action("git.annotate", "Annotate"));
            gitFileActionsChildren.add(MenuItemConfig.action("git.compare.same.version", "Compare with Same Repository Version", "COMPARE"));
            gitFileActionsChildren.add(MenuItemConfig.action("git.compare.with.revision", "Compare with Revision\u2026"));
            gitFileActionsChildren.add(MenuItemConfig.action("git.compare.with.branch", "Compare with Branch or Tag\u2026"));
            gitFileActionsChildren.add(MenuItemConfig.action("git.show.history", "Show History", "HISTORY"));
            gitFileActionsChildren.add(MenuItemConfig.action("git.show.history.for.selection", "Show History for Selection\u2026"));

            // Image 5: GitRepositoryActions
            List<MenuItemConfig> mergeGroupChildren = new ArrayList<>();
            mergeGroupChildren.add(MenuItemConfig.action("git.abort.merge", "Abort Merge"));

            List<MenuItemConfig> rebaseGroupChildren = new ArrayList<>();
            rebaseGroupChildren.add(MenuItemConfig.action("git.abort.rebase", "Abort Rebase"));
            rebaseGroupChildren.add(MenuItemConfig.action("git.continue.rebase", "Continue Rebase"));
            rebaseGroupChildren.add(MenuItemConfig.action("git.skip.commit", "Skip Commit"));

            List<MenuItemConfig> gitRepoActionsChildren = new ArrayList<>();
            gitRepoActionsChildren.add(MenuItemConfig.action("git.push", "Push\u2026", "UPLOAD"));
            gitRepoActionsChildren.add(MenuItemConfig.action("git.pull", "Pull\u2026"));
            gitRepoActionsChildren.add(MenuItemConfig.action("git.fetch", "Fetch", "DOWNLOAD"));
            gitRepoActionsChildren.add(MenuItemConfig.separator());
            gitRepoActionsChildren.add(MenuItemConfig.action("git.merge", "Merge\u2026", "BRANCHES"));
            gitRepoActionsChildren.add(MenuItemConfig.group("git.merge.group", "Merge", true, mergeGroupChildren));
            gitRepoActionsChildren.add(MenuItemConfig.action("git.rebase", "Rebase\u2026"));
            gitRepoActionsChildren.add(MenuItemConfig.group("git.rebase.group", "Rebase", true, rebaseGroupChildren));
            gitRepoActionsChildren.add(MenuItemConfig.separator());
            gitRepoActionsChildren.add(MenuItemConfig.action("git.branches", "Branches\u2026", "BRANCHES"));
            gitRepoActionsChildren.add(MenuItemConfig.action("git.new.branch", "New Branch\u2026", "PLUS"));
            gitRepoActionsChildren.add(MenuItemConfig.action("git.new.tag", "New Tag\u2026"));
            gitRepoActionsChildren.add(MenuItemConfig.action("git.reset.head", "Reset HEAD\u2026"));
            gitRepoActionsChildren.add(MenuItemConfig.separator());
            gitRepoActionsChildren.add(MenuItemConfig.action("git.stash.changes", "Stash Changes\u2026"));
            gitRepoActionsChildren.add(MenuItemConfig.action("git.unstash.changes", "Unstash Changes\u2026"));
            gitRepoActionsChildren.add(MenuItemConfig.separator());
            gitRepoActionsChildren.add(MenuItemConfig.action("git.manage.remotes", "Manage Remotes\u2026"));
            gitRepoActionsChildren.add(MenuItemConfig.action("git.clone", "Clone\u2026"));
            gitRepoActionsChildren.add(MenuItemConfig.separator());
            gitRepoActionsChildren.add(MenuItemConfig.action("git.abort.revert", "Abort Revert"));
            gitRepoActionsChildren.add(MenuItemConfig.action("git.abort.cherry.pick", "Abort Cherry-Pick"));

            // Image 3: Git group inside VCS Group
            List<MenuItemConfig> vcsGitGroupChildren = new ArrayList<>();
            vcsGitGroupChildren.add(MenuItemConfig.group("git.file.actions", "Git.FileActions", false, gitFileActionsChildren));
            vcsGitGroupChildren.add(MenuItemConfig.separator());
            vcsGitGroupChildren.add(MenuItemConfig.action("git.rollback", "Rollback\u2026", "UNDO"));
            vcsGitGroupChildren.add(MenuItemConfig.separator());
            vcsGitGroupChildren.add(MenuItemConfig.group("git.repository.actions", "GitRepositoryActions", false, gitRepoActionsChildren));
            vcsGitGroupChildren.add(MenuItemConfig.separator());
            vcsGitGroupChildren.add(MenuItemConfig.action("git.create.patch", "Create Patch from Local Changes\u2026", "PLUS"));
            vcsGitGroupChildren.add(MenuItemConfig.action("git.apply.patch", "Apply Patch\u2026"));
            vcsGitGroupChildren.add(MenuItemConfig.action("git.apply.patch.from.clipboard", "Apply Patch from Clipboard\u2026"));
            vcsGitGroupChildren.add(MenuItemConfig.action("git.shelve.changes", "Shelve Changes\u2026", "SAVE"));

            // Image 3: VCS Group
            List<MenuItemConfig> vcsGroupChildren = new ArrayList<>();
            vcsGroupChildren.add(MenuItemConfig.action("vcs.operations.popup", "VCS Operations Popup\u2026"));
            vcsGroupChildren.add(MenuItemConfig.action("vcs.commit", "Commit\u2026", "CHECK_CIRCLE"));
            vcsGroupChildren.add(MenuItemConfig.action("vcs.toggle.commit.ui", "Toggle Commit UI\u2026"));
            vcsGroupChildren.add(MenuItemConfig.action("vcs.update.project", "Update Project", "DOWNLOAD"));
            vcsGroupChildren.add(MenuItemConfig.action("vcs.integrate.project", "Integrate Project"));
            vcsGroupChildren.add(MenuItemConfig.action("vcs.refresh", "Refresh", "REFRESH"));
            vcsGroupChildren.add(MenuItemConfig.action("vcs.show.local.changes.uml", "Show Local Changes as UML", "DIAGRAM"));
            vcsGroupChildren.add(MenuItemConfig.separator());
            vcsGroupChildren.add(MenuItemConfig.group("vcs.specific", "Vcs.Specific", false, List.of()));
            vcsGroupChildren.add(MenuItemConfig.group("vcs.git.group", "Git", false, vcsGitGroupChildren));

            // Image 1: Browse VCS Repository and Import into Version Control
            List<MenuItemConfig> browseVcsRepoChildren = new ArrayList<>();
            browseVcsRepoChildren.add(MenuItemConfig.action("vcs.browse.git.log", "Show Git Repository Log\u2026"));

            List<MenuItemConfig> importVcsChildren = new ArrayList<>();
            importVcsChildren.add(MenuItemConfig.action("vcs.create.git.repository", "Create Git Repository\u2026"));

            // Image 2: Vcs.MainMenu
            List<MenuItemConfig> vcsMainMenuChildren = new ArrayList<>();
            vcsMainMenuChildren.add(MenuItemConfig.action("vcs.enable.integration", "Enable Version Control Integration\u2026"));
            vcsMainMenuChildren.add(MenuItemConfig.separator());
            vcsMainMenuChildren.add(MenuItemConfig.group("vcs.group", "VCS Group", false, vcsGroupChildren));
            vcsMainMenuChildren.add(MenuItemConfig.separator());
            vcsMainMenuChildren.add(MenuItemConfig.action("vcs.get.from.vcs", "Get from Version Control\u2026"));
            vcsMainMenuChildren.add(MenuItemConfig.group("vcs.browse.repository", "Browse VCS Repository", true, browseVcsRepoChildren));
            vcsMainMenuChildren.add(MenuItemConfig.separator());
            vcsMainMenuChildren.add(MenuItemConfig.group("vcs.import.into.vcs", "Import into Version Control", true, importVcsChildren));

            // Git.MainMenu (Images 1-4)
            // Image 3: Patch group under Git.MainMenu
            List<MenuItemConfig> patchGroupChildren = new ArrayList<>();
            patchGroupChildren.add(MenuItemConfig.action("git.create.patch", "Create Patch from Local Changes\u2026", "PLUS"));
            patchGroupChildren.add(MenuItemConfig.action("git.apply.patch", "Apply Patch\u2026"));
            patchGroupChildren.add(MenuItemConfig.action("git.apply.patch.from.clipboard", "Apply Patch from Clipboard\u2026"));

            // Image 4: Uncommitted Changes group under Git.MainMenu
            List<MenuItemConfig> umlDiffChildren = new ArrayList<>();
            umlDiffChildren.add(MenuItemConfig.action("vcs.show.local.changes.uml", "Show Local Changes as UML", "DIAGRAM"));

            List<MenuItemConfig> uncommittedChangesChildren = new ArrayList<>();
            uncommittedChangesChildren.add(MenuItemConfig.action("git.shelve.changes", "Shelve Changes\u2026", "SAVE"));
            uncommittedChangesChildren.add(MenuItemConfig.action("git.show.shelf", "Show Shelf"));
            uncommittedChangesChildren.add(MenuItemConfig.action("git.show.git.stash", "Show Git Stash"));
            uncommittedChangesChildren.add(MenuItemConfig.action("git.stash.changes", "Stash Changes\u2026"));
            uncommittedChangesChildren.add(MenuItemConfig.action("git.unstash.changes", "Unstash Changes\u2026"));
            uncommittedChangesChildren.add(MenuItemConfig.action("git.rollback", "Rollback\u2026", "UNDO"));
            uncommittedChangesChildren.add(MenuItemConfig.group("vcs.uml.diff", "Vcs.UmlDiff", false, umlDiffChildren));

            // Image 4: Git.MainMenu.FileActions > Git.FileActions
            List<MenuItemConfig> gitMainMenuFileActionsChildren = new ArrayList<>();
            gitMainMenuFileActionsChildren.add(MenuItemConfig.group("git.file.actions.ref", "Git.FileActions", false, gitFileActionsChildren));

            // Merge & Rebase popup groups for Git.MainMenu
            List<MenuItemConfig> gitMainMergeChildren = new ArrayList<>();
            gitMainMergeChildren.add(MenuItemConfig.action("git.abort.merge", "Abort Merge"));

            List<MenuItemConfig> gitMainRebaseChildren = new ArrayList<>();
            gitMainRebaseChildren.add(MenuItemConfig.action("git.abort.rebase", "Abort Rebase"));
            gitMainRebaseChildren.add(MenuItemConfig.action("git.continue.rebase", "Continue Rebase"));
            gitMainRebaseChildren.add(MenuItemConfig.action("git.skip.commit", "Skip Commit"));

            List<MenuItemConfig> gitMainMenuChildren = new ArrayList<>();
            gitMainMenuChildren.add(MenuItemConfig.action("git.main.commit", "Commit\u2026", "CHECK_CIRCLE"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.main.toggle.commit.ui", "Commit\u2026", "CHECK_CIRCLE"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.push", "Push\u2026", "UPLOAD"));
            gitMainMenuChildren.add(MenuItemConfig.action("vcs.update.project", "Update Project", "DOWNLOAD"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.pull", "Pull\u2026"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.fetch", "Fetch", "DOWNLOAD"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.unshallow", "Unshallow repository"));
            gitMainMenuChildren.add(MenuItemConfig.separator());
            gitMainMenuChildren.add(MenuItemConfig.action("git.merge", "Merge\u2026", "BRANCHES"));
            gitMainMenuChildren.add(MenuItemConfig.group("git.main.merge.group", "Merge", true, gitMainMergeChildren));
            gitMainMenuChildren.add(MenuItemConfig.action("git.rebase", "Rebase\u2026"));
            gitMainMenuChildren.add(MenuItemConfig.group("git.main.rebase.group", "Rebase", true, gitMainRebaseChildren));
            gitMainMenuChildren.add(MenuItemConfig.action("git.resolve.conflicts", "Resolve Conflicts\u2026"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.revert.resolved", "Revert Resolved", "UNDO"));
            gitMainMenuChildren.add(MenuItemConfig.separator());
            gitMainMenuChildren.add(MenuItemConfig.action("git.branches", "Branches\u2026", "BRANCHES"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.new.branch", "New Branch\u2026", "PLUS"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.new.tag", "New Tag\u2026"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.reset.head", "Reset HEAD\u2026"));
            gitMainMenuChildren.add(MenuItemConfig.separator());
            gitMainMenuChildren.add(MenuItemConfig.action("git.show.vcs.log", "Show VCS Log", "BRANCHES"));
            gitMainMenuChildren.add(MenuItemConfig.group("git.patch.group", "Patch", true, patchGroupChildren));
            gitMainMenuChildren.add(MenuItemConfig.group("git.uncommitted.changes.group", "Uncommitted Changes", true, uncommittedChangesChildren));
            gitMainMenuChildren.add(MenuItemConfig.group("git.main.menu.file.actions", "Git.MainMenu.FileActions", false, gitMainMenuFileActionsChildren));
            gitMainMenuChildren.add(MenuItemConfig.separator());
            gitMainMenuChildren.add(MenuItemConfig.action("git.manage.remotes", "Manage Remotes\u2026"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.clone", "Clone\u2026"));
            gitMainMenuChildren.add(MenuItemConfig.separator());
            gitMainMenuChildren.add(MenuItemConfig.action("vcs.operations.popup", "VCS Operations Popup\u2026"));
            gitMainMenuChildren.add(MenuItemConfig.separator());
            gitMainMenuChildren.add(MenuItemConfig.action("git.abort.revert", "Abort Revert"));
            gitMainMenuChildren.add(MenuItemConfig.action("git.abort.cherry.pick", "Abort Cherry-Pick"));

            // Git top-level menu assembling Images 1-4
            List<MenuItemConfig> gitMenu = new ArrayList<>();
            gitMenu.add(MenuItemConfig.group("vcs.main.menu", "Vcs.MainMenu", false, vcsMainMenuChildren));
            gitMenu.add(MenuItemConfig.group("git.main.menu", "Git.MainMenu", false, gitMainMenuChildren));
            mainMenuChildren.add(MenuItemConfig.group("menu.vcs", "Git", true, gitMenu));

            // -------------------------------------------------------------
            // Main Menu > Window (Exact match to DataGrip Images 1-5)
            // -------------------------------------------------------------
            // Image 2: Tool Window Layouts (Group)
            List<MenuItemConfig> toolWindowLayoutsChildren = new ArrayList<>();
            toolWindowLayoutsChildren.add(MenuItemConfig.action("window.layouts.default", "Default"));
            toolWindowLayoutsChildren.add(MenuItemConfig.separator());
            toolWindowLayoutsChildren.add(MenuItemConfig.action("window.layouts.list", "Tool Window Layout List"));
            toolWindowLayoutsChildren.add(MenuItemConfig.separator());
            toolWindowLayoutsChildren.add(MenuItemConfig.action("window.layouts.restore.current", "Restore Current Layout"));
            toolWindowLayoutsChildren.add(MenuItemConfig.action("window.layouts.save.changes.current", "Save Changes in Current Layout"));
            toolWindowLayoutsChildren.add(MenuItemConfig.action("window.layouts.save.as.new", "Save Current Layout as New\u2026"));

            // Image 3: Active Tool Window > Resize (Group)
            List<MenuItemConfig> twResizeChildren = new ArrayList<>();
            twResizeChildren.add(MenuItemConfig.action("window.tw.resize.left", "Stretch to Left"));
            twResizeChildren.add(MenuItemConfig.action("window.tw.resize.right", "Stretch to Right"));
            twResizeChildren.add(MenuItemConfig.action("window.tw.resize.top", "Stretch to Top"));
            twResizeChildren.add(MenuItemConfig.action("window.tw.resize.bottom", "Stretch to Bottom"));

            // Image 3: Active Tool Window (Group)
            List<MenuItemConfig> activeToolWindowChildren = new ArrayList<>();
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.hide.active", "Hide Active Tool Window"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.hide.side", "Hide Side Tool Windows"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.hide.all", "Hide All Tool Windows"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.open.as.editor.tab", "Open as Editor Tab", "EXTERNAL_LINK_ALT"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.pin.tab", "Pin Active Tool Window Tab"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.close.active.tab", "Close Active Tab"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.jump.last", "Jump to Last Tool Window"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.maximize", "Maximize Tool Window"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.dock", "Dock"));
            activeToolWindowChildren.add(MenuItemConfig.separator());
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.view.mode", "View Mode"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.move.to", "Move to"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.group.tabs", "Group Tabs"));
            activeToolWindowChildren.add(MenuItemConfig.action("window.toolwindow.show.list.of.tabs", "Show List of Tabs"));
            activeToolWindowChildren.add(MenuItemConfig.group("window.toolwindow.resize", "Resize", true, twResizeChildren));

            // Image 4: Editor Tabs > Editor Close Actions (Group)
            List<MenuItemConfig> editorCloseActionsChildren = new ArrayList<>();
            editorCloseActionsChildren.add(MenuItemConfig.action("window.close.tab", "Close Tab"));
            editorCloseActionsChildren.add(MenuItemConfig.action("window.close.other.tabs", "Close Other Tabs"));
            editorCloseActionsChildren.add(MenuItemConfig.action("window.close.all.tabs", "Close All Tabs"));
            editorCloseActionsChildren.add(MenuItemConfig.action("window.close.unmodified.tabs", "Close Unmodified Tabs"));
            editorCloseActionsChildren.add(MenuItemConfig.action("window.close.all.but.pinned", "Close All but Pinned"));
            editorCloseActionsChildren.add(MenuItemConfig.action("window.close.tabs.left", "Close Tabs to the Left"));
            editorCloseActionsChildren.add(MenuItemConfig.action("window.close.tabs.right", "Close Tabs to the Right"));
            editorCloseActionsChildren.add(MenuItemConfig.action("window.close.all.readonly", "Close All Read-Only Tabs"));
            editorCloseActionsChildren.add(MenuItemConfig.action("window.editor.open.as.editor.tab", "Open as Editor Tab", "EXTERNAL_LINK_ALT"));

            // Image 5: Editor Tabs > Split with Chooser Navigation (Group)
            List<MenuItemConfig> splitChooserNavChildren = new ArrayList<>();
            splitChooserNavChildren.add(MenuItemConfig.action("window.split.next", "Next Split"));
            splitChooserNavChildren.add(MenuItemConfig.action("window.split.prev", "Previous Split"));
            splitChooserNavChildren.add(MenuItemConfig.action("window.split.exit.chooser", "Exit Chooser"));
            splitChooserNavChildren.add(MenuItemConfig.action("window.split.chooser.split", "Split"));
            splitChooserNavChildren.add(MenuItemConfig.action("window.split.chooser.duplicate", "Duplicate"));
            splitChooserNavChildren.add(MenuItemConfig.action("window.split.chooser.without.split", "Without Split"));
            splitChooserNavChildren.add(MenuItemConfig.action("window.split.switch.up", "Use Top Split or Switch Up"));
            splitChooserNavChildren.add(MenuItemConfig.action("window.split.switch.left", "Use Left Split or Switch Left"));
            splitChooserNavChildren.add(MenuItemConfig.action("window.split.switch.down", "Use down Split or Switch Down"));
            splitChooserNavChildren.add(MenuItemConfig.action("window.split.switch.right", "Use Right Split or Switch Right"));

            // Images 4 & 5: Editor Tabs (Group)
            List<MenuItemConfig> editorTabsChildren = new ArrayList<>();
            editorTabsChildren.add(MenuItemConfig.action("window.editor.next.tab", "Select Next Tab"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.prev.tab", "Select Previous Tab"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.pin.tab", "Pin Active Tab"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.keep.tab.open", "Keep Tab Open"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.show.hidden.tabs", "Show Hidden Tabs"));
            editorTabsChildren.add(MenuItemConfig.separator());
            editorTabsChildren.add(MenuItemConfig.group("window.editor.close.actions", "Editor Close Actions", true, editorCloseActionsChildren));
            editorTabsChildren.add(MenuItemConfig.separator());
            editorTabsChildren.add(MenuItemConfig.action("window.reopen.tab", "Reopen Closed Tab"));
            editorTabsChildren.add(MenuItemConfig.action("window.split.right", "Split Right", "COLUMNS"));
            editorTabsChildren.add(MenuItemConfig.action("window.split.and.move.right", "Split and Move Right"));
            editorTabsChildren.add(MenuItemConfig.action("window.split.down", "Split Down", "COLUMNS"));
            editorTabsChildren.add(MenuItemConfig.action("window.split.and.move.down", "Split and Move Down"));
            editorTabsChildren.add(MenuItemConfig.action("window.split.chooser.open", "Open in Split with Chooser\u2026"));
            editorTabsChildren.add(MenuItemConfig.group("window.editor.split.chooser.navigation", "Split with Chooser Navigation", true, splitChooserNavChildren));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.stretch.top", "Stretch Editor to Top"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.stretch.left", "Stretch Editor to Left"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.stretch.bottom", "Stretch Editor to Bottom"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.stretch.right", "Stretch Editor to Right"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.change.splitter.orientation", "Change Splitter Orientation"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.maximize.splits", "Maximize Editor/Normalize Splits"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.unsplit", "Unsplit"));
            editorTabsChildren.add(MenuItemConfig.action("window.unsplit.all", "Unsplit All"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.goto.next.splitter", "Goto Next Splitter"));
            editorTabsChildren.add(MenuItemConfig.action("window.editor.goto.prev.splitter", "Goto Previous Splitter"));
            editorTabsChildren.add(MenuItemConfig.separator());
            editorTabsChildren.add(MenuItemConfig.action("window.editor.configure.tabs", "Configure Editor Tabs\u2026"));

            // Image 3: Notifications (Group)
            List<MenuItemConfig> notificationsChildren = new ArrayList<>();
            notificationsChildren.add(MenuItemConfig.action("window.notifications.close.first", "Close First"));
            notificationsChildren.add(MenuItemConfig.action("window.notifications.close.all", "Close All"));

            // Image 3: Background Tasks (Group)
            List<MenuItemConfig> backgroundTasksChildren = new ArrayList<>();
            backgroundTasksChildren.add(MenuItemConfig.action("window.background.tasks.show", "Show"));
            backgroundTasksChildren.add(MenuItemConfig.action("window.background.tasks.auto.show", "Auto Show"));

            // Image 2: Open Project Windows (Group)
            List<MenuItemConfig> openProjectWindowsChildren = new ArrayList<>();
            openProjectWindowsChildren.add(MenuItemConfig.action("window.next.project", "Next Project Window"));
            openProjectWindowsChildren.add(MenuItemConfig.action("window.prev.project", "Previous Project Window"));
            openProjectWindowsChildren.add(MenuItemConfig.action("window.project.merge.all", "Merge All Project Windows"));
            openProjectWindowsChildren.add(MenuItemConfig.separator());

            // Image 1: Window Top-Level Menu
            List<MenuItemConfig> winMenu = new ArrayList<>();
            winMenu.add(MenuItemConfig.action("window.minimize", "Minimize"));
            winMenu.add(MenuItemConfig.action("window.zoom", "Zoom"));
            winMenu.add(MenuItemConfig.group("window.layouts", "Tool Window Layouts", true, toolWindowLayoutsChildren));
            winMenu.add(MenuItemConfig.separator());
            winMenu.add(MenuItemConfig.group("window.active.tool.window", "Active Tool Window", true, activeToolWindowChildren));
            winMenu.add(MenuItemConfig.group("window.editor.tabs", "Editor Tabs", true, editorTabsChildren));
            winMenu.add(MenuItemConfig.group("window.notifications", "Notifications", true, notificationsChildren));
            winMenu.add(MenuItemConfig.group("window.background.tasks", "Background Tasks", true, backgroundTasksChildren));
            winMenu.add(MenuItemConfig.separator());
            winMenu.add(MenuItemConfig.group("window.open.project.windows", "Open Project Windows", true, openProjectWindowsChildren));
            mainMenuChildren.add(MenuItemConfig.group("menu.window", "Window", true, winMenu));

            // Help Menu (Matching DataGrip Images 1-4)
            List<MenuItemConfig> helpMenu = new ArrayList<>();

            // 1. Find Action...
            helpMenu.add(MenuItemConfig.action("help.find.action", "Find Action\u2026"));
            helpMenu.add(MenuItemConfig.separator());

            // 2. Help
            MenuItemConfig helpActionItem = MenuItemConfig.action("help.help", "Help");
            helpActionItem.setIconName("QUESTION_CIRCLE");
            helpMenu.add(helpActionItem);
            helpMenu.add(MenuItemConfig.separator());

            // 3. LearnGroup (non-popup group, Image 1)
            List<MenuItemConfig> learnGroupChildren = new ArrayList<>();
            MenuItemConfig learnFeaturesItem = MenuItemConfig.action("help.learn.features", "Learn IDE Features");
            learnFeaturesItem.setIconName("GRADUATION_CAP");
            learnGroupChildren.add(learnFeaturesItem);
            helpMenu.add(MenuItemConfig.group("help.learn.group", "LearnGroup", false, learnGroupChildren));
            helpMenu.add(MenuItemConfig.separator());

            // 4. Learning & Documentation items
            helpMenu.add(MenuItemConfig.action("help.whats.new", "What's New"));
            helpMenu.add(MenuItemConfig.action("help.configure.new.ui", "Configure the New UI"));
            helpMenu.add(MenuItemConfig.action("help.getting.started", "Getting Started"));
            helpMenu.add(MenuItemConfig.action("help.youtube", "DataGrip on YouTube"));
            helpMenu.add(MenuItemConfig.action("help.shortcuts.pdf", "Keyboard Shortcuts PDF"));

            // Image 2: ProductivityFeatures (popup submenu)
            List<MenuItemConfig> productivityChildren = new ArrayList<>();
            productivityChildren.add(MenuItemConfig.action("help.tip.of.the.day", "Tip of the Day"));
            productivityChildren.add(MenuItemConfig.separator());
            productivityChildren.add(MenuItemConfig.action("help.my.productivity", "My Productivity"));
            helpMenu.add(MenuItemConfig.group("help.productivity.features", "ProductivityFeatures", true, productivityChildren));
            helpMenu.add(MenuItemConfig.separator());

            // 5. Support & Feedback
            helpMenu.add(MenuItemConfig.action("help.contact.support", "Contact Support\u2026"));
            helpMenu.add(MenuItemConfig.action("help.bug.report", "Submit a Bug Report\u2026"));
            helpMenu.add(MenuItemConfig.action("help.submit.feedback", "Submit Feedback\u2026"));
            helpMenu.add(MenuItemConfig.separator());

            // 6. Logs & Diagnostics
            helpMenu.add(MenuItemConfig.action("help.show.log.in.files", "Show Log in Finder"));
            helpMenu.add(MenuItemConfig.action("help.show.sql.log.in.files", "Show SQL Log"));
            helpMenu.add(MenuItemConfig.action("help.collect.logs", "Collect Logs and Diagnostic Data"));
            helpMenu.add(MenuItemConfig.action("help.delete.leftover.dirs", "Delete Leftover IDE Directories\u2026"));
            helpMenu.add(MenuItemConfig.separator());

            // 7. Diagnostic Tools (popup submenu, Images 2 & 3)
            List<MenuItemConfig> diagnosticChildren = new ArrayList<>();
            diagnosticChildren.add(MenuItemConfig.action("help.diagnostic.activity.monitor", "Activity Monitor\u2026"));
            diagnosticChildren.add(MenuItemConfig.action("help.diagnostic.dump.threads", "Dump Threads"));
            diagnosticChildren.add(MenuItemConfig.action("help.diagnostic.run.memory.tester", "Run Memory Tester\u2026"));
            diagnosticChildren.add(MenuItemConfig.separator());
            diagnosticChildren.add(MenuItemConfig.action("help.diagnostic.debug.log.settings", "Debug Log Settings\u2026"));
            diagnosticChildren.add(MenuItemConfig.action("help.diagnostic.special.files", "Special Files and Folders"));

            // Image 3: StartProfileGroup -> AsyncGroup
            List<MenuItemConfig> asyncGroupChildren = new ArrayList<>();
            asyncGroupChildren.add(MenuItemConfig.separator());
            MenuItemConfig cpuProfilingItem = MenuItemConfig.action("help.diagnostic.start.cpu.profiling", "Start CPU Usage Profiling");
            cpuProfilingItem.setIconName("TACHOMETER_ALT");
            asyncGroupChildren.add(cpuProfilingItem);
            asyncGroupChildren.add(MenuItemConfig.action("help.diagnostic.start.async.profiler", "Start Async Profiler"));

            List<MenuItemConfig> startProfileChildren = new ArrayList<>();
            startProfileChildren.add(MenuItemConfig.group("help.diagnostic.async.group", "AsyncGroup", false, asyncGroupChildren));
            diagnosticChildren.add(MenuItemConfig.group("help.diagnostic.start.profile.group", "StartProfileGroup", false, startProfileChildren));

            // Image 3: DiagnosticGroup -> AsyncDiagnosticGroup
            List<MenuItemConfig> diagnosticGroupChildren = new ArrayList<>();
            diagnosticGroupChildren.add(MenuItemConfig.separator());
            MenuItemConfig memorySnapshotItem = MenuItemConfig.action("help.diagnostic.capture.memory.snapshot", "Capture Memory Snapshot");
            memorySnapshotItem.setIconName("CAMERA");
            diagnosticGroupChildren.add(memorySnapshotItem);
            diagnosticGroupChildren.add(MenuItemConfig.separator());

            List<MenuItemConfig> asyncDiagChildren = new ArrayList<>();
            asyncDiagChildren.add(MenuItemConfig.action("help.diagnostic.profile.indexing", "Profile Indexing"));
            diagnosticGroupChildren.add(MenuItemConfig.group("help.diagnostic.async.diagnostic.group", "AsyncDiagnosticGroup", false, asyncDiagChildren));
            diagnosticChildren.add(MenuItemConfig.group("help.diagnostic.diagnostic.group", "DiagnosticGroup", false, diagnosticGroupChildren));

            // Image 3: IndexingDiagnosticGroup
            List<MenuItemConfig> indexingDiagnosticChildren = new ArrayList<>();
            indexingDiagnosticChildren.add(MenuItemConfig.action("help.diagnostic.open.indexing.diagnostics", "Open Indexing Diagnostics"));
            diagnosticChildren.add(MenuItemConfig.group("help.diagnostic.indexing.diagnostic.group", "IndexingDiagnosticGroup", false, indexingDiagnosticChildren));

            helpMenu.add(MenuItemConfig.group("help.diagnostic.tools", "Diagnostic Tools", true, diagnosticChildren));

            // Siblings of Diagnostic Tools under Help
            helpMenu.add(MenuItemConfig.action("help.change.memory.settings", "Change Memory Settings"));
            helpMenu.add(MenuItemConfig.action("help.custom.properties", "Edit Custom Properties\u2026"));
            helpMenu.add(MenuItemConfig.action("help.custom.vm.options", "Edit Custom VM Options\u2026"));
            helpMenu.add(MenuItemConfig.separator());

            // 8. Registration Actions popup (Image 4)
            List<MenuItemConfig> registrationChildren = new ArrayList<>();
            registrationChildren.add(MenuItemConfig.action("help.registration.register", "Register\u2026"));
            helpMenu.add(MenuItemConfig.group("help.registration.actions", "Registration Actions", true, registrationChildren));

            // 9. Updates & About (Image 4)
            MenuItemConfig updatesItem = MenuItemConfig.action("help.updates", "Check for Updates\u2026");
            updatesItem.setIconName("DOWNLOAD");
            helpMenu.add(updatesItem);
            MenuItemConfig aboutItem = MenuItemConfig.action("help.about", "About");
            aboutItem.setIconName("INFO_CIRCLE");
            helpMenu.add(aboutItem);
            helpMenu.add(MenuItemConfig.separator());

            mainMenuChildren.add(MenuItemConfig.group("menu.help", "Help", true, helpMenu));

            roots.add(MenuItemConfig.group("root.main.menu", "Main Menu", mainMenuChildren));

            // 2. Main Toolbar
            List<MenuItemConfig> tbLeft = new ArrayList<>();
            tbLeft.add(MenuItemConfig.action("toolbar.project.widget", "Project Widget", "FOLDER"));
            List<MenuItemConfig> vcsGroup = new ArrayList<>();
            vcsGroup.add(MenuItemConfig.action("toolbar.vcs.widget", "VCS Widget", "CODE_BRANCH"));
            vcsGroup.add(MenuItemConfig.action("toolbar.vcs.merge.rebase.widget", "VCS Merge/Rebase Widget", "CODE_BRANCH"));
            tbLeft.add(MenuItemConfig.group("toolbar.vcs.group", "VCS Group", true, vcsGroup));
            List<MenuItemConfig> genActions = new ArrayList<>();
            genActions.add(MenuItemConfig.separator());
            tbLeft.add(MenuItemConfig.group("toolbar.general.actions.group", "General Actions", false, genActions));

            List<MenuItemConfig> tbCenter = new ArrayList<>();
            tbCenter.add(MenuItemConfig.action("toolbar.single.tool.window.bar", "Single Tool Window Bar", "DESKTOP"));
            tbCenter.add(MenuItemConfig.action("toolbar.file.name.widget", "File Name Widget (when editor tabs are hidden or \"Always show full path\" is enabled)", "FILE_ALT"));

            List<MenuItemConfig> tbRight = new ArrayList<>();
            tbRight.add(MenuItemConfig.group("toolbar.execution.targets.group", "ExecutionTargetsToolbarGroup", true, "FOLDER", new ArrayList<>()));
            tbRight.add(MenuItemConfig.action("toolbar.ai.assistant.action", "AIAssistantHubPopupAction", "ROBOT"));
            tbRight.add(MenuItemConfig.action("header.search", "Search Everywhere", "SEARCH"));
            tbRight.add(MenuItemConfig.action("header.settings", "IDE and Project Settings", "COG"));

            List<MenuItemConfig> toolbarChildren = new ArrayList<>();
            toolbarChildren.add(MenuItemConfig.group("toolbar.left", "Left", tbLeft));
            toolbarChildren.add(MenuItemConfig.group("toolbar.center", "Center", tbCenter));
            toolbarChildren.add(MenuItemConfig.group("toolbar.right", "Right", tbRight));
            roots.add(MenuItemConfig.group("root.main.toolbar", "Main Toolbar", toolbarChildren));

            // 3. Editor Popup Menu
            List<MenuItemConfig> editorPopup = new ArrayList<>();

            // ShowIntentionsGroup
            List<MenuItemConfig> intentionsGroup = new ArrayList<>();
            MenuItemConfig showCtxActions = MenuItemConfig.action("editor.popup.show.context.actions", "Show Context Actions", "LIGHTBULB");
            intentionsGroup.add(showCtxActions);
            intentionsGroup.add(MenuItemConfig.separator());
            editorPopup.add(MenuItemConfig.group("editor.popup.show.intentions", "ShowIntentionsGroup", false, intentionsGroup));

            // LightEditModePopup
            List<MenuItemConfig> lightEditGroup = new ArrayList<>();
            lightEditGroup.add(MenuItemConfig.action("editor.popup.open.file.in.project", "Open File in Project\u2026", "FOLDER_OPEN"));
            lightEditGroup.add(MenuItemConfig.separator());
            editorPopup.add(MenuItemConfig.group("editor.popup.light.edit.mode", "LightEditModePopup", false, lightEditGroup));

            // Cut, Copy, Copy as Rich Text, Paste
            editorPopup.add(MenuItemConfig.action("edit.cut", "Cut", "CUT"));
            editorPopup.add(MenuItemConfig.action("edit.copy", "Copy", "COPY"));
            editorPopup.add(MenuItemConfig.action("edit.copy.rich", "Copy as Rich Text"));
            editorPopup.add(MenuItemConfig.action("edit.paste", "Paste", "PASTE"));

            // Copy / Paste Special
            List<MenuItemConfig> copyPasteSpecial = new ArrayList<>();
            copyPasteSpecial.add(MenuItemConfig.action("edit.copy.reference", "Copy Reference"));
            copyPasteSpecial.add(MenuItemConfig.action("edit.copy.git.hosting.link", "Git.Hosting.Copy.Link.Group"));
            copyPasteSpecial.add(MenuItemConfig.action("edit.copy.plain", "Copy as Plain Text"));
            copyPasteSpecial.add(MenuItemConfig.action("edit.paste.plain", "Paste as Plain Text"));
            copyPasteSpecial.add(MenuItemConfig.action("edit.paste.history", "Paste from History\u2026"));
            editorPopup.add(MenuItemConfig.group("editor.popup.copy.paste.special", "Copy / Paste Special", true, copyPasteSpecial));

            // Copy JSON Pointer, Column Selection Mode
            editorPopup.add(MenuItemConfig.action("edit.copy.json.pointer", "Copy JSON Pointer"));
            editorPopup.add(MenuItemConfig.action("edit.column.selection.mode", "Column Selection Mode"));

            // Markdown.EditorContextMenuGroup
            List<MenuItemConfig> mdEditorGroup = new ArrayList<>();
            mdEditorGroup.add(MenuItemConfig.separator());

            List<MenuItemConfig> tableChildren = new ArrayList<>();
            tableChildren.add(MenuItemConfig.action("markdown.table.insert.col.left", "Insert Column Left", "PLUS"));
            tableChildren.add(MenuItemConfig.action("markdown.table.insert.col.right", "Insert Column Right", "PLUS"));
            tableChildren.add(MenuItemConfig.action("markdown.table.insert.row.above", "Insert Row Above", "ARROW_UP"));
            tableChildren.add(MenuItemConfig.action("markdown.table.insert.row.below", "Insert Row Below", "ARROW_DOWN"));
            tableChildren.add(MenuItemConfig.separator());

            List<MenuItemConfig> colAlignChildren = new ArrayList<>();
            List<MenuItemConfig> setColAlignChildren = new ArrayList<>();
            setColAlignChildren.add(MenuItemConfig.action("markdown.table.align.left", "Align Left", "ALIGN_LEFT"));
            setColAlignChildren.add(MenuItemConfig.action("markdown.table.align.center", "Align Center", "ALIGN_CENTER"));
            setColAlignChildren.add(MenuItemConfig.action("markdown.table.align.right", "Align Right", "ALIGN_RIGHT"));
            colAlignChildren.add(MenuItemConfig.group("markdown.table.set.col.alignment.group", "Set Column Alignment", true, setColAlignChildren));
            colAlignChildren.add(MenuItemConfig.action("markdown.table.move.col.left", "Move Column Left", "ARROW_LEFT"));
            colAlignChildren.add(MenuItemConfig.action("markdown.table.move.col.right", "Move Column Right", "ARROW_RIGHT"));
            colAlignChildren.add(MenuItemConfig.separator());
            colAlignChildren.add(MenuItemConfig.action("markdown.table.remove.col", "Remove Column", "TRASH"));
            colAlignChildren.add(MenuItemConfig.action("markdown.table.remove.row", "Remove Row", "TRASH"));

            tableChildren.add(MenuItemConfig.group("markdown.table.col.alignment.group", "Column Alignment", true, colAlignChildren));
            tableChildren.add(MenuItemConfig.action("markdown.table.insert", "Insert\u2026"));

            mdEditorGroup.add(MenuItemConfig.group("markdown.table.group", "Table", true, tableChildren));
            mdEditorGroup.add(MenuItemConfig.separator());
            editorPopup.add(MenuItemConfig.group("editor.popup.markdown.group", "Markdown.EditorContextMenuGroup", false, mdEditorGroup));

            editorPopup.add(MenuItemConfig.separator());

            // Editor Popup Menu Actions (1)
            List<MenuItemConfig> actions1Group = new ArrayList<>();
            actions1Group.add(MenuItemConfig.action("edit.find.in.files", "Find in Files"));

            List<MenuItemConfig> findRefactorGroup = new ArrayList<>();
            findRefactorGroup.add(MenuItemConfig.action("edit.find.usages", "Find Usages"));

            // Go To
            List<MenuItemConfig> goToChildren = new ArrayList<>();
            List<MenuItemConfig> tblNavigateGroup = new ArrayList<>();
            tblNavigateGroup.add(MenuItemConfig.action("console.table.navigate.first", "First Page", "FAST_BACKWARD"));
            tblNavigateGroup.add(MenuItemConfig.action("console.table.navigate.prev", "Previous Page", "STEP_BACKWARD"));
            tblNavigateGroup.add(MenuItemConfig.action("console.table.navigate.next", "Next Page", "STEP_FORWARD"));
            tblNavigateGroup.add(MenuItemConfig.action("console.table.navigate.last", "Last Page", "FAST_FORWARD"));
            tblNavigateGroup.add(MenuItemConfig.separator());
            goToChildren.add(MenuItemConfig.group("console.table.result.navigate.group", "Console.TableResult.Navigate.Group", false, tblNavigateGroup));

            goToChildren.add(MenuItemConfig.action("nav.jump.to.navbar", "Jump to Navigation Bar"));
            goToChildren.add(MenuItemConfig.action("nav.goto.declaration", "Go to Declaration or Usages"));
            goToChildren.add(MenuItemConfig.action("editor.edit.data", "Edit Data", "TABLE"));
            goToChildren.add(MenuItemConfig.action("editor.select.in.db.explorer", "Select in Database Explorer"));
            goToChildren.add(MenuItemConfig.action("nav.goto.implementation", "Go to Implementation(s)"));
            goToChildren.add(MenuItemConfig.action("nav.goto.type.declaration", "Go to Type Declaration"));
            goToChildren.add(MenuItemConfig.action("nav.goto.super.method", "Go to Super Method"));
            goToChildren.add(MenuItemConfig.action("nav.related.symbol", "Related Symbol\u2026"));
            goToChildren.add(MenuItemConfig.action("nav.goto.test", "Go to Test"));
            goToChildren.add(MenuItemConfig.action("editor.jump.ddl", "DDL"));
            goToChildren.add(MenuItemConfig.action("editor.goto.row", "Row\u2026"));

            List<MenuItemConfig> dbGoToGroup = new ArrayList<>();
            dbGoToGroup.add(MenuItemConfig.action("console.table.goto.related.rows", "Related Rows", "TH"));
            dbGoToGroup.add(MenuItemConfig.action("console.table.goto.open.url", "Open URL", "GLOBE"));
            dbGoToGroup.add(MenuItemConfig.action("console.table.goto.open.file.uri", "Open File URI", "FOLDER_OPEN"));
            dbGoToGroup.add(MenuItemConfig.action("console.table.goto.referencing.result", "Referencing Result"));
            dbGoToGroup.add(MenuItemConfig.separator());
            goToChildren.add(MenuItemConfig.group("console.table.result.database.goto.group", "Console.TableResult.Database.GoTo", false, dbGoToGroup));

            findRefactorGroup.add(MenuItemConfig.group("editor.popup.goto", "Go To", true, goToChildren));
            findRefactorGroup.add(MenuItemConfig.separator());

            // Folding
            List<MenuItemConfig> popupFoldingChildren = new ArrayList<>();
            popupFoldingChildren.add(MenuItemConfig.action("folding.expand", "Expand"));
            popupFoldingChildren.add(MenuItemConfig.action("folding.expand.recursively", "Expand Recursively"));
            popupFoldingChildren.add(MenuItemConfig.action("folding.expand.all", "Expand All"));
            popupFoldingChildren.add(MenuItemConfig.separator());
            popupFoldingChildren.add(MenuItemConfig.action("folding.collapse", "Collapse"));
            popupFoldingChildren.add(MenuItemConfig.action("folding.collapse.recursively", "Collapse Recursively"));
            popupFoldingChildren.add(MenuItemConfig.action("folding.collapse.all", "Collapse All"));
            popupFoldingChildren.add(MenuItemConfig.separator());

            List<MenuItemConfig> expandToLevel = new ArrayList<>();
            for (int lvl = 1; lvl <= 5; lvl++) {
                expandToLevel.add(MenuItemConfig.action("folding.expand.to.level." + lvl, String.valueOf(lvl)));
            }
            popupFoldingChildren.add(MenuItemConfig.group("folding.expand.to.level.group", "Expand to Level", true, expandToLevel));

            List<MenuItemConfig> expandAllToLevel = new ArrayList<>();
            for (int lvl = 1; lvl <= 5; lvl++) {
                expandAllToLevel.add(MenuItemConfig.action("folding.expand.all.to.level." + lvl, String.valueOf(lvl)));
            }
            popupFoldingChildren.add(MenuItemConfig.group("folding.expand.all.to.level.group", "Expand All to Level", true, expandAllToLevel));
            popupFoldingChildren.add(MenuItemConfig.separator());

            List<MenuItemConfig> langFolding = new ArrayList<>();
            langFolding.add(MenuItemConfig.action("folding.expand.doc.comments", "Expand Doc Comments"));
            langFolding.add(MenuItemConfig.action("folding.collapse.doc.comments", "Collapse Doc Comments"));
            popupFoldingChildren.add(MenuItemConfig.group("folding.language.specific.group", "LanguageSpecificFoldingGroup", false, langFolding));
            popupFoldingChildren.add(MenuItemConfig.separator());

            popupFoldingChildren.add(MenuItemConfig.action("folding.toggle", "Toggle Folding"));
            popupFoldingChildren.add(MenuItemConfig.separator());
            popupFoldingChildren.add(MenuItemConfig.action("folding.selection.remove.region", "Fold Selection / Remove Region"));
            popupFoldingChildren.add(MenuItemConfig.action("folding.code.block", "Fold Code Block"));

            findRefactorGroup.add(MenuItemConfig.group("editor.popup.folding", "Folding", true, popupFoldingChildren));
            findRefactorGroup.add(MenuItemConfig.action("tools.save.as.live.template", "Save as Live Template\u2026"));
            findRefactorGroup.add(MenuItemConfig.action("code.reformat", "Reformat Code", "INDENT"));

            actions1Group.add(MenuItemConfig.group("editor.popup.find.refactor", "EditorPopupMenu1.FindRefactor", false, findRefactorGroup));

            editorPopup.add(MenuItemConfig.group("editor.popup.actions.1", "Editor Popup Menu Actions (1)", false, actions1Group));

            editorPopup.add(MenuItemConfig.separator());

            // EditorPopupMenu2 (Image 1)
            List<MenuItemConfig> popupMenu2Children = new ArrayList<>();
            List<MenuItemConfig> sqlScriptsChildren = new ArrayList<>();
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.generator", "SQL Generator\u2026"));
            sqlScriptsChildren.add(MenuItemConfig.separator());
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.request.copy.ddl", "Request and Copy Original DDL"));
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.generate.ddl.clipboard", "Generate DDL to Clipboard"));
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.generate.ddl.queryfile", "Generate DDL to Query File"));
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.context.templates", "Context Templates"));
            sqlScriptsChildren.add(MenuItemConfig.separator());
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.run.script", "Run SQL Script\u2026"));
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.regenerate.definition", "Regenerate definition"));
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.import.to.database", "Import to Database\u2026", "UPLOAD"));
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.view.as.table", "View as Table", "TABLE"));
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.edit.as.table", "Edit as Table", "TABLE"));
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.change.file.language", "Change File Language"));
            sqlScriptsChildren.add(MenuItemConfig.action("editor.sql.change.sql.dialect", "Change SQL Dialect"));
            popupMenu2Children.add(MenuItemConfig.group("editor.sql.scripts", "SQL Scripts", true, "FOLDER", sqlScriptsChildren));
            editorPopup.add(MenuItemConfig.group("editor.popup.menu.2", "EditorPopupMenu2", true, popupMenu2Children));

            editorPopup.add(MenuItemConfig.separator());

            // EditorPopupMenu3 (Image 1)
            List<MenuItemConfig> popupMenu3Children = new ArrayList<>();
            popupMenu3Children.add(MenuItemConfig.action("editor.popup.set.background.image", "Set Background Image"));
            editorPopup.add(MenuItemConfig.group("editor.popup.menu.3", "EditorPopupMenu3", true, popupMenu3Children));

            editorPopup.add(MenuItemConfig.separator());

            // Search with Google
            editorPopup.add(MenuItemConfig.action("editor.search.with.google", "Search with Google"));

            editorPopup.add(MenuItemConfig.separator());

            // Editor Popup Menu Actions (2) (Images 2, 3, 4, 5)
            List<MenuItemConfig> actions2Group = new ArrayList<>();
            actions2Group.add(MenuItemConfig.separator());
            actions2Group.add(MenuItemConfig.action("editor.popup.rename", "Rename\u2026"));

            // Refactor [popup] (Image 2)
            List<MenuItemConfig> popupRefactorChildren = new ArrayList<>();
            popupRefactorChildren.add(MenuItemConfig.action("refactor.this", "Refactor This\u2026"));
            popupRefactorChildren.add(MenuItemConfig.action("refactor.rename", "Rename\u2026"));
            popupRefactorChildren.add(MenuItemConfig.action("refactor.change.signature", "Change Signature\u2026"));
            popupRefactorChildren.add(MenuItemConfig.action("refactor.modify.object", "Modify Object\u2026"));
            popupRefactorChildren.add(MenuItemConfig.separator());

            // Extract/Introduce [popup] (Image 2)
            List<MenuItemConfig> extractIntroChildren = new ArrayList<>();
            extractIntroChildren.add(MenuItemConfig.action("refactor.introduce.variable", "Introduce Variable\u2026"));
            extractIntroChildren.add(MenuItemConfig.action("refactor.extract.routine", "Extract Routine\u2026"));
            extractIntroChildren.add(MenuItemConfig.action("refactor.table.alias", "Table alias\u2026"));
            extractIntroChildren.add(MenuItemConfig.action("refactor.introduce.constant", "Introduce Constant\u2026"));
            extractIntroChildren.add(MenuItemConfig.action("refactor.introduce.field", "Introduce Field\u2026"));
            extractIntroChildren.add(MenuItemConfig.action("refactor.introduce.parameter", "Introduce Parameter\u2026"));
            extractIntroChildren.add(MenuItemConfig.separator());
            extractIntroChildren.add(MenuItemConfig.action("refactor.introduce.parameter.object", "Introduce Parameter Object\u2026"));
            extractIntroChildren.add(MenuItemConfig.separator());
            extractIntroChildren.add(MenuItemConfig.action("refactor.extract.method", "Extract Method\u2026"));
            extractIntroChildren.add(MenuItemConfig.separator());
            extractIntroChildren.add(MenuItemConfig.action("refactor.extract.delegate", "Extract Delegate\u2026"));
            extractIntroChildren.add(MenuItemConfig.action("refactor.include.file", "Include File\u2026"));
            extractIntroChildren.add(MenuItemConfig.action("refactor.extract.interface", "Extract Interface\u2026"));
            extractIntroChildren.add(MenuItemConfig.action("refactor.extract.superclass", "Extract Superclass\u2026"));
            extractIntroChildren.add(MenuItemConfig.action("refactor.extract.module", "Extract Module\u2026"));
            extractIntroChildren.add(MenuItemConfig.action("refactor.subquery.cte", "Subquery as CTE"));
            popupRefactorChildren.add(MenuItemConfig.group("editor.refactor.extract.introduce", "Extract/Introduce", true, extractIntroChildren));

            popupRefactorChildren.add(MenuItemConfig.action("refactor.inline", "Inline\u2026"));
            popupRefactorChildren.add(MenuItemConfig.separator());
            popupRefactorChildren.add(MenuItemConfig.action("refactor.move", "Move\u2026"));
            popupRefactorChildren.add(MenuItemConfig.action("refactor.copy", "Copy\u2026"));
            popupRefactorChildren.add(MenuItemConfig.action("refactor.safe.delete", "Safe Delete\u2026"));
            popupRefactorChildren.add(MenuItemConfig.separator());
            popupRefactorChildren.add(MenuItemConfig.action("refactor.pull.members.up", "Pull Members Up\u2026"));
            popupRefactorChildren.add(MenuItemConfig.action("refactor.push.members.down", "Push Members Down\u2026"));
            popupRefactorChildren.add(MenuItemConfig.action("refactor.invert.boolean", "Invert Boolean\u2026"));
            actions2Group.add(MenuItemConfig.group("editor.popup.refactor", "Refactor", true, popupRefactorChildren));

            actions2Group.add(MenuItemConfig.action("editor.popup.generate", "Generate\u2026"));
            actions2Group.add(MenuItemConfig.separator());

            // Debug Actions (Image 3)
            List<MenuItemConfig> debugActionsChildren = new ArrayList<>();
            debugActionsChildren.add(MenuItemConfig.action("debug.copy.js.script.clipboard", "Copy JS Script to Clipboard"));
            debugActionsChildren.add(MenuItemConfig.action("debug.show.js.script", "Show JS Script"));
            debugActionsChildren.add(MenuItemConfig.separator());
            debugActionsChildren.add(MenuItemConfig.action("debug.evaluate.expression", "Evaluate Expression\u2026", "CALCULATOR"));
            debugActionsChildren.add(MenuItemConfig.action("debug.run.to.cursor", "Run to Cursor"));
            debugActionsChildren.add(MenuItemConfig.action("debug.force.run.to.cursor", "Force Run to Cursor"));
            debugActionsChildren.add(MenuItemConfig.action("debug.add.to.watches", "Add to Watches"));
            debugActionsChildren.add(MenuItemConfig.action("debug.add.inline.watch", "Add Inline Watch"));
            debugActionsChildren.add(MenuItemConfig.action("debug.evaluate.in.console", "Evaluate in Console"));
            debugActionsChildren.add(MenuItemConfig.separator());

            List<MenuItemConfig> hotSwapChildren = new ArrayList<>();
            hotSwapChildren.add(MenuItemConfig.action("debug.compile.reload.modified.files", "Compile and Reload Modified Files"));
            hotSwapChildren.add(MenuItemConfig.separator());
            debugActionsChildren.add(MenuItemConfig.group("editor.popup.debug.hotswap", "EditorPopupMenuDebugHotSwap", false, hotSwapChildren));

            actions2Group.add(MenuItemConfig.group("editor.debug.actions", "Debug Actions", false, debugActionsChildren));

            // Compile/Run Actions (Image 3 & 4)
            List<MenuItemConfig> compileRunChildren = new ArrayList<>();
            List<MenuItemConfig> runConfigsChildren = new ArrayList<>();
            List<MenuItemConfig> runContextChildren = new ArrayList<>();

            // RunContextGroupInner (Image 3)
            List<MenuItemConfig> runContextInnerChildren = new ArrayList<>();
            List<MenuItemConfig> executorsChildren = new ArrayList<>();
            executorsChildren.add(MenuItemConfig.action("run.context.configuration", "Run context configuration", "PLAY"));
            executorsChildren.add(MenuItemConfig.action("debug.context.configuration", "Debug context configuration", "BUG"));
            runContextInnerChildren.add(MenuItemConfig.group("editor.run.context.executors.group", "RunContextExecutorsGroup", false, executorsChildren));

            List<MenuItemConfig> moreRunDebugChildren = new ArrayList<>();
            moreRunDebugChildren.add(MenuItemConfig.action("run.coverage.context.configuration", "Run with Coverage context configuration"));
            moreRunDebugChildren.add(MenuItemConfig.action("run.profiler.context.configuration", "Run with Profiler"));
            moreRunDebugChildren.add(MenuItemConfig.action("run.create.configuration", "Create Run Configuration"));
            moreRunDebugChildren.add(MenuItemConfig.separator());
            moreRunDebugChildren.add(MenuItemConfig.action("run.context.configuration.secondary", "Run context configuration", "PLAY"));
            moreRunDebugChildren.add(MenuItemConfig.action("debug.context.configuration.secondary", "Debug context configuration", "BUG"));
            moreRunDebugChildren.add(MenuItemConfig.action("run.modify.configuration", "Modify Run Configuration\u2026"));
            moreRunDebugChildren.add(MenuItemConfig.separator());
            runContextInnerChildren.add(MenuItemConfig.group("editor.more.run.debug.group", "More Run/Debug", false, moreRunDebugChildren));

            runContextChildren.add(MenuItemConfig.group("editor.run.context.group.inner", "RunContextGroupInner", false, runContextInnerChildren));
            runContextChildren.add(MenuItemConfig.separator());

            // Console.Jdbc.RunContextGroup (Image 4)
            List<MenuItemConfig> consoleJdbcRunChildren = new ArrayList<>();
            consoleJdbcRunChildren.add(MenuItemConfig.action("console.attach.data.source", "Attach Data Source"));
            consoleJdbcRunChildren.add(MenuItemConfig.separator());
            consoleJdbcRunChildren.add(MenuItemConfig.action("console.recompile", "Recompile\u2026"));

            List<MenuItemConfig> explainPlanChildren = new ArrayList<>();
            explainPlanChildren.add(MenuItemConfig.action("console.explain.plan", "Explain Plan"));
            explainPlanChildren.add(MenuItemConfig.action("console.explain.plan.raw", "Explain Plan (Raw)"));
            explainPlanChildren.add(MenuItemConfig.action("console.explain.analyse", "Explain Analyse"));
            explainPlanChildren.add(MenuItemConfig.action("console.explain.analyse.raw", "Explain Analyse (Raw)"));
            consoleJdbcRunChildren.add(MenuItemConfig.group("console.explain.plan.group", "Explain Plan", true, explainPlanChildren));

            consoleJdbcRunChildren.add(MenuItemConfig.action("middle.run", "Execute", "PLAY"));
            consoleJdbcRunChildren.add(MenuItemConfig.action("editor.execute.selection.single", "Execute Selection as Single Statement", "PLAY"));
            consoleJdbcRunChildren.add(MenuItemConfig.action("editor.export.data", "Export Data\u2026", "DOWNLOAD"));
            consoleJdbcRunChildren.add(MenuItemConfig.action("editor.debug", "Debug", "BUG"));
            consoleJdbcRunChildren.add(MenuItemConfig.action("editor.debug.routine", "Debug Routine\u2026", "BUG"));
            consoleJdbcRunChildren.add(MenuItemConfig.separator());
            consoleJdbcRunChildren.add(MenuItemConfig.action("editor.migrate.consoles.to.queryfiles", "Migrate Query Consoles to Query Files\u2026"));
            consoleJdbcRunChildren.add(MenuItemConfig.separator());

            runContextChildren.add(MenuItemConfig.group("console.jdbc.run.context.group", "Console.Jdbc.RunContextGroup", false, consoleJdbcRunChildren));

            runConfigsChildren.add(MenuItemConfig.group("editor.run.context.group", "RunContextGroup", false, runContextChildren));
            compileRunChildren.add(MenuItemConfig.group("editor.run.configurations.group", "Run Configurations", false, runConfigsChildren));
            actions2Group.add(MenuItemConfig.group("editor.compile.run.actions", "Compile/Run Actions", false, compileRunChildren));

            // SplitRevealGroup (Image 4)
            List<MenuItemConfig> splitRevealChildren = new ArrayList<>();
            splitRevealChildren.add(MenuItemConfig.action("editor.open.in.right.split", "Open in Right Split", "COLUMNS"));
            splitRevealChildren.add(MenuItemConfig.action("editor.open.in.split.chooser", "Open in Split with Chooser\u2026"));

            List<MenuItemConfig> openInChildren = new ArrayList<>();
            openInChildren.add(MenuItemConfig.action("editor.open.in.file.manager", "Show in File Manager"));
            openInChildren.add(MenuItemConfig.action("editor.open.in.associated.app", "Open in Associated Application"));
            openInChildren.add(MenuItemConfig.action("editor.open.in.browser", "Open in Browser", "GLOBE"));
            openInChildren.add(MenuItemConfig.action("editor.open.in.file.path", "File Path"));
            openInChildren.add(MenuItemConfig.action("editor.open.in.terminal", "Open in Terminal", "TERMINAL"));
            openInChildren.add(MenuItemConfig.action("editor.open.in.terminal.second", "Open in Terminal", "TERMINAL"));
            openInChildren.add(MenuItemConfig.action("git.hosting.open.in.browser.group", "Git.Hosting.Open.In.Browser.Group"));
            splitRevealChildren.add(MenuItemConfig.group("editor.open.in.group", "Open In", true, "FOLDER", openInChildren));
            splitRevealChildren.add(MenuItemConfig.separator());

            actions2Group.add(MenuItemConfig.group("editor.split.reveal.group", "SplitRevealGroup", false, splitRevealChildren));
            actions2Group.add(MenuItemConfig.separator());

            // VCS/LVCS Actions (Image 5)
            List<MenuItemConfig> vcsLvcsChildren = new ArrayList<>();
            List<MenuItemConfig> localHistoryChildren = new ArrayList<>();
            localHistoryChildren.add(MenuItemConfig.action("editor.local.history.show", "Show History\u2026"));
            localHistoryChildren.add(MenuItemConfig.action("editor.local.history.show.selection", "Show History for Selection\u2026"));
            localHistoryChildren.add(MenuItemConfig.separator());
            localHistoryChildren.add(MenuItemConfig.action("editor.local.history.show.project", "Show Project History\u2026"));
            localHistoryChildren.add(MenuItemConfig.action("editor.local.history.recent.changes", "Recent Changes"));
            localHistoryChildren.add(MenuItemConfig.action("editor.local.history.put.label", "Put Label\u2026"));
            localHistoryChildren.add(MenuItemConfig.action("editor.local.history.vcs.group", "Version Control Group"));
            vcsLvcsChildren.add(MenuItemConfig.group("editor.local.history.group", "Local History", true, localHistoryChildren));
            vcsLvcsChildren.add(MenuItemConfig.separator());
            vcsLvcsChildren.add(MenuItemConfig.action("editor.external.tools", "External Tools"));
            vcsLvcsChildren.add(MenuItemConfig.action("editor.update.tag.emmet", "Update tag with Emmet"));
            vcsLvcsChildren.add(MenuItemConfig.action("diff.compare.clipboard", "Compare with Clipboard", "EXCHANGE_ALT"));
            vcsLvcsChildren.add(MenuItemConfig.action("vcs.show.review.diff", "Show Review Diff", "ARROW_RIGHT"));
            vcsLvcsChildren.add(MenuItemConfig.action("vcs.add.review.comment", "Add Review Comment"));

            actions2Group.add(MenuItemConfig.group("editor.vcs.lvcs.actions.group", "VCS/LVCS Actions", false, vcsLvcsChildren));

            editorPopup.add(MenuItemConfig.group("editor.popup.actions.2", "Editor Popup Menu Actions (2)", false, actions2Group));

            // Git [popup] (Image 5)
            List<MenuItemConfig> gitChildren = new ArrayList<>();
            gitChildren.add(MenuItemConfig.action("git.show.local.version", "Show Local Version"));
            gitChildren.add(MenuItemConfig.action("git.compare.with.head", "Compare with HEAD Version"));
            gitChildren.add(MenuItemConfig.action("git.compare.with.local", "Compare with Local Version"));
            gitChildren.add(MenuItemConfig.action("git.compare.head.staged.local", "Compare HEAD, Staged and Local Versions"));
            editorPopup.add(MenuItemConfig.group("editor.popup.git", "Git", true, gitChildren));

            // Diagrams [popup] (Image 5)
            List<MenuItemConfig> diagChildren = new ArrayList<>();
            diagChildren.add(MenuItemConfig.action("diagrams.show.uml", "ShowUmlDiagram", "PROJECT_DIAGRAM"));
            diagChildren.add(MenuItemConfig.action("diagrams.show.uml.popup", "ShowUmlDiagramPopup", "PROJECT_DIAGRAM"));
            editorPopup.add(MenuItemConfig.group("editor.popup.diagrams", "Diagrams", true, diagChildren));

            // Change Template Data Language
            editorPopup.add(MenuItemConfig.action("file.change.template.lang", "Change Template Data Language"));

            // XPathView.EditorPopup [popup] (Image 5)
            List<MenuItemConfig> xpathChildren = new ArrayList<>();
            xpathChildren.add(MenuItemConfig.separator());
            xpathChildren.add(MenuItemConfig.action("xpath.evaluate", "Evaluate XPath\u2026"));
            xpathChildren.add(MenuItemConfig.action("xpath.show.unique", "Show Unique XPath"));
            xpathChildren.add(MenuItemConfig.action("xpath.file.associations", "File Associations"));
            editorPopup.add(MenuItemConfig.group("xpath.editor.popup", "XPathView.EditorPopup", true, xpathChildren));

            roots.add(MenuItemConfig.group("root.editor.popup", "Editor Popup Menu", editorPopup));

            // 4. Editor Gutter Popup Menu (Image 1)
            List<MenuItemConfig> gutterPopup = new ArrayList<>();
            List<MenuItemConfig> gutterVcsChildren = new ArrayList<>();
            gutterVcsChildren.add(MenuItemConfig.action("gutter.vcs.annotate", "Annotate"));
            gutterVcsChildren.add(MenuItemConfig.separator());
            gutterPopup.add(MenuItemConfig.group("gutter.vcs.popup", "EditorGutterVcsPopupMenu", false, gutterVcsChildren));

            List<MenuItemConfig> gutterBookmarkChildren = new ArrayList<>();
            gutterBookmarkChildren.add(MenuItemConfig.action("gutter.bookmark.add.another.list", "Add Bookmark to Another List"));
            gutterBookmarkChildren.add(MenuItemConfig.action("gutter.bookmark.rename", "Rename Bookmark\u2026", "EDIT"));
            gutterBookmarkChildren.add(MenuItemConfig.action("gutter.bookmark.toggle", "Toggle Bookmark"));
            gutterBookmarkChildren.add(MenuItemConfig.action("gutter.bookmark.remove.mnemonic", "Remove Mnemonic"));
            gutterBookmarkChildren.add(MenuItemConfig.action("gutter.bookmark.toggle.mnemonic", "Toggle Bookmark Mnemonic\u2026"));
            gutterBookmarkChildren.add(MenuItemConfig.separator());
            gutterPopup.add(MenuItemConfig.group("gutter.bookmark.context", "popup@BookmarkContextMenu", false, gutterBookmarkChildren));

            gutterPopup.add(MenuItemConfig.action("editor.softwrap", "Soft-Wrap", "ALIGN_LEFT"));
            gutterPopup.add(MenuItemConfig.action("editor.configure.softwraps", "Configure Soft Wraps\u2026"));
            gutterPopup.add(MenuItemConfig.separator());

            List<MenuItemConfig> gutterAppearanceChildren = new ArrayList<>();
            gutterAppearanceChildren.add(MenuItemConfig.action("gutter.appearance.line.numbers", "Show Line Numbers"));
            gutterAppearanceChildren.add(MenuItemConfig.action("gutter.appearance.breakpoints.over.line.numbers", "Breakpoints Over Line Numbers"));
            gutterAppearanceChildren.add(MenuItemConfig.action("gutter.appearance.indent.guides", "Show Indent Guides"));
            gutterAppearanceChildren.add(MenuItemConfig.action("gutter.appearance.sticky.lines", "Show Sticky Lines"));

            List<MenuItemConfig> gutterBreadcrumbsChildren = new ArrayList<>();
            gutterBreadcrumbsChildren.add(MenuItemConfig.action("gutter.breadcrumbs.top", "Top"));
            gutterBreadcrumbsChildren.add(MenuItemConfig.action("gutter.breadcrumbs.bottom", "Bottom"));
            gutterBreadcrumbsChildren.add(MenuItemConfig.action("gutter.breadcrumbs.dont.show", "Don't Show"));
            gutterAppearanceChildren.add(MenuItemConfig.group("gutter.breadcrumbs", "Breadcrumbs", true, "FOLDER", gutterBreadcrumbsChildren));
            gutterAppearanceChildren.add(MenuItemConfig.action("gutter.configure.icons", "Configure Gutter Icons\u2026"));
            gutterPopup.add(MenuItemConfig.group("gutter.appearance", "Appearance", true, "FOLDER", gutterAppearanceChildren));

            gutterPopup.add(MenuItemConfig.action("gutter.remove.breakpoints", "Remove other breakpoints"));
            gutterPopup.add(MenuItemConfig.action("gutter.disable.breakpoints", "Disable other breakpoints"));
            roots.add(MenuItemConfig.group("root.editor.gutter.popup", "Editor Gutter Popup Menu", gutterPopup));

            // 5. Editor Tab Popup Menu (Images 2, 3, 4, 5)
            List<MenuItemConfig> tabPopup = new ArrayList<>();

            // Editor Close Actions (Image 2 & 3)
            List<MenuItemConfig> tabCloseActions = new ArrayList<>();
            tabCloseActions.add(MenuItemConfig.action("tab.close", "Close Tab"));
            tabCloseActions.add(MenuItemConfig.action("tab.close.others", "Close Other Tabs"));
            tabCloseActions.add(MenuItemConfig.action("tab.close.all", "Close All Tabs"));
            tabCloseActions.add(MenuItemConfig.action("tab.close.unmodified", "Close Unmodified Tabs"));
            tabCloseActions.add(MenuItemConfig.action("tab.close.all.but.pinned", "Close All but Pinned"));
            tabCloseActions.add(MenuItemConfig.action("tab.close.left", "Close Tabs to the Left"));
            tabCloseActions.add(MenuItemConfig.action("tab.close.right", "Close Tabs to the Right"));
            tabCloseActions.add(MenuItemConfig.action("tab.close.all.readonly", "Close All Read-Only Tabs"));
            tabCloseActions.add(MenuItemConfig.action("tab.open.as.editor.tab", "Open as Editor Tab", "EXTERNAL_LINK_ALT"));
            tabCloseActions.add(MenuItemConfig.separator());
            tabPopup.add(MenuItemConfig.group("tab.close.actions", "Editor Close Actions", false, tabCloseActions));

            tabPopup.add(MenuItemConfig.action("tab.copy.paths", "Copy Paths"));
            tabPopup.add(MenuItemConfig.action("tab.copy.reference", "Copy Reference"));
            tabPopup.add(MenuItemConfig.action("tab.copy.json.pointer", "Copy JSON Pointer"));
            tabPopup.add(MenuItemConfig.action("tab.change.template.lang", "Change Template Data Language"));

            // Copy Path/Reference... (Image 2 & 3)
            List<MenuItemConfig> tabCopyPathRefChildren = new ArrayList<>();
            List<MenuItemConfig> tabCopyFileRefChildren = new ArrayList<>();
            tabCopyFileRefChildren.add(MenuItemConfig.action("tab.copy.path.absolute", "Absolute Path"));
            tabCopyFileRefChildren.add(MenuItemConfig.action("tab.copy.path.filename", "File Name"));
            tabCopyFileRefChildren.add(MenuItemConfig.separator());
            tabCopyFileRefChildren.add(MenuItemConfig.action("tab.copy.path.line.number", "Path with Line Number"));
            tabCopyFileRefChildren.add(MenuItemConfig.action("tab.copy.path.content.root", "Path from Content Root"));
            tabCopyFileRefChildren.add(MenuItemConfig.action("tab.copy.path.source.root", "Path from Source Root"));
            tabCopyFileRefChildren.add(MenuItemConfig.action("tab.copy.path.repo.root", "Path From Repository Root"));
            tabCopyFileRefChildren.add(MenuItemConfig.action("tab.copy.git.hosting.link", "Git.Hosting.Copy.Link.Group"));
            tabCopyPathRefChildren.add(MenuItemConfig.group("tab.copy.file.reference.group", "CopyFileReference", false, tabCopyFileRefChildren));
            tabCopyPathRefChildren.add(MenuItemConfig.separator());

            List<MenuItemConfig> tabCopyExtRefChildren = new ArrayList<>();
            tabCopyExtRefChildren.add(MenuItemConfig.action("tab.copy.toolbox.url", "Toolbox URL", "TOOLBOX"));
            tabCopyPathRefChildren.add(MenuItemConfig.group("tab.copy.external.reference.group", "CopyExternalReferenceGroup", false, tabCopyExtRefChildren));
            tabCopyPathRefChildren.add(MenuItemConfig.action("tab.copy.ref.action", "Copy Reference"));
            tabPopup.add(MenuItemConfig.group("tab.copy.path.reference.group", "Copy Path/Reference\u2026", true, tabCopyPathRefChildren));

            tabPopup.add(MenuItemConfig.separator());
            tabPopup.add(MenuItemConfig.action("tab.diff.separate.window", "Show Diff in Separate Window", "EXTERNAL_LINK_ALT"));
            tabPopup.add(MenuItemConfig.action("tab.diff.all.one.view", "Show All Files in One Diff View"));

            // Vcs.Diff.EditorTabs.Group (Image 4)
            List<MenuItemConfig> tabVcsDiffChildren = new ArrayList<>();
            tabVcsDiffChildren.add(MenuItemConfig.action("tab.vcs.diff.collapse.all", "Collapse All Files"));
            tabPopup.add(MenuItemConfig.group("vcs.diff.editor.tabs.group", "Vcs.Diff.EditorTabs.Group", false, tabVcsDiffChildren));

            tabPopup.add(MenuItemConfig.action("editor.split.right", "Split Right", "COLUMNS"));
            tabPopup.add(MenuItemConfig.action("tab.split.move.right", "Split and Move Right"));
            tabPopup.add(MenuItemConfig.action("editor.split.down", "Split Down", "WINDOW_RESTORE"));
            tabPopup.add(MenuItemConfig.action("tab.split.move.down", "Split and Move Down"));
            tabPopup.add(MenuItemConfig.action("tab.move.opposite.group", "Move to Opposite Group"));
            tabPopup.add(MenuItemConfig.action("tab.open.opposite.group", "Open in Opposite Group"));
            tabPopup.add(MenuItemConfig.action("tab.change.splitter.orientation", "Change Splitter Orientation"));
            tabPopup.add(MenuItemConfig.action("tab.unsplit", "Unsplit"));
            tabPopup.add(MenuItemConfig.action("tab.unsplit.all", "Unsplit All"));
            tabPopup.add(MenuItemConfig.separator());
            tabPopup.add(MenuItemConfig.action("tab.pin.active", "Pin Active Tab"));
            tabPopup.add(MenuItemConfig.action("tab.keep.open", "Keep Tab Open"));
            tabPopup.add(MenuItemConfig.action("tab.open.in.new.window", "Open Tab in New Window"));
            tabPopup.add(MenuItemConfig.action("tab.configure.editor.tabs", "Configure Editor Tabs\u2026"));
            tabPopup.add(MenuItemConfig.separator());
            tabPopup.add(MenuItemConfig.action("tab.reopen.closed", "Reopen Closed Tab"));

            // Database.EditorTabPopupMenu (Image 4)
            List<MenuItemConfig> tabDatabaseChildren = new ArrayList<>();
            tabDatabaseChildren.add(MenuItemConfig.separator());
            tabDatabaseChildren.add(MenuItemConfig.action("tab.database.shorten.titles", "Shorten Tab Titles"));
            tabPopup.add(MenuItemConfig.group("database.editor.tab.popup", "Database.EditorTabPopupMenu", false, tabDatabaseChildren));

            // Bookmarks (Image 4)
            List<MenuItemConfig> tabBookmarksChildren = new ArrayList<>();
            tabBookmarksChildren.add(MenuItemConfig.action("tab.bookmark.add.another.list", "Add Bookmark to Another List"));
            tabBookmarksChildren.add(MenuItemConfig.action("tab.bookmark.rename", "Rename Bookmark\u2026", "EDIT"));
            tabBookmarksChildren.add(MenuItemConfig.action("tab.bookmark.toggle", "Toggle Bookmark"));
            tabPopup.add(MenuItemConfig.group("tab.bookmarks", "Bookmarks", false, tabBookmarksChildren));

            // Editor Tab Popup Menu Actions (1) (Image 4 & 5)
            List<MenuItemConfig> tabActions1Children = new ArrayList<>();
            tabActions1Children.add(MenuItemConfig.separator());
            tabActions1Children.add(MenuItemConfig.action("tab.actions1.change.file.language", "Change File Language"));
            tabActions1Children.add(MenuItemConfig.action("tab.actions1.associate.file.type", "Associate with File Type\u2026"));

            // Mark File As (Image 5)
            List<MenuItemConfig> tabMarkFileAsChildren = new ArrayList<>();
            tabMarkFileAsChildren.add(MenuItemConfig.action("tab.mark.file.override.type", "Override File Type"));
            tabMarkFileAsChildren.add(MenuItemConfig.action("tab.mark.file.revert.override", "Revert File Type Override"));
            tabActions1Children.add(MenuItemConfig.group("tab.mark.file.as.group", "Mark File As", true, "FOLDER", tabMarkFileAsChildren));
            tabActions1Children.add(MenuItemConfig.separator());

            // Run Configurations (Image 5)
            List<MenuItemConfig> tabRunConfigsChildren = new ArrayList<>();
            List<MenuItemConfig> tabRunContextChildren = new ArrayList<>();
            List<MenuItemConfig> tabRunContextInnerChildren = new ArrayList<>();

            List<MenuItemConfig> tabExecutorsChildren = new ArrayList<>();
            tabExecutorsChildren.add(MenuItemConfig.action("run.context.configuration", "Run context configuration", "PLAY"));
            tabExecutorsChildren.add(MenuItemConfig.action("debug.context.configuration", "Debug context configuration", "BUG"));
            tabRunContextInnerChildren.add(MenuItemConfig.group("tab.run.context.executors.group", "RunContextExecutorsGroup", false, tabExecutorsChildren));

            List<MenuItemConfig> tabMoreRunDebugChildren = new ArrayList<>();
            tabMoreRunDebugChildren.add(MenuItemConfig.action("run.coverage.context.configuration", "Run with Coverage context configuration", "SHIELD_ALT"));
            tabMoreRunDebugChildren.add(MenuItemConfig.action("run.profiler.context.configuration", "Run with Profiler"));
            tabMoreRunDebugChildren.add(MenuItemConfig.action("run.create.configuration", "Create Run Configuration"));
            tabMoreRunDebugChildren.add(MenuItemConfig.separator());
            tabMoreRunDebugChildren.add(MenuItemConfig.action("run.context.configuration.secondary", "Run context configuration", "PLAY"));
            tabMoreRunDebugChildren.add(MenuItemConfig.action("debug.context.configuration.secondary", "Debug context configuration", "BUG"));
            tabMoreRunDebugChildren.add(MenuItemConfig.action("run.modify.configuration", "Modify Run Configuration\u2026"));
            tabRunContextInnerChildren.add(MenuItemConfig.group("tab.more.run.debug.group", "More Run/Debug", false, tabMoreRunDebugChildren));

            tabRunContextChildren.add(MenuItemConfig.group("tab.run.context.group.inner", "RunContextGroupInner", false, tabRunContextInnerChildren));
            tabRunContextChildren.add(MenuItemConfig.separator());

            // Console.Jdbc.RunContextGroup (Image 5)
            List<MenuItemConfig> tabConsoleJdbcRunChildren = new ArrayList<>();
            tabConsoleJdbcRunChildren.add(MenuItemConfig.action("console.attach.data.source", "Attach Data Source"));
            tabConsoleJdbcRunChildren.add(MenuItemConfig.separator());
            tabConsoleJdbcRunChildren.add(MenuItemConfig.action("console.recompile", "Recompile\u2026", "HAMMER"));

            List<MenuItemConfig> tabExplainPlanChildren = new ArrayList<>();
            tabExplainPlanChildren.add(MenuItemConfig.action("console.explain.plan", "Explain Plan", "PROJECT_DIAGRAM"));
            tabExplainPlanChildren.add(MenuItemConfig.action("console.explain.plan.raw", "Explain Plan (Raw)", "PROJECT_DIAGRAM"));
            tabExplainPlanChildren.add(MenuItemConfig.action("console.explain.analyse", "Explain Analyse", "PROJECT_DIAGRAM"));
            tabExplainPlanChildren.add(MenuItemConfig.action("console.explain.analyse.raw", "Explain Analyse (Raw)", "PROJECT_DIAGRAM"));
            tabConsoleJdbcRunChildren.add(MenuItemConfig.group("console.explain.plan.group", "Explain Plan", true, tabExplainPlanChildren));

            tabConsoleJdbcRunChildren.add(MenuItemConfig.action("middle.run", "Execute", "PLAY"));
            tabConsoleJdbcRunChildren.add(MenuItemConfig.action("editor.execute.selection.single", "Execute Selection as Single Statement", "PLAY"));
            tabConsoleJdbcRunChildren.add(MenuItemConfig.action("editor.export.data", "Export Data\u2026", "DOWNLOAD"));
            tabConsoleJdbcRunChildren.add(MenuItemConfig.action("editor.debug", "Debug", "BUG"));
            tabConsoleJdbcRunChildren.add(MenuItemConfig.action("editor.debug.routine", "Debug Routine\u2026", "BUG"));
            tabConsoleJdbcRunChildren.add(MenuItemConfig.separator());
            tabConsoleJdbcRunChildren.add(MenuItemConfig.action("editor.migrate.consoles.to.queryfiles", "Migrate Query Consoles to Query Files\u2026", "ARROW_RIGHT"));

            tabRunContextChildren.add(MenuItemConfig.group("console.jdbc.run.context.group", "Console.Jdbc.RunContextGroup", false, tabConsoleJdbcRunChildren));

            tabRunConfigsChildren.add(MenuItemConfig.group("tab.run.context.group", "RunContextGroup", false, tabRunContextChildren));
            tabActions1Children.add(MenuItemConfig.group("tab.run.configurations.group", "Run Configurations", false, tabRunConfigsChildren));

            // SplitRevealGroup (Image: media_1790694503981.png)
            List<MenuItemConfig> tabSplitRevealChildren = new ArrayList<>();
            tabSplitRevealChildren.add(MenuItemConfig.action("editor.open.in.right.split", "Open in Right Split", "COLUMNS"));
            tabSplitRevealChildren.add(MenuItemConfig.action("editor.open.in.split.chooser", "Open in Split with Chooser\u2026"));

            List<MenuItemConfig> tabOpenInChildren = new ArrayList<>();
            tabOpenInChildren.add(MenuItemConfig.action("editor.open.in.file.manager", "Show in File Manager"));
            tabOpenInChildren.add(MenuItemConfig.action("editor.open.in.associated.app", "Open in Associated Application"));
            tabOpenInChildren.add(MenuItemConfig.action("editor.open.in.browser", "Open in Browser", "GLOBE"));
            tabOpenInChildren.add(MenuItemConfig.action("editor.open.in.file.path", "File Path"));
            tabOpenInChildren.add(MenuItemConfig.action("editor.open.in.terminal", "Open in Terminal", "TERMINAL"));
            tabOpenInChildren.add(MenuItemConfig.action("editor.open.in.terminal.second", "Open in Terminal", "TERMINAL"));
            tabOpenInChildren.add(MenuItemConfig.action("git.hosting.open.in.browser.group", "Git.Hosting.Open.In.Browser.Group"));
            tabSplitRevealChildren.add(MenuItemConfig.group("editor.open.in.group", "Open In", true, tabOpenInChildren));

            tabActions1Children.add(MenuItemConfig.group("tab.split.reveal.group", "SplitRevealGroup", false, tabSplitRevealChildren));
            tabActions1Children.add(MenuItemConfig.separator());

            // VCS/LVCS Actions (Image: media_1790694503981.png)
            List<MenuItemConfig> tabVcsLvcsChildren = new ArrayList<>();
            List<MenuItemConfig> tabLocalHistoryChildren = new ArrayList<>();
            tabLocalHistoryChildren.add(MenuItemConfig.action("editor.local.history.show", "Show History\u2026"));
            tabLocalHistoryChildren.add(MenuItemConfig.action("editor.local.history.show.selection", "Show History for Selection\u2026"));
            tabLocalHistoryChildren.add(MenuItemConfig.separator());
            tabLocalHistoryChildren.add(MenuItemConfig.action("editor.local.history.show.project", "Show Project History\u2026"));
            tabLocalHistoryChildren.add(MenuItemConfig.action("editor.local.history.recent.changes", "Recent Changes"));
            tabLocalHistoryChildren.add(MenuItemConfig.action("editor.local.history.put.label", "Put Label\u2026"));
            tabLocalHistoryChildren.add(MenuItemConfig.action("editor.local.history.vcs.group", "Version Control Group"));
            tabVcsLvcsChildren.add(MenuItemConfig.group("editor.local.history.group", "Local History", true, tabLocalHistoryChildren));
            tabVcsLvcsChildren.add(MenuItemConfig.separator());
            tabVcsLvcsChildren.add(MenuItemConfig.action("editor.external.tools", "External Tools"));
            tabVcsLvcsChildren.add(MenuItemConfig.action("tab.rename.file", "Rename File\u2026"));

            tabActions1Children.add(MenuItemConfig.group("tab.vcs.lvcs.actions.group", "VCS/LVCS Actions", false, tabVcsLvcsChildren));

            tabPopup.add(MenuItemConfig.group("tab.editor.tab.popup.actions.1", "Editor Tab Popup Menu Actions (1)", false, tabActions1Children));
            roots.add(MenuItemConfig.group("root.editor.tab.popup", "Editor Tab Popup Menu", tabPopup));

            // 6. Project View Popup Menu
            List<MenuItemConfig> projectPopup = new ArrayList<>();
            projectPopup.add(MenuItemConfig.action("project.attach.directory", "Attach Directory to Project\u2026"));
            projectPopup.add(MenuItemConfig.group("project.new", "New", List.of(
                    MenuItemConfig.action("file.new.project", "Project\u2026"),
                    MenuItemConfig.action("file.new.sqlfile", "SQL File"),
                    MenuItemConfig.action("file.new.scratch", "Scratch File"),
                    MenuItemConfig.action("file.new.console", "Query Console")
            )));
            projectPopup.add(MenuItemConfig.action("project.associate.file.type", "Associate with File Type\u2026"));
            projectPopup.add(MenuItemConfig.action("project.restore.default.extensions", "Restore Default Extensions"));
            projectPopup.add(MenuItemConfig.separator());
            projectPopup.add(MenuItemConfig.group("project.cut.copy.paste", "Cut/Copy/Paste Actions", List.of(
                    MenuItemConfig.action("edit.cut", "Cut"),
                    MenuItemConfig.action("edit.copy", "Copy"),
                    MenuItemConfig.action("edit.paste", "Paste")
            )));
            projectPopup.add(MenuItemConfig.group("fileeditor.import.to.database.group", "FileEditor.ImportToDatabase.Group", List.of(
                    MenuItemConfig.action("file.import.data", "Import to Database\u2026")
            )));
            projectPopup.add(MenuItemConfig.action("project.edit.source", "Edit Source"));
            projectPopup.add(MenuItemConfig.group("changesview.applypatch.langgroup", "ChangesView.ApplyPatch.LangGroup", List.of()));
            projectPopup.add(MenuItemConfig.separator());
            projectPopup.add(MenuItemConfig.action("edit.find.usages", "Find Usages"));
            projectPopup.add(MenuItemConfig.action("edit.find.files", "Find in Files\u2026"));
            projectPopup.add(MenuItemConfig.action("edit.replace.files", "Replace in Files\u2026"));
            projectPopup.add(MenuItemConfig.group("project.inspect.code.action", "InspectCodeActionInPopupMenus", List.of()));
            projectPopup.add(MenuItemConfig.separator());
            projectPopup.add(MenuItemConfig.action("project.rename", "Rename\u2026"));
            projectPopup.add(MenuItemConfig.group("project.refactoring.group", "Project View Popup Refactoring Group", List.of()));
            projectPopup.add(MenuItemConfig.group("project.anonymous.group.0", "<anonymous-group-0>", List.of()));
            projectPopup.add(MenuItemConfig.separator());
            projectPopup.add(MenuItemConfig.group("project.modify.group", "Project View Popup Menu Modify Group", List.of()));
            projectPopup.add(MenuItemConfig.separator());
            projectPopup.add(MenuItemConfig.group("project.run.group", "Project View Popup Menu Run Group", List.of()));
            projectPopup.add(MenuItemConfig.group("project.split.reveal.group", "SplitRevealGroup", List.of()));
            projectPopup.add(MenuItemConfig.separator());
            projectPopup.add(MenuItemConfig.group("project.vcs.lvcs.actions", "VCS/LVCS Actions", List.of(
                    MenuItemConfig.action("vcs.commit", "Commit Directory\u2026"),
                    MenuItemConfig.action("vcs.diff", "Show Diff")
            )));
            projectPopup.add(MenuItemConfig.action("project.cache.recovery", "Cache Recovery"));
            projectPopup.add(MenuItemConfig.action("file.reload.disk", "Reload from Disk"));
            projectPopup.add(MenuItemConfig.separator());
            projectPopup.add(MenuItemConfig.action("project.goto.link.target", "Go to Link Target"));
            projectPopup.add(MenuItemConfig.separator());
            projectPopup.add(MenuItemConfig.action("project.compare.files", "Compare Files"));
            projectPopup.add(MenuItemConfig.action("project.compare.file.editor", "Compare File with Editor"));
            projectPopup.add(MenuItemConfig.separator());
            projectPopup.add(MenuItemConfig.group("project.external.tools", "External Tools", List.of()));
            projectPopup.add(MenuItemConfig.separator());
            projectPopup.add(MenuItemConfig.group("project.settings.group", "Project View Popup Menu Settings Group", List.of()));
            projectPopup.add(MenuItemConfig.action("project.set.background.image", "Set Background Image"));
            projectPopup.add(MenuItemConfig.group("project.diagrams", "Diagrams", List.of()));
            projectPopup.add(MenuItemConfig.action("project.file.associations", "File Associations"));
            projectPopup.add(MenuItemConfig.action("project.jump.external.editor", "Jump to External Editor"));
            projectPopup.add(MenuItemConfig.action("project.convert.to.png", "Convert to PNG"));
            roots.add(MenuItemConfig.group("root.project.view.popup", "Project View Popup Menu", projectPopup));

            // 7. Scope View Popup Menu
            List<MenuItemConfig> scopePopup = new ArrayList<>();
            scopePopup.add(MenuItemConfig.group("scope.project.view.popup", "Project View Popup Menu", List.of()));
            roots.add(MenuItemConfig.group("root.scope.view.popup", "Scope View Popup Menu", scopePopup));

            // 8. Navigation Bar Popup Menu
            List<MenuItemConfig> navBarPopup = new ArrayList<>();
            navBarPopup.add(MenuItemConfig.group("navbar.new", "New", List.of()));
            navBarPopup.add(MenuItemConfig.action("navbar.associate.file.type", "Associate with File Type\u2026"));
            navBarPopup.add(MenuItemConfig.separator());
            navBarPopup.add(MenuItemConfig.group("navbar.cut.copy.paste", "Cut/Copy/Paste Actions", List.of()));
            navBarPopup.add(MenuItemConfig.action("navbar.jump.source", "Jump to Source"));
            navBarPopup.add(MenuItemConfig.group("navbar.changesview.applypatch.langgroup", "ChangesView.ApplyPatch.LangGroup", List.of()));
            navBarPopup.add(MenuItemConfig.separator());
            navBarPopup.add(MenuItemConfig.action("navbar.navigation.bar", "Navigation Bar"));
            navBarPopup.add(MenuItemConfig.action("navbar.members.navigation.bar", "Members in Navigation Bar"));
            navBarPopup.add(MenuItemConfig.separator());
            navBarPopup.add(MenuItemConfig.action("navbar.find.usages", "Find Usages"));
            navBarPopup.add(MenuItemConfig.action("navbar.find.files", "Find in Files\u2026"));
            navBarPopup.add(MenuItemConfig.action("navbar.replace.files", "Replace in Files\u2026"));
            navBarPopup.add(MenuItemConfig.separator());
            navBarPopup.add(MenuItemConfig.action("navbar.rename", "Rename\u2026"));
            navBarPopup.add(MenuItemConfig.group("navbar.refactoring.group", "Project View Popup Refactoring Group", List.of()));
            navBarPopup.add(MenuItemConfig.separator());
            navBarPopup.add(MenuItemConfig.group("navbar.modify.group", "Project View Popup Menu Modify Group", List.of()));
            navBarPopup.add(MenuItemConfig.separator());
            navBarPopup.add(MenuItemConfig.group("navbar.run.group", "Project View Popup Menu Run Group", List.of()));
            navBarPopup.add(MenuItemConfig.group("navbar.split.reveal.group", "SplitRevealGroup", List.of()));
            navBarPopup.add(MenuItemConfig.separator());
            navBarPopup.add(MenuItemConfig.group("navbar.vcs.lvcs.actions", "VCS/LVCS Actions", List.of()));
            navBarPopup.add(MenuItemConfig.action("file.reload.disk", "Reload from Disk"));
            navBarPopup.add(MenuItemConfig.separator());
            navBarPopup.add(MenuItemConfig.group("navbar.external.tools", "External Tools", List.of()));
            navBarPopup.add(MenuItemConfig.separator());
            navBarPopup.add(MenuItemConfig.group("navbar.settings.group", "Project View Popup Menu Settings Group", List.of()));
            navBarPopup.add(MenuItemConfig.group("navbar.diagrams", "Diagrams", List.of()));
            roots.add(MenuItemConfig.group("root.navigation.bar.popup", "Navigation Bar Popup Menu", navBarPopup));

            // 9. Navigation Bar Toolbar
            List<MenuItemConfig> navBarToolbar = new ArrayList<>();
            navBarToolbar.add(MenuItemConfig.group("navbar.toolbar.run.actions", "Toolbar Run Actions", List.of()));
            navBarToolbar.add(MenuItemConfig.separator());
            navBarToolbar.add(MenuItemConfig.group("navbar.vcs.group", "NavBarVcsGroup", List.of()));
            navBarToolbar.add(MenuItemConfig.separator());
            navBarToolbar.add(MenuItemConfig.action("navbar.toolbar.others", "NavBarToolBarOthers"));
            navBarToolbar.add(MenuItemConfig.separator());
            navBarToolbar.add(MenuItemConfig.action("navbar.ai.assistant", "@ AIAssistantHubPopupAction"));
            navBarToolbar.add(MenuItemConfig.action("nav.search.everywhere", "Search Everywhere"));
            navBarToolbar.add(MenuItemConfig.action("file.settings", "IDE and Project Settings"));
            roots.add(MenuItemConfig.group("root.navigation.bar.toolbar", "Navigation Bar Toolbar", navBarToolbar));

            // 10. Debug Header More Popup
            List<MenuItemConfig> debugMore = new ArrayList<>();
            debugMore.add(MenuItemConfig.action("debug.force.step.over", "Force Step Over"));
            debugMore.add(MenuItemConfig.action("debug.force.step.into", "Force Step Into"));
            debugMore.add(MenuItemConfig.action("debug.smart.step.into", "Smart Step Into"));
            debugMore.add(MenuItemConfig.separator());
            debugMore.add(MenuItemConfig.action("debug.run.to.cursor", "Run to Cursor"));
            debugMore.add(MenuItemConfig.action("debug.force.run.to.cursor", "Force Run to Cursor"));
            debugMore.add(MenuItemConfig.separator());
            debugMore.add(MenuItemConfig.action("debug.show.point", "Show Execution Point"));
            debugMore.add(MenuItemConfig.separator());
            debugMore.add(MenuItemConfig.action("debug.evaluate.expression", "Evaluate Expression\u2026"));
            debugMore.add(MenuItemConfig.action("debug.reset.frame", "Reset Frame"));
            roots.add(MenuItemConfig.group("root.debug.header.more.popup", "Debug Header More Popup", debugMore));

            // 11. Debug Header Toolbar
            List<MenuItemConfig> debugToolbar = new ArrayList<>();
            debugToolbar.add(MenuItemConfig.action("middle.run", "Run"));
            debugToolbar.add(MenuItemConfig.action("debug.start", "Debug"));
            debugToolbar.add(MenuItemConfig.action("debug.rerun", "Rerun"));
            debugToolbar.add(MenuItemConfig.action("debug.stop", "Stop"));
            debugToolbar.add(MenuItemConfig.separator());
            debugToolbar.add(MenuItemConfig.group("debug.resume.ref", "Resume.Ref", List.of()));
            debugToolbar.add(MenuItemConfig.group("debug.pause.ref", "Pause.Ref", List.of()));
            debugToolbar.add(MenuItemConfig.group("debug.stepover.ref", "StepOver.Ref", List.of()));
            debugToolbar.add(MenuItemConfig.action("debug.step.into", "Step Into"));
            debugToolbar.add(MenuItemConfig.action("debug.step.out", "Step Out"));
            debugToolbar.add(MenuItemConfig.separator());
            debugToolbar.add(MenuItemConfig.action("debug.view.breakpoints", "View Breakpoints\u2026"));
            debugToolbar.add(MenuItemConfig.action("debug.mute.breakpoints", "Mute Breakpoints"));
            roots.add(MenuItemConfig.group("root.debug.header.toolbar", "Debug Header Toolbar", debugToolbar));

            // 12. Debug Watches Toolbar
            List<MenuItemConfig> debugWatches = new ArrayList<>();
            debugWatches.add(MenuItemConfig.action("watches.new", "+ New Watch\u2026"));
            debugWatches.add(MenuItemConfig.action("watches.remove", "\u2014 Remove Watch"));
            debugWatches.add(MenuItemConfig.action("watches.move.up", "Move Watch Up"));
            debugWatches.add(MenuItemConfig.action("watches.move.down", "Move Watch Down"));
            debugWatches.add(MenuItemConfig.action("watches.duplicate", "Duplicate Watch"));
            roots.add(MenuItemConfig.group("root.debug.watches.toolbar", "Debug Watches Toolbar", debugWatches));

            // 13. File History Toolbar
            List<MenuItemConfig> historyToolbar = new ArrayList<>();
            historyToolbar.add(MenuItemConfig.action("vcs.history.refresh", "Refresh"));
            historyToolbar.add(MenuItemConfig.action("vcs.history.diff", "Show Diff"));
            historyToolbar.add(MenuItemConfig.action("vcs.history.show.affected.files", "Show All Affected Files"));
            historyToolbar.add(MenuItemConfig.separator());
            historyToolbar.add(MenuItemConfig.group("vcs.history.view.options", "View Options", List.of()));
            historyToolbar.add(MenuItemConfig.separator());
            historyToolbar.add(MenuItemConfig.group("vcs.history.actions.group.toolbar", "VcsHistoryActionsGroup.Toolbar", List.of()));
            historyToolbar.add(MenuItemConfig.action("vcs.history.resume.indexing", "Resume Indexing"));
            roots.add(MenuItemConfig.group("root.file.history.toolbar", "File History Toolbar", historyToolbar));

            // 14. Floating Code Toolbar
            List<MenuItemConfig> floatingCode = new ArrayList<>();
            floatingCode.add(MenuItemConfig.group("code.extract.group", "Extract", List.of()));
            floatingCode.add(MenuItemConfig.group("code.surround.group", "Surround", List.of()));
            floatingCode.add(MenuItemConfig.group("xdebugger.code.toolbar", "XDebugger.Code.Toolbar", List.of()));
            floatingCode.add(MenuItemConfig.action("code.comment.line", "// Comment with Line Comment"));
            floatingCode.add(MenuItemConfig.action("code.reformat", "Reformat Code"));
            roots.add(MenuItemConfig.group("root.floating.code.toolbar", "Floating Code Toolbar", floatingCode));

            // 15. Markdown Editor Floating Toolbar
            List<MenuItemConfig> mdToolbar = new ArrayList<>();
            mdToolbar.add(MenuItemConfig.action("md.set.header.style", "Set Header Style"));
            mdToolbar.add(MenuItemConfig.separator());
            mdToolbar.add(MenuItemConfig.action("md.bold", "Bold"));
            mdToolbar.add(MenuItemConfig.action("md.italic", "Italic"));
            mdToolbar.add(MenuItemConfig.action("md.strikethrough", "Strikethrough"));
            mdToolbar.add(MenuItemConfig.action("md.code", "<> Code"));
            mdToolbar.add(MenuItemConfig.action("md.create.link", "Create Link"));
            mdToolbar.add(MenuItemConfig.separator());
            mdToolbar.add(MenuItemConfig.action("md.create.or.change.list", "Create Or Change List"));
            roots.add(MenuItemConfig.group("root.markdown.editor.floating.toolbar", "Markdown Editor Floating Toolbar", mdToolbar));

            // 16. Quick Actions Popup Toolbar
            List<MenuItemConfig> quickActions = new ArrayList<>();
            quickActions.add(MenuItemConfig.action("quick.actions.load.full.cell", "Load Full Cell"));
            quickActions.add(MenuItemConfig.action("quick.actions.related.rows", "Related Rows"));
            quickActions.add(MenuItemConfig.action("quick.actions.open.url", "Open URL"));
            quickActions.add(MenuItemConfig.action("quick.actions.open.file.uri", "Open File URI"));
            roots.add(MenuItemConfig.group("root.quick.actions.popup.toolbar", "Quick Actions Popup Toolbar", quickActions));

            // 17. Run Tool Window Header More Popup
            List<MenuItemConfig> runMore = new ArrayList<>();
            roots.add(MenuItemConfig.group("root.run.toolwindow.header.more.popup", "Run Tool Window Header More Popup", runMore));

            // 18. Run Tool Window Header Toolbar
            List<MenuItemConfig> runHeaderToolbar = new ArrayList<>();
            runHeaderToolbar.add(MenuItemConfig.action("middle.run", "Run"));
            runHeaderToolbar.add(MenuItemConfig.action("debug.start", "Debug"));
            runHeaderToolbar.add(MenuItemConfig.action("run.header.rerun", "Rerun"));
            runHeaderToolbar.add(MenuItemConfig.action("run.stop", "Stop"));
            roots.add(MenuItemConfig.group("root.run.toolwindow.header.toolbar", "Run Tool Window Header Toolbar", runHeaderToolbar));

            // 19. SQL Floating Toolbar
            List<MenuItemConfig> sqlFloating = new ArrayList<>();
            sqlFloating.add(MenuItemConfig.group("sql.floating.code.toolbar", "Floating Code Toolbar", List.of()));
            sqlFloating.add(MenuItemConfig.action("middle.run", "Execute"));
            sqlFloating.add(MenuItemConfig.action("editor.explain.plan", "Explain Plan"));
            roots.add(MenuItemConfig.group("root.sql.floating.toolbar", "SQL Floating Toolbar", sqlFloating));

            // 20. VCS Local Changes Toolbar
            List<MenuItemConfig> vcsLocal = new ArrayList<>();
            vcsLocal.add(MenuItemConfig.action("vcs.local.refresh", "Refresh"));
            vcsLocal.add(MenuItemConfig.action("vcs.local.commit", "Commit\u2026"));
            vcsLocal.add(MenuItemConfig.action("vcs.local.toggle.commit.ui", "Toggle Commit UI\u2026"));
            vcsLocal.add(MenuItemConfig.action("vcs.local.rollback", "Rollback\u2026"));
            vcsLocal.add(MenuItemConfig.action("vcs.local.show.diff", "Show Diff"));
            vcsLocal.add(MenuItemConfig.group("vcs.local.changelists", "Changelists", List.of()));
            vcsLocal.add(MenuItemConfig.action("vcs.local.shelve.silently", "Shelve Silently"));
            roots.add(MenuItemConfig.group("root.vcs.local.changes.toolbar", "VCS Local Changes Toolbar", vcsLocal));

            // 21. VCS Log Changes Browser Toolbar
            List<MenuItemConfig> vcsLogChanges = new ArrayList<>();
            vcsLogChanges.add(MenuItemConfig.group("vcs.repository.changes.browser.toolbar", "Vcs.RepositoryChangesBrowserToolbar", List.of()));
            vcsLogChanges.add(MenuItemConfig.action("vcs.log.show.affected.changes", "Show Only Affected Changes"));
            vcsLogChanges.add(MenuItemConfig.separator());
            vcsLogChanges.add(MenuItemConfig.group("vcs.log.changes.view.options", "View Options", List.of()));
            roots.add(MenuItemConfig.group("root.vcs.log.changes.browser.toolbar", "VCS Log Changes Browser Toolbar", vcsLogChanges));

            // 22. VCS Log Toolbar
            List<MenuItemConfig> vcsLogToolbar = new ArrayList<>();
            vcsLogToolbar.add(MenuItemConfig.action("vcs.log.resume.indexing", "Resume Indexing"));
            vcsLogToolbar.add(MenuItemConfig.action("vcs.log.refresh", "Refresh"));
            vcsLogToolbar.add(MenuItemConfig.group("vcs.log.toolbar.group", "Vcs.Log.Toolbar", List.of()));
            vcsLogToolbar.add(MenuItemConfig.group("vcs.log.view.options", "View Options", List.of()));
            vcsLogToolbar.add(MenuItemConfig.action("vcs.log.goto.hash", "Go To Hash/Branch/Tag"));
            roots.add(MenuItemConfig.group("root.vcs.log.toolbar", "VCS Log Toolbar", vcsLogToolbar));

            // 23. VCS Operations Popup
            List<MenuItemConfig> vcsOps = new ArrayList<>();
            vcsOps.add(MenuItemConfig.group("vcs.operations.popup.vcsaware", "Vcs.Operations.Popup.VcsAware", List.of()));
            vcsOps.add(MenuItemConfig.separator());
            vcsOps.add(MenuItemConfig.action("vcs.operations.popup.vcs.providers", "Vcs.Operations.Popup.Vcs.Providers"));
            vcsOps.add(MenuItemConfig.group("vcs.operations.popup.nonvcsaware", "Vcs.Operations.Popup.NonVcsAware", List.of()));
            vcsOps.add(MenuItemConfig.separator());
            vcsOps.add(MenuItemConfig.action("vcs.operations.annotated.line", "Annotated Line"));
            vcsOps.add(MenuItemConfig.action("vcs.operations.show.history", "Show History\u2026"));
            roots.add(MenuItemConfig.group("root.vcs.operations.popup", "VCS Operations Popup", vcsOps));

            return roots;
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
        public double getEditorLineHeight() { return editorLineHeight; }
        public void setEditorLineHeight(double editorLineHeight) { this.editorLineHeight = editorLineHeight; }
        public boolean isEditorEnableLigatures() { return editorEnableLigatures; }
        public void setEditorEnableLigatures(boolean editorEnableLigatures) { this.editorEnableLigatures = editorEnableLigatures; }
        public String getEditorFontMainWeight() { return editorFontMainWeight; }
        public void setEditorFontMainWeight(String editorFontMainWeight) { this.editorFontMainWeight = editorFontMainWeight != null ? editorFontMainWeight : "Regular"; }
        public String getEditorFontBoldWeight() { return editorFontBoldWeight; }
        public void setEditorFontBoldWeight(String editorFontBoldWeight) { this.editorFontBoldWeight = editorFontBoldWeight != null ? editorFontBoldWeight : "Bold Recommended"; }
        public String getEditorFallbackFont() { return editorFallbackFont; }
        public void setEditorFallbackFont(String editorFallbackFont) { this.editorFallbackFont = editorFallbackFont != null ? editorFallbackFont : "<None>"; }
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
        public static boolean isMac() {
            return System.getProperty("os.name", "").toLowerCase().contains("mac");
        }

        public static String defaultKeymapPreset() {
            return isMac() ? "macOS" : "Windows";
        }

        public static List<String> defaultKeymapPresets() {
            if (isMac()) {
                return new ArrayList<>(List.of(
                        "macOS",
                        "Emacs",
                        "IntelliJ IDEA Classic",
                        "macOS System Shortcuts",
                        "Sublime Text",
                        "Sublime Text (macOS)"
                ));
            } else {
                return new ArrayList<>(List.of(
                        "Windows",
                        "IntelliJ IDEA Classic",
                        "Emacs",
                        "Eclipse",
                        "Visual Studio",
                        "Sublime Text"
                ));
            }
        }

        public String getKeymapPreset() {
            if (keymapPreset == null || keymapPreset.isBlank() || "DataGrip Default".equals(keymapPreset)) {
                keymapPreset = defaultKeymapPreset();
            }
            return keymapPreset;
        }
        public void setKeymapPreset(String keymapPreset) { this.keymapPreset = keymapPreset; }

        public List<String> getCustomKeymapPresets() {
            if (customKeymapPresets == null) {
                customKeymapPresets = new ArrayList<>();
            }
            return customKeymapPresets;
        }
        public void setCustomKeymapPresets(List<String> customKeymapPresets) {
            this.customKeymapPresets = customKeymapPresets != null ? customKeymapPresets : new ArrayList<>();
        }

        public Map<String, List<String>> getCustomKeymapShortcuts() {
            if (customKeymapShortcuts == null) {
                customKeymapShortcuts = new LinkedHashMap<>();
            }
            return customKeymapShortcuts;
        }
        public void setCustomKeymapShortcuts(Map<String, List<String>> customKeymapShortcuts) {
            this.customKeymapShortcuts = customKeymapShortcuts != null ? customKeymapShortcuts : new LinkedHashMap<>();
        }

        public Map<String, List<String>> getRemovedKeymapShortcuts() {
            if (removedKeymapShortcuts == null) {
                removedKeymapShortcuts = new LinkedHashMap<>();
            }
            return removedKeymapShortcuts;
        }
        public void setRemovedKeymapShortcuts(Map<String, List<String>> removedKeymapShortcuts) {
            this.removedKeymapShortcuts = removedKeymapShortcuts != null ? removedKeymapShortcuts : new LinkedHashMap<>();
        }
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

        // Menus and Toolbars Getters and Setters
        public List<MenuItemConfig> getMenusAndToolbars() {
            if (menusAndToolbars == null || menusAndToolbars.isEmpty()) {
                menusAndToolbars = defaultMenusAndToolbars();
            } else {
                for (int i = 0; i < menusAndToolbars.size(); i++) {
                    MenuItemConfig root = menusAndToolbars.get(i);
                    if ("root.main.menu".equalsIgnoreCase(root.getId())) {
                        MenuItemConfig fileMenu = root.getChildren().stream()
                                .filter(c -> "menu.file".equalsIgnoreCase(c.getId()) || "File".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);
                        MenuItemConfig editMenu = root.getChildren().stream()
                                .filter(c -> "menu.edit".equalsIgnoreCase(c.getId()) || "Edit".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);
                        MenuItemConfig viewMenu = root.getChildren().stream()
                                .filter(c -> "menu.view".equalsIgnoreCase(c.getId()) || "View".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);
                        MenuItemConfig navMenu = root.getChildren().stream()
                                .filter(c -> "menu.navigate".equalsIgnoreCase(c.getId()) || "Navigate".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);
                        MenuItemConfig codeMenu = root.getChildren().stream()
                                .filter(c -> "menu.code".equalsIgnoreCase(c.getId()) || "Code".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);
                        MenuItemConfig refactorMenu = root.getChildren().stream()
                                .filter(c -> "menu.refactor".equalsIgnoreCase(c.getId()) || "Refactor".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);
                        MenuItemConfig runMenu = root.getChildren().stream()
                                .filter(c -> "menu.run".equalsIgnoreCase(c.getId()) || "Run".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);
                        MenuItemConfig toolsMenu = root.getChildren().stream()
                                .filter(c -> "menu.tools".equalsIgnoreCase(c.getId()) || "Tools".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);
                        MenuItemConfig vcsMenu = root.getChildren().stream()
                                .filter(c -> "menu.vcs".equalsIgnoreCase(c.getId()) || "Git".equalsIgnoreCase(c.getText()) || "VCS".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);
                        MenuItemConfig windowMenu = root.getChildren().stream()
                                .filter(c -> "menu.window".equalsIgnoreCase(c.getId()) || "Window".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);
                        MenuItemConfig helpMenu = root.getChildren().stream()
                                .filter(c -> "menu.help".equalsIgnoreCase(c.getId()) || "Help".equalsIgnoreCase(c.getText()))
                                .findFirst().orElse(null);

                        boolean needsMigration = false;
                        if (fileMenu != null && fileMenu.getChildren().stream().noneMatch(c -> "file.open.actions".equalsIgnoreCase(c.getId()))) {
                            needsMigration = true;
                        }
                        if (editMenu != null && editMenu.getChildren().stream().noneMatch(c -> "edit.generate.root.group".equalsIgnoreCase(c.getId()))) {
                            needsMigration = true;
                        }
                        if (viewMenu != null && viewMenu.getChildren().stream().noneMatch(c -> "view.recent.actions.group".equalsIgnoreCase(c.getId()))) {
                            needsMigration = true;
                        }
                        if (navMenu != null && navMenu.getChildren().stream().noneMatch(c -> "nav.goto.by.name.group".equalsIgnoreCase(c.getId()))) {
                            needsMigration = true;
                        }
                        if (codeMenu != null) {
                            boolean hasExpandedInspect = codeMenu.getChildren().stream()
                                    .filter(c -> "code.inspect.group".equalsIgnoreCase(c.getId()))
                                    .anyMatch(g -> !g.getChildren().isEmpty());
                            boolean hasExpandedFormatting = codeMenu.getChildren().stream()
                                    .filter(c -> "code.formatting.actions.group".equalsIgnoreCase(c.getId()))
                                    .anyMatch(g -> !g.getChildren().isEmpty());
                            if (!hasExpandedInspect || !hasExpandedFormatting) {
                                needsMigration = true;
                            }
                        }
                        if (refactorMenu != null && refactorMenu.getChildren().stream().noneMatch(c -> "refactor.this".equalsIgnoreCase(c.getId()))) {
                            needsMigration = true;
                        }
                        if (runMenu != null && runMenu.getChildren().stream().noneMatch(c -> "run.run.debug.group".equalsIgnoreCase(c.getId()))) {
                            needsMigration = true;
                        }
                        if (!root.getChildren().isEmpty()) {
                            if (fileMenu != null && toolsMenu == null) {
                                needsMigration = true;
                            }
                            if (toolsMenu != null && toolsMenu.getChildren().stream().noneMatch(c -> "tools.psi.viewer.actions".equalsIgnoreCase(c.getId()))) {
                                needsMigration = true;
                            }
                            if (vcsMenu != null) {
                                boolean hasVcsMain = vcsMenu.getChildren().stream().anyMatch(c -> "vcs.main.menu".equalsIgnoreCase(c.getId()));
                                boolean hasExpandedGitMain = vcsMenu.getChildren().stream()
                                        .filter(c -> "git.main.menu".equalsIgnoreCase(c.getId()))
                                        .anyMatch(g -> !g.getChildren().isEmpty());
                                if (!hasVcsMain || !hasExpandedGitMain) {
                                    needsMigration = true;
                                }
                            }
                            if (windowMenu != null) {
                                boolean hasOpenProj = windowMenu.getChildren().stream().anyMatch(c -> "window.open.project.windows".equalsIgnoreCase(c.getId()));
                                boolean hasEditorTabsGroup = windowMenu.getChildren().stream()
                                        .filter(c -> "window.editor.tabs".equalsIgnoreCase(c.getId()))
                                        .anyMatch(g -> g.getChildren().size() > 5);
                                if (!hasOpenProj || !hasEditorTabsGroup) {
                                    needsMigration = true;
                                }
                            }
                            if (helpMenu != null) {
                                boolean hasDiagnosticTools = helpMenu.getChildren().stream()
                                        .anyMatch(c -> "help.diagnostic.tools".equalsIgnoreCase(c.getId()));
                                boolean hasProductivity = helpMenu.getChildren().stream()
                                        .anyMatch(c -> "help.productivity.features".equalsIgnoreCase(c.getId()));
                                boolean hasRegistration = helpMenu.getChildren().stream()
                                        .anyMatch(c -> "help.registration.actions".equalsIgnoreCase(c.getId()));
                                if (!hasDiagnosticTools || !hasProductivity || !hasRegistration || helpMenu.getChildren().size() < 10) {
                                    needsMigration = true;
                                }
                            }
                        }
                        if (needsMigration) {
                            for (MenuItemConfig defRoot : defaultMenusAndToolbars()) {
                                if ("root.main.menu".equalsIgnoreCase(defRoot.getId())) {
                                    menusAndToolbars.set(i, defRoot.copy());
                                    break;
                                }
                            }
                        }
                    } else if ("root.main.toolbar".equalsIgnoreCase(root.getId())) {
                        boolean needsTbMigration = root.getChildren().stream()
                                .noneMatch(c -> c.getChildren().stream().anyMatch(sub -> "toolbar.single.tool.window.bar".equalsIgnoreCase(sub.getId()) || "Single Tool Window Bar".equalsIgnoreCase(sub.getText())));
                        if (needsTbMigration) {
                            for (MenuItemConfig defRoot : defaultMenusAndToolbars()) {
                                if ("root.main.toolbar".equalsIgnoreCase(defRoot.getId())) {
                                    menusAndToolbars.set(i, defRoot.copy());
                                    break;
                                }
                            }
                        }
                    } else if ("root.editor.popup".equalsIgnoreCase(root.getId())) {
                        boolean needsPopupMigration = root.getChildren().stream()
                                .noneMatch(c -> "editor.popup.menu.2".equalsIgnoreCase(c.getId()) && !c.getChildren().isEmpty());
                        if (needsPopupMigration) {
                            for (MenuItemConfig defRoot : defaultMenusAndToolbars()) {
                                if ("root.editor.popup".equalsIgnoreCase(defRoot.getId())) {
                                    menusAndToolbars.set(i, defRoot.copy());
                                    break;
                                }
                            }
                        }
                    } else if ("root.editor.gutter.popup".equalsIgnoreCase(root.getId())) {
                        boolean needsGutterMigration = root.getChildren().stream()
                                .noneMatch(c -> "gutter.appearance".equalsIgnoreCase(c.getId()) && !c.getChildren().isEmpty());
                        if (needsGutterMigration) {
                            for (MenuItemConfig defRoot : defaultMenusAndToolbars()) {
                                if ("root.editor.gutter.popup".equalsIgnoreCase(defRoot.getId())) {
                                    menusAndToolbars.set(i, defRoot.copy());
                                    break;
                                }
                            }
                        }
                    } else if ("root.editor.tab.popup".equalsIgnoreCase(root.getId())) {
                        boolean needsTabMigration = root.getChildren().stream()
                                .noneMatch(c -> "tab.editor.tab.popup.actions.1".equalsIgnoreCase(c.getId()) &&
                                        c.getChildren().stream().anyMatch(sub -> "tab.vcs.lvcs.actions.group".equalsIgnoreCase(sub.getId())));
                        if (needsTabMigration) {
                            for (MenuItemConfig defRoot : defaultMenusAndToolbars()) {
                                if ("root.editor.tab.popup".equalsIgnoreCase(defRoot.getId())) {
                                    menusAndToolbars.set(i, defRoot.copy());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return menusAndToolbars;
        }

        public void setMenusAndToolbars(List<MenuItemConfig> menusAndToolbars) {
            this.menusAndToolbars = (menusAndToolbars != null && !menusAndToolbars.isEmpty())
                    ? new ArrayList<>(menusAndToolbars)
                    : defaultMenusAndToolbars();
        }

        public MenuItemConfig getMenuConfig(String rootId) {
            if (rootId == null) return null;
            for (MenuItemConfig root : getMenusAndToolbars()) {
                if (rootId.equalsIgnoreCase(root.getId()) || rootId.equalsIgnoreCase(root.getText())) {
                    return root;
                }
            }
            // fallback search in default
            for (MenuItemConfig root : defaultMenusAndToolbars()) {
                if (rootId.equalsIgnoreCase(root.getId()) || rootId.equalsIgnoreCase(root.getText())) {
                    MenuItemConfig copy = root.copy();
                    getMenusAndToolbars().add(copy);
                    return copy;
                }
            }
            return null;
        }

        public void restoreMenu(String rootId) {
            if (rootId == null) return;
            List<MenuItemConfig> defaults = defaultMenusAndToolbars();
            MenuItemConfig defaultItem = null;
            for (MenuItemConfig def : defaults) {
                if (rootId.equalsIgnoreCase(def.getId()) || rootId.equalsIgnoreCase(def.getText())) {
                    defaultItem = def.copy();
                    break;
                }
            }
            if (defaultItem != null) {
                List<MenuItemConfig> current = getMenusAndToolbars();
                for (int i = 0; i < current.size(); i++) {
                    if (rootId.equalsIgnoreCase(current.get(i).getId()) || rootId.equalsIgnoreCase(current.get(i).getText())) {
                        current.set(i, defaultItem);
                        return;
                    }
                }
                current.add(defaultItem);
            }
        }

        public void restoreAllMenus() {
            this.menusAndToolbars = defaultMenusAndToolbars();
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

        public String getEditorColorScheme() {
            if (editorColorScheme == null || editorColorScheme.isBlank() || "Islands Dark Theme default".equals(editorColorScheme)) {
                editorColorScheme = "Dark Theme default";
            }
            return editorColorScheme;
        }
        public void setEditorColorScheme(String editorColorScheme) {
            this.editorColorScheme = (editorColorScheme != null && !editorColorScheme.isBlank())
                    ? editorColorScheme : "Dark Theme default";
        }

        public List<String> getCustomColorSchemes() {
            if (customColorSchemes == null) customColorSchemes = new ArrayList<>();
            return customColorSchemes;
        }
        public void setCustomColorSchemes(List<String> customColorSchemes) {
            this.customColorSchemes = customColorSchemes != null ? customColorSchemes : new ArrayList<>();
        }

        public Map<String, Map<String, ColorSchemeAttribute>> getColorSchemeOverrides() {
            if (colorSchemeOverrides == null) colorSchemeOverrides = new LinkedHashMap<>();
            return colorSchemeOverrides;
        }
        public void setColorSchemeOverrides(Map<String, Map<String, ColorSchemeAttribute>> colorSchemeOverrides) {
            this.colorSchemeOverrides = colorSchemeOverrides != null ? colorSchemeOverrides : new LinkedHashMap<>();
        }

        public boolean isDifferentToolWindowBackground() { return differentToolWindowBackground; }
        public void setDifferentToolWindowBackground(boolean differentToolWindowBackground) { this.differentToolWindowBackground = differentToolWindowBackground; }

        // Color Scheme Font Getters and Setters
        public boolean isUseColorSchemeFontInsteadOfDefault() { return useColorSchemeFontInsteadOfDefault; }
        public void setUseColorSchemeFontInsteadOfDefault(boolean val) { this.useColorSchemeFontInsteadOfDefault = val; }

        public String getColorSchemeFontFamily() {
            return (colorSchemeFontFamily != null && !colorSchemeFontFamily.isBlank()) ? colorSchemeFontFamily : "JetBrains Mono";
        }
        public void setColorSchemeFontFamily(String family) {
            this.colorSchemeFontFamily = (family != null && !family.isBlank()) ? family : "JetBrains Mono";
        }

        public String getColorSchemeFontFallbackFamily() {
            return (colorSchemeFontFallbackFamily != null && !colorSchemeFontFallbackFamily.isBlank()) ? colorSchemeFontFallbackFamily : "<None>";
        }
        public void setColorSchemeFontFallbackFamily(String family) {
            this.colorSchemeFontFallbackFamily = (family != null && !family.isBlank()) ? family : "<None>";
        }

        public double getColorSchemeFontSize() { return colorSchemeFontSize > 0 ? colorSchemeFontSize : 13.0; }
        public void setColorSchemeFontSize(double size) { this.colorSchemeFontSize = size > 0 ? size : 13.0; }

        public double getColorSchemeFontLineHeight() { return colorSchemeFontLineHeight > 0 ? colorSchemeFontLineHeight : 1.2; }
        public void setColorSchemeFontLineHeight(double lh) { this.colorSchemeFontLineHeight = lh > 0 ? lh : 1.2; }

        public boolean isColorSchemeFontEnableLigatures() { return colorSchemeFontEnableLigatures; }
        public void setColorSchemeFontEnableLigatures(boolean val) { this.colorSchemeFontEnableLigatures = val; }

        public boolean isColorSchemeFontShowOnlyMonospaced() { return colorSchemeFontShowOnlyMonospaced; }
        public void setColorSchemeFontShowOnlyMonospaced(boolean val) { this.colorSchemeFontShowOnlyMonospaced = val; }

        // Console Font Getters and Setters
        public boolean isUseConsoleFontInsteadOfDefault() { return useConsoleFontInsteadOfDefault; }
        public void setUseConsoleFontInsteadOfDefault(boolean val) { this.useConsoleFontInsteadOfDefault = val; }

        public String getConsoleFontFamily() {
            return (consoleFontFamily != null && !consoleFontFamily.isBlank()) ? consoleFontFamily : "JetBrains Mono";
        }
        public void setConsoleFontFamily(String family) {
            this.consoleFontFamily = (family != null && !family.isBlank()) ? family : "JetBrains Mono";
        }

        public String getConsoleFontFallbackFamily() {
            return (consoleFontFallbackFamily != null && !consoleFontFallbackFamily.isBlank()) ? consoleFontFallbackFamily : "<None>";
        }
        public void setConsoleFontFallbackFamily(String family) {
            this.consoleFontFallbackFamily = (family != null && !family.isBlank()) ? family : "<None>";
        }

        public double getConsoleFontSize() { return consoleFontSize > 0 ? consoleFontSize : 13.0; }
        public void setConsoleFontSize(double size) { this.consoleFontSize = size > 0 ? size : 13.0; }

        public double getConsoleFontLineHeight() { return consoleFontLineHeight > 0 ? consoleFontLineHeight : 1.2; }
        public void setConsoleFontLineHeight(double lh) { this.consoleFontLineHeight = lh > 0 ? lh : 1.2; }

        public boolean isConsoleFontEnableLigatures() { return consoleFontEnableLigatures; }
        public void setConsoleFontEnableLigatures(boolean val) { this.consoleFontEnableLigatures = val; }

        public boolean isConsoleFontShowOnlyMonospaced() { return consoleFontShowOnlyMonospaced; }
        public void setConsoleFontShowOnlyMonospaced(boolean val) { this.consoleFontShowOnlyMonospaced = val; }

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

        // Sticky Lines
        public boolean isStickyLinesEnabled() { return stickyLinesEnabled; }
        public void setStickyLinesEnabled(boolean val) { this.stickyLinesEnabled = val; }
        public int getStickyLinesMaxLines() { return stickyLinesMaxLines; }
        public void setStickyLinesMaxLines(int val) { this.stickyLinesMaxLines = Math.max(1, Math.min(20, val)); }
        public boolean isStickyLinesHtml() { return stickyLinesHtml; }
        public void setStickyLinesHtml(boolean val) { this.stickyLinesHtml = val; }
        public boolean isStickyLinesMarkdown() { return stickyLinesMarkdown; }
        public void setStickyLinesMarkdown(boolean val) { this.stickyLinesMarkdown = val; }
        public boolean isStickyLinesXhtml() { return stickyLinesXhtml; }
        public void setStickyLinesXhtml(boolean val) { this.stickyLinesXhtml = val; }
        public boolean isStickyLinesJson() { return stickyLinesJson; }
        public void setStickyLinesJson(boolean val) { this.stickyLinesJson = val; }
        public boolean isStickyLinesSql() { return stickyLinesSql; }
        public void setStickyLinesSql(boolean val) { this.stickyLinesSql = val; }
        public boolean isStickyLinesXml() { return stickyLinesXml; }
        public void setStickyLinesXml(boolean val) { this.stickyLinesXml = val; }

        // Code Editing
        public boolean isCodeEditingHighlightMatchedBrace() { return codeEditingHighlightMatchedBrace; }
        public void setCodeEditingHighlightMatchedBrace(boolean val) { this.codeEditingHighlightMatchedBrace = val; }
        public boolean isCodeEditingHighlightCurrentScope() { return codeEditingHighlightCurrentScope; }
        public void setCodeEditingHighlightCurrentScope(boolean val) { this.codeEditingHighlightCurrentScope = val; }
        public boolean isCodeEditingHighlightUsages() { return codeEditingHighlightUsages; }
        public void setCodeEditingHighlightUsages(boolean val) { this.codeEditingHighlightUsages = val; }
        public boolean isCodeEditingShowDocOnHover() { return codeEditingShowDocOnHover; }
        public void setCodeEditingShowDocOnHover(boolean val) { this.codeEditingShowDocOnHover = val; }
        public String getCodeEditingRefactoringOption() { return codeEditingRefactoringOption; }
        public void setCodeEditingRefactoringOption(String val) { this.codeEditingRefactoringOption = val != null ? val : "In the editor"; }
        public boolean isCodeEditingPreselectCurrentSymbol() { return codeEditingPreselectCurrentSymbol; }
        public void setCodeEditingPreselectCurrentSymbol(boolean val) { this.codeEditingPreselectCurrentSymbol = val; }
        public boolean isCodeEditingShowInlineDialogForLocalVars() { return codeEditingShowInlineDialogForLocalVars; }
        public void setCodeEditingShowInlineDialogForLocalVars(boolean val) { this.codeEditingShowInlineDialogForLocalVars = val; }
        public int getCodeEditingErrorStripeMarkMinHeight() { return codeEditingErrorStripeMarkMinHeight; }
        public void setCodeEditingErrorStripeMarkMinHeight(int val) { this.codeEditingErrorStripeMarkMinHeight = Math.max(1, val); }
        public int getCodeEditingAutoreparseDelayMs() { return codeEditingAutoreparseDelayMs; }
        public void setCodeEditingAutoreparseDelayMs(int val) { this.codeEditingAutoreparseDelayMs = Math.max(0, val); }
        public String getCodeEditingNextErrorAction() { return codeEditingNextErrorAction; }
        public void setCodeEditingNextErrorAction(String val) { this.codeEditingNextErrorAction = val != null ? val : "The problems with the highest priority"; }
        public int getCodeEditingTooltipDelayMs() { return codeEditingTooltipDelayMs; }
        public void setCodeEditingTooltipDelayMs(int val) { this.codeEditingTooltipDelayMs = Math.max(0, val); }
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

    public static class MenuItemConfig {
        public enum Type { ACTION, GROUP, SEPARATOR }

        private Type type = Type.ACTION;
        private String id = "";
        private String text = "";
        private String iconName;
        private String iconPath;
        private boolean popup = true;
        private List<MenuItemConfig> children = new ArrayList<>();

        public MenuItemConfig() {}

        public MenuItemConfig(Type type, String id, String text) {
            this(type, id, text, true);
        }

        public MenuItemConfig(Type type, String id, String text, boolean popup) {
            this.type = type != null ? type : Type.ACTION;
            this.id = id != null ? id : "";
            this.text = text != null ? text : "";
            this.popup = popup;
        }

        public MenuItemConfig(Type type, String id, String text, String iconName, String iconPath, List<MenuItemConfig> children) {
            this(type, id, text, iconName, iconPath, true, children);
        }

        public MenuItemConfig(Type type, String id, String text, String iconName, String iconPath, boolean popup, List<MenuItemConfig> children) {
            this.type = type != null ? type : Type.ACTION;
            this.id = id != null ? id : "";
            this.text = text != null ? text : "";
            this.iconName = iconName;
            this.iconPath = iconPath;
            this.popup = popup;
            if (children != null) this.children = new ArrayList<>(children);
        }

        public static MenuItemConfig action(String id, String text) {
            return new MenuItemConfig(Type.ACTION, id, text);
        }

        public static MenuItemConfig action(String id, String text, String iconName) {
            MenuItemConfig item = new MenuItemConfig(Type.ACTION, id, text);
            item.setIconName(iconName);
            return item;
        }

        public static MenuItemConfig group(String id, String text, List<MenuItemConfig> children) {
            return group(id, text, true, children);
        }

        public static MenuItemConfig group(String id, String text, boolean popup, List<MenuItemConfig> children) {
            MenuItemConfig g = new MenuItemConfig(Type.GROUP, id, text, popup);
            if (children != null) g.children = new ArrayList<>(children);
            return g;
        }

        public static MenuItemConfig group(String id, String text, String iconName, List<MenuItemConfig> children) {
            return group(id, text, true, iconName, children);
        }

        public static MenuItemConfig group(String id, String text, boolean popup, String iconName, List<MenuItemConfig> children) {
            MenuItemConfig g = new MenuItemConfig(Type.GROUP, id, text, popup);
            g.setIconName(iconName);
            if (children != null) g.children = new ArrayList<>(children);
            return g;
        }

        public static MenuItemConfig separator() {
            return new MenuItemConfig(Type.SEPARATOR, "separator", "--------------------");
        }

        public MenuItemConfig copy() {
            MenuItemConfig copy = new MenuItemConfig(type, id, text, popup);
            copy.iconName = this.iconName;
            copy.iconPath = this.iconPath;
            if (this.children != null) {
                copy.children = new ArrayList<>();
                for (MenuItemConfig c : this.children) {
                    copy.children.add(c.copy());
                }
            }
            return copy;
        }

        public Type getType() { return type != null ? type : Type.ACTION; }
        public void setType(Type type) { this.type = type != null ? type : Type.ACTION; }

        public String getId() { return id != null ? id : ""; }
        public void setId(String id) { this.id = id != null ? id : ""; }

        public String getText() { return text != null ? text : ""; }
        public void setText(String text) { this.text = text != null ? text : ""; }

        public String getIconName() { return iconName; }
        public void setIconName(String iconName) { this.iconName = iconName; }

        public String getIconPath() { return iconPath; }
        public void setIconPath(String iconPath) { this.iconPath = iconPath; }

        public boolean isPopup() { return popup; }
        public void setPopup(boolean popup) { this.popup = popup; }

        public List<MenuItemConfig> getChildren() {
            if (children == null) children = new ArrayList<>();
            return children;
        }
        public void setChildren(List<MenuItemConfig> children) {
            this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
        }

        @Override
        public String toString() {
            return text != null && !text.isEmpty() ? text : (id != null ? id : "");
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

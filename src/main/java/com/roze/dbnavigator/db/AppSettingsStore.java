package com.roze.dbnavigator.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

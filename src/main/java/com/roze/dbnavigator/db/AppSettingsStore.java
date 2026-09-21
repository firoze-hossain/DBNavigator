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

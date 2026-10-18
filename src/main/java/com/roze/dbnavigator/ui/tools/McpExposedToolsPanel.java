package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.*;

/**
 * Settings panel for Tools > MCP Server > Exposed Tools.
 * Matches DataGrip screenshot media_1791726405199_475d8a3e.png.
 */
public class McpExposedToolsPanel {

    public static class ExposedToolEntry {
        private final String key;
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty description = new SimpleStringProperty();
        private final BooleanProperty enabled = new SimpleBooleanProperty(true);
        private final boolean isGroup;

        public ExposedToolEntry(String key, String name, String description, boolean isGroup) {
            this.key = key;
            this.name.set(name);
            this.description.set(description);
            this.isGroup = isGroup;
        }

        public String getKey() { return key; }
        public String getName() { return name.get(); }
        public String getDescription() { return description.get(); }
        public BooleanProperty enabledProperty() { return enabled; }
        public boolean isEnabled() { return enabled.get(); }
        public void setEnabled(boolean val) { enabled.set(val); }
        public boolean isGroup() { return isGroup; }
    }

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public McpExposedToolsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.McpServerConfig cfg = settings.getMcpServer();
        Set<String> selectedTools = new HashSet<>(cfg.getEnabledExposedTools());
        inputs.put("tools_mcp_exposed_tools", selectedTools);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        TreeItem<ExposedToolEntry> root = new TreeItem<>(new ExposedToolEntry("root", "Root", "", true));
        root.setExpanded(true);

        // Populate tree
        buildTree(root, selectedTools);

        TreeTableView<ExposedToolEntry> treeTable = new TreeTableView<>(root);
        treeTable.setShowRoot(false);
        treeTable.setStyle("-fx-background-color: -surface; -fx-border-color: -border-subtle; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(treeTable, Priority.ALWAYS);

        // Name Column
        TreeTableColumn<ExposedToolEntry, String> nameCol = new TreeTableColumn<>("Name");
        nameCol.setPrefWidth(240);
        nameCol.setCellValueFactory(param -> param.getValue().getValue().name);
        nameCol.setCellFactory(col -> new TreeTableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private final Label label = new Label();
            private final HBox graphicBox = new HBox(6, checkBox, label);

            {
                graphicBox.setAlignment(Pos.CENTER_LEFT);
                checkBox.setOnAction(e -> {
                    TreeItem<ExposedToolEntry> treeItem = getTreeTableRow().getTreeItem();
                    if (treeItem != null) {
                        ExposedToolEntry entry = treeItem.getValue();
                        boolean selected = checkBox.isSelected();
                        entry.setEnabled(selected);

                        if (entry.isGroup()) {
                            // Propagate to all child tools
                            for (TreeItem<ExposedToolEntry> child : treeItem.getChildren()) {
                                child.getValue().setEnabled(selected);
                                if (selected) {
                                    selectedTools.add(child.getValue().getKey());
                                } else {
                                    selectedTools.remove(child.getValue().getKey());
                                }
                            }
                        } else {
                            // Individual tool
                            if (selected) {
                                selectedTools.add(entry.getKey());
                            } else {
                                selectedTools.remove(entry.getKey());
                            }
                            // Update parent group state
                            TreeItem<ExposedToolEntry> parent = treeItem.getParent();
                            if (parent != null && parent != root) {
                                boolean anyChecked = parent.getChildren().stream().anyMatch(c -> c.getValue().isEnabled());
                                parent.getValue().setEnabled(anyChecked);
                            }
                        }
                        treeTable.refresh();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    TreeItem<ExposedToolEntry> treeItem = getTreeTableRow().getTreeItem();
                    if (treeItem != null) {
                        ExposedToolEntry entry = treeItem.getValue();
                        label.setText(entry.getName());
                        label.setStyle(entry.isGroup()
                                ? "-fx-font-weight: bold; -fx-text-fill: -text;"
                                : "-fx-text-fill: -text;");
                        checkBox.setSelected(entry.isEnabled());
                        setGraphic(graphicBox);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            }
        });

        // Description Column
        TreeTableColumn<ExposedToolEntry, String> descCol = new TreeTableColumn<>("Description");
        descCol.setPrefWidth(550);
        descCol.setCellValueFactory(param -> param.getValue().getValue().description);
        descCol.setCellFactory(col -> new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");
                }
            }
        });

        treeTable.getColumns().addAll(nameCol, descCol);

        content.getChildren().add(treeTable);
        return content;
    }

    private void buildTree(TreeItem<ExposedToolEntry> root, Set<String> selectedTools) {
        // 1. AnalysisToolset
        addToolset(root, "AnalysisToolset", List.of(
                tool("AnalysisToolset.build_project", "build_project", "Triggers building of the project or specified files, waits for completion, and returns build errors.Use this tool when you need to compile code, check for build errors, or verify changes before running tests."),
                tool("AnalysisToolset.get_file_problems", "get_file_problems", "Analyzes the specified file for errors and warnings using DBNavigator inspections.Use this tool to identify code issues, syntax errors, and potential improvements in a specific file."),
                tool("AnalysisToolset.get_project_dependancies", "get_project_dependancies", "Get a list of all dependencies defined in the project.Returns structured information about project library dependencies including names, versions, and scopes."),
                tool("AnalysisToolset.get_project_modules", "get_project_modules", "Get a list of all modules in the project with their types.Returns structured information about each module including name, type, and source roots.")
        ), selectedTools);

        // 2. CodeInsightToolset
        addToolset(root, "CodeInsightToolset", List.of(
                tool("CodeInsightToolset.get_symbol_info", "get_symbol_info", "Retrieves information about the symbol at the specified position in the specified file. Provides quick documentation, declaration, and docstring.")
        ), selectedTools);

        // 3. ExecutionToolset
        addToolset(root, "ExecutionToolset", List.of(
                tool("ExecutionToolset.execute_run_configuration", "execute_run_configuration", "Run either an existing run configuration by name or a temporary run configuration created from a code location.Wait for execution to finish and return the exit code, stdout, and stderr."),
                tool("ExecutionToolset.get_run_configurations", "get_run_configurations", "Returns either project run configurations or executable code locations, depending on the input.Without parameters: lists existing run configurations in the project.")
        ), selectedTools);

        // 4. FileToolset
        addToolset(root, "FileToolset", List.of(
                tool("FileToolset.create_new_file", "create_new_file", "Creates a new file at the specified path within the project directory and optionally populates it with text content.Use this tool when you need to add a new file to the project."),
                tool("FileToolset.find_files_by_glob", "find_files_by_glob", "Searches for all files in the project whose relative paths match the specified glob pattern.The search is performed recursively from the project root."),
                tool("FileToolset.find_files_by_name_keyword", "find_files_by_name_keyword", "Searches for all files in the project whose names contain the specified keyword (case-insensitive).Use this tool to locate files when you only know part of the filename."),
                tool("FileToolset.get_all_open_file_paths", "get_all_open_file_paths", "Returns active editor's and other open editors' file paths relative to the project root.Use this tool to explore what the user is currently looking at."),
                tool("FileToolset.list_directory_tree", "list_directory_tree", "Provides a tree representation of the specified directory in the pseudo graphic format like 'tree' utility does.Use this tool to explore directory structure."),
                tool("FileToolset.open_file_in_editor", "open_file_in_editor", "Opens the specified file in the DBNavigator editor. Requires a filePath parameter containing the path to the file to open.")
        ), selectedTools);

        // 5. FormattingToolset
        addToolset(root, "FormattingToolset", List.of(
                tool("FormattingToolset.reformat_file", "reformat_file", "Reformats a specified file in DBNavigator. Use this tool to apply code formatting rules to a file identified by its path.")
        ), selectedTools);

        // 6. ReadToolset
        addToolset(root, "ReadToolset", List.of(
                tool("ReadToolset.read_file", "read_file", "Reads a file in the project directory or from any project dependency or other project source root.Can read either the entire file or a specific line range.")
        ), selectedTools);

        // 7. RefactoringToolset
        addToolset(root, "RefactoringToolset", List.of(
                tool("RefactoringToolset.rename_refactoring", "rename_refactoring", "Renames a symbol (variable, function, class, etc.) in the specified file.Use this tool to perform rename refactoring across the codebase.")
        ), selectedTools);

        // 8. SearchToolset
        addToolset(root, "SearchToolset", List.of(
                tool("SearchToolset.search_file", "search_file", "Searches for files by glob pattern within the project.Use this tool when you need to match file paths using wildcard expressions."),
                tool("SearchToolset.search_regex", "search_regex", "Searches for regex matches within project files.Use this tool when you need regex search with snippet results."),
                tool("SearchToolset.search_symbol", "search_symbol", "Searches for symbols (classes, methods, fields).Use this tool for semantic lookup by identifier fragments."),
                tool("SearchToolset.search_text", "search_text", "Searches for a text substring within project files.Use this tool for fast text search with snippet results.")
        ), selectedTools);

        // 9. TerminalToolset
        addToolset(root, "TerminalToolset", List.of(
                tool("TerminalToolset.execute_terminal_command", "execute_terminal_command", "Executes a specified shell command in the IDE's integrated terminal.Use this tool to run terminal commands, CLI tools, scripts, and shell operations.")
        ), selectedTools);

        // 10. TextToolset
        addToolset(root, "TextToolset", List.of(
                tool("TextToolset.get_file_text_by_path", "get_file_text_by_path", "Retrieves the text content of a file using its path relative to project root.Use this tool to read file contents directly."),
                tool("TextToolset.replace_text_in_file", "replace_text_in_file", "Replaces text in a file with flexible options for find and replace operations.Use this tool to make targeted edits to file contents."),
                tool("TextToolset.search_in_files_by_regex", "search_in_files_by_regex", "Searches with a regex pattern within all files in the project using the integrated search engine. Prefer this tool when available."),
                tool("TextToolset.search_in_files_by_text", "search_in_files_by_text", "Searches for a text substring within all files in the project using the integrated search engine. Prefer this tool when available.")
        ), selectedTools);

        // 11. VcsToolset
        addToolset(root, "VcsToolset", List.of(
                tool("VcsToolset.get_repositories", "get_repositories", "Retrieves the list of VCS roots in the project.This is useful to detect all repositories in a multi-repository workspace.")
        ), selectedTools);
    }

    private ExposedToolEntry tool(String key, String name, String desc) {
        return new ExposedToolEntry(key, name, desc, false);
    }

    private void addToolset(TreeItem<ExposedToolEntry> root, String groupName,
                            List<ExposedToolEntry> tools, Set<String> selectedTools) {
        boolean anyEnabled = false;
        for (ExposedToolEntry t : tools) {
            boolean en = selectedTools.contains(t.getKey());
            t.setEnabled(en);
            if (en) anyEnabled = true;
        }

        ExposedToolEntry groupEntry = new ExposedToolEntry(groupName, groupName, "", true);
        groupEntry.setEnabled(anyEnabled);

        TreeItem<ExposedToolEntry> groupItem = new TreeItem<>(groupEntry);
        groupItem.setExpanded(true);

        for (ExposedToolEntry t : tools) {
            groupItem.getChildren().add(new TreeItem<>(t));
        }

        root.getChildren().add(groupItem);
    }
}

package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

/**
 * Modal dialog for creating and editing an External Tool.
 * Matches DataGrip screenshot media_1791726305639_5893d7ce.png.
 */
public class CreateToolDialog {

    private final AppSettingsStore.ExternalToolItem item;
    private final boolean isEdit;
    private boolean saved = false;

    public CreateToolDialog(AppSettingsStore.ExternalToolItem item, boolean isEdit) {
        this.item = item != null ? item : new AppSettingsStore.ExternalToolItem();
        this.isEdit = isEdit;
    }

    public Optional<AppSettingsStore.ExternalToolItem> showAndWait(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(isEdit ? "Edit Tool" : "Create Tool");

        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 18, 16, 18));
        root.setStyle("-fx-background-color: -surface;");

        // Row 1: Name and Group
        Label nameLabel = new Label("Name:");
        nameLabel.setPrefWidth(100);
        nameLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField nameField = new TextField(item.getName());
        nameField.setPrefWidth(220);

        Label groupLabel = new Label("Group:");
        groupLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> groupCombo = new ComboBox<>();
        groupCombo.setEditable(true);
        groupCombo.getItems().addAll("External Tools", "Build", "Format", "Linters", "Deployment");
        groupCombo.setValue(item.getGroup() != null && !item.getGroup().isBlank() ? item.getGroup() : "External Tools");
        groupCombo.setPrefWidth(180);

        HBox row1 = new HBox(8, nameLabel, nameField, groupLabel, groupCombo);
        row1.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Description
        Label descLabel = new Label("Description:");
        descLabel.setPrefWidth(100);
        descLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField descField = new TextField(item.getDescription());
        HBox.setHgrow(descField, Priority.ALWAYS);

        HBox row2 = new HBox(8, descLabel, descField);
        row2.setAlignment(Pos.CENTER_LEFT);

        // Section: Tool Settings
        Label toolSettingsLabel = new Label("Tool Settings");
        toolSettingsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");
        Separator sep1 = new Separator();
        HBox toolSettingsHeader = new HBox(10, toolSettingsLabel, sep1);
        toolSettingsHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sep1, Priority.ALWAYS);

        // Program
        Label progLabel = new Label("Program:");
        progLabel.setPrefWidth(100);
        progLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField progField = new TextField(item.getProgram());
        HBox.setHgrow(progField, Priority.ALWAYS);

        Button progMacroBtn = new Button("+");
        progMacroBtn.setTooltip(new Tooltip("Insert Macro"));
        progMacroBtn.setOnAction(e -> showMacroMenu(progMacroBtn, progField));

        Button progBrowseBtn = new Button("📁");
        progBrowseBtn.setTooltip(new Tooltip("Browse Executable"));
        progBrowseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Executable");
            File file = fc.showOpenDialog(dialog);
            if (file != null) progField.setText(file.getAbsolutePath());
        });

        HBox progRow = new HBox(6, progLabel, progField, progMacroBtn, progBrowseBtn);
        progRow.setAlignment(Pos.CENTER_LEFT);

        // Arguments
        Label argsLabel = new Label("Arguments:");
        argsLabel.setPrefWidth(100);
        argsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField argsField = new TextField(item.getArguments());
        HBox.setHgrow(argsField, Priority.ALWAYS);

        Button argsMacroBtn = new Button("+");
        argsMacroBtn.setTooltip(new Tooltip("Insert Macro"));
        argsMacroBtn.setOnAction(e -> showMacroMenu(argsMacroBtn, argsField));

        Button argsExpandBtn = new Button("⤢");
        argsExpandBtn.setTooltip(new Tooltip("Expand Arguments"));
        argsExpandBtn.setOnAction(e -> showTextDialog("Edit Arguments", argsField));

        HBox argsRow = new HBox(6, argsLabel, argsField, argsMacroBtn, argsExpandBtn);
        argsRow.setAlignment(Pos.CENTER_LEFT);

        // Working directory
        Label workDirLabel = new Label("Working directory:");
        workDirLabel.setPrefWidth(100);
        workDirLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField workDirField = new TextField(item.getWorkingDirectory());
        HBox.setHgrow(workDirField, Priority.ALWAYS);

        Button workDirMacroBtn = new Button("+");
        workDirMacroBtn.setTooltip(new Tooltip("Insert Macro"));
        workDirMacroBtn.setOnAction(e -> showMacroMenu(workDirMacroBtn, workDirField));

        Button workDirBrowseBtn = new Button("📁");
        workDirBrowseBtn.setTooltip(new Tooltip("Browse Directory"));
        workDirBrowseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Working Directory");
            File dir = dc.showDialog(dialog);
            if (dir != null) workDirField.setText(dir.getAbsolutePath());
        });

        HBox workDirRow = new HBox(6, workDirLabel, workDirField, workDirMacroBtn, workDirBrowseBtn);
        workDirRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Advanced Options
        TitledPane advPane = new TitledPane();
        advPane.setText("Advanced Options");
        advPane.setExpanded(true);
        advPane.setStyle("-fx-text-fill: -text;");

        CheckBox syncCheck = new CheckBox("Synchronize files after execution");
        syncCheck.setSelected(item.isSynchronizeFiles());
        syncCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        CheckBox openConsoleCheck = new CheckBox("Open console for tool output");
        openConsoleCheck.setSelected(item.isOpenConsole());
        openConsoleCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        CheckBox stdoutCheck = new CheckBox("Make console active on message in stdout");
        stdoutCheck.setSelected(item.isConsoleActiveOnStdout());
        stdoutCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        stdoutCheck.setPadding(new Insets(0, 0, 0, 20));
        stdoutCheck.disableProperty().bind(openConsoleCheck.selectedProperty().not());

        CheckBox stderrCheck = new CheckBox("Make console active on message in stderr");
        stderrCheck.setSelected(item.isConsoleActiveOnStderr());
        stderrCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        stderrCheck.setPadding(new Insets(0, 0, 0, 20));
        stderrCheck.disableProperty().bind(openConsoleCheck.selectedProperty().not());

        Label outFilterLabel = new Label("Output filters:");
        outFilterLabel.setPrefWidth(90);
        outFilterLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField outFilterField = new TextField(item.getOutputFilters());
        HBox.setHgrow(outFilterField, Priority.ALWAYS);

        Button filterExpandBtn = new Button("⤢");
        filterExpandBtn.setOnAction(e -> showTextDialog("Edit Output Filters", outFilterField));

        HBox filterRow = new HBox(6, outFilterLabel, outFilterField, filterExpandBtn);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        Label filterHint = new Label("Each line is a regex, available macros: $FILE_PATH$, $LINE$ and $COLUMN$");
        filterHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        VBox advContent = new VBox(8, syncCheck, openConsoleCheck, stdoutCheck, stderrCheck, filterRow, filterHint);
        advContent.setPadding(new Insets(8, 4, 8, 4));
        advPane.setContent(advContent);

        // Buttons
        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setPrefWidth(80);
        okBtn.setOnAction(e -> {
            if (nameField.getText().trim().isEmpty()) {
                nameField.setStyle("-fx-border-color: red;");
                return;
            }
            item.setName(nameField.getText().trim());
            item.setGroup(groupCombo.getValue() != null ? groupCombo.getValue().trim() : "External Tools");
            item.setDescription(descField.getText().trim());
            item.setProgram(progField.getText().trim());
            item.setArguments(argsField.getText().trim());
            item.setWorkingDirectory(workDirField.getText().trim());
            item.setSynchronizeFiles(syncCheck.isSelected());
            item.setOpenConsole(openConsoleCheck.isSelected());
            item.setConsoleActiveOnStdout(stdoutCheck.isSelected());
            item.setConsoleActiveOnStderr(stderrCheck.isSelected());
            item.setOutputFilters(outFilterField.getText().trim());
            saved = true;
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setPrefWidth(80);
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(10, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(row1, row2, toolSettingsHeader, progRow, argsRow, workDirRow, advPane, btnBox);

        Scene scene = new Scene(root, 620, 520);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();

        return saved ? Optional.of(item) : Optional.empty();
    }

    private void showMacroMenu(Button anchor, TextField target) {
        ContextMenu menu = new ContextMenu();
        String[] macros = {
                "$FilePath$", "$FileName$", "$FileDir$", "$ProjectFileDir$",
                "$SelectedText$", "$Prompt$", "$LineNumber$", "$ColumnNumber$"
        };
        for (String m : macros) {
            MenuItem mi = new MenuItem(m);
            mi.setOnAction(e -> {
                int pos = target.getCaretPosition();
                target.insertText(pos, m);
            });
            menu.getItems().add(mi);
        }
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void showTextDialog(String title, TextField target) {
        Dialog<String> td = new Dialog<>();
        td.setTitle(title);
        TextArea area = new TextArea(target.getText());
        area.setPrefRowCount(8);
        area.setPrefColumnCount(40);
        td.getDialogPane().setContent(new VBox(10, area));
        td.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        td.setResultConverter(btn -> btn == ButtonType.OK ? area.getText() : null);
        td.showAndWait().ifPresent(target::setText);
    }
}

package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Settings panel for Tools > Debugger.
 * Matches DataGrip screenshot media_1791725709679_5e17e800.png.
 */
public class DebuggerSettingsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public DebuggerSettingsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.DebuggerConfig cfg = settings.getDebugger();

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Top checkboxes
        CheckBox showDebugWindow = new CheckBox("Show debug window on breakpoint");
        showDebugWindow.setSelected(cfg.isShowDebugWindowOnBreakpoint());
        showDebugWindow.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_debugger_showDebugWindow", showDebugWindow);

        CheckBox focusApp = new CheckBox("Focus application on breakpoint");
        focusApp.setSelected(cfg.isFocusApplicationOnBreakpoint());
        focusApp.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        focusApp.setPadding(new Insets(0, 0, 0, 20));
        focusApp.disableProperty().bind(showDebugWindow.selectedProperty().not());
        inputs.put("tools_debugger_focusApp", focusApp);

        CheckBox hideOnTermination = new CheckBox("Hide debug window on process termination");
        hideOnTermination.setSelected(cfg.isHideDebugWindowOnProcessTermination());
        hideOnTermination.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_debugger_hideOnTermination", hideOnTermination);

        CheckBox scrollToCenter = new CheckBox("Scroll execution point to center");
        scrollToCenter.setSelected(cfg.isScrollExecutionPointToCenter());
        scrollToCenter.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_debugger_scrollToCenter", scrollToCenter);

        CheckBox clickLineRunToCursor = new CheckBox("Click line number to perform run to cursor");
        clickLineRunToCursor.setSelected(cfg.isClickLineNumberToRunToCursor());
        clickLineRunToCursor.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_debugger_clickLineRunToCursor", clickLineRunToCursor);

        VBox checksBox = new VBox(10, showDebugWindow, focusApp, hideOnTermination, scrollToCenter, clickLineRunToCursor);

        // Remove breakpoint section
        Label removeBreakpointLabel = new Label("Remove breakpoint:");
        removeBreakpointLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 8 0 2 0;");

        ToggleGroup removeGroup = new ToggleGroup();
        inputs.put("tools_debugger_removeBreakpointGroup", removeGroup);

        RadioButton clickLeftMouse = new RadioButton("Click with left mouse button");
        clickLeftMouse.setToggleGroup(removeGroup);
        clickLeftMouse.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton dragOrMiddleClick = new RadioButton("Drag to the editor or click with middle mouse button");
        dragOrMiddleClick.setToggleGroup(removeGroup);
        dragOrMiddleClick.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if (cfg.getRemoveBreakpointMode().contains("Drag")) {
            dragOrMiddleClick.setSelected(true);
        } else {
            clickLeftMouse.setSelected(true);
        }

        CheckBox confirmRemoval = new CheckBox("Confirm removal of conditional or logging breakpoints");
        confirmRemoval.setSelected(cfg.isConfirmRemovalOfConditionalOrLoggingBreakpoints());
        confirmRemoval.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        confirmRemoval.setPadding(new Insets(4, 0, 0, 0));
        inputs.put("tools_debugger_confirmRemoval", confirmRemoval);

        VBox removeBox = new VBox(8, removeBreakpointLabel, clickLeftMouse, dragOrMiddleClick, confirmRemoval);

        content.getChildren().addAll(checksBox, removeBox);
        return content;
    }
}

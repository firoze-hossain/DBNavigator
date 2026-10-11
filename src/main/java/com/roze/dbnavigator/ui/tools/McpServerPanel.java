package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Settings panel for Tools > MCP Server.
 * Matches DataGrip screenshot media_1791726321056_aeaf96a5.png.
 */
public class McpServerPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;
    private final Consumer<String> navigateTo;

    public McpServerPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs, Consumer<String> navigateTo) {
        this.settings = settings;
        this.inputs = inputs;
        this.navigateTo = navigateTo;
    }

    public Node createPanel() {
        AppSettingsStore.McpServerConfig cfg = settings.getMcpServer();

        VBox content = new VBox(16);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Top CheckBox
        CheckBox enableMcp = new CheckBox("Enable MCP Server");
        enableMcp.setSelected(cfg.isEnableMcpServer());
        enableMcp.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_mcp_enable", enableMcp);

        // Description Paragraph
        Label desc1 = new Label("The MCP Server allows external AI clients to use functionality from the IDE, integrating the power of your IDE into your AI tools. ");
        desc1.setWrapText(true);
        desc1.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

        Hyperlink allMcpToolsLink = new Hyperlink("All MCP Tools ↗");
        allMcpToolsLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-padding: 0; -fx-border-width: 0;");
        allMcpToolsLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Tools / MCP Server / Exposed Tools");
            }
        });

        HBox descRow = new HBox(4, desc1, allMcpToolsLink);
        descRow.setAlignment(Pos.CENTER_LEFT);

        Label desc2 = new Label("When enabled, these detected clients can be auto-configured to use IDE features in one click:\n• Claude App\n• Claude Code\n• Codex");
        desc2.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px; -fx-line-spacing: 3;");

        VBox infoBox = new VBox(10, enableMcp, descRow, desc2);

        content.getChildren().add(infoBox);
        return content;
    }
}

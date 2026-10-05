package com.roze.dbnavigator.ui.plugin;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.plugin.Plugin;
import com.roze.dbnavigator.plugin.PluginManager;
import com.roze.dbnavigator.ui.Icons;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DataGrip-aligned Plugins and Marketplace Settings Panel.
 * Implements both Marketplace and Installed master-detail views,
 * search filtering, category groups, gear configuration menu,
 * and plugin lifecycle actions (Install, Enable/Disable, Uninstall, Disk Import).
 */
public class PluginsPanel extends BorderPane {

    private enum TabMode { MARKETPLACE, INSTALLED }

    private TabMode currentMode = TabMode.MARKETPLACE;
    private final PluginManager pluginManager = PluginManager.getInstance();

    private final Button marketplaceTabBtn = new Button("Marketplace");
    private final Button installedTabBtn = new Button("Installed");
    private final Button gearButton = new Button();

    private final TextField searchField = new TextField();
    private final MenuButton filterButton = new MenuButton();
    private String selectedCategory = "All";

    private final VBox listContainer = new VBox(6);
    private final ScrollPane listScrollPane = new ScrollPane(listContainer);
    private final VBox detailsPane = new VBox(12);
    private final ScrollPane detailsScrollPane = new ScrollPane(detailsPane);

    private final Label connectionStatusBanner = new Label();

    private List<Plugin> marketplacePlugins = new ArrayList<>();
    private List<Plugin> installedPlugins = new ArrayList<>();
    private Plugin selectedPlugin = null;

    public PluginsPanel() {
        getStyleClass().add("plugins-panel");
        setPadding(new Insets(6, 12, 12, 12));

        setTop(buildHeaderBar());

        // Split pane with Left list and Right details
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);

        VBox leftPane = buildLeftListPane();
        VBox rightPane = buildRightDetailsPane();

        leftPane.setMinWidth(300);
        leftPane.setPrefWidth(350);
        rightPane.setMinWidth(360);

        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.40);
        SplitPane.setResizableWithParent(leftPane, false);

        setCenter(splitPane);

        // Load initial data
        refreshData();
    }

    // -------------------------------------------------------------------------
    // Header Bar
    // -------------------------------------------------------------------------

    private Node buildHeaderBar() {
        Label title = new Label("Plugins");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;");

        // Segmented pill buttons: [Marketplace] [Installed]
        HBox segmentedTabs = new HBox(0, marketplaceTabBtn, installedTabBtn);
        segmentedTabs.setAlignment(Pos.CENTER_LEFT);
        segmentedTabs.setStyle("-fx-background-color: #1e1f22; -fx-background-radius: 6; -fx-padding: 2;");

        marketplaceTabBtn.getStyleClass().add("plugin-tab-pill");
        installedTabBtn.getStyleClass().add("plugin-tab-pill");

        updateTabButtonStyles();

        marketplaceTabBtn.setOnAction(e -> {
            currentMode = TabMode.MARKETPLACE;
            updateTabButtonStyles();
            renderLeftList();
        });

        installedTabBtn.setOnAction(e -> {
            currentMode = TabMode.INSTALLED;
            updateTabButtonStyles();
            renderLeftList();
        });

        // Gear icon button (Image 4 ContextMenu)
        gearButton.setGraphic(Icons.of(FontAwesomeSolid.COG, "#a9b7c6", 13));
        gearButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6;");
        gearButton.setTooltip(new Tooltip("Plugin Settings"));
        ContextMenu gearMenu = buildGearContextMenu();
        gearButton.setOnAction(e -> gearMenu.show(gearButton, Side.BOTTOM, 0, 4));

        Region leftSpacer = new Region();
        leftSpacer.setPrefWidth(16);

        Region mainSpacer = new Region();
        HBox.setHgrow(mainSpacer, Priority.ALWAYS);

        // Top right navigation controls
        Button backBtn = new Button();
        backBtn.setGraphic(Icons.of(FontAwesomeSolid.ARROW_LEFT, "#868a91", 11));
        backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        Button forwardBtn = new Button();
        forwardBtn.setGraphic(Icons.of(FontAwesomeSolid.ARROW_RIGHT, "#868a91", 11));
        forwardBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        Button pinBtn = new Button();
        pinBtn.setGraphic(Icons.of(FontAwesomeSolid.THUMBTACK, "#868a91", 11));
        pinBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        HBox topRow = new HBox(8, title, leftSpacer, segmentedTabs, gearButton, mainSpacer, backBtn, forwardBtn, pinBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(0, 0, 10, 0));

        // Connection status notification banner
        connectionStatusBanner.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-dim; -fx-padding: 0 0 6 2;");
        connectionStatusBanner.setVisible(false);
        connectionStatusBanner.setManaged(false);

        VBox headerContainer = new VBox(4, topRow, connectionStatusBanner);
        return headerContainer;
    }

    private void updateTabButtonStyles() {
        String activeStyle = "-fx-background-color: #2e436e; -fx-background-radius: 4; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 14; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-background-radius: 4; -fx-text-fill: -text-dim; -fx-font-size: 12px; -fx-padding: 4 14; -fx-cursor: hand;";

        if (currentMode == TabMode.MARKETPLACE) {
            marketplaceTabBtn.setStyle(activeStyle);
            installedTabBtn.setStyle(inactiveStyle);
        } else {
            marketplaceTabBtn.setStyle(inactiveStyle);
            installedTabBtn.setStyle(activeStyle);
        }
    }

    private ContextMenu buildGearContextMenu() {
        ContextMenu menu = new ContextMenu();

        CheckMenuItem autoUpdateItem = new CheckMenuItem("Update Plugins Automatically");
        autoUpdateItem.setSelected(AppSettingsStore.load().isAutoUpdateEnabled());
        autoUpdateItem.setOnAction(e -> {
            var s = AppSettingsStore.load();
            s.setAutoUpdateEnabled(autoUpdateItem.isSelected());
            AppSettingsStore.save(s);
        });

        MenuItem reposItem = new MenuItem("Manage Plugin Repositories...");
        reposItem.setOnAction(e -> showManageRepositoriesDialog());

        MenuItem proxyItem = new MenuItem("HTTP Proxy Settings...");
        proxyItem.setOnAction(e -> showProxyDialog());

        MenuItem certsItem = new MenuItem("Manage Plugin Certificates...");
        certsItem.setOnAction(e -> showCertificatesDialog());

        MenuItem installDiskItem = new MenuItem("Install Plugin from Disk...");
        installDiskItem.setGraphic(Icons.of(FontAwesomeSolid.PLUG, "#a9b7c6", 11));
        installDiskItem.setOnAction(e -> installPluginFromDiskAction());

        MenuItem disableAllDownloaded = new MenuItem("Disable All Downloaded Plugins");
        disableAllDownloaded.setOnAction(e -> {
            pluginManager.disableAllDownloaded();
            refreshData();
        });

        MenuItem enableAllDownloaded = new MenuItem("Enable All Downloaded Plugins");
        enableAllDownloaded.setOnAction(e -> {
            pluginManager.enableAllDownloaded();
            refreshData();
        });

        menu.getItems().addAll(
                autoUpdateItem,
                reposItem,
                proxyItem,
                certsItem,
                installDiskItem,
                new SeparatorMenuItem(),
                disableAllDownloaded,
                enableAllDownloaded
        );
        return menu;
    }

    // -------------------------------------------------------------------------
    // Left List Pane
    // -------------------------------------------------------------------------

    private VBox buildLeftListPane() {
        VBox leftPane = new VBox(8);
        leftPane.setPadding(new Insets(4, 6, 4, 0));

        // Search Bar with Funnel Filter Icon
        searchField.setPromptText("Type / to see options");
        searchField.setStyle("-fx-background-color: #1e1f22; -fx-background-radius: 4; -fx-text-fill: -text; -fx-padding: 6 8;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderLeftList());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        filterButton.setGraphic(Icons.of(FontAwesomeSolid.FILTER, "#868a91", 11));
        filterButton.setStyle("-fx-background-color: #1e1f22; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 4 8;");
        rebuildFilterMenu();

        HBox searchRow = new HBox(6, searchField, filterButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        listScrollPane.setFitToWidth(true);
        listScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(listScrollPane, Priority.ALWAYS);

        leftPane.getChildren().addAll(searchRow, listScrollPane);
        VBox.setVgrow(leftPane, Priority.ALWAYS);
        return leftPane;
    }

    private void rebuildFilterMenu() {
        filterButton.getItems().clear();
        String[] cats = {"All", "AI-Powered", "Database", "Editor", "HTML and XML", "IDE Localization", "Local AI/ML Tools", "Productivity", "Code Tools", "Security", "Version Control"};
        for (String c : cats) {
            RadioMenuItem item = new RadioMenuItem(c);
            item.setSelected(selectedCategory.equalsIgnoreCase(c));
            item.setOnAction(e -> {
                selectedCategory = c;
                renderLeftList();
            });
            filterButton.getItems().add(item);
        }
    }

    public void refreshData() {
        // Fetch marketplace asynchronously from RozeHub
        String q = searchField.getText();
        pluginManager.fetchMarketplacePlugins(q, selectedCategory).thenAccept(res -> {
            Platform.runLater(() -> {
                this.marketplacePlugins = res.plugins;
                this.installedPlugins = pluginManager.getInstalledPlugins();

                if (res.statusMessage != null && !res.statusMessage.isBlank()) {
                    connectionStatusBanner.setText(res.statusMessage);
                    connectionStatusBanner.setVisible(true);
                    connectionStatusBanner.setManaged(true);
                }

                renderLeftList();
            });
        });
    }

    private void renderLeftList() {
        listContainer.getChildren().clear();
        String q = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

        if (currentMode == TabMode.MARKETPLACE) {
            renderMarketplaceList(q);
        } else {
            renderInstalledList(q);
        }
    }

    private void renderMarketplaceList(String query) {
        Label sectionHeader = new Label("Staff Picks");
        sectionHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -text-dim; -fx-padding: 4 2;");
        listContainer.getChildren().add(sectionHeader);

        List<Plugin> filtered = marketplacePlugins.stream().filter(p -> {
            boolean matchQ = query.isBlank()
                    || p.getName().toLowerCase().contains(query)
                    || p.getSummary().toLowerCase().contains(query)
                    || p.getVendor().toLowerCase().contains(query);
            boolean matchCat = selectedCategory.equalsIgnoreCase("All")
                    || p.getCategory().equalsIgnoreCase(selectedCategory);
            return matchQ && matchCat;
        }).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label empty = new Label("No plugins found matching \"" + query + "\"");
            empty.setStyle("-fx-text-fill: -text-dim; -fx-padding: 12 4; -fx-font-size: 12px;");
            listContainer.getChildren().add(empty);
            return;
        }

        for (Plugin p : filtered) {
            Node card = buildMarketplaceCard(p);
            listContainer.getChildren().add(card);
        }

        // Auto-select first if none selected
        if (selectedPlugin == null && !filtered.isEmpty()) {
            selectPlugin(filtered.get(0));
        }
    }

    private Node buildMarketplaceCard(Plugin p) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 10, 8, 10));

        boolean isSelected = selectedPlugin != null && selectedPlugin.getId().equals(p.getId());
        String defaultBg = isSelected ? "#214283" : "#2b2d30";
        card.setStyle("-fx-background-color: " + defaultBg + "; -fx-background-radius: 6; -fx-cursor: hand;");

        // Brand icon
        Node iconGraphic = createBrandIcon(p, 32);

        // Name and Stats
        Label nameLbl = new Label(p.getName());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: white;");

        Label statsLbl = new Label("↓ " + p.formattedDownloads() + "   ★ " + p.formattedRating() + "   " + p.getVendor());
        statsLbl.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px;");

        VBox textCol = new VBox(2, nameLbl, statsLbl);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        // Action button (Install / Installed)
        Button actionBtn = new Button();
        actionBtn.setPrefWidth(68);
        actionBtn.setPrefHeight(26);

        if (p.isInstalled()) {
            actionBtn.setText("Installed");
            actionBtn.setDisable(true);
            actionBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868a91; -fx-border-color: #43454a; -fx-border-radius: 4; -fx-font-size: 11px;");
        } else {
            actionBtn.setText("Install");
            actionBtn.setStyle("-fx-background-color: #2e7d32; -fx-background-radius: 4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
            actionBtn.setOnAction(e -> {
                e.consume();
                actionBtn.setDisable(true);
                actionBtn.setText("...");
                pluginManager.installPlugin(p, pr -> {}).thenAccept(installed -> {
                    Platform.runLater(() -> {
                        refreshData();
                    });
                });
            });
        }

        card.getChildren().addAll(iconGraphic, textCol, actionBtn);

        card.setOnMouseClicked(e -> selectPlugin(p));
        return card;
    }

    private void renderInstalledList(String query) {
        installedPlugins = pluginManager.getInstalledPlugins();

        // Group by category
        Map<String, List<Plugin>> groups = new LinkedHashMap<>();

        // Group 1: User-installed
        List<Plugin> userInstalled = installedPlugins.stream()
                .filter(p -> !p.isBundled())
                .collect(Collectors.toList());
        if (!userInstalled.isEmpty()) {
            long enabledCount = userInstalled.stream().filter(Plugin::isEnabled).count();
            groups.put("User-installed (" + enabledCount + " of " + userInstalled.size() + " enabled)", userInstalled);
        }

        // Group 2+: Categorized bundled plugins
        for (Plugin p : installedPlugins) {
            if (p.isBundled()) {
                groups.computeIfAbsent(p.getCategory(), k -> new ArrayList<>()).add(p);
            }
        }

        for (Map.Entry<String, List<Plugin>> entry : groups.entrySet()) {
            String category = entry.getKey();
            List<Plugin> pluginsInCat = entry.getValue().stream().filter(p ->
                    query.isBlank() || p.getName().toLowerCase().contains(query) || p.getSummary().toLowerCase().contains(query)
            ).collect(Collectors.toList());

            if (pluginsInCat.isEmpty()) continue;

            // Section Header with "Disable all" link (Images 3 & 4)
            Label catLabel = new Label(category);
            catLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text-dim;");

            Hyperlink disableAllLink = new Hyperlink("Disable all");
            disableAllLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 11px; -fx-padding: 0;");
            disableAllLink.setOnAction(e -> {
                pluginManager.disableAllInCategory(category);
                refreshData();
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox headerRow = new HBox(6, catLabel, spacer, disableAllLink);
            headerRow.setAlignment(Pos.CENTER_LEFT);
            headerRow.setPadding(new Insets(8, 2, 4, 2));
            listContainer.getChildren().add(headerRow);

            // Plugin item rows
            for (Plugin p : pluginsInCat) {
                Node row = buildInstalledRow(p);
                listContainer.getChildren().add(row);
            }
        }

        if (selectedPlugin == null && !installedPlugins.isEmpty()) {
            selectPlugin(installedPlugins.get(0));
        }
    }

    private Node buildInstalledRow(Plugin p) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));

        boolean isSelected = selectedPlugin != null && selectedPlugin.getId().equals(p.getId());
        String defaultBg = isSelected ? "#214283" : "#2b2d30";
        row.setStyle("-fx-background-color: " + defaultBg + "; -fx-background-radius: 6; -fx-cursor: hand;");

        Node iconGraphic = createBrandIcon(p, 24);

        Label nameLbl = new Label(p.getName());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -text;");

        Label verLbl = new Label(p.getInstalledVersion());
        verLbl.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px;");

        VBox textCol = new VBox(1, nameLbl, verLbl);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        CheckBox enabledCheck = new CheckBox();
        enabledCheck.setSelected(p.isEnabled());
        if (p.isBundled() && "Database".equalsIgnoreCase(p.getCategory())) {
            // Core database tools cannot be unchecked
            enabledCheck.setDisable(true);
        }
        enabledCheck.setOnAction(e -> {
            e.consume();
            pluginManager.setPluginEnabled(p.getId(), enabledCheck.isSelected());
            p.setEnabled(enabledCheck.isSelected());
            if (selectedPlugin != null && selectedPlugin.getId().equals(p.getId())) {
                renderRightDetails();
            }
        });

        row.getChildren().addAll(iconGraphic, textCol, enabledCheck);
        row.setOnMouseClicked(e -> selectPlugin(p));
        return row;
    }

    private void selectPlugin(Plugin p) {
        this.selectedPlugin = p;
        renderLeftList();
        renderRightDetails();
    }

    // -------------------------------------------------------------------------
    // Right Details Pane
    // -------------------------------------------------------------------------

    private VBox buildRightDetailsPane() {
        detailsScrollPane.setFitToWidth(true);
        detailsScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(detailsScrollPane, Priority.ALWAYS);

        VBox container = new VBox(detailsScrollPane);
        container.setPadding(new Insets(4, 0, 4, 10));
        VBox.setVgrow(container, Priority.ALWAYS);
        return container;
    }

    private void renderRightDetails() {
        detailsPane.getChildren().clear();
        if (selectedPlugin == null) {
            Label placeholder = new Label("Select a plugin to view details");
            placeholder.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px; -fx-padding: 30;");
            detailsPane.getChildren().add(placeholder);
            return;
        }

        Plugin p = selectedPlugin;

        // 1. Category Badges (Image 2)
        HBox badgeRow = new HBox(6);
        List<String> tags = p.getTags();
        if (tags.isEmpty()) {
            tags = List.of(p.getCategory());
        }
        for (String tag : tags) {
            Label badge = new Label(tag);
            badge.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 11px;");
            badgeRow.getChildren().add(badge);
        }

        // 2. Title & Vendor / Website Link
        Label titleLbl = new Label(p.getName());
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label vendorLbl = new Label(p.getVendor());
        vendorLbl.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px;");

        Hyperlink websiteLink = new Hyperlink("Plugin homepage ↗");
        websiteLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-padding: 0;");
        websiteLink.setOnAction(e -> {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    String url = p.getWebsite().isBlank() ? "https://plugins.jetbrains.com" : p.getWebsite();
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                }
            } catch (Exception ignored) {}
        });

        HBox authorRow = new HBox(8, vendorLbl, websiteLink);
        authorRow.setAlignment(Pos.CENTER_LEFT);

        // 3. Action Buttons Row (Install, Disable ▾, Uninstall)
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Label verLabel = new Label(p.getInstalledVersion() != null ? p.getInstalledVersion() : p.getVersion());
        verLabel.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px;");

        if (currentMode == TabMode.MARKETPLACE && !p.isInstalled()) {
            Button installBtn = new Button("Install");
            installBtn.setStyle("-fx-background-color: #2e7d32; -fx-background-radius: 4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 18; -fx-cursor: hand;");
            installBtn.setOnAction(e -> {
                installBtn.setDisable(true);
                installBtn.setText("Installing...");
                pluginManager.installPlugin(p, pr -> {}).thenAccept(installed -> {
                    Platform.runLater(this::refreshData);
                });
            });
            actionRow.getChildren().addAll(installBtn, verLabel);
        } else {
            // Installed actions: Disable ▾ and Uninstall (Image 5)
            if (p.isBundled()) {
                Button enableBtn = new Button("Enable");
                enableBtn.setDisable(true);
                enableBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #868a91; -fx-background-radius: 4; -fx-padding: 6 16;");
                actionRow.getChildren().addAll(enableBtn, verLabel);
            } else {
                SplitMenuButton disableMenuBtn = new SplitMenuButton();
                disableMenuBtn.setText(p.isEnabled() ? "Disable" : "Enable");
                disableMenuBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-background-radius: 4; -fx-cursor: hand;");
                disableMenuBtn.setOnAction(e -> {
                    boolean next = !p.isEnabled();
                    pluginManager.setPluginEnabled(p.getId(), next);
                    p.setEnabled(next);
                    refreshData();
                });

                MenuItem disableProject = new MenuItem(p.isEnabled() ? "Disable for this project" : "Enable for this project");
                disableProject.setOnAction(e -> {
                    boolean next = !p.isEnabled();
                    pluginManager.setPluginEnabled(p.getId(), next);
                    p.setEnabled(next);
                    refreshData();
                });
                disableMenuBtn.getItems().add(disableProject);

                Button uninstallBtn = new Button("Uninstall");
                uninstallBtn.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #43454a; -fx-border-radius: 4; -fx-text-fill: -text; -fx-padding: 6 14; -fx-cursor: hand;");
                uninstallBtn.setOnAction(e -> {
                    pluginManager.uninstallPlugin(p);
                    refreshData();
                });

                actionRow.getChildren().addAll(disableMenuBtn, verLabel, uninstallBtn);
            }
        }

        // 4. Sub-Navigation Tabs: [Overview] [What's New] [Reviews] [Additional Info]
        TabPane subTabs = new TabPane();
        subTabs.setStyle("-fx-tab-min-width: 70px;");
        subTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab overviewTab = new Tab("Overview", buildOverviewContent(p));
        Tab whatsNewTab = new Tab("What's New", buildWhatsNewContent(p));
        Tab reviewsTab = new Tab("Reviews", buildReviewsContent(p));
        Tab additionalTab = new Tab("Additional Info", buildAdditionalInfoContent(p));

        subTabs.getTabs().addAll(overviewTab, whatsNewTab, reviewsTab, additionalTab);
        VBox.setVgrow(subTabs, Priority.ALWAYS);

        detailsPane.getChildren().addAll(badgeRow, titleLbl, authorRow, actionRow, subTabs);
    }

    private Node buildOverviewContent(Plugin p) {
        VBox overview = new VBox(12);
        overview.setPadding(new Insets(10, 0, 10, 0));

        // Screenshot Carousel / Dashboard Preview Container (Images 2 & 5)
        StackPane carouselContainer = new StackPane();
        carouselContainer.setPrefHeight(200);
        carouselContainer.setStyle("-fx-background-color: #1a1b1e; -fx-background-radius: 8; -fx-border-color: #313438; -fx-border-radius: 8;");

        Node mockGraphic = createCarouselGraphic(p);
        carouselContainer.getChildren().add(mockGraphic);

        // Carousel dots indicator: ( •  o  o  o  o )
        HBox dotsRow = new HBox(6);
        dotsRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < 5; i++) {
            Circle dot = new Circle(3);
            dot.setFill(i == 0 ? Color.web("#dfe1e5") : Color.web("#43454a"));
            dotsRow.getChildren().add(dot);
        }

        // Description text
        Label desc = new Label(p.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -text; -fx-font-size: 12px; -fx-line-spacing: 4px;");

        overview.getChildren().addAll(carouselContainer, dotsRow, desc);
        return overview;
    }

    private Node buildWhatsNewContent(Plugin p) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 4, 12, 4));

        Label ver = new Label("Version " + p.getVersion());
        ver.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text;");

        String notes = p.getReleaseNotes();
        if (notes == null || notes.isBlank()) {
            notes = "• Compatibility with DBNavigator Pro 2.0.0+\n" +
                    "• Performance optimization and responsive layout improvements\n" +
                    "• Fixes for UI interaction and keyboard shortcut handling";
        }

        Label notesLbl = new Label(notes);
        notesLbl.setWrapText(true);
        notesLbl.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px; -fx-line-spacing: 3px;");

        box.getChildren().addAll(ver, notesLbl);
        return box;
    }

    private Node buildReviewsContent(Plugin p) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12, 4, 12, 4));

        Label ratingTitle = new Label("Customer Reviews");
        ratingTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text;");

        Label score = new Label("★ " + p.formattedRating() + " / 5.0");
        score.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0a44c;");

        Label review1 = new Label("★★★★★  \"Essential plugin for our daily database workflows and queries.\"");
        review1.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");

        Label review2 = new Label("★★★★★  \"Works seamlessly, responsive, and very clean integration.\"");
        review2.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

        box.getChildren().addAll(ratingTitle, score, review1, review2);
        return box;
    }

    private Node buildAdditionalInfoContent(Plugin p) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.setPadding(new Insets(12, 4, 12, 4));

        grid.addRow(0, makeDimLabel("Plugin ID:"), makeValLabel(p.getId()));
        grid.addRow(1, makeDimLabel("Vendor:"), makeValLabel(p.getVendor()));
        grid.addRow(2, makeDimLabel("Category:"), makeValLabel(p.getCategory()));
        grid.addRow(3, makeDimLabel("Downloads:"), makeValLabel(p.formattedDownloads()));
        grid.addRow(4, makeDimLabel("License:"), makeValLabel("Commercial / JetBrains Proprietary"));
        grid.addRow(5, makeDimLabel("Compatibility:"), makeValLabel("DBNavigator Pro 2.0.0+"));

        return grid;
    }

    private Label makeDimLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px;");
        return l;
    }

    private Label makeValLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: -text; -fx-font-size: 12px; -fx-font-weight: bold;");
        return l;
    }

    // -------------------------------------------------------------------------
    // Visual Graphics & Brand Icons
    // -------------------------------------------------------------------------

    private Node createBrandIcon(Plugin p, int size) {
        StackPane pane = new StackPane();
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);

        Rectangle bg = new Rectangle(size, size);
        bg.setArcWidth(size / 3.0);
        bg.setArcHeight(size / 3.0);

        String type = p.getIconType();
        switch (type) {
            case "air" -> {
                bg.setFill(Color.web("#1e293b"));
                Label glyph = new Label("::: Air");
                glyph.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: " + (size > 28 ? "10px" : "8px") + ";");
                pane.getChildren().addAll(bg, glyph);
            }
            case "ideavim" -> {
                bg.setFill(Color.web("#059669"));
                Label v = new Label("V");
                v.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (size > 28 ? "16px" : "12px") + ";");
                pane.getChildren().addAll(bg, v);
            }
            case "ignore" -> {
                bg.setFill(Color.web("#475569"));
                Label star = new Label(".i*");
                star.setStyle("-fx-text-fill: #f1f5f9; -fx-font-weight: bold; -fx-font-size: " + (size > 28 ? "12px" : "9px") + ";");
                pane.getChildren().addAll(bg, star);
            }
            case "bigdata" -> {
                bg.setFill(Color.web("#6d28d9"));
                Label bd = new Label("BD");
                bd.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (size > 28 ? "12px" : "9px") + ";");
                pane.getChildren().addAll(bg, bd);
            }
            case "wakatime" -> {
                bg.setFill(Color.web("#0284c7"));
                Label w = new Label("W");
                w.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (size > 28 ? "14px" : "10px") + ";");
                pane.getChildren().addAll(bg, w);
            }
            case "mcp" -> {
                bg.setFill(Color.web("#1e3a8a"));
                Label mcp = new Label("MCP");
                mcp.setStyle("-fx-text-fill: #93c5fd; -fx-font-weight: bold; -fx-font-size: " + (size > 28 ? "9px" : "7px") + ";");
                pane.getChildren().addAll(bg, mcp);
            }
            case "db" -> {
                bg.setFill(Color.web("#0369a1"));
                pane.getChildren().addAll(bg, Icons.of(FontAwesomeSolid.DATABASE, "white", size > 28 ? 14 : 10));
            }
            case "xml" -> {
                bg.setFill(Color.web("#b45309"));
                Label slash = new Label("//");
                slash.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (size > 28 ? "12px" : "9px") + ";");
                pane.getChildren().addAll(bg, slash);
            }
            case "lang" -> {
                bg.setFill(Color.web("#b91c1c"));
                Label han = new Label("汉");
                han.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (size > 28 ? "12px" : "9px") + ";");
                pane.getChildren().addAll(bg, han);
            }
            case "ml" -> {
                bg.setFill(Color.web("#7c3aed"));
                pane.getChildren().addAll(bg, Icons.of(FontAwesomeSolid.BRAIN, "white", size > 28 ? 13 : 9));
            }
            default -> {
                bg.setFill(Color.web(p.getIconColor()));
                pane.getChildren().addAll(bg, Icons.of(FontAwesomeSolid.PUZZLE_PIECE, "white", size > 28 ? 13 : 9));
            }
        }
        return pane;
    }

    private Node createCarouselGraphic(Plugin p) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(16));

        Node bigIcon = createBrandIcon(p, 48);

        Label header = new Label(p.getName() + " Workspace Preview");
        header.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label sub = new Label("Real-time telemetry, query assistance, and interactive database controls.");
        sub.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px;");

        box.getChildren().addAll(bigIcon, header, sub);
        return box;
    }

    // -------------------------------------------------------------------------
    // Dialogs & Actions
    // -------------------------------------------------------------------------

    private void installPluginFromDiskAction() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Install Plugin from Disk");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Plugin Archives (*.jar, *.zip)", "*.jar", "*.zip"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        Window owner = getScene() == null ? null : getScene().getWindow();
        File file = chooser.showOpenDialog(owner);
        if (file != null) {
            try {
                Plugin imported = pluginManager.installPluginFromDisk(file);
                refreshData();
                selectPlugin(imported);
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Could not install plugin: " + ex.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    private void showManageRepositoriesDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Custom Plugin Repositories");
        dialog.setHeaderText("Add custom RozeHub or marketplace repository URLs:");

        TextField repoInput = new TextField(AppSettingsStore.load().getEffectiveRozeHubEndpoint());
        repoInput.setPrefWidth(380);

        VBox content = new VBox(10, new Label("Repository URL:"), repoInput);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? repoInput.getText() : null);
        dialog.showAndWait().ifPresent(url -> {
            if (!url.isBlank()) {
                var s = AppSettingsStore.load();
                s.setUpdateEndpoint(url.trim());
                AppSettingsStore.save(s);
                refreshData();
            }
        });
    }

    private void showProxyDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "HTTP Proxy Settings can be configured under Appearance & Behavior > System Settings > HTTP Proxy.", ButtonType.OK);
        alert.setHeaderText("Proxy Settings");
        alert.showAndWait();
    }

    private void showCertificatesDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "All marketplace packages and application updates are cryptographically verified using SHA-256 signatures from RozeHub.", ButtonType.OK);
        alert.setHeaderText("Plugin Certificates");
        alert.showAndWait();
    }
}

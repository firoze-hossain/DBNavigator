package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.action.ActionManager;
import com.roze.dbnavigator.ui.action.AnAction;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Dedicated settings panel and page for customizing Menus and Toolbars.
 * Provides hierarchical tree management, add/edit/move/delete/restore actions,
 * icon selection, and real-time search filtering.
 */
public class MenusAndToolbarsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public MenusAndToolbarsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        Label breadcrumb = new Label("Appearance & Behavior \u203A Menus and Toolbars");
        breadcrumb.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");

        List<AppSettingsStore.MenuItemConfig> currentConfigs = new ArrayList<>();
        if (settings.getMenusAndToolbars() != null) {
            for (AppSettingsStore.MenuItemConfig c : settings.getMenusAndToolbars()) {
                currentConfigs.add(c.copy());
            }
        }
        if (currentConfigs.isEmpty()) {
            for (AppSettingsStore.MenuItemConfig c : AppSettingsStore.Settings.defaultMenusAndToolbars()) {
                currentConfigs.add(c.copy());
            }
        }
        inputs.put("menusAndToolbarsList", currentConfigs);

        // Main TreeView
        TreeView<AppSettingsStore.MenuItemConfig> treeView = new TreeView<>();
        treeView.setShowRoot(false);
        TreeItem<AppSettingsStore.MenuItemConfig> hiddenRoot = new TreeItem<>();
        hiddenRoot.setExpanded(true);
        treeView.setRoot(hiddenRoot);

        Runnable syncConfigsFromTree = () -> {
            currentConfigs.clear();
            for (TreeItem<AppSettingsStore.MenuItemConfig> rootItem : hiddenRoot.getChildren()) {
                currentConfigs.add(extractMenuItemConfig(rootItem));
            }
        };

        Consumer<String> populateTree = filterText -> {
            hiddenRoot.getChildren().clear();
            String query = filterText != null ? filterText.trim().toLowerCase() : "";
            for (AppSettingsStore.MenuItemConfig rootCfg : currentConfigs) {
                TreeItem<AppSettingsStore.MenuItemConfig> node = buildFilterableTreeItem(rootCfg, query);
                if (node != null) {
                    hiddenRoot.getChildren().add(node);
                    if (!query.isEmpty()) {
                        expandAll(node);
                    }
                }
            }
        };

        populateTree.accept("");

        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(AppSettingsStore.MenuItemConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else if (item.getType() == AppSettingsStore.MenuItemConfig.Type.SEPARATOR) {
                    setText("--------------------------------------------------------------------------------");
                    setTextFill(javafx.scene.paint.Color.web("#6c707e"));
                    setGraphic(null);
                    setStyle("-fx-font-family: monospace; -fx-opacity: 0.6; -fx-font-size: 11px;");
                } else if (item.getType() == AppSettingsStore.MenuItemConfig.Type.GROUP) {
                    setText(item.getText());
                    setTextFill(javafx.scene.paint.Color.web("#dfdfe0"));
                    Node icon = ActionManager.createIconNode(item.getIconName(), item.getIconPath(), 13);
                    setGraphic(icon);
                    setStyle("-fx-font-weight: normal;");
                } else {
                    setText(item.getText());
                    setTextFill(javafx.scene.paint.Color.web("#dfdfe0"));
                    Node icon = ActionManager.createIconNode(item.getIconName(), item.getIconPath(), 13);
                    if (icon == null) {
                        AnAction registered = ActionManager.getInstance().getAction(item.getId());
                        if (registered != null && registered.getIcon() != null) {
                            icon = Icons.of(registered.getIcon(), registered.getIconColor() != null ? registered.getIconColor() : "#a9b7c6", 13);
                        }
                    }
                    setGraphic(icon);
                    setStyle("-fx-font-weight: normal;");
                }
            }
        });

        // Action Toolbar
        Button addBtn = new Button("Add\u2026");
        addBtn.getStyleClass().add("small-button");

        Button editBtn = new Button();
        editBtn.setGraphic(Icons.of(FontAwesomeSolid.EDIT, "#a9b7c6", 12));
        editBtn.setTooltip(new Tooltip("Edit action"));
        editBtn.getStyleClass().add("icon-button");

        Button upBtn = new Button();
        upBtn.setGraphic(Icons.of(FontAwesomeSolid.ARROW_UP, "#a9b7c6", 12));
        upBtn.setTooltip(new Tooltip("Move up"));
        upBtn.getStyleClass().add("icon-button");

        Button downBtn = new Button();
        downBtn.setGraphic(Icons.of(FontAwesomeSolid.ARROW_DOWN, "#a9b7c6", 12));
        downBtn.setTooltip(new Tooltip("Move down"));
        downBtn.getStyleClass().add("icon-button");

        Button deleteBtn = new Button();
        deleteBtn.setGraphic(Icons.of(FontAwesomeSolid.TRASH_ALT, "#a9b7c6", 12));
        deleteBtn.setTooltip(new Tooltip("Delete"));
        deleteBtn.getStyleClass().add("icon-button");

        Button restoreBtn = new Button();
        restoreBtn.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#a9b7c6", 12));
        restoreBtn.setTooltip(new Tooltip("Restore Defaults"));
        restoreBtn.getStyleClass().add("icon-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("Search");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> populateTree.accept(newVal));

        HBox toolbar = new HBox(6, addBtn, editBtn, upBtn, downBtn, deleteBtn, restoreBtn, spacer, searchField);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 4, 0));

        // Event Handlers
        addBtn.setOnAction(e -> showAddActionDialog(treeView, syncConfigsFromTree));

        editBtn.setOnAction(e -> {
            TreeItem<AppSettingsStore.MenuItemConfig> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null && sel.getValue().getType() != AppSettingsStore.MenuItemConfig.Type.SEPARATOR) {
                TextInputDialog dialog = new TextInputDialog(sel.getValue().getText());
                dialog.setTitle("Edit Action");
                dialog.setHeaderText("Edit label for " + sel.getValue().getId());
                dialog.setContentText("Text:");
                dialog.showAndWait().ifPresent(newText -> {
                    if (!newText.isBlank()) {
                        sel.getValue().setText(newText.trim());
                        treeView.refresh();
                        syncConfigsFromTree.run();
                    }
                });
            }
        });

        upBtn.setOnAction(e -> {
            TreeItem<AppSettingsStore.MenuItemConfig> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getParent() != null) {
                ObservableList<TreeItem<AppSettingsStore.MenuItemConfig>> siblings = sel.getParent().getChildren();
                int idx = siblings.indexOf(sel);
                if (idx > 0) {
                    siblings.remove(idx);
                    siblings.add(idx - 1, sel);
                    treeView.getSelectionModel().select(sel);
                    syncConfigsFromTree.run();
                }
            }
        });

        downBtn.setOnAction(e -> {
            TreeItem<AppSettingsStore.MenuItemConfig> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getParent() != null) {
                ObservableList<TreeItem<AppSettingsStore.MenuItemConfig>> siblings = sel.getParent().getChildren();
                int idx = siblings.indexOf(sel);
                if (idx < siblings.size() - 1) {
                    siblings.remove(idx);
                    siblings.add(idx + 1, sel);
                    treeView.getSelectionModel().select(sel);
                    syncConfigsFromTree.run();
                }
            }
        });

        deleteBtn.setOnAction(e -> {
            TreeItem<AppSettingsStore.MenuItemConfig> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getParent() != null && sel.getParent() != hiddenRoot) {
                TreeItem<AppSettingsStore.MenuItemConfig> parent = sel.getParent();
                int idx = parent.getChildren().indexOf(sel);
                parent.getChildren().remove(sel);
                if (!parent.getChildren().isEmpty()) {
                    treeView.getSelectionModel().select(parent.getChildren().get(Math.min(idx, parent.getChildren().size() - 1)));
                } else {
                    treeView.getSelectionModel().select(parent);
                }
                syncConfigsFromTree.run();
            }
        });

        restoreBtn.setOnAction(e -> {
            ContextMenu restoreMenu = new ContextMenu();
            TreeItem<AppSettingsStore.MenuItemConfig> sel = treeView.getSelectionModel().getSelectedItem();
            TreeItem<AppSettingsStore.MenuItemConfig> rootAncestor = findRootAncestor(sel, hiddenRoot);
            String selectedMenuName = rootAncestor != null && rootAncestor.getValue() != null ? rootAncestor.getValue().getText() : "Selected Menu";

            MenuItem restoreSelected = new MenuItem("Restore " + selectedMenuName);
            restoreSelected.setOnAction(ev -> {
                if (rootAncestor != null && rootAncestor.getValue() != null) {
                    String rootId = rootAncestor.getValue().getId();
                    settings.restoreMenu(rootId);
                    AppSettingsStore.MenuItemConfig restored = settings.getMenuConfig(rootId);
                    if (restored != null) {
                        for (int i = 0; i < currentConfigs.size(); i++) {
                            if (rootId.equalsIgnoreCase(currentConfigs.get(i).getId())) {
                                currentConfigs.set(i, restored.copy());
                                break;
                            }
                        }
                    }
                    populateTree.accept(searchField.getText());
                }
            });

            MenuItem restoreAll = new MenuItem("Restore All Defaults");
            restoreAll.setOnAction(ev -> {
                settings.restoreAllMenus();
                currentConfigs.clear();
                for (AppSettingsStore.MenuItemConfig c : AppSettingsStore.Settings.defaultMenusAndToolbars()) {
                    currentConfigs.add(c.copy());
                }
                populateTree.accept(searchField.getText());
            });

            restoreMenu.getItems().addAll(restoreSelected, restoreAll);
            restoreMenu.show(restoreBtn, javafx.geometry.Side.BOTTOM, 0, 4);
        });

        VBox.setVgrow(treeView, Priority.ALWAYS);
        VBox panel = new VBox(8, breadcrumb, toolbar, treeView);
        panel.setPadding(new Insets(6, 12, 12, 12));
        return panel;
    }

    private static TreeItem<AppSettingsStore.MenuItemConfig> findRootAncestor(
            TreeItem<AppSettingsStore.MenuItemConfig> item,
            TreeItem<AppSettingsStore.MenuItemConfig> hiddenRoot) {
        if (item == null || item == hiddenRoot) return null;
        TreeItem<AppSettingsStore.MenuItemConfig> cur = item;
        while (cur.getParent() != null && cur.getParent() != hiddenRoot) {
            cur = cur.getParent();
        }
        return cur;
    }

    private static AppSettingsStore.MenuItemConfig extractMenuItemConfig(TreeItem<AppSettingsStore.MenuItemConfig> treeItem) {
        AppSettingsStore.MenuItemConfig cfg = treeItem.getValue().copy();
        cfg.getChildren().clear();
        for (TreeItem<AppSettingsStore.MenuItemConfig> childItem : treeItem.getChildren()) {
            cfg.getChildren().add(extractMenuItemConfig(childItem));
        }
        return cfg;
    }

    private static TreeItem<AppSettingsStore.MenuItemConfig> buildFilterableTreeItem(AppSettingsStore.MenuItemConfig cfg, String query) {
        boolean matchesSelf = query.isEmpty() || (cfg.getText() != null && cfg.getText().toLowerCase().contains(query))
                || (cfg.getId() != null && cfg.getId().toLowerCase().contains(query));

        TreeItem<AppSettingsStore.MenuItemConfig> node = new TreeItem<>(cfg);
        boolean hasMatchingChild = false;
        for (AppSettingsStore.MenuItemConfig child : cfg.getChildren()) {
            TreeItem<AppSettingsStore.MenuItemConfig> childNode = buildFilterableTreeItem(child, query);
            if (childNode != null) {
                hasMatchingChild = true;
                node.getChildren().add(childNode);
            }
        }

        if (matchesSelf || hasMatchingChild) {
            return node;
        }
        return null;
    }

    private static void expandAll(TreeItem<?> item) {
        if (item == null) return;
        item.setExpanded(true);
        for (TreeItem<?> child : item.getChildren()) {
            expandAll(child);
        }
    }

    private static void showAddActionDialog(
            TreeView<AppSettingsStore.MenuItemConfig> mainTreeView,
            Runnable syncConfigsFromTree) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Action");
        dialog.setMinWidth(480);
        dialog.setMinHeight(520);

        TextField searchField = new TextField();
        searchField.setPromptText("Search");

        TreeItem<Object> catalogRoot = new TreeItem<>();
        catalogRoot.setExpanded(true);

        TreeItem<Object> separatorItem = new TreeItem<>("--- Separator");
        catalogRoot.getChildren().add(separatorItem);

        ActionManager am = ActionManager.getInstance();
        List<ActionManager.ActionCatalogCategory> categories = am.getActionCatalog();

        Consumer<String> filterCatalog = query -> {
            catalogRoot.getChildren().clear();
            catalogRoot.getChildren().add(separatorItem);
            String q = query != null ? query.trim().toLowerCase() : "";

            for (ActionManager.ActionCatalogCategory cat : categories) {
                TreeItem<Object> catItem = buildCatalogCategoryItem(cat, q);
                if (catItem != null) {
                    catalogRoot.getChildren().add(catItem);
                    if (!q.isEmpty()) {
                        expandAll(catItem);
                    }
                }
            }
        };

        filterCatalog.accept("");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterCatalog.accept(newVal));

        TreeView<Object> actionTree = new TreeView<>(catalogRoot);
        actionTree.setShowRoot(false);
        actionTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof String s) {
                    setText(s);
                    setGraphic(null);
                    if ("--- Separator".equals(s)) {
                        setStyle("-fx-font-family: monospace; -fx-text-fill: -text-dim;");
                    } else {
                        setStyle("");
                    }
                } else if (item instanceof ActionManager.ActionCatalogCategory cat) {
                    setText(cat.getName());
                    setGraphic(Icons.of(FontAwesomeSolid.FOLDER, "#dcb67a", 12));
                    setStyle("-fx-font-weight: bold;");
                } else if (item instanceof ActionManager.ActionCatalogEntry entry) {
                    setText(entry.getName());
                    Node icon = null;
                    if (entry.getIconName() != null) {
                        try {
                            FontAwesomeSolid fa = FontAwesomeSolid.valueOf(entry.getIconName().toUpperCase().replace('-', '_'));
                            icon = Icons.of(fa, "#a9b7c6", 12);
                        } catch (Exception ignored) {}
                    }
                    setGraphic(icon);
                    setStyle("");
                }
            }
        });

        // Icon Section
        Label iconLabel = new Label("Icon:");
        TextField iconField = new TextField("<None>");
        iconField.setEditable(false);
        HBox.setHgrow(iconField, Priority.ALWAYS);

        Button browseBtn = new Button("Browse\u2026");
        browseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Custom Icon");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files (*.svg, *.png, *.ico)", "*.svg", "*.png", "*.ico", "*.jpg")
            );
            File file = chooser.showOpenDialog(dialog);
            if (file != null) {
                iconField.setText(file.getAbsolutePath());
            }
        });

        HBox iconRow = new HBox(8, iconLabel, iconField, browseBtn);
        iconRow.setAlignment(Pos.CENTER_LEFT);

        Label iconHint = new Label("To set custom icon, specify path to SVG or PNG file.");
        iconHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        VBox iconSection = new VBox(4, iconRow, iconHint);

        // Buttons
        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setPrefWidth(80);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setPrefWidth(80);
        cancelBtn.setOnAction(e -> dialog.close());

        okBtn.setOnAction(e -> {
            TreeItem<Object> sel = actionTree.getSelectionModel().getSelectedItem();
            if (sel == null || sel.getValue() == null) return;

            AppSettingsStore.MenuItemConfig newConfig = null;
            if ("--- Separator".equals(sel.getValue())) {
                newConfig = AppSettingsStore.MenuItemConfig.separator();
            } else if (sel.getValue() instanceof ActionManager.ActionCatalogEntry entry) {
                newConfig = AppSettingsStore.MenuItemConfig.action(entry.getId(), entry.getName());
                String customIcon = iconField.getText();
                if (customIcon != null && !customIcon.isBlank() && !"<None>".equals(customIcon)) {
                    newConfig.setIconPath(customIcon.trim());
                } else if (entry.getIconName() != null) {
                    newConfig.setIconName(entry.getIconName());
                }
            }

            if (newConfig != null) {
                TreeItem<AppSettingsStore.MenuItemConfig> targetItem = new TreeItem<>(newConfig);
                TreeItem<AppSettingsStore.MenuItemConfig> mainSel = mainTreeView.getSelectionModel().getSelectedItem();
                if (mainSel != null) {
                    if (mainSel.getValue() != null && mainSel.getValue().getType() == AppSettingsStore.MenuItemConfig.Type.GROUP) {
                        mainSel.getChildren().add(targetItem);
                        mainSel.setExpanded(true);
                    } else if (mainSel.getParent() != null) {
                        int idx = mainSel.getParent().getChildren().indexOf(mainSel);
                        mainSel.getParent().getChildren().add(idx + 1, targetItem);
                    } else {
                        mainSel.getChildren().add(targetItem);
                    }
                } else if (!mainTreeView.getRoot().getChildren().isEmpty()) {
                    mainTreeView.getRoot().getChildren().get(0).getChildren().add(targetItem);
                }
                mainTreeView.getSelectionModel().select(targetItem);
                syncConfigsFromTree.run();
                dialog.close();
            }
        });

        HBox buttonBar = new HBox(8, okBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(actionTree, Priority.ALWAYS);
        VBox content = new VBox(10, searchField, actionTree, iconSection, buttonBar);
        content.setPadding(new Insets(12));
        content.setStyle("-fx-background-color: -bg;");

        Scene scene = new Scene(content, 500, 560);
        dialog.setScene(scene);
        try {
            dialog.getScene().getStylesheets().add(ThemeManager.stylesheetUrl(ThemeManager.getCurrent()));
        } catch (Exception ignored) {}
        dialog.showAndWait();
    }

    private static TreeItem<Object> buildCatalogCategoryItem(ActionManager.ActionCatalogCategory cat, String query) {
        TreeItem<Object> catItem = new TreeItem<>(cat);
        boolean hasMatchingChild = false;

        for (ActionManager.ActionCatalogCategory sub : cat.getSubCategories()) {
            TreeItem<Object> subItem = buildCatalogCategoryItem(sub, query);
            if (subItem != null) {
                hasMatchingChild = true;
                catItem.getChildren().add(subItem);
            }
        }

        for (ActionManager.ActionCatalogEntry entry : cat.getEntries()) {
            boolean matches = query.isEmpty() || entry.getName().toLowerCase().contains(query)
                    || entry.getId().toLowerCase().contains(query);
            if (matches) {
                hasMatchingChild = true;
                catItem.getChildren().add(new TreeItem<>(entry));
            }
        }

        if (hasMatchingChild || (query.isEmpty())) {
            return catItem;
        }
        return null;
    }
}

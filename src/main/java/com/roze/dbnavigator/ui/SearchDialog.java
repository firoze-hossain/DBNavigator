package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.SearchHistoryStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import com.roze.dbnavigator.search.SearchEverywhereService;
import com.roze.dbnavigator.search.SearchHit;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DataGrip-grade "Search Everywhere" Global Search Dialog.
 *
 * Engineering & Features:
 * - Persistence Across Restarts: Restores last search keyword and recent items
 *   from previous sessions via SearchHistoryStore.
 * - Multi-Source Search: Indexes Tables, Views, Columns, Sequences, Keys,
 *   Constraints, Indexes, Consoles, SQL Files, IDE Actions, and Text occurrences.
 * - Substring Match Highlighting: Real-time amber/gold match badges on results.
 * - Deep Navigation:
 *     * Table / View: Opens data tab and reveals/selects in Database Explorer.
 *     * Column: Opens table data tab, scrolls horizontally to that specific
 *       column, and selects the first row cell value of that column.
 *     * Console / File: Opens console/file editor.
 *     * Action / Setting: Executes the IDE action / opens configuration dialog.
 *     * Open in Right Split: Opens the hit in split editor layout (Shift+Enter or button).
 */
public class SearchDialog {

    private final Stage stage = new Stage();
    private final TextField searchField = new TextField();
    private final ListView<SearchHit> resultList = new ListView<>();
    private final Label uriLabel = new Label();
    private final Button splitRightBtn = new Button("Open in Right Split");
    private final CheckBox includeNonProjectCheck = new CheckBox("Include non-project items");
    private final Label commandTip = new Label("Type / to see commands");
    private final Button clearBtn = new Button("✕");

    private final List<Button> tabButtons = new ArrayList<>();
    private SearchHit.Category currentCategory = SearchHit.Category.ALL;

    private final MainWindow mainWindow;
    private final AtomicInteger generation = new AtomicInteger();

    public SearchDialog(Window owner, MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Search Everywhere");

        // 1. Top Category Tabs Header
        HBox tabsBox = buildCategoryTabs();

        // 2. Search Input Field Bar
        HBox searchBar = buildSearchBar();

        // 3. Results List View
        configureResultList();

        // 4. Status Bar / Footer
        HBox footerBar = buildFooterBar();

        VBox root = new VBox(0, tabsBox, searchBar, resultList, footerBar);
        root.getStyleClass().add("search-dialog");
        VBox.setVgrow(resultList, Priority.ALWAYS);

        Scene scene = new Scene(root, 700, 480);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);

        // Center on owner
        if (owner != null) {
            stage.setOnShown(e -> {
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2.0);
                stage.setY(owner.getY() + Math.max(40, (owner.getHeight() - stage.getHeight()) / 3.0));
            });
        }

        wireKeyboardAndEvents();
    }

    private HBox buildCategoryTabs() {
        HBox tabs = new HBox(4);
        tabs.setAlignment(Pos.CENTER_LEFT);
        tabs.setPadding(new Insets(8, 12, 6, 12));
        tabs.getStyleClass().add("search-tabs-bar");

        SearchHit.Category[] categories = {
                SearchHit.Category.ALL,
                SearchHit.Category.DATABASE,
                SearchHit.Category.FILES,
                SearchHit.Category.CODE,
                SearchHit.Category.ACTIONS,
                SearchHit.Category.TEXT
        };

        for (SearchHit.Category cat : categories) {
            String label = switch (cat) {
                case ALL -> "All";
                case DATABASE -> "Database";
                case FILES -> "Files";
                case CODE -> "Code";
                case ACTIONS -> "Actions";
                case TEXT -> "Text";
            };
            Button btn = new Button(label);
            btn.getStyleClass().add("search-tab-pill");
            if (cat == currentCategory) {
                btn.getStyleClass().add("search-tab-pill-active");
            }
            btn.setOnAction(e -> selectCategory(cat));
            tabButtons.add(btn);
            tabs.getChildren().add(btn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        includeNonProjectCheck.getStyleClass().add("search-nonproject-checkbox");
        includeNonProjectCheck.setOnAction(e -> search(searchField.getText()));

        Button filterBtn = new Button();
        filterBtn.setGraphic(Icons.of(FontAwesomeSolid.FILTER, "#868a91", 11));
        filterBtn.getStyleClass().add("search-icon-btn");

        Button optionsBtn = new Button();
        optionsBtn.setGraphic(Icons.of(FontAwesomeSolid.WINDOW_RESTORE, "#868a91", 11));
        optionsBtn.getStyleClass().add("search-icon-btn");

        HBox rightControls = new HBox(8, includeNonProjectCheck, filterBtn, optionsBtn);
        rightControls.setAlignment(Pos.CENTER_RIGHT);

        tabs.getChildren().addAll(spacer, rightControls);
        return tabs;
    }

    private void selectCategory(SearchHit.Category cat) {
        this.currentCategory = cat;
        for (int i = 0; i < tabButtons.size(); i++) {
            Button btn = tabButtons.get(i);
            btn.getStyleClass().remove("search-tab-pill-active");
            if (i == cat.ordinal()) {
                btn.getStyleClass().add("search-tab-pill-active");
            }
        }
        search(searchField.getText());
    }

    private void cycleCategory(boolean forward) {
        int total = SearchHit.Category.values().length;
        int next = forward ? (currentCategory.ordinal() + 1) % total : (currentCategory.ordinal() - 1 + total) % total;
        selectCategory(SearchHit.Category.values()[next]);
    }

    private HBox buildSearchBar() {
        searchField.setPromptText("Search everywhere…");
        searchField.getStyleClass().add("search-input-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        clearBtn.getStyleClass().add("search-clear-btn");
        clearBtn.setVisible(false);
        clearBtn.setOnAction(e -> {
            searchField.clear();
            searchField.requestFocus();
        });

        commandTip.getStyleClass().add("search-command-tip");

        StackPane rightFieldStack = new StackPane(commandTip, clearBtn);
        rightFieldStack.setAlignment(Pos.CENTER_RIGHT);
        StackPane.setMargin(clearBtn, new Insets(0, 4, 0, 0));
        StackPane.setMargin(commandTip, new Insets(0, 8, 0, 0));

        HBox fieldWrapper = new HBox(6, searchField, rightFieldStack);
        fieldWrapper.setAlignment(Pos.CENTER_LEFT);
        fieldWrapper.getStyleClass().add("search-field-wrapper");
        HBox.setHgrow(fieldWrapper, Priority.ALWAYS);

        HBox searchBar = new HBox(8, Icons.of(FontAwesomeSolid.SEARCH, "#868a91", 13), fieldWrapper);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(4, 12, 8, 12));
        searchBar.getStyleClass().add("search-bar");

        searchField.textProperty().addListener((obs, old, text) -> {
            boolean hasText = text != null && !text.isEmpty();
            clearBtn.setVisible(hasText);
            commandTip.setVisible(!hasText);
            search(text);
        });

        return searchBar;
    }

    private void configureResultList() {
        resultList.getStyleClass().add("search-result-list");
        resultList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SearchHit hit, boolean empty) {
                super.updateItem(hit, empty);
                if (empty || hit == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                // 1. Icon & Badge
                HBox iconBox = new HBox(4);
                iconBox.setAlignment(Pos.CENTER_LEFT);
                iconBox.getChildren().add(Icons.of(hit.getIcon(), hit.getIconColor(), 13));

                String badgeText = hit.getBadge();
                if (badgeText != null) {
                    Label badge = new Label(badgeText);
                    badge.getStyleClass().add("search-badge");
                    if ("PK".equals(badgeText)) badge.getStyleClass().add("search-badge-pk");
                    else if ("123".equals(badgeText)) badge.getStyleClass().add("search-badge-seq");
                    iconBox.getChildren().add(badge);
                }

                // 2. Title with Match Highlighting (Amber/Gold TextFlow)
                TextFlow titleFlow = buildHighlightedTitle(hit.getName(), searchField.getText());

                // 3. Middle Spacer
                Region gap = new Region();
                HBox.setHgrow(gap, Priority.ALWAYS);

                // 4. Detail Location
                Label detail = new Label(hit.getDetail());
                detail.getStyleClass().add("search-item-detail");

                HBox row = new HBox(8, iconBox, titleFlow, gap, detail);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 8, 4, 8));
                setGraphic(row);
                setText(null);
            }
        });

        resultList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getFullUri() != null) {
                uriLabel.setText(newVal.getFullUri());
            } else {
                uriLabel.setText("");
            }
        });
    }

    private TextFlow buildHighlightedTitle(String name, String query) {
        TextFlow flow = new TextFlow();
        if (name == null) return flow;

        if (query == null || query.isBlank()) {
            Text text = new Text(name);
            text.getStyleClass().add("search-text-normal");
            flow.getChildren().add(text);
            return flow;
        }

        String q = query.trim().toLowerCase();
        String lowerName = name.toLowerCase();
        int lastIdx = 0;
        int matchIdx;

        while ((matchIdx = lowerName.indexOf(q, lastIdx)) != -1) {
            if (matchIdx > lastIdx) {
                Text pre = new Text(name.substring(lastIdx, matchIdx));
                pre.getStyleClass().add("search-text-normal");
                flow.getChildren().add(pre);
            }

            Text match = new Text(name.substring(matchIdx, matchIdx + q.length()));
            match.getStyleClass().add("search-match-text");
            flow.getChildren().add(match);

            lastIdx = matchIdx + q.length();
        }

        if (lastIdx < name.length()) {
            Text post = new Text(name.substring(lastIdx));
            post.getStyleClass().add("search-text-normal");
            flow.getChildren().add(post);
        }

        return flow;
    }

    private HBox buildFooterBar() {
        uriLabel.getStyleClass().add("search-footer-uri");
        HBox.setHgrow(uriLabel, Priority.ALWAYS);

        splitRightBtn.getStyleClass().add("search-footer-split-btn");
        splitRightBtn.setOnAction(e -> openSelected(true));

        HBox footer = new HBox(10, uriLabel, splitRightBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(6, 12, 6, 12));
        footer.getStyleClass().add("search-footer-bar");
        return footer;
    }

    private void wireKeyboardAndEvents() {
        searchField.setOnKeyPressed(this::handleKey);
        resultList.setOnKeyPressed(this::handleKey);

        resultList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openSelected(false);
            }
        });

        stage.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) stage.close();
        });
    }

    private void handleKey(KeyEvent e) {
        if (e.getCode() == KeyCode.DOWN) {
            resultList.getSelectionModel().selectNext();
            resultList.scrollTo(resultList.getSelectionModel().getSelectedIndex());
            e.consume();
        } else if (e.getCode() == KeyCode.UP) {
            resultList.getSelectionModel().selectPrevious();
            resultList.scrollTo(resultList.getSelectionModel().getSelectedIndex());
            e.consume();
        } else if (e.getCode() == KeyCode.ENTER) {
            openSelected(e.isShiftDown());
            e.consume();
        } else if (e.getCode() == KeyCode.TAB) {
            cycleCategory(!e.isShiftDown());
            e.consume();
        } else if (e.getCode() == KeyCode.ESCAPE) {
            stage.close();
            e.consume();
        }
    }

    public void show() {
        // Restore last search keyword from previous session (DataGrip behavior)
        String lastQuery = SearchHistoryStore.getLastQuery();
        if (lastQuery != null && !lastQuery.isBlank()) {
            searchField.setText(lastQuery);
            searchField.selectAll();
            search(lastQuery);
        } else {
            showRecentItems();
        }

        stage.show();
        searchField.requestFocus();
    }

    private void showRecentItems() {
        List<SearchHistoryStore.RecentItem> recents = SearchHistoryStore.getRecentItems();
        List<SearchHit> hits = new ArrayList<>();

        for (SearchHistoryStore.RecentItem r : recents) {
            ConnectionProfile prof = null;
            if (r.profileId() != null) {
                for (ConnectionProfile p : com.roze.dbnavigator.db.ConnectionStore.load()) {
                    if (r.profileId().equals(p.getId())) {
                        prof = p;
                        break;
                    }
                }
            }

            SearchHit.Type type;
            try {
                type = SearchHit.Type.valueOf(r.type());
            } catch (Exception ex) {
                type = SearchHit.Type.TABLE;
            }

            DbObject obj = null;
            if (r.name() != null) {
                Kind k = (type == SearchHit.Type.COLUMN) ? Kind.COLUMN : Kind.TABLE;
                obj = new DbObject(r.name(), k, r.catalog(), r.schema());
                if (r.parentTable() != null) {
                    obj.setTableName(r.parentTable());
                }
            }

            String loc = (r.catalog() != null ? r.catalog() + "." : "")
                    + (r.schema() != null ? r.schema() + "." : "")
                    + (r.parentTable() != null ? r.parentTable() + " " : "")
                    + "[" + (r.profileName() != null ? r.profileName() : "db") + "]";

            hits.add(new SearchHit.Builder()
                    .type(type)
                    .name(r.name())
                    .detail(loc)
                    .fullUri(r.uri() != null ? r.uri() : "RECENT/" + r.name())
                    .profile(prof)
                    .dbObject(obj)
                    .parentTable(r.parentTable())
                    .columnName(r.columnName())
                    .build());
        }

        resultList.getItems().setAll(hits);
        if (!hits.isEmpty()) {
            resultList.getSelectionModel().selectFirst();
            uriLabel.setText(hits.get(0).getFullUri());
        } else {
            uriLabel.setText("Type a table, column, sequence, or console name to search everywhere");
        }
    }

    private void search(String query) {
        if (query == null || query.strip().isEmpty()) {
            showRecentItems();
            return;
        }

        String q = query.strip();
        int gen = generation.incrementAndGet();
        uriLabel.setText("Searching " + currentCategory.name().toLowerCase() + "…");

        boolean includeNonProject = includeNonProjectCheck.isSelected();

        AppExecutor.run(() -> {
            List<SearchHit> hits = SearchEverywhereService.search(
                    q, currentCategory, includeNonProject, mainWindow, generation, gen, 60
            );

            if (generation.get() != gen) return;

            Platform.runLater(() -> {
                resultList.getItems().setAll(hits);
                if (!hits.isEmpty()) {
                    resultList.getSelectionModel().selectFirst();
                    uriLabel.setText(hits.get(0).getFullUri());
                } else {
                    uriLabel.setText("No results found for \"" + q + "\" in " + currentCategory.name().toLowerCase());
                }
            });
        });
    }

    private void openSelected(boolean splitRight) {
        SearchHit hit = resultList.getSelectionModel().getSelectedItem();
        if (hit == null && !resultList.getItems().isEmpty()) {
            hit = resultList.getItems().get(0);
        }
        if (hit == null) return;

        // Persist keyword and visited item
        String query = searchField.getText();
        if (query != null && !query.isBlank()) {
            SearchHistoryStore.recordSearch(query);
        }

        String profileId = hit.getProfile() != null ? hit.getProfile().getId() : null;
        String profileName = hit.getProfile() != null ? hit.getProfile().getName() : null;
        String cat = hit.getDbObject() != null ? hit.getDbObject().getCatalog() : null;
        String sch = hit.getDbObject() != null ? hit.getDbObject().getSchema() : null;

        SearchHistoryStore.recordRecentItem(new SearchHistoryStore.RecentItem(
                hit.getType().name(),
                hit.getName(),
                profileId,
                profileName,
                cat,
                sch,
                hit.getParentTable(),
                hit.getColumnName(),
                hit.getFullUri(),
                System.currentTimeMillis()
        ));

        stage.close();

        // Perform DataGrip Interactive Navigation Action
        executeNavigation(hit, splitRight);
    }

    private void executeNavigation(SearchHit hit, boolean splitRight) {
        if (mainWindow == null) return;

        switch (hit.getType()) {
            case TABLE, VIEW -> {
                ConnectionProfile prof = hit.getProfile();
                DbObject obj = hit.getDbObject();
                if (prof != null && obj != null) {
                    mainWindow.openDataTab(prof, obj);
                    if (mainWindow.getSchemaPane() != null) {
                        mainWindow.getSchemaPane().selectObjectInTree(prof, obj);
                    }
                }
            }
            case COLUMN -> {
                // If it is column, take to that table column, first row value selected
                ConnectionProfile prof = hit.getProfile();
                String colName = hit.getColumnName() != null ? hit.getColumnName() : hit.getName();
                String parentTbl = hit.getParentTable();

                DbObject tableObj = hit.getDbObject();
                if (tableObj == null || tableObj.getKind() != Kind.TABLE) {
                    String cat = hit.getProfile() != null ? hit.getProfile().getDatabase() : null;
                    tableObj = new DbObject(parentTbl, Kind.TABLE, cat, null);
                }

                if (prof != null && tableObj != null) {
                    mainWindow.openDataTab(prof, tableObj, colName);
                    if (mainWindow.getSchemaPane() != null) {
                        mainWindow.getSchemaPane().selectObjectInTree(prof, tableObj);
                    }
                }
            }
            case SEQUENCE, INDEX, PRIMARY_KEY, CONSTRAINT -> {
                ConnectionProfile prof = hit.getProfile();
                DbObject obj = hit.getDbObject();
                if (prof != null && obj != null && mainWindow.getSchemaPane() != null) {
                    mainWindow.getSchemaPane().selectObjectInTree(prof, obj);
                }
            }
            case CONSOLE -> {
                for (var pane : mainWindow.editorTabPanes()) {
                    for (var tab : pane.getTabs()) {
                        if (tab.getText() != null && tab.getText().equalsIgnoreCase(hit.getName())) {
                            mainWindow.selectTab(tab);
                            break;
                        }
                    }
                }
            }
            case FILE -> {
                if (hit.getFile() != null && hit.getFile().exists()) {
                    try {
                        String sql = Files.readString(hit.getFile().toPath());
                        var profs = com.roze.dbnavigator.db.ConnectionStore.load();
                        ConnectionProfile p = !profs.isEmpty() ? profs.get(0) : null;
                        mainWindow.openQueryTab(p, null, sql);
                    } catch (Exception ignored) {}
                }
            }
            case ACTION -> {
                if (hit.getAction() != null) {
                    try {
                        hit.getAction().perform(mainWindow);
                    } catch (Exception ignored) {}
                }
            }
            case TEXT -> {
                // Find open tab and select it
                for (var pane : mainWindow.editorTabPanes()) {
                    for (var tab : pane.getTabs()) {
                        if (tab instanceof QueryTab qt) {
                            if (hit.getLineSnippet() != null && qt.getSqlText() != null
                                    && qt.getSqlText().contains(hit.getLineSnippet())) {
                                mainWindow.selectTab(qt);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (splitRight) {
            Platform.runLater(mainWindow::splitActiveTabRight);
        }
    }
}

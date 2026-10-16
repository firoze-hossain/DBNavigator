package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataGrip-style "Modify Table":
 * Dynamic master-detail object hierarchy tree (Columns, Keys, Foreign Keys, Indexes,
 * Checks, Virtual Columns, Virtual Foreign Keys), dynamic property form pane,
 * left toolbar with Add (+), Remove (-), Move Up (^), Move Down (v), Toggle Sort (T),
 * history navigation (< >), live SQL preview pane with collapsible toggle and popout,
 * and dialect-aware ALTER TABLE statement generation and execution.
 */
public final class ModifyTableDialog {

    // =========================================================================
    // Backward-compatibility wrapper for any legacy callers/tests
    // =========================================================================
    public static class ColumnRow {
        public final SimpleStringProperty originalName;
        public final SimpleStringProperty name;
        public final SimpleStringProperty type;
        public final String originalType;
        public final SimpleBooleanProperty nullable;
        public final SimpleBooleanProperty primaryKey;
        public final SimpleBooleanProperty markedForDrop = new SimpleBooleanProperty(false);
        public final boolean isNew;

        public ColumnRow(String originalName, String name, String type, boolean nullable, boolean pk, boolean isNew) {
            this.originalName = new SimpleStringProperty(originalName);
            this.name = new SimpleStringProperty(name);
            this.type = new SimpleStringProperty(type);
            this.originalType = type;
            this.nullable = new SimpleBooleanProperty(nullable);
            this.primaryKey = new SimpleBooleanProperty(pk);
            this.isNew = isNew;
        }
    }

    // =========================================================================
    // Rich Model Classes
    // =========================================================================
    public static class ColumnModel {
        public final String originalName;
        public final SimpleStringProperty name;
        public final String originalType;
        public final SimpleStringProperty type;
        public final boolean originalNullable;
        public final SimpleBooleanProperty nullable;
        public final String originalDefaultValue;
        public final SimpleStringProperty defaultValue;
        public final SimpleStringProperty comment;
        public final boolean originalPrimaryKey;
        public final SimpleBooleanProperty primaryKey;
        public final SimpleBooleanProperty autoIncrement;
        public final SimpleStringProperty collation;
        public final SimpleStringProperty generatedExpr;
        public final boolean isNew;
        public final SimpleBooleanProperty markedForDrop = new SimpleBooleanProperty(false);
        public final SimpleBooleanProperty identity = new SimpleBooleanProperty(false);
        public final SimpleStringProperty identityMin = new SimpleStringProperty("");
        public final SimpleStringProperty identityMax = new SimpleStringProperty("");
        public final SimpleStringProperty identityStep = new SimpleStringProperty("");
        public final SimpleStringProperty identityCache = new SimpleStringProperty("");
        public final SimpleBooleanProperty identityCycled = new SimpleBooleanProperty(false);

        public ColumnModel(String originalName, String name, String type, boolean nullable,
                           String defaultValue, boolean pk, boolean autoIncrement, boolean isNew) {
            this.originalName = originalName;
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.originalType = type != null ? type : "varchar(255)";
            this.type = new SimpleStringProperty(this.originalType);
            this.originalNullable = nullable;
            this.nullable = new SimpleBooleanProperty(nullable);
            this.originalDefaultValue = defaultValue != null ? defaultValue : "";
            this.defaultValue = new SimpleStringProperty(this.originalDefaultValue);
            this.comment = new SimpleStringProperty("");
            this.originalPrimaryKey = pk;
            this.primaryKey = new SimpleBooleanProperty(pk);
            this.autoIncrement = new SimpleBooleanProperty(autoIncrement);
            this.collation = new SimpleStringProperty("");
            this.generatedExpr = new SimpleStringProperty("");
            this.isNew = isNew;
        }
    }

    public static class KeyModel {
        public final String originalName;
        public final SimpleStringProperty name;
        public final SimpleStringProperty type; // "PRIMARY KEY" or "UNIQUE"
        public final ObservableList<String> columns = FXCollections.observableArrayList();
        public final SimpleStringProperty comment = new SimpleStringProperty("");
        public final boolean isNew;
        public final SimpleBooleanProperty markedForDrop = new SimpleBooleanProperty(false);

        public KeyModel(String originalName, String name, String type, List<String> columns, boolean isNew) {
            this.originalName = originalName;
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.type = new SimpleStringProperty(type != null ? type : "PRIMARY KEY");
            if (columns != null) this.columns.addAll(columns);
            this.isNew = isNew;
        }
    }

    public static class ForeignKeyModel {
        public final String originalName;
        public final SimpleStringProperty name;
        public final SimpleStringProperty targetTable;
        public final SimpleStringProperty localColumn;
        public final SimpleStringProperty referencedColumn;
        public final SimpleStringProperty onUpdate;
        public final SimpleStringProperty onDelete;
        public final SimpleStringProperty comment = new SimpleStringProperty("");
        public final boolean isNew;
        public final SimpleBooleanProperty markedForDrop = new SimpleBooleanProperty(false);

        public ForeignKeyModel(String originalName, String name, String targetTable,
                               String localColumn, String referencedColumn, boolean isNew) {
            this.originalName = originalName;
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.targetTable = new SimpleStringProperty(targetTable != null ? targetTable : "");
            this.localColumn = new SimpleStringProperty(localColumn != null ? localColumn : "");
            this.referencedColumn = new SimpleStringProperty(referencedColumn != null ? referencedColumn : "");
            this.onUpdate = new SimpleStringProperty("NO ACTION");
            this.onDelete = new SimpleStringProperty("NO ACTION");
            this.isNew = isNew;
        }
    }

    public static class IndexModel {
        public final String originalName;
        public final SimpleStringProperty name;
        public final SimpleBooleanProperty unique;
        public final SimpleStringProperty method; // BTREE, HASH, GIN, GIST, BRIN
        public final ObservableList<String> columns = FXCollections.observableArrayList();
        public final SimpleStringProperty predicate = new SimpleStringProperty("");
        public final boolean isNew;
        public final SimpleBooleanProperty markedForDrop = new SimpleBooleanProperty(false);

        public IndexModel(String originalName, String name, boolean unique, String method, List<String> columns, boolean isNew) {
            this.originalName = originalName;
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.unique = new SimpleBooleanProperty(unique);
            this.method = new SimpleStringProperty(method != null ? method : "BTREE");
            if (columns != null) this.columns.addAll(columns);
            this.isNew = isNew;
        }
    }

    public static class CheckModel {
        public final String originalName;
        public final SimpleStringProperty name;
        public final SimpleStringProperty expression;
        public final boolean isNew;
        public final SimpleBooleanProperty markedForDrop = new SimpleBooleanProperty(false);

        public CheckModel(String originalName, String name, String expression, boolean isNew) {
            this.originalName = originalName;
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.expression = new SimpleStringProperty(expression != null ? expression : "");
            this.isNew = isNew;
        }
    }

    public static class VirtualColumnModel {
        public final SimpleStringProperty name;
        public final SimpleStringProperty expression;
        public final boolean isNew;
        public final SimpleBooleanProperty markedForDrop = new SimpleBooleanProperty(false);

        public VirtualColumnModel(String name, String expression, boolean isNew) {
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.expression = new SimpleStringProperty(expression != null ? expression : "");
            this.isNew = isNew;
        }
    }

    public static class VirtualForeignKeyModel {
        public final SimpleStringProperty name;
        public final SimpleStringProperty targetTable;
        public final SimpleStringProperty localColumn;
        public final SimpleStringProperty referencedColumn;
        public final boolean isNew;
        public final SimpleBooleanProperty markedForDrop = new SimpleBooleanProperty(false);

        public VirtualForeignKeyModel(String name, String targetTable, String localColumn, String referencedColumn, boolean isNew) {
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.targetTable = new SimpleStringProperty(targetTable != null ? targetTable : "");
            this.localColumn = new SimpleStringProperty(localColumn != null ? localColumn : "");
            this.referencedColumn = new SimpleStringProperty(referencedColumn != null ? referencedColumn : "");
            this.isNew = isNew;
        }
    }

    public static class GrantRow {
        public final SimpleStringProperty grantee;
        public final SimpleStringProperty privileges;
        public final boolean isNew;
        public final SimpleBooleanProperty markedForDrop = new SimpleBooleanProperty(false);

        public GrantRow(String grantee, String privileges, boolean isNew) {
            this.grantee = new SimpleStringProperty(grantee != null ? grantee : "");
            this.privileges = new SimpleStringProperty(privileges != null ? privileges : "ALL");
            this.isNew = isNew;
        }
    }

    public static class TableMeta {
        public final String originalName;
        public final SimpleStringProperty name;
        public final SimpleStringProperty comment = new SimpleStringProperty("");
        public final SimpleStringProperty persistence = new SimpleStringProperty("PERSISTENT");
        public final SimpleBooleanProperty withOids = new SimpleBooleanProperty(false);
        public final SimpleStringProperty partitionExpression = new SimpleStringProperty("");
        public final SimpleStringProperty partitionKey = new SimpleStringProperty("");
        public final SimpleStringProperty options = new SimpleStringProperty("");
        public final SimpleStringProperty accessMethod = new SimpleStringProperty("heap");
        public final SimpleStringProperty tablespace = new SimpleStringProperty("<default>");
        public final SimpleStringProperty owner = new SimpleStringProperty("postgres");
        public final ObservableList<GrantRow> grants = FXCollections.observableArrayList();

        public TableMeta(String originalName) {
            this.originalName = originalName != null ? originalName : "";
            this.name = new SimpleStringProperty(this.originalName);
        }
    }

    // =========================================================================
    // Tree Node Types
    // =========================================================================
    public enum NodeType {
        TABLE_ROOT,
        CATEGORY_COLUMNS,
        CATEGORY_KEYS,
        CATEGORY_FOREIGN_KEYS,
        CATEGORY_INDEXES,
        CATEGORY_CHECKS,
        CATEGORY_VIRTUAL_COLUMNS,
        CATEGORY_VIRTUAL_FKS,
        ITEM_COLUMN,
        ITEM_KEY,
        ITEM_FOREIGN_KEY,
        ITEM_INDEX,
        ITEM_CHECK,
        ITEM_VIRTUAL_COLUMN,
        ITEM_VIRTUAL_FK
    }

    public static class ModifyNode {
        public final NodeType type;
        public final Object item;
        public String title;
        public String subtitle;

        public ModifyNode(NodeType type, String title, String subtitle, Object item) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.item = item;
        }
    }

    // =========================================================================
    // Fields
    // =========================================================================
    private final MainWindow mainWindow;
    private final ConnectionProfile profile;
    private final DbObject table;
    private Stage stage;

    private final TableMeta tableMeta;
    private final ObservableList<ColumnModel> columns = FXCollections.observableArrayList();
    private final ObservableList<KeyModel> keys = FXCollections.observableArrayList();
    private final ObservableList<ForeignKeyModel> foreignKeys = FXCollections.observableArrayList();
    private final ObservableList<IndexModel> indexes = FXCollections.observableArrayList();
    private final ObservableList<CheckModel> checks = FXCollections.observableArrayList();
    private final ObservableList<VirtualColumnModel> virtualColumns = FXCollections.observableArrayList();
    private final ObservableList<VirtualForeignKeyModel> virtualForeignKeys = FXCollections.observableArrayList();

    private final ObservableList<String> availableRoles = FXCollections.observableArrayList();
    private final ObservableList<String> availableTables = FXCollections.observableArrayList();

    private TreeView<ModifyNode> treeView;
    private TreeItem<ModifyNode> rootTreeItem;
    private TreeItem<ModifyNode> colCategoryItem;
    private TreeItem<ModifyNode> keyCategoryItem;
    private TreeItem<ModifyNode> fkCategoryItem;
    private TreeItem<ModifyNode> idxCategoryItem;
    private TreeItem<ModifyNode> chkCategoryItem;
    private TreeItem<ModifyNode> vcolCategoryItem;
    private TreeItem<ModifyNode> vfkCategoryItem;

    private final List<TreeItem<ModifyNode>> selectionHistory = new ArrayList<>();
    private int historyIndex = -1;
    private boolean isNavigatingHistory = false;
    private boolean isAlphabeticalSort = false;

    private StackPane dynamicFormPane;
    private TextArea previewTextArea;
    private VBox previewBox;
    private Label previewToggleLabel;
    private boolean isPreviewExpanded = true;
    private Button duplicateBtn;
    private static boolean dontAskAgainOnCancel = false;

    // Header pill elements
    private HBox headerPillBox;
    private Label pillIconLabel;
    private Label pillTitleLabel;

    // Controls in forms for live sync
    private TextField tableNameField;
    private TextArea tableCommentArea;
    private ComboBox<String> tablePersistenceCombo;
    private CheckBox tableWithOidsCheck;
    private TextField tablePartitionExprField;
    private TextField tablePartitionKeyField;
    private TextField tableOptionsField;
    private ComboBox<String> tableAccessMethodCombo;
    private ComboBox<String> tableTablespaceCombo;
    private ComboBox<String> tableOwnerCombo;
    private TableView<GrantRow> grantsTable;

    // =========================================================================
    // Constructor and Entry Point
    // =========================================================================
    ModifyTableDialog(MainWindow mainWindow, ConnectionProfile profile, DbObject table) {
        this(mainWindow, profile, table, true, true);
    }

    ModifyTableDialog(MainWindow mainWindow, ConnectionProfile profile, DbObject table, boolean autoLoad) {
        this(mainWindow, profile, table, true, autoLoad);
    }

    ModifyTableDialog(MainWindow mainWindow, ConnectionProfile profile, DbObject table, boolean initUi, boolean autoLoad) {
        this.mainWindow = mainWindow;
        this.profile = profile;
        this.table = table;
        this.tableMeta = new TableMeta(table != null ? table.getName() : "table");

        if (initUi) {
            buildUi();
            if (autoLoad) {
                loadMetadata();
            }
        }
    }

    public static void show(MainWindow mainWindow, ConnectionProfile profile, DbObject table) {
        new ModifyTableDialog(mainWindow, profile, table).getStage().showAndWait();
    }

    // =========================================================================
    // UI Construction
    // =========================================================================
    private void buildUi() {
        if (stage == null) {
            stage = new Stage();
        }
        stage.initOwner(mainWindow != null ? mainWindow.getOwnerWindow() : null);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Modify");
        stage.setMinWidth(920);
        stage.setMinHeight(620);
        stage.setWidth(1040);
        stage.setHeight(720);

        BorderPane mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("modify-dialog");

        // Center splitpane: Left Tree + Right Detail Form
        SplitPane centerSplit = new SplitPane();
        centerSplit.setOrientation(Orientation.HORIZONTAL);
        centerSplit.setDividerPositions(0.34);

        VBox leftPane = buildLeftPane();
        VBox rightPane = buildRightPane();

        centerSplit.getItems().addAll(leftPane, rightPane);
        VBox.setVgrow(centerSplit, Priority.ALWAYS);

        // Bottom area: Collapsible Preview + Action buttons
        VBox bottomArea = buildBottomArea();

        VBox centerWithPreview = new VBox(centerSplit, bottomArea);
        VBox.setVgrow(centerSplit, Priority.ALWAYS);

        mainLayout.setCenter(centerWithPreview);

        Scene scene = new Scene(mainLayout);
        DialogTheme.applyToScene(scene);
        Window owner = mainWindow != null ? mainWindow.getOwnerWindow() : null;
        if (owner != null && owner.getScene() != null) {
            for (String ss : owner.getScene().getStylesheets()) {
                if (!scene.getStylesheets().contains(ss)) {
                    scene.getStylesheets().add(ss);
                }
            }
        }

        stage.setOnCloseRequest(ev -> {
            ev.consume();
            handleCancelWithWarning();
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                ev.consume();
                handleCancelWithWarning();
            }
        });

        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN), () -> {
            if (duplicateBtn != null && !duplicateBtn.isDisabled()) {
                doDuplicateSelected();
            }
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.INSERT, KeyCombination.ALT_DOWN), this::handleAddContextual);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DELETE), this::doRemoveSelected);

        stage.setScene(scene);
    }

    private VBox buildLeftPane() {
        // Top Toolbar matching DataGrip: + (Add), - (Remove), Duplicate (Ctrl+D), ^ (Up), v (Down), T (Sort), < (Back), > (Forward)
        SplitMenuButton addSplitBtn = new SplitMenuButton();
        addSplitBtn.setGraphic(Icons.of(FontAwesomeSolid.PLUS, "#57965c", 11));
        addSplitBtn.getStyleClass().addAll("tool-button", "connection-tool-btn");
        addSplitBtn.setTooltip(new Tooltip("Add (Alt+Insert)"));
        addSplitBtn.setOnAction(e -> handleAddContextual());

        MenuItem addCol = new MenuItem("Column", Icons.of(FontAwesomeSolid.COLUMNS, "#868a91", 11));
        addCol.setOnAction(e -> doAddColumn());
        MenuItem addUniqueKey = new MenuItem("Unique Key", Icons.of(FontAwesomeSolid.KEY, "#e0a44c", 11));
        addUniqueKey.setOnAction(e -> doAddKey("UNIQUE"));
        MenuItem addFk = new MenuItem("Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#4a88c7", 11));
        addFk.setOnAction(e -> doAddForeignKey());
        MenuItem addIndex = new MenuItem("Index", Icons.of(FontAwesomeSolid.BOLT, "#3592c4", 11));
        addIndex.setOnAction(e -> doAddIndex());
        MenuItem addCheck = new MenuItem("Check", Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
        addCheck.setOnAction(e -> doAddCheck());
        MenuItem addVCol = new MenuItem("Virtual Column", Icons.of(FontAwesomeSolid.COLUMNS, "#c77dbb", 11));
        addVCol.setOnAction(e -> doAddVirtualColumn());
        MenuItem addVFk = new MenuItem("Virtual Foreign Key", Icons.of(FontAwesomeSolid.KEY, "#c77dbb", 11));
        addVFk.setOnAction(e -> doAddVirtualForeignKey());

        addSplitBtn.getItems().addAll(addCol, addUniqueKey, addFk, addIndex, addCheck,
                new SeparatorMenuItem(), addVCol, addVFk);

        Button removeBtn = createToolButton(FontAwesomeSolid.MINUS, "#cc6666", "Remove / Toggle Drop (Delete)");
        removeBtn.setOnAction(e -> doRemoveSelected());

        duplicateBtn = createToolButton(FontAwesomeSolid.CLONE, "#a9b7c6", "Duplicate (Ctrl+D)");
        duplicateBtn.setDisable(true);
        duplicateBtn.setOnAction(e -> doDuplicateSelected());

        Button moveUpBtn = createToolButton(FontAwesomeSolid.ARROW_UP, "#a9b7c6", "Move Up");
        moveUpBtn.setOnAction(e -> doMoveColumn(-1));

        Button moveDownBtn = createToolButton(FontAwesomeSolid.ARROW_DOWN, "#a9b7c6", "Move Down");
        moveDownBtn.setOnAction(e -> doMoveColumn(1));

        Button sortToggleBtn = createToolButton(FontAwesomeSolid.SORT_ALPHA_DOWN, "#a9b7c6", "Sort Alphabetically / Declaration Order");
        sortToggleBtn.setOnAction(e -> {
            isAlphabeticalSort = !isAlphabeticalSort;
            rebuildTreeStructure();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = createToolButton(FontAwesomeSolid.CHEVRON_LEFT, "#a9b7c6", "Back");
        backBtn.setOnAction(e -> navigateHistory(-1));

        Button forwardBtn = createToolButton(FontAwesomeSolid.CHEVRON_RIGHT, "#a9b7c6", "Forward");
        forwardBtn.setOnAction(e -> navigateHistory(1));

        HBox toolbar = new HBox(3, addSplitBtn, removeBtn, duplicateBtn, moveUpBtn, moveDownBtn, sortToggleBtn, spacer, backBtn, forwardBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("modify-toolbar");

        // TreeView
        treeView = new TreeView<>();
        treeView.setShowRoot(true);
        treeView.getStyleClass().add("modify-tree");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(ModifyNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    HBox cellBox = new HBox(6);
                    cellBox.setAlignment(Pos.CENTER_LEFT);

                    Node icon = createIconForNode(item);
                    if (icon != null) cellBox.getChildren().add(icon);

                    Label titleLabel = new Label(item.title);

                    // Style dropped or new items
                    if (isDropped(item)) {
                        titleLabel.getStyleClass().add("modify-dropped-item");
                        titleLabel.setTextFill(Color.web("#6f7680"));
                    } else if (isNew(item)) {
                        titleLabel.getStyleClass().add("modify-new-item");
                        titleLabel.setTextFill(Color.web("#57965c"));
                    } else {
                        titleLabel.setTextFill(Color.web("#dfe1e5"));
                    }

                    cellBox.getChildren().add(titleLabel);

                    if (item.subtitle != null && !item.subtitle.isBlank()) {
                        Label subLabel = new Label(item.subtitle);
                        if (isNew(item)) {
                            subLabel.setTextFill(Color.web("#57965c"));
                        } else {
                            subLabel.setTextFill(Color.web("#868a91"));
                        }
                        subLabel.setStyle("-fx-font-size: 11px;");
                        cellBox.getChildren().add(subLabel);
                    }

                    if (isDropped(item)) {
                        Label dropBadge = new Label("[dropped]");
                        dropBadge.setTextFill(Color.web("#cc6666"));
                        dropBadge.setStyle("-fx-font-size: 10px;");
                        cellBox.getChildren().add(dropBadge);
                    }

                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                if (!isNavigatingHistory) {
                    recordHistory(newVal);
                }
                showDetailFormForNode(newVal.getValue());
                if (duplicateBtn != null) {
                    duplicateBtn.setDisable(!isDuplicable(newVal));
                }
            } else {
                if (duplicateBtn != null) {
                    duplicateBtn.setDisable(true);
                }
            }
        });

        VBox left = new VBox(toolbar, treeView);
        left.setMinWidth(280);
        left.setPrefWidth(330);
        return left;
    }

    private Node createIconForNode(ModifyNode node) {
        return switch (node.type) {
            case TABLE_ROOT -> Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 12);
            case CATEGORY_COLUMNS, CATEGORY_KEYS, CATEGORY_FOREIGN_KEYS, CATEGORY_INDEXES, CATEGORY_CHECKS ->
                    Icons.of(FontAwesomeSolid.FOLDER, "#3592c4", 12);
            case CATEGORY_VIRTUAL_COLUMNS, CATEGORY_VIRTUAL_FKS ->
                    Icons.of(FontAwesomeSolid.FOLDER, "#c77dbb", 12);
            case ITEM_COLUMN -> {
                ColumnModel c = (ColumnModel) node.item;
                yield c.primaryKey.get()
                        ? Icons.of(FontAwesomeSolid.KEY, "#e0a44c", 11)
                        : Icons.of(FontAwesomeSolid.COLUMNS, "#868a91", 11);
            }
            case ITEM_KEY -> Icons.of(FontAwesomeSolid.KEY, "#e0a44c", 11);
            case ITEM_FOREIGN_KEY -> Icons.of(FontAwesomeSolid.KEY, "#4a88c7", 11);
            case ITEM_INDEX -> Icons.of(FontAwesomeSolid.BOLT, "#3592c4", 11);
            case ITEM_CHECK -> Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11);
            case ITEM_VIRTUAL_COLUMN -> Icons.of(FontAwesomeSolid.COLUMNS, "#c77dbb", 11);
            case ITEM_VIRTUAL_FK -> Icons.of(FontAwesomeSolid.KEY, "#c77dbb", 11);
        };
    }

    private boolean isDropped(ModifyNode node) {
        if (node.item instanceof ColumnModel c) return c.markedForDrop.get();
        if (node.item instanceof KeyModel k) return k.markedForDrop.get();
        if (node.item instanceof ForeignKeyModel f) return f.markedForDrop.get();
        if (node.item instanceof IndexModel i) return i.markedForDrop.get();
        if (node.item instanceof CheckModel ch) return ch.markedForDrop.get();
        if (node.item instanceof VirtualColumnModel vc) return vc.markedForDrop.get();
        if (node.item instanceof VirtualForeignKeyModel vfk) return vfk.markedForDrop.get();
        return false;
    }

    private boolean isNew(ModifyNode node) {
        if (node.item instanceof ColumnModel c) return c.isNew;
        if (node.item instanceof KeyModel k) return k.isNew;
        if (node.item instanceof ForeignKeyModel f) return f.isNew;
        if (node.item instanceof IndexModel i) return i.isNew;
        if (node.item instanceof CheckModel ch) return ch.isNew;
        if (node.item instanceof VirtualColumnModel vc) return vc.isNew;
        if (node.item instanceof VirtualForeignKeyModel vfk) return vfk.isNew;
        return false;
    }

    private boolean isDuplicable(TreeItem<ModifyNode> item) {
        if (item == null || item.getValue() == null) return false;
        NodeType t = item.getValue().type;
        return t == NodeType.ITEM_COLUMN || t == NodeType.ITEM_KEY || t == NodeType.ITEM_FOREIGN_KEY
                || t == NodeType.ITEM_INDEX || t == NodeType.ITEM_CHECK
                || t == NodeType.ITEM_VIRTUAL_COLUMN || t == NodeType.ITEM_VIRTUAL_FK;
    }

    private VBox buildRightPane() {
        // Tab pill header at top
        pillIconLabel = new Label();
        pillTitleLabel = new Label("table");
        pillTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dfe1e5;");

        headerPillBox = new HBox(6, pillIconLabel, pillTitleLabel);
        headerPillBox.setAlignment(Pos.CENTER_LEFT);
        headerPillBox.getStyleClass().add("modify-pill");

        HBox headerContainer = new HBox(headerPillBox);
        headerContainer.setPadding(new Insets(10, 14, 6, 14));

        dynamicFormPane = new StackPane();
        VBox.setVgrow(dynamicFormPane, Priority.ALWAYS);

        VBox right = new VBox(headerContainer, dynamicFormPane);
        right.setPadding(new Insets(0));
        VBox.setVgrow(right, Priority.ALWAYS);
        return right;
    }

    private VBox buildBottomArea() {
        // Preview Header matching DataGrip: v Preview   [gear] [popout] [check]
        previewToggleLabel = new Label("▾ Preview");
        previewToggleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dfe1e5; -fx-cursor: hand;");
        previewToggleLabel.setOnMouseClicked(e -> togglePreview());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button settingsBtn = createToolButton(FontAwesomeSolid.COG, "#868a91", "Settings");
        settingsBtn.setOnAction(e -> {
            ContextMenu ctx = new ContextMenu();
            CheckMenuItem autoFmt = new CheckMenuItem("Auto-format DDL");
            autoFmt.setSelected(true);
            CheckMenuItem wrapLines = new CheckMenuItem("Wrap Lines");
            wrapLines.setSelected(true);
            wrapLines.setOnAction(ev -> previewTextArea.setWrapText(wrapLines.isSelected()));
            ctx.getItems().addAll(autoFmt, wrapLines);
            ctx.show(settingsBtn, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        Button popoutBtn = createToolButton(FontAwesomeSolid.EXTERNAL_LINK_ALT, "#868a91", "Open in Query Console");
        popoutBtn.setOnAction(e -> {
            String sql = previewTextArea.getText();
            if (mainWindow != null && sql != null && !sql.isBlank()) {
                mainWindow.openQueryTab(profile, table != null ? table.getCatalog() : null, sql);
            }
        });

        Label statusCheck = new Label();
        statusCheck.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
        statusCheck.setTooltip(new Tooltip("Valid DDL"));

        HBox previewHeader = new HBox(8, previewToggleLabel, spacer, settingsBtn, popoutBtn, statusCheck);
        previewHeader.setAlignment(Pos.CENTER_LEFT);
        previewHeader.getStyleClass().add("modify-preview-header");

        previewTextArea = new TextArea();
        previewTextArea.setEditable(false);
        previewTextArea.setPrefRowCount(7);
        previewTextArea.setWrapText(true);
        previewTextArea.getStyleClass().addAll("modify-preview-text", "process-output");

        previewBox = new VBox(previewHeader, previewTextArea);

        // Footer Bar: ? (Help) on left, OK / Cancel on right
        Button helpBtn = createToolButton(FontAwesomeSolid.QUESTION_CIRCLE, "#868a91", "Help");
        helpBtn.setOnAction(e -> {
            Alert info = new Alert(Alert.AlertType.INFORMATION,
                    "Modify Table allows you to visually inspect and alter table columns, keys, indexes, constraints, and permissions with live DDL generation.",
                    ButtonType.OK);
            DialogTheme.apply(info);
            info.initOwner(stage);
            info.showAndWait();
        });

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> handleCancelWithWarning());

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("modify-ok-btn");
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(e -> executeChanges());

        HBox footerBar = new HBox(10, helpBtn, footerSpacer, okBtn, cancelBtn);
        footerBar.setAlignment(Pos.CENTER_RIGHT);
        footerBar.setPadding(new Insets(10, 16, 12, 16));

        VBox bottom = new VBox(previewBox, footerBar);
        return bottom;
    }

    private void togglePreview() {
        isPreviewExpanded = !isPreviewExpanded;
        previewBox.getChildren().clear();
        if (isPreviewExpanded) {
            previewToggleLabel.setText("▾ Preview");
            previewBox.getChildren().addAll(previewBox.getParent() != null ? previewBox.getChildren() : List.of());
            // Re-add header and text area
            HBox header = (HBox) previewToggleLabel.getParent();
            previewBox.getChildren().addAll(header, previewTextArea);
        } else {
            previewToggleLabel.setText("▸ Preview");
            HBox header = (HBox) previewToggleLabel.getParent();
            previewBox.getChildren().add(header);
        }
    }

    private Button createToolButton(FontAwesomeSolid icon, String color, String tooltip) {
        Button b = new Button();
        b.setGraphic(Icons.of(icon, color, 11));
        b.getStyleClass().addAll("tool-button", "connection-tool-btn");
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    // =========================================================================
    // Forms Construction & Dynamic Swapping
    // =========================================================================
    private void showDetailFormForNode(ModifyNode node) {
        if (node == null) return;

        pillIconLabel.setGraphic(createIconForNode(node));
        String tableName = tableMeta.name.get();
        if (node.type == NodeType.ITEM_COLUMN) {
            pillTitleLabel.setText(tableName + "." + node.title);
        } else if (node.type == NodeType.CATEGORY_COLUMNS) {
            pillTitleLabel.setText("columns (" + tableName + ")");
        } else if (node.type == NodeType.CATEGORY_KEYS) {
            pillTitleLabel.setText("keys (" + tableName + ")");
        } else if (node.type == NodeType.CATEGORY_FOREIGN_KEYS) {
            pillTitleLabel.setText("foreign keys (" + tableName + ")");
        } else if (node.type == NodeType.CATEGORY_INDEXES) {
            pillTitleLabel.setText("indexes (" + tableName + ")");
        } else if (node.type == NodeType.CATEGORY_CHECKS) {
            pillTitleLabel.setText("checks (" + tableName + ")");
        } else {
            pillTitleLabel.setText(node.title != null ? node.title : "");
        }

        dynamicFormPane.getChildren().clear();
        switch (node.type) {
            case TABLE_ROOT, CATEGORY_KEYS, CATEGORY_FOREIGN_KEYS,
                 CATEGORY_INDEXES, CATEGORY_CHECKS, CATEGORY_VIRTUAL_COLUMNS, CATEGORY_VIRTUAL_FKS ->
                    dynamicFormPane.getChildren().add(buildTableForm());
            case CATEGORY_COLUMNS -> dynamicFormPane.getChildren().add(buildColumnsCategoryView());
            case ITEM_COLUMN -> dynamicFormPane.getChildren().add(buildColumnForm((ColumnModel) node.item));
            case ITEM_KEY -> dynamicFormPane.getChildren().add(buildKeyForm((KeyModel) node.item));
            case ITEM_FOREIGN_KEY -> dynamicFormPane.getChildren().add(buildForeignKeyForm((ForeignKeyModel) node.item));
            case ITEM_INDEX -> dynamicFormPane.getChildren().add(buildIndexForm((IndexModel) node.item));
            case ITEM_CHECK -> dynamicFormPane.getChildren().add(buildCheckForm((CheckModel) node.item));
            case ITEM_VIRTUAL_COLUMN -> dynamicFormPane.getChildren().add(buildVirtualColumnForm((VirtualColumnModel) node.item));
            case ITEM_VIRTUAL_FK -> dynamicFormPane.getChildren().add(buildVirtualFkForm((VirtualForeignKeyModel) node.item));
        }
    }

    private ScrollPane buildTableForm() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 16, 16, 16));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(130);
        col1.setPrefWidth(140);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        // 1. Name
        Label nameLbl = createFormLabel("Name");
        tableNameField = new TextField(tableMeta.name.get());
        tableNameField.textProperty().bindBidirectional(tableMeta.name);
        tableNameField.textProperty().addListener((o, ov, nv) -> {
            updateRootNodeTitle(nv);
            refreshPreview();
        });

        Button kebabBtn = new Button("…");
        kebabBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868a91; -fx-cursor: hand;");
        kebabBtn.setOnAction(e -> {
            ContextMenu ctx = new ContextMenu();
            MenuItem reset = new MenuItem("Reset to original name");
            reset.setOnAction(ev -> tableNameField.setText(tableMeta.originalName));
            ctx.getItems().add(reset);
            ctx.show(kebabBtn, javafx.geometry.Side.BOTTOM, 0, 0);
        });
        HBox nameBox = new HBox(4, tableNameField, kebabBtn);
        HBox.setHgrow(tableNameField, Priority.ALWAYS);
        grid.add(nameLbl, 0, row);
        grid.add(nameBox, 1, row++);

        // 2. Comment
        Label commentLbl = createFormLabel("Comment");
        tableCommentArea = new TextArea(tableMeta.comment.get());
        tableCommentArea.setPrefRowCount(2);
        tableCommentArea.textProperty().bindBidirectional(tableMeta.comment);
        tableCommentArea.textProperty().addListener((o, ov, nv) -> refreshPreview());
        grid.add(commentLbl, 0, row);
        grid.add(tableCommentArea, 1, row++);

        // 3. Persistence & With OIDs
        Label persLbl = createFormLabel("Persistence");
        tablePersistenceCombo = new ComboBox<>(FXCollections.observableArrayList("PERSISTENT", "UNLOGGED", "TEMPORARY"));
        tablePersistenceCombo.setValue(tableMeta.persistence.get());
        tablePersistenceCombo.valueProperty().addListener((o, ov, nv) -> {
            tableMeta.persistence.set(nv != null ? nv : "PERSISTENT");
            refreshPreview();
        });

        tableWithOidsCheck = new CheckBox("With OIDs");
        tableWithOidsCheck.selectedProperty().bindBidirectional(tableMeta.withOids);
        tableWithOidsCheck.selectedProperty().addListener((o, ov, nv) -> refreshPreview());

        HBox persBox = new HBox(16, tablePersistenceCombo, tableWithOidsCheck);
        persBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(persLbl, 0, row);
        grid.add(persBox, 1, row++);

        // 4. Partition Expression
        Label partExprLbl = createFormLabel("Partition Expression");
        tablePartitionExprField = new TextField(tableMeta.partitionExpression.get());
        tablePartitionExprField.textProperty().bindBidirectional(tableMeta.partitionExpression);
        tablePartitionExprField.textProperty().addListener((o, ov, nv) -> refreshPreview());
        grid.add(partExprLbl, 0, row);
        grid.add(tablePartitionExprField, 1, row++);

        // 5. Partition Key
        Label partKeyLbl = createFormLabel("Partition Key");
        tablePartitionKeyField = new TextField(tableMeta.partitionKey.get());
        tablePartitionKeyField.textProperty().bindBidirectional(tableMeta.partitionKey);
        tablePartitionKeyField.textProperty().addListener((o, ov, nv) -> refreshPreview());
        grid.add(partKeyLbl, 0, row);
        grid.add(tablePartitionKeyField, 1, row++);

        // 6. Options
        Label optLbl = createFormLabel("Options");
        tableOptionsField = new TextField(tableMeta.options.get());
        tableOptionsField.textProperty().bindBidirectional(tableMeta.options);
        tableOptionsField.textProperty().addListener((o, ov, nv) -> refreshPreview());
        grid.add(optLbl, 0, row);
        grid.add(tableOptionsField, 1, row++);

        // 7. Access Method
        Label accessLbl = createFormLabel("Access Method");
        tableAccessMethodCombo = new ComboBox<>(FXCollections.observableArrayList("heap"));
        tableAccessMethodCombo.setValue(tableMeta.accessMethod.get());
        tableAccessMethodCombo.valueProperty().addListener((o, ov, nv) -> {
            tableMeta.accessMethod.set(nv != null ? nv : "heap");
            refreshPreview();
        });
        tableAccessMethodCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(accessLbl, 0, row);
        grid.add(tableAccessMethodCombo, 1, row++);

        // 8. Tablespace
        Label tsLbl = createFormLabel("Tablespace");
        tableTablespaceCombo = new ComboBox<>(FXCollections.observableArrayList("<default>", "pg_default", "pg_global"));
        tableTablespaceCombo.setValue(tableMeta.tablespace.get());
        tableTablespaceCombo.valueProperty().addListener((o, ov, nv) -> {
            tableMeta.tablespace.set(nv != null ? nv : "<default>");
            refreshPreview();
        });
        tableTablespaceCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(tsLbl, 0, row);
        grid.add(tableTablespaceCombo, 1, row++);

        // 9. Owner
        Label ownerLbl = createFormLabel("Owner");
        tableOwnerCombo = new ComboBox<>(availableRoles.isEmpty()
                ? FXCollections.observableArrayList("postgres")
                : availableRoles);
        tableOwnerCombo.setValue(tableMeta.owner.get());
        tableOwnerCombo.valueProperty().addListener((o, ov, nv) -> {
            tableMeta.owner.set(nv != null ? nv : "postgres");
            refreshPreview();
        });
        tableOwnerCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(ownerLbl, 0, row);
        grid.add(tableOwnerCombo, 1, row++);

        // 10. Grants section matching DataGrip:
        Label grantsLbl = createFormLabel("Grants");

        Button addGrantBtn = createToolButton(FontAwesomeSolid.PLUS, "#57965c", "Add Grant");
        addGrantBtn.setOnAction(e -> doAddGrant());

        Button removeGrantBtn = createToolButton(FontAwesomeSolid.MINUS, "#cc6666", "Remove Grant");
        removeGrantBtn.setOnAction(e -> doRemoveGrant());

        Button upGrantBtn = createToolButton(FontAwesomeSolid.ARROW_UP, "#a9b7c6", "Move Up");
        Button downGrantBtn = createToolButton(FontAwesomeSolid.ARROW_DOWN, "#a9b7c6", "Move Down");

        HBox grantsToolbar = new HBox(4, addGrantBtn, removeGrantBtn, upGrantBtn, downGrantBtn);
        grantsToolbar.setAlignment(Pos.CENTER_LEFT);

        grantsTable = new TableView<>();
        grantsTable.setItems(tableMeta.grants);
        grantsTable.setEditable(true);
        grantsTable.setPrefHeight(130);
        grantsTable.setPlaceholder(new Label("Nothing to show"));

        TableColumn<GrantRow, String> granteeCol = new TableColumn<>("Grantee");
        granteeCol.setCellValueFactory(d -> d.getValue().grantee);
        granteeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        granteeCol.setOnEditCommit(e -> {
            e.getRowValue().grantee.set(e.getNewValue());
            refreshPreview();
        });
        granteeCol.setPrefWidth(180);

        TableColumn<GrantRow, String> privCol = new TableColumn<>("Privileges");
        privCol.setCellValueFactory(d -> d.getValue().privileges);
        privCol.setCellFactory(TextFieldTableCell.forTableColumn());
        privCol.setOnEditCommit(e -> {
            e.getRowValue().privileges.set(e.getNewValue());
            refreshPreview();
        });
        privCol.setPrefWidth(220);

        grantsTable.getColumns().addAll(granteeCol, privCol);

        VBox grantsBox = new VBox(4, grantsToolbar, grantsTable);
        VBox.setVgrow(grantsBox, Priority.ALWAYS);

        grid.add(grantsLbl, 0, row);
        grid.add(grantsBox, 1, row++);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("modify-form-scroll");
        return scroll;
    }

    private Node buildColumnsCategoryView() {
        TableView<ColumnModel> grid = new TableView<>();
        grid.getStyleClass().add("modify-columns-grid");
        grid.setItems(columns);
        VBox.setVgrow(grid, Priority.ALWAYS);

        // 1. #
        TableColumn<ColumnModel, Integer> numCol = new TableColumn<>("#");
        numCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(columns.indexOf(d.getValue()) + 1));
        numCol.setPrefWidth(38);
        numCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #868a91;");

        // 2. Name
        TableColumn<ColumnModel, String> nameCol = createGridColumn("Name", 150, d -> d.getValue().name);
        // 3. Comment
        TableColumn<ColumnModel, String> commentCol = createGridColumn("Comment", 130, d -> d.getValue().comment);
        // 4. Data Type
        TableColumn<ColumnModel, String> typeCol = createGridColumn("Data Type", 130, d -> d.getValue().type);
        // 5. Not Null
        TableColumn<ColumnModel, String> notNullCol = createGridColumn("Not Null", 90,
                d -> new ReadOnlyStringWrapper(String.valueOf(!d.getValue().nullable.get())));
        // 6. Default ..
        TableColumn<ColumnModel, String> defCol = createGridColumn("Default ..", 110,
                d -> {
                    String v = d.getValue().defaultValue.get();
                    return new ReadOnlyStringWrapper(v != null && !v.isBlank() ? v : "null");
                });
        // 7. Generate..
        TableColumn<ColumnModel, String> genCol = createGridColumn("Generate..", 110,
                d -> {
                    String v = d.getValue().generatedExpr.get();
                    return new ReadOnlyStringWrapper(v != null && !v.isBlank() ? v : "null");
                });
        // 8. Position
        TableColumn<ColumnModel, Integer> posCol = new TableColumn<>("Position");
        posCol.setGraphic(Icons.of(FontAwesomeSolid.FILTER, "#6f7680", 9));
        posCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(columns.indexOf(d.getValue()) + 1));
        posCol.setPrefWidth(80);
        posCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        grid.getColumns().addAll(numCol, nameCol, commentCol, typeCol, notNullCol, defCol, genCol, posCol);

        // Double-click row navigation to that column in tree
        grid.setRowFactory(tv -> {
            TableRow<ColumnModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ColumnModel item = row.getItem();
                    selectColumnInTree(item);
                }
            });
            return row;
        });

        VBox box = new VBox(grid);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private <T> TableColumn<ColumnModel, T> createGridColumn(String title, double width,
            javafx.util.Callback<TableColumn.CellDataFeatures<ColumnModel, T>, ObservableValue<T>> cellValueFactory) {
        TableColumn<ColumnModel, T> col = new TableColumn<>(title);
        col.setGraphic(Icons.of(FontAwesomeSolid.FILTER, "#6f7680", 9));
        col.setCellValueFactory(cellValueFactory);
        col.setPrefWidth(width);
        return col;
    }

    private void selectColumnInTree(ColumnModel target) {
        if (colCategoryItem == null || target == null || treeView == null) return;
        for (TreeItem<ModifyNode> child : colCategoryItem.getChildren()) {
            if (child.getValue() != null && child.getValue().item == target) {
                treeView.getSelectionModel().select(child);
                break;
            }
        }
    }

    private ScrollPane buildColumnForm(ColumnModel col) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 16, 16, 16));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(110);
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        // 1. Name with "…" action button
        Label nameLbl = createFormLabel("Name");
        TextField nameField = new TextField(col.name.get());
        nameField.textProperty().bindBidirectional(col.name);
        nameField.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        Button nameMoreBtn = new Button("…");
        nameMoreBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868a91; -fx-cursor: hand;");
        nameMoreBtn.setOnAction(e -> {
            if (col.originalName != null) {
                nameField.setText(col.originalName);
            }
        });
        HBox nameBox = new HBox(4, nameField, nameMoreBtn);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        grid.add(nameLbl, 0, row);
        grid.add(nameBox, 1, row++);

        // 2. Comment with expand action button
        Label commLbl = createFormLabel("Comment");
        TextField commField = new TextField(col.comment.get());
        commField.textProperty().bindBidirectional(col.comment);
        commField.textProperty().addListener((o, ov, nv) -> refreshPreview());
        Button expandCommBtn = new Button("\u2922");
        expandCommBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868a91; -fx-cursor: hand;");
        expandCommBtn.setOnAction(e -> {
            Dialog<String> d = new Dialog<>();
            d.setTitle("Comment");
            d.initOwner(stage);
            TextArea ta = new TextArea(col.comment.get());
            ta.setPrefRowCount(5);
            ta.setWrapText(true);
            d.getDialogPane().setContent(ta);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            DialogTheme.apply(d);
            d.setResultConverter(btn -> btn == ButtonType.OK ? ta.getText() : null);
            d.showAndWait().ifPresent(col.comment::set);
        });
        HBox commBox = new HBox(4, commField, expandCommBtn);
        HBox.setHgrow(commField, Priority.ALWAYS);
        grid.add(commLbl, 0, row);
        grid.add(commBox, 1, row++);

        // 3. Data Type (combo with suggestions and arrow)
        Label typeLbl = createFormLabel("Data Type");
        ComboBox<String> typeCombo = new ComboBox<>(getTypeSuggestions());
        typeCombo.setEditable(true);
        typeCombo.setValue(col.type.get());
        typeCombo.getEditor().textProperty().bindBidirectional(col.type);
        typeCombo.getEditor().textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(typeLbl, 0, row);
        grid.add(typeCombo, 1, row++);

        // 4. Not Null checkbox
        CheckBox notNullCheck = new CheckBox("Not Null");
        notNullCheck.setSelected(!col.nullable.get());
        notNullCheck.selectedProperty().addListener((o, ov, nv) -> {
            col.nullable.set(!nv);
            refreshPreview();
        });
        grid.add(new Label(""), 0, row);
        grid.add(notNullCheck, 1, row++);

        // 5. Default Expression with ">"
        Label defLbl = createFormLabel("Default Expression");
        TextField defField = new TextField(col.defaultValue.get());
        defField.textProperty().bindBidirectional(col.defaultValue);
        defField.textProperty().addListener((o, ov, nv) -> refreshPreview());
        Button defArrowBtn = new Button(">");
        defArrowBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868a91; -fx-cursor: hand;");
        HBox defBox = new HBox(4, defField, defArrowBtn);
        HBox.setHgrow(defField, Priority.ALWAYS);
        grid.add(defLbl, 0, row);
        grid.add(defBox, 1, row++);

        // 6. Generated Always As
        Label genLbl = createFormLabel("Generated Always As");
        TextField genField = new TextField(col.generatedExpr.get());
        genField.textProperty().bindBidirectional(col.generatedExpr);
        genField.textProperty().addListener((o, ov, nv) -> refreshPreview());
        grid.add(genLbl, 0, row);
        grid.add(genField, 1, row++);

        // 7. Identity & Parameters
        CheckBox identityCheck = new CheckBox("Identity");
        identityCheck.selectedProperty().bindBidirectional(col.identity);

        Label minLbl = createFormLabel("Min");
        TextField minField = new TextField(col.identityMin.get());
        minField.setPrefWidth(65);
        minField.textProperty().bindBidirectional(col.identityMin);

        Label maxLbl = createFormLabel("Max");
        TextField maxField = new TextField(col.identityMax.get());
        maxField.setPrefWidth(65);
        maxField.textProperty().bindBidirectional(col.identityMax);

        Label stepLbl = createFormLabel("Step");
        TextField stepField = new TextField(col.identityStep.get());
        stepField.setPrefWidth(65);
        stepField.textProperty().bindBidirectional(col.identityStep);

        Label cacheLbl = createFormLabel("Cache Size");
        TextField cacheField = new TextField(col.identityCache.get());
        cacheField.setPrefWidth(65);
        cacheField.textProperty().bindBidirectional(col.identityCache);

        CheckBox cycledCheck = new CheckBox("Cycled");
        cycledCheck.selectedProperty().bindBidirectional(col.identityCycled);

        minField.disableProperty().bind(identityCheck.selectedProperty().not());
        maxField.disableProperty().bind(identityCheck.selectedProperty().not());
        stepField.disableProperty().bind(identityCheck.selectedProperty().not());
        cacheField.disableProperty().bind(identityCheck.selectedProperty().not());
        cycledCheck.disableProperty().bind(identityCheck.selectedProperty().not());

        HBox identityParamsBox = new HBox(8, minLbl, minField, maxLbl, maxField, stepLbl, stepField, cacheLbl, cacheField, cycledCheck);
        identityParamsBox.setAlignment(Pos.CENTER_LEFT);

        VBox identityBox = new VBox(6, identityCheck, identityParamsBox);
        grid.add(createFormLabel("Identity"), 0, row);
        grid.add(identityBox, 1, row++);

        // 8. Other Flags: Primary Key, Auto-increment, Collation
        CheckBox pkCheck = new CheckBox("Primary Key");
        pkCheck.selectedProperty().bindBidirectional(col.primaryKey);
        pkCheck.selectedProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });

        CheckBox autoIncCheck = new CheckBox("Auto-increment");
        autoIncCheck.selectedProperty().bindBidirectional(col.autoIncrement);
        autoIncCheck.selectedProperty().addListener((o, ov, nv) -> refreshPreview());

        HBox extraFlags = new HBox(16, pkCheck, autoIncCheck);
        grid.add(createFormLabel("Extra Flags"), 0, row);
        grid.add(extraFlags, 1, row++);

        Label collLbl = createFormLabel("Collation");
        TextField collField = new TextField(col.collation.get());
        collField.textProperty().bindBidirectional(col.collation);
        collField.textProperty().addListener((o, ov, nv) -> refreshPreview());
        grid.add(collLbl, 0, row);
        grid.add(collField, 1, row++);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("modify-form-scroll");
        return scroll;
    }

    private ScrollPane buildKeyForm(KeyModel key) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 16, 16, 16));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(110);
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        // Name
        Label nameLbl = createFormLabel("Name");
        TextField nameField = new TextField(key.name.get());
        nameField.textProperty().bindBidirectional(key.name);
        nameField.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(nameLbl, 0, row);
        grid.add(nameField, 1, row++);

        // Type
        Label typeLbl = createFormLabel("Type");
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("PRIMARY KEY", "UNIQUE"));
        typeCombo.setValue(key.type.get());
        typeCombo.valueProperty().addListener((o, ov, nv) -> {
            key.type.set(nv != null ? nv : "PRIMARY KEY");
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(typeLbl, 0, row);
        grid.add(typeCombo, 1, row++);

        // Columns CheckBox list
        Label colsLbl = createFormLabel("Columns");
        VBox colsBox = new VBox(6);
        for (ColumnModel c : columns) {
            CheckBox cb = new CheckBox(c.name.get());
            cb.setSelected(key.columns.contains(c.name.get()));
            cb.selectedProperty().addListener((o, ov, nv) -> {
                if (nv) {
                    if (!key.columns.contains(c.name.get())) key.columns.add(c.name.get());
                } else {
                    key.columns.remove(c.name.get());
                }
                updateCurrentItemInTree();
                refreshPreview();
            });
            colsBox.getChildren().add(cb);
        }
        grid.add(colsLbl, 0, row);
        grid.add(colsBox, 1, row++);

        // Comment
        Label commLbl = createFormLabel("Comment");
        TextField commField = new TextField(key.comment.get());
        commField.textProperty().bindBidirectional(key.comment);
        commField.textProperty().addListener((o, ov, nv) -> refreshPreview());
        grid.add(commLbl, 0, row);
        grid.add(commField, 1, row++);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("modify-form-scroll");
        return scroll;
    }

    private ScrollPane buildForeignKeyForm(ForeignKeyModel fk) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 16, 16, 16));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(110);
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        Label nameLbl = createFormLabel("Name");
        TextField nameField = new TextField(fk.name.get());
        nameField.textProperty().bindBidirectional(fk.name);
        nameField.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(nameLbl, 0, row);
        grid.add(nameField, 1, row++);

        Label targetLbl = createFormLabel("Target Table");
        ComboBox<String> targetCombo = new ComboBox<>(availableTables);
        targetCombo.setEditable(true);
        targetCombo.setValue(fk.targetTable.get());
        targetCombo.getEditor().textProperty().bindBidirectional(fk.targetTable);
        targetCombo.getEditor().textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(targetLbl, 0, row);
        grid.add(targetCombo, 1, row++);

        Label localColLbl = createFormLabel("Local Column");
        ObservableList<String> colNames = FXCollections.observableArrayList(columns.stream().map(c -> c.name.get()).toList());
        ComboBox<String> localColCombo = new ComboBox<>(colNames);
        localColCombo.setValue(fk.localColumn.get());
        localColCombo.valueProperty().addListener((o, ov, nv) -> {
            fk.localColumn.set(nv != null ? nv : "");
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(localColLbl, 0, row);
        grid.add(localColCombo, 1, row++);

        Label refColLbl = createFormLabel("Referenced Column");
        TextField refColField = new TextField(fk.referencedColumn.get());
        refColField.textProperty().bindBidirectional(fk.referencedColumn);
        refColField.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(refColLbl, 0, row);
        grid.add(refColField, 1, row++);

        Label onUpdLbl = createFormLabel("On Update");
        ComboBox<String> onUpdCombo = new ComboBox<>(FXCollections.observableArrayList("NO ACTION", "CASCADE", "SET NULL", "SET DEFAULT", "RESTRICT"));
        onUpdCombo.setValue(fk.onUpdate.get());
        onUpdCombo.valueProperty().addListener((o, ov, nv) -> {
            fk.onUpdate.set(nv != null ? nv : "NO ACTION");
            refreshPreview();
        });
        grid.add(onUpdLbl, 0, row);
        grid.add(onUpdCombo, 1, row++);

        Label onDelLbl = createFormLabel("On Delete");
        ComboBox<String> onDelCombo = new ComboBox<>(FXCollections.observableArrayList("NO ACTION", "CASCADE", "SET NULL", "SET DEFAULT", "RESTRICT"));
        onDelCombo.setValue(fk.onDelete.get());
        onDelCombo.valueProperty().addListener((o, ov, nv) -> {
            fk.onDelete.set(nv != null ? nv : "NO ACTION");
            refreshPreview();
        });
        grid.add(onDelLbl, 0, row);
        grid.add(onDelCombo, 1, row++);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("modify-form-scroll");
        return scroll;
    }

    private ScrollPane buildIndexForm(IndexModel idx) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 16, 16, 16));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(110);
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        Label nameLbl = createFormLabel("Name");
        TextField nameField = new TextField(idx.name.get());
        nameField.textProperty().bindBidirectional(idx.name);
        nameField.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(nameLbl, 0, row);
        grid.add(nameField, 1, row++);

        CheckBox uniqueCheck = new CheckBox("Unique");
        uniqueCheck.selectedProperty().bindBidirectional(idx.unique);
        uniqueCheck.selectedProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(createFormLabel("Unique"), 0, row);
        grid.add(uniqueCheck, 1, row++);

        Label methodLbl = createFormLabel("Method");
        ComboBox<String> methodCombo = new ComboBox<>(FXCollections.observableArrayList("BTREE", "HASH", "GIN", "GIST", "BRIN"));
        methodCombo.setValue(idx.method.get());
        methodCombo.valueProperty().addListener((o, ov, nv) -> {
            idx.method.set(nv != null ? nv : "BTREE");
            refreshPreview();
        });
        grid.add(methodLbl, 0, row);
        grid.add(methodCombo, 1, row++);

        Label colsLbl = createFormLabel("Columns");
        VBox colsBox = new VBox(6);
        for (ColumnModel c : columns) {
            CheckBox cb = new CheckBox(c.name.get());
            cb.setSelected(idx.columns.contains(c.name.get()));
            cb.selectedProperty().addListener((o, ov, nv) -> {
                if (nv) {
                    if (!idx.columns.contains(c.name.get())) idx.columns.add(c.name.get());
                } else {
                    idx.columns.remove(c.name.get());
                }
                updateCurrentItemInTree();
                refreshPreview();
            });
            colsBox.getChildren().add(cb);
        }
        grid.add(colsLbl, 0, row);
        grid.add(colsBox, 1, row++);

        Label predLbl = createFormLabel("Where (Predicate)");
        TextField predField = new TextField(idx.predicate.get());
        predField.textProperty().bindBidirectional(idx.predicate);
        predField.textProperty().addListener((o, ov, nv) -> refreshPreview());
        grid.add(predLbl, 0, row);
        grid.add(predField, 1, row++);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("modify-form-scroll");
        return scroll;
    }

    private ScrollPane buildCheckForm(CheckModel chk) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 16, 16, 16));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(110);
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        Label nameLbl = createFormLabel("Name");
        TextField nameField = new TextField(chk.name.get());
        nameField.textProperty().bindBidirectional(chk.name);
        nameField.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(nameLbl, 0, row);
        grid.add(nameField, 1, row++);

        Label exprLbl = createFormLabel("Expression");
        TextArea exprArea = new TextArea(chk.expression.get());
        exprArea.setPrefRowCount(3);
        exprArea.textProperty().bindBidirectional(chk.expression);
        exprArea.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(exprLbl, 0, row);
        grid.add(exprArea, 1, row++);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("modify-form-scroll");
        return scroll;
    }

    private ScrollPane buildVirtualColumnForm(VirtualColumnModel vc) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 16, 16, 16));

        grid.add(createFormLabel("Name"), 0, 0);
        TextField nameField = new TextField(vc.name.get());
        nameField.textProperty().bindBidirectional(vc.name);
        nameField.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(nameField, 1, 0);

        grid.add(createFormLabel("Expression"), 0, 1);
        TextField exprField = new TextField(vc.expression.get());
        exprField.textProperty().bindBidirectional(vc.expression);
        exprField.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(exprField, 1, 1);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("modify-form-scroll");
        return scroll;
    }

    private ScrollPane buildVirtualFkForm(VirtualForeignKeyModel vfk) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 16, 16, 16));

        grid.add(createFormLabel("Name"), 0, 0);
        TextField nameField = new TextField(vfk.name.get());
        nameField.textProperty().bindBidirectional(vfk.name);
        nameField.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(nameField, 1, 0);

        grid.add(createFormLabel("Target Table"), 0, 1);
        TextField targetField = new TextField(vfk.targetTable.get());
        targetField.textProperty().bindBidirectional(vfk.targetTable);
        targetField.textProperty().addListener((o, ov, nv) -> {
            updateCurrentItemInTree();
            refreshPreview();
        });
        grid.add(targetField, 1, 1);

        grid.add(createFormLabel("Local Column"), 0, 2);
        TextField localField = new TextField(vfk.localColumn.get());
        localField.textProperty().bindBidirectional(vfk.localColumn);
        grid.add(localField, 1, 2);

        grid.add(createFormLabel("Referenced Column"), 0, 3);
        TextField refField = new TextField(vfk.referencedColumn.get());
        refField.textProperty().bindBidirectional(vfk.referencedColumn);
        grid.add(refField, 1, 3);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("modify-form-scroll");
        return scroll;
    }

    private Label createFormLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#868a91"));
        l.setStyle("-fx-font-size: 12px;");
        return l;
    }

    private ObservableList<String> getTypeSuggestions() {
        return FXCollections.observableArrayList(
                "bigint", "integer", "smallint", "serial", "bigserial",
                "varchar(255)", "text", "boolean", "date", "timestamp", "timestamp(6)",
                "time", "time(6)", "numeric(10,2)", "double precision", "json", "jsonb", "uuid", "bytea"
        );
    }

    // =========================================================================
    // Tree Manipulation and Sorting
    // =========================================================================
    private void rebuildTreeStructure() {
        String dbDesc = (table != null && table.getCatalog() != null ? table.getCatalog() : "db")
                + "." + (table != null && table.getSchema() != null ? table.getSchema() : "public")
                + " [" + (profile != null ? profile.getName() : "connection") + "]";

        ModifyNode rootNode = new ModifyNode(NodeType.TABLE_ROOT, tableMeta.name.get(), dbDesc, tableMeta);
        rootTreeItem = new TreeItem<>(rootNode);
        rootTreeItem.setExpanded(true);

        // Columns Category
        colCategoryItem = new TreeItem<>(new ModifyNode(NodeType.CATEGORY_COLUMNS, "columns", String.valueOf(columns.size()), null));
        colCategoryItem.setExpanded(true);

        List<ColumnModel> displayColumns = new ArrayList<>(columns);
        if (isAlphabeticalSort) {
            displayColumns.sort(Comparator.comparing(c -> c.name.get().toLowerCase()));
        }
        for (ColumnModel c : displayColumns) {
            String sub = c.type.get();
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_COLUMN, c.name.get(), sub, c));
            colCategoryItem.getChildren().add(item);
        }
        rootTreeItem.getChildren().add(colCategoryItem);

        // Keys Category
        keyCategoryItem = new TreeItem<>(new ModifyNode(NodeType.CATEGORY_KEYS, "keys", String.valueOf(keys.size()), null));
        keyCategoryItem.setExpanded(true);
        for (KeyModel k : keys) {
            String sub = "(" + String.join(", ", k.columns) + ")";
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_KEY, k.name.get(), sub, k));
            keyCategoryItem.getChildren().add(item);
        }
        rootTreeItem.getChildren().add(keyCategoryItem);

        // Foreign Keys Category
        fkCategoryItem = new TreeItem<>(new ModifyNode(NodeType.CATEGORY_FOREIGN_KEYS, "foreign keys", String.valueOf(foreignKeys.size()), null));
        fkCategoryItem.setExpanded(true);
        for (ForeignKeyModel fk : foreignKeys) {
            String sub = "(" + fk.localColumn.get() + " -> " + fk.targetTable.get() + "." + fk.referencedColumn.get() + ")";
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_FOREIGN_KEY, fk.name.get(), sub, fk));
            fkCategoryItem.getChildren().add(item);
        }
        rootTreeItem.getChildren().add(fkCategoryItem);

        // Indexes Category
        idxCategoryItem = new TreeItem<>(new ModifyNode(NodeType.CATEGORY_INDEXES, "indexes", String.valueOf(indexes.size()), null));
        idxCategoryItem.setExpanded(true);
        for (IndexModel idx : indexes) {
            String sub = "(" + String.join(", ", idx.columns) + ")" + (idx.unique.get() ? " UNIQUE" : "");
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_INDEX, idx.name.get(), sub, idx));
            idxCategoryItem.getChildren().add(item);
        }
        rootTreeItem.getChildren().add(idxCategoryItem);

        // Checks Category
        chkCategoryItem = new TreeItem<>(new ModifyNode(NodeType.CATEGORY_CHECKS, "checks", String.valueOf(checks.size()), null));
        chkCategoryItem.setExpanded(true);
        for (CheckModel chk : checks) {
            String sub = "(" + chk.expression.get() + ")";
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_CHECK, chk.name.get(), sub, chk));
            chkCategoryItem.getChildren().add(item);
        }
        rootTreeItem.getChildren().add(chkCategoryItem);

        // Virtual Columns
        vcolCategoryItem = new TreeItem<>(new ModifyNode(NodeType.CATEGORY_VIRTUAL_COLUMNS, "virtual columns", String.valueOf(virtualColumns.size()), null));
        for (VirtualColumnModel vc : virtualColumns) {
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_VIRTUAL_COLUMN, vc.name.get(), vc.expression.get(), vc));
            vcolCategoryItem.getChildren().add(item);
        }
        rootTreeItem.getChildren().add(vcolCategoryItem);

        // Virtual Foreign Keys
        vfkCategoryItem = new TreeItem<>(new ModifyNode(NodeType.CATEGORY_VIRTUAL_FKS, "virtual foreign keys", String.valueOf(virtualForeignKeys.size()), null));
        for (VirtualForeignKeyModel vfk : virtualForeignKeys) {
            String sub = "(" + vfk.localColumn.get() + " -> " + vfk.targetTable.get() + "." + vfk.referencedColumn.get() + ")";
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_VIRTUAL_FK, vfk.name.get(), sub, vfk));
            vfkCategoryItem.getChildren().add(item);
        }
        rootTreeItem.getChildren().add(vfkCategoryItem);

        if (treeView != null) {
            treeView.setRoot(rootTreeItem);
            treeView.getSelectionModel().select(rootTreeItem);
        }
    }

    private void updateRootNodeTitle(String newName) {
        if (rootTreeItem != null && rootTreeItem.getValue() != null) {
            rootTreeItem.getValue().title = newName;
            if (treeView != null) {
                treeView.refresh();
            }
        }
    }

    private void updateCurrentItemInTree() {
        if (treeView == null) return;
        TreeItem<ModifyNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null) {
            ModifyNode node = selected.getValue();
            if (node.item instanceof ColumnModel c) {
                node.title = c.name.get();
                node.subtitle = c.type.get();
            } else if (node.item instanceof KeyModel k) {
                node.title = k.name.get();
                node.subtitle = "(" + String.join(", ", k.columns) + ")";
            } else if (node.item instanceof ForeignKeyModel fk) {
                node.title = fk.name.get();
                node.subtitle = "(" + fk.localColumn.get() + " -> " + fk.targetTable.get() + "." + fk.referencedColumn.get() + ")";
            } else if (node.item instanceof IndexModel idx) {
                node.title = idx.name.get();
                node.subtitle = "(" + String.join(", ", idx.columns) + ")" + (idx.unique.get() ? " UNIQUE" : "");
            } else if (node.item instanceof CheckModel chk) {
                node.title = chk.name.get();
                node.subtitle = "(" + chk.expression.get() + ")";
            } else if (node.item instanceof VirtualColumnModel vc) {
                node.title = vc.name.get();
                node.subtitle = vc.expression.get();
            } else if (node.item instanceof VirtualForeignKeyModel vfk) {
                node.title = vfk.name.get();
                node.subtitle = "(" + vfk.localColumn.get() + " -> " + vfk.targetTable.get() + "." + vfk.referencedColumn.get() + ")";
            }
            treeView.refresh();
        }
    }

    // =========================================================================
    // History Navigation
    // =========================================================================
    private void recordHistory(TreeItem<ModifyNode> item) {
        if (item == null) return;
        // Trim forward history
        while (selectionHistory.size() > historyIndex + 1) {
            selectionHistory.remove(selectionHistory.size() - 1);
        }
        selectionHistory.add(item);
        historyIndex = selectionHistory.size() - 1;
    }

    private void navigateHistory(int delta) {
        int next = historyIndex + delta;
        if (next >= 0 && next < selectionHistory.size()) {
            isNavigatingHistory = true;
            historyIndex = next;
            TreeItem<ModifyNode> item = selectionHistory.get(historyIndex);
            treeView.getSelectionModel().select(item);
            isNavigatingHistory = false;
        }
    }

    // =========================================================================
    // Toolbar Add / Remove / Reorder Handlers
    // =========================================================================
    private void handleAddContextual() {
        TreeItem<ModifyNode> selected = treeView != null ? treeView.getSelectionModel().getSelectedItem() : null;
        if (selected == null || selected.getValue() == null) {
            doAddColumn();
            return;
        }
        NodeType type = selected.getValue().type;
        switch (type) {
            case CATEGORY_COLUMNS, ITEM_COLUMN -> doAddColumn();
            case CATEGORY_KEYS, ITEM_KEY -> doAddKey("UNIQUE");
            case CATEGORY_FOREIGN_KEYS, ITEM_FOREIGN_KEY -> doAddForeignKey();
            case CATEGORY_INDEXES, ITEM_INDEX -> doAddIndex();
            case CATEGORY_CHECKS, ITEM_CHECK -> doAddCheck();
            case CATEGORY_VIRTUAL_COLUMNS, ITEM_VIRTUAL_COLUMN -> doAddVirtualColumn();
            case CATEGORY_VIRTUAL_FKS, ITEM_VIRTUAL_FK -> doAddVirtualForeignKey();
            default -> doAddColumn();
        }
    }

    private void doAddColumn() {
        String newName = generateNextColumnName();
        ColumnModel col = new ColumnModel(null, newName, "integer", true, null, false, false, true);
        columns.add(col);
        if (colCategoryItem != null && colCategoryItem.getValue() != null) {
            colCategoryItem.getValue().subtitle = String.valueOf(columns.size());
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_COLUMN, col.name.get(), col.type.get(), col));
            colCategoryItem.getChildren().add(item);
            colCategoryItem.setExpanded(true);
            if (treeView != null) {
                treeView.getSelectionModel().select(item);
            }
        }
        refreshPreview();
    }

    private String generateNextColumnName() {
        boolean hasBase = columns.stream().anyMatch(c -> "column_name".equalsIgnoreCase(c.name.get()));
        if (!hasBase) {
            return "column_name";
        }
        int maxIndex = 1;
        Pattern p = Pattern.compile("^column_name_(\\d+)$", Pattern.CASE_INSENSITIVE);
        for (ColumnModel c : columns) {
            Matcher m = p.matcher(c.name.get());
            if (m.matches()) {
                try {
                    int idx = Integer.parseInt(m.group(1));
                    if (idx > maxIndex) {
                        maxIndex = idx;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return "column_name_" + (maxIndex + 1);
    }

    private void doDuplicateSelected() {
        TreeItem<ModifyNode> selected = treeView != null ? treeView.getSelectionModel().getSelectedItem() : null;
        if (selected == null || selected.getValue() == null) return;

        ModifyNode node = selected.getValue();
        if (node.item instanceof ColumnModel src) {
            String newName = generateDuplicateName(src.name.get());
            ColumnModel dup = new ColumnModel(null, newName, src.type.get(), src.nullable.get(),
                    src.defaultValue.get(), src.primaryKey.get(), src.autoIncrement.get(), true);
            dup.comment.set(src.comment.get());
            dup.collation.set(src.collation.get());
            dup.generatedExpr.set(src.generatedExpr.get());
            dup.identity.set(src.identity.get());
            dup.identityMin.set(src.identityMin.get());
            dup.identityMax.set(src.identityMax.get());
            dup.identityStep.set(src.identityStep.get());
            dup.identityCache.set(src.identityCache.get());
            dup.identityCycled.set(src.identityCycled.get());

            int index = columns.indexOf(src);
            if (index >= 0 && index < columns.size()) {
                columns.add(index + 1, dup);
            } else {
                columns.add(dup);
            }

            if (colCategoryItem != null) {
                colCategoryItem.getValue().subtitle = String.valueOf(columns.size());
                TreeItem<ModifyNode> newItem = new TreeItem<>(new ModifyNode(NodeType.ITEM_COLUMN, dup.name.get(), dup.type.get(), dup));
                int treeIdx = colCategoryItem.getChildren().indexOf(selected);
                if (treeIdx >= 0) {
                    colCategoryItem.getChildren().add(treeIdx + 1, newItem);
                } else {
                    colCategoryItem.getChildren().add(newItem);
                }
                if (treeView != null) {
                    treeView.getSelectionModel().select(newItem);
                }
            }
            refreshPreview();
        } else if (node.item instanceof KeyModel src) {
            String newName = generateDuplicateName(src.name.get());
            KeyModel dup = new KeyModel(null, newName, src.type.get(), new ArrayList<>(src.columns), true);
            dup.comment.set(src.comment.get());
            int idx = keys.indexOf(src);
            if (idx >= 0 && idx < keys.size()) keys.add(idx + 1, dup); else keys.add(dup);
            if (keyCategoryItem != null) {
                keyCategoryItem.getValue().subtitle = String.valueOf(keys.size());
                TreeItem<ModifyNode> newItem = new TreeItem<>(new ModifyNode(NodeType.ITEM_KEY, dup.name.get(), "(" + String.join(", ", dup.columns) + ")", dup));
                int treeIdx = keyCategoryItem.getChildren().indexOf(selected);
                if (treeIdx >= 0) keyCategoryItem.getChildren().add(treeIdx + 1, newItem); else keyCategoryItem.getChildren().add(newItem);
                if (treeView != null) treeView.getSelectionModel().select(newItem);
            }
            refreshPreview();
        } else if (node.item instanceof ForeignKeyModel src) {
            String newName = generateDuplicateName(src.name.get());
            ForeignKeyModel dup = new ForeignKeyModel(null, newName, src.targetTable.get(), src.localColumn.get(), src.referencedColumn.get(), true);
            dup.onUpdate.set(src.onUpdate.get());
            dup.onDelete.set(src.onDelete.get());
            dup.comment.set(src.comment.get());
            int idx = foreignKeys.indexOf(src);
            if (idx >= 0 && idx < foreignKeys.size()) foreignKeys.add(idx + 1, dup); else foreignKeys.add(dup);
            if (fkCategoryItem != null) {
                fkCategoryItem.getValue().subtitle = String.valueOf(foreignKeys.size());
                TreeItem<ModifyNode> newItem = new TreeItem<>(new ModifyNode(NodeType.ITEM_FOREIGN_KEY, dup.name.get(), "(" + dup.localColumn.get() + " -> " + dup.targetTable.get() + "." + dup.referencedColumn.get() + ")", dup));
                int treeIdx = fkCategoryItem.getChildren().indexOf(selected);
                if (treeIdx >= 0) fkCategoryItem.getChildren().add(treeIdx + 1, newItem); else fkCategoryItem.getChildren().add(newItem);
                if (treeView != null) treeView.getSelectionModel().select(newItem);
            }
            refreshPreview();
        } else if (node.item instanceof IndexModel src) {
            String newName = generateDuplicateName(src.name.get());
            IndexModel dup = new IndexModel(null, newName, src.unique.get(), src.method.get(), new ArrayList<>(src.columns), true);
            dup.predicate.set(src.predicate.get());
            int idx = indexes.indexOf(src);
            if (idx >= 0 && idx < indexes.size()) indexes.add(idx + 1, dup); else indexes.add(dup);
            if (idxCategoryItem != null) {
                idxCategoryItem.getValue().subtitle = String.valueOf(indexes.size());
                TreeItem<ModifyNode> newItem = new TreeItem<>(new ModifyNode(NodeType.ITEM_INDEX, dup.name.get(), "(" + String.join(", ", dup.columns) + ")" + (dup.unique.get() ? " UNIQUE" : ""), dup));
                int treeIdx = idxCategoryItem.getChildren().indexOf(selected);
                if (treeIdx >= 0) idxCategoryItem.getChildren().add(treeIdx + 1, newItem); else idxCategoryItem.getChildren().add(newItem);
                if (treeView != null) treeView.getSelectionModel().select(newItem);
            }
            refreshPreview();
        } else if (node.item instanceof CheckModel src) {
            String newName = generateDuplicateName(src.name.get());
            CheckModel dup = new CheckModel(null, newName, src.expression.get(), true);
            int idx = checks.indexOf(src);
            if (idx >= 0 && idx < checks.size()) checks.add(idx + 1, dup); else checks.add(dup);
            if (chkCategoryItem != null) {
                chkCategoryItem.getValue().subtitle = String.valueOf(checks.size());
                TreeItem<ModifyNode> newItem = new TreeItem<>(new ModifyNode(NodeType.ITEM_CHECK, dup.name.get(), "(" + dup.expression.get() + ")", dup));
                int treeIdx = chkCategoryItem.getChildren().indexOf(selected);
                if (treeIdx >= 0) chkCategoryItem.getChildren().add(treeIdx + 1, newItem); else chkCategoryItem.getChildren().add(newItem);
                if (treeView != null) treeView.getSelectionModel().select(newItem);
            }
            refreshPreview();
        }
    }

    private String generateDuplicateName(String baseName) {
        String stem;
        int nextNum = 2;
        Pattern p = Pattern.compile("^(.*?)(?:_(\\d+))?$");
        Matcher m = p.matcher(baseName != null ? baseName : "item");
        if (m.matches()) {
            stem = m.group(1);
            if (m.group(2) != null) {
                try {
                    nextNum = Integer.parseInt(m.group(2)) + 1;
                } catch (NumberFormatException ignored) {}
            }
        } else {
            stem = baseName != null ? baseName : "item";
        }

        String candidate = stem + "_" + nextNum;
        while (isColumnNameExists(candidate)) {
            nextNum++;
            candidate = stem + "_" + nextNum;
        }
        return candidate;
    }

    private boolean isColumnNameExists(String name) {
        return columns.stream().anyMatch(c -> name.equalsIgnoreCase(c.name.get()));
    }

    private void handleCancelWithWarning() {
        if (stage == null) return;
        List<String> changes = buildAlterStatements();
        if (changes.isEmpty() || dontAskAgainOnCancel) {
            stage.close();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Warning");
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(okType);
        if (okBtn != null) {
            okBtn.getStyleClass().add("modify-ok-btn");
        }

        Label warnIcon = new Label();
        warnIcon.setGraphic(Icons.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, "#e0a44c", 24));

        Label messageLabel = new Label("You're about to lose your changes");
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dfe1e5; -fx-font-weight: bold;");

        CheckBox dontAskAgainCheck = new CheckBox("Don't ask again");
        dontAskAgainCheck.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px;");

        VBox textBox = new VBox(8, messageLabel, dontAskAgainCheck);
        HBox contentBox = new HBox(14, warnIcon, textBox);
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setPadding(new Insets(16, 20, 10, 20));

        dialog.getDialogPane().setContent(contentBox);
        DialogTheme.apply(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == okType) {
            if (dontAskAgainCheck.isSelected()) {
                dontAskAgainOnCancel = true;
            }
            stage.close();
        }
    }

    private void doAddKey(String type) {
        String baseName = tableMeta.name.get() + "_" + type.toLowerCase().replace(" ", "_") + "_" + (keys.size() + 1);
        List<String> defCols = columns.isEmpty() ? List.of() : List.of(columns.get(0).name.get());
        KeyModel key = new KeyModel(null, baseName, type, defCols, true);
        keys.add(key);
        if (keyCategoryItem != null && keyCategoryItem.getValue() != null) {
            keyCategoryItem.getValue().subtitle = String.valueOf(keys.size());
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_KEY, key.name.get(), "(" + String.join(", ", key.columns) + ")", key));
            keyCategoryItem.getChildren().add(item);
            keyCategoryItem.setExpanded(true);
            if (treeView != null) {
                treeView.getSelectionModel().select(item);
            }
        }
        refreshPreview();
    }

    private void doAddForeignKey() {
        String baseName = "fk_" + tableMeta.name.get() + "_" + (foreignKeys.size() + 1);
        String firstCol = columns.isEmpty() ? "id" : columns.get(0).name.get();
        String target = availableTables.isEmpty() ? "other_table" : availableTables.get(0);
        ForeignKeyModel fk = new ForeignKeyModel(null, baseName, target, firstCol, "id", true);
        foreignKeys.add(fk);
        if (fkCategoryItem != null && fkCategoryItem.getValue() != null) {
            fkCategoryItem.getValue().subtitle = String.valueOf(foreignKeys.size());
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_FOREIGN_KEY, fk.name.get(), "(" + fk.localColumn.get() + " -> " + fk.targetTable.get() + ".id)", fk));
            fkCategoryItem.getChildren().add(item);
            fkCategoryItem.setExpanded(true);
            if (treeView != null) {
                treeView.getSelectionModel().select(item);
            }
        }
        refreshPreview();
    }

    private void doAddIndex() {
        String baseName = "idx_" + tableMeta.name.get() + "_" + (indexes.size() + 1);
        List<String> defCols = columns.isEmpty() ? List.of() : List.of(columns.get(0).name.get());
        IndexModel idx = new IndexModel(null, baseName, false, "BTREE", defCols, true);
        indexes.add(idx);
        if (idxCategoryItem != null && idxCategoryItem.getValue() != null) {
            idxCategoryItem.getValue().subtitle = String.valueOf(indexes.size());
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_INDEX, idx.name.get(), "(" + String.join(", ", idx.columns) + ")", idx));
            idxCategoryItem.getChildren().add(item);
            idxCategoryItem.setExpanded(true);
            if (treeView != null) {
                treeView.getSelectionModel().select(item);
            }
        }
        refreshPreview();
    }

    private void doAddCheck() {
        String baseName = "chk_" + tableMeta.name.get() + "_" + (checks.size() + 1);
        CheckModel chk = new CheckModel(null, baseName, "status >= 0", true);
        checks.add(chk);
        if (chkCategoryItem != null && chkCategoryItem.getValue() != null) {
            chkCategoryItem.getValue().subtitle = String.valueOf(checks.size());
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_CHECK, chk.name.get(), "(" + chk.expression.get() + ")", chk));
            chkCategoryItem.getChildren().add(item);
            chkCategoryItem.setExpanded(true);
            if (treeView != null) {
                treeView.getSelectionModel().select(item);
            }
        }
        refreshPreview();
    }

    private void doAddVirtualColumn() {
        String baseName = "vcol_" + (virtualColumns.size() + 1);
        VirtualColumnModel vc = new VirtualColumnModel(baseName, "col1 || ' ' || col2", true);
        virtualColumns.add(vc);
        if (vcolCategoryItem != null && vcolCategoryItem.getValue() != null) {
            vcolCategoryItem.getValue().subtitle = String.valueOf(virtualColumns.size());
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_VIRTUAL_COLUMN, vc.name.get(), vc.expression.get(), vc));
            vcolCategoryItem.getChildren().add(item);
            vcolCategoryItem.setExpanded(true);
            if (treeView != null) {
                treeView.getSelectionModel().select(item);
            }
        }
        refreshPreview();
    }

    private void doAddVirtualForeignKey() {
        String baseName = "vfk_" + (virtualForeignKeys.size() + 1);
        String firstCol = columns.isEmpty() ? "id" : columns.get(0).name.get();
        String target = availableTables.isEmpty() ? "other_table" : availableTables.get(0);
        VirtualForeignKeyModel vfk = new VirtualForeignKeyModel(baseName, target, firstCol, "id", true);
        virtualForeignKeys.add(vfk);
        if (vfkCategoryItem != null && vfkCategoryItem.getValue() != null) {
            vfkCategoryItem.getValue().subtitle = String.valueOf(virtualForeignKeys.size());
            TreeItem<ModifyNode> item = new TreeItem<>(new ModifyNode(NodeType.ITEM_VIRTUAL_FK, vfk.name.get(), "(" + vfk.localColumn.get() + " -> " + vfk.targetTable.get() + ".id)", vfk));
            vfkCategoryItem.getChildren().add(item);
            vfkCategoryItem.setExpanded(true);
            if (treeView != null) {
                treeView.getSelectionModel().select(item);
            }
        }
        refreshPreview();
    }

    private void doRemoveSelected() {
        TreeItem<ModifyNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;

        ModifyNode node = selected.getValue();
        if (node.item instanceof ColumnModel c) {
            if (c.isNew) {
                columns.remove(c);
                colCategoryItem.getChildren().remove(selected);
                colCategoryItem.getValue().subtitle = String.valueOf(columns.size());
            } else {
                c.markedForDrop.set(!c.markedForDrop.get());
            }
        } else if (node.item instanceof KeyModel k) {
            if (k.isNew) {
                keys.remove(k);
                keyCategoryItem.getChildren().remove(selected);
                keyCategoryItem.getValue().subtitle = String.valueOf(keys.size());
            } else {
                k.markedForDrop.set(!k.markedForDrop.get());
            }
        } else if (node.item instanceof ForeignKeyModel fk) {
            if (fk.isNew) {
                foreignKeys.remove(fk);
                fkCategoryItem.getChildren().remove(selected);
                fkCategoryItem.getValue().subtitle = String.valueOf(foreignKeys.size());
            } else {
                fk.markedForDrop.set(!fk.markedForDrop.get());
            }
        } else if (node.item instanceof IndexModel idx) {
            if (idx.isNew) {
                indexes.remove(idx);
                idxCategoryItem.getChildren().remove(selected);
                idxCategoryItem.getValue().subtitle = String.valueOf(indexes.size());
            } else {
                idx.markedForDrop.set(!idx.markedForDrop.get());
            }
        } else if (node.item instanceof CheckModel chk) {
            if (chk.isNew) {
                checks.remove(chk);
                chkCategoryItem.getChildren().remove(selected);
                chkCategoryItem.getValue().subtitle = String.valueOf(checks.size());
            } else {
                chk.markedForDrop.set(!chk.markedForDrop.get());
            }
        } else if (node.item instanceof VirtualColumnModel vc) {
            virtualColumns.remove(vc);
            vcolCategoryItem.getChildren().remove(selected);
            vcolCategoryItem.getValue().subtitle = String.valueOf(virtualColumns.size());
        } else if (node.item instanceof VirtualForeignKeyModel vfk) {
            virtualForeignKeys.remove(vfk);
            vfkCategoryItem.getChildren().remove(selected);
            vfkCategoryItem.getValue().subtitle = String.valueOf(virtualForeignKeys.size());
        }

        treeView.refresh();
        refreshPreview();
    }

    private void doMoveColumn(int delta) {
        TreeItem<ModifyNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getValue().item instanceof ColumnModel col)) return;

        int index = columns.indexOf(col);
        int target = index + delta;
        if (target >= 0 && target < columns.size()) {
            columns.remove(index);
            columns.add(target, col);

            int treeIdx = colCategoryItem.getChildren().indexOf(selected);
            colCategoryItem.getChildren().remove(treeIdx);
            colCategoryItem.getChildren().add(treeIdx + delta, selected);
            treeView.getSelectionModel().select(selected);
            refreshPreview();
        }
    }

    private void doAddGrant() {
        String role = availableRoles.isEmpty() ? "public" : availableRoles.get(0);
        GrantRow row = new GrantRow(role, "SELECT, INSERT, UPDATE, DELETE", true);
        tableMeta.grants.add(row);
        refreshPreview();
    }

    private void doRemoveGrant() {
        GrantRow selected = grantsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tableMeta.grants.remove(selected);
            refreshPreview();
        }
    }

    // =========================================================================
    // Metadata Loading
    // =========================================================================
    private void loadMetadata() {
        AppExecutor.run(() -> {
            try {
                // 1. Columns
                List<MetadataService.ColumnInfo> colInfoList = MetadataService.loadColumnInfo(profile, table);
                List<ColumnModel> loadedCols = new ArrayList<>();
                for (MetadataService.ColumnInfo c : colInfoList) {
                    loadedCols.add(new ColumnModel(c.name(), c.name(), c.typeName(), c.nullable(), c.defaultValue(), c.primaryKey(), false, false));
                }

                // 2. Primary Keys & Unique Keys
                List<String> pkList = MetadataService.loadPrimaryKeys(profile, table);
                List<KeyModel> loadedKeys = new ArrayList<>();
                if (!pkList.isEmpty()) {
                    String pkName = table.getName() + "_pkey";
                    loadedKeys.add(new KeyModel(pkName, pkName, "PRIMARY KEY", pkList, false));
                }

                // 3. Foreign Keys
                List<MetadataService.ForeignKey> fkList = MetadataService.loadRelatedForeignKeys(profile, table);
                List<ForeignKeyModel> loadedFks = new ArrayList<>();
                for (MetadataService.ForeignKey fk : fkList) {
                    if (fk.fromTable().equalsIgnoreCase(table.getName())) {
                        loadedFks.add(new ForeignKeyModel(fk.constraintName(), fk.constraintName(),
                                fk.toTable(), fk.fromColumn(), fk.toColumn(), false));
                    }
                }

                // 4. Indexes
                List<IndexModel> loadedIdxs = new ArrayList<>();
                try {
                    var idxRes = MetadataService.loadTableIndexes(profile, table);
                    Map<String, List<String>> idxMap = new LinkedHashMap<>();
                    Map<String, Boolean> idxUniqueMap = new HashMap<>();
                    Map<String, String> idxTypeMap = new HashMap<>();
                    for (List<String> row : idxRes.getRows()) {
                        String idxName = row.get(0);
                        String col = row.get(1);
                        boolean uniq = "YES".equalsIgnoreCase(row.get(2));
                        String method = row.get(3);
                        idxMap.computeIfAbsent(idxName, k -> new ArrayList<>()).add(col);
                        idxUniqueMap.put(idxName, uniq);
                        idxTypeMap.put(idxName, method);
                    }
                    for (Map.Entry<String, List<String>> entry : idxMap.entrySet()) {
                        loadedIdxs.add(new IndexModel(entry.getKey(), entry.getKey(),
                                idxUniqueMap.getOrDefault(entry.getKey(), false),
                                idxTypeMap.getOrDefault(entry.getKey(), "BTREE"),
                                entry.getValue(), false));
                    }
                } catch (Exception ignored) {}

                // 5. Roles / Users (for PostgreSQL)
                List<String> roles = new ArrayList<>();
                try {
                    List<DbObject> roleObjs = MetadataService.loadRoles(profile);
                    for (DbObject r : roleObjs) roles.add(r.getName());
                } catch (Exception ignored) {
                    roles.addAll(List.of("postgres", "public"));
                }

                // 6. Tables in schema (for FK autocomplete)
                List<String> tbls = new ArrayList<>();
                try {
                    tbls = MetadataService.listAllTables(profile, table.getCatalog());
                } catch (Exception ignored) {}

                final List<ColumnModel> fCols = loadedCols;
                final List<KeyModel> fKeys = loadedKeys;
                final List<ForeignKeyModel> fFks = loadedFks;
                final List<IndexModel> fIdxs = loadedIdxs;
                final List<String> fRoles = roles;
                final List<String> fTables = tbls;

                Platform.runLater(() -> {
                    columns.setAll(fCols);
                    keys.setAll(fKeys);
                    foreignKeys.setAll(fFks);
                    indexes.setAll(fIdxs);
                    if (!fRoles.isEmpty()) availableRoles.setAll(fRoles);
                    if (!fTables.isEmpty()) availableTables.setAll(fTables);

                    rebuildTreeStructure();
                    refreshPreview();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    previewTextArea.setText("-- Error inspecting table metadata: " + ex.getMessage());
                    rebuildTreeStructure();
                });
            }
        });
    }

    // =========================================================================
    // Dynamic SQL Generation
    // =========================================================================
    public List<String> buildAlterStatements() {
        List<String> stmts = new ArrayList<>();
        String origTable = table != null ? table.getName() : tableMeta.originalName;
        String origQualified = table != null ? table.qualifiedName() : DbObject.quote(origTable);
        boolean isPostgres = profile == null || profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL;
        boolean isMySqlFamily = profile != null && (profile.getType() == ConnectionProfile.DatabaseType.MYSQL
                || profile.getType() == ConnectionProfile.DatabaseType.MARIADB);

        // 1. Table Rename
        boolean tableRenamed = !tableMeta.originalName.equalsIgnoreCase(tableMeta.name.get());
        String currentQualified = origQualified;
        if (tableRenamed) {
            String newQuoted = DbObject.quote(tableMeta.name.get());
            stmts.add("ALTER TABLE " + origQualified + " RENAME TO " + newQuoted + ";");
            // If schema exists:
            if (table != null && table.getSchema() != null && !table.getSchema().isBlank()) {
                currentQualified = DbObject.quote(table.getSchema()) + "." + newQuoted;
            } else {
                currentQualified = newQuoted;
            }
        }

        // 2. Table Comment
        if (tableMeta.comment.get() != null && !tableMeta.comment.get().isBlank()) {
            if (isPostgres) {
                stmts.add("COMMENT ON TABLE " + currentQualified + " IS '"
                        + tableMeta.comment.get().replace("'", "''") + "';");
            } else if (isMySqlFamily) {
                stmts.add("ALTER TABLE " + currentQualified + " COMMENT = '"
                        + tableMeta.comment.get().replace("'", "''") + "';");
            }
        }

        // 3. Tablespace
        if (!"<default>".equalsIgnoreCase(tableMeta.tablespace.get())) {
            stmts.add("ALTER TABLE " + currentQualified + " SET TABLESPACE "
                    + DbObject.quote(tableMeta.tablespace.get()) + ";");
        }

        // 4. Owner
        if (isPostgres && !"postgres".equalsIgnoreCase(tableMeta.owner.get())) {
            stmts.add("ALTER TABLE " + currentQualified + " OWNER TO "
                    + DbObject.quote(tableMeta.owner.get()) + ";");
        }

        // 5. Columns
        for (ColumnModel c : columns) {
            if (c.isNew) {
                if (c.markedForDrop.get()) continue;
                String colDef = DbObject.quote(c.name.get()) + " " + c.type.get()
                        + (c.nullable.get() ? "" : " NOT NULL")
                        + (c.defaultValue.get() != null && !c.defaultValue.get().isBlank() ? " DEFAULT " + c.defaultValue.get() : "")
                        + (c.primaryKey.get() ? " PRIMARY KEY" : "");
                stmts.add("ALTER TABLE " + currentQualified + " ADD COLUMN " + colDef + ";");
                continue;
            }
            if (c.markedForDrop.get()) {
                stmts.add("ALTER TABLE " + currentQualified + " DROP COLUMN "
                        + DbObject.quote(c.originalName) + ";");
                continue;
            }

            boolean renamed = !c.originalName.equals(c.name.get());
            boolean retyped = !c.type.get().equalsIgnoreCase(c.originalType);
            boolean nullChanged = c.originalNullable != c.nullable.get();
            boolean defChanged = !c.originalDefaultValue.equals(c.defaultValue.get());
            String curColName = c.originalName;

            if (renamed) {
                stmts.add("ALTER TABLE " + currentQualified + " RENAME COLUMN "
                        + DbObject.quote(curColName) + " TO " + DbObject.quote(c.name.get()) + ";");
                curColName = c.name.get();
            }
            if (retyped) {
                if (isMySqlFamily) {
                    stmts.add("ALTER TABLE " + currentQualified + " MODIFY COLUMN "
                            + DbObject.quote(curColName) + " " + c.type.get() + ";");
                } else {
                    stmts.add("ALTER TABLE " + currentQualified + " ALTER COLUMN "
                            + DbObject.quote(curColName) + " TYPE " + c.type.get() + ";");
                }
            }
            if (nullChanged) {
                if (isPostgres) {
                    stmts.add("ALTER TABLE " + currentQualified + " ALTER COLUMN "
                            + DbObject.quote(curColName) + (c.nullable.get() ? " DROP NOT NULL;" : " SET NOT NULL;"));
                }
            }
            if (defChanged) {
                if (isPostgres) {
                    stmts.add("ALTER TABLE " + currentQualified + " ALTER COLUMN "
                            + DbObject.quote(curColName) + (c.defaultValue.get().isBlank()
                            ? " DROP DEFAULT;"
                            : " SET DEFAULT " + c.defaultValue.get() + ";"));
                }
            }
            if (c.comment.get() != null && !c.comment.get().isBlank()) {
                if (isPostgres) {
                    stmts.add("COMMENT ON COLUMN " + currentQualified + "." + DbObject.quote(curColName)
                            + " IS '" + c.comment.get().replace("'", "''") + "';");
                }
            }
        }

        // 6. Keys (Primary Key & Unique Keys)
        for (KeyModel k : keys) {
            if (k.isNew) {
                if (k.markedForDrop.get() || k.columns.isEmpty()) continue;
                String cols = String.join(", ", k.columns.stream().map(DbObject::quote).toList());
                stmts.add("ALTER TABLE " + currentQualified + " ADD CONSTRAINT "
                        + DbObject.quote(k.name.get()) + " " + k.type.get() + " (" + cols + ");");
            } else if (k.markedForDrop.get()) {
                stmts.add("ALTER TABLE " + currentQualified + " DROP CONSTRAINT "
                        + DbObject.quote(k.originalName) + ";");
            }
        }

        // 7. Foreign Keys
        for (ForeignKeyModel fk : foreignKeys) {
            if (fk.isNew) {
                if (fk.markedForDrop.get()) continue;
                stmts.add("ALTER TABLE " + currentQualified + " ADD CONSTRAINT "
                        + DbObject.quote(fk.name.get()) + " FOREIGN KEY (" + DbObject.quote(fk.localColumn.get()) + ") "
                        + "REFERENCES " + DbObject.quote(fk.targetTable.get()) + " (" + DbObject.quote(fk.referencedColumn.get()) + ") "
                        + "ON UPDATE " + fk.onUpdate.get() + " ON DELETE " + fk.onDelete.get() + ";");
            } else if (fk.markedForDrop.get()) {
                stmts.add("ALTER TABLE " + currentQualified + " DROP CONSTRAINT "
                        + DbObject.quote(fk.originalName) + ";");
            }
        }

        // 8. Indexes
        for (IndexModel idx : indexes) {
            if (idx.isNew) {
                if (idx.markedForDrop.get() || idx.columns.isEmpty()) continue;
                String cols = String.join(", ", idx.columns.stream().map(DbObject::quote).toList());
                String whereClause = idx.predicate.get().isBlank() ? "" : " WHERE " + idx.predicate.get();
                stmts.add("CREATE " + (idx.unique.get() ? "UNIQUE " : "") + "INDEX "
                        + DbObject.quote(idx.name.get()) + " ON " + currentQualified
                        + (isPostgres ? " USING " + idx.method.get() : "")
                        + " (" + cols + ")" + whereClause + ";");
            } else if (idx.markedForDrop.get()) {
                stmts.add(isPostgres
                        ? "DROP INDEX " + DbObject.quote(idx.originalName) + ";"
                        : "DROP INDEX " + DbObject.quote(idx.originalName) + " ON " + currentQualified + ";");
            }
        }

        // 9. Checks
        for (CheckModel chk : checks) {
            if (chk.isNew) {
                if (chk.markedForDrop.get() || chk.expression.get().isBlank()) continue;
                stmts.add("ALTER TABLE " + currentQualified + " ADD CONSTRAINT "
                        + DbObject.quote(chk.name.get()) + " CHECK (" + chk.expression.get() + ");");
            } else if (chk.markedForDrop.get()) {
                stmts.add("ALTER TABLE " + currentQualified + " DROP CONSTRAINT "
                        + DbObject.quote(chk.originalName) + ";");
            }
        }

        // 10. Grants
        for (GrantRow gr : tableMeta.grants) {
            if (gr.isNew) {
                if (gr.markedForDrop.get() || gr.grantee.get().isBlank()) continue;
                stmts.add("GRANT " + gr.privileges.get() + " ON " + currentQualified + " TO "
                        + DbObject.quote(gr.grantee.get()) + ";");
            } else if (gr.markedForDrop.get()) {
                stmts.add("REVOKE " + gr.privileges.get() + " ON " + currentQualified + " FROM "
                        + DbObject.quote(gr.grantee.get()) + ";");
            }
        }

        return stmts;
    }

    private void refreshPreview() {
        if (previewTextArea == null) return;
        List<String> statements = buildAlterStatements();
        previewTextArea.setText(formatStatementsForPreview(statements));
    }

    public static String formatStatementsForPreview(List<String> statements) {
        if (statements == null || statements.isEmpty()) {
            return "-- No changes yet \u2014 edit name, add/drop columns, constraints, or permissions";
        }
        List<String> formatted = new ArrayList<>();
        for (String sql : statements) {
            formatted.add(formatSingleStatement(sql));
        }
        return String.join("\n\n", formatted);
    }

    private static String formatSingleStatement(String sql) {
        if (sql == null || sql.isBlank()) return "";
        String trimmed = sql.trim();

        // 1. alter table <target> add constraint <def>; -> alter table <target>\n    add constraint <def>;
        Pattern pAddConstraint = Pattern.compile("(?i)^ALTER\\s+TABLE\\s+(\\S+)\\s+ADD\\s+CONSTRAINT\\s+(.*);?$");
        Matcher mConstraint = pAddConstraint.matcher(trimmed);
        if (mConstraint.matches()) {
            String tableRef = mConstraint.group(1);
            String rest = mConstraint.group(2).trim();
            if (rest.endsWith(";")) rest = rest.substring(0, rest.length() - 1);
            return "alter table " + tableRef + "\n    add constraint " + rest + ";";
        }

        // 2. alter table <target> drop constraint <def>; -> alter table <target>\n    drop constraint <def>;
        Pattern pDropConstraint = Pattern.compile("(?i)^ALTER\\s+TABLE\\s+(\\S+)\\s+DROP\\s+CONSTRAINT\\s+(.*);?$");
        Matcher mDropC = pDropConstraint.matcher(trimmed);
        if (mDropC.matches()) {
            String tableRef = mDropC.group(1);
            String rest = mDropC.group(2).trim();
            if (rest.endsWith(";")) rest = rest.substring(0, rest.length() - 1);
            return "alter table " + tableRef + "\n    drop constraint " + rest + ";";
        }

        // 3. alter table <target> add column <def>; -> alter table <target>\n    add <def>;
        Pattern pAddCol = Pattern.compile("(?i)^ALTER\\s+TABLE\\s+(\\S+)\\s+ADD(?:\\s+COLUMN)?\\s+(?!CONSTRAINT\\b)(.*);?$");
        Matcher mAdd = pAddCol.matcher(trimmed);
        if (mAdd.matches()) {
            String tableRef = mAdd.group(1);
            String colDef = mAdd.group(2).trim();
            if (colDef.endsWith(";")) colDef = colDef.substring(0, colDef.length() - 1);
            return "alter table " + tableRef + "\n    add " + colDef + ";";
        }

        // 4. alter table <target> drop column <def>; -> alter table <target>\n    drop column <def>;
        Pattern pDropCol = Pattern.compile("(?i)^ALTER\\s+TABLE\\s+(\\S+)\\s+DROP(?:\\s+COLUMN)?\\s+(?!CONSTRAINT\\b)(.*);?$");
        Matcher mDrop = pDropCol.matcher(trimmed);
        if (mDrop.matches()) {
            String tableRef = mDrop.group(1);
            String colName = mDrop.group(2).trim();
            if (colName.endsWith(";")) colName = colName.substring(0, colName.length() - 1);
            return "alter table " + tableRef + "\n    drop column " + colName + ";";
        }

        // 5. alter table <target> alter column <def>; -> alter table <target>\n    alter column <def>;
        Pattern pAlterCol = Pattern.compile("(?i)^ALTER\\s+TABLE\\s+(\\S+)\\s+ALTER(?:\\s+COLUMN)?\\s+(.*);?$");
        Matcher mAlter = pAlterCol.matcher(trimmed);
        if (mAlter.matches()) {
            String tableRef = mAlter.group(1);
            String rest = mAlter.group(2).trim();
            if (rest.endsWith(";")) rest = rest.substring(0, rest.length() - 1);
            return "alter table " + tableRef + "\n    alter column " + rest + ";";
        }

        // 6. alter table <target> rename column <old> to <new>; -> alter table <target>\n    rename column <old> to <new>;
        Pattern pRenameCol = Pattern.compile("(?i)^ALTER\\s+TABLE\\s+(\\S+)\\s+RENAME(?:\\s+COLUMN)?\\s+(.*);?$");
        Matcher mRename = pRenameCol.matcher(trimmed);
        if (mRename.matches()) {
            String tableRef = mRename.group(1);
            String rest = mRename.group(2).trim();
            if (rest.endsWith(";")) rest = rest.substring(0, rest.length() - 1);
            return "alter table " + tableRef + "\n    rename column " + rest + ";";
        }

        // 7. alter table <target> rename to <new>; -> alter table <target>\n    rename to <new>;
        Pattern pRenameTbl = Pattern.compile("(?i)^ALTER\\s+TABLE\\s+(\\S+)\\s+RENAME\\s+TO\\s+(.*);?$");
        Matcher mRenameTbl = pRenameTbl.matcher(trimmed);
        if (mRenameTbl.matches()) {
            String tableRef = mRenameTbl.group(1);
            String rest = mRenameTbl.group(2).trim();
            if (rest.endsWith(";")) rest = rest.substring(0, rest.length() - 1);
            return "alter table " + tableRef + "\n    rename to " + rest + ";";
        }

        return trimmed;
    }

    // =========================================================================
    // Execution & Progress
    // =========================================================================
    private void executeChanges() {
        List<String> statements = buildAlterStatements();
        if (statements.isEmpty()) {
            stage.close();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Execute " + statements.size() + " statement(s) to modify " + tableMeta.originalName + "?",
                ButtonType.YES, ButtonType.NO);
        DialogTheme.apply(confirm);
        confirm.initOwner(stage);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        stage.close();
        if (mainWindow == null) return;

        RunPanel.RunHandle handle = mainWindow.getRunPanel().startRun("Modify Table \u2014 " + tableMeta.originalName);
        mainWindow.showRunPanel();

        AppExecutor.run(() -> {
            try (Connection conn = ClientRegistry.jdbc(profile, table != null ? table.getCatalog() : null).getConnection()) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    for (String sql : statements) {
                        handle.appendLine(sql);
                        stmt.execute(sql);
                    }
                    conn.commit();
                } catch (Exception inner) {
                    conn.rollback();
                    throw inner;
                }
                handle.appendLine("\u2713 Successfully applied " + statements.size() + " change(s)");
                handle.markFinished(0);

                // Auto-refresh the schema tree so newly modified table reflects in Database Explorer immediately
                Platform.runLater(() -> {
                    try {
                        if (mainWindow.getSchemaPane() != null) {
                            mainWindow.getSchemaPane().refreshConnection(profile);
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception ex) {
                handle.markFailed(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            }
        });
    }

    // =========================================================================
    // Getters for programmatic inspection / unit testing
    // =========================================================================
    public TableMeta getTableMeta() { return tableMeta; }
    public ObservableList<ColumnModel> getColumns() { return columns; }
    public ObservableList<KeyModel> getKeys() { return keys; }
    public ObservableList<ForeignKeyModel> getForeignKeys() { return foreignKeys; }
    public ObservableList<IndexModel> getIndexes() { return indexes; }
    public ObservableList<CheckModel> getChecks() { return checks; }
    public ObservableList<VirtualColumnModel> getVirtualColumns() { return virtualColumns; }
    public ObservableList<VirtualForeignKeyModel> getVirtualForeignKeys() { return virtualForeignKeys; }
    public Stage getStage() { return stage; }

    public TreeView<ModifyNode> getTreeView() { return treeView; }
    public TreeItem<ModifyNode> getRootTreeItem() { return rootTreeItem; }
    public TreeItem<ModifyNode> getColCategoryItem() { return colCategoryItem; }
    public TreeItem<ModifyNode> getKeyCategoryItem() { return keyCategoryItem; }
    public TreeItem<ModifyNode> getFkCategoryItem() { return fkCategoryItem; }
    public TreeItem<ModifyNode> getIdxCategoryItem() { return idxCategoryItem; }
    public TreeItem<ModifyNode> getChkCategoryItem() { return chkCategoryItem; }
    public TreeItem<ModifyNode> getVcolCategoryItem() { return vcolCategoryItem; }
    public TreeItem<ModifyNode> getVfkCategoryItem() { return vfkCategoryItem; }
    public String getPreviewText() {
        if (previewTextArea != null) {
            return previewTextArea.getText();
        }
        List<String> statements = buildAlterStatements();
        return formatStatementsForPreview(statements);
    }
    public boolean isAlphabeticalSort() { return isAlphabeticalSort; }
    public void toggleSort() { isAlphabeticalSort = !isAlphabeticalSort; rebuildTreeStructure(); }
    public void addColumn() { doAddColumn(); }
    public void addKey(String type) { doAddKey(type); }
    public void addForeignKey() { doAddForeignKey(); }
    public void addIndex() { doAddIndex(); }
    public void addCheck() { doAddCheck(); }
    public void addVirtualColumn() { doAddVirtualColumn(); }
    public void addVirtualForeignKey() { doAddVirtualForeignKey(); }
    public void removeSelected() { doRemoveSelected(); }
    public void moveColumn(int delta) { doMoveColumn(delta); }
    public void stepHistory(int delta) { navigateHistory(delta); }
    public void refreshTreeAndPreview() { rebuildTreeStructure(); refreshPreview(); }

    public static boolean isDontAskAgainOnCancel() { return dontAskAgainOnCancel; }
    public static void setDontAskAgainOnCancel(boolean val) { dontAskAgainOnCancel = val; }
    public void duplicateSelected() { doDuplicateSelected(); }
    public void addContextual() { handleAddContextual(); }
    public void cancelWithWarning() { handleCancelWithWarning(); }
    public String generateDuplicateNameForTesting(String baseName) { return generateDuplicateName(baseName); }
    public String generateNextColumnNameForTesting() { return generateNextColumnName(); }
}

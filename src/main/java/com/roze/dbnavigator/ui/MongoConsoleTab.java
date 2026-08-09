package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.MongoDbClient;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import com.roze.dbnavigator.util.MongoShellParser;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.bson.Document;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * A real MongoDB shell console: {@code use dbname;} and
 * {@code db.collection.method(args);} statements, run against the actual
 * driver — not just a read-only filter box like {@link MongoCollectionTab}.
 *
 * Supported methods: insertOne, insertMany, find, updateOne, updateMany,
 * deleteOne, deleteMany, countDocuments, drop. Arguments are written the
 * way the real Mongo shell accepts them (unquoted keys, single or double
 * quoted strings) via {@link MongoShellParser}, not strict JSON.
 *
 * {@code find()} results support the same real column sorting, Submit/
 * Revert cell editing, and CSV/JSON export as {@link MongoCollectionTab} —
 * sorting and editing both act against whichever collection/filter the
 * most recent {@code find()} used. Running a {@code find()} also switches
 * to the Result tab automatically, so the results are immediately visible
 * rather than left behind the Output log.
 *
 * Honest scope note: this covers the common CRUD operations, not the full
 * shell language — no variables, no JS expressions/functions beyond a
 * literal document/array argument, no aggregation pipeline helpers. That
 * covers what "insert/update/delete/query data" actually needs without
 * building a full JavaScript engine.
 */
public class MongoConsoleTab extends Tab {

    private final ConnectionProfile profile;
    private final CodeArea editor = MongoShellHighlighter.createEditor();
    private final TextArea outputArea = new TextArea();
    private final ResultGrid resultGrid = new ResultGrid();
    private final MongoGridEditManager editManager;
    private final Label dbLabel = new Label();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Button submitButton = new Button("Submit");
    private final Button revertButton = new Button("Revert");
    private final TabPane bottomTabs;
    private final Tab resultTab;

    private String currentDatabase;
    private String lastFindDatabase;
    private String lastFindCollection;
    private String lastFindFilter = "{}";
    private String sortField;
    private String sortDirection;   // "ASC", "DESC", or null

    public MongoConsoleTab(ConnectionProfile profile, String initialDatabase, String title) {
        this.profile = profile;
        this.currentDatabase = initialDatabase;

        setText(title);
        setGraphic(Icons.of(FontAwesomeSolid.LEAF, "#57965c", 11));

        editor.replaceText("// MongoDB console. Ctrl+Enter runs everything below.\n"
                + "// Example: use " + (initialDatabase != null ? initialDatabase : "mydb") + ";\n"
                + "// db.collection.insertOne({ name: \"Ada\", age: 30 });\n\n");
        VirtualizedScrollPane<CodeArea> editorScroll = new VirtualizedScrollPane<>(editor);

        Button runButton = new Button("Run");
        runButton.setGraphic(Icons.of(FontAwesomeSolid.PLAY, "#57965c", 12));
        runButton.getStyleClass().add("run-button");
        runButton.setOnAction(e -> runAll());

        submitButton.getStyleClass().add("run-button");
        submitButton.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#ffffff", 11));
        revertButton.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#a9b7c6", 11));
        editManager = new MongoGridEditManager(profile, resultGrid,
                submitButton, revertButton, this::rerunLastFind, this::log);

        MenuButton exportButton = new MenuButton();
        exportButton.setGraphic(Icons.of(FontAwesomeSolid.DOWNLOAD, "#e0a44c", 11));
        exportButton.setTooltip(new Tooltip("Export the current result"));
        MenuItem exportCsv = new MenuItem("Export to CSV\u2026");
        exportCsv.setOnAction(e -> resultGrid.exportCsv());
        MenuItem exportJson = new MenuItem("Export to JSON\u2026");
        exportJson.setOnAction(e -> resultGrid.exportJson());
        exportButton.getItems().addAll(exportCsv, exportJson);

        resultGrid.setSortRequestListener((columnName, direction) -> {
            sortField = direction == null ? null : columnName;
            sortDirection = direction;
            resultGrid.setCurrentSort(columnName, direction);
            rerunLastFind();
        });

        dbLabel.getStyleClass().add("console-connection-label");
        updateDbLabel();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, runButton, new Separator(), submitButton, revertButton,
                exportButton, spacer, dbLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        outputArea.setEditable(false);
        outputArea.getStyleClass().add("process-output");
        Tab outputTab = new Tab("Output", outputArea);
        outputTab.setClosable(false);
        resultTab = new Tab("Result", resultGrid);
        resultTab.setClosable(false);
        bottomTabs = new TabPane(outputTab, resultTab);

        SplitPane split = new SplitPane(editorScroll, bottomTabs);
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        split.setDividerPositions(0.55);

        VBox root = new VBox(toolbar, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        setContent(root);

        editor.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                runAll();
                e.consume();
            }
        });
    }

    private void updateDbLabel() {
        dbLabel.setText(profile.getName() + (currentDatabase != null ? "  \u203a  " + currentDatabase : ""));
    }

    private void log(String line) {
        outputArea.appendText("[" + LocalTime.now().format(timeFormat) + "] " + line + "\n");
    }

    private void runAll() {
        String script = editor.getText();
        List<String> statements = MongoShellParser.splitStatements(script);
        if (statements.isEmpty()) return;

        AppExecutor.run(() -> {
            for (String raw : statements) {
                MongoShellParser.Statement stmt = MongoShellParser.parse(raw);
                try {
                    runOne(stmt);
                } catch (Exception ex) {
                    String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    Platform.runLater(() -> log("Error: " + msg));
                }
            }
        });
    }

    private void runOne(MongoShellParser.Statement stmt) {
        switch (stmt.kind()) {
            case USE -> {
                currentDatabase = stmt.database();
                Platform.runLater(() -> {
                    updateDbLabel();
                    log("use " + stmt.database());
                });
            }
            case COMMAND -> runCommand(stmt);
            case UNKNOWN -> Platform.runLater(() ->
                    log("Could not understand: " + shorten(stmt.raw())
                            + "  (expected \"use db;\" or \"db.collection.method(args);\")"));
        }
    }

    private void runCommand(MongoShellParser.Statement stmt) {
        if (currentDatabase == null) {
            Platform.runLater(() -> log("No database selected \u2014 run \"use <database>;\" first"));
            return;
        }
        MongoDbClient client = ClientRegistry.mongo(profile);
        String collection = stmt.collection();
        long start = System.currentTimeMillis();

        try {
            List<Document> args = parseArgs(stmt.argsJson());
            switch (stmt.method()) {
                case "insertOne" -> {
                    var result = client.insertOne(currentDatabase, collection, toJson(args, 0));
                    reportCommand(stmt, result.message(), start);
                }
                case "insertMany" -> {
                    List<Document> docs = args.size() == 1 && args.get(0).get("__array__") != null
                            ? castList(args.get(0).get("__array__")) : args;
                    var result = client.insertMany(currentDatabase, collection, docs);
                    reportCommand(stmt, result.message(), start);
                }
                case "updateOne" -> {
                    var result = client.updateOne(currentDatabase, collection, toJson(args, 0), toJson(args, 1));
                    reportCommand(stmt, result.message(), start);
                }
                case "updateMany" -> {
                    var result = client.updateMany(currentDatabase, collection, toJson(args, 0), toJson(args, 1));
                    reportCommand(stmt, result.message(), start);
                }
                case "deleteOne" -> {
                    var result = client.deleteOne(currentDatabase, collection, toJson(args, 0));
                    reportCommand(stmt, result.message(), start);
                }
                case "deleteMany" -> {
                    var result = client.deleteMany(currentDatabase, collection, toJson(args, 0));
                    reportCommand(stmt, result.message(), start);
                }
                case "countDocuments" -> {
                    long count = client.countDocuments(currentDatabase, collection, toJson(args, 0));
                    reportCommand(stmt, count + " document(s)", start);
                }
                case "drop" -> {
                    var result = client.drop(currentDatabase, collection);
                    reportCommand(stmt, result.message(), start);
                }
                case "find" -> {
                    lastFindDatabase = currentDatabase;
                    lastFindCollection = collection;
                    lastFindFilter = args.isEmpty() ? "{}" : toJson(args, 0);
                    sortField = null;
                    sortDirection = null;
                    showFindResult(start);
                }
                default -> Platform.runLater(() -> log("Unsupported method: " + stmt.method()
                        + "  (supported: insertOne, insertMany, find, updateOne, updateMany, "
                        + "deleteOne, deleteMany, countDocuments, drop)"));
            }
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            Platform.runLater(() -> log("Error in " + shorten(stmt.raw()) + ": " + msg));
        }
    }

    /** Re-runs the most recent find() — used by both the sort icon and Revert, so both act on real fresh data. */
    private void rerunLastFind() {
        if (lastFindCollection == null) return;
        AppExecutor.run(() -> showFindResult(System.currentTimeMillis()));
    }

    private void showFindResult(long start) {
        MongoDbClient client = ClientRegistry.mongo(profile);
        try {
            QueryResult result = client.find(lastFindDatabase, lastFindCollection, lastFindFilter,
                    sortField, "DESC".equals(sortDirection), 0, 500);
            Platform.runLater(() -> {
                resultGrid.setCurrentSort(sortField, sortDirection);
                editManager.configure(lastFindDatabase, lastFindCollection, result);
                resultGrid.showResult(result);
                bottomTabs.getSelectionModel().select(resultTab);
                log("db." + lastFindCollection + ".find(...) \u2192 " + result.getRows().size()
                        + " document(s) in " + (System.currentTimeMillis() - start) + " ms");
            });
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            Platform.runLater(() -> log("Error running find(): " + msg));
        }
    }

    private void reportCommand(MongoShellParser.Statement stmt, String message, long start) {
        Platform.runLater(() -> log(stmt.raw().replaceAll("\\s+", " ") + "  \u2192  " + message
                + "  (" + (System.currentTimeMillis() - start) + " ms)"));
    }

    /** Parses the JSON-array wrapper MongoShellParser produces back into individual documents. */
    private List<Document> parseArgs(String argsJsonArray) {
        if (argsJsonArray == null || argsJsonArray.isBlank()) return List.of();
        Object parsed = Document.parse("{\"__wrap__\":" + argsJsonArray + "}").get("__wrap__");
        List<Document> result = new ArrayList<>();
        if (parsed instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Document doc) {
                    result.add(doc);
                } else if (item instanceof List<?> arr) {
                    // a bare array argument (e.g. insertMany's [doc, doc]) — wrap so
                    // callers can unwrap it back out via the __array__ sentinel below
                    Document wrapper = new Document();
                    wrapper.put("__array__", arr);
                    result.add(wrapper);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Document> castList(Object raw) {
        List<Document> result = new ArrayList<>();
        for (Object item : (List<Object>) raw) {
            if (item instanceof Document doc) result.add(doc);
        }
        return result;
    }

    private static String toJson(List<Document> args, int index) {
        return index < args.size() ? args.get(index).toJson() : "{}";
    }

    private static String shorten(String s) {
        String flat = s.replaceAll("\\s+", " ").strip();
        return flat.length() > 80 ? flat.substring(0, 80) + "\u2026" : flat;
    }
}

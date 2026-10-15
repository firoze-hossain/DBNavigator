package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.db.MongoDbClient;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import com.roze.dbnavigator.model.QueryResult;
import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MongoExplorerAndNavigationTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createMongoProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("mongo-test-1");
        p.setName("studentdb@localhost");
        p.setType(DatabaseType.MONGODB);
        p.setHost("localhost");
        p.setPort(27017);
        p.setDatabase("studentdb");
        return p;
    }

    @Test
    public void testMongoDatabaseChildrenReturnsCollectionsWithDetail() throws Exception {
        ConnectionProfile profile = createMongoProfile();
        DbObject database = new DbObject("studentdb", Kind.DATABASE, "studentdb", null);

        List<DbObject> children = MetadataService.loadDatabaseChildren(profile, database);
        assertNotNull(children);
        assertEquals(1, children.size());

        DbObject collFolder = children.get(0);
        assertEquals("collections", collFolder.getName());
        assertEquals(Kind.COLLECTIONS_FOLDER, collFolder.getKind());
        assertNotNull(collFolder.getDetail());
    }

    @Test
    public void testResultGridSelectColumnAndPositionStatus() {
        ResultGrid grid = new ResultGrid();
        QueryResult qr = new QueryResult();
        qr.getColumns().addAll(List.of("_id", "address", "age", "courses"));
        qr.getRows().add(List.of("6a7873cce1a85e610c15c636", "{\"street\": \"123 Main St\"}", "22", "[\"CS\"]"));
        qr.getRows().add(List.of("6a7873cce1a85e610c15c637", "{\"street\": \"456 Oak Ave\"}", "25", "[\"Math\"]"));

        grid.showResult(qr);
        assertEquals(5, grid.getColumns().size()); // serial + 4 data cols

        // Select "address" column
        grid.selectColumn("address");

        TableColumn<List<String>, ?> addressCol = grid.getColumns().get(2);
        assertTrue(grid.getSelectionModel().isSelected(0, addressCol));
        assertEquals("1:2", grid.selectionStatusProperty().get());

        // Select "age" column (numeric value 22)
        grid.selectColumn("age");
        TableColumn<List<String>, ?> ageCol = grid.getColumns().get(3);
        assertTrue(grid.getSelectionModel().isSelected(0, ageCol));
        assertTrue(grid.selectionStatusProperty().get().contains("SUM: 22"));
        assertTrue(grid.selectionStatusProperty().get().contains("1:3"));
    }

    @Test
    public void testMongoBreadcrumbHierarchy() {
        TreeItem<DbObject> root = new TreeItem<>(new DbObject("root", Kind.MESSAGE));
        TreeItem<DbObject> connItem = new TreeItem<>(new DbObject("studentdb@localhost", Kind.CONNECTION));
        root.getChildren().add(connItem);

        TreeItem<DbObject> dbItem = new TreeItem<>(new DbObject("studentdb", Kind.DATABASE, "studentdb", null));
        connItem.getChildren().add(dbItem);

        TreeItem<DbObject> collsFolder = new TreeItem<>(new DbObject("collections", Kind.COLLECTIONS_FOLDER, "studentdb", null));
        dbItem.getChildren().add(collsFolder);

        TreeItem<DbObject> studentColl = new TreeItem<>(new DbObject("student", Kind.COLLECTION, "studentdb", null));
        collsFolder.getChildren().add(studentColl);

        TreeItem<DbObject> fieldsFolder = new TreeItem<>(new DbObject("fields", Kind.FIELDS_FOLDER, "studentdb", null));
        studentColl.getChildren().add(fieldsFolder);

        DbObject addressField = new DbObject("address", Kind.FIELD, "studentdb", null);
        addressField.setTableName("student");
        addressField.setDetail("Object");
        TreeItem<DbObject> addressItem = new TreeItem<>(addressField);
        fieldsFolder.getChildren().add(addressItem);

        assertEquals("Database > studentdb@localhost > studentdb > tables > student",
                SchemaTreePane.buildBreadcrumb(studentColl));
        assertEquals("Database > studentdb@localhost > studentdb > tables > student > fields > address",
                SchemaTreePane.buildBreadcrumb(addressItem));
    }

    @Test
    public void testMongoCollectionTabTitleAndSelectColumn() {
        ConnectionProfile profile = createMongoProfile();
        DbObject collection = new DbObject("student", Kind.COLLECTION, "studentdb", null);
        MongoCollectionTab tab = new MongoCollectionTab(profile, collection);
        assertEquals("student [studentdb@localhost]", tab.getText());
        assertDoesNotThrow(() -> tab.selectColumn("address"));
    }

    @Test
    public void testDataTabTitleAndSelectColumn() {
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("pg-1");
        profile.setName("postgres@localhost");
        profile.setType(DatabaseType.POSTGRESQL);
        DbObject table = new DbObject("users", Kind.TABLE, "postgres", "public");
        DataTab tab = new DataTab(profile, table);
        assertEquals("users", tab.getText());
        assertDoesNotThrow(() -> tab.selectColumn("id"));
    }
}

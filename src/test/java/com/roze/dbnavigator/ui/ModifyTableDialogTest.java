package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ModifyTableDialogTest {

    private ConnectionProfile createPostgresProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setName("Production-PG");
        p.setType(ConnectionProfile.DatabaseType.POSTGRESQL);
        return p;
    }

    private ConnectionProfile createMySqlProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setName("Staging-MySQL");
        p.setType(ConnectionProfile.DatabaseType.MYSQL);
        return p;
    }

    private DbObject createTable(String name, String schema, String catalog) {
        return new DbObject(name, Kind.TABLE, catalog, schema);
    }

    @Test
    @DisplayName("Verify master tree initial categories, counts, and root hierarchy")
    public void testTreeCategoriesAndHierarchy() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("users", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);
        dialog.refreshTreeAndPreview();

        TreeItem<ModifyTableDialog.ModifyNode> root = dialog.getRootTreeItem();
        assertNotNull(root, "Root tree item should not be null");
        assertEquals("users", root.getValue().title);
        assertTrue(root.getValue().subtitle.contains("store_db.public [Production-PG]"));

        // Verify the 7 standard categories
        assertEquals(7, root.getChildren().size(), "Root should have 7 child categories");
        assertEquals("columns", root.getChildren().get(0).getValue().title);
        assertEquals("keys", root.getChildren().get(1).getValue().title);
        assertEquals("foreign keys", root.getChildren().get(2).getValue().title);
        assertEquals("indexes", root.getChildren().get(3).getValue().title);
        assertEquals("checks", root.getChildren().get(4).getValue().title);
        assertEquals("virtual columns", root.getChildren().get(5).getValue().title);
        assertEquals("virtual foreign keys", root.getChildren().get(6).getValue().title);
    }

    @Test
    @DisplayName("Verify Table Rename and Table Comment DDL generation")
    public void testTableRenameAndComment() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject table = createTable("users", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, pgProfile, table, false, false);
        dialog.getTableMeta().name.set("customers");
        dialog.getTableMeta().comment.set("Registered customer accounts");

        List<String> stmts = dialog.buildAlterStatements();
        assertEquals(2, stmts.size());
        assertEquals("ALTER TABLE public.users RENAME TO customers;", stmts.get(0));
        assertEquals("COMMENT ON TABLE public.customers IS 'Registered customer accounts';", stmts.get(1));

        // MySQL dialect
        ConnectionProfile mysqlProfile = createMySqlProfile();
        DbObject myTable = createTable("users", "", "store_db");
        ModifyTableDialog myDialog = new ModifyTableDialog(null, mysqlProfile, myTable, false, false);
        myDialog.getTableMeta().comment.set("Customer info");
        List<String> myStmts = myDialog.buildAlterStatements();
        assertEquals(1, myStmts.size());
        assertEquals("ALTER TABLE store_db.users COMMENT = 'Customer info';", myStmts.get(0));
    }

    @Test
    @DisplayName("Verify Tablespace and Owner DDL generation")
    public void testTablespaceAndOwner() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject table = createTable("logs", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, pgProfile, table, false, false);
        dialog.getTableMeta().tablespace.set("fast_ssd");
        dialog.getTableMeta().owner.set("reporting_user");

        List<String> stmts = dialog.buildAlterStatements();
        assertEquals(2, stmts.size());
        assertEquals("ALTER TABLE public.logs SET TABLESPACE fast_ssd;", stmts.get(0));
        assertEquals("ALTER TABLE public.logs OWNER TO reporting_user;", stmts.get(1));
    }

    @Test
    @DisplayName("Verify Column additions, drops, renames, retyping, and defaults")
    public void testColumnDdlGeneration() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject table = createTable("orders", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, pgProfile, table, false, false);

        // 1. Existing column rename and retype
        ModifyTableDialog.ColumnModel c1 = new ModifyTableDialog.ColumnModel(
                "amt", "total_amount", "integer", true, null, false, false, false);
        c1.type.set("numeric(12,2)");
        c1.nullable.set(false);
        c1.defaultValue.set("0.00");
        c1.comment.set("Order total with tax");
        dialog.getColumns().add(c1);

        // 2. Existing column marked for drop
        ModifyTableDialog.ColumnModel c2 = new ModifyTableDialog.ColumnModel(
                "legacy_code", "legacy_code", "varchar(50)", true, null, false, false, false);
        c2.markedForDrop.set(true);
        dialog.getColumns().add(c2);

        // 3. New column added
        ModifyTableDialog.ColumnModel c3 = new ModifyTableDialog.ColumnModel(
                null, "currency", "varchar(3)", false, "'USD'", false, false, true);
        dialog.getColumns().add(c3);

        List<String> stmts = dialog.buildAlterStatements();
        assertTrue(stmts.stream().anyMatch(s -> s.contains("RENAME COLUMN amt TO total_amount;")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("ALTER COLUMN total_amount TYPE numeric(12,2);")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("ALTER COLUMN total_amount SET NOT NULL;")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("ALTER COLUMN total_amount SET DEFAULT 0.00;")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("COMMENT ON COLUMN public.orders.total_amount IS 'Order total with tax';")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("DROP COLUMN legacy_code;")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("ADD COLUMN currency varchar(3) NOT NULL DEFAULT 'USD';")));
    }

    @Test
    @DisplayName("Verify Key constraints DDL (Primary Key & Unique Key)")
    public void testKeyConstraintsDdl() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject table = createTable("items", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, pgProfile, table, false, false);

        // Add Primary Key
        ModifyTableDialog.KeyModel pk = new ModifyTableDialog.KeyModel(
                null, "pk_items", "PRIMARY KEY", List.of("id"), true);
        dialog.getKeys().add(pk);

        // Add Unique Key
        ModifyTableDialog.KeyModel uq = new ModifyTableDialog.KeyModel(
                null, "uq_items_sku", "UNIQUE", List.of("sku", "warehouse_id"), true);
        dialog.getKeys().add(uq);

        // Existing key to drop
        ModifyTableDialog.KeyModel oldKey = new ModifyTableDialog.KeyModel(
                "old_uk_idx", "old_uk_idx", "UNIQUE", List.of("legacy_id"), false);
        oldKey.markedForDrop.set(true);
        dialog.getKeys().add(oldKey);

        List<String> stmts = dialog.buildAlterStatements();
        assertTrue(stmts.stream().anyMatch(s -> s.contains("ADD CONSTRAINT pk_items PRIMARY KEY (id);")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("ADD CONSTRAINT uq_items_sku UNIQUE (sku, warehouse_id);")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("DROP CONSTRAINT old_uk_idx;")));
    }

    @Test
    @DisplayName("Verify Foreign Key DDL generation and drop")
    public void testForeignKeyDdl() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject table = createTable("order_items", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, pgProfile, table, false, false);

        // Add FK
        ModifyTableDialog.ForeignKeyModel fk = new ModifyTableDialog.ForeignKeyModel(
                null, "fk_order_items_order", "orders", "order_id", "id", true);
        fk.onUpdate.set("CASCADE");
        fk.onDelete.set("CASCADE");
        dialog.getForeignKeys().add(fk);

        // Drop existing FK
        ModifyTableDialog.ForeignKeyModel oldFk = new ModifyTableDialog.ForeignKeyModel(
                "fk_deprecated", "fk_deprecated", "legacy_table", "ref_id", "id", false);
        oldFk.markedForDrop.set(true);
        dialog.getForeignKeys().add(oldFk);

        List<String> stmts = dialog.buildAlterStatements();
        assertTrue(stmts.stream().anyMatch(s -> s.contains("ADD CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON UPDATE CASCADE ON DELETE CASCADE;")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("DROP CONSTRAINT fk_deprecated;")));
    }

    @Test
    @DisplayName("Verify Index DDL generation with dialect differences")
    public void testIndexDdl() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject pgTable = createTable("products", "public", "catalog");

        ModifyTableDialog pgDialog = new ModifyTableDialog(null, pgProfile, pgTable, false, false);

        // New B-Tree index with predicate
        ModifyTableDialog.IndexModel idx1 = new ModifyTableDialog.IndexModel(
                null, "idx_products_category", false, "BTREE", List.of("category_id", "price"), true);
        idx1.predicate.set("deleted = false");
        pgDialog.getIndexes().add(idx1);

        // Drop index in Postgres
        ModifyTableDialog.IndexModel idxOld = new ModifyTableDialog.IndexModel(
                "idx_old_title", "idx_old_title", false, "BTREE", List.of("title"), false);
        idxOld.markedForDrop.set(true);
        pgDialog.getIndexes().add(idxOld);

        List<String> pgStmts = pgDialog.buildAlterStatements();
        assertTrue(pgStmts.stream().anyMatch(s -> s.contains("CREATE INDEX idx_products_category ON public.products USING BTREE (category_id, price) WHERE deleted = false;")));
        assertTrue(pgStmts.stream().anyMatch(s -> s.contains("DROP INDEX idx_old_title;")));

        // MySQL dialect
        ConnectionProfile myProfile = createMySqlProfile();
        DbObject myTable = createTable("products", "", "catalog");
        ModifyTableDialog myDialog = new ModifyTableDialog(null, myProfile, myTable, false, false);
        ModifyTableDialog.IndexModel myOldIdx = new ModifyTableDialog.IndexModel(
                "idx_mysql_old", "idx_mysql_old", false, "BTREE", List.of("title"), false);
        myOldIdx.markedForDrop.set(true);
        myDialog.getIndexes().add(myOldIdx);

        List<String> myStmts = myDialog.buildAlterStatements();
        assertTrue(myStmts.stream().anyMatch(s -> s.contains("DROP INDEX idx_mysql_old ON catalog.products;")));
    }

    @Test
    @DisplayName("Verify Check constraints DDL")
    public void testCheckConstraintsDdl() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject table = createTable("accounts", "public", "finance");

        ModifyTableDialog dialog = new ModifyTableDialog(null, pgProfile, table, false, false);

        // New Check
        ModifyTableDialog.CheckModel chk = new ModifyTableDialog.CheckModel(
                null, "chk_balance_positive", "balance >= 0.00", true);
        dialog.getChecks().add(chk);

        // Drop Check
        ModifyTableDialog.CheckModel chkOld = new ModifyTableDialog.CheckModel(
                "chk_old_rule", "chk_old_rule", "status > 0", false);
        chkOld.markedForDrop.set(true);
        dialog.getChecks().add(chkOld);

        List<String> stmts = dialog.buildAlterStatements();
        assertTrue(stmts.stream().anyMatch(s -> s.contains("ADD CONSTRAINT chk_balance_positive CHECK (balance >= 0.00);")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("DROP CONSTRAINT chk_old_rule;")));
    }

    @Test
    @DisplayName("Verify Grant and Revoke DDL generation")
    public void testGrantsDdl() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject table = createTable("documents", "public", "content");

        ModifyTableDialog dialog = new ModifyTableDialog(null, pgProfile, table, false, false);

        // Add Grant
        ModifyTableDialog.GrantRow gr1 = new ModifyTableDialog.GrantRow("analyst_role", "SELECT", true);
        dialog.getTableMeta().grants.add(gr1);

        // Revoke Grant
        ModifyTableDialog.GrantRow gr2 = new ModifyTableDialog.GrantRow("external_partner", "SELECT, INSERT", false);
        gr2.markedForDrop.set(true);
        dialog.getTableMeta().grants.add(gr2);

        List<String> stmts = dialog.buildAlterStatements();
        assertTrue(stmts.stream().anyMatch(s -> s.contains("GRANT SELECT ON public.documents TO analyst_role;")));
        assertTrue(stmts.stream().anyMatch(s -> s.contains("REVOKE SELECT, INSERT ON public.documents FROM external_partner;")));
    }

    @Test
    @DisplayName("Verify Toolbar interactive operations: Add Column, Remove, Sort Toggle, and Tree Count Sync")
    public void testToolbarOperationsAndSorting() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject table = createTable("widgets", "public", "inventory");

        ModifyTableDialog dialog = new ModifyTableDialog(null, pgProfile, table, false, false);
        dialog.refreshTreeAndPreview();

        assertEquals(0, dialog.getColumns().size());
        assertEquals("0", dialog.getColCategoryItem().getValue().subtitle);

        // 1. Add Column via Toolbar action (DataGrip naming: column_name, integer)
        dialog.addColumn();
        assertEquals(1, dialog.getColumns().size());
        assertEquals("1", dialog.getColCategoryItem().getValue().subtitle);
        assertEquals("column_name", dialog.getColumns().get(0).name.get());
        assertEquals("integer", dialog.getColumns().get(0).type.get());

        // 2. Add Second Column (DataGrip naming: column_name_2, integer)
        dialog.addColumn();
        assertEquals(2, dialog.getColumns().size());
        assertEquals("2", dialog.getColCategoryItem().getValue().subtitle);
        assertEquals("column_name_2", dialog.getColumns().get(1).name.get());
        assertEquals("integer", dialog.getColumns().get(1).type.get());

        // 3. Test Sort Toggle
        assertFalse(dialog.isAlphabeticalSort());
        dialog.getColumns().get(0).name.set("zebra");
        dialog.getColumns().get(1).name.set("alpha");
        dialog.toggleSort();
        assertTrue(dialog.isAlphabeticalSort());

        // Under alphabetical sort, 'alpha' should appear before 'zebra' in tree children
        assertEquals("alpha", dialog.getColCategoryItem().getChildren().get(0).getValue().title);
        assertEquals("zebra", dialog.getColCategoryItem().getChildren().get(1).getValue().title);

        dialog.toggleSort();
        assertFalse(dialog.isAlphabeticalSort());
        // Under declaration order, 'zebra' is first
        assertEquals("zebra", dialog.getColCategoryItem().getChildren().get(0).getValue().title);
        assertEquals("alpha", dialog.getColCategoryItem().getChildren().get(1).getValue().title);

        // 4. Test Add Key, FK, Index, Check
        dialog.addKey("PRIMARY KEY");
        assertEquals(1, dialog.getKeys().size());
        assertEquals("1", dialog.getKeyCategoryItem().getValue().subtitle);

        dialog.addForeignKey();
        assertEquals(1, dialog.getForeignKeys().size());
        assertEquals("1", dialog.getFkCategoryItem().getValue().subtitle);

        dialog.addIndex();
        assertEquals(1, dialog.getIndexes().size());
        assertEquals("1", dialog.getIdxCategoryItem().getValue().subtitle);

        dialog.addCheck();
        assertEquals(1, dialog.getChecks().size());
        assertEquals("1", dialog.getChkCategoryItem().getValue().subtitle);

        // 5. Test Live Preview text content (formatted matching DataGrip)
        String preview = dialog.getPreviewText();
        assertNotNull(preview);
        assertFalse(preview.isBlank());
        assertTrue(preview.contains("zebra"));
        assertTrue(preview.contains("add constraint"));
    }

    @Test
    @DisplayName("Verify DataGrip column naming progression and contextual add")
    public void testDataGripColumnNamingAndContextualAdd() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject table = createTable("users", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, pgProfile, table, false, false);
        dialog.refreshTreeAndPreview();

        assertEquals("column_name", dialog.generateNextColumnNameForTesting());
        dialog.addColumn();
        assertEquals("column_name", dialog.getColumns().get(0).name.get());

        assertEquals("column_name_2", dialog.generateNextColumnNameForTesting());
        dialog.addColumn();
        assertEquals("column_name_2", dialog.getColumns().get(1).name.get());

        assertEquals("column_name_3", dialog.generateNextColumnNameForTesting());
        dialog.addColumn();
        assertEquals("column_name_3", dialog.getColumns().get(2).name.get());
    }

    @Test
    @DisplayName("Verify DataGrip Duplicate (Ctrl+D) column cloning and _2 suffix generation")
    public void testDataGripDuplicateColumn() {
        ConnectionProfile pgProfile = createPostgresProfile();
        DbObject table = createTable("users", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, pgProfile, table, false, false);
        dialog.refreshTreeAndPreview();

        // Test name generation logic
        assertEquals("last_modified_by_2", dialog.generateDuplicateNameForTesting("last_modified_by"));
        assertEquals("last_modified_by_3", dialog.generateDuplicateNameForTesting("last_modified_by_2"));
        assertEquals("email_2", dialog.generateDuplicateNameForTesting("email"));

        // Add base column
        ModifyTableDialog.ColumnModel col = new ModifyTableDialog.ColumnModel(
                "last_modified_by", "last_modified_by", "bigint", false, null, false, false, false);
        col.comment.set("User who last updated this row");
        dialog.getColumns().add(col);
        dialog.refreshTreeAndPreview();

        // Duplicate col
        String dupName = dialog.generateDuplicateNameForTesting(col.name.get());
        assertEquals("last_modified_by_2", dupName);

        ModifyTableDialog.ColumnModel dupCol = new ModifyTableDialog.ColumnModel(
                null, dupName, col.type.get(), col.nullable.get(), col.defaultValue.get(),
                col.primaryKey.get(), col.autoIncrement.get(), true);
        dupCol.comment.set(col.comment.get());
        dialog.getColumns().add(dupCol);

        List<String> stmts = dialog.buildAlterStatements();
        assertTrue(stmts.stream().anyMatch(s -> s.contains("ADD COLUMN last_modified_by_2 bigint NOT NULL;")));

        String preview = ModifyTableDialog.formatStatementsForPreview(stmts);
        assertTrue(preview.contains("alter table public.users\n    add last_modified_by_2 bigint NOT NULL;"));
    }

    @Test
    @DisplayName("Verify DataGrip SQL preview formatting with 4-space indent and blank lines")
    public void testDataGripPreviewFormatting() {
        List<String> rawStatements = List.of(
                "ALTER TABLE public.users ADD COLUMN column_name integer;",
                "ALTER TABLE public.users ADD COLUMN column_name_2 integer;"
        );

        String formatted = ModifyTableDialog.formatStatementsForPreview(rawStatements);
        String expected = "alter table public.users\n    add column_name integer;\n\n"
                + "alter table public.users\n    add column_name_2 integer;";
        assertEquals(expected, formatted);
    }

    @Test
    @DisplayName("Verify Cancel warning state and don't ask again preference")
    public void testCancelWarningState() {
        assertFalse(ModifyTableDialog.isDontAskAgainOnCancel());
        ModifyTableDialog.setDontAskAgainOnCancel(true);
        assertTrue(ModifyTableDialog.isDontAskAgainOnCancel());
        ModifyTableDialog.setDontAskAgainOnCancel(false);
        assertFalse(ModifyTableDialog.isDontAskAgainOnCancel());
    }
}

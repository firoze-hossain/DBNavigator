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

    @Test
    @DisplayName("Verify KeyModel enhanced properties, primary/type sync, and DDL generation")
    public void testEnhancedKeyPropertiesAndDdl() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("allergy_type", "public", "clinic_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);

        ModifyTableDialog.KeyModel key = new ModifyTableDialog.KeyModel(
                null, "allergy_type_pkey", "PRIMARY KEY", List.of("id"), true);
        assertTrue(key.primary.get(), "Primary property should be true for PRIMARY KEY type");

        // Test primary <-> type sync
        key.primary.set(false);
        assertEquals("UNIQUE", key.type.get());
        key.type.set("PRIMARY KEY");
        assertTrue(key.primary.get());

        // Test flags
        key.deferrable.set(true);
        key.initiallyDeferred.set(true);
        key.notEnforced.set(true);
        key.definedWithPeriod.set(true);
        key.notValidated.set(true);
        key.excludeOperator.set("=");
        assertEquals("=", key.excludeOperator.get());

        dialog.getKeys().add(key);
        List<String> stmts = dialog.buildAlterStatements();
        assertEquals(1, stmts.size());
        String ddl = stmts.get(0);
        assertTrue(ddl.contains("ADD CONSTRAINT allergy_type_pkey PRIMARY KEY (id)"));
        assertTrue(ddl.contains("DEFERRABLE INITIALLY DEFERRED"));
        assertTrue(ddl.contains("NOT ENFORCED;"));
    }

    @Test
    @DisplayName("Verify IndexModel enhanced properties, order per column, nulls not distinct, include cols, tablespace, and DDL")
    public void testEnhancedIndexPropertiesAndDdl() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("allergy_type", "public", "clinic_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);

        ModifyTableDialog.IndexModel idx = new ModifyTableDialog.IndexModel(
                null, "idx_allergy_name", true, "btree", List.of("name", "code"), true);
        idx.nullsNotDistinct.set(true);
        idx.includeColumns.set("created_at");
        idx.tablespace.set("fast_ssd");
        idx.predicate.set("active = true");

        // Column properties
        idx.getOrderProperty("name").set("DESC NULLS LAST");
        idx.getCollationProperty("name").set("en_US");
        idx.getOpClassProperty("name").set("varchar_ops");
        assertEquals("DESC NULLS LAST", idx.getOrderProperty("name").get());
        assertEquals("en_US", idx.getCollationProperty("name").get());
        assertEquals("varchar_ops", idx.getOpClassProperty("name").get());

        dialog.getIndexes().add(idx);
        List<String> stmts = dialog.buildAlterStatements();
        assertEquals(1, stmts.size());
        String ddl = stmts.get(0);
        assertTrue(ddl.contains("CREATE UNIQUE INDEX idx_allergy_name ON public.allergy_type USING btree (name DESC NULLS LAST, code)"));
        assertTrue(ddl.contains("NULLS NOT DISTINCT"));
        assertTrue(ddl.contains("INCLUDE (created_at)"));
        assertTrue(ddl.contains("WHERE active = true"));
        assertTrue(ddl.contains("TABLESPACE fast_ssd;"));
    }

    @Test
    @DisplayName("Verify CheckModel enhanced properties (no inherit, not valid, deferrable, not enforced) and DDL")
    public void testEnhancedCheckPropertiesAndDdl() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("allergy_type", "public", "clinic_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);

        ModifyTableDialog.CheckModel chk = new ModifyTableDialog.CheckModel(
                null, "chk_code_valid", "length(code) >= 2", true);
        chk.noInherit.set(true);
        chk.notValidated.set(true);
        chk.deferrable.set(true);
        chk.initiallyDeferred.set(true);
        chk.notEnforced.set(true);

        dialog.getChecks().add(chk);
        List<String> stmts = dialog.buildAlterStatements();
        assertEquals(1, stmts.size());
        String ddl = stmts.get(0);
        assertTrue(ddl.contains("ADD CONSTRAINT chk_code_valid CHECK (length(code) >= 2)"));
        assertTrue(ddl.contains("NO INHERIT"));
        assertTrue(ddl.contains("NOT VALID"));
        assertTrue(ddl.contains("DEFERRABLE INITIALLY DEFERRED"));
        assertTrue(ddl.contains("NOT ENFORCED;"));
    }

    @Test
    @DisplayName("Verify ForeignKeyModel enhanced properties (deferrable, initially deferred) and DDL")
    public void testEnhancedForeignKeyPropertiesAndDdl() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("patient_allergies", "public", "clinic_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);

        ModifyTableDialog.ForeignKeyModel fk = new ModifyTableDialog.ForeignKeyModel(
                null, "fk_patient_allergy_type", "allergy_type", "type_id", "id", true);
        fk.deferrable.set(true);
        fk.initiallyDeferred.set(true);

        dialog.getForeignKeys().add(fk);
        List<String> stmts = dialog.buildAlterStatements();
        assertEquals(1, stmts.size());
        String ddl = stmts.get(0);
        assertTrue(ddl.contains("ADD CONSTRAINT fk_patient_allergy_type FOREIGN KEY (type_id) REFERENCES allergy_type (id)"));
        assertTrue(ddl.contains("DEFERRABLE INITIALLY DEFERRED;"));
    }

    @Test
    @DisplayName("Verify Master Tree node selection delegates for keys, indexes, checks, and foreign keys")
    public void testMasterTreeNodeSelectionDelegates() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("allergy_type", "public", "clinic_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);

        ModifyTableDialog.ColumnModel col = new ModifyTableDialog.ColumnModel("id", "id", "bigint", false, null, true, true, false);
        dialog.getColumns().add(col);

        ModifyTableDialog.KeyModel key = new ModifyTableDialog.KeyModel("pk_test", "pk_test", "PRIMARY KEY", List.of("id"), false);
        dialog.getKeys().add(key);

        ModifyTableDialog.IndexModel idx = new ModifyTableDialog.IndexModel("idx_test", "idx_test", false, "btree", List.of("id"), false);
        dialog.getIndexes().add(idx);

        ModifyTableDialog.CheckModel chk = new ModifyTableDialog.CheckModel("chk_test", "chk_test", "id > 0", false);
        dialog.getChecks().add(chk);

        ModifyTableDialog.ForeignKeyModel fk = new ModifyTableDialog.ForeignKeyModel("fk_test", "fk_test", "other", "id", "id", false);
        dialog.getForeignKeys().add(fk);

        dialog.refreshTreeAndPreview();

        // Exercise tree selection delegates
        assertDoesNotThrow(() -> dialog.selectColumnInTreeForTesting(col));
        assertDoesNotThrow(() -> dialog.selectKeyInTreeForTesting(key));
        assertDoesNotThrow(() -> dialog.selectIndexInTreeForTesting(idx));
        assertDoesNotThrow(() -> dialog.selectCheckInTreeForTesting(chk));
        assertDoesNotThrow(() -> dialog.selectForeignKeyInTreeForTesting(fk));
    }

    @Test
    @DisplayName("Verify Composite Foreign Key mappings, reordering, match type, not enforced, and DDL")
    public void testCompositeForeignKeyMappingsAndDdl() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("order_items", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);

        ModifyTableDialog.ForeignKeyModel fk = new ModifyTableDialog.ForeignKeyModel(
                null, "fk_order_items_composite", "orders", "tenant_id", "tenant_id", true);
        fk.mappings.add(new ModifyTableDialog.ForeignKeyMapping("order_id", "id"));
        fk.matchType.set("FULL");
        fk.onUpdate.set("cascade");
        fk.onDelete.set("set_null");
        fk.deferrable.set(true);
        fk.initiallyDeferred.set(true);
        fk.notEnforced.set(true);

        assertEquals(2, fk.mappings.size());
        assertEquals("tenant_id \u2192 tenant_id", fk.mappings.get(0).toString());
        assertEquals("order_id \u2192 id", fk.mappings.get(1).toString());

        dialog.getForeignKeys().add(fk);
        dialog.refreshTreeAndPreview();

        List<String> stmts = dialog.buildAlterStatements();
        assertEquals(1, stmts.size());
        String ddl = stmts.get(0);
        assertTrue(ddl.contains("ADD CONSTRAINT fk_order_items_composite FOREIGN KEY (tenant_id, order_id) REFERENCES orders (tenant_id, id)"));
        assertTrue(ddl.contains("MATCH FULL"));
        assertTrue(ddl.contains("ON UPDATE CASCADE ON DELETE SET NULL"));
        assertTrue(ddl.contains("DEFERRABLE INITIALLY DEFERRED"));
        assertTrue(ddl.contains("NOT ENFORCED;"));

        // Verify tree subtitle shows composite mappings
        TreeItem<ModifyTableDialog.ModifyNode> fkCategory = dialog.getFkCategoryItem();
        assertEquals(1, fkCategory.getChildren().size());
        assertEquals("(tenant_id -> orders.tenant_id, order_id -> orders.id)", fkCategory.getChildren().get(0).getValue().subtitle);
    }

    @Test
    @DisplayName("Verify Check Constraint predicate truncation in tree and expand predicate sync")
    public void testCheckPredicateTreeTruncation() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("allergy_type", "public", "clinic_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);

        // Long predicate matching DataGrip Image 2
        String longPred = "((title)::text = ANY (ARRAY['Food'::character varying, 'Drug'::character varying]::text[]))";
        ModifyTableDialog.CheckModel chk = new ModifyTableDialog.CheckModel(
                null, "allergy_type_title_check", longPred, true);
        dialog.getChecks().add(chk);
        dialog.refreshTreeAndPreview();

        TreeItem<ModifyTableDialog.ModifyNode> chkCategory = dialog.getChkCategoryItem();
        assertEquals(1, chkCategory.getChildren().size());
        // Subtitle should truncate to 19 chars + "..."
        assertEquals("(((title)::text = AN...)", chkCategory.getChildren().get(0).getValue().subtitle);
    }

    @Test
    @DisplayName("Verify Foreign Key and Check duplication copies enhanced properties and composite mappings")
    public void testForeignKeyAndCheckDuplication() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("order_items", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);
        dialog.refreshTreeAndPreview();

        // 1. Add composite FK and duplicate it
        ModifyTableDialog.ForeignKeyModel fk = new ModifyTableDialog.ForeignKeyModel(
                null, "fk_order", "orders", "tenant_id", "tenant_id", true);
        fk.mappings.add(new ModifyTableDialog.ForeignKeyMapping("order_id", "id"));
        fk.matchType.set("FULL");
        fk.notEnforced.set(true);
        dialog.getForeignKeys().add(fk);
        dialog.refreshTreeAndPreview();

        dialog.duplicateItemForTesting(fk);

        assertEquals(2, dialog.getForeignKeys().size());
        ModifyTableDialog.ForeignKeyModel dupFk = dialog.getForeignKeys().get(1);
        assertEquals("fk_order_2", dupFk.name.get());
        assertEquals("FULL", dupFk.matchType.get());
        assertTrue(dupFk.notEnforced.get());
        assertEquals(2, dupFk.mappings.size());
        assertEquals("tenant_id", dupFk.mappings.get(0).localColumn.get());
        assertEquals("order_id", dupFk.mappings.get(1).localColumn.get());

        // 2. Add Check and duplicate it
        ModifyTableDialog.CheckModel chk = new ModifyTableDialog.CheckModel(
                null, "chk_status", "status in ('A', 'B')", true);
        chk.noInherit.set(true);
        chk.notValidated.set(true);
        dialog.getChecks().add(chk);
        dialog.refreshTreeAndPreview();

        dialog.duplicateItemForTesting(chk);

        assertEquals(2, dialog.getChecks().size());
        ModifyTableDialog.CheckModel dupChk = dialog.getChecks().get(1);
        assertEquals("chk_status_2", dupChk.name.get());
        assertEquals("status in ('A', 'B')", dupChk.expression.get());
        assertTrue(dupChk.noInherit.get());
        assertTrue(dupChk.notValidated.get());
    }

    @Test
    @DisplayName("Verify DataGrip Unique Key dynamic add, naming (carts_pk), empty subtitle, and preview")
    public void testDataGripUniqueKeyDynamicAddAndNaming() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("carts", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);
        dialog.refreshTreeAndPreview();

        // 1. Add Unique Key
        dialog.addKey("UNIQUE");
        assertEquals(1, dialog.getKeys().size());
        ModifyTableDialog.KeyModel key1 = dialog.getKeys().get(0);
        assertEquals("carts_pk", key1.name.get());
        assertTrue(key1.columns.isEmpty());

        // Verify tree item has empty subtitle
        TreeItem<ModifyTableDialog.ModifyNode> keyCategory = dialog.getKeyCategoryItem();
        assertEquals(1, keyCategory.getChildren().size());
        assertEquals("", keyCategory.getChildren().get(0).getValue().subtitle);

        // Verify preview shows empty unique ()
        String preview1 = dialog.getPreviewText();
        assertTrue(preview1.contains("alter table public.carts\n    add constraint carts_pk\n        unique ();"),
                "Preview should match DataGrip Image 1:\n" + preview1);

        // 2. Add second Unique Key -> carts_pk_2
        assertEquals("carts_pk_2", dialog.generateNextKeyNameForTesting("UNIQUE"));
        dialog.addKey("UNIQUE");
        assertEquals(2, dialog.getKeys().size());
        assertEquals("carts_pk_2", dialog.getKeys().get(1).name.get());
    }

    @Test
    @DisplayName("Verify DataGrip Foreign Key dynamic add, naming (carts_carts_fk), empty subtitle, and preview")
    public void testDataGripForeignKeyDynamicAddAndNaming() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("carts", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);
        dialog.refreshTreeAndPreview();

        // 1. Add Foreign Key
        dialog.addForeignKey();
        assertEquals(1, dialog.getForeignKeys().size());
        ModifyTableDialog.ForeignKeyModel fk1 = dialog.getForeignKeys().get(0);
        assertEquals("carts_carts_fk", fk1.name.get());
        assertEquals("public.carts", fk1.targetTable.get());
        assertTrue(fk1.mappings.isEmpty());

        // Verify tree item has empty subtitle
        TreeItem<ModifyTableDialog.ModifyNode> fkCategory = dialog.getFkCategoryItem();
        assertEquals(1, fkCategory.getChildren().size());
        assertEquals("", fkCategory.getChildren().get(0).getValue().subtitle);

        // Verify preview shows empty foreign key () references public.carts ()
        String preview1 = dialog.getPreviewText();
        assertTrue(preview1.contains("alter table public.carts\n    add constraint carts_carts_fk\n        foreign key () references public.carts ();"),
                "Preview should match DataGrip Image 3:\n" + preview1);

        // 2. Add second Foreign Key -> carts_carts_fk_2
        assertEquals("carts_carts_fk_2", dialog.generateNextForeignKeyNameForTesting("public.carts"));
        dialog.addForeignKey();
        assertEquals(2, dialog.getForeignKeys().size());
        assertEquals("carts_carts_fk_2", dialog.getForeignKeys().get(1).name.get());
    }

    @Test
    @DisplayName("Verify DataGrip Index dynamic add, naming (carts__index), (???) subtitle, and preview")
    public void testDataGripIndexDynamicAddAndNaming() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("carts", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);
        dialog.refreshTreeAndPreview();

        // 1. Add Index
        dialog.addIndex();
        assertEquals(1, dialog.getIndexes().size());
        ModifyTableDialog.IndexModel idx1 = dialog.getIndexes().get(0);
        assertEquals("carts__index", idx1.name.get());
        assertTrue(idx1.columns.isEmpty());

        // Verify tree item has (???) subtitle matching DataGrip Image 4
        TreeItem<ModifyTableDialog.ModifyNode> idxCategory = dialog.getIdxCategoryItem();
        assertEquals(1, idxCategory.getChildren().size());
        assertEquals("(???)", idxCategory.getChildren().get(0).getValue().subtitle);

        // Verify preview shows create index carts__index\n    on public.carts ();
        String preview1 = dialog.getPreviewText();
        assertTrue(preview1.contains("create index carts__index\n    on public.carts ();"),
                "Preview should match DataGrip Image 4:\n" + preview1);

        // 2. Add second Index -> carts__index_2
        assertEquals("carts__index_2", dialog.generateNextIndexNameForTesting());
        dialog.addIndex();
        assertEquals(2, dialog.getIndexes().size());
        assertEquals("carts__index_2", dialog.getIndexes().get(1).name.get());
    }

    @Test
    @DisplayName("Verify DataGrip Check dynamic add, naming (check_name), empty subtitle, and preview")
    public void testDataGripCheckDynamicAddAndNaming() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("carts", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);
        dialog.refreshTreeAndPreview();

        // 1. Add Check
        dialog.addCheck();
        assertEquals(1, dialog.getChecks().size());
        ModifyTableDialog.CheckModel chk1 = dialog.getChecks().get(0);
        assertEquals("check_name", chk1.name.get());
        assertTrue(chk1.expression.get().isEmpty());

        // Verify tree item has empty subtitle matching DataGrip Image 5
        TreeItem<ModifyTableDialog.ModifyNode> chkCategory = dialog.getChkCategoryItem();
        assertEquals(1, chkCategory.getChildren().size());
        assertEquals("", chkCategory.getChildren().get(0).getValue().subtitle);

        // Verify preview shows alter table public.carts\n    add constraint check_name\n        check ();
        String preview1 = dialog.getPreviewText();
        assertTrue(preview1.contains("alter table public.carts\n    add constraint check_name\n        check ();"),
                "Preview should match DataGrip Image 5:\n" + preview1);

        // 2. Add second Check -> check_name_2
        assertEquals("check_name_2", dialog.generateNextCheckNameForTesting());
        dialog.addCheck();
        assertEquals(2, dialog.getChecks().size());
        assertEquals("check_name_2", dialog.getChecks().get(1).name.get());
    }

    @Test
    @DisplayName("Verify DataGrip Virtual Column dynamic add, naming (virtual_column_name), 'unknown' subtitle, expression update, duplication, and preview")
    public void testDataGripVirtualColumnDynamicAddAndNaming() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("carts", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);
        dialog.refreshTreeAndPreview();

        // 1. Initial next name
        assertEquals("virtual_column_name", dialog.generateNextVirtualColumnNameForTesting());

        // 2. Add Virtual Column
        dialog.addVirtualColumn();
        assertEquals(1, dialog.getVirtualColumns().size());
        ModifyTableDialog.VirtualColumnModel vc1 = dialog.getVirtualColumns().get(0);
        assertEquals("virtual_column_name", vc1.name.get());
        assertEquals("", vc1.expression.get());

        // Tree subtitle must be "unknown" matching DataGrip Image 2
        TreeItem<ModifyTableDialog.ModifyNode> vcolCategory = dialog.getVcolCategoryItem();
        assertEquals(1, vcolCategory.getChildren().size());
        assertEquals("virtual_column_name", vcolCategory.getChildren().get(0).getValue().title);
        assertEquals("unknown", vcolCategory.getChildren().get(0).getValue().subtitle);

        // 3. Modifying expression updates tree subtitle
        vc1.expression.set("col1 || ' ' || col2");
        dialog.refreshTreeAndPreview();
        assertEquals("col1 || ' ' || col2", dialog.getVcolCategoryItem().getChildren().get(0).getValue().subtitle);

        // 4. Second Virtual Column -> virtual_column_name_2
        assertEquals("virtual_column_name_2", dialog.generateNextVirtualColumnNameForTesting());
        dialog.addVirtualColumn();
        assertEquals(2, dialog.getVirtualColumns().size());
        ModifyTableDialog.VirtualColumnModel vc2 = dialog.getVirtualColumns().get(1);
        assertEquals("virtual_column_name_2", vc2.name.get());
        assertEquals("", vc2.expression.get());
        assertEquals(2, dialog.getVcolCategoryItem().getChildren().size());
        assertEquals("unknown", dialog.getVcolCategoryItem().getChildren().get(1).getValue().subtitle);

        // 5. Duplicate Virtual Column
        dialog.selectVirtualColumnInTreeForTesting(vc1);
        dialog.duplicateItemForTesting(vc1);
        assertEquals(3, dialog.getVirtualColumns().size());
        ModifyTableDialog.VirtualColumnModel vc3 = dialog.getVirtualColumns().get(1); // inserted after vc1
        assertEquals("virtual_column_name_3", vc3.name.get());
        assertEquals("col1 || ' ' || col2", vc3.expression.get());

        // 6. Preview: Virtual columns are client-side metadata, no database DDL emitted
        String preview = dialog.getPreviewText();
        assertFalse(preview.contains("virtual_column_name"), "Virtual columns should not emit ALTER TABLE statements");
    }

    @Test
    @DisplayName("Verify DataGrip Virtual Foreign Key dynamic add, naming (virtual_foreign_key_name), default target table, empty subtitle, mappings, duplication, and preview")
    public void testDataGripVirtualForeignKeyDynamicAddAndNaming() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = createTable("carts", "public", "store_db");

        ModifyTableDialog dialog = new ModifyTableDialog(null, profile, table, false, false);
        dialog.refreshTreeAndPreview();

        // 1. Initial next name
        assertEquals("virtual_foreign_key_name", dialog.generateNextVirtualForeignKeyNameForTesting());

        // 2. Add Virtual Foreign Key
        dialog.addVirtualForeignKey();
        assertEquals(1, dialog.getVirtualForeignKeys().size());
        ModifyTableDialog.VirtualForeignKeyModel vfk1 = dialog.getVirtualForeignKeys().get(0);
        assertEquals("virtual_foreign_key_name", vfk1.name.get());
        assertEquals("carts", vfk1.targetTable.get(), "Target Table should default to current table name matching DataGrip Image 3");
        assertTrue(vfk1.mappings.isEmpty(), "Initial column mappings should be empty matching DataGrip Image 3 'No Columns'");

        // Tree subtitle must be empty string until mappings are added
        assertEquals(1, dialog.getVfkCategoryItem().getChildren().size());
        assertEquals("virtual_foreign_key_name", dialog.getVfkCategoryItem().getChildren().get(0).getValue().title);
        assertEquals("", dialog.getVfkCategoryItem().getChildren().get(0).getValue().subtitle);

        // 3. Adding mappings updates tree subtitle
        vfk1.mappings.add(new ModifyTableDialog.ForeignKeyMapping("cart_id", "id"));
        dialog.refreshTreeAndPreview();
        assertEquals("(cart_id -> carts.id)", dialog.getVfkCategoryItem().getChildren().get(0).getValue().subtitle);

        // 4. Second Virtual FK -> virtual_foreign_key_name_2
        assertEquals("virtual_foreign_key_name_2", dialog.generateNextVirtualForeignKeyNameForTesting());
        dialog.addVirtualForeignKey();
        assertEquals(2, dialog.getVirtualForeignKeys().size());
        ModifyTableDialog.VirtualForeignKeyModel vfk2 = dialog.getVirtualForeignKeys().get(1);
        assertEquals("virtual_foreign_key_name_2", vfk2.name.get());
        assertEquals("carts", vfk2.targetTable.get());
        assertTrue(vfk2.mappings.isEmpty());
        assertEquals(2, dialog.getVfkCategoryItem().getChildren().size());
        assertEquals("", dialog.getVfkCategoryItem().getChildren().get(1).getValue().subtitle);

        // 5. Duplicate Virtual Foreign Key
        dialog.selectVirtualForeignKeyInTreeForTesting(vfk1);
        dialog.duplicateItemForTesting(vfk1);
        assertEquals(3, dialog.getVirtualForeignKeys().size());
        ModifyTableDialog.VirtualForeignKeyModel vfk3 = dialog.getVirtualForeignKeys().get(1); // inserted after vfk1
        assertEquals("virtual_foreign_key_name_3", vfk3.name.get());
        assertEquals("carts", vfk3.targetTable.get());
        assertEquals(1, vfk3.mappings.size());
        assertEquals("cart_id", vfk3.mappings.get(0).localColumn.get());
        assertEquals("id", vfk3.mappings.get(0).targetColumn.get());

        // 6. Preview: Virtual foreign keys are client-side metadata, no database DDL emitted
        String preview = dialog.getPreviewText();
        assertFalse(preview.contains("virtual_foreign_key_name"), "Virtual FKs should not emit ALTER TABLE statements");
    }
}


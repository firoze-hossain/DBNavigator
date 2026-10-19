package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import com.roze.dbnavigator.ui.diagram.DiagramGraph;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualForeignKeyServiceTest {

    @Test
    public void testExtractCandidatePrefixes() {
        List<String> prefixes = VirtualForeignKeyService.extractCandidatePrefixes("queueqtype_id");
        assertTrue(prefixes.contains("queueqtype"), "Should extract queueqtype from queueqtype_id");

        List<String> subTypePrefixes = VirtualForeignKeyService.extractCandidatePrefixes("queueqsub_type_id");
        assertTrue(subTypePrefixes.contains("queueqsub_type"), "Should extract queueqsub_type from queueqsub_type_id");

        List<String> userPrefixes = VirtualForeignKeyService.extractCandidatePrefixes("user_id");
        assertTrue(userPrefixes.contains("user"));

        List<String> codePrefixes = VirtualForeignKeyService.extractCandidatePrefixes("country_code");
        assertTrue(codePrefixes.contains("country"));

        List<String> noPrefixes = VirtualForeignKeyService.extractCandidatePrefixes("invoice_no");
        assertTrue(noPrefixes.contains("invoice"));

        List<String> idPrefixes = VirtualForeignKeyService.extractCandidatePrefixes("id_store");
        assertTrue(idPrefixes.contains("store"));

        List<String> gluedPrefixes = VirtualForeignKeyService.extractCandidatePrefixes("roleid");
        assertTrue(gluedPrefixes.contains("role"));
    }

    @Test
    public void testPluralAndSingularConversion() {
        assertEquals("users", VirtualForeignKeyService.toPlural("user"));
        assertEquals("categories", VirtualForeignKeyService.toPlural("category"));
        assertEquals("addresses", VirtualForeignKeyService.toPlural("address"));

        assertEquals("user", VirtualForeignKeyService.toSingular("users"));
        assertEquals("category", VirtualForeignKeyService.toSingular("categories"));
        assertEquals("order", VirtualForeignKeyService.toSingular("orders"));
    }

    @Test
    public void testResolveTargetTableExactAndNormalized() {
        DbObject queueq = new DbObject("queueq", Kind.TABLE, "uniquedb", "public");
        DbObject queueqtype = new DbObject("queueqtype", Kind.TABLE, "uniquedb", "public");
        DbObject queueqsubType = new DbObject("queueqsub_type", Kind.TABLE, "uniquedb", "public");
        DbObject users = new DbObject("users", Kind.TABLE, "uniquedb", "public");

        List<DbObject> tables = List.of(queueq, queueqtype, queueqsubType, users);
        VirtualForeignKeyService.TableIndex index = VirtualForeignKeyService.TableIndex.build(tables);

        // Exact match
        DbObject target1 = VirtualForeignKeyService.resolveTargetTable("queueqtype_id", index, queueq);
        assertNotNull(target1);
        assertEquals("queueqtype", target1.getName());

        // Normalized match
        DbObject target2 = VirtualForeignKeyService.resolveTargetTable("queueqsub_type_id", index, queueq);
        assertNotNull(target2);
        assertEquals("queueqsub_type", target2.getName());

        // Plural table match for singular column
        DbObject target3 = VirtualForeignKeyService.resolveTargetTable("user_id", index, queueq);
        assertNotNull(target3);
        assertEquals("users", target3.getName());

        // Self-referencing column (parent_id)
        DbObject targetSelf = VirtualForeignKeyService.resolveTargetTable("parent_id", index, queueq);
        assertNotNull(targetSelf);
        assertEquals("queueq", targetSelf.getName());

        // Non-FK columns should be rejected
        assertNull(VirtualForeignKeyService.resolveTargetTable("created_at", index, queueq));
        assertNull(VirtualForeignKeyService.resolveTargetTable("is_active", index, queueq));
        assertNull(VirtualForeignKeyService.resolveTargetTable("status", index, queueq));
        assertNull(VirtualForeignKeyService.resolveTargetTable("total_amount", index, queueq));
    }

    @Test
    public void testTypeCompatibility() {
        assertTrue(VirtualForeignKeyService.isTypeCompatible("bigint", "integer"));
        assertTrue(VirtualForeignKeyService.isTypeCompatible("serial", "int8"));
        assertTrue(VirtualForeignKeyService.isTypeCompatible("varchar(100)", "text"));
        assertTrue(VirtualForeignKeyService.isTypeCompatible("uuid", "uuid"));

        // Numeric vs varchar should not match to avoid false positives
        assertFalse(VirtualForeignKeyService.isTypeCompatible("bigint", "varchar(50)"));
    }

    @Test
    public void testInferForeignKeysForTablesMatchingUniqueDb() {
        // Mock the exact uniquedb schema tables: queueq, queueqtype, queueqsub_type
        DbObject queueq = new DbObject("queueq", Kind.TABLE, "uniquedb", "public");
        DbObject queueqtype = new DbObject("queueqtype", Kind.TABLE, "uniquedb", "public");
        DbObject queueqsubType = new DbObject("queueqsub_type", Kind.TABLE, "uniquedb", "public");

        List<DbObject> tables = List.of(queueq, queueqtype, queueqsubType);

        Map<String, List<MetadataService.ColumnInfo>> tableColumnsMap = new HashMap<>();

        // queueq columns: id, queueqtype_id, queueqsub_type_id, ...
        tableColumnsMap.put("queueq", List.of(
                new MetadataService.ColumnInfo("id", "bigint", 8, false, "nextval()", true),
                new MetadataService.ColumnInfo("name", "varchar", 255, false, null, false),
                new MetadataService.ColumnInfo("queueqtype_id", "bigint", 8, false, null, false),
                new MetadataService.ColumnInfo("queueqsub_type_id", "bigint", 8, false, null, false),
                new MetadataService.ColumnInfo("created_at", "timestamp", 8, true, null, false)
        ));

        // queueqtype columns: id (PK), name
        tableColumnsMap.put("queueqtype", List.of(
                new MetadataService.ColumnInfo("id", "bigint", 8, false, "nextval()", true),
                new MetadataService.ColumnInfo("type_name", "varchar", 100, false, null, false)
        ));

        // queueqsub_type columns: id (PK), name
        tableColumnsMap.put("queueqsub_type", List.of(
                new MetadataService.ColumnInfo("id", "bigint", 8, false, "nextval()", true),
                new MetadataService.ColumnInfo("sub_name", "varchar", 100, false, null, false)
        ));

        List<MetadataService.ForeignKey> existingPhysical = new ArrayList<>();

        List<MetadataService.ForeignKey> inferred = VirtualForeignKeyService.inferForeignKeysForTables(
                null, "uniquedb", tables, tableColumnsMap, existingPhysical
        );

        assertEquals(2, inferred.size(), "Should infer exactly 2 relationships for queueq");

        boolean foundType = false;
        boolean foundSubType = false;
        for (MetadataService.ForeignKey fk : inferred) {
            assertEquals("queueq", fk.fromTable());
            assertEquals("id", fk.toColumn());
            assertTrue(fk.constraintName().startsWith("inferred:"));

            if ("queueqtype_id".equals(fk.fromColumn()) && "queueqtype".equals(fk.toTable())) {
                foundType = true;
            } else if ("queueqsub_type_id".equals(fk.fromColumn()) && "queueqsub_type".equals(fk.toTable())) {
                foundSubType = true;
            }
        }

        assertTrue(foundType, "Should have inferred queueq.queueqtype_id -> queueqtype.id");
        assertTrue(foundSubType, "Should have inferred queueq.queueqsub_type_id -> queueqsub_type.id");
    }

    @Test
    public void testDiagramGraphVirtualEdge() {
        DiagramGraph graph = new DiagramGraph();
        DbObject queueq = new DbObject("queueq", Kind.TABLE, "uniquedb", "public");
        DbObject queueqtype = new DbObject("queueqtype", Kind.TABLE, "uniquedb", "public");

        graph.addNode(queueq, List.of(
                new MetadataService.ColumnInfo("id", "bigint", 8, false, null, true),
                new MetadataService.ColumnInfo("queueqtype_id", "bigint", 8, false, null, false)
        ));
        graph.addNode(queueqtype, List.of(
                new MetadataService.ColumnInfo("id", "bigint", 8, false, null, true)
        ));

        // Inferred edge
        graph.addEdge("queueq", "queueqtype_id", "queueqtype", "id", "inferred:queueqtype_id -> id");

        assertEquals(1, graph.getEdges().size());
        DiagramGraph.Edge edge = graph.getEdges().get(0);
        assertTrue(edge.isVirtual(), "Edge with inferred: constraint prefix must have isVirtual() == true");
    }
}

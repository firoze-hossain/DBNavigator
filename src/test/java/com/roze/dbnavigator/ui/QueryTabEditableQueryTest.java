package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.util.SqlParameters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QueryTabEditableQueryTest {

    @Test
    public void testStripCommentsPreservesLiterals() {
        String sql = "-- PostgreSQL console. Ctrl+Enter runs, Ctrl+Space completes.\n"
                + "SELECT * FROM brands WHERE name = '-- not a comment' AND note = '/* also not */';";
        String stripped = QueryTab.stripComments(sql);
        assertTrue(stripped.startsWith("SELECT * FROM brands"));
        assertTrue(stripped.contains("'-- not a comment'"));
        assertTrue(stripped.contains("'/* also not */'"));
        assertFalse(stripped.contains("Ctrl+Enter runs"));
    }

    @Test
    public void testDetectEditableTableWithLeadingComments() {
        String sql1 = "-- PostgreSQL console. Ctrl+Enter runs, Ctrl+Space completes.\n"
                + "select * from brands;";
        assertEquals("brands", QueryTab.detectEditableTable(sql1));

        String sql2 = "/* header comment */\nselect id, name from public.brands;";
        assertEquals("public.brands", QueryTab.detectEditableTable(sql2));

        String sql3 = "-- line 1\n-- line 2\nSELECT * FROM \"brands\";";
        assertEquals("\"brands\"", QueryTab.detectEditableTable(sql3));

        String sqlAgg = "-- banner\nSELECT count(*) FROM brands;";
        assertNull(QueryTab.detectEditableTable(sqlAgg));

        String sqlJoin = "-- banner\nSELECT * FROM brands b JOIN products p ON b.id = p.brand_id;";
        assertNull(QueryTab.detectEditableTable(sqlJoin));
    }

    @Test
    public void testIsSimpleSingleTableSelectWithComments() {
        String sql1 = "-- PostgreSQL console. Ctrl+Enter runs, Ctrl+Space completes.\n"
                + "select * from brands;";
        assertTrue(QueryTab.isSimpleSingleTableSelect(sql1));

        String sql2 = "-- comment\nSELECT * FROM brands WHERE id = 5;";
        assertTrue(QueryTab.isSimpleSingleTableSelect(sql2));

        String sqlComplex = "-- comment\nSELECT * FROM a, b WHERE a.id = b.id;";
        assertFalse(QueryTab.isSimpleSingleTableSelect(sqlComplex));

        // Partition queries with ORDER BY, newlines, and LIMIT
        String sqlOrderBy = "select *\nfrom appointment_tx order by created_at desc;";
        assertTrue(QueryTab.isSimpleSingleTableSelect(sqlOrderBy));
        assertEquals("appointment_tx", QueryTab.detectEditableTable(sqlOrderBy));

        String sqlLimit = "SELECT * FROM appointment_tx LIMIT 100;";
        assertTrue(QueryTab.isSimpleSingleTableSelect(sqlLimit));

        String sqlWhereOrder = "SELECT * FROM appointment_tx WHERE org_id = 1 ORDER BY created_at DESC;";
        assertTrue(QueryTab.isSimpleSingleTableSelect(sqlWhereOrder));

        // Classic Mac \r line ending in comment
        String sqlMacCr = "-- Mac comment\rSELECT * FROM appointment_tx ORDER BY created_at DESC;";
        assertTrue(QueryTab.isSimpleSingleTableSelect(sqlMacCr));
        assertEquals("appointment_tx", QueryTab.detectEditableTable(sqlMacCr));
    }

    @Test
    public void testAddRowIdentityToSelectWithComments() {
        String sql = "-- PostgreSQL console. Ctrl+Enter runs, Ctrl+Space completes.\n"
                + "select * from brands;";
        String rewritten = QueryTab.addRowIdentityToSelect(sql);
        assertTrue(rewritten.contains("tableoid::text AS tableoid, ctid, * from brands;"));
        assertTrue(rewritten.startsWith("-- PostgreSQL console. Ctrl+Enter runs, Ctrl+Space completes.\nselect"));

        String sql2 = "SELECT id, name FROM brands;";
        String rewritten2 = QueryTab.addRowIdentityToSelect(sql2);
        assertTrue(rewritten2.startsWith("SELECT tableoid::text AS tableoid, ctid, id, name FROM brands;"));
    }

    @Test
    public void testAddMissingPrimaryKeyColumnsWithComments() {
        String sql = "-- user comment\nSELECT name, price FROM products;";
        String rewritten = QueryTab.addMissingPrimaryKeyColumns(sql, List.of("id"));
        assertTrue(rewritten.contains("SELECT id, name, price FROM products;"));
        assertTrue(rewritten.startsWith("-- user comment\n"));

        String sqlStar = "-- user comment\nSELECT * FROM products;";
        String rewrittenStar = QueryTab.addMissingPrimaryKeyColumns(sqlStar, List.of("id"));
        assertEquals(sqlStar, rewrittenStar);
    }

    @Test
    public void testSqlParametersCommentTitleIgnoresConsoleHelp() {
        String banner = "-- PostgreSQL console. Ctrl+Enter runs, Ctrl+Space completes.\nselect * from brands;";
        assertNull(SqlParameters.extractPrecedingCommentTitle(banner, null));

        String userComment = "-- Top 10 Customers\nselect * from customers limit 10;";
        assertEquals("Top 10 Customers", SqlParameters.extractPrecedingCommentTitle(userComment, null));
    }
}

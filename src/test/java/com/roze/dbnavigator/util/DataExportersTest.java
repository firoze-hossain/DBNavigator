package com.roze.dbnavigator.util;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DataExportersTest {

    private QueryResult sampleResult;

    @BeforeEach
    public void setup() {
        sampleResult = new QueryResult();
        sampleResult.getColumns().addAll(List.of("id", "name", "score", "active", "metadata"));
        sampleResult.getColumnTypes().addAll(List.of("int", "varchar", "numeric", "boolean", "json"));
        sampleResult.getRows().addAll(List.of(
                List.of("1", "Alice", "95.5", "true", "{\"role\": \"admin\"}"),
                List.of("2", "Bob", "82.0", "false", "{\"role\": \"user\"}"),
                List.of("3", "Charlie, Jr.", "99.9", "true", "{\"role\": \"guest\"}")
        ));
    }

    @Test
    public void testAllFormatsRender() {
        for (DataExporters.Format format : DataExporters.Format.values()) {
            String rendered = DataExporters.render(format, sampleResult, "public.students", 10);
            assertNotNull(rendered, "Render output should not be null for format: " + format);
            if (format != DataExporters.Format.HTML_JS) {
                assertFalse(rendered.isEmpty(), "Render output should not be empty for format: " + format);
            }
        }
    }

    @Test
    public void testCsvRendering() {
        String csv = DataExporters.render(DataExporters.Format.CSV, sampleResult, "students", 10);
        assertTrue(csv.contains("id,name,score,active,metadata"));
        assertTrue(csv.contains("1,Alice,95.5,true"));
        // Check escaping of comma in Charlie, Jr.
        assertTrue(csv.contains("\"Charlie, Jr.\""));
    }

    @Test
    public void testTsvRendering() {
        String tsv = DataExporters.render(DataExporters.Format.TSV, sampleResult, "students", 10);
        assertTrue(tsv.contains("id\tname\tscore\tactive\tmetadata"));
        assertTrue(tsv.contains("1\tAlice\t95.5\ttrue"));
    }

    @Test
    public void testJsonRenderingWithNestedJsonAndPrimitives() {
        String json = DataExporters.render(DataExporters.Format.JSON, sampleResult, "students", 10);
        assertTrue(json.startsWith("[\n"));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("\"name\": \"Alice\""));
        assertTrue(json.contains("\"score\": 95.5"));
        assertTrue(json.contains("\"active\": true"));
        // Embedded valid JSON should not be escaped as a string
        assertTrue(json.contains("\"metadata\": {\"role\": \"admin\"}"));
    }

    @Test
    public void testMarkdownRendering() {
        String md = DataExporters.render(DataExporters.Format.MARKDOWN, sampleResult, "students", 10);
        assertTrue(md.contains("| id | name | score | active | metadata |"));
        assertTrue(md.contains("|:---|:---|:---|:---|:---|"));
        assertTrue(md.contains("| 1 | Alice | 95.5 | true |"));
    }

    @Test
    public void testPrettyRendering() {
        String pretty = DataExporters.render(DataExporters.Format.PRETTY, sampleResult, "students", 10);
        assertTrue(pretty.contains("+"));
        assertTrue(pretty.contains("|id"));
        assertTrue(pretty.contains("|Alice"));
    }

    @Test
    public void testPythonDataFrameRendering() {
        String py = DataExporters.render(DataExporters.Format.PYTHON_DATAFRAME, sampleResult, "students", 10);
        assertTrue(py.contains("import pandas as pd"));
        assertTrue(py.contains("df = pd.DataFrame(data)"));
        assertTrue(py.contains("\"active\": True"));
    }

    @Test
    public void testSqlInsertMultirowRendering() {
        String sql = DataExporters.render(DataExporters.Format.SQL_INSERT_MULTIROW, sampleResult, "students", 10);
        assertTrue(sql.contains("insert into students"));
        assertTrue(sql.contains("values ("));
        assertTrue(sql.contains("Alice"));
    }

    @Test
    public void testHtmlRendering() {
        String htmlGroovy = DataExporters.render(DataExporters.Format.HTML_GROOVY, sampleResult, "students", 10);
        assertTrue(htmlGroovy.contains("<table border=\"1\" style=\"border-collapse:collapse\">"));
        assertTrue(htmlGroovy.contains("<th>name</th>"));
        assertTrue(htmlGroovy.contains("<td>Alice</td>"));

        String htmlJs = DataExporters.render(DataExporters.Format.HTML_JS, sampleResult, "students", 10);
        assertTrue(htmlJs.isEmpty(), "HTML (js) output should be empty matching DataGrip preview");
    }

    @Test
    public void testXmlRendering() {
        String xml = DataExporters.render(DataExporters.Format.XML, sampleResult, "students", 10);
        assertTrue(xml.contains("<data>"));
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("    <name>Alice</name>"));
        assertTrue(xml.contains("</row>"));
        assertTrue(xml.contains("</data>"));
    }

    @Test
    public void testTransposition() {
        DataExporters.ExportOptions opts = new DataExporters.ExportOptions();
        opts.setTranspose(true);
        String csvTransposed = DataExporters.render(DataExporters.Format.CSV, sampleResult, "students", 10, opts);
        // Column names should now appear in the first column
        assertTrue(csvTransposed.contains("id,1,2,3"));
        assertTrue(csvTransposed.contains("name,Alice,Bob,\"Charlie, Jr.\""));
    }

    @Test
    public void testRowHeadersAndNoColumnHeaders() {
        DataExporters.ExportOptions opts = new DataExporters.ExportOptions();
        opts.setAddRowHeader(true);
        opts.setAddColumnHeader(false);
        String csv = DataExporters.render(DataExporters.Format.CSV, sampleResult, "students", 10, opts);
        assertFalse(csv.startsWith("id,name"), "Header row should be omitted");
        assertTrue(csv.startsWith("1,1,Alice") || csv.startsWith("1,Alice"), "Should include row index");
    }

    @Test
    public void testDefaultExtensions() {
        assertEquals(".csv", DataExporters.defaultExtension(DataExporters.Format.CSV));
        assertEquals(".csv", DataExporters.defaultExtension(DataExporters.Format.CSV_GROOVY));
        assertEquals(".tsv", DataExporters.defaultExtension(DataExporters.Format.TSV));
        assertEquals(".txt", DataExporters.defaultExtension(DataExporters.Format.PIPE));
        assertEquals(".txt", DataExporters.defaultExtension(DataExporters.Format.SEMICOLON));
        assertEquals(".json", DataExporters.defaultExtension(DataExporters.Format.JSON));
        assertEquals(".xlsx", DataExporters.defaultExtension(DataExporters.Format.EXCEL));
        assertEquals(".sql", DataExporters.defaultExtension(DataExporters.Format.SQL_INSERTS));
        assertEquals(".sql", DataExporters.defaultExtension(DataExporters.Format.SQL_INSERT_MULTIROW));
        assertEquals(".sql", DataExporters.defaultExtension(DataExporters.Format.ONE_ROW));
        assertEquals(".md", DataExporters.defaultExtension(DataExporters.Format.MARKDOWN));
        assertEquals(".py", DataExporters.defaultExtension(DataExporters.Format.PYTHON_DATAFRAME));
        assertEquals(".html", DataExporters.defaultExtension(DataExporters.Format.HTML_GROOVY));
        assertEquals(".html", DataExporters.defaultExtension(DataExporters.Format.HTML_JS));
        assertEquals(".xml", DataExporters.defaultExtension(DataExporters.Format.XML));
    }

    @Test
    public void testCsvGroovyRenderingWithNulls() {
        QueryResult qr = new QueryResult();
        qr.getColumns().addAll(List.of("id", "name", "desc", "active"));
        qr.getRows().add(java.util.Arrays.asList("1", "Checkup", null, "true"));
        qr.getRows().add(java.util.Arrays.asList("2", null, "Follow-up", "null"));

        String output = DataExporters.render(DataExporters.Format.CSV_GROOVY, qr, "appointment_tx", 10);
        // Groovy CSV has no header row and prints null literal
        assertFalse(output.startsWith("id,name"), "CSV_GROOVY should not include column headers");
        assertTrue(output.contains("1,Checkup,null,true"));
        assertTrue(output.contains("2,null,Follow-up,null"));
    }

    @Test
    public void testMarkdownRenderingDataGripParity() {
        QueryResult qr = new QueryResult();
        qr.getColumns().addAll(List.of("id", "created_at", "last_modified_by"));
        qr.getRows().add(java.util.Arrays.asList("1331", "2021-11-14 21:58:01.553000", null));

        String md = DataExporters.render(DataExporters.Format.MARKDOWN, qr, "appointment_tx", 10);
        // Underscores escaped
        assertTrue(md.contains("created\\_at"), "Underscores in column names should be escaped with backslash");
        assertTrue(md.contains("last\\_modified\\_by"), "Underscores in column names should be escaped with backslash");
        // Separator has :---
        assertTrue(md.contains(":---|:---|:---|"), "Separator row should use :---");
        // Nulls rendered as null literal
        assertTrue(md.contains("1331 | 2021-11-14 21:58:01.553000 | null |"));
    }

    @Test
    public void testHtmlGroovyRenderingDataGripParity() {
        QueryResult qr = new QueryResult();
        qr.getColumns().addAll(List.of("id", "created_at"));
        qr.getRows().add(java.util.Arrays.asList("1331", "2021-11-14"));

        String html = DataExporters.render(DataExporters.Format.HTML_GROOVY, qr, "appointment_tx", 10);
        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<colgroup>"));
        assertTrue(html.contains("<col width=\"50.00%\"/>"));
        assertTrue(html.contains("<table border=\"1\" style=\"border-collapse:collapse\">"));
        assertTrue(html.contains("<th>id</th>"));
        assertTrue(html.contains("<td>1331</td>"));
    }

    @Test
    public void testOneRowRenderingDataGripParity() {
        QueryResult qr = new QueryResult();
        qr.getColumns().addAll(List.of("id", "created_at", "status", "remarks"));
        qr.getRows().add(java.util.Arrays.asList("1331", "2021-11-14 21:58:01.553000", "18", null));
        qr.getRows().add(java.util.Arrays.asList("1579", "2021-12-26 09:46:53.330000", "18", null));

        String oneRow = DataExporters.render(DataExporters.Format.ONE_ROW, qr, "appointment_tx", 10);
        // Only row 1 is rendered as SQL literal list
        assertEquals("1331, '2021-11-14 21:58:01.553000', 18, null", oneRow.trim());
    }

    @Test
    public void testPrettyRenderingDataGripParity() {
        QueryResult qr = new QueryResult();
        qr.getColumns().addAll(List.of("id", "created_at", "created_by", "entity_version", "last_modified_at"));
        qr.getRows().add(java.util.Arrays.asList("1331", "2021-11-14 21:58:01.553000", "1", "1", "2025-10-11 10:47:32.309319"));
        qr.getRows().add(java.util.Arrays.asList("14439", "2021-11-14 21:54:58.867000", "1", "0", null));

        String pretty = DataExporters.render(DataExporters.Format.PRETTY, qr, "appointment_tx", 10);
        assertTrue(pretty.contains("+-----+--------------------------+----------+--------------+--------------------------+"));
        assertTrue(pretty.contains("|id   |created_at                |created_by|entity_version|last_modified_at          |"));
        assertTrue(pretty.contains("|1331 |2021-11-14 21:58:01.553000|1         |1             |2025-10-11 10:47:32.309319|"));
        assertTrue(pretty.contains("|14439|2021-11-14 21:54:58.867000|1         |0             |null                      |"));
    }

    @Test
    public void testSqlInsertMultirowRenderingDataGripParity() {
        QueryResult qr = new QueryResult();
        qr.getColumns().addAll(List.of("id", "created_at", "created_by", "status"));
        qr.getRows().add(java.util.Arrays.asList("1331", "2021-11-14 21:58:01.553000", "1", null));
        qr.getRows().add(java.util.Arrays.asList("1579", "2021-12-26 09:46:53.330000", "1", "18"));

        String sql = DataExporters.render(DataExporters.Format.SQL_INSERT_MULTIROW, qr, "erpdb.public.appointment_tx", 10);
        assertTrue(sql.startsWith("insert into public.appointment_tx (id, created_at, created_by, status)\n"));
        assertTrue(sql.contains("values (1331, '2021-11-14 21:58:01.553000', 1, null),\n"));
        assertTrue(sql.contains("       (1579, '2021-12-26 09:46:53.330000', 1, 18);\n"));
    }

    @Test
    public void testSqlInsertStatementsRenderingDataGripParity() {
        QueryResult qr = new QueryResult();
        qr.getColumns().addAll(List.of("id", "created_at", "created_by"));
        qr.getRows().add(java.util.Arrays.asList("1331", "2021-11-14 21:58:01.553000", "1"));
        qr.getRows().add(java.util.Arrays.asList("1579", "2021-12-26 09:46:53.330000", "1"));

        String sql = DataExporters.render(DataExporters.Format.SQL_INSERT_STATEMENTS, qr, "erpdb.public.appointment_tx", 10);
        assertTrue(sql.contains("insert into public.appointment_tx (id, created_at, created_by) values (1331, '2021-11-14 21:58:01.553000', 1);\n"));
        assertTrue(sql.contains("insert into public.appointment_tx (id, created_at, created_by) values (1579, '2021-12-26 09:46:53.330000', 1);\n"));
    }

    @Test
    public void testXmlRenderingDataGripParity() {
        QueryResult qr = new QueryResult();
        qr.getColumns().addAll(List.of("id", "remarks"));
        qr.getRows().add(java.util.Arrays.asList("1331", null));

        String xml = DataExporters.render(DataExporters.Format.XML, qr, "erpdb.public.appointment_tx", 10);
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<data>\n<row>\n"));
        assertTrue(xml.contains("    <id>1331</id>\n"));
        assertTrue(xml.contains("    <remarks>null</remarks>\n"));
        assertTrue(xml.endsWith("</row>\n</data>\n"));
    }
}

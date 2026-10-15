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
            assertFalse(rendered.isEmpty(), "Render output should not be empty for format: " + format);
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
        assertTrue(md.contains("| --- | --- | --- | --- | --- |"));
        assertTrue(md.contains("| 1 | Alice | 95.5 | true |"));
    }

    @Test
    public void testPrettyRendering() {
        String pretty = DataExporters.render(DataExporters.Format.PRETTY, sampleResult, "students", 10);
        assertTrue(pretty.contains("+"));
        assertTrue(pretty.contains("| id"));
        assertTrue(pretty.contains("| Alice"));
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
        assertTrue(sql.contains("INSERT INTO students"));
        assertTrue(sql.contains("VALUES\n  ("));
        assertTrue(sql.contains("Alice"));
    }

    @Test
    public void testHtmlRendering() {
        String htmlGroovy = DataExporters.render(DataExporters.Format.HTML_GROOVY, sampleResult, "students", 10);
        assertTrue(htmlGroovy.contains("<table>"));
        assertTrue(htmlGroovy.contains("<th>name</th>"));
        assertTrue(htmlGroovy.contains("<td>Alice</td>"));

        String htmlJs = DataExporters.render(DataExporters.Format.HTML_JS, sampleResult, "students", 10);
        assertTrue(htmlJs.contains("<!DOCTYPE html>"));
        assertTrue(htmlJs.contains("<table>"));
    }

    @Test
    public void testXmlRendering() {
        String xml = DataExporters.render(DataExporters.Format.XML, sampleResult, "students", 10);
        assertTrue(xml.contains("<data>"));
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("<name>Alice</name>"));
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
        assertEquals(".tsv", DataExporters.defaultExtension(DataExporters.Format.TSV));
        assertEquals(".json", DataExporters.defaultExtension(DataExporters.Format.JSON));
        assertEquals(".xlsx", DataExporters.defaultExtension(DataExporters.Format.EXCEL));
        assertEquals(".sql", DataExporters.defaultExtension(DataExporters.Format.SQL_INSERTS));
        assertEquals(".sql", DataExporters.defaultExtension(DataExporters.Format.SQL_INSERT_MULTIROW));
        assertEquals(".md", DataExporters.defaultExtension(DataExporters.Format.MARKDOWN));
        assertEquals(".py", DataExporters.defaultExtension(DataExporters.Format.PYTHON_DATAFRAME));
        assertEquals(".html", DataExporters.defaultExtension(DataExporters.Format.HTML_GROOVY));
        assertEquals(".xml", DataExporters.defaultExtension(DataExporters.Format.XML));
    }
}

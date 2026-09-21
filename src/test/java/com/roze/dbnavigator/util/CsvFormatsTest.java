package com.roze.dbnavigator.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.CsvFormatConfig;
import com.roze.dbnavigator.db.AppSettingsStore.QuotationRule;
import com.roze.dbnavigator.db.AppSettingsStore.Settings;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CsvFormatsTest {

    @Test
    public void testDefaultFormatsExist() {
        List<CsvFormatConfig> defaults = Settings.defaultCsvFormats();
        assertEquals(4, defaults.size());

        assertEquals("CSV", defaults.get(0).getName());
        assertEquals("Comma", defaults.get(0).getValueSeparator());
        assertEquals("Newline", defaults.get(0).getRowSeparator());

        assertEquals("TSV", defaults.get(1).getName());
        assertEquals("Tab", defaults.get(1).getValueSeparator());

        assertEquals("Pipe-separated", defaults.get(2).getName());
        assertEquals("Pipe", defaults.get(2).getValueSeparator());

        assertEquals("Semicolon-separated", defaults.get(3).getName());
        assertEquals("Semicolon", defaults.get(3).getValueSeparator());
    }

    @Test
    public void testFormatStandardCsv() {
        CsvFormatConfig config = new CsvFormatConfig(
                "CSV", "Comma", "Newline", "Empty string",
                CsvFormatConfig.defaultQuotationRules(), "When needed", false, false, false);

        List<String> headers = List.of("id", "name", "city");
        List<List<String>> rows = List.of(
                List.of("1", "Alice", "New York"),
                List.of("2", "Bob", "San Francisco")
        );

        String formatted = CsvFormatEngine.formatData(headers, rows, config, true);
        String expected = "id,name,city\n1,Alice,New York\n2,Bob,San Francisco";
        assertEquals(expected, formatted);
    }

    @Test
    public void testFormatTsv() {
        CsvFormatConfig config = new CsvFormatConfig(
                "TSV", "Tab", "Newline", "Empty string",
                CsvFormatConfig.defaultQuotationRules(), "When needed", false, false, false);

        List<String> headers = List.of("c1", "c2");
        List<List<String>> rows = List.of(List.of("v1", "v2"));

        String formatted = CsvFormatEngine.formatData(headers, rows, config, true);
        assertEquals("c1\tv2".replace("v2", "c2\nv1\tv2"), formatted);
    }

    @Test
    public void testFormatPipeSeparated() {
        CsvFormatConfig config = new CsvFormatConfig(
                "Pipe-separated", "Pipe", "Newline", "Empty string",
                CsvFormatConfig.defaultQuotationRules(), "When needed", false, false, false);

        List<String> headers = List.of("col1", "col2");
        List<List<String>> rows = List.of(List.of("val1", "val2"));

        String formatted = CsvFormatEngine.formatData(headers, rows, config, true);
        assertEquals("col1|col2\nval1|val2", formatted);
    }

    @Test
    public void testQuoteValuesModes() {
        // When needed: quotes only when value contains delimiter, quote, or newline
        CsvFormatConfig whenNeeded = new CsvFormatConfig(
                "CSV", "Comma", "Newline", "Empty string",
                CsvFormatConfig.defaultQuotationRules(), "When needed", false, false, false);

        assertEquals("simple", CsvFormatEngine.formatValue("simple", whenNeeded, true));
        assertEquals("\"with,comma\"", CsvFormatEngine.formatValue("with,comma", whenNeeded, true));
        assertEquals("\"with \"\"quotes\"\"\"", CsvFormatEngine.formatValue("with \"quotes\"", whenNeeded, true));

        // Always: quotes every value
        CsvFormatConfig always = new CsvFormatConfig(
                "CSV", "Comma", "Newline", "Empty string",
                CsvFormatConfig.defaultQuotationRules(), "Always", false, false, false);

        assertEquals("\"simple\"", CsvFormatEngine.formatValue("simple", always, true));
        assertEquals("\"with,comma\"", CsvFormatEngine.formatValue("with,comma", always, true));

        // Never: does not quote even with commas
        CsvFormatConfig never = new CsvFormatConfig(
                "CSV", "Comma", "Newline", "Empty string",
                CsvFormatConfig.defaultQuotationRules(), "Never", false, false, false);

        assertEquals("simple", CsvFormatEngine.formatValue("simple", never, true));
        assertEquals("with,comma", CsvFormatEngine.formatValue("with,comma", never, true));
    }

    @Test
    public void testQuoteEscapeModes() {
        // Duplicate escape
        CsvFormatConfig dupConfig = new CsvFormatConfig(
                "CSV", "Comma", "Newline", "Empty string",
                List.of(new QuotationRule("\"", "\"", "duplicate")),
                "When needed", false, false, false);

        assertEquals("\"hello \"\"world\"\"\"", CsvFormatEngine.formatValue("hello \"world\"", dupConfig, true));

        // Slash escape
        CsvFormatConfig slashConfig = new CsvFormatConfig(
                "CSV", "Comma", "Newline", "Empty string",
                List.of(new QuotationRule("\"", "\"", "slash")),
                "When needed", false, false, false);

        assertEquals("\"hello \\\"world\\\"\"", CsvFormatEngine.formatValue("hello \"world\"", slashConfig, true));
    }

    @Test
    public void testNullValueRepresentation() {
        CsvFormatConfig emptyNull = new CsvFormatConfig();
        emptyNull.setNullValueText("Empty string");
        assertEquals("", CsvFormatEngine.formatValue(null, emptyNull, true));

        CsvFormatConfig slashNNull = new CsvFormatConfig();
        slashNNull.setNullValueText("\\N");
        assertEquals("\\N", CsvFormatEngine.formatValue(null, slashNNull, true));

        CsvFormatConfig customNull = new CsvFormatConfig();
        customNull.setNullValueText("NULL");
        assertEquals("NULL", CsvFormatEngine.formatValue(null, customNull, true));
    }

    @Test
    public void testRowPrefixAndSuffix() {
        CsvFormatConfig config = new CsvFormatConfig();
        config.setRowPrefix("( ");
        config.setRowSuffix(" ),");

        List<List<String>> rows = List.of(
                List.of("1", "A"),
                List.of("2", "B")
        );

        String formatted = CsvFormatEngine.formatData(null, rows, config, false);
        assertEquals("( 1,A ),\n( 2,B ),", formatted);
    }

    @Test
    public void testParseDataFirstRowAsHeader() {
        CsvFormatConfig config = new CsvFormatConfig();
        config.setFirstRowIsHeader(true);

        String raw = "colA,colB,colC\nval1,val2,val3\nval4,val5,val6";
        CsvFormatEngine.ParsedTable parsed = CsvFormatEngine.parseData(raw, config);

        assertEquals(List.of("colA", "colB", "colC"), parsed.headers());
        assertEquals(2, parsed.rows().size());
        assertEquals(List.of("val1", "val2", "val3"), parsed.rows().get(0));
        assertEquals(List.of("val4", "val5", "val6"), parsed.rows().get(1));
    }

    @Test
    public void testParseDataFirstRowNotHeader() {
        CsvFormatConfig config = new CsvFormatConfig();
        config.setFirstRowIsHeader(false);

        String raw = "colA,colB\nval1,val2";
        CsvFormatEngine.ParsedTable parsed = CsvFormatEngine.parseData(raw, config);

        // Headers generated as C1, C2
        assertEquals(List.of("C1", "C2"), parsed.headers());
        // Data contains both rows
        assertEquals(2, parsed.rows().size());
        assertEquals(List.of("colA", "colB"), parsed.rows().get(0));
        assertEquals(List.of("val1", "val2"), parsed.rows().get(1));
    }

    @Test
    public void testTrimWhitespaces() {
        CsvFormatConfig trimConfig = new CsvFormatConfig();
        trimConfig.setTrimWhitespaces(true);

        String raw = " alpha , beta , gamma ";
        CsvFormatEngine.ParsedTable parsed = CsvFormatEngine.parseData(raw, trimConfig);

        assertEquals(List.of("alpha", "beta", "gamma"), parsed.rows().get(0));
    }

    @Test
    public void testSettingsStoreJsonSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        Settings settings = new Settings();
        List<CsvFormatConfig> list = new ArrayList<>();
        CsvFormatConfig custom = new CsvFormatConfig(
                "MyCustomFormat", "Semicolon", "Newline", "\\N",
                List.of(new QuotationRule("'", "'", "slash")),
                "Always", true, true, false
        );
        custom.setRowPrefix("[");
        custom.setRowSuffix("]");
        list.add(custom);
        settings.setCsvFormats(list);
        settings.setDefaultCsvFormat("MyCustomFormat");

        String json = mapper.writeValueAsString(settings);
        assertNotNull(json);

        Settings restored = mapper.readValue(json, Settings.class);
        assertEquals("MyCustomFormat", restored.getDefaultCsvFormat());
        assertEquals(1, restored.getCsvFormats().size());

        CsvFormatConfig rCustom = restored.getCsvFormats().get(0);
        assertEquals("MyCustomFormat", rCustom.getName());
        assertEquals("Semicolon", rCustom.getValueSeparator());
        assertEquals("Newline", rCustom.getRowSeparator());
        assertEquals("\\N", rCustom.getNullValueText());
        assertEquals("[", rCustom.getRowPrefix());
        assertEquals("]", rCustom.getRowSuffix());
        assertEquals("Always", rCustom.getQuoteValues());
        assertTrue(rCustom.isTrimWhitespaces());
        assertTrue(rCustom.isFirstRowIsHeader());
        assertFalse(rCustom.isFirstColumnIsHeader());

        assertEquals(1, rCustom.getQuotationRules().size());
        assertEquals("'", rCustom.getQuotationRules().get(0).getLeftQuote());
        assertEquals("'", rCustom.getQuotationRules().get(0).getRightQuote());
        assertEquals("slash", rCustom.getQuotationRules().get(0).getEscapeMode());
    }
}

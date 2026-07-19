package com.roze.dbnavigator.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes a single-sheet .xlsx file by hand — an XLSX is just a zip of a few
 * small XML files, so no Apache POI (or any other new dependency) is needed
 * for a flat "export this table's rows" use case. Every cell is written as
 * an inline string, which keeps this simple and is valid OOXML; it's not as
 * compact as using a shared-strings table, but for typical export sizes
 * that's an acceptable trade-off for zero extra dependencies.
 */
public final class XlsxWriter {

    private XlsxWriter() {}

    public static void write(File file, List<String> columns, List<List<String>> rows) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(file.toPath()))) {
            entry(zip, "[Content_Types].xml", CONTENT_TYPES);
            entry(zip, "_rels/.rels", RELS);
            entry(zip, "xl/workbook.xml", WORKBOOK);
            entry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
            entry(zip, "xl/styles.xml", STYLES);
            entry(zip, "xl/worksheets/sheet1.xml", buildSheet(columns, rows));
        }
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        write(zip, content);
        zip.closeEntry();
    }

    private static void write(OutputStream out, String content) throws IOException {
        out.write(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String buildSheet(List<String> columns, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<sheetData>");

        sb.append(row(1, columns, true));
        for (int r = 0; r < rows.size(); r++) {
            sb.append(row(r + 2, rows.get(r), false));
        }

        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private static String row(int rowIndex, List<String> values, boolean header) {
        StringBuilder sb = new StringBuilder("<row r=\"").append(rowIndex).append("\">");
        for (int c = 0; c < values.size(); c++) {
            String ref = columnRef(c) + rowIndex;
            String value = values.get(c);
            sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"");
            if (header) sb.append(" s=\"1\"");
            sb.append("><is><t xml:space=\"preserve\">")
              .append(escape(value == null ? "" : value))
              .append("</t></is></c>");
        }
        return sb.append("</row>").toString();
    }

    /** 0 -> A, 1 -> B, ... 26 -> AA, matching spreadsheet column references. */
    private static String columnRef(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        do {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final String CONTENT_TYPES =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String RELS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static final String WORKBOOK =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets><sheet name=\"Export\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";

    private static final String WORKBOOK_RELS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

    // Style index 0 = default; style index 1 = bold (for the header row)
    private static final String STYLES =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"1\"><fill><patternFill patternType=\"none\"/></fill></fills>"
            + "<borders count=\"1\"><border/></borders>"
            + "<cellStyleXfs count=\"1\"><xf/></cellStyleXfs>"
            + "<cellXfs count=\"2\"><xf fontId=\"0\"/><xf fontId=\"1\"/></cellXfs>"
            + "</styleSheet>";
}

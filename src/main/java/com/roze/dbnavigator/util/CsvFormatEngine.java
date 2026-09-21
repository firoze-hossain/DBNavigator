package com.roze.dbnavigator.util;

import com.roze.dbnavigator.db.AppSettingsStore.CsvFormatConfig;
import com.roze.dbnavigator.db.AppSettingsStore.QuotationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DataGrip-style dynamic CSV / delimited format engine.
 * Handles custom value separators, row separators, quotation rules,
 * null representations, row prefixes/suffixes, and whitespace trimming.
 */
public final class CsvFormatEngine {

    public record ParsedTable(List<String> headers, List<List<String>> rows) {}

    public static final List<String> SAMPLE_HEADERS = List.of(
            "customer_id", "first_name", "last_name", "active", "create_date"
    );

    public static final List<List<String>> SAMPLE_ROWS = List.of(
            List.of("1", "MARY", "SMITH", "true", "2006-02-14"),
            List.of("2", "PATRICIA", "JOHNSON", "true", "2006-02-14"),
            List.of("3", "LINDA", "WILLIAMS", "true", "2006-02-14"),
            List.of("4", "BARBARA", "JONES", "false", "2006-02-14"),
            List.of("5", "ELIZABETH", "BROWN", "false", "2006-02-14"),
            List.of("6", "JENNIFER", "DAVIS", "true", "2006-02-14"),
            List.of("7", "MARIA", "MILLER", "false", "2006-02-14"),
            List.of("8", "SUSAN", "WILSON", "true", "2006-02-14"),
            List.of("9", "MARGARET", "MOORE", "true", "2006-02-14"),
            List.of("10", "DOROTHY", "TAYLOR", "true", "2006-02-14"),
            List.of("11", "LISA", "ANDERSON", "true", "2006-02-14"),
            List.of("12", "NANCY", "THOMAS", "true", "2006-02-14")
    );

    private CsvFormatEngine() {}

    /** Converts DataGrip dropdown labels ("Comma", "Tab", etc.) or custom string to literal separator. */
    public static String resolveSeparator(String text) {
        if (text == null || text.isEmpty()) return ",";
        String s = text.trim();
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "comma" -> ",";
            case "tab" -> "\t";
            case "semicolon" -> ";";
            case "pipe" -> "|";
            case "space" -> " ";
            case "newline" -> "\n";
            default -> unescapeString(text);
        };
    }

    public static String unescapeString(String str) {
        if (str == null) return "";
        return str.replace("\\t", "\t")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\\"", "\"")
                  .replace("\\'", "'");
    }

    /** Formats a tabular result into delimited text based on the provided CsvFormatConfig. */
    public static String formatData(List<String> headers, List<List<String>> rows, CsvFormatConfig config, boolean includeHeader) {
        if (config == null) return "";
        String valSep = resolveSeparator(config.getValueSeparator());
        String rowSep = resolveSeparator(config.getRowSeparator());
        String prefix = config.getRowPrefix() != null ? config.getRowPrefix() : "";
        String suffix = config.getRowSuffix() != null ? config.getRowSuffix() : "";

        StringBuilder sb = new StringBuilder();

        if (includeHeader && headers != null && !headers.isEmpty()) {
            sb.append(prefix);
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0) sb.append(valSep);
                sb.append(formatValue(headers.get(i), config, false));
            }
            sb.append(suffix);
        }

        if (rows != null) {
            for (List<String> row : rows) {
                if (sb.length() > 0) {
                    sb.append(rowSep);
                }
                sb.append(prefix);
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) sb.append(valSep);
                    sb.append(formatValue(row.get(i), config, true));
                }
                sb.append(suffix);
            }
        }

        return sb.toString();
    }

    /** Formats a single value according to quotation rules and null representation. */
    public static String formatValue(String value, CsvFormatConfig config, boolean isDataCell) {
        if (value == null) {
            String nullText = config.getNullValueText();
            if (nullText == null || nullText.equalsIgnoreCase("Empty string") || nullText.equalsIgnoreCase("Undefined")) {
                return "";
            }
            if (nullText.equals("\\N")) return "\\N";
            return nullText;
        }

        List<QuotationRule> rules = config.getQuotationRules();
        QuotationRule primaryRule = (rules != null && !rules.isEmpty()) ? rules.get(0) : new QuotationRule("\"", "\"", "duplicate");
        String lQuote = primaryRule.getLeftQuote() != null ? primaryRule.getLeftQuote() : "\"";
        String rQuote = primaryRule.getRightQuote() != null ? primaryRule.getRightQuote() : "\"";
        String escapeMode = primaryRule.getEscapeMode() != null ? primaryRule.getEscapeMode() : "duplicate";

        String quoteMode = config.getQuoteValues() != null ? config.getQuoteValues() : "When needed";
        boolean alwaysQuote = "Always".equalsIgnoreCase(quoteMode);
        boolean neverQuote = "Never".equalsIgnoreCase(quoteMode);

        String valSep = resolveSeparator(config.getValueSeparator());
        String rowSep = resolveSeparator(config.getRowSeparator());

        boolean needsQuote = alwaysQuote || (!neverQuote && (
                value.contains(valSep) ||
                value.contains(rowSep) ||
                value.contains(lQuote) ||
                value.contains(rQuote) ||
                value.contains("\n") ||
                value.contains("\r") ||
                value.startsWith(" ") ||
                value.endsWith(" ")
        ));

        if (!needsQuote) {
            return value;
        }

        String escaped = escapeQuote(value, rQuote, escapeMode);
        return lQuote + escaped + rQuote;
    }

    private static String escapeQuote(String value, String quoteChar, String escapeMode) {
        if (quoteChar == null || quoteChar.isEmpty()) return value;
        if ("slash".equalsIgnoreCase(escapeMode) || "backslash".equalsIgnoreCase(escapeMode)) {
            return value.replace("\\", "\\\\").replace(quoteChar, "\\" + quoteChar);
        } else {
            // duplicate
            return value.replace(quoteChar, quoteChar + quoteChar);
        }
    }

    /**
     * Parses raw text into a ParsedTable according to CsvFormatConfig.
     * Accurately tracks quotes, row prefixes/suffixes, and handles firstRowIsHeader.
     */
    public static ParsedTable parseData(String rawText, CsvFormatConfig config) {
        if (rawText == null || rawText.isEmpty()) {
            return new ParsedTable(List.of(), List.of());
        }

        String rowSep = resolveSeparator(config.getRowSeparator());
        String valSep = resolveSeparator(config.getValueSeparator());
        String prefix = config.getRowPrefix() != null ? config.getRowPrefix() : "";
        String suffix = config.getRowSuffix() != null ? config.getRowSuffix() : "";
        boolean trim = config.isTrimWhitespaces();
        String nullText = config.getNullValueText();

        List<QuotationRule> rules = config.getQuotationRules();
        QuotationRule rule = (rules != null && !rules.isEmpty()) ? rules.get(0) : new QuotationRule("\"", "\"", "duplicate");
        String lQuote = rule.getLeftQuote() != null ? rule.getLeftQuote() : "\"";
        String rQuote = rule.getRightQuote() != null ? rule.getRightQuote() : "\"";
        boolean slashEscape = "slash".equalsIgnoreCase(rule.getEscapeMode()) || "backslash".equalsIgnoreCase(rule.getEscapeMode());

        List<List<String>> allRows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();

        boolean inQuotes = false;
        int n = rawText.length();
        int valSepLen = valSep.length();
        int rowSepLen = rowSep.length();

        int i = 0;
        while (i < n) {
            if (!inQuotes) {
                // Check prefix at start of line
                if (!prefix.isEmpty() && currentRow.isEmpty() && currentField.length() == 0) {
                    if (rawText.startsWith(prefix, i)) {
                        i += prefix.length();
                        continue;
                    }
                }
                // Check suffix at end of line (before rowSep or EOF)
                if (!suffix.isEmpty() && rawText.startsWith(suffix, i)) {
                    int nextIdx = i + suffix.length();
                    if (nextIdx == n || rawText.startsWith(rowSep, nextIdx)) {
                        i = nextIdx;
                        continue;
                    }
                }
            }

            // Check quote start
            if (!inQuotes && currentField.length() == 0 && rawText.startsWith(lQuote, i)) {
                inQuotes = true;
                i += lQuote.length();
                continue;
            }

            // Check quote end or escape
            if (inQuotes) {
                if (slashEscape && rawText.startsWith("\\", i) && i + 1 < n) {
                    currentField.append(rawText.charAt(i + 1));
                    i += 2;
                    continue;
                } else if (!slashEscape && rawText.startsWith(rQuote + rQuote, i)) {
                    currentField.append(rQuote);
                    i += rQuote.length() * 2;
                    continue;
                } else if (rawText.startsWith(rQuote, i)) {
                    inQuotes = false;
                    i += rQuote.length();
                    continue;
                }
                currentField.append(rawText.charAt(i));
                i++;
                continue;
            }

            // Outside quotes: check value separator
            if (rawText.startsWith(valSep, i)) {
                String val = currentField.toString();
                if (trim) val = val.trim();
                val = resolveNull(val, nullText);
                currentRow.add(val);
                currentField.setLength(0);
                i += valSepLen;
                continue;
            }

            // Outside quotes: check row separator
            if (rawText.startsWith(rowSep, i)) {
                String val = currentField.toString();
                if (trim) val = val.trim();
                val = resolveNull(val, nullText);
                currentRow.add(val);
                currentField.setLength(0);
                allRows.add(new ArrayList<>(currentRow));
                currentRow.clear();
                i += rowSepLen;
                continue;
            }

            currentField.append(rawText.charAt(i));
            i++;
        }

        // Final field
        if (currentField.length() > 0 || !currentRow.isEmpty()) {
            String val = currentField.toString();
            if (trim) val = val.trim();
            val = resolveNull(val, nullText);
            currentRow.add(val);
            allRows.add(new ArrayList<>(currentRow));
        }

        if (allRows.isEmpty()) {
            return new ParsedTable(List.of(), List.of());
        }

        List<String> headers;
        List<List<String>> dataRows;

        if (config.isFirstRowIsHeader()) {
            headers = allRows.get(0);
            dataRows = allRows.size() > 1 ? allRows.subList(1, allRows.size()) : List.of();
        } else {
            int maxCols = 0;
            for (List<String> r : allRows) {
                if (r.size() > maxCols) maxCols = r.size();
            }
            headers = new ArrayList<>();
            for (int c = 1; c <= maxCols; c++) {
                headers.add("C" + c);
            }
            dataRows = allRows;
        }

        return new ParsedTable(headers, dataRows);
    }

    private static String resolveNull(String value, String nullText) {
        if (nullText != null && !nullText.isEmpty() && !nullText.equalsIgnoreCase("Empty string")) {
            if (value.equals(nullText)) return null;
        }
        return value;
    }
}

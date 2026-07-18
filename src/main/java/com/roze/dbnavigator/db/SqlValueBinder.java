package com.roze.dbnavigator.db;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Converts a grid's text representation of a value into the correctly typed
 * JDBC parameter, replacing ad-hoc SQL string-literal building. This is what
 * lets dates, booleans, numerics, and NULLs round-trip correctly through
 * PreparedStatement instead of being pasted into SQL text.
 */
public final class SqlValueBinder {

    private SqlValueBinder() {}

    private static final DateTimeFormatter[] TIMESTAMP_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]"),
    };

    /**
     * Binds one parameter, converting the grid's text form to the column's
     * real JDBC type. "NULL" (the grid's NULL placeholder) and blank/absent
     * type information fall back to setNull / setString respectively.
     *
     * @param jdbcType a java.sql.Types constant, or null when unknown (falls
     *                 back to treating the value as a plain string)
     */
    public static void bind(PreparedStatement stmt, int index, Integer jdbcType, String text)
            throws SQLException {
        if (text == null || text.equals("NULL")) {
            stmt.setNull(index, jdbcType == null ? Types.VARCHAR : jdbcType);
            return;
        }
        if (jdbcType == null) {
            stmt.setString(index, text);
            return;
        }

        try {
            switch (jdbcType) {
                case Types.BIT, Types.BOOLEAN -> stmt.setBoolean(index, parseBoolean(text));
                case Types.TINYINT, Types.SMALLINT -> stmt.setShort(index, Short.parseShort(text.trim()));
                case Types.INTEGER -> stmt.setInt(index, Integer.parseInt(text.trim()));
                case Types.BIGINT -> stmt.setLong(index, Long.parseLong(text.trim()));
                case Types.FLOAT, Types.REAL -> stmt.setFloat(index, Float.parseFloat(text.trim()));
                case Types.DOUBLE -> stmt.setDouble(index, Double.parseDouble(text.trim()));
                case Types.DECIMAL, Types.NUMERIC -> stmt.setBigDecimal(index, new BigDecimal(text.trim()));
                case Types.DATE -> stmt.setDate(index, java.sql.Date.valueOf(parseDatePart(text)));
                case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE ->
                        stmt.setTimestamp(index, parseTimestamp(text));
                case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY ->
                        stmt.setBytes(index, hexOrRawBytes(text));
                default -> stmt.setString(index, text);
            }
        } catch (RuntimeException conversionFailure) {
            // If the typed conversion fails (bad input, unexpected format),
            // fall back to a plain string rather than losing the edit entirely —
            // the database will reject it with a clear error if it's truly wrong.
            stmt.setString(index, text);
        }
    }

    private static boolean parseBoolean(String text) {
        String t = text.trim().toLowerCase(Locale.ROOT);
        return t.equals("true") || t.equals("t") || t.equals("1") || t.equals("yes");
    }

    private static LocalDate parseDatePart(String text) {
        String datePart = text.length() >= 10 ? text.substring(0, 10) : text;
        return LocalDate.parse(datePart);
    }

    private static Timestamp parseTimestamp(String text) {
        String normalized = text.trim();
        if (normalized.length() == 10) normalized += " 00:00:00";   // date-only input
        for (DateTimeFormatter fmt : TIMESTAMP_FORMATS) {
            try {
                return Timestamp.valueOf(LocalDateTime.parse(normalized, fmt));
            } catch (Exception ignored) { /* try next format */ }
        }
        return Timestamp.valueOf(normalized.replace('T', ' '));
    }

    private static byte[] hexOrRawBytes(String text) {
        String t = text.trim();
        if (t.startsWith("\\x") || t.startsWith("0x")) {
            String hex = t.substring(2);
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return bytes;
        }
        return t.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}

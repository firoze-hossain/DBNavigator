package com.roze.dbnavigator.util;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.Settings;
import com.roze.dbnavigator.db.AppSettingsStore.UserParameterPattern;
import com.roze.dbnavigator.model.ConnectionProfile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataGrip-style named parameters and comment-title parsing:
 * Dynamically recognizes parameters in SQL using configured User Parameter patterns
 * (e.g. :name, ${name}, #name#, %(name)s, :'name', etc.), prompts for values,
 * and substitutes them before execution.
 *
 * Also extracts query titles from preceding comments (-- or /* *\/) matching DataGrip's
 * "Create title for results from comment before query" feature.
 */
public final class SqlParameters {

    /** One distinct parameter, with a best-effort guess at the column it's compared against. */
    public record Parameter(String name, String guessedColumn) {}

    public record TokenRange(int start, int end, String name) {}

    private static final Pattern COLUMN_COMPARISON = Pattern.compile(
            "([A-Za-z_][A-Za-z0-9_.]*)\\s*(?:=|<>|!=|>=|<=|>|<|IN)\\s*[:$#%](?:\\{([A-Za-z0-9_]+)\\}|\\(([A-Za-z0-9_]+)\\)s|'([A-Za-z0-9_]+)'|([A-Za-z0-9_]+))",
            Pattern.CASE_INSENSITIVE);

    private SqlParameters() {}

    /** Distinct parameters in the order they first appear using current settings. */
    public static List<Parameter> detect(String sql) {
        return detect(sql, AppSettingsStore.load(), null);
    }

    public static List<Parameter> detect(String sql, Settings settings, ConnectionProfile profile) {
        if (sql == null || sql.isBlank()) return List.of();
        if (settings != null && !settings.isEnableUserParameters()) return List.of();

        List<TokenRange> ranges = findTokenRanges(sql, settings, profile);
        if (ranges.isEmpty()) return List.of();

        Set<String> names = new LinkedHashSet<>();
        for (TokenRange r : ranges) {
            names.add(r.name());
        }

        Map<String, String> guesses = guessColumns(sql);
        List<Parameter> result = new ArrayList<>();
        for (String name : names) {
            result.add(new Parameter(name, guesses.get(name)));
        }
        return result;
    }

    /** Replaces every matched parameter placeholder with its typed literal value. */
    public static String substitute(String sql, Map<String, String> values) {
        return substitute(sql, values, AppSettingsStore.load(), null);
    }

    public static String substitute(String sql, Map<String, String> values, Settings settings, ConnectionProfile profile) {
        if (sql == null || values == null || values.isEmpty()) return sql;
        List<TokenRange> ranges = findTokenRanges(sql, settings, profile);
        if (ranges.isEmpty()) return sql;

        StringBuilder out = new StringBuilder();
        int lastEnd = 0;
        for (TokenRange range : ranges) {
            out.append(sql, lastEnd, range.start());
            String val = values.get(range.name());
            out.append(literal(val));
            lastEnd = range.end();
        }
        out.append(sql.substring(lastEnd));
        return out.toString();
    }

    /**
     * Extracts a title from a leading comment preceding the SQL query,
     * e.g. "-- Top 10 Customers" -> "Top 10 Customers".
     */
    public static String extractPrecedingCommentTitle(String sql, String treatAfterText) {
        if (sql == null || sql.isBlank()) return null;
        String s = sql.stripLeading();
        String commentText = null;
        if (s.startsWith("--")) {
            int eol = s.indexOf('\n');
            if (eol != -1) {
                commentText = s.substring(2, eol).trim();
            } else {
                commentText = s.substring(2).trim();
            }
        } else if (s.startsWith("/*")) {
            int end = s.indexOf("*/");
            if (end != -1) {
                commentText = s.substring(2, end).trim();
            }
        }
        if (commentText == null || commentText.isBlank()) return null;

        if (treatAfterText != null && !treatAfterText.isBlank() && !treatAfterText.equalsIgnoreCase("comment beginning")) {
            int idx = commentText.indexOf(treatAfterText);
            if (idx != -1) {
                commentText = commentText.substring(idx + treatAfterText.length()).trim();
            }
        }
        if (commentText.length() > 50) {
            commentText = commentText.substring(0, 47) + "…";
        }
        return commentText.isBlank() ? null : commentText;
    }

    private static Map<String, String> guessColumns(String sql) {
        Map<String, String> guesses = new LinkedHashMap<>();
        try {
            Matcher matcher = COLUMN_COMPARISON.matcher(sql);
            while (matcher.find()) {
                String col = matcher.group(1);
                for (int g = 2; g <= matcher.groupCount(); g++) {
                    String param = matcher.group(g);
                    if (param != null && !param.isBlank()) {
                        guesses.putIfAbsent(param, col);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
        return guesses;
    }

    private record CompiledRule(Pattern pattern, boolean inScripts, boolean inLiterals) {}

    public static List<TokenRange> findTokenRanges(String sql, Settings settings, ConnectionProfile profile) {
        List<TokenRange> ranges = new ArrayList<>();
        if (sql == null || sql.isEmpty()) return ranges;

        Settings currentSettings = (settings != null) ? settings : AppSettingsStore.load();
        if (!currentSettings.isEnableUserParameters()) return ranges;

        List<UserParameterPattern> patternConfigs = currentSettings.getUserParameterPatterns();
        if (patternConfigs == null || patternConfigs.isEmpty()) {
            patternConfigs = Settings.defaultUserParameterPatterns();
        }

        String dbTypeName = (profile != null && profile.getType() != null)
                ? profile.getType().name().toUpperCase(Locale.ROOT) : "";

        List<CompiledRule> rules = new ArrayList<>();
        for (UserParameterPattern cfg : patternConfigs) {
            if (!cfg.isEnabled()) continue;
            if (!isLanguageApplicable(cfg.getLanguages(), dbTypeName)) continue;
            Pattern p = compilePattern(cfg.getPattern());
            if (p != null) {
                rules.add(new CompiledRule(p, cfg.isInScripts(), cfg.isInLiterals()));
            }
        }
        if (rules.isEmpty()) return ranges;

        boolean allowInsideStrings = currentSettings.isSubstituteInsideSqlStrings();

        int n = sql.length();
        boolean inSingleQuote = false, inDoubleQuote = false, inLineComment = false, inBlockComment = false;

        int i = 0;
        while (i < n) {
            char c = sql.charAt(i);
            char next = i + 1 < n ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                i++;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') { inBlockComment = false; i += 2; continue; }
                i++;
                continue;
            }
            if (inSingleQuote) {
                if (c == '\'' && next == '\'') { i += 2; continue; }
                if (c == '\'') { inSingleQuote = false; i++; continue; }
            } else if (inDoubleQuote) {
                if (c == '"' && next == '"') { i += 2; continue; }
                if (c == '"') { inDoubleQuote = false; i++; continue; }
            } else {
                if (c == '-' && next == '-') { inLineComment = true; i += 2; continue; }
                if (c == '/' && next == '*') { inBlockComment = true; i += 2; continue; }
                if (c == '\'') { inSingleQuote = true; i++; continue; }
                if (c == '"') { inDoubleQuote = true; i++; continue; }
            }

            // Test if any rule matches at current index
            boolean canMatchInContext = inSingleQuote
                    ? (allowInsideStrings)
                    : (!inDoubleQuote);

            if (canMatchInContext) {
                boolean matched = false;
                for (CompiledRule rule : rules) {
                    if (inSingleQuote && !rule.inLiterals && !allowInsideStrings) continue;
                    if (!inSingleQuote && !rule.inScripts) continue;

                    Matcher m = rule.pattern.matcher(sql);
                    m.region(i, n);
                    if (m.lookingAt()) {
                        int start = m.start();
                        int end = m.end();
                        String paramName = m.groupCount() >= 1 ? m.group(1) : m.group();
                        if (paramName != null && !paramName.isBlank()) {
                            ranges.add(new TokenRange(start, end, paramName));
                            i = end;
                            matched = true;
                            break;
                        }
                    }
                }
                if (matched) continue;
            }
            i++;
        }

        return ranges;
    }

    private static boolean isLanguageApplicable(String languages, String dbTypeName) {
        if (languages == null || languages.isBlank() || languages.equalsIgnoreCase("All languages")
                || languages.equalsIgnoreCase("everywhere")) {
            return true;
        }
        String upper = languages.toUpperCase(Locale.ROOT);
        if (upper.contains("ALL EXCL. SQL")) {
            return false;
        }
        if (dbTypeName != null && !dbTypeName.isEmpty()) {
            return upper.contains(dbTypeName);
        }
        return true;
    }

    public static Pattern compilePattern(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String p = raw.trim().replaceAll("^\"|\"$", "");
        if (p.isEmpty()) return null;

        try {
            if (p.equals(":name")) {
                return Pattern.compile("(?<!:):([A-Za-z_][A-Za-z0-9_]*)(?!:)");
            }
            if (p.equals("${name}")) {
                return Pattern.compile("\\$\\{([A-Za-z0-9_.]+)\\}");
            }
            if (p.equals("$name")) {
                return Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)");
            }
            if (p.equals("#name#")) {
                return Pattern.compile("#([A-Za-z0-9_.]+)#");
            }
            if (p.equals("%(name)s")) {
                return Pattern.compile("%\\(([A-Za-z0-9_]+)\\)s");
            }
            if (p.equals("%name")) {
                return Pattern.compile("%([A-Za-z_][A-Za-z0-9_]*)");
            }
            if (p.equals(":'name'")) {
                return Pattern.compile(":'([A-Za-z0-9_]+)'");
            }
            if (p.equals("$a.b.c$?")) {
                return Pattern.compile("\\$([A-Za-z0-9_.]+)\\$");
            }
            if (p.equals("#a.b.c#?")) {
                return Pattern.compile("#([A-Za-z0-9_.]+)#");
            }

            // Custom pattern containing placeholder "name" or "a.b.c"
            if (p.contains("name")) {
                String regex = Pattern.quote(p).replace("name", "\\E([A-Za-z0-9_]+)\\Q");
                return Pattern.compile(regex);
            }
            if (p.contains("a.b.c")) {
                String regex = Pattern.quote(p).replace("a.b.c", "\\E([A-Za-z0-9_.]+)\\Q");
                return Pattern.compile(regex);
            }
            // If user supplied regex directly with a capture group
            if (p.contains("(") && p.contains(")")) {
                return Pattern.compile(p);
            }
            return Pattern.compile(Pattern.quote(p));
        } catch (Exception e) {
            return null;
        }
    }

    private static String literal(String value) {
        if (value == null || value.isBlank()) return "NULL";
        if (value.matches("-?\\d+(\\.\\d+)?")) return value;
        return "'" + value.replace("'", "''") + "'";
    }
}

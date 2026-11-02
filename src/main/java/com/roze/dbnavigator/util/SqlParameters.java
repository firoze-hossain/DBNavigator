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

    /** One distinct parameter, with a best-effort guess at the column it's compared against and context snippet. */
    public record Parameter(String name, String guessedColumn, String contextSnippet) {
        public Parameter(String name, String guessedColumn) {
            this(name, guessedColumn, null);
        }
    }

    public record TokenRange(int start, int end, String name) {}

    private static final Pattern COLUMN_COMPARISON = Pattern.compile(
            "([A-Za-z_][A-Za-z0-9_.]*(?:::date|::text|::int|::[A-Za-z0-9_]+)?)\\s*(?:=|<>|!=|>=|<=|>|<|IN|BETWEEN|LIKE)\\s*[:$#%](?:\\{([A-Za-z0-9_]+)\\}|\\(([A-Za-z0-9_]+)\\)s|'([A-Za-z0-9_]+)'|([A-Za-z0-9_]+))(?:\\s+AND\\s+[:$#%](?:\\{([A-Za-z0-9_]+)\\}|\\(([A-Za-z0-9_]+)\\)s|'([A-Za-z0-9_]+)'|([A-Za-z0-9_]+)))?",
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
        Map<String, String> snippets = new LinkedHashMap<>();
        for (TokenRange r : ranges) {
            names.add(r.name());
            if (!snippets.containsKey(r.name())) {
                snippets.put(r.name(), extractContextSnippet(sql, r.start(), r.end()));
            }
        }

        Map<String, String> guesses = guessColumns(sql);
        List<Parameter> result = new ArrayList<>();
        for (String name : names) {
            result.add(new Parameter(name, guesses.get(name), snippets.get(name)));
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
            out.append(formatParameterValue(sql, range.start(), range.end(), val));
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
        String lower = commentText.toLowerCase(Locale.ROOT);
        if (lower.contains("ctrl+enter runs") || lower.contains("ctrl+space completes")
                || lower.matches(".*[a-z0-9_]+\\s+console\\.?$")) {
            return null;
        }

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

    /**
     * Extracts a clean contextual snippet surrounding a parameter placeholder,
     * replacing the parameter with '_' to show where it fits in the query (DataGrip style).
     * E.g. "a.status = _", "(_)" for IN clauses, "... ::date BETWEEN _ AND"
     */
    public static String extractContextSnippet(String sql, int paramStart, int paramEnd) {
        if (sql == null || paramStart < 0 || paramEnd > sql.length() || paramStart > paramEnd) {
            return "";
        }

        // If inside IN (...) clause immediately surrounded by parentheses, return DataGrip-style "(_)"
        if (isInClause(sql, paramStart, paramEnd)) {
            int p = paramStart - 1;
            while (p >= 0 && Character.isWhitespace(sql.charAt(p))) p--;
            int q = paramEnd;
            while (q < sql.length() && Character.isWhitespace(sql.charAt(q))) q++;
            if (p >= 0 && sql.charAt(p) == '(' && q < sql.length() && sql.charAt(q) == ')') {
                return "(_)";
            }
        }

        // Extract up to 24 chars before paramStart
        int beforeStart = Math.max(0, paramStart - 24);
        int lastNewline = sql.lastIndexOf('\n', paramStart - 1);
        if (lastNewline >= beforeStart) {
            beforeStart = lastNewline + 1;
        } else if (beforeStart > 0) {
            int space = sql.indexOf(' ', beforeStart);
            if (space != -1 && space < paramStart && space - beforeStart <= 8) {
                beforeStart = space + 1;
            }
        }
        String before = sql.substring(beforeStart, paramStart).replaceAll("\\s+", " ");
        if (beforeStart > 0 && !before.startsWith("...")) {
            before = "... " + before.stripLeading();
        }

        // Strip leading SQL clause conjunctions like "... WHERE " to focus on the column and operator (DataGrip style)
        String cleanedBefore = before;
        if (cleanedBefore.startsWith("... ")) {
            cleanedBefore = cleanedBefore.substring(4).stripLeading();
        }
        for (String conj : List.of("WHERE ", "where ", "AND ", "and ", "OR ", "or ", "HAVING ", "having ", "SET ", "set ", "ON ", "on ")) {
            if (cleanedBefore.startsWith(conj)) {
                cleanedBefore = cleanedBefore.substring(conj.length()).stripLeading();
                break;
            }
        }
        if (!cleanedBefore.isEmpty()) {
            before = cleanedBefore;
        }

        // Extract up to 16 chars after paramEnd (stopping at next parameter or newline)
        int afterEnd = Math.min(sql.length(), paramEnd + 16);
        int nextNewline = sql.indexOf('\n', paramEnd);
        if (nextNewline != -1 && nextNewline < afterEnd) {
            afterEnd = nextNewline;
        }
        int nextParam = sql.indexOf(':', paramEnd);
        if (nextParam != -1 && nextParam < afterEnd && nextParam > paramEnd) {
            afterEnd = nextParam;
        }
        String after = sql.substring(paramEnd, afterEnd).replaceAll("\\s+", " ");

        return (before + "_" + after).trim();
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

                // PostgreSQL type cast ::type or PL/SQL := assignment can never be a user parameter
                if (c == ':' && next == ':') {
                    while (i < n && sql.charAt(i) == ':') i++;
                    while (i < n && Character.isWhitespace(sql.charAt(i))) i++;
                    while (i < n && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) i++;
                    continue;
                }
                if (c == ':' && next == '=') {
                    i += 2;
                    continue;
                }
                if (c == ':' && i > 0 && sql.charAt(i - 1) == ':') {
                    i++;
                    continue;
                }
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
                    m.useTransparentBounds(true);
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

    public static boolean isInClause(String sql, int tokenStart, int tokenEnd) {
        if (sql == null || tokenStart < 0 || tokenEnd > sql.length() || tokenStart > tokenEnd) {
            return false;
        }

        // Walk backward from tokenStart to check if inside ( ... )
        int depth = 0;
        int openParenPos = -1;
        for (int i = tokenStart - 1; i >= 0; i--) {
            char c = sql.charAt(i);
            if (c == ')') {
                depth++;
            } else if (c == '(') {
                if (depth == 0) {
                    openParenPos = i;
                    break;
                }
                depth--;
            } else if (depth == 0 && (c == ';' || c == '\n')) {
                break;
            }
        }

        if (openParenPos != -1) {
            int p = openParenPos - 1;
            while (p >= 0 && Character.isWhitespace(sql.charAt(p))) p--;
            if (p >= 1) {
                int wordEnd = p + 1;
                while (p >= 0 && Character.isLetter(sql.charAt(p))) p--;
                String word = sql.substring(p + 1, wordEnd);
                if (word.equalsIgnoreCase("IN")) {
                    return true;
                }
            }
        } else {
            int p = tokenStart - 1;
            while (p >= 0 && Character.isWhitespace(sql.charAt(p))) p--;
            if (p >= 1) {
                int wordEnd = p + 1;
                while (p >= 0 && Character.isLetter(sql.charAt(p))) p--;
                String word = sql.substring(p + 1, wordEnd);
                if (word.equalsIgnoreCase("IN")) {
                    return true;
                }
            }
        }

        return false;
    }

    public static List<String> parseTokens(String val) {
        List<String> tokens = new ArrayList<>();
        if (val == null || val.isBlank()) return tokens;

        StringBuilder sb = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        int parenDepth = 0;

        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (c == '\'' && !inDouble) {
                if (inSingle && i + 1 < val.length() && val.charAt(i + 1) == '\'') {
                    sb.append("''");
                    i++;
                    continue;
                }
                inSingle = !inSingle;
                sb.append(c);
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                sb.append(c);
            } else if (!inSingle && !inDouble) {
                if (c == '(' || c == '[') {
                    parenDepth++;
                    sb.append(c);
                } else if (c == ')' || c == ']') {
                    if (parenDepth > 0) parenDepth--;
                    sb.append(c);
                } else if (c == ',' && parenDepth == 0) {
                    String part = sb.toString().trim();
                    if (!part.isEmpty()) {
                        tokens.add(part);
                    }
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        String lastPart = sb.toString().trim();
        if (!lastPart.isEmpty()) {
            tokens.add(lastPart);
        }
        return tokens;
    }

    public static boolean isLiteralToken(String token) {
        if (token == null || token.isBlank()) return false;
        if (token.matches("-?\\d+(\\.\\d+)?")) return true;
        if ((token.startsWith("'") && token.endsWith("'") && token.length() >= 2) ||
            (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2)) return true;
        if (token.equalsIgnoreCase("true") || token.equalsIgnoreCase("false") ||
            token.equalsIgnoreCase("null") || token.equalsIgnoreCase("<null>")) return true;
        return false;
    }

    public static String formatSingleLiteral(String single) {
        if (single == null || single.isBlank() || single.equalsIgnoreCase("<null>") || single.equalsIgnoreCase("null")) {
            return "NULL";
        }
        single = single.trim();
        if (single.matches("-?\\d+(\\.\\d+)?")) {
            return single;
        }
        if ((single.startsWith("'") && single.endsWith("'") && single.length() >= 2) ||
            (single.startsWith("\"") && single.endsWith("\"") && single.length() >= 2)) {
            return single;
        }
        if (single.equalsIgnoreCase("true") || single.equalsIgnoreCase("false")) {
            return single.toUpperCase(Locale.ROOT);
        }
        return "'" + single.replace("'", "''") + "'";
    }

    public static String formatParameterValue(String sql, int tokenStart, int tokenEnd, String rawValue) {
        if (rawValue == null || rawValue.isBlank() || rawValue.equalsIgnoreCase("<null>") || rawValue.equalsIgnoreCase("null")) {
            return "NULL";
        }
        String val = rawValue.trim();

        boolean inClause = (sql != null && tokenStart >= 0 && tokenEnd >= 0) && isInClause(sql, tokenStart, tokenEnd);

        // If inside an IN (...) clause and user wrapped their input in parentheses, strip them
        if (inClause && val.startsWith("(") && val.endsWith(")") && val.length() >= 2) {
            val = val.substring(1, val.length() - 1).trim();
            if (val.isBlank() || val.equalsIgnoreCase("<null>") || val.equalsIgnoreCase("null")) {
                return "NULL";
            }
        }

        List<String> tokens = parseTokens(val);
        if (tokens.isEmpty()) {
            return "NULL";
        }

        if (tokens.size() == 1) {
            return formatSingleLiteral(tokens.get(0));
        }

        // Multi-token: check if all tokens are numbers (e.g. "136,138" or "1, 2, 3")
        boolean allNumbers = tokens.stream().allMatch(t -> t.matches("-?\\d+(\\.\\d+)?"));
        if (allNumbers) {
            return String.join(", ", tokens);
        }

        // Multi-token: if all tokens are already valid literals (numbers, quoted strings, booleans, NULL)
        boolean allLiterals = tokens.stream().allMatch(SqlParameters::isLiteralToken);
        if (allLiterals) {
            return String.join(", ", tokens.stream().map(SqlParameters::formatSingleLiteral).toList());
        }

        // Multi-token: inside an IN clause, format each token as a literal (e.g. ACTIVE, PENDING -> 'ACTIVE', 'PENDING')
        if (inClause) {
            return String.join(", ", tokens.stream().map(SqlParameters::formatSingleLiteral).toList());
        }

        // Not in IN clause and not all tokens are literals -> treat the whole string as a single text literal
        return formatSingleLiteral(val);
    }

    private static String literal(String value) {
        return formatParameterValue(null, -1, -1, value);
    }
}

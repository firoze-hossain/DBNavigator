package com.roze.dbnavigator.util;

import com.roze.dbnavigator.db.AppSettingsStore.ExecuteActionConfig;
import javafx.scene.control.IndexRange;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamically resolves what SQL statement(s) to execute based on the active
 * Execute Action configuration, selection state, caret position, and dialect splitting rules.
 * Matches DataGrip's Query Execution options.
 */
public final class QueryExecutionResolver {

    private static final Pattern SUBQUERY_START = Pattern.compile(
            "^\\s*\\(\\s*(?:SELECT|WITH)\\b", Pattern.CASE_INSENSITIVE);

    public record CandidateOption(
            String title,
            String subtitle,
            List<String> statements,
            int startOffset,
            int endOffset
    ) {}

    public static class ResolvedExecution {
        public enum ActionType {
            EXECUTE,
            ASK_CHOOSER,
            NO_OP
        }

        private final ActionType actionType;
        private final List<String> statements;
        private final List<CandidateOption> candidates;
        private final String summary;
        private final int startOffset;
        private final int endOffset;

        private ResolvedExecution(ActionType actionType, List<String> statements,
                                  List<CandidateOption> candidates, String summary,
                                  int startOffset, int endOffset) {
            this.actionType = actionType;
            this.statements = statements != null ? statements : List.of();
            this.candidates = candidates != null ? candidates : List.of();
            this.summary = summary != null ? summary : "";
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        public static ResolvedExecution execute(List<String> statements, String summary, int start, int end) {
            return new ResolvedExecution(ActionType.EXECUTE, statements, null, summary, start, end);
        }

        public static ResolvedExecution ask(List<CandidateOption> candidates) {
            return new ResolvedExecution(ActionType.ASK_CHOOSER, null, candidates, "Choose statement to execute", -1, -1);
        }

        public static ResolvedExecution noOp(String reason) {
            return new ResolvedExecution(ActionType.NO_OP, null, null, reason, -1, -1);
        }

        public ActionType getActionType() { return actionType; }
        public List<String> getStatements() { return statements; }
        public List<CandidateOption> getCandidates() { return candidates; }
        public String getSummary() { return summary; }
        public int getStartOffset() { return startOffset; }
        public int getEndOffset() { return endOffset; }
    }

    private QueryExecutionResolver() {}

    /**
     * Resolves the execution target dynamically.
     */
    public static ResolvedExecution resolve(String fullText,
                                            IndexRange selection,
                                            int caretPosition,
                                            ExecuteActionConfig config,
                                            String scriptSplittingMode) {
        if (fullText == null || fullText.isBlank()) {
            return ResolvedExecution.noOp("Editor is empty");
        }

        String splittingMode = (scriptSplittingMode != null && !scriptSplittingMode.isBlank())
                ? scriptSplittingMode : SqlStatementSplitter.MODE_ANSI_OR_SEPARATOR;

        // 1. ACTIVE SELECTION
        if (selection != null && selection.getLength() > 0) {
            int selStart = Math.min(selection.getStart(), selection.getEnd());
            int selEnd = Math.max(selection.getStart(), selection.getEnd());
            String selectedText = fullText.substring(selStart, selEnd).strip();
            if (!selectedText.isEmpty()) {
                String forSelection = config.getForSelection() != null
                        ? config.getForSelection() : "Exactly as separate statements";

                switch (forSelection) {
                    case "Exactly as a single statement" -> {
                        String clean = cleanSingleStatement(selectedText);
                        return ResolvedExecution.execute(List.of(clean), "Selected statement", selStart, selEnd);
                    }
                    case "Smart expand to script" -> {
                        List<SqlStatementSplitter.Statement> all = SqlStatementSplitter.split(fullText, splittingMode);
                        List<SqlStatementSplitter.Statement> overlapping = new ArrayList<>();
                        for (SqlStatementSplitter.Statement s : all) {
                            if (s.end() > selStart && s.start() < selEnd) {
                                overlapping.add(s);
                            }
                        }
                        if (!overlapping.isEmpty()) {
                            int expStart = overlapping.get(0).start();
                            int expEnd = overlapping.get(overlapping.size() - 1).end();
                            List<String> list = overlapping.stream()
                                    .map(s -> cleanSingleStatement(s.text()))
                                    .filter(s -> !s.isEmpty())
                                    .toList();
                            return ResolvedExecution.execute(list, "Expanded " + list.size() + " statement(s)", expStart, expEnd);
                        }
                        // Fallback to separate
                        return separateStatements(selectedText, splittingMode, selStart, selEnd);
                    }
                    case "Exactly as separate statements" -> {
                        return separateStatements(selectedText, splittingMode, selStart, selEnd);
                    }
                    default -> {
                        return separateStatements(selectedText, splittingMode, selStart, selEnd);
                    }
                }
            }
        }

        // 2. NO SELECTION - CHECK CARET
        List<SqlStatementSplitter.Statement> allStatements = SqlStatementSplitter.split(fullText, splittingMode);
        SqlStatementSplitter.Statement stmtAtCaret = SqlStatementSplitter.statementAt(allStatements, caretPosition);

        if (stmtAtCaret != null) {
            // CARET INSIDE STATEMENT
            String whenInside = config.getWhenCaretInside() != null ? config.getWhenCaretInside() : "Ask what to execute";
            return handleCaretInside(fullText, allStatements, stmtAtCaret, caretPosition, whenInside, splittingMode);
        } else {
            // CARET OUTSIDE STATEMENT
            String whenOutside = config.getWhenCaretOutside() != null ? config.getWhenCaretOutside() : "Nothing";
            return handleCaretOutside(fullText, allStatements, caretPosition, whenOutside);
        }
    }

    private static ResolvedExecution handleCaretInside(String fullText,
                                                       List<SqlStatementSplitter.Statement> allStatements,
                                                       SqlStatementSplitter.Statement currentStmt,
                                                       int caretPosition,
                                                       String mode,
                                                       String splittingMode) {
        switch (mode) {
            case "Ask what to execute" -> {
                List<CandidateOption> candidates = new ArrayList<>();
                // 1. Current Statement
                String curClean = cleanSingleStatement(currentStmt.text());
                candidates.add(new CandidateOption(
                        "Statement: " + preview(curClean, 60),
                        "Current statement under caret",
                        List.of(curClean),
                        currentStmt.start(),
                        currentStmt.end()));

                // 2. Subquery if present
                SubqueryRange subquery = findEnclosingSubquery(fullText, caretPosition);
                if (subquery != null) {
                    String subClean = cleanSingleStatement(subquery.text);
                    candidates.add(new CandidateOption(
                            "Subquery: " + preview(subClean, 60),
                            "Enclosing subquery at caret",
                            List.of(subClean),
                            subquery.start,
                            subquery.end));
                }

                // 3. Whole Script
                if (allStatements.size() > 1) {
                    List<String> scriptStatements = allStatements.stream()
                            .map(s -> cleanSingleStatement(s.text()))
                            .filter(s -> !s.isEmpty())
                            .toList();
                    candidates.add(new CandidateOption(
                            "Whole script (" + scriptStatements.size() + " statements)",
                            "Execute all statements in console",
                            scriptStatements,
                            0,
                            fullText.length()));
                }

                // 4. Everything from caret
                List<String> fromCaret = new ArrayList<>();
                int fromStart = currentStmt.start();
                for (SqlStatementSplitter.Statement s : allStatements) {
                    if (s.end() >= currentStmt.start()) {
                        String clean = cleanSingleStatement(s.text());
                        if (!clean.isEmpty()) fromCaret.add(clean);
                    }
                }
                if (fromCaret.size() > 1 && fromCaret.size() < allStatements.size()) {
                    candidates.add(new CandidateOption(
                            "Everything from caret (" + fromCaret.size() + " statements)",
                            "Execute from current statement to end",
                            fromCaret,
                            fromStart,
                            fullText.length()));
                }

                return ResolvedExecution.ask(candidates);
            }

            case "Smallest subquery or statement" -> {
                SubqueryRange subquery = findEnclosingSubquery(fullText, caretPosition);
                if (subquery != null && !subquery.text.isBlank()) {
                    String subClean = cleanSingleStatement(subquery.text);
                    return ResolvedExecution.execute(List.of(subClean), "Subquery", subquery.start, subquery.end);
                }
                String clean = cleanSingleStatement(currentStmt.text());
                return ResolvedExecution.execute(List.of(clean), "Current statement", currentStmt.start(), currentStmt.end());
            }

            case "Smallest statement" -> {
                String clean = cleanSingleStatement(currentStmt.text());
                return ResolvedExecution.execute(List.of(clean), "Smallest statement", currentStmt.start(), currentStmt.end());
            }

            case "Largest statement", "Largest statement or batch" -> {
                String clean = cleanSingleStatement(currentStmt.text());
                return ResolvedExecution.execute(List.of(clean), "Largest statement", currentStmt.start(), currentStmt.end());
            }

            case "Whole script" -> {
                List<String> scriptStatements = allStatements.stream()
                        .map(s -> cleanSingleStatement(s.text()))
                        .filter(s -> !s.isEmpty())
                        .toList();
                return ResolvedExecution.execute(scriptStatements, "Whole script (" + scriptStatements.size() + " statements)", 0, fullText.length());
            }

            case "Everything from caret" -> {
                List<String> fromCaret = new ArrayList<>();
                for (SqlStatementSplitter.Statement s : allStatements) {
                    if (s.end() >= currentStmt.start()) {
                        String clean = cleanSingleStatement(s.text());
                        if (!clean.isEmpty()) fromCaret.add(clean);
                    }
                }
                return ResolvedExecution.execute(fromCaret, "From caret (" + fromCaret.size() + " statements)", currentStmt.start(), fullText.length());
            }

            default -> {
                String clean = cleanSingleStatement(currentStmt.text());
                return ResolvedExecution.execute(List.of(clean), "Statement", currentStmt.start(), currentStmt.end());
            }
        }
    }

    private static ResolvedExecution handleCaretOutside(String fullText,
                                                        List<SqlStatementSplitter.Statement> allStatements,
                                                        int caretPosition,
                                                        String mode) {
        switch (mode) {
            case "Nothing" -> {
                return ResolvedExecution.noOp("No statement at caret");
            }
            case "Whole script" -> {
                List<String> scriptStatements = allStatements.stream()
                        .map(s -> cleanSingleStatement(s.text()))
                        .filter(s -> !s.isEmpty())
                        .toList();
                return ResolvedExecution.execute(scriptStatements, "Whole script (" + scriptStatements.size() + " statements)", 0, fullText.length());
            }
            case "Everything below caret" -> {
                List<String> below = new ArrayList<>();
                int start = -1;
                for (SqlStatementSplitter.Statement s : allStatements) {
                    if (s.start() >= caretPosition) {
                        if (start < 0) start = s.start();
                        String clean = cleanSingleStatement(s.text());
                        if (!clean.isEmpty()) below.add(clean);
                    }
                }
                if (below.isEmpty()) {
                    return ResolvedExecution.noOp("No statements below caret");
                }
                return ResolvedExecution.execute(below, "Below caret (" + below.size() + " statements)", start, fullText.length());
            }
            default -> {
                return ResolvedExecution.noOp("No statement at caret");
            }
        }
    }

    private static ResolvedExecution separateStatements(String text, String splittingMode, int start, int end) {
        List<SqlStatementSplitter.Statement> split = SqlStatementSplitter.split(text, splittingMode);
        List<String> list = split.stream()
                .map(s -> cleanSingleStatement(s.text()))
                .filter(s -> !s.isEmpty())
                .toList();
        if (list.isEmpty()) {
            return ResolvedExecution.noOp("Selection contains no valid statements");
        }
        return ResolvedExecution.execute(list, list.size() + " statement(s)", start, end);
    }

    public static String cleanSingleStatement(String sql) {
        if (sql == null) return "";
        String trimmed = sql.strip();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).stripTrailing();
        }
        return trimmed;
    }

    private static String preview(String sql, int maxLen) {
        String singleLine = sql.replaceAll("\\s+", " ").strip();
        if (singleLine.length() <= maxLen) return singleLine;
        return singleLine.substring(0, maxLen - 1) + "\u2026";
    }

    public record SubqueryRange(String text, int start, int end) {}

    /**
     * Finds the innermost balanced subquery enclosing the caret position.
     * Looks for matched parentheses containing SELECT / WITH.
     */
    public static SubqueryRange findEnclosingSubquery(String sql, int caretPos) {
        if (sql == null || caretPos < 0 || caretPos > sql.length()) return null;

        int n = sql.length();
        // Look for enclosing parentheses around caretPos
        int bestStart = -1;
        int bestEnd = -1;
        int minSpan = Integer.MAX_VALUE;

        // Scan backward for '('
        for (int i = caretPos - 1; i >= 0; i--) {
            if (sql.charAt(i) == '(') {
                // Find matching ')'
                int matchEnd = findMatchingCloseParen(sql, i);
                if (matchEnd >= caretPos) {
                    int span = matchEnd - i;
                    if (span < minSpan) {
                        String candidate = sql.substring(i, matchEnd + 1);
                        if (SUBQUERY_START.matcher(candidate).find()) {
                            minSpan = span;
                            bestStart = i;
                            bestEnd = matchEnd + 1;
                        }
                    }
                }
            }
        }

        if (bestStart >= 0 && bestEnd > bestStart) {
            String inner = sql.substring(bestStart + 1, bestEnd - 1).strip();
            return new SubqueryRange(inner, bestStart + 1, bestEnd - 1);
        }

        return null;
    }

    private static int findMatchingCloseParen(String sql, int openIndex) {
        int n = sql.length();
        int depth = 0;
        boolean inSingle = false, inDouble = false, inLineComment = false, inBlockComment = false;

        for (int i = openIndex; i < n; i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < n) ? sql.charAt(i + 1) : '\0';

            if (inLineComment) { if (c == '\n') inLineComment = false; continue; }
            if (inBlockComment) { if (c == '*' && next == '/') { inBlockComment = false; i++; } continue; }
            if (inSingle) { if (c == '\'' && next == '\'') { i++; continue; } if (c == '\'') inSingle = false; continue; }
            if (inDouble) { if (c == '"' && next == '"') { i++; continue; } if (c == '"') inDouble = false; continue; }

            if (c == '-' && next == '-') { inLineComment = true; i++; continue; }
            if (c == '/' && next == '*') { inBlockComment = true; i++; continue; }
            if (c == '\'') { inSingle = true; continue; }
            if (c == '"') { inDouble = true; continue; }

            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
